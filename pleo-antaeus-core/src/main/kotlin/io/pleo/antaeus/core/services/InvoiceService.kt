/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchInvoicesByCurrency(currency: Currency): List<Invoice> {
        return dal.fetchInvoicesByCurrency(currency)
    }

    fun markInvoiceAsPaid(id: Int) =
        dal.updateStatusOfInvoice(id, InvoiceStatus.PAID)

    fun markInvoiceAsFailed(id: Int) =
        dal.updateStatusOfInvoice(id, InvoiceStatus.FAILED)

    fun markInvoiceAsRetryable(id: Int) =
        dal.updateStatusOfInvoice(id, InvoiceStatus.PENDING)
}
