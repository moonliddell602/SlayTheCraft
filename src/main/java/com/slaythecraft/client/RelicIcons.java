package com.slaythecraft.client;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.combat.core.Relic;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 遗物小图标的**统一画法**（v0.5.1.3）。
 *
 * <p>遗物直到 v0.5.2.5 才有图标：**优先用作者自定义的 PNG**（约定路径
 * {@code stc:textures/relic/<遗物 id>.png}，丢进去就生效、不用改代码），
 * 其次用 v0.5.2.5 指定的**原版物品图标**（见 {@link #itemFor}），
 * 最后兜底画一个**程序化符文**（底色由 id 决定的小方块 + 遗物名首字）。</p>
 *
 * <p>纯客户端绘制，没有任何副作用。</p>
 */
public final class RelicIcons {

    /** 程序化兜底的底色（按 id 稳定选一个，不会每次重画都换色）。 */
    private static final int[] RUNE_COLORS = {
            0xFF7A4AB5, 0xFF4A7FB5, 0xFFB5533C, 0xFF3F8F5A, 0xFFB58A3C, 0xFF8A4A6A,
    };
    private static final int RUNE_EDGE = 0xFF20202A;

    private RelicIcons() {
    }

    /** 遗物图标的约定路径：{@code stc:textures/relic/<遗物 id>.png}。 */
    public static ResourceLocation path(Relic relic) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID,
                "textures/relic/" + relic.id() + ".png");
    }

    /**
     * 这件遗物**用哪个原版物品的图标**（v0.5.2.5 美术需求）—— 返回的是**物品 id 字符串**。
     *
     * <p>需求给的是"用某某图标"（末影箱 / 平滑石头 / 猪刷怪蛋 / 蓝色玻璃 / 十字弩 / 山羊角 /
     * 失活的珊瑚 / 海洋之心 / 牛排 / 蜜脾 …）。</p>
     *
     * <p>⚠️ 这里**刻意返回字符串而不是 {@code Items.X}**：自检套件跑在没有
     * {@code Bootstrap.bootStrap()} 的环境里，一碰 {@code Items} 就会
     * {@code ExceptionInInitializerError}（整个套件崩掉）。所以表是纯数据，
     * 真正解析成 {@link net.minecraft.world.item.Item} 放在 {@link #itemFor} 里，只在游戏内调用。</p>
     *
     * <p><b>优先级</b>：自定义 PNG（{@code textures/relic/<id>.png}）＞ 这里的原版图标 ＞ 程序化符文。
     * 也就是说作者往资源包里丢一张同名 PNG，就能覆盖下表的安排。</p>
     *
     * @return 原版物品 id（如 {@code minecraft:ender_chest}）；{@code null} = 没安排
     */
    public static String itemIdFor(String relicId) {
        if (relicId == null) {
            return null;
        }
        return switch (relicId) {
            // ---- v0.5.2.5 用户指定的十件
            case "bag_of_preparation" -> "minecraft:ender_chest";
            case "oddly_smooth_stone" -> "minecraft:smooth_stone";
            case "startled_piglet" -> "minecraft:pig_spawn_egg";
            case "cracked_core" -> "minecraft:blue_stained_glass";
            case "crossbow" -> "minecraft:crossbow";
            case "raid_horn" -> "minecraft:goat_horn";
            case "dead_coral" -> "minecraft:dead_tube_coral";
            case "heart_of_the_sea" -> "minecraft:heart_of_the_sea";
            case "mogu_fillet" -> "minecraft:cooked_beef";
            case "honey_seasoning" -> "minecraft:honeycomb";
            // ---- v0.5.2.5 八件新遗物
            case "dirty_glass" -> "minecraft:tinted_glass";
            case "worn_flint" -> "minecraft:flint_and_steel";
            case "trader_llama_shawl" -> "minecraft:blue_carpet";
            case "cartography_table" -> "minecraft:cartography_table";
            case "unusual_shulker_box" -> "minecraft:white_shulker_box";
            case "dragon_egg" -> "minecraft:dragon_egg";
            case "soul_campfire" -> "minecraft:soul_campfire";
            case "glow_ink_sac" -> "minecraft:glow_ink_sac";
            // ---- v0.5.5.1（用户）："为遗物图鉴的遗物补全小图标显示" ——
            // 把剩下 6 件（原先只有程序化符文）也安排上原版物品贴图。
            case "vajra" -> "minecraft:lightning_rod";              // 金刚杵 = 雷杵
            case "anchor" -> "minecraft:respawn_anchor";            // 锚
            case "burning_blood" -> "minecraft:magma_cream";        // 燃烧之血
            case "ring_of_the_snake" -> "minecraft:gold_ingot";      // 蛇之戒指
            case "potion_belt" -> "minecraft:brewing_stand";        // 药水腰带
            case "small_vial" -> "minecraft:potion";                // 小血瓶
            default -> null;
        };
    }

    /** 解析成真正的 {@code Item}（**只在游戏内调用**：需要注册表已经加载）。 */
    private static net.minecraft.world.item.Item itemFor(String relicId) {
        String id = itemIdFor(relicId);
        ResourceLocation loc = id == null ? null : ResourceLocation.tryParse(id);
        if (loc == null) {
            return null;
        }
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
        return item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    /**
     * 只画贴图（没有贴图就什么都不画）。
     *
     * @return 有没有真的画出图标（调用方用它决定后续文字从哪个 x 开始）
     */
    public static boolean textured(GuiGraphics graphics, Relic relic, int x, int y, int size) {
        ResourceLocation texture = path(relic);
        if (!ClientAssets.present(texture)) {
            return false;
        }
        // 交给 IconFit 等比缩放（作者放多大的图都不会被压扁）
        return !IconFit.draw(graphics, texture, x, y, size, size).empty();
    }

    /**
     * 这件遗物**有没有可用的图标**（自定义 PNG 或已安排的原版物品图标）。
     *
     * <p>v0.5.5.1：遗物图鉴的列表格子用它决定"名字从哪个 x 开始" ——
     * 以前那里只认 PNG，所以用原版物品贴图的那十几件在图鉴里既没有图标、
     * 名字又没左移，看起来像"漏了图标"。</p>
     */
    public static boolean available(Relic relic) {
        return relic != null && (ClientAssets.present(path(relic)) || itemIdFor(relic.id()) != null);
    }

    /**
     * 画一个遗物图标：**自定义贴图 → 原版物品图标 → 程序化符文**（保证一定看得见东西）。
     *
     * @param name 兜底符文用哪个字（一般传遗物名）
     */
    public static void draw(GuiGraphics graphics, Font font, Relic relic, String name, int x, int y, int size) {
        if (textured(graphics, relic, x, y, size)) {
            return;
        }
        net.minecraft.world.item.Item item = itemFor(relic.id());
        if (item != null) {
            ItemIcons.draw(graphics, item, x, y, size);
            return;
        }
        int color = RUNE_COLORS[Math.floorMod(relic.id().hashCode(), RUNE_COLORS.length)];
        graphics.fill(x, y, x + size, y + size, color);
        graphics.fill(x, y, x + size, y + 1, RUNE_EDGE);
        graphics.fill(x, y + size - 1, x + size, y + size, RUNE_EDGE);
        graphics.fill(x, y, x + 1, y + size, RUNE_EDGE);
        graphics.fill(x + size - 1, y, x + size, y + size, RUNE_EDGE);
        String label = name == null || name.isEmpty() ? "?" : name.substring(0, 1);
        int textX = x + Math.max(0, (size - font.width(label)) / 2);
        int textY = y + Math.max(0, (size - 8) / 2);
        TextStyle.draw(graphics, font, label, textX, textY, 0xFFFFFFFF);
    }
}
