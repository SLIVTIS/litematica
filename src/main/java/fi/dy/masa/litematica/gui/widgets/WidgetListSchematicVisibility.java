package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.AlphaNumComparator;
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

        if (this.gui.getMode() == GuiSchematicVisibility.Mode.BLOCKS)
        {
            List<VisibilityEntry> entries = new ArrayList<>();

            for (Block block : getCandidateBlocks(schematic))
            {
                entries.add(VisibilityEntry.of(block));
            }

            return entries;
        }
        else
        {
            List<VisibilityEntry> entries = new ArrayList<>();

            for (EntityType<?> type : getCandidateEntityTypes(schematic))
            {
                entries.add(VisibilityEntry.of(type));
            }

            return entries;
        }
    }

    @Override
    protected Comparator<VisibilityEntry> getComparator()
    {
        return new EntryComparator();
    }

    @Override
    protected List<String> getEntryStringsForFilter(VisibilityEntry entry)
    {
        return ImmutableList.of(getName(entry).toLowerCase());
    }

    @Override
    protected WidgetVisibilityEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, VisibilityEntry entry)
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
     * All distinct, non-air Blocks used across every sub-region of the given schematic.
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
                    if (state != null && state.isAir() == false)
                    {
                        blocks.add(state.getBlock());
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * All distinct entity types used across every sub-region of the given schematic.
     */
    public static Set<EntityType<?>> getCandidateEntityTypes(LitematicaSchematic schematic)
    {
        Set<EntityType<?>> types = new LinkedHashSet<>();

        for (String regionName : schematic.getAreas().keySet())
        {
            List<LitematicaSchematic.EntityInfo> entities = schematic.getEntityListForRegion(regionName);

            if (entities != null)
            {
                for (LitematicaSchematic.EntityInfo info : entities)
                {
                    String idStr = info.nbt().getStringOrDefault("id", "");
                    Identifier id = idStr.isEmpty() ? null : Identifier.tryParse(idStr);

                    if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
                    {
                        types.add(BuiltInRegistries.ENTITY_TYPE.getValue(id));
                    }
                }
            }
        }

        return types;
    }

    protected static class EntryComparator extends AlphaNumComparator implements Comparator<VisibilityEntry>
    {
        @Override
        public int compare(VisibilityEntry entry1, VisibilityEntry entry2)
        {
            return this.compare(getName(entry1), getName(entry2));
        }
    }
}
