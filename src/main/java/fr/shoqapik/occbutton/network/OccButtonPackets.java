package fr.shoqapik.occbutton.network;

import com.klikli_dev.occultism.api.common.blockentity.IStorageController;
import com.klikli_dev.occultism.api.common.data.GlobalBlockPos;
import com.klikli_dev.occultism.common.blockentity.StorageControllerBlockEntity;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import com.klikli_dev.occultism.registry.OccultismItems;
import fr.shoqapik.occbutton.StorageConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class OccButtonPackets {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("occbutton", "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, MessageOpenStorage.class,
                MessageOpenStorage::encode,
                MessageOpenStorage::decode,
                MessageOpenStorage::handle);
    }

    public static void sendOpenStorage() {
        try {
            LOGGER.info("[occbutton] Intentando enviar packet...");
            CHANNEL.send(PacketDistributor.SERVER.noArg(), new MessageOpenStorage());
            LOGGER.info("[occbutton] Packet enviado OK");
        } catch (Exception e) {
            LOGGER.error("[occbutton] ERROR enviando packet: {}", e.getMessage(), e);
        }
    }

    public static class MessageOpenStorage {
        public MessageOpenStorage() {}
        public static void encode(MessageOpenStorage msg, FriendlyByteBuf buf) {
            LOGGER.info("[occbutton] encode llamado");
        }
        public static MessageOpenStorage decode(FriendlyByteBuf buf) {
            LOGGER.info("[occbutton] decode llamado");
            return new MessageOpenStorage();
        }

        public static void handle(MessageOpenStorage msg, Supplier<NetworkEvent.Context> ctx) {
            LOGGER.info("[occbutton] handle llamado, lado={}", ctx.get().getDirection());
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                LOGGER.info("[occbutton] enqueueWork ejecutado, player={}", player);
                if (player == null) return;
                openStorageForPlayer(player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    static void openStorageForPlayer(ServerPlayer player) {
        ServerLevel storageWorld = player.getServer().getLevel(StorageConstants.DIMENSION);
        if (storageWorld == null) {
            player.sendSystemMessage(Component.translatable("occbutton.error.dimension_not_found"));
            return;
        }

        BlockEntity be = storageWorld.getBlockEntity(StorageConstants.CONTROLLER_POS);
        if (!(be instanceof IStorageController)) {
            player.sendSystemMessage(Component.translatable("occbutton.error.controller_not_found"));
            return;
        }

        StorageControllerBlockEntity controller = (StorageControllerBlockEntity) be;

        ItemStack fakeRemote = new ItemStack(OccultismItems.STORAGE_REMOTE.get());
        CompoundTag tag = new CompoundTag();
        GlobalBlockPos controllerGlobalPos = new GlobalBlockPos(StorageConstants.CONTROLLER_POS, storageWorld);
        tag.put("linkedStorageController", controllerGlobalPos.serializeNBT());
        fakeRemote.setTag(tag);

        ItemStack prevOffhand = player.getOffhandItem();
        player.getInventory().offhand.set(0, fakeRemote);

        try {
            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, playerInv, p) -> new StorageControllerContainer(id, playerInv, controller) {
                                @Override
                                public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                                    return true;
                                }
                            },
                            Component.literal("Dimensional Storage")
                    ),
                    buf -> {}
            );
            LOGGER.info("[occbutton] openScreen llamado OK");
        } catch (Exception e) {
            LOGGER.error("[occbutton] ERROR en openScreen: {}", e.getMessage(), e);
        } finally {
            player.getInventory().offhand.set(0, prevOffhand);
        }
    }
}
