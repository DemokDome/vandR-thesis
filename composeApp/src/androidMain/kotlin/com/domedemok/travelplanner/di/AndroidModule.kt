package com.domedemok.travelplanner.di

import com.domedemok.travelplanner.BuildKonfig
import com.domedemok.travelplanner.data.local.DatabaseDriverFactory
import com.domedemok.travelplanner.data.local.database.TravelDatabase
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Koin bindings: Firebase singletons, the SQLDelight database, and the
 * Android-specific repository implementations.
 */
actual val platformModule: Module = module {
    single<FirebaseAuth>      { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }
    single<FirebaseDatabase> {
        FirebaseDatabase.getInstance(BuildKonfig.FIREBASE_DATABASE_URL)
    }

    // The driver factory is only needed to construct the database — no need
    // to surface it as its own singleton.
    single { TravelDatabase(DatabaseDriverFactory(androidContext()).createDriver()) }

    single { NetworkMonitor(androidContext()) }

    single<AuthRepository>      { AuthRepositoryImpl(get(), get()) }
    single<TripRepository>      { TripRepositoryImpl(get(), get(), get()) }
    single<PlaceRepository>     { PlaceRepositoryImpl(get(), get(), get(), get()) }
    single<ChatRepository>      { ChatRepositoryImpl(get(), get()) }
    single<AiChatRepository>    { AiChatRepositoryImpl(get(), get()) }
    single<ExpenseRepository>   { ExpenseRepositoryImpl(get()) }
    single<ItineraryRepository> { ItineraryRepositoryImpl(get()) }
    single<PhotoRepository>     { PhotoRepositoryImpl(get(), get()) }
}
