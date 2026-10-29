package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 卡池（S4 起带品质与归属角色）。
 *
 * <p>这是**当前唯一**的卡牌定义来源；设计文档 §5.3 决定最终搬到数据包注册表 {@code stc:card}，
 * 迁移时这张表会变成内置 JSON，字段一一对应（cost/type/target/rarity/character/effects），
 * 因此本表就是迁移的清单。</p>
 *
 * <p>品格约定：{@code character == null} 表示公共池，任何角色都能抽到；
 * {@link CardRarity#GOLD} 是 BOSS 奖励专属，不进普通奖励池。</p>
 */
public final class CardLibrary {

    /** 归属角色的命名空间 id（与 {@code CharacterRegistry} 的角色 id 一致）。 */
    public static final String IRONCLAD = "stc:ironclad";
    public static final String SILENT = "stc:silent";
    public static final String DEFECT = "stc:defect";
    /** v0.0.3 W3：新角色"叛逃掠夺者"。 */
    public static final String PILLAGER = "stc:pillager";

    private static final Map<String, Card> BY_ID = new LinkedHashMap<>();
    /** 强化版定义（v0.0.2 V11）：与基础定义同 id，但数值更高；不进 {@link #BY_ID}，也不会被抽卡池抽到。 */
    private static final Map<String, Card> UPGRADED = new LinkedHashMap<>();

    // ---------------------------------------------------------------- 公共（任何角色都能抽到）
    public static final Card STRIKE = register(new Card(
            "strike", "打击", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 6 点伤害。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> ctx.dealDamage(self, target, 6, "「打击」")));

    public static final Card DEFEND = register(new Card(
            "defend", "格挡", CardType.SKILL, 1, CardTarget.SELF, "获得 5 点格挡。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> ctx.grantPlayerBlock(5, "「格挡」")));

    // ---------------------------------------------------------------- 铁甲战士
    public static final Card BASH = register(new Card(
            "bash", "重击", CardType.ATTACK, 2, CardTarget.ENEMY, "造成 8 点伤害，施加 2 层易伤。",
            CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 8, "「重击」");
                if (target != null && target.alive()) {
                    target.addVulnerable(2);
                    ctx.announce("  " + target.name() + " 获得易伤 2" + target.statusSuffix());
                }
            }));

    public static final Card CLEAVE = register(new Card(
            "cleave", "顺劈斩", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES, "对所有敌人造成 8 点伤害。",
            CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 8, "「顺劈斩」");
                }
            }));

    public static final Card IRON_WAVE = register(new Card(
            "iron_wave", "铁斩波", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 5 点伤害，获得 5 点格挡。",
            CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 5, "「铁斩波」");
                ctx.grantPlayerBlock(5, "「铁斩波」");
            }));

    public static final Card POMMEL_STRIKE = register(new Card(
            "pommel_strike", "柄击", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 9 点伤害，抽 1 张牌。",
            CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 9, "「柄击」");
                ctx.drawCards(1);
            }));

    public static final Card SHRUG_IT_OFF = register(new Card(
            "shrug_it_off", "不痛不痒", CardType.SKILL, 1, CardTarget.SELF, "获得 8 点格挡，抽 1 张牌。",
            CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(8, "「不痛不痒」");
                ctx.drawCards(1);
            }));

    // ---------------------------------------------------------------- 静默猎手
    public static final Card NEUTRALIZE = register(new Card(
            "neutralize", "中和", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 3 点伤害，施加 1 层虚弱。",
            CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 3, "「中和」");
                if (target != null && target.alive()) {
                    target.addWeak(1);
                    ctx.announce("  " + target.name() + " 获得虚弱 1" + target.statusSuffix());
                }
            }));

    public static final Card SURVIVOR = register(new Card(
            "survivor", "生存", CardType.SKILL, 1, CardTarget.SELF, "获得 8 点格挡。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> ctx.grantPlayerBlock(8, "「生存」")));

    // ---- v0.4.2：静默猎手的新牌（能力牌 + 中毒体系，v0.4.6 起"涂毒"更名为"中毒"）

    /**
     * 余像（v0.4.2）：1 费 稀有 **能力牌** —— 每打出一张牌就获得 1 点格挡；升级获得【固有】。
     */
    public static final Card AFTER_IMAGE = register(new Card(
            "after_image", "余像", CardType.POWER, 1, CardTarget.SELF, "每打出一张牌，获得 1 点格挡。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> ctx.enableAfterImage(self)));

    /** 肾上腺素（v0.4.2）：0 费 稀有 技能牌 —— 获得 1 点能量、抽 2 张牌、消耗；升级 2 点能量。 */
    public static final Card ADRENALINE = register(new Card(
            "adrenaline", "肾上腺素", CardType.SKILL, 0, CardTarget.SELF,
            "获得 1 点能量。抽 2 张牌。消耗。",
            CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                ctx.gainEnergy(self, 1);
                ctx.drawCards(2);
            }));

    /** 触媒（v0.4.2）：1 费 稀有 **能力牌** —— 【中毒】额外触发一次；升级两次。 */
    public static final Card CATALYST = register(new Card(
            "catalyst", "触媒", CardType.POWER, 1, CardTarget.SELF, "【中毒】会额外触发一次。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> ctx.addPoisonTriggers(self, 1)));

    /** 背刺（v0.4.2）：0 费 罕见 攻击 —— 造成 10 点伤害，固有、消耗；升级 15 点。 */
    public static final Card BACKSTAB = register(new Card(
            "backstab", "背刺", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 10 点伤害。固有。消耗。",
            CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.INNATE, CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.dealDamage(self, target, 10, "「背刺」")));

    /** 弹跳药瓶（v0.4.2）：2 费 罕见 技能 —— 给随机敌人 3 层中毒，三次；升级四次。 */
    public static final Card BOUNCING_FLASK = register(new Card(
            "bouncing_flask", "弹跳药瓶", CardType.SKILL, 2, CardTarget.SELF,
            "给予随机敌人 3 层中毒，三次。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                for (int i = 0; i < 3; i++) {
                    ctx.applyPoison(ctx.randomLivingEnemy(), 3);
                }
            }));

    /** 咕嘟冒泡（v0.4.2）：1 费 罕见 技能 —— 如果敌人已有中毒，则再给 9 层；升级 12 层。 */
    public static final Card BUBBLE_BUBBLE = register(new Card(
            "bubble_bubble", "咕嘟冒泡", CardType.SKILL, 1, CardTarget.ENEMY,
            "如果敌人拥有中毒，则给予 9 层中毒。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                if (target == null || !target.alive()) {
                    return;
                }
                if (target.poison() <= 0) {
                    ctx.announce("  " + target.name() + " 身上没有中毒，什么也没发生");
                    return;
                }
                ctx.applyPoison(target, 9);
            }));

    // ---------------------------------------------------------------- 故障机器人（充能球，v0.0.3 W4）
    /** 电击：1 费 生成 1 颗闪电球（被动 3 伤害 / 激发 8 伤害）。 */
    public static final Card ZAP = register(new Card(
            "zap", "电击", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗闪电球。",
            CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> ctx.channelOrb(self, Orb.LIGHTNING)));

    /** 冰球：1 费 生成 1 颗冰球（被动 2 格挡 / 激发 5 格挡）。 */
    public static final Card FROST = register(new Card(
            "frost", "冰球", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗冰球。",
            CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> ctx.channelOrb(self, Orb.FROST)));

    /** 双重释放：1 费 激发最前面的充能球 2 次。 */
    public static final Card DUAL_CAST = register(new Card(
            "dual_cast", "双重释放", CardType.SKILL, 1, CardTarget.SELF, "激发你最前面的充能球 2 次。",
            CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.evokeOrb(self);
                ctx.evokeOrb(self);
            }));

    // ---- v0.3.0：补全另外两种球（暗球 / 等离子球）以及它们的牌

    /** 暗影：1 费 生成 1 颗暗球（被动每回合充能 +6，激发时砸向生命最低的敌人）。 */
    public static final Card DARKNESS = register(new Card(
            "darkness", "暗影", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗暗球。",
            CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> ctx.channelOrb(self, Orb.DARK)));

    /** 核聚变：2 费 生成 1 颗等离子球（回合开始 +1 能量，激发 +2 能量）。 */
    public static final Card FUSION = register(new Card(
            "fusion", "核聚变", CardType.SKILL, 2, CardTarget.SELF, "生成 1 颗等离子球。",
            CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> ctx.channelOrb(self, Orb.PLASMA)));

    /** 混沌：1 费 生成 1 颗随机充能球。 */
    public static final Card CHAOS = register(new Card(
            "chaos", "混沌", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗随机充能球。",
            CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> ctx.channelOrb(self, ctx.randomOrb())));

    /** 递归：1 费 激发最前面的充能球，然后再生成同样的一颗。 */
    public static final Card RECURSION = register(new Card(
            "recursion", "递归", CardType.SKILL, 1, CardTarget.SELF,
            "激发你最前面的充能球，然后再生成同样的一颗。",
            CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                List<com.slaythecraft.combat.core.OrbInstance> orbs = self.orbs();
                if (orbs.isEmpty()) {
                    ctx.announce("  没有充能球可以激发");
                    return;
                }
                Orb type = orbs.get(0).type();
                ctx.evokeOrb(self);
                ctx.channelOrb(self, type);
            }));

    // ---------------------------------------------------------------- 金色（BOSS 奖励专属，v0.4.0 按角色分开）
    /** 铁甲战士的金色牌：恶魔形态（力量 +3）。 */
    public static final Card DEMON_FORM = register(new Card(
            "demon_form", "恶魔形态", CardType.POWER, 3, CardTarget.SELF, "获得 3 点力量。",
            CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> {
                self.addStrength(3);
                ctx.announce("  你获得力量 +3" + self.statusSuffix());
            }));

    /** 铁甲战士的金色牌：无懈可击（20 格挡）。 */
    public static final Card IMPERVIOUS = register(new Card(
            "impervious", "无懈可击", CardType.SKILL, 2, CardTarget.SELF, "获得 20 点格挡。",
            CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> ctx.grantPlayerBlock(20, "「无懈可击」")));

    /** 静默猎手的金色牌：致命毒药（施加 12 层中毒）。 */
    public static final Card DEADLY_POISON = register(new Card(
            "deadly_poison", "致命毒药", CardType.SKILL, 1, CardTarget.ENEMY, "施加 12 层中毒。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> {
                if (target != null && target.alive()) {
                    target.addPoison(12);
                    ctx.announce("  " + target.name() + " 中毒 12 层" + target.statusSuffix());
                }
            }));

    /** 故障机器人的金色牌：雷霆万钧（对所有敌人 12 点伤害，并生成 2 颗闪电球）。 */
    public static final Card THUNDER = register(new Card(
            "thunder", "雷霆万钧", CardType.SKILL, 3, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 12 点伤害，生成 2 颗闪电球。",
            CardRarity.RARE, DEFECT,
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 12, "「雷霆万钧」");
                }
                ctx.channelOrb(self, Orb.LIGHTNING);
                ctx.channelOrb(self, Orb.LIGHTNING);
            }));

    /** 叛逃掠夺者的金色牌：穿云箭（对单个敌人 24 点伤害）。 */
    public static final Card PIERCING_ARROW = register(new Card(
            "piercing_arrow", "穿云箭", CardType.ATTACK, 2, CardTarget.ENEMY, "造成 24 点伤害。",
            CardRarity.RARE, PILLAGER,
            (ctx, self, target) -> ctx.dealDamage(self, target, 24, "「穿云箭」")));

    // ---------------------------------------------------------------- 无色牌（v0.0.2 V4 起：关键字的实际载体）
    // 说明：这两张的正式来源是 V11 的问号事件（武器匠 / 傻子村民），现在先用调试指令验证关键字行为。
    public static final Card ARROW_RAIN = register(new Card(
            "arrow_rain", "箭雨", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES, "对所有敌人造成 8 点伤害，抽 2 张牌。",
            CardRarity.UNCOMMON, null, Set.of(CardKeyword.RETAIN),
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 8, "「箭雨」");
                }
                ctx.drawCards(2);
            }));

    public static final Card CAKE = register(new Card(
            "cake", "蛋糕", CardType.SKILL, 2, CardTarget.SELF, "恢复 5 点生命。",
            CardRarity.COMMON, null, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                self.heal(5);
                ctx.announce("  恢复 5 点生命，当前 HP " + self.hp() + "/" + self.maxHp());
            }));

    // ---------------------------------------------------------------- 强化版（v0.0.2 V11）
    // 同一张卡被强化后不再返回基础定义：数值更高、名字带 "+"。
    // 这里刻意集中在一个静态块里，是因为"强化"是后加的机制，集中放置最容易与基础定义对照。
    static {        upgrade(new Card("strike", "打击+", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 9 点伤害。",
                CardRarity.COMMON, null,
                (ctx, self, target) -> ctx.dealDamage(self, target, 9, "「打击+」")));
        upgrade(new Card("defend", "格挡+", CardType.SKILL, 1, CardTarget.SELF, "获得 8 点格挡。",
                CardRarity.COMMON, null,
                (ctx, self, target) -> ctx.grantPlayerBlock(8, "「格挡+」")));
        upgrade(new Card("bash", "重击+", CardType.ATTACK, 2, CardTarget.ENEMY, "造成 10 点伤害，施加 3 层易伤。",
                CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 10, "「重击+」");
                    if (target != null && target.alive()) {
                        target.addVulnerable(3);
                        ctx.announce("  " + target.name() + " 获得易伤 3" + target.statusSuffix());
                    }
                }));
        upgrade(new Card("cleave", "顺劈斩+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES, "对所有敌人造成 11 点伤害。",
                CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 11, "「顺劈斩+」");
                    }
                }));
        upgrade(new Card("iron_wave", "铁斩波+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 7 点伤害，获得 7 点格挡。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 7, "「铁斩波+」");
                    ctx.grantPlayerBlock(7, "「铁斩波+」");
                }));
        upgrade(new Card("pommel_strike", "柄击+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 10 点伤害，抽 2 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 10, "「柄击+」");
                    ctx.drawCards(2);
                }));
        upgrade(new Card("shrug_it_off", "不痛不痒+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 11 点格挡，抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(11, "「不痛不痒+」");
                    ctx.drawCards(1);
                }));
        upgrade(new Card("neutralize", "中和+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 4 点伤害，施加 2 层虚弱。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 4, "「中和+」");
                    if (target != null && target.alive()) {
                        target.addWeak(2);
                        ctx.announce("  " + target.name() + " 获得虚弱 2" + target.statusSuffix());
                    }
                }));
        upgrade(new Card("survivor", "生存+", CardType.SKILL, 1, CardTarget.SELF, "获得 11 点格挡。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> ctx.grantPlayerBlock(11, "「生存+」")));
        // v0.4.2：静默猎手新牌的强化版
        // 余像+：获得【固有】（开局就在手里）
        upgrade(new Card("after_image", "余像+", CardType.POWER, 1, CardTarget.SELF,
                "固有。每打出一张牌，获得 1 点格挡。", CardRarity.RARE, SILENT,
                Set.of(CardKeyword.INNATE),
                (ctx, self, target) -> ctx.enableAfterImage(self)));
        // 肾上腺素+：1 点能量 → 2 点能量
        upgrade(new Card("adrenaline", "肾上腺素+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 2 点能量。抽 2 张牌。消耗。", CardRarity.RARE, SILENT,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    ctx.gainEnergy(self, 2);
                    ctx.drawCards(2);
                }));
        // 触媒+：额外触发一次 → 两次
        upgrade(new Card("catalyst", "触媒+", CardType.POWER, 1, CardTarget.SELF,
                "【中毒】会额外触发两次。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> ctx.addPoisonTriggers(self, 2)));
        upgrade(new Card("backstab", "背刺+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 15 点伤害。固有。消耗。", CardRarity.UNCOMMON, SILENT,
                Set.of(CardKeyword.INNATE, CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.dealDamage(self, target, 15, "「背刺+」")));
        upgrade(new Card("bouncing_flask", "弹跳药瓶+", CardType.SKILL, 2, CardTarget.SELF,
                "给予随机敌人 3 层中毒，四次。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    for (int i = 0; i < 4; i++) {
                        ctx.applyPoison(ctx.randomLivingEnemy(), 3);
                    }
                }));
        upgrade(new Card("bubble_bubble", "咕嘟冒泡+", CardType.SKILL, 1, CardTarget.ENEMY,
                "如果敌人拥有中毒，则给予 12 层中毒。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    if (target == null || !target.alive() || target.poison() <= 0) {
                        return;
                    }
                    ctx.applyPoison(target, 12);
                }));
        upgrade(new Card("demon_form", "恶魔形态+", CardType.POWER, 3, CardTarget.SELF, "获得 4 点力量。",
                CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> {
                    self.addStrength(4);
                    ctx.announce("  你获得力量 +4" + self.statusSuffix());
                }));
        upgrade(new Card("impervious", "无懈可击+", CardType.SKILL, 2, CardTarget.SELF, "获得 30 点格挡。",
                CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> ctx.grantPlayerBlock(30, "「无懈可击+」")));
        // v0.4.0：另外三个角色的金色牌（每个角色都有自己的 BOSS 池）
        upgrade(new Card("deadly_poison", "致命毒药+", CardType.SKILL, 1, CardTarget.ENEMY,
                "施加 16 层中毒。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> {
                    if (target != null && target.alive()) {
                        target.addPoison(16);
                        ctx.announce("  " + target.name() + " 中毒 16 层" + target.statusSuffix());
                    }
                }));
        upgrade(new Card("thunder", "雷霆万钧+", CardType.SKILL, 3, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 16 点伤害，生成 3 颗闪电球。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 16, "「雷霆万钧+」");
                    }
                    ctx.channelOrb(self, Orb.LIGHTNING);
                    ctx.channelOrb(self, Orb.LIGHTNING);
                    ctx.channelOrb(self, Orb.LIGHTNING);
                }));
        upgrade(new Card("piercing_arrow", "穿云箭+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 32 点伤害。", CardRarity.RARE, PILLAGER,
                (ctx, self, target) -> ctx.dealDamage(self, target, 32, "「穿云箭+」")));
        // 需求点名的两张（V11）：箭雨 11 伤害、蛋糕 7 恢复
        upgrade(new Card("arrow_rain", "箭雨+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 11 点伤害，抽 2 张牌。", CardRarity.UNCOMMON, null, Set.of(CardKeyword.RETAIN),
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 11, "「箭雨+」");
                    }
                    ctx.drawCards(2);
                }));
        upgrade(new Card("cake", "蛋糕+", CardType.SKILL, 2, CardTarget.SELF, "恢复 7 点生命。",
                CardRarity.COMMON, null, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    self.heal(7);
                    ctx.announce("  恢复 7 点生命，当前 HP " + self.hp() + "/" + self.maxHp());
                }));
        // v0.0.3 W3：新卡与充能球卡的强化版
        upgrade(new Card("aimed_shot", "集中射击+", CardType.ATTACK, 2, CardTarget.ENEMY, "造成 9 点伤害。",
                CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> ctx.dealDamage(self, target, 9, "「集中射击+」")));
        upgrade(new Card("take_cover", "寻找掩体+", CardType.SKILL, 2, CardTarget.SELF,
                "获得 11 点格挡，在下回合获得 7 点格挡。", CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(11, "「寻找掩体+」");
                    self.addNextTurnBlock(7);
                    ctx.announce("  同时预留了 7 点格挡给下个回合");
                }));
        // 翻滚+：效果一样，但**去掉消耗**（需求原文："升级：获得 2 点敏捷。（消耗词条移除）"）
        upgrade(new Card("roll", "翻滚+", CardType.SKILL, 1, CardTarget.SELF, "获得 2 点敏捷。",
                CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> {
                    self.addDexterity(2);
                    ctx.announce("  迅捷 +2" + self.statusSuffix());
                }));
        // 烟雾弹+：费用变 0
        upgrade(new Card("smoke_bomb", "烟雾弹+", CardType.SKILL, 0, CardTarget.ENEMY,
                "敌人失去 3 点力量，你获得 2 点力量。", CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> {
                    if (target != null) {
                        int before = target.strength();
                        target.addStrength(-3);
                        int lost = before - target.strength();
                        if (lost > 0) {
                            ctx.announce("  " + target.name() + " 力量 -" + lost + "（当前 "
                                    + signed(target.strength()) + "）");
                        } else {
                            ctx.announce("  " + target.name() + " 本来就没有力量，失去 3 点力量没有效果"
                                    + "（当前 " + signed(target.strength()) + "）");
                        }
                    }
                    self.addStrength(2);
                    ctx.announce("  你获得力量 +2（当前 " + signed(self.strength()) + "）");
                }));
        upgrade(new Card("zap", "电击+", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗闪电球。",
                CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> ctx.channelOrb(self, Orb.LIGHTNING)));
        upgrade(new Card("frost", "冰球+", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗冰球。",
                CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> ctx.channelOrb(self, Orb.FROST)));
        // 双重释放+：激发 3 次（2 → 3）
        upgrade(new Card("dual_cast", "双重释放+", CardType.SKILL, 1, CardTarget.SELF,
                "激发你最前面的充能球 3 次。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.evokeOrb(self);
                    ctx.evokeOrb(self);
                    ctx.evokeOrb(self);
                }));
        // v0.3.0：暗球 / 等离子球的强化版
        upgrade(new Card("darkness", "暗影+", CardType.SKILL, 1, CardTarget.SELF, "生成 2 颗暗球。",
                CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.channelOrb(self, Orb.DARK);
                    ctx.channelOrb(self, Orb.DARK);
                }));
        // 核聚变+：费用 2 → 1
        upgrade(new Card("fusion", "核聚变+", CardType.SKILL, 1, CardTarget.SELF, "生成 1 颗等离子球。",
                CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> ctx.channelOrb(self, Orb.PLASMA)));
        upgrade(new Card("chaos", "混沌+", CardType.SKILL, 1, CardTarget.SELF, "生成 2 颗随机充能球。",
                CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.channelOrb(self, ctx.randomOrb());
                    ctx.channelOrb(self, ctx.randomOrb());
                }));
        // 递归+：费用 1 → 0
        upgrade(new Card("recursion", "递归+", CardType.SKILL, 0, CardTarget.SELF,
                "激发你最前面的充能球，然后再生成同样的一颗。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    List<com.slaythecraft.combat.core.OrbInstance> orbs = self.orbs();
                    if (orbs.isEmpty()) {
                        ctx.announce("  没有充能球可以激发");
                        return;
                    }
                    Orb type = orbs.get(0).type();
                    ctx.evokeOrb(self);
                    ctx.channelOrb(self, type);
                }));
    }

    // ---------------------------------------------------------------- 状态牌（v0.0.3 W2）
    // 由怪物塞进玩家的手牌 / 弃牌堆 / 抽牌堆；不进任何卡池（rewardPool / goldPool 会排除 STATUS）。
    /** 虚空：抽到时本回合费用 −1，不能被打出。 */
    public static final Card VOID = register(new Card(
            "void", "虚空", CardType.STATUS, 0, CardTarget.SELF, "抽到时：本回合费用 -1。不能被打出。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /** 粘液：1 费，抽一张牌，消耗。 */
    public static final Card SLIMED = register(new Card(
            "slimed", "粘液", CardType.STATUS, 1, CardTarget.SELF, "抽一张牌。消耗。",
            CardRarity.COMMON, null, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.drawCards(1)));

    /** 灼烧：不能被打出；回合结束时若在手中，对自己造成 4 点伤害（可被格挡）。 */
    public static final Card BURN = register(new Card(
            "burn", "灼烧", CardType.STATUS, 0, CardTarget.SELF, "不能被打出。回合结束时若这张牌在你的手中，对你造成 4 点伤害。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /** 晕眩：不能被打出；虚无（回合结束仍在手中则被消耗）。 */
    public static final Card DAZED = register(new Card(
            "dazed", "晕眩", CardType.STATUS, 0, CardTarget.SELF, "不能被打出。虚无。",
            CardRarity.COMMON, null, Set.of(CardKeyword.ETHEREAL),
            (ctx, self, target) -> {
            }));

    /** 毒液：1 费；回合结束时若在手中，对自己造成 4 点伤害（可被格挡）。 */
    public static final Card VENOM = register(new Card(
            "venom", "毒液", CardType.STATUS, 1, CardTarget.SELF, "回合结束时若这张牌在你的手中，对你造成 4 点伤害。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /** 呼唤：1 费；回合结束时若在手中，你失去 5 点生命值（无视格挡）。 */
    public static final Card CALL = register(new Card(
            "call", "呼唤", CardType.STATUS, 1, CardTarget.SELF, "回合结束时若这张牌在你的手中，你失去 5 点生命值。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /**
     * 伤口：不能被打出，没有任何效果 —— 纯粹是"污染手牌"的状态牌（v0.5.2.1 A4-8【野性打击】用）。
     *
     * <p>它与【晕眩】的区别：晕眩带虚无（回合结束会被消耗），伤口会一直留在手里。</p>
     */
    public static final Card WOUND = register(new Card(
            "wound", "伤口", CardType.STATUS, 0, CardTarget.SELF, "不能被打出。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    // ---------------------------------------------------------------- 叛逃掠夺者（v0.0.3 W3）
    /** 集中射击：2 费 7 点伤害（升级 9）。 */
    public static final Card AIMED_SHOT = register(new Card(
            // v0.4.7.0 平衡：集中射击 2 费 → 1 费（需求原文："集中射击 2费-->1费"）
            "aimed_shot", "集中射击", CardType.ATTACK, 1, CardTarget.ENEMY, "造成 7 点伤害。",
            CardRarity.COMMON, PILLAGER,
            (ctx, self, target) -> ctx.dealDamage(self, target, 7, "「集中射击」")));

    /** 寻找掩体：2 费 9 格挡，并在下回合获得 5 点格挡（升级 11 / 7）。 */
    public static final Card TAKE_COVER = register(new Card(
            "take_cover", "寻找掩体", CardType.SKILL, 2, CardTarget.SELF, "获得 9 点格挡，在下回合获得 5 点格挡。",
            CardRarity.COMMON, PILLAGER,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(9, "「寻找掩体」");
                self.addNextTurnBlock(5);
                ctx.announce("  同时预留了 5 点格挡给下个回合");
            }));

    /** 翻滚：1 费 获得 2 点敏捷，消耗（升级：不再消耗）。 */
    public static final Card ROLL = register(new Card(
            "roll", "翻滚", CardType.SKILL, 1, CardTarget.SELF, "获得 2 点敏捷，消耗。",
            CardRarity.COMMON, PILLAGER, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                self.addDexterity(2);
                ctx.announce("  迅捷 +2" + self.statusSuffix());
            }));

    /**
     * 烟雾弹：1 费 —— **施加 2 层虚弱**，自己获得 2 点力量（升级：费用变 0）。
     *
     * <p>v0.5.1.4 平衡（需求原文）："烟雾弹 —— 失去两点力量：改为施加两层虚弱。"
     * 虚弱重做之后"2 层虚弱"= 对方每次攻击少打 2 点、**永久**，而且会先把它身上的力量抵掉
     * （正好接过旧版"失去力量"想做的事，并且对"本来就没有力量"的敌人也照样有效）。</p>
     */
    public static final Card SMOKE_BOMB = register(new Card(
            "smoke_bomb", "烟雾弹", CardType.SKILL, 1, CardTarget.ENEMY, "施加 2 层虚弱，你获得 2 点力量。",
            CardRarity.COMMON, PILLAGER,
            (ctx, self, target) -> {
                if (target != null) {
                    target.addWeak(2);
                    ctx.announce("  " + target.name() + " 获得虚弱 2" + target.statusSuffix());
                }
                self.addStrength(2);
                ctx.announce("  你获得力量 +2（当前 " + signed(self.strength()) + "）");
            }));

    // ---------------------------------------------------------------- 无色 / 状态牌（v0.0.3）
    /**
     * 箭在弦上（v0.0.3 W3）：0 费无色**状态牌**，获得活力 4。
     *
     * <p>来源是叛逃掠夺者的初始遗物【臂弩】；刻意不是任何卡池的内容（STATUS 不进奖励池/商店）。</p>
     */
    public static final Card ARROW_NOCKED = register(new Card(
            "arrow_nocked", "箭在弦上", CardType.STATUS, 0, CardTarget.SELF, "获得活力 4。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
                self.addVigor(4);
                ctx.announce("  获得活力 4（下一张攻击牌额外造成 4 点伤害）");
            }));

    // ---------------------------------------------------------------- 诅咒牌（v0.4.6 新卡牌类型）
    // 需求："你是否会懊悔过去的行为？" —— 四种诅咒都是"出牌回合结束时若还在手里就发作"。
    // 与状态牌一样**不进任何卡池**（rewardPool / goldPool / colorless 都会排除 CURSE）。

    /** 残忍：不能被打出；回合结束时若在手中，失去等同于当前手牌数量的生命值。 */
    public static final Card CURSE_CRUEL = register(new Card(
            StatusCards.CURSE_CRUEL, "残忍", CardType.CURSE, 0, CardTarget.SELF,
            "不能被打出。如果这张牌在出牌回合结束时在你的手牌中，你失去等同于当前手牌数量的生命。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /** 贪婪：不能被打出；回合结束时若在手中，失去 10 金币（不会变成负数）。 */
    public static final Card CURSE_GREED = register(new Card(
            StatusCards.CURSE_GREED, "贪婪", CardType.CURSE, 0, CardTarget.SELF,
            "不能被打出。如果这张牌在出牌回合结束时在你的手牌中，你失去 10 金币。金币数量不会为负数。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /** 戏谑：1 费，可以打出（打出后自然不会再"留在手里"）；回合结束时若在手中，复制一张进弃牌堆。 */
    public static final Card CURSE_MOCKERY = register(new Card(
            StatusCards.CURSE_MOCKERY, "戏谑", CardType.CURSE, 1, CardTarget.SELF,
            "如果这张牌在出牌回合结束时在你的手牌中，将一张本牌的复制品加入你的弃牌堆。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
                // 打出它本身没有效果：真正的代价是"它在手里会自我复制"
                ctx.announce("  你把这张牌打了出去 —— 至少这一回合它不会再繁殖了");
            }));

    /** 固执：不能被打出；回合结束时若在手中，获得 2 层易伤。 */
    public static final Card CURSE_STUBBORN = register(new Card(
            StatusCards.CURSE_STUBBORN, "固执", CardType.CURSE, 0, CardTarget.SELF,
            "不能被打出。如果这张牌在出牌回合结束时在你的手牌中，你获得 2 层易伤。",
            CardRarity.COMMON, null,
            (ctx, self, target) -> {
            }));

    /**
     * **状态与诅咒牌图鉴**（v0.4.6.2 需求）。
     *
     * <p>需求："添加&lt;状态与诅咒牌图鉴&gt;，描述：这些牌会干扰你的行动和战斗。图鉴包含：所有状态/诅咒牌。"</p>
     */
    public static List<Card> statusAndCurseCodex() {
        List<Card> list = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.type() == CardType.STATUS || card.type() == CardType.CURSE) {
                list.add(card);
            }
        }
        return List.copyOf(list);
    }

    /** 全部诅咒牌（图鉴/调试用）。 */
    public static List<Card> curses() {
        List<Card> list = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.type() == CardType.CURSE) {
                list.add(card);
            }
        }
        return List.copyOf(list);
    }

    private CardLibrary() {
    }

    private static Card register(Card card) {
        BY_ID.put(card.id(), card);
        return card;
    }

    /** 注册"强化版"定义：用同一个 id，但数值更强（v0.0.2 V11）。 */
    private static Card upgrade(Card card) {
        UPGRADED.put(card.id(), card);
        return card;
    }

    /**
     * 带符号的力量数值（v0.5.1.0）。
     *
     * <p>力量有下限 0，所以播报时写 {@code +0} 比写 {@code 0} 更能说明"它本来就没力量"。</p>
     */
    static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    public static Card byId(String id) {
        return BY_ID.get(id);
    }

    /** 强化版定义；这张卡没有强化版时返回 {@code null}。 */
    public static Card upgraded(String id) {
        return UPGRADED.get(id);
    }

    /** 这张卡能不能被强化。 */
    public static boolean canUpgrade(String id) {
        return UPGRADED.containsKey(id);
    }

    /**
     * 取"某张卡在第 N 次强化后的定义"（v0.0.2 V11）。
     *
     * <p>档案里只存 {@code upgrades} 次数；真正的数值在这里解析。目前只支持一级强化
     * （{@code upgrades >= 1} 就用强化版），没有强化版时原样返回基础定义。</p>
     */
    /**
     * **卡牌来源**（v0.4.7.0 需求："新卡牌类型：生成牌 —— 不同于角色牌和无色牌，
     * 生成牌不能在除了由遗物和一些卡牌生成并加入牌堆之外的途径获取。"）。
     *
     * <p>按需求选的方案 (a)：生成牌**不是新的稀有度**，而是一个独立的"来源"标记，
     * 所以它仍然可以是普通 / 罕见 / 稀有的牌，但**永远不进奖励池、商店、无色牌池**。</p>
     */
    public enum CardPool {
        /** 角色专属牌（奖励池里的正常牌）。 */
        CHARACTER,
        /** 无色牌（商店 / 事件）。 */
        COLORLESS,
        /** **生成牌**：只能由遗物或别的卡牌生成并加入牌堆。 */
        GENERATED,
        /** 状态牌（由怪物 / 事件塞进牌组）。 */
        STATUS,
        /** 诅咒牌（由事件获得）。 */
        CURSE
    }

    /**
     * **X 费卡**的 id 集合（v0.4.7.3）。
     *
     * <p>为什么不用"负数费用"当哨兵：{@link Card} 会把负费用夹成 0，`cost()` 读回来永远不是 -1。
     * 所以 X 费改成**独立标记**：卡面 `cost()` 是 0，是否 X 费看这个集合。</p>
     */
    private static final java.util.Set<String> X_COST = new java.util.LinkedHashSet<>();

    /** 登记一张 **X 费**卡（花光当前全部能量，X = 花掉的量）。 */
    public static Card registerXCost(Card card) {
        X_COST.add(card.id());
        return register(card);
    }

    /** 这张牌是不是 X 费（按 id）。 */
    public static boolean isXCost(String cardId) {
        return cardId != null && X_COST.contains(cardId);
    }

    /** 这张牌是不是 X 费。 */
    public static boolean isXCost(Card card) {
        return card != null && isXCost(card.id());
    }

    /** 已登记的**生成牌** id（[基础版, 强化版] 都会登记）。 */
    private static final java.util.Set<String> GENERATED = new java.util.LinkedHashSet<>();

    /** 登记一张生成牌：它不会出现在任何卡池里（见 {@link #poolOf(Card)}）。 */
    public static Card registerGenerated(Card card) {
        GENERATED.add(card.id());
        return register(card);
    }

    /** 登记一对生成牌（基础版 + 强化版）。 */
    public static Card registerGenerated(Card base, Card upgraded) {
        registerGenerated(base);
        if (upgraded != null) {
            registerGenerated(upgraded);
        }
        return base;
    }

    /** 这张牌是不是**生成牌**（没有强化版的生成牌也照样是生成牌）。 */
    public static boolean isGenerated(String cardId) {
        return cardId != null && GENERATED.contains(cardId);
    }

    /** 已登记的生成牌数量（自检用）。 */
    public static int generatedCount() {
        return GENERATED.size();
    }

    // ================== v0.5.1.4 多人卡牌 与 魔术技巧的费用档 ==================
    // v0.5.2.5 消歧义（用户）：下面那五张**不是"仅多人模式卡牌"，是自带卡牌** ——
    // 它们照常进奖励池与商店（单人也能拿到），MULTIPLAYER_STYLE 只用来给图鉴分一页。

    /**
     * **多人玩法向的卡牌** id（v0.5.1.4 加的集合；v0.5.2.5 消歧义后**不再等于"仅多人可获得"**）。
     *
     * <p>v0.5.1.4 的需求是"这些卡牌只有在多人模式下才会从奖励池里出现"，
     * 但用户 v0.5.2.5 明确纠正："魔术技巧！、村庄英雄、沉船打捞、我们团结一心！、快哉，肉排
     * 这五张牌其实不是仅多人模式卡牌，是**自带卡牌**。"</p>
     *
     * <p>所以 v0.5.2.5 起：它们**照常进奖励池 / 商店**（单人也能拿到），这个集合只用来给图鉴分页。</p>
     *
     * <p>v0.5.5.1（用户）："现在的多人模式卡牌图鉴应该**只有：商队护送**。"
     * 所以这个集合现在只登记【商队护送】一张 —— 那五张自带卡牌不再单独分页（它们在各自角色的图鉴里）。</p>
     */
    private static final java.util.Set<String> MULTIPLAYER_STYLE = new java.util.LinkedHashSet<>(
            java.util.Set.of(
            // v0.5.3.1：用户给的第五张叛逃掠夺者新卡是"多人玩法向"（你与队友）
            "caravan_escort"));

    /** 这张牌是不是**多人玩法向**（图鉴分页用；不代表"只能多人获得"）。 */
    public static boolean isMultiplayerStyle(String cardId) {
        return cardId != null && MULTIPLAYER_STYLE.contains(cardId);
    }

    /** 多人玩法向卡牌数（自检用）。 */
    public static int multiplayerCount() {
        return MULTIPLAYER_STYLE.size();
    }

    /** **多人玩法向卡牌图鉴**的内容（排序交给 {@link #sortedForCodex}）。 */
    public static List<Card> multiplayerCodex() {
        List<Card> out = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (isMultiplayerStyle(card.id())) {
                out.add(card);
            }
        }
        return out;
    }

    /** 【魔术技巧！】的基础费用（每减 1 费就是一个独立定义，见 {@link #magicTrickId(int)}）。 */
    public static final int MAGIC_TRICK_COST = 10;

    /** 【魔术技巧！】最多能减到 0 费。 */
    public static final int MAGIC_TRICK_MAX_DISCOUNT = MAGIC_TRICK_COST;

    /** 这张牌是不是【魔术技巧！】的某一档费用。 */
    public static boolean isMagicTrick(String cardId) {
        return cardId != null && (cardId.equals("magic_trick") || cardId.startsWith("magic_trick_d"));
    }

    /** 这张牌当前减了几费（解析 id 后缀；不是魔术技巧时返回 0）。 */
    public static int magicTrickDiscount(String cardId) {
        if (cardId == null || !cardId.startsWith("magic_trick_d")) {
            return 0;
        }
        try {
            return Integer.parseInt(cardId.substring("magic_trick_d".length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 减了 {@code discount} 费时的卡牌 id。 */
    public static String magicTrickId(int discount) {
        return discount <= 0 ? "magic_trick" : "magic_trick_d" + discount;
    }

    /** 这张牌的**来源分类**（图鉴"来源"与卡池过滤共用这一份判定）。 */
    public static CardPool poolOf(Card card) {
        if (card == null) {
            return CardPool.CHARACTER;
        }
        if (isGenerated(card.id())) {
            return CardPool.GENERATED;
        }
        if (card.type() == CardType.STATUS) {
            return CardPool.STATUS;
        }
        if (card.type() == CardType.CURSE) {
            return CardPool.CURSE;
        }
        return card.character() == null ? CardPool.COLORLESS : CardPool.CHARACTER;
    }

    /** 这张牌能不能通过"奖励 / 商店 / 无色池"等正常途径拿到。 */
    public static boolean obtainable(Card card) {
        CardPool pool = poolOf(card);
        return pool == CardPool.CHARACTER || pool == CardPool.COLORLESS;
    }

    // ================== v0.5.4.1（A4-20）多次强化 ==================

    /**
     * **可以强化超过一次**的卡 id → 最多几次（v0.5.4.1 A4-20【灼热打击】）。
     *
     * <p>其余卡一律走 {@link #UPGRADED} 的老路（0 / 1 两级）。原版【灼热打击】写的是
     * 「このカードは何度でもアップグレードできる」= **任意次**，所以这里是
     * {@link Integer#MAX_VALUE}，档位定义按需要懒生成（见 {@link #searingBlowCard(int)}）。</p>
     */
    private static final java.util.Map<String, Integer> MAX_UPGRADES = new java.util.LinkedHashMap<>();

    /** 【灼热打击】每次强化的伤害增量（基础 12，每档 +4）。 */
    public static final int SEARING_BLOW_DAMAGE_STEP = 4;

    /** 【灼热打击】的基础伤害。 */
    public static final int SEARING_BLOW_BASE_DAMAGE = 12;

    /** 【灼热打击】的档位定义缓存（{@code 强化次数 → 定义}），懒生成。 */
    private static final java.util.Map<Integer, Card> SEARING_BLOW_TIERS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 取"某张卡在第 N 次强化后的定义"（v0.0.2 V11；v0.5.4.1 支持多次强化）。
     *
     * <p>档案里只存 {@code upgrades} 次数；真正的数值在这里解析。普通卡：
     * {@code upgrades >= 1} 就用强化版，没有强化版时原样返回基础定义。
     * 多次强化的卡（【灼热打击】）按档位取，超出上限就取最高档。</p>
     */
    public static Card resolve(String id, int upgrades) {
        Card base = BY_ID.get(id);
        if (base == null) {
            return null;
        }
        if (isMultiUpgrade(id)) {
            return searingBlowCard(upgrades);
        }
        Card upgraded = UPGRADED.get(id);
        return upgrades > 0 && upgraded != null ? upgraded : base;
    }

    /** 这张卡是不是"可以强化多次"的那种（目前只有【灼热打击】）。 */
    public static boolean isMultiUpgrade(String cardId) {
        return cardId != null && MAX_UPGRADES.containsKey(cardId);
    }

    /**
     * 这张卡**最多能强化几次**（v0.5.4.1）。
     *
     * <p>普通卡 = 1（有强化版）或 0（没有）；【灼热打击】= {@link Integer#MAX_VALUE}。
     * 卡牌档案里"还能不能强化"一律问这里，不再写死"{@code upgrades == 0}"。</p>
     */
    public static int maxUpgrades(String cardId) {
        if (cardId == null) {
            return 0;
        }
        Integer custom = MAX_UPGRADES.get(cardId);
        if (custom != null) {
            return custom;
        }
        return UPGRADED.containsKey(cardId) ? 1 : 0;
    }

    /** 这张定义最多能强化几次。 */
    public static int maxUpgrades(Card card) {
        return card == null ? 0 : maxUpgrades(card.id());
    }

    /**
     * 这张**定义**已经处于第几档强化（0 = 基础版）。
     *
     * <p>为什么需要一个"反查"：本引擎的 {@code Card} 是不可变共享定义，手里的牌只有定义、
     * 没有"我是第几档"的字段，所以【军备】这种"本场战斗升级一张手牌"必须从定义反推档位。</p>
     *
     * <p>比较用 {@link #sameDefinition}（忽略附魔）：带附魔的手牌是
     * {@code withEnchantments} 造出来的**新对象**，引用比较会认不出来。</p>
     */
    public static int upgradeLevel(Card card) {
        if (card == null) {
            return 0;
        }
        if (isMultiUpgrade(card.id())) {
            return searingBlowLevel(card.name());
        }
        return sameDefinition(UPGRADED.get(card.id()), card) ? 1 : 0;
    }

    /** 这张定义**再强化一档**（到顶就原样返回；没有强化版也原样返回）。 */
    public static Card nextUpgradeOf(Card card) {
        if (card == null) {
            return null;
        }
        int level = upgradeLevel(card);
        if (level >= maxUpgrades(card.id())) {
            return card;
        }
        Card next = resolve(card.id(), level + 1);
        return next == null ? card : next;
    }

    /** 两份定义是不是"同一档"（**忽略附魔**：附魔是实例数据，见 {@code Card.withEnchantments}）。 */
    private static boolean sameDefinition(Card a, Card b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b || (a.id().equals(b.id()) && a.name().equals(b.name()) && a.cost() == b.cost()
                && a.type() == b.type() && a.description().equals(b.description()));
    }

    /**
     * 【灼热打击】第 {@code level} 档的定义（懒生成 + 缓存）。
     *
     * <p>为什么懒生成而不是预先登记 20 张：原版是"任意次"，预先登记只能封顶；
     * 而且这些档位**不进 {@link #BY_ID}**，所以奖励池、图鉴、卡牌总数都不受影响
     * （图鉴里只出现基础版一张）。</p>
     */
    public static Card searingBlowCard(int level) {
        int clamped = Math.max(0, level);
        return SEARING_BLOW_TIERS.computeIfAbsent(clamped, CardLibrary::newSearingBlowCard);
    }

    private static Card newSearingBlowCard(int level) {
        int damage = SEARING_BLOW_BASE_DAMAGE + SEARING_BLOW_DAMAGE_STEP * level;
        String name = searingBlowName(level);
        return new Card("searing_blow", name, CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 " + damage + " 点伤害。这张牌可以被强化任意次。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, damage, "「" + name + "」"));
    }

    /** 第 N 档的卡名（{@code 灼热打击} / {@code 灼热打击+} / {@code 灼热打击+7}）。 */
    private static String searingBlowName(int level) {
        if (level <= 0) {
            return "灼热打击";
        }
        return level == 1 ? "灼热打击+" : "灼热打击+" + level;
    }

    /** 从卡名反查【灼热打击】的档位（认不出来按 0 处理）。 */
    private static int searingBlowLevel(String name) {
        if (name == null) {
            return 0;
        }
        int plus = name.indexOf('+');
        if (plus < 0) {
            return 0;
        }
        String tail = name.substring(plus + 1).trim();
        if (tail.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(tail));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static List<Card> all() {
        return List.copyOf(BY_ID.values());
    }

    /**
     * 这张牌**有没有强化版**（v0.4.7.0）。
     *
     * <p>注意：{@link #resolve(String, int)} 在没有强化版时会**回退成基础牌**，所以判断
     * "能不能查看升级"必须用这个方法，不然每张牌都会显示「查看升级」按钮。</p>
     */
    public static boolean hasUpgrade(String cardId) {
        return cardId != null && UPGRADED.containsKey(cardId);
    }

    /** 可以被强化的卡 id 列表（调试指令与自检用）。 */
    public static List<String> upgradableIds() {
        return List.copyOf(UPGRADED.keySet());
    }

    /**
     * **基础牌**（打击 / 格挡）：每个角色的起始牌组都有它们，所以每个角色的池子里都算一份。
     *
     * <p>v0.4.0 的规则："不同角色不能用共同的卡牌池" —— 基础牌是唯一的例外（它们在所有角色的
     * 起始牌组里，本身不属于任何角色）。</p>
     */
    private static final Set<String> BASIC_CARD_IDS = Set.of("strike", "defend");

    /** 这张牌是不是基础牌（打击 / 格挡）。 */
    public static boolean isBasic(String cardId) {
        return cardId != null && BASIC_CARD_IDS.contains(cardId);
    }

    /**
     * 某角色可抽到的**普通奖励池**（v0.4.0 重做）。
     *
     * <p>规则（需求原文）："不同角色不能用共同的卡牌池，每个角色都有自己的卡牌池。
     * 无色牌不应该出现在卡牌奖励中。" 于是：</p>
     * <ul>
     *   <li>只收**归属该角色**的牌 + 基础牌（打击 / 格挡）；</li>
     *   <li>无色牌（{@code character == null}）**不进奖励池** —— 它们只从问号事件 / 遗物获得；</li>
     *   <li>金色牌走 {@link #goldPool(String)}；状态牌/诅咒牌永远不进池子。</li>
     * </ul>
     *
     * @param characterId 角色 id 字符串，例如 {@code stc:ironclad}；null 返回空（无色牌不进池子）
     */
    public static List<Card> rewardPool(String characterId) {
        // 单人视角（v0.5.1.4 起多人卡牌只在多人模式进池，见下面的重载）
        return rewardPool(characterId, 1);
    }

    /**
     * 某角色的**奖励池**（v0.5.1.4 起按队伍人数过滤）。
     *
     * <p>需求："新卡牌类型：多人卡牌 —— 这些卡牌只有在多人模式下才会从奖励池里出现。"</p>
     *
     * <p>顺带把**生成牌**彻底挡在池子外：以前只靠"生成牌的 character 是 null"来回避，
     * 而弩箭射击 / 适应打击的 0 费复制品其实都带着角色 id、于是就悄悄混进了奖励池
     * （{@link #obtainable(Card)} 本来就是为这件事写的判定，只是这里没用上）。</p>
     *
     * @param partySize 队伍人数（{@code <= 1} = 单人，多人卡牌不进池）
     */
    public static List<Card> rewardPool(String characterId, int partySize) {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.rarity() == CardRarity.GOLD || card.type() == CardType.STATUS
                    || card.type() == CardType.CURSE) {
                // v0.0.3：状态牌/诅咒牌不进任何卡池（它们只能由怪物塞给你）
                continue;
            }
            if (!obtainable(card)) {
                // v0.5.1.4：生成牌（弩箭射击 / 0 费复制品 / 魔术技巧的费用档）从来不属于任何卡池
                continue;
            }
            // v0.5.2.5：多人玩法向的那 5 张"自带卡牌"**单人也能抽到**（不再被排除）。
            // v0.5.5.2（用户 B7）：但【商队护送】**"在队伍只有 1 名玩家时，不加入奖励池"** ——
            // 它现在就是这个集合里唯一的卡，所以规则写成"多人玩法向的卡单人时不进池"。
            if (partySize <= 1 && isMultiplayerStyle(card.id())) {
                continue;
            }
            if (isInPool(card, characterId)) {
                pool.add(card);
            }
        }
        return pool;
    }

    /** 这张牌是否属于某角色的池子（基础牌人人有份；无色牌不属于任何人）。 */
    private static boolean isInPool(Card card, String characterId) {
        if (characterId == null) {
            return false;
        }
        if (isBasic(card.id())) {
            return true;
        }
        return characterId.equals(card.character());
    }

    /**
     * 某角色的 **BOSS 奖励池**：只收该角色的金色牌。
     *
     * <p>⚠️ v0.5.1.5 起**这个池子是空的** —— 需求"统一目前每个角色的金色和稀有卡，
     * 全部将品质统一为稀有"，所以原来的 5 张金色牌（含强化版共 10 个定义）品质都改成了稀有。
     * 方法保留只是不想破坏附属包 / 旧调用点的引用；**BOSS 奖励现在走 {@link #rarePool(String)}**。</p>
     */
    public static List<Card> goldPool(String characterId) {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.rarity() == CardRarity.GOLD && card.type() != CardType.STATUS
                    && isInPool(card, characterId)) {
                pool.add(card);
            }
        }
        return pool;
    }

    /**
     * 某角色的**稀有卡池**（v0.5.1.5：BOSS 奖励改成从这里抽）。
     *
     * <p>需求原文："BOSS奖励给予的『金色卡奖励』实际上应该是从当前角色的稀有卡池抽取奖励。
     * 统一目前每个角色的金色和稀有卡，全部将品质统一为稀有。"</p>
     *
     * <p>池子规则与普通奖励一致（可获得、非状态非诅咒、多人卡牌按人数过滤），
     * 只是把品质限定成稀有。原来的金色牌现在就在这个池子里 —— 也就是说它们**从 BOSS 专属变成了普通稀有卡**，
     * 普通战斗奖励与商店也会出现。</p>
     */
    public static List<Card> rarePool(String characterId) {
        return rarePool(characterId, 1);
    }

    public static List<Card> rarePool(String characterId, int partySize) {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.rarity() != CardRarity.RARE || card.type() == CardType.STATUS
                    || card.type() == CardType.CURSE) {
                continue;
            }
            if (!obtainable(card)) {
                continue;
            }
            // v0.5.5.2（用户 B7）：多人玩法向的卡（=【商队护送】）单人时不进池，与 rewardPool 同一规则
            if (partySize <= 1 && isMultiplayerStyle(card.id())) {
                continue;
            }
            if (isInPool(card, characterId)) {
                pool.add(card);
            }
        }
        return pool;
    }

    /** 全部金色牌（图鉴/调试用；v0.5.1.5 起应为**空**）。 */
    public static List<Card> allGold() {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.rarity() == CardRarity.GOLD && card.type() != CardType.STATUS) {
                pool.add(card);
            }
        }
        return pool;
    }

    /** 全部稀有牌（v0.5.1.5：验证"金色已统一成稀有"的入口）。 */
    public static List<Card> allRare() {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.rarity() == CardRarity.RARE && card.type() != CardType.STATUS) {
                pool.add(card);
            }
        }
        return pool;
    }

    /** **无色牌**（不属于任何角色）：只从问号事件 / 遗物获得，不进奖励池。 */
    public static List<Card> colorless() {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.character() == null && !isBasic(card.id()) && card.type() != CardType.STATUS
                    && card.type() != CardType.CURSE
                    // v0.5.1.4：生成牌（例如魔术技巧的费用档）不是无色牌，不能混进无色牌图鉴
                    && !isGenerated(card.id())) {
                pool.add(card);
            }
        }
        return pool;
    }

    /**
     * 某角色的**卡牌图鉴内容**（v0.4.0 的"查看图鉴"用）：该角色专属牌 + 基础牌 + 该角色的金色牌。
     *
     * <p>v0.4.1：**无色牌从角色图鉴里独立出去了**（需求："将无色牌从图鉴独立出去。形成：无色牌图鉴。"），
     * 用 {@link #colorlessCodex()} 单独查看。</p>
     */
    /**
     * **图鉴排序**（v0.4.7.0 需求："从上到下按照普通，罕见，稀有品质筛选并分类卡牌"）。
     *
     * <p>品质优先（枚举顺序就是 普通 → 罕见 → 稀有 → 金色），同品质内按卡名排，方便查找。</p>
     */
    public static List<Card> sortedForCodex(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(java.util.Comparator.comparingInt((Card card) -> card.rarity().ordinal())
                .thenComparing(Card::name));
        return List.copyOf(sorted);
    }

    public static List<Card> codexFor(String characterId) {
        List<Card> pool = new ArrayList<>();
        for (Card card : BY_ID.values()) {
            if (card.type() == CardType.STATUS || card.type() == CardType.CURSE) {
                continue;
            }
            // v0.5.1.4：生成牌（费用档等）不是"这个角色的牌"
            // v0.5.2.5：多人玩法向的牌**留在常规图鉴里**（它们也是叛逃掠夺者的自带卡牌），
            //            只是图鉴主页额外给了它们一页汇总
            if (isGenerated(card.id())) {
                continue;
            }
            if (isInPool(card, characterId)) {
                pool.add(card);
            }
        }
        return pool;
    }

    /**
     * **无色牌图鉴**（v0.4.1）：不属于任何角色的牌（只从问号事件 / 遗物获得）。
     *
     * <p>不含基础牌（打击 / 格挡）与状态牌。</p>
     */
    public static List<Card> colorlessCodex() {
        return colorless();
    }

    /** 某角色的起始牌组（含重复），按 id 顺序展开。 */
    public static List<Card> deckOf(List<String> cardIds) {
        List<Card> deck = new ArrayList<>(cardIds.size());
        for (String id : cardIds) {
            Card card = BY_ID.get(id);
            if (card != null) {
                deck.add(card);
            }
        }
        return deck;
    }

    // ------------------------------------------------------------------ v0.4.7.0 叛逃掠夺者新卡

    /** 弩箭射击（生成牌）：0 费 普通 攻击，造成 5 点伤害；升级 7 点。 */
    public static final Card BOLT_SHOT = registerGenerated(new Card(
            "bolt_shot", "弩箭射击", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 5 点伤害。",
            CardRarity.COMMON, PILLAGER,
            (ctx, self, target) -> ctx.dealDamage(self, target, 5 + self.boltBonus(), "「弩箭射击」")));

    // ================== v0.5.0.3 战士第一批 D（剑气 / 破灭 / 壁垒）
    public static final Card SWORD_BOOMERANG = register(new Card(
            "sword_boomerang", "剑气", CardType.ATTACK, 1, CardTarget.SELF,
            "对随机敌人造成 3 点伤害 3 次。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> ctx.dealRandomDamage(self, 3, 3, "「剑气」")));

    static {
        upgrade(new Card("sword_boomerang", "剑气+", CardType.ATTACK, 1, CardTarget.SELF,
                "对随机敌人造成 4 点伤害 3 次。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealRandomDamage(self, 3, 4, "「剑气+」")));
    }

    public static final Card RUPTURE = register(new Card(
            "rupture", "破灭", CardType.POWER, 1, CardTarget.SELF,
            "每当你因自己的牌失去生命时，获得 1 点力量。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                self.addRupture(1);
                ctx.announce("  破灭：失去生命时将获得力量");
            }));

    static {
        upgrade(new Card("rupture", "破灭+", CardType.POWER, 1, CardTarget.SELF,
                "每当你因自己的牌失去生命时，获得 2 点力量。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    self.addRupture(2);
                    ctx.announce("  破灭+：失去生命时将获得 2 点力量");
                }));
    }

    public static final Card BARRICADE = register(new Card(
            "barricade", "壁垒", CardType.POWER, 3, CardTarget.SELF,
            "格挡不再会在回合开始时消失。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> {
                self.setBarricade(true);
                ctx.announce("  壁垒：格挡不会在回合开始时消失");
            }));

    static {
        upgrade(new Card("barricade", "壁垒+", CardType.POWER, 2, CardTarget.SELF,
                "格挡不再会在回合开始时消失。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> {
                    self.setBarricade(true);
                    ctx.announce("  壁垒+：格挡不会在回合开始时消失");
                }));
    }

    // ================== v0.5.0.2 战士第一批 C（战斗三昧 / 血祭 / 大扫除）
    public static final Card BATTLE_TRANCE = register(new Card(
            "battle_trance", "战斗三昧", CardType.SKILL, 0, CardTarget.SELF,
            "抽 3 张牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> ctx.drawCards(3)));

    static {
        upgrade(new Card("battle_trance", "战斗三昧+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 4 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> ctx.drawCards(4)));
    }

    public static final Card BLOODLETTING = register(new Card(
            "bloodletting", "血祭", CardType.SKILL, 0, CardTarget.SELF,
            "失去 3 点生命。获得 2 点能量。消耗。", CardRarity.UNCOMMON, IRONCLAD,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                ctx.loseHp(self, 3, "「血祭」");
                ctx.gainEnergy(self, 2);
            }));

    static {
        upgrade(new Card("bloodletting", "血祭+", CardType.SKILL, 0, CardTarget.SELF,
                "失去 3 点生命。获得 3 点能量。消耗。", CardRarity.UNCOMMON, IRONCLAD,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    ctx.loseHp(self, 3, "「血祭」");
                    ctx.gainEnergy(self, 3);
                }));
    }

    public static final Card REAPER = register(new Card(
            "reaper", "大扫除", CardType.ATTACK, 2, CardTarget.SELF,
            "对所有敌人造成 4 点伤害。回复等同于未被格挡伤害的生命。消耗。", CardRarity.RARE, IRONCLAD,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                int healed = 0;
                for (Enemy enemy : ctx.aliveEnemies()) {
                    healed += ctx.dealDamage(self, enemy, 4, "「大扫除」");
                }
                if (healed > 0) {
                    self.heal(healed);
                    ctx.announce("  大扫除：回复 " + healed + " 点生命（HP " + self.hp() + "/" + self.maxHp() + "）");
                }
            }));

    static {
        upgrade(new Card("reaper", "大扫除+", CardType.ATTACK, 2, CardTarget.SELF,
                "对所有敌人造成 5 点伤害。回复等同于未被格挡伤害的生命。消耗。", CardRarity.RARE, IRONCLAD,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    int healed = 0;
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        healed += ctx.dealDamage(self, enemy, 5, "「大扫除+」");
                    }
                    if (healed > 0) {
                        self.heal(healed);
                    }
                }));
    }

    // ================== v0.5.0.1 战士第一批 B（愤怒 / 旋风斩 / 牺牲）
    public static final Card ANGER = register(new Card(
            "anger", "愤怒", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 6 点伤害。将一张本牌的复制加入你的弃牌堆。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 6, "「愤怒」");
                ctx.stateOf(self).discardPile.add(resolve("anger", 0));
            }));

    static {
        upgrade(new Card("anger", "愤怒+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 8 点伤害。将一张本牌的复制加入你的弃牌堆。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 8, "「愤怒+」");
                    ctx.stateOf(self).discardPile.add(resolve("anger", 1));
                }));
    }

    public static final Card WHIRLWIND = registerXCost(new Card(
            "whirlwind", "旋风斩", CardType.ATTACK, 0, CardTarget.SELF,
            "对所有敌人造成 X 次 5 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                int x = ctx.lastX(self);
                for (int i = 0; i < x; i++) {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 5, "「旋风斩」");
                    }
                }
            }));

    static {
        upgrade(new Card("whirlwind", "旋风斩+", CardType.ATTACK, 0, CardTarget.SELF,
                "对所有敌人造成 X 次 7 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    int x = ctx.lastX(self);
                    for (int i = 0; i < x; i++) {
                        for (Enemy enemy : ctx.aliveEnemies()) {
                            ctx.dealDamage(self, enemy, 7, "「旋风斩+」");
                        }
                    }
                }));
    }

    public static final Card OFFERING = register(new Card(
            "offering", "牺牲", CardType.SKILL, 0, CardTarget.SELF,
            "失去 6 点生命。获得 2 点能量。抽 3 张牌。消耗。", CardRarity.RARE, IRONCLAD,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                ctx.loseHp(self, 6, "「牺牲」");
                ctx.gainEnergy(self, 2);
                ctx.drawCards(3);
            }));

    static {
        upgrade(new Card("offering", "牺牲+", CardType.SKILL, 0, CardTarget.SELF,
                "失去 6 点生命。获得 2 点能量。抽 5 张牌。消耗。", CardRarity.RARE, IRONCLAD,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    ctx.loseHp(self, 6, "「牺牲」");
                    ctx.gainEnergy(self, 2);
                    ctx.drawCards(5);
                }));
    }

    // ================== v0.5.0.0 战士第一批 A（重刃 / 双重打击 / 猛击）
    public static final Card HEAVY_BLADE = register(new Card(
            "heavy_blade", "重刃", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 14 点伤害。力量对这张牌生效 3 次。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> ctx.dealDamage(self, target, 14 + self.strength() * 2, "「重刃」")));

    static {
        upgrade(new Card("heavy_blade", "重刃+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 18 点伤害。力量对这张牌生效 5 次。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, 18 + self.strength() * 4, "「重刃+」")));
    }

    public static final Card TWIN_STRIKE = register(new Card(
            "twin_strike", "双重打击", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 5 点伤害 2 次。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 5, "「双重打击」");
                ctx.dealDamage(self, target, 5, "「双重打击」");
            }));

    static {
        upgrade(new Card("twin_strike", "双重打击+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 7 点伤害 2 次。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 7, "「双重打击+」");
                    ctx.dealDamage(self, target, 7, "「双重打击+」");
                }));
    }

    public static final Card CLOTHESLINE = register(new Card(
            "clothesline", "猛击", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 12 点伤害，给予 2 层虚弱。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 12, "「猛击」");
                if (target != null && target.alive()) {
                    target.addWeak(2);
                }
            }));

    static {
        upgrade(new Card("clothesline", "猛击+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 14 点伤害，给予 3 层虚弱。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 14, "「猛击+」");
                    if (target != null && target.alive()) {
                        target.addWeak(3);
                    }
                }));
    }

    // ================================================================== v0.4.9.5：机宝收尾三张
    /** 静电放电：1 费 罕见 能力牌 —— 每当你受到攻击伤害时，生成 1 颗闪电球（升级 2 颗）。 */
    public static final Card STATIC_DISCHARGE = register(new Card(
            "static_discharge", "静电放电", CardType.POWER, 1, CardTarget.SELF,
            "每当你受到攻击伤害时，生成 1 颗闪电球。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                self.addStaticDischarge(1);
                ctx.announce("  静电放电：受伤时将生成闪电球");
            }));

    static {
        upgrade(new Card("static_discharge", "静电放电+", CardType.POWER, 1, CardTarget.SELF,
                "每当你受到攻击伤害时，生成 2 颗闪电球。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    self.addStaticDischarge(2);
                    ctx.announce("  静电放电+：受伤时将生成 2 颗闪电球");
                }));
    }

    /** 循环：1 费 罕见 能力牌 —— 每回合开始时触发充能球被动 1 次（升级 2 次）。 */
    public static final Card LOOP = register(new Card(
            "loop", "循环", CardType.POWER, 1, CardTarget.SELF,
            "每回合开始时，触发你的充能球被动 1 次。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                self.addLoopCount(1);
                ctx.announce("  循环：每回合开始将额外触发球被动");
            }));

    static {
        upgrade(new Card("loop", "循环+", CardType.POWER, 1, CardTarget.SELF,
                "每回合开始时，触发你的充能球被动 2 次。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    self.addLoopCount(2);
                    ctx.announce("  循环+：每回合开始将额外触发球被动 2 次");
                }));
    }

    /** 创造力：3 费 稀有 能力牌 —— 每回合开始时随机获得 1 张能力牌。 */
    public static final Card CREATIVE_AI = register(new Card(
            "creative_ai", "创造力", CardType.POWER, 3, CardTarget.SELF,
            "每回合开始时，随机获得 1 张能力牌。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> {
                self.setCreativeAI(true);
                ctx.announce("  创造力：每回合开始将随机获得 1 张能力牌");
            }));

    static {
        upgrade(new Card("creative_ai", "创造力+", CardType.POWER, 2, CardTarget.SELF,
                "每回合开始时，随机获得 1 张能力牌。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> {
                    self.setCreativeAI(true);
                    ctx.announce("  创造力+：每回合开始将随机获得 1 张能力牌");
                }));
    }

    // ================================================================== v0.4.9.4：增幅器
    /** 增幅器：1 费 罕见 能力牌 —— 每回合你首次激发充能球时，额外触发一次（升级 0 费）。 */
    public static final Card AMPLIFY = register(new Card(
            "amplify", "增幅器", CardType.POWER, 1, CardTarget.SELF,
            "每回合你首次激发充能球时，额外触发一次。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                self.setEvokeBonusOnce(true);
                ctx.announce("  增幅器：本回合首次激发将额外触发一次");
            }));

    static {
        upgrade(new Card("amplify", "增幅器+", CardType.POWER, 0, CardTarget.SELF,
                "每回合你首次激发充能球时，额外触发一次。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    self.setEvokeBonusOnce(true);
                    ctx.announce("  增幅器+：本回合首次激发将额外触发一次");
                }));
    }

    // ================================================================== v0.4.9.3：机宝第九批
    /** 启动：0 费 普通 技能 消耗 —— 获得 10 点格挡（升级 13 点）。 */
    public static final Card BOOT_SEQUENCE = register(new Card(
            "boot_sequence", "启动", CardType.SKILL, 0, CardTarget.SELF,
            "获得 10 点格挡。消耗。", CardRarity.COMMON, DEFECT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.grantPlayerBlock(10, "「启动」")));

    static {
        upgrade(new Card("boot_sequence", "启动+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 13 点格挡。消耗。", CardRarity.COMMON, DEFECT,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.grantPlayerBlock(13, "「启动+」")));
    }

    /** 电容器：1 费 普通 能力牌 —— 获得 2 个充能球栏位（升级 3 个）。 */
    public static final Card CAPACITOR = register(new Card(
            "capacitor", "电容器", CardType.POWER, 1, CardTarget.SELF,
            "获得 2 个充能球栏位。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                self.setOrbSlots(self.orbSlots() + 2);
                ctx.announce("  电容器：球位变成 " + self.orbSlots());
            }));

    static {
        upgrade(new Card("capacitor", "电容器+", CardType.POWER, 1, CardTarget.SELF,
                "获得 3 个充能球栏位。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    self.setOrbSlots(self.orbSlots() + 3);
                    ctx.announce("  电容器+：球位变成 " + self.orbSlots());
                }));
    }

    /** 白噪声：1 费 罕见 技能 —— 从 3 张能力牌里随机拿 1 张加入手牌，本回合费用为 0（升级 0 费）。 */
    public static final Card WHITE_NOISE = register(new Card(
            "white_noise", "白噪声", CardType.SKILL, 1, CardTarget.SELF,
            "随机获得 1 张能力牌（缓冲 / 碎片整理 / 电动力学）。消耗。", CardRarity.UNCOMMON, DEFECT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> whiteNoiseEffect(ctx, self)));

    static {
        upgrade(new Card("white_noise", "白噪声+", CardType.SKILL, 0, CardTarget.SELF,
                "随机获得 1 张能力牌（缓冲 / 碎片整理 / 电动力学）。消耗。", CardRarity.UNCOMMON, DEFECT,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> whiteNoiseEffect(ctx, self)));
    }

    /** 白噪声：从三张机宝能力牌里随机拿一张进手牌。 */
    private static void whiteNoiseEffect(CombatContext ctx, Combatant self) {
        Card[] pool = {BUFFER, DEFRAGMENT, ELECTRODYNAMICS};
        Card picked = pool[ctx.nextRandomInt(pool.length)];
        ctx.addCardsToHand(self, picked, 1);
        ctx.announce("  白噪声：获得「" + picked.name() + "」");
    }

    // ================================================================== v0.4.9.1：电动力学
    /** 电动力学：2 费 稀有 能力牌 —— 你的闪电球被动改为对所有敌人结算。 */
    public static final Card ELECTRODYNAMICS = register(new Card(
            "electrodynamics", "电动力学", CardType.POWER, 2, CardTarget.SELF,
            "你的闪电球被动改为对所有敌人结算。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> {
                self.setLightningHitsAll(true);
                ctx.announce("  电动力学：闪电球被动将对所有敌人结算");
            }));

    static {
        upgrade(new Card("electrodynamics", "电动力学+", CardType.POWER, 2, CardTarget.SELF,
                "你的闪电球被动改为对所有敌人结算。生成 1 颗闪电球。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> {
                    self.setLightningHitsAll(true);
                    ctx.channelOrb(self, Orb.LIGHTNING);
                }));
    }

    // ================================================================== v0.4.9.0：机宝第八批（7 张）
    /** 风暴：X 费 稀有 技能 —— 生成 X 颗闪电球（升级 X+1 颗）。 */
    public static final Card TEMPEST = registerXCost(new Card(
            "tempest", "风暴", CardType.SKILL, 0, CardTarget.SELF,
            "生成 X 颗闪电球。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> tempestEffect(ctx, self, 0)));

    static {
        upgrade(new Card("tempest", "风暴+", CardType.SKILL, 0, CardTarget.SELF,
                "生成 X+1 颗闪电球。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> tempestEffect(ctx, self, 1)));
    }

    /** 双重能量：1 费 罕见 技能 —— 你的能量翻倍（升级 0 费）。 */
    public static final Card DOUBLE_ENERGY = register(new Card(
            "double_energy", "双重能量", CardType.SKILL, 1, CardTarget.SELF,
            "将你的能量翻倍。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> ctx.gainEnergy(self, ctx.energy())));

    static {
        upgrade(new Card("double_energy", "双重能量+", CardType.SKILL, 0, CardTarget.SELF,
                "将你的能量翻倍。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> ctx.gainEnergy(self, ctx.energy())));
    }

    /** 碎片整理：1 费 罕见 能力牌 —— 获得 1 点集中（升级 2 点）。 */
    public static final Card DEFRAGMENT = register(new Card(
            "defragment", "碎片整理", CardType.POWER, 1, CardTarget.SELF,
            "获得 1 点集中。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> defragmentEffect(ctx, self, 1)));

    static {
        upgrade(new Card("defragment", "碎片整理+", CardType.POWER, 1, CardTarget.SELF,
                "获得 2 点集中。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> defragmentEffect(ctx, self, 2)));
    }

    /** 冰雹：0 费 罕见 技能 —— 每有一名敌人就生成 1 颗冰球（升级 2 颗）。 */
    public static final Card CHILL = register(new Card(
            "chill", "冰雹", CardType.SKILL, 0, CardTarget.SELF,
            "每有一名敌人，生成 1 颗冰球。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> chillEffect(ctx, self, 1)));

    static {
        upgrade(new Card("chill", "冰雹+", CardType.SKILL, 0, CardTarget.SELF,
                "每有一名敌人，生成 2 颗冰球。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> chillEffect(ctx, self, 2)));
    }

    /** 刮削：1 费 罕见 攻击 —— 7 伤害；抽 4 张牌；弃掉所有费用不为 0 的手牌（升级 10 伤害）。 */
    public static final Card SCRAPE = register(new Card(
            "scrape", "刮削", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 7 点伤害。抽 4 张牌。弃掉所有费用不为 0 的牌。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> scrapeEffect(ctx, self, target, 7)));

    static {
        upgrade(new Card("scrape", "刮削+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 10 点伤害。抽 4 张牌。弃掉所有费用不为 0 的牌。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> scrapeEffect(ctx, self, target, 10)));
    }

    /** 碎裂：3 费 罕见 攻击 —— 24 伤害；如果击杀目标则获得 3 点能量（升级 32 伤害）。 */
    public static final Card SUNDER = register(new Card(
            "sunder", "碎裂", CardType.ATTACK, 3, CardTarget.ENEMY,
            "造成 24 点伤害。如果这张牌击杀了目标，获得 3 点能量。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> sunderEffect(ctx, self, target, 24)));

    static {
        upgrade(new Card("sunder", "碎裂+", CardType.ATTACK, 3, CardTarget.ENEMY,
                "造成 32 点伤害。如果这张牌击杀了目标，获得 3 点能量。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> sunderEffect(ctx, self, target, 32)));
    }

    /** 回弹：1 费 普通 攻击 —— 9 伤害；把这张牌放到抽牌堆顶（升级 16 伤害）。 */
    public static final Card REBOUND = register(new Card(
            "rebound", "回弹", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 9 点伤害。将这张牌放到抽牌堆顶部。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> reboundEffect(ctx, self, target, 9)));

    static {
        upgrade(new Card("rebound", "回弹+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 16 点伤害。将这张牌放到抽牌堆顶部。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> reboundEffect(ctx, self, target, 16)));
    }

    private static void tempestEffect(CombatContext ctx, Combatant self, int extra) {
        int count = ctx.lastX(self) + extra;
        for (int i = 0; i < count; i++) {
            ctx.channelOrb(self, Orb.LIGHTNING);
        }
    }

    private static void defragmentEffect(CombatContext ctx, Combatant self, int amount) {
        self.addFocus(amount);
        ctx.announce("  碎片整理：集中 +" + amount + "（当前 " + self.focus() + "）");
    }

    private static void chillEffect(CombatContext ctx, Combatant self, int perEnemy) {
        int enemies = ctx.aliveEnemies().size();
        for (int i = 0; i < enemies * perEnemy; i++) {
            ctx.channelOrb(self, Orb.FROST);
        }
    }

    private static void scrapeEffect(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「刮削」");
        ctx.drawCards(4);
        PlayerState state = ctx.stateOf(self);
        for (int i = state.hand.size() - 1; i >= 0; i--) {
            if (state.hand.get(i).cost() != 0) {
                // v0.5.3.7：走 discardFromHand（唯一入口），这样【反射】【战术家】这类
                // "被弃置时"的牌在多人混编时也照常触发
                ctx.discardFromHand(self, i);
            }
        }
    }

    private static void sunderEffect(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「碎裂」");
        if (target != null && !target.alive()) {
            ctx.gainEnergy(self, 3);
        }
    }

    private static void reboundEffect(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「回弹」");
        ctx.putOnTopOfDrawPile(self, REBOUND);
    }

    // ================================================================== v0.4.8.6：机宝第七批（集中）
    /** 超能光束：2 费 稀有 攻击 —— 对全体造成 26 伤害；本场战斗失去 3 点集中（升级 34 伤害）。 */
    public static final Card HYPERBEAM = register(new Card(
            "hyperbeam", "超能光束", CardType.ATTACK, 2, CardTarget.SELF,
            "对所有敌人造成 26 点伤害。你失去 3 点集中。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> hyperbeamEffect(ctx, self, 26)));

    static {
        upgrade(new Card("hyperbeam", "超能光束+", CardType.ATTACK, 2, CardTarget.SELF,
                "对所有敌人造成 34 点伤害。你失去 3 点集中。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> hyperbeamEffect(ctx, self, 34)));
    }

    /** 重编程：1 费 罕见 技能 —— 失去 1 点集中，获得 1 点力量与 1 点敏捷（升级 2/2）。 */
    public static final Card REPROGRAM = register(new Card(
            "reprogram", "重编程", CardType.SKILL, 1, CardTarget.SELF,
            "失去 1 点集中。获得 1 点力量。获得 1 点敏捷。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> reprogramEffect(ctx, self, 1)));

    static {
        upgrade(new Card("reprogram", "重编程+", CardType.SKILL, 1, CardTarget.SELF,
                "失去 1 点集中。获得 2 点力量。获得 2 点敏捷。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> reprogramEffect(ctx, self, 2)));
    }

    private static void hyperbeamEffect(CombatContext ctx, Combatant self, int damage) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「超能光束」");
        }
        self.addFocus(-3);
        ctx.announce("  超能光束：失去 3 点集中（当前 " + self.focus() + "）");
    }

    private static void reprogramEffect(CombatContext ctx, Combatant self, int amount) {
        self.addFocus(-1);
        self.addStrength(amount);
        self.addDexterity(amount);
        ctx.announce("  重编程：集中 " + self.focus() + "，力量 +" + amount + "，敏捷 +" + amount);
    }

    // ================================================================== v0.4.8.5：机宝第六批
    /** 堆叠：1 费 普通 技能 —— 获得等同弃牌堆张数的格挡（升级额外 +3）。 */
    public static final Card STACK = register(new Card(
            "stack", "堆叠", CardType.SKILL, 1, CardTarget.SELF,
            "获得等同于你弃牌堆中牌数的格挡。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> stackEffect(ctx, self, 0)));

    static {
        upgrade(new Card("stack", "堆叠+", CardType.SKILL, 1, CardTarget.SELF,
                "获得等同于你弃牌堆中牌数的格挡，再额外获得 3 点格挡。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> stackEffect(ctx, self, 3)));
    }

    /** 回收：1 费 普通 技能 消耗 —— 消耗手牌里费用最高的一张，获得等同其费用的能量。 */
    public static final Card RECYCLE = register(new Card(
            "recycle", "回收", CardType.SKILL, 1, CardTarget.SELF,
            "消耗你手牌中的一张牌（自动挑费用最高的），获得等同于其费用的能量。消耗。",
            CardRarity.COMMON, DEFECT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> recycleEffect(ctx, self)));

    static {
        upgrade(new Card("recycle", "回收+", CardType.SKILL, 0, CardTarget.SELF,
                "消耗你手牌中的一张牌（自动挑费用最高的），获得等同于其费用的能量。消耗。",
                CardRarity.COMMON, DEFECT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> recycleEffect(ctx, self)));
    }

    /** 重启：0 费 罕见 技能 消耗 —— 把弃牌堆全部放回抽牌堆，然后抽 4 张（升级 6 张）。 */
    public static final Card REBOOT = register(new Card(
            "reboot", "重启", CardType.SKILL, 0, CardTarget.SELF,
            "将你弃牌堆中的所有牌洗回抽牌堆。抽 4 张牌。消耗。", CardRarity.RARE, DEFECT,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> rebootEffect(ctx, self, 4)));

    static {
        upgrade(new Card("reboot", "重启+", CardType.SKILL, 0, CardTarget.SELF,
                "将你弃牌堆中的所有牌洗回抽牌堆。抽 6 张牌。消耗。", CardRarity.RARE, DEFECT,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> rebootEffect(ctx, self, 6)));
    }

    private static void stackEffect(CombatContext ctx, Combatant self, int extra) {
        int size = ctx.stateOf(self).discardPile.size();
        ctx.grantPlayerBlock(size + extra, "「堆叠」");
    }

    private static void recycleEffect(CombatContext ctx, Combatant self) {
        // v0.5.1.7 A3：卡面写着"消耗你手牌中的一张牌"，所以真的交给玩家选（不再自动挑费用最高的）
        ctx.beginCardPick(self, "回收", CombatContext.CardPickKind.RECYCLE,
                CombatContext.CardPickSource.HAND, 1, null);
    }

    private static void rebootEffect(CombatContext ctx, Combatant self, int draw) {
        PlayerState state = ctx.stateOf(self);
        java.util.List<Card> shuffled = state.discardPile.drainAll();
        state.drawPile().addAll(shuffled);
        ctx.drawCards(draw);
    }

    // ================================================================== v0.4.8.4：机宝第五批
    /** 涡轮：0 费 普通 技能 —— 获得 2 点能量；把 1 张「虚空」放进弃牌堆（升级 3 点能量）。 */
    public static final Card TURBO = register(new Card(
            "turbo", "涡轮", CardType.SKILL, 0, CardTarget.SELF,
            "获得 2 点能量。将 1 张「虚空」放入你的弃牌堆。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> turboEffect(ctx, self, 2)));

    static {
        upgrade(new Card("turbo", "涡轮+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 3 点能量。将 1 张「虚空」放入你的弃牌堆。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> turboEffect(ctx, self, 3)));
    }

    /** 缓冲：2 费 罕见 能力牌 —— 获得 2 层缓降（免疫接下来 2 次伤害），升级 3 层。 */
    public static final Card BUFFER = register(new Card(
            "buffer", "缓冲", CardType.POWER, 2, CardTarget.SELF,
            "获得 2 层缓降（免疫接下来受到的 2 次伤害）。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> bufferEffect(ctx, self, 2)));

    static {
        upgrade(new Card("buffer", "缓冲+", CardType.POWER, 2, CardTarget.SELF,
                "获得 3 层缓降（免疫接下来受到的 3 次伤害）。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> bufferEffect(ctx, self, 3)));
    }

    /** 暴风雪：1 费 罕见 攻击 —— 对全体造成 2 × 本场生成冰球数 的伤害（升级 3×）。 */
    public static final Card BLIZZARD = register(new Card(
            "blizzard", "暴风雪", CardType.ATTACK, 1, CardTarget.SELF,
            "对所有敌人造成 2 倍于本场战斗中生成冰球数的伤害。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> blizzardEffect(ctx, self, 2)));

    static {
        upgrade(new Card("blizzard", "暴风雪+", CardType.ATTACK, 1, CardTarget.SELF,
                "对所有敌人造成 3 倍于本场战斗中生成冰球数的伤害。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> blizzardEffect(ctx, self, 3)));
    }

    private static void turboEffect(CombatContext ctx, Combatant self, int energy) {
        ctx.gainEnergy(self, energy);
        ctx.insertCards(self.name(), CardLibrary.byId("void"), 1);
    }

    private static void bufferEffect(CombatContext ctx, Combatant self, int layers) {
        self.addSlowFall(layers);
        ctx.announce("  缓冲：获得 " + layers + " 层缓降" + self.statusSuffix());
    }

    private static void blizzardEffect(CombatContext ctx, Combatant self, int perFrost) {
        int damage = perFrost * self.frostCount();
        if (damage <= 0) {
            ctx.announce("  暴风雪：本场还没有生成过冰球，没有伤害");
            return;
        }
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「暴风雪」");
        }
    }

    // ================================================================== v0.4.8.3：机宝第四批
    /** 流星：5 费 稀有 攻击 —— 24 伤害 + 生成 3 颗等离子球（升级 30 伤害）。 */
    public static final Card METEOR_STRIKE = register(new Card(
            "meteor_strike", "流星", CardType.ATTACK, 5, CardTarget.ENEMY,
            "造成 24 点伤害。生成 3 颗等离子球。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> meteorEffect(ctx, self, target, 24)));

    static {
        upgrade(new Card("meteor_strike", "流星+", CardType.ATTACK, 5, CardTarget.ENEMY,
                "造成 30 点伤害。生成 3 颗等离子球。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> meteorEffect(ctx, self, target, 30)));
    }

    private static void meteorEffect(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「流星」");
        for (int i = 0; i < 3; i++) {
            ctx.channelOrb(self, Orb.PLASMA);
        }
    }

    // ================================================================== v0.4.8.2：机宝第三批
    /** 爪击：0 费 普通 攻击 —— 3 伤害，并让本场战斗所有爪击 +2（升级 5 伤害）。 */
    public static final Card CLAW_CARD = register(new Card(
            "claw_defect", "爪击", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 3 点伤害。本场战斗中，所有「爪击」的伤害 +2。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 3 + self.clawBonus(), "「爪击」");
                self.addClawBonus(2);
            }));

    static {
        upgrade(new Card("claw_defect", "爪击+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 5 点伤害。本场战斗中，所有「爪击」的伤害 +2。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 5 + self.clawBonus(), "「爪击+」");
                    self.addClawBonus(2);
                }));
    }

    /** 光束：0 费 普通 攻击 —— 3 伤害 + 1 层易伤（升级 4 伤害）。 */
    public static final Card BEAM_CELL = register(new Card(
            "beam_cell", "光束", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 3 点伤害，给予 1 层易伤。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> beamCellEffect(ctx, self, target, 3)));

    static {
        upgrade(new Card("beam_cell", "光束+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 4 点伤害，给予 1 层易伤。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> beamCellEffect(ctx, self, target, 4)));
    }

    private static void beamCellEffect(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「光束」");
        if (target != null && target.alive()) {
            target.addVulnerable(1);
        }
    }

    /** 超频：0 费 罕见 技能 —— 抽 2 张牌，受到 1 点伤害（升级 抽 3 张）。 */
    public static final Card OVERCLOCK = register(new Card(
            "overclock", "超频", CardType.SKILL, 0, CardTarget.SELF,
            "抽 2 张牌。受到 1 点伤害。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> overclockEffect(ctx, self, 2)));

    static {
        upgrade(new Card("overclock", "超频+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 3 张牌。受到 1 点伤害。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> overclockEffect(ctx, self, 3)));
    }

    private static void overclockEffect(CombatContext ctx, Combatant self, int draw) {
        ctx.drawCards(draw);
        ctx.loseHp(self, 1, "「超频」");
    }

    // ================================================================== v0.4.8.1：机宝第二批
    /** 多重施法：X 费 稀有 技能 —— 激发最右侧充能球 X 次（X = 花掉的能量）。 */
    public static final Card MULTI_CAST = registerXCost(new Card(
            "multi_cast", "多重施法", CardType.SKILL, 0, CardTarget.SELF,
            "激发你最右侧的充能球 X 次。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> multiCastEffect(ctx, self)));

    static {
        upgrade(new Card("multi_cast", "多重施法+", CardType.SKILL, 0, CardTarget.SELF,
                "激发你最右侧的充能球 X 次。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> multiCastEffect(ctx, self)));
    }

    /** 汇总：1 费 罕见 技能 —— 1 点能量 + 抽牌堆每 10 张再 +1（升级 0 费）。 */
    public static final Card AGGREGATE = register(new Card(
            "aggregate", "汇总", CardType.SKILL, 1, CardTarget.SELF,
            "获得 1 点能量。你的抽牌堆每有 10 张牌，额外获得 1 点能量。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> aggregateEffect(ctx, self)));

    static {
        upgrade(new Card("aggregate", "汇总+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 1 点能量。你的抽牌堆每有 10 张牌，额外获得 1 点能量。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> aggregateEffect(ctx, self)));
    }

    /** 全息影像：1 费 普通 技能 消耗 —— 把弃牌堆里费用最高的一张拿回手牌（自动选）。 */
    public static final Card HOLOGRAM = register(new Card(
            "hologram", "全息影像", CardType.SKILL, 1, CardTarget.SELF,
            "从弃牌堆选择一张牌放回手牌。消耗。", CardRarity.COMMON, DEFECT,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> hologramEffect(ctx, self)));

    static {
        upgrade(new Card("hologram", "全息影像+", CardType.SKILL, 1, CardTarget.SELF,
                "从弃牌堆选择一张牌放回手牌。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> hologramEffect(ctx, self)));
    }

    /** 蒸汽护盾：0 费 普通 技能 —— 6 格挡（升级 8）。 */
    public static final Card STEAM_BARRIER = register(new Card(
            "steam_barrier", "蒸汽护盾", CardType.SKILL, 0, CardTarget.SELF,
            "获得 6 点格挡。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> ctx.grantPlayerBlock(6, "「蒸汽护盾」")));

    static {
        upgrade(new Card("steam_barrier", "蒸汽护盾+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 8 点格挡。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> ctx.grantPlayerBlock(8, "「蒸汽护盾+」")));
    }

    /** 锯齿：1 费 普通 攻击 —— 每有一颗充能球就造成 4 点伤害（升级 5）。 */
    public static final Card BARRAGE_CARD = register(new Card(
            "barrage_defect", "锯齿", CardType.ATTACK, 1, CardTarget.ENEMY,
            "每有一颗充能球，就造成 4 点伤害。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> barrageEffect(ctx, self, target, 4)));

    static {
        upgrade(new Card("barrage_defect", "锯齿+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "每有一颗充能球，就造成 5 点伤害。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> barrageEffect(ctx, self, target, 5)));
    }

    private static void multiCastEffect(CombatContext ctx, Combatant self) {
        int x = ctx.lastX(self);
        for (int i = 0; i < x; i++) {
            ctx.evokeOrb(self);
        }
    }

    private static void aggregateEffect(CombatContext ctx, Combatant self) {
        int gained = 1 + ctx.stateOf(self).drawPile().size() / 10;
        ctx.gainEnergy(self, gained);
        ctx.announce("  汇总：获得 " + gained + " 点能量");
    }

    private static void hologramEffect(CombatContext ctx, Combatant self) {
        java.util.List<Card> discard = ctx.stateOf(self).discardPile.view();
        int best = -1;
        int bestCost = -1;
        for (int i = 0; i < discard.size(); i++) {
            if (discard.get(i).cost() >= bestCost) {
                bestCost = discard.get(i).cost();
                best = i;
            }
        }
        if (best < 0) {
            return;
        }
        Card picked = ctx.stateOf(self).discardPile.removeAt(best);
        if (picked != null) {
            ctx.stateOf(self).hand.add(picked);
        }
    }

    private static void barrageEffect(CombatContext ctx, Combatant self, Combatant target, int perOrb) {
        int orbs = self.orbs().size();
        for (int i = 0; i < orbs; i++) {
            ctx.dealDamage(self, target, perOrb, "「锯齿」");
        }
    }

    // ================================================================== v0.4.8.0：机宝（故障机器人）卡牌补全

    /** 电球生成：1 费 普通 攻击 —— 7 伤害 + 生成 1 颗闪电球（升级 10 伤害）。 */
    public static final Card BALL_LIGHTNING = register(new Card(
            "ball_lightning", "电球生成", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 7 点伤害。生成 1 颗闪电球。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 7, "「电球生成」");
                ctx.channelOrb(self, Orb.LIGHTNING);
            }));

    static {
        upgrade(new Card("ball_lightning", "电球生成+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 10 点伤害。生成 1 颗闪电球。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 10, "「电球生成+」");
                    ctx.channelOrb(self, Orb.LIGHTNING);
                }));
    }

    /** 寒冰碎片：1 费 普通 攻击 —— 6 伤害 + 生成 1 颗冰球（升级 9 伤害）。 */
    public static final Card COLD_SNAP = register(new Card(
            "cold_snap", "寒冰碎片", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 6 点伤害。生成 1 颗冰球。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 6, "「寒冰碎片」");
                ctx.channelOrb(self, Orb.FROST);
            }));

    static {
        upgrade(new Card("cold_snap", "寒冰碎片+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 9 点伤害。生成 1 颗冰球。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 9, "「寒冰碎片+」");
                    ctx.channelOrb(self, Orb.FROST);
                }));
    }

    /** 充能电池：1 费 普通 技能 —— 7 格挡 + 1 点能量（升级 10 格挡）。 */
    public static final Card CHARGE_BATTERY = register(new Card(
            "charge_battery", "充能电池", CardType.SKILL, 1, CardTarget.SELF,
            "获得 7 点格挡。获得 1 点能量。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(7, "「充能电池」");
                ctx.gainEnergy(self, 1);
            }));

    static {
        upgrade(new Card("charge_battery", "充能电池+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 10 点格挡。获得 1 点能量。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(10, "「充能电池+」");
                    ctx.gainEnergy(self, 1);
                }));
    }

    /** 冷却：1 费 普通 技能 —— 生成 1 颗冰球 + 抽 1 张牌（升级抽 2 张）。 */
    public static final Card COOLHEADED = register(new Card(
            "coolheaded", "冷却", CardType.SKILL, 1, CardTarget.SELF,
            "生成 1 颗冰球。抽 1 张牌。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.channelOrb(self, Orb.FROST);
                ctx.drawCards(1);
            }));

    static {
        upgrade(new Card("coolheaded", "冷却+", CardType.SKILL, 1, CardTarget.SELF,
                "生成 1 颗冰球。抽 2 张牌。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.channelOrb(self, Orb.FROST);
                    ctx.drawCards(2);
                }));
    }

    /** 双重施法：1 费 普通 技能 —— 激发最右侧充能球 2 次（升级 0 费）。 */
    public static final Card DUALCAST = register(new Card(
            "dualcast", "双重施法", CardType.SKILL, 1, CardTarget.SELF,
            "激发你最右侧的充能球 2 次。", CardRarity.COMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.evokeOrb(self);
                ctx.evokeOrb(self);
            }));

    static {
        upgrade(new Card("dualcast", "双重施法+", CardType.SKILL, 0, CardTarget.SELF,
                "激发你最右侧的充能球 2 次。", CardRarity.COMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.evokeOrb(self);
                    ctx.evokeOrb(self);
                }));
    }

    /** 冰封：2 费 罕见 技能 —— 7 格挡 + 生成 2 颗冰球（升级 10 格挡）。 */
    public static final Card GLACIER = register(new Card(
            "glacier", "冰封", CardType.SKILL, 2, CardTarget.SELF,
            "获得 7 点格挡，生成 2 颗冰球。", CardRarity.UNCOMMON, DEFECT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(7, "「冰封」");
                ctx.channelOrb(self, Orb.FROST);
                ctx.channelOrb(self, Orb.FROST);
            }));

    static {
        upgrade(new Card("glacier", "冰封+", CardType.SKILL, 2, CardTarget.SELF,
                "获得 10 点格挡，生成 2 颗冰球。", CardRarity.UNCOMMON, DEFECT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(10, "「冰封+」");
                    ctx.channelOrb(self, Orb.FROST);
                    ctx.channelOrb(self, Orb.FROST);
                }));
    }

    /** 不屈（v0.4.7.8）：稀有 状态牌 —— 不能被打出；在手里挡一次死，之后从牌组移除。 */
    public static final Card LAST_STAND = register(new Card(
            "last_stand", "不屈", CardType.STATUS, 0, CardTarget.SELF,
            "不能被打出。保留。当这张牌在你的手中，且你即将死亡时：恢复最大生命值的 30%，获得 5 层力量，获得 3 点能量。之后，这张牌会彻底从你的牌组移除。",
            CardRarity.RARE, PILLAGER, Set.of(CardKeyword.RETAIN), (ctx, self, target) -> { }));

    /** 废料回收（v0.4.7.7）：1 费 罕见 技能 —— 9/11 格挡 + 选弃牌堆一张置顶。 */
    public static final Card SCRAP_RECYCLE = register(new Card(
            "scrap_recycle", "废料回收", CardType.SKILL, 1, CardTarget.SELF,
            "获得 9 点格挡。选择你弃牌堆的一张牌，将其放到抽牌堆牌顶。", CardRarity.UNCOMMON, PILLAGER,
            (ctx, self, target) -> scrapRecycleEffect(ctx, self, 9)));

    static {
        upgrade(new Card("scrap_recycle", "废料回收+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 11 点格挡。选择你弃牌堆的一张牌，将其放到抽牌堆牌顶。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> scrapRecycleEffect(ctx, self, 11)));
    }

    /** 应急储备（v0.4.7.7）：1 费 稀有 技能 消耗 —— 手牌一张变化为闪光弹。 */
    public static final Card CONTINGENCY = register(new Card(
            "contingency", "应急储备", CardType.SKILL, 1, CardTarget.SELF,
            "选择你手牌中的 1 张牌，将其变化为「闪光弹」。消耗。", CardRarity.RARE, PILLAGER,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> contingencyEffect(ctx, self, resolve("flashbang", 0))));

    static {
        upgrade(new Card("contingency", "应急储备+", CardType.SKILL, 1, CardTarget.SELF,
                "选择你手牌中的 1 张牌，将其变化为升级的「闪光弹」。消耗。", CardRarity.RARE, PILLAGER,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> contingencyEffect(ctx, self, resolve("flashbang", 1))));
    }

    /** 弃置边陵（v0.4.7.7）：0 费 稀有 技能 固有 + 保留 —— 手牌最多 5/7 张消耗。 */
    public static final Card ABANDON_BORDER = register(new Card(
            "abandon_border", "弃置边陵", CardType.SKILL, 0, CardTarget.SELF,
            "选择你手牌中的最多 5 张牌，将其消耗。固有。保留。", CardRarity.RARE, PILLAGER,
            Set.of(CardKeyword.INNATE, CardKeyword.RETAIN),
            (ctx, self, target) -> abandonBorderEffect(ctx, self, 5)));

    static {
        upgrade(new Card("abandon_border", "弃置边陵+", CardType.SKILL, 0, CardTarget.SELF,
                "选择你手牌中的最多 7 张牌，将其消耗。固有。保留。", CardRarity.RARE, PILLAGER,
                Set.of(CardKeyword.INNATE, CardKeyword.RETAIN),
                (ctx, self, target) -> abandonBorderEffect(ctx, self, 7)));
    }

    private static void scrapRecycleEffect(CombatContext ctx, Combatant self, int block) {
        ctx.grantPlayerBlock(block, "「废料回收」");
        // v0.5.1.7 A3：卡面写着"选择你弃牌堆的一张牌"，所以真的交给玩家选
        ctx.beginCardPick(self, "废料回收", CombatContext.CardPickKind.SCRAP_RECYCLE,
                CombatContext.CardPickSource.DISCARD, 1, null);
    }

    private static void contingencyEffect(CombatContext ctx, Combatant self, Card flashbang) {
        // v0.5.1.7 A3：卡面写着"选择你手牌中的 1 张牌"，所以真的交给玩家选
        ctx.beginCardPick(self, "应急储备", CombatContext.CardPickKind.CONTINGENCY,
                CombatContext.CardPickSource.HAND, 1, flashbang);
    }

    private static void abandonBorderEffect(CombatContext ctx, Combatant self, int count) {
        // v0.5.1.7 A3：卡面写着"选择你手牌中的最多 N 张牌"，所以真的交给玩家选
        ctx.beginCardPick(self, "弃置边陵", CombatContext.CardPickKind.ABANDON,
                CombatContext.CardPickSource.HAND, count, null);
    }

    /** 紧急避险（v0.4.7.5）：0 费 罕见 技能 消耗 —— 20 格挡，但下一回合直接跳过；升级 28 格挡。 */
    public static final Card EMERGENCY_DODGE = register(new Card(
            "emergency_dodge", "紧急避险", CardType.SKILL, 0, CardTarget.SELF,
            "获得 20 点格挡。你的下一回合进入晕眩（直接跳过）。消耗。", CardRarity.UNCOMMON, PILLAGER,
            Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> emergencyDodgeEffect(ctx, self, 20)));

    static {
        upgrade(new Card("emergency_dodge", "紧急避险+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 28 点格挡。你的下一回合进入晕眩（直接跳过）。消耗。", CardRarity.UNCOMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> emergencyDodgeEffect(ctx, self, 28)));
    }

    /** 紧急避险的效果体：格挡 + 挂上"跳过下一回合"。 */
    private static void emergencyDodgeEffect(CombatContext ctx, Combatant self, int block) {
        ctx.grantPlayerBlock(block, "「紧急避险」");
        ctx.skipNextTurn(self);
    }

    /** 多重射击（v0.4.7.4）：1 费 罕见 攻击 —— 全体 8 点，打完把它自己放回抽牌堆顶；升级 11 点。 */
    public static final Card BARRAGE = register(new Card(
            "barrage", "多重射击", CardType.ATTACK, 1, CardTarget.SELF,
            "对所有敌人造成 8 点伤害。之后，将这张牌放到抽牌堆顶部。", CardRarity.UNCOMMON, PILLAGER,
            (ctx, self, target) -> barrageEffect(ctx, self, 8)));

    static {
        upgrade(new Card("barrage", "多重射击+", CardType.ATTACK, 1, CardTarget.SELF,
                "对所有敌人造成 11 点伤害。之后，将这张牌放到抽牌堆顶部。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> barrageEffect(ctx, self, 11)));
    }

    /** 多重射击的效果体（抽出来写成方法，避开静态初始化器里的复杂 lambda）。 */
    private static void barrageEffect(CombatContext ctx, Combatant self, int damage) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「多重射击」");
        }
        ctx.putOnTopOfDrawPile(self, BARRAGE);
    }

    /**
     * v0.4.7.0 需求原文里的 12 张叛逃掠夺者新卡（这一批先把不依赖新 UI 的做完）。
     *
     * <p>剩余 9 张（躲避灾厄 / 废料回收 / 应急储备 / 闪光弹 / 弃置边陵 / 紧急避险 / 不屈 /
     * 多重射击 / 弓弦强化）需要 X 费、选牌页、变化牌、击晕、跳过回合、濒死触发、置顶、
     * 按卡名加伤这些新原语，见 README §61 的清单。</p>
     */
    static {
        // 弩箭射击的强化版（还是同一个 id，所以它照样是生成牌）
        upgrade(new Card("bolt_shot", "弩箭射击+", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 7 点伤害。",
                CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> ctx.dealDamage(self, target, 7 + self.boltBonus(), "「弩箭射击+」")));

        // 闪光弹：0 费 无色牌 技能 —— 选择一个敌人，击晕（当前回合意图变「晕眩」）；保留 + 消耗。
        // v0.5.5.2（用户 B5 权威文本）："闪光弹 0费 无色牌 技能牌／选择一个敌人，击晕（使其当前回合的
        // 意图变为：晕眩）。／保留。消耗。" —— 所以**没有**易伤（v0.5.1.x 那 2 层易伤是我加的，已移除），
        // 并且补上**消耗**（原文有；以前只有保留 —— 保留 + 0 费 + 无消耗 = 每回合白嫖击晕，会锁死敌人）。
        register(new Card("flashbang", "闪光弹", CardType.SKILL, 0, CardTarget.ENEMY,
                "选择一个敌人，击晕（使其当前回合的意图变为：晕眩）。保留。消耗。", CardRarity.COMMON, null,
                Set.of(CardKeyword.RETAIN, CardKeyword.EXHAUST), (ctx, self, target) -> {
                    if (target instanceof Enemy enemy) {
                        ctx.stunEnemy(enemy);
                    }
                }));
        // 闪光弹+：用户只给了基础版文本；这里保留我原先补的"击晕 + 2 层易伤"作为**升级收益**
        // （⚠️ 仍是我补的数值，等用户给权威值；只要基础版按原文是纯击晕 + 消耗，就不会锁死敌人）。
        upgrade(new Card("flashbang", "闪光弹+", CardType.SKILL, 0, CardTarget.ENEMY,
                "选择一个敌人，击晕（使其当前回合的意图变为：晕眩），并给予 2 层易伤。保留。消耗。",
                CardRarity.COMMON, null, Set.of(CardKeyword.RETAIN, CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    if (target instanceof Enemy enemy) {
                        ctx.stunEnemy(enemy);
                        enemy.addVulnerable(2);
                    }
                }));

        // 躲避灾厄：X 费 罕见 技能 —— 对自己造成 X 次 1 点伤害，获得 X 点迅捷（升级 X+1）
        registerXCost(new Card("dodge_calamity", "躲避灾厄", CardType.SKILL, 0, CardTarget.SELF,
                "对自己造成 X 次 1 点伤害。获得 X 点迅捷。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    int x = ctx.lastX(self);
                    for (int i = 0; i < x; i++) {
                        ctx.takeFixedDamage(self, 1, "「躲避灾厄」");
                    }
                    self.addDexterity(x);
                    ctx.announce("  X = " + x + "：对自己造成 " + x + " 次 1 点伤害，获得迅捷 " + x);
                }));
        upgrade(new Card("dodge_calamity", "躲避灾厄+", CardType.SKILL, 0, CardTarget.SELF,
                "对自己造成 X 次 1 点伤害。获得 X+1 点迅捷。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    int x = ctx.lastX(self);
                    for (int i = 0; i < x; i++) {
                        ctx.takeFixedDamage(self, 1, "「躲避灾厄+」");
                    }
                    self.addDexterity(x + 1);
                    ctx.announce("  X = " + x + "：对自己造成 " + x + " 次 1 点伤害，获得迅捷 " + (x + 1));
                }));

        // 弓弦强化：2 费 罕见 能力牌 —— 你的「弩箭射击」伤害 +1（升级 +2）
        register(new Card("bowstring_upgrade", "弓弦强化", CardType.POWER, 2, CardTarget.SELF,
                "你的「弩箭射击」伤害 +1。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    self.addBoltBonus(1);
                    ctx.announce("  弓弦强化：弩箭射击伤害 +1（当前 +" + self.boltBonus() + "）");
                }));
        upgrade(new Card("bowstring_upgrade", "弓弦强化+", CardType.POWER, 2, CardTarget.SELF,
                "你的「弩箭射击」伤害 +2。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    self.addBoltBonus(2);
                    ctx.announce("  弓弦强化+：弩箭射击伤害 +2（当前 +" + self.boltBonus() + "）");
                }));

        // 快跃！这货要逃：2 费 普通 攻击 —— 12 伤害；目标意图为格挡时额外 6 点；升级 14
        register(new Card("quick_leap", "快跃！这货要逃", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 12 点伤害。如果这名敌人的意图为格挡，额外造成 6 点伤害。", CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 12, "「快跃！这货要逃」");
                    if (target instanceof Enemy enemy && enemy.hasAction(IntentType.DEFEND)) {
                        ctx.dealDamage(self, target, 6, "「快跃！这货要逃」追加（敌人正在格挡）");
                    }
                }));
        upgrade(new Card("quick_leap", "快跃！这货要逃+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 14 点伤害。如果这名敌人的意图为格挡，额外造成 6 点伤害。", CardRarity.COMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 14, "「快跃！这货要逃+」");
                    if (target instanceof Enemy enemy && enemy.hasAction(IntentType.DEFEND)) {
                        ctx.dealDamage(self, target, 6, "「快跃！这货要逃+」追加（敌人正在格挡）");
                    }
                }));

        // 休养生息：1 费 普通 技能 消耗 —— 7 格挡 + 回 1 生命；升级回 3
        register(new Card("recuperate", "休养生息", CardType.SKILL, 1, CardTarget.SELF,
                "获得 7 点格挡，恢复 1 点生命。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    ctx.grantPlayerBlock(7, "「休养生息」");
                    int before = self.hp();
                    self.heal(1);
                    ctx.announce("  恢复 " + (self.hp() - before) + " 点生命（HP " + self.hp() + "/"
                            + self.maxHp() + "）");
                }));
        upgrade(new Card("recuperate", "休养生息+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 7 点格挡，恢复 3 点生命。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    ctx.grantPlayerBlock(7, "「休养生息+」");
                    int before = self.hp();
                    self.heal(3);
                    ctx.announce("  恢复 " + (self.hp() - before) + " 点生命（HP " + self.hp() + "/"
                            + self.maxHp() + "）");
                }));

        // 装填弩箭：2 费 普通 技能 消耗 —— 抽牌堆加 3 张弩箭射击；升级 4 张
        register(new Card("reload_bolts", "装填弩箭", CardType.SKILL, 2, CardTarget.SELF,
                "在你的抽牌堆加入 3 张「弩箭射击」。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.addToDrawPile(self, BOLT_SHOT, 3)));
        upgrade(new Card("reload_bolts", "装填弩箭+", CardType.SKILL, 2, CardTarget.SELF,
                "在你的抽牌堆加入 4 张「弩箭射击」。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.addToDrawPile(self, BOLT_SHOT, 4)));

        // 锁定目标：1 费 普通 技能 消耗 —— 2 层易伤；升级 3
        register(new Card("mark_target", "锁定目标", CardType.SKILL, 1, CardTarget.ENEMY,
                "给予 2 层易伤。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    if (target != null) {
                        target.addVulnerable(2);
                        ctx.announce("  " + target.name() + " 陷入易伤 2" + target.statusSuffix());
                    }
                }));
        upgrade(new Card("mark_target", "锁定目标+", CardType.SKILL, 1, CardTarget.ENEMY,
                "给予 3 层易伤。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    if (target != null) {
                        target.addVulnerable(3);
                        ctx.announce("  " + target.name() + " 陷入易伤 3" + target.statusSuffix());
                    }
                }));

        // 诱饵：1 费 普通 技能 消耗 —— 3 层虚弱；升级 4
        register(new Card("decoy", "诱饵", CardType.SKILL, 1, CardTarget.ENEMY,
                "给予 3 层虚弱。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    if (target != null) {
                        target.addWeak(3);
                        ctx.announce("  " + target.name() + " 陷入虚弱 3" + target.statusSuffix());
                    }
                }));
        upgrade(new Card("decoy", "诱饵+", CardType.SKILL, 1, CardTarget.ENEMY,
                "给予 4 层虚弱。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    if (target != null) {
                        target.addWeak(4);
                        ctx.announce("  " + target.name() + " 陷入虚弱 4" + target.statusSuffix());
                    }
                }));

        // 收集土豆：0 费 普通 技能 消耗 —— 2 层再生；升级 3
        register(new Card("gather_potatoes", "收集土豆", CardType.SKILL, 0, CardTarget.SELF,
                "获得 2 层再生。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    self.addRegeneration(2);
                    ctx.announce("  获得再生 2" + self.statusSuffix());
                }));
        upgrade(new Card("gather_potatoes", "收集土豆+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 3 层再生。消耗。", CardRarity.COMMON, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    self.addRegeneration(3);
                    ctx.announce("  获得再生 3" + self.statusSuffix());
                }));

        // 铁质箭矢：1 费 罕见 攻击 —— 9 伤害 + 2 易伤；升级 11 + 3
        register(new Card("iron_arrow", "铁质箭矢", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 9 点伤害，给予 2 层易伤。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 9, "「铁质箭矢」");
                    if (target != null && target.alive()) {
                        target.addVulnerable(2);
                    }
                }));
        upgrade(new Card("iron_arrow", "铁质箭矢+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 11 点伤害，给予 3 层易伤。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 11, "「铁质箭矢+」");
                    if (target != null && target.alive()) {
                        target.addVulnerable(3);
                    }
                }));

        // 箭袋补充：2 费 罕见 技能 —— 手牌 / 抽牌堆 / 弃牌堆各 1 张弩箭射击；升级版给强化弩箭
        register(new Card("quiver_refill", "箭袋补充", CardType.SKILL, 2, CardTarget.SELF,
                "在你的手牌、抽牌堆、弃牌堆各添加 1 张「弩箭射击」。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    ctx.addCardsToHand(self, BOLT_SHOT, 1);
                    ctx.addToDrawPile(self, BOLT_SHOT, 1);
                    ctx.insertCards(self.name(), BOLT_SHOT, 1);
                }));
        upgrade(new Card("quiver_refill", "箭袋补充+", CardType.SKILL, 2, CardTarget.SELF,
                "在你的手牌、抽牌堆、弃牌堆各添加 1 张强化「弩箭射击」。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> {
                    Card strong = resolve("bolt_shot", 1);
                    ctx.addCardsToHand(self, strong, 1);
                    ctx.addToDrawPile(self, strong, 1);
                    ctx.insertCards(self.name(), strong, 1);
                }));

        // 连弩射击：2 费 罕见 攻击 —— 对随机敌人 2 点 × 6（每次独立随机）；升级 3 点
        register(new Card("repeating_crossbow", "连弩射击", CardType.ATTACK, 2, CardTarget.SELF,
                "对随机敌人造成 2 点伤害 6 次。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> ctx.dealRandomDamage(self, 6, 2, "「连弩射击」")));
        upgrade(new Card("repeating_crossbow", "连弩射击+", CardType.ATTACK, 2, CardTarget.SELF,
                "对随机敌人造成 3 点伤害 6 次。", CardRarity.UNCOMMON, PILLAGER,
                (ctx, self, target) -> ctx.dealRandomDamage(self, 6, 3, "「连弩射击+」")));

        // 突出重围：0 费 罕见 技能 固有 + 保留 + 消耗 —— 4 格挡，本回合每打出过一张牌再 +3；升级去掉消耗
        register(new Card("break_out", "突出重围", CardType.SKILL, 0, CardTarget.SELF,
                "获得 4 点格挡。你在本回合每打出过一张牌，额外获得 3 点格挡。固有。保留。消耗。",
                CardRarity.UNCOMMON, PILLAGER,
                Set.of(CardKeyword.INNATE, CardKeyword.RETAIN, CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    int played = Math.max(0, ctx.cardsPlayedThisTurn() - 1);
                    ctx.grantPlayerBlock(4 + 3 * played, "「突出重围」");
                }));
        upgrade(new Card("break_out", "突出重围+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 4 点格挡。你在本回合每打出过一张牌，额外获得 3 点格挡。固有。保留。",
                CardRarity.UNCOMMON, PILLAGER, Set.of(CardKeyword.INNATE, CardKeyword.RETAIN),
                (ctx, self, target) -> {
                    int played = Math.max(0, ctx.cardsPlayedThisTurn() - 1);
                    ctx.grantPlayerBlock(4 + 3 * played, "「突出重围+」");
                }));

        // 流星飞索：3 费 稀有 攻击 消耗 —— 全体缓慢 7 + 全体 18 伤害；升级缓慢 9
        register(new Card("meteor_chain", "流星飞索", CardType.ATTACK, 3, CardTarget.SELF,
                "给予所有敌人缓慢 7。对所有敌人造成 18 点伤害。消耗。", CardRarity.RARE, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        enemy.addSlow(7);
                    }
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 18, "「流星飞索」");
                    }
                }));
        upgrade(new Card("meteor_chain", "流星飞索+", CardType.ATTACK, 3, CardTarget.SELF,
                "给予所有敌人缓慢 9。对所有敌人造成 18 点伤害。消耗。", CardRarity.RARE, PILLAGER,
                Set.of(CardKeyword.EXHAUST), (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        enemy.addSlow(9);
                    }
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 18, "「流星飞索+」");
                    }
                }));
    }

    // ================== v0.5.1.4 叛逃掠夺者：五张【多人卡牌】 ==================
    //
    // 需求："新卡牌类型：多人卡牌 —— 这些卡牌只有在多人模式下才会从奖励池里出现。
    // 为卡牌图鉴添加：多人模式卡牌图鉴。" 这五张都属于叛逃掠夺者，单人时**不进奖励池**，
    // 由图鉴主页的「多人卡牌图鉴」单独一页展示。

    /** 快哉，肉排！：0 费 罕见 技能 消耗 —— 获得 2 层力量；升级 3 层。 */
    public static final Card HEARTY_MEAL = register(new Card(
            "hearty_meal", "快哉，肉排！", CardType.SKILL, 0, CardTarget.SELF,
            "获得 2 层力量。消耗。", CardRarity.UNCOMMON, PILLAGER, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                self.addStrength(2);
                ctx.announce("  获得力量 +2（当前 " + signed(self.strength()) + "）");
            }));

    static {
        upgrade(new Card("hearty_meal", "快哉，肉排！+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 3 层力量。消耗。", CardRarity.UNCOMMON, PILLAGER, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    self.addStrength(3);
                    ctx.announce("  获得力量 +3（当前 " + signed(self.strength()) + "）");
                }));
    }

    /** 我们团结一心！：1 费 罕见 技能 —— 对所有敌人造成 8 点伤害，每存在一个敌人再 +2（升级 +3）。 */
    public static final Card UNITED_WE_STAND = register(new Card(
            "united_we_stand", "我们团结一心！", CardType.SKILL, 1, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 8 点伤害。每存在一个敌人，这张卡牌的伤害 +2。",
            CardRarity.UNCOMMON, PILLAGER, (ctx, self, target) -> unitedWeStand(ctx, self, 2)));

    static {
        upgrade(new Card("united_we_stand", "我们团结一心！+", CardType.SKILL, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 8 点伤害。每存在一个敌人，这张卡牌的伤害 +3。",
                CardRarity.UNCOMMON, PILLAGER, (ctx, self, target) -> unitedWeStand(ctx, self, 3)));
    }

    /**
     * 我们团结一心！的效果体。
     *
     * <p>伤害按**施放瞬间**的敌人数算一次，再逐个结算 —— 否则"A 被打死之后 B 就少吃了加成"。</p>
     */
    private static void unitedWeStand(CombatContext ctx, Combatant self, int perEnemy) {
        int count = ctx.aliveEnemies().size();
        int damage = 8 + perEnemy * count;
        ctx.announce("  场上 " + count + " 个敌人 → 每个受到 " + damage + " 点伤害");
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「我们团结一心！」");
        }
    }

    /** 沉船打捞：1 费 稀有 技能 消耗 —— 三个牌堆各塞一张"其他角色的随机稀有非能力牌"（升级：强化版）。 */
    public static final Card SHIPWRECK_SALVAGE = register(new Card(
            "shipwreck_salvage", "沉船打捞", CardType.SKILL, 1, CardTarget.SELF,
            "在你的抽牌堆、弃牌堆、手牌中各添加一张来自其他角色的随机稀有非能力牌。消耗。",
            CardRarity.RARE, PILLAGER, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> shipwreckSalvage(ctx, self, false)));

    static {
        upgrade(new Card("shipwreck_salvage", "沉船打捞+", CardType.SKILL, 1, CardTarget.SELF,
                "在你的抽牌堆、弃牌堆、手牌中各添加一张来自其他角色的随机稀有非能力牌（它们是强化版）。消耗。",
                CardRarity.RARE, PILLAGER, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> shipwreckSalvage(ctx, self, true)));
    }

    /**
     * 沉船打捞的效果体。
     *
     * <p>"其他角色"= 不属于叛逃掠夺者的**角色牌**（无色牌不算角色）、稀有、**非**能力牌。
     * 三张尽量不重复（池子不够时才允许重复），依次进 抽牌堆 / 弃牌堆 / 手牌 —— 顺序固定，方便自检。</p>
     */
    private static void shipwreckSalvage(CombatContext ctx, Combatant self, boolean upgraded) {
        List<Card> copy = new ArrayList<>();
        for (Card card : all()) {
            if (card.rarity() != CardRarity.RARE || card.type() == CardType.POWER
                    || card.character() == null || PILLAGER.equals(card.character())
                    || !obtainable(card)) {
                continue;
            }
            copy.add(card);
        }
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        if (copy.isEmpty()) {
            ctx.announce("  沉船里什么都没有……（没有可打捞的其他角色稀有非能力牌）");
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int slot = 0; slot < 3 && !copy.isEmpty(); slot++) {
            Card picked = copy.remove(ctx.nextRandomInt(copy.size()));
            Card definition = upgraded ? resolve(picked.id(), 1) : picked;
            if (definition == null) {
                definition = picked;
            }
            switch (slot) {
                case 0 -> state.drawPile.add(definition);
                case 1 -> state.discardPile.add(definition);
                default -> state.hand.add(definition);
            }
            if (names.length() > 0) {
                names.append('、');
            }
            names.append(definition.name());
        }
        ctx.announce("  打捞上来的牌：" + names + "（抽牌堆 / 弃牌堆 / 手牌 各一张）");
    }

    /** 村庄英雄的说明（基础版与强化版只有费用不同）。 */
    private static final String VILLAGE_HERO_TEXT = "在本次战斗里：你每使用 3 次攻击牌，就对随机敌人施加 15 层凋零；"
            + "每使用 3 次技能牌，就对自己（和队友）施加 1 层附甲；回合结束时，你的附甲层数不会再降低。";

    /** 村庄英雄：3 费（升级 2）稀有 能力 —— 每 3 张攻击牌给随机敌人 15 层凋零、每 3 张技能牌给自己与队友 1 层附甲、附甲不再衰减。 */
    public static final Card VILLAGE_HERO = register(new Card(
            "village_hero", "村庄英雄", CardType.POWER, 3, CardTarget.SELF,
            VILLAGE_HERO_TEXT, CardRarity.RARE, PILLAGER,
            (ctx, self, target) -> villageHeroEffect(ctx, self)));

    static {
        upgrade(new Card("village_hero", "村庄英雄+", CardType.POWER, 2, CardTarget.SELF,
                VILLAGE_HERO_TEXT, CardRarity.RARE, PILLAGER,
                (ctx, self, target) -> villageHeroEffect(ctx, self)));
    }

    private static void villageHeroEffect(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.villageHero = true;
        self.setPlatedArmorLocked(true);
        ctx.announce("  【村庄英雄】生效：每 3 张攻击牌 → 随机敌人凋零 15；"
                + "每 3 张技能牌 → 自己与队友附甲 +1；你的附甲不再随回合衰减");
    }

    /** 魔术技巧！的说明（基础版 60 伤害 / 强化版 76）。 */
    private static final String MAGIC_TRICK_TEXT_60 =
            "当这张牌在你的手牌中时，你每次打出技能牌都会使这张牌的费用-1。对所有敌人造成 60 点伤害。固有。保留。消耗。";
    private static final String MAGIC_TRICK_TEXT_76 =
            "当这张牌在你的手牌中时，你每次打出技能牌都会使这张牌的费用-1。对所有敌人造成 76 点伤害。固有。保留。消耗。";
    /** 魔术技巧！的关键字（固有 + 保留 + 消耗）。 */
    private static final Set<CardKeyword> MAGIC_TRICK_KEYWORDS =
            Set.of(CardKeyword.INNATE, CardKeyword.RETAIN, CardKeyword.EXHAUST);

    /** 魔术技巧！：10 费 稀有 攻击 固有+保留+消耗 —— 在手牌中时每打出技能牌 -1 费；60 伤害（升级 76）。 */
    public static final Card MAGIC_TRICK = register(new Card(
            "magic_trick", "魔术技巧！", CardType.ATTACK, MAGIC_TRICK_COST, CardTarget.ALL_ENEMIES,
            MAGIC_TRICK_TEXT_60, CardRarity.RARE, PILLAGER, MAGIC_TRICK_KEYWORDS,
            (ctx, self, target) -> magicTrickEffect(ctx, self, 60)));

    static {
        upgrade(new Card("magic_trick", "魔术技巧！+", CardType.ATTACK, MAGIC_TRICK_COST,
                CardTarget.ALL_ENEMIES, MAGIC_TRICK_TEXT_76, CardRarity.RARE, PILLAGER,
                MAGIC_TRICK_KEYWORDS, (ctx, self, target) -> magicTrickEffect(ctx, self, 76)));
        // 费用档：减 1..10 费各一份**独立定义**，登记成**生成牌**（不进任何卡池、不进图鉴）。
        // 手牌里的那一张由 CombatContext.advanceMagicTricks 换成便宜一档 —— 于是"每张单独算"
        // 是结构上成立的，不需要在别处维护一份与手牌平行的折扣表（那种写法漏一个抽/弃牌点就错位）。
        for (int discount = 1; discount <= MAGIC_TRICK_MAX_DISCOUNT; discount++) {
            int cost = Math.max(0, MAGIC_TRICK_COST - discount);
            String id = magicTrickId(discount);
            registerGenerated(new Card(id, "魔术技巧！", CardType.ATTACK, cost, CardTarget.ALL_ENEMIES,
                    MAGIC_TRICK_TEXT_60, CardRarity.RARE, PILLAGER, MAGIC_TRICK_KEYWORDS,
                    (ctx, self, target) -> magicTrickEffect(ctx, self, 60)));
            upgrade(new Card(id, "魔术技巧！+", CardType.ATTACK, cost, CardTarget.ALL_ENEMIES,
                    MAGIC_TRICK_TEXT_76, CardRarity.RARE, PILLAGER, MAGIC_TRICK_KEYWORDS,
                    (ctx, self, target) -> magicTrickEffect(ctx, self, 76)));
        }
    }

    private static void magicTrickEffect(CombatContext ctx, Combatant self, int damage) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「魔术技巧！」");
        }
    }

    // ================== v0.5.0.4 机宝第八批（来自二代 StS2 的稀有卡）

    /**
     * 适应打击的 **0 费复制品**（v0.5.0.4）。
     *
     * <p>需求原文："将这张牌的一张0费复制品添加到你的弃牌堆。"</p>
     *
     * <p>用 {@link #registerGenerated} 而不是 {@link #upgrade} 登记：它**不是**"适应打击的强化版"，
     * 而是同 id 的 0 费变体。用 upgrade 会把"适应打击"整张牌的强化版覆盖掉
     * （这正是踩过的"卡牌 id 撞车会覆盖别人的强化版"那个坑）。</p>
     */
    private static final Card ADAPTIVE_STRIKE_FREE = registerGenerated(new Card(
            "adaptive_strike_free", "适应打击（0费）", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 18 点伤害。（0 费复制品，不再生成新的复制品）", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> ctx.dealDamage(self, target, 18, "「适应打击」（0 费复制品）")));

    static {
        // 0 费复制品本身也登记一份强化版：卡池规则要求"每张非状态牌都能强化"。
        // 需求只给了"复制品是 0 费"，没给复制品的强化数值，所以强化版保持 18 点伤害。
        upgrade(new Card("adaptive_strike_free", "适应打击（0费）+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 18 点伤害。（0 费复制品，不再生成新的复制品）", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> ctx.dealDamage(self, target, 18, "「适应打击」（0 费复制品）")));
    }

    /** 适应打击：2 费 稀有 攻击 —— 18 伤害，并往弃牌堆塞一张自己的 0 费复制（升级 23 伤害）。 */
    public static final Card ADAPTIVE_STRIKE = register(new Card(
            "adaptive_strike", "适应打击", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 18 点伤害。将这张牌的一张 0 费复制品添加到你的弃牌堆。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> adaptiveStrikeEffect(ctx, self, target, 18, ADAPTIVE_STRIKE_FREE)));

    static {
        upgrade(new Card("adaptive_strike", "适应打击+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 23 点伤害。将这张牌的一张 0 费复制品添加到你的弃牌堆。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> adaptiveStrikeEffect(ctx, self, target, 23, ADAPTIVE_STRIKE_FREE)));
    }

    /** 适应打击的效果体：造成伤害 + 把一张 0 费复制品加进弃牌堆。 */
    private static void adaptiveStrikeEffect(CombatContext ctx, Combatant self, Combatant target,
                                             int damage, Card copy) {
        ctx.dealDamage(self, target, damage, "「适应打击」");
        ctx.stateOf(self).discardPile.add(copy);
        ctx.announce("  将一张 " + copy.cost() + " 费复制品加入弃牌堆");
    }

    /** 电流相生：3 费 稀有 技能 —— 生成"本场已生成过的闪电球数量"颗闪电球。消耗（升级失去消耗）。 */
    public static final Card VOLTAIC = register(new Card(
            "voltaic", "电流相生", CardType.SKILL, 3, CardTarget.SELF,
            "生成等同于你在本次战斗中已经生成过的闪电球数量的闪电球。消耗。", CardRarity.RARE, DEFECT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> voltaicEffect(ctx, self)));

    static {
        upgrade(new Card("voltaic", "电流相生+", CardType.SKILL, 3, CardTarget.SELF,
                "生成等同于你在本次战斗中已经生成过的闪电球数量的闪电球。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> voltaicEffect(ctx, self)));
    }

    /**
     * 电流相生的效果体（v0.5.0.4）。
     *
     * <p>需求："生成等同于你在本次战斗中已经生成过的闪电球数量的闪电球。（生成 X 次，X 为在本次战斗中已经生成过的闪电球数量）"
     * 且"制作要求：能够显示 X 次的具体数值"。</p>
     *
     * <p>X 来自 {@link Combatant#lightningCount()}——它在 {@code channelOrb} 里**真正生成成功之后**记账，
     * 所以没有球位时不会虚增。注意 X 是在**本牌自己开始生成之前**读的，
     * 否则会边生成边把 X 越推越大。</p>
     */
    private static void voltaicEffect(CombatContext ctx, Combatant self) {
        int x = self.lightningCount();
        ctx.announce("  电流相生：本场已生成 " + x + " 颗闪电球 → 生成 " + x + " 颗");
        for (int i = 0; i < x; i++) {
            ctx.channelOrb(self, Orb.LIGHTNING);
        }
    }

    /**
     * 模组改造：0 费 稀有 技能 —— 获得 1 个充能球栏位、抽 1 张牌，本牌费用 +1（升级抽 2 张）。
     *
     * <p>需求原文："获得1个充能球栏位。 抽1张牌。这张牌的耗能加1。升级：抽1张牌--&gt;抽2张牌。"</p>
     *
     * <p>费用记在 {@link #MODDED_BOOST} 上（{@link CombatContext} 的每场战斗计数器，换战斗归零）。
     * 用法见 {@link #moddedBoostEffect} 的说明。</p>
     */
    public static final Card MODDED = register(new Card(
            "modded", "模组改造", CardType.SKILL, 0, CardTarget.SELF,
            "获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> moddedBoostEffect(ctx, self, 1)));

    static {
        upgrade(new Card("modded", "模组改造+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 1 个充能球栏位。抽 2 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> moddedBoostEffect(ctx, self, 2)));
    }

    /** 模组改造的第 1 档（0 费打出后变 1 费，抽 1 张的那条线）。 */
    private static final Card MODDED_BOOST = registerGenerated(new Card(
            "modded_boost", "模组改造+", CardType.SKILL, 1, CardTarget.SELF,
            "获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> moddedBoostEffect(ctx, self, 1)));

    /** 模组改造的第 1 档（强化线：0 费打出后变 1 费，抽 2 张）。 */
    private static final Card MODDED_BOOST_PLUS = registerGenerated(new Card(
            "modded_boost_plus", "模组改造+", CardType.SKILL, 1, CardTarget.SELF,
            "获得 1 个充能球栏位。抽 2 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> moddedBoostEffect(ctx, self, 2)));

    /** 模组改造的第 2 档（再打出一次就到这里，费用封顶在 2）。 */
    private static final Card MODDED_BOOST_2 = registerGenerated(new Card(
            "modded_boost_2", "模组改造+", CardType.SKILL, 2, CardTarget.SELF,
            "获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> moddedBoostEffect(ctx, self, 1)));

    /** 模组改造的第 2 档（强化线）。 */
    private static final Card MODDED_BOOST_2_PLUS = registerGenerated(new Card(
            "modded_boost_2_plus", "模组改造+", CardType.SKILL, 2, CardTarget.SELF,
            "获得 1 个充能球栏位。抽 2 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
            (ctx, self, target) -> moddedBoostEffect(ctx, self, 2)));

    static {
        // 四个费用档位各登记一份强化版：卡池规则要求"每张非状态牌都能强化"
        upgrade(new Card("modded_boost", "模组改造+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> moddedBoostEffect(ctx, self, 1)));
        upgrade(new Card("modded_boost_plus", "模组改造+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 1 个充能球栏位。抽 2 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> moddedBoostEffect(ctx, self, 2)));
        upgrade(new Card("modded_boost_2", "模组改造+", CardType.SKILL, 2, CardTarget.SELF,
                "获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> moddedBoostEffect(ctx, self, 1)));
        upgrade(new Card("modded_boost_2_plus", "模组改造+", CardType.SKILL, 2, CardTarget.SELF,
                "获得 1 个充能球栏位。抽 2 张牌。这张牌的费用加 1。", CardRarity.RARE, DEFECT,
                (ctx, self, target) -> moddedBoostEffect(ctx, self, 2)));
    }

    /**
     * 模组改造的效果体（v0.5.0.4）。
     *
     * <p>"这张牌的费用加 1" 的实现说明：{@link Card} 是**不可变的共享定义**
     * （一副牌组就是一个 {@code List<Card>}，没有"卡牌实例 id"），所以引擎里
     * **没有单卡实例改费用的机制**。这里的做法是"**费用档位**"：
     * 同一张牌有几个同名的定义（0 / 1 / 2 费），每打出一次就换成费用 +1 的那一档。</p>
     *
     * <p>玩法上因此表现为：第一次打出是 0 费，之后弃牌堆里那张变成 1 费，
     * 再打出变成 2 费（封顶），符合需求"这张牌的耗能加 1"的可见效果。</p>
     *
     * <p><b>已知偏差</b>：档位绑在"弃牌堆里那一张"上，洗回抽牌堆、被复制或被"变化"时
     * 不保证跟着走；同理，若一副牌组里有多张模组改造，它们各自独立计档
     * （不共享"打了几次"）。要完全等同于原版需要给卡牌引入实例 id，属于引擎级改动。</p>
     */
    private static void moddedBoostEffect(CombatContext ctx, Combatant self, int draw) {
        self.setOrbSlots(self.orbSlots() + 1);
        ctx.announce("  模组改造：充能球栏位 +1（当前 " + self.orbSlots() + "）");
        ctx.drawCardsFor(self, draw);
        Card next = nextModdedTier(draw, ctx.boostedCard());
        ctx.markBoostedCard(next);
        ctx.announce("  模组改造：本牌费用 +1 → 下次打出是 " + next.cost() + " 费");
    }

    /**
     * 取模组改造的下一档费用。
     *
     * <p>档位是从**当前这张牌自己**推出来的（0 → 1 → 2），不依赖外部计数：
     * 这样即使一张牌被复制，复制品的档位也跟它自己一致。</p>
     */
    private static Card nextModdedTier(int draw, Card current) {
        int cost = current == null ? 0 : current.cost();
        boolean plus = draw >= 2;
        if (cost <= 0) {
            return plus ? MODDED_BOOST_PLUS : MODDED_BOOST;
        }
        // cost >= 1：再打一次到第 2 档（2 费封顶）
        return plus ? MODDED_BOOST_2_PLUS : MODDED_BOOST_2;
    }

    // ==================================================================
    // v0.5.2.0 A4：战士 / 猎手补卡
    // ==================================================================
    // 说明：这一批全是**引擎已有原语就能实现**的牌（不新加任何战斗机制），
    // 所以每张牌只是"定义 + 强化版定义"，没有配套的引擎改动。
    // 缺口全表与后续批次见 docs/a4-card-plan.md。

    // ---------------------------------------------------------------- 战士（第一批 E）

    /** 上勾拳：2 费 罕见 攻击 —— 造成 13 点伤害，给予 1 层虚弱和 1 层易伤；升级虚弱/易伤 1→2。 */
    public static final Card UPPERCUT = register(new Card(
            "uppercut", "上勾拳", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 13 点伤害，给予 1 层虚弱和 1 层易伤。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 13, "「上勾拳」");
                if (target != null && target.alive()) {
                    target.addWeak(1);
                    target.addVulnerable(1);
                }
            }));

    static {
        upgrade(new Card("uppercut", "上勾拳+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 13 点伤害，给予 2 层虚弱和 2 层易伤。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 13, "「上勾拳+」");
                    if (target != null && target.alive()) {
                        target.addWeak(2);
                        target.addVulnerable(2);
                    }
                }));
    }

    /**
     * 撞击：1 费 常见 攻击 —— 造成**等同于你当前格挡值**的伤害；升级 1→0 费。
     *
     * <p>数值是"出牌那一刻的格挡值"，所以卡面文案里没有固定数字；卡面的括号里那个
     * 实时数字由 {@code CardPreview.liveDescription} 补上（当前格挡值）。</p>
     */
    public static final Card BODY_SLAM = register(new Card(
            "body_slam", "撞击", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成等同于你格挡值的伤害。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> ctx.dealDamage(self, target, self.block(), "「撞击」")));

    static {
        upgrade(new Card("body_slam", "撞击+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成等同于你格挡值的伤害。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, self.block(), "「撞击+」")));
    }

    /** 雷霆一击：1 费 常见 攻击 —— 对所有敌人造成 4 点伤害并给予 1 层易伤；升级 4→7 点。 */
    public static final Card THUNDERCLAP = register(new Card(
            "thunderclap", "雷霆一击", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 4 点伤害，给予 1 层易伤。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 4, "「雷霆一击」");
                    if (enemy.alive()) {
                        enemy.addVulnerable(1);
                    }
                }
            }));

    static {
        upgrade(new Card("thunderclap", "雷霆一击+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 7 点伤害，给予 1 层易伤。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 7, "「雷霆一击+」");
                        if (enemy.alive()) {
                            enemy.addVulnerable(1);
                        }
                    }
                }));
    }

    // ---------------------------------------------------------------- 战士（第二批 F）

    /** 灵活：0 费 常见 技能 —— 获得 2 点力量，回合结束时失去（临时力量）；升级 2→4 点。 */
    public static final Card FLEX = register(new Card(
            "flex", "灵活", CardType.SKILL, 0, CardTarget.SELF,
            "获得 2 点力量。回合结束时失去。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                self.addTemporaryStrength(2);
                ctx.announce("  灵活：力量 +2（本回合结束时收回）" + self.statusSuffix());
            }));

    static {
        upgrade(new Card("flex", "灵活+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 4 点力量。回合结束时失去。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    self.addTemporaryStrength(4);
                    ctx.announce("  灵活+：力量 +4（本回合结束时收回）" + self.statusSuffix());
                }));
    }

    /** 恐吓：0 费 罕见 技能 —— 对所有敌人施加 1 层虚弱；升级 1→2 层。消耗。 */
    public static final Card INTIMIDATE = register(new Card(
            "intimidate", "恐吓", CardType.SKILL, 0, CardTarget.ALL_ENEMIES,
            "对所有敌人施加 1 层虚弱。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    enemy.addWeak(1);
                }
            }));

    static {
        upgrade(new Card("intimidate", "恐吓+", CardType.SKILL, 0, CardTarget.ALL_ENEMIES,
                "对所有敌人施加 2 层虚弱。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        enemy.addWeak(2);
                    }
                }));
    }

    /** 见红：1 费 罕见 技能 —— 获得 2 点能量；升级 1→0 费。消耗。 */
    public static final Card SEEING_RED = register(new Card(
            "seeing_red", "见红", CardType.SKILL, 1, CardTarget.SELF,
            "获得 2 点能量。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.gainEnergy(self, 2)));

    static {
        upgrade(new Card("seeing_red", "见红+", CardType.SKILL, 0, CardTarget.SELF,
                "获得 2 点能量。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.gainEnergy(self, 2)));
    }

    // ---------------------------------------------------------------- 静默猎手（第一批）

    /** 快斩：1 费 常见 攻击 —— 造成 8 点伤害，抽 1 张牌；升级 8→12 点。 */
    public static final Card QUICK_SLASH = register(new Card(
            "quick_slash", "快斩", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 8 点伤害，抽 1 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 8, "「快斩」");
                ctx.drawCards(1);
            }));

    static {
        upgrade(new Card("quick_slash", "快斩+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 12 点伤害，抽 1 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 12, "「快斩+」");
                    ctx.drawCards(1);
                }));
    }

    /** 后空翻：1 费 常见 技能 —— 获得 5 点格挡，抽 2 张牌；升级 5→8 点格挡。 */
    public static final Card BACKFLIP = register(new Card(
            "backflip", "后空翻", CardType.SKILL, 1, CardTarget.SELF,
            "获得 5 点格挡，抽 2 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(5, "「后空翻」");
                ctx.drawCards(2);
            }));

    static {
        upgrade(new Card("backflip", "后空翻+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 8 点格挡，抽 2 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(8, "「后空翻+」");
                    ctx.drawCards(2);
                }));
    }

    /** 毒刺：1 费 常见 攻击 —— 造成 6 点伤害，给予 3 层中毒；升级 8 点伤害 + 4 层中毒。 */
    public static final Card POISONED_STAB = register(new Card(
            "poisoned_stab", "毒刺", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 6 点伤害，给予 3 层中毒。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 6, "「毒刺」");
                if (target != null && target.alive()) {
                    ctx.applyPoison(target, 3);
                }
            }));

    static {
        upgrade(new Card("poisoned_stab", "毒刺+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 8 点伤害，给予 4 层中毒。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 8, "「毒刺+」");
                    if (target != null && target.alive()) {
                        ctx.applyPoison(target, 4);
                    }
                }));
    }

    // ---------------------------------------------------------------- 静默猎手（第二批）

    /** 切片：0 费 常见 攻击 —— 造成 6 点伤害；升级 6→9 点。 */
    public static final Card SLICE = register(new Card(
            "slice", "切片", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 6 点伤害。",
            CardRarity.COMMON, SILENT,
            (ctx, self, target) -> ctx.dealDamage(self, target, 6, "「切片」")));

    static {
        upgrade(new Card("slice", "切片+", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 9 点伤害。",
                CardRarity.COMMON, SILENT,
                (ctx, self, target) -> ctx.dealDamage(self, target, 9, "「切片+」")));
    }

    /** 偏斜：0 费 常见 技能 —— 获得 4 点格挡；升级 4→7 点。 */
    public static final Card DEFLECT = register(new Card(
            "deflect", "偏斜", CardType.SKILL, 0, CardTarget.SELF, "获得 4 点格挡。",
            CardRarity.COMMON, SILENT,
            (ctx, self, target) -> ctx.grantPlayerBlock(4, "「偏斜」")));

    static {
        upgrade(new Card("deflect", "偏斜+", CardType.SKILL, 0, CardTarget.SELF, "获得 7 点格挡。",
                CardRarity.COMMON, SILENT,
                (ctx, self, target) -> ctx.grantPlayerBlock(7, "「偏斜+」")));
    }

    /** 偷袭：1 费 常见 攻击 —— 造成 7 点伤害，给予 1 层虚弱；升级 7→9 点。 */
    public static final Card SUCKER_PUNCH = register(new Card(
            "sucker_punch", "偷袭", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 7 点伤害，给予 1 层虚弱。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 7, "「偷袭」");
                if (target != null && target.alive()) {
                    target.addWeak(1);
                }
            }));

    static {
        upgrade(new Card("sucker_punch", "偷袭+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 9 点伤害，给予 1 层虚弱。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 9, "「偷袭+」");
                    if (target != null && target.alive()) {
                        target.addWeak(1);
                    }
                }));
    }

    // ---------------------------------------------------------------- 战士（第三批 G）

    /** 重锤：3 费 稀有 攻击 —— 造成 32 点伤害；升级 32→42 点。 */
    public static final Card BLUDGEON = register(new Card(
            "bludgeon", "重锤", CardType.ATTACK, 3, CardTarget.ENEMY, "造成 32 点伤害。",
            CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> ctx.dealDamage(self, target, 32, "「重锤」")));

    static {
        upgrade(new Card("bludgeon", "重锤+", CardType.ATTACK, 3, CardTarget.ENEMY, "造成 42 点伤害。",
                CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, 42, "「重锤+」")));
    }

    /** 血液动力学：1 费 罕见 攻击 —— 失去 2 点生命，造成 15 点伤害；升级 15→20 点。 */
    public static final Card HEMOKINESIS = register(new Card(
            "hemokinesis", "血液动力学", CardType.ATTACK, 1, CardTarget.ENEMY,
            "失去 2 点生命。造成 15 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.loseHp(self, 2, "「血液动力学」");
                ctx.dealDamage(self, target, 15, "「血液动力学」");
            }));

    static {
        upgrade(new Card("hemokinesis", "血液动力学+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "失去 2 点生命。造成 20 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.loseHp(self, 2, "「血液动力学+」");
                    ctx.dealDamage(self, target, 20, "「血液动力学+」");
                }));
    }

    /**
     * 卸甲：1 费 罕见 技能 —— 敌人失去 2 点力量；升级 2→3 点。消耗。
     *
     * <p>与【烟雾弹】同一套写法：目标本来就没有力量时什么也不发生（不会变成负数），
     * 并把"实际掉了多少"写进日志。</p>
     */
    public static final Card DISARM = register(new Card(
            "disarm", "卸甲", CardType.SKILL, 1, CardTarget.ENEMY,
            "敌人失去 2 点力量。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> disarm(ctx, target, 2, "「卸甲」")));

    static {
        upgrade(new Card("disarm", "卸甲+", CardType.SKILL, 1, CardTarget.ENEMY,
                "敌人失去 3 点力量。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> disarm(ctx, target, 3, "「卸甲+」")));
    }

    /** 卸甲的效果体（基础版与强化版共用）。 */
    private static void disarm(CombatContext ctx, Combatant target, int amount, String label) {
        if (target == null) {
            return;
        }
        int before = target.strength();
        target.addStrength(-amount);
        int lost = before - target.strength();
        if (lost > 0) {
            ctx.announce("  " + label + "：" + target.name() + " 力量 -" + lost + "（当前 "
                    + signed(target.strength()) + "）");
        } else {
            ctx.announce("  " + label + "：" + target.name() + " 本来就没有力量");
        }
    }

    // ---------------------------------------------------------------- 静默猎手（第三批）

    /** 匕首雨：1 费 常见 攻击 —— 对所有敌人造成 4 点伤害 2 次；升级 4→6 点。 */
    public static final Card DAGGER_SPRAY = register(new Card(
            "dagger_spray", "匕首雨", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 4 点伤害 2 次。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                for (int i = 0; i < 2; i++) {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 4, "「匕首雨」");
                    }
                }
            }));

    static {
        upgrade(new Card("dagger_spray", "匕首雨+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 6 点伤害 2 次。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    for (int i = 0; i < 2; i++) {
                        for (Enemy enemy : ctx.aliveEnemies()) {
                            ctx.dealDamage(self, enemy, 6, "「匕首雨+」");
                        }
                    }
                }));
    }

    /** 扫腿：2 费 罕见 技能 —— 给予 2 层虚弱，获得 11 点格挡；升级 11→14 点格挡。 */
    public static final Card LEG_SWEEP = register(new Card(
            "leg_sweep", "扫腿", CardType.SKILL, 2, CardTarget.ENEMY,
            "给予 2 层虚弱，获得 11 点格挡。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                if (target != null && target.alive()) {
                    target.addWeak(2);
                }
                ctx.grantPlayerBlock(11, "「扫腿」");
            }));

    static {
        upgrade(new Card("leg_sweep", "扫腿+", CardType.SKILL, 2, CardTarget.ENEMY,
                "给予 2 层虚弱，获得 14 点格挡。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    if (target != null && target.alive()) {
                        target.addWeak(2);
                    }
                    ctx.grantPlayerBlock(14, "「扫腿+」");
                }));
    }

    /** 脚部功夫：1 费 罕见 能力牌 —— 获得 2 点敏捷；升级 2→3 点。 */
    public static final Card FOOTWORK = register(new Card(
            "footwork", "脚部功夫", CardType.POWER, 1, CardTarget.SELF, "获得 2 点敏捷。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                self.addDexterity(2);
                ctx.announce("  脚部功夫：迅捷 +2" + self.statusSuffix());
            }));

    static {
        upgrade(new Card("footwork", "脚部功夫+", CardType.POWER, 1, CardTarget.SELF, "获得 3 点敏捷。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    self.addDexterity(3);
                    ctx.announce("  脚部功夫+：迅捷 +3" + self.statusSuffix());
                }));
    }

    // ---------------------------------------------------------------- 战士（第四批 H）

    /**
     * 巩固：2 费 罕见 技能 —— **当前格挡翻倍**；升级 2→1 费。
     *
     * <p>直接 {@code gainBlock(当前格挡)}，不走 {@code grantPlayerBlock}：后者的语义是"一张牌给多少格挡"，
     * 会把迅捷再算一遍 —— 而"翻倍"只是把已有的数字乘 2。</p>
     */
    public static final Card ENTRENCH = register(new Card(
            "entrench", "巩固", CardType.SKILL, 2, CardTarget.SELF, "你的格挡翻倍。",
            CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                int before = self.block();
                self.gainBlock(before);
                ctx.announce("  巩固：格挡 " + before + " → " + self.block());
            }));

    static {
        upgrade(new Card("entrench", "巩固+", CardType.SKILL, 1, CardTarget.SELF, "你的格挡翻倍。",
                CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    int before = self.block();
                    self.gainBlock(before);
                    ctx.announce("  巩固+：格挡 " + before + " → " + self.block());
                }));
    }

    /** 突破极限：1 费 稀有 技能 —— **力量翻倍**。消耗；升级去掉消耗。 */
    public static final Card LIMIT_BREAK = register(new Card(
            "limit_break", "突破极限", CardType.SKILL, 1, CardTarget.SELF, "你的力量翻倍。消耗。",
            CardRarity.RARE, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> doubleStrength(ctx, self, "「突破极限」")));

    static {
        upgrade(new Card("limit_break", "突破极限+", CardType.SKILL, 1, CardTarget.SELF, "你的力量翻倍。",
                CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> doubleStrength(ctx, self, "「突破极限+」")));
    }

    /** 突破极限的效果体（基础版与强化版的差别只在"消耗"）。 */
    private static void doubleStrength(CombatContext ctx, Combatant self, String label) {
        int before = self.strength();
        self.addStrength(before);
        ctx.announce("  " + label + "：力量 " + before + " → " + self.strength());
    }

    /** 强力打击：1 费 罕见 攻击 —— 造成 2 点伤害 4 次（升级 5 次）。消耗。 */
    public static final Card PUMMEL = register(new Card(
            "pummel", "强力打击", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 2 点伤害 4 次。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> pummelHits(ctx, self, target, 4, "「强力打击」")));

    static {
        upgrade(new Card("pummel", "强力打击+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 2 点伤害 5 次。消耗。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> pummelHits(ctx, self, target, 5, "「强力打击+」")));
    }

    /** 强力打击的效果体（段数不同，每段都是 2 点）。 */
    private static void pummelHits(CombatContext ctx, Combatant self, Combatant target, int hits, String label) {
        for (int i = 0; i < hits; i++) {
            ctx.dealDamage(self, target, 2, label);
        }
    }

    // ---------------------------------------------------------------- 静默猎手（第四批）

    /** 剧毒：1 费 常见 攻击 —— 造成 7 点伤害；若目标已有中毒，再造成 7 点；升级 7→10 点。 */
    public static final Card BANE = register(new Card(
            "bane", "剧毒", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 7 点伤害。如果目标拥有中毒，再造成 7 点伤害。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> bane(ctx, self, target, 7, "「剧毒」")));

    static {
        upgrade(new Card("bane", "剧毒+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 10 点伤害。如果目标拥有中毒，再造成 10 点伤害。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> bane(ctx, self, target, 10, "「剧毒+」")));
    }

    /** 剧毒的效果体：第一段必打，第二段只在目标身上有中毒时打。 */
    private static void bane(CombatContext ctx, Combatant self, Combatant target, int damage, String label) {
        ctx.dealDamage(self, target, damage, label);
        if (target != null && target.alive() && target.poison() > 0) {
            ctx.announce("  " + target.name() + " 身上有中毒，" + label + "再打一次");
            ctx.dealDamage(self, target, damage, label);
        }
    }

    /**
     * 专精：1 费 罕见 技能 —— 抽牌直到手牌有 6 张（升级 7 张）。
     *
     * <p>打出时这张牌**已经离开手牌**（见 {@code playCardFor} 里先 remove 再结算），
     * 所以这里直接按当前手牌数补差值即可，不需要再减 1。</p>
     */
    public static final Card EXPERTISE = register(new Card(
            "expertise", "专精", CardType.SKILL, 1, CardTarget.SELF, "抽牌直到你的手牌有 6 张。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> ctx.drawCards(Math.max(0, 6 - ctx.hand().size()))));

    static {
        upgrade(new Card("expertise", "专精+", CardType.SKILL, 1, CardTarget.SELF,
                "抽牌直到你的手牌有 7 张。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> ctx.drawCards(Math.max(0, 7 - ctx.hand().size()))));
    }

    /** 致残毒云：2 费 罕见 技能 —— 对所有敌人施加 4 层中毒和 2 层虚弱（升级 7 / 3）。消耗。 */
    public static final Card CRIPPLING_CLOUD = register(new Card(
            "crippling_cloud", "致残毒云", CardType.SKILL, 2, CardTarget.ALL_ENEMIES,
            "对所有敌人施加 4 层中毒和 2 层虚弱。消耗。", CardRarity.UNCOMMON, SILENT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> cripplingCloud(ctx, 4, 2)));

    static {
        upgrade(new Card("crippling_cloud", "致残毒云+", CardType.SKILL, 2, CardTarget.ALL_ENEMIES,
                "对所有敌人施加 7 层中毒和 3 层虚弱。消耗。", CardRarity.UNCOMMON, SILENT,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> cripplingCloud(ctx, 7, 3)));
    }

    /** 致残毒云的效果体（中毒层数 / 虚弱层数两档）。 */
    private static void cripplingCloud(CombatContext ctx, int poison, int weak) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.applyPoison(enemy, poison);
            enemy.addWeak(weak);
        }
    }

    // ---------------------------------------------------------------- 战士（第五批 I）

    /** 燃烧：1 费 罕见 能力牌 —— 获得 2 点力量；升级 2→3 点。 */
    public static final Card INFLAME = register(new Card(
            "inflame", "燃烧", CardType.POWER, 1, CardTarget.SELF, "获得 2 点力量。",
            CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                self.addStrength(2);
                ctx.announce("  燃烧：力量 +2" + self.statusSuffix());
            }));

    static {
        upgrade(new Card("inflame", "燃烧+", CardType.POWER, 1, CardTarget.SELF, "获得 3 点力量。",
                CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    self.addStrength(3);
                    ctx.announce("  燃烧+：力量 +3" + self.statusSuffix());
                }));
    }

    /** 冲击波：2 费 罕见 技能 —— 对所有敌人施加 3 层虚弱和 3 层易伤；升级各 5 层。消耗。 */
    public static final Card SHOCKWAVE = register(new Card(
            "shockwave", "冲击波", CardType.SKILL, 2, CardTarget.ALL_ENEMIES,
            "对所有敌人施加 3 层虚弱和 3 层易伤。消耗。", CardRarity.UNCOMMON, IRONCLAD,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> shockwave(ctx, 3, 3)));

    static {
        upgrade(new Card("shockwave", "冲击波+", CardType.SKILL, 2, CardTarget.ALL_ENEMIES,
                "对所有敌人施加 5 层虚弱和 5 层易伤。消耗。", CardRarity.UNCOMMON, IRONCLAD,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> shockwave(ctx, 5, 5)));
    }

    /** 冲击波的效果体。 */
    private static void shockwave(CombatContext ctx, int weak, int vulnerable) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            enemy.addWeak(weak);
            enemy.addVulnerable(vulnerable);
        }
    }

    /**
     * 飞踢：1 费 罕见 攻击 —— 造成 5 点伤害；目标有**易伤**时回 1 能量 + 抽 1 张；升级 5→8 点。
     *
     * <p>"条件奖励"的判定放在伤害**之前**取（伤害可能把目标打死，那之后状态就没了）。</p>
     */
    public static final Card DROPKICK = register(new Card(
            "dropkick", "飞踢", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 5 点伤害。如果目标拥有易伤，获得 1 点能量并抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> conditionalBonus(ctx, self, target, 5, "「飞踢」",
                    target != null && target.vulnerable() > 0)));

    static {
        upgrade(new Card("dropkick", "飞踢+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 8 点伤害。如果目标拥有易伤，获得 1 点能量并抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> conditionalBonus(ctx, self, target, 8, "「飞踢+」",
                        target != null && target.vulnerable() > 0)));
    }

    /**
     * 飞踢 / 脚后跟钩共用的效果体：先打伤害，条件满足再回 1 能量 + 抽 1 张。
     *
     * @param met 条件是否成立（调用方在**伤害之前**算好：目标可能是被这一下打死的）
     */
    private static void conditionalBonus(CombatContext ctx, Combatant self, Combatant target, int damage,
                                         String label, boolean met) {
        ctx.dealDamage(self, target, damage, label);
        if (target != null && target.alive() && met) {
            ctx.gainEnergy(self, 1);
            ctx.drawCards(1);
            ctx.announce("  " + label + "：条件满足，获得 1 点能量并抽 1 张牌");
        }
    }

    // ---------------------------------------------------------------- 静默猎手（第五批）

    /** 冲刺：2 费 罕见 攻击 —— 获得 10 点格挡，对所有敌人造成 10 点伤害；升级各 13 点。 */
    public static final Card DASH = register(new Card(
            "dash", "冲刺", CardType.ATTACK, 2, CardTarget.ALL_ENEMIES,
            "获得 10 点格挡。对所有敌人造成 10 点伤害。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(10, "「冲刺」");
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 10, "「冲刺」");
                }
            }));

    static {
        upgrade(new Card("dash", "冲刺+", CardType.ATTACK, 2, CardTarget.ALL_ENEMIES,
                "获得 13 点格挡。对所有敌人造成 13 点伤害。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(13, "「冲刺+」");
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 13, "「冲刺+」");
                    }
                }));
    }

    /** 脚后跟钩：1 费 常见 攻击 —— 造成 5 点伤害；目标有**虚弱**时回 1 能量 + 抽 1 张；升级 5→8 点。 */
    public static final Card HEEL_HOOK = register(new Card(
            "heel_hook", "脚后跟钩", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 5 点伤害。如果目标拥有虚弱，获得 1 点能量并抽 1 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> conditionalBonus(ctx, self, target, 5, "「脚后跟钩」",
                    target != null && target.weak() > 0)));

    static {
        upgrade(new Card("heel_hook", "脚后跟钩+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 8 点伤害。如果目标拥有虚弱，获得 1 点能量并抽 1 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> conditionalBonus(ctx, self, target, 8, "「脚后跟钩+」",
                        target != null && target.weak() > 0)));
    }

    /**
     * 恐惧：1 费 罕见 技能 —— 给予 99 层易伤；升级 1→0 费。消耗。
     *
     * <p>本 mod 的易伤是"层数 = 剩余回合数、每回合 -1"，所以 99 层在本场战斗里等于永久
     * （与原版"99 层易伤"的意图一致）。</p>
     */
    public static final Card TERROR = register(new Card(
            "terror", "恐惧", CardType.SKILL, 1, CardTarget.ENEMY,
            "给予 99 层易伤。消耗。", CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> terror(ctx, target)));

    static {
        upgrade(new Card("terror", "恐惧+", CardType.SKILL, 0, CardTarget.ENEMY,
                "给予 99 层易伤。消耗。", CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> terror(ctx, target)));
    }

    /** 恐惧的效果体（基础版与强化版只差费用）。 */
    private static void terror(CombatContext ctx, Combatant target) {
        if (target == null || !target.alive()) {
            return;
        }
        target.addVulnerable(99);
        ctx.announce("  " + target.name() + " 获得易伤 99" + target.statusSuffix());
    }

    // ---------------------------------------------------------------- 战士（第六批 J）

    /** 幽灵护甲：1 费 罕见 技能 —— **虚无**，获得 10 点格挡；升级 10→13 点。 */
    public static final Card GHOSTLY_ARMOR = register(new Card(
            "ghostly_armor", "幽灵护甲", CardType.SKILL, 1, CardTarget.SELF,
            "虚无。获得 10 点格挡。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.ETHEREAL),
            (ctx, self, target) -> ctx.grantPlayerBlock(10, "「幽灵护甲」")));

    static {
        upgrade(new Card("ghostly_armor", "幽灵护甲+", CardType.SKILL, 1, CardTarget.SELF,
                "虚无。获得 13 点格挡。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.ETHEREAL),
                (ctx, self, target) -> ctx.grantPlayerBlock(13, "「幽灵护甲+」")));
    }

    /** 杀戮：2 费 罕见 攻击 —— **虚无**，造成 20 点伤害；升级 20→28 点。 */
    public static final Card CARNAGE = register(new Card(
            "carnage", "杀戮", CardType.ATTACK, 2, CardTarget.ENEMY,
            "虚无。造成 20 点伤害。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.ETHEREAL),
            (ctx, self, target) -> ctx.dealDamage(self, target, 20, "「杀戮」")));

    static {
        upgrade(new Card("carnage", "杀戮+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "虚无。造成 28 点伤害。", CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.ETHEREAL),
                (ctx, self, target) -> ctx.dealDamage(self, target, 28, "「杀戮+」")));
    }

    /** 鲁莽冲锋：0 费 罕见 攻击 —— 造成 7 点伤害，把一张『眩晕』洗入抽牌堆；升级 7→10 点。 */
    public static final Card RECKLESS_CHARGE = register(new Card(
            "reckless_charge", "鲁莽冲锋", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 7 点伤害。将一张『眩晕』洗入你的抽牌堆。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 7, "「鲁莽冲锋」");
                ctx.addToDrawPile(self, DAZED, 1);
            }));

    static {
        upgrade(new Card("reckless_charge", "鲁莽冲锋+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 10 点伤害。将一张『眩晕』洗入你的抽牌堆。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 10, "「鲁莽冲锋+」");
                    ctx.addToDrawPile(self, DAZED, 1);
                }));
    }

    // ---------------------------------------------------------------- 静默猎手（第六批）

    /** 千疮百孔：2 费 罕见 攻击 —— 造成 3 点伤害 5 次；升级 3→4 点。 */
    public static final Card RIDDLE_WITH_HOLES = register(new Card(
            "riddle_with_holes", "千疮百孔", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 3 点伤害 5 次。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> multiHit(ctx, self, target, 3, 5, "「千疮百孔」")));

    static {
        upgrade(new Card("riddle_with_holes", "千疮百孔+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 4 点伤害 5 次。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> multiHit(ctx, self, target, 4, 5, "「千疮百孔+」")));
    }

    /** 多段攻击的效果体（每段独立结算：力量 / 虚弱 / 易伤都逐段生效）。 */
    private static void multiHit(CombatContext ctx, Combatant self, Combatant target, int damage, int hits,
                                String label) {
        for (int i = 0; i < hits; i++) {
            ctx.dealDamage(self, target, damage, label);
        }
    }

    /**
     * 飞刀：1 费 罕见 攻击 —— **手牌中每张技能牌**造成 4 点伤害；升级 4→6 点。
     *
     * <p>打出时这张牌已经离开手牌，所以数的就是"剩下的手牌里有几张技能牌"。
     * 每一张独立结算一次伤害（力量逐次生效，与原版一致）。</p>
     */
    public static final Card FLECHETTES = register(new Card(
            "flechettes", "飞刀", CardType.ATTACK, 1, CardTarget.ENEMY,
            "手牌中每张技能牌造成 4 点伤害。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> flechettes(ctx, self, target, 4, "「飞刀」")));

    static {
        upgrade(new Card("flechettes", "飞刀+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "手牌中每张技能牌造成 6 点伤害。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> flechettes(ctx, self, target, 6, "「飞刀+」")));
    }

    /** 飞刀的效果体：按手牌里的技能牌数量打等量的段数。 */
    private static void flechettes(CombatContext ctx, Combatant self, Combatant target, int damage, String label) {
        int skills = 0;
        for (Card card : ctx.hand()) {
            if (card.type() == CardType.SKILL) {
                skills++;
            }
        }
        ctx.announce("  " + label + "：手牌里有 " + skills + " 张技能牌");
        multiHit(ctx, self, target, damage, skills, label);
    }

    /** 全力攻击：1 费 罕见 攻击 —— 对所有敌人造成 10 点伤害，然后**随机弃掉一张手牌**；升级 14 点。 */
    public static final Card ALL_OUT_ATTACK = register(new Card(
            "all_out_attack", "全力攻击", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 10 点伤害。随机弃掉一张牌。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 10, "「全力攻击」");
                }
                discardRandom(ctx, self);
            }));

    static {
        upgrade(new Card("all_out_attack", "全力攻击+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 14 点伤害。随机弃掉一张牌。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 14, "「全力攻击+」");
                    }
                    discardRandom(ctx, self);
                }));
    }

    /** 随机弃掉一张手牌（A4-6【全力攻击】；手牌为空时什么也不发生）。 */
    private static void discardRandom(CombatContext ctx, Combatant self) {
        int size = ctx.hand().size();
        if (size <= 0) {
            return;
        }
        Card discarded = ctx.discardFromHand(self, ctx.nextRandomInt(size));
        if (discarded != null) {
            ctx.announce("  随机弃掉了「" + discarded.name() + "」");
        }
    }

    // ---------------------------------------------------------------- 战士（第七批 K）

    /**
     * 献祭：2 费 稀有 攻击 —— 对所有敌人造成 21 点伤害，把一张『灼伤』放进你的弃牌堆；升级 28 点。
     *
     * <p>『灼伤』是状态牌：抽到手后出牌回合结束时按层数扣血（见 {@code StatusCards}）。</p>
     */
    public static final Card IMMOLATE = register(new Card(
            "immolate", "献祭", CardType.ATTACK, 2, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 21 点伤害。将一张『灼伤』放入你的弃牌堆。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 21, "「献祭」");
                }
                ctx.addToDiscardPile(self, BURN, 1);
            }));

    static {
        upgrade(new Card("immolate", "献祭+", CardType.ATTACK, 2, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 28 点伤害。将一张『灼伤』放入你的弃牌堆。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 28, "「献祭+」");
                    }
                    ctx.addToDiscardPile(self, BURN, 1);
                }));
    }

    /**
     * 恶魔火焰：2 费 稀有 攻击 —— **消耗你的所有手牌**，每张造成 7 点伤害；升级每张 10 点。消耗。
     *
     * <p>打出时这张牌已经离开手牌，所以消耗的是"剩下的手牌"（与原版一致）。</p>
     */
    public static final Card FIEND_FIRE = register(new Card(
            "fiend_fire", "恶魔火焰", CardType.ATTACK, 2, CardTarget.ENEMY,
            "消耗你的所有手牌。每张消耗的牌造成 7 点伤害。消耗。", CardRarity.RARE, IRONCLAD,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> fiendFire(ctx, self, target, 7, "「恶魔火焰」")));

    static {
        upgrade(new Card("fiend_fire", "恶魔火焰+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "消耗你的所有手牌。每张消耗的牌造成 10 点伤害。消耗。", CardRarity.RARE, IRONCLAD,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> fiendFire(ctx, self, target, 10, "「恶魔火焰+」")));
    }

    /** 恶魔火焰的效果体：先烧光手牌（拿到张数），再按张数打伤害。 */
    private static void fiendFire(CombatContext ctx, Combatant self, Combatant target, int damage, String label) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < ctx.hand().size(); i++) {
            all.add(i);
        }
        int burned = ctx.exhaustFromHand(self, all);
        ctx.announce("  " + label + "：消耗了 " + burned + " 张手牌");
        multiHit(ctx, self, target, damage, burned, label);
    }

    /** 斩断灵魂：2 费 罕见 攻击 —— 消耗所有**非攻击牌**，造成 16 点伤害；升级 16→22 点。 */
    public static final Card SEVER_SOUL = register(new Card(
            "sever_soul", "斩断灵魂", CardType.ATTACK, 2, CardTarget.ENEMY,
            "消耗你手中所有非攻击牌。造成 16 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> severSoul(ctx, self, target, 16, "「斩断灵魂」")));

    static {
        upgrade(new Card("sever_soul", "斩断灵魂+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "消耗你手中所有非攻击牌。造成 22 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> severSoul(ctx, self, target, 22, "「斩断灵魂+」")));
    }

    /** 斩断灵魂的效果体：挑出非攻击牌的下标交给 {@code exhaustFromHand}。 */
    private static void severSoul(CombatContext ctx, Combatant self, Combatant target, int damage, String label) {
        List<Card> hand = ctx.hand();
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type() != CardType.ATTACK) {
                targets.add(i);
            }
        }
        int burned = ctx.exhaustFromHand(self, targets);
        ctx.announce("  " + label + "：消耗了 " + burned + " 张非攻击牌");
        ctx.dealDamage(self, target, damage, label);
    }

    // ---------------------------------------------------------------- 静默猎手（第七批）

    /** 闪避翻滚：1 费 常见 技能 —— 获得 4 点格挡，下回合再获得 4 点；升级各 4→6 点。 */
    public static final Card DODGE_AND_ROLL = register(new Card(
            "dodge_and_roll", "闪避翻滚", CardType.SKILL, 1, CardTarget.SELF,
            "获得 4 点格挡。下回合获得 4 点格挡。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(4, "「闪避翻滚」");
                self.addNextTurnBlock(4);
            }));

    static {
        upgrade(new Card("dodge_and_roll", "闪避翻滚+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 6 点格挡。下回合获得 6 点格挡。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(6, "「闪避翻滚+」");
                    self.addNextTurnBlock(6);
                }));
    }

    /** 串刺：X 费 罕见 攻击 —— 造成 7 点伤害 X 次；升级每段 7→10 点。 */
    public static final Card SKEWER = registerXCost(new Card(
            "skewer", "串刺", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 7 点伤害 X 次。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> multiHit(ctx, self, target, 7, ctx.lastX(self), "「串刺」")));

    static {
        upgrade(new Card("skewer", "串刺+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 10 点伤害 X 次。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> multiHit(ctx, self, target, 10, ctx.lastX(self), "「串刺+」")));
    }

    /**
     * 逃生计划：0 费 罕见 技能 —— 抽 1 张牌；抽到的是**技能牌**就再获得 3 点格挡；升级 3→5 点。
     *
     * <p>判定用的是"抽完之后手牌里最后一张"（{@code drawCards} 是往手牌尾部追加），
     * 这样连"抽牌堆空了、把弃牌堆洗回来"的情况也判得对；
     * 手牌已满时那张牌直接进弃牌堆、不算抽到，也就不给格挡。</p>
     */
    public static final Card ESCAPE_PLAN = register(new Card(
            "escape_plan", "逃生计划", CardType.SKILL, 0, CardTarget.SELF,
            "抽 1 张牌。如果抽到的是技能牌，获得 3 点格挡。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> escapePlan(ctx, 3)));

    static {
        upgrade(new Card("escape_plan", "逃生计划+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 1 张牌。如果抽到的是技能牌，获得 5 点格挡。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> escapePlan(ctx, 5)));
    }

    /** 逃生计划的效果体（格挡两档）。 */
    private static void escapePlan(CombatContext ctx, int block) {
        int before = ctx.hand().size();
        ctx.drawCards(1);
        List<Card> hand = ctx.hand();
        if (hand.size() > before && hand.get(hand.size() - 1).type() == CardType.SKILL) {
            ctx.grantPlayerBlock(block, "「逃生计划」");
        } else {
            ctx.announce("  逃生计划：抽到的不是技能牌，没有获得格挡");
        }
    }

    // ---------------------------------------------------------------- 战士（第八批 L）

    /**
     * 第二次呼吸：1 费 罕见 技能 —— 消耗所有非攻击牌，每张获得 5 点格挡；升级每张 5→7 点。
     *
     * <p>逐张给格挡（而不是一次给 5×N）：敏捷对**每一张**都生效，与原版一致。</p>
     */
    public static final Card SECOND_WIND = register(new Card(
            "second_wind", "第二次呼吸", CardType.SKILL, 1, CardTarget.SELF,
            "消耗你手中所有非攻击牌。每张获得 5 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> secondWind(ctx, self, 5, "「第二次呼吸」")));

    static {
        upgrade(new Card("second_wind", "第二次呼吸+", CardType.SKILL, 1, CardTarget.SELF,
                "消耗你手中所有非攻击牌。每张获得 7 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> secondWind(ctx, self, 7, "「第二次呼吸+」")));
    }

    /** 第二次呼吸的效果体。 */
    private static void secondWind(CombatContext ctx, Combatant self, int blockPerCard, String label) {
        int burned = ctx.exhaustFromHand(self, nonAttackIndices(ctx));
        ctx.announce("  " + label + "：消耗了 " + burned + " 张非攻击牌");
        for (int i = 0; i < burned; i++) {
            ctx.gainBlockFor(self, CombatContext.previewBlock(self, blockPerCard));
        }
    }

    /** 手里所有**非攻击牌**的下标（斩断灵魂 / 第二次呼吸共用）。 */
    private static List<Integer> nonAttackIndices(CombatContext ctx) {
        List<Card> hand = ctx.hand();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type() != CardType.ATTACK) {
                indices.add(i);
            }
        }
        return indices;
    }

    /** 坚毅：1 费 常见 技能 —— 获得 7 点格挡，然后**随机消耗**一张手牌；升级 7→9 点格挡。 */
    public static final Card TRUE_GRIT = register(new Card(
            "true_grit", "坚毅", CardType.SKILL, 1, CardTarget.SELF,
            "获得 7 点格挡。随机消耗一张手牌。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(7, "「坚毅」");
                exhaustRandom(ctx, self, "「坚毅」");
            }));

    static {
        upgrade(new Card("true_grit", "坚毅+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 9 点格挡。随机消耗一张手牌。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(9, "「坚毅+」");
                    exhaustRandom(ctx, self, "「坚毅+」");
                }));
    }

    /** 随机消耗一张手牌（A4-8【坚毅】；手牌为空时什么也不发生）。 */
    private static void exhaustRandom(CombatContext ctx, Combatant self, String label) {
        int size = ctx.hand().size();
        if (size <= 0) {
            return;
        }
        int done = ctx.exhaustFromHand(self, List.of(ctx.nextRandomInt(size)));
        if (done > 0) {
            ctx.announce("  " + label + "：随机消耗了 1 张手牌");
        }
    }

    /** 野性打击：1 费 常见 攻击 —— 造成 12 点伤害，把一张『伤口』洗入抽牌堆；升级 12→17 点。 */
    public static final Card WILD_STRIKE = register(new Card(
            "wild_strike", "野性打击", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 12 点伤害。将一张『伤口』洗入你的抽牌堆。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 12, "「野性打击」");
                ctx.addToDrawPile(self, WOUND, 1);
            }));

    static {
        upgrade(new Card("wild_strike", "野性打击+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 17 点伤害。将一张『伤口』洗入你的抽牌堆。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 17, "「野性打击+」");
                    ctx.addToDrawPile(self, WOUND, 1);
                }));
    }

    // ---------------------------------------------------------------- 静默猎手（第八批）

    /** 死吧死吧死吧：1 费 稀有 攻击 —— 对所有敌人造成 13 点伤害；升级 13→17 点。消耗。 */
    public static final Card DIE_DIE_DIE = register(new Card(
            "die_die_die", "死吧死吧死吧", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 13 点伤害。消耗。", CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                for (Enemy enemy : ctx.aliveEnemies()) {
                    ctx.dealDamage(self, enemy, 13, "「死吧死吧死吧」");
                }
            }));

    static {
        upgrade(new Card("die_die_die", "死吧死吧死吧+", CardType.ATTACK, 1, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 17 点伤害。消耗。", CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    for (Enemy enemy : ctx.aliveEnemies()) {
                        ctx.dealDamage(self, enemy, 17, "「死吧死吧死吧+」");
                    }
                }));
    }

    /**
     * 分身：X 费 稀有 技能 —— 抽 X 张牌、获得 X 点能量；升级 X+1。
     *
     * <p>X 费牌的 X = 打出时花掉的能量（引擎先扣光再结算），所以基础版"花 X 回 X"净花费为 0、
     * 换来 X 张手牌；升级版净赚 1 点能量。</p>
     */
    public static final Card DOPPELGANGER = registerXCost(new Card(
            "doppelganger", "分身", CardType.SKILL, 0, CardTarget.SELF,
            "抽 X 张牌。获得 X 点能量。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> doppelganger(ctx, self, 0)));

    static {
        upgrade(new Card("doppelganger", "分身+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 X+1 张牌。获得 X+1 点能量。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> doppelganger(ctx, self, 1)));
    }

    /** 分身的效果体（{@code bonus} = 升级带来的 +1）。 */
    private static void doppelganger(CombatContext ctx, Combatant self, int bonus) {
        int amount = ctx.lastX(self) + bonus;
        if (amount <= 0) {
            return;
        }
        ctx.gainEnergy(self, amount);
        ctx.drawCardsFor(self, amount);
    }

    /**
     * 不适：X 费 稀有 技能 —— 敌人失去 X 点力量并获得 X 层虚弱；升级 X+1。消耗。
     *
     * <p>先减力量再叠虚弱（与原版卡面顺序一致）；本 mod 里力量与虚弱会互相抵消，
     * 所以"力量被虚弱再抵掉一层"是符合本 mod 规则的正常结果。</p>
     */
    public static final Card MALAISE = registerXCost(new Card(
            "malaise", "不适", CardType.SKILL, 0, CardTarget.ENEMY,
            "敌人失去 X 点力量，并获得 X 层虚弱。消耗。", CardRarity.RARE, SILENT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> malaise(ctx, self, target, 0)));

    static {
        upgrade(new Card("malaise", "不适+", CardType.SKILL, 0, CardTarget.ENEMY,
                "敌人失去 X+1 点力量，并获得 X+1 层虚弱。消耗。", CardRarity.RARE, SILENT,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> malaise(ctx, self, target, 1)));
    }

    /** 不适的效果体（{@code bonus} = 升级带来的 +1）。 */
    private static void malaise(CombatContext ctx, Combatant self, Combatant target, int bonus) {
        if (target == null) {
            return;
        }
        int amount = ctx.lastX(self) + bonus;
        if (amount <= 0) {
            return;
        }
        disarm(ctx, target, amount, "「不适」");
        target.addWeak(amount);
    }

    // ---------------------------------------------------------------- 战士（第九批 M）

    /**
     * 头槌：1 费 常见 攻击 —— 造成 9 点伤害，然后把**弃牌堆里选的一张**放到抽牌堆顶；升级 12 点。
     *
     * <p>选牌复用了【废料回收】的那套（{@link CombatContext.CardPickKind#SCRAP_RECYCLE}）：
     * 效果一模一样（从弃牌堆选一张 → 抽牌堆顶），所以不再新加一种 kind。</p>
     */
    public static final Card HEADBUTT = register(new Card(
            "headbutt", "头槌", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 9 点伤害。将弃牌堆中的一张牌置于抽牌堆顶部。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 9, "「头槌」");
                pickFromDiscardTop(ctx, self, "「头槌」");
            }));

    static {
        upgrade(new Card("headbutt", "头槌+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 12 点伤害。将弃牌堆中的一张牌置于抽牌堆顶部。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 12, "「头槌+」");
                    pickFromDiscardTop(ctx, self, "「头槌+」");
                }));
    }

    /** 从弃牌堆选一张放回抽牌堆顶（弃牌堆为空时不弹选牌页）。 */
    private static void pickFromDiscardTop(CombatContext ctx, Combatant self, String label) {
        if (ctx.stateOf(self) == null || ctx.stateOf(self).discardPile().isEmpty()) {
            ctx.announce("  " + label + "：弃牌堆是空的，没有牌可以放回去");
            return;
        }
        ctx.beginCardPick(self, label, CombatContext.CardPickKind.SCRAP_RECYCLE,
                CombatContext.CardPickSource.DISCARD, 1, null);
    }

    /**
     * 进食：1 费 稀有 攻击 —— 造成 10 点伤害；**若这一下杀死目标，最大生命 +3**（升级 12 伤害 / +4）。消耗。
     *
     * <p>"杀死了"的判定就是伤害结算完目标不再存活（能指定它说明它本来活着）。</p>
     */
    public static final Card FEED = register(new Card(
            "feed", "进食", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 10 点伤害。如果杀死了目标，最大生命值 +3。消耗。", CardRarity.RARE, IRONCLAD,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 10, "「进食」");
                if (target != null && !target.alive()) {
                    self.addMaxHp(3);
                    ctx.announce("  进食：目标被击杀，最大生命 +3（当前 " + self.hp() + "/"
                            + self.maxHp() + "）");
                }
            }));

    static {
        upgrade(new Card("feed", "进食+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 12 点伤害。如果杀死了目标，最大生命值 +4。消耗。", CardRarity.RARE, IRONCLAD,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 12, "「进食+」");
                    if (target != null && !target.alive()) {
                        self.addMaxHp(4);
                        ctx.announce("  进食+：目标被击杀，最大生命 +4（当前 " + self.hp() + "/"
                                + self.maxHp() + "）");
                    }
                }));
    }

    /**
     * 完美打击：2 费 常见 攻击 —— 造成 6 点伤害，牌组中每张"『打击』类"牌使伤害 +2（升级 +3）。
     *
     * <p><b>"『打击』类"的判定 = 卡牌 id 里含 {@code strike}</b>，这正是原版"名字里带 Strike"的同一批牌：
     * 打击 `strike` / 双重打击 `twin_strike` / 野性打击 `wild_strike` / 柄击 `pommel_strike` /
     * 完美打击 `perfected_strike` 自己。
     * 用中文名去匹配反而会多算【强力打击】（Pummel，原版不含 Strike）、漏算【柄击】（Pommel **Strike**），
     * 所以这里按 id 认。</p>
     */
    public static final Card PERFECTED_STRIKE = register(new Card(
            "perfected_strike", "完美打击", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 6 点伤害。牌组中每张『打击』类牌使伤害 +2。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> perfectedStrike(ctx, self, target, 2, "「完美打击」")));

    static {
        upgrade(new Card("perfected_strike", "完美打击+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 6 点伤害。牌组中每张『打击』类牌使伤害 +3。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> perfectedStrike(ctx, self, target, 3, "「完美打击+」")));
    }

    /** 完美打击的效果体（每张"打击类"牌的加成两档：2 / 3）。 */
    private static void perfectedStrike(CombatContext ctx, Combatant self, Combatant target, int perCard,
                                        String label) {
        int count = 0;
        for (Card card : ctx.deckOf(self)) {
            if (card.id().contains("strike")) {
                count++;
            }
        }
        ctx.announce("  " + label + "：牌组里有 " + count + " 张『打击』类牌（伤害 +" + count * perCard + "）");
        ctx.dealDamage(self, target, 6 + count * perCard, label);
    }

    // ---------------------------------------------------------------- 静默猎手（第九批）

    /** 卸货：1 费 稀有 攻击 —— 造成 14 点伤害，然后**弃掉手里所有非攻击牌**；升级 18 点。 */
    public static final Card UNLOAD = register(new Card(
            "unload", "卸货", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 14 点伤害。弃掉你手中所有非攻击牌。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 14, "「卸货」");
                discardNonAttacks(ctx, self, "「卸货」");
            }));

    static {
        upgrade(new Card("unload", "卸货+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 18 点伤害。弃掉你手中所有非攻击牌。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 18, "「卸货+」");
                    discardNonAttacks(ctx, self, "「卸货+」");
                }));
    }

    /** 弃掉手里所有非攻击牌（A4-9【卸货】；从后往前删，下标不会错位）。 */
    private static void discardNonAttacks(CombatContext ctx, Combatant self, String label) {
        List<Card> hand = ctx.hand();
        int done = 0;
        for (int i = hand.size() - 1; i >= 0; i--) {
            if (hand.get(i).type() != CardType.ATTACK && ctx.discardFromHand(self, i) != null) {
                done++;
            }
        }
        ctx.announce("  " + label + "：弃掉了 " + done + " 张非攻击牌");
    }

    /**
     * 准备：0 费 常见 技能 —— 抽 1 张牌，然后弃 1 张牌；升级变成**抽 2 弃 2**。
     *
     * <p>校验来源：kamigame 卡页（准备：カードを1枚引き、1枚捨てる／强化后"2枚引き、2枚捨てる"）。</p>
     */
    public static final Card PREPARED = register(new Card(
            "prepared", "准备", CardType.SKILL, 0, CardTarget.SELF,
            "抽 1 张牌。弃 1 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> prepared(ctx, self, 1)));

    static {
        upgrade(new Card("prepared", "准备+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 2 张牌。弃 2 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> prepared(ctx, self, 2)));
    }

    /** 准备的效果体（抽 N 弃 N；手牌为空时不弹选牌页）。 */
    private static void prepared(CombatContext ctx, Combatant self, int amount) {
        ctx.drawCardsFor(self, amount);
        if (ctx.hand().isEmpty()) {
            ctx.announce("  准备：手牌是空的，不用弃牌");
            return;
        }
        int picks = Math.min(amount, ctx.hand().size());
        ctx.beginCardPick(self, "「准备」", CombatContext.CardPickKind.DISCARD,
                CombatContext.CardPickSource.HAND, picks, null);
    }

    /**
     * 集中精神：0 费 罕见 技能 —— 弃 3 张牌，获得 2 点能量；升级变成**弃 2 张**（能量不变）。
     *
     * <p>校验来源：kamigame 卡页（集中：カードを3枚捨てる。エナジー2を得る／强化后"2枚捨てる"）。
     * 注意升级是**少弃一张**，不是多给能量。</p>
     */
    public static final Card CONCENTRATE = register(new Card(
            "concentrate", "集中精神", CardType.SKILL, 0, CardTarget.SELF,
            "弃 3 张牌。获得 2 点能量。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> concentrate(ctx, self, 3)));

    static {
        upgrade(new Card("concentrate", "集中精神+", CardType.SKILL, 0, CardTarget.SELF,
                "弃 2 张牌。获得 2 点能量。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> concentrate(ctx, self, 2)));
    }

    /** 集中精神的效果体（弃 N 张 + 2 能量；手牌不够就按手里有的张数弃）。 */
    private static void concentrate(CombatContext ctx, Combatant self, int amount) {
        if (ctx.hand().isEmpty()) {
            ctx.announce("  集中精神：手牌是空的，直接获得 2 点能量");
            ctx.gainEnergy(self, 2);
            return;
        }
        int picks = Math.min(amount, ctx.hand().size());
        // 能量先给：选牌是"挂起"的，等玩家选完再给会让人以为牌没生效
        ctx.gainEnergy(self, 2);
        ctx.beginCardPick(self, "「集中精神」", CombatContext.CardPickKind.DISCARD,
                CombatContext.CardPickSource.HAND, picks, null);
    }

    // ---------------------------------------------------------------- 战士（第十批 N）

    /**
     * 军备：1 费 常见 技能 —— 获得 5 点格挡，把手里**一张**牌升级（本场战斗）；升级后升级手里**全部**牌。
     *
     * <p>校验来源：kamigame 卡页（武装：5 ブロック + 手札のカード1枚をアップグレード／强化后"すべて"）。</p>
     */
    public static final Card ARMAMENTS = register(new Card(
            "armaments", "军备", CardType.SKILL, 1, CardTarget.SELF,
            "获得 5 点格挡。升级手牌中的一张牌（本场战斗）。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(5, "「军备」");
                upgradeOne(ctx, self, "「军备」");
            }));

    static {
        upgrade(new Card("armaments", "军备+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 5 点格挡。升级手牌中的所有牌（本场战斗）。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(5, "「军备+」");
                    upgradeAll(ctx, self, "「军备+」");
                }));
    }

    /** 这张牌还有没有"强化版"可用（已经是强化版 / 状态牌 → false）。 */
    private static boolean canUpgrade(Card card) {
        if (card == null) {
            return false;
        }
        // v0.5.4.1（A4-20）：改问"档位"而不是"是不是强化版" ——
        // 【灼热打击】+3 手里还能再升到 +4。
        return upgradeLevel(card) < maxUpgrades(card.id());
    }

    /** 军备：让玩家选一张能升级的牌。 */
    private static void upgradeOne(CombatContext ctx, Combatant self, String label) {
        int candidates = 0;
        for (Card card : ctx.hand()) {
            if (canUpgrade(card)) {
                candidates++;
            }
        }
        if (candidates == 0) {
            ctx.announce("  " + label + "：手里没有可以升级的牌");
            return;
        }
        ctx.beginCardPick(self, label, CombatContext.CardPickKind.UPGRADE_ONE,
                CombatContext.CardPickSource.HAND, 1, null, CardLibrary::canUpgrade, 0);
    }

    /** 军备+：把手里**所有**能升级的牌全升掉（从后往前改，下标不会错位）。 */
    private static void upgradeAll(CombatContext ctx, Combatant self, String label) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        int done = 0;
        for (int i = state.hand().size() - 1; i >= 0; i--) {
            Card card = state.hand().get(i);
            if (!canUpgrade(card)) {
                continue;
            }
            // v0.5.4.1（A4-20）：上一档（并保留附魔），不是写死 resolve(id, 1)
            Card upgraded = nextUpgradeOf(card);
            if (upgraded != null && ctx.transformInHand(self, i, upgraded.withEnchantments(card.enchantments()))) {
                done++;
            }
        }
        ctx.announce("  " + label + "：升级了 " + done + " 张手牌");
    }

    /**
     * 双持：1 费 罕见 技能 —— 复制手里一张**攻击牌或能力牌**，加 1 张进手牌；升级加 **2** 张。
     *
     * <p>校验来源：kamigame 卡页（二刀流：手札にあるアタックかパワーを複製し、1枚手札に加える／强化后"2枚"）。</p>
     */
    public static final Card DUAL_WIELD = register(new Card(
            "dual_wield", "双持", CardType.SKILL, 1, CardTarget.SELF,
            "复制手牌中的一张攻击牌或能力牌，将 1 张复制品加入手牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> duplicateInHand(ctx, self, "「双持」", 1)));

    static {
        upgrade(new Card("dual_wield", "双持+", CardType.SKILL, 1, CardTarget.SELF,
                "复制手牌中的一张攻击牌或能力牌，将 2 张复制品加入手牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> duplicateInHand(ctx, self, "「双持+」", 2)));
    }

    /** 双持的效果体（复制张数两档）。 */
    private static void duplicateInHand(CombatContext ctx, Combatant self, String label, int copies) {
        boolean any = false;
        for (Card card : ctx.hand()) {
            if (card.type() == CardType.ATTACK || card.type() == CardType.POWER) {
                any = true;
                break;
            }
        }
        if (!any) {
            ctx.announce("  " + label + "：手里没有攻击牌或能力牌可以复制");
            return;
        }
        ctx.beginCardPick(self, label, CombatContext.CardPickKind.DUPLICATE,
                CombatContext.CardPickSource.HAND, 1, null,
                card -> card.type() == CardType.ATTACK || card.type() == CardType.POWER, copies);
    }

    /** 燃烧契约：1 费 罕见 技能 —— 消耗一张手牌，抽 2 张；升级抽 3 张。 */
    public static final Card BURNING_PACT = register(new Card(
            "burning_pact", "燃烧契约", CardType.SKILL, 1, CardTarget.SELF,
            "消耗一张手牌。抽 2 张牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> burningPact(ctx, self, 2)));

    static {
        upgrade(new Card("burning_pact", "燃烧契约+", CardType.SKILL, 1, CardTarget.SELF,
                "消耗一张手牌。抽 3 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> burningPact(ctx, self, 3)));
    }

    /** 燃烧契约的效果体（抽牌数两档）。 */
    private static void burningPact(CombatContext ctx, Combatant self, int draw) {
        if (ctx.hand().isEmpty()) {
            ctx.announce("  燃烧契约：手里没有牌可以消耗，直接抽 " + draw + " 张");
            ctx.drawCardsFor(self, draw);
            return;
        }
        ctx.beginCardPick(self, "「燃烧契约」", CombatContext.CardPickKind.BURNING_PACT,
                CombatContext.CardPickSource.HAND, 1, null, null, draw);
    }

    // ---------------------------------------------------------------- 静默猎手（第十批）

    /** 杂技：1 费 常见 技能 —— 抽 3 张弃 1 张；升级抽 4 张弃 1 张。 */
    public static final Card ACROBATICS = register(new Card(
            "acrobatics", "杂技", CardType.SKILL, 1, CardTarget.SELF,
            "抽 3 张牌。弃 1 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> drawThenDiscard(ctx, self, 3, 1, "「杂技」")));

    static {
        upgrade(new Card("acrobatics", "杂技+", CardType.SKILL, 1, CardTarget.SELF,
                "抽 4 张牌。弃 1 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> drawThenDiscard(ctx, self, 4, 1, "「杂技+」")));
    }

    /** 先抽后弃（杂技 / 飞刀投掷共用）。 */
    private static void drawThenDiscard(CombatContext ctx, Combatant self, int draw, int discard, String label) {
        ctx.drawCardsFor(self, draw);
        if (ctx.hand().isEmpty()) {
            ctx.announce("  " + label + "：手牌是空的，不用弃牌");
            return;
        }
        ctx.beginCardPick(self, label, CombatContext.CardPickKind.DISCARD,
                CombatContext.CardPickSource.HAND, Math.min(discard, ctx.hand().size()), null);
    }

    /** 飞刀投掷：1 费 常见 攻击 —— 造成 9 点伤害，抽 1 张弃 1 张；升级 12 点伤害。 */
    public static final Card DAGGER_THROW = register(new Card(
            "dagger_throw", "飞刀投掷", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 9 点伤害。抽 1 张牌。弃 1 张牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.dealDamage(self, target, 9, "「飞刀投掷」");
                drawThenDiscard(ctx, self, 1, 1, "「飞刀投掷」");
            }));

    static {
        upgrade(new Card("dagger_throw", "飞刀投掷+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 12 点伤害。抽 1 张牌。弃 1 张牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.dealDamage(self, target, 12, "「飞刀投掷+」");
                    drawThenDiscard(ctx, self, 1, 1, "「飞刀投掷+」");
                }));
    }

    /**
     * 计算赌博：0 费 罕见 技能 —— **弃掉全部手牌，再抽等量的牌**。消耗；升级去掉消耗。
     *
     * <p>校验来源：kamigame 卡页（イカサマ：手札をすべて捨て、同じ枚数だけカードを引く。廃棄／强化版没有"廃棄"）。</p>
     */
    public static final Card CALCULATED_GAMBLE = register(new Card(
            "calculated_gamble", "计算赌博", CardType.SKILL, 0, CardTarget.SELF,
            "弃掉你的所有手牌，然后抽等量的牌。消耗。", CardRarity.UNCOMMON, SILENT,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> calculatedGamble(ctx, self)));

    static {
        upgrade(new Card("calculated_gamble", "计算赌博+", CardType.SKILL, 0, CardTarget.SELF,
                "弃掉你的所有手牌，然后抽等量的牌。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> calculatedGamble(ctx, self)));
    }

    /** 计算赌博的效果体：弃光手牌（从后往前），再抽回等量。 */
    private static void calculatedGamble(CombatContext ctx, Combatant self) {
        int count = ctx.hand().size();
        for (int i = count - 1; i >= 0; i--) {
            ctx.discardFromHand(self, i);
        }
        ctx.announce("  计算赌博：弃掉了 " + count + " 张手牌");
        ctx.drawCardsFor(self, count);
    }

    // ==================================================================
    // v0.5.2.4 A4-11：小刀（Shiv）与"牌堆顶"这一族
    // ==================================================================

    /**
     * 小刀（Shiv）：0 费 攻击 **生成牌** —— 造成 4 点伤害，消耗。
     *
     * <p>它不进任何奖励池（{@link #registerGenerated}），只能被【刃舞】【斗篷与匕首】
     * 【钢铁风暴】这类牌生成。【精准】会给它加伤害（{@link Combatant#shivBonus()}）。</p>
     *
     * <p>⚠️ 注册方式：基础版走 {@code registerGenerated}，强化版必须走 {@code upgrade}
     * （两者**同 id**）—— 不能用 {@code registerGenerated(base, upgraded)}：那会把两者都
     * 塞进 {@code BY_ID} 的同一个键上，后一个覆盖前一个，结果"基础版没了、强化版也没登记"，
     * 直接被"每张非状态牌都要有强化版"那条自检抓出来。</p>
     */
    public static final Card SHIV = registerGenerated(new Card(
            "shiv", "小刀", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 4 点伤害。消耗。",
            CardRarity.COMMON, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.dealDamage(self, target, 4 + self.shivBonus(), "「小刀」")));

    static {
        upgrade(new Card("shiv", "小刀+", CardType.ATTACK, 0, CardTarget.ENEMY, "造成 6 点伤害。消耗。",
                CardRarity.COMMON, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.dealDamage(self, target, 6 + self.shivBonus(), "「小刀+」")));
    }

    // ---------------------------------------------------------------- 战士（第十一批 O）

    /**
     * 战吼：0 费 常见 技能 —— 抽 1 张牌，然后把手里一张放到抽牌堆顶。消耗；升级改成**抽 2 张**（仍然消耗）。
     *
     * <p>校验来源：kamigame 卡页（雄叫び：カードを1枚引く。手札のカードを山札の1番上に置く。廃棄／
     * 强化后"2枚引く"、廃棄**保留**）。⚠️ 这里又踩了一次"凭印象会写错"：我原以为升级是去掉消耗。</p>
     */
    public static final Card WARCRY = register(new Card(
            "warcry", "战吼", CardType.SKILL, 0, CardTarget.SELF,
            "抽 1 张牌。将一张手牌置于抽牌堆顶部。消耗。", CardRarity.COMMON, IRONCLAD,
            Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> warcry(ctx, self, 1)));

    static {
        upgrade(new Card("warcry", "战吼+", CardType.SKILL, 0, CardTarget.SELF,
                "抽 2 张牌。将一张手牌置于抽牌堆顶部。消耗。", CardRarity.COMMON, IRONCLAD,
                Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> warcry(ctx, self, 2)));
    }

    /** 战吼的效果体（抽牌数两档）。 */
    private static void warcry(CombatContext ctx, Combatant self, int draw) {
        ctx.drawCardsFor(self, draw);
        if (ctx.hand().isEmpty()) {
            ctx.announce("  战吼：手牌是空的，没有牌可以放回去");
            return;
        }
        ctx.beginCardPick(self, "「战吼」", CombatContext.CardPickKind.PUT_ON_TOP,
                CombatContext.CardPickSource.HAND, 1, null);
    }

    /**
     * 浩劫：1 费 常见 技能 —— 打出抽牌堆顶的一张牌，然后消耗它；升级 1→0 费。
     *
     * <p>校验来源：kamigame 卡页（荒廃：山札の1番上のカードをプレイし、廃棄する／コスト 1→0）。</p>
     */
    public static final Card HAVOC = register(new Card(
            "havoc", "浩劫", CardType.SKILL, 1, CardTarget.SELF,
            "打出抽牌堆顶部的一张牌，并将其消耗。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> havoc(ctx, self)));

    static {
        upgrade(new Card("havoc", "浩劫+", CardType.SKILL, 0, CardTarget.SELF,
                "打出抽牌堆顶部的一张牌，并将其消耗。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> havoc(ctx, self)));
    }

    /** 浩劫的效果体（基础版与强化版只差费用）。 */
    private static void havoc(CombatContext ctx, Combatant self) {
        Card played = ctx.playTopOfDrawPile(self);
        if (played == null) {
            ctx.announce("  浩劫：抽牌堆是空的，什么也没发生");
            return;
        }
        ctx.announce("  浩劫：打出了「" + played.name() + "」并消耗它");
    }

    /**
     * 察觉弱点：1 费 罕见 技能 —— 若目标**意图攻击**，获得 3 点力量；升级 4 点。
     *
     * <p>校验来源：kamigame 卡页（弱点発見：敵が攻撃する意思表示をしているなら、筋力3を得る／强化后4）。</p>
     */
    public static final Card SPOT_WEAKNESS = register(new Card(
            "spot_weakness", "察觉弱点", CardType.SKILL, 1, CardTarget.ENEMY,
            "如果目标意图攻击，获得 3 点力量。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> spotWeakness(ctx, self, target, 3)));

    static {
        upgrade(new Card("spot_weakness", "察觉弱点+", CardType.SKILL, 1, CardTarget.ENEMY,
                "如果目标意图攻击，获得 4 点力量。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> spotWeakness(ctx, self, target, 4)));
    }

    /** 察觉弱点的效果体（力量两档）。 */
    private static void spotWeakness(CombatContext ctx, Combatant self, Combatant target, int strength) {
        if (!(target instanceof Enemy enemy) || !enemy.alive()) {
            ctx.announce("  察觉弱点：没有可以观察的目标");
            return;
        }
        if (!enemy.hasAction(com.slaythecraft.combat.core.IntentType.ATTACK)) {
            ctx.announce("  " + enemy.name() + " 这一轮不打算攻击，察觉弱点没有效果");
            return;
        }
        self.addStrength(strength);
        ctx.announce("  " + enemy.name() + " 打算攻击 → 力量 +" + strength + self.statusSuffix());
    }

    // ---------------------------------------------------------------- 静默猎手（第十一批）

    /** 刃舞：1 费 常见 技能 —— 把 3 张『小刀』加入手牌；升级 4 张。 */
    public static final Card BLADE_DANCE = register(new Card(
            "blade_dance", "刃舞", CardType.SKILL, 1, CardTarget.SELF,
            "将 3 张『小刀』加入你的手牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> addShivs(ctx, self, 3, "「刃舞」")));

    static {
        upgrade(new Card("blade_dance", "刃舞+", CardType.SKILL, 1, CardTarget.SELF,
                "将 4 张『小刀』加入你的手牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> addShivs(ctx, self, 4, "「刃舞+」")));
    }

    /** 生成若干张『小刀』进手牌（刃舞 / 斗篷与匕首共用）。 */
    private static void addShivs(CombatContext ctx, Combatant self, int count, String label) {
        int added = ctx.addCardsToHand(self, SHIV, count);
        ctx.announce("  " + label + "：加入 " + added + " 张『小刀』");
    }

    /** 斗篷与匕首：1 费 常见 技能 —— 获得 6 点格挡 + 1 张『小刀』；升级 2 张（格挡不变）。 */
    public static final Card CLOAK_AND_DAGGER = register(new Card(
            "cloak_and_dagger", "斗篷与匕首", CardType.SKILL, 1, CardTarget.SELF,
            "获得 6 点格挡。将 1 张『小刀』加入你的手牌。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> {
                ctx.grantPlayerBlock(6, "「斗篷与匕首」");
                addShivs(ctx, self, 1, "「斗篷与匕首」");
            }));

    static {
        upgrade(new Card("cloak_and_dagger", "斗篷与匕首+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 6 点格挡。将 2 张『小刀』加入你的手牌。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> {
                    ctx.grantPlayerBlock(6, "「斗篷与匕首+」");
                    addShivs(ctx, self, 2, "「斗篷与匕首+」");
                }));
    }

    /** 精准：1 费 罕见 能力牌 —— 『小刀』额外造成 4 点伤害；升级 6 点。 */
    public static final Card ACCURACY = register(new Card(
            "accuracy", "精准", CardType.POWER, 1, CardTarget.SELF,
            "『小刀』额外造成 4 点伤害。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> {
                self.addShivBonus(4);
                ctx.announce("  精准：小刀伤害 +4（当前 +" + self.shivBonus() + "）");
            }));

    static {
        upgrade(new Card("accuracy", "精准+", CardType.POWER, 1, CardTarget.SELF,
                "『小刀』额外造成 6 点伤害。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> {
                    self.addShivBonus(6);
                    ctx.announce("  精准+：小刀伤害 +6（当前 +" + self.shivBonus() + "）");
                }));
    }

    // ==================================================================
    // v0.5.3.1（用户）：叛逃掠夺者再补 5 张
    // ==================================================================

    /**
     * 午时已到：3 费 稀有 攻击 消耗 —— 对所有敌人造成 20 点伤害；意图为攻击的敌人**再**吃 15 点；
     * 升级把"额外"那一档抬到 20。
     *
     * <p>实现上是**两段伤害**（先 20，再对打算攻击的敌人补一下），不是把 20+15 合成一次 35 ——
     * 卡面写的是"额外造成 N 点伤害"，两段各自吃力量 / 虚弱 / 易伤，这与本模组其它
     * "额外造成"类卡牌（例如【快跃！这货要逃】）一致。</p>
     */
    public static final Card HIGH_NOON = register(new Card(
            "high_noon", "午时已到", CardType.ATTACK, 3, CardTarget.ALL_ENEMIES,
            "对所有敌人造成 20 点伤害。对意图攻击的敌人额外造成 15 点伤害。消耗。",
            CardRarity.RARE, PILLAGER, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> highNoon(ctx, self, 15)));

    static {
        upgrade(new Card("high_noon", "午时已到+", CardType.ATTACK, 3, CardTarget.ALL_ENEMIES,
                "对所有敌人造成 20 点伤害。对意图攻击的敌人额外造成 20 点伤害。消耗。",
                CardRarity.RARE, PILLAGER, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> highNoon(ctx, self, 20)));
    }

    /** 午时已到的效果体（"额外"那一档两档：15 / 20）。 */
    private static void highNoon(CombatContext ctx, Combatant self, int extra) {
        int attacking = 0;
        for (Enemy enemy : ctx.aliveEnemies()) {
            if (enemy.hasAction(IntentType.ATTACK)) {
                attacking++;
            }
        }
        ctx.announce("  午时已到：" + ctx.aliveEnemies().size() + " 个敌人各受 20 点，其中 " + attacking
                + " 个意图攻击的额外受 " + extra + " 点");
        for (Enemy enemy : ctx.aliveEnemies()) {
            boolean willStrike = enemy.hasAction(IntentType.ATTACK);
            ctx.dealDamage(self, enemy, 20, "「午时已到」");
            if (willStrike && enemy.alive()) {
                ctx.dealDamage(self, enemy, extra, "「午时已到」额外");
            }
        }
    }

    /**
     * 山火：2 费 稀有 攻击 —— 造成 14 点伤害，并给予**等同于这次伤害**的灼烧；升级 17 点。
     *
     * <p>"造成伤害"取的是**格挡吸收之前**的那个数值（力量 / 虚弱 / 易伤算完之后），
     * 与【火焰附加】的口径一致（见 {@code CombatContext.fireAspectBurn}）。
     * 目标若免疫灼烧（地狱生物），层数不会加上去，这里按**实际加上的层数**写日志。</p>
     */
    public static final Card WILDFIRE = register(new Card(
            "wildfire", "山火", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 14 点伤害。给予等同于造成伤害的灼烧。", CardRarity.RARE, PILLAGER,
            (ctx, self, target) -> wildfire(ctx, self, target, 14)));

    static {
        upgrade(new Card("wildfire", "山火+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 17 点伤害。给予等同于造成伤害的灼烧。", CardRarity.RARE, PILLAGER,
                (ctx, self, target) -> wildfire(ctx, self, target, 17)));
    }

    /** 山火的效果体（伤害两档：14 / 17）。 */
    private static void wildfire(CombatContext ctx, Combatant self, Combatant target, int damage) {
        if (!(target instanceof Enemy enemy) || !enemy.alive()) {
            ctx.announce("  山火：没有可以点燃的目标");
            return;
        }
        // 先算"这次会打多少"（与战报里那个数字、以及【火焰附加】的口径一致），再真的打
        int shown = Math.max(0, CombatContext.previewDamage(self, enemy, damage));
        ctx.dealDamage(self, enemy, damage, "「山火」");
        if (!enemy.alive()) {
            ctx.announce("  山火：" + enemy.name() + " 被烧死了，没有目标可以附加灼烧");
            return;
        }
        int before = enemy.burn();
        enemy.addBurn(shown);
        int gained = enemy.burn() - before;
        if (gained <= 0) {
            ctx.announce("  山火：造成 " + shown + " 点伤害 → " + enemy.name()
                    + " 免疫灼烧（" + enemy.factionLabel() + "）");
        } else {
            ctx.announce("  山火：造成 " + shown + " 点伤害 → 附加灼烧 " + gained + enemy.statusSuffix());
        }
    }

    /** 众生皆苦：1 费（升级 0）稀有 能力 —— 每打出一张牌，所有敌人获得 1 层凋零。 */
    public static final Card ALL_SUFFERING = register(new Card(
            "all_suffering", "众生皆苦", CardType.POWER, 1, CardTarget.SELF,
            "每当你打出一张牌，给予所有敌人 1 层凋零。", CardRarity.RARE, PILLAGER,
            (ctx, self, target) -> allSuffering(ctx, self)));

    static {
        upgrade(new Card("all_suffering", "众生皆苦+", CardType.POWER, 0, CardTarget.SELF,
                "每当你打出一张牌，给予所有敌人 1 层凋零。", CardRarity.RARE, PILLAGER,
                (ctx, self, target) -> allSuffering(ctx, self)));
    }

    /** 众生皆苦的效果体（基础版与强化版只差费用）。 */
    private static void allSuffering(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.allSuffering = true;
        ctx.announce("  【众生皆苦】生效：每打出一张牌 → 所有敌人获得凋零 1");
    }

    /** 劫掠形态：3 费 稀有 能力 —— 每打出一张攻击牌，加一张「弩箭射击」进手牌；升级加强化版。 */
    public static final Card PILLAGE_FORM = register(new Card(
            "pillage_form", "劫掠形态", CardType.POWER, 3, CardTarget.SELF,
            "每当你打出一张攻击牌，将一张「弩箭射击」加入你的手牌。", CardRarity.RARE, PILLAGER,
            (ctx, self, target) -> pillageForm(ctx, self, false)));

    static {
        upgrade(new Card("pillage_form", "劫掠形态+", CardType.POWER, 3, CardTarget.SELF,
                "每当你打出一张攻击牌，将一张「弩箭射击+」加入你的手牌。", CardRarity.RARE, PILLAGER,
                (ctx, self, target) -> pillageForm(ctx, self, true)));
    }

    /** 劫掠形态的效果体（{@code upgraded} = 加的是不是强化版弩箭射击）。 */
    private static void pillageForm(CombatContext ctx, Combatant self, boolean upgraded) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.pillageForm = true;
        state.pillageFormUpgraded = upgraded;
        ctx.announce("  【劫掠形态】生效：每打出一张攻击牌 → 手牌 +1 张「"
                + (upgraded ? "弩箭射击+" : "弩箭射击") + "」");
    }

    /**
     * 商队护送：2 费（升级 1）罕见 技能 消耗 —— 你与队友各 15 点格挡；若这一轮没人被打穿格挡，
     * 战斗结束后额外 +20 金币。
     *
     * <p>这是一张**多人玩法向**的牌（进图鉴的「多人玩法向」分页，见 {@link #MULTIPLAYER_STYLE}），
     * 但按 v0.5.2.5 的消歧义，它**照常进奖励池与商店**（单人也能拿到，只是没人可护送时
     * "你与队友"就只剩你自己）。</p>
     */
    public static final Card CARAVAN_ESCORT = register(new Card(
            "caravan_escort", "商队护送", CardType.SKILL, 2, CardTarget.SELF,
            "你与队友各获得 15 点格挡。如果队友在这个回合内没有被破除格挡，战斗结束后额外获得 20 金币。消耗。",
            CardRarity.UNCOMMON, PILLAGER, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.grantEscortBlock(self, 15, "「商队护送」")));

    static {
        upgrade(new Card("caravan_escort", "商队护送+", CardType.SKILL, 1, CardTarget.SELF,
                "你与队友各获得 15 点格挡。如果队友在这个回合内没有被破除格挡，战斗结束后额外获得 20 金币。消耗。",
                CardRarity.UNCOMMON, PILLAGER, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.grantEscortBlock(self, 15, "「商队护送+」")));
    }

    // ==================================================================
    // v0.5.3.3 A4-12：回合钩子（金属化 / 燃烧 / 火焰屏障 / 无限刀刃 / 毒雾 / 布置）
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame（页面 id 见 docs/a4-card-plan.md §0.5 那张表）：
    // 金属化 3→4、燃烧 5→7、炎の障壁 12/4→16/6、無限の刃 升级是【天賦】、
    // 有毒ガス 2→3、セットアップ 1 费→**0 费**（效果不变）。

    /**
     * 金属化：1 费 罕见 能力 —— **回合结束时**获得 3 点格挡；升级 4 点。
     *
     * <p>校验来源：kamigame「金属化」（ターン終了時に3ブロックを得る／强化后 4）。</p>
     */
    public static final Card METALLICIZE = register(new Card(
            "metallicize", "金属化", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合结束时，获得 3 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> metallicize(ctx, self, 3)));

    static {
        upgrade(new Card("metallicize", "金属化+", CardType.POWER, 1, CardTarget.SELF,
                "在你的回合结束时，获得 4 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> metallicize(ctx, self, 4)));
    }

    /** 金属化的效果体（格挡两档）。 */
    private static void metallicize(CombatContext ctx, Combatant self, int block) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.metallicize += block;
        ctx.announce("  【金属化】生效：每回合结束获得 " + block + " 点格挡（共 " + state.metallicize() + "）");
    }

    /**
     * 自燃：1 费 罕见 能力 —— **回合结束时**失去 1 点生命，并对所有敌人造成 5 点伤害；升级 7 点。
     *
     * <p>校验来源：kamigame「燃焼」（ターン終了時にHPを1失い、敵全体に5ダメージを与える／强化后 7）。
     * 中文名取灰机 wiki 的官方译名【**自燃**】—— 因为【燃烧】这个名字已经被
     * {@link #INFLAME}（Inflame，+力量的同名卡）占了，两张卡不能同名。</p>
     */
    public static final Card COMBUST = register(new Card(
            "combust", "自燃", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合结束时，失去 1 点生命并对所有敌人造成 5 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> combust(ctx, self, 5)));

    static {
        upgrade(new Card("combust", "自燃+", CardType.POWER, 1, CardTarget.SELF,
                "在你的回合结束时，失去 1 点生命并对所有敌人造成 7 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> combust(ctx, self, 7)));
    }

    /** 自燃的效果体（伤害两档）。 */
    private static void combust(CombatContext ctx, Combatant self, int damage) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.combust += damage;
        ctx.announce("  【自燃】生效：每回合结束失去 1 点生命，并对所有敌人造成 " + damage
                + " 点伤害（共 " + state.combust() + "）");
    }

    /**
     * 火焰屏障：2 费 罕见 技能 —— 获得 12 点格挡；**本回合内**每当你被攻击，对攻击者造成 4 点伤害。
     * 升级 16 / 6。
     *
     * <p>校验来源：kamigame「炎の障壁」（12ブロックを得る。このターン攻撃を受けるたび、
     * 攻撃した敵に4ダメージを与える／强化后 16 / 6）。</p>
     *
     * <p>实现上直接复用**荆棘**：它本来就是"被攻击时反弹等量伤害"，而且是本模组唯一会反弹的机制。
     * 与【脏玻璃】那种永久荆棘的差别只在"回合结束收回"（{@code addTemporaryThorns}）。</p>
     */
    public static final Card FLAME_BARRIER = register(new Card(
            "flame_barrier", "火焰屏障", CardType.SKILL, 2, CardTarget.SELF,
            "获得 12 点格挡。本回合内每当你被攻击，对攻击者造成 4 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> flameBarrier(ctx, self, 12, 4)));

    static {
        upgrade(new Card("flame_barrier", "火焰屏障+", CardType.SKILL, 2, CardTarget.SELF,
                "获得 16 点格挡。本回合内每当你被攻击，对攻击者造成 6 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> flameBarrier(ctx, self, 16, 6)));
    }

    /** 火焰屏障的效果体（格挡 / 反弹两档）。 */
    private static void flameBarrier(CombatContext ctx, Combatant self, int block, int reflect) {
        ctx.grantPlayerBlock(block, "「火焰屏障」");
        self.addTemporaryThorns(reflect);
        ctx.announce("  【火焰屏障】：本回合内被攻击时反弹 " + reflect + " 点伤害（当前荆棘 "
                + self.thorns() + "，下回合开始时收回 " + reflect + "）");
    }

    /**
     * 无限刀刃：1 费 罕见 能力 —— **回合开始时**加一张『小刀』进手牌；升级后带【固有】。
     *
     * <p>校验来源：kamigame「無限の刃」（ターン開始時、ナイフを1枚手札に加える／
     * 强化版是「**天賦**。ターン開始時、ナイフを1枚手札に加える」—— 升级只加固有，效果与费用都不变）。</p>
     */
    public static final Card INFINITE_BLADES = register(new Card(
            "infinite_blades", "无限刀刃", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合开始时，将一张『小刀』加入你的手牌。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> infiniteBlades(ctx, self)));

    static {
        upgrade(new Card("infinite_blades", "无限刀刃+", CardType.POWER, 1, CardTarget.SELF,
                "固有。在你的回合开始时，将一张『小刀』加入你的手牌。", CardRarity.UNCOMMON, SILENT,
                Set.of(CardKeyword.INNATE), (ctx, self, target) -> infiniteBlades(ctx, self)));
    }

    /** 无限刀刃的效果体（基础版与强化版只差"固有"）。 */
    private static void infiniteBlades(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.infiniteBlades = true;
        ctx.announce("  【无限刀刃】生效：每回合开始加入 1 张『小刀』");
    }

    /**
     * 毒雾：1 费 罕见 能力 —— **回合开始时**对所有敌人施加 2 层中毒；升级 3 层。
     *
     * <p>校验来源：kamigame「有毒ガス」（ターン開始時、敵全体に毒2を与える／强化后 3）。</p>
     */
    public static final Card NOXIOUS_FUMES = register(new Card(
            "noxious_fumes", "毒雾", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合开始时，给予所有敌人 2 层中毒。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> noxiousFumes(ctx, self, 2)));

    static {
        upgrade(new Card("noxious_fumes", "毒雾+", CardType.POWER, 1, CardTarget.SELF,
                "在你的回合开始时，给予所有敌人 3 层中毒。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> noxiousFumes(ctx, self, 3)));
    }

    /** 毒雾的效果体（中毒两档）。 */
    private static void noxiousFumes(CombatContext ctx, Combatant self, int poison) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.noxiousFumes += poison;
        ctx.announce("  【毒雾】生效：每回合开始对所有敌人施加中毒 " + poison
                + "（共 " + state.noxiousFumes() + "）");
    }

    /**
     * 布置：1 费 罕见 技能 —— 将一张手牌置于抽牌堆顶部，**那张牌被打出之前费用为 0**；升级费用 0。
     *
     * <p>校验来源：kamigame「セットアップ」（手札のカード1枚を山札の1番上に置く。
     * プレイされるまでそのコストは0／强化后 コスト 1 → **0**，效果不变）。</p>
     *
     * <p>"费用为 0"的实现见 {@code PlayerState.freeToPlay} 与 {@code CombatContext.effectiveCost}：
     * 本引擎的卡牌是没有实例的共享定义，所以用「id → 还能免费几次」近似（见交接稿 §4.40 的说明）。</p>
     */
    public static final Card SETUP = register(new Card(
            "setup", "布置", CardType.SKILL, 1, CardTarget.SELF,
            "将一张手牌置于抽牌堆顶部。那张牌被打出之前费用为 0。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> setup(ctx, self)));

    static {
        upgrade(new Card("setup", "布置+", CardType.SKILL, 0, CardTarget.SELF,
                "将一张手牌置于抽牌堆顶部。那张牌被打出之前费用为 0。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> setup(ctx, self)));
    }

    /** 布置的效果体（基础版与强化版只差费用）。 */
    private static void setup(CombatContext ctx, Combatant self) {
        if (ctx.hand().isEmpty()) {
            ctx.announce("  布置：手牌是空的，没有牌可以布置");
            return;
        }
        ctx.beginCardPick(self, "「布置」", CombatContext.CardPickKind.SETUP,
                CombatContext.CardPickSource.HAND, 1, null);
    }

    // ==================================================================
    // v0.5.3.3 A4-13：出牌钩子 / 格挡钩子 / 双发 / 费用改写
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：堕落 3→2、ダブルタップ 1 张→2 张、ジャガーノート 5→7、
    // メッタ切り 1→2、猛毒の仕込み 2→**1 费**（毒量不变）、バースト 1 张→2 张。

    /**
     * 腐化：3 费 稀有 能力 —— **技能牌费用为 0**，且打出的技能牌**直接消耗**；升级 3→2 费。
     *
     * <p>校验来源：kamigame「堕落」（スキルがエナジーを消費しない。プレイしたスキルは廃棄する／
     * 强化后 コスト 3 → 2，效果不变）。中文名用官方译名【腐化】（该页日文名是「堕落」）。</p>
     *
     * <p>实现分两处：费用走 {@code CombatContext.effectiveCost}（界面显示与实际扣费同一个出口），
     * 消耗走 {@code playCardFor} 的落堆分支。</p>
     */
    public static final Card CORRUPTION = register(new Card(
            "corruption", "腐化", CardType.POWER, 3, CardTarget.SELF,
            "技能牌的费用变为 0。你打出的技能牌会被消耗。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> corruption(ctx, self)));

    static {
        upgrade(new Card("corruption", "腐化+", CardType.POWER, 2, CardTarget.SELF,
                "技能牌的费用变为 0。你打出的技能牌会被消耗。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> corruption(ctx, self)));
    }

    /** 腐化的效果体（基础版与强化版只差费用）。 */
    private static void corruption(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.corruption = true;
        ctx.announce("  【腐化】生效：技能牌费用为 0，且打出的技能牌直接消耗");
    }

    /**
     * 双重施法：1 费 稀有 技能 —— 本回合接下来的 **1** 张攻击牌打两遍；升级后是 **2** 张。
     *
     * <p>校验来源：kamigame「ダブルタップ」（このターン、次のアタックを2回プレイする／
     * 强化后「次の2枚のアタックを…」，费用仍是 1）。</p>
     */
    public static final Card DOUBLE_TAP = register(new Card(
            "double_tap", "双重施法", CardType.SKILL, 1, CardTarget.SELF,
            "在本回合内，你打出的下一张攻击牌会打出两次。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> doubleTap(ctx, self, 1)));

    static {
        upgrade(new Card("double_tap", "双重施法+", CardType.SKILL, 1, CardTarget.SELF,
                "在本回合内，你打出的下 2 张攻击牌会打出两次。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> doubleTap(ctx, self, 2)));
    }

    /** 双重施法的效果体（张数两档 1 / 2）。 */
    private static void doubleTap(CombatContext ctx, Combatant self, int count) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.doubleTap += count;
        ctx.announce("  【双重施法】：本回合接下来 " + state.doubleTap() + " 张攻击牌会打出两次");
    }

    /**
     * 主宰：2 费 稀有 能力 —— **每次获得格挡**时对随机敌人造成 5 点伤害；升级 7 点。
     *
     * <p>校验来源：kamigame「ジャガーノート」（ブロックを得るたび、ランダムな敵に5ダメージ／
     * 强化后 7）。</p>
     */
    public static final Card JUGGERNAUT = register(new Card(
            "juggernaut", "主宰", CardType.POWER, 2, CardTarget.SELF,
            "每当你获得格挡时，对随机敌人造成 5 点伤害。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> juggernaut(ctx, self, 5)));

    static {
        upgrade(new Card("juggernaut", "主宰+", CardType.POWER, 2, CardTarget.SELF,
                "每当你获得格挡时，对随机敌人造成 7 点伤害。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> juggernaut(ctx, self, 7)));
    }

    /** 主宰的效果体（伤害两档）。 */
    private static void juggernaut(CombatContext ctx, Combatant self, int damage) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.juggernaut += damage;
        ctx.announce("  【主宰】生效：每次获得格挡 → 随机敌人受 " + damage + " 点伤害（共 "
                + state.juggernaut() + "）");
    }

    /**
     * 千刀万剐：2 费 稀有 能力 —— **每打出一张牌**（含它自己）对所有敌人造成 1 点伤害；升级 2 点。
     *
     * <p>校验来源：kamigame「メッタ切り」（カードをプレイするたび、敵全体に1ダメージを与える／
     * 强化后 2）。</p>
     */
    public static final Card A_THOUSAND_CUTS = register(new Card(
            "a_thousand_cuts", "千刀万剐", CardType.POWER, 2, CardTarget.SELF,
            "每当你打出一张牌，对所有敌人造成 1 点伤害。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> aThousandCuts(ctx, self, 1)));

    static {
        upgrade(new Card("a_thousand_cuts", "千刀万剐+", CardType.POWER, 2, CardTarget.SELF,
                "每当你打出一张牌，对所有敌人造成 2 点伤害。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> aThousandCuts(ctx, self, 2)));
    }

    /** 千刀万剐的效果体（伤害两档）。 */
    private static void aThousandCuts(CombatContext ctx, Combatant self, int damage) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.thousandCuts += damage;
        ctx.announce("  【千刀万剐】生效：每打出一张牌 → 全体敌人受 " + damage + " 点伤害（共 "
                + state.thousandCuts() + "）");
    }

    /**
     * 涂毒：2 费（升级 **1** 费）稀有 能力 —— 攻击牌**造成伤害**时给那个敌人 1 层中毒。
     *
     * <p>校验来源：kamigame「猛毒の仕込み」（アタックでダメージを与えるたび、毒1を与える／
     * 强化后 コスト 2 → **1**，毒量不变）。</p>
     */
    public static final Card ENVENOM = register(new Card(
            "envenom", "涂毒", CardType.POWER, 2, CardTarget.SELF,
            "每当你用攻击牌造成伤害时，给予那个敌人 1 层中毒。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> envenom(ctx, self)));

    static {
        upgrade(new Card("envenom", "涂毒+", CardType.POWER, 1, CardTarget.SELF,
                "每当你用攻击牌造成伤害时，给予那个敌人 1 层中毒。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> envenom(ctx, self)));
    }

    /** 涂毒的效果体（基础版与强化版只差费用）。 */
    private static void envenom(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.envenom += 1;
        ctx.announce("  【涂毒】生效：攻击牌造成伤害时 → 该敌人中毒 +1");
    }

    /**
     * 爆发：1 费 稀有 技能 —— 本回合接下来的 **1** 张技能牌打两遍；升级后是 **2** 张。
     *
     * <p>校验来源：kamigame「バースト」（このターン、次のスキルを2回プレイする／
     * 强化后「次の2枚のスキルを…」，费用仍是 1）。</p>
     */
    public static final Card BURST = register(new Card(
            "burst", "爆发", CardType.SKILL, 1, CardTarget.SELF,
            "在本回合内，你打出的下一张技能牌会打出两次。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> burst(ctx, self, 1)));

    static {
        upgrade(new Card("burst", "爆发+", CardType.SKILL, 1, CardTarget.SELF,
                "在本回合内，你打出的下 2 张技能牌会打出两次。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> burst(ctx, self, 2)));
    }

    /** 爆发的效果体（张数两档 1 / 2）。 */
    private static void burst(CombatContext ctx, Combatant self, int count) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.burstSkills += count;
        ctx.announce("  【爆发】：本回合接下来 " + state.burstSkills() + " 张技能牌会打出两次");
    }

    // ==================================================================
    // v0.5.3.5 A4-14：抽牌钩子 / 消耗钩子 / 下回合能量与抽牌
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：進化 抽 1→2、炎の吐息 6→10、無痛 3→4、
    // 飛び膝蹴り 8→11（能量 1 不变）、先読み 下回合能量 2→3、略奪 15→20（抽 2 不变）。

    /**
     * 进化：1 费 罕见 能力 —— **每抽到一张状态牌**，再抽 1 张；升级 2 张。
     *
     * <p>校验来源：kamigame「進化」（状態異常を引くたび、カードを1枚引く／强化后 2 枚）。</p>
     */
    public static final Card EVOLVE = register(new Card(
            "evolve", "进化", CardType.POWER, 1, CardTarget.SELF,
            "每当你抽到一张状态牌，就再抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> evolve(ctx, self, 1)));

    static {
        upgrade(new Card("evolve", "进化+", CardType.POWER, 1, CardTarget.SELF,
                "每当你抽到一张状态牌，就再抽 2 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> evolve(ctx, self, 2)));
    }

    /** 进化的效果体（抽牌两档）。 */
    private static void evolve(CombatContext ctx, Combatant self, int draw) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.evolve += draw;
        ctx.announce("  【进化】生效：每抽到一张状态牌 → 再抽 " + draw + " 张（共 " + state.evolve() + "）");
    }

    /**
     * 火焰吐息：1 费 罕见 能力 —— **每抽到一张状态牌或诅咒牌**，对所有敌人造成 6 点伤害；升级 10 点。
     *
     * <p>校验来源：kamigame「炎の吐息」（状態異常か呪いを引くたび、敵全体に6ダメージ／强化后 10）。</p>
     */
    public static final Card FIRE_BREATHING = register(new Card(
            "fire_breathing", "火焰吐息", CardType.POWER, 1, CardTarget.SELF,
            "每当你抽到一张状态牌或诅咒牌，对所有敌人造成 6 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> fireBreathing(ctx, self, 6)));

    static {
        upgrade(new Card("fire_breathing", "火焰吐息+", CardType.POWER, 1, CardTarget.SELF,
                "每当你抽到一张状态牌或诅咒牌，对所有敌人造成 10 点伤害。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> fireBreathing(ctx, self, 10)));
    }

    /** 火焰吐息的效果体（伤害两档）。 */
    private static void fireBreathing(CombatContext ctx, Combatant self, int damage) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.fireBreathing += damage;
        ctx.announce("  【火焰吐息】生效：每抽到一张状态 / 诅咒牌 → 全体敌人受 " + damage
                + " 点伤害（共 " + state.fireBreathing() + "）");
    }

    /**
     * 无痛：1 费 罕见 能力 —— **每消耗一张牌**，获得 3 点格挡；升级 4 点。
     *
     * <p>校验来源：kamigame「無痛」（カードを廃棄するたび、3ブロックを得る／强化后 4）。</p>
     */
    public static final Card FEEL_NO_PAIN = register(new Card(
            "feel_no_pain", "无痛", CardType.POWER, 1, CardTarget.SELF,
            "每当你消耗一张牌，获得 3 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> feelNoPain(ctx, self, 3)));

    static {
        upgrade(new Card("feel_no_pain", "无痛+", CardType.POWER, 1, CardTarget.SELF,
                "每当你消耗一张牌，获得 4 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> feelNoPain(ctx, self, 4)));
    }

    /** 无痛的效果体（格挡两档）。 */
    private static void feelNoPain(CombatContext ctx, Combatant self, int block) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.feelNoPain += block;
        ctx.announce("  【无痛】生效：每消耗一张牌 → 获得 " + block + " 点格挡（共 " + state.feelNoPain() + "）");
    }

    /**
     * 飞膝：1 费 常见 攻击 —— 造成 8 点伤害，并在**下个回合**获得 1 点能量；升级 11 点伤害。
     *
     * <p>校验来源：kamigame「飛び膝蹴り」（8ダメージを与える。次のターン、エナジー1を得る／强化后 11，
     * 能量仍 1）。</p>
     */
    public static final Card FLYING_KNEE = register(new Card(
            "flying_knee", "飞膝", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 8 点伤害。在下个回合获得 1 点能量。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> flyingKnee(ctx, self, target, 8, 1)));

    static {
        upgrade(new Card("flying_knee", "飞膝+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 11 点伤害。在下个回合获得 1 点能量。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> flyingKnee(ctx, self, target, 11, 1)));
    }

    /** 飞膝的效果体（伤害两档，能量固定 1）。 */
    private static void flyingKnee(CombatContext ctx, Combatant self, Combatant target, int damage,
                                   int energy) {
        ctx.dealDamage(self, target, damage, "「飞膝」");
        self.addNextTurnEnergy(energy);
        ctx.announce("  下个回合额外获得 " + energy + " 点能量（当前挂着 " + self.nextTurnEnergy() + "）");
    }

    /**
     * 机动：1 费 常见 技能 —— 在**下个回合**获得 2 点能量；升级 3 点。
     *
     * <p>校验来源：kamigame「先読み」（次のターン、エナジー2を得る／强化后 3）。</p>
     */
    public static final Card OUTMANEUVER = register(new Card(
            "outmaneuver", "机动", CardType.SKILL, 1, CardTarget.SELF,
            "在下个回合获得 2 点能量。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> outmaneuver(ctx, self, 2)));

    static {
        upgrade(new Card("outmaneuver", "机动+", CardType.SKILL, 1, CardTarget.SELF,
                "在下个回合获得 3 点能量。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> outmaneuver(ctx, self, 3)));
    }

    /** 机动的效果体（能量两档）。 */
    private static void outmaneuver(CombatContext ctx, Combatant self, int energy) {
        self.addNextTurnEnergy(energy);
        ctx.announce("  【机动】：下个回合额外获得 " + energy + " 点能量（当前挂着 "
                + self.nextTurnEnergy() + "）");
    }

    /**
     * 捕食者：2 费 罕见 攻击 —— 造成 15 点伤害，并在**下个回合开始时**抽 2 张牌；升级 20 点伤害。
     *
     * <p>校验来源：kamigame「略奪」（15ダメージを与える。次のターン開始時、カードを2枚引く／
     * 强化后 20，抽 2 张不变）。</p>
     */
    public static final Card PREDATOR = register(new Card(
            "predator", "捕食者", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 15 点伤害。在下个回合开始时抽 2 张牌。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> predator(ctx, self, target, 15, 2)));

    static {
        upgrade(new Card("predator", "捕食者+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 20 点伤害。在下个回合开始时抽 2 张牌。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> predator(ctx, self, target, 20, 2)));
    }

    /** 捕食者的效果体（伤害两档，抽牌固定 2）。 */
    private static void predator(CombatContext ctx, Combatant self, Combatant target, int damage, int draw) {
        ctx.dealDamage(self, target, damage, "「捕食者」");
        self.addNextTurnDraw(draw);
        ctx.announce("  下个回合开始额外抽 " + draw + " 张（当前挂着 " + self.nextTurnDraw() + "）");
    }

    // ==================================================================
    // v0.5.3.6 A4-15：消耗钩子扩展 / 本回合攻击计数 / 自消耗效果
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：闇の抱擁 2→1 费、激怒 3→5 格挡、見張り 5/2→8/3、
    // まきびし 3→5、ブラー 5→8 格挡、とどめの一撃 ×6→×8。

    /**
     * **"这张牌自己被消耗时"的效果表**（v0.5.3.6 A4-15【哨兵】）。
     *
     * <p>本引擎的 {@code Card} 是不可变共享定义、不能自己带回调之外的字段，
     * 所以用**按 id 查表**的方式 —— 与 {@code StatusCards}（按 id 判"不可打出 / 回合末自伤"）
     * 是同一个套路。挂载点在 {@code CombatContext.exhaustCard}（消耗的唯一入口）。</p>
     */
    public interface SelfExhaustEffect {
        /**
         * @param owner 这张牌的拥有者（不是"当前行动玩家"，避免多人时搞错人）
         * @param card  真正被消耗的那一张（能看出是不是强化版）
         */
        void apply(CombatContext ctx, PlayerState owner, Card card);
    }

    private static final Map<String, SelfExhaustEffect> SELF_EXHAUST = new LinkedHashMap<>();

    /** 登记"这张牌自己被消耗时"的效果（同一 id 只登记一次）。 */
    public static void registerSelfExhaust(String cardId, SelfExhaustEffect effect) {
        if (cardId != null && effect != null) {
            SELF_EXHAUST.put(cardId, effect);
        }
    }

    /** 查这张牌被消耗时有没有额外效果（没有返回 {@code null}）。 */
    public static SelfExhaustEffect selfExhaust(String cardId) {
        return cardId == null ? null : SELF_EXHAUST.get(cardId);
    }

    /** 登记了几张"自消耗有效果"的牌（自检用）。 */
    public static int selfExhaustCount() {
        return SELF_EXHAUST.size();
    }

    /**
     * **"这张牌被弃置时"的效果表**（v0.5.3.7 A4-16【反射】【战术家】）。
     *
     * <p>与 {@link SelfExhaustEffect} 完全对称：挂载点在 {@code CombatContext.discardFromHand}
     * —— 那是"出牌环节里弃牌"的唯一入口，而**回合结束弃手牌走的是 {@code discardHand}**，
     * 所以正好对应卡面上那句"在你的回合内被弃置时"。</p>
     */
    public interface SelfDiscardEffect {
        /**
         * @param owner 这张牌的拥有者
         * @param card  真正被弃掉的那一张（能看出是不是强化版）
         */
        void apply(CombatContext ctx, PlayerState owner, Card card);
    }

    private static final Map<String, SelfDiscardEffect> SELF_DISCARD = new LinkedHashMap<>();

    /** 登记"这张牌被弃置时"的效果。 */
    public static void registerSelfDiscard(String cardId, SelfDiscardEffect effect) {
        if (cardId != null && effect != null) {
            SELF_DISCARD.put(cardId, effect);
        }
    }

    /** 查这张牌被弃置时有没有额外效果（没有返回 {@code null}）。 */
    public static SelfDiscardEffect selfDiscard(String cardId) {
        return cardId == null ? null : SELF_DISCARD.get(cardId);
    }

    /** 登记了几张"被弃置有效果"的牌（自检用）。 */
    public static int selfDiscardCount() {
        return SELF_DISCARD.size();
    }

    // ================== v0.5.5.0（A6-1）数据包注册表 stc:card 的覆盖层 ==================

    /**
     * **一张来自数据包（{@code stc:card} 注册表）的卡**。
     *
     * <p>{@code base} 必需；其余字段 {@code null} 表示"继承内置定义"（新卡则按"没有"处理）：</p>
     * <ul>
     *   <li>{@code upgraded}：强化版定义（{@code null} = 这张卡没有强化版）；</li>
     *   <li>{@code xCost} / {@code generated} / {@code multiplayerStyle}：三个 id 标记集合；</li>
     *   <li>{@code maxUpgrades}：最多强化几次（{@code null} = 有强化版就 1 次）；</li>
     *   <li>{@code onExhaust} / {@code onDiscard}：{"自己被消耗 / 被弃置时"} 的额外效果。</li>
     * </ul>
     *
     * <p>⚠️ 本层是**覆盖**而不是替换：{@link #applyExternal} 先把卡池还原成代码内置状态，
     * 再把数据包的定义盖上去 —— 所以没被数据包碰过的卡一定还是内置那份（不会因为某次加载失败而
     * 让卡池残缺）。</p>
     */
    public record ExternalCard(Card base, Card upgraded, Boolean xCost, Boolean generated,
                               Boolean multiplayerStyle, Integer maxUpgrades,
                               SelfExhaustEffect onExhaust, SelfDiscardEffect onDiscard) {

        /** 只有基础定义（大多数数据包卡的写法）。 */
        public static ExternalCard of(Card base) {
            return new ExternalCard(base, null, null, null, null, null, null, null);
        }

        /** 基础 + 强化两档。 */
        public static ExternalCard of(Card base, Card upgraded) {
            return new ExternalCard(base, upgraded, null, null, null, null, null, null);
        }
    }

    /** 代码内置状态的快照（第一次应用数据包时拍下，{@link #resetExternal} 用它还原）。 */
    private record BuiltinSnapshot(Map<String, Card> byId, Map<String, Card> upgraded,
                                   Set<String> xCost, Set<String> generated, Set<String> multiplayer,
                                   Map<String, Integer> maxUpgrades,
                                   Map<String, SelfExhaustEffect> selfExhaust,
                                   Map<String, SelfDiscardEffect> selfDiscard) {
    }

    private static BuiltinSnapshot builtinSnapshot;
    private static final Set<String> EXTERNAL_IDS = new java.util.LinkedHashSet<>();

    /**
     * 用数据包定义**覆盖 / 新增**卡牌，然后让整个卡池生效。
     *
     * <p>调用方是 MC 层（{@code CardRegistryLoader}）：引擎自己不认识注册表，只负责"把外面送进来的
     * 定义接进自己的表里"。这样 {@code byId} / {@code resolve} / 奖励池 / 图鉴 / 组牌**全都不用改** ——
     * 它们本来就是查这几张表。</p>
     */
    public static synchronized void applyExternal(List<ExternalCard> cards) {
        if (builtinSnapshot == null) {
            builtinSnapshot = snapshot();
        }
        restore(builtinSnapshot);
        EXTERNAL_IDS.clear();
        if (cards == null || cards.isEmpty()) {
            return;
        }
        for (ExternalCard external : cards) {
            if (external == null || external.base() == null) {
                continue;
            }
            String id = external.base().id();
            BY_ID.put(id, external.base());
            if (external.upgraded() != null) {
                UPGRADED.put(id, external.upgraded());
            } else {
                // 数据包没给强化版 = 这张卡没有强化版（覆盖内置时就是"把强化版拿掉"）
                UPGRADED.remove(id);
            }
            if (external.xCost() != null) {
                setFlag(X_COST, id, external.xCost());
            }
            if (external.generated() != null) {
                setFlag(GENERATED, id, external.generated());
            }
            if (external.multiplayerStyle() != null) {
                setFlag(MULTIPLAYER_STYLE, id, external.multiplayerStyle());
            }
            if (external.maxUpgrades() != null) {
                MAX_UPGRADES.put(id, external.maxUpgrades());
            }
            if (external.onExhaust() != null) {
                SELF_EXHAUST.put(id, external.onExhaust());
            }
            if (external.onDiscard() != null) {
                SELF_DISCARD.put(id, external.onDiscard());
            }
            EXTERNAL_IDS.add(id);
        }
    }

    /** 撤掉所有数据包定义，回到**代码内置**的卡池（`/reload`、退出世界、单机换存档时用）。 */
    public static synchronized void resetExternal() {
        if (builtinSnapshot == null) {
            return;
        }
        restore(builtinSnapshot);
        EXTERNAL_IDS.clear();
    }

    /** 现在有几张卡来自数据包（指令与自检用）。 */
    public static int externalCount() {
        return EXTERNAL_IDS.size();
    }

    /** 现在有哪些卡来自数据包（按加入顺序）。 */
    public static List<String> externalIds() {
        return List.copyOf(EXTERNAL_IDS);
    }

    /** 这张卡现在是数据包提供的吗。 */
    public static boolean isExternal(String cardId) {
        return cardId != null && EXTERNAL_IDS.contains(cardId);
    }

    private static void setFlag(Set<String> flags, String id, boolean on) {
        if (on) {
            flags.add(id);
        } else {
            flags.remove(id);
        }
    }

    private static BuiltinSnapshot snapshot() {
        return new BuiltinSnapshot(new LinkedHashMap<>(BY_ID), new LinkedHashMap<>(UPGRADED),
                new java.util.LinkedHashSet<>(X_COST), new java.util.LinkedHashSet<>(GENERATED),
                new java.util.LinkedHashSet<>(MULTIPLAYER_STYLE), new LinkedHashMap<>(MAX_UPGRADES),
                new LinkedHashMap<>(SELF_EXHAUST), new LinkedHashMap<>(SELF_DISCARD));
    }

    private static void restore(BuiltinSnapshot snapshot) {
        BY_ID.clear();
        BY_ID.putAll(snapshot.byId());
        UPGRADED.clear();
        UPGRADED.putAll(snapshot.upgraded());
        X_COST.clear();
        X_COST.addAll(snapshot.xCost());
        GENERATED.clear();
        GENERATED.addAll(snapshot.generated());
        MULTIPLAYER_STYLE.clear();
        MULTIPLAYER_STYLE.addAll(snapshot.multiplayer());
        MAX_UPGRADES.clear();
        MAX_UPGRADES.putAll(snapshot.maxUpgrades());
        SELF_EXHAUST.clear();
        SELF_EXHAUST.putAll(snapshot.selfExhaust());
        SELF_DISCARD.clear();
        SELF_DISCARD.putAll(snapshot.selfDiscard());
    }

    /**
     * 黑暗拥抱：2 费（升级 **1** 费）罕见 能力 —— **每消耗一张牌**就抽 1 张。
     *
     * <p>校验来源：kamigame「闇の抱擁」（カードを廃棄するたび、カードを1枚引く／
     * 强化后 コスト 2 → 1，效果不变）。</p>
     */
    public static final Card DARK_EMBRACE = register(new Card(
            "dark_embrace", "黑暗拥抱", CardType.POWER, 2, CardTarget.SELF,
            "每当你消耗一张牌，就抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> darkEmbrace(ctx, self, 1)));

    static {
        upgrade(new Card("dark_embrace", "黑暗拥抱+", CardType.POWER, 1, CardTarget.SELF,
                "每当你消耗一张牌，就抽 1 张牌。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> darkEmbrace(ctx, self, 1)));
    }

    /** 黑暗拥抱的效果体（基础版与强化版只差费用）。 */
    private static void darkEmbrace(CombatContext ctx, Combatant self, int draw) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.darkEmbrace += draw;
        ctx.announce("  【黑暗拥抱】生效：每消耗一张牌 → 抽 " + draw + " 张（共 " + state.darkEmbrace() + "）");
    }

    /**
     * 狂怒：0 费 罕见 技能 —— **本回合内**每打出一张攻击牌，获得 3 点格挡；升级 5 点。
     *
     * <p>校验来源：kamigame「激怒」（このターン、アタックをプレイするたび3ブロックを得る／
     * 强化后 5）。</p>
     */
    public static final Card RAGE = register(new Card(
            "rage", "狂怒", CardType.SKILL, 0, CardTarget.SELF,
            "在本回合内，每当你打出一张攻击牌，获得 3 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> rage(ctx, self, 3)));

    static {
        upgrade(new Card("rage", "狂怒+", CardType.SKILL, 0, CardTarget.SELF,
                "在本回合内，每当你打出一张攻击牌，获得 5 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> rage(ctx, self, 5)));
    }

    /** 狂怒的效果体（格挡两档）。 */
    private static void rage(CombatContext ctx, Combatant self, int block) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.rage += block;
        ctx.announce("  【狂怒】：本回合每打出一张攻击牌 → 获得 " + block + " 点格挡（共 "
                + state.rage() + "）");
    }

    /**
     * 哨兵：1 费 罕见 技能 —— 获得 5 点格挡；**如果这张牌被消耗**，获得 2 点能量。
     * 升级 8 格挡 / 3 能量。
     *
     * <p>校验来源：kamigame「見張り」（5ブロックを得る。このカードを廃棄したとき、エナジー2を得る／
     * 强化后 8 ブロック / 3 エナジー）。</p>
     *
     * <p>"这张牌自己被消耗"用 {@link #registerSelfExhaust} 按 id 登记 —— 因为
     * {@code Card} 是不可变共享定义，没有"这张牌自己"这个概念。</p>
     */
    public static final Card SENTINEL = register(new Card(
            "sentinel", "哨兵", CardType.SKILL, 1, CardTarget.SELF,
            "获得 5 点格挡。如果这张牌被消耗，获得 2 点能量。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> sentinel(ctx, self, 5)));

    static {
        upgrade(new Card("sentinel", "哨兵+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 8 点格挡。如果这张牌被消耗，获得 3 点能量。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> sentinel(ctx, self, 8)));
    }

    /** 哨兵的效果体（格挡两档；被消耗时的能量另外登记）。 */
    private static void sentinel(CombatContext ctx, Combatant self, int block) {
        ctx.grantPlayerBlock(block, "「哨兵」");
    }

    static {
        // 哨兵被消耗时回能量（基础 2 / 强化 3）—— 由 exhaustCard 查这张表执行
        registerSelfExhaust("sentinel", (ctx, owner, card) -> {
            int energy = card != null && card.name().endsWith("+") ? 3 : 2;
            int gained = ctx.gainEnergy(owner.combatant(), energy);
            ctx.announce("  【哨兵】被消耗 → 获得 " + gained + " 点能量");
        });
    }

    /**
     * 铁蒺藜：1 费 罕见 能力 —— 每当你被攻击，对攻击者造成 3 点伤害；升级 5 点。
     *
     * <p>校验来源：kamigame「まきびし」（攻撃を受けるたび、攻撃を行った敵に3ダメージ／强化后 5）。</p>
     *
     * <p>实现就是**荆棘**（本模组唯一会反弹的机制）：3 / 5 层永久荆棘。</p>
     */
    public static final Card CALTROPS = register(new Card(
            "caltrops", "铁蒺藜", CardType.POWER, 1, CardTarget.SELF,
            "每当你被攻击时，对攻击者造成 3 点伤害。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> caltrops(ctx, self, 3)));

    static {
        upgrade(new Card("caltrops", "铁蒺藜+", CardType.POWER, 1, CardTarget.SELF,
                "每当你被攻击时，对攻击者造成 5 点伤害。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> caltrops(ctx, self, 5)));
    }

    /** 铁蒺藜的效果体（荆棘层数两档）。 */
    private static void caltrops(CombatContext ctx, Combatant self, int thorns) {
        self.addThorns(thorns);
        ctx.announce("  【铁蒺藜】：被攻击时反弹 " + thorns + " 点伤害（当前荆棘 " + self.thorns() + "）");
    }

    /**
     * 模糊：1 费 罕见 技能 —— 获得 5 点格挡；**下个回合开始时不清空格挡**；升级 8 点格挡。
     *
     * <p>校验来源：kamigame「ブラー」（5ブロックを得る。次のターン開始時にブロックを失わない／
     * 强化后 8 ブロック，其余不变）。</p>
     */
    public static final Card BLUR = register(new Card(
            "blur", "模糊", CardType.SKILL, 1, CardTarget.SELF,
            "获得 5 点格挡。下个回合开始时，你不会失去格挡。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> blur(ctx, self, 5)));

    static {
        upgrade(new Card("blur", "模糊+", CardType.SKILL, 1, CardTarget.SELF,
                "获得 8 点格挡。下个回合开始时，你不会失去格挡。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> blur(ctx, self, 8)));
    }

    /** 模糊的效果体（格挡两档）。 */
    private static void blur(CombatContext ctx, Combatant self, int block) {
        ctx.grantPlayerBlock(block, "「模糊」");
        self.addBlurTurns(1);
        ctx.announce("  【模糊】：下个回合开始时不清空格挡（已挂 " + self.blurTurns() + " 次）");
    }

    /**
     * 终结技：1 费 罕见 攻击 —— 对**一个敌人**造成"本回合已打出的攻击牌张数 × 6"点伤害；
     * 升级 ×8。
     *
     * <p>校验来源：kamigame「とどめの一撃」（このターンプレイしたアタックの枚数×6ダメージ／
     * 强化后 ×8）。⚠️ 该页**没有标注目标**，2025 用户 v0.5.3.7 明确"**生效对象是单体**"
     * （原文："终结技的生效对象：斩杀单体时"），所以这里是 {@code CardTarget.ENEMY}。</p>
     *
     * <p>本模组里"打出的攻击牌张数"由 {@code PlayerState.attacksPlayedThisTurn} 记账，
     * **包含这张牌自己**（它也是本回合打出的一张攻击牌），双发时算两张。</p>
     */
    public static final Card FINISHER = register(new Card(
            "finisher", "终结技", CardType.ATTACK, 1, CardTarget.ENEMY,
            "对这名敌人造成「本回合你已经打出的攻击牌张数 × 6」点伤害。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> finisher(ctx, self, target, 6)));

    static {
        upgrade(new Card("finisher", "终结技+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "对这名敌人造成「本回合你已经打出的攻击牌张数 × 8」点伤害。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> finisher(ctx, self, target, 8)));
    }

    /** 终结技的效果体（单体；每张攻击牌的伤害两档）。 */
    private static void finisher(CombatContext ctx, Combatant self, Combatant target, int perAttack) {
        int count = ctx.attacksPlayedThisTurn(self);
        if (count <= 0) {
            ctx.announce("  终结技：本回合还没打出过攻击牌，没有伤害");
            return;
        }
        if (!(target instanceof Enemy enemy) || !enemy.alive()) {
            ctx.announce("  终结技：没有可以斩杀的目标");
            return;
        }
        ctx.announce("  终结技：本回合已打出 " + count + " 张攻击牌 → 对 " + enemy.name() + " 造成 "
                + count + " × " + perAttack + " = " + (count * perAttack) + " 点伤害");
        for (int i = 0; i < count; i++) {
            if (!enemy.alive()) {
                break;
            }
            ctx.dealDamage(self, enemy, perAttack, "「终结技」");
        }
    }

    // ==================================================================
    // v0.5.3.7 A4-16：弃牌钩子 / 随机攻击牌 + 本回合 0 费 / 回合开始自伤
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame（反射+ / 战术家+ 的升级行 kamigame 没有，
    // 用 slaythespire.wiki.gg 核对，见交接稿）：
    // やせ我慢 15→20 格挡、残虐 加【天賦】、非道の刃 1→0 费、鋼の嵐 ナイフ→ナイフ＋、
    // 反射 抽 2→3、策士 能量 1→2。

    /**
     * 强力冲锋：1 费 罕见 技能 —— 将 2 张『伤口』加入手牌，获得 15 点格挡；升级 20 点。
     *
     * <p>校验来源：kamigame「やせ我慢」（負傷を2枚手札に加える。15ブロックを得る／强化后 20）。</p>
     */
    public static final Card POWER_THROUGH = register(new Card(
            "power_through", "强力冲锋", CardType.SKILL, 1, CardTarget.SELF,
            "将 2 张『伤口』加入你的手牌。获得 15 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> powerThrough(ctx, self, 15)));

    static {
        upgrade(new Card("power_through", "强力冲锋+", CardType.SKILL, 1, CardTarget.SELF,
                "将 2 张『伤口』加入你的手牌。获得 20 点格挡。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> powerThrough(ctx, self, 20)));
    }

    /** 强力冲锋的效果体（格挡两档，伤口固定 2 张）。 */
    private static void powerThrough(CombatContext ctx, Combatant self, int block) {
        int added = ctx.addCardsToHand(self, WOUND, 2);
        ctx.announce("  强力冲锋：加入 " + added + " 张『伤口』");
        ctx.grantPlayerBlock(block, "「强力冲锋」");
    }

    /**
     * 残忍：0 费 稀有 能力 —— **回合开始时**失去 1 点生命并抽 1 张牌；升级加【固有】。
     *
     * <p>校验来源：kamigame「残虐」（ターン開始時、HPを1失いカードを1枚引く／强化后 天賦，
     * 费用与数值都不变）。</p>
     */
    public static final Card BRUTALITY = register(new Card(
            "brutality", "残忍", CardType.POWER, 0, CardTarget.SELF,
            "在你的回合开始时，失去 1 点生命并抽 1 张牌。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> brutality(ctx, self)));

    static {
        upgrade(new Card("brutality", "残忍+", CardType.POWER, 0, CardTarget.SELF,
                "固有。在你的回合开始时，失去 1 点生命并抽 1 张牌。", CardRarity.RARE, IRONCLAD,
                Set.of(CardKeyword.INNATE), (ctx, self, target) -> brutality(ctx, self)));
    }

    /** 残忍的效果体（基础版与强化版只差"固有"）。 */
    private static void brutality(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.brutality = 1;
        ctx.announce("  【残忍】生效：每回合开始失去 1 点生命并抽 1 张");
    }

    /**
     * 地狱之刃：1 费（升级 **0** 费）罕见 技能 消耗 —— 将一张**随机攻击牌**加入手牌，
     * 那张牌**本回合费用为 0**。
     *
     * <p>校验来源：kamigame「非道の刃」（ランダムなアタックを1枚手札に加える。このターン、
     * そのコストは0。廃棄／强化后 コスト 1 → 0，效果不变）。</p>
     *
     * <p>"随机攻击牌"取**铁甲战士**的池子（这张牌本身就是战士专属，所以与"当前角色"等价）；
     * "本回合 0 费"走 {@code PlayerState.freeThisTurn}（回合结束作废，与【布置】的长期免费分开）。</p>
     */
    public static final Card INFERNAL_BLADE = register(new Card(
            "infernal_blade", "地狱之刃", CardType.SKILL, 1, CardTarget.SELF,
            "将一张随机攻击牌加入你的手牌，这张牌在本回合的费用为 0。消耗。",
            CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> infernalBlade(ctx, self)));

    static {
        upgrade(new Card("infernal_blade", "地狱之刃+", CardType.SKILL, 0, CardTarget.SELF,
                "将一张随机攻击牌加入你的手牌，这张牌在本回合的费用为 0。消耗。",
                CardRarity.UNCOMMON, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> infernalBlade(ctx, self)));
    }

    /** 地狱之刃的效果体（基础版与强化版只差费用）。 */
    private static void infernalBlade(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        Card gift = ctx.randomAttackCard(IRONCLAD);
        if (gift == null) {
            ctx.announce("  地狱之刃：抽不到攻击牌");
            return;
        }
        int added = ctx.addCardsToHand(self, gift, 1);
        state.markFreeThisTurn(gift.id());
        ctx.announce("  地狱之刃：加入 " + added + " 张「" + gift.name() + "」，它在本回合的费用为 0");
    }

    /**
     * 钢铁风暴：1 费 稀有 技能 —— 弃掉**全部手牌**，每弃 1 张就把 1 张『小刀』加入手牌；
     * 升级后加入的是**『小刀+』**。
     *
     * <p>校验来源：kamigame「鋼の嵐」（手札をすべて捨てる。捨てたカード1枚につきナイフを1枚
     * 手札に加える／强化后 **ナイフ＋**，费用与弃牌规则不变）。
     * ⚠️ 升级**不是**降费 —— 这点与"稀有牌升级常降费"的直觉相反，是卡页纠正的。</p>
     */
    public static final Card STORM_OF_STEEL = register(new Card(
            "storm_of_steel", "钢铁风暴", CardType.SKILL, 1, CardTarget.SELF,
            "弃掉你的所有手牌。每弃掉一张牌，就将一张『小刀』加入你的手牌。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> stormOfSteel(ctx, self, false)));

    static {
        upgrade(new Card("storm_of_steel", "钢铁风暴+", CardType.SKILL, 1, CardTarget.SELF,
                "弃掉你的所有手牌。每弃掉一张牌，就将一张『小刀+』加入你的手牌。",
                CardRarity.RARE, SILENT,
                (ctx, self, target) -> stormOfSteel(ctx, self, true)));
    }

    /** 钢铁风暴的效果体（{@code upgraded} = 加的是不是强化版小刀）。 */
    private static void stormOfSteel(CombatContext ctx, Combatant self, boolean upgraded) {
        int count = ctx.hand().size();
        for (int i = count - 1; i >= 0; i--) {
            ctx.discardFromHand(self, i);
        }
        Card shiv = upgraded ? CardLibrary.resolve("shiv", 1) : SHIV;
        int added = 0;
        for (int i = 0; i < count; i++) {
            added += ctx.addCardsToHand(self, shiv, 1);
        }
        ctx.announce("  钢铁风暴：弃掉 " + count + " 张手牌 → 加入 " + added + " 张「" + shiv.name() + "」");
    }

    /**
     * 反射：**使用不可** 罕见 技能 —— 当这张牌**被弃置**时，抽 2 张牌；升级 3 张。
     *
     * <p>校验来源：kamigame「反射」（使用不可。このカードを捨てたとき、カードを2枚引く／
     * kamigame 没有升级行）→ 升级值用 **slaythespire.wiki.gg** 核对：Reflex+ 抽 **3** 张
     * （该页 Update History 记录 Patch V2.2 把 1→2 / 2→3，旧资料 1/2 已过时）。</p>
     */
    public static final Card REFLEX = register(new Card(
            "reflex", "反射", CardType.SKILL, 0, CardTarget.SELF,
            "使用不可。当这张牌被弃置时，抽 2 张牌。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> ctx.announce("  反射：使用不可（被弃置时才会触发）")));

    static {
        upgrade(new Card("reflex", "反射+", CardType.SKILL, 0, CardTarget.SELF,
                "使用不可。当这张牌被弃置时，抽 3 张牌。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> ctx.announce("  反射+：使用不可（被弃置时才会触发）")));
        registerSelfDiscard("reflex", (ctx, owner, card) -> {
            int draw = card != null && card.name().endsWith("+") ? 3 : 2;
            ctx.announce("  【反射】被弃置 → 抽 " + draw + " 张");
            ctx.drawCardsFor(owner.combatant(), draw);
        });
    }

    /**
     * 战术家：**使用不可** 罕见 技能 —— 当这张牌**被弃置**时，获得 1 点能量；升级 2 点。
     *
     * <p>校验来源：kamigame「策士」（使用不可。このカードを捨てたとき、エナジー1を得る／
     * kamigame 没有升级行）→ 升级值用 **slaythespire.wiki.gg** 核对：Tactician+ 给 **2** 点能量
     * （韩文 wikidot 也记 1→2，可交叉印证）。</p>
     */
    public static final Card TACTICIAN = register(new Card(
            "tactician", "战术家", CardType.SKILL, 0, CardTarget.SELF,
            "使用不可。当这张牌被弃置时，获得 1 点能量。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> ctx.announce("  战术家：使用不可（被弃置时才会触发）")));

    static {
        upgrade(new Card("tactician", "战术家+", CardType.SKILL, 0, CardTarget.SELF,
                "使用不可。当这张牌被弃置时，获得 2 点能量。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> ctx.announce("  战术家+：使用不可（被弃置时才会触发）")));
        registerSelfDiscard("tactician", (ctx, owner, card) -> {
            int energy = card != null && card.name().endsWith("+") ? 2 : 1;
            int gained = ctx.gainEnergy(owner.combatant(), energy);
            ctx.announce("  【战术家】被弃置 → 获得 " + gained + " 点能量");
        });
    }

    // ==================================================================
    // v0.5.3.8 A4-17：E5 单卡记账（"这张牌自己记一个数"）
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：ランページ 成长 5→8（基础伤害仍 8）、血には血を 18费4→22费3、
    // 狂戦士 弱体 2→1（能量仍 1）、一撃必殺 12→16、腹裂き 7×3→9×3、ガラスのナイフ 8×2→12×2。

    /**
     * 狂暴：1 费 罕见 攻击 —— 造成 8 点伤害；**这张牌每被打出一次，本场战斗中的伤害就 +5**；
     * 升级把成长值抬到 **+8**（基础伤害仍是 8）。
     *
     * <p>校验来源：kamigame「ランページ」（8ダメージを与える。このカードをプレイするたび、
     * 戦闘終了までダメージが5増加／强化后 8増加 —— ⚠️ 升级**不改基础伤害**）。</p>
     *
     * <p>E5 记账：`PlayerState.cardCounters["rampage"]`。⚠️ 同名多张共享这一份账（没有卡牌实例）。</p>
     */
    public static final Card RAMPAGE = register(new Card(
            "rampage", "暴走", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 8 点伤害。这张牌每被打出一次，本场战斗中的伤害就 +5。", CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> rampage(ctx, self, target, 8, 5)));

    static {
        upgrade(new Card("rampage", "暴走+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 8 点伤害。这张牌每被打出一次，本场战斗中的伤害就 +8。", CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> rampage(ctx, self, target, 8, 8)));
    }

    /** 狂暴的效果体（基础伤害 / 成长值两档）。 */
    private static void rampage(CombatContext ctx, Combatant self, Combatant target, int base, int growth) {
        PlayerState state = ctx.stateOf(self);
        int played = state == null ? 0 : state.cardCounter("rampage");
        int bonus = growth * played;
        ctx.dealDamage(self, target, base + bonus, "「暴走」");
        if (state != null) {
            state.addCardCounter("rampage", 1);
        }
        if (bonus > 0) {
            ctx.announce("  暴走：这是第 " + (played + 1) + " 次打出 → 伤害 " + base + " + " + bonus
                    + " = " + (base + bonus));
        }
    }

    /**
     * 血债血偿：4 费 罕见 攻击 —— 造成 18 点伤害；**本场战斗中你每失去一次生命，这张牌的费用就 -1**；
     * 升级 **22 伤害 + 基础费用 4 → 3**。
     *
     * <p>校验来源：kamigame「血には血を」（戦闘でHPを失うたび、エナジー消費が−1。18ダメージを与える／
     * 强化后 22ダメージ + コスト 3）。</p>
     *
     * <p>费用走 {@code effectiveCost}（每失去一次生命 -1，最低 0；界面显示与实际扣费同一个出口）。</p>
     */
    public static final Card BLOOD_FOR_BLOOD = register(new Card(
            "blood_for_blood", "血债血偿", CardType.ATTACK, 4, CardTarget.ENEMY,
            "造成 18 点伤害。本场战斗中你每失去一次生命，这张牌的费用就 -1。",
            CardRarity.UNCOMMON, IRONCLAD,
            (ctx, self, target) -> ctx.dealDamage(self, target, 18, "「血债血偿」")));

    static {
        upgrade(new Card("blood_for_blood", "血债血偿+", CardType.ATTACK, 3, CardTarget.ENEMY,
                "造成 22 点伤害。本场战斗中你每失去一次生命，这张牌的费用就 -1。",
                CardRarity.UNCOMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, 22, "「血债血偿+」")));
    }

    /**
     * 狂暴（稀有版）：0 费 稀有 能力 —— **获得 2 层易伤**（代价），并且**每个回合开始获得 1 点能量**；
     * 升级把代价降到 **1 层易伤**（能量仍是 1）。
     *
     * <p>校验来源：kamigame「狂戦士」（弱体2を得る。ターン開始時、エナジー1を得る／强化后 弱体1）。</p>
     */
    public static final Card BERSERK = register(new Card(
            "berserk", "狂暴", CardType.POWER, 0, CardTarget.SELF,
            "获得 2 层易伤。在你的回合开始时，获得 1 点能量。", CardRarity.RARE, IRONCLAD,
            (ctx, self, target) -> berserk(ctx, self, 2)));

    static {
        upgrade(new Card("berserk", "狂暴+", CardType.POWER, 0, CardTarget.SELF,
                "获得 1 层易伤。在你的回合开始时，获得 1 点能量。", CardRarity.RARE, IRONCLAD,
                (ctx, self, target) -> berserk(ctx, self, 1)));
    }

    /** 狂暴（稀有版）的效果体（易伤层数两档；每回合能量固定 1）。 */
    private static void berserk(CombatContext ctx, Combatant self, int vulnerable) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        self.addVulnerable(vulnerable);
        state.berserk += 1;
        ctx.announce("  【狂暴】：获得 " + vulnerable + " 层易伤；每回合开始 +1 能量" + self.statusSuffix());
    }

    /**
     * 高超打击：0 费 罕见 攻击 —— 造成 12 点伤害；**本场战斗中你每失去一次生命，这张牌的费用就 +1**；
     * 升级 16 伤害。
     *
     * <p>校验来源：kamigame「一撃必殺」（12ダメージを与える。ダメージを受けるたび、このカードの
     * コストが1増加／强化后 16）。</p>
     *
     * <p>⚠️ 判断：日文写的是"**受到伤害**时 +1 费"，我实现成"**失去生命**时 +1 费" ——
     * 即与【血债血偿】共用同一份 `hpLossEvents` 账。理由是原版资料对这张牌的通行说法是
     * "each time you lose HP this combat"，且被格挡吃光的攻击不该让牌变贵。
     * 若要改成"受到任意伤害（含被格挡）"，需要另立一份计数（见交接稿 §4.45）。</p>
     */
    public static final Card MASTERFUL_STAB = register(new Card(
            "masterful_stab", "高超打击", CardType.ATTACK, 0, CardTarget.ENEMY,
            "造成 12 点伤害。本场战斗中你每失去一次生命，这张牌的费用就 +1。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> ctx.dealDamage(self, target, 12, "「高超打击」")));

    static {
        upgrade(new Card("masterful_stab", "高超打击+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "造成 16 点伤害。本场战斗中你每失去一次生命，这张牌的费用就 +1。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> ctx.dealDamage(self, target, 16, "「高超打击+」")));
    }

    /**
     * 剔骨：3 费 罕见 攻击 —— 造成 7 点伤害 **3 次**；**本回合中你每弃掉一张牌，这张牌的费用就 -1**；
     * 升级 9 伤害 ×3。
     *
     * <p>校验来源：kamigame「腹裂き」（このターンに捨てたカード1枚につき、エナジーの消費−1。
     * 7ダメージを3回与える／强化后 9×3）。</p>
     */
    public static final Card EVISCERATE = register(new Card(
            "eviscerate", "剔骨", CardType.ATTACK, 3, CardTarget.ENEMY,
            "造成 7 点伤害 3 次。本回合中你每弃掉一张牌，这张牌的费用就 -1。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> eviscerate(ctx, self, target, 7)));

    static {
        upgrade(new Card("eviscerate", "剔骨+", CardType.ATTACK, 3, CardTarget.ENEMY,
                "造成 9 点伤害 3 次。本回合中你每弃掉一张牌，这张牌的费用就 -1。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> eviscerate(ctx, self, target, 9)));
    }

    /** 剔骨的效果体（每段伤害两档，固定 3 段）。 */
    private static void eviscerate(CombatContext ctx, Combatant self, Combatant target, int damage) {
        PlayerState state = ctx.stateOf(self);
        int discarded = state == null ? 0 : state.cardsDiscardedThisTurn();
        if (discarded > 0) {
            ctx.announce("  剔骨：本回合已弃掉 " + discarded + " 张 → 费用 -" + discarded
                    + "（实际 " + (state == null ? 0 : ctx.effectiveCost(state, EVISCERATE)) + " 费）");
        }
        for (int i = 0; i < 3; i++) {
            if (target == null || !target.alive()) {
                break;
            }
            ctx.dealDamage(self, target, damage, "「剔骨」");
        }
    }

    /**
     * 玻璃刀：1 费 稀有 攻击 —— 造成 8 点伤害 **2 次**；**这张牌每被打出一次，本场战斗中的伤害就 -2**；
     * 升级 12 伤害 ×2。
     *
     * <p>校验来源：kamigame「ガラスのナイフ」（8ダメージを2回与える。この戦闘中、このカードの
     * ダメージが2低下する／强化后 12×2）。</p>
     *
     * <p>E5 记账：`PlayerState.cardCounters["glass_knife"]`（每打出一次 +1，伤害 = 基础 - 2 × 次数）。</p>
     */
    public static final Card GLASS_KNIFE = register(new Card(
            "glass_knife", "玻璃刀", CardType.ATTACK, 1, CardTarget.ENEMY,
            "造成 8 点伤害 2 次。这张牌每被打出一次，本场战斗中的伤害就 -2。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> glassKnife(ctx, self, target, 8)));

    static {
        upgrade(new Card("glass_knife", "玻璃刀+", CardType.ATTACK, 1, CardTarget.ENEMY,
                "造成 12 点伤害 2 次。这张牌每被打出一次，本场战斗中的伤害就 -2。",
                CardRarity.RARE, SILENT,
                (ctx, self, target) -> glassKnife(ctx, self, target, 12)));
    }

    /** 玻璃刀的效果体（基础伤害两档，固定 2 段、每次 -2）。 */
    private static void glassKnife(CombatContext ctx, Combatant self, Combatant target, int base) {
        PlayerState state = ctx.stateOf(self);
        int played = state == null ? 0 : state.cardCounter("glass_knife");
        int damage = Math.max(0, base - 2 * played);
        if (played > 0) {
            ctx.announce("  玻璃刀：已经打出过 " + played + " 次 → 本次每段 " + base + " - " + (2 * played)
                    + " = " + damage + " 点");
        }
        for (int i = 0; i < 2; i++) {
            if (target == null || !target.alive()) {
                break;
            }
            ctx.dealDamage(self, target, damage, "「玻璃刀」");
        }
        if (state != null) {
            state.addCardCounter("glass_knife", 1);
        }
    }

    // ==================================================================
    // v0.5.3.9 A4-18：消耗堆选牌（E11）+ 条件可打出（E16）+ 下回合给牌
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：発掘 1→0 费、クラッシュ 14→18、悪夢 3→2 费、
    // スニーキーストライク 12→16、動揺 1→0 费。
    // ⚠️ kamigame 上「職人技」是 **Expertise**（抽到 6/7 张手牌），不是 Distraction。

    /**
     * 挖掘：1 费（升级 **0** 费）稀有 技能 消耗 —— **从消耗堆里选 1 张牌拿回手牌**，然后消耗。
     *
     * <p>校验来源：kamigame「発掘」（廃棄したカードから1枚選んで手札に加える。廃棄／
     * 强化后 コスト 1 → 0，效果不变）。</p>
     *
     * <p>这条解锁的是 **E11**：选牌系统新增 `CardPickSource.EXHAUST`（消耗堆）与
     * `CardPickKind.EXHUME`（拿回手牌）。</p>
     */
    public static final Card EXHUME = register(new Card(
            "exhume", "挖掘", CardType.SKILL, 1, CardTarget.SELF,
            "从你的消耗堆里选择一张牌，将其加入你的手牌。消耗。",
            CardRarity.RARE, IRONCLAD, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> exhume(ctx, self)));

    static {
        upgrade(new Card("exhume", "挖掘+", CardType.SKILL, 0, CardTarget.SELF,
                "从你的消耗堆里选择一张牌，将其加入你的手牌。消耗。",
                CardRarity.RARE, IRONCLAD, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> exhume(ctx, self)));
    }

    /** 挖掘的效果体（基础版与强化版只差费用）。 */
    private static void exhume(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null || state.exhaustPile.view().isEmpty()) {
            ctx.announce("  挖掘：消耗堆是空的，没有牌可以拿回来");
            return;
        }
        ctx.beginCardPick(self, "「挖掘」", CombatContext.CardPickKind.EXHUME,
                CombatContext.CardPickSource.EXHAUST, 1, null);
    }

    /**
     * 交锋：0 费 常见 攻击 —— **只有当你手里全部是攻击牌时才能打出**；造成 14 点伤害；升级 18 点。
     *
     * <p>校验来源：kamigame「クラッシュ」（手札がすべてアタックの場合にプレイ可能。14ダメージを与える／
     * 强化后 18）。</p>
     *
     * <p>这条解锁的是 **E16**：`CombatContext.playableNow(state, card)` —— 出牌门禁与
     * 下发客户端的 {@code HandCard.playable} 共用同一个判定。</p>
     */
    public static final Card CLASH = register(new Card(
            "clash", "交锋", CardType.ATTACK, 0, CardTarget.ENEMY,
            "只有当你手牌中全部是攻击牌时才能打出。造成 14 点伤害。", CardRarity.COMMON, IRONCLAD,
            (ctx, self, target) -> ctx.dealDamage(self, target, 14, "「交锋」")));

    static {
        upgrade(new Card("clash", "交锋+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "只有当你手牌中全部是攻击牌时才能打出。造成 18 点伤害。", CardRarity.COMMON, IRONCLAD,
                (ctx, self, target) -> ctx.dealDamage(self, target, 18, "「交锋+」")));
    }

    /**
     * 噩梦：3 费（升级 **2** 费）稀有 技能 消耗 —— 从手牌选 1 张，**下个回合**开始把
     * **3 张复制**加入手牌，然后消耗。
     *
     * <p>校验来源：kamigame「悪夢」（手札のカード1枚を選ぶ。次のターン、そのカードの複製を
     * 3枚手札に加える。廃棄／强化后 コスト 3 → 2）。</p>
     *
     * <p>用 {@code PlayerState.nextTurnCards}（下回合开始加入手牌的**指定牌**）——
     * 与"下回合多抽 N 张"是两回事。</p>
     */
    public static final Card NIGHTMARE = register(new Card(
            "nightmare", "噩梦", CardType.SKILL, 3, CardTarget.SELF,
            "选择一张手牌。在下个回合开始时，将 3 张它的复制加入你的手牌。消耗。",
            CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> nightmare(ctx, self)));

    static {
        upgrade(new Card("nightmare", "噩梦+", CardType.SKILL, 2, CardTarget.SELF,
                "选择一张手牌。在下个回合开始时，将 3 张它的复制加入你的手牌。消耗。",
                CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> nightmare(ctx, self)));
    }

    /** 噩梦的效果体（基础版与强化版只差费用）。 */
    private static void nightmare(CombatContext ctx, Combatant self) {
        if (ctx.hand().isEmpty()) {
            ctx.announce("  噩梦：手牌是空的，没有牌可以复制");
            return;
        }
        ctx.beginCardPick(self, "「噩梦」", CombatContext.CardPickKind.NIGHTMARE,
                CombatContext.CardPickSource.HAND, 1, null, null, 3);
    }

    /**
     * 偷偷一击：2 费 常见 攻击 —— 造成 12 点伤害；**如果本回合你弃过牌**，获得 2 点能量；升级 16 伤害。
     *
     * <p>校验来源：kamigame「スニーキーストライク」（12ダメージを与える。このターンにカードを
     * 捨てていれば、エナジー2を得る／强化后 16）。</p>
     *
     * <p>复用 v0.5.3.8 的 {@code cardsDiscardedThisTurn}（【剔骨】那份账）。</p>
     */
    public static final Card SNEAKY_STRIKE = register(new Card(
            "sneaky_strike", "偷偷一击", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 12 点伤害。如果本回合你弃过牌，获得 2 点能量。", CardRarity.COMMON, SILENT,
            (ctx, self, target) -> sneakyStrike(ctx, self, target, 12)));

    static {
        upgrade(new Card("sneaky_strike", "偷偷一击+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 16 点伤害。如果本回合你弃过牌，获得 2 点能量。", CardRarity.COMMON, SILENT,
                (ctx, self, target) -> sneakyStrike(ctx, self, target, 16)));
    }

    /** 偷偷一击的效果体（伤害两档，回能量固定 2）。 */
    private static void sneakyStrike(CombatContext ctx, Combatant self, Combatant target, int damage) {
        ctx.dealDamage(self, target, damage, "「偷偷一击」");
        PlayerState state = ctx.stateOf(self);
        if (state != null && state.cardsDiscardedThisTurn() > 0) {
            int gained = ctx.gainEnergy(self, 2);
            ctx.announce("  偷偷一击：本回合已弃掉 " + state.cardsDiscardedThisTurn() + " 张 → 获得 "
                    + gained + " 点能量");
        }
    }

    /**
     * 转移：1 费（升级 **0** 费）罕见 技能 消耗 —— 随机加一张**技能牌**入手，**本回合 0 费**，然后消耗。
     *
     * <p>校验来源：kamigame「動揺」= Distraction（ランダムなスキルを1枚手札に加える。このターン、
     * そのコストは0。廃棄／强化后 コスト 1 → 0，效果不变）。</p>
     *
     * <p>机制与【地狱之刃】完全对称，只是取的是**技能**池。</p>
     */
    public static final Card DISTRACTION = register(new Card(
            "distraction", "转移", CardType.SKILL, 1, CardTarget.SELF,
            "将一张随机技能牌加入你的手牌，这张牌在本回合的费用为 0。消耗。",
            CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> distraction(ctx, self)));

    static {
        upgrade(new Card("distraction", "转移+", CardType.SKILL, 0, CardTarget.SELF,
                "将一张随机技能牌加入你的手牌，这张牌在本回合的费用为 0。消耗。",
                CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> distraction(ctx, self)));
    }

    /** 转移的效果体（基础版与强化版只差费用）。 */
    private static void distraction(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        Card gift = ctx.randomSkillCard(SILENT);
        if (gift == null) {
            ctx.announce("  转移：抽不到技能牌");
            return;
        }
        int added = ctx.addCardsToHand(self, gift, 1);
        state.markFreeThisTurn(gift.id());
        ctx.announce("  转移：加入 " + added + " 张「" + gift.name() + "」，它在本回合的费用为 0");
    }

    // ==================================================================
    // v0.5.4.0 A4-19：无形（E15）/ 条件可打出扩展 / 每张牌的伤害与费用改写
    // ==================================================================
    //
    // 数值全部逐页核对过 kamigame：グランドフィナーレ 50→60、幻影の暗殺者 1→0 费、
    // 窒息 HP 3→5、バレットタイム 3→2 费、死霊化 無形 2→3、終わりなき苦痛 4→6。

    /**
     * 华丽收场：0 费 稀有 攻击 —— **只有抽牌堆里没有牌时才能打出**；对**全体敌人**造成 50 点伤害；
     * 升级 60 点。
     *
     * <p>校验来源：kamigame「グランドフィナーレ」（山札にカードがない時だけプレイ可能。
     * 敵全体に50ダメージを与える／强化后 60）。</p>
     *
     * <p>复用 v0.5.3.9 的 {@code playableNow}：出牌门禁与客户端灰显共用同一个判定。</p>
     */
    public static final Card GRAND_FINALE = register(new Card(
            "grand_finale", "华丽收场", CardType.ATTACK, 0, CardTarget.ALL_ENEMIES,
            "只有当你抽牌堆中没有牌时才能打出。对所有敌人造成 50 点伤害。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> grandFinale(ctx, self, 50)));

    static {
        upgrade(new Card("grand_finale", "华丽收场+", CardType.ATTACK, 0, CardTarget.ALL_ENEMIES,
                "只有当你抽牌堆中没有牌时才能打出。对所有敌人造成 60 点伤害。",
                CardRarity.RARE, SILENT,
                (ctx, self, target) -> grandFinale(ctx, self, 60)));
    }

    /** 华丽收场的效果体（伤害两档）。 */
    private static void grandFinale(CombatContext ctx, Combatant self, int damage) {
        for (Enemy enemy : ctx.aliveEnemies()) {
            ctx.dealDamage(self, enemy, damage, "「华丽收场」");
        }
    }

    /**
     * 幻影杀手：1 费 稀有 技能 —— **下个回合**，你的攻击伤害翻倍；升级 费用 1 → 0。
     *
     * <p>校验来源：kamigame「幻影の暗殺者」（**次のターン**、アタックのダメージが2倍になる／
     * 强化后 コスト 1 → 0）。⚠️ 是「次のターン」= **下个回合**，不是"本回合"。</p>
     */
    public static final Card PHANTASMAL_KILLER = register(new Card(
            "phantasmal_killer", "幻影杀手", CardType.SKILL, 1, CardTarget.SELF,
            "在下个回合，你的攻击牌伤害翻倍。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> phantasmalKiller(ctx, self)));

    static {
        upgrade(new Card("phantasmal_killer", "幻影杀手+", CardType.SKILL, 0, CardTarget.SELF,
                "在下个回合，你的攻击牌伤害翻倍。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> phantasmalKiller(ctx, self)));
    }

    /** 幻影杀手的效果体（基础版与强化版只差费用）。 */
    private static void phantasmalKiller(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.phantomKillerPending = true;
        ctx.announce("  【幻影杀手】：下个回合你的攻击牌伤害翻倍");
    }

    /**
     * 窒息：2 费 罕见 攻击 —— 造成 12 点伤害；**本回合内每打出一张牌**，这个敌人就失去 3 点生命
     * （无视格挡）；升级把每张的失血抬到 5。
     *
     * <p>校验来源：kamigame「窒息」（12ダメージを与える。このターンカードをプレイするたび、
     * 対象の敵はHP3を失う／强化后 HP5，伤害与费用不变）。</p>
     */
    public static final Card CHOKE = register(new Card(
            "choke", "窒息", CardType.ATTACK, 2, CardTarget.ENEMY,
            "造成 12 点伤害。在本回合内，每当你打出一张牌，这名敌人就失去 3 点生命。",
            CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> choke(ctx, self, target, 12, 3)));

    static {
        upgrade(new Card("choke", "窒息+", CardType.ATTACK, 2, CardTarget.ENEMY,
                "造成 12 点伤害。在本回合内，每当你打出一张牌，这名敌人就失去 5 点生命。",
                CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> choke(ctx, self, target, 12, 5)));
    }

    /** 窒息的效果体（伤害固定 12，失血两档）。 */
    private static void choke(CombatContext ctx, Combatant self, Combatant target, int damage, int perCard) {
        ctx.dealDamage(self, target, damage, "「窒息」");
        PlayerState state = ctx.stateOf(self);
        if (state == null || !(target instanceof Enemy enemy)) {
            return;
        }
        state.chokeEnemy = enemy;
        state.chokeAmount = perCard;
        ctx.announce("  【窒息】：本回合此后每打出一张牌，" + enemy.name() + " 失去 " + perCard
                + " 点生命（这张牌自己不算）");
    }

    /**
     * 子弹时间：3 费（升级 **2** 费）稀有 技能 —— **本回合不能抽牌**，且**手牌费用全部变成 0**。
     *
     * <p>校验来源：kamigame「バレットタイム」（このターン、カードを引くことができない。
     * このターン、あなたの手札のコストは0になる／强化后 コスト 3 → 2）。</p>
     */
    public static final Card BULLET_TIME = register(new Card(
            "bullet_time", "子弹时间", CardType.SKILL, 3, CardTarget.SELF,
            "在本回合内，你不能抽牌。在本回合内，你的手牌费用为 0。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> bulletTime(ctx, self)));

    static {
        upgrade(new Card("bullet_time", "子弹时间+", CardType.SKILL, 2, CardTarget.SELF,
                "在本回合内，你不能抽牌。在本回合内，你的手牌费用为 0。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> bulletTime(ctx, self)));
    }

    /** 子弹时间的效果体（基础版与强化版只差费用）。 */
    private static void bulletTime(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.bulletTime = true;
        ctx.announce("  【子弹时间】：本回合不能抽牌，手牌费用全部为 0");
    }

    /**
     * 幽魂形态（死霊化）：3 费 稀有 能力 —— 获得 **2 层无形**（升级 3 层）；
     * **回合结束时失去 1 点敏捷**。
     *
     * <p>校验来源：kamigame「死霊化」（無形2を得る。ターン終了時、敏捷性1を失う／
     * 强化后 無形3，费用与敏捷惩罚不变）。</p>
     *
     * <p>这条解锁的是 **E15 无形**：`Combatant.intangibleTurns` —— 期间受到的伤害**一律变成 1**，
     * 在自己回合开始时递减。</p>
     */
    public static final Card WRAITH_FORM = register(new Card(
            "wraith_form", "幽魂形态", CardType.POWER, 3, CardTarget.SELF,
            "获得 2 层无形。在每个回合结束时，失去 1 点敏捷。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> wraithForm(ctx, self, 2)));

    static {
        upgrade(new Card("wraith_form", "幽魂形态+", CardType.POWER, 3, CardTarget.SELF,
                "获得 3 层无形。在每个回合结束时，失去 1 点敏捷。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> wraithForm(ctx, self, 3)));
    }

    /** 幽魂形态的效果体（无形层数两档）。 */
    private static void wraithForm(CombatContext ctx, Combatant self, int intangible) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.wraithForm = true;
        self.addIntangibleTurns(intangible);
        ctx.announce("  【幽魂形态】：获得 " + intangible + " 层无形（接下来 " + intangible
                + " 个敌人回合里每种伤害都只有 1 点）；回合结束时失去 1 点敏捷");
    }

    /**
     * 无尽苦痛：0 费 罕见 攻击 消耗 —— **每当你抽到这张牌**，就往手里再补一张；造成 4 点伤害；
     * 升级 6 点。
     *
     * <p>校验来源：kamigame「終わりなき苦痛」（このカードを引くたび、「終わりなき苦痛」を手札に加える。
     * 4ダメージを与える。廃棄／强化后 6ダメージ）。</p>
     *
     * <p>⚠️ 它不是"使用不可"（该页没有 プレイ不可 字样）—— 是一张 0 费攻击，只是抽到就自我复制。</p>
     */
    public static final Card ENDLESS_AGONY = register(new Card(
            "endless_agony", "无尽苦痛", CardType.ATTACK, 0, CardTarget.ENEMY,
            "每当你抽到这张牌，就将一张『无尽苦痛』加入你的手牌。造成 4 点伤害。消耗。",
            CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> ctx.dealDamage(self, target, 4, "「无尽苦痛」")));

    static {
        upgrade(new Card("endless_agony", "无尽苦痛+", CardType.ATTACK, 0, CardTarget.ENEMY,
                "每当你抽到这张牌，就将一张『无尽苦痛』加入你的手牌。造成 6 点伤害。消耗。",
                CardRarity.UNCOMMON, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> ctx.dealDamage(self, target, 6, "「无尽苦痛+」")));
    }

    // ==================================================================
    // v0.5.4.1（A4-20）收官批：灼热打击 + 炼金 / 尸体爆炸 / 行业工具 / 计划周全
    // ==================================================================

    /**
     * 灼热打击：2 费 罕见 攻击 —— 造成 12 点伤害；**这张牌可以被强化任意次**，每次 +4 伤害。
     *
     * <p>校验来源：kamigame「灼熱の一撃」（12ダメージを与える。このカードは何度でも
     * アップグレードできる／强化后 16ダメージ，费用不变）。页面只列了基础版与 +1 两行，
     * 第 +2 档起按"每档 +4"递推（原版规则就是每强化一次 +4）。</p>
     *
     * <p>这是**唯一**一张"多档强化"卡：档位定义懒生成、不进 {@link #BY_ID}
     * （见 {@link #searingBlowCard(int)}），所以图鉴与奖励池里只出现基础版一张。</p>
     */
    public static final Card SEARING_BLOW = register(searingBlowCard(0));

    static {
        // 登记"可以强化任意次"：卡牌档案的"还能不能强化"问 CardLibrary.maxUpgrades
        MAX_UPGRADES.put("searing_blow", Integer.MAX_VALUE);
        // +1 档照样登记成"强化版"，这样【查看升级】、图鉴的强化对照都照常工作
        upgrade(searingBlowCard(1));
    }

    /**
     * 炼金：1 费（升级 **0** 费）稀有 技能 消耗 —— 随机获得一瓶药水。
     *
     * <p>校验来源：kamigame「錬金術」（ランダムなポーションを1つ入手する。廃棄／
     * 强化后 コスト 1 → 0，效果文本不变）。</p>
     *
     * <p>⚠️ 药水是**档案层**的数据（{@code PlayerCardData.potions}），零 MC 依赖的引擎碰不到它，
     * 所以这里只记一笔"待发药水"（{@code PlayerState.pendingPotions}），由 MC 层在**这张牌打完之后**
     * 立刻兑现（{@code CombatManager.grantPendingPotions}）—— 这样本场战斗就能直接用上。</p>
     */
    public static final Card ALCHEMIZE = register(new Card(
            "alchemize", "炼金", CardType.SKILL, 1, CardTarget.SELF,
            "随机获得一瓶药水。消耗。", CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
            (ctx, self, target) -> alchemize(ctx, self, 1)));

    static {
        upgrade(new Card("alchemize", "炼金+", CardType.SKILL, 0, CardTarget.SELF,
                "随机获得一瓶药水。消耗。", CardRarity.RARE, SILENT, Set.of(CardKeyword.EXHAUST),
                (ctx, self, target) -> alchemize(ctx, self, 1)));
    }

    /** 炼金的效果体（基础版与强化版只差费用）。 */
    private static void alchemize(CombatContext ctx, Combatant self, int count) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.addPendingPotion(count);
        ctx.announce("  【炼金】：随机获得 " + count + " 瓶药水");
    }

    /**
     * 尸体爆炸：2 费 稀有 技能 —— 给予 6 层中毒（升级 **9**）；
     * **该敌人死亡时**，对所有敌人造成等同于其最大生命值的伤害。
     *
     * <p>校验来源：kamigame「死体爆破」（毒6を与える。対象の敵が死亡した時、
     * その最大HPに等しいダメージを敵全体に与える／强化后 毒9，费用不变）。</p>
     *
     * <p>实现见 {@code CombatContext.detonateCorpseExplosions()}：标记记在**敌人身上**
     * （{@code Combatant.corpseExplosion}），在 {@code checkBattleEnd()} 的最前面结算 ——
     * 所以无论是被伤害、中毒、凋零还是荆棘打死的，只要它死了就会炸。</p>
     */
    public static final Card CORPSE_EXPLOSION = register(new Card(
            "corpse_explosion", "尸体爆炸", CardType.SKILL, 2, CardTarget.ENEMY,
            "给予 6 层中毒。当该敌人死亡时，对所有敌人造成等同于其最大生命值的伤害。",
            CardRarity.RARE, SILENT,
            (ctx, self, target) -> corpseExplosion(ctx, target, 6)));

    static {
        upgrade(new Card("corpse_explosion", "尸体爆炸+", CardType.SKILL, 2, CardTarget.ENEMY,
                "给予 9 层中毒。当该敌人死亡时，对所有敌人造成等同于其最大生命值的伤害。",
                CardRarity.RARE, SILENT,
                (ctx, self, target) -> corpseExplosion(ctx, target, 9)));
    }

    /** 尸体爆炸的效果体（中毒两档；死亡爆炸的标记一样）。 */
    private static void corpseExplosion(CombatContext ctx, Combatant target, int poison) {
        if (target == null) {
            ctx.announce("  尸体爆炸：没有目标");
            return;
        }
        ctx.applyPoison(target, poison);
        target.markCorpseExplosion();
        ctx.announce("  【尸体爆炸】：" + target.name() + " 死亡时，将对所有敌人造成等同于其最大生命值（"
                + target.maxHp() + "）的伤害");
    }

    /**
     * 行业工具：1 费（升级 **0** 费）稀有 能力 —— 在你的回合开始时，抽 1 张牌，然后弃 1 张牌。
     *
     * <p>校验来源：kamigame「商売道具」（ターン開始時、カードを1枚引き、1枚捨てる／
     * 强化后 コスト 1 → 0，效果文本不变）。</p>
     */
    public static final Card TOOLS_OF_THE_TRADE = register(new Card(
            "tools_of_the_trade", "行业工具", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合开始时，抽 1 张牌，然后弃 1 张牌。", CardRarity.RARE, SILENT,
            (ctx, self, target) -> toolsOfTheTrade(ctx, self)));

    static {
        upgrade(new Card("tools_of_the_trade", "行业工具+", CardType.POWER, 0, CardTarget.SELF,
                "在你的回合开始时，抽 1 张牌，然后弃 1 张牌。", CardRarity.RARE, SILENT,
                (ctx, self, target) -> toolsOfTheTrade(ctx, self)));
    }

    /** 行业工具的效果体（基础版与强化版只差费用）。 */
    private static void toolsOfTheTrade(CombatContext ctx, Combatant self) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.toolsOfTheTrade += 1;
        ctx.announce("  【行业工具】生效：每个回合开始抽 1 张、弃 1 张（共 " + state.toolsOfTheTrade() + " 层）");
    }

    /**
     * 计划周全：1 费 罕见 能力 —— 在你的回合结束时，**保留 1 张牌**；升级 **2 张**。
     *
     * <p>校验来源：kamigame「用意周到」（ターン終了時、カードを1枚保留する／
     * 强化后 1 → 2 枚，费用不变）。</p>
     */
    public static final Card WELL_LAID_PLANS = register(new Card(
            "well_laid_plans", "计划周全", CardType.POWER, 1, CardTarget.SELF,
            "在你的回合结束时，保留 1 张牌。", CardRarity.UNCOMMON, SILENT,
            (ctx, self, target) -> wellLaidPlans(ctx, self, 1)));

    static {
        upgrade(new Card("well_laid_plans", "计划周全+", CardType.POWER, 1, CardTarget.SELF,
                "在你的回合结束时，保留 2 张牌。", CardRarity.UNCOMMON, SILENT,
                (ctx, self, target) -> wellLaidPlans(ctx, self, 2)));
    }

    /** 计划周全的效果体（保留张数两档；打出两张会叠加）。 */
    private static void wellLaidPlans(CombatContext ctx, Combatant self, int retain) {
        PlayerState state = ctx.stateOf(self);
        if (state == null) {
            return;
        }
        state.wellLaidPlans += retain;
        ctx.announce("  【计划周全】生效：每个回合结束保留 " + retain + " 张牌（共 "
                + state.wellLaidPlans() + " 张）");
    }
}





















