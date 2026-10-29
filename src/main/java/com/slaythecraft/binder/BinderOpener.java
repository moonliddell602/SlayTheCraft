package com.slaythecraft.binder;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**
 * 打开收纳包界面的统一入口：快捷键、右键物品、以及将来引导方块里的"查看卡组"都走这里。
 */
public final class BinderOpener {

    private BinderOpener() {
    }

    /**
     * 打开收纳包 UI。身上任何位置都没有收纳包时只给提示，不打开。
     *
     * <p>v0.4.6.3：判定从 {@code hasBinder}（必须手持）放宽到 {@code carried}（**背包里也算**）——
     * 需求原文："如果卡牌收纳包在背包内，也可以允许 B 键打开 UI。" 三个入口（B 键的网络包、
     * 右键物品、尖塔入口的"查看卡组"按钮）都走这里，所以一处放宽三处同时生效；
     * 「开始尖塔对战」的判定没有动，仍然是必须拿在手上。</p>
     */
    public static void open(ServerPlayer player) {
        if (!BinderResolver.carried(player)) {
            StcChat.tell(player, Component.literal(
                            "[STC] 需要携带【卡牌收纳包】才能打开（当前位置：" + BinderResolver.binderLocation(player)
                                    + "）。" + BinderResolver.equipHint())
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        // 确保客户端手上是最新档案，界面首帧不会显示旧数据
        StcAttachments.syncTo(player);
        // v0.5.1.2：顺手把"开发者模式开关"也同步一次。登录时已经发过一次，但那个时机
        // （PlayerLoggedInEvent）比客户端真正准备好收包要早，万一丢了，图鉴里的
        // 「加入牌组 / 删除」按钮就会**静默消失**。收纳包每次打开都补一次，成本是一个布尔值。
        com.slaythecraft.dev.DevMode.syncTo(player);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> CardBinderMenu.binderMenu(containerId, inventory),
                Component.translatable("stc.binder.title")));
    }
}
