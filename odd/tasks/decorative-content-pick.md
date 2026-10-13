# Decorative content pick

## Objective
Pick all materials needed to reproduce decorative schematic contents, without copying storage inventories.

## Scope and decisions
- Work on `feat/decorative-content-pick`, based on master. Commits explicitly authorized by user; no merge or push authorized. Commit messages: Spanish Conventional Commits, one line, maximum 100 characters, no attribution.
- Include flower pots, normal/glow item frames, armor stands with equipped armor, chiseled bookshelves, and shelves with displayed items. Chiseled bookshelves provide one generic book, not stored books.
- Creative supplies base item plus decorative contents. Follow-up approval permits destructive overwrite ONLY of configured hotbar pick slots when capacity requires it; never discard main-inventory, armor or offhand contents. Survival remains selection-only and never creates items.
- Preserve existing hidden-block exclusions, pick callbacks, Ctrl-pick, and ordinary storage-block picking.
- Preserve unrelated `.gitignore` changes and untracked `.codegraph/`.
- Missing schematic content data must safely fall back to base pick.

## Tasks
- [x] T1 (implementation verified; commit eefecf8e): Add decorative block material collection and safe creative multi-item delivery, preserving survival and callbacks. Add applicable focused tests and user-facing behavior documentation.
- [x] T2 (implementation verified; commit eefecf8e): Add schematic decorative entity targeting and collection for frames and armor stands, with applicable focused tests/documentation.
- [x] T3 (automated verification complete; manual checks separately pending): Verify build, focused tests and regressions; run native review if enabled. Record remaining manual Minecraft checks and evidence.

- [x] T4 (user-reported client acceptance): User reports the final version works perfectly after manual testing. No independent claim that every multiplayer checklist scenario was tested.

- [x] T5 (implementation verified; commit eefecf8e): Fix full-hotbar picking with authorized configured-slot overwrite and add exact-variant painting picking; cover both with behavioral RED/GREEN tests and documentation.
- [x] T6 (automated verification complete; user-reported client acceptance recorded): Verify follow-up build/tests and run fresh candidate native review if enabled; keep manual follow-up playtesting explicit.

- [x] T7 (implementation verified; commit eefecf8e): Place every decorative pick material in the creative hotbar, not just its base, reusing/moving existing matches and replacing configured pick slots as authorized. Test full hotbar with free/full main inventory, repeated contents, components and atomic insufficient-hotbar-capacity failure.
- [x] T8 (automated verification complete; user-reported client acceptance recorded): Verify hotbar-only follow-up and run fresh native review; record new manual cases.

## Acceptance and checks
- Pots yield pot and plant; shelves yield shelf and displayed items; chiseled bookshelves yield shelf and one ordinary book.
- Frames yield frame variant and displayed item; stands yield stand and occupied armor slots; paintings yield the exact variant-bearing painting item.
- All required decorative materials must end in the hotbar with sufficient counts, including supplements. Existing matching items in main inventory must be moved/reused rather than merely satisfying availability there. Only configured hotbar slots may be overwritten; pre-existing matches in other hotbar slots may be reused without destructive edits. If the complete set cannot fit in allowed hotbar capacity, warn and change nothing, rather than silently spilling to main inventory.
- Existing matching items are reused. Follow-up approval permits replacing configured hotbar pick slots even when full, deleting their contents only if necessary. Main inventory/armor/offhand stay protected; if all requirements still cannot fit after allowed replacements, fail atomically with no changes.
- No creative item creation in survival. No ordinary storage contents are copied.
- Empty contents, unavailable data, hidden blocks, callbacks and Ctrl-pick remain safe.
- Run applicable deterministic tests test-first where available; compile/build and Minecraft manual checklist. Do not claim manual playtesting without running the client.

## Evidence and progress
- Exploration: WorldUtils schematic pick currently targets blocks and returns one stack; InventoryUtils creative delivery replaces selected slot, survival only selects existing items.
- T1: test-first failures observed for pot materials and inventory capacity; both assertion suites then passed. Worker ran `./gradlew test`, `./gradlew build --no-daemon`: BUILD SUCCESSFUL. Parent spot check `git diff --check`: passed.
- T1: fixtures required Minecraft component initialization and Gradle test discovery corrections; resolved before passing checks. Runtime callbacks, multiplayer synchronization and Minecraft playtesting remain manual/unrun.
- T2 initial partial handoff (superseded by completion below): frame/armor material collection and closest/furthest targeting implemented; three assertion harnesses, `./gradlew test`, `./gradlew build --no-daemon`, and `git diff --check` passed. RED was a missing-API compilation failure, not behavioral proof. Actual rendered-entity membership guard missing; T2 documentation update and client/multiplayer checks pending. Do not ship.
- T2 completion: read-only renderer membership accessor and stale/unavailable snapshot rejection added. Behavioral RED `unrendered closest entity must not win` observed; all three assertion harnesses then passed. Worker `./gradlew test`, `./gradlew build --no-daemon`, `git diff --check`: successful. T2 documentation updated. Client timing of strict snapshot freshness, live callbacks and multiplayer remain unrun/manual.
- Native review: `review-2c899c34da47bec8`, 12 files/707 changed lines, medium consolidated reliability lens; approved. Exact acknowledgement succeeded, authority burned for target `sha256:66a56e9f3ae4cd436f50d4e3596a0aa9f581055e0d5d0960039bdf0f24de97b1`. Includes pre-existing tracked `.gitignore` diff without editing it; excludes `.codegraph/` and task bookkeeping.
- Initial consent expired without lineage; fresh start subsequently created the review. No replay of expired binding.
- ASSESS unavailable (untracked declaration requirement): risk unassessable, independent verifier required. Independent verifier completed the required checks (see below).
- Independent verification: `./gradlew test --rerun-tasks` successful (7 executed tasks, all three harnesses printed passed); `./gradlew build --no-daemon` successful; `git diff --check` passed. No concrete deterministic defect established. Warnings: deprecated APIs, unchecked operations, JOML Unsafe, Gradle deprecations (non-fatal).
- Artifact: `build/libs/litematica-fabric-26.3-0.29.2-sakura.1.jar`. Older 0.29.0 artifacts are not this feature build.
- Manual checks not run: moving/stationary camera across tick boundaries, third-person/frustum, reload/world changes, multiplayer inventory sync, live callbacks, Ctrl-pick, survival shulker path and mixed-target occlusion. Planner atomicity is local, not multiplayer transaction proof. Strict snapshot freshness may defer entity picks until a completed render pass.
- User feedback: existing feature works in game; this is general client feedback, not confirmation of each multiplayer/manual scenario. Requested full-hotbar replacement and exact painting variant pick.
- User explicitly chose destructive overwrite of configured hotbar pick slots if needed. Main-inventory slots must not be erased; fail without any changes if the material set still cannot fit. Prefer reuse/safe swap before discarding.
- Follow-up exploration: hotbar slot selection rejects occupied protected slots even with main space; current delivery planner never frees overwriteable hotbar capacity. Painting variant API requires inspection of mapped 26.3 bytecode/source before implementation.
- T5 completed: configured-hotbar-only overwrite planning, top-up/reuse/safe swap before destruction; required counts/components protected, empty pick slots and remaining insufficient capacity fail atomically. Behavioral REDs observed for full hotbar, matching-stack capacity and exact painting variant; all three assertion harnesses GREEN. `./gradlew test --rerun-tasks`, `./gradlew build --no-daemon`, `git diff --check`: successful. Parent diff-check spot check passed.
- Painting API verified using mapped 26.3 jar bytecode: `Painting.getPickResult()` is plain; collector attaches `DataComponents.PAINTING_VARIANT` from `getVariant()`. Vanilla placement consumes the variant component. Live placement/multiplayer remain manual.
- Follow-up native review `review-fa3f4f5d3f41f2f1`: approved; exact acknowledgement burned authority for `sha256:381348c06197e6d4349f3d8aa15edcc6baf507af0e5094c0f3322bbeb51e8f12`. 12 files/903 diff lines, consolidated reliability lens. Fresh consent succeeded after initial envelope expiry; no expired binding replay.
- Follow-up ASSESS unavailable due untracked declaration requirement; independent verifier completed as required. `./gradlew test --rerun-tasks` successful (7/7 tasks executed, three harnesses passed); `./gradlew build --no-daemon` successful (3 executed/8 up-to-date, harnesses passed); `git diff --check` passed. No concrete defect established. Git status unchanged by verifier. Jar 2,012,305 bytes at documented artifact path. Nonfatal deprecation/unchecked/JOML Unsafe/Gradle warnings remain.
- Follow-up manual checks pending: actual exact painting placement, configured hotbar overwrite in client, multiplayer inventory synchronization, survival/shulkers, callbacks/Ctrl-pick and render/input timing. Tests prove local planner behavior, not network transactions. Vanilla placement bytecode inspected by writer, not re-executed by verifier.
- Latest user manual feedback: prior hotbar-base and exact painting changes work; maceta base replaces hotbar item but plant remains in main inventory. User explicitly requests hotbar placement/replacement for ALL supplemental items (frames/armor/shelves too), creative only. No proof of complete multiplayer matrix inferred.
- Root cause observed in InventoryUtils: delivery plans across all 36 slots and then moves only the first material into hotbar. Change final layout invariant, not item collection or survival behavior.
- T7 completed: bounded all-hotbar allocation with base selected, main-inventory matches moved, safe relocation before configured-only deletion, component/count preservation, atomic failure on insufficient hotbar capacity. Behavioral RED `pot AND plant must reach hotbar when main inventory is free` observed, then all three harnesses GREEN. Worker `./gradlew test --rerun-tasks` (7 tasks), `./gradlew build --no-daemon`, `git diff --check` successful; parent diff-check passed. Jar rebuilt at existing path. Client and multiplayer follow-up unrun.
- T7 native review `review-f76621bdbc0fb7ab`: approved (12 files/1087 diff lines), exact acknowledgement succeeded and authority burned for target `sha256:be391fe155ede5f83d2967a99a55a4727ac418d05f0161bbe09110ab336e3af0`.
- T8 ASSESS unavailable due untracked declaration requirement; independent verifier completed: `./gradlew test --rerun-tasks` successful (7 executed), `./gradlew build --no-daemon` successful (3 executed/8 up-to-date), `git diff --check` passed. All three harnesses printed passed in both runs; no concrete defect established, Git status unchanged. Jar 2,013,345 bytes. Nonfatal deprecated API/unchecked/JOML Unsafe/Gradle deprecation warnings remain.
- Config evidence: Configs.java:125 default permitted pick slots `1,2,3,4,5`; enough for stand plus four armor pieces. Saved narrower settings may need more slots; defaults unchanged.
- T8 manual checks pending: all-hotbar pot/frame/stand contents, narrow-capacity warning without changes, repeated picks, live placement, multiplayer sync, survival/callback/render timing. No automated network/client proof claimed.
- Final manual acceptance: user confirms everything works perfectly and explicitly requests commits. Earlier unrun/manual notes above are historical; no independent multiplayer test evidence added.
- Feature work-unit commit: `eefecf8e6fecf2036f27b3b635689f5da3c213b8` — `feat(pick): entregar materiales decorativos en hotbar y conservar variantes de cuadros`. Contains behavior, tests and user-facing documentation for T1–T8.
- Task evidence is recorded separately in a documentation commit. Unrelated `.gitignore` and `.codegraph/` excluded. No push/master integration.

## Next step
Commit the task verification record with the authorized single-line Spanish Conventional Commit format. Push and integration into master remain separate user decisions. Automated tests/build, native review, independent verification and user-reported manual acceptance completed; multiplayer matrix not independently observed.
