import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardRarity;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出【叛逃掠夺者】的现有卡牌内容（v0.5.1.0，一次性工具）。
 *
 * <p>从 {@link CardLibrary} 直接读，保证导出的内容与实际游戏一致（不是凭记忆写的文档）。</p>
 */
public class ExportPillagerCards {

    private static final String CHARACTER = CardLibrary.PILLAGER;
    private static final String RELIC_ID = "crossbow";

    public static void main(String[] args) throws IOException {
        List<Card> base = new ArrayList<>();
        for (Card card : CardLibrary.all()) {
            if (CHARACTER.equals(card.character())) {
                base.add(card);
            }
        }
        // 按品质 → 卡名排序（与图鉴一致）
        base.sort((a, b) -> {
            int byRarity = a.rarity().ordinal() - b.rarity().ordinal();
            return byRarity != 0 ? byRarity : a.name().compareTo(b.name());
        });

        StringBuilder md = new StringBuilder();
        md.append("# 叛逃掠夺者（Pillager）卡表\n\n");
        md.append("> 本文件由 `CardLibrary` **自动导出**，不是手写的 —— 所以它一定与当前代码一致。\n");
        md.append("> 重新导出：见文末「重新导出」一节。\n\n");

        // ---- 角色信息
        md.append("## 角色\n\n");
        md.append("| 项 | 值 |\n| --- | --- |\n");
        md.append("| 角色 id | `").append(CHARACTER).append("` |\n");
        md.append("| 初始遗物 | 【").append(relicName(RELIC_ID)).append("】`").append(RELIC_ID).append("` |\n");
        md.append("| 专属卡数 | **").append(base.size()).append("** 张 |\n");
        md.append("| 无色牌（不在本角色池里） | ")
                .append(CardLibrary.colorless().size()).append(" 张 |\n\n");

        // ---- 汇总表
        md.append("## 汇总\n\n");
        md.append("| 品质 | 张数 |\n| --- | ---: |\n");
        for (CardRarity rarity : CardRarity.values()) {
            long n = base.stream().filter(c -> c.rarity() == rarity).count();
            if (n > 0) {
                md.append("| ").append(rarity.label()).append("（").append(rarity.name()).append("） | ")
                        .append(n).append(" |\n");
            }
        }
        md.append('\n');

        // ---- 逐张明细
        md.append("## 卡牌明细\n\n");
        md.append("费用栏里的 **X** 表示 X 费（引擎里 `cost` 字段是 0，用 `CardLibrary.isXCost(id)` 判定）。\n\n");
        for (CardRarity rarity : CardRarity.values()) {
            List<Card> group = new ArrayList<>();
            for (Card card : base) {
                if (card.rarity() == rarity) {
                    group.add(card);
                }
            }
            if (group.isEmpty()) {
                continue;
            }
            md.append("### ").append(rarity.label()).append("（").append(rarity.name()).append("）\n\n");
            for (Card card : group) {
                appendCard(md, card);
            }
        }

        // ---- 可复制的 Java 模板
        md.append("## 新增卡牌的代码模板\n\n");
        md.append("按项目约定：**类级字段** + **独立的 `static { }` 写强化版**（不要塞进那个大静态块里），\n");
        md.append("效果体抽成私有静态方法。`register(...)` 进基础池，`upgrade(...)` 进强化池。\n\n");
        md.append("```java\n");
        md.append("// 基础版：叛逃掠夺者专属，普通品质，1 费攻击\n");
        md.append("public static final Card MY_CARD = register(new Card(\n");
        md.append("        \"my_card\", \"我的卡\", CardType.ATTACK, 1, CardTarget.ENEMY, \"造成 8 点伤害。\",\n");
        md.append("        CardRarity.COMMON, PILLAGER,\n");
        md.append("        (ctx, self, target) -> ctx.dealDamage(self, target, 8, \"「我的卡」\")));\n\n");
        md.append("// 强化版：同一个 id、名字带「+」、数值更高 —— 必须与基础版**同批**写上\n");
        md.append("static {\n");
        md.append("    upgrade(new Card(\"my_card\", \"我的卡+\", CardType.ATTACK, 1, CardTarget.ENEMY,\n");
        md.append("            \"造成 11 点伤害。\", CardRarity.COMMON, PILLAGER,\n");
        md.append("            (ctx, self, target) -> ctx.dealDamage(self, target, 11, \"「我的卡」\")));\n");
        md.append("}\n");
        md.append("```\n\n");
        md.append("**加卡时的两条纪律**（项目里踩过坑）：\n\n");
        md.append("1. **先查 id 是否被占用**：id 撞车会**悄悄覆盖别人的强化版**；\n");
        md.append("2. **每张非状态/非诅咒牌都必须有强化版** —— 自检会以「每张非状态/非诅咒牌都有强化版」拦下来。\n\n");

        // ---- 可用的引擎原语（新卡常用）
        md.append("## 写新卡时可用的引擎原语\n\n");
        md.append("| 原语 | 用途 |\n| --- | --- |\n");
        md.append("| `ctx.dealDamage(self, target, n, label)` | 造成伤害（返回未被格挡的实际掉血，可用于「回复未格挡伤害」类卡） |\n");
        md.append("| `ctx.grantPlayerBlock(n, label)` | 获得格挡（会走迅捷/缓慢修正） |\n");
        md.append("| `ctx.grantEscortBlock(self, n, label)` | **全场存活玩家**各加 n 点格挡，并挂上【商队护送】的结算（v0.5.3.1） |\n");
        md.append("| `ctx.drawCards(self, n)` / `ctx.gainEnergy(self, n)` | 抽牌 / 获得能量 |\n");
        md.append("| `ctx.addToDrawPile(self, card, n)` | 往**抽牌堆**加牌（`insertCards` 只进弃牌堆） |\n");
        md.append("| `ctx.putOnTopOfDrawPile(self, card)` | 把一张牌放到抽牌堆**顶** |\n");
        md.append("| `ctx.discardPile(self)` / `hand(self)` | 读弃牌堆 / 手牌 |\n");
        md.append("| `ctx.moveDiscardToDrawPileTop(self, index)` | 弃牌堆选一张置顶（自动选版本） |\n");
        md.append("| `ctx.transformInHand(self, index, replacement)` | 手牌变形成另一张 |\n");
        md.append("| `ctx.exhaustFromHand(self, indices)` | 手牌消耗 |\n");
        md.append("| `ctx.lastX(self)` | 取 X 费卡的 X（配合 `registerXCost`） |\n");
        md.append("| `ctx.stunEnemy(enemy)` | 击晕（当前回合意图改成「晕眩」，不扣血） |\n");
        md.append("| `ctx.dealRandomDamage(self, times, n, label)` | 对随机敌人打 N 次（**每次独立随机选目标**） |\n");
        md.append("| `ctx.announce(text)` | 战报里写一行（不静默） |\n");
        md.append("| `self.addStrength/addDexterity/addFocus/...` | 给自己加状态 |\n");
        md.append("| `target.addVulnerable/addWeak/addBurn/addPoison/addDecay/...` | 给目标加状态 |\n");
        md.append("| `ctx.channelOrb(self, Orb.XXX)` | 生成充能球（只有故障机器人有球位） |\n\n");

        md.append("## 重新导出\n\n");
        md.append("卡表分两段导出（卡池在纯逻辑层、初始牌组在 MC 层），都跑完再手工把第二段合并进来。\n\n");
        md.append("**① 卡牌明细**（`CardLibrary`，裸 JVM 即可）：\n\n");
        md.append("```powershell\n");
        md.append("$jdk = \"D:\\itheima\\environments\\openjdk21\\bin\"\n");
        md.append("$out = \"build\\_export\"\n");
        md.append("$src = Get-ChildItem 'src\\main\\java\\com\\slaythecraft\\combat\\core' -Filter *.java | % FullName\n");
        md.append("& \"$jdk\\javac.exe\" -encoding UTF-8 -proc:none -d $out @src tools\\ExportPillagerCards.java\n");
        md.append("& \"$jdk\\java.exe\" -cp $out ExportPillagerCards   # → docs/pillager-cards.md\n");
        md.append("```\n\n");
        md.append("**② 初始牌组 / 初始遗物**（`CharacterRegistry`，需要 MC 类路径）：\n\n");
        md.append("```powershell\n");
        md.append("$env:JAVA_HOME = \"D:\\itheima\\environments\\openjdk21\"\n");
        md.append("$env:Path = \"$env:JAVA_HOME\\bin;$env:Path\"\n");
        md.append(".\\gradlew.bat compileTestJava --offline\n");
        md.append("$cp = 'build\\classes\\java\\main;build\\classes\\java\\test;build\\resources\\main;' +\n");
        md.append("    ((Get-Content 'build\\neoForm\\neoFormJoined1.21.1-20240808.144430\\steps\\writeMinecraftClasspathData\\classpath.txt' -Encoding UTF8) -join ';')\n");
        md.append("& java -Dstdout.encoding=UTF-8 -cp $cp com.slaythecraft.tools.ExportPillagerDeck\n");
        md.append("```\n\n");
        md.append("> 第二段生成 `docs/pillager-deck-section.md`，把它插到「## 汇总」之前即可。\n");
        md.append("> 两个导出都**从代码里读**，不依赖任何手写清单 —— 所以永远不会与实现脱节。\n");

        Path target = Path.of("docs", "pillager-cards.md");
        Files.createDirectories(target.getParent());
        Files.writeString(target, md.toString(), StandardCharsets.UTF_8);
        System.out.println("已导出 " + base.size() + " 张叛逃掠夺者卡牌到 " + target.toAbsolutePath());
    }

    private static void appendCard(StringBuilder md, Card card) {
        String cost = CardLibrary.isXCost(card.id()) ? "X" : String.valueOf(card.cost());
        md.append("#### ").append(card.name()).append("\n\n");
        md.append("| 项 | 值 |\n| --- | --- |\n");
        md.append("| id | `").append(card.id()).append("` |\n");
        md.append("| 费用 | ").append(cost).append(" |\n");
        md.append("| 类型 | ").append(card.type().label()).append("（").append(card.type().name()).append("） |\n");
        md.append("| 目标 | ").append(targetLabel(card)).append(" |\n");
        md.append("| 描述 | ").append(card.description()).append(" |\n");
        if (!card.keywordLabel().isEmpty()) {
            md.append("| 关键字 | ").append(card.keywordLabel()).append(" |\n");
        }
        if (!card.enchantmentLabel().isEmpty()) {
            md.append("| 起始附魔 | ").append(card.enchantmentLabel()).append(" |\n");
        }
        if (CardLibrary.isXCost(card.id())) {
            md.append("| X 费 | 是（必须用 `CardLibrary.registerXCost(...)` 登记） |\n");
        }
        md.append("| 强化版 | ");
        if (CardLibrary.canUpgrade(card.id())) {
            Card up = CardLibrary.resolve(card.id(), 1);
            if (up != null) {
                String upCost = String.valueOf(up.cost());
                md.append("【").append(up.name()).append("】**").append(up.description()).append("**");
                if (!upCost.equals(cost)) {
                    md.append("（费用 ").append(cost).append(" → **").append(upCost).append("**）");
                }
                // 关键字差异也标出来（例如"去掉消耗"）
                String baseKw = card.keywordLabel();
                String upKw = up.keywordLabel();
                if (!baseKw.equals(upKw)) {
                    md.append("（关键字：").append(baseKw.isEmpty() ? "无" : baseKw)
                            .append(" → **").append(upKw.isEmpty() ? "无" : upKw).append("**）");
                }
            } else {
                md.append("（登记为可强化但取不到，异常）");
            }
        } else {
            md.append("**无**");
        }
        md.append(" |\n\n");
    }

    private static String targetLabel(Card card) {
        return switch (card.target()) {
            case ENEMY -> "单个敌人";
            case ALL_ENEMIES -> "所有敌人";
            case SELF -> "自己";
            case NONE -> "无目标";
        };
    }

    private static String relicName(String relicId) {
        Relic relic = RelicLibrary.byId(relicId);
        return relic == null ? relicId : relic.name();
    }
}
