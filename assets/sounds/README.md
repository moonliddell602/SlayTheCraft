# 声音（sounds/）

> **v0.4.6.6 起：BOSS 音乐「放文件就生效」，不用改代码。**
> 唯一硬性要求：文件必须是 **Ogg Vorbis（`.ogg`）**。

## BOSS 音乐（`boss/floor_1|2|3/`）

按下面的结构放好，然后跑一次导入脚本就行：

```
assets/sounds/boss/floor_1/floor_1.ogg   ->  assets/stc/sounds/boss/floor_1/floor_1.ogg
assets/sounds/boss/floor_2/floor_2.ogg
assets/sounds/boss/floor_3/floor_3.ogg
```

```powershell
.\tools\import-assets.ps1          # 先预览
.\tools\import-assets.ps1 -Apply   # 真的复制
```

模组自带的 `assets/stc/sounds.json` 已经定义好 1-3 层的事件：

```json
{ "boss_music_floor_1": { "sounds": ["stc:boss/floor_1/floor_1"], "stream": true } }
```

**第 4 层及以上**：事件名约定是 `stc:boss_music_floor_<层号>`。
在工作区 `assets/sounds/sounds.json` 里照抄一条、把数字改掉，
再跑导入脚本 —— 脚本会把它放到正确的 `assets/stc/sounds.json`。

当前 1-3 层音乐**已经随包发布**（Ogg Vorbis，`stream: true`）；
要换成自己的曲子，直接覆盖同名 ogg 即可。

### 三个必须知道的坑

| 坑 | 说明 |
| --- | --- |
| mp3 / wav / m4a / flac | **放不出来**。Minecraft 只支持 Ogg Vorbis。转换：`ffmpeg -i in.mp3 -c:a libvorbis -q:a 6 out.ogg` |
| `sounds.json` 放错位置 | 必须是 `assets/stc/sounds.json`；放进 `assets/stc/sounds/` 里 = 所有声音事件全部失效 |
| 忘了 `"stream": true` | 长音频不流式加载会拖慢进游戏（自带的 sounds.json 已经写好） |

导入脚本会自动跳过后缀不对的音频并**红字提醒**（不会静默复制）；
以 `_` 开头的目录（如 `_originals/`，放转换前的 mp3 原件）不导入也不报警。

### 想用自己的事件名 / 原版音乐

```java
StcApi.registerBossMusic(1, ResourceLocation.fromNamespaceAndPath("mypack", "my_boss_theme"));
StcApi.registerBossMusic(3, ResourceLocation.withDefaultNamespace("music_dragon"));
```

登记优先于约定；没配音乐的层完全不动原版音乐。

## 音效（`sfx/`）

普通音效（开战、锻造、抽牌、球激发…）。放进 `sfx/` 并告诉我用在哪，
我来定义声音事件并接线。建议 0.2-2 秒，音量正常化，别爆音。

## 当前音效来源（暂时都用原版）

| 时机 | 现在用的 |
| --- | --- |
| 攻击 / 暴击 | `minecraft:entity.player.attack.crit` |
| 上格挡 | `minecraft:item.armor.equip_iron` |
| 受击 | `minecraft:entity.player.hurt` |
| 完全格挡 | `minecraft:item.shield.block` |
| 充能球伤害 / 格挡 | `minecraft:block.amethyst_block.chime`、`minecraft:entity.lightning_bolt.impact` |
| 村民职业工作音（卡牌大师） | `minecraft:entity.villager.work_cartographer` |