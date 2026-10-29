package com.slaythecraft.combat.core;

/**
 * 战斗站位计算（v0.0.2 V3）：玩家与敌人**面对面**、保持至少 {@code distance} 格直线距离。
 *
 * <p>刻意放在 {@code combat/core}（零 Minecraft 依赖）：这部分是纯几何，最容易算错，
 * 放在这里就能被既有引擎自检直接覆盖（朝向、距离、多人不重叠）。真正"找安全落点/传送"
 * 的部分在 {@code combat.mc.PlacementHelper}。</p>
 *
 * <p><b>v0.0.2 测试反馈修正</b>：默认站位改成"把**敌人**排到玩家正前方"
 * （见 {@link EnemyFormation}），不再强制传送玩家；本类的 {@link #compute} 仍然保留，
 * 用于需要把玩家摆到敌人对面的场合（例如将来做"玩家被拉进战斗"的选项）。</p>
 */
public final class StandoffLayout {

    /** 一名参与者的落位：坐标 + 面向角度（Minecraft yaw 约定）。 */
    public record Placement(double x, double y, double z, float yaw) {
    }

    /** 多人站位的横向间距（格）。 */
    public static final double LATERAL_SPACING = 1.6D;

    /** 允许的最小对峙距离：需求要求至少 5 格。 */
    public static final double MIN_DISTANCE = 5.0D;

    private StandoffLayout() {
    }

    /**
     * 计算第 {@code index} 名参与者的站位。
     *
     * <p>方向 = 从"敌人重心"指向"该玩家原来的位置"，因此玩家仍待在自己那一侧；
     * 距离取 {@code max(distance, 5)}；多人沿垂直方向散开，避免叠在一起。</p>
     *
     * @param centroidX 敌人重心的 X
     * @param centroidZ 敌人重心的 Z
     * @param fromX     该玩家原来的 X（用来决定站哪一侧）
     * @param fromZ     该玩家原来的 Z
     * @param index     该玩家在参战名单里的序号（0 基）
     * @param count     参战人数
     * @param distance  期望的对峙距离（会被抬到至少 5）
     * @param y         落点的 Y（由调用方按地形决定）
     */
    public static Placement compute(double centroidX, double centroidZ, double fromX, double fromZ,
                                    int index, int count, double distance, double y) {
        double dx = fromX - centroidX;
        double dz = fromZ - centroidZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D) {
            // 玩家正好站在敌人重心上：随便挑一个方向，保证仍然面对面
            dx = 1.0D;
            dz = 0.0D;
            length = 1.0D;
        }
        double dirX = dx / length;
        double dirZ = dz / length;
        // 垂直方向（用于多人散开）
        double lateralX = -dirZ;
        double lateralZ = dirX;

        double offset = (index - (Math.max(1, count) - 1) / 2.0D) * LATERAL_SPACING;
        double standoff = Math.max(distance, MIN_DISTANCE);
        double x = centroidX + dirX * standoff + lateralX * offset;
        double z = centroidZ + dirZ * standoff + lateralZ * offset;
        return new Placement(x, y, z, yawTowards(x, z, centroidX, centroidZ));
    }

    /**
     * 从 {@code (x,z)} 望向 {@code (targetX,targetZ)} 的 yaw。
     *
     * <p>Minecraft 约定：0° 朝 +Z（南）、90° 朝 -X（西）、-90° 朝 +X（东）、180° 朝 -Z（北）。</p>
     */
    public static float yawTowards(double x, double z, double targetX, double targetZ) {
        double dx = targetX - x;
        double dz = targetZ - z;
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }

    /** 该落点与敌人重心的水平距离（自检用）。 */
    public static double horizontalDistance(Placement placement, double centroidX, double centroidZ) {
        double dx = placement.x() - centroidX;
        double dz = placement.z() - centroidZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
