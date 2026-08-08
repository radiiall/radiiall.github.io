package com.radiaiallbroadcastnetwork

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Configuración de la fuente HTTP; evita incluir credenciales administrativas en el código
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RADIIALL-Android/1.0")
            .setDefaultRequestProperties(mapOf(
                // Solo incluir cabeceras necesarias; ajusta según lo que realmente requiera el servidor.
                "Icy-MetaData" to "1"
            ))
            .setAllowCrossProtocolRedirects(true)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(httpDataSourceFactory)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // ForwardingPlayer para interceptar/normalizar metadatos
        val forwardingPlayer = object : ForwardingPlayer(player!!) {
            override fun getMediaMetadata(): MediaMetadata {
                val metadata = super.getMediaMetadata()
                val title = metadata.title?.toString() ?: ""
                val artist = metadata.artist?.toString() ?: ""

                val cleanTitle = title.split("|")[0].trim()
                val cleanArtist = artist.split("|")[0].trim()

                val isGeneric = cleanTitle.isBlank() || cleanTitle.lowercase() in listOf(
                    "no name", "n/a", "unknown", "radiall", "radiiall", "radio en vivo"
                )

                val displayTitle = if (isGeneric) "RADIIALL" else cleanTitle
                val displayArtist = if (cleanArtist.isNotEmpty() && cleanArtist.lowercase() != "unknown") cleanArtist else "RADIIALL ONLINE"

                return metadata.buildUpon()
                    .setTitle(displayTitle)
                    .setDisplayTitle(displayTitle)
                    .setArtist(displayArtist)
                    .setAlbumArtist("RADIIALL")
                    .setStation("RADIIALL")
                    .build()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()

        // Si quieres que el servicio siga en background, crea notificación y llama startForeground(...)
        // startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Decide si quieres parar la reproducción o mantenerla
        // mediaSession?.player?.stop()
        // stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            mediaSession?.player?.release()
            mediaSession?.release()
        } catch (t: Throwable) {
            // log if needed
        } finally {
            mediaSession = null
            player = null
            super.onDestroy()
        }
    }

    // ejemplo simple de notificación (necesitas canal en Android O+)
    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "media_channel")
            .setContentTitle("RADIIALL")
            .setContentText("Reproduciendo en vivo")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pending)
            .build()
    }
}
