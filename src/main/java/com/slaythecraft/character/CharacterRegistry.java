package com.slaythecraft.character;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * 可选角色（设计文档 §7；v0.0.3 W3 增加描述、初始遗物与第四个角色）。
 *
 * <p>代号与《杀戮尖塔》对应：铁甲战士 / 静默猎手 / 故障机器人，
 * 外加本 mod 的原创角色"叛逃掠夺者"。初始遗物按原版设定：燃烧之血 / 蛇之戒指 / 破损核心。</p>
 */
public final class CharacterRegistry {

    private static final Map<ResourceLocation, CharacterProfile> BY_ID = new LinkedHashMap<>();
    private static final List<CharacterProfile> ORDERED = new ArrayList<>();

    /** 铁甲战士：高血、直接伤害与易伤；初始遗物【燃烧之血】；不用充能球。 */
    public static final CharacterProfile IRONCLAD = register(new CharacterProfile(
            id("ironclad"), 80, 3, ids("strike", "strike", "strike", "strike", "strike",
            "defend", "defend", "defend", "defend", "bash"), "burning_blood", 0, ""));

    /** 静默猎手：灵活、虚弱与抽牌；初始遗物【蛇之戒指】；不用充能球。 */
    public static final CharacterProfile SILENT = register(new CharacterProfile(
            id("silent"), 70, 3, ids("strike", "strike", "strike", "strike", "strike",
            "defend", "defend", "defend", "defend", "defend", "neutralize", "survivor"),
            "ring_of_the_snake", 0, ""));

    /** 故障机器人：充能球机制（v0.0.3 W4）；初始遗物【破损核心】；**只有它有 3 个充能球栏位**。 */
    public static final CharacterProfile DEFECT = register(new CharacterProfile(
            id("defect"), 75, 3, ids("strike", "strike", "strike", "strike",
            "defend", "defend", "defend", "defend", "zap", "frost"), "cracked_core", 3, ""));

    /**
     * 叛逃掠夺者（v0.0.3 W3）：一个倡导和平的掠夺者。
     *
     * <p>初始遗物【臂弩】：战斗开始时把一张【箭在弦上】加入手牌。</p>
     */
    public static final CharacterProfile PILLAGER = register(new CharacterProfile(
            id("pillager"), 75, 3, ids("strike", "strike", "strike", "strike",
            "defend", "defend", "defend", "defend", "aimed_shot", "take_cover", "roll", "smoke_bomb"),
            "crossbow", 0, ""));

    private CharacterRegistry() {
    }

    /** 注册是否已经冻结（列表第一次被读取之后就不再接受注册）。 */
    private static boolean frozen;

    /**
     * 注册一个角色（v0.5.0.9 起**对附属包开放**）。
     *
     * <p>需求原文："角色选择后续需要改成滚轮式显示（因为后面还有新角色，以及要给附属包的开发者留下接口）。"</p>
     *
     * <p>附属包在 {@code FMLCommonSetupEvent}（或更早）里调用本方法即可加入自己的角色：</p>
     * <pre>
     * CharacterRegistry.register(new CharacterProfile(myId, hp, energy, deck, relicId, orbSlots, ""));
     * </pre>
     *
     * <p>两条纪律（保证列表**确定性**，避免"有时能选到、有时不能"）：</p>
     * <ul>
     *   <li>**列表被读过之后不能再注册**——第一个 {@link #all()} / {@link #byIndex(int)} 调用会把注册冻结，
     *       之后再注册会被拒绝并记一条警告（否则游戏中途多出一个角色，索引与存档都会错位）；</li>
     *   <li>**id 唯一**——重复 id 会被拒绝（否则 {@link #byId(ResourceLocation)} 会指向另一只）。</li>
     * </ul>
     *
     * @return 注册成功返回该角色；被拒绝返回 {@code null}
     */
    public static CharacterProfile register(CharacterProfile profile) {
        if (profile == null || profile.id() == null) {
            log("[STC] 角色注册被拒绝：profile 或 id 为 null");
            return null;
        }
        if (frozen) {
            log("[STC] 角色注册被拒绝：注册表已冻结（角色列表已被读取过）。"
                    + "请在 FMLCommonSetupEvent 或更早的阶段注册 " + profile.id());
            return null;
        }
        CharacterProfile existing = BY_ID.get(profile.id());
        if (existing != null) {
            log("[STC] 角色注册被拒绝：id " + profile.id() + " 已经被注册过了");
            return null;
        }
        BY_ID.put(profile.id(), profile);
        ORDERED.add(profile);
        log("[STC] 已注册角色 " + profile.id() + "（当前共 " + ORDERED.size() + " 个）");
        return profile;
    }

    /**
     * 打一条日志，**对失败免疫**（v0.5.0.9）。
     *
     * <p>为什么要这么小心：本类自己的静态常量就是靠 {@link #register} 初始化的，
     * 而 {@code SlayTheCraft} 的静态初始化又会反过来碰到注册表 ——
     * 直接引用 {@code SlayTheCraft.LOGGER} 会引发 {@code ExceptionInInitializerError}
     * （自检里已经抓到过一次）。所以这里用 try/catch 包起来，
     * 日志系统还没就绪时**只是不打日志**，绝不连累注册本身。</p>
     */
    private static void log(String message) {
        try {
            SlayTheCraft.LOGGER.info(message);
        } catch (Throwable ignored) {
            // 日志还没就绪（静态初始化顺序）——注册本身照常进行
        }
    }

    /** 冻结注册（列表一旦被读取就调用；之后 {@link #register} 一律拒绝）。 */
    private static void freeze() {
        frozen = true;
    }

    /** 注册是否已冻结（自检/附属包用）。 */
    public static boolean isFrozen() {
        return frozen;
    }

    /** 已注册的角色数量。 */
    public static int count() {
        return ORDERED.size();
    }

    /**
     * 全部角色的固定顺序（界面按这个顺序展示）。
     *
     * <p>**第一次调用会冻结注册**：之后 {@link #register} 不再接受新角色。</p>
     */
    public static List<CharacterProfile> all() {
        freeze();
        return List.copyOf(ORDERED);
    }

    public static CharacterProfile byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static CharacterProfile byIndex(int index) {
        freeze();
        return index >= 0 && index < ORDERED.size() ? ORDERED.get(index) : null;
    }

    public static int indexOf(ResourceLocation id) {
        for (int i = 0; i < ORDERED.size(); i++) {
            if (ORDERED.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, path);
    }

    private static List<ResourceLocation> ids(String... paths) {
        List<ResourceLocation> list = new ArrayList<>(paths.length);
        for (String path : paths) {
            list.add(id(path));
        }
        return list;
    }
}
