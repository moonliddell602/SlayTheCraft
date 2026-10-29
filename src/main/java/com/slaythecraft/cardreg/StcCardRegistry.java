package com.slaythecraft.cardreg;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * **卡牌数据包注册表 {@code stc:card}**（v0.5.5.0 A6-1，来自设计文档 §5.3 / handover §5.1 A6）。
 *
 * <p>数据包里的 JSON 放在 {@code data/<数据包命名空间>/stc/card/<卡牌 id>.json}
 * （NeoForge 的规则是 {@code data/<ns>/<注册表键命名空间>/<注册表键路径>/}，所以路径里有两个 {@code stc}）。</p>
 *
 * <p>三条设计约定，写给下一个人：</p>
 * <ol>
 *   <li><b>注册表是"覆盖层"，不是"唯一来源"</b>：内置 246 张卡的定义仍然在代码
 *       （{@link com.slaythecraft.combat.core.CardLibrary}）里 —— 因为引擎自检要在**裸 JVM** 上跑，
 *       而注册表必须依赖 Minecraft 的数据包加载。数据包的作用是**新增卡**和**覆盖内置卡的数值**，
 *       由 {@link CardRegistryLoader} 在数据包加载完之后"灌"进引擎（见
 *       {@link com.slaythecraft.combat.core.CardLibrary#applyExternal}）。</li>
 *   <li><b>同步给客户端</b>：这里传了第二个 codec，所以注册表会随数据包同步到客户端，
 *       客户端（{@code StcClientSetup}）再把它灌进同一个覆盖层 —— 图鉴 / 卡面 / 商店才看得到数据包卡。</li>
 *   <li><b>卡牌效果不能序列化</b>：JSON 里写的是**内置行为表**的 id
 *       （{@link com.slaythecraft.combat.core.CardBehaviors}），不是任意脚本。</li>
 * </ol>
 */
public final class StcCardRegistry {

    /** 注册表键：{@code stc:card}。 */
    public static final ResourceKey<Registry<CardDefinition>> CARDS =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "card"));

    private StcCardRegistry() {
    }

    /**
     * 在 **mod 事件总线**上登记数据包注册表（由 {@code SlayTheCraft} 的构造器调用）。
     *
     * <p>用同一个 {@link CardDefinition#CODEC} 当网络 codec —— 1.20.5 起原版同步数据包注册表时
     * 用的就是元素 codec 编码成 NBT，所以一个 codec 两端通用。</p>
     */
    public static void registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CARDS, CardDefinition.CODEC, CardDefinition.CODEC);
    }
}
