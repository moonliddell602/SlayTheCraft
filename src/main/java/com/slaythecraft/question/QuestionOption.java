package com.slaythecraft.question;

import java.util.Random;

import com.slaythecraft.data.PlayerCardData;

/**
 * 问号事件里的一个选项（v0.0.2 V11；v0.5.0 支持"先选一张牌"）。
 *
 * <p>效果只依赖玩家档案 {@link PlayerCardData} 与一个 {@link Random}，**不碰 Minecraft**，
 * 因此能用无头自检逐条验证"选了这一项之后档案确实变成什么样"。</p>
 *
 * <p>v0.5.0：有些选项需要玩家**先选一张牌**（盔甲商的"随机升级两张技能牌"里的删牌分支、
 * 武器匠的"升级一张攻击牌"、傻子村民的"删一张牌"）。这时 {@link #pick()} 不是 {@code NONE}，
 * 客户端会先打开选牌页，再把选中的下标一起回传；服务端用 {@link #choose()} 结算。</p>
 *
 * @param id          稳定 id（存档/自检用）
 * @param label       按钮上的短标签
 * @param description 选项说明（界面里显示一行）
 * @param pick        需不需要先选牌、能选哪些牌
 * @param effect      不需要选牌时的效果；返回给玩家看的结果文案
 * @param choose      需要选牌时的效果（入参是选中的牌下标）；返回结果文案
 */
public record QuestionOption(String id, String label, String description, CardPick pick, Effect effect,
                            PickEffect choose) {

    /** "先选一张牌"的筛选条件。 */
    public enum CardPick {
        /** 不需要选牌。 */
        NONE,
        /** 只能选**攻击牌**（且还没强化过）——武器匠开刃。 */
        UPGRADE_ATTACK,
        /** 牌组里任意一张——盔甲商收废料 / 傻子村民换吃的。 */
        REMOVE_ANY
    }

    /** 选牌页右上角要显示的说明。 */
    public String pickHint() {
        return switch (pick) {
            case NONE -> "";
            case UPGRADE_ATTACK -> "选择一张攻击牌（会被强化）";
            case REMOVE_ANY -> "选择一张要从牌组里移除的牌";
        };
    }

    public QuestionOption {
        pick = pick == null ? CardPick.NONE : pick;
    }

    /** 不需要选牌的选项。 */
    public QuestionOption(String id, String label, String description, Effect effect) {
        this(id, label, description, CardPick.NONE, effect, null);
    }

    /** 需要先选一张牌的选项。 */
    public QuestionOption(String id, String label, String description, CardPick pick, PickEffect choose) {
        this(id, label, description, pick, null, choose);
    }

    /** 这一项要不要先选牌。 */
    public boolean needsCard() {
        return pick != CardPick.NONE;
    }

    /** 选项效果（不需要选牌）。 */
    @FunctionalInterface
    public interface Effect {
        String apply(PlayerCardData data, Random random);
    }

    /** 选项效果（需要先选牌，入参是选中的牌下标）。 */
    @FunctionalInterface
    public interface PickEffect {
        String apply(PlayerCardData data, int cardIndex);
    }
}
