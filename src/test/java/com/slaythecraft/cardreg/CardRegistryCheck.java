package com.slaythecraft.cardreg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardBehaviors;
import com.slaythecraft.combat.core.CardKeyword;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.CombatLog;
import com.slaythecraft.combat.core.CombatRules;
import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.Enemy;
import com.slaythecraft.combat.core.Intent;
import com.slaythecraft.combat.core.PlayerState;
import com.slaythecraft.data.CardIds;

import net.minecraft.resources.ResourceLocation;

/**
 * {@code stc:card} 数据包注册表的自检（v0.5.5.0 A6-1）。
 *
 * <p>这里挡的是三类问题：</p>
 * <ol>
 *   <li><b>codec 与 JSON 对不上</b>：内置的试点 JSON 必须能被 {@link CardDefinition#CODEC} 解析；</li>
 *   <li><b>试点 JSON 与代码内置卡"看起来一样、其实不一样"</b>：逐字段比对 + **各打一场**比对结果。
 *       试点卡的用途正是这个 —— 它们是内置卡的 JSON 镜像，一旦哪边改了另一边没改，这里立刻报红；</li>
 *   <li><b>"能加载但会静默出错"</b>：行为 id 写错 / 参数个数不对 / 类型或角色写错时必须**报错并跳过**，
 *       不能悄悄给出一张空效果牌。</li>
 * </ol>
 */
public final class CardRegistryCheck {

    /** 内置的试点 JSON（{@code data/stc/stc/card/<id>.json}）—— 必须与代码内置卡完全一致。 */
    private static final List<String> PILOTS = List.of("strike", "defend", "bash", "cleave", "iron_wave",
            "shrug_it_off", "poisoned_stab", "quick_slash", "inflame", "thunderclap");

    /** 示例卡：**只存在于 JSON** 的新卡（生成牌，不进任何卡池）。 */
    private static final String DEMO = "datapack_demo";

    /** 卡牌 JSON 的目录（注册表键 {@code stc:card} → {@code data/<ns>/stc/card/}）。 */
    private static final Path CARD_DIR = Path.of("src", "main", "resources", "data", "stc", "stc", "card");

    private CardRegistryCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 注册表键与目录约定
        check(lines, counters, "注册表键是 stc:card",
                "stc:card".equals(StcCardRegistry.CARDS.location().toString()), "stc:card",
                StcCardRegistry.CARDS.location());

        Set<String> files = new LinkedHashSet<>();
        try (Stream<Path> stream = Files.list(CARD_DIR)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> files.add(path.getFileName().toString().replace(".json", "")));
        } catch (IOException e) {
            check(lines, counters, "卡牌 JSON 目录可读：" + CARD_DIR, false, "可读", e.getClass().getSimpleName());
        }
        Set<String> expected = new LinkedHashSet<>(PILOTS);
        expected.add(DEMO);
        check(lines, counters, "卡牌 JSON 目录里的文件与我们登记的一致（新增文件别忘了加进测试）",
                files.equals(expected), expected, files);

        // 全部 JSON 只解析一次，后面各段共用
        Map<String, CardDefinition> definitions = new LinkedHashMap<>();
        for (String id : expected) {
            CardDefinition definition = readDefinition(id, lines, counters);
            if (definition != null) {
                definitions.put(id, definition);
            }
        }

        int builtinCount = CardLibrary.all().size();

        // ---------------------------------------------------------- 试点 JSON：逐字段 + 各打一场
        for (String id : PILOTS) {
            CardDefinition definition = definitions.get(id);
            Card builtin = CardLibrary.byId(id);
            if (definition == null || builtin == null) {
                continue;
            }
            check(lines, counters, id + "：试点 JSON 与内置卡的字段逐项一致（基础版）",
                    sameBaseFields(definition, builtin), describe(builtin), describe(definition));
            Card upgraded = CardLibrary.upgraded(id);
            check(lines, counters, id + "：试点 JSON 与内置卡的字段逐项一致（强化版）",
                    upgraded != null && sameUpgradeFields(definition, upgraded), describe(upgraded),
                    definition.upgrade().map(Object::toString).orElse("（没有 upgrade 段）"));

            List<String> errors = new ArrayList<>();
            CardLibrary.ExternalCard external = definition.toExternal(id, errors);
            check(lines, counters, id + "：解析到引擎定义没有报错", external != null && errors.isEmpty(),
                    "无错误", errors);
            if (external == null) {
                continue;
            }
            check(lines, counters, id + "：JSON 版打出来的结果与内置版一致",
                    fingerprint(external.base()).equals(fingerprint(builtin)), fingerprint(builtin),
                    fingerprint(external.base()));
            if (upgraded != null && external.upgraded() != null) {
                check(lines, counters, id + "：强化版打出来的结果也一致",
                        fingerprint(external.upgraded()).equals(fingerprint(upgraded)),
                        fingerprint(upgraded), fingerprint(external.upgraded()));
            }
        }

        // ---------------------------------------------------------- 示例卡：只存在于 JSON 的新卡
        CardDefinition demo = definitions.get(DEMO);
        if (demo != null) {
            List<String> errors = new ArrayList<>();
            CardLibrary.ExternalCard external = demo.toExternal(DEMO, errors);
            check(lines, counters, "示例卡能解析成引擎定义", external != null && errors.isEmpty(), "无错误", errors);
            if (external != null) {
                CardLibrary.applyExternal(List.of(external));
                check(lines, counters, "示例卡进了卡池", CardLibrary.byId(DEMO) != null, DEMO,
                        CardLibrary.byId(DEMO));
                check(lines, counters, "示例卡被登记成**生成牌**", CardLibrary.isGenerated(DEMO), true,
                        CardLibrary.isGenerated(DEMO));
                check(lines, counters, "示例卡不进任何角色的奖励池",
                        CardLibrary.rewardPool(CardLibrary.IRONCLAD).stream()
                                .noneMatch(card -> card.id().equals(DEMO))
                                && CardLibrary.rewardPool(CardLibrary.SILENT).stream()
                                        .noneMatch(card -> card.id().equals(DEMO)),
                        "不在池子里", "混进了奖励池");
                check(lines, counters, "CardIds 认得数据包提供的卡",
                        CardIds.isKnown(ResourceLocation.fromNamespaceAndPath("stc", DEMO)), true, false);
                check(lines, counters, "示例卡打得出 7 点伤害（200 → 193）",
                        fingerprint(external.base()).startsWith("193/"), "193/…", fingerprint(external.base()));
            }
        }

        // ---------------------------------------------------------- 整体应用：卡池不残缺
        List<CardLibrary.ExternalCard> all = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        for (String id : expected) {
            CardDefinition definition = definitions.get(id);
            if (definition == null) {
                continue;
            }
            CardLibrary.ExternalCard external = definition.toExternal(id, allErrors);
            if (external != null) {
                all.add(external);
            }
        }
        check(lines, counters, "全部内置 JSON 一起解析都没有报错", allErrors.isEmpty(), "无错误", allErrors);
        CardLibrary.applyExternal(all);
        check(lines, counters, "整体应用后卡池张数 = 内置 + 示例卡",
                CardLibrary.all().size() == builtinCount + 1, builtinCount + 1, CardLibrary.all().size());
        check(lines, counters, "整体应用后打击的强化版仍是 9 点（JSON 镜像没改坏内置卡）",
                CardLibrary.resolve("strike", 1).description().contains("9 点伤害"), true,
                CardLibrary.resolve("strike", 1).description());
        check(lines, counters, "整体应用后数据包卡计数 = " + expected.size(),
                CardLibrary.externalCount() == expected.size(), expected.size(), CardLibrary.externalCount());
        check(lines, counters, "覆盖层里每张卡都标着「数据包提供」",
                CardLibrary.externalIds().containsAll(expected), expected, CardLibrary.externalIds());
        CardLibrary.resetExternal();
        check(lines, counters, "还原后卡池回到内置张数", CardLibrary.all().size() == builtinCount, builtinCount,
                CardLibrary.all().size());
        check(lines, counters, "还原后不再认识示例卡",
                !CardIds.isKnown(ResourceLocation.fromNamespaceAndPath("stc", DEMO)), false, true);
        check(lines, counters, "还原后打击回到内置的 6 点",
                CardLibrary.resolve("strike", 0).description().contains("6 点伤害"), true,
                CardLibrary.resolve("strike", 0).description());

        // ---------------------------------------------------------- 报错路径
        List<String> errors = new ArrayList<>();
        check(lines, counters, "行为 id 写错 → 这张卡被拒绝（不静默生效）",
                definition("坏卡", "attack", "enemy", "common", Optional.empty(),
                        List.of(new CardDefinition.Behavior("no_such", List.of(), List.of())))
                        .toExternal("broken", errors) == null && !errors.isEmpty(),
                "有错误", errors);
        List<String> paramErrors = new ArrayList<>();
        check(lines, counters, "行为参数个数不对 → 这张卡被拒绝",
                definition("参数少", "attack", "enemy", "common", Optional.empty(),
                        List.of(new CardDefinition.Behavior("damage", List.of(), List.of())))
                        .toExternal("wrong", paramErrors) == null && !paramErrors.isEmpty(),
                "有错误", paramErrors);
        List<String> typeErrors = new ArrayList<>();
        check(lines, counters, "type 写错 → 这张卡被拒绝",
                definition("坏类型", "spell", "enemy", "common", Optional.empty(), List.of())
                        .toExternal("badtype", typeErrors) == null && !typeErrors.isEmpty(),
                "有错误", typeErrors);
        List<String> targetErrors = new ArrayList<>();
        check(lines, counters, "target 写错 → 这张卡被拒绝",
                definition("坏目标", "attack", "nobody", "common", Optional.empty(), List.of())
                        .toExternal("badtarget", targetErrors) == null && !targetErrors.isEmpty(),
                "有错误", targetErrors);
        List<String> charErrors = new ArrayList<>();
        check(lines, counters, "character 写错 → 这张卡被拒绝",
                definition("坏角色", "attack", "enemy", "common", Optional.of("stc:nobody"), List.of())
                        .toExternal("badchar", charErrors) == null && !charErrors.isEmpty(),
                "有错误", charErrors);

        // ---------------------------------------------------------- 行为表覆盖
        boolean behaviorsKnown = true;
        for (CardDefinition definition : definitions.values()) {
            for (CardDefinition.Behavior behavior : definition.effects()) {
                if (CardBehaviors.byId(behavior.type()) == null) {
                    behaviorsKnown = false;
                    check(lines, counters, "行为在册：" + behavior.type(), false, "在册", "缺失");
                }
            }
            Optional<CardDefinition.Upgrade> upgrade = definition.upgrade();
            if (upgrade.isPresent() && upgrade.get().effects().isPresent()) {
                for (CardDefinition.Behavior behavior : upgrade.get().effects().get()) {
                    if (CardBehaviors.byId(behavior.type()) == null) {
                        behaviorsKnown = false;
                        check(lines, counters, "行为在册（强化版）：" + behavior.type(), false, "在册", "缺失");
                    }
                }
            }
        }
        check(lines, counters, "试点 JSON 用到的行为都在行为表里", behaviorsKnown, true, false);
        check(lines, counters, "行为表至少有 15 条（够组一张普通卡）", CardBehaviors.count() >= 15, ">= 15",
                CardBehaviors.count());

        // ---------------------------------------------------------- 文档不会过期
        String doc = readFile(Path.of("docs", "card-registry.md"), lines, counters);
        if (doc != null) {
            boolean documented = true;
            for (String id : CardBehaviors.ids()) {
                if (!doc.contains("`" + id + "`")) {
                    documented = false;
                    check(lines, counters, "行为写进了 docs/card-registry.md：" + id, false, "有", "缺");
                }
            }
            check(lines, counters, "行为表里的每一条都写进了 docs/card-registry.md", documented, true, false);
            check(lines, counters, "文档写清了 JSON 的存放目录约定",
                    doc.contains("data/<你的数据包命名空间>/stc/card/"), true, false);
        }

        // ---------------------------------------------------------- 源码级接线
        String main = readSource("SlayTheCraft.java", lines, counters);
        if (main != null) {
            check(lines, counters, "主类在 mod 总线上登记了数据包注册表",
                    main.contains("StcCardRegistry::registerDatapackRegistry"), "出现", "缺失");
        }
        String loader = readSource("cardreg/CardRegistryLoader.java", lines, counters);
        if (loader != null) {
            check(lines, counters, "加载器挂在 ServerStartedEvent + OnDatapackSyncEvent 上（/reload 也会重灌）",
                    loader.contains("ServerStartedEvent") && loader.contains("OnDatapackSyncEvent"), "出现",
                    "缺失");
            check(lines, counters, "加载器把定义灌进引擎（CardLibrary.applyExternal）",
                    loader.contains("CardLibrary.applyExternal"), "出现", "缺失");
        }
        String registry = readSource("cardreg/StcCardRegistry.java", lines, counters);
        if (registry != null) {
            check(lines, counters, "注册表带网络 codec（同步给客户端）",
                    registry.contains("dataPackRegistry(CARDS, CardDefinition.CODEC, CardDefinition.CODEC)"),
                    "出现", "缺失");
        }
        String client = readSource("client/StcClientSetup.java", lines, counters);
        if (client != null) {
            check(lines, counters, "客户端把同步下来的注册表灌进本地卡池",
                    client.contains("syncCardRegistry(minecraft)")
                            && client.contains("CardRegistryLoader.apply"),
                    "出现", "缺失");
            check(lines, counters, "客户端断开连接时还原卡池（别把上个世界的数据包卡带出去）",
                    client.contains("CardLibrary.resetExternal()"), "出现", "缺失");
        }
        String ids = readSource("data/CardIds.java", lines, counters);
        if (ids != null) {
            check(lines, counters, "CardIds 的 S2 TODO 已经收口（说明数据包走覆盖层）",
                    !ids.contains("TODO(S2)") && ids.contains("applyExternal"), "已收口", "还留着 TODO(S2)");
        }

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    /** 造一张最小定义（报错路径用）。 */
    private static CardDefinition definition(String name, String type, String target, String rarity,
                                             Optional<String> character, List<CardDefinition.Behavior> effects) {
        return new CardDefinition(name, type, 1, target, "描述", rarity, character, List.of(), effects,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), List.of());
    }

    /** 读一个 JSON 并解码；失败会记一条断言。 */
    private static CardDefinition readDefinition(String id, List<String> lines, int[] counters) {
        Path file = CARD_DIR.resolve(id + ".json");
        if (!Files.isRegularFile(file)) {
            check(lines, counters, "卡牌 JSON 存在：" + file, false, "存在", "缺失");
            return null;
        }
        JsonElement json;
        try {
            json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            check(lines, counters, "卡牌 JSON 可读：" + file, false, "可读", e.getClass().getSimpleName());
            return null;
        }
        DataResult<CardDefinition> decoded = CardDefinition.CODEC.parse(JsonOps.INSTANCE, json);
        if (decoded.error().isPresent()) {
            check(lines, counters, id + "：能被 CardDefinition.CODEC 解析", false, "解析成功",
                    decoded.error().get().message());
            return null;
        }
        check(lines, counters, id + "：能被 CardDefinition.CODEC 解析", true, "解析成功", "解析成功");
        return decoded.result().orElse(null);
    }

    /** 基础版：JSON 定义与代码内置卡逐字段一致。 */
    private static boolean sameBaseFields(CardDefinition definition, Card card) {
        if (card == null) {
            return false;
        }
        boolean characterOk = definition.character().orElse(null) == null
                ? card.character() == null
                : definition.character().orElse("").equals(card.character());
        return definition.name().equals(card.name())
                && definition.cost() == card.cost()
                && definition.description().equals(card.description())
                && definition.type().equalsIgnoreCase(card.type().name())
                && definition.target().equalsIgnoreCase(card.target().name())
                && definition.rarity().equalsIgnoreCase(card.rarity().name())
                && characterOk
                && definition.normalizedKeywords().equals(normalizedKeywords(card));
    }

    /** 强化版：{@code upgrade} 段（缺省项按"继承基础版"算）与代码内置强化版一致。 */
    private static boolean sameUpgradeFields(CardDefinition definition, Card upgraded) {
        CardDefinition.Upgrade up = definition.upgrade().orElse(null);
        if (up == null) {
            return false;
        }
        if (!up.name().orElse(definition.name() + "+").equals(upgraded.name())) {
            return false;
        }
        if (up.cost().orElse(definition.cost()) != upgraded.cost()) {
            return false;
        }
        if (!up.description().orElse(definition.description()).equals(upgraded.description())) {
            return false;
        }
        List<String> keywords = up.keywords().orElse(definition.keywords());
        List<String> normalized = new ArrayList<>();
        for (String keyword : keywords) {
            normalized.add(keyword.toLowerCase(Locale.ROOT));
        }
        normalized.sort(String::compareTo);
        return normalized.equals(normalizedKeywords(upgraded));
    }

    private static List<String> normalizedKeywords(Card card) {
        List<String> out = new ArrayList<>();
        for (CardKeyword keyword : card.keywords()) {
            out.add(keyword.name().toLowerCase(Locale.ROOT));
        }
        out.sort(String::compareTo);
        return out;
    }

    private static String describe(Card card) {
        if (card == null) {
            return "（没有这张卡）";
        }
        return card.name() + " | " + card.cost() + " | " + card.description() + " | " + card.character();
    }

    private static String describe(CardDefinition definition) {
        return definition.name() + " | " + definition.cost() + " | " + definition.description() + " | "
                + definition.character().orElse("（无色）");
    }

    /**
     * 打一场的"结果指纹"：把这张牌打出去之后，双方关键数值拼成一行 —— 用来比对 JSON 版与内置版
     * 是不是真的同一个效果（字段一样但效果写错的情况只有这里能抓到）。
     */
    private static String fingerprint(Card card) {
        TestPlayer player = new TestPlayer("玩家", 60);
        Enemy enemy = new Enemy("僵尸", 200, Enemy.cycle(Intent.unknown()));
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            deck.add(card);
        }
        CombatContext ctx = new CombatContext(player, List.of(enemy), deck, CombatRules.defaults(), 20260101L,
                CombatLog.toList(new ArrayList<>()));
        ctx.start();
        ctx.gainEnergy(player, 90);
        PlayerState state = ctx.stateOf(player);
        CombatContext.PlayResult result = ctx.playCard(0, 0);
        if (!result.success()) {
            return "打不出去：" + result.message();
        }
        return enemy.hp() + "/" + player.block() + "/" + player.strength() + "/" + player.dexterity() + "/"
                + enemy.vulnerable() + "/" + enemy.poison() + "/" + enemy.weak() + "/" + state.hand().size()
                + "/" + state.discardPile().size();
    }

    /** 自检用的最简玩家。 */
    private static final class TestPlayer extends Combatant {
        private TestPlayer(String name, int hp) {
            super(name, hp);
        }
    }

    private static String readSource(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
        return readFile(path, lines, counters);
    }

    private static String readFile(Path path, List<String> lines, int[] counters) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, "文件可读：" + path, false, "可读", e.getClass().getSimpleName());
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
