package com.slaythecraft.network;

import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CombatPhase;
import com.slaythecraft.combat.core.PotionLibrary;

import net.minecraft.resources.ResourceLocation;

/**
 * 战斗快照的编解码往返校验（v0.0.2 V1）。
 *
 * <p>{@link BattleView} 是手写 NBT 编解码：编译器不会帮忙检查字段名与类型，所以这里逐字段往返比对。
 * 出牌界面完全依赖这一层，字段写错会表现为"界面上什么都看不到"，必须在这里挡住。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.network.BattleViewCheck
 * </pre>
 */
public final class BattleViewCheck {

    private BattleViewCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        List<BattleView.HandCard> hand = List.of(
                new BattleView.HandCard(id("strike"), 1, true, 0, "", "", "造成 4 点伤害。", true),
                new BattleView.HandCard(id("defend"), 1, false, 0, "", "", "获得 5 点格挡。", true),
                // v0.5.3.9：第 4 个布尔（playable=false）用来验"条件不满足"的牌在客户端被灰掉
                new BattleView.HandCard(id("bash"), 2, true, 1, "保留·消耗", "横扫之刃·亡灵杀手",
                        "造成 10 点伤害，施加 3 层易伤。", true),
                new BattleView.HandCard(id("clash"), 0, true, 0, "", "", "造成 14 点伤害。", false),
                // v0.5.4.1（A4-20）：强化**档数**（不再是 boolean）——【灼热打击】+3 要能原样往返
                new BattleView.HandCard(id("searing_blow"), 2, true, 3, "", "", "造成 24 点伤害。这张牌可以被强化任意次。",
                        true));
        List<BattleView.EnemyView> enemies = List.of(
                new BattleView.EnemyView("僵尸", 20, 20, 5, List.of("攻击 5"), true, true, "虚弱 5 · 易伤 2", false),
                // v0.5.0.7：多行动是**多行**，不再是 "攻击 4×2 + 施加虚弱 1"
                new BattleView.EnemyView("骷髅", 12, 20, 0, List.of("攻击 4×2（共 8）", "施加虚弱 1"),
                        false, true, "", true),
                new BattleView.EnemyView("蜘蛛", 0, 16, 0, List.of("未知"), false, false, "", false));
        List<ResourceLocation> potions = List.of(id("heal_potion"), id("poison_potion"), id("weak_potion"));
        List<String> selfStatuses = List.of("力量 2", "虚弱 3", "活力 4");
        List<String> selfOrbs = List.of("闪电球", "冰球", "", "玻璃球");

        // v0.4.2：三个牌堆的内容（"查看牌堆"用）
        List<BattleView.CardRef> drawCards = List.of(new BattleView.CardRef(id("strike"), false),
                new BattleView.CardRef(id("defend"), true));
        List<BattleView.CardRef> discardCards = List.of(new BattleView.CardRef(id("bash"), false));
        List<BattleView.CardRef> exhaustCards = List.of();

        BattleView original = new BattleView(true, true, "玩家A", 3, "PLAYER_TURN",
                2, 3, hand, 4, 3, 1, 15, 20, 7, selfStatuses, selfOrbs, 4, enemies, potions,
                drawCards, discardCards, exhaustCards, true, 2, 5,
                // v0.5.1.6：参战名单多带一个"这一轮是否已确认结束回合"
                List.of(new BattleView.PartyMember("玩家A", 15, 20, 7, true, true, true),
                        new BattleView.PartyMember("玩家B", 9, 20, 0, true, false, false)),
                // v0.4.6.7：手牌满标记（HUD 在药水栏上方提示"我的手牌太多了..."）
                true,
                // v0.5.1.6：玩家回合标记
                true);
        BattleView restored = BattleView.fromTag(original.toTag());

        check(lines, counters, "牌堆内容往返（v0.4.2：抽 / 弃 / 消耗）",
                restored.drawCards().equals(drawCards) && restored.discardCards().equals(discardCards)
                        && restored.exhaustCards().isEmpty(),
                drawCards + "/" + discardCards + "/[]",
                restored.drawCards() + "/" + restored.discardCards() + "/" + restored.exhaustCards());
        check(lines, counters, "牌堆内容的强化标记保持",
                restored.drawCards().get(1).upgraded() && !restored.drawCards().get(0).upgraded(),
                "false/true", restored.drawCards().get(0).upgraded() + "/"
                        + restored.drawCards().get(1).upgraded());
        // v0.5.3.9（A4-18）：手牌快照带上"这一刻能不能打出"（【交锋】这类动态条件）
        check(lines, counters, "手牌「能不能打出」标记往返（false 也照样保留）",
                restored.hand().get(0).playable() && restored.hand().get(3).playable() == false,
                "true/false", restored.hand().get(0).playable() + "/" + restored.hand().get(3).playable());

        check(lines, counters, "自己的充能球往返（v0.0.3 W4）",
                restored.selfOrbs().equals(selfOrbs) && restored.orbSlots() == 4,
                selfOrbs + " / 4", restored.selfOrbs() + " / " + restored.orbSlots());

        check(lines, counters, "active 保持", original.active() == restored.active(),
                original.active(), restored.active());
        check(lines, counters, "yourTurn 保持", original.yourTurn() == restored.yourTurn(),
                original.yourTurn(), restored.yourTurn());
        check(lines, counters, "当前行动玩家保持", original.activePlayer().equals(restored.activePlayer()),
                original.activePlayer(), restored.activePlayer());
        check(lines, counters, "回合数保持", original.turn() == restored.turn(), original.turn(), restored.turn());
        check(lines, counters, "阶段保持", original.phase().equals(restored.phase()),
                original.phase(), restored.phase());
        check(lines, counters, "能量与上限保持",
                original.energy() == restored.energy() && original.energyPerTurn() == restored.energyPerTurn(),
                original.energy() + "/" + original.energyPerTurn(),
                restored.energy() + "/" + restored.energyPerTurn());
        check(lines, counters, "堆计数器保持",
                original.drawPile() == restored.drawPile() && original.discardPile() == restored.discardPile()
                        && original.exhaustPile() == restored.exhaustPile(),
                "4/3/1", restored.drawPile() + "/" + restored.discardPile() + "/" + restored.exhaustPile());
        check(lines, counters, "玩家血量/上限/格挡保持",
                original.hp() == restored.hp() && original.maxHp() == restored.maxHp()
                        && original.block() == restored.block(),
                "15/20/7", restored.hp() + "/" + restored.maxHp() + "/" + restored.block());
        check(lines, counters, "状态文本保持", original.selfStatuses().equals(restored.selfStatuses()),
                original.selfStatuses(), restored.selfStatuses());
        check(lines, counters, "自己状态逐条往返（v0.0.3）",
                restored.selfStatuses().size() == 3 && restored.selfStatuses().contains("虚弱 3"),
                original.selfStatuses(), restored.selfStatuses());
        check(lines, counters, "卡面描述随状态往返（v0.0.3）",
                "造成 4 点伤害。".equals(restored.hand().get(0).description()),
                "造成 4 点伤害。", restored.hand().get(0).description());
        check(lines, counters, "敌人效果往返（v0.0.3）",
                "虚弱 5 · 易伤 2".equals(restored.enemies().get(0).statuses()), "虚弱 5 · 易伤 2",
                restored.enemies().get(0).statuses());
        check(lines, counters, "没有效果的敌人是空串", restored.enemies().get(1).statuses().isEmpty(), true,
                restored.enemies().get(1).statuses());
        check(lines, counters, "手牌数量保持", original.hand().size() == restored.hand().size(),
                original.hand().size(), restored.hand().size());
        check(lines, counters, "手牌字段逐张保持", original.hand().equals(restored.hand()),
                original.hand(), restored.hand());
        check(lines, counters, "需要指定目标的标记保持",
                restored.hand().get(0).needsTarget() && !restored.hand().get(1).needsTarget(), true,
                restored.hand().get(0).needsTarget());
        check(lines, counters, "强化标记保持", restored.hand().get(2).upgrades() == 1, 1,
                restored.hand().get(2).upgrades());
        // v0.5.4.1（A4-20）：档数（不是 boolean）要原样往返，而且能解析回正确的那一档定义
        check(lines, counters, "强化档数往返（【灼热打击】+3）", restored.hand().get(4).upgrades() == 3, 3,
                restored.hand().get(4).upgrades());
        check(lines, counters, "档数能解析回正确的那一档",
                CardLibrary.resolve(restored.hand().get(4).id().getPath(), restored.hand().get(4).upgrades())
                        .description().contains("24 点伤害"), true,
                CardLibrary.resolve(restored.hand().get(4).id().getPath(), restored.hand().get(4).upgrades())
                        .description());
        check(lines, counters, "关键字文本保持", "保留·消耗".equals(restored.hand().get(2).keywords()),
                "保留·消耗", restored.hand().get(2).keywords());
        check(lines, counters, "附魔文本保持", "横扫之刃·亡灵杀手".equals(restored.hand().get(2).enchantments()),
                "横扫之刃·亡灵杀手", restored.hand().get(2).enchantments());
        check(lines, counters, "无附魔的牌为空串", restored.hand().get(0).enchantments().isEmpty(), true,
                restored.hand().get(0).enchantments());
        check(lines, counters, "无关键字的牌为空串", restored.hand().get(0).keywords().isEmpty(), true,
                restored.hand().get(0).keywords());
        check(lines, counters, "敌人数量保持", original.enemies().size() == restored.enemies().size(),
                original.enemies().size(), restored.enemies().size());
        check(lines, counters, "敌人字段逐条保持", original.enemies().equals(restored.enemies()),
                original.enemies(), restored.enemies());
        check(lines, counters, "冲着我的意图标记保持", restored.enemies().get(0).targetingYou()
                && !restored.enemies().get(1).targetingYou(), true, restored.enemies().get(0).targetingYou());
        check(lines, counters, "已倒下敌人标记保持", !restored.enemies().get(2).alive(), false,
                restored.enemies().get(2).alive());
        check(lines, counters, "药水槽位数量保持", original.potions().size() == restored.potions().size(),
                original.potions().size(), restored.potions().size());
        check(lines, counters, "药水槽位顺序与内容保持", original.potions().equals(restored.potions()),
                original.potions(), restored.potions());
        check(lines, counters, "药水 id 能被药水表识别",
                PotionLibrary.byId(restored.potions().get(0).getPath()) == PotionLibrary.HEAL, "heal_potion",
                restored.potions().get(0).getPath());
        check(lines, counters, "负面药水能判定为指向敌人",
                PotionLibrary.byId(restored.potions().get(1).getPath()).kind().canTargetEnemy(), true,
                PotionLibrary.byId(restored.potions().get(1).getPath()).kind());
        check(lines, counters, "正面药水能判定为指向自己",
                !PotionLibrary.byId(restored.potions().get(0).getPath()).kind().canTargetEnemy(), false,
                PotionLibrary.byId(restored.potions().get(0).getPath()).kind());

        // 载荷包装层
        BattleView viaPayload = BattleStatePayload.of(original).view();
        check(lines, counters, "载荷包装后内容不变", viaPayload.equals(original), original, viaPayload);

        // 非战斗快照
        BattleView inactive = BattleView.inactive();
        BattleView inactiveBack = BattleView.fromTag(inactive.toTag());
        check(lines, counters, "非战斗快照往返后仍为非战斗", !inactiveBack.active(), false, inactiveBack.active());
        check(lines, counters, "非战斗快照手牌与敌人都为空",
                inactiveBack.hand().isEmpty() && inactiveBack.enemies().isEmpty(), true,
                inactiveBack.hand().size() + inactiveBack.enemies().size());
        check(lines, counters, "非战斗快照药水槽位为空", inactiveBack.potions().isEmpty(), true,
                inactiveBack.potions().size());
        check(lines, counters, "非战斗快照自己状态为空", inactiveBack.selfStatuses().isEmpty(), true,
                inactiveBack.selfStatuses().size());

        // 空手牌/空敌人也不能崩
        BattleView bare = new BattleView(true, false, "", 1, "ENEMY_TURN", 0, 3, List.of(), 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), 0, List.of(), List.of(), List.of(), List.of(), List.of(), false, 0, 3,
                List.of(), false, false);

        // v0.4.5：BOSS 战标记 + 层号（客户端据此播放该层 BOSS 音乐）
        check(lines, counters, "BOSS 战标记往返", restored.bossBattle(), true, restored.bossBattle());
        check(lines, counters, "层号往返", restored.floor() == 2, 2, restored.floor());
        check(lines, counters, "非 BOSS 战默认不打标记", !bare.bossBattle() && bare.floor() == 0, "false/0",
                bare.bossBattle() + "/" + bare.floor());
        check(lines, counters, "非战斗快照没有 BOSS 标记", !inactiveBack.bossBattle(), false,
                inactiveBack.bossBattle());

        // v0.4.6：药水栏位与参战名单（正面药水可以给队友用）
        check(lines, counters, "药水栏位往返（含【药水腰带】加成）", restored.potionSlots() == 5, 5,
                restored.potionSlots());
        check(lines, counters, "参战名单往返", restored.party().size() == 2
                        && restored.party().get(1).name().equals("玩家B")
                        && !restored.party().get(1).self(),
                "2 人 / 玩家B / 不是自己", restored.party().size() + " 人");
        // ---- v0.5.1.6：玩家回合 + 逐人确认
        check(lines, counters, "玩家回合标记往返", restored.playerRound(), true, restored.playerRound());
        check(lines, counters, "非战斗快照不是玩家回合", !inactiveBack.playerRound(), false,
                inactiveBack.playerRound());
        check(lines, counters, "「我已确认」标记往返", restored.selfReady(), true, restored.selfReady());
        check(lines, counters, "队友的确认状态也往返（玩家B 未确认）",
                !restored.party().get(1).ready(), false, restored.party().get(1).ready());
        check(lines, counters, "waitingFor 只列出没确认的队友",
                restored.waitingFor().equals(List.of("玩家B")), List.of("玩家B"), restored.waitingFor());
        check(lines, counters, "全员确认后 waitingFor 为空",
                new BattleView(true, true, "玩家A", 1, "PLAYER_TURN", 3, 3, List.of(), 0, 0, 0, 1, 1, 0,
                        List.of(), List.of(), 0, List.of(), List.of(), List.of(), List.of(), List.of(), false, 0, 3,
                        List.of(new BattleView.PartyMember("玩家A", 1, 1, 0, true, true, true),
                                new BattleView.PartyMember("玩家B", 1, 1, 0, true, false, true)),
                        false, true).waitingFor().isEmpty(),
                "空表", "非空");
        check(lines, counters, "倒下的队友不算在等待名单里",
                new BattleView(true, true, "玩家A", 1, "PLAYER_TURN", 3, 3, List.of(), 0, 0, 0, 1, 1, 0,
                        List.of(), List.of(), 0, List.of(), List.of(), List.of(), List.of(), List.of(), false, 0, 3,
                        List.of(new BattleView.PartyMember("玩家A", 1, 1, 0, true, true, true),
                                new BattleView.PartyMember("玩家B", 0, 1, 0, false, false, false)),
                        false, true).waitingFor().isEmpty(),
                "空表", "非空");
        check(lines, counters, "非战斗快照的参战名单为空", inactiveBack.party().isEmpty(), true,
                inactiveBack.party().size());

        // v0.4.6.7：手牌满标记（客户端在药水栏上方提示"我的手牌太多了..."）
        check(lines, counters, "手牌满标记往返", restored.handFull(), true, restored.handFull());
        check(lines, counters, "非战斗快照默认手牌不满", !inactiveBack.handFull(), false, inactiveBack.handFull());

        check(lines, counters, "空内容快照往返不崩",
                BattleView.fromTag(bare.toTag()).phase().equals("ENEMY_TURN"), "ENEMY_TURN",
                BattleView.fromTag(bare.toTag()).phase());

        // ---- v0.5.1.9：敌怪回合界面不再收起（"可以按 wasd 移动和旋转视角"的问题）
        check(lines, counters, "phase 字面量与引擎枚举对齐（改名会当场挂）",
                BattleView.PHASE_ENEMY_TURN.equals(CombatPhase.ENEMY_TURN.name()),
                CombatPhase.ENEMY_TURN.name(), BattleView.PHASE_ENEMY_TURN);
        check(lines, counters, "敌怪回合快照被识别为敌怪回合", bare.enemyTurn(), true, bare.enemyTurn());
        check(lines, counters, "玩家回合快照不算敌怪回合", !restored.enemyTurn(), false, restored.enemyTurn());
        check(lines, counters, "非战斗快照不算敌怪回合", !inactiveBack.enemyTurn(), false, inactiveBack.enemyTurn());
        check(lines, counters, "战斗结束（VICTORY/DEFEAT）不算敌怪回合",
                !new BattleView(true, false, "玩家A", 1, "VICTORY", 0, 3, List.of(), 0, 0, 0, 1, 1, 0,
                        List.of(), List.of(), 0, List.of(), List.of(), List.of(), List.of(), List.of(), false, 0, 3,
                        List.of(), false, false).enemyTurn(),
                "false", "true");
        check(lines, counters, "整场战斗都留着界面（keepsHudOpen = active）",
                bare.keepsHudOpen() && !inactiveBack.keepsHudOpen(), "true/false",
                bare.keepsHudOpen() + "/" + inactiveBack.keepsHudOpen());

        // ---- v0.5.1.6 回合模型改造：界面与文案的接线（源码级）
        String clientState = readSource("client/ClientBattleState.java", lines, counters);
        if (clientState != null) {
            // v0.5.1.9：整场战斗都显示（敌怪回合不再收起 → 玩家不会掉回世界里走动）
            check(lines, counters, "界面整场战斗都显示（含敌怪回合）",
                    clientState.contains("view.keepsHudOpen() && hiddenRemaining() <= 0L"), "出现", "缺失");
            check(lines, counters, "不再用「玩家回合 || 等队友」当显示条件（那会让敌怪回合掉回世界）",
                    !clientState.contains("view.yourTurn() || isWaitingForOthers()"), "已移除", "仍在");
            check(lines, counters, "提供 isEnemyPhase（敌怪回合界面留着但不可交互）",
                    clientState.contains("public static boolean isEnemyPhase()")
                            && clientState.contains("return view.enemyTurn();"),
                    "出现", "缺失");
            check(lines, counters, "进了敌怪回合就立刻取消「收起界面」（堵住 ESC 那几秒的窗口）",
                    clientState.contains("if (next.enemyTurn()) {")
                            && clientState.contains("hiddenUntil = 0L;"),
                    "出现", "缺失");
            check(lines, counters, "提供 isWaitingForOthers（玩家回合 + 已确认 + 未确认时不算）",
                    clientState.contains("public static boolean isWaitingForOthers()")
                            && clientState.contains("view.playerRound() && !view.yourTurn() && view.selfReady()"),
                    "出现", "缺失");
        }
        String screen = readSource("client/BattleScreen.java", lines, counters);
        if (screen != null) {
            // v0.5.1.9：只要战斗还在就绘制（敌怪回合也画）
            check(lines, counters, "出牌界面只要战斗没结束就继续绘制（含敌怪回合）",
                    screen.contains("if (!view.active()) {")
                            && screen.contains("ClientBattleState.isEnemyPhase()"),
                    "出现", "缺失");
            check(lines, counters, "敌怪回合画「敌怪正在行动」提示",
                    screen.contains("stc.battle.enemy_phase"), "出现", "缺失");
            check(lines, counters, "敌怪回合的结束回合按钮改成「敌怪回合…」",
                    screen.contains("stc.battle.enemy_turn"), "出现", "缺失");
            check(lines, counters, "轮不到我出牌时手牌变暗（INACTIVE_HAND_ALPHA）",
                    screen.contains("INACTIVE_HAND_ALPHA") && screen.contains("alphaOf(stable.get(i)) * handAlpha"),
                    "出现", "缺失");
            check(lines, counters, "轮不到我出牌时不吃悬停（hovered = -1）",
                    screen.contains(": -1;") && screen.contains("boolean interactive = view.yourTurn()"),
                    "出现", "缺失");
            check(lines, counters, "敌怪回合也留着「放弃战斗 / 查看牌堆」的出口（不整块吞掉点击）",
                    !screen.contains("!(canAct || ClientBattleState.isWaitingForOthers())")
                            && screen.contains("if (!canAct) {"),
                    "旧守卫已移除 / 出牌点击仍被吃掉", "仍在 / 缺失");
            check(lines, counters, "敌怪回合 ESC 不给收起界面（收起 = 放玩家回世界）",
                    screen.contains("if (ClientBattleState.isEnemyPhase()) {"), "出现", "缺失");
            check(lines, counters, "打断的拖动会跨回合清掉（render 里 clearDrag）",
                    screen.contains("if (!view.yourTurn()) {") && screen.contains("clearDrag();"), "出现", "缺失");
            check(lines, counters, "点击处理区分「能出牌」与「只能等」（canAct）",
                    screen.contains("boolean canAct = view.active() && view.yourTurn()"), "出现", "缺失");
            check(lines, counters, "已确认时画「等待队友」提示（waitingFor）",
                    screen.contains("view.waitingFor()") && screen.contains("stc.battle.waiting"), "出现", "缺失");
            check(lines, counters, "结束回合按钮在已确认后变成「已结束回合」",
                    screen.contains("stc.battle.end_turn.done"), "出现", "缺失");
            check(lines, counters, "多人回合标题在没有行动玩家时不画空名字",
                    screen.contains("!view.activePlayer().isEmpty()"), "出现", "缺失");
        }
        String manager = readSource("combat/mc/CombatManager.java", lines, counters);
        if (manager != null) {
            check(lines, counters, "出牌按「是谁发的包」分派（context.playCard(actor, ...)）",
                    manager.contains("context.playCard(actor, handNumber - 1"), "出现", "缺失");
            check(lines, counters, "结束回合走逐人确认（confirmEndTurn）",
                    manager.contains("context.confirmEndTurn(actor)"), "出现", "缺失");
            check(lines, counters, "意图数值按**观察者**算（actualIntentLines(self)）",
                    manager.contains("enemy.actualIntentLines(self == null ? enemy.target() : self)"), "出现",
                    "缺失");
        }
        String zh = readSource("main/resources/assets/stc/lang/zh_cn.json", lines, counters);
        String en = readSource("main/resources/assets/stc/lang/en_us.json", lines, counters);
        if (zh != null && en != null) {
            for (String key : new String[] {"stc.battle.waiting", "stc.battle.waiting.any",
                    "stc.battle.end_turn.done"}) {
                check(lines, counters, "回合模型新增语言键中英都有：" + key,
                        zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
            }
            // ---- v0.5.1.9：敌怪回合的文案（界面不再收起）
            for (String key : new String[] {"stc.battle.enemy_turn", "stc.battle.enemy_phase"}) {
                check(lines, counters, "敌怪回合语言键中英都有：" + key,
                        zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
            }
            // ---- v0.5.1.7 A3：选牌页的文案
            for (String key : new String[] {"stc.pick.title", "stc.pick.heading", "stc.pick.hint",
                    "stc.pick.hint.multi", "stc.pick.confirm", "stc.pick.skip", "stc.pick.empty"}) {
                check(lines, counters, "选牌页语言键中英都有：" + key,
                        zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
            }
        }

        // ---- v0.5.1.7 A3：卡牌选牌（引擎挂起 → 下发候选 → 回传下标）
        CardPickOfferPayload offer = new CardPickOfferPayload("选择一张牌", 2,
                List.of(new CardPickOfferPayload.Choice(id("strike"), false),
                        new CardPickOfferPayload.Choice(id("defend"), true)));
        check(lines, counters, "选牌请求：active / maxPicks / 候选（含强化标记）都保留",
                offer.active() && offer.maxPicks() == 2 && offer.candidates().size() == 2
                        && !offer.candidates().get(0).upgraded() && offer.candidates().get(1).upgraded(),
                "2 张 / 第 2 张是强化版", offer.candidates().size() + " 张");
        check(lines, counters, "空请求 = 「没有待选牌了」（客户端据此收掉选牌页）",
                !CardPickOfferPayload.cleared().active(), false, true);
        check(lines, counters, "选牌结果：空选择 + auto=false 表示「明确不选」",
                !new CardPickChoicePayload(List.of(), false).auto()
                        && new CardPickChoicePayload(List.of(1), false).indices().equals(List.of(1)),
                "auto=false 且下标保留", "auto=" + new CardPickChoicePayload(List.of(), false).auto());

        String pickManager = readSource("combat/mc/CombatManager.java", lines, counters);
        if (pickManager != null) {
            check(lines, counters, "出牌后会把候选下发给玩家（offerCardPickIfNeeded）",
                    pickManager.contains("offerCardPickIfNeeded(player, context, actor)"), "出现", "缺失");
            check(lines, counters, "收到回传后走引擎结算（context.resolveCardPick）",
                    pickManager.contains("context.resolveCardPick(actor,"), "出现", "缺失");
            check(lines, counters, "选牌有超时兜底（一直不选不会挡住整轮）",
                    pickManager.contains("PICK_DEADLINE") && pickManager.contains("cancelCardPick("), "出现", "缺失");
        }
        String setup = readSource("client/StcClientSetup.java", lines, counters);
        if (setup != null) {
            check(lines, counters, "选牌页不会被出牌界面每 tick 顶掉（pickPending 判断）",
                    setup.contains("boolean pickPending = ClientCardPickState.offer() != null")
                            && setup.contains("shouldShowScreen() && !pickPending"),
                    "出现", "缺失");
            check(lines, counters, "收到请求就弹选牌页（BattleCardPickScreen）",
                    setup.contains("new BattleCardPickScreen("), "出现", "缺失");
        }
        String battleClientState = readSource("client/ClientBattleState.java", lines, counters);
        if (battleClientState != null) {
            check(lines, counters, "战斗结束时清掉待选牌请求",
                    battleClientState.contains("ClientCardPickState.clear()"), "出现", "缺失");
        }
        String engine = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine != null) {
            check(lines, counters, "候选在挂起那一刻就快照（否则打出的那张落堆会让下标错位）",
                    engine.contains("candidates") && engine.contains("state.discardPile.view()"),
                    "有快照", "没有");
            check(lines, counters, "选牌没完成前不能出牌 / 不能结束回合",
                    engine.contains("先完成「\" + state.pendingPick.cardName()")
                            && engine.contains("还在等你选牌"),
                    "两道门禁都在", "缺门禁");
        }

        // ---- v0.5.3.3（A4-12）：回合钩子 + 【布置】的"打出前 0 费"
        String manager0533 = readSource("combat/mc/CombatManager.java", lines, counters);
        if (manager0533 != null) {
            check(lines, counters, "卡面费用用引擎算出来的**实际费用**（布置过的牌显示 0、扣费也是 0）",
                    manager0533.contains("context.effectiveCost(state, card)"), "出现", "缺失");
            check(lines, counters, "选牌提示有新 kind【布置】的分支",
                    manager0533.contains("case SETUP ->"), "出现", "缺失");
        }
        String engine0533 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0533 != null) {
            check(lines, counters, "两个回合钩子存在（onTurnStartForPowers / onTurnEndForPowers）",
                    engine0533.contains("private void onTurnStartForPowers(")
                            && engine0533.contains("private void onTurnEndForPowers("),
                    "都存在", "缺失");
            check(lines, counters, "回合钩子**接进了**回合开始与回合结束的流程",
                    engine0533.contains("onTurnStartForPowers(state);")
                            && engine0533.contains("onTurnEndForPowers(state);"),
                    "都接上了", "没人调用");
            check(lines, counters, "出牌的费用校验 / 扣费都走 effectiveCost（【布置】的唯一出口）",
                    engine0533.contains("int cost = effectiveCost(state, card);")
                            && engine0533.contains("state.energy -= cost;"),
                    "出现", "缺失");
            check(lines, counters, "【火焰屏障】的临时荆棘在**自己下一个回合开始**才收回",
                    engine0533.contains("temporaryThorns()") && engine0533.contains("clearTemporaryThorns()"),
                    "出现", "缺失");
        }

        // ---- v0.5.3.3（A4-13）：出牌钩子 / 格挡钩子 / 双发 / 费用改写
        String engine0534 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0534 != null) {
            check(lines, counters, "格挡只有一条入口 gainBlockFor（【主宰】要挂在每一次获得格挡上）",
                    engine0534.contains("public void gainBlockFor(Combatant owner, int amount)")
                            && engine0534.contains("triggerJuggernaut(owner)"),
                    "出现", "缺失");
            check(lines, counters, "【涂毒】用\"正在结算的是不是攻击牌\"判定，而不是看伤害来源",
                    engine0534.contains("private boolean resolvingAttackCard")
                            && engine0534.contains("resolvingAttackCard && hpLoss > 0"),
                    "出现", "缺失");
            check(lines, counters, "【腐化】走 effectiveCost（技能免费）",
                    engine0534.contains("state.corruption && card.type() == CardType.SKILL"),
                    "出现", "缺失");
            check(lines, counters, "【双重施法】/【爆发】在出牌流程里循环结算同一张牌",
                    engine0534.contains("state.consumeDoublePlay(card.type())")
                            && engine0534.contains("for (int play = 0; play < plays; play++)"),
                    "出现", "缺失");
            check(lines, counters, "双发遇到选牌会略过第二遍（避免选牌请求互相覆盖）",
                    engine0534.contains("还在等你选牌，第二遍略过"), "出现", "缺失");
            check(lines, counters, "【腐化】打完技能进消耗堆",
                    engine0534.contains("state.corruption && card.type() == CardType.SKILL) {")
                            && engine0534.contains("【腐化】：「"), "出现", "缺失");
        }

        // ---- v0.5.3.5（A4-14）：抽牌钩子 / 消耗钩子 / 下回合能量与抽牌
        String engine0535 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0535 != null) {
            check(lines, counters, "消耗只有一条入口 exhaustCard（【无痛】要挂在每一次消耗上）",
                    engine0535.contains("private void exhaustCard(PlayerState state, Card card)")
                            && engine0535.contains("onCardExhaustedForPowers(state, card)"),
                    "出现", "缺失");
            check(lines, counters, "抽牌钩子接在\"牌真的进了手牌\"之后（手牌满直接进弃牌堆的不算抽到）",
                    engine0535.contains("onCardDrawnForPowers(state, card)"),
                    "出现", "缺失");
            check(lines, counters, "下回合额外能量 / 抽牌在回合开始结算（takeNextTurnEnergy / takeNextTurnDraw）",
                    engine0535.contains("takeNextTurnEnergy()") && engine0535.contains("takeNextTurnDraw()"),
                    "出现", "缺失");
        }
        String combatant0535 = readSource("combat/core/Combatant.java", lines, counters);
        if (combatant0535 != null) {
            check(lines, counters, "「下回合能量 / 抽牌」与「下回合格挡」同一套写法（记账 → 取走并清零）",
                    combatant0535.contains("public final int takeNextTurnEnergy()")
                            && combatant0535.contains("public final int takeNextTurnDraw()"),
                    "出现", "缺失");
        }

        // ---- v0.5.3.6（A4-15）：自消耗效果表 / 本回合攻击计数 / 模糊保格挡
        String engine0536 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0536 != null) {
            check(lines, counters, "消耗入口会查「这张牌自己被消耗」的效果表（【哨兵】）",
                    engine0536.contains("CardLibrary.selfExhaust(card.id())")
                            && engine0536.contains("selfExhaust.apply(this, state, card)"),
                    "出现", "缺失");
            check(lines, counters, "本回合攻击牌计数在出牌循环里记（双发算两张）",
                    engine0536.contains("state.attacksPlayedThisTurn++"),
                    "出现", "缺失");
            check(lines, counters, "【模糊】在回合开始走\"不清空格挡\"的分支",
                    engine0536.contains("actor.blurTurns() > 0") && engine0536.contains("consumeBlurTurn()"),
                    "出现", "缺失");
        }
        String lib0536 = readSource("combat/core/CardLibrary.java", lines, counters);
        if (lib0536 != null) {
            check(lines, counters, "「自消耗有效果」是一张按 id 查的表（和 StatusCards 同一套路）",
                    lib0536.contains("registerSelfExhaust(") && lib0536.contains("public interface SelfExhaustEffect"),
                    "出现", "缺失");
        }

        // ---- v0.5.3.7（A4-16）：弃牌钩子 / 本回合 0 费 / 使用不可的技能牌
        String engine0537 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0537 != null) {
            check(lines, counters, "弃牌钩子挂在 discardFromHand（回合结束弃手牌不走这里，正好对应「你的回合内」）",
                    engine0537.contains("CardLibrary.selfDiscard(card.id())")
                            && engine0537.contains("selfDiscard.apply(this, state, card)"),
                    "出现", "缺失");
            check(lines, counters, "【地狱之刃】的「本回合 0 费」走 freeThisTurn（与【布置】的长期免费分开）",
                    engine0537.contains("state.isFreeThisTurn(card.id())")
                            && engine0537.contains("state.consumeFreeThisTurn(card.id())"),
                    "出现", "缺失");
            check(lines, counters, "随机攻击牌从该角色的奖励池里取",
                    engine0537.contains("public Card randomAttackCard(String characterId)"),
                    "出现", "缺失");
        }
        String discard0537 = readSource("combat/core/CardLibrary.java", lines, counters);
        if (discard0537 != null) {
            check(lines, counters, "「被弃置有效果」也是一张按 id 查的表（与自消耗表对称）",
                    discard0537.contains("registerSelfDiscard(")
                            && discard0537.contains("public interface SelfDiscardEffect"),
                    "出现", "缺失");
        }
        String status0537 = readSource("combat/core/StatusCards.java", lines, counters);
        if (status0537 != null) {
            check(lines, counters, "「使用不可」除了状态 / 诅咒，还包含两张技能牌（反射 / 战术家）",
                    status0537.contains("UNPLAYABLE_SKILLS")
                            && status0537.contains("\"reflex\"") && status0537.contains("\"tactician\""),
                    "出现", "缺失");
        }

        // ---- v0.5.3.8（A4-17）：E5 单卡记账 + 按 id 的费用改写
        String engine0538 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0538 != null) {
            check(lines, counters, "【血债血偿】/【高超打击】共用一份「本场失去生命次数」的账",
                    engine0538.contains("state.hpLossEvents()") && engine0538.contains("recordHpLoss()"),
                    "出现", "缺失");
            check(lines, counters, "【剔骨】用「本回合弃牌数」改费，且每回合重置",
                    engine0538.contains("cardsDiscardedThisTurn()") && engine0538.contains("recordDiscard()")
                            && engine0538.contains("state.cardsDiscardedThisTurn = 0;"),
                    "出现", "缺失");
        }
        String state0538 = readSource("combat/core/PlayerState.java", lines, counters);
        if (state0538 != null) {
            check(lines, counters, "E5 单卡记账表（cardCounters）挂在 PlayerState 上",
                    state0538.contains("cardCounters") && state0538.contains("public int cardCounter("),
                    "出现", "缺失");
        }

        // ---- v0.5.4.0（A4-19）：无形 / 抽牌堆为空的打出条件 / 本回合费用与失血
        String engine0540 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0540 != null) {
            check(lines, counters, "E15【无形】在 dealDamage 里把伤害封到 1",
                    engine0540.contains("target.intangibleTurns() > 0 && damage > 1"),
                    "出现", "缺失");
            check(lines, counters, "【华丽收场】走 playableNow（抽牌堆为空才能打出）",
                    engine0540.contains("return state.drawPile().isEmpty();"),
                    "出现", "缺失");
            check(lines, counters, "【子弹时间】同时挡住抽牌与费用",
                    engine0540.contains("state.bulletTime && amount > 0") && engine0540.contains("if (state.bulletTime)"),
                    "出现", "缺失");
            check(lines, counters, "【窒息】的失血挂在出牌钩子上",
                    engine0540.contains("state.chokeAmount > 0 && state.chokeEnemy != null"),
                    "出现", "缺失");
        }
        String state0540 = readSource("combat/core/PlayerState.java", lines, counters);
        if (state0540 != null) {
            check(lines, counters, "【幻影杀手】是「下个回合」生效（pending / active 两半）",
                    state0540.contains("phantomKillerPending") && state0540.contains("phantomKiller;"),
                    "出现", "缺失");
        }

        // ---- v0.5.4.1（A4-20）：多次强化（灼热打击）、尸体爆炸、炼金、行业工具、计划周全
        String engine0541 = readSource("combat/core/CombatContext.java", lines, counters);
        if (engine0541 != null) {
            check(lines, counters, "【尸体爆炸】在 checkBattleEnd 里结算（所有击杀路径都会走到）",
                    engine0541.contains("detonateCorpseExplosions();")
                            && engine0541.contains("private void detonateCorpseExplosions()"),
                    "出现", "缺失");
            check(lines, counters, "尸体爆炸有防重入闸（避免连锁爆炸递归）",
                    engine0541.contains("detonatingCorpseExplosions"), "出现", "缺失");
            check(lines, counters, "【行业工具】挂在正常抽牌之后",
                    engine0541.contains("applyToolsOfTheTrade(state);"), "出现", "缺失");
            check(lines, counters, "【计划周全】的保留额度在 discardHand 里",
                    engine0541.contains("keepByPlans"), "出现", "缺失");
            check(lines, counters, "【炼金】的待发药水由 MC 层取走（引擎只记账）",
                    engine0541.contains("public int takePendingPotions(Combatant owner)"), "出现", "缺失");
            check(lines, counters, "本场战斗升级按**当前档位**上一档（不再写死 resolve(id, 1)）",
                    engine0541.contains("CardLibrary.nextUpgradeOf(card)"), "出现", "缺失");
        }
        String lib0541 = readSource("combat/core/CardLibrary.java", lines, counters);
        if (lib0541 != null) {
            check(lines, counters, "【灼热打击】的档位是懒生成的（不进 BY_ID，图鉴只出现基础版）",
                    lib0541.contains("SEARING_BLOW_TIERS.computeIfAbsent"), "出现", "缺失");
            check(lines, counters, "多次强化的上限问 maxUpgrades",
                    lib0541.contains("public static int maxUpgrades(String cardId)")
                            && lib0541.contains("MAX_UPGRADES.put(\"searing_blow\", Integer.MAX_VALUE)"),
                    "出现", "缺失");
            check(lines, counters, "档位反查忽略附魔（sameDefinition）",
                    lib0541.contains("private static boolean sameDefinition(Card a, Card b)"), "出现", "缺失");
        }
        String manager0541 = readSource("combat/mc/CombatManager.java", lines, counters);
        if (manager0541 != null) {
            check(lines, counters, "【炼金】在打完牌那一刻兑现药水",
                    manager0541.contains("grantPendingPotions(player, context, actor);"), "出现", "缺失");
            check(lines, counters, "快照发的是强化**档数**",
                    manager0541.contains("CardLibrary.upgradeLevel(card)"), "出现", "缺失");
        }
        String view0541 = readSource("network/BattleView.java", lines, counters);
        if (view0541 != null) {
            check(lines, counters, "手牌快照带的是 upgrades（档数）而不是 upgraded（布尔）",
                    view0541.contains("entry.putInt(\"upgrades\", card.upgrades())")
                            && view0541.contains("entry.contains(\"upgrades\")"),
                    "出现", "缺失");
        }

        // ---- v0.5.1.7 美术：每颗充能球的动画分开播
        String fx = readSource("client/ClientBattleFx.java", lines, counters);
        if (fx != null) {
            check(lines, counters, "充能球伤害走**排队**（enqueue）而不是覆盖（start）",
                    fx.contains("enqueue(victim, CombatFx.ENEMY_HURT, BattleAnim.HURT_MS,")
                            && !fx.contains("start(target, CombatFx.ENEMY_HURT"),
                    "排队", "仍是覆盖式 start");
            check(lines, counters, "充能球格挡也排队（每颗球各蹲起一次 + 各响一声）",
                    fx.contains("enqueue(actor, CombatFx.PLAYER_BLOCK, BattleAnim.DEFEND_MS,"), "出现", "缺失");
            check(lines, counters, "排队的槽位带音效（音效也跟着逐颗播，而不是同时响）",
                    fx.contains("SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.8F, victim"), "出现", "缺失");
        }

        // ---- v0.5.1.8：拖动打出（非指向性牌）+ 同名卡动画修复
        check(lines, counters, "短按（还没到长按时间）停在「按住」阶段，不会出牌",
                com.slaythecraft.client.BattleScreen.dragStage(true, 0L, false)
                        == com.slaythecraft.client.BattleScreen.DragStage.PRESSING,
                "PRESSING", com.slaythecraft.client.BattleScreen.dragStage(true, 0L, false));
        check(lines, counters, "长按够久进入拖动阶段",
                com.slaythecraft.client.BattleScreen.dragStage(true,
                        com.slaythecraft.client.BattleScreen.DRAG_HOLD_MS, false)
                        == com.slaythecraft.client.BattleScreen.DragStage.DRAGGING,
                "DRAGGING", "别的阶段");
        check(lines, counters, "指向性牌不走拖动（保持「点牌 → 点敌人」）",
                com.slaythecraft.client.BattleScreen.dragStage(true, 9999L, true)
                        == com.slaythecraft.client.BattleScreen.DragStage.NONE,
                "NONE", "被当成可拖动");
        check(lines, counters, "没按下时不在任何阶段",
                com.slaythecraft.client.BattleScreen.dragStage(false, 9999L, false)
                        == com.slaythecraft.client.BattleScreen.DragStage.NONE,
                "NONE", "别的阶段");
        for (int guiHeight : new int[] {240, 300, 480}) {
            int[] zone = com.slaythecraft.client.BattleScreen.playZoneRect(320, guiHeight);
            int handTop = com.slaythecraft.client.BattleScreen.handYFor(guiHeight, true);
            check(lines, counters, "打出区水平居中（高 " + guiHeight + "）",
                    zone[0] + zone[2] / 2 == 320 / 2, 160, zone[0] + zone[2] / 2);
            check(lines, counters, "打出区在屏幕内（高 " + guiHeight + "）",
                    zone[1] >= 0 && zone[1] + zone[3] <= guiHeight, "在屏幕内",
                    zone[1] + ".." + (zone[1] + zone[3]));
            check(lines, counters, "打出区不压到手牌（高 " + guiHeight + "）",
                    zone[1] + zone[3] <= handTop - 8, "≤ " + (handTop - 8), zone[1] + zone[3]);
            check(lines, counters, "打出区有实际面积（高 " + guiHeight + "）",
                    zone[2] >= 40 && zone[3] >= 24, "≥ 40×24", zone[2] + "×" + zone[3]);
        }
        int[] zone480 = com.slaythecraft.client.BattleScreen.playZoneRect(320, 480);
        check(lines, counters, "屏幕中心算在打出区里（松手就能出牌）",
                com.slaythecraft.client.BattleScreen.isInPlayZone(160, zone480[1] + zone480[3] / 2.0, 320, 480),
                true, false);
        check(lines, counters, "屏幕四角不算在打出区里（拖到别处松手不会误出牌）",
                !com.slaythecraft.client.BattleScreen.isInPlayZone(2, 2, 320, 480)
                        && !com.slaythecraft.client.BattleScreen.isInPlayZone(318, 478, 320, 480),
                true, false);

        String battleScreen = readSource("client/BattleScreen.java", lines, counters);
        if (battleScreen != null) {
            check(lines, counters, "非指向性牌：点击只开始「按住」，真正的出牌在 mouseReleased",
                    battleScreen.contains("pressedCard = index;")
                            && battleScreen.contains("public boolean mouseReleased("),
                    "出现", "缺失");
            check(lines, counters, "松手时只有落在打出区里才出牌（并且检查能量/可打出）",
                    battleScreen.contains("dragged && inZone && index >= 0")
                            && battleScreen.contains("if (playable && card.cost() <= view.energy())"),
                    "出现", "缺失");
            check(lines, counters, "拖动期间画打出区与跟着鼠标的牌",
                    battleScreen.contains("drawDragInteraction(")
                            && battleScreen.contains("playZoneRect(width, height)"),
                    "出现", "缺失");
            // bug 修复：同名卡不再被误播动画
            check(lines, counters, "同名卡动画：按**定义**计数（不再用会重编号的位置编号）",
                    battleScreen.contains("definitionCounts(previousHandKeys)")
                            && battleScreen.contains("definitionCounts(stable)"),
                    "按定义计数", "仍按位置编号");
            check(lines, counters, "同名卡动画：扣掉已经起过的动画，避免重复播",
                    battleScreen.contains("int toStart = missing - inFlightFor(definition)"), "出现", "缺失");
        }
        if (zh != null && en != null) {
            for (String key : new String[] {"stc.battle.drag.hold", "stc.battle.drag.release",
                    "stc.battle.drag.no_energy"}) {
                check(lines, counters, "拖动打出语言键中英都有：" + key,
                        zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
            }
        }

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

    /** 读一段主源码（源码级断言用）；读不到返回 {@code null}。 */
    private static String readSource(String relative, List<String> lines, int[] counters) {
        java.nio.file.Path path = java.nio.file.Path.of("src", "main", "java", "com", "slaythecraft")
                .resolve(relative);
        if (!java.nio.file.Files.isRegularFile(path)) {
            // 语言文件在 resources 下，不在 java 包内 —— 换一条路径再试
            path = java.nio.file.Path.of("src").resolve(relative);
        }
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
