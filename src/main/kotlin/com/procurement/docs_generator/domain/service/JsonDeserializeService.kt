package com.procurement.docs_generator.domain.service

inline fun <reified T> JsonDeserializeService.deserialize(json: String): T = this.deserialize(json, T::class.java)

interface JsonDeserializeService {
    fun <T> deserialize(json: String, targetClass: Class<T>): T
    fun toMap(json: String): Map<String, Any>
}