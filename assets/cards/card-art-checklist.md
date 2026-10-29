# 卡面贴图 id 总表（自动生成，共 263 个 id）

> 由 `powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -List` 生成；
> 改了卡池之后重跑一次即可。机器可读版：[`card-art-checklist.csv`](card-art-checklist.csv)

## 怎么用

1. 图片按 **卡牌 id** 命名：`<id>.png`，放进 `assets/cards/`；
2. 跑 `powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -Import`
   —— 批量复制到 `src/main/resources/assets/stc/textures/card/` 并报告还缺哪些；
3. 重新构建（`.\gradlew.bat build --offline`）。

**注意**：

* **强化版与基础版共用一张图**（同一个 id），所以只需要一个文件；
* **生成牌**（小刀 / 弩箭射击 / 魔术技巧的费用档…）也按各自的 id 取图：
  想让它们长得像母牌，就把母牌的图**复制成这些文件名**（例：`shiv.png`、`bolt_shot.png`）；
* 缺图不会崩：插画位画成占位框（灰底 + 「图」字）。

### 生成牌的共图建议（省得单独画）

| 生成牌 id | 建议直接复制哪张图 |
| --- | --- |
| `magic_trick_d1` … `magic_trick_d10` | `magic_trick.png`（同一个「魔术技巧！」，只是费用档不同） |
| `modded_boost` / `modded_boost_plus` / `modded_boost_2` / `modded_boost_2_plus` | `modded.png`（模组改造的费用档） |
| `adaptive_strike_free` | `adaptive_strike.png`（适应打击的 0 费复制品） |
| `shiv`（小刀） | 自己一张（它是独立卡牌，虽然由卡牌生成） |
| `bolt_shot`（弩箭射击） | 自己一张（同上） |

> 分组按**类型优先**：属于角色的状态牌（如叛逃掠夺者的『伤口』）会归到「状态牌」一组，
> 所以「叛逃掠夺者 33 + 状态牌 9」与文档里的「掠夺者 34 + 状态牌 8」是同一批卡。

## 分组总览

| 分组 | id 数 |
| --- | ---: |
| 铁甲战士 | 73 |
| 静默猎手 | 73 |
| 故障机器人（机宝） | 49 |
| 叛逃掠夺者 | 33 |
| 无色牌 | 3 |
| 基础牌（打击 / 格挡） | 2 |
| 状态牌 | 9 |
| 诅咒牌 | 4 |
| 生成牌（只能被遗物 / 卡牌生成，不进任何卡池） | 17 |
| **合计** | **263** |

## 铁甲战士（73）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `anger` | 愤怒 | 攻击 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/anger.png` |
| `clash` | 交锋 | 攻击 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/clash.png` |
| `flex` | 灵活 | 技能 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/flex.png` |
| `warcry` | 战吼 | 技能 | 普通 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/warcry.png` |
| `armaments` | 军备 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/armaments.png` |
| `body_slam` | 撞击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/body_slam.png` |
| `cleave` | 顺劈斩 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/cleave.png` |
| `havoc` | 浩劫 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/havoc.png` |
| `headbutt` | 头槌 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/headbutt.png` |
| `iron_wave` | 铁斩波 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/iron_wave.png` |
| `sword_boomerang` | 剑气 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/sword_boomerang.png` |
| `thunderclap` | 雷霆一击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/thunderclap.png` |
| `true_grit` | 坚毅 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/true_grit.png` |
| `twin_strike` | 双重打击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/twin_strike.png` |
| `wild_strike` | 野性打击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/wild_strike.png` |
| `bash` | 重击 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/bash.png` |
| `clothesline` | 猛击 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/clothesline.png` |
| `heavy_blade` | 重刃 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/heavy_blade.png` |
| `perfected_strike` | 完美打击 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/perfected_strike.png` |
| `battle_trance` | 战斗三昧 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/battle_trance.png` |
| `bloodletting` | 血祭 | 技能 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/bloodletting.png` |
| `intimidate` | 恐吓 | 技能 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/intimidate.png` |
| `rage` | 狂怒 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/rage.png` |
| `reckless_charge` | 鲁莽冲锋 | 攻击 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/reckless_charge.png` |
| `whirlwind` | 旋风斩 | 攻击 | 罕见 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/whirlwind.png` |
| `burning_pact` | 燃烧契约 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/burning_pact.png` |
| `combust` | 自燃 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/combust.png` |
| `disarm` | 卸甲 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/disarm.png` |
| `dropkick` | 飞踢 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dropkick.png` |
| `dual_wield` | 双持 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dual_wield.png` |
| `evolve` | 进化 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/evolve.png` |
| `feel_no_pain` | 无痛 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/feel_no_pain.png` |
| `fire_breathing` | 火焰吐息 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/fire_breathing.png` |
| `ghostly_armor` | 幽灵护甲 | 技能 | 罕见 | 1 | 可强化 [虚无] | `src/main/resources/assets/stc/textures/card/ghostly_armor.png` |
| `hemokinesis` | 血液动力学 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/hemokinesis.png` |
| `infernal_blade` | 地狱之刃 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/infernal_blade.png` |
| `inflame` | 燃烧 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/inflame.png` |
| `metallicize` | 金属化 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/metallicize.png` |
| `pommel_strike` | 柄击 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/pommel_strike.png` |
| `power_through` | 强力冲锋 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/power_through.png` |
| `pummel` | 强力打击 | 攻击 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/pummel.png` |
| `rampage` | 暴走 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/rampage.png` |
| `rupture` | 破灭 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/rupture.png` |
| `second_wind` | 第二次呼吸 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/second_wind.png` |
| `seeing_red` | 见红 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/seeing_red.png` |
| `sentinel` | 哨兵 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/sentinel.png` |
| `shrug_it_off` | 不痛不痒 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/shrug_it_off.png` |
| `spot_weakness` | 察觉弱点 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/spot_weakness.png` |
| `carnage` | 杀戮 | 攻击 | 罕见 | 2 | 可强化 [虚无] | `src/main/resources/assets/stc/textures/card/carnage.png` |
| `dark_embrace` | 黑暗拥抱 | 能力 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/dark_embrace.png` |
| `entrench` | 巩固 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/entrench.png` |
| `flame_barrier` | 火焰屏障 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/flame_barrier.png` |
| `searing_blow` | 灼热打击 | 攻击 | 罕见 | 2 | 可强化(无限) | `src/main/resources/assets/stc/textures/card/searing_blow.png` |
| `sever_soul` | 斩断灵魂 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/sever_soul.png` |
| `shockwave` | 冲击波 | 技能 | 罕见 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/shockwave.png` |
| `uppercut` | 上勾拳 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/uppercut.png` |
| `blood_for_blood` | 血债血偿 | 攻击 | 罕见 | 4 | 可强化 | `src/main/resources/assets/stc/textures/card/blood_for_blood.png` |
| `berserk` | 狂暴 | 能力 | 稀有 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/berserk.png` |
| `brutality` | 残忍 | 能力 | 稀有 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/brutality.png` |
| `offering` | 牺牲 | 技能 | 稀有 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/offering.png` |
| `double_tap` | 双重施法 | 技能 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/double_tap.png` |
| `exhume` | 挖掘 | 技能 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/exhume.png` |
| `feed` | 进食 | 攻击 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/feed.png` |
| `limit_break` | 突破极限 | 技能 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/limit_break.png` |
| `fiend_fire` | 恶魔火焰 | 攻击 | 稀有 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/fiend_fire.png` |
| `immolate` | 献祭 | 攻击 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/immolate.png` |
| `impervious` | 无懈可击 | 技能 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/impervious.png` |
| `juggernaut` | 主宰 | 能力 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/juggernaut.png` |
| `reaper` | 大扫除 | 攻击 | 稀有 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/reaper.png` |
| `barricade` | 壁垒 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/barricade.png` |
| `bludgeon` | 重锤 | 攻击 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/bludgeon.png` |
| `corruption` | 腐化 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/corruption.png` |
| `demon_form` | 恶魔形态 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/demon_form.png` |

## 静默猎手（73）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `deflect` | 偏斜 | 技能 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/deflect.png` |
| `prepared` | 准备 | 技能 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/prepared.png` |
| `slice` | 切片 | 攻击 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/slice.png` |
| `acrobatics` | 杂技 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/acrobatics.png` |
| `backflip` | 后空翻 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/backflip.png` |
| `bane` | 剧毒 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/bane.png` |
| `blade_dance` | 刃舞 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/blade_dance.png` |
| `cloak_and_dagger` | 斗篷与匕首 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/cloak_and_dagger.png` |
| `dagger_spray` | 匕首雨 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dagger_spray.png` |
| `dagger_throw` | 飞刀投掷 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dagger_throw.png` |
| `dodge_and_roll` | 闪避翻滚 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dodge_and_roll.png` |
| `flying_knee` | 飞膝 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/flying_knee.png` |
| `heel_hook` | 脚后跟钩 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/heel_hook.png` |
| `neutralize` | 中和 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/neutralize.png` |
| `outmaneuver` | 机动 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/outmaneuver.png` |
| `poisoned_stab` | 毒刺 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/poisoned_stab.png` |
| `quick_slash` | 快斩 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/quick_slash.png` |
| `sucker_punch` | 偷袭 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/sucker_punch.png` |
| `sneaky_strike` | 偷偷一击 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/sneaky_strike.png` |
| `backstab` | 背刺 | 攻击 | 罕见 | 0 | 可强化 [固有/消耗] | `src/main/resources/assets/stc/textures/card/backstab.png` |
| `calculated_gamble` | 计算赌博 | 技能 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/calculated_gamble.png` |
| `concentrate` | 集中精神 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/concentrate.png` |
| `endless_agony` | 无尽苦痛 | 攻击 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/endless_agony.png` |
| `escape_plan` | 逃生计划 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/escape_plan.png` |
| `masterful_stab` | 高超打击 | 攻击 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/masterful_stab.png` |
| `reflex` | 反射 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/reflex.png` |
| `skewer` | 串刺 | 攻击 | 罕见 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/skewer.png` |
| `tactician` | 战术家 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/tactician.png` |
| `accuracy` | 精准 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/accuracy.png` |
| `all_out_attack` | 全力攻击 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/all_out_attack.png` |
| `blur` | 模糊 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/blur.png` |
| `bubble_bubble` | 咕嘟冒泡 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/bubble_bubble.png` |
| `caltrops` | 铁蒺藜 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/caltrops.png` |
| `distraction` | 转移 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/distraction.png` |
| `expertise` | 专精 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/expertise.png` |
| `finisher` | 终结技 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/finisher.png` |
| `flechettes` | 飞刀 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/flechettes.png` |
| `footwork` | 脚部功夫 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/footwork.png` |
| `infinite_blades` | 无限刀刃 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/infinite_blades.png` |
| `noxious_fumes` | 毒雾 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/noxious_fumes.png` |
| `setup` | 布置 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/setup.png` |
| `survivor` | 生存 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/survivor.png` |
| `terror` | 恐惧 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/terror.png` |
| `well_laid_plans` | 计划周全 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/well_laid_plans.png` |
| `bouncing_flask` | 弹跳药瓶 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/bouncing_flask.png` |
| `choke` | 窒息 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/choke.png` |
| `crippling_cloud` | 致残毒云 | 技能 | 罕见 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/crippling_cloud.png` |
| `dash` | 冲刺 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/dash.png` |
| `leg_sweep` | 扫腿 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/leg_sweep.png` |
| `predator` | 捕食者 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/predator.png` |
| `riddle_with_holes` | 千疮百孔 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/riddle_with_holes.png` |
| `eviscerate` | 剔骨 | 攻击 | 罕见 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/eviscerate.png` |
| `adrenaline` | 肾上腺素 | 技能 | 稀有 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/adrenaline.png` |
| `doppelganger` | 分身 | 技能 | 稀有 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/doppelganger.png` |
| `grand_finale` | 华丽收场 | 攻击 | 稀有 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/grand_finale.png` |
| `malaise` | 不适 | 技能 | 稀有 | 0 | X费 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/malaise.png` |
| `after_image` | 余像 | 能力 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/after_image.png` |
| `alchemize` | 炼金 | 技能 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/alchemize.png` |
| `burst` | 爆发 | 技能 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/burst.png` |
| `catalyst` | 触媒 | 能力 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/catalyst.png` |
| `deadly_poison` | 致命毒药 | 技能 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/deadly_poison.png` |
| `die_die_die` | 死吧死吧死吧 | 攻击 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/die_die_die.png` |
| `glass_knife` | 玻璃刀 | 攻击 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/glass_knife.png` |
| `phantasmal_killer` | 幻影杀手 | 技能 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/phantasmal_killer.png` |
| `storm_of_steel` | 钢铁风暴 | 技能 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/storm_of_steel.png` |
| `tools_of_the_trade` | 行业工具 | 能力 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/tools_of_the_trade.png` |
| `unload` | 卸货 | 攻击 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/unload.png` |
| `a_thousand_cuts` | 千刀万剐 | 能力 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/a_thousand_cuts.png` |
| `corpse_explosion` | 尸体爆炸 | 技能 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/corpse_explosion.png` |
| `envenom` | 涂毒 | 能力 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/envenom.png` |
| `bullet_time` | 子弹时间 | 技能 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/bullet_time.png` |
| `nightmare` | 噩梦 | 技能 | 稀有 | 3 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/nightmare.png` |
| `wraith_form` | 幽魂形态 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/wraith_form.png` |

## 故障机器人（机宝）（49）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `beam_cell` | 光束 | 攻击 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/beam_cell.png` |
| `boot_sequence` | 启动 | 技能 | 普通 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/boot_sequence.png` |
| `claw_defect` | 爪击 | 攻击 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/claw_defect.png` |
| `steam_barrier` | 蒸汽护盾 | 技能 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/steam_barrier.png` |
| `turbo` | 涡轮 | 技能 | 普通 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/turbo.png` |
| `ball_lightning` | 电球生成 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/ball_lightning.png` |
| `barrage_defect` | 锯齿 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/barrage_defect.png` |
| `charge_battery` | 充能电池 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/charge_battery.png` |
| `cold_snap` | 寒冰碎片 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/cold_snap.png` |
| `coolheaded` | 冷却 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/coolheaded.png` |
| `darkness` | 暗影 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/darkness.png` |
| `dualcast` | 双重施法 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dualcast.png` |
| `frost` | 冰球 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/frost.png` |
| `hologram` | 全息影像 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/hologram.png` |
| `rebound` | 回弹 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/rebound.png` |
| `recycle` | 回收 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/recycle.png` |
| `stack` | 堆叠 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/stack.png` |
| `zap` | 电击 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/zap.png` |
| `chill` | 冰雹 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/chill.png` |
| `overclock` | 超频 | 技能 | 罕见 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/overclock.png` |
| `aggregate` | 汇总 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/aggregate.png` |
| `amplify` | 增幅器 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/amplify.png` |
| `blizzard` | 暴风雪 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/blizzard.png` |
| `capacitor` | 电容器 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/capacitor.png` |
| `chaos` | 混沌 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/chaos.png` |
| `defragment` | 碎片整理 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/defragment.png` |
| `double_energy` | 双重能量 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/double_energy.png` |
| `dual_cast` | 双重释放 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/dual_cast.png` |
| `loop` | 循环 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/loop.png` |
| `recursion` | 递归 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/recursion.png` |
| `reprogram` | 重编程 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/reprogram.png` |
| `scrape` | 刮削 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/scrape.png` |
| `static_discharge` | 静电放电 | 能力 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/static_discharge.png` |
| `white_noise` | 白噪声 | 技能 | 罕见 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/white_noise.png` |
| `buffer` | 缓冲 | 能力 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/buffer.png` |
| `fusion` | 核聚变 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/fusion.png` |
| `glacier` | 冰封 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/glacier.png` |
| `sunder` | 碎裂 | 攻击 | 罕见 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/sunder.png` |
| `modded` | 模组改造 | 技能 | 稀有 | 0 | 可强化 | `src/main/resources/assets/stc/textures/card/modded.png` |
| `multi_cast` | 多重施法 | 技能 | 稀有 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/multi_cast.png` |
| `reboot` | 重启 | 技能 | 稀有 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/reboot.png` |
| `tempest` | 风暴 | 技能 | 稀有 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/tempest.png` |
| `adaptive_strike` | 适应打击 | 攻击 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/adaptive_strike.png` |
| `electrodynamics` | 电动力学 | 能力 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/electrodynamics.png` |
| `hyperbeam` | 超能光束 | 攻击 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/hyperbeam.png` |
| `creative_ai` | 创造力 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/creative_ai.png` |
| `thunder` | 雷霆万钧 | 技能 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/thunder.png` |
| `voltaic` | 电流相生 | 技能 | 稀有 | 3 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/voltaic.png` |
| `meteor_strike` | 流星 | 攻击 | 稀有 | 5 | 可强化 | `src/main/resources/assets/stc/textures/card/meteor_strike.png` |

## 叛逃掠夺者（33）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `gather_potatoes` | 收集土豆 | 技能 | 普通 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/gather_potatoes.png` |
| `aimed_shot` | 集中射击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/aimed_shot.png` |
| `decoy` | 诱饵 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/decoy.png` |
| `mark_target` | 锁定目标 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/mark_target.png` |
| `recuperate` | 休养生息 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/recuperate.png` |
| `roll` | 翻滚 | 技能 | 普通 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/roll.png` |
| `smoke_bomb` | 烟雾弹 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/smoke_bomb.png` |
| `quick_leap` | 快跃！这货要逃 | 攻击 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/quick_leap.png` |
| `reload_bolts` | 装填弩箭 | 技能 | 普通 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/reload_bolts.png` |
| `take_cover` | 寻找掩体 | 技能 | 普通 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/take_cover.png` |
| `break_out` | 突出重围 | 技能 | 罕见 | 0 | 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/break_out.png` |
| `dodge_calamity` | 躲避灾厄 | 技能 | 罕见 | 0 | X费 可强化 | `src/main/resources/assets/stc/textures/card/dodge_calamity.png` |
| `emergency_dodge` | 紧急避险 | 技能 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/emergency_dodge.png` |
| `hearty_meal` | 快哉，肉排！ | 技能 | 罕见 | 0 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/hearty_meal.png` |
| `barrage` | 多重射击 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/barrage.png` |
| `iron_arrow` | 铁质箭矢 | 攻击 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/iron_arrow.png` |
| `scrap_recycle` | 废料回收 | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/scrap_recycle.png` |
| `united_we_stand` | 我们团结一心！ | 技能 | 罕见 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/united_we_stand.png` |
| `bowstring_upgrade` | 弓弦强化 | 能力 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/bowstring_upgrade.png` |
| `caravan_escort` | 商队护送 | 技能 | 罕见 | 2 | 多人玩法向 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/caravan_escort.png` |
| `quiver_refill` | 箭袋补充 | 技能 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/quiver_refill.png` |
| `repeating_crossbow` | 连弩射击 | 攻击 | 罕见 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/repeating_crossbow.png` |
| `abandon_border` | 弃置边陵 | 技能 | 稀有 | 0 | 可强化 [固有/保留] | `src/main/resources/assets/stc/textures/card/abandon_border.png` |
| `all_suffering` | 众生皆苦 | 能力 | 稀有 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/all_suffering.png` |
| `contingency` | 应急储备 | 技能 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/contingency.png` |
| `shipwreck_salvage` | 沉船打捞 | 技能 | 稀有 | 1 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/shipwreck_salvage.png` |
| `piercing_arrow` | 穿云箭 | 攻击 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/piercing_arrow.png` |
| `wildfire` | 山火 | 攻击 | 稀有 | 2 | 可强化 | `src/main/resources/assets/stc/textures/card/wildfire.png` |
| `high_noon` | 午时已到 | 攻击 | 稀有 | 3 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/high_noon.png` |
| `meteor_chain` | 流星飞索 | 攻击 | 稀有 | 3 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/meteor_chain.png` |
| `pillage_form` | 劫掠形态 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/pillage_form.png` |
| `village_hero` | 村庄英雄 | 能力 | 稀有 | 3 | 可强化 | `src/main/resources/assets/stc/textures/card/village_hero.png` |
| `magic_trick` | 魔术技巧！ | 攻击 | 稀有 | 10 | 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick.png` |

## 无色牌（3）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `flashbang` | 闪光弹 | 技能 | 普通 | 0 | 可强化 [保留/消耗] | `src/main/resources/assets/stc/textures/card/flashbang.png` |
| `cake` | 蛋糕 | 技能 | 普通 | 2 | 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/cake.png` |
| `arrow_rain` | 箭雨 | 攻击 | 罕见 | 1 | 可强化 [保留] | `src/main/resources/assets/stc/textures/card/arrow_rain.png` |

## 基础牌（打击 / 格挡）（2）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `defend` | 格挡 | 技能 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/defend.png` |
| `strike` | 打击 | 攻击 | 普通 | 1 | 可强化 | `src/main/resources/assets/stc/textures/card/strike.png` |

## 状态牌（9）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `arrow_nocked` | 箭在弦上 | 状态 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/arrow_nocked.png` |
| `burn` | 灼烧 | 状态 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/burn.png` |
| `dazed` | 晕眩 | 状态 | 普通 | 0 | [虚无] | `src/main/resources/assets/stc/textures/card/dazed.png` |
| `void` | 虚空 | 状态 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/void.png` |
| `wound` | 伤口 | 状态 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/wound.png` |
| `call` | 呼唤 | 状态 | 普通 | 1 |  | `src/main/resources/assets/stc/textures/card/call.png` |
| `slimed` | 粘液 | 状态 | 普通 | 1 | [消耗] | `src/main/resources/assets/stc/textures/card/slimed.png` |
| `venom` | 毒液 | 状态 | 普通 | 1 |  | `src/main/resources/assets/stc/textures/card/venom.png` |
| `last_stand` | 不屈 | 状态 | 稀有 | 0 | [保留] | `src/main/resources/assets/stc/textures/card/last_stand.png` |

## 诅咒牌（4）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `curse_cruel` | 残忍 | 诅咒 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/curse_cruel.png` |
| `curse_greed` | 贪婪 | 诅咒 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/curse_greed.png` |
| `curse_stubborn` | 固执 | 诅咒 | 普通 | 0 |  | `src/main/resources/assets/stc/textures/card/curse_stubborn.png` |
| `curse_mockery` | 戏谑 | 诅咒 | 普通 | 1 |  | `src/main/resources/assets/stc/textures/card/curse_mockery.png` |

## 生成牌（只能被遗物 / 卡牌生成，不进任何卡池）（17）

| 卡牌 id（= 文件名） | 卡名 | 类型 | 品质 | 费 | 标记 | 贴图路径 |
| --- | --- | --- | --- | ---: | --- | --- |
| `bolt_shot` | 弩箭射击 | 攻击 | 普通 | 0 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/bolt_shot.png` |
| `shiv` | 小刀 | 攻击 | 普通 | 0 | 生成牌 可强化 [消耗] | `src/main/resources/assets/stc/textures/card/shiv.png` |
| `adaptive_strike_free` | 适应打击（0费） | 攻击 | 稀有 | 0 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/adaptive_strike_free.png` |
| `magic_trick_d10` | 魔术技巧！ | 攻击 | 稀有 | 0 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d10.png` |
| `magic_trick_d9` | 魔术技巧！ | 攻击 | 稀有 | 1 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d9.png` |
| `modded_boost` | 模组改造+ | 技能 | 稀有 | 1 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/modded_boost.png` |
| `modded_boost_plus` | 模组改造+ | 技能 | 稀有 | 1 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/modded_boost_plus.png` |
| `magic_trick_d8` | 魔术技巧！ | 攻击 | 稀有 | 2 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d8.png` |
| `modded_boost_2` | 模组改造+ | 技能 | 稀有 | 2 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/modded_boost_2.png` |
| `modded_boost_2_plus` | 模组改造+ | 技能 | 稀有 | 2 | 生成牌 可强化 | `src/main/resources/assets/stc/textures/card/modded_boost_2_plus.png` |
| `magic_trick_d7` | 魔术技巧！ | 攻击 | 稀有 | 3 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d7.png` |
| `magic_trick_d6` | 魔术技巧！ | 攻击 | 稀有 | 4 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d6.png` |
| `magic_trick_d5` | 魔术技巧！ | 攻击 | 稀有 | 5 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d5.png` |
| `magic_trick_d4` | 魔术技巧！ | 攻击 | 稀有 | 6 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d4.png` |
| `magic_trick_d3` | 魔术技巧！ | 攻击 | 稀有 | 7 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d3.png` |
| `magic_trick_d2` | 魔术技巧！ | 攻击 | 稀有 | 8 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d2.png` |
| `magic_trick_d1` | 魔术技巧！ | 攻击 | 稀有 | 9 | 生成牌 可强化 [消耗/保留/固有] | `src/main/resources/assets/stc/textures/card/magic_trick_d1.png` |
