package com.slaythecraft.combat.mc;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.Config;
import com.slaythecraft.chat.StcChat;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.reward.RewardService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives battles from the game's tick loop and keeps other systems from interfering with a fight
 * that is supposed to be resolved by cards only.
 */
// NeoForge 21.1 起 EventBusSubscriber.bus() 已标记待删除：GAME 就是默认总线，不再显式指定。
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CombatEvents {

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatManager.quit(player, "玩家离线");
        }
    }

    /**
     * **重进存档时取消未正常结束的战斗**（v0.5.0.20）。
     *
     * <p>反馈："玩家意外关闭了游戏后，重启游戏并开启存档会出现立即死亡的情况。正解：取消最后的战斗。"
     * 强制关闭时 {@link #onLoggedOut} 里的收尾跑不完（它最后会击杀玩家），
     * 而"本局已战败"标记已经落盘 —— 重进就像被处决。这里把它撤销掉。</p>
     */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String message = CombatManager.recoverFromInterruptedBattle(player);
        if (message != null) {
            StcChat.note(player, message);
        }
    }

    /**
     * 伤害拦截（v0.0.2 V3 + 测试反馈修正）：
     * <ol>
     *   <li>参战玩家：**一切外来伤害**都拦下（不会被局外事物打死）；</li>
     *   <li>还有待领战斗奖励时同样拦下（反馈："在完成奖励前应该保持玩家不被攻击"）；</li>
     *   <li>其余情况：来自被战斗锁定的生物的原版伤害同样拦下。</li>
     * </ol>
     * 引擎结算走 {@code setHp} 不经过 {@code hurt}，所以拦下 {@code hurt} 不影响战斗数值；
     * 独立生命值也与原版血量无关（v0.2.1 起完全解耦）。
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (CombatManager.inCombat(player) && Config.immunityDuringCombat()) {
                event.setCanceled(true);
                return;
            }
            // 奖励界面弹出期间不受伤害（战斗结束后立刻进奖励，领完才继续）
            if (StcAttachments.dataOf(player).pendingRewards() > 0) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof ServerPlayer && CombatManager.isLocked(event.getSource().getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * 僵死锁定的自愈（v0.2.3）：实体带着"战斗锁定"标记进入世界、却已经不属于任何战斗时，
     * 立刻解除冻结。覆盖"服务器在战斗中崩溃/异常退出"这种把 {@code NoAI} 存进存档的情况。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!mob.getPersistentData().getBoolean(MobCombatant.LOCK_TAG)) {
            return;
        }
        if (CombatManager.isLocked(mob)) {
            return;
        }
        mob.getPersistentData().remove(MobCombatant.LOCK_TAG);
        mob.setInvulnerable(false);
        mob.setNoAi(false);
        SlayTheCraft.LOGGER.debug("[STC] 清理僵死的战斗冻结：{}", mob.getName().getString());
    }

    /**
     * 待领奖励没领完时，隔一段时间自动把三选一界面再弹一次（测试反馈：奖励入口移到战斗结束后，
     * 玩家按 ESC 关掉也有办法回来）。
     */
    @SubscribeEvent
    public static void onPlayerTickReward(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 60 != 0 || CombatManager.inCombat(player)) {
            return;
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.pendingRewards() <= 0) {
            return;
        }
        // 已经在别的容器/界面里就别打断（玩家自己开着的界面优先）
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }
        RewardService.openPendingRewards(player);
    }
}
