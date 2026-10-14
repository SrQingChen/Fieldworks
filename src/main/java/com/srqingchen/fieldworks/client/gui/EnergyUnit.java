package com.srqingchen.fieldworks.client.gui;


/**
 * 能量显示单位(261014 T4)：内部 1J = 1FE = 1RF(策划稿 §能量立场)，仅显示层换算。
 * 点击机器 GUI 能量条循环 FE → RF → J；选择持久化到本地配置目录(客户端偏好)。
 */
public enum EnergyUnit {
    FE("FE"),
    RF("RF"),
    J("J");

    public final String suffix;

    EnergyUnit(String suffix) {
        this.suffix = suffix;
    }

    private static EnergyUnit current = FE;

    public static EnergyUnit current() {
        return current;
    }

    public static void cycle() {
        current = switch (current) {
            case FE -> RF;
            case RF -> J;
            case J -> FE;
        };
        save();
    }

    /** 千分位格式化 + 当前单位后缀。 */
    public static String format(int amount) {
        return String.format(java.util.Locale.ROOT, "%,d", amount) + " " + current.suffix;
    }

    // ── 轻量持久化(.fieldworks/energy_unit.txt，无配置库依赖) ──
    private static void save() {
        try {
            var dir = new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "fieldworks");
            dir.mkdirs();
            java.nio.file.Files.writeString(new java.io.File(dir, "energy_unit.txt").toPath(), current.name());
        } catch (Exception ignored) {
        }
    }

    public static void load() {
        try {
            var f = new java.io.File(new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "fieldworks"),
                    "energy_unit.txt");
            if (f.isFile()) {
                current = EnergyUnit.valueOf(java.nio.file.Files.readString(f.toPath()).trim());
            }
        } catch (Exception ignored) {
        }
    }
}
