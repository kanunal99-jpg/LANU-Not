package com.lanu.notdefteri.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.worker.ReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val WORK_NAME_PREFIX = "reminder_"
    const val REMINDER_TAG = "lanu_note_reminder"

    /**
     * Schedules a local notification reminder for the given note at a specific timestamp (millis).
     */
    fun scheduleReminder(context: Context, note: Note, triggerAtMillis: Long) {
        val delayMillis = triggerAtMillis - System.currentTimeMillis()
        if (delayMillis <= 0) return

        scheduleWithDelay(context, note, delayMillis, triggerAtMillis)
    }

    /**
     * Schedules a reminder using an explicit delay in milliseconds.
     */
    fun scheduleWithDelay(context: Context, note: Note, delayMillis: Long, triggerAtMillis: Long = System.currentTimeMillis() + delayMillis) {
        if (delayMillis <= 0) return

        val workManager = WorkManager.getInstance(context)
        val data = Data.Builder()
            .putInt("NOTE_ID", note.id)
            .putString("NOTE_TITLE", note.title)
            .putString("NOTE_CONTENT", note.content)
            .putLong("TRIGGER_TIME", triggerAtMillis)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(REMINDER_TAG)
            .addTag("${WORK_NAME_PREFIX}${note.id}")
            .build()

        workManager.enqueueUniqueWork(
            "${WORK_NAME_PREFIX}${note.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels any pending scheduled reminder for a note.
     */
    fun cancelReminder(context: Context, noteId: Int) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("${WORK_NAME_PREFIX}$noteId")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Formats reminder timestamp into user-friendly localized text.
     * E.g. "Bugün 18:30", "Yarın 09:00", or "15 Eki 2026, 14:00"
     */
    fun formatReminderDate(timeMillis: Long): String {
        val reminderCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val nowCal = Calendar.getInstance()

        val timeFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
        val timeStr = timeFormat.format(Date(timeMillis))

        val isToday = reminderCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                reminderCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = reminderCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                reminderCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Bugün $timeStr"
            isTomorrow -> "Yarın $timeStr"
            reminderCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) -> {
                val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("tr", "TR"))
                dateFormat.format(Date(timeMillis))
            }
            else -> {
                val fullFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr", "TR"))
                fullFormat.format(Date(timeMillis))
            }
        }
    }

    /**
     * Computes human-friendly remaining duration string (e.g. "45 dk sonra", "3 saat sonra", "Yarın").
     */
    fun getRemainingTimeString(triggerMillis: Long): String {
        val diff = triggerMillis - System.currentTimeMillis()
        if (diff <= 0) return "Süresi geçti"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Birazdan"
            minutes < 60 -> "$minutes dk sonra"
            hours < 24 -> "$hours saat ${minutes % 60} dk sonra"
            days == 1L -> "Yarın"
            else -> "$days gün sonra"
        }
    }
}
