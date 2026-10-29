package com.slaythecraft.client;

import com.slaythecraft.network.BattleStatePayload;
import com.slaythecraft.network.BattleView;

/**
 * 客户端保存"最近一次收到的战斗快照"（v0.0.2 V1；v0.2.3 修正 ESC 行为；v0.5.1.9 改成整场战斗都留在屏幕上）。
 *
 * <p>出牌界面在**整场战斗期间**都顶在屏幕上：玩家回合可以出牌，敌怪回合只许看。
 * 按 ESC 可以把它收起来看一眼世界，但**只是暂时收起**：{@link #HIDE_MILLIS} 之后会自动弹回来。</p>
 *
 * <p>为什么改成"暂时"：以前是"本回合不再弹出"，而结束回合又只能在出牌界面上点，
 * 于是按 ESC 之后战斗就卡住了——界面不回来、回合推不下去、敌人一直保持被冻结的状态
 * （实测反馈原文："使用 ESC 键直接退出战斗会导致敌人仍然被定在原地"）。
 * 想真正离开战斗请用出牌界面上的「放弃战斗」（两步确认）。</p>
 *
 * <p>v0.5.1.9：敌怪回合不再收起界面（见 {@link #shouldShowScreen()}）——收起会让玩家掉回世界，
 * 于是能按 wasd 走动、能转视角，把"敌人正在打你"的演出全躲过去了。</p>
 */
public final class ClientBattleState {

    /** 收起后多久自动弹回来（毫秒）。 */
    private static final long HIDE_MILLIS = 4000L;

    private static BattleView view = BattleView.inactive();
    /** v0.4.4：每开一场新战斗 +1（客户端据此只对一次 UI 对齐）。 */
    private static long battleId;
    /** 收起界面的截止时间戳；0 = 没被收起。 */
    private static long hiddenUntil;

    private ClientBattleState() {
    }

    public static void accept(BattleStatePayload payload) {
        BattleView next = payload.view();
        boolean wasActive = view.active();
        view = next;
        if (!next.active()) {
            hiddenUntil = 0L;
            // v0.3.0：战斗结束就收掉动作动画，别带到下一场
            ClientBattleFx.clear();
            // v0.4.5：BOSS 音乐也跟着停
            BossMusic.reset();
            // v0.5.1.7 A3：战斗结束时把待选牌请求也清掉（否则选牌页会浮在奖励界面上面）
            ClientCardPickState.clear();
            return;
        }
        // v0.4.6.3：「战斗开始」与「第 X 回合」改由服务端下发**原版 /title 大标题**
        // （见 BattleTitles），客户端不再自绘横幅；这里只保留"新战斗"的编号推进。
        if (!wasActive) {
            battleId++;
        }
        // v0.5.1.9：敌怪回合**不允许**界面被收起 —— 万一玩家在自己回合里刚按了 ESC、
        // 界面正收着，这一刻立刻把它拉回来（否则这几秒里他能走开、能转视角）。
        if (next.enemyTurn()) {
            hiddenUntil = 0L;
        }
    }

    public static BattleView view() {
        return view;
    }

    /** 当前这场战斗的编号（换一场就变，界面据此重新对齐一次敌人位置）。 */
    public static long battleId() {
        return battleId;
    }

    /** ESC 收起：暂时收起，几秒后自动弹回（避免战斗卡死）。 */
    public static void hideForThisTurn() {
        hiddenUntil = System.currentTimeMillis() + HIDE_MILLIS;
    }

    /** 收起界面的剩余毫秒数（给界面提示用，0 = 没被收起）。 */
    public static long hiddenRemaining() {
        return Math.max(0L, hiddenUntil - System.currentTimeMillis());
    }

    /**
     * 现在要不要把出牌界面顶在屏幕上。
     *
     * <p>v0.5.1.6 回合模型改造：以前只在"轮到我"时显示；后来改成整个玩家回合都显示 ——
     * 自己确认结束回合之后还要留在屏幕上等队友（负责推进的由服务端做），只是不能再出牌了。</p>
     *
     * <p>v0.5.1.9 修正：**敌怪回合也留在屏幕上**。需求原文："出牌回合结束后的敌怪环节，
     * 出现了玩家可能会暂时离开战斗UI，可以按 wasd 移动和旋转视角的问题。" 原因是旧逻辑在
     * 敌怪回合把界面收掉，玩家就掉回世界里了 —— 而界面在屏幕上本身就是"禁止移动 / 禁止转视角"
     * 的唯一手段（Minecraft 打开界面时不喂玩家移动输入、也不转视角）。</p>
     *
     * <p>ESC 的"暂时收起几秒"是玩家**主动**做的、且会自动弹回，所以保留；但敌怪回合里不给收
     * （见 {@code BattleScreen#keyPressed}）。</p>
     */
    public static boolean shouldShowScreen() {
        return view.keepsHudOpen() && hiddenRemaining() <= 0L;
    }

    /**
     * v0.5.1.9：现在是不是**敌怪回合**（界面留着，但手牌不可交互）。
     *
     * @see BattleView#enemyTurn()
     */
    public static boolean isEnemyPhase() {
        return view.enemyTurn();
    }

    /**
     * "玩家回合里我已经确认结束回合、正在等队友"（v0.5.1.6）。
     *
     * <p>这时界面要留在屏幕上、手牌与按钮置灰，并显示还在等谁。</p>
     */
    public static boolean isWaitingForOthers() {
        return view.active() && view.playerRound() && !view.yourTurn() && view.selfReady();
    }

    public static void clear() {
        view = BattleView.inactive();
        hiddenUntil = 0L;
        ClientBattleFx.clear();
        BossMusic.reset();
    }
}
