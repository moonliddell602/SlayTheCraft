package com.slaythecraft.combat.core;

/**
 * 每层里可以被计数、也有上限的事件类型（v0.0.2 V9）。
 *
 * <p>{@link #BOSS} 是"出口"：它不计入上限，也永远可以触发（需求里的"可跳过剩余内容直接挑战 BOSS"）。</p>
 *
 * <p>v0.5.3.0 追加 {@link #TREASURE}（藏宝箱）：当时需求只说"和篝火一样不能被连续使用"，
 * 所以它**不计数**、只更新 {@code lastEvent}（于是"连开两个藏宝箱"会被挡住）。</p>
 *
 * <p>v0.5.5.1（用户）："事件回合统计没有添加有关藏宝箱的内容（包括目前完成进度、**上限 5 次**、
 * 以及事件触发条件介绍）" —— 所以藏宝箱**改为计数**、每层上限 5，并出现在事件统计页里。</p>
 */
public enum EventKind {

    NORMAL("常规战斗"),
    ELITE("精英战斗"),
    CAMPFIRE("火堆"),
    QUESTION("问号事件"),
    BOSS("BOSS 战"),
    /** 藏宝箱（v0.5.3.0）：副手收纳包 + 主手镐 → 右键陷阱箱，开出一件随机非特殊遗物。 */
    TREASURE("藏宝箱");

    private final String label;

    EventKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** BOSS 战不受每层上限限制（但仍会更新 {@code lastEvent}）。v0.5.5.1 起藏宝箱**受限**（5 次）。 */
    public boolean capped() {
        return this != BOSS;
    }
}
