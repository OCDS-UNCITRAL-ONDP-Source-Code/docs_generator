package com.procurement.docs_generator.domain.model.document.context.mapper

import com.procurement.docs_generator.domain.model.document.context.WorksContext
import com.procurement.docs_generator.domain.model.release.ACReleasesPackage
import com.procurement.docs_generator.domain.model.release.EVReleasesPackage
import com.procurement.docs_generator.domain.model.release.MSReleasesPackage
import java.time.LocalDate
import java.time.Period

object WorksContextMapper {

    fun mapToContext(publishDate: LocalDate,
                     acRelease: ACReleasesPackage.Release,
                     evRelease: EVReleasesPackage.Release,
                     msRelease: MSReleasesPackage.Release
    ): Map<String, Any> {

        val partyBuyer = acRelease.parties.partyByRole(role = "buyer")
        val partySupplier = acRelease.parties.partyByRole(role = "supplier")

        val ctx = WorksContext(
            ac = WorksContext.AC(
                date = acRelease.date.toLocalDate(),
                contract = acRelease.contracts[0].let { contract ->
                    val contractPeriod = Period.between(
                        contract.period.startDate.toLocalDate(),
                        contract.period.endDate.toLocalDate()
                    )
                    WorksContext.AC.Contract(
                        id = contract.id,
                        description = contract.description,
                        monthNumber = (contractPeriod.years * 12) + contractPeriod.months + if (contractPeriod.days > 0) 1 else 0,
                        amount = contract.value.amount.toString(),
                        amountNet = contract.value.amountNet.toString(),
                        agreedMetrics = getContractAgreedMetrics(acRelease)
                    )
                },
                tender = acRelease.tender.let { tender ->
                    WorksContext.AC.Tender(
                        classification = tender.classification.let { classification ->
                            WorksContext.AC.Tender.Classification(
                                id = classification.id,
                                description = classification.description
                            )
                        },
                        procurementMethodDetails = getProcurementMethodDetails(msRelease)
                    )
                },
                buyer = partyBuyer.let { party ->
                    WorksContext.AC.Buyer(
                        address = party.address.let { address ->
                            WorksContext.AC.Buyer.Address(
                                country = address.addressDetails.country.description,
                                region = address.addressDetails.region.description,
                                locality = address.addressDetails.locality.description,
                                streetAddress = address.streetAddress,
                                postalCode = address.postalCode
                            )
                        },
                        identifier = party.identifier.let { identifier ->
                            WorksContext.AC.Buyer.Identifier(
                                id = identifier.id,
                                legalName = identifier.legalName
                            )
                        },
                        additionalIdentifiers = party.mapAdditionalIdentifiersByScheme(scheme = "MD-FISCAL") {
                            WorksContext.AC.Buyer.AdditionalIdentifier(id = it.id)
                        },
                        contactPoint = party.contactPoint.let { contactPoint ->
                            WorksContext.AC.Buyer.ContactPoint(
                                telephone = contactPoint.telephone,
                                fax = contactPoint.faxNumber
                            )
                        },
                        //persones required for buyer
                        persones = party.persones!!.map { person ->
                            WorksContext.AC.Buyer.Person(
                                title = person.title,
                                name = person.name,
                                businessFunctions = person.businessFunctions.mapBusinessFunctionByType(type = "authority") {
                                    WorksContext.AC.Buyer.Person.BusinessFunction(
                                        jobTitle = it.jobTitle
                                    )
                                }
                            )
                        },
                        //details required for buyer
                        details = party.details!!.let { detail ->
                            WorksContext.AC.Buyer.Details(
                                bankAccount = detail.bankAccounts[0].let { bankAccount ->
                                    WorksContext.AC.Buyer.Details.BankAccounts(
                                        accountIdentification = bankAccount.accountIdentification.id,
                                        identifier = bankAccount.identifier.id,
                                        name = bankAccount.bankName
                                    )
                                },
                                legalForm = WorksContext.AC.Buyer.Details.LegalForm(
                                    detail.legalForm.description
                                ),
                                permit = detail.permits.firstOrNullPermitByScheme(scheme = "SRL") { permit ->
                                    WorksContext.AC.Buyer.Details.Permit(
                                        id = permit.id,
                                        startDate = permit.permitDetails.validityPeriod.startDate.toLocalDate()
                                    )
                                }
                            )
                        }
                    )
                },
                supplier = partySupplier.let { party ->
                    WorksContext.AC.Supplier(
                        address = party.address.let { address ->
                            WorksContext.AC.Supplier.Address(
                                country = address.addressDetails.country.description,
                                region = address.addressDetails.region.description,
                                locality = address.addressDetails.locality.description,
                                streetAddress = address.streetAddress,
                                postalCode = address.postalCode
                            )
                        },
                        identifier = party.identifier.let { identifier ->
                            WorksContext.AC.Supplier.Identifier(
                                id = identifier.id,
                                legalName = identifier.legalName
                            )
                        },
                        additionalIdentifiers = party.mapAdditionalIdentifiersByScheme(scheme = "MD-FISCAL") {
                            WorksContext.AC.Supplier.AdditionalIdentifier(id = it.id)
                        },
                        contactPoint = party.contactPoint.let { contactPoint ->
                            WorksContext.AC.Supplier.ContactPoint(
                                telephone = contactPoint.telephone,
                                fax = contactPoint.faxNumber
                            )
                        },
                        //persones required for supplier
                        persones = party.persones!!.map { person ->
                            WorksContext.AC.Supplier.Person(
                                title = person.title,
                                name = person.name,
                                businessFunctions = person.businessFunctions.mapBusinessFunctionByType(type = "authority") { businessFunction ->
                                    WorksContext.AC.Supplier.Person.BusinessFunction(
                                        jobTitle = businessFunction.jobTitle
                                    )
                                }
                            )
                        },
                        //details required for supplier
                        details = party.details!!.let { detail ->
                            val permitSRL = detail.permits?.firstOrNull {
                                it.scheme.toUpperCase() == "SRL"
                            }

                            val permitSRLE = detail.permits?.firstOrNull {
                                it.scheme.toUpperCase() == "SRLE"
                            }

                            val permit = if (permitSRL != null || permitSRLE != null) {
                                val startDateSRLE =
                                    permitSRLE?.let { permit -> permit.permitDetails.validityPeriod.startDate.toLocalDate() }
                                val endDateSRLE =
                                    permitSRLE?.let { permit -> permit.permitDetails.validityPeriod.endDate?.toLocalDate() }

                                WorksContext.AC.Supplier.Details.Permit(
                                    idSRL = permitSRL?.id,
                                    startDateSRL = permitSRL?.let { permit -> permit.permitDetails.validityPeriod.startDate.toLocalDate() },
                                    idSRLE = permitSRLE?.id,
                                    startDateSRLE = startDateSRLE,
                                    yearsNumber = if (startDateSRLE != null && endDateSRLE != null) {
                                        val period = Period.between(startDateSRLE, endDateSRLE)
                                        period.years + if (period.months > 0 || period.days > 0) 1 else 0
                                    } else
                                        null,
                                    issuedBy = permitSRLE?.let { permit ->
                                        permit.permitDetails.issuedBy.let { issuedBy ->
                                            WorksContext.AC.Supplier.Details.Permit.IssuedBy(
                                                id = issuedBy.id,
                                                name = issuedBy.name
                                            )
                                        }
                                    },
                                    issuedThought = permitSRLE?.let { permit ->
                                        permit.permitDetails.issuedThought.let { issuedThought ->
                                            WorksContext.AC.Supplier.Details.Permit.IssuedThought(
                                                id = issuedThought.id,
                                                name = issuedThought.name
                                            )
                                        }
                                    }
                                )
                            } else
                                null

                            WorksContext.AC.Supplier.Details(
                                bankAccount = detail.bankAccounts[0].let { bankAccount ->
                                    WorksContext.AC.Supplier.Details.BankAccounts(
                                        accountIdentification = bankAccount.accountIdentification.id,
                                        identifier = bankAccount.identifier.id,
                                        name = bankAccount.bankName
                                    )
                                },
                                legalForm = WorksContext.AC.Supplier.Details.LegalForm(
                                    description = detail.legalForm.description
                                ),
                                permit = permit
                            )
                        }
                    )
                }
            ),
            ev = WorksContext.EV(
                publishDate = publishDate,
                tender = evRelease.tender.let { tender ->
                    WorksContext.EV.Tender(
                        id = tender.id
                    )
                }
            )
        )

        return mutableMapOf<String, Any>().apply {
            this["context"] = ctx
        }
    }

    private fun getProcurementMethodDetails(msRelease: MSReleasesPackage.Release): String {
        val amount = msRelease.tender.value.amount.toLong()
        return when {
            amount < 100000 -> "mv"
            amount in 100000..1500000 -> "sv"
            else -> "ot"
        }
    }

    private fun getContractAgreedMetrics(release: ACReleasesPackage.Release): WorksContext.AC.Contract.AgreedMetrics {
        val metrics = ContractAgreedMetrics(
            mutableMapOf<String, String>().apply {
                for (metric in release.contracts[0].agreedMetrics) {
                    if (metric.id.startsWith("cc-buyer")) {
                        metric.observations.forEach {
                            when (it.id) {
                                "cc-buyer-1-1" -> this["ccBuyer_1_1Measure"] = it.measure
                            }
                        }
                    }

                    if (metric.id.startsWith("cc-tenderer")) {
                        metric.observations.forEach {
                            when (it.id) {
                                "cc-tenderer-1-1" -> this["ccTenderer_1_1Measure"] = it.measure
                                "cc-tenderer-1-5" -> this["ccTenderer_1_5Measure"] = it.measure
                                "cc-tenderer-2-2" -> this["ccTenderer_2_2Measure"] = it.measure
                                "cc-tenderer-2-3" -> this["ccTenderer_2_3Measure"] = it.measure
                                "cc-tenderer-2-4" -> this["ccTenderer_2_4Measure"] = it.measure
                                "cc-tenderer-3-2" -> this["ccTenderer_3_2Measure"] = it.measure
                                "cc-tenderer-3-3" -> this["ccTenderer_3_3Measure"] = it.measure
                            }
                        }
                    }
                }
            }
        )

        return WorksContext.AC.Contract.AgreedMetrics(
            ccTenderer_1_1Measure = metrics.ccTenderer_1_1Measure,
            ccTenderer_1_5Measure = metrics.ccTenderer_1_5Measure,
            ccTenderer_2_2Measure = metrics.ccTenderer_2_2Measure,
            ccTenderer_2_3Measure = metrics.ccTenderer_2_3Measure,
            ccTenderer_2_4Measure = metrics.ccTenderer_2_4Measure,
            ccTenderer_3_2Measure = metrics.ccTenderer_3_2Measure,
            ccTenderer_3_3Measure = metrics.ccTenderer_3_3Measure,

            ccBuyer_1_1Measure = metrics.ccBuyer_1_1Measure
        )
    }

    private class ContractAgreedMetrics(props: Map<String, String>) {
        val ccTenderer_1_1Measure: String by props
        val ccTenderer_1_5Measure: String by props
        val ccTenderer_2_2Measure: String by props
        val ccTenderer_2_3Measure: String by props
        val ccTenderer_2_4Measure: String by props
        val ccTenderer_3_2Measure: String by props
        val ccTenderer_3_3Measure: String by props

        val ccBuyer_1_1Measure: String by props
    }
}