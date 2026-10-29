package com.slaythecraft.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slaythecraft.api.StcApi;

import net.minecraft.resources.ResourceLocation;

/**
 * BOSS 音乐资源 + "约定事件名"自检（v0.4.6.6）。
 *
 * <p>钉死两件最容易出错的事：</p>
 * <ol>
 *   <li><b>文件位置</b>：{@code sounds.json} 必须在 {@code assets/stc/} 根目录，
 *       放进 {@code assets/stc/sounds/} 里面就完全不生效（游戏加载不到事件）。</li>
 *   <li><b>事件名与目录的对应</b>：{@code sounds.json} 里的事件名 / 引用路径，
 *       必须和 {@link StcApi#bossMusicConvention(int)} 的约定以及
 *       {@code sounds/boss/floor_<n>/floor_<n>.ogg} 的目录结构互相咬合。</li>
 * </ol>
 * <p>另外钉死"只支持 ogg"这个事实：打包目录里不允许出现 mp3/wav 等 Minecraft
 * 根本解码不了的音频（宁可没有，也不要给玩家一个放不出声的假音乐）。</p>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.client.SoundAssetCheck
 * </pre>
 */
public final class SoundAssetCheck {

    private static final int FLOORS = 3;

    private SoundAssetCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        Path resources = Path.of("src", "main", "resources");
        Path soundsJson = resources.resolve("assets/stc/sounds.json");
        Path wrongSoundsJson = resources.resolve("assets/stc/sounds/sounds.json");
        Path workspaceSoundsJson = Path.of("assets", "sounds", "sounds.json");

        // ================= A. 文件位置（最经典的坑） =================
        check(lines, counters, "sounds.json 在 assets/stc/ 根目录（正确位置）", Files.isRegularFile(soundsJson),
                "存在", Files.isRegularFile(soundsJson) ? "存在" : "缺失");
        check(lines, counters, "assets/stc/sounds/sounds.json 不存在（放这里等于没放）",
                !Files.isRegularFile(wrongSoundsJson), "不存在",
                Files.isRegularFile(wrongSoundsJson) ? "竟然存在" : "不存在");
        check(lines, counters, "工作区 assets/sounds/sounds.json 与打包版内容一致",
                sameText(workspaceSoundsJson, soundsJson), "一致", "不一致");

        for (int floor = 1; floor <= FLOORS; floor++) {
            Path readme = resources.resolve("assets/stc/sounds/boss/floor_" + floor + "/README.txt");
            check(lines, counters, "第 " + floor + " 层音乐目录有 README 说明（告诉玩家放什么）",
                    Files.isRegularFile(readme), "存在", "缺失");
        }
        check(lines, counters, "BOSS 音乐目录本身有 README", 
                Files.isRegularFile(resources.resolve("assets/stc/sounds/boss/README.txt")), "存在", "缺失");

        // ================= B. 打包目录里不许有放不出来的音频 =================
        List<String> badAudio = new ArrayList<>();
        List<String> oggFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(resources.resolve("assets/stc/sounds"))) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".flac")) {
                    badAudio.add(resources.relativize(path).toString().replace('\\', '/'));
                } else if (name.endsWith(".ogg")) {
                    oggFiles.add(resources.relativize(path).toString().replace('\\', '/'));
                }
            }
        } catch (IOException e) {
            check(lines, counters, "能遍历 sound 资源目录", false, "可遍历", e.getClass().getSimpleName());
        }
        check(lines, counters, "打包目录里没有 mp3/wav/m4a/flac（Minecraft 放不出来）", badAudio.isEmpty(), "0 个",
                badAudio);
        check(lines, counters, "随包发布的 BOSS 音乐都是 .ogg（1-3 层各一个）",
                oggFiles.size() >= FLOORS && oggFiles.stream().allMatch(p -> p.endsWith(".ogg")),
                "至少 " + FLOORS + " 个 ogg", oggFiles);
        for (int floor = 1; floor <= FLOORS; floor++) {
            Path music = resources.resolve("assets/stc/sounds/boss/floor_" + floor + "/floor_" + floor + ".ogg");
            check(lines, counters, "第 " + floor + " 层音乐文件在位：sounds/boss/floor_" + floor + "/floor_"
                    + floor + ".ogg", Files.isRegularFile(music), "存在",
                    Files.isRegularFile(music) ? "存在" : "缺失");
        }

        // ================= C. sounds.json 内容 =================
        JsonObject root = null;
        try {
            root = JsonParser.parseString(Files.readString(soundsJson, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            check(lines, counters, "sounds.json 能被解析成 JSON 对象", false, "可解析", e.getClass().getSimpleName());
        }
        if (root != null) {
            Set<String> expected = new LinkedHashSet<>();
            for (int floor = 1; floor <= FLOORS; floor++) {
                expected.add(StcApi.bossMusicConvention(floor).getPath());
            }
            check(lines, counters, "sounds.json 里正好定义了 " + FLOORS + " 个 BOSS 音乐事件",
                    root.size() == FLOORS, FLOORS, root.size());
            check(lines, counters, "事件名集合 == 约定事件名集合（不多不少）",
                    root.keySet().equals(expected), expected, root.keySet());

            boolean refsOk = true;
            for (int floor = 1; floor <= FLOORS; floor++) {
                String event = StcApi.bossMusicConvention(floor).getPath();
                JsonElement element = root.get(event);
                if (element == null || !element.isJsonObject()) {
                    check(lines, counters, "事件 " + event + " 是对象", false, "对象", "缺失");
                    refsOk = false;
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                String wanted = "stc:boss/floor_" + floor + "/floor_" + floor;
                check(lines, counters, event + " 引用 stc:boss/floor_" + floor + "/floor_" + floor,
                        oneSoundEquals(entry, wanted), wanted, soundList(entry));
                check(lines, counters, event + " 带 stream=true（长音频必须流式，否则卡加载）",
                        entry.has("stream") && entry.get("stream").getAsBoolean(), true,
                        entry.has("stream") ? entry.get("stream").getAsBoolean() : "缺 stream");
                check(lines, counters, event + " 分类是 music（走音乐音量滑条）",
                        !entry.has("category") || "music".equals(entry.get("category").getAsString()), "music",
                        entry.has("category") ? entry.get("category").getAsString() : "music(默认)");
                // 事件名 -> 引用路径 -> 磁盘文件，三者必须咬合，否则游戏里查不到音频
                Path onDisk = resources.resolve("assets/stc/sounds/boss/floor_" + floor + "/floor_" + floor + ".ogg");
                refsOk = refsOk && oneSoundEquals(entry, wanted) && Files.isRegularFile(onDisk);
            }
            check(lines, counters, "每个事件引用的 ogg 都能在磁盘上找到（事件<->文件不脱节）", refsOk, true, refsOk);
        }

        // ================= D. 约定 API：登记优先 + 约定兜底 =================
        StcApi.clearBossMusic();
        check(lines, counters, "bossMusicConvention(1) = stc:boss_music_floor_1",
                StcApi.bossMusicConvention(1).equals(id("boss_music_floor_1")), "stc:boss_music_floor_1",
                StcApi.bossMusicConvention(1));
        check(lines, counters, "bossMusicConvention(7) = stc:boss_music_floor_7（任意层号都能推）",
                StcApi.bossMusicConvention(7).equals(id("boss_music_floor_7")), "stc:boss_music_floor_7",
                StcApi.bossMusicConvention(7));
        boolean threw = false;
        try {
            StcApi.bossMusicConvention(0);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check(lines, counters, "层号 0 依然抛 IllegalArgumentException", threw, true, threw);

        check(lines, counters, "bossMusicFor(1) 没登记也能给出事件名（这就是「放文件就响」）",
                StcApi.bossMusicFor(1).equals(id("boss_music_floor_1")), "stc:boss_music_floor_1",
                StcApi.bossMusicFor(1));

        ResourceLocation custom = StcApi.id("mypack:my_boss_theme");
        StcApi.registerBossMusic(2, custom);
        check(lines, counters, "登记后 bossMusicFor(2) 用登记的（登记优先于约定）",
                StcApi.bossMusicFor(2).equals(custom), custom, StcApi.bossMusicFor(2));
        check(lines, counters, "没登记的层仍然走约定",
                StcApi.bossMusicFor(3).equals(id("boss_music_floor_3")), "stc:boss_music_floor_3",
                StcApi.bossMusicFor(3));
        StcApi.clearBossMusic();
        check(lines, counters, "清空登记后 bossMusicFor(2) 回到约定（不会留下悬空引用）",
                StcApi.bossMusicFor(2).equals(id("boss_music_floor_2")), "stc:boss_music_floor_2",
                StcApi.bossMusicFor(2));
        check(lines, counters, "bossMusicFor 用 orElseGet 懒取（不白建对象）",
                source("api/StcApi.java").contains("orElseGet("), "含 orElseGet", "未见");

        // ================= E. 源码接线 =================
        String bossMusic = source("client/BossMusic.java");
        check(lines, counters, "BossMusic 走 StcApi.bossMusicFor（约定兜底已接上）",
                bossMusic.contains("StcApi.bossMusicFor("), "含 bossMusicFor", "未见");
        check(lines, counters, "BossMusic 不再直接用 bossMusic() 查登记（否则约定失效）",
                !bossMusic.contains("StcApi.bossMusic("), "不含", "仍在直接调用");
        check(lines, counters, "BossMusic 文档写明只能 ogg",
                bossMusic.toLowerCase(java.util.Locale.ROOT).contains("ogg"), "含 ogg", "未见");

        // ================= F. 导入脚本（给作者用的护栏） =================
        Path script = Path.of("tools", "import-assets.ps1");
        String scriptText = readText(script);
        check(lines, counters, "导入脚本把 sounds.json 特殊映射到 assets/stc 根目录",
                scriptText.contains("$targetRoot 'sounds.json'"), "含特殊映射", "未见");
        check(lines, counters, "导入脚本把 mp3/wav 等列入不可用名单",
                scriptText.contains("$audioBad") && scriptText.contains("'.mp3'"), "含 audioBad", "未见");
        check(lines, counters, "导入脚本提示 ffmpeg 转换命令",
                scriptText.contains("ffmpeg"), "含 ffmpeg", "未见");
        String allowedLine = scriptText.lines().filter(line -> line.contains("$allowed = @(")).findFirst().orElse("");
        check(lines, counters, "导入脚本的 $allowed 里没有 .wav（wav 同样放不出来）",
                !allowedLine.contains(".wav"), "不含 .wav", allowedLine.trim());
        check(lines, counters, "导入脚本把 wav/flac 列进不可用名单（是跳过而不是静默复制）",
                scriptText.contains("'.wav'") && scriptText.contains("'.flac'"), "含 wav/flac", "未见");
        check(lines, counters, "导入脚本自身是 UTF-8 带 BOM（否则中文会乱码解析失败）",
                hasBom(script), "带 BOM", "无 BOM");

        lines.forEach(System.out::println);
        StcApi.clearBossMusic();
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static boolean oneSoundEquals(JsonObject entry, String wanted) {
        return entry.has("sounds") && entry.getAsJsonArray("sounds").size() == 1
                && wanted.equals(entry.getAsJsonArray("sounds").get(0).getAsString());
    }

    private static String soundList(JsonObject entry) {
        return entry.has("sounds") ? entry.getAsJsonArray("sounds").toString() : "缺 sounds";
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    private static String source(String relative) {
        return readText(Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative));
    }

    private static String readText(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private static boolean sameText(Path a, Path b) {
        String left = readText(a).replace("\r\n", "\n").trim();
        String right = readText(b).replace("\r\n", "\n").trim();
        return !left.isEmpty() && left.equals(right);
    }

    private static boolean hasBom(Path path) {
        try {
            byte[] head = Files.readAllBytes(path);
            return head.length >= 3 && (head[0] & 0xFF) == 0xEF && (head[1] & 0xFF) == 0xBB
                    && (head[2] & 0xFF) == 0xBF;
        } catch (IOException e) {
            return false;
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