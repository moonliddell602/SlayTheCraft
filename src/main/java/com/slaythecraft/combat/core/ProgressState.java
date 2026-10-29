package com.slaythecraft.combat.core;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 尖塔进度（v0.0.2 V9）：当前层数、本层各类事件的计数、已击败的 BOSS、是否通关。
 *
 * <p>不可变：所有操作返回新的状态，方便自检逐条比对，也避免"改了一半"写回档案。
 * 每层事件计数在**进阶时清零**（需求：上限是每层独立计算）。</p>
 *
 * <p>V10 追加 {@code lastEvent}：只记"本层最近一次经历的事件"，用来实现
 * <b>篝火不能连续使用</b>（两次火堆之间必须发生一次其它事件）。BOSS 战也会更新它，
 * 但不占任何计数。</p>
 */
public final class ProgressState {

    /** 会被计数（有上限）的事件，顺序固定用于展示。v0.5.5.1 起藏宝箱也进这张表（上限 5）。 */
    public static final EventKind[] CAPPED_KINDS = {
            EventKind.NORMAL, EventKind.ELITE, EventKind.CAMPFIRE, EventKind.QUESTION, EventKind.TREASURE
    };

    private final int floor;
    private final Map<EventKind, Integer> counts;
    private final Set<Integer> bossesDefeated;
    private final boolean completed;
    /** 本层最近一次经历的事件（null = 本层还没发生任何事件）。 */
    private final EventKind lastEvent;
    /** 本局是否已经战败过（测试反馈：通关或战败前不允许换角色）。 */
    private final boolean defeated;
    /**
     * 本层**已经访问过的问号事件 id**（v0.4.5）。
     *
     * <p>需求："相同问号事件每层只能访问一次" —— 所以记忆按层保存，进阶时自动清空
     * （{@link #recordBossDefeat()} 换新层时会重新建一份空集合）。</p>
     */
    private final Set<String> visitedEvents;
    /**
     * 本局**已经触发过战斗的 BOSS 实体 id**（v0.4.6.7）。
     *
     * <p>需求："所有层数的 BOSS 都只能触发一次战斗，且进入下一层后，不能再和上一层的 boss
     * 触发战斗。" 层数限制由"只找当前层的 BOSS"天然保证；这里额外记住"这只 BOSS 已经打过"，
     * 于是同一只 BOSS 不会在一局里被触发第二次（重开一局会清空）。</p>
     */
    private final Set<String> bossesFought;

    private ProgressState(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                          boolean completed, EventKind lastEvent, boolean defeated) {
        this(floor, counts, bossesDefeated, completed, lastEvent, defeated, Set.of(), Set.of());
    }

    private ProgressState(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                          boolean completed, EventKind lastEvent, boolean defeated, Set<String> visitedEvents) {
        this(floor, counts, bossesDefeated, completed, lastEvent, defeated, visitedEvents, Set.of());
    }

    private ProgressState(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                          boolean completed, EventKind lastEvent, boolean defeated, Set<String> visitedEvents,
                          Set<String> bossesFought) {
        this.floor = Math.min(Math.max(1, floor), FloorBoss.FLOORS);
        this.counts = new EnumMap<>(EventKind.class);
        for (EventKind kind : EventKind.values()) {
            Integer value = counts.get(kind);
            this.counts.put(kind, value == null ? 0 : Math.max(0, value));
        }
        this.bossesDefeated = new LinkedHashSet<>(bossesDefeated);
        this.completed = completed;
        this.lastEvent = lastEvent;
        this.defeated = defeated;
        this.visitedEvents = new LinkedHashSet<>(visitedEvents == null ? Set.of() : visitedEvents);
        this.bossesFought = new LinkedHashSet<>(bossesFought == null ? Set.of() : bossesFought);
    }

    // ------------------------------------------------------------------ BOSS 只打一次（v0.4.6.7）

    /** 本局已经触发过战斗的 BOSS 实体 id（只读）。 */
    public Set<String> bossesFought() {
        return Set.copyOf(bossesFought);
    }

    /** 这只 BOSS 本局是否已经触发过战斗。 */
    public boolean bossFought(String entityId) {
        return entityId != null && bossesFought.contains(entityId);
    }

    /** 清空"已打过的 BOSS"记录（用于自愈：旧逻辑把"开战"误记成"打过"时允许重新挑战）。 */
    public ProgressState clearBossesFought() {
        return bossesFought.isEmpty() ? this
                : new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, visitedEvents,
                        Set.of());
    }

    /** 记下"这只 BOSS 本局已经打过一次"；重复记录返回原对象。 */
    public ProgressState withBossFought(String entityId) {
        if (entityId == null || bossesFought.contains(entityId)) {
            return this;
        }
        Set<String> next = new LinkedHashSet<>(bossesFought);
        next.add(entityId);
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, visitedEvents,
                next);
    }

    /** 全新进度：第 1 层，什么都没打。 */
    public static ProgressState fresh() {
        return new ProgressState(1, Map.of(), Set.of(), false, null, false);
    }

    public static ProgressState of(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                                   boolean completed) {
        return new ProgressState(floor, counts, bossesDefeated, completed, null, false);
    }

    public static ProgressState of(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                                   boolean completed, EventKind lastEvent) {
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, false);
    }

    public static ProgressState of(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                                   boolean completed, EventKind lastEvent, boolean defeated) {
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated);
    }

    /** 完整构造（v0.4.5：额外带上本层已访问的问号事件 id）。 */
    public static ProgressState of(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                                   boolean completed, EventKind lastEvent, boolean defeated,
                                   Set<String> visitedEvents) {
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, visitedEvents);
    }

    /** 完整构造（v0.4.6.7：再带上"本局已经打过的 BOSS 实体 id"）。 */
    public static ProgressState of(int floor, Map<EventKind, Integer> counts, Set<Integer> bossesDefeated,
                                   boolean completed, EventKind lastEvent, boolean defeated,
                                   Set<String> visitedEvents, Set<String> bossesFought) {
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, visitedEvents,
                bossesFought);
    }

    // ------------------------------------------------------------------ 问号事件的本层记忆（v0.4.5）

    /** 本层已经访问过的问号事件 id（只读）。 */
    public Set<String> visitedEvents() {
        return Set.copyOf(visitedEvents);
    }

    /** 本层是否已经访问过这个问号事件（需求：相同问号事件每层只能访问一次）。 */
    public boolean hasVisitedEvent(String eventId) {
        return eventId != null && visitedEvents.contains(eventId);
    }

    /** 记下"本层已经访问过这个问号事件"；重复记录返回原对象。 */
    public ProgressState withVisitedEvent(String eventId) {
        if (eventId == null || visitedEvents.contains(eventId)) {
            return this;
        }
        Set<String> next = new LinkedHashSet<>(visitedEvents);
        next.add(eventId);
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, next, bossesFought);
    }

    /** 清空本层的问号事件记忆（调试指令用）。 */
    public ProgressState clearVisitedEvents() {
        return visitedEvents.isEmpty() ? this
                : new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, defeated, Set.of(), bossesFought);
    }

    /** 本局是否已经战败过。 */
    public boolean defeated() {
        return defeated;
    }

    /**
     * 本局是否已经**结束**：通关三层，或者战败过一次。
     *
     * <p>只有结束之后才允许更换角色 / 开始新的一局（测试反馈：防"切角色重复发初始牌组"）。</p>
     */
    public boolean isRunOver() {
        return completed || defeated;
    }

    /** 记下"本局战败"。 */
    public ProgressState withDefeated() {
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, true, visitedEvents, bossesFought);
    }

    /**
     * **解除"本局战败"标记**（v0.5.0.20）。
     *
     * <p>用于"意外关闭游戏后重进存档"的恢复：那时战斗已经没了（战斗状态在内存里、随进程消失），
     * 但"本局已战败"这个标记已经落盘。按需求"取消最后的战斗"处理 —— 把标记清掉，
     * 让玩家能继续正常游玩（而不是一进世界就走战败重开的流程）。</p>
     *
     * <p>只清 {@code defeated} 一个字段：楼层、事件计数、已击败 BOSS、通关状态都原样保留。</p>
     */
    public ProgressState withoutDefeated() {
        if (!defeated) {
            return this;
        }
        return new ProgressState(floor, counts, bossesDefeated, completed, lastEvent, false, visitedEvents,
                bossesFought);
    }

    /** 本层最近一次经历的事件；本层什么都还没发生时返回 {@code null}。 */
    public EventKind lastEvent() {
        return lastEvent;
    }

    /** 最近一次事件就是这一类（篝火"不能连续使用"的判定：{@code isConsecutive(CAMPFIRE)}）。 */
    public boolean isConsecutive(EventKind kind) {
        return lastEvent == kind;
    }

    public int floor() {
        return floor;
    }

    public boolean completed() {
        return completed;
    }

    /** 只读快照（顺序固定）。 */
    public Map<EventKind, Integer> counts() {
        return Map.copyOf(counts);
    }

    public int countOf(EventKind kind) {
        return counts.getOrDefault(kind, 0);
    }

    public Set<Integer> bossesDefeated() {
        return Set.copyOf(bossesDefeated);
    }

    public boolean bossDefeated(int floorNumber) {
        return bossesDefeated.contains(floorNumber);
    }

    /** 当前层的 BOSS（通关后仍指向最后一层，方便显示"已通关"）。 */
    public FloorBoss boss() {
        return FloorBoss.ofFloor(floor);
    }

    public boolean isFinalFloor() {
        return floor >= FloorBoss.FLOORS;
    }

    /** 这一类事件在本层还能不能触发（BOSS 永远可以）。 */
    public boolean canTrigger(EventKind kind, EventCaps caps) {
        if (completed) {
            return kind == EventKind.BOSS;
        }
        if (!kind.capped()) {
            return true;
        }
        return countOf(kind) < caps.capOf(kind);
    }

    /** 记一次事件；达到上限时**原样返回**（调用方应先问 {@link #canTrigger}）。 */
    public ProgressState record(EventKind kind, EventCaps caps) {
        if (!canTrigger(kind, caps)) {
            return this;
        }
        if (!kind.capped()) {
            // BOSS 战不占计数，但它是"一次其它事件"：要更新 lastEvent（篝火连用判定用）
            return new ProgressState(floor, counts, bossesDefeated, completed, kind, defeated, visitedEvents, bossesFought);
        }
        Map<EventKind, Integer> next = new EnumMap<>(counts);
        next.put(kind, countOf(kind) + 1);
        return new ProgressState(floor, next, bossesDefeated, completed, kind, defeated, visitedEvents, bossesFought);
    }

    /**
     * 击败当前层 BOSS：解锁下一层并清空本层事件计数；最后一层则标记通关。
     *
     * <p>通关后层数停在最后一层，计数保留（显示用），{@link #canTrigger} 只允许再挑战 BOSS。</p>
     */
    public ProgressState recordBossDefeat() {
        Set<Integer> defeated = new LinkedHashSet<>(bossesDefeated);
        defeated.add(floor);
        if (isFinalFloor()) {
            return new ProgressState(floor, counts, defeated, true, EventKind.BOSS, this.defeated, visitedEvents, bossesFought);
        }
        // 进阶：新的一层，计数与 lastEvent 都从零开始
        return new ProgressState(floor + 1, Map.of(), defeated, false, null, this.defeated, Set.of(), bossesFought);
    }

    /** 重开一局（调试指令 / 通关后重打）：回到第 1 层。 */
    public ProgressState reset() {
        return fresh();
    }

    /** 每层事件计数的展示文本，例如 {@code 常规 3/15 · 精英 1/5 · 火堆 0/5 · 问号 0/5}。 */
    public String describeCounts(EventCaps caps) {
        StringBuilder text = new StringBuilder();
        for (EventKind kind : CAPPED_KINDS) {
            if (!text.isEmpty()) {
                text.append(" · ");
            }
            text.append(kind.label()).append(' ').append(countOf(kind)).append('/').append(caps.capOf(kind));
        }
        return text.toString();
    }

    /** 单行摘要：{@code 第 2 层 · 目标 凋零 · 常规 3/15 · …}。 */
    public String describe(EventCaps caps) {
        FloorBoss boss = boss();
        String goal = boss == null ? "—" : boss.name();
        return "第 " + floor + " 层" + (completed ? "（已通关）" : "") + " · 目标 " + goal + " · "
                + describeCounts(caps);
    }
}
