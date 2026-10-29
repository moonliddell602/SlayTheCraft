package com.slaythecraft.villager;

import com.slaythecraft.chat.StcChat;
import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 卡牌大师的调试指令（v0.4.5）：{@code /stc villager cardmaster}。
 *
 * <p>正常玩法里这一步是**原版逻辑**：无业村民走到【尖塔入口】旁边认领工作站点就自动变职业。
 * 但"让村民自己走过去"在测试时太慢，所以给一条指令：把最近的村民直接变成卡牌大师
 * （同时把它的工作站点登记成那个位置，保持与原版一致）。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CardMasterCommands {

    /** 指令搜索半径（格）。 */
    private static final double RADIUS = 16.0D;

    private CardMasterCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("stc")
                .then(Commands.literal("villager")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cardmaster").executes(CardMasterCommands::makeCardMaster))));
    }

    private static int makeCardMaster(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(RADIUS));
        if (villagers.isEmpty()) {
            StcChat.failure(context.getSource(), Component.literal("[STC] " + (int) RADIUS + " 格内没有村民。"));
            return 0;
        }
        villagers.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        Villager target = villagers.get(0);
        target.setVillagerData(target.getVillagerData()
                .setProfession(StcVillagers.CARD_MASTER.get())
                .setType(CardMasterEvents.LOOK));
        // 把脚下的方块登记成它的工作站点（原版村民也是这么认领工作站的）
        level.getPoiManager().add(target.blockPosition(), StcVillagers.CARD_MASTER_POI);
        StcChat.feedback(context.getSource(), () -> Component.literal("[STC] "
                + target.getName().getString() + " 已变成【卡牌大师】（右键他打开尖塔入口）。"));
        return 1;
    }

    /** 这个村民职业是不是卡牌大师（其它指令/界面复用）。 */
    public static boolean isCardMaster(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession == StcVillagers.CARD_MASTER.get();
    }
}
