package com.slaythecraft.combat.core;

/**
 * 独立生命值（v0.0.2 V9）。
 *
 * <p>玩家的战斗生命**不再使用原版血量**：上限默认 75，跨战斗保留，只由战斗引擎与事件改变。
 * 这里只做纯粹的数值裁剪，方便在无 Minecraft 的自检里逐条验证。</p>
 *
 * <p>不可变：所有改动都返回新的池子，避免"改了一半"的状态被写回档案。</p>
 */
public record LifePool(int hp, int maxHp) {

    public static final int DEFAULT_MAX = 75;

    public LifePool {
        if (maxHp < 1) {
            maxHp = 1;
        }
        if (hp < 0) {
            hp = 0;
        }
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    /** 满血的新池子。 */
    public static LifePool full(int maxHp) {
        return new LifePool(Math.max(1, maxHp), Math.max(1, maxHp));
    }

    public static LifePool of(int hp, int maxHp) {
        return new LifePool(hp, maxHp);
    }

    /** 默认配置（上限 75、满血）。 */
    public static LifePool standard() {
        return full(DEFAULT_MAX);
    }

    /** 改上限；当前生命按比例保留吗？不——按需求只做裁剪：上限变小时生命跟着降下来。 */
    public LifePool withMax(int newMax) {
        return new LifePool(hp, newMax);
    }

    public LifePool set(int newHp) {
        return new LifePool(newHp, maxHp);
    }

    public LifePool damage(int amount) {
        return amount <= 0 ? this : new LifePool(hp - amount, maxHp);
    }

    public LifePool heal(int amount) {
        return amount <= 0 ? this : new LifePool(hp + amount, maxHp);
    }

    /** 回满（战败重开、事件奖励等用）。 */
    public LifePool restore() {
        return new LifePool(maxHp, maxHp);
    }

    public boolean isDown() {
        return hp <= 0;
    }

    public boolean isFull() {
        return hp >= maxHp;
    }

    public int missing() {
        return maxHp - hp;
    }

    /** 展示文本：{@code 62/75}。 */
    public String describe() {
        return hp + "/" + maxHp;
    }
}
