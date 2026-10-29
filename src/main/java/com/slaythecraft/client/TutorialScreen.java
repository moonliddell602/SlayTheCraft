package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.gate.SpireGateMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * **新手教程**（v0.5.0.12）。
 *
 * <p>需求原文："为玩家提供【新手教程】模块，可以点击后前往详情页查看。
 * 内容包含如：我该怎么玩这个模组？，战斗机制，尖塔旅行，如何使用卡牌收纳包等内容。"</p>
 *
 * <h2>为什么单独做一个页面，而不是复用 {@link InfoPageScreen}</h2>
 *
 * <p>{@code InfoPageScreen} 在正文超出时用「…」**截断**（它的定位是"看一段说明"）。
 * 教程天然比一屏长，截断就等于教不完，所以这里做一个**分页**版本：
 * 正文按书页宽度折行、按可用高度切页，底部「上一页 / 下一页 + 第 n / N 页」，
 * 每一页的正文都保证画在书页内。</p>
 *
 * <p>正文全部来自 {@code assets/stc/lang/*.json} 的 {@code stc.tutorial.<chapter>.body}，
 * 用 {@code \n} 分段，所以**改文案不需要碰 Java**（中英各一份）。</p>
 */
public class TutorialScreen extends AbstractContainerScreen<SpireGateMenu> {

    /** 正文第一行的 y（相对面板顶角）。 */
    private static final int BODY_TOP = 52;
    /** 行高（与 {@link BinderStyle#LINE_H} 一致）。 */
    private static final int LINE_H = BinderStyle.LINE_H;
    /** 正文最下面一行到页脚之间的留白。 */
    private static final int BODY_BOTTOM_GAP = 44;

    /** 章节的翻译键后缀（同时也是自检比对的依据）。 */
    private static final String[] CHAPTERS = {
        "intro",      // 我该怎么玩这个模组？
        "combat",     // 战斗机制
        "spire",      // 尖塔旅行
        "binder",     // 如何使用卡牌收纳包
        "codex",      // 图鉴与奖励
    };

    private final Supplier<Screen> back;

    /** 每页的正文行（在 {@link #init()} 里算，因为要用到真实字体宽度）。 */
    private List<List<String>> pages = List.of();
    private int page;

    public TutorialScreen(SpireGateMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, null);
    }

    public TutorialScreen(SpireGateMenu menu, Inventory inventory, Component title, Supplier<Screen> back) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.back = back;
    }

    /** 章节数（自检用）。 */
    public static int chapterCount() {
        return CHAPTERS.length;
    }

    /** 第 {@code index} 章的翻译键（自检用）。 */
    public static String chapterTitleKey(int index) {
        return "stc.tutorial." + CHAPTERS[index] + ".title";
    }

    /** 第 {@code index} 章的正文翻译键（自检用）。 */
    public static String chapterBodyKey(int index) {
        return "stc.tutorial." + CHAPTERS[index] + ".body";
    }

    /** 全部章节的标题键 / 正文键（自检遍历用）。 */
    public static String[] allChapterKeys() {
        String[] keys = new String[CHAPTERS.length * 2];
        for (int i = 0; i < CHAPTERS.length; i++) {
            keys[i * 2] = chapterTitleKey(i);
            keys[i * 2 + 1] = chapterBodyKey(i);
        }
        return keys;
    }

    /**
     * 把"每章的正文行"切成一页一页（自检直接调这个）。
     *
     * <p>切页规则：**尽量填满当前页**再翻页；一章比一页长就跨页（不丢字、不截断）。</p>
     *
     * @param chapterLines 每章已经折好行的正文（至少 1 行）
     * @param linesPerPage 一页能画几行
     */
    public static List<List<String>> paginate(List<List<String>> chapterLines, int linesPerPage) {
        List<List<String>> result = new ArrayList<>();
        if (chapterLines == null || chapterLines.isEmpty() || linesPerPage <= 0) {
            return result;
        }
        List<String> current = new ArrayList<>();
        for (List<String> chapter : chapterLines) {
            for (String line : chapter) {
                if (current.size() >= linesPerPage) {
                    result.add(List.copyOf(current));
                    current.clear();
                }
                current.add(line);
            }
            // 章与章之间空一行（视觉分隔），同样受页容量约束
            if (current.size() >= linesPerPage) {
                result.add(List.copyOf(current));
                current.clear();
            }
            current.add("");
        }
        if (!current.isEmpty()) {
            // 去掉最后多余的空行
            while (!current.isEmpty() && current.get(current.size() - 1).isEmpty()) {
                current.remove(current.size() - 1);
            }
            if (!current.isEmpty()) {
                result.add(List.copyOf(current));
            }
        }
        return result.isEmpty() ? List.of(List.of()) : result;
    }

    /** 一页能画几行正文。 */
    private int linesPerPage() {
        return Math.max(1, (imageHeight - BODY_TOP - BODY_BOTTOM_GAP) / LINE_H);
    }

    @Override
    public void init() {
        this.imageWidth = BinderStyle.widthFor(this);
        this.imageHeight = BinderStyle.heightFor(this);
        super.init();

        int width = imageWidth - BinderStyle.GRID_X * 2;
        List<List<String>> chapterLines = new ArrayList<>();
        for (int i = 0; i < CHAPTERS.length; i++) {
            List<String> lines = new ArrayList<>();
            // 章节标题作为正文第一行（缩进两格，和正文区分）
            lines.add("§ " + Component.translatable(chapterTitleKey(i)).getString());
            lines.addAll(BinderStyle.wrap(font, Component.translatable(chapterBodyKey(i)).getString(), width));
            chapterLines.add(lines);
        }
        this.pages = paginate(chapterLines, linesPerPage());
        this.page = Math.max(0, Math.min(pages.size() - 1, page));
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

        Component heading = Component.translatable("stc.tutorial.title");
        TextStyle.draw(graphics, font, heading, x + (width - font.width(heading)) / 2, topPos + 12,
                BinderStyle.ACCENT);
        graphics.fill(x, topPos + 26, x + width, topPos + 27, BinderStyle.BOOK_LINE);

        List<String> lines = pages.isEmpty() ? List.of() : pages.get(page);
        int y = topPos + BODY_TOP;
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                boolean isChapterTitle = line.startsWith("§ ");
                TextStyle.draw(graphics, font, isChapterTitle ? line.substring(2) : line, x, y,
                        isChapterTitle ? BinderStyle.ACCENT : BinderStyle.INK);
            }
            y += LINE_H;
        }

        // ---- 页脚：返回（左） + 上一页 / 下一页（右）
        int footer = topPos + BinderStyle.footerY(imageHeight);
        int total = Math.max(1, pages.size());
        BinderStyle.button(graphics, font, x, footer, BinderStyle.FOOTER_BACK_W, BinderStyle.FOOTER_BTN_H,
                Component.translatable("stc.binder.back").getString(), true);

        int btnW = BinderStyle.pageButtonWidth(imageWidth);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.prevPageButtonX(imageWidth), footer, btnW,
                BinderStyle.FOOTER_BTN_H, Component.translatable("stc.tutorial.prev").getString(), page > 0);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.nextPageButtonX(imageWidth), footer, btnW,
                BinderStyle.FOOTER_BTN_H, Component.translatable("stc.tutorial.next").getString(),
                page < total - 1);

        Component pageText = Component.translatable("stc.tutorial.page", page + 1, total);
        if (BinderStyle.footerCountFits(imageWidth, font.width(pageText))) {
            TextStyle.draw(graphics, font, pageText, BinderStyle.footerCountX(leftPos), footer + 3,
                    BinderStyle.INK_DIM);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int footer = BinderStyle.footerY(imageHeight);
        int total = Math.max(1, pages.size());
        int btnW = BinderStyle.pageButtonWidth(imageWidth);

        // 返回
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + BinderStyle.FOOTER_BACK_W
                && relY >= footer && relY < footer + BinderStyle.FOOTER_BTN_H) {
            minecraft.setScreen(back != null ? back.get()
                    : new SpireGateScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        // 上一页
        if (page > 0 && relX >= BinderStyle.prevPageButtonX(imageWidth)
                && relX < BinderStyle.prevPageButtonX(imageWidth) + btnW
                && relY >= footer && relY < footer + BinderStyle.FOOTER_BTN_H) {
            page--;
            return true;
        }
        // 下一页
        if (page < total - 1 && relX >= BinderStyle.nextPageButtonX(imageWidth)
                && relX < BinderStyle.nextPageButtonX(imageWidth) + btnW
                && relY >= footer && relY < footer + BinderStyle.FOOTER_BTN_H) {
            page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int total = Math.max(1, pages.size());
        if (total <= 1 || scrollY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY < 0 && page < total - 1) {
            page++;
        } else if (scrollY > 0 && page > 0) {
            page--;
        }
        return true;
    }
}
