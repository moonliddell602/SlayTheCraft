package com.slaythecraft.gate;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.binder.BinderOpener;
import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.character.CharacterProfile;
import com.slaythecraft.character.CharacterRegistry;
import com.slaythecraft.character.CharacterSelection;
import com.slaythecraft.character.RunService;
import com.slaythecraft.combat.mc.CombatManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 尖塔入口菜单（设计文档 §6.2 / §8）：选角色、看队伍准备（S6）、开始对战。
 *
 * <p>按钮约定：{@code 0..2} = 选择第 N 个角色，{@link #BUTTON_START_BATTLE} = 开始对战，
 * {@link #BUTTON_OPEN_BINDER} = 打开卡牌收纳包界面。</p>
 */
public class SpireGateMenu extends AbstractContainerMenu {

    public static final int BUTTON_START_BATTLE = 100;
    public static final int BUTTON_OPEN_BINDER = 101;
    /** V9：跳过剩余内容，直接挑战当前层的 BOSS。 */
    public static final int BUTTON_CHALLENGE_BOSS = 102;
    /** 测试反馈修正：重开本局（客户端有二次确认）。 */
    public static final int BUTTON_RESTART_RUN = 103;
    /**
     * v0.5.0.9：查看**本层可挑战名单**（按阵营分组的敌怪与 BOSS）。
     *
     * <p>需求："显示：挑战第X层boss的功能按钮（把boss拉过来进行战斗）
     * 功能修改为：详情查看当前层数可以触发战斗的敌怪和boss（以阵营筛选……）。"</p>
     *
     * <p>这个按钮只在客户端开新界面，所以菜单侧不需要单独处理（会落到默认分支返回 false）。</p>
     */
    public static final int BUTTON_VIEW_ROSTER = 104;

    private final Player owner;

    public SpireGateMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, inventory.player);
    }

    public SpireGateMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory);
    }

    private SpireGateMenu(int containerId, Inventory inventory, Player owner) {
        super(StcMenus.SPIRE_GATE.get(), containerId);
        this.owner = owner;
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
        if (id >= 0 && id < CharacterRegistry.all().size()) {
            CharacterProfile profile = CharacterRegistry.byIndex(id);
            if (profile == null) {
                return false;
            }
            // 设计文档 §7：战斗中禁止切换角色
            if (CombatManager.inCombat(serverPlayer)) {
                StcChat.tell(serverPlayer, Component.literal("[STC] 战斗中无法切换角色（先结束或放弃战斗）。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            String message = CharacterSelection.select(serverPlayer, profile);
            StcChat.tell(serverPlayer, Component.literal("[STC] " + message));
            return true;
        }
        if (id == BUTTON_START_BATTLE) {
            String failure = CombatManager.start(serverPlayer, -1);
            if (failure != null) {
                StcChat.tell(serverPlayer, Component.literal("[STC] " + failure).withStyle(ChatFormatting.RED));
                return false;
            }
            serverPlayer.closeContainer();
            return true;
        }
        if (id == BUTTON_OPEN_BINDER) {
            serverPlayer.closeContainer();
            BinderOpener.open(serverPlayer);
            return true;
        }
        if (id == BUTTON_CHALLENGE_BOSS) {
            String failure = CombatManager.challengeBoss(serverPlayer, -1);
            if (failure != null) {
                StcChat.tell(serverPlayer, Component.literal("[STC] " + failure).withStyle(ChatFormatting.RED));
                return false;
            }
            serverPlayer.closeContainer();
            return true;
        }
        if (id == BUTTON_RESTART_RUN) {
            // v0.4.0：「重开本局」= 清空本局并把角色置空，然后由玩家**重新选择角色**
            String message = RunService.clearForReselect(serverPlayer);
            StcChat.tell(serverPlayer, Component.literal("[STC] " + message));
            return true;
        }
        return false;
    }
}
