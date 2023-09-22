package com.stephentetzlaff.audit.factories

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.stephentetzlaff.audit.AuditIdCollectionSerializer

val defaultObjectMapper = ObjectMapper().apply {
    registerModules(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build(),
        SimpleModule().apply {
            addSerializer(Collection::class.java, AuditIdCollectionSerializer())
        }
    )
}
