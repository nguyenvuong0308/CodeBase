package com.core.utilities.manager

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.StrictMode
import com.core.utilities.util.Timber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

fun Context.isNetworkConnected(): Boolean {
    var result = false
    val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
    cm?.run {
        cm.getNetworkCapabilities(cm.activeNetwork)
            ?.run {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            }
    }
    Timber.d("isNetworkConnected $result cm $cm")
    return result
}

private fun hasInternetAccess(context: Context): Boolean {
    return if (context.isNetworkConnected()) {
        try {
            val timeout = 1500
            val executor: ExecutorService = Executors.newCachedThreadPool()
            val task: Callable<Boolean> = Callable<Boolean> {
                val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder()
                    .permitAll()
                    .build()
                StrictMode.setThreadPolicy(policy)

                val httpURLConnection: HttpURLConnection = URL("https://www.google.com").openConnection() as HttpURLConnection
                httpURLConnection.setRequestProperty("User-Agent", "Android")
                httpURLConnection.setRequestProperty("Connection", "close")
                httpURLConnection.requestMethod = "GET"
                httpURLConnection.connectTimeout = timeout
                httpURLConnection.readTimeout = timeout
                httpURLConnection.connect()
                httpURLConnection.responseCode == 200
            }
            val future: Future<Boolean> = executor.submit(task)
            return future.get(timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}

fun Context.hasInternetAccessCheck(doTask: () -> Unit, doException: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        when {
            hasInternetAccess(this@hasInternetAccessCheck) -> withContext(Dispatchers.Main) {
                doTask()
            }
            else -> withContext(Dispatchers.Main) {
                doException()
            }
        }
    }
}

@Singleton
class NetworkConnectionManager @Inject constructor(
    @ApplicationContext context: Context,
)  {

    private val connectivityManager: ConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE)
            as ConnectivityManager

    private val networkCallback = NetworkCallback()

    private val _currentNetwork = MutableStateFlow(provideDefaultCurrentNetwork())

    private val coroutineScope = CoroutineScope(SupervisorJob())

    val isNetworkConnectedFlow: StateFlow<Boolean> =
        _currentNetwork
            .map { it.isConnected() }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = _currentNetwork.value.isConnected()
            )

    /**
     * Whether Android currently has an active Wi-Fi, cellular, Ethernet, or VPN transport.
     *
     * Unlike [isNetworkConnectedFlow], this does not require the network to be validated or
     * capable of reaching the Internet. It is intended for flows which only need to react to the
     * user enabling/disabling a network transport.
     */
    val isNetworkAvailableFlow: StateFlow<Boolean> =
        _currentNetwork
            .map { it.hasActiveTransport() }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = _currentNetwork.value.hasActiveTransport()
            )

    val isNetworkConnected: Boolean
        get() = isNetworkConnectedFlow.value

    val isNetworkAvailable: Boolean
        get() = _currentNetwork.value.hasActiveTransport()

    init {
        startListenNetworkState()
    }

    private fun startListenNetworkState() {
        if (_currentNetwork.value.isListening) {
            return
        }

        // Reset state before start listening
        _currentNetwork.update {
            provideDefaultCurrentNetwork()
                .copy(isListening = true)
        }
        Timber.d(
            "[NetworkAvailability] start available=${_currentNetwork.value.hasActiveTransport()} " +
                    "connected=${_currentNetwork.value.isConnected()}"
        )
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            connectivityManager.registerBestMatchingNetworkCallback(NetworkRequest.Builder().build())
//        } else {
//        }
    }

    fun stopListenNetworkState() {
        if (!_currentNetwork.value.isListening) {
            return
        }

        _currentNetwork.update {
            it.copy(isListening = false)
        }

        connectivityManager.unregisterNetworkCallback(networkCallback)
    }


    private inner class NetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _currentNetwork.update {
                it.copy(
                    network = network,
                    isAvailable = true,
                    networkCapabilities = if (it.network == network) {
                        it.networkCapabilities
                    } else {
                        null
                    }
                )
            }
            Timber.d("[NetworkAvailability] onAvailable network=$network")
        }

        override fun onLost(network: Network) {
            _currentNetwork.update {
                if (it.network != network) {
                    it
                } else {
                    it.copy(
                        network = null,
                        isAvailable = false,
                        networkCapabilities = null
                    )
                }
            }
            Timber.d("[NetworkAvailability] onLost network=$network")
        }

        override fun onUnavailable() {
            _currentNetwork.update {
                it.copy(
                    network = null,
                    isAvailable = false,
                    networkCapabilities = null
                )
            }
            Timber.d("[NetworkAvailability] onUnavailable")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            _currentNetwork.update {
                if (it.network != null && it.network != network) {
                    it
                } else {
                    it.copy(
                        network = network,
                        isAvailable = true,
                        networkCapabilities = networkCapabilities
                    )
                }
            }
            Timber.d(
                "[NetworkAvailability] onCapabilitiesChanged network=$network " +
                        "activeTransport=${networkCapabilities.hasSupportedTransport()}"
            )
        }

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            _currentNetwork.update {
                if (it.network == network) it.copy(isBlocked = blocked) else it
            }
            Timber.d("[NetworkAvailability] onBlockedStatusChanged network=$network blocked=$blocked")
        }
    }

    /**
     * On Android 9, [ConnectivityManager.NetworkCallback.onBlockedStatusChanged] is not called when
     * we call the [ConnectivityManager.registerDefaultNetworkCallback] function.
     * Hence we assume that the network is unblocked by default.
     */
    private fun provideDefaultCurrentNetwork(): CurrentNetwork {
        val activeNetwork = connectivityManager.activeNetwork
        return CurrentNetwork(
            isListening = false,
            network = activeNetwork,
            networkCapabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities),
            isAvailable = activeNetwork != null,
            isBlocked = false
        )
    }

    private data class CurrentNetwork(
        val isListening: Boolean,
        val network: Network?,
        val networkCapabilities: NetworkCapabilities?,
        val isAvailable: Boolean,
        val isBlocked: Boolean
    )

    private fun CurrentNetwork.isConnected(): Boolean {
        // Since we don't know the network state if NetworkCallback is not registered.
        // We assume that it's disconnected.
        return isListening &&
                isAvailable &&
                !isBlocked &&
                networkCapabilities.isNetworkCapabilitiesValid()
    }

    private fun CurrentNetwork.hasActiveTransport(): Boolean {
        return isListening && isAvailable && networkCapabilities.hasSupportedTransport()
    }

    private fun NetworkCapabilities?.hasSupportedTransport(): Boolean = when {
        this == null -> false
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }

    private fun NetworkCapabilities?.isNetworkCapabilitiesValid(): Boolean = when {
        this == null -> false
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                hasSupportedTransport() -> true
        else -> false
    }
}
