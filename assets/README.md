# 素材工作区（`assets/`）

这里是你**放素材的地方**：卡牌插画、贴图、模型、音乐、音效、字体。
你只要把文件丢进对应子文件夹（按下面的命名规则），告诉我一声，我就能把它们接进游戏并打进 jar。

> ⚠️ 注意区分两个 `assets`：
> * **本目录**（项目根目录的 `assets/`）= **素材工作区**，不参与打包，放 PSD / 大图 / `.bbmodel` 都无所谓。
> * `src/main/resources/assets/stc/` = **打包目录**，只有进到这里的东西才会被打进 jar。
>   不想等我来同步，也可以直接把成品放进打包目录（路径见每张表里的「打包后位置」）。

---

## 1. 目录 → 打包位置对照

| 你放在这里 | 文件类型 | 打包后位置（`src/main/resources/` 下） | 命名规则 |
| --- | --- | --- | --- |
| `cards/` | `.png` | `assets/stc/textures/card/<卡牌路径>.png` | 用**卡牌 id 的路径**命名，例如 `strike.png`（打击）、`arrow_rain.png`（箭雨）；本体以外的卡用你自己的命名空间 |
| `textures/items/` | `.png` | `assets/stc/textures/item/<名字>.png` | 16×16 或 32×32；例如 `battle_standard.png` |
| `textures/blocks/` | `.png` | `assets/stc/textures/block/<名字>.png` | 16×16；例如 `spire_gate.png`、`card_recycler.png` |
| `textures/gui/binder/` | `.png` | `assets/stc/textures/gui/binder/<名字>.png` | **收纳包界面皮肤**：`background.png`（整图拉伸）/ `button.png`、`button_hover.png`、`button_disabled.png`（九宫格，四角各 3 像素）/ `avatar_<角色>.png`、`avatar.png`（22×22 方框）—— 详见 `assets/textures/gui/README.md` |
| `textures/gui/`（其它子目录） | `.png` | `assets/stc/textures/gui/<名字>.png` | 战斗界面 / HUD 等贴图（需要我先接线，见 §3） |
| `textures/entity/villager/profession/` | `.png` | `assets/stc/textures/entity/villager/profession/<职业>.png` | 64×64 村民皮肤；目前是 `card_master.png`（卡牌大师） |
| `textures/status/` | `.png` | `assets/stc/textures/status/<状态>.png` | 状态 / 充能球图标（需要我先接线，见 §3） |
| `models/item/` | `.json` | `assets/stc/models/item/<名字>.json` | 物品模型；`layer0` 指向 `stc:item/<名字>` 即用上面的贴图 |
| `models/block/` | `.json` | `assets/stc/models/block/<名字>.json` | 方块模型（通常是 `cube_all` + `all` 指向 `stc:block/<名字>`） |
| `sounds/boss/floor_1|2|3/` | `.ogg` | `assets/stc/sounds/boss/floor_<层>/floor_<层>.ogg` | **每层 BOSS 音乐**：**放好就生效，不用写代码**（事件名约定 `stc:boss_music_floor_<层>`，模组自带 `sounds.json` 已经定义 1-3 层，1-3 层音乐已随包发布）。**只支持 Ogg Vorbis**，mp3/wav 不会响也不报错 |
| `sounds/sounds.json` | `.json` | `assets/stc/sounds.json`（**根目录**，不在 `sounds/` 里！） | 声音事件表；新层照抄一条、把数字改掉即可（导入脚本会自动放到正确位置） |
| `sounds/_originals/` | 任意 | **不导入** | 转换前的 mp3 原件、参考素材都丢这里，脚本不会复制也不会报警 |
| `sounds/sfx/` | `.ogg` | `assets/stc/sounds/sfx/<名字>.ogg` | 自定义音效（需要我先接线，见 §3） |
| `relic/` | `.png` | `assets/stc/textures/relic/<遗物 id>.png` | 遗物图鉴的图标位（**可选**：有图就画，没图就只显示名字） |
| `fonts/` | `.ttf` / `.otf` | `assets/stc/font/<名字>.ttf` | 中文字体（需要我先接线，见 §3） |
| `lang/` | `.json` | 合并进 `assets/stc/lang/zh_cn.json` / `en_us.json` | 只要给我「键 → 文案」，我来合并 |

`.bbmodel`（Blockbench 工程）请放在 `models/` 下任意位置 —— `build.gradle` 已经显式排除
`**/*.bbmodel`，不会被误打进 jar。

---

## 1.5 模组图标与 pack.mcmeta（v0.4.6.7）

| 位置 | 说明 |
| --- | --- |
| `src/main/resources/icon.png` | **模组图标槽位**（128×128 PNG）。现在是占位图（金尖塔 + STC）；换图标只要覆盖这张文件重新打包，`neoforge.mods.toml` 里的 `logoFile` 已经指好 |
| `src/main/resources/pack.mcmeta` | 资源包/数据包声明（1.21.1：资源 34、数据 48）。已注册，一般不用动 |

这两个文件**不在** `assets/` 工作区里（它们属于打包目录根），要改直接改上面这两个路径。

## 2. 怎么把素材送进游戏 / jar

1. 放进本目录对应子文件夹（可以带中间目录，我会保留结构）；
2. 告诉我一句「素材放好了」，或者自己跑同步脚本：

   ```powershell
   # 预览（默认 dry-run，只打印会复制什么）
   .\tools\import-assets.ps1
   # 真的复制过去
   .\tools\import-assets.ps1 -Apply
   ```

   脚本的护栏：音频只搬 `.ogg`，mp3/wav/m4a/flac 会**跳过并红字提醒**
   （Minecraft 只认 Ogg Vorbis：`ffmpeg -i in.mp3 -c:a libvorbis -q:a 6 out.ogg`）；
   `sounds/sounds.json` 会被搬到 `assets/stc/sounds.json`（**不是** `sounds/` 里面）；
   以 `_` 开头的目录不导入。

3. 我这边会照 §3 的表决定「直接可用」还是「需要接线」（改模型 / 加 API / 接线音效），
   然后 `.\gradlew.bat build` 出 jar 给你。

---

## 3. 哪些位置现在就能用，哪些需要我先接线

| 素材 | 现状 |
| --- | --- |
| **卡牌插画** | ✅ **已接线**：把图片按 §1 的规则命名就会出现在战斗手牌（卡面上半部分）、卡牌详情页、卡牌图鉴、牌堆列表 |
| **BOSS 音乐** | ✅ **已接线（v0.4.6.6 起零代码）**：`sounds/boss/floor_<层>/floor_<层>.ogg` 放好就生效；想换文件名 / 命名空间再用 `StcApi.registerBossMusic(层号, 事件id)`。BOSS 战开始循环播放、结束立刻停 |
| **卡牌大师村民皮肤** | ✅ **已接线**：替换 `card_master.png` 即可（现在用的是沙漠村民贴图） |
| **物品 / 方块贴图** | ⚠️ 一行改动即可：目前【战斗军旗】用原版旗帜图案、【尖塔入口】【卡牌回收站】用原版方块贴图。给我图，我把模型里的 `layer0` / `all` 指过去 |
| **收纳包界面皮肤**（背景 / 按钮 / 头像） | ✅ **已接线**（v0.4.6.5）：放 `textures/gui/binder/` 下那几张图就生效，缺图自动退回程序化画法；也可以用 `StcApi.registerBinder*` 指定 |
| **其它界面贴图**（战斗界面、HUD、图鉴装饰） | ⚠️ 需要接线：这些仍是程序化绘制，给我图我按同一套约定加接口 |
| **状态 / 充能球图标** | ⚠️ 需要接线：现在复用原版状态效果图标（中毒 / 凋零 / 灼烧…）。给我图，我加一个「自定义图标表」，按状态名覆盖 |
| **音效 / 字体** | ⚠️ 需要接线：告诉我用在哪（例如开战、锻造、抽牌）我再接 |

---

## 4. 建议的素材尺寸（避免糊 / 避免过大）

| 用途 | 建议尺寸 | 说明 |
| --- | --- | --- |
| 卡牌插画 | 64×64 或 128×128（正方形） | 会被拉伸到卡面插画位（约 58×26 显示区域），正方形最省心 |
| 物品贴图 | 16×16（高清可 32×32） | 与其它原版物品一致 |
| 方块贴图 | 16×16 | 会被平铺 |
| 界面面板 | 按 1:1 像素画即可 | 我需要知道它要贴在哪块区域（卡面框 / 书页面板 / 按钮） |
| BOSS 音乐 | `.ogg`，建议 1–3 分钟并循环无缝 | 必须是 **Ogg Vorbis**（mp3/wav 不响也不报错）；`sounds.json` 里已带 `"stream": true` |
| 音效 | `.ogg`，0.2–2 秒 | 音量我做归一化，别爆音 |

---

## 5. 打包顺序（我会做的）

1. 同步素材 → `src/main/resources/assets/stc/…`；
2. 需要时改模型 / `sounds.json` / 代码接线；
3. `.\gradlew.bat compileJava processResources` 确认资源进得去；
4. `.\gradlew.bat build` → `dist/stc-<版本>.jar`；
5. 跑一遍自检（引擎 + 全部套件）确保没打坏别的东西。

---

## 6. 素材清单（可选，但很有帮助）

如果你一次给很多图，写一份 `assets/素材清单.md` 告诉我每个文件是什么、用在哪，我能一次接完。
最简单的格式：

```
cards/strike.png      —— 打击的插画
textures/items/battle_standard.png —— 战斗军旗（要我改成自绘贴图）
sounds/boss/floor_1/theme.ogg —— 第一层 BOSS 音乐
```
