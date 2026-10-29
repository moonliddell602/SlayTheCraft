package com.slaythecraft.combat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.Config;
import com.slaythecraft.character.RunService;
import com.slaythecraft.data.PlayerCardData;

/**
 * 战败处理的回归自检（v0.4.6.3）。
 *
 * <p>需求原文：</p>
 *
 * <pre>
 * 单人模式下，玩家战败时应当：立刻击杀此玩家并重开，回到第一层重新选择角色。
 * 多人模式下：队伍仍有存活玩家时，战败玩家会在局内继续等待队友，直到战斗结束或全军覆没。
 *             战败玩家的局内回合会直接结束。如果战斗胜利，战败玩家会恢复 1 点血量。
 * 队伍玩家全部战败：立刻击杀所有同组全部玩家并令同组全部玩家重开，回到第一层重新选择角色。
 * </pre>
 *
 * <p>引擎侧（"部分倒下 ≠ 战斗结束、倒下的人被跳过、全员倒下才判负"）已经由
 * {@code CombatSelfTest} 的"多人遭遇战"一节覆盖；本类盯住的是 **MC 层**新增的三件事：</p>
 * <ol>
 *   <li>{@link RunService#clearRun} 真的把本局清成"回第 1 层 + 待选角色 + 初始金币"（这条是真调用）；</li>
 *   <li>全员倒下时走 {@code wipeAndRestart}：清本局 + {@code kill()} 每个同组玩家；</li>
 *   <li>倒下但队友打赢时只恢复 1 点生命值（不是回满）。</li>
 * </ol>
 *
 * <p>为什么 2 / 3 是读源码：{@code CombatManager} 的静态块就要加载原版实体与 FML 上下文，
 * 纯 JVM 起不来（同样的理由见 {@code BossCodexCheck} 的类注释）。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.combat.DefeatFlowCheck
 * </pre>
 */
public final class DefeatFlowCheck {

    private DefeatFlowCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 1. 重开 = 回第 1 层 + 重选角色 + 初始金币
        PlayerCardData data = new PlayerCardData();
        data.setCharacter(net.minecraft.resources.ResourceLocation.parse("stc:ironclad"));
        data.addGold(500);
        data.addCard(com.slaythecraft.data.CardInstance.of(
                net.minecraft.resources.ResourceLocation.parse("stc:strike")));
        data.setProgress(com.slaythecraft.combat.core.ProgressState.of(3, java.util.Map.of(),
                java.util.Set.of(1, 2, 3), true));
        RunService.clearRun(data);
        check(lines, counters, "战败重开：角色被清空（回到'待选角色'）", data.character() == null, null,
                data.character());
        check(lines, counters, "战败重开：回到第 1 层", data.floor() == 1, 1, data.floor());
        check(lines, counters, "战败重开：三层进度清空", !data.progress().completed(), false,
                data.progress().completed());
        check(lines, counters, "战败重开：牌组清空", data.cardCount() == 0, 0, data.cardCount());
        check(lines, counters, "战败重开：金币重置为初始金币（" + Config.startingGold() + "）",
                data.gold() == Config.startingGold(), Config.startingGold(), data.gold());
        check(lines, counters, "战败重开：独立生命值回满", data.life().isFull(), true, data.life().describe());
        check(lines, counters, "战败重开：本局不再是'已结束'状态（可以在入口重新选人）", !data.isRunOver(), false,
                data.isRunOver());

        // ---------------------------------------------------------- 2. 全员倒下：击杀 + 重开
        String manager = readSource("combat/mc/CombatManager.java", lines, counters);
        if (manager == null) {
            report(lines, counters);
            return;
        }
        check(lines, counters, "全员倒下有条专门的收尾方法 wipeAndRestart",
                manager.contains("private static void wipeAndRestart("), true, false);
        check(lines, counters, "全员倒下时对同组每个玩家清空本局（RunService.clearRun）",
                manager.contains("RunService.clearRun(data)"), true, false);
        check(lines, counters, "全员倒下时立刻击杀玩家（原版 kill 流程）",
                manager.contains("member.kill()"), true, false);
        check(lines, counters, "战败分支在 release 之后才重开（否则生命值会被再写回一次）",
                manager.indexOf("wipeAndRestart(roster);") > manager.indexOf("release(player, context, outcome);"),
                true, "顺序不对");
        check(lines, counters, "战败分支只对单人/全灭生效（胜利分支不给重开）",
                manager.contains("if (won) {") && manager.contains("} else {\n            // v0.4.6.3"), true, false);

        // ---------------------------------------------------------- 3. 多人：倒下的人在局内等待
        check(lines, counters, "倒下的玩家在行动栏看到等待提示（不立刻出局）",
                manager.contains("等待队友战斗结束"), true, false);
        check(lines, counters, "倒下但队友打赢：只恢复 1 点生命值",
                manager.contains("if (combatant.hp() <= 0 && won)") && manager.contains("pool.set(1)"), true, false);
        check(lines, counters, "倒下恢复 1 点的分支优先于 BOSS 战的'回满'",
                manager.indexOf("if (combatant.hp() <= 0 && won)") < manager.indexOf("} else if (bossVictory) {"),
                true, "顺序不对");
        check(lines, counters, "全队倒下时不再只'标记本局结束'（旧的 markRunDefeated 已删除）",
                !manager.contains("private static void markRunDefeated"), true, "仍在");

        report(lines, counters);
    }

    private static void report(List<String> lines, int[] counters) {
        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static String readSource(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
        if (!Files.isRegularFile(path)) {
            check(lines, counters, "源码存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
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
