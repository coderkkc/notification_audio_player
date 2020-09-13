import 'dart:async';
import 'package:flutter/services.dart';

enum AudioPlayerState {
  STOPPED,
  PLAYING,
  PAUSED,
  COMPLETED,
}

class NotificationAudioPlayer {
  static const MethodChannel _channel = const MethodChannel('notification_audio_player');
  static const EventChannel _eventChannel = const EventChannel("notification_audio_player_event");
  static const EventChannel _preparedDurationEventChannel = const EventChannel("notification_audio_player_preparedDuration_event");
  static const EventChannel _curPosEventChannel = const EventChannel("notification_audio_player_curPos_event");

  // ignore: close_sinks
  static final StreamController _completeEventController = StreamController.broadcast();
  static final StreamController _resumeEventController = StreamController.broadcast();
  static final StreamController _pauseEventController = StreamController.broadcast();
  static final StreamController _stopEventController = StreamController.broadcast();
  static final StreamController _switchPreviousEventController = StreamController.broadcast();
  static final StreamController _switchNextEventController = StreamController.broadcast();
  static final StreamController _headPhoneOutEventController = StreamController.broadcast();
  static final StreamController _headPhoneInEventController = StreamController.broadcast();
  static final StreamController _preparedDurationEventController = StreamController.broadcast();
  static final StreamController _curPosEventController = StreamController.broadcast();

  Stream get onCompleteEvent => _completeEventController.stream;
  Stream get onResumeEvent => _resumeEventController.stream;
  Stream get onPauseEvent => _pauseEventController.stream;
  Stream get onStopEvent => _stopEventController.stream;
  Stream get switchPreviousEvent => _switchPreviousEventController.stream;
  Stream get switchNextEvent => _switchNextEventController.stream;
  Stream get headPhoneOutEvent => _headPhoneOutEventController.stream;
  Stream get headPhoneInEvent => _headPhoneInEventController.stream;
  Stream get preparedDurationEvent => _preparedDurationEventController.stream;
  Stream get curPosEvent => _curPosEventController.stream;

  NotificationAudioPlayer(){
    _eventChannel.receiveBroadcastStream().listen((event) {
      switch(event) {
        case "complete":
          _completeEventController.add(event);
          break;
        case "resume":
          _resumeEventController.add(event);
          break;
        case "pause":
          _pauseEventController.add(event);
          break;
        case "stop":
          _stopEventController.add(event);
          break;
        case "switchPrevious":
          _switchPreviousEventController.add(event);
          break;
        case "switchNext":
          _switchNextEventController.add(event);
          break;
        case "headPhoneStatus: true":
          _headPhoneOutEventController.add(event);
          break;
        case "headPhoneStatus: false":
          _headPhoneInEventController.add(event);
          break;
        default:
          print("unknown event!");
      }
    });
    _preparedDurationEventChannel.receiveBroadcastStream().listen((event) {
      _preparedDurationEventController.add(event);
    });
    _curPosEventChannel.receiveBroadcastStream().listen((event) {
      _curPosEventController.add(event);
    });
  }

  // 获取播放状态
  Future<String> get playerState async{
    return await _channel.invokeMethod('getPlayerState');
  }

  // 获取总时长
  Future<int> get duration async{
    return await _channel.invokeMethod('getDuration');
  }

  // 获取当前时长
  Future<int> get currentPosition async{
    return await _channel.invokeMethod('getCurrentPosition');
  }

  // 阻止休眠
  Future<int> setWakeLock() async{
    return await _channel.invokeMethod('setWakeLock');
  }

  // 设置速率(>= Android.M)
  Future<int> setSpeed(double speed) async{
    return await _channel.invokeMethod('setSpeed', {'speed': speed});
  }

  // 设置音量(只能设置当前音量的百分比，系统音量需要AudioManger控制)
  Future<int> setVolume(double leftVolume, double rightVolume) async{
    return await _channel.invokeMethod('setVolume', {
      'leftVolume': leftVolume,
      'rightVolume': rightVolume
    });
  }

  // 设置是否播完完毕自动循环
  Future<int> setIsLooping(bool isLooping) async{
    return await _channel.invokeMethod('setIsLooping', {'isLooping': isLooping});
  }

  // 跳转指定进度
  Future<int> seek(int position) async{
    return await _channel.invokeMethod('seek', {'position': position});
  }

  // 设置音频播放
  Future<int> play(String title, String author, String avatar, String url) async{
    return await _channel.invokeMethod('play', {
      'title': title,
      'author': author,
      'avatar': avatar,
      'url': url
    });
  }

  // 暂停
  Future<int> pause() async{
    return await _channel.invokeMethod('pause');
  }

  // 恢复
  Future<int> resume() async{
    return await _channel.invokeMethod('resume');
  }

  // 停止
  Future<int> stop() async{
    return await _channel.invokeMethod('stop');
  }

  // 释放
  Future<int> release() async{
    return await _channel.invokeMethod('release');
  }

  // 关闭通知栏
  Future<int> removeNotification() async{
    return await _channel.invokeMethod('removeNotification');
  }
}
