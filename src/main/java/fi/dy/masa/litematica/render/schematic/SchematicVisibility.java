package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import fi.dy.masa.litematica.config.Configs;

/**
 * Global, session-persisted registry of which schematic ghost-rendering
 * materials (Blocks) and entity types are currently hidden from view.
 * Hidden blocks are excluded from schematic pick-block and Easy Place as well as rendering.
 * This does not change schematic data, pasting, the Material List, or the schematic verifier.
 */
public class SchematicVisibility
{
    public static final SchematicVisibility INSTANCE = new SchematicVisibility();

    private final Set<Block> hiddenBlocks = new HashSet<>();
    private final Set<EntityType<?>> hiddenEntityTypes = new HashSet<>();
    private boolean loaded;

    private SchematicVisibility()
    {
    }

    public boolean isBlockHidden(Block block)
    {
        this.loadFromConfigIfNeeded();
        return this.hiddenBlocks.contains(block);
    }

    public boolean isEntityTypeHidden(EntityType<?> type)
    {
        this.loadFromConfigIfNeeded();
        return this.hiddenEntityTypes.contains(type);
    }

    public void setBlockHidden(Block block, boolean hidden)
    {
        this.loadFromConfigIfNeeded();

        if (hidden)
        {
            this.hiddenBlocks.add(block);
        }
        else
        {
            this.hiddenBlocks.remove(block);
        }

        this.syncBlocksToConfig();
    }

    public void setEntityTypeHidden(EntityType<?> type, boolean hidden)
    {
        this.loadFromConfigIfNeeded();

        if (hidden)
        {
            this.hiddenEntityTypes.add(type);
        }
        else
        {
            this.hiddenEntityTypes.remove(type);
        }

        this.syncEntityTypesToConfig();
    }

    /**
     * Hides every block in {@code allCandidates} except {@code toShow}.
     */
    public void showOnlyBlock(Collection<Block> allCandidates, Block toShow)
    {
        this.loadFromConfigIfNeeded();
        this.hiddenBlocks.clear();
        this.hiddenBlocks.addAll(allCandidates);
        this.hiddenBlocks.remove(toShow);
        this.syncBlocksToConfig();
    }

    /**
     * Hides every entity type in {@code allCandidates} except {@code toShow}.
     */
    public void showOnlyEntityType(Collection<EntityType<?>> allCandidates, EntityType<?> toShow)
    {
        this.loadFromConfigIfNeeded();
        this.hiddenEntityTypes.clear();
        this.hiddenEntityTypes.addAll(allCandidates);
        this.hiddenEntityTypes.remove(toShow);
        this.syncEntityTypesToConfig();
    }

    public void showAllBlocks()
    {
        this.loadFromConfigIfNeeded();
        this.hiddenBlocks.clear();
        this.syncBlocksToConfig();
    }

    public void hideAllBlocks(Collection<Block> allCandidates)
    {
        this.loadFromConfigIfNeeded();
        this.hiddenBlocks.clear();
        this.hiddenBlocks.addAll(allCandidates);
        this.syncBlocksToConfig();
    }

    public void showAllEntityTypes()
    {
        this.loadFromConfigIfNeeded();
        this.hiddenEntityTypes.clear();
        this.syncEntityTypesToConfig();
    }

    public void hideAllEntityTypes(Collection<EntityType<?>> allCandidates)
    {
        this.loadFromConfigIfNeeded();
        this.hiddenEntityTypes.clear();
        this.hiddenEntityTypes.addAll(allCandidates);
        this.syncEntityTypesToConfig();
    }

    private void loadFromConfigIfNeeded()
    {
        if (this.loaded)
        {
            return;
        }

        this.loaded = true;

        for (String str : Configs.Visuals.HIDDEN_SCHEMATIC_BLOCKS.getStrings())
        {
            Identifier id = Identifier.tryParse(str);

            if (id != null && BuiltInRegistries.BLOCK.containsKey(id))
            {
                this.hiddenBlocks.add(BuiltInRegistries.BLOCK.getValue(id));
            }
        }

        for (String str : Configs.Visuals.HIDDEN_SCHEMATIC_ENTITIES.getStrings())
        {
            Identifier id = Identifier.tryParse(str);

            if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
            {
                this.hiddenEntityTypes.add(BuiltInRegistries.ENTITY_TYPE.getValue(id));
            }
        }
    }

    private void syncBlocksToConfig()
    {
        List<String> values = new ArrayList<>();

        for (Block block : this.hiddenBlocks)
        {
            values.add(BuiltInRegistries.BLOCK.getKey(block).toString());
        }

        Configs.Visuals.HIDDEN_SCHEMATIC_BLOCKS.setStrings(values);
    }

    private void syncEntityTypesToConfig()
    {
        List<String> values = new ArrayList<>();

        for (EntityType<?> type : this.hiddenEntityTypes)
        {
            values.add(EntityType.getKey(type).toString());
        }

        Configs.Visuals.HIDDEN_SCHEMATIC_ENTITIES.setStrings(values);
    }
}
