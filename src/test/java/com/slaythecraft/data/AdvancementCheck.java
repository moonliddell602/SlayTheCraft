package com.slaythecraft.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slaythecraft.achievement.AchievementService;

/**
 * 进度（成就）文件的结构与**成就树**校验。
 *
 * <p>为什么需要：{@code data/stc/advancement/**} 是**纯数据**，编译器不管。而原版
 * {@code ServerAdvancementManager} 对反序列化失败的进度只写一行
 * {@code LOGGER.error("Parsing error loading custom advancement {}")} 就**静默丢弃**这条，
 * 之后 {@code AdvancementTree.addAll} 会因为它下面的子节点找不到父节点
 * 打印 {@code "Couldn't load advancements"} 并 {@code break} ——
 * 在游戏里的表现是"根成就还在，其余成就整片消失"，极难排查
 * （v0.5.0.11 就是踩了这个坑）。</p>
 *
 * <p>所以这里**照着 1.21.1 的 codec 定义**逐字段复刻校验，把"文件存在但游戏不认"钉死在自检里：</p>
 *
 * <ul>
 *   <li><b>字段白名单</b>：每个对象只允许 codec 认识的键。原版 codec 用
 *       {@code RecordCodecBuilder}，多写的键会被直接忽略——但换个版本或换成严格解析就是解析失败，
 *       所以宁可报错；</li>
 *   <li><b>精确类型</b>：{@code Advancement.parent} 是 {@code ResourceLocation.CODEC}（必须是
 *       {@code namespace:path}），{@code DisplayInfo.background} 同理，布尔字段必须是布尔而不是字符串；</li>
 *   <li><b>DisplayInfo 必填项</b>：{@code icon} / {@code title} / {@code description} 是
 *       {@code fieldOf}（**必填**），漏了直接解析失败；</li>
 *   <li><b>icon</b>：{@code ItemStack.STRICT_CODEC} 的 {@code id} 必填、
 *       {@code count} 若写必须在 1..99（{@code ExtraCodecs.intRange(1, 99)}），且不允许 {@code minecraft:air}；</li>
 *   <li><b>frame</b>：必须是 {@code task} / {@code goal} / {@code challenge} 之一
 *       （{@code AdvancementType.CODEC} 是枚举 stringResolver，写错会解析失败）；</li>
 *   <li><b>触发器</b>：必须在 1.21.1 的 56 个 {@code CriteriaTriggers} 注册名里
 *       （写错会解析失败并整条丢弃）；</li>
 *   <li>进度文件路径与 id 的换算（{@code data/<ns>/advancement/<path>.json} → {@code <ns>:<path>}）、
 *       {@code parent} 与登记的树结构逐字一致且父节点存在、只有根有 {@code background}、
 *       除【回响形态了 回响形态】外都不是隐藏成就、每个翻译键中英都有。</li>
 * </ul>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.data.AdvancementCheck
 * </pre>
 */
public final class AdvancementCheck {

    /** 进度目录。 */
    private static final Path ADVANCEMENT_DIR = Path.of("src", "main", "resources", "data", "stc", "advancement");
    /** 语言文件目录。 */
    private static final Path LANG_DIR = Path.of("src", "main", "resources", "assets", "stc", "lang");
    /** 唯一的隐藏成就（需求点名的那一条）。 */
    private static final String HIDDEN_ID = "stc:echo_form";

    /** {@code Advancement} 记录认识的顶层键（见 1.21.1 {@code Advancement.CODEC}）。 */
    private static final Set<String> ADVANCEMENT_KEYS = Set.of(
            "parent", "display", "rewards", "criteria", "requirements", "sends_telemetry_event");

    /** {@code DisplayInfo} 认识的键（见 1.21.1 {@code DisplayInfo.CODEC}）。 */
    private static final Set<String> DISPLAY_KEYS = Set.of(
            "icon", "title", "description", "background", "frame", "show_toast", "announce_to_chat", "hidden");

    /** {@code ItemStack.CODEC} 认识的键；{@code STRICT_CODEC} 额外禁止 {@code minecraft:air}。 */
    private static final Set<String> ICON_KEYS = Set.of("id", "count", "components");

    /** {@code AdvancementRewards.CODEC} 认识的键。 */
    private static final Set<String> REWARD_KEYS = Set.of("experience", "loot", "recipes", "function");

    /** {@code AdvancementType.CODEC} 的全部合法值。 */
    private static final Set<String> FRAMES = Set.of("task", "goal", "challenge");

    /**
     * 1.21.1 {@code CriteriaTriggers} 注册的全部 56 个触发器名（从真实源码提取）。
     *
     * <p>写错触发器名 = 反序列化失败 = 整条成就被静默丢弃，所以这里必须是**白名单**。
     * 新增触发器时同步这份表（它比"记得去查原版"可靠）。</p>
     */
    private static final Set<String> TRIGGERS = Set.of(
            "impossible", "player_killed_entity", "entity_killed_player", "enter_block", "inventory_changed",
            "recipe_unlocked", "player_hurt_entity", "entity_hurt_player", "enchanted_item", "filled_bucket",
            "brewed_potion", "construct_beacon", "used_ender_eye", "summoned_entity", "bred_animals", "location",
            "slept_in_bed", "cured_zombie_villager", "villager_trade", "item_durability_changed", "levitation",
            "changed_dimension", "tick", "tame_animal", "placed_block", "consume_item", "effects_changed",
            "used_totem", "nether_travel", "fishing_rod_hooked", "channeled_lightning", "shot_crossbow",
            "killed_by_crossbow", "hero_of_the_village", "voluntary_exile", "slide_down_block",
            "bee_nest_destroyed", "target_hit", "item_used_on_block", "default_block_use", "any_block_use",
            "player_generates_container_loot", "thrown_item_picked_up_by_entity", "thrown_item_picked_up_by_player",
            "player_interacted_with_entity", "started_riding", "lightning_strike", "using_item",
            "fall_from_height", "ride_entity_in_lava", "kill_mob_near_sculk_catalyst", "allay_drop_item_on_block",
            "avoid_vibration", "recipe_crafted", "crafter_recipe_crafted", "fall_after_explosion");

    private AdvancementCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        Map<net.minecraft.resources.ResourceLocation, AchievementService.Entry> byId = new HashMap<>();
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            byId.put(entry.id(), entry);
        }

        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            checkOne(lines, counters, entry, byId);
        }
        checkTree(lines, counters, byId);
        checkTranslations(lines, counters);
        report(lines, counters);
    }

    /** 检查单个进度文件。 */
    private static void checkOne(List<String> lines, int[] counters, AchievementService.Entry entry,
                                 Map<net.minecraft.resources.ResourceLocation, AchievementService.Entry> byId) {
        String name = entry.id().getPath();
        Path file = ADVANCEMENT_DIR.resolve(entry.file() + ".json");
        check(lines, counters, "进度文件存在：" + entry.file() + ".json", Files.isRegularFile(file), "存在", "缺失");
        if (!Files.isRegularFile(file)) {
            return;
        }
        // ---- 文件路径与 id 的换算：data/<ns>/advancement/<path>.json -> <ns>:<path>
        check(lines, counters, name + "：文件路径与登记 id 一致（" + entry.file() + " -> " + entry.id() + "）",
                entry.file().equals(entry.id().getNamespace() + "/" + entry.id().getPath()),
                entry.id().getNamespace() + "/" + entry.id().getPath(), entry.file());
        JsonObject root = read(file, lines, counters);
        if (root == null) {
            return;
        }

        // ---- 顶层字段：白名单 + 必填
        checkNoUnknownKeys(lines, counters, name + " 顶层", root, ADVANCEMENT_KEYS);
        check(lines, counters, name + "：有 criteria 段（codec 必填，缺失=解析失败）", root.has("criteria"), "有", "无");
        if (!root.has("criteria")) {
            return;
        }

        // ---- 前置：与登记的树结构逐字一致，而且父节点存在
        boolean isRoot = entry.parent() == null;
        if (isRoot) {
            check(lines, counters, name + "：根成就**没有** parent 字段", !root.has("parent"), "没有", "有");
            check(lines, counters, name + "：根成就**必须有** display 段（无 display 的根不会生成标签页）",
                    root.has("display"), "有", "无");
        } else {
            check(lines, counters, name + "：有 parent 字段", root.has("parent"), "有", "无");
            String actual = root.has("parent") ? root.get("parent").getAsString() : "(缺失)";
            check(lines, counters, name + "：parent 与登记一致（" + entry.parent() + "）",
                    entry.parent().equals(actual), entry.parent(), actual);
            // ResourceLocation.CODEC：必须是 namespace:path，缺冒号会解析失败
            check(lines, counters, name + "：parent 是合法资源路径（含冒号）", actual.indexOf(':') > 0,
                    "namespace:path", actual);
            check(lines, counters, name + "：父节点在登记表里（指到不存在的父节点会让成就掉下树）",
                    byId.containsKey(net.minecraft.resources.ResourceLocation.parse(entry.parent())),
                    "在表里", "不在表里");
            check(lines, counters, name + "：非根成就**没有** background 字段（背景只属于根）",
                    !root.has("background") && !hasBackground(root), "没有", "有");
        }

        // ---- display 段
        if (!checkDisplay(lines, counters, entry, root, isRoot)) {
            return;
        }

        // ---- criteria 段：每条必须有合法触发器
        checkCriteria(lines, counters, entry, root);

        // ---- 可选段
        if (root.has("requirements")) {
            checkRequirements(lines, counters, name, root.get("requirements"), root.getAsJsonObject("criteria"));
        }
        if (root.has("rewards")) {
            checkNoUnknownKeys(lines, counters, name + " rewards", root.getAsJsonObject("rewards"), REWARD_KEYS);
        }
        if (root.has("sends_telemetry_event")) {
            check(lines, counters, name + "：sends_telemetry_event 是布尔",
                    isBoolean(root.get("sends_telemetry_event")), "布尔",
                    root.get("sends_telemetry_event").getClass().getSimpleName());
        }
    }

    /** display 段：白名单、必填项、精确类型、frame 枚举、icon 细节。 */
    private static boolean checkDisplay(List<String> lines, int[] counters, AchievementService.Entry entry,
                                        JsonObject root, boolean isRoot) {
        String name = entry.id().getPath();
        check(lines, counters, name + "：有 display 段", root.has("display"), "有", "无");
        if (!root.has("display")) {
            return false;
        }
        JsonObject display = root.getAsJsonObject("display");
        checkNoUnknownKeys(lines, counters, name + " display", display, DISPLAY_KEYS);

        // codec 里 icon/title/description 是 fieldOf（必填）
        check(lines, counters, name + "：display.icon 必填（codec fieldOf）", display.has("icon"), "有", "无");
        check(lines, counters, name + "：display.title 必填（codec fieldOf）", display.has("title"), "有", "无");
        check(lines, counters, name + "：display.description 必填（codec fieldOf）",
                display.has("description"), "有", "无");
        if (!display.has("icon") || !display.has("title") || !display.has("description")) {
            return false;
        }

        // ---- icon：ItemStack.STRICT_CODEC
        JsonElement icon = display.get("icon");
        check(lines, counters, name + "：icon 是对象", icon.isJsonObject(), "对象", kind(icon));
        if (icon.isJsonObject()) {
            JsonObject iconObj = icon.getAsJsonObject();
            checkNoUnknownKeys(lines, counters, name + " icon", iconObj, ICON_KEYS);
            check(lines, counters, name + "：icon.id 存在", iconObj.has("id"), "有", "无");
            if (iconObj.has("id")) {
                String itemId = iconObj.get("id").getAsString();
                check(lines, counters, name + "：icon.id 是合法物品 id 且有命名空间",
                        itemId.indexOf(':') > 0, "namespace:path", itemId);
                check(lines, counters, name + "：icon.id 不是 minecraft:air（STRICT_CODEC 拒绝）",
                        !"minecraft:air".equals(itemId), "非 air", itemId);
            }
            if (iconObj.has("count")) {
                // ExtraCodecs.intRange(1, 99)
                int count = iconObj.get("count").getAsInt();
                check(lines, counters, name + "：icon.count 在 1..99（intRange 限制）",
                        count >= 1 && count <= 99, "1..99", count);
            }
        }

        // ---- title / description：ComponentSerialization.CODEC，至少要有 text 或 translate
        checkComponent(lines, counters, name + "：title", display.get("title"));
        checkComponent(lines, counters, name + "：description", display.get("description"));

        // ---- frame：AdvancementType.CODEC 枚举
        if (display.has("frame")) {
            String frame = display.get("frame").getAsString();
            check(lines, counters, name + "：frame 是 task/goal/challenge（写错=解析失败）",
                    FRAMES.contains(frame), "task/goal/challenge", frame);
        }
        // ---- 布尔字段必须是真布尔（写成字符串 "false" 会解析失败）
        for (String key : new String[] {"show_toast", "announce_to_chat", "hidden"}) {
            if (display.has(key)) {
                check(lines, counters, name + "：" + key + " 是布尔", isBoolean(display.get(key)), "布尔",
                        kind(display.get(key)));
            }
        }
        // ---- background 只有根能有，且必须是资源路径
        if (isRoot) {
            String bg = background(root);
            check(lines, counters, name + "：根成就的背景是白色羊毛",
                    AchievementService.BACKGROUND.equals(bg), AchievementService.BACKGROUND, bg);
            if (display.has("background")) {
                check(lines, counters, name + "：background 是资源路径（含冒号）",
                        bg.indexOf(':') > 0, "namespace:path", bg);
            }
        } else {
            check(lines, counters, name + "：display 段里也没有 background",
                    !display.has("background"), "没有", "有");
        }
        // ---- 隐藏标记
        boolean actualHidden = display.has("hidden") && display.get("hidden").getAsBoolean();
        check(lines, counters, name + "：hidden 标记正确（登记 " + entry.hidden() + "）",
                actualHidden == entry.hidden(), entry.hidden(), actualHidden);
        // ---- toast / 聊天栏（v0.5.0.11：根不弹，其余都弹）
        boolean toast = !display.has("show_toast") || display.get("show_toast").getAsBoolean();
        if (isRoot) {
            check(lines, counters, name + "：根不弹 toast（原版根成就都是 show_toast:false）",
                    !toast, false, toast);
        } else {
            check(lines, counters, name + "：这条会弹 toast（show_toast 未关掉）", toast, true, toast);
        }
        return true;
    }

    /** criteria 段：至少一条，每条都能被 Criterion.CODEC 解析（trigger 必填且必须在注册表里）。 */
    private static void checkCriteria(List<String> lines, int[] counters, AchievementService.Entry entry,
                                      JsonObject root) {
        String name = entry.id().getPath();
        JsonObject criteria = root.getAsJsonObject("criteria");
        check(lines, counters, name + "：至少一条 criterion", criteria.size() >= 1, "≥ 1", criteria.size());
        String firstTrigger = "";
        for (String key : criteria.keySet()) {
            JsonElement c = criteria.get(key);
            check(lines, counters, name + "：criterion「" + key + "」是对象", c.isJsonObject(), "对象", kind(c));
            if (!c.isJsonObject()) {
                continue;
            }
            JsonObject criterion = c.getAsJsonObject();
            check(lines, counters, name + "：criterion「" + key + "」有 trigger",
                    criterion.has("trigger"), "有", "无");
            if (!criterion.has("trigger")) {
                continue;
            }
            String trigger = criterion.get("trigger").getAsString();
            if (firstTrigger.isEmpty()) {
                firstTrigger = trigger;
            }
            // CriteriaTriggers 的名字不带命名空间（内部会补 minecraft:），但我们统一写成带前缀的形式
            String bare = trigger.startsWith("minecraft:") ? trigger.substring("minecraft:".length()) : trigger;
            check(lines, counters, name + "：触发器「" + trigger + "」在 1.21.1 注册表里",
                    TRIGGERS.contains(bare), "已注册", "未注册");
        }
        check(lines, counters, name + "：触发器与登记一致（" + entry.trigger() + "）",
                entry.trigger().equals(firstTrigger), entry.trigger(), firstTrigger);
        if (entry.itemId() != null) {
            check(lines, counters, name + "：条件点名了物品 " + entry.itemId(),
                    criteria.toString().contains("\"" + entry.itemId() + "\""), entry.itemId(), "未点名");
        }
    }

    /** requirements：AdvancementRequirements.CODEC 是「数组的数组」。 */
    private static void checkRequirements(List<String> lines, int[] counters, String name, JsonElement req,
                                          JsonObject criteria) {
        check(lines, counters, name + "：requirements 是数组", req.isJsonArray(), "数组", kind(req));
        if (!req.isJsonArray()) {
            return;
        }
        boolean shapes = true;
        boolean names = true;
        int groups = 0;
        for (JsonElement group : req.getAsJsonArray()) {
            groups++;
            if (!group.isJsonArray()) {
                shapes = false;
                continue;
            }
            for (JsonElement item : group.getAsJsonArray()) {
                if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                    shapes = false;
                    continue;
                }
                // 每条 requirements 里点名的 criterion 必须真的存在，否则 validate() 失败
                if (!criteria.has(item.getAsString())) {
                    names = false;
                }
            }
        }
        check(lines, counters, name + "：requirements 每项都是字符串数组", shapes, "数组的数组", "形状不对");
        check(lines, counters, name + "：requirements 只点名已定义的 criterion", names, "都存在", "有未定义的");
        if (groups == 0) {
            check(lines, counters, name + "：requirements 非空（空数组=没有达成路径）", false, "≥1 组", 0);
        }
    }

    /** 文本组件：ComponentSerialization.CODEC 至少需要一个 text / translate / key。 */
    private static void checkComponent(List<String> lines, int[] counters, String label, JsonElement component) {
        check(lines, counters, label + " 是对象或字符串（文本组件）",
                component.isJsonObject() || component.isJsonPrimitive(), "对象/字符串", kind(component));
        if (component.isJsonObject()) {
            JsonObject obj = component.getAsJsonObject();
            check(lines, counters, label + " 有 text / translate / key",
                    obj.has("text") || obj.has("translate") || obj.has("key"), "有", "都没有");
        }
    }

    /** 整棵树：只有一个根、每个非根都能一路走到根（不会出现环或断链）。 */
    private static void checkTree(List<String> lines, int[] counters,
                                  Map<net.minecraft.resources.ResourceLocation, AchievementService.Entry> byId) {
        int roots = 0;
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            if (entry.parent() == null) {
                roots++;
            }
        }
        check(lines, counters, "成就树只有一个根", roots == 1, 1, roots);

        boolean reachable = true;
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            AchievementService.Entry cursor = entry;
            int guard = 0;
            while (cursor.parent() != null && guard++ < AchievementService.ENTRIES.length + 1) {
                cursor = byId.get(net.minecraft.resources.ResourceLocation.parse(cursor.parent()));
                if (cursor == null) {
                    reachable = false;
                    break;
                }
            }
            if (cursor == null || cursor.parent() != null) {
                reachable = false;
            }
        }
        check(lines, counters, "每条成就都能顺着 parent 一路走到根（无断链、无环）", reachable, "都能走到", "有走不到的");

        // 需求点名的前置关系
        check(lines, counters, "前置：尖塔在等着你 ← 方块尖塔（根）",
                "stc:root".equals(parentOf("stc:entered_game", byId)));
        check(lines, counters, "前置：我进塔！← 尖塔在等着你",
                "stc:entered_game".equals(parentOf("stc:spire_gate", byId)));
        check(lines, counters, "前置：！？收纳袋？！← 尖塔在等着你",
                "stc:entered_game".equals(parentOf("stc:card_binder", byId)));
        check(lines, counters, "前置：再玩小卡组找人弄你 ← ！？收纳袋？！",
                "stc:card_binder".equals(parentOf("stc:card_delete", byId)));
        check(lines, counters, "前置：看起来不太聪明 ← 尖塔在等着你",
                "stc:entered_game".equals(parentOf("stc:first_blood", byId)));
        check(lines, counters, "前置：犹豫就会败北 ← 看起来不太聪明",
                "stc:first_blood".equals(parentOf("stc:first_defeat", byId)));
        check(lines, counters, "前置：人形宝可梦出击 ← 尖塔在等着你",
                "stc:entered_game".equals(parentOf("stc:battle_standard", byId)));
        check(lines, counters, "前置：集中冰才对 ← 人形宝可梦出击",
                "stc:battle_standard".equals(parentOf("stc:focus_frost", byId)));
        check(lines, counters, "前置：回响形态了 回响形态 ← 人形宝可梦出击",
                "stc:battle_standard".equals(parentOf("stc:echo_form", byId)));

        // v0.5.0.11 症状②："给出图片的左上角文字才应该为【方块尖塔】"
        // 那行字就是 AdvancementTab.title = 根节点 display.title
        check(lines, counters, "根的文件里 title 指向标签页标题键（" + AchievementService.ROOT_TITLE_KEY + "）",
                AchievementService.ROOT_TITLE_KEY.equals(rootTitleKey()));

        // 隐藏成就恰好一条
        long hidden = 0;
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            if (entry.hidden()) {
                hidden++;
            }
        }
        check(lines, counters, "隐藏成就恰好 1 条（回响形态了 回响形态）", hidden == 1, 1, hidden);
        check(lines, counters, "隐藏的正是【回响形态了 回响形态】（stc:echo_form）",
                isHidden("stc:echo_form", byId));
        check(lines, counters, "一共登记了 10 个成就（1 个标签页根 + 9 条成就）",
                AchievementService.ENTRIES.length == 10, 10, AchievementService.ENTRIES.length);

        // ---- 代码授予路径用的 id 必须都在登记表里
        // （grant() 在找不到进度时只写一条 warn 就返回 false，写错 id 会静默失效）
        for (net.minecraft.resources.ResourceLocation granted : new net.minecraft.resources.ResourceLocation[] {
                AchievementService.ENTERED_GAME, AchievementService.FIRST_BLOOD, AchievementService.FIRST_DEFEAT,
                AchievementService.CARD_DELETE, AchievementService.FOCUS_FROST, AchievementService.ECHO_FORM}) {
            check(lines, counters, "代码授予用的 id 已登记：" + granted, byId.containsKey(granted), "已登记", "未登记");
        }
        // 【尖塔在等着你】必须**不是**根：根在原版里不弹 toast，也不适合当"进入游戏"的提示
        check(lines, counters, "【尖塔在等着你】是子成就而不是根（root 的 parent 为空，它必须有 parent）",
                "stc:root".equals(parentOf("stc:entered_game", byId)));
    }

    /** 读取根文件里 {@code display.title.translate}，它就是屏幕上那行字。 */
    private static String rootTitleKey() {
        Path file = ADVANCEMENT_DIR.resolve("stc/root.json");
        String text = readText(file, new ArrayList<>(), new int[] {0, 0});
        if (text == null) {
            return "(读不到 root.json)";
        }
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        if (!root.has("display")) {
            return "(根没有 display)";
        }
        JsonObject title = root.getAsJsonObject("display").getAsJsonObject("title");
        return title != null && title.has("translate") ? title.get("translate").getAsString() : "(缺 translate)";
    }

    private static String parentOf(String id, Map<net.minecraft.resources.ResourceLocation, AchievementService.Entry> byId) {
        AchievementService.Entry entry = byId.get(net.minecraft.resources.ResourceLocation.parse(id));
        return entry == null ? "(未登记)" : String.valueOf(entry.parent());
    }

    private static boolean isHidden(String id, Map<net.minecraft.resources.ResourceLocation, AchievementService.Entry> byId) {
        AchievementService.Entry entry = byId.get(net.minecraft.resources.ResourceLocation.parse(id));
        return entry != null && entry.hidden();
    }

    /** 每个进度的标题 / 描述翻译键都要在中英语言文件里存在。 */
    private static void checkTranslations(List<String> lines, int[] counters) {
        String zh = readText(LANG_DIR.resolve("zh_cn.json"), lines, counters);
        String en = readText(LANG_DIR.resolve("en_us.json"), lines, counters);
        if (zh == null || en == null) {
            return;
        }
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            String base = "advancements." + entry.id().getNamespace() + "." + entry.id().getPath();
            check(lines, counters, "中文有翻译键 " + base + ".title", zh.contains("\"" + base + ".title\""),
                    "有", "缺失");
            check(lines, counters, "中文有翻译键 " + base + ".description",
                    zh.contains("\"" + base + ".description\""), "有", "缺失");
            check(lines, counters, "英文有翻译键 " + base + ".title", en.contains("\"" + base + ".title\""),
                    "有", "缺失");
            check(lines, counters, "英文有翻译键 " + base + ".description",
                    en.contains("\"" + base + ".description\""), "有", "缺失");
        }
        // v0.5.0.11 症状②：屏幕上那行字必须是【方块尖塔】。
        // 这条查的是**语言文件里的实际文本**（而不是翻译键有没有），所以它是真断言。
        String rootTitleZh = value(zh, AchievementService.ROOT_TITLE_KEY);
        String rootTitleEn = value(en, AchievementService.ROOT_TITLE_KEY);
        check(lines, counters, "根标题（=标签页名）中文翻成【方块尖塔】", "方块尖塔".equals(rootTitleZh),
                "方块尖塔", rootTitleZh);
        check(lines, counters, "根标题（=标签页名）英文翻成 Block Spire", "Block Spire".equals(rootTitleEn),
                "Block Spire", rootTitleEn);
        check(lines, counters, "【尖塔在等着你】改挂在子成就 entered_game 的标题上（两者不再是同一行字）",
                !rootTitleZh.equals(value(zh, "advancements.stc.entered_game.title")),
                "两行不同的字", "仍然相同");
        check(lines, counters, "【尖塔在等着你】的中文文本确实在 entered_game 上",
                "尖塔在等着你".equals(value(zh, "advancements.stc.entered_game.title")),
                "尖塔在等着你", value(zh, "advancements.stc.entered_game.title"));
    }

    /** 从语言文件里取某个键的原始文本（不解析 JSON，避免转义差异）。 */
    private static String value(String langJson, String key) {
        int i = langJson.indexOf("\"" + key + "\"");
        if (i < 0) {
            return "(缺键)";
        }
        int colon = langJson.indexOf(':', i);
        if (colon < 0) {
            return "(格式坏)";
        }
        int open = langJson.indexOf('"', colon);
        int close = open < 0 ? -1 : langJson.indexOf('"', open + 1);
        return open < 0 || close < 0 ? "(格式坏)" : langJson.substring(open + 1, close);
    }

    // ------------------------------------------------------------------ 小工具

    /** 报告 codec 不认识的键（原版会忽略，但那通常意味着写错了字段名）。 */
    private static void checkNoUnknownKeys(List<String> lines, int[] counters, String label, JsonObject obj,
                                           Set<String> allowed) {
        List<String> unknown = new ArrayList<>();
        for (String key : obj.keySet()) {
            if (!allowed.contains(key)) {
                unknown.add(key);
            }
        }
        check(lines, counters, label + "：没有 codec 不认识的键（多余字段）", unknown.isEmpty(), "无", unknown);
    }

    private static boolean hasBackground(JsonObject root) {
        return root.has("display") && root.getAsJsonObject("display").has("background");
    }

    private static boolean isBoolean(JsonElement e) {
        return e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean();
    }

    private static String kind(JsonElement e) {
        if (e == null) {
            return "(null)";
        }
        if (e.isJsonObject()) {
            return "对象";
        }
        if (e.isJsonArray()) {
            return "数组";
        }
        if (e.isJsonPrimitive()) {
            if (e.getAsJsonPrimitive().isBoolean()) {
                return "布尔";
            }
            if (e.getAsJsonPrimitive().isNumber()) {
                return "数字";
            }
            return "字符串";
        }
        return "空";
    }

    private static String background(JsonObject root) {
        JsonObject display = root.getAsJsonObject("display");
        return display != null && display.has("background") ? display.get("background").getAsString() : "(缺失)";
    }

    private static JsonObject read(Path file, List<String> lines, int[] counters) {
        String text = readText(file, lines, counters);
        if (text == null) {
            return null;
        }
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            check(lines, counters, file.getFileName() + "：能被解析（" + e.getMessage() + "）", false,
                    "合法 JSON", e.getClass().getSimpleName());
            return null;
        }
    }

    private static String readText(Path file, List<String> lines, int[] counters) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, file.getFileName() + "：能读取（" + e.getMessage() + "）", false,
                    "可读", "读取失败");
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

    /** 只报"通过/失败"的简写（不需要给出期望/实际值）。 */
    private static void check(List<String> lines, int[] counters, String label, boolean condition) {
        check(lines, counters, label, condition, true, condition);
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
