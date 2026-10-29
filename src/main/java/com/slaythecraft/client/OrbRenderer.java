package com.slaythecraft.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Orb;
import com.slaythecraft.combat.core.OrbRingLayout;
import com.slaythecraft.network.BattleView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 充能球的**世界内渲染**（v0.0.3 W4；v0.3.0 按测试反馈重做）。
 *
 * <p>需求原文："显示为方块，环绕在玩家身边（腰部环绕，以标靶代替空栏位，蓝冰表示冰球，
 * 黄色蛙鸣灯表示闪电球）"；v0.3.0 追加："球缩小四分之三的体积，轨道的半径扩大到原来的三分之四，
 * 且修改为：下一个触发打碎的球始终面朝敌人，不用再旋转，只需要每次生成球的时候旋转。"</p>
 *
 * <p>因此：</p>
 * <ul>
 *   <li>球是**边长 0.63 的方块**（体积缩到 1/4，见 {@link OrbRingLayout#ORB_SCALE}），
 *       轨道半径 1.0（原 0.75 的 4/3），高度在腰部；</li>
 *   <li>队首（下一个会被激发的球）永远排在**朝向最近敌人**的方向；</li>
 *   <li>静止时不旋转；只有**生成新球**的那一刻，整圈会"咔哒"转回一格（{@link OrbRingLayout#spinOffset}）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID, value = Dist.CLIENT)
public final class OrbRenderer {

    /** 最近一次"生成球"的时间戳（0 = 这一场还没生成过）。 */
    private static long lastChannelAt;
    /** 上一次看到的球位内容，用来判断"是不是刚生成了新球"。 */
    private static List<String> lastOrbs = List.of();

    /**
     * 每颗球（含空栏位）在**屏幕上的位置**（v0.4.6）：
     * {@code {屏幕 x, 屏幕 y, orbId 或 null}} —— HUD 用它做"悬停 0.6 秒看说明"的命中判定。
     */
    private static volatile List<OrbHit> hits = List.of();

    /** 一颗球在屏幕上的命中信息。 */
    public record OrbHit(double x, double y, int radius, String orbId) {
    }

    /** 最近一帧算出的球屏幕位置（没有战斗时是空表）。 */
    public static List<OrbHit> hits() {
        return hits;
    }

    private OrbRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        BattleView view = ClientBattleState.view();
        if (!view.active()) {
            reset();
            return;
        }
        int slots = Math.max(0, view.orbSlots());
        if (slots == 0) {
            return;
        }
        List<String> orbs = view.selfOrbs();
        long now = System.currentTimeMillis();
        if (channeled(orbs)) {
            lastChannelAt = now;
        }
        lastOrbs = List.copyOf(orbs);

        double facing = facingAngle(player, view);
        double spin = OrbRingLayout.spinOffset(now, lastChannelAt, slots);

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        var camera = event.getCamera().getPosition();

        // ---- v0.5.0.13：圆心 = 玩家**插值后**的精确坐标（需求："充能球的圆心应该始终为玩家当前的精确坐标。"）
        //
        // 以前这里直接用 player.getX()/getZ()（非插值），而玩家模型是用插值位置渲染的
        // （Entity.getPosition(partialTick) 走 xOld → x）。移动中、或被服务端摆位（开战站位）时
        // 球环会落后于模型，看起来就是"错位"。这里改成与模型同一份插值坐标。
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double playerX = net.minecraft.util.Mth.lerp((double) partialTick, player.xOld, player.getX());
        double playerZ = net.minecraft.util.Mth.lerp((double) partialTick, player.zOld, player.getZ());
        double[] center = OrbRingLayout.ringCenter(playerX, player.getY(), playerZ);
        double px = center[0];
        double py = center[1];
        double pz = center[2];

        // v0.4.6：顺便把每颗球投影到屏幕上（HUD 悬停提示用）
        List<OrbHit> screenHits = new java.util.ArrayList<>(slots);
        org.joml.Matrix4f viewProjection = new org.joml.Matrix4f(event.getProjectionMatrix())
                .mul(new org.joml.Matrix4f(event.getModelViewMatrix()));

        for (int i = 0; i < slots; i++) {
            double angle = OrbRingLayout.angleFor(i, slots, facing, spin);
            double x = px + OrbRingLayout.offsetX(angle);
            double z = pz + OrbRingLayout.offsetZ(angle);
            BlockState state = i < orbs.size() ? blockFor(orbs.get(i)) : emptySlot();

            // 投影到屏幕：方块中心（世界坐标 → 裁剪空间 → 屏幕像素）
            org.joml.Vector4f clip = new org.joml.Vector4f(
                    (float) (x - camera.x),
                    (float) (py - camera.y),
                    (float) (z - camera.z), 1.0F);
            viewProjection.transform(clip);
            if (clip.w > 0.0001F) {
                double sx = (clip.x / clip.w * 0.5D + 0.5D) * minecraft.getWindow().getGuiScaledWidth();
                double sy = (1.0D - (clip.y / clip.w * 0.5D + 0.5D)) * minecraft.getWindow().getGuiScaledHeight();
                int radius = Math.max(6, (int) Math.round(9.0D * OrbRingLayout.ORB_SCALE
                        / Math.max(0.4D, clip.w)));
                screenHits.add(new OrbHit(sx, sy, radius, i < orbs.size() ? orbs.get(i) : null));
            }

            pose.pushPose();
            pose.translate(x - camera.x, py - camera.y, z - camera.z);
            // 需求：缩小体积 —— 方块以自己中心为基准缩放，边长 = ORB_SCALE
            float scale = OrbRingLayout.ORB_SCALE;
            pose.translate(0.5D * (1.0D - scale), 0.5D * (1.0D - scale), 0.5D * (1.0D - scale));
            pose.scale(scale, scale, scale);
            minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffer, 15728880,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffer.endBatch();
        hits = List.copyOf(screenHits);
    }

    /** 球的列表变了（多了一颗或少了）—— 生成新球时转一格。 */
    private static boolean channeled(List<String> orbs) {
        return orbs.size() > lastOrbs.size() || (orbs.size() == lastOrbs.size() && !orbs.equals(lastOrbs));
    }

    /** 朝向"最近的敌人"：用战斗快照里的敌人名字去附近找实体；找不到就退回玩家自己的朝向。 */
    private static double facingAngle(Player player, BattleView view) {
        Entity target = nearestEnemy(player, view, 32.0D);
        if (target != null) {
            return OrbRingLayout.facingAngle(target.getX() - player.getX(), target.getZ() - player.getZ());
        }
        double yaw = Math.toRadians(player.getYRot());
        return OrbRingLayout.facingAngle(-Math.sin(yaw), Math.cos(yaw));
    }

    /**
     * 最近的"参战敌人"。
     *
     * <p>判断依据是**原版就会同步给客户端的自定义名字**：战斗中的怪物头顶写着
     * {@code 名字：动作类型}（例如 {@code 僵尸：攻击}，见 {@code MobCombatant.syncIntentLabel}），
     * 而快照里带着这一场敌人的名字，两边一比对就能准确筛出参战的那几只，不会误锁路过的其它生物。</p>
     *
     * <p>⚠️ 判定用的是 {@code startsWith}，所以头顶文案**前几个字必须是名字** —— v0.5.1.2 把头顶
     * 从"名字 + 一长串带数值的意图"改成"名字 + 动作类型"时，这里没受影响（前缀没动）。</p>
     */
    private static Entity nearestEnemy(Player player, BattleView view, double radius) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (BattleView.EnemyView enemy : view.enemies()) {
            if (enemy.alive()) {
                names.add(enemy.name());
            }
        }
        if (names.isEmpty()) {
            return null;
        }
        Entity best = null;
        double bestDistance = radius * radius;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive()
                        && candidate.getCustomName() != null)) {
            String name = entity.getCustomName().getString();
            boolean known = false;
            for (String enemyName : names) {
                if (name.startsWith(enemyName)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                continue;
            }
            double distance = entity.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private static void reset() {
        lastChannelAt = 0L;
        lastOrbs = List.of();
        hits = List.of();
    }

    /** 球 → 方块（id 认不出来时退回冰球外观，保证永远看得见）。 */
    private static BlockState blockFor(String orbId) {
        try {
            Orb orb = Orb.valueOf(orbId);
            BlockState state = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.parse(orb.blockId())).map(block -> block.defaultBlockState())
                    .orElse(null);
            if (state != null) {
                return state;
            }
        } catch (IllegalArgumentException ignored) {
            // 未知球类型：用冰球外观
        }
        return Blocks.BLUE_ICE.defaultBlockState();
    }

    /** 空栏位：标靶方块。 */
    private static BlockState emptySlot() {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(Orb.EMPTY_SLOT_BLOCK))
                .map(block -> block.defaultBlockState())
                .orElse(Blocks.TARGET.defaultBlockState());
    }
}
