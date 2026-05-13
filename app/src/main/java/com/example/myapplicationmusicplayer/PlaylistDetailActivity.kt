package com.example.myapplicationmusicplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        // プレイリストデータを取得
        val playlist = intent.getSerializableExtra("PLAYLIST") as? Playlist ?: return
        title = playlist.name

        val rv = findViewById<RecyclerView>(R.id.rv_playlist_songs)
        rv.layoutManager = LinearLayoutManager(this)

        // 🎵 修正ポイント
        // 1. playlist.songNames を playlist.songs に変更
        // 2. SongAdapter の引数を (リスト, クリック処理, 削除処理) の3つに合わせる
        val adapter = SongAdapter(
            allSongNames = playlist.songs,
            onSongClick = { position ->
                // 曲がタップされたらMainActivityへ
                val intent = Intent(this, MainActivity::class.java)

                // プレイリストの全曲（選んだ順）を渡す
                intent.putStringArrayListExtra("SELECTED_SONGS", ArrayList(playlist.songs))

                // タップした位置（position）を渡すことで、その曲から再生が始まる
                intent.putExtra("SONG_INDEX", position)

                startActivity(intent)
            },
            onLongClickDelete = null // 詳細画面では削除ボタンを表示しない（または何もしない）
        )

        rv.adapter = adapter
    }
}