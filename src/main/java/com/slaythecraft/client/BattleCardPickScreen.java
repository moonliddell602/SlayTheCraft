package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.network.CardPickChoicePayload;
import com.slaythecraft.network.CardPickOfferPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 战斗中的**选牌页**（v0.5.1.7 A3）。
 *
 * <p>需求："A3：应该让玩家自己选择。" 废料回收 / 应急储备 / 弃置边陵 / 回收 打出后会弹这一页，
 * 候选与服务端是同一份快照（下标一一对应）。</p>
 *
 * <p>交互：</p>
 * <ul>
 *   <li>点格子 = 选中 / 取消选中；「确认」把下标回传（弃置边陵这类"最多 N 张"允许少选甚至不选）；</li>
 *   <li>「放弃」或按 ESC = 回传 {@code auto}，让引擎按"贪心"替他选（保证效果不会凭空消失，
 *       也照顾"我就想快速跳过"的玩家）；</li>
 *   <li>不需要瞄准目标的那类选牌，界面照旧暂停游戏（{@code isPauseScreen} 为 false，
 *       因为战斗界面本来就不暂停）。</li>
 * </ul>
 */
public class BattleCardPickScreen extends Screen {

    private static final int COLS = 5;
    private static final int CELL_W = 56;
    private static final int CELL_H = 26;
    private static final int GAP = 4;
    private static final int PAGE_SIZE = COLS * 3;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int HIGHLIGHT = 0xFFFFD24A;
    private static final int SELECTED = 0xFF3A3420;

    private final CardPickOfferPayload offer;
    private final List<Integer> selected = new ArrayList<>();
    private int page;

    public BattleCardPickScreen(CardPickOfferPayload offer) {
        super(Component.translatable("stc.pick.title"));
        this.offer = offer;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** 面板几何（渲染与点击共用一份，避免两处各算一次）。 */
    private int[] panel() {
        int panelW = COLS * (CELL_W + GAP) + 16;
        int rows = Math.max(1, Math.min(PAGE_SIZE, offer.candidates().size()) / COLS
                + (Math.min(PAGE_SIZE, offer.candidates().size()) % COLS == 0 ? 0 : 1));
        int panelH = 74 + rows * (CELL_H + GAP) + 22;
        return new int[] {(width - panelW) / 2, (height - panelH) / 2, panelW, panelH};
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int[] p = panel();
        int left = p[0];
        int top = p[1];
        graphics.fill(left, top, left + p[2], top + p[3], PANEL_BG);
        border(graphics, left, top, left + p[2], top + p[3], PANEL_EDGE);

        Component title = Component.translatable("stc.pick.heading");
        TextStyle.draw(graphics, font, title, left + (p[2] - font.width(title)) / 2, top + 8, HIGHLIGHT);
        TextStyle.draw(graphics, font, offer.prompt(), left + (p[2] - font.width(offer.prompt())) / 2, top + 22,
                TEXT);
        Component hint = Component.translatable(offer.maxPicks() > 1 ? "stc.pick.hint.multi" : "stc.pick.hint",
                offer.maxPicks());
        TextStyle.draw(graphics, font, hint, left + (p[2] - font.width(hint)) / 2, top + 34, TEXT_DIM);

        if (offer.candidates().isEmpty()) {
            Component empty = Component.translatable("stc.pick.empty");
            TextStyle.draw(graphics, font, empty, left + (p[2] - font.width(empty)) / 2, top + 60, TEXT_DIM);
            return;
        }

        int start = page * PAGE_SIZE;
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = start + cell;
            if (index >= offer.candidates().size()) {
                break;
            }
            CardPickOfferPayload.Choice choice = offer.candidates().get(index);
            Card definition = CardLibrary.resolve(choice.id().getPath(), choice.upgraded() ? 1 : 0);
            int x = left + 8 + (cell % COLS) * (CELL_W + GAP);
            int y = top + 50 + (cell / COLS) * (CELL_H + GAP);
            boolean hovered = isInside(mouseX, mouseY, x, y, CELL_W, CELL_H);
            boolean picked = selected.contains(index);
            graphics.fill(x, y, x + CELL_W, y + CELL_H, picked ? SELECTED : 0xFF26262E);
            border(graphics, x, y, x + CELL_W, y + CELL_H,
                    picked ? HIGHLIGHT : (hovered ? 0xFF8A8A94 : PANEL_EDGE));
            String name = definition == null ? choice.id().getPath() : definition.name();
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(name, CELL_W - 8), x + 4, y + 4,
                    picked || hovered ? HIGHLIGHT : TEXT);
            String cost = definition == null ? "?" : Component.translatable("stc.reward.energy",
                    definition.cost()).getString();
            TextStyle.draw(graphics, font, font.plainSubstrByWidth(cost, CELL_W - 8), x + 4, y + 15, TEXT_DIM);
            if (picked) {
                String order = String.valueOf(selected.indexOf(index) + 1);
                TextStyle.draw(graphics, font, order, x + CELL_W - 10, y + 15, HIGHLIGHT);
            }
        }

        // 页码 + 按钮
        int pages = pages();
        if (pages > 1) {
            Component pageText = Component.translatable("stc.question.pick.page", page + 1, pages);
            TextStyle.draw(graphics, font, pageText, left + 8, top + p[3] - 34, TEXT_DIM);
        }
        int buttonY = top + p[3] - 20;
        drawButton(graphics, left + 8, buttonY, 56, Component.translatable("stc.pick.confirm").getString(),
                !selected.isEmpty(), mouseX, mouseY);
        drawButton(graphics, left + p[2] - 8 - 56, buttonY, 56,
                Component.translatable("stc.pick.skip").getString(), true, mouseX, mouseY);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, String label, boolean enabled,
                            int mouseX, int mouseY) {
        boolean hovered = enabled && isInside(mouseX, mouseY, x, y, w, 16);
        graphics.fill(x, y, x + w, y + 16, enabled ? (hovered ? 0xFF3A3A48 : 0xFF26262E) : 0xFF1A1A20);
        border(graphics, x, y, x + w, y + 16, hovered ? HIGHLIGHT : PANEL_EDGE);
        TextStyle.draw(graphics, font, label, x + (w - font.width(label)) / 2, y + 4,
                enabled ? (hovered ? HIGHLIGHT : TEXT) : TEXT_DIM);
    }

    private int pages() {
        return Math.max(1, (offer.candidates().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] p = panel();
        int left = p[0];
        int top = p[1];
        int buttonY = top + p[3] - 20;
        if (isInside(mouseX, mouseY, left + 8, buttonY, 56, 16) && !selected.isEmpty()) {
            send(false);
            return true;
        }
        if (isInside(mouseX, mouseY, left + p[2] - 8 - 56, buttonY, 56, 16)) {
            send(true);
            return true;
        }
        int start = page * PAGE_SIZE;
        for (int cell = 0; cell < PAGE_SIZE; cell++) {
            int index = start + cell;
            if (index >= offer.candidates().size()) {
                break;
            }
            int x = left + 8 + (cell % COLS) * (CELL_W + GAP);
            int y = top + 50 + (cell / COLS) * (CELL_H + GAP);
            if (isInside(mouseX, mouseY, x, y, CELL_W, CELL_H)) {
                if (selected.contains(index)) {
                    selected.remove((Integer) index);
                } else if (selected.size() < offer.maxPicks()) {
                    selected.add(index);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages() > 1) {
            page = Math.max(0, Math.min(pages() - 1, page + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        // ESC = 放弃：让引擎按贪心替他选（效果不会凭空消失）
        send(true);
    }

    private void send(boolean auto) {
        if (ClientCardPickState.offer() == null) {
            return;
        }
        UiFeedback.click();
        PacketDistributor.sendToServer(new CardPickChoicePayload(auto ? List.of() : List.copyOf(selected), auto));
        ClientCardPickState.clear();
        if (minecraft != null) {
            // 回到战斗界面：不用主动 setScreen，StcClientSetup 的 tick 会把出牌界面接回来
            minecraft.setScreen(null);
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
