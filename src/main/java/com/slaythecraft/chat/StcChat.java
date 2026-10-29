package com.slaythecraft.chat;

import java.util.Collection;
import java.util.function.Supplier;

import com.slaythecraft.Config;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 聊天框输出的**唯一出口**（v0.4.6.3 建立；v0.4.6.5 拆成三类开关）。
 *
 * <p>需求（v0.4.6.3）："聊天框不再输出系统执行结果和指令。"<br>
 * 需求（v0.4.6.5 追加）："指令回执保留、只有战报和系统提示静音。"</p>
 *
 * <p>于是分成三条互不影响的通道，每条一个配置项：</p>
 *
 * <table border="1">
 *   <caption>三类输出</caption>
 *   <tr><th>通道</th><th>内容</th><th>配置</th><th>默认</th></tr>
 *   <tr><td>{@link #feedback}</td><td>{@code /stc …} 指令回执</td>
 *       <td>{@code chatCommandFeedback}</td><td><b>开</b>（需求：指令回执保留）</td></tr>
 *   <tr><td>{@link #battle}</td><td>战斗战报（伤害 / 抽牌 / 状态跳字）</td>
 *       <td>{@code chatBattleLog}</td><td>关（需求：战报静音）</td></tr>
 *   <tr><td>{@link #tell}</td><td>界面 / 流程的系统提示</td>
 *       <td>{@code chatSystemMessage}</td><td>关（需求：系统提示静音）</td></tr>
 * </table>
 *
 * <p>{@code chatOutput} 是**总开关**：打开时无视上面三项，一切都写回聊天框
 * （等于 v0.4.6.2 及以前的旧行为），排查数值时最省事。</p>
 *
 * <p>用 {@code Supplier} 形式传文案的重载在整条通道关闭时**不会求值**，顺带省掉一次字符串拼接
 * ——战斗战报就是靠这一点做到"默认零开销"的。</p>
 */
public final class StcChat {

    private StcChat() {
    }

    // ------------------------------------------------------------------ 通道开关

    /** 指令回执是否可见（总开关打开时恒真）。 */
    public static boolean feedbackEnabled() {
        return Config.chatOutput() || Config.chatCommandFeedback();
    }

    /** 战斗战报是否可见（总开关打开时恒真）。 */
    public static boolean battleEnabled() {
        return Config.chatOutput() || Config.chatBattleLog();
    }

    /** 系统提示是否可见（总开关打开时恒真）。 */
    public static boolean systemEnabled() {
        return Config.chatOutput() || Config.chatSystemMessage();
    }

    /** 兼容旧调用：三条通道任意一条打开就算"有输出"。 */
    public static boolean enabled() {
        return feedbackEnabled() || battleEnabled() || systemEnabled();
    }

    // ------------------------------------------------------------------ 指令回执（默认保留）

    /** 指令执行成功（{@code /stc …}）。 */
    public static void feedback(CommandSourceStack source, Supplier<Component> message) {
        if (source == null || message == null || !feedbackEnabled()) {
            return;
        }
        source.sendSuccess(message, false);
    }

    /** 指令执行成功（文案已经算好的情况）。 */
    public static void feedback(CommandSourceStack source, Component message) {
        feedback(source, () -> message);
    }

    /** 指令执行失败。 */
    public static void failure(CommandSourceStack source, Supplier<Component> message) {
        if (source == null || message == null || !feedbackEnabled()) {
            return;
        }
        source.sendFailure(message.get());
    }

    /** 指令执行失败（文案已经算好的情况）。 */
    public static void failure(CommandSourceStack source, Component message) {
        failure(source, () -> message);
    }

    // ------------------------------------------------------------------ 战斗战报（默认静音）

    /** 一条战斗战报（引擎战报走这里；整场战斗会调用几百次）。 */
    public static void battle(Collection<ServerPlayer> roster, Supplier<String> message) {
        if (roster == null || message == null || !battleEnabled()) {
            return;
        }
        Component component = Component.literal("[STC] " + message.get());
        for (ServerPlayer member : roster) {
            if (member != null) {
                member.sendSystemMessage(component);
            }
        }
    }

    /** 一条战斗战报（文案已经算好的情况）。 */
    public static void battle(Collection<ServerPlayer> roster, String message) {
        if (message == null) {
            return;
        }
        battle(roster, () -> message);
    }

    // ------------------------------------------------------------------ 系统提示（默认静音）

    /** 系统提示（界面按钮、流程结果、事件结果…）。 */
    public static void tell(ServerPlayer player, Component message) {
        if (player == null || message == null || !systemEnabled()) {
            return;
        }
        player.sendSystemMessage(message);
    }

    /** 系统提示（纯文本，调用方自己带 {@code [STC] } 前缀）。 */
    public static void tell(ServerPlayer player, String message) {
        if (message == null) {
            return;
        }
        tell(player, Component.literal(message));
    }

    /** 系统提示（纯文本，自动补 {@code [STC] } 前缀 —— 与旧版本的聊天栏格式一致）。 */
    public static void note(ServerPlayer player, String message) {
        if (message == null) {
            return;
        }
        tell(player, Component.literal("[STC] " + message));
    }

    /** 给一组玩家发同一条系统提示。 */
    public static void tellAll(Collection<ServerPlayer> players, Component message) {
        if (players == null || !systemEnabled()) {
            return;
        }
        for (ServerPlayer player : players) {
            tell(player, message);
        }
    }

    /** 给一组玩家发同一条系统提示（纯文本）。 */
    public static void tellAll(Collection<ServerPlayer> players, String message) {
        if (players == null || message == null) {
            return;
        }
        tellAll(players, Component.literal(message));
    }
}
