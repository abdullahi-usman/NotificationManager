package com.dahham.notificationmanager

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.room.*

data class NotificationAndroid(@Ignore val parcel: Parcel? = null){


    var icon: Int = 0
    var tickerText: String = ""
    var `when`: Long = 0L
    var contentTitle: String = ""
    var contentText: String = ""
    var channelId: String = ""


    init {
        if(parcel != null){
            val noti = android.app.Notification(parcel)

            this.icon = noti.icon
            //this.channelId = noti.channelId
        }

    }

    fun getNotification(context: Context, channelId: String): android.app.Notification{
        return NotificationCompat.Builder(context, channelId).setContentText(contentText).setContentTitle(contentTitle).build()
    }
}

@Entity(tableName = "Notifications")
data class Notification(@PrimaryKey(autoGenerate = true) var uuid: Int = 0, @Ignore val
statusBarNotification: StatusBarNotification? = null){


     var pkg: String? = null
     var id = 0
     var tag: String? = null
     var key: String? = null
     var groupKey: String? = null
     var overrideGroupKey: String? = null
     var uid = 0
     var opPkg: String? = null
     var initialPid = 0
     @Embedded var notification: NotificationAndroid? = null
     var postTime: Long = 0

    init {
        if (statusBarNotification != null){
            pkg = statusBarNotification.packageName
            opPkg = statusBarNotification.opPkg
            id = statusBarNotification.id
            tag = statusBarNotification.tag
            uid = statusBarNotification.uid
            //initialPid = statusBarNotification.in
            postTime = statusBarNotification.postTime
            overrideGroupKey = statusBarNotification.overrideGroupKey

        }
    }
}

@Dao
interface NotificationDao{
    @Query("SELECT * from notifications")
    fun getAll(): LiveData<List<Notification>>

    @Insert
    fun add(notification: Notification): Long

    @Delete
    fun delete(vararg notification: Notification)
}


@Database(entities = [Notification::class], version = 1)
abstract class NotificationDatabase: RoomDatabase() {
    abstract fun NotificationDao(): NotificationDao

    companion object{
        @JvmStatic
        var database: NotificationDatabase? = null
        fun instance(context: Context): NotificationDatabase{
            if (database == null) {
                database = Room.databaseBuilder(
                    context.applicationContext, NotificationDatabase::class.java,
                    "Notifications.db"
                ).allowMainThreadQueries().build()

            }

            return database!!
        }
    }
}