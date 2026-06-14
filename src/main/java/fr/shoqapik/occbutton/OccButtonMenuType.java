package fr.shoqapik.occbutton;

import com.klikli_dev.occultism.common.blockentity.StorageControllerBlockEntity;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import fr.shoqapik.occbutton.client.OccButtonStorageGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

public class OccButtonMenuType {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    /**
     * Referencia temporal al controller, establecida por el servidor justo antes
     * de llamar a openScreen y leída por el cliente en la factory.
     * Es seguro en singleplayer porque ambos hilos son secuenciales respecto
     * a la apertura del menú.
     */
    public static final AtomicReference<StorageControllerBlockEntity> PENDING_CONTROLLER
            = new AtomicReference<>(null);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, OccButton.MOD_ID);

    public static final RegistryObject<MenuType<StorageControllerContainer>> DIMENSIONAL_STORAGE =
            MENU_TYPES.register("dimensional_storage",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        LOGGER.info("[occbutton] CLIENT factory llamada!");

                        StorageControllerBlockEntity controller = PENDING_CONTROLLER.getAndSet(null);

                        if (controller == null) {
                            LOGGER.error("[occbutton] PENDING_CONTROLLER es null!");
                            return null;
                        }

                        StorageControllerContainer container = new StorageControllerContainer(
                                windowId, inv, controller) {
                            @Override
                            public boolean stillValid(Player player) {
                                return true;
                            }

                            @Override
                            public MenuType<?> getType() {
                                return OccButtonMenuType.DIMENSIONAL_STORAGE.get();
                            }
                        };

                        // Abrir la GUI en el siguiente tick del render thread
                        final StorageControllerContainer finalContainer = container;
                        Minecraft.getInstance().execute(() -> {
                            OccButtonStorageGui gui = new OccButtonStorageGui(
                                    finalContainer, inv,
                                    Component.literal("Dimensional Storage"));
                            Minecraft.getInstance().setScreen(gui);
                            Minecraft.getInstance().player.containerMenu = finalContainer;
                            LOGGER.info("[occbutton] GUI abierta OK");
                        });

                        return container;
                    }));
}
