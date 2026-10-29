package com.slaythecraft.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * 战斗内的"信息页"标记（v0.4.2）。
 *
 * <p>战斗界面是**自动弹出**的（{@code StcClientSetup} 每个 tick 检查"该显示出牌界面"就 setScreen），
 * 所以在战斗里打开别的页面时，必须让它知道"这是战斗的子页面，不要把我顶掉"。</p>
 */
public interface BattleInfoScreen {

    /** 子页面关闭后要回到的界面（一般是战斗界面）。 */
    Screen backScreen();
}
