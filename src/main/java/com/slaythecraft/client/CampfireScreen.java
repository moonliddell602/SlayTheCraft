package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.campfire.CampfireMenu;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 篝火准备界面（v0.0.3 W5）：三个选项 + 等待名单。
 *
 * <p>多人时必须**所有同队玩家都在准备界面**才能开始（等待名单会列出还没来的人，按钮置灰）。</p>
 */
public class CampfireScreen extends AbstractContainerScreen<CampfireMenu> {

    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int DANGER = 0xFFD05A5A;

    private static final int BUTTON_X = 24;
    private static final int BUTTON_W = 184;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_Y = 74;
    private static final int BUTTON_GAP = 24;

    public CampfireScreen(CampfireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 232;
        // v0.5.2.5：加高一点，给"折行后的底部说明"留位置（原来 190 时说明被截断）
        this.imageHeight = 196;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x2 = leftPos + imageWidth;
        int y2 = topPos + imageHeight;
        graphics.fill(leftPos - 2, topPos - 2, x2 + 2, y2 + 2, PANEL_EDGE);
        graphics.fill(leftPos, topPos, x2, y2, PANEL_BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ClientCampfireState.Snapshot state = ClientCampfireState.state();
        // v0.5.2.5：本层次数用完 → 按钮一律置灰（服务端也会拒绝，这里只是别让玩家白点）
        boolean quotaLeft = usedCount() < capCount();
        boolean ready = (state == null || state.ready()) && quotaLeft;

        Component title = Component.translatable("stc.campfire.title");
        TextStyle.draw(graphics, font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 10, GOLD);
        PlayerCardData data = BinderStyle.data();
        if (data != null) {
            Component hp = Component.translatable("stc.campfire.hp", data.life().describe());
            TextStyle.draw(graphics, font, hp, leftPos + 12, topPos + 24, TEXT);
        }
        if (state != null) {
            Component used = Component.translatable("stc.campfire.uses", usedCount(), capCount());
            TextStyle.draw(graphics, font, used, leftPos + 12, topPos + 36, quotaLeft ? TEXT_DIM : DANGER);
            Component status = !quotaLeft
                    ? Component.translatable("stc.campfire.exhausted")
                    : (state.ready()
                            ? Component.translatable("stc.campfire.ready")
                            : Component.translatable("stc.campfire.waiting", String.join("、", state.waiting())));
            TextStyle.draw(graphics, font, trim(status.getString(), imageWidth - 24), leftPos + 12, topPos + 50,
                    !quotaLeft || !state.ready() ? DANGER : GOLD);
        }

        boolean hasAllies = state != null && !state.allies().isEmpty();
        // v0.4.5 修复：绘制坐标必须加上 topPos（以前用的是相对坐标，和点击检测差了一个 topPos，按钮"错位"）
        // v0.5.1.2 美术需求："篝火 3 个（休息=白色床、锻造=铁砧、愈合=金苹果）"。
        // 三个按钮依次是 愈合 / 锻造 / 多人包扎，对应 金苹果 / 铁砧 / 白色床。
        button(graphics, topPos + BUTTON_Y, net.minecraft.world.item.Items.GOLDEN_APPLE,
                Component.translatable("stc.campfire.heal").getString(), ready);
        button(graphics, topPos + BUTTON_Y + BUTTON_GAP, net.minecraft.world.item.Items.ANVIL,
                Component.translatable("stc.campfire.forge").getString(), ready);
        button(graphics, topPos + BUTTON_Y + BUTTON_GAP * 2, net.minecraft.world.item.Items.WHITE_BED,
                Component.translatable("stc.campfire.bandage").getString(), ready && hasAllies);

        // v0.5.2.5 修："下方描述文字显示不全" —— 以前是 trim 成一行直接截断，
        // 现在按面板宽度**折行**，最多 3 行，从面板底部往上排。
        Component hint = Component.translatable("stc.campfire.hint");
        drawWrapped(graphics, hint.getString(), leftPos + 12, topPos + imageHeight - 10, imageWidth - 24, 3);
    }

    /** 按宽度折行绘制（最多 {@code maxLines} 行，从 {@code bottomY} 往上排）。 */
    private void drawWrapped(GuiGraphics graphics, String text, int x, int bottomY, int maxWidth, int maxLines) {
        java.util.List<com.slaythecraft.combat.core.TextLayout.Range> lines =
                com.slaythecraft.combat.core.TextLayout.wrapRanges(text, maxWidth, font::width);
        int count = Math.min(lines.size(), maxLines);
        int y = bottomY - count * 12;
        for (int i = 0; i < count; i++) {
            TextStyle.draw(graphics, font, lines.get(i).substring(text), x, y, TEXT_DIM);
            y += 12;
        }
    }

    /** 本层火堆已用次数 / 上限（客户端自己读档案与配置，不需要额外的网络数据）。 */
    private int usedCount() {
        PlayerCardData data = BinderStyle.data();
        return data == null ? 0 : data.progress().countOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE);
    }

    private int capCount() {
        return com.slaythecraft.Config.eventCaps().capOf(com.slaythecraft.combat.core.EventKind.CAMPFIRE);
    }

    /** 一个行动按钮：左侧小图标 + 居中标题（v0.5.1.2 加图标）。 */
    private void button(GuiGraphics graphics, int y, net.minecraft.world.item.Item icon, String label,
                        boolean enabled) {
        int x = leftPos + BUTTON_X;
        graphics.fill(x, y, x + BUTTON_W, y + BUTTON_H, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + BUTTON_W, y + BUTTON_H, enabled ? PANEL_EDGE : 0xFF33333C);
        ItemIcons.draw(graphics, icon, x + 6, y + (BUTTON_H - ItemIcons.NATIVE) / 2, ItemIcons.NATIVE);
        TextStyle.draw(graphics, font, label, x + (BUTTON_W - font.width(label)) / 2, y + (BUTTON_H - 8) / 2,
                enabled ? TEXT : TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        ClientCampfireState.Snapshot state = ClientCampfireState.state();
        // v0.5.2.5：本层次数用完就什么都点不动（服务端同样会拒绝）
        boolean ready = (state == null || state.ready()) && usedCount() < capCount();
        if (!ready || relX < BUTTON_X || relX >= BUTTON_X + BUTTON_W) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (relY >= BUTTON_Y && relY < BUTTON_Y + BUTTON_H) {
            click(CampfireMenu.BUTTON_HEAL);
            return true;
        }
        if (relY >= BUTTON_Y + BUTTON_GAP && relY < BUTTON_Y + BUTTON_GAP + BUTTON_H) {
            // 锻造 → **新开选卡页面**（v0.2.6 约定）
            minecraft.setScreen(new CampfireForgeScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        if (relY >= BUTTON_Y + BUTTON_GAP * 2 && relY < BUTTON_Y + BUTTON_GAP * 2 + BUTTON_H) {
            if (state != null && !state.allies().isEmpty()) {
                minecraft.setScreen(new CampfireAllyScreen(menu, minecraft.player.getInventory(), title,
                        state.allies()));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void removed() {
        super.removed();
        ClientCampfireState.clear();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private String trim(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        return font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, maxWidth);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
