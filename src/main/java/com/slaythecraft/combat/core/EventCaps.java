package com.slaythecraft.combat.core;

/**
 * 每层的**五类事件**上限（v0.0.2 V9 需求给的 15/5/5/5；v0.5.5.1 加入藏宝箱的 5）。
 *
 * <p>上限按层独立计算，达到上限后该类别在本层不可再触发（见 {@link ProgressState#canTrigger}）。</p>
 *
 * @param normal   常规战斗
 * @param elite    精英战斗
 * @param campfire 火堆
 * @param question 问号事件
 * @param treasure 藏宝箱（v0.5.5.1：用户要求"上限 5 次"并写进事件统计）
 */
public record EventCaps(int normal, int elite, int campfire, int question, int treasure) {

    /** 需求原始数值：常规战斗 15 / 精英 5 / 火堆 5 / 问号 5 / 藏宝箱 5。 */
    public static final EventCaps DEFAULT = new EventCaps(15, 5, 5, 5, 5);

    public EventCaps {
        normal = Math.max(0, normal);
        elite = Math.max(0, elite);
        campfire = Math.max(0, campfire);
        question = Math.max(0, question);
        treasure = Math.max(0, treasure);
    }

    /** 兼容 v0.5.4.1 及以前的四参构造（藏宝箱取默认 5）。 */
    public EventCaps(int normal, int elite, int campfire, int question) {
        this(normal, elite, campfire, question, 5);
    }

    /** 该类事件的上限；BOSS 战不受限（返回 {@link Integer#MAX_VALUE}）。 */
    public int capOf(EventKind kind) {
        return switch (kind) {
            case NORMAL -> normal;
            case ELITE -> elite;
            case CAMPFIRE -> campfire;
            case QUESTION -> question;
            case TREASURE -> treasure;
            case BOSS -> Integer.MAX_VALUE;
        };
    }
}
