package com.dedztbh.demagica.util

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.lang.ref.WeakReference
import kotlin.random.Random

/**
 * Created by DEDZTBH on 2019-02-13.
 * Project DEMagica
 */

typealias P<T, R> = Pair<T, R>

val World.isLocal: Boolean
    get() = !isRemote

fun Random.nextVec3d(maxSpread: Double): Vec3d =
        if (maxSpread > 0.0) {
            (maxSpread / 2).let { halfSpread ->
                Vec3d(
                        (nextDouble(maxSpread) - halfSpread),
                        (nextDouble(maxSpread) - halfSpread),
                        (nextDouble(maxSpread) - halfSpread)
                )
            }
        } else {
            Vec3d(0.0, 0.0, 0.0)
        }

fun Random.nextPitch(): Float =
        0.4f / (nextFloat() * 0.4f + 0.8f)

fun <T> T.weakRef() = WeakReference(this)

fun TileEntity.oppositeBlockPosAndEnumFacings() = pos.run {
    listOf(P(up(), EnumFacing.DOWN),
            P(down(), EnumFacing.UP),
            P(east(), EnumFacing.WEST),
            P(west(), EnumFacing.EAST),
            P(south(), EnumFacing.NORTH),
            P(north(), EnumFacing.SOUTH))
}

infix fun <T> Boolean.then(block: () -> T): T? = if (this) block() else null

fun Boolean.toInt(): Int = if (this) 1 else 0
fun Int.toBool(): Boolean = this != 0

infix fun <T : Comparable<T>> T.orIfSmaller(b: T) = if (compareTo(b) > 0) b else this

fun <T> nil(): T? = null

infix fun <T> T.onlyIf(cond: Boolean): T? = if (cond) this else null
infix fun <T> T.onlyIfNot(cond: Boolean): T? = onlyIf(!cond)