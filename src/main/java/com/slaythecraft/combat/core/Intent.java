package com.slaythecraft.combat.core;

/**
 * A telegraphed enemy action. {@code value} means damage, block, strength or status turns
 * depending on {@link #type()}; {@code times} is the hit count for multi-attacks.
 *
 * <p>{@code payload} 是 v0.0.3 W2 加的：状态牌意图要携带"塞哪张卡"（其余意图为空串）。</p>
 */
public record Intent(IntentType type, int value, int times, String label, String payload) {

    public static Intent attack(int damage) {
        return attack(1, damage);
    }

    public static Intent attack(int times, int damage) {
        String label = times > 1 ? "攻击 " + damage + "×" + times : "攻击 " + damage;
        return new Intent(IntentType.ATTACK, damage, Math.max(1, times), label, "");
    }

    /**
     * 状态牌意图（v0.0.3 W2）：把 {@code count} 张指定状态牌塞进目标玩家的**弃牌堆**。
     *
     * @param cardId 状态牌 id（{@code void} / {@code slimed} / {@code burn} / {@code dazed} /
     *               {@code venom} / {@code call}）
     */
    public static Intent statusCard(String cardId, int count) {
        Card card = CardLibrary.byId(cardId);
        String name = card == null ? cardId : card.name();
        return new Intent(IntentType.DEBUFF_STATUS_CARD, Math.max(1, count), 1,
                "塞入 " + Math.max(1, count) + " 张「" + name + "」", cardId);
    }

    public static Intent defend(int block) {
        return new Intent(IntentType.DEFEND, block, 1, "防御 " + block, "");
    }

    public static Intent strength(int amount) {
        return new Intent(IntentType.BUFF_STRENGTH, amount, 1, "蓄力 力量+" + amount, "");
    }

    /**
     * 对玩家施加**挖掘疲劳**（v0.5.1.4 起取代"给予虚弱"）。
     *
     * @param turns 层数 = 持续回合数，期间攻击伤害 -25%
     */
    public static Intent miningFatigue(int turns) {
        return new Intent(IntentType.DEBUFF_MINING_FATIGUE, turns, 1, "弱化 挖掘疲劳" + turns, "");
    }

    public static Intent vulnerable(int turns) {
        return new Intent(IntentType.DEBUFF_VULNERABLE, turns, 1, "破防 易伤" + turns, "");
    }

    /** 灼烧：目标下回合开始受到等同层数的伤害。 */
    public static Intent burn(int stacks) {
        return new Intent(IntentType.DEBUFF_BURN, stacks, 1, "灼烧 " + stacks, "");
    }

    /** 凋零：与灼烧同机制。 */
    public static Intent decay(int stacks) {
        return new Intent(IntentType.DEBUFF_DECAY, stacks, 1, "凋零 " + stacks, "");
    }

    /** 中毒：层数大于当前生命时回合结束立刻死亡。 */
    public static Intent poison(int stacks) {
        return new Intent(IntentType.DEBUFF_POISON, stacks, 1, "中毒 " + stacks, "");
    }

    /** 缓慢：每层使目标获得的格挡 -1。 */
    public static Intent slow(int stacks) {
        return new Intent(IntentType.DEBUFF_SLOW, stacks, 1, "缓慢 " + stacks, "");
    }

    /** v0.4.6.7：敌人自我治疗（女巫 恢复 8 点生命）。 */
    public static Intent heal(int amount) {
        return new Intent(IntentType.HEAL, Math.max(1, amount), 1, "恢复 " + Math.max(1, amount) + " 生命", "");
    }

    /** v0.4.6.7：蓄能（闪电苦力怕）—— 增加 {@code stacks} 层蓄能。 */
    public static Intent charge(int stacks) {
        return new Intent(IntentType.BUFF_CHARGE, Math.max(1, stacks), 1, "蓄能 " + Math.max(1, stacks), "");
    }

    /**
     * v0.4.6.7：召唤 {@code count} 只 {@code entityId} 加入当前战斗
     * （唤魔者"召唤恼鬼 ×2"）。
     */
    public static Intent summon(String entityId, int count) {
        int amount = Math.max(1, count);
        return new Intent(IntentType.SUMMON, amount, 1, "召唤 " + amount + " 只随从", entityId == null ? "" : entityId);
    }

    /**
     * v0.4.6.7：场上这种怪只剩 {@code atMost} 只时，再召唤 {@code count} 只
     * （掠夺者：场上只剩 1 只掠夺者时再叫一个）。
     */
    public static Intent summonIfFew(String entityId, int count, int atMost) {
        int amount = Math.max(1, count);
        int limit = Math.max(0, atMost);
        // times 存的是"阈值 + 1"：0 表示无条件召唤，所以阈值 0（场上归零）也能表达
        return new Intent(IntentType.SUMMON_IF_FEW, amount, limit + 1,
                "召唤 " + amount + " 只随从（场上同类不超过 " + limit + " 只时）",
                entityId == null ? "" : entityId);
    }

    /**
     * v0.4.6.7：晕眩 / 锁血结算（闪电苦力怕）。
     *
     * @param charge 结算时的蓄能层数（只用于文案，实际结算读的是敌人当前的蓄能）
     */
    public static Intent lock(int charge) {
        // v0.5.0.8 需求（用户例子）："在面板上显示的行动应该为：自爆 X（X 等同于蓄能的层数）"
        return new Intent(IntentType.LOCK, Math.max(0, charge), 1, "自爆 " + Math.max(0, charge), "");
    }

    public static Intent unknown() {
        return new Intent(IntentType.UNKNOWN, 0, 1, "未知", "");
    }

    /** The damage this intent would deal before strength / weak / vulnerable are applied. */
    public int totalRawDamage() {
        return type == IntentType.ATTACK ? value * times : 0;
    }
}
