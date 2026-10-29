package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.gate.SpireGateMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 尖塔入口界面：滚轮式角色选择 + 打开收纳包 / 查看本层可挑战 / 新手教程 / 重开本局。
 *
 * <p>不做美术：角色卡是程序化绘制的面板，显示名称、最大高度、起始牌组张数，当前角色高亮。
 * 数据来自已同步的玩家档案，不需要额外的网络包。</p>
 *
 * <h2>v0.5.0.12 的三处修复</h2>
 *
 * <ul>
 *   <li><b>"开始对战按钮应该被移除（无效）"</b> —— 它在面板上按不动：这个按钮走
 *       {@code SpireGateMenu.BUTTON_START_BATTLE}，而菜单的 {@code stillValid} 要求
 *       "玩家还是打开菜单的那一个"，实战里点它常常什么都没发生。开战本来就有两条可用路径
 *       （军旗右键生物 / 尖塔入口附近有敌人时自动遭遇），所以**直接移除按钮**，
 *       但菜单侧的按钮分支保留（指令与旧客户端仍可触发）。</li>
 *   <li><b>"文字存在会超出UI范围"</b> —— 面板从 240×268 扩到 280×280；
 *       底部提示改成**按面板宽度自动折行 + 自底向上定位**（最后一行永远落在面板内）。</li>
 *   <li><b>新增「新手教程」入口</b> —— 打开 {@link TutorialScreen} 分页教程。</li>
 * </ul>
 */
public class SpireGateScreen extends AbstractContainerScreen<SpireGateMenu> {

    private static final int CARD_X = 12;
    private static final int CARD_W = 216;

    // ------------------------------------------------------------------ 面板尺寸（v0.5.0.12 扩大）

    /**
     * 面板宽度（v0.5.0.12：240 → 280）。
     *
     * <p>反馈："尖塔入口主页的文字存在会超出UI范围的问题。解决方法：扩大背景。"
     * 240 宽放不下底部那几行中文提示（"点「查看本层可挑战」按阵营看这一层能打谁…"），
     * 也放不下滚轮的三张角色卡（3×72 + 2×4 = 224 > 240 − 2×12）。</p>
     */
    private static final int GATE_IMAGE_W = 280;

    /**
     * 面板高度（v0.5.0.12：268 → 280）。
     *
     * <p>增加的高度给底部提示块：提示按宽度折行后可能有 2–3 行，整体自底向上排，
     * 保证最后一行一定在面板内（见 {@link #hintBlockTop}）。</p>
     */
    private static final int GATE_IMAGE_H = 280;

    // ------------------------------------------------------------------ 滚轮式角色选择

    private static final int CAROUSEL_Y = 20;
    private static final int CARD_ITEM_W = 72;
    private static final int CARD_ITEM_H = 58;
    private static final int CAROUSEL_GAP = 4;
    private static final int CAROUSEL_CENTER_X = GATE_IMAGE_W / 2;
    /** 当前选中项两侧各最多显示几张（1 → 同屏 3 张）。 */
    private static final int VISIBLE_RANGE = 1;
    private static final int ARROW_W = 14;

    // ------------------------------------------------------------------ 按钮与提示的纵向布局

    /** 第一行按钮的 y（打开收纳包，整行宽）。 */
    private static final int BUTTON_Y = 110;
    /** 第二行按钮的 y（查看本层可挑战 + 重开并重选角色）。 */
    private static final int BUTTON2_Y = 128;
    /** 第三行按钮的 y（新手教程）。 */
    private static final int BUTTON3_Y = 146;

    /** 提示文字的行距。 */
    private static final int HINT_STEP = 11;
    /** 提示块最下面那一行距离面板底边的留白。 */
    private static final int HINT_BOTTOM_MARGIN = 6;
    /**
     * 提示块第一行**允许的最低 y**（夹紧下限）。
     *
     * <p>提示块是自底向上定位的：行数越多，第一行越靠上。这个下限保证它不会一路顶到按钮上，
     * 配合 {@link #HINT_BOTTOM_MARGIN} 就同时钉住了上下两头。</p>
     */
    private static final int HINT_TOP_LIMIT = 226;

    /** 底部最多画几行提示（超出不再画，避免顶穿按钮）。 */
    public static final int HINT_MAX_LINES = 3;

    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int HIGHLIGHT = 0xFFFFD24A;

    public SpireGateScreen(SpireGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GATE_IMAGE_W;
        this.imageHeight = GATE_IMAGE_H;
    }

    // ------------------------------------------------------------------ 供自检使用的纯几何

    /** 面板尺寸（自检用：验证提示与卡片都不会溢出）。 */
    public static int[] gatePanelSize() {
        return new int[] {GATE_IMAGE_W, GATE_IMAGE_H};
    }

    /** 一行提示文字的高度（自检用）。 */
    public static int hintLineHeight() {
        return 9;
    }

    /** 按钮行的 y（自检用）。 */
    public static int buttonRowY() {
        return BUTTON_Y;
    }

    /** 第二排按钮行的 y（自检用）。 */
    public static int buttonRow2Y() {
        return BUTTON2_Y;
    }

    /** 第三排按钮行的 y（自检用）。 */
    public static int buttonRow3Y() {
        return BUTTON3_Y;
    }

    /**
     * 底部提示块的**第一行** y（相对面板顶角）—— 自底向上定位。
     *
     * <p>公式：最下面一行固定在 {@code H − 底边留白 − 行高}，往上每行减一个行距；
     * 最后再对 {@code [HINT_TOP_LIMIT, ...]} 做下限夹紧，所以行数再多也不会顶到按钮上。</p>
     *
     * @param lines 实际要画几行（≤ {@link #HINT_MAX_LINES}）
     */
    public static int hintBlockTop(int lines) {
        int n = Math.max(1, Math.min(HINT_MAX_LINES, lines));
        int bottomLine = GATE_IMAGE_H - HINT_BOTTOM_MARGIN - hintLineHeight();
        return Math.max(HINT_TOP_LIMIT, bottomLine - (n - 1) * HINT_STEP);
    }

    /** 第 {@code line} 行提示的 y（相对面板左边顶角）—— 与 {@link #hintBlockTop} 一致。 */
    public static int hintLineY(int lines, int line) {
        return hintBlockTop(lines) + Math.max(0, line) * HINT_STEP;
    }

    /**
     * 提示块的**第一行 y 到最后一行的底边**都在面板内（自检用）。
     *
     * <p>这条断言就是"文字不再越框"的机器化版本。</p>
     */
    public static boolean hintBlockInside(int lines) {
        int n = Math.max(1, Math.min(HINT_MAX_LINES, lines));
        int top = hintBlockTop(n);
        int bottom = top + (n - 1) * HINT_STEP + hintLineHeight();
        return top > BUTTON3_Y + 14 && bottom <= GATE_IMAGE_H;
    }

    // ------------------------------------------------------------------ 滚轮几何（v0.5.0.9）

    /** 轮播当前居中到第几个角色。 */
    private int carouselIndex;

    /** 轮播是否已经初始化（第一次渲染时把当前角色摆到中间）。 */
    private boolean carouselReady;

    /**
     * 把轮播索引夹到合法范围，并且把 {@code index} 折到可见范围内（超出的返回 false，不画）。
     *
     * @param offset 相对当前选中项的偏移（0 = 中间，±1 = 两侧）
     */
    public static boolean carouselVisible(int offset) {
        return Math.abs(offset) <= VISIBLE_RANGE;
    }

    /** 某个偏移对应的卡片 x（相对面板左边）—— 自检用它验证"不会挤出面板"。 */
    public static int carouselCardX(int offset) {
        return CAROUSEL_CENTER_X - CARD_ITEM_W / 2
                + offset * (CARD_ITEM_W + CAROUSEL_GAP);
    }

    /** 可见卡片的宽度（自检用；两侧的会窄一点）。 */
    public static int carouselCardWidth() {
        return CARD_ITEM_W;
    }

    /** 滚轮切换：把索引循环前后移动。 */
    public static int carouselNext(int index, int count, int step) {
        if (count <= 0) {
            return 0;
        }
        return ((index + step) % count + count) % count;
    }

    private void clampCarousel(int count) {
        if (count <= 0) {
            carouselIndex = 0;
            return;
        }
        carouselIndex = ((carouselIndex % count) + count) % count;
    }

    private PlayerCardData data() {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : StcAttachments.dataOf(player);
    }

    private void drawCharacterCarousel(GuiGraphics graphics, int mouseX, int mouseY, String activeId) {
        List<CharacterProfile> profiles = CharacterRegistry.all();
        int count = profiles.size();
        clampCarousel(count);
        if (!carouselReady) {
            // 第一次打开：把当前角色摆到中间（没选角色就从第一个开始）
            int active = activeId == null ? -1 : CharacterRegistry.indexOf(ResourceLocation.tryParse(activeId));
            carouselIndex = active >= 0 ? active : 0;
            carouselReady = true;
            clampCarousel(count);
        }
        if (count == 0) {
            TextStyle.draw(graphics, font, Component.translatable("stc.gate.character.none").getString(),
                    leftPos + CARD_X, topPos + CAROUSEL_Y, HIGHLIGHT);
            return;
        }

        // 左右箭头
        boolean canFlip = count > 1;
        drawArrow(graphics, leftPos + CARD_X + 2, topPos + CAROUSEL_Y + CARD_ITEM_H / 2 - 7, "<", canFlip
                && isInside(mouseX, mouseY, leftPos + CARD_X + 2, topPos + CAROUSEL_Y + CARD_ITEM_H / 2 - 7,
                        ARROW_W, 14));
        drawArrow(graphics, leftPos + imageWidth - CARD_X - 2 - ARROW_W,
                topPos + CAROUSEL_Y + CARD_ITEM_H / 2 - 7, ">",
                canFlip && isInside(mouseX, mouseY, leftPos + imageWidth - CARD_X - 2 - ARROW_W,
                        topPos + CAROUSEL_Y + CARD_ITEM_H / 2 - 7, ARROW_W, 14));

        // 卡片：离中间越远画得越小（越暗），形成轮播感
        for (int offset = VISIBLE_RANGE; offset >= -VISIBLE_RANGE; offset--) {
            if (!carouselVisible(offset)) {
                continue;
            }
            int profileIndex = carouselNext(carouselIndex, count, offset);
            CharacterProfile profile = profiles.get(profileIndex);
            int x = leftPos + carouselCardX(offset);
            int y = topPos + CAROUSEL_Y + Math.abs(offset) * 4;
            int w = CARD_ITEM_W - Math.abs(offset) * 8;
            int h = CARD_ITEM_H - Math.abs(offset) * 6;
            boolean center = offset == 0;
            boolean active = profile.id().toString().equals(activeId);
            boolean hovered = isInside(mouseX, mouseY, x, y, w, h);

            graphics.fill(x, y, x + w, y + h, active ? 0xFF3A3420 : (center ? 0xFF2C2C36 : 0xFF22222A));
            border(graphics, x, y, x + w, y + h,
                    active ? HIGHLIGHT : (center && hovered ? TEXT : PANEL_EDGE));

            // 角色卡里的作者头像（BinderSkin 约定路径 avatar_<角色>.png），没有才留空
            int avatar = Math.min(w - 8, h - 20);
            int avatarX = x + (w - avatar) / 2;
            int avatarY = y + 3;
            if (!BinderSkin.drawAvatar(graphics, profile.id(), avatarX, avatarY, avatar)) {
                graphics.fill(avatarX, avatarY, avatarX + avatar, avatarY + avatar, 0xFF3A3A44);
            }
            Component name = Component.translatable(profile.nameKey());
            String shown = font.plainSubstrByWidth(name.getString(), w - 4);
            TextStyle.draw(graphics, font, shown, x + (w - font.width(shown)) / 2, y + h - 11,
                    active ? HIGHLIGHT : (center ? TEXT : TEXT_DIM));
            if (active) {
                graphics.fill(x + w / 2 - 8, y + h - 1, x + w / 2 + 8, y + h, HIGHLIGHT);
            }
        }

        // 索引与提示：当前是第几个 / 共几个 + 怎么翻页
        Component pos = Component.translatable("stc.gate.character.position", carouselIndex + 1, count);
        TextStyle.draw(graphics, font, pos, leftPos + (imageWidth - font.width(pos)) / 2,
                topPos + CAROUSEL_Y + CARD_ITEM_H + 10, TEXT_DIM);
        Component scroll = Component.translatable("stc.gate.character.scroll");
        TextStyle.draw(graphics, font, scroll, leftPos + (imageWidth - font.width(scroll)) / 2,
                topPos + CAROUSEL_Y + CARD_ITEM_H + 20, TEXT_DIM);
    }

    private void drawArrow(GuiGraphics graphics, int x, int y, String label, boolean hovered) {
        graphics.fill(x, y, x + ARROW_W, y + 14, hovered ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + ARROW_W, y + 14, hovered ? HIGHLIGHT : PANEL_EDGE);
        TextStyle.draw(graphics, font, label, x + (ARROW_W - font.width(label)) / 2, y + 3,
                hovered ? HIGHLIGHT : TEXT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        TextStyle.draw(graphics, font, title, leftPos + 10, topPos + 8, TEXT);

        PlayerCardData data = data();
        String activeId = data == null || data.character() == null ? null : data.character().toString();

        drawCharacterCarousel(graphics, mouseX, mouseY, activeId);

        // 第一行：打开收纳包（v0.5.0.12 起「开始对战」按钮已移除，这一行只剩它、占满宽度）
        button(graphics, leftPos + CARD_X, topPos + BUTTON_Y, GATE_IMAGE_W - CARD_X * 2, 14,
                Component.translatable("stc.gate.binder").getString(), true);

        // 第二行：查看本层可挑战名单 + 重开并重选角色
        int floor = data == null ? 1 : data.floor();
        int rosterCount = com.slaythecraft.combat.core.FloorRoster.count(floor);
        Component roster = Component.translatable("stc.gate.roster", floor, rosterCount);
        button(graphics, leftPos + CARD_X, topPos + BUTTON2_Y, GATE_IMAGE_W - CARD_X * 2 - 80, 14,
                roster.getString(), true);
        button(graphics, leftPos + GATE_IMAGE_W - CARD_X - 74, topPos + BUTTON2_Y, 74, 14,
                Component.translatable("stc.gate.restart").getString(), true);

        // 第三行：新手教程（v0.5.0.12 新功能）
        button(graphics, leftPos + CARD_X, topPos + BUTTON3_Y, 120, 14,
                Component.translatable("stc.gate.tutorial").getString(), true);

        drawHints(graphics, activeId);
    }

    /**
     * 底部提示：先按面板宽度逐行折行，再**自底向上**定位画出来。
     *
     * <p>这就是"文字超出 UI"的结构性修复 —— 不再假设文案多宽，而是先量再画；
     * 放不下的行直接不画（而不是画到面板外面去）。</p>
     */
    private void drawHints(GuiGraphics graphics, String activeId) {
        boolean noCharacter = activeId == null;
        int width = imageWidth - CARD_X * 2;
        List<String> lines = new java.util.ArrayList<>();
        List<String> keys = java.util.Arrays.asList(hintKeys(noCharacter));
        for (String key : keys) {
            lines.addAll(BinderStyle.wrap(font, Component.translatable(key).getString(), width));
        }
        int shown = Math.min(HINT_MAX_LINES, lines.size());
        int top = hintBlockTop(shown);
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            // 最后一行如果是"尚未选择角色"（行动号召），用金色标出来
            boolean callToAction = noCharacter && lines.get(i).equals(
                    Component.translatable("stc.gate.character.none").getString());
            TextStyle.draw(graphics, font, line, leftPos + (width - font.width(line)) / 2,
                    topPos + top + i * HINT_STEP, callToAction ? HIGHLIGHT : TEXT_DIM);
        }
    }

    /** 底部提示的文案键，按顺序。 */
    private static String[] hintKeys(boolean noCharacter) {
        return new String[] {
            "stc.gate.party.hint",
            "stc.gate.hint",
            "stc.gate.boss.hint",
            noCharacter ? "stc.gate.character.none" : "stc.gate.tutorial.hint"
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;

        // 滚轮式角色选择 —— 左右箭头翻页；点卡片开详情页
        List<CharacterProfile> profiles = CharacterRegistry.all();
        int count = profiles.size();
        if (count > 0) {
            int arrowY = CAROUSEL_Y + CARD_ITEM_H / 2 - 7;
            if (count > 1 && relX >= CARD_X + 2 && relX < CARD_X + 2 + ARROW_W
                    && relY >= arrowY && relY < arrowY + 14) {
                carouselIndex = carouselNext(carouselIndex, count, -1);
                return true;
            }
            int rightArrowX = imageWidth - CARD_X - 2 - ARROW_W;
            if (count > 1 && relX >= rightArrowX && relX < rightArrowX + ARROW_W
                    && relY >= arrowY && relY < arrowY + 14) {
                carouselIndex = carouselNext(carouselIndex, count, 1);
                return true;
            }
            for (int offset = VISIBLE_RANGE; offset >= -VISIBLE_RANGE; offset--) {
                if (!carouselVisible(offset)) {
                    continue;
                }
                int x = carouselCardX(offset);
                int y = CAROUSEL_Y + Math.abs(offset) * 4;
                int w = CARD_ITEM_W - Math.abs(offset) * 8;
                int h = CARD_ITEM_H - Math.abs(offset) * 6;
                if (relX < x || relX >= x + w || relY < y || relY >= y + h) {
                    continue;
                }
                int profileIndex = carouselNext(carouselIndex, count, offset);
                if (offset != 0) {
                    // 点两侧的卡片 = 把它转到中间（先居中，再点一次才进详情页）
                    carouselIndex = profileIndex;
                    return true;
                }
                // 中间那张：点进详情页（描述 / 初始牌组 / 初始遗物），在那里确认选择
                if (minecraft != null && minecraft.player != null) {
                    minecraft.setScreen(new CharacterDetailScreen(menu, minecraft.player.getInventory(), title,
                            profileIndex, () -> this));
                }
                return true;
            }
        }

        // 第一行：打开收纳包（整行宽）
        if (relY >= BUTTON_Y && relY < BUTTON_Y + 14 && relX >= CARD_X && relX < imageWidth - CARD_X) {
            click(SpireGateMenu.BUTTON_OPEN_BINDER);
            return true;
        }
        // 第二行：可挑战名单 + 重开
        if (relY >= BUTTON2_Y && relY < BUTTON2_Y + 14) {
            if (relX >= CARD_X && relX < imageWidth - CARD_X - 80) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.setScreen(new FloorRosterScreen(menu, minecraft.player.getInventory(), title));
                }
                return true;
            }
            if (relX >= imageWidth - CARD_X - 74 && relX < imageWidth - CARD_X) {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.setScreen(new ConfirmPageScreen<>(menu, minecraft.player.getInventory(), title,
                            Component.translatable("stc.gate.restart.title"),
                            List.of(
                                    Component.translatable("stc.gate.restart.line1").getString(),
                                    Component.translatable("stc.gate.restart.line2").getString(),
                                    Component.translatable("stc.gate.restart.line3").getString()),
                            Component.translatable("stc.gate.restart.do").getString(), true,
                            () -> click(SpireGateMenu.BUTTON_RESTART_RUN),
                            () -> this));
                }
                return true;
            }
        }
        // 第三行：新手教程
        if (relY >= BUTTON3_Y && relY < BUTTON3_Y + 14 && relX >= CARD_X && relX < CARD_X + 120) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.setScreen(new TutorialScreen(menu, minecraft.player.getInventory(), title));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** 滚轮切换角色（v0.5.0.9：需求"角色选择后续需要改成滚轮式显示"）。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int count = CharacterRegistry.count();
        if (count <= 1 || scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        carouselIndex = carouselNext(carouselIndex, count, scrollY < 0 ? 1 : -1);
        return true;
    }

    private void click(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label, boolean enabled) {
        graphics.fill(x, y, x + width, y + height, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + width, y + height, enabled ? PANEL_EDGE : 0xFF33333C);
        // v0.5.0.12：按钮文字也走折行兜底，长文案不会再爬出按钮
        BinderStyle.text(graphics, font, label, x + 3, y + (height - 8) / 2, width - 6, 1,
                enabled ? TEXT : TEXT_DIM);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    /** 点是否落在矩形里（本类局部 helper）。 */
    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
