package com.srqingchen.fieldworks.machine;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.resources.Identifier;

/**
 * 机器规格：一台工艺机器 = 场感知 + 能量 + 配方执行器(§10.1)，
 * 机器之间的差异只在"感知什么场/执行什么配方/什么档位"——架构统一，内容即数据。
 */
public record MachineSpec(
        Identifier id,
        int tier,                  // Ⅰ..Ⅴ → 1..5
        Family family,             // 进度线(发光色，§16.1)
        int energyCapacity,        // J
        int maxEnergyReceive,      // J/t
        int inputSlots,
        int outputSlots,
        int tanks,                 // 气体/流体罐数
        int tankCapacity,          // mB
        Mode mode) {

    /** 进度线五色(§16.1 发光语言)。 */
    public enum Family {
        ENERGY(0xE8A33D),     // 能源 琥珀
        MATERIAL(0xD8DEE9),   // 材料 银白
        FIELD(0x4FC3D9),      // 场学 冷青
        ENVIRONMENT(0x6BCB77),// 环境 翠绿
        CONVENIENCE(0xB18CFF);// 便利 紫

        public final int glowColor;

        Family(int glowColor) {
            this.glowColor = glowColor;
        }
    }

    /** 工作模式。 */
    public enum Mode {
        PROCESSOR,     // 耗电执行配方(电解器/鼓风熔炉/压缩机…)
        GENERATOR,     // 燃料→电(蒸汽发电机)
        FLUID_BURNER,  // 蒸汽流体→电(蒸汽轮机)
        STORAGE,       // 纯储存(储气罐)
        PUMP           // 无限水泵(261014 T7：水源邻接+耗电产水)
    }

    public String langKey() {
        return "block.fieldworks." + id.getPath();
    }

    public static MachineSpec processor(String path, int tier, Family family, int cap, int maxIn,
                                        int in, int out, int tanks, int tankCap) {
        return new MachineSpec(Fieldworks.id(path), tier, family, cap, maxIn, in, out, tanks, tankCap, Mode.PROCESSOR);
    }

    public static MachineSpec generator(String path, int tier, Family family, int cap, int maxOut,
                                        int in, int out, int tanks, int tankCap) {
        return new MachineSpec(Fieldworks.id(path), tier, family, cap, maxOut, in, out, tanks, tankCap, Mode.GENERATOR);
    }
}
