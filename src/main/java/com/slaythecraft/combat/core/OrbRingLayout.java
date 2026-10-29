package com.slaythecraft.combat.core;

/**
 * 充能球在玩家腰间的**环形布局**（v0.3.0）。
 *
 * <p>需求原文："我希望这个轨道上的球缩小四分之三的体积，轨道的半径扩大到原来的三分之四，
 * 且修改为：下一个触发打碎的球始终面朝敌人，不用再旋转，只需要每次生成球的时候旋转。"</p>
 *
 * <p>于是这里有三个常量与三条规则：</p>
 * <ul>
 *   <li>{@link #ORB_SCALE}：体积缩到原来的 1/4 → 边长缩到 ∛(1/4) ≈ 0.63；</li>
 *   <li>{@link #RADIUS}：原半径 0.75 的 4/3 → 1.0；</li>
 *   <li>{@link #angleFor}：**队首（下一个会被激发的球）永远朝向敌人**，其余球按
 *       {@link #stepAngle} 依次排在后面，所以静止时不旋转；</li>
 *   <li>{@link #spinOffset}：只有在**生成球**的那一刻，整圈会从"被推后一格"平滑转回原位
 *       （表现成"咔哒转了一格"），转完队首依旧朝向敌人。</li>
 * </ul>
 *
 * <p>放在 core 里是为了让这套几何能进裸 JVM 自检（core 包不允许依赖 Minecraft）。</p>
 */
public final class OrbRingLayout {

    /** 环绕半径（原 0.75 的 4/3）。 */
    public static final double RADIUS = 1.0D;
    /** 腰部高度（相对实体脚底）。 */
    public static final double WAIST_Y = 0.95D;
    /** 方块边长的**基准**比例：体积缩到原来的 1/4 → 边长 ∛(1/4) ≈ 0.63（v0.3.0 的"缩小四分之三"）。 */
    public static final float ORB_SCALE_BASE = 0.63F;

    /**
     * v0.5.0.5 的再缩小系数：**缩小四分之一**（边长 ×0.75）。
     *
     * <p>需求原文："充能球（包括栏位）的体积缩小四分之一，以方便玩家观看（体积过大影响观感）。"</p>
     */
    public static final float ORB_SHRINK = 0.75F;
    /** 方块边长比例（当前生效值）= 基准 × 再缩小系数。 */
    public static final float ORB_SCALE = ORB_SCALE_BASE * ORB_SHRINK;
    /** "生成球时转一格"的动画时长（毫秒）。 */
    public static final long CHANNEL_STEP_MS = 320L;

    private OrbRingLayout() {
    }

    /** 一格的角度（球位数 ≤ 0 时返回 0，避免除零）。 */
    public static double stepAngle(int slots) {
        return slots <= 0 ? 0.0D : Math.PI * 2.0D / slots;
    }

    /**
     * "从玩家指向目标"的方位角。
     *
     * <p>与 {@link #offsetX}/{@link #offsetZ} 配套：角度 0 = +X 方向。</p>
     */
    public static double facingAngle(double dx, double dz) {
        if (dx == 0.0D && dz == 0.0D) {
            return 0.0D;
        }
        return Math.atan2(dz, dx);
    }

    /**
     * 第 {@code index} 颗球的角度。
     *
     * <p>{@code index = 0} 是**下一个会被激发的球**（队首），它永远落在"朝向敌人"的方向上。</p>
     */
    public static double angleFor(int index, int slots, double facing, double spinOffset) {
        return facing + spinOffset + index * stepAngle(slots);
    }

    /**
     * 生成球时的"转一格"偏移。
     *
     * @param nowMillis 当前时间
     * @param startedAt 最近一次生成球的时刻
     * @return 从 {@code -step} 平滑回到 0 的角度偏移（动画结束、或还没生成过球时是 0）
     */
    public static double spinOffset(long nowMillis, long startedAt, int slots) {
        if (startedAt <= 0L) {
            // 这一场还没生成过球：不旋转
            return 0.0D;
        }
        long elapsed = Math.max(0L, nowMillis - startedAt);
        if (elapsed >= CHANNEL_STEP_MS) {
            return 0.0D;
        }
        double remaining = 1.0D - (double) elapsed / CHANNEL_STEP_MS;
        return -stepAngle(slots) * easeOut(remaining);
    }

    /** 世界坐标 X 偏移（相对玩家）。 */
    public static double offsetX(double angle) {
        return Math.cos(angle) * RADIUS;
    }

    /** 世界坐标 Z 偏移（相对玩家）。 */
    public static double offsetZ(double angle) {
        return Math.sin(angle) * RADIUS;
    }

    // ------------------------------------------------------------------ v0.5.0.13 圆心与玩家精确坐标

    /**
     * 环形轨道的**圆心**（世界坐标）—— 需求："充能球的圆心应该始终为玩家当前的精确坐标。"
     *
     * <p>为什么单独抽一个函数：以前圆心是渲染层里手写展开的 {@code px + offsetX(...)}，
     * 用的是**非插值**的 {@code entity.getX()/getZ()}，而玩家模型是用**插值后**的位置渲染的
     * （{@code Entity.getPosition(partialTick)} 走 {@code xOld → x}）。
     * 两者在移动、或被服务端摆位（开战站位）时会对不上，看起来就是球环"挂在身体旁边"。
     * 把圆心变成一个**有名字的、可断言的**量之后，渲染层与自检用的是同一份定义。</p>
     *
     * <p>水平方向就是玩家的精确坐标（{@code position} 在水平方向是包围盒**中心**，
     * 所以 {@code getX()/getZ()} 正是玩家所在的那一格中心）；竖直方向抬到腰部。
     * 传插值后的坐标进来（见 {@code OrbRenderer} 用的 {@code xOld → x} 插值），
     * 圆心就与玩家模型严格重合。</p>
     *
     * @param playerX 玩家精确 X（建议传插值后的值，与模型渲染用同一份）
     * @param playerY 玩家脚底 Y
     * @param playerZ 玩家精确 Z（同上）
     * @return {@code {x, y, z}} —— 三颗球到它的水平距离都等于 {@link #RADIUS}
     */
    public static double[] ringCenter(double playerX, double playerY, double playerZ) {
        return new double[] {playerX, playerY + WAIST_Y, playerZ};
    }

    /**
     * 圆心到第 {@code index} 颗球中心的水平距离（自检用：必须恒等于 {@link #RADIUS}）。
     *
     * <p>这条断言就是"圆心 = 玩家精确坐标"的机器化版本：任一行数、任一旋转偏移下都成立。</p>
     */
    public static double distanceFromCenter(int index, int slots, double facing, double spinOffset) {
        double angle = angleFor(index, slots, facing, spinOffset);
        double dx = offsetX(angle);
        double dz = offsetZ(angle);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * 全部球位相对圆心的平均偏移量。
     *
     * <p>⚠️ **这个量在球位数 &lt; 3 时本来就不为 0**（1 颗球时它就是 {@code (R·cosθ, R·sinθ)}），
     * 所以它**不能**用来断言"圆心居中"——我一开始就这么写错过一次，被自检当场抓出来。
     * 真正的不变量是 {@link #distanceFromCenter}：每颗球到圆心的距离恒等于 {@link #RADIUS}。
     * 这里保留该方法只作为调试 / 观察用。</p>
     *
     * @return {@code {平均 dx, 平均 dz}}
     */
    public static double[] meanOffset(int slots, double facing, double spinOffset) {
        if (slots <= 0) {
            return new double[] {0.0D, 0.0D};
        }
        double sx = 0.0D;
        double sz = 0.0D;
        for (int i = 0; i < slots; i++) {
            double angle = angleFor(i, slots, facing, spinOffset);
            sx += offsetX(angle);
            sz += offsetZ(angle);
        }
        return new double[] {sx / slots, sz / slots};
    }

    /** 缓出曲线：开头快、结尾慢（0 → 0，1 → 1）。 */
    public static double easeOut(double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return 1.0D - Math.pow(1.0D - clamped, 3.0D);
    }
}
