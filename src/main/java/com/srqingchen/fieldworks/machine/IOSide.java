package com.srqingchen.fieldworks.machine;

/** 六面 IO 模式(§10.3 底座能力，Thermal 式)。 */
public enum IOSide {
    INPUT,     // 输入面(青)
    OUTPUT,    // 输出面(琥珀)
    DISABLED,  // 禁用(灰)
    AUTO;      // 自动：按接触网络动态判断(§10.5)

    public IOSide next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public IOSide previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /** 图集 config 覆盖位偏移(参考文档 §4.2 配色语义)。 */
    public int atlasOffset() {
        return switch (this) {
            case INPUT -> FW_IO_INPUT;
            case OUTPUT -> FW_IO_OUTPUT;
            case DISABLED -> FW_IO_NONE;
            case AUTO -> FW_IO_BOTH;
        };
    }

    // 对应 atlas CFGOV 列(0/23/46/69)
    public static final int FW_IO_NONE = 0, FW_IO_INPUT = 23, FW_IO_OUTPUT = 46, FW_IO_BOTH = 69;
}
