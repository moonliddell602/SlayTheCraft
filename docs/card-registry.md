# 卡牌数据包注册表 `stc:card`（v0.5.5.0 / A6-1）

> 给**整合包作者**看的说明：怎么用数据包**新增卡牌**、**改内置卡的数值**，以及有哪些做不到的事。
> 代码侧的接线说明在 `docs/handover.md` §4.49。

---

## 1. 一句话

卡牌定义放在数据包里的 JSON：

```
data/<你的数据包命名空间>/stc/card/<卡牌 id>.json
```

* 注册表键是 `stc:card`，NeoForge 的规则是 `data/<数据包命名空间>/<注册表键命名空间>/<注册表键路径>/`，
  所以路径里出现**两个** `stc`（前一个是你的数据包，后一个是注册表的命名空间）；
* **文件名（去掉 `.json`）就是卡牌 id**，例如 `data/mypack/stc/card/fireball.json` → `stc:fireball`；
* 命名空间**必须是 `stc`**（卡牌 id 的命名空间固定是 `stc`，不是你的数据包名）；
* 内置的 11 个 JSON（10 张试点镜像 + 1 张示例卡）就在 `src/main/resources/data/stc/stc/card/`，可以照抄。

数据包在**服务器启动**与 **`/reload`** 时加载，并**同步给客户端**（客户端装同一个 mod 即可，
不需要你的数据包文件）。

---

## 2. 字段表

| 字段 | 类型 | 必填 | 缺省 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | 字符串 | ✅ | — | 卡名（卡面与战报都显示这个） |
| `type` | 字符串 | ✅ | — | `attack` / `skill` / `power` / `status` / `curse` |
| `cost` | 整数 | | `0` | 费用 |
| `target` | 字符串 | | `self` | `enemy`（要选敌人）/ `all_enemies` / `self` / `none` |
| `description` | 字符串 | ✅ | — | 卡面文案（**自己写**，不会根据效果自动生成） |
| `rarity` | 字符串 | | `common` | `common` / `uncommon` / `rare` / `gold` |
| `character` | 字符串 | | 无 | `stc:ironclad` / `stc:silent` / `stc:defect` / `stc:pillager`；**不写 = 无色牌** |
| `keywords` | 字符串数组 | | `[]` | `retain` / `exhaust` / `ethereal` / `innate` |
| `effects` | 行为数组 | | `[]` | 打出后的效果（按顺序结算） |
| `upgrade` | 对象 | | 无 | 强化版（见下）；**不写就表示这张卡没有强化版** |
| `x_cost` | 布尔 | | 继承 | X 费卡（花光当前全部能量） |
| `generated` | 布尔 | | 继承 | **生成牌**：不进奖励池 / 商店 / 无色池 |
| `multiplayer` | 布尔 | | 继承 | 图鉴里归到「多人玩法向」那一页 |
| `max_upgrades` | 整数 | | 继承 | 最多能强化几次（默认：有 `upgrade` 就是 1，否则 0） |
| `on_exhaust` | 行为数组 | | 无 | **这张牌自己被消耗时**的额外效果 |
| `on_discard` | 行为数组 | | 无 | **这张牌在出牌环节被弃置时**的额外效果 |

`upgrade` 段（四项都可选，缺省 = 继承基础版）：

| 字段 | 类型 | 缺省 |
| --- | --- | --- |
| `name` | 字符串 | 基础卡名 + `+` |
| `cost` | 整数 | 基础费用 |
| `description` | 字符串 | 基础描述 |
| `keywords` | 字符串数组 | 基础关键字 |
| `effects` | 行为数组 | 基础效果 |

**行为数组**里每个元素：

```json
{"type": "damage", "numbers": [6], "strings": []}
```

* `type` 必须是下面**行为表**里的 id；
* `numbers` / `strings` 都是**位置参数**（第 0 个、第 1 个…），个数不对会被拒绝。

---

## 3. 行为表（`/stc card behaviors` 也能列出来）

| type | numbers | strings | 效果 |
| --- | --- | --- | --- |
| `damage` | 伤害 | — | 对目标造成 N 点伤害（`target` 不是 `enemy` 时取第一个存活敌人） |
| `damage_all` | 伤害 | — | 对所有存活敌人各造成 N 点伤害 |
| `damage_random` | 伤害, 次数 | — | 对随机存活敌人造成 N 点伤害，共 M 次 |
| `block` | 格挡 | — | 获得 N 点格挡 |
| `next_turn_block` | 格挡 | — | 下个回合开始时获得 N 点格挡 |
| `draw` | 张数 | — | 抽 N 张牌 |
| `energy` | 能量 | — | 获得 N 点能量 |
| `heal_self` | 生命 | — | 回复 N 点生命 |
| `lose_hp_self` | 生命 | — | 失去 N 点生命（无视格挡） |
| `strength_self` | 层数 | — | 获得 N 点力量 |
| `temporary_strength_self` | 层数 | — | 获得 N 点力量（回合结束收回） |
| `dexterity_self` | 层数 | — | 获得 N 点敏捷 |
| `poison` | 层数 | — | 对目标施加 N 层中毒 |
| `poison_all` | 层数 | — | 对所有存活敌人施加 N 层中毒 |
| `weak` | 层数 | — | 对目标施加 N 层虚弱 |
| `weak_all` | 层数 | — | 对所有存活敌人施加 N 层虚弱 |
| `vulnerable` | 层数 | — | 对目标施加 N 层易伤 |
| `vulnerable_all` | 层数 | — | 对所有存活敌人施加 N 层易伤 |
| `add_card` | 张数 | 卡牌 id | 把指定的卡加 N 张进手牌（例如 `{"type":"add_card","numbers":[1],"strings":["shiv"]}`） |

> 想加**新机制**（像【腐化】【双发】那种）必须先在代码的 `CardBehaviors` 里登记一条新行为 ——
> 数据包只能组合这张表里已有的行为。

---

## 4. 三个例子

### 4.1 只加一张新卡（生成牌，不进任何卡池）

`data/mypack/stc/card/fireball.json`

```json
{
  "name": "火球",
  "type": "attack",
  "cost": 1,
  "target": "enemy",
  "description": "造成 7 点伤害，给予 2 层易伤。",
  "rarity": "common",
  "generated": true,
  "effects": [
    {"type": "damage", "numbers": [7]},
    {"type": "vulnerable", "numbers": [2]}
  ],
  "upgrade": {
    "description": "造成 10 点伤害，给予 3 层易伤。",
    "effects": [
      {"type": "damage", "numbers": [10]},
      {"type": "vulnerable", "numbers": [3]}
    ]
  }
}
```

### 4.2 覆盖内置卡的数值（把【打击】改成 10 点）

同名文件会**整张覆盖**内置卡，所以连强化版一起写：

`data/mypack/stc/card/strike.json`

```json
{
  "name": "打击",
  "type": "attack",
  "cost": 1,
  "target": "enemy",
  "description": "造成 10 点伤害。",
  "rarity": "common",
  "effects": [{"type": "damage", "numbers": [10]}],
  "upgrade": {
    "name": "打击+",
    "description": "造成 14 点伤害。",
    "effects": [{"type": "damage", "numbers": [14]}]
  }
}
```

> ⚠️ 只写基础定义 = **这张卡不再有强化版**（原来有的也会被去掉）。

### 4.3 加一张"被消耗时给能量"的卡（像【哨兵】）

```json
{
  "name": "余烬",
  "type": "skill",
  "cost": 1,
  "target": "self",
  "description": "获得 8 点格挡。消耗。如果这张牌被消耗，获得 2 点能量。",
  "rarity": "uncommon",
  "character": "stc:ironclad",
  "keywords": ["exhaust"],
  "effects": [{"type": "block", "numbers": [8]}],
  "on_exhaust": [{"type": "energy", "numbers": [2]}]
}
```

⚠️ `on_exhaust` / `on_discard` 的行为拿不到"这张牌是不是强化版"这个信息（原版【哨兵】要的正是它），
所以数据包里写不出"强化时多给能量"这种效果。

---

## 5. 调试与校验

```
/reload                 原版指令：重载数据包（卡牌注册表会一起重载，并重新灌进卡池）
/stc card list          看当前有多少张卡来自数据包
/stc card info <id>     看某张卡**现在**的定义（会标出"代码内置 / 数据包"）
/stc card behaviors     列出全部可用行为
/stc card validate      重新校验并报告问题（有问题的卡会被跳过，其余照常生效）
/stc card reload        手动重新灌一次
```

JSON 写错（行为 id 不认识 / 参数个数不对 / `type`、`target`、`rarity`、`character` 写错）时：

* **不会崩游戏**：那张卡被**跳过**，控制台会打印原因，其余卡照常生效；
* `/stc card validate` 会把原因用聊天栏列出来。

---

## 6. 已知限制（写在前面省得踩）

1. **只能组合行为表里已有的行为**，不能写脚本；
2. **覆盖是整张覆盖**：不写 `upgrade` 就没有强化版；`x_cost` / `generated` / `multiplayer` /
   `max_upgrades` 不写会**继承内置卡**（新卡则按"没有"处理）；
3. **多个数据包改同一张卡时，加载顺序靠后（字母序更靠后 / 更高优先级）的生效** ——
   原版注册表的规则，没有额外仲裁；
4. **`on_exhaust` / `on_discard` 看不到强化版**（见 4.3）；
5. **多档强化卡（【灼热打击】）是代码特例**：数据包可以覆盖它的基础定义，但"任意次强化"的档位
   仍然由代码算，`max_upgrades` 改不动它；
6. **客户端必须装同一个 mod**（注册表是同步注册表，原版要求两端都有）；
7. **改卡不影响已经存档的卡牌实例**：档案里存的是 id + 强化次数，定义每次都重新查表；
8. 内置的 10 张试点 JSON 与代码内置卡是**逐字段 + 打一场**比对过的（自检 `CardRegistryCheck`），
   所以**改内置卡的数值时必须同步改对应 JSON**，否则自检会红。
