package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.mc.MobCodex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 怪物图鉴详情页（v0.4.6，独立新页面）。
 *
 * <p>需求原文："内容包含：怪物生命值，常规/精英/boss 敌怪，意图状态机，描述。"</p>
 *
 * <p>所以这一页按需求列四块：生命值（原版 / 尖塔战斗）、档位、意图状态机（每回合一行）、描述。</p>
 */
public class MobDetailScreen extends Screen implements BattleInfoScreen {

    private static final int PANEL_W = 260;
    private static final int LINE_H = 11;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;

    private final CardBinderMenu menu;
    private final MobCodex.MobEntry mob;
    private final Supplier<Screen> back;
    private final List<String> lines = new ArrayList<>();

    public MobDetailScreen(CardBinderMenu menu, net.minecraft.world.entity.player.Inventory inventory,
                           Component title, MobCodex.MobEntry mob, Supplier<Screen> back) {
        super(Component.translatable("stc.codex.mob.title"));
        this.menu = menu;
        this.mob = mob;
        this.back = back;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public Screen backScreen() {
        return back.get();
    }

    private void buildLines() {
        if (!lines.isEmpty()) {
            return;
        }
        // v0.4.6.3：BOSS 在图鉴里统一归【尖塔护卫】，这里必须用同一套归属，否则两边对不上
        Faction faction = MobCodex.factionOf(mob);
        // v0.4.6.2 需求："不再显示原版生命值" —— 只写尖塔战斗生命值
        lines.add(Component.translatable("stc.codex.mob.tier", mob.tierLabel()).getString());
        lines.add(Component.translatable("stc.codex.mob.hp", mob.battleHp()).getString());
        if (mob.overridden()) {
            lines.add(Component.translatable("stc.codex.mob.hp.note").getString());
        }
        lines.add(Component.translatable("stc.codex.mob.faction",
                faction == null ? Component.translatable("stc.codex.mob.no_faction").getString()
                        : faction.label()).getString());
        lines.add("");
        lines.add(Component.translatable("stc.codex.mob.script").getString());
        lines.addAll(mob.scriptLines());
        lines.add("");
        lines.add(Component.translatable("stc.codex.mob.description").getString());
        lines.addAll(BinderStyle.wrap(font, mob.description(), PANEL_W - 24));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        buildLines();
        int panelH = 44 + lines.size() * LINE_H + 28;
        int left = (width - PANEL_W) / 2;
        int top = Math.max(6, (height - panelH) / 2);
        graphics.fill(left, top, left + PANEL_W, top + panelH, PANEL_BG);
        border(graphics, left, top, left + PANEL_W, top + panelH, PANEL_EDGE);
        TextStyle.draw(graphics, font, mob.name(), left + (PANEL_W - font.width(mob.name())) / 2, top + 10, GOLD);
        graphics.fill(left + 12, top + 26, left + PANEL_W - 12, top + 27, PANEL_EDGE);
        int y = top + 34;
        for (String line : lines) {
            TextStyle.draw(graphics, font, line, left + 12, y, line.isEmpty() ? TEXT_DIM : TEXT);
            y += LINE_H;
        }
        Component backHint = Component.translatable("stc.battle.deck.back");
        TextStyle.draw(graphics, font, backHint, left + (PANEL_W - font.width(backHint)) / 2, top + panelH - 16,
                TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        onClose();
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(backScreen());
        }
        super.onClose();
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
