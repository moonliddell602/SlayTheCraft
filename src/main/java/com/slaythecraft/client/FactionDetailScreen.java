package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.mc.MobCodex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 阵营详情页（v0.4.6）。
 *
 * <p>需求原文："点击查看阵营详情后，显示所属阵营内的怪物图鉴。点击可以查看怪物图鉴详情……
 * 例：点击敌人阵营图鉴→点击亡灵生物阵营→上方显示阵营描述和特性，在下方显示怪物图鉴
 * →点击怪物图鉴查看怪物详情"。</p>
 *
 * <p>所以这一页分两段：上面是阵营描述 + 特性，下面是该阵营的**怪物图鉴网格**（滚轮翻页）。</p>
 */
public class FactionDetailScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int COLS = 2;
    private static final int VISIBLE_ROWS = 3;
    private static final int CELL_H = 26;
    private static final int CELL_GAP = 4;
    private static final int CELL_X = 12;
    private static final int CELL_W = 244;

    private final Faction faction;
    private final List<MobCodex.MobEntry> mobs;
    private final List<String> descriptionLines;
    private int scroll;

    public FactionDetailScreen(CardBinderMenu menu, Inventory inventory, Component title, Faction faction) {
        super(menu, inventory, title);
        this.faction = faction;
        this.mobs = MobCodex.of(faction);
        this.imageWidth = BinderStyle.WIDTH;
        this.imageHeight = BinderStyle.HEIGHT;
        // 描述按两列宽度折行后塞进上半部分
        this.descriptionLines = new ArrayList<>();
    }

    @Override
    public void init() {
        this.imageWidth = BinderStyle.widthFor(this);
        this.imageHeight = BinderStyle.heightFor(this);
        super.init();
        descriptionLines.clear();
        descriptionLines.addAll(BinderStyle.wrap(font, faction.description(), imageWidth - 24));
        if (!faction.traits().isEmpty()) {
            for (String trait : faction.traits()) {
                descriptionLines.addAll(BinderStyle.wrap(font,
                        Component.translatable("stc.codex.faction.trait", trait).getString(), imageWidth - 24));
            }
        }
        if (faction.neutral()) {
            descriptionLines.add(Component.translatable("stc.codex.faction.neutral").getString());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        BinderStyle.book(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    private int mobsTop() {
        return 58 + descriptionLines.size() * 10 + 12;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BinderStyle.header(graphics, font, leftPos, topPos);
        TextStyle.draw(graphics, font, faction.label(), leftPos + CELL_X, topPos + 38, BinderStyle.INK);

        // ---- 上半：描述 + 特性
        int y = topPos + 52;
        for (String line : descriptionLines) {
            TextStyle.draw(graphics, font, line, leftPos + CELL_X, y, BinderStyle.INK_DIM);
            y += 10;
        }

        // ---- 下半：怪物图鉴
        int top = topPos + mobsTop();
        Component heading = Component.translatable("stc.codex.faction.mobs");
        TextStyle.draw(graphics, font, heading, leftPos + CELL_X, top - 12, BinderStyle.INK);

        clampScroll();
        int first = scroll * (COLS * VISIBLE_ROWS);
        for (int cell = 0; cell < COLS * VISIBLE_ROWS; cell++) {
            int index = first + cell;
            if (index >= mobs.size()) {
                break;
            }
            MobCodex.MobEntry mob = mobs.get(index);
            int x = leftPos + CELL_X;
            int cellY = top + cell * (CELL_H + CELL_GAP);
            boolean hovered = isInside(mouseX, mouseY, x, cellY, CELL_W, CELL_H);
            graphics.fill(x, cellY, x + CELL_W, cellY + CELL_H, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
            BinderStyle.border(graphics, x, cellY, x + CELL_W, cellY + CELL_H,
                    hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
            TextStyle.draw(graphics, font, mob.name(), x + 6, cellY + 3, BinderStyle.INK);
            Component tier = Component.literal(mob.tierLabel());
            TextStyle.draw(graphics, font, tier, x + 6, cellY + 14, tierColor(mob));
            // v0.4.6.2：只显示尖塔战斗生命值（原版生命值不再展示）
            Component hp = Component.translatable("stc.codex.mob.hp_short", mob.battleHp());
            TextStyle.draw(graphics, font, hp, x + 58, cellY + 14, BinderStyle.INK_DIM);
            Component open = Component.translatable("stc.codex.open");
            TextStyle.draw(graphics, font, open, x + CELL_W - 6 - font.width(open), cellY + 8,
                    BinderStyle.ACCENT);
        }
        if (mobs.isEmpty()) {
            Component empty = Component.translatable("stc.codex.faction.mobs.empty");
            TextStyle.draw(graphics, font, empty, leftPos + CELL_X, top + 6, BinderStyle.INK_DIM);
        }
        if (mobs.size() > COLS * VISIBLE_ROWS) {
            Component page = Component.translatable("stc.question.pick.page", scroll + 1, pages());
            TextStyle.draw(graphics, font, page, leftPos + CELL_X + CELL_W - font.width(page), top - 12,
                    BinderStyle.INK_DIM);
        }

        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X,
                topPos + BinderStyle.footerY(imageHeight), 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    private int pages() {
        return Math.max(1, (mobs.size() + COLS * VISIBLE_ROWS - 1) / (COLS * VISIBLE_ROWS));
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(pages() - 1, scroll));
    }

    private static int tierColor(MobCodex.MobEntry mob) {
        return switch (mob.tier()) {
            case BOSS -> 0xFFB5533C;
            case ELITE -> 0xFF7A4AB5;
            case NORMAL -> 0xFF3F6B3F;
            case NONE -> 0xFF6A6A6A;
        };
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
            minecraft.setScreen(new FactionCodexScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        int top = mobsTop();
        int first = scroll * (COLS * VISIBLE_ROWS);
        for (int cell = 0; cell < COLS * VISIBLE_ROWS; cell++) {
            int index = first + cell;
            if (index >= mobs.size()) {
                break;
            }
            int cellY = top + cell * (CELL_H + CELL_GAP);
            if (isInside(relX, relY, CELL_X, cellY, CELL_W, CELL_H)) {
                minecraft.setScreen(new MobDetailScreen(menu, minecraft.player.getInventory(), title,
                        mobs.get(index), () -> this));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages() > 1) {
            scroll = Math.max(0, Math.min(pages() - 1, scroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
