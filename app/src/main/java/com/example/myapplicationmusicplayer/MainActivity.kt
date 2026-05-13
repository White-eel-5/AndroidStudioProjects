package com.example.myapplicationmusicplayer

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // 管理を Int から String (Uri文字列) に変更
    private var musicList: List<String> = listOf()
    private var currentIndex = 0
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 全曲リストを自動取得し、Uri文字列のリストに変換
        val allMusicList = R.raw::class.java.fields
            .map { field ->
                val resId = resources.getIdentifier(field.name, "raw", packageName)
                // リソースIDを android.resource:// パッケージ名/ID の形式のUri文字列に変換
                "android.resource://$packageName/$resId"
            }.filter { !it.endsWith("/0") }

        musicList = allMusicList

        // 2. Intentからデータを受け取る
        val selectedSongNames = intent.getStringArrayListExtra("SELECTED_SONGS")
        val startIndex = intent.getIntExtra("SONG_INDEX", 0)

        // 3. 再生用リストを決定する
        if (!selectedSongNames.isNullOrEmpty()) {
            musicList = selectedSongNames.mapNotNull { name ->
                // 名前から一致するUriを探す
                val rawUri = allMusicList.find { uriString ->
                    val resId = uriString.substringAfterLast("/").toInt()
                    resources.getResourceEntryName(resId) == name
                }
                if (rawUri != null) {
                    rawUri
                } else {
                    // 🌟ここから追加：見つからなければ内部ストレージ(filesDir)を探す
                    val file = java.io.File(filesDir, name)
                    if (file.exists()) {
                        file.toURI().toString() // ファイルの場所を返す
                    } else {
                        null
                    }



                }
            }
        }

        currentIndex = startIndex

        // --- レイアウト調整 ---
        val container = findViewById<android.view.View>(R.id.container)
        if (container != null) {
            ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        volumeControlStream = AudioManager.STREAM_MUSIC

        // ビューの取得
        val buttonSelect = findViewById<ImageButton>(R.id.button_select)
        val buttonPlay = findViewById<FloatingActionButton>(R.id.button_play)
        val buttonPrev = findViewById<ImageButton>(R.id.button_prev)
        val buttonNext = findViewById<ImageButton>(R.id.button_next)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)

        buttonSelect.setOnClickListener { finish() }

        buttonPlay.setOnClickListener {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                audioPause()
                buttonPlay.setImageResource(android.R.drawable.ic_media_play)
            } else {
                audioPlay()
                buttonPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        buttonPrev.setOnClickListener {
            if (musicList.isNotEmpty()) {
                currentIndex = if (currentIndex > 0) currentIndex - 1 else musicList.size - 1
                changeMusic()
                buttonPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        buttonNext.setOnClickListener {
            if (musicList.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % musicList.size
                changeMusic()
                buttonPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    updateTimeLabels()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        runnable = Runnable {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    updateTimeLabels()
                }
            }
            handler.postDelayed(runnable, 500)
        }

        updateTitle()
        audioPlay()
        buttonPlay.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun updateTitle() {
        val textViewTitle = findViewById<TextView>(R.id.textView_title)
        if (textViewTitle != null && musicList.isNotEmpty()) {
            val uriString = musicList[currentIndex]
            val title = if (uriString.startsWith("android.resource")) {
                val resId = uriString.substringAfterLast("/").toInt()
                resources.getResourceEntryName(resId)
            } else {
                // 外部ファイルの場合はUriの最後を表示
                Uri.parse(uriString).lastPathSegment ?: "Unknown"
            }
            textViewTitle.text = title
        }
    }

    private fun updateTimeLabels() {
        val tvPosition = findViewById<TextView>(R.id.textView_position)
        val tvDuration = findViewById<TextView>(R.id.textView_duration)
        mediaPlayer?.let {
            tvPosition?.text = formatTime(it.currentPosition)
            tvDuration?.text = formatTime(it.duration)
        }
    }

    private fun formatTime(ms: Int): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun changeMusic() {
        audioStop()
        updateTitle()
        audioPlay()
    }

    private fun audioPlay() {
        if (musicList.isEmpty()) return
        try {
            if (mediaPlayer == null) {
                // 🎵 MediaPlayer.create ではなく、setDataSource を使う方式に変更
                mediaPlayer = MediaPlayer().apply {
                    val uriStr = musicList[currentIndex]
                    if (uriStr.startsWith("android.resource://")) {
                        setDataSource(this@MainActivity, Uri.parse(uriStr))
                    } else {
                        // 内部ストレージのファイルは権限の関係でFileDescriptor経由で渡す必要がある
                        val file = java.io.File(Uri.parse(uriStr).path!!)
                        java.io.FileInputStream(file).use { fis ->
                            setDataSource(fis.fd)
                        }
                    }
                    prepare() // 外部ファイルも扱うため prepare が必要
                    val seekBar = findViewById<SeekBar>(R.id.seekBar)
                    seekBar.max = duration
                    setOnCompletionListener {
                        findViewById<ImageButton>(R.id.button_next).performClick()
                    }
                }
            }
            mediaPlayer?.start()
            handler.post(runnable)

            updateTitle() // タイトル表示を更新
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "再生エラーが発生しました。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun audioPause() {
        mediaPlayer?.pause()
        handler.removeCallbacks(runnable)
    }

    private fun audioStop() {
        handler.removeCallbacks(runnable)
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        audioStop()
    }
}