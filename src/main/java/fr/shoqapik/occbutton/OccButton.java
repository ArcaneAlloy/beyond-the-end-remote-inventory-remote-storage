package fr.shoqapik.occbutton;

import fr.shoqapik.occbutton.client.ClientSetup;
import fr.shoqapik.occbutton.client.KeybindHandler;
import fr.shoqapik.occbutton.client.StorageButtonOverlay;
import fr.shoqapik.occbutton.client.StorageGuiButtonRemover;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OccButton.MOD_ID)
public class OccButton {
    public static final String MOD_ID = "occbutton";

    public OccButton() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        OccButtonMenuType.MENU_TYPES.register(modBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(ClientSetup::onRegisterKeyMappings);
            modBus.addListener(ClientSetup::onClientSetup);

            StorageButtonOverlay overlay = new StorageButtonOverlay();
            MinecraftForge.EVENT_BUS.register(overlay);
            MinecraftForge.EVENT_BUS.register(new KeybindHandler(overlay));
            // Aplica a CUALQUIER StorageControllerGuiBase — botón + clic derecho vanilla
            MinecraftForge.EVENT_BUS.register(new StorageGuiButtonRemover());
        });
    }
}
