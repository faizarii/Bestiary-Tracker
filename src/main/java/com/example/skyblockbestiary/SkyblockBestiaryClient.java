package com.example.skyblockbestiary;

import com.example.skyblockbestiary.hud.BestiaryHUD;
import net.fabricmc.api.ClientModInitializer;

public final class SkyblockBestiaryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BestiaryHUD.init();
    }
}
