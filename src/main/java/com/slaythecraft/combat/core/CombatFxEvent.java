package com.slaythecraft.combat.core;

/**
 * 一次表现层事件（v0.3.0）：{@link CombatFx} 类型 + 动作发起者 + 目标（可能为 {@code null}）。
 *
 * <p>{@code actor}/{@code target} 的实体编号用 {@link Combatant#fxId()}：Minecraft 层在创建
 * 参战者时写入 {@code entity.getId()}，表现层据此找到要播动画的实体。</p>
 *
 * <p>v0.5.0.5 追加 {@code hits}：多段攻击一次结算会产生 N 段动作，客户端据此排队播 N 次动画
 * （需求："敌人的多段攻击动画会被按照单次攻击结算。应为攻击多少次就播放多少次动画。"）。
 * 3 参构造器保留给"单段"的既有调用点，默认 {@code hits = 1}。</p>
 */
public record CombatFxEvent(CombatFx kind, Combatant actor, Combatant target, int hits) {

    /** 3 参构造器：单段动作（绝大多数事件）。 */
    public CombatFxEvent(CombatFx kind, Combatant actor, Combatant target) {
        this(kind, actor, target, 1);
    }

    public CombatFxEvent {
        hits = Math.max(1, hits);
    }

    /** 发起者的实体编号（没有绑定实体时是 -1）。 */
    public int actorFxId() {
        return actor == null ? -1 : actor.fxId();
    }

    /** 目标的实体编号（没有目标时是 -1）。 */
    public int targetFxId() {
        return target == null ? -1 : target.fxId();
    }

    /** 这段动作是不是"多段动作"（客户端要排队播放）。 */
    public boolean multiHit() {
        return hits > 1;
    }
}
