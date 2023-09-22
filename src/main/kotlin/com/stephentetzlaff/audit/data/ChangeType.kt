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
    val removed: List<Any?>
) : ChangeType(property)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuditId
