package com.slaythecraft.cardreg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.CardLibrary;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 把数据包注册表 {@code stc:card} **灌进引擎卡池**（v0.5.5.0 A6-1）。
 *
 * <p>时机：</p>
 * <ul>
 *   <li>{@link ServerStartedEvent}：服务器起来（数据包注册表已经加载完）；</li>
 *   <li>{@link OnDatapackSyncEvent}：`/reload` 之后，以及有玩家加入时（原版在这个点已经把
 *       数据包注册表重新加载过）；</li>
 *   <li>客户端自己那侧由 {@code StcClientSetup} 用**同步下来的**注册表调用同一个
 *       {@link #apply(RegistryAccess)}。</li>
 * </ul>
 *
 * <p>⚠️ 每次应用都会**先还原代码内置卡池**（{@code CardLibrary.resetExternal()}），再整体盖上数据包
 * 定义 —— 所以不存在"某次加载失败后卡池残缺"的状态：失败的那一张只是**没被覆盖**，
 * 内置定义照常可用。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CardRegistryLoader {

    private CardRegistryLoader() {
    }

    /**
     * 一次应用的结果。
     *
     * @param applied 真正生效的数据包卡张数
     * @param errors  被跳过的卡与原因（**空**才表示这次加载完全干净）
     */
    public record Result(int applied, List<String> errors) {

        public boolean clean() {
            return errors.isEmpty();
        }
    }

    /**
     * 读注册表 → 校验 → 整体应用。
     *
     * <p>注册表不存在（例如还没加载完）时按"没有数据包卡"处理：只还原内置卡池。</p>
     */
    public static Result apply(RegistryAccess access) {
        List<String> errors = new ArrayList<>();
        Optional<Registry<CardDefinition>> registry = access == null
                ? Optional.empty() : access.registry(StcCardRegistry.CARDS);
        if (registry.isEmpty()) {
            CardLibrary.resetExternal();
            return new Result(0, errors);
        }
        List<CardLibrary.ExternalCard> cards = new ArrayList<>();
        for (Map.Entry<ResourceKey<CardDefinition>, CardDefinition> entry : registry.get().entrySet()) {
            ResourceLocation key = entry.getKey().location();
            if (!SlayTheCraft.MODID.equals(key.getNamespace())) {
                errors.add("卡 " + key + "：命名空间必须是 " + SlayTheCraft.MODID
                        + "（文件名就是卡牌 id，不要带别的命名空间）");
                continue;
            }
            List<String> problems = new ArrayList<>();
            CardLibrary.ExternalCard external = entry.getValue().toExternal(key.getPath(), problems);
            if (external == null || !problems.isEmpty()) {
                errors.addAll(problems);
                continue;
            }
            cards.add(external);
        }
        CardLibrary.applyExternal(cards);
        return new Result(cards.size(), errors);
    }

    /** 应用并把结果写进日志（服务器侧的两个事件点用）。 */
    public static Result applyAndLog(RegistryAccess access) {
        Result result = apply(access);
        if (result.clean()) {
            SlayTheCraft.LOGGER.info("[STC] stc:card 注册表：{} 张数据包卡已接入引擎卡池", result.applied());
        } else {
            SlayTheCraft.LOGGER.warn("[STC] stc:card 注册表：{} 张接入，{} 条被跳过",
                    result.applied(), result.errors().size());
            for (String error : result.errors()) {
                SlayTheCraft.LOGGER.warn("[STC]   - {}", error);
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        applyAndLog(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        applyAndLog(event.getPlayerList().getServer().registryAccess());
    }
}
