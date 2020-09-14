import 'package:flutter/material.dart';
import 'package:notification_audio_player/notification_audio_player.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  NotificationAudioPlayer notificationAudioPlayer = NotificationAudioPlayer();

  @override
  void initState() {
    super.initState();
    notificationAudioPlayer.onCompleteEvent.listen((event) {
      print("complete");
    });
    notificationAudioPlayer.onResumeEvent.listen((event) {
      print("resume");
    });
    notificationAudioPlayer.onPauseEvent.listen((event) {
      print("pause");
    });
    notificationAudioPlayer.onStopEvent.listen((event) {
      print("stop");
    });
    notificationAudioPlayer.switchPreviousEvent.listen((event) {
      print("switch previous");
    });
    notificationAudioPlayer.switchNextEvent.listen((event) {
      print("swtich next");
    });
    notificationAudioPlayer.curPosEvent.listen((data) {
      print('current position: $data');
    });
    notificationAudioPlayer.preparedDurationEvent.listen((data) {
      print("duration: $data");
    });
    notificationAudioPlayer.headPhoneOutEvent.listen((event) {
      print("headphone out");
    });
    notificationAudioPlayer.headPhoneInEvent.listen((event) {
      print("headphone in");
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Notification Audio Player'),
        ),
        body: Center(
          child: Column(children: [
            RaisedButton(
              child: Text('Play'),
              onPressed: () async{
                String title = "Shape of Yo";
                String author = "J.Fla";
                String avatar = "http://p3.music.126.net/hZ2ttGYOQbL9ei9yABpejQ==/109951163032775841.jpg?param=320y320";
                String url = "https://music.163.com/song/media/outer/url?id=468882985.mp3";
                print(await notificationAudioPlayer.play(title, author, avatar, url));
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
          ],)
        ),
      ),
    );
  }
}
