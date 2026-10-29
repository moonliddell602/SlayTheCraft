package com.slaythecraft.combat.core;

/**
 * 遗物的触发点（v0.0.2 V7）。
 *
 * <p>刻意做成一个**固定的小集合**：以后加遗物只需要在 {@link RelicLibrary} 里登记，
 * 不用改引擎。每个触发点由 {@link CombatContext} 在对应时机统一广播给参战者身上所有遗物。</p>
 */
public enum RelicTrigger {
    /** 自己本场战斗的第一个回合开始（此时能量/格挡已重置、抽牌之前）。 */
    BATTLE_START("战斗开始"),
    /** 自己每个回合开始（能量/格挡已重置之后、持续伤害与抽牌之前）。 */
    TURN_START("回合开始"),
    /** 自己结束出牌时（手牌结算之后、敌人行动之前）。 */
    TURN_END("回合结束"),
    /** 自己打出一张牌并结算完之后。 */
    CARD_PLAYED("打出卡牌后"),
    /** 自己所在的队伍打赢了这场战斗。 */
    ON_VICTORY("战斗胜利"),
    /**
     * 本场战斗**第一次失去生命值**（v0.2.8）。
     *
     * <p>"第一次"由引擎判定（每个参战者每场战斗只广播一次），所以遗物本身仍然是无状态的。
     * 只有**真的掉了血**才会触发：被格挡完全吸收的伤害不算失去生命。</p>
     */
    FIRST_HP_LOST("本场战斗第一次失去生命");

    private final String label;

    RelicTrigger(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
