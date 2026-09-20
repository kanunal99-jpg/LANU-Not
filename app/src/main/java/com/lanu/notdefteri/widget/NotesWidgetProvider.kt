package com.lanu.notdefteri.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lanu.notdefteri.MainActivity
import com.lanu.notdefteri.R
import com.lanu.notdefteri.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the latest notes from database
                val allNotes = db.noteDao().getAllNotes().first()
                val topNotes = allNotes.take(3)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)

                    // 1. Setup Quick Mic Action
                    val micIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("LAUNCH_ACTION", "QUICK_RECORD")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val micPendingIntent = PendingIntent.getActivity(
                        context,
                        9999, // Distinct RequestCode for Quick Mic
                        micIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_mic, micPendingIntent)

                    // 2. Populate Static Note Slots
                    if (topNotes.isEmpty()) {
                        views.setViewVisibility(R.id.txt_widget_empty, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_note_slot_1, View.GONE)
                        views.setViewVisibility(R.id.widget_note_slot_2, View.GONE)
                        views.setViewVisibility(R.id.widget_note_slot_3, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.txt_widget_empty, View.GONE)

                        // Slot 1
                        if (topNotes.size > 0) {
                            val note = topNotes[0]
                            views.setViewVisibility(R.id.widget_note_slot_1, View.VISIBLE)
                            views.setTextViewText(R.id.txt_note_title_1, note.title.ifBlank { "Başlıksız Not" })
                            
                            val cleanContent = note.content.replace("<[^>]*>".toRegex(), "")
                            views.setTextViewText(R.id.txt_note_content_1, cleanContent.ifBlank { "İçerik boş." })

                            val noteIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra("LAUNCH_NOTE_ID", note.id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            val notePendingIntent = PendingIntent.getActivity(
                                context,
                                note.id,
                                noteIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_note_slot_1, notePendingIntent)
                        } else {
                            views.setViewVisibility(R.id.widget_note_slot_1, View.GONE)
                        }

                        // Slot 2
                        if (topNotes.size > 1) {
                            val note = topNotes[1]
                            views.setViewVisibility(R.id.widget_note_slot_2, View.VISIBLE)
                            views.setTextViewText(R.id.txt_note_title_2, note.title.ifBlank { "Başlıksız Not" })
                            
                            val cleanContent = note.content.replace("<[^>]*>".toRegex(), "")
                            views.setTextViewText(R.id.txt_note_content_2, cleanContent.ifBlank { "İçerik boş." })

                            val noteIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra("LAUNCH_NOTE_ID", note.id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            val notePendingIntent = PendingIntent.getActivity(
                                context,
                                note.id,
                                noteIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_note_slot_2, notePendingIntent)
                        } else {
                            views.setViewVisibility(R.id.widget_note_slot_2, View.GONE)
                        }

                        // Slot 3
                        if (topNotes.size > 2) {
                            val note = topNotes[2]
                            views.setViewVisibility(R.id.widget_note_slot_3, View.VISIBLE)
                            views.setTextViewText(R.id.txt_note_title_3, note.title.ifBlank { "Başlıksız Not" })
                            
                            val cleanContent = note.content.replace("<[^>]*>".toRegex(), "")
                            views.setTextViewText(R.id.txt_note_content_3, cleanContent.ifBlank { "İçerik boş." })

                            val noteIntent = Intent(context, MainActivity::class.java).apply {
                                putExtra("LAUNCH_NOTE_ID", note.id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            val notePendingIntent = PendingIntent.getActivity(
                                context,
                                note.id,
                                noteIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_note_slot_3, notePendingIntent)
                        } else {
                            views.setViewVisibility(R.id.widget_note_slot_3, View.GONE)
                        }
                    }

                    // Perform update
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Auto update widgets when a broadcast/db update arrives if requested
        val action = intent.action
        if (action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NotesWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
