package com.slaythecraft;

import com.slaythecraft.combat.core.CombatRules;
import com.slaythecraft.combat.core.EventCaps;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Combat tunables. Every accessor falls back to a sane default when the config has not been loaded
 * yet, so game code never has to care about config lifecycle.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ENERGY_PER_TURN = BUILDER
            .comment("每回合的卡牌能量上限")
            .defineInRange("energyPerTurn", 3, 1, 10);

    public static final ModConfigSpec.IntValue HAND_SIZE = BUILDER
            .comment("每回合开始时抽取的手牌数")
            .defineInRange("handSize", 5, 1, 10);

    public static final ModConfigSpec.IntValue MAX_HAND_SIZE = BUILDER
            .comment("手牌数量上限，超出的牌会直接进入弃牌堆")
            .defineInRange("maxHandSize", 10, 1, 20);

    public static final ModConfigSpec.IntValue MAX_ENEMIES = BUILDER
            .comment("普通战斗最多卷入的敌人数量（测试反馈：3 只观感与难度最合适）")
            .defineInRange("maxEnemies", 3, 1, 8);

    public static final ModConfigSpec.IntValue ELITE_ENCOUNTER_SIZE = BUILDER
            .comment("精英 / BOSS 战斗卷入的敌人数量（测试反馈：默认 1，只打那只精英/BOSS）")
            .defineInRange("eliteEncounterSize", 1, 1, 4);

    public static final ModConfigSpec.BooleanValue ELITE_SQUAD = BUILDER
            .comment("是否保留\"猪灵蛮兵小队 2 只\"这个例外（false = 精英战永远只有 1 只）")
            .define("eliteSquad", true);

    public static final ModConfigSpec.IntValue COMBAT_RADIUS = BUILDER
            .comment("以玩家为中心的开战半径（格）")
            .defineInRange("combatRadius", 12, 2, 48);

    public static final ModConfigSpec.IntValue MAX_PARTY_SIZE = BUILDER
            .comment("一场尖塔遭遇战最多几名同队玩家一起打（1-4）")
            .defineInRange("maxPartySize", 4, 1, 4);

    public static final ModConfigSpec.BooleanValue FREEZE_ENEMIES = BUILDER
            .comment("战斗中锁定敌人的 AI 并使其免疫原版伤害，保证战斗只由卡牌驱动")
            .define("freezeEnemies", true);

    // 说明：v0.2.1 起独立生命值与玩家生命值完全解耦，战斗失败**不再**杀死原版角色，
    // 所以旧的 defeatKillsPlayer 配置已删除（失败 = 本局结束 + 按 resetHpOnDefeat 处理独立生命值）。

    public static final ModConfigSpec.DoubleValue MOB_HEALTH_SCALE = BUILDER
            .comment("敌人血量倍率，用来调节与原版生物的强度差")
            .defineInRange("mobHealthScale", 1.0D, 0.25D, 4.0D);

    // ------------------------------------------------------------------ 卡牌收纳包（设计文档 §13）

    public static final ModConfigSpec.IntValue STARTING_GOLD = BUILDER
            .comment("首次进入世界时发放的初始金币（v0.4.6.3：默认 99；“重开本局”也会重置回这个数）")
            .defineInRange("startingGold", 99, 0, 1_000_000);

    public static final ModConfigSpec.IntValue CARD_DELETE_COST = BUILDER
            .comment("删除一张卡牌的固定花费（收纳包 UI 与卡牌回收站共用同一价格）")
            .defineInRange("cardDeleteCost", 100, 0, 1_000_000);

    // 说明：v0.2.6 起"是否携带收纳包"改成**主手/副手手持**（或 Curios 已装备），
    // 所以旧的 requireCurios 配置已删除（它原来是"必须放进 Curios 饰品槽"）。

    public static final ModConfigSpec.BooleanValue REQUIRE_BINDER_TO_START = BUILDER
            .comment("没有携带卡牌收纳包时，是否禁止开始尖塔对战")
            .define("requireBinderToStart", true);

    // 说明：v0.0.2 起"换角色会重复发初始牌组"这个问题改成"本局结束（通关或战败）前不能换角色"，
    // 所以旧的 switchCharacterClearsCards 已经没有意义，直接删掉了。

    // ------------------------------------------------------------------ 击杀奖励（设计文档 §9.2 / §13）

    public static final ModConfigSpec.IntValue REWARD_THRESHOLD_NORMAL = BUILDER
            .comment("普通怪物：击败多少只（同种类）触发一次卡牌奖励与金币（v0.4.5 按需求提高到 60）")
            .defineInRange("rewardThresholdNormal", 60, 1, 500);

    public static final ModConfigSpec.IntValue REWARD_THRESHOLD_ELITE = BUILDER
            .comment("精英怪物：触发奖励所需的击杀数（v0.4.5 按需求提高到 30）")
            .defineInRange("rewardThresholdElite", 30, 1, 500);

    public static final ModConfigSpec.IntValue REWARD_THRESHOLD_BOSS = BUILDER
            .comment("BOSS：触发奖励所需的击杀数")
            .defineInRange("rewardThresholdBoss", 1, 1, 500);

    public static final ModConfigSpec.IntValue GOLD_NORMAL = BUILDER
            .comment("普通怪物达标时给的金币")
            .defineInRange("goldNormal", 16, 0, 100_000);

    public static final ModConfigSpec.IntValue GOLD_ELITE = BUILDER
            .comment("精英怪物达标时给的金币")
            .defineInRange("goldElite", 40, 0, 100_000);

    public static final ModConfigSpec.IntValue GOLD_BOSS = BUILDER
            .comment("BOSS 达标时给的金币")
            .defineInRange("goldBoss", 60, 0, 100_000);

    public static final ModConfigSpec.IntValue REWARD_CHOICES = BUILDER
            .comment("每次卡牌奖励给出几张候选（三选一 = 3）")
            .defineInRange("rewardChoices", 3, 1, 5);

    public static final ModConfigSpec.BooleanValue BOSS_REWARD_GOLDEN_ONLY = BUILDER
            .comment("BOSS 奖励是否必出金色品质卡牌")
            .define("bossRewardGoldenOnly", true);

    public static final ModConfigSpec.BooleanValue REWARD_ANNOUNCE_IN_CHAT = BUILDER
            .comment("奖励可用时是否额外在聊天栏提示一次（HUD 提示之外）")
            .define("rewardAnnounceInChat", true);

    public static final ModConfigSpec.BooleanValue REWARD_PER_VICTORY = BUILDER
            .comment("每次战斗胜利必给一次卡牌三选一 + 金币（v0.2.1 起默认开启：奖励改成一打完就弹）")
            .define("rewardPerVictory", true);

    /**
     * 聊天框输出的**总开关**（v0.4.6.3；v0.4.6.5 起拆成细项）。
     *
     * <p>需求（v0.4.6.3）："聊天框不再输出系统执行结果和指令。"<br>
     * 需求（v0.4.6.5 追加）："指令回执保留、只有战报和系统提示静音。"</p>
     *
     * <p>所以现在细分成三项（见下面三个配置）；这个总开关打开时**无视细项**，
     * 一切都写回聊天框（相当于 v0.4.6.2 及以前的旧行为），方便排查数值。</p>
     */
    public static final ModConfigSpec.BooleanValue CHAT_OUTPUT = BUILDER
            .comment("聊天框总开关：true = 全部照旧写进聊天框（排查用；会覆盖下面三个细项）")
            .define("chatOutput", false);

    /** 指令回执是否进聊天框（v0.4.6.5 需求："指令回执保留"）—— 默认开。 */
    public static final ModConfigSpec.BooleanValue CHAT_COMMAND_FEEDBACK = BUILDER
            .comment("是否把 /stc … 指令回执输出到聊天框（默认开；关掉后指令只剩执行、没有回执）")
            .define("chatCommandFeedback", true);

    /** 战斗战报是否进聊天框（v0.4.6.5 需求："只有战报…静音"）—— 默认关。 */
    public static final ModConfigSpec.BooleanValue CHAT_BATTLE_LOG = BUILDER
            .comment("是否把战斗战报（每一条伤害 / 抽牌 / 状态跳字）输出到聊天框（默认关）")
            .define("chatBattleLog", false);

    /** 系统提示是否进聊天框（v0.4.6.5 需求："…和系统提示静音"）—— 默认关。 */
    public static final ModConfigSpec.BooleanValue CHAT_SYSTEM_MESSAGE = BUILDER
            .comment("是否把界面 / 流程的系统提示输出到聊天框（默认关；界面与行动栏上已有同样信息）")
            .define("chatSystemMessage", false);

    // ------------------------------------------------------------------ 战斗表现（v0.0.2）

    public static final ModConfigSpec.BooleanValue SHOW_ENEMY_INTENT_ABOVE_HEAD = BUILDER
            .comment("战斗时是否在敌人头顶显示意图文字（用原版名称牌渲染，多人可见）")
            .define("showEnemyIntentAboveHead", true);

    public static final ModConfigSpec.BooleanValue BATTLE_STANDOFF = BUILDER
            .comment("开战时是否把玩家摆到敌人正对面（面对面、至少 5 格）")
            .define("battleStandoff", true);

    public static final ModConfigSpec.IntValue BATTLE_STANDOFF_DISTANCE = BUILDER
            .comment("对峙距离（格），低于 5 会被抬到 5")
            .defineInRange("battleStandoffDistance", 5, 5, 16);

    public static final ModConfigSpec.BooleanValue FORCE_THIRD_PERSON = BUILDER
            .comment("战斗期间客户端是否自动切到第三人称（战斗结束恢复玩家原来的视角设置）")
            .define("forceThirdPerson", true);

    public static final ModConfigSpec.IntValue BATTLE_CAMERA_PITCH = BUILDER
            .comment("战斗期间固定的俯视角度（度，0 = 不干预视角；测试反馈：45 度俯瞰，玩家模型不会挡住怪物）")
            .defineInRange("battleCameraPitch", 45, 0, 90);

    public static final ModConfigSpec.BooleanValue FREEZE_NEARBY_MOBS = BUILDER
            .comment("战斗期间是否顺带冻结附近的敌对生物（防止被局外怪物打断/打死）")
            .define("freezeNearbyMobs", true);

    public static final ModConfigSpec.IntValue FREEZE_RADIUS = BUILDER
            .comment("顺带冻结的半径（格），0 表示跟随 combatRadius；上限 24，避免影响过大")
            .defineInRange("freezeRadius", 0, 0, 24);

    public static final ModConfigSpec.BooleanValue IMMUNITY_DURING_COMBAT = BUILDER
            .comment("战斗期间玩家是否免疫来自战斗之外的所有伤害（引擎结算的伤害不受影响）")
            .define("immunityDuringCombat", true);

    public static final ModConfigSpec.BooleanValue DOT_DECAYS_EACH_TURN = BUILDER
            .comment("灼烧/凋零是否每回合递减 1 层（默认按需求字面意思：不递减，永久生效）")
            .define("dotDecaysEachTurn", false);

    public static final ModConfigSpec.IntValue POTION_SLOTS = BUILDER
            .comment("玩家的药水槽位数量（显示在物品栏上方）；遗物【药水腰带】会在此基础上 +2")
            .defineInRange("potionSlots", 3, 1, 8);

    // ------------------------------------------------------------------ 独立生命值与三层进度（v0.0.2 V9）

    public static final ModConfigSpec.IntValue MAX_PLAYER_HP = BUILDER
            .comment("玩家的独立生命值上限（与原版血量解耦，跨战斗保留）")
            .defineInRange("maxPlayerHp", 75, 1, 1000);

    public static final ModConfigSpec.IntValue EVENT_CAP_NORMAL = BUILDER
            .comment("每层常规战斗的上限，达到后本层不再能发起常规战斗")
            .defineInRange("eventCapNormal", 15, 0, 1000);

    public static final ModConfigSpec.IntValue EVENT_CAP_ELITE = BUILDER
            .comment("每层精英战斗的上限")
            .defineInRange("eventCapElite", 5, 0, 1000);

    public static final ModConfigSpec.IntValue EVENT_CAP_CAMPFIRE = BUILDER
            .comment("每层火堆事件的上限")
            .defineInRange("eventCapCampfire", 5, 0, 1000);

    public static final ModConfigSpec.IntValue EVENT_CAP_QUESTION = BUILDER
            .comment("每层问号事件的上限")
            .defineInRange("eventCapQuestion", 5, 0, 1000);

    public static final ModConfigSpec.IntValue EVENT_CAP_TREASURE = BUILDER
            .comment("每层藏宝箱的上限（v0.5.5.1：用户要求 5 次，并计入事件统计）")
            .defineInRange("eventCapTreasure", 5, 0, 1000);

    public static final ModConfigSpec.IntValue BOSS_CHALLENGE_RADIUS = BUILDER
            .comment("挑战本层 BOSS 时搜索该 BOSS 实体的半径（格）；BOSS 不会由本 mod 生成，需要自己找到")
            .defineInRange("bossChallengeRadius", 64, 8, 256);

    public static final ModConfigSpec.BooleanValue RESET_HP_ON_DEFEAT = BUILDER
            .comment("战败后是否把独立生命值回满（关闭则保留 1 点，方便硬核玩法接续）")
            .define("resetHpOnDefeat", true);

    public static final ModConfigSpec.IntValue CAMPFIRE_HEAL_PERCENT = BUILDER
            .comment("篝火愈合/包扎回复的生命值比例（按独立生命上限计算；需求指定 25%）")
            .defineInRange("campfireHealPercent", 25, 1, 100);

    /**
     * 常规战斗掉落遗物的概率（v0.5.0.20）。
     *
     * <p>需求原文："现在，常规战斗的结算有百分之十的概率掉落一件非特殊遗物。"
     * 存成**百分数**（0..100），界面上更好读；代码里除 100 用。</p>
     */
    public static final ModConfigSpec.IntValue NORMAL_BATTLE_RELIC_CHANCE = BUILDER
            .comment("常规战斗结算时掉落一件非特殊遗物的概率（百分数；需求指定 10%）")
            .defineInRange("normalBattleRelicChance", 10, 0, 100);

    // ------------------------------------------------------------------ 界面（v0.5.0）

    public static final ModConfigSpec.BooleanValue UI_TEXT_SHADOW = BUILDER
            .comment("模组界面文字是否画原版投影（默认关闭：中文小字号 + 深色面板上投影看起来像重影）")
            .define("uiTextShadow", false);

    // ------------------------------------------------------------------ 卡牌商店（设计文档 §6.4 / §13）

    public static final ModConfigSpec.IntValue SHOP_OFFER_COUNT = BUILDER
            .comment("商店一次摆出几张卡牌（v0.4.6 需求：5 张）")
            .defineInRange("shopOfferCount", 5, 1, 8);

    public static final ModConfigSpec.IntValue SHOP_RELIC_COUNT = BUILDER
            .comment("商店一次摆出几件遗物（v0.4.6 需求：3 件；只卖能随机掉落的遗物）")
            .defineInRange("shopRelicCount", 3, 0, 6);

    public static final ModConfigSpec.IntValue SHOP_POTION_COUNT = BUILDER
            .comment("商店一次摆出几瓶药水（v0.4.6 需求：3 瓶）")
            .defineInRange("shopPotionCount", 3, 0, 6);

    public static final ModConfigSpec.IntValue SHOP_REFRESH_COST = BUILDER
            .comment("刷新一次商品的金币花费（v0.4.6 需求：40；刷新不会动「删牌机会」）")
            .defineInRange("shopRefreshCost", 40, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SHOP_REFRESH_LIMIT = BUILDER
            .comment("每次进店最多刷新几次（v0.4.6 需求：3 次）")
            .defineInRange("shopRefreshLimit", 3, 0, 99);

    public static final ModConfigSpec.IntValue SHOP_PRICE_RELIC = BUILDER
            .comment("商店里遗物的售价")
            .defineInRange("shopPriceRelic", 150, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SHOP_PRICE_POTION = BUILDER
            .comment("商店里药水的售价")
            .defineInRange("shopPricePotion", 60, 0, 1_000_000);

    public static final ModConfigSpec.IntValue CARD_DELETE_COST_INCREASE = BUILDER
            .comment("每删过一张牌，下一次删牌的价格就上涨这么多（v0.4.6 需求：+50）")
            .defineInRange("cardDeleteCostIncrease", 50, 0, 100_000);

    public static final ModConfigSpec.IntValue SHOP_PRICE_COMMON = BUILDER
            .comment("普通品质卡的售价")
            .defineInRange("shopPriceCommon", 75, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SHOP_PRICE_UNCOMMON = BUILDER
            .comment("罕见品质卡的售价")
            .defineInRange("shopPriceUncommon", 150, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SHOP_PRICE_RARE = BUILDER
            .comment("稀有品质卡的售价")
            .defineInRange("shopPriceRare", 300, 0, 1_000_000);

    public static final ModConfigSpec.IntValue SHOP_PRICE_GOLD = BUILDER
            .comment("金色品质卡的售价（商店默认不卖金色，此值只在卡池被整合包改成出售时生效）")
            .defineInRange("shopPriceGold", 600, 0, 1_000_000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    /** Rules snapshot for a new battle. */
    public static CombatRules rules() {
        return new CombatRules(
                intOr(ENERGY_PER_TURN, 3),
                intOr(HAND_SIZE, 5),
                intOr(MAX_HAND_SIZE, 10),
                // v0.4.5：兜底值与 Config.MAX_ENEMIES 的默认值保持一致（常规战斗最多 3 名敌人）
                intOr(MAX_ENEMIES, 3),
                boolOr(DOT_DECAYS_EACH_TURN, false));
    }

    public static int combatRadius() {
        return intOr(COMBAT_RADIUS, 12);
    }

    public static int maxPartySize() {
        return intOr(MAX_PARTY_SIZE, 4);
    }

    public static double mobHealthScale() {
        try {
            return MOB_HEALTH_SCALE.getAsDouble();
        } catch (RuntimeException e) {
            return 1.0D;
        }
    }

    public static boolean freezeEnemies() {
        return boolOr(FREEZE_ENEMIES, true);
    }

    /** 初始金币（v0.4.6.3 起默认 99；配置未加载时也必须回退到 99，见 ConfigDefaultsCheck）。 */
    public static int startingGold() {
        return intOr(STARTING_GOLD, 99);
    }

    public static int cardDeleteCost() {
        return intOr(CARD_DELETE_COST, 100);
    }

    public static boolean requireBinderToStart() {
        return boolOr(REQUIRE_BINDER_TO_START, true);
    }

    public static int maxEnemies() {
        return intOr(MAX_ENEMIES, 3);
    }

    public static int eliteEncounterSize() {
        return intOr(ELITE_ENCOUNTER_SIZE, 1);
    }

    public static boolean eliteSquad() {
        return boolOr(ELITE_SQUAD, true);
    }

    public static int rewardThresholdNormal() {
        // v0.4.5：兜底值必须与配置默认值一致（需求把普通怪阈值提到 60）
        return intOr(REWARD_THRESHOLD_NORMAL, 60);
    }

    public static int rewardThresholdElite() {
        // v0.4.5：兜底值必须与配置默认值一致（需求把精英阈值提到 30）
        return intOr(REWARD_THRESHOLD_ELITE, 30);
    }

    public static int rewardThresholdBoss() {
        return intOr(REWARD_THRESHOLD_BOSS, 1);
    }

    public static int goldNormal() {
        return intOr(GOLD_NORMAL, 16);
    }

    public static int goldElite() {
        return intOr(GOLD_ELITE, 40);
    }

    public static int goldBoss() {
        return intOr(GOLD_BOSS, 60);
    }

    public static int rewardChoices() {
        return intOr(REWARD_CHOICES, 3);
    }

    public static boolean bossRewardGoldenOnly() {
        return boolOr(BOSS_REWARD_GOLDEN_ONLY, true);
    }

    public static boolean rewardAnnounceInChat() {
        return boolOr(REWARD_ANNOUNCE_IN_CHAT, true);
    }

    public static boolean rewardPerVictory() {
        return boolOr(REWARD_PER_VICTORY, true);
    }

    /** 聊天框是否输出系统执行结果与指令回执（v0.4.6.3 起默认关闭）。 */
    /** 聊天框总开关（true 时三项细项失效，一切都写聊天框）。 */
    public static boolean chatOutput() {
        return boolOr(CHAT_OUTPUT, false);
    }

    /** 指令回执是否进聊天框（默认 true —— 需求："指令回执保留"）。 */
    public static boolean chatCommandFeedback() {
        return boolOr(CHAT_COMMAND_FEEDBACK, true);
    }

    /** 战斗战报是否进聊天框（默认 false —— 需求："只有战报…静音"）。 */
    public static boolean chatBattleLog() {
        return boolOr(CHAT_BATTLE_LOG, false);
    }

    /** 系统提示是否进聊天框（默认 false —— 需求："…和系统提示静音"）。 */
    public static boolean chatSystemMessage() {
        return boolOr(CHAT_SYSTEM_MESSAGE, false);
    }

    public static boolean showEnemyIntentAboveHead() {
        return boolOr(SHOW_ENEMY_INTENT_ABOVE_HEAD, true);
    }

    public static boolean battleStandoff() {
        return boolOr(BATTLE_STANDOFF, true);
    }

    public static int battleStandoffDistance() {
        return intOr(BATTLE_STANDOFF_DISTANCE, 5);
    }

    public static boolean forceThirdPerson() {
        return boolOr(FORCE_THIRD_PERSON, true);
    }

    public static int battleCameraPitch() {
        return intOr(BATTLE_CAMERA_PITCH, 45);
    }

    public static boolean freezeNearbyMobs() {
        return boolOr(FREEZE_NEARBY_MOBS, true);
    }

    public static int freezeRadius() {
        return intOr(FREEZE_RADIUS, 0);
    }

    public static boolean immunityDuringCombat() {
        return boolOr(IMMUNITY_DURING_COMBAT, true);
    }

    public static boolean dotDecaysEachTurn() {
        return boolOr(DOT_DECAYS_EACH_TURN, false);
    }

    public static int potionSlots() {
        return intOr(POTION_SLOTS, 3);
    }

    public static int maxPlayerHp() {
        return intOr(MAX_PLAYER_HP, 75);
    }

    /** 每层事件上限（需求给的 15/5/5/5；v0.5.5.1 起藏宝箱也有 5 次上限）。 */
    public static EventCaps eventCaps() {
        return new EventCaps(
                intOr(EVENT_CAP_NORMAL, 15),
                intOr(EVENT_CAP_ELITE, 5),
                intOr(EVENT_CAP_CAMPFIRE, 5),
                intOr(EVENT_CAP_QUESTION, 5),
                intOr(EVENT_CAP_TREASURE, 5));
    }

    public static int bossChallengeRadius() {
        return intOr(BOSS_CHALLENGE_RADIUS, 64);
    }

    public static boolean resetHpOnDefeat() {
        return boolOr(RESET_HP_ON_DEFEAT, true);
    }

    public static int campfireHealPercent() {
        return intOr(CAMPFIRE_HEAL_PERCENT, 25);
    }

    /** 常规战斗掉落遗物的概率（0..1；配置里存的是百分数）。 */
    public static double normalBattleRelicChance() {
        return intOr(NORMAL_BATTLE_RELIC_CHANCE, 10) / 100.0D;
    }

    /** 模组界面文字要不要画投影（v0.5.0：默认不画，避免中文显示成"重影"）。 */
    public static boolean uiTextShadow() {
        return boolOr(UI_TEXT_SHADOW, false);
    }

    public static int shopOfferCount() {
        return intOr(SHOP_OFFER_COUNT, 5);
    }

    public static int shopRelicCount() {
        return intOr(SHOP_RELIC_COUNT, 3);
    }

    public static int shopPotionCount() {
        return intOr(SHOP_POTION_COUNT, 3);
    }

    public static int shopRefreshCost() {
        return intOr(SHOP_REFRESH_COST, 40);
    }

    public static int shopRefreshLimit() {
        return intOr(SHOP_REFRESH_LIMIT, 3);
    }

    public static int shopPriceRelic() {
        return intOr(SHOP_PRICE_RELIC, 150);
    }

    public static int shopPricePotion() {
        return intOr(SHOP_PRICE_POTION, 60);
    }

    /** 每删过一张牌，下一次删牌贵多少（v0.4.6）。 */
    public static int cardDeleteCostIncrease() {
        return intOr(CARD_DELETE_COST_INCREASE, 50);
    }

    public static int shopPriceCommon() {
        return intOr(SHOP_PRICE_COMMON, 75);
    }

    public static int shopPriceUncommon() {
        return intOr(SHOP_PRICE_UNCOMMON, 150);
    }

    public static int shopPriceRare() {
        return intOr(SHOP_PRICE_RARE, 300);
    }

    public static int shopPriceGold() {
        return intOr(SHOP_PRICE_GOLD, 600);
    }

    private static int intOr(ModConfigSpec.IntValue value, int fallback) {
        try {
            return value.getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static boolean boolOr(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
