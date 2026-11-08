package com.example.game.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.game.model.RoomInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

class NetworkManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        const val TAG = "NetworkManager"
        const val TCP_PORT = 8888
        const val UDP_DISCOVERY_PORT = 8889
        const val BROADCAST_PREFIX = "STRIKE3D_ROOM"
    }

    private val _discoveredRooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val discoveredRooms: StateFlow<List<RoomInfo>> = _discoveredRooms.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Ready")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val _pingMs = MutableStateFlow(0L)
    val pingMs: StateFlow<Long> = _pingMs.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var socketWriter: PrintWriter? = null
    private var udpBeaconJob: Job? = null
    private var udpDiscoveryJob: Job? = null
    private var connectionJob: Job? = null

    var isHost: Boolean = false
        private set

    /**
     * Get the device's local Wi-Fi IPv4 address
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Focus on wlan or non-loopback interfaces
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.contains(".") && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    /**
     * Start Hosting a Local Wi-Fi Match
     */
    fun startHosting(hostPlayerName: String) {
        stopAll()
        isHost = true
        _connectionStatus.value = "Hosting on port $TCP_PORT... Waiting for friend"

        connectionJob = scope.launch {
            try {
                val server = ServerSocket(TCP_PORT)
                serverSocket = server
                Log.d(TAG, "Server listening on $TCP_PORT")

                // Start UDP Room beacon broadcast so nearby devices automatically discover this room
                startUdpBeacon(hostPlayerName)

                val socket = server.accept()
                clientSocket = socket
                socket.tcpNoDelay = true
                _isConnected.value = true
                _connectionStatus.value = "Connected to ${socket.inetAddress.hostAddress}!"
                Log.d(TAG, "Friend connected: ${socket.inetAddress}")

                // Setup IO streams
                setupSocketStreams(socket)
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Host server error: ${e.message}")
                    _connectionStatus.value = "Host error: ${e.message}"
                    _isConnected.value = false
                }
            }
        }
    }

    /**
     * Broadcast UDP packets on local Wi-Fi so friends can find the room
     */
    private fun startUdpBeacon(hostPlayerName: String) {
        udpBeaconJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val hostIp = getLocalIpAddress()
                val message = "$BROADCAST_PREFIX|$hostPlayerName|$hostIp|$TCP_PORT"
                val sendData = message.toByteArray()

                // Broadcast address
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(sendData, sendData.size, broadcastAddr, UDP_DISCOVERY_PORT)

                while (isActive) {
                    socket.send(packet)
                    delay(1200)
                }
            } catch (e: Exception) {
                Log.w(TAG, "UDP Beacon exception: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    /**
     * Start scanning local Wi-Fi for active rooms
     */
    fun startDiscovery() {
        stopDiscovery()
        _discoveredRooms.value = emptyList()

        udpDiscoveryJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_DISCOVERY_PORT))
                    soTimeout = 3000
                }
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()
                        if (text.startsWith(BROADCAST_PREFIX)) {
                            val parts = text.split("|")
                            if (parts.size >= 4) {
                                val hostName = parts[1]
                                val hostIp = parts[2]
                                val port = parts[3].toIntOrNull() ?: TCP_PORT

                                val room = RoomInfo(
                                    roomName = "$hostName's Match",
                                    hostIp = hostIp,
                                    hostPort = port,
                                    hostPlayerName = hostName
                                )

                                val current = _discoveredRooms.value.toMutableList()
                                if (current.none { it.hostIp == hostIp }) {
                                    current.add(room)
                                    _discoveredRooms.value = current
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // socket timeout or read error, loop continues
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Discovery socket failed: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun stopDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
    }

    /**
     * Connect to a Host IP
     */
    fun connectToHost(hostIp: String, port: Int = TCP_PORT) {
        stopAll()
        isHost = false
        _connectionStatus.value = "Connecting to $hostIp:$port..."

        connectionJob = scope.launch {
            try {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(hostIp, port), 5000)
                clientSocket = socket
                _isConnected.value = true
                _connectionStatus.value = "Connected to host ($hostIp)!"

                setupSocketStreams(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Connect error: ${e.message}")
                _connectionStatus.value = "Connection failed: ${e.message}"
                _isConnected.value = false
            }
        }
    }

    private suspend fun setupSocketStreams(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val writer = PrintWriter(socket.getOutputStream(), true)
            socketWriter = writer
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Launch periodic ping loop
            scope.launch {
                while (isActive && _isConnected.value) {
                    val now = System.currentTimeMillis()
                    sendMessage("PING:$now")
                    delay(2000)
                }
            }

            var line: String?
            while (isActive) {
                line = reader.readLine() ?: break
                if (line.startsWith("PING:")) {
                    val sentTime = line.substringAfter("PING:").toLongOrNull() ?: 0L
                    sendMessage("PONG:$sentTime")
                } else if (line.startsWith("PONG:")) {
                    val sentTime = line.substringAfter("PONG:").toLongOrNull() ?: 0L
                    if (sentTime > 0) {
                        _pingMs.value = System.currentTimeMillis() - sentTime
                    }
                } else {
                    _incomingMessages.tryEmit(line)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket stream read error: ${e.message}")
        } finally {
            _isConnected.value = false
            _connectionStatus.value = "Disconnected"
            socket.close()
        }
    }

    fun sendMessage(msg: String) {
        scope.launch(Dispatchers.IO) {
            try {
                socketWriter?.println(msg)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending msg: ${e.message}")
            }
        }
    }

    fun stopAll() {
        stopDiscovery()
        udpBeaconJob?.cancel()
        udpBeaconJob = null
        connectionJob?.cancel()
        connectionJob = null
        try {
            socketWriter?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignored
        }
        socketWriter = null
        clientSocket = null
        serverSocket = null
        _isConnected.value = false
        _connectionStatus.value = "Idle"
    }
}
