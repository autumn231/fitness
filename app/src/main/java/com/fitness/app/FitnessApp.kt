package com.fitness.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
