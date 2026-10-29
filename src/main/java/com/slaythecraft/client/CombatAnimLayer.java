package com.slaythecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.BattleAnim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 战斗动作的**渲染层**（v0.3.0）：在实体被画出来之前，按当前动画平移/缩放一下模型。
 *
 * <p>只改渲染矩阵，不动实体真实坐标：</p>
 * <ul>
 *   <li>攻击（玩家与敌人一样）→ 沿朝向向前冲出去再收回来；</li>
 *   <li>防御 → 往下沉一点并轻微压扁（蹲起）；</li>
 *   <li>受击 → 沿朝向向后一步再快速回位。</li>
 * </ul>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID, value = Dist.CLIENT)
public final class CombatAnimLayer {

    private CombatAnimLayer() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        // 只处理战斗中的实体（避免给路过的小怪也加动画）
        if (!ClientBattleState.view().active()) {
            return;
        }
        ClientBattleFx.Animation animation = ClientBattleFx.animationOf(entity.getId());
        if (animation == null) {
            return;
        }
        long now = System.currentTimeMillis();
        PoseStack pose = event.getPoseStack();
        switch (animation.kind()) {
            case PLAYER_ATTACK, ENEMY_ATTACK -> {
                // 用动画自己的时长：多段攻击的每一段更短（= 快四分之一，见 BattleAnim.multiHitMs）
                float t = BattleAnim.strike(now, animation.startedAt(), animation.duration());
                translateAlongLook(pose, entity, t * BattleAnim.ATTACK_DISTANCE);
            }
            case PLAYER_BLOCK, ENEMY_BLOCK -> {
                // v0.4.1：敌人获得格挡也是"蹲起一次"（和玩家同一套）
                float t = BattleAnim.crouch(now, animation.startedAt());
                pose.translate(0.0D, -t * BattleAnim.CROUCH_DEPTH, 0.0D);
                // 起点在脚底：轻微纵向压扁看起来就是"蹲下去"
                pose.scale(1.0F, 1.0F - 0.25F * t, 1.0F);
            }
            case PLAYER_HURT -> {
                float t = BattleAnim.knockback(now, animation.startedAt(), animation.duration());
                translateAlongLook(pose, entity, -t * BattleAnim.HURT_DISTANCE);
            }
            case ENEMY_HURT -> {
                float t = BattleAnim.knockback(now, animation.startedAt(), animation.duration());
                translateAlongLook(pose, entity, -t * BattleAnim.HURT_DISTANCE * 0.5D);
            }
            case ENEMY_CAST -> {
                // v0.5.0.5：敌人施法——原地轻微下沉再起身（配合唤魔者音效 + 药水粒子）
                float t = BattleAnim.crouch(now, animation.startedAt());
                pose.translate(0.0D, -t * BattleAnim.CROUCH_DEPTH * 0.6D, 0.0D);
            }
            case ENEMY_EXPLODE -> {
                // v0.5.0.8：自爆——先鼓起再缩下去（配合爆炸音效 + 爆炸粒子）
                float t = (float) BattleAnim.progress(now, animation.startedAt(), animation.duration());
                if (t < 0.4F) {
                    float grow = t / 0.4F;
                    pose.scale(1.0F + 0.25F * grow, 1.0F + 0.25F * grow, 1.0F + 0.25F * grow);
                } else {
                    float shrink = 1.0F - (t - 0.4F) / 0.6F;
                    pose.scale(Math.max(0.05F, shrink), Math.max(0.05F, shrink), Math.max(0.05F, shrink));
                }
            }
        }
        // 只在客户端主线程渲染时使用；顺带清掉过期的动画
        if (Minecraft.getInstance().level != null) {
            ClientBattleFx.tick();
        }
    }

    /**
     * 沿实体**朝向**平移（水平方向）。
     *
     * <p>MC 的 yaw 约定：yaw = 0 面向 +Z，朝向向量是 {@code (-sin yaw, 0, cos yaw)}。</p>
     *
     * @param forward 正数 = 向前，负数 = 向后
     */
    private static void translateAlongLook(PoseStack pose, LivingEntity entity, double forward) {
        if (forward == 0.0D) {
            return;
        }
        double yaw = Math.toRadians(entity.getYRot());
        double dx = -Math.sin(yaw) * forward;
        double dz = Math.cos(yaw) * forward;
        pose.translate(dx, 0.0D, dz);
    }
}
