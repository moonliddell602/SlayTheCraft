# 卡牌机制功能设计（卡牌收纳包 · 奖励 · 删卡 · 多人）

> 文档状态：**已确认**（第 16 节记录了 4 个已拍板决策；S0 正在实施）
> 本文只做设计，不包含实现。实现按第 15 节分阶段推进，每阶段都有可验收产物。

---

## 1. 范围

**本设计覆盖**

1. 玩家卡牌档案（以玩家为单位的数据：已拥有卡牌、金币、击杀进度、角色、待领奖励）
2. 【卡牌收纳包】物品：配方、Curios 饰品槽携带、UI、删除卡牌
3. 【卡牌回收站】方块、【卡牌商人】村民：另外两条删卡 / 购卡途径
4. 引导方块：以方块为媒介的对战入口、角色选择、多人准备
5. 卡牌奖励：击杀计数触发、右下角提示、三选一 / 跳过、BOSS 金色品质
6. 金币经济：掉落数值、删卡固定花费、商店消费
7. 队伍与多人：≤4 人同队校验，`/team` 与 FTB Teams 双通道（FTB 优先）
8. 与已有战斗系统的对接（战斗读取收纳包作为卡组；无包禁止开战）

**明确不在本设计内**（后续里程碑）

* 卡面美术资源、卡牌拖拽动画、遗物系统、地图节点 / 层数推进、卡牌升级（锻造）
* 牌组编成（从收纳包中挑选"出战牌组"）：本阶段**收纳包内全部卡牌 = 出战牌组**

---

## 2. 术语

| 术语 | 含义 |
| --- | --- |
| 卡牌档案 `PlayerCardData` | 挂在玩家身上的持久化数据：卡牌列表、金币、击杀计数、角色、待领奖励 |
| 收纳包 `card_binder` | 物品。**只是钥匙**：决定玩家能否访问档案 / 能否开战，不承载数据 |
| 引导方块 `spire_gate` | 右键打开尖塔入口界面（选角色 / 组队 / 开战）的方块 |
| 卡牌回收站 `card_recycler` | 方块，花金币删卡 |
| 卡牌商人 | 村民职业，花金币买卡 |
| 击杀档位 | 同种类怪物的分级：普通 / 精英 / BOSS |

---

## 3. 系统总览

```
                 ┌────────────────────────────┐
   物品层         │ 卡牌收纳包(Curios charm 槽)  │  钥匙：无包 → 不能开战
                 └─────────────┬──────────────┘
                               │ 打开 UI / 校验
   数据层         ┌─────────────▼──────────────┐
                 │   PlayerCardData (Attachment)│  ← 以玩家 UUID 为准，多包同源
                 │  卡牌[] · 金币 · 击杀计数     │
                 │  角色 · 待领奖励              │
                 └───┬───────────┬───────────┬──┘
                     │           │           │
   入口层     引导方块       卡牌回收站     卡牌商人
              (开战/选角色)   (删卡-100)   (买卡-金币)
                     │
   战斗层      Encounter ← 读取玩家卡牌作为出战牌组
                     ▲
   奖励层      击杀计数达标 → 待领奖励 +1 金币 +N → 右下角提示 → UI 三选一
```

依赖方向：**数据层不依赖物品**（物品被销毁 / 复制不影响档案），奖励与商店只写数据层。

---

## 4. 依赖与版本落点（已核实）

| 依赖 | 结论 | 证据 |
| --- | --- | --- |
| MC / NeoForge | **需从 1.21 升到 1.21.1 + NeoForge 21.1.251** | NeoForge 版本接口：`filter=21.1.` → `21.1.251` |
| Curios（必需，饰品槽） | **9.5.1+1.21.1**（1.21.1 线：9.2.0 → 9.5.1） | Curios maven metadata；**元数据中不存在任何 `+1.21` 构建**，只有 `+1.20.6` 与 `+1.21.1` |
| FTB Teams（可选） | **2101.1.11**（1.21.1 线；另有 `2100.1.0` 对应 1.21） | FTB maven metadata |
| Curios 槽位分配方式 | 物品标签 `data/curios/tags/item/<槽位>.json`；自定义槽位用数据包槽位定义 + validator | [Curios 1.21.x 文档 · Assigning Slots](https://docs.illusivesoulworks.com/curios/items/assign-slots) |
| FTB Teams 队伍查询 | `FTBTeamsAPI.api().getManager().arePlayersInSameTeam(uuid1, uuid2)`、`getTeamForPlayer(ServerPlayer)` | [FTB-Teams 1.21.1/main · TeamManager](https://github.com/FTBTeam/FTB-Teams) |

> **这是本设计里唯一的地基级风险**：留在 1.21 就没有 Curios 9.x（1.21 线根本没有构建），而"用 Curios 饰品槽携带收纳包"是你明确指定的核心交互。见第 16 节 Q1。

---

## 5. 数据模型：玩家卡牌档案

### 5.1 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `cards` | `List<CardInstance>` | 已拥有卡牌；`CardInstance = { id: ResourceLocation, upgrades: int }`，允许重复 |
| `gold` | `int` | 金币（0 起步，可配置初始值；测试用指令发放） |
| `character` | `ResourceLocation` | 当前角色，未选择时为 `null` |
| `killProgress` | `Map<ResourceLocation /*实体类型*/, Integer>` | 每种怪物的累计击杀数（计数到阈值即清空该条目重新累计） |
| `pendingRewards` | `int` | 待领卡牌奖励次数（右下角提示的计数来源） |
| `stats` | `{ kills, battlesWon, battlesLost, cardsDeleted }` | 只读统计，供 UI 显示与后续成就 |

### 5.2 存储与生命周期

* 使用 **NeoForge `AttachmentType<PlayerCardData>`** 挂在 `Player` 上（1.21 的官方推荐做法，替代旧 Capability）。
* 序列化：`Codec` + `CompoundTag`；`copyOnDeath(true)` —— 死亡不掉档（可配置改为清空）。
* 同步：`AttachmentType.Builder#sync(...)`，HUD 与 UI 客户端可读（金币 / 待领奖励角标）。
* 与物品彻底解耦：**做 10 个包也是同一份档案**；包丢了档案还在（但要重新做包才能开战）。

### 5.3 卡牌定义：数据包注册表（已定）

卡牌定义走 **datapack registry**（`DataPackRegistryEvent.NewRegistry` 注册 `stc:card`），
因此整合包 / 玩家可以直接增删改卡池，不需要重新编译。

* 注册路径：`data/<namespace>/stc/card/<id>.json`，内置卡随模组提供在 `data/stc/stc/card/`。
* 数据包注册表**由服务端自动同步到客户端**，客户端 UI 无需额外同步卡牌定义。
* 存档只存 `CardInstance = { id, upgrades }`；文案不进 JSON，走 lang key（`card.stc.strike.name` / `.desc`），中英双语。

**CardDefinition 字段**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| （注册 id） | `ResourceLocation` | `stc:strike`，稳定标识 |
| `cost` | int | 能量费用 |
| `type` | `attack / skill / power / status / curse` | 沿用战斗引擎模型 |
| `target` | `enemy / all_enemies / self / none` | 取目标方式 |
| `rarity` | `common / uncommon / rare / gold` | `gold` 为 BOSS 奖励专属 |
| `character` | `ResourceLocation?` | 归属角色卡池；缺省 = 公共池 |
| `effects` | `CardEffectSpec[]` | 声明式效果列表（见 5.4） |

### 5.4 声明式卡牌效果（数据包配套）

现有 `CardEffect` 是 Java lambda，数据包用不了。做法：**效果类型在代码里注册，参数由数据包给**，
用 `Codec.dispatch` 按 `type` 分派：

```json
{
  "cost": 1,
  "type": "attack",
  "target": "enemy",
  "rarity": "common",
  "character": "stc:ironclad",
  "effects": [
    { "type": "stc:damage", "amount": 6 },
    { "type": "stc:block", "amount": 5 },
    { "type": "stc:apply_status", "status": "vulnerable", "turns": 2, "to": "target" },
    { "type": "stc:draw", "cards": 1 }
  ]
}
```

首批效果类型（覆盖现有 7 张卡后仍可扩展）：

| type | 参数 | 对应已有卡牌能力 |
| --- | --- | --- |
| `stc:damage` | `amount`, 可选 `times` | 打击 / 重击 / 顺劈斩 / 铁斩波 / 柄击 |
| `stc:block` | `amount` | 防御 / 铁斩波 / 不痛不痒 |
| `stc:apply_status` | `status`(strength/weak/vulnerable), `turns`/`amount`, `to`(self/target/all_enemies) | 重击的易伤 |
| `stc:draw` | `cards` | 柄击 / 不痛不痒 |
| `stc:gain_energy` | `amount` | （预留） |
| `stc:heal` | `amount` | （预留） |

* 代码侧：`CardEffectType` 接口（`Codec` + `apply(spec, ctx, self, target)`）注册进 `StcRegistries.CARD_EFFECT`。
* 引擎侧：`CombatContext` 只需按 `List<CardEffectSpec>` 依次执行，不再依赖 lambda——现有 39 项自检改为用内置 JSON 卡跑同一批断言，作为迁移回归基线。
* 找不到的效果类型 / 缺失的卡牌 id：打日志跳过该条，不让整场战斗崩掉。

---

## 6. 物品 / 方块 / 村民清单

### 6.1 卡牌收纳包 `stc:card_binder`

* 类型：普通物品（`stacksTo(1)`），**Curios 饰品**，占用 `charm` 槽（Stage 1 用标签方式，零风险；后续可换成专用 `stc:binder` 槽位）。
* 配方（工作台，3×3，从左到右 / 从上到下）：

```
皮革  皮革  皮革
线    羊毛  线
皮革  皮革  皮革
```
* 交互：
  * 右键 → 打开收纳包 UI
  * 装备在 Curios 槽 → 允许开战（右键引导方块）
* **约束：没有携带收纳包（Curios 槽位无包）时，引导方块拒绝开战**并给出提示。
* Curios 缺失时的降级（可配置）：依次检测 副手 → 主手 → 背包，保证模组能独立跑。

### 6.2 引导方块 `stc:spire_gate`

* 右键 → 打开「尖塔入口」界面：角色选择 / 队伍准备 / 开始对战 / 查看卡组。
* 存在意义：**不劫持原版玩法**，一切尖塔内容都由这个方块主动触发。
* 多人：同队玩家（≤4）在方块附近各自右键加入准备位；发起者点「开始」才进入战斗。

### 6.3 卡牌回收站 `stc:card_recycler`

* 右键 → 打开删卡界面：列出卡牌 → 选中 → 二次确认 → 扣 **100 金币** → 删除。
* 与 UI 内删卡**同一套服务端逻辑、同一价格、同一确认组件**（避免两套代码分叉）。

### 6.4 卡牌商人（村民职业 `stc:card_merchant`）

* 通过 `PlayerInteractEvent.EntityInteract` 拦截：右键该职业村民 → 打开「卡牌商店」界面。
* 商店内容：3 张随机卡（可刷新，刷新收费可配置）+ 价格（例如 75 / 150 / 300，按稀有度）。
* 金币是**档案里的数字**，不是物品，所以无法走原版交易界面 → 必须自定义菜单（这点在设计上显式记录）。

---

## 7. 角色设计（先 3 个，不急于做卡池内容）

| 角色 | HP | 能量 | 起始牌组（本阶段可直接用现有卡凑） | 差异化定位 |
| --- | --- | --- | --- | --- |
| 铁甲战士 `ironclad` | 80 | 3 | 打击×5 · 防御×4 · 重击×1 | 高血、直接伤害与易伤 |
| 静默猎手 `silent` | 70 | 3 | 打击×5 · 防御×5 · 弱化牌×1 | 灵活、上虚弱与连击 |
| 故障机器人 `defect` | 75 | 3 | 打击×4 · 防御×4 · 充能牌×2（占位） | 充能球机制（内容后补） |

* 角色数据同样是**代码定义 + 后续可数据包化**；本阶段只需能选、能切换、能决定开局 HP/能量/起始牌组。
* 切换角色：回引导方块界面重选（战斗中禁止切换；切换后把角色起始牌组写入档案，已拥有卡牌是否清空 → 见 Q4 之外的默认：**保留已拥有卡牌，仅重置起始牌组标记**，可配置为"换角色清档"）。
* HP 结算：战斗外按角色最大 HP 修正玩家属性值（`Attribute.MAX_HEALTH`），进战斗用当前 HP。

---

## 8. UI 设计

### 8.1 收纳包 UI（核心界面）

```
┌─ 卡牌收纳包 ────────────────────────── 金币 240 ─┐
│ [卡牌] [奖励 ②] [统计]                          │
│ ┌────┐┌────┐┌────┐┌────┐┌────┐                 │
│ │打击││打击││防御││重击││弱化│  ← 卡片网格      │
│ │ 1  ││ 1  ││ 1  ││ 2  ││ 1  │    点击选中     │
│ └────┘└────┘└────┘└────┘└────┘                 │
│ ┌────┐┌────┐                                    │
│ │…   ││…   │   共 12 张                         │
│ └────┘└────┘                                    │
│ ──────────────────────────────────────────────  │
│ 选中：打击  造成 6 点伤害                        │
│ [ 删除卡牌 ] 费用 100 金币       [ 关闭 ]        │
└─────────────────────────────────────────────────┘
```

* **卡牌**页：网格展示全部已拥有卡牌（悬停显示完整描述）、金币余额、选中卡 → 「删除卡牌」。
* **奖励**页：待领奖励列表（`待领 ②`），点「领取奖励」→ 进入三选一界面。
* **统计**页：累计击杀 / 胜利 / 失败 / 已删卡数。
* 删除流程（二次确认）：点删除 → 弹确认框「消耗 100 金币删除【打击】？此操作不可撤销」→ 确认 → 服务端校验金币与卡牌存在性 → 扣钱删卡 → 刷新。

### 8.2 奖励三选一

```
┌─ 选择一张卡牌 ─────────────────────────────┐
│  ┌──────┐   ┌──────┐   ┌──────┐            │
│  │ 铁斩波│   │ 顺劈斩│   │ 柄击 │  ← BOSS 为金色边框
│  │ 1 能量│   │ 1 能量│   │ 1 能量│           │
│  │ …效果… │   │ …效果… │   │ …效果… │          │
│  └──────┘   └──────┘   └──────┘            │
│            [ 跳过 ]                         │
└─────────────────────────────────────────────┘
```
* 三张随机（角色卡池 + 公共池，按稀有度权重）；BOSS 战必出 **3 张金色品质**。
* 选择 → 立即写入档案 `cards` → `pendingRewards - 1`；跳过 → 只减计数不写卡。

### 8.3 快捷键与打开方式

| 方式 | 说明 |
| --- | --- |
| 快捷键（默认 `B`，**可改**） | 打开收纳包 UI；客户端按键 → 服务端校验 → 打开菜单 |
| 右键收纳包物品 | 同上 |
| 右键引导方块 | 打开尖塔入口（含卡组预览入口） |
| 右键回收站 / 商人 | 打开对应界面 |

### 8.4 右下角奖励提示（HUD）

* 击杀达标时在**屏幕右下角**绘制提示条：`新卡牌奖励可用（B）`，带淡入淡出与停留时间。
* 实现：`RegisterGuiLayersEvent` 注册一层 HUD（NeoForge 21.1），数据读档案的 `pendingRewards`（已同步到客户端）。
* 同时给一次聊天栏/提示音提醒（可配置），避免玩家没看到。

### 8.5 视觉占位策略

本阶段**不引入美术资源**：卡片用程序化绘制（圆角矩形 + 稀有度配色边框 + 名称 + 费用圆点 + 描述文字），后续替换为卡面贴图时只改渲染层，不动数据结构。

---

## 9. 奖励与金币机制

### 9.1 怪物分级

用**数据包标签**定义档位（整合包可自行增删，不需要改代码）：

```
data/stc/tags/entity_type/mob_tier/normal.json   → 未列入 elite/boss 的敌对生物（默认）
data/stc/tags/entity_type/mob_tier/elite.json    → 卫道士、掠夺者、女巫、末影人、岩浆怪…
data/stc/tags/entity_type/mob_tier/boss.json     → 末影龙、凋灵、监守者
```

### 9.2 击杀计数与触发

* 监听 `LivingDeathEvent`：取最后一击来源玩家（`lastHurtByPlayer`）；无玩家击杀则归给**击杀时附近同队玩家**（可配置为不计数）。
* 计数键：`(玩家, 实体类型)` —— 即你要求的"**同种类**怪物"分别累计。
* 达到阈值 → 该类型计数清零、`pendingRewards + 1`、金币增加、右下角提示。

| 档位 | 需击杀数量 | 金币 | 卡牌奖励 |
| --- | --- | --- | --- |
| 普通 | 10 | 16 | 3 选 1（可跳过） |
| 精英 | 4 | 40 | 3 选 1（可跳过） |
| BOSS | 1 | 60 | 3 选 1，**必为金色品质** |

> 全部数值进配置文件；"金币与卡牌奖励同一触发"是默认行为，另一种方案见 Q3。

### 9.3 金币

* 唯一来源：击杀达标（+ 指令 / 配置初始值 / 后续任务系统）。
* 唯一去向：删卡（固定 100）、商店购卡。
* 金币是档案里的数字，**不做成物品**（避免复制与背包占用）。

---

## 10. 队伍与多人（≤4）

### 10.1 同队判定优先级

```
1. 服务器装了 FTB Teams 且双方都有队伍   → 以 FTB 队伍为准（最高优先级，直接返回结果，不再看 /team）
2. 否则看原版计分板队伍（/team）         → 同队即通过
3. 都没有队伍                            → 只有自己算自己人
```

* 封装为 `TeamResolver.areTeammates(ServerPlayer a, ServerPlayer b)`，全模组只此一处判定。
* FTB 走**软依赖**：`ModList.get().isLoaded("ftbteams")` + 独立 compat 类，编译期用 `compileOnly` 引入 API。

### 10.2 多人玩法规则

| 规则 | 设计 |
| --- | --- |
| 人数 | 1–4 人，超过 4 人时后来者不能加入 |
| 加入方式 | 同队玩家右键同一引导方块进入准备位 |
| 开始条件 | 发起者点「开始」；所有人都在方块附近（可配置半径） |
| 战斗结构 | 共享一场 Encounter：敌人共用，**玩家各自拥有能量 / 手牌 / 抽牌堆 / 格挡 / 状态**；回合顺序 = 玩家1 → 玩家2 → … → 玩家N → 敌人回合 |
| 敌人目标 | 单体意图随机指向一名存活玩家（意图文案显示目标名）；群体意图打所有人 |
| 奖励 | 每个参与者各自计数、各自拿金币与奖励（BOSS 每人一次三选一） |
| 掉线 / 死亡 | 单人倒下即退出战斗（不再被选为目标）；全员倒下 = 团灭 |
| 中途退出 | 玩家退出后其手牌等状态保留在 Encounter 内直到战斗结束（便于重连） |

---

## 11. 与战斗系统的对接

* **卡组来源**：开战时读取该玩家档案的 `cards` → 转为引擎的牌组（现有 `CardLibrary.starterDeck()` 退为默认/调试用）。
* **无包禁战**：引导方块与命令两条入口都做 `BinderResolver.hasBinder(player)` 校验。
* **角色影响**：开局 HP 用玩家当前血量；能量上限与起始牌组由角色决定（`CombatRules` 从角色档案取，而非全局固定 3 点）。
* **引擎改造**：`CombatContext`（单玩家）→ `Encounter`（1..4 玩家 + 敌人组）。单人是 N=1 的特例，现有 39 项自检全部保留为回归基线，并新增多人回合顺序 / 目标选择的自检项。

---

## 12. 边界与异常

| 场景 | 处理 |
| --- | --- |
| 反复造包 | 档案以玩家为准，包只是钥匙，**不复制数据** |
| 包被丢 / 被烧 | 档案保留；不能开战，提示"需要携带卡牌收纳包" |
| 玩家死亡 | 档案默认不丢（`copyOnDeath`）；战斗结束并解锁敌人 |
| 换维度 / 离屏 | 战斗随玩家位置判定；敌人丢失超过 N 秒按"敌人逃逸"结束 |
| 并发点击 | 删除 / 领取 / 购买一律服务端二次校验（金币、卡牌存在性、待领次数），失败给明确提示 |
| Curios 未安装 | 降级为背包检测（可配置 `requireCurios`） |
| FTB Teams 未安装 | 回落到 `/team` |
| 语言 | 所有文案走 lang key，中英双语 |

---

## 13. 配置项（追加到 `config/stc-common.toml`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `startingGold` | 0 | 初始金币 |
| `cardDeleteCost` | 100 | 删卡固定费用（两条途径共用） |
| `rewardThresholdNormal` / `Elite` / `Boss` | 10 / 4 / 1 | 击杀达标数量 |
| `goldNormal` / `Elite` / `Boss` | 16 / 40 / 60 | 达标金币 |
| `rewardChoices` | 3 | 三选一的候选数量 |
| `bossRewardGoldenOnly` | true | BOSS 必出金色 |
| `maxPartySize` | 4 | 同队人数上限 |
| `requireCurios` | true | 是否强制 Curios 槽位携带 |
| `keepCardsOnDeath` | true | 死亡是否保留档案 |
| `switchCharacterClearsCards` | false | 换角色是否清空已拥有卡牌 |
| `merchantPriceCommon/Uncommon/Rare` | 75 / 150 / 300 | 商店价格 |

---

## 14. 技术实现要点

* **包结构**（在现有 `com.slaythecraft` 下扩展）：

```
data/      PlayerCardData, CardInstance, CharacterProfile, CardDefinition, StcAttachments
binder/    CardBinderItem, BinderResolver, CardBinderMenu, CardBinderScreen, BinderNetworking
reward/    KillTracker, RewardService, RewardMenu/Screen, RewardHudLayer, MobTierTags
economy/   GoldService, CardDeleteService（回收站与 UI 共用）
block/     SpireGateBlock, CardRecyclerBlock, + 对应 Menu/Screen
villager/  CardMerchantProfession, CardShopMenu/Screen, MerchantIntercept
team/      TeamResolver, FtbTeamsCompat
character/ CharacterRegistry, CharacterSelectionMenu/Screen
combat/    core（引擎，尽量不再碰 Minecraft 类型） + mc（接入层）
```

* **菜单与同步**：`AbstractContainerMenu` + 虚拟 `Slot`（只读展示卡牌）+ `clickMenuButton(id)` 承载动作；金币 / 待领奖励走已同步的 Attachment 供 HUD 直接读。
* **网络**：1.21.1 用 `RegisterPayloadHandlersEvent` + `PayloadRegistrar` 注册 `ServerboundOpenBinderPayload`（快捷键）等少量自定义包。
* **数据包资源**：卡牌定义（若选数据包方案）、怪物档位标签、Curios 槽位标签、配方、lang、模型。
* **回归基线**：现有 `CombatSelfTest` 39 项 + 新增多人/目标选择项，`/stc combat selftest` 继续可用。

---

## 15. 分阶段实施计划

| 阶段 | 内容 | 验收标准 | 状态 |
| --- | --- | --- | --- |
| **S0** | 升级 1.21.1 / NeoForge 21.1.251；接入 Curios 9.5.1（implementation）、FTB Teams 2101.1.11（compileOnly）；`javap` 校准两套 API | `runData` 加载成功且 mod 列表出现 curios；能编译引用 `CuriosApi` 与 `FTBTeamsAPI`；战斗自检 39/39 仍通过 | ✅ 已完成 |
| **S1** | `PlayerCardData` Attachment + 收纳包物品 + 配方 + Curios 槽位 + 金币 + 同步；调试指令 `/stc binder …` | 重登数据仍在；两个包显示同一份数据；`/stc binder gold 500` 生效 | ✅ 已完成（重登一项需游戏内验收，见下） |
| **S2** | 收纳包 UI（网格 / 金币 / 选中详情 / 删除二次确认）+ 快捷键 | 删除扣 100 且服务端校验生效；客户端显示与服务端一致 | ✅ 已完成（界面观感需游戏内验收） |
| **S3** | 引导方块 + 三角色选择 + 战斗读取档案卡组 + 无包禁战 | 无包右键方块被拒并提示；选角色后起始牌组来自档案 | ✅ 已完成（界面观感需游戏内验收） |
| **S4** | 击杀计数（分档标签）+ 金币 + 待领奖励 + 三选一/跳过 + 右下角 HUD | 击杀 10 只僵尸弹提示、领取入包、金币 +16；BOSS 出 3 张金色 | ✅ 已完成（界面观感需游戏内验收） |
| **S5** | 卡牌回收站方块 + 卡牌商人村民 + 商店 | 两处删卡都扣 100；商人可购卡并正确扣金币 | ✅ 已完成（界面观感需游戏内验收） |
| **S6** | 多人 ≤4：`TeamResolver`（FTB 优先）+ 多人 Encounter 回合 + 各自奖励 | 同队两人进入同一场战斗、不同队被拒；FTB 队伍优先于 `/team`；多人自检项通过 | ✅ 已完成（两人实机对战需游戏内验收） |

### S0 完成证据

* `compileJava` / `processResources` 在 MC 1.21.1 + NeoForge 21.1.251 上 BUILD SUCCESSFUL
* `runData` 运行期 mod 列表：`Curios API 9.5.1+1.21.1 (curios)`、`Minecraft 1.21.1`、`NeoForge 21.1.251`、`SlayTheCraft 0.2.0 (stc)`，Curios mixin 正常注入
* `TeamResolver` / `FtbTeamsCompat`（S6 的队伍判定地基）已提前落地，签名按真实 jar 校准
* `CuriosCompat`（饰品槽检测）已提前落地，`SlotResult.slotContext()/stack()`、`SlotContext.identifier()` 均已核对
* 顺手修掉 NeoForge 21.1 的弃用告警：`@EventBusSubscriber(bus = GAME)` → `@EventBusSubscriber`（GAME 是默认总线，该参数已标记待删除）
* 旧的战斗规则自检迁移前后均 **39/39 通过**

### S1 完成证据

代码：`data/`（档案 + Attachment + 初始化事件）、`binder/`（物品 + 携带判定 + 状态摘要 + 调试指令）、
资源（配方 / Curios `charm` 标签 / 模型 / 中英文 lang）。

* `PlayerCardData` 用 **AttachmentType**（`serialize(CODEC)` + `copyOnDeath()` + `sync(STREAM_CODEC)`），以玩家为单位，与物品解耦
* 档案序列化校验 `CardDataCodecCheck`：**26/26 通过**，覆盖 NBT 落盘往返、**客户端同步流往返**、
  删卡 / 金币不足拒付 / 待领奖励不可领成负数等边界
  （运行方式：`java -cp "build\classes\java\main;build\classes\java\test;<NeoForm classpath>" com.slaythecraft.data.CardDataCodecCheck`）
* 配方格式与 1.21.1 原版配方逐字对齐（`key`/`pattern`/`result.id`，皮革 `{"item":...}`、羊毛 `{"tag":"minecraft:wool"}`）
* `runData` 运行期：mod 列表正常，`BinderCommands` 与 `PlayerDataEvents` 自动挂到游戏总线，Attachment 注册无异常
* 开战门槛已接：`CombatManager.start` 在 `requireBinderToStart=true`（默认）时校验收纳包

**仍需游戏内验收**（无头环境无法覆盖）：重登后档案仍在、两个收纳包显示同一份数据、
收纳包放进 Curios `charm` 槽后可装备、工作台合成成功。步骤见 README 3.4。

### S2 完成证据

实现：`CardBinderMenu`（无物品槽的菜单 + `clickMenuButton` 动作）、`CardBinderScreen`（程序化绘制的卡片网格 /
金币 / 分页 / 选中详情 / 二次确认浮层）、`StcMenus`、`CardDeleteService`、`BinderOpener`、
`OpenBinderPayload` + `StcPayloads`、`StcClientSetup`（快捷键 B + 界面注册）。

* 删卡逻辑校验 `CardDeleteServiceCheck`：**17/17 通过**（余额不足/越界/负数索引都不动数据、按 id 只删一张、刚好够钱可删）
* 数据同步**零自定义数据包**：菜单里没有物品槽，卡片由程序化绘制，客户端直接读 S1 配好的 Attachment 同步；
  唯一的自定义包是"请求打开界面"（快捷键），且服务端会重新校验收纳包是否携带
* 服务端二次校验：`clickMenuButton` 里重新查档案、重新扣钱、重新判索引，客户端伪造按钮 id 无法白拿删卡
* 客户端/服务端隔离：客户端接线集中在 `com.slaythecraft.client`，主类用 `FMLEnvironment.dist` 守卫；
  已避开 21.1 标记待删除的 `@EventBusSubscriber(bus = …)`
* 编译通过；`runData` 运行期无异常（见下条）

**仍需游戏内验收**：界面布局与观感（我无法无头验证渲染）、快捷键 B 能打开界面、
确认框交互、删除后卡片即时消失。

### S3 完成证据

实现：`character/`（三角色档案 + 注册表 + 选择/切换纯逻辑 + 属性侧包装）、
`data/DeckBuilder`（档案 → 战斗牌组）、`gate/`（引导方块 + 入口菜单）、
`client/SpireGateScreen`；`CombatManager.start` 改为读取档案卡组与角色能量。

* 角色表：铁甲战士（80 HP / 3 能量 / 10 张）、静默猎手（70 / 3 / 12 张）、故障机器人（75 / 3 / 10 张）
* 为后两个角色的起始牌组补了 4 张卡：中和、生存、电击、冰球（充能球机制留待后续，先用普通效果占位）
* 角色与卡组校验 `CharacterServiceCheck`：**25/25 通过** —— 角色表自洽、起始牌组全部可翻译成真实卡牌、
  未知卡 id 被跳过并记录、**切换角色不重复发牌（防刷）**、`clearCards` 生效
* **无包禁战**：`CombatManager.start` 同时校验①是否携带收纳包 ②是否已选角色 ③档案里是否有可用卡牌 ④附近是否有敌人，
  四道门槛都给出可操作的提示
* 战斗中禁止切换角色（在入口菜单里拦截）
* 1.21.1 的方块交互用的是新 API `useWithoutItem`（持物右键走 `useItemOn`，不拦截），已按实际反编译源码确认
* 编译通过；`runData` 运行期无异常

**仍需游戏内验收**：入口界面观感、放置方块并右键打开、点角色名后提示与血上限变化（80/70/75）、
点「开始对战」后战斗用的是档案里的卡组而不是固定初始牌组。

### 关于"切换角色"的一条防刷规则

首次选某角色 → 发放它的起始牌组；切到**从未开过局**的另一个角色 → 发放它的起始牌组；
**切回已经开过局的老角色 → 只改当前角色，不再发牌**。因此最多只会累积 3 副起始牌组，
不可能靠来回切换无限刷牌。`switchCharacterClearsCards=true` 可改为"切到新角色时先清空卡牌"。

### S4 完成证据

实现：`combat/core/CardRarity` + `Card` 增补品质/归属字段、`CardLibrary` 标注品质与角色并新增 2 张金色卡、
`combat/mc/MobTiers`（实体类型标签分档）、`reward/`（抽取器 + 服务 + 击杀跟踪 + 奖励菜单 + 调试指令）、
`client/RewardScreen` + `client/RewardHudLayer`、收纳包界面新增「领取奖励」按钮、
`PlayerCardData` 的待领奖励改为**队列 + 已抽候选**（含 S1~S3 老存档迁移）。

* 奖励抽取校验 `RewardRollerCheck`：**26/26 通过** —— 角色池/公共池/金色池互不越界、
  候选不重复、金色奖励只出金色卡、池子不足按实际数量给、同种子可复现、
  待领奖励先进先出、**老存档 `pending_rewards` 计数能迁移成待领奖励**
* 角色校验新增"起始牌组的卡要么公共、要么归属该角色"：**26/26 通过**
* **候选在击杀达标时抽好并存档**：关掉界面再打开还是同样三张，读档/重连刷不出更好的结果
* 服务端二次校验：领取时重新读档案、判索引，坏候选（卡池变更）只跳过该次奖励
* 分档：`data/stc/tags/entity_type/mob_tier/{elite,boss}.json`（整合包可增删）；
  未收录且是敌对生物才算普通（打牛羊不产出奖励）；环境死亡（摔死/淹死）不计入，避免挂机刷奖
* 编译通过；`runData` 运行期无异常

**仍需游戏内验收**：右下角提示是否出现、三选一界面观感、点击领取后卡牌入包与金币到账、
BOSS 三张金卡、`/stc reward grant` 调试发放。

### S5 完成证据

实现：`station/CardRecyclerBlock`（回收站方块）、`merchant/`（卡牌商人实体 + 生成蛋 + 商店菜单/定价/货架）、
`network/ShopOffersPayload` + `client/ClientShopOffers`（货架下发）、`client/CardShopScreen`。

* **两条删卡途径共用同一段代码**：回收站打开的是和收纳包**同一个菜单类**
  （`CardBinderMenu.Mode.RECYCLER`），价格、服务端校验、二次确认完全一致——
  从结构上排除了"两条路径行为分叉"的可能
* 卡牌商人**继承原版村民**：外观就是村民（"特殊村民"），右键走 `mobInteract` 打开我们的商店，
  不注册兴趣点、不劫持服务器上的普通村民，渲染直接复用 `VillagerRenderer`（零美术成本）
* 商店货架是**菜单本地状态**：首个 tick 由服务端抽好并通过新增的 clientbound 包下发；
  买走一件自动补货，也可花金币刷新整店；价格按品质（75/150/300），**不卖金色卡**（BOSS 专属）
* 所有消费都走 `PlayerCardData.spendGold`（余额不足不扣钱），并在服务端二次校验
* 商店校验 `CardShopCheck`：**11/11 通过** —— 定价与品质一致、同店不重复、不卖金色卡、
  未选角色时只卖公共卡、要得比池子多时按实际数量给
* 编译通过；`runData` 运行期无异常

**仍需游戏内验收**：回收站方块外观与右键打开、两处删卡各扣 100、生成蛋召唤商人并右键开店、
购买扣金币且卡牌入包、刷新商品扣 25 金币。

### S6 完成证据

实现：`combat/core/PlayerState`（每名玩家独立的能量/手牌/抽牌堆/弃牌堆）、`CombatContext` 改造为
**1..4 人共享敌人**的遭遇战（单玩家构造与既有 API 原样保留）、`Enemy` 增加"意图目标"、
每回合能量下移到玩家状态（角色不同则能量不同）、`CombatManager` 把同一场战斗的所有参与者映射到同一个 Context
并校验"只有当前行动玩家能出牌/结束回合"、入口界面打开时给出参战预览。

* 引擎自检从 39 项扩展到 **68 项**：新增 29 项多人校验 —— 回合顺序（玩家1 → 玩家2 → 敌人 → 玩家1）、
  三人的能量/手牌/弃牌堆互相独立、敌人单体意图会预先指定目标并显示目标名、
  **目标倒下后自动改打幸存者**、倒下的玩家被跳过、**全员倒下才判负**、人数不影响胜利条件
* **既有 39 项单玩家自检在重构后原样通过**，正是这次引擎改造的回归基线
* 队伍判定接进开战流程：`TeamResolver`（FTB party 队伍优先 → 否则原版 `/team` → 否则仅自己）
* 开战门槛对**每名**参与者单独生效（收纳包 / 角色 / 可用卡牌），入口界面打开时收到"参战预览"：
  谁会被一起拉进来、谁因为什么（未携带收纳包 / 未选角色 / 卡组为空 / 已在别的战斗 / 距离太远）不能参战
* 编译通过；`runData` 运行期无异常

**与设计文档 §10.2 的一处偏差**（已在 README 标注）：原本写的是"同队玩家各自右键加入准备位，发起者再点开始"
（一个显式准备大厅）。本阶段改为**发起者开战时自动把半径内满足条件的同队队友拉进来**——
准备大厅需要额外的房间状态机、超时与并发处理，而自动纳入 + 参战预览已经能给出可玩的四人同屏体验，
也不会出现"有人没点准备导致战斗开不起来"。需要显式准备位可以在现有基础上加。

**仍需游戏内验收**：两名同队玩家各带收纳包与角色，一人开战 → 两人进入同一场战斗；
行动栏显示轮到谁（▶ 标记）；非当前行动玩家出牌提示"现在轮到 X 行动"；
一人倒下后另一人继续打完；FTB 队伍优先于 `/team`。

每个阶段结束都会给出：编译结果、可执行验收步骤、必要的自检项。

---

## 16. 已确认决策（含落地位置）

| # | 决策 | 结论 | 落地位置 |
| --- | --- | --- | --- |
| Q1 | 版本落点 | **升级到 MC 1.21.1 + NeoForge 21.1.251**，使用 Curios 9.5.1+1.21.1 | `gradle.properties`（`minecraft_version` / `neo_version` / `curios_version`）、`build.gradle` 依赖与仓库 |
| Q2 | 卡牌定义来源 | **数据包注册表**（`stc:card` DataPackRegistry + 声明式效果） | 见 5.3 / 5.4；代码侧新增 `StcRegistries`、`CardDefinition`、`CardEffectType` |
| Q3 | 金币节奏 | **与卡牌奖励同一触发**：达标才给（普通 16 / 精英 40 / BOSS 60） | 见 9.2；`KillTracker` 达标时同时 `pendingRewards+1` 与 `gold+=N` |
| Q4 | 多人奖励 | **每人独立计数与奖励** | 见 10.2；计数键为 `(玩家, 实体类型)`，BOSS 每人各一次三选一 |

已随之落地的构建变更：

* `neo_version=21.1.251`、`minecraft_version=1.21.1`、`mod_version=0.2.0`
* `implementation top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1`（仓库 `maven.theillusivec4.top`）
* `compileOnly dev.ftb.mods:ftb-teams-neoforge:2101.1.11`（仓库 `maven.ftb.dev/releases`，运行期软依赖）
* `neoforge.mods.toml`：`curios` 与 `ftbteams` 均声明为 **optional**，保证缺依赖时模组仍能加载（降级为背包检测 / 计分板队伍）
* Parchment 暂时停用（1.21.1 版本号在 JFrog 元数据取不到，只影响参数名可读性），需要时可取消注释
