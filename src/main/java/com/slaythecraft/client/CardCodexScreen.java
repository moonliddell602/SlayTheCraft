package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardTextDiff;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「卡牌图鉴 → 某个角色的卡牌」页（v0.4.0）。
 *
 * <p>展示的是**这个角色能用的牌**：该角色专属牌 + 基础牌（打击/格挡）+ 无色牌（只能从事件/遗物获得）。
 * 点任意一张 → **新开详情页**（沿用 v0.2.6 的约定）。</p>
 */
public class CardCodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    // v0.4.3：按反馈改成 4 列
    // v0.5.5.1（用户）："卡牌图鉴内的卡牌显示方式将和手牌显示的方式一致（包括美工等）"
    // → 格子里不再画"缩略图 + 名字 + 费用"的小方块，而是**整张卡面**（CardFace，与手牌同一份画法）
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
        int available = BinderStyle.footerY(imageHeight) - 6 - GRID_Y;
        return CardFace.visibleRows(available, cellH(), CELL_GAP);
    }

    private final int characterIndex;
    /** v0.4.1：无色牌图鉴（不按角色分，直接列全部无色牌）。 */
    private final boolean colorless;
    /** v0.4.6.2：状态与诅咒牌图鉴（不按角色分，直接列全部状态/诅咒牌）。 */
    private final boolean statusCards;
    /**
     * v0.5.1.4：**多人模式卡牌图鉴**（不按角色分，直接列全部多人卡牌）。
     *
     * <p>需求："新卡牌类型：多人卡牌 …… 为卡牌图鉴添加：多人模式卡牌图鉴。"</p>
     */
    private final boolean multiplayerCards;
    private int scroll;

    public CardCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int characterIndex) {
        this(menu, inventory, title, characterIndex, false);
    }

    public CardCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int characterIndex,
                           boolean colorless) {
        this(menu, inventory, title, characterIndex, colorless, false, false);
    }

    /** v0.4.6.2：状态与诅咒牌图鉴（与无色牌图鉴共用同一套网格与详情页）。 */
    public static CardCodexScreen statusAndCurse(CardBinderMenu menu, Inventory inventory, Component title) {
        return new CardCodexScreen(menu, inventory, title, 0, false, true, false);
    }

    /** v0.5.1.4：**多人模式卡牌图鉴**（只列多人卡牌）。 */
    public static CardCodexScreen multiplayer(CardBinderMenu menu, Inventory inventory, Component title) {
        return new CardCodexScreen(menu, inventory, title, 0, false, false, true);
    }

    private CardCodexScreen(CardBinderMenu menu, Inventory inventory, Component title, int characterIndex,
                            boolean colorless, boolean statusCards, boolean multiplayerCards) {
        super(menu, inventory, title);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        this.characterIndex = characterIndex;
        this.colorless = colorless;
        this.statusCards = statusCards;
        this.multiplayerCards = multiplayerCards;
    }

    private CharacterProfile profile() {
        return CharacterRegistry.byIndex(characterIndex);
    }

    private List<Card> cards() {
        // v0.4.7.0 需求："点击查看角色卡牌后的页面，从上到下按照普通，罕见，稀有品质筛选并分类卡牌。"
        if (multiplayerCards) {
            // v0.5.1.4：多人模式卡牌图鉴（不按角色分）
            return CardLibrary.sortedForCodex(CardLibrary.multiplayerCodex());
        }
        if (statusCards) {
            // v0.4.6.2：状态与诅咒牌图鉴（不按角色分）
            return CardLibrary.sortedForCodex(CardLibrary.statusAndCurseCodex());
        }
        if (colorless) {
            // v0.4.1：无色牌图鉴（不按角色分）
            return CardLibrary.sortedForCodex(CardLibrary.colorlessCodex());
        }
        CharacterProfile profile = profile();
        return profile == null ? List.of()
                : CardLibrary.sortedForCodex(CardLibrary.codexFor(profile.id().toString()));
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
        CharacterProfile profile = profile();
        Component heading = multiplayerCards ? Component.translatable("stc.codex.cards.multiplayer")
                : (statusCards ? Component.translatable("stc.codex.cards.status.of")
                        : (colorless ? Component.translatable("stc.codex.cards.colorless")
                                : (profile == null ? Component.literal("?")
                                        : Component.translatable("stc.codex.cards.of",
                                                Component.translatable(profile.nameKey())))));
        TextStyle.draw(graphics, font, heading, leftPos + BinderStyle.GRID_X, topPos + 40, BinderStyle.INK);

        List<Card> cards = cards();
        clampScroll(cards);
        int first = scroll * COLS;
        int cellW = cellW();
        int cellH = cellH();
        float scale = faceScale();
        for (int cell = 0; cell < COLS * visibleRows(); cell++) {
            int index = first + cell;
            if (index >= cards.size()) {
                break;
            }
            Card card = cards.get(index);
            int x = leftPos + BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP);
            int y = topPos + GRID_Y + (cell / COLS) * (cellH + CELL_GAP);
            boolean hovered = isInside(mouseX, mouseY, x, y, cellW, cellH);
            // v0.5.5.1：整张卡面（卡图 / 费用 / 卡名 / 类型 / 描述 / 稀有度边框）直接复用战斗内的画法。
            // 图鉴列的是**基础版**（强化版走详情页的「查看升级」），所以强化标记固定 0。
            CardFace.drawScaled(graphics, font, card,
                    ResourceLocation.fromNamespaceAndPath(com.slaythecraft.SlayTheCraft.MODID, card.id()),
                    new CardFace.View(card.cost(), card.keywordLabel(), card.enchantmentLabel(),
                            card.description(), 0, true, true, false, hovered),
                    x, y, scale);
        }
        if (cards.isEmpty()) {
            Component empty = Component.translatable("stc.codex.empty");
            TextStyle.draw(graphics, font, empty, leftPos + (imageWidth - font.width(empty)) / 2, topPos + 110,
                    BinderStyle.INK_DIM);
        }
        drawScrollbar(graphics, cards);

        int y = topPos + BinderStyle.footerY(imageHeight);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, y, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        Component count = Component.translatable("stc.codex.cards.count", cards.size());
        TextStyle.draw(graphics, font, count, leftPos + BinderStyle.GRID_X + 58, y + 3, BinderStyle.INK_DIM);
    }

    private void drawScrollbar(GuiGraphics graphics, List<Card> cards) {
        int rows = rows(cards);
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
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(colorless || statusCards || multiplayerCards
                        ? new CodexScreen(menu, minecraft.player.getInventory(), title)
                        : new CardCodexCharactersScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        List<Card> cards = cards();
        int cellW = cellW();
        int cellH = cellH();
        for (int cell = 0; cell < COLS * visibleRows(); cell++) {
            int index = scroll * COLS + cell;
            if (index >= cards.size()) {
                break;
            }
            int x = BinderStyle.GRID_X + (cell % COLS) * (cellW + CELL_GAP);
            int y = GRID_Y + (cell / COLS) * (cellH + CELL_GAP);
            if (isInside(mouseX - leftPos, mouseY - topPos, x, y, cellW, cellH)) {
                openDetail(cards.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Card> cards = cards();
        int visible = visibleRows();
        if (rows(cards) > visible) {
            int maxScroll = rows(cards) - visible;
            scroll = Math.max(0, Math.min(maxScroll, scroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * 重建"本页"（v0.4.6.3 修复）。
     *
     * <p>反馈原文："状态与诅咒牌图鉴中，详细查看卡牌后返回时被重定向到了铁甲战士卡牌图鉴。"
     * 原因是详情页的返回回调只带了 {@code characterIndex}，把无色牌 / 状态诅咒牌的标记丢了，
     * 于是重建出来的是"第 0 个角色（铁甲战士）的卡牌页"。这里把三个标记原样带上。</p>
     */
    private CardCodexScreen samePage() {
        return new CardCodexScreen(menu, minecraft.player.getInventory(), title, characterIndex, colorless,
                statusCards, multiplayerCards);
    }

    /** 卡牌详情（新页面）：费用 / 类型 / 品质 / 来源 / 描述（+「查看升级」按钮）。 */
    private void openDetail(Card card) {
        openDetail(card, null);
    }

    /**
     * 卡牌详情（v0.4.7.0：详情页里的「查看升级」会再开一页看强化版）。
     *
     * @param base 从强化版详情页返回时回到的那张卡（{@code null} = 当前就是基础版）
     */
    private void openDetail(Card card, Card base) {
        List<InfoPageScreen.BodyLine> body = new java.util.ArrayList<>();
        body.add(new InfoPageScreen.BodyLine(
                Component.translatable("stc.codex.detail.cost", card.cost(), card.type().label()).getString(),
                BinderStyle.INK));
        body.add(new InfoPageScreen.BodyLine(
                Component.translatable("stc.codex.detail.rarity", card.rarity().label()).getString(),
                BinderStyle.INK));
        body.add(new InfoPageScreen.BodyLine(sourceLabel(card), BinderStyle.INK));
        if (!card.keywordLabel().isEmpty()) {
            body.add(new InfoPageScreen.BodyLine(
                    Component.translatable("stc.codex.detail.keywords", card.keywordLabel()).getString(),
                    BinderStyle.INK));
        }
        // v0.5.1.2 需求："升级后的卡牌得到的改动以黄色字体高亮，例如数值变化。
        // 如果为『失去消耗』等内容则不用。" —— 正在看强化版时，把相对基础版改动的词标成金色。
        // 只删不增（例如升级后去掉「消耗」）不会产生高亮，因为升级版里没有对应内容可画。
        boolean[] marks = base == null ? null : CardTextDiff.marks(base.description(), card.description());
        body.add(marks == null ? new InfoPageScreen.BodyLine(card.description())
                : new InfoPageScreen.BodyLine(card.description(), marks));
        // v0.4.7.0 需求："详情页面中，点击「查看升级」按钮可以查看这张卡牌的升级后版本（如果有）。
        // 不能升级 / 没有升级的卡牌则没有此选项。" —— 于是不再把强化版写进正文，改成一个按钮
        Card upgraded = CardLibrary.resolve(card.id(), 1);
        InfoPageScreen.Action upgrade = upgraded == null ? null
                : new InfoPageScreen.Action(Component.translatable("stc.codex.detail.view_upgrade").getString(),
                        () -> openDetail(upgraded, card));
        // v0.5.1.2 开发者模式需求："可以在图鉴里直接向卡组添加卡牌。"
        // 只在开发者模式开着时出现（开关由服务端同步下来，见 ClientDevState）；点击只是发请求，
        // 真正的写入由服务端 DevDeckService 复核后执行。
        InfoPageScreen.Action devAdd = ClientDevState.enabled()
                ? new InfoPageScreen.Action(Component.translatable("stc.dev.add_card").getString(),
                        () -> ClientDevActions.addCard(card.id()))
                : null;
        java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> back = base == null
                ? this::samePage : () -> {
            openDetail(base);
            return samePage();
        };
        minecraft.setScreen(InfoPageScreen.richWithActions(menu, minecraft.player.getInventory(), title,
                Component.literal(card.name()), body, upgrade, devAdd, back));
    }

    /** 这张牌从哪来：角色专属 / 基础 / 无色 / 状态与诅咒。 */
    private String sourceLabel(Card card) {
        // v0.4.7.0：生成牌单独一条来源（"只能由遗物或卡牌生成获得"）
        if (CardLibrary.isGenerated(card.id())) {
            return Component.translatable("stc.codex.source.generated").getString();
        }
        if (CardLibrary.isBasic(card.id())) {
            return Component.translatable("stc.codex.source.basic").getString();
        }
        // v0.4.6.2：状态牌与诅咒牌既不属于角色也不是无色牌，单独写一条来源
        if (card.type() == com.slaythecraft.combat.core.CardType.STATUS) {
            return Component.translatable("stc.codex.source.status").getString();
        }
        if (card.type() == com.slaythecraft.combat.core.CardType.CURSE) {
            return Component.translatable("stc.codex.source.curse").getString();
        }
        if (card.character() == null) {
            return Component.translatable("stc.codex.source.colorless").getString();
        }
        CharacterProfile owner = CharacterRegistry.byId(
                net.minecraft.resources.ResourceLocation.parse(card.character()));
        String name = owner == null ? card.character()
                : Component.translatable(owner.nameKey()).getString();
        return Component.translatable("stc.codex.source.character", name).getString();
    }

    private static int rows(List<Card> cards) {
        return Math.max(1, (cards.size() + COLS - 1) / COLS);
    }

    private void clampScroll(List<Card> cards) {
        scroll = Math.max(0, Math.min(Math.max(0, rows(cards) - visibleRows()), scroll));
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static int typeColor(Card card) {
        return switch (card.type()) {
            case ATTACK -> 0xFFB5533C;
            case SKILL -> 0xFF4A7FB5;
            case POWER -> 0xFF7A4AB5;
            case STATUS, CURSE -> 0xFF6A6A6A;
        };
    }
}
