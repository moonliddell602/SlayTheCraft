package com.slaythecraft.binder;

import java.util.function.Supplier;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.gate.SpireGateMenu;
import com.slaythecraft.merchant.CardShopMenu;
import com.slaythecraft.reward.RewardMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单类型注册。 */
public final class StcMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SlayTheCraft.MODID);

    /** 卡牌收纳包界面。数据不在菜单里同步，客户端直接读已同步的玩家档案（见 S1）。 */
    public static final Supplier<MenuType<CardBinderMenu>> CARD_BINDER = MENUS.register("card_binder",
            () -> IMenuTypeExtension.create(CardBinderMenu::binder));

    /** 卡牌回收站界面：与收纳包共用界面类，只是不能领奖。 */
    public static final Supplier<MenuType<CardBinderMenu>> RECYCLER = MENUS.register("card_recycler",
            () -> IMenuTypeExtension.create(CardBinderMenu::recycler));

    /** 尖塔入口界面（引导方块）：选角色 / 开始对战 / 查看卡组。 */
    public static final Supplier<MenuType<SpireGateMenu>> SPIRE_GATE = MENUS.register("spire_gate",
            () -> IMenuTypeExtension.create(SpireGateMenu::new));

    /** 卡牌奖励三选一界面。 */
    public static final Supplier<MenuType<RewardMenu>> REWARD = MENUS.register("reward",
            () -> IMenuTypeExtension.create(RewardMenu::new));

    /** 战斗结算界面（v0.5.0.20）：领取金币 / 领取遗物 / 卡牌奖励 / 跳过。 */
    public static final Supplier<MenuType<RewardMenu>> BATTLE_RESULT = MENUS.register("battle_result",
            () -> IMenuTypeExtension.create((containerId, inventory, data) ->
                    new RewardMenu(containerId, inventory, RewardMenu.Mode.RESULT)));

    /** 卡牌商店界面（卡牌商人）。 */
    public static final Supplier<MenuType<CardShopMenu>> CARD_SHOP = MENUS.register("card_shop",
            () -> IMenuTypeExtension.create(CardShopMenu::new));

    /** 篝火准备界面（v0.0.3 W5）：锻造 / 愈合 / 包扎。 */
    public static final Supplier<MenuType<com.slaythecraft.campfire.CampfireMenu>> CAMPFIRE =
            MENUS.register("campfire",
                    () -> IMenuTypeExtension.create(com.slaythecraft.campfire.CampfireMenu::new));

    private StcMenus() {
    }
}
