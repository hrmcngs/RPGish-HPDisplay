package com.example.mh_rpgish.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MhRpgishFabric implements ModInitializer {
    public static final String MOD_ID = "mh_rpgish";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(HpDisplayHandler::onLevelTick);
        LOGGER.info("RPGish HP Display (Fabric) loaded");
    }
}
