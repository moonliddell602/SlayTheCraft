package com.slaythecraft.binder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Curios 兼容层（编译期 {@code implementation}，运行期软依赖）。
 *
 * <p>与 {@code FtbTeamsCompat} 同样的隔离约定：**任何类型来自 Curios，只有在 curios 已加载时
 * 才允许加载本类**，调用方必须先过 {@code BinderResolver.curiosPresent()}（S1 落地），
 * 否则会抛 {@link NoClassDefFoundError}。</p>
 *
 * <p>API 已按 {@code curios-neoforge-9.5.1+1.21.1.jar}（含 sources）校准：</p>
 * <ul>
 *   <li>{@code CuriosApi.getCuriosInventory(LivingEntity)} → {@code Optional<ICuriosItemHandler>}；
 *       {@code isEquipped(Item)} / {@code findFirstCurio(Item)}；</li>
 *   <li>{@code CuriosApi.registerCurio(Item, ICurioItem)}：**必须**调用，否则物品没有
 *       {@code CuriosCapability.ITEM}，根本放不进饰品槽 —— 这就是"必须携带收纳包"一直失败的原因
 *       （v0.2.4 修复）。Curios 只在 {@code RegisterCapabilitiesEvent} 里为"实现了 ICurioItem 的物品"
 *       或"注册过的物品"挂上这个能力，所以本类提供一个注册入口。</li>
 * </ul>
 */
final class CuriosCompat {

    private CuriosCompat() {
    }

    /**
     * 把【卡牌收纳包】注册成 Curios 饰品。
     *
     * <p>注意 {@code canEquipFromUse()} 返回 false：右键仍然是"打开卡牌收纳包界面"，
     * 装备靠饰品栏拖拽（或饰品栏里 Shift 点击），两条路径不会互相抢。</p>
     */
    static void registerBinder(Item binder) {
        CuriosApi.registerCurio(binder, new ICurioItem() {
            @Override
            public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
                return false;
            }
        });
    }

    /** 饰品槽里是否装备了指定物品。 */
    static boolean isEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }

    /** 饰品槽里该物品的第一份实例，没有则返回空。 */
    static java.util.Optional<ItemStack> findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(result -> result.stack());
    }

    /** 玩家当前装备的饰品槽位 id（用于提示"请放入 XX 槽"）。 */
    static java.util.Optional<String> equippedSlotId(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(result -> result.slotContext().identifier());
    }
}
