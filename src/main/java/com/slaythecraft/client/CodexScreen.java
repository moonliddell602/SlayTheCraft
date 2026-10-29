package com.slaythecraft.client;

import com.slaythecraft.binder.CardBinderMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 「查看图鉴」主页（v0.4.0）。
 *
 * <p>需求原文："给卡牌收纳包添加一个：'查看图鉴'的功能，允许玩家预览模组内已经可以使用的所有遗物和卡牌，
 * 其中，卡牌图鉴需要按照角色分开放出（选择卡牌图鉴→选择要查看的角色→展示当前角色所可以使用的卡牌→
 * 点击查看详情）。"</p>
 *
 * <p>所以这一层只有两个入口：遗物图鉴 / 卡牌图鉴 —— 每一步都是**新页面**（沿用 v0.2.6 的约定）。</p>
 */
public class CodexScreen extends AbstractContainerScreen<CardBinderMenu> {

    private static final int MODULE_X = 16;
    private static final int MODULE_W = 200;
    private static final int MODULE_GAP = 8;
    private static final int MODULE_Y = 74;

    /**
     * 九个模块：遗物 / 角色卡牌 / 无色牌 / 状态与诅咒牌 / 敌人阵营 / 状态图鉴 / 药水图鉴 / 问号事件 /
     * **多人模式卡牌**。
     *
     * <p>v0.4.6.7 需求："新图鉴：状态图鉴" / "新图鉴：药水图鉴"，所以模块数 5 → 7。</p>
     * <p>v0.5.1.2 需求："图鉴：新增问号事件图鉴（查看问号事件与事件奖励）"，所以 7 → 8。</p>
     * <p>v0.5.1.4 需求："为卡牌图鉴添加：多人模式卡牌图鉴。"，所以 8 → 9
     * （模块高度公式早就改成"先扣间隙再平分"，再加模块也不会顶到页脚）。</p>
     */
    private static final int MODULES = 9;

    /**
     * 模块高度按可用空间自适应。
     *
     * <p>⚠️ v0.5.1.2 修正：加入第 8 个模块后，原来的 {@code available / MODULES - GAP} 再夹一个
     * 下限 20 就不成立了 —— 8×20 + 7×8 = 216 已经超过可用的 194，最后一个模块会顶到页脚上。
     * 现在改成**先把间隙扣掉再平分**（{@code (available - 间隙总和) / MODULES}），
     * 所以"模块总高 + 间隙总和 ≤ 可用高度"是恒等式，加多少模块都不会溢出。</p>
     */
    private int moduleHeight() {
        int available = BinderStyle.footerY(imageHeight) - MODULE_Y - 6;
        int per = (available - (MODULES - 1) * MODULE_GAP) / MODULES;
        return Math.max(14, Math.min(34, per));
    }

    public CodexScreen(CardBinderMenu menu, Inventory inventory, Component title) {
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
        Component heading = Component.translatable("stc.codex.heading");
        TextStyle.draw(graphics, font, heading, leftPos + MODULE_X, topPos + 40, BinderStyle.INK);
        Component hint = Component.translatable("stc.codex.hint");
        TextStyle.draw(graphics, font, hint, leftPos + MODULE_X, topPos + 52, BinderStyle.INK_DIM);

        int y = topPos + MODULE_Y;
        int step = moduleHeight() + MODULE_GAP;
        module(graphics, mouseX, mouseY, y, Component.translatable("stc.codex.relics").getString(),
                Component.translatable("stc.codex.relics.hint").getString());
        module(graphics, mouseX, mouseY, y + step,
                Component.translatable("stc.codex.cards").getString(),
                Component.translatable("stc.codex.cards.hint").getString());
        module(graphics, mouseX, mouseY, y + step * 2,
                Component.translatable("stc.codex.cards.colorless").getString(),
                Component.translatable("stc.codex.cards.colorless.hint").getString());
        // v0.4.6.2：状态与诅咒牌图鉴
        module(graphics, mouseX, mouseY, y + step * 3,
                Component.translatable("stc.codex.cards.status").getString(),
                Component.translatable("stc.codex.cards.status.hint").getString());
        module(graphics, mouseX, mouseY, y + step * 4,
                Component.translatable("stc.codex.faction").getString(),
                Component.translatable("stc.codex.faction.hint").getString());
        // v0.4.6.7：状态图鉴 / 药水图鉴（同样都是"点进去是新页面"）
        module(graphics, mouseX, mouseY, y + step * 5,
                Component.translatable("stc.codex.status").getString(),
                Component.translatable("stc.codex.status.hint").getString());
        module(graphics, mouseX, mouseY, y + step * 6,
                Component.translatable("stc.codex.potion").getString(),
                Component.translatable("stc.codex.potion.hint").getString());
        // v0.5.1.2：问号事件图鉴（看怎么触发、能拿到什么奖励）
        module(graphics, mouseX, mouseY, y + step * 7,
                Component.translatable("stc.codex.question").getString(),
                Component.translatable("stc.codex.question.hint").getString());
        // v0.5.1.4：多人模式卡牌图鉴（这些牌只在多人模式的奖励池里出现）
        module(graphics, mouseX, mouseY, y + step * 8,
                Component.translatable("stc.codex.cards.multiplayer").getString(),
                Component.translatable("stc.codex.cards.multiplayer.hint").getString());

        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, topPos + BinderStyle.footerY(imageHeight), 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
    }

    private void module(GuiGraphics graphics, int mouseX, int mouseY, int y, String label, String hint) {
        int x = leftPos + MODULE_X;
        int h = moduleHeight();
        boolean hovered = mouseX >= x && mouseX < x + MODULE_W && mouseY >= y && mouseY < y + h;
        graphics.fill(x, y, x + MODULE_W, y + h, hovered ? 0xFFFFF3D6 : 0xFFF2E6C4);
        BinderStyle.border(graphics, x, y, x + MODULE_W, y + h,
                hovered ? BinderStyle.CELL_SELECTED : BinderStyle.BOOK_EDGE);
        TextStyle.draw(graphics, font, label, x + 8, y + 4, BinderStyle.INK);
        // 高度被压扁时省略副标题，保证模块名永远看得见
        if (h >= 28) {
            String shown = font.plainSubstrByWidth(hint, MODULE_W - 16);
            TextStyle.draw(graphics, font, shown, x + 8, y + 17, BinderStyle.INK_DIM);
        }
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
            minecraft.setScreen(new CardBinderScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        int step = moduleHeight() + MODULE_GAP;
        for (int i = 0; i < MODULES; i++) {
            int y = MODULE_Y + i * step;
            if (relX >= MODULE_X && relX < MODULE_X + MODULE_W && relY >= y && relY < y + moduleHeight()) {
                switch (i) {
                    case 0 -> minecraft.setScreen(new RelicCodexScreen(menu, minecraft.player.getInventory(),
                            title));
                    case 1 -> minecraft.setScreen(new CardCodexCharactersScreen(menu,
                            minecraft.player.getInventory(), title));
                    case 2 -> minecraft.setScreen(new CardCodexScreen(menu, minecraft.player.getInventory(),
                            title, 0, true));
                    // v0.4.6.2：状态与诅咒牌图鉴（与无色牌图鉴共用网格）
                    case 3 -> minecraft.setScreen(CardCodexScreen.statusAndCurse(menu,
                            minecraft.player.getInventory(), title));
                    case 4 -> minecraft.setScreen(new FactionCodexScreen(menu,
                            minecraft.player.getInventory(), title));
                    // v0.4.6.7：状态图鉴 / 药水图鉴
                    case 5 -> minecraft.setScreen(new StatusCodexScreen(menu,
                            minecraft.player.getInventory(), title));
                    // v0.5.1.2：问号事件图鉴
                    case 6 -> minecraft.setScreen(new PotionCodexScreen(menu,
                            minecraft.player.getInventory(), title));
                    case 7 -> minecraft.setScreen(new QuestionCodexScreen(menu,
                            minecraft.player.getInventory(), title));
                    // v0.5.1.4：多人模式卡牌图鉴
                    default -> minecraft.setScreen(CardCodexScreen.multiplayer(menu,
                            minecraft.player.getInventory(), title));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
