package com.example.addon.mixin;

import com.example.addon.modules.AutoTrade;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onSetTradeOffers", at = @At("TAIL"))
    public void onSetTradeOffers(SetTradeOffersS2CPacket packet, CallbackInfo ci){
        var autoTrade = Modules.get().get(AutoTrade.class);
        if (autoTrade != null && autoTrade.isActive()) {
            autoTrade.onSetTradeOffers(packet);
        }
    }
}
