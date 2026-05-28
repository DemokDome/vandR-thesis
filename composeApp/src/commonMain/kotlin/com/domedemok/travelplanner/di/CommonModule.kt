package com.domedemok.travelplanner.di

import com.domedemok.travelplanner.data.remote.AiService
import com.domedemok.travelplanner.data.remote.ContextBuilder
import com.domedemok.travelplanner.data.remote.FoursquareService
import com.domedemok.travelplanner.viewmodel.AiChatViewModel
import com.domedemok.travelplanner.viewmodel.AuthViewModel
import com.domedemok.travelplanner.viewmodel.BudgetViewModel
import com.domedemok.travelplanner.viewmodel.ChatViewModel
import com.domedemok.travelplanner.viewmodel.GalleryViewModel
import com.domedemok.travelplanner.viewmodel.InboxViewModel
import com.domedemok.travelplanner.viewmodel.ItineraryViewModel
import com.domedemok.travelplanner.viewmodel.JoinTripViewModel
import com.domedemok.travelplanner.viewmodel.PlaceViewModel
import com.domedemok.travelplanner.viewmodel.TripDetailViewModel
import com.domedemok.travelplanner.viewmodel.TripViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform-specific Koin module — provides Firebase, the local DB, and per-platform repositories. */
expect val platformModule: Module

val commonModule = module {
    single { ContextBuilder() }
    single { FoursquareService(get()) }
    single { AiService(get()) }

    // Shared Ktor client. Json keeps `ignoreUnknownKeys` so additive Foursquare /
    // Gemini fields don't break parsing; `prettyPrint` is omitted because the wire
    // payloads aren't read by humans and indentation just bloats requests.
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient         = true
                })
            }
        }
    }

    // viewModelOf resolves constructor parameters automatically — no manual get() chains needed.
    viewModelOf(::AuthViewModel)
    viewModelOf(::TripViewModel)
    viewModelOf(::TripDetailViewModel)
    viewModelOf(::JoinTripViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::PlaceViewModel)
    viewModelOf(::AiChatViewModel)
    viewModelOf(::BudgetViewModel)
    viewModelOf(::ItineraryViewModel)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::InboxViewModel)
}

/** Master module exported to the Koin starter — the union of common + per-platform bindings. */
val appModule = module {
    includes(commonModule, platformModule)
}
