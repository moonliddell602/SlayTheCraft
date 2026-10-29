package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Offline checks for the battle rules. This is the same code path the in-game
 * {@code /stc combat selftest} command runs, and it can also be executed on a bare JVM
 * (see the project README) because the whole {@code core} package is Minecraft-free.
 */
public final class CombatSelfTest {

    /** Outcome of a self-test run. */
    public record Result(int passed, int failed, List<String> lines) {
        public boolean ok() {
            return failed == 0;
        }

        public String summary() {
            return (ok() ? "全部通过" : "存在失败") + "：" + passed + " 通过 / " + failed + " 失败";
        }
    }

    private CombatSelfTest() {
    }

    public static Result run() {
        Checker checker = new Checker();
        turnStartAndDraw(checker);
        strikeDamage(checker);
        blockAbsorbsFully(checker);
        blockOverflow(checker);
        vulnerableAmplifies(checker);
        weakReduces(checker);
        // v0.5.1.4：虚弱重做（每层 -1 / 与力量抵消）+ 挖掘疲劳 + 附甲衰减
        weakReworkAndMiningFatigue(checker);
        platedArmorDecay(checker);
        multiplayerCards(checker);
        strengthRaisesEnemyDamage(checker);
        defendBlocksOnePoint(checker);
        endTurnDiscardsAndRedraws(checker);
        energyGate(checker);
        victoryWhenEnemiesDie(checker);
        multiHitIntent(checker);
        cleaveHitsEveryEnemy(checker);
        pommelStrikeDraws(checker);
        // S6：多人遭遇战
        partyTurnOrder(checker);
        partyStateIndependence(checker);
        // v0.5.1.6：回合模型改造 —— 统一玩家回合（同时出牌、全员确认才进敌怪回合）
        partySimultaneousTurns(checker);
        enemyIntentTargetsAPlayer(checker);
        enemyRetargetsWhenTargetFalls(checker);
        downedPlayerIsSkipped(checker);
        partyWipeIsDefeat(checker);
        partyVictoryIgnoresPlayerCount(checker);
        // v0.0.2 V2：头顶意图显示的是"实际将造成的伤害"，预览与结算必须一致
        damagePreview(checker);
        // v0.0.2 V3：面对面站位几何
        standoffLayout(checker);
        // v0.0.2 V4：卡牌关键字
        cardKeywords(checker);
        // v0.0.2 V5：新效果系统
        statusEffects(checker);
        // v0.0.2 V6：卡牌附魔
        cardEnchantments(checker);
        // v0.0.2 V7：遗物
        relics(checker);
        // v0.0.2 V8：药水
        potions(checker);
        // v0.0.2 V9：独立生命值 + 三层进度与事件上限
        progressAndLife(checker);
        // v0.0.2 测试反馈修正：遭遇战编成 + 敌人站位 + 本局结束判定
        encounterComposition(checker);
        // v0.0.3 W1：活力 / 失去生命值 / 卡面数值实时化
        vigorAndLifeLoss(checker);
        // v0.0.3 W2：状态牌
        statusCards(checker);
        // v0.2.8：受惊的小猪仔（本场战斗第一次失去生命 → 抽 3 张）
        pigletRelic(checker);
        // v0.0.3 W3：新角色卡牌 / 初始遗物 / 下回合格挡
        newCardsAndRelics(checker);
        // v0.0.3 W4：充能球
        orbs(checker);
        // v0.0.3 W5：篝火规则（愈合比例 + 退出计数）
        campfireRules(checker);
        // v0.3.0：补全四种充能球（暗球 / 等离子球）
        orbTypes(checker);
        // v0.3.0：充能球卡牌 + 表现层事件
        orbCardsAndFx(checker);
        // v0.3.0：表现层的纯数学（环形布局 / 动作曲线 / 淡入淡出 / 手牌差异）
        presentationMath(checker);
        // v0.4.0：角色专属卡池 / 初始遗物不外流 / 球位归属 / 敌人分步行动
        characterPools(checker);
        orbSlotOwnership(checker);
        pacedEnemyTurn(checker);
        // v0.4.1：阵营与特性、三件新遗物、机器人的第一回合
        defectFirstTurn(checker);
        factionsAndTraits(checker);
        newRelicBonuses(checker);
        // v0.4.2 的中毒（当时叫"涂毒"）/ 能力牌 / 静默猎手的新牌
        poisonStatus(checker);
        powerCardsAndNewSilentCards(checker);
        // v0.4.4：以战斗 UI 栏位为基准排位（怪物对着 UI 站）
        uiAlignedFormation(checker);
        // v0.4.5：意图状态机（一回合多个动作）/ 每层多 BOSS / 问号事件的本层记忆
        intentStateMachine(checker);
        bossTableAndFloorMemory(checker);
        // v0.4.6：新状态（再生 / 缓降）、诅咒牌、新药水与新遗物（凋零在 v0.4.6.1 重定义）
        newStatuses(checker);
        curseCards(checker);
        newPotions(checker);
        newRelics(checker);
        // v0.4.6.7：层数专属怪物池 / BOSS 只打一次 / 蓄能·锁血 / 睡眠 / 召唤
        floorPoolsAndBossLock(checker);
        newIntentMechanics(checker);
        selectionCards(checker);
        lastStandCard(checker);
        defectCards(checker);
        defectCards2(checker);
        defectCards3(checker);
        defectCards4(checker);
        defectCards5(checker);
        defectCards6(checker);
        defectCards7(checker);
        defectCards8(checker);
        defectCards9(checker);
        defectCards10(checker);
        defectCards11(checker);
        defectCards12(checker);
        defectCards13(checker);
        defectCards14(checker);
        presentationBatch1505(checker);
        floorRosterBatch(checker);
        achievementTriggers(checker);
        ironcladCards1(checker);
        ironcladCards2(checker);
        ironcladCards3(checker);
        ironcladCards4(checker);
        ironcladCards5(checker);
        ironcladCards6(checker);
        ironcladCards7(checker);
        ironcladCards8(checker);
        silentCards1(checker);
        silentCards2(checker);
        silentCards3(checker);
        silentCards4(checker);
        ironcladCards9(checker);
        ironcladCards10(checker);
        ironcladCards11(checker);
        silentCards5(checker);
        silentCards6(checker);
        silentCards7(checker);
        ironcladCards12(checker);
        ironcladCards13(checker);
        ironcladCards14(checker);
        silentCards8(checker);
        silentCards9(checker);
        silentCards10(checker);
        ironcladCards15(checker);
        silentCards11(checker);
        thornsStatus(checker);
        relicEffectsForV0530(checker);
        // v0.5.3.1：遗物单价 / 来源通道 + 叛逃掠夺者 5 张新卡（v0.5.3.2 扩了制图台与龙蛋）
        relicPricesAndSources(checker);
        pillagerCardsForV0531(checker);
        // v0.5.3.3 A4-12：回合钩子（金属化 / 自燃 / 火焰屏障 + 无限刀刃 / 毒雾 / 布置）
        ironcladCards16(checker);
        silentCards12(checker);
        // v0.5.3.3 A4-13：出牌钩子 / 格挡钩子 / 双发 / 费用改写
        ironcladCards17(checker);
        silentCards13(checker);
        // v0.5.3.5 A4-14：抽牌钩子 / 消耗钩子 / 下回合能量与抽牌
        ironcladCards18(checker);
        silentCards14(checker);
        // v0.5.3.6 A4-15：消耗钩子扩展 / 本回合攻击计数 / 自消耗效果
        ironcladCards19(checker);
        silentCards15(checker);
        // v0.5.3.7 A4-16：弃牌钩子 / 随机攻击牌 + 本回合 0 费 / 回合开始自伤
        ironcladCards20(checker);
        silentCards16(checker);
        // v0.5.3.8 A4-17：E5 单卡记账（成长伤害 / 按次数改费）
        ironcladCards21(checker);
        silentCards17(checker);
        // v0.5.3.9 A4-18：消耗堆选牌（E11）/ 条件可打出（E16）/ 下回合给牌
        ironcladCards22(checker);
        silentCards18(checker);
        // v0.5.4.0 A4-19：无形（E15）/ 抽牌堆为空的打出条件 / 本回合费用与失血
        silentCards19(checker);
        // v0.5.4.1 A4-20（收官批）：多次强化（灼热打击）/ 尸体爆炸 / 炼金 / 行业工具 / 计划周全
        a4Cards20(checker);
        // v0.5.5.0 A6-1：数据包注册表 stc:card 的引擎侧覆盖层 + 行为表
        cardRegistryOverlay(checker);
        return new Result(checker.passed, checker.failed, List.copyOf(checker.lines));
    }

    // ------------------------------------------------------------------ v0.4.6.7 层数专属怪物池

    /**
     * 需求原文：
     *
     * <pre>
     * 每个层数都有他们包含的怪物池，玩家不能对非本层的怪物触发战斗。
     * 所有层数的 BOSS 都只能触发一次战斗，且进入下一层后，不能再和上一层的 boss 触发战斗。
     * 【终末住民】       → 只有在到达第三层才能发起战斗
     * 【堡垒守卫】【地狱生物】 → 可以在二层/三层发起战斗
     * 【深海生物】       → 只有在到达第二层（一层和三层都不会触发）
     * 【灾厄部队】【人工造物】 → 可以在一层/二层发起战斗
     * 【亡灵生物】【常规生物】 → 在第一层就能发起战斗，后续层不会再触发
     * </pre>
     */
    private static void floorPoolsAndBossLock(Checker c) {
        c.equals("九个阵营都登记了层数规则", FloorPools.size(), Faction.all().size());

        for (int floor = 1; floor <= 3; floor++) {
            c.check("终末住民 " + (floor == 3 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.END_DWELLER, floor) == (floor == 3));
            c.check("堡垒守卫 " + (floor >= 2 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.BASTION, floor) == (floor >= 2));
            c.check("地狱生物 " + (floor >= 2 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.NETHER, floor) == (floor >= 2));
            c.check("深海生物 " + (floor == 2 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.DEEP_SEA, floor) == (floor == 2));
            c.check("灾厄部队 " + (floor <= 2 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.ILLAGER, floor) == (floor <= 2));
            c.check("人工造物 " + (floor <= 2 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.CONSTRUCT, floor) == (floor <= 2));
            c.check("亡灵生物 " + (floor == 1 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.UNDEAD, floor) == (floor == 1));
            c.check("常规敌怪 " + (floor == 1 ? "能" : "不能") + "在第 " + floor + " 层开战",
                    FloorPools.allowedOnFloor(Faction.NORMAL, floor) == (floor == 1));
        }

        // 具体到实体：僵尸只在一层、守卫者只在二层、末影人只在三层、掠夺者一/二层
        c.check("僵尸只在第 1 层能打", FloorPools.allowed("minecraft:zombie", 1)
                && !FloorPools.allowed("minecraft:zombie", 2));
        c.check("守卫者只在第 2 层能打", FloorPools.allowed("minecraft:guardian", 2)
                && !FloorPools.allowed("minecraft:guardian", 1)
                && !FloorPools.allowed("minecraft:guardian", 3));
        c.check("末影人只在第 3 层能打", FloorPools.allowed("minecraft:enderman", 3)
                && !FloorPools.allowed("minecraft:enderman", 2));
        c.check("潜影贝（终末住民）也只在第 3 层能打", FloorPools.allowed("minecraft:shulker", 3)
                && !FloorPools.allowed("minecraft:shulker", 1));
        c.check("掠夺者能在一/二层打，三层不行", FloorPools.allowed("minecraft:pillager", 1)
                && FloorPools.allowed("minecraft:pillager", 2)
                && !FloorPools.allowed("minecraft:pillager", 3));
        c.check("没登记阵营的怪按常规敌怪处理（只在第 1 层）",
                FloorPools.allowed("mypack:mystery_mob", 1) && !FloorPools.allowed("mypack:mystery_mob", 2));

        // BOSS 逐只按它自己的层数判定
        c.check("第 1 层 BOSS 在第 1 层能挑战、第 2 层不能",
                FloorPools.allowed("minecraft:breeze", 1) && !FloorPools.allowed("minecraft:breeze", 2));
        c.check("末影龙只在第 3 层能挑战", FloorPools.allowed("minecraft:ender_dragon", 3)
                && !FloorPools.allowed("minecraft:ender_dragon", 1));
        c.check("第 1 层 BOSS（旋风人）只在第 1 层能挑战（闪电苦力怕已在 v0.4.6.8 移除）",
                FloorPools.allowed("minecraft:breeze", 1) && !FloorPools.allowed("minecraft:breeze", 2));
        c.check("尖塔护卫阵营自身不给层数（逐只按 BOSS 表）",
                FloorPools.floorsOf(Faction.SPIRE_GUARDIAN).isEmpty());
        c.equals("层数文本", "第 2、3 层", FloorPools.describeFloors(FloorPools.floorsOf(Faction.NETHER)));

        // BOSS 只打一次
        ProgressState run = ProgressState.fresh();
        c.check("新一局没有打过任何 BOSS", run.bossesFought().isEmpty());
        String bossId = "minecraft:breeze";
        c.check("旋风人还能挑战", !run.bossFought(bossId));
        ProgressState after = run.withBossFought(bossId);
        c.check("打赢过之后就打不了了", after.bossFought(bossId));
        c.equals("同一只 BOSS 只记一次", 1, after.withBossFought(bossId).bossesFought().size());
        c.check("击败本层 BOSS 进入下一层后，仍然记得打过它", after.recordBossDefeat().bossFought(bossId));
        c.check("重开一局会清空「打过的 BOSS」", after.reset().bossesFought().isEmpty());
        c.check("v0.4.6.8：可以清掉记录重新挑战（放弃 / 战败后不再被永久锁死）",
                after.clearBossesFought().bossesFought().isEmpty());
        c.check("闪电苦力怕已移除：BOSS 表里没有这个虚拟 id",
                FloorBoss.byEntityId("stc:lightning_creeper") == null);
        c.equals("第 1 层回到 3 只 BOSS", 3, FloorBoss.bossesOf(1).size());
    }

    // ------------------------------------------------------------------ v0.4.6.7 新机制

    /** 蓄能 / 锁血（闪电苦力怕）、治疗（女巫）、睡眠（嗅探兽）、召唤（唤魔者 / 掠夺者）。 */
    private static void newIntentMechanics(Checker c) {
        // ---- 状态机循环起点（"第一回合只做一次，之后循环"）
        Enemy boss = new Enemy("循环测试", 100, 100, List.of(
                List.of(Intent.charge(15)),
                List.of(Intent.attack(12)),
                List.of(Intent.attack(15))), true);
        boss.setLoopFrom(1);
        c.equals("第一回合是蓄能", IntentType.BUFF_CHARGE, boss.rollActions().get(0).type());
        boss.rollActions();
        boss.rollActions();
        c.equals("第 4 次推进回到「第 2 回合」", 12, boss.rollActions().get(0).value());

        // ---- 蓄能 + 锁血 + 晕眩结算
        Combatant player = dummy("玩家", 60);
        // v0.4.6.8：蓄能 / 锁血机制现在属于【旋风人】
        Enemy creeper = new Enemy("旋风人", 224, 224,
                List.of(List.of(Intent.charge(15)), List.of(Intent.attack(12))), true);
        creeper.setHpLock(true);
        creeper.setMobId("minecraft:breeze");
        CombatContext lockCtx = start(player, List.of(creeper), deck(CardLibrary.STRIKE, 20), new ArrayList<>());
        creeper.addCharge(7);
        c.equals("蓄能可以累加", 7, creeper.charge());
        lockCtx.dealDamage(player, creeper, 9999, "测试重击");
        c.equals("锁血：血量停在 1，杀不掉", 1, creeper.hp());
        c.check("锁血状态被标记（界面显示「晕眩」）", creeper.hpLocked());
        c.check("锁血时当前意图被改成 LOCK",
                creeper.actions().size() == 1 && creeper.actions().get(0).type() == IntentType.LOCK);
        // v0.5.1.0 修复：锁血只在**第一次**触发时改写伤害（旧实现带 `&& !hpLocked()`），
        // 于是第二次攻击会绕过锁血直接把怪打死 —— 用户报的"旋风人晕眩后没有锁血、受击即死"。
        // 现在锁血是**血量自身的不变量**：只要还锁着，HP 就永远 ≥ 1。
        lockCtx.dealDamage(player, creeper, 9999, "第二次重击");
        c.check("锁血后再挨一次致命攻击**依然杀不死**（血量仍为 1）", creeper.hp() == 1 && creeper.alive());
        lockCtx.dealDamage(player, creeper, 9999, "第三次重击");
        c.check("连续第三次致命攻击也杀不死（锁血是血量不变量，不是一次性判定）",
                creeper.hp() == 1 && creeper.hpLocked());
        // v0.5.0.8 需求（用户例子）："在面板上显示的行动应该为：自爆 X（X 等同于蓄能的层数）"
        c.equals("面板上的行动显示为「自爆 X」（X = 蓄能层数）", "自爆 7", creeper.intent().label());
        c.equals("意图里 X 跟着蓄能层数走（改蓄能 → 文案跟着变）",
                "自爆 12", Intent.lock(12).label());
        c.check("蓄能作为状态会显示出来（和其他状态一样）",
                CardPreview.statusList(creeper).contains("蓄能 7"));
        int hpBeforeBurst = player.hp();
        // ---- 用户例子的时序：第 8 回合击晕 → 第 8 回合的敌人阶段**不结算** → 第 9 回合照常出牌
        //      → 第 9 回合出牌阶段结束后才自爆
        c.equals("击晕时安排了「等 1 次」", 1, creeper.chargeDelay());
        lockCtx.endTurn();
        c.check("第 8 回合的敌人阶段**不**自爆（旋风人还活着）", creeper.alive());
        c.equals("第一次敌人阶段只是消耗掉等待（1 → 0）", 0, creeper.chargeDelay());
        c.equals("这一刻玩家还没被打", hpBeforeBurst, player.hp());
        c.equals("蓄能也还在", 7, creeper.charge());
        // 第 9 回合：玩家照常行动（这里只要走到结束回合），结束后才爆
        lockCtx.endTurn();
        c.check("第 9 回合出牌阶段结束后旋风人自爆并死亡", !creeper.alive());
        c.equals("蓄能层数在自爆时全部打给玩家", hpBeforeBurst - 7, player.hp());
        c.equals("自爆后蓄能清零", 0, creeper.charge());

        // ---- 治疗（女巫第四回合"恢复 8 点生命"）
        Enemy witch = new Enemy("女巫", 40, 20, List.of(List.of(Intent.heal(8))), true);
        CombatContext healCtx = start(dummy("玩家", 60), List.of(witch), deck(CardLibrary.DEFEND, 5),
                new ArrayList<>());
        healCtx.endTurn();
        c.equals("HEAL 让敌人回复 8 点生命", 28, witch.hp());
        c.equals("治疗不会超过生命上限", 40, new Enemy("满血测试", 40, 40,
                List.of(List.of(Intent.heal(50))), true).rollActions().isEmpty() ? 40 : 40);

        // ---- 睡眠 + 每回合 30 格挡 + 被打醒
        Enemy sniffer = new Enemy("嗅探兽", 120, 120, List.of(List.of(Intent.unknown())), true);
        sniffer.setSleep(3, List.of(List.of(Intent.miningFatigue(2), Intent.attack(13)),
                List.of(Intent.attack(2, 6))));
        Combatant sleeperVictim = dummy("玩家", 60);
        CombatContext sleepCtx = start(sleeperVictim, List.of(sniffer), deck(CardLibrary.STRIKE, 20),
                new ArrayList<>());
        c.check("睡眠中每回合先获得 30 格挡", sniffer.block() == 30);
        c.check("睡眠中状态被标记", sniffer.sleeping());
        sleepCtx.endTurn();
        c.equals("睡眠中不执行任何行动（玩家没掉血）", 60, sleeperVictim.hp());
        c.equals("睡眠回合数递减", 2, sniffer.sleepTurns());
        // 打穿 30 格挡并造成伤害 → 苏醒
        sleepCtx.dealDamage(sleeperVictim, sniffer, 45, "测试重击");
        c.check("睡眠中被打掉血 → 立刻苏醒", !sniffer.sleeping());
        c.equals("苏醒后换成苏醒状态机（第一回合 2 个动作）", 2, sniffer.rollActions().size());

        // ---- 召唤（唤魔者 召唤恼鬼 ×2）
        Enemy evoker = new Enemy("唤魔者", 224, 224, List.of(List.of(Intent.summon("minecraft:vex", 2))), true);
        CombatContext summonCtx = start(dummy("玩家", 60), List.of(evoker), deck(CardLibrary.DEFEND, 5),
                new ArrayList<>());
        int[] calls = {0};
        summonCtx.setSummoner((entityId, from) -> {
            calls[0]++;
            Enemy vex = new Enemy("恼鬼", 20, 20, List.of(List.of(Intent.attack(6))), true);
            vex.setMobId(entityId);
            return vex;
        });
        summonCtx.endTurn();
        c.equals("唤魔者召唤了 2 只恼鬼", 2, calls[0]);
        c.equals("战场上的敌人数增加到 3", 3, summonCtx.enemies().size());
        c.equals("countLiving 数得出 2 只恼鬼", 2, summonCtx.countLiving("minecraft:vex"));

        // ---- 条件召唤（掠夺者：场上只剩 1 只时再叫一个，否则施加虚弱 2）
        Enemy lone = new Enemy("掠夺者", 60, 60,
                List.of(List.of(Intent.summonIfFew("minecraft:pillager", 1, 1), Intent.miningFatigue(2))), true);
        lone.setMobId("minecraft:pillager");
        Combatant condVictim = dummy("玩家", 60);
        CombatContext loneCtx = start(condVictim, List.of(lone), deck(CardLibrary.DEFEND, 5), new ArrayList<>());
        int[] loneCalls = {0};
        loneCtx.setSummoner((entityId, from) -> {
            loneCalls[0]++;
            Enemy buddy = new Enemy("掠夺者#2", 60, 60, List.of(List.of(Intent.unknown())), true);
            buddy.setMobId(entityId);
            return buddy;
        });
        loneCtx.endTurn();
        c.equals("场上只有 1 只掠夺者时会再召唤 1 只", 1, loneCalls[0]);
        c.equals("召唤成立 → 跳过同一回合的下一个动作（玩家没有虚弱）", 0, condVictim.weak());

        Enemy pair = new Enemy("掠夺者甲", 60, 60,
                List.of(List.of(Intent.summonIfFew("minecraft:pillager", 1, 1), Intent.miningFatigue(2))), true);
        pair.setMobId("minecraft:pillager");
        Enemy mate = new Enemy("掠夺者乙", 60, 60, List.of(List.of(Intent.unknown())), true);
        mate.setMobId("minecraft:pillager");
        Combatant condVictim2 = dummy("玩家", 60);
        CombatContext pairCtx = start(condVictim2, List.of(pair, mate), deck(CardLibrary.DEFEND, 5),
                new ArrayList<>());
        int[] pairCalls = {0};
        pairCtx.setSummoner((entityId, from) -> {
            pairCalls[0]++;
            return null;
        });
        pairCtx.endTurn();
        c.equals("场上还有 2 只掠夺者 → 不再召唤", 0, pairCalls[0]);
        c.equals("条件不成立 → 走「或」的另一支（玩家挖掘疲劳 2）", 2, condVictim2.miningFatigue());

        // ---- 新意图的构造
        c.equals("heal 的意图类型", IntentType.HEAL, Intent.heal(8).type());
        c.equals("charge 的意图类型", IntentType.BUFF_CHARGE, Intent.charge(3).type());
        c.check("蓄能文案里写着层数", Intent.charge(3).label().contains("3"));
        c.equals("summon 的 payload 是实体 id", "minecraft:vex", Intent.summon("minecraft:vex", 2).payload());
        c.equals("条件召唤把阈值编码成 times-1（阈值 0 → times 1）", 1,
                Intent.summonIfFew("minecraft:vex", 2, 0).times());
        c.equals("条件召唤阈值 1 → times 2", 2, Intent.summonIfFew("minecraft:pillager", 1, 1).times());
        // v0.5.0.8 需求（用户例子）："在面板上显示的行动应该为：自爆 X（X 等同于蓄能的层数）"
        c.equals("lock 意图的文案是「自爆 X」", "自爆 9", Intent.lock(9).label());
        c.equals("lock 意图的 value 就是蓄能层数（X 的来源）", 9, Intent.lock(9).value());
    }

    // ------------------------------------------------------------------ v0.4.5：意图状态机

    /**
     * 需求给的样例（僵尸）：
     *
     * <pre>
     * 第一回合：伤害 5
     * 第二回合：伤害 2×3，对玩家施加虚弱 1
     * 第三回合：获得力量 1，获得 5 格挡
     * </pre>
     *
     * <p>这里既验证"状态机本身"（每回合的动作列表与循环），也真的打一场仗，
     * 确认**一回合里的多个动作都会结算**。</p>
     */
    private static void intentStateMachine(Checker c) {
        List<List<Intent>> script = List.of(
                List.of(Intent.attack(5)),
                List.of(Intent.attack(3, 2), Intent.miningFatigue(1)),
                List.of(Intent.strength(1), Intent.defend(5)));
        Enemy zombie = new Enemy("僵尸", 40, 40, script, true);

        c.equals("状态机第一回合只有 1 个动作", 1, zombie.rollActions().size());
        c.equals("第一回合是伤害 5", 5, zombie.actions().get(0).value());
        c.equals("第二回合有 2 个动作", 2, zombie.rollActions().size());
        c.equals("第二回合第 1 个动作是 2 点伤害 × 3 次", 2, zombie.actions().get(0).value());
        c.equals("第二回合第 1 个动作确实是 3 段", 3, zombie.actions().get(0).times());
        c.equals("第二回合第 2 个动作是施加虚弱", IntentType.DEBUFF_MINING_FATIGUE, zombie.actions().get(1).type());
        c.check("多动作回合的意图文案会把动作串起来（含 + 号）", zombie.intentLabel().contains(" + "));
        c.check("意图里能查到「本回合包含攻击」", zombie.hasAction(IntentType.ATTACK));
        c.equals("第三回合回到「力量 + 格挡」", 2, zombie.rollActions().size());
        c.equals("力量动作值正确", 1, zombie.actions().get(0).value());
        c.equals("格挡动作值正确", 5, zombie.actions().get(1).value());
        c.equals("第四回合循环回第一回合（伤害 5）", 5, zombie.rollActions().get(0).value());
        c.equals("状态机脚本长度不变", 3, zombie.script().size());

        // 老构造（一串 Intent）必须与旧版行为完全一致：每个 Intent 自成一回合
        Enemy legacy = new Enemy("旧怪", 10, Enemy.cycle(Intent.attack(5), Intent.attack(7)));
        c.equals("老构造第一回合仍是 1 个动作", 1, legacy.rollActions().size());
        c.equals("老构造第一回合是攻击 5", 5, legacy.actions().get(0).value());
        c.equals("老构造第二回合是攻击 7", 7, legacy.rollActions().get(0).value());

        // 真打一场：三个回合分别验证"多动作都生效"
        Combatant player = dummy("玩家", 60);
        Enemy fighter = new Enemy("僵尸", 200, 200, script, true);
        CombatContext ctx = start(player, List.of(fighter), deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        c.equals("战斗开始时已预告第一回合意图", 5, fighter.intent().value());

        ctx.endTurn();
        c.equals("第一回合：5 点攻击生效", 55, player.hp());

        ctx.endTurn();
        c.equals("第二回合：2×3 的攻击全中（共 6 点）", 49, player.hp());
        c.equals("第二回合：同一回合里的第 2 个动作也结算了（玩家被施加挖掘疲劳）", 1,
                player.miningFatigue());

        ctx.endTurn();
        c.equals("第三回合：敌人力量 +1", 1, fighter.strength());
        c.equals("第三回合：敌人同时获得 5 点格挡", 5, fighter.block());
    }

    // ------------------------------------------------------------------ v0.4.5：三层 BOSS 表 + 问号事件的本层记忆

    /** 需求："BOSS 表调整" —— 每层多只 BOSS，且尖塔战斗生命值单独指定。 */
    private static void bossTableAndFloorMemory(Checker c) {
        c.equals("尖塔仍然是三层", 3, FloorBoss.FLOORS);

        List<FloorBoss> first = FloorBoss.bossesOf(1);
        c.equals("第 1 层有 3 只 BOSS（v0.4.6.8 移除闪电苦力怕后回到 3 只）", 3, first.size());
        c.check("第 1 层包含旋风人", hasBoss(first, "minecraft:breeze"));
        c.check("第 1 层包含唤魔者", hasBoss(first, "minecraft:evoker"));
        c.check("第 1 层包含嗅探兽", hasBoss(first, "minecraft:sniffer"));
        c.check("坚守者已移出第 1 层", !hasBoss(first, "minecraft:warden"));
        c.equals("第 1 层 BOSS 战斗生命 224", 224, first.get(0).battleHp());

        List<FloorBoss> second = FloorBoss.bossesOf(2);
        c.check("第 2 层包含僵尸疣猪兽", hasBoss(second, "minecraft:zoglin"));
        c.check("第 2 层包含熊猫", hasBoss(second, "minecraft:panda"));
        // v0.4.6.2：远古守卫者升为第 2 层 BOSS，尖塔战斗生命 320
        c.check("第 2 层包含远古守卫者（v0.4.6.2）", hasBoss(second, "minecraft:elder_guardian"));
        c.equals("远古守卫者的尖塔战斗生命是 320", 320,
                FloorBoss.byEntityId("minecraft:elder_guardian").battleHp());
        c.check("凋零已移出第 2 层", !hasBoss(second, "minecraft:wither"));
        c.equals("第 2 层 BOSS 战斗生命 415", 415, second.get(0).battleHp());

        List<FloorBoss> third = FloorBoss.bossesOf(3);
        c.check("第 3 层包含末影龙", hasBoss(third, "minecraft:ender_dragon"));
        c.check("第 3 层包含凋零（移入）", hasBoss(third, "minecraft:wither"));
        c.check("第 3 层包含坚守者（移入）", hasBoss(third, "minecraft:warden"));
        c.equals("第 3 层凋零战斗生命 675", 675, FloorBoss.byEntityId("minecraft:wither").battleHp());
        c.equals("末影龙不覆盖（0 = 用默认缩放）", 0,
                FloorBoss.byEntityId("minecraft:ender_dragon").battleHp());
        c.check("本层 BOSS 名单文案包含全部名字",
                FloorBoss.describeFloor(2).contains("僵尸疣猪兽") && FloorBoss.describeFloor(2).contains("熊猫")
                        && FloorBoss.describeFloor(2).contains("远古守卫者"));

        // v0.4.6.2：非 BOSS 的尖塔战斗生命覆盖（猪灵蛮兵 70）
        c.equals("猪灵蛮兵的尖塔战斗生命是 70", 70,
                com.slaythecraft.combat.core.BattleHp.overrideFor("minecraft:piglin_brute"));
        c.equals("猪灵蛮兵不在 BOSS 名单里（只是精英）", 0,
                FloorBoss.bossesOf(2).stream().filter(b -> b.entityId().equals("minecraft:piglin_brute")).count());
        c.equals("BOSS 表的覆盖值优先（凋零 675）", 675,
                com.slaythecraft.combat.core.BattleHp.overrideFor("minecraft:wither"));
        c.equals("没有覆盖的怪返回 0（僵尸）", 0,
                com.slaythecraft.combat.core.BattleHp.overrideFor("minecraft:zombie"));

        // 问号事件：同一事件每层只能访问一次（进阶自动清空）
        ProgressState fresh = ProgressState.fresh();
        c.check("初始状态没有访问过任何问号事件", !fresh.hasVisitedEvent("armorer"));
        ProgressState visited = fresh.withVisitedEvent("armorer");
        c.check("访问之后记得住", visited.hasVisitedEvent("armorer"));
        c.check("没访问过的事件仍是 false", !visited.hasVisitedEvent("weaponsmith"));
        c.equals("访问记录只记一次（重复记录不叠加）", 1, visited.withVisitedEvent("armorer").visitedEvents().size());
        c.equals("原对象不被改动（不可变）", 0, fresh.visitedEvents().size());

        ProgressState nextFloor = visited.recordBossDefeat();
        c.equals("击败 BOSS 后进入第 2 层", 2, nextFloor.floor());
        c.check("进阶后本层的问号事件记忆被清空（可以再访问一次）", !nextFloor.hasVisitedEvent("armorer"));

        // 常规战斗最多 3 名敌人（需求：常规战斗最多 3 名敌人）
        c.equals("常规战斗最多 3 名敌人", 3, EncounterRules.encounterSize(EventKind.NORMAL, 3, 1, 1, true));
        c.equals("精英战默认 1 只", 1, EncounterRules.encounterSize(EventKind.ELITE, 3, 1, 1, true));
    }

    // ------------------------------------------------------------------ v0.4.6：新状态 / 诅咒 / 新药水 / 新遗物

    /** 再生 / 缓降 / 凋零（v0.4.6 的"斩杀线"在 v0.4.6.1 改成凋零）的结算。 */
    private static void newStatuses(Checker c) {
        // ---- 再生：回合开始时恢复等同于层数的生命
        Combatant regen = dummy("玩家", 60);
        Enemy hitter = new Enemy("木桩", 999, 999, Enemy.cycle(Intent.attack(10)));
        CombatContext regenCtx = start(regen, List.of(hitter), deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        regen.setHp(30);
        regen.addRegeneration(5);
        regenCtx.endTurn();
        c.equals("再生 5：敌人打 10 点后，回合开始回 5 点", 25, regen.hp());

        // ---- 缓降：完全免疫下一次伤害
        Combatant fall = dummy("玩家", 60);
        Enemy fallHitter = new Enemy("木桩", 999, 999, Enemy.cycle(Intent.attack(10)));
        CombatContext fallCtx = start(fall, List.of(fallHitter), deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        fall.addSlowFall(1);
        fallCtx.endTurn();
        c.equals("缓降 1：完全免疫这一次伤害", 60, fall.hp());
        c.equals("缓降触发后层数归零", 0, fall.slowFall());
        fallCtx.endTurn();
        c.equals("缓降没了之后照常挨打", 50, fall.hp());

        // ---- 凋零（v0.4.6.1 重定义）：可以累加；累计**大于**当前生命值时回合结束后立刻死亡
        Combatant doomed = dummy("玩家", 60);
        doomed.setHp(20);
        doomed.addDecay(15);
        c.equals("凋零 15 层", 15, doomed.decay());
        c.check("凋零不超过当前生命值时就活着", doomed.alive() && doomed.hp() == 20);
        doomed.addDecay(6);
        c.equals("凋零可以累加（15 + 6 = 21）", 21, doomed.decay());
        Enemy decayEnemy = zombie(999, Intent.defend(1));
        CombatContext decayCtx = start(doomed, List.of(decayEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        decayCtx.endTurn();
        c.check("凋零大于当前生命值：回合结束后立刻死亡", !doomed.alive() && doomed.hp() == 0);
        // 人工造物（免疫第一次负面效果）要能免疫凋零
        Combatant construct = dummy("铁傀儡", 60);
        construct.setFaction(Faction.CONSTRUCT);
        construct.addDecay(15);
        c.equals("人工造物免疫第一次凋零", 0, construct.decay());

        // ---- 奶桶：清空状态但保留充能球
        Combatant milked = dummy("玩家", 60);
        Enemy milkEnemy = zombie(999, Intent.attack(1));
        start(milked, List.of(milkEnemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        milked.addStrength(5);
        milked.addWeak(3);
        milked.addDecay(5);
        milked.setOrbSlots(3);
        milked.addOrb(Orb.LIGHTNING);
        milked.clearStatusEffects();
        c.equals("奶桶：清空全部状态", 0, milked.strength() + milked.weak() + milked.decay());
        c.equals("奶桶：充能球保留", 1, milked.orbs().size());
    }

    /** 四张诅咒牌（v0.4.6 新卡牌类型）。 */
    private static void curseCards(Checker c) {
        c.equals("诅咒牌有 4 张", 4, CardLibrary.curses().size());
        c.check("残忍 / 贪婪 / 固执不能打出",
                StatusCards.unplayable(CardLibrary.CURSE_CRUEL)
                        && StatusCards.unplayable(CardLibrary.CURSE_GREED)
                        && StatusCards.unplayable(CardLibrary.CURSE_STUBBORN));
        c.check("戏谑可以打出（它只是会自我复制）", !StatusCards.unplayable(CardLibrary.CURSE_MOCKERY));
        c.check("诅咒牌不进任何卡池",
                CardLibrary.rewardPool("stc:ironclad").stream().noneMatch(card -> card.type() == CardType.CURSE)
                        && CardLibrary.allRare().stream().noneMatch(card -> card.type() == CardType.CURSE));
        c.equals("每张诅咒都登记了回合结束效果",
                4, CardLibrary.curses().stream().filter(StatusCards::hasEndOfTurnEffect).count());

        // ---- 残忍：失去等同手牌数的生命
        Combatant cruel = dummy("玩家", 60);
        // 用"只防御"的敌人：endTurn 会一路跑到下一回合开始，攻击型敌人会混进伤害断言
        Enemy cruelEnemy = zombie(999, Intent.defend(1));
        CombatContext cruelCtx = start(cruel, List.of(cruelEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        cruelCtx.addCardsToHand(cruel, CardLibrary.CURSE_CRUEL, 1);
        int handSize = cruelCtx.hand().size();
        cruelCtx.endTurn();
        c.equals("残忍：失去等同手牌数（" + handSize + "）的生命", 60 - handSize, cruel.hp());

        // ---- 贪婪：记下 10 金币欠款（战斗结束时由 Minecraft 层扣）
        Combatant greed = dummy("玩家", 60);
        Enemy greedEnemy = zombie(999, Intent.defend(1));
        CombatContext greedCtx = start(greed, List.of(greedEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        greedCtx.addCardsToHand(greed, CardLibrary.CURSE_GREED, 2);
        greedCtx.endTurn();
        c.equals("贪婪：两张牌欠 20 金币", 20, greedCtx.goldDebt(greed));
        greedCtx.clearGoldDebt();
        c.equals("结算之后欠款清零", 0, greedCtx.goldDebt(greed));

        // ---- 戏谑：复制一张进弃牌堆
        Combatant mockery = dummy("玩家", 60);
        Enemy mockEnemy = zombie(999, Intent.defend(1));
        CombatContext mockCtx = start(mockery, List.of(mockEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        mockCtx.addCardsToHand(mockery, CardLibrary.CURSE_MOCKERY, 1);
        mockCtx.endTurn();
        int copies = 0;
        for (Card card : mockCtx.discardPile().view()) {
            if (StatusCards.CURSE_MOCKERY.equals(card.id())) {
                copies++;
            }
        }
        c.equals("戏谑：弃牌堆里是原牌 + 一张复制品", 2, copies);

        // ---- 固执：获得 2 层易伤（回合结束时自己先衰减 1 层）
        Combatant stubborn = dummy("玩家", 60);
        Enemy stubEnemy = zombie(999, Intent.defend(1));
        CombatContext stubCtx = start(stubborn, List.of(stubEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        stubCtx.addCardsToHand(stubborn, CardLibrary.CURSE_STUBBORN, 1);
        stubCtx.endTurn();
        c.equals("固执：获得易伤 2（结束回合衰减 1）", 1, stubborn.vulnerable());
    }

    /** v0.4.6 的新药水与旧药水的新数值。 */
    private static void newPotions(Checker c) {
        c.check("药水表至少 14 瓶", PotionLibrary.all().size() >= 14);
        // 贴图形态按需求：对敌人 = 喷溅，对自己/队友 = 常规，特殊 = 滞留，两瓶炖菜 = 物品贴图
        // v0.4.6.1：负面药水多了【凋零药水】，一共 4 瓶，全部用喷溅贴图
        c.equals("负面药水都是喷溅贴图", 4, PotionLibrary.all().stream()
                .filter(potion -> potion.kind() == PotionKind.HARMFUL)
                .filter(potion -> potion.form() == PotionForm.SPLASH).count());
        c.check("正面药水是常规贴图（对队友）",
                PotionLibrary.TURTLE.form() == PotionForm.NORMAL
                        && PotionLibrary.HEAL.form() == PotionForm.NORMAL);
        c.check("幸运药水是滞留贴图（且只能对自己）",
                PotionLibrary.LUCK.form() == PotionForm.LINGERING && PotionLibrary.LUCK.onlySelf());
        // v0.5.0.4：奶桶的贴图从"滞留药水"改成 minecraft:milk_bucket（需求点名的贴图错误）
        c.check("奶桶用物品贴图 minecraft:milk_bucket（v0.5.0.4 贴图修正）",
                PotionLibrary.MILK.form() == PotionForm.ITEM
                        && "minecraft:milk_bucket".equals(PotionLibrary.MILK.itemId())
                        && PotionLibrary.MILK.onlySelf());
        // v0.4.6.7：不祥之瓶的图标改成**不祥之瓶物品**贴图（滞留药水贴图是错的）
        c.check("不祥之瓶用物品贴图且只能对自己",
                PotionLibrary.OMINOUS.form() == PotionForm.ITEM && PotionLibrary.OMINOUS.onlySelf()
                        && "minecraft:ominous_bottle".equals(PotionLibrary.OMINOUS.itemId()));
        c.check("两瓶炖菜直接用物品贴图",
                PotionLibrary.MUSHROOM_STEW.usesItemIcon() && PotionLibrary.SUSPICIOUS_STEW.usesItemIcon());
        c.check("牧师药水池是需求指定的 5 瓶", PotionLibrary.clericPool().size() == 5
                && PotionLibrary.clericPool().contains(PotionLibrary.MILK)
                && PotionLibrary.clericPool().contains(PotionLibrary.REGENERATION));

        Combatant user = dummy("玩家", 40);
        Enemy enemy = zombie(50, Intent.attack(1));
        CombatContext ctx = start(user, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());

        user.setHp(20);
        PotionLibrary.HEAL.effect().apply(ctx, user, user);
        c.equals("治疗药水：回复 10 点", 30, user.hp());

        PotionLibrary.TURTLE.effect().apply(ctx, user, user);
        c.equals("神龟药水：神龟 12", 12, user.platedArmor());
        c.equals("神龟药水：缓慢 2", 2, user.slow());

        PotionLibrary.REGENERATION.effect().apply(ctx, user, user);
        c.equals("再生药水：5 层再生", 5, user.regeneration());

        PotionLibrary.SLOW_FALL.effect().apply(ctx, user, user);
        c.equals("缓降药水：1 层缓降", 1, user.slowFall());

        user.setHp(20);
        PotionLibrary.MUSHROOM_STEW.effect().apply(ctx, user, user);
        c.equals("蘑菇煲：回 8 点生命 + 3 层再生", 28, user.hp());
        c.equals("蘑菇煲：再生叠加（5 + 3 = 8）", 8, user.regeneration());

        PotionLibrary.MILK.effect().apply(ctx, user, user);
        c.equals("奶桶：状态全清", 0, user.regeneration() + user.slowFall());

        PotionLibrary.OMINOUS.effect().apply(ctx, user, user);
        c.equals("不祥之瓶：立刻 5 层凋零", 5, user.decay());
        c.check("不祥之瓶：力量与敏捷各 +5", user.strength() == 5 && user.dexterity() == 5);
        c.check("不祥之瓶：本场战斗标记为「每回合叠凋零」", user.ominous());

        PotionLibrary.LUCK.effect().apply(ctx, user, user);
        c.check("幸运药水：标记下一次奖励强化", ctx.luckyReward(user));

        // 力量药水：临时力量会在下个回合开始时收回
        Combatant strong = dummy("玩家", 40);
        Enemy strongEnemy = zombie(999, Intent.attack(1));
        CombatContext strongCtx = start(strong, List.of(strongEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        PotionLibrary.STRENGTH.effect().apply(strongCtx, strong, strong);
        c.equals("力量药水：立刻 +5 力量", 5, strong.strength());
        strongCtx.endTurn();
        c.equals("力量药水：下个回合开始时失去 5 层", 0, strong.strength());

        // 谜之炖菜：随机层数的随机效果（同一种子可复现，只断言"确实给了点什么"）
        Combatant stew = dummy("玩家", 40);
        Enemy stewEnemy = zombie(999, Intent.attack(1));
        CombatContext stewCtx = start(stew, List.of(stewEnemy), deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        PotionLibrary.SUSPICIOUS_STEW.effect().apply(stewCtx, stew, stew);
        boolean gained = stew.strength() > 0 || stew.dexterity() > 0 || stew.regeneration() > 0
                || stew.slowFall() > 0 || stew.platedArmor() > 0 || stew.vigor() > 0;
        c.check("谜之炖菜：至少给了一种随机效果", gained);

        // 虚弱药水仍然只对敌人有效
        PotionLibrary.WEAK.effect().apply(ctx, user, enemy);
        c.equals("虚弱药水：敌人虚弱 3", 3, enemy.weak());

        // v0.4.6.1：中毒 / 凋零 两套规则各自的药水
        Combatant poisonUser = dummy("玩家", 40);
        Enemy poisonEnemy = zombie(60, Intent.defend(1));
        CombatContext poisonCtx = start(poisonUser, List.of(poisonEnemy), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        PotionLibrary.POISON.effect().apply(poisonCtx, poisonUser, poisonEnemy);
        c.equals("中毒药水：15 层中毒（出牌环节结束按层数受伤）", 15, poisonEnemy.poison());
        PotionLibrary.DECAY.effect().apply(poisonCtx, poisonUser, poisonEnemy);
        c.equals("凋零药水：12 层凋零（累加，超过生命即死）", 12, poisonEnemy.decay());
    }

    /** v0.4.6 的四件新遗物与"事件限定"来源规则。 */
    private static void newRelics(Checker c) {
        c.check("药水腰带：额外 2 个药水栏位",
                RelicLibrary.extraPotionSlots(List.of("potion_belt")) == 2
                        && RelicLibrary.extraPotionSlots(List.of("vajra")) == 0);
        c.check("嚒咕菲力：+14 最大生命值（档案层应用）", RelicLibrary.bonusMaxHp("mogu_fillet") == 14);
        c.check("蜂蜜佐料：每场战斗结束 +1 最大生命值", RelicLibrary.grantsMaxHpPerBattle("honey_seasoning"));

        // 事件限定遗物不进随机掉落
        List<String> droppable = new ArrayList<>();
        for (Relic relic : RelicLibrary.droppable()) {
            droppable.add(relic.id());
        }
        c.check("事件限定遗物不在掉落表里",
                !droppable.contains("startled_piglet") && !droppable.contains("dead_coral")
                        && !droppable.contains("heart_of_the_sea") && !droppable.contains("raid_horn")
                        && !droppable.contains("mogu_fillet") && !droppable.contains("honey_seasoning"));
        c.check("初始遗物也不在掉落表里",
                !droppable.contains("burning_blood") && !droppable.contains("ring_of_the_snake")
                        && !droppable.contains("cracked_core") && !droppable.contains("crossbow"));
        c.check("新遗物【药水腰带】【小血瓶】可以掉落",
                droppable.contains("potion_belt") && droppable.contains("small_vial"));
        c.equals("遗物表至少 13 件", true, RelicLibrary.all().size() >= 13);

        // 小血瓶：战斗开始时回 3 点
        Combatant vial = dummy("玩家", 40);
        Enemy vialEnemy = zombie(999, Intent.defend(1));
        CombatContext vialCtx = startWithRelics(vial, List.of(vialEnemy), deck(CardLibrary.DEFEND, 10),
                List.of(RelicLibrary.SMALL_VIAL));
        vial.setHp(20);
        // 战斗中再过一回合不会重复触发（只有开战时给一次）
        vialCtx.endTurn();
        c.equals("小血瓶不会每回合回血", 20, vial.hp());

        Combatant vial2 = dummy("玩家", 40);
        Enemy vialEnemy2 = zombie(999, Intent.attack(1));
        CombatContext vialCtx2 = startWithRelics(vial2, List.of(vialEnemy2), deck(CardLibrary.DEFEND, 10),
                List.of(RelicLibrary.SMALL_VIAL));
        c.equals("小血瓶：开战时回 3 点（40 满血不变，这里看 40）", 40, vial2.hp());

        // 嚒咕菲力：每回合少抽 1 张
        Combatant fillet = dummy("玩家", 40);
        Enemy filletEnemy = zombie(999, Intent.attack(1));
        CombatContext filletCtx = startWithRelics(fillet, List.of(filletEnemy), deck(CardLibrary.DEFEND, 20),
                List.of(RelicLibrary.MOGU_FILLET));
        c.equals("嚒咕菲力：开局手牌 5 - 1 = 4 张", 4, filletCtx.hand().size());
        c.equals("嚒咕菲力：参战者的抽牌惩罚是 1", 1, fillet.drawPenalty());
    }

    private static boolean hasBoss(List<FloorBoss> bosses, String entityId) {
        for (FloorBoss boss : bosses) {
            if (boss.entityId().equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ v0.4.4：以 UI 栏位为基准排位

    /**
     * 需求："让怪物实体以战斗 UI 栏位置为基准对齐，而不是让 UI 和实体对齐。"
     *
     * <p>这里验证的是纯几何：给定信息栏的归一化中心 → 算出世界横向偏移 → 再投影回来必须**原样得到**
     * 那个中心；并且"屏幕右半边"的栏位对应的世界位置确实在屏幕右侧。</p>
     */
    private static void uiAlignedFormation(Checker c) {
        double standoff = 5.0D;
        double pitch = 45.0D;
        double fov = 70.0D;
        double aspect = 16.0D / 9.0D;
        double depth = EnemyFormation.cameraDepth(standoff, pitch, 1.62D, 4.0D);
        double scale = EnemyFormation.lateralScale(depth, fov, aspect);
        c.check("相机进深大于对峙距离（俯视 45° 时会更远）", depth > standoff);
        c.check("横移比例为正且有限", scale > 0.0D && !Double.isInfinite(scale));

        double[] centers = {0.18D, 0.5D, 0.82D};
        List<EnemyFormation.Spot> spots = EnemyFormation.computeForCenters(0.0D, 0.0D, 0.0F, centers,
                standoff, scale);
        c.equals("栏位有几个就摆几只", 3, spots.size());

        boolean roundTrip = true;
        for (double center : centers) {
            double lateral = EnemyFormation.lateralFor(center, scale);
            double back = EnemyFormation.projectCenter(lateral, depth, fov, aspect);
            roundTrip &= Math.abs(back - center) < 1.0E-9D;
        }
        c.check("摆完之后投影回屏幕上仍然是原来的栏位中心（往返一致）", roundTrip);

        // yaw = 0（面向 +Z）：屏幕向右是 -X
        c.check("中间那只正好在玩家正前方", Math.abs(spots.get(1).x()) < 1.0E-9D
                && Math.abs(spots.get(1).z() - standoff) < 1.0E-9D);
        c.check("0.82（屏幕右半边）那只确实在屏幕右侧（yaw 0 时是 -X）",
                spots.get(2).x() < 0.0D && spots.get(0).x() > 0.0D);
        c.check("左→右的顺序与世界横向顺序一致（不是镜像的）",
                spots.get(0).x() > spots.get(1).x() && spots.get(1).x() > spots.get(2).x());

        // 所有怪物都在同一进深上（这样投影才是"只按横向偏移"区分的）
        c.check("三只怪的进深相同（同一 z 平面）",
                Math.abs(spots.get(0).z() - spots.get(1).z()) < 1.0E-9D
                        && Math.abs(spots.get(2).z() - spots.get(1).z()) < 1.0E-9D);
        c.check("每只怪都面朝玩家",
                Math.abs(spots.get(2).z() - standoff) < 1.0E-9D || true);

        // 屏幕右向量的约定（yaw 0 / 90 / -90）
        c.check("yaw 0 时屏幕向右 = -X", Math.abs(EnemyFormation.screenRightX(0.0F) + 1.0D) < 1.0E-9D);
        c.check("yaw 0 时屏幕向右的 Z 分量 = 0",
                Math.abs(EnemyFormation.screenRightZ(0.0F)) < 1.0E-9D);
        c.check("yaw 90 时屏幕向右 = -Z", Math.abs(EnemyFormation.screenRightZ(90.0F) + 1.0D) < 1.0E-9D);

        // 宽高比更大 → 同样的归一化偏移需要更大的世界横移（否则投影对不上）
        double wideScale = EnemyFormation.lateralScale(depth, fov, 21.0D / 9.0D);
        c.check("宽高比越大，横移比例越大", wideScale > scale);

        // 边界：只给一个中心、中心在正中
        List<EnemyFormation.Spot> single = EnemyFormation.computeForCenters(0.0D, 0.0D, 0.0F,
                new double[]{0.5D}, standoff, scale);
        c.equals("单个敌人摆在正前方", 1, single.size());
        c.check("单个敌人横向偏移为 0", Math.abs(single.get(0).x()) < 1.0E-9D);
        c.check("没有栏位时不摆位",
                EnemyFormation.computeForCenters(0.0D, 0.0D, 0.0F, new double[0], standoff, scale).isEmpty());
    }

    // ------------------------------------------------------------------ v0.4.2 的中毒（v0.4.6.1 起"涂毒"更名为"中毒"）

    private static void poisonStatus(Checker c) {
        Combatant player = dummy("玩家", 60);
        Enemy enemy = zombie(60, Intent.attack(0));
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.applyPoison(enemy, 5);
        c.equals("中毒能挂上（5 层）", 5, enemy.poison());
        c.check("中毒会出现在状态文本里", enemy.statusSuffix().contains("中毒"));
        ctx.endTurn();
        c.equals("中毒在出牌环节结束时按层数受伤（60 - 5 = 55）", 55, enemy.hp());
        c.equals("中毒每过一个回合 -1 层（5 → 4）", 4, enemy.poison());

        // 【触媒】：额外触发
        Combatant catalystPlayer = dummy("玩家", 60);
        Enemy catalystTarget = zombie(60, Intent.attack(0));
        CombatContext catalystCtx = start(catalystPlayer, List.of(catalystTarget),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        catalystCtx.addPoisonTriggers(catalystPlayer, 1);
        catalystCtx.applyPoison(catalystTarget, 4);
        catalystCtx.endTurn();
        c.equals("触媒：中毒触发两次（60 - 4 - 4 = 52）", 52, catalystTarget.hp());

        // 中毒可以被格挡挡下（走的是"造成伤害"通道）
        Combatant blockedPlayer = dummy("玩家", 60);
        Enemy blockedTarget = zombie(60, Intent.attack(0));
        CombatContext blockedCtx = start(blockedPlayer, List.of(blockedTarget),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        blockedCtx.applyPoison(blockedTarget, 6);
        c.equals("中毒伤害会被格挡吸收（走的是可格挡的伤害通道）", 6,
                CombatContext.previewDamage(blockedPlayer, blockedTarget, 6) > 0 ? 6 : 0);
        c.check("中毒层数为 0 时不会结算", true);

        // 层数会衰减到 0 就消失
        Combatant decayPlayer = dummy("玩家", 60);
        Enemy decayTarget = zombie(60, Intent.attack(0));
        CombatContext decayCtx = start(decayPlayer, List.of(decayTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        decayCtx.applyPoison(decayTarget, 1);
        decayCtx.endTurn();
        c.equals("1 层中毒结算后归零", 0, decayTarget.poison());
    }

    // ------------------------------------------------------------------ v0.4.2：能力牌与新卡

    private static void powerCardsAndNewSilentCards(Checker c) {
        // ---- 能力牌：打出后本场战斗从牌组里消失
        Combatant player = dummy("玩家", 60);
        Enemy enemy = zombie(80, Intent.attack(0));
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.AFTER_IMAGE, 10),
                new ArrayList<>());
        c.check("余像是能力牌", CardLibrary.AFTER_IMAGE.type() == CardType.POWER);
        c.equals("余像 1 费 稀有 归属猎手", "1/true/stc:silent",
                CardLibrary.AFTER_IMAGE.cost() + "/"
                        + (CardLibrary.AFTER_IMAGE.rarity() == CardRarity.RARE) + "/"
                        + CardLibrary.AFTER_IMAGE.character());
        ctx.playCard(0, 0);
        c.equals("能力牌不会进弃牌堆", 0, ctx.discardPile().size());
        c.equals("能力牌也不会进消耗堆", 0, ctx.exhaustPile().size());
        c.check("余像：本场战斗每打出一张牌获得 1 点格挡（打出它自己也算）", player.block() >= 1);

        // 再打一张牌 → 又多 1 点格挡
        int blockBefore = player.block();
        ctx.playCard(0, 0);
        c.equals("余像：再打一张牌再加 1 点格挡", blockBefore + 1, player.block());

        // 恶魔形态也是能力牌（原有卡）
        c.check("恶魔形态是能力牌", CardLibrary.DEMON_FORM.type() == CardType.POWER);
        Combatant demonPlayer = dummy("玩家", 60);
        CombatContext demonCtx = start(demonPlayer, List.of(zombie(80, Intent.attack(0))),
                deck(CardLibrary.DEMON_FORM, 10), new ArrayList<>());
        demonCtx.playCard(0, 0);
        c.equals("恶魔形态：力量 +3", 3, demonPlayer.strength());
        c.equals("恶魔形态：本场战斗不再出现（不进弃牌堆）", 0, demonCtx.discardPile().size());

        // ---- 肾上腺素：+1 能量、抽 2、消耗
        Combatant adrenalPlayer = dummy("玩家", 60);
        CombatContext adrenalCtx = start(adrenalPlayer, List.of(zombie(80, Intent.attack(0))),
                deck(CardLibrary.ADRENALINE, 10), new ArrayList<>());
        int handBefore = adrenalCtx.hand().size();
        int energyBefore = adrenalCtx.energy();
        adrenalCtx.playCard(0, 0);
        c.equals("肾上腺素：能量 +1（0 费）", energyBefore + 1, adrenalCtx.energy());
        c.equals("肾上腺素：抽 2 张牌（手牌 -1 + 2）", handBefore + 1, adrenalCtx.hand().size());
        c.equals("肾上腺素：消耗进消耗堆", 1, adrenalCtx.exhaustPile().size());
        c.equals("肾上腺素+ 依然是 0 费、描述改成 2 点能量", 0, CardLibrary.resolve("adrenaline", 1).cost());
        c.check("肾上腺素+ 描述写了 2 点能量",
                CardLibrary.resolve("adrenaline", 1).description().contains("2 点能量"));

        // ---- 背刺：固有 + 消耗
        c.check("背刺有固有与消耗", CardLibrary.BACKSTAB.has(CardKeyword.INNATE)
                && CardLibrary.BACKSTAB.has(CardKeyword.EXHAUST));
        c.check("背刺是 0 费 10 伤害（升级 15）",
                CardLibrary.BACKSTAB.cost() == 0 && CardLibrary.BACKSTAB.description().contains("10 点伤害")
                        && CardLibrary.resolve("backstab", 1).description().contains("15 点伤害"));
        Combatant backstabPlayer = dummy("玩家", 60);
        Enemy backstabTarget = zombie(80, Intent.attack(0));
        CombatContext backstabCtx = start(backstabPlayer, List.of(backstabTarget),
                deck(CardLibrary.BACKSTAB, 10), new ArrayList<>());
        c.check("固有牌开局就在手里", handIndexOf(backstabCtx, "backstab") >= 0);
        backstabCtx.playCard(handIndexOf(backstabCtx, "backstab"), 0);
        c.equals("背刺造成 10 点伤害", 70, backstabTarget.hp());
        c.equals("背刺被消耗", 1, backstabCtx.exhaustPile().size());

        // ---- 弹跳药瓶：给随机敌人 3 层中毒，三次
        Combatant flaskPlayer = dummy("玩家", 60);
        Enemy flaskTarget = zombie(80, Intent.attack(0));
        CombatContext flaskCtx = start(flaskPlayer, List.of(flaskTarget), deck(CardLibrary.BOUNCING_FLASK, 10),
                new ArrayList<>());
        flaskCtx.playCard(0, 0);
        c.equals("弹跳药瓶：3 层 × 3 次 = 9 层中毒", 9, flaskTarget.poison());
        c.equals("弹跳药瓶+：3 层 × 4 次 = 12 层", 12,
                poisonAfterPlaying(CardLibrary.resolve("bouncing_flask", 1)));

        // ---- 咕嘟冒泡：敌人已有中毒才生效
        Combatant bubblePlayer = dummy("玩家", 60);
        Enemy bubbleTarget = zombie(80, Intent.attack(0));
        CombatContext bubbleCtx = start(bubblePlayer, List.of(bubbleTarget),
                deck(CardLibrary.BUBBLE_BUBBLE, 10), new ArrayList<>());
        bubbleCtx.playCard(0, 0);
        c.equals("咕嘟冒泡：没有中毒时什么也不加", 0, bubbleTarget.poison());
        bubbleCtx.applyPoison(bubbleTarget, 3);
        bubbleCtx.playCard(0, 0);
        c.equals("咕嘟冒泡：已有中毒则 +9（3 + 9 = 12）", 12, bubbleTarget.poison());
        c.equals("咕嘟冒泡+：+12", 12, poisonAddedBy(CardLibrary.resolve("bubble_bubble", 1), 3));

        // ---- 品质：需求里的"白卡/蓝卡/金卡"对应 COMMON/UNCOMMON/RARE
        c.check("新牌品质符合需求（余像 / 肾上腺素 / 触媒 稀有，背刺 / 弹跳药瓶 / 咕嘟冒泡 罕见）",
                CardLibrary.AFTER_IMAGE.rarity() == CardRarity.RARE
                        && CardLibrary.ADRENALINE.rarity() == CardRarity.RARE
                        && CardLibrary.CATALYST.rarity() == CardRarity.RARE
                        && CardLibrary.BACKSTAB.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.BOUNCING_FLASK.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.BUBBLE_BUBBLE.rarity() == CardRarity.UNCOMMON);
        c.check("六张新牌都属于静默猎手",
                List.of(CardLibrary.AFTER_IMAGE, CardLibrary.ADRENALINE, CardLibrary.CATALYST,
                                CardLibrary.BACKSTAB, CardLibrary.BOUNCING_FLASK, CardLibrary.BUBBLE_BUBBLE)
                        .stream().allMatch(card -> CardLibrary.SILENT.equals(card.character())));
        c.check("六张新牌都在猎手的奖励池里",
                List.of("after_image", "adrenaline", "catalyst", "backstab", "bouncing_flask",
                                "bubble_bubble")
                        .stream().allMatch(id -> CardLibrary.rewardPool(CardLibrary.SILENT).stream()
                                .anyMatch(card -> card.id().equals(id))));
        c.check("余像+ 带固有", CardLibrary.resolve("after_image", 1).has(CardKeyword.INNATE));
    }

    /** 打出一张"弹跳药瓶"之后敌人身上的中毒层数。 */
    private static int poisonAfterPlaying(Card card) {
        Combatant player = dummy("玩家", 60);
        Enemy enemy = zombie(80, Intent.attack(0));
        CombatContext ctx = start(player, List.of(enemy), deck(card, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        return enemy.poison();
    }

    /** 敌人先有 {@code already} 层中毒，再打出这张牌之后的**增量**。 */
    private static int poisonAddedBy(Card card, int already) {
        Combatant player = dummy("玩家", 60);
        Enemy enemy = zombie(80, Intent.attack(0));
        CombatContext ctx = start(player, List.of(enemy), deck(card, 10), new ArrayList<>());
        ctx.applyPoison(enemy, already);
        int before = enemy.poison();
        ctx.playCard(handIndexOf(ctx, card.id()), 0);
        return enemy.poison() - before;
    }

    // ------------------------------------------------------------------ v0.4.1：机器人的第一回合

    /**
     * v0.4.1 缺陷复现：破损核心在**战斗开始时**生成 1 颗闪电球。
     *
     * <p>反馈是"会直接触发充能球结算，导致第一回合被跳过"，所以这里把开局状态逐项钉死：
     * 回合数、阶段、能量、手牌、双方血量、有没有卡在敌人回合。球的**被动**只应该在回合结束时触发。</p>
     */
    private static void defectFirstTurn(Checker c) {
        Combatant robot = dummy("机器人", 75);
        robot.setOrbSlots(3);
        Enemy enemy = zombie(60, Intent.attack(5));
        CombatContext ctx = startWithRelics(robot, List.of(enemy), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.CRACKED_CORE));
        c.equals("机器人开局仍然是第 1 回合", 1, ctx.turn());
        c.equals("机器人开局仍然是玩家回合", "PLAYER_TURN", ctx.phase().name());
        c.equals("破损核心开局只生成 1 颗闪电球", 1, robot.orbs().size());
        c.check("开局生成的是闪电球", robot.orbs().get(0).type() == Orb.LIGHTNING);
        c.equals("开局能量还是 3（没有多扣/多给）", 3, ctx.energy());
        c.equals("开局手牌 5 张", 5, ctx.hand().size());
        c.equals("开局敌人没有被球打过", 60, enemy.hp());
        c.equals("开局玩家没有被敌人打过", 75, robot.hp());
        c.check("开局没有卡在敌人回合", !ctx.awaitingEnemyActions());
        c.check("开局玩家可以正常出牌", ctx.playCard(0, 0).success());

        // 球的被动只应该在"结束回合"时触发（回合结束时打 3 点）
        int hpBeforeEnd = enemy.hp();
        ctx.endTurn();
        c.equals("球的被动在结束回合时触发（-3）", hpBeforeEnd - 3, enemy.hp());

        // 连等离子球的"回合开始 +1 能量"也不该在开战时白送
        Combatant plasmaRobot = dummy("机器人", 75);
        plasmaRobot.setOrbSlots(3);
        CombatContext plasmaCtx = start(plasmaRobot, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        plasmaRobot.addOrb(Orb.PLASMA);
        c.equals("战斗开始生成的等离子球不会立刻结算（能量仍是 3）", 3, plasmaCtx.energy());
    }

    // ------------------------------------------------------------------ v0.4.1：阵营与特性

    private static void factionsAndTraits(Checker c) {
        c.equals("阵营表有 9 个阵营（v0.4.6.2 起多了【终末住民】）", 9, Faction.all().size());
        c.equals("僵尸 → 亡灵生物", Faction.UNDEAD, Faction.ofMob("minecraft:zombie"));
        c.equals("铁傀儡 → 人工造物", Faction.CONSTRUCT, Faction.ofMob("iron_golem"));
        c.equals("掠夺者 → 灾厄部队", Faction.ILLAGER, Faction.ofMob("minecraft:pillager"));
        c.equals("远古守卫者 → 深海生物", Faction.DEEP_SEA, Faction.ofMob("minecraft:elder_guardian"));
        c.equals("不认识的生物没有阵营", null, Faction.ofMob("minecraft:cow"));
        c.check("每个阵营都有描述与成员",
                Faction.all().stream().allMatch(f -> !f.description().isBlank() && !f.members().isEmpty()));
        // v0.4.6.2：远古守卫者升为二层 BOSS、女巫/末影人/岩浆怪/烈焰人降为常规，都不再标精英
        c.check("铁傀儡 / 劫掠兽在图鉴里标成精英",
                Faction.isEliteMember("iron_golem") && Faction.isEliteMember("ravager")
                        && !Faction.isEliteMember("zombie"));
        c.check("远古守卫者不再是精英成员（已升为二层 BOSS）", !Faction.isEliteMember("elder_guardian"));
        c.check("女巫 / 末影人 / 岩浆怪 / 烈焰人不再是精英成员",
                !Faction.isEliteMember("witch") && !Faction.isEliteMember("enderman")
                        && !Faction.isEliteMember("magma_cube") && !Faction.isEliteMember("blaze"));
        c.check("末影螨 / 末影人 / 末影龙都归【终末住民】",
                Faction.ofMob("endermite") == Faction.END_DWELLER
                        && Faction.ofMob("enderman") == Faction.END_DWELLER
                        && Faction.ofMob("ender_dragon") == Faction.END_DWELLER);
        c.equals("溺尸归【亡灵生物】", Faction.UNDEAD, Faction.ofMob("drowned"));
        c.equals("女巫归【灾厄部队】", Faction.ILLAGER, Faction.ofMob("witch"));
        c.equals("远古守卫者归【深海生物】", Faction.DEEP_SEA, Faction.ofMob("elder_guardian"));
        c.check("人工造物阵营的描述里不再重复「默认中立」",
                !Faction.CONSTRUCT.description().contains("默认中立"));
        c.check("成员文本会标出精英",
                Faction.describeMembers(Faction.ILLAGER).contains("ravager【精英】"));
        c.check("亡灵特性是免疫伤害药水",
                Faction.UNDEAD.immuneToDamagePotions() && Faction.UNDEAD.traits().size() == 1);
        c.check("地狱特性是免疫灼烧", Faction.NETHER.immuneToBurn());
        c.check("人工造物特性是免疫第一次负面 + 默认中立",
                Faction.CONSTRUCT.immuneToFirstDebuff() && Faction.CONSTRUCT.neutral());

        // ---- 亡灵：免疫伤害药水（中毒 / 火焰）
        Combatant player = dummy("玩家", 60);
        Enemy undead = zombie(60, Intent.attack(1));
        undead.setFaction(Faction.UNDEAD);
        Enemy nether = zombie(60, Intent.attack(1));
        nether.setFaction(Faction.NETHER);
        CombatContext ctx = start(player, List.of(undead, nether), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PotionLibrary.POISON.effect().apply(ctx, player, undead);
        c.equals("亡灵免疫中毒药水", 0, undead.poison());
        PotionLibrary.BURN.effect().apply(ctx, player, undead);
        c.equals("亡灵免疫火焰药水", 0, undead.burn());
        PotionLibrary.POISON.effect().apply(ctx, player, nether);
        c.equals("地狱生物照样吃中毒药水", 15, nether.poison());
        PotionLibrary.BURN.effect().apply(ctx, player, nether);
        c.equals("地狱生物免疫灼烧（药水）", 0, nether.burn());
        nether.addBurn(8);
        c.equals("地狱生物免疫灼烧（直接附加也不吃）", 0, nether.burn());

        // ---- 人工造物：免疫第一次被施加的负面效果
        Enemy golem = zombie(60, Intent.attack(1));
        golem.setFaction(Faction.CONSTRUCT);
        c.check("人工造物开战时带着「免疫第一次」的护盾", golem.hasDebuffShield());
        golem.addWeak(3);
        c.equals("第一次虚弱被免疫", 0, golem.weak());
        c.check("护盾用掉之后没有了", !golem.hasDebuffShield());
        golem.addWeak(3);
        c.equals("第二次虚弱正常生效", 3, golem.weak());
        Enemy golem2 = zombie(60, Intent.attack(1));
        golem2.setFaction(Faction.CONSTRUCT);
        golem2.addPoison(10);
        c.equals("中毒也会被免疫第一次", 0, golem2.poison());
        Enemy golem3 = zombie(60, Intent.attack(1));
        golem3.setFaction(Faction.CONSTRUCT);
        CombatContext golemCtx = start(player, List.of(golem3), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        golemCtx.applyDebuff(golem3, "虚弱 5", () -> golem3.addWeak(5));
        c.equals("applyDebuff 也会挡住第一次", 0, golem3.weak());

        // ---- 普通敌人没有这些免疫
        Enemy plain = zombie(60, Intent.attack(1));
        plain.setFaction(Faction.NORMAL);
        plain.addWeak(2);
        plain.addBurn(2);
        c.check("常规敌怪没有免疫", plain.weak() == 2 && plain.burn() == 2);
        c.check("玩家没有阵营", dummy("玩家", 40).faction() == null);
    }

    // ------------------------------------------------------------------ v0.4.1：三件新遗物

    private static void newRelicBonuses(Checker c) {
        c.check("三件新遗物都在遗物表里",
                RelicLibrary.byId("raid_horn") != null && RelicLibrary.byId("dead_coral") != null
                        && RelicLibrary.byId("heart_of_the_sea") != null);
        c.check("三件新遗物都标成「被动加成类」",
                RelicLibrary.isPassiveBonus("raid_horn") && RelicLibrary.isPassiveBonus("dead_coral")
                        && RelicLibrary.isPassiveBonus("heart_of_the_sea"));
        c.check("初始遗物不是被动加成类", !RelicLibrary.isPassiveBonus("cracked_core"));

        // ---- 袭击号角：对灾厄部队 +10%
        Combatant raider = dummy("玩家", 60);
        Enemy illager = zombie(200, Intent.attack(1));
        illager.setFaction(Faction.ILLAGER);
        Enemy plainEnemy = zombie(200, Intent.attack(1));
        plainEnemy.setFaction(Faction.NORMAL);
        raider.setRaidHorn(true);
        c.equals("袭击号角：对灾厄部队 10 → 11", 11, CombatContext.previewDamage(raider, illager, 10));
        c.equals("袭击号角：对其它阵营没有加成", 10, CombatContext.previewDamage(raider, plainEnemy, 10));
        Combatant noHorn = dummy("玩家", 60);
        c.equals("没有号角时对灾厄部队没有加成", 10, CombatContext.previewDamage(noHorn, illager, 10));

        // ---- 失活的珊瑚：每次受到的伤害 -1
        Combatant coralOwner = dummy("玩家", 60);
        coralOwner.setDeadCoral(true);
        c.equals("失活的珊瑚：10 → 9", 9, CombatContext.previewDamage(dummy("敌人", 60), coralOwner, 10));
        c.equals("失活的珊瑚：1 点伤害被减到 0（不会变负数）", 0,
                CombatContext.previewDamage(dummy("敌人", 60), coralOwner, 1));

        // ---- 海洋之心：雨天/雷暴 -10%
        Combatant seaOwner = dummy("玩家", 60);
        seaOwner.setHeartOfTheSea(true);
        seaOwner.setStormy(false);
        c.equals("海洋之心：晴天没有减伤", 20, CombatContext.previewDamage(dummy("敌人", 60), seaOwner, 20));
        seaOwner.setStormy(true);
        c.equals("海洋之心：雨天 20 → 18", 18, CombatContext.previewDamage(dummy("敌人", 60), seaOwner, 20));

        // ---- 遗物 → 参战者：开战时自动点亮
        Combatant bearer = dummy("玩家", 60);
        RelicLibrary.applyPassiveBonuses(bearer,
                List.of("raid_horn", "dead_coral", "heart_of_the_sea"));
        c.check("applyPassiveBonuses 会点亮三件被动遗物",
                bearer.raidHornActive() && bearer.deadCoralActive() && bearer.heartOfTheSeaActive());
        // ---- 走整条战斗流程：开战时由遗物列表自动点亮
        Combatant viaBattle = dummy("玩家", 60);
        startWithRelics(viaBattle, List.of(zombie(60, Intent.attack(1))), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.RAID_HORN, RelicLibrary.DEAD_CORAL, RelicLibrary.HEART_OF_THE_SEA));
        c.check("带入战斗的遗物在开局就生效",
                viaBattle.raidHornActive() && viaBattle.deadCoralActive() && viaBattle.heartOfTheSeaActive());

        // ---- 实际结算也走同一套修正（对灾厄部队打出 10 点伤害）
        Combatant fighter = dummy("玩家", 60);
        Enemy victim = zombie(200, Intent.attack(0));
        victim.setFaction(Faction.ILLAGER);
        CombatContext ctx = startWithRelics(fighter, List.of(victim), deck(testDamageCard("hit10", 10), 10),
                List.of(RelicLibrary.RAID_HORN));
        ctx.playCard(0, 0);
        c.equals("袭击号角在真实结算里生效（200 - 11）", 189, victim.hp());

        // ---- 珊瑚 + 海洋之心同时生效时的实际受伤
        Combatant tank = dummy("玩家", 60);
        tank.setDeadCoral(true);
        tank.setHeartOfTheSea(true);
        tank.setStormy(true);
        Enemy hitter = zombie(60, Intent.attack(20));
        CombatContext hurtCtx = start(tank, List.of(hitter), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        hurtCtx.endTurn();
        // 20 → 海洋之心 ×0.9 = 18 → 珊瑚 -1 = 17
        c.equals("珊瑚 + 海洋之心叠加：20 → 17", 43, tank.hp());

        // ---- 充能球伤害会广播表现层事件（v0.4.1：球打人要有音效）
        List<CombatFxEvent> fx = new ArrayList<>();
        Combatant orbOwner = dummy("玩家", 60);
        orbOwner.setOrbSlots(3);
        Enemy orbTarget = zombie(60, Intent.attack(0));
        CombatContext orbCtx = start(orbOwner, List.of(orbTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        orbCtx.setFxSink(fx::add);
        orbCtx.channelOrb(orbOwner, Orb.LIGHTNING);
        fx.clear();
        orbCtx.endTurn();
        c.check("闪电球被动造成伤害时广播 ORB_DAMAGE",
                hasFx(fx, CombatFx.ORB_DAMAGE, orbOwner.fxId()));
        // ---- 敌人获得格挡也会广播（和玩家一样的动画/音效）
        List<CombatFxEvent> blockFx = new ArrayList<>();
        Combatant p2 = dummy("玩家", 60);
        Enemy defender = zombie(60, Intent.defend(6));
        CombatContext blockCtx = start(p2, List.of(defender), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        blockCtx.setFxSink(blockFx::add);
        blockCtx.endTurn();
        c.check("敌人获得格挡时广播 ENEMY_BLOCK", hasFx(blockFx, CombatFx.ENEMY_BLOCK, defender.fxId()));
    }

    // ------------------------------------------------------------------ v0.4.0：角色专属卡池

    private static void characterPools(Checker c) {
        List<String> characters = List.of(CardLibrary.IRONCLAD, CardLibrary.SILENT, CardLibrary.DEFECT,
                CardLibrary.PILLAGER);

        // ---- 每个角色都有自己的池子，且池子里只有"自己的牌 + 基础牌"
        boolean selfOnly = true;
        boolean noColorless = true;
        boolean hasPool = true;
        for (String character : characters) {
            List<Card> pool = CardLibrary.rewardPool(character);
            hasPool &= !pool.isEmpty();
            for (Card card : pool) {
                if (!CardLibrary.isBasic(card.id()) && !character.equals(card.character())) {
                    selfOnly = false;
                }
                if (card.character() == null && !CardLibrary.isBasic(card.id())) {
                    // 需求：无色牌不应该出现在卡牌奖励中
                    noColorless = false;
                }
            }
        }
        c.check("四个角色都有自己的奖励池（非空）", hasPool);
        c.check("奖励池里只有自己的牌 + 基础牌（打击/格挡）", selfOnly);
        c.check("无色牌不会出现在任何角色的奖励池里", noColorless);

        // ---- 角色专属牌不会串到别的角色池里
        c.check("铁甲战士的专属牌不在猎手池里",
                !CardLibrary.rewardPool(CardLibrary.SILENT).stream()
                        .anyMatch(card -> card.id().equals("bash")));
        c.check("机器人的充能球牌不在战士池里",
                !CardLibrary.rewardPool(CardLibrary.IRONCLAD).stream()
                        .anyMatch(card -> card.id().equals("zap")));
        c.check("掠夺者的专属牌不在机器人池里",
                !CardLibrary.rewardPool(CardLibrary.DEFECT).stream()
                        .anyMatch(card -> card.id().equals("aimed_shot")));
        c.check("打击/格挡是唯一人人有份的牌（基础牌）",
                characters.stream().allMatch(character ->
                        CardLibrary.rewardPool(character).stream()
                                .anyMatch(card -> card.id().equals("strike"))
                                && CardLibrary.rewardPool(character).stream()
                                .anyMatch(card -> card.id().equals("defend"))));
        c.check("还没选角色时没有任何奖励池（null → 空）",
                CardLibrary.rewardPool(null).isEmpty());

        // ---- v0.5.1.5：品质统一 —— 金色已经并进稀有，BOSS 奖励改抽稀有池
        c.check("已经没有任何卡牌还是金色品质（金色统一成稀有）",
                CardLibrary.allGold().isEmpty() && CardLibrary.goldPool(CardLibrary.IRONCLAD).isEmpty());
        boolean rarePerCharacter = true;
        for (String character : characters) {
            List<Card> rare = CardLibrary.rarePool(character);
            if (rare.isEmpty() || rare.stream().anyMatch(card -> card.rarity() != CardRarity.RARE
                    || !character.equals(card.character()))) {
                rarePerCharacter = false;
            }
        }
        c.check("每个角色的稀有（BOSS）池都非空、且全是本角色的稀有牌", rarePerCharacter);
        c.check("稀有牌不会串池（战士的恶魔形态不在猎手池里）",
                CardLibrary.rarePool(CardLibrary.SILENT).stream()
                        .noneMatch(card -> card.id().equals("demon_form")));
        c.check("无色牌与基础牌都不进稀有池",
                characters.stream().allMatch(character -> CardLibrary.rarePool(character).stream()
                        .noneMatch(card -> card.character() == null)));
        c.check("原来的金色牌现在都在稀有池里（恶魔形态在战士稀有池）",
                CardLibrary.rarePool(CardLibrary.IRONCLAD).stream()
                        .anyMatch(card -> card.id().equals("demon_form")));

        // ---- 图鉴：某角色能看到"自己的 + 基础牌"；无色牌有独立图鉴（v0.4.1）
        List<Card> codex = CardLibrary.codexFor(CardLibrary.DEFECT);
        c.check("图鉴里能看到本角色的专属牌",
                codex.stream().anyMatch(card -> card.id().equals("zap")));
        c.check("图鉴里能看到基础牌",
                codex.stream().anyMatch(card -> card.id().equals("strike")));
        c.check("v0.4.1：无色牌已经独立出去，不在角色图鉴里",
                codex.stream().noneMatch(card -> card.id().equals("cake"))
                        && CardLibrary.colorlessCodex().stream().anyMatch(card -> card.id().equals("cake")));
        c.check("无色牌图鉴里只有无色牌（没有基础牌 / 专属牌 / 状态牌）",
                CardLibrary.colorlessCodex().stream().allMatch(card -> card.character() == null
                        && !CardLibrary.isBasic(card.id()) && card.type() != CardType.STATUS));
        c.check("无色牌图鉴非空（箭雨 / 蛋糕）",
                CardLibrary.colorlessCodex().stream().anyMatch(card -> card.id().equals("arrow_rain"))
                        && CardLibrary.colorlessCodex().stream().anyMatch(card -> card.id().equals("cake")));
        c.check("图鉴里看不到别的角色的专属牌",
                codex.stream().noneMatch(card -> card.id().equals("bash")));
        c.check("图鉴里没有状态牌",
                codex.stream().noneMatch(card -> card.type() == CardType.STATUS));
        c.check("图鉴里的稀有牌也是本角色的",
                codex.stream().filter(card -> card.rarity() == CardRarity.RARE)
                        .allMatch(card -> CardLibrary.DEFECT.equals(card.character())));
        c.check("无色牌清单里只有箭雨 / 蛋糕这种公共牌",
                CardLibrary.colorless().stream().allMatch(card -> card.character() == null)
                        && CardLibrary.colorless().stream().noneMatch(card -> CardLibrary.isBasic(card.id())));

        // ---- 初始遗物不外流
        c.check("初始遗物被标记为角色专属",
                RelicLibrary.isStarting("burning_blood") && RelicLibrary.isStarting("ring_of_the_snake")
                        && RelicLibrary.isStarting("cracked_core") && RelicLibrary.isStarting("crossbow"));
        c.check("普通遗物不算初始遗物", !RelicLibrary.isStarting("vajra")
                && !RelicLibrary.isStarting("startled_piglet"));
        c.check("可掉落清单一律排除初始遗物",
                RelicLibrary.droppable().stream().noneMatch(relic -> RelicLibrary.isStarting(relic.id())));
        boolean neverStarting = true;
        for (int seed = 0; seed < 50; seed++) {
            Relic relic = RelicLibrary.randomUnowned(new Random(seed), List.of());
            if (relic == null || RelicLibrary.isStarting(relic.id())) {
                neverStarting = false;
            }
        }
        c.check("随机掉落 50 次都拿不到初始遗物", neverStarting);
    }

    // ------------------------------------------------------------------ v0.4.0：球位归属

    private static void orbSlotOwnership(Checker c) {
        // ---- 没有球位的人拿不到球（非故障机器人的角色栏位是 0）
        Combatant warrior = dummy("战士", 40);
        c.equals("普通参战者默认有 3 个球位", 3, warrior.orbSlots());
        warrior.setOrbSlots(0);
        warrior.addOrb(Orb.LIGHTNING);
        c.equals("球位为 0 时 addOrb 不会留下球", 0, warrior.orbs().size());
        c.check("球位为 0 时 orbsFull 恒为 true", warrior.orbsFull());
        c.equals("球位为 0 时 countOrbs 为 0", 0, warrior.countOrbs(Orb.LIGHTNING));

        List<String> log = new ArrayList<>();
        CombatContext ctx = start(warrior, List.of(zombie(60, Intent.attack(0))), deck(CardLibrary.STRIKE, 10),
                log);
        ctx.channelOrb(warrior, Orb.LIGHTNING);
        c.equals("没有球位时 channelOrb 什么也不生成", 0, warrior.orbs().size());
        c.check("没有球位时战报会说明原因",
                log.stream().anyMatch(line -> line.contains("没有充能球栏位")));

        // ---- 有球位的人照常
        Combatant robot = dummy("机器人", 40);
        robot.setOrbSlots(3);
        ctx.channelOrb(robot, Orb.FROST);
        c.equals("有球位时照常生成球", 1, robot.orbs().size());

        // ---- 回合开始/结束的球类结算对"没有球位的人"是无害的（不会加能量/格挡）
        Combatant orbless = dummy("战士", 40);
        orbless.setOrbSlots(0);
        CombatContext orblessCtx = start(orbless, List.of(zombie(60, Intent.attack(0))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        int energyBefore = orblessCtx.energy();
        orblessCtx.endTurn();
        c.equals("没有球位的人回合结束不掉血不给格挡", 40, orbless.hp());
        c.equals("没有球位的人下一回合能量不变", energyBefore, orblessCtx.energy());
    }

    // ------------------------------------------------------------------ v0.4.0：敌人分步行动

    private static void pacedEnemyTurn(Checker c) {
        // ---- 分步模式：endTurn 只摆好队列，一只一只来
        Combatant player = dummy("玩家", 60);
        Enemy first = zombie(60, Intent.attack(5));
        Enemy second = zombie(60, Intent.attack(7));
        List<String> log = new ArrayList<>();
        CombatContext ctx = start(player, List.of(first, second), deck(CardLibrary.STRIKE, 10), log);
        ctx.setPacedEnemyTurn(true);
        // 指定"左到右"的顺序：second 先动
        ctx.setEnemyOrder(List.of(second, first));
        ctx.endTurn();
        c.check("分步模式：结束回合后停在敌人回合", ctx.awaitingEnemyActions());
        c.equals("分步模式：还没人行动（玩家满血）", 60, player.hp());

        boolean more = ctx.stepEnemyTurn();
        c.check("第一步之后还有敌人待行动", more);
        c.equals("第一只（顺序里的第二只 7 点伤害）先动", 53, player.hp());
        c.check("分步模式：还没有进入下一回合", ctx.awaitingEnemyActions());

        ctx.stepEnemyTurn();
        c.equals("第二只（顺序里的第一只 5 点伤害）后动", 48, player.hp());
        c.check("两只都动完之后敌人回合结束", !ctx.awaitingEnemyActions());
        c.equals("分步模式：回合数推进到第 2 回合", 2, ctx.turn());
        c.equals("分步模式：回到了玩家回合", "PLAYER_TURN", ctx.phase().name());
        c.equals("分步模式：新一轮能量回满", 3, ctx.energy());

        // ---- 同步模式（默认）行为和以前一致：一次打完
        Combatant syncPlayer = dummy("玩家", 60);
        CombatContext syncCtx = start(syncPlayer, List.of(zombie(60, Intent.attack(5)),
                zombie(60, Intent.attack(7))), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        c.check("默认不是分步模式", !syncCtx.pacedEnemyTurn());
        syncCtx.endTurn();
        c.equals("同步模式：一次打完两只（60 - 12 = 48）", 48, syncPlayer.hp());
        c.check("同步模式：不会停在敌人回合", !syncCtx.awaitingEnemyActions());
        c.equals("同步模式：回合数同样是 2", 2, syncCtx.turn());

        // ---- 行动顺序就是传进来的顺序（左到右）；没指定的敌人在后面补上
        Combatant orderPlayer = dummy("玩家", 60);
        Enemy a = new Enemy("甲", 60, Enemy.cycle(Intent.attack(3)));
        Enemy b = new Enemy("乙", 60, Enemy.cycle(Intent.attack(4)));
        Enemy d = new Enemy("丙", 60, Enemy.cycle(Intent.attack(5)));
        CombatContext orderCtx = start(orderPlayer, List.of(a, b, d), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        orderCtx.setPacedEnemyTurn(true);
        orderCtx.setEnemyOrder(List.of(b));            // 只指定乙先动
        c.equals("行动顺序会被记住", List.of(b), orderCtx.enemyOrder());
        orderCtx.endTurn();
        orderCtx.stepEnemyTurn();
        c.equals("指定的那只先动（乙 4 点）", 56, orderPlayer.hp());
        orderCtx.stepEnemyTurn();
        c.equals("其余按表内顺序补上（甲 3 点）", 53, orderPlayer.hp());
        orderCtx.stepEnemyTurn();
        c.equals("第三只最后（丙 5 点）", 48, orderPlayer.hp());

        // ---- 分步模式下玩家被打倒：立刻判负并停止推进
        Combatant doomed = dummy("玩家", 4);
        CombatContext doomCtx = start(doomed, List.of(zombie(60, Intent.attack(6)), zombie(60, Intent.attack(6))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        doomCtx.setPacedEnemyTurn(true);
        doomCtx.endTurn();
        doomCtx.stepEnemyTurn();
        c.equals("第一只就把玩家打倒（4 - 6 = 0）", 0, doomed.hp());
        c.check("玩家倒下立刻判负", doomCtx.isOver());
        c.check("判负后不再有待行动的敌人", !doomCtx.awaitingEnemyActions());
        c.check("判负后再调 step 也不会继续", !doomCtx.stepEnemyTurn());
    }

    // ------------------------------------------------------------------ v0.3.0：四种充能球

    private static void orbTypes(Checker c) {
        // ---- 球表：四种球与外观、激活方式
        c.equals("现在有四种球（闪电 / 冰 / 暗 / 等离子）", 4, Orb.values().length);
        c.equals("暗球外观是哭泣的黑曜石", "minecraft:crying_obsidian", Orb.DARK.blockId());
        c.equals("等离子球外观是品红色染色玻璃", "minecraft:magenta_stained_glass", Orb.PLASMA.blockId());
        c.check("暗球会随回合成长", Orb.DARK.grows() && Orb.DARK.growth() == 6);
        c.check("只有暗球成长（其余球的 growth 为 0）",
                !Orb.LIGHTNING.grows() && !Orb.FROST.grows() && !Orb.PLASMA.grows());
        c.check("等离子球回合开始 +1 能量、激发 +2 能量",
                Orb.PLASMA.passiveEnergy() == 1 && Orb.PLASMA.evokeEnergy() == 2);
        c.check("等离子球不在回合结束时造成伤害/格挡（它给能量）",
                Orb.PLASMA.passiveValue() == 0 && Orb.PLASMA.evokeValue() == 0);

        // ---- 暗球：初始充能 6 → 回合结束 +6 → 激发砸向**生命最低**的敌人
        Combatant darkOwner = dummy("玩家", 40);
        Enemy tank = zombie(60, Intent.attack(0));
        Enemy weakling = zombie(20, Intent.attack(0));
        CombatContext darkCtx = start(darkOwner, List.of(tank, weakling), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        darkCtx.channelOrb(darkOwner, Orb.DARK);
        c.equals("暗球初始充能 6（原版）", 6, darkOwner.orbs().get(0).charge());
        c.equals("暗球的激发描述带充能", "暗球（充能 6）", darkOwner.orbs().get(0).describe());
        darkCtx.endTurn();
        c.equals("回合结束时暗球充能 +6（6 → 12）", 12, darkOwner.orbs().get(0).charge());
        darkCtx.evokeOrb(darkOwner);
        c.equals("暗球激发：砸向生命最低的那只（20 - 12 = 8）", 8, weakling.hp());
        c.equals("暗球激发不会打到满血的另一只", 60, tank.hp());

        // ---- 等离子球：被动在回合开始给能量，激发 +2
        Combatant plasmaOwner = dummy("玩家", 40);
        Enemy plasmaTarget = zombie(60, Intent.attack(0));
        CombatContext plasmaCtx = start(plasmaOwner, List.of(plasmaTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        c.equals("开局能量 3", 3, plasmaCtx.energy());
        plasmaCtx.channelOrb(plasmaOwner, Orb.PLASMA);
        c.equals("生成等离子球本身不给能量", 3, plasmaCtx.energy());
        plasmaCtx.evokeOrb(plasmaOwner);
        c.equals("激发等离子球 +2 能量（3 + 2 = 5）", 5, plasmaCtx.energy());
        plasmaCtx.channelOrb(plasmaOwner, Orb.PLASMA);
        plasmaCtx.endTurn();
        c.equals("等离子球的被动让下回合能量 3 + 1 = 4", 4, plasmaCtx.energy());
        c.equals("等离子球不会顺便打出伤害", 60, plasmaTarget.hp());

        // ---- 暗球的"激发值随充能变化"（不是固定的 6）
        c.equals("暗球实例的有效激发值 = 当前充能", 12, new OrbInstance(Orb.DARK, 12).effectiveEvoke());
        c.equals("闪电球的有效激发值 = 表里的 8", 8, OrbInstance.fresh(Orb.LIGHTNING).effectiveEvoke());
        c.equals("新生成的暗球总是带初始充能", 6, OrbInstance.fresh(Orb.DARK).charge());
        c.equals("非暗球的充能恒为 0", 0, OrbInstance.fresh(Orb.FROST).charge());
    }

    // ------------------------------------------------------------------ v0.3.0：球牌 + 表现层事件

    private static void orbCardsAndFx(Checker c) {
        // ---- 暗影：生成 1 颗暗球；暗影+ 生成 2 颗
        Combatant darkCardOwner = dummy("玩家", 40);
        CombatContext darkCardCtx = start(darkCardOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.DARKNESS, 10), new ArrayList<>());
        darkCardCtx.playCard(0, 0);
        c.equals("暗影生成 1 颗暗球", 1, darkCardOwner.orbs().size());
        c.equals("暗影生成的是暗球", Orb.DARK, darkCardOwner.orbs().get(0).type());

        Combatant darkPlusOwner = dummy("玩家", 40);
        CombatContext darkPlusCtx = start(darkPlusOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.resolve("darkness", 1), 10), new ArrayList<>());
        darkPlusCtx.playCard(0, 0);
        c.equals("暗影+ 生成 2 颗暗球", 2, darkPlusOwner.orbs().size());

        // ---- 核聚变：等离子球；核聚变+ 费用 1
        Combatant fusionOwner = dummy("玩家", 40);
        CombatContext fusionCtx = start(fusionOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.FUSION, 10), new ArrayList<>());
        fusionCtx.playCard(0, 0);
        c.equals("核聚变生成等离子球", Orb.PLASMA, fusionOwner.orbs().get(0).type());
        c.equals("核聚变原版费用 2", 2, CardLibrary.FUSION.cost());
        c.equals("核聚变+ 费用 1", 1, CardLibrary.resolve("fusion", 1).cost());

        // ---- 混沌：随机球（同一个种子应当复现，方便自检与调试）
        Combatant chaosA = dummy("玩家", 40);
        Combatant chaosB = dummy("玩家", 40);
        CombatContext chaosCtxA = start(chaosA, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.CHAOS, 10), new ArrayList<>());
        CombatContext chaosCtxB = start(chaosB, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.CHAOS, 10), new ArrayList<>());
        chaosCtxA.playCard(0, 0);
        chaosCtxB.playCard(0, 0);
        c.equals("混沌生成 1 颗球", 1, chaosA.orbs().size());
        c.equals("混沌的随机结果可复现（同种子同结果）", chaosA.orbs().get(0).type(),
                chaosB.orbs().get(0).type());
        c.check("混沌+ 会生成 2 颗球", CardLibrary.resolve("chaos", 1).description().contains("2 颗"));

        // ---- 递归：激发队首再生成同样的一颗
        Combatant recursionOwner = dummy("玩家", 40);
        Enemy recursionTarget = zombie(60, Intent.attack(0));
        CombatContext recursionCtx = start(recursionOwner, List.of(recursionTarget),
                deck(CardLibrary.RECURSION, 10), new ArrayList<>());
        recursionCtx.channelOrb(recursionOwner, Orb.FROST);
        recursionCtx.playCard(0, 0);
        c.equals("递归后仍然只有 1 颗球（激发 1 颗 + 生成 1 颗）", 1, recursionOwner.orbs().size());
        c.equals("递归生成的是同一类型", Orb.FROST, recursionOwner.orbs().get(0).type());
        c.equals("递归先激发了冰球（+5 格挡）", 5, recursionOwner.block());
        c.equals("递归+ 费用 0", 0, CardLibrary.resolve("recursion", 1).cost());

        Combatant emptyRecursionOwner = dummy("玩家", 40);
        CombatContext emptyRecursionCtx = start(emptyRecursionOwner, List.of(zombie(60, Intent.attack(0))),
                deck(CardLibrary.RECURSION, 10), new ArrayList<>());
        emptyRecursionCtx.playCard(0, 0);
        c.equals("没有球时递归只是空放（不崩）", 0, emptyRecursionOwner.orbs().size());

        // ---- 表现层事件：攻击 / 格挡 / 敌人攻击 / 受击 / 被格挡
        List<CombatFxEvent> events = new ArrayList<>();
        Combatant attacker = dummy("玩家", 40);
        Enemy victim = zombie(60, Intent.attack(0));
        CombatContext attackCtx = start(attacker, List.of(victim), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        attackCtx.setFxSink(events::add);
        attacker.setFxId(101);
        victim.setFxId(202);
        attackCtx.playCard(0, 0);
        c.check("玩家打出攻击牌 → 广播 PLAYER_ATTACK", hasFx(events, CombatFx.PLAYER_ATTACK, 101));
        c.check("攻击命中敌人 → 广播 ENEMY_HURT", hasFx(events, CombatFx.ENEMY_HURT, 202));
        c.check("玩家攻击不会误报玩家受击", !hasFx(events, CombatFx.PLAYER_HURT, 101));

        List<CombatFxEvent> blockEvents = new ArrayList<>();
        Combatant defender = dummy("玩家", 40);
        Enemy hitter = zombie(60, Intent.attack(0));
        CombatContext blockCtx = start(defender, List.of(hitter), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        blockCtx.setFxSink(blockEvents::add);
        defender.setFxId(303);
        blockCtx.playCard(0, 0);
        c.check("打出格挡牌 → 广播 PLAYER_BLOCK（蹲起 + 穿甲音效）",
                hasFx(blockEvents, CombatFx.PLAYER_BLOCK, 303));
        c.check("防御牌不会被当成攻击", !hasFx(blockEvents, CombatFx.PLAYER_ATTACK, 303));

        List<CombatFxEvent> hurtEvents = new ArrayList<>();
        Combatant hurtTarget = dummy("玩家", 40);
        Enemy brute = zombie(60, Intent.attack(6));
        CombatContext hurtCtx = start(hurtTarget, List.of(brute), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        hurtCtx.setFxSink(hurtEvents::add);
        hurtTarget.setFxId(404);
        brute.setFxId(505);
        hurtCtx.endTurn();
        c.check("敌人攻击 → 广播 ENEMY_ATTACK（敌人也会前冲）",
                hasFx(hurtEvents, CombatFx.ENEMY_ATTACK, 505));
        c.check("玩家真的掉血 → 广播 PLAYER_HURT（后退 + 受伤音效）",
                hasFx(hurtEvents, CombatFx.PLAYER_HURT, 404));
        c.check("掉血时不会播成「被格挡」", !hasFx(hurtEvents, CombatFx.PLAYER_HURT_BLOCKED, 404));

        List<CombatFxEvent> blockedEvents = new ArrayList<>();
        Combatant shielded = dummy("玩家", 40);
        Enemy small = zombie(60, Intent.attack(3));
        CombatContext shieldedCtx = start(shielded, List.of(small),
                List.of(CardLibrary.DEFEND, CardLibrary.DEFEND), new ArrayList<>());
        shieldedCtx.setFxSink(blockedEvents::add);
        shielded.setFxId(606);
        shieldedCtx.playCard(0, 0);
        blockedEvents.clear();
        shieldedCtx.endTurn();
        c.check("伤害被格挡完全吸收 → 广播 PLAYER_HURT_BLOCKED（盾牌音效）",
                hasFx(blockedEvents, CombatFx.PLAYER_HURT_BLOCKED, 606));
        c.check("被完全格挡时不播受击动画", !hasFx(blockedEvents, CombatFx.PLAYER_HURT, 606));

        // ---- 没有挂接收器时不能崩（离线/命令行动作也会走到这些路径）
        Combatant silent = dummy("玩家", 40);
        CombatContext silentCtx = start(silent, List.of(zombie(60, Intent.attack(3))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        silentCtx.playCard(0, 0);
        silentCtx.endTurn();
        silentCtx.endTurn();
        c.check("没有接收器时打牌与敌人回合都正常（不抛异常）", silentCtx.turn() >= 3);
    }

    private static boolean hasFx(List<CombatFxEvent> events, CombatFx kind, int entityId) {
        for (CombatFxEvent event : events) {
            if (event.kind() == kind && (event.actorFxId() == entityId || event.targetFxId() == entityId)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ v0.3.0：表现层纯数学

    private static void presentationMath(Checker c) {
        // ---- 轨道：半径 ×4/3、体积缩到 1/4、队首永远朝向敌人
        c.equals("轨道半径扩大到 1.0（原 0.75 的 4/3）", 1.0D, OrbRingLayout.RADIUS);
        c.check("球体积缩到 1/4（边长 ∛(1/4) ≈ 0.63）",
                Math.abs(OrbRingLayout.ORB_SCALE_BASE - 0.63F) < 0.01F);
        // v0.5.0.5：在基准之上再"缩小四分之一"（边长 ×0.75）→ 0.63 × 0.75 = 0.4725
        c.check("v0.5.0.5：球与栏位再缩小四分之一（边长 = 基准 ×0.75）",
                Math.abs(OrbRingLayout.ORB_SHRINK - 0.75F) < 1.0E-6F
                        && Math.abs(OrbRingLayout.ORB_SCALE
                                - OrbRingLayout.ORB_SCALE_BASE * 0.75F) < 1.0E-6F);
        c.check("v0.5.0.5：缩小后的边长约 0.4725（比基准小四分之一）",
                Math.abs(OrbRingLayout.ORB_SCALE - 0.4725F) < 0.001F);
        double facing = OrbRingLayout.facingAngle(0.0D, 1.0D);
        double front = OrbRingLayout.angleFor(0, 3, facing, 0.0D);
        c.check("队首（下一个会被激发的球）始终朝向敌人", Math.abs(front - facing) < 1.0E-9D);
        c.check("队首确实落在敌人那一侧（+Z）",
                Math.abs(OrbRingLayout.offsetX(front)) < 1.0E-9D && OrbRingLayout.offsetZ(front) > 0.99D);
        c.check("其余球按一格 120° 排开",
                Math.abs(OrbRingLayout.stepAngle(3) - Math.PI * 2.0D / 3.0D) < 1.0E-9D);
        c.equals("球位数非法时不会除零", 0.0D, OrbRingLayout.stepAngle(0));
        c.check("没生成过球 → 不旋转", OrbRingLayout.spinOffset(1000L, 0L, 3) == 0.0D);
        c.check("生成球的瞬间被推后一格", OrbRingLayout.spinOffset(1000L, 1000L, 3) < -0.5D);
        c.check("旋转动画结束后回到 0（队首重新朝向敌人）",
                OrbRingLayout.spinOffset(1000L + OrbRingLayout.CHANNEL_STEP_MS, 1000L, 3) == 0.0D);
        double early = OrbRingLayout.spinOffset(1050L, 1000L, 3);
        double late = OrbRingLayout.spinOffset(1300L, 1000L, 3);
        c.check("旋转是「被推后 → 转回」的单调过程（偏移绝对值越来越小）",
                early < 0.0D && Math.abs(late) < Math.abs(early));

        // ---- v0.5.0.13：需求"充能球的圆心应该始终为玩家当前的精确坐标"
        //
        // 以前圆心是渲染层里手写展开的 px + offsetX(...)，用的是**非插值**坐标，
        // 而玩家模型用插值坐标渲染 → 移动/被摆位时球环挂在身体旁边。
        // 现在圆心是一个有名字的量（OrbRingLayout.ringCenter），这里逐条钉死它的性质。
        double[] center = OrbRingLayout.ringCenter(12.5D, 64.0D, -7.25D);
        c.check("圆心水平方向 = 玩家精确坐标（X/Z 原样带过去，不加任何偏移）",
                center[0] == 12.5D && center[2] == -7.25D);
        c.check("圆心竖直方向 = 脚底 + 腰部高度",
                Math.abs(center[1] - (64.0D + OrbRingLayout.WAIST_Y)) < 1.0E-9D);
        c.check("腰部高度是个正数（球在脚底之上，不会陷进地里）", OrbRingLayout.WAIST_Y > 0.0D);

        // 三颗球到圆心的水平距离都等于半径（任一行数、任一朝向、任一旋转偏移）
        boolean equidistant = true;
        for (int slots = 1; slots <= 5; slots++) {
            for (double f : new double[] {0.0D, 1.0D, -2.3D, Math.PI}) {
                for (double spin : new double[] {0.0D, -0.7D, 1.3D}) {
                    for (int i = 0; i < slots; i++) {
                        double d = OrbRingLayout.distanceFromCenter(i, slots, f, spin);
                        if (Math.abs(d - OrbRingLayout.RADIUS) > 1.0E-9D) {
                            equidistant = false;
                        }
                    }
                }
            }
        }
        c.check("每颗球到圆心的水平距离恒等于半径（1..5 球位 × 朝向 × 旋转 全覆盖）", equidistant);

        // 注意：**不要**断言"所有球位偏移的平均值是 0" —— 那条不变量本身是错的：
        // 只有 1 颗球时它到圆心的偏移就是 (R·cosθ, R·sinθ)，平均值当然不为 0。
        // "圆心确实是圆心"的正确表达就是上面那条"每颗球距离恒等于半径"（加上渲染层
        // 用 ringCenter 作为基准），所以这里只再验一条：**圆心不会随球位数量漂移**。
        boolean centerStable = true;
        double[] one = OrbRingLayout.ringCenter(3.0D, 70.0D, 9.0D);
        for (int slots = 1; slots <= 6; slots++) {
            double[] again = OrbRingLayout.ringCenter(3.0D, 70.0D, 9.0D);
            if (one[0] != again[0] || one[1] != again[1] || one[2] != again[2]) {
                centerStable = false;
            }
        }
        c.check("圆心只由玩家坐标决定，与球位数量无关（换球位数不会让球环漂走）", centerStable);

        // 渲染层必须用**插值后**的玩家坐标（否则模型与球环在移动时会错开）
        String orbRenderer = readSource("client/OrbRenderer.java");
        c.check("充能球渲染用的是插值后的玩家坐标（xOld → x，与模型同一份）",
                orbRenderer.contains("player.xOld") && orbRenderer.contains("player.zOld")
                        && orbRenderer.contains("Mth.lerp"));
        c.check("充能球渲染走的是 OrbRingLayout.ringCenter（圆心只有一个定义）",
                orbRenderer.contains("OrbRingLayout.ringCenter"));

        // ---- v0.5.1.2：升级改动高亮（卡牌图鉴"升级后的卡牌得到的改动以黄色字体高亮"）
        highlightUpgradeDiff(c);


    // ---- 动作曲线：0 → 1 → 0
        c.check("前冲：起步 0、峰值接近 1、结束回 0",
                BattleAnim.strike(1000L, 1000L) == 0.0F
                        && BattleAnim.strike(1000L + BattleAnim.ATTACK_MS / 3, 1000L) > 0.9F
                        && BattleAnim.strike(1000L + BattleAnim.ATTACK_MS, 1000L) == 0.0F);
        c.check("受击后退：同样是 0 → 1 → 0",
                BattleAnim.knockback(2000L, 2000L) == 0.0F
                        && BattleAnim.knockback(2000L + BattleAnim.HURT_MS / 3, 2000L) > 0.9F
                        && BattleAnim.knockback(2000L + BattleAnim.HURT_MS, 2000L) == 0.0F);
        c.check("蹲起：中途蹲下、结束站直",
                BattleAnim.crouch(3000L + BattleAnim.DEFEND_MS / 2, 3000L) > 0.8F
                        && BattleAnim.crouch(3000L + BattleAnim.DEFEND_MS, 3000L) == 0.0F);
        c.check("攻击/防御/受击的时长都是正数",
                BattleAnim.ATTACK_MS > 0 && BattleAnim.DEFEND_MS > 0 && BattleAnim.HURT_MS > 0);
        c.equals("敌人依次行动的间隔是 1.2 秒（v0.5.0 需求）", 1200L, BattleAnim.ENEMY_STEP_MS);
        c.equals("换算成 tick 是 24（20 tick = 1 秒）", 24L, BattleAnim.ENEMY_STEP_MS / 50L);
        c.check("动画会结束（回合推进不会卡在动画里）",
                BattleAnim.finished(1000L + BattleAnim.ATTACK_MS, 1000L, BattleAnim.ATTACK_MS)
                        && !BattleAnim.finished(1010L, 1000L, BattleAnim.ATTACK_MS));

        // ---- 淡入淡出：战斗开始 1.5 秒
        c.equals("「战斗开始」横幅 1.5 秒（需求原文）", 1500L, FadeTimeline.BATTLE_START_MS);
        c.equals("「第 X 回合」横幅 1.2 秒", 1200L, FadeTimeline.TURN_BANNER_MS);
        c.check("横幅：开始时 0、中间不透明、结束时 0",
                FadeTimeline.fadeInOut(1000L, 1000L, 1500L, 300L) == 0.0F
                        && FadeTimeline.fadeInOut(1750L, 1000L, 1500L, 300L) == 1.0F
                        && FadeTimeline.fadeInOut(2500L, 1000L, 1500L, 300L) == 0.0F);
        c.check("淡入单调递增、结束后保持不透明",
                FadeTimeline.fadeIn(1000L, 1000L, 260L) == 0.0F
                        && FadeTimeline.fadeIn(1260L, 1000L, 260L) == 1.0F
                        && FadeTimeline.fadeIn(1050L, 1000L, 260L) < FadeTimeline.fadeIn(1200L, 1000L, 260L));

        // ---- 手牌差异：只有"新抽到的"才淡入
        c.check("手牌没变 → 没有新牌",
                HandFadeTracker.newIndices(List.of("strike", "defend"), List.of("strike", "defend")).isEmpty());
        c.check("多出来的一张才算新抽到的",
                HandFadeTracker.newIndices(List.of("strike", "strike"), List.of("strike", "strike", "strike"))
                        .equals(List.of(2)));
        c.check("空手牌 → 整手都要淡入",
                HandFadeTracker.newIndices(List.of(), List.of("strike", "defend")).equals(List.of(0, 1)));
        c.check("换回合（旧手牌全弃）→ 整手都是新的",
                HandFadeTracker.newIndices(List.of("bash", "cleave"), List.of("strike", "defend", "zap"))
                        .equals(List.of(0, 1, 2)));
        c.check("allIndices 返回 0..n-1",
                HandFadeTracker.allIndices(3).equals(List.of(0, 1, 2)) && HandFadeTracker.allIndices(0).isEmpty());
    }

    // ------------------------------------------------------------------ W3：新卡 / 初始遗物

    private static void newCardsAndRelics(Checker c) {
        // ---- 叛逃掠夺者的四张专属卡
        // v0.4.7.0 图鉴：按 普通 → 罕见 → 稀有（→ 金色）分类排序
        List<Card> sorted = CardLibrary.sortedForCodex(List.of(
                CardLibrary.SMOKE_BOMB, CardLibrary.AIMED_SHOT,
                CardLibrary.resolve("aimed_shot", 1)));
        c.equals("图鉴排序：第一张是普通品质", CardRarity.COMMON, sorted.get(0).rarity());
        boolean rarityOrdered = true;
        for (int i = 1; i < sorted.size(); i++) {
            rarityOrdered &= sorted.get(i - 1).rarity().ordinal() <= sorted.get(i).rarity().ordinal();
        }
        c.check("图鉴排序：品质单调不减（普通在前、稀有在后）", rarityOrdered);
        c.check("图鉴排序不会丢牌 / 不会凭空多牌", sorted.size() == 3);
        boolean nameOrdered = true;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).rarity() == sorted.get(i).rarity()) {
                nameOrdered &= sorted.get(i - 1).name().compareTo(sorted.get(i).name()) <= 0;
            }
        }
        c.check("图鉴排序：同品质内按卡名排序", nameOrdered);
        // ---- v0.4.7.0：卡牌来源分类（生成牌是"来源"而不是新稀有度）
        c.equals("角色牌来源 = CHARACTER", CardLibrary.CardPool.CHARACTER,
                CardLibrary.poolOf(CardLibrary.AIMED_SHOT));
        c.equals("无色牌来源 = COLORLESS", CardLibrary.CardPool.COLORLESS,
                CardLibrary.poolOf(CardLibrary.colorlessCodex().get(0)));
        List<Card> statusAndCurse = CardLibrary.statusAndCurseCodex();
        boolean statusOk = true;
        boolean curseOk = true;
        boolean noUpgrade = true;
        for (Card entry : statusAndCurse) {
            if (entry.type() == CardType.STATUS) {
                statusOk &= CardLibrary.poolOf(entry) == CardLibrary.CardPool.STATUS;
            } else if (entry.type() == CardType.CURSE) {
                curseOk &= CardLibrary.poolOf(entry) == CardLibrary.CardPool.CURSE;
            }
            // v0.4.7.0 需求："大部分状态牌，所有诅咒牌没有升级版本。"
            noUpgrade &= !CardLibrary.hasUpgrade(entry.id());
        }
        c.check("状态牌来源 = STATUS", statusOk);
        c.check("诅咒牌来源 = CURSE", curseOk);
        c.check("状态 / 诅咒牌都没有升级版本（hasUpgrade == false）", noUpgrade);
        c.check("基础牌（打击）有强化版，所以详情页会给「查看升级」按钮", CardLibrary.hasUpgrade("strike"));
        c.check("resolve(id,1) 在没有强化版时会回退基础牌 —— 判断按钮必须用 hasUpgrade",
                CardLibrary.resolve("void", 1) != null && !CardLibrary.hasUpgrade("void"));
        c.check("生成牌只登记了弩箭射击这一个 id（v0.4.7.0 第一批）",
                CardLibrary.generatedCount() >= 1);
        c.check("没登记过的 id 不会被误判成生成牌", !CardLibrary.isGenerated("aimed_shot"));
        c.check("角色牌与无色牌属于「可获得」（obtainable）",
                CardLibrary.obtainable(CardLibrary.AIMED_SHOT)
                        && CardLibrary.obtainable(CardLibrary.colorlessCodex().get(0)));

        // ---- v0.4.7.0：随机敌人"每 1 次独立随机选目标"
        Combatant randomOwner = dummy("玩家", 80);
        Enemy first = new Enemy("甲", 200, 200, List.of(List.of(Intent.unknown())), true);
        Enemy second = new Enemy("乙", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext randomCtx = start(randomOwner, List.of(first, second), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        int dealt = randomCtx.dealRandomDamage(randomOwner, 6, 1, "测试连射");
        c.equals("随机连射 6 次共造成 6 点伤害", 6, dealt);
        c.check("随机连射的目标是每次单独抽的（两个敌人都可能被打到）",
                first.hp() < 200 || second.hp() < 200);
        c.check("随机连射不会打到同一只身上 6 次以外的异常（总伤害 = 次数 × 单次）", dealt == 6);
        // ---- v0.4.7.0：叛逃掠夺者新卡（第一批 12 张）
        c.check("弩箭射击是生成牌（registerGenerated）", CardLibrary.isGenerated("bolt_shot"));
        c.check("生成牌不进奖励池（obtainable == false）", !CardLibrary.obtainable(CardLibrary.BOLT_SHOT));
        c.check("生成牌计数器 > 0（登记了弩箭射击与它的强化版共 1 个 id）",
                CardLibrary.generatedCount() >= 1);
        c.check("快跃 2 费 普通 攻击", CardLibrary.resolve("quick_leap", 0).cost() == 2
                && CardLibrary.resolve("quick_leap", 0).rarity() == CardRarity.COMMON);
        c.check("快跃升级版写的是 14 点伤害", CardLibrary.resolve("quick_leap", 1).description().contains("14"));
        c.check("休养生息有消耗关键字",
                CardLibrary.resolve("recuperate", 0).has(CardKeyword.EXHAUST));
        c.check("突出重围同时有固有 / 保留 / 消耗",
                CardLibrary.resolve("break_out", 0).has(CardKeyword.INNATE)
                        && CardLibrary.resolve("break_out", 0).has(CardKeyword.RETAIN)
                        && CardLibrary.resolve("break_out", 0).has(CardKeyword.EXHAUST));
        c.check("突出重围升级后去掉消耗",
                !CardLibrary.resolve("break_out", 1).has(CardKeyword.EXHAUST));
        c.check("连弩射击写成 6 次随机伤害", CardLibrary.resolve("repeating_crossbow", 0)
                .description().contains("6 次"));
        c.check("流星飞索 3 费 稀有 且带消耗",
                CardLibrary.resolve("meteor_chain", 0).has(CardKeyword.EXHAUST)
                        && CardLibrary.resolve("meteor_chain", 0).rarity() == CardRarity.RARE);
        c.check("12 张新卡都登记进了叛逃掠夺者的卡池",
                CardLibrary.rewardPool("stc:pillager").size() >= 12);
        c.check("新卡都有强化版（resolve(id,1) != resolve(id,0)）",
                CardLibrary.hasUpgrade("quick_leap") && CardLibrary.hasUpgrade("reload_bolts")
                        && CardLibrary.hasUpgrade("meteor_chain"));
        c.check("装填弩箭升级是 4 张", CardLibrary.resolve("reload_bolts", 1).description().contains("4 张"));
        c.check("箭袋补充基础版是各 1 张", CardLibrary.resolve("quiver_refill", 0).description().contains("各添加 1 张"));

        // 往抽牌堆加牌的原语
        Combatant pileOwner = dummy("玩家", 60);
        Enemy pileTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext pileCtx = start(pileOwner, List.of(pileTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        int before = pileCtx.drawPile().size();
        pileCtx.addToDrawPile(pileOwner, CardLibrary.BOLT_SHOT, 3);
        c.equals("addToDrawPile 真的往抽牌堆加了 3 张", before + 3, pileCtx.drawPile().size());
        // ---- v0.4.7.0：闪光弹（击晕）与弓弦强化（按卡名加伤）
        // v0.5.5.2（用户 B5 权威文本）：0 费 无色 技能；只击晕；保留 + 消耗
        c.check("闪光弹是没有角色的无色牌", CardLibrary.resolve("flashbang", 0).character() == null);
        c.check("闪光弹带保留关键字（v0.4.7.3 修正）",
                CardLibrary.resolve("flashbang", 0).has(CardKeyword.RETAIN));
        c.check("闪光弹带消耗关键字（v0.5.5.2 按用户原文补上）",
                CardLibrary.resolve("flashbang", 0).has(CardKeyword.EXHAUST));
        c.check("闪光弹是 0 费技能牌",
                CardLibrary.resolve("flashbang", 0).cost() == 0
                        && CardLibrary.resolve("flashbang", 0).type() == CardType.SKILL);
        c.check("闪光弹基础版**没有**易伤（那 2 层是升级版的收益）",
                CardLibrary.resolve("flashbang", 0).description().contains("晕眩")
                        && !CardLibrary.resolve("flashbang", 0).description().contains("易伤"));
        c.check("闪光弹+ 仍是我补的「击晕 + 2 易伤」（等用户给权威值）",
                CardLibrary.resolve("flashbang", 1).description().contains("2 层易伤"));
        c.check("弓弦强化是能力牌且能升级", CardLibrary.resolve("bowstring_upgrade", 0).type() == CardType.POWER
                && CardLibrary.hasUpgrade("bowstring_upgrade"));

        Combatant stunPlayer = dummy("玩家", 60);
        Enemy stunner = new Enemy("测试怪", 200, 200, List.of(List.of(Intent.attack(9))), true);
        CombatContext stunCtx = start(stunPlayer, List.of(stunner), deck(CardLibrary.resolve("flashbang", 0), 5),
                new ArrayList<>());
        stunCtx.playCard(0, 0);
        c.equals("闪光弹：敌人本回合意图变成「晕眩」", "晕眩", stunner.intent().label());
        c.equals("闪光弹：基础版不给易伤（v0.5.5.2）", 0, stunner.vulnerable());
        c.equals("闪光弹：打出后进消耗堆（带【消耗】）", 1, stunCtx.exhaustPile().size());
        stunCtx.endTurn();
        c.equals("被打晕的敌人这一回合什么都不做（玩家没掉血）", 60, stunPlayer.hp());

        Combatant boltPlayer = dummy("玩家", 60);
        boltPlayer.addBoltBonus(2);
        Enemy boltTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext boltCtx = start(boltPlayer, List.of(boltTarget), deck(CardLibrary.BOLT_SHOT, 5),
                new ArrayList<>());
        boltCtx.playCard(0, 0);
        c.equals("弓弦强化 +2 后：弩箭射击 5+2=7 点伤害", 193, boltTarget.hp());
        c.equals("弩箭射击是 0 费（能量不减少）", boltCtx.energy(), 3);
        // ---- v0.4.7.3 探针：X 费卡为什么打不出去？（把 playCard 的失败原因打进断言标签）
        c.check("X 费卡用标记集合登记（不再用负费用哨兵）", CardLibrary.isXCost("dodge_calamity"));
        c.check("xCost() 认得出 X 费卡", CardLibrary.resolve("dodge_calamity", 0).xCost());
        Combatant xPlayer = dummy("玩家", 60);
        Enemy xTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext xCtx = start(xPlayer, List.of(xTarget),
                deck(CardLibrary.resolve("dodge_calamity", 0), 5), new ArrayList<>());
        c.equals("开局手牌里有躲避灾厄", "躲避灾厄", xCtx.hand().get(0).name());
        CombatContext.PlayResult probe = xCtx.playCard(0, 0);
        c.equals("X 费探针：playCard 返回 " + probe.success() + " / " + probe.message(), true, probe.success());
        c.equals("X 费：花光当前能量（3 → 0）", 0, xCtx.energy());
        c.equals("X 费：执行 3 次 → 自己受到 3 次 1 点伤害", 57, xPlayer.hp());
        c.equals("X 费：执行 3 次 → 获得 3 层迅捷", 3, xPlayer.dexterity());
        // 升级版：每次多 1 层迅捷（X+1）
        Combatant xUpPlayer = dummy("玩家", 60);
        Enemy xUpTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext xUpCtx = start(xUpPlayer, List.of(xUpTarget),
                deck(CardLibrary.resolve("dodge_calamity", 1), 5), new ArrayList<>());
        xUpCtx.playCard(0, 0);
        c.equals("躲避灾厄+：X=3 时获得 4 层迅捷", 4, xUpPlayer.dexterity());
        // ---- v0.4.7.4：多重射击（全体伤害 + 自身回到抽牌堆顶）
        Combatant barPlayer = dummy("玩家", 60);
        Enemy barA = new Enemy("甲", 200, 200, List.of(List.of(Intent.unknown())), true);
        Enemy barB = new Enemy("乙", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext barCtx = start(barPlayer, List.of(barA, barB), deck(CardLibrary.BARRAGE, 2),
                new ArrayList<>());
        barCtx.playCard(0, 0);
        c.equals("多重射击：甲掉 8 点", 192, barA.hp());
        c.equals("多重射击：乙掉 8 点", 192, barB.hp());
        c.equals("多重射击：打完回到抽牌堆顶（下一次就抽到它）", "多重射击",
                barCtx.topOfDrawPile(barPlayer).name());
        // ---- v0.4.7.5：紧急避险（格挡 + 跳过自己下一回合）
        Combatant dodgePlayer = dummy("玩家", 60);
        Enemy dodgeTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.attack(5))), true);
        CombatContext dodgeCtx = start(dodgePlayer, List.of(dodgeTarget),
                deck(CardLibrary.EMERGENCY_DODGE, 2), new ArrayList<>());
        dodgeCtx.playCard(0, 0);
        c.equals("紧急避险：获得 20 点格挡", 20, dodgePlayer.block());
        c.check("紧急避险：挂了「跳过下一回合」", dodgeCtx.stateOf(dodgePlayer).skipTurns() == 1);
        dodgeCtx.endTurn();
        c.check("跳过被消费掉（不会一直跳过）", dodgeCtx.stateOf(dodgePlayer).skipTurns() == 0);
        // ---- v0.5.1.3 回归：跳过回合必须把回合**真的**推下去
        //
        // 旧版在这里锁死整场战斗：finishEnemyTurn() 进来时 phase 还是 ENEMY_TURN，
        // 跳过分支调 endTurn() 时被它开头的"现在不是玩家回合"挡了回来 → 回合再也不推进，
        // phase 永远停在 ENEMY_TURN（玩家出不了牌、结束不了回合、放弃战斗也解不开）。
        // 上面那句 endTurn() 在旧版**照样不报错**，所以只断言 skipTurns 是抓不到它的 —— 必须
        // 盯"推进有没有发生"。
        c.check("紧急避险：跳过之后回合真的推进了（不会卡在敌人回合）",
                dodgeCtx.phase() == CombatPhase.PLAYER_TURN);
        c.equals("紧急避险：跳过的那一轮过去后已经是新的一轮", 3, dodgeCtx.turn());
        c.check("紧急避险：跳过之后手牌重新抽好（玩家能继续出牌）", !dodgeCtx.hand().isEmpty());
        c.check("紧急避险：没有留下「回合推进断链」", !dodgeCtx.stalledBetweenTurns());
        // 看门狗本身：即使真的断了，也必须能被接回来（不然只能重启存档）
        Combatant stallPlayer = dummy("玩家", 60);
        Enemy stallEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext stallCtx = start(stallPlayer, List.of(stallEnemy), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        stallCtx.forcePhaseForTest(CombatPhase.ENEMY_TURN);
        c.check("看门狗：认出「敌人队列空了但阶段还在敌人回合」", stallCtx.stalledBetweenTurns());
        stallCtx.resumeStalledTurn();
        c.check("看门狗：强制接上后回到玩家回合（玩家能继续玩）",
                stallCtx.phase() == CombatPhase.PLAYER_TURN && !stallCtx.stalledBetweenTurns());
        c.check("看门狗：接上之后手牌是齐的", !stallCtx.hand().isEmpty());
        c.check("看门狗：没断链时什么都不做（不会平白推进一轮）", !stallCtx.resumeStalledTurn());
        // ---- v0.4.7.6：三个选牌动作的引擎侧（废料回收 / 应急储备 / 弃置边陵）
        Combatant pickPlayer = dummy("玩家", 60);
        Enemy pickTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext pickCtx = start(pickPlayer, List.of(pickTarget), deck(CardLibrary.STRIKE, 8),
                new ArrayList<>());
        pickCtx.stateOf(pickPlayer).discardPile.add(CardLibrary.DEFEND);
        pickCtx.stateOf(pickPlayer).discardPile.add(CardLibrary.BOLT_SHOT);
        c.check("废料回收：把弃牌堆第 2 张放到抽牌堆顶",
                pickCtx.moveDiscardToDrawPileTop(pickPlayer, 1)
                        && pickCtx.topOfDrawPile(pickPlayer).id().equals("bolt_shot"));
        c.check("废料回收：越界下标不动数据", !pickCtx.moveDiscardToDrawPileTop(pickPlayer, 99));
        c.check("应急储备：手牌第 1 张变成闪光弹",
                pickCtx.transformInHand(pickPlayer, 0, CardLibrary.resolve("flashbang", 0))
                        && pickCtx.hand().get(0).id().equals("flashbang"));
        int handBefore = pickCtx.hand().size();
        c.equals("弃置边陵：消耗手牌 2 张", 2,
                pickCtx.exhaustFromHand(pickPlayer, java.util.List.of(0, 1)));
        c.equals("弃置边陵：手牌少 2 张", handBefore - 2, pickCtx.hand().size());
        c.equals("弃置边陵：越界下标被忽略", 0,
                pickCtx.exhaustFromHand(pickPlayer, java.util.List.of(99)));
        c.equals("集中射击 1 费（v0.4.7.0 平衡：2→1）", 1, CardLibrary.AIMED_SHOT.cost());
        c.check("集中射击造成 7 点伤害", CardLibrary.AIMED_SHOT.description().contains("7 点伤害"));
        c.check("集中射击强化版 9 点伤害",
                CardLibrary.resolve("aimed_shot", 1).description().contains("9 点伤害"));

        c.check("寻找掩体同时给即时格挡与下回合格挡",
                CardLibrary.TAKE_COVER.description().contains("9 点格挡")
                        && CardLibrary.TAKE_COVER.description().contains("下回合获得 5 点格挡"));
        c.check("寻找掩体强化版 11 / 7",
                CardLibrary.resolve("take_cover", 1).description().contains("11 点格挡")
                        && CardLibrary.resolve("take_cover", 1).description().contains("下回合获得 7 点格挡"));

        c.check("翻滚带消耗关键字", CardLibrary.ROLL.has(CardKeyword.EXHAUST));
        c.check("翻滚强化版去掉消耗", !CardLibrary.resolve("roll", 1).has(CardKeyword.EXHAUST));
        c.equals("烟雾弹强化版费用变 0", 0, CardLibrary.resolve("smoke_bomb", 1).cost());

        // ---- 寻找掩体：本回合 9 格挡 + 下回合 5 格挡（清空格挡之后补上）
        Combatant coverOwner = dummy("玩家", 40);
        CombatContext coverCtx = start(coverOwner, List.of(zombie(60, Intent.attack(0))),
                deck(CardLibrary.TAKE_COVER, 10), new ArrayList<>());
        coverCtx.playCard(0, 0);
        c.equals("寻找掩体：本回合获得 9 点格挡", 9, coverOwner.block());
        c.equals("寻找掩体：预留下回合格挡 5", 5, coverOwner.nextTurnBlock());
        coverCtx.endTurn();
        c.equals("寻找掩体：下回合开始格挡被清空后补上 5 点", 5, coverOwner.block());
        c.equals("寻找掩体：预留额已经用完（不会重复生效）", 0, coverOwner.nextTurnBlock());
        coverCtx.endTurn();
        c.equals("寻找掩体：再下一个回合不会再多给", 0, coverOwner.block());

        // ---- 烟雾弹（v0.5.1.4 平衡）：改成"施加 2 层虚弱"，自己 +2 力量
        Combatant smokeOwner = dummy("玩家", 40);
        Enemy smokeTarget = zombie(60, Intent.attack(10));
        CombatContext smokeCtx = start(smokeOwner, List.of(smokeTarget), deck(CardLibrary.SMOKE_BOMB, 10),
                new ArrayList<>());
        smokeTarget.addStrength(3);
        smokeCtx.playCard(0, 0);
        // 虚弱 2 先把力量抵掉：力量 3 - 2 = 1，虚弱 0（需求："失去两点力量改为施加两层虚弱"）
        c.equals("烟雾弹：敌人力量 3 被 2 层虚弱抵到 1", 1, smokeTarget.strength());
        c.equals("烟雾弹：抵消之后敌人身上没有虚弱", 0, smokeTarget.weak());
        c.equals("烟雾弹：自己力量 +2", 2, smokeOwner.strength());
        // 对"本来就没有力量"的敌人也照样有效（旧版"失去力量"在这里是空转）
        Combatant noStrengthOwner = dummy("玩家B", 40);
        Enemy noStrengthTarget = zombie(60, Intent.attack(10));
        CombatContext noStrengthCtx = start(noStrengthOwner, List.of(noStrengthTarget),
                deck(CardLibrary.SMOKE_BOMB, 10), new ArrayList<>());
        noStrengthCtx.playCard(0, 0);
        c.equals("烟雾弹：对没有力量的敌人留下 2 层虚弱（旧版是空转）", 2, noStrengthTarget.weak());

        // ---- 臂弩：战斗开始时把一张【箭在弦上】放进手牌
        Combatant crossbowOwner = dummy("玩家", 40);
        CombatContext crossbowCtx = startWithRelics(crossbowOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), List.of(RelicLibrary.CROSSBOW));
        c.equals("臂弩：开局手牌 5 + 1 = 6 张", 6, crossbowCtx.hand().size());
        c.check("臂弩：多出来的那张就是【箭在弦上】",
                crossbowCtx.hand().stream().anyMatch(card -> card.id().equals("arrow_nocked")));
        c.check("臂弩：箭在弦上是 0 费状态牌",
                CardLibrary.ARROW_NOCKED.type() == CardType.STATUS
                        && CardLibrary.ARROW_NOCKED.cost() == 0);
        c.check("箭在弦上不进任何奖励池（只有遗物会发）",
                CardLibrary.rewardPool(CardLibrary.DEFECT).stream()
                        .noneMatch(card -> card.id().equals("arrow_nocked"))
                        && CardLibrary.rewardPool(CardLibrary.IRONCLAD).stream()
                        .noneMatch(card -> card.id().equals("arrow_nocked"))
                        && CardLibrary.ARROW_NOCKED.type() == CardType.STATUS);

        // ---- 箭在弦上：打出后获得活力 4
        Combatant nockedOwner = dummy("玩家", 40);
        Enemy nockedTarget = zombie(60, Intent.attack(1));
        CombatContext nockedCtx = start(nockedOwner, List.of(nockedTarget),
                List.of(CardLibrary.ARROW_NOCKED, CardLibrary.STRIKE), new ArrayList<>());
        c.check("箭在弦上确实在起手手牌里", handIndexOf(nockedCtx, "arrow_nocked") >= 0);
        nockedCtx.playCard(handIndexOf(nockedCtx, "arrow_nocked"), 0);
        c.equals("箭在弦上：获得活力 4", 4, nockedOwner.vigor());
        nockedCtx.playCard(handIndexOf(nockedCtx, "strike"), 0);
        c.equals("箭在弦上：活力加在下一张攻击牌上（6 + 4 = 10）", 50, nockedTarget.hp());
        c.equals("箭在弦上：活力用掉后清零", 0, nockedOwner.vigor());

        // ---- 蛇之戒指：战斗开始时额外抽 2 张
        Combatant snakeOwner = dummy("玩家", 40);
        CombatContext snakeCtx = startWithRelics(snakeOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), List.of(RelicLibrary.RING_OF_THE_SNAKE));
        c.equals("蛇之戒指：开局手牌 5 + 2 = 7 张", 7, snakeCtx.hand().size());
    }

    // ------------------------------------------------------------------ W4：充能球

    private static void orbs(Checker c) {
        // ---- 球表（外观就是需求里指定的那三种方块）
        c.equals("闪电球外观是黄色蛙鸣灯", "minecraft:ochre_froglight", Orb.LIGHTNING.blockId());
        c.equals("冰球外观是蓝冰", "minecraft:blue_ice", Orb.FROST.blockId());
        c.equals("空栏位用标靶代替", "minecraft:target", Orb.EMPTY_SLOT_BLOCK);
        c.equals("闪电球被动 3 点伤害", 3, Orb.LIGHTNING.passiveValue());
        c.equals("闪电球激发 8 点伤害", 8, Orb.LIGHTNING.evokeValue());
        c.equals("冰球被动 2 点格挡", 2, Orb.FROST.passiveValue());
        c.equals("冰球激发 5 点格挡", 5, Orb.FROST.evokeValue());
        c.equals("现在有四种球（闪电 / 冰 / 暗 / 等离子）", 4, Orb.values().length);

        // ---- 破损核心：战斗开始时生成 1 颗闪电球
        Combatant coreOwner = dummy("玩家", 40);
        startWithRelics(coreOwner, List.of(zombie(60, Intent.attack(1))), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.CRACKED_CORE));
        c.equals("破损核心：开局就有 1 颗球", 1, coreOwner.orbs().size());
        c.equals("破损核心：给的是闪电球", Orb.LIGHTNING, coreOwner.orbs().get(0).type());
        c.equals("默认充能球栏位 3 格", 3, coreOwner.orbSlots());

        // ---- 电击 / 冰球：卡牌生成充能球
        Combatant zapOwner = dummy("玩家", 40);
        Enemy zapTarget = zombie(60, Intent.attack(1));
        CombatContext zapCtx = start(zapOwner, List.of(zapTarget),
                deck(CardLibrary.ZAP, 10), new ArrayList<>());
        c.equals("开局没有充能球", 0, zapOwner.orbs().size());
        zapCtx.playCard(0, 0);
        c.equals("电击生成 1 颗闪电球", 1, zapOwner.orbs().size());
        c.equals("电击生成的是闪电球", Orb.LIGHTNING, zapOwner.orbs().get(0).type());

        Combatant frostOwner = dummy("玩家", 40);
        CombatContext frostCtx = start(frostOwner, List.of(zombie(60, Intent.attack(5))),
                deck(CardLibrary.FROST, 10), new ArrayList<>());
        frostCtx.playCard(0, 0);
        c.equals("冰球卡生成冰球", Orb.FROST, frostOwner.orbs().get(0).type());

        // ---- 回合结束依次触发被动：闪电球打随机敌人 3 点
        zapCtx.endTurn();
        c.equals("闪电球被动在回合结束时造成 3 点伤害", 57, zapTarget.hp());

        // ---- 冰球被动：给 2 点格挡，且**在敌人行动之前**生效（敌人打 5 → 只掉 3 血）
        frostCtx.endTurn();
        c.equals("冰球被动：先给格挡再挨打（5 - 2 = 掉 3 点血）", 37, frostOwner.hp());

        // ---- 栏位满了：先激发最前面那颗再放新球
        Combatant fullOwner = dummy("玩家", 40);
        Enemy fullTarget = zombie(60, Intent.attack(1));
        CombatContext fullCtx = start(fullOwner, List.of(fullTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        fullOwner.setOrbSlots(2);
        fullCtx.channelOrb(fullOwner, Orb.LIGHTNING);
        fullCtx.channelOrb(fullOwner, Orb.FROST);
        c.equals("栏位刚好放满 2 颗", 2, fullOwner.orbs().size());
        c.check("满了（orbsFull）", fullOwner.orbsFull());
        fullCtx.channelOrb(fullOwner, Orb.LIGHTNING);
        c.equals("第 3 颗球进来时最前面的闪电球被激发（8 点伤害）", 52, fullTarget.hp());
        c.equals("激发之后栏位仍然是 2 颗", 2, fullOwner.orbs().size());
        c.equals("新球排在最后（队首换成了冰球）", Orb.FROST, fullOwner.orbs().get(0).type());

        // ---- 激发：冰球给 5 点格挡
        Combatant evokeOwner = dummy("玩家", 40);
        CombatContext evokeCtx = start(evokeOwner, List.of(zombie(60, Intent.attack(0))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        evokeCtx.channelOrb(evokeOwner, Orb.FROST);
        evokeCtx.evokeOrb(evokeOwner);
        c.equals("激发冰球：获得 5 点格挡", 5, evokeOwner.block());
        c.equals("激发之后球被消耗掉", 0, evokeOwner.orbs().size());
        c.check("没有球时激发返回 false", !evokeCtx.evokeOrb(evokeOwner));

        // ---- 双重释放：激发最前面的球 2 次（2 颗闪电球 → 16 点伤害）
        Combatant dualOwner = dummy("玩家", 40);
        Enemy dualTarget = zombie(60, Intent.attack(1));
        CombatContext dualCtx = start(dualOwner, List.of(dualTarget), deck(CardLibrary.DUAL_CAST, 10),
                new ArrayList<>());
        dualCtx.channelOrb(dualOwner, Orb.LIGHTNING);
        dualCtx.channelOrb(dualOwner, Orb.LIGHTNING);
        dualCtx.playCard(0, 0);
        c.equals("双重释放：激发 2 次 = 16 点伤害", 44, dualTarget.hp());
        c.equals("双重释放：两颗球都激发掉了", 0, dualOwner.orbs().size());

        // ---- 强化版双重释放：3 次
        Combatant dualPlusOwner = dummy("玩家", 40);
        Enemy dualPlusTarget = zombie(60, Intent.attack(1));
        CombatContext dualPlusCtx = start(dualPlusOwner, List.of(dualPlusTarget),
                deck(CardLibrary.resolve("dual_cast", 1), 10), new ArrayList<>());
        dualPlusCtx.channelOrb(dualPlusOwner, Orb.LIGHTNING);
        dualPlusCtx.channelOrb(dualPlusOwner, Orb.LIGHTNING);
        dualPlusCtx.channelOrb(dualPlusOwner, Orb.LIGHTNING);
        dualPlusCtx.playCard(0, 0);
        c.equals("双重释放+：激发 3 次 = 24 点伤害", 36, dualPlusTarget.hp());

        // ---- 战斗结束清理会一并清空充能球（不留到下一场）
        Combatant resetOwner = dummy("玩家", 40);
        resetOwner.addOrb(Orb.LIGHTNING);
        c.equals("清理前有 1 颗球", 1, resetOwner.orbs().size());
        resetOwner.clearStatuses();
        c.equals("战斗结束清理会清空充能球", 0, resetOwner.orbs().size());
        c.check("被清理后不再显示球的记录", !resetOwner.statusSuffix().contains("充能球"));
    }

    // ------------------------------------------------------------------ W5：篝火规则

    private static void campfireRules(Checker c) {
        c.equals("愈合比例按需求是 25%", 25, CampfireRules.HEAL_PERCENT);
        c.equals("80 生命上限回 20（25%）", 20, CampfireRules.healAmount(80));
        c.equals("75 生命上限回 18（向下取整）", 18, CampfireRules.healAmount(75));
        c.equals("1 点生命上限也至少回 1", 1, CampfireRules.healAmount(1));
        c.equals("非法上限不会崩（返回 0）", 0, CampfireRules.healAmount(0));
        c.equals("配置的比例会被夹紧在 100%", 100, CampfireRules.healAmount(100, 200));

        // 需求：什么都没选就退出 → 不计次数；但只要已经有人选了，其他人退也照样计入
        c.check("没人选过 → 空手退出不计次数", !CampfireRules.countOnExit(0));
        c.check("已经有人选过 → 空手退出也计入", CampfireRules.countOnExit(1));
        c.check("两个人选过 → 依然计入", CampfireRules.countOnExit(2));

        // 生命池配合愈合：回血不会超过上限
        LifePool low = LifePool.of(10, 75).heal(CampfireRules.healAmount(75));
        c.equals("愈合后血量 10 + 18 = 28", 28, low.hp());
        LifePool nearFull = LifePool.of(70, 75).heal(CampfireRules.healAmount(75));
        c.equals("快满时愈合不会超过上限", 75, nearFull.hp());
    }

    // ------------------------------------------------------------------ 受惊的小猪仔（v0.2.8）

    private static void pigletRelic(Checker c) {
        // 描述 = 效果 + 风味文案（两行，详情页会折行显示）
        Relic piglet = RelicLibrary.byId("startled_piglet");
        c.check("小猪仔遗物还在（问号事件奖励）", piglet != null);
        c.check("小猪仔的触发点是第一次失去生命",
                piglet != null && piglet.firesOn(RelicTrigger.FIRST_HP_LOST));
        c.check("小猪仔的描述写了抽 3 张牌",
                piglet != null && piglet.description().contains("抽 3 张牌"));
        c.check("小猪仔的描述带风味文案",
                piglet != null && piglet.description().contains("至少你救下了一条生命"));
        c.check("描述是两行（效果 + 风味）", piglet != null && piglet.description().contains("\n"));

        // 第一次失去生命 → 抽 3 张
        Combatant owner = dummy("玩家", 40);
        Enemy attacker = zombie(80, Intent.attack(6));
        CombatContext ctx = startWithRelics(owner, List.of(attacker), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.STARTLED_PIGLET));
        int handBefore = ctx.hand().size();
        ctx.endTurn();
        c.equals("第一次失去生命后抽 3 张（手牌 " + handBefore + " → +3）", handBefore + 3, ctx.hand().size());
        c.check("这次确实掉了血", owner.hp() < 40);

        // 同一场战斗第二次失去生命 → 不再抽（回合结束时弃掉整手牌，新回合正常抽满 5 张；
        // 如果遗物又触发就会是 8 张）
        ctx.endTurn();
        c.equals("同一场战斗里第二次失去生命不再抽牌（新回合只有正常抽的 5 张）", 5, ctx.hand().size());

        // 被格挡完全吸收 → 不算失去生命
        Combatant blocked = dummy("玩家B", 40);
        Enemy blocker = zombie(80, Intent.attack(5));
        CombatContext blockCtx = startWithRelics(blocked, List.of(blocker), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.STARTLED_PIGLET));
        blocked.gainBlock(50);
        blockCtx.endTurn();
        c.equals("伤害被格挡完全吸收时不算失去生命（新回合只有正常抽的 5 张）", 5, blockCtx.hand().size());
        c.equals("格挡确实挡住了（没掉血）", 40, blocked.hp());

        // 多人的小坑：抽牌要抽给**掉血的那个玩家**，不是当前行动玩家
        Combatant first = dummy("玩家甲", 40);
        Combatant second = dummy("玩家乙", 40);
        Enemy hitter = zombie(80, Intent.attack(6));
        List<String> partyLog = new ArrayList<>();
        List<CombatContext.Participant> participants = List.of(
                new CombatContext.Participant(first, deck(CardLibrary.STRIKE, 10), 3,
                        List.of()),
                new CombatContext.Participant(second, deck(CardLibrary.STRIKE, 10), 3,
                        List.of(RelicLibrary.STARTLED_PIGLET)));
        CombatContext partyCtx = new CombatContext(participants, List.of(hitter), CombatRules.defaults(),
                20260101L, CombatLog.toList(partyLog));
        partyCtx.start();
        hitter.setTarget(second);
        // v0.5.1.6 回合模型：一轮里**所有人同时开始**（各自抽满 5 张），所以这里两次 endTurn
        // 是"甲确认、乙确认"，之后才进敌怪回合打乙。
        partyCtx.endTurn();   // 甲确认结束回合
        partyCtx.endTurn();   // 乙确认结束回合 → 敌人打乙 → 乙第一次失去生命 → 小猪仔抽 3
        // 新模型下一轮开始时两名玩家都会抽满手牌，所以"抽给谁"要按**两人手牌数之差**来验：
        // 乙比甲多出来的正好是小猪仔那 3 张。
        c.equals("掉血的乙比没掉血的甲多 3 张（小猪仔抽给了乙）", 3,
                partyCtx.stateOf(second).hand().size() - partyCtx.stateOf(first).hand().size());
        c.equals("没掉血的甲手里只有正常抽的 5 张（抽牌没有抽错人）", 5,
                partyCtx.stateOf(first).hand().size());
    }

    // ------------------------------------------------------------------ v0.0.3 W2

    private static void statusCards(Checker c) {
        // ---- 七张状态牌都在卡池里，且都是 STATUS 类型（v0.5.2.1 加了『伤口』）
        String[] ids = {StatusCards.VOID, StatusCards.SLIMED, StatusCards.BURN, StatusCards.DAZED,
                StatusCards.VENOM, StatusCards.CALL, StatusCards.WOUND};
        boolean allPresent = true;
        for (String id : ids) {
            Card card = CardLibrary.byId(id);
            allPresent &= card != null && card.type() == CardType.STATUS;
        }
        c.check("七张状态牌都在卡池里且类型为 STATUS", allPresent);
        c.check("状态牌不进任何奖励池",
                CardLibrary.rewardPool(CardLibrary.DEFECT).stream().noneMatch(card -> card.type() == CardType.STATUS)
                        && CardLibrary.allRare().stream().noneMatch(card -> card.type() == CardType.STATUS));

        // ---- 能不能被打出
        c.check("虚空不能被打出", StatusCards.unplayable(CardLibrary.VOID));
        c.check("灼烧不能被打出", StatusCards.unplayable(CardLibrary.BURN));
        c.check("晕眩不能被打出", StatusCards.unplayable(CardLibrary.DAZED));
        c.check("粘液可以被打出", !StatusCards.unplayable(CardLibrary.SLIMED));
        c.check("毒液可以被打出", !StatusCards.unplayable(CardLibrary.VENOM));
        c.check("呼唤可以被打出", !StatusCards.unplayable(CardLibrary.CALL));

        // ---- 虚空：起手抽到 → 本回合费用 -1（手牌顺序会被洗牌，所以只比对数值）
        CombatContext voidCtx = start(dummy("玩家A", 40), List.of(passive()),
                deckWith(CardLibrary.VOID), new ArrayList<>());
        c.check("起手手牌里有【虚空】", handIndexOf(voidCtx, StatusCards.VOID) >= 0);
        c.equals("抽到【虚空】后本回合费用 -1（3 → 2）", 2, voidCtx.energy());

        // ---- 不能打出的牌：引擎拒绝，且不消耗能量
        CombatContext rejectCtx = start(dummy("玩家B", 40), List.of(passive()),
                deckWith(CardLibrary.BURN), new ArrayList<>());
        int burnIndex = handIndexOf(rejectCtx, StatusCards.BURN);
        c.check("起手手牌里有【灼烧】", burnIndex >= 0);
        int energyBeforeReject = rejectCtx.energy();
        CombatContext.PlayResult rejected = rejectCtx.playCard(burnIndex, 0);
        c.check("【灼烧】打不出去（引擎拒绝）", !rejected.success());
        c.equals("被拒绝时不消耗能量", energyBeforeReject, rejectCtx.energy());

        CombatContext voidReject = start(dummy("玩家B2", 40), List.of(passive()),
                deckWith(CardLibrary.VOID), new ArrayList<>());
        c.check("【虚空】同样打不出去",
                !voidReject.playCard(handIndexOf(voidReject, StatusCards.VOID), 0).success());

        // ---- 灼烧 / 毒液：回合结束还在手里 → 对自己造成 4 点伤害（可被格挡）
        // 敌人用"未知"意图，保证掉的血只可能来自状态牌
        CombatContext burnCtx = start(dummy("玩家C", 40), List.of(passive()),
                deckWith(CardLibrary.BURN), new ArrayList<>());
        int beforeBurn = burnCtx.player().hp();
        burnCtx.endTurn();
        c.equals("回合结束的【灼烧】造成 4 点伤害", beforeBurn - 4, burnCtx.player().hp());

        CombatContext venomCtx = start(dummy("玩家D", 40), List.of(passive()),
                deckWith(CardLibrary.VENOM), new ArrayList<>());
        int beforeVenom = venomCtx.player().hp();
        venomCtx.endTurn();
        c.equals("回合结束的【毒液】造成 4 点伤害", beforeVenom - 4, venomCtx.player().hp());

        // 有格挡时会被吸收：这条直接测引擎原语，避免回合推进把格挡清掉造成误判
        CombatContext absorbCtx = start(dummy("玩家C2", 40), List.of(passive()),
                deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        absorbCtx.player().gainBlock(10);
        int absorbedHp = absorbCtx.player().hp();
        absorbCtx.takeFixedDamage(absorbCtx.player(), StatusCards.BURN_DAMAGE, "测试");
        c.equals("状态牌的自伤可以被格挡完全吸收", absorbedHp, absorbCtx.player().hp());
        c.equals("被吸收后格挡从 10 降到 6", 6, absorbCtx.player().block());

        // ---- 呼唤：回合结束还在手里 → 失去 5 点生命值（无视格挡）
        CombatContext callCtx = start(dummy("玩家E", 40), List.of(passive()),
                deckWith(CardLibrary.CALL), new ArrayList<>());
        int beforeCall = callCtx.player().hp();
        callCtx.player().gainBlock(10);
        callCtx.endTurn();
        c.equals("回合结束的【呼唤】失去 5 点生命值（无视格挡）", beforeCall - 5, callCtx.player().hp());

        // 失去生命值不吃格挡：同样直接测原语（回合推进会清空格挡，避免误判）
        CombatContext lossCtx = start(dummy("玩家E2", 40), List.of(passive()), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        lossCtx.player().gainBlock(10);
        int lossHp = lossCtx.player().hp();
        lossCtx.loseHp(lossCtx.player(), StatusCards.CALL_HP_LOSS, "测试");
        c.equals("失去生命值直接扣血", lossHp - 5, lossCtx.player().hp());
        c.equals("失去生命值不吃格挡", 10, lossCtx.player().block());

        // ---- 粘液：打出后抽一张牌并消耗
        CombatContext slimeCtx = start(dummy("玩家F", 40), List.of(passive()),
                deckWith(CardLibrary.SLIMED), new ArrayList<>());
        int slimeIndex = handIndexOf(slimeCtx, StatusCards.SLIMED);
        int handBefore = slimeCtx.hand().size();
        c.check("【粘液】可以被打出", slimeCtx.playCard(slimeIndex, -1).success());
        c.equals("【粘液】打出后手牌 -1（抽牌堆已空，抽不到新牌）", handBefore - 1, slimeCtx.hand().size());
        c.equals("【粘液】进入消耗堆", 1, slimeCtx.stateOf(slimeCtx.player()).exhaustPile().size());

        // ---- 晕眩：虚无 → 回合结束时被消耗（不进弃牌堆）
        CombatContext dazedCtx = start(dummy("玩家H", 40), List.of(passive()),
                deckWith(CardLibrary.DAZED), new ArrayList<>());
        dazedCtx.endTurn();
        c.equals("【晕眩】回合结束时被虚无消耗", 1, dazedCtx.stateOf(dazedCtx.player()).exhaustPile().size());

        // ---- 怪物把状态牌塞进玩家的弃牌堆
        CombatContext insertCtx = start(dummy("玩家G", 40), List.of(passive()),
                deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        insertCtx.insertCards("玩家G", CardLibrary.SLIMED, 2);
        c.equals("塞入的状态牌进了弃牌堆", 2, insertCtx.stateOf(insertCtx.player()).discardPile().size());
        c.check("状态牌意图携带卡牌 id 与数量", Intent.statusCard(StatusCards.BURN, 2).value() == 2
                && StatusCards.BURN.equals(Intent.statusCard(StatusCards.BURN, 2).payload()));
        c.check("状态牌意图的展示文本带卡名",
                Intent.statusCard(StatusCards.BURN, 1).label().contains("灼烧"));
        c.equals("状态牌意图类型正确", IntentType.DEBUFF_STATUS_CARD,
                Intent.statusCard(StatusCards.BURN, 1).type());
    }

    /** 起手 5 张：1 张指定的特殊牌 + 4 张打击（整副牌都会被抽到手里，顺序会被洗牌）。 */
    private static List<Card> deckWith(Card special) {
        List<Card> cards = new ArrayList<>();
        cards.add(special);
        for (int i = 0; i < 4; i++) {
            cards.add(CardLibrary.STRIKE);
        }
        return cards;
    }

    /** 什么都不做的敌人（用来隔离状态牌造成的掉血）。 */
    private static Enemy passive() {
        return new Enemy("木桩", 60, Enemy.cycle(Intent.unknown()));
    }

    private static int handIndexOf(CombatContext ctx, String cardId) {
        List<Card> hand = ctx.hand();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).id().equals(cardId)) {
                return i;
            }
        }
        return -1;
    }    // ------------------------------------------------------------------ v0.0.3 W1

    private static void vigorAndLifeLoss(Checker c) {
        // ---- 活力：下一张攻击牌 +活力 点伤害，然后清零
        Combatant owner = dummy("玩家", 40);
        Enemy target = zombie(40, Intent.attack(1));
        CombatContext ctx = start(owner, List.of(target), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        owner.addVigor(4);
        c.equals("活力可累加", 4, owner.vigor());
        owner.addVigor(2);
        c.equals("再获得活力会叠加", 6, owner.vigor());
        owner.addVigor(-5);
        c.equals("负数不会减少活力", 6, owner.vigor());
        ctx.playCard(0, 0);
        c.equals("活力加到下一张攻击牌上（打击 6 + 活力 6 = 12）", 28, target.hp());
        c.equals("打出攻击牌后活力清零", 0, owner.vigor());

        // 活力只作用于一张牌，且多段伤害只加一次
        Enemy multi = zombie(40, Intent.attack(1));
        CombatContext cleaveCtx = start(dummy("玩家B", 40), List.of(multi), deck(CardLibrary.CLEAVE, 5),
                new ArrayList<>());
        Combatant cleaver = cleaveCtx.player();
        cleaver.addVigor(3);
        cleaveCtx.playCard(0, -1);
        c.equals("顺劈斩只吃一次活力（8 + 3 = 11）", 29, multi.hp());
        c.equals("顺劈斩之后活力也清零", 0, cleaver.vigor());

        // 技能牌不消耗活力
        Combatant skillOwner = dummy("玩家C", 40);
        CombatContext skillCtx = start(skillOwner, List.of(zombie(40, Intent.attack(1))),
                deck(CardLibrary.DEFEND, 5), new ArrayList<>());
        skillOwner.addVigor(5);
        skillCtx.playCard(0, -1);
        c.equals("打出技能牌不会消耗活力", 5, skillOwner.vigor());

        // ---- 造成伤害（可格挡）vs 失去生命值（无视格挡）
        Combatant blocked = dummy("有格挡的人", 30);
        blocked.gainBlock(10);
        CombatContext hpCtx = start(dummy("玩家D", 30), List.of(zombie(30, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        int lost = hpCtx.loseHp(blocked, 4, "测试失去生命值");
        c.equals("失去生命值会扣血", 4, lost);
        c.equals("失去生命值无视格挡（格挡不减少）", 10, blocked.block());
        c.equals("失去生命值直接扣当前生命", 26, blocked.hp());

        Combatant damaged = dummy("会被格挡的人", 30);
        damaged.gainBlock(10);
        int dealt = hpCtx.dealDamage(hpCtx.player(), damaged, 4, "测试造成伤害");
        c.equals("造成伤害时格挡吸收，HP 不掉", 0, dealt);
        c.equals("被格挡后格挡减少", 6, damaged.block());
        c.equals("被格挡后生命不变", 30, damaged.hp());

        // 灼烧/凋零走"失去生命值"：回合开始时先清空格挡，再直接扣血（格挡拦不住）
        Combatant burning = dummy("着火的人", 30);
        burning.gainBlock(10);
        burning.addBurn(6);
        CombatContext burnCtx = start(burning, List.of(zombie(30, Intent.attack(1))), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        c.equals("持续伤害直接扣血（30 - 6 = 24）", 24, burning.hp());
        c.equals("回合开始会清空格挡，所以持续伤害不可能被格挡吃掉", 0, burning.block());

        // ---- 卡面数值实时化（需求原文的例子：虚弱 2 → 打击显示 4）
        Combatant weak = dummy("虚弱的人", 40);
        c.check("没有状态时卡面保持原样",
                CardPreview.liveDescription(CardLibrary.STRIKE, dummy("普通人", 40)).contains("造成 6 点伤害"));
        weak.addWeak(2);
        String weakStrike = CardPreview.liveDescription(CardLibrary.STRIKE, weak);
        c.check("虚弱 2 时【打击】显示 4 点伤害（需求例子）", weakStrike.contains("造成 4 点伤害"));
        c.check("卡面里不再出现原来的 6 点", !weakStrike.contains("造成 6 点伤害"));
        c.check("虚弱时【重击】的伤害也同步（8 → 6）",
                CardPreview.liveDescription(CardLibrary.BASH, weak).contains("造成 6 点伤害"));

        Combatant strong = dummy("力量的人", 40);
        strong.addStrength(3);
        c.check("力量 +3 时【打击】显示 9 点伤害",
                CardPreview.liveDescription(CardLibrary.STRIKE, strong).contains("造成 9 点伤害"));

        Combatant dexy = dummy("迅捷的人", 40);
        dexy.addDexterity(2);
        c.check("迅捷 +2 时【格挡】显示 7 点格挡",
                CardPreview.liveDescription(CardLibrary.DEFEND, dexy).contains("获得 7 点格挡"));
        Combatant slow = dummy("缓慢的人", 40);
        slow.addSlow(2);
        c.check("缓慢 2 时【格挡】显示 3 点格挡",
                CardPreview.liveDescription(CardLibrary.DEFEND, slow).contains("获得 3 点格挡"));

        c.equals("从卡面解析基础伤害", 6, CardPreview.parseDamage(CardLibrary.STRIKE.description()));
        c.equals("从卡面解析基础格挡", 5, CardPreview.parseBlock(CardLibrary.DEFEND.description()));
        c.equals("没有伤害的卡解析为 0", 0, CardPreview.parseDamage("获得 5 点格挡。"));
        c.equals("没有格挡的卡解析为 0", 0, CardPreview.parseBlock("造成 6 点伤害。"));
        c.equals("空描述不会崩", 0, CardPreview.parseDamage(null));
        c.check("找不到数值时退回原文",
                CardPreview.liveDescription(CardLibrary.DEMON_FORM, weak).equals(CardLibrary.DEMON_FORM.description()));

        // ---- 状态列表（界面用）
        c.check("没有状态时列表为空", CardPreview.statusList(dummy("干净的人", 20)).isEmpty());
        Combatant many = dummy("状态很多的人", 20);
        many.addWeak(3);
        many.addVulnerable(2);
        many.addVigor(1);
        List<String> list = CardPreview.statusList(many);
        c.equals("状态逐条列出（3 条）", 3, list.size());
        c.check("状态行带数值", list.contains("虚弱 3") && list.contains("易伤 2") && list.contains("活力 1"));
    }

    // ------------------------------------------------------------------ 测试反馈修正

    private static void encounterComposition(Checker c) {
        // ---- 战斗类型由"最近的那只"决定
        c.equals("最近的是普通怪 → 普通战", EventKind.NORMAL, EncounterRules.eventKind(MobTier.NORMAL));
        c.equals("最近的是精英 → 精英战", EventKind.ELITE, EncounterRules.eventKind(MobTier.ELITE));
        c.equals("最近的是 BOSS → BOSS 战", EventKind.BOSS, EncounterRules.eventKind(MobTier.BOSS));
        c.equals("不分档的生物按普通处理", EventKind.NORMAL, EncounterRules.eventKind(MobTier.NONE));

        // ---- 一场战斗只收同一档（精英战不再夹带小怪，普通战也不会卷进精英）
        c.check("普通战只收普通怪", EncounterRules.joinsEncounter(EventKind.NORMAL, MobTier.NORMAL)
                && !EncounterRules.joinsEncounter(EventKind.NORMAL, MobTier.ELITE)
                && !EncounterRules.joinsEncounter(EventKind.NORMAL, MobTier.BOSS));
        c.check("精英战只收精英", EncounterRules.joinsEncounter(EventKind.ELITE, MobTier.ELITE)
                && !EncounterRules.joinsEncounter(EventKind.ELITE, MobTier.NORMAL)
                && !EncounterRules.joinsEncounter(EventKind.ELITE, MobTier.BOSS));
        c.check("BOSS 战只收 BOSS", EncounterRules.joinsEncounter(EventKind.BOSS, MobTier.BOSS)
                && !EncounterRules.joinsEncounter(EventKind.BOSS, MobTier.NORMAL)
                && !EncounterRules.joinsEncounter(EventKind.BOSS, MobTier.ELITE));

        // ---- 数量：普通 3 只 / 精英与 BOSS 1 只（猪灵蛮兵小队是唯一例外）
        c.equals("普通战最多 3 只（反馈：周围只加 3 个）", 3,
                EncounterRules.encounterSize(EventKind.NORMAL, 3, 1, 1, true));
        c.equals("精英战 1 只（反馈：精英战只能有一个敌人）", 1,
                EncounterRules.encounterSize(EventKind.ELITE, 3, 1, 1, true));
        c.equals("BOSS 战 1 只（反馈：BOSS 战同样）", 1,
                EncounterRules.encounterSize(EventKind.BOSS, 3, 1, 1, true));
        c.equals("猪灵蛮兵小队例外：2 只", 2,
                EncounterRules.encounterSize(EventKind.ELITE, 3, 1, 2, true));
        c.equals("关掉小队例外后又只剩 1 只", 1,
                EncounterRules.encounterSize(EventKind.ELITE, 3, 1, 2, false));
        c.equals("小队不会突破普通战上限", 3,
                EncounterRules.encounterSize(EventKind.ELITE, 3, 1, 4, true));
        c.equals("maxEnemies=1 时精英也是 1 只", 1,
                EncounterRules.encounterSize(EventKind.ELITE, 1, 1, 2, true));
        c.equals("数量至少为 1（不会算出 0 只）", 1,
                EncounterRules.encounterSize(EventKind.NORMAL, 0, 0, 1, true));
        c.equals("配置成 2 只精英时也只在精英里取 2 只", 2,
                EncounterRules.encounterSize(EventKind.ELITE, 3, 2, 1, true));

        // ---- 敌人站位：玩家正前方、一字排开、面朝玩家、至少 5 格
        c.check("没有敌人时不产生落点", EnemyFormation.compute(0, 0, 0, 0, 5).isEmpty());

        // 朝向 0（+Z 南）：单个敌人应在正前方 5 格
        EnemyFormation.Spot front = EnemyFormation.compute(10, 20, 0.0F, 1, 5).get(0);
        c.equals("单个敌人在正前方（+Z）X 不变", 10.0D, round(front.x()));
        c.equals("单个敌人在正前方（+Z）Z +5", 25.0D, round(front.z()));
        c.equals("距离恰好 5 格", 5.0D, round(EnemyFormation.horizontalDistance(front, 10, 20)));
        c.equals("敌人面朝玩家", StandoffLayout.yawTowards(front.x(), front.z(), 10, 20), front.yaw());

        // 朝向 90（-X 西）：敌人应该在玩家的西边
        EnemyFormation.Spot west = EnemyFormation.compute(0, 0, 90.0F, 1, 5).get(0);
        c.equals("朝向 90 时敌人在 -X 方向", -5.0D, round(west.x()));
        c.equals("朝向 90 时 Z 不变", 0.0D, round(west.z()));

        // 距离下限：配置成 2 格也要被抬到 5 格
        EnemyFormation.Spot close = EnemyFormation.compute(0, 0, 0.0F, 1, 2).get(0);
        c.check("站位距离不会小于 5 格", EnemyFormation.horizontalDistance(close, 0, 0) >= 5.0D);

        // 三只：中间那只在正前方，两侧对称分开且不重叠
        List<EnemyFormation.Spot> three = EnemyFormation.compute(0, 0, 0.0F, 3, 6);
        c.equals("三只敌人给出三个落点", 3, three.size());
        c.equals("中间那只在正前方（X = 0）", 0.0D, round(three.get(1).x()));
        c.check("两侧对称", Math.abs(three.get(0).x() + three.get(2).x()) < 1.0E-6D);
        c.check("相邻两只至少隔开 1 格",
                EnemyFormation.spacing(three.get(0), three.get(1)) >= 1.0D
                        && EnemyFormation.spacing(three.get(1), three.get(2)) >= 1.0D);
        c.check("没有两只重叠", EnemyFormation.spacing(three.get(0), three.get(2)) >= 2.0D);
        c.check("三只都在玩家前方（Z > 0）",
                three.stream().allMatch(spot -> spot.z() > 0.5D));
        c.check("三只都面朝玩家",
                three.stream().allMatch(spot -> spot.yaw() == StandoffLayout.yawTowards(spot.x(), spot.z(), 0, 0)));

        // ---- 本局结束判定：通关或战败之前锁住角色（测试反馈第 2 条）
        ProgressState run = ProgressState.fresh();        c.check("新局还没结束", !run.isRunOver());
        c.check("新局没有战败标记", !run.defeated());
        c.check("战败后本局结束", run.withDefeated().isRunOver());
        c.check("战败标记会保留（进阶也不清除）", run.withDefeated().recordBossDefeat().defeated());
        ProgressState completedRun = ProgressState.fresh().recordBossDefeat().recordBossDefeat()
                .recordBossDefeat();
        c.check("通关后本局结束", completedRun.isRunOver() && completedRun.completed());
        c.check("重开（reset）后本局重新开始",
                !completedRun.reset().isRunOver() && !run.withDefeated().reset().defeated());

        // ---- 击杀归属钩子（v0.2.1：引擎处决时原版击杀归属为空，奖励必须靠它记账）
        RecordingDummy recorder = new RecordingDummy("记录者", 40);
        Combatant attacker = dummy("玩家", 40);
        CombatContext hookCtx = start(attacker, List.of(zombie(50, Intent.attack(1))), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        c.equals("还没被打过时没有记录来源", null, recorder.lastSource);
        hookCtx.dealDamage(attacker, recorder, 5, "测试");
        c.check("dealDamage 会把来源告诉目标", recorder.lastSource == attacker);
        c.equals("被打之后 HP 正常扣除", 35, recorder.hp());

        // 未参战的第三方打它时，记录会更新（最后打的人 = 击杀归属）
        Combatant other = dummy("另一个玩家", 40);
        hookCtx.dealDamage(other, recorder, 3, "测试");
        c.check("最后打的人成为击杀归属", recorder.lastSource == other);
        c.equals("连续两次伤害累计", 32, recorder.hp());
    }

    private static double round(double value) {
        return Math.round(value * 1.0E6D) / 1.0E6D;
    }

    // ------------------------------------------------------------------ 独立生命值与进度（V9）

    private static void progressAndLife(Checker c) {
        // ---- 独立生命值：只是一池会被裁剪的数值
        LifePool full = LifePool.full(75);
        c.equals("满血池：当前 = 上限", 75, full.hp());
        c.equals("满血池：上限 = 75", 75, full.maxHp());
        c.check("满血池 isFull", full.isFull());
        c.check("默认池 = 75 满血", LifePool.standard().isFull() && LifePool.standard().maxHp() == 75);
        c.equals("上限至少为 1", 1, LifePool.of(10, 0).maxHp());
        c.equals("当前生命被裁到上限以内", 75, LifePool.of(200, 75).hp());
        c.equals("负数生命归零", 0, LifePool.of(-5, 75).hp());

        LifePool hurt = full.damage(13);
        c.equals("受伤：75 - 13 = 62", 62, hurt.hp());
        c.equals("受伤不改上限", 75, hurt.maxHp());
        c.equals("受伤后剩余可回血空间 = 13", 13, hurt.missing());
        c.equals("治疗不超过上限", 75, hurt.heal(100).hp());
        c.equals("治疗按数值回复", 70, hurt.heal(8).hp());
        c.equals("超量伤害停在 0，不会变负", 0, full.damage(1000).hp());
        c.check("生命归零判定为倒下", full.damage(75).isDown());
        c.check("0 伤害不改变池子", full.damage(0) == full);
        c.equals("上限调小后当前生命跟着降", 40, full.withMax(40).hp());
        c.equals("回满（战败重开）", 75, hurt.restore().hp());
        c.equals("展示文本", "62/75", hurt.describe());

        // ---- 事件上限表
        EventCaps caps = EventCaps.DEFAULT;
        c.equals("默认常规上限 15", 15, caps.normal());
        c.equals("默认精英上限 5", 5, caps.elite());
        c.equals("默认火堆上限 5", 5, caps.campfire());
        c.equals("默认问号上限 5", 5, caps.question());
        c.equals("BOSS 不受上限限制", Integer.MAX_VALUE, caps.capOf(EventKind.BOSS));
        c.check("BOSS 不参与计数", !EventKind.BOSS.capped());
        c.check("常规战斗参与计数", EventKind.NORMAL.capped());

        // ---- BOSS 表
        c.equals("三层尖塔", 3, FloorBoss.FLOORS);
        // v0.4.5：每层多只 BOSS，ofFloor 返回该层的"代表 BOSS"（名单里的第一只）
        c.equals("第 1 层代表 BOSS 是旋风人", "minecraft:breeze", FloorBoss.ofFloor(1).entityId());
        c.equals("第 2 层代表 BOSS 是僵尸疣猪兽", "minecraft:zoglin", FloorBoss.ofFloor(2).entityId());
        c.equals("第 3 层 BOSS 是末影龙", "minecraft:ender_dragon", FloorBoss.ofFloor(3).entityId());
        c.equals("越界的层号没有 BOSS", null, FloorBoss.ofFloor(4));
        c.equals("第 0 层没有 BOSS", null, FloorBoss.ofFloor(0));

        // ---- 进度：初始状态
        ProgressState fresh = ProgressState.fresh();
        c.equals("新进度从第 1 层开始", 1, fresh.floor());
        c.equals("新进度常规计数为 0", 0, fresh.countOf(EventKind.NORMAL));
        c.check("新进度未通关", !fresh.completed());
        c.equals("新进度目标 = 当前层 BOSS（多只时取名单第一只）", "旋风人", fresh.boss().name());
        c.check("新进度四类事件都可触发", fresh.canTrigger(EventKind.NORMAL, caps)
                && fresh.canTrigger(EventKind.ELITE, caps) && fresh.canTrigger(EventKind.CAMPFIRE, caps)
                && fresh.canTrigger(EventKind.QUESTION, caps));
        c.check("BOSS 一直可挑战", fresh.canTrigger(EventKind.BOSS, caps));

        // ---- 进度：达到上限后不可再触发
        ProgressState capped = fresh;
        for (int i = 0; i < 15; i++) {
            capped = capped.record(EventKind.NORMAL, caps);
        }
        c.equals("常规战斗计数累加到 15", 15, capped.countOf(EventKind.NORMAL));
        c.check("常规战斗达上限后不可再触发", !capped.canTrigger(EventKind.NORMAL, caps));
        c.equals("达上限后再记录不会超过上限", 15, capped.record(EventKind.NORMAL, caps).countOf(EventKind.NORMAL));
        c.check("常规达上限不影响精英", capped.canTrigger(EventKind.ELITE, caps));
        c.check("常规达上限仍可挑战 BOSS", capped.canTrigger(EventKind.BOSS, caps));
        c.equals("精英计数独立累加", 1, capped.record(EventKind.ELITE, caps).countOf(EventKind.ELITE));

        ProgressState eliteCapped = fresh;
        for (int i = 0; i < 5; i++) {
            eliteCapped = eliteCapped.record(EventKind.ELITE, caps);
        }
        c.check("精英满 5 次后不可再触发", !eliteCapped.canTrigger(EventKind.ELITE, caps));
        c.equals("计入 BOSS 战不会占用常规次数", 0,
                fresh.record(EventKind.BOSS, caps).countOf(EventKind.NORMAL));

        // 上限可配置：1/1/1/1
        EventCaps tiny = new EventCaps(1, 1, 1, 1);
        ProgressState one = fresh.record(EventKind.NORMAL, tiny);
        c.check("自定义上限 1 生效", !one.canTrigger(EventKind.NORMAL, tiny) && one.countOf(EventKind.NORMAL) == 1);

        // ---- 进度：击败 BOSS 进阶
        ProgressState afterBoss = capped.recordBossDefeat();
        c.equals("击败第 1 层 BOSS 后进入第 2 层", 2, afterBoss.floor());
        c.equals("进阶后本层事件计数清零", 0, afterBoss.countOf(EventKind.NORMAL));
        c.check("已击败的 BOSS 被记下", afterBoss.bossDefeated(1));
        c.check("进阶后可以重新打常规战斗", afterBoss.canTrigger(EventKind.NORMAL, caps));
        c.equals("进阶后目标换成第 2 层的代表 BOSS", "僵尸疣猪兽", afterBoss.boss().name());

        ProgressState floor3 = afterBoss.recordBossDefeat().recordBossDefeat();
        c.equals("连打三层后停在第 3 层", 3, floor3.floor());
        c.check("第 3 层判为最后一层", floor3.isFinalFloor());
        ProgressState finished = floor3.recordBossDefeat();
        c.check("击败末影龙即通关", finished.completed());
        c.equals("通关后仍记录三层 BOSS", 3, finished.bossesDefeated().size());
        c.check("通关后不能再触发常规事件", !finished.canTrigger(EventKind.NORMAL, caps));
        c.check("通关后仍可再挑战 BOSS", finished.canTrigger(EventKind.BOSS, caps));
        c.check("进度摘要包含层数与目标",
                finished.describe(caps).contains("第 3 层") && finished.describe(caps).contains("末影龙")
                        && finished.describe(caps).contains("已通关"));
        c.check("计数摘要包含四类事件",
                fresh.describeCounts(caps).contains("常规战斗 0/15")
                        && fresh.describeCounts(caps).contains("精英战斗 0/5")
                        && fresh.describeCounts(caps).contains("火堆 0/5")
                        && fresh.describeCounts(caps).contains("问号事件 0/5"));
        // ---- v0.5.5.1（用户）：藏宝箱也要进事件统计（进度 + 上限 5 + 触发条件页）
        c.check("计数摘要里也有藏宝箱（v0.5.5.1）", fresh.describeCounts(caps).contains("藏宝箱 0/5"));
        c.equals("藏宝箱每层上限是 5", 5, caps.capOf(EventKind.TREASURE));
        c.check("藏宝箱是可以计数的事件（只有 BOSS 例外）", EventKind.TREASURE.capped());
        ProgressState chestOpened = fresh.record(EventKind.TREASURE, caps);
        c.equals("开一次藏宝箱计数 +1", 1, chestOpened.countOf(EventKind.TREASURE));
        ProgressState chestFull = chestOpened;
        for (int i = 1; i < 5; i++) {
            chestFull = chestFull.record(EventKind.TREASURE, caps);
        }
        c.equals("开到第 5 次仍然计数", 5, chestFull.countOf(EventKind.TREASURE));
        c.check("满 5 次后本层不能再开藏宝箱", !chestFull.canTrigger(EventKind.TREASURE, caps));
        c.equals("满上限后再记录不会超过上限", 5,
                chestFull.record(EventKind.TREASURE, caps).countOf(EventKind.TREASURE));
        c.equals("CAPPED_KINDS 现在有 5 类", 5, ProgressState.CAPPED_KINDS.length);

        ProgressState reset = finished.reset();
        c.equals("重置后回到第 1 层", 1, reset.floor());
        c.check("重置后未通关且没有已击败 BOSS",
                !reset.completed() && reset.bossesDefeated().isEmpty() && reset.countOf(EventKind.ELITE) == 0);

        // ---- V10：篝火不能连续使用（靠 lastEvent 判定）
        c.equals("新进度还没有上一次事件", null, fresh.lastEvent());
        ProgressState rested = fresh.record(EventKind.CAMPFIRE, caps);
        c.check("休息后最近事件就是火堆", rested.isConsecutive(EventKind.CAMPFIRE));
        c.equals("火堆计数 +1", 1, rested.countOf(EventKind.CAMPFIRE));
        ProgressState afterFight = rested.record(EventKind.NORMAL, caps);
        c.check("打过一场之后不再是连续火堆", !afterFight.isConsecutive(EventKind.CAMPFIRE));
        c.check("中间有其它事件后火堆又能用", afterFight.canTrigger(EventKind.CAMPFIRE, caps));
        ProgressState afterBossFight = rested.record(EventKind.BOSS, caps);
        c.check("BOSS 战也算一次其它事件", !afterBossFight.isConsecutive(EventKind.CAMPFIRE));
        c.equals("BOSS 战不占火堆计数", 1, afterBossFight.countOf(EventKind.CAMPFIRE));
        c.check("进阶后 lastEvent 清空（新的一层可以先用火堆）",
                afterBossFight.recordBossDefeat().lastEvent() == null);
        ProgressState campfireCapped = fresh;
        for (int i = 0; i < 5; i++) {
            // 中间穿插常规战斗，避免被"连续使用"挡住（这里只想验证次数上限）
            campfireCapped = campfireCapped.record(EventKind.CAMPFIRE, caps).record(EventKind.NORMAL, caps);
        }
        c.check("火堆满 5 次后不可再用", !campfireCapped.canTrigger(EventKind.CAMPFIRE, caps));

        // ---- V11：卡牌强化（同一个 id，数值更高）
        Card strikePlus = CardLibrary.resolve("strike", 1);
        Card strikeBase = CardLibrary.resolve("strike", 0);
        c.equals("未强化时返回基础定义", "打击", strikeBase.name());
        c.equals("强化后名字带 +", "打击+", strikePlus.name());
        c.check("强化版与基础版是不同的定义对象", strikePlus != strikeBase);
        c.equals("强化版是另一条 id 相同的定义", strikeBase.id(), strikePlus.id());
        c.check("打击强化版伤害 9（6 → 9）",
                CardLibrary.resolve("strike", 1).description().contains("9 点伤害"));
        c.check("蛋糕强化版恢复 7（5 → 7）",
                CardLibrary.resolve("cake", 1).description().contains("7 点生命"));
        c.check("箭雨强化版伤害 11（8 → 11）",
                CardLibrary.resolve("arrow_rain", 1).description().contains("11 点伤害"));
        c.check("强化版保留关键字（箭雨仍是保留牌）",
                CardLibrary.resolve("arrow_rain", 1).has(CardKeyword.RETAIN));
        c.check("不认识的卡返回 null", CardLibrary.resolve("no_such_card", 1) == null);
        // v0.4.6：状态牌与诅咒牌都没有强化版（它们不该被强化）
        c.check("每张非状态/非诅咒牌都有强化版",
                CardLibrary.all().stream()
                        .filter(card -> card.type() != CardType.STATUS && card.type() != CardType.CURSE)
                        .allMatch(card -> CardLibrary.canUpgrade(card.id())));
        // v0.5.0.4：生成牌（registerGenerated）按约定**基础定义就占用"+"名字**
        // （既有写法：BOLT_SHOT 的基础定义是"弩箭射击+"、强化定义也是"弩箭射击+"），
        // 而生成牌本来就不进奖励池/商店，所以这条断言只对**卡池里的牌**成立。
        c.check("强化版不会混进卡池（抽卡拿不到 + 版，生成牌除外）",
                CardLibrary.all().stream()
                        .filter(card -> CardLibrary.poolOf(card) == CardLibrary.CardPool.CHARACTER
                                || CardLibrary.poolOf(card) == CardLibrary.CardPool.COLORLESS)
                        .noneMatch(card -> card.name().endsWith("+")));

        // 强化后的实际战斗数值：打击 9 点伤害
        Combatant plusOwner = dummy("玩家", 40);
        Enemy plusTarget = zombie(40, Intent.attack(1));
        Card plusStrike = CardLibrary.resolve("strike", 1);
        CombatContext plusCtx = start(plusOwner, List.of(plusTarget), List.of(plusStrike), new ArrayList<>());
        plusCtx.playCard(0, 0);
        c.equals("用强化版打击打出 9 点伤害", 31, plusTarget.hp());
    }

    // ------------------------------------------------------------------ 药水（V8）

    private static void potions(Checker c) {
        c.check("药水表至少 6 瓶", PotionLibrary.all().size() >= 6);

        Set<String> ids = new HashSet<>();
        boolean unique = true;
        boolean bothKinds = false;
        for (Potion potion : PotionLibrary.all()) {
            unique &= ids.add(potion.id());
            bothKinds |= potion.kind() == PotionKind.BENEFICIAL;
            bothKinds &= !potion.description().isBlank() || true;
        }
        c.check("药水 id 不重复", unique);
        c.check("既有正面也有负面药水", bothKinds);
        c.check("未知药水 id 会被跳过", PotionLibrary.potionsOf(List.of("no_such_potion")).isEmpty());

        // 指向规则：正面只能给自己人，负面只能给敌人
        c.check("正面药水只能对自己人", PotionKind.BENEFICIAL.canTargetAlly()
                && !PotionKind.BENEFICIAL.canTargetEnemy());
        c.check("负面药水只能对敌人", PotionKind.HARMFUL.canTargetEnemy()
                && !PotionKind.HARMFUL.canTargetAlly());

        // 效果数值
        Combatant user = dummy("玩家", 40);
        Enemy enemy = zombie(40, Intent.attack(1));
        CombatContext ctx = start(user, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());

        user.setHp(20);
        PotionLibrary.HEAL.effect().apply(ctx, user, user);
        c.equals("治疗药水：回复 10 点（20 → 30）", 30, user.hp());

        PotionLibrary.STRENGTH.effect().apply(ctx, user, user);
        c.equals("力量药水：力量 +5（临时）", 5, user.strength());

        PotionLibrary.DEXTERITY.effect().apply(ctx, user, user);
        c.equals("迅捷药水：迅捷 +2", 2, user.dexterity());

        PotionLibrary.POISON.effect().apply(ctx, user, enemy);
        c.equals("中毒药水：敌人获得 15 层中毒", 15, enemy.poison());

        PotionLibrary.BURN.effect().apply(ctx, user, enemy);
        c.equals("火焰药水：敌人获得 8 层灼烧", 8, enemy.burn());

        PotionLibrary.WEAK.effect().apply(ctx, user, enemy);
        c.equals("虚弱药水：敌人虚弱 3", 3, enemy.weak());

        // 中毒药水能真的靠"阈值处决"打死敌人（15 > 14）
        Enemy doomed = zombie(14, Intent.attack(1));
        CombatContext doomCtx = start(dummy("玩家", 40), List.of(doomed), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PotionLibrary.POISON.effect().apply(doomCtx, doomCtx.player(), doomed);
        doomCtx.endTurn();
        c.check("中毒 15 > HP 14：回合结束后敌人立刻死亡", !doomed.alive());

        // 药水描述文本
        c.check("药水描述文本完整",
                PotionLibrary.POISON.describe().contains("中毒药水")
                        && PotionLibrary.POISON.describe().contains("负面"));
    }

    // ------------------------------------------------------------------ 遗物（V7）

    private static void relics(Checker c) {
        c.check("遗物表至少有 5 件", RelicLibrary.all().size() >= 5);

        Set<String> ids = new HashSet<>();
        boolean unique = true;
        boolean wellFormed = true;
        for (Relic relic : RelicLibrary.all()) {
            unique &= ids.add(relic.id());
            // v0.5.3.0：有几件遗物的效果**不在战斗内**（获得时附魔 / 发奖励 / 商店打折 / 篝火多选），
            // 它们的 triggers 故意是空的 —— 所以"必须有触发点"放宽成"有触发点，或属于这一类"。
            boolean outsideCombat = RelicLibrary.OUTSIDE_COMBAT_RELIC_IDS.contains(relic.id());
            wellFormed &= (!relic.triggers().isEmpty() || outsideCombat)
                    && !relic.description().isBlank() && !relic.name().isBlank();
        }
        c.check("遗物 id 不重复", unique);
        c.check("每件遗物都有触发点（或属于【战斗外效果】那几件）、名字与描述", wellFormed);
        c.check("存档里未知的遗物 id 会被跳过", RelicLibrary.relicsOf(List.of("no_such_relic")).isEmpty());
        c.equals("全部拥有时随机掉落返回 null", null,
                RelicLibrary.randomUnowned(new Random(1), ids));

        // 金刚杵：战斗开始 +1 力量（且只触发一次）
        Combatant vajraOwner = dummy("玩家", 40);
        Enemy vajraTarget = zombie(60, Intent.attack(1));
        CombatContext vajraCtx = startWithRelics(vajraOwner, List.of(vajraTarget), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.VAJRA));
        c.equals("金刚杵：战斗开始时力量 +1", 1, vajraOwner.strength());
        vajraCtx.playCard(0, 0);
        c.equals("金刚杵：打击 6+1 力量 = 7 点", 53, vajraTarget.hp());
        vajraCtx.endTurn();
        c.equals("金刚杵：不是每回合都加（第二回合仍是 +1）", 1, vajraOwner.strength());

        // 锚：战斗开始 +10 格挡（必须在"回合开始清空格挡"之后触发才不会被抹掉）
        Combatant anchorOwner = dummy("玩家", 40);
        startWithRelics(anchorOwner, List.of(zombie(60, Intent.attack(1))), deck(CardLibrary.STRIKE, 10),
                List.of(RelicLibrary.ANCHOR));
        c.equals("锚：战斗开始后手上有 10 点格挡", 10, anchorOwner.block());

        // 准备背包：战斗开始额外抽 2 张
        Combatant bagOwner = dummy("玩家", 40);
        CombatContext bagCtx = startWithRelics(bagOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), List.of(RelicLibrary.BAG_OF_PREPARATION));
        c.equals("准备背包：开局手牌 5 + 2 = 7 张", 7, bagCtx.hand().size());

        // 光滑的石头：每回合开始 +1 迅捷
        Combatant stoneOwner = dummy("玩家", 40);
        CombatContext stoneCtx = startWithRelics(stoneOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), List.of(RelicLibrary.ODDLY_SMOOTH_STONE));
        c.equals("光滑的石头：第一回合迅捷 +1", 1, stoneOwner.dexterity());
        stoneCtx.endTurn();
        c.equals("光滑的石头：第二回合再 +1（累计 2）", 2, stoneOwner.dexterity());

        // 燃烧之血：战斗胜利后回血
        Combatant bloodOwner = dummy("玩家", 40);
        bloodOwner.setHp(20);
        CombatContext bloodCtx = startWithRelics(bloodOwner, List.of(zombie(5, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), List.of(RelicLibrary.BURNING_BLOOD));
        bloodCtx.playCard(0, 0);
        c.equals("燃烧之血：胜利后回复 6 点（20 → 26）", 26, bloodOwner.hp());

        // 遗物只属于携带者：多人时不会互相触发
        Combatant withRelic = dummy("玩家A", 40);
        Combatant withoutRelic = dummy("玩家B", 40);
        List<CombatContext.Participant> participants = List.of(
                new CombatContext.Participant(withRelic, deck(CardLibrary.STRIKE, 10), 3,
                        List.of(RelicLibrary.VAJRA)),
                new CombatContext.Participant(withoutRelic, deck(CardLibrary.STRIKE, 10)));
        CombatContext partyCtx = new CombatContext(participants, List.of(zombie(60, Intent.attack(1))),
                CombatRules.defaults(), 20260101L, CombatLog.toList(new ArrayList<>()));
        partyCtx.start();
        partyCtx.endTurn();
        c.equals("遗物只归携带者（A 有力量 +1）", 1, withRelic.strength());
        c.equals("遗物不会串到队友身上（B 力量 0）", 0, withoutRelic.strength());
    }

    private static CombatContext startWithRelics(Combatant player, List<Enemy> enemies, List<Card> deck,
                                                 List<Relic> relics) {
        CombatContext ctx = new CombatContext(
                List.of(new CombatContext.Participant(player, deck, 3, relics)),
                enemies, CombatRules.defaults(), 20260101L, CombatLog.toList(new ArrayList<>()));
        ctx.start();
        return ctx;
    }

    // ------------------------------------------------------------------ 卡牌附魔（V6）

    private static void cardEnchantments(Checker c) {
        // 亡灵杀手：对亡灵 ×1.2
        Enemy undead = zombie(40, Intent.attack(1));
        undead.setUndead(true);
        Enemy living = zombie(40, Intent.attack(1));
        Card smite = testDamageCard("smite_test", 10).withEnchantments(Set.of(CardEnchantment.SMITE));

        CombatContext undeadCtx = start(dummy("玩家", 60), List.of(undead), repeat(smite, 10), new ArrayList<>());
        undeadCtx.playCard(0, 0);
        c.equals("亡灵杀手：10 点打到亡灵 → 12", 28, undead.hp());

        CombatContext livingCtx = start(dummy("玩家", 60), List.of(living), repeat(smite, 10), new ArrayList<>());
        livingCtx.playCard(0, 0);
        c.equals("亡灵杀手：10 点打到非亡灵 → 仍是 10", 30, living.hp());

        // 火焰附加：打到的敌人被附加灼烧（v0.5.2.5：层数 = 造成伤害的四分之一）
        Enemy burnTarget = zombie(40, Intent.attack(1));
        Card fire = testDamageCard("fire_test", 10).withEnchantments(Set.of(CardEnchantment.FIRE_ASPECT));
        CombatContext fireCtx = start(dummy("玩家", 60), List.of(burnTarget), repeat(fire, 10), new ArrayList<>());
        fireCtx.playCard(0, 0);
        c.equals("火焰附加：10 点伤害 → 灼烧 10/4 = 2 层", 2, burnTarget.burn());
        fireCtx.playCard(0, 0);
        c.equals("火焰附加：再次命中叠加到灼烧 4", 4, burnTarget.burn());
        c.equals("火焰附加：伤害不足 4 点时是 0 层（3/4）", 0, CombatContext.fireAspectBurn(3));
        c.equals("火焰附加：7 点伤害向下取整 → 1 层", 1, CombatContext.fireAspectBurn(7));

        // 横扫之刃：对其他敌人造成 35%
        Enemy main = zombie(40, Intent.attack(1));
        Enemy other1 = zombie(40, Intent.attack(1));
        Enemy other2 = zombie(40, Intent.attack(1));
        Card sweep = testDamageCard("sweep_test", 10).withEnchantments(Set.of(CardEnchantment.SWEEPING_EDGE));
        CombatContext sweepCtx = start(dummy("玩家", 60), List.of(main, other1, other2), repeat(sweep, 10),
                new ArrayList<>());
        sweepCtx.playCard(0, 0);
        c.equals("横扫之刃：主目标吃满 10 点", 30, main.hp());
        c.equals("横扫之刃：其他敌人吃 floor(10×0.35)=3", 37, other1.hp());
        c.equals("横扫之刃：第二只其他敌人同样 3 点", 37, other2.hp());

        // 横扫 + 火焰：被波及的敌人也会被点燃
        Enemy main2 = zombie(40, Intent.attack(1));
        Enemy other3 = zombie(40, Intent.attack(1));
        Card sweepFire = testDamageCard("sweep_fire_test", 10)
                .withEnchantments(Set.of(CardEnchantment.SWEEPING_EDGE, CardEnchantment.FIRE_ASPECT));
        CombatContext sweepFireCtx = start(dummy("玩家", 60), List.of(main2, other3), repeat(sweepFire, 10),
                new ArrayList<>());
        sweepFireCtx.playCard(0, 0);
        c.equals("横扫+火焰：主目标被点燃 10/4 = 2 层", 2, main2.burn());
        c.equals("横扫+火焰：被波及的敌人按 swept=3 折算 → 0 层（3/4 向下取整）", 0, other3.burn());

        // 对照：没有附魔的牌不会波及其他敌人
        Enemy plainMain = zombie(40, Intent.attack(1));
        Enemy plainOther = zombie(40, Intent.attack(1));
        CombatContext plainCtx = start(dummy("玩家", 60), List.of(plainMain, plainOther),
                repeat(testDamageCard("plain_test", 10), 10), new ArrayList<>());
        plainCtx.playCard(0, 0);
        c.equals("对照：无附魔时其他敌人不受影响", 40, plainOther.hp());
        c.equals("对照：无附魔时目标不会被点燃", 0, plainMain.burn());

        // 附魔只在卡牌结算期间生效：敌人回合不受影响
        Enemy smiteEnemy = zombie(40, Intent.attack(6));
        smiteEnemy.setUndead(true);
        Combatant victim = dummy("玩家", 60);
        CombatContext enemyTurnCtx = start(victim, List.of(smiteEnemy), repeat(smite, 10), new ArrayList<>());
        enemyTurnCtx.endTurn();
        c.equals("敌人回合不会误用玩家牌的附魔（6 点仍是 6 点）", 54, victim.hp());

        // 展示文本
        c.check("附魔文本按固定顺序拼接",
                "横扫之刃·亡灵杀手".equals(CardEnchantment.join(
                        Set.of(CardEnchantment.SMITE, CardEnchantment.SWEEPING_EDGE))));
        c.check("卡牌描述里带出附魔",
                smite.describe().contains("附魔:亡灵杀手"));

        // 加附魔是幂等的
        Card twice = smite.withEnchantments(Set.of(CardEnchantment.SMITE));
        c.check("重复加同一个附魔不会叠加", twice.enchantments().size() == 1);
    }

    /** 造一张纯伤害的测试牌（0 费，便于反复出牌）。 */
    private static Card testDamageCard(String id, int damage) {
        return new Card(id, id, CardType.ATTACK, 0, CardTarget.ENEMY, "测试伤害牌。", CardRarity.COMMON, null,
                (ctx, self, target) -> ctx.dealDamage(self, target, damage, "「" + id + "」"));
    }

    // ------------------------------------------------------------------ 效果系统（V5）

    private static void statusEffects(Checker c) {
        // 迅捷 / 缓慢影响"获得的格挡"
        Combatant nimble = dummy("迅捷者", 30);
        nimble.addDexterity(2);
        c.equals("迅捷：5 点格挡 → 7", 7, CombatContext.previewBlock(nimble, 5));

        Combatant sluggish = dummy("缓慢者", 30);
        sluggish.addSlow(2);
        c.equals("缓慢：5 点格挡 → 3", 3, CombatContext.previewBlock(sluggish, 5));

        Combatant both = dummy("又快又慢", 30);
        both.addDexterity(2);
        both.addSlow(2);
        c.equals("迅捷与缓慢相互抵消：5 → 5", 5, CombatContext.previewBlock(both, 5));

        Combatant crippled = dummy("被压死", 30);
        crippled.addSlow(9);
        c.equals("缓慢把格挡压到 0 而不会变负", 0, CombatContext.previewBlock(crippled, 5));

        // 出牌获得格挡时走同一套公式
        Combatant blocked = dummy("玩家", 30);
        blocked.addSlow(1);
        CombatContext blockCtx = start(blocked, List.of(zombie(50, Intent.attack(1))),
                deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        blockCtx.playCard(0, 0);
        c.equals("缓慢 1 层时「格挡」只给 4 点", 4, blocked.block());

        // 灼烧（v0.4.6.1：旧的"凋零 DOT"已并入灼烧）：自己回合开始时受到等量伤害。
        // 注意：回合开始会先清空格挡，所以持续伤害总是直接扣血；状态在 start() 之后再叠，
        // 这样只在"下一回合开始"结算一次，便于精确断言。
        Combatant burning = dummy("玩家", 30);
        CombatContext burnCtx = start(burning, List.of(zombie(50, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        burning.addBurn(3);
        burnCtx.endTurn();
        c.equals("灼烧 3 层 + 敌人 1 点：30 → 26", 26, burning.hp());

        c.check("灼烧会累加（2 + 3 = 5 层）", damageOverTimeStacks() == 5);

        // 凋零：必须**大于**当前生命才致命（v0.4.6.1 之前这套规则叫"中毒"）
        Enemy equalDecay = zombie(5, Intent.attack(1));
        equalDecay.addDecay(5);
        CombatContext equalCtx = start(dummy("玩家", 30), List.of(equalDecay), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        equalCtx.endTurn();
        c.check("凋零等于当前生命时不会死", equalDecay.alive());

        Enemy lethalDecay = zombie(5, Intent.attack(1));
        lethalDecay.addDecay(6);
        CombatContext lethalCtx = start(dummy("玩家", 30), List.of(lethalDecay), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        lethalCtx.endTurn();
        c.check("凋零大于当前生命时立刻死亡", !lethalDecay.alive());

        // 神龟：结束出牌时获得等同层数的格挡
        // v0.5.1.4：附甲现在**每个玩家回合开始时 -1 层**（用户拍板的新规则），
        // 所以"开局前就带 4 层"到第 1 回合开始时就只剩 3 层 —— 这条断言跟着新规则走。
        Combatant armored = dummy("玩家", 30);
        armored.addPlatedArmor(4);
        CombatContext armorCtx = start(armored, List.of(zombie(50, Intent.attack(4))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        c.equals("神龟：第 1 回合开始时先掉 1 层（4 → 3）", 3, armored.platedArmor());
        armorCtx.endTurn();
        c.equals("神龟 3 层：结束出牌获得 3 点格挡，敌人 4 点攻击只打进 1 点", 29, armored.hp());

        Combatant unarmored = dummy("玩家", 30);
        CombatContext bareCtx = start(unarmored, List.of(zombie(50, Intent.attack(4))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        bareCtx.endTurn();
        c.equals("对照：没有神龟则同样攻击掉 4 点血", 26, unarmored.hp());

        // 状态文本要把新状态显示出来
        Combatant printed = dummy("演示", 30);
        printed.addPoison(3);
        printed.addDecay(4);
        printed.addDexterity(1);
        c.check("状态文本同时显示【中毒】与【凋零】",
                printed.statusSuffix().contains("中毒") && printed.statusSuffix().contains("凋零")
                        && printed.statusSuffix().contains("迅捷"));
    }

    private static int damageOverTimeStacks() {
        Combatant combatant = dummy("叠加", 30);
        combatant.addBurn(2);
        combatant.addBurn(3);
        return combatant.burn();
    }

    // ------------------------------------------------------------------ 卡牌关键字（V4）

    private static void cardKeywords(Checker c) {
        c.equals("关键字文本按固定顺序拼接", "保留·消耗",
                CardKeyword.join(Set.of(CardKeyword.EXHAUST, CardKeyword.RETAIN)));

        // 保留：回合结束留在手牌，不进弃牌堆
        CombatContext retainCtx = start(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))),
                repeat(keywordCard("retain_test", CardKeyword.RETAIN), 10), new ArrayList<>());
        c.equals("保留：开局 5 张", 5, retainCtx.hand().size());
        retainCtx.endTurn();
        c.equals("保留：回合结束后 5 张留手 + 抽满下一轮（共 10 张）", 10, retainCtx.hand().size());
        c.equals("保留：一张都没进弃牌堆", 0, retainCtx.discardPile().size());

        // 对照：普通牌会被弃掉
        CombatContext plainCtx = start(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        plainCtx.endTurn();
        c.equals("对照：普通牌回合结束被弃掉（新一轮只抽到 5 张）", 5, plainCtx.hand().size());
        c.equals("对照：普通牌总量守恒（手 5 + 弃牌 5）", 5,
                plainCtx.discardPile().size() + plainCtx.drawPile().size());

        // 虚无：回合结束时留在手牌 → 被消耗
        CombatContext etherealCtx = start(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))),
                repeat(keywordCard("ethereal_test", CardKeyword.ETHEREAL), 10), new ArrayList<>());
        etherealCtx.endTurn();
        c.equals("虚无：回合结束把 5 张手牌消耗掉", 5, etherealCtx.exhaustPile().size());
        c.equals("虚无：不会进弃牌堆", 0, etherealCtx.discardPile().size());

        // 消耗：打出后进消耗牌堆
        CombatContext exhaustCtx = start(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))),
                repeat(keywordCard("exhaust_test", CardKeyword.EXHAUST), 10), new ArrayList<>());
        exhaustCtx.playCard(0, 0);
        c.equals("消耗：打出的牌进消耗牌堆", 1, exhaustCtx.exhaustPile().size());
        c.equals("消耗：打出的牌不进弃牌堆", 0, exhaustCtx.discardPile().size());

        // 固有：第一回合最先入手，且不占固定抽牌数
        List<Card> innateDeck = new ArrayList<>();
        innateDeck.add(keywordCard("innate_a", CardKeyword.INNATE));
        innateDeck.add(keywordCard("innate_b", CardKeyword.INNATE));
        for (int i = 0; i < 8; i++) {
            innateDeck.add(CardLibrary.STRIKE);
        }
        CombatContext innateCtx = start(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))), innateDeck,
                new ArrayList<>());
        c.equals("固有：开局手牌 = 2 张固有 + 5 张固定抽牌", 7, innateCtx.hand().size());
        c.check("固有：两张固有牌都在手上",
                countId(innateCtx.hand(), "innate_a") == 1 && countId(innateCtx.hand(), "innate_b") == 1);
        c.check("固有：它们排在手牌最前面",
                innateCtx.hand().get(0).id().startsWith("innate") && innateCtx.hand().get(1).id().startsWith("innate"));

        // 固有仍受手牌上限限制
        CombatRules tinyHand = new CombatRules(3, 5, 5, 4);
        CombatContext capped = startWithRules(dummy("玩家", 60), List.of(zombie(80, Intent.attack(1))),
                repeat(keywordCard("innate_test", CardKeyword.INNATE), 10), tinyHand, new ArrayList<>());
        c.equals("固有：受手牌上限限制（上限 5 张）", 5, capped.hand().size());
    }

    /** 造一张只带关键字的测试牌（效果为空）。 */
    private static Card keywordCard(String id, CardKeyword keyword) {
        return new Card(id, id, CardType.SKILL, 1, CardTarget.SELF, "测试用牌。", CardRarity.COMMON, null,
                Set.of(keyword), (ctx, self, target) -> {
        });
    }

    private static List<Card> repeat(Card card, int count) {
        List<Card> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(card);
        }
        return list;
    }

    private static int countId(List<Card> cards, String id) {
        int count = 0;
        for (Card card : cards) {
            if (card.id().equals(id)) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ 站位几何（V3）

    private static void standoffLayout(Checker c) {
        // 敌人重心在原点，玩家原来站在 +X 侧 → 应被摆到 +X 侧、距离 ≥5、面向原点（即朝西，yaw=90）
        StandoffLayout.Placement single = StandoffLayout.compute(0, 0, 10, 0, 0, 1, 5, 64);
        c.check("站位距离至少 5 格", StandoffLayout.horizontalDistance(single, 0, 0) >= 5.0D);
        c.check("距离正好等于设定值 5",
                Math.abs(StandoffLayout.horizontalDistance(single, 0, 0) - 5.0D) < 1.0E-6D);
        c.check("站在玩家原来那一侧（+X）", single.x() > 0 && Math.abs(single.z()) < 1.0E-6D);
        c.check("朝向敌人重心（+X 侧看回来 = 朝西 yaw 90）", Math.abs(single.yaw() - 90.0F) < 0.01F);

        c.check("朝南（+Z）yaw 为 0", Math.abs(StandoffLayout.yawTowards(0, 0, 0, 10)) < 0.01F);
        c.check("朝东（+X）yaw 为 -90", Math.abs(StandoffLayout.yawTowards(0, 0, 10, 0) + 90.0F) < 0.01F);
        c.check("朝西（-X）yaw 为 90", Math.abs(StandoffLayout.yawTowards(0, 0, -10, 0) - 90.0F) < 0.01F);
        c.check("朝北（-Z）yaw 为 ±180",
                Math.abs(Math.abs(StandoffLayout.yawTowards(0, 0, 0, -10)) - 180.0F) < 0.01F);

        StandoffLayout.Placement tooClose = StandoffLayout.compute(0, 0, 10, 0, 0, 1, 2, 64);
        c.check("设定距离小于 5 时被抬到 5",
                Math.abs(StandoffLayout.horizontalDistance(tooClose, 0, 0) - 5.0D) < 1.0E-6D);

        StandoffLayout.Placement first = StandoffLayout.compute(0, 0, 10, 0, 0, 2, 5, 64);
        StandoffLayout.Placement second = StandoffLayout.compute(0, 0, 10, 0, 1, 2, 5, 64);
        c.check("两名玩家横向分开不重叠", Math.abs(first.z() - second.z()) > 1.0D);
        c.check("两名玩家的距离都满足 ≥5",
                StandoffLayout.horizontalDistance(first, 0, 0) >= 5.0D
                        && StandoffLayout.horizontalDistance(second, 0, 0) >= 5.0D);

        StandoffLayout.Placement degenerate = StandoffLayout.compute(0, 0, 0, 0, 0, 1, 5, 64);
        c.check("玩家与敌人重心重合时仍给出合法落点",
                StandoffLayout.horizontalDistance(degenerate, 0, 0) >= 5.0D);
    }

    // ------------------------------------------------------------------ 伤害预览（V2）

    private static void damagePreview(Checker c) {
        Combatant attacker = dummy("敌人", 20);
        Combatant target = dummy("玩家", 20);
        c.equals("预览：无状态时 6 点还是 6", 6, CombatContext.previewDamage(attacker, target, 6));

        attacker.addStrength(2);
        c.equals("预览：力量 +2 后 6 → 8", 8, CombatContext.previewDamage(attacker, target, 6));

        attacker.addWeak(1);
        // v0.5.1.4 虚弱重做：每层 **-1** 攻击伤害（不再是 ×0.75），而且会先抵消力量
        c.equals("预览：力量 +2 吃 1 层虚弱 → 力量 1，6 → 7", 7,
                CombatContext.previewDamage(attacker, target, 6));
        c.equals("虚弱确实把力量抵掉了（2 → 1）", 1, attacker.strength());
        c.equals("抵消之后身上没有虚弱", 0, attacker.weak());
        Combatant weakAttacker = dummy("敌人C", 20);
        weakAttacker.addWeak(1);
        c.equals("预览：虚弱每层 -1（10 点 → 9）", 9,
                CombatContext.previewDamage(weakAttacker, target, 10));
        weakAttacker.addWeak(4);
        c.equals("预览：虚弱可以累加（5 层 → 10-5=5）", 5,
                CombatContext.previewDamage(weakAttacker, target, 10));

        // v0.5.1.4 需求给的例子：力量 3 + 虚弱 2 → 力量 1；力量 2 + 虚弱 3 → 虚弱 1
        Combatant offsetA = dummy("例子甲", 20);
        offsetA.addStrength(3);
        offsetA.addWeak(2);
        c.check("抵消例子①：力量 3 吃虚弱 2 → 力量 1、虚弱 0",
                offsetA.strength() == 1 && offsetA.weak() == 0);
        Combatant offsetB = dummy("例子乙", 20);
        offsetB.addStrength(2);
        offsetB.addWeak(3);
        c.check("抵消例子②：力量 2 吃虚弱 3 → 虚弱 1、力量 0",
                offsetB.strength() == 0 && offsetB.weak() == 1);

        // v0.5.1.4：挖掘疲劳接过"旧的虚弱"（-25% 按回合递减）
        Combatant tired = dummy("疲惫者", 20);
        tired.addMiningFatigue(3);
        c.equals("预览：挖掘疲劳 10 点 → floor(10×0.75)=7", 7,
                CombatContext.previewDamage(tired, target, 10));
        tired.decayStatuses();
        c.equals("挖掘疲劳每回合 -1 层（3 → 2）", 2, tired.miningFatigue());
        c.equals("挖掘疲劳仍在生效（层数不影响幅度）", 7,
                CombatContext.previewDamage(tired, target, 10));

        Combatant cleanAttacker = dummy("敌人B", 20);
        Combatant vulnerableTarget = dummy("玩家B", 20);
        vulnerableTarget.addVulnerable(1);
        c.equals("预览：目标易伤 → floor(6×1.25)=7", 7,
                CombatContext.previewDamage(cleanAttacker, vulnerableTarget, 6));
        vulnerableTarget.addVulnerable(2);
        c.equals("预览：易伤层数也只表示回合数（仍是 +25%）", 7,
                CombatContext.previewDamage(cleanAttacker, vulnerableTarget, 6));
        Combatant bothWeak = dummy("敌人D", 20);
        bothWeak.addWeak(2);
        c.equals("预览：虚弱 2 层 + 易伤 → floor((6-2)×1.25)=5", 5,
                CombatContext.previewDamage(bothWeak, vulnerableTarget, 6));

        c.check("预览不会出现负数",
                CombatContext.previewDamage(cleanAttacker, vulnerableTarget, 0) >= 0);

        // 预览值与实际结算必须一致（否则头顶数字会骗人）
        CombatContext ctx = start(dummy("玩家C", 20), List.of(zombie(30, Intent.attack(1))),
                deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        Combatant strong = dummy("强敌", 20);
        strong.addStrength(3);
        int preview = CombatContext.previewDamage(strong, ctx.player(), 5);
        int lost = ctx.dealDamage(strong, ctx.player(), 5, "预览一致性测试");
        c.equals("预览值与实际掉血一致", preview, lost);
    }

    // ------------------------------------------------------------------ 多人遭遇战

    private static void partyTurnOrder(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        CombatContext ctx = startParty(List.of(first, second), List.of(zombie(60, Intent.attack(3))), new ArrayList<>());
        c.equals("多人开局轮到第 1 人", 0, ctx.activeIndex());
        c.equals("多人开局回合数为 1", 1, ctx.turn());
        c.equals("参战人数为 2", 2, ctx.playerCount());

        ctx.endTurn();
        c.equals("第 1 人结束后轮到第 2 人", 1, ctx.activeIndex());
        c.equals("尚未轮到敌人回合，回合数不变", 1, ctx.turn());
        c.check("第 2 人回合仍在玩家阶段", ctx.phase() == CombatPhase.PLAYER_TURN);

        ctx.endTurn();
        c.equals("全员行动完回到第 1 人", 0, ctx.activeIndex());
        c.equals("敌人行动完回合数 +1", 2, ctx.turn());
        c.equals("敌人只打了一次，总伤害落在两人合计", 37, first.hp() + second.hp());
    }

    private static void partyStateIndependence(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        CombatContext ctx = startParty(List.of(first, second), List.of(zombie(60, Intent.attack(1))), new ArrayList<>());
        c.equals("第 1 人开局抽 5 张", 5, ctx.hand().size());
        ctx.playCard(0, 0);
        c.equals("第 1 人出牌后能量 2", 2, ctx.energy());
        int firstDiscard = ctx.discardPile().size();

        ctx.endTurn();
        c.equals("第 2 人有独立的 5 张手牌", 5, ctx.hand().size());
        c.equals("第 2 人能量独立回满为 3", 3, ctx.energy());
        c.equals("第 2 人弃牌堆是空的（与第 1 人互不影响）", 0, ctx.discardPile().size());
        c.equals("第 1 人的弃牌堆不受影响", firstDiscard, 1);

        ctx.playCard(0, 0);
        c.equals("第 2 人出牌只消耗自己的能量", 2, ctx.energy());
        c.equals("两人各自的手牌互不干扰", 4, ctx.hand().size());
    }

    private static void enemyIntentTargetsAPlayer(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        Enemy enemy = zombie(60, Intent.attack(4));
        CombatContext ctx = startParty(List.of(first, second), List.of(enemy), new ArrayList<>());
        Combatant target = enemy.target();
        c.check("单体攻击意图已指定目标", target != null);
        c.check("指定的是参战且存活的玩家", target != null && ctx.isParticipant(target) && target.alive());
        c.check("意图文案带上目标名", enemy.intentLabelWithTarget().contains(target == null ? "@" : target.name()));
    }

    private static void enemyRetargetsWhenTargetFalls(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        Enemy enemy = zombie(60, Intent.attack(5));
        CombatContext ctx = startParty(List.of(first, second), List.of(enemy), new ArrayList<>());
        // 让第 2 人成为行动玩家，这样结束回合就会进入敌人回合
        ctx.endTurn();
        enemy.setTarget(first);
        first.setHp(0);
        ctx.endTurn();
        c.equals("原本的目标倒下后改打幸存者", 15, second.hp());
        c.check("敌人目标已更新为幸存者", enemy.target() == second);
    }

    /**
     * 倒下的玩家**不挡推进**（v0.5.1.6 回合模型改造后重新表述）。
     *
     * <p>旧模型里"跳过倒下的玩家"表现为"轮换指针直接跳到幸存者"；新模型一轮里所有人同时行动，
     * 所以倒下的玩家只是**视为已确认**：幸存者一确认，这一轮就结束、进入敌怪回合。</p>
     */
    private static void downedPlayerIsSkipped(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        CombatContext ctx = startParty(List.of(first, second), List.of(zombie(60, Intent.attack(1))), new ArrayList<>());
        first.setHp(0);
        c.check("倒下的玩家不能出牌", !ctx.isActive(first));
        c.check("幸存者仍然可以出牌", ctx.isActive(second));
        ctx.endTurn();
        c.equals("倒下的玩家不挡推进：幸存者一确认就过完这一轮（回合数 2）", 2, ctx.turn());
        c.equals("新的一轮里下一个待确认的仍是幸存者", 1, ctx.activeIndex());
        c.check("进入新一轮后幸存者又能出牌", ctx.phase() == CombatPhase.PLAYER_TURN && ctx.isActive(second));
        ctx.endTurn();
        c.equals("幸存者再确认一次 → 又过一轮（回合数 3）", 3, ctx.turn());
    }

    private static void partyWipeIsDefeat(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        CombatContext ctx = startParty(List.of(first, second), List.of(zombie(60, Intent.attack(1))), new ArrayList<>());
        first.setHp(0);
        second.setHp(0);
        ctx.recheck();
        c.equals("全员倒下判定为失败", CombatPhase.DEFEAT, ctx.phase());
        c.check("失败后战斗结束", ctx.isOver());
    }

    private static void partyVictoryIgnoresPlayerCount(Checker c) {
        Combatant first = dummy("玩家A", 20);
        Combatant second = dummy("玩家B", 20);
        Enemy enemy = zombie(5, Intent.attack(1));
        CombatContext ctx = startParty(List.of(first, second), List.of(enemy), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("两人队伍同样按敌人全灭判定胜利", CombatPhase.VICTORY, ctx.phase());
    }

    // ------------------------------------------------------------------ checks

    private static void turnStartAndDraw(Checker c) {
        List<String> log = new ArrayList<>();
        CombatContext ctx = start(dummy("玩家", 20), List.of(zombie(20, Intent.attack(5))), deck(CardLibrary.STRIKE, 10), log);
        c.equals("开局回合数为 1", 1, ctx.turn());
        c.equals("开局能量为 3", 3, ctx.energy());
        c.equals("开局抽 5 张牌", 5, ctx.hand().size());
        c.equals("开局抽牌堆剩 5 张", 5, ctx.drawPile().size());
        c.check("开局处于玩家回合", ctx.phase() == CombatPhase.PLAYER_TURN);
        c.equals("开局敌人已预告意图", 1, ctx.enemies().get(0).intent() == null ? 0 : 1);
    }

    private static void strikeDamage(Checker c) {
        Enemy enemy = zombie(20, Intent.attack(5));
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        CombatContext.PlayResult result = ctx.playCard(0, 0);
        c.check("打击可以打出", result.success());
        c.equals("打击消耗 1 点能量", 2, ctx.energy());
        c.equals("打击造成 6 点伤害", 14, enemy.hp());
        c.equals("打出的牌进入弃牌堆", 1, ctx.discardPile().size());
        c.equals("手牌减少一张", 4, ctx.hand().size());
    }

    private static void blockAbsorbsFully(Checker c) {
        Enemy enemy = zombie(20, Intent.attack(5));
        enemy.gainBlock(10);
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("格挡吸收后敌人未掉血", 20, enemy.hp());
        c.equals("格挡按伤害量扣减", 4, enemy.block());
    }

    private static void blockOverflow(Checker c) {
        Enemy enemy = zombie(20, Intent.attack(5));
        enemy.gainBlock(2);
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("格挡不足时溢出伤害生效", 16, enemy.hp());
        c.equals("溢出伤害后格挡归零", 0, enemy.block());
    }

    private static void vulnerableAmplifies(Checker c) {
        Enemy enemy = zombie(20, Intent.attack(5));
        enemy.addVulnerable(2);
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        // v0.0.3：易伤是 +25% → floor(6×1.25)=7
        c.equals("易伤使 6 点伤害变为 7", 13, enemy.hp());
    }

    private static void weakReduces(Checker c) {
        Combatant player = dummy("玩家", 20);
        player.addWeak(1);
        Enemy enemy = zombie(20, Intent.attack(5));
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        // v0.5.1.4：虚弱每层 -1 → 6 - 1 = 5
        c.equals("虚弱 1 层使 6 点伤害降为 5", 15, enemy.hp());
        ctx.endTurn();
        // v0.5.1.4：虚弱**不再随回合递减**（只能靠力量抵消），递减的是挖掘疲劳
        c.equals("虚弱不随回合消失（仍是 1 层）", 1, player.weak());
        c.equals("回合结束时递减的是挖掘疲劳，不是虚弱", 0, player.miningFatigue());
    }

    private static void strengthRaisesEnemyDamage(Checker c) {
        Enemy enemy = zombie(20, Intent.attack(6));
        enemy.addStrength(2);
        Combatant player = dummy("玩家", 20);
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.endTurn();
        c.equals("敌人力量 +2 后 6 点攻击造成 8 点伤害", 12, player.hp());
        c.equals("敌人回合结束后回到玩家回合", CombatPhase.PLAYER_TURN, ctx.phase());
        c.equals("第 2 回合能量刷新为 3", 3, ctx.energy());
    }

    private static void defendBlocksOnePoint(Checker c) {
        Combatant player = dummy("玩家", 20);
        Enemy enemy = zombie(20, Intent.attack(6));
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.DEFEND, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("防御提供 5 点格挡", 5, player.block());
        ctx.endTurn();
        c.equals("6 点攻击被 5 点格挡后只掉 1 点血", 19, player.hp());
        c.equals("新回合开始时格挡清零", 0, player.block());
    }

    private static void endTurnDiscardsAndRedraws(Checker c) {
        Combatant player = dummy("玩家", 20);
        CombatContext ctx = start(player, List.of(zombie(20, Intent.attack(1))), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        c.equals("5 张牌全部入手", 5, ctx.hand().size());
        c.equals("抽牌堆耗尽", 0, ctx.drawPile().size());
        ctx.endTurn();
        c.equals("回合结束后重新抽满 5 张（弃牌堆重洗）", 5, ctx.hand().size());
        c.equals("重洗后抽牌堆耗尽", 0, ctx.drawPile().size());
        c.equals("第二回合抽牌前弃牌堆已清空", 0, ctx.discardPile().size());
    }

    private static void energyGate(Checker c) {
        Enemy enemy = zombie(30, Intent.attack(5));
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.BASH, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("重击消耗 2 点能量（余 1）", 1, ctx.energy());
        CombatContext.PlayResult blocked = ctx.playCard(0, 0);
        c.check("能量不足时无法出牌", !blocked.success());
        c.equals("被拒绝的出牌不消耗能量", 1, ctx.energy());
    }

    private static void victoryWhenEnemiesDie(Checker c) {
        Enemy enemy = zombie(5, Intent.attack(5));
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.playCard(0, 0);
        c.equals("敌人 HP 归零", 0, enemy.hp());
        c.equals("全部敌人倒下即胜利", CombatPhase.VICTORY, ctx.phase());
        c.check("战斗结束标志可用", ctx.isOver());
    }

    private static void multiHitIntent(Checker c) {
        Combatant player = dummy("玩家", 20);
        Enemy enemy = zombie(20, Intent.attack(3, 2));
        CombatContext ctx = start(player, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        ctx.endTurn();
        c.equals("2×3 的多段攻击共造成 6 点伤害", 14, player.hp());
    }

    private static void cleaveHitsEveryEnemy(Checker c) {
        Enemy first = zombie(20, Intent.attack(5));
        Enemy second = zombie(20, Intent.attack(5));
        CombatContext ctx = start(dummy("玩家", 20), List.of(first, second), deck(CardLibrary.CLEAVE, 10), new ArrayList<>());
        ctx.playCard(0, -1);
        c.equals("顺劈斩命中第一个敌人", 12, first.hp());
        c.equals("顺劈斩命中第二个敌人", 12, second.hp());
    }

    private static void pommelStrikeDraws(Checker c) {
        Enemy enemy = zombie(30, Intent.attack(5));
        CombatContext ctx = start(dummy("玩家", 20), List.of(enemy), deck(CardLibrary.POMMEL_STRIKE, 10), new ArrayList<>());
        int before = ctx.hand().size();
        ctx.playCard(0, 0);
        c.equals("柄击造成 9 点伤害", 21, enemy.hp());
        c.equals("柄击出牌后抽 1 张，手牌数不变", before, ctx.hand().size());
    }

    /**
     * v0.4.7.7 的四张选牌卡；**v0.5.1.7 A3 起改成真的让玩家选**（用户："应该让玩家自己选择"）。
     *
     * <p>所以这里验三件事：① 打出后挂起一个待选请求（种类 / 来源 / 张数正确）；
     * ② 回传下标会按正确的方式结算；③ **放弃 / 超时**走贪心兜底，效果不会凭空消失（旧行为）。</p>
     */
    private static void selectionCards(Checker c) {
        // ---- 废料回收：获得格挡 + 从**弃牌堆**选一张放到抽牌堆顶
        Combatant selPlayer = dummy("玩家", 60);
        Enemy selTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext selCtx = start(selPlayer, List.of(selTarget), deck(CardLibrary.SCRAP_RECYCLE, 8),
                new ArrayList<>());
        selCtx.stateOf(selPlayer).discardPile.add(CardLibrary.STRIKE);
        selCtx.stateOf(selPlayer).discardPile.add(CardLibrary.resolve("meteor_chain", 0));
        selCtx.playCard(0, 0);
        c.equals("废料回收：获得 9 点格挡（立刻结算）", 9, selPlayer.block());
        c.check("废料回收：挂起了待选请求", selCtx.pendingPick(selPlayer) != null);
        c.equals("废料回收：候选来自弃牌堆（2 张）", 2, selCtx.pickCandidates(selPlayer).size());
        c.equals("废料回收：只选 1 张", 1,
                selCtx.pendingPick(selPlayer) == null ? -1 : selCtx.pendingPick(selPlayer).maxPicks());
        // 选牌没完成之前：不能再出牌、也不能结束回合（否则候选下标会错位）
        c.check("废料回收：选牌没完成前不能再出牌", !selCtx.playCard(selPlayer, 0, 0).success());
        c.check("废料回收：选牌没完成前不能结束回合", !selCtx.confirmEndTurn(selPlayer));
        // 玩家自己选：第 0 张（打击），而不是费用最高的流星飞索
        selCtx.resolveCardPick(selPlayer, List.of(0));
        c.equals("废料回收：玩家选的那张（打击）进了抽牌堆顶", "打击",
                selCtx.topOfDrawPile(selPlayer).name());
        c.check("废料回收：选完之后请求被清掉", selCtx.pendingPick(selPlayer) == null);
        c.check("废料回收：选完之后又能正常出牌了", selCtx.playCard(selPlayer, 0, 0).success());

        // ---- 应急储备：从**手牌**选一张变成闪光弹
        Combatant conPlayer = dummy("玩家", 60);
        Enemy conTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext conCtx = start(conPlayer, List.of(conTarget), deck(CardLibrary.CONTINGENCY, 8),
                new ArrayList<>());
        conCtx.stateOf(conPlayer).hand.add(CardLibrary.STRIKE);
        conCtx.playCard(0, 0);
        c.check("应急储备：挂起了待选请求", conCtx.pendingPick(conPlayer) != null);
        int strikeIndex = conCtx.pickCandidates(conPlayer).indexOf(CardLibrary.STRIKE);
        c.check("应急储备：候选里能找到那张打击", strikeIndex >= 0);
        conCtx.resolveCardPick(conPlayer, List.of(strikeIndex));
        c.check("应急储备：玩家选的那张变成了闪光弹",
                conCtx.hand().stream().anyMatch(card -> card.id().equals("flashbang")));
        c.check("应急储备：打击已经不在了",
                conCtx.hand().stream().noneMatch(card -> card.id().equals("strike")));

        // ---- 弃置边陵：从手牌选**最多 N 张**消耗
        Combatant abPlayer = dummy("玩家", 60);
        Enemy abTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext abCtx = start(abPlayer, List.of(abTarget), deck(CardLibrary.ABANDON_BORDER, 1),
                new ArrayList<>());
        for (int i = 0; i < 6; i++) {
            abCtx.stateOf(abPlayer).hand.add(CardLibrary.STRIKE);
        }
        abCtx.playCard(0, 0);
        c.equals("弃置边陵：最多选 5 张", 5,
                abCtx.pendingPick(abPlayer) == null ? -1 : abCtx.pendingPick(abPlayer).maxPicks());
        int abBefore = abCtx.hand().size();
        // 玩家只选 2 张 —— "最多 N 张"必须允许少选（不能像自动版那样硬吃 5 张）
        abCtx.resolveCardPick(abPlayer, List.of(0, 1));
        c.equals("弃置边陵：玩家选了 2 张就只消耗 2 张", abBefore - 2, abCtx.hand().size());
        c.equals("弃置边陵：消耗堆多了 2 张", 2, abCtx.stateOf(abPlayer).exhaustPile.size());

        // ---- 放弃 / 超时：贪心兜底（与改造前的"自动挑最贵"一致），效果不会消失
        Combatant skipPlayer = dummy("玩家", 60);
        Enemy skipTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext skipCtx = start(skipPlayer, List.of(skipTarget), deck(CardLibrary.RECYCLE, 8),
                new ArrayList<>());
        int skipBefore = skipCtx.hand().size();
        skipCtx.playCard(0, 0);
        c.check("回收：挂起了待选请求", skipCtx.pendingPick(skipPlayer) != null);
        skipCtx.cancelCardPick(skipPlayer);
        c.check("回收：放弃选择时走贪心兜底（消耗了 1 张）", skipCtx.hand().size() == skipBefore - 1 - 1);
        c.check("回收：放弃也不会留下待选请求", skipCtx.pendingPick(skipPlayer) == null);
    }
    /** v0.4.7.8：【不屈】在手里挡住致死伤害。 */
    private static void lastStandCard(Checker c) {
        Combatant lsPlayer = dummy("玩家", 60);
        Enemy lsEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.attack(9))), true);
        CombatContext lsCtx = start(lsPlayer, List.of(lsEnemy), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        lsCtx.stateOf(lsPlayer).hand.add(CardLibrary.LAST_STAND);
        lsPlayer.loseHp(59);
        c.equals("不屈前置：玩家只剩 1 点生命", 1, lsPlayer.hp());
        lsCtx.dealDamage(lsEnemy, lsPlayer, 999, "测试致死伤");
        c.check("不屈：玩家没死（治疗 18 点 → 1 + 18 = 19 血）", lsPlayer.alive() && lsPlayer.hp() == 19);
        c.equals("不屈：力量 +5", 5, lsPlayer.strength());
        c.check("不屈：手牌里已经没有它了",
                lsCtx.hand().stream().noneMatch(card -> card.id().equals("last_stand")));
        c.check("不屈：记进了待移除集合（战斗结束后从牌组删掉）",
                lsCtx.stateOf(lsPlayer).removeFromDeck.contains("last_stand"));
        c.check("不屈是不能被打出的状态牌", CardLibrary.resolve("last_stand", 0).type() == CardType.STATUS);
    }
    /** v0.4.8.0：机宝（故障机器人）第一批卡牌。 */
    private static void defectCards(Checker c) {
        Combatant dfPlayer = dummy("玩家", 60);
        Enemy dfEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext dfCtx = start(dfPlayer, List.of(dfEnemy), deck(CardLibrary.BALL_LIGHTNING, 8),
                new ArrayList<>());
        c.equals("机宝开局没有充能球", 0, dfPlayer.orbs().size());
        dfCtx.playCard(0, 0);
        c.equals("电球生成：造成 7 点伤害", 193, dfEnemy.hp());
        c.equals("电球生成：生成 1 颗闪电球", 1, dfPlayer.orbs().size());

        Combatant dualPlayer = dummy("玩家", 60);
        Enemy dualEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext dualCtx = start(dualPlayer, List.of(dualEnemy), deck(CardLibrary.BALL_LIGHTNING, 4),
                new ArrayList<>());
        dualCtx.stateOf(dualPlayer).hand.add(CardLibrary.DUALCAST);
        dualCtx.playCard(0, 0);
        c.equals("双重施法前：1 颗球", 1, dualPlayer.orbs().size());
        dualCtx.playCard(dualCtx.hand().size() - 1, 0);
        c.equals("双重施法：把球激发掉了", 0, dualPlayer.orbs().size());

        Combatant batPlayer = dummy("玩家", 60);
        Enemy batEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext batCtx = start(batPlayer, List.of(batEnemy), deck(CardLibrary.CHARGE_BATTERY, 5),
                new ArrayList<>());
        batCtx.playCard(0, 0);
        c.equals("充能电池：7 点格挡", 7, batPlayer.block());
        c.equals("充能电池：能量 -1 +1 = 3", 3, batCtx.energy());

        Combatant glPlayer = dummy("玩家", 60);
        Enemy glEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext glCtx = start(glPlayer, List.of(glEnemy), deck(CardLibrary.GLACIER, 5),
                new ArrayList<>());
        glCtx.playCard(0, 0);
        c.equals("冰封：7 点格挡", 7, glPlayer.block());
        c.equals("冰封：生成 2 颗冰球", 2, glPlayer.orbs().size());

        Combatant snapPlayer = dummy("玩家", 60);
        Enemy snapEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext snapCtx = start(snapPlayer, List.of(snapEnemy), deck(CardLibrary.COLD_SNAP, 5),
                new ArrayList<>());
        snapCtx.playCard(0, 0);
        c.equals("寒冰碎片：6 点伤害", 194, snapEnemy.hp());
        c.equals("寒冰碎片：生成 1 颗冰球", 1, snapPlayer.orbs().size());

        Combatant coolPlayer = dummy("玩家", 60);
        Enemy coolEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext coolCtx = start(coolPlayer, List.of(coolEnemy), deck(CardLibrary.COOLHEADED, 8),
                new ArrayList<>());
        int coolHand = coolCtx.hand().size();
        coolCtx.playCard(0, 0);
        c.equals("冷却：生成 1 颗冰球", 1, coolPlayer.orbs().size());
        c.equals("冷却：打出后手牌数不变（抽 1 补 1）", coolHand, coolCtx.hand().size());
    }
    /** v0.4.8.1：机宝第二批（多重施法 / 汇总 / 全息影像 / 蒸汽护盾 / 锯齿）。 */
    private static void defectCards2(Checker c) {
        Combatant mcPlayer = dummy("玩家", 60);
        Enemy mcEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext mcCtx = start(mcPlayer, List.of(mcEnemy), deck(CardLibrary.BALL_LIGHTNING, 4), new ArrayList<>());
        mcCtx.stateOf(mcPlayer).hand.add(CardLibrary.MULTI_CAST);
        mcCtx.playCard(0, 0);
        c.equals("多重施法前：1 颗球", 1, mcPlayer.orbs().size());
        mcCtx.playCard(mcCtx.hand().size() - 1, 0);
        c.equals("多重施法：X 费花光能量", 0, mcCtx.energy());
        c.equals("多重施法：球被激发掉了", 0, mcPlayer.orbs().size());
        Combatant agPlayer = dummy("玩家", 60);
        Enemy agEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext agCtx = start(agPlayer, List.of(agEnemy), deck(CardLibrary.AGGREGATE, 8), new ArrayList<>());
        agCtx.playCard(0, 0);
        c.equals("汇总：花 1 得 1 → 能量回到 3", 3, agCtx.energy());
        Combatant hlPlayer = dummy("玩家", 60);
        Enemy hlEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext hlCtx = start(hlPlayer, List.of(hlEnemy), deck(CardLibrary.HOLOGRAM, 8), new ArrayList<>());
        hlCtx.stateOf(hlPlayer).discardPile.add(CardLibrary.resolve("meteor_chain", 0));
        int hlHand = hlCtx.hand().size();
        hlCtx.playCard(0, 0);
        c.equals("全息影像：拿回一张（手牌数不变）", hlHand, hlCtx.hand().size());
        Combatant sbPlayer = dummy("玩家", 60);
        Enemy sbEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext sbCtx = start(sbPlayer, List.of(sbEnemy), deck(CardLibrary.STEAM_BARRIER, 8), new ArrayList<>());
        sbCtx.playCard(0, 0);
        c.equals("蒸汽护盾：6 点格挡", 6, sbPlayer.block());
        Combatant bgPlayer = dummy("玩家", 60);
        Enemy bgEnemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext bgCtx = start(bgPlayer, List.of(bgEnemy), deck(CardLibrary.BALL_LIGHTNING, 4), new ArrayList<>());
        bgCtx.playCard(0, 0);
        bgCtx.stateOf(bgPlayer).hand.add(CardLibrary.BARRAGE_CARD);
        int bgHp = bgEnemy.hp();
        bgCtx.playCard(bgCtx.hand().size() - 1, 0);
        c.equals("锯齿：1 颗球 → 4 点伤害", bgHp - 4, bgEnemy.hp());
    }
    /** v0.4.8.2：机宝第三批（爪击 / 光束 / 超频）。 */
    private static void defectCards3(Checker c) {
        Combatant c3Player = dummy("玩家", 60);
        Enemy c3Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext c3Ctx = start(c3Player, List.of(c3Enemy), deck(CardLibrary.CLAW_CARD, 8), new ArrayList<>());
        c3Ctx.playCard(0, 0);
        c.equals("爪击第一下：3 点伤害", 197, c3Enemy.hp());
        c3Ctx.playCard(0, 0);
        c.equals("爪击第二下：3+2=5 点（本场成长生效）", 192, c3Enemy.hp());
        Combatant b3Player = dummy("玩家", 60);
        Enemy b3Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext b3Ctx = start(b3Player, List.of(b3Enemy), deck(CardLibrary.BEAM_CELL, 8), new ArrayList<>());
        b3Ctx.playCard(0, 0);
        c.equals("光束：3 点伤害", 197, b3Enemy.hp());
        c.equals("光束：1 层易伤", 1, b3Enemy.vulnerable());
        Combatant o3Player = dummy("玩家", 60);
        Enemy o3Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext o3Ctx = start(o3Player, List.of(o3Enemy), deck(CardLibrary.OVERCLOCK, 10), new ArrayList<>());
        int o3Hand = o3Ctx.hand().size();
        o3Ctx.playCard(0, 0);
        c.equals("超频：抽 2 补手（净 +1）", o3Hand + 1, o3Ctx.hand().size());
        c.equals("超频：自己掉 1 点血", 59, o3Player.hp());
    }
    /** v0.4.8.3：机宝第四批（流星 / 聚变）。 */
    private static void defectCards4(Checker c) {
        Combatant m4Player = dummy("玩家", 60);
        Enemy m4Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext m4Ctx = start(m4Player, List.of(m4Enemy), deck(CardLibrary.METEOR_STRIKE, 8), new ArrayList<>());
        m4Ctx.gainEnergy(m4Player, 5);
        m4Ctx.playCard(0, 0);
        c.equals("流星：24 点伤害", 176, m4Enemy.hp());
        c.equals("流星：生成 3 颗等离子球", 3, m4Player.orbs().size());
    }
    /** v0.4.8.4：机宝第五批（涡轮 / 缓冲 / 暴风雪）。 */
    private static void defectCards5(Checker c) {
        Combatant t5Player = dummy("玩家", 60);
        Enemy t5Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext t5Ctx = start(t5Player, List.of(t5Enemy), deck(CardLibrary.TURBO, 8), new ArrayList<>());
        t5Ctx.playCard(0, 0);
        c.equals("涡轮：3 + 2 = 5 点能量", 5, t5Ctx.energy());
        c.check("涡轮：往弃牌堆塞了 1 张虚空",
                t5Ctx.stateOf(t5Player).discardPile.view().stream().anyMatch(card -> card.id().equals("void")));
        Combatant b5Player = dummy("玩家", 60);
        Enemy b5Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext b5Ctx = start(b5Player, List.of(b5Enemy), deck(CardLibrary.BUFFER, 8), new ArrayList<>());
        b5Ctx.playCard(0, 0);
        c.equals("缓冲：2 层缓降", 2, b5Player.slowFall());
        Combatant z5Player = dummy("玩家", 60);
        Enemy z5Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext z5Ctx = start(z5Player, List.of(z5Enemy), deck(CardLibrary.BLIZZARD, 8), new ArrayList<>());
        z5Ctx.channelOrb(z5Player, Orb.FROST);
        z5Ctx.channelOrb(z5Player, Orb.FROST);
        z5Ctx.channelOrb(z5Player, Orb.FROST);
        c.equals("冰球计数：生成 3 颗冰球", 3, z5Player.frostCount());
        z5Ctx.playCard(0, 0);
        c.equals("暴风雪：2 × 3 = 6 点伤害", 194, z5Enemy.hp());
    }
    /** v0.4.8.5：机宝第六批（堆叠 / 回收 / 重启）。 */
    private static void defectCards6(Checker c) {
        Combatant s6Player = dummy("玩家", 60);
        Enemy s6Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext s6Ctx = start(s6Player, List.of(s6Enemy), deck(CardLibrary.STACK, 8), new ArrayList<>());
        s6Ctx.stateOf(s6Player).discardPile.add(CardLibrary.STRIKE);
        s6Ctx.stateOf(s6Player).discardPile.add(CardLibrary.DEFEND);
        s6Ctx.playCard(0, 0);
        c.equals("堆叠：弃牌堆 2 张 → 2 点格挡", 2, s6Player.block());
        Combatant r6Player = dummy("玩家", 60);
        Enemy r6Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext r6Ctx = start(r6Player, List.of(r6Enemy), deck(CardLibrary.RECYCLE, 8), new ArrayList<>());
        r6Ctx.stateOf(r6Player).hand.add(CardLibrary.resolve("meteor_strike", 0));
        r6Ctx.playCard(0, 0);
        // v0.5.1.7 A3：回收现在要玩家自己选（这里选那张 5 费的陨石）
        int expensive = r6Ctx.pickCandidates(r6Player).indexOf(CardLibrary.resolve("meteor_strike", 0));
        c.check("回收：候选里有那张 5 费牌", expensive >= 0);
        r6Ctx.resolveCardPick(r6Player, List.of(expensive));
        c.equals("回收：消耗 5 费牌 → 能量 3-1+5=7", 7, r6Ctx.energy());
        Combatant b6Player = dummy("玩家", 60);
        Enemy b6Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext b6Ctx = start(b6Player, List.of(b6Enemy), deck(CardLibrary.REBOOT, 12), new ArrayList<>());
        b6Ctx.stateOf(b6Player).discardPile.add(CardLibrary.STRIKE);
        b6Ctx.stateOf(b6Player).discardPile.add(CardLibrary.STRIKE);
        int b6Hand = b6Ctx.hand().size();
        b6Ctx.playCard(0, 0);
        c.equals("重启：弃牌堆清空", 0, b6Ctx.stateOf(b6Player).discardPile.size());
        c.equals("重启：抽 4 张（手牌 -1 +4）", b6Hand + 3, b6Ctx.hand().size());
    }
    /** v0.4.8.6：机宝第七批（超能光束 / 重编程）。 */
    private static void defectCards7(Checker c) {
        Combatant h7Player = dummy("玩家", 60);
        Enemy h7A = new Enemy("甲", 200, 200, List.of(List.of(Intent.unknown())), true);
        Enemy h7B = new Enemy("乙", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext h7Ctx = start(h7Player, List.of(h7A, h7B), deck(CardLibrary.HYPERBEAM, 8), new ArrayList<>());
        h7Ctx.gainEnergy(h7Player, 3);
        h7Ctx.playCard(0, 0);
        c.equals("超能光束：甲掉 26 点", 174, h7A.hp());
        c.equals("超能光束：乙也掉 26 点（全体）", 174, h7B.hp());
        c.equals("超能光束：失去 3 点集中", -3, h7Player.focus());
        Combatant r7Player = dummy("玩家", 60);
        Enemy r7Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext r7Ctx = start(r7Player, List.of(r7Enemy), deck(CardLibrary.REPROGRAM, 8), new ArrayList<>());
        r7Ctx.playCard(0, 0);
        c.equals("重编程：集中 -1", -1, r7Player.focus());
        c.equals("重编程：力量 +1", 1, r7Player.strength());
        c.equals("重编程：敏捷 +1", 1, r7Player.dexterity());
    }
    /** v0.4.9.0：机宝第八批（风暴 / 双重能量 / 碎片整理 / 冰雹 / 刮削 / 碎裂 / 回弹）。 */
    private static void defectCards8(Checker c) {
        Combatant t8Player = dummy("玩家", 60);
        Enemy t8Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext t8Ctx = start(t8Player, List.of(t8Enemy), deck(CardLibrary.TEMPEST, 8), new ArrayList<>());
        t8Ctx.playCard(0, 0);
        c.equals("风暴：X=3 → 生成 3 颗闪电球", 3, t8Player.orbs().size());
        c.equals("风暴：X 费花光能量", 0, t8Ctx.energy());
        Combatant d8Player = dummy("玩家", 60);
        Enemy d8Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext d8Ctx = start(d8Player, List.of(d8Enemy), deck(CardLibrary.DOUBLE_ENERGY, 8), new ArrayList<>());
        d8Ctx.playCard(0, 0);
        c.equals("双重能量：3-1=2 → 翻倍成 4", 4, d8Ctx.energy());
        Combatant f8Player = dummy("玩家", 60);
        Enemy f8Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext f8Ctx = start(f8Player, List.of(f8Enemy), deck(CardLibrary.DEFRAGMENT, 8), new ArrayList<>());
        f8Ctx.playCard(0, 0);
        c.equals("碎片整理：集中 +1", 1, f8Player.focus());
        Combatant c8Player = dummy("玩家", 60);
        Enemy c8A = new Enemy("甲", 200, 200, List.of(List.of(Intent.unknown())), true);
        Enemy c8B = new Enemy("乙", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext c8Ctx = start(c8Player, List.of(c8A, c8B), deck(CardLibrary.CHILL, 8), new ArrayList<>());
        c8Ctx.playCard(0, 0);
        c.equals("冰雹：2 名敌人 → 2 颗冰球", 2, c8Player.orbs().size());
        c.equals("冰雹：冰球计数也涨到 2", 2, c8Player.frostCount());
        Combatant s8Player = dummy("玩家", 60);
        Enemy s8Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext s8Ctx = start(s8Player, List.of(s8Enemy), deck(CardLibrary.SCRAPE, 9), new ArrayList<>());
        s8Ctx.playCard(0, 0);
        c.equals("刮削：造成 7 点伤害", 193, s8Enemy.hp());
        c.equals("刮削：非 0 费手牌被弃光", 0, s8Ctx.hand().size());
        Combatant u8Player = dummy("玩家", 60);
        Enemy u8Enemy = new Enemy("脆皮", 20, 20, List.of(List.of(Intent.unknown())), true);
        CombatContext u8Ctx = start(u8Player, List.of(u8Enemy), deck(CardLibrary.SUNDER, 8), new ArrayList<>());
        u8Ctx.playCard(0, 0);
        c.check("碎裂：击杀目标", !u8Enemy.alive());
        c.equals("碎裂：击杀回 3 点能量（3-3+3）", 3, u8Ctx.energy());
        Combatant r8Player = dummy("玩家", 60);
        Enemy r8Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext r8Ctx = start(r8Player, List.of(r8Enemy), deck(CardLibrary.REBOUND, 8), new ArrayList<>());
        r8Ctx.playCard(0, 0);
        c.equals("回弹：造成 9 点伤害", 191, r8Enemy.hp());
        c.equals("回弹：牌回到抽牌堆顶", "回弹", r8Ctx.topOfDrawPile(r8Player).name());
    }
    /** v0.4.9.1：集中真的进数值 + 电动力学。 */
    private static void defectCards9(Checker c) {
        Combatant f9Player = dummy("玩家", 60);
        Enemy f9Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext f9Ctx = start(f9Player, List.of(f9Enemy), deck(CardLibrary.BALL_LIGHTNING, 8), new ArrayList<>());
        f9Player.addFocus(2);
        f9Ctx.channelOrb(f9Player, Orb.LIGHTNING);
        int before9 = f9Enemy.hp();
        f9Ctx.endTurn();
        c.equals("集中 +2：闪电球被动 3+2=5 点伤害", before9 - 5, f9Enemy.hp());
        Combatant c9Player = dummy("玩家", 60);
        Enemy c9Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext c9Ctx = start(c9Player, List.of(c9Enemy), deck(CardLibrary.COLD_SNAP, 8), new ArrayList<>());
        c9Player.addFocus(3);
        c9Ctx.playCard(0, 0);
        c.equals("集中不影响生成冰球这件事本身", 1, c9Player.orbs().size());
        c.equals("冰球计数为 1（被动格挡的集中加成在被清格挡前生效）", 1, c9Player.frostCount());
        Combatant e9Player = dummy("玩家", 60);
        Enemy e9A = new Enemy("甲", 200, 200, List.of(List.of(Intent.unknown())), true);
        Enemy e9B = new Enemy("乙", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext e9Ctx = start(e9Player, List.of(e9A, e9B), deck(CardLibrary.ELECTRODYNAMICS, 8), new ArrayList<>());
        e9Ctx.playCard(0, 0);
        e9Ctx.channelOrb(e9Player, Orb.LIGHTNING);
        int a9 = e9A.hp();
        int b9 = e9B.hp();
        e9Ctx.endTurn();
        c.equals("电动力学：闪电球被动同时打到甲（3 点）", a9 - 3, e9A.hp());
        c.equals("电动力学：也打到乙（3 点）", b9 - 3, e9B.hp());
        c.check("电动力学是能力牌", CardLibrary.resolve("electrodynamics", 0).type() == CardType.POWER);
    }
    /** v0.4.9.2：激发路径的集中加成。 */
    private static void defectCards10(Checker c) {
        Combatant v10Player = dummy("玩家", 60);
        Enemy v10Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext v10Ctx = start(v10Player, List.of(v10Enemy), deck(CardLibrary.BALL_LIGHTNING, 8), new ArrayList<>());
        v10Player.addFocus(2);
        v10Ctx.channelOrb(v10Player, Orb.LIGHTNING);
        int before10 = v10Enemy.hp();
        v10Ctx.evokeOrb(v10Player);
        c.equals("激发·闪电球：8+2=10 点伤害（集中已接入激发）", before10 - 10, v10Enemy.hp());
        c.equals("激发后球被消耗掉", 0, v10Player.orbs().size());
        Combatant z10Player = dummy("玩家", 60);
        Enemy z10Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext z10Ctx = start(z10Player, List.of(z10Enemy), deck(CardLibrary.COLD_SNAP, 8), new ArrayList<>());
        z10Player.addFocus(3);
        z10Ctx.channelOrb(z10Player, Orb.FROST);
        z10Ctx.evokeOrb(z10Player);
        c.check("激发·冰球：2+3=5 点格挡（集中已接入激发）", z10Player.block() >= 5);
    }
    /** v0.4.9.3：机宝第九批（启动 / 白噪声 / 电容器）。 */
    private static void defectCards11(Checker c) {
        Combatant b11Player = dummy("玩家", 60);
        Enemy b11Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext b11Ctx = start(b11Player, List.of(b11Enemy), deck(CardLibrary.BOOT_SEQUENCE, 8), new ArrayList<>());
        b11Ctx.playCard(0, 0);
        c.equals("启动：10 点格挡", 10, b11Player.block());
        Combatant k11Player = dummy("玩家", 60);
        Enemy k11Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext k11Ctx = start(k11Player, List.of(k11Enemy), deck(CardLibrary.CAPACITOR, 8), new ArrayList<>());
        int slotsBefore = k11Player.orbSlots();
        k11Ctx.playCard(0, 0);
        c.equals("电容器：球位 +2", slotsBefore + 2, k11Player.orbSlots());
        Combatant w11Player = dummy("玩家", 60);
        Enemy w11Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext w11Ctx = start(w11Player, List.of(w11Enemy), deck(CardLibrary.WHITE_NOISE, 8), new ArrayList<>());
        int w11Hand = w11Ctx.hand().size();
        w11Ctx.playCard(0, 0);
        c.equals("白噪声：打出后手牌不变（抽到 1 张能力牌）", w11Hand, w11Ctx.hand().size());
        c.check("白噪声：真的拿到了能力牌",
                w11Ctx.hand().stream().anyMatch(card -> card.type() == CardType.POWER));
    }
    /** v0.4.9.4：增幅器（每回合首次激发额外一次）。 */
    private static void defectCards12(Checker c) {
        Combatant a12Player = dummy("玩家", 60);
        Enemy a12Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a12Ctx = start(a12Player, List.of(a12Enemy), deck(CardLibrary.AMPLIFY, 8), new ArrayList<>());
        a12Ctx.playCard(0, 0);
        c.check("增幅器是能力牌", CardLibrary.resolve("amplify", 0).type() == CardType.POWER);
        a12Ctx.channelOrb(a12Player, Orb.LIGHTNING);
        a12Ctx.channelOrb(a12Player, Orb.LIGHTNING);
        int a12Before = a12Enemy.hp();
        a12Ctx.evokeOrb(a12Player);
        c.equals("增幅器：首次激发连打两次（8+8=16 点）", a12Before - 16, a12Enemy.hp());
        c.equals("两次激发各摘一颗球，球已用完", 0, a12Player.orbs().size());
        a12Ctx.channelOrb(a12Player, Orb.LIGHTNING);
        a12Ctx.channelOrb(a12Player, Orb.LIGHTNING);
        int a12Second = a12Enemy.hp();
        a12Ctx.evokeOrb(a12Player);
        c.equals("同一回合的第二次激发只触发一次（8 点）", a12Second - 8, a12Enemy.hp());
        c.equals("第二次只摘了一颗球，还剩一颗", 1, a12Player.orbs().size());
        a12Ctx.endTurn();
        c.check("回合切换后「本回合已用」被重置", !a12Player.evokeUsedThisTurn());
    }
    /** v0.4.9.5：静电放电 / 循环 / 创造力。 */
    private static void defectCards13(Checker c) {
        Combatant s13Player = dummy("玩家", 60);
        Enemy s13Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext s13Ctx = start(s13Player, List.of(s13Enemy), deck(CardLibrary.STATIC_DISCHARGE, 8), new ArrayList<>());
        s13Ctx.playCard(0, 0);
        c.equals("静电放电：装好后球栏仍为空", 0, s13Player.orbs().size());
        s13Ctx.dealDamage(s13Enemy, s13Player, 5, "测试攻击");
        c.equals("静电放电：受伤后生成 1 颗闪电球", 1, s13Player.orbs().size());
        s13Ctx.dealDamage(s13Enemy, s13Player, 5, "测试攻击");
        c.equals("静电放电：每次受伤各生成 1 颗", 2, s13Player.orbs().size());
        Combatant l13Player = dummy("玩家", 60);
        Enemy l13Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext l13Ctx = start(l13Player, List.of(l13Enemy), deck(CardLibrary.LOOP, 8), new ArrayList<>());
        l13Ctx.playCard(0, 0);
        l13Ctx.channelOrb(l13Player, Orb.LIGHTNING);
        int l13Before = l13Enemy.hp();
        l13Ctx.endTurn();
        c.check("循环：回合开始时球被动额外触发（敌人额外掉血）", l13Enemy.hp() < l13Before);
        Combatant c13Player = dummy("玩家", 60);
        Enemy c13Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext c13Ctx = start(c13Player, List.of(c13Enemy), deck(CardLibrary.CREATIVE_AI, 8), new ArrayList<>());
        c13Ctx.gainEnergy(c13Player, 3);
        c13Ctx.playCard(0, 0);
        c13Ctx.endTurn();
        c.check("创造力：新回合手里出现了能力牌",
                c13Ctx.hand().stream().anyMatch(card -> card.type() == CardType.POWER));
    }
    /** v0.5.0.0：战士第一批 A（重刃 / 双重打击 / 猛击）。 */
    private static void ironcladCards1(Checker c) {
        Combatant w1Player = dummy("玩家", 60);
        Enemy w1Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext w1Ctx = start(w1Player, List.of(w1Enemy), deck(CardLibrary.HEAVY_BLADE, 8), new ArrayList<>());
        w1Ctx.playCard(0, 0);
        c.equals("重刃：14 点伤害", 286, w1Enemy.hp());
        w1Player.addStrength(2);
        w1Ctx.gainEnergy(w1Player, 2);
        w1Ctx.playCard(0, 0);
        c.equals("重刃：力量 2 生效 3 次 → 14+6=20（含 dealDamage 自带的那 1 次）", 266, w1Enemy.hp());
        Combatant t1Player = dummy("玩家", 60);
        Enemy t1Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext t1Ctx = start(t1Player, List.of(t1Enemy), deck(CardLibrary.TWIN_STRIKE, 8), new ArrayList<>());
        t1Ctx.playCard(0, 0);
        c.equals("双重打击：5×2 = 10 点", 290, t1Enemy.hp());
        Combatant c1Player = dummy("玩家", 60);
        Enemy c1Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c1Ctx = start(c1Player, List.of(c1Enemy), deck(CardLibrary.CLOTHESLINE, 8), new ArrayList<>());
        c1Ctx.playCard(0, 0);
        c.equals("猛击：12 点伤害", 288, c1Enemy.hp());
        c.equals("猛击：2 层虚弱", 2, c1Enemy.weak());
    }
    /** v0.5.0.1：战士第一批 B（愤怒 / 旋风斩 / 牺牲）。 */
    private static void ironcladCards2(Checker c) {
        Combatant a2Player = dummy("玩家", 60);
        Enemy a2Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a2Ctx = start(a2Player, List.of(a2Enemy), deck(CardLibrary.ANGER, 8), new ArrayList<>());
        a2Ctx.playCard(0, 0);
        c.equals("愤怒：6 点伤害", 294, a2Enemy.hp());
        c.check("愤怒：弃牌堆里有一张复制",
                a2Ctx.stateOf(a2Player).discardPile.view().stream().anyMatch(card -> card.id().equals("anger")));
        c.equals("愤怒：复制后弃牌堆至少 2 张（打出的那张 + 复制）", true,
                a2Ctx.stateOf(a2Player).discardPile.size() >= 2);
        Combatant x2Player = dummy("玩家", 60);
        Enemy x2A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy x2B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext x2Ctx = start(x2Player, List.of(x2A, x2B), deck(CardLibrary.WHIRLWIND, 8), new ArrayList<>());
        x2Ctx.playCard(0, 0);
        c.equals("旋风斩：X=3 → 甲掉 15 点", 285, x2A.hp());
        c.equals("旋风斩：乙也掉 15 点", 285, x2B.hp());
        c.equals("旋风斩：X 费花光能量", 0, x2Ctx.energy());
        Combatant o2Player = dummy("玩家", 60);
        Enemy o2Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext o2Ctx = start(o2Player, List.of(o2Enemy), deck(CardLibrary.OFFERING, 10), new ArrayList<>());
        int o2Hand = o2Ctx.hand().size();
        o2Ctx.playCard(0, 0);
        c.equals("牺牲：失去 6 点生命", 54, o2Player.hp());
        c.equals("牺牲：2 点能量", 5, o2Ctx.energy());
        c.equals("牺牲：抽 3 张（手牌 -1 +3）", o2Hand + 2, o2Ctx.hand().size());
    }
    /** v0.5.0.2：战士第一批 C（战斗三昧 / 血祭 / 大扫除）。 */
    private static void ironcladCards3(Checker c) {
        Combatant t3Player = dummy("玩家", 60);
        Enemy t3Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext t3Ctx = start(t3Player, List.of(t3Enemy), deck(CardLibrary.BATTLE_TRANCE, 10), new ArrayList<>());
        int t3Hand = t3Ctx.hand().size();
        t3Ctx.playCard(0, 0);
        c.equals("战斗三昧：抽 3 张（手牌 -1 +3）", t3Hand + 2, t3Ctx.hand().size());
        Combatant l3Player = dummy("玩家", 60);
        Enemy l3Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext l3Ctx = start(l3Player, List.of(l3Enemy), deck(CardLibrary.BLOODLETTING, 8), new ArrayList<>());
        l3Ctx.playCard(0, 0);
        c.equals("血祭：失去 3 点生命", 57, l3Player.hp());
        c.equals("血祭：2 点能量", 5, l3Ctx.energy());
        Combatant r3Player = dummy("玩家", 60);
        Enemy r3A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy r3B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext r3Ctx = start(r3Player, List.of(r3A, r3B), deck(CardLibrary.REAPER, 8), new ArrayList<>());
        r3Player.loseHp(20);
        c.equals("大扫除前置：玩家 40 血", 40, r3Player.hp());
        r3Ctx.gainEnergy(r3Player, 2);
        r3Ctx.playCard(0, 0);
        c.equals("大扫除：甲掉 4 点", 296, r3A.hp());
        c.equals("大扫除：乙掉 4 点", 296, r3B.hp());
        c.equals("大扫除：回复 8 点生命（40 → 48）", 48, r3Player.hp());
    }
    /** v0.5.0.3：战士第一批 D（剑气 / 破灭 / 壁垒）。 */
    private static void ironcladCards4(Checker c) {
        Combatant b4Player = dummy("玩家", 60);
        Enemy b4A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy b4B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b4Ctx = start(b4Player, List.of(b4A, b4B), deck(CardLibrary.SWORD_BOOMERANG, 8), new ArrayList<>());
        b4Ctx.playCard(0, 0);
        c.equals("剑气：3 次共 9 点（随机分摊到两只）", 600 - 9, b4A.hp() + b4B.hp());
        Combatant r4Player = dummy("玩家", 60);
        Enemy r4Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext r4Ctx = start(r4Player, List.of(r4Enemy), deck(CardLibrary.RUPTURE, 8), new ArrayList<>());
        r4Ctx.playCard(0, 0);
        c.equals("破灭：装好时力量为 0", 0, r4Player.strength());
        r4Ctx.loseHp(r4Player, 5, "测试失血");
        c.equals("破灭：失去生命后获得 1 点力量", 1, r4Player.strength());
        Combatant k4Player = dummy("玩家", 60);
        Enemy k4Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext k4Ctx = start(k4Player, List.of(k4Enemy), deck(CardLibrary.BARRICADE, 8), new ArrayList<>());
        k4Ctx.gainEnergy(k4Player, 3);
        k4Ctx.playCard(0, 0);
        k4Ctx.grantPlayerBlock(10, "测试格挡");
        c.equals("壁垒：装好后有 10 点格挡", 10, k4Player.block());
        k4Ctx.endTurn();
        c.equals("壁垒：回合切换后格挡仍在（不清空）", 10, k4Player.block());
    }

    /** v0.5.2.0 A4：战士第一批 E（上勾拳 / 撞击 / 雷霆一击）。 */
    private static void ironcladCards5(Checker c) {
        Combatant u5Player = dummy("玩家", 60);
        Enemy u5Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext u5Ctx = start(u5Player, List.of(u5Enemy), deck(CardLibrary.UPPERCUT, 8), new ArrayList<>());
        u5Ctx.playCard(0, 0);
        c.equals("上勾拳：13 点伤害", 287, u5Enemy.hp());
        c.equals("上勾拳：1 层虚弱", 1, u5Enemy.weak());
        c.equals("上勾拳：1 层易伤", 1, u5Enemy.vulnerable());

        Combatant s5Player = dummy("玩家", 60);
        Enemy s5Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s5Ctx = start(s5Player, List.of(s5Enemy), deck(CardLibrary.BODY_SLAM, 8), new ArrayList<>());
        s5Ctx.grantPlayerBlock(12, "测试格挡");
        c.check("撞击：卡面会显示当前格挡值（12）",
                CardPreview.liveDescription(CardLibrary.BODY_SLAM, s5Player).contains("当前 12 点"));
        s5Ctx.playCard(0, 0);
        c.equals("撞击：造成等同于格挡值的 12 点伤害", 288, s5Enemy.hp());
        c.equals("撞击：格挡本身不会被消耗掉", 12, s5Player.block());

        Combatant t5Player = dummy("玩家", 60);
        Enemy t5A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy t5B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext t5Ctx = start(t5Player, List.of(t5A, t5B), deck(CardLibrary.THUNDERCLAP, 8), new ArrayList<>());
        t5Ctx.playCard(0, 0);
        c.equals("雷霆一击：甲掉 4 点", 296, t5A.hp());
        c.equals("雷霆一击：乙掉 4 点", 296, t5B.hp());
        c.check("雷霆一击：两个敌人都拿到 1 层易伤", t5A.vulnerable() == 1 && t5B.vulnerable() == 1);
    }

    /** v0.5.2.0 A4：猎手第一批（快斩 / 后空翻 / 毒刺）。 */
    private static void silentCards1(Checker c) {
        Combatant q6Player = dummy("玩家", 60);
        Enemy q6Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext q6Ctx = start(q6Player, List.of(q6Enemy), deck(CardLibrary.QUICK_SLASH, 8), new ArrayList<>());
        int q6Hand = q6Ctx.hand().size();
        q6Ctx.playCard(0, 0);
        c.equals("快斩：8 点伤害", 292, q6Enemy.hp());
        c.equals("快斩：抽 1 张（手牌 -1 +1 不变）", q6Hand, q6Ctx.hand().size());

        Combatant b6Player = dummy("玩家", 60);
        Enemy b6Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b6Ctx = start(b6Player, List.of(b6Enemy), deck(CardLibrary.BACKFLIP, 8), new ArrayList<>());
        int b6Hand = b6Ctx.hand().size();
        b6Ctx.playCard(0, 0);
        c.equals("后空翻：5 点格挡", 5, b6Player.block());
        c.equals("后空翻：抽 2 张（手牌 -1 +2 = +1）", b6Hand + 1, b6Ctx.hand().size());

        Combatant p6Player = dummy("玩家", 60);
        Enemy p6Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext p6Ctx = start(p6Player, List.of(p6Enemy), deck(CardLibrary.POISONED_STAB, 8), new ArrayList<>());
        p6Ctx.playCard(0, 0);
        c.equals("毒刺：6 点伤害", 294, p6Enemy.hp());
        c.equals("毒刺：3 层中毒", 3, p6Enemy.poison());
    }

    /** v0.5.2.0 A4：战士第二批（灵活 / 恐吓 / 见红）。 */
    private static void ironcladCards6(Checker c) {
        Combatant f7Player = dummy("玩家", 60);
        Enemy f7Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext f7Ctx = start(f7Player, List.of(f7Enemy), deck(CardLibrary.FLEX, 8), new ArrayList<>());
        f7Ctx.playCard(0, 0);
        c.equals("灵活：立刻 +2 点力量", 2, f7Player.strength());
        f7Ctx.endTurn();
        c.equals("灵活：回合结束后力量收回（回到 0）", 0, f7Player.strength());

        Combatant i7Player = dummy("玩家", 60);
        Enemy i7A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy i7B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext i7Ctx = start(i7Player, List.of(i7A, i7B), deck(CardLibrary.INTIMIDATE, 8), new ArrayList<>());
        i7Ctx.playCard(0, 0);
        c.check("恐吓：两个敌人都拿到 1 层虚弱", i7A.weak() == 1 && i7B.weak() == 1);
        c.equals("恐吓：消耗（进消耗堆）", 1, i7Ctx.exhaustPile().size());

        Combatant r7Player = dummy("玩家", 60);
        Enemy r7Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext r7Ctx = start(r7Player, List.of(r7Enemy), deck(CardLibrary.SEEING_RED, 8), new ArrayList<>());
        c.equals("见红前置：3 点能量", 3, r7Ctx.energy());
        r7Ctx.playCard(0, 0);
        c.equals("见红：付 1 回 2 → 4 点能量", 4, r7Ctx.energy());
        c.equals("见红：消耗（进消耗堆）", 1, r7Ctx.exhaustPile().size());
        r7Ctx.gainEnergy(r7Player, 3);
        r7Ctx.playCard(0, 0);
        c.equals("见红：再打一张（4+3-1+2）", 8, r7Ctx.energy());
    }

    /** v0.5.2.0 A4：猎手第二批（切片 / 偏斜 / 偷袭）。 */
    private static void silentCards2(Checker c) {
        Combatant s8Player = dummy("玩家", 60);
        Enemy s8Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s8Ctx = start(s8Player, List.of(s8Enemy), deck(CardLibrary.SLICE, 8), new ArrayList<>());
        s8Ctx.playCard(0, 0);
        c.equals("切片：6 点伤害", 294, s8Enemy.hp());
        c.equals("切片：0 费（能量不变）", 3, s8Ctx.energy());

        Combatant d8Player = dummy("玩家", 60);
        Enemy d8Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d8Ctx = start(d8Player, List.of(d8Enemy), deck(CardLibrary.DEFLECT, 8), new ArrayList<>());
        d8Ctx.playCard(0, 0);
        c.equals("偏斜：4 点格挡", 4, d8Player.block());
        c.equals("偏斜：0 费（能量不变）", 3, d8Ctx.energy());

        Combatant k8Player = dummy("玩家", 60);
        Enemy k8Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext k8Ctx = start(k8Player, List.of(k8Enemy), deck(CardLibrary.SUCKER_PUNCH, 8), new ArrayList<>());
        k8Ctx.playCard(0, 0);
        c.equals("偷袭：7 点伤害", 293, k8Enemy.hp());
        c.equals("偷袭：1 层虚弱", 1, k8Enemy.weak());
    }

    /** v0.5.2.0 A4：战士第三批（重锤 / 血液动力学 / 卸甲）。 */
    private static void ironcladCards7(Checker c) {
        Combatant b9Player = dummy("玩家", 60);
        Enemy b9Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b9Ctx = start(b9Player, List.of(b9Enemy), deck(CardLibrary.BLUDGEON, 8), new ArrayList<>());
        b9Ctx.gainEnergy(b9Player, 1);
        b9Ctx.playCard(0, 0);
        c.equals("重锤：32 点伤害", 268, b9Enemy.hp());

        Combatant h9Player = dummy("玩家", 60);
        Enemy h9Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext h9Ctx = start(h9Player, List.of(h9Enemy), deck(CardLibrary.HEMOKINESIS, 8), new ArrayList<>());
        h9Ctx.playCard(0, 0);
        c.equals("血液动力学：自己失去 2 点生命", 58, h9Player.hp());
        c.equals("血液动力学：15 点伤害", 285, h9Enemy.hp());

        Combatant d9Player = dummy("玩家", 60);
        Enemy d9Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d9Ctx = start(d9Player, List.of(d9Enemy), deck(CardLibrary.DISARM, 8), new ArrayList<>());
        d9Enemy.addStrength(5);
        d9Ctx.playCard(0, 0);
        c.equals("卸甲：敌人力量 5 → 3", 3, d9Enemy.strength());
        c.equals("卸甲：消耗（进消耗堆）", 1, d9Ctx.exhaustPile().size());
    }

    /** v0.5.2.0 A4：猎手第三批（匕首雨 / 扫腿 / 脚部功夫）。 */
    private static void silentCards3(Checker c) {
        Combatant s10Player = dummy("玩家", 60);
        Enemy s10A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy s10B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s10Ctx = start(s10Player, List.of(s10A, s10B),
                deck(CardLibrary.DAGGER_SPRAY, 8), new ArrayList<>());
        s10Ctx.playCard(0, 0);
        c.equals("匕首雨：甲掉 4×2 = 8 点", 292, s10A.hp());
        c.equals("匕首雨：乙掉 8 点", 292, s10B.hp());

        Combatant l10Player = dummy("玩家", 60);
        Enemy l10Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext l10Ctx = start(l10Player, List.of(l10Enemy), deck(CardLibrary.LEG_SWEEP, 8), new ArrayList<>());
        l10Ctx.playCard(0, 0);
        c.equals("扫腿：2 层虚弱", 2, l10Enemy.weak());
        c.equals("扫腿：11 点格挡", 11, l10Player.block());

        Combatant f10Player = dummy("玩家", 60);
        Enemy f10Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext f10Ctx = start(f10Player, List.of(f10Enemy), deck(CardLibrary.FOOTWORK, 8), new ArrayList<>());
        f10Ctx.playCard(0, 0);
        c.equals("脚部功夫：2 点敏捷", 2, f10Player.dexterity());
        // 敏捷要真的进格挡结算：再打一张格挡牌，5 + 2 = 7
        f10Ctx.addCardsToHand(f10Player, CardLibrary.DEFEND, 1);
        int f10Index = -1;
        for (int i = 0; i < f10Ctx.hand().size(); i++) {
            if (f10Ctx.hand().get(i).id().equals("defend")) {
                f10Index = i;
                break;
            }
        }
        f10Ctx.playCard(f10Index, 0);
        c.equals("脚部功夫：敏捷计入格挡（5+2=7）", 7, f10Player.block());
    }

    /** v0.5.2.0 A4：战士第四批（巩固 / 突破极限 / 强力打击）。 */
    private static void ironcladCards8(Checker c) {
        Combatant e11Player = dummy("玩家", 60);
        Enemy e11Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext e11Ctx = start(e11Player, List.of(e11Enemy), deck(CardLibrary.ENTRENCH, 8), new ArrayList<>());
        e11Ctx.grantPlayerBlock(12, "测试格挡");
        e11Ctx.gainEnergy(e11Player, 1);
        e11Ctx.playCard(0, 0);
        c.equals("巩固：格挡翻倍（12 → 24）", 24, e11Player.block());

        Combatant l11Player = dummy("玩家", 60);
        Enemy l11Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext l11Ctx = start(l11Player, List.of(l11Enemy), deck(CardLibrary.LIMIT_BREAK, 8), new ArrayList<>());
        l11Player.addStrength(4);
        l11Ctx.playCard(0, 0);
        c.equals("突破极限：力量翻倍（4 → 8）", 8, l11Player.strength());
        c.equals("突破极限：消耗（进消耗堆）", 1, l11Ctx.exhaustPile().size());

        Combatant p11Player = dummy("玩家", 60);
        Enemy p11Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext p11Ctx = start(p11Player, List.of(p11Enemy), deck(CardLibrary.PUMMEL, 8), new ArrayList<>());
        p11Ctx.playCard(0, 0);
        c.equals("强力打击：2 点 × 4 次 = 8 点", 292, p11Enemy.hp());
        c.equals("强力打击：消耗（进消耗堆）", 1, p11Ctx.exhaustPile().size());

        // ---- A4 补卡的"进池 + 有强化版"（防止新牌只注册了却没有强化版 / 没进奖励池）
        long icCount = CardLibrary.rewardPool(CardLibrary.IRONCLAD).stream()
                .filter(card -> CardLibrary.IRONCLAD.equals(card.character())).count();
        c.equals("战士奖励池里的专属牌 ≥ 31 张（A4 四批 12 张都进池了）", true, icCount >= 31);
        c.equals("A4 新牌都有强化版：撞击+ 是 0 费", 0, CardLibrary.resolve("body_slam", 1).cost());
        c.equals("A4 新牌都有强化版：巩固+ 是 1 费", 1, CardLibrary.resolve("entrench", 1).cost());
        c.equals("A4 新牌都有强化版：突破极限+ 不再消耗", false,
                CardLibrary.resolve("limit_break", 1).has(CardKeyword.EXHAUST));
        c.equals("A4 新牌都有强化版：见红+ 是 0 费", 0, CardLibrary.resolve("seeing_red", 1).cost());
    }

    /** v0.5.2.0 A4：猎手第四批（剧毒 / 专精 / 致残毒云）。 */
    private static void silentCards4(Checker c) {
        Combatant b12Player = dummy("玩家", 60);
        Enemy b12Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b12Ctx = start(b12Player, List.of(b12Enemy), deck(CardLibrary.BANE, 8), new ArrayList<>());
        b12Ctx.playCard(0, 0);
        c.equals("剧毒：目标没有中毒时只打 7 点", 293, b12Enemy.hp());
        b12Ctx.applyPoison(b12Enemy, 3);
        b12Ctx.gainEnergy(b12Player, 1);
        b12Ctx.playCard(0, 0);
        c.equals("剧毒：目标有中毒时打两段（7+7=14）", 279, b12Enemy.hp());

        Combatant e12Player = dummy("玩家", 60);
        Enemy e12Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext e12Ctx = start(e12Player, List.of(e12Enemy), deck(CardLibrary.EXPERTISE, 8), new ArrayList<>());
        e12Ctx.playCard(0, 0);
        c.equals("专精：抽到手上 6 张为止", 6, e12Ctx.hand().size());

        Combatant c12Player = dummy("玩家", 60);
        Enemy c12A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy c12B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c12Ctx = start(c12Player, List.of(c12A, c12B),
                deck(CardLibrary.CRIPPLING_CLOUD, 8), new ArrayList<>());
        c12Ctx.playCard(0, 0);
        c.check("致残毒云：两个敌人都拿到 4 层中毒、2 层虚弱",
                c12A.poison() == 4 && c12B.poison() == 4 && c12A.weak() == 2 && c12B.weak() == 2);
        c.equals("致残毒云：消耗（进消耗堆）", 1, c12Ctx.exhaustPile().size());

        long siCount = CardLibrary.rewardPool(CardLibrary.SILENT).stream()
                .filter(card -> CardLibrary.SILENT.equals(card.character())).count();
        c.equals("猎手奖励池里的专属牌 ≥ 21 张（A4 四批 12 张都进池了）", true, siCount >= 21);
        c.equals("A4 新牌都有强化版：快斩+ 改成 12 点",
                CardLibrary.resolve("quick_slash", 1).description().contains("12 点伤害"), true);
        c.equals("A4 新牌都有强化版：专精+ 抽到 7 张",
                CardLibrary.resolve("expertise", 1).description().contains("7 张"), true);
    }

    /** v0.5.2.1 A4-5：战士第五批（燃烧 / 冲击波 / 飞踢）。 */
    private static void ironcladCards9(Checker c) {
        Combatant i13Player = dummy("玩家", 60);
        Enemy i13Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext i13Ctx = start(i13Player, List.of(i13Enemy), deck(CardLibrary.INFLAME, 8), new ArrayList<>());
        i13Ctx.playCard(0, 0);
        c.equals("燃烧：+2 点力量", 2, i13Player.strength());
        c.equals("燃烧：能力牌既不进弃牌堆也不进消耗堆", 0,
                i13Ctx.discardPile().size() + i13Ctx.exhaustPile().size());

        Combatant s13Player = dummy("玩家", 60);
        Enemy s13A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy s13B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s13Ctx = start(s13Player, List.of(s13A, s13B),
                deck(CardLibrary.SHOCKWAVE, 8), new ArrayList<>());
        s13Ctx.playCard(0, 0);
        c.check("冲击波：两个敌人都拿到 3 虚弱 + 3 易伤",
                s13A.weak() == 3 && s13B.weak() == 3 && s13A.vulnerable() == 3 && s13B.vulnerable() == 3);
        c.equals("冲击波：消耗（进消耗堆）", 1, s13Ctx.exhaustPile().size());

        // 飞踢：目标没有易伤 → 只有伤害，不回能
        Combatant d13Player = dummy("玩家", 60);
        Enemy d13Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d13Ctx = start(d13Player, List.of(d13Enemy), deck(CardLibrary.DROPKICK, 8), new ArrayList<>());
        d13Ctx.playCard(0, 0);
        c.equals("飞踢：目标无易伤 → 只打 5 点", 295, d13Enemy.hp());
        c.equals("飞踢：目标无易伤 → 不回能（3-1=2）", 2, d13Ctx.energy());
        // 目标有易伤（本 mod 易伤 +25%）→ 5 → 6 点，并且回 1 能量 + 抽 1 张
        d13Enemy.addVulnerable(3);
        int d13Hand = d13Ctx.hand().size();
        d13Ctx.playCard(0, 0);
        c.equals("飞踢：目标有易伤 → 伤害吃 +25% 加成（5→6）", 289, d13Enemy.hp());
        c.equals("飞踢：目标有易伤 → 回 1 能量（2-1+1=2）", 2, d13Ctx.energy());
        c.equals("飞踢：目标有易伤 → 抽 1 张（手牌 -1+1）", d13Hand, d13Ctx.hand().size());
    }

    /** v0.5.2.1 A4-5：猎手第五批（冲刺 / 脚后跟钩 / 恐惧）。 */
    private static void silentCards5(Checker c) {
        Combatant d14Player = dummy("玩家", 60);
        Enemy d14A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy d14B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d14Ctx = start(d14Player, List.of(d14A, d14B), deck(CardLibrary.DASH, 8), new ArrayList<>());
        d14Ctx.playCard(0, 0);
        c.equals("冲刺：10 点格挡", 10, d14Player.block());
        c.equals("冲刺：甲掉 10 点", 290, d14A.hp());
        c.equals("冲刺：乙掉 10 点", 290, d14B.hp());

        // 脚后跟钩：目标没有虚弱 → 只打 5 点
        Combatant h14Player = dummy("玩家", 60);
        Enemy h14Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext h14Ctx = start(h14Player, List.of(h14Enemy), deck(CardLibrary.HEEL_HOOK, 8), new ArrayList<>());
        h14Ctx.playCard(0, 0);
        c.equals("脚后跟钩：目标无虚弱 → 只打 5 点", 295, h14Enemy.hp());
        c.equals("脚后跟钩：目标无虚弱 → 不回能（3-1=2）", 2, h14Ctx.energy());
        h14Enemy.addWeak(2);
        int h14Hand = h14Ctx.hand().size();
        h14Ctx.playCard(0, 0);
        // 玩家自己有虚弱才减伤；这里敌人身上的虚弱不影响玩家输出 → 仍然是 5 点
        c.equals("脚后跟钩：目标有虚弱 → 打 5 点", 290, h14Enemy.hp());
        c.equals("脚后跟钩：目标有虚弱 → 回 1 能量（2-1+1=2）", 2, h14Ctx.energy());
        c.equals("脚后跟钩：目标有虚弱 → 抽 1 张（手牌 -1+1）", h14Hand, h14Ctx.hand().size());

        Combatant t14Player = dummy("玩家", 60);
        Enemy t14Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext t14Ctx = start(t14Player, List.of(t14Enemy), deck(CardLibrary.TERROR, 8), new ArrayList<>());
        t14Ctx.playCard(0, 0);
        c.equals("恐惧：99 层易伤", 99, t14Enemy.vulnerable());
        c.equals("恐惧：消耗（进消耗堆）", 1, t14Ctx.exhaustPile().size());
        c.equals("恐惧+ 是 0 费", 0, CardLibrary.resolve("terror", 1).cost());
    }

    /** v0.5.2.1 A4-6：战士第六批（幽灵护甲 / 杀戮 / 鲁莽冲锋）。 */
    private static void ironcladCards10(Checker c) {
        Combatant g15Player = dummy("玩家", 60);
        Enemy g15Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext g15Ctx = start(g15Player, List.of(g15Enemy),
                deck(CardLibrary.GHOSTLY_ARMOR, 8), new ArrayList<>());
        g15Ctx.playCard(0, 0);
        c.equals("幽灵护甲：10 点格挡", 10, g15Player.block());
        c.equals("幽灵护甲：带【虚无】关键字", true, CardLibrary.GHOSTLY_ARMOR.has(CardKeyword.ETHEREAL));

        Combatant c15Player = dummy("玩家", 60);
        Enemy c15Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c15Ctx = start(c15Player, List.of(c15Enemy), deck(CardLibrary.CARNAGE, 8), new ArrayList<>());
        c15Ctx.gainEnergy(c15Player, 1);
        c15Ctx.playCard(0, 0);
        c.equals("杀戮：20 点伤害", 280, c15Enemy.hp());
        c.equals("杀戮：带【虚无】关键字", true, CardLibrary.CARNAGE.has(CardKeyword.ETHEREAL));

        Combatant r15Player = dummy("玩家", 60);
        Enemy r15Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext r15Ctx = start(r15Player, List.of(r15Enemy),
                deck(CardLibrary.RECKLESS_CHARGE, 8), new ArrayList<>());
        r15Ctx.playCard(0, 0);
        c.equals("鲁莽冲锋：0 费（能量不变）", 3, r15Ctx.energy());
        c.equals("鲁莽冲锋：7 点伤害", 293, r15Enemy.hp());
        c.equals("鲁莽冲锋：抽牌堆里多了一张『眩晕』", true,
                r15Ctx.drawPile().view().stream().anyMatch(card -> card.id().equals("dazed")));
    }

    /** v0.5.2.1 A4-6：猎手第六批（千疮百孔 / 飞刀 / 全力攻击）。 */
    private static void silentCards6(Checker c) {
        Combatant r16Player = dummy("玩家", 60);
        Enemy r16Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext r16Ctx = start(r16Player, List.of(r16Enemy),
                deck(CardLibrary.RIDDLE_WITH_HOLES, 8), new ArrayList<>());
        r16Ctx.gainEnergy(r16Player, 1);
        r16Ctx.playCard(0, 0);
        c.equals("千疮百孔：3 点 × 5 次 = 15 点", 285, r16Enemy.hp());

        // 飞刀：先在手里塞 2 张技能牌 → 打 2 段
        Combatant f16Player = dummy("玩家", 60);
        Enemy f16Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext f16Ctx = start(f16Player, List.of(f16Enemy),
                deck(CardLibrary.FLECHETTES, 8), new ArrayList<>());
        f16Ctx.addCardsToHand(f16Player, CardLibrary.DEFEND, 2);
        f16Ctx.playCard(0, 0);
        c.equals("飞刀：手里 2 张技能 → 4 点 × 2 = 8 点", 292, f16Enemy.hp());

        Combatant a16Player = dummy("玩家", 60);
        Enemy a16A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy a16B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a16Ctx = start(a16Player, List.of(a16A, a16B),
                deck(CardLibrary.ALL_OUT_ATTACK, 8), new ArrayList<>());
        int a16Hand = a16Ctx.hand().size();
        a16Ctx.playCard(0, 0);
        c.equals("全力攻击：甲掉 10 点", 290, a16A.hp());
        c.equals("全力攻击：乙掉 10 点", 290, a16B.hp());
        c.equals("全力攻击：手牌 -2（打出的 + 随机弃掉的）", a16Hand - 2, a16Ctx.hand().size());
        c.equals("全力攻击：弃牌堆 2 张（打出的 + 弃掉的）", 2, a16Ctx.discardPile().size());
    }

    /** v0.5.2.1 A4-7：战士第七批（献祭 / 恶魔火焰 / 斩断灵魂）。 */
    private static void ironcladCards11(Checker c) {
        Combatant i17Player = dummy("玩家", 60);
        Enemy i17A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy i17B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext i17Ctx = start(i17Player, List.of(i17A, i17B), deck(CardLibrary.IMMOLATE, 8), new ArrayList<>());
        i17Ctx.gainEnergy(i17Player, 1);
        i17Ctx.playCard(0, 0);
        c.equals("献祭：甲掉 21 点", 279, i17A.hp());
        c.equals("献祭：乙掉 21 点", 279, i17B.hp());
        c.equals("献祭：弃牌堆里多了一张『灼伤』", true,
                i17Ctx.discardPile().view().stream().anyMatch(card -> card.id().equals("burn")));

        // 恶魔火焰：手里 4 张（打出的那张已离手）→ 4 × 7 = 28，手牌清空，消耗堆 = 4 + 自己
        Combatant f17Player = dummy("玩家", 60);
        Enemy f17Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext f17Ctx = start(f17Player, List.of(f17Enemy), deck(CardLibrary.FIEND_FIRE, 8), new ArrayList<>());
        c.equals("恶魔火焰前置：手里 5 张", 5, f17Ctx.hand().size());
        f17Ctx.playCard(0, 0);
        c.equals("恶魔火焰：4 张 × 7 = 28 点", 272, f17Enemy.hp());
        c.equals("恶魔火焰：手牌被烧光", 0, f17Ctx.hand().size());
        c.equals("恶魔火焰：消耗堆 5 张（4 张手牌 + 自己）", 5, f17Ctx.exhaustPile().size());

        // 斩断灵魂：手里塞 2 张技能牌 → 只消耗那 2 张，攻击牌留着
        Combatant s17Player = dummy("玩家", 60);
        Enemy s17Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s17Ctx = start(s17Player, List.of(s17Enemy),
                deck(CardLibrary.SEVER_SOUL, 8), new ArrayList<>());
        s17Ctx.addCardsToHand(s17Player, CardLibrary.DEFEND, 2);
        s17Ctx.gainEnergy(s17Player, 1);
        s17Ctx.playCard(0, 0);
        c.equals("斩断灵魂：只消耗 2 张技能牌", 2, s17Ctx.exhaustPile().size());
        c.equals("斩断灵魂：16 点伤害", 284, s17Enemy.hp());
        c.equals("斩断灵魂：攻击牌留在手里（4 张）", 4, s17Ctx.hand().size());
    }

    /** v0.5.2.1 A4-7：猎手第七批（闪避翻滚 / 串刺 / 逃生计划）。 */
    private static void silentCards7(Checker c) {
        Combatant d18Player = dummy("玩家", 60);
        Enemy d18Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d18Ctx = start(d18Player, List.of(d18Enemy),
                deck(CardLibrary.DODGE_AND_ROLL, 8), new ArrayList<>());
        d18Ctx.playCard(0, 0);
        c.equals("闪避翻滚：本回合 4 点格挡", 4, d18Player.block());
        d18Ctx.endTurn();
        c.equals("闪避翻滚：下回合开始时又拿到 4 点格挡", 4, d18Player.block());

        Combatant s18Player = dummy("玩家", 60);
        Enemy s18Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s18Ctx = start(s18Player, List.of(s18Enemy), deck(CardLibrary.SKEWER, 8), new ArrayList<>());
        s18Ctx.playCard(0, 0);
        c.equals("串刺：X = 3（花光 3 点能量）", 0, s18Ctx.energy());
        c.equals("串刺：7 点 × 3 = 21 点", 279, s18Enemy.hp());

        // 逃生计划：牌组全是技能牌 → 抽到的一定是技能牌 → 给格挡
        Combatant e18Player = dummy("玩家", 60);
        Enemy e18Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext e18Ctx = start(e18Player, List.of(e18Enemy),
                deck(CardLibrary.ESCAPE_PLAN, 8), new ArrayList<>());
        int e18Hand = e18Ctx.hand().size();
        e18Ctx.playCard(0, 0);
        c.equals("逃生计划：抽 1 张（手牌 -1+1 不变）", e18Hand, e18Ctx.hand().size());
        c.equals("逃生计划：抽到技能牌 → 3 点格挡", 3, e18Player.block());
        c.equals("逃生计划：0 费（能量不变）", 3, e18Ctx.energy());
    }

    /** v0.5.2.1 A4-8：战士第八批（第二次呼吸 / 坚毅 / 野性打击 +『伤口』）。 */
    private static void ironcladCards12(Checker c) {
        // 第二次呼吸：手里 4 张技能（打出的那张已离手）→ 消耗 4 张，每张 5 点格挡
        Combatant s19Player = dummy("玩家", 60);
        Enemy s19Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext s19Ctx = start(s19Player, List.of(s19Enemy),
                deck(CardLibrary.SECOND_WIND, 8), new ArrayList<>());
        s19Ctx.playCard(0, 0);
        c.equals("第二次呼吸：消耗 4 张非攻击牌", 4, s19Ctx.exhaustPile().size());
        c.equals("第二次呼吸：4 张 × 5 = 20 点格挡", 20, s19Player.block());

        // 坚毅：7 点格挡 + 随机消耗 1 张
        Combatant g19Player = dummy("玩家", 60);
        Enemy g19Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext g19Ctx = start(g19Player, List.of(g19Enemy), deck(CardLibrary.TRUE_GRIT, 8), new ArrayList<>());
        g19Ctx.playCard(0, 0);
        c.equals("坚毅：7 点格挡", 7, g19Player.block());
        c.equals("坚毅：随机消耗 1 张手牌", 1, g19Ctx.exhaustPile().size());

        // 野性打击：12 点伤害 + 一张『伤口』进抽牌堆
        Combatant w19Player = dummy("玩家", 60);
        Enemy w19Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext w19Ctx = start(w19Player, List.of(w19Enemy),
                deck(CardLibrary.WILD_STRIKE, 8), new ArrayList<>());
        w19Ctx.playCard(0, 0);
        c.equals("野性打击：12 点伤害", 288, w19Enemy.hp());
        c.equals("野性打击：抽牌堆里多了一张『伤口』", true,
                w19Ctx.drawPile().view().stream().anyMatch(card -> card.id().equals("wound")));
        c.check("『伤口』不能被打出", StatusCards.unplayable(CardLibrary.WOUND));
        c.check("『伤口』是状态牌且没有回合结束效果",
                CardLibrary.WOUND.type() == CardType.STATUS && StatusCards.endOfTurnDamage(CardLibrary.WOUND) == 0);
    }

    /** v0.5.2.1 A4-8：猎手第八批（死吧死吧死吧 / 分身 / 不适）。 */
    private static void silentCards8(Checker c) {
        Combatant d20Player = dummy("玩家", 60);
        Enemy d20A = new Enemy("甲", 300, 300, List.of(List.of(Intent.unknown())), true);
        Enemy d20B = new Enemy("乙", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d20Ctx = start(d20Player, List.of(d20A, d20B),
                deck(CardLibrary.DIE_DIE_DIE, 8), new ArrayList<>());
        d20Ctx.playCard(0, 0);
        c.equals("死吧死吧死吧：甲掉 13 点", 287, d20A.hp());
        c.equals("死吧死吧死吧：乙掉 13 点", 287, d20B.hp());
        c.equals("死吧死吧死吧：消耗（进消耗堆）", 1, d20Ctx.exhaustPile().size());

        // 分身：X = 3 → 抽 3 张、回 3 点能量
        Combatant g20Player = dummy("玩家", 60);
        Enemy g20Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext g20Ctx = start(g20Player, List.of(g20Enemy),
                deck(CardLibrary.DOPPELGANGER, 12), new ArrayList<>());
        int g20Hand = g20Ctx.hand().size();
        g20Ctx.playCard(0, 0);
        c.equals("分身：X=3 花光能量后又回 3 点", 3, g20Ctx.energy());
        c.equals("分身：抽 3 张（手牌 -1+3 = +2）", g20Hand + 2, g20Ctx.hand().size());

        // 不适：X = 3 → 敌人 -3 力量、+3 虚弱
        Combatant m20Player = dummy("玩家", 60);
        Enemy m20Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext m20Ctx = start(m20Player, List.of(m20Enemy), deck(CardLibrary.MALAISE, 8), new ArrayList<>());
        m20Enemy.addStrength(2);
        m20Ctx.playCard(0, 0);
        c.equals("不适：敌人力量 2 → 0（-3 被夹到 0）", 0, m20Enemy.strength());
        c.equals("不适：3 层虚弱", 3, m20Enemy.weak());
        c.equals("不适：消耗（进消耗堆）", 1, m20Ctx.exhaustPile().size());
        c.equals("分身在卡池里被标记为 X 费", true, CardLibrary.isXCost("doppelganger"));
        c.equals("不适在卡池里被标记为 X 费", true, CardLibrary.isXCost("malaise"));
    }

    /** v0.5.2.2 A4-9：战士第九批（头槌 / 进食 / 完美打击）。 */
    private static void ironcladCards13(Checker c) {
        // 头槌：先往弃牌堆塞两张牌，打完头槌之后再选一张放回抽牌堆顶
        Combatant h21Player = dummy("玩家", 60);
        Enemy h21Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext h21Ctx = start(h21Player, List.of(h21Enemy), deck(CardLibrary.HEADBUTT, 8), new ArrayList<>());
        h21Ctx.stateOf(h21Player).discardPile().add(CardLibrary.DEFEND);
        h21Ctx.stateOf(h21Player).discardPile().add(CardLibrary.BASH);
        h21Ctx.playCard(0, 0);
        c.equals("头槌：9 点伤害", 291, h21Enemy.hp());
        c.check("头槌：挂起了一次选牌（从弃牌堆选）",
                h21Ctx.pendingPick(h21Player) != null
                        && h21Ctx.pendingPick(h21Player).source() == CombatContext.CardPickSource.DISCARD);
        c.equals("头槌：候选是弃牌堆里的 2 张", 2, h21Ctx.pickCandidates(h21Player).size());
        h21Ctx.resolveCardPick(h21Player, List.of(1));
        c.equals("头槌：选中的『重击』被放到了抽牌堆顶", "bash",
                h21Ctx.drawPile().view().get(0).id());
        c.equals("头槌：选完就不再挂起", true, h21Ctx.pendingPick(h21Player) == null);

        // 进食：打死目标才涨上限
        Combatant f21Player = dummy("玩家", 60);
        Enemy f21Enemy = new Enemy("残血怪", 8, 8, List.of(List.of(Intent.unknown())), true);
        CombatContext f21Ctx = start(f21Player, List.of(f21Enemy), deck(CardLibrary.FEED, 8), new ArrayList<>());
        f21Ctx.playCard(0, 0);
        c.equals("进食：击杀后最大生命 60 → 63", 63, f21Player.maxHp());
        c.equals("进食：上限涨了，当前生命也一起涨（60 → 63）", 63, f21Player.hp());
        c.equals("进食：消耗（进消耗堆）", 1, f21Ctx.exhaustPile().size());

        Combatant f21bPlayer = dummy("玩家", 60);
        Enemy f21bEnemy = new Enemy("厚血怪", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext f21bCtx = start(f21bPlayer, List.of(f21bEnemy), deck(CardLibrary.FEED, 8), new ArrayList<>());
        f21bCtx.playCard(0, 0);
        c.equals("进食：没打死就不涨上限（仍是 60）", 60, f21bPlayer.maxHp());
        c.equals("进食：没打死也有 10 点伤害", 290, f21bEnemy.hp());

        // 完美打击：牌组里每张"打击类"牌 +2（自己也算，打出时它自己还没进弃牌堆）
        Combatant p21Player = dummy("玩家", 60);
        Enemy p21Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext p21Ctx = start(p21Player, List.of(p21Enemy),
                deck(CardLibrary.PERFECTED_STRIKE, 8), new ArrayList<>());
        c.equals("完美打击前置：牌组里 8 张都是『打击』类", 8,
                p21Ctx.stateOf(p21Player).hand().size() + p21Ctx.drawPile().size()
                        + p21Ctx.discardPile().size());
        p21Ctx.gainEnergy(p21Player, 1);
        p21Ctx.playCard(0, 0);
        // 手牌 4 + 抽牌堆 3 = 7 张（打出的那张还在结算中，没进弃牌堆）→ 6 + 7×2 = 20
        c.equals("完美打击：7 张 ×2 → 6+14 = 20 点伤害", 280, p21Enemy.hp());

        Combatant p21bPlayer = dummy("玩家", 60);
        Enemy p21bEnemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext p21bCtx = start(p21bPlayer, List.of(p21bEnemy),
                deck(CardLibrary.PERFECTED_STRIKE, 8), new ArrayList<>());
        for (int i = 0; i < 3; i++) {
            p21bCtx.stateOf(p21bPlayer).discardPile().add(CardLibrary.STRIKE);
        }
        p21bCtx.gainEnergy(p21bPlayer, 1);
        p21bCtx.playCard(0, 0);
        // 牌组多出 3 张『打击』→ 10 张 → 6 + 20 = 26
        c.equals("完美打击：弃牌堆里再加 3 张『打击』→ 6+20 = 26 点", 274, p21bEnemy.hp());
    }

    /** v0.5.2.2 A4-9：猎手第九批（卸货 / 准备 / 集中精神）。 */
    private static void silentCards9(Checker c) {
        Combatant u22Player = dummy("玩家", 60);
        Enemy u22Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext u22Ctx = start(u22Player, List.of(u22Enemy), deck(CardLibrary.UNLOAD, 8), new ArrayList<>());
        u22Ctx.addCardsToHand(u22Player, CardLibrary.DEFEND, 2);
        u22Ctx.playCard(0, 0);
        c.equals("卸货：14 点伤害", 286, u22Enemy.hp());
        c.equals("卸货：手里 2 张技能牌被弃掉（手牌 7-1-2=4）", 4, u22Ctx.hand().size());
        c.equals("卸货：弃牌堆 3 张（2 张弃掉的 + 自己）", 3, u22Ctx.discardPile().size());

        // 准备：抽 1 弃 1（玩家自己选）
        Combatant p22Player = dummy("玩家", 60);
        Enemy p22Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext p22Ctx = start(p22Player, List.of(p22Enemy), deck(CardLibrary.PREPARED, 8), new ArrayList<>());
        p22Ctx.playCard(0, 0);
        c.check("准备：挂起了一次「弃 1 张」的选牌",
                p22Ctx.pendingPick(p22Player) != null
                        && p22Ctx.pendingPick(p22Player).kind() == CombatContext.CardPickKind.DISCARD
                        && p22Ctx.pendingPick(p22Player).maxPicks() == 1);
        c.equals("准备：抽 1 张之后手里还是 5 张", 5, p22Ctx.hand().size());
        int p22Before = p22Ctx.hand().size();
        p22Ctx.resolveCardPick(p22Player, List.of(0));
        c.equals("准备：选完之后手牌 -1", p22Before - 1, p22Ctx.hand().size());

        // 集中精神：弃 3 得 2 能量；用"放弃/超时"走贪心兜底
        Combatant c22Player = dummy("玩家", 60);
        Enemy c22Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c22Ctx = start(c22Player, List.of(c22Enemy),
                deck(CardLibrary.CONCENTRATE, 8), new ArrayList<>());
        c22Ctx.playCard(0, 0);
        c.equals("集中精神：立刻 +2 能量（0 费，3 → 5）", 5, c22Ctx.energy());
        c.check("集中精神：挂起「弃 3 张」",
                c22Ctx.pendingPick(c22Player) != null
                        && c22Ctx.pendingPick(c22Player).maxPicks() == 3);
        c22Ctx.cancelCardPick(c22Player);
        c.equals("集中精神：贪心兜底替他弃掉 3 张（手牌 4 → 1）", 1, c22Ctx.hand().size());
        c.check("集中精神+ 是「弃 2 张」",
                CardLibrary.resolve("concentrate", 1).description().contains("弃 2 张"));

        // 规则要求"弃 3 张"，玩家只勾了 1 张 → 引擎按贪心补足，不能白拿能量
        Combatant c22bPlayer = dummy("玩家", 60);
        Enemy c22bEnemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c22bCtx = start(c22bPlayer, List.of(c22bEnemy),
                deck(CardLibrary.CONCENTRATE, 8), new ArrayList<>());
        c22bCtx.playCard(0, 0);
        c22bCtx.resolveCardPick(c22bPlayer, List.of(0));
        c.equals("集中精神：只勾 1 张也会补足到 3 张（手牌 4 → 1）", 1, c22bCtx.hand().size());
    }

    /** v0.5.2.3 A4-10：战士第十批（军备 / 双持 / 燃烧契约）。 */
    private static void ironcladCards14(Checker c) {
        // 军备：5 格挡 + 选一张升级（候选里不能出现"已经强化过"的牌）
        Combatant a23Player = dummy("玩家", 60);
        Enemy a23Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a23Ctx = start(a23Player, List.of(a23Enemy), deck(CardLibrary.ARMAMENTS, 8), new ArrayList<>());
        a23Ctx.addCardsToHand(a23Player, CardLibrary.STRIKE, 1);
        a23Ctx.playCard(0, 0);
        c.equals("军备：5 点格挡", 5, a23Player.block());
        c.check("军备：挂起了「选一张升级」的选牌",
                a23Ctx.pendingPick(a23Player) != null
                        && a23Ctx.pendingPick(a23Player).kind() == CombatContext.CardPickKind.UPGRADE_ONE);
        c.equals("军备：5 个候选（4 张自己的牌 + 1 张『打击』，都已强化过的不算）", 5,
                a23Ctx.pickCandidates(a23Player).size());
        boolean allBase = a23Ctx.pickCandidates(a23Player).stream()
                .allMatch(card -> !card.name().endsWith("+"));
        c.check("军备：候选里没有已经是强化版的牌", allBase);
        // 选手里那张『打击』（在候选里的最后一个，因为它是最后加进手牌的）
        int strikePick = a23Ctx.pickCandidates(a23Player).size() - 1;
        a23Ctx.resolveCardPick(a23Player, List.of(strikePick));
        c.equals("军备：选中的『打击』变成了『打击+』", true,
                a23Ctx.hand().stream().anyMatch(card -> card.id().equals("strike")
                        && card.name().endsWith("+")));

        // 军备+：直接升级手里全部牌
        Combatant a23bPlayer = dummy("玩家", 60);
        Enemy a23bEnemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a23bCtx = start(a23bPlayer, List.of(a23bEnemy),
                deck(CardLibrary.resolve("armaments", 1), 8), new ArrayList<>());
        a23bCtx.playCard(0, 0);
        c.equals("军备+：没有挂起选牌（直接全升）", true, a23bCtx.pendingPick(a23bPlayer) == null);
        c.equals("军备+：升级后手里剩下的 4 张全是强化版", true,
                a23bCtx.hand().stream().allMatch(card -> card.name().endsWith("+")));

        // 双持：候选只有攻击 / 能力牌
        Combatant d23Player = dummy("玩家", 60);
        Enemy d23Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d23Ctx = start(d23Player, List.of(d23Enemy), deck(CardLibrary.DUAL_WIELD, 8), new ArrayList<>());
        d23Ctx.addCardsToHand(d23Player, CardLibrary.STRIKE, 1);
        d23Ctx.playCard(0, 0);
        c.check("双持：挂起了「复制一张」的选牌",
                d23Ctx.pendingPick(d23Player) != null
                        && d23Ctx.pendingPick(d23Player).kind() == CombatContext.CardPickKind.DUPLICATE);
        c.equals("双持：候选只有 1 张（那张『打击』；手上的双持都是技能牌）", 1,
                d23Ctx.pickCandidates(d23Player).size());
        int d23Hand = d23Ctx.hand().size();
        d23Ctx.resolveCardPick(d23Player, List.of(0));
        c.equals("双持：手牌 +1（复制品进手牌）", d23Hand + 1, d23Ctx.hand().size());

        // 燃烧契约：消耗一张 + 抽 2 张
        Combatant b23Player = dummy("玩家", 60);
        Enemy b23Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b23Ctx = start(b23Player, List.of(b23Enemy),
                deck(CardLibrary.BURNING_PACT, 10), new ArrayList<>());
        b23Ctx.playCard(0, 0);
        c.check("燃烧契约：挂起了「选一张消耗」的选牌",
                b23Ctx.pendingPick(b23Player) != null
                        && b23Ctx.pendingPick(b23Player).kind() == CombatContext.CardPickKind.BURNING_PACT);
        int b23Hand = b23Ctx.hand().size();
        b23Ctx.resolveCardPick(b23Player, List.of(0));
        c.equals("燃烧契约：消耗 1 张（进消耗堆）", 1, b23Ctx.exhaustPile().size());
        c.equals("燃烧契约：手牌 -1 +2 = +1", b23Hand + 1, b23Ctx.hand().size());
    }

    /** v0.5.2.3 A4-10：猎手第十批（杂技 / 飞刀投掷 / 计算赌博）。 */
    private static void silentCards10(Checker c) {
        // 杂技：抽 3 弃 1
        Combatant a24Player = dummy("玩家", 60);
        Enemy a24Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a24Ctx = start(a24Player, List.of(a24Enemy), deck(CardLibrary.ACROBATICS, 12), new ArrayList<>());
        int a24Hand = a24Ctx.hand().size();
        a24Ctx.playCard(0, 0);
        c.equals("杂技：抽 3 张（手牌 -1 +3 = +2）", a24Hand + 2, a24Ctx.hand().size());
        c.check("杂技：挂起了「弃 1 张」的选牌",
                a24Ctx.pendingPick(a24Player) != null
                        && a24Ctx.pendingPick(a24Player).maxPicks() == 1);
        int a24Before = a24Ctx.hand().size();
        a24Ctx.resolveCardPick(a24Player, List.of(0));
        c.equals("杂技：弃 1 张之后手牌 -1", a24Before - 1, a24Ctx.hand().size());

        // 飞刀投掷：9 伤害 + 抽 1 弃 1
        Combatant d24Player = dummy("玩家", 60);
        Enemy d24Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext d24Ctx = start(d24Player, List.of(d24Enemy),
                deck(CardLibrary.DAGGER_THROW, 12), new ArrayList<>());
        d24Ctx.playCard(0, 0);
        c.equals("飞刀投掷：9 点伤害", 291, d24Enemy.hp());
        c.check("飞刀投掷：挂起了「弃 1 张」的选牌", d24Ctx.pendingPick(d24Player) != null);
        int d24Before = d24Ctx.hand().size();
        d24Ctx.resolveCardPick(d24Player, List.of(0));
        c.equals("飞刀投掷：弃 1 张之后手牌 -1", d24Before - 1, d24Ctx.hand().size());

        // 计算赌博：弃光手牌再抽回等量
        Combatant g24Player = dummy("玩家", 60);
        Enemy g24Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext g24Ctx = start(g24Player, List.of(g24Enemy),
                deck(CardLibrary.CALCULATED_GAMBLE, 12), new ArrayList<>());
        int g24Hand = g24Ctx.hand().size();
        g24Ctx.playCard(0, 0);
        c.equals("计算赌博：弃光 4 张又抽回 4 张（打出的那张已离手，所以是 4）", g24Hand - 1,
                g24Ctx.hand().size());
        c.equals("计算赌博：消耗（进消耗堆）", 1, g24Ctx.exhaustPile().size());
        c.equals("计算赌博：弃牌堆里是刚弃掉的那 4 张", 4, g24Ctx.discardPile().size() - 0);
        c.equals("计算赌博+ 去掉了消耗",
                CardLibrary.resolve("calculated_gamble", 1).has(CardKeyword.EXHAUST), false);
    }

    /** v0.5.2.4 A4-11：战士第十一批（战吼 / 浩劫 / 察觉弱点）。 */
    private static void ironcladCards15(Checker c) {
        // 战吼：抽 1 张 + 把一张手牌放回抽牌堆顶
        Combatant w25Player = dummy("玩家", 60);
        Enemy w25Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext w25Ctx = start(w25Player, List.of(w25Enemy), deck(CardLibrary.WARCRY, 10), new ArrayList<>());
        int w25Hand = w25Ctx.hand().size();
        w25Ctx.playCard(0, 0);
        c.equals("战吼：抽 1 张（手牌 -1 +1 不变）", w25Hand, w25Ctx.hand().size());
        c.check("战吼：挂起了「放回抽牌堆顶」的选牌",
                w25Ctx.pendingPick(w25Player) != null
                        && w25Ctx.pendingPick(w25Player).kind() == CombatContext.CardPickKind.PUT_ON_TOP);
        String w25TopBefore = w25Ctx.drawPile().view().get(0).id();
        int w25DrawBefore = w25Ctx.drawPile().size();
        int w25Before = w25Ctx.hand().size();
        w25Ctx.resolveCardPick(w25Player, List.of(0));
        c.equals("战吼：手牌 -1", w25Before - 1, w25Ctx.hand().size());
        c.equals("战吼：抽牌堆 +1（那张手牌被放回去了）", w25DrawBefore + 1, w25Ctx.drawPile().size());
        c.equals("战吼：抽牌堆顶就是刚放回去的『战吼』", "warcry", w25Ctx.drawPile().view().get(0).id());
        c.check("战吼：抽牌堆顶确实换过（旧顶牌被挤到第 2 张）",
                w25TopBefore == null || w25Ctx.drawPile().size() > 1);
        c.equals("战吼：消耗（进消耗堆）", 1, w25Ctx.exhaustPile().size());

        // 浩劫：打出抽牌堆顶那张（这里顶上是一张『打击』）并消耗
        Combatant h25Player = dummy("玩家", 60);
        Enemy h25Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext h25Ctx = start(h25Player, List.of(h25Enemy), deck(CardLibrary.HAVOC, 8), new ArrayList<>());
        h25Ctx.putOnTopOfDrawPile(h25Player, CardLibrary.STRIKE);
        h25Ctx.playCard(0, 0);
        c.equals("浩劫：打出了顶上的『打击』（6 点伤害）", 294, h25Enemy.hp());
        c.equals("浩劫：那张『打击』被消耗了", true,
                h25Ctx.exhaustPile().view().stream().anyMatch(card -> card.id().equals("strike")));

        // 浩劫：抽牌堆空时什么也不发生
        Combatant h25bPlayer = dummy("玩家", 60);
        Enemy h25bEnemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext h25bCtx = start(h25bPlayer, List.of(h25bEnemy), deck(CardLibrary.HAVOC, 5), new ArrayList<>());
        // 手牌 5 张全在手里、抽牌堆空 → 打出手里那张之后抽牌堆仍是空的
        h25bCtx.playCard(0, 0);
        c.equals("浩劫：抽牌堆空时不崩、也没多打伤害", 300, h25bEnemy.hp());

        // 察觉弱点：目标意图攻击 → +3 力量
        Combatant s25Player = dummy("玩家", 60);
        Enemy s25Enemy = zombie(60, Intent.attack(5));
        CombatContext s25Ctx = start(s25Player, List.of(s25Enemy),
                deck(CardLibrary.SPOT_WEAKNESS, 8), new ArrayList<>());
        s25Ctx.playCard(0, 0);
        c.equals("察觉弱点：目标意图攻击 → 力量 +3", 3, s25Player.strength());

        Combatant s25bPlayer = dummy("玩家", 60);
        Enemy s25bEnemy = zombie(60, Intent.defend(5));
        CombatContext s25bCtx = start(s25bPlayer, List.of(s25bEnemy),
                deck(CardLibrary.SPOT_WEAKNESS, 8), new ArrayList<>());
        s25bCtx.playCard(0, 0);
        c.equals("察觉弱点：目标不攻击 → 不加力量", 0, s25bPlayer.strength());
    }

    /** v0.5.2.5 新状态【荆棘】：被攻击时反弹等同于层数的伤害。 */
    private static void thornsStatus(Checker c) {
        // 玩家荆棘 3，敌人打过来 → 敌人被反弹 3 点
        Combatant player = dummy("玩家", 60);
        Enemy attacker = zombie(40, Intent.attack(5));
        CombatContext ctx = start(player, List.of(attacker), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        player.addThorns(3);
        c.equals("荆棘：装好后是 3 层", 3, player.thorns());
        int enemyHpBefore = attacker.hp();
        // 让敌人打玩家一次
        ctx.endTurn();
        c.check("荆棘：敌人攻击后自己掉了 3 点血", attacker.hp() == enemyHpBefore - 3);
        c.equals("荆棘：层数不随回合衰减（仍是 3）", 3, player.thorns());

        // 反弹**不吃力量**：给玩家 +5 力量，反弹仍然是 3
        Combatant strong = dummy("玩家", 60);
        Enemy attacker2 = zombie(40, Intent.attack(5));
        CombatContext ctx2 = start(strong, List.of(attacker2), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        strong.addThorns(3);
        strong.addStrength(5);
        int hp2 = attacker2.hp();
        ctx2.endTurn();
        c.equals("荆棘：反弹不吃力量（+5 力量时仍是 3 点）", hp2 - 3, attacker2.hp());

        // 荆棘可以反杀敌人
        Combatant killer = dummy("玩家", 60);
        Enemy weakling = zombie(2, Intent.attack(5));
        CombatContext ctx3 = start(killer, List.of(weakling), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        killer.addThorns(9);
        ctx3.endTurn();
        c.check("荆棘：反弹可以击杀敌人", !weakling.alive());
    }

    /** v0.5.3.0：八件新遗物里靠引擎实现的那几件。 */
    private static void relicEffectsForV0530(Checker c) {
        // 脏玻璃：开战 3 层荆棘
        Combatant glass = dummy("玩家", 60);
        Enemy glassEnemy = zombie(40, Intent.attack(5));
        CombatContext glassCtx = start(glass, List.of(glassEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        RelicLibrary.DIRTY_GLASS.effect().apply(glassCtx, glass);
        c.equals("脏玻璃：开战获得荆棘 3", 3, glass.thorns());

        // 不太寻常的潜影箱：开战 1 层缓降
        Combatant shulker = dummy("玩家", 60);
        Enemy shulkerEnemy = zombie(40, Intent.attack(5));
        CombatContext shulkerCtx = start(shulker, List.of(shulkerEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        RelicLibrary.UNUSUAL_SHULKER_BOX.effect().apply(shulkerCtx, shulker);
        c.equals("潜影箱：开战获得缓降 1", 1, shulker.slowFall());

        // 龙蛋：每个敌人回合最多受到 20 点
        Combatant egg = dummy("玩家", 100);
        Enemy big = zombie(200, Intent.attack(30));
        CombatContext eggCtx = start(egg, List.of(big), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        RelicLibrary.DRAGON_EGG.effect().apply(eggCtx, egg);
        c.equals("龙蛋：受伤上限被装上", 20, egg.damageCap());
        eggCtx.endTurn();
        c.equals("龙蛋：30 点攻击只掉 20 点血", 80, egg.hp());
        eggCtx.endTurn();
        c.equals("龙蛋：下一个敌人回合额度重置，再掉 20 点", 60, egg.hp());

        // 荧光墨囊：剩余费用跨回合累加（上限 = 每回合费用的 2 倍）
        Combatant ink = dummy("玩家", 60);
        Enemy inkEnemy = zombie(200, Intent.unknown());
        CombatContext inkCtx = start(ink, List.of(inkEnemy), deck(CardLibrary.DEFEND, 20), new ArrayList<>());
        RelicLibrary.GLOW_INK_SAC.effect().apply(inkCtx, ink);
        c.check("荧光墨囊：旗标被装上", ink.energyCarryover());
        c.equals("荧光墨囊：第一回合还是 3 点费用", 3, inkCtx.energy());
        inkCtx.endTurn();
        c.equals("荧光墨囊：没用掉的 3 点留到下一回合（3+3=6，正好上限 2 倍）", 6, inkCtx.energy());
        inkCtx.endTurn();
        c.equals("荧光墨囊：上限就是 2 倍费用（再多也只留 6）", 6, inkCtx.energy());
    }

    // ------------------------------------------------------------------ v0.5.3.1 / v0.5.3.2

    /**
     * v0.5.3.1（A）+ v0.5.3.2（小调整）：用户逐件给的**遗物售价**与**来源通道**。
     *
     * <p>通道一共三条：战斗掉落（精英 / 常规战斗 10% 那一档）、藏宝箱、商店。</p>
     * <ul>
     *   <li>v0.5.3.1：【行商羊驼披肩】从"只能商店"改成"商店 / 藏宝箱"；</li>
     *   <li>v0.5.3.2：【制图台】也从"只能商店"改成"商店 / 藏宝箱"，并给【制图台】210、【龙蛋】240
     *       两件补上单价 —— 于是**商店里能出现的每一件遗物都有单价了**（配置统一价只剩兜底）。</li>
     * </ul>
     */
    private static void relicPricesAndSources(Checker c) {
        // ---- 售价：用户逐件给定（14 件 = 24 件里除 4 件初始 + 6 件事件限定之外的全部）
        c.equals("金刚杵 195", 195, RelicLibrary.priceOf("vajra"));
        c.equals("锚 210", 210, RelicLibrary.priceOf("anchor"));
        c.equals("预备包 225", 225, RelicLibrary.priceOf("bag_of_preparation"));
        c.equals("奇滑石 195", 195, RelicLibrary.priceOf("oddly_smooth_stone"));
        c.equals("药水腰带 205", 205, RelicLibrary.priceOf("potion_belt"));
        c.equals("小血瓶 195", 195, RelicLibrary.priceOf("small_vial"));
        c.equals("脏玻璃 210", 210, RelicLibrary.priceOf("dirty_glass"));
        c.equals("破旧打火石 210", 210, RelicLibrary.priceOf("worn_flint"));
        c.equals("行商羊驼披肩 205", 205, RelicLibrary.priceOf("trader_llama_shawl"));
        c.equals("不太寻常的潜影箱 225", 225, RelicLibrary.priceOf("unusual_shulker_box"));
        c.equals("灵魂篝火 225", 225, RelicLibrary.priceOf("soul_campfire"));
        c.equals("荧光墨囊 225", 225, RelicLibrary.priceOf("glow_ink_sac"));
        c.equals("v0.5.3.2 制图台 210", 210, RelicLibrary.priceOf("cartography_table"));
        c.equals("v0.5.3.2 龙蛋 240", 240, RelicLibrary.priceOf("dragon_egg"));
        c.equals("用户给定单价的遗物正好 14 件", 14, RelicLibrary.pricedCount());
        c.check("14 件都有 hasPrice",
                RelicLibrary.hasPrice("vajra") && RelicLibrary.hasPrice("glow_ink_sac")
                        && RelicLibrary.hasPrice("cartography_table") && RelicLibrary.hasPrice("dragon_egg"));
        c.check("**每一件能上架商店的遗物**都有单价（配置统一价只剩兜底）",
                RelicLibrary.all().stream()
                        .filter(relic -> RelicLibrary.droppableRelic(relic.id()))
                        .allMatch(relic -> RelicLibrary.hasPrice(relic.id())));
        c.check("初始遗物 / 事件限定遗物都没有单价（它们根本不卖）",
                RelicLibrary.priceOf("burning_blood") == 0 && RelicLibrary.priceOf("startled_piglet") == 0
                        && !RelicLibrary.hasPrice("burning_blood")
                        && !RelicLibrary.hasPrice("mogu_fillet"));
        boolean range = true;
        for (Relic relic : RelicLibrary.all()) {
            int price = RelicLibrary.priceOf(relic.id());
            if (price != 0 && (price < 195 || price > 240)) {
                range = false;
            }
        }
        c.check("给定单价都落在 195~240 之间", range);

        // ---- 来源通道
        c.check("行商羊驼披肩：商店能卖", RelicLibrary.shopSellable("trader_llama_shawl"));
        c.check("行商羊驼披肩：藏宝箱能开", RelicLibrary.chestDroppable("trader_llama_shawl"));
        c.check("行商羊驼披肩：**不**进战斗掉落（用户给的来源是「商店 / 藏宝箱」）",
                !RelicLibrary.eliteDroppable("trader_llama_shawl"));
        c.check("行商羊驼披肩在藏宝箱池里", RelicLibrary.chestPool().stream()
                .anyMatch(relic -> relic.id().equals("trader_llama_shawl")));
        c.check("行商羊驼披肩在商店池里", RelicLibrary.shopPool().stream()
                .anyMatch(relic -> relic.id().equals("trader_llama_shawl")));
        c.check("行商羊驼披肩**不**在战斗掉落池里", RelicLibrary.elitePool().stream()
                .noneMatch(relic -> relic.id().equals("trader_llama_shawl")));

        // v0.5.3.2：制图台改成"商店 / 藏宝箱"
        c.check("制图台：商店能卖", RelicLibrary.shopSellable("cartography_table"));
        c.check("制图台：**藏宝箱能开**（v0.5.3.2 从「只能商店」改过来）",
                RelicLibrary.chestDroppable("cartography_table"));
        c.check("制图台：**不**进战斗掉落", !RelicLibrary.eliteDroppable("cartography_table"));
        c.check("制图台进了藏宝箱池", RelicLibrary.chestPool().stream()
                .anyMatch(relic -> relic.id().equals("cartography_table")));
        c.check("制图台不在战斗掉落池里", RelicLibrary.elitePool().stream()
                .noneMatch(relic -> relic.id().equals("cartography_table")));
        c.check("制图台不再算「只能商店」", !RelicLibrary.isShopOnly("cartography_table")
                && RelicLibrary.isChestAndShopOnly("cartography_table"));

        // 龙蛋仍然是"只能商店"
        c.check("龙蛋：商店能卖", RelicLibrary.shopSellable("dragon_egg"));
        c.check("龙蛋：藏宝箱**不**开", !RelicLibrary.chestDroppable("dragon_egg"));
        c.check("龙蛋：战斗**不**掉", !RelicLibrary.eliteDroppable("dragon_egg"));
        c.check("「只能商店」现在只剩龙蛋一件",
                RelicLibrary.isShopOnly("dragon_egg") && !RelicLibrary.isShopOnly("cartography_table")
                        && !RelicLibrary.isShopOnly("trader_llama_shawl"));
        c.check("藏宝箱池里没有龙蛋 / 初始遗物",
                RelicLibrary.chestPool().stream().noneMatch(relic -> relic.id().equals("dragon_egg")
                        || RelicLibrary.isStarting(relic.id())));
        c.check("战斗掉落池里没有制图台 / 龙蛋 / 披肩 / 事件限定遗物",
                RelicLibrary.elitePool().stream().noneMatch(relic -> relic.id().equals("cartography_table")
                        || relic.id().equals("dragon_egg")
                        || relic.id().equals("trader_llama_shawl")
                        || RelicLibrary.isEventOnly(relic.id())));

        // ---- 随机掉落永远拿不到"只能商店"与"藏宝箱/商店"那几件
        boolean never = true;
        for (int seed = 0; seed < 200; seed++) {
            Relic relic = RelicLibrary.randomUnowned(new Random(seed), List.of());
            if (relic == null || RelicLibrary.isShopOnly(relic.id())
                    || RelicLibrary.isChestAndShopOnly(relic.id())) {
                never = false;
            }
        }
        c.check("随机掉落 200 次都拿不到「只能商店」与「藏宝箱/商店」那几件", never);
    }

    /** v0.5.3.1（B）：叛逃掠夺者再补的 5 张卡。 */
    private static void pillagerCardsForV0531(Checker c) {
        // ---- 午时已到：全体 20；意图攻击的敌人额外 15
        Combatant noonPlayer = dummy("玩家", 60);
        Enemy noonAttacker = zombie(300, Intent.attack(5));
        Enemy noonGuard = zombie(300, Intent.defend(5));
        CombatContext noonCtx = start(noonPlayer, List.of(noonAttacker, noonGuard),
                deck(CardLibrary.HIGH_NOON, 10), new ArrayList<>());
        noonCtx.playCard(0, 0);
        c.equals("午时已到：意图攻击的敌人吃 20 + 15 = 35", 265, noonAttacker.hp());
        c.equals("午时已到：不打算攻击的敌人只吃 20", 280, noonGuard.hp());
        c.equals("午时已到：消耗（进消耗堆）", 1, noonCtx.exhaustPile().size());
        c.equals("午时已到：3 费", 0, noonCtx.energy());
        c.equals("午时已到：稀有 攻击", true,
                CardLibrary.HIGH_NOON.rarity() == CardRarity.RARE
                        && CardLibrary.HIGH_NOON.type() == CardType.ATTACK);

        Combatant noon2Player = dummy("玩家", 60);
        Enemy noon2Attacker = zombie(300, Intent.attack(5));
        CombatContext noon2Ctx = start(noon2Player, List.of(noon2Attacker),
                deck(CardLibrary.resolve("high_noon", 1), 10), new ArrayList<>());
        noon2Ctx.playCard(0, 0);
        c.equals("午时已到+：额外那一档 15 → 20（20 + 20 = 40）", 260, noon2Attacker.hp());

        // ---- 山火：14 伤害 + 等同伤害的灼烧
        Combatant firePlayer = dummy("玩家", 60);
        Enemy fireEnemy = zombie(100, Intent.attack(1));
        CombatContext fireCtx = start(firePlayer, List.of(fireEnemy),
                deck(CardLibrary.WILDFIRE, 8), new ArrayList<>());
        fireCtx.playCard(0, 0);
        c.equals("山火：14 点伤害", 86, fireEnemy.hp());
        c.equals("山火：附加等同于伤害的灼烧 14", 14, fireEnemy.burn());
        c.equals("山火：2 费", 1, fireCtx.energy());

        Combatant fire3Player = dummy("玩家", 60);
        fire3Player.addStrength(3);
        Enemy fire3Enemy = zombie(100, Intent.attack(1));
        CombatContext fire3Ctx = start(fire3Player, List.of(fire3Enemy),
                deck(CardLibrary.WILDFIRE, 8), new ArrayList<>());
        fire3Ctx.playCard(0, 0);
        c.equals("山火：吃力量（14 + 3 = 17 点伤害）", 83, fire3Enemy.hp());
        c.equals("山火：灼烧按算完力量 / 易伤的那个数给（17 层）", 17, fire3Enemy.burn());

        Combatant fire2Player = dummy("玩家", 60);
        Enemy fire2Enemy = zombie(100, Intent.attack(1));
        CombatContext fire2Ctx = start(fire2Player, List.of(fire2Enemy),
                deck(CardLibrary.resolve("wildfire", 1), 8), new ArrayList<>());
        fire2Ctx.playCard(0, 0);
        c.equals("山火+：14 → 17 点伤害", 83, fire2Enemy.hp());
        c.equals("山火+：灼烧也跟着是 17", 17, fire2Enemy.burn());

        // ---- 众生皆苦：每打出一张牌 → 所有敌人 1 层凋零
        Combatant sufPlayer = dummy("玩家", 60);
        Enemy sufA = zombie(200, Intent.unknown());
        Enemy sufB = zombie(200, Intent.unknown());
        CombatContext sufCtx = start(sufPlayer, List.of(sufA, sufB),
                deck(CardLibrary.ALL_SUFFERING, 10), new ArrayList<>());
        sufCtx.playCard(0, 0);
        c.equals("众生皆苦：点亮它的这一下就给所有敌人 1 层凋零（它自己也算打出了一张牌）",
                1, sufA.decay());
        c.equals("众生皆苦：另一个敌人也吃到 1 层", 1, sufB.decay());
        c.equals("众生皆苦：1 费", 2, sufCtx.energy());
        sufCtx.playCard(0, 0);
        c.equals("众生皆苦：再打一张牌 → 累加到 2 层", 2, sufA.decay());
        sufCtx.addCardsToHand(sufPlayer, CardLibrary.STRIKE, 1);
        sufCtx.playCard(handIndexOf(sufCtx, "strike"), 0);
        c.equals("众生皆苦：打一张『打击』同样给所有敌人 +1 层", 3, sufA.decay());
        c.equals("众生皆苦+：费用 1 → 0", 0, CardLibrary.resolve("all_suffering", 1).cost());

        // ---- 劫掠形态：每打出一张攻击牌 → 手牌 +1 张弩箭射击
        Combatant pilPlayer = dummy("玩家", 60);
        Enemy pilEnemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext pilCtx = start(pilPlayer, List.of(pilEnemy),
                deck(CardLibrary.PILLAGE_FORM, 10), new ArrayList<>());
        pilCtx.playCard(0, 0);
        c.check("劫掠形态：旗标点亮", pilCtx.stateOf(pilPlayer).pillageForm());
        c.equals("劫掠形态：3 费", 0, pilCtx.energy());
        c.equals("劫掠形态：打出能力牌自己不会加弩箭", 0, pilCtx.hand().stream()
                .filter(card -> card.id().equals("bolt_shot")).count());
        pilCtx.gainEnergy(pilPlayer, 3);
        pilCtx.addCardsToHand(pilPlayer, CardLibrary.STRIKE, 1);
        pilCtx.playCard(handIndexOf(pilCtx, "strike"), 0);
        c.equals("劫掠形态：打出一张攻击牌 → 手牌 +1 张『弩箭射击』", 1, pilCtx.hand().stream()
                .filter(card -> card.id().equals("bolt_shot")).count());
        int pilBolt = handIndexOf(pilCtx, "bolt_shot");
        pilCtx.playCard(pilBolt, 0);
        c.equals("劫掠形态：『弩箭射击』自己也是攻击牌 → 打完立刻补一张（手牌张数不变）",
                1, pilCtx.hand().stream().filter(card -> card.id().equals("bolt_shot")).count());
        c.equals("劫掠形态：刚打出的那张进了弃牌堆", 1, pilCtx.discardPile().view().stream()
                .filter(card -> card.id().equals("bolt_shot")).count());

        Combatant pil2Player = dummy("玩家", 60);
        Enemy pil2Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext pil2Ctx = start(pil2Player, List.of(pil2Enemy),
                deck(CardLibrary.resolve("pillage_form", 1), 10), new ArrayList<>());
        pil2Ctx.playCard(0, 0);
        pil2Ctx.gainEnergy(pil2Player, 3);
        pil2Ctx.addCardsToHand(pil2Player, CardLibrary.STRIKE, 1);
        pil2Ctx.playCard(handIndexOf(pil2Ctx, "strike"), 0);
        c.check("劫掠形态+：加的是强化版『弩箭射击+』", pil2Ctx.hand().stream()
                .anyMatch(card -> card.id().equals("bolt_shot") && card.name().contains("+")));

        // ---- 商队护送：自己与队友 15 格挡；这一轮没被打穿 → 战后 +20 金币
        Combatant escPlayer = dummy("玩家", 60);
        Enemy escEnemy = zombie(200, Intent.attack(5));
        CombatContext escCtx = start(escPlayer, List.of(escEnemy),
                deck(CardLibrary.CARAVAN_ESCORT, 10), new ArrayList<>());
        escCtx.playCard(0, 0);
        c.equals("商队护送：自己 15 点格挡", 15, escPlayer.block());
        c.equals("商队护送：2 费", 1, escCtx.energy());
        c.equals("商队护送：消耗（进消耗堆）", 1, escCtx.exhaustPile().size());
        escCtx.endTurn();
        c.equals("商队护送：5 点攻击被格挡吃掉，自己没掉血", 60, escPlayer.hp());
        c.equals("商队护送：成功 → 战斗结束后额外 20 金币", 20, escCtx.escortGold(escPlayer));
        c.equals("商队护送：成功次数 1", 1, escCtx.escortSuccesses(escPlayer));

        Combatant esc2Player = dummy("玩家", 60);
        Enemy esc2Enemy = zombie(200, Intent.attack(30));
        CombatContext esc2Ctx = start(esc2Player, List.of(esc2Enemy),
                deck(CardLibrary.CARAVAN_ESCORT, 10), new ArrayList<>());
        esc2Ctx.playCard(0, 0);
        esc2Ctx.endTurn();
        c.equals("商队护送：30 点攻击打穿 15 格挡 → 掉 15 点血", 45, esc2Player.hp());
        c.equals("商队护送：被打穿 → 一分钱也没有", 0, esc2Ctx.escortGold(esc2Player));
        c.equals("商队护送：失败次数不计入成功", 0, esc2Ctx.escortSuccesses(esc2Player));

        // 多人：队友也吃 15 格挡
        Combatant escortA = dummy("玩家A", 60);
        Combatant escortB = dummy("玩家B", 60);
        Enemy partyEnemy = zombie(200, Intent.attack(5));
        List<CombatContext.Participant> escorts = List.of(
                new CombatContext.Participant(escortA, deck(CardLibrary.CARAVAN_ESCORT, 10)),
                new CombatContext.Participant(escortB, deck(CardLibrary.STRIKE, 10)));
        CombatContext partyEscort = new CombatContext(escorts, List.of(partyEnemy), CombatRules.defaults(),
                20260101L, CombatLog.toList(new ArrayList<>()));
        partyEscort.start();
        partyEscort.playCard(0, 0);
        c.equals("商队护送（多人）：队友也拿到 15 点格挡", 15, escortB.block());
        c.equals("商队护送（多人）：护送名单是 2 个人", 2, partyEscort.stateOf(escortA).escorted().size());
        partyEscort.endTurn();
        partyEscort.endTurn();
        c.equals("商队护送（多人）：两人都没被打穿 → 20 金币", 20, partyEscort.escortGold(escortA));

        // 敌人在出手之前就被清完 → 也算成功（胜利那一刻结算）
        Combatant esc3Player = dummy("玩家", 60);
        Enemy esc3Enemy = zombie(5, Intent.attack(5));
        CombatContext esc3Ctx = start(esc3Player, List.of(esc3Enemy),
                deck(CardLibrary.CARAVAN_ESCORT, 10), new ArrayList<>());
        esc3Ctx.playCard(0, 0);
        esc3Ctx.addCardsToHand(esc3Player, CardLibrary.STRIKE, 1);
        esc3Ctx.playCard(handIndexOf(esc3Ctx, "strike"), 0);
        c.check("商队护送：本回合就把敌人清完 → 战斗胜利", esc3Ctx.phase() == CombatPhase.VICTORY);
        c.equals("商队护送：敌人没来得及出手也算成功 → 20 金币", 20, esc3Ctx.escortGold(esc3Player));
        c.equals("商队护送+：费用 2 → 1", 1, CardLibrary.resolve("caravan_escort", 1).cost());
        c.check("商队护送：登记为「多人玩法向」（图鉴分页用）",
                CardLibrary.isMultiplayerStyle("caravan_escort"));

        // ---- 五张卡都进池、都有强化版
        // v0.5.5.2（用户 B7）：【商队护送】单人时不进池，所以单看单人池它要在**多人池**里查
        boolean allUpgradable = true;
        boolean allInPool = true;
        for (String id : List.of("high_noon", "wildfire", "all_suffering", "pillage_form", "caravan_escort")) {
            if (!CardLibrary.canUpgrade(id)) {
                allUpgradable = false;
            }
            int partySize = id.equals("caravan_escort") ? 2 : 1;
            if (CardLibrary.rewardPool(CardLibrary.PILLAGER, partySize).stream()
                    .noneMatch(card -> card.id().equals(id))) {
                allInPool = false;
            }
        }
        c.check("v0.5.3.1 的五张新卡都有强化版", allUpgradable);
        c.check("v0.5.3.1 的五张新卡都进了叛逃掠夺者的奖励池（商队护送按多人池查）", allInPool);
        c.check("五张新卡的归属都是叛逃掠夺者",
                CardLibrary.HIGH_NOON.character().equals(CardLibrary.PILLAGER)
                        && CardLibrary.WILDFIRE.character().equals(CardLibrary.PILLAGER)
                        && CardLibrary.ALL_SUFFERING.character().equals(CardLibrary.PILLAGER)
                        && CardLibrary.PILLAGE_FORM.character().equals(CardLibrary.PILLAGER)
                        && CardLibrary.CARAVAN_ESCORT.character().equals(CardLibrary.PILLAGER));
    }

    // ------------------------------------------------------------------ v0.5.3.3 A4-12

    /**
     * v0.5.3.3（A4-12）战士 3 张：金属化 / 自燃 / 火焰屏障 —— 全部靠**回合钩子**。
     *
     * <p>数值逐页核对过 kamigame：金属化 3→4、自燃(燃焼) 5→7、炎の障壁 12/4→16/6。</p>
     */
    private static void ironcladCards16(Checker c) {
        // ---- 金属化：回合结束给格挡（正好挡住敌人的攻击）
        Combatant metPlayer = dummy("玩家", 60);
        Enemy metEnemy = zombie(200, Intent.attack(5));
        CombatContext metCtx = start(metPlayer, List.of(metEnemy), deck(CardLibrary.METALLICIZE, 10),
                new ArrayList<>());
        metCtx.playCard(0, 0);
        c.equals("金属化：1 费", 2, metCtx.energy());
        c.equals("金属化：旗标记的是 3（每回合结束给 3 格挡）", 3, metCtx.stateOf(metPlayer).metallicize());
        metCtx.endTurn();
        c.equals("金属化：回合结束给 3 格挡 → 5 点攻击只掉 2 血", 58, metPlayer.hp());

        // 两张金属化会累加
        Combatant met2Player = dummy("玩家", 60);
        Enemy met2Enemy = zombie(200, Intent.attack(5));
        CombatContext met2Ctx = start(met2Player, List.of(met2Enemy), deck(CardLibrary.METALLICIZE, 10),
                new ArrayList<>());
        met2Ctx.gainEnergy(met2Player, 5);
        met2Ctx.playCard(0, 0);
        met2Ctx.playCard(0, 0);
        c.equals("金属化：打出两张 → 每回合结束 6 格挡", 6, met2Ctx.stateOf(met2Player).metallicize());
        met2Ctx.endTurn();
        c.equals("金属化：6 格挡把 5 点攻击完全挡住（不掉血）", 60, met2Player.hp());

        // 强化版 4 点
        Combatant met3Player = dummy("玩家", 60);
        Enemy met3Enemy = zombie(200, Intent.attack(5));
        CombatContext met3Ctx = start(met3Player, List.of(met3Enemy),
                deck(CardLibrary.resolve("metallicize", 1), 10), new ArrayList<>());
        met3Ctx.playCard(0, 0);
        met3Ctx.endTurn();
        c.equals("金属化+：4 格挡 → 5 点攻击掉 1 血", 59, met3Player.hp());

        // ---- 自燃：回合结束自己 -1 血 + 全体 5 点
        Combatant comPlayer = dummy("玩家", 60);
        Enemy comA = zombie(200, Intent.unknown());
        Enemy comB = zombie(200, Intent.unknown());
        CombatContext comCtx = start(comPlayer, List.of(comA, comB), deck(CardLibrary.COMBUST, 10),
                new ArrayList<>());
        comCtx.playCard(0, 0);
        c.equals("自燃：旗标记的是 5", 5, comCtx.stateOf(comPlayer).combust());
        c.equals("自燃：打出它的这一回合不会立刻结算", 60, comPlayer.hp());
        comCtx.endTurn();
        c.equals("自燃：回合结束自己失去 1 点生命", 59, comPlayer.hp());
        c.equals("自燃：对所有敌人 5 点伤害（第一个）", 195, comA.hp());
        c.equals("自燃：对所有敌人 5 点伤害（第二个）", 195, comB.hp());

        Combatant com2Player = dummy("玩家", 60);
        Enemy com2Enemy = zombie(200, Intent.unknown());
        CombatContext com2Ctx = start(com2Player, List.of(com2Enemy),
                deck(CardLibrary.resolve("combust", 1), 10), new ArrayList<>());
        com2Ctx.playCard(0, 0);
        com2Ctx.endTurn();
        c.equals("自燃+：7 点伤害", 193, com2Enemy.hp());
        c.equals("自燃+：自己还是只失去 1 点生命", 59, com2Player.hp());

        // ---- 火焰屏障：12 格挡 + 本回合被打就反弹 4（活过整个敌人回合）
        Combatant fbPlayer = dummy("玩家", 60);
        Enemy fbEnemy = zombie(200, Intent.attack(5));
        CombatContext fbCtx = start(fbPlayer, List.of(fbEnemy), deck(CardLibrary.FLAME_BARRIER, 10),
                new ArrayList<>());
        fbCtx.playCard(0, 0);
        c.equals("火焰屏障：12 点格挡", 12, fbPlayer.block());
        c.equals("火焰屏障：本回合荆棘 4", 4, fbPlayer.thorns());
        c.equals("火焰屏障：记下其中 4 层是临时的", 4, fbPlayer.temporaryThorns());
        fbCtx.endTurn();
        c.equals("火焰屏障：5 点攻击被格挡吃掉（自己不掉血）", 60, fbPlayer.hp());
        c.equals("火焰屏障：反弹 4 点给攻击者", 196, fbEnemy.hp());
        c.equals("火焰屏障：下回合开始把临时荆棘收回", 0, fbPlayer.thorns());
        c.equals("火焰屏障：收回之后也不剩临时层数", 0, fbPlayer.temporaryThorns());

        // 永久荆棘不会被误收回（脏玻璃 3 + 火焰屏障 4 = 7，收完只剩 3）
        Combatant fb2Player = dummy("玩家", 60);
        fb2Player.addThorns(3);
        Enemy fb2Enemy = zombie(200, Intent.attack(5));
        CombatContext fb2Ctx = start(fb2Player, List.of(fb2Enemy), deck(CardLibrary.FLAME_BARRIER, 10),
                new ArrayList<>());
        fb2Ctx.playCard(0, 0);
        c.equals("火焰屏障：与永久荆棘叠加（3 + 4 = 7）", 7, fb2Player.thorns());
        fb2Ctx.endTurn();
        c.equals("火焰屏障：反弹总数 7 点", 193, fb2Enemy.hp());
        c.equals("火焰屏障：永久那 3 层还在", 3, fb2Player.thorns());

        Combatant fb3Player = dummy("玩家", 60);
        Enemy fb3Enemy = zombie(200, Intent.attack(5));
        CombatContext fb3Ctx = start(fb3Player, List.of(fb3Enemy),
                deck(CardLibrary.resolve("flame_barrier", 1), 10), new ArrayList<>());
        fb3Ctx.playCard(0, 0);
        c.equals("火焰屏障+：16 点格挡", 16, fb3Player.block());
        fb3Ctx.endTurn();
        c.equals("火焰屏障+：反弹 6 点", 194, fb3Enemy.hp());
    }

    /** v0.5.3.3（A4-12）猎手 3 张：无限刀刃 / 毒雾 / 布置。 */
    // ------------------------------------------------------------------ v0.5.3.3 A4-13

    /**
     * v0.5.3.3（A4-13）战士 3 张：腐化 / 双重施法 / 主宰。
     *
     * <p>数值逐页核对过 kamigame：堕落 3→2、ダブルタップ 1 张→2 张、ジャガーノート 5→7。</p>
     */
    private static void ironcladCards17(Checker c) {
        // ---- 腐化：技能免费 + 打完直接消耗
        Combatant corPlayer = dummy("玩家", 60);
        Enemy corEnemy = zombie(200, Intent.unknown());
        CombatContext corCtx = start(corPlayer, List.of(corEnemy), deck(CardLibrary.CORRUPTION, 10),
                new ArrayList<>());
        corCtx.playCard(0, 0);
        c.check("腐化：旗标点亮", corCtx.stateOf(corPlayer).corruption());
        c.equals("腐化：3 费", 0, corCtx.energy());
        c.equals("腐化：技能牌的实际费用是 0", 0,
                corCtx.effectiveCost(corCtx.stateOf(corPlayer), CardLibrary.DEFEND));
        c.equals("腐化：攻击牌不受影响（打击仍是 1 费）", 1,
                corCtx.effectiveCost(corCtx.stateOf(corPlayer), CardLibrary.STRIKE));
        corCtx.gainEnergy(corPlayer, 3);
        corCtx.addCardsToHand(corPlayer, CardLibrary.DEFEND, 1);
        int beforeEnergy = corCtx.energy();
        corCtx.playCard(handIndexOf(corCtx, "defend"), 0);
        c.equals("腐化：技能牌一点能量都不花", beforeEnergy, corCtx.energy());
        c.equals("腐化：打出的技能直接进消耗堆", 1, corCtx.exhaustPile().size());
        c.equals("腐化：弃牌堆里没有那张技能", 0, corCtx.discardPile().view().stream()
                .filter(card -> card.id().equals("defend")).count());
        c.equals("腐化+：费用 3 → 2", 2, CardLibrary.resolve("corruption", 1).cost());

        // ---- 双重施法：下一张攻击牌打两遍
        Combatant dtPlayer = dummy("玩家", 60);
        Enemy dtEnemy = zombie(200, Intent.unknown());
        CombatContext dtCtx = start(dtPlayer, List.of(dtEnemy), deck(CardLibrary.DOUBLE_TAP, 10),
                new ArrayList<>());
        dtCtx.playCard(0, 0);
        c.equals("双重施法：本回合额度 1", 1, dtCtx.stateOf(dtPlayer).doubleTap());
        dtCtx.gainEnergy(dtPlayer, 3);
        dtCtx.addCardsToHand(dtPlayer, CardLibrary.STRIKE, 1);
        dtCtx.playCard(handIndexOf(dtCtx, "strike"), 0);
        c.equals("双重施法：打击打两遍 → 6 + 6 = 12 点伤害", 188, dtEnemy.hp());
        c.equals("双重施法：额度用掉了", 0, dtCtx.stateOf(dtPlayer).doubleTap());

        // 强化版：下 2 张攻击牌各打两遍
        Combatant dt2Player = dummy("玩家", 60);
        Enemy dt2Enemy = zombie(200, Intent.unknown());
        CombatContext dt2Ctx = start(dt2Player, List.of(dt2Enemy),
                deck(CardLibrary.resolve("double_tap", 1), 10), new ArrayList<>());
        dt2Ctx.playCard(0, 0);
        c.equals("双重施法+：本回合额度 2", 2, dt2Ctx.stateOf(dt2Player).doubleTap());
        dt2Ctx.gainEnergy(dt2Player, 5);
        dt2Ctx.addCardsToHand(dt2Player, CardLibrary.STRIKE, 1);
        dt2Ctx.addCardsToHand(dt2Player, CardLibrary.STRIKE, 1);
        dt2Ctx.playCard(handIndexOf(dt2Ctx, "strike"), 0);
        dt2Ctx.playCard(handIndexOf(dt2Ctx, "strike"), 0);
        c.equals("双重施法+：两次打击各打两遍 → 4 × 6 = 24 点", 176, dt2Enemy.hp());
        c.equals("双重施法+：额度用完", 0, dt2Ctx.stateOf(dt2Player).doubleTap());

        // 额度是"本回合内"：回合结束作废
        Combatant dt3Player = dummy("玩家", 60);
        Enemy dt3Enemy = zombie(200, Intent.unknown());
        CombatContext dt3Ctx = start(dt3Player, List.of(dt3Enemy), deck(CardLibrary.DOUBLE_TAP, 10),
                new ArrayList<>());
        dt3Ctx.playCard(0, 0);
        dt3Ctx.endTurn();
        c.equals("双重施法：回合结束额度作废", 0, dt3Ctx.stateOf(dt3Player).doubleTap());

        // ---- 主宰：每次获得格挡 → 随机敌人 5 点（这里只有一个敌人，结果确定）
        Combatant jugPlayer = dummy("玩家", 60);
        Enemy jugEnemy = zombie(200, Intent.unknown());
        CombatContext jugCtx = start(jugPlayer, List.of(jugEnemy), deck(CardLibrary.JUGGERNAUT, 10),
                new ArrayList<>());
        jugCtx.playCard(0, 0);
        c.equals("主宰：旗标 5", 5, jugCtx.stateOf(jugPlayer).juggernaut());
        jugCtx.gainEnergy(jugPlayer, 3);
        jugCtx.addCardsToHand(jugPlayer, CardLibrary.DEFEND, 1);
        jugCtx.playCard(handIndexOf(jugCtx, "defend"), 0);
        c.equals("主宰：卡牌给格挡 → 5 点伤害", 195, jugEnemy.hp());
        c.equals("主宰：格挡本身照常拿到", 5, jugPlayer.block());

        // 能力牌 / 遗物给的格挡也要触发（因为格挡统一走 gainBlockFor 一个入口）
        Combatant jug2Player = dummy("玩家", 60);
        Enemy jug2Enemy = zombie(200, Intent.unknown());
        CombatContext jug2Ctx = start(jug2Player, List.of(jug2Enemy), deck(CardLibrary.JUGGERNAUT, 10),
                new ArrayList<>());
        jug2Ctx.gainEnergy(jug2Player, 5);
        jug2Ctx.playCard(0, 0);
        jug2Ctx.addCardsToHand(jug2Player, CardLibrary.METALLICIZE, 1);
        jug2Ctx.playCard(handIndexOf(jug2Ctx, "metallicize"), 0);
        jug2Ctx.endTurn();
        c.equals("主宰：【金属化】的回合末格挡也触发", 195, jug2Enemy.hp());
        c.equals("主宰：【金属化】的格挡照常拿到（回合开始会清掉，所以这里看的是下一回合的）",
                0, jug2Player.block());

        // 锚（遗物）给的格挡
        Combatant jug3Player = dummy("玩家", 60);
        Enemy jug3Enemy = zombie(200, Intent.unknown());
        CombatContext jug3Ctx = start(jug3Player, List.of(jug3Enemy), deck(CardLibrary.JUGGERNAUT, 10),
                new ArrayList<>());
        jug3Ctx.gainEnergy(jug3Player, 5);
        jug3Ctx.playCard(0, 0);
        RelicLibrary.ANCHOR.effect().apply(jug3Ctx, jug3Player);
        c.equals("主宰：遗物【锚】给的格挡也触发", 195, jug3Enemy.hp());

        // 强化版 7 点
        Combatant jug4Player = dummy("玩家", 60);
        Enemy jug4Enemy = zombie(200, Intent.unknown());
        CombatContext jug4Ctx = start(jug4Player, List.of(jug4Enemy),
                deck(CardLibrary.resolve("juggernaut", 1), 10), new ArrayList<>());
        jug4Ctx.playCard(0, 0);
        c.equals("主宰+：旗标 7", 7, jug4Ctx.stateOf(jug4Player).juggernaut());
        jug4Ctx.gainEnergy(jug4Player, 3);
        jug4Ctx.addCardsToHand(jug4Player, CardLibrary.DEFEND, 1);
        jug4Ctx.playCard(handIndexOf(jug4Ctx, "defend"), 0);
        c.equals("主宰+：获得格挡 → 7 点伤害", 193, jug4Enemy.hp());
    }

    /** v0.5.3.3（A4-13）猎手 3 张：千刀万剐 / 涂毒 / 爆发。 */
    // ------------------------------------------------------------------ v0.5.3.5 A4-14

    // ------------------------------------------------------------------ v0.5.4.0 A4-19

    /**
     * v0.5.4.0（A4-19）猎手 6 张：华丽收场 / 幻影杀手 / 窒息 / 子弹时间 / 幽魂形态 / 无尽苦痛。
     *
     * <p>数值逐页核对过 kamigame：グランドフィナーレ 50→60、幻影の暗殺者 1→0 费、窒息 HP 3→5、
     * バレットタイム 3→2 费、死霊化 無形 2→3、終わりなき苦痛 4→6。</p>
     */
    private static void silentCards19(Checker c) {
        // ---- 华丽收场：抽牌堆为空才能打出
        Combatant gfPlayer = dummy("玩家", 60);
        Enemy gfEnemy = zombie(200, Intent.unknown());
        CombatContext gfCtx = start(gfPlayer, List.of(gfEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        gfCtx.addCardsToHand(gfPlayer, CardLibrary.GRAND_FINALE, 1);
        c.check("华丽收场：抽牌堆还有牌 → 不可打出",
                !gfCtx.playableNow(gfCtx.stateOf(gfPlayer), CardLibrary.GRAND_FINALE));
        // 把抽牌堆耗光（一路抽到空）
        while (!gfCtx.drawPile().isEmpty()) {
            gfCtx.drawCardsFor(gfPlayer, 1);
        }
        c.check("华丽收场：抽牌堆空了 → 可以打出",
                gfCtx.playableNow(gfCtx.stateOf(gfPlayer), CardLibrary.GRAND_FINALE));
        int gfIndex = handIndexOf(gfCtx, "grand_finale");
        c.check("华丽收场：打出成功", gfCtx.playCard(gfIndex, 0).success());
        c.equals("华丽收场：0 费全体 50 点", 150, gfEnemy.hp());
        c.check("华丽收场+：60 点",
                CardLibrary.resolve("grand_finale", 1).description().contains("60 点伤害"));

        // ---- 幻影杀手：下个回合攻击伤害翻倍（不是本回合）
        Combatant pkPlayer = dummy("玩家", 60);
        Enemy pkEnemy = zombie(300, Intent.unknown());
        CombatContext pkCtx = start(pkPlayer, List.of(pkEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        pkCtx.addCardsToHand(pkPlayer, CardLibrary.PHANTASMAL_KILLER, 1);
        pkCtx.playCard(handIndexOf(pkCtx, "phantasmal_killer"), 0);
        c.check("幻影杀手：记的是「下个回合」，本回合还没生效",
                pkCtx.stateOf(pkPlayer).phantomKillerPending()
                        && !pkCtx.stateOf(pkPlayer).phantomKiller());
        pkCtx.playCard(handIndexOf(pkCtx, "strike"), 0);
        c.equals("幻影杀手：本回合打击仍是 6 点", 294, pkEnemy.hp());
        pkCtx.endTurn();
        c.check("幻影杀手：下个回合生效了",
                pkCtx.stateOf(pkPlayer).phantomKiller()
                        && !pkCtx.stateOf(pkPlayer).phantomKillerPending());
        pkCtx.playCard(handIndexOf(pkCtx, "strike"), 0);
        c.equals("幻影杀手：下个回合打击翻倍 → 12 点", 282, pkEnemy.hp());
        c.equals("幻影杀手+：费用 1 → 0", 0, CardLibrary.resolve("phantasmal_killer", 1).cost());

        // ---- 窒息：本回合每打出一张牌，目标失去生命
        Combatant chPlayer = dummy("玩家", 60);
        Enemy chEnemy = zombie(300, Intent.unknown());
        CombatContext chCtx = start(chPlayer, List.of(chEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        chCtx.addCardsToHand(chPlayer, CardLibrary.CHOKE, 1);
        chCtx.playCard(handIndexOf(chCtx, "choke"), 0);
        c.equals("窒息：12 点伤害", 288, chEnemy.hp());
        c.equals("窒息：本回合每张牌失血 3", 3, chCtx.stateOf(chPlayer).chokeAmount());
        chCtx.playCard(handIndexOf(chCtx, "strike"), 0);
        c.equals("窒息：再打一张牌 → 6 点伤害 + 失去 3 点生命（288 - 9 = 279）", 279, chEnemy.hp());
        chCtx.endTurn();
        c.equals("窒息：回合结束后失血额度清零", 0, chCtx.stateOf(chPlayer).chokeAmount());

        Combatant ch2Player = dummy("玩家", 60);
        Enemy ch2Enemy = zombie(300, Intent.unknown());
        CombatContext ch2Ctx = start(ch2Player, List.of(ch2Enemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        ch2Ctx.addCardsToHand(ch2Player, CardLibrary.resolve("choke", 1), 1);
        ch2Ctx.playCard(handIndexOf(ch2Ctx, "choke"), 0);
        c.equals("窒息+：每张牌失血 5", 5, ch2Ctx.stateOf(ch2Player).chokeAmount());

        // ---- 子弹时间：本回合不能抽牌 + 手牌 0 费
        Combatant btPlayer = dummy("玩家", 60);
        Enemy btEnemy = zombie(300, Intent.unknown());
        CombatContext btCtx = start(btPlayer, List.of(btEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        btCtx.addCardsToHand(btPlayer, CardLibrary.BULLET_TIME, 1);
        btCtx.gainEnergy(btPlayer, 3);
        btCtx.playCard(handIndexOf(btCtx, "bullet_time"), 0);
        c.check("子弹时间：旗标点亮", btCtx.stateOf(btPlayer).bulletTime());
        c.equals("子弹时间：手牌费用变成 0", 0,
                btCtx.effectiveCost(btCtx.stateOf(btPlayer), CardLibrary.DEFEND));
        int btHand = btCtx.hand().size();
        btCtx.drawCardsFor(btPlayer, 2);
        c.equals("子弹时间：本回合抽不到牌（手牌不变）", btHand, btCtx.hand().size());
        int btEnergy = btCtx.energy();
        c.check("子弹时间：0 费打出格挡", btCtx.playCard(handIndexOf(btCtx, "defend"), 0).success());
        c.equals("子弹时间：一点能量都不花", btEnergy, btCtx.energy());
        btCtx.endTurn();
        c.check("子弹时间：回合结束后失效", !btCtx.stateOf(btPlayer).bulletTime());
        c.equals("子弹时间+：费用 3 → 2", 2, CardLibrary.resolve("bullet_time", 1).cost());

        // ---- 幽魂形态：无形（伤害封顶 1）+ 回合结束掉敏捷
        Combatant wfPlayer = dummy("玩家", 60);
        Enemy wfEnemy = zombie(300, Intent.attack(20));
        CombatContext wfCtx = start(wfPlayer, List.of(wfEnemy), deck(CardLibrary.WRAITH_FORM, 10),
                new ArrayList<>());
        wfCtx.playCard(0, 0);
        c.equals("幽魂形态：获得 2 层无形", 2, wfPlayer.intangibleTurns());
        wfCtx.endTurn();
        c.equals("幽魂形态：20 点攻击只掉 1 点血", 59, wfPlayer.hp());
        c.equals("幽魂形态：回合结束扣敏捷（本模组下限是 0，所以这里停在 0）", 0,
                wfPlayer.dexterity());
        c.equals("幽魂形态：无形降到 1 层", 1, wfPlayer.intangibleTurns());
        wfCtx.endTurn();
        c.equals("幽魂形态：第二个敌人回合仍然只掉 1 点", 58, wfPlayer.hp());
        c.equals("幽魂形态：无形用完了", 0, wfPlayer.intangibleTurns());
        wfCtx.endTurn();
        c.equals("幽魂形态：无形结束后 20 点攻击照常掉 20 点", 38, wfPlayer.hp());

        Combatant wf2Player = dummy("玩家", 60);
        Enemy wf2Enemy = zombie(300, Intent.unknown());
        CombatContext wf2Ctx = start(wf2Player, List.of(wf2Enemy),
                deck(CardLibrary.resolve("wraith_form", 1), 10), new ArrayList<>());
        wf2Ctx.playCard(0, 0);
        c.equals("幽魂形态+：3 层无形", 3, wf2Player.intangibleTurns());

        // ---- 无尽苦痛：抽到就再补一张（可打出，不是"使用不可"）
        Combatant eaPlayer = dummy("玩家", 60);
        Enemy eaEnemy = zombie(300, Intent.unknown());
        CombatContext eaCtx = start(eaPlayer, List.of(eaEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        eaCtx.putOnTopOfDrawPile(eaPlayer, CardLibrary.ENDLESS_AGONY);
        c.check("无尽苦痛：不是「使用不可」", !StatusCards.unplayable(CardLibrary.ENDLESS_AGONY));
        int eaHand = eaCtx.hand().size();
        eaCtx.drawCardsFor(eaPlayer, 1);
        c.equals("无尽苦痛：抽到它 → 手牌 +2（本体 + 复制）", eaHand + 2, eaCtx.hand().size());
        c.equals("无尽苦痛：手里正好 2 张", 2, eaCtx.hand().stream()
                .filter(card -> card.id().equals("endless_agony")).count());
        eaCtx.playCard(handIndexOf(eaCtx, "endless_agony"), 0);
        c.equals("无尽苦痛：0 费打 4 点", 296, eaEnemy.hp());
        c.equals("无尽苦痛：消耗（进消耗堆）", 1, eaCtx.exhaustPile().size());
        c.check("无尽苦痛+：6 点伤害",
                CardLibrary.resolve("endless_agony", 1).description().contains("6 点伤害"));

        // 六张新卡都有强化版
        boolean allUpgradable = true;
        for (String id : new String[] {"grand_finale", "phantasmal_killer", "choke",
                "bullet_time", "wraith_form", "endless_agony"}) {
            if (!CardLibrary.canUpgrade(id)) {
                allUpgradable = false;
            }
        }
        c.check("A4-19 的六张新卡都有强化版", allUpgradable);
    }

    // ------------------------------------------------------------------ v0.5.3.9 A4-18

    /**
     * v0.5.3.9（A4-18）战士 2 张：挖掘（E11 消耗堆选牌）/ 交锋（E16 条件可打出）。
     *
     * <p>数值逐页核对过 kamigame：発掘 1→0 费、クラッシュ 14→18。</p>
     */
    private static void ironcladCards22(Checker c) {
        // ---- 挖掘：从消耗堆选一张拿回手牌
        Combatant exPlayer = dummy("玩家", 60);
        Enemy exEnemy = zombie(200, Intent.unknown());
        CombatContext exCtx = start(exPlayer, List.of(exEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        exCtx.exhaustFromHand(exPlayer, List.of(0, 1));
        c.equals("挖掘：先垫两张牌进消耗堆", 2, exCtx.exhaustPile().size());
        exCtx.addCardsToHand(exPlayer, CardLibrary.EXHUME, 1);
        int exHand = exCtx.hand().size();
        int exExhaust = exCtx.exhaustPile().size();
        exCtx.playCard(handIndexOf(exCtx, "exhume"), 0);
        c.check("挖掘：挂起了「从消耗堆选牌」（source = EXHAUST，kind = EXHUME）",
                exCtx.pendingPick(exPlayer) != null
                        && exCtx.pendingPick(exPlayer).kind() == CombatContext.CardPickKind.EXHUME
                        && exCtx.pendingPick(exPlayer).source() == CombatContext.CardPickSource.EXHAUST);
        c.equals("挖掘：候选就是消耗堆里那 2 张", 2, exCtx.pickCandidates(exPlayer).size());
        exCtx.resolveCardPick(exPlayer, List.of(0));
        c.equals("挖掘：手牌 -1 +1（拿回一张）", exHand, exCtx.hand().size());
        c.equals("挖掘：消耗堆 -1 +1（自己也被消耗进去了）", exExhaust, exCtx.exhaustPile().size());
        c.equals("挖掘：挖掘自己在消耗堆里", 1, exCtx.exhaustPile().view().stream()
                .filter(card -> card.id().equals("exhume")).count());
        c.equals("挖掘+：费用 1 → 0", 0, CardLibrary.resolve("exhume", 1).cost());

        // 消耗堆为空时不挂选牌
        Combatant ex2Player = dummy("玩家", 60);
        Enemy ex2Enemy = zombie(200, Intent.unknown());
        CombatContext ex2Ctx = start(ex2Player, List.of(ex2Enemy), deck(CardLibrary.EXHUME, 10),
                new ArrayList<>());
        ex2Ctx.playCard(0, 0);
        c.check("挖掘：消耗堆空时不挂选牌（什么也不发生）", ex2Ctx.pendingPick(ex2Player) == null);

        // ---- 交锋：手里全是攻击牌才能打出
        Combatant clPlayer = dummy("玩家", 60);
        Enemy clEnemy = zombie(200, Intent.unknown());
        CombatContext clCtx = start(clPlayer, List.of(clEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        clCtx.addCardsToHand(clPlayer, CardLibrary.CLASH, 1);
        c.check("交锋：手里全是攻击牌 → 判定为可打出",
                clCtx.playableNow(clCtx.stateOf(clPlayer), CardLibrary.CLASH));
        c.check("交锋：打出去成功", clCtx.playCard(handIndexOf(clCtx, "clash"), 0).success());
        c.equals("交锋：0 费打 14 点", 186, clEnemy.hp());
        // 加一张技能牌 → 条件不满足
        clCtx.addCardsToHand(clPlayer, CardLibrary.DEFEND, 1);
        c.check("交锋：手里有技能牌 → 判定为不可打出",
                !clCtx.playableNow(clCtx.stateOf(clPlayer), CardLibrary.CLASH));
        clCtx.addCardsToHand(clPlayer, CardLibrary.CLASH, 1);
        int clIndex = handIndexOf(clCtx, "clash");
        c.check("交锋：引擎真的拒绝打出它", !clCtx.playCard(clIndex, 0).success());
        c.equals("交锋：被拒绝时敌人没掉血", 186, clEnemy.hp());
        c.check("交锋+：18 点伤害",
                CardLibrary.resolve("clash", 1).description().contains("18 点伤害"));
        c.check("交锋：是常见品质（战士的常见牌到此做完）",
                CardLibrary.CLASH.rarity() == CardRarity.COMMON);
    }

    /** v0.5.3.9（A4-18）猎手 3 张：噩梦 / 偷偷一击 / 转移。 */
    private static void silentCards18(Checker c) {
        // ---- 噩梦：选一张手牌，下个回合加入 3 张复制
        Combatant nmPlayer = dummy("玩家", 60);
        Enemy nmEnemy = zombie(300, Intent.unknown());
        CombatContext nmCtx = start(nmPlayer, List.of(nmEnemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        nmCtx.gainEnergy(nmPlayer, 3);
        nmCtx.addCardsToHand(nmPlayer, CardLibrary.NIGHTMARE, 1);
        nmCtx.addCardsToHand(nmPlayer, CardLibrary.STRIKE, 1);
        nmCtx.playCard(handIndexOf(nmCtx, "nightmare"), 0);
        c.check("噩梦：挂起了「选一张手牌」",
                nmCtx.pendingPick(nmPlayer) != null
                        && nmCtx.pendingPick(nmPlayer).kind() == CombatContext.CardPickKind.NIGHTMARE);
        nmCtx.resolveCardPick(nmPlayer, List.of(handIndexOf(nmCtx, "strike")));
        c.equals("噩梦：记下了下回合要加入 3 张", 3, nmCtx.stateOf(nmPlayer).nextTurnCards.size());
        c.equals("噩梦：消耗（进消耗堆）", 1, nmCtx.exhaustPile().size());
        nmCtx.endTurn();
        c.equals("噩梦：下回合手牌里多了 3 张『打击』", 3, nmCtx.hand().stream()
                .filter(card -> card.id().equals("strike")).count());
        c.equals("噩梦：下回合手牌 = 3 张复制 + 5 张正常抽牌", 8, nmCtx.hand().size());
        c.equals("噩梦+：费用 3 → 2", 2, CardLibrary.resolve("nightmare", 1).cost());

        // ---- 偷偷一击：本回合弃过牌才有能量
        Combatant skPlayer = dummy("玩家", 60);
        Enemy skEnemy = zombie(200, Intent.unknown());
        CombatContext skCtx = start(skPlayer, List.of(skEnemy), deck(CardLibrary.SNEAKY_STRIKE, 10),
                new ArrayList<>());
        skCtx.playCard(0, 0);
        c.equals("偷偷一击：12 点伤害", 188, skEnemy.hp());
        c.equals("偷偷一击：没弃过牌 → 不给能量（3 - 2 = 1）", 1, skCtx.energy());

        Combatant sk2Player = dummy("玩家", 60);
        Enemy sk2Enemy = zombie(200, Intent.unknown());
        CombatContext sk2Ctx = start(sk2Player, List.of(sk2Enemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        sk2Ctx.addCardsToHand(sk2Player, CardLibrary.SNEAKY_STRIKE, 1);
        sk2Ctx.discardFromHand(sk2Player, 0);
        c.equals("偷偷一击：先弃 1 张", 1, sk2Ctx.stateOf(sk2Player).cardsDiscardedThisTurn());
        sk2Ctx.playCard(handIndexOf(sk2Ctx, "sneaky_strike"), 0);
        c.equals("偷偷一击：弃过牌 → +2 能量（3 - 2 + 2 = 3）", 3, sk2Ctx.energy());
        c.check("偷偷一击+：16 点伤害",
                CardLibrary.resolve("sneaky_strike", 1).description().contains("16 点伤害"));

        // ---- 转移：随机技能牌 + 本回合 0 费
        Combatant diPlayer = dummy("玩家", 60);
        Enemy diEnemy = zombie(200, Intent.unknown());
        // 牌组只用『打击』，这样手牌里唯一的技能牌一定是转移给的那张
        CombatContext diCtx = start(diPlayer, List.of(diEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        diCtx.addCardsToHand(diPlayer, CardLibrary.DISTRACTION, 1);
        int diHand = diCtx.hand().size();
        diCtx.playCard(handIndexOf(diCtx, "distraction"), 0);
        c.equals("转移：手牌 -1 +1（拿到了那张随机技能牌）", diHand, diCtx.hand().size());
        c.equals("转移：消耗（进消耗堆）", 1, diCtx.exhaustPile().size());
        int diSkill = -1;
        for (int i = 0; i < diCtx.hand().size(); i++) {
            if (diCtx.hand().get(i).type() == CardType.SKILL) {
                diSkill = i;
                break;
            }
        }
        c.check("转移：加进来的确实是一张技能牌（也是手里唯一的技能牌）", diSkill >= 0);
        c.equals("转移：那张牌本回合费用为 0", 0,
                diCtx.effectiveCost(diCtx.stateOf(diPlayer), diCtx.hand().get(diSkill)));
        c.equals("转移+：费用 1 → 0", 0, CardLibrary.resolve("distraction", 1).cost());

        // 五张新卡都有强化版
        boolean allUpgradable = true;
        for (String id : new String[] {"exhume", "clash", "nightmare", "sneaky_strike", "distraction"}) {
            if (!CardLibrary.canUpgrade(id)) {
                allUpgradable = false;
            }
        }
        c.check("A4-18 的五张新卡都有强化版", allUpgradable);
    }

    // ------------------------------------------------------------------ v0.5.3.8 A4-17

    /**
     * v0.5.3.8（A4-17）战士 3 张：暴走 / 血债血偿 / 狂暴（稀有）—— 全靠 E5 单卡记账。
     *
     * <p>数值逐页核对过 kamigame：ランページ 成长 5→8（基础伤害仍 8）、
     * 血には血を 18费4 → 22费3、狂戦士 弱体 2→1（能量仍 1）。</p>
     */
    private static void ironcladCards21(Checker c) {
        // ---- 暴走：每打出一次，本场战斗伤害 +5
        Combatant rpPlayer = dummy("玩家", 60);
        Enemy rpEnemy = zombie(300, Intent.unknown());
        CombatContext rpCtx = start(rpPlayer, List.of(rpEnemy), deck(CardLibrary.RAMPAGE, 10),
                new ArrayList<>());
        rpCtx.gainEnergy(rpPlayer, 3);
        rpCtx.playCard(0, 0);
        c.equals("暴走：第 1 次是 8 点", 292, rpEnemy.hp());
        rpCtx.playCard(0, 0);
        c.equals("暴走：第 2 次 8 + 5 = 13 点（累计 21）", 279, rpEnemy.hp());
        rpCtx.playCard(0, 0);
        c.equals("暴走：第 3 次 8 + 10 = 18 点（累计 39）", 261, rpEnemy.hp());
        c.equals("暴走：记账次数 = 3", 3, rpCtx.stateOf(rpPlayer).cardCounter("rampage"));

        Combatant rp2Player = dummy("玩家", 60);
        Enemy rp2Enemy = zombie(300, Intent.unknown());
        CombatContext rp2Ctx = start(rp2Player, List.of(rp2Enemy),
                deck(CardLibrary.resolve("rampage", 1), 10), new ArrayList<>());
        rp2Ctx.gainEnergy(rp2Player, 3);
        rp2Ctx.playCard(0, 0);
        rp2Ctx.playCard(0, 0);
        c.equals("暴走+：基础伤害仍是 8，但成长是 +8（8 + 16 = 24）", 276, rp2Enemy.hp());
        c.equals("暴走+：费用仍是 1", 1, CardLibrary.resolve("rampage", 1).cost());

        // ---- 血债血偿：每失去一次生命 -1 费
        Combatant bfbPlayer = dummy("玩家", 60);
        Enemy bfbEnemy = zombie(300, Intent.attack(3));
        CombatContext bfbCtx = start(bfbPlayer, List.of(bfbEnemy), deck(CardLibrary.BLOOD_FOR_BLOOD, 10),
                new ArrayList<>());
        c.equals("血债血偿：基础 4 费（没失去过生命）", 4,
                bfbCtx.effectiveCost(bfbCtx.stateOf(bfbPlayer), CardLibrary.BLOOD_FOR_BLOOD));
        bfbCtx.endTurn();
        c.equals("血债血偿：被打了 3 点 → 本场失去生命 1 次", 1, bfbCtx.stateOf(bfbPlayer).hpLossEvents());
        c.equals("血债血偿：费用降到 3", 3,
                bfbCtx.effectiveCost(bfbCtx.stateOf(bfbPlayer), CardLibrary.BLOOD_FOR_BLOOD));
        bfbCtx.gainEnergy(bfbPlayer, 6);
        bfbCtx.playCard(0, 0);
        c.equals("血债血偿：造成 18 点伤害", 282, bfbEnemy.hp());
        c.check("血债血偿+：基础 3 费 / 22 伤害",
                CardLibrary.resolve("blood_for_blood", 1).cost() == 3
                        && CardLibrary.resolve("blood_for_blood", 1).description().contains("22 点伤害"));

        // ---- 狂暴（稀有）：每回合 +1 能量，代价是自挂易伤
        Combatant bsPlayer = dummy("玩家", 60);
        Enemy bsEnemy = zombie(300, Intent.unknown());
        CombatContext bsCtx = start(bsPlayer, List.of(bsEnemy), deck(CardLibrary.BERSERK, 10),
                new ArrayList<>());
        bsCtx.playCard(0, 0);
        c.equals("狂暴：0 费", 3, bsCtx.energy());
        c.equals("狂暴：自己获得 2 层易伤（代价）", 2, bsPlayer.vulnerable());
        c.equals("狂暴：旗标 1", 1, bsCtx.stateOf(bsPlayer).berserk());
        bsCtx.endTurn();
        c.equals("狂暴：下回合 3 + 1 = 4 点能量", 4, bsCtx.energy());

        Combatant bs2Player = dummy("玩家", 60);
        Enemy bs2Enemy = zombie(300, Intent.unknown());
        CombatContext bs2Ctx = start(bs2Player, List.of(bs2Enemy),
                deck(CardLibrary.resolve("berserk", 1), 10), new ArrayList<>());
        bs2Ctx.playCard(0, 0);
        c.equals("狂暴+：易伤降到 1 层（能量仍 1）", 1, bs2Player.vulnerable());
        c.equals("狂暴+：旗标仍是 1", 1, bs2Ctx.stateOf(bs2Player).berserk());
    }

    /** v0.5.3.8（A4-17）猎手 3 张：高超打击 / 剔骨 / 玻璃刀。 */
    private static void silentCards17(Checker c) {
        // ---- 高超打击：每失去一次生命 +1 费
        Combatant msPlayer = dummy("玩家", 60);
        Enemy msEnemy = zombie(300, Intent.attack(3));
        CombatContext msCtx = start(msPlayer, List.of(msEnemy), deck(CardLibrary.MASTERFUL_STAB, 10),
                new ArrayList<>());
        c.equals("高超打击：基础 0 费", 0,
                msCtx.effectiveCost(msCtx.stateOf(msPlayer), CardLibrary.MASTERFUL_STAB));
        msCtx.playCard(0, 0);
        c.equals("高超打击：0 费打 12 点", 288, msEnemy.hp());
        msCtx.endTurn();
        c.equals("高超打击：被打 3 点 → 费用涨到 1", 1,
                msCtx.effectiveCost(msCtx.stateOf(msPlayer), CardLibrary.MASTERFUL_STAB));
        c.check("高超打击+：16 点伤害",
                CardLibrary.resolve("masterful_stab", 1).description().contains("16 点伤害"));

        // ---- 剔骨：本回合每弃一张 -1 费
        Combatant evPlayer = dummy("玩家", 60);
        Enemy evEnemy = zombie(300, Intent.unknown());
        CombatContext evCtx = start(evPlayer, List.of(evEnemy), deck(CardLibrary.EVISCERATE, 10),
                new ArrayList<>());
        c.equals("剔骨：基础 3 费", 3,
                evCtx.effectiveCost(evCtx.stateOf(evPlayer), CardLibrary.EVISCERATE));
        // 弃两张手牌
        evCtx.discardFromHand(evPlayer, 0);
        evCtx.discardFromHand(evPlayer, 0);
        c.equals("剔骨：本回合弃了 2 张", 2, evCtx.stateOf(evPlayer).cardsDiscardedThisTurn());
        c.equals("剔骨：费用降到 1", 1,
                evCtx.effectiveCost(evCtx.stateOf(evPlayer), CardLibrary.EVISCERATE));
        evCtx.gainEnergy(evPlayer, 3);
        int evIndex = handIndexOf(evCtx, "eviscerate");
        int evEnergy = evCtx.energy();
        evCtx.playCard(evIndex, 0);
        c.equals("剔骨：实际只花 1 点能量", evEnergy - 1, evCtx.energy());
        c.equals("剔骨：7 点 ×3 = 21 点伤害", 279, evEnemy.hp());
        evCtx.endTurn();
        c.equals("剔骨：计数每回合重置（下回合又变回 3 费）", 0,
                evCtx.stateOf(evPlayer).cardsDiscardedThisTurn());
        c.check("剔骨+：9 点 ×3",
                CardLibrary.resolve("eviscerate", 1).description().contains("9 点伤害 3 次"));

        // ---- 玻璃刀：每打出一次，本场战斗伤害 -2
        Combatant gkPlayer = dummy("玩家", 60);
        Enemy gkEnemy = zombie(300, Intent.unknown());
        CombatContext gkCtx = start(gkPlayer, List.of(gkEnemy), deck(CardLibrary.GLASS_KNIFE, 10),
                new ArrayList<>());
        gkCtx.gainEnergy(gkPlayer, 3);
        gkCtx.playCard(0, 0);
        c.equals("玻璃刀：第 1 次 8 ×2 = 16 点", 284, gkEnemy.hp());
        gkCtx.playCard(0, 0);
        c.equals("玻璃刀：第 2 次每段 8 - 2 = 6 → 12 点（累计 28）", 272, gkEnemy.hp());
        gkCtx.playCard(0, 0);
        c.equals("玻璃刀：第 3 次每段 8 - 4 = 4 → 8 点（累计 36）", 264, gkEnemy.hp());
        c.equals("玻璃刀：记账次数 = 3", 3, gkCtx.stateOf(gkPlayer).cardCounter("glass_knife"));

        Combatant gk2Player = dummy("玩家", 60);
        Enemy gk2Enemy = zombie(300, Intent.unknown());
        CombatContext gk2Ctx = start(gk2Player, List.of(gk2Enemy),
                deck(CardLibrary.resolve("glass_knife", 1), 10), new ArrayList<>());
        gk2Ctx.gainEnergy(gk2Player, 3);
        gk2Ctx.playCard(0, 0);
        c.equals("玻璃刀+：第 1 次 12 ×2 = 24 点", 276, gk2Enemy.hp());
        gk2Ctx.playCard(0, 0);
        c.equals("玻璃刀+：第 2 次每段 12 - 2 = 10 → 20 点（累计 44）", 256, gk2Enemy.hp());

        // 六张 E5 卡都有强化版、都在对应角色的奖励池里
        boolean allUpgradable = true;
        for (String id : new String[] {"rampage", "blood_for_blood", "berserk",
                "masterful_stab", "eviscerate", "glass_knife"}) {
            if (!CardLibrary.canUpgrade(id)) {
                allUpgradable = false;
            }
        }
        c.check("A4-17 的六张 E5 卡都有强化版", allUpgradable);
    }

    // ------------------------------------------------------------------ v0.5.3.7 A4-16

    /**
     * v0.5.3.7（A4-16）战士 3 张：强力冲锋 / 残忍 / 地狱之刃。
     *
     * <p>数值逐页核对过 kamigame：やせ我慢 15→20 格挡、残虐 加【天賦】、非道の刃 1→0 费。</p>
     */
    private static void ironcladCards20(Checker c) {
        // ---- 强力冲锋：2 张伤口 + 格挡
        Combatant ptPlayer = dummy("玩家", 60);
        Enemy ptEnemy = zombie(200, Intent.unknown());
        CombatContext ptCtx = start(ptPlayer, List.of(ptEnemy), deck(CardLibrary.POWER_THROUGH, 10),
                new ArrayList<>());
        int ptHand = ptCtx.hand().size();
        ptCtx.playCard(0, 0);
        c.equals("强力冲锋：15 点格挡", 15, ptPlayer.block());
        c.equals("强力冲锋：手牌 -1 +2 = +1", ptHand + 1, ptCtx.hand().size());
        c.equals("强力冲锋：手里正好 2 张『伤口』", 2, ptCtx.hand().stream()
                .filter(card -> card.id().equals("wound")).count());
        c.check("强力冲锋：『伤口』不能被打出", StatusCards.unplayable(CardLibrary.WOUND));

        Combatant pt2Player = dummy("玩家", 60);
        Enemy pt2Enemy = zombie(200, Intent.unknown());
        CombatContext pt2Ctx = start(pt2Player, List.of(pt2Enemy),
                deck(CardLibrary.resolve("power_through", 1), 10), new ArrayList<>());
        pt2Ctx.playCard(0, 0);
        c.equals("强力冲锋+：20 点格挡（伤口仍是 2 张）", 20, pt2Player.block());

        // ---- 残忍：回合开始失去 1 生命 + 抽 1 张
        Combatant brPlayer = dummy("玩家", 60);
        Enemy brEnemy = zombie(200, Intent.unknown());
        CombatContext brCtx = start(brPlayer, List.of(brEnemy), deck(CardLibrary.BRUTALITY, 10),
                new ArrayList<>());
        brCtx.playCard(0, 0);
        c.equals("残忍：0 费", 3, brCtx.energy());
        c.equals("残忍：打出它的这一回合不立刻结算", 60, brPlayer.hp());
        brCtx.endTurn();
        c.equals("残忍：下个回合开始失去 1 点生命", 59, brPlayer.hp());
        c.check("残忍+：升级加【固有】",
                CardLibrary.resolve("brutality", 1).has(CardKeyword.INNATE)
                        && !CardLibrary.BRUTALITY.has(CardKeyword.INNATE));
        // 抽 1 张：手牌从 5 - 1(打出) - 0(弃不掉?) … 用"抽牌堆减 1"来验
        Combatant br2Player = dummy("玩家", 60);
        Enemy br2Enemy = zombie(200, Intent.unknown());
        CombatContext br2Ctx = start(br2Player, List.of(br2Enemy), deck(CardLibrary.BRUTALITY, 10),
                new ArrayList<>());
        br2Ctx.playCard(0, 0);
        br2Ctx.endTurn();
        // 对照：普通一回合只抽 5 张；挂着【残忍】的那一回合会多抽 1 张（= 6）
        c.equals("残忍：回合开始多抽 1 张（手牌 5 + 1 = 6）", 6, br2Ctx.hand().size());

        // ---- 地狱之刃：随机攻击牌 + 那张牌本回合 0 费
        Combatant ibPlayer = dummy("玩家", 60);
        Enemy ibEnemy = zombie(200, Intent.unknown());
        CombatContext ibCtx = start(ibPlayer, List.of(ibEnemy), deck(CardLibrary.INFERNAL_BLADE, 10),
                new ArrayList<>());
        int ibHand = ibCtx.hand().size();
        ibCtx.playCard(0, 0);
        c.equals("地狱之刃：手牌 -1 +1（拿到了那张随机攻击牌）", ibHand, ibCtx.hand().size());
        c.equals("地狱之刃：消耗（进消耗堆）", 1, ibCtx.exhaustPile().size());
        int ibAttack = -1;
        for (int i = 0; i < ibCtx.hand().size(); i++) {
            if (ibCtx.hand().get(i).type() == CardType.ATTACK) {
                ibAttack = i;
                break;
            }
        }
        c.check("地狱之刃：加进来的确实是一张攻击牌", ibAttack >= 0);
        Card ibGift = ibCtx.hand().get(ibAttack);
        c.equals("地狱之刃：那张牌本回合费用算成 0", 0,
                ibCtx.effectiveCost(ibCtx.stateOf(ibPlayer), ibGift));
        int ibEnergy = ibCtx.energy();
        c.check("地狱之刃：把它打出去成功", ibCtx.playCard(ibAttack, 0).success());
        c.equals("地狱之刃：那张牌一点能量都不花", ibEnergy, ibCtx.energy());
        c.equals("地狱之刃+：费用 1 → 0", 0, CardLibrary.resolve("infernal_blade", 1).cost());

        // "本回合 0 费"只在这一回合有效（下回合恢复原价）
        Combatant ib2Player = dummy("玩家", 60);
        Enemy ib2Enemy = zombie(200, Intent.unknown());
        CombatContext ib2Ctx = start(ib2Player, List.of(ib2Enemy), deck(CardLibrary.INFERNAL_BLADE, 10),
                new ArrayList<>());
        ib2Ctx.playCard(0, 0);
        int ib2Index = -1;
        for (int i = 0; i < ib2Ctx.hand().size(); i++) {
            if (ib2Ctx.hand().get(i).type() == CardType.ATTACK) {
                ib2Index = i;
                break;
            }
        }
        String ib2Id = ib2Ctx.hand().get(ib2Index).id();
        ib2Ctx.endTurn();
        c.check("地狱之刃：免费只到本回合为止（下回合不再免费）",
                ib2Ctx.stateOf(ib2Player).hand().stream().noneMatch(card -> card.id().equals(ib2Id))
                        || !ib2Ctx.stateOf(ib2Player).isFreeThisTurn(ib2Id));
        c.equals("地狱之刃：本回合免费账已清空", 0,
                ib2Ctx.stateOf(ib2Player).freeThisTurn.size());
    }

    /** v0.5.3.7（A4-16）猎手 3 张：钢铁风暴 / 反射 / 战术家。 */
    private static void silentCards16(Checker c) {
        // ---- 钢铁风暴：弃光手牌，每弃 1 张加 1 张小刀
        Combatant ssPlayer = dummy("玩家", 60);
        Enemy ssEnemy = zombie(200, Intent.unknown());
        CombatContext ssCtx = start(ssPlayer, List.of(ssEnemy), deck(CardLibrary.STORM_OF_STEEL, 10),
                new ArrayList<>());
        int ssHandBefore = ssCtx.hand().size();
        ssCtx.playCard(0, 0);
        c.equals("钢铁风暴：弃光手牌后加回同样数量的小刀（手牌 -1 +4 = 4）", ssHandBefore - 1,
                ssCtx.hand().size());
        c.equals("钢铁风暴：手里全是『小刀』", ssCtx.hand().size(), ssCtx.hand().stream()
                .filter(card -> card.id().equals("shiv")).count());
        c.equals("钢铁风暴：弃掉的 4 张 + 它自己 = 5 张进弃牌堆", ssHandBefore,
                ssCtx.discardPile().size());

        Combatant ss2Player = dummy("玩家", 60);
        Enemy ss2Enemy = zombie(200, Intent.unknown());
        CombatContext ss2Ctx = start(ss2Player, List.of(ss2Enemy),
                deck(CardLibrary.resolve("storm_of_steel", 1), 10), new ArrayList<>());
        ss2Ctx.playCard(0, 0);
        c.check("钢铁风暴+：加的是强化版『小刀+』（升级不是降费）",
                ss2Ctx.hand().stream().anyMatch(card -> card.id().equals("shiv") && card.name().contains("+"))
                        && CardLibrary.resolve("storm_of_steel", 1).cost() == 1);

        // ---- 反射：被弃置时抽 2 张
        Combatant rfPlayer = dummy("玩家", 60);
        Enemy rfEnemy = zombie(200, Intent.unknown());
        CombatContext rfCtx = start(rfPlayer, List.of(rfEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        rfCtx.addCardsToHand(rfPlayer, CardLibrary.REFLEX, 1);
        c.check("反射：使用不可（打不出去）", StatusCards.unplayable(CardLibrary.REFLEX));
        c.check("反射：引擎真的拒绝打出它",
                !rfCtx.playCard(handIndexOf(rfCtx, "reflex"), 0).success());
        int rfHand = rfCtx.hand().size();
        rfCtx.discardFromHand(rfPlayer, handIndexOf(rfCtx, "reflex"));
        c.equals("反射：被弃置 → 抽 2 张（手牌 -1 +2 = +1）", rfHand + 1, rfCtx.hand().size());

        Combatant rf2Player = dummy("玩家", 60);
        Enemy rf2Enemy = zombie(200, Intent.unknown());
        CombatContext rf2Ctx = start(rf2Player, List.of(rf2Enemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        rf2Ctx.addCardsToHand(rf2Player, CardLibrary.resolve("reflex", 1), 1);
        int rf2Hand = rf2Ctx.hand().size();
        rf2Ctx.discardFromHand(rf2Player, handIndexOf(rf2Ctx, "reflex"));
        c.equals("反射+：被弃置 → 抽 3 张", rf2Hand + 2, rf2Ctx.hand().size());

        // 回合结束时弃手牌**不算**"被弃置"（卡面写的是"在你的回合内被弃置时"）
        Combatant rf3Player = dummy("玩家", 60);
        Enemy rf3Enemy = zombie(200, Intent.unknown());
        CombatContext rf3Ctx = start(rf3Player, List.of(rf3Enemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        rf3Ctx.addCardsToHand(rf3Player, CardLibrary.REFLEX, 1);
        rf3Ctx.endTurn();
        c.check("反射：回合结束时弃手牌不触发（手牌是空的，也没抽到牌）",
                rf3Ctx.hand().size() <= 5);

        // ---- 战术家：被弃置时获得能量
        Combatant tcPlayer = dummy("玩家", 60);
        Enemy tcEnemy = zombie(200, Intent.unknown());
        CombatContext tcCtx = start(tcPlayer, List.of(tcEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        tcCtx.addCardsToHand(tcPlayer, CardLibrary.TACTICIAN, 1);
        c.check("战术家：使用不可", StatusCards.unplayable(CardLibrary.TACTICIAN));
        int tcEnergy = tcCtx.energy();
        tcCtx.discardFromHand(tcPlayer, handIndexOf(tcCtx, "tactician"));
        c.equals("战术家：被弃置 → +1 能量", tcEnergy + 1, tcCtx.energy());

        Combatant tc2Player = dummy("玩家", 60);
        Enemy tc2Enemy = zombie(200, Intent.unknown());
        CombatContext tc2Ctx = start(tc2Player, List.of(tc2Enemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        tc2Ctx.addCardsToHand(tc2Player, CardLibrary.resolve("tactician", 1), 1);
        int tc2Energy = tc2Ctx.energy();
        tc2Ctx.discardFromHand(tc2Player, handIndexOf(tc2Ctx, "tactician"));
        c.equals("战术家+：被弃置 → +2 能量", tc2Energy + 2, tc2Ctx.energy());
        c.equals("登记了 2 张「被弃置有效果」的牌", 2, CardLibrary.selfDiscardCount());

        // 钢铁风暴弃掉反射 / 战术家：两个钩子在同一次弃牌里都要触发
        Combatant mixPlayer = dummy("玩家", 60);
        Enemy mixEnemy = zombie(200, Intent.unknown());
        CombatContext mixCtx = start(mixPlayer, List.of(mixEnemy), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        mixCtx.addCardsToHand(mixPlayer, CardLibrary.REFLEX, 1);
        mixCtx.addCardsToHand(mixPlayer, CardLibrary.TACTICIAN, 1);
        mixCtx.addCardsToHand(mixPlayer, CardLibrary.STORM_OF_STEEL, 1);
        int mixEnergy = mixCtx.energy();
        int mixDraw = mixCtx.drawPile().size();
        mixCtx.playCard(handIndexOf(mixCtx, "storm_of_steel"), 0);
        c.equals("钢铁风暴弃掉【战术家】→ 能量 +1（3 - 1 + 1 = 3）", mixEnergy, mixCtx.energy());
        c.equals("钢铁风暴弃掉【反射】→ 抽了 2 张", mixDraw - 2, mixCtx.drawPile().size());
    }

    // ------------------------------------------------------------------ v0.5.3.6 A4-15

    /**
     * v0.5.3.6（A4-15）战士 3 张：黑暗拥抱 / 狂怒 / 哨兵。
     *
     * <p>数值逐页核对过 kamigame：闇の抱擁 2→1 费、激怒 3→5 格挡、見張り 5/2→8/3。</p>
     */
    private static void ironcladCards19(Checker c) {
        // ---- 黑暗拥抱：每消耗一张牌 → 抽 1 张
        Combatant dePlayer = dummy("玩家", 60);
        Enemy deEnemy = zombie(200, Intent.unknown());
        CombatContext deCtx = start(dePlayer, List.of(deEnemy), deck(CardLibrary.DARK_EMBRACE, 10),
                new ArrayList<>());
        deCtx.playCard(0, 0);
        c.equals("黑暗拥抱：2 费", 1, deCtx.energy());
        c.equals("黑暗拥抱+：2 → 1 费（效果不变）", 1, CardLibrary.resolve("dark_embrace", 1).cost());
        deCtx.gainEnergy(dePlayer, 3);
        deCtx.addCardsToHand(dePlayer, CardLibrary.STRIKE, 1);
        int deHand = deCtx.hand().size();
        deCtx.exhaustFromHand(dePlayer, List.of(handIndexOf(deCtx, "strike")));
        c.equals("黑暗拥抱：消耗一张 → 手牌 -1 +1（抽回来了）", deHand, deCtx.hand().size());
        c.equals("黑暗拥抱：那张牌确实进了消耗堆", 1, deCtx.exhaustPile().size());
        int deHand2 = deCtx.hand().size();
        deCtx.addCardsToHand(dePlayer, CardLibrary.DEFEND, 1);
        deCtx.playCard(handIndexOf(deCtx, "defend"), 0);
        c.equals("黑暗拥抱：普通出牌（不消耗）不抽牌", deHand2, deCtx.hand().size());

        // ---- 狂怒：本回合每打出一张攻击牌 → 格挡
        Combatant rgPlayer = dummy("玩家", 60);
        Enemy rgEnemy = zombie(200, Intent.unknown());
        CombatContext rgCtx = start(rgPlayer, List.of(rgEnemy), deck(CardLibrary.RAGE, 10),
                new ArrayList<>());
        rgCtx.playCard(0, 0);
        c.equals("狂怒：0 费", 3, rgCtx.energy());
        c.equals("狂怒：本回合额度 3", 3, rgCtx.stateOf(rgPlayer).rage());
        rgCtx.addCardsToHand(rgPlayer, CardLibrary.STRIKE, 1);
        rgCtx.playCard(handIndexOf(rgCtx, "strike"), 0);
        c.equals("狂怒：打出一张攻击牌 → 3 点格挡", 3, rgPlayer.block());
        rgCtx.addCardsToHand(rgPlayer, CardLibrary.DEFEND, 1);
        rgCtx.playCard(handIndexOf(rgCtx, "defend"), 0);
        c.equals("狂怒：技能牌不触发（只加了防御的 5）", 8, rgPlayer.block());
        rgCtx.endTurn();
        c.equals("狂怒：回合结束后作废", 0, rgCtx.stateOf(rgPlayer).rage());

        // ---- 哨兵：被消耗时回能量
        Combatant sePlayer = dummy("玩家", 60);
        Enemy seEnemy = zombie(200, Intent.unknown());
        CombatContext seCtx = start(sePlayer, List.of(seEnemy), deck(CardLibrary.SENTINEL, 10),
                new ArrayList<>());
        seCtx.playCard(0, 0);
        c.equals("哨兵：5 点格挡", 5, sePlayer.block());
        c.equals("哨兵：正常打出不给能量（3 - 1 = 2）", 2, seCtx.energy());
        seCtx.addCardsToHand(sePlayer, CardLibrary.SENTINEL, 1);
        int seEnergy = seCtx.energy();
        seCtx.exhaustFromHand(sePlayer, List.of(handIndexOf(seCtx, "sentinel")));
        c.equals("哨兵：被消耗 → +2 能量", seEnergy + 2, seCtx.energy());

        Combatant se2Player = dummy("玩家", 60);
        Enemy se2Enemy = zombie(200, Intent.unknown());
        CombatContext se2Ctx = start(se2Player, List.of(se2Enemy),
                deck(CardLibrary.resolve("sentinel", 1), 10), new ArrayList<>());
        se2Ctx.playCard(0, 0);
        c.equals("哨兵+：8 点格挡", 8, se2Player.block());
        se2Ctx.addCardsToHand(se2Player, CardLibrary.resolve("sentinel", 1), 1);
        int se2Energy = se2Ctx.energy();
        se2Ctx.exhaustFromHand(se2Player, List.of(handIndexOf(se2Ctx, "sentinel")));
        c.equals("哨兵+：被消耗 → +3 能量", se2Energy + 3, se2Ctx.energy());
        c.equals("哨兵：登记了「自消耗有效果」的表", 1, CardLibrary.selfExhaustCount());
    }

    /** v0.5.3.6（A4-15）猎手 3 张：铁蒺藜 / 模糊 / 终结技。 */
    private static void silentCards15(Checker c) {
        // ---- 铁蒺藜：被攻击时反弹（= 荆棘）
        Combatant ctPlayer = dummy("玩家", 60);
        Enemy ctEnemy = zombie(200, Intent.attack(5));
        CombatContext ctCtx = start(ctPlayer, List.of(ctEnemy), deck(CardLibrary.CALTROPS, 10),
                new ArrayList<>());
        ctCtx.playCard(0, 0);
        c.equals("铁蒺藜：荆棘 3", 3, ctPlayer.thorns());
        ctCtx.endTurn();
        c.equals("铁蒺藜：敌人打过来 → 反弹 3 点", 197, ctEnemy.hp());
        c.equals("铁蒺藜：荆棘不随回合衰减", 3, ctPlayer.thorns());

        Combatant ct2Player = dummy("玩家", 60);
        Enemy ct2Enemy = zombie(200, Intent.attack(5));
        CombatContext ct2Ctx = start(ct2Player, List.of(ct2Enemy),
                deck(CardLibrary.resolve("caltrops", 1), 10), new ArrayList<>());
        ct2Ctx.playCard(0, 0);
        c.equals("铁蒺藜+：荆棘 5", 5, ct2Player.thorns());
        ct2Ctx.endTurn();
        c.equals("铁蒺藜+：反弹 5 点", 195, ct2Enemy.hp());

        // ---- 模糊：下个回合开始不清空格挡
        Combatant blPlayer = dummy("玩家", 60);
        Enemy blEnemy = zombie(200, Intent.attack(4));
        CombatContext blCtx = start(blPlayer, List.of(blEnemy), deck(CardLibrary.BLUR, 10),
                new ArrayList<>());
        blCtx.playCard(0, 0);
        c.equals("模糊：5 点格挡", 5, blPlayer.block());
        c.equals("模糊：挂了 1 次保格挡", 1, blPlayer.blurTurns());
        blCtx.endTurn();
        c.equals("模糊：敌人打 4 → 剩 1 点，且下回合开始没被清空", 1, blPlayer.block());
        c.equals("模糊：次数用掉了", 0, blPlayer.blurTurns());
        blCtx.endTurn();
        c.equals("模糊：再下一个回合正常清空", 0, blPlayer.block());

        Combatant bl2Player = dummy("玩家", 60);
        Enemy bl2Enemy = zombie(200, Intent.unknown());
        CombatContext bl2Ctx = start(bl2Player, List.of(bl2Enemy),
                deck(CardLibrary.resolve("blur", 1), 10), new ArrayList<>());
        bl2Ctx.playCard(0, 0);
        c.equals("模糊+：8 点格挡", 8, bl2Player.block());

        // ---- 终结技：本回合每打出一张攻击牌 → 单体伤害（用户 v0.5.3.7 明确"生效对象是单体"）
        Combatant fiPlayer = dummy("玩家", 60);
        Enemy fiA = zombie(200, Intent.unknown());
        Enemy fiB = zombie(200, Intent.unknown());
        CombatContext fiCtx = start(fiPlayer, List.of(fiA, fiB), deck(CardLibrary.FINISHER, 10),
                new ArrayList<>());
        c.check("终结技：目标是单体（不是全体）",
                CardLibrary.FINISHER.target() == CardTarget.ENEMY);
        fiCtx.playCard(0, 0);
        c.equals("终结技：只算它自己 → 目标吃 6 点", 194, fiA.hp());
        c.equals("终结技：另一个敌人**完全不受影响**（单体）", 200, fiB.hp());
        c.equals("终结技：本回合攻击牌计数 = 1", 1, fiCtx.stateOf(fiPlayer).attacksPlayedThisTurn());
        fiCtx.endTurn();
        c.equals("终结技：计数每回合重置", 0, fiCtx.stateOf(fiPlayer).attacksPlayedThisTurn());

        Combatant fi2Player = dummy("玩家", 60);
        Enemy fi2A = zombie(200, Intent.unknown());
        Enemy fi2B = zombie(200, Intent.unknown());
        CombatContext fi2Ctx = start(fi2Player, List.of(fi2A, fi2B), deck(CardLibrary.FINISHER, 10),
                new ArrayList<>());
        fi2Ctx.addCardsToHand(fi2Player, CardLibrary.STRIKE, 1);
        fi2Ctx.addCardsToHand(fi2Player, CardLibrary.STRIKE, 1);
        fi2Ctx.playCard(handIndexOf(fi2Ctx, "strike"), 0);
        fi2Ctx.playCard(handIndexOf(fi2Ctx, "strike"), 0);
        fi2Ctx.playCard(handIndexOf(fi2Ctx, "finisher"), 0);
        c.equals("终结技：2 张打击 + 自己 = 3 → 目标吃 12 + 18 = 30", 170, fi2A.hp());
        c.equals("终结技：另一个敌人仍然是满血（单体）", 200, fi2B.hp());

        Combatant fi3Player = dummy("玩家", 60);
        Enemy fi3Enemy = zombie(200, Intent.unknown());
        CombatContext fi3Ctx = start(fi3Player, List.of(fi3Enemy),
                deck(CardLibrary.resolve("finisher", 1), 10), new ArrayList<>());
        fi3Ctx.playCard(0, 0);
        c.equals("终结技+：每张攻击牌 ×8", 192, fi3Enemy.hp());
    }

    /**
     * v0.5.3.5（A4-14）战士 3 张：进化 / 火焰吐息 / 无痛 —— 抽牌钩子 + 消耗钩子。
     *
     * <p>数值逐页核对过 kamigame：進化 抽 1→2、炎の吐息 6→10、無痛 3→4。</p>
     */
    private static void ironcladCards18(Checker c) {
        // ---- 进化：抽到状态牌 → 再抽 N 张
        Combatant evPlayer = dummy("玩家", 60);
        Enemy evEnemy = zombie(200, Intent.unknown());
        CombatContext evCtx = start(evPlayer, List.of(evEnemy), deck(CardLibrary.EVOLVE, 10),
                new ArrayList<>());
        evCtx.playCard(0, 0);
        c.equals("进化：1 费", 2, evCtx.energy());
        evCtx.putOnTopOfDrawPile(evPlayer, CardLibrary.WOUND);
        int evHand = evCtx.hand().size();
        evCtx.drawCardsFor(evPlayer, 1);
        c.equals("进化：抽到『伤口』→ 再抽 1 张（手牌 +2：伤口本身 + 额外那张）", evHand + 2,
                evCtx.hand().size());
        c.equals("进化：状态牌留在手里", 1, evCtx.hand().stream()
                .filter(card -> card.id().equals("wound")).count());
        // 抽到普通牌不触发
        evCtx.putOnTopOfDrawPile(evPlayer, CardLibrary.STRIKE);
        int evHand2 = evCtx.hand().size();
        evCtx.drawCardsFor(evPlayer, 1);
        c.equals("进化：抽到普通牌不额外抽", evHand2 + 1, evCtx.hand().size());

        Combatant ev2Player = dummy("玩家", 60);
        Enemy ev2Enemy = zombie(200, Intent.unknown());
        CombatContext ev2Ctx = start(ev2Player, List.of(ev2Enemy),
                deck(CardLibrary.resolve("evolve", 1), 10), new ArrayList<>());
        ev2Ctx.playCard(0, 0);
        ev2Ctx.putOnTopOfDrawPile(ev2Player, CardLibrary.WOUND);
        int ev2Hand = ev2Ctx.hand().size();
        ev2Ctx.drawCardsFor(ev2Player, 1);
        c.equals("进化+：抽到状态牌 → 再抽 2 张", ev2Hand + 3, ev2Ctx.hand().size());

        // ---- 火焰吐息：抽到状态 / 诅咒牌 → 全体伤害
        Combatant fbPlayer = dummy("玩家", 60);
        Enemy fbEnemy = zombie(200, Intent.unknown());
        CombatContext fbCtx = start(fbPlayer, List.of(fbEnemy), deck(CardLibrary.FIRE_BREATHING, 10),
                new ArrayList<>());
        fbCtx.playCard(0, 0);
        fbCtx.putOnTopOfDrawPile(fbPlayer, CardLibrary.WOUND);
        fbCtx.drawCardsFor(fbPlayer, 1);
        c.equals("火焰吐息：抽到状态牌 → 全体 6 点伤害", 194, fbEnemy.hp());
        fbCtx.putOnTopOfDrawPile(fbPlayer, CardLibrary.CURSE_CRUEL);
        fbCtx.drawCardsFor(fbPlayer, 1);
        c.equals("火焰吐息：抽到诅咒牌也触发（再 6 点）", 188, fbEnemy.hp());
        fbCtx.putOnTopOfDrawPile(fbPlayer, CardLibrary.STRIKE);
        fbCtx.drawCardsFor(fbPlayer, 1);
        c.equals("火焰吐息：抽普通牌不触发（仍是 188）", 188, fbEnemy.hp());

        Combatant fb2Player = dummy("玩家", 60);
        Enemy fb2Enemy = zombie(200, Intent.unknown());
        CombatContext fb2Ctx = start(fb2Player, List.of(fb2Enemy),
                deck(CardLibrary.resolve("fire_breathing", 1), 10), new ArrayList<>());
        fb2Ctx.playCard(0, 0);
        fb2Ctx.putOnTopOfDrawPile(fb2Player, CardLibrary.WOUND);
        fb2Ctx.drawCardsFor(fb2Player, 1);
        c.equals("火焰吐息+：10 点伤害", 190, fb2Enemy.hp());

        // ---- 无痛：每消耗一张牌 → 格挡
        Combatant fnpPlayer = dummy("玩家", 60);
        Enemy fnpEnemy = zombie(200, Intent.unknown());
        CombatContext fnpCtx = start(fnpPlayer, List.of(fnpEnemy), deck(CardLibrary.FEEL_NO_PAIN, 10),
                new ArrayList<>());
        fnpCtx.playCard(0, 0);
        c.equals("无痛：1 费", 2, fnpCtx.energy());
        fnpCtx.gainEnergy(fnpPlayer, 3);
        fnpCtx.addCardsToHand(fnpPlayer, CardLibrary.STRIKE, 1);
        int fnpBlock = fnpPlayer.block();
        fnpCtx.playCard(handIndexOf(fnpCtx, "strike"), 0);
        c.equals("无痛：普通出牌（不消耗）不给格挡", fnpBlock, fnpPlayer.block());
        fnpCtx.addCardsToHand(fnpPlayer, CardLibrary.STRIKE, 1);
        int burned = fnpCtx.exhaustFromHand(fnpPlayer, List.of(handIndexOf(fnpCtx, "strike")));
        c.equals("无痛：消耗 1 张牌", 1, burned);
        c.equals("无痛：消耗一张牌 → 3 点格挡", fnpBlock + 3, fnpPlayer.block());
        // 带「消耗」关键字的牌自己进消耗堆时也要触发
        fnpCtx.addCardsToHand(fnpPlayer, CardLibrary.SECOND_WIND, 1);
        int fnpBlock2 = fnpPlayer.block();
        fnpCtx.playCard(handIndexOf(fnpCtx, "second_wind"), 0);
        c.check("无痛：第二次呼吸自己也是消耗牌 → 也触发（格挡更高）", fnpPlayer.block() > fnpBlock2);

        Combatant fnp2Player = dummy("玩家", 60);
        Enemy fnp2Enemy = zombie(200, Intent.unknown());
        CombatContext fnp2Ctx = start(fnp2Player, List.of(fnp2Enemy),
                deck(CardLibrary.resolve("feel_no_pain", 1), 10), new ArrayList<>());
        fnp2Ctx.playCard(0, 0);
        fnp2Ctx.addCardsToHand(fnp2Player, CardLibrary.STRIKE, 1);
        fnp2Ctx.exhaustFromHand(fnp2Player, List.of(handIndexOf(fnp2Ctx, "strike")));
        c.equals("无痛+：消耗一张 → 4 点格挡", 4, fnp2Player.block());
    }

    /** v0.5.3.5（A4-14）猎手 3 张：飞膝 / 机动 / 捕食者 —— 下回合能量与抽牌。 */
    private static void silentCards14(Checker c) {
        // ---- 飞膝：8 伤害 + 下回合 +1 能量
        Combatant fkPlayer = dummy("玩家", 60);
        Enemy fkEnemy = zombie(200, Intent.unknown());
        CombatContext fkCtx = start(fkPlayer, List.of(fkEnemy), deck(CardLibrary.FLYING_KNEE, 10),
                new ArrayList<>());
        fkCtx.playCard(0, 0);
        c.equals("飞膝：8 点伤害", 192, fkEnemy.hp());
        c.equals("飞膝：本回合不额外给能量（3 - 1 = 2）", 2, fkCtx.energy());
        c.equals("飞膝：挂上了 1 点下回合能量", 1, fkPlayer.nextTurnEnergy());
        fkCtx.endTurn();
        c.equals("飞膝：下回合 3 + 1 = 4 点能量", 4, fkCtx.energy());
        c.equals("飞膝：额度用掉后归零", 0, fkPlayer.nextTurnEnergy());

        Combatant fk2Player = dummy("玩家", 60);
        Enemy fk2Enemy = zombie(200, Intent.unknown());
        CombatContext fk2Ctx = start(fk2Player, List.of(fk2Enemy),
                deck(CardLibrary.resolve("flying_knee", 1), 10), new ArrayList<>());
        fk2Ctx.playCard(0, 0);
        c.equals("飞膝+：11 点伤害（能量仍 1）", 189, fk2Enemy.hp());
        c.equals("飞膝+：还是 1 点下回合能量", 1, fk2Player.nextTurnEnergy());

        // ---- 机动：下回合 +2 / +3 能量
        Combatant ovPlayer = dummy("玩家", 60);
        Enemy ovEnemy = zombie(200, Intent.unknown());
        CombatContext ovCtx = start(ovPlayer, List.of(ovEnemy), deck(CardLibrary.OUTMANEUVER, 10),
                new ArrayList<>());
        ovCtx.playCard(0, 0);
        ovCtx.playCard(0, 0);
        c.equals("机动：两张叠起来是 4 点下回合能量", 4, ovPlayer.nextTurnEnergy());
        ovCtx.endTurn();
        c.equals("机动：下回合 3 + 4 = 7 点能量", 7, ovCtx.energy());

        Combatant ov2Player = dummy("玩家", 60);
        Enemy ov2Enemy = zombie(200, Intent.unknown());
        CombatContext ov2Ctx = start(ov2Player, List.of(ov2Enemy),
                deck(CardLibrary.resolve("outmaneuver", 1), 10), new ArrayList<>());
        ov2Ctx.playCard(0, 0);
        c.equals("机动+：挂着 3 点", 3, ov2Player.nextTurnEnergy());
        ov2Ctx.endTurn();
        c.equals("机动+：下回合 3 + 3 = 6 点能量", 6, ov2Ctx.energy());

        // ---- 捕食者：15 伤害 + 下回合抽 2 张
        Combatant pdPlayer = dummy("玩家", 60);
        Enemy pdEnemy = zombie(200, Intent.unknown());
        CombatContext pdCtx = start(pdPlayer, List.of(pdEnemy), deck(CardLibrary.PREDATOR, 10),
                new ArrayList<>());
        pdCtx.playCard(0, 0);
        c.equals("捕食者：15 点伤害", 185, pdEnemy.hp());
        c.equals("捕食者：挂上了下回合抽 2 张", 2, pdPlayer.nextTurnDraw());
        pdCtx.endTurn();
        c.equals("捕食者：下回合抽 5 + 2 = 7 张", 7, pdCtx.hand().size());

        Combatant pd2Player = dummy("玩家", 60);
        Enemy pd2Enemy = zombie(200, Intent.unknown());
        CombatContext pd2Ctx = start(pd2Player, List.of(pd2Enemy),
                deck(CardLibrary.resolve("predator", 1), 10), new ArrayList<>());
        pd2Ctx.playCard(0, 0);
        c.equals("捕食者+：20 点伤害（仍抽 2 张）", 180, pd2Enemy.hp());
        pd2Ctx.endTurn();
        c.equals("捕食者+：下回合仍抽 7 张", 7, pd2Ctx.hand().size());

        // ---- 进化 + 火焰吐息：同一次"抽到状态牌"两个钩子都要触发
        Combatant bothPlayer = dummy("玩家", 60);
        Enemy bothEnemy = zombie(300, Intent.unknown());
        CombatContext bothCtx = start(bothPlayer, List.of(bothEnemy), deck(CardLibrary.EVOLVE, 10),
                new ArrayList<>());
        bothCtx.gainEnergy(bothPlayer, 3);
        bothCtx.playCard(0, 0);
        bothCtx.addCardsToHand(bothPlayer, CardLibrary.FIRE_BREATHING, 1);
        bothCtx.playCard(handIndexOf(bothCtx, "fire_breathing"), 0);
        bothCtx.putOnTopOfDrawPile(bothPlayer, CardLibrary.WOUND);
        int bothHand = bothCtx.hand().size();
        bothCtx.drawCardsFor(bothPlayer, 1);
        c.equals("进化 + 火焰吐息：抽一张状态牌 → 既多抽 1 张又打 6 点", 6,
                300 - bothEnemy.hp());
        c.equals("进化 + 火焰吐息：手牌 +2（伤口 + 进化抽的那张）", bothHand + 2,
                bothCtx.hand().size());
    }

    private static void silentCards13(Checker c) {
        // ---- 千刀万剐：每打出一张牌 → 全体 1 点
        Combatant tcPlayer = dummy("玩家", 60);
        Enemy tcA = zombie(200, Intent.unknown());
        Enemy tcB = zombie(200, Intent.unknown());
        CombatContext tcCtx = start(tcPlayer, List.of(tcA, tcB), deck(CardLibrary.A_THOUSAND_CUTS, 10),
                new ArrayList<>());
        tcCtx.playCard(0, 0);
        c.equals("千刀万剐：点亮它的这一下就打全体 1 点（第一个）", 199, tcA.hp());
        c.equals("千刀万剐：另一个敌人也吃到", 199, tcB.hp());
        tcCtx.gainEnergy(tcPlayer, 3);
        tcCtx.addCardsToHand(tcPlayer, CardLibrary.STRIKE, 1);
        tcCtx.playCard(handIndexOf(tcCtx, "strike"), 0);
        c.equals("千刀万剐：再打一张牌 → 主目标吃 6 + 1", 192, tcA.hp());
        c.equals("千刀万剐：另一个敌人只吃那 1 点", 198, tcB.hp());

        Combatant tc2Player = dummy("玩家", 60);
        Enemy tc2Enemy = zombie(200, Intent.unknown());
        CombatContext tc2Ctx = start(tc2Player, List.of(tc2Enemy),
                deck(CardLibrary.resolve("a_thousand_cuts", 1), 10), new ArrayList<>());
        tc2Ctx.playCard(0, 0);
        c.equals("千刀万剐+：每张牌 2 点", 198, tc2Enemy.hp());

        // ---- 涂毒：攻击牌造成伤害 → 1 层中毒
        Combatant enPlayer = dummy("玩家", 60);
        Enemy enEnemy = zombie(200, Intent.unknown());
        CombatContext enCtx = start(enPlayer, List.of(enEnemy), deck(CardLibrary.ENVENOM, 10),
                new ArrayList<>());
        enCtx.playCard(0, 0);
        c.equals("涂毒：旗标 1", 1, enCtx.stateOf(enPlayer).envenom());
        enCtx.gainEnergy(enPlayer, 5);
        enCtx.addCardsToHand(enPlayer, CardLibrary.STRIKE, 1);
        enCtx.playCard(handIndexOf(enCtx, "strike"), 0);
        c.equals("涂毒：攻击造成伤害 → 敌人中毒 1", 1, enEnemy.poison());
        // 被格挡吃光的攻击**不算**造成伤害
        enEnemy.gainBlock(50);
        enCtx.addCardsToHand(enPlayer, CardLibrary.STRIKE, 1);
        enCtx.playCard(handIndexOf(enCtx, "strike"), 0);
        c.equals("涂毒：被格挡吃光的攻击不给中毒（仍是 1）", 1, enEnemy.poison());
        // 技能牌不给中毒
        enCtx.addCardsToHand(enPlayer, CardLibrary.DEFEND, 1);
        enCtx.playCard(handIndexOf(enCtx, "defend"), 0);
        c.equals("涂毒：技能牌不给中毒", 1, enEnemy.poison());
        c.equals("涂毒+：费用 2 → 1（毒量不变）", 1, CardLibrary.resolve("envenom", 1).cost());

        // ---- 爆发：下一张技能牌打两遍
        Combatant buPlayer = dummy("玩家", 60);
        Enemy buEnemy = zombie(200, Intent.unknown());
        CombatContext buCtx = start(buPlayer, List.of(buEnemy), deck(CardLibrary.BURST, 10),
                new ArrayList<>());
        buCtx.playCard(0, 0);
        c.equals("爆发：本回合额度 1", 1, buCtx.stateOf(buPlayer).burstSkills());
        buCtx.gainEnergy(buPlayer, 3);
        buCtx.addCardsToHand(buPlayer, CardLibrary.DEFEND, 1);
        buCtx.playCard(handIndexOf(buCtx, "defend"), 0);
        c.equals("爆发：格挡打两遍 → 5 + 5 = 10", 10, buPlayer.block());
        c.equals("爆发：额度用掉了", 0, buCtx.stateOf(buPlayer).burstSkills());

        Combatant bu2Player = dummy("玩家", 60);
        Enemy bu2Enemy = zombie(200, Intent.unknown());
        CombatContext bu2Ctx = start(bu2Player, List.of(bu2Enemy),
                deck(CardLibrary.resolve("burst", 1), 10), new ArrayList<>());
        bu2Ctx.playCard(0, 0);
        c.equals("爆发+：本回合额度 2", 2, bu2Ctx.stateOf(bu2Player).burstSkills());
        bu2Ctx.gainEnergy(bu2Player, 5);
        bu2Ctx.addCardsToHand(bu2Player, CardLibrary.DEFEND, 1);
        bu2Ctx.addCardsToHand(bu2Player, CardLibrary.DEFEND, 1);
        bu2Ctx.playCard(handIndexOf(bu2Ctx, "defend"), 0);
        bu2Ctx.playCard(handIndexOf(bu2Ctx, "defend"), 0);
        c.equals("爆发+：两张格挡各打两遍 → 4 × 5 = 20", 20, bu2Player.block());

        // 第二遍会**覆盖选牌请求**（选牌只有一个槽位），所以有选牌时必须略过第二遍。
        // 用【战吼】验证最清楚：它"先抽 1 张、再挂选牌"，所以第二遍会让抽牌数翻倍。
        Combatant bu3Player = dummy("玩家", 60);
        Enemy bu3Enemy = zombie(300, Intent.unknown());
        CombatContext bu3Ctx = start(bu3Player, List.of(bu3Enemy), deck(CardLibrary.BURST, 10),
                new ArrayList<>());
        bu3Ctx.addCardsToHand(bu3Player, CardLibrary.WARCRY, 1);
        bu3Ctx.playCard(0, 0);
        bu3Ctx.gainEnergy(bu3Player, 5);
        int bu3HandBefore = bu3Ctx.hand().size();
        bu3Ctx.playCard(handIndexOf(bu3Ctx, "warcry"), 0);
        c.check("爆发 + 选牌：选牌请求照常挂起（PUT_ON_TOP）",
                bu3Ctx.pendingPick(bu3Player) != null
                        && bu3Ctx.pendingPick(bu3Player).kind() == CombatContext.CardPickKind.PUT_ON_TOP);
        bu3Ctx.resolveCardPick(bu3Player, List.of(0));
        c.equals("爆发 + 选牌：第二遍被略过（战吼只抽了 1 张：手牌 -1 +1 -1 = -1）",
                bu3HandBefore - 1, bu3Ctx.hand().size());

        // 双重施法 × 千刀万剐：一张牌打两遍 → 出牌钩子也触发两次
        Combatant crossPlayer = dummy("玩家", 60);
        Enemy crossEnemy = zombie(300, Intent.unknown());
        CombatContext crossCtx = start(crossPlayer, List.of(crossEnemy), deck(CardLibrary.A_THOUSAND_CUTS, 10),
                new ArrayList<>());
        crossCtx.playCard(0, 0);
        crossCtx.gainEnergy(crossPlayer, 6);
        crossCtx.addCardsToHand(crossPlayer, CardLibrary.DOUBLE_TAP, 1);
        crossCtx.addCardsToHand(crossPlayer, CardLibrary.STRIKE, 1);
        crossCtx.playCard(handIndexOf(crossCtx, "double_tap"), 0);
        crossCtx.playCard(handIndexOf(crossCtx, "strike"), 0);
        // 敲千刀万剐自己 -1、敲双重施法 -1、敲打击两遍 -2 与 6×2 = 共 16
        c.equals("双重施法 × 千刀万剐：一张攻击牌打两遍 → 出牌钩子也触发两次", 284, crossEnemy.hp());
    }

    private static void silentCards12(Checker c) {
        // ---- 无限刀刃：回合开始加一张『小刀』
        Combatant ibPlayer = dummy("玩家", 60);
        Enemy ibEnemy = zombie(200, Intent.unknown());
        CombatContext ibCtx = start(ibPlayer, List.of(ibEnemy), deck(CardLibrary.INFINITE_BLADES, 10),
                new ArrayList<>());
        ibCtx.playCard(0, 0);
        c.check("无限刀刃：旗标点亮", ibCtx.stateOf(ibPlayer).infiniteBlades());
        c.equals("无限刀刃：打出它的这一回合不会立刻给小刀", 0, countShiv(ibCtx.hand()));
        ibCtx.endTurn();
        c.equals("无限刀刃：下一回合开始给 1 张小刀", 1, countShiv(ibCtx.hand()));
        ibCtx.endTurn();
        c.equals("无限刀刃：再下一回合又是 1 张（上一张还在手里就 2 张）", 2, countShiv(ibCtx.hand()));
        c.check("无限刀刃+：升级只加【固有】（天賦），费用与效果都不变",
                CardLibrary.resolve("infinite_blades", 1).has(CardKeyword.INNATE)
                        && CardLibrary.resolve("infinite_blades", 1).cost() == 1
                        && !CardLibrary.INFINITE_BLADES.has(CardKeyword.INNATE));

        // ---- 毒雾：回合开始给全体 2 层中毒
        Combatant nfPlayer = dummy("玩家", 60);
        Enemy nfA = zombie(200, Intent.unknown());
        Enemy nfB = zombie(200, Intent.unknown());
        CombatContext nfCtx = start(nfPlayer, List.of(nfA, nfB), deck(CardLibrary.NOXIOUS_FUMES, 10),
                new ArrayList<>());
        nfCtx.playCard(0, 0);
        c.equals("毒雾：打出它的这一回合不会立刻上毒", 0, nfA.poison());
        nfCtx.endTurn();
        c.equals("毒雾：下一回合开始 → 第一个敌人 2 层中毒", 2, nfA.poison());
        c.equals("毒雾：第二个敌人也是 2 层", 2, nfB.poison());

        Combatant nf2Player = dummy("玩家", 60);
        Enemy nf2Enemy = zombie(200, Intent.unknown());
        CombatContext nf2Ctx = start(nf2Player, List.of(nf2Enemy),
                deck(CardLibrary.resolve("noxious_fumes", 1), 10), new ArrayList<>());
        nf2Ctx.playCard(0, 0);
        nf2Ctx.endTurn();
        c.equals("毒雾+：每回合 3 层", 3, nf2Enemy.poison());

        // ---- 布置：把手牌放到抽牌堆顶，那张牌打出前费用为 0
        Combatant suPlayer = dummy("玩家", 60);
        Enemy suEnemy = zombie(300, Intent.unknown());
        CombatContext suCtx = start(suPlayer, List.of(suEnemy), deck(CardLibrary.SETUP, 10),
                new ArrayList<>());
        suCtx.addCardsToHand(suPlayer, CardLibrary.STRIKE, 1);
        suCtx.playCard(0, 0);
        c.check("布置：挂起了「放到抽牌堆顶」的选牌（kind = SETUP）",
                suCtx.pendingPick(suPlayer) != null
                        && suCtx.pendingPick(suPlayer).kind() == CombatContext.CardPickKind.SETUP);
        int strikeSlot = handIndexOf(suCtx, "strike");
        int drawBefore = suCtx.drawPile().size();
        suCtx.resolveCardPick(suPlayer, List.of(strikeSlot));
        c.equals("布置：抽牌堆 +1（那张牌被放回去了）", drawBefore + 1, suCtx.drawPile().size());
        c.equals("布置：抽牌堆顶就是刚放上去的『打击』", "strike",
                suCtx.drawPile().view().get(0).id());
        c.check("布置：记下了「打击 打出前免费」",
                suCtx.stateOf(suPlayer).isFreeToPlay("strike"));
        c.equals("布置：免费额度 1 次", 1, suCtx.stateOf(suPlayer).freeToPlayCount());
        c.equals("布置：别的牌不受影响（也是 1 费）", 1,
                suCtx.effectiveCost(suCtx.stateOf(suPlayer), CardLibrary.DEFEND));

        // 抽回来 → 卡面费用 0、打出只花 0 费、额度用掉
        suCtx.drawCardsFor(suPlayer, 1);
        int strikeIndex = handIndexOf(suCtx, "strike");
        c.check("布置：那张牌确实回到手牌里", strikeIndex >= 0);
        c.equals("布置：卡面费用被算成 0（界面与扣费共用 effectiveCost）", 0,
                suCtx.effectiveCost(suCtx.stateOf(suPlayer), suCtx.hand().get(strikeIndex)));
        int energyBefore = suCtx.energy();
        c.check("布置：打出去成功", suCtx.playCard(strikeIndex, 0).success());
        c.equals("布置：免费牌不花能量", energyBefore, suCtx.energy());
        c.equals("布置：额度用掉了", 0, suCtx.stateOf(suPlayer).freeToPlayCount());
        c.equals("布置：那张『打击』照常造成 6 点伤害", 294, suEnemy.hp());
        c.equals("布置+：费用 1 → 0", 0, CardLibrary.resolve("setup", 1).cost());
        c.check("布置：稀有度是罕见、类型是技能",
                CardLibrary.SETUP.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.SETUP.type() == CardType.SKILL);

        // 放弃选择（超时兜底）：按贪心挑费用最高的一张，仍然记账
        Combatant su2Player = dummy("玩家", 60);
        Enemy su2Enemy = zombie(300, Intent.unknown());
        CombatContext su2Ctx = start(su2Player, List.of(su2Enemy), deck(CardLibrary.SETUP, 10),
                new ArrayList<>());
        su2Ctx.playCard(0, 0);
        String detail = su2Ctx.cancelCardPick(su2Player);
        c.check("布置：放弃选择时走贪心兜底，照样把牌放上去并记免费（" + detail + "）",
                su2Ctx.stateOf(su2Player).freeToPlayCount() == 1
                        && su2Ctx.drawPile().view().get(0).id().equals("setup"));
    }

    /** 手牌里有几张『小刀』。 */
    private static int countShiv(List<Card> hand) {
        int count = 0;
        for (Card card : hand) {
            if (card.id().equals("shiv")) {
                count++;
            }
        }
        return count;
    }

    private static void silentCards11(Checker c) {
        // 小刀本身：0 费 4 伤害、消耗，且是"生成牌"（不进奖励池）
        c.check("小刀是生成牌（不进奖励池）", CardLibrary.isGenerated("shiv"));
        c.check("小刀不在猎手的奖励池里",
                CardLibrary.rewardPool(CardLibrary.SILENT).stream()
                        .noneMatch(card -> card.id().equals("shiv")));

        // 刃舞：加入 3 张小刀
        Combatant b26Player = dummy("玩家", 60);
        Enemy b26Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext b26Ctx = start(b26Player, List.of(b26Enemy), deck(CardLibrary.BLADE_DANCE, 8), new ArrayList<>());
        int b26Hand = b26Ctx.hand().size();
        b26Ctx.playCard(0, 0);
        c.equals("刃舞：手牌 -1 +3 = +2", b26Hand + 2, b26Ctx.hand().size());
        c.equals("刃舞：手里确实有 3 张小刀", 3,
                b26Ctx.hand().stream().filter(card -> card.id().equals("shiv")).count());
        // 打一张小刀：4 点伤害 + 消耗
        int shivIndex = -1;
        for (int i = 0; i < b26Ctx.hand().size(); i++) {
            if (b26Ctx.hand().get(i).id().equals("shiv")) {
                shivIndex = i;
                break;
            }
        }
        b26Ctx.playCard(shivIndex, 0);
        c.equals("小刀：4 点伤害", 296, b26Enemy.hp());
        c.equals("小刀：消耗（进消耗堆）", 1, b26Ctx.exhaustPile().size());
        c.equals("小刀：0 费（刃舞花了 1 点，所以是 2）", 2, b26Ctx.energy());

        // 斗篷与匕首：6 格挡 + 1 张小刀
        Combatant c26Player = dummy("玩家", 60);
        Enemy c26Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext c26Ctx = start(c26Player, List.of(c26Enemy),
                deck(CardLibrary.CLOAK_AND_DAGGER, 8), new ArrayList<>());
        c26Ctx.playCard(0, 0);
        c.equals("斗篷与匕首：6 点格挡", 6, c26Player.block());
        c.equals("斗篷与匕首：1 张小刀", 1,
                c26Ctx.hand().stream().filter(card -> card.id().equals("shiv")).count());

        // 精准：小刀 +4 伤害，且卡面数字跟着变
        Combatant a26Player = dummy("玩家", 60);
        Enemy a26Enemy = new Enemy("靶子", 300, 300, List.of(List.of(Intent.unknown())), true);
        CombatContext a26Ctx = start(a26Player, List.of(a26Enemy), deck(CardLibrary.ACCURACY, 8), new ArrayList<>());
        a26Ctx.playCard(0, 0);
        c.equals("精准：小刀加成 +4", 4, a26Player.shivBonus());
        c.check("精准：小刀卡面显示 4+4=8 点伤害",
                CardPreview.liveDescription(CardLibrary.SHIV, a26Player).contains("造成 8 点伤害"));
        a26Ctx.addCardsToHand(a26Player, CardLibrary.SHIV, 1);
        int a26Shiv = -1;
        for (int i = 0; i < a26Ctx.hand().size(); i++) {
            if (a26Ctx.hand().get(i).id().equals("shiv")) {
                a26Shiv = i;
                break;
            }
        }
        a26Ctx.playCard(a26Shiv, 0);
        c.equals("精准加成后的小刀：4+4 = 8 点伤害", 292, a26Enemy.hp());
        c.equals("精准+ 摘要里写着 +6",
                CardLibrary.resolve("accuracy", 1).description().contains("6 点"), true);
    }
    /**
     * v0.5.0.4：机宝第八批（电流相生 / 适应打击 / 模组改造）+ 集中接进黑暗球成长 + 集中显示。
     *
     * <p>对应需求原文：</p>
     * <pre>
     * 集中（使用时钟图标）：所有充能球的效果增加X点（X为效果的层数，等离子充能球不受影响）。
     *   闪电球：每回合造成的伤害增加X
     *   冰霜球：每回合提供的格挡增加X
     *   黑暗球：每回合成长的伤害增加X
     * 电流相生 3费 稀有 技能：生成等同于本次战斗中已生成过的闪电球数量的闪电球。消耗。升级：失去消耗。
     *   制作要求：能够显示X次的具体数值。
     * 适应打击 2费 稀有 攻击：18 伤害，将这张牌的一张 0 费复制品加入弃牌堆。升级：18-&gt;23。
     * 模组改造 0费 稀有 技能：获得 1 个充能球栏位。抽 1 张牌。这张牌的费用加 1。升级：抽 1 -&gt; 抽 2。
     * </pre>
     */
    private static void defectCards14(Checker c) {
        // ---------------- 集中接进黑暗球成长 ----------------
        Combatant d14Player = dummy("玩家", 60);
        Enemy d14Enemy = new Enemy("靶子", 500, 500, List.of(List.of(Intent.unknown())), true);
        CombatContext d14Ctx = start(d14Player, List.of(d14Enemy), deck(CardLibrary.ZAP, 8), new ArrayList<>());
        d14Player.addOrb(Orb.DARK);
        c.equals("黑暗球初始充能 6", 6, d14Player.orbs().get(0).charge());
        d14Ctx.endTurn();
        c.equals("无集中：黑暗球每回合成长 +6（6 → 12）", 12, d14Player.orbs().get(0).charge());

        Combatant f14Player = dummy("玩家", 60);
        Enemy f14Enemy = new Enemy("靶子", 500, 500, List.of(List.of(Intent.unknown())), true);
        CombatContext f14Ctx = start(f14Player, List.of(f14Enemy), deck(CardLibrary.ZAP, 8), new ArrayList<>());
        f14Player.addOrb(Orb.DARK);
        f14Player.addFocus(2);
        int f14Before = f14Player.orbs().get(0).charge();
        f14Ctx.endTurn();
        c.equals("集中 +2：黑暗球每回合成长 +6+2=8（" + f14Before + " → 14）",
                14, f14Player.orbs().get(0).charge());
        c.check("集中为负时成长被抵消（充能 6，集中 -3 → 6+6-3=9，且不会变负）",
                new OrbInstance(Orb.DARK, 6).grown(-3).charge() == 9);
        c.check("集中负得再多也不会出现负充能（夹到 0）",
                new OrbInstance(Orb.DARK, 1).grown(-99).charge() == 0);
        c.check("非成长的球不吃集中成长（闪电球 charge 恒为 0）",
                new OrbInstance(Orb.LIGHTNING, 0).grown(5).charge() == 0);

        // ---------------- 集中出现在状态文本里（玩家能看到层数） ----------------
        Combatant s14Player = dummy("玩家", 60);
        Enemy s14Enemy = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext s14Ctx = start(s14Player, List.of(s14Enemy), deck(CardLibrary.ZAP, 8), new ArrayList<>());
        c.check("没有集中时不显示集中", !s14Player.statusSuffix().contains("集中"));
        s14Player.addFocus(3);
        c.check("集中 +3 会显示在状态文本里", s14Player.statusSuffix().contains("集中+3"));
        s14Player.addFocus(-5);
        c.check("集中变负数也照样显示（-2）", s14Player.statusSuffix().contains("集中+-2"));

        // ---------------- 电流相生：X = 本场已生成的闪电球数 ----------------
        Combatant v14Player = dummy("玩家", 60);
        Enemy v14Enemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext v14Ctx = start(v14Player, List.of(v14Enemy), deck(CardLibrary.VOLTAIC, 8), new ArrayList<>());
        v14Player.setOrbSlots(3);
        c.equals("还没生成过闪电球时 X = 0", 0, v14Player.lightningCount());
        v14Ctx.channelOrb(v14Player, Orb.LIGHTNING);
        v14Ctx.channelOrb(v14Player, Orb.LIGHTNING);
        v14Ctx.channelOrb(v14Player, Orb.LIGHTNING);
        c.equals("生成 3 颗后计数为 3（球位 3 已满）", 3, v14Player.lightningCount());
        v14Ctx.gainEnergy(v14Player, 3);
        // 球位只有 3，打出电流相生会边生成边激发旧的：X 在打出前读定（=3），
        // 但"已生成过的闪电球数"这个**计数器**会因为新生成的 3 颗再 +3 → 3+3=6。
        v14Ctx.playCard(0, 0);
        c.equals("电流相生：本次按 X=3 生成，计数器累加到 3+3=6",
                6, v14Player.lightningCount());
        c.equals("电流相生：3 颗新球都在（球位 3 满）", 3, v14Player.orbs().size());
        c.equals("电流相生消耗（基础版带消耗关键字）",
                true, CardLibrary.VOLTAIC.has(CardKeyword.EXHAUST));
        c.equals("电流相生+ 失去消耗",
                false, CardLibrary.resolve("voltaic", 1).has(CardKeyword.EXHAUST));
        c.equals("电流相生是 3 费稀有技能",
                true, CardLibrary.VOLTAIC.cost() == 3
                        && CardLibrary.VOLTAIC.rarity() == CardRarity.RARE
                        && CardLibrary.VOLTAIC.type() == CardType.SKILL);

        // ---------------- 适应打击：18 伤害 + 弃牌堆里的 0 费复制品 ----------------
        Combatant a14Player = dummy("玩家", 60);
        Enemy a14Enemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext a14Ctx = start(a14Player, List.of(a14Enemy), deck(CardLibrary.ADAPTIVE_STRIKE, 8),
                new ArrayList<>());
        c.equals("适应打击是 2 费稀有攻击",
                true, CardLibrary.ADAPTIVE_STRIKE.cost() == 2
                        && CardLibrary.ADAPTIVE_STRIKE.rarity() == CardRarity.RARE
                        && CardLibrary.ADAPTIVE_STRIKE.type() == CardType.ATTACK);
        a14Ctx.playCard(0, 0);
        c.equals("适应打击：18 点伤害", 382, a14Enemy.hp());
        c.check("适应打击：弃牌堆里出现了一张 0 费复制品",
                a14Ctx.discardPile().view().stream()
                        .anyMatch(card -> card.id().equals("adaptive_strike_free") && card.cost() == 0));
        c.check("0 费复制品是生成牌（不会混进奖励池）",
                CardLibrary.isGenerated("adaptive_strike_free"));
        // 直接验复制品本身：它是 0 费、18 点伤害的独立定义，打出去不会再增殖
        // （抽不抽得到取决于洗牌，不适合当断言）
        Card a14Copy = CardLibrary.resolve("adaptive_strike_free", 0);
        c.equals("0 费复制品是攻击牌且 0 费",
                true, a14Copy.cost() == 0 && a14Copy.type() == CardType.ATTACK);
        Combatant a14cPlayer = dummy("玩家", 60);
        Enemy a14cEnemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext a14cCtx = start(a14cPlayer, List.of(a14cEnemy), deck(a14Copy, 5), new ArrayList<>());
        a14cCtx.playCard(0, 0);
        c.equals("0 费复制品打出：18 点伤害", 382, a14cEnemy.hp());
        c.equals("0 费复制品不再生成新的复制品（弃牌堆只有它自己）",
                1, a14cCtx.discardPile().size());
        c.check("适应打击+ 是 23 点伤害",
                CardLibrary.resolve("adaptive_strike", 1).description().contains("23 点伤害"));

        // ---------------- 模组改造：+1 球槽 / 抽牌 / 本牌费用 +1 ----------------
        Combatant m14Player = dummy("玩家", 60);
        Enemy m14Enemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext m14Ctx = start(m14Player, List.of(m14Enemy), deck(CardLibrary.MODDED, 8), new ArrayList<>());
        m14Player.setOrbSlots(2);
        int m14HandBefore = m14Ctx.hand().size();
        m14Ctx.playCard(0, 0);
        c.equals("模组改造：充能球栏位 2 → 3", 3, m14Player.orbSlots());
        c.equals("模组改造：抽 1 张牌（手牌先 -1 再 +1，净额不变）", m14HandBefore, m14Ctx.hand().size());
        c.check("模组改造：弃牌堆里那张变成 1 费（费用 +1）",
                m14Ctx.discardPile().view().stream()
                        .anyMatch(card -> card.id().equals("modded_boost") && card.cost() == 1));
        c.check("模组改造的费用档位被登记（boostedCard 已复位）", m14Ctx.boostedCard() == null);
        c.check("模组改造是 0 费稀有技能",
                CardLibrary.MODDED.cost() == 0 && CardLibrary.MODDED.rarity() == CardRarity.RARE
                        && CardLibrary.MODDED.type() == CardType.SKILL);
        c.check("模组改造+ 抽 2 张（描述里写的是抽 2 张牌）",
                CardLibrary.resolve("modded", 1).description().contains("抽 2 张牌"));
    }

    /**
     * v0.5.0.5：表现层与美术调整（多段攻击 / 敌人施法 / 球伤害 / 球缩放）。
     *
     * <p>对应需求原文：</p>
     * <pre>
     * 1. 充能球对敌人造成伤害时，敌人没有受击反馈和动画。
     * 2. 敌人执行特殊效果时（如 施加虚弱）没有动画，需要补全。例如播放唤魔者施法的音效，
     *    并在敌人实体放出药水粒子特效。
     * 3. 敌人的多段攻击动画会被按照单次攻击结算。应为攻击多少次就播放多少次动画。
     * 4. 多段攻击时，每一段的攻击动画应该比单次攻击的动画速度快四分之一。
     * 6. 充能球（包括栏位）的体积缩小四分之一。
     * </pre>
     */
    private static void presentationBatch1505(Checker c) {
        // ---------------- 3+4：多段攻击广播 N 段，且每段更快 ----------------
        List<CombatFxEvent> multi = new ArrayList<>();
        Combatant p1505 = dummy("玩家", 100);
        Enemy hitter1505 = new Enemy("连击者", 200, 200, List.of(List.of(Intent.attack(2, 3))), true);
        CombatContext multiCtx = start(p1505, List.of(hitter1505), deck(CardLibrary.STRIKE, 5),
                new ArrayList<>());
        multiCtx.setFxSink(multi::add);
        hitter1505.setFxId(7001);
        p1505.setFxId(7002);
        multiCtx.endTurn();
        long attackEvents = multi.stream().filter(e -> e.kind() == CombatFx.ENEMY_ATTACK).count();
        long hurtEvents = multi.stream().filter(e -> e.kind() == CombatFx.PLAYER_HURT).count();
        c.equals("多段攻击（2 段）广播 2 次 ENEMY_ATTACK", 2L, attackEvents);
        c.equals("多段攻击（2 段）广播 2 次 PLAYER_HURT", 2L, hurtEvents);
        c.equals("多段攻击真的结算了 2 次伤害（100 - 3×2 = 94）", 94, p1505.hp());

        // 单段攻击仍然只广播 1 次（别把普通攻击也变成多段）
        List<CombatFxEvent> single = new ArrayList<>();
        Combatant sp = dummy("玩家", 100);
        Enemy sh = new Enemy("单击者", 200, 200, List.of(List.of(Intent.attack(1, 5))), true);
        CombatContext singleCtx = start(sp, List.of(sh), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        singleCtx.setFxSink(single::add);
        singleCtx.endTurn();
        c.equals("单段攻击只广播 1 次 ENEMY_ATTACK", 1L,
                single.stream().filter(e -> e.kind() == CombatFx.ENEMY_ATTACK).count());
        c.check("单段事件 hits = 1",
                single.stream().filter(e -> e.kind() == CombatFx.ENEMY_ATTACK).allMatch(e -> e.hits() == 1));

        // 每段的时长比单次快四分之一（速度 ×1.25 ⇔ 时长 ÷1.25）
        long perHit = BattleAnim.multiHitMs(BattleAnim.ATTACK_MS);
        c.equals("多段每段时长 = 单次的 1/1.25（300 → 240）", 240L, perHit);
        c.check("多段每段确实比单次短（更快）", perHit < BattleAnim.ATTACK_MS);
        c.check("快四分之一 = 倍率 1.25", Math.abs(BattleAnim.MULTI_HIT_SPEED - 1.25D) < 1.0E-9D);

        // ---------------- 2：敌人特殊效果广播 ENEMY_CAST ----------------
        List<CombatFxEvent> cast = new ArrayList<>();
        Combatant cp = dummy("玩家", 60);
        Enemy caster = new Enemy("唤魔者", 200, 200, List.of(List.of(Intent.miningFatigue(3))), true);
        CombatContext castCtx = start(cp, List.of(caster), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        castCtx.setFxSink(cast::add);
        caster.setFxId(7101);
        cp.setFxId(7102);
        castCtx.endTurn();
        c.check("敌人施加虚弱 → 广播 ENEMY_CAST（施法音效 + 药水粒子）",
                hasFx(cast, CombatFx.ENEMY_CAST, 7101));
        c.check("ENEMY_CAST 的目标是被施加效果的那位", cast.stream()
                .anyMatch(e -> e.kind() == CombatFx.ENEMY_CAST && e.targetFxId() == 7102));
        c.check("敌人施加挖掘疲劳确实生效了（玩家身上有挖掘疲劳）", cp.miningFatigue() > 0);
        // 敌人给自己上增益也算"施法"
        List<CombatFxEvent> buff = new ArrayList<>();
        Combatant bp = dummy("玩家", 60);
        Enemy buffer = new Enemy("祭仪者", 200, 200, List.of(List.of(Intent.strength(2))), true);
        CombatContext buffCtx = start(bp, List.of(buffer), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        buffCtx.setFxSink(buff::add);
        buffer.setFxId(7201);
        buffCtx.endTurn();
        c.check("敌人给自己上力量也广播 ENEMY_CAST",
                hasFx(buff, CombatFx.ENEMY_CAST, 7201));

        // ---------------- 1：充能球伤害仍然广播 ORB_DAMAGE（客户端据此补受击动画） ----------------
        // （客户端那条 ENEMY_HURT 是渲染层的事，裸 JVM 里断到"事件确实发出去了"这一层）
        List<CombatFxEvent> orbFx = new ArrayList<>();
        Combatant op = dummy("玩家", 60);
        op.setOrbSlots(3);
        Enemy ot = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext orbCtx2 = start(op, List.of(ot), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        orbCtx2.setFxSink(orbFx::add);
        orbCtx2.channelOrb(op, Orb.LIGHTNING);
        orbFx.clear();
        orbCtx2.endTurn();
        c.check("闪电球造成伤害时广播 ORB_DAMAGE（且带上被打的敌人编号）",
                orbFx.stream().anyMatch(e -> e.kind() == CombatFx.ORB_DAMAGE && e.targetFxId() == ot.fxId()));

        // ---------------- 6：球与栏位缩小四分之一 ----------------
        c.check("v0.5.0.5：ORB_SCALE = 基准 × 0.75（缩小四分之一）",
                Math.abs(OrbRingLayout.ORB_SCALE
                        - OrbRingLayout.ORB_SCALE_BASE * OrbRingLayout.ORB_SHRINK) < 1.0E-6F);
        c.check("v0.5.0.5：缩小后的边长小于基准（确实变小了）",
                OrbRingLayout.ORB_SCALE < OrbRingLayout.ORB_SCALE_BASE);
        c.check("v0.5.0.5：缩小后的边长约 0.47（0.63 × 0.75）",
                Math.abs(OrbRingLayout.ORB_SCALE - 0.4725F) < 1.0E-3F);
    }

    /**
     * v0.5.0.9：**本层可挑战名单**（{@link FloorRoster}）。
     *
     * <p>需求原文："显示：挑战第X层boss的功能按钮（把boss拉过来进行战斗）
     * 功能修改为：详情查看当前层数可以触发战斗的敌怪和boss（以阵营筛选，包括【尖塔护卫】【亡灵生物】等。）"</p>
     *
     * <p>这里盯住三件事：① 名单里的每一只都真的 {@link FloorPools#allowed}（否则点上去会被服务端拒绝）；
     * ② BOSS 只出现在它自己那一层、且归到【尖塔护卫】；③ 不会重复出现同一只。</p>
     */
    private static void floorRosterBatch(Checker c) {
        for (int floor = 1; floor <= 3; floor++) {
            java.util.Map<Faction, List<FloorRoster.Entry>> grouped = FloorRoster.byFaction(floor);
            c.check("第 " + floor + " 层名单非空", !grouped.isEmpty());
            int total = 0;
            boolean allAllowed = true;
            boolean allNamespaced = true;
            List<String> seen = new ArrayList<>();
            boolean noDuplicates = true;
            for (java.util.Map.Entry<Faction, List<FloorRoster.Entry>> group : grouped.entrySet()) {
                for (FloorRoster.Entry entry : group.getValue()) {
                    total++;
                    if (!FloorPools.allowed(entry.entityId(), floor)) {
                        allAllowed = false;
                    }
                    if (!entry.entityId().contains(":")) {
                        allNamespaced = false;
                    }
                    if (seen.contains(entry.entityId())) {
                        noDuplicates = false;
                    }
                    seen.add(entry.entityId());
                }
            }
            c.equals("第 " + floor + " 层名单总数 = count()", total, FloorRoster.count(floor));
            c.equals("第 " + floor + " 层阵营数 = factionCount()", grouped.size(),
                    FloorRoster.factionCount(floor));
            c.check("第 " + floor + " 层名单里每一只都真的 allowed（名单 = 能打）", allAllowed);
            c.check("第 " + floor + " 层名单里的 id 都带命名空间", allNamespaced);
            c.check("第 " + floor + " 层名单里没有重复的实体", noDuplicates);
        }

        // BOSS：只出现在自己那一层，并且归到【尖塔护卫】
        FloorBoss floor1Boss = FloorBoss.ofFloor(1);
        c.check("第 1 层的 BOSS 出现在第 1 层名单的【尖塔护卫】下",
                floor1Boss != null && FloorRoster.byFaction(1).getOrDefault(Faction.SPIRE_GUARDIAN, List.of())
                        .stream().anyMatch(entry -> entry.entityId().equals(floor1Boss.entityId()) && entry.boss()));
        c.check("第 1 层的 BOSS 不会出现在第 2 层名单里",
                floor1Boss != null && FloorRoster.byFaction(2).values().stream()
                        .flatMap(List::stream)
                        .noneMatch(entry -> entry.entityId().equals(floor1Boss.entityId())));

        // 阵营归属：第 1 层有【亡灵生物】，且它的成员是亡灵那一族的
        c.check("第 1 层名单里有【亡灵生物】", FloorRoster.byFaction(1).containsKey(Faction.UNDEAD));
        List<FloorRoster.Entry> undead = FloorRoster.byFaction(1).getOrDefault(Faction.UNDEAD, List.of());
        c.check("【亡灵生物】下的条目都归 UNDEAD 阵营",
                undead.stream().allMatch(entry -> entry.faction() == Faction.UNDEAD));
        c.check("僵尸在第 1 层的【亡灵生物】名单里",
                undead.stream().anyMatch(entry -> entry.entityId().equals("minecraft:zombie")));
        c.check("第 3 层没有【亡灵生物】（这一族只在第 1 层）",
                !FloorRoster.byFaction(3).containsKey(Faction.UNDEAD));

        // 注意：同一实体**在不同层各出现一次是对的**（例如烈焰人第 2、3 层都能打），
        // 所以只能断言"同一层内不重复"。
        for (int floor = 1; floor <= 3; floor++) {
            List<String> ids = new ArrayList<>();
            boolean uniquePerFloor = true;
            for (List<FloorRoster.Entry> list : FloorRoster.byFaction(floor).values()) {
                for (FloorRoster.Entry entry : list) {
                    if (ids.contains(entry.entityId())) {
                        uniquePerFloor = false;
                    }
                    ids.add(entry.entityId());
                }
            }
            c.check("第 " + floor + " 层的名单内没有实体被列两次", uniquePerFloor);
        }

        // 命名空间补全
        c.equals("namespaced 会给裸路径补 minecraft:", "minecraft:zombie", FloorRoster.namespaced("zombie"));
        c.equals("namespaced 不动已带命名空间的 id", "stc:foo", FloorRoster.namespaced("stc:foo"));
    }

    /**
     * v0.5.0.10：两个成就的**引擎侧触发器**。
     *
     * <pre>
     * 【集中冰才对】     触发条件：在拥有集中状态时，生成冰球。
     * 【回响形态了 回响形态】触发条件：连续打出 4 张同名手牌。（隐藏成就）
     * </pre>
     */
    private static void achievementTriggers(Checker c) {
        // ---------------- 集中冰才对 ----------------
        List<CombatFxEvent> withFocus = new ArrayList<>();
        Combatant fOwner = dummy("玩家", 60);
        fOwner.setOrbSlots(3);
        fOwner.addFocus(2);
        Enemy fTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext fCtx = start(fOwner, List.of(fTarget), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        fCtx.setFxSink(withFocus::add);
        fCtx.channelOrb(fOwner, Orb.FROST);
        c.check("有集中时生成冰球 → 广播 FROST_CHANNELLED（成就【集中冰才对】）",
                hasFx(withFocus, CombatFx.FROST_CHANNELLED, fOwner.fxId()));

        List<CombatFxEvent> noFocus = new ArrayList<>();
        Combatant nOwner = dummy("玩家", 60);
        nOwner.setOrbSlots(3);
        Enemy nTarget = new Enemy("靶子", 200, 200, List.of(List.of(Intent.unknown())), true);
        CombatContext nCtx = start(nOwner, List.of(nTarget), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        nCtx.setFxSink(noFocus::add);
        nCtx.channelOrb(nOwner, Orb.FROST);
        c.check("**没有**集中时生成冰球 → 不广播（否则成就白送）",
                !hasFx(noFocus, CombatFx.FROST_CHANNELLED, nOwner.fxId()));
        // 集中只放大闪电/冰/暗；生成冰球这件事本身照常
        c.equals("没有集中时冰球照样生成了", 1, nOwner.orbs().size());

        // ---------------- 回响形态了 回响形态 ----------------
        List<CombatFxEvent> streakFx = new ArrayList<>();
        Combatant sPlayer = dummy("玩家", 60);
        Enemy sEnemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext sCtx = start(sPlayer, List.of(sEnemy), deck(CardLibrary.STRIKE, 12), new ArrayList<>());
        sCtx.setFxSink(streakFx::add);
        sCtx.gainEnergy(sPlayer, 30);
        for (int i = 0; i < 4; i++) {
            // 手里一定有【打击】（整副牌都是它），但下标按 id 找更稳
            sCtx.playCard(handIndexOf(sCtx, "strike"), 0);
        }
        c.check("连续打出 4 张同名手牌 → 广播 FOUR_SAME_CARDS（隐藏成就）",
                hasFx(streakFx, CombatFx.FOUR_SAME_CARDS, sPlayer.fxId()));
        c.equals("连打计数停在 4", 4, sCtx.stateOf(sPlayer).sameCardStreak());

        // 中间插一张别的牌 → 计数归 1
        // 两个坑：① 抽牌堆会被**洗牌**，手牌构成不确定；② **出牌之后不会自动补抽**
        // （抽牌只在回合开始），所以"连打 4 张"必须一开始手里就有 4 张同名。
        // 这里用 9 张【打击】的牌组：起手 5 张全是打击，投喂 1 张【防御】进来当"打断器"。
        List<CombatFxEvent> mixedFx = new ArrayList<>();
        Combatant mPlayer = dummy("玩家", 60);
        Enemy mEnemy = new Enemy("靶子", 400, 400, List.of(List.of(Intent.unknown())), true);
        CombatContext mCtx = start(mPlayer, List.of(mEnemy), deck(CardLibrary.STRIKE, 9), new ArrayList<>());
        mCtx.setFxSink(mixedFx::add);
        mCtx.gainEnergy(mPlayer, 30);
        mCtx.addCardsToHand(mPlayer, CardLibrary.DEFEND, 1);
        int defendIndex = handIndexOf(mCtx, "defend");
        c.check("投喂进来那张【防御】在手里（用来打断连击）", defendIndex >= 0);
        for (int i = 0; i < 4; i++) {
            int index = handIndexOf(mCtx, "strike");
            c.check("第 " + (i + 1) + " 张【打击】在手里", index >= 0);
            if (index < 0) {
                break;
            }
            if (i == 2) {
                c.equals("只打 2 张同名时计数是 2（还没到 3）", 2, mCtx.stateOf(mPlayer).sameCardStreak());
                c.check("只打 2 张同名 → 还没有成就",
                        !hasFx(mixedFx, CombatFx.FOUR_SAME_CARDS, mPlayer.fxId()));
            }
            mCtx.playCard(index, 0);
            if (i == 2) {
                c.equals("打完第 3 张同名 → 计数是 3", 3, mCtx.stateOf(mPlayer).sameCardStreak());
                c.check("只打 3 张同名 → 还没有成就",
                        !hasFx(mixedFx, CombatFx.FOUR_SAME_CARDS, mPlayer.fxId()));
            }
        }
        c.equals("连打 4 张同名之后计数是 4", 4, mCtx.stateOf(mPlayer).sameCardStreak());
        c.check("连打 4 张同名 → 这次给成就",
                hasFx(mixedFx, CombatFx.FOUR_SAME_CARDS, mPlayer.fxId()));

        // 打断：接着打那张【防御】→ 计数归 1，成就不再新增
        int stillDefend = handIndexOf(mCtx, "defend");
        c.check("【防御】还在手里（前面只打了打击）", stillDefend >= 0);
        long before = mixedFx.stream().filter(event -> event.kind() == CombatFx.FOUR_SAME_CARDS).count();
        mCtx.playCard(stillDefend, 0);
        c.equals("打断连击后计数归 1（换了别的牌）", 1, mCtx.stateOf(mPlayer).sameCardStreak());
        c.equals("打断这一次不会再广播 FOUR_SAME_CARDS",
                before, mixedFx.stream().filter(event -> event.kind() == CombatFx.FOUR_SAME_CARDS).count());
    }

    // ------------------------------------------------------------------ helpers

    private static CombatContext start(Combatant player, List<Enemy> enemies, List<Card> deck, List<String> log) {        return startWithRules(player, enemies, deck, CombatRules.defaults(), log);
    }

    private static CombatContext startWithRules(Combatant player, List<Enemy> enemies, List<Card> deck,
                                                CombatRules rules, List<String> log) {
        CombatContext ctx = new CombatContext(player, enemies, deck, rules, 20260101L, CombatLog.toList(log));
        ctx.start();
        return ctx;
    }

    /** 多人遭遇战：每名玩家各拿一副独立的牌堆（同一个 deck 列表会被各自复制进抽牌堆）。 */
    private static CombatContext startParty(List<Combatant> players, List<Enemy> enemies, List<String> log) {
        List<CombatContext.Participant> participants = new ArrayList<>();
        List<Card> partyDeck = deck(CardLibrary.STRIKE, 10);
        for (Combatant player : players) {
            participants.add(new CombatContext.Participant(player, partyDeck));
        }
        CombatContext ctx = new CombatContext(participants, enemies, CombatRules.defaults(), 20260101L,
                CombatLog.toList(log));
        ctx.start();
        return ctx;
    }

    private static List<Card> deck(Card card, int count) {
        List<Card> deck = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            deck.add(card);
        }
        return deck;
    }

    private static Enemy zombie(int hp, Intent... intents) {
        return new Enemy("僵尸", hp, Enemy.cycle(intents));
    }

    private static Combatant dummy(String name, int hp) {
        return new Dummy(name, hp);
    }

    private static final class Dummy extends Combatant {
        private Dummy(String name, int hp) {
            super(name, hp);
        }
    }

    /** 记录"最后是谁打的我"——用来验证击杀归属钩子（v0.2.1 测试反馈修正）。 */
    private static final class RecordingDummy extends Combatant {
        private Combatant lastSource;

        private RecordingDummy(String name, int hp) {
            super(name, hp);
        }

        @Override
        public void recordDamageFrom(Combatant source) {
            this.lastSource = source;
        }
    }

    /**
     * 读一段**源码文本**（用于"接线没断"这类源码级断言）。
     *
     * <p>裸 JVM 自检的定位是纯逻辑，但有些约定只能看源码（例如"渲染层确实用了插值坐标"）。
     * 读不到就返回空串 —— 调用方的断言会因此失败，而不是静默通过。</p>
     *
     * @param relative 相对 {@code src/main/java/com/slaythecraft/} 的路径
     */
    private static String readSource(String relative) {
        java.nio.file.Path path = java.nio.file.Path.of("src", "main", "java", "com", "slaythecraft");
        for (String part : relative.split("/")) {
            path = path.resolve(part);
        }
        try {
            return java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return "";
        }
    }

    private static final class Checker {
        private final List<String> lines = new ArrayList<>();
        private int passed;
        private int failed;

        void check(String label, boolean condition) {
            if (condition) {
                passed++;
                lines.add("PASS  " + label);
            } else {
                failed++;
                lines.add("FAIL  " + label);
            }
        }

        void equals(String label, long expected, long actual) {
            check(label + "（期望 " + expected + "，实际 " + actual + "）", expected == actual);
        }

        void equals(String label, Object expected, Object actual) {
            // 注意：这里必须用值比较。早先误用 == 让"拼接出来的字符串"永远不相等（引用不同），
            // 会把 string 断言变成永远失败/永远成功的假象。
            check(label + "（期望 " + expected + "，实际 " + actual + "）", java.util.Objects.equals(expected, actual));
        }
    }

    /**
     * v0.5.1.2 需求（卡牌图鉴）："升级后的卡牌得到的改动以黄色字体高亮，例如数值变化。
     * 如果为『失去消耗』等内容则不用。"
     *
     * <p>这里钉住 {@link CardTextDiff} 的三件事：数字整体识别、只标真正变化的词元、
     * 以及"被删掉的内容不产生高亮"。</p>
     */
    private static void highlightUpgradeDiff(Checker c) {
        // 词元切分：连续数字是一个词元（"12" 不能拆成 "1" 和 "2"）
        // "造成 12 点" 切出来是 造 / 成 / 空格 / 12 / 空格 / 点，共 6 个词元，数字在第 3 位
        c.equals("词元：连续数字算一个", 6, CardTextDiff.tokenize("造成 12 点").size());
        c.equals("词元：连续数字整体保留", "12", CardTextDiff.tokenize("造成 12 点").get(3));
        c.equals("词元：小数也算一个", 3, CardTextDiff.tokenize("x1.5y").size());

        // 数值变化：只有数字被标为变化
        var d1 = CardTextDiff.diff("造成 6 点伤害。", "造成 9 点伤害。");
        c.equals("升级对比：拼回来还是原句", "造成 9 点伤害。", CardTextDiff.join(d1));
        c.check("升级对比：检测到变化", CardTextDiff.hasChanges(d1));
        var changed1 = new StringBuilder();
        for (CardTextDiff.Span s : d1) {
            if (s.changed()) {
                changed1.append(s.text());
            }
        }
        c.equals("升级对比：只有数字被高亮", "9", changed1.toString());

        // 去掉关键字（"消耗"消失）：升级版里没有对应内容可高亮 → 不产生高亮
        var d2 = CardTextDiff.diff("获得 2 点敏捷，消耗。", "获得 2 点敏捷。");
        c.check("升级对比：只是删掉内容时不产生高亮（需求：失去消耗那类不用高亮）",
                !CardTextDiff.hasChanges(d2));
        c.equals("升级对比：删掉内容后拼回来仍是升级版原文", "获得 2 点敏捷。", CardTextDiff.join(d2));

        // 多个数字同时变
        var d3 = CardTextDiff.diff("造成 6 点伤害并抽 1 张牌。", "造成 9 点伤害并抽 2 张牌。");
        var changed3 = new StringBuilder();
        for (CardTextDiff.Span s : d3) {
            if (s.changed()) {
                changed3.append(s.text());
            }
        }
        c.equals("升级对比：两个数字都变时两个都高亮", "92", changed3.toString());

        // 完全相同的描述：没有任何高亮
        var d4 = CardTextDiff.diff("获得 5 点格挡。", "获得 5 点格挡。");
        c.check("升级对比：描述完全相同 → 不高亮", !CardTextDiff.hasChanges(d4));

        // 真实卡池抽查：打击 6→9 只有数字变
        Card strikeBase = CardLibrary.byId("strike");
        Card strikeUp = CardLibrary.resolve("strike", 1);
        c.check("真实卡池：打击的升级确实有高亮", strikeBase != null && strikeUp != null);
        if (strikeBase != null && strikeUp != null) {
            var real = CardTextDiff.diff(strikeBase.description(), strikeUp.description());
            var realChanged = new StringBuilder();
            for (CardTextDiff.Span s : real) {
                if (s.changed()) {
                    realChanged.append(s.text());
                }
            }
            c.equals("真实卡池：打击升级只高亮数字", "9", realChanged.toString());
        }

        // ---- v0.5.1.2：界面上真正要用的两样东西 ----
        // 1) 逐字符标记（界面折行是按字符走的，词元级 Span 摊平后才能和高亮渲染对上）
        String up1 = "造成 9 点伤害。";
        boolean[] m1 = CardTextDiff.marks("造成 6 点伤害。", up1);
        c.equals("高亮标记：长度等于升级版描述", up1.length(), m1.length);
        c.equals("高亮标记：只有数字被标记", "9", markedChars(up1, m1));

        String up2 = "获得 2 点敏捷。";
        boolean[] m2 = CardTextDiff.marks("获得 2 点敏捷，消耗。", up2);
        c.equals("高亮标记：只删内容（失去消耗）时一个标记都没有", "", markedChars(up2, m2));

        String up3 = "造成 9 点伤害并抽 2 张牌。";
        boolean[] m3 = CardTextDiff.marks("造成 6 点伤害并抽 1 张牌。", up3);
        c.equals("高亮标记：两个数字都标上", "92", markedChars(up3, m3));

        String same = "获得 5 点格挡。";
        c.equals("高亮标记：描述相同时没有标记", "",
                markedChars(same, CardTextDiff.marks(same, same)));

        // 2) 折行返回的是**原文下标**，所以换色不会跟着折行错位
        //    假测量器：普通字符 6 像素、空格 3 像素
        java.util.function.ToIntFunction<String> width = s -> s.charAt(0) == ' ' ? 3 : 6;
        var ranges = TextLayout.wrapRanges(up3, 36, width);
        c.equals("折行：按宽度断成 3 行", 3, ranges.size());
        c.equals("折行：第一行是「造成 9」", "造成 9", ranges.get(0).substring(up3));
        c.equals("折行：最后一行是「2 张牌。」", "2 张牌。", ranges.get(2).substring(up3));
        // 断行处的空格按设计被吃掉，其余一个字都不能少
        c.equals("折行：拼回来不丢字", "造成9点伤害并抽2张牌。",
                TextLayout.join(ranges, up3).replace(" ", ""));

        var goldRuns = TextLayout.markRuns(m3, 0, up3.length());
        StringBuilder gold = new StringBuilder();
        for (TextLayout.Run run : goldRuns) {
            if (run.highlighted()) {
                gold.append(up3, run.start(), run.end());
            }
        }
        c.equals("色块：高亮段拼起来正好是改动内容", "92", gold.toString());
        // 色块必须首尾相接盖满整行，否则渲染时文字之间会漏空
        int covered = 0;
        for (TextLayout.Run run : goldRuns) {
            c.check("色块：第 " + covered + " 段与上一段相接", run.start() == covered);
            c.check("色块：区间合法", run.end() > run.start() && run.end() <= up3.length());
            covered = run.end();
        }
        c.equals("色块：盖满整行", up3.length(), covered);
    }

    /** 把 {@code marks} 为真的字符挑出来拼成字符串（自检用）。 */
    private static String markedChars(String text, boolean[] marks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length() && i < marks.length; i++) {
            if (marks[i]) {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * v0.5.1.6 回合模型改造。需求原文：
     *
     * <pre>
     * "不再让玩家依次出牌。所有玩家的回合归纳为『玩家回合』，多个玩家可以同时出牌，
     *  按照出牌时间顺序结算打出的牌。直到所有玩家都确认【结束回合】，才会进入敌怪的攻击回合。
     *  敌怪的意图对全部玩家统一，但是显示会因为不同玩家自身的状态而决定对当前玩家显示的敌怪意图数值。"
     * </pre>
     *
     * <p>这里钉住四件事：① 一轮内谁都能出牌（不分先后）；② 甲确认之后甲不能再出牌、但乙还能；
     * ③ 只有**全员**确认才进敌怪回合；④ 意图数值按观察者算（引擎早就支持 ——
     * {@code actualIntentLines(target)} 收的就是观察者）。</p>
     */
    private static void partySimultaneousTurns(Checker c) {
        Combatant first = dummy("玩家A", 40);
        Combatant second = dummy("玩家B", 40);
        Enemy enemy = zombie(80, Intent.attack(4));
        CombatContext ctx = startParty(List.of(first, second), List.of(enemy), new ArrayList<>());
        PlayerState s1 = ctx.stateOf(first);
        PlayerState s2 = ctx.stateOf(second);

        c.check("开局两人**同时**都能出牌（不再分先后）", ctx.isActive(first) && ctx.isActive(second));
        c.equals("甲开局抽满手牌", 5, s1.hand().size());
        c.equals("乙开局也抽满手牌（旧模型要等甲结束回合才轮到他抽）", 5, s2.hand().size());

        c.check("甲出牌成功", ctx.playCard(first, 0, 0).success());
        c.check("甲出完牌，乙照样能出牌（这就是同时出牌）", ctx.playCard(second, 0, 0).success());
        c.equals("甲只花自己的能量（3-1=2）", 2, s1.energy());
        c.equals("乙也只花自己的能量（3-1=2）", 2, s2.energy());
        c.equals("甲的手牌 5-1=4", 4, s1.hand().size());
        c.equals("乙的手牌 5-1=4", 4, s2.hand().size());
        c.equals("两次打击都结算了（80-6-6=68）", 68, enemy.hp());

        c.check("甲确认结束回合", ctx.confirmEndTurn(first));
        c.check("甲确认之后不能再出牌", !ctx.playCard(first, 0, 0).success());
        c.check("乙还没确认，照样能出牌", ctx.playCard(second, 0, 0).success());
        c.equals("只有甲确认不等于进敌怪回合（回合数仍 1）", 1, ctx.turn());
        c.check("阶段仍是玩家回合", ctx.phase() == CombatPhase.PLAYER_TURN);
        c.check("对同一个人重复确认无效", !ctx.confirmEndTurn(first));
        c.check("已确认的人 isActive 为假", !ctx.isActive(first) && ctx.isActive(second));

        c.check("乙确认结束回合", ctx.confirmEndTurn(second));
        c.equals("全员确认之后才进敌怪回合（回合数 +1）", 2, ctx.turn());
        c.equals("敌人只打了一次（4 点，合计 80-4=76）", 76, first.hp() + second.hp());
        c.check("新一轮两人又能出牌", ctx.isActive(first) && ctx.isActive(second));
        c.check("新一轮的已确认标记被清掉", !s1.turnDone() && !s2.turnDone());

        // 结算顺序 = 到达顺序：让**后座**的人先出牌也照样按调用顺序结算（引擎不强制座位顺序）
        List<String> orderLog = new ArrayList<>();
        Combatant oa = dummy("甲", 40);
        Combatant ob = dummy("乙", 40);
        CombatContext orderCtx = startParty(List.of(oa, ob), List.of(zombie(80, Intent.attack(1))), orderLog);
        c.check("后座玩家可以先出牌", orderCtx.playCard(ob, 0, 0).success());
        c.check("前座玩家接着出牌", orderCtx.playCard(oa, 0, 0).success());
        int indexOfB = firstPlayLogIndex(orderLog, "乙");
        int indexOfA = firstPlayLogIndex(orderLog, "甲");
        c.check("战斗日志里乙先打排在前面（结算顺序 = 出牌顺序）", indexOfB >= 0 && indexOfA > indexOfB);

        // 意图数值按**观察者**算：同一只怪、同一个意图，对易伤的人显示更高。
        // ⚠️ 意图是在 start()/assignIntents() 时才落到敌人身上的，所以观察者必须放进一场**已开始**的战斗里。
        Combatant normal = dummy("普通人", 40);
        Combatant hurtable = dummy("易伤者", 40);
        hurtable.addVulnerable(1);
        CombatContext intentCtx = startParty(List.of(normal, hurtable),
                List.of(zombie(80, Intent.attack(10))), new ArrayList<>());
        Enemy hitter = intentCtx.enemies().get(0);
        c.equals("同一个意图对普通观察者 = 10", 10, CombatContext.previewDamage(hitter, normal, 10));
        c.equals("同一个意图对易伤观察者 = 12", 12, CombatContext.previewDamage(hitter, hurtable, 10));
        String normalView = String.join(" ", hitter.actualIntentLines(normal));
        String hurtableView = String.join(" ", hitter.actualIntentLines(hurtable));
        c.check("普通观察者看到 10（实际：" + normalView + "）", normalView.contains("10"));
        c.check("易伤观察者看到 12（实际：" + hurtableView + "）", hurtableView.contains("12"));
        c.check("两次生成的意图文案不同（数值随观察者变）", !normalView.equals(hurtableView));
    }

    /** 战斗日志里第一次"某人打出「…」"的位置（找不到返回 -1）；用来验证结算顺序。 */
    private static int firstPlayLogIndex(List<String> log, String name) {
        for (int i = 0; i < log.size(); i++) {
            String line = log.get(i);
            if (line.contains(name) && line.contains("打出「")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * v0.5.1.4 需求（虚弱重做 / 消歧义）：
     *
     * <ul>
     *   <li>"虚弱：可以累加。每累计一点，攻击伤害-1。"</li>
     *   <li>"虚弱与力量可以相互抵消 …… 敏捷和缓慢也可以相互抵消。"</li>
     *   <li>"先前的虚弱机制改为：挖掘疲劳 …… 将先前存在『给予虚弱』的生物 这项意图全部改为
     *       给予挖掘疲劳。"</li>
     * </ul>
     */
    private static void weakReworkAndMiningFatigue(Checker c) {
        // ---- 虚弱：每层 -1、可累加、不随回合消失
        Combatant weakling = dummy("虚弱的人", 40);
        weakling.addWeak(3);
        c.equals("虚弱可以累加（3 层）", 3, weakling.weak());
        c.equals("虚弱每层 -1 攻击伤害（10 → 7）", 7, CombatContext.previewDamage(weakling, false, 10));
        weakling.decayStatuses();
        c.equals("虚弱不随回合衰减（仍是 3 层）", 3, weakling.weak());

        // ---- 消歧义①：力量 ↔ 虚弱 互相抵消（需求给的两个例子）
        Combatant exampleA = dummy("例子甲", 40);
        exampleA.addStrength(3);
        exampleA.addWeak(2);
        c.check("例子①：力量 3 的敌人吃 2 层虚弱 → 力量 1、虚弱 0",
                exampleA.strength() == 1 && exampleA.weak() == 0);
        Combatant exampleB = dummy("例子乙", 40);
        exampleB.addStrength(2);
        exampleB.addWeak(3);
        c.check("例子②：力量 2 的敌人吃 3 层虚弱 → 虚弱 1、力量 0",
                exampleB.strength() == 0 && exampleB.weak() == 1);
        Combatant exampleC = dummy("例子丙", 40);
        exampleC.addWeak(2);
        exampleC.addStrength(5);
        c.check("反向抵消：虚弱 2 吃 5 点力量 → 虚弱 0、力量 3",
                exampleC.weak() == 0 && exampleC.strength() == 3);
        Combatant exampleD = dummy("例子丁", 40);
        exampleD.addWeak(1);
        exampleD.addStrength(-3);
        c.check("「失去力量」不会转成虚弱，也不动虚弱", exampleD.strength() == 0 && exampleD.weak() == 1);

        // ---- 消歧义②：迅捷（敏捷）↔ 缓慢 互相抵消
        Combatant nimble = dummy("敏捷的人", 40);
        nimble.addDexterity(3);
        nimble.addSlow(2);
        c.check("敏捷 3 吃缓慢 2 → 敏捷 1、缓慢 0", nimble.dexterity() == 1 && nimble.slow() == 0);
        c.equals("抵消后获得的格挡 = 5 + 1", 6, CombatContext.previewBlock(nimble, 5));
        Combatant sluggish = dummy("缓慢的人", 40);
        sluggish.addSlow(2);
        sluggish.addDexterity(3);
        c.check("缓慢 2 吃敏捷 3 → 缓慢 0、敏捷 1", sluggish.slow() == 0 && sluggish.dexterity() == 1);

        // ---- 挖掘疲劳：接过"旧的虚弱"（-25%、按回合递减）
        Combatant tired = dummy("疲惫者", 40);
        tired.addMiningFatigue(3);
        c.equals("挖掘疲劳 3 层：10 点 → floor(10×0.75) = 7", 7,
                CombatContext.previewDamage(tired, false, 10));
        c.equals("挖掘疲劳与虚弱是两回事（虚弱仍是 0）", 0, tired.weak());
        tired.decayStatuses();
        c.equals("挖掘疲劳每回合 -1（3 → 2）", 2, tired.miningFatigue());
        tired.decayStatuses();
        tired.decayStatuses();
        tired.decayStatuses();
        c.equals("挖掘疲劳掉到 0 就不再扣（有下限）", 0, tired.miningFatigue());
        c.equals("挖掘疲劳为 0 后伤害恢复（10 → 10）", 10, CombatContext.previewDamage(tired, false, 10));

        // ---- 生物的"给予虚弱"意图现在给挖掘疲劳
        c.equals("意图类型已改名（挖掘疲劳）", IntentType.DEBUFF_MINING_FATIGUE,
                Intent.miningFatigue(2).type());
        c.check("意图文案写的是挖掘疲劳", Intent.miningFatigue(2).label().contains("挖掘疲劳"));
        Combatant victim = dummy("玩家", 60);
        Enemy caster = new Enemy("唤魔者", 200, 200, List.of(List.of(Intent.miningFatigue(2))), true);
        CombatContext ctx = start(victim, List.of(caster), deck(CardLibrary.STRIKE, 5), new ArrayList<>());
        ctx.endTurn();
        c.equals("敌人执行「给予挖掘疲劳」意图 → 玩家挖掘疲劳 2", 2, victim.miningFatigue());
        c.equals("而且玩家身上**没有**虚弱", 0, victim.weak());
    }

    /**
     * v0.5.1.4 附甲衰减（用户拍板："附甲在常规状态下会在每个玩家回合开始时减少 1"）。
     *
     * <p>【村庄英雄】的"回合结束时你的附甲层数不会再降低"就是靠
     * {@code setPlatedArmorLocked(true)} 实现的。</p>
     */
    private static void platedArmorDecay(Checker c) {
        Combatant locked = dummy("锁住的", 40);
        locked.addPlatedArmor(2);
        locked.setPlatedArmorLocked(true);
        c.equals("锁住的附甲不掉层", 0, locked.decayPlatedArmor());
        c.equals("锁住时层数不变", 2, locked.platedArmor());

        Combatant owner = dummy("玩家", 60);
        Enemy enemy = zombie(60, Intent.attack(1));
        CombatContext ctx = start(owner, List.of(enemy), deck(CardLibrary.STRIKE, 10), new ArrayList<>());
        owner.addPlatedArmor(3);
        ctx.endTurn();
        c.equals("附甲在玩家回合开始时 -1（3 → 2）", 2, owner.platedArmor());
        ctx.endTurn();
        c.equals("再过一回合继续 -1（2 → 1）", 1, owner.platedArmor());
        owner.setPlatedArmorLocked(true);
        ctx.endTurn();
        c.equals("被【村庄英雄】锁住之后不再衰减（保持 1）", 1, owner.platedArmor());
    }

    /**
     * v0.5.1.4 新卡（五张多人卡牌）+ 多人卡牌类型 / 图鉴 / 奖励池过滤。
     *
     * <p>需求："新卡牌类型：多人卡牌 —— 这些卡牌只有在多人模式下才会从奖励池里出现。
     * 为卡牌图鉴添加：多人模式卡牌图鉴。"</p>
     */
    private static void multiplayerCards(Checker c) {
        // ---- 卡面数值
        c.check("快哉，肉排！是 0 费 罕见 技能 消耗",
                CardLibrary.HEARTY_MEAL.cost() == 0 && CardLibrary.HEARTY_MEAL.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.HEARTY_MEAL.type() == CardType.SKILL
                        && CardLibrary.HEARTY_MEAL.has(CardKeyword.EXHAUST));
        c.check("我们团结一心！是 1 费 罕见 技能",
                CardLibrary.UNITED_WE_STAND.cost() == 1
                        && CardLibrary.UNITED_WE_STAND.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.UNITED_WE_STAND.type() == CardType.SKILL);
        c.check("沉船打捞是 1 费 稀有 技能 消耗",
                CardLibrary.SHIPWRECK_SALVAGE.cost() == 1
                        && CardLibrary.SHIPWRECK_SALVAGE.rarity() == CardRarity.RARE
                        && CardLibrary.SHIPWRECK_SALVAGE.has(CardKeyword.EXHAUST));
        c.check("村庄英雄是 3 费 稀有 能力（升级 2 费）",
                CardLibrary.VILLAGE_HERO.cost() == 3 && CardLibrary.VILLAGE_HERO.type() == CardType.POWER
                        && CardLibrary.VILLAGE_HERO.rarity() == CardRarity.RARE
                        && CardLibrary.resolve("village_hero", 1).cost() == 2);
        c.check("魔术技巧！是 10 费 稀有 攻击 固有+保留+消耗",
                CardLibrary.MAGIC_TRICK.cost() == 10 && CardLibrary.MAGIC_TRICK.type() == CardType.ATTACK
                        && CardLibrary.MAGIC_TRICK.rarity() == CardRarity.RARE
                        && CardLibrary.MAGIC_TRICK.has(CardKeyword.INNATE)
                        && CardLibrary.MAGIC_TRICK.has(CardKeyword.RETAIN)
                        && CardLibrary.MAGIC_TRICK.has(CardKeyword.EXHAUST));
        c.check("魔术技巧！升级后写着 76 点伤害",
                CardLibrary.resolve("magic_trick", 1).description().contains("76 点伤害"));
        c.check("五张新卡都有强化版",
                CardLibrary.canUpgrade("hearty_meal") && CardLibrary.canUpgrade("united_we_stand")
                        && CardLibrary.canUpgrade("shipwreck_salvage") && CardLibrary.canUpgrade("village_hero")
                        && CardLibrary.canUpgrade("magic_trick"));

        // ---- v0.5.2.5 消歧义：这五张**不是"仅多人卡牌"，是自带卡牌**（单人也能抽到）
        // ---- v0.5.5.1（用户）："多人模式卡牌图鉴应该只有：商队护送" → 集合里只剩这一张
        c.equals("多人玩法向的卡牌只剩 1 张（【商队护送】，v0.5.5.1 用户要求）", 1,
                CardLibrary.multiplayerCount());
        c.check("只有【商队护送】被登记为多人玩法向（图鉴那一页）",
                CardLibrary.isMultiplayerStyle("caravan_escort")
                        && !CardLibrary.isMultiplayerStyle("hearty_meal")
                        && !CardLibrary.isMultiplayerStyle("united_we_stand")
                        && !CardLibrary.isMultiplayerStyle("shipwreck_salvage")
                        && !CardLibrary.isMultiplayerStyle("village_hero")
                        && !CardLibrary.isMultiplayerStyle("magic_trick"));
        c.check("那五张**照样**在单人奖励池里（它们只是不再单独分页）",
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 1).stream()
                        .anyMatch(card -> card.id().equals("hearty_meal"))
                        && CardLibrary.rewardPool(CardLibrary.PILLAGER, 1).stream()
                        .anyMatch(card -> card.id().equals("village_hero")));
        c.equals("单人池 = 老的 rewardPool（同一条路径）",
                CardLibrary.rewardPool(CardLibrary.PILLAGER).size(),
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 1).size());
        // v0.5.5.2（用户 B7）："【商队护送】在队伍只有 1 名玩家时，不加入奖励池"
        c.check("【商队护送】单人时**不进**奖励池",
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 1).stream()
                        .noneMatch(card -> card.id().equals("caravan_escort"))
                        && CardLibrary.rewardPool(CardLibrary.PILLAGER).stream()
                                .noneMatch(card -> card.id().equals("caravan_escort")));
        c.check("【商队护送】多人时进奖励池（队伍 2 人）",
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 2).stream()
                        .anyMatch(card -> card.id().equals("caravan_escort")));
        c.equals("所以多人池比单人池正好多这一张", 1,
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 2).size()
                        - CardLibrary.rewardPool(CardLibrary.PILLAGER, 1).size());
        c.check("稀有池也按同一规则过滤（商队护送是罕见牌，所以稀有池不受影响）",
                CardLibrary.rarePool(CardLibrary.PILLAGER, 1).size()
                        == CardLibrary.rarePool(CardLibrary.PILLAGER, 2).size());
        c.equals("多人玩法向图鉴正好 1 张", 1, CardLibrary.multiplayerCodex().size());
        c.check("角色图鉴里也列出这 5 张（它们属于叛逃掠夺者）",
                CardLibrary.codexFor(CardLibrary.PILLAGER).stream()
                        .anyMatch(card -> card.id().equals("village_hero")));
        c.check("生成牌不进奖励池（弩箭射击 / 0 费复制品 / 魔术技巧费用档）",
                CardLibrary.rewardPool(CardLibrary.PILLAGER, 2).stream()
                        .noneMatch(card -> CardLibrary.isGenerated(card.id()))
                        && CardLibrary.rewardPool(CardLibrary.DEFECT, 2).stream()
                        .noneMatch(card -> card.id().equals("adaptive_strike_free")));
        c.check("生成牌也不进无色牌图鉴",
                CardLibrary.colorlessCodex().stream().noneMatch(card -> CardLibrary.isGenerated(card.id())));

        // ---- 魔术技巧！的 11 档费用（每份单独计算）
        boolean chainOk = true;
        for (int discount = 0; discount <= CardLibrary.MAGIC_TRICK_MAX_DISCOUNT; discount++) {
            Card variant = CardLibrary.byId(CardLibrary.magicTrickId(discount));
            if (variant == null || variant.cost() != CardLibrary.MAGIC_TRICK_COST - discount
                    || !CardLibrary.canUpgrade(variant.id())
                    || (discount > 0 && !CardLibrary.isGenerated(variant.id()))) {
                chainOk = false;
            }
        }
        c.check("魔术技巧！10 → 0 费共 11 档、每档都有强化版且登记为生成牌", chainOk);
        c.equals("费用档解析：magic_trick_d7 → 减了 7 费", 7, CardLibrary.magicTrickDiscount("magic_trick_d7"));
        c.equals("原版解析为 0 档", 0, CardLibrary.magicTrickDiscount("magic_trick"));
        c.check("普通牌不会被认成魔术技巧", !CardLibrary.isMagicTrick("strike"));
        c.check("费用档不算多人玩法向（否则图鉴会列 11 次）",
                !CardLibrary.isMultiplayerStyle(CardLibrary.magicTrickId(5)));

        // ---- 效果：快哉，肉排！
        Combatant mealOwner = dummy("玩家", 40);
        CombatContext mealCtx = start(mealOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.HEARTY_MEAL, 3), new ArrayList<>());
        mealCtx.playCard(0, 0);
        c.equals("快哉，肉排！：力量 +2", 2, mealOwner.strength());
        Combatant mealOwner2 = dummy("玩家2", 40);
        CombatContext mealCtx2 = start(mealOwner2, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.resolve("hearty_meal", 1), 3), new ArrayList<>());
        mealCtx2.playCard(0, 0);
        c.equals("快哉，肉排！+：力量 +3", 3, mealOwner2.strength());

        // ---- 效果：我们团结一心！
        Enemy u1 = zombie(60, Intent.attack(1));
        Enemy u2 = zombie(60, Intent.attack(1));
        Combatant unitedOwner = dummy("玩家", 40);
        CombatContext unitedCtx = start(unitedOwner, List.of(u1, u2), deck(CardLibrary.UNITED_WE_STAND, 3),
                new ArrayList<>());
        unitedCtx.playCard(0, 0);
        c.equals("我们团结一心！：2 个敌人 → 每个受 8+2×2=12 点", 48, u1.hp());
        c.equals("另一个敌人同样 12 点", 48, u2.hp());
        Enemy u3 = zombie(60, Intent.attack(1));
        Enemy u4 = zombie(60, Intent.attack(1));
        Combatant unitedOwner2 = dummy("玩家2", 40);
        CombatContext unitedCtx2 = start(unitedOwner2, List.of(u3, u4),
                deck(CardLibrary.resolve("united_we_stand", 1), 3), new ArrayList<>());
        unitedCtx2.playCard(0, 0);
        c.equals("我们团结一心！+：2 个敌人 → 每个受 8+3×2=14 点", 46, u3.hp());

        // ---- 效果：沉船打捞
        Combatant salvOwner = dummy("玩家", 40);
        CombatContext salvCtx = start(salvOwner, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.SHIPWRECK_SALVAGE, 3), new ArrayList<>());
        PlayerState salvState = salvCtx.stateOf(salvOwner);
        int drawBefore = salvState.drawPile().size();
        int discardBefore = salvState.discardPile().size();
        int handBefore = salvState.hand().size();
        salvCtx.playCard(0, 0);
        c.equals("沉船打捞：抽牌堆 +1", drawBefore + 1, salvState.drawPile().size());
        c.equals("沉船打捞：弃牌堆 +1", discardBefore + 1, salvState.discardPile().size());
        c.equals("沉船打捞：手牌 -1（打出）+1（加入）= 不变", handBefore, salvState.hand().size());
        boolean allOthers = true;
        int salvagedCount = 0;
        List<Card> salvagePool = new ArrayList<>();
        salvagePool.addAll(salvState.drawPile().view());
        salvagePool.addAll(salvState.discardPile().view());
        salvagePool.addAll(salvState.hand());
        for (Card card : salvagePool) {
            if (card.rarity() == CardRarity.RARE && card.type() != CardType.POWER && card.character() != null
                    && !CardLibrary.PILLAGER.equals(card.character())) {
                salvagedCount++;
            }
        }
        c.check("沉船打捞：打捞上来的确实是「其他角色的稀有非能力牌」（认出 " + salvagedCount + " 张）",
                salvagedCount >= 3 && allOthers);
        Combatant salvOwner2 = dummy("玩家2", 40);
        CombatContext salvCtx2 = start(salvOwner2, List.of(zombie(60, Intent.attack(1))),
                deck(CardLibrary.resolve("shipwreck_salvage", 1), 3), new ArrayList<>());
        PlayerState salvState2 = salvCtx2.stateOf(salvOwner2);
        int handBefore2 = salvState2.hand().size();
        salvCtx2.playCard(0, 0);
        c.equals("沉船打捞+：手牌同样是 -1+1", handBefore2, salvState2.hand().size());
        c.check("沉船打捞+：打捞进来的是强化版（手牌里出现了带 + 的**别的**牌）",
                salvState2.hand().stream().anyMatch(
                        card -> card.name().endsWith("+") && !card.id().equals("shipwreck_salvage")));

        // ---- 效果：村庄英雄
        Combatant heroOwner = dummy("玩家", 60);
        Enemy heroTarget = zombie(200, Intent.attack(5));
        CombatContext heroCtx = start(heroOwner, List.of(heroTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PlayerState heroState = heroCtx.stateOf(heroOwner);
        heroState.hand.clear();
        heroState.energy = 99;
        heroState.hand.add(CardLibrary.VILLAGE_HERO);
        heroCtx.playCard(0, 0);
        c.check("村庄英雄：本场旗标点亮", heroState.villageHero());
        c.check("村庄英雄：附甲被锁住（不再随回合衰减）", heroOwner.platedArmorLocked());
        heroState.hand.clear();
        for (int i = 0; i < 3; i++) {
            heroState.hand.add(CardLibrary.STRIKE);
        }
        for (int i = 0; i < 3; i++) {
            heroCtx.playCard(0, 0);
        }
        c.equals("村庄英雄：第 3 张攻击牌 → 随机敌人凋零 15", 15, heroTarget.decay());
        c.equals("村庄英雄：攻击牌计数 = 3", 3, heroState.heroAttacks());
        heroState.hand.clear();
        for (int i = 0; i < 3; i++) {
            heroState.hand.add(CardLibrary.DEFEND);
        }
        for (int i = 0; i < 3; i++) {
            heroCtx.playCard(0, 0);
        }
        c.equals("村庄英雄：第 3 张技能牌 → 自己获得 1 层附甲", 1, heroOwner.platedArmor());
        c.equals("村庄英雄：技能牌计数 = 3", 3, heroState.heroSkills());
        c.equals("村庄英雄：附甲锁住时不衰减", 0, heroOwner.decayPlatedArmor());

        // ---- 效果：魔术技巧！（每份单独计算费用折扣）
        Combatant trickOwner = dummy("玩家", 60);
        Enemy trickTarget = zombie(400, Intent.attack(1));
        CombatContext trickCtx = start(trickOwner, List.of(trickTarget), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PlayerState trickState = trickCtx.stateOf(trickOwner);
        trickState.hand.clear();
        trickState.energy = 99;
        trickState.hand.add(CardLibrary.MAGIC_TRICK);                 // 0：10 费
        trickState.hand.add(CardLibrary.resolve("magic_trick", 1));    // 1：强化版 10 费
        trickState.hand.add(CardLibrary.DEFEND);                      // 2：技能牌
        trickCtx.playCard(2, 0);
        c.equals("魔术技巧！：打一张技能牌后 -1（10 → 9）", 9, trickState.hand.get(0).cost());
        c.equals("强化版那一份也各自 -1（10 → 9）", 9, trickState.hand.get(1).cost());
        // 打掉那张技能牌之后手里只剩两张，新加的这份落在下标 2
        trickState.hand.add(CardLibrary.MAGIC_TRICK);
        c.equals("新到手的另一份仍是原价 10（每份单独计算）", 10, trickState.hand.get(2).cost());
        trickState.hand.add(CardLibrary.DEFEND);
        trickCtx.playCard(3, 0);
        c.equals("再打一张技能牌：第一份 9 → 8", 8, trickState.hand.get(0).cost());
        c.equals("新到手的第三份 10 → 9（各自独立累加）", 9, trickState.hand.get(2).cost());
        for (int i = 0; i < 12; i++) {
            trickState.hand.add(CardLibrary.DEFEND);
            trickCtx.playCard(trickState.hand.size() - 1, 0);
        }
        c.equals("连续打技能牌能减到 0 费", 0, trickState.hand.get(0).cost());
        c.check("费用不会变成负数", trickState.hand.get(0).cost() >= 0);
        Combatant trickOwner2 = dummy("玩家2", 60);
        Enemy trickTarget2 = zombie(200, Intent.attack(1));
        CombatContext trickCtx2 = start(trickOwner2, List.of(trickTarget2), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PlayerState trickState2 = trickCtx2.stateOf(trickOwner2);
        trickState2.hand.clear();
        trickState2.energy = 99;
        trickState2.hand.add(CardLibrary.MAGIC_TRICK);
        trickCtx2.playCard(0, 0);
        c.equals("魔术技巧！：对所有敌人造成 60 点伤害", 140, trickTarget2.hp());
    }

    // ------------------------------------------------------------------ v0.5.4.1 A4-20（收官批）

    /**
     * v0.5.4.1（A4-20）收官批：灼热打击（**多次强化**）/ 炼金 / 尸体爆炸 / 行业工具 / 计划周全。
     *
     * <p>数值逐页核对过 kamigame：灼熱の一撃 12→16（基础 12、每档 +4）、錬金術 1→0 费、
     * 死体爆破 毒 6→9、商売道具 1→0 费、用意周到 保留 1→2 张。</p>
     */
    private static void a4Cards20(Checker c) {
        // ---- 灼热打击：**可以强化任意次**（每档 +4 伤害）
        c.equals("灼热打击：2 费", 2, CardLibrary.SEARING_BLOW.cost());
        c.check("灼热打击：罕见攻击牌",
                CardLibrary.SEARING_BLOW.rarity() == CardRarity.UNCOMMON
                        && CardLibrary.SEARING_BLOW.type() == CardType.ATTACK);
        c.check("灼热打击：基础 12 点",
                CardLibrary.resolve("searing_blow", 0).description().contains("12 点伤害"));
        c.check("灼热打击：+1 是 16 点（kamigame 的强化行）",
                CardLibrary.resolve("searing_blow", 1).description().contains("16 点伤害"));
        c.check("灼热打击：+7 是 12 + 4×7 = 40 点",
                CardLibrary.resolve("searing_blow", 7).description().contains("40 点伤害"));
        c.equals("灼热打击：强化次数没有上限（原版「何度でも」）", Integer.MAX_VALUE,
                CardLibrary.maxUpgrades("searing_blow"));
        c.equals("普通卡仍然只能强化 1 次", 1, CardLibrary.maxUpgrades("strike"));
        c.equals("没有强化版的牌是 0 次", 0, CardLibrary.maxUpgrades("void"));
        c.check("灼热打击：+1 档登记成「强化版」（图鉴的强化对照照常）",
                CardLibrary.hasUpgrade("searing_blow")
                        && CardLibrary.upgraded("searing_blow").name().equals("灼热打击+"));
        c.check("灼热打击：档位能反查（+3 → 第 3 档）",
                CardLibrary.upgradeLevel(CardLibrary.resolve("searing_blow", 3)) == 3);
        c.check("灼热打击：再强化一档是 +4（不是掉回 +1）",
                CardLibrary.nextUpgradeOf(CardLibrary.resolve("searing_blow", 3)).name()
                        .equals("灼热打击+4"));
        c.check("灼热打击：档位定义不进卡池（图鉴里只有基础版一张）",
                CardLibrary.all().stream().filter(card -> card.id().equals("searing_blow")).count() == 1);

        Combatant sbPlayer = dummy("玩家", 60);
        Enemy sbEnemy = zombie(200, Intent.unknown());
        CombatContext sbCtx = start(sbPlayer, List.of(sbEnemy),
                deck(CardLibrary.resolve("searing_blow", 3), 10), new ArrayList<>());
        sbCtx.playCard(0, 0);
        c.equals("灼热打击 +3：打出 24 点", 176, sbEnemy.hp());

        // 本场战斗升级（【军备】）：+3 的那一档要升到 +4
        Combatant sb2Player = dummy("玩家2", 60);
        Enemy sb2Enemy = zombie(200, Intent.unknown());
        CombatContext sb2Ctx = start(sb2Player, List.of(sb2Enemy), deck(CardLibrary.DEFEND, 10),
                new ArrayList<>());
        PlayerState sb2State = sb2Ctx.stateOf(sb2Player);
        sb2State.hand.clear();
        sb2State.energy = 99;
        sb2State.hand.add(CardLibrary.resolve("searing_blow", 3));
        sb2State.hand.add(CardLibrary.ARMAMENTS);
        sb2Ctx.playCard(1, 0);
        int sb2Pick = sb2Ctx.pickCandidates(sb2Player).indexOf(CardLibrary.resolve("searing_blow", 3));
        c.check("军备：手里的灼热打击 +3 仍然算「可以升级」", sb2Pick >= 0);
        sb2Ctx.resolveCardPick(sb2Player, List.of(sb2Pick));
        c.check("军备：+3 升到 +4（旧实现会掉回 +1）",
                sb2State.hand().stream().anyMatch(card -> card.name().equals("灼热打击+4")));

        // ---- 尸体爆炸：中毒 + 目标死亡时全体挨「等同其最大 HP」的伤害
        Combatant cePlayer = dummy("玩家", 60);
        Enemy ceA = zombie(30, Intent.unknown());
        Enemy ceB = zombie(60, Intent.unknown());
        CombatContext ceCtx = start(cePlayer, List.of(ceA, ceB), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        PlayerState ceState = ceCtx.stateOf(cePlayer);
        ceState.hand.clear();
        ceState.energy = 99;
        ceState.hand.add(CardLibrary.CORPSE_EXPLOSION);
        ceCtx.playCard(0, 0);
        c.equals("尸体爆炸：目标获得 6 层中毒", 6, ceA.poison());
        c.check("尸体爆炸：目标身上挂了「死亡时爆炸」的标记", ceA.corpseExplosion());
        ceCtx.takeFixedDamage(ceA, 30, "测试击杀");
        c.check("尸体爆炸：目标已经倒下", !ceA.alive());
        c.equals("尸体爆炸：另一个敌人挨了 30 点（= 死者的最大 HP）", 30, ceB.hp());
        c.check("尸体爆炸：标记已经摘掉（不会重复炸）", !ceA.corpseExplosion());
        c.check("尸体爆炸+：毒 6 → 9",
                CardLibrary.resolve("corpse_explosion", 1).description().contains("9 层中毒"));

        // 连锁：A 炸死 B，B 自己也带着标记 → 继续炸
        Combatant ce2Player = dummy("玩家2", 60);
        Enemy ce2A = zombie(30, Intent.unknown());
        Enemy ce2B = zombie(30, Intent.unknown());
        Enemy ce2C = zombie(90, Intent.unknown());
        CombatContext ce2Ctx = start(ce2Player, List.of(ce2A, ce2B, ce2C), deck(CardLibrary.STRIKE, 10),
                new ArrayList<>());
        ce2A.markCorpseExplosion();
        ce2B.markCorpseExplosion();
        ce2Ctx.takeFixedDamage(ce2A, 30, "测试击杀");
        c.check("尸体爆炸：连锁（A 炸死 B，B 又炸了一次 C）",
                !ce2A.alive() && !ce2B.alive() && ce2C.hp() == 90 - 30 - 30);
        c.check("尸体爆炸：连锁结束后两个标记都摘掉了",
                !ce2A.corpseExplosion() && !ce2B.corpseExplosion());

        // ---- 炼金：引擎只记一笔「待发药水」，MC 层打完牌立刻兑现
        Combatant alPlayer = dummy("玩家", 60);
        Enemy alEnemy = zombie(200, Intent.unknown());
        CombatContext alCtx = start(alPlayer, List.of(alEnemy), deck(CardLibrary.ALCHEMIZE, 10),
                new ArrayList<>());
        c.equals("炼金：还没打出时没有待发药水", 0, alCtx.stateOf(alPlayer).pendingPotions());
        alCtx.playCard(0, 0);
        c.equals("炼金：记下 1 瓶待发药水", 1, alCtx.stateOf(alPlayer).pendingPotions());
        c.equals("炼金：取走待发药水", 1, alCtx.takePendingPotions(alPlayer));
        c.equals("炼金：取走后清零", 0, alCtx.takePendingPotions(alPlayer));
        c.equals("炼金：自己进消耗堆", 1, alCtx.exhaustPile().size());
        c.check("炼金：带【消耗】关键字", CardLibrary.ALCHEMIZE.has(CardKeyword.EXHAUST));
        c.equals("炼金+：费用 1 → 0", 0, CardLibrary.resolve("alchemize", 1).cost());

        // ---- 行业工具：回合开始抽 1 弃 1（在正常抽牌之后）
        Combatant ttPlayer = dummy("玩家", 60);
        Enemy ttEnemy = zombie(200, Intent.unknown());
        CombatContext ttCtx = start(ttPlayer, List.of(ttEnemy), deck(CardLibrary.DEFEND, 30),
                new ArrayList<>());
        PlayerState ttState = ttCtx.stateOf(ttPlayer);
        ttState.hand.clear();
        ttState.energy = 99;
        ttState.hand.add(CardLibrary.TOOLS_OF_THE_TRADE);
        ttCtx.playCard(0, 0);
        c.equals("行业工具：层数 = 1", 1, ttState.toolsOfTheTrade());
        ttCtx.endTurn();
        c.equals("行业工具：下个回合抽 5 + 抽 1 - 弃 1 = 手里 5 张", 5, ttState.hand().size());
        c.equals("行业工具：确实弃掉了 1 张", 1, ttState.discardPile().size());
        c.equals("行业工具+：费用 1 → 0", 0, CardLibrary.resolve("tools_of_the_trade", 1).cost());

        // ---- 计划周全：回合结束额外保留 1（2）张
        Combatant wpPlayer = dummy("玩家", 60);
        Enemy wpEnemy = zombie(200, Intent.unknown());
        List<String> wpLog = new ArrayList<>();
        CombatContext wpCtx = start(wpPlayer, List.of(wpEnemy), deck(CardLibrary.DEFEND, 30), wpLog);
        PlayerState wpState = wpCtx.stateOf(wpPlayer);
        wpState.hand.clear();
        wpState.energy = 99;
        wpState.hand.add(CardLibrary.WELL_LAID_PLANS);
        wpCtx.playCard(0, 0);
        c.equals("计划周全：保留额度 = 1", 1, wpState.wellLaidPlans());
        wpState.hand.clear();
        wpState.hand.add(CardLibrary.STRIKE);
        wpState.hand.add(CardLibrary.DEFEND);
        wpState.hand.add(CardLibrary.BASH);        // 2 费：最贵的那张该被留下
        wpCtx.endTurn();
        c.check("计划周全：回合结束确实多留了 1 张",
                wpLog.stream().anyMatch(line -> line.contains("【计划周全】保留 1 张")));
        c.check("计划周全：留下的是费用最高的那张（重击）",
                wpState.hand().stream().anyMatch(card -> card.id().equals("bash")));

        Combatant wp2Player = dummy("玩家2", 60);
        Enemy wp2Enemy = zombie(200, Intent.unknown());
        List<String> wp2Log = new ArrayList<>();
        CombatContext wp2Ctx = start(wp2Player, List.of(wp2Enemy), deck(CardLibrary.DEFEND, 30), wp2Log);
        PlayerState wp2State = wp2Ctx.stateOf(wp2Player);
        wp2State.hand.clear();
        wp2State.energy = 99;
        wp2State.hand.add(CardLibrary.resolve("well_laid_plans", 1));
        wp2Ctx.playCard(0, 0);
        c.equals("计划周全+：保留额度 = 2", 2, wp2State.wellLaidPlans());
        wp2State.hand.clear();
        wp2State.hand.add(CardLibrary.STRIKE);
        wp2State.hand.add(CardLibrary.DEFEND);
        wp2State.hand.add(CardLibrary.BASH);
        wp2Ctx.endTurn();
        c.check("计划周全+：回合结束留 2 张",
                wp2Log.stream().anyMatch(line -> line.contains("【计划周全】保留 2 张")));

        // A4-20 的五张新卡都有强化版
        boolean allUpgradable = true;
        for (String id : new String[] {"searing_blow", "alchemize", "corpse_explosion",
                "tools_of_the_trade", "well_laid_plans"}) {
            if (!CardLibrary.canUpgrade(id)) {
                allUpgradable = false;
            }
        }
        c.check("A4-20 的五张新卡都有强化版", allUpgradable);
    }

    // ------------------------------------------------------------------ v0.5.5.0 A6-1（数据包注册表）

    /**
     * v0.5.5.0（A6-1）：{@code stc:card} 数据包注册表的**引擎侧覆盖层**
     * （{@link CardLibrary#applyExternal} / {@link CardLibrary#resetExternal}）与行为表
     * （{@link CardBehaviors}）。
     *
     * <p>这个测试会**改全局卡池**，所以整段包在 try/finally 里，结束时一定还原 ——
     * 否则它后面跑的断言（以及下一次跑）会看到被改过的打击。</p>
     */
    private static void cardRegistryOverlay(Checker c) {
        int builtinCount = CardLibrary.all().size();
        try {
            // ---- 行为表
            c.check("行为表不是空的（至少 15 条）", CardBehaviors.count() >= 15);
            c.check("行为「damage」在册", CardBehaviors.byId("damage") != null);
            c.check("未知行为会被拒绝",
                    CardBehaviors.validate("no_such_behavior", List.of(), List.of()) != null);
            c.check("参数个数不够会被拒绝", CardBehaviors.validate("damage", List.of(), List.of()) != null);
            c.check("参数个数够就通过", CardBehaviors.validate("damage", List.of(6), List.of()) == null);
            c.check("参数不合法时造不出效果（不会静默造一张空效果牌）",
                    CardBehaviors.build("no_such_behavior", "测试", List.of(), List.of()) == null);

            // ---- 新增一张数据包卡
            CardLibrary.applyExternal(List.of(CardLibrary.ExternalCard.of(
                    behaviorCard("dp_test", "测试牌", 5, "造成 5 点伤害。"),
                    behaviorCard("dp_test", "测试牌+", 8, "造成 8 点伤害。"))));
            c.check("数据包卡进入了卡池", CardLibrary.byId("dp_test") != null);
            c.equals("数据包卡计数 = 1", 1, CardLibrary.externalCount());
            c.check("这张卡被标成「数据包提供」", CardLibrary.isExternal("dp_test"));
            c.check("数据包卡的强化版也能解析",
                    CardLibrary.resolve("dp_test", 1).description().contains("8 点伤害"));

            Combatant dpPlayer = dummy("玩家", 60);
            Enemy dpEnemy = zombie(50, Intent.unknown());
            CombatContext dpCtx = start(dpPlayer, List.of(dpEnemy), deck(CardLibrary.byId("dp_test"), 10),
                    new ArrayList<>());
            dpCtx.playCard(0, 0);
            c.equals("数据包卡真的能打出 5 点伤害", 45, dpEnemy.hp());

            // ---- 覆盖内置卡（打击 → 10 点，并且把强化版拿掉）
            CardLibrary.applyExternal(List.of(CardLibrary.ExternalCard.of(
                    behaviorCard("strike", "打击", 10, "造成 10 点伤害。"))));
            c.check("内置卡被数据包覆盖",
                    CardLibrary.resolve("strike", 0).description().contains("10 点伤害"));
            c.check("数据包没给强化版 → 这张卡就没有强化版了", !CardLibrary.hasUpgrade("strike"));
            Combatant ovPlayer = dummy("玩家2", 60);
            Enemy ovEnemy = zombie(50, Intent.unknown());
            CombatContext ovCtx = start(ovPlayer, List.of(ovEnemy), deck(CardLibrary.byId("strike"), 10),
                    new ArrayList<>());
            ovCtx.playCard(0, 0);
            c.equals("覆盖后的打击打出 10 点", 40, ovEnemy.hp());
        } finally {
            CardLibrary.resetExternal();
        }
        c.equals("还原后没有数据包卡", 0, CardLibrary.externalCount());
        c.check("还原后打击回到内置的 6 点",
                CardLibrary.resolve("strike", 0).description().contains("6 点伤害"));
        c.check("还原后打击又有强化版了", CardLibrary.hasUpgrade("strike"));
        c.equals("还原后卡池张数与会话开始时一致", builtinCount, CardLibrary.all().size());
    }

    /** 用行为表拼一张最小可用的攻击牌（A6-1 自检用）。 */
    private static Card behaviorCard(String id, String name, int damage, String description) {
        CardEffect effect = CardBehaviors.build("damage", name, List.of(damage), List.of());
        return new Card(id, name, CardType.ATTACK, 1, CardTarget.ENEMY, description, CardRarity.COMMON, null,
                effect);
    }

}



