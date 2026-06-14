package fr.shoqapik.occbutton.client;

import com.klikli_dev.occultism.client.gui.storage.StorageControllerGuiBase;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public class StorageGuiButtonRemover {

    private static final Logger LOGGER = LogManager.getLogger("occbutton");

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        // Aplica a cualquier StorageControllerGuiBase — tanto la nuestra como la vanilla de Occultism
        if (!(screen instanceof StorageControllerGuiBase<?>)) return;

        // Ocultar botones de modo via reflection
        Class<?> baseClass = StorageControllerGuiBase.class;
        for (String fieldName : new String[]{"autocraftingModeButton", "inventoryModeButton"}) {
            try {
                Field field = baseClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object btn = field.get(screen);
                if (btn instanceof AbstractWidget widget) {
                    widget.visible = false;
                    widget.active  = false;
                    // Mover fuera de pantalla via Unsafe para que no se renderice aunque
                    // StorageControllerGuiBase lo dibuje directamente
                    sun.misc.Unsafe unsafe = getUnsafe();
                    if (unsafe != null) {
                        try {
                            // AbstractWidget tiene campos x e y (o f_94051_, f_94052_ en SRG)
                            Field xField = getField(net.minecraft.client.gui.components.AbstractWidget.class,
                                    "x", "f_94051_");
                            Field yField = getField(net.minecraft.client.gui.components.AbstractWidget.class,
                                    "y", "f_94052_");
                            if (xField != null) unsafe.putInt(widget, unsafe.objectFieldOffset(xField), -9999);
                            if (yField != null) unsafe.putInt(widget, unsafe.objectFieldOffset(yField), -9999);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[occbutton] Error ocultando boton {}: {}", fieldName, e.getMessage());
            }
        }

        // Ocultar slot del storage_remote (SimpleContainer) permanentemente
        if (screen instanceof StorageControllerGuiBase<?> gui) {
            gui.getMenu().slots.stream()
                    .filter(slot -> slot.container instanceof net.minecraft.world.SimpleContainer)
                    .findFirst()
                    .ifPresent(slot -> {
                        sun.misc.Unsafe unsafe = getUnsafe();
                        if (unsafe == null) return;
                        try {
                            Field xField = getField(net.minecraft.world.inventory.Slot.class, "x", "f_40220_");
                            Field yField = getField(net.minecraft.world.inventory.Slot.class, "y", "f_40221_");
                            if (xField != null) unsafe.putInt(slot, unsafe.objectFieldOffset(xField), -9999);
                            if (yField != null) unsafe.putInt(slot, unsafe.objectFieldOffset(yField), -9999);
                        } catch (Exception e) {
                            LOGGER.error("[occbutton] Error ocultando slot: {}", e.getMessage());
                        }
                    });
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
