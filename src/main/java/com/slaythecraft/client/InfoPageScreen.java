package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.combat.core.TextLayout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;

/**
 * 通用「信息页」（v0.2.7）：**任何查看详情都新开这一页**，而不是在母页面里塞文字。
 *
 * <p>反馈确立的约定："默认任何有子页面的内容，其母页面的查看详情等引导都默认新建页面 UI 显示子页面，
 * 以防出现文本堆叠等问题。" 本页把所有正文**折行**显示（不再截断），可带一个可选的动作按钮
 * （例如卡牌详情页里的"删除这张卡"），底部是「返回」。</p>
 *
 * <p>v0.5.1.2：正文支持**部分文字换色**（{@link BodyLine}）。卡牌图鉴的"升级后改动高亮"就走这里：
 * 改动的那几个词画成黄色，其余照旧。折行与换色都对得上，是因为折行返回的是
 * **原文下标区间**（{@link TextLayout#wrapRanges}），而不是折行后的新字符串。</p>
 *
 * @param <M> 复用的菜单类型（保持同一个容器打开，避免服务端容器状态错乱）
 */
public class InfoPageScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    private static final int BODY_Y = 52;
    private static final int LINE_H = 11;
    private static final int ACTION_H = 16;
    /** 带图标的正文段：图标边长与图标-文字间距（v0.5.1.2）。 */
    private static final int ICON = 16;
    private static final int ICON_GAP = 3;

    /** 升级改动的高亮色（BinderStyle.GOLD 的同一个值，这里不想为了一个常量多依赖一层）。 */
    public static final int HIGHLIGHT = 0xFFFFD24A;

    /**
     * 正文的一段。
     *
     * @param text      文字（按面板宽度折行）
     * @param color     普通字色
     * @param marks     逐字符"要不要高亮"的标记；{@code null} = 整段都普通色
     * @param markColor 高亮字色（{@code marks} 不为 {@code null} 时有意义）
     * @param icon      这一段左侧的小图标（{@code null} = 不画图标）；整段（含折行后的续行）
     *                  会一起右移，图标只画在第一行
     */
    public record BodyLine(String text, int color, boolean[] marks, int markColor, Item icon) {
        public BodyLine(String text, int color) {
            this(text, color, null, color, null);
        }

        public BodyLine(String text) {
            this(text, BinderStyle.INK, null, BinderStyle.INK, null);
        }

        /** 带高亮区间的正文段（marks 长度应与 text 相同）。 */
        public BodyLine(String text, boolean[] marks) {
            this(text, BinderStyle.INK, marks, HIGHLIGHT, null);
        }

        /** 左侧带一个小图标的正文段（v0.5.1.2 美术需求）。 */
        public BodyLine(String text, int color, Item icon) {
            this(text, color, null, color, icon);
        }
    }

    /**
     * 底部动作按钮（v0.5.1.2：一个页面最多两个 —— 图鉴详情页在开发者模式下既要
     * 「查看升级」又要「加入牌组」）。
     *
     * @param label 按钮文字
     * @param run   点下去做什么
     */
    public record Action(String label, Runnable run) {
    }

    private final Component heading;
    private final List<BodyLine> body;
    private final List<Action> actions;
    private final Supplier<Screen> back;

    public InfoPageScreen(M menu, Inventory inventory, Component title, Component heading, List<String> body,
                          String actionLabel, Runnable action, Supplier<Screen> back) {
        this(menu, inventory, title, heading, toLines(body), actionsOf(actionLabel, action), back, true);
    }

    /**
     * 造一个正文可以**部分换色**的信息页（v0.5.1.2）。
     *
     * <p>用具名工厂而不是重载构造器：{@code List<String>} 和 {@code List<BodyLine>} 擦除后
     * 是同一个签名，重载会直接编译不过。</p>
     */
    public static <M extends AbstractContainerMenu> InfoPageScreen<M> rich(
            M menu, Inventory inventory, Component title, Component heading, List<BodyLine> body,
            String actionLabel, Runnable action, Supplier<Screen> back) {
        return new InfoPageScreen<>(menu, inventory, title, heading, List.copyOf(body),
                actionsOf(actionLabel, action), back, true);
    }

    /**
     * 同上，但可以带**两个**动作按钮（上：第一个，下：第二个；离「返回」更近的是第二个）。
     *
     * <p>故意不叫重载的 {@code rich}：{@code rich(…, null, null, back)} 在两个重载之间是有歧义的
     * （编译器无法判断两个 null 是要 {@code (String, Runnable)} 还是 {@code (Action, Action)}），
     * 所以换个名字。</p>
     */
    public static <M extends AbstractContainerMenu> InfoPageScreen<M> richWithActions(
            M menu, Inventory inventory, Component title, Component heading, List<BodyLine> body,
            Action first, Action second, Supplier<Screen> back) {
        List<Action> actions = new ArrayList<>();
        if (first != null) {
            actions.add(first);
        }
        if (second != null) {
            actions.add(second);
        }
        return new InfoPageScreen<>(menu, inventory, title, heading, List.copyOf(body), List.copyOf(actions), back,
                true);
    }

    private InfoPageScreen(M menu, Inventory inventory, Component title, Component heading, List<BodyLine> body,
                           List<Action> actions, Supplier<Screen> back, boolean richMarker) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.heading = heading;
        this.body = List.copyOf(body);
        this.actions = List.copyOf(actions);
        this.back = back;
    }

    private static List<Action> actionsOf(String label, Runnable run) {
        return label == null || run == null ? List.of() : List.of(new Action(label, run));
    }

    private static List<BodyLine> toLines(List<String> body) {
        List<BodyLine> out = new ArrayList<>();
        if (body != null) {
            for (String line : body) {
                out.add(new BodyLine(line == null ? "" : line));
            }
        }
        return out;
    }

    @Override
    public void init() {
        // v0.4.3：面板尺寸按屏幕自适应（默认更大，小窗口自动收缩）
        this.imageWidth = BinderStyle.widthFor(this);
        this.imageHeight = BinderStyle.heightFor(this);
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        BinderStyle.book(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + BinderStyle.GRID_X;
        int width = imageWidth - BinderStyle.GRID_X * 2;

        TextStyle.draw(graphics, font, heading, x + (width - font.width(heading)) / 2, topPos + 12, BinderStyle.ACCENT);
        graphics.fill(x, topPos + 26, x + width, topPos + 27, BinderStyle.BOOK_LINE);

        int lineEnd = topPos + imageHeight - 40 - actions.size() * ACTION_H;
        int y = topPos + BODY_Y;
        boolean overflow = false;
        for (BodyLine line : body) {
            // 带图标的一段整体右移，图标只画在第一行（v0.5.1.2）
            int indent = line.icon() == null ? 0 : ICON + ICON_GAP;
            boolean first = true;
            // 折行返回的是原文下标，所以换色不会跟着折行错位（v0.5.1.2）
            for (TextLayout.Range range : TextLayout.wrapRanges(line.text(), width - indent,
                    s -> font.width(s))) {
                if (y + LINE_H > lineEnd) {
                    overflow = true;
                    break;
                }
                if (first && line.icon() != null) {
                    // 文字基线在 y..y+8，图标 16 高，所以上移 4 才是居中对齐
                    ItemIcons.draw(graphics, line.icon(), x, y - 4, ICON);
                }
                drawLine(graphics, line, range, x + indent, y);
                first = false;
                y += LINE_H;
            }
            if (overflow) {
                break;
            }
        }
        if (overflow) {
            TextStyle.draw(graphics, font, "…", x, y, BinderStyle.INK_DIM);
        }

        int footerY = topPos + imageHeight - 30;
        BinderStyle.button(graphics, font, x, footerY, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        // 动作按钮从下往上堆（第二个动作离「返回」最近），与命中判定共用 actionY()
        for (int i = 0; i < actions.size(); i++) {
            BinderStyle.button(graphics, font, x, topPos + actionY(i), width, ACTION_H,
                    actions.get(i).label(), true);
        }
    }

    /** 第 {@code index} 个动作按钮相对面板顶的 y（自下往上排）。 */
    private int actionY(int index) {
        int footerY = imageHeight - 30;
        return footerY - 4 - ACTION_H * (actions.size() - index);
    }

    /** 画一行：按"高亮/不高亮"切成连续的几段，每段一个颜色。 */
    private void drawLine(GuiGraphics graphics, BodyLine line, TextLayout.Range range, int x, int y) {
        if (range.start() >= range.end()) {
            return;
        }
        List<TextLayout.Run> runs = TextLayout.markRuns(line.marks(), range.start(), range.end());
        if (runs.isEmpty()) {
            TextStyle.draw(graphics, font, range.substring(line.text()), x, y, line.color());
            return;
        }
        int cursor = x;
        for (TextLayout.Run run : runs) {
            String text = line.text().substring(run.start(), run.end());
            TextStyle.draw(graphics, font, text, cursor, y, run.highlighted() ? line.markColor() : line.color());
            cursor += font.width(text);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int footerY = imageHeight - 30;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= footerY && relY < footerY + 14) {
            UiFeedback.click();
            minecraft.setScreen(back.get());
            return true;
        }
        if (!actions.isEmpty()) {
            for (int i = 0; i < actions.size(); i++) {
                int actionY = actionY(i);
                if (relX >= BinderStyle.GRID_X && relX < imageWidth - BinderStyle.GRID_X
                        && relY >= actionY && relY < actionY + ACTION_H) {
                    UiFeedback.click();
                    actions.get(i).run().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
