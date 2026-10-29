package com.slaythecraft.combat.core;

/**
 * 状态牌（<b>卡牌类型</b>，v0.0.3 W2）与**诅咒牌**（<b>新卡牌类型</b>，v0.4.6）的共同行为表。
 *
 * <p>状态牌需求："这些牌属于当前玩家的状态描述，一些不能被打出，另一些则会为玩家添加负面效果；
 * 这些牌通常由对局中的怪物添加（放入手牌/弃牌堆/出牌堆）。"</p>
 *
 * <p>诅咒需求："你是否会懊悔过去的行为？" —— 四种诅咒都是"出牌回合结束时如果还在手里就发作"，
 * 与状态牌的结算时机完全一致，所以放在同一张表里：</p>
 *
 * <ul>
 *   <li>{@link #unplayable(Card)}：不能被打出（虚空 / 灼烧 / 晕眩 / 伤口 + 残忍 / 贪婪 / 固执）；</li>
 *   <li>{@link #onDrawEnergyLoss(Card)}：抽到时本回合费用 −N（虚空）；</li>
 *   <li>{@link #endOfTurnDamage(Card)}：回合结束时还在手里 → 对自己造成 N 点伤害（可被格挡，灼烧 / 毒液）；</li>
 *   <li>{@link #endOfTurnHpLoss(Card)}：回合结束时还在手里 → 失去 N 点生命值（**无视格挡**，呼唤）；</li>
 *   <li>{@link #endOfTurnHpLossPerHandCard(Card)}：残忍 —— 失去等同**当前手牌数**的生命值；</li>
 *   <li>{@link #endOfTurnGoldLoss(Card)}：贪婪 —— 失去 10 金币（不会变成负数）；</li>
 *   <li>{@link #copiesItselfOnEndOfTurn(Card)}：戏谑 —— 把一张复制品放进弃牌堆；</li>
 *   <li>{@link #endOfTurnVulnerable(Card)}：固执 —— 获得 2 层易伤。</li>
 * </ul>
 */
public final class StatusCards {

    public static final String VOID = "void";
    /** 不屈（v0.4.7.8）：不能被打出，只在手里"挡死"。 */
    public static final String LAST_STAND = "last_stand";

    /** 这张牌是不是【不屈】（不能被打出）。 */
    public static boolean isLastStand(Card card) {
        return card != null && LAST_STAND.equals(card.id());
    }
    public static final String SLIMED = "slimed";
    public static final String BURN = "burn";
    public static final String DAZED = "dazed";
    public static final String VENOM = "venom";
    public static final String CALL = "call";
    /** 伤口（v0.5.2.1 A4-8）：不能被打出、没有任何效果，会一直留在手里。 */
    public static final String WOUND = "wound";

    // ---- v0.4.6 诅咒（新卡牌类型） ----
    /** 残忍：不能打出；回合结束还在手里 → 失去等同手牌数的生命。 */
    public static final String CURSE_CRUEL = "curse_cruel";
    /** 贪婪：不能打出；回合结束还在手里 → 失去 10 金币。 */
    public static final String CURSE_GREED = "curse_greed";
    /** 戏谑：1 费可打出；回合结束还在手里 → 复制一张进弃牌堆。 */
    public static final String CURSE_MOCKERY = "curse_mockery";
    /** 固执：不能打出；回合结束还在手里 → 获得 2 层易伤。 */
    public static final String CURSE_STUBBORN = "curse_stubborn";

    /** 灼烧 / 毒液回合结束时的自伤（可被格挡）。 */
    public static final int BURN_DAMAGE = 4;

    /** 呼唤回合结束时失去的生命值（无视格挡）。 */
    public static final int CALL_HP_LOSS = 5;

    /** 贪婪回合结束时失去的金币。 */
    public static final int GREED_GOLD_LOSS = 10;

    /** 固执回合结束时获得的易伤层数。 */
    public static final int STUBBORN_VULNERABLE = 2;

    private StatusCards() {
    }

    /** 这张牌是不是状态牌。 */
    public static boolean isStatusCard(Card card) {
        return card != null && card.type() == CardType.STATUS;
    }

    /** 这张牌是不是诅咒牌（v0.4.6 新类型）。 */
    public static boolean isCurse(Card card) {
        return card != null && card.type() == CardType.CURSE;
    }

    /** 状态牌 + 诅咒牌（"回合结束还在手里就发作"的那一类）。 */
    public static boolean isStatusOrCurse(Card card) {
        return isStatusCard(card) || isCurse(card);
    }

    /**
     * 不能被打出的牌：三种状态牌 + 除【戏谑】之外的诅咒 + **两张"使用不可"的技能牌**
     * （v0.5.3.7 A4-16【反射】【战术家】—— 它们的效果是"被弃置时"触发，"打出"这件事本身不成立）。
     */
    public static boolean unplayable(Card card) {
        if (UNPLAYABLE_SKILLS.contains(card.id())) {
            return true;
        }
        if (isStatusCard(card)) {
            return VOID.equals(card.id()) || BURN.equals(card.id()) || DAZED.equals(card.id())
                    || WOUND.equals(card.id());
        }
        return isCurse(card) && !CURSE_MOCKERY.equals(card.id());
    }

    /** "使用不可"的**技能牌**（不是状态 / 诅咒，但同样打不出去）。 */
    public static final java.util.Set<String> UNPLAYABLE_SKILLS =
            java.util.Set.of("reflex", "tactician");

    /** 抽到时本回合费用减少多少（虚空 = 1，其余 = 0）。 */
    public static int onDrawEnergyLoss(Card card) {
        return isStatusCard(card) && VOID.equals(card.id()) ? 1 : 0;
    }

    /** 回合结束还在手里时对自己造成的伤害（可被格挡）。 */
    public static int endOfTurnDamage(Card card) {
        if (!isStatusCard(card)) {
            return 0;
        }
        return BURN.equals(card.id()) || VENOM.equals(card.id()) ? BURN_DAMAGE : 0;
    }

    /** 回合结束还在手里时失去的生命值（无视格挡）。 */
    public static int endOfTurnHpLoss(Card card) {
        return isStatusCard(card) && CALL.equals(card.id()) ? CALL_HP_LOSS : 0;
    }

    /**
     * 【残忍】：回合结束还在手里时，失去**等同于当前手牌数量**的生命值。
     *
     * @return 每张手牌对应的生命值损失（0 = 没有这个效果）
     */
    public static int endOfTurnHpLossPerHandCard(Card card) {
        return isCurse(card) && CURSE_CRUEL.equals(card.id()) ? 1 : 0;
    }

    /** 【贪婪】：回合结束还在手里时失去的金币（0 = 没有这个效果）。 */
    public static int endOfTurnGoldLoss(Card card) {
        return isCurse(card) && CURSE_GREED.equals(card.id()) ? GREED_GOLD_LOSS : 0;
    }

    /** 【戏谑】：回合结束还在手里时，把一张复制品加入弃牌堆。 */
    public static boolean copiesItselfOnEndOfTurn(Card card) {
        return isCurse(card) && CURSE_MOCKERY.equals(card.id());
    }

    /** 【固执】：回合结束还在手里时获得的易伤层数。 */
    public static int endOfTurnVulnerable(Card card) {
        return isCurse(card) && CURSE_STUBBORN.equals(card.id()) ? STUBBORN_VULNERABLE : 0;
    }

    /** 这张状态牌/诅咒牌有没有"回合结束"效果（给界面/自检用）。 */
    public static boolean hasEndOfTurnEffect(Card card) {
        return endOfTurnDamage(card) > 0 || endOfTurnHpLoss(card) > 0 || endOfTurnHpLossPerHandCard(card) > 0
                || endOfTurnGoldLoss(card) > 0 || copiesItselfOnEndOfTurn(card) || endOfTurnVulnerable(card) > 0;
    }
}
