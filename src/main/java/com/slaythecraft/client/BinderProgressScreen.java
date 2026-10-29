package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.binder.CardBinderMenu;
import com.slaythecraft.combat.core.EventCaps;
import com.slaythecraft.combat.core.EventKind;
import com.slaythecraft.combat.core.ProgressState;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 「事件回合统计」子页面（v0.2.6 独立成新页面）。 */
public class BinderProgressScreen extends AbstractContainerScreen<CardBinderMenu> {

    public BinderProgressScreen(CardBinderMenu menu, Inventory inventory, Component title) {
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
        PlayerCardData data = BinderStyle.data();
        if (data == null) {
            return;
        }
        EventCaps caps = Config.eventCaps();
        ProgressState progress = data.progress();
        int x = leftPos + BinderStyle.GRID_X;
        int y = topPos + 58;

        TextStyle.draw(graphics, font, Component.translatable("stc.binder.progress.floor", progress.floor(),
                progress.boss() == null ? "?" : progress.boss().name()), x, y, BinderStyle.ACCENT);
        TextStyle.draw(graphics, font, Component.translatable("stc.binder.progress.hp", data.life().describe(),
                Config.maxPlayerHp()), x, y + 14, BinderStyle.INK);

        int lineY = y + 34;
        for (EventKind kind : ProgressState.CAPPED_KINDS) {
            int count = progress.countOf(kind);
            boolean capped = !progress.canTrigger(kind, caps);
            String text = kind.label() + " " + count + "/" + caps.capOf(kind)
                    + (capped ? Component.translatable("stc.binder.progress.capped").getString() : "");
            TextStyle.draw(graphics, font, text, x, lineY, capped ? BinderStyle.INK_DIM : BinderStyle.INK);
            lineY += 12;
        }

        // v0.4.6.3 需求："回合统计模块里，已经击败的BOSS文案超出边框。解决建议：不显示此内容。"
        // 所以"已击败 BOSS：…"整行不再绘制（BOSS 名字一多就会顶出书页边框）；通关提示保留。
        if (progress.completed()) {
            TextStyle.draw(graphics, font, Component.translatable("stc.binder.progress.done"), x, lineY + 4,
                    BinderStyle.ACCENT);
        }
        // v0.5.0.20 需求："移除详情页下方的统计数据（击杀，胜败，删牌）。"
        // 所以这里不再绘制 stc.binder.progress.stats 那一行（统计数据仍留在档案里，
        // 只是不再展示；需要时把下面三行恢复即可）。
        //
        // Component stats = Component.translatable("stc.binder.progress.stats", data.stats().kills(),
        //         data.stats().battlesWon(), data.stats().battlesLost(), data.stats().cardsDeleted());
        // TextStyle.draw(graphics, font, stats, x, topPos + BinderStyle.footerY(imageHeight) - 16,
        //         BinderStyle.INK_DIM);

        int footerY = topPos + BinderStyle.footerY(imageHeight);
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X, footerY, 52, 14,
                Component.translatable("stc.binder.back").getString(), true);
        // v0.4.1：每个事件的"触发条件"单独一页（沿用"子页面一律新开页面"的约定）
        BinderStyle.button(graphics, font, leftPos + BinderStyle.GRID_X + 58, footerY, 96, 14,
                Component.translatable("stc.binder.progress.trigger.button").getString(), true);
    }

    /**
     * 触发方式页的正文（需求给的四条原文 + v0.5.5.1 的藏宝箱）。
     *
     * <p>v0.5.1.2 美术需求："事件触发方式页 4 个（常规=铁剑、精英=钻石剑、问号=minecraft:spyglass、
     * 篝火=minecraft:campfire）" —— 每条左边挂上对应的小图标。</p>
     *
     * <p>v0.5.5.1（用户）："事件回合统计没有添加有关藏宝箱的内容。（包括目前完成进度，上限 5 次，
     * 以及事件触发条件介绍）" —— 所以这里补第 5 条（图标用陷阱箱）。</p>
     */
    private List<InfoPageScreen.BodyLine> triggerLines() {
        List<InfoPageScreen.BodyLine> body = new ArrayList<>();
        List<net.minecraft.world.item.Item> icons = List.of(net.minecraft.world.item.Items.IRON_SWORD,
                net.minecraft.world.item.Items.DIAMOND_SWORD, net.minecraft.world.item.Items.SPYGLASS,
                net.minecraft.world.item.Items.CAMPFIRE, net.minecraft.world.item.Items.TRAPPED_CHEST);
        List<String> keys = List.of("stc.binder.progress.trigger.normal", "stc.binder.progress.trigger.elite",
                "stc.binder.progress.trigger.question", "stc.binder.progress.trigger.campfire",
                "stc.binder.progress.trigger.treasure");
        for (int i = 0; i < keys.size(); i++) {
            body.add(new InfoPageScreen.BodyLine(Component.translatable(keys.get(i)).getString(),
                    BinderStyle.INK, icons.get(i)));
        }
        return body;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (minecraft == null || minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (relX >= BinderStyle.GRID_X && relX < BinderStyle.GRID_X + 52
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(new CardBinderScreen(menu, minecraft.player.getInventory(), title));
            return true;
        }
        if (relX >= BinderStyle.GRID_X + 58 && relX < BinderStyle.GRID_X + 58 + 96
                && relY >= BinderStyle.footerY(imageHeight) && relY < BinderStyle.footerY(imageHeight) + 14) {
            minecraft.setScreen(InfoPageScreen.rich(menu, minecraft.player.getInventory(), title,
                    Component.translatable("stc.binder.progress.trigger.title"), triggerLines(), null, null,
                    () -> this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
