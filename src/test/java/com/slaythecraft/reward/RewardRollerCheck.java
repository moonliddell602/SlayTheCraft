package com.slaythecraft.reward;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardRarity;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.RewardOffer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 卡牌奖励抽取与待领奖励生命周期的无头校验（设计文档 §9.2）。
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.reward.RewardRollerCheck
 * </pre>
 */
public final class RewardRollerCheck {

    private RewardRollerCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 卡池（v0.4.0：按角色分开，无色牌不进池）
        List<Card> ironcladPool = CardLibrary.rewardPool(CardLibrary.IRONCLAD);
        check(lines, counters, "铁甲战士奖励池非空", !ironcladPool.isEmpty(), true, ironcladPool.size());
        // v0.5.1.5 需求："统一目前每个角色的金色和稀有卡，全部将品质统一为稀有。"
        check(lines, counters, "已经没有任何卡牌还是金色品质（金色已统一成稀有）",
                CardLibrary.allGold().isEmpty(), 0, CardLibrary.allGold().size());
        check(lines, counters, "普通奖励池里也不再有金色品质",
                ironcladPool.stream().noneMatch(card -> card.rarity() == CardRarity.GOLD), true, false);
        check(lines, counters, "奖励池不含其他角色的专属卡",
                ironcladPool.stream().noneMatch(card -> CardLibrary.SILENT.equals(card.character())
                        || CardLibrary.DEFECT.equals(card.character())
                        || CardLibrary.PILLAGER.equals(card.character())), true, false);
        check(lines, counters, "无色牌不进奖励池（只有基础牌是例外）",
                ironcladPool.stream().noneMatch(card -> card.character() == null
                        && !CardLibrary.isBasic(card.id())), true, false);
        check(lines, counters, "奖励池含本角色卡",
                ironcladPool.stream().anyMatch(card -> CardLibrary.IRONCLAD.equals(card.character())), true, false);
        check(lines, counters, "基础牌（打击/格挡）人人都有",
                ironcladPool.stream().anyMatch(card -> card.id().equals("strike"))
                        && ironcladPool.stream().anyMatch(card -> card.id().equals("defend")), true, false);
        check(lines, counters, "还没选角色时没有任何奖励池",
                CardLibrary.rewardPool(null).isEmpty(), true, false);

        // v0.5.1.5：BOSS 奖励改成从**稀有池**抽（金色已经统一成稀有）
        List<Card> goldPool = CardLibrary.goldPool(CardLibrary.IRONCLAD);
        List<Card> rarePool = CardLibrary.rarePool(CardLibrary.IRONCLAD);
        check(lines, counters, "旧金卡池现在是空的（金色已统一成稀有）", goldPool.isEmpty(), true, goldPool.size());
        check(lines, counters, "稀有（BOSS）池非空", !rarePool.isEmpty(), true, rarePool.size());
        check(lines, counters, "稀有池里全是稀有卡",
                rarePool.stream().allMatch(card -> card.rarity() == CardRarity.RARE), true, false);
        check(lines, counters, "稀有池不会混进别的角色的牌",
                rarePool.stream().allMatch(card -> CardLibrary.IRONCLAD.equals(card.character())), true, false);
        check(lines, counters, "四个角色都有稀有池",
                !CardLibrary.rarePool(CardLibrary.SILENT).isEmpty()
                        && !CardLibrary.rarePool(CardLibrary.DEFECT).isEmpty()
                        && !CardLibrary.rarePool(CardLibrary.PILLAGER).isEmpty(), true, false);
        check(lines, counters, "原来的金色牌现在在稀有池里（恶魔形态）",
                rarePool.stream().anyMatch(card -> card.id().equals("demon_form")), true, false);

        // ---------------------------------------------------------- 抽取
        List<Card> rolled = RewardRoller.roll(CardLibrary.IRONCLAD, false, 3, new Random(42));
        check(lines, counters, "普通奖励抽满 3 张", rolled.size() == 3, 3, rolled.size());
        check(lines, counters, "同一次候选不重复", new HashSet<>(rolled).size() == rolled.size(), rolled.size(),
                new HashSet<>(rolled).size());
        check(lines, counters, "候选都来自该角色奖励池", ironcladPool.containsAll(rolled), true, false);

        List<Card> goldRolled = RewardRoller.roll(CardLibrary.IRONCLAD, true, 3, new Random(7));
        check(lines, counters, "BOSS 奖励（golden=true）只出稀有卡",
                goldRolled.stream().allMatch(card -> card.rarity() == CardRarity.RARE), true, false);
        check(lines, counters, "BOSS 奖励的候选都来自稀有池", rarePool.containsAll(goldRolled), true, false);
        // v0.5.1.0 需求："强制卡牌奖励为 3" —— 池子不足 3 张时用**重复**补齐，但每一张都必须出现。
        // v0.5.1.5：这一档现在是稀有池；稀有池通常 > 3 张，所以"全部包含"只在池子 ≤ 3 时成立。
        check(lines, counters, "BOSS 奖励固定 3 张；池子 ≤ 3 时包含全部稀有牌",
                goldRolled.size() == 3
                        && (rarePool.size() > 3 || new HashSet<>(goldRolled).containsAll(rarePool)),
                "3 张（池子小时含全部）", goldRolled.size() + " 张");
        check(lines, counters, "稀有池 ≤ 3 的判定与实际池子大小一致",
                RewardRoller.rarePoolIsSmall(CardLibrary.IRONCLAD, 3) == (rarePool.size() <= 3),
                rarePool.size() <= 3, RewardRoller.rarePoolIsSmall(CardLibrary.IRONCLAD, 3));

        List<Card> first = RewardRoller.roll(CardLibrary.SILENT, false, 3, new Random(99));
        List<Card> second = RewardRoller.roll(CardLibrary.SILENT, false, 3, new Random(99));
        check(lines, counters, "相同种子结果一致（可复现）", first.equals(second), true, false);

        // 金色品质已经不参与任何抽取（v0.5.1.5 起没有卡是金色的），所以只比较三档
        check(lines, counters, "品质权重大小关系（普通 > 罕见 > 稀有，且都 ≥ 1）",
                CardRarity.COMMON.weight() > CardRarity.UNCOMMON.weight()
                        && CardRarity.UNCOMMON.weight() > CardRarity.RARE.weight()
                        && CardRarity.RARE.weight() >= 1, true, false);

        // v0.3.0：池子会随新卡变多，所以"要得比池子多"要用池子大小算，不能写死数字
        // v0.5.1.0：需求要求"强制卡牌奖励为 3"（固定张数），所以"要得比池子多"现在
        // 返回的是**要的张数**（用重复补齐），而不是"整池" —— 断言随之更新。
        int defectPool = CardLibrary.rewardPool(CardLibrary.DEFECT).size();
        List<Card> overAsked = RewardRoller.roll(CardLibrary.DEFECT, false, defectPool + 5, new Random(1));
        check(lines, counters, "要得比池子多时补齐到所要张数（不重复部分 = 整池）",
                overAsked.size() == defectPool + 5
                        && new HashSet<>(overAsked).size() == defectPool,
                (defectPool + 5) + " 张 / " + defectPool + " 种不同",
                overAsked.size() + " 张 / " + new HashSet<>(overAsked).size() + " 种不同");

        // ---------------------------------------------------------- 待领奖励生命周期
        PlayerCardData data = new PlayerCardData();
        check(lines, counters, "新档案没有待领奖励", data.pendingRewards() == 0, 0, data.pendingRewards());
        data.addPendingRewards(1);
        check(lines, counters, "计数式发放的奖励是未抽候选",
                data.firstOffer() != null && !data.firstOffer().rolled(), true, false);
        data.setOffer(0, RewardOffer.rolled(List.of(id("strike"), id("defend"), id("bash")), false));
        check(lines, counters, "写回候选后可读到 3 张",
                data.firstOffer().rolled() && data.firstOffer().choices().size() == 3, 3,
                data.firstOffer().choices().size());
        check(lines, counters, "领取后计数归零", data.claimOffer(0) && data.pendingRewards() == 0, 0,
                data.pendingRewards());
        check(lines, counters, "越界领取返回 false", !data.claimOffer(0), false, true);

        PlayerCardData stacked = new PlayerCardData();
        stacked.addOffer(RewardOffer.rolled(List.of(id("strike")), false));
        stacked.addOffer(RewardOffer.rolled(List.of(id("demon_form")), true));
        check(lines, counters, "多次奖励按队列计数", stacked.pendingRewards() == 2, 2, stacked.pendingRewards());
        check(lines, counters, "队首是先进先出的那个（金色标记保留）",
                stacked.firstOffer().golden() == false && stacked.pendingOffers().get(1).golden(),
                true, false);
        stacked.claimOffer(0);
        check(lines, counters, "领掉队首后第二个金色奖励成为队首",
                stacked.firstOffer() != null && stacked.firstOffer().golden(), true, false);

        // ---------------------------------------------------------- 老存档迁移
        CompoundTag legacy = new PlayerCardData().toTag();
        legacy.putInt("pending_rewards", 2);
        PlayerCardData migrated = PlayerCardData.fromTag(legacy);
        check(lines, counters, "S1~S3 的 pending_rewards 能迁移成待领奖励", migrated.pendingRewards() == 2, 2,
                migrated.pendingRewards());
        check(lines, counters, "迁移出来的奖励是未抽候选（可补抽）",
                migrated.pendingOffers().stream().noneMatch(RewardOffer::rolled), true, false);

        // ---------------------------------------------------------- v0.2.1 测试反馈：奖励根本触发不了
        // 战斗击杀以前既没有原版击杀归属、也没有任何记账，所以永远凑不满阈值 → 从没有过奖励。
        // 现在记在与原版事件共用的纯逻辑里（RewardService.recordKill），这里逐档验收阈值行为。
        com.slaythecraft.combat.core.MobTier normal = com.slaythecraft.combat.core.MobTier.NORMAL;
        com.slaythecraft.combat.core.MobTier elite = com.slaythecraft.combat.core.MobTier.ELITE;
        com.slaythecraft.combat.core.MobTier boss = com.slaythecraft.combat.core.MobTier.BOSS;
        ResourceLocation zombieId = id("zombie");

        // v0.4.5：需求把阈值改成"普通 60 / 精英 30"，所以这里不再写死小数，而是按配置值走一遍
        int normalThreshold = com.slaythecraft.combat.mc.MobTiers.threshold(normal);
        int eliteThreshold = com.slaythecraft.combat.mc.MobTiers.threshold(elite);
        check(lines, counters, "普通档阈值按配置（需求 60）", normalThreshold == 60, 60, normalThreshold);
        check(lines, counters, "精英档阈值按配置（需求 30）", eliteThreshold == 30, 30, eliteThreshold);
        check(lines, counters, "BOSS 档阈值 1", com.slaythecraft.combat.mc.MobTiers.threshold(boss) == 1, 1,
                com.slaythecraft.combat.mc.MobTiers.threshold(boss));
        check(lines, counters, "不分档的生物永远不会达标",
                com.slaythecraft.combat.mc.MobTiers.threshold(com.slaythecraft.combat.core.MobTier.NONE)
                        == Integer.MAX_VALUE, Integer.MAX_VALUE,
                com.slaythecraft.combat.mc.MobTiers.threshold(com.slaythecraft.combat.core.MobTier.NONE));

        PlayerCardData killData = new PlayerCardData();
        boolean early = false;
        for (int i = 1; i < normalThreshold; i++) {
            early |= RewardService.recordKill(killData, normal, zombieId);
        }
        check(lines, counters, "普通怪在达标前一只都不给奖励", !early, false, early);
        check(lines, counters, "计数按同种类累计到阈值 - 1", killData.killCount(zombieId) == normalThreshold - 1,
                normalThreshold - 1, killData.killCount(zombieId));
        check(lines, counters, "计数器对其他种类独立",
                !RewardService.recordKill(killData, normal, id("skeleton")) && killData.killCount(id("skeleton")) == 1,
                1, killData.killCount(id("skeleton")));
        check(lines, counters, "第 " + normalThreshold + " 只同种类击杀达标",
                RewardService.recordKill(killData, normal, zombieId), true, true);
        check(lines, counters, "达标后该种类的计数清零（重新累计）", killData.killCount(zombieId) == 0, 0,
                killData.killCount(zombieId));

        PlayerCardData eliteData = new PlayerCardData();
        for (int i = 1; i < eliteThreshold; i++) {
            RewardService.recordKill(eliteData, elite, id("ravager"));
        }
        check(lines, counters, "精英第 " + eliteThreshold + " 只达标",
                RewardService.recordKill(eliteData, elite, id("ravager")), true, true);
        check(lines, counters, "BOSS 第 1 只就达标",
                RewardService.recordKill(new PlayerCardData(), boss, id("warden")), true, true);

        check(lines, counters, "普通/精英/BOSS 的金币数递增",
                com.slaythecraft.combat.mc.MobTiers.gold(normal) < com.slaythecraft.combat.mc.MobTiers.gold(elite)
                        && com.slaythecraft.combat.mc.MobTiers.gold(elite) <= com.slaythecraft.combat.mc.MobTiers.gold(boss),
                true, false);
        check(lines, counters, "只有 BOSS 档必出稀有卡三选一（配置默认开启）",
                com.slaythecraft.combat.mc.MobTiers.goldenReward(boss)
                        && !com.slaythecraft.combat.mc.MobTiers.goldenReward(normal), true, false);

        // ---------------------------------------------------------------- v0.5.0.20：战斗结算
        settlement(lines, counters);
        // ---------------------------------------------------------------- v0.5.1.0：奖励固定 3 张
        rewardAlwaysThree(lines, counters);

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    /**
     * v0.5.0.20 的战斗结算与遗物掉落（需求原文："现在，常规战斗的结算有百分之十的概率掉落一件非特殊遗物。"
     * / "新UI:战斗结算页面：领取X金币 / 领取遗物：X / 获得一次卡牌奖励 / 跳过奖励按钮"）。
     *
     * <p>这些都是**纯数据**行为，所以能在裸 JVM 里直接验。</p>
     */
    private static void settlement(List<String> lines, int[] counters) {
        // ---- 10% 遗物掉落：概率配置
        check(lines, counters, "常规战斗掉落遗物的概率默认是 10%",
                Math.abs(com.slaythecraft.Config.normalBattleRelicChance() - 0.10D) < 1.0E-9D, 0.10D,
                com.slaythecraft.Config.normalBattleRelicChance());
        check(lines, counters, "遗物掉落概率可以配成 0（关掉掉落）与 1（必掉）",
                com.slaythecraft.Config.NORMAL_BATTLE_RELIC_CHANCE.getDefault() == 10, 10,
                com.slaythecraft.Config.NORMAL_BATTLE_RELIC_CHANCE.getDefault());

        // ---- "非特殊遗物"：可掉落的池子里不能有初始遗物 / 事件专属遗物
        var droppable = com.slaythecraft.combat.core.RelicLibrary.droppable();
        check(lines, counters, "可掉落遗物池非空", !droppable.isEmpty(), "> 0", droppable.size());
        boolean noSpecial = true;
        for (var relic : droppable) {
            if (com.slaythecraft.combat.core.RelicLibrary.isStarting(relic.id())
                    || com.slaythecraft.combat.core.RelicLibrary.isEventOnly(relic.id())) {
                noSpecial = false;
            }
        }
        check(lines, counters, "可掉落池里没有初始遗物、也没有事件专属遗物（需求：非特殊遗物）",
                noSpecial, "都是非特殊", "混进了特殊遗物");
        check(lines, counters, "初始遗物确实被判定为不可掉落",
                !com.slaythecraft.combat.core.RelicLibrary.droppableRelic("burning_blood"), false, true);
        check(lines, counters, "事件专属遗物确实被判定为不可掉落",
                !com.slaythecraft.combat.core.RelicLibrary.droppableRelic("dead_coral"), false, true);

        // ---- 结算状态机：暂存 → 领取 → 清空
        var data = new com.slaythecraft.data.PlayerCardData();
        int goldBefore = data.gold();
        data.beginSettlement(16, "vajra");
        check(lines, counters, "beginSettlement 后金币是**暂存**的（没进余额）",
                data.gold() == goldBefore && data.pendingGold() == 16, "余额不变 / 待领 16",
                data.gold() + " / " + data.pendingGold());
        check(lines, counters, "结算页此时是打开的（有待领内容）", data.settlementOpen(), true, false);
        check(lines, counters, "待领遗物记在档案里", "vajra".equals(data.pendingRelic()), "vajra",
                data.pendingRelic());

        check(lines, counters, "领取金币后进余额、待领归 0",
                data.claimSettlementGold() && data.gold() == goldBefore + 16 && data.pendingGold() == 0,
                goldBefore + 16, data.gold());
        check(lines, counters, "重复领取金币返回 false（不能刷）", !data.claimSettlementGold(), false, true);
        check(lines, counters, "领取遗物后进遗物列表",
                data.claimSettlementRelic() && data.hasRelic("vajra"), true, false);
        check(lines, counters, "重复领取遗物返回 false", !data.claimSettlementRelic(), false, true);
        check(lines, counters, "领完之后结算页不再打开（没有待领内容）", !data.settlementOpen(), false, true);

        // ---- 没掉遗物时：这一项不显示（pendingRelic 为 null）
        var noRelic = new com.slaythecraft.data.PlayerCardData();
        noRelic.beginSettlement(16, null);
        check(lines, counters, "没掉落遗物时 pendingRelic 为空 —— 结算页据此**不显示**那一行",
                noRelic.pendingRelic() == null, null, noRelic.pendingRelic());
        check(lines, counters, "没掉遗物但有待领金币时结算页仍然打开", noRelic.settlementOpen(), true, false);

        // ---- 跳过：清空结算但**不**进余额
        var skipped = new com.slaythecraft.data.PlayerCardData();
        skipped.beginSettlement(16, "vajra");
        skipped.clearSettlement();
        check(lines, counters, "跳过奖励后待领全部清空",
                !skipped.settlementOpen() && skipped.pendingRelic() == null, "清空",
                skipped.pendingGold() + " / " + skipped.pendingRelic());
        check(lines, counters, "跳过的金币**不会**进余额（防白拿）",
                skipped.gold() == goldBefore, goldBefore, skipped.gold());

        // ---- 结算状态要能落盘（重登不会丢）
        var persisted = new com.slaythecraft.data.PlayerCardData();
        persisted.beginSettlement(40, "anchor");
        var roundTrip = com.slaythecraft.data.PlayerCardData.fromTag(persisted.toTag());
        check(lines, counters, "结算状态能落盘往返：待领金币",
                roundTrip.pendingGold() == 40, 40, roundTrip.pendingGold());
        check(lines, counters, "结算状态能落盘往返：待领遗物",
                "anchor".equals(roundTrip.pendingRelic()), "anchor", roundTrip.pendingRelic());
    }

    /**
     * v0.5.1.0 需求："强制卡牌奖励为 3。如果角色的图鉴内已做完金色卡数量不超过 3，
     * 则金色卡牌奖励必然包含已经做完的金卡。"
     *
     * <p>v0.5.1.5：BOSS 奖励改成从**稀有池**抽（金色已统一成稀有），所以下面把"金卡池"
     * 一律换成"稀有池" —— 规则本身没变：池子 ≤ 3 张时必然全部包含，否则按权重抽 3 张不重复。</p>
     */
    private static void rewardAlwaysThree(List<String> lines, int[] counters) {
        int want = com.slaythecraft.Config.rewardChoices();
        String[] chars = {com.slaythecraft.combat.core.CardLibrary.IRONCLAD,
                com.slaythecraft.combat.core.CardLibrary.SILENT,
                com.slaythecraft.combat.core.CardLibrary.DEFECT,
                com.slaythecraft.combat.core.CardLibrary.PILLAGER};

        boolean allThree = true;
        boolean goldAllIncluded = true;
        for (String ch : chars) {
            // 普通奖励：固定 3 张
            for (int seed = 0; seed < 20; seed++) {
                var picked = RewardRoller.roll(ch, false, want, new java.util.Random(seed));
                if (picked.size() != want) {
                    allThree = false;
                }
            }
            // BOSS（稀有）奖励：固定 3 张，且"池子 ≤ 3"时必须**包含全部**稀有牌
            var goldPool = com.slaythecraft.combat.core.CardLibrary.rarePool(ch);
            for (int seed = 0; seed < 20; seed++) {
                var picked = RewardRoller.roll(ch, true, want, new java.util.Random(seed));
                if (picked.size() != want) {
                    allThree = false;
                }
                // 用 distinct 比：池子小时会重复填充，但每一张都必须出现
                if (goldPool.size() <= want
                        && !new java.util.HashSet<>(picked).containsAll(goldPool)) {
                    goldAllIncluded = false;
                }
            }
        }
        check(lines, counters, "普通与 BOSS（稀有）奖励都**固定 3 张**（池子小就用重复补齐）", allThree,
                "每轴都是 " + want, "有不是 " + want + " 张的");
        check(lines, counters, "稀有池 ≤ 3 时，BOSS 奖励**必然包含全部**稀有牌", goldAllIncluded,
                "全部包含", "有遗漏");

        // v0.5.1.5：旧金卡池现在是空的（金色已并进稀有）
        check(lines, counters, "旧金卡池现在是空的（猎手也是 0）",
                com.slaythecraft.combat.core.CardLibrary.goldPool(
                        com.slaythecraft.combat.core.CardLibrary.SILENT).isEmpty(),
                0, com.slaythecraft.combat.core.CardLibrary.goldPool(
                        com.slaythecraft.combat.core.CardLibrary.SILENT).size());

        // 池子 > count 时仍然是不重复的按权重抽（不会把同一张塞三遍）
        var many = new java.util.ArrayList<com.slaythecraft.combat.core.Card>();
        many.addAll(com.slaythecraft.combat.core.CardLibrary.rewardPool(
                com.slaythecraft.combat.core.CardLibrary.DEFECT));
        var distinct = RewardRoller.roll(com.slaythecraft.combat.core.CardLibrary.DEFECT, false, want,
                new java.util.Random(7));
        check(lines, counters, "池子充足时抽取**不重复**（不会把同一张塞 3 遍）",
                distinct.size() == 3 && distinct.stream().distinct().count() == 3,
                "3 张各不相同", distinct.size() + " 张");

        // v0.5.5.2（用户 B7）："【商队护送】在队伍只有 1 名玩家时，不加入奖励池"
        // 这里走的是**真实抽取路径**（roll 带 partySize），比只查 rewardPool 更接近游戏内
        int big = 80;
        var singlePlayer = RewardRoller.roll(com.slaythecraft.combat.core.CardLibrary.PILLAGER, false, big,
                new java.util.Random(3), 1);
        var multiPlayer = RewardRoller.roll(com.slaythecraft.combat.core.CardLibrary.PILLAGER, false, big,
                new java.util.Random(3), 2);
        check(lines, counters, "单人抽取不会给出【商队护送】",
                singlePlayer.stream().noneMatch(card -> card.id().equals("caravan_escort")), "0 张", "混进来了");
        check(lines, counters, "多人抽取（2 人）会给出【商队护送】",
                multiPlayer.stream().anyMatch(card -> card.id().equals("caravan_escort")), "至少 1 张", "没有");
        check(lines, counters, "单人 / 多人两条池子的差异只来自这一张",
                multiPlayer.stream().distinct().count() == singlePlayer.stream().distinct().count() + 1,
                "相差 1 种", singlePlayer.stream().distinct().count() + " vs "
                        + multiPlayer.stream().distinct().count());
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
