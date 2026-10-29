package com.slaythecraft.combat.core;

/**
 * 怪物的战斗分档（v0.0.2 修正从 {@code combat.mc.MobTiers} 里提到 core）。
 *
 * <p>提上来的原因：遭遇战的**编成规则**（一次能卷入几只、只收哪一档）现在是纯逻辑，
 * 放到 core 才能在无头自检里逐条验证；{@code MobTiers} 只负责"实体 → 分档"的 Minecraft 部分。</p>
 */
public enum MobTier {

    NORMAL("普通"),
    ELITE("精英"),
    BOSS("BOSS"),
    /** 不参与战斗与奖励（动物等）。 */
    NONE("无");

    private final String label;

    MobTier(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 是否算"值得打"的敌人（普通/精英/BOSS）。 */
    public boolean battleWorthy() {
        return this != NONE;
    }
}
