package com.example.vegasbluetooth.sound

import android.app.Application
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.IOException
import javax.inject.Inject

class SoundImpl@Inject constructor(
    val application: Application
): SoundApi {

    private lateinit var soundPool: SoundPool
    private lateinit var assetManager: AssetManager

    override var catSound: Int = 0
    override var chickenSound: Int = 0
    override var cowSound: Int = 0
    override var dogSound: Int = 0
    override var duckSound: Int = 0
    override var sheepSound: Int = 0

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .build()

        assetManager = application.assets
        catSound = loadSound("cat.ogg")
        chickenSound = loadSound("chicken.ogg")
        cowSound = loadSound("cow.ogg")
        dogSound = loadSound("dog.ogg")
        duckSound = loadSound("duck.ogg")
        sheepSound = loadSound("sheep.ogg")
    }

    override fun play(sound: Int) {
        if (sound > 0) {
            soundPool.play(sound, 1F, 1F, 1, 0, 1F)
        }
    }

    private fun loadSound(fileName: String): Int {

        val afd: AssetFileDescriptor = try {
            application.assets.openFd(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("zzz", "Can't load $fileName")

            return -1
        }

        val ret = soundPool.load(afd, 1)

        return ret
    }
}