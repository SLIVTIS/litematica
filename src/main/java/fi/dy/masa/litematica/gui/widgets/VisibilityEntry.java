package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * A single row's subject in the schematic visibility list - either a Block
 * (materials mode) or an EntityType (entities mode), never both - along with
 * how many of it are required by the schematic.
 */
public record VisibilityEntry(@Nullable Block block, @Nullable EntityType<?> entityType, int count)
{
    public static VisibilityEntry of(Block block, int count)
    {
        return new VisibilityEntry(block, null, count);
    }

    public static VisibilityEntry of(EntityType<?> entityType, int count)
    {
        return new VisibilityEntry(null, entityType, count);
    }

    public boolean isBlock()
    {
        return this.block != null;
    }
}
