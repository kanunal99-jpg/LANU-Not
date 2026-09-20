package com.lanu.notdefteri.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lanu.notdefteri.MainActivity
import com.lanu.notdefteri.data.local.AppDatabase

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getInt("NOTE_ID", 0)
        val noteTitle = inputData.getString("NOTE_TITLE") ?: "Not Hatırlatıcısı"
        val noteContent = inputData.getString("NOTE_CONTENT") ?: "Zamanlanmış not hatırlatıcınız geldi."

        showNotification(noteId, noteTitle, noteContent)

        // Clear or update reminderTime in local database so the note reflects that the reminder was triggered
        if (noteId != 0) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.noteDao().updateReminderTime(noteId, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    private fun showNotification(noteId: Int, title: String, content: String) {
        val channelId = "lanu_note_reminders_channel"
        val notificationId = if (noteId != 0) noteId else 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel on SDK 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LANU Not Hatırlatıcıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "LANU Not Defteri zamanlanmış not bildirimleri"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action Intent to open app directly into the note when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("LAUNCH_NOTE_ID", noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanContent = content
            .replace("<[^>]*>".toRegex(), "")
            .trim()
            .ifBlank { "Notunuzu görüntülemek için dokunun." }

        val appIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(appIcon)
            .setContentTitle("⏰ Hatırlatıcı: $title")
            .setContentText(cleanContent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("⏰ Hatırlatıcı: $title")
                    .bigText(cleanContent)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                appIcon,
                "Notu Aç",
                pendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
