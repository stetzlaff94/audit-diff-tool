package com.stephentetzlaff.audit

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.stephentetzlaff.audit.data.AuditId
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class AuditIdCollectionSerializer : JsonSerializer<Collection<*>>() {
    override fun serialize(value: Collection<*>, gen: JsonGenerator, serializers: SerializerProvider) {
        val objectMapper = gen.codec as ObjectMapper
        val arrayNode: ArrayNode = objectMapper.createArrayNode()
        value.forEach { element ->
            when {
                element.isNotPrimitive() -> {
                    val itemNode = objectMapper.valueToTree<ObjectNode>(element)
                    element!!::class.memberProperties.find {
                        it.findAnnotation<AuditId>() != null
                    }?.let { property ->
                        val idValue: Any? = property.getter.call(element)
                        itemNode.put("id", idValue.toString())
                    }
                    arrayNode.add(itemNode)
                }
                else -> arrayNode.addPOJO(element)
            }
        }
        gen.writeTree(arrayNode)
    }

    private fun Any?.isNotPrimitive() = when (this) {
        null, is Byte, is Short, is Int, is Long, is Float, is Double, is Char, is Boolean, is String -> false
        else -> true
    }
}
