package com.procurement.docs_generator.domain.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.procurement.docs_generator.domain.model.command.id.CommandId
import com.procurement.docs_generator.domain.model.command.id.CommandIdDeserializer
import com.procurement.docs_generator.domain.model.command.id.CommandIdSerializer
import com.procurement.docs_generator.domain.model.version.ApiVersion
import com.procurement.docs_generator.domain.model.version.ApiVersionDeserializer
import com.procurement.docs_generator.domain.model.version.ApiVersionSerializer
import com.procurement.docs_generator.domain.view.View

@JsonPropertyOrder("version", "id", "errors")
class CommandError(
    @JsonDeserialize(using = ApiVersionDeserializer::class)
    @JsonSerialize(using = ApiVersionSerializer::class)
    @field:JsonProperty("version") @param:JsonProperty("version") val version: ApiVersion,

    @JsonDeserialize(using = CommandIdDeserializer::class)
    @JsonSerialize(using = CommandIdSerializer::class)
    @field:JsonProperty("id") @param:JsonProperty("id") val id: CommandId,

    @field:JsonProperty("errors") @param:JsonProperty("errors") val errors: List<Error>
) : View {
    @JsonPropertyOrder("code", "description")
    data class Error(
        @field:JsonProperty("code") @param:JsonProperty("code") private val code: String,
        @field:JsonProperty("description") @param:JsonProperty("description") private val description: String
    )
}