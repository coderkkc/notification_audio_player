import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:notification_audio_player/notification_audio_player.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  NotificationAudioPlayer notificationAudioPlayer = NotificationAudioPlayer();

  @override
  void initState() {
    super.initState();
    notificationAudioPlayer.onCompleteEvent.listen((event) {
      print("播放完成");
    });
    notificationAudioPlayer.onResumeEvent.listen((event) {
      print("恢复");
    });
    notificationAudioPlayer.onPauseEvent.listen((event) {
      print("暂停");
    });
    notificationAudioPlayer.onStopEvent.listen((event) {
      print("停止");
    });
    notificationAudioPlayer.switchPreviousEvent.listen((event) {
      print("上一首");
    });
    notificationAudioPlayer.switchNextEvent.listen((event) {
      print("下一首");
    });
    notificationAudioPlayer.curPosEvent.listen((data) {
//      print('当前进度 $data');
    });
    notificationAudioPlayer.preparedDurationEvent.listen((data) {
      print("播放时间 $data");
    });
    notificationAudioPlayer.headPhoneOutEvent.listen((event) {
      print("耳机拔出");
    });
    notificationAudioPlayer.headPhoneInEvent.listen((event) {
      print("耳机插入");
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(children: [
            RaisedButton(
              child: Text('getPlayerState'),
              onPressed: () async{
                print(await notificationAudioPlayer.playerState);
              },
            ),
            RaisedButton(
              child: Text('getDuration'),
              onPressed: () async{
                print(await notificationAudioPlayer.duration);
              },
            ),
            RaisedButton(
              child: Text('getCurrentPosition'),
              onPressed: () async{
                print(await notificationAudioPlayer.currentPosition);
              },
            ),
            RaisedButton(
              child: Text('setWakeLock'),
              onPressed: () async{
                print(await notificationAudioPlayer.setWakeLock());
              },
            ),
            RaisedButton(
              child: Text('setSpeed'),
              onPressed: () async{
                print(await notificationAudioPlayer.setSpeed(1.5));
              },
            ),
            RaisedButton(
              child: Text('setVolume'),
              onPressed: () async{
                print(await notificationAudioPlayer.setVolume(0.5, 0.5));
              },
            ),
            RaisedButton(
              child: Text('setIsLooping'),
              onPressed: () async{
                print(await notificationAudioPlayer.setIsLooping(true));
              },
            ),
            RaisedButton(
              child: Text('seek'),
              onPressed: () async{
                print(await notificationAudioPlayer.seek(10000));
              },
            ),
            RaisedButton(
              child: Text('Play'),
              onPressed: () async{
                String title = "点歌的人";
                String author = "海来阿木";
                String avatar = "https://d.musicapp.migu.cn/prod/file-service/file-down/b1899d500dda5db2da11df3efc89cba6/d210e36411266f0305ada5026f041997/3856873e38b7e5c7e41c1a8cd725c18a";
                String url = "http://freetyst.nf.migu.cn/public/product9th/product41/2020/08/1013/2020年04月07日11点20分紧急内容准入众立文化2首/标清高清/MP3_128_16_Stero/63299100635132217.mp3";
                print(await notificationAudioPlayer.play(title, author, avatar, url));
//                print(await notificationAudioPlayer.playerState);
//                print(await notificationAudioPlayer.duration);
//                print(await notificationAudioPlayer.currentPosition);
              },
            ),
            RaisedButton(
              child: Text('pause'),
              onPressed: () async{
                print(await notificationAudioPlayer.pause());
              },
            ),
            RaisedButton(
              child: Text('resume'),
              onPressed: () async{
                print(await notificationAudioPlayer.resume());
              },
            ),
            RaisedButton(
              child: Text('stop'),
              onPressed: () async{
                print(await notificationAudioPlayer.stop());
              },
            ),
            RaisedButton(
              child: Text('release'),
              onPressed: () async{
                print(await notificationAudioPlayer.release());
              },
            ),
            RaisedButton(
              child: Text('removeNotification'),
              onPressed: () async{
                print(await notificationAudioPlayer.removeNotification());
              },
            ),
          ],)
        ),
      ),
    );
  }
}
