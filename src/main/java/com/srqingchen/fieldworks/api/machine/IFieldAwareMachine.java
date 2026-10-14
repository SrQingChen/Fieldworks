package com.srqingchen.fieldworks.api.machine;

import com.srqingchen.fieldworks.api.field.FieldDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 机器读场统一契约(§19.1 api.machine / §19.2 用例 2)：
 * 附属模组实现本接口 + 数据包配方，即完整享受升级芯/休眠调度/Fit 面板(机器框架即 SDK)。
 */
public interface IFieldAwareMachine {
    /** 本机器感知的场集(空 = 感知全部注册场)。 */
    default List<FieldDefinition> sensedFields() {
        return List.of();
    }

    /** 场档案白名单：该机器可执行的配方 machine id。 */
    Identifier machineId();
}
