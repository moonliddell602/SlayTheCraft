import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardKeyword;
import com.slaythecraft.combat.core.CardLibrary;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出**全部卡牌 id 表**（v0.5.5.4，给卡图绑定用）。
 *
 * <p>用法（由 {@code tools/import-card-art.ps1 -List} 调用）：</p>
 *
 * <pre>
 * java -Dstdout.encoding=UTF-8 -cp &lt;engine classes&gt; ExportCardIds &lt;输出目录&gt;
 * </pre>
 *
 * <p>产出两份：</p>
 * <ul>
 *   <li>{@code card-art-checklist.md} —— 分组可读版（角色 / 品质，带目标路径与标记）；</li>
 *   <li>{@code card-art-checklist.csv} —— 机器可读版（id,卡名,分组,类型,品质,费用,flags,贴图路径）。</li>
 * </ul>
 *
 * <p><b>贴图按"卡牌 id"绑定</b>（{@code textures/card/<id>.png}），基础版与强化版**共用同一张图**，
 * 所以一个 id 只需要一个文件；<b>生成牌</b>（小刀 / 弩箭射击 / 魔术技巧的费用档…）也各有 id，
 * 想让它们长得像"母牌"，就把同一张图复制成这些文件名。</p>
 */
public final class ExportCardIds {

    private ExportCardIds() {
    }

    /** 分组顺序（角色在前，杂项在后）。 */
    private static final Map<String, String> GROUP_LABELS = new LinkedHashMap<>();

    static {
        GROUP_LABELS.put("stc:ironclad", "铁甲战士");
        GROUP_LABELS.put("stc:silent", "静默猎手");
        GROUP_LABELS.put("stc:defect", "故障机器人（机宝）");
        GROUP_LABELS.put("stc:pillager", "叛逃掠夺者");
        GROUP_LABELS.put("colorless", "无色牌");
        GROUP_LABELS.put("basic", "基础牌（打击 / 格挡）");
        GROUP_LABELS.put("status", "状态牌");
        GROUP_LABELS.put("curse", "诅咒牌");
        GROUP_LABELS.put("generated", "生成牌（只能被遗物 / 卡牌生成，不进任何卡池）");
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        List<Card> cards = new ArrayList<>(CardLibrary.all());
        cards.sort(Comparator.comparing(Card::id));

        Path dir = Path.of(args.length > 0 ? args[0] : "assets/cards");
        Files.createDirectories(dir);
        Path markdown = dir.resolve("card-art-checklist.md");
        Path csv = dir.resolve("card-art-checklist.csv");
        Files.writeString(markdown, markdown(cards), StandardCharsets.UTF_8);
        Files.writeString(csv, csv(cards), StandardCharsets.UTF_8);

        int generated = 0;
        int upgradable = 0;
        for (Card card : cards) {
            if (CardLibrary.isGenerated(card.id())) {
                generated++;
            }
            if (CardLibrary.hasUpgrade(card.id())) {
                upgradable++;
            }
        }
        out.println("cards=" + cards.size());
        out.println("generated=" + generated);
        out.println("upgradable=" + upgradable + " (base and upgraded share ONE image)");
        out.println("checklist=" + markdown.toAbsolutePath());
        out.println("csv=" + csv.toAbsolutePath());
        // 逐行输出（id + 名字 + 分组），方便脚本 / 管道直接消费
        for (Card card : cards) {
            out.println(card.id() + "\t" + card.name() + "\t" + group(card));
        }
    }

    private static String group(Card card) {
        if (CardLibrary.isBasic(card.id())) {
            return "basic";
        }
        if (CardLibrary.isGenerated(card.id())) {
            return "generated";
        }
        if (card.type() == com.slaythecraft.combat.core.CardType.STATUS) {
            return "status";
        }
        if (card.type() == com.slaythecraft.combat.core.CardType.CURSE) {
            return "curse";
        }
        return card.character() == null ? "colorless" : card.character();
    }

    private static String flags(Card card) {
        StringBuilder sb = new StringBuilder();
        if (CardLibrary.isXCost(card.id())) {
            sb.append("X费 ");
        }
        if (CardLibrary.isGenerated(card.id())) {
            sb.append("生成牌 ");
        }
        if (CardLibrary.isMultiplayerStyle(card.id())) {
            sb.append("多人玩法向 ");
        }
        if (CardLibrary.hasUpgrade(card.id())) {
            int max = CardLibrary.maxUpgrades(card.id());
            sb.append(max >= Integer.MAX_VALUE ? "可强化(无限) " : max > 1 ? "可强化(" + max + "档) " : "可强化 ");
        }
        if (!card.keywords().isEmpty()) {
            StringBuilder kw = new StringBuilder();
            for (CardKeyword keyword : card.keywords()) {
                if (kw.length() > 0) {
                    kw.append('/');
                }
                kw.append(keyword.label());
            }
            sb.append('[').append(kw).append("] ");
        }
        return sb.toString().trim();
    }

    private static String artPath(Card card) {
        return "src/main/resources/assets/stc/textures/card/" + card.id() + ".png";
    }

    private static String markdown(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 卡面贴图 id 总表（自动生成，共 ").append(cards.size()).append(" 个 id）\n\n");
        sb.append("> 由 `powershell -ExecutionPolicy Bypass -File tools\\import-card-art.ps1 -List` 生成；\n");
        sb.append("> 改了卡池之后重跑一次即可。机器可读版：[`card-art-checklist.csv`](card-art-checklist.csv)\n\n");
        sb.append("## 怎么用\n\n");
        sb.append("1. 图片按 **卡牌 id** 命名：`<id>.png`，放进 `assets/cards/`；\n");
        sb.append("2. 跑 `powershell -ExecutionPolicy Bypass -File tools\\import-card-art.ps1 -Import`\n");
        sb.append("   —— 批量复制到 `src/main/resources/assets/stc/textures/card/` 并报告还缺哪些；\n");
        sb.append("3. 重新构建（`.\\gradlew.bat build --offline`）。\n\n");
        sb.append("**注意**：\n\n");
        sb.append("* **强化版与基础版共用一张图**（同一个 id），所以只需要一个文件；\n");
        sb.append("* **生成牌**（小刀 / 弩箭射击 / 魔术技巧的费用档…）也按各自的 id 取图：\n");
        sb.append("  想让它们长得像母牌，就把母牌的图**复制成这些文件名**（例：`shiv.png`、`bolt_shot.png`）；\n");
        sb.append("* 缺图不会崩：插画位画成占位框（灰底 + 「图」字）。\n\n");
        sb.append("### 生成牌的共图建议（省得单独画）\n\n");
        sb.append("| 生成牌 id | 建议直接复制哪张图 |\n| --- | --- |\n");
        sb.append("| `magic_trick_d1` … `magic_trick_d10` | `magic_trick.png`（同一个「魔术技巧！」，只是费用档不同） |\n");
        sb.append("| `modded_boost` / `modded_boost_plus` / `modded_boost_2` / `modded_boost_2_plus` | `modded.png`（模组改造的费用档） |\n");
        sb.append("| `adaptive_strike_free` | `adaptive_strike.png`（适应打击的 0 费复制品） |\n");
        sb.append("| `shiv`（小刀） | 自己一张（它是独立卡牌，虽然由卡牌生成） |\n");
        sb.append("| `bolt_shot`（弩箭射击） | 自己一张（同上） |\n\n");
        sb.append("> 分组按**类型优先**：属于角色的状态牌（如叛逃掠夺者的『伤口』）会归到「状态牌」一组，\n");
        sb.append("> 所以「叛逃掠夺者 33 + 状态牌 9」与文档里的「掠夺者 34 + 状态牌 8」是同一批卡。\n\n");
        sb.append("## 分组总览\n\n");
        sb.append("| 分组 | id 数 |\n| --- | ---: |\n");
        Map<String, List<Card>> grouped = group(cards);
        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append("| ").append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey())).append(" | ")
                    .append(entry.getValue().size()).append(" |\n");
        }
        sb.append("| **合计** | **").append(cards.size()).append("** |\n");

        for (Map.Entry<String, List<Card>> entry : grouped.entrySet()) {
            sb.append("\n## ").append(GROUP_LABELS.getOrDefault(entry.getKey(), entry.getKey()))
                    .append("（").append(entry.getValue().size()).append("）\n\n");
            sb.append("| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |\n");
            sb.append("| --- | --- | --- | --- | ---: | --- | --- |\n");
            for (Card card : entry.getValue()) {
                sb.append("| `").append(card.id()).append("` | ").append(card.name()).append(" | ")
                        .append(card.type().label()).append(" | ").append(card.rarity().label()).append(" | ")
                        .append(card.cost()).append(" | ").append(flags(card).replace("|", "/"))
                        .append(" | `").append(artPath(card)).append("` |\n");
            }
        }
        return sb.toString();
    }

    private static Map<String, List<Card>> group(List<Card> cards) {
        Map<String, List<Card>> grouped = new LinkedHashMap<>();
        for (String key : GROUP_LABELS.keySet()) {
            grouped.put(key, new ArrayList<>());
        }
        for (Card card : cards) {
            grouped.computeIfAbsent(group(card), k -> new ArrayList<>()).add(card);
        }
        grouped.values().removeIf(List::isEmpty);
        // 组内按 品质（普通 → 罕见 → 稀有 → 金）→ 费用 → id 排
        for (List<Card> list : grouped.values()) {
            list.sort(Comparator.comparingInt((Card card) -> card.rarity().ordinal())
                    .thenComparingInt(Card::cost)
                    .thenComparing(Card::id));
        }
        return grouped;
    }

    private static String csv(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,group,type,rarity,cost,generated,x_cost,multiplayer,upgradable,art_path\n");
        for (Card card : cards) {
            sb.append(quote(card.id())).append(',')
                    .append(quote(card.name())).append(',')
                    .append(quote(group(card))).append(',')
                    .append(quote(card.type().label())).append(',')
                    .append(quote(card.rarity().label())).append(',')
                    .append(card.cost()).append(',')
                    .append(CardLibrary.isGenerated(card.id())).append(',')
                    .append(CardLibrary.isXCost(card.id())).append(',')
                    .append(CardLibrary.isMultiplayerStyle(card.id())).append(',')
                    .append(CardLibrary.hasUpgrade(card.id())).append(',')
                    .append(quote(artPath(card))).append('\n');
        }
        return sb.toString();
    }

    private static String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
