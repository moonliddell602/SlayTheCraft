package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 药水表（v0.0.2 V8；v0.4.6 扩充到 14 瓶）。
 *
 * <p>需求（v0.4.6）新增了一批药水，并给出"目标范围 + 贴图形态"的规则：</p>
 * <ul>
 *   <li>对敌人使用 → **喷溅**药水贴图（中毒 / 火焰 / 虚弱）；</li>
 *   <li>对自己 / 队友使用 → **常规**药水贴图（治疗 / 力量 / 迅捷 / 神龟 / 再生 / 缓降 / 蘑菇煲）；</li>
 *   <li>特殊药水（只能对自己用）→ **滞留**药水贴图（幸运药水 / 奶桶），
 *       不祥之瓶与两瓶炖菜则直接用**物品贴图**（{@link PotionForm#ITEM}）。</li>
 * </ul>
 *
 * <p>v0.4.6.7 修正：不祥之瓶的名字里『祥』以前误写成了『详』（已更正）；它的图标以前用的是**滞留药水**贴图，
 * 按需求原文"不祥之瓶（药水）的图标修订：不祥之瓶，而不是滞留药水"改为原版**不祥之瓶物品**贴图。</p>
 */
public final class PotionLibrary {

    private static final Map<String, Potion> BY_ID = new LinkedHashMap<>();

    // ------------------------------------------------------------------ 正面：对自己 / 队友

    /** 治疗药水（v0.4.6 按需求改为回复 10 点生命）。 */
    public static final Potion HEAL = register(new Potion(
            "heal_potion", "治疗药水", "恢复 10 点生命值。", PotionKind.BENEFICIAL, PotionForm.NORMAL, 0xF82423, "",
            false,
            (ctx, user, target) -> {
                target.heal(10);
                ctx.announce("  回复 10 点生命（HP " + target.hp() + "/" + target.maxHp() + "）");
            }));

    /**
     * 力量药水（v0.4.6 按需求改为"临时力量"）。
     *
     * <p>需求原文："获得 5 层力量。下个回合开始时，失去 5 层力量。"</p>
     */
    public static final Potion STRENGTH = register(new Potion(
            "strength_potion", "力量药水", "获得 5 层力量。下个回合开始时，失去 5 层力量。", PotionKind.BENEFICIAL,
            PotionForm.NORMAL, 0x932423, "", false,
            (ctx, user, target) -> {
                target.addTemporaryStrength(5);
                ctx.announce("  力量 +5（下个回合开始时失去，当前 +" + target.strength() + "）");
            }));

    /** 迅捷药水：获得 2 点迅捷。 */
    public static final Potion DEXTERITY = register(new Potion(
            "dexterity_potion", "迅捷药水", "获得 2 点迅捷。", PotionKind.BENEFICIAL, PotionForm.NORMAL, 0x7CAFC6,
            "", false,
            (ctx, user, target) -> {
                target.addDexterity(2);
                ctx.announce("  迅捷 +2（当前 +" + target.dexterity() + "）");
            }));

    /** 神龟药水（v0.4.6）：获得神龟 12、缓慢 2。 */
    public static final Potion TURTLE = register(new Potion(
            "turtle_potion", "神龟药水", "获得神龟 12，获得缓慢 2。", PotionKind.BENEFICIAL, PotionForm.NORMAL,
            0x9146F5, "", false,
            (ctx, user, target) -> {
                target.addPlatedArmor(12);
                target.addSlow(2);
                ctx.announce("  获得神龟 12、缓慢 2" + target.statusSuffix());
            }));

    /** 再生药水（v0.4.6）：获得 5 层再生。 */
    public static final Potion REGENERATION = register(new Potion(
            "regeneration_potion", "再生药水", "获得 5 层再生。", PotionKind.BENEFICIAL, PotionForm.NORMAL, 0xCD5CAB,
            "", false,
            (ctx, user, target) -> {
                target.addRegeneration(5);
                ctx.announce("  获得再生 5（每回合开始回复 5 点生命）" + target.statusSuffix());
            }));

    /** 缓降药水（v0.4.6）：获得 1 层缓降（免疫下一次伤害）。 */
    public static final Potion SLOW_FALL = register(new Potion(
            "slow_fall_potion", "缓降药水", "获得 1 层缓降（免疫下一次受到的伤害）。", PotionKind.BENEFICIAL,
            PotionForm.NORMAL, 0xE8E8F0, "", false,
            (ctx, user, target) -> {
                target.addSlowFall(1);
                ctx.announce("  获得缓降 1（免疫下一次受到的伤害）" + target.statusSuffix());
            }));

    /** 蘑菇煲（v0.4.6）：获得 3 层再生，恢复 8 点生命。贴图直接用蘑菇煲物品。 */
    public static final Potion MUSHROOM_STEW = register(new Potion(
            "mushroom_stew_potion", "蘑菇煲", "获得 3 层再生，恢复 8 点生命。", PotionKind.BENEFICIAL,
            PotionForm.ITEM, 0, "minecraft:mushroom_stew", false,
            (ctx, user, target) -> {
                target.addRegeneration(3);
                target.heal(8);
                ctx.announce("  获得再生 3，回复 8 点生命（HP " + target.hp() + "/" + target.maxHp() + "）"
                        + target.statusSuffix());
            }));

    // ------------------------------------------------------------------ 负面：对敌人（喷溅贴图）

    /**
     * 中毒药水（v0.4.6 重定义）：为敌人施加 15 层【中毒】。
     *
     * <p>需求："中毒：每拥有 x 点中毒，出牌环节结束时，受到 X 点伤害。每一个新的回合会减少 1 层数。"
     * 所以这瓶药水现在给的是**持续伤害**那套（图标用原版中毒效果）。</p>
     */
    public static final Potion POISON = register(new Potion(
            "poison_potion", "中毒药水",
            "为敌人施加 15 层中毒（出牌环节结束时受到 15 点伤害，每回合 -1 层）。", PotionKind.HARMFUL,
            PotionForm.SPLASH, 0x4E9331, "", false,
            (ctx, user, target) -> {
                if (target.immuneToDamagePotions()) {
                    ctx.announce("  " + target.name() + " 是【" + target.factionLabel()
                            + "】：免疫伤害药水");
                    return;
                }
                ctx.applyPoison(target, 15);
            }));

    /**
     * 凋零药水（v0.4.6 新增）：为敌人施加 12 层【凋零】。
     *
     * <p>需求："凋零：可以累加。当对应角色/敌人累计的中毒大于当前生命值，回合结束后会立刻杀死该敌人。"
     * 图标用原版**凋零（wither）**效果。</p>
     */
    public static final Potion DECAY = register(new Potion(
            "decay_potion", "凋零药水",
            "为敌人施加 12 层凋零（累计大于当前生命值时，回合结束后立刻死亡）。", PotionKind.HARMFUL,
            PotionForm.SPLASH, 0x736156, "", false,
            (ctx, user, target) -> {
                if (target.immuneToDamagePotions()) {
                    ctx.announce("  " + target.name() + " 是【" + target.factionLabel()
                            + "】：免疫伤害药水");
                    return;
                }
                ctx.applyDecay(target, 12);
            }));

    /** 火焰药水：为敌人施加 8 层灼烧。 */
    public static final Potion BURN = register(new Potion(
            "burn_potion", "火焰药水", "为敌人施加 8 层灼烧。", PotionKind.HARMFUL, PotionForm.SPLASH, 0xE49A3A, "",
            false,
            (ctx, user, target) -> {
                if (target.immuneToDamagePotions()) {
                    ctx.announce("  " + target.name() + " 是【" + target.factionLabel()
                            + "】：免疫伤害药水");
                    return;
                }
                if (target.immuneToBurn()) {
                    ctx.announce("  " + target.name() + " 是【" + target.factionLabel()
                            + "】：免疫灼烧");
                    return;
                }
                target.addBurn(8);
                ctx.announce("  为 " + target.name() + " 施加灼烧 8" + target.statusSuffix());
            }));

    /**
     * 虚弱药水（v0.5.1.4 文案跟着「虚弱重做」更新）。
     *
     * <p>虚弱现在"每层 -1 攻击伤害、可以累加、不随回合消失"，所以 3 层就是**永久**让对方每次攻击
     * 少打 3 点；旧那套"-25% 持续 3 回合"已经改名叫【挖掘疲劳】。
     * ⚠️ 这个取舍是**我按需求原文判的**（需求只点名"生物的给予虚弱意图"改成挖掘疲劳，没提药水）：
     * 要改成"给予 3 层挖掘疲劳"的话，改这一行即可。</p>
     */
    public static final Potion WEAK = register(new Potion(
            "weak_potion", "虚弱药水", "给予 3 层虚弱（每层使其攻击伤害 -1，会先抵消它的力量）。",
            PotionKind.HARMFUL, PotionForm.SPLASH, 0x484D48, "", false,
            (ctx, user, target) -> {
                target.addWeak(3);
                ctx.announce("  " + target.name() + " 陷入虚弱 3" + target.statusSuffix());
            }));

    // ------------------------------------------------------------------ 特殊：只能对自己（滞留 / 物品贴图）

    /**
     * 不祥之瓶（v0.4.6）：每回合开始时给自己 5 层**凋零**；获得 5 力量、5 敏捷、抽 3 张牌。
     *
     * <p>凋零是**负面**状态：累计层数大于当前生命值就回合结束即死，所以这瓶药是"用时间换爆发"。</p>
     *
     * <p>v0.4.6.7：名字里『祥』以前误写成『详』（已更正）；图标由滞留药水改为原版
     * **不祥之瓶物品**贴图（需求原文："不祥之瓶（药水）的图标修订：不祥之瓶，而不是滞留药水"），
     * 因此形态从 {@link PotionForm#LINGERING} 改成 {@link PotionForm#ITEM} + {@code minecraft:ominous_bottle}。
     * 药水效果本身完全不变。</p>
     */
    public static final Potion OMINOUS = register(new Potion(
            "ominous_bottle", "不祥之瓶",
            "每回合开始时给予自己 5 层凋零。获得 5 层力量，获得 5 层敏捷，抽三张牌。",
            PotionKind.BENEFICIAL, PotionForm.ITEM, 0, "minecraft:ominous_bottle", true,
            (ctx, user, target) -> {
                // "每回合开始时给予自己 5 层凋零" 是一个持续标记，不是一次性效果
                target.setOminous(true);
                target.addDecay(5);
                target.addStrength(5);
                target.addDexterity(5);
                ctx.drawCardsFor(target, 3);
                ctx.announce("  获得 5 层凋零、力量 +5、敏捷 +5，并抽 3 张牌" + target.statusSuffix());
            }));

    /** 幸运药水（v0.4.6）：升级下一次卡牌奖励的所有可选卡牌。 */
    public static final Potion LUCK = register(new Potion(
            "luck_potion", "幸运药水", "升级下一次卡牌奖励的所有可选卡牌。", PotionKind.BENEFICIAL,
            PotionForm.LINGERING, 0x339900, "", true,
            (ctx, user, target) -> ctx.markLuckyReward(target)));

    /**
     * 奶桶（v0.4.6）：清除自身的所有状态。
     *
     * <p>v0.5.0.4 贴图修订：需求原文"奶桶（药水物品）的贴图错误，应该为 minecraft:milk_bucket"。
     * 原来用的是 {@link PotionForm#LINGERING}（程序化画的滞留药水瓶），
     * 现在和 {@link #OMINOUS} / {@link #MUSHROOM_STEW} / {@link #SUSPICIOUS_STEW} 一样
     * 走 {@link PotionForm#ITEM} + 原版物品贴图。药水效果完全不变。</p>
     */
    public static final Potion MILK = register(new Potion(
            "milk_bucket", "奶桶", "清除自身的所有状态。", PotionKind.BENEFICIAL, PotionForm.ITEM, 0,
            "minecraft:milk_bucket", true,
            (ctx, user, target) -> {
                target.clearStatusEffects();
                ctx.announce("  所有状态被清除（充能球不受影响）");
            }));

    /** 谜之炖菜（v0.4.6）：获得随机层数的随机效果。贴图直接用谜之炖菜物品。 */
    public static final Potion SUSPICIOUS_STEW = register(new Potion(
            "suspicious_stew_potion", "谜之炖菜", "获得随机层数的随机效果。", PotionKind.BENEFICIAL,
            PotionForm.ITEM, 0, "minecraft:suspicious_stew", true,
            (ctx, user, target) -> applyRandomStew(ctx, target)));

    private PotionLibrary() {
    }

    /** 谜之炖菜的随机效果表：每一种给"1..4 层"的随机强度。 */
    private static void applyRandomStew(CombatContext ctx, Combatant target) {
        int roll = ctx.nextRandomInt(6);
        int amount = 1 + ctx.nextRandomInt(4);
        switch (roll) {
            case 0 -> {
                target.addStrength(amount);
                ctx.announce("  谜之炖菜：力量 +" + amount + target.statusSuffix());
            }
            case 1 -> {
                target.addDexterity(amount);
                ctx.announce("  谜之炖菜：迅捷 +" + amount + target.statusSuffix());
            }
            case 2 -> {
                target.addRegeneration(amount);
                ctx.announce("  谜之炖菜：再生 " + amount + target.statusSuffix());
            }
            case 3 -> {
                target.addSlowFall(amount);
                ctx.announce("  谜之炖菜：缓降 " + amount + target.statusSuffix());
            }
            case 4 -> {
                target.addPlatedArmor(amount);
                ctx.announce("  谜之炖菜：神龟 " + amount + target.statusSuffix());
            }
            default -> {
                target.addVigor(amount);
                ctx.announce("  谜之炖菜：活力 " + amount + target.statusSuffix());
            }
        }
    }

    private static Potion register(Potion potion) {
        BY_ID.put(potion.id(), potion);
        return potion;
    }

    public static Potion byId(String id) {
        return BY_ID.get(id);
    }

    public static List<Potion> all() {
        return List.copyOf(BY_ID.values());
    }

    /**
     * 牧师事件（v0.4.6 需求）：药水选项从这五瓶里**随机抽一瓶**。
     *
     * <p>需求原文："牧师的药水选项将从【治疗药水/再生药水/虚弱药水/力量药水/奶桶】随机抽取。"</p>
     */
    public static List<Potion> clericPool() {
        return List.of(HEAL, REGENERATION, WEAK, STRENGTH, MILK);
    }

    /** 可以随机发放 / 商店出售的药水（目前就是全部）。 */
    public static List<Potion> obtainable() {
        return all();
    }

    /** 把存档里的 id 列表翻译成药水（不认识的 id 跳过，删档不会报错）。 */
    public static List<Potion> potionsOf(Collection<String> ids) {
        List<Potion> potions = new ArrayList<>(ids.size());
        for (String id : ids) {
            Potion potion = BY_ID.get(id);
            if (potion != null) {
                potions.add(potion);
            }
        }
        return List.copyOf(potions);
    }
}
