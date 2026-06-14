package fr.shoqapik.occbutton.client;

import com.klikli_dev.occultism.registry.OccultismContainers;
import com.mojang.blaze3d.platform.InputConstants;
import fr.shoqapik.occbutton.OccButtonMenuType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    public static final KeyMapping OPEN_STORAGE_KEY = new KeyMapping(
            "key.occbutton.open_storage",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.occbutton"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STORAGE_KEY);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Nuestro MenuType propio (para acceso remoto cross-dimension)
            MenuScreens.register(OccButtonMenuType.DIMENSIONAL_STORAGE.get(),
                    OccButtonStorageGui::new);

            // Sobreescribir la pantalla de Occultism para el StorageController vanilla
            // Asi cuando el jugador hace clic derecho en el bloque, se abre OccButtonStorageGui
            // que ya tiene los botones y el slot ocultos
            MenuScreens.register(OccultismContainers.STORAGE_CONTROLLER.get(),
                    OccButtonStorageGui::new);
        });
    }
}
