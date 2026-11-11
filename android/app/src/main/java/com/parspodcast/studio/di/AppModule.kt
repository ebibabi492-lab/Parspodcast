package com.parspodcast.studio.di

import android.content.Context
import androidx.room.Room
import com.parspodcast.studio.data.ai.LiteRTLMEngine
import com.parspodcast.studio.data.local.ParsPodcastDatabase
import com.parspodcast.studio.data.local.dao.*
import com.parspodcast.studio.data.manager.ModelFileManager
import com.parspodcast.studio.data.repository.*
import com.parspodcast.studio.domain.ai.LocalLLMProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ParsPodcastDatabase {
        return Room.databaseBuilder(
            context,
            ParsPodcastDatabase::class.java,
            ParsPodcastDatabase.DATABASE_NAME
        ).addMigrations(ParsPodcastDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideNewsDao(db: ParsPodcastDatabase): NewsDao = db.newsDao()

    @Provides
    fun provideProductionDao(db: ParsPodcastDatabase): ProductionDao = db.productionDao()

    @Provides
    fun provideVoiceDao(db: ParsPodcastDatabase): VoiceDao = db.voiceDao()

    @Provides
    fun providePublishingDao(db: ParsPodcastDatabase): PublishingDao = db.publishingDao()

    @Provides
    @Singleton
    fun provideModelFileManager(@ApplicationContext context: Context): ModelFileManager {
        return ModelFileManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    abstract fun bindProductionRepository(impl: ProductionRepositoryImpl): ProductionRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(impl: VoiceRepositoryImpl): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindPublishingRepository(impl: PublishingRepositoryImpl): PublishingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindLocalLLMProvider(impl: LiteRTLMEngine): LocalLLMProvider

    @Binds
    @Singleton
    abstract fun bindTTSProvider(impl: com.parspodcast.studio.data.tts.SherpaOnnxTtsEngine): com.parspodcast.studio.domain.tts.TTSProvider
}
