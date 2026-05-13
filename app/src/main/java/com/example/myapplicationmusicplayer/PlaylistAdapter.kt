package com.example.myapplicationmusicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaylistAdapter(
    private var allPlaylists: List<Playlist>, // 全データ
    private val onClick: (Playlist) -> Unit,
    private val onLongClick: (Playlist) -> Unit,
): RecyclerView.Adapter<PlaylistAdapter.VH>() {

    //  実際に画面に表示するリスト（検索結果）
    private var filteredList: List<Playlist> = allPlaylists

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(android.R.id.text1)
    }

    //  検索フィルタリング用関数
    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            allPlaylists
        } else {
            allPlaylists.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    //  データの更新用（PlaylistActivityから呼ぶ）
    fun updateData(newList: List<Playlist>) {
        allPlaylists = newList
        // 現在の検索状態は維持せず、一旦全表示にするか、Activity側で再度filterを呼ぶ
        filteredList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val playlist = filteredList[position]
        holder.text.text = playlist.name
        //単押し
        holder.itemView.setOnClickListener {
            onClick(playlist)
        }
        //長押し
        holder.itemView.setOnLongClickListener {
            onLongClick(playlist)
            true
        }
    }

    //  表示するのは filteredList のサイズ
    override fun getItemCount() = filteredList.size
}