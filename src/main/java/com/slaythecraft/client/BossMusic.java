package com.slaythecraft.client;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.api.StcApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * 每层 BOSS 战的音乐（v0.4.5）。
 *
 * <p>需求："每层 BOSS 音乐可定制" —— 音乐由作者提供，两种接法：</p>
 * <ol>
 *   <li><b>放文件就行</b>（推荐）：把 {@code floor_<层>.ogg} 放进
 *       {@code assets/stc/sounds/boss/floor_<层>/}；模组自带的
 *       {@code assets/stc/sounds.json} 已经按约定定义了事件
 *       {@code stc:boss_music_floor_<层>}（见 {@link StcApi#bossMusicConvention(int)}）；</li>
 *   <li><b>写代码</b>：{@link StcApi#registerBossMusic(int, ResourceLocation)} 绑自己的声音事件。</li>
 * </ol>
 * <p>本类负责在 BOSS 战开始/结束时起停；没配音乐的层完全不动原版音乐。</p>
 * <p><b>音乐必须是 Ogg Vorbis（.ogg）</b>：Minecraft 只能播 ogg，mp3 / wav / flac 都不行。</p>
 *
 * <p>没有绑定音乐的层不动原版音乐（玩家自己的 BGM 继续放）。</p>
 */
public final class BossMusic {

    /** 正在播放的层（-1 = 没在放）。 */
    private static int playingFloor = -1;
    private static SoundInstance current;
    /** 要求下一次 tick 立刻停掉（战斗结束 / 断线时置位）。 */
    private static boolean stopRequested;
    /** 找不到声音事件时只警告一次（避免刷屏）。 */
    private static ResourceLocation lastMissing;

    private BossMusic() {
    }

    /** 每客户端 tick 调一次：进入 BOSS 战 → 放这一层的音乐；离开 → 停。 */
    public static void tick(Minecraft minecraft) {
        if (stopRequested) {
            stopRequested = false;
            stop(minecraft);
            if (playingFloor > 0) {
                MusicDiagnostics.record("战斗结束停止", null, playingFloor);
            }
            playingFloor = -1;
        }
        var view = ClientBattleState.view();
        int floor = view.active() && view.bossBattle() ? view.floor() : -1;
        // v0.5.1.0：把"这一帧的判定依据"记进诊断（只在状态变化时写，不会刷屏）
        MusicDiagnostics.observe(view.active(), view.bossBattle(), view.floor(), floor);
        if (floor == playingFloor) {
            return;
        }
        stop(minecraft);
        playingFloor = floor;
        if (floor > 0) {
            start(minecraft, floor);
        }
    }

    private static void start(Minecraft minecraft, int floor) {
        // v0.4.6.6：登记优先，其次"约定事件名"（模组自带 sounds.json 已经定义好 1–3 层，
        // 所以作者只把 ogg 放进 assets/stc/sounds/boss/floor_<层>/ 就能响，不用改代码）
        ResourceLocation soundId = StcApi.bossMusicFor(floor);
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (event == null) {
            if (!soundId.equals(lastMissing)) {
                lastMissing = soundId;
                // v0.5.1.0：把"为什么没播"写清楚（用户报过"BOSS 音乐无法播放"，
                // 而这一层信息以前只有一句"没有注册"，看不出是 sounds.json 缺条目还是路径写错）
                SlayTheCraft.LOGGER.warn("[STC] 第 {} 层 BOSS 音乐没有播放：声音事件 {} 未注册。"
                        + "请检查 src/main/resources/assets/stc/sounds.json 里是否有这个键、"
                        + "以及它引用的 ogg 是否在 assets/stc/sounds/ 下（且必须是 Ogg Vorbis）",
                        floor, soundId);
                MusicDiagnostics.record("事件未注册", soundId, floor);
            }
            return;
        }
        SoundInstance instance = SimpleSoundInstance.forMusic(event);
        minecraft.getSoundManager().play(instance);
        current = instance;
        SlayTheCraft.LOGGER.info("[STC] 播放第 {} 层 BOSS 音乐：{}", floor, soundId);
        MusicDiagnostics.record("开始播放", soundId, floor);
    }

    private static void stop(Minecraft minecraft) {
        if (current != null) {
            minecraft.getSoundManager().stop(current);
            current = null;
        }
    }

    /** 当前正在播放的层号（自检 / 调试用）。 */
    public static int playingFloor() {
        return playingFloor;
    }

    /** 断线 / 退出世界时清干净（真正的停止交给下一次 tick）。 */
    public static void reset() {
        stopRequested = true;
    }
}
