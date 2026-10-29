package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.core.FloorRoster;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.gate.SpireGateMenu;
import com.slaythecraft.network.ChallengeTargetPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * **本层可挑战名单**（v0.5.0.9）。
 *
 * <p>需求原文："显示：挑战第X层boss的功能按钮（把boss拉过来进行战斗）
 * 功能修改为：详情查看当前层数可以触发战斗的敌怪和boss（以阵营筛选，包括【尖塔护卫】【亡灵生物】等。）"</p>
 *
 * <p>数据来自纯逻辑 {@link FloorRoster#byFaction(int)}（和 {@code FloorPools} 同一条判定，
 * 所以名单里有的就打得起来）。点某一条会把它的实体 id 发给服务端，
 * 由 {@code CombatManager.challengeTarget} 校验"本层允许 + 半径内真的有这一只"再开战。</p>
 *
 * <p>列表可能比一屏长，所以支持**滚轮 / 翻页按钮**。</p>
 *
 * <p>v0.5.0.12 修了三处反馈：</p>
 * <ul>
 *   <li><b>"可挑战名称里，生物的名称没有汉化"</b> —— {@link FloorRoster.Entry#name()} 对非 BOSS
 *       存的是**实体 id 的路径**（{@code zombie}），那是给引擎用的稳定标识、不是给人看的。
 *       现在显示名走 {@link #displayName}（实体类型的本地化描述），引擎侧的 {@code name()} 保持不变；</li>
 *   <li><b>"右侧的ID存在会超出UI范围"</b> —— id 原来画在一个写死的 x 上，长 id
 *       （{@code minecraft:zombie_villager}）直接爬出面板。现在名字与 id 各占**固定列宽**，
 *       id 列用 {@link BinderStyle#text} 折行兜住，两列宽度之和加上间隙恰好等于内容区宽度；</li>
 *   <li><b>"页码被返回按钮覆盖"</b> —— 页码与「返回」原来都按写死坐标画（页码从 x=12 起、
 *       「返回」从 x=78 起），中文页码更宽就压住了。现在页脚按钮**从右往左排**，
 *       页码占据左侧剩余空间，放不下自动换短文案（{@code stc.roster.page.short}）。</li>
 * </ul>
 */
public class FloorRosterScreen extends AbstractContainerScreen<SpireGateMenu> {

    private static final int PANEL_W = 268;
    private static final int PANEL_H = 240;
    private static final int ROW_H = 18;
    private static final int CONTENT_X = 12;
    private static final int CONTENT_TOP = 34;
    /** 每页最多画多少行（含阵营标题行）。 */
    private static final int ROWS_PER_PAGE = 10;

    /** 左侧"名字"列的宽度（反馈："右侧的ID存在会超出UI范围"）。 */
    private static final int NAME_COL_W = 150;
    /** 名字列与 id 列之间的间隙。 */
    private static final int COL_GAP = 8;
    /** id 列的宽度 —— 由内容区宽度推出来，保证两列正好铺满（自检盯这条等式）。 */
    private static final int ID_COL_W = PANEL_W - CONTENT_X * 2 - NAME_COL_W - COL_GAP;
    /** 名字列的文字缩进（在行内再往右一点，看起来像列表项）。 */
    private static final int NAME_INDENT = 10;

    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int INK = 0xFFE8E8E8;
    private static final int INK_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int DANGER = 0xFFD05A5A;

    private int floor = 1;
    private int page;
    /** 把"阵营标题 + 条目"拉平成一维，便于分页与命中判定。 */
    private List<Row> rows = List.of();

    /** 一行（要么是阵营标题，要么是一条可挑战目标）。 */
    private record Row(String header, FloorRoster.Entry entry) {
        private static Row ofHeader(String text) {
            return new Row(text, null);
        }

        private static Row ofEntry(FloorRoster.Entry entry) {
            return new Row(null, entry);
        }

        private boolean isHeader() {
            return entry == null;
        }
    }

    public FloorRosterScreen(SpireGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    // ------------------------------------------------------------------ v0.5.0.12 名字本地化

    /**
     * 一条目标的**显示名**：实体类型的本地化名字（"僵尸" / "溺尸" / "僵尸村民"…）。
     *
     * <p>为什么不在 {@link FloorRoster} 里做：那一层是**纯逻辑**（零 Minecraft 依赖、能进裸 JVM 自检），
     * 拿不到语言文件。所以本地化留在客户端界面这一层做。</p>
     */
    public static String displayName(FloorRoster.Entry entry) {
        return displayName(entry.entityId(), entry.name());
    }

    /**
     * {@link #displayName(FloorRoster.Entry)} 的裸参数版本（自检直接调这个）。
     *
     * <p>找不到实体类型（附属包注册的、或 id 拼错）时**退回原始名字**，绝不画成空白。</p>
     */
    public static String displayName(String entityId, String fallback) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(entityId);
            if (id != null) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (type != null) {
                    String name = type.getDescription().getString();
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // 注册表不可用（无头环境）或 id 非法 —— 退回 fallback
        }
        return fallback == null ? "" : fallback;
    }

    // ------------------------------------------------------------------ v0.5.0.12 两列几何

    /** id 列的 x（相对面板左边）。 */
    public static int idColumnX() {
        return CONTENT_X + NAME_COL_W + COL_GAP;
    }

    /** 名字列与 id 列是否恰好铺满内容区（自检用）。 */
    public static boolean columnsFit() {
        return CONTENT_X * 2 + NAME_COL_W + COL_GAP + ID_COL_W == PANEL_W;
    }

    /** 两列的宽度（自检用）。 */
    public static int[] columnWidths() {
        return new int[] {NAME_COL_W, ID_COL_W};
    }

    /** 面板尺寸（自检用）。 */
    public static int[] panelSize() {
        return new int[] {PANEL_W, PANEL_H};
    }

    /** 一行（作为点击 / 高亮目标）相对面板的矩形：x / y / 宽 / 高。绘制与命中判定共用它。 */
    public static int[] rowRect(int rowIndex, int firstRowOnPage) {
        return new int[] {CONTENT_X, CONTENT_TOP + (rowIndex - firstRowOnPage) * ROW_H,
                NAME_COL_W + COL_GAP + ID_COL_W, ROW_H};
    }

    /** 显示 id：去掉 {@code minecraft:} 前缀（省宽度，反正绝大多数都是原版生物）。 */
    public static String shortId(String entityId) {
        if (entityId == null) {
            return "";
        }
        return entityId.startsWith("minecraft:") ? entityId.substring("minecraft:".length()) : entityId;
    }

    /** 页脚按钮的 y（相对面板顶）。 */
    public static int footerY() {
        return PANEL_H - 22;
    }

    @Override
    protected void init() {
        super.init();
        PlayerCardData data = data();
        this.floor = data == null ? 1 : data.floor();
        this.rows = buildRows(this.floor);
        clampPage();
    }

    private static PlayerCardData data() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return StcAttachments.dataOf(Minecraft.getInstance().player);
    }

    /** 把名单拉平：每个阵营一个标题行 + 它的成员行。 */
    static List<Row> buildRows(int floor) {
        List<Row> list = new ArrayList<>();
        for (Map.Entry<Faction, List<FloorRoster.Entry>> group : FloorRoster.byFaction(floor).entrySet()) {
            list.add(Row.ofHeader(group.getKey().label() + "（" + group.getValue().size() + "）"));
            for (FloorRoster.Entry entry : group.getValue()) {
                list.add(Row.ofEntry(entry));
            }
        }
        return List.copyOf(list);
    }

    /** 名单要几页。 */
    public static int pagesFor(int rowCount) {
        return Math.max(1, (Math.max(0, rowCount) + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    /** 每页行数（自检用）。 */
    public static int rowsPerPage() {
        return ROWS_PER_PAGE;
    }

    private void clampPage() {
        page = Math.max(0, Math.min(pagesFor(rows.size()) - 1, page));
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
        // ---- v0.5.0.15：顺序**必须**是 先画面板背景、再画文字 ----
        //
        // 反馈（配图）：名单的行画到了面板边框**外面**（"尖塔护卫(3)"等跑到框上方），
        // 标题与第一个阵营标题还叠在一起。
        //
        // 根因是顺序：以前先调 {@code super.render(...)}（它内部画 renderBg 面板底），
        // 然后在 renderLabels 里画标题 + 在 render 里画行；而 {@code AbstractContainerScreen.render}
        // 会在画完 renderBg **之后**再画一次 renderLabels，两次绘制的时机与顺序一旦错开，
        // 文字就会落在底之上/之外。这里改成**显式自己控制顺序**：面板底 → 标题 → 行 → 页脚，
        // 全程只用一份坐标，不再依赖 super 的绘制次序。
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, PANEL_EDGE);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG);

        int pages = pagesFor(rows.size());
        TextStyle.draw(graphics, font, Component.translatable("stc.roster.title", floor).getString(),
                leftPos + CONTENT_X, topPos + 10, GOLD);

        if (rows.isEmpty()) {
            TextStyle.draw(graphics, font, Component.translatable("stc.roster.empty").getString(),
                    leftPos + CONTENT_X, topPos + CONTENT_TOP, INK_DIM);
        }

        int first = page * ROWS_PER_PAGE;
        int last = Math.min(rows.size(), first + ROWS_PER_PAGE);
        // v0.5.0.15：把列表**裁剪在内容区内**（结构性保险）。
        // 反馈配图里行画到了面板边框之外；即使坐标算错，裁剪也能保证"列表永远不会越出面板"。
        int listTop = topPos + CONTENT_TOP - 2;
        int listBottom = topPos + footerY() - 2;
        graphics.enableScissor(leftPos + 2, listTop, leftPos + imageWidth - 2, listBottom);
        for (int i = first; i < last; i++) {
            Row row = rows.get(i);
            int[] rect = rowRect(i, first);
            int y = topPos + rect[1];
            if (row.isHeader()) {
                graphics.fill(leftPos + CONTENT_X, y + ROW_H - 2, leftPos + imageWidth - CONTENT_X, y + ROW_H - 1,
                        PANEL_EDGE);
                TextStyle.draw(graphics, font, row.header(), leftPos + CONTENT_X, y + 4, GOLD);
                continue;
            }
            FloorRoster.Entry entry = row.entry();
            boolean hovered = isInside(mouseX, mouseY, leftPos + rect[0], y, rect[2], rect[3]);

            // 名字列：本地化名，单行折行（长名字也不会压到 id 列）
            BinderStyle.text(graphics, font, entryLabel(entry), leftPos + CONTENT_X + NAME_INDENT, y + 4,
                    NAME_COL_W - NAME_INDENT, 1, hovered ? GOLD : (entry.boss() ? DANGER : INK));
            // id 列：自己的列宽 + 折行兜住（反馈的"ID 超出 UI"）
            BinderStyle.text(graphics, font, shortId(entry.entityId()), leftPos + idColumnX(), y + 4,
                    ID_COL_W, 1, INK_DIM);
        }
        graphics.disableScissor();

        // ---- 页脚（v0.5.0.12：按钮从右往左排，页码不会再被「返回」压住）
        int footer = topPos + footerY();
        if (pages > 1) {
            String pageText = BinderStyle.rosterPageText(font, imageWidth, page + 1, pages,
                    Component.translatable("stc.roster.page", page + 1, pages).getString(),
                    Component.translatable("stc.roster.page.short", page + 1, pages).getString());
            TextStyle.draw(graphics, font, pageText, leftPos + BinderStyle.GRID_X,
                    footer + BinderStyle.ROSTER_FOOTER_TEXT_Y, INK_DIM);
            button(graphics, leftPos + BinderStyle.rosterPageButtonX(imageWidth), footer, BinderStyle.ROSTER_BTN_W,
                    14, Component.translatable("stc.roster.prev").getString(), true);
        }
        button(graphics, leftPos + BinderStyle.rosterBackButtonX(imageWidth), footer, BinderStyle.ROSTER_BTN_W, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    private String entryLabel(FloorRoster.Entry entry) {
        String shown = displayName(entry);
        if (entry.boss()) {
            return Component.translatable("stc.roster.boss", shown, entry.bossHp()).getString();
        }
        return shown;
    }

    private void button(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean enabled) {
        graphics.fill(x, y, x + w, y + h, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + w, y + h, enabled ? PANEL_EDGE : 0xFF404048);
        BinderStyle.text(graphics, font, label, x + 3, y + 3, w - 6, 1, enabled ? INK : INK_DIM);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }

    /** 点是否落在矩形里（本类的局部 helper；容器屏幕没有现成的）。 */
    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        int footer = footerY();
        int pages = pagesFor(rows.size());

        // 返回（右对齐，绘制与命中判定共用 BinderStyle 的几何）
        if (relX >= BinderStyle.rosterBackButtonX(imageWidth)
                && relX < BinderStyle.rosterBackButtonX(imageWidth) + BinderStyle.ROSTER_BTN_W
                && relY >= footer && relY < footer + 14) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.setScreen(new SpireGateScreen(menu, minecraft.player.getInventory(), title));
            }
            return true;
        }
        // 翻页
        if (pages > 1
                && relX >= BinderStyle.rosterPageButtonX(imageWidth)
                && relX < BinderStyle.rosterPageButtonX(imageWidth) + BinderStyle.ROSTER_BTN_W
                && relY >= footer && relY < footer + 14) {
            if (page < pages - 1) {
                page++;
            } else {
                page = 0;
            }
            return true;
        }
        // 点某一条 → 挑战它（矩形与绘制共用 rowRect）
        int first = page * ROWS_PER_PAGE;
        int last = Math.min(rows.size(), first + ROWS_PER_PAGE);
        for (int i = first; i < last; i++) {
            Row row = rows.get(i);
            if (row.isHeader()) {
                continue;
            }
            int[] rect = rowRect(i, first);
            if (isInside(relX, relY, rect[0], rect[1], rect[2], rect[3])) {
                PacketDistributor.sendToServer(new ChallengeTargetPayload(row.entry().entityId()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int pages = pagesFor(rows.size());
        if (pages <= 1) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY < 0) {
            page = (page + 1) % pages;
        } else if (scrollY > 0) {
            page = (page - 1 + pages) % pages;
        }
        return true;
    }
}
