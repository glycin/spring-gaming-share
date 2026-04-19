package com.glycin.springsouls.render

import com.glycin.springsouls.render.animation.Animation
import com.glycin.springsouls.render.animation.AnimationLoader
import com.glycin.springsouls.render.animation.Skeleton
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.slf4j.LoggerFactory
import java.nio.IntBuffer

data class Color3(val r: Float, val g: Float, val b: Float)

data class MeshMaterial(
    val diffuse: Color3 = Color3(0.8f, 0.8f, 0.8f),
    val texture: Texture? = null,
)

data class ModelLoadResult(
    val meshes: List<Mesh>,
    val skeleton: Skeleton,
    val animations: List<Animation>,
    val materials: List<MeshMaterial> = emptyList(),
)

class ModelLoader {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelLoader::class.java)

        private const val DEFAULT_FLAGS = aiProcess_Triangulate or
                aiProcess_GenNormals or
                aiProcess_FlipUVs or
                aiProcess_JoinIdenticalVertices

        private const val ANIMATED_FLAGS = aiProcess_Triangulate or
                aiProcess_GenSmoothNormals or
                aiProcess_FlipUVs or
                aiProcess_JoinIdenticalVertices or
                aiProcess_LimitBoneWeights

        fun load(filePath: String, flags: Int = DEFAULT_FLAGS): List<Mesh> {
            val scene = aiImportFile(filePath, flags)
                ?: error("Failed to load model: $filePath\n${aiGetErrorString()}")

            val meshBuffer = scene.mMeshes()!!
            val meshes = (0..<scene.mNumMeshes()).map { i ->
                processMesh(AIMesh.create(meshBuffer.get(i)))
            }

            aiReleaseImport(scene)
            return meshes
        }

        fun loadFromResources(resourcePath: String, flags: Int = DEFAULT_FLAGS): List<Mesh> =
            load(resolveResourcePath(resourcePath), flags)

        fun loadAnimated(filePath: String): ModelLoadResult {
            val scene = aiImportFile(filePath, ANIMATED_FLAGS)
                ?: error("Failed to load animated model: $filePath\n${aiGetErrorString()}")

            val skeleton = AnimationLoader.loadSkeleton(scene)
            val animations = AnimationLoader.loadAnimations(scene)

            // Extract all embedded textures, indexed by *index, filename, and basename
            val embeddedTextures = mutableMapOf<String, Texture>()
            val texBuffer = scene.mTextures()
            if (texBuffer != null) {
                for (i in 0..<scene.mNumTextures()) {
                    val aiTexture = AITexture.create(texBuffer.get(i))
                    val filename = aiTexture.mFilename().dataString()
                    val texture = Texture.fromEmbedded(aiTexture)
                    embeddedTextures["*$i"] = texture
                    if (filename.isNotBlank()) {
                        val basename = filename.substringAfterLast('/').substringAfterLast('\\')
                        embeddedTextures[filename] = texture
                        embeddedTextures[basename] = texture
                    }
                }
            }

            val meshes = mutableListOf<Mesh>()
            val materials = mutableListOf<MeshMaterial>()
            for (i in 0..<scene.mNumMeshes()) {
                val aiMesh = AIMesh.create(scene.mMeshes()!!.get(i))
                val (boneIds, boneWeights) = AnimationLoader.extractBoneWeights(
                    aiMesh, skeleton.boneNameToId, aiMesh.mNumVertices()
                )
                meshes.add(processMesh(aiMesh, boneIds, boneWeights))
                materials.add(resolveMeshMaterial(scene, aiMesh, embeddedTextures))
            }

            aiReleaseImport(scene)
            return ModelLoadResult(meshes, skeleton, animations, materials)
        }

        fun loadAnimatedFromResources(resourcePath: String): ModelLoadResult =
            loadAnimated(resolveResourcePath(resourcePath))

        fun loadAnimationsFromResources(resourcePath: String): List<Animation> {
            val filePath = resolveResourcePath(resourcePath)
            val scene = aiImportFile(filePath, ANIMATED_FLAGS)
                ?: error("Failed to load animation: $filePath\n${aiGetErrorString()}")

            val animations = AnimationLoader.loadAnimations(scene)
            aiReleaseImport(scene)
            return animations
        }

        private fun resolveResourcePath(resourcePath: String): String {
            val url = ModelLoader::class.java.classLoader.getResource(resourcePath)
                ?: error("Resource not found: $resourcePath")
            return java.io.File(url.toURI()).absolutePath
        }

        private fun resolveMeshMaterial(scene: AIScene, aiMesh: AIMesh, embeddedTextures: Map<String, Texture>): MeshMaterial {
            val matBuffer = scene.mMaterials() ?: return MeshMaterial()
            val material = AIMaterial.create(matBuffer.get(aiMesh.mMaterialIndex()))

            val color = AIColor4D.create()
            val diffuse = if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color) == aiReturn_SUCCESS) {
                Color3(color.r(), color.g(), color.b())
            } else {
                Color3(0.8f, 0.8f, 0.8f)
            }

            val textureTypes = intArrayOf(aiTextureType_DIFFUSE, aiTextureType_BASE_COLOR)
            var texture: Texture? = null
            val path = AIString.create()
            for (type in textureTypes) {
                val texResult = aiGetMaterialTexture(
                    material, type, 0, path,
                    null as IntBuffer?, null as IntBuffer?, null, null as IntBuffer?, null as IntBuffer?, null as IntBuffer?
                )
                if (texResult == aiReturn_SUCCESS) {
                    val texPath = path.dataString()
                    val basename = texPath.substringAfterLast('/').substringAfterLast('\\')
                    texture = embeddedTextures[texPath]
                        ?: embeddedTextures[basename]
                        ?: if (texPath.startsWith("*")) embeddedTextures[texPath] else null
                    if (texture != null) break
                }
            }

            return MeshMaterial(diffuse, texture)
        }

        private fun processMesh(
            aiMesh: AIMesh,
            boneIds: IntArray? = null,
            boneWeights: FloatArray? = null,
        ): Mesh {
            val vertexCount = aiMesh.mNumVertices()

            val positions = FloatArray(vertexCount * 3)
            val normals = FloatArray(vertexCount * 3)
            val texCoords = FloatArray(vertexCount * 2)

            for (i in 0..<vertexCount) {
                val pos = aiMesh.mVertices().get(i)
                positions[i * 3] = pos.x()
                positions[i * 3 + 1] = pos.y()
                positions[i * 3 + 2] = pos.z()

                if (aiMesh.mNormals() != null) {
                    val norm = aiMesh.mNormals()!!.get(i)
                    normals[i * 3] = norm.x()
                    normals[i * 3 + 1] = norm.y()
                    normals[i * 3 + 2] = norm.z()
                }

                val uvSet = aiMesh.mTextureCoords(0)
                if (uvSet != null) {
                    val uv = uvSet.get(i)
                    texCoords[i * 2] = uv.x()
                    texCoords[i * 2 + 1] = uv.y()
                }
            }

            val indices = buildList {
                for (i in 0..<aiMesh.mNumFaces()) {
                    val face = aiMesh.mFaces().get(i)
                    for (j in 0..<face.mNumIndices()) {
                        add(face.mIndices().get(j))
                    }
                }
            }.toIntArray()

            return Mesh(positions, normals, texCoords, indices, boneIds, boneWeights)
        }
    }
}
