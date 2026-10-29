package com.slaythecraft.question;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.DeckBuilder;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 问号事件的规则自检（v0.0.2 V11）。
 *
 * <p>事件效果只依赖玩家档案，所以这里可以真的"选一遍每个分支"，逐条比对档案发生了什么：
 * 加了几张卡、加了多少钱、强化的卡数值有没有变、药水/遗物有没有进档案。
 * 界面的观感与 Shift+右键的交互仍需游戏内验收（无头覆盖不到）。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.question.QuestionCheck
 * </pre>
 */
public final class QuestionCheck {

    /** 需求点名的 6 种村民职业 + 受惊的小猪仔 + 山羊 / 流浪商人 + v0.4.6 的两个新事件。 */
    private static final List<String> REQUIRED_IDS = List.of(
            "armorer", "weaponsmith", "nitwit", "librarian", "butcher", "cleric", "startled_piglet",
            "goat", "strange_merchant", "mushroom_island", "smoked_beehive");

    private static final List<String> REQUIRED_PROFESSIONS = List.of(
            "minecraft:armorer", "minecraft:weaponsmith", "minecraft:nitwit",
            "minecraft:librarian", "minecraft:butcher", "minecraft:cleric");

    private QuestionCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---- 事件表结构
        check(lines, counters, "事件表有 11 个事件（6 村民 + 小猪仔 + 山羊 + 流浪商人 + 蘑菇岛 + 烟熏蜜蜂）",
                QuestionLibrary.all().size() == REQUIRED_IDS.size(), REQUIRED_IDS.size(),
                QuestionLibrary.all().size());
        Set<String> ids = new LinkedHashSet<>();
        boolean wellFormed = true;
        for (QuestionEvent event : QuestionLibrary.all()) {
            ids.add(event.id());
            wellFormed &= !event.name().isBlank() && event.optionCount() >= 2 && event.optionCount() <= 3;
            for (QuestionOption option : event.options()) {
                wellFormed &= !option.label().isBlank() && !option.description().isBlank()
                        && (option.effect() != null || option.choose() != null);
                if (option.needsCard()) {
                    wellFormed &= option.choose() != null && !option.pickHint().isBlank();
                }
            }
        }
        check(lines, counters, "每个事件都有 2–3 个带标签与说明的分支", wellFormed, true, false);
        check(lines, counters, "事件 id 不重复", ids.size() == QuestionLibrary.all().size(), true, ids.size());
        for (String id : REQUIRED_IDS) {
            check(lines, counters, "事件在册：" + id, QuestionLibrary.byId(id) != null, true,
                    QuestionLibrary.byId(id) != null);
        }
        check(lines, counters, "未知事件返回 null", QuestionLibrary.byId("no_such_event") == null, null,
                QuestionLibrary.byId("no_such_event"));
        check(lines, counters, "越界的选项返回 null", QuestionLibrary.ARMORER.option(99) == null, null,
                QuestionLibrary.ARMORER.option(99));

        for (String profession : REQUIRED_PROFESSIONS) {
            check(lines, counters, "职业映射：" + profession,
                    QuestionLibrary.byProfession(profession) != null, true,
                    QuestionLibrary.byProfession(profession) != null);
        }
        check(lines, counters, "没有事件的职业返回 null（普通村民不受影响）",
                QuestionLibrary.byProfession("minecraft:none") == null, null,
                QuestionLibrary.byProfession("minecraft:none"));

        // ---- v0.5.0：三个事件按需求重写（描述 + 选项 + 结果文案）
        check(lines, counters, "盔甲商有事件描述（两段）",
                QuestionLibrary.ARMORER.description().size() == 2
                        && QuestionLibrary.ARMORER.description().get(0).contains("辛苦打铁的村民"),
                true, QuestionLibrary.ARMORER.description().size());
        check(lines, counters, "盔甲商选项 = 打理护甲 / 交废料",
                QuestionLibrary.ARMORER.option(0).label().equals("给他护甲用来打理")
                        && QuestionLibrary.ARMORER.option(1).label().equals("给他不需要的装备"),
                true, QuestionLibrary.ARMORER.option(0).label() + " / "
                        + QuestionLibrary.ARMORER.option(1).label());
        check(lines, counters, "武器匠有事件描述并点名制箭师",
                QuestionLibrary.WEAPONSMITH.description().get(0).contains("制箭师"),
                true, QuestionLibrary.WEAPONSMITH.description().size());
        check(lines, counters, "武器匠选项 = 传递武器 / 问他箭卖不卖",
                QuestionLibrary.WEAPONSMITH.option(0).label().equals("传递武器")
                        && QuestionLibrary.WEAPONSMITH.option(1).label().equals("问他箭卖不卖"),
                true, QuestionLibrary.WEAPONSMITH.option(0).label() + " / "
                        + QuestionLibrary.WEAPONSMITH.option(1).label());
        check(lines, counters, "傻子村民有事件描述并提到绿袍村民",
                QuestionLibrary.NITWIT.description().get(0).contains("绿袍村民"),
                true, QuestionLibrary.NITWIT.description().size());
        check(lines, counters, "傻子村民选项 = 给他吃的 / 问问他有没有吃的",
                QuestionLibrary.NITWIT.option(0).label().equals("给他吃的")
                        && QuestionLibrary.NITWIT.option(1).label().equals("问问他有没有吃的"),
                true, QuestionLibrary.NITWIT.option(0).label() + " / "
                        + QuestionLibrary.NITWIT.option(1).label());

        // ---- 盔甲商「给他护甲用来打理」：花 30 金币随机升级两张技能牌
        PlayerCardData polish = new PlayerCardData();
        polish.addGold(100);
        polish.addCard(CardInstance.of(id("defend")));
        polish.addCard(CardInstance.of(id("defend")));
        polish.addCard(CardInstance.of(id("defend")));
        polish.addCard(CardInstance.of(id("strike")));
        String polishResult = applyEffect(QuestionLibrary.ARMORER, 0, polish);
        check(lines, counters, "盔甲商「打理」花掉 30 金币", polish.gold() == 70, 70, polish.gold());
        check(lines, counters, "盔甲商「打理」随机强化两张技能牌",
                countUpgraded(polish, "defend") == 2, 2, countUpgraded(polish, "defend"));
        check(lines, counters, "盔甲商「打理」不会顺手强化攻击牌",
                countUpgraded(polish, "strike") == 0, 0, countUpgraded(polish, "strike"));
        check(lines, counters, "盔甲商「打理」的结果文案是需求给的那句",
                polishResult.contains("光亮整洁，就像新的一样。你觉得这是个不错的交易。"), true,
                polishResult);

        PlayerCardData poorData = new PlayerCardData();
        poorData.addCard(CardInstance.of(id("defend")));
        String poorResult = applyEffect(QuestionLibrary.ARMORER, 0, poorData);
        check(lines, counters, "金币不够时不会强化也不会扣钱",
                poorData.gold() == 0 && countUpgraded(poorData, "defend") == 0 && poorResult.contains("金币不够"),
                true, poorResult);

        // ---- 盔甲商 / 傻子村民的「选一张牌移除」
        PlayerCardData scrap = new PlayerCardData();
        scrap.addCard(CardInstance.of(id("strike")));
        scrap.addCard(CardInstance.of(id("defend")));
        String scrapResult = applyCardPick(QuestionLibrary.ARMORER, 1, scrap, 0);
        check(lines, counters, "盔甲商「交废料」按选中的下标删牌",
                scrap.cardCount() == 1 && scrap.cards().get(0).id().getPath().equals("defend"), "剩 1 张格挡",
                scrap.cardCount() + " 张");
        check(lines, counters, "盔甲商「交废料」的结果文案是需求给的那句",
                scrapResult.contains("那个盔甲匠皱了皱眉") && scrapResult.contains("炉子"), true, scrapResult);

        PlayerCardData food = new PlayerCardData();
        food.addCard(CardInstance.of(id("strike")));
        food.addCard(CardInstance.of(id("defend")));
        applyCardPick(QuestionLibrary.NITWIT, 0, food, 1);
        check(lines, counters, "傻子村民「给他吃的」删掉选中的那张",
                food.cardCount() == 1 && food.cards().get(0).id().getPath().equals("strike"), "剩 1 张打击",
                food.cardCount() + " 张");

        // ---- 武器匠「传递武器」：只能选攻击牌
        PlayerCardData whet = new PlayerCardData();
        whet.addCard(CardInstance.of(id("strike")));
        whet.addCard(CardInstance.of(id("defend")));
        String whetResult = applyCardPick(QuestionLibrary.WEAPONSMITH, 0, whet, 0);
        check(lines, counters, "武器匠「传递武器」强化选中的攻击牌",
                countUpgraded(whet, "strike") == 1 && countUpgraded(whet, "defend") == 0, 1,
                countUpgraded(whet, "strike"));
        check(lines, counters, "武器匠的结果文案是需求给的那句（含市面流通的吐槽）",
                whetResult.contains("笑嘻嘻地") && whetResult.contains("市面流通"), true, whetResult);
        PlayerCardData filterData = new PlayerCardData();
        filterData.addCard(CardInstance.of(id("strike")));
        filterData.addCard(CardInstance.of(id("defend")));
        check(lines, counters, "武器匠的选牌条件只放攻击牌",
                QuestionCards.matches(QuestionOption.CardPick.UPGRADE_ATTACK, filterData, 0)
                        && !QuestionCards.matches(QuestionOption.CardPick.UPGRADE_ATTACK, filterData, 1), true,
                false);
        check(lines, counters, "已强化过的攻击牌不能重复选（选牌页会把它过滤掉）",
                !QuestionCards.matches(QuestionOption.CardPick.UPGRADE_ATTACK, whet, 0)
                        && QuestionCards.selectable(QuestionOption.CardPick.UPGRADE_ATTACK, whet).isEmpty(),
                true, false);
        check(lines, counters, "交废料的选项什么牌都能选",
                QuestionCards.matches(QuestionOption.CardPick.REMOVE_ANY, filterData, 1)
                        && QuestionCards.selectable(QuestionOption.CardPick.REMOVE_ANY, filterData).size() == 2,
                true, false);

        // ---- 武器匠「问他箭卖不卖」：给一张箭雨
        PlayerCardData arrows = new PlayerCardData();
        String arrowResult = applyEffect(QuestionLibrary.WEAPONSMITH, 1, arrows);
        check(lines, counters, "武器匠「问他箭卖不卖」给 1 张箭雨", countCards(arrows, "arrow_rain") == 1, 1,
                countCards(arrows, "arrow_rain"));
        check(lines, counters, "买箭的结果文案是需求给的那句",
                arrowResult.contains("他把那一大把箭全部塞给了你，并祝你好运"), true, arrowResult);

        // ---- 傻子村民「问问他有没有吃的」：给一张蛋糕
        PlayerCardData cakeGift = new PlayerCardData();
        String cakeResult = applyEffect(QuestionLibrary.NITWIT, 1, cakeGift);
        check(lines, counters, "傻子村民「问问他有没有吃的」给 1 张蛋糕", countCards(cakeGift, "cake") == 1, 1,
                countCards(cakeGift, "cake"));
        check(lines, counters, "讨蛋糕的结果文案是需求给的那句",
                cakeResult.contains("悄咪咪") && cakeResult.contains("滑稽的动作跑开了"), true, cakeResult);

        // ---- 逐条验收其它分支效果（老事件保持原样）
        PlayerCardData data = new PlayerCardData();
        data.addCard(CardInstance.of(id("strike")));
        data.addCard(CardInstance.of(id("strike")));
        data.addCard(CardInstance.of(id("defend")));
        int before = data.cardCount();

        PlayerCardData enchantData = new PlayerCardData();
        enchantData.addCard(CardInstance.of(id("strike")));
        apply(QuestionLibrary.LIBRARIAN, 0, enchantData);
        check(lines, counters, "图书管理员「借阅手稿」给打击附魔",
                enchantData.cards().get(0).enchantments().size() == 1, 1,
                enchantData.cards().get(0).enchantments().size());

        PlayerCardData potionData = new PlayerCardData();
        apply(QuestionLibrary.LIBRARIAN, 1, potionData);
        check(lines, counters, "图书管理员「换一瓶药水」+1 瓶", potionData.potionCount() == 1, 1,
                potionData.potionCount());
        check(lines, counters, "随机药水能被药水表解析",
                PotionLibrary.byId(potionData.potionAt(0)) != null, true,
                PotionLibrary.byId(potionData.potionAt(0)));

        PlayerCardData fullPotion = new PlayerCardData();
        for (int i = 0; i < com.slaythecraft.Config.potionSlots(); i++) {
            fullPotion.addPotion("heal_potion", com.slaythecraft.Config.potionSlots());
        }
        apply(QuestionLibrary.LIBRARIAN, 1, fullPotion);
        check(lines, counters, "药水槽位满时不会塞进第 4 瓶",
                fullPotion.potionCount() == com.slaythecraft.Config.potionSlots(),
                com.slaythecraft.Config.potionSlots(), fullPotion.potionCount());

        PlayerCardData meatData = new PlayerCardData();
        meatData.setLife(meatData.life().damage(40));
        int meatBefore = meatData.hp();
        apply(QuestionLibrary.BUTCHER, 0, meatData);
        check(lines, counters, "屠夫「来点肉」回上限 30%（22 点）", meatData.hp() == meatBefore + 22,
                meatBefore + 22, meatData.hp());

        PlayerCardData cakeData = new PlayerCardData();
        apply(QuestionLibrary.BUTCHER, 1, cakeData);
        check(lines, counters, "屠夫「拿两块蛋糕」+2 张蛋糕", countCards(cakeData, "cake") == 2, 2,
                countCards(cakeData, "cake"));

        // v0.4.6：牧师给药水改成"从 5 瓶里随机抽"
        PlayerCardData poisonData = new PlayerCardData();
        apply(QuestionLibrary.CLERIC, 0, poisonData);
        String clericPotion = poisonData.potionAt(0);
        boolean inPool = false;
        for (com.slaythecraft.combat.core.Potion potion : com.slaythecraft.combat.core.PotionLibrary.clericPool()) {
            inPool |= potion.id().equals(clericPotion);
        }
        check(lines, counters, "牧师「求一瓶药水」给的是【治疗/再生/虚弱/力量/奶桶】之一", inPool, true,
                clericPotion);

        PlayerCardData prayData = new PlayerCardData();
        prayData.addCard(CardInstance.of(id("strike")));
        prayData.addCard(CardInstance.of(id("strike")));
        apply(QuestionLibrary.CLERIC, 1, prayData);
        check(lines, counters, "牧师「做一次祈祷」免费删掉一张打击", countCards(prayData, "strike") == 1, 1,
                countCards(prayData, "strike"));
        check(lines, counters, "免费删卡不扣金币", prayData.gold() == 0, 0, prayData.gold());
        check(lines, counters, "删卡统计也计入（牌组信息一致）", prayData.stats().cardsDeleted() == 1, 1,
                prayData.stats().cardsDeleted());

        // v0.4.6：遗物【受惊的小猪仔】改成只能从屠夫事件获得，小猪仔事件改成给一瓶随机药水
        PlayerCardData pigletData = new PlayerCardData();
        apply(QuestionLibrary.STARTLED_PIGLET, 0, pigletData);
        check(lines, counters, "小猪仔「安抚它」给一瓶随机药水（不再给遗物）",
                pigletData.potionCount() == 1 && !pigletData.hasRelic("startled_piglet"), "1 瓶药水 / 无遗物",
                pigletData.potionCount() + " 瓶 / " + pigletData.relicCount() + " 遗物");
        check(lines, counters, "该遗物在遗物表里且效果是「第一次失去生命抽 3 张」（v0.2.8）",
                RelicLibrary.byId("startled_piglet") != null
                        && RelicLibrary.byId("startled_piglet").description().contains("抽 3 张牌")
                        && RelicLibrary.byId("startled_piglet")
                                .firesOn(com.slaythecraft.combat.core.RelicTrigger.FIRST_HP_LOST),
                true, false);
        // v0.4.6：屠夫事件是遗物【受惊的小猪仔】的唯一来源，重复领取不会叠加
        PlayerCardData butcherData = new PlayerCardData();
        apply(QuestionLibrary.BUTCHER, 2, butcherData);
        check(lines, counters, "屠夫「问问他那只受惊的小猪仔」给遗物",
                butcherData.hasRelic("startled_piglet"), true, butcherData.relicCount());
        apply(QuestionLibrary.BUTCHER, 2, butcherData);
        check(lines, counters, "同一件遗物不会重复获得", butcherData.relicCount() == 1, 1, butcherData.relicCount());

        // v0.4.6：两个新事件的选项效果
        PlayerCardData mushroomData = new PlayerCardData();
        applyEffect(QuestionLibrary.MUSHROOM_ISLAND, 0, mushroomData);
        check(lines, counters, "蘑菇岛「屠宰嚒菇牛」给【残忍】+【嚒咕菲力】",
                mushroomData.hasRelic("mogu_fillet") && countCards(mushroomData, "curse_cruel") == 1,
                "1 张残忍 / 有嚒咕菲力", countCards(mushroomData, "curse_cruel") + " 张 / "
                        + mushroomData.relicCount() + " 遗物");
        check(lines, counters, "【嚒咕菲力】让最大生命值 +14", mushroomData.maxHp() == 75 + 14, 89,
                mushroomData.maxHp());
        PlayerCardData stewData = new PlayerCardData();
        applyEffect(QuestionLibrary.MUSHROOM_ISLAND, 1, stewData);
        check(lines, counters, "蘑菇岛「摘蘑菇」给蘑菇煲药水",
                "mushroom_stew_potion".equals(stewData.potionAt(0)), "mushroom_stew_potion", stewData.potionAt(0));

        PlayerCardData beeData = new PlayerCardData();
        int beeBefore = beeData.hp();
        applyEffect(QuestionLibrary.SMOKED_BEEHIVE, 0, beeData);
        check(lines, counters, "烟熏蜜蜂「击落蜂巢」失去 8 点生命并获得【蜂蜜佐料】",
                beeData.hp() == beeBefore - 8 && beeData.hasRelic("honey_seasoning"), (beeBefore - 8) + " / 有佐料",
                beeData.hp() + " / " + beeData.relicCount() + " 遗物");
        PlayerCardData smokeData = new PlayerCardData();
        smokeData.setLife(smokeData.life().damage(30));
        int smokeBefore = smokeData.hp();
        applyEffect(QuestionLibrary.SMOKED_BEEHIVE, 1, smokeData);
        check(lines, counters, "烟熏蜜蜂「点燃篝火」回复 15 点生命", smokeData.hp() == smokeBefore + 15,
                smokeBefore + 15, smokeData.hp());

        PlayerCardData leaveData = new PlayerCardData();
        leaveData.setLife(leaveData.life().damage(20));
        int leaveBefore = leaveData.hp();
        apply(QuestionLibrary.STARTLED_PIGLET, 1, leaveData);
        check(lines, counters, "小猪仔「轻轻放过」回 10 点", leaveData.hp() == leaveBefore + 10, leaveBefore + 10,
                leaveData.hp());

        // ---- 强化机制的边界
        PlayerCardData once = new PlayerCardData();
        once.addCard(CardInstance.of(id("strike")));
        check(lines, counters, "upgradeFirstCard 成功一次", once.upgradeFirstCard(id("strike")), true, true);
        check(lines, counters, "同一张卡不会二次强化", !once.upgradeFirstCard(id("strike")), false, true);
        check(lines, counters, "档案里没有那张卡时强化失败",
                !once.upgradeFirstCard(id("defend")), false, true);
        check(lines, counters, "没有强化版的卡不能强化",
                !new PlayerCardData().upgradeFirstCard(id("strike")) || !CardLibrary.canUpgrade("no_such"), true,
                CardLibrary.canUpgrade("no_such"));

        // ---- v0.5.4.1（A4-20）【灼热打击】：档案层的"可以强化任意次"
        PlayerCardData searing = new PlayerCardData();
        searing.addCard(CardInstance.of(id("searing_blow")));
        int searingUpgrades = 0;
        for (int i = 0; i < 5; i++) {
            if (searing.upgradeFirstCard(id("searing_blow"))) {
                searingUpgrades++;
            }
        }
        check(lines, counters, "档案层：灼热打击能连续强化 5 次（不像普通卡只能 1 次）",
                searingUpgrades == 5, 5, searingUpgrades);
        check(lines, counters, "档案层：灼热打击强化后仍是「可以继续强化」",
                searing.canUpgradeAt(0), true, false);
        check(lines, counters, "档案层：灼热打击的档位解析成 +5（伤害 32）",
                CardLibrary.resolve("searing_blow", searing.cards().get(0).upgrades())
                        .description().contains("32 点伤害"),
                true, CardLibrary.resolve("searing_blow", searing.cards().get(0).upgrades()).description());
        check(lines, counters, "档案层：普通卡升到 +1 之后就不能再升",
                once.canUpgradeAt(0) == false, false, once.canUpgradeAt(0));

        // ---- 随机来源稳定：同一 seed 结果一致（服务器只用档案 + Random，不依赖客户端）
        PlayerCardData seededA = new PlayerCardData();
        PlayerCardData seededB = new PlayerCardData();
        QuestionLibrary.LIBRARIAN.option(1).effect().apply(seededA, new Random(42L));
        QuestionLibrary.LIBRARIAN.option(1).effect().apply(seededB, new Random(42L));
        check(lines, counters, "同 seed 的随机药水结果一致",
                seededA.potionAt(0).equals(seededB.potionAt(0)), seededA.potionAt(0), seededB.potionAt(0));

        // ---- v0.4.1：两个新事件（山羊 / 流浪商人）
        check(lines, counters, "山羊事件在册且有描述",
                QuestionLibrary.byId("goat") != null
                        && QuestionLibrary.GOAT.description().get(0).contains("山羊总是强大的攀岩者"),
                true, QuestionLibrary.GOAT.description().size());
        check(lines, counters, "山羊事件的两个选项标签符合需求",
                QuestionLibrary.GOAT.option(0).label().equals("砍下山羊的一只角")
                        && QuestionLibrary.GOAT.option(1).label().equals("放山羊走"),
                true, QuestionLibrary.GOAT.option(0).label() + " / " + QuestionLibrary.GOAT.option(1).label());
        PlayerCardData hornData = new PlayerCardData();
        String hornResult = applyEffect(QuestionLibrary.GOAT, 0, hornData);
        check(lines, counters, "砍下山羊的一只角 → 获得袭击号角",
                hornData.hasRelic("raid_horn"), true, hornData.relics());
        check(lines, counters, "砍角的结果文案是需求给的那句",
                hornResult.contains("你拿出剑砍下了山羊的角") && hornResult.contains("竞争配偶"), true,
                hornResult);

        PlayerCardData freeGoat = new PlayerCardData();
        int freeHpBefore = freeGoat.hp();
        String freeResult = applyEffect(QuestionLibrary.GOAT, 1, freeGoat);
        check(lines, counters, "放山羊走 → +50 金币",
                freeGoat.gold() == 50, 50, freeGoat.gold());
        check(lines, counters, "放山羊走 → 失去 6 点生命值",
                freeGoat.hp() == freeHpBefore - 6, freeHpBefore - 6, freeGoat.hp());
        check(lines, counters, "放走的结果文案是需求给的那句",
                freeResult.contains("跟着山羊的脚印走") && freeResult.contains("钱袋子"), true, freeResult);

        check(lines, counters, "奇怪的商人在册且提到羊驼与蓝袍",
                QuestionLibrary.STRANGE_MERCHANT != null
                        && QuestionLibrary.STRANGE_MERCHANT.description().get(0).contains("蓝袍")
                        && QuestionLibrary.STRANGE_MERCHANT.description().get(0).contains("羊驼"),
                true, QuestionLibrary.STRANGE_MERCHANT.description().size());
        check(lines, counters, "奇怪的商人有三个选项",
                QuestionLibrary.STRANGE_MERCHANT.optionCount() == 3, 3,
                QuestionLibrary.STRANGE_MERCHANT.optionCount());

        PlayerCardData coralData = new PlayerCardData();
        coralData.addGold(500);
        String coralResult = applyEffect(QuestionLibrary.STRANGE_MERCHANT, 0, coralData);
        check(lines, counters, "买失活的珊瑚：花 200 金币并拿到遗物",
                coralData.gold() == 300 && coralData.hasRelic("dead_coral"), "300 + 珊瑚",
                coralData.gold() + " / " + coralData.relics());
        check(lines, counters, "珊瑚的结果文案是需求给的那句",
                coralResult.contains("皱巴巴的失活珊瑚") && coralResult.contains("好运"), true, coralResult);

        PlayerCardData heartData = new PlayerCardData();
        heartData.addGold(500);
        String heartResult = applyEffect(QuestionLibrary.STRANGE_MERCHANT, 1, heartData);
        check(lines, counters, "买海洋之心：花 150 金币并拿到遗物",
                heartData.gold() == 350 && heartData.hasRelic("heart_of_the_sea"), "350 + 海洋之心",
                heartData.gold() + " / " + heartData.relics());
        check(lines, counters, "海洋之心的结果文案是需求给的那句",
                heartResult.contains("闪闪发光的海洋之心") && heartResult.contains("收藏品"), true, heartResult);

        PlayerCardData poorBuyer = new PlayerCardData();
        poorBuyer.addGold(10);
        String cannotAfford = applyEffect(QuestionLibrary.STRANGE_MERCHANT, 0, poorBuyer);
        check(lines, counters, "金币不够时买不到（也不扣钱）",
                !poorBuyer.hasRelic("dead_coral") && poorBuyer.gold() == 10
                        && cannotAfford.contains("金币不够"), true, cannotAfford);

        PlayerCardData refuseData = new PlayerCardData();
        int refuseHpBefore = refuseData.hp();
        String refuseResult = applyEffect(QuestionLibrary.STRANGE_MERCHANT, 2, refuseData);
        check(lines, counters, "都不想要 → 失去 3 点生命",
                refuseData.hp() == refuseHpBefore - 3, refuseHpBefore - 3, refuseData.hp());
        check(lines, counters, "拒绝的结果文案是需求给的那句",
                refuseResult.contains("恼羞成怒") && refuseResult.contains("骑着羊驼快速逃跑"), true, refuseResult);

        // ---- 实体 id → 事件（山羊 / 流浪商人）
        check(lines, counters, "山羊 → 山羊事件",
                QuestionLibrary.byEntityType("minecraft:goat") == QuestionLibrary.GOAT, true,
                QuestionLibrary.byEntityType("minecraft:goat"));
        check(lines, counters, "流浪商人 → 奇怪的商人",
                QuestionLibrary.byEntityType("minecraft:wandering_trader") == QuestionLibrary.STRANGE_MERCHANT,
                true, QuestionLibrary.byEntityType("minecraft:wandering_trader"));
        check(lines, counters, "其它实体没有事件",
                QuestionLibrary.byEntityType("minecraft:cow") == null, null,
                QuestionLibrary.byEntityType("minecraft:cow"));

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static void apply(QuestionEvent event, int optionIndex, PlayerCardData data) {
        applyEffect(event, optionIndex, data);
    }

    /** 结算"不需要选牌"的选项，返回结果文案。 */
    private static String applyEffect(QuestionEvent event, int optionIndex, PlayerCardData data) {
        QuestionOption option = event.option(optionIndex);
        if (option == null || option.effect() == null) {
            throw new IllegalStateException("选项 " + optionIndex + " 不是普通选项：" + event.id());
        }
        return option.effect().apply(data, new Random(7L));
    }

    /** 结算"需要先选一张牌"的选项，返回结果文案。 */
    private static String applyCardPick(QuestionEvent event, int optionIndex, PlayerCardData data, int cardIndex) {
        QuestionOption option = event.option(optionIndex);
        if (option == null || option.choose() == null) {
            throw new IllegalStateException("选项 " + optionIndex + " 不需要选牌：" + event.id());
        }
        if (!QuestionCards.matches(option.pick(), data, cardIndex)) {
            throw new IllegalStateException("下标 " + cardIndex + " 不符合选项 " + optionIndex + " 的筛选条件");
        }
        return option.choose().apply(data, cardIndex);
    }

    /** 数一数某张卡有几份是强化过的（v0.5.0 的"随机升级两张技能牌"用它验收）。 */
    private static int countUpgraded(PlayerCardData data, String cardId) {
        int count = 0;
        for (CardInstance instance : data.cards()) {
            if (instance.id().getPath().equals(cardId) && instance.upgraded()) {
                count++;
            }
        }
        return count;
    }

    private static int countCards(PlayerCardData data, String cardId) {
        int count = 0;
        for (CardInstance instance : data.cards()) {
            if (instance.id().getPath().equals(cardId)) {
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
