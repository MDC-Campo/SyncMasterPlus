package com.example.syncmasterplus.di

import android.content.Context
import androidx.room.Room
import com.example.syncmasterplus.bluetooth.AndroidBluetoothController
import com.example.syncmasterplus.data.chat.room.RoomDao
import com.example.syncmasterplus.data.chat.room.database.AppDatabase
import com.example.syncmasterplus.data.chat.storage.ExternalStorage
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothController
import com.example.syncmasterplus.domain.chat.playback.AndroidAudioPlayer
import com.example.syncmasterplus.domain.chat.recorder.AndroidAudioRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
        return AndroidBluetoothController(context)
    }

    @Provides
    fun provideAudioPlayer(@ApplicationContext context: Context): AndroidAudioPlayer {
        return AndroidAudioPlayer(context)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AndroidAudioRecorder {
        return AndroidAudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideCacheDir(@ApplicationContext context: Context): File {
        return context.cacheDir
    }

    @Provides
    @Singleton
    fun provideExternalStorage(@ApplicationContext context: Context): ExternalStorage {
        return ExternalStorage(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .fallbackToDestructiveMigration().build()
    }


    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): RoomDao {
        return db.roomDao()
    }
}