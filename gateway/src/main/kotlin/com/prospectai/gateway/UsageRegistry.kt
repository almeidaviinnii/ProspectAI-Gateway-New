package com.prospectai.gateway

import com.prospectai.core.model.ProviderUsageMetrics
import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class UsageRegistry(
    private val dailyLimit: Int,
    logPath: String,
    private val monthlyLimit: Int = Int.MAX_VALUE,
) {
    private val dailyCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val monthlyCounters = ConcurrentHashMap<String, AtomicInteger>()
    private val statistics = ConcurrentHashMap<String, MutableProviderStatistics>()
    private val usageLog = Path.of(logPath)

    init {
        usageLog.parent?.let(Files::createDirectories)
        if (Files.exists(usageLog)) restoreCurrentPeriod()
    }

    @Synchronized
    fun record(provider: String, amount: Int = 1) {
        require(amount > 0) { "A quantidade de uso precisa ser positiva." }
        val date = LocalDate.now(ZoneOffset.UTC)
        val month = YearMonth.from(date)
        val dailyCounter = dailyCounters.computeIfAbsent("$date|$provider") { AtomicInteger(0) }
        val monthlyCounter = monthlyCounters.computeIfAbsent("$month|$provider") { AtomicInteger(0) }
        if (dailyCounter.get() + amount > dailyLimit) {
            throw GatewayException("Limite diário do Gateway atingido para $provider.", HttpStatusCode.TooManyRequests)
        }
        if (monthlyCounter.get() + amount > monthlyLimit) {
            throw GatewayException("Limite mensal do Gateway atingido para $provider.", HttpStatusCode.TooManyRequests)
        }

        val dailyTotal = dailyCounter.addAndGet(amount)
        val monthlyTotal = monthlyCounter.addAndGet(amount)
        runCatching {
            append("$date,${Instant.now()},${safe(provider)},$amount,$dailyTotal,$monthlyTotal,usage,0\n")
        }.getOrElse {
            dailyCounter.addAndGet(-amount)
            monthlyCounter.addAndGet(-amount)
            throw GatewayException("Não foi possível registrar o consumo do Gateway.", HttpStatusCode.ServiceUnavailable)
        }
    }

    @Synchronized
    fun recordOutcome(provider: String, successful: Boolean, latencyMs: Long) {
        val date = LocalDate.now(ZoneOffset.UTC)
        val daily = current(provider)
        val monthly = currentMonthly(provider)
        runCatching {
            append("$date,${Instant.now()},${safe(provider)},0,$daily,$monthly,${if (successful) "success" else "failure"},${latencyMs.coerceAtLeast(0)}\n")
        }.getOrElse {
            throw GatewayException("Não foi possível registrar as métricas do Gateway.", HttpStatusCode.ServiceUnavailable)
        }
        statistics.computeIfAbsent(provider) { MutableProviderStatistics() }.record(successful, latencyMs)
    }

    fun <T> track(provider: String, block: () -> T): T = track(provider, { true }, block)

    fun <T> track(provider: String, isSuccessful: (T) -> Boolean, block: () -> T): T {
        val startedAt = System.nanoTime()
        return try {
            val result = block()
            recordOutcome(provider, isSuccessful(result), elapsedMillis(startedAt))
            result
        } catch (error: Throwable) {
            runCatching { recordOutcome(provider, false, elapsedMillis(startedAt)) }
                .onFailure(error::addSuppressed)
            throw error
        }
    }

    fun current(provider: String): Int =
        dailyCounters["${LocalDate.now(ZoneOffset.UTC)}|$provider"]?.get() ?: 0

    fun currentMonthly(provider: String): Int =
        monthlyCounters["${YearMonth.now(ZoneOffset.UTC)}|$provider"]?.get() ?: 0

    fun snapshot(): List<ProviderUsageMetrics> {
        val providers = buildSet {
            dailyCounters.keys.mapTo(this) { it.substringAfter('|') }
            monthlyCounters.keys.mapTo(this) { it.substringAfter('|') }
            addAll(statistics.keys)
        }
        return providers.sorted().map { provider ->
            val stats = statistics[provider] ?: MutableProviderStatistics()
            ProviderUsageMetrics(
                provider = provider,
                dailyRequests = current(provider),
                monthlyRequests = currentMonthly(provider),
                successfulCalls = stats.successes,
                failedCalls = stats.failures,
                consecutiveFailures = stats.consecutiveFailures,
                averageResponseMs = stats.averageLatencyMs(),
            )
        }
    }

    private fun restoreCurrentPeriod() {
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        val month = YearMonth.now(ZoneOffset.UTC).toString()
        Files.lines(usageLog).use { lines ->
            lines.map { it.split(',') }
                .filter { it.size >= 4 }
                .forEach { columns ->
                    val date = columns[0]
                    val provider = columns[2]
                    val amount = columns[3].toIntOrNull() ?: 0
                    if (date == today && amount > 0) {
                        dailyCounters.computeIfAbsent("$today|$provider") { AtomicInteger(0) }.addAndGet(amount)
                    }
                    if (date.startsWith(month) && amount > 0) {
                        monthlyCounters.computeIfAbsent("$month|$provider") { AtomicInteger(0) }.addAndGet(amount)
                    }
                    if (date.startsWith(month) && columns.size >= 8 && columns[6] in setOf("success", "failure")) {
                        statistics.computeIfAbsent(provider) { MutableProviderStatistics() }
                            .record(columns[6] == "success", columns[7].toLongOrNull() ?: 0)
                    }
                }
        }
    }

    private fun append(line: String) {
        Files.writeString(usageLog, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    private fun safe(provider: String): String = provider.replace(',', '_').replace('\n', '_').replace('\r', '_')
    private fun elapsedMillis(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private class MutableProviderStatistics {
        var successes: Int = 0
            private set
        var failures: Int = 0
            private set
        var consecutiveFailures: Int = 0
            private set
        private var totalLatencyMs: Long = 0

        fun record(successful: Boolean, latencyMs: Long) {
            if (successful) {
                successes += 1
                consecutiveFailures = 0
            } else {
                failures += 1
                consecutiveFailures += 1
            }
            totalLatencyMs += latencyMs.coerceAtLeast(0)
        }

        fun averageLatencyMs(): Long {
            val calls = successes + failures
            return if (calls == 0) 0 else totalLatencyMs / calls
        }
    }
}
