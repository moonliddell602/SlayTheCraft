package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.RewardOffer;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.reward.RewardMenu;
import com.slaythecraft.reward.RewardService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 奖励界面 —— 按菜单的 {@link RewardMenu.Mode} 画**两个页面之一**（v0.5.0.20）：
 *
 * <ul>
 *   <li>{@link RewardMenu.Mode#CHOICE} 卡牌三选一（原有行为）；</li>
 *   <li>{@link RewardMenu.Mode#RESULT} **战斗结算页**：领取金币 / 领取遗物 / 获得一次卡牌奖励 / 跳过奖励。</li>
 * </ul>
 *
 * <p>需求原文："新UI:战斗结算页面：领取X金币（x为在战斗中获得的金币）/ 领取遗物：X（X为遗物名称，
 * 如果有则显示此选项，没有掉落则不显示）/ 获得一次卡牌奖励（打开卡牌领取页面）/ 跳过奖励按钮"，
 * 并特别说明"如果点开了卡牌领取页面，但战斗结算页面仍然有未领取的内容，则回到战斗结算页面"——
 * 那条由服务端换菜单实现（见 {@link RewardMenu} 的类注释）。</p>
 *
 * <p>v0.5.0.20 另外两条美术要求：两个页面都做**渐显**、每次点按钮都播
 * {@code block.note_block.hat}（音高 F♯4，见 {@link UiFeedback}）。</p>
 */
public class RewardScreen extends AbstractContainerScreen<RewardMenu> {

    private static final int CHOICE_W = 68;
    private static final int CHOICE_H = 100;
    private static final int CHOICE_Y = 30;
    private static final int CHOICE_X = 14;
    private static final int CHOICE_GAP = 72;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int DONE = 0xFF5AD05A;

    // ---- 战斗结算页的行**纯几何**（自检直接调，避免"绘制与点击判定各写一份"）----

    /** 结算页面板尺寸。 */
    private static final int RESULT_W = 260;
    private static final int RESULT_H = 186;
    /** 结算页第一行（金币）的 y。 */
    private static final int RESULT_ROW_Y = 42;
    /** 结算页每行的高度。 */
    private static final int RESULT_ROW_H = 24;
    /** 结算页行按钮的宽度。 */
    private static final int RESULT_BTN_W = 220;
    /** 结算页行按钮的 x（面板内居中）。 */
    private static final int RESULT_BTN_X = (RESULT_W - RESULT_BTN_W) / 2;

    /** 结算页面板尺寸（自检用）。 */
    public static int[] resultPanelSize() {
        return new int[] {RESULT_W, RESULT_H};
    }

    /** 结算页第 {@code row} 行的矩形 {x, y, w, h}（0 = 金币，1 = 遗物，2 = 卡牌奖励，3 = 跳过）。 */
    public static int[] resultRowRect(int row) {
        return new int[] {RESULT_BTN_X, RESULT_ROW_Y + Math.max(0, row) * RESULT_ROW_H, RESULT_BTN_W, 18};
    }

    /** 界面的打开时刻（渐显用）。 */
    private final long openedAt = System.currentTimeMillis();

    public RewardScreen(RewardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        if (menu.mode() == RewardMenu.Mode.RESULT) {
            this.imageWidth = RESULT_W;
            this.imageHeight = RESULT_H;
        } else {
            this.imageWidth = 240;
            this.imageHeight = 170;
        }
    }

    private PlayerCardData data() {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : StcAttachments.dataOf(player);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 需求："战斗结算，卡牌领取页面添加动画：渐显。"
        float alpha = UiFeedback.fadeAlpha(System.currentTimeMillis(), openedAt);
        FadeStyle.withAlpha(alpha, () -> {
            if (menu.mode() == RewardMenu.Mode.RESULT) {
                renderResult(graphics, mouseX, mouseY);
            } else {
                renderChoice(graphics, mouseX, mouseY);
            }
        });
    }

    // ------------------------------------------------------------------ 战斗结算页

    private void renderResult(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);

        PlayerCardData data = data();
        Component titleText = Component.translatable("stc.reward.result.title");
        TextStyle.draw(graphics, font, titleText, leftPos + 10, topPos + 8, GOLD);
        Component sub = Component.translatable("stc.reward.result.subtitle");
        TextStyle.draw(graphics, font, sub, leftPos + 10, topPos + 22, TEXT_DIM);

        if (data == null) {
            return;
        }

        // 第 1 行：领取金币
        int[] goldRow = resultRowRect(0);
        boolean goldDone = data.settleGoldClaimed() || data.pendingGold() <= 0;
        String goldLabel = Component.translatable("stc.reward.result.gold", data.pendingGold()).getString();
        resultRow(graphics, mouseX, mouseY, goldRow, goldLabel, !goldDone, goldDone);

        // 第 2 行：领取遗物（**没掉落就不显示这一项**）
        String relicId = data.pendingRelic();
        boolean hasRelic = relicId != null && !relicId.isBlank();
        int[] relicRow = resultRowRect(1);
        if (hasRelic) {
            String relicLabel = Component.translatable("stc.reward.result.relic",
                    RewardService.relicDisplayName(relicId)).getString();
            resultRow(graphics, mouseX, mouseY, relicRow, relicLabel, true, false);
        }

        // 第 3 行：获得一次卡牌奖励
        int[] cardRow = resultRowRect(2);
        boolean hasCard = data.pendingRewards() > 0;
        String cardLabel = Component.translatable("stc.reward.result.card", data.pendingRewards()).getString();
        resultRow(graphics, mouseX, mouseY, cardRow, cardLabel, hasCard, !hasCard);

        // 第 4 行：跳过奖励
        int[] skipRow = resultRowRect(3);
        resultRow(graphics, mouseX, mouseY, skipRow,
                Component.translatable("stc.reward.result.skip").getString(), true, false);
    }

    /** 结算页的一行：一行文字 + 一个按钮。 */
    private void resultRow(GuiGraphics graphics, int mouseX, int mouseY, int[] rect, String label,
                           boolean enabled, boolean done) {
        int x = leftPos + rect[0];
        int y = topPos + rect[1];
        int w = rect[2];
        int h = rect[3];
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, w, h);
        graphics.fill(x, y, x + w, y + h, done ? 0xFF23301F : (hovered ? 0xFF3A3A48 : 0xFF26262E));
        border(graphics, x, y, x + w, y + h, done ? DONE : (hovered ? GOLD : PANEL_EDGE));
        String shown = font.plainSubstrByWidth(label, w - 10);
        TextStyle.draw(graphics, font, shown, x + 5, y + (h - 8) / 2,
                done ? DONE : (enabled ? TEXT : TEXT_DIM));
    }

    // ------------------------------------------------------------------ 卡牌三选一页

    private void renderChoice(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);

        PlayerCardData data = data();
        RewardOffer offer = data == null ? null : data.firstOffer();

        Component titleText = offer != null && offer.golden()
                ? Component.translatable("stc.reward.title.golden")
                : Component.translatable("stc.reward.title");
        TextStyle.draw(graphics, font, titleText, leftPos + 10, topPos + 8,
                offer != null && offer.golden() ? GOLD : TEXT);

        if (data != null) {
            Component pending = Component.translatable("stc.reward.pending", data.pendingRewards());
            TextStyle.draw(graphics, font, pending, leftPos + imageWidth - 10 - font.width(pending), topPos + 8,
                    TEXT_DIM);
        }

        if (offer == null || offer.choices().isEmpty()) {
            Component empty = Component.translatable("stc.reward.empty");
            TextStyle.draw(graphics, font, empty, leftPos + (imageWidth - font.width(empty)) / 2, topPos + 70,
                    TEXT_DIM);
            return;
        }

        List<ResourceLocation> choices = offer.choices();
        for (int i = 0; i < choices.size(); i++) {
            int x = leftPos + CHOICE_X + i * CHOICE_GAP;
            int y = topPos + CHOICE_Y;
            ResourceLocation id = choices.get(i);
            Card definition = CardLibrary.byId(id.getPath());
            int accent = definition == null ? PANEL_EDGE : definition.rarity().color();

            graphics.fill(x, y, x + CHOICE_W, y + CHOICE_H, 0xFF26262E);
            border(graphics, x, y, x + CHOICE_W, y + CHOICE_H, accent);

            String name = definition == null ? id.getPath() : definition.name();
            if (font.width(name) > CHOICE_W - 8) {
                name = font.plainSubstrByWidth(name, CHOICE_W - 8);
            }
            TextStyle.draw(graphics, font, name, x + (CHOICE_W - font.width(name)) / 2, y + 8, TEXT);

            if (definition != null) {
                TextStyle.draw(graphics, font, definition.rarity().label(),
                        x + (CHOICE_W - font.width(definition.rarity().label())) / 2, y + 20, accent);
                String cost = Component.translatable("stc.reward.energy", definition.cost()).getString();
                TextStyle.draw(graphics, font, cost, x + (CHOICE_W - font.width(cost)) / 2, y + 32, GOLD);
                List<String> lines = wrap(definition.description(), CHOICE_W - 8);
                for (int line = 0; line < lines.size() && line < 4; line++) {
                    TextStyle.draw(graphics, font, lines.get(line), x + 4, y + 48 + line * 10, TEXT_DIM);
                }
            }

            Component pick = Component.translatable("stc.reward.pick");
            TextStyle.draw(graphics, font, pick, x + (CHOICE_W - font.width(pick)) / 2, y + CHOICE_H - 14, GOLD);
        }

        button(graphics, leftPos + CHOICE_X, topPos + 140, 74, 14,
                Component.translatable("stc.reward.skip").getString(), true);
        Component hint = Component.translatable("stc.reward.hint");
        TextStyle.draw(graphics, font, hint, leftPos + imageWidth - 10 - font.width(hint), topPos + 143, TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        PlayerCardData data = data();
        if (data == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;

        if (menu.mode() == RewardMenu.Mode.RESULT) {
            return clickedResult(relX, relY, data);
        }

        RewardOffer offer = data.firstOffer();
        if (offer != null) {
            for (int i = 0; i < offer.choices().size(); i++) {
                int x = CHOICE_X + i * CHOICE_GAP;
                if (relX >= x && relX < x + CHOICE_W && relY >= CHOICE_Y && relY < CHOICE_Y + CHOICE_H) {
                    click(i);
                    return true;
                }
            }
        }
        if (relX >= CHOICE_X && relX < CHOICE_X + 74 && relY >= 140 && relY < 154) {
            click(RewardMenu.BUTTON_SKIP);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean clickedResult(int relX, int relY, PlayerCardData data) {
        // 第 1 行：金币（已领则不可点）
        if (hitRow(relX, relY, 0) && data.pendingGold() > 0) {
            click(RewardMenu.BUTTON_CLAIM_GOLD);
            return true;
        }
        // 第 2 行：遗物（没掉落时这一行不存在）
        if (data.pendingRelic() != null && hitRow(relX, relY, 1)) {
            click(RewardMenu.BUTTON_CLAIM_RELIC);
            return true;
        }
        // 第 3 行：卡牌奖励
        if (hitRow(relX, relY, 2) && data.pendingRewards() > 0) {
            click(RewardMenu.BUTTON_TAKE_CARD);
            return true;
        }
        // 第 4 行：跳过
        if (hitRow(relX, relY, 3)) {
            click(RewardMenu.BUTTON_SKIP_ALL);
            return true;
        }
        return super.mouseClicked(relX + leftPos, relY + topPos, 0);
    }

    private boolean hitRow(int relX, int relY, int row) {
        int[] rect = resultRowRect(row);
        return relX >= rect[0] && relX < rect[0] + rect[2] && relY >= rect[1] && relY < rect[1] + rect[3];
    }

    private void click(int buttonId) {
        // 需求："为每次点击按钮都添加音效：block.note_block.hat，音高（第一八度）F♯/G♭"
        UiFeedback.click();
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    /** 按宽度切行（中文没有空格，只能按像素宽度硬切）。 */
    private List<String> wrap(String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty() && lines.size() < 6) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
            remaining = remaining.substring(line.length());
        }
        return lines;
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label, boolean enabled) {
        graphics.fill(x, y, x + width, y + height, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + width, y + height, enabled ? PANEL_EDGE : 0xFF33333C);
        TextStyle.draw(graphics, font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2,
                enabled ? TEXT : TEXT_DIM);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    /** 点是否在矩形内（局部 helper）。 */
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
