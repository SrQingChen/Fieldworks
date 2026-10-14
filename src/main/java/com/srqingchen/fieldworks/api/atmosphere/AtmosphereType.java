package com.srqingchen.fieldworks.api.atmosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 大气类型(§4.2)：类型决定"是什么气"，特征值决定"有多浓"。
 * MVP 15 种(策划稿 §4.2 表)；第三方/数据包可注册新大气(§19.3 数据包分层)。
 */
public record AtmosphereType(Identifier id, TraitVector baseline, String flavorKey) {

    public static final Codec<AtmosphereType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("id").forGetter(AtmosphereType::id),
            TraitVector.CODEC.optionalFieldOf("baseline", TraitVector.AMBIENT).forGetter(AtmosphereType::baseline),
            Codec.STRING.optionalFieldOf("flavor", "atmosphere.fieldworks.ambient_air").forGetter(AtmosphereType::flavorKey)
    ).apply(inst, AtmosphereType::new));

    public Component displayName() {
        return Component.translatable("atmosphere." + id.toShortLanguageKey().replace('/', '.'));
    }

    public Component flavor() {
        return Component.translatable(flavorKey);
    }
}
