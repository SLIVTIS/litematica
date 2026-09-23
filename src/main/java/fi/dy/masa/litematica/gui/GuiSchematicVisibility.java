package fi.dy.masa.litematica.gui;

import java.util.Collection;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

import fi.dy.masa.litematica.gui.widgets.VisibilityEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVisibility;
import fi.dy.masa.litematica.gui.widgets.WidgetVisibilityEntry;
import fi.dy.masa.litematica.render.schematic.SchematicVisibility;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

public class GuiSchematicVisibility extends GuiListBase<VisibilityEntry, WidgetVisibilityEntry, WidgetListSchematicVisibility>
{
    private final SchematicPlacement placement;
    private Mode mode = Mode.BLOCKS;

    public GuiSchematicVisibility(SchematicPlacement placement)
    {
        super(10, 44);

        this.placement = placement;
        this.title = StringUtils.translate("litematica.gui.title.schematic_visibility");
    }

    public SchematicPlacement getSchematicPlacement()
    {
        return this.placement;
    }

    public Mode getMode()
    {
        return this.mode;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.getScreenWidth() - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = 24;

        x += this.createButton(x, y, ButtonListener.Type.MODE_BLOCKS) + 1;
        x += this.createButton(x, y, ButtonListener.Type.MODE_ENTITIES) + 6;
        this.createButton(x, y, ButtonListener.Type.SHOW_ALL);

        int xRight = this.getScreenWidth() - 10;
        xRight -= this.createButtonRight(xRight, y, ButtonListener.Type.HIDE_ALL) + 1;
    }

    private int createButton(int x, int y, ButtonListener.Type type)
    {
        String label = type.getDisplayName(this);
        int width = this.getStringWidth(label) + 10;
        this.addButton(new ButtonGeneric(x, y, width, 20, label), new ButtonListener(type, this));
        return width;
    }

    private int createButtonRight(int xRight, int y, ButtonListener.Type type)
    {
        String label = type.getDisplayName(this);
        int width = this.getStringWidth(label) + 10;
        this.addButton(new ButtonGeneric(xRight - width, y, width, 20, label), new ButtonListener(type, this));
        return width;
    }

    @Override
    protected WidgetListSchematicVisibility createListWidget(int listX, int listY)
    {
        return new WidgetListSchematicVisibility(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public record ButtonListener(Type type, GuiSchematicVisibility parent) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            switch (this.type)
            {
                case MODE_BLOCKS:
                    this.parent.mode = Mode.BLOCKS;
                    break;

                case MODE_ENTITIES:
                    this.parent.mode = Mode.ENTITIES;
                    break;

                case SHOW_ALL:
                {
                    if (this.parent.mode == Mode.BLOCKS)
                    {
                        SchematicVisibility.INSTANCE.showAllBlocks();
                    }
                    else
                    {
                        SchematicVisibility.INSTANCE.showAllEntityTypes();
                    }

                    SchematicWorldRefresher.INSTANCE.updateAll();
                    break;
                }

                case HIDE_ALL:
                {
                    LitematicaSchematic schematic = this.parent.placement.getSchematic();

                    if (this.parent.mode == Mode.BLOCKS)
                    {
                        Collection<Block> all = WidgetListSchematicVisibility.getCandidateBlocks(schematic);
                        SchematicVisibility.INSTANCE.hideAllBlocks(all);
                    }
                    else
                    {
                        Collection<EntityType<?>> all = WidgetListSchematicVisibility.getCandidateEntityTypes(schematic);
                        SchematicVisibility.INSTANCE.hideAllEntityTypes(all);
                    }

                    SchematicWorldRefresher.INSTANCE.updateAll();
                    break;
                }
            }

            this.parent.initGui();
        }

        public enum Type
        {
            MODE_BLOCKS     ("litematica.gui.button.schematic_visibility.mode_blocks"),
            MODE_ENTITIES   ("litematica.gui.button.schematic_visibility.mode_entities"),
            SHOW_ALL        ("litematica.gui.button.schematic_visibility.show_all"),
            HIDE_ALL        ("litematica.gui.button.schematic_visibility.hide_all");

            private final String translationKey;

            Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName(GuiSchematicVisibility gui)
            {
                String base = StringUtils.translate(this.translationKey);

                if ((this == MODE_BLOCKS && gui.mode == Mode.BLOCKS) ||
                    (this == MODE_ENTITIES && gui.mode == Mode.ENTITIES))
                {
                    return GuiBase.TXT_GREEN + base + GuiBase.TXT_RST;
                }

                return base;
            }
        }
    }

    public enum Mode
    {
        BLOCKS,
        ENTITIES
    }
}
