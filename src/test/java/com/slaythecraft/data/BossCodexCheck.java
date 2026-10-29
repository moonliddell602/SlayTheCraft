package com.slaythecraft.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.slaythecraft.combat.mc.MobIntents;
import com.slaythecraft.combat.core.Faction;
import com.slaythecraft.combat.mc.MobIntents;
import com.slaythecraft.combat.core.FloorBoss;
import com.slaythecraft.combat.mc.MobIntents;
import com.slaythecraft.combat.core.MobTier;
import com.slaythecraft.combat.mc.MobIntents;
import com.slaythecraft.combat.mc.MobCodex;

/**
 * 怪物图鉴的完整性校验（v0.4.6.3）。
 *
 * <p>反馈原文："BOSS 图鉴没有正常在各个所属阵营显示，有时也会显示'阵营没有记录怪物'的文案。
 * 解决建议：将所有的 BOSS 图鉴全部移到【尖塔护卫】阵营。"</p>
 *
 * <p>根因是两层叠加的：</p>
 * <ol>
 *   <li>图鉴的数据源是 {@code MobIntents.entries()}，而三层尖塔的 BOSS（以及铁傀儡 / 猪灵蛮兵 /
 *       恼鬼）**从来没登记过意图状态机** —— 它们在图鉴里根本不存在，所属阵营自然显示"没有记录怪物"；</li>
 *   <li>BOSS 的阵营归属散落在深海 / 终末 / 灾厄 / 地狱各处，而不是集中在一处。</li>
 * </ol>
 *
 * <p>这里逐条盯住：<b>每个阵营声明的成员都真的在图鉴里</b>（这就是"阵营没有记录怪物"的回归测试）、
 * <b>每一只 BOSS 都登记过状态机</b>、以及<b>BOSS 在图鉴里一律归【尖塔护卫】</b>。</p>
 *
 * <p>为什么一半断言是"读源码"：{@code MobIntents} 的静态块要构造原版 {@code EntityType}，
 * 而 {@code EntityType} 的类初始化会碰 {@code FeatureFlags} → 需要 FML 装载上下文，
 * 纯 JVM 里必然 NPE（{@code Bootstrap.bootStrap()} 也一样，它会去加载原版方块表）。
 * 所以这边沿用 {@code CuriosSlotCheck} 的做法：能在纯 JVM 跑的用真调用，跑不动的读源码文本。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.data.BossCodexCheck
 * </pre>
 */
public final class BossCodexCheck {

    /**
     * 登记过的实体类型常量：{@code registerScript(EntityType.XXX, ...)} 与
     * {@code register(EntityType.XXX, new Profile(...))}（v0.4.6.7 起 BOSS 用后者登记特殊机制）。
     */
    private static final Pattern REGISTERED_TYPE =
            Pattern.compile("register(?:Script)?\\(EntityType\\.([A-Z0-9_]+)");
    /** {@code new FloorBoss(层号, "minecraft:xxx", ...)} 里的实体 id。 */
    private static final Pattern FLOOR_BOSS_ID =
            Pattern.compile("new FloorBoss\\(\\s*\\d+\\s*,\\s*\"(minecraft:[a-z0-9_]+)\"");
    /** {@code Faction} 枚举里的成员名（{@code List.of("zombie", "husk", ...)} 的小写路径）。 */
    private static final Pattern FACTION_MEMBER = Pattern.compile("\"([a-z0-9_]+)\"");

    /**
     * 这个 BOSS id 是不是**虚拟**的（原版没有这个实体类型，例如带电的苦力怕）。
     *
     * <p>只看 core 里的 {@link FloorBoss#LIGHTNING_CREEPER_ID}，不碰 {@code MobIntents}
     * ——后者会触发原版实体类型的类初始化，裸 JVM 自检里加载不起来。</p>
     */
    private static boolean isVirtualBoss(String entityId) {
        return !entityId.startsWith("minecraft:");
    }

    private BossCodexCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        String intentsSource = readSource("combat/mc/MobIntents.java", lines, counters);
        String factionSource = readSource("combat/core/Faction.java", lines, counters);
        String floorBossSource = readSource("combat/core/FloorBoss.java", lines, counters);
        String codexSource = readSource("combat/mc/MobCodex.java", lines, counters);
        String detailSource = readSource("client/MobDetailScreen.java", lines, counters);
        if (intentsSource == null || factionSource == null || floorBossSource == null
                || codexSource == null || detailSource == null) {
            report(lines, counters);
            return;
        }

        Set<String> registered = new LinkedHashSet<>();
        Matcher registeredMatcher = REGISTERED_TYPE.matcher(intentsSource);
        while (registeredMatcher.find()) {
            registered.add(registeredMatcher.group(1));
        }
        check(lines, counters, "MobIntents 里有登记过的意图状态机（读到了 " + registered.size() + " 种）",
                registered.size() >= 30, ">= 30", registered.size());

        // ---------------------------------------------------------- 1. 每只 BOSS 都登记进图鉴
        // v0.4.6.7：BOSS 表以 FloorBoss.ALL 为准（原版没有"闪电苦力怕"这个实体类型，
        // 它用虚拟 id stc:lightning_creeper + 虚拟档案登记，所以不能只按字面量正则去抠）
        List<String> floorBossIds = new ArrayList<>();
        for (FloorBoss boss : FloorBoss.ALL) {
            floorBossIds.add(boss.entityId());
        }
        check(lines, counters, "FloorBoss 表读到了 " + floorBossIds.size() + " 只 BOSS（含虚拟 BOSS）",
                floorBossIds.size() == FloorBoss.ALL.size(), FloorBoss.ALL.size(), floorBossIds.size());
        Matcher bossMatcher = FLOOR_BOSS_ID.matcher(floorBossSource);
        int literals = 0;
        while (bossMatcher.find()) {
            literals++;
        }
        check(lines, counters, "FloorBoss 源码里至少写了 " + (floorBossIds.size() - 1) + " 条字面量条目",
                literals >= floorBossIds.size() - 1, ">= " + (floorBossIds.size() - 1), literals);
        for (String id : floorBossIds) {
            if (isVirtualBoss(id)) {
                // 虚拟 BOSS：源码里要有它的虚拟 id 常量，档案里要有状态机
                // （注意：这里**只能读源码**。MobIntents 引用原版实体类型，裸 JVM 里一碰
                //   它的静态初始化就会因为没有 NeoForge 加载环境而炸掉。）
                check(lines, counters, "虚拟 BOSS " + id + " 用常量登记（源码里有 LIGHTNING_CREEPER_ID）",
                        floorBossSource.contains("LIGHTNING_CREEPER_ID")
                                && intentsSource.contains("VIRTUALS.put"),
                        "有", "没有");
                check(lines, counters, "虚拟 BOSS " + id + " 在 MobIntents 里有档案",
                        intentsSource.contains("LIGHTNING_CREEPER_ID"), "有", "没有");
                continue;
            }
            String constant = id.substring(id.indexOf(':') + 1).toUpperCase(Locale.ROOT);
            check(lines, counters, "BOSS " + id + " 已登记意图状态机（否则图鉴里没有它）",
                    registered.contains(constant), "EntityType." + constant, "未登记");
        }

        // ---------------------------------------------------------- 2. 每个阵营的成员都能在图鉴里查到
        Set<String> factionMembers = new LinkedHashSet<>();
        Matcher memberMatcher = FACTION_MEMBER.matcher(factionSource);
        while (memberMatcher.find()) {
            factionMembers.add(memberMatcher.group(1));
        }
        check(lines, counters, "Faction 表读到了 " + factionMembers.size() + " 个成员",
                factionMembers.size() >= 30, ">= 30", factionMembers.size());
        for (String member : factionMembers) {
            boolean known = registered.contains(member.toUpperCase(Locale.ROOT))
                    || isVirtualBoss("stc:" + member);
            check(lines, counters, "阵营成员 " + member + " 在图鉴里查得到（不会显示'没有记录怪物'）",
                    known, "已登记", "未登记");
        }
        // 每个阵营至少要有一个能查到的成员
        for (Faction faction : Faction.all()) {
            boolean hasMob = false;
            for (String member : faction.members()) {
                hasMob |= registered.contains(member.toUpperCase(Locale.ROOT))
                        || isVirtualBoss("stc:" + member);
            }
            check(lines, counters, "阵营【" + faction.label() + "】在图鉴里至少有一只怪", hasMob, true, false);
        }

        // ---------------------------------------------------------- 3. BOSS 一律归【尖塔护卫】
        // 这一段是**真调用**：factionOf 只用到 Faction / MobTier，不需要原版注册表
        for (FloorBoss boss : FloorBoss.ALL) {
            Faction owner = MobCodex.factionOf(boss.entityId(), MobTier.BOSS);
            check(lines, counters, "BOSS【" + boss.name() + "】在图鉴里归【尖塔护卫】",
                    owner == Faction.SPIRE_GUARDIAN, Faction.SPIRE_GUARDIAN.label(), nameOf(owner));
        }
        check(lines, counters, "非 BOSS 档位不受影响（僵尸仍是亡灵生物）",
                MobCodex.factionOf("minecraft:zombie", MobTier.NORMAL) == Faction.UNDEAD,
                Faction.UNDEAD.label(), nameOf(MobCodex.factionOf("minecraft:zombie", MobTier.NORMAL)));
        check(lines, counters, "非 BOSS 档位不受影响（末影龙按战斗归属仍是终末住民）",
                MobCodex.factionOf("minecraft:ender_dragon", MobTier.NORMAL) == Faction.END_DWELLER,
                Faction.END_DWELLER.label(),
                nameOf(MobCodex.factionOf("minecraft:ender_dragon", MobTier.NORMAL)));

        // ---------------------------------------------------------- 4. 界面确实用了同一套归属
        check(lines, counters, "MobCodex 的阵营分组走 factionOf（而不是直接 Faction.ofMob）",
                codexSource.contains("factionOf(built)"), true, "未接线");
        check(lines, counters, "怪物详情页的'所属阵营'用同一套归属（两边不会打架）",
                detailSource.contains("MobCodex.factionOf(mob)"), true, "未接线");

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

    private static String nameOf(Faction faction) {
        return faction == null ? "无" : faction.label();
    }

    private static String readSource(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
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
