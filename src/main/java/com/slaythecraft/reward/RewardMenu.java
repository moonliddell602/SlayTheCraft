package com.slaythecraft.reward;

import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.chat.StcChat;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 奖励菜单 —— 一个菜单承载**两个页面**（v0.5.0.20）：
 *
 * <ol>
 *   <li>{@link Mode#CHOICE} 卡牌三选一（原 {@code RewardScreen} 的行为）；</li>
 *   <li>{@link Mode#RESULT} **战斗结算页**：领取金币 / 领取遗物 / 获得一次卡牌奖励 / 跳过奖励。</li>
 * </ol>
 *
 * <p>需求原文："新UI:战斗结算页面：领取X金币（x为在战斗中获得的金币）/
 * 领取遗物：X（X为遗物名称，如果有则显示此选项，没有掉落则不显示）/
 * 获得一次卡牌奖励（打开卡牌领取页面）/ 跳过奖励按钮"，
 * 并特别说明："如果点开了卡牌领取页面，但战斗结算页面仍然有未领取的内容，则回到战斗结算页面。"</p>
 *
 * <h2>为什么用"服务端换菜单"而不是客户端切屏</h2>
 *
 * <p>状态（待领金币 / 待领遗物 / 待领卡牌次数）**全在服务端档案里**，客户端只是渲染。
 * 所以"从结算页进卡牌页、再回结算页"由服务端决定：换菜单 = 换页面。
 * 客户端 {@code RewardScreen} 在 {@code init()} 里按服务端给的菜单类型挑要画哪种页面，
 * 于是两个方向都会自动回到正确的页面，不需要额外的网络包。</p>
 */
public class RewardMenu extends AbstractContainerMenu {

    /** 跳过（三选一页面用）。 */
    public static final int BUTTON_SKIP = 50;

    // ---- 战斗结算页的按钮编号 ----
    /** 领取金币。 */
    public static final int BUTTON_CLAIM_GOLD = 60;
    /** 领取遗物。 */
    public static final int BUTTON_CLAIM_RELIC = 61;
    /** 获得一次卡牌奖励（进三选一）。 */
    public static final int BUTTON_TAKE_CARD = 62;
    /** 跳过奖励（清空结算）。 */
    public static final int BUTTON_SKIP_ALL = 63;

    /** 这个菜单是哪一页。 */
    public enum Mode {
        /** 卡牌三选一。 */
        CHOICE,
        /** 战斗结算。 */
        RESULT
    }

    private final Player owner;
    private final Mode mode;

    public RewardMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, Mode.CHOICE);
    }

    public RewardMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, Mode.CHOICE);
    }

    public RewardMenu(int containerId, Inventory inventory, Mode mode) {
        this(containerId, inventory, inventory.player, mode);
    }

    private RewardMenu(int containerId, Inventory inventory, Player owner, Mode mode) {
        super(mode == Mode.RESULT ? StcMenus.BATTLE_RESULT.get() : StcMenus.REWARD.get(), containerId);
        this.owner = owner;
        this.mode = mode;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (mode == Mode.RESULT) {
            return clickResult(serverPlayer, id);
        }
        String message = id == BUTTON_SKIP
                ? RewardService.skip(serverPlayer)
                : RewardService.claim(serverPlayer, id);
        StcChat.tell(serverPlayer, Component.literal("[STC] " + message));
        // 领完/跳完：结算页还有没领的东西就回去，否则直接关掉
        if (RewardService.hasCardReward(serverPlayer)) {
            // 还有下一次卡牌奖励：保持在三选一
            return true;
        }
        if (StcAttachments.dataOf(serverPlayer).settlementOpen()) {
            RewardService.openResultMenu(serverPlayer);
        } else {
            serverPlayer.closeContainer();
        }
        return true;
    }

    /** 战斗结算页的按钮。 */
    private boolean clickResult(ServerPlayer player, int id) {
        PlayerCardData data = StcAttachments.dataOf(player);
        switch (id) {
            case BUTTON_CLAIM_GOLD -> {
                if (data.claimSettlementGold()) {
                    StcChat.tell(player, Component.literal("[STC] 已领取 " + data.gold() + " 金币。"));
                } else {
                    StcChat.tell(player, Component.literal("[STC] 没有待领取的金币。"));
                }
                StcAttachments.syncTo(player);
                return true;
            }
            case BUTTON_CLAIM_RELIC -> {
                String relicId = data.pendingRelic();
                if (data.claimSettlementRelic()) {
                    StcChat.tell(player, Component.literal("[STC] 已领取遗物【"
                            + RewardService.relicDisplayName(relicId) + "】。"));
                } else {
                    StcChat.tell(player, Component.literal("[STC] 本场没有掉落遗物。"));
                }
                StcAttachments.syncTo(player);
                return true;
            }
            case BUTTON_TAKE_CARD -> {
                // 需求："获得一次卡牌奖励（打开卡牌领取页面）"
                RewardService.openChoiceMenu(player);
                return true;
            }
            case BUTTON_SKIP_ALL -> {
                data.clearSettlement();
                // 跳过 = 连卡牌奖励一起放弃
                while (data.claimOffer(0)) {
                    // 把待领的卡牌奖励也清掉
                }
                StcAttachments.syncTo(player);
                StcChat.tell(player, Component.literal("[STC] 已跳过本次战斗的全部奖励。"));
                player.closeContainer();
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
