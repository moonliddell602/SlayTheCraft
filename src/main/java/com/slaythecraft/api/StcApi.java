package com.slaythecraft.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * SlayTheCraft 给**整合包 / 附加模组 / 数据包作者**用的稳定接口（v0.4.5）。
 *
 * <p>目前提供两组接口，都不需要改本 mod 的代码：</p>
 *
 * <h2>1. 卡牌贴图</h2>
 * <p>卡面预留了"插画位"（战斗内手牌 / 卡牌详情 / 图鉴 / 牌堆列表，位置参照原版《杀戮尖塔》：
 * 插画在卡面**上半部分**）。两种用法：</p>
 * <ol>
 *   <li><b>零代码</b>：把图片放到 {@code assets/<卡牌id的命名空间>/textures/card/<卡牌路径>.png}。
 *       例如本体的【打击】是 {@code stc:strike} → {@code assets/stc/textures/card/strike.png}；</li>
 *   <li><b>指定别的路径</b>：{@code StcApi.registerCardTexture(id("stc:strike"), id("mypack:cards/my_strike"))}。</li>
 * </ol>
 * <p>贴图尺寸随意（绘制时会拉伸到插画位）。缺图时插画位画成占位框，不影响使用。</p>
 *
 * <h2>2. BOSS 音乐</h2>
 * <p>每层 BOSS 战可以有自己的音乐：先在自己的 {@code sounds.json} 里定义一个声音事件，
 * 再用 {@code StcApi.registerBossMusic(1, id("mypack:boss_floor1"))} 绑定到某一层；
 * BOSS 战开始时会自动循环播放，战斗结束自动停止（替换原版音乐）。</p>
 *
 * <p>接口线程安全，可以在 mod 构造期调用（推荐）或任何时刻调用。</p>
 */
public final class StcApi {

    /** 卡牌 id → 贴图位置（注册表；没登记的走上面的"约定路径"）。 */
    private static final Map<ResourceLocation, ResourceLocation> CARD_TEXTURES = new LinkedHashMap<>();
    /** 层号 → BOSS 音乐的声音事件 id。 */
    private static final Map<Integer, ResourceLocation> BOSS_MUSIC = new LinkedHashMap<>();

    private StcApi() {
    }

    // ------------------------------------------------------------------ 卡牌贴图

    /**
     * 为某张卡指定插画贴图。
     *
     * @param cardId  卡牌 id（例如 {@code stc:strike}）
     * @param texture 贴图位置（**不带** {@code textures/} 前缀、不带 {@code .png}），
     *                例如 {@code ResourceLocation.fromNamespaceAndPath("mypack", "card/strike")}
     */
    public static void registerCardTexture(ResourceLocation cardId, ResourceLocation texture) {
        if (cardId == null || texture == null) {
            throw new IllegalArgumentException("cardId / texture 不能为空");
        }
        CARD_TEXTURES.put(cardId, texture);
    }

    /** 是否有人显式登记过这张卡的贴图。 */
    public static boolean hasRegisteredCardTexture(ResourceLocation cardId) {
        return cardId != null && CARD_TEXTURES.containsKey(cardId);
    }

    /** 显式登记过的贴图；没有则返回空。 */
    public static Optional<ResourceLocation> cardTexture(ResourceLocation cardId) {
        return Optional.ofNullable(cardId == null ? null : CARD_TEXTURES.get(cardId));
    }

    /**
     * 实际应该尝试加载的卡面贴图：优先用登记的，否则用约定路径
     * {@code <卡牌命名空间>:textures/card/<卡牌路径>.png}。
     */
    public static ResourceLocation textureForCard(ResourceLocation cardId) {
        ResourceLocation registered = CARD_TEXTURES.get(cardId);
        if (registered != null) {
            return registered;
        }
        return cardId.withPath(path -> "textures/card/" + path + ".png");
    }

    /** 已登记的卡牌贴图数量（自检 / 调试用）。 */
    public static int cardTextureCount() {
        return CARD_TEXTURES.size();
    }

    /** 清空卡牌贴图登记（自检用；游戏内不要调用）。 */
    public static void clearCardTextures() {
        CARD_TEXTURES.clear();
    }

    // ------------------------------------------------------------------ BOSS 音乐

    /**
     * 给某一层的 BOSS 战绑定音乐。
     *
     * @param floor          层号（1..3）
     * @param soundEventId   声音事件 id（必须已经注册，例如整合包 sounds.json 里定义的
     *                       {@code mypack:boss_floor1}）
     */
    public static void registerBossMusic(int floor, ResourceLocation soundEventId) {
        if (floor < 1) {
            throw new IllegalArgumentException("层号从 1 开始：" + floor);
        }
        if (soundEventId == null) {
            throw new IllegalArgumentException("soundEventId 不能为空");
        }
        BOSS_MUSIC.put(floor, soundEventId);
    }

    /** 这一层绑定的 BOSS 音乐；没有则返回空（此时不动原版音乐）。 */
    public static Optional<ResourceLocation> bossMusic(int floor) {
        return Optional.ofNullable(BOSS_MUSIC.get(floor));
    }

    /** 是否给这一层配了 BOSS 音乐。 */
    public static boolean hasBossMusic(int floor) {
        return BOSS_MUSIC.containsKey(floor);
    }

    /**
     * **约定事件名**：第 {@code floor} 层的 BOSS 音乐事件 = {@code stc:boss_music_floor_<层号>}。
     *
     * <p>模组自带的 {@code assets/stc/sounds.json} 已经按这个约定定义了 1–3 层的事件，
     * 所以**只要把 ogg 放进对应目录，什么都不用写就会响**：
     * {@code src/main/resources/assets/stc/sounds/boss/floor_<层>/floor_<层>.ogg}。</p>
     *
     * @throws IllegalArgumentException 层号小于 1
     */
    public static ResourceLocation bossMusicConvention(int floor) {
        if (floor < 1) {
            throw new IllegalArgumentException("层号从 1 开始：" + floor);
        }
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "boss_music_floor_" + floor);
    }

    /**
     * 这一层**实际应该播放**的音乐事件：登记过的优先，其次用 {@link #bossMusicConvention(int)}。
     *
     * <p>客户端 {@code BossMusic} 用的就是这个方法 —— 所以"放文件"和"写代码"两种做法都支持。</p>
     */
    public static ResourceLocation bossMusicFor(int floor) {
        return bossMusic(floor).orElseGet(() -> bossMusicConvention(floor));
    }

    /** 已登记的层数（自检用）。 */
    public static int bossMusicCount() {
        return BOSS_MUSIC.size();
    }

    /** 清空 BOSS 音乐登记（自检用）。 */
    public static void clearBossMusic() {
        BOSS_MUSIC.clear();
    }

    // ------------------------------------------------------------------ 收纳包界面皮肤（v0.4.6.5）

    /** 收纳包书页背景（{@code null} = 用程序化羊皮纸）。 */
    private static ResourceLocation binderBackground;
    /** 收纳包按钮三态。 */
    private static ResourceLocation binderButtonNormal;
    private static ResourceLocation binderButtonHovered;
    private static ResourceLocation binderButtonDisabled;
    /** 角色 id → 头像贴图；以及通用头像。 */
    private static final Map<ResourceLocation, ResourceLocation> BINDER_AVATARS = new LinkedHashMap<>();
    private static ResourceLocation binderDefaultAvatar;

    /**
     * 指定**卡牌收纳包的书页背景**贴图。
     *
     * <p>整张贴图会被拉伸到面板矩形（默认 268×300），所以建议按这个尺寸画；
     * 传 {@code null} 恢复程序化羊皮纸。约定路径见 {@code assets/stc/textures/gui/binder/background.png}。</p>
     */
    public static void registerBinderBackground(ResourceLocation texture) {
        binderBackground = texture;
    }

    /**
     * 指定**收纳包按钮**的三态贴图（九宫格绘制，角落不缩放）。
     *
     * <p>三个都可以传 {@code null} 表示"该状态用约定路径 / 退回正常态"；
     * 贴图建议 ≥12×12、四角各留 3 像素装饰，任意尺寸都行（绘制时读原图尺寸）。</p>
     */
    public static void registerBinderButtons(ResourceLocation normal, ResourceLocation hovered,
                                             ResourceLocation disabled) {
        binderButtonNormal = normal;
        binderButtonHovered = hovered;
        binderButtonDisabled = disabled;
    }

    /** 指定某个**角色的头像**贴图（等比压进抬头那个方框）。 */
    public static void registerBinderAvatar(ResourceLocation characterId, ResourceLocation texture) {
        if (characterId == null) {
            throw new IllegalArgumentException("characterId 不能为空");
        }
        if (texture == null) {
            BINDER_AVATARS.remove(characterId);
        } else {
            BINDER_AVATARS.put(characterId, texture);
        }
    }

    /** 指定**通用头像**（角色专属贴图 / 约定路径都找不到时用它）。 */
    public static void registerBinderDefaultAvatar(ResourceLocation texture) {
        binderDefaultAvatar = texture;
    }

    public static java.util.Optional<ResourceLocation> binderBackground() {
        return java.util.Optional.ofNullable(binderBackground);
    }

    public static java.util.Optional<ResourceLocation> binderButtonNormal() {
        return java.util.Optional.ofNullable(binderButtonNormal);
    }

    public static java.util.Optional<ResourceLocation> binderButtonHovered() {
        return java.util.Optional.ofNullable(binderButtonHovered);
    }

    public static java.util.Optional<ResourceLocation> binderButtonDisabled() {
        return java.util.Optional.ofNullable(binderButtonDisabled);
    }

    public static java.util.Optional<ResourceLocation> binderAvatar(ResourceLocation characterId) {
        return java.util.Optional.ofNullable(BINDER_AVATARS.get(characterId));
    }

    public static java.util.Optional<ResourceLocation> binderDefaultAvatar() {
        return java.util.Optional.ofNullable(binderDefaultAvatar);
    }

    /** 已登记的角色头像数量（自检用）。 */
    public static int binderAvatarCount() {
        return BINDER_AVATARS.size();
    }

    /** 清空全部收纳包皮肤登记（自检用）。 */
    public static void clearBinderSkin() {
        binderBackground = null;
        binderButtonNormal = null;
        binderButtonHovered = null;
        binderButtonDisabled = null;
        binderDefaultAvatar = null;
        BINDER_AVATARS.clear();
    }

    // ------------------------------------------------------------------ 通用

    /** 便捷方法：{@code StcApi.id("mypack:card/strike")}。 */
    public static ResourceLocation id(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new IllegalArgumentException("不是合法的 ResourceLocation：" + value);
        }
        return parsed;
    }

    /** 本体的命名空间（{@code stc}）。 */
    public static String modId() {
        return SlayTheCraft.MODID;
    }
}
