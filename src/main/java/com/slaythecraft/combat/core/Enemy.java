package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A combatant that acts on its own turn. Its behaviour is a rotation of telegraphed
 * {@link Intent}s supplied by the Minecraft layer (usually derived from the mob type).
 */
public class Enemy extends Combatant {
    /**
     * **意图状态机**（v0.4.5）：每一"回合"是一组按顺序执行的动作，整体循环。
     *
     * <p>需求给的例子（僵尸）：</p>
     * <pre>
     * 第一回合：伤害 5
     * 第二回合：伤害 2×3，对玩家施加虚弱 1
     * 第三回合：获得力量 1，获得 5 格挡
     * </pre>
     *
     * <p>老构造（一串 {@link Intent}）会被自动包装成"每回合一个动作"，所以既有内容不受影响。</p>
     */
    private List<List<Intent>> script;
    private int rotationIndex;
    /**
     * **睡眠回合数**（v0.4.6.7，嗅探兽）。
     *
     * <p>需求原文："嗅探兽的前三回合处于『睡眠』状态，在每回合的出牌环节开始前获得 30 格挡，
     * 不会执行任何行动。在这期间，如果嗅探兽的格挡被击破并导致其受到伤害，其会结束『睡眠』状态。"</p>
     */
    private int sleepTurns;
    /** 苏醒后要换成哪套状态机（睡眠期间先存着）。 */
    private List<List<Intent>> awakeScript = List.of();
    /**
     * 状态机**循环回哪一回合**（v0.4.6.7）。
     *
     * <p>需求里很多 BOSS 是"第一回合只做一次，之后第 2~5 回合循环"（闪电苦力怕 / 唤魔者 / 嗅探兽），
     * 所以除了"从头循环"（0）之外还要能指定"从第 1 个下标开始循环"。</p>
     */
    private int loopFrom;
    /** 本回合要依次执行的全部动作。 */
    private List<Intent> actions = List.of();
    /** 兼容字段：本回合的**第一个**动作（预览 / 旧接口用）。 */
    private Intent intent;
    /** 本次意图的目标（多玩家战斗时是某名玩家；防御/蓄力类为 null = 自己）。 */
    private Combatant target;

    /** 老构造：每个 {@link Intent} 当成"一回合只有一个动作"。 */
    public Enemy(String name, int maxHp, int currentHp, List<Intent> rotation) {
        this(name, maxHp, currentHp, wrap(rotation), true);
    }

    /** 意图状态机构造：每个元素是一回合里要依次执行的动作列表。 */
    public Enemy(String name, int maxHp, int currentHp, List<List<Intent>> script, boolean stateMachine) {
        super(name, maxHp, currentHp);
        this.script = List.copyOf(Objects.requireNonNull(script, "script"));
    }

    public Enemy(String name, int maxHp, List<Intent> rotation) {
        this(name, maxHp, maxHp, rotation);
    }

    private static List<List<Intent>> wrap(List<Intent> rotation) {
        List<List<Intent>> wrapped = new ArrayList<>(rotation == null ? 0 : rotation.size());
        if (rotation != null) {
            for (Intent action : rotation) {
                wrapped.add(List.of(action));
            }
        }
        return wrapped;
    }

    /** The intent shown to the player for the upcoming enemy turn, or {@code null} before the first roll. */
    public Intent intent() {
        return intent;
    }

    /** 本回合会依次执行的全部动作（状态机的"当前回合程序"）。 */
    public List<Intent> actions() {
        return actions;
    }

    /** Picks the next intent from the rotation and telegraphes it（老接口：只取第一个动作）。 */
    public Intent rollIntent() {
        rollActions();
        return intent;
    }

    /** 推进状态机，返回本回合的全部动作。 */
    public List<Intent> rollActions() {
        // v0.5.0.8【旋风人】：锁血（晕眩）期间**冻结意图**，一直显示「自爆 X」，
        // 直到真正自爆为止。否则第一次敌人阶段之后状态机会推回到下一回合的攻击，
        // 面板上就会从「自爆 7」变回「攻击 12」，玩家看不到即将到来的自爆。
        if (hpLocked()) {
            actions = List.of(Intent.lock(charge()));
            intent = actions.get(0);
            return actions;
        }
        if (sleeping()) {
            // 睡眠中：这一回合什么都不做（文案显示"睡眠"），也不推进苏醒后的状态机
            actions = List.of();
            intent = new Intent(IntentType.UNKNOWN, 0, 1, "睡眠", "");
            return actions;
        }
        if (script.isEmpty()) {
            actions = List.of(Intent.unknown());
            intent = actions.get(0);
            return actions;
        }
        if (rotationIndex >= script.size()) {
            rotationIndex = Math.max(0, Math.min(loopFrom, script.size() - 1));
        }
        actions = script.get(rotationIndex);
        rotationIndex++;
        intent = actions.isEmpty() ? Intent.unknown() : actions.get(0);
        return actions;
    }

    // ------------------------------------------------------------------ 睡眠 / 苏醒（v0.4.6.7，嗅探兽）

    /**
     * 设定"前 {@code turns} 回合在睡觉"，并记下苏醒后要用的状态机。
     *
     * @param turns       睡眠回合数（0 = 不睡）
     * @param awakeScript 苏醒后的状态机（空 = 沿用原状态机）
     */
    public final void setSleep(int turns, List<List<Intent>> awakeScript) {
        this.sleepTurns = Math.max(0, turns);
        this.awakeScript = awakeScript == null ? List.of() : List.copyOf(awakeScript);
    }

    /**
     * 本回合的动作可被**临时替换**（v0.4.6.7：闪电苦力怕进入锁血时把意图换成"晕眩"）。
     */
    public final void setActions(List<Intent> newActions) {
        this.actions = newActions == null ? List.of() : List.copyOf(newActions);
        this.intent = this.actions.isEmpty() ? null : this.actions.get(0);
    }

    /** 这只怪对应的实体 id（{@code minecraft:vex}），召唤类动作靠它数"场上有几只同类"。 */
    private String mobId = "";

    /** 设置实体 id（Minecraft 层在造敌人时写入）。 */
    public final void setMobId(String id) {
        this.mobId = id == null ? "" : id;
    }

    /** 这只怪的实体 id（可能为空）。 */
    public final String mobId() {
        return mobId;
    }

    /** 状态机循环回哪一回合（0 = 从头循环）。 */
    public final int loopFrom() {
        return loopFrom;
    }

    /** 设置"从第 {@code index} 个回合开始循环"。 */
    public final void setLoopFrom(int index) {
        this.loopFrom = Math.max(0, index);
    }

    /** 现在还在睡吗。 */
    public final boolean sleeping() {
        return sleepTurns > 0;
    }

    /** 还剩几个睡眠回合。 */
    public final int sleepTurns() {
        return sleepTurns;
    }

    /** 睡着时被打疼了：立刻结束睡眠，换成苏醒后的状态机（从它自己的第一回合重新开始）。 */
    public final void wake() {
        if (sleepTurns <= 0 && (awakeScript.isEmpty() || script == awakeScript)) {
            return;
        }
        sleepTurns = 0;
        if (!awakeScript.isEmpty()) {
            script = awakeScript;
        }
        rotationIndex = 0;
        actions = List.of();
        intent = null;
    }

    /**
     * 过掉一个睡眠回合。
     *
     * @return true = 这一回合仍然在睡（调用方应该跳过它的行动）
     */
    public final boolean spendSleepTurn() {
        if (sleepTurns <= 0) {
            return false;
        }
        sleepTurns--;
        if (sleepTurns == 0) {
            wake();
            return false;
        }
        return true;
    }

    /** 兼容：整串状态机里的"每回合第一个动作"。 */
    public List<Intent> rotation() {
        List<Intent> first = new ArrayList<>(script.size());
        for (List<Intent> turn : script) {
            first.add(turn.isEmpty() ? Intent.unknown() : turn.get(0));
        }
        return List.copyOf(first);
    }

    /** 意图状态机本体（只读）。 */
    public List<List<Intent>> script() {
        return script;
    }

    /** 状态机走到第几回合了（0 起；显示/自检用）。 */
    public int scriptIndex() {
        return rotationIndex;
    }

    /**
     * 本回合意图的文案（v0.4.5：一回合可以有多个动作，全部平铺出来）。
     *
     * <p>例：僵尸第二回合显示"攻击 2×3 + 施加虚弱 1"。</p>
     *
     * <p>v0.5.0.7：这是**不带数值**的静态标签（来自 {@link Intent#label()}），
     * UI 请改用 {@link #actualIntentLines()}——那个会把力量/虚弱/易伤/迅捷/缓慢算进数值，
     * 并且**一个行动一行**，不会再被挤到显示不全。</p>
     */
    public String intentLabel() {
        if (actions.isEmpty()) {
            return intent == null ? "未知" : intent.label();
        }
        if (actions.size() == 1) {
            return actions.get(0).label();
        }
        StringBuilder sb = new StringBuilder();
        for (Intent action : actions) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(action.label());
        }
        return sb.toString();
    }

    /**
     * 本回合每个动作**一行**的意图文案，且带**实际数值**（v0.5.0.7）。
     *
     * <p>对应两条需求：</p>
     * <ul>
     *   <li>"失去力量后，攻击数值的显示应该相应的被减少。例如敌人失去了 3 力量，意图本体是造成 8 点伤害，
     *       失去 3 力量后应该造成 5 伤害，显示也应该是造成 5 伤害。"（玩家卡牌同理，走
     *       {@code CardPreview.liveDescription}）</li>
     *   <li>"存在意图行动超出敌人UI范围导致显示不全的问题。解决方案：有多个行动则换行，
     *       而不是显示『行动1+行动2』导致显示不全。"</li>
     * </ul>
     *
     * <p>数值统一走 {@link CombatContext#previewDamage}/{@link CombatContext#previewBlock}，
     * 与真正结算**共用同一份判定**，所以头顶/面板上的数字不会和实际掉血不一致。</p>
     *
     * @param target 这回合被瞄准的参战者（没有就传 null，易伤按"无"算）
     */
    public List<String> actualIntentLines(Combatant target) {
        List<Intent> current = actions.isEmpty()
                ? (intent == null ? List.of() : List.of(intent))
                : actions;
        if (current.isEmpty()) {
            return List.of("未知");
        }
        List<String> lines = new ArrayList<>(current.size());
        for (Intent action : current) {
            lines.add(actualIntentText(action, target));
        }
        return lines;
    }

    /** 单个动作的实际意图文案。 */
    private String actualIntentText(Intent action, Combatant target) {
        return switch (action.type()) {
            case ATTACK -> {
                int base = CombatContext.previewDamage(this, target == null ? this : target, action.value());
                int total = base * Math.max(1, action.times());
                yield action.times() > 1
                        ? "攻击 " + base + "×" + action.times() + "（共 " + total + "）"
                        : "攻击 " + base;
            }
            case DEFEND -> "防御 " + CombatContext.previewBlock(this, action.value());
            case BUFF_STRENGTH -> "力量 +" + action.value();
            case BUFF_CHARGE -> "蓄能 +" + action.value();
            case HEAL -> "恢复 " + action.value();
            case DEBUFF_MINING_FATIGUE -> "施加挖掘疲劳 " + action.value();
            case DEBUFF_VULNERABLE -> "施加易伤 " + action.value();            case DEBUFF_BURN -> "施加灼烧 " + action.value();
            case DEBUFF_DECAY -> "施加凋零 " + action.value();
            case DEBUFF_POISON -> "施加中毒 " + action.value();
            case DEBUFF_SLOW -> "施加缓慢 " + action.value();
            case DEBUFF_STATUS_CARD -> "塞入状态牌 ×" + action.value();
            case SUMMON -> "召唤 ×" + action.value();
            case SUMMON_IF_FEW -> "召唤 ×" + action.value();
            case LOCK -> "自爆 " + charge();
            case UNKNOWN -> "未知";
        };
    }

    /** 本回合里"是否包含某个类型的动作"（表现层/自检用）。 */
    public boolean hasAction(IntentType type) {        for (Intent action : actions) {
            if (action.type() == type) {
                return true;
            }
        }
        return false;
    }

    /** 本次意图的目标（可能是 null：防御/蓄力类意图没有指定目标）。 */
    public Combatant target() {
        return target;
    }

    public void setTarget(Combatant target) {
        this.target = target;
    }

    /** 带目标名的意图文案，多玩家时用来告诉玩家"这一下打谁"。 */
    public String intentLabelWithTarget() {
        String label = intentLabel();
        return target == null ? label : label + " → " + target.name();
    }

    /** Convenience for the Minecraft layer: an enemy built from a single repeated intent. */
    public static List<Intent> cycle(Intent... intents) {
        List<Intent> list = new ArrayList<>(intents.length);
        for (Intent intent : intents) {
            list.add(intent);
        }
        return list;
    }
}
