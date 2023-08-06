package com.example.vegasbluetooth.sound

interface SoundApi {

    var catSound: Int
    var chickenSound: Int
    var cowSound: Int
    var dogSound: Int
    var duckSound: Int
    var sheepSound: Int

    fun play(sound: Int)
}