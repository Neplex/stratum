package fr.neplex.stratum.mixin;

import fr.neplex.stratum.worldgen.MutableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements MutableBlockEntity {

    @Accessor("worldPosition")
    public abstract void stratum$setWorldPosition(BlockPos pos);
}
