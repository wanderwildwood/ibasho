package com.wanderwildwood.ibasho.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.databinding.ActivitySetupWarningsBinding
import com.wanderwildwood.ibasho.permissions.globalAppPermissions
import com.wanderwildwood.ibasho.permissions.isMissingGlobalAppPermission
import com.wanderwildwood.ibasho.services.ServerConnectivityCheckService
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity
import com.wanderwildwood.ibasho.warnings.UploadStall
import com.wanderwildwood.ibasho.warnings.isBackgroundRestricted
import com.wanderwildwood.ibasho.warnings.isMuditaKompakt
import com.wanderwildwood.ibasho.warnings.openKompaktHelp
import com.wanderwildwood.ibasho.warnings.openAppSettings
import com.wanderwildwood.ibasho.warnings.shouldWarnUnifiedPushRequired
import com.wanderwildwood.ibasho.warnings.uploadStall
import java.text.DateFormat


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
        setupWarnUploadStall(this)
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

        // A Kompakt's App info has no Battery page, so "Open settings" would lead nowhere.
        if (isMuditaKompakt()) {
            viewBinding.backgroundRestricted.backgroundRestrictedText.setText(
                R.string.background_restricted_text_kompakt
            )
            viewBinding.backgroundRestricted.backgroundRestrictedButton.setText(
                R.string.kompakt_help_button
            )
        }
        viewBinding.backgroundRestricted.backgroundRestrictedButton.setOnClickListener {
            if (isMuditaKompakt()) openKompaktHelp(context) else openAppSettings(context)
        }
    }

    private fun setupWarnUploadStall(context: Context) {
        val row = viewBinding.uploadStall
        val stall = uploadStall(context)
        val settings = SettingsRepository.getInstance(context)

        // "Since when": the last location that arrived, or failing that, when the job last ran.
        val lastUpload =
            (settings.get(Settings.SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME) as Number).toLong()
        val lastJob =
            (settings.get(Settings.SET_FMDSERVER_LAST_UPLOAD_JOB_MILLIS) as Number).toLong()
        val since = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(if (lastUpload > 0L) lastUpload else lastJob)

        row.uploadStallText.text = when (stall) {
            UploadStall.NONE -> getString(
                if (settings.serverAccountExists()) R.string.upload_stall_ok
                else R.string.upload_stall_off
            )
            UploadStall.NOT_RUNNING -> getString(
                if (isMuditaKompakt()) R.string.upload_stall_not_running_kompakt
                else R.string.upload_stall_not_running,
                since
            )
            UploadStall.NO_FIX -> getString(R.string.upload_stall_no_fix, since)
        }

        row.icCheck.isVisible = stall == UploadStall.NONE
        row.uploadStallButton.isVisible = stall != UploadStall.NONE
        row.uploadStallButton.setText(
            if (stall == UploadStall.NOT_RUNNING && isMuditaKompakt()) R.string.kompakt_help_button
            else R.string.background_restricted_button
        )
        row.uploadStallButton.setOnClickListener {
            when {
                stall == UploadStall.NOT_RUNNING && isMuditaKompakt() -> openKompaktHelp(context)
                stall == UploadStall.NOT_RUNNING -> openAppSettings(context)
                // No fix: the location sources are chosen on the server screen.
                else -> startActivity(Intent(this, FMDServerActivity::class.java))
            }
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
            // Neither is visible from the server, which just stops hearing from the phone.
            || uploadStall(context) != UploadStall.NONE
}
