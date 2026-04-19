package com.glycin.util

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

object YamlSettingsWriter {

    fun writeAll(paths: List<Path>, values: Map<String, Any>) {
        paths.forEach { write(it, values) }
    }

    fun write(yamlPath: Path, values: Map<String, Any>) {
        val yaml = Yaml()
        val file = yamlPath.toFile()
        val data: MutableMap<String, Any> = if (file.exists()) {
            file.reader().use { yaml.load(it) } ?: mutableMapOf()
        } else {
            mutableMapOf()
        }

        for ((dottedKey, value) in values) {
            setNested(data, dottedKey.split("."), value)
        }

        val dumperOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        val outputYaml = Yaml(dumperOptions)
        yamlPath.toFile().writer().use { outputYaml.dump(data, it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setNested(map: MutableMap<String, Any>, keys: List<String>, value: Any) {
        val key = keys.first()
        if (keys.size == 1) {
            map[key] = value
        } else {
            val child = map.getOrPut(key) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
            setNested(child, keys.drop(1), value)
        }
    }
}