package com.example.mh_rpgish;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MhRpgishMod.MOD_ID)
public class MhRpgishMod {
    public static final String MOD_ID = "mh_rpgish";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MhRpgishMod() {
        MinecraftForge.EVENT_BUS.register(new HpDisplayEvents());
        LOGGER.info("RPGish HP Display loaded");
    }
}
