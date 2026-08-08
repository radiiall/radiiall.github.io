PlaybackService example

This sample shows a MediaSessionService using ExoPlayer (Media3) with a ForwardingPlayer to normalize metadata.

Usage:
- Add INTERNET permission to AndroidManifest.xml: <uses-permission android:name="android.permission.INTERNET" />
- Declare the service in your manifest inside <application>:
  <service
    android:name=".PlaybackService"
    android:exported="false"
    android:foregroundServiceType="media" />
- If you want persistent background playback, create a notification channel and call startForeground with the notification returned by createNotification().

Notes:
- Do NOT include admin credentials or admin URLs in source code.
- Adjust DefaultHttpDataSource request properties only if required by your stream provider.
