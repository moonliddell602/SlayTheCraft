package com.slaythecraft.combat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.FadeTimeline;

/**
 * 「战斗开始 / 第 X 回合」大标题的自检（v0.4.6.3）。
 *
 * <p>需求原文："战斗开始和第 X 回合的呈现动画可以使用原版 /title 大标题浮现。"</p>
 *
 * <p>做法：把客户端自绘的战斗横幅（{@code BattleBanners} / {@code BattleBannerLayer}）整个删掉，
 * 改由服务端在开战与每次回合数变化时下发原版 title 包
 * （{@code ClientboundSetTitleTextPacket} / {@code ClientboundSetTitlesAnimationPacket}），
 * 与 {@code /title} 指令走同一条路径。</p>
 *
 * <p>时长的**真实来源**是 {@link FadeTimeline} 的两个常量（1.5 秒 / 1.2 秒，需求原文里的数字），
 * 这里把它换算成 tick 去核对 {@code BattleTitles} 里的 6/18/6 与 6/12/6；
 * 其余接线与"旧横幅已删除"用读源码的方式核对（这些类一加载就要碰原版注册表，纯 JVM 起不来）。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.combat.BattleTitleCheck
 * </pre>
 */
public final class BattleTitleCheck {

    /** 20 tick = 1 秒。 */
    private static final long TICKS_PER_SECOND = 20L;

    private BattleTitleCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 1. 时长换算
        check(lines, counters, "「战斗开始」总时长 1.5 秒（需求原文）= 30 tick",
                FadeTimeline.BATTLE_START_MS * TICKS_PER_SECOND / 1000L == 30L, 30L,
                FadeTimeline.BATTLE_START_MS * TICKS_PER_SECOND / 1000L);
        check(lines, counters, "「第 X 回合」总时长 1.2 秒 = 24 tick",
                FadeTimeline.TURN_BANNER_MS * TICKS_PER_SECOND / 1000L == 24L, 24L,
                FadeTimeline.TURN_BANNER_MS * TICKS_PER_SECOND / 1000L);
        check(lines, counters, "淡入淡出各 0.3 秒 = 6 tick（两组动画都一样）",
                FadeTimeline.BANNER_FADE_MS * TICKS_PER_SECOND / 1000L == 6L, 6L,
                FadeTimeline.BANNER_FADE_MS * TICKS_PER_SECOND / 1000L);

        String titles = readSource("combat/mc/BattleTitles.java", lines, counters);
        String manager = readSource("combat/mc/CombatManager.java", lines, counters);
        String clientState = readSource("client/ClientBattleState.java", lines, counters);
        String clientSetup = readSource("client/StcClientSetup.java", lines, counters);
        if (titles == null || manager == null || clientState == null || clientSetup == null) {
            report(lines, counters);
            return;
        }

        // ---------------------------------------------------------- 2. 用的是原版 title 包
        check(lines, counters, "BattleTitles 发的是原版大标题包（ClientboundSetTitleTextPacket）",
                titles.contains("ClientboundSetTitleTextPacket"), true, false);
        check(lines, counters, "BattleTitles 发的是原版动画包（ClientboundSetTitlesAnimationPacket）",
                titles.contains("ClientboundSetTitlesAnimationPacket"), true, false);
        check(lines, counters, "BattleTitles 用的是原版副标题包（开战时主/副标题同屏）",
                titles.contains("ClientboundSetSubtitleTextPacket"), true, false);
        check(lines, counters, "战斗结束时清掉还在淡出的标题（ClientboundClearTitlesPacket）",
                titles.contains("ClientboundClearTitlesPacket"), true, false);
        check(lines, counters, "战斗开始：6 / 18 / 6 tick（= 30 tick = 1.5 秒）",
                titles.contains("new ClientboundSetTitlesAnimationPacket(6, 18, 6)"), true, false);
        check(lines, counters, "回合开始：6 / 12 / 6 tick（= 24 tick = 1.2 秒）",
                titles.contains("new ClientboundSetTitlesAnimationPacket(6, 12, 6)"), true, false);
        check(lines, counters, "标题文案沿用现有语言键（中英都能显示）",
                titles.contains("\"stc.battle.banner.start\"") && titles.contains("\"stc.battle.banner.turn\""),
                true, false);
        check(lines, counters, "标题从服务端下发（走 connection.send，不是自定义包）",
                titles.contains("member.connection.send("), true, false);

        // ---------------------------------------------------------- 3. 战斗流程确实会发标题
        check(lines, counters, "开战时立刻播「战斗开始」", manager.contains("BattleTitles.battleStart(roster)"),
                true, false);
        check(lines, counters, "回合数变化时补发「第 X 回合」", manager.contains("BattleTitles.turn("), true, false);
        check(lines, counters, "记着「已播报的回合数」，多人时不会重复发包",
                manager.contains("ANNOUNCED_TURN"), true, false);
        check(lines, counters, "战斗解散时清掉标题记账并收掉标题",
                manager.contains("ANNOUNCED_TURN.remove(context)") && manager.contains("BattleTitles.clear("),
                true, false);

        // ---------------------------------------------------------- 4. 旧的自绘横幅已经删掉
        check(lines, counters, "客户端不再自绘横幅（ClientBattleState 里没有 BattleBanners）",
                !clientState.contains("BattleBanners"), true, "仍在");
        check(lines, counters, "客户端不再注册横幅渲染层（StcClientSetup 里没有 BattleBannerLayer）",
                !clientSetup.contains("BattleBannerLayer"), true, "仍在");
        check(lines, counters, "BattleBanners.java 已删除",
                !Files.isRegularFile(sourcePath("client/BattleBanners.java")), true, "仍存在");
        check(lines, counters, "BattleBannerLayer.java 已删除",
                !Files.isRegularFile(sourcePath("client/BattleBannerLayer.java")), true, "仍存在");

        // ---------------------------------------------------------- 5. v0.4.6.9：BOSS 优先
        check(lines, counters, "start() 先查本层 BOSS（nearbyChallengableBosses）",
                manager.contains("nearbyChallengableBosses(host, bossRadius)"), true, false);
        check(lines, counters, "BOSS 优先时直接开 BOSS 战（EventKind.BOSS）",
                manager.contains("beginEncounter(host, pickedBoss, EventKind.BOSS"), true, false);
        check(lines, counters, "旧存档自愈：只是挑战过也会放行（clearBossesFought）",
                manager.contains("clearBossesFought()"), true, false);

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

    private static Path sourcePath(String relative) {
        return Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
    }

    private static String readSource(String relative, List<String> lines, int[] counters) {
        Path path = sourcePath(relative);
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

