package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private val billingService = BillingService(paymentProvider, Currency.EUR, invoiceService)

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

        billingService.runningBillingForProvider()

        verify(exactly = 1) { invoiceService.markInvoiceAsPaid(eq(1)) }
    }
}
