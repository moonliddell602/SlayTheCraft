package com.slaythecraft.cardreg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardBehaviors;
import com.slaythecraft.combat.core.CardEffect;
import com.slaythecraft.combat.core.CardKeyword;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardRarity;
import com.slaythecraft.combat.core.CardTarget;
import com.slaythecraft.combat.core.CardType;

/**
 * 数据包卡牌定义（v0.5.5.0 A6-1）。
 *
 * <p>一张卡 = 一个 JSON 文件，放在 {@code data/<数据包命名空间>/stc/card/<卡牌 id>.json}
 * （注册表键是 {@code stc:card}，所以路径里会出现两次 {@code stc}：一次是命名空间、一次是注册表名）。
 * 文件里的 **id 就是文件名**（不含 {@code .json}），并且**必须**是合法路径。</p>
 *
 * <p>完整字段与可用行为见 {@code docs/card-registry.md}。最小例子：</p>
 *
 * <pre>{@code
 * {
 *   "name": "打击",
 *   "type": "attack",
 *   "cost": 1,
 *   "target": "enemy",
 *   "description": "造成 6 点伤害。",
 *   "rarity": "common",
 *   "effects": [{"type": "damage", "numbers": [6]}],
 *   "upgrade": {"description": "造成 9 点伤害。", "effects": [{"type": "damage", "numbers": [9]}]}
 * }
 * }</pre>
 *
 * <p>⚠️ 只写基础定义时会**覆盖**掉那张内置卡（含它的强化版）：数据包说了算。想保留强化版就必须
 * 自己写 {@code upgrade} 段。</p>
 */
public record CardDefinition(String name, String type, int cost, String target, String description,
                             String rarity, Optional<String> character, List<String> keywords,
                             List<Behavior> effects, Optional<Upgrade> upgrade,
                             Optional<Boolean> xCost, Optional<Boolean> generated,
                             Optional<Boolean> multiplayer, Optional<Integer> maxUpgrades,
                             List<Behavior> onExhaust, List<Behavior> onDiscard) {

    /** 一条行为调用：{@code type} 是 {@link CardBehaviors} 里的 id，后两个是位置参数。 */
    public record Behavior(String type, List<Integer> numbers, List<String> strings) {

        public static final Codec<Behavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Behavior::type),
                Codec.INT.listOf().optionalFieldOf("numbers", List.of()).forGetter(Behavior::numbers),
                Codec.STRING.listOf().optionalFieldOf("strings", List.of()).forGetter(Behavior::strings)
        ).apply(instance, Behavior::new));

        public Behavior {
            numbers = numbers == null ? List.of() : List.copyOf(numbers);
            strings = strings == null ? List.of() : List.copyOf(strings);
        }
    }

    /**
     * 强化版定义。
     *
     * <p>四个字段都是可选的：不写就**继承基础版**（名字自动加 {@code +}）。所以"只把伤害从 6 改成 9"
     * 可以只写 {@code "effects"}。</p>
     */
    public record Upgrade(Optional<String> name, Optional<Integer> cost, Optional<String> description,
                          Optional<List<String>> keywords, Optional<List<Behavior>> effects) {

        public static final Codec<Upgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name").forGetter(Upgrade::name),
                Codec.INT.optionalFieldOf("cost").forGetter(Upgrade::cost),
                Codec.STRING.optionalFieldOf("description").forGetter(Upgrade::description),
                Codec.STRING.listOf().optionalFieldOf("keywords").forGetter(Upgrade::keywords),
                Behavior.CODEC.listOf().optionalFieldOf("effects").forGetter(Upgrade::effects)
        ).apply(instance, Upgrade::new));
    }

    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CardDefinition::name),
            Codec.STRING.fieldOf("type").forGetter(CardDefinition::type),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(CardDefinition::cost),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(CardDefinition::target),
            Codec.STRING.fieldOf("description").forGetter(CardDefinition::description),
            Codec.STRING.optionalFieldOf("rarity", "common").forGetter(CardDefinition::rarity),
            Codec.STRING.optionalFieldOf("character").forGetter(CardDefinition::character),
            Codec.STRING.listOf().optionalFieldOf("keywords", List.of()).forGetter(CardDefinition::keywords),
            Behavior.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(CardDefinition::effects),
            Upgrade.CODEC.optionalFieldOf("upgrade").forGetter(CardDefinition::upgrade),
            Codec.BOOL.optionalFieldOf("x_cost").forGetter(CardDefinition::xCost),
            Codec.BOOL.optionalFieldOf("generated").forGetter(CardDefinition::generated),
            Codec.BOOL.optionalFieldOf("multiplayer").forGetter(CardDefinition::multiplayer),
            Codec.INT.optionalFieldOf("max_upgrades").forGetter(CardDefinition::maxUpgrades),
            Behavior.CODEC.listOf().optionalFieldOf("on_exhaust", List.of()).forGetter(CardDefinition::onExhaust),
            Behavior.CODEC.listOf().optionalFieldOf("on_discard", List.of()).forGetter(CardDefinition::onDiscard)
    ).apply(instance, CardDefinition::new));

    public CardDefinition {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        effects = effects == null ? List.of() : List.copyOf(effects);
        onExhaust = onExhaust == null ? List.of() : List.copyOf(onExhaust);
        onDiscard = onDiscard == null ? List.of() : List.copyOf(onDiscard);
    }

    /**
     * 把它变成引擎能用的卡（{@link CardLibrary.ExternalCard}）。
     *
     * @param id     卡牌 id（= 文件名）
     * @param errors 出错信息会**追加**进来；只要这里有内容，调用方就必须跳过这张卡（不要静默用一半）
     * @return 引擎用的外部卡定义；有错时返回 {@code null}
     */
    public CardLibrary.ExternalCard toExternal(String id, List<String> errors) {
        int before = errors.size();
        CardType cardType = parseType(type, id, errors);
        CardTarget cardTarget = parseTarget(target, id, errors);
        CardRarity cardRarity = parseRarity(rarity, id, errors);
        Set<CardKeyword> cardKeywords = parseKeywords(keywords, id, errors);
        String owner = character.orElse(null);
        if (owner != null && !isKnownCharacter(owner)) {
            errors.add("卡 " + id + "：character「" + owner + "」不是已知角色（只能是 "
                    + String.join(" / ", KNOWN_CHARACTERS) + "，或整行删掉表示无色牌）");
        }
        CardEffect effect = buildEffect(effects, name, id, errors);
        if (errors.size() != before || cardType == null || cardTarget == null || cardRarity == null) {
            return null;
        }
        Card base = new Card(id, name, cardType, cost, cardTarget, description, cardRarity, owner,
                cardKeywords, effect);

        Card upgraded = null;
        if (upgrade.isPresent()) {
            int upgradeBefore = errors.size();
            Upgrade up = upgrade.get();
            String upgradedName = up.name().orElse(name + "+");
            int upgradedCost = up.cost().orElse(cost);
            String upgradedDescription = up.description().orElse(description);
            Set<CardKeyword> upgradedKeywords = up.keywords().isPresent()
                    ? parseKeywords(up.keywords().get(), id, errors) : cardKeywords;
            List<Behavior> upgradedEffects = up.effects().orElse(effects);
            CardEffect upgradedEffect = buildEffect(upgradedEffects, upgradedName, id, errors);
            if (errors.size() != upgradeBefore) {
                return null;
            }
            upgraded = new Card(id, upgradedName, cardType, upgradedCost, cardTarget, upgradedDescription,
                    cardRarity, owner, upgradedKeywords, upgradedEffect);
        }

        CardLibrary.ExternalCard external = new CardLibrary.ExternalCard(base, upgraded, xCost.orElse(null),
                generated.orElse(null), multiplayer.orElse(null), maxUpgrades.orElse(null),
                buildSelfExhaust(onExhaust, name, id, errors), buildSelfDiscard(onDiscard, name, id, errors));
        return errors.size() == before ? external : null;
    }

    /** 已知角色 id（与 {@code CharacterRegistry} 一致）。 */
    public static final List<String> KNOWN_CHARACTERS = List.of(CardLibrary.IRONCLAD, CardLibrary.SILENT,
            CardLibrary.DEFECT, CardLibrary.PILLAGER);

    private static boolean isKnownCharacter(String id) {
        return KNOWN_CHARACTERS.contains(id);
    }

    private static CardType parseType(String raw, String cardId, List<String> errors) {
        if (raw != null) {
            for (CardType candidate : CardType.values()) {
                if (candidate.name().equalsIgnoreCase(raw.trim())) {
                    return candidate;
                }
            }
        }
        errors.add("卡 " + cardId + "：type「" + raw + "」不认识（只能是 attack / skill / power / status / curse）");
        return null;
    }

    private static CardTarget parseTarget(String raw, String cardId, List<String> errors) {
        if (raw != null) {
            for (CardTarget candidate : CardTarget.values()) {
                if (candidate.name().equalsIgnoreCase(raw.trim())) {
                    return candidate;
                }
            }
        }
        errors.add("卡 " + cardId + "：target「" + raw + "」不认识（只能是 enemy / all_enemies / self / none）");
        return null;
    }

    private static CardRarity parseRarity(String raw, String cardId, List<String> errors) {
        if (raw != null) {
            for (CardRarity candidate : CardRarity.values()) {
                if (candidate.name().equalsIgnoreCase(raw.trim())) {
                    return candidate;
                }
            }
        }
        errors.add("卡 " + cardId + "：rarity「" + raw + "」不认识（只能是 common / uncommon / rare / gold）");
        return null;
    }

    private static Set<CardKeyword> parseKeywords(List<String> raw, String cardId, List<String> errors) {
        Set<CardKeyword> parsed = new LinkedHashSet<>();
        for (String name : raw) {
            CardKeyword found = null;
            for (CardKeyword candidate : CardKeyword.values()) {
                if (candidate.name().equalsIgnoreCase(name == null ? "" : name.trim())) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                errors.add("卡 " + cardId + "：关键字「" + name + "」不认识（只能是 retain / exhaust / ethereal / innate）");
            } else {
                parsed.add(found);
            }
        }
        return parsed;
    }

    /** 按顺序拼出效果（多个行为依次执行；空列表 = 打出后什么都不发生）。 */
    private static CardEffect buildEffect(List<Behavior> behaviors, String cardName, String cardId,
                                          List<String> errors) {
        List<CardEffect> parts = new ArrayList<>();
        for (Behavior behavior : behaviors) {
            String problem = CardBehaviors.validate(behavior.type(), behavior.numbers(), behavior.strings());
            if (problem != null) {
                errors.add("卡 " + cardId + "： " + problem);
                continue;
            }
            CardEffect part = CardBehaviors.build(behavior.type(), cardName, behavior.numbers(),
                    behavior.strings());
            if (part == null) {
                errors.add("卡 " + cardId + "：行为「" + behavior.type() + "」构造失败");
            } else {
                parts.add(part);
            }
        }
        return (ctx, self, target) -> {
            for (CardEffect part : parts) {
                part.apply(ctx, self, target);
            }
        };
    }

    /**
     * "这张牌自己被消耗时"的效果。
     *
     * <p>⚠️ 行为表里的行为都按 {@code (ctx, self, target)} 工作，所以这里把**牌的拥有者**当
     * {@code self}、把 {@code target} 设成 {@code null} —— 数据包造的"自消耗效果"**看不到这张牌是不是强化版**
     * （原版【哨兵】要的正是这个判断，那种效果只能写在代码里）。</p>
     */
    private static CardLibrary.SelfExhaustEffect buildSelfExhaust(List<Behavior> behaviors, String cardName,
                                                                  String cardId, List<String> errors) {
        if (behaviors.isEmpty()) {
            return null;
        }
        CardEffect effect = buildEffect(behaviors, cardName, cardId, errors);
        return (ctx, owner, card) -> effect.apply(ctx, owner == null ? null : owner.combatant(), null);
    }

    /** 与 {@link #buildSelfExhaust} 完全对称（挂载点是"出牌环节里被弃置"）。 */
    private static CardLibrary.SelfDiscardEffect buildSelfDiscard(List<Behavior> behaviors, String cardName,
                                                                  String cardId, List<String> errors) {
        if (behaviors.isEmpty()) {
            return null;
        }
        CardEffect effect = buildEffect(behaviors, cardName, cardId, errors);
        return (ctx, owner, card) -> effect.apply(ctx, owner == null ? null : owner.combatant(), null);
    }

    /** 指令里展示用的一行说明。 */
    public String describe(String id) {
        return id + "（" + name + "）：" + cost + " 费 · " + type + " · " + target + " · " + rarity
                + " · " + description;
    }

    /** 供自检比对：把关键字列表规范化成小写排序，避免大小写/顺序造成的假差异。 */
    public List<String> normalizedKeywords() {
        List<String> out = new ArrayList<>();
        for (String keyword : keywords) {
            out.add(keyword.toLowerCase(Locale.ROOT));
        }
        out.sort(String::compareTo);
        return out;
    }
}
