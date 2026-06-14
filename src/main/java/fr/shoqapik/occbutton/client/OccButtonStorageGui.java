package fr.shoqapik.occbutton.client;

import com.klikli_dev.occultism.client.gui.storage.StorageControllerGui;
import com.klikli_dev.occultism.common.container.storage.StorageControllerContainer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

public class OccButtonStorageGui extends StorageControllerGui {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");
    private Slot remoteSlot = null;

    public OccButtonStorageGui(StorageControllerContainer container,
                                Inventory playerInventory,
                                Component title) {
        super(container, playerInventory, title);
    }

    @Override
    protected void init() {
        // Forzar modo INVENTORY antes de init para que muestre los items
        // y no el modo maquinas (horno/autocrafting)
        try {
            java.lang.reflect.Field modeField =
                com.klikli_dev.occultism.client.gui.storage.StorageControllerGuiBase.class
                    .getDeclaredField("guiMode");
            modeField.setAccessible(true);
            modeField.set(this, com.klikli_dev.occultism.api.common.data.StorageControllerGuiMode.INVENTORY);
        } catch (Exception e) {
            LOGGER.error("[occbutton] Error forzando modo INVENTORY: {}", e.getMessage());
        }

        super.init();
        hideModeSwitchButtons();
        for (Slot slot : this.menu.slots) {
            if (slot.container instanceof SimpleContainer) {
                remoteSlot = slot;
                // Mover permanentemente a -9999 en init — antes del primer render
                setSlotPos(remoteSlot, -9999, -9999);
                break;
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Mover el raton fuera del area del slot remoto para que no reciba hover
        // El slot permanece en su posicion original pero ignoramos el raton sobre el
        if (remoteSlot != null) {
            boolean mouseOverRemote = Math.abs(mouseX - (this.leftPos + remoteSlot.x)) < 10
                    && Math.abs(mouseY - (this.topPos + remoteSlot.y)) < 10;
            if (mouseOverRemote) {
                mouseX = -9999;
                mouseY = -9999;
            }
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Mantener el slot del remote fuera de pantalla permanentemente
        if (remoteSlot != null && remoteSlot.x != -9999) {
            setSlotPos(remoteSlot, -9999, -9999);
        }
    }

    private void setSlotPos(Slot slot, int x, int y) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            if (unsafe == null) return;
            Field xField = getField(Slot.class, "x", "f_40220_");
            Field yField = getField(Slot.class, "y", "f_40221_");
            if (xField != null) unsafe.putInt(slot, unsafe.objectFieldOffset(xField), x);
            if (yField != null) unsafe.putInt(slot, unsafe.objectFieldOffset(yField), y);
        } catch (Exception ignored) {}
    }

    private void hideModeSwitchButtons() {
        Class<?> baseClass = com.klikli_dev.occultism.client.gui.storage.StorageControllerGuiBase.class;
        for (String fieldName : new String[]{"autocraftingModeButton", "inventoryModeButton"}) {
            try {
                Field field = baseClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object btn = field.get(this);
                if (btn instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                    widget.visible = false;
                    widget.active  = false;
                    this.removeWidget(widget);
                }
            } catch (Exception e) {
                LOGGER.error("[occbutton] Error ocultando boton {}: {}", fieldName, e.getMessage());
            }
        }
    }

    private static Field getField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static sun.misc.Unsafe getUnsafe() {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
