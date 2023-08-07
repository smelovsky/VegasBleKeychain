package com.example.vegasbluetooth.di

import android.app.Application
import android.content.Context
import com.example.vegasbluetooth.ble.BleScanApi
import com.example.vegasbluetooth.ble.BleScanImpl
import com.example.vegasbluetooth.sound.SoundApi
import com.example.vegasbluetooth.sound.SoundImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object BleScanModule {
    @Provides
    @Singleton
    fun provideBleScanApi(@ApplicationContext appContext: Context): BleScanApi {

        return BleScanImpl(appContext)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SoundModule {
    @Provides
    @Singleton
    fun provideSoundApi(app: Application): SoundApi {

        return SoundImpl(app)
    }
}

/*
@Module
@InstallIn(SingletonComponent::class)
object PlaySoundModule {
    @Provides
    fun providePlaySound() {

        return PlaySoundImpl()
    }

    private fun PlaySoundImpl() {

    }

}

 */



