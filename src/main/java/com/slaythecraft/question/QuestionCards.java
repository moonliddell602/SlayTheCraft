package com.slaythecraft.question;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardType;
import com.slaythecraft.data.PlayerCardData;

/**
 * 问号事件里"选一张牌"的筛选与结算（v0.5.0）。
 *
 * <p>筛选规则放在这里，客户端（选牌页）与服务端（结算前的校验）用**同一份代码**，
 * 所以不可能出现"客户端能点、服务端点不动"的错位。</p>
 */
public final class QuestionCards {

    private QuestionCards() {
    }

    /** 这张牌能不能被这个选项选中。 */
    public static boolean matches(QuestionOption.CardPick pick, PlayerCardData data, int index) {
        if (pick == null || pick == QuestionOption.CardPick.NONE) {
            return false;
        }
        if (data == null || index < 0 || index >= data.cardCount()) {
            return false;
        }
        Card card = CardLibrary.byId(data.cards().get(index).id().getPath());
        if (card == null) {
            return false;
        }
        return switch (pick) {
            case NONE -> false;
            case UPGRADE_ATTACK -> card.type() == CardType.ATTACK && data.canUpgradeAt(index);
            case REMOVE_ANY -> true;
        };
    }

    /** 选牌页要列出的下标（按牌组顺序）。 */
    public static List<Integer> selectable(QuestionOption.CardPick pick, PlayerCardData data) {
        List<Integer> indices = new ArrayList<>();
        if (data == null) {
            return List.copyOf(indices);
        }
        for (int i = 0; i < data.cardCount(); i++) {
            if (matches(pick, data, i)) {
                indices.add(i);
            }
        }
        return List.copyOf(indices);
    }

    /** 选牌页每次最多显示多少个（超过就滚轮翻页；牌组一般不会这么大）。 */
    public static final int PAGE_SIZE = 20;

    /** 分页显示时的页数。 */
    public static int pages(int total) {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
