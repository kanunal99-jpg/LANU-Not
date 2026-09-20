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
import com.lanu.notdefteri.util.LocationHelper

class LocationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getDatabase(context)
            val notesWithLocation = db.noteDao().getAllNotesList().filter {
                it.latitude != null && it.longitude != null
            }

            if (notesWithLocation.isEmpty()) {
                return Result.success()
            }

            LocationHelper.getCurrentLocation(
                context = context,
                onSuccess = { currentLocation ->
                    for (note in notesWithLocation) {
                        val lat = note.latitude ?: continue
                        val lng = note.longitude ?: continue

                        val distanceMeters = LocationHelper.calculateDistanceMeters(
                            currentLocation.latitude, currentLocation.longitude,
                            lat, lng
                        )

                        // Trigger notification if user is within 250 meters
                        if (distanceMeters <= 250f) {
                            showLocationNotification(
                                noteId = note.id,
                                title = note.title,
                                content = note.content,
                                locationName = note.locationName ?: "Hedef Konum"
                            )
                        }
                    }
                },
                onError = {
                    // Suppress error
                }
            )

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun showLocationNotification(
        noteId: Int,
        title: String,
        content: String,
        locationName: String
    ) {
        val channelId = "lanu_location_reminders_channel"
        val notificationId = noteId + 2000

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LANU Konum Hatırlatıcıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Belirli konumlara yaklaştığınızda verilen not hatırlatmaları"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

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
            .ifBlank { "$locationName konumuna ulaştınız!" }

        val appIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_map

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(appIcon)
            .setContentTitle("📍 Konum Hatırlatıcı: $locationName")
            .setContentText("Not: $title — $cleanContent")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📍 $locationName Yakınındasınız")
                    .bigText("[$title]\n$cleanContent")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
