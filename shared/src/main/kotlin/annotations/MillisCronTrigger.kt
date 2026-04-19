package com.glycin.annotations

import org.springframework.scheduling.Trigger
import org.springframework.scheduling.TriggerContext
import org.springframework.scheduling.support.CronExpression
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// A disgusting custom Trigger that extends Spring's 6-field cron with a millisecond first field.
// Spring cron only goes down to seconds, so this invents a 7-field dialect which is CRUCIAL for gaming yes yes:
//
//   millis  second  minute  hour  day-of-month  month  day-of-week
//   */16    *       *       *     *             *      *
//
// The millis field controls the tick interval (e.g. */16 = every 16ms = ~60 FPS).
// The remaining 6 fields are standard Spring cron and act as a calendar gate ticks only fire during seconds that match the cron expression.
//
// Examples:
//   "*/16 * * * * * *"         → 60 FPS, always
//   "*/16 * * 9-17 * * MON-FRI"   → 60 FPS, but only on business hours
//   "*/8 * * * * * SAT-SUN"    → 120 FPS, but only on weekends
class MillisCronTrigger(expression: String) : Trigger {

    private val millisInterval: Long
    private val cronExpression: CronExpression
    private val zone: ZoneId = ZoneId.systemDefault()

    // Cache the cron check per wall-clock second
    // cronExpression.next() ~60 times per second when the answer doesn't change
    private var cachedEpochSecond: Long = -1
    private var cachedAllowed: Boolean = false

    init {
        val fields = expression.trim().split(WHITESPACE)
        require(fields.size == 7) {
            "Millis cron requires 7 fields: <millis> <second> <minute> <hour> <day-of-month> <month> <day-of-week>, got: $expression"
        }

        // Parse the invented millis field: * = every 1ms, */N = every N ms, N = every N ms
        millisInterval = when {
            fields[0] == "*"           -> 1L
            fields[0].startsWith("*/") -> fields[0].removePrefix("*/").toLong()
            else                       -> fields[0].toLong()
        }
        require(millisInterval > 0) { "Millis interval must be positive, got: $millisInterval" }

        // The remaining 6 fields are a standard Spring CronExpression
        cronExpression = CronExpression.parse(fields.drop(1).joinToString(" "))
    }

    override fun nextExecution(context: TriggerContext): Instant? {
        val last = context.lastCompletion() ?: Instant.now()
        val candidate = last.plusMillis(millisInterval)

        return if (isSecondAllowed(candidate)) {
            candidate
        } else {
            // The candidate falls in a second not allowed by the standard cron expression.
            // Fast-forward to the start of the next cron-matching second.
            val truncated = toLocalDateTime(last).withNano(0)
            cronExpression.next(truncated)?.atZone(zone)?.toInstant()
        }
    }

    // Checks whether the given instant's enclosing second is allowed by the 6-field cron.
    // Uses a trick: if cronExpression.next(oneSecondBefore) lands on this exact second,
    // then this second is a valid cron match.
    private fun isSecondAllowed(instant: Instant): Boolean {
        val epochSecond = instant.epochSecond
        if (epochSecond == cachedEpochSecond) return cachedAllowed

        val truncated = toLocalDateTime(instant).withNano(0)
        val previous = truncated.minusSeconds(1)
        val next = cronExpression.next(previous)
        cachedAllowed = next != null && next.withNano(0) == truncated
        cachedEpochSecond = epochSecond
        return cachedAllowed
    }

    private fun toLocalDateTime(instant: Instant): LocalDateTime {
        return LocalDateTime.ofInstant(instant, zone)
    }

    companion object {
        private val WHITESPACE = "\\s+".toRegex()
    }
}
