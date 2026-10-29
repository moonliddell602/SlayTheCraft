package com.slaythecraft.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 模组元数据（{@code META-INF/neoforge.mods.toml}）校验（v0.5.1.1）。
 *
 * <p>为什么需要：模组列表里显示的**作者 / 名称 / 描述**都来自这个文件，而它是**纯文本、编译器不管**
 * —— 改错了（写坏 TOML、名字被改回去、编码变成 BOM）只有在游戏里打开模组列表才会发现。</p>
 *
 * <p>这里盯四件事：</p>
 * <ol>
 *   <li>文件存在，且是**合法 UTF-8 且不带 BOM**（BOM 会让 TOML 解析器把第一个键读坏）；</li>
 *   <li>{@code authors} 与期望一致（v0.5.1.0 需求：作者改名）；</li>
 *   <li>必备字段（modId / version / displayName）都在，且 modId 与工程一致；</li>
 *   <li>{@code logoFile} 指向的文件真的在资源目录里。</li>
 * </ol>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.data.ModMetadataCheck
 * </pre>
 */
public final class ModMetadataCheck {

    private static final Path TOML = Path.of("src", "main", "resources", "META-INF", "neoforge.mods.toml");

    /**
     * 期望的作者字符串（v0.5.1.0 需求）。
     *
     * <p>需求原文："把作者名称改成：电脑玩家里德尔（如果不支持中文/在 en_us 语言内，写入 BotPlayerLiddell）"。
     * {@code authors} 是**原样显示的字符串**、不经过语言文件、没法按语言切换，
     * 所以把两个名字都写上（中文名在前、拉丁名在括号里）。</p>
     */
    private static final String EXPECTED_AUTHOR = "电脑玩家里德尔 (BotPlayerLiddell)";

    /** 旧作者名：绝不能留在文件里（防止改回去 / 漏改）。 */
    private static final String OLD_AUTHOR = "SlayTheCraft";

    private ModMetadataCheck() {
    }

    public static void main(String[] args) throws IOException {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        check(lines, counters, "neoforge.mods.toml 存在", Files.isRegularFile(TOML), "存在", "缺失");
        if (!Files.isRegularFile(TOML)) {
            report(lines, counters);
            return;
        }

        byte[] bytes = Files.readAllBytes(TOML);
        // ---- 编码：必须是 UTF-8 且**不带 BOM**
        boolean hasBom = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
        check(lines, counters, "mods.toml 不带 BOM（BOM 会让第一个键读坏）", !hasBom, "无 BOM", "有 BOM");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            check(lines, counters, "mods.toml 是合法 UTF-8", true, "合法", "非法");
        } catch (RuntimeException e) {
            check(lines, counters, "mods.toml 是合法 UTF-8（" + e.getMessage() + "）", false, "合法", "非法");
            report(lines, counters);
            return;
        }

        // ---- 作者
        String author = value(text, "authors");
        check(lines, counters, "作者已改为需求指定的名字", EXPECTED_AUTHOR.equals(author), EXPECTED_AUTHOR, author);
        check(lines, counters, "作者里包含中文名【电脑玩家里德尔】", author.contains("电脑玩家里德尔"),
                "含中文名", author);
        check(lines, counters, "作者里同时保留了拉丁名 BotPlayerLiddell（en_us 环境下也能认）",
                author.contains("BotPlayerLiddell"), "含拉丁名", author);
        check(lines, counters, "旧作者名 SlayTheCraft 没有残留在 authors 里", !author.contains(OLD_AUTHOR),
                "无残留", author);

        // ---- 必备字段
        check(lines, counters, "modId 占位符写在文件里（构建时由 gradle 注入 stc）",
                text.contains("modId=\"${mod_id}\""), "有占位符", "没有");
        check(lines, counters, "version 占位符写在文件里", text.contains("version=\"${mod_version}\""),
                "有占位符", "没有");
        check(lines, counters, "displayName 占位符写在文件里", text.contains("displayName=\"${mod_name}\""),
                "有占位符", "没有");

        // ---- 图标文件真的在
        String logo = value(text, "logoFile");
        check(lines, counters, "logoFile 指向的文件存在（" + logo + "）",
                !logo.isEmpty() && Files.isRegularFile(Path.of("src", "main", "resources", logo)),
                "存在", "缺失");

        report(lines, counters);
    }

    /** 取 `key="value"` 形式的第一个值（不解析完整 TOML：这里只需读几个简单键）。 */
    private static String value(String text, String key) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || !trimmed.startsWith(key + "=")) {
                continue;
            }
            String rest = trimmed.substring(key.length() + 1).trim();
            if (rest.startsWith("\"") && rest.length() > 1) {
                int end = rest.indexOf('"', 1);
                if (end > 0) {
                    return rest.substring(1, end);
                }
            }
        }
        return "(缺失)";
    }

    private static void report(List<String> lines, int[] counters) {
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
