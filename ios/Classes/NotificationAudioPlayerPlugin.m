#import "NotificationAudioPlayerPlugin.h"
#if __has_include(<notification_audio_player/notification_audio_player-Swift.h>)
#import <notification_audio_player/notification_audio_player-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "notification_audio_player-Swift.h"
#endif

@implementation NotificationAudioPlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftNotificationAudioPlayerPlugin registerWithRegistrar:registrar];
}
@end
