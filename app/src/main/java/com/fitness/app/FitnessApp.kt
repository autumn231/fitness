package com.fitness.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.fitness.app.data.ExerciseRepository
import com.fitness.app.data.local.AppDatabase
import com.fitness.app.data.prefs.SettingsDataStore

class FitnessApp : Application(), ImageLoaderFactory {

    lateinit var repository: ExerciseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = ExerciseRepository(this, db, SettingsDataStore(this))
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                // minSdk 31，ImageDecoder 一定可用，GIF 动图直接走系统解码器
                add(ImageDecoderDecoder.Factory())
            }
            .build()
}
