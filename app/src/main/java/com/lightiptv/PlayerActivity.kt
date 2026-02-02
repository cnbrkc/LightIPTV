package com.lightiptv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.lightiptv.databinding.ActivityPlayerBinding
import com.lightiptv.utils.PrefsManager

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var prefs: PrefsManager
    
    private var player: ExoPlayer? = null
    private var channelName: String = ""
    private var channelUrl: String = ""
    
    private val hideHandler = Handler(Looper.getMainLooper())
    private var controlsVisible = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Tam ekran
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        channelName = intent.getStringExtra("channel_name") ?: ""
        channelUrl = intent.getStringExtra("channel_url") ?: ""
        
        binding.tvChannelName.text = channelName
        
        initializePlayer()
    }
    
    private fun initializePlayer() {
        if (channelUrl.isEmpty()) {
            showError("Kanal URL'si bulunamadı")
            finish()
            return
        }
        
        // Ultra hafif load control
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                prefs.bufferSize,           // Min buffer
                prefs.bufferSize * 2,       // Max buffer
                500,                         // Playback başlama buffer
                1000                         // Rebuffer sonrası
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        
        // HTTP DataSource
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("ExoPlayer")
        
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        
        // Media Source
        val mediaSource = when {
            channelUrl.contains(".m3u8") || channelUrl.contains("m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(channelUrl))
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(channelUrl))
            }
        }
        
        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaSource(mediaSource)
                playWhenReady = true
                prepare()
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                binding.progressBuffer.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                binding.progressBuffer.visibility = View.GONE
                            }
                            Player.STATE_ENDED -> {
                                // Canlı yayın için yeniden başlat
                                seekTo(0)
                                play()
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        showError("Oynatma hatası: ${error.message}")
                        // Yeniden dene
                        Handler(Looper.getMainLooper()).postDelayed({
                            prepare()
                            play()
                        }, 2000)
                    }
                })
            }
        
        binding.playerView.player = player
    }
    
    private fun showControls() {
        binding.layoutControls.visibility = View.VISIBLE
        controlsVisible = true
        
        hideHandler.removeCallbacksAndMessages(null)
        hideHandler.postDelayed({
            hideControls()
        }, 3000)
    }
    
    private fun hideControls() {
        binding.layoutControls.visibility = View.GONE
        controlsVisible = false
    }
    
    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                binding.ivPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                it.play()
                binding.ivPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (controlsVisible) {
                    togglePlayPause()
                } else {
                    showControls()
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (controlsVisible) {
                    hideControls()
                    return true
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                showControls()
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        player?.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
