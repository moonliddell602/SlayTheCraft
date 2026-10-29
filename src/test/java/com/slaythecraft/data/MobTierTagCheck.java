package com.slaythecraft.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.slaythecraft.combat.core.FloorBoss;
import com.slaythecraft.combat.mc.MobIntents;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/**
 * 怪物分档标签的数据校验（v0.0.2 V10；v0.4.5 跟着 BOSS 表调整）。
 *
 * <p>需求点名的四种原版精英（远古守卫者 / 劫掠兽 / 铁傀儡 / 猪灵蛮兵）与三层 BOSS
 * 全靠 `data/stc/tags/entity_type/mob_tier/*.json` 驱动；这些 JSON 编译器不管、`runData` 也只在
 * 真正的服务器加载数据包时才校验，所以这里直接读文件按同样格式检查一遍：
 * id 必须合法、四种精英必须在精英标签里、BOSS 标签必须与本层 BOSS 表**逐条一致**、
 * 掠夺者/卫道士（v0.4.5 降为常规敌怪）不在精英与 BOSS 标签里、两个标签不能重叠。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.data.MobTierTagCheck
 * </pre>
 */
public final class MobTierTagCheck {

    /**
     * 需求点名的原版精英（猪灵蛮兵按"小队 2 只"处理，见 {@code MobTiers.squadSize}）。
     *
     * <p>v0.4.6.2：远古守卫者升为第 2 层 BOSS，已从这里移出。</p>
     */
    private static final List<String> REQUIRED_ELITES = List.of(
            "minecraft:ravager",
            "minecraft:iron_golem",
            "minecraft:piglin_brute",
            "minecraft:wither_skeleton");

    /**
     * 三层尖塔的全部 BOSS（v0.4.5 需求："每层可以有不止一只 BOSS"）。
     *
     * <p>这份名单必须与 {@code FloorBoss.ALL} 完全一致（下面会逐条比对）。</p>
     */
    private static final List<String> REQUIRED_BOSSES = List.of(
            // 第 1 层：旋风人 / 唤魔者 / 嗅探兽（移出坚守者）
            "minecraft:breeze",
            "minecraft:evoker",
            "minecraft:sniffer",
            // 第 2 层：僵尸疣猪兽 / 熊猫 / 远古守卫者（v0.4.6.2 加入，战斗生命 320）
            "minecraft:zoglin",
            "minecraft:panda",
            "minecraft:elder_guardian",
            // 第 3 层：末影龙 / 凋零 / 坚守者
            "minecraft:ender_dragon",
            "minecraft:wither",
            "minecraft:warden");

    /**
     * 需求点名降为**常规敌怪**的怪（既不是精英也不是 BOSS）。
     *
     * <p>v0.4.5：掠夺者 / 卫道士；v0.4.6.2：女巫、末影人、末影螨、岩浆怪、烈焰人。</p>
     */
    private static final List<String> DEMOTED = List.of(
            "minecraft:pillager",
            "minecraft:vindicator",
            "minecraft:witch",
            "minecraft:enderman",
            "minecraft:endermite",
            "minecraft:magma_cube",
            "minecraft:blaze",
            "minecraft:ghast");

    private MobTierTagCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        Set<String> elites = readTag("elite", lines, counters);
        Set<String> bosses = readTag("boss", lines, counters);

        for (String id : REQUIRED_ELITES) {
            check(lines, counters, "精英标签包含 " + id, elites.contains(id), true, elites.contains(id));
        }
        for (String id : REQUIRED_BOSSES) {
            check(lines, counters, "BOSS 标签包含 " + id, bosses.contains(id), true, bosses.contains(id));
        }
        check(lines, counters, "BOSS 标签数量与 BOSS 表一致",
                bosses.size() == REQUIRED_BOSSES.size(), REQUIRED_BOSSES.size(), bosses.size());
        Set<String> missing = new LinkedHashSet<>(REQUIRED_BOSSES);
        missing.removeAll(bosses);
        check(lines, counters, "BOSS 表里的每一只都在标签里", missing.isEmpty(), "空", missing);
        Set<String> extra = new LinkedHashSet<>(bosses);
        extra.removeAll(REQUIRED_BOSSES);
        check(lines, counters, "标签里没有 BOSS 表之外的怪", extra.isEmpty(), "空", extra);
        // v0.4.6.7：闪电苦力怕是**虚拟 BOSS**（带电的苦力怕），原版实体标签里不可能有它，
        // 所以比对时先把虚拟 id 从 BOSS 表里剔除
        List<String> realBossIds = new ArrayList<>();
        for (String id : floorBossIds()) {
            if (id.startsWith("minecraft:")) {
                realBossIds.add(id);
            }
        }
        check(lines, counters, "标签与 FloorBoss.ALL 完全一致（虚拟 BOSS 除外）",
                bosses.equals(new LinkedHashSet<>(realBossIds)), realBossIds, bosses);
        for (String id : floorBossIds()) {
            if (!id.startsWith("minecraft:")) {
                check(lines, counters, "虚拟 BOSS " + id + " 不进实体标签（标签里没有它）",
                        !bosses.contains(id), true, bosses.contains(id));
            }
        }
        for (String id : DEMOTED) {
            check(lines, counters, id + " 已降为常规敌怪（不在精英标签里）", !elites.contains(id), false,
                    elites.contains(id));
            check(lines, counters, id + " 已降为常规敌怪（不在 BOSS 标签里）", !bosses.contains(id), false,
                    bosses.contains(id));
        }
        // v0.4.6.2 需求：远古守卫者是第 2 层 BOSS（尖塔战斗生命 320），不再算精英
        check(lines, counters, "远古守卫者是 BOSS 而不是精英",
                bosses.contains("minecraft:elder_guardian") && !elites.contains("minecraft:elder_guardian"), true,
                false);
        Set<String> overlap = new LinkedHashSet<>(elites);
        overlap.retainAll(bosses);
        check(lines, counters, "两个标签没有重叠", overlap.isEmpty(), "空", overlap);
        check(lines, counters, "精英数量合理（普通整合包十几个以内）",
                elites.size() >= REQUIRED_ELITES.size() && elites.size() <= 64, true, elites.size());
        check(lines, counters, "标签文件不是 replace=true（否则会顶掉别家的追加）",
                !replaceTrue("elite") && !replaceTrue("boss"), true, false);

        // v0.4.5：击杀阈值（常规 60 / 精英 30）走配置默认，细项见 ConfigDefaultsCheck
        int normalThreshold = com.slaythecraft.combat.mc.MobTiers
                .threshold(com.slaythecraft.combat.core.MobTier.NORMAL);
        int eliteThreshold = com.slaythecraft.combat.mc.MobTiers
                .threshold(com.slaythecraft.combat.core.MobTier.ELITE);
        check(lines, counters, "常规怪击杀阈值不再是从前的 5（需求：60）", normalThreshold == 60
                || normalThreshold == com.slaythecraft.Config.REWARD_THRESHOLD_NORMAL.getDefault(), 60,
                normalThreshold);
        check(lines, counters, "精英击杀阈值不再是从前的 3（需求：30）", eliteThreshold == 30
                || eliteThreshold == com.slaythecraft.Config.REWARD_THRESHOLD_ELITE.getDefault(), 30,
                eliteThreshold);

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1] + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    /** {@code FloorBoss.ALL} 里的实体 id（顺序与表一致）。 */
    private static List<String> floorBossIds() {
        List<String> ids = new ArrayList<>();
        for (com.slaythecraft.combat.core.FloorBoss boss : com.slaythecraft.combat.core.FloorBoss.ALL) {
            ids.add(boss.entityId());
        }
        return ids;
    }

    private static Set<String> readTag(String tier, List<String> lines, int[] counters) {
        Set<String> values = new LinkedHashSet<>();
        JsonObject root = readJson(tier, lines, counters);
        if (root == null) {
            return values;
        }
        JsonElement element = root.get("values");
        if (element == null || !element.isJsonArray()) {
            check(lines, counters, tier + " 标签有 values 数组", false, "数组", String.valueOf(element));
            return values;
        }
        JsonArray array = element.getAsJsonArray();
        boolean wellFormed = true;
        for (JsonElement entry : array) {
            String id = entry.getAsString();
            values.add(id);
            wellFormed &= ResourceLocation.tryParse(id) != null;
        }
        check(lines, counters, tier + " 标签里的 id 都是合法资源位置", wellFormed, true, wellFormed);
        check(lines, counters, tier + " 标签里的 id 不重复", values.size() == array.size(), array.size(),
                values.size());
        return values;
    }

    private static boolean replaceTrue(String tier) {
        JsonObject root = readJson(tier, new ArrayList<>(), new int[] {0, 0});
        return root != null && root.has("replace") && root.get("replace").getAsBoolean();
    }

    private static JsonObject readJson(String tier, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "resources", "data", "stc", "tags", "entity_type", "mob_tier",
                tier + ".json");
        if (!Files.isRegularFile(path)) {
            check(lines, counters, "标签文件存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            check(lines, counters, tier + " 标签是合法 JSON", true, true, true);
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            check(lines, counters, tier + " 标签能被解析（" + e + "）", false, "合法 JSON",
                    e.getClass().getSimpleName());
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
