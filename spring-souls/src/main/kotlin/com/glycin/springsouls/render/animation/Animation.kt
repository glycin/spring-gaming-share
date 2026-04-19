package com.glycin.springsouls.render.animation

import com.glycin.util.Mat4
import com.glycin.util.Quaternion
import com.glycin.util.Vec3

data class Bone(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val offsetMatrix: Mat4,
)

data class NodeInfo(
    val name: String,
    val parentIndex: Int,
    val defaultTransform: Mat4,
)

data class Skeleton(
    val bones: List<Bone>,
    val boneNameToId: Map<String, Int>,
    val globalInverseTransform: Mat4,
    val nodes: List<NodeInfo>,
)

data class VectorKey(val time: Float, val value: Vec3)
data class QuatKey(val time: Float, val value: Quaternion)

data class BoneAnimation(
    val boneName: String,
    val positionKeys: List<VectorKey>,
    val rotationKeys: List<QuatKey>,
    val scalingKeys: List<VectorKey>,
)

data class Animation(
    val name: String,
    val duration: Float,
    val ticksPerSecond: Float,
    val channels: Map<String, BoneAnimation>,
) {
    fun stripRootMotion(rootBoneName: String): Animation {
        val channel = channels[rootBoneName] ?: return this
        if (channel.positionKeys.isEmpty()) return this

        val base = channel.positionKeys.first().value
        val fixedKeys = channel.positionKeys.map { key ->
            VectorKey(key.time, Vec3(base.x, key.value.y, base.z))
        }
        val fixedChannel = channel.copy(positionKeys = fixedKeys)
        return copy(channels = channels + (rootBoneName to fixedChannel))
    }

    fun remapChannels(nodeNames: Set<String>): Animation {
        if (channels.keys.all { it in nodeNames }) return this

        val remapped = mutableMapOf<String, BoneAnimation>()
        for ((channelName, anim) in channels) {
            val stripped = channelName.substringAfterLast(':')
            val target = nodeNames.find { it == stripped || it.substringAfterLast(':') == stripped }
            if (target != null) {
                remapped[target] = anim.copy(boneName = target)
            }
        }
        return copy(channels = remapped)
    }
}
