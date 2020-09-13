package com.chen.notification_audio_player

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

/** NotificationAudioPlayerPlugin */
public class NotificationAudioPlayerPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel : MethodChannel
  private lateinit var flutterActivity: Activity
  private lateinit var audioBinder: AudioService.AudioBinder
  private lateinit var audioService: AudioService
  private var eventSink: EventChannel.EventSink? = null
  private var preparedDurationEventSink: EventChannel.EventSink? = null
  private var curPosEventSink: EventChannel.EventSink? = null
  private var isBindService = false

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "notification_audio_player_event")
    val preparedDurationEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "notification_audio_player_preparedDuration_event")
    val curPosEventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "notification_audio_player_curPos_event")
    eventChannel.setStreamHandler(object : EventChannel.StreamHandler{
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
      }
      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    })
    preparedDurationEventChannel.setStreamHandler(object : EventChannel.StreamHandler{
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        preparedDurationEventSink = events
      }
      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    })
    curPosEventChannel.setStreamHandler(object : EventChannel.StreamHandler{
      override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        curPosEventSink = events
      }
      override fun onCancel(arguments: Any?) {
        eventSink = null
      }
    })

    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "notification_audio_player")
    channel.setMethodCallHandler(this);
  }

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "notification_audio_player")
      channel.setMethodCallHandler(NotificationAudioPlayerPlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "getPlayerState" -> result.success(audioService.getPlayerState())
      "getDuration" -> result.success(audioService.getDuration())
      "getCurrentPosition" -> result.success(audioService.getCurrentPosition())
      "setWakeLock" -> {
        audioService.setWakeLock()
        result.success(1)
      }
      "setSpeed" -> {
        val speed: Double? = call.argument("speed")
        speed?.apply { audioService.setSpeed(this.toFloat()) }
        result.success(1)
      }
      "setVolume" -> {
        val leftVolume: Double? = call.argument("leftVolume")
        val rightVolume: Double? = call.argument("rightVolume")
        if (leftVolume != null && rightVolume != null) {
          audioService.setVolume(leftVolume.toFloat(), rightVolume.toFloat())
          result.success(1)
        } else {
          result.success(-1)
        }
      }
      "setIsLooping" -> {
        val isLooping: Boolean? = call.argument("isLooping")
        println()
        isLooping?.apply { audioService.setIsLooping(isLooping) }
        result.success(1)
      }
      "seek" -> {
        val position: Int? = call.argument("position")
        position?.apply { audioService.seek(position) }
        result.success(1)
      }
      "play" -> {
        val title: String? = call.argument("title")
        val author: String? = call.argument("author")
        val avatar: String? = call.argument("avatar")
        val url: String? = call.argument("url")
        if (title !=null && author !=null && avatar != null && url != null) {
          audioService.setAudio(title, author, avatar, url)
          result.success(1)
        } else {
          result.success(-1)
        }
      }
      "pause" -> {
        audioService.pause()
        result.success(1)
      }
      "resume" -> {
        audioService.resume()
        result.success(1)
      }
      "stop" -> {
        audioService.stop()
        result.success(1)
      }
      "release" -> {
        audioService.release()
        result.success(1)
      }
      "removeNotification" -> {
        audioService.removeNotification()
        result.success(1)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivity() {
    flutterActivity.unbindService(serviceConnection)
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    flutterActivity = binding.activity
    val intent = Intent(flutterActivity, AudioService::class.java)
    flutterActivity.startService(intent)
    flutterActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }

  private val serviceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceDisconnected(p0: ComponentName?) {
      isBindService = false
    }

    override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
      isBindService = true
      audioBinder = iBinder as AudioService.AudioBinder
      audioService = audioBinder.getService()
      audioService.setEventCallBack(eventCallBack)
      audioService.setActivity(flutterActivity)
    }
  }

  private val eventCallBack = object : AudioService.EventCallBack{
    override fun complete() {
      eventSink?.success("complete")
    }

    override fun resume() {
      eventSink?.success("resume")
    }

    override fun pause() {
      eventSink?.success("pause")
    }

    override fun stop() {
      eventSink?.success("stop")
    }

    override fun switchPrevious() {
      eventSink?.success("switchPrevious")
    }

    override fun switchNext() {
      eventSink?.success("switchNext")
    }

    override fun currentPosition(pos: Int) {
      curPosEventSink?.success(pos)
    }

    override fun preparedDuration(position: Int) {
      preparedDurationEventSink?.success(position)
    }

    override fun headPhoneStatus(out: Boolean) {
      eventSink?.success("headPhoneStatus: $out")
    }
  }
}
