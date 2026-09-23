package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntrySortable;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import fi.dy.masa.litematica.gui.GuiSchematicVisibility;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.render.schematic.SchematicVisibility;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

public class WidgetVisibilityEntry extends WidgetListEntrySortable<VisibilityEntry>
{
    @Nullable private final VisibilityEntry entry;
    private final WidgetListSchematicVisibility listWidget;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetVisibilityEntry(int x, int y, int width, int height, boolean isOdd,
            @Nullable VisibilityEntry entry, int listIndex, WidgetListSchematicVisibility listWidget)
    {
        super(x, y, width, height, entry, listIndex);

        this.columnCount = 2;
        this.entry = entry;
        this.isOdd = isOdd;
        this.listWidget = listWidget;

        if (entry != null)
        {
            int posX = x + width - 2;
            int posY = y + 1;

            // Note: placed from right to left
            posX = this.createSoloButton(posX, posY);

            WidgetCheckBox cb = new WidgetCheckBox(posX - 18, y + (height - 11) / 2,
                    Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, "");
            cb.setChecked(isHidden(entry) == false, false);
            cb.setListener(new CheckBoxListener(entry));
            this.addWidget(cb);

            this.buttonsStartX = posX - 20;
        }
        else
        {
            this.buttonsStartX = x + width;
        }
    }

    private int createSoloButton(int xRight, int y)
    {
        String label = StringUtils.translate("litematica.gui.button.schematic_visibility.solo");
        int width = this.getStringWidth(label) + 10;
        xRight -= width;
        this.addButton(new ButtonGeneric(xRight, y, width, 18, label), new SoloButtonListener(this.entry, this.listWidget));

        return xRight - 2;
    }

    private static boolean isHidden(VisibilityEntry entry)
    {
        if (entry.isBlock())
        {
            return SchematicVisibility.INSTANCE.isBlockHidden(entry.block());
        }
        else
        {
            return SchematicVisibility.INSTANCE.isEntityTypeHidden(entry.entityType());
        }
    }

    private static ItemStack getIcon(VisibilityEntry entry)
    {
        if (entry.isBlock())
        {
            ItemStack stack = new ItemStack(entry.block().asItem());
            return stack.isEmpty() ? new ItemStack(Items.BARRIER) : stack;
        }
        else
        {
            return SpawnEggItem.byId(entry.entityType())
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(new ItemStack(Items.EGG));
        }
    }

    @Override
    protected int getColumnPosX(int column)
    {
        int x1 = this.x + 22;
        int x2 = this.x + this.width - 220;

        return column == 1 ? x2 : x1;
    }

    @Override
    protected int getCurrentSortColumn()
    {
        return this.listWidget.getParentGui().getSortCriteria().ordinal();
    }

    @Override
    protected boolean getSortInReverse()
    {
        return this.listWidget.getParentGui().getSortInReverse();
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return false;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        if (super.onMouseClickedImpl(click, doubleClick))
        {
            return true;
        }

        if (this.entry != null)
        {
            return false;
        }

        int column = this.getMouseOverColumn((int) click.x(), (int) click.y());

        if (column == 0)
        {
            this.listWidget.getParentGui().setSortCriteria(GuiSchematicVisibility.SortCriteria.NAME);
            return true;
        }
        else if (column == 1)
        {
            this.listWidget.getParentGui().setSortCriteria(GuiSchematicVisibility.SortCriteria.COUNT);
            return true;
        }

        return false;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (this.entry != null && this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0101010);
        }
        else
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0303030);
        }

        int x1 = this.getColumnPosX(0);
        int x2 = this.getColumnPosX(1);
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.entry == null)
        {
            String header1 = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.schematic_visibility.name") + GuiBase.TXT_RST;
            String header2 = GuiBase.TXT_BOLD + StringUtils.translate("litematica.gui.label.schematic_visibility.count") + GuiBase.TXT_RST;

            this.drawString(ctx, x1, y, color, header1);
            this.drawString(ctx, x2, y, color, header2);
            this.renderColumnHeader(ctx, mouseX, mouseY, Icons.ARROW_DOWN, Icons.ARROW_UP);
        }
        else
        {
            int iconY = this.y + 3;
            RenderUtils.drawRect(ctx, x1 - 20, iconY, 16, 16, 0x20FFFFFF);
            ctx.renderItem(getIcon(this.entry), x1 - 20, iconY);

            this.drawString(ctx, x1, y, color, WidgetListSchematicVisibility.getName(this.entry));
            this.drawString(ctx, x2, y, color, String.valueOf(this.entry.count()));

            super.render(ctx, mouseX, mouseY, selected);
        }
    }

    private record CheckBoxListener(VisibilityEntry entry) implements ISelectionListener<WidgetCheckBox>
    {
        @Override
        public void onSelectionChange(WidgetCheckBox cb)
        {
            if (cb == null)
            {
                return;
            }

            boolean hidden = cb.isChecked() == false;

            if (this.entry.isBlock())
            {
                SchematicVisibility.INSTANCE.setBlockHidden(this.entry.block(), hidden);
            }
            else
            {
                SchematicVisibility.INSTANCE.setEntityTypeHidden(this.entry.entityType(), hidden);
            }

            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

    private record SoloButtonListener(VisibilityEntry entry, WidgetListSchematicVisibility listWidget)
            implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            LitematicaSchematic schematic = this.listWidget.getParentGui().getSchematicPlacement().getSchematic();

            if (this.entry.isBlock())
            {
                Collection<Block> all = WidgetListSchematicVisibility.getCandidateBlocks(schematic);
                SchematicVisibility.INSTANCE.showOnlyBlock(all, this.entry.block());
            }
            else
            {
                Collection<EntityType<?>> all = WidgetListSchematicVisibility.getCandidateEntityTypes(schematic);
                SchematicVisibility.INSTANCE.showOnlyEntityType(all, this.entry.entityType());
            }

            SchematicWorldRefresher.INSTANCE.updateAll();
            this.listWidget.refreshEntries();
        }
    }
}
