package com.slaythecraft.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.PotionForm;
import com.slaythecraft.combat.core.PotionLibrary;

import net.minecraft.resources.ResourceLocation;

/**
 * 小图标自适应缩放 + 不祥之瓶图标修订的自检（v0.4.6.7）。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"小图标如果出现大小错误的问题，自动将图像调整到合适大小"；</li>
 *   <li>"不祥之瓶（药水）的图标修订：不祥之瓶，而不是滞留药水"；</li>
 *   <li>"当手牌达到上限时，在药水栏上方提示『我的手牌太多了...』"。</li>
 * </ul>
 *
 * <p>这里盯四件事：</p>
 * <ol>
 *   <li><b>等比缩放</b>（{@link IconFit#fit}）：16×16 进 8×8 还是正方；32×16 进 16×16 变成 16×8；</li>
 *   <li><b>居中且绝不越界</b>：多出来的边距两边平分，任何组合都不会画出方框之外；</li>
 *   <li><b>退化输入</b>：0 尺寸的方框 / 贴图不会算出负尺寸，{@code draw} 也不会崩；</li>
 *   <li><b>接线没断</b>：不祥之瓶叫"不祥之瓶"、图标解析到 {@code minecraft:textures/item/ominous_bottle.png}
 *       （{@link PotionForm#ITEM}），状态图标走 {@link IconFit}，HUD 真的引用了手牌满的翻译键。</li>
 * </ol>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.client.IconFitCheck
 * </pre>
 */
public final class IconFitCheck {

    /** 手牌满提示用的翻译键（HUD 必须引用它，否则界面上没有这句话）。 */
    private static final String HAND_FULL_KEY = "stc.battle.hand_full";

    private IconFitCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        // ---------------------------------------------------------- 1. 等比缩放
        IconFit.Fit square = IconFit.fit(0, 0, 8, 8, 16, 16);
        check(lines, counters, "16×16 进 8×8 仍然是正方（8×8）",
                square.w() == 8 && square.h() == 8, "8×8", square.w() + "×" + square.h());

        IconFit.Fit wide = IconFit.fit(0, 0, 16, 16, 32, 16);
        check(lines, counters, "32×16 进 16×16 变成 16×8（保持 2:1）",
                wide.w() == 16 && wide.h() == 8, "16×8", wide.w() + "×" + wide.h());

        IconFit.Fit tall = IconFit.fit(0, 0, 16, 16, 8, 32);
        check(lines, counters, "8×32 进 16×16 变成 4×16（保持 1:4）",
                tall.w() == 4 && tall.h() == 16, "4×16", tall.w() + "×" + tall.h());

        IconFit.Fit same = IconFit.fit(3, 5, 16, 16, 16, 16);
        check(lines, counters, "16×16 进 16×16 原尺寸（正常贴图结果不变）",
                same.w() == 16 && same.h() == 16, "16×16", same.w() + "×" + same.h());

        IconFit.Fit statusFit = IconFit.fit(10, 20, 8, 8, 18, 18);
        check(lines, counters, "18×18 的原版状态贴图进 8×8 正好铺满（视觉与旧版一致）",
                statusFit.w() == 8 && statusFit.h() == 8, "8×8", statusFit.w() + "×" + statusFit.h());

        // 长宽比：缩放后的比值与原图一致（允许 ±0.02 的取整误差）
        double ratioBox = (double) wide.w() / wide.h();
        double ratioTex = 32.0D / 16.0D;
        check(lines, counters, "等比缩放：宽高比与原图一致（2:1）",
                Math.abs(ratioBox - ratioTex) < 0.02D, ratioTex, ratioBox);

        // ---------------------------------------------------------- 2. 居中 + 绝不越界
        IconFit.Fit centred = IconFit.fit(0, 0, 16, 16, 16, 8);
        check(lines, counters, "16×8 放进 16×16 的方框：垂直居中于 y+4",
                centred.x() == 0 && centred.y() == 4 && centred.w() == 16 && centred.h() == 8,
                "0,4,16,8", centred.x() + "," + centred.y() + "," + centred.w() + "," + centred.h());

        IconFit.Fit centredX = IconFit.fit(100, 50, 16, 16, 8, 16);
        check(lines, counters, "8×16 放进 16×16 的方框：水平居中于 x+4、并带上原点偏移",
                centredX.x() == 104 && centredX.y() == 50 && centredX.w() == 8 && centredX.h() == 16,
                "104,50,8,16",
                centredX.x() + "," + centredX.y() + "," + centredX.w() + "," + centredX.h());

        int[][] cases = {
                {8, 8, 16, 16},
                {8, 8, 32, 32},
                {8, 8, 32, 16},
                {8, 8, 1, 64},
                {8, 8, 64, 1},
                {16, 16, 7, 3},
                {20, 20, 256, 256},
                {3, 3, 1, 1},
                {64, 4, 5, 97},
                {1, 1, 16, 16},
        };
        boolean inside = true;
        boolean nonNegative = true;
        boolean anyBad = false;
        for (int[] c : cases) {
            IconFit.Fit fit = IconFit.fit(7, 9, c[0], c[1], c[2], c[3]);
            inside &= fit.x() >= 7 && fit.y() >= 9 && fit.x() + fit.w() <= 7 + c[0]
                    && fit.y() + fit.h() <= 9 + c[1];
            nonNegative &= fit.w() >= 0 && fit.h() >= 0;
            if (fit.w() > c[0] || fit.h() > c[1]) {
                anyBad = true;
            }
        }
        check(lines, counters, "任何组合都不画出方框之外", inside, true, false);
        check(lines, counters, "任何组合都不会放大到超过方框", !anyBad, false, true);
        check(lines, counters, "任何组合都不会出现负尺寸", nonNegative, true, false);

        // 小图（4×4）进 8×8：放大到 8×8 但不越过方框
        IconFit.Fit up = IconFit.fit(0, 0, 8, 8, 4, 4);
        check(lines, counters, "4×4 的小图会被放大到 8×8（但不越过方框）",
                up.w() == 8 && up.h() == 8, "8×8", up.w() + "×" + up.h());

        // ---------------------------------------------------------- 3. 退化输入
        IconFit.Fit zeroBox = IconFit.fit(5, 6, 0, 0, 16, 16);
        check(lines, counters, "0 尺寸方框：退化成 0 尺寸而不是负数",
                zeroBox.w() == 0 && zeroBox.h() == 0, "0×0", zeroBox.w() + "×" + zeroBox.h());
        IconFit.Fit zeroBoxW = IconFit.fit(5, 6, 0, 8, 32, 16);
        check(lines, counters, "只有宽为 0 的方框同样不画（0×0）",
                zeroBoxW.w() == 0 && zeroBoxW.h() == 0, "0×0", zeroBoxW.w() + "×" + zeroBoxW.h());
        IconFit.Fit zeroTex = IconFit.fit(2, 3, 8, 8, 0, 0);
        check(lines, counters, "0 尺寸贴图：按整个方框处理，不出现负数",
                zeroTex.w() == 8 && zeroTex.h() == 8 && zeroTex.x() == 2 && zeroTex.y() == 3,
                "2,3,8,8", zeroTex.x() + "," + zeroTex.y() + "," + zeroTex.w() + "×" + zeroTex.h());
        IconFit.Fit halfZeroTex = IconFit.fit(0, 0, 8, 8, 0, 16);
        check(lines, counters, "贴图只有一边为 0：退化成铺满方框，不出现负数",
                halfZeroTex.w() == 8 && halfZeroTex.h() == 8, "8×8", halfZeroTex.w() + "×" + halfZeroTex.h());

        // draw 在"没有图形上下文 / 没有贴图 / 0 尺寸方框"时也不能崩（这段不需要真客户端）
        boolean drawSafe = true;
        String drawError = "";
        try {
            IconFit.Fit nullGraphics = IconFit.draw(null, id("textures/item/ominous_bottle.png"), 0, 0, 8, 8);
            drawSafe &= nullGraphics.w() == 0 && nullGraphics.h() == 0;
            IconFit.Fit nullTexture = IconFit.draw(null, null, 0, 0, 8, 8);
            drawSafe &= nullTexture.w() == 0 && nullTexture.h() == 0;
            IconFit.Fit emptyBox = IconFit.draw(null, id("textures/item/ominous_bottle.png"), 4, 4, 0, 8);
            drawSafe &= emptyBox.w() == 0 && emptyBox.h() == 0;
        } catch (Throwable t) {
            drawSafe = false;
            drawError = t.getClass().getSimpleName();
        }
        check(lines, counters, "draw 遇到空参数 / 0 尺寸方框不崩、也不画东西", drawSafe, true,
                drawError.isEmpty() ? false : drawError);

        // ---------------------------------------------------------- 4. 不祥之瓶的修订
        check(lines, counters, "药水表里 ominous_bottle 的名字 = 不祥之瓶",
                "不祥之瓶".equals(PotionLibrary.byId("ominous_bottle").name()), "不祥之瓶",
                PotionLibrary.byId("ominous_bottle").name());
        check(lines, counters, "不祥之瓶是正面药水（数值没被改动）",
                PotionLibrary.OMINOUS.kind() == com.slaythecraft.combat.core.PotionKind.BENEFICIAL
                        && PotionLibrary.OMINOUS.selfOnly(),
                "正面 / 只能对自己", PotionLibrary.OMINOUS.kind() + " / " + PotionLibrary.OMINOUS.selfOnly());
        check(lines, counters, "不祥之瓶改用物品贴图（不再是滞留药水贴图）",
                PotionLibrary.OMINOUS.form() == PotionForm.ITEM, PotionForm.ITEM, PotionLibrary.OMINOUS.form());
        check(lines, counters, "不祥之瓶解析到 minecraft:textures/item/ominous_bottle.png",
                "textures/item/ominous_bottle.png".equals(PotionLibrary.OMINOUS.itemTexturePath()),
                "textures/item/ominous_bottle.png", PotionLibrary.OMINOUS.itemTexturePath());
        check(lines, counters, "不祥之瓶的物品是 minecraft:ominous_bottle",
                "minecraft:ominous_bottle".equals(PotionLibrary.OMINOUS.itemId()), "minecraft:ominous_bottle",
                PotionLibrary.OMINOUS.itemId());
        // 其它药水的图标不许被顺手改掉
        // v0.5.0.4：奶桶从"滞留贴图"改成物品贴图 minecraft:milk_bucket，
        // 所以这里不再期望 LINGERING——它现在和炖菜们一样是 ITEM。
        check(lines, counters, "其它药水的图标形态没被改动（只动了奶桶）",
                PotionLibrary.MILK.form() == PotionForm.ITEM
                        && PotionLibrary.LUCK.form() == PotionForm.LINGERING
                        && PotionLibrary.HEAL.form() == PotionForm.NORMAL
                        && PotionLibrary.MUSHROOM_STEW.form() == PotionForm.ITEM
                        && PotionLibrary.SUSPICIOUS_STEW.form() == PotionForm.ITEM,
                "物品/滞留/常规/物品/物品",
                PotionLibrary.MILK.form() + "/" + PotionLibrary.LUCK.form() + "/" + PotionLibrary.HEAL.form()
                        + "/" + PotionLibrary.MUSHROOM_STEW.form() + "/" + PotionLibrary.SUSPICIOUS_STEW.form());
        // v0.5.0.4 需求："奶桶（药水物品）的贴图错误，应该为 minecraft:milk_bucket。"
        check(lines, counters, "奶桶的贴图是 minecraft:milk_bucket",
                "minecraft:milk_bucket".equals(PotionLibrary.MILK.itemId())
                        && "textures/item/milk_bucket.png".equals(PotionLibrary.MILK.itemTexturePath()),
                "minecraft:milk_bucket / textures/item/milk_bucket.png",
                PotionLibrary.MILK.itemId() + " / " + PotionLibrary.MILK.itemTexturePath());
        check(lines, counters, "非物品贴图的药水没有物品贴图路径",
                PotionLibrary.LUCK.itemTexturePath().isEmpty() && PotionLibrary.HEAL.itemTexturePath().isEmpty(),
                "空串", PotionLibrary.LUCK.itemTexturePath() + "/" + PotionLibrary.HEAL.itemTexturePath());

        // ---------------------------------------------------------- 5. 接线没断（源码级）
        String status = read("client/StatusIcons.java", lines, counters);
        if (status != null) {
            check(lines, counters, "状态图标改走 IconFit（真实尺寸等比缩放）",
                    status.contains("IconFit.draw("), true, false);
            check(lines, counters, "状态图标不再用写死的 8×8 blit",
                    !status.contains("blit(texture, x, y, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE)"), false, true);
            check(lines, counters, "状态图标的宽度仍按 8×8 排版（布局不变）",
                    status.contains("SIZE + GAP"), true, false);
        }
        String hud = read("client/PotionHudLayer.java", lines, counters);
        if (hud != null) {
            check(lines, counters, "PotionHudLayer 引用手牌满的翻译键 " + HAND_FULL_KEY,
                    hud.contains(HAND_FULL_KEY), true, false);
            check(lines, counters, "手牌满提示是 Component.translatable（不写死中文）",
                    hud.contains("Component.translatable(" + "\"" + HAND_FULL_KEY + "\")"), true, false);
            check(lines, counters, "手牌满提示取用的是快照里的 handFull",
                    hud.contains("ClientBattleState.view().handFull()"), true, false);
            check(lines, counters, "手牌满提示画在药水栏上方（用 barY - HINT_GAP）",
                    hud.contains("barY - HINT_GAP"), true, false);
        }
        String view = read("network/BattleView.java", lines, counters);
        if (view != null) {
            check(lines, counters, "BattleView 暴露了 handFull 字段",
                    view.contains("boolean handFull"), true, false);
            check(lines, counters, "handFull 进了快照编解码（hand_full）",
                    view.contains("tag.putBoolean(\"hand_full\", handFull)")
                            && view.contains("tag.getBoolean(\"hand_full\")"), true, false);
        }
        String manager = read("combat/mc/CombatManager.java", lines, counters);
        if (manager != null) {
            check(lines, counters, "服务端按本场战斗规则算 handFull",
                    manager.contains("context.rules().maxHandSize()") && manager.contains("handFull"), true, false);
        }
        String potionLibrary = read("combat/core/PotionLibrary.java", lines, counters);
        if (potionLibrary != null) {
            check(lines, counters, "PotionLibrary 里已经没有错别字『不详』",
                    !potionLibrary.contains("不详"), false, true);
            check(lines, counters, "PotionLibrary 里不祥之瓶是 minecraft:ominous_bottle",
                    potionLibrary.contains("\"minecraft:ominous_bottle\""), true, false);
        }
        String iconFit = read("client/IconFit.java", lines, counters);
        if (iconFit != null) {
            check(lines, counters, "IconFit 暴露纯数学的 fit(...)（可脱开 Minecraft 自检）",
                    iconFit.contains("public static Fit fit(int boxX, int boxY, int boxW, int boxH, int texW"),
                    true, false);
            check(lines, counters, "IconFit 用 ClientAssets.sizeOf 查真实贴图尺寸",
                    iconFit.contains("ClientAssets.sizeOf("), true, false);
        }

        // ---------------------------------------------------------- v0.5.2.5 遗物图标 + 荆棘图标
        // 用户点名的十件遗物各用一个原版图标；八件新遗物也一并安排好了。
        String[] mapped = {"bag_of_preparation", "oddly_smooth_stone", "startled_piglet", "cracked_core",
                "crossbow", "raid_horn", "dead_coral", "heart_of_the_sea", "mogu_fillet", "honey_seasoning",
                "dirty_glass", "worn_flint", "trader_llama_shawl", "cartography_table",
                "unusual_shulker_box", "dragon_egg", "soul_campfire", "glow_ink_sac",
                // v0.5.5.1（用户）："为遗物图鉴的遗物补全小图标显示" —— 剩下 6 件也安排上
                "vajra", "anchor", "burning_blood", "ring_of_the_snake", "potion_belt", "small_vial"};
        List<String> unmapped = new ArrayList<>();
        for (String id : mapped) {
            if (RelicIcons.itemIdFor(id) == null) {
                unmapped.add(id);
            }
        }
        check(lines, counters, "用户点名的 10 件 + 8 件新遗物 + v0.5.5.1 补的 6 件都安排了原版图标",
                unmapped.isEmpty(), "0 件缺失", unmapped);
        check(lines, counters, "24 件遗物**全部**有图标（图鉴不再出现空图标格）",
                com.slaythecraft.combat.core.RelicLibrary.all().stream()
                        .allMatch(relic -> RelicIcons.itemIdFor(relic.id()) != null),
                "24 件都有", com.slaythecraft.combat.core.RelicLibrary.all().size());
        check(lines, counters, "未知 / 空 id 仍然返回 null（不会崩）",
                RelicIcons.itemIdFor("no_such_relic") == null && RelicIcons.itemIdFor(null) == null, true, false);
        String relicIcons = read("client/RelicIcons.java", lines, counters);
        if (relicIcons != null) {
            check(lines, counters, "遗物图标优先级：自定义 PNG → 原版图标 → 符文",
                    relicIcons.indexOf("if (textured(") < relicIcons.indexOf("itemFor(relic.id())")
                            && relicIcons.indexOf("itemFor(relic.id())") < relicIcons.indexOf("RUNE_COLORS["),
                    "自定义 → 原版 → 符文", "顺序不对");
            check(lines, counters, "原版图标走 renderItem（方块物品才画得对），且解析延后到游戏内",
                    relicIcons.contains("ItemIcons.draw(graphics, item, x, y, size)"), true, false);
            // v0.5.5.1：图鉴列表原先只认 PNG，所以"用原版物品贴图"的遗物在图鉴里看不到图标
            check(lines, counters, "【available】认得自定义 PNG 与原版图标（图鉴用它决定名字的起始 x）",
                    relicIcons.contains("public static boolean available(Relic relic)")
                            && relicIcons.contains("ClientAssets.present(path(relic)) || itemIdFor(relic.id()) != null"),
                    "出现", "缺失");
        }
        String statusIcons = read("client/StatusIcons.java", lines, counters);
        if (statusIcons != null) {
            check(lines, counters, "新状态【荆棘】已登记（v0.5.5.1：改用 缠怨藤 物品贴图）",
                    statusIcons.contains("item(\"荆棘\", \"minecraft:twisting_vines\"")
                            && statusIcons.contains("textures/item/"), true, false);
            check(lines, counters, "【集中】改用 末地水晶 物品贴图（v0.5.5.1 美术调整）",
                    statusIcons.contains("item(\"集中\", \"minecraft:end_crystal\""), true, false);
            check(lines, counters, "【荆棘】有风味文案",
                    statusIcons.contains("flavor(\"荆棘\""), true, false);
        }
        String preview = read("combat/core/CardPreview.java", lines, counters);
        if (preview != null) {
            check(lines, counters, "【荆棘】会出现在状态列表里",
                    preview.contains("addStatus(lines, \"荆棘\", combatant.thorns())"), true, false);
        }

        // ---------------------------------------------------------- v0.5.2.5 音效音量 ×1.25
        String battleFx = read("client/ClientBattleFx.java", lines, counters);
        if (battleFx != null) {
            check(lines, counters, "音效音量系数 = 1.25（需求「提高四分之一」）",
                    battleFx.contains("SFX_VOLUME_SCALE = 1.25F"), true, false);
            check(lines, counters, "所有战斗音效在 playAt 里乘上系数",
                    battleFx.contains("volume * SFX_VOLUME_SCALE"), true, false);
        }
        String uiFeedback = read("client/UiFeedback.java", lines, counters);
        if (uiFeedback != null) {
            check(lines, counters, "按钮音效共用同一个音量系数",
                    uiFeedback.contains("ClientBattleFx.SFX_VOLUME_SCALE"), true, false);
        }

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("stc", path);
    }

    private static String read(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
        if (!Files.isRegularFile(path)) {
            check(lines, counters, "源码存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, "源码可读：" + path, false, "可读", e.getClass().getSimpleName());
            return null;
        }
    }

    private static void check(List<String> lines, int[] counters, String label, boolean condition,
                             Object expected, Object actual) {
        if (condition) {
            counters[0]++;
            lines.add("PASS  " + label);
        } else {
            counters[1]++;
            lines.add("FAIL  " + label + "（期望 " + expected + "，实际 " + actual + "）");
        }
    }
}
