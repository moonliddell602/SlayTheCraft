package com.slaythecraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;

/**
 * 战斗界面里的两个小图标：**生命前的红心**、**格挡前的盾牌**（v0.5.1.2 美术需求）。
 *
 * <p>需求原文："小图标：……战斗内生命前红心、格挡前盾牌。"</p>
 *
 * <p>红心是**程序化点阵**画的（原版没有"心"这个物品，也拿不到稳定的 HUD 贴图路径，
 * 一旦贴图改名就会变成紫黑格子）；盾牌直接用原版物品 {@code minecraft:shield} 的模型，
 * 和物品栏里的样子一致。</p>
 *
 * <p>纯像素填充，没有贴图依赖，所以任何资源包下都不会画错。</p>
 */
public final class HudIcons {

    /**
     * 红心的点阵（{@code #} = 上色）。7×6，正好和 8 像素高的字对齐。
     */
    private static final String[] HEART = {
            " ## ## ",
            "#######",
            "#######",
            " ##### ",
            "  ###  ",
            "   #   ",
    };

    /** 红心宽（像素）。 */
    public static final int HEART_W = 7;
    /** 红心高（像素）。 */
    public static final int HEART_H = 6;
    /** 盾牌图标边长（比字略大一点，和红心视觉重量一致）。 */
    public static final int SHIELD = 9;
    /** 图标与后面文字的间距。 */
    public static final int GAP = 2;

    private HudIcons() {
    }

    /** 画一颗红心（左上角 {@code (x, y)}）。 */
    public static void heart(GuiGraphics graphics, int x, int y, int color) {
        if (graphics == null) {
            return;
        }
        for (int row = 0; row < HEART.length; row++) {
            String line = HEART[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    /** 画一面盾牌（左上角 {@code (x, y)}，边长 {@link #SHIELD}）。 */
    public static void shield(GuiGraphics graphics, int x, int y) {
        ItemIcons.draw(graphics, Items.SHIELD, x, y, SHIELD);
    }
}
