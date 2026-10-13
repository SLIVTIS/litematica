package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Pick materials only; never serialize a block entity or inspect storage inventories. */
public final class DecorativePickMaterials
{
    private DecorativePickMaterials() {}

    public static boolean isDecorative(Entity entity)
    {
        return entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Painting;
    }

    /** Only display contents and the four armor slots; never hands or arbitrary living entities. */
    public static List<ItemStack> collect(Entity entity)
    {
        if (entity instanceof ItemFrame frame)
        {
            return collectFrame(frame instanceof GlowItemFrame, frame.getItem());
        }
        if (entity instanceof ArmorStand stand)
        {
            return collectArmor(List.of(stand.getItemBySlot(EquipmentSlot.HEAD),
                    stand.getItemBySlot(EquipmentSlot.CHEST), stand.getItemBySlot(EquipmentSlot.LEGS),
                    stand.getItemBySlot(EquipmentSlot.FEET)));
        }
        if (entity instanceof Painting painting)
        {
            return collectPainting(painting.getVariant());
        }
        return List.of();
    }

    /** Vanilla getPickResult is plain; placement consumes this implicit component. */
    public static List<ItemStack> collectPainting(Holder<PaintingVariant> variant)
    {
        ItemStack stack = new ItemStack(Items.PAINTING);
        stack.set(DataComponents.PAINTING_VARIANT, variant);
        return List.of(stack);
    }

    public static List<ItemStack> collectFrame(boolean glow, ItemStack displayed)
    {
        List<ItemStack> materials = new ArrayList<>();
        materials.add(new ItemStack(glow ? Items.GLOW_ITEM_FRAME : Items.ITEM_FRAME));
        if (!displayed.isEmpty()) materials.add(displayed.copy());
        return materials;
    }

    public static List<ItemStack> collectArmor(List<ItemStack> armor)
    {
        List<ItemStack> materials = new ArrayList<>();
        materials.add(new ItemStack(Items.ARMOR_STAND));
        for (ItemStack stack : armor)
        {
            if (!stack.isEmpty()) materials.add(stack.copy());
        }
        return materials;
    }

    public static boolean isDecorative(BlockState state)
    {
        return state.getBlock() instanceof FlowerPotBlock || state.is(Blocks.CHISELED_BOOKSHELF) ||
               state.getBlock() instanceof ShelfBlock;
    }

    public static List<ItemStack> collect(BlockState state, @Nullable BlockEntity blockEntity, ItemStack fallback)
    {
        List<ItemStack> materials = new ArrayList<>();
        if (state.getBlock() instanceof FlowerPotBlock pot)
        {
            materials.add(new ItemStack(Items.FLOWER_POT));
            ItemStack plant = new ItemStack(pot.getPotted());
            if (!plant.isEmpty()) materials.add(plant);
        }
        else if (state.is(Blocks.CHISELED_BOOKSHELF))
        {
            materials.add(new ItemStack(Items.CHISELED_BOOKSHELF));
            materials.add(new ItemStack(Items.BOOK));
        }
        else if (state.getBlock() instanceof ShelfBlock)
        {
            materials.add(new ItemStack(state.getBlock()));
            if (blockEntity instanceof ShelfBlockEntity shelf)
            {
                for (ItemStack stack : shelf.getItems())
                {
                    if (!stack.isEmpty()) materials.add(stack.copy());
                }
            }
        }
        else
        {
            materials.add(fallback.copy());
        }
        return materials;
    }
}
