package com.procurement.docs_generator.infrastructure.dispatcher

import com.procurement.docs_generator.configuration.properties.GlobalProperties
import org.springframework.http.HttpStatus

interface CodeError {
    val httpStatus: HttpStatus
    val code: String
}

enum class CodesOfErrors(final override val httpStatus: HttpStatus, group: String, id: String) : CodeError {
    INVALID_VALUE_OF_PARAM(     httpStatus = HttpStatus.BAD_REQUEST,            group = "01", id = "01"),
    FILE_ERROR(                 httpStatus = HttpStatus.BAD_REQUEST,            group = "01", id = "02"),

    INCORRECT_FORMAT_MESSAGE(   httpStatus = HttpStatus.BAD_REQUEST,            group = "02", id = "01"),
    BAD_PAYLOAD_COMMAND(        httpStatus = HttpStatus.BAD_REQUEST,            group = "02", id = "02"),

    NO_AVAILABLE_TEMPLATE(      httpStatus = HttpStatus.BAD_REQUEST,            group = "03", id = "01"),
    TEMPLATE_IS_ALREADY(        httpStatus = HttpStatus.BAD_REQUEST,            group = "03", id = "02"),
    TEMPLATE_UPDATE_ERROR(      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,  group = "03", id = "03"),
    TEMPLATE_INVALID_FORMAT(    httpStatus = HttpStatus.BAD_REQUEST,            group = "03", id = "04"),

    //Common
    SERVER_ERROR(               httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,  group = "00", id = "00");

    override val code: String = "${httpStatus.value()}.${GlobalProperties.serviceId}.$group.$id"

    override fun toString(): String = code
}
