package com.slaythecraft.binder;

import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * "玩家是否携带【卡牌收纳包】"的唯一判定入口（设计文档 §6.1）。
 *
 * <p>规则：</p>
 * <ol>
 *   <li>装了 Curios 且饰品槽里有收纳包 → 通过</li>
 *   <li>装了 Curios、{@code requireCurios=true} 且槽里没有 → 拒绝（规范玩法）</li>
 *   <li>没装 Curios（或 {@code requireCurios=false}）→ 降级检查双手与背包，保证模组单独也能玩</li>
 * </ol>
 *
 * <p>没有收纳包就不能开始尖塔对战，这一条由 {@code CombatManager} 在两种入口上统一校验。</p>
 */
public final class BinderResolver {
    private static final String CURIOS_MOD_ID = "curios";

    /** 收纳包专属饰品槽的 id（由 {@code data/stc/curios/slots/binder.json} 定义）。 */
    public static final String BINDER_SLOT = "binder";

    private BinderResolver() {
    }

    /** Curios 是否在场（决定能否触碰 {@link CuriosCompat}）。 */
    public static boolean curiosPresent() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    /**
     * 把【卡牌收纳包】注册成 Curios 饰品（v0.2.4 修复）。
     *
     * <p>必须在 mod 总线的 {@code RegisterCapabilitiesEvent} 里调用：Curios 就是在这个时机
     * 决定"哪些物品拥有饰品能力"。以前我们只写了物品标签、没有注册，导致收纳包**根本放不进饰品槽**，
     * 于是所有"必须携带收纳包"的判定全部失败。</p>
     *
     * <p>Curios 不在场时直接跳过（不加载 Curios 类型）。</p>
     */
    public static void registerCuriosItem() {
        if (!curiosPresent()) {
            return;
        }
        CuriosCompat.registerBinder(SlayTheCraft.CARD_BINDER.get());
    }

    /**
     * 是否"携带"了收纳包（v0.2.6 改；v0.4.6.3 补充说明）。
     *
     * <p>实测反馈："你仍然没有为 Curios 创建栏位。因此，重新制作这方面的触发条件：
     * 玩家主手手持来代替原来需要装到饰品栏位才能执行的操作。"</p>
     *
     * <p>所以判定是：**主手或副手手持**即算携带；如果装了 Curios 并且确实装备在饰品槽里，
     * 也算（那条路保留但不强求）。</p>
     *
     * <p><b>这条是"开始尖塔对战"的规则</b>（设计文档 §6.1），所以"放在背包里没拿出来"仍然不算。
     * 只是想**打开收纳包界面**（B 键 / 右键 / 尖塔入口按钮）的话走 {@link #carried(Player)} ——
     * v0.4.6.3 需求："如果卡牌收纳包在背包内，也可以允许 B 键打开 UI。"</p>
     */
    public static boolean hasBinder(Player player) {
        Item binder = SlayTheCraft.CARD_BINDER.get();
        if (holding(player, binder)) {
            return true;
        }
        return curiosPresent() && CuriosCompat.isEquipped(player, binder);
    }

    /** 主手或副手是不是拿着收纳包。 */
    public static boolean holding(Player player, Item binder) {
        return player.getMainHandItem().is(binder) || player.getOffhandItem().is(binder);
    }

    /** 收纳包当前的位置描述，用于提示。 */
    public static String binderLocation(Player player) {
        Item binder = SlayTheCraft.CARD_BINDER.get();
        if (player.getMainHandItem().is(binder)) {
            return "主手";
        }
        if (player.getOffhandItem().is(binder)) {
            return "副手";
        }
        if (curiosPresent()) {
            var slot = CuriosCompat.equippedSlotId(player, binder);
            if (slot.isPresent()) {
                return "饰品槽 " + slot.get();
            }
        }
        if (player.getInventory().contains(new ItemStack(binder))) {
            return "背包里";
        }
        return "未携带";
    }

    /** 给玩家看的"怎么携带"说明（未携带时的提示里带上它）。 */
    public static String equipHint() {
        return "把【卡牌收纳包】拿在**主手或副手**即可（装了 Curios 的话放进饰品槽也算）。";
    }

    /**
     * 玩家身上**任何位置**有没有收纳包（v0.4.6.3）。
     *
     * <p>需求原文："如果卡牌收纳包在背包内，也可以允许 B 键打开 UI。"</p>
     *
     * <p>所以"打开界面"判定用这条：主手 / 副手 / Curios 饰品槽 / **背包（含快捷栏与护甲槽）** 都算。
     * 「开始尖塔对战」仍然用更严格的 {@link #hasBinder(Player)} —— 那条要求拿在手上，
     * 是设计文档 §6.1 的玩法规则，本次没有改动。</p>
     */
    public static boolean carried(Player player) {
        Item binder = SlayTheCraft.CARD_BINDER.get();
        if (holding(player, binder)) {
            return true;
        }
        if (curiosPresent() && CuriosCompat.isEquipped(player, binder)) {
            return true;
        }
        // 背包判定用谓词重载：物品一旦带上数据组件，ItemStack 相等判断会失配
        return player.getInventory().contains(stack -> stack.is(binder));
    }

    /** 收纳包是不是只在背包里（既没拿在手上、也没进饰品槽）—— 用来给"背包里也能开界面"的提示措辞。 */
    public static boolean onlyInBackpack(Player player) {
        return !hasBinder(player) && carried(player);
    }
}
