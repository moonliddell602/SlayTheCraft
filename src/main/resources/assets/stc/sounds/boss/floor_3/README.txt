第 3 层 BOSS 音乐目录（v0.4.6.6）
=====================================

把这一层的音乐放成：

    assets/stc/sounds/boss/floor_3/floor_3.ogg

就完成了 —— 模组自带的声音事件 stc:boss_music_floor_3 会直接找到它，**不用写代码**。

    * 只支持 Ogg Vorbis（.ogg）：mp3 / wav 放进来不会响，也不报错。
      转换：ffmpeg -i in.mp3 -c:a libvorbis -q:a 6 floor_3.ogg
    * 想用别的文件名 / 别的命名空间：StcApi.registerBossMusic(3, <你的声音事件>)
    * 建议 1-3 分钟、可无缝循环；sounds.json 里已有 stream=true（流式加载，不占内存）。

完整说明见上一级目录的 README.txt。