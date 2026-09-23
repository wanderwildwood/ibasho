package com.wanderwildwood.ibasho.ui.protov2wizard

import java.security.KeyPair

sealed class WizardState {

    data class ERROR(val msg: String) : WizardState()

    object STEP_1 : WizardState()

    object STEP_1_LOADING : WizardState()

    data class STEP_2(
        val numLocations: Int,
        val numPictures: Int,
        val keyPair: KeyPair,
    ) : WizardState()

    object STEP_2_LOADING : WizardState()

    object STEP_3 : WizardState()

    object FINISH : WizardState()
}
