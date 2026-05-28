package com.domedemok.travelplanner.di

import com.domedemok.travelplanner.data.repository.AiChatRepository
import com.domedemok.travelplanner.data.repository.AiChatRepositoryImpl
import com.domedemok.travelplanner.data.repository.AuthRepository
import com.domedemok.travelplanner.data.repository.AuthRepositoryImpl
import com.domedemok.travelplanner.data.repository.ChatRepository
import com.domedemok.travelplanner.data.repository.ChatRepositoryImpl
import com.domedemok.travelplanner.data.repository.ExpenseRepository
import com.domedemok.travelplanner.data.repository.ExpenseRepositoryImpl
import com.domedemok.travelplanner.data.repository.ItineraryRepository
import com.domedemok.travelplanner.data.repository.ItineraryRepositoryImpl
import com.domedemok.travelplanner.data.repository.PhotoRepository
import com.domedemok.travelplanner.data.repository.PhotoRepositoryImpl
import com.domedemok.travelplanner.data.repository.PlaceRepository
import com.domedemok.travelplanner.data.repository.PlaceRepositoryImpl
import com.domedemok.travelplanner.data.repository.TripRepository
import com.domedemok.travelplanner.data.repository.TripRepositoryImpl
import com.domedemok.travelplanner.util.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * JS / Wasm Koin bindings — web is online-only and uses the GitLive Firebase
 * wrappers, so the JS repository impls don't take Auth/Firestore singletons.
 */
actual val platformModule: Module = module {
    single { NetworkMonitor() }

    single<AuthRepository>      { AuthRepositoryImpl() }
    single<TripRepository>      { TripRepositoryImpl() }
    single<PlaceRepository>     { PlaceRepositoryImpl(get()) }
    single<ChatRepository>      { ChatRepositoryImpl() }
    single<AiChatRepository>    { AiChatRepositoryImpl() }
    single<ExpenseRepository>   { ExpenseRepositoryImpl() }
    single<ItineraryRepository> { ItineraryRepositoryImpl() }
    single<PhotoRepository>     { PhotoRepositoryImpl() }
}
