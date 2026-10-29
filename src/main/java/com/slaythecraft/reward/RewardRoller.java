package com.slaythecraft.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;

/**
 * 卡牌奖励抽取（设计文档 §9.2）。纯逻辑、不依赖 Minecraft，便于无头校验：
 * 按**角色专属池** + 品质权重抽 N 张候选；BOSS 奖励只从**该角色的稀有池**抽。
 *
 * <p>v0.4.0：池子按角色分开（不再有跨角色公共池），无色牌不进奖励池（见
 * {@link CardLibrary#rewardPool(String)}）。</p>
 *
 * <p>v0.5.1.5：**金色品质取消、统一成稀有**，所以 BOSS 那一档从"金卡池"改成了
 * {@link CardLibrary#rarePool(String)}。{@code golden} 这个参数名保留（它标的是"BOSS 奖励"这一档，
 * 不是品质），免得改动扩散到网络包与界面。</p>
 *
 * <h2>v0.5.1.0：奖励**固定 N 张**</h2>
 *
 * <p>反馈："卡牌奖励是可以重复出现牌组里已经有的牌的，但是尤其金色奖励经常出现只有一张卡牌奖励的
 * 问题。强制卡牌奖励为 3。如果角色的图鉴内已做完金色卡数量不超过 3，则金色卡牌奖励必然包含已经做完的金卡。"
 * （v0.5.1.5 之后这里的"金色卡"读作"稀有卡"。）</p>
 *
 * <p>当时的根因：**金卡池非常小**（战士 2 张，猎手 / 机宝 / 叛逃掠夺者各 1 张），
 * 而原来的 {@code pick} 是"**不重复**抽"，所以那一档最多只出 1–2 张。现在：</p>
 * <ul>
 *   <li>池子 ≤ N 张 → **直接把池子全部给出去**（既满足"固定 N"里"尽量多给"的意图，
 *       也正好满足"池子不超过 3 时必然包含全部"）；</li>
 *   <li>池子 > N 张 → 按权重抽 N 张**不重复**的（v0.5.1.5 之后稀有池都 > 3，所以走的是这一支）；</li>
 *   <li>池子小于 N 时用**同一张卡重复填充**到 N 张（需求明确允许奖励里出现已有的牌）。</li>
 * </ul>
 */
public final class RewardRoller {

    private RewardRoller() {
    }

    /**
     * @param characterId 玩家角色 id 字符串（{@code stc:ironclad} 等）；null 表示还没选角色（池子为空）
     * @param golden      **BOSS 奖励**这一档：只从该角色的**稀有池**抽（v0.5.1.5 起；金色品质已取消）
     * @param count       候选数量（三选一 = 3）
     */
    public static List<Card> roll(String characterId, boolean golden, int count, Random random) {
        // 单人视角（多人卡牌不进池）
        return roll(characterId, golden, count, random, 1);
    }

    /**
     * 同上，但显式给出**队伍人数**（v0.5.1.4）。
     *
     * <p>需求："新卡牌类型：多人卡牌 —— 这些卡牌只有在多人模式下才会从奖励池里出现。"
     * 所以抽取时要按参战人数过滤（{@code partySize <= 1} 时多人卡牌不进池）。</p>
     *
     * <p>v0.5.1.5：{@code golden} 这一档（BOSS 奖励）**改成从稀有卡池抽**
     * —— 需求："BOSS奖励给予的『金色卡奖励』实际上应该是从当前角色的稀有卡池抽取奖励。"</p>
     */
    public static List<Card> roll(String characterId, boolean golden, int count, Random random, int partySize) {
        List<Card> pool = golden ? CardLibrary.rarePool(characterId, partySize)
                : CardLibrary.rewardPool(characterId, partySize);
        return pick(pool, count, random);
    }

    /**
     * 从池子里抽 {@code count} 张候选（v0.5.1.0：**保证正好 count 张**，池子不够就重复）。
     *
     * <p>规则（合起来同时满足需求的两句）：</p>
     * <ol>
     *   <li>先从池子里按权重抽 {@code min(count, 池子大小)} 张**不重复**的卡
     *       —— 于是"池子 ≤ 3"时这一步就把**全部**金卡都放进来了；</li>
     *   <li>不够 {@code count} 张时，用已经抽到的那几张**循环补齐**
     *       （需求明确允许奖励里出现已有的牌）—— 于是永远是 3 张。</li>
     * </ol>
     */
    static List<Card> pick(List<Card> pool, int count, Random random) {
        if (pool == null || pool.isEmpty() || count <= 0) {
            return List.of();
        }
        List<Card> remaining = new ArrayList<>(pool);
        List<Card> picked = new ArrayList<>();
        int distinct = Math.min(count, remaining.size());
        for (int i = 0; i < distinct; i++) {
            Card card = weightedPick(remaining, random);
            if (card == null) {
                break;
            }
            picked.add(card);
            remaining.remove(card);
        }
        if (picked.isEmpty()) {
            return List.of();
        }
        // 池子比一次奖励还小：用抽到的卡循环补齐到 count 张（需求允许重复）
        for (int i = 0; picked.size() < count; i++) {
            picked.add(picked.get(i % distinct));
        }
        return List.copyOf(picked);
    }

    /**
     * BOSS 奖励要保证"池子不超过 3 张时全部包含"（v0.5.1.0 的规则，v0.5.1.5 换成了稀有池）。
     *
     * <p>纯查询：给定角色的**稀有池**是不是"小池"（≤ {@code count}）。自检与界面提示共用。</p>
     */
    public static boolean rarePoolIsSmall(String characterId, int count) {
        return CardLibrary.rarePool(characterId).size() <= count;
    }

    static Card weightedPick(List<Card> pool, Random random) {
        int total = 0;
        for (Card card : pool) {
            total += weight(card);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Card card : pool) {
            roll -= weight(card);
            if (roll < 0) {
                return card;
            }
        }
        return pool.get(pool.size() - 1);
    }

    static int weight(Card card) {
        return Math.max(1, card.rarity().weight());
    }
}
