BOSS 音乐目录（v0.4.6.6）
=========================

本模组**不自带**任何 BOSS 音乐（避免把版权音频打进包里），
但已经把"放对文件就响"的约定接好了：**不需要写任何代码**。

目录约定
--------
    assets/stc/sounds/boss/floor_1/floor_1.ogg     <- 第 1 层
    assets/stc/sounds/boss/floor_2/floor_2.ogg     <- 第 2 层
    assets/stc/sounds/boss/floor_3/floor_3.ogg     <- 第 3 层

文件名保持 floor_<层号>.ogg，子目录名保持 floor_<层号>，模组会自己找到它。
对应的声音事件（模组自带的 assets/stc/sounds.json 已经写好 1-3 层）：
    stc:boss_music_floor_1 / stc:boss_music_floor_2 / stc:boss_music_floor_3

第 4 层及以上：事件名约定 = stc:boss_music_floor_<层号>，
在 assets/stc/sounds.json 里照抄一条、把数字改掉就行。

      !!! 音频必须是 Ogg Vorbis（.ogg） !!!
      Minecraft 只认 ogg。mp3 / wav / m4a / flac 放进去**不会响**，
      而且不会有任何报错 —— 这是"我放了音乐怎么没声音"最常见的原因。
      转换：ffmpeg -i 原文件.mp3 -c:a libvorbis -q:a 6 目标.ogg

      !!! sounds.json 的位置 !!!
      必须是  assets/stc/sounds.json
      不能是  assets/stc/sounds/sounds.json     <- 放这里 = 所有声音事件全部失效

想用别的文件名 / 别的命名空间
-----------------------------
不用改文件名，也可以显式登记（登记优先于上面的约定）：

    StcApi.registerBossMusic(1, ResourceLocation.fromNamespaceAndPath("stc", "boss/floor_1/theme"));

也可以直接绑定**原版**声音事件（零资源即可生效）：

    StcApi.registerBossMusic(3, ResourceLocation.withDefaultNamespace("music_dragon"));

行为
----
    * 战斗快照里带"这一战是不是 BOSS 战"和"当前层号"；
    * 客户端在 BOSS 战开始的瞬间循环播放该层音乐，战斗结束立刻停止；
    * 没配音乐的层完全不动原版音乐；
    * 声音事件不存在时只在日志里警告一次（不会崩、也不会静默播错东西）。