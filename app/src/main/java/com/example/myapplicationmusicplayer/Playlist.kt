package com.example.myapplicationmusicplayer

import java.io.Serializable

data class Playlist (
    val name:String,
    val songs:MutableList<String>
) : Serializable