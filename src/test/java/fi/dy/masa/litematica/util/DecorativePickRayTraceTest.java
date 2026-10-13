package fi.dy.masa.litematica.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.litematica.materials.DecorativePickMaterialsTest;

/** Deterministic hit-selection math, not a client renderer or callback simulation. */
public class DecorativePickRayTraceTest
{
    public static void main(String[] args)
    {
        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(0, 0, 5);
        double hit = RayTraceUtils.decorativeHitDistance(new AABB(-1, -1, 2, 1, 1, 3), start, end);
        check(hit == 4, "box entry squared distance");
        check(RayTraceUtils.decorativeHitDistance(new AABB(2, 2, 2, 3, 3, 3), start, end) < 0, "off-ray entity");
        check(RayTraceUtils.decorativeHitDistance(new AABB(-1, -1, 6, 1, 1, 7), start, end) < 0, "out of range");
        check(RayTraceUtils.decorativeHitDistance(new AABB(-1, -1, -1, 1, 1, 1), start, end) == 0, "eye inside box");
        check(RayTraceUtils.isBetterDecorativeHit(4, 9, 25, true), "closest entity beats farther schematic block");
        check(!RayTraceUtils.isBetterDecorativeHit(9, 4, 25, true), "closest block wins");
        check(RayTraceUtils.isBetterDecorativeHit(9, 4, 25, false), "furthest entity wins before real hit");
        check(!RayTraceUtils.isBetterDecorativeHit(4, 9, 25, false), "furthest block wins");
        check(!RayTraceUtils.isBetterDecorativeHit(9, -1, 4, true), "near real block or entity occludes");
        check(!RayTraceUtils.isBetterDecorativeHit(4, -1, 4, false), "real hit wins distance tie");
        check(!RayTraceUtils.isBetterDecorativeHit(4, 4, 25, true), "schematic block wins tie");
        check(!RayTraceUtils.isBetterDecorativeHit(-1, -1, 25, true), "miss rejected");
        check(!RayTraceUtils.isBetterDecorativeHit(4, 9, 25, true, false), "unrendered closest entity must not win");
        check(!RayTraceUtils.isBetterDecorativeHit(9, 4, 25, false, false), "unrendered furthest entity must not win");
        check(!RayTraceUtils.isBetterDecorativeHit(4, -1, -1, true, false), "unavailable membership must not pick even without competition");
        check(RayTraceUtils.isBetterDecorativeHit(4, 9, 25, true, true), "rendered closest entity remains eligible");
        check(RayTraceUtils.isBetterDecorativeHit(9, 4, 25, false, true), "rendered furthest entity remains eligible");
        check(!RayTraceUtils.isBetterDecorativeHit(9, -1, 4, true, true), "render membership never bypasses real occlusion");
        System.out.println("DecorativePickRayTraceTest passed");
    }

    private static void check(boolean condition, String message)
    {
        DecorativePickMaterialsTest.check(condition, message);
    }
}
