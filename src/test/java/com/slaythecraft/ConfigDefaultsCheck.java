package com.slaythecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置**默认值**自检（v0.4.5）。
 *
 * <p>为什么单独一个自检：{@link Config} 的每个访问器在"配置还没加载"时都会回退到一个写死的兜底值，
 * 而需求点名的那几项（击杀阈值 60 / 30、常规战斗最多 3 名敌人、篝火回复 25%、UI 文字不描边）
 * 必须**默认值本身**就对，否则老存档里没有这个键时玩家拿到的还是旧数值。</p>
 *
 * <p>本类放在 {@code com.slaythecraft} 包下，才能读到包内可见的 {@code Config.SPEC} 与各字段。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.ConfigDefaultsCheck
 * </pre>
 */
public final class ConfigDefaultsCheck {

    private ConfigDefaultsCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        check(lines, counters, "常规怪击杀阈值默认 60", Config.REWARD_THRESHOLD_NORMAL.getDefault() == 60, 60,
                Config.REWARD_THRESHOLD_NORMAL.getDefault());
        check(lines, counters, "精英击杀阈值默认 30", Config.REWARD_THRESHOLD_ELITE.getDefault() == 30, 30,
                Config.REWARD_THRESHOLD_ELITE.getDefault());
        check(lines, counters, "BOSS 击杀阈值默认 1", Config.REWARD_THRESHOLD_BOSS.getDefault() == 1, 1,
                Config.REWARD_THRESHOLD_BOSS.getDefault());
        check(lines, counters, "常规战斗最多 3 名敌人（默认）", Config.MAX_ENEMIES.getDefault() == 3, 3,
                Config.MAX_ENEMIES.getDefault());
        check(lines, counters, "精英 / BOSS 战默认 1 只", Config.ELITE_ENCOUNTER_SIZE.getDefault() == 1, 1,
                Config.ELITE_ENCOUNTER_SIZE.getDefault());
        check(lines, counters, "篝火回复默认 25%", Config.CAMPFIRE_HEAL_PERCENT.getDefault() == 25, 25,
                Config.CAMPFIRE_HEAL_PERCENT.getDefault());
        check(lines, counters, "UI 文字默认不描边", Boolean.FALSE.equals(Config.UI_TEXT_SHADOW.getDefault()), false,
                Config.UI_TEXT_SHADOW.getDefault());

        // v0.4.6：商店与删卡的默认值（需求：5 卡 / 3 遗物 / 3 药水 / 40 刷新 ×3 / 删牌每次 +50）
        check(lines, counters, "商店默认卖 5 张卡", Config.SHOP_OFFER_COUNT.getDefault() == 5, 5,
                Config.SHOP_OFFER_COUNT.getDefault());
        check(lines, counters, "商店默认卖 3 件遗物", Config.SHOP_RELIC_COUNT.getDefault() == 3, 3,
                Config.SHOP_RELIC_COUNT.getDefault());
        check(lines, counters, "商店默认卖 3 瓶药水", Config.SHOP_POTION_COUNT.getDefault() == 3, 3,
                Config.SHOP_POTION_COUNT.getDefault());
        check(lines, counters, "刷新默认 40 金币", Config.SHOP_REFRESH_COST.getDefault() == 40, 40,
                Config.SHOP_REFRESH_COST.getDefault());
        check(lines, counters, "刷新默认最多 3 次", Config.SHOP_REFRESH_LIMIT.getDefault() == 3, 3,
                Config.SHOP_REFRESH_LIMIT.getDefault());
        check(lines, counters, "每次删牌默认涨价 50", Config.CARD_DELETE_COST_INCREASE.getDefault() == 50, 50,
                Config.CARD_DELETE_COST_INCREASE.getDefault());

        // v0.4.6.3：初始金币 99（重开会重置回这个数）+ 聊天框默认不再输出系统执行结果与指令
        check(lines, counters, "初始金币默认 99", Config.STARTING_GOLD.getDefault() == 99, 99,
                Config.STARTING_GOLD.getDefault());
        check(lines, counters, "聊天框输出默认关闭（v0.4.6.3 需求）",
                Boolean.FALSE.equals(Config.CHAT_OUTPUT.getDefault()), false, Config.CHAT_OUTPUT.getDefault());

        // v0.4.6.5：聊天框拆成三类开关（需求：指令回执保留，战报与系统提示静音）
        check(lines, counters, "指令回执默认开", Boolean.TRUE.equals(Config.CHAT_COMMAND_FEEDBACK.getDefault()), true,
                Config.CHAT_COMMAND_FEEDBACK.getDefault());
        check(lines, counters, "战斗战报默认关", Boolean.FALSE.equals(Config.CHAT_BATTLE_LOG.getDefault()), false,
                Config.CHAT_BATTLE_LOG.getDefault());
        check(lines, counters, "系统提示默认关", Boolean.FALSE.equals(Config.CHAT_SYSTEM_MESSAGE.getDefault()), false,
                Config.CHAT_SYSTEM_MESSAGE.getDefault());
        check(lines, counters, "聊天框总开关默认关", Boolean.FALSE.equals(Config.CHAT_OUTPUT.getDefault()), false,
                Config.CHAT_OUTPUT.getDefault());

        // 访问器在"配置未加载"时必须回退到同一组数值（战斗逻辑到处直接调访问器）
        if (!Config.SPEC.isLoaded()) {
            check(lines, counters, "未加载配置时：常规阈值回退 60", Config.rewardThresholdNormal() == 60, 60,
                    Config.rewardThresholdNormal());
            check(lines, counters, "未加载配置时：精英阈值回退 30", Config.rewardThresholdElite() == 30, 30,
                    Config.rewardThresholdElite());
            check(lines, counters, "未加载配置时：常规战斗人数回退 3", Config.maxEnemies() == 3, 3,
                    Config.maxEnemies());
            check(lines, counters, "未加载配置时：战斗规则里的敌人上限也是 3",
                    Config.rules().maxEnemies() == 3, 3, Config.rules().maxEnemies());
            check(lines, counters, "未加载配置时：初始金币回退 99", Config.startingGold() == 99, 99,
                    Config.startingGold());
            check(lines, counters, "未加载配置时：聊天框输出回退关闭", !Config.chatOutput(), false,
                    Config.chatOutput());
            // v0.5.0.20：常规战斗掉落遗物的概率（需求指定 10%）
            check(lines, counters, "未加载配置时：常规战斗遗物掉落概率回退 10%",
                    Math.abs(Config.normalBattleRelicChance() - 0.10D) < 1.0E-9D, 0.10D,
                    Config.normalBattleRelicChance());
        } else {
            lines.add("SKIP  配置已加载：跳过「未加载时的回退值」检查");
        }

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
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
