package com.slaythecraft.combat.mc;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.StandoffLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 把 {@link StandoffLayout} 算出来的坐标落到真实地形上（v0.0.2 V3）。
 *
 * <p>摆位是"改变玩家位置"的操作，必须保守：找不到安全落点就**不传送**，只把玩家转向敌人，
 * 绝不允许把玩家塞进墙里/岩浆里。</p>
 */
public final class PlacementHelper {

    private PlacementHelper() {
    }

    /** 安全落点搜索范围（上下各 4 格）。 */
    private static final int VERTICAL_SEARCH = 4;

    /**
     * 把玩家摆到指定落点并转向敌人。
     *
     * @return 是否真的传送了（false = 没找到安全落点，只转了向）
     */
    public static boolean place(ServerPlayer player, StandoffLayout.Placement placement) {
        ServerLevel level = player.serverLevel();
        BlockPos target = findSafeSpot(level, placement.x(), placement.z(), player.blockPosition().getY());
        if (target == null) {
            face(player, placement.yaw());
            SlayTheCraft.LOGGER.debug("[STC] 没有找到安全落点，{} 保持原位只转向", player.getScoreboardName());
            return false;
        }
        player.teleportTo(level, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, placement.yaw(), 0.0F);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        return true;
    }

    /** 只转向不移动（落点不安全时的兜底）。 */
    public static void face(ServerPlayer player, float yaw) {
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(0.0F);
    }

    /** 让玩家原地转身面向某个水平坐标（测试反馈修正：不再强制传送玩家）。 */
    public static void faceTowards(ServerPlayer player, double targetX, double targetZ) {
        float yaw = StandoffLayout.yawTowards(player.getX(), player.getZ(), targetX, targetZ);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
    }

    /**
     * 把怪物摆到指定落点并让它面朝玩家（v0.0.2 测试反馈修正）。
     *
     * <p>和玩家一样保守：找不到安全落点就**不动它**（返回 false），绝不会把怪塞进墙里或岩浆里。</p>
     *
     * @param spot 由 {@link com.slaythecraft.combat.core.EnemyFormation} 算出的落点（含面向角度）
     * @return 是否真的传送了
     */
    public static boolean placeMob(Mob mob, com.slaythecraft.combat.core.EnemyFormation.Spot spot) {
        if (mob.isRemoved() || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos target = findSafeSpot(level, spot.x(), spot.z(), mob.blockPosition().getY());
        if (target == null) {
            SlayTheCraft.LOGGER.debug("[STC] 没有找到安全落点，{} 保持原位", mob.getName().getString());
            return false;
        }
        mob.getNavigation().stop();
        mob.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        mob.setYRot(spot.yaw());
        mob.setYHeadRot(spot.yaw());
        mob.setXRot(0.0F);
        mob.setDeltaMovement(0.0D, 0.0D, 0.0D);
        mob.fallDistance = 0.0F;
        // 有些怪（例如劫掠兽）被传送后会立刻想走过去：站位是"面对面"，所以再把导航掐一次
        mob.getNavigation().stop();
        return true;
    }

    /**
     * 在 (x,z) 附近找"脚下实心、头顶两格空、不是流体"的落脚点。
     * 优先用地表高度，往下/往上各试几格（覆盖室内与洞穴）。
     */
    static BlockPos findSafeSpot(ServerLevel level, double x, double z, int referenceY) {
        BlockPos column = BlockPos.containing(x, referenceY, z);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        for (int dy = 0; dy >= -VERTICAL_SEARCH; dy--) {
            BlockPos feet = surface.offset(0, dy, 0);
            if (isStandable(level, feet)) {
                return feet;
            }
        }
        for (int dy = 1; dy <= VERTICAL_SEARCH; dy++) {
            BlockPos feet = surface.offset(0, dy, 0);
            if (isStandable(level, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockState floor = level.getBlockState(feet.below());
        if (!floor.isSolidRender(level, feet.below())) {
            return false;
        }
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }
}
