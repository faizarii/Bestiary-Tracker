package com.example.skyblockbestiary.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

// Only the item and skin profile are needed; menu lore and kill counts are stored separately.
record BestiaryIcon(String item, JsonElement profile) {
    static BestiaryIcon capture(ItemStack stack) {
        ResolvableProfile skin = stack.get(DataComponents.PROFILE);
        return new BestiaryIcon(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
            skin == null ? null : ResolvableProfile.CODEC.encodeStart(JsonOps.INSTANCE, skin).getOrThrow());
    }

    ItemStack restore() {
        Identifier id = Identifier.tryParse(item);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown icon item: " + item);
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        if (profile != null && !profile.isJsonNull()) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.CODEC.parse(JsonOps.INSTANCE, profile).getOrThrow());
        }
        return stack;
    }
}
