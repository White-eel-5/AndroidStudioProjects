package com.example.myapplicationmusicplayer

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlaylistActivity : AppCompatActivity() {
    companion object {
        var allPlaylists = mutableListOf<Playlist>()

        fun savePlaylists(context: Context) {
            val sharedPreferences = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val json = Gson().toJson(allPlaylists)
            editor.putString("playlists", json)
            editor.apply()
        }

        fun loadPlaylists(context: Context) {
            val sharedPreferences = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
            val json = sharedPreferences.getString("playlists", null)

            if (json != null) {
                try {
                    val type = object : TypeToken<MutableList<Playlist>>() {}.type
                    val loadedData: MutableList<Playlist> = Gson().fromJson(json, type)
                    allPlaylists.clear()
                    allPlaylists.addAll(loadedData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private lateinit var adapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        loadPlaylists(this)

        val rv = findViewById<RecyclerView>(R.id.rv_playlists)

        adapter = PlaylistAdapter(
            allPlaylists = allPlaylists,
            onClick = { playlist ->
                // 通常クリック：詳細画面へ
                val intent = Intent(this, PlaylistDetailActivity::class.java)
                intent.putExtra("PLAYLIST", playlist)
                startActivity(intent)
            },
            onLongClick = { playlist ->
                // 長押しクリック：削除確認ダイアログを表示
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("プレイリストの削除")
                    .setMessage("「${playlist.name}」を削除しますか？")
                    .setPositiveButton("削除") { _, _ ->
                        // 1. リストから削除
                        allPlaylists.remove(playlist)

                        // 2. データを永続保存
                        savePlaylists(this)

                        // 3. アダプターの表示を更新
                        adapter.updateData(allPlaylists)

                        // 4. 検索中だった場合のために再フィルタリング
                        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchViewPlaylist)
                        adapter.filter(searchView.query.toString())

                        android.widget.Toast.makeText(this, "削除しました", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        )


        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        //  検索機能の設定
        val searchView = findViewById<SearchView>(R.id.searchViewPlaylist)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                // 文字が入力されるたびにアダプターをフィルタリング
                adapter.filter(newText ?: "")
                return true
            }
        })

        findViewById<FloatingActionButton>(R.id.fab_add_playlist).setOnClickListener {
            val intent = Intent(this, SelectActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 1. 最新の全データをアダプターに同期
        adapter.updateData(allPlaylists)

        // 2. 現在の検索ワードを維持して再フィルタリング
        val searchView = findViewById<SearchView>(R.id.searchViewPlaylist)
        adapter.filter(searchView.query.toString())
    }
}