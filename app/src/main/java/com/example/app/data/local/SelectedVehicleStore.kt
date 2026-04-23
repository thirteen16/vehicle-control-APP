package com.example.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.selectedVehicleDataStore by preferencesDataStore(name = "selected_vehicle_store")

class SelectedVehicleStore(private val context: Context) {

    companion object {
        private val SELECTED_VEHICLE_ID_KEY = stringPreferencesKey("selected_vehicle_id")
    }

    suspend fun saveSelectedVehicleId(vehicleId: String) {
        context.selectedVehicleDataStore.edit { prefs ->
            if (vehicleId.isBlank()) {
                prefs.remove(SELECTED_VEHICLE_ID_KEY)
            } else {
                prefs[SELECTED_VEHICLE_ID_KEY] = vehicleId
            }
        }
    }

    suspend fun getSelectedVehicleId(): String? {
        val prefs = context.selectedVehicleDataStore.data.first()
        return prefs[SELECTED_VEHICLE_ID_KEY]
    }

    suspend fun clear() {
        context.selectedVehicleDataStore.edit { prefs ->
            prefs.remove(SELECTED_VEHICLE_ID_KEY)
        }
    }
}