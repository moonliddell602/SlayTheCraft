package com.slaythecraft.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/**
 * 配方文件的结构校验（v0.4.6.4）。
 *
 * <p>原因：{@code data/stc/recipe/*.json} 是**纯数据**，编译器不管、`runData` 也不一定会在
 * 我们关心的范围内报错 —— 写错一个键名（例如把 {@code pattern} 打成 {@code patterns}）只有在
 * 游戏里打开工作台才会发现。这里按原版 1.21.1 的格式逐条检查：</p>
 *
 * <ul>
 *   <li>顶层必须有 {@code type} 与 {@code result}，且 {@code result.id} 是本模组的物品；</li>
 *   <li>{@code crafting_shaped} 必须有 {@code pattern}（1–3 行、每行等宽、最多 3 列）与 {@code key}；</li>
 *   <li>{@code pattern} 里出现的每个字符都必须在 {@code key} 里有定义，{@code key} 里的每个字符
 *       也都要真的用到（否则多半是打错了字符）；</li>
 *   <li>每个材料的 {@code item} / {@code tag} 必须是合法的 {@link ResourceLocation}，
 *       而且两种写法不能同时出现。</li>
 * </ul>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.data.RecipeCheck
 * </pre>
 */
public final class RecipeCheck {

    /** 配方目录。 */
    private static final Path RECIPE_DIR = Path.of("src", "main", "resources", "data", "stc", "recipe");

    private RecipeCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(RECIPE_DIR)) {
            stream.filter(path -> path.toString().endsWith(".json")).sorted().forEach(files::add);
        } catch (IOException e) {
            check(lines, counters, "配方目录存在：" + RECIPE_DIR, false, "存在", e.getClass().getSimpleName());
            report(lines, counters);
            return;
        }
        check(lines, counters, "配方目录里有配方文件", !files.isEmpty(), true, files.size());

        // v0.4.6.3 的两个配方必须都在（需求点名的合成方式）
        Set<String> names = new java.util.LinkedHashSet<>();
        for (Path file : files) {
            names.add(file.getFileName().toString());
        }
        check(lines, counters, "尖塔入口配方在册（spire_gate.json）", names.contains("spire_gate.json"), true,
                names);
        check(lines, counters, "战斗军旗配方在册（battle_standard.json）", names.contains("battle_standard.json"),
                true, names);

        for (Path file : files) {
            inspect(file, lines, counters);
        }

        report(lines, counters);
    }

    private static void inspect(Path file, List<String> lines, int[] counters) {
        String name = file.getFileName().toString();
        JsonObject root = read(file, lines, counters);
        if (root == null) {
            return;
        }
        String type = string(root, "type");
        check(lines, counters, name + "：声明了 type", type != null, "非空", type);
        if (type == null) {
            return;
        }

        // ---- 产物
        JsonObject result = root.has("result") && root.get("result").isJsonObject()
                ? root.getAsJsonObject("result") : null;
        check(lines, counters, name + "：有 result 对象", result != null, "对象", root.get("result"));
        if (result != null) {
            String id = string(result, "id");
            check(lines, counters, name + "：result.id 合法", ResourceLocation.tryParse(id == null ? "" : id) != null,
                    "合法的资源位置", id);
            check(lines, counters, name + "：result.id 属于本模组", id != null && id.startsWith("stc:"), "stc:…", id);
            int count = result.has("count") ? result.get("count").getAsInt() : 1;
            check(lines, counters, name + "：result.count 在 1–64", count >= 1 && count <= 64, "1..64", count);
        }

        if (!"minecraft:crafting_shaped".equals(type)) {
            // 目前只有 shaped 配方；将来加了别的类型就在这里放行
            lines.add("SKIP  " + name + "：类型 " + type + " 暂不检查形状");
            return;
        }

        // ---- pattern
        JsonArray pattern = root.has("pattern") && root.get("pattern").isJsonArray()
                ? root.getAsJsonArray("pattern") : null;
        check(lines, counters, name + "：crafting_shaped 必须有 pattern 数组", pattern != null, "数组",
                root.get("pattern"));
        if (pattern == null) {
            return;
        }
        int rows = pattern.size();
        check(lines, counters, name + "：pattern 有 1–3 行", rows >= 1 && rows <= 3, "1..3", rows);
        Map<String, Integer> used = new LinkedHashMap<>();
        boolean widthsOk = true;
        int width = rows == 0 ? 0 : pattern.get(0).getAsString().length();
        for (JsonElement row : pattern) {
            String text = row.getAsString();
            widthsOk &= text.length() == width && width <= 3 && !text.isEmpty();
            for (int i = 0; i < text.length(); i++) {
                char symbol = text.charAt(i);
                if (symbol != ' ') {
                    used.merge(String.valueOf(symbol), 1, Integer::sum);
                }
            }
        }
        check(lines, counters, name + "：pattern 每行等宽且不超过 3 列", widthsOk, "等宽、≤3", width);

        // ---- key
        JsonObject key = root.has("key") && root.get("key").isJsonObject() ? root.getAsJsonObject("key") : null;
        check(lines, counters, name + "：有 key 对象", key != null, "对象", root.get("key"));
        if (key == null) {
            return;
        }
        boolean allDefined = true;
        boolean keyUsed = true;
        boolean ingredientsValid = true;
        for (String symbol : used.keySet()) {
            allDefined &= key.has(symbol);
        }
        for (String symbol : key.keySet()) {
            keyUsed &= used.containsKey(symbol);
        }
        for (Map.Entry<String, JsonElement> entry : key.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                ingredientsValid = false;
                continue;
            }
            JsonObject ingredient = entry.getValue().getAsJsonObject();
            boolean hasItem = ingredient.has("item");
            boolean hasTag = ingredient.has("tag");
            // 原版两种写法二选一，不能同时出现
            if (hasItem == hasTag) {
                ingredientsValid = false;
                continue;
            }
            String value = hasItem ? ingredient.get("item").getAsString() : ingredient.get("tag").getAsString();
            if (ResourceLocation.tryParse(value) == null) {
                ingredientsValid = false;
            }
        }
        check(lines, counters, name + "：pattern 里的字符都在 key 里定义", allDefined, true, key.keySet());
        check(lines, counters, name + "：key 里的字符都被 pattern 用到", keyUsed, true, used.keySet());
        check(lines, counters, name + "：每个材料都是合法且唯一的 item / tag 写法", ingredientsValid, true, false);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static JsonObject read(Path file, List<String> lines, int[] counters) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            check(lines, counters, file.getFileName() + "：是合法 JSON", true, true, true);
            return root;
        } catch (IOException | RuntimeException e) {
            check(lines, counters, file.getFileName() + "：能被解析（" + e + "）", false, "合法 JSON",
                    e.getClass().getSimpleName());
            return null;
        }
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
