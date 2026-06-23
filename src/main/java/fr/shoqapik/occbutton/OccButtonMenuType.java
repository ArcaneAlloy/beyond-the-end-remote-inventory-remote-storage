package fr.shoqapik.occbutton;

import com.klikli_dev.occultism.common.blockentity.StorageControllerBlockEntity;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import fr.shoqapik.occbutton.client.ClientMenuHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

public class OccButtonMenuType {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    /**
     * En singleplayer el servidor integrado pone el controller aquí justo antes
     * de NetworkHooks.openScreen, y la factory del cliente lo recoge.
     * Thread-safe via AtomicReference.
     */
    public static final AtomicReference<StorageControllerBlockEntity> PENDING_CONTROLLER
            = new AtomicReference<>(null);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, OccButton.MOD_ID);

    public static final RegistryObject<MenuType<StorageControllerContainer>> DIMENSIONAL_STORAGE =
            MENU_TYPES.register("dimensional_storage",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        // Leer buffer — clases seguras en servidor
                        BlockPos pos = data.readBlockPos();
                        ResourceLocation dimRL = data.readResourceLocation();

                        LOGGER.info("[occbutton] Factory cliente: pos={} dim={}", pos, dimRL);

                        // Delegar a ClientMenuHelper (@OnlyIn CLIENT) para evitar
                        // que Forge rechace esta clase en servidor dedicado
                        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                                () -> () -> ClientMenuHelper.buildContainer(windowId, inv, pos, dimRL));
                    }));
}
