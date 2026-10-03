package com.example.skyblockbestiary.data;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class BestiaryIconCheck {
    public static void main(String[] args) {
        BestiaryTierMessagesCheck.run();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(net.minecraft.data.registries.VanillaRegistries.createLookup())
            .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        Gson gson = new Gson();
        ResolvableProfile profile = ResolvableProfile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            {"id":[0,0,0,1],"name":"Mob","properties":[
              {"name":"textures","value":"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZjMGIzNmQ1M2ZmZjY5YTQ5YzdkNmYzOTMyZjJiMGZlOTQ4ZTAzMjIyNmQ1ZTgwNDVlYzU4NDA4YTM2ZTk1MSJ9fX0="}
            ]}
            """)).getOrThrow();
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, profile);
        head.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Mob I"));
        BestiaryIcon saved = BestiaryIcon.capture(head);
        assert saved.profile().toString().contains("eyJ0ZXh0dXJlcy") : "Texture property was not captured";
        ItemStack restored = gson.fromJson(gson.toJson(saved), BestiaryIcon.class).restore();
        assert restored.is(Items.PLAYER_HEAD);
        assert BestiaryIcon.capture(restored).equals(saved) : "Skin texture lost across JSON round-trip";
        assert restored.get(DataComponents.CUSTOM_NAME) == null : "Menu metadata should not be cached";

        BestiaryIcon plain = BestiaryIcon.capture(new ItemStack(Items.DRAGON_EGG));
        assert gson.fromJson(gson.toJson(plain), BestiaryIcon.class).restore().is(Items.DRAGON_EGG);
        assert gson.fromJson("{\"item\":\"minecraft:player_head\"}", BestiaryIcon.class).restore().is(Items.PLAYER_HEAD);
        try {
            new BestiaryIcon("minecraft:missing_item", null).restore();
            throw new AssertionError("Unknown item was accepted");
        } catch (IllegalArgumentException expected) {
            // The caller skips invalid icons without discarding saved progress.
        }
        try {
            new BestiaryIcon("minecraft:player_head", JsonParser.parseString("[]")).restore();
            throw new AssertionError("Malformed profile was accepted");
        } catch (IllegalStateException expected) {
            // Codec errors are isolated to the damaged icon.
        }
        System.out.println("Bestiary icon persistence checks passed");
    }
}
