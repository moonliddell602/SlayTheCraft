package com.slaythecraft.data;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.resources.ResourceLocation;

/**
 * 卡牌 id 的合法性来源。
 *
 * <p>v0.5.5.0（A6-1）起，卡牌定义有了**两个来源**：代码内置（{@link CardLibrary} 里的静态定义）
 * 与数据包注册表 {@code stc:card}（{@code com.slaythecraft.cardreg}）。数据包那一侧由
 * {@code CardRegistryLoader} 在加载完之后**灌进** {@link CardLibrary} 的同一张表
 * （{@code applyExternal}），所以这里继续查 {@code CardLibrary} 就是对的 ——
 * 调用方（指令、奖励、UI、组牌）一行都不用改。</p>
 *
 * <p>⚠️ 这也是为什么**没有**把 {@code CardLibrary} 换成真正的注册表：{@code combat/core} 必须保持
 * 零 Minecraft 依赖（引擎自检要在裸 JVM 上跑），而注册表必须依赖数据包加载。数据包是
 * **覆盖层**，内置卡永远是回退。</p>
 */
public final class CardIds {

    private CardIds() {
    }

    /** 是否是当前已知的卡牌 id（内置卡 + 数据包注册表里加载进来的卡）。 */
    public static boolean isKnown(ResourceLocation id) {
        return SlayTheCraft.MODID.equals(id.getNamespace()) && CardLibrary.byId(id.getPath()) != null;
    }

    /** 把代码卡池里的 id 转成带命名空间的 {@link ResourceLocation}。 */
    public static ResourceLocation of(Card card) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, card.id());
    }
}
