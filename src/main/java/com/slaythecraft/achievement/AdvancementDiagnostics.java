package com.slaythecraft.achievement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 成就树的自证诊断（v0.5.0.12）。
 *
 * <p><b>为什么需要它</b>：原版 {@code ServerAdvancementManager} 对**反序列化失败**的进度
 * 只打一行 {@code LOGGER.error("Parsing error loading custom advancement …")} 就静默丢弃，
 * 之后 {@code AdvancementTree.addAll} 会因子节点找不到父节点再打一行
 * {@code "Couldn't load advancements: …"} 并 {@code break}。游戏里的表现是
 * "根成就还在，其余成就整片消失"——即使用户把成就全解锁也依然只有根。
 * 这类问题**没有任何界面提示**，只能靠日志。</p>
 *
 * <p>所以这里在服务端启动时、以及玩家登录时，把本模组成就的**真实加载结果**打到日志里：
 * 只写几条 INFO，正常情况下不值得注意；一旦成就缺失，日志里会直接点名是哪一条、
 * 缺的是"文件没加载"还是"父节点没解析"。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class AdvancementDiagnostics {

    private AdvancementDiagnostics() {
    }

    /** 服务端启动：汇总一次"登记表 vs 实际加载"。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerAdvancementManager manager = server.getAdvancements();
        if (manager == null) {
            SlayTheCraft.LOGGER.warn("[STC/成就] 服务端还没有 AdvancementManager，跳过诊断");
            return;
        }

        List<String> missing = new ArrayList<>();
        for (AchievementService.Entry entry : AchievementService.ENTRIES) {
            AdvancementHolder holder = manager.get(entry.id());
            if (holder == null) {
                missing.add(entry.id() + "（登记为 " + entry.file() + ".json）");
            } else if (holder.value().display().isEmpty() && entry.parent() != null) {
                missing.add(entry.id() + "（**没有 display 段，不会在成就界面显示**）");
            }
        }

        AdvancementTree tree = manager.tree();
        int nodeCount = 0;
        int rootCount = 0;
        Map<String, Integer> childrenOf = new LinkedHashMap<>();
        for (AdvancementNode root : tree.roots()) {
            rootCount++;
            nodeCount += countNodes(root, childrenOf);
        }

        SlayTheCraft.LOGGER.info("[STC/成就] 登记 {} 条；成就树里 {} 个节点、{} 个根（含原版）",
                AchievementService.ENTRIES.length, nodeCount, rootCount);

        if (missing.isEmpty()) {
            SlayTheCraft.LOGGER.info("[STC/成就] ✅ 本模组 {} 条成就全部已加载且都有 display",
                    AchievementService.ENTRIES.length);
        } else {
            SlayTheCraft.LOGGER.error("[STC/成就] ❌ 有 {} 条成就没被加载（这就是「只有根显示」的原因）：",
                    missing.size());
            for (String m : missing) {
                SlayTheCraft.LOGGER.error("[STC/成就]    - {}", m);
            }
            SlayTheCraft.LOGGER.error("[STC/成就] 请在日志里搜索 「Couldn't parse data file」"
                    + " 或 「Parsing error loading custom advancement」，那里有原版给出的具体原因");
        }
    }

    /** 玩家登录：把该玩家真正**收到**的节点列出来（原版可见性过滤之后的结果）。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 服务端权威的"这个玩家能看到哪些进度"，取自 PlayerAdvancements 内部
        Collection<ResourceLocation> visible = visibleAdvancements(player);
        if (visible == null) {
            return;
        }
        List<String> mine = new ArrayList<>();
        for (ResourceLocation id : visible) {
            if (SlayTheCraft.MODID.equals(id.getNamespace())) {
                mine.add(id.getPath());
            }
        }
        java.util.Collections.sort(mine);
        NameCount total = totalLoaded(player.getServer());
        SlayTheCraft.LOGGER.info("[STC/成就] 玩家 {} 可见本模组成就 {} / 已登记 {} 条：{}",
                player.getScoreboardName(), mine.size(), total.registered, mine);
        if (mine.size() < total.registered) {
            SlayTheCraft.LOGGER.warn("[STC/成就] ⚠️ 可见数少于登记数——差的是没被授予前置，"
                    + "或是这一条根本没加载成功（见上一条启动日志）");
        }
    }

    private record NameCount(int registered, int loaded) {
    }

    private static NameCount totalLoaded(MinecraftServer server) {
        int loaded = 0;
        if (server != null && server.getAdvancements() != null) {
            for (ResourceLocation id : server.getAdvancements().getAllAdvancements().stream()
                    .map(AdvancementHolder::id).toList()) {
                if (SlayTheCraft.MODID.equals(id.getNamespace())) {
                    loaded++;
                }
            }
        }
        return new NameCount(AchievementService.ENTRIES.length, loaded);
    }

    /**
     * 取该玩家"可见"的进度集合。
     *
     * <p>原版把这个集合放在 {@code PlayerAdvancements.visible} 里，是个私有字段、没有公开读取口，
     * 所以这里用反射读；读不到就安静地跳过诊断（**绝不能让诊断本身把正常游戏搞崩**）。</p>
     */
    @SuppressWarnings("unchecked")
    private static Collection<ResourceLocation> visibleAdvancements(ServerPlayer player) {
        try {
            Object advancements = player.getAdvancements();
            java.lang.reflect.Field field = advancements.getClass().getDeclaredField("visible");
            field.setAccessible(true);
            Object value = field.get(advancements);
            if (!(value instanceof Collection<?> collection)) {
                return null;
            }
            List<ResourceLocation> result = new ArrayList<>();
            for (Object o : collection) {
                if (o instanceof AdvancementHolder holder) {
                    result.add(holder.id());
                }
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static int countNodes(AdvancementNode node, Map<String, Integer> childrenOf) {
        int n = 1;
        int kids = 0;
        for (AdvancementNode child : node.children()) {
            kids++;
            n += countNodes(child, childrenOf);
        }
        childrenOf.put(node.holder().id().toString(), kids);
        return n;
    }
}
