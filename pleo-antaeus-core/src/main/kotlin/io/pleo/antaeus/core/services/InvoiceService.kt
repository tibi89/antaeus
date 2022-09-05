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

    fun fetchPendingInvoicesByCurrency(currency: Currency, limit: Int = 100): List<Invoice> {
        return dal.fetchPendingInvoicesByCurrency(currency, limit)
    }

    fun markInvoiceAsPaid(id: Int) =
        dal.updateStatusOfInvoice(id, InvoiceStatus.PAID)

    fun markInvoiceAsFailed(id: Int) =
        dal.updateStatusOfInvoice(id, InvoiceStatus.FAILED)

    fun markInvoiceAsRetryable(id: Int) =
        dal.makeInvoiceAsRetryable(id)
}
