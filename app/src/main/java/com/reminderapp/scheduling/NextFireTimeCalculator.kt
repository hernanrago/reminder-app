package com.reminderapp.scheduling

import com.reminderapp.data.model.Schedule
import java.util.Calendar

object NextFireTimeCalculator {

    /**
     * Calcula el próximo epoch millis a partir de [fromMillis].
     * Retorna null si el schedule ya no tiene disparos futuros (OneTime expirado).
     */
    fun compute(schedule: Schedule, fromMillis: Long = System.currentTimeMillis()): Long? {
        return when (schedule) {
            is Schedule.OneTime -> {
                if (schedule.triggerAtMillis > fromMillis) schedule.triggerAtMillis else null
            }

            is Schedule.Interval -> {
                fromMillis + schedule.intervalMinutes * 60_000L
            }

            is Schedule.Daily -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, schedule.hourOfDay)
                    set(Calendar.MINUTE, schedule.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= fromMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }

            is Schedule.Weekly -> {
                if (schedule.daysOfWeek.isEmpty()) return null
                val sortedDays = schedule.daysOfWeek.sorted()
                val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
                val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

                // Buscar el próximo día de la semana configurado
                for (offset in 0..7) {
                    val candidateCal = Calendar.getInstance().apply {
                        timeInMillis = fromMillis
                        add(Calendar.DAY_OF_YEAR, offset)
                        set(Calendar.HOUR_OF_DAY, schedule.hourOfDay)
                        set(Calendar.MINUTE, schedule.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val candidateDay = candidateCal.get(Calendar.DAY_OF_WEEK)
                    if (candidateDay in sortedDays && candidateCal.timeInMillis > fromMillis) {
                        return candidateCal.timeInMillis
                    }
                }
                null
            }
        }
    }
}
