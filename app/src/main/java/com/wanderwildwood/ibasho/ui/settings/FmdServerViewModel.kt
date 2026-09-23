package com.wanderwildwood.ibasho.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wanderwildwood.ibasho.FmdApplication
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.FmdServerRepository

class FmdServerViewModel(
    private val settings: SettingsRepository,
    private val api: FmdServerRepository,
) : ViewModel() {

    companion object {
        // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FmdApplication)
                val settings = SettingsRepository.getInstance(app)
                val api = FmdServerRepository(app)
                FmdServerViewModel(settings, api)
            }
        }
    }

    private val _serverVersion = MutableLiveData<String>("")
    val serverVersion: LiveData<String> = _serverVersion

    fun queryServerVersion() {
        val baseUrl = settings.get(Settings.SET_FMDSERVER_URL) as String
        api.getServerVersion(baseUrl, { response ->
            _serverVersion.postValue(response)
        }, { error ->
            // Return an empty version. The connection status already shows the connection error.
            _serverVersion.postValue("")
        })
    }
}
