package com.factory.capturemasterpro

import android.app.Application
import com.factory.capturemasterpro.billing.BillingManager
import com.factory.capturemasterpro.billing.PremiumManager
import com.factory.capturemasterpro.data.db.AppDatabase
import com.factory.capturemasterpro.data.repository.RecordingRepository

class CaptureMasterApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: RecordingRepository by lazy { RecordingRepository(database.recordingDao()) }
    val premiumManager: PremiumManager by lazy { PremiumManager(this) }
    val billingManager: BillingManager by lazy { BillingManager(this, premiumManager) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        billingManager.startConnection()
    }

    companion object {
        lateinit var instance: CaptureMasterApp
            private set
    }
}
