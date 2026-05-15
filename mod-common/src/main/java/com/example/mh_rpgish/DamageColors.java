package com.example.mh_rpgish;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class DamageColors {
    private DamageColors() {}

    public static final int WHITE     = 0xFFFFFF;
    public static final int POISON    = 0xD0FF95;
    public static final int FIRE      = 0xFFC18A;
    public static final int FREEZING  = 0x9CF6FF;
    public static final int WITHER    = 0x272628;
    public static final int ICE       = 0x39D4FF;
    public static final int ELECTRIC  = 0xFFFF55;
    public static final int CORROSION = 0xFF19E8;
    public static final int HOLY      = 0xFFAA00;
    public static final int ERROR     = 0xFF5555;

    public static final String TAG_ICE       = "minecraft_armor_weapon.mh_rpgish.ice_damage";
    public static final String TAG_ELECTRIC  = "minecraft_armor_weapon.mh_rpgish.electric_damage";
    public static final String TAG_CORROSION = "minecraft_armor_weapon.mh_rpgish.corrosion_damage";
    public static final String TAG_HOLY      = "minecraft_armor_weapon.mh_rpgish.holy_damage";
    public static final String TAG_ERROR     = "minecraft_armor_weapon.mh_rpgish.error_damage";

    public static int resolve(LivingEntity victim, DamageSource source) {
        if (victim.getTags().contains(TAG_ERROR))     return ERROR;
        if (victim.getTags().contains(TAG_HOLY))      return HOLY;
        if (victim.getTags().contains(TAG_CORROSION)) return CORROSION;
        if (victim.getTags().contains(TAG_ELECTRIC))  return ELECTRIC;
        if (victim.getTags().contains(TAG_ICE))       return ICE;

        if (source != null) {
            if (source.is(DamageTypes.WITHER) || victim.hasEffect(MobEffects.WITHER)) return WITHER;
            if (source.is(DamageTypes.FREEZE) || victim.isFreezing())                 return FREEZING;
            if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                    || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.LAVA)
                    || victim.isOnFire())                                              return FIRE;
            if (source.is(DamageTypes.MAGIC) && victim.hasEffect(MobEffects.POISON))   return POISON;
        }
        if (victim.hasEffect(MobEffects.POISON)) return POISON;
        return WHITE;
    }

    public static void clearTags(LivingEntity entity) {
        entity.removeTag(TAG_ICE);
        entity.removeTag(TAG_ELECTRIC);
        entity.removeTag(TAG_CORROSION);
        entity.removeTag(TAG_HOLY);
        entity.removeTag(TAG_ERROR);
    }
}
