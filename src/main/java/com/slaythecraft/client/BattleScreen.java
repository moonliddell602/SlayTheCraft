package com.slaythecraft.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.slaythecraft.Config;
import com.slaythecraft.combat.core.BattleAnim;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.FadeTimeline;
import com.slaythecraft.combat.core.HandFadeTracker;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.StatusCards;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.network.BattleView;
import com.slaythecraft.network.EndTurnPayload;
import com.slaythecraft.network.PlayCardPayload;
import com.slaythecraft.network.QuitBattlePayload;
import com.slaythecraft.network.UsePotionPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 出牌界面（v0.0.2 V1）：手牌横排在屏幕底部（即物品栏位置），参考《杀戮尖塔》的战斗布局。
 *
 * <p>要点：</p>
 * <ul>
 *   <li><b>不暂停游戏</b>（{@code isPauseScreen = false}），也不画模糊背景，避免遮住战斗画面；</li>
 *   <li>需要指定目标的牌：先点牌进入选目标模式，再点敌人出牌；右键/ESC 取消；</li>
 *   <li>数字键 1–9 也能直接出牌（不用鼠标时的快捷方式）；</li>
 *   <li>ESC 收起界面去看世界，本回合不再自动弹出（回合切换后恢复）。</li>
 * </ul>
 */
public class BattleScreen extends Screen {

    private static final int CARD_W = 80;
    /** v0.4.5：加高 8 像素，给卡面**插画位**腾地方（位置参照原版：插画在上半部分）。 */
    private static final int CARD_H = 96;
    /**
     * v0.5.0.5 手牌放大系数 —— **只放大宽度**。
     *
     * <p>需求原文："放大四分之一手牌的显示大小。"</p>
     *
     * <p>为什么高度不跟着放大：Minecraft 的 GUI 刻度高度常常只有 240，而 v0.5.0.6 还要把整条手牌
     * **下移** {@link #HUD_SHIFT} 去补上被隐藏的原版 HUD。高度也放大 1.25（→120）会直接顶出屏幕底部
     * —— 自检 {@code CodexPagesCheck.cardLayout} 就是这么抓出来的。所以高度保持 96，宽度 64 → 80
     * 让卡面明显变宽，观感上同样是"放大了"。</p>
     */
    public static final float HAND_SCALE = 1.25F;
    /** 原来的手牌尺寸（自检用它验证"确实放大了四分之一"）。 */
    public static final int CARD_W_BASE = 64;
    public static final int CARD_H_BASE = 96;
    /** v0.5.0.5：未悬停的手牌只露出**上半部分**（卡图 + 卡名 + 费用）。 */
    private static final int CARD_PEEK_H = 62;
    /** v0.5.0.5：悬停时手牌向上浮起的高度。 */
    private static final int CARD_HOVER_LIFT = 34;
    /** v0.5.0.5：上浮动画的插值速度（每帧靠近目标的 30%）。 */
    private static final float HOVER_LIFT_LERP = 0.30F;
    /** v0.5.0.5：未悬停时手牌略微缩小（3D 层叠感）。 */
    private static final float CARD_IDLE_SCALE = 0.94F;
    /**
     * 卡面版式（v0.5.0.15 按用户版式图重排）。
     *
     * <p>需求（配图 + 文字）：卡面从上到下依次是 <b>费用框 / 卡牌名称 / 卡图 / 类型（攻击·防御·技能…）/
     * 卡牌描述（应当适当缩放文本）</b>，最外圈 <b>边框表示稀有度且要加粗</b>。</p>
     *
     * <p>纵向分配（自底向上算，牌高怎么变都不会把最下面那块顶出牌外）：</p>
     * <pre>
     * y=  2 .. 12   卡名带（费用框压在这一带左上角；附魔/强化标记贴右）
     * y= 12 .. 50  卡图
     * y= 50 .. 59  类型标签带
     * y= 59 .. 84  效果文案框（内容按可用空间自适应缩放）
     * y= 84 .. 96  底部留白
     * </pre>
     *
     * <p><b>为什么卡名从"卡图下沿"搬到"最上面"</b>：版式图里卡名就在最上边（费用框右边）。
     * 搬上来之后下面空出来的位置正好给卡图加高（34 → 38），效果框也长高了（22 → 25），
     * 于是"卡图更大 + 描述更放得下"两个诉求一起满足。</p>
     */
    private static final int CARD_ART_X = 2;
    private static final int CARD_ART_Y = 12;
    /**
     * 卡图宽度（v0.5.0.18）。
     *
     * <p>用户给的原版卡面参考里，插画是**通栏铺满卡宽**的（只在最外圈留一点边框），
     * 原来这里是 {@code CARD_W - 8}（左右各留 4 像素），看起来像"卡里嵌了一个小方框"。
     * 现在左右各留 2 像素，视觉上接近原版的通栏插画。</p>
     */
    private static final int CARD_ART_W = CARD_W - 4;
    /**
     * 卡图高度。
     *
     * <p>v0.5.0.17：38 → **32**。反馈"手牌的描述模块出现严重错位"的根因是**描述框太窄又太矮**：
     * 框内宽原来只有 68 像素，而"造成 6 点伤害。"这种最短的正式描述就要约 72 像素 ——
     * 于是一张普通打击牌的描述**必然折行**，而框内高度只够两行，文字被挤/被缩，
     * 看起来就是"错位"。所以从卡图这里让出 4 像素、并且把内缩从 6 收到 3。
     * （用户给的原版参考里描述区约占卡高 45%、卡图约 38%；80×96 的窄牌放不下这个比例，
     * 所以先把"能读清"做扎实，比例上取折中。）</p>
     */
    private static final int CARD_ART_H = 32;
    /** 类型标签带的高度（v0.5.0.15：12 → 9，把空间让给卡图与描述）。 */
    private static final int CARD_TYPE_BAND_H = 9;
    /**
     * 效果文案框的左右内缩。
     *
     * <p>v0.5.0.16：6 → **3**。内缩从两侧各减 3 像素，框内宽 68 → **74** ——
     * 刚好让"造成 6 点伤害。"这类短描述**单行放下**（配合下面的 {@link #CARD_TEXT_MIN_W} 断言）。</p>
     */
    private static final int CARD_TEXT_INSET = 3;
    /** 效果文案框的高度（v0.5.0.17：25 → **33**，够放 3 行 9 像素高的字）。 */
    private static final int CARD_TEXT_H = 33;
    /**
     * 描述框**底部**的留白（关键字那一行画在这里）。
     *
     * <p>原版参考里描述区一直画到卡底、没有这一条；这里留 8 像素是因为本模组的卡还有
     * "保留 / 消耗 / 虚无 / 固有"这些关键字要显示（原版的对应位置是描述文字内的彩色关键字）。</p>
     */
    private static final int CARD_BOTTOM_PAD = 8;
    /** 卡名带的高度（最上面那一条：费用框 + 卡名 + 附魔/强化标记）。 */
    private static final int CARD_NAME_BAND_H = 10;
    /** 卡名带里文字的 y 偏移（9 像素高的字在这条带里垂直居中）。 */
    private static final int CARD_NAME_TEXT_DY = 3;
    /** 卡名带**上方**的留白（让加粗边框不贴字）。 */
    private static final int CARD_TOP_PAD = 2;
    /** 卡图与类型带之间的留白（v0.5.0.16：让"卡图 / 类型 / 描述"三块的分隔均匀可辨）。 */
    private static final int CARD_ART_GAP = 2;
    /**
     * 描述框的**最小内宽**（自检用）。
     *
     * <p>这张卡上最短的正式描述是"造成 6 点伤害。"（5 个全角 + 1 个半角 ≈ 72 像素），
     * 所以内宽必须 ≥ 这个数，否则打击牌的描述就要折行。</p>
     */
    public static final int CARD_TEXT_MIN_W = 72;
    /**
     * 卡面**稀有度边框的粗细**（v0.5.0.15）。
     *
     * <p>需求原文："边框需要加粗以更好地显示卡牌稀有度。" 原来是 1 像素，在 80×96 的卡上太细，
     * 品质色几乎看不出来；现在 2 像素（自检断言 ≥ 2）。</p>
     */
    private static final int CARD_BORDER = 2;
    private static final int CARD_GAP = 6;
    /**
     * 手牌底边距（v0.5.0.6：原来是 8，改成 28）。
     *
     * <p>必须**大于等于** {@link #HUD_SHIFT}：手牌的 y 是
     * {@code 屏幕高 − CARD_H − 这个边距 + HUD_SHIFT}，若边距小于下移量，底边就会跑到屏幕外
     * （自检 {@code CodexPagesCheck.cardLayout} 会抓到）。边距 28 = 下移 26 + 2 像素空隙。</p>
     */
    private static final int HAND_BOTTOM_MARGIN = 30;
    /**
     * 战斗 UI 整体**下移**的像素（v0.5.0.6）。
     *
     * <p>需求原文："卡牌位置过高。应该向下（正下位置，可以隐藏物品栏，饱食度，经验值和生命值等UI
     * 来达到只显示卡牌的效果。）。届时，也要一并移动药水栏，状态显示位置和结束回合按钮。"</p>
     *
     * <p>被隐藏的原版 HUD（物品栏/饱食度/经验/生命）在左下与底部占了大约一条，所以这里把
     * 手牌、能量、抽牌堆、药水栏、结束回合按钮一起下移这么多，正好落进腾出来的空间。</p>
     */
    private static final int HUD_SHIFT = 26;
    /** 本帧生效的下移量（战斗内才下移；非战斗查看牌组等界面保持原位）。 */
    private int hudShift() {
        return ClientBattleState.view().active() ? HUD_SHIFT : 0;
    }
    private static final int ENEMY_ROW_Y = 26;
    private static final int ENEMY_W = 116;
    /** 敌人面板的行高（名字 / 生命与格挡 / 每条意图）。 */
    private static final int ENEMY_LINE_H = 11;
    /** 意图第一行的 y 偏移（在名字与生命行之下）。 */
    private static final int ENEMY_INTENT_TOP = 30;

    /**
     * 一条意图的 y 偏移（v0.5.0.7：多行动换行）。
     *
     * @param line 第几行（0 起）
     */
    public static int intentLineY(int line) {
        return ENEMY_INTENT_TOP + Math.max(0, line) * ENEMY_LINE_H;
    }

    /**
     * 敌人面板的高度（v0.5.0.7：按意图行数自适应）。
     *
     * <p>需求："有多个行动则换行，而不是显示『行动1+行动2』导致显示不全。" 所以面板要跟着变高，
     * 否则换行之后最后几行还是会被裁掉。底部再留 12 像素给状态图标那一排。</p>
     */
    public static int enemyHeight(BattleView.EnemyView enemy) {
        int lines = Math.max(1, enemy.intents().size());
        return intentLineY(lines - 1) + 10 + 14;
    }

    private static final int PANEL_BG = 0xC0101016;
    private static final int PANEL_EDGE = 0xFF5A5A64;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int TEXT_DIM = 0xFF9A9AA4;
    private static final int GOLD = 0xFFFFD24A;
    private static final int DANGER = 0xFFD05A5A;
    private static final int POTION_GOOD = 0xFF5AD05A;
    /** V8：药水槽位尺寸。 */
    private static final int POTION_SIZE = 22;
    private static final int POTION_GAP = 4;
    /** v0.0.3：自己状态块的宽度上限（超过就换行）。 */
    private static final int STATUS_BLOCK_W = 150;

    private int targetingCard = -1;
    /** V8：正在等待选择目标的药水槽位（−1 = 没有）。 */
    private int potionTargeting = -1;
    /** v0.2.3：「放弃战斗」的二次确认状态。 */
    private boolean confirmQuit;
    /** v0.3.0：每张手牌的"出现时刻"，用来做淡入（下标 → 时间戳）。 */
    private final Map<String, Long> cardAppearAt = new HashMap<>();

    /**
     * 手牌当前的**显示 x**（v0.4.6.7 手牌进入动画）。
     *
     * <p>需求原文："抽到的手牌从左到右依次渐显，同时，第一张抽到的手牌放置到屏幕中心，之后在抽到
     * 下一张牌再次进入显示动画，已抽到的手牌向两侧展开，后续抽出的牌也使用这种动画。手牌的排序稳定
     * 中心对齐，以防出现手牌一直向左或右移动超出屏幕的问题（即手牌的左右位置对称，4 张牌则左 2 右 2，
     * 5 张牌则左 2 中 1 右 2）。"</p>
     *
     * <p>做法：真正的槽位就是**居中对称**的（见 {@link #handSlotX}），这里额外让每张牌从"屏幕中心"
     * 平滑滑到自己的槽位 —— 于是第一张牌停在中心，后面每抽一张，已有的牌都会被推向两侧。</p>
     */
    private final Map<String, Float> handSlideX = new HashMap<>();

    /**
     * 每张手牌当前的**上浮高度**（v0.5.0.5）。
     *
     * <p>需求："在鼠标悬停在相当位置的手牌上时，手牌会被向上拉（需要一个向上浮动的动画）
     * 以显示下半段的文字描述。未打出则回到原位。"</p>
     *
     * <p>按牌的键存当前值，每帧朝目标（悬停 = {@link #CARD_HOVER_LIFT}，否则 0）插值，
     * 于是浮起和落回都是平滑的，而不是瞬移。</p>
     */
    private final Map<String, Float> handHoverLift = new HashMap<>();

    /** 手牌滑入的每帧插值系数（越大越快）。 */
    private static final float HAND_SLIDE_LERP = 0.35F;
    /** 依次渐显的间隔（毫秒）：抽到的手牌从左到右一张张亮起来。 */
    private static final long HAND_STAGGER_MS = 70L;
    /** 上一帧手牌的键（id + 强化标记），用来判断哪些牌是新抽到的。 */
    private List<String> lastHandKeys = List.of();
    /** 上一帧的回合数：换回合时整手牌都算"新抽到"的。 */
    private int lastTurn = -1;

    public BattleScreen() {
        super(Component.translatable("stc.battle.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 不画暗化/模糊背景：这是战斗 HUD，不是模态窗口。 */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private BattleView view() {
        return ClientBattleState.view();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BattleView view = view();
        // v0.5.1.6 回合模型：玩家回合里**一直**画（自己确认之后也要留在屏幕上等队友）。
        // v0.5.1.9：敌怪回合也画 —— 界面在屏幕上 = 玩家不能走动、不能转视角
        // （需求原文："出牌回合结束后的敌怪环节，出现了玩家可能会暂时离开战斗UI，
        // 可以按 wasd 移动和旋转视角的问题。"）。敌怪回合里手牌只是"看得见、点不动"。
        if (!view.active()) {
            return;
        }
        boolean waiting = ClientBattleState.isWaitingForOthers();
        // v0.5.1.9：敌怪回合 —— 画提示、手牌变暗且不响应悬停/拖动
        boolean enemyPhase = ClientBattleState.isEnemyPhase();
        if (!view.yourTurn()) {
            // 轮不到我出牌（敌怪回合 / 我已确认等队友）时把拖动状态扔掉，
            // 否则被打断的那次长按会跨回合留在界面上
            clearDrag();
        }

        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
        currentPotionSlots = Math.max(1, view.potionSlots());
        ensureAligned();
        drawTopBar(graphics, view);
        // v0.5.1.3 需求："显示已经拥有的遗物（显示其小图标）鼠标在遗物上悬停以查看描述。"
        drawRelicBar(graphics, mouseX, mouseY);
        drawEnemies(graphics, view, mouseX, mouseY);
        drawPiles(graphics, view, mouseX, mouseY);
        drawPotions(graphics, view);
        drawHand(graphics, view, mouseX, mouseY);
        // v0.5.1.8：拖动打出的"打出区"与跟着鼠标的那张牌（只在自己回合画）
        if (view.yourTurn()) {
            drawDragInteraction(graphics, view, mouseX, mouseY);
        }
        // v0.5.0.5：打牌动画画在普通手牌之上
        drawPlayedCards(graphics, view);
        if (enemyPhase) {
            // v0.5.1.9：明确告诉玩家"现在是敌怪在动，你只能看"
            // v0.5.5.1（用户）："存在药水栏和敌人行动文字重叠问题" —— 提示行搬到**药水栏上方**
            Component note = Component.translatable("stc.battle.enemy_phase");
            TextStyle.draw(graphics, font, trim(note.getString(), width - 24),
                    (width - Math.min(font.width(note), width - 24)) / 2, hintLineY(), DANGER);
        } else if (waiting) {
            // v0.5.1.6：已确认结束回合 → 明确告诉玩家在等谁（否则界面看起来像卡住了）
            List<String> waitingFor = view.waitingFor();
            Component note = waitingFor.isEmpty()
                    ? Component.translatable("stc.battle.waiting.any")
                    : Component.translatable("stc.battle.waiting", String.join("、", waitingFor));
            TextStyle.draw(graphics, font, trim(note.getString(), width - 24),
                    (width - Math.min(font.width(note), width - 24)) / 2, hintLineY(), GOLD);
        }
        if (targetingCard >= 0) {
            Component hint = Component.translatable("stc.battle.choose_target");
            TextStyle.draw(graphics, font, trim(hint.getString(), width - 24), (width - Math.min(font.width(hint),
                    width - 24)) / 2, hintLineY(), GOLD);
        } else if (potionTargeting >= 0) {
            Component hint = Component.translatable("stc.battle.choose_potion_target");
            TextStyle.draw(graphics, font, trim(hint.getString(), width - 24), (width - Math.min(font.width(hint),
                    width - 24)) / 2, hintLineY(), DANGER);
        }
        // v0.5.0.19：一帧结束 —— 刷新卡面绘制诊断（纯诊断，写失败也不影响绘制）
        CardDrawLog.endFrame();
    }

    /** V8：药水槽位（手牌上方一排）。 */
    private void drawPotions(GuiGraphics graphics, BattleView view) {
        // v0.4.6：【药水腰带】会让栏位 +2
        int slots = Math.max(1, view.potionSlots());
        int startX = potionBarStartX(slots);
        int y = potionBarY();
        for (int i = 0; i < slots; i++) {
            int x = startX + i * (POTION_SIZE + POTION_GAP);
            graphics.fill(x, y, x + POTION_SIZE, y + POTION_SIZE, PANEL_BG);
            boolean selected = i == potionTargeting;
            border(graphics, x, y, x + POTION_SIZE, y + POTION_SIZE, selected ? GOLD : PANEL_EDGE);
            if (i >= view.potions().size()) {
                continue;
            }
            Potion potion = PotionLibrary.byId(view.potions().get(i).getPath());
            if (potion == null) {
                continue;
            }
            int accent = potion.kind().canTargetEnemy() ? DANGER : POTION_GOOD;
            border(graphics, x + 1, y + 1, x + POTION_SIZE - 1, y + POTION_SIZE - 1, accent);
            // v0.4.6：画成原版药水贴图（常规 / 喷溅 / 滞留），不再用文字代替
            PotionIcons.draw(graphics, potion, x + 3, y + 3, 1.0F);
        }
        // v0.0.3 W1：自己的效果显示在药水栏**左边**，逐条换行
        drawSelfStatuses(graphics, view, startX - 8, y);
    }

    /**
     * 自己的状态：从药水栏左侧往左排，超出一行就换行（最多 4 行，避免压到手牌）。
     *
     * <p>v0.4.6：每条状态左侧画**原版状态图标**（8×8，与文字同高），
     * 并且鼠标在一条状态上停留 0.6 秒会显示它的功能说明。</p>
     *
     * <p>v0.5.5.1（用户）："战斗内的状态常态不再显示名称，只显示图标和数字。悬停显示不变。"
     * 加上"添加获得状态时的动画：渐显" —— 常态走 {@link StatusIcons#drawCompactLine}（图标 + 数字），
     * 新出现的状态在 {@link #STATUS_FADE_MS} 内从透明渐显；悬停提示仍用**完整条目**。</p>
     */
    private void drawSelfStatuses(GuiGraphics graphics, BattleView view, int rightEdge, int topY) {
        List<String> statuses = view.selfStatuses();
        if (statuses.isEmpty()) {
            HoverTracker.update("");
            statusAppearAt.clear();
            return;
        }
        int lineY = topY;
        int width = 0;
        List<String> line = new java.util.ArrayList<>();
        List<List<String>> lines = new java.util.ArrayList<>();
        for (String status : statuses) {
            int itemWidth = StatusIcons.compactWidth(status, font.width(StatusIcons.valueOf(status)));
            int needed = width + (line.isEmpty() ? 0 : font.width(" · ")) + itemWidth;
            if (needed > STATUS_BLOCK_W && !line.isEmpty()) {
                lines.add(List.copyOf(line));
                line.clear();
                width = 0;
                needed = itemWidth;
            }
            line.add(status);
            width = needed;
        }
        lines.add(List.copyOf(line));

        long now = System.currentTimeMillis();
        java.util.Set<String> alive = new java.util.HashSet<>();
        String hovered = null;
        for (List<String> current : lines) {
            if (lineY > topY + 30) {
                break;
            }
            float[] alphas = new float[current.size()];
            for (int i = 0; i < current.size(); i++) {
                alphas[i] = statusAlpha(current.get(i), now);
                alive.add(current.get(i));
            }
            List<int[]> rects = StatusIcons.drawCompactLine(graphics, current, rightEdge, lineY, GOLD, alphas);
            for (int i = 0; i < rects.size(); i++) {
                int[] rect = rects.get(i);
                if (isInside(pendingMouseX, pendingMouseY, rect[0], rect[1], rect[2], rect[3])) {
                    hovered = current.get(i);
                }
            }
            lineY += 10;
        }
        // 已经消失的状态丢掉渐显记录（下次再出现时重新渐显）
        statusAppearAt.keySet().retainAll(alive);

        // v0.4.6：悬停 0.6 秒显示状态说明
        if (HoverTracker.update(hovered == null ? "" : hovered) && hovered != null) {
            showTooltip(graphics, StatusIcons.describe(hovered), pendingMouseX, pendingMouseY);
        }
    }

    // ------------------------------------------------------------------ v0.5.5.1 状态渐显

    /** "获得状态时渐显"的时长（毫秒）。 */
    private static final long STATUS_FADE_MS = 320L;

    /** 每条状态第一次出现的时间戳（键 = 状态条目文本；数字变化不算"新获得"）。 */
    private final java.util.Map<String, Long> statusAppearAt = new java.util.HashMap<>();

    /** 某条状态**这一次**出现的时间戳（第一次见到就记下，之后一直用同一个）。 */
    private long statusAppearAt(String entry, long now) {
        return statusAppearAt.computeIfAbsent(entry, key -> now);
    }

    /** 某条状态现在的透明度：刚出现时 0 → {@link #STATUS_FADE_MS} 之后 1。 */
    private float statusAlpha(String entry, long now) {
        long appear = statusAppearAt(entry, now);
        float alpha = (now - appear) / (float) STATUS_FADE_MS;
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }


    /** v0.4.6：本帧的药水栏位数（含【药水腰带】加成；render 开头从快照里读）。 */
    private int currentPotionSlots = Config.potionSlots();

    /** 本帧鼠标位置（drawSelfStatuses 里做命中判定用；render 开头写入）。 */
    private int pendingMouseX;
    private int pendingMouseY;

    /** 显示一个多行提示框（v0.4.6 的状态 / 充能球说明都用它）。 */
    /**
     * 卡面内部各块的 y 偏移（相对牌的顶边）—— 抽成可断言的纯函数（v0.5.0.6）。
     *
     * <p>版式（自上而下，用户给的版式图）：卡图 → 卡名 → 效果文案框 → 类型标签带（贴着效果框下沿）。</p>
     *
     * <p>排布是**自底向上**算的，这样牌高怎么变都不会把最下面那块顶出牌外：
     * 效果框底边 = 牌高 − {@link #CARD_BOTTOM_PAD}；类型带紧贴效果框上沿。</p>
     *
     * @return {卡图上, 卡图下, 卡名文字 y, 效果框上, 效果框下, 类型带上, 类型带下, 牌高,
     *         卡名带上, 卡名带下}
     */
    public static int[] cardLayoutOffsets() {
        int nameTop = CARD_TOP_PAD;
        int nameBottom = nameTop + CARD_NAME_BAND_H;
        int artTop = nameBottom;
        int typeTop = artTop + CARD_ART_H + CARD_ART_GAP;
        int textTop = typeTop + CARD_TYPE_BAND_H;
        int textBottom = textTop + CARD_TEXT_H;
        // 注意：数组第 6 位是"类型带的**下沿**"，也就是 textTop（类型带与效果框首尾相接）
        return new int[] {artTop, artTop + CARD_ART_H, nameTop + CARD_NAME_TEXT_DY, textTop, textBottom,
                typeTop, textTop, CARD_H, nameTop, nameBottom};
    }

    /** 卡面各块之间的**全部**固定留白之和（自检用：各块高度 + 留白必须精确等于牌高）。 */
    public static int cardFixedPadTotal() {
        return CARD_TOP_PAD + CARD_ART_GAP + CARD_BOTTOM_PAD;
    }

    /** 类型带的底边（贴着效果框上沿）。 */
    private static int typeBottom() {
        return CARD_H - CARD_BOTTOM_PAD - CARD_TEXT_H;
    }

    /** 卡面稀有度边框的粗细（自检用：需求要求"加粗"）。 */
    public static int cardBorderThickness() {
        return CARD_BORDER;
    }

    /** 卡面底部留白（自检用：验证各块高度之和 + 留白 = 牌高）。 */
    public static int cardBottomPad() {
        return CARD_BOTTOM_PAD;
    }

    /**
     * 描述框里**文字实际能用的宽度**（自检与绘制共用同一份定义）。
     *
     * <p>⚠️ 这里有个我踩过的算术错误：一开始写成 {@code CARD_W - CARD_TEXT_INSET * 2 - 6}，
     * 把"文字留白"减了**两次**（框本身已经是 {@code CARD_W - CARD_TEXT_INSET * 2} = 74 宽，
     * 文字再从左边内缩 3 起画）。结果函数报 68、而实际可用是 74 附近，
     * 我自己被这个数字误导、把一个已经够宽的框又去改窄。
     * **绘制、折行、自检必须共用这一个函数**。</p>
     */
    public static int textBoxInnerW() {
        // 框宽 = CARD_W − 左右内缩；文字再从左边各缩 3 像素
        return CARD_W - CARD_TEXT_INSET * 2;
    }

    /** 描述框的**内高**（去掉上下各 2 像素的文字留白）。 */
    public static int textBoxInnerH() {
        return CARD_TEXT_H - 4;
    }

    /** 描述框能画几行（自检用）。 */
    public static int textBoxLines() {
        return Math.max(1, textBoxInnerH() / 9);
    }

    /** 卡面各块的高度（自检用：{卡图高, 类型带高, 效果框高, 卡名带宽}）。 */
    public static int[] cardBlockHeights() {
        return new int[] {CARD_ART_H, CARD_TYPE_BAND_H, CARD_TEXT_H, CARD_NAME_BAND_H};
    }

    /** 卡图矩形（自检用：{x 偏移, y 偏移, 宽, 高}，相对牌的左上角）。 */
    public static int[] cardArtRect() {
        return new int[] {CARD_ART_X, CARD_ART_Y, CARD_ART_W, CARD_ART_H};
    }

    /** 手牌相邻槽位的间距（自检与诊断日志用）。 */
    public static int handSlotStride() {
        return CARD_W + CARD_GAP;
    }

    /** 手牌槽位的 y（顶边）—— 自检用它验证"卡牌确实下移了"（v0.5.0.6）。 */
    public static int handYFor(int screenHeight, boolean inBattle) {
        return screenHeight - CARD_H - HAND_BOTTOM_MARGIN + (inBattle ? HUD_SHIFT : 0);
    }

    /** 战斗内通用的下移量（自检用）。 */
    public static int hudShiftAmount() {
        return HUD_SHIFT;
    }

    /**
     * 左侧**遗物栏**的底边（v0.5.1.3）。
     *
     * <p>遗物栏画在状态框下面那一列空白里，但不能压到左下角的能量框和它上面的
     * 「查看我的牌组」按钮（按钮顶边 = 能量框顶边 − 22，高 18），所以再留 4 像素余量。</p>
     */
    public static int relicBarBottomLimit(int guiHeight) {
        return guiHeight - CARD_H - 10 + HUD_SHIFT - 22 - 4;
    }

    /**
     * 画左侧遗物栏（v0.5.1.3 美术需求）。
     *
     * <p>需求原文："显示已经拥有的遗物（显示其小图标）鼠标在遗物上悬停以查看描述。"
     * 数据直接用同步过来的档案（{@code PlayerCardData.relics()}），不需要新的网络包。</p>
     */
    private void drawRelicBar(GuiGraphics graphics, int mouseX, int mouseY) {
        PlayerCardData data = BinderStyle.data();
        if (data == null) {
            return;
        }
        BattleRelicBar.draw(graphics, font, data.relics(), height, width, mouseX, mouseY);
    }

    /** 手牌底边距（自检用：验证"下移量 < 底边距"这条硬约束）。 */
    public static int handBottomMargin() {
        return HAND_BOTTOM_MARGIN;
    }

    // ------------------------------------------------------------------ v0.5.1.8：拖动打出

    /**
     * 长按多久才算"可以开始拖动"（毫秒）。
     *
     * <p>需求："长按卡面可以执行拖动操作，拖动到屏幕中心后松开可以执行卡牌效果。"</p>
     */
    public static final long DRAG_HOLD_MS = 180L;
    /** 打出区（屏幕中心那块矩形）的目标尺寸。 */
    private static final int PLAY_ZONE_W = 190;
    private static final int PLAY_ZONE_H = 130;
    /**
     * 轮不到我出牌时手牌的透明度（v0.5.1.9）。
     *
     * <p>敌怪回合 / 已确认等队友：牌还在手牌区摆着（信息不能丢），但明显变暗，
     * 表示"看得见、出不了"。</p>
     */
    private static final float INACTIVE_HAND_ALPHA = 0.45F;

    /** 拖动交互的阶段（纯逻辑，自检直接算）。 */
    public enum DragStage {
        /** 没按下，或者这是一张**指向性**牌（指向性牌仍然是"点牌 → 点敌人"）。 */
        NONE,
        /** 按住但还没到长按时间。 */
        PRESSING,
        /** 已经长按够久，进入拖动。 */
        DRAGGING
    }

    /**
     * 纯逻辑：现在处于拖动的哪个阶段。
     *
     * @param pressed  鼠标是否按在一张可拖动的牌上
     * @param heldMs   已经按了多久
     * @param targeted 这张牌是不是指向性（指向性牌不走拖动，保持"点牌 → 点敌人"）
     */
    public static DragStage dragStage(boolean pressed, long heldMs, boolean targeted) {
        if (!pressed || targeted) {
            return DragStage.NONE;
        }
        return heldMs >= DRAG_HOLD_MS ? DragStage.DRAGGING : DragStage.PRESSING;
    }

    /**
     * **打出区**的矩形（屏幕中心，供拖放）：{@code {x, y, w, h}}。
     *
     * <p>竖直方向夹在"屏幕中偏上"和"手牌上沿"之间 —— 矮窗口（GUI 刻度高度只有 240 左右）时
     * 手牌上沿会往上顶，这里必须让开，否则打出区会盖住手牌。</p>
     */
    public static int[] playZoneRect(int width, int height) {
        int w = Math.min(PLAY_ZONE_W, Math.max(40, width - 40));
        int handTop = handYFor(height, true);
        int h = Math.min(PLAY_ZONE_H, Math.max(40, height / 3));
        int y = Math.max(8, (height - h) / 2 - 10);
        // 手牌上方留 8 像素
        h = Math.min(h, Math.max(24, handTop - 8 - y));
        int x = (width - w) / 2;
        return new int[] {x, y, w, h};
    }

    /** 松手时是否落在打出区里。 */
    public static boolean isInPlayZone(double mouseX, double mouseY, int width, int height) {
        int[] zone = playZoneRect(width, height);
        return mouseX >= zone[0] && mouseX < zone[0] + zone[2]
                && mouseY >= zone[1] && mouseY < zone[1] + zone[3];
    }

    /** 按在手里的牌（-1 = 没按着）。 */
    private int pressedCard = -1;
    /** 按下的时刻。 */
    private long pressedAt;
    /** 正在拖动的那张（-1 = 没在拖）。 */
    private int dragCard = -1;

    private void clearDrag() {
        pressedCard = -1;
        dragCard = -1;
    }

    /**
     * 这张手牌是不是"**正被拖在鼠标上**"（v0.5.5.1 需求：拖动时原位置不再显示）。
     *
     * <p>分两段看：{@code dragCard} 是已经进入拖动阶段的；还没到长按时长但已经"长按够久"的
     * 也算（这样拖动刚开始的那一帧原位置就已经空了，不会闪一下）。</p>
     */
    private boolean isDraggingIndex(int index) {
        if (index < 0 || pressedCard != index) {
            return false;
        }
        return dragCard == index
                || dragStage(true, System.currentTimeMillis() - pressedAt, false) == DragStage.DRAGGING;
    }

    /**
     * 画拖动交互：屏幕中间的**打出区** + 跟着鼠标的那张牌（v0.5.1.8）。
     *
     * <p>"长按够久就进入拖动"的判断放在这里（每帧一次），所以玩家长按之后**不用先移动鼠标**
     * 也会进入可拖动状态 —— 符合需求"长按卡面可以执行拖动操作"。</p>
     */
    private void drawDragInteraction(GuiGraphics graphics, BattleView view, int mouseX, int mouseY) {
        if (pressedCard < 0) {
            return;
        }
        boolean dragging = dragCard >= 0
                || dragStage(true, System.currentTimeMillis() - pressedAt, false) == DragStage.DRAGGING;
        if (dragging) {
            dragCard = pressedCard;
        }

        int[] zone = playZoneRect(width, height);
        boolean affordable = pressedCard < view.hand().size() && view.hand().get(pressedCard).cost() <= view.energy();
        boolean hot = dragging && isInPlayZone(mouseX, mouseY, width, height);
        int edge = !hot ? PANEL_EDGE : (affordable ? GOLD : DANGER);
        graphics.fill(zone[0], zone[1], zone[0] + zone[2], zone[1] + zone[3],
                hot ? (affordable ? 0x50FFD24A : 0x40D05A5A) : 0x30101016);
        border(graphics, zone[0], zone[1], zone[0] + zone[2], zone[1] + zone[3], edge);
        String hint = Component.translatable(!hot ? (dragging ? "stc.battle.drag.release" : "stc.battle.drag.hold")
                : (affordable ? "stc.battle.drag.release" : "stc.battle.drag.no_energy")).getString();
        TextStyle.draw(graphics, font, hint, zone[0] + (zone[2] - font.width(hint)) / 2,
                zone[1] + (zone[3] - 8) / 2, hot ? edge : TEXT_DIM);

        // 跟着鼠标画那张牌（手牌可能在拖动期间被服务端改掉，所以下标要防越界）
        if (dragging && pressedCard < view.hand().size()) {
            BattleView.HandCard card = view.hand().get(pressedCard);
            drawCard(graphics, view, card, pressedCard, mouseX - CARD_W / 2, mouseY - CARD_H / 2, false, 1.0F);
        }
    }

    /** 卡面尺寸与内部块尺寸（自检用：{宽, 高, 卡图高, 类型带高, 效果框高}）。 */
    public static int[] cardMetrics() {
        return new int[] {CARD_W, CARD_H, CARD_ART_H, CARD_TYPE_BAND_H, CARD_TEXT_H};
    }

    /** 手牌放大系数（自检用）。 */
    public static float handScale() {
        return HAND_SCALE;
    }

    private void showTooltip(GuiGraphics graphics, List<String> lines, int mouseX, int mouseY) {
        List<net.minecraft.network.chat.Component> components = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(net.minecraft.network.chat.Component.literal(line));
        }
        graphics.renderTooltip(font, components,
                java.util.Optional.<net.minecraft.world.inventory.tooltip.TooltipComponent>empty(), mouseX, mouseY);
    }

    // ------------------------------------------------------------------ 绘制

    /** v0.4.3：**结束回合**按钮（药水栏右边）的坐标。 */
    private int quitButtonX() {
        // v0.4.6：栏位数要跟着【药水腰带】走，否则多出来的槽位会和"结束回合"按钮叠在一起
        int slots = Math.max(1, currentPotionSlots);
        return potionBarStartX(slots) + slots * (POTION_SIZE + POTION_GAP) + 6;
    }

    private int quitButtonY() {
        return potionBarY();
    }

    /** 左上角状态模块（v0.4.2：加边框、格挡另起一行、位置略微下移）。 */
    private static final int STATE_X = 8;
    private static final int STATE_Y = 26;
    private static final int STATE_W = 132;
    private static final int STATE_H = 44;

    private void drawTopBar(GuiGraphics graphics, BattleView view) {
        // v0.2.3：所有自绘文字都要按可用宽度裁剪，否则长名字/长状态会直接顶出屏幕
        int leftLimit = width - 260;
        // v0.4.2：需求"为当前回合数和生命添加边框来让玩家更方便地看见当前自己的状态"
        graphics.fill(STATE_X, STATE_Y, STATE_X + STATE_W, STATE_Y + STATE_H, PANEL_BG);
        border(graphics, STATE_X, STATE_Y, STATE_X + STATE_W, STATE_Y + STATE_H, PANEL_EDGE);
        // v0.5.1.9：敌怪回合时服务端算不出"当前行动玩家"（activePlayer 为空），
        // 这时退回不带名字的写法，免得多人战斗里画出"第 3 回合 · "
        Component turn = view.party().size() > 1 && !view.activePlayer().isEmpty()
                ? Component.translatable("stc.battle.turn.multi", view.turn(), view.activePlayer())
                : Component.translatable("stc.battle.turn", view.turn());
        TextStyle.draw(graphics, font, trim(turn.getString(), leftLimit), STATE_X + 6, STATE_Y + 5, TEXT);
        // v0.4.2：格挡与生命换行显示（格挡在生命下方）
        // v0.5.1.2 美术需求："战斗内生命前红心、格挡前盾牌" —— 文字相应右移，并按**面板宽度**
        // （而不是屏幕宽度）裁剪，所以图标不会把文字顶出状态框
        Component hp = Component.translatable("stc.battle.hp", view.hp(), view.maxHp());
        int hpX = STATE_X + 6 + HudIcons.HEART_W + HudIcons.GAP;
        HudIcons.heart(graphics, STATE_X + 6, STATE_Y + 19, DANGER);
        TextStyle.draw(graphics, font, trim(hp.getString(), STATE_W - 12 - HudIcons.HEART_W - HudIcons.GAP), hpX,
                STATE_Y + 18, GOLD);
        Component block = Component.translatable("stc.battle.block", view.block());
        int blockX = STATE_X + 6 + HudIcons.SHIELD + HudIcons.GAP;
        HudIcons.shield(graphics, STATE_X + 6, STATE_Y + 30);
        TextStyle.draw(graphics, font, trim(block.getString(), STATE_W - 12 - HudIcons.SHIELD - HudIcons.GAP),
                blockX, STATE_Y + 31, TEXT);

        // 能量（左下，手牌左侧）
        int orbX = 16;
        int orbY = height - CARD_H - 10 + hudShift();
        graphics.fill(orbX - 2, orbY - 2, orbX + 44, orbY + 30, PANEL_BG);
        border(graphics, orbX - 2, orbY - 2, orbX + 44, orbY + 30, PANEL_EDGE);
        Component energy = Component.translatable("stc.battle.energy", view.energy(), view.energyPerTurn());
        TextStyle.draw(graphics, font, energy, orbX + 2, orbY + 10, GOLD);

        // v0.4.2：能量上方的「查看我的牌组」
        button(graphics, orbX - 2, orbY - 22, 76, 18,
                Component.translatable("stc.battle.deck.open").getString(), true);

        // v0.4.3：两个按钮对调 —— **结束回合**在药水栏右边（主操作顺手），**放弃战斗**回到右上角
        // v0.5.1.6：确认过之后按钮变成「已结束回合」（置灰），玩家一眼能看出是在等队友
        // v0.5.1.9：敌怪回合单独一行文案（这时既不是"可以结束回合"也不是"我已结束在等队友"）
        int endW = 104;
        int endX = quitButtonX();
        int endY = quitButtonY();
        String endKey = view.yourTurn() ? "stc.battle.end_turn"
                : (view.enemyTurn() ? "stc.battle.enemy_turn" : "stc.battle.end_turn.done");
        button(graphics, endX, endY, endW, POTION_SIZE + 2,
                Component.translatable(endKey).getString(),
                view.yourTurn());
        int quitW = 78;
        int quitX = width - quitW - 12;
        button(graphics, quitX, 10, quitW, 18,
                Component.translatable(confirmQuit ? "stc.battle.quit.confirm" : "stc.battle.quit").getString(),
                true);
        if (confirmQuit) {
            // 需求："在二次确认放弃战斗时，高亮提示玩家"
            Component note = Component.translatable("stc.battle.quit.note");
            int noteW = Math.min(width - 16, font.width(note) + 16);
            int noteX = Math.max(4, Math.min(width - noteW - 4, quitX + quitW / 2 - noteW / 2));
            int noteY = 32;
            graphics.fill(noteX, noteY, noteX + noteW, noteY + 18, 0xE0201010);
            border(graphics, noteX, noteY, noteX + noteW, noteY + 18, DANGER);
            TextStyle.draw(graphics, font, trim(note.getString(), noteW - 8), noteX + 4, noteY + 5, DANGER);
        }
        long hidden = ClientBattleState.hiddenRemaining();
        if (hidden > 0L) {
            Component hint = Component.translatable("stc.battle.hidden", (hidden + 999L) / 1000L);
            TextStyle.draw(graphics, font, trim(hint.getString(), width - 24), STATE_X,
                    STATE_Y + STATE_H + 6, TEXT_DIM);
        }
    }

    private static final int PILE_W = 84;
    private static final int PILE_H = 18;
    private static final int PILE_GAP = 4;

    /** 抽牌堆（能量下方）的矩形。 */
    private int drawPileX() {
        return 16 - 2;
    }

    private int drawPileY() {
        return height - CARD_H - 10 + 34 + hudShift();
    }

    /** 消耗堆（弃牌堆上方）的矩形。 */
    private int exhaustPileX() {
        return width - PILE_W - 8;
    }

    private int exhaustPileY() {
        return height - PILE_H * 2 - PILE_GAP - 8;
    }

    /** 弃牌堆（右下角）的矩形。 */
    private int discardPileX() {
        return width - PILE_W - 8;
    }

    private int discardPileY() {
        return height - PILE_H - 8;
    }

    private void pileBox(GuiGraphics graphics, int x, int y, String label, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, x, y, PILE_W, PILE_H);
        graphics.fill(x, y, x + PILE_W, y + PILE_H, hovered ? 0xFF3A3A48 : PANEL_BG);
        border(graphics, x, y, x + PILE_W, y + PILE_H, hovered ? GOLD : PANEL_EDGE);
        TextStyle.draw(graphics, font, font.plainSubstrByWidth(label, PILE_W - 8), x + 4, y + 5,
                hovered ? GOLD : TEXT_DIM);
    }

    /** v0.4.2：三个牌堆拆开画（抽牌堆 / 弃牌堆 / 消耗堆），都可以点开看内容。 */
    private void drawPiles(GuiGraphics graphics, BattleView view, int mouseX, int mouseY) {
        pileBox(graphics, drawPileX(), drawPileY(),
                Component.translatable("stc.battle.pile.draw", view.drawPile()).getString(), mouseX, mouseY);
        pileBox(graphics, discardPileX(), discardPileY(),
                Component.translatable("stc.battle.pile.discard", view.discardPile()).getString(), mouseX, mouseY);
        pileBox(graphics, exhaustPileX(), exhaustPileY(),
                Component.translatable("stc.battle.pile.exhaust", view.exhaustPile()).getString(), mouseX, mouseY);
    }

    /**
     * 这一场敌人的信息栏横坐标（**整齐的等距布局**）。
     *
     * <p>v0.4.2 曾让 UI 去跟随实体，v0.4.4 按反馈反过来：**UI 是基准** —— 栏位固定在屏幕上等距排开，
     * 由客户端把这份布局发给服务端，让怪物被摆到"正好投影在栏位下面"的位置
     * （见 {@code CombatManager.alignEnemies}）。所以这里永远返回等距坐标。</p>
     */
    /**
     * 敌人信息栏的横坐标（等距居中）。
     *
     * <p>v0.5.0.7 需求："召唤同伴/仆从（如唤魔者召唤恼鬼等操作）时，先前被击杀的同伴UI未被清除，
     * 导致生成的实体位置和UI位置都会超出屏幕。"</p>
     *
     * <p>根因：这里原来按 {@code enemies().size()} 均分居中，而**已阵亡的敌人仍在名单里**。
     * 于是每死一只、每召一只，"参与均分的数量"只增不减，面板整体越铺越宽，最后被挤出屏幕两边。
     * 现在只按**存活**的敌人排布；阵亡的给一个屏幕外的坐标（它们也不该被点选）。</p>
     */
    private int[] enemyPanelXs(BattleView view) {
        List<BattleView.EnemyView> enemies = view.enemies();
        List<Integer> aliveIndices = new java.util.ArrayList<>();
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                aliveIndices.add(i);
            }
        }
        int[] xs = new int[enemies.size()];
        int count = aliveIndices.size();
        if (count <= 0) {
            java.util.Arrays.fill(xs, -ENEMY_W * 2);
            return xs;
        }
        int totalWidth = count * ENEMY_W;
        int startX = (width - totalWidth) / 2;
        for (int slot = 0; slot < count; slot++) {
            xs[aliveIndices.get(slot)] = startX + slot * ENEMY_W;
        }
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                continue;
            }
            // 阵亡的挪到屏幕外：不参与排布，也不会被点选
            xs[i] = -ENEMY_W * 2;
        }
        return xs;
    }

    /** v0.4.4：把每个信息栏的归一化中心 + FOV + 宽高比发给服务端（怪物按它对齐）。 */
    private void sendAlignment() {
        BattleView view = view();
        List<BattleView.EnemyView> enemies = view.enemies();
        // v0.5.0.7：只对**存活**的敌人发对齐（阵亡的不该再占位置，见 enemyPanelXs 的说明）
        List<Integer> aliveIndices = new java.util.ArrayList<>();
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                aliveIndices.add(i);
            }
        }
        int count = aliveIndices.size();
        if (minecraft == null || count <= 0 || width <= 0) {
            return;
        }
        int totalWidth = count * ENEMY_W;
        int startX = (width - totalWidth) / 2;
        java.util.List<Float> centers = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double center = startX + i * ENEMY_W + (ENEMY_W - 4) / 2.0D;
            centers.add((float) (center / width));
        }
        int fov = minecraft.options.fov().get();
        float aspect = (float) minecraft.getWindow().getWidth()
                / Math.max(1, minecraft.getWindow().getHeight());
        PacketDistributor.sendToServer(new com.slaythecraft.network.BattleAlignPayload(centers, fov, aspect));
    }

    /** 每场战斗只对齐一次；窗口尺寸变了（栏位位置变了）就再对齐一次。 */
    private long alignedBattleId = -1L;
    private int alignedWidth = -1;
    private int alignedCount = -1;

    private void ensureAligned() {
        BattleView view = view();
        int count = view.enemies().size();
        long battleId = ClientBattleState.battleId();
        if (count <= 0) {
            return;
        }
        if (alignedBattleId == battleId && alignedWidth == width && alignedCount == count) {
            return;
        }
        alignedBattleId = battleId;
        alignedWidth = width;
        alignedCount = count;
        sendAlignment();
    }

    private void drawEnemies(GuiGraphics graphics, BattleView view, int mouseX, int mouseY) {
        List<BattleView.EnemyView> enemies = view.enemies();
        int[] panelXs = enemyPanelXs(view);
        for (int i = 0; i < enemies.size(); i++) {
            BattleView.EnemyView enemy = enemies.get(i);
            int x = panelXs[i];
            int y = ENEMY_ROW_Y;
            int panelH = enemyHeight(enemy);
            boolean hovered = isInside(mouseX, mouseY, x, y, ENEMY_W - 4, panelH);
            int accent = enemy.alive() ? (enemy.targetingYou() ? DANGER : PANEL_EDGE) : 0xFF404048;

            graphics.fill(x, y, x + ENEMY_W - 4, y + panelH, PANEL_BG);
            border(graphics, x, y, x + ENEMY_W - 4, y + panelH, targetingCard >= 0 && hovered ? GOLD : accent);

            TextStyle.draw(graphics, font, trim(enemy.name(), ENEMY_W - 12), x + 4, y + 4, enemy.alive() ? TEXT : TEXT_DIM);
            // v0.4.6.7 闪电苦力怕：锁血期间血量**显示为「晕眩」**，不再是数字
            Component hp = enemy.stunned()
                    ? Component.translatable("stc.battle.stunned")
                    : Component.translatable("stc.battle.enemy_hp", enemy.hp(), enemy.maxHp(), enemy.block());
            TextStyle.draw(graphics, font, trim(hp.getString(), ENEMY_W - 12), x + 4, y + 16,
                    enemy.alive() ? TEXT_DIM : 0xFF606068);
            if (enemy.alive()) {
                // v0.5.0.7 需求："有多个行动则换行，而不是显示『行动1+行动2』导致显示不全。"
                // 面板高度按行数自适应，逐行画（每行都带实际数值，见 Enemy.actualIntentLines）
                List<String> intents = enemy.intents();
                for (int line = 0; line < intents.size(); line++) {
                    String raw = intents.get(line);
                    String intentText = enemy.targetingYou()
                            ? Component.translatable("stc.battle.intent_you", raw).getString()
                            : raw;
                    TextStyle.draw(graphics, font, trim(intentText, ENEMY_W - 12), x + 4,
                            y + intentLineY(line), enemy.targetingYou() ? DANGER : GOLD);
                }
            } else {
                TextStyle.draw(graphics, font, Component.translatable("stc.battle.enemy_down").getString(),
                        x + 4, y + intentLineY(0), TEXT_DIM);
            }
            // v0.0.3 W1：敌人当前效果显示在意图框下面（例如"虚弱 5 · 易伤 2"）
            // v0.4.6：每条效果左侧画原版状态图标，并且悬停 0.6 秒有说明
            // v0.5.5.1：战斗内常态只画图标 + 数字（悬停仍是完整说明），新出现的状态渐显
            if (enemy.alive() && !enemy.statuses().isEmpty()) {
                List<String> entries = StatusIcons.split(enemy.statuses());
                long statusNow = System.currentTimeMillis();
                float[] alphas = new float[entries.size()];
                for (int e = 0; e < entries.size(); e++) {
                    alphas[e] = statusAlpha("enemy:" + enemy.name() + ":" + entries.get(e), statusNow);
                }
                List<int[]> rects = StatusIcons.drawCompactLine(graphics, entries, x + ENEMY_W - 8,
                        y + panelH - 12, GOLD, alphas);
                for (int e = 0; e < rects.size(); e++) {
                    int[] rect = rects.get(e);
                    String key = "enemy:" + enemy.name() + ":" + entries.get(e);
                    if (isInside(mouseX, mouseY, rect[0], rect[1], rect[2], rect[3])) {
                        if (HoverTracker.update(key)) {
                            showTooltip(graphics, StatusIcons.describe(entries.get(e)), mouseX, mouseY);
                        }
                    }
                }
            }
        }
    }

    private void drawHand(GuiGraphics graphics, BattleView view, int mouseX, int mouseY) {
        List<BattleView.HandCard> hand = view.hand();
        if (hand.isEmpty()) {
            Component empty = Component.translatable("stc.battle.no_cards");
            TextStyle.draw(graphics, font, empty, (width - font.width(empty)) / 2, height - 30, TEXT_DIM);
            lastHandKeys = List.of();
            // v0.5.0.5：手牌空了也要把"离手检测"的基准清掉，否则下一手会误判整手都在被打出
            previousHandKeys.clear();
            lastHandCards.clear();
            lastHandSlotX.clear();
            return;
        }
        int y = height - CARD_H - HAND_BOTTOM_MARGIN + hudShift();

        // v0.3.0：需求"让每张牌以淡入的方式进入手牌" —— 只有新出现的那几张才播淡入
        // v0.4.6.7：加上"从左到右依次渐显"与"从屏幕中心滑到槽位"
        List<String> keys = handKeys(hand);
        List<String> stable = stableKeys(keys);
        updateAppearTimes(view, keys, stable);
        List<Float> slotX = handSlotX(hand.size());
        // v0.5.0.5：记下这一帧的手牌，下一帧用来发现"哪张牌离手了"（没在 play() 里走到的出牌路径也能补上动画）
        detectPlayedCards(view, stable);

        // 从左到右绘制，被鼠标悬停的最后画（盖在最上面）
        // v0.5.1.9：轮不到我出牌（敌怪回合 / 已确认在等队友）时手牌整体变暗、也不响应悬停 ——
        // 让"现在不能出牌"一眼看得出来，同时把屏幕下半部让给敌怪的攻击演出。
        boolean interactive = view.yourTurn();
        float handAlpha = interactive ? 1.0F : INACTIVE_HAND_ALPHA;
        int hovered = interactive
                ? handIndexAt(mouseX, mouseY, hand.size(), handSlotX(width, hand.size(), 0), y)
                : -1;
        for (int i = 0; i < hand.size(); i++) {
            // v0.5.5.1（用户）："战斗中，手牌拖动时，原位置应该取消显示" ——
            // 正被拖在鼠标上的那张牌不再画在原槽位（松手没打出去就回到原位，因为拖动状态被清掉了）
            if (i == hovered || isDraggingIndex(i)) {
                continue;
            }
            drawHandCard(graphics, view, hand.get(i), i,
                    Math.round(slideX(stable.get(i), slotX.get(i))), y, false,
                    alphaOf(stable.get(i)) * handAlpha, stable.get(i));
        }
        if (hovered >= 0 && !isDraggingIndex(hovered)) {
            drawHandCard(graphics, view, hand.get(hovered), hovered,
                    Math.round(slideX(stable.get(hovered), slotX.get(hovered))), y, true,
                    alphaOf(stable.get(hovered)) * handAlpha, stable.get(hovered));
        }
        // 不再在手牌里的牌丢掉滑入/上浮状态，避免键越堆越多
        java.util.Set<String> present = new java.util.HashSet<>(stable);
        handSlideX.keySet().retainAll(present);
        handHoverLift.keySet().retainAll(present);
    }

    /**
     * 画一张手牌（v0.5.0.5：带**上浮动画**与"未悬停只露上半段"的裁切）。
     *
     * <p>需求原文："未打出的手牌下半部分会常态隐藏，只显示上半部分的卡图和卡名。在鼠标悬停在相当位置
     * 的手牌上时，手牌会被向上拉（需要一个向上浮动的动画）以显示下半段的文字描述。未打出则回到原位。"</p>
     *
     * <p>做法：悬停时把牌上移 {@link #CARD_HOVER_LIFT}，并用每帧插值做成"浮起"动画；
     * 未悬停时用 {@code enableScissor} 把绘制裁到 {@link #CARD_PEEK_H} 高度，
     * 于是卡图/卡名/费用露着、下半段的描述文字被裁掉。</p>
     */
    private void drawHandCard(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index,
                              int x, int baseY, boolean hovered, float alpha, String key) {
        // 上浮动画：每帧朝目标靠近，形成"被拉上去"的浮动感
        float target = hovered ? CARD_HOVER_LIFT : 0.0F;
        float lift = handHoverLift.getOrDefault(key, 0.0F);
        lift += (target - lift) * HOVER_LIFT_LERP;
        if (Math.abs(lift - target) < 0.4F) {
            lift = target;
        }
        handHoverLift.put(key, lift);
        int y = baseY - Math.round(lift);

        // 未悬停且动画已经落回原位：只露出上半段
        boolean peek = !hovered && lift <= 0.5F;
        // 未悬停的牌略微缩小，形成 3D 层叠（悬停的那张恢复原大小、压在最上面）
        float scale = hovered ? 1.0F : CARD_IDLE_SCALE;

        if (peek) {
            // 裁切要跟着缩放走：以牌中心为基准的缩放会把可见高度也缩放同样的比例
            float scaledTop = y + CARD_H / 2.0F - (CARD_H / 2.0F) * scale;
            int clipTop = (int) Math.floor(scaledTop);
            int clipBottom = (int) Math.ceil(scaledTop + CARD_PEEK_H * scale);
            graphics.enableScissor(x - 2, clipTop, x + CARD_W + 2, clipBottom);
            drawCardFaded(graphics, view, card, index, x, y, false, alpha, scale);
            graphics.disableScissor();
        } else {
            drawCardFaded(graphics, view, card, index, x, y, hovered, alpha, scale);
        }
    }

    /**
     * 手牌的第 {@code index} 个槽位 x（**居中对称**）。
     *
     * <p>4 张牌 = 左 2 右 2、5 张牌 = 左 2 中 1 右 2：整套牌以屏幕中心为轴左右对称，
     * 所以牌变多时是"向两侧展开"，不会一直朝一个方向推出去。</p>
     */
    public static int handSlotX(int screenWidth, int handSize, int index) {
        if (handSize <= 0) {
            return screenWidth / 2;
        }
        int totalWidth = handSize * CARD_W + (handSize - 1) * CARD_GAP;
        int startX = (screenWidth - totalWidth) / 2;
        return startX + index * (CARD_W + CARD_GAP);
    }

    /** 当前界面里每个槽位的 x。 */
    private List<Float> handSlotX(int handSize) {
        List<Float> list = new java.util.ArrayList<>(handSize);
        for (int i = 0; i < handSize; i++) {
            list.add((float) handSlotX(width, handSize, i));
        }
        return list;
    }

    /**
     * 这张牌现在应该画在哪个 x：从"屏幕中心"出发，逐帧平滑滑向自己的槽位。
     *
     * <p>第一次见到这张牌时，如果它是刚抽到的（{@link #cardAppearAt} 里有它），就从屏幕中心开始 ——
     * 这正是需求里"第一张抽到的手牌放置到屏幕中心"。老牌（例如重开界面）直接落在槽位上。</p>
     */
    private float slideX(String key, float target) {
        Float current = handSlideX.get(key);
        if (current == null) {
            current = cardAppearAt.containsKey(key) ? (width - CARD_W) / 2.0F : target;
        }
        float next = current + (target - current) * HAND_SLIDE_LERP;
        if (Math.abs(target - next) < 0.5F) {
            next = target;
        }
        handSlideX.put(key, next);
        return next;
    }

    /** 手牌的**稳定键**：同一种牌的第 n 张（滑入与渐显都按它记录，出牌后下标变化也不会错位）。 */
    private List<String> stableKeys(List<String> keys) {
        Map<String, Integer> seen = new HashMap<>();
        List<String> stable = new java.util.ArrayList<>(keys.size());
        for (String key : keys) {
            int occurrence = seen.merge(key, 1, Integer::sum) - 1;
            stable.add(key + "#" + occurrence);
        }
        return stable;
    }

    /** 手牌的身份键（id + 是否强化）：同种牌有多张时按多重集匹配。 */
    private List<String> handKeys(List<BattleView.HandCard> hand) {
        List<String> keys = new java.util.ArrayList<>(hand.size());
        for (BattleView.HandCard card : hand) {
            keys.add(HandFadeTracker.key(card.id().getPath(), card.upgrades() > 0));
        }
        return keys;
    }

    /**
     * 记录"哪几张牌是新抽到的"。
     *
     * <p>换回合（回合数变了）时整手牌都是新抽到的 —— 上一手在回合结束时已经被弃掉了。</p>
     */
    private void updateAppearTimes(BattleView view, List<String> keys, List<String> stable) {
        long now = System.currentTimeMillis();
        boolean newTurn = view.turn() != lastTurn;
        List<Integer> fresh = newTurn ? HandFadeTracker.allIndices(keys.size())
                : HandFadeTracker.newIndices(lastHandKeys, keys);
        // v0.4.6.7：从左到右依次渐显 —— 第 k 张新牌比第一张晚 k×70ms 开始淡入
        int order = 0;
        for (int index : fresh) {
            cardAppearAt.put(stable.get(index), now + order * HAND_STAGGER_MS);
            order++;
        }
        // 清掉已经不在手牌里的键，避免键越堆越多（也避免同键复用时误判"不是新牌"）
        cardAppearAt.keySet().retainAll(stable);
        lastHandKeys = keys;
        lastTurn = view.turn();
    }

    /** 某张手牌当前的不透明度（1 = 早就抽到了；排在后面的新牌会等到自己的时刻才开始淡入）。 */
    private float alphaOf(String key) {
        Long appearedAt = cardAppearAt.get(key);
        if (appearedAt == null) {
            return 1.0F;
        }
        return FadeTimeline.fadeIn(System.currentTimeMillis(), appearedAt, FadeTimeline.CARD_FADE_MS);
    }

    /** 带淡入的卡牌绘制：新抽到的牌由下往上滑入并渐显。 */
    private void drawCardFaded(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index,
                               int x, int y, boolean hovered, float alpha) {
        drawCardFaded(graphics, view, card, index, x, y, hovered, alpha, 1.0F);
    }

    /** 带淡入 + 缩放的卡牌绘制（v0.5.0.5：多出手牌用 {@code scale} 做 3D 层叠）。 */
    private void drawCardFaded(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index,
                               int x, int y, boolean hovered, float alpha, float scale) {
        if (alpha <= 0.01F) {
            return;
        }
        int slide = alpha >= 0.999F ? 0 : Math.round((1.0F - alpha) * 10.0F);
        int cardY = y + slide;
        if (alpha >= 0.999F) {
            drawCard(graphics, view, card, index, x, cardY, hovered, scale);
        } else {
            FadeStyle.withAlpha(alpha, () -> drawCard(graphics, view, card, index, x, cardY, hovered, scale));
        }
    }

    private void drawCard(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index, int x, int y,
                          boolean hovered) {
        drawCard(graphics, view, card, index, x, y, hovered, 1.0F);
    }

    /**
     * 画一张手牌。
     *
     * @param scale 整体缩放（以牌的中心为基准；1.0 = 原大小）
     */
    private void drawCard(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index, int x, int y,
                          boolean hovered, float scale) {
        // ---- v0.5.0.19：整张牌的所有绘制都在这一个姿态作用域里 ----
        //
        // 反馈（配图）：某张牌的**卡体与卡面内容错开约一张牌的宽度** —— 看起来像两张牌叠着。
        // 我逐项核对过坐标（`drawCardBody` 内部所有元素都用同一个 `x,y` 基准）与姿态栈
        // （全文件只有一对 push/pop，平衡），静态推演不出这个偏移。
        // 所以这里做**结构性收口**：不管外部留下了什么变换，牌体与内容都在同一个作用域里、
        // 用同一个 `(x, y)` 基准绘制 —— "内容与卡体走不同的坐标"在结构上不可能发生。
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        if (Math.abs(scale - 1.0F) > 1.0E-4F) {
            // 以牌的中心为基准缩放（此时原点已在牌的左上角）
            graphics.pose().translate(CARD_W / 2.0F, CARD_H / 2.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.pose().translate(-CARD_W / 2.0F, -CARD_H / 2.0F, 0.0F);
        }
        try {
            // 作用域内一律用"相对牌的左上角"的局部坐标：0, 0
            drawCardBody(graphics, view, card, index, 0, 0, hovered);
            // v0.5.0.19：把这张牌**实际绘制时的矩形**记下来（诊断用）
            CardDrawLog.record(card.id().getPath(), x, y, CARD_W, CARD_H, scale,
                    ClientBattleState.view().hand().size());
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * 画一张手牌的**卡面内容**（v0.5.5.1 起直接委托给 {@link CardFace}）。
     *
     * <p>需求（v0.5.5.1）："拥有卡牌和卡牌图鉴内的卡牌显示方式将和手牌显示的方式一致（包括美工等）。"
     * —— 所以整张卡面的画法**只留一份**（{@link CardFace}），这里只负责把"这一帧手牌的状态"
     * 翻成 {@link CardFace.View}：服务端算好的实时描述、能不能打出、费用够不够、有没有被选中。</p>
     */
    private void drawCardBody(GuiGraphics graphics, BattleView view, BattleView.HandCard card, int index, int x, int y,
                              boolean hovered) {
        // V11：强化过的牌要用强化版定义（数值与描述都不一样）
        // v0.5.4.1（A4-20）：按**档数**取定义 —— 手牌里的【灼热打击】+3 就是 +3 那一档的名字
        Card definition = CardLibrary.resolve(card.id().getPath(), card.upgrades());
        if (definition == null) {
            return;
        }
        // v0.0.3 W2：不能打出的状态牌（虚空 / 灼烧 / 晕眩）画成灰的，也点不动
        // v0.5.3.9：除了"状态 / 诅咒不可打出"，还要看服务端下发的"这一刻能不能打出"（【交锋】）
        boolean unplayable = StatusCards.unplayable(definition) || !card.playable();
        boolean affordable = !unplayable && card.cost() <= view.energy();
        CardFace.draw(graphics, font, definition, card.id(),
                new CardFace.View(card.cost(), card.keywords(), card.enchantments(), card.description(),
                        card.upgrades(), !unplayable, affordable, index == targetingCard, hovered),
                x, y);
    }

    /**
     * 画一条**指定粗细**的边框（v0.5.0.15：稀有度边框加粗）。
     *
     * @param thickness 边框向**内侧**生长的像素数
     */
    private void thickBorder(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int thickness) {
        int t = Math.max(1, thickness);
        for (int i = 0; i < t; i++) {
            // 每一圈画一个 1 像素的矩形框，逐圈内缩 —— 得到实心的 N 像素边框
            graphics.fill(x1 + i, y1 + i, x2 - i, y1 + i + 1, color);
            graphics.fill(x1 + i, y2 - i - 1, x2 - i, y2 - i, color);
            graphics.fill(x1 + i, y1 + i, x1 + i + 1, y2 - i, color);
            graphics.fill(x2 - i - 1, y1 + i, x2 - i, y2 - i, color);
        }
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        BattleView view = view();
        // v0.5.1.6：界面在"玩家回合里等我确认"和"我已确认、等队友"两种情况下都画；
        // 但只有**还没确认**时才能出牌 / 用负面药水 / 结束回合。
        // v0.5.1.9：敌怪回合界面也画着，所以这里不能整块吞掉点击 —— 「放弃战斗」与
        // 「查看牌堆」这些只读/逃生入口要留着（下面 `if (!canAct) return true;` 会把
        // 出牌相关的点击吃掉）。
        boolean canAct = view.active() && view.yourTurn();
        if (!view.active()) {
            return false;
        }
        // v0.4.3：结束回合（药水栏右边）/ 放弃战斗（右上角，二次确认）
        if (canAct && isInside(mouseX, mouseY, quitButtonX(), quitButtonY(), 104, POTION_SIZE + 2)) {
            PacketDistributor.sendToServer(EndTurnPayload.INSTANCE);
            targetingCard = -1;
            potionTargeting = -1;
            confirmQuit = false;
            return true;
        }
        int quitX = width - 78 - 12;
        if (isInside(mouseX, mouseY, quitX, 10, 78, 18)) {
            if (confirmQuit) {
                confirmQuit = false;
                PacketDistributor.sendToServer(QuitBattlePayload.INSTANCE);
            } else {
                confirmQuit = true;
            }
            return true;
        }
        // v0.4.2：查看我的牌组 / 三个牌堆
        int orbY = height - CARD_H - 10 + hudShift();
        if (isInside(mouseX, mouseY, 16 - 2, orbY - 22, 76, 18)) {
            minecraft.setScreen(new BattleCardListScreen(BattleCardListScreen.Kind.DECK));
            return true;
        }
        if (isInside(mouseX, mouseY, drawPileX(), drawPileY(), PILE_W, PILE_H)) {
            minecraft.setScreen(new BattleCardListScreen(BattleCardListScreen.Kind.DRAW));
            return true;
        }
        if (isInside(mouseX, mouseY, discardPileX(), discardPileY(), PILE_W, PILE_H)) {
            minecraft.setScreen(new BattleCardListScreen(BattleCardListScreen.Kind.DISCARD));
            return true;
        }
        if (isInside(mouseX, mouseY, exhaustPileX(), exhaustPileY(), PILE_W, PILE_H)) {
            minecraft.setScreen(new BattleCardListScreen(BattleCardListScreen.Kind.EXHAUST));
            return true;
        }
        if (confirmQuit) {
            // 点了别处 = 取消放弃
            confirmQuit = false;
        }
        // 以下动作都需要"我这一轮还没确认结束回合"（v0.5.1.6）
        if (!canAct) {
            return true;
        }
        // V8：负面药水选目标
        if (potionTargeting >= 0) {
            int enemyIndex = enemyIndexAt(mouseX, mouseY, view);
            if (enemyIndex >= 0 && view.enemies().get(enemyIndex).alive()) {
                PacketDistributor.sendToServer(new UsePotionPayload(potionTargeting, enemyIndex));
                potionTargeting = -1;
                return true;
            }
            if (button == 1) {
                potionTargeting = -1;
            }
            return true;
        }
        // V8：点药水槽位
        int potionSlot = potionSlotAt(mouseX, mouseY, view);
        if (potionSlot >= 0) {
            Potion potion = PotionLibrary.byId(view.potions().get(potionSlot).getPath());
            if (potion != null) {
                if (potion.kind().canTargetEnemy()) {
                    potionTargeting = potionSlot;
                    targetingCard = -1;
                } else if (potion.canTargetAlly() && view.party().size() > 1) {
                    // v0.4.6：正面药水可以给自己/队友 —— 多人时先开"选择目标"页面
                    minecraft.setScreen(new PotionTargetScreen(this, potionSlot, potion, view.party()));
                } else {
                    // 单人 / 只能对自己的药水：直接对自己使用
                    PacketDistributor.sendToServer(new UsePotionPayload(potionSlot, -1));
                }
            }
            return true;
        }
        // 选目标模式：点敌人
        if (targetingCard >= 0) {
            int enemyIndex = enemyIndexAt(mouseX, mouseY, view);
            if (enemyIndex >= 0 && view.enemies().get(enemyIndex).alive()) {
                play(targetingCard, enemyIndex);
                targetingCard = -1;
                return true;
            }
            if (button == 1) {
                targetingCard = -1;
            }
            return true;
        }
        // 点手牌
        List<BattleView.HandCard> hand = view.hand();
        int startX = handStartX(hand.size());
        int y = height - CARD_H - HAND_BOTTOM_MARGIN + hudShift();
        int index = handIndexAt(mouseX, mouseY, hand.size(), startX, y);
        if (index >= 0) {
            BattleView.HandCard card = hand.get(index);
            Card definition = CardLibrary.resolve(card.id().getPath(), card.upgrades());
            // v0.0.3 W2：不能打出的状态牌点了也不发请求（服务端本来也会拒绝）
            if (definition != null && StatusCards.unplayable(definition)) {
                return true;
            }
            if (card.cost() > view.energy()) {
                return true;
            }
            if (card.needsTarget()) {
                // 指向性牌（选敌人的攻击牌等）：保持原来的"点牌 → 点敌人"
                targetingCard = index;
            } else {
                // v0.5.1.8 需求："非指向性（获得格挡 / 对所有敌人造成伤害 / 对随机敌人造成伤害…）
                // 使用拖动操作，而不是单点操作。长按卡面可以执行拖动操作，拖动到屏幕中心后松开可以
                // 执行卡牌效果。" —— 所以这里**只开始按住**，真正的出牌在 mouseReleased 里做。
                pressedCard = index;
                pressedAt = System.currentTimeMillis();
                dragCard = -1;
                // 开始拖一张非指向性牌时，取消"等着选敌人"的状态
                targetingCard = -1;
                potionTargeting = -1;
                confirmQuit = false;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 松开鼠标：非指向性牌拖到屏幕中心就打出（v0.5.1.8）。
     *
     * <p>短按（还没到长按时间就松开）**不会**出牌 —— 需求明确是"而不是单点操作"；
     * 这时打出区的提示已经把用法写清楚了。</p>
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressedCard >= 0) {
            boolean dragged = dragCard >= 0;
            int index = dragged ? dragCard : pressedCard;
            boolean inZone = isInPlayZone(mouseX, mouseY, width, height);
            BattleView view = view();
            clearDrag();
            if (dragged && inZone && index >= 0 && index < view.hand().size() && view.yourTurn()) {
                BattleView.HandCard card = view.hand().get(index);
                Card definition = CardLibrary.resolve(card.id().getPath(), card.upgrades());
                boolean playable = definition == null
                        || (!StatusCards.unplayable(definition) && card.playable());
                if (playable && card.cost() <= view.energy()) {
                    play(index, -1);
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC：暂时收起界面去看世界（几秒后自动弹回；想离开战斗请点「放弃战斗」）
        if (keyCode == 256) {
            if (ClientBattleState.isEnemyPhase()) {
                // v0.5.1.9：敌怪回合**不给**收起 —— 收起就等于把玩家放回世界里走动、转视角，
                // 敌怪的攻击演出也就白做了。这里只是吃掉按键，不关界面。
                return true;
            }
            ClientBattleState.hideForThisTurn();
            onClose();
            return true;
        }
        // v0.4.2：R = 结束回合（军旗右键不再结束回合，这里补一个顺手的快捷键）
        if (keyCode == 82) {
            BattleView endView = view();
            if (endView.active() && endView.yourTurn()) {
                PacketDistributor.sendToServer(EndTurnPayload.INSTANCE);
                targetingCard = -1;
                potionTargeting = -1;
                confirmQuit = false;
            }
            return true;
        }
        // 数字键 1-9 直接出牌
        int index = keyCode - 49;
        BattleView view = view();
        if (index >= 0 && index < view.hand().size() && view.yourTurn()) {
            BattleView.HandCard card = view.hand().get(index);
            Card definition = CardLibrary.resolve(card.id().getPath(), card.upgrades());
            if (definition != null && StatusCards.unplayable(definition)) {
                return true;
            }
            if (card.cost() <= view.energy()) {
                if (card.needsTarget()) {
                    targetingCard = index;
                } else {
                    play(index, -1);
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void play(int handIndex, int enemyIndex) {
        // v0.5.0.5：在发请求之前先把这张牌登记成"正在被打出"，动画不依赖回包（更跟手）
        BattleView view = view();
        if (handIndex >= 0 && handIndex < view.hand().size()) {
            List<String> keys = handKeys(view.hand());
            List<String> stable = stableKeys(keys);
            startPlayAnim(stable.get(handIndex), view.hand().get(handIndex),
                    Math.round(slideX(stable.get(handIndex),
                            handSlotX(width, view.hand().size(), handIndex))));
        }
        PacketDistributor.sendToServer(new PlayCardPayload(handIndex, enemyIndex));
    }

    // ------------------------------------------------------------------ v0.5.0.5：打牌动画

    /**
     * 一张**正在被打出**的牌的动画阶段（v0.5.0.5）。
     *
     * <p>需求原文："卡牌被打出时，会先出现从手牌中被放置到屏幕中间的动画，等待卡牌触发的效果结束后，
     * 然后再从屏幕中间向弃牌堆边缩小边移动（以显示牌被丢入弃牌堆）。为了防止卡牌堆在一起，
     * 只有当卡牌效果的动画结束时，卡牌才会结束等待并被丢弃。其他跟随打出的卡牌则会等比例缩小
     * 并被放置在当前卡牌后方（3D效果）。"</p>
     */
    private enum PlayPhase {        /** 手牌 → 屏幕中间。 */
        TO_CENTER,
        /** 停在屏幕中间，等效果动画结束。 */
        HOLD,
        /** 屏幕中间 → 弃牌堆，边缩边移。 */
        TO_DISCARD
    }

    /** 一段打牌动画。 */
    private static final class PlayedCard {
        private final BattleView.HandCard card;
        private final boolean upgraded;
        private final long startedAt;
        private final int fromY;
        private PlayPhase phase = PlayPhase.TO_CENTER;
        private float scale = 1.0F;

        private PlayedCard(BattleView.HandCard card, long startedAt, int fromY) {
            this.card = card;
            this.upgraded = card.upgrades() > 0;
            this.startedAt = startedAt;
            this.fromY = fromY;
        }
    }

    /** 手牌 → 屏幕中间的时长（毫秒）。 */
    private static final long PLAY_TO_CENTER_MS = 220L;
    /** 在屏幕中间至少停留这么久（等效果动画；效果还在放就继续等）。 */
    private static final long PLAY_HOLD_MIN_MS = 260L;
    /** 屏幕中间 → 弃牌堆的时长。 */
    private static final long PLAY_TO_DISCARD_MS = 280L;
    /** 同时在播的打牌动画上限（超出就丢掉最老的，避免堆一大堆）。 */
    private static final int PLAY_ANIM_LIMIT = 6;

    /** 正在播出的打牌动画（按开始时刻）。 */
    private final Map<Long, PlayedCard> playedCards = new java.util.LinkedHashMap<>();
    /** 已经在播/刚播过的**手牌稳定键**（v0.5.0.7：同一次出牌不会重复起动画）。 */
    private final Set<String> playingKeys = new java.util.HashSet<>();
    /** 上一帧手牌的**稳定键（有序）**，用来按定义计数判断"哪张牌离手了"（v0.5.1.8 从 Set 改成 List）。 */
    private final List<String> previousHandKeys = new java.util.ArrayList<>();

    /**
     * 起一段打牌动画（v0.5.0.7 修正"同名卡被连带播动画"）。
     *
     * <p>需求原文："打出动画存在会将打出的手牌中同名卡牌也执行动画的情况。"</p>
     *
     * <p>根因：这张牌会被**两条路径**各登记一次 —— 一条是点牌时 {@link #play(int, int)} 立刻登记
     * （让动画跟手），另一条是下一帧 {@link #detectPlayedCards} 发现"它离手了"再补一次。
     * 两条都指向同一张牌，于是同一张牌被起了两段动画，看起来就像"同名卡也跟着动"。
     * 这里用 {@code key} 做幂等：同一张牌（同一个稳定键）只起一段动画，重复调用直接忽略。</p>
     */
    private void startPlayAnim(String key, BattleView.HandCard card, int fromX) {
        if (key == null || !playingKeys.add(key)) {
            // 这张牌已经在播了，不要再起第二段（也不会波及同名的另一张：它们的稳定键不同）
            return;
        }
        long now = System.currentTimeMillis();
        PlayedCard played = new PlayedCard(card, now, height - CARD_H - HAND_BOTTOM_MARGIN + hudShift());
        playedFromX.put(now, fromX);
        playedKeys.put(now, key);
        playedCards.put(now, played);
        if (playedCards.size() > PLAY_ANIM_LIMIT) {
            java.util.Iterator<Long> it = playedCards.keySet().iterator();
            if (it.hasNext()) {
                Long oldest = it.next();
                it.remove();
                playedFromX.remove(oldest);
                playingKeys.remove(playedKeys.remove(oldest));
            }
        }
    }

    /** 每段动画对应的手牌稳定键（播完要把它从 {@link #playingKeys} 里放掉）。 */
    private final Map<Long, String> playedKeys = new HashMap<>();

    /** 每段动画的起点 x。 */
    private final Map<Long, Integer> playedFromX = new HashMap<>();

    /**
     * 发现"这一帧有哪些牌离手了"，给它们各起一段打牌动画（v0.5.0.5）。
     *
     * <p>这是 {@link #play(int, int)} 之外的第二条补漏路径：换回合弃牌、键盘/其它入口出牌、
     * 服务端把牌拿走等情况都会在这里被捕捉到，动画不会漏。</p>
     *
     * <p><b>v0.5.1.8 修的 bug</b>：用户反馈"打出的卡牌和手牌中的最后一张牌同名时，最后一张牌也会播放
     * 一遍打出动画，虽然不会被真的打出"。根因是**稳定键按位置编号**（{@code 打击#0 / 打击#1}）：
     * 打出第一张同名牌后，剩下的那张会被重新编号成 {@code #0}，于是"上一帧有 #1、这一帧没有" →
     * 差值把**留在手里的那张**误判成"离手了"，替它播了一段动画。</p>
     *
     * <p>现在改成**按定义（同名同强化）计数**：某个定义少了几张，就替它补几段动画，
     * 但要先**扣掉已经为这个定义起过的动画**（{@code play()} 那条"跟手"的路径已经登记过一张），
     * 所以不会重复播、也不会播到留在手里的那张。</p>
     */
    private void detectPlayedCards(BattleView view, List<String> stable) {
        List<BattleView.HandCard> hand = view.hand();
        if (!previousHandKeys.isEmpty()) {
            Map<String, Integer> before = definitionCounts(previousHandKeys);
            Map<String, Integer> after = definitionCounts(stable);
            for (Map.Entry<String, Integer> entry : before.entrySet()) {
                String definition = entry.getKey();
                int missing = entry.getValue() - after.getOrDefault(definition, 0);
                int toStart = missing - inFlightFor(definition);
                if (toStart <= 0) {
                    continue;
                }
                // 离手的那张已经不在当前手牌里了，所以牌面与起点都从"上一帧的记录"里取；
                // 同名有多张时按上一帧的先后取，并跳过已经起过动画的那些
                for (String key : previousHandKeys) {
                    if (toStart <= 0) {
                        break;
                    }
                    if (!definition.equals(definitionOf(key)) || playingKeys.contains(key)) {
                        continue;
                    }
                    BattleView.HandCard cached = lastHandCards.get(key);
                    if (cached == null) {
                        continue;
                    }
                    int fromX = lastHandSlotX.getOrDefault(key, (width - CARD_W) / 2);
                    startPlayAnim(key, cached, fromX);
                    toStart--;
                }
            }
        }
        previousHandKeys.clear();
        previousHandKeys.addAll(stable);
        // 顺手记下这一帧每张牌的位置与牌面，供下一帧"离手"时使用
        lastHandCards.clear();
        lastHandSlotX.clear();
        for (int i = 0; i < stable.size(); i++) {
            lastHandCards.put(stable.get(i), hand.get(i));
            lastHandSlotX.put(stable.get(i), Math.round(slideX(stable.get(i), handSlotX(width, hand.size(), i))));
        }
    }

    /** 稳定键 {@code 打击#1} → 定义键 {@code 打击}（v0.5.1.8）。 */
    private static String definitionOf(String stableKey) {
        int hash = stableKey == null ? -1 : stableKey.lastIndexOf('#');
        return hash < 0 ? stableKey : stableKey.substring(0, hash);
    }

    /** 一组稳定键里每个**定义**各出现几次。 */
    private static Map<String, Integer> definitionCounts(List<String> stableKeys) {
        Map<String, Integer> counts = new HashMap<>();
        for (String key : stableKeys) {
            counts.merge(definitionOf(key), 1, Integer::sum);
        }
        return counts;
    }

    /** 正在播的动画里，某个定义有几段（v0.5.1.8：用它避免同名卡被重复播动画）。 */
    private int inFlightFor(String definition) {
        int count = 0;
        for (String key : playedKeys.values()) {
            if (definition.equals(definitionOf(key))) {
                count++;
            }
        }
        return count;
    }

    /** 上一帧每张手牌的牌面（稳定键 → 牌）。 */
    private final Map<String, BattleView.HandCard> lastHandCards = new HashMap<>();
    /** 上一帧每张手牌的槽位 x（稳定键 → x），用作打牌动画的起点。 */
    private final Map<String, Integer> lastHandSlotX = new HashMap<>();

    /**
     * 推进并绘制所有打牌动画。
     *
     * <p>在 {@code drawHand} 之后调用，所以它会画在普通手牌**之上**。</p>
     */
    private void drawPlayedCards(GuiGraphics graphics, BattleView view) {
        if (playedCards.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int centerX = (width - CARD_W) / 2;
        int centerY = (height - CARD_H) / 2;
        boolean effectsBusy = ClientBattleFx.activeCount() > 0;

        java.util.Iterator<Map.Entry<Long, PlayedCard>> it = playedCards.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, PlayedCard> entry = it.next();
            long started = entry.getKey();
            PlayedCard played = entry.getValue();
            long elapsed = now - started;
            int fromX = playedFromX.getOrDefault(started, centerX);

            switch (played.phase) {
                case TO_CENTER -> {
                    float t = Math.min(1.0F, (float) elapsed / PLAY_TO_CENTER_MS);
                    float ease = (float) BattleAnim.easeOut(t);
                    int x = Math.round(fromX + (centerX - fromX) * ease);
                    int y = Math.round(played.fromY + (centerY - played.fromY) * ease);
                    float scale = 0.85F + 0.15F * ease;
                    played.scale = scale;
                    drawCard(graphics, view, played.card, -1, x, y, true, scale);
                    if (t >= 1.0F) {
                        played.phase = PlayPhase.HOLD;
                    }
                }
                case HOLD -> {
                    // 需求："只有当卡牌效果的动画结束时，卡牌才会结束等待并被丢弃。"
                    // 效果还在放就继续等；放完了也要满足最短停留时间，不然一闪而过看不清
                    played.scale = 1.0F;
                    drawCard(graphics, view, played.card, -1, centerX, centerY, true, 1.0F);
                    if (elapsed >= PLAY_TO_CENTER_MS + PLAY_HOLD_MIN_MS && !effectsBusy) {
                        played.phase = PlayPhase.TO_DISCARD;
                        played.scale = 1.0F;
                    }
                }
                case TO_DISCARD -> {
                    float t = Math.min(1.0F,
                            (float) (elapsed - PLAY_TO_CENTER_MS - PLAY_HOLD_MIN_MS) / PLAY_TO_DISCARD_MS);
                    float ease = (float) BattleAnim.easeIn(t);
                    int tx = discardPileX() + PILE_W / 2 - CARD_W / 2;
                    int ty = discardPileY() - CARD_H / 2;
                    int x = Math.round(centerX + (tx - centerX) * ease);
                    int y = Math.round(centerY + (ty - centerY) * ease);
                    float scale = 1.0F - 0.75F * ease;
                    played.scale = scale;
                    drawCard(graphics, view, played.card, -1, x, y, false, scale);
                    if (t >= 1.0F) {
                        it.remove();
                        playedFromX.remove(started);
                        // 播完了才放掉这个键，允许同一张牌以后再被播出一次
                        String done = playedKeys.remove(started);
                        if (done != null) {
                            playingKeys.remove(done);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ 命中测试与工具

    private int handStartX(int handSize) {
        int totalWidth = handSize * CARD_W + (handSize - 1) * CARD_GAP;
        return (width - totalWidth) / 2;
    }

    private int potionBarStartX(int slots) {
        int totalWidth = slots * POTION_SIZE + (slots - 1) * POTION_GAP;
        return (width - totalWidth) / 2;
    }

    private int potionBarY() {
        return height - CARD_H - HAND_BOTTOM_MARGIN - POTION_SIZE - 12 + hudShift();
    }

    /**
     * 战斗内**提示行**的 y（"敌怪正在行动，请稍候…" / "请选择目标" / "正在等待队友"）。
     *
     * <p>v0.5.5.1（用户）："存在药水栏和敌人行动文字重叠问题" —— 这几个提示原先画在
     * {@code height - CARD_H - 40}，而药水栏在 {@code height - CARD_H - 64}（GUI 刻度 240 时两者
     * 只差 2 像素），于是红字正好压在药水槽上。现在统一放到**药水栏上方**一行。</p>
     */
    private int hintLineY() {
        return potionBarY() - 11;
    }

    /** 命中的药水槽位（只对已装药的槽位有效）。 */
    private int potionSlotAt(double mouseX, double mouseY, BattleView view) {
        int slots = Math.max(1, view.potionSlots());
        int startX = potionBarStartX(slots);
        int y = potionBarY();
        for (int i = 0; i < Math.min(slots, view.potions().size()); i++) {
            int x = startX + i * (POTION_SIZE + POTION_GAP);
            if (isInside(mouseX, mouseY, x, y, POTION_SIZE, POTION_SIZE)) {
                return i;
            }
        }
        return -1;
    }

    private int handIndexAt(double mouseX, double mouseY, int handSize, int startX, int y) {
        for (int i = handSize - 1; i >= 0; i--) {
            int x = startX + i * (CARD_W + CARD_GAP);
            int cardY = y;
            if (isInside(mouseX, mouseY, x, cardY, CARD_W, CARD_H)) {
                return i;
            }
        }
        // 悬停放大的那张会往上抬 10 像素，单独再判一次
        for (int i = handSize - 1; i >= 0; i--) {
            int x = startX + i * (CARD_W + CARD_GAP);
            if (isInside(mouseX, mouseY, x, y - 10, CARD_W, 10)) {
                return i;
            }
        }
        return -1;
    }

    /** v0.4.2：命中判定用的是**和绘制同一份**面板坐标（对齐实体后仍然点得准）。 */
    private int enemyIndexAt(double mouseX, double mouseY, BattleView view) {
        int[] panelXs = enemyPanelXs(view);
        List<BattleView.EnemyView> enemies = view.enemies();
        for (int i = 0; i < panelXs.length; i++) {
            // v0.5.0.7：面板高度按意图行数自适应，命中判定要跟着用同一个高度
            int panelH = i < enemies.size() ? enemyHeight(enemies.get(i)) : intentLineY(0) + 24;
            if (isInside(mouseX, mouseY, panelXs[i], ENEMY_ROW_Y, ENEMY_W - 4, panelH)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** 按可用宽度裁剪文本（v0.2.3：防止长名字 / 长状态把文字顶出屏幕）。 */
    private String trim(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        return font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, maxWidth);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty() && lines.size() < 6) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
            remaining = remaining.substring(line.length());
        }
        return lines;
    }

    private void button(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean enabled) {
        graphics.fill(x, y, x + w, y + h, enabled ? 0xFF3A3A48 : 0xFF26262E);
        border(graphics, x, y, x + w, y + h, enabled ? PANEL_EDGE : 0xFF33333C);
        TextStyle.draw(graphics, font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, enabled ? TEXT : TEXT_DIM);
    }

    private void border(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color);
        graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color);
        graphics.fill(x2 - 1, y1, x2, y2, color);
    }
}
