package com.slaythecraft.combat.core;

/**
 * 篝火规则（v0.0.3 W5）。
 *
 * <p>放在 {@code core} 里是为了让"愈合比例"和"退出是否计数"这两条需求也能在没有 Minecraft 的
 * 裸 JVM 自检里验收（{@code core} 包不允许依赖任何 Minecraft 类型）。</p>
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>愈合 / 多人包扎：回复生命上限的 <b>25%</b>；</li>
 *   <li>什么都没选就退出 → <b>不计次数</b>；但只要<b>已经有人选了</b>，
 *       其他人即使什么都没选就退出，也<b>照样计入次数</b>。</li>
 * </ul>
 */
public final class CampfireRules {

    /** 愈合 / 包扎回复生命上限的比例（需求写死 25%）。 */
    public static final int HEAL_PERCENT = 25;

    private CampfireRules() {
    }

    /**
     * 回复量：生命上限 × 比例，向下取整，至少 1 点。
     *
     * @param maxHp   生命上限
     * @param percent 回复比例（百分数）
     */
    public static int healAmount(int maxHp, int percent) {
        if (maxHp <= 0) {
            return 0;
        }
        return Math.max(1, maxHp * Math.max(1, Math.min(100, percent)) / 100);
    }

    /** 按需求默认比例（25%）计算回复量。 */
    public static int healAmount(int maxHp) {
        return healAmount(maxHp, HEAL_PERCENT);
    }

    /**
     * "什么都没选就退出"是否要计入本次篝火。
     *
     * <p>判据是"**别人**已经选过了"——自己选的那一次已经在选项结算时单独记过账了。
     * 所以只有别人已经选过时，这个空手退出的玩家才补记一次。</p>
     *
     * @param otherChoosers 已经做出选择的<b>其他人</b>数量
     */
    public static boolean countOnExit(int otherChoosers) {
        return otherChoosers > 0;
    }
}
