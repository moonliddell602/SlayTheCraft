package com.slaythecraft.character;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.DeckBuilder;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 角色与卡组的无头校验：三个角色的配置是否自洽、起始牌组能不能真的变成战斗牌组、
 * 以及"切换角色不重复发牌"这条防刷规则。
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.character.CharacterServiceCheck
 * </pre>
 */
public final class CharacterServiceCheck {

    /** 篝火界面（v0.0.3 W5）用到的全部翻译键。 */
    private static final List<String> CAMPFIRE_KEYS = List.of(
            "stc.campfire.title", "stc.campfire.hp", "stc.campfire.uses", "stc.campfire.ready",
            "stc.campfire.waiting", "stc.campfire.heal", "stc.campfire.forge", "stc.campfire.bandage",
            "stc.campfire.hint", "stc.campfire.forge.hint", "stc.campfire.bandage.title",
            "stc.campfire.bandage.none",
            // v0.5.2.5：本层次数用完时替换 hint 的那句
            "stc.campfire.exhausted");

    private CharacterServiceCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 角色表自洽
        List<CharacterProfile> all = CharacterRegistry.all();
        check(lines, counters, "共有 4 个可选角色（v0.0.3 W3 加了叛逃掠夺者）", all.size() == 4, 4, all.size());
        Set<ResourceLocation> ids = new HashSet<>();
        boolean distinct = true;
        boolean sane = true;
        boolean decksKnown = true;
        for (CharacterProfile profile : all) {
            distinct &= ids.add(profile.id());
            sane &= profile.maxHp() > 0 && profile.energyPerTurn() >= 1 && !profile.startingDeck().isEmpty();
            for (ResourceLocation cardId : profile.startingDeck()) {
                decksKnown &= CardIds.isKnown(cardId);
            }
        }
        check(lines, counters, "角色 id 不重复", distinct, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "角色参数都有效（生命/能量/起始牌组非空）", sane, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "起始牌组里的卡牌 id 都能被识别", decksKnown, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "indexOf/ byIndex 一致",
                CharacterRegistry.indexOf(all.get(1).id()) == 1
                        && CharacterRegistry.byIndex(1).id().equals(all.get(1).id()),
                Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "未知 id 返回 null", CharacterRegistry.byId(id("nobody")) == null,
                Boolean.TRUE, Boolean.FALSE);

        // 起始牌组里的每张卡：要么是公共卡，要么正好归属这个角色（防止角色 id 字符串写错）
        boolean ownershipOk = true;
        for (CharacterProfile profile : all) {
            for (ResourceLocation cardId : profile.startingDeck()) {
                Card card = com.slaythecraft.combat.core.CardLibrary.byId(cardId.getPath());
                if (card == null) {
                    ownershipOk = false;
                    continue;
                }
                if (card.character() != null && !card.character().equals(profile.id().toString())) {
                    ownershipOk = false;
                }
            }
        }
        check(lines, counters, "起始牌组的卡要么公共、要么归属该角色", ownershipOk, Boolean.TRUE, Boolean.FALSE);

        // ---------------------------------------------------------- 初始遗物与描述（v0.0.3 W3）
        boolean relicsResolve = true;
        for (CharacterProfile profile : all) {
            if (!profile.hasStartingRelic()) {
                relicsResolve = false;
                continue;
            }
            var relic = profile.startingRelicDefinition();
            relicsResolve &= relic != null && relic.id().equals(profile.startingRelic());
        }
        check(lines, counters, "每个角色的初始遗物都能解析成真实遗物", relicsResolve, Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "战士的初始遗物是燃烧之血",
                "burning_blood".equals(CharacterRegistry.IRONCLAD.startingRelic()), "burning_blood",
                CharacterRegistry.IRONCLAD.startingRelic());
        check(lines, counters, "猎手的初始遗物是蛇之戒指",
                "ring_of_the_snake".equals(CharacterRegistry.SILENT.startingRelic()), "ring_of_the_snake",
                CharacterRegistry.SILENT.startingRelic());
        check(lines, counters, "机器人的初始遗物是破损核心",
                "cracked_core".equals(CharacterRegistry.DEFECT.startingRelic()), "cracked_core",
                CharacterRegistry.DEFECT.startingRelic());
        check(lines, counters, "叛逃掠夺者的初始遗物是臂弩",
                "crossbow".equals(CharacterRegistry.PILLAGER.startingRelic()), "crossbow",
                CharacterRegistry.PILLAGER.startingRelic());
        check(lines, counters, "每个角色都有独立的翻译键前缀",
                all.stream().map(CharacterProfile::nameKey).distinct().count() == all.size(),
                all.size(), all.stream().map(CharacterProfile::nameKey).distinct().count());

        // 名字与描述走语言文件（角色详情页直接读 key），所以这里直接查两个语言文件
        Set<String> zhKeys = langKeys("zh_cn");
        Set<String> enKeys = langKeys("en_us");
        check(lines, counters, "语言文件可读（zh_cn / en_us）", !zhKeys.isEmpty() && !enKeys.isEmpty(),
                "两个文件都有内容", zhKeys.size() + " / " + enKeys.size());
        boolean described = true;
        for (CharacterProfile profile : all) {
            described &= zhKeys.contains(profile.nameKey()) && zhKeys.contains(profile.descriptionKey())
                    && enKeys.contains(profile.nameKey()) && enKeys.contains(profile.descriptionKey());
        }
        check(lines, counters, "四个角色的名字与描述都在语言文件里（详情页不会显示原始 key）", described,
                Boolean.TRUE, Boolean.FALSE);
        boolean campfireLang = CAMPFIRE_KEYS.stream().allMatch(zhKeys::contains)
                && CAMPFIRE_KEYS.stream().allMatch(enKeys::contains);
        check(lines, counters, "篝火界面的 " + CAMPFIRE_KEYS.size() + " 条文案中英都在语言文件里", campfireLang,
                Boolean.TRUE, CAMPFIRE_KEYS.stream().filter(key -> !zhKeys.contains(key)).toList());

        // ---- v0.5.2.5 篝火三个 bug 的源码级检查（这几个都是"只在游戏里才看得出来"的行为）
        String campfireService = readSource("campfire/CampfireService.java", lines, counters);
        if (campfireService != null) {
            check(lines, counters, "选完立刻关掉界面（closeContainer）—— 修「能一直用」",
                    campfireService.contains("player.closeContainer();"), "出现", "缺失");
            check(lines, counters, "动手前再查一次门槛（usableNow）—— 修「次数用完还能继续用」",
                    campfireService.contains("private static String usableNow(ServerPlayer player)")
                            && campfireService.contains("blockReason(player)"),
                    "出现", "缺失");
            check(lines, counters, "三个选项都走 usableNow",
                    campfireService.indexOf("usableNow(player)") >= 0
                            && campfireService.split("usableNow\\(player\\)", -1).length == 4,
                    "3 处（愈合 / 包扎 / 锻造）", campfireService.split("usableNow\\(player\\)", -1).length - 1);
        }
        String campfireSession = readSource("campfire/CampfireSession.java", lines, counters);
        if (campfireSession != null) {
            check(lines, counters, "已经选过的人退出时不再记一次账（修双重计数）",
                    campfireSession.contains("if (chosen.contains(player.getUUID())) {"), "出现", "缺失");
        }
        String campfireScreen = readSource("client/CampfireScreen.java", lines, counters);
        if (campfireScreen != null) {
            check(lines, counters, "底部说明按宽度折行（修「描述文字显示不全」）",
                    campfireScreen.contains("TextLayout.wrapRanges") && campfireScreen.contains("drawWrapped"),
                    "出现", "缺失");
            check(lines, counters, "次数用完时按钮置灰并显示「已用完」",
                    campfireScreen.contains("usedCount() < capCount()")
                            && campfireScreen.contains("stc.campfire.exhausted"),
                    "出现", "缺失");
        }

        // ---- v0.5.3.0 藏宝箱事件
        String treasure = readSource("treasure/TreasureChestEvents.java", lines, counters);
        if (treasure == null) {
            check(lines, counters, "藏宝箱事件类存在（treasure/TreasureChestEvents.java）", false, "存在", "缺失");
        } else {
            check(lines, counters, "藏宝箱触发条件：主手任意镐 + 副手收纳包",
                    treasure.contains("ItemTags.PICKAXES") && treasure.contains("CARD_BINDER.get()"),
                    "出现", "缺失");
            check(lines, counters, "藏宝箱目标方块是陷阱箱",
                    treasure.contains("Blocks.TRAPPED_CHEST"), "出现", "缺失");
            check(lines, counters, "藏宝箱不能连用（走 EventKind.TREASURE 的 lastEvent 判定）",
                    treasure.contains("isConsecutive(EventKind.TREASURE)"), "出现", "缺失");
            check(lines, counters, "藏宝箱奖励从 chestPool 抽（排除「只能商店」的两件）",
                    treasure.contains("RelicLibrary.chestPool()"), "出现", "缺失");
            check(lines, counters, "藏宝箱也要走「获得遗物」的通用结算",
                    treasure.contains("RelicObtainService.apply("), "出现", "缺失");
            // v0.5.5.1（用户）：藏宝箱要计入事件统计、每层上限 5 次
            check(lines, counters, "藏宝箱开箱前先查本层次数上限（v0.5.5.1：上限 5）",
                    treasure.contains("canTrigger(EventKind.TREASURE, caps)"), "出现", "缺失");
        }
        String eventKind = readSource("combat/core/EventKind.java", lines, counters);
        if (eventKind != null) {
            // v0.5.5.1（用户）："事件回合统计没有添加有关藏宝箱的内容（包括目前完成进度，上限 5 次…）"
            // → 藏宝箱从"不计数"改为"计数且上限 5"，所以它不再是 capped() 的例外
            check(lines, counters, "EventKind 里有 TREASURE，且它现在**计数**（只有 BOSS 是例外）",
                    eventKind.contains("TREASURE(\"藏宝箱\")") && eventKind.contains("return this != BOSS;"),
                    "出现", "缺失");
            check(lines, counters, "藏宝箱不被排除在上限之外（旧的 `this != TREASURE` 已去掉）",
                    !eventKind.contains("this != BOSS && this != TREASURE"), "已去掉", "还在");
        }
        String progress = readSource("combat/core/ProgressState.java", lines, counters);
        if (progress != null) {
            check(lines, counters, "事件统计的分类表里有藏宝箱（CAPPED_KINDS）",
                    progress.contains("EventKind.TREASURE"), "出现", "缺失");
        }
        String caps = readSource("combat/core/EventCaps.java", lines, counters);
        if (caps != null) {
            check(lines, counters, "EventCaps 有藏宝箱这一档（默认 5）",
                    caps.contains("int treasure") && caps.contains("new EventCaps(15, 5, 5, 5, 5)"),
                    "出现", "缺失");
        }
        String triggerPage = readSource("client/BinderProgressScreen.java", lines, counters);
        if (triggerPage != null) {
            check(lines, counters, "事件触发方式页有第 5 条（藏宝箱）",
                    triggerPage.contains("stc.binder.progress.trigger.treasure"), "出现", "缺失");
        }
        // ---- v0.5.3.0 商店：只能商店的龙蛋 + 打折；v0.5.3.1/.2 逐件定价 + 来源通道
        String shopOffers = readSource("merchant/CardShopOffers.java", lines, counters);
        if (shopOffers != null) {
            check(lines, counters, "商店用 shopPool（含「只能商店」的龙蛋）",
                    shopOffers.contains("RelicLibrary.shopPool()"), "出现", "缺失");
            check(lines, counters, "行商羊驼披肩：价格 -25%（向下取整、最低 1）",
                    shopOffers.contains("base * 3 / 4") && shopOffers.contains("trader_llama_shawl"),
                    "出现", "缺失");
        }
        String shopMenu = readSource("merchant/CardShopMenu.java", lines, counters);
        if (shopMenu != null) {
            check(lines, counters, "商店买遗物用**逐件单价**（hasPrice / priceOf），没有单价的才退回配置统一价",
                    shopMenu.contains("RelicLibrary.hasPrice(relicId)")
                            && shopMenu.contains("RelicLibrary.priceOf(relicId)")
                            && shopMenu.contains("Config.shopPriceRelic()"),
                    "出现", "缺失");
            check(lines, counters, "买遗物后先 grantRelic 再跑「获得时」结算（RelicObtainService）",
                    shopMenu.contains("data.grantRelic(relicId)")
                            && shopMenu.contains("RelicObtainService.apply(serverPlayer, relicId)"),
                    "出现", "缺失");
        }
        String relicLib = readSource("combat/core/RelicLibrary.java", lines, counters);
        if (relicLib != null) {
            check(lines, counters, "遗物表登记了八件新遗物",
                    relicLib.contains("\"dirty_glass\"") && relicLib.contains("\"worn_flint\"")
                            && relicLib.contains("\"trader_llama_shawl\"")
                            && relicLib.contains("\"cartography_table\"")
                            && relicLib.contains("\"unusual_shulker_box\"")
                            && relicLib.contains("\"dragon_egg\"") && relicLib.contains("\"soul_campfire\"")
                            && relicLib.contains("\"glow_ink_sac\""),
                    "8 件都在", "有缺失");
            check(lines, counters, "「只能商店」的【龙蛋】不进藏宝箱池、也不进战斗掉落",
                    relicLib.contains("SHOP_ONLY_RELIC_IDS") && relicLib.contains("chestDroppable(")
                            && relicLib.contains("eliteDroppable("),
                    "出现", "缺失");
            check(lines, counters, "v0.5.3.1/.2 逐件定价表（PRICES / priceOf / hasPrice / pricedCount）",
                    relicLib.contains("private static final Map<String, Integer> PRICES")
                            && relicLib.contains("public static int priceOf(")
                            && relicLib.contains("public static boolean hasPrice(")
                            && relicLib.contains("public static int pricedCount()"),
                    "出现", "缺失");
            check(lines, counters, "【行商羊驼披肩】【制图台】走「藏宝箱 / 商店」、不进战斗掉落",
                    relicLib.contains("CHEST_AND_SHOP_ONLY_RELIC_IDS")
                            && relicLib.contains("isChestAndShopOnly(")
                            && relicLib.contains("trader_llama_shawl")
                            && relicLib.contains("cartography_table"),
                    "出现", "缺失");
        }
        String rewardService = readSource("reward/RewardService.java", lines, counters);
        if (rewardService != null) {
            check(lines, counters, "常规战斗的 10% 遗物掉落改用 eliteDroppable（不会掉出「只能商店」的）",
                    rewardService.contains("RelicLibrary.eliteDroppable("), "出现", "缺失");
        }
        String obtain = readSource("relic/RelicObtainService.java", lines, counters);
        if (obtain != null) {
            check(lines, counters, "获得时效果服务：破旧打火石附魔 + 制图台 5 次卡牌奖励",
                    obtain.contains("worn_flint") && obtain.contains("cartography_table")
                            && obtain.contains("addPendingRewards(5)"),
                    "出现", "缺失");
        }
        if (campfireService != null) {
            check(lines, counters, "灵魂篝火：同一次访问可以多选，但只记一次数",
                    campfireService.contains("hasSoulCampfire(player)")
                            && campfireService.contains("markCounted(player)"),
                    "出现", "缺失");
        }

        // 叛逃掠夺者：75 生命 / 3 费 / 12 张起始牌组（打击×4 防御×4 + 4 张专属牌）
        CharacterProfile pillager = CharacterRegistry.PILLAGER;
        check(lines, counters, "叛逃掠夺者 75 生命", pillager.maxHp() == 75, 75, pillager.maxHp());
        check(lines, counters, "叛逃掠夺者每回合 3 点能量", pillager.energyPerTurn() == 3, 3,
                pillager.energyPerTurn());
        check(lines, counters, "叛逃掠夺者起始牌组 12 张", pillager.startingDeck().size() == 12, 12,
                pillager.startingDeck().size());
        check(lines, counters, "叛逃掠夺者牌组 = 打击×4 + 防御×4 + 集中射击/寻找掩体/翻滚/烟雾弹",
                countId(pillager, "strike") == 4 && countId(pillager, "defend") == 4
                        && countId(pillager, "aimed_shot") == 1 && countId(pillager, "take_cover") == 1
                        && countId(pillager, "roll") == 1 && countId(pillager, "smoke_bomb") == 1,
                "4/4/1/1/1/1", countId(pillager, "strike") + "/" + countId(pillager, "defend") + "/"
                        + countId(pillager, "aimed_shot") + "/" + countId(pillager, "take_cover") + "/"
                        + countId(pillager, "roll") + "/" + countId(pillager, "smoke_bomb"));
        check(lines, counters, "叛逃掠夺者的专属牌都归属它自己",
                pillager.startingDeck().stream()
                        .map(cardId -> com.slaythecraft.combat.core.CardLibrary.byId(cardId.getPath()))
                        .filter(card -> card != null && card.character() != null)
                        .allMatch(card -> card.character().equals(pillager.id().toString())),
                Boolean.TRUE, Boolean.FALSE);

        // ---------------------------------------------------------- DeckBuilder
        List<CardInstance> mixed = new ArrayList<>();
        mixed.add(CardInstance.of(id("strike")));
        mixed.add(CardInstance.of(id("strike")));
        mixed.add(CardInstance.of(id("defend")));
        mixed.add(CardInstance.of(id("does_not_exist")));
        mixed.add(CardInstance.of(ResourceLocation.fromNamespaceAndPath("othermod", "strike")));
        DeckBuilder.Result built = DeckBuilder.fromArchive(mixed);
        check(lines, counters, "可识别的卡进入牌组", built.deck().size() == 3, 3, built.deck().size());
        check(lines, counters, "重复卡保留两份", countStrikes(built.deck()) == 2, 2, countStrikes(built.deck()));
        check(lines, counters, "未知 id 被跳过并记录", built.unknown().size() == 2, 2, built.unknown().size());
        check(lines, counters, "空档案得到空牌组",
                DeckBuilder.fromArchive(List.of()).deck().isEmpty(), Boolean.TRUE, Boolean.FALSE);

        // ---------------------------------------------------------- 角色选择 / 切换
        PlayerCardData data = new PlayerCardData();
        CharacterProfile ironclad = CharacterRegistry.IRONCLAD;
        CharacterProfile silent = CharacterRegistry.SILENT;
        CharacterProfile defect = CharacterRegistry.DEFECT;

        check(lines, counters, "首次选择角色返回 STARTED",
                CharacterService.select(data, ironclad) == CharacterService.Result.STARTED,
                CharacterService.Result.STARTED, "?");
        check(lines, counters, "首次选择写入当前角色", ironclad.id().equals(data.character()),
                ironclad.id(), data.character());
        check(lines, counters, "首次选择发放起始牌组",
                data.cardCount() == ironclad.startingDeck().size(), ironclad.startingDeck().size(), data.cardCount());
        int afterFirst = data.cardCount();

        check(lines, counters, "重复点同一角色返回 ALREADY_ACTIVE",
                CharacterService.select(data, ironclad) == CharacterService.Result.ALREADY_ACTIVE,
                CharacterService.Result.ALREADY_ACTIVE, "?");
        check(lines, counters, "重复点同一角色不会重复发牌", data.cardCount() == afterFirst, afterFirst, data.cardCount());

        check(lines, counters, "本局进行中：换角色返回 LOCKED（测试反馈第 2 条）",
                CharacterService.select(data, silent) == CharacterService.Result.LOCKED,
                CharacterService.Result.LOCKED, "?");
        check(lines, counters, "本局没结束之前换角色会被锁住，且不会重复发牌",
                data.cardCount() == afterFirst, afterFirst, data.cardCount());
        check(lines, counters, "被锁时当前角色不变", ironclad.id().equals(data.character()), ironclad.id(),
                data.character());

        // 战败 = 本局结束 → 可以换角色（开新的一局）
        data.markRunDefeated();
        check(lines, counters, "战败即本局结束", data.isRunOver(), Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "战败后换角色返回 RESTARTED",
                CharacterService.select(data, silent) == CharacterService.Result.RESTARTED,
                CharacterService.Result.RESTARTED, "?");
        check(lines, counters, "新一局只发一份起始牌组（不再叠加）",
                data.cardCount() == silent.startingDeck().size(), silent.startingDeck().size(), data.cardCount());
        check(lines, counters, "换角色后当前角色正确", silent.id().equals(data.character()), silent.id(),
                data.character());
        int afterSwitch = data.cardCount();

        check(lines, counters, "重复点同一角色返回 ALREADY_ACTIVE",
                CharacterService.select(data, silent) == CharacterService.Result.ALREADY_ACTIVE,
                CharacterService.Result.ALREADY_ACTIVE, "?");
        check(lines, counters, "重复点同一角色不会重复发牌", data.cardCount() == afterSwitch, afterSwitch,
                data.cardCount());

        // 通关 = 本局结束 → 也可以换角色
        PlayerCardData cleared = new PlayerCardData();
        CharacterService.select(cleared, ironclad);
        cleared.setProgress(cleared.progress().recordBossDefeat().recordBossDefeat().recordBossDefeat());
        check(lines, counters, "通关后本局结束", cleared.isRunOver() && cleared.progress().completed(),
                Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "通关后可以换角色（开新局）",
                CharacterService.select(cleared, defect) == CharacterService.Result.RESTARTED,
                CharacterService.Result.RESTARTED, "?");
        check(lines, counters, "通关后换角色也是重置牌组",
                cleared.cardCount() == defect.startingDeck().size(), defect.startingDeck().size(),
                cleared.cardCount());

        // ---- 重开本局（测试反馈：加一个重开按钮）：清本局资源，但保留金币
        PlayerCardData restartData = new PlayerCardData();
        CharacterService.select(restartData, ironclad);
        restartData.addGold(321);
        restartData.addRelic("vajra");
        restartData.addPotion("heal_potion", 3);
        restartData.recordKill(id("zombie"));
        restartData.addPendingRewards(2);
        restartData.damageHp(40);
        restartData.recordEvent(com.slaythecraft.combat.core.EventKind.NORMAL,
                com.slaythecraft.combat.core.EventCaps.DEFAULT);
        restartData.recordBossDefeat();
        RunService.resetForNewRun(restartData, ironclad);
        check(lines, counters, "重开：牌组回到该角色的起始牌组",
                restartData.cardCount() == ironclad.startingDeck().size(), ironclad.startingDeck().size(),
                restartData.cardCount());
        check(lines, counters, "重开：遗物只剩初始遗物（v0.0.3 起重开也发初始遗物）",
                restartData.relicCount() == 1 && restartData.hasRelic(ironclad.startingRelic()), 1,
                restartData.relicCount() + " 件：" + restartData.hasRelic(ironclad.startingRelic()));
        check(lines, counters, "重开：药水清空", restartData.potionCount() == 0, 0, restartData.potionCount());
        check(lines, counters, "重开：待领奖励清空", restartData.pendingRewards() == 0, 0,
                restartData.pendingRewards());
        check(lines, counters, "重开：击杀计数清空", restartData.killCount(id("zombie")) == 0, 0,
                restartData.killCount(id("zombie")));
        check(lines, counters, "重开：生命值回满", restartData.life().isFull(), Boolean.TRUE,
                restartData.life().describe());
        check(lines, counters, "重开：回到第 1 层且计数清零",
                restartData.floor() == 1
                        && restartData.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL) == 0,
                "第 1 层 / 0", restartData.floor() + " / "
                        + restartData.progress().countOf(com.slaythecraft.combat.core.EventKind.NORMAL));
        check(lines, counters, "重开：本局重新开始（可以再战）", !restartData.isRunOver(), Boolean.TRUE,
                restartData.isRunOver());
        check(lines, counters, "重开：金币重置为初始金币（v0.4.6.3 起不再保留）",
                restartData.gold() == com.slaythecraft.Config.startingGold(),
                com.slaythecraft.Config.startingGold(), restartData.gold());

        // ---------------------------------------------------------- v0.4.0：重开 = 重新选角色
        PlayerCardData reselect = new PlayerCardData();
        CharacterService.select(reselect, ironclad);
        reselect.addGold(500);
        reselect.addRelic("vajra");
        reselect.addCard(CardInstance.of(id("bash")));
        reselect.addPotion("heal_potion", 3);
        reselect.recordEvent(com.slaythecraft.combat.core.EventKind.NORMAL,
                com.slaythecraft.combat.core.EventCaps.DEFAULT);
        // 需求："系统清空当前玩家所扮演的角色，玩家重新选择角色"
        RunService.clearRun(reselect);
        check(lines, counters, "重开（清空）：角色被清空", reselect.character() == null, null,
                reselect.character());
        check(lines, counters, "重开（清空）：牌组 / 遗物 / 药水 / 进度都清空",
                reselect.cardCount() == 0 && reselect.relicCount() == 0 && reselect.potionCount() == 0
                        && reselect.floor() == 1,
                "0/0/0/1", reselect.cardCount() + "/" + reselect.relicCount() + "/" + reselect.potionCount()
                        + "/" + reselect.floor());
        check(lines, counters, "重开（清空）：金币重置为初始金币（v0.4.6.3 起不再保留）",
                reselect.gold() == com.slaythecraft.Config.startingGold(),
                com.slaythecraft.Config.startingGold(), reselect.gold());
        check(lines, counters, "重开（清空）：生命值回满", reselect.life().isFull(), Boolean.TRUE,
                reselect.life().describe());
        // 清空之后可以**换一个角色**，而且算"首次开局"（发新角色的牌组与初始遗物）
        check(lines, counters, "清空后可以换角色（不再被 LOCKED 挡住）",
                CharacterService.select(reselect, CharacterRegistry.DEFECT) == CharacterService.Result.STARTED,
                CharacterService.Result.STARTED, "?");
        check(lines, counters, "清空后选新角色：拿到新角色的起始牌组",
                reselect.cardCount() == CharacterRegistry.DEFECT.startingDeck().size(),
                CharacterRegistry.DEFECT.startingDeck().size(), reselect.cardCount());
        check(lines, counters, "清空后选新角色：拿到新角色的初始遗物",
                reselect.relicCount() == 1 && reselect.hasRelic(CharacterRegistry.DEFECT.startingRelic()),
                1, reselect.relicCount() + " 件");
        check(lines, counters, "清空后选新角色：尖塔入口不再显示'当前角色'",
                !CharacterRegistry.IRONCLAD.id().equals(reselect.character()), CharacterRegistry.DEFECT.id(),
                reselect.character());

        // ---------------------------------------------------------- v0.4.0：球位按角色给
        check(lines, counters, "只有故障机器人有充能球栏位",
                CharacterRegistry.DEFECT.orbSlots() == 3 && CharacterRegistry.IRONCLAD.orbSlots() == 0
                        && CharacterRegistry.SILENT.orbSlots() == 0
                        && CharacterRegistry.PILLAGER.orbSlots() == 0,
                "3/0/0/0", CharacterRegistry.DEFECT.orbSlots() + "/" + CharacterRegistry.IRONCLAD.orbSlots()
                        + "/" + CharacterRegistry.SILENT.orbSlots() + "/"
                        + CharacterRegistry.PILLAGER.orbSlots());
        check(lines, counters, "usesOrbs 只有机器人是 true",
                CharacterRegistry.DEFECT.usesOrbs() && !CharacterRegistry.IRONCLAD.usesOrbs(),
                Boolean.TRUE, Boolean.FALSE);
        check(lines, counters, "每个角色的奖励池都只属于自己的牌（不再共用公共池）",
                all.stream().allMatch(profile -> com.slaythecraft.combat.core.CardLibrary
                        .rewardPool(profile.id().toString()).stream()
                        .allMatch(card -> com.slaythecraft.combat.core.CardLibrary.isBasic(card.id())
                                || profile.id().toString().equals(card.character()))),
                Boolean.TRUE, Boolean.FALSE);

        PlayerCardData toClear = new PlayerCardData();
        toClear.addCard(CardInstance.of(id("strike")));
        toClear.addCard(CardInstance.of(id("defend")));
        int beforeClear = toClear.cardCount();
        toClear.clearCards();
        check(lines, counters, "clearCards 清空全部卡牌（原 " + beforeClear + " 张）", toClear.cardCount() == 0,
                0, toClear.cardCount());

        // 起始牌组能被 DeckBuilder 全部翻译成真实卡牌
        PlayerCardData fresh = new PlayerCardData();
        CharacterService.select(fresh, silent);
        DeckBuilder.Result fromCharacter = DeckBuilder.fromArchive(fresh.cards());
        check(lines, counters, "角色起始牌组全部可战斗（无未知卡）", fromCharacter.unknown().isEmpty(),
                Boolean.TRUE, fromCharacter.unknown());
        check(lines, counters, "角色起始牌组张数一致",
                fromCharacter.deck().size() == silent.startingDeck().size(),
                silent.startingDeck().size(), fromCharacter.deck().size());

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static int countStrikes(List<Card> deck) {
        int count = 0;
        for (Card card : deck) {
            if ("strike".equals(card.id())) {
                count++;
            }
        }
        return count;
    }

    /** 读取某个语言文件的全部键；读不到时返回空集合（测试会因此失败，不会静默通过）。 */
    private static Set<String> langKeys(String locale) {
        java.nio.file.Path path = java.nio.file.Path.of("src/main/resources/assets/stc/lang/" + locale + ".json");
        try {
            String json = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            return new com.google.gson.JsonParser().parse(json).getAsJsonObject().keySet();
        } catch (Exception e) {
            return Set.of();
        }
    }

    /** 读取一份源码（v0.5.2.5：篝火那几个行为只能在源码层面钉住）。 */
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

    /** 某个角色的起始牌组里指定卡 id 的张数（叛逃掠夺者的牌组构成靠它验收）。 */
    private static int countId(CharacterProfile profile, String cardPath) {
        int count = 0;
        for (ResourceLocation cardId : profile.startingDeck()) {
            if (cardPath.equals(cardId.getPath())) {
                count++;
            }
        }
        return count;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
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
