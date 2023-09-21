package com.stephentetzlaff.audit.data

sealed class ChangeType(
    open val property: String
)

data class PropertyUpdated(
    override val property: String,
    val previous: Any?,
    val current: Any?
) : ChangeType(property)

data class ListUpdate(
    override val property: String,
    val added: List<Any?>,
    val current: List<Any?>
) : ChangeType(property)
