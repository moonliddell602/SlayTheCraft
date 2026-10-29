package com.slaythecraft.client;

import java.util.List;

import com.slaythecraft.merchant.CardShopOffer;
import com.slaythecraft.network.ShopOffersPayload;

/**
 * 客户端保存"当前这家店卖什么"（v0.0.2 V8；v0.4.6 扩充成卡牌 + 遗物 + 药水 + 删牌价）。
 *
 * <p>商店货架是菜单本地状态，不像玩家档案那样常驻同步，所以由 {@link ShopOffersPayload}
 * 在打开 / 购买 / 刷新后推送过来。</p>
 */
public final class ClientShopOffers {

    private static int containerId = -1;
    private static List<CardShopOffer> offers = List.of();
    private static List<String> relics = List.of();
    private static List<String> potions = List.of();
    private static int refreshCost;
    private static int refreshLeft;
    private static int relicPrice;
    private static int potionPrice;
    private static int deleteCost;

    private ClientShopOffers() {
    }

    public static void accept(ShopOffersPayload payload) {
        containerId = payload.containerId();
        offers = payload.stock().offers();
        relics = payload.stock().relics();
        potions = payload.stock().potions();
        refreshCost = payload.prices().refreshCost();
        relicPrice = payload.prices().relicPrice();
        potionPrice = payload.prices().potionPrice();
        deleteCost = payload.prices().deleteCost();
        refreshLeft = payload.refreshLeft();
    }

    /** 该菜单当前是否已经收到过货架数据。 */
    public static boolean hasDataFor(int menuContainerId) {
        return menuContainerId == containerId;
    }

    public static List<CardShopOffer> offersFor(int menuContainerId) {
        return menuContainerId == containerId ? offers : List.of();
    }

    public static List<String> relicsFor(int menuContainerId) {
        return menuContainerId == containerId ? relics : List.of();
    }

    public static List<String> potionsFor(int menuContainerId) {
        return menuContainerId == containerId ? potions : List.of();
    }

    public static int refreshCostFor(int menuContainerId) {
        return menuContainerId == containerId ? refreshCost : 0;
    }

    public static int refreshLeftFor(int menuContainerId) {
        return menuContainerId == containerId ? refreshLeft : 0;
    }

    public static int relicPriceFor(int menuContainerId) {
        return menuContainerId == containerId ? relicPrice : 0;
    }

    public static int potionPriceFor(int menuContainerId) {
        return menuContainerId == containerId ? potionPrice : 0;
    }

    public static int deleteCostFor(int menuContainerId) {
        return menuContainerId == containerId ? deleteCost : 0;
    }

    public static void clear() {
        containerId = -1;
        offers = List.of();
        relics = List.of();
        potions = List.of();
    }
}
