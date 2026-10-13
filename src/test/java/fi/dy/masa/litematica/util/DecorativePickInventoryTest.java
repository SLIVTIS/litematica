package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import fi.dy.masa.litematica.materials.DecorativePickMaterialsTest;
import static fi.dy.masa.litematica.materials.DecorativePickMaterialsTest.check;

public class DecorativePickInventoryTest
{
    public static void main(String[] args)
    {
        DecorativePickMaterialsTest.bootstrap();
        List<ItemStack> inventory = new ArrayList<>(List.of(new ItemStack(Items.STONE), ItemStack.EMPTY, ItemStack.EMPTY));
        List<ItemStack> materials = List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION));
        List<ItemStack> plan = InventoryUtils.planDecorativeDelivery(inventory, materials).orElseThrow(() -> new AssertionError("empty slots must accept base plus plant"));
        check(plan.get(0).is(Items.STONE) && plan.get(1).is(Items.FLOWER_POT) && plan.get(2).is(Items.DANDELION), "unrelated slot preserved");
        check(inventory.get(1).isEmpty(), "planning does not mutate inventory");
        check(InventoryUtils.planDecorativeDelivery(List.of(new ItemStack(Items.STONE), ItemStack.EMPTY), materials).isEmpty(), "insufficient space aborts whole delivery");
        check(InventoryUtils.planDecorativeDelivery(plan, materials).isPresent(), "full inventory reuses matching items");
        check(InventoryUtils.planDecorativeDelivery(List.of(new ItemStack(Items.STONE)), materials).isEmpty(), "full unrelated inventory is safe");
        List<ItemStack> counted = InventoryUtils.planDecorativeDelivery(List.of(new ItemStack(Items.DIAMOND, 2), ItemStack.EMPTY),
                List.of(new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.DIAMOND, 2))).orElseThrow();
        check(counted.get(0).getCount() == 2 && counted.get(1).getCount() == 3, "aggregate display counts and reuse existing count");
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("display"));
        List<ItemStack> namedPlan = InventoryUtils.planDecorativeDelivery(List.of(new ItemStack(Items.DIAMOND), ItemStack.EMPTY), List.of(named)).orElseThrow();
        check(namedPlan.get(1).get(DataComponents.CUSTOM_NAME) != null, "components must match for reuse");
        check(InventoryUtils.planDecorativeDelivery(List.of(ItemStack.EMPTY, ItemStack.EMPTY),
                List.of(new ItemStack(Items.DIAMOND, 65))).orElseThrow().get(1).getCount() == 1, "split at maximum stack size");
        verifyAllHotbarRegression();
        verifyConfiguredDelivery();
        verifyHotbarCapacityAndReuse();
        verifyPickGuards();
        System.out.println("DecorativePickInventoryTest passed");
    }

    private static void verifyAllHotbarRegression()
    {
        List<ItemStack> inventory = fullInventory();
        for (int slot = 9; slot < 36; ++slot) inventory.set(slot, ItemStack.EMPTY);
        List<ItemStack> materials = List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION));
        var plan = InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of()).orElseThrow();
        check(plan.items().subList(0, 9).stream().anyMatch(s -> s.is(Items.FLOWER_POT)) &&
                plan.items().subList(0, 9).stream().anyMatch(s -> s.is(Items.DANDELION)),
                "pot AND plant must reach hotbar when main inventory is free");
    }

    private static List<ItemStack> fullInventory()
    {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < 36; ++slot) items.add(new ItemStack(Items.STONE, 64));
        return items;
    }

    private static void verifyConfiguredDelivery()
    {
        List<ItemStack> materials = List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION));
        List<ItemStack> inventory = fullInventory();
        inventory.set(10, ItemStack.EMPTY);
        var one = InventoryUtils.planDecorativePick(inventory, List.of(materials.getFirst()), List.of(2), 2, List.of(2)).orElseThrow();
        check(one.items().get(10).is(Items.STONE) && one.items().get(2).is(Items.FLOWER_POT), "single free main slot preserves displaced stack");
        inventory.set(11, ItemStack.EMPTY);
        var safe = InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of(2))
                .orElseThrow(() -> new AssertionError("full protected hotbar with main space must relocate both displaced stacks"));
        check(safe.items().get(safe.selectedSlot()).is(Items.FLOWER_POT), "configured base selected");
        check(safe.items().get(10).is(Items.STONE) && safe.items().get(11).is(Items.STONE), "both displaced hotbar stacks preserved in main slots");
        assertHotbar(safe.items(), materials);
        inventory = fullInventory();
        inventory.set(12, new ItemStack(Items.FLOWER_POT));
        inventory.set(13, new ItemStack(Items.DANDELION));
        var existing = InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of(2)).orElseThrow();
        check(existing.items().get(12).is(Items.STONE) && existing.items().get(13).is(Items.STONE), "full inventory moves both existing stacks with safe swaps");
        assertHotbar(existing.items(), materials);
        inventory = fullInventory();
        var overwrite = InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of(2)).orElseThrow();
        check(overwrite.items().get(overwrite.selectedSlot()).is(Items.FLOWER_POT), "full unrelated inventory allows configured overwrite");
        for (int slot = 0; slot < 36; ++slot)
            if (slot != 2 && slot != 4) check(ItemStack.matches(inventory.get(slot), overwrite.items().get(slot)), "only configured slots overwritten: " + slot);
        check(inventory.get(2).is(Items.STONE), "successful plan leaves input unchanged");
        check(InventoryUtils.planDecorativePick(inventory, materials, List.of(2), 2, List.of()).isEmpty(), "insufficient configured capacity fails atomically");
        check(inventory.stream().allMatch(s -> s.is(Items.STONE)), "failed plan leaves input unchanged");
        check(InventoryUtils.planDecorativePick(existing.items(), materials, List.of(), 2, List.of()).isEmpty(), "empty pick slots fail even with existing base");
        inventory.set(2, new ItemStack(Items.DANDELION, 3));
        var protectedCount = InventoryUtils.planDecorativePick(inventory,
                List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION, 3)), List.of(2, 4), 2, List.of()).orElseThrow();
        check(protectedCount.items().stream().filter(s -> s.is(Items.DANDELION)).mapToInt(ItemStack::getCount).sum() == 3, "required material count survives overwrite and swap");
        ItemStack named = new ItemStack(Items.STONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("required"));
        inventory.set(2, named);
        check(InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.FLOWER_POT), named), List.of(2), 2, List.of()).isEmpty(), "required components cannot be erased for capacity");
        inventory = fullInventory();
        var toolPreference = InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.FLOWER_POT)), List.of(2, 4), 2, List.of(2)).orElseThrow();
        check(toolPreference.selectedSlot() == 4 && toolPreference.items().get(2).is(Items.STONE), "nonprotected overwrite preferred");
        check(InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.FLOWER_POT)), List.of(36, -1, 9), 2, List.of()).isEmpty(), "nonhotbar indices never authorize destruction");
        inventory.set(0, ItemStack.EMPTY);
        var constrained = InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.FLOWER_POT)), List.of(4), 0, List.of()).orElseThrow();
        check(constrained.selectedSlot() == 4 && constrained.items().get(0).isEmpty(), "unconfigured hotbar empties remain untouched");
        inventory = fullInventory();
        inventory.set(2, new ItemStack(Items.DANDELION, 3));
        inventory.set(4, new ItemStack(Items.DANDELION, 3));
        var surplus = InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of()).orElseThrow();
        check(surplus.items().stream().filter(s -> s.is(Items.DANDELION)).mapToInt(ItemStack::getCount).sum() == 6, "surplus matching stacks merge before configured deletion");
        ItemStack painting = fi.dy.masa.litematica.materials.DecorativePickMaterials.collectPainting(
                DecorativePickMaterialsTest.paintingVariant("donkey_kong", 4, 3)).getFirst();
        inventory = fullInventory();
        inventory.set(2, painting);
        check(InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.ITEM_FRAME), painting), List.of(2), 2, List.of()).isEmpty(), "exact required painting variant protected during overwrite planning");
        check(InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.ITEM_FRAME), painting), List.of(2, 4), 2, List.of()).orElseThrow()
                .items().stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, painting)), "exact painting survives permitted unrelated overwrite");
        inventory = fullInventory();
        inventory.set(2, new ItemStack(Items.FLOWER_POT));
        inventory.set(12, new ItemStack(Items.DANDELION, 2));
        var toppedUp = InventoryUtils.planDecorativePick(inventory,
                List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION, 3)), List.of(2, 4), 2, List.of()).orElseThrow();
        check(toppedUp.items().get(4).is(Items.DANDELION) && toppedUp.items().get(4).getCount() == 3 && toppedUp.items().get(2).is(Items.FLOWER_POT), "main supplement moved and topped up in hotbar");
        check(inventory.get(12).getCount() == 2, "top-up planning does not mutate existing counts");
        inventory.add(new ItemStack(Items.DIAMOND_HELMET));
        check(InventoryUtils.planDecorativePick(inventory, materials, List.of(2, 4), 2, List.of()).isEmpty(), "equipment-bearing input rejected");
    }

    private static void assertHotbar(List<ItemStack> inventory, List<ItemStack> materials)
    {
        for (ItemStack material : materials)
        {
            int required = materials.stream().filter(s -> ItemStack.isSameItemSameComponents(s, material)).mapToInt(ItemStack::getCount).sum();
            int present = inventory.subList(0, 9).stream().filter(s -> ItemStack.isSameItemSameComponents(s, material)).mapToInt(ItemStack::getCount).sum();
            check(present >= required, "all aggregated item/components/counts must be in hotbar: " + material);
        }
    }

    private static void verifyHotbarCapacityAndReuse()
    {
        List<ItemStack> inventory = fullInventory();
        for (int slot = 9; slot < 36; ++slot) inventory.set(slot, ItemStack.EMPTY);
        List<ItemStack> pot = List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION));
        check(InventoryUtils.planDecorativePick(inventory, pot, List.of(2), 2, List.of()).isEmpty(), "main capacity cannot hide insufficient hotbar capacity");
        check(inventory.get(2).is(Items.STONE) && inventory.get(9).isEmpty(), "capacity failure changes nothing");
        inventory.set(0, new ItemStack(Items.DANDELION, 3));
        var readonly = InventoryUtils.planDecorativePick(inventory, pot, List.of(2), 2, List.of()).orElseThrow();
        assertHotbar(readonly.items(), pot);
        check(ItemStack.matches(readonly.items().get(0), inventory.get(0)), "nonconfigured supplement reused read-only");
        inventory.set(0, new ItemStack(Items.DANDELION));
        List<ItemStack> more = List.of(new ItemStack(Items.FLOWER_POT), new ItemStack(Items.DANDELION, 3));
        check(InventoryUtils.planDecorativePick(inventory, more, List.of(2), 2, List.of()).isEmpty(), "nonconfigured stack cannot be topped up");
        var topup = InventoryUtils.planDecorativePick(inventory, more, List.of(2, 4), 2, List.of()).orElseThrow();
        assertHotbar(topup.items(), more);
        check(topup.items().get(0).getCount() == 1 && topup.items().get(4).getCount() == 2, "deficit supplied only in configured slot");
        inventory.set(0, new ItemStack(Items.FLOWER_POT));
        var base = InventoryUtils.planDecorativePick(inventory, pot, List.of(4), 4, List.of()).orElseThrow();
        check(base.selectedSlot() == 0 && ItemStack.matches(base.items().get(0), inventory.get(0)), "nonconfigured base selected without edits");

        List<ItemStack> armor = new ArrayList<>();
        armor.add(new ItemStack(Items.ARMOR_STAND));
        for (var item : List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS))
        {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.MAX_STACK_SIZE, 1); // Real equipped armor is nonstackable.
            armor.add(stack);
        }
        inventory = fullInventory();
        var equipped = InventoryUtils.planDecorativePick(inventory, armor, List.of(0, 1, 2, 3, 4), 0, List.of(1, 2)).orElseThrow();
        assertHotbar(equipped.items(), armor);
        check(equipped.items().get(equipped.selectedSlot()).is(Items.ARMOR_STAND), "stand stays selected");
        check(InventoryUtils.planDecorativePick(inventory, armor, List.of(0, 1, 2, 3), 0, List.of()).isEmpty(), "five stand materials need five slots");
        List<ItemStack> freeMain = fullInventory();
        for (int slot = 9; slot < 36; ++slot) freeMain.set(slot, ItemStack.EMPTY);
        var safeArmor = InventoryUtils.planDecorativePick(freeMain, armor, List.of(0, 1, 2, 3, 4), 0, List.of()).orElseThrow();
        assertHotbar(safeArmor.items(), armor);
        check(safeArmor.items().stream().filter(s -> s.is(Items.STONE)).mapToInt(ItemStack::getCount).sum() == 9 * 64, "free main preserves all five displaced armor-pick stacks");
        List<ItemStack> doubledArmor = List.of(new ItemStack(Items.ARMOR_STAND), armor.get(1), armor.get(1));
        var nonstackable = InventoryUtils.planDecorativePick(inventory, doubledArmor, List.of(0, 1, 2), 0, List.of()).orElseThrow();
        assertHotbar(nonstackable.items(), doubledArmor);
        check(nonstackable.items().get(1).getCount() == 1 && nonstackable.items().get(2).getCount() == 1, "nonstackable repeats occupy separate slots");

        List<ItemStack> repeats = List.of(new ItemStack(Items.OAK_SHELF), new ItemStack(Items.DIAMOND, 40), new ItemStack(Items.DIAMOND, 30));
        inventory.set(12, new ItemStack(Items.DIAMOND, 64));
        inventory.set(13, new ItemStack(Items.DIAMOND, 6));
        var counted = InventoryUtils.planDecorativePick(inventory, repeats, List.of(2, 4, 6), 2, List.of()).orElseThrow();
        assertHotbar(counted.items(), repeats);
        check(counted.items().stream().filter(s -> s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 70, "existing main counts move rather than duplicate");
        var repeated = InventoryUtils.planDecorativePick(counted.items(), repeats, List.of(2, 4, 6), 2, List.of()).orElseThrow();
        for (int slot = 0; slot < 36; ++slot) check(ItemStack.matches(counted.items().get(slot), repeated.items().get(slot)), "repeat is idempotent: " + slot);
        check(InventoryUtils.planDecorativePick(inventory, List.of(new ItemStack(Items.DIAMOND, Integer.MAX_VALUE)), List.of(2, 4), 2, List.of()).isEmpty(), "oversized count rejects boundedly");

        ItemStack first = fi.dy.masa.litematica.materials.DecorativePickMaterials.collectPainting(DecorativePickMaterialsTest.paintingVariant("donkey_kong", 4, 3)).getFirst();
        ItemStack second = fi.dy.masa.litematica.materials.DecorativePickMaterials.collectPainting(DecorativePickMaterialsTest.paintingVariant("pointer", 4, 4)).getFirst();
        inventory = fullInventory();
        inventory.set(12, second);
        for (var frame : List.of(Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME))
        {
            List<ItemStack> variants = List.of(new ItemStack(frame), first, second);
            var paintings = InventoryUtils.planDecorativePick(inventory, variants, List.of(2, 4, 6), 2, List.of()).orElseThrow();
            assertHotbar(paintings.items(), variants);
            check(paintings.items().get(paintings.selectedSlot()).is(frame), "frame variant selected");
            check(paintings.items().stream().filter(s -> ItemStack.isSameItemSameComponents(s, second)).mapToInt(ItemStack::getCount).sum() == 1, "exact existing painting reused");
            check(InventoryUtils.planDecorativePick(inventory, variants, List.of(2, 4), 2, List.of()).isEmpty(), "component-distinct paintings require distinct slots");
        }
    }

    /** Source-level regression checks only: live ray tracing/callbacks need a Minecraft client. */
    private static void verifyPickGuards()
    {
        try
        {
            String inventorySource = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/fi/dy/masa/litematica/util/InventoryUtils.java"));
            String creativeDelivery = inventorySource.substring(inventorySource.indexOf("public static void pickDecorativeMaterials"), inventorySource.indexOf("private static boolean canPickToSlot"));
            check(creativeDelivery.indexOf("!EntityUtils.isCreativeMode(mc.player)") < creativeDelivery.indexOf("planDecorativePick(before"), "survival cannot enter grant planner");
            check(creativeDelivery.contains("getNonEquipmentItems()"), "equipment and offhand excluded from live delivery");
            String world = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/fi/dy/masa/litematica/util/WorldUtils.java"));
            world = world.substring(world.indexOf("SchematicPickBlockEventHandler.getInstance().resetCancelled()"));
            int firstHidden = world.indexOf("SchematicVisibility.INSTANCE.isBlockHidden");
            int start = world.indexOf("onSchematicPickBlockStart");
            int secondHidden = world.indexOf("SchematicVisibility.INSTANCE.isBlockHidden", firstHidden + 1);
            int gather = world.indexOf("onSchematicPickBlockPreGather");
            int prePick = world.indexOf("onSchematicPickBlockPrePick");
            int handler = world.indexOf("executePickBlockHandler");
            int delivery = world.indexOf("InventoryUtils.pickDecorativeMaterials", handler);
            check(firstHidden >= 0 && firstHidden < start && secondHidden > start && secondHidden < gather, "hidden blocks excluded before callbacks and after target refresh");
            check(gather < prePick && prePick < handler && handler < delivery, "cancellation and slot handler precede delivery");
            check(world.contains("!stackOverridden && EntityUtils.isCreativeMode(mc.player)"), "survival and overrides bypass supplemental delivery");
            check(world.contains("stackOverridden |= SchematicPickBlockEventHandler.getInstance().hasSlotHandler()"), "custom slot handlers cannot double-grant supplements on legacy fallthrough");
            check(world.contains("stackOverridden |= !ItemStack.matches(gatheredStack, stack)"), "mutated callback stack suppresses supplements");
            check(world.contains("mc, !DecorativePickMaterials.isDecorative(state)"), "decorative overrides cannot auto-copy inventory NBT");
            int entityTarget = world.indexOf("EntityHitResult decorativeEntity");
            String entityPick = world.substring(entityTarget, world.indexOf("if (pos != null)", entityTarget));
            check(entityTarget > start && entityPick.contains("getDecorativeEntityPick"), "entity re-resolved after start cancellation");
            check(!entityPick.contains("onSchematicPickBlockPreGather") && !entityPick.contains("onSchematicPickBlockPrePick") &&
                    !entityPick.contains("executePickBlockHandler"), "no fabricated block callbacks for entities");
            check(entityPick.indexOf("hasSlotHandler()) return true") < entityPick.indexOf("InventoryUtils.pickDecorativeMaterials"), "entity redirects fail closed");
            check(entityPick.contains("if (EntityUtils.isCreativeMode(mc.player))") &&
                    entityPick.contains("world, mc, false)"), "entity survival uses selection-only path without NBT");
            String ray = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/fi/dy/masa/litematica/util/RayTraceUtils.java"));
            ray = ray.substring(ray.indexOf("public static EntityHitResult getDecorativeEntityPick"), ray.indexOf("static double decorativeHitDistance"));
            check(ray.contains("RENDER_SCHEMATIC_ENTITIES") && ray.contains("ENABLE_SCHEMATIC_RENDERING") && ray.contains("ENABLE_RENDERING"), "entity rendering gates");
            check(ray.contains("isEntityTypeHidden") && ray.contains("isPositionWithinRange") && ray.contains("checkIfChunkShouldRender"), "entity visibility/layer/placement gates");
            check(ray.contains("!closest && !hasRealBlock") && ray.contains("clientWorld.getEntities(player, search)"), "furthest requires real block and respects real entities");
            check(ray.contains("renderer.isEntityRendered(candidate)"), "target selection requires rendered membership");
            String renderer = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/fi/dy/masa/litematica/render/schematic/WorldRendererSchematic.java"));
            String membership = renderer.substring(renderer.indexOf("public boolean isEntityRendered"), renderer.indexOf("private void clearRenderedEntityMembership"));
            check(membership.contains("entity.level() != this.world") && membership.contains("entity.isRemoved()"), "membership belongs to current schematic entity");
            check(membership.contains("renderedEntityClientWorld != this.mc.level") && membership.contains("renderedEntityTick != this.mc.level.getGameTime()"), "stale client world or tick fails closed");
            check(membership.contains("renderedEntityCameraPosition.equals(camera.position())") && membership.contains("renderedEntityCameraRotation.equals(camera.rotation())"), "changed camera fails closed");
            check(membership.contains("entity.getUUID().equals(this.renderedEntities.get(entity.position()))"), "membership requires matching UUID and position");
            for (String signature : List.of("public void setWorldAndLoadRenderers", "public void loadRenderers", "protected void stopChunkUpdates", "public void prepareEntities"))
            {
                int method = renderer.indexOf(signature);
                check(method >= 0 && renderer.substring(method, renderer.indexOf("\n    @Override", method)).contains("this.clearRenderedEntityMembership()"), "snapshot invalidated by " + signature);
            }
            String frameCleanup = renderer.substring(renderer.indexOf("public void clearWorldRenderStates"), renderer.indexOf("public void renderEntityDebugHitboxes"));
            check(!frameCleanup.contains("this.clearRenderedEntityMembership()"), "routine frame cleanup retains input snapshot");
            String preparation = renderer.substring(renderer.indexOf("public void prepareEntities"), renderer.indexOf("public void renderEntities"));
            check(preparation.indexOf("clearRenderedEntityMembership()") < preparation.indexOf("renderEntitiesStartupCounter") &&
                    preparation.indexOf("renderedEntityClientWorld = this.mc.level") > preparation.indexOf("this.renderedEntities.put(entityTmp.position()"), "snapshot published only after successful preparation");
            String collector = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/fi/dy/masa/litematica/materials/DecorativePickMaterials.java"));
            check(collector.contains("entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Painting"), "decorative entity allowlist includes paintings");
            check(collector.contains("collectPainting(painting.getVariant())"), "entity collector forwards actual painting variant");
            for (String slot : List.of("HEAD", "CHEST", "LEGS", "FEET"))
                check(collector.contains("stand.getItemBySlot(EquipmentSlot." + slot + ")"), "occupied armor slot mapped: " + slot);
            check(!collector.contains("EquipmentSlot.MAINHAND") && !collector.contains("EquipmentSlot.OFFHAND"), "stand hands excluded");
        }
        catch (java.io.IOException e)
        {
            throw new AssertionError("Unable to inspect pick integration guards", e);
        }
    }
}
