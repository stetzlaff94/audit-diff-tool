package com.stephentetzlaff.audit

import com.fasterxml.jackson.databind.JsonNode
import com.stephentetzlaff.audit.data.ChangeType
import com.stephentetzlaff.audit.data.PropertyUpdated
import com.stephentetzlaff.audit.factories.defaultObjectMapper

fun interface DiffTool {
    fun diff(itemOld: Any?, itemNew: Any?): List<ChangeType>
}

class DefaultDiffTool : DiffTool {
    override fun diff(itemOld: Any?, itemNew: Any?) = diff(
        defaultObjectMapper.valueToTree(itemOld),
        defaultObjectMapper.valueToTree(itemNew),
        "",
        emptyList()
    )

    private fun diff(
        nodeOld: JsonNode,
        nodeNew: JsonNode,
        path: String,
        changeLog: List<ChangeType>
    ): List<ChangeType> = when {
        nodeOld.isObject && nodeNew.isObject -> {
            nodeOld.fieldNames().asSequence().toSet()
                .intersect(nodeNew.fieldNames().asSequence().toSet())
                .fold(changeLog) { acc, it ->
                    acc + diff(nodeOld[it], nodeNew[it], "$path.$it", changeLog)
                }
        }
        nodeOld.isArray && nodeNew.isArray -> {
            TODO()
            emptyList()
        }

        nodeOld.isValueNode && nodeNew.isValueNode -> {
            if (nodeOld == nodeNew) {
                changeLog
            } else {
                changeLog + PropertyUpdated(
                    property = path,
                    previous = nodeOld.asText(),
                    current = nodeNew.asText()
                )
            }
        }

        else -> {
            throw IllegalArgumentException("Cannot compare $nodeOld to $nodeNew")
        }
    }

    fun <K, V> mergeMaps(map1: Map<K, V>, map2: Map<K, V>): Map<K, Pair<V?, V?>> {
        val allKeys = map1.keys + map2.keys
        return allKeys.associateWith { key ->
            map1.getOrDefault(key, null) to map2.getOrDefault(key, null)
        }
    }
}
