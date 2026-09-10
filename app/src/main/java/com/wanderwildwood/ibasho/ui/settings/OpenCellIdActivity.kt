package com.wanderwildwood.ibasho.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.databinding.ActivityOpenCellIdBinding
import com.wanderwildwood.ibasho.net.OpenCelliDRepository
import com.wanderwildwood.ibasho.net.OpenCelliDSpec
import com.wanderwildwood.ibasho.permissions.LocationPermission
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.wanderwildwood.ibasho.utils.CellParameters
import com.wanderwildwood.ibasho.utils.Utils.Companion.getGeoURI
import com.wanderwildwood.ibasho.utils.Utils.Companion.getOpenStreetMapLink
import com.wanderwildwood.ibasho.utils.Utils.Companion.openUrl
import com.wanderwildwood.ibasho.utils.Utils.Companion.pasteFromClipboard
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.utils.requestCellInfo


class OpenCellIdActivity : FmdActivity(), TextWatcher {

    private lateinit var viewBinding: ActivityOpenCellIdBinding

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityOpenCellIdBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        settings = SettingsRepository.getInstance(this)
        val apiToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        viewBinding.editTextOpenCellIDAPIKey.setText(apiToken)
        viewBinding.editTextOpenCellIDAPIKey.addTextChangedListener(this)


        viewBinding.buttonBeaconDbWebsite.setOnClickListener({
            openUrl(it.context, "https://beacondb.net/")
        })
        viewBinding.buttonOpenOpenCellIdWebsite.setOnClickListener({
            openUrl(it.context, "https://opencellid.org/")
        })
        viewBinding.buttonOpenFmdWebsite.setOnClickListener({
            openUrl(it.context, "https://fmd-foss.org/docs/fmd-android/locating#crowdsource")
        })

        viewBinding.buttonPaste.setOnClickListener(::onPasteClicked)
        viewBinding.buttonTestOpenCellId.setOnClickListener(::onTestConnectionClicked)

        setupTestConnection(apiToken.isEmpty())
    }

    private fun setupTestConnection(isApiTokenEmpty: Boolean) {
        if (isApiTokenEmpty) {
            viewBinding.buttonTestOpenCellId.isEnabled = false
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.GONE
        } else {
            viewBinding.buttonTestOpenCellId.isEnabled = true
            viewBinding.textViewTestOpenCellIdResponse.text = ""
            viewBinding.textViewTestOpenCellIdResponse.visibility = View.VISIBLE
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(edited: Editable) {
        if (edited === viewBinding.editTextOpenCellIDAPIKey.text) {
            val newToken = edited.toString().trim()
            settings.set(Settings.SET_OPENCELLID_API_KEY, newToken)
            setupTestConnection(newToken.isEmpty())
        }
    }

    private fun onPasteClicked(view: View) {
        viewBinding.editTextOpenCellIDAPIKey.setText(pasteFromClipboard(view.context))
    }

    private fun onTestConnectionClicked(view: View) {
        val context = view.context

        val permission = LocationPermission()
        if (!permission.isGranted(context)) {
            permission.request(this)
            return
        }

        @SuppressLint("MissingPermission") // ACCESS_FINE_LOCATION
        requestCellInfo(context, this::onCellInfoUpdate)
    }

    private fun onCellInfoUpdate(paras: List<CellParameters>) {
        val context = this
        if (paras.isEmpty()) {
            context.log().i(TAG, "No cell location found")
            viewBinding.textViewTestOpenCellIdResponse.text =
                context.getString(R.string.OpenCellId_test_no_connection)
            return
        }

        val repo = OpenCelliDRepository.getInstance(OpenCelliDSpec(context))
        val apiAccessToken = settings.get(Settings.SET_OPENCELLID_API_KEY) as String

        viewBinding.textViewTestOpenCellIdResponse.text = ""

        paras.forEach {
            queryOpenCelliD(it, repo, apiAccessToken)
        }
    }

    private fun queryOpenCelliD(
        paras: CellParameters,
        repo: OpenCelliDRepository,
        apiAccessToken: String
    ) {
        repo.getCellLocation(
            paras, apiAccessToken,
            onSuccess = {
                val geoURI = getGeoURI(it.lat, it.lon)
                val osm = getOpenStreetMapLink(it.lat, it.lon)
                append(
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n${geoURI}\nOpenStreetMap: $osm"
                )
            },
            onError = {
                append(
                    "Paras: $paras\n\nOpenCelliD: ${it.url}\n\nError: ${it.error}"
                )
            },
        )
    }

    @SuppressLint("SetTextI18n")
    private fun append(string: String) {
        val old = viewBinding.textViewTestOpenCellIdResponse.text.toString()
        viewBinding.textViewTestOpenCellIdResponse.text = old + "\n\n" + string
    }

    companion object {
        private val TAG = OpenCellIdActivity::class.simpleName
    }
}
