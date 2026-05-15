package com.example.mh_rpgish;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class HpDisplayEvents {

    public static final String NS_TAG = "mh_rpgish";
    private static final String KEY_HP_BAR_RESET = NS_TAG + "_hp_bar_reset";
    private static final String KEY_ORIGINAL_NAME = NS_TAG + "_original_name";
    private static final String KEY_HAS_ORIGINAL = NS_TAG + "_has_original";
    private static final String KEY_INDICATOR_DEATH = NS_TAG + "_indicator_death";
    private static final String TAG_INDICATOR = "mh_rpgish.dmg_indicator";

    private static final int HP_BAR_TICKS = 100;   // 5 seconds
    private static final int INDICATOR_TICKS = 10; // 0.5 seconds

    private final Map<ResourceKey<Level>, Set<UUID>> trackedBars = new HashMap<>();
    private final Map<ResourceKey<Level>, Set<UUID>> trackedIndicators = new HashMap<>();

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return;

        float damage = event.getAmount();
        if (damage <= 0f) return;

        // LivingDamageEvent fires BEFORE setHealth, so getHealth() is still the pre-damage value.
        // Compute the projected post-damage HP for the bar.
        float projectedHp = Math.max(0f, victim.getHealth() - damage);

        // Player: アクションバーに表示 + 頭上のダメージ数値
        if (victim instanceof ServerPlayer player) {
            spawnDamageIndicator((ServerLevel) level, player, damage, event.getSource());
            sendPlayerHpBar(player, projectedHp);
            DamageColors.clearTags(victim);
            return;
        }

        if (!shouldTrack(victim)) return;

        saveOriginalName(victim);
        spawnDamageIndicator((ServerLevel) level, victim, damage, event.getSource());
        applyHpBar(victim, projectedHp);

        DamageColors.clearTags(victim);
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return;
        if (event.getAmount() <= 0f) return;

        // LivingHealEvent fires BEFORE the heal is applied — project the post-heal HP.
        float projectedHp = Math.min(victim.getMaxHealth(), victim.getHealth() + event.getAmount());

        if (victim instanceof ServerPlayer player) {
            sendPlayerHpBar(player, projectedHp);
            return;
        }

        if (!shouldTrack(victim)) return;
        saveOriginalName(victim);
        applyHpBar(victim, projectedHp);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        ResourceKey<Level> key = serverLevel.dimension();
        long now = serverLevel.getGameTime();

        Set<UUID> bars = trackedBars.get(key);
        if (bars != null && !bars.isEmpty()) {
            Iterator<UUID> it = bars.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Entity e = serverLevel.getEntity(uuid);
                if (!(e instanceof LivingEntity living) || !e.isAlive()) {
                    it.remove();
                    continue;
                }
                CompoundTag data = living.getPersistentData();
                long deadline = data.getLong(KEY_HP_BAR_RESET);
                if (deadline == 0L || now >= deadline) {
                    resetHpBar(living);
                    data.remove(KEY_HP_BAR_RESET);
                    it.remove();
                }
            }
        }

        Set<UUID> inds = trackedIndicators.get(key);
        if (inds != null && !inds.isEmpty()) {
            Iterator<UUID> it = inds.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Entity e = serverLevel.getEntity(uuid);
                if (e == null || !e.isAlive()) {
                    it.remove();
                    continue;
                }
                long deadline = e.getPersistentData().getLong(KEY_INDICATOR_DEATH);
                if (deadline != 0L && now >= deadline) {
                    e.discard();
                    it.remove();
                }
            }
        }
    }

    private boolean shouldTrack(LivingEntity entity) {
        if (entity instanceof Player) return false;
        if (entity instanceof EnderDragon) return false;
        if (entity instanceof WitherBoss) return false;
        if (entity instanceof ArmorStand) return false;
        if (entity.getType() == EntityType.ARMOR_STAND) return false;
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) return false;
        return entity.getMaxHealth() > 0f;
    }

    private void saveOriginalName(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(KEY_HAS_ORIGINAL)) return;
        // Only treat as "has original" when our reset key is absent (avoid capturing our own bar text)
        if (data.contains(KEY_HP_BAR_RESET)) return;
        Component name = entity.getCustomName();
        if (name == null) {
            data.putBoolean(KEY_HAS_ORIGINAL, false);
            return;
        }
        String json = Component.Serializer.toJson(name);
        data.putString(KEY_ORIGINAL_NAME, json);
        data.putBoolean(KEY_HAS_ORIGINAL, true);
    }

    private void applyHpBar(LivingEntity entity, float projectedHp) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Component bar = HpBarFormatter.bar(projectedHp, entity.getMaxHealth());
        entity.setCustomName(bar);
        entity.setCustomNameVisible(true);

        long deadline = serverLevel.getGameTime() + HP_BAR_TICKS;
        entity.getPersistentData().putLong(KEY_HP_BAR_RESET, deadline);

        trackedBars.computeIfAbsent(serverLevel.dimension(), k -> new HashSet<>()).add(entity.getUUID());
    }

    private void sendPlayerHpBar(ServerPlayer player, float projectedHp) {
        Component bar = HpBarFormatter.bar(projectedHp, player.getMaxHealth());
        player.displayClientMessage(bar, true); // true = action bar
    }

    private void resetHpBar(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(KEY_HAS_ORIGINAL)) {
            String json = data.getString(KEY_ORIGINAL_NAME);
            try {
                Component restored = Component.Serializer.fromJson(json);
                entity.setCustomName(restored);
                entity.setCustomNameVisible(restored != null);
            } catch (Exception ignored) {
                entity.setCustomName(null);
                entity.setCustomNameVisible(false);
            }
        } else {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    private void spawnDamageIndicator(ServerLevel level, LivingEntity victim, float damage, DamageSource source) {
        double yOffset = pickHeightOffset(victim);
        double x = victim.getX();
        double y = victim.getY() + yOffset;
        double z = victim.getZ();

        ArmorStand stand = new ArmorStand(level, x, y, z);
        stand.setInvisible(true);
        stand.setNoGravity(false);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.addTag(TAG_INDICATOR);

        int dmgInt = Math.max(1, Math.round(damage));
        int color = DamageColors.resolve(victim, source);
        Component label = Component.literal(Integer.toString(dmgInt))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        stand.setCustomName(label);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double mx = (rng.nextBoolean() ? 1 : -1) * 0.08;
        double mz = (rng.nextBoolean() ? 1 : -1) * 0.04;
        stand.setDeltaMovement(mx, 0.2, mz);
        stand.hasImpulse = true;

        long deadline = level.getGameTime() + INDICATOR_TICKS;
        stand.getPersistentData().putLong(KEY_INDICATOR_DEATH, deadline);

        level.addFreshEntity(stand);
        trackedIndicators.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(stand.getUUID());
    }

    private double pickHeightOffset(LivingEntity entity) {
        float h = entity.getBbHeight();
        if (h < 1.0f) return 0.4;
        if (h < 1.5f) return 0.8;
        if (h < 2.0f) return 1.3;
        return 1.9;
    }

    public static boolean isIndicator(Entity e) {
        return e instanceof ArmorStand && e.getTags().contains(TAG_INDICATOR);
    }
}
