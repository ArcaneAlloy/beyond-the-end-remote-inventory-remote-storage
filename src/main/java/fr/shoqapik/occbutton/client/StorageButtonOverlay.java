package fr.shoqapik.occbutton.client;

import com.klikli_dev.occultism.api.common.blockentity.IStorageController;
import com.klikli_dev.occultism.common.blockentity.StorageControllerBlockEntity;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.shoqapik.occbutton.OccButtonMenuType;
import fr.shoqapik.occbutton.StorageConstants;
import fr.shoqapik.occbutton.network.OccButtonPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StorageButtonOverlay {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    private static final int BTN_SIZE     = 20;
    private static final int BTN_OFFSET_X = -24;
    private static final int BTN_OFFSET_Y =  4;

    private int btnX, btnY;

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isPlayerInventoryScreen(screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
        btnX = gui.getGuiLeft() + BTN_OFFSET_X;
        btnY = gui.getGuiTop()  + BTN_OFFSET_Y;

        int mouseX = getMouseX(mc);
        int mouseY = getMouseY(mc);
        boolean hovered = isHovered(mouseX, mouseY);

        drawButton(event.getPoseStack(), mc, hovered);

        if (hovered) {
            List<net.minecraft.util.FormattedCharSequence> lines = List.of(
                    Component.translatable("occbutton.open_storage").getVisualOrderText(),
                    Component.translatable("occbutton.open_storage.hint")
                            .withStyle(ChatFormatting.GRAY).getVisualOrderText()
            );
            gui.renderTooltip(event.getPoseStack(), lines, mouseX, mouseY);
        }
    }

    @SubscribeEvent
    public void onMouseClick(ScreenEvent.MouseButtonPressed.Post event) {
        if (!isPlayerInventoryScreen(event.getScreen())) return;
        if (event.getButton() != 0) return;

        if (isHovered((int) event.getMouseX(), (int) event.getMouseY())) {
            openStorage();
        }
    }

    public void openStorage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.0f);

        if (mc.getSingleplayerServer() != null) {
            // Singleplayer / LAN: acceder al servidor integrado directamente
            openStorageSingleplayer(mc);
        } else {
            // Servidor dedicado: enviar packet C2S
            LOGGER.info("[occbutton] Modo multijugador — enviando packet C2S");
            OccButtonPackets.sendOpenStorage();
        }
    }

    private void openStorageSingleplayer(Minecraft mc) {
        mc.getSingleplayerServer().execute(() -> {
            ServerLevel storageWorld = mc.getSingleplayerServer()
                    .getLevel(StorageConstants.DIMENSION);
            if (storageWorld == null) {
                mc.execute(() -> mc.player.sendSystemMessage(
                        Component.translatable("occbutton.error.dimension_not_found")));
                return;
            }

            BlockEntity be = storageWorld.getBlockEntity(StorageConstants.CONTROLLER_POS);
            if (!(be instanceof IStorageController)) {
                mc.execute(() -> mc.player.sendSystemMessage(
                        Component.translatable("occbutton.error.controller_not_found")));
                return;
            }

            StorageControllerBlockEntity controller = (StorageControllerBlockEntity) be;
            ServerPlayer serverPlayer = mc.getSingleplayerServer()
                    .getPlayerList().getPlayer(mc.player.getUUID());
            if (serverPlayer == null) return;

            // Establecer el controller ANTES de openScreen para que la factory
            // del cliente lo encuentre cuando procese el paquete S2C
            OccButtonMenuType.PENDING_CONTROLLER.set(controller);

            NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, p) -> new StorageControllerContainer(id, inv, controller) {
                                @Override
                                public boolean stillValid(Player player) {
                                    return true;
                                }

                                @Override
                                public net.minecraft.world.inventory.MenuType<?> getType() {
                                    return OccButtonMenuType.DIMENSIONAL_STORAGE.get();
                                }
                            },
                            Component.literal("Dimensional Storage")
                    ),
                    buf -> {
                        buf.writeBlockPos(StorageConstants.CONTROLLER_POS);
                        buf.writeResourceLocation(StorageConstants.DIMENSION.location());
                    }
            );

            LOGGER.info("[occbutton] openScreen singleplayer OK");
        });
    }

    private void drawButton(PoseStack poseStack, Minecraft mc, boolean hovered) {
        int colorBg    = hovered ? 0xFFBBBBBB : 0xFF888888;
        int colorLight = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
        int colorDark  = 0xFF444444;

        GuiComponent.fill(poseStack, btnX,     btnY,     btnX + BTN_SIZE,     btnY + BTN_SIZE,     colorDark);
        GuiComponent.fill(poseStack, btnX + 1, btnY + 1, btnX + BTN_SIZE - 1, btnY + BTN_SIZE - 1, colorBg);
        GuiComponent.fill(poseStack, btnX + 1, btnY + 1, btnX + BTN_SIZE - 1, btnY + 2,            colorLight);
        GuiComponent.fill(poseStack, btnX + 1, btnY + 1, btnX + 2,            btnY + BTN_SIZE - 1, colorLight);

        ItemStack icon = new ItemStack(OccultismItems.STORAGE_REMOTE.get());
        mc.getItemRenderer().renderGuiItem(icon, btnX + 2, btnY + 2);
    }

    private boolean isHovered(int mx, int my) {
        return mx >= btnX && mx < btnX + BTN_SIZE
            && my >= btnY && my < btnY + BTN_SIZE;
    }

    private static int getMouseX(Minecraft mc) {
        return (int)(mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / mc.getWindow().getScreenWidth());
    }

    private static int getMouseY(Minecraft mc) {
        return (int)(mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / mc.getWindow().getScreenHeight());
    }

    private static boolean isPlayerInventoryScreen(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        if (screen instanceof AbstractContainerScreen<?>) {
            String name = screen.getClass().getName();
            return name.contains("curios") && name.toLowerCase().contains("inventory");
        }
        return false;
    }
}
