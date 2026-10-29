package com.slaythecraft.combat.core;

/**
 * 一颗**具体的**充能球（v0.3.0）。
 *
 * <p>v0.0.3 的球只有类型，暗球需要"记住了多少充能"，所以把球从枚举升级成"类型 + 充能"的实例：
 * 只有暗球会用 {@code charge}（初始 6，每回合结束 +6，激发时按它算伤害），其余球恒为 0。</p>
 */
public record OrbInstance(Orb type, int charge) {

    public OrbInstance {
        if (type == null) {
            throw new IllegalArgumentException("an orb needs a type");
        }
        charge = Math.max(0, charge);
    }

    /** 新生成一颗球：暗球带着初始充能，其余球充能记 0。 */
    public static OrbInstance fresh(Orb type) {
        return new OrbInstance(type, type.grows() ? type.evokeValue() : 0);
    }

    /** 回合结束时的被动成长（暗球 +6；其余球原样返回）。 */
    public OrbInstance grown() {
        return grown(0);
    }

    /**
     * 回合结束时的被动成长，带上【集中】加成（v0.5.0.4）。
     *
     * <p>需求："黑暗球：每回合成长的伤害增加 X"——X 就是集中的层数。
     * 暗球每回合的充能增长 = {@code growth() + bonus}（初始充能不吃集中，
     * 因为激发伤害 = 充能 + 集中，那条路径已经在 {@code CombatContext.evokeOrb} 里加过了）。</p>
     *
     * <p>只有会成长的球（暗球）使用 {@code bonus}；其余球原样返回。
     * 负数 bonus（集中被减到负）由构造器夹到 0，不会出现负充能。</p>
     */
    public OrbInstance grown(int bonus) {
        return type.grows() ? new OrbInstance(type, charge + type.growth() + bonus) : this;
    }
    /** 激发时实际生效的数值：暗球是当前充能，其余球是枚举里的激发值。 */
    public int effectiveEvoke() {
        return type.grows() ? charge : type.evokeValue();
    }

    /** 给战报/界面看的名字（暗球会带上充能）。 */
    public String describe() {
        return type.grows() ? type.label() + "（充能 " + charge + "）" : type.label();
    }
}
