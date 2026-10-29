package com.slaythecraft.chat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天框输出的回归自检（v0.4.6.3）。
 *
 * <p>需求（v0.4.6.3）："聊天框不再输出系统执行结果和指令。"<br>
 * 需求（v0.4.6.5 追加）："指令回执保留、只有战报和系统提示静音。"
 * 于是 {@link StcChat} 拆成三条通道（指令回执默认开，战报 / 系统提示默认关），
 * 下面除了"没有漏网的原版聊天 API"，还逐条核对这三个开关与接线。</p>
 *
 * <p>实现方式是"单一出口"：模组里所有会写进聊天框的调用都必须经过 {@link StcChat}
 * （它读 {@code Config.chatOutput}，默认关闭）。这个自检把整棵 {@code src/main/java} 扫一遍，
 * 确认**没有漏网的原始聊天 API 调用** —— 以后新加代码时如果直接写 {@code sendSystemMessage}
 * 或 {@code sendSuccess}，这里会立刻报出来。</p>
 *
 * <p>哪些不算漏网：{@link StcChat} 自己（它就是那个出口），以及行动栏
 * （{@code displayClientMessage(..., true)} —— 那是屏幕下方的一行提示，不是聊天框）。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.chat.ChatOutputCheck
 * </pre>
 */
public final class ChatOutputCheck {

    /**
     * 聊天框写入 API（出现在 StcChat 之外就算漏网）。
     *
     * <p>全是正则：{@code displayClientMessage} 的第二个参数才是"聊天 / 行动栏"的开关
     * （{@code false} = 聊天框，{@code true} = 屏幕下方的行动栏，后者不在本次范围内）。</p>
     */
    private static final List<String> CHAT_APIS = List.of(
            "\\.sendSystemMessage\\(",
            "\\.sendSuccess\\(",
            "\\.sendFailure\\(",
            "\\.displayClientMessage\\([^;]*?,\\s*false\\)");

    /** 必须通过 StcChat 出口的关键文件（覆盖引擎战报、指令回执、界面提示三条链路）。 */
    private static final Map<String, String> MUST_ROUTE = new LinkedHashMap<>();

    static {
        MUST_ROUTE.put("combat/mc/CombatManager.java", "战斗系统提示 + 引擎战报出口");
        MUST_ROUTE.put("combat/mc/CombatCommands.java", "战斗指令回执");
        MUST_ROUTE.put("combat/mc/CombatStatusCommands.java", "状态指令回执");
        MUST_ROUTE.put("binder/BinderCommands.java", "收纳包指令回执");
        MUST_ROUTE.put("progress/ProgressCommands.java", "进度指令回执");
        MUST_ROUTE.put("relic/RelicCommands.java", "遗物指令回执");
        MUST_ROUTE.put("potion/PotionCommands.java", "药水指令回执");
        MUST_ROUTE.put("reward/RewardCommands.java", "奖励指令回执");
        MUST_ROUTE.put("villager/CardMasterCommands.java", "卡牌大师指令回执");
        MUST_ROUTE.put("binder/BinderOpener.java", "收纳包界面提示");
        MUST_ROUTE.put("merchant/CardShopMenu.java", "商店按钮结果");
        MUST_ROUTE.put("gate/SpireGateMenu.java", "尖塔入口按钮结果");
        MUST_ROUTE.put("campfire/CampfireMenu.java", "篝火按钮结果");
        MUST_ROUTE.put("campfire/CampfireEvents.java", "篝火交互提示");
        MUST_ROUTE.put("campfire/CampfireService.java", "篝火流程提示");
        MUST_ROUTE.put("reward/RewardMenu.java", "奖励领取结果");
        MUST_ROUTE.put("reward/RewardService.java", "击杀/胜利奖励提示");
        MUST_ROUTE.put("question/QuestionService.java", "问号事件结果");
        MUST_ROUTE.put("item/BattleStandardItem.java", "战斗军旗提示");
        MUST_ROUTE.put("network/StcPayloads.java", "网络包处理结果");
        MUST_ROUTE.put("villager/CardMasterEvents.java", "卡牌大师生成公告");
        MUST_ROUTE.put("merchant/CardMerchantSpawnEggItem.java", "商人生成提示");
        MUST_ROUTE.put("binder/CardBinderMenu.java", "收纳包/回收站按钮结果");
    }

    private ChatOutputCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        Path root = Path.of("src", "main", "java");
        Map<String, String> sources = new LinkedHashMap<>();
        int[] io = {0};
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        sources.put(relativize(root, file), Files.readString(file, StandardCharsets.UTF_8));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    io[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            check(lines, counters, "能遍历 " + root + "（" + e + "）", false, "可遍历", e.getClass().getSimpleName());
        }
        check(lines, counters, "读到了 " + sources.size() + " 个源文件", sources.size() > 100, "> 100",
                sources.size());
        check(lines, counters, "遍历过程中没有读不了的文件", io[0] == 0, 0, io[0]);

        // ---------------------------------------------------------- 1. 没有漏网的原始聊天调用
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String name = entry.getKey();
            if (name.endsWith("chat/StcChat.java")) {
                continue; // 出口本身
            }
            for (String api : CHAT_APIS) {
                if (java.util.regex.Pattern.compile(api).matcher(entry.getValue()).find()) {
                    check(lines, counters, name + " 不再直接调用 /" + api + "/（必须走 StcChat）", false,
                            "走 StcChat", "直接调用");
                }
            }
        }
        // 上面循环只在失败时计数；成功时补一条总结，避免"零断言"的假绿
        boolean clean = true;
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            if (entry.getKey().endsWith("chat/StcChat.java")) {
                continue;
            }
            for (String api : CHAT_APIS) {
                clean &= !java.util.regex.Pattern.compile(api).matcher(entry.getValue()).find();
            }
        }
        check(lines, counters, "全部 " + sources.size() + " 个源文件里没有绕过 StcChat 的聊天写入", clean, true, clean);

        // ---------------------------------------------------------- 2. 出口本身接上了配置开关
        String gate = sources.get("com/slaythecraft/chat/StcChat.java");
        check(lines, counters, "chat/StcChat.java 存在（聊天框的唯一出口）", gate != null, "存在", "缺失");
        if (gate != null) {
            check(lines, counters, "StcChat 读总开关 Config.chatOutput()", gate.contains("Config.chatOutput()"),
                    true, false);
            check(lines, counters, "StcChat 读指令回执开关 Config.chatCommandFeedback()",
                    gate.contains("Config.chatCommandFeedback()"), true, false);
            check(lines, counters, "StcChat 读战报开关 Config.chatBattleLog()",
                    gate.contains("Config.chatBattleLog()"), true, false);
            check(lines, counters, "StcChat 读系统提示开关 Config.chatSystemMessage()",
                    gate.contains("Config.chatSystemMessage()"), true, false);
            check(lines, counters, "StcChat 里保留了真正的原版调用（打开开关时能恢复）",
                    gate.contains("sendSystemMessage") && gate.contains("sendSuccess")
                            && gate.contains("sendFailure"), true, false);
        }

        // ---------------------------------------------------------- 2b. 三类通道的默认值（需求：指令回执保留、战报与系统提示静音）
        check(lines, counters, "指令回执默认**开**（需求：指令回执保留）",
                com.slaythecraft.Config.CHAT_COMMAND_FEEDBACK.getDefault(), true,
                com.slaythecraft.Config.CHAT_COMMAND_FEEDBACK.getDefault());
        check(lines, counters, "战斗战报默认**关**（需求：战报静音）",
                Boolean.FALSE.equals(com.slaythecraft.Config.CHAT_BATTLE_LOG.getDefault()), false,
                com.slaythecraft.Config.CHAT_BATTLE_LOG.getDefault());
        check(lines, counters, "系统提示默认**关**（需求：系统提示静音）",
                Boolean.FALSE.equals(com.slaythecraft.Config.CHAT_SYSTEM_MESSAGE.getDefault()), false,
                com.slaythecraft.Config.CHAT_SYSTEM_MESSAGE.getDefault());
        check(lines, counters, "总开关默认关（排查时才打开）",
                Boolean.FALSE.equals(com.slaythecraft.Config.CHAT_OUTPUT.getDefault()), false,
                com.slaythecraft.Config.CHAT_OUTPUT.getDefault());
        // 行为断言用"回退值"（配置没加载时访问器读的就是它）；SPEC 是包内可见的，
        // 所以这里用 Config.chatOutput() 这类公开访问器来判断默认行为是否一致。
        check(lines, counters, "默认行为：指令回执通道是开的（Config.chatCommandFeedback 回退值）",
                com.slaythecraft.Config.chatCommandFeedback(), true,
                com.slaythecraft.Config.chatCommandFeedback());
        check(lines, counters, "默认行为：战报通道是关的（Config.chatBattleLog 回退值）",
                !com.slaythecraft.Config.chatBattleLog(), false, com.slaythecraft.Config.chatBattleLog());
        check(lines, counters, "默认行为：系统提示通道是关的（Config.chatSystemMessage 回退值）",
                !com.slaythecraft.Config.chatSystemMessage(), false,
                com.slaythecraft.Config.chatSystemMessage());
        check(lines, counters, "通道开关与 StcChat 判定一致（总开关关闭时）",
                !com.slaythecraft.Config.chatOutput()
                        && StcChat.feedbackEnabled() == com.slaythecraft.Config.chatCommandFeedback()
                        && StcChat.battleEnabled() == com.slaythecraft.Config.chatBattleLog()
                        && StcChat.systemEnabled() == com.slaythecraft.Config.chatSystemMessage(),
                true, false);

        // ---------------------------------------------------------- 3. 关键链路都接了出口
        for (Map.Entry<String, String> entry : MUST_ROUTE.entrySet()) {
            String source = sources.get("com/slaythecraft/" + entry.getKey());
            check(lines, counters, entry.getKey() + " 走 StcChat 出口（" + entry.getValue() + "）",
                    source != null && source.contains("StcChat."), "已接线", source == null ? "文件缺失" : "未接线");
        }
        // 引擎战报：CombatContext / 三张牌表里的每一条 log 都由这个 lambda 接到"战报"通道
        String combat = sources.get("com/slaythecraft/combat/mc/CombatManager.java");
        check(lines, counters, "引擎战报接到 StcChat 的**战报**通道（只有战报静音）",
                combat != null && combat.contains("CombatLog log = message -> battleLog(roster, message)")
                        && combat.contains("StcChat.battle(roster, message)"),
                true, "未接线");
        check(lines, counters, "战斗系统提示走 StcChat 的**系统提示**通道（send → StcChat.note）",
                combat != null && combat.contains("StcChat.note(player, message)"), true, "未接线");

        // ---------------------------------------------------------- 4. 行动栏没有被误伤
        String actionBarSource = combat;
        check(lines, counters, "行动栏（displayClientMessage(..., true)）保持不变",
                actionBarSource != null && actionBarSource.contains("displayClientMessage(Component.literal(text), true)"),
                true, "被改掉了");

        // ---------------------------------------------------------- 5. 调用点数量（防止"改一半"）
        int callSites = 0;
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            if (entry.getKey().endsWith("chat/StcChat.java")) {
                continue;
            }
            int from = 0;
            while ((from = entry.getValue().indexOf("StcChat.", from)) >= 0) {
                callSites++;
                from += "StcChat.".length();
            }
        }
        check(lines, counters, "StcChat 调用点数量符合预期（> 100 处，含全部指令回执与系统提示）",
                callSites > 100, "> 100", callSites);

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static String relativize(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
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
