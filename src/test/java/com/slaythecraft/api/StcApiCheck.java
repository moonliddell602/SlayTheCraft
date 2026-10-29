package com.slaythecraft.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * {@link StcApi}（v0.4.5 的对外接口）自检。
 *
 * <p>接口是给整合包用的，一旦签名/语义变了会直接破坏别人的模组，所以这里把行为钉死：
 * 约定路径怎么拼、登记优先、非法参数要抛异常、BOSS 音乐按层查表。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.api.StcApiCheck
 * </pre>
 */
public final class StcApiCheck {

    private StcApiCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        StcApi.clearCardTextures();
        StcApi.clearBossMusic();

        // ---- 卡牌贴图：约定路径
        ResourceLocation strike = StcApi.id("stc:strike");
        check(lines, counters, "约定路径 = <命名空间>:textures/card/<路径>.png",
                StcApi.textureForCard(strike).equals(
                        ResourceLocation.fromNamespaceAndPath("stc", "textures/card/strike.png")),
                "stc:textures/card/strike.png", StcApi.textureForCard(strike));
        check(lines, counters, "约定的命名空间跟着卡牌 id 走（别的命名空间不会落到 stc）",
                StcApi.textureForCard(StcApi.id("mypack:custom_card")).getNamespace().equals("mypack"),
                "mypack", StcApi.textureForCard(StcApi.id("mypack:custom_card")));

        // ---- 卡牌贴图：显式登记优先
        ResourceLocation custom = StcApi.id("mypack:cards/fancy_strike");
        StcApi.registerCardTexture(strike, custom);
        check(lines, counters, "登记之后用登记的贴图", StcApi.textureForCard(strike).equals(custom), custom,
                StcApi.textureForCard(strike));
        check(lines, counters, "cardTexture() 能查到登记的贴图",
                StcApi.cardTexture(strike).orElse(null) != null, custom, StcApi.cardTexture(strike).orElse(null));
        check(lines, counters, "没登记的卡返回空 Optional", StcApi.cardTexture(StcApi.id("stc:defend")).isEmpty(),
                true, StcApi.cardTexture(StcApi.id("stc:defend")).isPresent());
        check(lines, counters, "登记计数正确", StcApi.cardTextureCount() == 1, 1, StcApi.cardTextureCount());

        boolean threw = false;
        try {
            StcApi.registerCardTexture(null, custom);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(lines, counters, "cardId 为空要抛 IllegalArgumentException", threw, true, threw);

        // ---- BOSS 音乐
        StcApi.registerBossMusic(1, StcApi.id("mypack:boss_floor1"));
        StcApi.registerBossMusic(3, ResourceLocation.withDefaultNamespace("music_dragon"));
        check(lines, counters, "第 1 层音乐查到登记的 id",
                StcApi.bossMusic(1).orElseThrow().equals(StcApi.id("mypack:boss_floor1")), "mypack:boss_floor1",
                StcApi.bossMusic(1).orElse(null));
        check(lines, counters, "第 2 层没配音乐 → 空（不会去动原版音乐）", StcApi.bossMusic(2).isEmpty(), true,
                StcApi.hasBossMusic(2));
        check(lines, counters, "可以绑定原版声音事件（零资源也能用）",
                StcApi.bossMusic(3).orElseThrow().equals(
                        ResourceLocation.withDefaultNamespace("music_dragon")), "minecraft:music_dragon",
                StcApi.bossMusic(3).orElse(null));
        check(lines, counters, "层数计数正确", StcApi.bossMusicCount() == 2, 2, StcApi.bossMusicCount());

        threw = false;
        try {
            StcApi.registerBossMusic(0, StcApi.id("mypack:boss"));
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(lines, counters, "层号从 1 开始，0 要抛异常", threw, true, threw);

        // ---- v0.4.6.6：约定事件名（作者只放文件、不写代码也能有 BOSS 音乐）
        check(lines, counters, "bossMusicConvention(2) = stc:boss_music_floor_2",
                StcApi.bossMusicConvention(2).equals(StcApi.id("stc:boss_music_floor_2")), "stc:boss_music_floor_2",
                StcApi.bossMusicConvention(2));
        check(lines, counters, "没登记时 bossMusicFor 回落到约定（第 4 层）",
                StcApi.bossMusicFor(4).equals(StcApi.id("stc:boss_music_floor_4")), "stc:boss_music_floor_4",
                StcApi.bossMusicFor(4));
        check(lines, counters, "hasBossMusic 仍只反映「登记」状态（约定不算登记）",
                !StcApi.hasBossMusic(4), false, StcApi.hasBossMusic(4));
        StcApi.registerBossMusic(4, StcApi.id("mypack:floor4_theme"));
        check(lines, counters, "登记后 bossMusicFor(4) 用登记的，覆盖约定",
                StcApi.bossMusicFor(4).equals(StcApi.id("mypack:floor4_theme")), "mypack:floor4_theme",
                StcApi.bossMusicFor(4));
        StcApi.clearBossMusic();
        check(lines, counters, "clearBossMusic 后回到约定（不留悬空）",
                StcApi.bossMusicFor(4).equals(StcApi.id("stc:boss_music_floor_4")), "stc:boss_music_floor_4",
                StcApi.bossMusicFor(4));

        // ---- 通用
        check(lines, counters, "modId() 是 stc", "stc".equals(StcApi.modId()), "stc", StcApi.modId());
        threw = false;
        try {
            StcApi.id("not a valid id");
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(lines, counters, "非法 ResourceLocation 抛异常", threw, true, threw);

        StcApi.clearCardTextures();
        StcApi.clearBossMusic();

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
