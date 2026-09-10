package com.wanderwildwood.ibasho.transports

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.AccessResponse
import com.wanderwildwood.ibasho.commands.ParserResult
import com.wanderwildwood.ibasho.commands.hasPermission
import com.wanderwildwood.ibasho.data.FmdLocation
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.FmdServerRepository
import com.wanderwildwood.ibasho.permissions.Permission
import com.wanderwildwood.ibasho.ui.settings.AddAccountActivity
import com.wanderwildwood.ibasho.utils.log


class FmdServerTransport(
    val context: Context,
    private val destination: String,
) : Transport<Unit>(Unit) {

    constructor(context: Context) : this(context, "FMD Server")

    companion object {
        private val TAG = FmdServerTransport::class.simpleName
    }

    private val repo = FmdServerRepository(context).getApiService()
    private val settings = SettingsRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_cloud

    @get:StringRes
    override val title = R.string.transport_fmd_server_title

    override val description = context.getString(R.string.transport_fmd_server_description)

    override val descriptionAuth = context.getString(R.string.transport_fmd_server_description_auth)

    override val descriptionNote = context.getString(R.string.transport_fmd_server_description_note)

    override val requiredPermissions = emptyList<Permission>()

    override val actions = listOf(TransportAction(R.string.Settings_Settings) { activity ->
        activity.startActivity(Intent(context, AddAccountActivity::class.java))
    })

    override fun getDestinationString() = destination

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val url = (settings.get(Settings.SET_FMDSERVER_URL) as String).toUri()
        val grantedPerms = (settings.get(Settings.SET_FMDSERVER_PERMISSIONS) as Number).toLong()

        val hasPermission = grantedPerms.hasPermission(parsed.command.permission)
        context.log().i(
            TAG,
            "Server account '$username@${url.host}' granted access to '${parsed.command.keyword}': $hasPermission"
        )
        return if (hasPermission) AccessResponse.ALLOWED else AccessResponse.DENIED_EXISTS
    }

    @SuppressLint("MissingSuperCall")
    override fun send(context: Context, msg: String) {
        //super.send(context, msg, destination)
        // not implemented for FMD Server
    }

    override fun sendNewLocation(context: Context, location: FmdLocation) {
        // no call to super(), we need to completely replace this for FMD Server
        settings.set(Settings.SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME, location.timeMillis)
        repo.sendLocation(location)
    }
}
