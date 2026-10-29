package com.slaythecraft.client;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 右下角"新卡牌奖励可用"提示（设计文档 §8.4）。
 *
 * <p>只要档案里还有待领奖励就一直显示（带次数），开着别的界面时自动隐藏，避免挡住 UI。</p>
 */
public final class RewardHudLayer implements LayeredDraw.Layer {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "reward_notice");

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        int pending = StcAttachments.dataOf(player).pendingRewards();
        if (pending <= 0) {
            return;
        }
        Component text = Component.translatable("stc.reward.hud");
        // v0.2.3：文字比屏幕还宽时也不能跑到屏幕外（以前 x 会变成负数）
        String trimmed = minecraft.font.width(text) > graphics.guiWidth() - 16
                ? minecraft.font.plainSubstrByWidth(text.getString(), graphics.guiWidth() - 16)
                : text.getString();
        int width = minecraft.font.width(trimmed);
        int x = Math.max(4, graphics.guiWidth() - width - 8);
        int y = graphics.guiHeight() - 26;
        graphics.fill(x - 4, y - 4, Math.min(graphics.guiWidth() - 4, x + width + 4), y + 12, 0xB4000000);
        TextStyle.draw(graphics, minecraft.font, trimmed, x, y, 0xFFFFD24A);
    }
}
