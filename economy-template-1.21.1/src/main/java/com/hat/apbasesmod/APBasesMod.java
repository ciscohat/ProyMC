package com.hat.apbasesmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(APBasesMod.MODID)
public class APBasesMod {
    public static final String MODID = "apbasesmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public APBasesMod(IEventBus modEventBus, ModContainer modContainer) {
        // Registra los ítems del mod (sin esto, las monedas no existen).
        ModItems.register(modEventBus);

        // Mete las monedas en la pestaña creativa "Ingredientes".
        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.COPPER_COIN);
            event.accept(ModItems.SILVER_COIN);
            event.accept(ModItems.GOLD_COIN);
        }
    }
}
