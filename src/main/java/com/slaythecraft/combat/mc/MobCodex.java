package com.slaythecraft.combat.mc;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.core.BattleHp;
import com.slaythecraft.combat.core.FloorBoss;
import com.slaythecraft.combat.core.Intent;
import com.slaythecraft.combat.core.MobTier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * **怪物图鉴**的数据来源（v0.4.6）。
 *
 * <p>需求原文："点击查看阵营详情后，显示所属阵营内的怪物图鉴。点击可以查看怪物图鉴详情，
 * 内容包含：怪物生命值，常规/精英/boss 敌怪，意图状态机，描述。"</p>
 *
 * <p>于是每条记录把四处数据合起来：</p>
 * <ul>
 *   <li>名字与意图状态机 → {@link MobIntents}；</li>
 *   <li>档位（常规 / 精英 / BOSS）→ {@code mob_tier/*} 标签（{@link MobTiers#tierOf(EntityType)}）；</li>
 *   <li>尖塔战斗生命值 → {@link BattleHp} 的覆盖值（没有覆盖时按原版生命 × {@code mobHealthScale} 缩放）。</li>
 * </ul>
 *
 * <p>v0.4.6.2 需求："不再显示原版生命值" + "怪物排序从上到下根据 BOSS-精英-常规的顺序排序"，
 * 所以 {@code vanillaHp} 只作为内部计算依据保留、界面不再展示，列表按档位排序。</p>
 */
public final class MobCodex {

    private MobCodex() {
    }

    /**
     * 图鉴里的一条怪物记录。
     *
     * @param id          实体 id（{@code minecraft:zombie}）
     * @param name        战斗里的显示名
     * @param tier        常规 / 精英 / BOSS / 不参战
     * @param vanillaHp   原版生命值（**只用于内部换算**；v0.4.6.2 起界面不再展示）
     * @param battleHp    尖塔战斗里的生命值（覆盖值，或原版 × 配置缩放）
     * @param overridden  尖塔战斗生命值是不是覆盖表里指定的固定值
     * @param description 图鉴描述
     * @param turns       意图状态机（每回合一行动作列表）
     */
    public record MobEntry(String id, String name, MobTier tier, double vanillaHp, int battleHp,
                           boolean overridden, String description, List<List<Intent>> turns) {

        /** 档位文案（常规 / 精英 / BOSS）。 */
        public String tierLabel() {
            return switch (tier) {
                case BOSS -> "BOSS";
                case ELITE -> "精英";
                case NORMAL -> "常规";
                case NONE -> "不参战";
            };
        }

        /** 意图状态机的可读文本：每回合一行。 */
        public List<String> scriptLines() {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < turns.size(); i++) {
                StringBuilder line = new StringBuilder("第 ").append(i + 1).append(" 回合：");
                List<Intent> actions = turns.get(i);
                if (actions.isEmpty()) {
                    line.append("什么都不做");
                } else {
                    for (int a = 0; a < actions.size(); a++) {
                        if (a > 0) {
                            line.append("，");
                        }
                        line.append(label(actions.get(a)));
                    }
                }
                lines.add(line.toString());
            }
            return lines;
        }

        private static String label(Intent intent) {
            return switch (intent.type()) {
                case ATTACK -> intent.times() > 1
                        ? "伤害 " + intent.value() + "×" + intent.times()
                        : "伤害 " + intent.value();
                case DEFEND -> "获得 " + intent.value() + " 格挡";
                case BUFF_STRENGTH -> "获得力量 " + intent.value();
                case DEBUFF_MINING_FATIGUE -> "对玩家施加挖掘疲劳 " + intent.value();
                case DEBUFF_VULNERABLE -> "对玩家施加易伤 " + intent.value();
                case DEBUFF_BURN -> "对玩家施加灼烧 " + intent.value();
                case DEBUFF_DECAY -> "对玩家施加凋零 " + intent.value();
                case DEBUFF_POISON -> "对玩家施加中毒 " + intent.value();
                case DEBUFF_SLOW -> "对玩家施加缓慢 " + intent.value();
                case DEBUFF_STATUS_CARD -> {
                    Card card = com.slaythecraft.combat.core.CardLibrary.byId(intent.payload());
                    yield "塞入 " + intent.value() + " 张【" + (card == null ? intent.payload() : card.name()) + "】";
                }
                // -------------------------------------------------- v0.4.6.7
                case HEAL -> "恢复 " + intent.value() + " 生命";
                case BUFF_CHARGE -> "蓄能 " + intent.value();
                case SUMMON -> "召唤 " + intent.value() + " 只随从";
                case SUMMON_IF_FEW -> "召唤 " + intent.value() + " 只随从（场上不超过 " + intent.times() + " 只时）";
                case LOCK -> "晕眩（结算蓄能后倒下）";
                case UNKNOWN -> "未知动作";
            };
        }
    }

    /**
     * **虚拟怪物**（v0.4.6.7）：原版没有对应的实体类型，但要有图鉴条目 —— 目前是
     * 【闪电苦力怕】（带电的苦力怕，第 1 层 BOSS，尖塔战斗生命 250）。
     */
    public static List<MobEntry> virtualEntries() {
        List<MobEntry> list = new ArrayList<>();
        for (String id : MobIntents.virtualIds()) {
            MobIntents.Profile profile = MobIntents.virtualProfile(id);
            FloorBoss boss = FloorBoss.byEntityId(id);
            if (profile == null || boss == null) {
                continue;
            }
            list.add(new MobEntry(id, profile.displayName(), MobTier.BOSS, 0.0D, boss.battleHp(), true,
                    MobIntents.describeVirtual(id), profile.turns()));
        }
        sortForCodex(list);
        return list;
    }

    /**
     * 图鉴里某条记录**归属的阵营**（v0.4.6.3）。
     *
     * <p>需求原文："BOSS 图鉴没有正常在各个所属阵营显示，有时也会显示'阵营没有记录怪物'的文案。
     * 解决建议：将所有的 BOSS 图鉴全部移到【尖塔护卫】阵营。"（即代码里的 {@link Faction#SPIRE_GUARDIAN}
     * 【尖塔护卫】。）</p>
     *
     * <p>所以 BOSS 档位的记录**一律**归【尖塔护卫】，不论它在 {@link Faction#ofMob(String)} 里的
     * 战斗归属是什么。注意这只影响**图鉴展示**：战斗里的阵营特性（免疫灼烧等）仍然走
     * {@code Faction.ofMob}，由 {@code MobCombatant} 决定。</p>
     */
    public static Faction factionOf(String entityId, MobTier tier) {
        if (tier == MobTier.BOSS) {
            return Faction.SPIRE_GUARDIAN;
        }
        return Faction.ofMob(entityId);
    }

    /** 图鉴里这条记录归属的阵营（BOSS 一律归【尖塔护卫】）。 */
    public static Faction factionOf(MobEntry entry) {
        return entry == null ? null : factionOf(entry.id(), entry.tier());
    }

    /** 某个阵营里的全部怪物（按档位 + 显示名排序）。 */
    public static List<MobEntry> of(Faction faction) {
        List<MobEntry> list = new ArrayList<>();
        if (faction == null) {
            return list;
        }
        for (MobIntents.Entry entry : MobIntents.entries()) {
            MobEntry built = build(entry);
            if (factionOf(built) == faction) {
                list.add(built);
            }
        }
        for (MobEntry virtual : virtualEntries()) {
            if (factionOf(virtual) == faction) {
                list.add(virtual);
            }
        }
        sortForCodex(list);
        return list;
    }

    /** 全部怪物。 */
    public static List<MobEntry> all() {
        List<MobEntry> list = new ArrayList<>();
        for (MobIntents.Entry entry : MobIntents.entries()) {
            list.add(build(entry));
        }
        list.addAll(virtualEntries());
        sortForCodex(list);
        return list;
    }

    /**
     * 图鉴排序（v0.4.6.2 需求："怪物排序从上到下根据 BOSS-精英-常规的顺序排序"）。
     *
     * <p>同一档位内按显示名排序，方便查找。</p>
     */
    public static void sortForCodex(List<MobEntry> list) {
        list.sort((a, b) -> {
            int byTier = Integer.compare(order(a.tier()), order(b.tier()));
            return byTier != 0 ? byTier : a.name().compareTo(b.name());
        });
    }

    /** 排序权重：BOSS 最前，其次精英、常规，最后是不参战的生物。 */
    public static int order(MobTier tier) {
        return switch (tier) {
            case BOSS -> 0;
            case ELITE -> 1;
            case NORMAL -> 2;
            case NONE -> 3;
        };
    }

    /** 按实体 id 找一条（{@code minecraft:zombie}）。 */
    public static MobEntry byId(String entityId) {
        for (MobIntents.Entry entry : MobIntents.entries()) {
            if (entry.id().equals(entityId)) {
                return build(entry);
            }
        }
        return null;
    }

    private static MobEntry build(MobIntents.Entry entry) {
        EntityType<?> type = entry.type();
        MobTier tier = MobTiers.tierOf(type);
        double vanillaHp = vanillaMaxHp(type);
        // v0.4.6.2：BOSS 表 + 非 BOSS 覆盖表统一走 BattleHp
        int override = BattleHp.overrideFor(entry.id());
        boolean overridden = override > 0;
        int battleHp = overridden ? override
                : (int) Math.max(1, Math.round(vanillaHp * Config.mobHealthScale()));
        return new MobEntry(entry.id(), entry.profile().displayName(), tier, vanillaHp, battleHp, overridden,
                MobIntents.describe(type), entry.profile().turns());
    }

    /** 原版默认最大生命值（查不到时返回 0，界面显示"？"）。 */
    private static double vanillaMaxHp(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return 0.0D;
        }
        try {
            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity> living = (EntityType<? extends LivingEntity>) type;
            return DefaultAttributes.getSupplier(living).getValue(Attributes.MAX_HEALTH);
        } catch (RuntimeException e) {
            return 0.0D;
        }
    }

    /** 实体 id 的命名空间（图鉴里显示"来自 mod xxx"用）。 */
    public static String namespaceOf(String entityId) {
        ResourceLocation id = entityId == null ? null : ResourceLocation.tryParse(entityId);
        return id == null ? "" : id.getNamespace();
    }

    /** 该实体类型是不是原版的（非原版会在图鉴里标注）。 */
    public static boolean isVanilla(String entityId) {
        return "minecraft".equals(namespaceOf(entityId));
    }

    /** 实体类型（用于显示方块/物品图标等；查不到返回 null）。 */
    public static EntityType<?> typeOf(String entityId) {
        ResourceLocation id = entityId == null ? null : ResourceLocation.tryParse(entityId);
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
    }
}
