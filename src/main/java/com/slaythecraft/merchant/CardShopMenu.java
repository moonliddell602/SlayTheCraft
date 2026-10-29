package com.slaythecraft.merchant;

import com.slaythecraft.chat.StcChat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.CardDeleteService;
import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.ShopOffersPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 卡牌大师商店菜单（设计文档 §6.4；v0.4.6 扩充成需求指定的"全货架"）。
 *
 * <p>需求原文（v0.4.6）："售卖：5张卡牌 三种遗物 3种药水 一次删牌机会。
 * 花费 40 金币可以刷新除删牌机会外的其他物资，最多三次。"</p>
 *
 * <p>货架是**菜单本地状态**（不落盘）：首次 tick 服务端抽好并通过 {@link ShopOffersPayload}
 * 下发给客户端，客户端只负责展示。买走一件就补抽一件；刷新整店会重置卡牌/遗物/药水，
 * **不重置删牌机会**，并且受 {@code shopRefreshLimit}（默认 3 次）限制。</p>
 *
 * <p>按钮 id：</p>
 * <ul>
 *   <li>{@code 0..n-1}：买第 n 张卡</li>
 *   <li>{@link #RELIC_BASE} + i：买第 i 件遗物</li>
 *   <li>{@link #POTION_BASE} + i：买第 i 瓶药水</li>
 *   <li>{@link #BUTTON_REFRESH}：刷新商品</li>
 *   <li>{@link #DELETE_CARD_BASE} + 卡牌下标：删掉那张卡</li>
 * </ul>
 */
public class CardShopMenu extends AbstractContainerMenu {

    public static final int BUTTON_REFRESH = 50;
    public static final int RELIC_BASE = 100;
    public static final int POTION_BASE = 200;
    public static final int DELETE_CARD_BASE = 400;

    private final Player owner;
    private List<CardShopOffer> offers = List.of();
    private List<String> relics = List.of();
    private List<String> potions = List.of();
    private int refreshesLeft;
    private boolean offersSent;

    public CardShopMenu(int containerId, Inventory inventory) {
        super(StcMenus.CARD_SHOP.get(), containerId);
        this.owner = inventory.player;
        this.refreshesLeft = Config.shopRefreshLimit();
        if (owner instanceof ServerPlayer serverPlayer) {
            rollAll(serverPlayer);
        }
    }

    /** 客户端构造（服务端会推货架）。 */
    public CardShopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        super(StcMenus.CARD_SHOP.get(), containerId);
        this.owner = inventory.player;
    }

    /** 开店（服务端调用）。 */
    public static void open(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StcAttachments.syncTo(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new CardShopMenu(containerId, inventory),
                    Component.translatable("stc.shop.title")));
        }
    }

    /** 抽一整店货（卡牌 + 遗物 + 药水）。 */
    private void rollAll(ServerPlayer player) {
        PlayerCardData data = StcAttachments.dataOf(player);
        Random random = new Random(player.getRandom().nextLong());
        // v0.5.5.2（用户 B10）："单人商店不出售，多人出售" —— 货架按队伍人数过滤
        offers = CardShopOffers.roll(characterId(player), Config.shopOfferCount(), random, data,
                com.slaythecraft.combat.mc.CombatManager.partySize(player));
        relics = CardShopOffers.rollRelics(Config.shopRelicCount(), random, data.relics());
        potions = CardShopOffers.rollPotions(Config.shopPotionCount(), random);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && owner.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 首个 tick 把货架下发给客户端（clientbound 包只在服务端发得出）。 */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (offersSent || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        sendOffers(serverPlayer);
        offersSent = true;
    }

    private void sendOffers(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ShopOffersPayload(containerId,
                new ShopOffersPayload.ShopStock(offers, relics, potions),
                // v0.5.3.0：所有价格都过一遍【行商羊驼披肩】的 -25%
                new ShopOffersPayload.ShopPrices(
                        CardShopOffers.discount(StcAttachments.dataOf(player), Config.shopRefreshCost()),
                        CardShopOffers.discount(StcAttachments.dataOf(player), Config.shopPriceRelic()),
                        CardShopOffers.discount(StcAttachments.dataOf(player), Config.shopPricePotion()),
                        CardShopOffers.discount(StcAttachments.dataOf(player),
                                CardDeleteService.cost(StcAttachments.dataOf(player)))),
                refreshesLeft));
    }

    /** 买了东西 / 刷新之后重新下发（客户端据此重画）。 */
    private void resend(ServerPlayer player) {
        sendOffers(player);
        offersSent = true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        PlayerCardData data = StcAttachments.dataOf(serverPlayer);
        Random random = new Random(serverPlayer.getRandom().nextLong());

        // ---- 删牌（v0.4.6：价格随"已删张数"上涨）
        if (id >= DELETE_CARD_BASE) {
            int cost = CardShopOffers.discount(data, CardDeleteService.cost(data));
            var result = CardDeleteService.deleteAt(data, id - DELETE_CARD_BASE);
            switch (result) {
                case OK -> {
                    StcAttachments.syncTo(serverPlayer);
                    // v0.5.0.10 成就【再玩小卡组找人弄你】：需求"第一次删牌"
                    com.slaythecraft.achievement.AchievementService.grantCardDelete(serverPlayer);
                    int next = CardDeleteService.cost(data);
                    StcChat.tell(serverPlayer, Component.literal(
                                    "[STC] 商人帮你删掉了 1 张卡牌，花费 " + cost + " 金币（剩余 " + data.gold()
                                            + "，卡牌 " + data.cardCount() + " 张）。下一次删牌要 "
                                            + next + " 金币。")
                            .withStyle(ChatFormatting.GRAY));
                    resend(serverPlayer);
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
                    StcChat.tell(serverPlayer, Component.literal("[STC] 该卡牌已不存在（列表已刷新）。")
                            .withStyle(ChatFormatting.RED));
                    return false;
                }
            }
        }

        // ---- 刷新（需求：40 金币、最多三次、不动删牌机会）
        if (id == BUTTON_REFRESH) {
            if (refreshesLeft <= 0) {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 这家店本次最多只能刷新 " + Config.shopRefreshLimit() + " 次。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            int cost = CardShopOffers.discount(data, Config.shopRefreshCost());
            if (!data.spendGold(cost)) {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 刷新商品需要 " + cost + " 金币，当前 " + data.gold() + "。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            refreshesLeft--;
            rollAll(serverPlayer);
            StcAttachments.syncTo(serverPlayer);
            resend(serverPlayer);
            StcChat.tell(serverPlayer, Component.literal("[STC] 已刷新商品，花费 " + cost
                    + " 金币（剩余 " + data.gold() + "，还能刷新 " + refreshesLeft + " 次）。")
                    .withStyle(ChatFormatting.GRAY));
            return true;
        }

        // ---- 买遗物
        if (id >= RELIC_BASE && id < POTION_BASE) {
            int index = id - RELIC_BASE;
            if (index < 0 || index >= relics.size()) {
                return false;
            }
            String relicId = relics.get(index);
            Relic relic = RelicLibrary.byId(relicId);
            if (relic == null) {
                StcChat.tell(serverPlayer, Component.literal("[STC] 这件商品已经失效（遗物表变更）。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            if (data.hasRelic(relicId)) {
                StcChat.tell(serverPlayer, Component.literal("[STC] 你已经拥有【" + relic.name() + "】了。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            // v0.5.3.1：每件遗物的售价由 RelicLibrary 逐件给定（没有单价的沿用配置统一价）
            int baseRelicPrice = com.slaythecraft.combat.core.RelicLibrary.hasPrice(relicId)
                    ? com.slaythecraft.combat.core.RelicLibrary.priceOf(relicId)
                    : Config.shopPriceRelic();
            int price = CardShopOffers.discount(data, baseRelicPrice);
            if (!data.spendGold(price)) {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 金币不足：【" + relic.name() + "】需要 " + price + " 金币，当前 "
                                        + data.gold() + "。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            String extra = data.grantRelic(relicId);
            // v0.5.3.0：遗物的"获得时"效果（破旧打火石附魔 / 制图台 5 次卡牌奖励）
            String onObtain = com.slaythecraft.relic.RelicObtainService.apply(serverPlayer, relicId);
            // 货架补一件
            List<String> updated = new ArrayList<>(relics);
            String replacement = CardShopOffers.rollOneRelic(random, data.relics(), updated);
            if (replacement == null) {
                updated.remove(index);
            } else {
                updated.set(index, replacement);
            }
            relics = List.copyOf(updated);
            StcAttachments.syncTo(serverPlayer);
            resend(serverPlayer);
            StcChat.tell(serverPlayer, Component.literal("[STC] 购买了【" + relic.name() + "】，花费 "
                    + price + " 金币（剩余 " + data.gold() + "）。" + (extra.isEmpty() ? "" : extra)
                    + (onObtain.isEmpty() ? "" : onObtain))
                    .withStyle(ChatFormatting.GOLD));
            return true;
        }

        // ---- 买药水
        if (id >= POTION_BASE && id < DELETE_CARD_BASE) {
            int index = id - POTION_BASE;
            if (index < 0 || index >= potions.size()) {
                return false;
            }
            Potion potion = PotionLibrary.byId(potions.get(index));
            if (potion == null) {
                StcChat.tell(serverPlayer, Component.literal("[STC] 这瓶商品已经失效（药水表变更）。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            int slots = data.potionSlots(Config.potionSlots());
            if (data.potionCount() >= slots) {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 药水槽位已满（" + slots + " 格），先用掉一瓶再来买。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            int price = CardShopOffers.discount(data, Config.shopPricePotion());
            if (!data.spendGold(price)) {
                StcChat.tell(serverPlayer, Component.literal(
                                "[STC] 金币不足：【" + potion.name() + "】需要 " + price + " 金币，当前 "
                                        + data.gold() + "。")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            data.addPotion(potion.id(), slots);
            List<String> updated = new ArrayList<>(potions);
            String replacement = CardShopOffers.rollOnePotion(random, updated);
            if (replacement == null) {
                updated.remove(index);
            } else {
                updated.set(index, replacement);
            }
            potions = List.copyOf(updated);
            StcAttachments.syncTo(serverPlayer);
            resend(serverPlayer);
            StcChat.tell(serverPlayer, Component.literal("[STC] 购买了【" + potion.name() + "】，花费 "
                    + price + " 金币（剩余 " + data.gold() + "）。").withStyle(ChatFormatting.GOLD));
            return true;
        }

        // ---- 买卡
        if (id < 0 || id >= offers.size()) {
            return false;
        }
        CardShopOffer offer = offers.get(id);
        Card definition = CardLibrary.byId(offer.cardId().getPath());
        if (definition == null) {
            StcChat.tell(serverPlayer, Component.literal("[STC] 这件商品已经失效（卡池变更）。")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (!data.spendGold(offer.price())) {
            StcChat.tell(serverPlayer, Component.literal(
                            "[STC] 金币不足：【" + definition.name() + "】需要 " + offer.price()
                                    + " 金币，当前 " + data.gold() + "。")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        data.addCard(CardInstance.of(offer.cardId()));

        // 买走的位置补一件新商品，店面始终保持满架
        List<CardShopOffer> updated = new ArrayList<>(offers);
        // v0.4.6.3：补位时避开货架上已有的卡，避免买走之后出现两张一样的
        updated.remove(id);
        CardShopOffer replacement = CardShopOffers.rollOne(characterId(serverPlayer), random, updated, data,
                com.slaythecraft.combat.mc.CombatManager.partySize(serverPlayer));
        if (replacement == null) {
            updated.remove(id);
        } else {
            updated.set(id, replacement);
        }
        offers = List.copyOf(updated);
        StcAttachments.syncTo(serverPlayer);
        resend(serverPlayer);
        StcChat.tell(serverPlayer, Component.literal("[STC] 购买了【" + definition.name() + "】，花费 "
                + offer.price() + " 金币（剩余 " + data.gold() + "，卡牌 " + data.cardCount() + " 张）。")
                .withStyle(ChatFormatting.GOLD));
        SlayTheCraft.LOGGER.debug("[STC] {} 从商店购买了 {}", serverPlayer.getScoreboardName(), offer.cardId());
        return true;
    }

    /** 玩家当前角色的 id 字符串（未选角色则只卖公共卡）。 */
    private static String characterId(ServerPlayer player) {
        var character = StcAttachments.dataOf(player).character();
        return character == null ? null : character.toString();
    }
}
