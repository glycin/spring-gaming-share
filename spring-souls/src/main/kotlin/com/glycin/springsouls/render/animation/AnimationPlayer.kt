package com.glycin.springsouls.render.animation

import com.glycin.util.Mat4
import com.glycin.util.Quaternion
import com.glycin.util.Vec3

class AnimationPlayer(
    private val skeleton: Skeleton,
    private val animations: List<Animation>,
) {

    var currentAnimationIndex: Int = 0
    var currentTime: Float = 0f
    var speed: Float = 1f
    var looping: Boolean = true
    var defaultCrossfadeDuration: Float = 0.2f

    // Root motion extraction
    var rootMotionBoneName: String? = null
    var rootMotionOffsetX = 0f
        private set
    var rootMotionOffsetZ = 0f
        private set
    private var rootMotionBaseX = 0f
    private var rootMotionBaseZ = 0f
    private var rootMotionInitialized = false

    private var crossfade: Crossfade? = null

    private val nodeGlobalTransforms = Array(skeleton.nodes.size) { Mat4.identity() }
    private val flatBoneMatrices = FloatArray(skeleton.bones.size * 16)
    private val boneNodeIndices: IntArray

    private class Crossfade(
        val fromAnimIndex: Int,
        var fromTime: Float,
        val fromLooping: Boolean,
        val duration: Float,
        var elapsed: Float = 0f,
        val fromRmBoneName: String?,
        val fromRmBaseX: Float,
        val fromRmBaseZ: Float,
    )

    init {
        val nodeNameToIndex = mutableMapOf<String, Int>()
        skeleton.nodes.forEachIndexed { i, node -> nodeNameToIndex[node.name] = i }
        boneNodeIndices = IntArray(skeleton.bones.size) { nodeNameToIndex[skeleton.bones[it].name] ?: -1 }
    }

    fun update(deltaSeconds: Float) {
        if (animations.isEmpty()) return

        // Advance "to" animation time
        val toAnim = animations[currentAnimationIndex]
        currentTime += deltaSeconds * toAnim.ticksPerSecond * speed
        if (looping) {
            if (toAnim.duration > 0f) currentTime %= toAnim.duration
        } else {
            currentTime = currentTime.coerceAtMost(toAnim.duration)
        }

        // Advance crossfade
        val cf = crossfade
        var blendFactor = 1f
        var fromAnim: Animation? = null
        if (cf != null) {
            cf.elapsed += deltaSeconds
            blendFactor = (cf.elapsed / cf.duration).coerceIn(0f, 1f)
            fromAnim = animations[cf.fromAnimIndex]
            cf.fromTime += deltaSeconds * fromAnim.ticksPerSecond * speed
            if (cf.fromLooping) {
                if (fromAnim.duration > 0f) cf.fromTime %= fromAnim.duration
            } else {
                cf.fromTime = cf.fromTime.coerceAtMost(fromAnim.duration)
            }
            if (blendFactor >= 1f) crossfade = null
        }

        val rmBone = rootMotionBoneName
        val nodes = skeleton.nodes

        for (i in nodes.indices) {
            val node = nodes[i]
            val toChannel = toAnim.channels[node.name]
            val fromChannel = if (cf != null) fromAnim!!.channels[node.name] else null

            val localTransform: Mat4

            if (cf != null && toChannel != null && fromChannel != null) {
                // Both animations have channels — blend TRS
                var toPos = interpolateVec3(toChannel.positionKeys, currentTime)
                val toRot = interpolateRotation(toChannel.rotationKeys, currentTime)
                val toScale = interpolateVec3(toChannel.scalingKeys, currentTime)

                var fromPos = interpolateVec3(fromChannel.positionKeys, cf.fromTime)
                val fromRot = interpolateRotation(fromChannel.rotationKeys, cf.fromTime)
                val fromScale = interpolateVec3(fromChannel.scalingKeys, cf.fromTime)

                // Root motion stripping on both sides
                if (rmBone != null && node.name == rmBone) {
                    toPos = stripAndTrackRootMotion(toPos)
                }
                if (cf.fromRmBoneName != null && node.name == cf.fromRmBoneName) {
                    fromPos = Vec3(cf.fromRmBaseX, fromPos.y, cf.fromRmBaseZ)
                }

                val pos = fromPos.lerp(toPos, blendFactor)
                val rot = fromRot.slerp(toRot, blendFactor)
                val scale = fromScale.lerp(toScale, blendFactor)
                localTransform = Mat4.fromTranslation(pos) * rot.toMat4() * Mat4.fromScale(scale)

            } else if (toChannel != null) {
                // Only "to" has a channel
                var pos = interpolateVec3(toChannel.positionKeys, currentTime)
                val rot = interpolateRotation(toChannel.rotationKeys, currentTime)
                val scale = interpolateVec3(toChannel.scalingKeys, currentTime)

                if (rmBone != null && node.name == rmBone) {
                    pos = stripAndTrackRootMotion(pos)
                }

                localTransform = Mat4.fromTranslation(pos) * rot.toMat4() * Mat4.fromScale(scale)

            } else if (cf != null && fromChannel != null) {
                // Only "from" has a channel
                var pos = interpolateVec3(fromChannel.positionKeys, cf.fromTime)
                val rot = interpolateRotation(fromChannel.rotationKeys, cf.fromTime)
                val scale = interpolateVec3(fromChannel.scalingKeys, cf.fromTime)

                if (cf.fromRmBoneName != null && node.name == cf.fromRmBoneName) {
                    pos = Vec3(cf.fromRmBaseX, pos.y, cf.fromRmBaseZ)
                }

                localTransform = Mat4.fromTranslation(pos) * rot.toMat4() * Mat4.fromScale(scale)
            } else {
                localTransform = node.defaultTransform
            }

            nodeGlobalTransforms[i] = if (node.parentIndex >= 0) {
                nodeGlobalTransforms[node.parentIndex] * localTransform
            } else {
                localTransform
            }
        }

        for (bone in skeleton.bones) {
            val nodeIndex = boneNodeIndices[bone.id]
            val globalTransform = if (nodeIndex >= 0) {
                nodeGlobalTransforms[nodeIndex]
            } else {
                Mat4.identity()
            }

            val finalMatrix = skeleton.globalInverseTransform * globalTransform * bone.offsetMatrix
            System.arraycopy(finalMatrix.data, 0, flatBoneMatrices, bone.id * 16, 16)
        }
    }

    val isFinished: Boolean
        get() {
            if (looping || animations.isEmpty()) return false
            return currentTime >= animations[currentAnimationIndex].duration
        }

    fun getBoneMatrices(): FloatArray = flatBoneMatrices

    fun playAnimation(index: Int, crossfadeDuration: Float = defaultCrossfadeDuration) {
        if (index !in animations.indices) return
        if (index == currentAnimationIndex && crossfade == null) return

        if (crossfadeDuration > 0f) {
            crossfade = Crossfade(
                fromAnimIndex = currentAnimationIndex,
                fromTime = currentTime,
                fromLooping = looping,
                duration = crossfadeDuration,
                fromRmBoneName = rootMotionBoneName,
                fromRmBaseX = rootMotionBaseX,
                fromRmBaseZ = rootMotionBaseZ,
            )
        }

        currentAnimationIndex = index
        currentTime = 0f
        rootMotionInitialized = false
        rootMotionOffsetX = 0f
        rootMotionOffsetZ = 0f
    }

    fun playAnimation(name: String, crossfadeDuration: Float = defaultCrossfadeDuration) {
        val index = animations.indexOfFirst { it.name == name }
        if (index >= 0) playAnimation(index, crossfadeDuration)
    }

    private fun stripAndTrackRootMotion(pos: Vec3): Vec3 {
        if (!rootMotionInitialized) {
            rootMotionBaseX = pos.x
            rootMotionBaseZ = pos.z
            rootMotionInitialized = true
        }
        rootMotionOffsetX = pos.x - rootMotionBaseX
        rootMotionOffsetZ = pos.z - rootMotionBaseZ
        return Vec3(rootMotionBaseX, pos.y, rootMotionBaseZ)
    }

    private fun interpolateVec3(keys: List<VectorKey>, time: Float): Vec3 {
        if (keys.size == 1) return keys[0].value

        for (i in 0..<keys.size - 1) {
            if (time < keys[i + 1].time) {
                val t = (time - keys[i].time) / (keys[i + 1].time - keys[i].time)
                return keys[i].value.lerp(keys[i + 1].value, t)
            }
        }
        return keys.last().value
    }

    private fun interpolateRotation(keys: List<QuatKey>, time: Float): Quaternion {
        if (keys.size == 1) return keys[0].value

        for (i in 0..<keys.size - 1) {
            if (time < keys[i + 1].time) {
                val t = (time - keys[i].time) / (keys[i + 1].time - keys[i].time)
                return keys[i].value.slerp(keys[i + 1].value, t)
            }
        }
        return keys.last().value
    }
}
