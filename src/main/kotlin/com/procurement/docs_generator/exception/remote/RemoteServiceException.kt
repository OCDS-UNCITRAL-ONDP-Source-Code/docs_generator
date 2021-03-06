package com.procurement.docs_generator.exception.remote

import org.springframework.http.HttpStatus

class RemoteServiceException(val code: HttpStatus? = null,
                             val payload: String? = null,
                             message: String,
                             exception: Throwable? = null) :
    RuntimeException(message, exception)