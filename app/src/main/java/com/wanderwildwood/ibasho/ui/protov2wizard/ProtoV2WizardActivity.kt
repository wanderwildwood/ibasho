package com.wanderwildwood.ibasho.ui.protov2wizard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.theme.AppTheme

class ProtoV2WizardActivity : FmdActivity() {

    private val viewModel: ProtoV2WizardViewModel by viewModels { ProtoV2WizardViewModel.Factory }

    private lateinit var username: String
    private lateinit var serverUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsRepository.getInstance(this)
        username = settings.get(Settings.SET_FMDSERVER_ID) as String
        serverUrl = settings.get(Settings.SET_FMDSERVER_URL) as String

        setContent {
            AppTheme {
                Scaffold(
                    topBar = { FmdTopAppBar(R.string.proto_v2_wizard_title) { finish() } }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        val state by viewModel.wizardState.observeAsState(WizardState.STEP_1)
                        ScreenForWizardState(state)
                    }
                }
            }
        }
    }

    @Composable
    fun ScreenForWizardState(state: WizardState) {
        when (state) {
            is WizardState.ERROR -> ErrorScreen(state.msg) { finish() }

            is WizardState.STEP_1 -> Step1Screen(username, serverUrl) { password ->
                if (password.isBlank()) {
                    Toast.makeText(this, R.string.pw_change_empty, Toast.LENGTH_LONG).show()
                } else {
                    viewModel.advanceFromStep1(password)
                }
            }

            is WizardState.STEP_1_LOADING -> LoadingScreen()

            is WizardState.STEP_2 -> Step2Screen(
                state.numLocations, state.numPictures,
                onDeleteDataClicked = { viewModel.advanceFromStep2DeleteData() },
                onMigrateDataClicked = { viewModel.advanceFromStep2MigrateData(state.keyPair) },
            )

            is WizardState.STEP_2_LOADING -> LoadingScreen(R.string.proto_v2_wizard_step2_text_migration_in_progress)

            is WizardState.STEP_3 -> Step3Screen { viewModel.advanceFromStep3() }
            is WizardState.FINISH -> finish()
        }
    }

}
