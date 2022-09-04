package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlin.math.log

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val currency: Currency,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun runningBillingForProvider() {
        invoiceService.fetchInvoicesByCurrency(currency).forEach {
            if (it.status == InvoiceStatus.PENDING) {
                try {
                    val result = paymentProvider.charge(it)
                    if (result) {
                        invoiceService.markInvoiceAsPaid(it.id)
                    } else {
                        invoiceService.markInvoiceAsFailed(it.id)
                    }
                } catch (e: CurrencyMismatchException) {
                    logger.error { "Invoice doesn't have the same currency as the one expected by the payment provider" }
                    invoiceService.markInvoiceAsFailed(it.id)
                } catch (e: CustomerNotFoundException) {
                    logger.error { "Customer not found for invoice ${it.id}" }
                    invoiceService.markInvoiceAsFailed(it.id)
                } catch (e: NetworkException) {
                    logger.error { "Network error when trying to charge invoice ${it.id}" }
                    invoiceService.markInvoiceAsRetryable(it.id)
                } catch (e: Exception) {
                    logger.error { "Unexpected exception while trying to charge invoice ${it.id}" }
                    invoiceService.markInvoiceAsFailed(it.id)
                }
            }
        }
    }
}
