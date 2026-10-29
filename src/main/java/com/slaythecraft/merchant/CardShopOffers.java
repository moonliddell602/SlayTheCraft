package com.slaythecraft.merchant;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.PlayerCardData;

/**
 * 商店商品的定价与抽取（设计文档 §6.4；v0.4.6 扩充成"卡牌 + 遗物 + 药水"，
 * v0.4.6.3 修掉"卖初始牌"与"货架不满"两个问题）。
 *
 * <p>需求原文（v0.4.6）："售卖：5张卡牌 三种遗物 3种药水 一次删牌机会。"<br>
 * 需求原文（v0.4.6.3）："卡牌大师不应该售卖初始牌。"、"卡牌大师有时候售卖的卡牌不足5。"</p>
 *
 * <p>于是卡牌货架的池子 = **角色专属牌（去掉初始牌/基础牌）+ 无色牌**：</p>
 * <ul>
 *   <li>【打击】【格挡】这类**初始牌**不再上架（玩家开局就有，卖了也没意义）；</li>
 *   <li>无色牌的图鉴描述本来就是"不固定属于任何人的牌，可以从商店或事件获得"，
 *       所以把它们并进货池，既符合设定又保证货架**永远是满的 5 张**；</li>
 *   <li>万一池子还是比货架小（整合包删过卡），允许重复也要摆满，绝不出现空槽。</li>
 * </ul>
 *
 * <p>遗物只从"能随机掉落"的表里抽（初始遗物与事件限定遗物不上架）；药水从药水表里抽。</p>
 */
public final class CardShopOffers {

    private CardShopOffers() {
    }

    /** 商品价格按品质决定（金币是档案里的数字，不走原版交易界面）。 */
    public static int priceOf(Card card) {
        return switch (card.rarity()) {
            case COMMON -> Config.shopPriceCommon();
            case UNCOMMON -> Config.shopPriceUncommon();
            case RARE -> Config.shopPriceRare();
            case GOLD -> Config.shopPriceGold();
        };
    }

    /**
     * 【行商羊驼披肩】（v0.5.3.0）：商店里的一切价格**降低四分之一**。
     *
     * <p>需求原文："商店的价格降低四分之一。" 所有价格（卡牌 / 遗物 / 药水 / 刷新 / 删牌）
     * 都过这里，所以只在这一处判定遗物，别处不用管。向下取整，最低 1 金币。</p>
     */
    public static int discount(PlayerCardData data, int price) {
        int base = Math.max(0, price);
        if (data == null || !data.hasRelic("trader_llama_shawl")) {
            return base;
        }
        return Math.max(1, base * 3 / 4);
    }

    /** 卡牌价格（含【行商羊驼披肩】折扣）。 */
    public static int priceOf(Card card, PlayerCardData data) {
        return discount(data, priceOf(card));
    }

    /**
     * 卡牌货架的池子（v0.4.6.3）。
     *
     * <p>= 该角色的奖励池 **去掉初始牌/基础牌** + 全部无色牌。</p>
     */
    public static List<Card> shopPool(String characterId) {
        return shopPool(characterId, 1);
    }

    /**
     * 卡牌货架的池子（v0.5.5.2：按**队伍人数**过滤）。
     *
     * <p>用户 B10："添加过滤。单人商店不出售，多人出售。" —— 所以商店和奖励池走同一条规则：
     * 多人玩法向的卡（=【商队护送】）在单人（{@code partySize <= 1}）时不上架，
     * 队伍 ≥ 2 人时正常出售。</p>
     */
    public static List<Card> shopPool(String characterId, int partySize) {
        List<Card> pool = new ArrayList<>();
        for (Card card : CardLibrary.rewardPool(characterId, partySize)) {
            // 需求："卡牌大师不应该售卖初始牌" —— 打击 / 格挡不上架
            if (CardLibrary.isBasic(card.id())) {
                continue;
            }
            pool.add(card);
        }
        // 无色牌：图鉴描述就是"不固定属于任何人的牌，可以从商店或事件获得"
        pool.addAll(CardLibrary.colorless());
        return List.copyOf(pool);
    }

    /**
     * 抽一整店**卡牌**（v0.4.6.3：数量一定摆满）。
     *
     * @param characterId 玩家角色（null = 没选角色，此时只有无色牌可卖）
     * @param count       货架数量（配置 {@code shopOfferCount}，默认 5）
     */
    public static List<CardShopOffer> roll(String characterId, int count, Random random) {
        return roll(characterId, count, random, null, 1);
    }

    /** 带上玩家的版本：会按【行商羊驼披肩】打折（v0.5.3.0）；单人视角。 */
    public static List<CardShopOffer> roll(String characterId, int count, Random random, PlayerCardData data) {
        return roll(characterId, count, random, data, 1);
    }

    /** 完整版本：{@code partySize} 决定多人玩法向的卡上不上架（v0.5.5.2 用户 B10）。 */
    public static List<CardShopOffer> roll(String characterId, int count, Random random, PlayerCardData data,
                                           int partySize) {
        if (count <= 0) {
            return List.of();
        }
        List<Card> pool = new ArrayList<>(shopPool(characterId, partySize));
        List<Card> picked = pickDistinct(pool, count, random);
        // 池子比货架还小：允许重复也要摆满（避免出现"只卖 4 张"的空槽）
        int guard = 0;
        while (picked.size() < count && !pool.isEmpty() && guard++ < count * 8) {
            picked.add(pool.get(random.nextInt(pool.size())));
        }
        List<CardShopOffer> offers = new ArrayList<>(picked.size());
        for (Card card : picked) {
            offers.add(new CardShopOffer(CardIds.of(card), priceOf(card, data)));
        }
        return List.copyOf(offers);
    }

    /**
     * 抽单件卡牌商品（某件被买走后补位）。
     *
     * <p>v0.4.6.3：优先补一张**不在当前货架上**的卡，避免买走之后出现两张一样的；</p>
     */
    public static CardShopOffer rollOne(String characterId, Random random, List<CardShopOffer> onShelf) {
        return rollOne(characterId, random, onShelf, null, 1);
    }

    /** 带上玩家的版本：会按【行商羊驼披肩】打折（v0.5.3.0）；单人视角。 */
    public static CardShopOffer rollOne(String characterId, Random random, List<CardShopOffer> onShelf,
                                        PlayerCardData data) {
        return rollOne(characterId, random, onShelf, data, 1);
    }

    /** 完整版本：补位也要按队伍人数过滤（v0.5.5.2 用户 B10）。 */
    public static CardShopOffer rollOne(String characterId, Random random, List<CardShopOffer> onShelf,
                                        PlayerCardData data, int partySize) {
        List<Card> pool = new ArrayList<>(shopPool(characterId, partySize));
        Set<net.minecraft.resources.ResourceLocation> taken = new LinkedHashSet<>();
        if (onShelf != null) {
            for (CardShopOffer offer : onShelf) {
                taken.add(offer.cardId());
            }
        }
        pool.removeIf(card -> taken.contains(CardIds.of(card)));
        if (pool.isEmpty()) {
            // 全都已经在货架上了：退回允许重复
            List<CardShopOffer> one = roll(characterId, 1, random, data, partySize);
            return one.isEmpty() ? null : one.get(0);
        }
        Card card = weightedPick(pool, random);
        return card == null ? null : new CardShopOffer(CardIds.of(card), priceOf(card, data));
    }

    /** 兼容旧签名（不带上架信息）。 */
    public static CardShopOffer rollOne(String characterId, Random random) {
        return rollOne(characterId, random, List.of());
    }

    /** 抽 {@code count} 张不重复的卡（按品质权重，与奖励抽取同一套权重）。 */
    private static List<Card> pickDistinct(List<Card> pool, int count, Random random) {
        List<Card> remaining = new ArrayList<>(pool);
        List<Card> picked = new ArrayList<>();
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            Card card = weightedPick(remaining, random);
            if (card == null) {
                break;
            }
            picked.add(card);
            remaining.remove(card);
        }
        return picked;
    }

    private static Card weightedPick(List<Card> pool, Random random) {
        int total = 0;
        for (Card card : pool) {
            total += Math.max(1, card.rarity().weight());
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Card card : pool) {
            roll -= Math.max(1, card.rarity().weight());
            if (roll < 0) {
                return card;
            }
        }
        return pool.get(pool.size() - 1);
    }

    /**
     * 抽遗物货架：只从"能随机掉落"的表里抽（初始遗物 + 事件限定遗物不上架），
     * 且**跳过已经拥有的**，尽量不重复。
     */
    public static List<String> rollRelics(int count, Random random, Set<String> owned) {
        if (count <= 0) {
            return List.of();
        }
        List<Relic> pool = new ArrayList<>();
        for (Relic relic : RelicLibrary.shopPool()) {
            if (owned == null || !owned.contains(relic.id())) {
                pool.add(relic);
            }
        }
        List<String> picked = new ArrayList<>(count);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            picked.add(pool.remove(random.nextInt(pool.size())).id());
        }
        return List.copyOf(picked);
    }

    /** 抽药水货架：从药水表里随机抽 {@code count} 瓶不重复的。 */
    public static List<String> rollPotions(int count, Random random) {
        if (count <= 0) {
            return List.of();
        }
        List<Potion> pool = new ArrayList<>(PotionLibrary.obtainable());
        List<String> picked = new ArrayList<>(count);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            picked.add(pool.remove(random.nextInt(pool.size())).id());
        }
        return List.copyOf(picked);
    }

    /** 补一件遗物（买走之后）：从"可掉落且未拥有且不在货架上"的表里挑。 */
    public static String rollOneRelic(Random random, Set<String> owned, List<String> onShelf) {
        Set<String> taken = new LinkedHashSet<>(onShelf == null ? List.of() : onShelf);
        List<Relic> pool = new ArrayList<>();
        for (Relic relic : RelicLibrary.shopPool()) {
            if ((owned == null || !owned.contains(relic.id())) && !taken.contains(relic.id())) {
                pool.add(relic);
            }
        }
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size())).id();
    }

    /** 补一瓶药水（买走之后）。 */
    public static String rollOnePotion(Random random, List<String> onShelf) {
        Set<String> taken = new LinkedHashSet<>(onShelf == null ? List.of() : onShelf);
        List<Potion> pool = new ArrayList<>();
        for (Potion potion : PotionLibrary.obtainable()) {
            if (!taken.contains(potion.id())) {
                pool.add(potion);
            }
        }
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size())).id();
    }
}
