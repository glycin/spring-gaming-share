package com.glycin.springsouls.render.animation

import com.glycin.util.Mat4
import com.glycin.util.Quaternion
import com.glycin.util.Vec3
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.slf4j.LoggerFactory

const val MAX_BONES = 128
const val MAX_BONE_INFLUENCES = 4

class AnimationLoader {

    companion object {
        private val logger = LoggerFactory.getLogger(AnimationLoader::class.java)

        fun loadSkeleton(scene: AIScene): Skeleton {
            val boneNameToId = mutableMapOf<String, Int>()
            val offsetMatrices = mutableMapOf<String, Mat4>()

            val meshBuffer = scene.mMeshes()!!
            for (i in 0..<scene.mNumMeshes()) {
                val aiMesh = AIMesh.create(meshBuffer.get(i))
                val boneBuffer = aiMesh.mBones() ?: continue
                for (j in 0..<aiMesh.mNumBones()) {
                    val aiBone = AIBone.create(boneBuffer.get(j))
                    val name = aiBone.mName().dataString()
                    if (name !in boneNameToId) {
                        boneNameToId[name] = boneNameToId.size
                        offsetMatrices[name] = aiBone.mOffsetMatrix().toMat4()
                    }
                }
            }

            val nodes = mutableListOf<NodeInfo>()
            collectNodes(scene.mRootNode()!!, -1, nodes)

            val nodeNameToIndex = mutableMapOf<String, Int>()
            nodes.forEachIndexed { i, node -> nodeNameToIndex[node.name] = i }

            val parentMap = mutableMapOf<String, String?>()
            buildParentMap(scene.mRootNode()!!, null, boneNameToId, parentMap)

            val bones = boneNameToId.entries.sortedBy { it.value }.map { (name, id) ->
                Bone(
                    id = id,
                    name = name,
                    parentId = parentMap[name]?.let { boneNameToId[it] },
                    offsetMatrix = offsetMatrices[name] ?: Mat4.identity(),
                )
            }

            val rootTransform = scene.mRootNode()!!.mTransformation().toMat4()
            val globalInverse = rootTransform.inverse()

            return Skeleton(bones, boneNameToId, globalInverse, nodes)
        }

        fun loadAnimations(scene: AIScene): List<Animation> {
            val animBuffer = scene.mAnimations() ?: return emptyList()
            val animations = mutableListOf<Animation>()

            for (i in 0..<scene.mNumAnimations()) {
                val aiAnim = AIAnimation.create(animBuffer.get(i))
                val channels = mutableMapOf<String, BoneAnimation>()

                for (j in 0..<aiAnim.mNumChannels()) {
                    val channel = AINodeAnim.create(aiAnim.mChannels()!!.get(j))
                    val boneName = channel.mNodeName().dataString()

                    val posKeys = (0..<channel.mNumPositionKeys()).map { k ->
                        val key = channel.mPositionKeys()!!.get(k)
                        val v = key.mValue()
                        VectorKey(key.mTime().toFloat(), Vec3(v.x(), v.y(), v.z()))
                    }

                    val rotKeys = (0..<channel.mNumRotationKeys()).map { k ->
                        val key = channel.mRotationKeys()!!.get(k)
                        val q = key.mValue()
                        QuatKey(key.mTime().toFloat(), Quaternion(q.x(), q.y(), q.z(), q.w()))
                    }

                    val scaleKeys = (0..<channel.mNumScalingKeys()).map { k ->
                        val key = channel.mScalingKeys()!!.get(k)
                        val v = key.mValue()
                        VectorKey(key.mTime().toFloat(), Vec3(v.x(), v.y(), v.z()))
                    }

                    channels[boneName] = BoneAnimation(boneName, posKeys, rotKeys, scaleKeys)
                }

                val tps = if (aiAnim.mTicksPerSecond() != 0.0) aiAnim.mTicksPerSecond().toFloat() else 25f
                animations.add(Animation(
                    name = aiAnim.mName().dataString(),
                    duration = aiAnim.mDuration().toFloat(),
                    ticksPerSecond = tps,
                    channels = channels,
                ))
            }

            return animations
        }

        fun extractBoneWeights(
            aiMesh: AIMesh,
            boneNameToId: Map<String, Int>,
            vertexCount: Int,
        ): Pair<IntArray, FloatArray> {
            val boneIds = IntArray(vertexCount * MAX_BONE_INFLUENCES)
            val boneWeights = FloatArray(vertexCount * MAX_BONE_INFLUENCES)
            val influenceCount = IntArray(vertexCount)

            val boneBuffer = aiMesh.mBones() ?: return boneIds to boneWeights
            for (i in 0..<aiMesh.mNumBones()) {
                val aiBone = AIBone.create(boneBuffer.get(i))
                val boneId = boneNameToId[aiBone.mName().dataString()] ?: continue

                val weightBuffer = aiBone.mWeights()
                for (j in 0..<aiBone.mNumWeights()) {
                    val weight = weightBuffer.get(j)
                    val vertexId = weight.mVertexId()
                    val slot = influenceCount[vertexId]
                    if (slot < MAX_BONE_INFLUENCES) {
                        boneIds[vertexId * MAX_BONE_INFLUENCES + slot] = boneId
                        boneWeights[vertexId * MAX_BONE_INFLUENCES + slot] = weight.mWeight()
                        influenceCount[vertexId]++
                    }
                }
            }

            return boneIds to boneWeights
        }

        private fun collectNodes(node: AINode, parentIndex: Int, result: MutableList<NodeInfo>) {
            val index = result.size
            result.add(NodeInfo(node.mName().dataString(), parentIndex, node.mTransformation().toMat4()))
            val children = node.mChildren()
            if (children != null) {
                for (i in 0..<node.mNumChildren()) {
                    collectNodes(AINode.create(children.get(i)), index, result)
                }
            }
        }

        private fun buildParentMap(
            node: AINode,
            parentName: String?,
            boneNames: Map<String, Int>,
            result: MutableMap<String, String?>,
        ) {
            val name = node.mName().dataString()
            if (name in boneNames) {
                result[name] = parentName
            }
            val boneParent = if (name in boneNames) name else parentName
            val children = node.mChildren()
            if (children != null) {
                for (i in 0..<node.mNumChildren()) {
                    buildParentMap(AINode.create(children.get(i)), boneParent, boneNames, result)
                }
            }
        }

        private fun AIMatrix4x4.toMat4(): Mat4 = Mat4(floatArrayOf(
            a1(), b1(), c1(), d1(),
            a2(), b2(), c2(), d2(),
            a3(), b3(), c3(), d3(),
            a4(), b4(), c4(), d4(),
        ))
    }
}
