package com.slaythecraft.combat.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * **数据包卡牌的"行为"表**（v0.5.5.0 A6-1）。
 *
 * <p>数据包里的卡牌只有**数据**（名称 / 费用 / 类型 / 数值…），没有代码 —— 而本引擎的卡牌效果是
 * Java 的 {@link CardEffect} 函数。所以这里提供一张**内置行为表**：JSON 里写
 * {@code {"type": "damage", "numbers": [6]}}，加载时按 id 在这里取出构造器，拼出真正的效果。</p>
 *
 * <p>这张表的存在意味着两件事，写给整合包作者：</p>
 * <ul>
 *   <li><b>能做什么</b>：下面登记的每一种行为都可以在 JSON 里直接组合使用（一张卡可以有多个行为，
 *       按顺序结算）；</li>
 *   <li><b>不能做什么</b>：原版那些"特殊机制"（如【腐化】【双发】【无形】）没有对应行为，
 *       数据包**造不出**它们 —— 要加新机制得在代码里登记一条新行为。</li>
 * </ul>
 *
 * <p>行为里的 {@code numbers} / {@code strings} 都是位置参数（见 {@link Definition#numbers()} /
 * {@link Definition#strings()} 声明的个数），{@link #validate} 会检查数量对不对。</p>
 *
 * <p>这个类零 Minecraft 依赖，所以裸 JVM 的引擎自检能直接跑它。</p>
 */
public final class CardBehaviors {

    /** 行为构造器：拿卡名与参数，造出一个可执行的 {@link CardEffect}。 */
    @FunctionalInterface
    public interface Builder {
        CardEffect build(String cardName, List<Integer> numbers, List<String> strings);
    }

    /**
     * 一条行为。
     *
     * @param id       JSON 里 {@code "type"} 写的名字
     * @param summary  一句话说明（`/stc card behaviors` 直接打印给整合包作者看）
     * @param numbers  需要几个数值参数
     * @param strings  需要几个文本参数
     * @param builder  真正的构造器
     */
    public record Definition(String id, String summary, int numbers, int strings, Builder builder) {
    }

    private static final Map<String, Definition> BY_ID = new LinkedHashMap<>();

    private CardBehaviors() {
    }

    /** 登记一条行为（同 id 覆盖，方便调试）。 */
    public static void register(Definition definition) {
        if (definition != null && definition.id() != null && definition.builder() != null) {
            BY_ID.put(definition.id(), definition);
        }
    }

    /** 按 id 取行为定义（不认识返回 {@code null}）。 */
    public static Definition byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** 所有已登记的行为 id（按登记顺序）。 */
    public static List<String> ids() {
        return List.copyOf(BY_ID.keySet());
    }

    /** 所有已登记的行为定义。 */
    public static List<Definition> all() {
        return List.copyOf(BY_ID.values());
    }

    /** 登记了几条行为（自检用）。 */
    public static int count() {
        return BY_ID.size();
    }

    /**
     * 校验一条行为调用（id 认不认识 + 参数个数对不对）。
     *
     * @return 出错说明；没问题返回 {@code null}
     */
    public static String validate(String id, List<Integer> numbers, List<String> strings) {
        Definition definition = byId(id);
        if (definition == null) {
            return "未知行为「" + id + "」（可用：" + String.join(" / ", ids()) + "）";
        }
        int numCount = numbers == null ? 0 : numbers.size();
        int strCount = strings == null ? 0 : strings.size();
        if (numCount < definition.numbers()) {
            return "行为「" + id + "」需要 " + definition.numbers() + " 个数值参数，实际 " + numCount + " 个";
        }
        if (strCount < definition.strings()) {
            return "行为「" + id + "」需要 " + definition.strings() + " 个文本参数，实际 " + strCount + " 个";
        }
        return null;
    }

    /**
     * 造出一个效果。
     *
     * @param cardName 卡名（用在战报的「」标注里）
     * @return 效果；行为不存在或参数不合法时返回 {@code null}（调用方负责报错，不要静默）
     */
    public static CardEffect build(String id, String cardName, List<Integer> numbers, List<String> strings) {
        if (validate(id, numbers, strings) != null) {
            return null;
        }
        String name = cardName == null ? "数据包卡牌" : cardName;
        return BY_ID.get(id).builder().build(name, numbers == null ? List.of() : numbers,
                strings == null ? List.of() : strings);
    }

    /** 取第 {@code index} 个数值参数（越界取 {@code fallback}）。 */
    static int number(List<Integer> numbers, int index, int fallback) {
        if (numbers == null || index < 0 || index >= numbers.size() || numbers.get(index) == null) {
            return fallback;
        }
        return numbers.get(index);
    }

    /** 取第 {@code index} 个文本参数（越界取 {@code fallback}）。 */
    static String text(List<String> strings, int index, String fallback) {
        if (strings == null || index < 0 || index >= strings.size() || strings.get(index) == null) {
            return fallback;
        }
        return strings.get(index);
    }

    /** 「卡名」这种战报标注。 */
    private static String tag(String cardName) {
        return "「" + cardName + "」";
    }

    /**
     * 卡牌的目标：指向性卡牌拿到的是玩家选的那一个；{@code ALL_ENEMIES} 类卡牌拿到的是
     * {@code null}，这时退回"第一个存活敌人"（纯伤害/上状态的单体行为用它兜底）。
     */
    private static Combatant targetOrFirst(CombatContext ctx, Combatant target) {
        if (target != null && target.alive()) {
            return target;
        }
        List<Enemy> alive = ctx.aliveEnemies();
        return alive.isEmpty() ? null : alive.get(0);
    }

    private static List<Enemy> alive(CombatContext ctx) {
        return ctx.aliveEnemies();
    }

    static {
        // ---- 伤害
        register(new Definition("damage", "对目标造成 N 点伤害（目标为空时取第一个存活敌人）", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    Combatant victim = targetOrFirst(ctx, target);
                    if (victim != null) {
                        ctx.dealDamage(self, victim, number(nums, 0, 0), tag(name));
                    }
                }));
        register(new Definition("damage_all", "对所有存活敌人各造成 N 点伤害", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    for (Enemy enemy : alive(ctx)) {
                        ctx.dealDamage(self, enemy, number(nums, 0, 0), tag(name));
                    }
                }));
        register(new Definition("damage_random", "对随机存活敌人造成 N 点伤害，共 M 次", 2, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    int times = Math.max(0, number(nums, 1, 1));
                    for (int i = 0; i < times; i++) {
                        List<Enemy> pool = alive(ctx);
                        if (pool.isEmpty()) {
                            return;
                        }
                        ctx.dealDamage(self, pool.get(ctx.nextRandomInt(pool.size())), number(nums, 0, 0),
                                tag(name));
                    }
                }));

        // ---- 格挡与资源
        register(new Definition("block", "获得 N 点格挡", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> ctx.grantPlayerBlock(number(nums, 0, 0), tag(name))));
        register(new Definition("next_turn_block", "下个回合开始时获得 N 点格挡", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> self.addNextTurnBlock(number(nums, 0, 0))));
        register(new Definition("draw", "抽 N 张牌", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> ctx.drawCards(number(nums, 0, 0))));
        register(new Definition("energy", "获得 N 点能量", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> ctx.gainEnergy(self, number(nums, 0, 0))));

        // ---- 自身增益 / 自伤
        register(new Definition("heal_self", "回复 N 点生命", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> self.heal(number(nums, 0, 0))));
        register(new Definition("lose_hp_self", "失去 N 点生命（无视格挡）", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> ctx.loseHp(self, number(nums, 0, 0), tag(name))));
        register(new Definition("strength_self", "获得 N 点力量", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> self.addStrength(number(nums, 0, 0))));
        register(new Definition("temporary_strength_self", "获得 N 点力量（本回合结束收回）", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> self.addTemporaryStrength(number(nums, 0, 0))));
        register(new Definition("dexterity_self", "获得 N 点敏捷", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> self.addDexterity(number(nums, 0, 0))));

        // ---- 负面状态
        register(new Definition("poison", "对目标施加 N 层中毒", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    Combatant victim = targetOrFirst(ctx, target);
                    if (victim != null) {
                        ctx.applyPoison(victim, number(nums, 0, 0));
                    }
                }));
        register(new Definition("poison_all", "对所有存活敌人施加 N 层中毒", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    for (Enemy enemy : alive(ctx)) {
                        ctx.applyPoison(enemy, number(nums, 0, 0));
                    }
                }));
        register(new Definition("weak", "对目标施加 N 层虚弱", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    Combatant victim = targetOrFirst(ctx, target);
                    if (victim != null) {
                        victim.addWeak(number(nums, 0, 0));
                    }
                }));
        register(new Definition("weak_all", "对所有存活敌人施加 N 层虚弱", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    for (Enemy enemy : alive(ctx)) {
                        enemy.addWeak(number(nums, 0, 0));
                    }
                }));
        register(new Definition("vulnerable", "对目标施加 N 层易伤", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    Combatant victim = targetOrFirst(ctx, target);
                    if (victim != null) {
                        victim.addVulnerable(number(nums, 0, 0));
                    }
                }));
        register(new Definition("vulnerable_all", "对所有存活敌人施加 N 层易伤", 1, 0,
                (name, nums, strs) -> (ctx, self, target) -> {
                    for (Enemy enemy : alive(ctx)) {
                        enemy.addVulnerable(number(nums, 0, 0));
                    }
                }));

        // ---- 造牌
        register(new Definition("add_card", "把 card 文本参数指定的卡加 N 张进手牌", 1, 1,
                (name, nums, strs) -> (ctx, self, target) -> {
                    Card card = CardLibrary.byId(text(strs, 0, null));
                    if (card != null) {
                        ctx.addCardsToHand(self, card, number(nums, 0, 1));
                    }
                }));
    }

    /** 行为 id 的稳定副本（自检里比对用）。 */
    public static List<String> idsCopy() {
        return new ArrayList<>(BY_ID.keySet());
    }
}
