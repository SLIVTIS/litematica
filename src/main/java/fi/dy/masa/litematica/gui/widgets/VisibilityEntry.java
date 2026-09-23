package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * A single row's subject in the schematic visibility list - either a Block
 * (materials mode) or an EntityType (entities mode), never both.
 */
public record VisibilityEntry(@Nullable Block block, @Nullable EntityType<?> entityType)
{
    public static VisibilityEntry of(Block block)
    {
        return new VisibilityEntry(block, null);
    }

    public static VisibilityEntry of(EntityType<?> entityType)
    {
        return new VisibilityEntry(null, entityType);
    }

    public boolean isBlock()
    {
        return this.block != null;
    }
}
