package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.StringUtils;

import fi.dy.masa.litematica.gui.GuiSchematicVisibility;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

public class WidgetListSchematicVisibility extends WidgetListBase<VisibilityEntry, WidgetVisibilityEntry>
{
    private final GuiSchematicVisibility gui;

    public WidgetListSchematicVisibility(int x, int y, int width, int height, GuiSchematicVisibility parent)
    {
        super(x, y, width, height, null);

        this.gui = parent;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
        this.shouldSortList = true;
    }

    public GuiSchematicVisibility getParentGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<VisibilityEntry> getAllEntries()
    {
        LitematicaSchematic schematic = this.gui.getSchematicPlacement().getSchematic();
        List<VisibilityEntry> entries = new ArrayList<>();

        if (this.gui.getMode() == GuiSchematicVisibility.Mode.BLOCKS)
        {
            for (Map.Entry<Block, Integer> e : getBlockCounts(schematic).entrySet())
            {
                entries.add(VisibilityEntry.of(e.getKey(), e.getValue()));
            }
        }
        else
        {
            for (Map.Entry<EntityType<?>, Integer> e : getEntityTypeCounts(schematic).entrySet())
            {
                entries.add(VisibilityEntry.of(e.getKey(), e.getValue()));
            }
        }

        return entries;
    }

    @Override
    protected Comparator<VisibilityEntry> getComparator()
    {
        return new EntrySorter(this.gui);
    }

    @Override
    protected List<String> getEntryStringsForFilter(VisibilityEntry entry)
    {
        return ImmutableList.of(getName(entry).toLowerCase());
    }

    @Override
    protected WidgetVisibilityEntry createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        int height = this.browserEntryHeight;

        if ((usedHeight + height) > usableHeight)
        {
            return null;
        }

        return this.createListEntryWidget(x, y, listIndexStart, true, null);
    }

    @Override
    protected WidgetVisibilityEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable VisibilityEntry entry)
    {
        return new WidgetVisibilityEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }

    public static String getName(VisibilityEntry entry)
    {
        if (entry.isBlock())
        {
            return StringUtils.translate(entry.block().getDescriptionId());
        }
        else
        {
            return StringUtils.translate(entry.entityType().getDescriptionId());
        }
    }

    /**
     * All distinct, non-air Blocks used across every sub-region of the given schematic, with counts.
     * Order is insertion order (first time each block is encountered), not sorted.
     */
    public static Map<Block, Integer> getBlockCounts(LitematicaSchematic schematic)
    {
        Object2IntOpenHashMap<Block> counts = new Object2IntOpenHashMap<>();

        for (String regionName : schematic.getAreas().keySet())
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

            if (container == null)
            {
                continue;
            }

            Vec3i size = container.getSize();
            final int sizeX = size.getX();
            final int sizeY = size.getY();
            final int sizeZ = size.getZ();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        BlockState state = container.get(x, y, z);

                        if (state.isAir() == false && state.getBlock() != Blocks.STRUCTURE_VOID)
                        {
                            counts.addTo(state.getBlock(), 1);
                        }
                    }
                }
            }
        }

        Map<Block, Integer> result = new LinkedHashMap<>();

        for (Block block : counts.keySet())
        {
            result.put(block, counts.getInt(block));
        }

        return result;
    }

    /**
     * All distinct block types (no counts) used across every sub-region of the given schematic.
     * Cheaper than {@link #getBlockCounts} - only walks the palette, not every block position.
     */
    public static Set<Block> getCandidateBlocks(LitematicaSchematic schematic)
    {
        Set<Block> blocks = new LinkedHashSet<>();

        for (String regionName : schematic.getAreas().keySet())
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

            if (container != null)
            {
                for (BlockState state : container.getPalette().fromMapping())
                {
                    if (state != null && state.isAir() == false && state.getBlock() != Blocks.STRUCTURE_VOID)
                    {
                        blocks.add(state.getBlock());
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * All distinct entity types used across every sub-region of the given schematic, with counts.
     */
    public static Map<EntityType<?>, Integer> getEntityTypeCounts(LitematicaSchematic schematic)
    {
        Object2IntOpenHashMap<EntityType<?>> counts = new Object2IntOpenHashMap<>();

        for (String regionName : schematic.getAreas().keySet())
        {
            List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(regionName);

            if (entities == null)
            {
                continue;
            }

            for (LitematicaSchematic.EntityInfo info : entities)
            {
                String idStr = info.nbt().getStringOrDefault("id", "");
                Identifier id = idStr.isEmpty() ? null : Identifier.tryParse(idStr);

                if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
                {
                    counts.addTo(BuiltInRegistries.ENTITY_TYPE.getValue(id), 1);
                }
            }
        }

        Map<EntityType<?>, Integer> result = new LinkedHashMap<>();

        for (EntityType<?> type : counts.keySet())
        {
            result.put(type, counts.getInt(type));
        }

        return result;
    }

    public static Set<EntityType<?>> getCandidateEntityTypes(LitematicaSchematic schematic)
    {
        return getEntityTypeCounts(schematic).keySet();
    }

    protected static class EntrySorter implements Comparator<VisibilityEntry>
    {
        private final GuiSchematicVisibility gui;

        protected EntrySorter(GuiSchematicVisibility gui)
        {
            this.gui = gui;
        }

        @Override
        public int compare(VisibilityEntry entry1, VisibilityEntry entry2)
        {
            boolean reverse = this.gui.getSortInReverse();
            int nameCompare = getName(entry1).compareTo(getName(entry2));

            if (this.gui.getSortCriteria() == GuiSchematicVisibility.SortCriteria.COUNT)
            {
                int c1 = entry1.count();
                int c2 = entry2.count();

                return c1 == c2 ? nameCompare : ((c1 > c2) != reverse ? -1 : 1);
            }

            return reverse == false ? nameCompare * -1 : nameCompare;
        }
    }
}
