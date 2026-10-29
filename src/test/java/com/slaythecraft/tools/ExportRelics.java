package com.slaythecraft.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;
import com.slaythecraft.combat.core.RelicTrigger;

/**
 * 导出**全部遗物的统计**（v0.5.2.5 一次性工具）。
 *
 * <p>数据全部来自 {@link RelicLibrary}（引擎层、无 MC 依赖），所以这个工具**不需要 MC 类路径**：</p>
 *
 * <pre>
 * java -Dstdout.encoding=UTF-8 -cp "build\classes\java\main;build\classes\java\test" com.slaythecraft.tools.ExportRelics
 * </pre>
 *
 * <p>产出两个文件：{@code docs/relics-export.md}（人看的报告）与 {@code docs/relics-export.csv}
 * （给表格软件）。"来源"一列的信息不在引擎里（初始遗物在 {@code CharacterRegistry}、
 * 事件限定遗物在 {@code QuestionLibrary}），所以在本工具里以**常量表**维护 —— 改那两处时要顺手改这里。</p>
 */
public final class ExportRelics {

    private ExportRelics() {
    }

    /** 初始遗物 → 角色（来源：{@code CharacterRegistry} 的四个 CharacterProfile）。 */
    private static final Map<String, String> STARTING_OWNER = new LinkedHashMap<>();

    /** 事件限定遗物 → 获取它的那个事件（来源：{@code QuestionLibrary} 的选项）。 */
    private static final Map<String, String> EVENT_SOURCE = new LinkedHashMap<>();

    static {
        STARTING_OWNER.put("burning_blood", "铁甲战士");
        STARTING_OWNER.put("ring_of_the_snake", "静默猎手");
        STARTING_OWNER.put("cracked_core", "故障机器人");
        STARTING_OWNER.put("crossbow", "叛逃掠夺者");

        EVENT_SOURCE.put("startled_piglet", "问号事件·屠夫");
        EVENT_SOURCE.put("raid_horn", "问号事件·山羊");
        EVENT_SOURCE.put("dead_coral", "问号事件·流浪商人（200 金币）");
        EVENT_SOURCE.put("heart_of_the_sea", "问号事件·流浪商人（150 金币）");
        EVENT_SOURCE.put("mogu_fillet", "问号事件·嚒菇牛（附带一张【残忍】诅咒）");
        EVENT_SOURCE.put("honey_seasoning", "问号事件·蜂巢（失去 8 点生命）");
    }

    /** 遗物带来的**额外机制**（引擎里以专门方法实现的那些）。 */
    private static String specialOf(String id) {
        List<String> notes = new ArrayList<>();
        int potionSlots = RelicLibrary.extraPotionSlots(List.of(id));
        if (potionSlots > 0) {
            notes.add("药水栏位 +" + potionSlots);
        }
        int maxHp = RelicLibrary.bonusMaxHp(id);
        if (maxHp > 0) {
            notes.add("获得时最大生命 +" + maxHp);
        }
        if (RelicLibrary.grantsMaxHpPerBattle(id)) {
            notes.add("每场战斗结束最大生命 +1");
        }
        if ("mogu_fillet".equals(id)) {
            notes.add("每回合少抽 1 张牌");
        }
        if (RelicLibrary.isPassiveBonus(id)) {
            notes.add("旗标型：开战点亮，真正的数值修正在结算时读旗标");
        }
        return notes.isEmpty() ? "—" : String.join("；", notes);
    }

    /** 这件遗物从哪来（细分：带上角色名 / 事件名）。 */
    private static String sourceOf(Relic relic) {
        String id = relic.id();
        if (RelicLibrary.isStarting(id)) {
            return "角色初始（" + STARTING_OWNER.getOrDefault(id, "?") + "）";
        }
        if (RelicLibrary.isEventOnly(id)) {
            return "事件限定（" + EVENT_SOURCE.getOrDefault(id, "?") + "）";
        }
        // v0.5.3.0：用户给八件新遗物分了来源 —— "只能从商店获取" 与 "商店/藏宝箱都能"
        // v0.5.3.1：来源细化成三条通道（战斗掉落 / 藏宝箱 / 商店），见 RelicLibrary 的几个 *Droppable
        if (RelicLibrary.isShopOnly(id)) {
            return "只能商店";
        }
        if (RelicLibrary.isChestAndShopOnly(id)) {
            return "藏宝箱 / 商店";
        }
        return "战斗掉落 / 藏宝箱 / 商店";
    }

    /** 来源的**大类**（统计用）。 */
    private static String sourceGroup(Relic relic) {
        String id = relic.id();
        if (RelicLibrary.isStarting(id)) {
            return "角色初始";
        }
        if (RelicLibrary.isEventOnly(id)) {
            return "事件限定";
        }
        if (RelicLibrary.isShopOnly(id)) {
            return "只能商店";
        }
        if (RelicLibrary.isChestAndShopOnly(id)) {
            return "藏宝箱 / 商店";
        }
        return "战斗掉落 / 藏宝箱 / 商店";
    }

    /** 商店售价（没给单价的返回 {@code null}，表格里显示「—」）。 */
    private static String priceOf(String id) {
        return RelicLibrary.hasPrice(id) ? String.valueOf(RelicLibrary.priceOf(id)) : "—";
    }

    private static String triggersOf(Relic relic) {
        if (relic.triggers().isEmpty()) {
            return "—";
        }
        List<String> labels = new ArrayList<>();
        for (RelicTrigger trigger : RelicTrigger.values()) {
            if (relic.firesOn(trigger)) {
                labels.add(trigger.label());
            }
        }
        return String.join(" / ", labels);
    }

    /** 描述里可能带风味文案（换行分隔），导出时换成一行。 */
    private static String oneLine(String description) {
        return description.replace("\r", "").replace("\n", " ／ ");
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    public static void main(String[] args) throws Exception {
        List<Relic> relics = RelicLibrary.all();

        StringBuilder md = new StringBuilder();
        md.append("# 遗物全表（自动导出）\n\n");
        md.append("> 由 `com.slaythecraft.tools.ExportRelics` 从 `RelicLibrary` **直接导出**，")
                .append("所以和代码永远一致；改完遗物重跑一次即可。\n>\n");
        md.append("> 复现命令（**不需要 MC 类路径**）：\n>\n");
        md.append("> ```powershell\n");
        md.append("> .\\gradlew.bat compileTestJava --offline\n");
        md.append("> java \"-Dstdout.encoding=UTF-8\" -cp \"build\\classes\\java\\main;build\\classes\\java\\test\" ")
                .append("com.slaythecraft.tools.ExportRelics\n> ```\n>\n");
        md.append("> 产出两个文件：`docs/relics-export.md`（这份报告）与 `docs/relics-export.csv`")
                .append("（给表格软件，**带 UTF-8 BOM**，否则中文 Windows 的 Excel 会按 GBK 读成乱码）。\n\n");

        // ---------------------------------------------------------------- 统计
        Map<String, Integer> bySource = new LinkedHashMap<>();
        bySource.put("角色初始", 0);
        bySource.put("事件限定", 0);
        bySource.put("战斗掉落 / 藏宝箱 / 商店", 0);
        bySource.put("藏宝箱 / 商店", 0);
        bySource.put("只能商店", 0);
        Map<String, Integer> byTrigger = new TreeMap<>();
        List<String> unusedTriggers = new ArrayList<>();
        int passive = 0;
        int multiTrigger = 0;
        int priced = 0;
        int priceSum = 0;
        for (Relic relic : relics) {
            String source = sourceGroup(relic);
            bySource.merge(source, 1, Integer::sum);
            for (RelicTrigger trigger : RelicTrigger.values()) {
                if (relic.firesOn(trigger)) {
                    byTrigger.merge(trigger.label(), 1, Integer::sum);
                }
            }
            if (relic.triggers().isEmpty()) {
                passive++;
            }
            if (relic.triggers().size() > 1) {
                multiTrigger++;
            }
            if (RelicLibrary.hasPrice(relic.id())) {
                priced++;
                priceSum += RelicLibrary.priceOf(relic.id());
            }
        }
        for (RelicTrigger trigger : RelicTrigger.values()) {
            if (!byTrigger.containsKey(trigger.label())) {
                unusedTriggers.add(trigger.label());
            }
        }

        md.append("## 1. 统计\n\n");
        md.append("**遗物总数：").append(relics.size()).append(" 件**\n\n");
        md.append("| 来源 | 数量 |\n| --- | ---: |\n");
        for (Map.Entry<String, Integer> entry : bySource.entrySet()) {
            md.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
        }
        md.append("\n| 触发点 | 挂了它的遗物数 |\n| --- | ---: |\n");
        for (Map.Entry<String, Integer> entry : byTrigger.entrySet()) {
            md.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
        }
        md.append("\n* 不挂任何触发点（纯被动）：**").append(passive).append("** 件\n");
        md.append("* 挂了**多个**触发点：**").append(multiTrigger).append("** 件\n");
        md.append("* 目前**还没有遗物使用**的触发点：");
        md.append(unusedTriggers.isEmpty() ? "（无，六类都用上了）" : String.join("、", unusedTriggers));
        md.append("\n\n> 说明：一件遗物可以挂**多个**触发点，所以触发点那一列的合计会大于总数。\n");
        md.append("\n**商店售价（v0.5.3.1 / v0.5.3.2，用户逐件给定）**：").append(priced).append(" 件有单价，")
                .append("区间 195~240 金币，均价 ").append(priced == 0 ? 0 : priceSum / priced)
                .append(" 金币。没有单价的只有**初始遗物**与**事件限定遗物** —— 它们根本不上架，")
                .append("所以「商店里能出现的每一件遗物都有明确单价」，")
                .append("配置统一价 `shopPriceRelic`（默认 150）只剩兜底作用。\n");
        md.append("\n**来源通道（v0.5.3.1）**：`战斗掉落` = 精英 / BOSS 必掉 + 常规战斗 10% 那一档；")
                .append("`藏宝箱` = 副手收纳包 + 主手镐右键陷阱箱；`商店` = 卡牌大师的遗物货架。\n");
        md.append("\n**带额外机制的遗物**：属性类加成不走触发点，由引擎的专门方法实现 —— ")
                .append("药水栏位 +2（【药水腰带】）、最大生命 +14（【嚒咕菲力】）、")
                .append("每场战斗结束最大生命 +1（【蜂蜜佐料】）、每回合少抽 1 张（【嚒咕菲力】）。\n\n");

        // ---------------------------------------------------------------- 全表
        md.append("## 2. 全表\n\n");
        md.append("| # | 名称 | id | 售价 | 触发点 | 来源 | 效果 | 额外机制 |\n");
        md.append("| ---: | --- | --- | ---: | --- | --- | --- | --- |\n");
        int index = 1;
        for (Relic relic : relics) {
            md.append("| ").append(index++).append(" | ").append(relic.name()).append(" | `")
                    .append(relic.id()).append("` | ").append(priceOf(relic.id())).append(" | ")
                    .append(triggersOf(relic)).append(" | ")
                    .append(sourceOf(relic)).append(" | ").append(oneLine(relic.description()))
                    .append(" | ").append(specialOf(relic.id())).append(" |\n");
        }

        md.append("\n## 3. 按来源分组\n\n");
        for (String group : new String[] {"角色初始", "事件限定", "战斗掉落 / 藏宝箱 / 商店",
                "藏宝箱 / 商店", "只能商店"}) {
            md.append("### ").append(group).append("\n\n");
            for (Relic relic : relics) {
                if (group.equals(sourceGroup(relic))) {
                    md.append("* 【").append(relic.name()).append("】`").append(relic.id()).append("`（")
                            .append(priceOf(relic.id())).append(" 金币） —— ")
                            .append(oneLine(relic.description()))
                            .append("　→　").append(sourceOf(relic)).append("\n");
                }
            }
            md.append("\n");
        }

        md.append("## 4. 已知偏差\n\n");
        md.append("* 遗物**没有任何美术资源**：战斗左侧遗物栏用程序化符文兜底，")
                .append("丢一张 `assets/stc/textures/relic/<id>.png` 就会变成真图标。\n");
        md.append("* 遗物表是**代码注册表**（`RelicLibrary`），不是数据包注册表 —— ")
                .append("整合包要自定义得等 A6 那一类改造。\n");
        md.append("* 遗物**没有稀有度字段**，所以商店/掉落是等概率随机（`randomUnowned`）。\n");
        md.append("* 商店里能出现的遗物（14 件）**每一件都有明确单价**；只有初始遗物与事件限定遗物没有价，")
                .append("而它们根本不上架。\n");
        md.append("* 【袭击号角】【失活的珊瑚】【海洋之心】是**旗标型**：效果只负责在自己身上点亮一个旗标，")
                .append("真正的数值修正在 `previewDamage` 等结算处读取；另外 `applyPassiveBonuses` 里")
                .append("还有一份「开战前直接按 id 点亮」的等价入口（离线自检用）。\n");

        Path mdPath = Path.of("docs", "relics-export.md");
        Files.createDirectories(mdPath.getParent());
        Files.write(mdPath, md.toString().getBytes(StandardCharsets.UTF_8));

        // ---------------------------------------------------------------- CSV
        StringBuilder csv = new StringBuilder();
        csv.append("序号,名称,id,售价,触发点,来源,效果,额外机制\n");
        index = 1;
        for (Relic relic : relics) {
            csv.append(index++).append(',')
                    .append(csv(relic.name())).append(',')
                    .append(csv(relic.id())).append(',')
                    .append(csv(priceOf(relic.id()))).append(',')
                    .append(csv(triggersOf(relic))).append(',')
                    .append(csv(sourceOf(relic))).append(',')
                    .append(csv(oneLine(relic.description()))).append(',')
                    .append(csv(specialOf(relic.id()))).append('\n');
        }
        Path csvPath = Path.of("docs", "relics-export.csv");
        // CSV 带 UTF-8 BOM：中文 Windows 上的 Excel / WPS 默认按 GBK 读无 BOM 的 CSV，中文会变乱码。
        // （Java 源码不能带 BOM，但导出文件带 BOM 恰恰是为了让表格软件读对。）
        Files.write(csvPath, ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8));

        System.out.println("遗物总数：" + relics.size());
        for (Map.Entry<String, Integer> entry : bySource.entrySet()) {
            System.out.println("  " + entry.getKey() + "：" + entry.getValue());
        }
        System.out.println("已写入 " + mdPath.toAbsolutePath());
        System.out.println("已写入 " + csvPath.toAbsolutePath());
    }
}
