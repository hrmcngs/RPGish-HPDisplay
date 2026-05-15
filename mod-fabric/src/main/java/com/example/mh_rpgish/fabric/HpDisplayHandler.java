package com.example.mh_rpgish.fabric;

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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class HpDisplayHandler {
    private HpDisplayHandler() {}

    public static final String TAG_INDICATOR = "mh_rpgish.dmg_indicator";
    private static final int HP_BAR_TICKS = 100;
    private static final int INDICATOR_TICKS = 10;

    public static final class BarState {
        @Nullable Component originalName;
        boolean hasOriginal;
        boolean originalCaptured;
        long resetDeadline;
    }

    private static final Map<UUID, BarState> BAR_STATES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<UUID>> TRACKED_BARS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, Long>> TRACKED_INDICATORS = new HashMap<>();

    public static void onDamage(LivingEntity victim, DamageSource source, float damage) {
        Level level = victim.level();
        if (level.isClientSide()) return;
        if (damage <= 0f) return;

        // The Fabric mixin injects at RETURN of hurt(), so victim.getHealth() is already post-damage.
        if (victim instanceof ServerPlayer player) {
            spawnDamageIndicator((ServerLevel) level, player, damage, source);
            sendPlayerHpBar(player);
            DamageColors.clearTags(victim);
            return;
        }

        if (!shouldTrack(victim)) return;

        captureOriginalName(victim);
        spawnDamageIndicator((ServerLevel) level, victim, damage, source);
        applyHpBar((ServerLevel) level, victim);

        DamageColors.clearTags(victim);
    }

    public static void onHeal(LivingEntity victim, float healed) {
        Level level = victim.level();
        if (level.isClientSide()) return;
        if (healed <= 0f) return;

        if (victim instanceof ServerPlayer player) {
            sendPlayerHpBar(player);
            return;
        }

        if (!shouldTrack(victim)) return;
        captureOriginalName(victim);
        applyHpBar((ServerLevel) level, victim);
    }

    private static void sendPlayerHpBar(ServerPlayer player) {
        Component bar = HpBarFormatter.bar(player.getHealth(), player.getMaxHealth());
        player.displayClientMessage(bar, true); // true = action bar
    }

    public static void onLevelTick(ServerLevel level) {
        long now = level.getGameTime();
        ResourceKey<Level> dim = level.dimension();

        Set<UUID> bars = TRACKED_BARS.get(dim);
        if (bars != null && !bars.isEmpty()) {
            Iterator<UUID> it = bars.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Entity e = level.getEntity(uuid);
                BarState state = BAR_STATES.get(uuid);
                if (!(e instanceof LivingEntity living) || !e.isAlive() || state == null) {
                    BAR_STATES.remove(uuid);
                    it.remove();
                    continue;
                }
                if (now >= state.resetDeadline) {
                    resetHpBar(living, state);
                    BAR_STATES.remove(uuid);
                    it.remove();
                }
            }
        }

        Map<UUID, Long> inds = TRACKED_INDICATORS.get(dim);
        if (inds != null && !inds.isEmpty()) {
            Iterator<Map.Entry<UUID, Long>> it = inds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                Entity e = level.getEntity(entry.getKey());
                if (e == null || !e.isAlive()) {
                    it.remove();
                    continue;
                }
                if (now >= entry.getValue()) {
                    e.discard();
                    it.remove();
                }
            }
        }
    }

    private static boolean shouldTrack(LivingEntity entity) {
        if (entity instanceof Player) return false;
        if (entity instanceof EnderDragon) return false;
        if (entity instanceof WitherBoss) return false;
        if (entity instanceof ArmorStand) return false;
        if (entity.getType() == EntityType.ARMOR_STAND) return false;
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) return false;
        return entity.getMaxHealth() > 0f;
    }

    private static void captureOriginalName(LivingEntity entity) {
        BarState state = BAR_STATES.get(entity.getUUID());
        if (state != null && state.originalCaptured) return;
        if (state == null) {
            state = new BarState();
            BAR_STATES.put(entity.getUUID(), state);
        }
        Component name = entity.getCustomName();
        state.originalName = name;
        state.hasOriginal = name != null;
        state.originalCaptured = true;
    }

    private static void applyHpBar(ServerLevel level, LivingEntity entity) {
        Component bar = HpBarFormatter.bar(entity.getHealth(), entity.getMaxHealth());
        entity.setCustomName(bar);
        entity.setCustomNameVisible(true);

        BarState state = BAR_STATES.computeIfAbsent(entity.getUUID(), k -> {
            BarState s = new BarState();
            s.originalCaptured = true;
            return s;
        });
        state.resetDeadline = level.getGameTime() + HP_BAR_TICKS;
        TRACKED_BARS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(entity.getUUID());
    }

    private static void resetHpBar(LivingEntity entity, BarState state) {
        if (state.hasOriginal && state.originalName != null) {
            entity.setCustomName(state.originalName);
            entity.setCustomNameVisible(true);
        } else {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }

    private static void spawnDamageIndicator(ServerLevel level, LivingEntity victim, float damage, DamageSource source) {
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
        level.addFreshEntity(stand);
        TRACKED_INDICATORS.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(stand.getUUID(), deadline);
    }

    private static double pickHeightOffset(LivingEntity entity) {
        float h = entity.getBbHeight();
        if (h < 1.0f) return 0.4;
        if (h < 1.5f) return 0.8;
        if (h < 2.0f) return 1.3;
        return 1.9;
    }
}
