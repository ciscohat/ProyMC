package com.hat.apbasesmod;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/*
 * Aquí se declaran los ítems del mod.
 * DeferredRegister guarda la "receta" de cada ítem; NeoForge los crea
 * de verdad cuando el juego abre el registro (por eso se usa .get() después).
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(APBasesMod.MODID);

    // El texto es el id: "apbasesmod:copper_coin". Debe coincidir con el nombre del
    // modelo JSON, la textura PNG y la clave de idioma.
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin", new Item.Properties());
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin", new Item.Properties());
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin", new Item.Properties());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
