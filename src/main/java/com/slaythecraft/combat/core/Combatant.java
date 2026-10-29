package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A participant in a card battle. This class is deliberately free of any Minecraft type so the
 * whole rule set can be exercised by plain JVM code (see {@link CombatSelfTest}).
 *
 * <p>HP、格挡与全部状态都住在这里；Minecraft 层只把 {@link #onHpChanged(int, int)} 与
 * {@link #onDefeated()} 映射到真实实体上。</p>
 *
 * <p><b>状态语义（v0.5.1.4 重做"虚弱"之后）</b>：</p>
 * <ul>
 *   <li>{@code strength 力量}：每层攻击伤害 +1（可累加，不衰减）；**与虚弱互相抵消**</li>
 *   <li>{@code weak 虚弱}（**v0.5.1.4 重做**）：可以累加，每层攻击伤害 **−1**，**不随回合衰减**；
 *       施加时会先抵消目标身上的力量（需求给的例子：力量 3 的敌人吃 2 层虚弱 → 力量 1）</li>
 *   <li>{@code miningFatigue 挖掘疲劳}（**v0.5.1.4 新**）：可以累加，每累计 X 点则在 X 回合内
 *       攻击伤害 **−25%**（每回合 -1 层）。**这是"旧虚弱"的机制换了个名字**</li>
 *   <li>{@code vulnerable 易伤}（v0.0.3 改）：层数 = **剩余回合数**，期间受到的伤害 **+25%**（可累加）</li>
 *   <li>{@code vigor 活力}（v0.0.3 新）：下一张攻击牌额外造成等于层数的伤害，打完清零</li>
 *   <li>{@code dexterity 敏捷}：每层获得的格挡 +1；**与缓慢互相抵消**</li>
 *   <li>{@code slow 缓慢}：每层获得的格挡 −1；**与敏捷互相抵消**</li>
 *   <li>{@code burn 灼烧}（v0.4.6）：可以累加；自己回合开始时失去等同层数的**生命值**（无视格挡）</li>
 *   <li>{@code poison 中毒}（v0.4.6 重定义 = 旧"涂毒"）：出牌环节结束时按层数受到伤害，每回合 -1 层</li>
 *   <li>{@code decay 凋零}（v0.4.6 重定义 = 旧"中毒"）：可以累加；层数**大于**当前生命时回合结束后立刻死亡</li>
 *   <li>{@code platedArmor 神龟}：结束出牌时获得等同层数的格挡</li>
 * </ul>
 */
public abstract class Combatant {
    private final String name;
    /** 最大生命值（v0.5.2.2：不再是 final —— A4-9【进食】要把上限永久抬高）。 */
    private int maxHp;
    private int hp;
    private int block;
    private int strength;
    /**
     * 虚弱（**v0.5.1.4 重做**）：可以累加，每层让攻击伤害 **−1**，且**不随回合衰减**。
     *
     * <p>需求原文："虚弱：可以累加。每累计一点，攻击伤害-1。" 并且"虚弱与力量可以相互抵消"。
     * 施加虚弱时会先把目标的力量抵掉（力量 3 的敌人吃 2 层虚弱 → 力量 1、虚弱 0）。</p>
     */
    private int weak;
    /**
     * **挖掘疲劳**（v0.5.1.4 新）：可以累加，每累计 X 点则在 X 回合内攻击伤害 **−25%**。
     *
     * <p>这就是"旧虚弱"的那套机制，只是换了个名字 —— 需求原文："先前的虚弱机制改为：挖掘疲劳。
     * 挖掘疲劳：可以累加。每累计X点，则在X回合内，攻击伤害-25%。" 于是 {@link #weak} 腾出来做
     * "每层 -1 伤害"，这个新字段接过"按回合递减的 -25%"。</p>
     */
    private int miningFatigue;
    private int vulnerable;
    private int dexterity;
    private int slow;
    private int burn;
    /**
     * 凋零（v0.4.6 重定义）：**可以累加**；累计层数大于当前生命值时，回合结束后立刻死亡。
     *
     * <p>需求原文："凋零：可以累加。当对应角色/敌人累计的中毒大于当前生命值，回合结束后会立刻杀死该敌人。"
     * （这里的"中毒"是需求沿用了旧称呼 —— 也就是 v0.4.6 之前 {@code poison} 的那套规则。）</p>
     */
    private int decay;
    /**
     * 中毒（v0.4.6 重定义，就是 v0.4.2 的"涂毒"）：出牌环节结束时受到等同层数的伤害，
     * 每个新回合 -1 层。
     */
    private int poison;
    private int platedArmor;
    /** 活力（v0.0.3）：下一张攻击牌额外增加等于层数的伤害，打完清零。 */
    private int vigor;

    /**
     * 再生（v0.4.6）：自己回合开始时恢复等于层数的生命。
     *
     * <p>需求原文："再生：在回合开始时，恢复等同于层数的生命。" —— 按字面意思**不递减**。</p>
     */
    private int regeneration;
    /**
     * 缓降（v0.4.6）：免疫**下一次**受到的伤害，触发后层数 -1。
     *
     * <p>需求原文："缓降：免疫下一次受到的伤害。" 只挡"伤害"（走 {@code dealDamage} / 固定伤害），
     * 不挡"失去生命值"（那是另一套语义）。</p>
     */
    private int slowFall;

    /**
     * 临时的力量（v0.4.6）：【力量药水】给的力量会在**下个回合开始时**收回。
     */
    private int temporaryStrength;
    /**
     * 【不祥之瓶】（v0.4.6；v0.4.6.1 起叠的是【凋零】）：本场战斗里每回合开始时给自己叠 5 层凋零。
     */
    private boolean ominous;
    /**
     * 每回合**少抽几张牌**（v0.4.6）：遗物【嚒咕菲力】用。
     *
     * <p>需求原文："获得 14 点最大生命值，每回合少抽 1 张牌。"</p>
     */
    private int drawPenalty;
    /** 充能球（v0.0.3 W4；v0.3.0 起是"类型 + 充能"的实例）：栏位内容与栏位数。 */
    private final List<OrbInstance> orbs = new ArrayList<>();
    private int orbSlots = 3;
    /**
     * **蓄能**（v0.4.6.7，闪电苦力怕的专属状态）。
     *
     * <p>需求原文："此怪物的特殊状态：蓄能（使用蓄风药水效果图标）。描述：在死亡前，
     * 对玩家造成等同于蓄能层数的伤害。" 于是它是一本"死亡时一次性结账"的伤害会计：
     * 锁血结束的那一回合把全部层数打给玩家，然后自己死亡。</p>
     */
    private int charge;
    /**
     * **锁血**（v0.4.6.7，闪电苦力怕）。
     *
     * <p>需求原文："当此怪物的血量即将低于 1：进入锁血状态，无法被击杀，血量显示为『晕眩』，
     * 而不再是数字。" 所以 {@code hpLock} 表示"这只怪带锁血机制"，
     * {@code hpLocked} 表示"它现在正处于锁血中"。</p>
     */
    private boolean hpLock;
    private boolean hpLocked;
    /** 下回合开始时获得的格挡（v0.0.3 W3，"寻找掩体"用）。 */
    private int nextTurnBlock;
    /**
     * 表现层用的实体编号（v0.3.0）。
     *
     * <p>战斗引擎是纯逻辑的，但"谁该做攻击动画"要落到真实实体上，所以 Minecraft 层会把
     * {@code entity.getId()} 记在这里；引擎只负责把它转发给表现层，自己不认识 Minecraft。</p>
     */
    private int fxId = -1;
    /** 敌人**阵营**（v0.4.1）；玩家为 {@code null}。 */
    private Faction faction;
    /** 人工造物的"免疫第一次负面效果"是否还没用掉。 */
    private boolean debuffShield;
    /** 【袭击号角】：对灾厄部队 +10% 伤害（由遗物在开战时点亮）。 */
    private boolean raidHorn;
    /** 【失活的珊瑚】：每次受到的伤害 -1（由遗物在开战时点亮）。 */
    private boolean deadCoral;
    /** 【海洋之心】：雨天/雷暴时受到的伤害 -10%（由遗物在开战时点亮）。 */
    private boolean heartOfTheSea;
    /** 当前天气是不是雨天/雷暴（Minecraft 层每 tick 同步；离线自检直接设）。 */
    private boolean stormy;
    /** 是不是亡灵（亡灵杀手附魔的判定条件；由 Minecraft 层按原版 sensitive_to_smite 标签设置）。 */
    private boolean undead;

    protected Combatant(String name, int maxHp) {
        this(name, maxHp, maxHp);
    }

    protected Combatant(String name, int maxHp, int currentHp) {
        this.name = name;
        this.maxHp = Math.max(1, maxHp);
        this.hp = Math.max(0, Math.min(this.maxHp, currentHp));
    }

    public final String name() {
        return name;
    }

    /**
     * 记录"这次伤害是谁造成的"（v0.0.2 测试反馈修正：击杀归属）。
     *
     * <p>引擎在 {@link CombatContext#dealDamage} 里对**目标**调用它。默认什么都不做；
     * {@code MobCombatant} 用它记住"最后打这只怪的玩家"，因为引擎处决时原版击杀归属是空的
     * （{@code mob.kill()} 走的是无来源伤害），奖励系统必须靠这份信息才能记账。</p>
     */
    public void recordDamageFrom(Combatant source) {
    }

    public final int maxHp() {
        return maxHp;
    }

    public final int hp() {
        return hp;
    }

    public final int block() {
        return block;
    }

    public final int strength() {
        return strength;
    }

    public final int weak() {
        return weak;
    }

    /** 挖掘疲劳层数（v0.5.1.4）：> 0 时攻击伤害 -25%，每回合 -1 层。 */
    public final int miningFatigue() {
        return miningFatigue;
    }

    public final int vulnerable() {
        return vulnerable;
    }

    /** 活力（v0.0.3）：下一张攻击牌额外增加等于层数的伤害，打完清零。 */
    public final int vigor() {
        return vigor;
    }

    public final int dexterity() {
        return dexterity;
    }

    public final int slow() {
        return slow;
    }

    public final int burn() {
        return burn;
    }

    /** 凋零层数（v0.4.6：累计大于当前生命值时回合结束即死）。 */
    public final int decay() {
        return decay;
    }

    /** 中毒层数（v0.4.6：出牌环节结束时按层数受伤，每回合 -1 层）。 */
    public final int poison() {
        return poison;
    }

    public final int platedArmor() {
        return platedArmor;
    }

    /** 临时力量（还没被收回的部分）。 */
    public final int temporaryStrength() {
        return temporaryStrength;
    }

    /** 获得"临时力量"：立刻 +amount，并在下个回合开始时收回。 */
    public final void addTemporaryStrength(int amount) {
        if (amount > 0) {
            strength += amount;
            temporaryStrength += amount;
        }
    }

    /** 收回全部临时力量（回合开始时调用），返回实际收回的点数。 */
    public final int takeTemporaryStrength() {
        int value = temporaryStrength;
        temporaryStrength = 0;
        if (value > 0) {
            strength = Math.max(0, strength - value);
        }
        return value;
    }

    /** 是否带着【不祥之瓶】的"每回合叠凋零"标记（v0.4.6）。 */
    public final boolean ominous() {
        return ominous;
    }

    public final void setOminous(boolean value) {
        this.ominous = value;
    }

    /**
     * 清空**状态效果**（v0.4.6 的【奶桶】）：力量/虚弱/易伤/灼烧/中毒/凋零/再生/缓降等全部归零，
     * 但**不动充能球**（球是角色的资源，不是状态）。
     */
    public final void clearStatusEffects() {
        strength = 0;
        weak = 0;
        miningFatigue = 0;
        vulnerable = 0;
        dexterity = 0;
        slow = 0;
        burn = 0;
        decay = 0;
        poison = 0;
        platedArmor = 0;
        platedArmorLocked = false;
        vigor = 0;
        regeneration = 0;
        slowFall = 0;
        thorns = 0;
        temporaryStrength = 0;
        ominous = false;
        nextTurnBlock = 0;
    }

    /** 每回合少抽几张牌（v0.4.6）。 */
    public final int drawPenalty() {
        return drawPenalty;
    }

    public final void setDrawPenalty(int penalty) {
        this.drawPenalty = Math.max(0, penalty);
    }

    /** 再生层数（v0.4.6）。 */
    public final int regeneration() {
        return regeneration;
    }

    /** 缓降层数（v0.4.6）：还能免疫几次伤害。 */
    public final int slowFall() {
        return slowFall;
    }


    /** 增加再生层数（负面效果免疫不挡这个正面状态）。 */
    public final void addRegeneration(int amount) {
        regeneration = Math.max(0, regeneration + amount);
    }

    /** 增加缓降层数（正面状态）。 */
    public final void addSlowFall(int amount) {
        if (amount > 0) {
            slowFall += amount;
        }
    }


    /**
     * 尝试用掉一层【缓降】来完全免疫这次伤害。
     *
     * @return true = 这次伤害被免疫（调用方应该直接 return，不要扣血）
     */
    public final boolean absorbWithSlowFall() {
        if (slowFall <= 0) {
            return false;
        }
        slowFall--;
        return true;
    }

    // ------------------------------------------------------------------ 蓄能 / 锁血（v0.4.6.7，闪电苦力怕）

    /** 当前蓄能层数（死亡时一次性打给玩家的伤害会计）。 */
    public final int charge() {
        return charge;
    }

    /** 增加蓄能层数。 */
    public final void addCharge(int amount) {
        if (amount > 0) {
            charge += amount;
        }
    }

    /** 清空蓄能（结算之后）。 */
    public final void clearCharge() {
        charge = 0;
    }

    /**
     * 蓄能爆发还要**等几个敌人回合**才结算（v0.5.0.8）。
     *
     * <p>需求（用户给的例子）："玩家在第 8 回合击晕了旋风人，则旋风人应该在第九回合出牌回合结束后
     * 结算蓄能状态。"</p>
     *
     * <p>时序：玩家在自己出牌阶段把它打到血线以下 → 它进入【晕眩】并把意图换成「自爆 X」→
     * 该玩家结束回合后的敌人阶段**不结算**（这里把计数从 1 减到 0，表现为"这一手没爆"）→
     * 玩家第 9 回合照常出牌 → 第 9 回合结束后的敌人阶段才真正引爆。</p>
     */
    private int chargeDelay;

    /** 还要等几个敌人回合才引爆。 */
    public final int chargeDelay() {
        return chargeDelay;
    }

    /** 安排"再等 {@code turns} 个敌人回合"才引爆。 */
    public final void setChargeDelay(int turns) {
        chargeDelay = Math.max(0, turns);
    }

    /** 消耗一次等待；返回 true = 这次仍然不结算（还在等）。 */
    public final boolean consumeChargeDelay() {
        if (chargeDelay <= 0) {
            return false;
        }
        chargeDelay--;
        return true;
    }

    /**
     * 「弩箭射击」的额外伤害（v0.4.7.0 弓弦强化）：每层让弩箭射击多打 1 点。
     */
    private int boltBonus;

    /**
     * 「小刀」（Shiv）的额外伤害（v0.5.2.4 A4-11【精准】）：每层让小刀多打 1 点。
     */
    private int shivBonus;

    /**
     * 【荆棘】（v0.5.2.5）：可以累加、不衰减；**被攻击时**对攻击者反弹等同于层数的伤害。
     */
    private int thorns;

    /**
     * 【荧光墨囊】（v0.5.3.0）：回合结束时**不重置**剩余费用，可以累加到下一回合
     * （上限 = 每回合费用的 2 倍，在 {@code CombatContext.beginPlayerTurn} 里夹）。
     */
    private boolean energyCarryover;

    /**
     * 【龙蛋】（v0.5.3.0）：**每个敌人回合**受到的伤害总和不会大于这个数（0 = 没这个遗物）。
     */
    private int damageCap;

    /** 本回合（敌人回合）已经受到的伤害（配合 {@link #damageCap} 用）。 */
    private int damageTakenThisTurn;

    /**
     * 【商队护送】（v0.5.3.1）：本回合格挡**有没有被打穿**（也就是真的掉了血）。
     *
     * <p>注意它和 {@link #damageTakenThisTurn} **不是**一回事：那个只在【龙蛋】给额度时记账，
     * 而且记的是"受到的伤害"（含被格挡的部分）；这里只关心"有没有掉血"。
     * 每次敌人回合开始时清零（见 {@code CombatContext.runEnemyTurn}）。</p>
     */
    private boolean escortBreached;

    /**
     * 【尸体爆炸】（v0.5.4.1 A4-20）：这只怪身上带着"死亡时爆炸"的标记
     * （伤害 = 它自己的最大生命值）。
     *
     * <p>标记记在**怪物身上**而不是玩家身上：同一只怪被两张【尸体爆炸】打中只会炸一次
     * （与原版一致 —— 原版也是给敌人挂一个 corpse explosion 状态）。</p>
     */
    private boolean corpseExplosion;

    /** 弩箭射击的额外伤害层数。 */
    /**
     * 「爪击」的额外伤害（v0.4.8.2）：本场战斗中每打出一张爪击，所有爪击多打 2 点。
     */
    private int clawBonus;

    /** 本场战斗生成过的冰球数（v0.4.8.4 暴风雪用）。 */
    private int frostCount;

    /** 本场生成过的冰球数。 */
    /**
     * 集中 / Focus (v0.4.8.6, renamed in v0.5.0.4).
     *
     * <p>Requirement (v0.5.0.4): "所有充能球的效果增加 X 点（X 为层数，等离子充能球不受影响）",
     * i.e. for every orb the focus amount is ADDED to its numbers:
     * Lightning passive/evoke damage, Frost passive/evoke block, Dark per-turn growth.
     * Plasma is deliberately excluded. The name shown to players is 集中 (was 专注).</p>
     */
    private int focus;

    /** 本场战斗中已经生成过的闪电球数量（v0.5.0.4 电流相生 用）。 */
    private int lightningCount;

    /** 电动力学（v0.4.9.1）：闪电球被动改为对所有敌人结算。 */
    private boolean lightningHitsAll;

    /** 增幅器（v0.4.9.4）：每回合首次激发额外触发一次。 */
    private boolean evokeBonusOnce;
    private boolean evokeUsedThisTurn;

    /** 静电放电（v0.4.9.5）：每次受到攻击伤害时生成这么多颗闪电球。 */
    private int staticDischarge;
    /** 创造力（v0.4.9.5）：每回合开始时随机给 1 张能力牌。 */
    private boolean creativeAI;
    /** 循环（v0.4.9.5）：每回合开始时触发充能球被动的次数。 */
    private int loopCount;

    /** 破灭（v0.5.0.3）：失去生命时获得这么多点力量。 */
    private int rupture;
    /** 壁垒（v0.5.0.3）：格挡不再在回合开始时消失。 */
    private boolean barricade;

    public final int rupture() {
        return rupture;
    }

    public final void addRupture(int amount) {
        rupture += amount;
    }

    public final boolean barricade() {
        return barricade;
    }

    public final void setBarricade(boolean value) {
        this.barricade = value;
    }

    /**
     * 【模糊】（A4-15）：接下来**几次"自己的回合开始"不清空格挡**（0 = 正常清空）。
     *
     * <p>与【壁垒】的区别：壁垒是**永久**的，模糊只保一次（可叠加）。</p>
     */
    private int blurTurns;

    /**
     * 【幽魂形态】（A4-19，E15 无形）：还要保护几个敌人回合 —— 期间受到的伤害**一律变成 1**。
     *
     * <p>在自己每个回合开始时 -1（与原版一致：2 层 = 保两个敌人回合）。</p>
     */
    private int intangibleTurns;

    /** 还要保护几个敌人回合。 */
    public final int intangibleTurns() {
        return intangibleTurns;
    }

    /** 获得 N 个回合的【无形】。 */
    public final void addIntangibleTurns(int amount) {
        if (amount > 0) {
            intangibleTurns += amount;
        }
    }

    /** 自己回合开始时递减（返回递减之后还剩几层）。 */
    public final int decayIntangible() {
        if (intangibleTurns > 0) {
            intangibleTurns--;
        }
        return intangibleTurns;
    }

    /** 还要保留几次格挡。 */
    public final int blurTurns() {
        return blurTurns;
    }

    /** 加一次"下个回合开始不清空格挡"。 */
    public final void addBlurTurns(int amount) {
        if (amount > 0) {
            blurTurns += amount;
        }
    }

    /** 用掉一次（回合开始时调用）。 */
    public final void consumeBlurTurn() {
        if (blurTurns > 0) {
            blurTurns--;
        }
    }
    public final int staticDischarge() {
        return staticDischarge;
    }

    public final void addStaticDischarge(int amount) {
        staticDischarge += amount;
    }

    public final boolean creativeAI() {
        return creativeAI;
    }

    public final void setCreativeAI(boolean value) {
        this.creativeAI = value;
    }

    public final int loopCount() {
        return loopCount;
    }

    public final void addLoopCount(int amount) {
        loopCount += amount;
    }
    public final boolean evokeBonusOnce() {
        return evokeBonusOnce;
    }

    public final void setEvokeBonusOnce(boolean value) {
        this.evokeBonusOnce = value;
    }

    public final boolean evokeUsedThisTurn() {
        return evokeUsedThisTurn;
    }

    public final void markEvokeUsedThisTurn() {
        this.evokeUsedThisTurn = true;
    }

    public final void resetEvokeUsedThisTurn() {
        this.evokeUsedThisTurn = false;
    }
    public final boolean lightningHitsAll() {
        return lightningHitsAll;
    }

    public final void setLightningHitsAll(boolean value) {
        this.lightningHitsAll = value;
    }
    public final int focus() {
        return focus;
    }

    /** 增加（或减少，传负数）集中。 */
    public final void addFocus(int amount) {
        focus += amount;
    }
    public final int frostCount() {
        return frostCount;
    }

    /** 生成一颗冰球时记账。 */
    public final void addFrostCount(int amount) {
        if (amount > 0) {
            frostCount += amount;
        }
    }

    /** 本场战斗中已经生成过的闪电球总数（电流相生 的 X）。 */
    public final int lightningCount() {
        return lightningCount;
    }

    /** 生成一颗闪电球时记账（只计成功生成的那一次）。 */
    public final void addLightningCount(int amount) {
        if (amount > 0) {
            lightningCount += amount;
        }
    }
    public final int clawBonus() {
        return clawBonus;
    }

    public final void addClawBonus(int amount) {
        if (amount > 0) {
            clawBonus += amount;
        }
    }
    public final int boltBonus() {
        return boltBonus;
    }

    /** 增加弩箭射击的额外伤害。 */
    public final void addBoltBonus(int amount) {
        if (amount > 0) {
            boltBonus += amount;
        }
    }

    /** 「小刀」的额外伤害层数（v0.5.2.4【精准】）。 */
    public final int shivBonus() {
        return shivBonus;
    }

    /** 增加小刀的额外伤害。 */
    public final void addShivBonus(int amount) {
        if (amount > 0) {
            shivBonus += amount;
        }
    }

    /** 【荆棘】层数（v0.5.2.5）：被攻击时反弹这么多伤害。 */
    public final int thorns() {
        return thorns;
    }

    /** 增加【荆棘】层数（可累加；负面效果免疫不挡这个正面状态）。 */
    public final void addThorns(int amount) {
        if (amount > 0) {
            thorns += amount;
        }
    }

    /** 直接把【荆棘】层数设成 {@code value}（收回临时荆棘时用，负数按 0 处理）。 */
    public final void setThorns(int value) {
        thorns = Math.max(0, value);
    }

    /**
     * 【火焰屏障】（A4-12）：**只这一回合有效**的荆棘层数。
     *
     * <p>和永久荆棘（【脏玻璃】遗物 / 敌人特性）共用同一份 {@code thorns} 数值，
     * 所以反弹逻辑一行都不用改；这里只额外记下"其中有多少是临时加的"，
     * 好在回合结束时把它们收回来（{@code CombatContext.onTurnEndForPowers}）。</p>
     */
    private int temporaryThorns;

    /** 这一回合临时加上的荆棘层数（0 = 没有）。 */
    public final int temporaryThorns() {
        return temporaryThorns;
    }

    /** 加一层"本回合有效"的荆棘（同时真的加进 {@link #thorns()}）。 */
    public final void addTemporaryThorns(int amount) {
        if (amount > 0) {
            temporaryThorns += amount;
            thorns += amount;
        }
    }

    /** 收回全部临时荆棘（回合结束时调用）。 */
    public final void clearTemporaryThorns() {
        if (temporaryThorns > 0) {
            thorns = Math.max(0, thorns - temporaryThorns);
            temporaryThorns = 0;
        }
    }

    /** 【荧光墨囊】：剩余费用是否跨回合累加。 */
    public final boolean energyCarryover() {
        return energyCarryover;
    }

    public final void setEnergyCarryover(boolean value) {
        this.energyCarryover = value;
    }

    /** 【龙蛋】：每个敌人回合的受伤上限（0 = 没有上限）。 */
    public final int damageCap() {
        return damageCap;
    }

    public final void setDamageCap(int value) {
        this.damageCap = Math.max(0, value);
    }

    /** 本回合已经受到的伤害（敌人回合开始时清零）。 */
    public final int damageTakenThisTurn() {
        return damageTakenThisTurn;
    }

    public final void addDamageTakenThisTurn(int amount) {
        if (amount > 0) {
            damageTakenThisTurn += amount;
        }
    }

    public final void resetDamageTakenThisTurn() {
        damageTakenThisTurn = 0;
    }

    /** 【商队护送】本回合是否被打穿格挡（真的掉了血）。 */
    public final boolean escortBreached() {
        return escortBreached;
    }

    /** 【商队护送】记下"这一轮格挡被打穿了"。 */
    public final void markEscortBreached() {
        this.escortBreached = true;
    }

    /** 【商队护送】每次敌人回合开始时清零。 */
    public final void resetEscortBreached() {
        this.escortBreached = false;
    }

    /** 【尸体爆炸】这只怪身上有没有"死亡时爆炸"的标记。 */
    public final boolean corpseExplosion() {
        return corpseExplosion;
    }

    /** 【尸体爆炸】给这只怪挂上标记。 */
    public final void markCorpseExplosion() {
        this.corpseExplosion = true;
    }

    /** 【尸体爆炸】爆炸结算完之后把标记摘掉（避免同一只怪炸第二次）。 */
    public final void clearCorpseExplosion() {
        this.corpseExplosion = false;
    }

    /** 这只怪是否带"锁血"机制（由它的意图状态机档案决定）。 */
    public final boolean hpLock() {
        return hpLock;
    }

    /** 设置"带锁血机制"（{@code MobIntents.Profile#hpLock}）。 */
    public final void setHpLock(boolean value) {
        this.hpLock = value;
    }

    /** 现在是否正处于锁血状态（血量显示为"晕眩"）。 */
    public final boolean hpLocked() {
        return hpLocked;
    }

    /** 进入 / 解除锁血状态。 */
    public final void setHpLocked(boolean value) {
        this.hpLocked = value;
    }

    /** 亡灵（用于"亡灵杀手"附魔）。 */
    public final boolean undead() {
        return undead;
    }
    public final void setUndead(boolean undead) {
        this.undead = undead;
    }

    public final boolean alive() {
        return hp > 0;
    }

    // ------------------------------------------------------------------ 生命与格挡

    /**
     * Sets HP, clamped into {@code [0, maxHp]}, and reports the change to the adapter.
     *
     * <p><b>v0.5.1.0：锁血状态下血量有下限 1。</b>以前"锁血"只在**触发那一次**改写伤害
     * （见 {@code CombatContext.dealDamage} 里 {@code !lockable.hpLocked()} 的判断），
     * 于是第二次攻击会绕过它直接把怪打死 —— 用户报的"旋风人晕眩后没有锁血、受击即死"
     * 就是这个。现在把不变量放在**血量本身**：只要 {@link #hpLocked()}，HP 就夹到 ≥ 1，
     * 无论有多少次攻击都杀不死它，直到自爆流程自己 {@code setHpLocked(false)} + {@code setHp(0)}。</p>
     */
    public final void setHp(int newHp) {
        int clamped = Math.max(0, Math.min(maxHp, newHp));
        // 锁血中：血量下限 1（唯一的解除方式是自爆流程显式清掉锁血标记）
        if (hpLocked && clamped < 1) {
            clamped = 1;
        }
        if (clamped == hp) {
            return;
        }
        int previous = hp;
        hp = clamped;
        onHpChanged(previous, hp);
        if (hp == 0) {
            onDefeated();
        }
    }

    public final void loseHp(int amount) {
        if (amount > 0) {
            setHp(hp - amount);
        }
    }

    public final void heal(int amount) {
        if (amount > 0) {
            setHp(hp + amount);
        }
    }

    /**
     * 提升**最大生命值**，并同时回复等量生命（v0.5.2.2 A4-9【进食】）。
     *
     * <p>原版的"提升最大生命值"是上限与当前值一起涨的，所以这里一起处理。
     * 上限只增不减：{@code amount <= 0} 直接忽略。</p>
     */
    public final void addMaxHp(int amount) {
        if (amount <= 0) {
            return;
        }
        maxHp += amount;
        setHp(hp + amount);
    }

    /**
     * 直接获得格挡（**原始值**）。
     *
     * <p>注意：战斗里所有"获得格挡"都应该走 {@link CombatContext#previewBlock(Combatant, int)}，
     * 这样迅捷/缓慢才会生效；直接调用本方法会绕过那两个状态。</p>
     */
    public final void gainBlock(int amount) {
        if (amount > 0) {
            block += amount;
        }
    }

    public final void clearBlock() {
        block = 0;
    }

    /** Consumes as much incoming damage as this combatant has block, returning the absorbed part. */
    public final int absorbWithBlock(int incoming) {
        int absorbed = Math.min(block, Math.max(0, incoming));
        block -= absorbed;
        return absorbed;
    }

    // ------------------------------------------------------------------ 状态增减

    /**
     * 获得**力量**（v0.5.1.4：先与【虚弱】互相抵消）。
     *
     * <p>需求原文："虚弱与力量可以相互抵消。例如，对已经有力量三的敌人施加虚弱2，则结算后，
     * 敌人应该有力量1。" 反向同理：给带着虚弱的角色加力量，先把虚弱抵掉。</p>
     *
     * <p>传入负数（例如"失去 2 点力量"）仍然只是单纯地把力量夹到 ≥ 0，**不会**转成虚弱
     * ——"失去力量"和"被施加虚弱"是两回事。</p>
     */
    public final void addStrength(int amount) {
        if (amount <= 0) {
            strength = Math.max(0, strength + amount);
            return;
        }
        int cancel = Math.min(amount, weak);
        weak -= cancel;
        strength += amount - cancel;
    }

    /**
     * 施加**虚弱**（v0.5.1.4 重做）：每层攻击伤害 −1、可以累加、不随回合衰减；与力量互相抵消。
     */
    public final void addWeak(int amount) {
        if (amount <= 0 || immuneToFirstDebuffNow()) {
            return;
        }
        int cancel = Math.min(amount, strength);
        strength -= cancel;
        weak += amount - cancel;
    }

    /**
     * 施加**挖掘疲劳**（v0.5.1.4）：层数 = 剩余回合数，期间攻击伤害 −25%（可累加）。
     *
     * <p>这就是从旧【虚弱】搬过来的那套机制，生物"给予虚弱"的意图现在全部走这里。</p>
     */
    public final void addMiningFatigue(int turns) {
        if (turns > 0 && !immuneToFirstDebuffNow()) {
            miningFatigue += turns;
        }
    }

    public final void addVulnerable(int turns) {
        if (turns > 0 && !immuneToFirstDebuffNow()) {
            vulnerable += turns;
        }
    }

    /**
     * 获得**敏捷**（v0.5.1.4：先与【缓慢】互相抵消）。
     *
     * <p>需求原文："敏捷和缓慢也可以相互抵消。原理和虚弱与力量的原理一致。"</p>
     */
    public final void addDexterity(int amount) {
        if (amount <= 0) {
            dexterity = Math.max(0, dexterity + amount);
            return;
        }
        int cancel = Math.min(amount, slow);
        slow -= cancel;
        dexterity += amount - cancel;
    }

    /**
     * 施加**缓慢**（v0.5.1.4：先与【敏捷】互相抵消）。
     */
    public final void addSlow(int amount) {
        if (amount <= 0) {
            slow = Math.max(0, slow + amount);
            return;
        }
        if (immuneToFirstDebuffNow()) {
            return;
        }
        int cancel = Math.min(amount, dexterity);
        dexterity -= cancel;
        slow += amount - cancel;
    }

    public final void addBurn(int amount) {
        // v0.4.1：地狱生物免疫灼烧（不是"免疫一次"，而是完全免疫）
        if (amount > 0 && (immuneToBurn() || immuneToFirstDebuffNow())) {
            return;
        }
        burn = Math.max(0, burn + amount);
    }



    /**
     * 人工造物的"免疫第一次负面效果"：被负面效果命中时消耗掉护盾。
     *
     * <p>返回 true 表示这次负面效果被免疫（调用方可以直接 return）。</p>
     */
    private boolean immuneToFirstDebuffNow() {
        return debuffShield && consumeDebuffShield();
    }

    /** 施加**凋零**（v0.4.6：可以累加，累计大于当前生命值时回合结束即死）。 */
    public final void addDecay(int amount) {
        if (amount > 0 && !immuneToFirstDebuffNow()) {
            decay += amount;
        }
    }

    public final void addPlatedArmor(int amount) {
        platedArmor = Math.max(0, platedArmor + amount);
    }

    /**
     * 附甲是否**被锁住**（v0.5.1.4，【村庄英雄】："回合结束时，你的附甲层数不会再降低"）。
     *
     * <p>没锁住时附甲按常规规则衰减（见 {@link #decayPlatedArmor()}）。</p>
     */
    private boolean platedArmorLocked;

    public final boolean platedArmorLocked() {
        return platedArmorLocked;
    }

    public final void setPlatedArmorLocked(boolean value) {
        this.platedArmorLocked = value;
    }

    /**
     * **玩家回合开始时**的附甲衰减（v0.5.1.4 新规则）。
     *
     * <p>用户拍板："附甲在常规状态下会在每个玩家回合开始时减少 1。" —— 于是【神龟】不再是一叠到底：
     * 每回合先掉 1 层，再按剩下的层数在回合结束时给格挡。【村庄英雄】开着时这条衰减被跳过。</p>
     *
     * @return 实际掉了几层（0 = 没掉）
     */
    public final int decayPlatedArmor() {
        if (platedArmorLocked || platedArmor <= 0) {
            return 0;
        }
        platedArmor--;
        return 1;
    }

    /**
     * 施加**中毒**（v0.4.6 的旧"涂毒"）：出牌环节结束时按层数受到伤害，每回合 -1 层。
     *
     * <p>负值只能用来清空。</p>
     */
    public final void addPoison(int amount) {
        if (amount > 0 && !immuneToFirstDebuffNow()) {
            poison += amount;
        } else if (amount < 0) {
            poison = Math.max(0, poison + amount);
        }
    }

    // ------------------------------------------------------------------ 充能球与"下回合格挡"（v0.0.3 W3/W4）

    /** 充能球栏位内容（顺序 = 从左到右，最前面那颗会被优先激发）。 */
    public final List<OrbInstance> orbs() {
        return List.copyOf(orbs);
    }

    public final int orbSlots() {
        return orbSlots;
    }

    public final void setOrbSlots(int slots) {
        this.orbSlots = Math.max(0, slots);
    }

    public final boolean orbsFull() {
        return orbs.size() >= orbSlots;
    }

    /** 生成一颗球（自动带上初始充能）；**没有充能球栏位的人拿不到球**（v0.4.0）。 */
    public final void addOrb(Orb orb) {
        if (orb != null) {
            addOrb(OrbInstance.fresh(orb));
        }
    }

    public final void addOrb(OrbInstance orb) {
        if (orb == null || orbSlots <= 0) {
            // v0.4.0：只有故障机器人有充能球栏位，其他人就算被塞了球也不该持有
            return;
        }
        orbs.add(orb);
    }

    /** 取走最前面的球（激发）。没有球时返回 {@code null}。 */
    public final OrbInstance removeFirstOrb() {
        return orbs.isEmpty() ? null : orbs.remove(0);
    }

    /**
     * 回合结束时的充能成长（暗球 +6，并计入集中）：直接改写栏位里的实例。
     *
     * <p>v0.5.0.4：把【集中】接进黑暗球的成长——需求"黑暗球：每回合成长的伤害增加 X"。</p>
     */
    public final void growOrbs() {
        for (int i = 0; i < orbs.size(); i++) {
            orbs.set(i, orbs.get(i).grown(focus));
        }
    }

    /** 数一数某种球有几颗（等离子球的"回合开始 +1 能量"用它）。 */
    public final int countOrbs(Orb type) {
        int count = 0;
        for (OrbInstance orb : orbs) {
            if (orb.type() == type) {
                count++;
            }
        }
        return count;
    }

    public final void clearOrbs() {
        orbs.clear();
    }

    /** 表现层用的实体编号；{@code -1} = 没有绑定实体（纯自检/离线用）。 */
    public final int fxId() {
        return fxId;
    }

    public final void setFxId(int fxId) {
        this.fxId = fxId;
    }

    // ------------------------------------------------------------------ 阵营与特性（v0.4.1）

    /** 这个参战者的阵营；玩家为 {@code null}。 */
    public final Faction faction() {
        return faction;
    }

    public final void setFaction(Faction faction) {
        this.faction = faction;
        // 人工造物天生带着"免疫第一次负面效果"
        this.debuffShield = faction != null && faction.immuneToFirstDebuff();
    }

    /** 阵营名（没有阵营时返回空串）。 */
    public final String factionLabel() {
        return faction == null ? "" : faction.label();
    }

    /** 亡灵生物：免疫伤害药水。 */
    public final boolean immuneToDamagePotions() {
        return faction != null && faction.immuneToDamagePotions();
    }

    /** 地狱生物：免疫灼烧效果。 */
    public final boolean immuneToBurn() {
        return faction != null && faction.immuneToBurn();
    }

    /** 人工造物：免疫第一次被施加的负面效果（还没用掉时返回 true）。 */
    public final boolean hasDebuffShield() {
        return debuffShield;
    }

    /** 用掉一次"免疫负面效果"；本来就没有时返回 false。 */
    public final boolean consumeDebuffShield() {
        if (!debuffShield) {
            return false;
        }
        debuffShield = false;
        return true;
    }

    public final boolean raidHornActive() {
        return raidHorn;
    }

    public final void setRaidHorn(boolean raidHorn) {
        this.raidHorn = raidHorn;
    }

    public final boolean deadCoralActive() {
        return deadCoral;
    }

    public final void setDeadCoral(boolean deadCoral) {
        this.deadCoral = deadCoral;
    }

    public final boolean heartOfTheSeaActive() {
        return heartOfTheSea;
    }

    public final void setHeartOfTheSea(boolean heartOfTheSea) {
        this.heartOfTheSea = heartOfTheSea;
    }

    /** 当前是不是雨天/雷暴（【海洋之心】的判定条件）。 */
    public final boolean stormy() {
        return stormy;
    }

    public final void setStormy(boolean stormy) {
        this.stormy = stormy;
    }

    /** 下回合开始时获得的格挡（"寻找掩体"这类延迟效果）。 */
    public final void addNextTurnBlock(int amount) {
        if (amount > 0) {
            nextTurnBlock += amount;
        }
    }

    public final int nextTurnBlock() {
        return nextTurnBlock;
    }

    /** 取走并清零"下回合格挡"（回合开始时结算）。 */
    public final int takeNextTurnBlock() {
        int value = nextTurnBlock;
        nextTurnBlock = 0;
        return value;
    }

    /**
     * 【机动】【飞膝】的"下回合额外能量"（v0.5.3.5 A4-14）。
     *
     * <p>和"下回合格挡"同一套写法：本回合记账、下回合开始时取走并清零。
     * 记账放在 {@code Combatant} 上而不是 {@code PlayerState} 上，是为了与
     * {@code nextTurnBlock} 对称（两者都是"跨回合的状态"，而不是"这一场的旗标"）。</p>
     */
    private int nextTurnEnergy;
    /** 【捕食者】的"下回合额外抽牌"。 */
    private int nextTurnDraw;

    /** 下回合开始时额外获得的能量。 */
    public final void addNextTurnEnergy(int amount) {
        if (amount > 0) {
            nextTurnEnergy += amount;
        }
    }

    public final int nextTurnEnergy() {
        return nextTurnEnergy;
    }

    /** 取走并清零"下回合额外能量"（回合开始时结算）。 */
    public final int takeNextTurnEnergy() {
        int value = nextTurnEnergy;
        nextTurnEnergy = 0;
        return value;
    }

    /** 下回合开始时额外抽几张牌。 */
    public final void addNextTurnDraw(int amount) {
        if (amount > 0) {
            nextTurnDraw += amount;
        }
    }

    public final int nextTurnDraw() {
        return nextTurnDraw;
    }

    /** 取走并清零"下回合额外抽牌"（回合开始时结算）。 */
    public final int takeNextTurnDraw() {
        int value = nextTurnDraw;
        nextTurnDraw = 0;
        return value;
    }

    /** 获得活力（可累加）。 */
    public final void addVigor(int amount) {
        if (amount > 0) {
            vigor += amount;
        }
    }

    /** 消耗掉全部活力（下一张攻击牌打出时调用）。 */
    public final void clearVigor() {
        vigor = 0;
    }

    /**
     * 回合结束时衰减的状态。
     *
     * <p>v0.5.1.4：**衰减的是【挖掘疲劳】而不是【虚弱】** —— 虚弱现在"每层 -1 伤害、可以累加、
     * 不随回合消失"（只能靠力量抵消），而"-25% 持续 X 回合"那套搬到了挖掘疲劳身上。
     * 易伤与中毒照旧按回合递减。</p>
     */
    public final void decayStatuses() {
        if (miningFatigue > 0) {
            miningFatigue--;
        }
        if (vulnerable > 0) {
            vulnerable--;
        }
        // v0.4.2 的"涂毒"在 v0.4.6 更名为【中毒】，仍然每过一个回合 -1 层
        if (poison > 0) {
            poison--;
        }
    }

    /** 清空全部状态（调试指令与"战斗结束清理"用）。 */
    public final void clearStatuses() {
        strength = 0;
        weak = 0;
        miningFatigue = 0;
        vulnerable = 0;
        dexterity = 0;
        slow = 0;
        burn = 0;
        decay = 0;
        poison = 0;
        platedArmor = 0;
        platedArmorLocked = false;
        vigor = 0;
        nextTurnBlock = 0;
        // v0.4.6：药水带来的新状态
        regeneration = 0;
        slowFall = 0;
        thorns = 0;
        drawPenalty = 0;
        temporaryStrength = 0;
        ominous = false;
        orbs.clear();
    }

    /**
     * True when the combatant carries at least one status worth printing.
     *
     * <p>v0.5.0.4：把 {@code focus} 也算进来——否则"只有集中、没有别的状态"时
     * {@link #statusSuffix()} 会直接返回空串，玩家就看不到集中层数了。</p>
     */
    public final boolean hasStatuses() {
        return strength > 0 || weak > 0 || miningFatigue > 0 || vulnerable > 0 || dexterity > 0 || slow > 0
                || burn > 0 || decay > 0 || poison > 0 || platedArmor > 0 || vigor > 0
                || nextTurnBlock > 0 || regeneration > 0 || slowFall > 0
                || focus != 0
                || !orbs.isEmpty();
    }

    public final String statusSuffix() {
        if (!hasStatuses()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" [");
        boolean first = true;
        first = appendStatus(sb, first, "力量+", strength);
        first = appendStatus(sb, first, "虚弱", weak);
        first = appendStatus(sb, first, "挖掘疲劳", miningFatigue);
        first = appendStatus(sb, first, "易伤", vulnerable);
        first = appendStatus(sb, first, "活力", vigor);
        first = appendStatus(sb, first, "迅捷+", dexterity);
        first = appendStatus(sb, first, "缓慢", slow);
        first = appendStatus(sb, first, "灼烧", burn);
        first = appendStatus(sb, first, "中毒", poison);
        first = appendStatus(sb, first, "凋零", decay);
        first = appendStatus(sb, first, "再生", regeneration);
        first = appendStatus(sb, first, "缓降", slowFall);
        // 集中可以是负数（超能光束 / 重编程），所以不能过 appendStatus 的 value > 0 过滤
        if (focus != 0) {
            if (!first) {
                sb.append(' ');
            }
            sb.append("集中+").append(focus);
            first = false;
        }
        first = appendStatus(sb, first, "充能球", orbs.size());
        appendStatus(sb, first, "神龟", platedArmor);
        return sb.append(']').toString();
    }

    private static boolean appendStatus(StringBuilder sb, boolean first, String label, int value) {
        if (value <= 0) {
            return first;
        }
        if (!first) {
            sb.append(' ');
        }
        sb.append(label).append(value);
        return false;
    }

    /** @param previousHp HP before the change (never equal to {@code newHp}) */
    protected void onHpChanged(int previousHp, int newHp) {
    }

    /** Called once, when HP first reaches zero. */
    protected void onDefeated() {
    }

    @Override
    public String toString() {
        return name + " " + hp + "/" + maxHp + (block > 0 ? " (格挡 " + block + ")" : "");
    }
}
