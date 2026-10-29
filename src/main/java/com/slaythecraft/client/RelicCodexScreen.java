package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.combat.core.RelicTrigger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「遗物图鉴」页（v0.4.0）：列出模组里**全部**遗物（不只是自己拥有的），点进去看完整描述。
 *
 * <p>初始遗物会标明归属角色（需求："不同角色不能获得其他角色的初始遗物，这是固定角色特有的"）。</p>
 *
 * <p>v0.4.6.7 需求原文：</p>
 * <ul>
 *   <li>"未详情查看的图鉴外侧不再显示获取方式。获取方式，机制，趣闻描述都显示在详情页面。" ——
 *       所以这一页的格子里**只剩图标 + 名字**（+ 页脚的遗物总数），
 *       获取方式 / 机制 / 趣闻（触发点）全部搬进 {@link #openDetail} 打开的详情页；</li>
 *   <li>"遗物图鉴外部的排列修改为每行3个。" —— 列数固定为 {@link #columns()}（3），行数见
 *       {@link #rowsFor(int)}（两个都是公开静态方法，自检可以直接算）。</li>
 * </ul>
 */
public class RelicCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    /** v0.4.6.7 需求："遗物图鉴外部的排列修改为每行3个。" */
    public static final int COLS = 3;
    private static final int ROW_H = 22;
    /** 3 列正好占满书页正文宽度：16 + 3×76 + 2×4 = 268。 */
    private static final int ROW_W = 76;
    private static final int CELL_GAP = 4;
    private static final int GRID_Y = 50;
    /** 列表格子里遗物图标位的边长（约定贴图存在时才画）。 */
    private static final int ICON = 16;

    /** 每行几个（v0.4.6.7 固定 3 个；抽成静态方法方便自检直接调用）。 */
    public static int columns() {
        return COLS;
    }

    /** {@code count} 件遗物要排几行（列数见 {@link #columns()}；空列表也算 1 行）。 */
    public static int rowsFor(int count) {
        return Math.max(1, (Math.max(0, count) + COLS - 1) / COLS);
    }

    public RelicCodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
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
        BinderStyle.header(graphics, font, leftPos, topPos);
        Component heading = Component.translatable("stc.codex.relics.heading");
        TextStyle.draw(graphics, font, heading, leftPos + BinderStyle.GRID_X, topPos + 38, BinderStyle.INK);

        List<Relic> relics = RelicLibrary.all();
        int rowH = rowHeight(relics.size());
        for (int i = 0; i < relics.size(); i++) {
            Relic relic = relics.get(i);
            int x = leftPos + BinderStyle.GRID_X + (i % COLS) * (ROW_W + CELL_GAP);
            int y = topPos + GRID_Y + (i / COLS) * rowH;
            boolean hovered = mouseX >= x && mouseX < x + ROW_W && mouseY >= y && mouseY < y + rowH - 2;
            graphics.fill(x, y, x + ROW_W, y + rowH - 2, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, y, x + ROW_W, y + rowH - 2,
                    RelicLibrary.isStarting(relic.id()) ? BinderStyle.ACCENT
                            : (hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE));
            // v0.4.6.7 需求："未详情查看的图鉴外侧不再显示获取方式。"
            // 所以格子里只有【图标 + 名字】，获取方式 / 机制 / 趣闻一律进详情页（见 openDetail）。
            int textX = x + 4;
            if (drawIcon(graphics, relic, x + 3, y + (rowH - 2 - ICON) / 2)) {
                textX = x + 3 + ICON + 3;
            }
            // 名字按格子宽度折行（长名字也不截断）
            List<String> nameLines = BinderStyle.wrap(font, relic.name(), x + ROW_W - 4 - textX);
            int lineY = y + Math.max(2, (rowH - 2 - nameLines.size() * 10) / 2);
            for (String line : nameLines) {
                if (lineY + 8 > y + rowH - 2) {
                    break;
                }
                TextStyle.draw(graphics, font, line, textX, lineY, BinderStyle.INK);
                lineY += 10;
            }
        }

        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, topPos + BinderStyle.footerY(imageHeight), 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        Component count = Component.translatable("stc.codex.relics.count", relics.size());
        TextStyle.draw(graphics, font, count, leftPos + BinderStyle.GRID_X + 58, topPos + BinderStyle.footerY(imageHeight) + 3,
                BinderStyle.INK_DIM);
    }

    /** 初始遗物归属于哪个角色。 */
    private static String ownerOf(Relic relic) {
        for (CharacterProfile profile : CharacterRegistry.all()) {
            if (relic.id().equals(profile.startingRelic())) {
                return Component.translatable(profile.nameKey()).getString();
            }
        }
        return "?";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(new CodexScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        List<Relic> relics = RelicLibrary.all();
        int rowH = rowHeight(relics.size());
        for (int i = 0; i < relics.size(); i++) {
            int x = BinderStyle.GRID_X + (i % COLS) * (ROW_W + CELL_GAP);
            int y = GRID_Y + (i / COLS) * rowH;
            if (relX >= x && relX < x + ROW_W && relY >= y && relY < y + rowH - 2) {
                openDetail(relics.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /** 行高按可用空间自适应（遗物越加越多时自动收缩，不会顶出书页）。 */
    private int rowHeight(int count) {
        int available = BinderStyle.footerY(imageHeight) - GRID_Y - 4;
        return Math.max(14, Math.min(ROW_H, available / rowsFor(count) - 2));
    }

    /**
     * 列表格子里的遗物图标位（v0.4.6.7 需求："只留图标 + 名字"）。
     *
     * <p>v0.5.1.3：路径与画法统一挪到 {@link RelicIcons}（战斗界面的遗物栏也要用同一套约定）。</p>
     *
     * <p>v0.5.5.1（用户）："为遗物图鉴的遗物补全小图标显示" —— 以前这里只认**自定义 PNG**，
     * 所以那 18 件"用原版物品贴图"的遗物在图鉴里**什么都看不到**（战斗里的遗物栏反而有）。
     * 现在改走 {@link RelicIcons#draw}（自定义 PNG ＞ 原版物品图标 ＞ 程序化符文），
     * 图鉴与战斗栏就完全一致了。</p>
     *
     * @return 有没有画出图标（调用方用它决定名字从哪个 x 开始）
     */
    private static boolean drawIcon(GuiGraphics graphics, Relic relic, int x, int y) {
        if (!RelicIcons.available(relic)) {
            return false;
        }
        RelicIcons.draw(graphics, net.minecraft.client.Minecraft.getInstance().font, relic, relic.name(), x, y,
                ICON);
        return true;
    }

    /**
     * 遗物详情（**新页面**，v0.4.6.7 按需求重排）。
     *
     * <p>需求原文："获取方式，机制，趣闻描述都显示在详情页面。" 所以这一页按
     * 机制 → 趣闻 / 触发点 → 获取方式 三段写全（列表页那边一句都不留）。</p>
     */
    private void openDetail(Relic relic) {
        List<String> body = new ArrayList<>();
        // ---- 机制：这件遗物怎么结算
        body.add(Component.translatable("stc.codex.detail.mechanics").getString());
        body.add(relic.description());
        body.add("");
        // ---- 趣闻 / 触发点：它在什么时机出手
        body.add(Component.translatable("stc.codex.relics.flavor").getString());
        String triggers = triggerLabels(relic);
        body.add(triggers.isEmpty()
                ? Component.translatable("stc.codex.relics.triggers.none").getString()
                : Component.translatable("stc.codex.relics.triggers", relic.triggers().size()).getString()
                        + " " + triggers);
        body.add("");
        // ---- 获取方式：初始遗物（点名归属角色）还是可掉落
        body.add(Component.translatable("stc.codex.relics.source").getString());
        if (RelicLibrary.isStarting(relic.id())) {
            body.add(Component.translatable("stc.codex.relics.starting.long", ownerOf(relic)).getString());
        } else {
            body.add(Component.translatable("stc.codex.relics.drop.long").getString());
        }
        // v0.5.5.1（用户）："遗物图鉴 不再显示ID" —— 详情页里那一行 ID 已去掉
        minecraft.setScreen(new InfoPageScreen<>(menu, minecraft.player.getInventory(), title,
                Component.literal(relic.name()), body, null, null,
                () -> new RelicCodexScreen(menu, minecraft.player.getInventory(), title)));
    }

    /** 这件遗物的触发点（按枚举声明顺序排好，拼成一行；没有触发点就是空串）。 */
    private static String triggerLabels(Relic relic) {
        List<String> labels = new ArrayList<>();
        for (RelicTrigger trigger : RelicTrigger.values()) {
            if (relic.firesOn(trigger)) {
                labels.add(trigger.label());
            }
        }
        return String.join("、", labels);
    }
}
