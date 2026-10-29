package com.slaythecraft.combat.core;

import java.util.Objects;
import java.util.Set;

/**
 * 一件遗物（v0.0.2 V7）：一条数据 + 一个效果，效果挂在若干 {@link RelicTrigger} 上。
 *
 * <p>刻意保持"无状态"：效果只能读写 {@link CombatContext} 与自己的 {@link Combatant}，
 * 因此加遗物不需要动引擎；需要跨回合计数的遗物（例如"每打出 3 张牌抽 1"）等以后引入
 * 遗物内部计数器时再做。</p>
 */
public record Relic(String id, String name, String description, Set<RelicTrigger> triggers, RelicEffect effect) {

    /** 遗物效果：在触发点被调用。 */
    @FunctionalInterface
    public interface RelicEffect {
        void apply(CombatContext context, Combatant owner);
    }

    public Relic {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(effect, "effect");
        triggers = triggers == null ? Set.of() : Set.copyOf(triggers);
    }

    public boolean firesOn(RelicTrigger trigger) {
        return triggers.contains(trigger);
    }

    /** 展示文本：{@code 金刚杵 —— 战斗开始时获得 1 点力量。} */
    public String describe() {
        return name + " —— " + description;
    }
}
