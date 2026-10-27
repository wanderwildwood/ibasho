package com.wanderwildwood.ibasho.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

data class NetworkInfo(
    val interfaceName: String,
    val linkAddresses: List<String>,
) {
    override fun toString(): String {
        return "Interface: $interfaceName\n" + linkAddresses.joinToString("\n")
    }
}

object NetworkUtils {

    // https://developer.android.com/develop/connectivity/network-ops/reading-network-state
    fun getIps(context: Context): List<NetworkInfo> {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        @Suppress("Deprecation")
        return connectivityManager.allNetworks
            .map { network -> connectivityManager.getLinkProperties(network) }
            .filterNotNull()
            .map { linkProps ->
                NetworkInfo(
                    linkProps.interfaceName ?: "",
                    linkProps.linkAddresses.map { addr -> addr.toString() }.toList()
                )
            }
            .toList()
    }

    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        @Suppress("Deprecation")
        return connectivityManager.allNetworks.any { network ->
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return@any false

            // Exclude VPN networks, because they still have the INTERNET, VALIDATED capabilities,
            // even after the device transitioned into airplane mode.
            // Check the underlying physical networks instead.
            // See https://gitlab.com/fmd-foss/fmd-android/-/work_items/448
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
