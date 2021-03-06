package com.dedztbh.demagica.projectile

import com.dedztbh.demagica.global.Config
import com.dedztbh.demagica.util.isLocal
import com.dedztbh.demagica.util.onlyIfNot
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World


/**
 * Created by DEDZTBH on 2019-02-13.
 * Project DEMagica
 */

class MagicBallHeavy : MagicBall {

    constructor(worldIn: World) : super(worldIn)

    constructor(worldIn: World, shooter: EntityLivingBase) : super(worldIn, shooter)

    override var gravity: Double = 0.02

    init {
        damage = 20.0
    }

    override fun onHit(raytraceResultIn: RayTraceResult) {
        super.onHit(raytraceResultIn)
        if (world.isLocal) {
            world.newExplosion(thrower onlyIfNot Config.explosionDoAffectSelf, posX, posY, posZ, 1f, false, false)
            setDead()
        }
    }
}