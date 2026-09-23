package com.wanderwildwood.ibasho.ui.protov2wizard


import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wanderwildwood.ibasho.FmdApplication
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.crypto.CryptoV2
import com.wanderwildwood.ibasho.crypto.LongTermKeys
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_MASTER_KEY
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_PASSWORD_KEY
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.FMD_SERVER_PROTO_V2
import com.wanderwildwood.ibasho.net.FmdServerApiV1RepoSpec
import com.wanderwildwood.ibasho.net.FmdServerApiV1Repository
import com.wanderwildwood.ibasho.net.FmdServerApiV2RepoSpec
import com.wanderwildwood.ibasho.net.FmdServerApiV2Repository
import com.wanderwildwood.ibasho.utils.CypherUtils
import com.wanderwildwood.ibasho.utils.encodeBase64
import com.wanderwildwood.ibasho.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.KeyPair

class ProtoV2WizardViewModel(
    private val context: Application,
    private val settings: SettingsRepository,
    private val encryptedSettings: EncryptedSettingsRepository,
    private val apiV1: FmdServerApiV1Repository,
    private val apiV2: FmdServerApiV2Repository,
) : ViewModel() {

    companion object {
        val TAG = ProtoV2WizardViewModel::class.simpleName

        // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FmdApplication)
                val settings = SettingsRepository.getInstance(app)
                val encryptedSettings = EncryptedSettingsRepository.getInstance(app)
                val apiV1 = FmdServerApiV1Repository.getInstance(FmdServerApiV1RepoSpec(app))
                val apiV2 = FmdServerApiV2Repository.getInstance(FmdServerApiV2RepoSpec(app))
                ProtoV2WizardViewModel(app, settings, encryptedSettings, apiV1, apiV2)
            }
        }
    }

    private val _wizardState = MutableLiveData<WizardState>(WizardState.STEP_1)
    val wizardState: LiveData<WizardState> = _wizardState

    fun advanceFromStep1(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _wizardState.postValue(WizardState.STEP_1_LOADING)

        // Hash password and decrypt v1 private key
        // Same as FmdServerApiV1Repository::changePassword
        context.log().d(TAG, "Decrypting v1 key")
        val encryptedPrivKey = settings.get(Settings.SET_FMD_CRYPT_PRIVKEY) as String
        var keyPair: KeyPair? = null
        try {
            keyPair = CypherUtils.decryptPrivateKeyWithPassword(encryptedPrivKey, password)
        } catch (e: Exception) {
            _wizardState.postValue(WizardState.ERROR(e.stackTraceToString()))
            return@launch
        }

        if (keyPair == null) {
            val msg = context.getString(R.string.pw_change_wrong_password)
            _wizardState.postValue(WizardState.ERROR(msg))
            return@launch
        }

        // Generate v2 keys
        // Same as FmdServerApiV2Repository::register
        context.log().d(TAG, "Generating v2 key")
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val hashResult = CryptoV2.hashPassword(username, password)
        val ltk = LongTermKeys.generate(username)
        encryptedSettings.putString(KEY_FMDSERVER_V2_MASTER_KEY, ltk.masterKey.encodeBase64())
        encryptedSettings.putString(
            KEY_FMDSERVER_V2_PASSWORD_KEY, hashResult.passwordKey.encodeBase64()
        )
        apiV2.initServicePub() // reload v2 keys from encryptedSettings

        // Upload v2 keys (change password)
        context.log().d(TAG, "Uploading v2 key")
        apiV2.changePassword(password, password, {
            // Critical part complete, v2 keys uploaded
            settings.set(Settings.SET_FMD_CRYPT_PROTO, FMD_SERVER_PROTO_V2)

            // Get old v1 location/picture sizes for Step 2
            context.log().d(TAG, "Getting v1 location size")
            apiV1.getLocationSize({ numLocations ->

                context.log().d(TAG, "Getting v1 picture size")
                apiV1.getPictureSize(
                    { numPictures ->
                        // Step 1 complete, proceed to either step 2 or 3
                        if (numLocations == 0 && numPictures == 0) {
                            _wizardState.postValue(WizardState.STEP_3)
                        } else {
                            _wizardState.postValue(
                                WizardState.STEP_2(numLocations, numPictures, keyPair)
                            )
                        }
                    },
                    { _wizardState.postValue(WizardState.ERROR(it.message)) },
                )
            }, {
                _wizardState.postValue(WizardState.ERROR(it.message))
            })

        }, {
            _wizardState.postValue(WizardState.ERROR(it.message))
        })

    }

    fun advanceFromStep2DeleteData() {
        _wizardState.postValue(WizardState.STEP_2_LOADING)

        context.log().d(TAG, "Deleting v1 locations")
        apiV1.deleteAllLocations({

            context.log().d(TAG, "Deleting v1 pictures")
            apiV1.deleteAllPictures(
                {
                    // Wizard complete
                    _wizardState.postValue(WizardState.STEP_3)
                },
                { _wizardState.postValue(WizardState.ERROR(it.message)) },
            )
        }, {
            _wizardState.postValue(WizardState.ERROR(it.message))
        })
    }

    fun advanceFromStep2MigrateData(keyPair: KeyPair) {
        _wizardState.postValue(WizardState.STEP_2_LOADING)

        // Download the data via APIv1 and re-upload it via APIv2
        context.log().d(TAG, "Downloading v1 locations")
        apiV1.getAllLocations(
            keyPair.private,
            { locations ->
                // Each upload is waited for before the next step. Upstream sent them without
                // waiting and went straight on to delete the v1 copies, so a failed upload
                // lost the history. Now a failure stops here with the v1 data still in place.
                context.log().d(TAG, "Uploading v2 locations")
                apiV2.sendLocations(locations, {

                    context.log().d(TAG, "Downloading v1 pictures")
                    apiV1.getAllPictures(
                        keyPair.private,
                        { pictures ->
                            context.log().d(TAG, "Uploading v2 pictures")
                            apiV2.sendPictures(pictures, {
                                // Delete the data
                                advanceFromStep2DeleteData()
                            }, { _wizardState.postValue(WizardState.ERROR(it.message)) })
                        },
                        { _wizardState.postValue(WizardState.ERROR(it.message)) },
                    )
                }, { _wizardState.postValue(WizardState.ERROR(it.message)) })
            },
            { _wizardState.postValue(WizardState.ERROR(it.message)) },
        )
    }

    fun advanceFromStep3() {
        // Remove the crypto v1 data
        context.log().d(TAG, "Removing v1 settings")
        settings.remove(Settings.SET_FMD_CRYPT_PUBKEY)
        settings.remove(Settings.SET_FMD_CRYPT_PRIVKEY)
        settings.remove(Settings.SET_FMD_CRYPT_HPW)

        _wizardState.postValue(WizardState.FINISH)
    }
}
