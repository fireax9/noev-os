package com.example.ui.cordly

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object CordlyNotificationHelper {
    private const val CHANNEL_MESSAGES = "neov_messages_channel"
    private const val CHANNEL_CALLS = "neov_calls_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "neov chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when friends send messages"
            }
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "neov chat Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video calls"
            }
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyIncomingMessage(
        context: Context,
        senderName: String,
        messagePreview: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = "لقد أرسل لك $senderName: $messagePreview"
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() and 0xFFFFFF).toInt(),
                notification
            )
        } catch (_: SecurityException) {
        }
    }

    fun notifyIncomingCall(
        context: Context,
        callerName: String,
        isVideo: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            callerName.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callLabel = if (isVideo) "مكالمة فيديو واردة من $callerName" else "مكالمة صوتية واردة من $callerName"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("neov chat")
            .setContentText(callLabel)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                99881,
                notification
            )
        } catch (_: SecurityException) {
        }
    }
}
