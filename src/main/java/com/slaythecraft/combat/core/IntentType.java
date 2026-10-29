package com.slaythecraft.combat.core;

/** What an enemy is telegraphing for its next turn. */
public enum IntentType {
    ATTACK,
    DEFEND,
    /** Applies strength, which permanently raises the damage of its later attacks. */
    BUFF_STRENGTH,
    /**
     * 对玩家施加【挖掘疲劳】：每层让玩家在一回合内攻击伤害 -25%。
     *
     * <p>v0.5.1.4：这条意图原来是 {@code DEBUFF_WEAK}（"给予虚弱"）。需求原文："先前的虚弱机制改为：
     * 挖掘疲劳 …… 将先前存在『给予虚弱』的生物 这项意图全部改为 给予挖掘疲劳。"
     * 于是这里连枚举名一起换掉，编译器会把所有用到的地方都指出来。</p>
     */
    DEBUFF_MINING_FATIGUE,
    /** Applies vulnerable to the player: each stack lasts one turn and raises damage taken by 25%. */
    DEBUFF_VULNERABLE,
    /** 灼烧：目标下回合开始受到等同层数的伤害（无视格挡）。 */
    DEBUFF_BURN,
    /** 凋零：与灼烧同机制，只是来源不同。 */
    DEBUFF_DECAY,
    /** 中毒：层数大于当前生命时，回合结束立刻死亡。 */
    DEBUFF_POISON,
    /** 缓慢：每层使目标获得的格挡 -1。 */
    DEBUFF_SLOW,
    /** v0.0.3 W2：把状态牌塞进玩家的弃牌堆（{@code Intent#payload()} 是要塞的卡牌 id）。 */
    DEBUFF_STATUS_CARD,
    /**
     * v0.4.6.7：敌人**自我治疗**（女巫第四回合"恢复 8 点生命"）。
     *
     * <p>{@code value} = 回复量。</p>
     */
    HEAL,
    /**
     * v0.4.6.7：**蓄能**（闪电苦力怕的特殊状态）。
     *
     * <p>{@code value} = 增加层数。蓄能是"死亡时结算"的伤害会计：锁血结束的那一回合，
     * 把全部蓄能层数一次性打给玩家（见 {@link #LOCK}）。</p>
     */
    BUFF_CHARGE,
    /**
     * v0.4.6.7：**召唤**（唤魔者"召唤恼鬼 ×2 加入当前战斗"）。
     *
     * <p>{@code payload} = 要召唤的实体 id，{@code value} = 数量。</p>
     */
    SUMMON,
    /**
     * v0.4.6.7：**条件召唤**（掠夺者"或 召唤一个新的掠夺者"，条件是场上只剩 1 只）。
     *
     * <p>{@code payload} = 实体 id，{@code value} = 数量，{@code times} = 触发阈值
     * （场上这种怪的数量 **小于等于** 阈值时才召唤）。</p>
     */
    SUMMON_IF_FEW,
    /**
     * v0.4.6.7：**晕眩 / 锁血结算**（闪电苦力怕）。
     *
     * <p>敌人被打到"即将低于 1 点生命"时进入锁血：血量不再显示数字而显示"晕眩"，
     * 下一回合执行这个意图 —— 把蓄能层数全部打给玩家，然后自己死亡。</p>
     */
    LOCK,
    /** Idle turn, mostly used by non-hostile mobs. */
    UNKNOWN
}
