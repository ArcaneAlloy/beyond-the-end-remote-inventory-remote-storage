package fr.shoqapik.occbutton.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class KeybindHandler {

    private final StorageButtonOverlay overlay;

    public KeybindHandler(StorageButtonOverlay overlay) {
        this.overlay = overlay;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        while (ClientSetup.OPEN_STORAGE_KEY.consumeClick()) {
            overlay.openStorage();
        }
    }
}
