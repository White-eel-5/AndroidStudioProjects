package com.example.myapplicationmusicplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectActivity : AppCompatActivity() {

    private val songNames = mutableListOf<String>()
    private lateinit var songAdapter: SongAdapter

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "外部の曲"
            if (it.scheme == "content") {
                contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            fileName = cursor.getString(index)
                        }
                    }
                }
            }
            if (fileName == "外部の曲") {
                fileName = it.lastPathSegment?.substringAfterLast("/") ?: "外部の曲"
            }
            try {
                val destFile = java.io.File(filesDir, fileName)
                contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "ファイルの読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                return@let
            }

            songNames.add(fileName)
            songAdapter.updateAllSongs(songNames)
            val currentQuery = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView).query.toString()
            songAdapter.filter(currentQuery)
            Toast.makeText(this, "「$fileName」を追加しました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        // 1. res/raw 内の曲を読み込む
        val rawMusicList = R.raw::class.java.fields.map { field ->
            resources.getIdentifier(field.name, "raw", packageName)
        }
        songNames.addAll(rawMusicList.map { resources.getResourceEntryName(it) })

        // 2. 保存済みファイルを読み込む
        val savedFiles = filesDir.listFiles()
        savedFiles?.forEach { file ->
            if (!songNames.contains(file.name)) {
                songNames.add(file.name)
            }
        }

        val rvSongList = findViewById<RecyclerView>(R.id.rv_song_list)
        val buttonAdd = findViewById<ImageButton>(R.id.button_add)

        // 3. アダプターの初期化（削除処理もここに含める）
        songAdapter = SongAdapter(
            songNames,
            onSongClick = null,
            onLongClickDelete = { songName ->
                AlertDialog.Builder(this)
                    .setTitle("曲の削除")
                    .setMessage("「$songName」を削除しますか？\n(アプリ内からのみ削除されます)")
                    .setPositiveButton("削除") { _, _ ->
                        deleteSong(songName) // ここで呼び出し
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        )
        rvSongList.layoutManager = LinearLayoutManager(this)
        rvSongList.adapter = songAdapter

        // 4. 検索機能の設定
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                songAdapter.filter(newText ?: "")
                return true
            }
        })

        // 5. ボタンの設定
        buttonAdd.setOnLongClickListener {
            pickAudioLauncher.launch("audio/*")
            true
        }
        buttonAdd.setOnClickListener {
            val selectedSongs = songAdapter.getSelectedSongs()
            if (selectedSongs.isNotEmpty()) {
                showSaveDialog(selectedSongs)
            } else {
                Toast.makeText(this, "曲を1つ以上選択してください", Toast.LENGTH_SHORT).show()
            }
        }
    } // onCreate の終わり

    // 🗑 削除処理の関数（onCreateの外に配置）
    private fun deleteSong(songName: String) {
        // 1. 内部ストレージからファイルを削除
        val file = java.io.File(filesDir, songName)
        if (file.exists()) {
            file.delete()
        }

        // 2. リストから削除
        songNames.remove(songName)

        //削除された曲は全プレイリストから除外される
        PlaylistActivity.allPlaylists.forEach{ playlist
            ->
            playlist.songs.removeAll{ it == songName }
        }

        PlaylistActivity.savePlaylists(this)

        // 3. アダプターを更新
        songAdapter.updateAllSongs(songNames)

        //現在の検索ワードで再フィルタリング
        val currentQuery = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView).query.toString()
        songAdapter.filter(currentQuery)
        Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show()
    }

    private fun showSaveDialog(selectedSongs: List<String>) {
        val editText = EditText(this)
        editText.hint = "プレイリスト名を入力"
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 0)
        editText.layoutParams = params
        container.addView(editText)

        AlertDialog.Builder(this)
            .setTitle("プレイリストの保存")
            .setMessage("プレイリストの名前を決めてください")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val name = editText.text.toString().ifEmpty {
                    "マイプレイリスト ${PlaylistActivity.allPlaylists.size + 1}"
                }
                val newPlaylist = Playlist(name, selectedSongs.toMutableList())
                PlaylistActivity.allPlaylists.add(newPlaylist)
                PlaylistActivity.savePlaylists(this)
                Toast.makeText(this, "「$name」を保存しました", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}