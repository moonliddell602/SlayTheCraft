package com.slaythecraft.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import net.minecraft.resources.ResourceLocation;

/**
 * BOSS 音乐的**运行时诊断**（v0.5.1.0）。
 *
 * <p>用户报"boss 音乐出现无法播放的问题，请帮我检查音乐文件和 sounds.json 文件"。
 * 我把能静态验的都验了：三个 ogg 都是**真的 Ogg Vorbis**（文件头 {@code OggS} + 含 {@code vorbis}）、
 * {@code sounds.json} 在 {@code assets/stc/} 根目录、键名与代码里的约定事件名
 * （{@code stc:boss_music_floor_<层>}）逐字一致、引用的路径 {@code stc:boss/floor_<层>/floor_<层>}
 * 也正好指向那几个文件 —— **静态看没有一处不对**。</p>
 *
 * <p>所以这里补一份运行时记录，把"每一次决策的**依据**"写下来：
 * 客户端看到的 {@code active} / {@code bossBattle} / {@code floor} 各是多少、
 * 算出来的目标层号是多少、声音事件有没有注册、最后播没播。
 * 出问题时把这个文件发出来就能一眼判断是"服务端没标 BOSS 战"、
 * "sounds.json 没加载"、还是"播了但听不见"。</p>
 *
 * <p>纯诊断：不参与播放逻辑，写失败也安静跳过。</p>
 */
final class MusicDiagnostics {

    private static final String FILE_NAME = "stc-boss-music.log";
    /** 上一次的状态（只在变化时写一行，避免每 tick 刷屏）。 */
    private static String lastState;
    /** 上一次写下的"判定"快照。 */
    private static String lastLine = "";
    private static boolean opened;

    private MusicDiagnostics() {
    }

    /**
     * 每次 tick 观察一次判定依据。
     *
     * @param active      快照是否"战斗中"
     * @param bossBattle  快照是否标了 BOSS 战
     * @param snapshotFloor 快照里的层号
     * @param targetFloor 算出来的目标层号（&gt; 0 表示要播这一层）
     */
    static void observe(boolean active, boolean bossBattle, int snapshotFloor, int targetFloor) {
        String state = active + "/" + bossBattle + "/" + snapshotFloor + "/" + targetFloor;
        if (state.equals(lastState)) {
            return;
        }
        lastState = state;
        record("判定：" + describe(active, bossBattle, snapshotFloor, targetFloor), null, targetFloor);
    }

    private static String describe(boolean active, boolean bossBattle, int snapshotFloor, int targetFloor) {
        if (!active) {
            return "不在战斗中 → 不放音乐";
        }
        if (!bossBattle) {
            return "在战斗中但快照没有标 BOSS 战 → 不放音乐（服务端 isBossBattle 判定）";
        }
        if (targetFloor <= 0) {
            return "标记了 BOSS 战但层号是 " + snapshotFloor + " → 不放音乐";
        }
        return "BOSS 战第 " + targetFloor + " 层 → 应播放 stc:boss_music_floor_" + targetFloor;
    }

    /** 记一条事件。 */
    static void record(String what, ResourceLocation soundId, int floor) {
        String line = "[" + what + "] 层=" + floor + (soundId == null ? "" : " 事件=" + soundId);
        if (line.equals(lastLine)) {
            return;
        }
        lastLine = line;
        write(line);
    }

    private static void write(String line) {
        Path path = Path.of("logs", FILE_NAME);
        try {
            if (!opened) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "", StandardCharsets.UTF_8);
                opened = true;
            }
            if (Files.exists(path) && Files.size(path) > 512L * 1024L) {
                Files.writeString(path, "", StandardCharsets.UTF_8);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException | RuntimeException ignored) {
            // 诊断绝不能影响播放
        }
    }

    /** 是否已经在写（自检用）。 */
    static boolean active() {
        return opened;
    }
}
