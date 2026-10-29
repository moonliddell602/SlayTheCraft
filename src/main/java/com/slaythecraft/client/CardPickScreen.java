package com.slaythecraft.client;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 通用「卡牌列表页」（v0.2.7 起；v0.0.3 W5 增加"升级"用途）。
 *
 * <p>三种用途共用：收纳包（只看）/ 卡牌回收站与卡牌商人（删除）/ 篝火锻造（升级）。
 * 点一张卡永远**新开详情页**，动作按钮在详情页里（删除还要再进一层确认页）——
 * 遵循"任何二级引导都新开页面"的约定。</p>
 *
 * @param <M> 复用的菜单类型
 */
public class CardPickScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    /** 详情页里的动作。 */
    public enum Action {
        /** 只看，没有动作按钮。 */
        NONE,
        /** 删除（花金币，需二次确认）。 */
        DELETE,
        /** 升级（篝火锻造，直接执行）。 */
        UPGRADE
    }

    // v0.4.3：改成 4 列
    // v0.5.5.1（用户）："拥有卡牌和卡牌图鉴内的卡牌显示方式将和手牌显示的方式一致（包括美工等）"
    // → 格子里画**整张卡面**（CardFace，与战斗内手牌同一份画法），不再是"色块 + 名字 + 费用"
    private static final int COLS = 4;
    private static final int CELL_GAP = 4;
    private static final int GRID_Y = 54;

    /** 一格的宽度（按面板可用宽度平分）。 */
    private int cellW() {
        return CardFace.gridCellW(imageWidth - BinderStyle.GRID_X * 2, COLS, CELL_GAP);
    }

    /** 一格的高度（按卡面比例）。 */
    private int cellH() {
        return CardFace.gridCellH(cellW());
    }

    /** 卡面在这个格子里缩放多少（只缩小）。 */
    private float faceScale() {
        return CardFace.scaleFor(cellW());
    }

    /** 这一页能放几行（按格高与页脚位置算）。 */
    private int visibleRows() {
        int available = BinderStyle.footerY(imageHeight) - 34 - GRID_Y;
        return CardFace.visibleRows(available, cellH(), CELL_GAP);
    }

    private final Action action;
    private final String hint;
    private final IntConsumer sender;
    private final Supplier<Screen> back;

    private int scroll;

    public CardPickScreen(M menu, Inventory inventory, Component title, Action action, String hint,
                          IntConsumer sender, Supplier<Screen> back) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.action = action;
        this.hint = hint;
        this.sender = sender;
        this.back = back;
    }

    /** 兼容旧调用（只看 / 删除两种）。 */
    public CardPickScreen(M menu, Inventory inventory, Component title, boolean allowDelete, String hint,
                          IntConsumer sender, Supplier<Screen> back) {
        this(menu, inventory, title, allowDelete ? Action.DELETE : Action.NONE, hint, sender, back);
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
        PlayerCardData data = BinderStyle.data();
        if (data == null) {
            return;
        }
        clampScroll(data);
        int first = scroll * COLS;
        int cellW = cellW();
        int cellH = cellH();
        float scale = faceScale();
        for (int cell = 0; cell < COLS * visibleRows(); cell++) {
            int index = first + cell;
            if (index >= data.cardCount()) {
                break;
            }
            int x = leftPos + BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP);
            int y = topPos + GRID_Y + (cell / COLS) * (cellH + CELL_GAP);
            CardInstance instance = data.cards().get(index);
            Card definition = CardLibrary.resolve(instance.id().getPath(), instance.upgrades());
            if (definition == null) {
                continue;
            }
            boolean hovered = isInside(mouseX, mouseY, x, y, cellW, cellH);
            // 升级用途：已经到强化上限（或没有强化版）的卡画灰
            boolean upgradable = action != Action.UPGRADE || data.canUpgradeAt(index);
            // v0.5.5.1：整张卡面（与手牌同一份画法）；"能不能再强化"用 playable 通道表达成灰显
            CardFace.drawScaled(graphics, font, definition,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.slaythecraft.SlayTheCraft.MODID, definition.id()),
                    new CardFace.View(definition.cost(), definition.keywordLabel(),
                            instance.enchantmentLabel(), definition.description(), instance.upgrades(),
                            upgradable, true, false, hovered),
                    x, y, scale);
            // 附魔文本走**实例**数据（同一张卡的不同副本可以附不同的魔），覆盖卡面里的那一行
            if (instance.enchanted()) {
                String label = instance.enchantmentLabel();
                TextStyle.draw(graphics, font, font.plainSubstrByWidth(label, cellW - 12),
                        x + 6, y + Math.round(14 * scale), 0xFFB48AD8);
            }
        }
        if (data.cardCount() == 0) {
            Component empty = Component.translatable("stc.binder.empty");
            TextStyle.draw(graphics, font, empty, leftPos + (imageWidth - font.width(empty)) / 2, topPos + 110,
                    BinderStyle.INK_DIM);
        } else if (action != Action.NONE && hint != null) {
            TextStyle.draw(graphics, font, hint, leftPos + BinderStyle.GRID_X, topPos + imageHeight - 44,
                    BinderStyle.ACCENT);
        }
        drawScrollbar(graphics, data);

        int y = topPos + BinderStyle.footerY(imageHeight);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, y, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        Component count = Component.translatable("stc.binder.count", data.cardCount());
        TextStyle.draw(graphics, font, count, leftPos + BinderStyle.GRID_X + 58, y + 3, BinderStyle.INK_DIM);
    }

    private void drawScrollbar(GuiGraphics graphics, PlayerCardData data) {
        int rows = rows(data);
        int visible = visibleRows();
        if (rows <= visible) {
            return;
        }
        int cellH = cellH();
        int x = leftPos + imageWidth - 12;
        int top = topPos + GRID_Y;
        int height = visible * (cellH + CELL_GAP) - CELL_GAP;
        graphics.fill(x, top, x + 4, top + height, BinderStyle.BOOK_LINE);
        int thumb = Math.max(14, height * visible / rows);
        int maxScroll = rows - visible;
        int offset = maxScroll == 0 ? 0 : (height - thumb) * scroll / maxScroll;
        graphics.fill(x, top + offset, x + 4, top + offset + thumb, BinderStyle.ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        PlayerCardData data = BinderStyle.data();
        if (data == null || minecraft == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(back.get());
            return true;
        }
        int cellW = cellW();
        int cellH = cellH();
        for (int cell = 0; cell < COLS * visibleRows(); cell++) {
            int index = scroll * COLS + cell;
            if (index >= data.cardCount()) {
                break;
            }
            int x = BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP);
            int y = GRID_Y + (cell / COLS) * (cellH + CELL_GAP);
            if (relX >= x && relX < x + cellW && relY >= y && relY < y + cellH) {
                openDetail(data, index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PlayerCardData data = BinderStyle.data();
        int visible = visibleRows();
        if (data != null && rows(data) > visible) {
            int maxScroll = rows(data) - visible;
            scroll = Math.max(0, Math.min(maxScroll, scroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 打开卡牌详情页（新页面）；动作按钮按用途显示。 */
    private void openDetail(PlayerCardData data, int index) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        CardInstance instance = data.cards().get(index);
        Card definition = CardLibrary.resolve(instance.id().getPath(), instance.upgrades());
        List<String> body = new java.util.ArrayList<>();
        body.add("ID：" + instance.id());
        if (definition != null) {
            body.add("费用 " + definition.cost() + " · " + definition.type().label() + " · "
                    + definition.rarity().label());
            if (!definition.keywordLabel().isEmpty()) {
                body.add("关键字：" + definition.keywordLabel());
            }
            if (!definition.enchantmentLabel().isEmpty()) {
                body.add("附魔：" + definition.enchantmentLabel());
            }
            body.add(definition.description());
        }
        if (instance.upgraded()) {
            body.add("强化：+" + instance.upgrades());
        }

        Supplier<Screen> backToPicker = () -> this;
        String actionLabel = null;
        Runnable actionRunner = null;
        if (action == Action.DELETE) {
            actionLabel = Component.translatable("stc.binder.delete",
                    com.slaythecraft.binder.CardDeleteService.cost(data)).getString();
            actionRunner = () -> minecraft.setScreen(new ConfirmPageScreen<>(menu, minecraft.player.getInventory(),
                    title, Component.translatable("stc.binder.confirm.title"),
                    List.of(Component.translatable("stc.binder.confirm",
                                    com.slaythecraft.binder.CardDeleteService.cost(data),
                                    definition == null ? instance.id().getPath() : definition.name()).getString(),
                            Component.translatable("stc.binder.confirm.warning").getString()),
                    Component.translatable("stc.binder.confirm.yes").getString(), true,
                    () -> sender.accept(index), backToPicker));
        } else if (action == Action.UPGRADE && data.canUpgradeAt(index)) {
            actionLabel = Component.translatable("stc.campfire.forge.this").getString();
            // 升级是纯收益，直接执行（不需要二次确认）
            actionRunner = () -> {
                sender.accept(index);
                minecraft.setScreen(back.get());
            };
        } else if (action == Action.NONE && ClientDevState.enabled()) {
            // v0.5.1.2 开发者模式需求："可以直接在查看卡组页面删牌。"
            // 收纳包里的"查看牌组"本来是**只读**的（删除只在回收站 / 卡牌商人处提供），
            // 开发者模式下才多出这个按钮：请求发给服务端，由 DevDeckService 复核后删。
            String cardId = instance.id().getPath();
            actionLabel = Component.translatable("stc.dev.remove_card").getString();
            actionRunner = () -> ClientDevActions.removeCard(cardId);
        }

        minecraft.setScreen(new InfoPageScreen<>(menu, minecraft.player.getInventory(), title,
                definition == null ? Component.literal(instance.id().getPath()) : Component.literal(definition.name()),
                body, actionLabel, actionRunner, backToPicker));
    }

    // ------------------------------------------------------------------ 小工具

    private static int rows(PlayerCardData data) {
        return Math.max(1, (data.cardCount() + COLS - 1) / COLS);
    }

    private void clampScroll(PlayerCardData data) {
        scroll = Math.max(0, Math.min(Math.max(0, rows(data) - visibleRows()), scroll));
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** 卡牌类型的底色（原来叫 typeColor；名字保留一个中性说法）。 */
    private static int upstreamColor(Card definition) {
        if (definition == null) {
            return 0xFF4A4A54;
        }
        return switch (definition.type()) {
            case ATTACK -> 0xFFB5533C;
            case SKILL -> 0xFF4A7FB5;
            case POWER -> 0xFF7A4AB5;
            case STATUS, CURSE -> 0xFF6A6A6A;
        };
    }
}
