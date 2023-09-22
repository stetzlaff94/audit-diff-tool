package com.stephentetzlaff.audit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
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
    ): List<ChangeType> {
        // This is ugly but its nice to have the Nodes typed.
        val nodeOldNulled = if (nodeOld is NullNode) null else nodeOld
        return when (nodeOldNulled) {
            is ObjectNode -> changeLog + diff(nodeOldNulled, nodeNew as? ObjectNode, path)
            is ArrayNode -> changeLog + diff(nodeOldNulled, nodeNew as? ArrayNode, path)
            is ValueNode -> changeLog + diff(nodeOldNulled, nodeNew as? ValueNode, path)
            null -> {
                when (nodeNew) {
                    null -> changeLog
                    is ObjectNode -> changeLog + diff(null, nodeNew, path)
                    is ArrayNode -> changeLog + diff(null, nodeNew, path)
                    is ValueNode -> changeLog + diff(null, nodeNew, path)
                    else -> throw IllegalArgumentException("Cannot compare $nodeOld to $nodeNew")
                }
            }

            else -> {
                throw IllegalArgumentException("Cannot compare $nodeOld to $nodeNew")
            }
        }
    }

    private fun diff(nodeOld: ValueNode?, nodeNew: ValueNode?, path: String) = if (nodeOld != nodeNew) {
        listOf(
            PropertyUpdated(
                property = path,
                previous = preservePrimitiveType(nodeOld),
                current = preservePrimitiveType(nodeNew)
            )
        )
    } else {
        emptyList()
    }

    /**
     * Preserve the primitive type of the node.
     */
    private fun preservePrimitiveType(node: ValueNode?): Any? {
        return when (node) {
            null -> null
            is NumericNode -> node.numberValue()
            is BinaryNode -> node.binaryValue()
            is BooleanNode -> node.booleanValue()
            is MissingNode -> null
            is NullNode -> null
            is TextNode -> node.textValue()
            else -> node.toString()
        }
    }

    private fun diff(nodeOld: ObjectNode?, nodeNew: ObjectNode?, path: String): List<ChangeType> {
        val allFieldNames = (nodeOld?.fieldNames()?.asSequence()?.toSet() ?: emptySet()) +
            (nodeNew?.fieldNames()?.asSequence()?.toSet() ?: emptySet())

        return allFieldNames.fold(emptyList()) { acc, fieldName ->
            val fieldOld = nodeOld?.get(fieldName)
            val fieldNew = nodeNew?.get(fieldName)
            acc + diff(fieldOld, fieldNew, "$path.$fieldName", emptyList())
        }
    }

    private fun diff(nodeOld: ArrayNode?, nodeNew: ArrayNode?, path: String): List<ChangeType> {
        return if ((nodeOld?.get(0)?.isValueNode ?: nodeNew?.get(0)?.isValueNode) == true) {
            listOf(diffPrimitiveList(nodeOld, nodeNew, path))
        } else {
            diffObjectList(nodeOld, nodeNew, path)
        }
    }

    private fun diffObjectList(nodeOld: ArrayNode?, nodeNew: ArrayNode?, path: String): List<ChangeType> {
        val map1 = nodeOld?.associateBy {
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

    private fun diffPrimitiveList(nodeOld: ArrayNode?, nodeNew: ArrayNode?, path: String): ListUpdate =
        nodeOld?.toSet().let { s1 ->
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
