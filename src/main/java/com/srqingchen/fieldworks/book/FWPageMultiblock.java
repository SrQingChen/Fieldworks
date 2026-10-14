package com.srqingchen.fieldworks.book;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.gui.components.Button;
import vazkii.patchouli.client.book.page.PageMultiblock;

/**
 * 场域工艺多方块书页(261013 规划 T5)：
 * 继承 Patchouli 官方多方块页(书内 3D 预览 PiP 沿用父类——其渲染路径无缺陷)，
 * 仅把"可视化"按钮的世界投影从 Patchouli 破损的
 * MultiblockVisualizationHandler(viewRotation 双重应用→投影反相移动, 261013 R4)
 * 换接到 {@link com.srqingchen.fieldworks.client.book.FWMultiblockGhost}。
 *
 * <p>JSON：{@code "type": "fieldworks:multiblock"} + {@code "multiblock_variant": "<结构名>"}；
 * 其余字段(multiblock/name/text/enable_visualize)与官方页同构。</p>
 */
public class FWPageMultiblock extends PageMultiblock {
    /** 结构变体名(giant_structures/&lt;variant&gt;.json)——本页投影的数据源。 */
    @SerializedName("multiblock_variant")
    public String multiblockVariant = "";

    @Override
    public void handleButtonVisualize(Button button) {
        if (multiblockVariant != null && !multiblockVariant.isEmpty()) {
            boolean on = com.srqingchen.fieldworks.client.book.FWMultiblockGhost.toggle(multiblockVariant);
            if (on) {
                // 261014 T8：开启投影即自动关闭手册界面，直接进入世界内放置状态
                // (书界面打开时无法进行世界右键锚定——用户实测"点眼睛没反应"根因)。
                net.minecraft.client.Minecraft.getInstance().setScreen(null);
            }
        }
    }
}
