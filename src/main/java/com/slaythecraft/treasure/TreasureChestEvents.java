package com.slaythecraft.treasure;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.BinderResolver;
import com.slaythecraft.chat.StcChat;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 藏宝箱事件（v0.5.3.0）。
 *
 * <p>需求原文：</p>
 * <pre>
 * 新事件类型 藏宝箱
 * 触发条件：副手 卡牌收纳包 主手 任意类型的镐
 * 目标方块：陷阱箱
 * 效果：奖励一个随机非特殊遗物。
 * 同样的，藏宝箱和篝火一样不能被连续使用。
 * </pre>
 *
 * <p>"不能连续使用"复用了篝火那套 {@link com.slaythecraft.combat.core.ProgressState#isConsecutive}：
 * 每次开箱都会把 {@code lastEvent} 记成 {@link EventKind#TREASURE}，所以连开第二个会被拒；
 * 而它**不占每层次数上限**（{@link EventKind#capped()} 为 false），因为需求没给上限。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class TreasureChestEvents {

    private TreasureChestEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.TRAPPED_CHEST)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean pickaxe = main.is(net.minecraft.tags.ItemTags.PICKAXES);
        boolean binder = off.is(SlayTheCraft.CARD_BINDER.get());
        if (!pickaxe || !binder) {
            // 只有"看起来像要开藏宝箱"的时候才提示，免得拿镐挖箱子时被刷屏
            if (pickaxe || binder) {
                StringBuilder missing = new StringBuilder();
                if (!binder) {
                    missing.append("副手【卡牌收纳包】");
                }
                if (!pickaxe) {
                    if (missing.length() > 0) {
                        missing.append(" + ");
                    }
                    missing.append("主手【任意镐】");
                }
                StcChat.tell(player, Component.literal(
                        "[STC] 藏宝箱需要：副手【卡牌收纳包】+ 主手【任意镐】（还缺 " + missing + "）"));
            }
            return;
        }
        String reason = blockReason(player);
        if (reason != null) {
            StcChat.tell(player, Component.literal("[STC] " + reason));
            player.displayClientMessage(Component.literal("[STC] " + reason), true);
            SlayTheCraft.LOGGER.info("[STC] {} 的藏宝箱被拒绝：{}", player.getScoreboardName(), reason);
            return;
        }
        // 挡住原版右键（否则会打开陷阱箱的容器界面）
        event.setCanceled(true);
        String message = open(player, event.getPos());
        if (message != null) {
            StcChat.tell(player, Component.literal("[STC] " + message));
        }
    }

    /** 能不能开藏宝箱；{@code null} = 可以。 */
    public static String blockReason(ServerPlayer player) {
        if (CombatManager.inCombat(player)) {
            return "战斗中不能开藏宝箱（先结束这场战斗）。";
        }
        if (com.slaythecraft.Config.requireBinderToStart()
                && !BinderResolver.hasBinder(player)) {
            return "藏宝箱需要：副手【卡牌收纳包】+ 主手【任意镐】（当前位置："
                    + BinderResolver.binderLocation(player) + "）。" + BinderResolver.equipHint();
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        if (data.progress().isConsecutive(EventKind.TREASURE)) {
            return "不能连续开藏宝箱：中间要先经历一次其它事件（战斗 / 精英 / 问号 / 篝火）。";
        }
        // v0.5.5.1：藏宝箱**每层上限 5 次**（用户要求，并计入事件统计）
        com.slaythecraft.combat.core.EventCaps caps = com.slaythecraft.Config.eventCaps();
        if (!data.progress().canTrigger(EventKind.TREASURE, caps)) {
            return "本层的藏宝箱已经开完（" + data.progress().countOf(EventKind.TREASURE) + "/"
                    + caps.capOf(EventKind.TREASURE) + "），进入下一层后重置。";
        }
        return null;
    }

    /**
     * 开箱：给一件随机**非特殊**遗物（排除初始遗物、事件限定、以及"只能商店"的那三件）。
     *
     * @return 给玩家看的一句话
     */
    public static String open(ServerPlayer player, BlockPos pos) {
        PlayerCardData data = StcAttachments.dataOf(player);
        Relic relic = randomFromChest(player, data);
        if (relic == null) {
            // 全部拿完了：仍然记一次事件，免得玩家反复点（也符合"不能连用"）
            data.recordEvent(EventKind.TREASURE, com.slaythecraft.Config.eventCaps());
            StcAttachments.syncTo(player);
            return "藏宝箱里已经没有你还没拿过的遗物了。";
        }
        String extra = data.grantRelic(relic.id());
        // v0.5.3.0：藏宝箱本身也走"获得遗物"的通用结算（破旧打火石那类效果）
        String onObtain = com.slaythecraft.relic.RelicObtainService.apply(player, relic.id());
        data.recordEvent(EventKind.TREASURE, com.slaythecraft.Config.eventCaps());
        StcAttachments.syncTo(player);
        SlayTheCraft.LOGGER.info("[STC] {} 在藏宝箱 {} 开出了遗物 {}", player.getScoreboardName(), pos,
                relic.id());
        StringBuilder message = new StringBuilder("藏宝箱：【" + relic.name() + "】—— " + relic.description());
        if (!extra.isEmpty()) {
            message.append("（").append(extra).append("）");
        }
        if (!onObtain.isEmpty()) {
            message.append("（").append(onObtain).append("）");
        }
        return message.toString();
    }

    /** 从藏宝箱池里抽一件还没拥有的遗物（全部拿完返回 {@code null}）。 */
    private static Relic randomFromChest(ServerPlayer player, PlayerCardData data) {
        java.util.List<Relic> pool = RelicLibrary.chestPool();
        java.util.List<Relic> candidates = new java.util.ArrayList<>();
        for (Relic relic : pool) {
            if (!data.hasRelic(relic.id())) {
                candidates.add(relic);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int index = player.getRandom().nextInt(candidates.size());
        return candidates.get(index);
    }
}
