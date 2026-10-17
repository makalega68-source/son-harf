package com.obi.local

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

/** Home screen card: Obi in today's look, the next reminder and the word of the day. Tap to chat. */
class ObiWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(context, ObiWidget::class.java)) }.getOrNull() ?: return
            if (ids.isEmpty()) return
            val store = ObiStore(context)
            val views = RemoteViews(context.packageName, R.layout.obi_widget)
            runCatching { views.setImageViewBitmap(R.id.obi_widget_face, portrait(context, store)) }
            views.setTextViewText(R.id.obi_widget_next, store.nextAlarmLine()?.let { "⏰ $it" } ?: "⏰ Kurulu hatırlatma yok")
            val word = ObiLife.wordsOfDay(LocalDate.now()).first()
            views.setTextViewText(R.id.obi_widget_word, "📘 ${word.en} — ${word.tr}")
            val countdown = store.countdowns().minByOrNull { ObiLife.daysUntil(it, LocalDate.now()) }
            if (countdown != null) {
                views.setViewVisibility(R.id.obi_widget_extra, View.VISIBLE)
                views.setTextViewText(R.id.obi_widget_extra, "⏳ ${ObiLife.title(countdown.label)}: ${ObiLife.daysUntil(countdown, LocalDate.now())} gün")
            } else views.setViewVisibility(R.id.obi_widget_extra, View.GONE)
            val open = PendingIntent.getForegroundService(context, 31,
                Intent(context, ObiOverlayService::class.java).setAction(ObiOverlayService.ACTION_OPEN_CHAT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.obi_widget_root, open)
            ids.forEach { runCatching { manager.updateAppWidget(it, views) } }
        }

        /** Draws the real animated rig once into a small bitmap, wearing the chosen skin and hat. */
        private fun portrait(context: Context, store: ObiStore): Bitmap {
            val size = 220
            val view = WordSiegeMascotView(context)
            view.setSkin(store.skin())
            view.updateGame(null, false, 0, 0, 0, null, emptyList(), true, WordSiegeMascotEmotion.HAPPY)
            view.updateCompanion(0L, null, false, 1f, false, true, hat = store.hat())
            view.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, size, size)
            return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
    }
}
