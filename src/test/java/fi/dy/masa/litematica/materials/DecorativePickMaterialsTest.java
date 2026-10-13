package fi.dy.masa.litematica.materials;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;

/** Standalone deterministic tests: no client, world, or mixins required. */
public class DecorativePickMaterialsTest
{
    public static void main(String[] args)
    {
        bootstrap();
        List<ItemStack> pot = DecorativePickMaterials.collect(Blocks.POTTED_DANDELION.defaultBlockState(), null, new ItemStack(Items.DANDELION));
        check(pot.size() == 2 && pot.get(0).is(Items.FLOWER_POT) && pot.get(1).is(Items.DANDELION), "potted plant needs pot plus plant");
        check(DecorativePickMaterials.collect(Blocks.FLOWER_POT.defaultBlockState(), null, new ItemStack(Items.FLOWER_POT)).size() == 1, "empty pot");
        List<ItemStack> books = DecorativePickMaterials.collect(Blocks.CHISELED_BOOKSHELF.defaultBlockState(), null, new ItemStack(Items.CHISELED_BOOKSHELF));
        check(books.size() == 2 && books.get(1).is(Items.BOOK) && books.get(1).getCount() == 1, "one generic book, without inspecting stored books");
        ShelfBlockEntity shelf = new ShelfBlockEntity(BlockPos.ZERO, Blocks.OAK_SHELF.defaultBlockState());
        shelf.getItems().set(0, new ItemStack(Items.DIAMOND, 3));
        List<ItemStack> display = DecorativePickMaterials.collect(Blocks.OAK_SHELF.defaultBlockState(), shelf, new ItemStack(Items.OAK_SHELF));
        check(display.size() == 2 && display.get(1).getCount() == 3, "displayed stack count");
        display.get(1).shrink(1);
        check(shelf.getItems().get(0).getCount() == 3, "collector copies stacks");
        check(DecorativePickMaterials.collect(Blocks.OAK_SHELF.defaultBlockState(), null, new ItemStack(Items.OAK_SHELF)).size() == 1, "missing shelf data");
        check(DecorativePickMaterials.collect(Blocks.CHEST.defaultBlockState(), shelf, new ItemStack(Items.CHEST)).size() == 1, "storage is not decorative");
        List<ItemStack> frame = DecorativePickMaterials.collectFrame(false, new ItemStack(Items.DIAMOND, 2));
        check(frame.size() == 2 && frame.get(0).is(Items.ITEM_FRAME) && frame.get(1).getCount() == 2, "normal frame and displayed stack");
        frame.get(1).shrink(1);
        List<ItemStack> glow = DecorativePickMaterials.collectFrame(true, ItemStack.EMPTY);
        check(glow.size() == 1 && glow.get(0).is(Items.GLOW_ITEM_FRAME), "empty glow frame retains variant");
        check(DecorativePickMaterials.collectFrame(false, ItemStack.EMPTY).get(0).is(Items.ITEM_FRAME), "empty normal frame");
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        List<ItemStack> armor = DecorativePickMaterials.collectArmor(List.of(helmet, new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)));
        check(armor.size() == 5 && armor.get(0).is(Items.ARMOR_STAND), "stand with all four armor slots");
        armor.get(1).shrink(1);
        check(helmet.getCount() == 1, "armor is copied");
        check(DecorativePickMaterials.collectArmor(List.of(ItemStack.EMPTY, helmet, ItemStack.EMPTY, ItemStack.EMPTY)).size() == 2, "empty armor slots ignored");
        check(DecorativePickMaterials.collectArmor(List.of()).size() == 1, "empty stand retains base");
        var variant = paintingVariant("donkey_kong", 4, 3);
        var other = paintingVariant("pointer", 4, 4);
        ItemStack painting = DecorativePickMaterials.collectPainting(variant).getFirst();
        check(painting.is(Items.PAINTING) && painting.get(net.minecraft.core.component.DataComponents.PAINTING_VARIANT) == variant, "nondefault painting must carry exact variant");
        check(painting.get(net.minecraft.core.component.DataComponents.ENTITY_DATA) == null, "painting has no copied entity NBT");
        ItemStack different = DecorativePickMaterials.collectPainting(other).getFirst();
        check(!ItemStack.isSameItemSameComponents(painting, different), "different painting variants cannot merge");
        check(!ItemStack.isSameItemSameComponents(painting, new ItemStack(Items.PAINTING)), "plain painting cannot satisfy exact variant");
        var planned = fi.dy.masa.litematica.util.InventoryUtils.planDecorativeDelivery(
                List.of(different, ItemStack.EMPTY), List.of(painting)).orElseThrow();
        check(planned.get(1).get(net.minecraft.core.component.DataComponents.PAINTING_VARIANT) == variant, "delivery preserves exact painting component");
        check(fi.dy.masa.litematica.util.InventoryUtils.planDecorativeDelivery(List.of(different), List.of(painting)).isEmpty(), "full different variant is not reused");
        System.out.println("DecorativePickMaterialsTest passed");
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.decoration.painting.PaintingVariant> paintingVariant(String name, int width, int height)
    {
        return net.minecraft.core.Holder.direct(new net.minecraft.world.entity.decoration.painting.PaintingVariant(
                width, height, net.minecraft.resources.Identifier.withDefaultNamespace(name), java.util.Optional.empty(), java.util.Optional.empty()));
    }

    public static void bootstrap()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Minimal deterministic fixture: real 26.3 component defaults need data-pack tags.
        // These tests exercise collection/planning, not vanilla data-pack initialization.
        net.minecraft.core.registries.BuiltInRegistries.ITEM.listElements().forEach(holder ->
                holder.bindComponents(net.minecraft.core.component.DataComponentMap.builder()
                        .set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 64).build()));
    }

    public static void check(boolean condition, String message)
    {
        if (!condition) throw new AssertionError(message);
    }
}
