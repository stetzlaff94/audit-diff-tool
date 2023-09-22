package com.stephentetzlaff.audit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.stephentetzlaff.audit.data.ChangeType
import com.stephentetzlaff.audit.data.ListUpdate
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
        nodeOld: JsonNode?,
        nodeNew: JsonNode?,
        path: String,
        changeLog: List<ChangeType>
    ): List<ChangeType> =

        when (nodeOld) {
            is ObjectNode -> nodeOld.diff(nodeNew as? ObjectNode, path)
            is ArrayNode -> nodeOld.diff(nodeNew as? ArrayNode, path)
            is ValueNode -> changeLog + nodeOld.diff(nodeNew as? ValueNode, path)
            null -> {
                when (nodeNew) {
                    null -> changeLog
                    is ObjectNode -> changeLog + nodeOld.diff(nodeNew, path)
                    is ArrayNode -> changeLog + nodeOld.diff(nodeNew, path)
                    is ValueNode -> changeLog + nodeOld.diff(nodeNew, path)
                    else -> throw IllegalArgumentException("Cannot compare $nodeOld to $nodeNew")
                }
            }
            else -> {
                throw IllegalArgumentException("Cannot compare $nodeOld to $nodeNew")
            }
        }

    private fun ValueNode?.diff(nodeNew: ValueNode?, path: String) = if (this != nodeNew) {
        listOf(
            PropertyUpdated(
                property = path,
                previous = this?.asText(),
                current = nodeNew?.asText()
            )
        )
    } else {
        emptyList()
    }

    private fun ObjectNode?.diff(nodeNew: ObjectNode?, path: String): List<ChangeType> {
        val allFieldNames = (this?.fieldNames()?.asSequence()?.toSet() ?: emptySet()) +
            (nodeNew?.fieldNames()?.asSequence()?.toSet() ?: emptySet())

        return allFieldNames.fold(emptyList()) { acc, fieldName ->
            val fieldOld = this?.get(fieldName)
            val fieldNew = nodeNew?.get(fieldName)
            acc + diff(fieldOld, fieldNew, "$path.$fieldName", emptyList())
        }
    }

    private fun ArrayNode?.diff(nodeNew: ArrayNode?, path: String): List<ChangeType> {
        return if ((this?.get(0)?.isValueNode ?: nodeNew?.get(0)?.isValueNode) == true) {
            listOf(this.diffPrimitiveList(nodeNew, path))
        } else {
            this.diffObjectList(nodeNew, path)
        }
    }

    private fun ArrayNode?.diffObjectList(nodeNew: ArrayNode?, path: String): List<ChangeType> {
        val map1 = this?.associateBy {
            it["id"]?.asText()
                ?: throw IllegalArgumentException("Cannot compare audit without id field or one labeled with @AuditId")
        } ?: emptyMap()
        val map2 = nodeNew?.associateBy {
            it["id"]?.asText()
                ?: throw IllegalArgumentException("Cannot compare audit without id field or one labeled with @AuditId")
        } ?: emptyMap()
        val allKeys = map1.keys + map2.keys
        return allKeys.associateWith { key ->
            Pair(map1[key], map2[key])
        }.flatMap {
            diff(it.value.first, it.value.second, "$path[${it.key}]", emptyList())
        }
    }

    private fun ArrayNode?.diffPrimitiveList(nodeNew: ArrayNode?, path: String): ListUpdate =
        this?.toSet().let { s1 ->
            nodeNew?.toSet().let { s2 ->
                val set1 = s1 ?: emptySet()
                val set2 = s2 ?: emptySet()
                ListUpdate(
                    path,
                    (set2 - set1).map {
                        it.asText()
                    },
                    (set1 - set2).map {
                        it.asText()
                    }
                )
            }
        }
}
