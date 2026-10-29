package com.slaythecraft.binder;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 删卡逻辑校验：**收纳包 UI 与卡牌回收站共用同一段代码与同一个价格**，
 * 而且这两处都会产生金币消耗，所以边界必须钉死。
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.binder.CardDeleteServiceCheck
 * </pre>
 */
public final class CardDeleteServiceCheck {

    private CardDeleteServiceCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // v0.4.6：价格 = 基础价 + 50 × 已经删过的张数
        check(lines, counters, "基础删卡价格为 100", CardDeleteService.baseCost() == 100, 100,
                CardDeleteService.baseCost());
        PlayerCardData fresh = new PlayerCardData();
        check(lines, counters, "没删过牌时价格 = 基础价", CardDeleteService.cost(fresh) == 100, 100,
                CardDeleteService.cost(fresh));
        check(lines, counters, "每次删牌后价格 +50（第 1 次删完变成 150）",
                CardDeleteService.costAfter(1) == 150, 150, CardDeleteService.costAfter(1));
        check(lines, counters, "第 3 次删牌要 250", CardDeleteService.costAfter(3) == 250, 250,
                CardDeleteService.costAfter(3));

        // 金币充足：正常删除
        PlayerCardData rich = archive(3, 500);
        CardDeleteService.Result ok = CardDeleteService.deleteAt(rich, 0);
        check(lines, counters, "金币充足时删除成功", ok == CardDeleteService.Result.OK,
                CardDeleteService.Result.OK, ok);
        check(lines, counters, "删除后卡牌数 -1", rich.cardCount() == 2, 2, rich.cardCount());
        check(lines, counters, "删除后金币 -100", rich.gold() == 400, 400, rich.gold());
        check(lines, counters, "删卡统计 +1", rich.stats().cardsDeleted() == 1, 1, rich.stats().cardsDeleted());
        check(lines, counters, "删过一次之后这一次的价格已经涨到 150", CardDeleteService.cost(rich) == 150, 150,
                CardDeleteService.cost(rich));

        // 金币不足：什么都不该发生
        PlayerCardData poor = archive(2, 99);
        CardDeleteService.Result rejected = CardDeleteService.deleteAt(poor, 0);
        check(lines, counters, "金币不足时被拒绝", rejected == CardDeleteService.Result.NOT_ENOUGH_GOLD,
                CardDeleteService.Result.NOT_ENOUGH_GOLD, rejected);
        check(lines, counters, "被拒绝时卡牌未被删除", poor.cardCount() == 2, 2, poor.cardCount());
        check(lines, counters, "被拒绝时金币未变", poor.gold() == 99, 99, poor.gold());

        // 索引越界
        PlayerCardData bounds = archive(1, 500);
        check(lines, counters, "越界索引返回 NO_SUCH_CARD",
                CardDeleteService.deleteAt(bounds, 5) == CardDeleteService.Result.NO_SUCH_CARD, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "越界尝试不扣金币", bounds.gold() == 500, 500, bounds.gold());
        check(lines, counters, "负数索引返回 NO_SUCH_CARD",
                CardDeleteService.deleteAt(bounds, -1) == CardDeleteService.Result.NO_SUCH_CARD, Boolean.TRUE, Boolean.FALSE);

        // 按 id 删除
        PlayerCardData byId = archive(0, 500);
        byId.addCard(CardInstance.of(id("strike")));
        byId.addCard(CardInstance.of(id("defend")));
        byId.addCard(CardInstance.of(id("strike")));
        CardDeleteService.Result removedFirst = CardDeleteService.deleteById(byId, id("strike"));
        check(lines, counters, "按 id 删除成功", removedFirst == CardDeleteService.Result.OK,
                CardDeleteService.Result.OK, removedFirst);
        check(lines, counters, "按 id 只删一张", byId.cardCount() == 2, 2, byId.cardCount());
        check(lines, counters, "剩下的第一张变成 defend", byId.cards().get(0).id().equals(id("defend")),
                id("defend"), byId.cards().get(0).id());
        check(lines, counters, "删除不存在的 id 返回 NO_SUCH_CARD",
                CardDeleteService.deleteById(byId, id("bash")) == CardDeleteService.Result.NO_SUCH_CARD,
                Boolean.TRUE, Boolean.FALSE);

        // 刚好够钱
        PlayerCardData exact = archive(1, 100);
        check(lines, counters, "金币刚好等于价格时可以删除",
                CardDeleteService.deleteAt(exact, 0) == CardDeleteService.Result.OK, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "删完余额为 0", exact.gold() == 0, 0, exact.gold());

        // ---- v0.5.1.6：【不屈】的"彻底从牌组移除"复用删牌
        //
        // 用户给定的语义："直接从玩家的卡组中删掉（就是删牌）。" 所以它必须走**同一个方法**
        // （removeFirstCard），连"删牌数 +1"（推高下次删牌价格）都一致。
        PlayerCardData stubborn = archive(0, 0);
        stubborn.addCard(CardInstance.of(id("strike")));
        stubborn.addCard(CardInstance.of(id("last_stand")));
        check(lines, counters, "不屈的那张牌在档案里",
                stubborn.cardCount() == 2 && stubborn.indexOfCard(id("last_stand")) >= 0, true, false);
        boolean removedStubborn = stubborn.removeFirstCard(id("last_stand"));
        check(lines, counters, "按 id 删掉不屈：成功", removedStubborn, true, false);
        check(lines, counters, "不屈删牌后卡牌数 -1（永久）", stubborn.cardCount() == 1, 1, stubborn.cardCount());
        check(lines, counters, "不屈删牌也会让删牌数 +1（和主动删牌一样，价格会涨）",
                stubborn.stats().cardsDeleted() == 1, 1, stubborn.stats().cardsDeleted());
        check(lines, counters, "不屈删牌**不花钱**（它是卡牌自己的效果，不是商店服务）",
                stubborn.gold() == 0, 0, stubborn.gold());
        check(lines, counters, "没有这张牌时删不掉（返回 false，不误伤别的牌）",
                !stubborn.removeFirstCard(id("last_stand")) && stubborn.cardCount() == 1, true, false);

        // 接线：引擎只记账，真正写回档案必须在 MC 层（以前这里漏了，不屈打完还会回牌组）
        String manager = readSource("combat/mc/CombatManager.java", lines, counters);
        if (manager != null) {
            check(lines, counters, "CombatManager 消费了引擎的 removeFromDeck 集合",
                    manager.contains("removeFromDeck()"), "出现", "缺失");
            check(lines, counters, "而且用的是和删牌同一个方法（removeFirstCard）",
                    manager.contains("removeFirstCard("), "出现", "缺失");
            check(lines, counters, "结算发生在 release 之前（之后 context 就没了）",
                    manager.indexOf("discardRemovedCards(context, roster);")
                            < manager.indexOf("release(player, context, outcome);"),
                    "在 release 之前", "在 release 之后或没接");
        }

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    /** 造一个含若干张 strike 的档案。 */
    private static PlayerCardData archive(int cards, int gold) {
        PlayerCardData data = new PlayerCardData();
        for (int i = 0; i < cards; i++) {
            data.addCard(CardInstance.of(id("strike")));
        }
        data.setGold(gold);
        return data;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    /** 读一段主源码（源码级断言用）；读不到返回 {@code null}。 */
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
