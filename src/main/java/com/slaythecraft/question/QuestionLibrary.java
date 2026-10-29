package com.slaythecraft.question;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardEnchantment;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardType;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 问号事件表（v0.0.2 V11）：需求点名的 6 种村民 + 受惊的小猪仔。
 *
 * <p>每个事件给两个分支，**效果实现只动玩家档案**（加卡 / 加金币 / 加药水 / 加遗物 / 强化 / 附魔 / 回血），
 * 所以规则本身可以在无头自检里逐条验收（见 {@code question/QuestionCheck}）。</p>
 *
 * <p>选项里的"随机"都用服务端 seed 的 {@link Random}，不依赖任何 Minecraft 类型。</p>
 */
public final class QuestionLibrary {

    private static final Map<String, QuestionEvent> BY_ID = new LinkedHashMap<>();

    /** 盔甲商（v0.5.0 按需求重写）：花钱打理护甲（随机升级两张技能牌）或交出废料换删牌。 */
    public static final QuestionEvent ARMORER = register(new QuestionEvent(
            "armorer", "盔甲商",
            List.of("你看到了一个正在辛苦打铁的村民，他浑身是汗...",
                    "虽然他正处于百忙之中，不过他注意到了你。哦，他看到了你身上的护甲，"
                            + "并向你示意可以给你的护甲做做装修..."),
            List.of(
                    new QuestionOption("armorer_polish", "给他护甲用来打理",
                            "失去 30 金币，随机升级两张技能牌。",
                            (data, random) -> {
                                int cost = 30;
                                if (data.gold() < cost) {
                                    return "你的金币不够（需要 " + cost + "，当前 " + data.gold()
                                            + "），盔甲商耸了耸肩。";
                                }
                                List<Integer> skills = new ArrayList<>();
                                for (int i = 0; i < data.cardCount(); i++) {
                                    Card card = CardLibrary.byId(data.cards().get(i).id().getPath());
                                    if (card != null && card.type() == CardType.SKILL && data.canUpgradeAt(i)) {
                                        skills.add(i);
                                    }
                                }
                                if (skills.isEmpty()) {
                                    return "你的技能牌都已经强化过了，盔甲商没什么可打理的。";
                                }
                                data.spendGold(cost);
                                java.util.Collections.shuffle(skills, random);
                                int upgraded = 0;
                                for (int i = 0; i < skills.size() && upgraded < 2; i++) {
                                    if (data.upgradeCardAt(skills.get(i))) {
                                        upgraded++;
                                    }
                                }
                                return "光亮整洁，就像新的一样。你觉得这是个不错的交易。"
                                        + "（花掉 " + cost + " 金币，强化了 " + upgraded + " 张技能牌，剩余金币 "
                                        + data.gold() + "）";
                            }),
                    new QuestionOption("armorer_scrap", "给他不需要的装备",
                            "选择一张牌从牌组移除。",
                            QuestionOption.CardPick.REMOVE_ANY,
                            (data, index) -> {
                                data.removeCardAt(index);
                                return "那个盔甲匠皱了皱眉...不过也没说什么。"
                                        + "然后，他顺手把你给他的废料丢到了旁边冒着火星的炉子里，"
                                        + "或许以后这件物品会成为一件新的装备...";
                            }))));

    /** 武器匠（v0.5.0 按需求重写）：开刃（升级一张攻击牌）或买箭（获得一张【箭雨】）。 */
    public static final QuestionEvent WEAPONSMITH = register(new QuestionEvent(
            "weaponsmith", "武器匠",
            List.of("话说制箭不应该是制箭师的事情吗？这个古怪的武器匠似乎在干本属于制箭师的工作，"
                            + "而制箭师本人却不知去向。",
                    "你看了看手中的武器，在思考要不要交给他开个刃..."),
            List.of(
                    new QuestionOption("weaponsmith_whetstone", "传递武器",
                            "选择一张攻击牌，升级它。",
                            QuestionOption.CardPick.UPGRADE_ATTACK,
                            (data, index) -> {
                                data.upgradeCardAt(index);
                                return "那个武器匠二话不说，拿起你的武器就去了后房处理。"
                                        + "你在外面坐了一会，之后看他笑嘻嘻地把你的已经被他修理完毕的武器还给了你。\n"
                                        + "不过后来你听说，你的武器好像出现了同款并且在市面流通...";
                            }),
                    new QuestionOption("weaponsmith_arrow_rain", "问他箭卖不卖",
                            "获得一张【箭雨】。",
                            (data, random) -> {
                                addCards(data, "arrow_rain", 1);
                                return "他看了看你，又看了看箭。\n"
                                        + "然后，他把那一大把箭全部塞给了你，并祝你好运。";
                            }))));

    /** 傻子村民（v0.5.0 按需求重写）：给吃的换删牌，或者讨一块蛋糕。 */
    public static final QuestionEvent NITWIT = register(new QuestionEvent(
            "nitwit", "傻子村民",
            List.of("你看到了一个到处疯跑的绿袍村民在不停的打断别的村民的兴致..."
                    + "虽然他看着挺痴傻的，不过他笑嘻嘻的向你蹭来，指着嘴向你示意着。"),
            List.of(
                    new QuestionOption("nitwit_food", "给他吃的",
                            "选择一张牌从牌组移除。",
                            QuestionOption.CardPick.REMOVE_ANY,
                            (data, index) -> {
                                data.removeCardAt(index);
                                return "你给了他一份食物，他二话不说一口下肚，"
                                        + "然后以一个滑稽的动作跑开了...";
                            }),
                    new QuestionOption("nitwit_cake", "问问他有没有吃的",
                            "获得一张【蛋糕】。",
                            (data, random) -> {
                                addCards(data, "cake", 1);
                                return "你试着不把他当傻子，问他有没有吃的。"
                                        + "他悄咪咪的给你塞了一块怀里的蛋糕，然后以一个滑稽的动作跑开了...";
                            }))));

    /** 图书管理员：附魔的正式来源（V6 起附魔就有数据，这里给一个正常入口）。 */
    public static final QuestionEvent LIBRARIAN = register(new QuestionEvent(
            "librarian", "图书管理员", List.of(
                    new QuestionOption("librarian_enchant", "借阅手稿",
                            "给第一张【打击】附上随机附魔。",
                            (data, random) -> {
                                CardEnchantment enchantment = randomEnchantment(random);
                                boolean ok = data.enchantFirstCard(id("strike"), enchantment);
                                return ok
                                        ? "图书管理员在【打击】上写下了【" + enchantment.label() + "】。"
                                        : "你没有可以附魔的【打击】。";
                            }),
                    new QuestionOption("librarian_potion", "换一瓶药水",
                            "获得 1 瓶随机药水。",
                            (data, random) -> {
                                Potion potion = randomPotion(random);
                                if (!data.addPotion(potion.id(), data.potionSlots(Config.potionSlots()))) {
                                    return "药水槽位已满（" + data.potionSlots(Config.potionSlots()) + " 格），这瓶【" + potion.name()
                                            + "】没地方放了。";
                                }
                                return "图书管理员给了你一瓶【" + potion.name() + "】。";
                            }))));

    /** 屠夫：需求点名【蛋糕】的正式来源。 */
    public static final QuestionEvent BUTCHER = register(new QuestionEvent(
            "butcher", "屠夫", List.of(
                    new QuestionOption("butcher_meal", "来点肉",
                            "回复上限 30% 的生命。",
                            (data, random) -> {
                                int amount = Math.max(1, data.maxHp() * 30 / 100);
                                int healed = heal(data, amount);
                                return "屠夫管了一顿饱饭：回复 " + healed + " 点生命（" + data.life().describe() + "）。";
                            }),
                    new QuestionOption("butcher_cake", "拿两块蛋糕",
                            "获得 2 张【蛋糕】（消耗）。",
                            (data, random) -> {
                                addCards(data, "cake", 2);
                                return "屠夫塞给你 2 张【蛋糕】（打出后消耗）。";
                            }),
                    // v0.4.6：需求"受惊的小猪仔 只能通过屠夫事件获得"
                    new QuestionOption("butcher_piglet", "问问他那只受惊的小猪仔",
                            "获得遗物【受惊的小猪仔】。",
                            (data, random) -> {
                                Relic relic = RelicLibrary.STARTLED_PIGLET;
                                if (data.hasRelic(relic.id())) {
                                    return "你已经有一件【" + relic.name() + "】了，屠夫挠了挠头。";
                                }
                                String extra = data.grantRelic(relic.id());
                                return "屠夫从后厨抱出一只瑟瑟发抖的小猪仔塞给你："
                                        + "「它吓得不行，你带它走吧。」\n获得遗物【" + relic.name() + "】—— "
                                        + relic.description() + (extra.isEmpty() ? "" : "（" + extra + "）");
                            }))));

    /**
     * 牧师（v0.4.6 调整）。
     *
     * <p>需求原文："牧师的药水选项将从【治疗药水/再生药水/虚弱药水/力量药水/奶桶】随机抽取。"
     * 所以选项文案只说"求一瓶药水"，真正给哪一瓶在点击时才抽（随机由档案的随机源决定）。</p>
     */
    public static final QuestionEvent CLERIC = register(new QuestionEvent(
            "cleric", "牧师", List.of(
                    new QuestionOption("cleric_potion", "求一瓶药水",
                            "从【治疗/再生/虚弱/力量/奶桶】中随机获得 1 瓶。",
                            (data, random) -> {
                                List<Potion> pool = PotionLibrary.clericPool();
                                Potion potion = pool.get(random.nextInt(pool.size()));
                                if (!data.addPotion(potion.id(), data.potionSlots(Config.potionSlots()))) {
                                    return "药水槽位已满（" + data.potionSlots(Config.potionSlots()) + " 格），这瓶【"
                                            + potion.name() + "】没地方放了。";
                                }
                                return "牧师递给你一瓶【" + potion.name() + "】—— " + potion.description()
                                        + (potion.canTargetEnemy() ? "（负面药水只能对敌人用）" : "");
                            }),
                    new QuestionOption("cleric_pray", "做一次祈祷",
                            "免费删除一张【打击】。",
                            (data, random) -> data.removeFirstCard(id("strike"))
                                    ? "牧师帮你把一张【打击】从牌组里抹去了（免费删卡）。"
                                    : "你的牌组里没有【打击】可以删。"))));

    /**
     * 山羊角会再生吗？（v0.4.1 新事件）：与**山羊**交互触发。
     *
     * <p>需求原文：描述 + 两个选项（砍角得【袭击号角】/ 放走它得 50 金币但失去 6 点生命值）+ 两段结果文案。</p>
     */
    public static final QuestionEvent GOAT = register(new QuestionEvent(
            "goat", "山羊角会再生吗？",
            List.of("山羊总是强大的攀岩者，不过他们的角也可以拿去卖。或许，你面前这只山羊的角就很合适。"),
            List.of(
                    new QuestionOption("goat_horn", "砍下山羊的一只角",
                            "获得遗物【袭击号角】。",
                            (data, random) -> {
                                Relic relic = RelicLibrary.RAID_HORN;
                                if (data.hasRelic(relic.id())) {
                                    return "你已经有一件【" + relic.name() + "】了。（山羊角还是会再生的）";
                                }
                                data.grantRelic(relic.id());
                                return "你拿出剑砍下了山羊的角，以免它突然发疯撞飞你。"
                                        + "你原以为山羊会很痛，不过你忘了其实山羊角和指甲也没什么区别。"
                                        + "山羊重心不稳的离开了，你希望他不会在竞争配偶的过程中被别的山羊击伤...";
                            }),
                    new QuestionOption("goat_free", "放山羊走",
                            "获得 50 金币，失去 6 点生命值。",
                            (data, random) -> {
                                data.addGold(50);
                                data.damageHp(6);
                                return "你最终决定不伤害这只山羊。你选择了放开山羊让它离开，"
                                        + "然而，你放走它之后，你发现你迷了路，以此你只能选择跟着山羊的脚印走。\n"
                                        + "可能是因祸得福，你倒是因此在沿路发现了一个没人要的钱袋子。"
                                        + "看来这笔钱够你饱餐一顿了。"
                                        + "（+50 金币，失去 6 点生命值，当前 " + data.life().describe() + "）";
                            }))));

    /**
     * 奇怪的商人（v0.4.1 新事件）：与**流浪商人**交互触发。
     *
     * <p>需求原文：描述 + 三个选项（200 金币买【失活的珊瑚】/ 150 金币买【海洋之心】/ 都不要失去 3 点生命）+ 三段结果文案。</p>
     */
    public static final QuestionEvent STRANGE_MERCHANT = register(new QuestionEvent(
            "strange_merchant", "奇怪的商人",
            List.of("你遇到了一个带着羊驼的蓝袍商人。这家伙拿着一大堆的异域物品向你兜售着他们的价值，"
                    + "你眼花缭乱，决定从里面挑一件..."),
            List.of(
                    new QuestionOption("merchant_coral", "失去 200 金币，获得：失活的珊瑚",
                            "买下【失活的珊瑚】（每次受到的伤害 -1）。",
                            (data, random) -> buyRelic(data, RelicLibrary.DEAD_CORAL, 200,
                                    "流浪商人从羊驼的箱子里给你掏出了一个皱巴巴的失活珊瑚。\n"
                                            + "你不知道它有什么用，但是似乎会有好运发生...")),
                    new QuestionOption("merchant_heart", "失去 150 金币，获得：海洋之心",
                            "买下【海洋之心】（雨天受到的伤害 -10%）。",
                            (data, random) -> buyRelic(data, RelicLibrary.HEART_OF_THE_SEA, 150,
                                    "流浪商人从斗篷里摸出了一个闪闪发光的海洋之心。\n"
                                            + "尽管你知道这玩意只有下雨天才有用，但150块钱换个收藏品还是不亏的，不是吗。")),
                    new QuestionOption("merchant_refuse", "表示你都不想要",
                            "失去 3 点生命。",
                            (data, random) -> {
                                data.damageHp(3);
                                return "流浪商人上上下下打量了你一遍，最后恼羞成怒的给了你一拳。"
                                        + "然后它骑着羊驼快速逃跑了，可能是不希望你弄脏他的衣服。"
                                        + "（失去 3 点生命值，当前 " + data.life().describe() + "）";
                            }))));

    /**
     * 受惊的小猪仔（v0.4.6 调整）：遗物【受惊的小猪仔】**只能从屠夫事件获得**，
     * 所以这里改成"它把藏起来的药水拱出来给你"。
     */
    public static final QuestionEvent STARTLED_PIGLET = register(new QuestionEvent(
            "startled_piglet", "受惊的小猪仔", List.of(
                    new QuestionOption("piglet_soothe", "安抚它",
                            "获得 1 瓶随机药水。",
                            (data, random) -> {
                                Potion potion = randomPotion(random);
                                if (potion == null || !data.addPotion(potion.id(),
                                        data.potionSlots(Config.potionSlots()))) {
                                    return "药水槽位已满（" + data.potionSlots(Config.potionSlots())
                                            + " 格），小猪仔拱出来的药水没地方放了。";
                                }
                                return "小猪仔安静下来，蹭了蹭你的腿，还从泥里拱出一瓶【" + potion.name()
                                        + "】献给你。";
                            }),
                    new QuestionOption("piglet_leave", "轻轻放过",
                            "回复 10 点生命。",
                            (data, random) -> {
                                int healed = heal(data, 10);
                                return "你放走了它，心里轻松了些：回复 " + healed + " 点生命（" + data.life().describe()
                                        + "）。";
                            }))));

    // ------------------------------------------------------------------ v0.4.6：两个新事件

    /**
     * 蘑菇岛事件（v0.4.6）。
     *
     * <p>触发条件：**副手【卡牌收纳包】+ 主手【战斗军旗】+ 右键哞菇牛**。</p>
     */
    public static final QuestionEvent MUSHROOM_ISLAND = register(new QuestionEvent(
            "mushroom_island", "蘑菇岛事件",
            List.of("到处都是蘑菇！尽管你知道这意味着无尽的蘑菇煲，但是你的身边就是嚒菇牛。"),
            List.of(
                    new QuestionOption("mushroom_slaughter", "屠宰嚒菇牛",
                            "在牌组里增加一张【残忍】，获得遗物：嚒咕菲力。",
                            (data, random) -> {
                                addCards(data, com.slaythecraft.combat.core.StatusCards.CURSE_CRUEL, 1);
                                Relic relic = RelicLibrary.MOGU_FILLET;
                                boolean already = data.hasRelic(relic.id());
                                String extra = already ? "" : data.grantRelic(relic.id());
                                return "你真是个残忍的家伙。\n（牌组 +1【残忍】"
                                        + (already ? "，你已经拥有【嚒咕菲力】"
                                                : "，获得遗物【嚒咕菲力】" + (extra.isEmpty() ? "" : "：" + extra))
                                        + "）";
                            }),
                    new QuestionOption("mushroom_stew", "摘蘑菇制作蘑菇煲",
                            "获得蘑菇煲（药水）。",
                            (data, random) -> {
                                Potion potion = PotionLibrary.MUSHROOM_STEW;
                                if (!data.addPotion(potion.id(), data.potionSlots(Config.potionSlots()))) {
                                    return "药水槽位已满（" + data.potionSlots(Config.potionSlots()) + " 格），"
                                            + "这锅【" + potion.name() + "】没地方放了。";
                                }
                                return "我们从小就被倡导爱护生物，伤害友善动物肯定是无法下手的。\n"
                                        + "即使是蘑菇煲，你也觉得比杀生的来的血肉要美味。\n"
                                        + "（获得【蘑菇煲】：" + potion.description() + "）";
                            }))));

    /**
     * 烟熏蜜蜂事件（v0.4.6）。
     *
     * <p>触发条件：**副手【卡牌收纳包】+ 主手剪刀 + 右键蜂巢**。</p>
     */
    public static final QuestionEvent SMOKED_BEEHIVE = register(new QuestionEvent(
            "smoked_beehive", "烟熏蜜蜂事件",
            List.of("香喷喷的蜂蜜！你感觉自己饿极了，恨不得赶紧弄点香甜可口的蜂蜜吃。不过，该怎么对付蜜蜂呢？"),
            List.of(
                    new QuestionOption("beehive_smash", "直接击落并撕开蜂巢，获取蜜脾",
                            "失去 8 点生命，获得遗物：蜂蜜佐料。",
                            (data, random) -> {
                                data.damageHp(8);
                                Relic relic = RelicLibrary.HONEY_SEASONING;
                                boolean already = data.hasRelic(relic.id());
                                String extra = already ? "" : data.grantRelic(relic.id());
                                return "你饿坏了，以及顾不及身边围绕着的蜜蜂了。你抓起蜂巢转头就跑，边跑边吃，"
                                        + "当然，你也没逃过蜜蜂的一顿毒打。\n"
                                        + "（失去 8 点生命，当前 " + data.life().describe()
                                        + (already ? "；你已经拥有【蜂蜜佐料】"
                                                : "；获得遗物【蜂蜜佐料】" + (extra.isEmpty() ? "" : "：" + extra))
                                        + "）";
                            }),
                    new QuestionOption("beehive_smoke", "点燃篝火驱散蜂群，拿玻璃瓶接蜂蜜",
                            "恢复 15 点生命。",
                            (data, random) -> {
                                int healed = heal(data, 15);
                                return "蜜蜂受不了高温四散而逃，你乘乱谨慎地收集了一小瓶甘露到玻璃瓶里。\n"
                                        + "你迅速的撤离了现场。路上，你感受到甜暖的蜂蜜在滋润着你的胃部。\n"
                                        + "（回复 " + healed + " 点生命，当前 " + data.life().describe() + "）";
                            }))));

    /**
     * 事件 id → **触发方式**（v0.5.1.2 问号事件图鉴用）。
     *
     * <p>图鉴要回答"这个事件怎么触发"，而触发条件散在 {@link #byProfession}、{@link #byEntityType}
     * 和 {@code EventTriggers} 三个地方，所以这里显式登记一份给人看的文案。自检会核对
     * **每个已登记的事件都有一条触发方式** —— 以后加了新事件忘了写，自检当场报出来。</p>
     */
    private static final Map<String, String> TRIGGERS = new LinkedHashMap<>();

    static {
        // 常规村民：右键对应职业的村民
        TRIGGERS.put(ARMORER.id(), "右键【盔甲商】村民（常规事件）");
        TRIGGERS.put(WEAPONSMITH.id(), "右键【武器匠】村民（常规事件）");
        TRIGGERS.put(NITWIT.id(), "右键【傻子村民】（常规事件）");
        TRIGGERS.put(LIBRARIAN.id(), "右键【图书管理员】村民（常规事件）");
        TRIGGERS.put(BUTCHER.id(), "右键【屠夫】村民（常规事件）");
        TRIGGERS.put(CLERIC.id(), "右键【牧师】村民（常规事件）");
        // 挂在别的生物身上
        TRIGGERS.put(GOAT.id(), "右键【山羊】");
        TRIGGERS.put(STRANGE_MERCHANT.id(), "右键【流浪商人】");
        TRIGGERS.put(STARTLED_PIGLET.id(), "右键【幼年猪】");
        // 特殊触发（副手收纳包 + 主手特定物品）
        TRIGGERS.put(MUSHROOM_ISLAND.id(), "副手持【卡牌收纳包】+ 主手持【战斗军旗】，右键【哞菇牛】");
        TRIGGERS.put(SMOKED_BEEHIVE.id(), "副手持【卡牌收纳包】+ 主手持【剪刀】，右键【蜂巢 / 蜂箱】");
    }

    /** 事件的触发方式文案（图鉴用）；没登记返回空串。 */
    public static String triggerOf(String eventId) {
        return TRIGGERS.getOrDefault(eventId, "");
    }

    /** 有没有给这个事件登记触发方式（自检用）。 */
    public static boolean hasTrigger(String eventId) {
        return TRIGGERS.containsKey(eventId);
    }

    private QuestionLibrary() {
    }

    private static QuestionEvent register(QuestionEvent event) {
        BY_ID.put(event.id(), event);
        return event;
    }

    public static QuestionEvent byId(String id) {
        return BY_ID.get(id);
    }

    /** v0.4.6：这两个事件的触发条件是"手持特定物品右键特定目标"，与职业/实体表无关。 */
    public static final List<String> SPECIAL_TRIGGER_IDS = List.of("mushroom_island", "smoked_beehive");

    public static List<QuestionEvent> all() {
        return List.copyOf(BY_ID.values());
    }

    /** 村民职业 id → 事件（{@code minecraft:armorer} 之类）。 */
    public static QuestionEvent byProfession(String professionId) {
        return switch (professionId) {
            case "minecraft:armorer" -> ARMORER;
            case "minecraft:weaponsmith" -> WEAPONSMITH;
            case "minecraft:nitwit" -> NITWIT;
            case "minecraft:librarian" -> LIBRARIAN;
            case "minecraft:butcher" -> BUTCHER;
            case "minecraft:cleric" -> CLERIC;
            default -> null;
        };
    }

    /**
     * 实体 id → 事件（v0.4.1）：有些事件挂在不属于村民的生物身上。
     *
     * <ul>
     *   <li>{@code minecraft:goat} → 山羊角会再生吗？</li>
     *   <li>{@code minecraft:wandering_trader} → 奇怪的商人</li>
     *   <li>{@code minecraft:pig}（幼年）→ 受惊的小猪仔（这条走 {@code QuestionService.eventOf} 的体型判断）</li>
     * </ul>
     */
    public static QuestionEvent byEntityType(String entityTypeId) {
        return switch (entityTypeId) {
            case "minecraft:goat" -> GOAT;
            case "minecraft:wandering_trader" -> STRANGE_MERCHANT;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ 小工具

    /**
     * 花金币买一件遗物（v0.4.1 的"奇怪的商人"用）。
     *
     * @return 结果文案（买不起 / 已经拥有 / 成功）
     */
    private static String buyRelic(PlayerCardData data, Relic relic, int price, String flavor) {
        if (data.gold() < price) {
            return "你的金币不够（需要 " + price + "，当前 " + data.gold() + "），商人把东西收了回去。";
        }
        if (data.hasRelic(relic.id())) {
            return "你已经有一件【" + relic.name() + "】了，商人耸了耸肩。";
        }
        data.spendGold(price);
        data.grantRelic(relic.id());
        return flavor + "（花掉 " + price + " 金币，剩余 " + data.gold() + "）";
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    private static void addCards(PlayerCardData data, String cardId, int count) {
        for (int i = 0; i < count; i++) {
            data.addCard(CardInstance.of(id(cardId)));
        }
    }

    /** 回血并返回实际回复量（可能被上限截断）。 */
    private static int heal(PlayerCardData data, int amount) {
        int before = data.hp();
        data.healHp(amount);
        return data.hp() - before;
    }

    private static Card randomCard(List<Card> pool, Random random) {
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    /** 附魔用同一个 id 前缀（{@code stc:}），与卡牌附魔表一致。 */
    private static CardEnchantment randomEnchantment(Random random) {
        List<CardEnchantment> all = new ArrayList<>(List.of(CardEnchantment.values()));
        return all.get(random.nextInt(all.size()));
    }

    private static Potion randomPotion(Random random) {
        List<Potion> all = PotionLibrary.all();
        return all.get(random.nextInt(all.size()));
    }
}
