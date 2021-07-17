package com.dahham.notificationmanager

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    
    var statusList = listOf<StatusBarNotification>()
    lateinit var notificationRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (this::notificationRecyclerView.isInitialized.not()) {
            setContentView(R.layout.activity_main)
            setSupportActionBar(findViewById(R.id.toolbar))
            
            notificationRecyclerView = findViewById(R.id.notification_recycler_view)
            notificationRecyclerView.layoutManager = LinearLayoutManager(this)
            notificationRecyclerView.adapter = NotificationLayoutAdapter()
        }
        
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            serviceConnection?.refreshNotifications()
            notificationRecyclerView.adapter?.notifyDataSetChanged()
        }
    }
    
    private inner class NotificationLayoutAdapter : RecyclerView.Adapter<NotificationLayoutViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationLayoutViewHolder {
            if (statusList.isEmpty())
                return NotificationLayoutViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.notification_view_empty, parent, false) as ViewGroup)
            
            return NotificationLayoutViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.notification_view, parent, false) as ViewGroup)
        }
        
        override fun onBindViewHolder(holder: NotificationLayoutViewHolder, position: Int) {
            if (statusList.isEmpty())return
            if (holder._itemView.findViewById<TextView>(R.id.empty_notification_view) != null){
                holder._itemView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            
            holder.bind(statusList[position])
        }
        
        override fun getItemCount(): Int {
            var size = statusList.size
            if (size <= 0)size = 1
            
            return size
        }
        
    }
    
    private inner class NotificationLayoutViewHolder(val _itemView: ViewGroup) : RecyclerView.ViewHolder(_itemView) {
        fun bind(notification: StatusBarNotification) {
            val containerLayout = LinearLayout(this@MainActivity)
            containerLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            val notificationBuilder = NotificationCompat.Builder(applicationContext, notification.notification)
            _itemView.removeAllViews()
            _itemView.addView(notificationBuilder.createContentView()?.apply(applicationContext, containerLayout))
            
            _itemView.setOnClickListener {
                notification.notification?.contentIntent?.send()
            }
        }
    }
    
    private abstract inner class NotificationServiceConnnection : ServiceConnection {
        open var hasAttachedToService = false
        abstract fun refreshNotifications()
    }
    
    private var serviceConnection: NotificationServiceConnnection? = object : NotificationServiceConnnection() {
        
        private var serviceBinder: NotificationListenerService.LocalBinder? = null
        override fun refreshNotifications() {
            if (serviceBinder != null) statusList = serviceBinder !!.refreshNotifications()
        }
        
        @SuppressLint("NotifyDataSetChanged")
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hasAttachedToService = true
            Log.i("dahham", "hello service created: $service")
            
            if (service is NotificationListenerService.LocalBinder) {
                service.getNotifications().observe(this@MainActivity) {
                    statusList = it
                    notificationRecyclerView.adapter?.notifyDataSetChanged()
                }
                serviceBinder = service
            }
            
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            hasAttachedToService = false
            Log.i("dahham", "hello disconnected")
        }
        
    }
    
    private fun attachToService() {
        Log.i("dahham", "attaching to service")
        if (serviceConnection?.hasAttachedToService?.not() ?: return) {
            bindService(Intent(this, NotificationListenerService::class.java), serviceConnection!!, 0)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (serviceConnection != null){
            unbindService(serviceConnection!!)
            serviceConnection = null //so that we wont leak memory
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (serviceConnection?.hasAttachedToService?.not() ?: return) attachToService()
    }
    
    override fun onStart() {
        super.onStart()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && notificationManager.isNotificationListenerAccessGranted(ComponentName(this, NotificationListenerService::class.java)).not()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }
    
    
}