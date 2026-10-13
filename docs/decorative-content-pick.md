# Pick decorative schematic contents

Creative schematic pick supplies base items and decorative contents for supported blocks, item frames, armor stands and exact painting variants. It never traverses storage inventories. Survival keeps selection-only behavior and never creates items.

## Materials

| Schematic target | Creative materials |
| --- | --- |
| Empty flower pot | One flower pot |
| Potted plant | One flower pot and its plant |
| Chiseled bookshelf | One chiseled bookshelf and **one ordinary book**, regardless of occupancy or stored book data |
| Shelf | One matching shelf and the displayed stacks, preserving their counts and item components |
| Shelf without block-entity data | The plain matching shelf only |
| Ordinary storage block | Existing pick behavior; no supplemental inventory contents |
| Normal / glow item frame | One matching frame variant and its displayed stack; an empty frame still supplies the frame |
| Armor stand | One armor stand and the occupied head, chest, legs and feet stacks; empty slots and hand slots are ignored |
| Painting | One painting carrying the schematic's exact `PAINTING_VARIANT` component, not a plain/random painting |
| Other entities / entity containers | Not targeted by decorative picking |

**Every required material ends in the hotbar on success**, with the base selected. Repeated matching items **and components** are counted together, splitting across slots at the stack-size limit (including nonstackable armor). Existing main-inventory matches are moved/reused, not counted as hotbar availability or granted again. Existing nonconfigured hotbar matches count toward requirements but remain read-only, including their counts.

The planner reserves matching configured stacks for all materials before allocating replacements. Additional placement prefers empty configured slots, then unprotected slots (selected first), then protected slots. Only configured slots may receive new items or top-ups. It uses a bounded allocation across at most nine hotbar slots, not a combinatorial inventory search; oversized requirements fail immediately.

Displaced configured contents are merged or relocated into main inventory or available configured capacity before deletion. Main source slots freed by moving materials allow safe swaps even when the inventory starts full. If preservation capacity is exhausted, creative decorative picking may **discard occupied configured hotbar pick-slot contents only**, without losing required counts/components. Surplus matching contents are merged where possible and may be discarded only when required counts remain. Main-inventory items are never discarded; armor/offhand are excluded entirely. Unconfigured hotbar slots are never changed. Tool/damageable avoidance remains a preference, not a veto on the explicitly authorized configured replacements. Ordinary block pick keeps its existing protection rules.

If the complete set cannot fit in permitted hotbar capacity, nothing changes, including selection, and the existing no-suitable-slot warning is shown—even with empty main inventory. There are no partial grants or supplemental grants into main inventory. Empty/invalid configured pick slots always fail without mutation, even when matching items already occupy the hotbar. Narrow configurations are not automatically expanded: a fully equipped stand normally needs five permitted slots, unless existing nonconfigured hotbar matches supply some requirements.

## Entity targeting

Both pick modes compare schematic entity bounding-box entry hits with the existing schematic block target. Closest chooses the nearer hit; furthest chooses the farther hit **before** the nearest real-world block or entity, and retains the real-block-behind requirement. Real-world hits win distance ties. Schematic block targets also win ties against entities. Ray misses and targets outside the existing pick range are excluded. The legacy adjacent, non-collidable block fallback remains authoritative when it has no genuine ray distance to compare.

Entity eligibility requires global schematic rendering (including its invert hotkey), entity rendering, visible entity type, the current render layer, enabled placement chunk and render distance. It additionally requires membership by UUID and position in a completed entity-render preparation snapshot, so entities outside rendered chunks or the renderer's frustum cannot become pick targets. Only a boolean query is exposed, never a mutable collection.

Snapshots fail closed before preparation, during renderer startup, after world changes, renderer reloads or stopped chunk updates, and when the client world, client tick, camera position/rotation or entity position no longer matches. Normal frame-end render-state cleanup retains the read-only snapshot for input handling between frames. A changed view may therefore require another completed render pass before entity picking works. Replacement renderers default to no entity eligibility unless they implement this read-only contract.

## Compatibility

- Hidden blocks remain excluded before start callbacks and after target refresh.
- Cancelling callbacks/slot handlers stop built-in delivery using the existing event contract.
- Any custom slot handler suppresses supplements: the legacy handler API falls through on SUCCESS as well as ERROR, so the existing single-stack fallback remains but cannot double-grant decorative contents.
- Explicit stack overrides and in-place stack mutations use single-stack picking, without decorative supplements.
- Survival retains the old material-cache stack and shulker-selection path for blocks. Entity survival picks select only an existing frame variant, armor stand or exact variant-bearing painting (or use the existing configured shulker-selection path); they never grant the base or contents.
- Entities run global start cancellation before target refresh. The entity target is then resolved again with all visibility and occlusion guards. The existing global success notification remains available.
- The callback API's pre-gather, pre-pick and custom slot handlers require block targets and are not invoked with fabricated entity block positions. Global stack/slot redirects, including retained legacy redirects, fail closed for entities: no built-in delivery or success notification occurs. Entity-specific overrides require a future API, not weaker safeguards.
- Ctrl-pick retains NBT behavior for ordinary blocks. Decorative targets, including callback fallback picks, do not automatically embed their block-entity inventories; creative material delivery works the same with or without Ctrl.
- Ctrl-pick entities uses the same base-plus-materials delivery (including the painting variant component); it does not serialize the entity or automatically embed storage contents.
- Collection copies displayed/equipped stacks and preserves their counts and item components; it does not change the schematic. No storage inventories or unrelated living entities are inspected.

## Painting API evidence

Local mapped jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-683eea452e/26.3/minecraft-merged-683eea452e-26.3.jar`.

`javap -c` confirms `net.minecraft.world.entity.decoration.painting.Painting.getPickResult()` constructs only `new ItemStack(Items.PAINTING)`. The collector therefore explicitly sets `DataComponents.PAINTING_VARIANT` from `Painting.getVariant()` without copying entity NBT. `HangingEntityItem.useOn()` creates a painting, applies `EntityType.createDefaultStackConfig()` before `survives()`, and then adds the entity. `javap -c -p EntityType` confirms its component processor calls `Entity.applyComponentsFromItemStack()`. Painting's implicit-component methods apply `PAINTING_VARIANT` with `setVariant()`. This verifies the exact-variant placement path in vanilla bytecode, not a live placement test; unsuitable wall space can still prevent placement. Paintings use the same entity allowlist, rendered-membership, visibility, occlusion and callback restrictions as frames/stands.

## Verification

`./gradlew test` runs standalone assertion tests for collection and atomic inventory planning without a running client. The fixture bootstraps vanilla registries and binds minimal item stack-size defaults because 26.3's real default components require data-pack tags. Tests cover pot contents, generic books, shelf counts/copy isolation, missing data, storage exclusion, matching components, reuse, aggregated counts, stack splitting, full inventory and atomic capacity failure.

Entity collection tests exercise normal/glow frame material variants, empty displays, all four armor slots, empty equipment and copy isolation through deterministic collectors. Ray-trace tests execute box-entry/range math, closest/furthest comparison, ties, real-hit occlusion and rejection of unrendered membership. These are executable behavior tests, but do not construct a live renderer or simulate a Minecraft client.

Source-level regression assertions also check hidden-block ordering, cancellation/slot-handler ordering, creative/override gating, decorative NBT suppression, entity allowlisting, armor-slot mapping, render filters, snapshot freshness/invalidation and the rendered-membership query at targeting. These are structural checks, **not** runtime renderer, callback or network proof. `./gradlew build --no-daemon` validates compilation and packaging.

T7 behavioral RED: `./gradlew test --rerun-tasks` failed with `pot AND plant must reach hotbar when main inventory is free` against the previous base-only hotbar planner. After the all-hotbar change, the same command passed all three assertion harnesses. Runtime planner tests cover free/full main inventory, moved main supplements, five-material equipped stands, nonstackable repeats, normal/glow frames, aggregated multi-stack counts, component-distinct paintings, repeated-pick idempotence, read-only nonconfigured matches, configured-only overwrite, insufficient hotbar capacity despite free main capacity, disabled/invalid configuration and equipment-bearing input rejection. Survival gating is checked structurally, not through a live player. Client and multiplayer proof remains pending.

T5 historical behavioral RED (its main-inventory supplement expectations are superseded by T7): topping up a matching stack in a full inventory initially threw `NoSuchElementException` because no plan existed; the planner now uses matching stack capacity before empty slots or replacement. The new inventory/selection seam initially returned no plan and failed `full protected hotbar with main space must safely swap`; after inventory implementation the suite reached painting collection and failed `nondefault painting must carry exact variant` against a plain painting stack. Deterministic tests cover safe swaps with one/two empty main slots, full-inventory reuse, configured-only destruction, protection preferences, required counts/components, surplus stacks, insufficiency/input immutability, empty/invalid pick slots and distinct nondefault painting components. Actual painting entities, live placement, survival selection and multiplayer synchronization remain manual checks, not simulated client proof.

T2's resumed behavioral RED was `unrendered closest entity must not win`; the ungated hit comparator failed that assertion before the membership condition was applied. GREEN executed all three assertion harnesses successfully through `./gradlew test`.

## Manual Minecraft checklist (not playtested)

- [ ] Creative: pick every supported block with empty inventory; verify base selected and **all contents in the hotbar**, with sufficient counts.
- [ ] Repeat picks; verify no duplicate grants when existing counts suffice. Test named displayed items and duplicate shelf slots.
- [ ] Fill hotbar with unrelated items but leave main-inventory space; verify pot AND plant, frame AND display, and stand AND all four armor items reach the hotbar; displaced items should relocate without deletion when space exists. Check multiplayer synchronization.
- [ ] Fill all slots; verify configured hotbar replacement supplies missing materials and never discards main inventory, armor or offhand. Test protected tools and required displayed stacks; verify only surplus/unrelated configured contents can be discarded and every required material stays in the hotbar.
- [ ] Configure too few pick slots for the complete material set; verify warning, no changes and unchanged selection **even with empty main inventory**. Test main-existing supplements moving into configured hotbar slots without duplicate grants, and sufficient/insufficient read-only matches in nonconfigured hotbar slots.
- [ ] Disable all pick slots; check warning and no changes even when the base is already in the hotbar.
- [ ] Survival: verify existing plant/book/shelf selection, missing-item behavior and configured shulker selection; no items appear.
- [ ] Hide targets; test closest/furthest picks and a start callback that changes the target to a hidden block.
- [ ] Cancel start/pre-gather/pre-pick; override a stack at pre-gather/pre-pick; mutate it in place; handle or decline slot callbacks. Verify supplements are never granted for an override or handled pick.
- [ ] Ctrl-pick decorative blocks and ordinary chests; verify decorative bases have no embedded inventory while ordinary Ctrl-pick keeps its prior behavior.
- [ ] Load shelves with missing data and empty displays; verify safe base-only delivery.

## Manual entity checklist (not playtested)

- [ ] Creative: pick empty/occupied normal and glow frames, including named displayed stacks; verify the correct variant and counts.
- [ ] Pick empty and fully equipped armor stands; verify all four occupied armor slots and no hand-slot contents. Test invisible/marker stands without assuming a zero-sized hitbox is pickable.
- [ ] Pick and place multiple nondefault paintings (for example Donkey Kong and Pointer) with suitable wall space; verify exact artwork, separate variant stacks, repeated-pick reuse and Ctrl-pick without entity NBT.
- [ ] Repeat picks, full inventories, disabled pick slots and Ctrl-pick; verify reuse, configured-only destructive replacement and atomic failure when insufficient. Check multiplayer synchronization.
- [ ] Survival: select existing/missing frame variants, stands and exact painting variants, including configured shulker selection; verify no items are created.
- [ ] Compare closest/furthest picks across mixed schematic blocks/entities. Place real blocks and entities nearer the ray; test distance ties, range limits and the furthest real-block-behind requirement.
- [ ] Toggle rendering/invert/entity rendering, hide entity types, narrow layers and disable placement rendering. Test frustum boundaries, third-person views, renderer startup/reloads and world changes; stale/unavailable snapshots must decline entity picks.
- [ ] Cancel global start or change the view/target there; verify refreshed visibility/occlusion checks. Install global stack/slot redirects; verify entities fail closed without fabricated block callbacks or supplemental grants.
- [ ] Aim at unrelated mobs and storage entities; verify decorative picking does not inspect or copy their contents.
