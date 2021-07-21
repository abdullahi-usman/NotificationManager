package com.dahham.notificationmanager

import android.content.Intent
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

class NotificationListenerService : NotificationListenerService() {

    private val statusBarNotification = arrayListOf<StatusBarNotification>()

    private val statusBarNotificationLiveData: MutableLiveData<List<StatusBarNotification>> by lazy {
        MutableLiveData<List<StatusBarNotification>>(statusBarNotification)
    }

    inner class LocalBinder : Binder() {
        fun getService(): com.dahham.notificationmanager.NotificationListenerService {
            return this@NotificationListenerService
        }

        fun getNotifications(): LiveData<List<StatusBarNotification>> {
            return statusBarNotificationLiveData
        }

        fun refreshNotifications(): List<StatusBarNotification> {
            this@NotificationListenerService.refreshNotifications()
            return statusBarNotification
        }
    }

    var mBinder = LocalBinder()
    private var isServiceConnnected = false

    override fun onBind(intent: Intent?): IBinder? {
        if (isServiceConnnected) return mBinder
        return super.onBind(intent)
    }


    override fun onListenerConnected() {
        isServiceConnnected = true
        Log.i("dahham", "service connected")

//        val notificationChannelCompat = NotificationChannelCompat.Builder(
//            "notificationbackground",
//            NotificationManagerCompat.IMPORTANCE_DEFAULT
//        ).setName("Background Notification Service").setDescription("Service to listen to Notification when posted").build()
//
//        NotificationManagerCompat.from(applicationContext).createNotificationChannel(notificationChannelCompat)
//
//        startForeground(application?.packageName?.hashCode()!!, NotificationCompat.Builder(this.applicationContext, notificationChannelCompat.id).build())

        refreshNotifications()

        statusBarNotificationLiveData.postValue(statusBarNotification)
    }

    private fun refreshNotifications() {
        synchronized(statusBarNotification) {
            val __blklist = arrayListOf<StatusBarNotification>()
            statusBarNotification.forEach {
                if (it.hasExpired) {
                    __blklist.add(it)
                }
            }

            __blklist.forEach {
                statusBarNotification.removeAt(
                    statusBarNotification.getIndex(it) ?: return@forEach
                )
            }

            activeNotifications.forEach {
                if (it.cannotBeAdded) return
                statusBarNotification.add(it)
            }
        }
    }

    private val StatusBarNotification.hasExpired: Boolean
        get() {
            return (System.currentTimeMillis() - this.postTime) > 20000L
        }

    override fun onListenerDisconnected() {
        isServiceConnnected = false
        Log.i("dahham", "service disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.cannotBeAdded!!) return

        synchronized(statusBarNotification) {
            statusBarNotification.add(sbn)
        }

        statusBarNotificationLiveData.postValue(statusBarNotification)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        if (sbn?.cannotBeRemoved!!) return

        synchronized(statusBarNotification) {
            statusBarNotification.removeAt(statusBarNotification.getIndex(sbn) ?: return)
        }
        statusBarNotificationLiveData.postValue(statusBarNotification)
    }

    private fun List<StatusBarNotification>.has(other: StatusBarNotification): Boolean {
        return this.any { it.id == other.id }
    }

    private fun List<StatusBarNotification>.getIndex(other: StatusBarNotification): Int? {

        this.forEachIndexed { index, statusBarNotification ->
            if (statusBarNotification.id == other.id) {
                return index
            }
        }

        return null
    }

    private val StatusBarNotification?.cannotBeAdded: Boolean
    get(){
        return (this == null || this.isOngoing || statusBarNotification.has(this))
    }

    private val StatusBarNotification?.cannotBeRemoved: Boolean
    get() {
        return (this == null || this.hasExpired.not() || this.isOngoing)
    }
}