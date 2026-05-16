package com.example.mh_rpgish.fabric;

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
    private static final int INDICATOR_TICKS = 16; // ~0.8 秒 (浮上→落下が見える長さ)
    private static final double INDICATOR_INIT_VY = 0.3;   // 初速 (上方向)
    private static final double INDICATOR_GRAVITY = 0.045; // 1 tick ごとの落下加速
    private static final double INDICATOR_HDRAG = 0.92;    // 水平方向の減速

    public static final class BarState {
        @Nullable Component originalName;
        boolean hasOriginal;
        boolean originalCaptured;
        long resetDeadline;
    }

    public static final class IndicatorState {
        long deathTick;
        double vx, vy, vz;
    }

    private static final Map<UUID, BarState> BAR_STATES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<UUID>> TRACKED_BARS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, IndicatorState>> TRACKED_INDICATORS = new HashMap<>();

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

        Map<UUID, IndicatorState> inds = TRACKED_INDICATORS.get(dim);
        if (inds != null && !inds.isEmpty()) {
            Iterator<Map.Entry<UUID, IndicatorState>> it = inds.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, IndicatorState> entry = it.next();
                Entity e = level.getEntity(entry.getKey());
                if (e == null || !e.isAlive()) {
                    it.remove();
                    continue;
                }
                IndicatorState st = entry.getValue();
                if (now >= st.deathTick) {
                    e.discard();
                    it.remove();
                    continue;
                }
                // 浮き上がってから落下する放物線アニメーション (tick で手動制御)。
                // Marker armor stand はエンジン物理が効かないため自前で動かす。
                e.setPos(e.getX() + st.vx, e.getY() + st.vy, e.getZ() + st.vz);
                st.vy -= INDICATOR_GRAVITY;
                st.vx *= INDICATOR_HDRAG;
                st.vz *= INDICATOR_HDRAG;
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
        stand.setNoGravity(true); // 動きは tick で手動制御するためエンジン重力は切る
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.addTag(TAG_INDICATOR);

        // Marker 化: 当たり判定ゼロ + 名前プレートを spawn 位置のすぐ上に描画。
        // 通常の Armor Stand は本体が約 2 ブロック高で、名前がその上に出るため
        // 数値がモブの頭上はるか上に表示されてしまう。Marker setter は private なので
        // NBT を往復させてフラグだけ立てる。Marker は当たり判定も無いので
        // 「透明・回収不可・当たり判定なし」の要件も満たす。
        CompoundTag flags = new CompoundTag();
        stand.addAdditionalSaveData(flags);
        flags.putBoolean("Marker", true);
        stand.readAdditionalSaveData(flags);

        int dmgInt = Math.max(1, Math.round(damage));
        int color = DamageColors.resolve(victim, source);
        Component label = Component.literal(Integer.toString(dmgInt))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        stand.setCustomName(label);

        // 浮き上がってから落下する放物線アニメーション用の初速。左右にランダムに散らす。
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        IndicatorState st = new IndicatorState();
        st.vx = (rng.nextDouble() - 0.5) * 0.12;
        st.vy = INDICATOR_INIT_VY;
        st.vz = (rng.nextDouble() - 0.5) * 0.12;
        st.deathTick = level.getGameTime() + INDICATOR_TICKS;

        level.addFreshEntity(stand);
        TRACKED_INDICATORS.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(stand.getUUID(), st);
    }

    private static double pickHeightOffset(LivingEntity entity) {
        // Marker armor stand の名前プレートは spawn 位置の約 0.5 上に描画される。
        // 被弾エンティティの頭頂 +0.3 あたりに数値が来るよう、その分を引いておく。
        return entity.getBbHeight() + 0.3 - 0.5;
    }
}
