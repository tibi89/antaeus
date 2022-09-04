package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Currency
import mu.KotlinLogging
import java.time.LocalDate
import kotlin.concurrent.timer
import kotlin.random.Random

class InvoiceBillingScheduler(
    private val billingServicesToCurrency: Map<Currency, BillingService> // fixme: this should be for country, since we can have a different billing service for currency and country
) {
    private val logger = KotlinLogging.logger {}

    fun run() {
        logger.info { "Starting invoice billing scheduler" }
        billingServicesToCurrency.forEach { (currency, billingService) ->
            scheduleRecurringTask(currency, billingService)
        }
    }

    private fun scheduleRecurringTask(currency: Currency, billingService: BillingService) {
        // starting the job randomly between 0 and 10 seconds so that not all jobs are started at the same time
        timer(name = "BillingService-${currency.name}", daemon = true, period = 10000, initialDelay = Random.nextLong(10000)) {
            // we need to run this job only on the first and second day of the month (second day since we can retry invoices),
            // doing it like this since we want to run it multiple times
            // per day, but only on the first and second day of the month, which is not possible with the scheduler
            if (!shouldRun()) {
                return@timer
            }
            try {
                billingService.runningBillingForProvider()
            } catch (e: Exception) {
                logger.error(e) { "Error while running billing for ${currency.name}" }
            }
        }
    }

    private fun shouldRun(): Boolean {
        val dayOfMonth = LocalDate.now().dayOfMonth
        return dayOfMonth == 1 || dayOfMonth == 2
    }
}
