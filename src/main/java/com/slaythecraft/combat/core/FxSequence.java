package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * **多段动作的时间排队**（v0.4.6.7）。
 *
 * <p>需求原文："如果敌人执行的是多段攻击（例如：造成 1 点伤害 × 次），则播放 × 次攻击动画。"</p>
 *
 * <p>问题是：3 段攻击的 3 个表现层事件会在**同一瞬间**到达客户端，而客户端上"一个实体只有一个
 * 当前动画"（后到的覆盖先到的）—— 于是玩家只看到 1 次前冲。这里把时间轴算清楚：第 1 段立刻开始，
 * 后面的每段都排在前一段结束之后（{@code max(现在, 上一段结束)}），客户端按这个时刻依次播放。</p>
 *
 * <p>纯逻辑、零 Minecraft 依赖，所以能进裸 JVM 自检。</p>
 */
public final class FxSequence {

    /** 已排定的开始时刻（供自检/调试查看）。 */
    private final List<Long> starts = new ArrayList<>();
    /** 最后一段的结束时刻（0 = 现在没有排队的东西）。 */
    private long busyUntil;

    /**
     * 追加一段动作。
     *
     * @param now      当前时刻
     * @param duration 这段动作要播多久
     * @return 这段动作应该**开始**的时刻（等于 now 表示立刻开始）
     */
    public long schedule(long now, long duration) {
        long start = Math.max(now, busyUntil);
        busyUntil = start + Math.max(0L, duration);
        starts.add(start);
        return start;
    }

    /** 现在还有排在后面的动作吗。 */
    public boolean busy(long now) {
        return busyUntil > now;
    }

    /** 全部排定的开始时刻（按顺序）。 */
    public List<Long> starts() {
        return List.copyOf(starts);
    }

    /** 已经排了几段。 */
    public int size() {
        return starts.size();
    }

    /** 清空（战斗结束 / 断线）。 */
    public void reset() {
        starts.clear();
        busyUntil = 0L;
    }
}
