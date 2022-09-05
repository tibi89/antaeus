package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>(relaxed = true)

    private val billingService = BillingService(paymentProvider, invoiceService)

    @Test
    fun `mark invoice as paid`() {
        every { paymentProvider.charge(any()) } returns true
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                Invoice(
                    id = 1,
                    customerId = 1,
                    amount = Money(
                        value = BigDecimal.valueOf(100),
                        currency = Currency.EUR
                    ),
                    status = InvoiceStatus.PENDING,
                    retryCount = 0
                )
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsPaid(eq(1)) }
    }

    @Test
    fun `mark invoice as failed because of payment provider`() {
        every { paymentProvider.charge(any()) } returns false
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                Invoice(
                    id = 1,
                    customerId = 1,
                    amount = Money(
                        value = BigDecimal.valueOf(100),
                        currency = Currency.EUR
                    ),
                    status = InvoiceStatus.PENDING,
                    retryCount = 0
                )
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsFailed(eq(1)) }
    }

    @Test
    fun `billing with both failed and successful payments`() {
        val successInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(
                value = BigDecimal.valueOf(100),
                currency = Currency.EUR
            ),
            status = InvoiceStatus.PENDING,
            retryCount = 0
        )
        val failInvoice = Invoice(
            id = 2,
            customerId = 2,
            amount = Money(
                value = BigDecimal.valueOf(125),
                currency = Currency.EUR
            ),
            status = InvoiceStatus.PENDING,
            retryCount = 0
        )
        every { paymentProvider.charge(eq(successInvoice)) } returns true
        every { paymentProvider.charge(eq(failInvoice)) } returns false
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                successInvoice,
                failInvoice
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsFailed(eq(2)) }
        verify(exactly = 1) { invoiceService.markInvoiceAsPaid(eq(1)) }
    }

    @Test
    fun `mark invoice as failed because of currency mismatch`() {
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(1, 1)
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                Invoice(
                    id = 1,
                    customerId = 1,
                    amount = Money(
                        value = BigDecimal.valueOf(100),
                        currency = Currency.EUR
                    ),
                    status = InvoiceStatus.PENDING,
                    retryCount = 0
                )
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsFailed(eq(1)) }
    }

    @Test
    fun `mark invoice as failed because of customer not found`() {
        every { paymentProvider.charge(any()) } throws CustomerNotFoundException(1)
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                Invoice(
                    id = 1,
                    customerId = 1,
                    amount = Money(
                        value = BigDecimal.valueOf(100),
                        currency = Currency.EUR
                    ),
                    status = InvoiceStatus.PENDING,
                    retryCount = 0
                )
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsFailed(eq(1)) }
    }

    @Test
    fun `mark invoice as retryable because of network exception`() {
        every { paymentProvider.charge(any()) } throws NetworkException()
        every { invoiceService.fetchPendingInvoicesByCurrency(eq(Currency.EUR), any()) } returns
            listOf(
                Invoice(
                    id = 1,
                    customerId = 1,
                    amount = Money(
                        value = BigDecimal.valueOf(100),
                        currency = Currency.EUR
                    ),
                    status = InvoiceStatus.PENDING,
                    retryCount = 0
                )
            )

        billingService.runningBillingForProvider(Currency.EUR)

        verify(exactly = 1) { invoiceService.markInvoiceAsRetryable(eq(1)) }
    }
}
