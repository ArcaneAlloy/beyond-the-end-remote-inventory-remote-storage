package fr.shoqapik.occbutton;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = OccButton.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForceChunkLoader {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel storageWorld = event.getServer().getLevel(StorageConstants.DIMENSION);

        if (storageWorld == null) {
            LOGGER.warn("[occbutton] Dimension {} no encontrada al iniciar — el chunk no se puede forzar todavía.",
                    StorageConstants.DIMENSION.location());
            return;
        }

        ChunkPos chunkPos = new ChunkPos(StorageConstants.CONTROLLER_POS);

        // Forzar carga permanente del chunk mientras el servidor esté activo
        boolean result = ForgeChunkManager.forceChunk(
                storageWorld,
                OccButton.MOD_ID,
                StorageConstants.CONTROLLER_POS,
                chunkPos.x,
                chunkPos.z,
                true,   // add = true (forzar)
                true    // ticking = true (el chunk procesa ticks)
        );

        LOGGER.info("[occbutton] Chunk {} forzado en {}: {}",
                chunkPos, StorageConstants.DIMENSION.location(), result ? "OK" : "FALLO");
    }
}
