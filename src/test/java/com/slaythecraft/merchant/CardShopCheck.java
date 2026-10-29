package com.slaythecraft.merchant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardRarity;
import com.slaythecraft.combat.core.RelicLibrary;

/**
 * 卡牌商店的定价与货架抽取校验（设计文档 §6.4）。
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.merchant.CardShopCheck
 * </pre>
 */
public final class CardShopCheck {

    private CardShopCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 定价
        check(lines, counters, "普通卡售价 75",
                CardShopOffers.priceOf(cardOf("strike")) == 75, 75, CardShopOffers.priceOf(cardOf("strike")));
        check(lines, counters, "罕见卡售价 150",
                CardShopOffers.priceOf(cardOf("pommel_strike")) == 150, 150,
                CardShopOffers.priceOf(cardOf("pommel_strike")));
        // v0.5.1.5：金色已统一成稀有 → 恶魔形态现在按**稀有**档定价（300，不再是金色的 600）
        check(lines, counters, "原金色卡（现在是稀有）按稀有档定价",
                CardShopOffers.priceOf(cardOf("demon_form")) == 300
                        && cardOf("demon_form").rarity() == CardRarity.RARE
                        && CardShopOffers.priceOf(cardOf("demon_form")) < 600,
                300, CardShopOffers.priceOf(cardOf("demon_form")) + " / " + cardOf("demon_form").rarity());

        // ---------------------------------------------------------- 货架
        List<CardShopOffer> offers = CardShopOffers.roll(CardLibrary.IRONCLAD, 3, new Random(2024));
        check(lines, counters, "一店摆 3 件商品", offers.size() == 3, 3, offers.size());
        check(lines, counters, "同一店里不重复",
                new HashSet<>(offers.stream().map(CardShopOffer::cardId).toList()).size() == offers.size(),
                offers.size(), new HashSet<>(offers.stream().map(CardShopOffer::cardId).toList()).size());
        boolean allPriced = true;
        boolean noGold = true;
        for (CardShopOffer offer : offers) {
            Card card = CardLibrary.byId(offer.cardId().getPath());
            if (card == null || offer.price() != CardShopOffers.priceOf(card) || offer.price() <= 0) {
                allPriced = false;
            }
            if (card != null && card.rarity() == CardRarity.GOLD) {
                noGold = false;
            }
        }
        check(lines, counters, "每件商品价格与其品质一致且为正数", allPriced, true, false);
        // v0.5.1.5：已经没有金色卡了，所以这条变成"守卫"：将来真加了金色卡也不会被售卖
        check(lines, counters, "商店不出售金色品质的卡（当前一张金色卡都不存在）", noGold, true, false);
        check(lines, counters, "而且确认整个卡池里已经没有金色品质",
                CardLibrary.allGold().isEmpty(), 0, CardLibrary.allGold().size());

        // v0.4.6.3：货架池 = 角色牌（去掉初始牌）+ 无色牌；要多少就必须摆满多少
        int defectPool = CardShopOffers.shopPool(CardLibrary.DEFECT).size();
        List<CardShopOffer> big = CardShopOffers.roll(CardLibrary.DEFECT, defectPool + 5, new Random(5));
        check(lines, counters, "要得比池子多时也要摆满（允许重复）",
                big.size() == defectPool + 5, defectPool + 5, big.size());
        check(lines, counters, "大量抽取也不会混入金色品质的卡",
                big.stream().noneMatch(offer -> {
                    Card card = CardLibrary.byId(offer.cardId().getPath());
                    return card != null && card.rarity() == CardRarity.GOLD;
                }), true, false);

        // v0.4.6.3：不卖初始牌（打击 / 格挡）
        List<CardShopOffer> many = CardShopOffers.roll(CardLibrary.IRONCLAD, 60, new Random(9));
        boolean noBasic = many.stream().noneMatch(offer -> CardLibrary.isBasic(offer.cardId().getPath()));
        check(lines, counters, "卡牌大师不售卖初始牌（打击 / 格挡）", noBasic, true,
                many.stream().filter(o -> CardLibrary.isBasic(o.cardId().getPath())).count() + " 张初始牌");

        // v0.4.6.3：每个角色的货架都必须满 5 张、且尽量不重复
        boolean alwaysFive = true;
        boolean distinct = true;
        StringBuilder sizes = new StringBuilder();
        for (String character : List.of(CardLibrary.IRONCLAD, CardLibrary.SILENT, CardLibrary.DEFECT,
                CardLibrary.PILLAGER)) {
            List<CardShopOffer> shelf = CardShopOffers.roll(character, 5, new Random(31));
            alwaysFive &= shelf.size() == 5;
            distinct &= new HashSet<>(shelf.stream().map(CardShopOffer::cardId).toList()).size() == 5;
            sizes.append(character).append('=').append(shelf.size()).append(' ');
        }
        check(lines, counters, "每个角色的货架都是满 5 张（" + sizes.toString().trim() + "）", alwaysFive, 5,
                sizes.toString().trim());
        check(lines, counters, "满货架时 5 张互不重复", distinct, true, false);

        // 池子大小：至少要能摆满 5 张不重复的
        boolean bigEnough = true;
        StringBuilder poolSizes = new StringBuilder();
        for (String character : List.of(CardLibrary.IRONCLAD, CardLibrary.SILENT, CardLibrary.DEFECT,
                CardLibrary.PILLAGER)) {
            int size = CardShopOffers.shopPool(character).size();
            bigEnough &= size >= 5;
            poolSizes.append(character).append('=').append(size).append(' ');
        }
        check(lines, counters, "每个角色的货池都 ≥ 5 张（" + poolSizes.toString().trim() + "）", bigEnough, true,
                poolSizes.toString().trim());

        // ---------------------------------------------------------- v0.5.5.2（用户 B10）：
        // "添加过滤。单人商店不出售，多人出售。" —— 多人玩法向的卡（=【商队护送】）
        boolean singleHasEscort = CardShopOffers.shopPool(CardLibrary.PILLAGER, 1).stream()
                .anyMatch(card -> card.id().equals("caravan_escort"));
        boolean multiHasEscort = CardShopOffers.shopPool(CardLibrary.PILLAGER, 2).stream()
                .anyMatch(card -> card.id().equals("caravan_escort"));
        check(lines, counters, "单人（1 人）商店**不**出售【商队护送】", !singleHasEscort, false, singleHasEscort);
        check(lines, counters, "多人（2 人）商店**出售**【商队护送】", multiHasEscort, true, multiHasEscort);
        check(lines, counters, "旧的单参 shopPool 仍是单人视角（不含【商队护送】）",
                CardShopOffers.shopPool(CardLibrary.PILLAGER).stream()
                        .noneMatch(card -> card.id().equals("caravan_escort")),
                true, false);
        // 抽一整店 / 补一件都要走过滤后的池子
        boolean rolledSingle = CardShopOffers.roll(CardLibrary.PILLAGER, 40, new Random(11), null, 1).stream()
                .anyMatch(offer -> offer.cardId().getPath().equals("caravan_escort"));
        boolean rolledMulti = CardShopOffers.roll(CardLibrary.PILLAGER, 40, new Random(11), null, 2).stream()
                .anyMatch(offer -> offer.cardId().getPath().equals("caravan_escort"));
        check(lines, counters, "单人抽一整店不会出现【商队护送】", !rolledSingle, false, rolledSingle);
        check(lines, counters, "多人抽一整店会出现【商队护送】", rolledMulti, true, rolledMulti);
        String shopMenu = readSource("merchant/CardShopMenu.java", lines, counters);
        if (shopMenu != null) {
            check(lines, counters, "商店摆架与补货都把队伍人数传进去（partySize）",
                    shopMenu.contains("CombatManager.partySize(player)")
                            && shopMenu.contains("CombatManager.partySize(serverPlayer)"),
                    "出现", "缺失");
        }

        // 补货：不能补一张已经在货架上的卡
        List<CardShopOffer> shelf = new java.util.ArrayList<>(
                CardShopOffers.roll(CardLibrary.SILENT, 5, new Random(77)));
        CardShopOffer replacement = CardShopOffers.rollOne(CardLibrary.SILENT, new Random(78), shelf);
        check(lines, counters, "补货（单件）能拿到商品", replacement != null, true, false);
        check(lines, counters, "补位上架的卡不会和货架上已有的重复",
                replacement != null && shelf.stream().noneMatch(offer -> offer.cardId().equals(replacement.cardId())),
                true, false);

        // 未选角色：角色池为空，但无色牌的图鉴描述就是"可以从商店获得"，所以只卖无色牌
        List<CardShopOffer> noCharacter = CardShopOffers.roll(null, 3, new Random(3));
        check(lines, counters, "未选角色时只卖无色牌（角色池为空）",
                noCharacter.stream().allMatch(offer -> {
                    Card card = CardLibrary.byId(offer.cardId().getPath());
                    return card == null || card.character() == null;
                }), true, false);

        // ---------------------------------------------------------- v0.5.3.1/.2：遗物逐件定价 + 来源通道
        check(lines, counters, "逐件定价：金刚杵 195",
                RelicLibrary.hasPrice("vajra") && RelicLibrary.priceOf("vajra") == 195, 195,
                RelicLibrary.priceOf("vajra"));
        check(lines, counters, "v0.5.3.2 逐件定价：制图台 210 / 龙蛋 240",
                RelicLibrary.priceOf("cartography_table") == 210 && RelicLibrary.priceOf("dragon_egg") == 240,
                "210 / 240", RelicLibrary.priceOf("cartography_table") + " / "
                        + RelicLibrary.priceOf("dragon_egg"));
        check(lines, counters, "逐件定价一共 14 件（= 24 件里除初始 4 + 事件限定 6 之外的全部）",
                RelicLibrary.pricedCount() == 14, 14, RelicLibrary.pricedCount());
        check(lines, counters, "**每一件能上架商店的遗物都有单价**（配置统一价只剩兜底）",
                RelicLibrary.all().stream()
                        .filter(relic -> RelicLibrary.droppableRelic(relic.id()))
                        .allMatch(relic -> RelicLibrary.hasPrice(relic.id())),
                "全都有", "有漏定价的");
        check(lines, counters, "初始遗物 / 事件限定遗物没有单价（它们根本不卖）",
                !RelicLibrary.hasPrice("burning_blood") && !RelicLibrary.hasPrice("mogu_fillet")
                        && RelicLibrary.priceOf("burning_blood") == 0,
                "0 / 没有单价", RelicLibrary.priceOf("burning_blood"));
        check(lines, counters, "配置里的遗物统一价（兜底价）是 150",
                com.slaythecraft.Config.shopPriceRelic() == 150, 150,
                com.slaythecraft.Config.shopPriceRelic());

        var plainBuyer = new com.slaythecraft.data.PlayerCardData();
        check(lines, counters, "没有披肩时不打折（200 → 200）",
                CardShopOffers.discount(plainBuyer, 200) == 200, 200,
                CardShopOffers.discount(plainBuyer, 200));
        var shawlBuyer = new com.slaythecraft.data.PlayerCardData();
        shawlBuyer.grantRelic("trader_llama_shawl");
        check(lines, counters, "有【行商羊驼披肩】时 -25%（逐件价 200 → 150）",
                CardShopOffers.discount(shawlBuyer, 200) == 150, 150,
                CardShopOffers.discount(shawlBuyer, 200));
        check(lines, counters, "有披肩时最低仍收 1 金币（4 → 3、1 → 1）",
                CardShopOffers.discount(shawlBuyer, 4) == 3 && CardShopOffers.discount(shawlBuyer, 1) == 1,
                "3 / 1", CardShopOffers.discount(shawlBuyer, 4) + " / "
                        + CardShopOffers.discount(shawlBuyer, 1));
        check(lines, counters, "披肩打在**逐件价**上（195 → 146，不是先打 150）",
                CardShopOffers.discount(shawlBuyer, RelicLibrary.priceOf("vajra")) == 146, 146,
                CardShopOffers.discount(shawlBuyer, RelicLibrary.priceOf("vajra")));

        check(lines, counters, "商店池 = 可掉落 + 「只能商店」的龙蛋，披肩与制图台也在",
                RelicLibrary.shopPool().stream().anyMatch(relic -> relic.id().equals("dragon_egg"))
                        && RelicLibrary.shopPool().stream().anyMatch(relic -> relic.id().equals("cartography_table"))
                        && RelicLibrary.shopPool().stream()
                        .anyMatch(relic -> relic.id().equals("trader_llama_shawl")),
                "三件都在", "有缺失");
        check(lines, counters, "藏宝箱池含披肩与制图台（v0.5.3.2）、不含龙蛋",
                RelicLibrary.chestPool().stream().anyMatch(relic -> relic.id().equals("trader_llama_shawl"))
                        && RelicLibrary.chestPool().stream()
                        .anyMatch(relic -> relic.id().equals("cartography_table"))
                        && RelicLibrary.chestPool().stream()
                        .noneMatch(relic -> relic.id().equals("dragon_egg")), "符合", "不符合");
        check(lines, counters, "战斗掉落池里没有龙蛋 / 制图台 / 披肩",
                RelicLibrary.elitePool().stream().noneMatch(relic -> relic.id().equals("dragon_egg")
                        || relic.id().equals("cartography_table")
                        || relic.id().equals("trader_llama_shawl")), "符合", "不符合");
        check(lines, counters, "遗物货架上不会出现初始遗物 / 事件限定遗物",
                CardShopOffers.rollRelics(5, new Random(11), java.util.Set.of()).stream()
                        .noneMatch(id -> RelicLibrary.isStarting(id) || RelicLibrary.isEventOnly(id)),
                "符合", "不符合");

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static Card cardOf(String id) {
        Card card = CardLibrary.byId(id);
        if (card == null) {
            throw new IllegalStateException("卡池里没有 " + id);
        }
        return card;
    }

    /** 读一段主源码（源码级断言用）；读不到返回 {@code null} 并记一条失败。 */
    private static String readSource(String relative, List<String> lines, int[] counters) {
        java.nio.file.Path path = java.nio.file.Path.of("src", "main", "java", "com", "slaythecraft")
                .resolve(relative);
        try {
            return java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            check(lines, counters, "源码可读：" + path, false, "可读", e.getClass().getSimpleName());
            return null;
        }
    }

    private static void check(List<String> lines, int[] counters, String label, boolean condition,
                             Object expected, Object actual) {
        if (condition) {
            counters[0]++;
            lines.add("PASS  " + label);
        } else {
            counters[1]++;
            lines.add("FAIL  " + label + "（期望 " + expected + "，实际 " + actual + "）");
        }
    }
}
