package io.pleo.anteus.core.scheduler

import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.scheduler.InvoiceBillingScheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Currency
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class InvoiceBillingSchedulerTest {
    private val billingService = mockk<BillingService>(relaxed = true)

    @Test
    fun `will not run task`() {
        val scheduler = InvoiceBillingScheduler(mapOf(Currency.EUR to billingService), Clock.systemUTC())
        scheduler.task(Currency.EUR, billingService)
        verify(exactly = 0, timeout = 11_000) { billingService.runningBillingForProvider(Currency.EUR) }
    }

    @Test
    fun `will run task on the first day of the month`() {
        val scheduler = InvoiceBillingScheduler(mapOf(Currency.EUR to billingService), Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.of("UTC")))
        scheduler.task(Currency.EUR, billingService)
        verify(exactly = 1, timeout = 11_000) { billingService.runningBillingForProvider(Currency.EUR) }
    }

    @Test
    fun `will run task on the second day of the month`() {
        val scheduler = InvoiceBillingScheduler(mapOf(Currency.EUR to billingService), Clock.fixed(Instant.parse("2020-01-02T00:00:00Z"), ZoneId.of("UTC")))
        scheduler.task(Currency.EUR, billingService)
        verify(exactly = 1, timeout = 11_000) { billingService.runningBillingForProvider(Currency.EUR) }
    }

    @Test
    fun `will not run task on random day of the month`() {
        val scheduler = InvoiceBillingScheduler(mapOf(Currency.EUR to billingService), Clock.fixed(Instant.parse("2020-01-05T00:00:00Z"), ZoneId.of("UTC")))
        scheduler.task(Currency.EUR, billingService)
        verify(exactly = 0, timeout = 11_000) { billingService.runningBillingForProvider(Currency.EUR) }
    }
}
