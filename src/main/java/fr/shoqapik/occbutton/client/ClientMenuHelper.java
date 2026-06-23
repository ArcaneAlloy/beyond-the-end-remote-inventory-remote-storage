package fr.shoqapik.occbutton.client;

import com.klikli_dev.occultism.common.blockentity.StorageControllerBlockEntity;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import fr.shoqapik.occbutton.OccButtonMenuType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lógica cliente para la factory del MenuType.
 * En servidor dedicado: lee BlockPos+dim del buffer y busca el controller.
 * En singleplayer: el controller ya está en PENDING_CONTROLLER (puesto por el servidor integrado).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientMenuHelper {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    private ClientMenuHelper() {}

    public static StorageControllerContainer buildContainer(int windowId, Inventory inv,
                                                             BlockPos pos, ResourceLocation dimRL) {
        // Intentar primero PENDING_CONTROLLER (singleplayer / LAN)
        StorageControllerBlockEntity controller = OccButtonMenuType.PENDING_CONTROLLER.getAndSet(null);

        if (controller != null) {
            LOGGER.info("[occbutton] Controller obtenido de PENDING_CONTROLLER OK");
        } else {
            // Servidor dedicado: buscar desde el servidor integrado si existe
            var singleplayer = Minecraft.getInstance().getSingleplayerServer();
            if (singleplayer != null) {
                var dimKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimRL);
                var serverLevel = singleplayer.getLevel(dimKey);
                if (serverLevel != null) {
                    BlockEntity be = serverLevel.getBlockEntity(pos);
                    if (be instanceof StorageControllerBlockEntity sce) {
                        controller = sce;
                        LOGGER.info("[occbutton] Controller obtenido desde servidor integrado OK");
                    }
                }
            }
        }

        if (controller == null) {
            LOGGER.error("[occbutton] No se pudo obtener el controller en pos={} dim={}", pos, dimRL);
            return null;
        }

        final StorageControllerBlockEntity finalController = controller;
        return new StorageControllerContainer(windowId, inv, finalController) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }

            @Override
            public MenuType<?> getType() {
                return OccButtonMenuType.DIMENSIONAL_STORAGE.get();
            }
        };
    }
}
