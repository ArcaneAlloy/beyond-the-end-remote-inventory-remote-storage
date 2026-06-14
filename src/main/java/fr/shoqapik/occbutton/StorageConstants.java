package fr.shoqapik.occbutton;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class StorageConstants {
    private StorageConstants() {}

    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            net.minecraft.core.Registry.DIMENSION_REGISTRY,
            new ResourceLocation("ender_journey", "the_forgotten_realm")
    );

    public static final BlockPos CONTROLLER_POS = new BlockPos(0, 112, 31);
}
