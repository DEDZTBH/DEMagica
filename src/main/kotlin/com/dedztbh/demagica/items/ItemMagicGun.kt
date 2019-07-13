package com.dedztbh.demagica.items

import com.dedztbh.demagica.DEMagica
import com.dedztbh.demagica.global.ServerTickOS
import com.dedztbh.demagica.projectile.MagicBall
import com.dedztbh.demagica.projectile.MagicBallHeavy
import com.dedztbh.demagica.projectile.MagicBallKatyusha
import com.dedztbh.demagica.projectile.MagicBomb
import com.dedztbh.demagica.util.TickTaskManager
import com.dedztbh.demagica.util.isLocal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*


class ItemMagicGun : ItemBow() {
    enum class MagicGunMode(val delayMs: Long) {
        LIGHT(0L),
        HEAVY(200L),
        KATYUSHA(200L),
        MORTAR(500L)
    }

    private val taskManager: TickTaskManager
    private val itemStackShooters = object : WeakHashMap<ItemStack, ItemStackShooter>() {
        fun getOrCreate(key: ItemStack) =
                get(key)?.let { it }
                        ?: ItemStackShooter().also { set(key, it) }
    }

    inner class ItemStackShooter {
        var runningCoroutineTerminationFlag = false
        var firingTask: TickTaskManager.Task? = null
        var runningCoroutine: Job? = null

        fun asyncShootMagicBall(stack: ItemStack, stackShooter: ItemStackShooter, worldIn: World, playerIn: EntityPlayer, handIn: EnumHand, doShoot: Boolean = true): Job {
            return GlobalScope.launch {
                stackShooter.run {
                    if (runningCoroutineTerminationFlag) {
                        runningCoroutine = null
                        return@launch
                    }
                    if (doShoot) {
                        val magicGunMode = MagicGunMode.valueOf(stack.tagCompound!!.getString("MagicGunMode"))
                        firingTask = taskManager.runTask {
                            worldIn.apply {
                                when (magicGunMode) {
                                    MagicGunMode.LIGHT -> {
                                        spawnEntity(
                                                MagicBall(this, playerIn).apply {
                                                    shoot(playerIn, 5f, 1f)
                                                })
                                    }
                                    MagicGunMode.HEAVY -> {
                                        spawnEntity(
                                                MagicBallHeavy(this, playerIn).apply {
                                                    shoot(playerIn, 5f, 1f)
                                                })
                                    }
                                    MagicGunMode.KATYUSHA -> {
                                        spawnEntity(
                                                MagicBallKatyusha(this, playerIn).apply {
                                                    shoot(playerIn, 4f, 10f)
                                                })
                                    }
                                    MagicGunMode.MORTAR -> {
                                        spawnEntity(
                                                MagicBomb(this, playerIn).apply {
                                                    shoot(playerIn, 3.5f, 2f)
                                                })
                                    }
                                }
                            }
                        }
                        delay(magicGunMode.delayMs)
                    }
                    if (firingTask?.isTerminated == true) {
                        firingTask = null
                    }
                    runningCoroutine = asyncShootMagicBall(stack, this, worldIn, playerIn, handIn, firingTask == null)
                }
            }
        }
    }

    init {
        setRegistryName("magicgun")
        unlocalizedName = DEMagica.MODID + ".magicgun"

        creativeTab = CreativeTabs.COMBAT
        maxStackSize = 1

        taskManager = ServerTickOS.create(this)
    }

    @SideOnly(Side.CLIENT)
    fun initModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(registryName!!, "inventory"))
    }

    override fun onUpdate(itemstack: ItemStack, world: World, entity: Entity, metadata: Int, bool: Boolean) {
        if (itemstack.tagCompound == null) {
            itemstack.tagCompound = NBTTagCompound().apply {
                setString("MagicGunMode", MagicGunMode.LIGHT.name)
            }
        }
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        worldIn.apply {
            if (isLocal()) {
                val stack = playerIn.getHeldItem(handIn)
                itemStackShooters.getOrCreate(stack).run {
                    runningCoroutineTerminationFlag = false
                    if (runningCoroutine?.isActive == true) {
//                    runBlocking {
//                        runningCoroutine?.cancelAndJoin()
//                    }
                    } else {
                        runningCoroutine = asyncShootMagicBall(stack, this@run, worldIn, playerIn, handIn)
                    }
                }
            }
        }
        playerIn.activeHand = handIn
        return ActionResult.newResult(EnumActionResult.PASS, playerIn.getHeldItem(handIn))
    }

    override fun onPlayerStoppedUsing(stack: ItemStack, worldIn: World, entityLiving: EntityLivingBase, timeLeft: Int) {
        if (worldIn.isLocal()) {
            itemStackShooters.getOrCreate(stack).runningCoroutineTerminationFlag = true
        }
    }

    override fun onEntitySwing(entityLiving: EntityLivingBase, stack: ItemStack): Boolean {
        if (entityLiving.world.isLocal()) {
            itemStackShooters.getOrCreate(stack).run {
                val newMagicGunMode = when (MagicGunMode.valueOf(stack.tagCompound!!.getString("MagicGunMode"))) {
                    MagicGunMode.LIGHT -> MagicGunMode.HEAVY
                    MagicGunMode.HEAVY -> MagicGunMode.KATYUSHA
                    MagicGunMode.KATYUSHA -> MagicGunMode.MORTAR
                    MagicGunMode.MORTAR -> MagicGunMode.LIGHT
                }
                stack.tagCompound?.setString("MagicGunMode", newMagicGunMode.name)
                (entityLiving as? EntityPlayer)?.sendStatusMessage(
                        TextComponentString("Magic Gun Mode: $newMagicGunMode").apply {
                            style.color = TextFormatting.GREEN
                        }, false)
            }
        }
        return false
    }
}