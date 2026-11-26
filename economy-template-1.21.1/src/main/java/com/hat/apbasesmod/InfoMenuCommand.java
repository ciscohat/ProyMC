package com.hat.apbasesmod;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = APBasesMod.MODID)
public class InfoMenuCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // /infomenu -> abre el menú de información
        event.getDispatcher().register(Commands.literal("infomenu")
                .executes(ctx -> {
                    InfoMenu.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
