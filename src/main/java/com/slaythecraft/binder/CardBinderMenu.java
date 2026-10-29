package com.slaythecraft.binder;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * 卡牌列表菜单（设计文档 §8.1 / §6.3）。
 *
 * <p>**同一个类被两个入口复用**：{@link Mode#BINDER}（收纳包物品 / 快捷键）与
 * {@link Mode#RECYCLER}（卡牌回收站方块）。两者共用同一套删卡逻辑与价格
 * （{@link CardDeleteService}）。</p>
 *
 * <p><b>测试反馈修正</b>：战斗奖励改成"战斗结束后立刻弹出三选一"，
 * 这里原来那个"打开奖励界面"的按钮已经移除（{@code BUTTON_CLAIM_REWARD} 不再存在）。</p>
 *
 * <p>按钮 id 语义：{@code 0 <= id < 卡牌数} = 删除该下标。</p>
 */
public class CardBinderMenu extends AbstractContainerMenu {

    public enum Mode {
        /** 收纳包：可删卡 */
        BINDER,
        /** 回收站：只能删卡（花钱） */
        RECYCLER
    }

    private final Player owner;
    private final Mode mode;

    public static CardBinderMenu binderMenu(int containerId, Inventory inventory) {
        return new CardBinderMenu(StcMenus.CARD_BINDER.get(), containerId, inventory, Mode.BINDER);
    }

    public static CardBinderMenu recyclerMenu(int containerId, Inventory inventory) {
        return new CardBinderMenu(StcMenus.RECYCLER.get(), containerId, inventory, Mode.RECYCLER);
    }

    /** 供 {@code IMenuTypeExtension} 使用的工厂（客户端不需要额外数据）。 */
    public static CardBinderMenu binder(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        return binderMenu(containerId, inventory);
    }

    public static CardBinderMenu recycler(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        return recyclerMenu(containerId, inventory);
    }

    private CardBinderMenu(MenuType<?> type, int containerId, Inventory inventory, Mode mode) {
        super(type, containerId);
        this.owner = inventory.player;
        this.mode = mode;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    /** 本菜单没有物品槽，Shift-点击没有可搬运的东西。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        PlayerCardData data = StcAttachments.dataOf(serverPlayer);
        int cost = CardDeleteService.cost(data);
        CardDeleteService.Result result = CardDeleteService.deleteAt(data, id);
        switch (result) {
            case OK -> {
                StcAttachments.syncTo(serverPlayer);
                // v0.5.0.10 成就【再玩小卡组找人弄你】：需求"第一次删牌"
                com.slaythecraft.achievement.AchievementService.grantCardDelete(serverPlayer);
                String where = mode == Mode.RECYCLER ? "回收站" : "收纳包";
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] " + where + "已删除 1 张卡牌，花费 " + cost + " 金币（剩余 " + data.gold()
                                        + "，卡牌 " + data.cardCount() + " 张）。")
                        .withStyle(ChatFormatting.GRAY));
                return true;
            }
            case NOT_ENOUGH_GOLD -> {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 金币不足：删卡需要 " + cost + " 金币，当前 " + data.gold() + "。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            case NO_SUCH_CARD -> {
                StcAttachments.syncTo(serverPlayer);
                StcChat.tell(serverPlayer, Component.literal("[STC] 该卡牌已不存在（界面已刷新）。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
        }
        return false;
    }
}
