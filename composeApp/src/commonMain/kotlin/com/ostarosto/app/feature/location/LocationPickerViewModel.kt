package com.ostarosto.app.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.repository.DeliveryRepository
import com.ostarosto.app.domain.model.DeliveryAvailability
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationPickerUiState(
    val centerLatitude: Double = DefaultCenter.LAT,
    val centerLongitude: Double = DefaultCenter.LNG,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasPinned: Boolean = false,
    val checking: Boolean = false,
    val availability: DeliveryAvailability? = null,
    val error: String? = null,
)

/** Cairo — used only as a fallback map center when the selected branch has no coordinates. */
private object DefaultCenter {
    const val LAT = 30.0444
    const val LNG = 31.2357
}

class LocationPickerViewModel(
    private val selection: SelectionStore,
    private val deliveryRepo: DeliveryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<LocationPickerUiState> = _state.asStateFlow()

    private var checkJob: Job? = null

    private fun initialState(): LocationPickerUiState {
        val branch = selection.branch.value
        val lat = branch?.latitude
        val lng = branch?.longitude
        return if (lat != null && lng != null) {
            LocationPickerUiState(centerLatitude = lat, centerLongitude = lng)
        } else {
            LocationPickerUiState()
        }
    }

    /** Debounced so dragging the pin doesn't fire a request per pixel. */
    fun onPinMoved(lat: Double, lng: Double) {
        _state.update { it.copy(latitude = lat, longitude = lng, hasPinned = true, checking = true, error = null) }
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            delay(500)
            when (val r = deliveryRepo.check(lat, lng, selection.branchRef)) {
                is ApiResult.Success -> _state.update { it.copy(checking = false, availability = r.value, error = null) }
                is ApiResult.HttpError -> _state.update { it.copy(checking = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(checking = false, error = r.cause.message) }
            }
        }
    }
}
