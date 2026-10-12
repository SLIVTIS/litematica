# Hidden block interactions

## Intent and scope
Hidden schematic blocks cannot be picked or Easy Placed and no longer obstruct visible targets behind them. Preserve vanilla occlusion/picking, visible behavior, editing/rebuild targets, data, pasting, material counts and verifier. User approved interaction-only changes in RayTraceUtils, WorldUtils and EasyPlaceUtils. Visibility documentation updated. Spanish Conventional Commits without agent attribution; User explicitly authorized commits after successful Minecraft retest. No push or merge.

## Tasks
- [x] T1: Close verified visibility-aware interaction changes with an authorized work-unit commit. Evidence: 483e83da, `fix(visibilidad): respetar bloques ocultos en pick y easy-place`.

## Implementation
Action guards in schematic pick and both modern/legacy Easy Place; event reset at entry and fresh target/state after start callbacks. RayTraceUtils overloads default to unfiltered behavior; eight pick/Easy Place call sites opt into schematic-only filtering. Hidden starting/loop cells and fluids skipped while DDA continues. LAST adjacent fallback filtered. Vanilla traces and distance boundaries unchanged; editing callers retain default false. Override guards retained.

## Verification evidence
Writer and independent verifier ./gradlew build and git diff --check passed. test and compileTestJava NO-SOURCE: no automated gameplay coverage or RED/GREEN; meaningful client behavior runner unavailable. Existing deprecation/unchecked and LF/CRLF warnings only. Parent inspected callback guard ordering. Native review excluded untracked tracking document; candidate 169 code diff lines, medium risk, reliability review approved and exact acknowledgement completed for review-2dc753fbc482b19e, authority burned. Earlier 60-line candidate approval is historical only.

## Caveats and runtime checks
User confirmed initial action guards, then reported hidden occluders; user confirmed follow-up runtime retest successful ("Ya quedo"). Specific edge-case coverage was not itemized. Independent inspection noted conditional third-party callback caveats: start callback can run for hidden-only null trace, and later callbacks can mutate visibility; no observed gameplay failure. Check FIRST/LAST hidden-before-visible, fluids/start cells, LAST fallback, click/hold rewrite on/off, vanilla obstruction, visible slabs, overrides, restoration and editing unchanged.

## Delivery and next step
Branch fix/hidden-block-interactions from master 13c62616; master unchanged. ask-on-risk, 169 authored code diff lines. Code committed as 483e83da after user runtime acceptance. Tracking documentation committed separately. No push or merge. Next: user decides publishing feature branch and integration into master.
