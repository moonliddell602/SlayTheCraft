package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.network.UsePotionPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 药水的"选择目标"页面（v0.4.6 独立新页面）。
 *
 * <p>需求：药水"对自己 / 队友使用"。多人战斗时点一瓶正面药水先开这一页，列出全部参战玩家
 * （含自己，倒下的用灰色显示且点不动）；单人战斗直接生效，不会弹这一页。</p>
 *
 * <p>沿用本模组的页面约定：**永远新开一页**，页脚是「返回」。</p>
 */
public class PotionTargetScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int ROW_H = 24;
    private static final int ROW_GAP = 4;
    private static final int PANEL_BG = 0xF01C1C22;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int GOOD = 0xFF5AD05A;
    private static final int BAD = 0xFFD05A5A;

    private final Screen back;
    private final int slot;
    private final Potion potion;
    private final List<BattleViewAlias> party;

    /** 仅为了少写一层泛型：页面只读这几个字段。 */
    public record BattleViewAlias(String name, int hp, int maxHp, int block, boolean alive, boolean self) {
        static BattleViewAlias of(com.slaythecraft.network.BattleView.PartyMember member) {
            return new BattleViewAlias(member.name(), member.hp(), member.maxHp(), member.block(), member.alive(),
                    member.self());
        }
    }

    public PotionTargetScreen(Screen back, int slot, Potion potion,
                              List<com.slaythecraft.network.BattleView.PartyMember> party) {
        super(Component.translatable("stc.potion.target.title"));
        this.back = back;
        this.slot = slot;
        this.potion = potion;
        this.party = party.stream().map(BattleViewAlias::of).toList();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private int panelTop() {
        int panelH = panelHeight();
        return (height - panelH) / 2;
    }

    private int panelHeight() {
        return 44 + party.size() * (ROW_H + ROW_GAP) + 26;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_W) / 2;
        int top = panelTop();
        int panelH = panelHeight();
        graphics.fill(left, top, left + PANEL_W, top + panelH, PANEL_BG);
        border(graphics, left, top, left + PANEL_W, top + panelH, PANEL_EDGE);

        Component title = Component.translatable("stc.potion.target.heading",
                potion == null ? "?" : potion.name());
        TextStyle.draw(graphics, font, title, left + (PANEL_W - font.width(title)) / 2, top + 8, GOLD);
        Component hint = Component.translatable("stc.potion.target.hint");
        TextStyle.draw(graphics, font, hint, left + (PANEL_W - font.width(hint)) / 2, top + 22, TEXT_DIM);

        for (int i = 0; i < party.size(); i++) {
            BattleViewAlias member = party.get(i);
            int x = left + 12;
            int y = top + 40 + i * (ROW_H + ROW_GAP);
            boolean hovered = isInside(mouseX, mouseY, x, y, PANEL_W - 24, ROW_H);
            graphics.fill(x, y, x + PANEL_W - 24, y + ROW_H, hovered ? 0xFF3A3420 : 0xFF26262E);
            border(graphics, x, y, x + PANEL_W - 24, y + ROW_H,
                    hovered && member.alive() ? GOLD : PANEL_EDGE);

            String label = member.self()
                    ? Component.translatable("stc.potion.target.me").getString()
                    : member.name();
            TextStyle.draw(graphics, font, label, x + 8, y + 4, member.alive() ? TEXT : TEXT_DIM);
            Component hp = Component.literal(member.hp() + "/" + member.maxHp());
            TextStyle.draw(graphics, font, hp, x + 8, y + 14, member.alive() ? GOOD : BAD);
            if (member.block() > 0) {
                Component block = Component.translatable("stc.battle.block", member.block());
                TextStyle.draw(graphics, font, block, x + 72, y + 14, TEXT_DIM);
            }
            if (!member.alive()) {
                Component down = Component.translatable("stc.battle.down");
                TextStyle.draw(graphics, font, down, x + PANEL_W - 24 - 8 - font.width(down), y + 8, TEXT_DIM);
            }
        }

        Component backHint = Component.translatable("stc.battle.deck.back");
        TextStyle.draw(graphics, font, backHint, left + (PANEL_W - font.width(backHint)) / 2, top + panelH - 16,
                TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANEL_W) / 2;
        int top = panelTop();
        for (int i = 0; i < party.size(); i++) {
            BattleViewAlias member = party.get(i);
            int x = left + 12;
            int y = top + 40 + i * (ROW_H + ROW_GAP);
            if (isInside(mouseX, mouseY, x, y, PANEL_W - 24, ROW_H)) {
                if (!member.alive()) {
                    return true;
                }
                PacketDistributor.sendToServer(
                        new UsePotionPayload(slot, UsePotionPayload.ALLY_BASE + i));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(back);
        }
        super.onClose();
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
