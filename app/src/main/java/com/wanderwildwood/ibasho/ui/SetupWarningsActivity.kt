package com.wanderwildwood.ibasho.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.wanderwildwood.ibasho.databinding.ActivitySetupWarningsBinding
import com.wanderwildwood.ibasho.permissions.globalAppPermissions
import com.wanderwildwood.ibasho.permissions.isMissingGlobalAppPermission
import com.wanderwildwood.ibasho.services.ServerConnectivityCheckService
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity
import com.wanderwildwood.ibasho.warnings.isBackgroundRestricted
import com.wanderwildwood.ibasho.warnings.openAppSettings
import com.wanderwildwood.ibasho.warnings.shouldWarnUnifiedPushRequired


class SetupWarningsActivity : FmdActivity() {

    private lateinit var viewBinding: ActivitySetupWarningsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySetupWarningsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(viewBinding.appBar)
        setupEdgeToEdgeScrollView(viewBinding.scrollView)
    }

    override fun onResume() {
        super.onResume()

        // For simplicity, we always show all warnings/recommendations.
        // Easier for developers (no big if-else tree), and
        // more transparent for users (why did this suddenly disappear?).
        setupRecommPush(this)
        setupRecommConnectivity(this)
        setupWarnBackgroundRestricted(this)
        setupPermissionsList(
            this,
            viewBinding.permissionsRequiredTitle,
            viewBinding.permissionsRequiredList,
            globalAppPermissions()
        )
    }

    private fun setupRecommPush(context: Context) {
        val shouldNudge = shouldWarnUnifiedPushRequired(context)

        viewBinding.push.icCheck.isVisible = !shouldNudge
        viewBinding.push.button.isVisible = shouldNudge

        viewBinding.push.button.setOnClickListener {
            val intent = Intent(this, FMDServerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupWarnBackgroundRestricted(context: Context) {
        val shouldNudge = isBackgroundRestricted(context)

        viewBinding.backgroundRestricted.icCheck.isVisible = !shouldNudge
        viewBinding.backgroundRestricted.backgroundRestrictedButton.isVisible = shouldNudge

        viewBinding.backgroundRestricted.backgroundRestrictedButton.setOnClickListener {
            openAppSettings(context)
        }
    }

    private fun setupRecommConnectivity(context: Context) {
        val shouldNudge =
            ServerConnectivityCheckService.shouldNudgeAboutConnectivityCheck(context)

        viewBinding.connectivity.icCheck.isVisible = !shouldNudge
        viewBinding.connectivity.recommendationConnCheckEnableButton.isVisible = shouldNudge

        viewBinding.connectivity.recommendationConnCheckEnableButton.setOnClickListener {
            val intent = Intent(this, FMDServerActivity::class.java)
            startActivity(intent)
        }
    }
}

fun shouldShowSetupWarnings(context: Context): Boolean {
    return ServerConnectivityCheckService.shouldNudgeAboutConnectivityCheck(context)
            || isMissingGlobalAppPermission(context)
            // Nothing works at all in this state, so it earns the warning triangle.
            || isBackgroundRestricted(context)
}
