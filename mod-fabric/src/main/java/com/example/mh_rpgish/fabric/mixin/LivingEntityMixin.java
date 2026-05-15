package com.example.mh_rpgish.fabric.mixin;

import com.example.mh_rpgish.fabric.HpDisplayHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private float mh_rpgish$preHealth;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void mh_rpgish$captureHpBeforeHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        mh_rpgish$preHealth = self.getHealth();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void mh_rpgish$afterHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())) return;
        LivingEntity self = (LivingEntity) (Object) this;
        float damage = mh_rpgish$preHealth - self.getHealth();
        if (damage <= 0f) return;
        HpDisplayHandler.onDamage(self, source, damage);
    }

    @Inject(method = "heal", at = @At("HEAD"))
    private void mh_rpgish$captureHpBeforeHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        mh_rpgish$preHealth = self.getHealth();
    }

    @Inject(method = "heal", at = @At("RETURN"))
    private void mh_rpgish$afterHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        float healed = self.getHealth() - mh_rpgish$preHealth;
        if (healed <= 0f) return;
        HpDisplayHandler.onHeal(self, healed);
    }
}
