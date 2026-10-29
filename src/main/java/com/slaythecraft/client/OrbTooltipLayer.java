package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Orb;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 充能球的**悬停说明**（v0.4.6 需求："将鼠标悬停在充能球上持续 0.6 秒，可以查看充能球功能描述"）。
 *
 * <p>球本身是渲染在世界里的方块（见 {@link OrbRenderer}），所以这里读它每帧算出的**屏幕位置**
 * 做命中判定：鼠标停在某颗球上一段时间后，在鼠标旁边弹出它的功能描述。</p>
 *
 * <p>做成 HUD 层的好处：出牌界面开着 / 收起来（ESC 看世界）都能用。</p>
 */
public final class OrbTooltipLayer implements LayeredDraw.Layer {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "orb_tooltip");

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientBattleState.view().active()) {
            HoverTracker.update("");
            return;
        }
        List<OrbRenderer.OrbHit> hits = OrbRenderer.hits();
        if (hits.isEmpty()) {
            HoverTracker.update("");
            return;
        }
        int mouseX = (int) minecraft.mouseHandler.xpos() * graphics.guiWidth() / minecraft.getWindow().getWidth();
        int mouseY = (int) minecraft.mouseHandler.ypos() * graphics.guiHeight() / minecraft.getWindow().getHeight();

        OrbRenderer.OrbHit hovered = null;
        for (OrbRenderer.OrbHit hit : hits) {
            double dx = mouseX - hit.x();
            double dy = mouseY - hit.y();
            if (dx * dx + dy * dy <= (double) hit.radius() * hit.radius()) {
                hovered = hit;
                break;
            }
        }

        String key = hovered == null ? "" : "orb:" + hovered.orbId() + ":" + (int) hovered.x();
        if (!HoverTracker.update(key) || hovered == null) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        if (hovered.orbId() == null) {
            lines.add(Component.literal(Component.translatable("stc.battle.orb.empty").getString()));
            lines.add(Component.literal(Orb.emptySlotDescription()));
        } else {
            Orb orb = parse(hovered.orbId());
            if (orb == null) {
                return;
            }
            lines.add(Component.literal(orb.label()));
            for (String line : BinderStyle.wrap(minecraft.font, orb.description(), 170)) {
                lines.add(Component.literal(line));
            }
        }
        graphics.renderTooltip(minecraft.font, lines,
                java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
    }

    private static Orb parse(String orbId) {
        try {
            return Orb.valueOf(orbId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
