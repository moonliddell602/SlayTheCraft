package com.slaythecraft.combat.mc;

import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.Enemy;
import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.core.Intent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * An {@link Enemy} backed by a real mob. While the battle lasts the mob is locked out of vanilla
 * combat: its AI is disabled, it cannot be hurt by normal means, and its health is driven purely by
 * the card battle.
 */
public final class MobCombatant extends Enemy {
    private final Mob mob;
    private boolean unlocked;
    /** 最后对这只怪造成伤害的玩家（击杀归属；见 {@link Combatant#recordDamageFrom}）。 */
    private ServerPlayer killer;

    public MobCombatant(Mob mob, String displayName, List<List<Intent>> turns) {
        this(mob, displayName, turns, 1.0D, 0);
    }

    /**
     * @param turns 意图状态机（v0.4.5：每个元素是一回合里依次执行的动作列表）
     * @param hpFactor 多人加成（v0.4.5：每多一名组队玩家，战斗生命 +50%）
     * @param battleHpOverride 尖塔战斗内的生命覆盖（v0.4.5：BOSS 表指定；0 = 用默认缩放）
     */
    /**
     * v0.4.6.7：直接吃一份意图状态机档案 —— 除了状态机本身，还要把档案里的**特殊机制**
     * 装到这只敌人身上（锁血 / 循环起点 / 睡眠）。
     */
    public MobCombatant(Mob mob, String displayName, MobIntents.Profile profile, double hpFactor,
                        int battleHpOverride) {
        this(mob, displayName, profile.turns(), hpFactor, battleHpOverride);
        setHpLock(profile.hpLock());
        setLoopFrom(profile.loopFrom());
        if (profile.sleepTurns() > 0) {
            setSleep(profile.sleepTurns(), profile.awakeTurns());
        }
    }

    public MobCombatant(Mob mob, String displayName, List<List<Intent>> turns, double hpFactor,
                        int battleHpOverride) {
        super(displayName, battleMaxHp(mob, hpFactor, battleHpOverride),
                battleCurrentHp(mob, hpFactor, battleHpOverride), turns, true);
        this.mob = mob;
        // V6：亡灵杀手附魔按原版 sensitive_to_smite 标签判定（整合包可扩展）
        setUndead(mob.getType().is(net.minecraft.tags.EntityTypeTags.SENSITIVE_TO_SMITE));
        // v0.4.1：阵营（图鉴与特性共用同一张表；玩家的药水/负面效果会被特性挡下）
        setFaction(Faction.ofMob(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()));
        // v0.4.6.7：记下实体 id（召唤类动作要数"场上还有几只同类"）
        setMobId(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }

    public Mob mob() {
        return mob;
    }

    /** 最后打这只怪的玩家；没人打过则为 {@code null}。 */
    public ServerPlayer killer() {
        return killer;
    }

    @Override
    public void recordDamageFrom(Combatant source) {
        if (source instanceof PlayerCombatant playerCombatant) {
            this.killer = playerCombatant.player();
        }
    }

    /** True when the underlying mob is gone (chunk unloaded, killed by something else, …). */
    public boolean isGone() {
        return mob.isRemoved() || !mob.isAlive();
    }

    /**
     * 被战斗锁定的标记（写在实体持久数据里）。
     *
     * <p>用途：万一服务器在战斗中被杀掉 / 存档异常，实体身上的 {@code NoAI}+{@code Invulnerable}
     * 会跟着存档一起留下来 —— 表现就是"敌人永远被定在原地"。带上这个标记之后，
     * 实体下次加入世界时若已经没有对应的战斗，就会被自动解锁（见 {@code CombatEvents}）。</p>
     */
    public static final String LOCK_TAG = "stc_locked";

    public void lock() {
        if (!Config.freezeEnemies()) {
            return;
        }
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean(LOCK_TAG, true);
    }

    /** Releases the mob on battle end. Defeated mobs are already dead and stay dead. */
    public void unlock() {
        if (unlocked) {
            return;
        }
        unlocked = true;
        mob.getPersistentData().remove(LOCK_TAG);
        clearIntentLabel();
        if (mob.isRemoved()) {
            return;
        }
        mob.setInvulnerable(false);
        mob.setNoAi(false);
    }

    /**
     * 把当前意图写成**头顶可见的文字**（v0.0.2 V2）。
     *
     * <p>用原版名称牌渲染而不是自绘世界文字：名称牌会自动面向镜头、正确贴合头顶高度、
     * 并且是实体数据天然同步给所有客户端——因此多人游戏里每个人看到的是同一份意图，
     * 也不需要处理投影矩阵。战斗结束时 {@link #unlock()} 会把名字清掉。</p>
     *
     * <p>文字里显示的是**实际**将造成的伤害（把力量/虚弱/易伤算进去），与结算共用
     * {@link CombatContext#previewDamage}，所以头顶数字不会骗人。</p>
     *
     * <p><b>v0.5.1.2</b>：需求"怪物头顶不再显示那么一大串字（战斗示意）" ——
     * 头顶现在只写 <b>名字 + 动作类型</b>（例如"僵尸：攻击"），
     * 具体的伤害/层数/目标全部留给战斗 UI 的敌人面板。完整文案仍是
     * {@link #turnText()}，供面板与自检使用。</p>
     */
    public void syncIntentLabel() {
        if (!alive()) {
            clearIntentLabel();
            return;
        }
        if (actions().isEmpty() && intent() == null) {
            return;
        }
        mob.setCustomName(Component.literal(name() + "：" + shortText()));
        mob.setCustomNameVisible(true);
    }

    /**
     * 头顶那行**短文案**：多个动作只列出不同的动作类型（例如"攻击 + 弱化"），不带任何数值。
     *
     * <p>数值（实际伤害、层数、目标）在战斗 UI 的敌人面板里，那里有足够宽度写清楚。
     * 头顶只保留"能远看分辨它要干什么"这一层信息。</p>
     */
    public String shortText() {
        List<Intent> actions = actions();
        if (actions.isEmpty()) {
            Intent single = intent();
            return single == null ? "未知" : typeLabel(single);
        }
        StringBuilder sb = new StringBuilder();
        for (Intent action : actions) {
            String label = typeLabel(action);
            // 同一回合里的重复动作只列一次（"攻击 2×3" 仍然是"攻击"）
            if (sb.indexOf(label) >= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(label);
        }
        return sb.length() == 0 ? "未知" : sb.toString();
    }

    /**
     * 一个动作的**类型**文字（不含量值）。
     *
     * <p>按意图类型给一个简短的中文动词：攻击 / 防御 / 蓄力 / 弱化 / 破防 / 灼烧 / 凋零 / 中毒 /
     * 缓慢 / 塞状态牌 / 治疗 / 蓄能 / 召唤 / 眩晕 …… 需求只要"别那么大串字"，所以刻意不写数值。</p>
     */
    private static String typeLabel(Intent action) {
        if (action == null) {
            return "未知";
        }
        return switch (action.type()) {
            case ATTACK -> "攻击";
            case DEFEND -> "防御";
            case BUFF_STRENGTH -> "蓄力";
            case BUFF_CHARGE -> "蓄能";
            case DEBUFF_MINING_FATIGUE -> "挖掘疲劳";
            case DEBUFF_VULNERABLE -> "破防";
            case DEBUFF_BURN -> "灼烧";
            case DEBUFF_DECAY -> "凋零";
            case DEBUFF_POISON -> "中毒";
            case DEBUFF_SLOW -> "缓慢";
            case DEBUFF_STATUS_CARD -> "塞状态牌";
            case HEAL -> "治疗";
            case SUMMON, SUMMON_IF_FEW -> "召唤";
            case LOCK -> "眩晕";
            case UNKNOWN -> "未知";
        };
    }

    /**
     * 本回合**全部动作**的头顶文案（v0.4.5 意图状态机）。
     *
     * <p>一回合只有一个动作时就是老文案；多个动作按状态机里的顺序用 " + " 串起来，
     * 例如僵尸第二回合显示"攻击 2×3（将对 玩家 共造成 6 点伤害） + 弱化（…）"。</p>
     */
    public String turnText() {
        List<Intent> actions = actions();
        if (actions.isEmpty()) {
            Intent single = intent();
            return single == null ? "未知" : intentText(single);
        }
        if (actions.size() == 1) {
            return intentText(actions.get(0));
        }
        StringBuilder sb = new StringBuilder();
        for (Intent action : actions) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(intentText(action));
        }
        return sb.toString();
    }

    /** 战斗结束/敌人倒下：恢复成没有名字牌的状态。 */
    public void clearIntentLabel() {
        if (mob.isRemoved()) {
            return;
        }
        mob.setCustomName(null);
        mob.setCustomNameVisible(false);
    }

    private String intentText(Intent intent) {
        Combatant target = target();
        String targetName = target == null ? null : target.name();
        return switch (intent.type()) {
            case ATTACK -> {
                int single = CombatContext.previewDamage(this, target == null ? this : target, intent.value());
                int total = single * intent.times();
                String who = targetName == null ? "玩家" : targetName;
                yield intent.times() > 1
                        ? "攻击 " + intent.value() + "×" + intent.times() + "（将对 " + who + " 共造成 " + total + " 点伤害）"
                        : "攻击 " + intent.value() + "（将对 " + who + " 造成 " + single + " 点伤害）";
            }
            case DEFEND -> "防御 " + intent.value() + "（将获得 " + intent.value() + " 点格挡）";
            case BUFF_STRENGTH -> "蓄力（力量 +" + intent.value() + "）";
            case DEBUFF_MINING_FATIGUE -> "挖掘疲劳（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 挖掘疲劳 " + intent.value() + "，" + intent.value() + " 回合内攻击伤害 -25%）";
            case DEBUFF_VULNERABLE -> "破防（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 易伤 " + intent.value() + "，" + intent.value() + " 回合内受到的伤害 +25%）";
            case DEBUFF_BURN -> "灼烧（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 下回合开始受到 " + intent.value() + " 点伤害）";
            // v0.4.6.1：凋零改成"累加、超过当前生命即死"那一套
            case DEBUFF_DECAY -> "凋零（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 累积 " + intent.value() + " 层凋零，凋零大于当前生命时回合结束即死）";
            // v0.4.6.1：中毒改成"出牌环节结束按层数受伤、每回合 -1"那一套
            case DEBUFF_POISON -> "中毒（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 累积 " + intent.value() + " 层中毒，出牌环节结束时按层数受伤）";
            case DEBUFF_SLOW -> "缓慢（将使 " + (targetName == null ? "玩家" : targetName)
                    + " 获得的格挡 -" + intent.value() + "）";
            case DEBUFF_STATUS_CARD -> {
                com.slaythecraft.combat.core.Card statusCard =
                        com.slaythecraft.combat.core.CardLibrary.byId(intent.payload());
                String cardName = statusCard == null ? intent.payload() : statusCard.name();
                yield "塞入状态牌（将把 " + intent.value() + " 张「" + cardName + "」放进 "
                        + (targetName == null ? "玩家" : targetName) + " 的弃牌堆）";
            }
            // -------------------------------------------------- v0.4.6.7
            case HEAL -> "恢复（将回复 " + intent.value() + " 点生命）";
            case BUFF_CHARGE -> "蓄能（蓄能 +" + intent.value() + "，死亡前会一次性打出来）";
            case SUMMON -> "召唤（将召唤 " + intent.value() + " 只随从加入战斗）";
            case SUMMON_IF_FEW -> "召唤（场上同类不超过 " + intent.times() + " 只时召唤 " + intent.value() + " 只）";
            // v0.5.0.8 需求（用户例子）："在面板上显示的行动应该为：自爆 X（X 等同于蓄能的层数）"
            case LOCK -> hpLocked()
                    ? "自爆 " + charge() + "（被击晕：下一次敌人回合一次性打出来后倒下）"
                    : "自爆 " + charge();
            case UNKNOWN -> "未知";
        };
    }

    @Override
    protected void onHpChanged(int previousHp, int newHp) {
        mob.setHealth(Math.max(0.0F, Math.min(mob.getMaxHealth(), newHp)));
    }

    @Override
    protected void onDefeated() {
        CombatManager.killMob(mob, killer);
    }

    /**
     * 尖塔战斗内的生命值（v0.4.5）。
     *
     * <p>优先用 BOSS 表的覆盖值（需求："其尖塔玩法内战斗生命值调整为 X，不调整其尖塔玩法局外的原版生命值"）；
     * 否则用"原版生命 × 配置缩放 × 多人加成"。**原版实体的血量上限完全不动**，
     * 战斗伤害只会同步到实体的当前血量用于表现。</p>
     */
    public static int battleMaxHp(Mob mob, double hpFactor, int battleHpOverride) {
        if (battleHpOverride > 0) {
            return Math.max(1, (int) Math.round(battleHpOverride * hpFactor));
        }
        return Math.max(1, (int) Math.round(mob.getMaxHealth() * Config.mobHealthScale() * hpFactor));
    }

    /**
     * 战斗开始时的当前生命：沿用"按原版当前血量比例"的老规则；
     * BOSS 覆盖值是绝对值，所以直接满血开始。
     */
    public static int battleCurrentHp(Mob mob, double hpFactor, int battleHpOverride) {
        if (battleHpOverride > 0) {
            return battleMaxHp(mob, hpFactor, battleHpOverride);
        }
        return Math.max(1, (int) Math.round(mob.getHealth() * Config.mobHealthScale() * hpFactor));
    }
}
