package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * **一张完整卡面的画法**（v0.5.5.1）——战斗内手牌、卡牌图鉴、「拥有卡牌」列表共用这一份。
 *
 * <p>需求原文（v0.5.5.1）："拥有卡牌和卡牌图鉴内的卡牌显示方式将和手牌显示的方式一致（包括美工等）。"</p>
 *
 * <p>所以这里把原先只属于 {@code BattleScreen.drawCardBody} 的那套版式抽成**唯一的一份绘制**：</p>
 * <ul>
 *   <li>几何（各块的 y 偏移、卡图矩形、描述框内宽/内高、边框粗细）继续由
 *       {@link BattleScreen} 的静态纯函数提供 —— 绘制、折行、自检共用同一份定义，
 *       不会出现"图鉴里画的和手牌不一样"；</li>
 *   <li>内容由 {@link View} 提供（费用 / 关键字 / 附魔 / 描述 / 强化档数 / 能不能打出），
 *       所以手牌可以传"服务端按当前状态算好的描述"，图鉴传卡库里的原始描述；</li>
 *   <li>卡图统一走 {@link CardArt}（约定路径 {@code stc:textures/card/<id>.png}，缺图是占位框）。</li>
 * </ul>
 *
 * <p>纯客户端绘制，没有任何副作用。</p>
 */
public final class CardFace {

    private CardFace() {
    }

    /**
     * 这一张卡**要画的内容**。
     *
     * @param cost         显示的费用（-1 = 用定义里的费用）
     * @param keywords     关键字文本（空串 = 不画）
     * @param enchantments 附魔文本（空串 = 不画）
     * @param description  描述文本（空串 = 用定义里的描述）
     * @param upgrades     强化档数（&gt; 0 时卡名右侧画「+」）
     * @param playable     这一刻能不能打出（false = 灰掉并显示"无法打出"）
     * @param affordable   费用够不够（false = 费用框画成红色）
     * @param selected     被选中（金色边框，比 hovered 优先）
     * @param hovered      鼠标悬停（边框提亮）
     */
    public record View(int cost, String keywords, String enchantments, String description, int upgrades,
                       boolean playable, boolean affordable, boolean selected, boolean hovered) {

        /** 默认的"能打出、买得起"的展示（图鉴 / 拥有卡牌列表用）。 */
        public static View of(int cost, String keywords, String enchantments, String description,
                              int upgrades) {
            return new View(cost, keywords, enchantments, description, upgrades, true, true, false, false);
        }
    }

    /** 一步画完：稀有度边框 + 卡图 + 费用框 + 卡名 + 类型带 + 描述框 + 关键字/附魔/强化标记。 */
    public static void draw(GuiGraphics graphics, Font font, Card definition, ResourceLocation id, View view,
                            int x, int y) {
        if (graphics == null || font == null || definition == null || id == null) {
            return;
        }
        View content = view == null ? View.of(definition.cost(), "", "", definition.description(), 0) : view;
        int cardW = BattleScreen.cardMetrics()[0];
        int cardH = BattleScreen.cardMetrics()[1];
        int[] art = BattleScreen.cardArtRect();
        int[] blocks = BattleScreen.cardBlockHeights();
        int border = BattleScreen.cardBorderThickness();

        boolean playable = content.playable();
        int accent = definition.rarity().color();

        // ---- 牌体 + 稀有度边框（v0.5.0.15：加粗）
        graphics.fill(x, y, x + cardW, y + cardH, 0xFF1C1C22);
        thickBorder(graphics, x, y, x + cardW, y + cardH,
                content.selected() ? 0xFFFFD24A
                        : (content.hovered() ? (playable ? 0xFFE8E8E8 : 0xFF9A9AA4) : accent),
                border);

        int[] off = BattleScreen.cardLayoutOffsets();
        int nameTop = off[8];
        int nameY = off[2];
        int typeBandTop = off[5];
        int textTop = off[3];

        // ---- 卡图
        CardArt.draw(graphics, id, x + art[0], y + art[1], art[2], art[3]);

        // ---- 费用框（X 费牌显示「X」）
        int cost = content.cost() >= 0 ? content.cost() : definition.cost();
        String costText = CardLibrary.isXCost(definition.id())
                ? Component.translatable("stc.card.cost.x").getString()
                : String.valueOf(cost);
        int costColor = !playable ? 0xFF9A9AA4 : (content.affordable() ? 0xFFFFD24A : 0xFFD05A5A);
        graphics.fill(x + 4, y + 4, x + 20, y + 18, 0xFF202028);
        border(graphics, x + 4, y + 4, x + 20, y + 18, costColor);
        TextStyle.draw(graphics, font, costText, x + 12 - font.width(costText) / 2, y + 7, costColor);

        // ---- 卡名（最上面那一条带；左右给费用框与「+」留位置）
        int nameLeft = x + 4 + 16 + 2;
        int nameRight = x + cardW - 12;
        String shownName = definition.name();
        if (font.width(shownName) > nameRight - nameLeft) {
            shownName = font.plainSubstrByWidth(shownName, nameRight - nameLeft);
        }
        TextStyle.draw(graphics, font, shownName,
                nameLeft + (nameRight - nameLeft - font.width(shownName)) / 2, y + nameY,
                !playable ? 0xFF9A9AA4 : (content.affordable() ? 0xFFE8E8E8 : 0xFF9A9AA4));

        // ---- 类型带（攻击 / 技能 / 能力）
        String type = definition.type().label();
        graphics.fill(x + 4, y + typeBandTop, x + cardW - 4, y + typeBandTop + blocks[1], 0x40203030);
        TextStyle.draw(graphics, font, type, x + (cardW - font.width(type)) / 2, y + typeBandTop + 1,
                0xFF9A9AA4);

        // ---- 描述框（先量再缩，保证整段塞进框里）
        int inset = (cardW - BattleScreen.textBoxInnerW()) / 2;
        String description = content.description().isEmpty() ? definition.description() : content.description();
        List<String> lines = BinderStyle.wrap(font, description, BattleScreen.textBoxInnerW());
        graphics.fill(x + inset, y + textTop, x + cardW - inset, y + textTop + blocks[2], 0xE8E8E0E8);
        border(graphics, x + inset, y + textTop, x + cardW - inset, y + textTop + blocks[2],
                playable ? accent : 0xFF9A9AA4);
        int lineH = 9;
        int maxLines = BattleScreen.textBoxLines();
        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        int clippedLines = Math.min(lines.size(), maxLines);
        float descScale = TextStyle.fitScale(widest, clippedLines, BattleScreen.textBoxInnerW(),
                BattleScreen.textBoxInnerH(), lineH);
        for (int line = 0; line < clippedLines; line++) {
            TextStyle.drawScaled(graphics, font, lines.get(line), x + inset + 3, y + textTop + 2 + line * lineH,
                    descScale, playable ? 0xFF241C10 : 0xFF9A9AA4);
        }
        if (!playable) {
            String mark = Component.translatable("stc.battle.unplayable").getString();
            TextStyle.draw(graphics, font, mark, x + (cardW - font.width(mark)) / 2,
                    y + art[1] + art[3] / 2 - 4, 0xFFD05A5A);
        }

        // ---- 关键字（底部留白条居中）
        if (content.keywords() != null && !content.keywords().isEmpty()) {
            String keywords = font.plainSubstrByWidth(content.keywords(), cardW - 12);
            TextStyle.draw(graphics, font, keywords, x + (cardW - font.width(keywords)) / 2,
                    y + cardH - BattleScreen.cardBottomPad() + 2, 0xFFFFD24A);
        }
        // ---- 附魔（卡名带右侧）
        if (content.enchantments() != null && !content.enchantments().isEmpty()) {
            String enchantments = font.plainSubstrByWidth(content.enchantments(), cardW - 40);
            TextStyle.draw(graphics, font, enchantments, x + cardW - 12 - font.width(enchantments),
                    y + nameTop + 3, 0xFFB48AD8);
        }
        // ---- 强化标记
        if (content.upgrades() > 0) {
            TextStyle.draw(graphics, font, "+", x + cardW - 10, y + nameTop + 3, 0xFFFFD24A);
        }
    }

    // ------------------------------------------------------------------ 网格版的几何（图鉴 / 拥有卡牌共用）

    /** 网格里一格的宽度（把可用宽度按列数平分）。 */
    public static int gridCellW(int gridWidth, int cols, int gap) {
        int columns = Math.max(1, cols);
        return Math.max(24, (gridWidth - (columns - 1) * gap) / columns);
    }

    /** 网格里一格的高度（按卡面 80:96 的比例算，保证不变形）。 */
    public static int gridCellH(int cellW) {
        int[] metrics = BattleScreen.cardMetrics();
        return Math.max(24, cellW * metrics[1] / metrics[0]);
    }

    /** 这个格宽下卡面的缩放系数（永远 ≤ 1：**只缩小、不放大**）。 */
    public static float scaleFor(int cellW) {
        int[] metrics = BattleScreen.cardMetrics();
        return Math.min(1.0F, cellW / (float) metrics[0]);
    }

    /** 给定可用高度能放几行（至少 1 行）。 */
    public static int visibleRows(int availableHeight, int cellH, int gap) {
        return Math.max(1, availableHeight / Math.max(1, cellH + gap));
    }

    /**
     * 画一张**缩放后**的卡面（缩放以牌的中心为基准，所以 {@code x, y} 仍是左上角）。
     *
     * <p>网格列表（卡牌图鉴 / 拥有卡牌）用它把整张卡面塞进一格。</p>
     */
    public static void drawScaled(GuiGraphics graphics, Font font, Card definition, ResourceLocation id, View view,
                                  int x, int y, float scale) {
        if (graphics == null || definition == null) {
            return;
        }
        if (Math.abs(scale - 1.0F) < 1.0E-4F) {
            draw(graphics, font, definition, id, view, x, y);
            return;
        }
        int[] metrics = BattleScreen.cardMetrics();
        float halfW = metrics[0] / 2.0F;
        float halfH = metrics[1] / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + halfW, y + halfH, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-halfW, -halfH, 0.0F);
        try {
            draw(graphics, font, definition, id, view, 0, 0);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** 1 像素描边（费用框 / 描述框用，与手牌同一套画法）。 */
    private static void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    /** 画一条**指定粗细**的边框（与战斗内手牌同一套画法）。 */
    public static void thickBorder(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color,
                                   int thickness) {
        int t = Math.max(1, thickness);
        for (int i = 0; i < t; i++) {
            graphics.fill(x1 + i, y1 + i, x2 - i, y1 + i + 1, color);
            graphics.fill(x1 + i, y2 - i - 1, x2 - i, y2 - i, color);
            graphics.fill(x1 + i, y1 + i, x1 + i + 1, y2 - i, color);
            graphics.fill(x2 - i - 1, y1 + i, x2 - i, y2 - i, color);
        }
    }
}
