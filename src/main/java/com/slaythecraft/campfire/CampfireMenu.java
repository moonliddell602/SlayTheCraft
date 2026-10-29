package com.slaythecraft.campfire;

import com.slaythecraft.chat.StcChat;
import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.team.TeamResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 篝火准备界面（v0.0.3 W5）。
 *
 * <p>按钮 id：</p>
 * <ul>
 *   <li>{@link #BUTTON_HEAL} 愈合；</li>
 *   <li>{@link #BUTTON_FORGE} 锻造（客户端接着打开"选一张卡"页面）；</li>
 *   <li>{@link #BUTTON_BANDAGE} 包扎（客户端接着打开"选队友"页面）；</li>
 *   <li>{@link #UPGRADE_BASE + index} 锻造选中的那张卡；</li>
 *   <li>{@link #ALLY_BASE + index} 包扎选中的那名队友。</li>
 * </ul>
 *
 * <p>只有所有同队玩家都进入准备界面（客户端收到 ready=true）之后，客户端才允许点前三个按钮；
 * 服务端同样会检查（防作弊）。</p>
 */
public class CampfireMenu extends AbstractContainerMenu {

    public static final int BUTTON_HEAL = 0;
    public static final int BUTTON_FORGE = 1;
    public static final int BUTTON_BANDAGE = 2;
    public static final int UPGRADE_BASE = 100;
    public static final int ALLY_BASE = 200;

    private final Player owner;
    private final BlockPos pos;

    public CampfireMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(StcMenus.CAMPFIRE.get(), containerId);
        this.owner = inventory.player;
        this.pos = pos;
    }

    public CampfireMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        // v0.5.0：缓冲区可能是空的（菜单不再携带位置），所以**什么都不读** —— 位置只在服务端有用
        this(containerId, inventory, BlockPos.ZERO);
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 附近的同队队友（包扎用）。 */
    public static List<ServerPlayer> allies(ServerPlayer player) {
        List<ServerPlayer> allies = new ArrayList<>();
        double radiusSqr = (double) CampfireService.PARTY_RADIUS * CampfireService.PARTY_RADIUS;
        for (ServerPlayer mate : TeamResolver.onlineTeammates(player)) {
            if (mate.getUUID().equals(player.getUUID())) {
                continue;
            }
            if (mate.level() == player.level() && mate.distanceToSqr(player) <= radiusSqr) {
                allies.add(mate);
            }
        }
        return allies;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        // v0.0.3 W5：多人时必须队友都进入过准备界面才能真的动手（客户端也会置灰）
        boolean isAction = id == BUTTON_HEAL || id >= UPGRADE_BASE;
        if (isAction && !CampfireService.ready(serverPlayer)) {
            StcChat.tell(serverPlayer, Component.literal(
                    "[STC] 还有队友没进入篝火准备界面，等他们都进来之后再选。"));
            CampfireService.pushState(serverPlayer);
            return false;
        }
        if (id == BUTTON_HEAL) {
            StcChat.tell(serverPlayer, Component.literal("[STC] " + CampfireService.heal(serverPlayer)));
            return true;
        }
        if (id == BUTTON_FORGE || id == BUTTON_BANDAGE) {
            // 真正的动作在客户端打开的"选卡 / 选队友"页面里完成
            return true;
        }
        if (id >= ALLY_BASE) {
            List<ServerPlayer> allies = allies(serverPlayer);
            int index = id - ALLY_BASE;
            if (index < 0 || index >= allies.size()) {
                StcChat.tell(serverPlayer, Component.literal("[STC] 找不到这位队友。"));
                return false;
            }
            StcChat.tell(serverPlayer, Component.literal("[STC] "
                    + CampfireService.healAlly(serverPlayer, allies.get(index))));
            return true;
        }
        if (id >= UPGRADE_BASE) {
            StcChat.tell(serverPlayer, Component.literal("[STC] "
                    + CampfireService.forge(serverPlayer, id - UPGRADE_BASE)));
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            CampfireService.onClosed(serverPlayer);
        }
    }

    /** 客户端用来判断"能不能开始选"（服务端会再检查一次）。 */
    public static boolean canChoose(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        return data.cardCount() > 0 || !data.life().isFull() || !allies(player).isEmpty();
    }
}
