package com.procurement.docs_generator.infrastructure.dispatcher

import com.procurement.docs_generator.application.service.document.DocumentService
import com.procurement.docs_generator.application.service.kafka.KafkaMessageHandler
import com.procurement.docs_generator.configuration.properties.GlobalProperties
import com.procurement.docs_generator.domain.command.Command
import com.procurement.docs_generator.domain.command.CommandError
import com.procurement.docs_generator.domain.command.ac.ContractFinalizationCommand
import com.procurement.docs_generator.domain.command.ac.GenerateACDocCommand
import com.procurement.docs_generator.domain.logger.Logger
import com.procurement.docs_generator.domain.logger.debug
import com.procurement.docs_generator.domain.logger.error
import com.procurement.docs_generator.domain.model.command.id.CommandId
import com.procurement.docs_generator.domain.model.command.name.CommandName.GENERATE_AC_DOC
import com.procurement.docs_generator.domain.service.JsonDeserializeService
import com.procurement.docs_generator.domain.service.JsonSerializeService
import com.procurement.docs_generator.domain.service.deserialize
import com.procurement.docs_generator.domain.view.MessageErrorView
import com.procurement.docs_generator.exception.app.ApplicationException
import com.procurement.docs_generator.exception.json.JsonParseToObjectException
import com.procurement.docs_generator.infrastructure.logger.Slf4jLogger
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.*

@Service
class CommandDispatcher(
    private val deserializer: JsonDeserializeService,
    private val serialize: JsonSerializeService,
    private val documentService: DocumentService
) : KafkaMessageHandler {

    companion object {
        private val log: Logger = Slf4jLogger()
    }

    override fun handle(message: String): String {
        val result = try {
            val command: Command = deserializer.deserialize(message)
            MDC.put("command-id", command.id.value.toString())
            MDC.put("command-name", command.name.code)

            commandDispatcher(command, message)
        } catch (exception: JsonParseToObjectException) {
            MessageErrorView(
                errors = listOf(
                    MessageErrorView.Error(
                        code = CodesOfErrors.INCORRECT_FORMAT_MESSAGE.code,
                        description = "Incorrect format of message ($message)."
                    )
                )
            )
        } finally {
            MDC.remove("command-name")
            MDC.remove("command-id")
        }

        return serialize.serialize(result)
    }

    private fun commandDispatcher(command: Command, body: String): Any {
        log.debug { "Retrieve command: $body" }

        return when (command.name) {
            GENERATE_AC_DOC -> {
                try {
                    val data = deserializer.deserialize<GenerateACDocCommand>(body)
                        .let { documentService.processing(it) }
                    ContractFinalizationCommand(
                        version = GlobalProperties.App.apiVersion,
                        id = CommandId(UUID.randomUUID()),
                        name = "contractFinalization",
                        data = data
                    )
                } catch (exception: Throwable) {
                    errorHandler(command, exception)
                }
            }
        }
    }

    private fun errorHandler(command: Command, exception: Throwable): CommandError {
        return when (exception) {
            is JsonParseToObjectException -> {
                log.error { exception.message!! }
                errorView(
                    commandId = command.id,
                    codeError = CodesOfErrors.BAD_PAYLOAD_COMMAND,
                    description = "The bad data of command."
                )
            }

            is ApplicationException -> {
                log.perform(level = exception.loglevel, exception = exception, message = exception.message!!)
                errorView(
                    commandId = command.id,
                    codeError = exception.codeError,
                    description = exception.message!!
                )
            }

            else -> {
                log.error(exception) { exception.message!! }
                errorView(
                    commandId = command.id,
                    codeError = CodesOfErrors.SERVER_ERROR,
                    description = "Unknown server error."
                )
            }
        }
    }

    private fun errorView(commandId: CommandId, codeError: CodeError, description: String): CommandError {
        return CommandError(
            id = commandId,
            version = GlobalProperties.App.apiVersion,
            errors = listOf(
                CommandError.Error(
                    code = codeError.code,
                    description = description
                )
            )
        )
    }
}