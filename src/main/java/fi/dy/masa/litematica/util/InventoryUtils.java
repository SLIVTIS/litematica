package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import fi.dy.masa.malilib.render.InventoryOverlayRefresher;
import fi.dy.masa.malilib.util.EquipmentUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.EntityDataManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class InventoryUtils
{
    private static final List<Integer> PICK_BLOCKABLE_SLOTS = new ArrayList<>();
    private static int nextPickSlotIndex;
    private static Pair<BlockPos, InventoryOverlayContext> lastBlockEntityContext = null;

    public static void setPickBlockableSlots(String configStr)
    {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");

        for (String str : parts)
        {
            try
            {
                int slotNum = Integer.parseInt(str) - 1;

                if (Inventory.isHotbarSlot(slotNum) &&
                    PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    public static void setPickedItemToHand(ItemStack stack, Minecraft mc)
    {
        if (mc.player == null) return;
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setPickedItemToHand(int sourceSlot, ItemStack stack, Minecraft mc)
    {
        if (mc.player == null) return;
        Player player = mc.player;
        Inventory inventory = player.getInventory();

        if (Inventory.isHotbarSlot(sourceSlot))
        {
            inventory.setSelectedSlot(sourceSlot);
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return;
            }

            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || Inventory.isHotbarSlot(sourceSlot) == false)
            {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1)
            {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1)
            {
                inventory.setSelectedSlot(hotbarSlot);

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.getNonEquipmentItems().set(hotbarSlot, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }

                EasyPlaceUtils.setEasyPlaceLastPickBlockTime();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }
    }

	/**
	 * Simulates the 'setPickedItemToHand' logic but does not actually swap the item.
	 * @param stack (Reference Stack)
	 * @param mc ()
	 * @return (Slot Number, or -1)
	 */
	public static int getPickedItemHandSlotNoSwap(ItemStack stack, Minecraft mc)
	{
		if (mc.player == null) return -1;
		int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
		return getPickedItemHandSlotNoSwap(slotNum, stack, mc);
	}

	/**
	 * Simulates the 'setPickedItemToHand' logic but does not actually swap the item.
	 * @param sourceSlot ()
	 * @param stack (Reference Stack)
	 * @param mc ()
	 * @return (Slot Number, or -1)
	 */
	public static int getPickedItemHandSlotNoSwap(int sourceSlot, ItemStack stack, Minecraft mc)
	{
		if (mc.player == null) return -1;
		Player player = mc.player;
		Inventory inventory = player.getInventory();

		if (Inventory.isHotbarSlot(sourceSlot))
		{
			inventory.setSelectedSlot(sourceSlot);
		}
		else
		{
			if (PICK_BLOCKABLE_SLOTS.size() == 0)
			{
				InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
				return -1;
			}

			int hotbarSlot = sourceSlot;

			if (sourceSlot == -1 || Inventory.isHotbarSlot(sourceSlot) == false)
			{
				hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
			}

			if (hotbarSlot == -1)
			{
				hotbarSlot = getPickBlockTargetSlot(player);
			}

			if (hotbarSlot != -1)
			{
				int resultSlot = -1;
				inventory.setSelectedSlot(hotbarSlot);

				if (EntityUtils.isCreativeMode(player))
				{
					resultSlot = hotbarSlot;
				}
				else
				{
					resultSlot = getMainHandSlotForItem(stack.copy(), mc);
				}

				// Can still be -1
				if (resultSlot != -1)
				{
					EasyPlaceUtils.setEasyPlaceLastPickBlockTime();
				}

				return resultSlot;
			}
			else
			{
				InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
			}
		}

		return -1;
	}

	/**
	 * Simulates the MaLiLib -> swapItemToMainHand() without actually swapping the item.
	 * @param stackReference ()
	 * @param mc ()
	 * @return (The Slot ID or -1)
	 */
	private static int getMainHandSlotForItem(ItemStack stackReference, Minecraft mc)
	{
		Player player = mc.player;
		if (mc.player == null) return -1;
		boolean isCreative = player.hasInfiniteMaterials();

		if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(stackReference, player.getMainHandItem()))
		{
			return -1;
		}

		if (isCreative)
		{
			player.getInventory().setSelectedSlot(player.getInventory().getSuitableHotbarSlot());
			return 36 + player.getInventory().getSelectedSlot();
		}
		else
		{
			return fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.inventoryMenu, stackReference, true);
		}
	}

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               Level schematicWorld, Minecraft mc)
    {
        schematicWorldPickBlock(stack, pos, schematicWorld, mc, true);
    }

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               Level schematicWorld, Minecraft mc, boolean copyBlockEntityNbt)
    {
        if (mc.player == null || mc.gameMode == null || mc.level == null)
        {
            return;
        }
        if (stack.isEmpty() == false)
        {
            Inventory inv = mc.player.getInventory();
            stack = stack.copy();

            if (EntityUtils.isCreativeMode(mc.player))
            {
                BlockEntity te = schematicWorld.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (copyBlockEntityNbt && GuiBase.isCtrlDown() && te != null && mc.level.isEmptyBlock(pos))
                {
                    //te.setStackNbt(stack, schematicWorld.getRegistryManager());
                    BlockUtils.setStackNbt(stack, te, schematicWorld.registryAccess());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.gameMode.handleCreativeModeItemAdd(mc.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inv.getSelectedSlot());

                //return true;
            }
            else
            {
                int slot = inv.findSlotMatchingItem(stack);
                boolean shouldPick = inv.getSelectedSlot() != slot;

                if (shouldPick && slot != -1)
                {
                    setPickedItemToHand(stack, mc);
                }
                else if (slot == -1 && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
                {
                    slot = findSlotWithBoxWithItem(mc.player.inventoryMenu, stack, false);

                    if (slot != -1)
                    {
                        ItemStack boxStack = mc.player.inventoryMenu.slots.get(slot).getItem();
                        setPickedItemToHand(boxStack, mc);
                    }
                }

                //return shouldPick == false || canPick;
            }
        }
    }

    /** Atomic empty-slot-only plan. Existing matching counts satisfy requirements without copying them. */
    public static Optional<List<ItemStack>> planDecorativeDelivery(List<ItemStack> inventory, List<ItemStack> materials)
    {

        List<ItemStack> required = new ArrayList<>();
        for (ItemStack material : materials)
        {
            if (material.isEmpty()) continue;
            ItemStack matching = null;
            for (ItemStack stack : required)
            {
                if (ItemStack.isSameItemSameComponents(stack, material))
                {
                    matching = stack;
                    break;
                }
            }
            if (matching == null) required.add(material.copy());
            else matching.grow(material.getCount());
        }

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : inventory) result.add(stack.copy());
        for (ItemStack material : required)
        {
            int missing = material.getCount();
            for (ItemStack stack : inventory)
            {
                if (ItemStack.isSameItemSameComponents(stack, material)) missing -= stack.getCount();
            }
            for (int slot = 0; slot < result.size() && missing > 0; ++slot)
            {
                if (result.get(slot).isEmpty())
                {
                    int count = Math.min(missing, material.getMaxStackSize());
                    result.set(slot, material.copyWithCount(count));
                    missing -= count;
                }
            }
            if (missing > 0) return Optional.empty();
        }
        return Optional.of(result);
    }

    public record DecorativePickPlan(List<ItemStack> items, int selectedSlot) {}

    public static Optional<DecorativePickPlan> planDecorativePick(List<ItemStack> inventory,
            List<ItemStack> materials, List<Integer> pickSlots, int selectedSlot, List<Integer> protectedSlots)
    {
        List<Integer> slots = pickSlots.stream().filter(s -> s >= 0 && s < 9 && s < inventory.size()).distinct().toList();
        if (slots.isEmpty() || materials.isEmpty() || materials.get(0).isEmpty() || inventory.size() > 36) return Optional.empty();

        List<ItemStack> required = new ArrayList<>();
        for (ItemStack material : materials)
        {
            if (material.isEmpty()) continue;
            ItemStack matching = required.stream().filter(s -> ItemStack.isSameItemSameComponents(s, material)).findFirst().orElse(null);
            long count = material.getCount() + (matching == null ? 0L : matching.getCount());
            // No requirement can exceed nine full stacks. Bound counts before
            // aggregation, avoiding overflow and unbounded stack-placement loops.
            if (count > 9L * material.getMaxStackSize()) return Optional.empty();
            if (matching == null) required.add(material.copy());
            else matching.setCount((int) count);
        }

        List<Integer> available = new ArrayList<>(slots);
        available.sort(java.util.Comparator.<Integer>comparingInt(s -> inventory.get(s).isEmpty() ? 0 : 1)
                .thenComparingInt(s -> protectedSlots.contains(s) ? 1 : 0)
                .thenComparingInt(s -> s == selectedSlot ? 0 : 1));
        List<ItemStack> targets = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); ++slot) targets.add(ItemStack.EMPTY);
        List<Integer> missing = new ArrayList<>();
        for (ItemStack material : required)
        {
            int count = material.getCount();
            for (int slot = 0; slot < Math.min(9, inventory.size()); ++slot)
                if (!slots.contains(slot) && ItemStack.isSameItemSameComponents(inventory.get(slot), material))
                    count -= inventory.get(slot).getCount();
            // Reserve matching configured stacks for ALL materials before allocating
            // replacements, so a supplement cannot overwrite the base or another supplement.
            for (int slot : slots)
            {
                if (count <= 0) break;
                if (available.contains(slot) && ItemStack.isSameItemSameComponents(inventory.get(slot), material))
                {
                    int size = Math.min(count, material.getMaxStackSize());
                    targets.set(slot, material.copyWithCount(Math.max(size, inventory.get(slot).getCount())));
                    available.remove(Integer.valueOf(slot));
                    count -= targets.get(slot).getCount();
                }
            }
            missing.add(Math.max(0, count));
        }
        for (int index = 0; index < required.size(); ++index)
        {
            ItemStack material = required.get(index);
            int count = missing.get(index);
            while (count > 0)
            {
                if (available.isEmpty()) return Optional.empty();
                int slot = available.removeFirst();
                int size = Math.min(count, material.getMaxStackSize());
                targets.set(slot, material.copyWithCount(size));
                count -= size;
            }
        }

        List<ItemStack> after = new ArrayList<>();
        List<ItemStack> displaced = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); ++slot)
        {
            after.add(inventory.get(slot).copy());
            if (!targets.get(slot).isEmpty())
            {
                displaced.add(after.get(slot));
                after.set(slot, ItemStack.EMPTY);
            }
        }
        for (int slot : slots)
        {
            ItemStack target = targets.get(slot);
            if (target.isEmpty()) continue;
            int needed = target.getCount();
            for (ItemStack source : displaced) needed = consumeMatching(source, target, needed);
            // Main inventory contributes only by moving existing matching items;
            // it never satisfies hotbar availability by itself and never receives grants.
            for (int source = 0; source < after.size() && needed > 0; ++source)
                if (source >= 9 || (slots.contains(source) && targets.get(source).isEmpty()))
                    needed = consumeMatching(after.get(source), target, needed);
            after.set(slot, target.copy()); // Only the remaining deficit is a creative grant.
        }
        for (ItemStack stack : displaced)
        {
            if (stack.isEmpty()) continue;
            // Freed main source slots make safe swaps possible even with a full
            // inventory. Merge/relocate before using authorized configured deletion.
            relocateDisplaced(after, stack, slots, targets);
        }
        if (!preservesRequiredCounts(inventory, after, materials)) return Optional.empty();
        for (ItemStack material : required)
        {
            int count = 0;
            for (int slot = 0; slot < Math.min(9, after.size()); ++slot)
                if (ItemStack.isSameItemSameComponents(after.get(slot), material)) count += after.get(slot).getCount();
            if (count < material.getCount()) return Optional.empty();
        }
        for (int slot = 0; slot < Math.min(9, after.size()); ++slot)
            if (ItemStack.isSameItemSameComponents(after.get(slot), materials.getFirst()))
                return Optional.of(new DecorativePickPlan(after, slot));
        return Optional.empty();
    }

    private static int consumeMatching(ItemStack source, ItemStack target, int needed)
    {
        if (needed > 0 && ItemStack.isSameItemSameComponents(source, target))
        {
            int count = Math.min(needed, source.getCount());
            source.shrink(count);
            needed -= count;
        }
        return needed;
    }

    private static void relocateDisplaced(List<ItemStack> after, ItemStack displaced,
            List<Integer> slots, List<ItemStack> targets)
    {
        for (int slot = 9; slot < after.size() && !displaced.isEmpty(); ++slot)
        {
            ItemStack stack = after.get(slot);
            if (ItemStack.isSameItemSameComponents(stack, displaced))
            {
                int count = Math.min(displaced.getCount(), Math.max(0, stack.getMaxStackSize() - stack.getCount()));
                stack.grow(count);
                displaced.shrink(count);
            }
        }
        // Configured matching stacks may also absorb displaced surplus without
        // losing any placed requirement. Nonconfigured hotbar matches stay read-only.
        for (int slot : slots)
        {
            ItemStack stack = after.get(slot);
            if (!displaced.isEmpty() && ItemStack.isSameItemSameComponents(stack, displaced))
            {
                int count = Math.min(displaced.getCount(), Math.max(0, stack.getMaxStackSize() - stack.getCount()));
                stack.grow(count);
                displaced.shrink(count);
            }
        }
        for (int slot = 9; slot < after.size() && !displaced.isEmpty(); ++slot)
            if (after.get(slot).isEmpty())
            {
                after.set(slot, displaced.copy());
                displaced.setCount(0);
            }
        for (int slot : slots)
            if (!displaced.isEmpty() && targets.get(slot).isEmpty() && after.get(slot).isEmpty())
            {
                after.set(slot, displaced.copy());
                displaced.setCount(0);
            }
    }

    private static boolean preservesRequiredCounts(List<ItemStack> before, List<ItemStack> after, List<ItemStack> materials)
    {
        for (ItemStack material : materials)
        {
            if (material.isEmpty()) continue;
            int required = 0, original = 0, remaining = 0;
            for (ItemStack stack : materials)
                if (ItemStack.isSameItemSameComponents(stack, material)) required += stack.getCount();
            for (ItemStack stack : before)
                if (ItemStack.isSameItemSameComponents(stack, material)) original += stack.getCount();
            for (ItemStack stack : after)
                if (ItemStack.isSameItemSameComponents(stack, material)) remaining += stack.getCount();
            if (remaining < Math.min(original, required)) return false;
        }
        return true;
    }

    /** Creative decoration delivery; only configured hotbar contents may be discarded. */
    public static void pickDecorativeMaterials(List<ItemStack> materials, Minecraft mc)
    {
        if (mc.player == null || mc.gameMode == null || mc.level == null ||
            !EntityUtils.isCreativeMode(mc.player) || materials.isEmpty() || materials.get(0).isEmpty()) return;

        Inventory inventory = mc.player.getInventory();
        List<ItemStack> before = inventory.getNonEquipmentItems();
        if (PICK_BLOCKABLE_SLOTS.isEmpty())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
            return;
        }
        List<Integer> protectedSlots = new ArrayList<>();
        for (int slot : PICK_BLOCKABLE_SLOTS)
            if (Inventory.isHotbarSlot(slot) && !canPickToSlot(inventory, slot)) protectedSlots.add(slot);
        Optional<DecorativePickPlan> planned = planDecorativePick(before, materials,
                PICK_BLOCKABLE_SLOTS, inventory.getSelectedSlot(), protectedSlots);
        if (planned.isEmpty())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            return;
        }
        List<ItemStack> after = planned.get().items();
        int targetSlot = planned.get().selectedSlot();
        for (int slot = 0; slot < after.size(); ++slot)
        {
            if (!ItemStack.matches(before.get(slot), after.get(slot)))
            {
                inventory.setItem(slot, after.get(slot));
                mc.gameMode.handleCreativeModeItemAdd(after.get(slot), Inventory.isHotbarSlot(slot) ? 36 + slot : slot);
            }
        }
        inventory.setSelectedSlot(targetSlot);
        EasyPlaceUtils.setEasyPlaceLastPickBlockTime();
    }

    private static boolean canPickToSlot(Inventory inventory, int slotNum)
    {
        if (PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
        {
            return false;
        }

        ItemStack stack = inventory.getItem(slotNum);

        if (stack.isEmpty())
        {
            return true;
        }

        return (Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() == false ||
                stack.isDamageableItem() == false) &&
               (Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() == false ||
                (EquipmentUtils.isRegularTool(stack)) == false);
    }

    private static int getPickBlockTargetSlot(Player player)
    {
        if (PICK_BLOCKABLE_SLOTS.isEmpty() || player == null)
        {
            return -1;
        }

        int slotNum = player.getInventory().getSelectedSlot();

        if (canPickToSlot(player.getInventory(), slotNum))
        {
            return slotNum;
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
        {
            nextPickSlotIndex = 0;
        }

        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
            {
                nextPickSlotIndex = 0;
            }

            if (canPickToSlot(player.getInventory(), slotNum))
            {
                return slotNum;
            }
        }

        return -1;
    }

    private static int getEmptyPickBlockableHotbarSlot(Inventory inventory)
    {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i);

            if (Inventory.isHotbarSlot(slotNum))
            {
                ItemStack stack = inventory.getItem(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    public static boolean doesShulkerBoxContainItem(ItemStack stack, ItemStack referenceItem)
    {
        NonNullList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    public static boolean doesBundleContainItem(ItemStack stack, ItemStack referenceItem)
    {
        NonNullList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getBundleItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    private static boolean doesListContainItem(NonNullList<ItemStack> items, ItemStack referenceItem)
    {
        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static int findSlotWithBoxWithItem(AbstractContainerMenu container, ItemStack stackReference, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof InventoryMenu;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((isPlayerInv == false || fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.index, false)) &&
                doesShulkerBoxContainItem(slot.getItem(), stackReference))
            {
                return slot.index;
            }
        }

        return -1;
    }

    /**
     * Get a valid Inventory Object by any means necessary.
     *
     * @param world (Input ClientWorld)
     * @param pos (Pos of the Tile Entity)
     * @return (The result InventoryOverlay.Context | NULL if not obtainable)
     */
    public static @Nullable InventoryOverlayContext getTargetInventory(Level world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        Block blockTmp = state.getBlock();
        CompoundData data = new CompoundData();
        BlockEntity be = null;

        if (blockTmp instanceof EntityBlock)
        {
			Optional<NbtInventory> combinedInv = getCombinedInventory(world, pos);
			ListData list = null;

			if (combinedInv.isPresent())
			{
				NbtInventory inventory = combinedInv.get();
				list = inventory.sorted().toDataList(world.registryAccess());
			}

            if (world instanceof ServerLevel || world instanceof WorldSchematic)
            {
                be = world.getChunkAt(pos).getBlockEntity(pos);

                if (be != null)
                {
					data = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(world.registryAccess()));

					if (list != null && !list.isEmpty())
					{
						data.remove(NbtKeys.ITEMS);
						data.put(NbtKeys.ITEMS, list);
					}
                }
            }
            else
            {
                Pair<BlockEntity, CompoundData> pair = EntityDataManager.getInstance().requestBlockEntityWrapped(world, pos);

                if (pair != null)
                {
					data = pair.getRight();
                    be = pair.getLeft();

	                if (list != null && !list.isEmpty())
	                {
		                data.remove(NbtKeys.ITEMS);
		                data.put(NbtKeys.ITEMS, list);
	                }
                }
            }

//            Litematica.LOGGER.warn("getTarget():2: pos [{}], be [{}], nbt [{}]", pos.toShortString(), be != null, data != null);
            InventoryOverlayContext ctx = getTargetInventoryFromBlock(world, pos, be, data);

            if (world instanceof WorldSchematic)
            {
                return ctx;
            }

            if (lastBlockEntityContext != null && !lastBlockEntityContext.getLeft().equals(pos))
            {
                lastBlockEntityContext = null;
            }

            if (ctx != null && ctx.inv() != null)
            {
                lastBlockEntityContext = Pair.of(pos, ctx);
                return ctx;
            }
            else if (lastBlockEntityContext != null && lastBlockEntityContext.getLeft().equals(pos))
            {
                return lastBlockEntityContext.getRight();
            }
        }

        return null;
    }

    private static @Nullable InventoryOverlayContext getTargetInventoryFromBlock(Level world, BlockPos pos, @Nullable BlockEntity be, CompoundData data)
    {
        Container inv;

        if (be != null)
        {
            if (data.isEmpty())
            {
				data = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(world.registryAccess()));
            }
            inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
        }
        else
        {
            if (data.isEmpty())
            {
                Pair<BlockEntity, CompoundData> pair = EntityDataManager.getInstance().requestBlockEntityWrapped(world, pos);

                if (pair != null)
                {
					data = pair.getRight();
                }
            }

            inv = EntityDataManager.getInstance().getBlockInventoryWrapped(world, pos, true);
        }

        if (data != null && !data.isEmpty())
        {
            Container inv2 = fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data, inv != null ? inv.getContainerSize() : -1, world.registryAccess());

            if (inv == null)
            {
                inv = inv2;
            }
        }

//        Litematica.LOGGER.warn("getTarget(): [SchematicWorld? {}] pos [{}], inv [{}], be [{}], nbt [{}]", world instanceof WorldSchematic ? "YES" : "NO", pos.toShortString(), inv != null, be != null, data != null ? data.getString("id") : new CompoundData());

	    if (be == null)
	    {
		    be = world.getBlockEntity(pos);

		    if (inv == null || inv.isEmpty())
		    {
			    if (be instanceof Container cc)
			    {
				    inv = cc;
			    }
		    }
	    }

		// Continue showing nothing when null
        if (inv == null || data == null)
        {
            return null;
        }

        return new InventoryOverlayContext(InventoryOverlay.getBestInventoryType(inv, data), inv, be != null ? be : world.getBlockEntity(pos), null, data, new Refresher());
    }

    // This really isn't used for this use case; but this is just here for Compat
    public static class Refresher implements InventoryOverlayRefresher
    {

        @Override
        public InventoryOverlayContext onContextRefresh(InventoryOverlayContext data, Level world)
        {
            // Refresh data
            if (data.be() != null)
            {
                getTargetInventory(world, data.be().getBlockPos());
                data = getTargetInventoryFromBlock(data.be().getLevel(), data.be().getBlockPos(), data.be(), data.data());
            }
            /*
            else if (data.entity() != null)
            {
                EntitiesDataStorage.getInstance().requestEntity(world, data.entity().getId());
                data = getTargetInventoryFromEntity(data.entity(), data.data());
            }
             */

            return data;
        }
    }

	public static Optional<NbtInventory> getCombinedInventory(Level world, BlockPos pos)
	{
		Container inv;

		if (world instanceof ServerLevel || world instanceof WorldSchematic)
		{
			inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
		}
		else
		{
			inv = EntityDataManager.getInstance().getBlockInventoryWrapped(world, pos, false);
		}

		if (inv != null && inv.getContainerSize() >= NbtInventory.DEFAULT_SIZE)
		{
			return Optional.of(NbtInventory.fromInventory(inv));
		}

		return Optional.empty();
	}

	/**
	 * Converts an NbtCompound representation of an ItemStack into a '/give' compatible string.
	 * This is the format used by the ItemStringReader(), including Data Components.
	 *
	 * @param nbt (Nbt Input, must be valid ItemStack.encode() format)
	 * @return (The String Result | NULL if the NBT is invalid)
	 */
	@Deprecated(forRemoval = true)
	@Nullable
	public static String convertItemNbtToString(CompoundTag nbt)
	{
		StringBuilder result = new StringBuilder();

		if (nbt.isEmpty())
		{
			return null;
		}

		if (nbt.contains("id"))
		{
			result.append(nbt.getStringOr("id", "?"));
		}
		else
		{
			return null;
		}
		if (nbt.contains("components"))
		{
			CompoundTag components = nbt.getCompoundOrEmpty("components");
			int count = 0;

			result.append("[");

			for (String key : components.keySet())
			{
				if (count > 0)
				{
					result.append(", ");
				}

				result.append(key);
				result.append("=");
				result.append(components.get(key));
				count++;
			}

			result.append("]");
		}
		if (nbt.contains("count"))
		{
			int count = nbt.getIntOr("count", 1);

			if (count > 1)
			{
				result.append(" ");
				result.append(count);
			}
		}

		return result.toString();
	}

    /**
     * Post Re-Write Code
     * -
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether to allow taking items from other hotbar slots
     */
    @ApiStatus.Experimental
    public static void preRestockHand(Player player,
                                      InteractionHand hand,
                                      int threshold,
                                      boolean allowHotbar)
    {
        if (player == null) return;
        Inventory container = player.getInventory();
        final ItemStack handStack = player.getItemInHand(hand);
        final int count = handStack.getCount();
        final int max = handStack.getMaxStackSize();

        if (handStack.isEmpty() == false &&
            getCursorStack().isEmpty() &&
            (count <= threshold && count < max))
        {
            int endSlot = allowHotbar ? 44 : 35;
            int currentMainHandSlot = getSelectedHotbarSlot() + 36;
            int currentSlot = hand == InteractionHand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                Minecraft mc = Minecraft.getInstance();
                AbstractContainerMenu handler = player.inventoryMenu;

                Slot slot = handler.slots.get(slotNum);
                ItemStack stackSlot = container.getItem(slotNum);

                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreDurability(stackSlot, handStack))
                {
                    // If all the items from the found slot can fit into the current
                    // stack in hand, then left click, otherwise right click to split the stack
                    int button = stackSlot.getCount() + count <= max ? 0 : 1;

                    //clickSlot(container, slot, button, ClickType.PICKUP);
                    //clickSlot(container, currentSlot, 0, ClickType.PICKUP);

                    mc.gameMode.handleContainerInput(handler.containerId, slot.index, button, ContainerInput.PICKUP, player);
                    mc.gameMode.handleContainerInput(handler.containerId, currentSlot, 0, ContainerInput.PICKUP, player);

                    break;
                }
            }
        }
    }

    @ApiStatus.Experimental
    public static ItemStack getCursorStack()
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        Inventory inv = player.getInventory();
        return inv != null ? inv.getSelectedItem() : ItemStack.EMPTY;
    }

    @ApiStatus.Experimental
    public static int getSelectedHotbarSlot()
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
        {
            return 0;
        }
        Inventory inv = player.getInventory();
        return inv != null ? inv.getSelectedSlot() : 0;
    }
}
