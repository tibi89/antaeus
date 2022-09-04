package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    val currency: Currency,
    private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun runningBillingForProvider() {
        runBlocking {
            // fixme: this should be done in a transaction, as we're reading invoices and then updating them
            // using flatmapmerge to run the payment in parallel up to the concurrency level
            invoiceService.fetchPendingInvoicesByCurrency(currency).asFlow().flatMapMerge(concurrency = 5) {
                flow {
                    processInvoice(it)
                    emit(it)
                }
            }
                .collect()
        }
    }

    private fun processInvoice(invoice: Invoice) {
        if (invoice.status == InvoiceStatus.PENDING) {
            try {
                val result = paymentProvider.charge(invoice)
                if (result) {
                    invoiceService.markInvoiceAsPaid(invoice.id)
                } else {
                    invoiceService.markInvoiceAsFailed(invoice.id)
                }
            } catch (e: CurrencyMismatchException) {
                logger.error { "Invoice doesn't have the same currency as the one expected by the payment provider" }
                invoiceService.markInvoiceAsFailed(invoice.id)
            } catch (e: CustomerNotFoundException) {
                logger.error { "Customer not found for invoice ${invoice.id}" }
                invoiceService.markInvoiceAsFailed(invoice.id)
            } catch (e: NetworkException) {
                logger.error { "Network error when trying to charge invoice ${invoice.id}" }
                invoiceService.markInvoiceAsRetryable(invoice.id)
            } catch (e: Exception) {
                logger.error { "Unexpected exception while trying to charge invoice ${invoice.id}" }
                invoiceService.markInvoiceAsFailed(invoice.id)
            }
        }
    }
}
