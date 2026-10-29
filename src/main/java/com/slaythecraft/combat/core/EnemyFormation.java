package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 敌人的站位（v0.0.2 测试反馈修正）：把参战怪物摆到**玩家当前朝向的正前方**，一字排开。
 *
 * <p>反馈原文："将参与对战的怪物强制移动到玩家当前朝向前方的位置；怪物位置过于分散，影响观感。"
 * 之前是把玩家摆到敌人对面，怪物留在原地，所以怪常常散得到处都是。</p>
 *
 * <p>纯几何、零 Minecraft 依赖：朝向、距离、不重叠都在引擎自检里验证；
 * 真正"找安全落点并传送"的部分在 {@code combat.mc.PlacementHelper}。</p>
 */
public final class EnemyFormation {

    /** 一名敌人的落位：水平坐标 + 面向角度（Minecraft yaw 约定）。 */
    public record Spot(double x, double z, float yaw) {
    }

    /** 敌人之间的横向间距（格）：2 格刚好不挤在一起。 */
    public static final double LATERAL_SPACING = 2.0D;

    /** 两侧的敌人往后收一点，形成浅弧形（观感更自然）。 */
    private static final double ARC_PULLBACK = 0.25D;

    private EnemyFormation() {
    }

    /**
     * 计算 {@code count} 只敌人在玩家正前方的站位。
     *
     * @param hostX    玩家 X
     * @param hostZ    玩家 Z
     * @param hostYaw  玩家当前朝向（Minecraft yaw：0 = +Z 南、90 = -X 西）
     * @param count    敌人数量（&lt;= 0 返回空列表）
     * @param distance 期望的对峙距离（会被抬到至少 {@link StandoffLayout#MIN_DISTANCE}）
     */
    public static List<Spot> compute(double hostX, double hostZ, float hostYaw, int count, double distance) {
        if (count <= 0) {
            return List.of();
        }
        double standoff = Math.max(distance, StandoffLayout.MIN_DISTANCE);
        double radians = Math.toRadians(hostYaw);
        // Minecraft yaw：0 朝 +Z，90 朝 -X
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        // 与朝向垂直的方向（用于一字排开）
        double lateralX = forwardZ;
        double lateralZ = -forwardX;

        List<Spot> spots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0D) * LATERAL_SPACING;
            double pullback = Math.abs(offset) * ARC_PULLBACK;
            double x = hostX + forwardX * (standoff - pullback) + lateralX * offset;
            double z = hostZ + forwardZ * (standoff - pullback) + lateralZ * offset;
            // 敌人朝向玩家（面对面）
            spots.add(new Spot(x, z, StandoffLayout.yawTowards(x, z, hostX, hostZ)));
        }
        return List.copyOf(spots);
    }

    /** 落点与玩家的水平距离（自检用）。 */
    public static double horizontalDistance(Spot spot, double hostX, double hostZ) {
        double dx = spot.x() - hostX;
        double dz = spot.z() - hostZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ------------------------------------------------------------------ v0.4.4：以"战斗 UI 栏"为基准排位

    /**
     * 把怪物排到"正好落在各自战斗信息栏下面"的位置（需求："让怪物实体以战斗 UI 栏位置为基准对齐"）。
     *
     * <p>UI 是**基准**：信息栏保持屏幕上整齐等距的固定布局，怪物被摆到与之一一对应的世界坐标上。
     * 关键推导（针孔相机）：</p>
     * <pre>
     *   归一化横坐标 u = 0.5 + lateral / (2 · depth · tan(fov/2) · aspect)
     *   ⇒ lateral = (u − 0.5) · lateralScale，其中 lateralScale = 2 · depth · tan(fov/2) · aspect
     *   depth = standoff · cos(pitch) + eyeHeight · sin(pitch) + cameraDistance
     * </pre>
     *
     * <p>注意 {@code depth} 与横向偏移无关（屏幕右方向与视线垂直），所以所有怪物都摆在**同一进深**上，
     * 只按横向偏移区分 —— 这样投影恰好落在各自的栏位中心。</p>
     *
     * @param normalizedCenters 每个敌人信息栏的归一化中心（0..1，从左到右）
     * @param lateralScale      {@link #lateralScale} 算出来的比例
     */
    public static List<Spot> computeForCenters(double hostX, double hostZ, float hostYaw,
                                               double[] normalizedCenters, double standoff,
                                               double lateralScale) {
        if (normalizedCenters == null || normalizedCenters.length == 0) {
            return List.of();
        }
        double distance = Math.max(standoff, StandoffLayout.MIN_DISTANCE);
        double radians = Math.toRadians(hostYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        // **屏幕向右**的单位向量 = 朝向 × 上方向（MC 约定：yaw 0 面向 +Z 时向右是 -X）
        double rightX = -forwardZ;
        double rightZ = forwardX;

        List<Spot> spots = new ArrayList<>(normalizedCenters.length);
        for (double center : normalizedCenters) {
            // center > 0.5 = 屏幕右半边 → 往屏幕右侧摆
            double lateral = lateralFor(center, lateralScale);
            double x = hostX + forwardX * distance + rightX * lateral;
            double z = hostZ + forwardZ * distance + rightZ * lateral;
            spots.add(new Spot(x, z, StandoffLayout.yawTowards(x, z, hostX, hostZ)));
        }
        return List.copyOf(spots);
    }

    /**
     * 归一化横坐标 → 世界横向偏移（0.5 = 正前方，正值 = 屏幕右侧）。
     *
     * <p>注意这里是沿{@link #screenRightX 屏幕向右}量的（见 {@link #computeForCenters} 内的说明）。</p>
     */
    public static double lateralFor(double normalizedCenter, double lateralScale) {
        return (normalizedCenter - 0.5D) * lateralScale;
    }

    /** 屏幕向右的单位向量 X（yaw 0 面向 +Z 时是 -X）。 */
    public static double screenRightX(float hostYaw) {
        return -Math.cos(Math.toRadians(hostYaw));
    }

    /** 屏幕向右的单位向量 Z。 */
    public static double screenRightZ(float hostYaw) {
        return -Math.sin(Math.toRadians(hostYaw));
    }

    /** 相机到敌人所在平面的进深（pitch 为俯视角，eyeHeight 为眼睛高度，cameraDistance 为第三人称距离）。 */
    public static double cameraDepth(double standoff, double pitchDegrees, double eyeHeight,
                                     double cameraDistance) {
        double pitch = Math.toRadians(Math.max(0.0D, Math.min(89.0D, pitchDegrees)));
        double distance = Math.max(standoff, StandoffLayout.MIN_DISTANCE);
        return distance * Math.cos(pitch) + eyeHeight * Math.sin(pitch) + cameraDistance;
    }

    /** 归一化偏移与世界横向偏移的比例（由进深、垂直 FOV 与宽高比决定）。 */
    public static double lateralScale(double depth, double fovDegrees, double aspect) {
        double fov = Math.max(30.0D, Math.min(170.0D, fovDegrees));
        double ratio = Math.max(0.2D, aspect);
        return 2.0D * Math.max(0.5D, depth) * Math.tan(Math.toRadians(fov) / 2.0D) * ratio;
    }

    /** 反算：把世界横向偏移投影成归一化横坐标（自检里用来验证"摆完正好落在栏位中心"）。 */
    public static double projectCenter(double lateral, double depth, double fovDegrees, double aspect) {
        return 0.5D + lateral / lateralScale(depth, fovDegrees, aspect);
    }

    /** 两个落点之间的水平距离（自检用：必须 ≥ 1 格，否则会叠在一起）。 */
    public static double spacing(Spot a, Spot b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
