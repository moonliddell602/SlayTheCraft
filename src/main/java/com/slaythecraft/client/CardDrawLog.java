package com.slaythecraft.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.SlayTheCraft;

/**
 * 卡面绘制的**运行时诊断**（v0.5.0.19）。
 *
 * <p>为什么需要它：用户报了"手牌卡面错位"（卡体与卡面内容错开约一张牌的宽度），
 * 但我逐项核对坐标与姿态栈都自洽，**静态推演不出这个偏移**。这类"只在真实渲染时才出现"的
 * 问题只能拿运行时数据定位。</p>
 *
 * <p>本类把每个渲染帧里每张牌**实际被绘制时的矩形**写到 {@code logs/stc-card-draw.log}，
 * 每帧一段、以 {@code --- frame ---} 分隔。它是**纯诊断**：不参与任何绘制逻辑，
 * 写失败也安静跳过（绝不影响游戏）。</p>
 *
 * <h2>怎么用</h2>
 *
 * <p>正常玩一局、让界面出现错位，然后把日志文件交给开发者。判读方法：</p>
 * <ul>
 *   <li><b>正常的一帧</b>：同一帧内各张牌 {@code y} 相同、{@code x} 逐张递增约 86（缩放后 100）；</li>
 *   <li>某张牌的 {@code x} 与相邻牌相差**接近一个牌宽** → 偏移出在算出这个 x 的地方；</li>
 *   <li>同一帧里**同一张牌出现两次** → 这张牌被画了两遍（本类会在该行直接标出来）。</li>
 * </ul>
 */
public final class CardDrawLog {

    /** 诊断日志文件名（在游戏运行目录的 logs/ 下）。 */
    private static final String FILE_NAME = "stc-card-draw.log";
    /** 最多保留多少帧（再多就从头覆盖），避免一直玩把文件撑大。 */
    private static final int MAX_FRAMES = 400;

    /** 本帧已画的牌（牌 id → 上一行的描述），用来检测"同一张牌被画了两次"。 */
    private static final Map<String, String> FRAME = new LinkedHashMap<>();
    /** 已经写了多少帧。 */
    private static int frames;
    private static boolean opened;
    /** 关闭开关：出问题时用户可以在配置外手动删日志；这里只在首次写时清空。 */
    private static final List<String> PENDING = new ArrayList<>();

    private CardDrawLog() {
    }

    /** 记一次卡面绘制。 */
    static void record(String cardId, int x, int y, int w, int h, float scale, int handSize) {
        String line = "  " + cardId + "  x=" + x + " y=" + y + " w=" + w + " h=" + h
                + " scale=" + String.format("%.2f", scale);
        String previous = FRAME.get(cardId);
        if (previous != null) {
            // 同一帧里同一张牌被画了两次 —— 这本身就是最可能的线索，直接标出来
            line = line + "   **同一帧重复绘制**（上一次 " + previous.trim() + "）";
        }
        FRAME.put(cardId, line);
        if (FRAME.size() == 1) {
            PENDING.add("--- frame " + frames + " 手牌数=" + handSize + " ---");
            frames++;
        }
        PENDING.add(line);
        // 攒够一批就落盘（崩溃/强杀也不会丢掉全部诊断）
        if (PENDING.size() >= 64) {
            flush();
        }
    }

    /** 把攒下的行落盘。 */
    private static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        Path path = Path.of("logs", FILE_NAME);
        try {
            if (!opened) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "", StandardCharsets.UTF_8);
                opened = true;
            }
            if (frames > MAX_FRAMES) {
                // 超出上限就整体重来，永远保留最近的一段
                Files.writeString(path, "", StandardCharsets.UTF_8);
                frames = 0;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                for (String line : PENDING) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            PENDING.clear();
        } catch (IOException | RuntimeException e) {
            // 诊断绝不能影响游戏
            PENDING.clear();
            SlayTheCraft.LOGGER.debug("[STC] 卡面绘制诊断写日志失败：{}", e.toString());
        }
    }

    /** 一帧结束：清掉"本帧"的去重表（诊断只区分"同一帧内重复"）。 */
    static void endFrame() {
        if (!FRAME.isEmpty()) {
            flush();
            FRAME.clear();
        }
    }

    /** 诊断是否正在工作（自检用）。 */
    static boolean active() {
        return opened;
    }
}
