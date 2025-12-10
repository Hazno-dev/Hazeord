package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;

import java.util.function.Predicate;

public class AutoTrade extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgTrade = this.settings.createGroup("Trade");

    private final Setting<Boolean> SkipIfEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("Skip Empty Stock")
        .description("Skips opening the merchant if the target trade is depleted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Item> BuyItem = sgTrade.add(new ItemSetting.Builder()
        .name("Buy Item")
        .description("The item to give to the villager.")
        .defaultValue(Items.EMERALD)
        .build()
    );

    private final Setting<Item> SellItem = sgTrade.add(new ItemSetting.Builder()
        .name("Sell Item")
        .description("The item to receive from the villager.")
        .defaultValue(Items.EMERALD)
        .build()
    );

    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public AutoTrade() {
        super(AddonTemplate.CATEGORY, "AutoTrade", "Automatically trades items with a villager.");
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        // Create & stretch the marker object
        var target = event.entity;

        //if not target instanceof VillagerEntity villager, return
        if (!(target instanceof VillagerEntity villager)) {
            return;
        }

        //var offers = villager.getOffers();
        //if (offers == null || offers.isEmpty()) {
        //    return;
        //}
//        KeepAliveC2SPacket
//        for (var offer : offers) {
//            offer.
//            // Check if the offer is valid and the player has enough items to trade
//            if (offer.getMaxUses() > 0 && mc.player.getInventory().count(offer.getAdjustedFirstBuyItem()) >= offer.getFirstBuyItem().getCount()) {
//                // Perform the trade by sending a packet to the server
//                mc.interactionManager.interactEntity(mc.player, villager, offer);
//                break; // Trade only once per interaction
//            }
//        }
    }

    public void onSetTradeOffers(SetTradeOffersS2CPacket tradeOffersPacket) {
        var offers = tradeOffersPacket.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null || client.world == null || client.getNetworkHandler() == null) {
            return;
        }

        if (tradeOffersPacket.getSyncId() != client.player.currentScreenHandler.syncId
            || !(client.player.currentScreenHandler instanceof MerchantScreenHandler merchantScreenHandler)) {
            return;
        }

        var playerInv = client.player.getInventory();
        if (playerInv == null) {
            return;
        }

        var shouldClose = false;
        for (int i = 0; i < offers.size(); i++) {
            var offer = offers.get(i);
            var offerItem = offer.getOriginalFirstBuyItem().getItem();
            if (offerItem != BuyItem.get()) {
                continue;
            }

            var sellItem = offer.getSellItem().getItem();
            if (sellItem != SellItem.get()) {
                continue;
            }

            var uses = offer.getUses();
            var maxUses = offer.getMaxUses();
            if (maxUses <= 0) {
                continue;
            }

            if (offer.isDisabled() || uses >= maxUses) {
                if (SkipIfEmpty.get()) {
                    shouldClose = true;
                    ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Skipping villager as (highlight)%s(default) trade out of stock.", offerItem.getName().getString());
//                    var entity = client.targetedEntity;
//                    if (entity instanceof VillagerEntity villager) {
//                        try {
//                            var restockTimeField = VillagerEntity.class.getDeclaredField("lastRestockTime");
//                            var restocksTodayField = VillagerEntity.class.getDeclaredField("restocksToday");
//                            restockTimeField.setAccessible(true);
//                            restocksTodayField.setAccessible(true);
//                            var lastRestockTime = restockTimeField.getLong(villager);
//                            var restocksToday = restocksTodayField.getInt(villager);
//                            var currentTime = villager.getEntityWorld().getTime();
//
//                            if (currentTime > lastRestockTime + 2400L && restocksToday < 2) {
//                                ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Skipping villager as (highlight)%s(default) trade out of stock.", offerItem.getName().getString());
//                            } else {
//                                ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Skipping villager as (highlight)%s(default) trade out of stock.\n" +
//                                    "Villager cannot restock! Restocks Today: %s. Next Restock Time: %s", offerItem.getName().getString(), restocksToday, Math.max(currentTime - (lastRestockTime + 2400L), 0L));
//                            }
//
//                        }
//                        catch (Exception e) {
//                            ChatUtils.sendMsg(this.hashCode(), Formatting.RED, "Failed to get lastRestockTime field.");
//                        }
//                    }
                    break;
                }

                continue;
            }

            var count = offer.getDisplayedFirstBuyItem().getCount();


            var used = uses;
            while (uses < maxUses) {
                var playerInvSlot = GetSlotMatching(client, (stack) -> stack.getItem() == offerItem && stack.getCount() >= count);
                if (playerInvSlot == -1) {
                    //ChatUtils.sendMsg(this.hashCode(), Formatting.RED, "No (highlight)%s x%s(default) found - Stopping AutoTrade.", offerItem.getName().getString(), count);
                    break;
                }

                merchantScreenHandler.setRecipeIndex(i);
                merchantScreenHandler.switchTo(i);
                client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(i));
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, 2, 0, SlotActionType.QUICK_MOVE, client.player);
                uses = offer.getUses();
                shouldClose = true;



                //playerInv.removeStack(playerInvSlot, count);
                //playerInv.insertStack(offer.getSellItem().copy());
            }


            if (shouldClose){
                ChatUtils.sendMsg(this.hashCode(), Formatting.YELLOW, "Complete (highlight)%s(default) out of (highlight)%s(default) trades. [(highlight)%s x%s(default)]", uses - used, maxUses - used, offerItem.getName().getString(), count);
            } else {
                ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "No (highlight)%s x%s(default) found - Skipping AutoTrade.", offerItem.getName().getString(), count);
            }
            break;
        }

        if (shouldClose) {
            client.player.closeScreen();
            client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(tradeOffersPacket.getSyncId()));
        }
    }

    private static int GetSlotMatching(MinecraftClient mc, Predicate<ItemStack> predicate) {
        if (mc == null || mc.player == null){
            return -1;
        }

        var inventory = mc.player.getInventory();
        if (inventory == null){
            return -1;
        }

        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (predicate.test(stack)) {
                return i;
            }
        }

        return -1;
    }
}
