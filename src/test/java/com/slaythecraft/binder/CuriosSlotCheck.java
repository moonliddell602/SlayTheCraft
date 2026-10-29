package com.slaythecraft.binder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Curios 集成的静态校验（v0.2.4 修复回归守卫）。
 *
 * <p>背景：Curios 9.x 的槽位是**数据包**定义的（{@code data/<modid>/curios/slots/<槽id>.json}），
 * 物品能不能放进某个槽由**物品标签** {@code curios:<槽id>} 决定，而"物品有没有饰品能力"
 * 由 {@code CuriosApi.registerCurio(...)}（或实现 {@code ICurioItem}）决定。</p>
 *
 * <p>实测反馈："无法打开页面，显示必须携带才能使用" —— 根因就是只写了标签、没注册能力，
 * 收纳包根本放不进饰品槽。这个自检把三样东西一起钉死：</p>
 * <ol>
 *   <li>槽位定义 JSON 存在且合法（id 与代码里的 {@link BinderResolver#BINDER_SLOT} 一致）；</li>
 *   <li>物品标签 {@code curios:<槽id>} 存在，且指向的正是注册的物品 id；</li>
 *   <li>原有的 {@code curios:charm} 标签仍然包含收纳包（护符槽也能用）。</li>
 * </ol>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.binder.CuriosSlotCheck
 * </pre>
 */
public final class CuriosSlotCheck {

    private CuriosSlotCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        String slotId = BinderResolver.BINDER_SLOT;
        check(lines, counters, "槽位 id 是稳定的 'binder'", "binder".equals(slotId), "binder", slotId);

        // ---- 槽位定义（data/stc/curios/slots/binder.json）
        Path slotPath = Path.of("src", "main", "resources", "data", "stc", "curios", "slots", slotId + ".json");
        JsonObject slot = readJson(slotPath, lines, counters, "槽位定义");
        if (slot != null) {
            Set<String> validators = new LinkedHashSet<>();
            JsonElement validatorsElement = slot.get("validators");
            if (validatorsElement != null && validatorsElement.isJsonArray()) {
                for (JsonElement entry : validatorsElement.getAsJsonArray()) {
                    validators.add(entry.getAsString());
                }
            }
            check(lines, counters, "槽位用 curios:tag 校验器（决定它读取 curios:<槽id> 物品标签）",
                    validators.contains("curios:tag"), true, validators);
            check(lines, counters, "槽位至少 1 格", slot.has("size") && slot.get("size").getAsInt() >= 1, 1,
                    slot.has("size") ? slot.get("size").getAsInt() : "缺失");
            check(lines, counters, "槽位有图标（Curios 会去找 assets/<ns>/textures/<icon>.png）",
                    slot.has("icon") && !slot.get("icon").getAsString().isBlank(), true,
                    slot.has("icon") ? slot.get("icon").getAsString() : "缺失");
            check(lines, counters, "槽位有排序（决定在饰品栏里的位置）", slot.has("order"), true,
                    slot.has("order") ? slot.get("order").getAsInt() : "缺失");
        }

        // ---- 物品标签（data/curios/tags/item/<槽id>.json 与 charm.json）
        // 这里不去引用 SlayTheCraft.CARD_BINDER（会触发 FML 类加载，裸 JVM 跑不了），
        // 改成直接读源码里的注册名，等价且更稳：注册名一改这里就会红。
        String itemId = "stc:card_binder";
        String mainSource = readText(Path.of("src", "main", "java", "com", "slaythecraft", "SlayTheCraft.java"),
                lines, counters, "主类源码");
        check(lines, counters, "主类里注册的物品名与标签一致（ITEMS.register(\"card_binder\"）",
                mainSource != null && mainSource.contains("register(\"card_binder\""), true,
                mainSource == null ? "读不到" : "已检查");
        check(lines, counters, "主类在 RegisterCapabilitiesEvent 里注册了 Curios 饰品能力",
                mainSource != null && mainSource.contains("RegisterCapabilitiesEvent")
                        && mainSource.contains("registerCuriosItem"), true,
                mainSource == null ? "读不到" : "已检查");

        String resolverSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "binder", "BinderResolver.java"),
                lines, counters, "BinderResolver 源码");
        check(lines, counters, "BinderResolver 里确实调用了 CuriosApi.registerCurio",
                resolverSource != null && resolverSource.contains("CuriosCompat.registerBinder"), true,
                resolverSource == null ? "读不到" : "已检查");
        String compatSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "binder", "CuriosCompat.java"),
                lines, counters, "CuriosCompat 源码");
        check(lines, counters, "CuriosCompat 里确实调用了 CuriosApi.registerCurio",
                compatSource != null && compatSource.contains("CuriosApi.registerCurio"), true,
                compatSource == null ? "读不到" : "已检查");

        Set<String> slotTag = readTagValues("data/curios/tags/item/" + slotId + ".json", lines, counters,
                "收纳包槽标签");
        check(lines, counters, "收纳包在 curios:" + slotId + " 标签里（专属槽可用）",
                slotTag.contains(itemId), true, slotTag);

        Set<String> charmTag = readTagValues("data/curios/tags/item/charm.json", lines, counters, "护符槽标签");
        check(lines, counters, "收纳包仍在 curios:charm 标签里（护符槽也能用）", charmTag.contains(itemId), true,
                charmTag);
        check(lines, counters, "两个标签都不是 replace=true（不会顶掉别家的追加）",
                !replaceTrue("data/curios/tags/item/" + slotId + ".json")
                        && !replaceTrue("data/curios/tags/item/charm.json"), true, false);

        // ---- v0.2.6：触发条件改成"主手手持"
        String triggerSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "binder", "BinderResolver.java"),
                lines, counters, "BinderResolver 源码");
        check(lines, counters, "携带判定用主手/副手手持（v0.2.6）",
                triggerSource != null && triggerSource.contains("getMainHandItem().is(binder)")
                        && triggerSource.contains("getOffhandItem().is(binder)")
                        && triggerSource.contains("holding(player, binder)"), true,
                triggerSource == null ? "读不到" : "已检查");
        check(lines, counters, "未携带提示写明拿到主手/副手",
                triggerSource != null && triggerSource.contains("主手或副手"), true,
                triggerSource == null ? "读不到" : "已检查");
        // ---- v0.4.6.3：需求"如果卡牌收纳包在背包内，也可以允许 B 键打开 UI"
        check(lines, counters, "v0.4.6.3：背包里也算携带（carried 判定含背包）",
                triggerSource != null && triggerSource.contains("public static boolean carried(Player player)")
                        && triggerSource.contains("getInventory().contains("), true,
                triggerSource == null ? "读不到" : "已检查");
        String openerSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "binder", "BinderOpener.java"),
                lines, counters, "BinderOpener 源码");
        check(lines, counters, "v0.4.6.3：打开界面用放宽后的 carried（B 键 / 右键 / 入口按钮同时生效）",
                openerSource != null && openerSource.contains("BinderResolver.carried(player)"), true,
                openerSource == null ? "读不到" : "未接线");
        check(lines, counters, "v0.4.6.3：开战判定仍然用严格的 hasBinder（玩法规则没被放宽）",
                triggerSource != null && triggerSource.contains("public static boolean hasBinder(Player player)"),
                true, triggerSource == null ? "读不到" : "已检查");
        String configSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "Config.java"), lines, counters,
                "Config 源码");
        check(lines, counters, "旧的 requireCurios 已删除（不再强制 Curios 槽）",
                configSource != null && !configSource.contains("REQUIRE_CURIOS"), true,
                configSource == null ? "读不到" : "已检查");

        // ---- v0.2.6/0.2.7 约定：有子页面的引导必须"新建页面"，不在同一页里叠内容
        for (String screen : List.of("CardPickScreen", "InfoPageScreen", "ConfirmPageScreen",
                "BinderRelicsScreen", "BinderProgressScreen")) {
            check(lines, counters, screen + " 作为独立页面存在",
                    Files.isRegularFile(Path.of("src", "main", "java", "com", "slaythecraft", "client",
                            screen + ".java")), true, "缺失");
        }
        check(lines, counters, "旧的同页确认框（BinderCardsScreen）已删除",
                !Files.isRegularFile(Path.of("src", "main", "java", "com", "slaythecraft", "client",
                        "BinderCardsScreen.java")), true, "仍存在");
        String binderHome = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "client", "CardBinderScreen.java"),
                lines, counters, "收纳包主页源码");
        check(lines, counters, "主页用 setScreen 打开子页面（不再同页切视图）",
                binderHome != null && binderHome.contains("setScreen(new CardPickScreen")
                        && binderHome.contains("setScreen(new BinderRelicsScreen")
                        && binderHome.contains("setScreen(new BinderProgressScreen"), true,
                binderHome == null ? "读不到" : "已检查");
        check(lines, counters, "收纳包里只有回收站模式才允许删卡（v0.2.7）",
                binderHome != null && binderHome.contains("Mode.RECYCLER")
                        && binderHome.contains("recycler,"), true,
                binderHome == null ? "读不到" : "已检查");
        String pickSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "client", "CardPickScreen.java"),
                lines, counters, "卡牌列表页源码");
        check(lines, counters, "卡牌详情/删除都是新开页面（InfoPageScreen / ConfirmPageScreen）",
                pickSource != null && pickSource.contains("new InfoPageScreen<>")
                        && pickSource.contains("new ConfirmPageScreen<>"), true,
                pickSource == null ? "读不到" : "已检查");
        String relicSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "client", "BinderRelicsScreen.java"),
                lines, counters, "遗物页源码");
        check(lines, counters, "遗物描述进独立详情页（不再截断）",
                relicSource != null && relicSource.contains("new InfoPageScreen<>")
                        && !relicSource.contains("plainSubstrByWidth(relic.description()"), true,
                relicSource == null ? "读不到" : "已检查");
        String gateSource = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "client", "SpireGateScreen.java"),
                lines, counters, "尖塔入口源码");
        check(lines, counters, "重开确认是独立页面（ConfirmPageScreen）",
                gateSource != null && gateSource.contains("new ConfirmPageScreen<>")
                        && !gateSource.contains("drawRestartConfirm"), true,
                gateSource == null ? "读不到" : "已检查");
        String shopMenu = readText(
                Path.of("src", "main", "java", "com", "slaythecraft", "merchant", "CardShopMenu.java"),
                lines, counters, "卡牌商店菜单源码");
        check(lines, counters, "卡牌商人提供删卡服务（DELETE_CARD_BASE）",
                shopMenu != null && shopMenu.contains("DELETE_CARD_BASE")
                        && shopMenu.contains("CardDeleteService.deleteAt"), true,
                shopMenu == null ? "读不到" : "已检查");

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static Set<String> readTagValues(String relative, List<String> lines, int[] counters, String label) {
        Set<String> values = new LinkedHashSet<>();
        JsonObject root = readJson(Path.of("src", "main", "resources").resolve(relative), lines, counters, label);
        if (root == null) {
            return values;
        }
        JsonElement element = root.get("values");
        if (element == null || !element.isJsonArray()) {
            check(lines, counters, label + " 有 values 数组", false, "数组", String.valueOf(element));
            return values;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            values.add(entry.getAsString());
        }
        return values;
    }

    private static boolean replaceTrue(String relative) {
        JsonObject root = readJson(Path.of("src", "main", "resources").resolve(relative), new ArrayList<>(),
                new int[] {0, 0}, "标签");
        return root != null && root.has("replace") && root.get("replace").getAsBoolean();
    }

    private static JsonObject readJson(Path path, List<String> lines, int[] counters, String label) {
        String text = readText(path, lines, counters, label);
        if (text == null) {
            return null;
        }
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            check(lines, counters, label + " 能被解析（" + e + "）", false, "合法 JSON",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    /** 读源码/资源文件原文（不存在时记一条失败）。 */
    private static String readText(Path path, List<String> lines, int[] counters, String label) {
        if (!Files.isRegularFile(path)) {
            check(lines, counters, label + " 文件存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, label + " 可读（" + e + "）", false, "可读", e.getClass().getSimpleName());
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
