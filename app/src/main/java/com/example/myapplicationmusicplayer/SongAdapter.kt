package com.example.myapplicationmusicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    // 1. 外部からのリストを保持するための変数名を変更
    private var allSongNames: List<String>,
    private val onSongClick: ((Int) -> Unit)? = null,
    private val onLongClickDelete: ((String) -> Unit)? = null
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    // 実際に画面に表示するリスト（初期値は全曲）
    private var filteredList: List<String> = allSongNames

    // 選択された曲名を保持するセット
    private val selectedSongs = mutableListOf<String>()

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSongName: TextView = view.findViewById(R.id.tv_song_name)
        val checkBox: CheckBox = view.findViewById(R.id.cb_select)
    }

    // ★ 追加：外部から曲が追加された時に呼び出すメソッド
    fun updateAllSongs(newList: List<String>) {
        // 全曲リストを最新の状態に更新
        this.allSongNames = newList.toList()
        // 現在の検索状態を維持したまま表示を更新するために filter を呼び出す
        // (検索中でなければ全曲が表示される)
        filter("")
    }

    //  検索フィルタリング用のメソッド
    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            allSongNames // allSongNames を使うように修正
        } else {
            allSongNames.filter { it.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged() // リスト更新を通知
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val songName = filteredList[position]
        holder.tvSongName.text = songName

        //削除ボタンの処理
        val btnDelete = holder.itemView.findViewById<ImageButton>(R.id.btn_delete)
        //このコードはアプリ内で追加した曲しか削除できない
        btnDelete.setOnClickListener {
            onLongClickDelete?.invoke(songName)
        }

        if (onSongClick != null) {
            holder.checkBox.visibility = View.GONE
            holder.itemView.setOnClickListener {
                onSongClick.invoke(position)
            }
        } else {
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = selectedSongs.contains(songName)

            holder.itemView.setOnClickListener {
                val isChecked = !holder.checkBox.isChecked
                holder.checkBox.isChecked = isChecked
                toggleSelection(songName, isChecked)
            }

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                toggleSelection(songName, isChecked)
            }
        }
    }


    private fun toggleSelection(songName: String, isChecked: Boolean) {
        if (isChecked) {
            //まだリストになければ追加
            if (!selectedSongs.contains(songName)) {
                selectedSongs.add(songName)
            }
        } else {
            selectedSongs.remove(songName)
        }
    }

    override fun getItemCount() = filteredList.size

    fun getSelectedSongs(): List<String> {
        return selectedSongs
    }
}