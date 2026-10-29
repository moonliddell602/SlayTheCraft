package com.slaythecraft.client;

import com.slaythecraft.campfire.CampfireMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 篝火「锻造」选卡页（v0.0.3 W5，独立新页面）。
 *
 * <p>复用通用卡牌列表页，只是动作换成"升级"：点一张卡 → 详情页里有「锻造这张卡」→ 直接升级。</p>
 */
public class CampfireForgeScreen extends CardPickScreen<CampfireMenu> {

    public CampfireForgeScreen(CampfireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Action.UPGRADE,
                Component.translatable("stc.campfire.forge.hint").getString(),
                index -> send(menu, index),
                () -> new CampfireScreen(menu, inventory, title));
    }

    private static void send(CampfireMenu menu, int index) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CampfireMenu.UPGRADE_BASE + index);
        }
    }
}
