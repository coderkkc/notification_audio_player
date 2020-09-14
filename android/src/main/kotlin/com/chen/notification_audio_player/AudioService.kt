package com.chen.notification_audio_player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import java.net.URL

class AudioService : Service(){
    private lateinit var flutterActivity: Activity
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var eventCallBack: EventCallBack
    private lateinit var manager: NotificationManager
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var builder: NotificationCompat.Builder
    private lateinit var runnable: Runnable
    private var playbackState = PlaybackStateCompat.STATE_STOPPED
    private val audioBinder = AudioBinder()
    private var volume = 1.0f
    private var bufferPercent = 0
    private var isFirstRun = false
    private var isPrepared = false
    private var isLooping = false
    private var isReleased = false
    private var shouldSeekTo = -1
    private var title = ""
    private var author = ""
    private var avatar = ""
    private var url = ""

    private val intentFilter = IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
    private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()
    private val MEDIA_SESSION_ACTIONS = (
        PlaybackStateCompat.ACTION_PLAY
        or PlaybackStateCompat.ACTION_PAUSE
        or PlaybackStateCompat.ACTION_PLAY_PAUSE
        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        or PlaybackStateCompat.ACTION_STOP
        or PlaybackStateCompat.ACTION_SEEK_TO)

    private val mediaSessionCallBack = object : MediaSessionCompat.Callback(){
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val event =
                mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            when (event!!.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> eventCallBack.switchNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> eventCallBack.switchPrevious()
                KeyEvent.KEYCODE_MEDIA_PLAY -> Log.d("KeyEvent","播放事件")
                KeyEvent.KEYCODE_MEDIA_PAUSE -> Log.d("KeyEvent","暂停事件")
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Log.d("KeyEvent","暂停_播放事件")
                KeyEvent.KEYCODE_MEDIA_STOP -> Log.d("KeyEvent","停止事件")
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        @SuppressLint("WrongConstant", "RestrictedApi")
        override fun onPlay() {
            resume()
            eventCallBack.resume()
        }

        @SuppressLint("RestrictedApi")
        override fun onPause() {
            // 有的蓝牙耳机只会触发pause事件，这里进行判断
            if (playbackState == PlaybackState.STATE_PLAYING) {
                pause()
                eventCallBack.pause()
            } else {
                resume()
                eventCallBack.resume()
            }
        }

        override fun onStop() {
            stop()
            eventCallBack.stop()
        }

        override fun onSeekTo(pos: Long) {
            seek(pos.toInt())
            resume()
            eventCallBack.resume()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return audioBinder
    }

    override fun onDestroy() {
        Handler().removeCallbacksAndMessages(runnable)
        mediaPlayer.release()
        unregisterReceiver(myNoisyAudioStreamReceiver)
        super.onDestroy()
    }

    // 初始化MediaPlayer
    @SuppressLint("WrongConstant")
    fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()
        mediaSessionCompat = MediaSessionCompat(this, "audio_service")
        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(MEDIA_SESSION_ACTIONS)
            .setState(PlaybackState.STATE_NONE, 0, 1.0f)
        registerReceiver(myNoisyAudioStreamReceiver, intentFilter)

        mediaSessionCompat.apply {
            setCallback(mediaSessionCallBack)
            setPlaybackState(stateBuilder.build())
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }

        mediaPlayer.apply {
            isLooping = false
            setVolume(volume, volume)
            setAudioStreamType(AudioManager.STREAM_MUSIC)

            // 缓存进度回调
            setOnBufferingUpdateListener { mp, percent ->
                bufferPercent = percent
            }

            // 完成回调
            setOnCompletionListener {
                eventCallBack.complete()
                if (this@AudioService.isLooping) {
                    seekTo(0)
                    resume()
                } else {
                    switchAudioStatus(PlaybackState.STATE_PAUSED)
                }
            }

            // 错误回调
            setOnErrorListener { mediaPlayer, what, extra ->
                val whatMsg = if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    "MEDIA_ERROR_SERVER_DIED"
                } else {
                    "MEDIA_ERROR_UNKNOWN {what:$what}"
                }
                var extraMsg = when (extra) {
                    -2147483648 -> "MEDIA_ERROR_SYSTEM"
                    MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
                    MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
                    MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
                    MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
                    else -> "MEDIA_ERROR_UNKNOWN"
                }
                Log.i("error", "$whatMsg : $extraMsg")
                false
            }

            // prepared回调
            setOnPreparedListener {
                // 缓冲完再执行Seek操作
                isPrepared = true
                if (shouldSeekTo >=0) {
                    mediaPlayer.seekTo(shouldSeekTo)
                    shouldSeekTo = -1
                }
                // 播放音频
                resume()
                eventCallBack.preparedDuration(duration)
                // 设置音频进度
                val mediaMetadataCompat = MediaMetadataCompat.Builder()
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.duration.toLong())
                mediaSessionCompat.setMetadata(mediaMetadataCompat.build())
                stateBuilder.setState(PlaybackState.STATE_PLAYING, mediaPlayer.currentPosition.toLong(), 1.0f)

                mediaSessionCompat.setPlaybackState(stateBuilder.build())
                // 回调歌曲当前currentPosition
                if (!isFirstRun) {
                    isFirstRun = true
                    runnable = object : Runnable{
                        override fun run() {
                            val curPos = mediaPlayer.currentPosition
                            eventCallBack.currentPosition(curPos)
                            Handler().postDelayed(this,500)
                        }
                    }
                    Handler().post(runnable)
                }
            }
        }
    }

    // 初始化Notification
    @SuppressLint("WrongConstant", "RestrictedApi")
    private fun initNotification() {
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = PendingIntent.getActivity(this, 1, Intent(this, flutterActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel("audio_service", "audio_service", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.apply {
                enableVibration(false)
                enableLights(false)
                vibrationPattern = longArrayOf(0)
                setSound(null, null)
            }
            manager.createNotificationChannel(notificationChannel)
        }

        builder = NotificationCompat.Builder(this, "audio_service")
            .setContentTitle("title")
            .setContentText("author")
            .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .setSmallIcon(getResourceId("mipmap/ic_launcher"))
            .setVibrate(longArrayOf(0))
            .setSound(null)

        builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSessionCompat.sessionToken)
            .setShowActionsInCompactView(1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackState.ACTION_STOP
            ))
        )

        builder.mActions = actionsList(false)
    }

    // 设置新音频
    @SuppressLint("WrongConstant")
    fun setAudio(title: String, author: String, avatar: String, url: String) {
        // 防止重复播放
        if (this.url != url && url != "") {
            this.url = url
            this.title = title
            this.author = author
            this.avatar = avatar
            isPrepared = false
            mediaPlayer.apply {
                reset()
                setDataSource(url)
                prepareAsync()
            }
            setLargeIcon(avatar)
            builder.setContentTitle(title)
            builder.setContentText(author)
            startForeground(1, builder.build())
        }
    }

    // 设置Activity
    fun setActivity(flutterActivity: Activity) {
        this.flutterActivity = flutterActivity
        initNotification()
    }

    // 设置事件回调
    fun setEventCallBack(callback: EventCallBack) {
        this.eventCallBack = callback
    }

    // 获取播放状态
    fun getPlayerState(): String {
        return when (playbackState) {
            PlaybackState.STATE_PLAYING -> "PLAYING"
            PlaybackState.STATE_PAUSED -> "PAUSED"
            PlaybackState.STATE_STOPPED -> "STOPPED"
            else -> "ERROR_STATE"
        }
    }

    // 获取总时长
    fun getDuration(): Int {
        return mediaPlayer.duration;
    }

    // 获取当前时长
    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition
    }

    // 阻止休眠
    fun setWakeLock() {
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
    }

    // 设置速率
    fun setSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(speed)
        }
    }

    // 设置音量(只能设置当前音量的百分比，系统音量需要AudioManger控制)
    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer.setVolume(leftVolume, rightVolume)
    }

    // 设置是否播完完毕自动循环
    fun setIsLooping(isLooping: Boolean) {
        this.isLooping = isLooping
    }

    // 跳转指定进度
    fun seek(position: Int) {
        if (isPrepared) {
            mediaPlayer.seekTo(position)
        } else {
            shouldSeekTo = position
        }
    }

    // 暂停
    fun pause() {
        mediaPlayer.pause()
        switchAudioStatus(PlaybackState.STATE_PAUSED)
    }

    // 恢复
    fun resume() {
        mediaPlayer.start()
        switchAudioStatus(PlaybackState.STATE_PLAYING)
    }

    // 停止
    fun stop() {
        seek(0)
        resume()
        mediaPlayer.stop()
        switchAudioStatus(PlaybackState.STATE_STOPPED)
    }

    // 释放
    fun release() {
        Handler().removeCallbacksAndMessages(runnable)
        isReleased = true
        mediaPlayer.release()
        removeNotification()
    }

    // 关闭通知栏
    fun removeNotification() {
        stopForeground(true)
    }

    // 切换播放状态
    @SuppressLint("RestrictedApi")
    private fun switchAudioStatus(currentPlaybackState: Int) {
        playbackState = currentPlaybackState
        stateBuilder.setState(currentPlaybackState, mediaPlayer.currentPosition.toLong(), 1.0f)
        mediaSessionCompat.setPlaybackState(stateBuilder.build())
        builder.mActions = actionsList(currentPlaybackState == PlaybackState.STATE_PLAYING)
        manager.notify(1, builder.build())
    }

    // 返回播放&暂停两组按钮
    @SuppressLint("WrongConstant")
    private fun actionsList(isPlaying: Boolean): ArrayList<NotificationCompat.Action>{
        val actionsList = ArrayList<NotificationCompat.Action>()
        actionsList.add(NotificationCompat.Action(R.drawable.ic_action_skip_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext,
            PlaybackState.ACTION_SKIP_TO_PREVIOUS
        )))
        if (!isPlaying) {
            actionsList.add(NotificationCompat.Action(R.drawable.ic_action_play_arrow, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext,
                PlaybackState.ACTION_PLAY
            )))
        } else {
            actionsList.add(NotificationCompat.Action(R.drawable.ic_action_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext,
                PlaybackState.ACTION_PAUSE
            )))
        }
        actionsList.add(NotificationCompat.Action(R.drawable.ic_action_skip_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext,
            PlaybackState.ACTION_SKIP_TO_NEXT
        )))
        return actionsList
    }

    // 设置背景图
    private fun setLargeIcon(url: String) {
        Thread(Runnable {
            try {
                val conn = URL(url).openConnection()
                conn.doInput = true
                conn.connect()
                val inputStream = conn.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                builder.setLargeIcon(bitmap)
                manager.notify(1, builder.build())
                inputStream.close()
            } catch (e: Exception) {
                Log.d("setLargeIcon","$e")
            }
        }).start()
    }

    // 获取APP图标
    private fun getResourceId(resource: String): Int {
        val parts = resource.split("/")
        val resourceType = parts[0]
        val resourceName = parts[1]
        return resources.getIdentifier(resourceName, resourceType, packageName)
    }

    // Binder
    inner class AudioBinder() : Binder() {
        fun getService(): AudioService {
            return this@AudioService
        }
    }

    // 监听耳机插拔广播
    inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_HEADSET_PLUG && intent.hasExtra("state") && isFirstRun) {
                if (intent.getIntExtra("state", 0) != 1) {
                    mediaPlayer.pause()
                    eventCallBack.headPhoneStatus(true)
                } else {
                    eventCallBack.headPhoneStatus(false)
                }
            }
        }
    }

    // 事件回调
    interface EventCallBack {
        fun complete()
        fun resume()
        fun pause()
        fun stop()
        fun switchPrevious()
        fun switchNext()
        fun currentPosition(position: Int)
        fun preparedDuration(position: Int)
        fun headPhoneStatus(out: Boolean)
    }
}
