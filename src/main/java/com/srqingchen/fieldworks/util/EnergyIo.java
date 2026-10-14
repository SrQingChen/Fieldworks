package com.srqingchen.fieldworks.util;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** 事务安全能量操作(§6.7 主线程仅提交点)。 */
public final class EnergyIo {

    public static void insertSafe(SimpleEnergyHandler h, int amount) {
        if (amount <= 0) return;
        try (Transaction t = Transaction.openRoot()) {
            h.insert(amount, t);
            t.commit();
        }
    }

    public static void extractSafe(SimpleEnergyHandler h, int amount) {
        if (amount <= 0) return;
        try (Transaction t = Transaction.openRoot()) {
            h.extract(amount, t);
            t.commit();
        }
    }

    /** try-with-resources 之外的直接改写(内部逻辑)。 */
    public static void setSafe(SimpleEnergyHandler h, int amount) {
        h.set(Math.max(0, Math.min(amount, (int) h.getCapacityAsLong())));
    }

    private EnergyIo() {}
}
