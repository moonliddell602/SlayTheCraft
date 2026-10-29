package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 药水槽位 HUD（v0.0.2 V8；测试反馈修正：**只在战斗内显示**）。
 *
 * <p>反馈原文："常态情况下仍然显示药水栏会很挡视野。应该只在战斗内显示。"
 * 所以这里先判断"是否在战斗中"，战斗外一律不画；战斗内如果开着别的界面（含出牌界面，
 * 它自己会画一排可点击的槽位）这一层也让位，避免两排槽位叠在一起。</p>
 *
 * <p>按 ESC 收起出牌界面看世界时，这一层正好顶上——战斗中仍然能看到自己的药水。</p>
 *
 * <p>v0.4.6.7 追加：手牌达到上限时，在这排药水槽位**正上方**提示"我的手牌太多了..."（需求原文：
 * "当手牌达到上限时，在药水栏上方提示『我的手牌太多了...』"）。手牌上限由服务端战斗规则决定，
 * 客户端从战斗快照的 {@code BattleView.handFull()} 读。</p>
 */
public final class PotionHudLayer implements LayeredDraw.Layer {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "potion_slots");

    private static final int SLOT = 20;
    private static final int GAP = 3;
    private static final int BG = 0x90000000;
    private static final int EDGE = 0xFF5A5A64;
    private static final int GOOD = 0xFF5AD05A;
    private static final int BAD = 0xFFD05A5A;
    /** 手牌满提示的颜色（v0.4.6.7：暖黄，和药水栏边框区分开）。 */
    private static final int HINT = 0xFFFFD24A;
    /** 手牌满提示与药水栏顶边的距离（提示画在药水栏**上方**）。 */
    private static final int HINT_GAP = 12;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        // 只有战斗内才显示（测试反馈）
        if (!ClientBattleState.view().active()) {
            return;
        }
        var playerData = StcAttachments.dataOf(player);
        List<String> potions = playerData.potions();
        // v0.4.6：【药水腰带】会让栏位 +2
        int slots = Math.max(1, playerData.potionSlots(Config.potionSlots()));
        int total = slots * SLOT + (slots - 1) * GAP;
        int startX = (graphics.guiWidth() - total) / 2;
        int y = graphics.guiHeight() - 46;

        for (int i = 0; i < slots; i++) {
            int x = startX + i * (SLOT + GAP);
            graphics.fill(x, y, x + SLOT, y + SLOT, BG);
            border(graphics, x, y, x + SLOT, y + SLOT, EDGE);
            if (i >= potions.size()) {
                continue;
            }
            Potion potion = PotionLibrary.byId(potions.get(i));
            if (potion == null) {
                continue;
            }
            border(graphics, x + 1, y + 1, x + SLOT - 1, y + SLOT - 1,
                    potion.kind().canTargetEnemy() ? BAD : GOOD);
            // v0.4.6：药水画成原版药水贴图（按目标类型选常规 / 喷溅 / 滞留），不再用文字代替
            PotionIcons.draw(graphics, potion, x + 2, y + 2, 1.0F);
        }

        // v0.4.6.7：手牌满提示 —— 需求原文："当手牌达到上限时，在药水栏上方提示『我的手牌太多了...』"
        // 手牌上限是服务端战斗规则，客户端从快照（BattleView.handFull）读，不自己猜
        handFullHint(graphics, minecraft, y);

        // 悬停提示：说明这瓶药水是什么、战斗中怎么用
        int hovered = hovered(minecraft, graphics, potions, slots, startX, y);
        if (hovered >= 0) {
            Potion potion = PotionLibrary.byId(potions.get(hovered));
            if (potion != null) {
                Component usage = potion.kind().canTargetEnemy()
                        ? Component.translatable("stc.potion.usage.enemy")
                        : (potion.onlySelf()
                                ? Component.translatable("stc.potion.usage.self_only")
                                : Component.translatable("stc.potion.usage.self"));
                graphics.renderTooltip(minecraft.font,
                        List.of(Component.literal(potion.name()), Component.literal(potion.description()), usage),
                        java.util.Optional.empty(), (int) minecraft.mouseHandler.xpos() * graphics.guiWidth()
                                / minecraft.getWindow().getWidth(),
                        (int) minecraft.mouseHandler.ypos() * graphics.guiHeight()
                                / minecraft.getWindow().getHeight());
            }
        }
    }

    /**
     * 手牌满提示（v0.4.6.7）。
     *
     * <p>需求原文："当手牌达到上限时，在药水栏上方提示『我的手牌太多了...』"。
     * 本层本来就只有战斗内会画，所以这里只要判断快照里的 {@code handFull}；
     * 文字居中在药水栏正上方（{@link #HINT_GAP} 像素）。</p>
     *
     * @param barY 药水栏的顶边 y
     */
    private void handFullHint(GuiGraphics graphics, Minecraft minecraft, int barY) {
        if (!ClientBattleState.view().handFull()) {
            return;
        }
        Component hint = Component.translatable("stc.battle.hand_full");
        int width = minecraft.font.width(hint);
        TextStyle.draw(graphics, minecraft.font, hint, (graphics.guiWidth() - width) / 2, barY - HINT_GAP, HINT);
    }

    private int hovered(Minecraft minecraft, GuiGraphics graphics, List<String> potions, int slots, int startX, int y) {
        int mouseX = (int) minecraft.mouseHandler.xpos() * graphics.guiWidth() / minecraft.getWindow().getWidth();
        int mouseY = (int) minecraft.mouseHandler.ypos() * graphics.guiHeight() / minecraft.getWindow().getHeight();
        for (int i = 0; i < Math.min(slots, potions.size()); i++) {
            int x = startX + i * (SLOT + GAP);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                return i;
            }
        }
        return -1;
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
