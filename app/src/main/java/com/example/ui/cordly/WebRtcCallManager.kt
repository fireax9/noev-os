package com.example.ui.cordly

import android.content.Context
import android.util.Log
import com.example.data.model.CallSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time peer-to-peer WebRTC audio/video engine using Google STUN + lean Firestore SDP/ICE signaling.
 * Eliminates multi-minute Firestore Base64 queuing lag and uses hardware AEC/NS for crystal-clear speech.
 */
class WebRtcCallManager(
    context: Context,
    private val currentUserId: String,
    private val onSendSignal: (
        recipientId: String,
        type: String,
        sdp: String?,
        candidate: String?,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ) -> Unit,
    private val onConsumeSignal: (signalId: String) -> Unit
) {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val released = AtomicBoolean(false)

    val eglBase: EglBase = EglBase.create()

    private val audioDeviceModule: JavaAudioDeviceModule?
    private val factory: PeerConnectionFactory

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrackImpl: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val pendingIceByPeer = ConcurrentHashMap<String, MutableList<IceCandidate>>()
    private val remoteDescSetPeers = ConcurrentHashMap<String, Boolean>()
    private val offeredPeers = ConcurrentHashMap<String, Boolean>()
    private val processedSignalIds = ConcurrentHashMap.newKeySet<String>()
    private val connectedPeers = ConcurrentHashMap.newKeySet<String>()

    private var currentPreset: VideoQualityPreset = VideoQualityPreset.Q480P
    private var cameraStarted = false
    private var audioStarted = false
    private var currentMuted = false
    private var currentVideoEnabled = false

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTracks = MutableStateFlow<Map<String, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<String, VideoTrack>> = _remoteVideoTracks.asStateFlow()

    private val _isUsingFrontCamera = MutableStateFlow(true)
    val isUsingFrontCamera: StateFlow<Boolean> = _isUsingFrontCamera.asStateFlow()

    private val _connectedPeerCount = MutableStateFlow(0)
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount.asStateFlow()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    init {
        ensureFactoryInitialized(appContext)
        audioDeviceModule = try {
            JavaAudioDeviceModule.builder(appContext)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()
        } catch (t: Throwable) {
            Log.w(TAG, "JavaAudioDeviceModule fallback", t)
            null
        }
        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
        if (audioDeviceModule != null) {
            builder.setAudioDeviceModule(audioDeviceModule)
        }
        factory = builder.createPeerConnectionFactory()
    }

    fun startOrUpdateLocalMedia(
        hasAudioPermission: Boolean,
        hasCameraPermission: Boolean,
        videoEnabled: Boolean,
        muted: Boolean,
        preset: VideoQualityPreset
    ) {
        if (released.get()) return
        executor.execute {
            if (released.get()) return@execute
            currentMuted = muted
            currentVideoEnabled = videoEnabled && hasCameraPermission
            val qualityChanged = currentPreset != preset
            currentPreset = preset

            if (hasAudioPermission && !audioStarted) {
                startLocalAudioInternal()
            }
            localAudioTrack?.setEnabled(!muted)

            if (hasCameraPermission && videoEnabled && !cameraStarted) {
                startLocalCameraInternal(preset)
            }
            localVideoTrackImpl?.setEnabled(currentVideoEnabled)

            if (qualityChanged && cameraStarted) {
                applyVideoQualityInternal(preset)
            }
        }
    }

    fun setMuted(muted: Boolean) {
        if (released.get()) return
        executor.execute {
            currentMuted = muted
            localAudioTrack?.setEnabled(!muted)
        }
    }

    fun setVideoEnabled(enabled: Boolean, hasCameraPermission: Boolean) {
        if (released.get()) return
        executor.execute {
            currentVideoEnabled = enabled && hasCameraPermission
            if (currentVideoEnabled && !cameraStarted) {
                startLocalCameraInternal(currentPreset)
            }
            localVideoTrackImpl?.setEnabled(currentVideoEnabled)
        }
    }

    fun switchCamera() {
        if (released.get()) return
        executor.execute {
            val capturer = videoCapturer ?: return@execute
            try {
                capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                        _isUsingFrontCamera.value = isFrontCamera
                    }

                    override fun onCameraSwitchError(errorDescription: String?) {
                        Log.w(TAG, "Camera switch error: $errorDescription")
                    }
                })
            } catch (t: Throwable) {
                Log.w(TAG, "switchCamera failed", t)
            }
        }
    }

    fun applyVideoQuality(preset: VideoQualityPreset) {
        if (released.get()) return
        executor.execute {
            currentPreset = preset
            applyVideoQualityInternal(preset)
        }
    }

    fun ensurePeersAndConnect(
        callStatus: String,
        participantIds: List<String>
    ) {
        if (released.get() || callStatus != "active") return
        val remotePeers = participantIds.filter { it.isNotBlank() && it != currentUserId }.distinct()
        executor.execute {
            if (released.get()) return@execute
            remotePeers.forEach { peerId ->
                val pc = getOrCreatePeerConnection(peerId) ?: return@forEach
                // Deterministic tie-breaker: lower UID initiates the SDP offer so both sides never race.
                if (currentUserId < peerId && offeredPeers[peerId] != true) {
                    offeredPeers[peerId] = true
                    createAndSendOffer(peerId, pc)
                }
            }
        }
    }

    fun handleIncomingSignals(signals: List<CallSignal>) {
        if (released.get() || signals.isEmpty()) return
        signals.forEach { signal ->
            val sigId = signal.id
            if (sigId.isNotBlank() && !processedSignalIds.add(sigId)) {
                return@forEach
            }
            executor.execute {
                if (released.get()) return@execute
                processSignalInternal(signal)
            }
            if (sigId.isNotBlank()) {
                onConsumeSignal(sigId)
            }
        }
    }

    private fun startLocalAudioInternal() {
        if (audioStarted) return
        try {
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }
            val src = factory.createAudioSource(audioConstraints)
            val track = factory.createAudioTrack("NEOV_AUDIO_0", src)
            track.setEnabled(!currentMuted)
            audioSource = src
            localAudioTrack = track
            audioStarted = true

            peerConnections.values.forEach { pc ->
                try {
                    pc.addTrack(track, listOf("NEOV_STREAM"))
                } catch (_: Exception) {
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start local audio track", t)
        }
    }

    private fun startLocalCameraInternal(preset: VideoQualityPreset) {
        if (cameraStarted) return
        try {
            val enumerator = Camera2Enumerator(appContext)
            val capturer = createCapturer(enumerator) ?: return
            val helper = SurfaceTextureHelper.create("NeovCaptureThread", eglBase.eglBaseContext)
            val src = factory.createVideoSource(capturer.isScreencast)
            capturer.initialize(helper, appContext, src.capturerObserver)
            capturer.startCapture(preset.captureWidth, preset.captureHeight, preset.captureFps)
            val track = factory.createVideoTrack("NEOV_VIDEO_0", src)
            track.setEnabled(currentVideoEnabled)

            videoCapturer = capturer
            surfaceTextureHelper = helper
            videoSource = src
            localVideoTrackImpl = track
            _localVideoTrack.value = track
            cameraStarted = true

            peerConnections.forEach { (peerId, pc) ->
                try {
                    pc.addTrack(track, listOf("NEOV_STREAM"))
                    if (currentUserId < peerId) {
                        createAndSendOffer(peerId, pc)
                    }
                } catch (_: Exception) {
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start local camera", t)
        }
    }

    private fun createCapturer(enumerator: CameraEnumerator): CameraVideoCapturer? {
        val names = enumerator.deviceNames
        names.firstOrNull { enumerator.isFrontFacing(it) }?.let { name ->
            enumerator.createCapturer(name, null)?.let {
                _isUsingFrontCamera.value = true
                return it
            }
        }
        names.firstOrNull()?.let { name ->
            enumerator.createCapturer(name, null)?.let {
                _isUsingFrontCamera.value = false
                return it
            }
        }
        return null
    }

    private fun applyVideoQualityInternal(preset: VideoQualityPreset) {
        try {
            videoCapturer?.changeCaptureFormat(preset.captureWidth, preset.captureHeight, preset.captureFps)
        } catch (t: Throwable) {
            Log.w(TAG, "changeCaptureFormat fallback for ${preset.id}", t)
        }
        peerConnections.values.forEach { pc ->
            try {
                pc.senders.forEach { sender ->
                    if (sender.track()?.kind() == "video") {
                        val params = sender.parameters
                        params.encodings.forEach { enc ->
                            enc.maxBitrateBps = preset.maxBitrateBps
                            enc.maxFramerate = preset.captureFps
                        }
                        sender.parameters = params
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun getOrCreatePeerConnection(peerId: String): PeerConnection? {
        peerConnections[peerId]?.let { return it }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED ||
                    state == PeerConnection.IceConnectionState.COMPLETED
                ) {
                    connectedPeers.add(peerId)
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                    state == PeerConnection.IceConnectionState.FAILED ||
                    state == PeerConnection.IceConnectionState.CLOSED
                ) {
                    connectedPeers.remove(peerId)
                }
                _connectedPeerCount.value = connectedPeers.size
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null || released.get()) return
                onSendSignal(
                    peerId,
                    "ice",
                    null,
                    candidate.sdp,
                    candidate.sdpMid,
                    candidate.sdpMLineIndex
                )
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                val track = stream?.videoTracks?.firstOrNull() ?: return
                _remoteVideoTracks.value = _remoteVideoTracks.value + (peerId to track)
            }

            override fun onRemoveStream(stream: MediaStream?) {
                _remoteVideoTracks.value = _remoteVideoTracks.value - peerId
            }

            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                if (currentUserId < peerId && offeredPeers[peerId] == true) {
                    peerConnections[peerId]?.let { createAndSendOffer(peerId, it) }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    _remoteVideoTracks.value = _remoteVideoTracks.value + (peerId to track)
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) {
                    _remoteVideoTracks.value = _remoteVideoTracks.value + (peerId to track)
                }
            }
        }) ?: return null

        localAudioTrack?.let { pc.addTrack(it, listOf("NEOV_STREAM")) }
        localVideoTrackImpl?.let { pc.addTrack(it, listOf("NEOV_STREAM")) }
        peerConnections[peerId] = pc
        return pc
    }

    private fun createAndSendOffer(peerId: String, pc: PeerConnection) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null || released.get()) return
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        onSendSignal(peerId, "offer", desc.description, null, null, null)
                    }
                }, desc)
            }
        }, constraints)
    }

    private fun processSignalInternal(signal: CallSignal) {
        val peerId = signal.senderId
        if (peerId.isBlank() || peerId == currentUserId) return
        val pc = getOrCreatePeerConnection(peerId) ?: return

        when (signal.type) {
            "offer" -> {
                val sdpText = signal.sdp ?: return
                val remoteSdp = SessionDescription(SessionDescription.Type.OFFER, sdpText)
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        remoteDescSetPeers[peerId] = true
                        flushPendingIce(peerId, pc)
                        val answerConstraints = MediaConstraints().apply {
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                        }
                        pc.createAnswer(object : SimpleSdpObserver() {
                            override fun onCreateSuccess(answerDesc: SessionDescription?) {
                                if (answerDesc == null || released.get()) return
                                pc.setLocalDescription(object : SimpleSdpObserver() {
                                    override fun onSetSuccess() {
                                        onSendSignal(peerId, "answer", answerDesc.description, null, null, null)
                                    }
                                }, answerDesc)
                            }
                        }, answerConstraints)
                    }
                }, remoteSdp)
            }

            "answer" -> {
                val sdpText = signal.sdp ?: return
                val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpText)
                pc.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        remoteDescSetPeers[peerId] = true
                        flushPendingIce(peerId, pc)
                    }
                }, remoteSdp)
            }

            "ice" -> {
                val candStr = signal.candidate ?: return
                val mid = signal.sdpMid ?: "0"
                val mLine = signal.sdpMLineIndex ?: 0
                val ice = IceCandidate(mid, mLine, candStr)
                if (remoteDescSetPeers[peerId] == true) {
                    pc.addIceCandidate(ice)
                } else {
                    val queue = pendingIceByPeer.getOrPut(peerId) { mutableListOf() }
                    queue.add(ice)
                }
            }
        }
    }

    private fun flushPendingIce(peerId: String, pc: PeerConnection) {
        val queued = pendingIceByPeer.remove(peerId) ?: return
        queued.forEach { pc.addIceCandidate(it) }
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        _localVideoTrack.value = null
        _remoteVideoTracks.value = emptyMap()
        _connectedPeerCount.value = 0

        executor.execute {
            try {
                peerConnections.values.forEach { pc ->
                    try {
                        pc.close()
                        pc.dispose()
                    } catch (_: Exception) {
                    }
                }
                peerConnections.clear()
                pendingIceByPeer.clear()
                remoteDescSetPeers.clear()
                offeredPeers.clear()
                connectedPeers.clear()

                try {
                    videoCapturer?.stopCapture()
                } catch (_: Exception) {
                }
                try {
                    videoCapturer?.dispose()
                } catch (_: Exception) {
                }
                videoCapturer = null

                try {
                    localVideoTrackImpl?.dispose()
                } catch (_: Exception) {
                }
                localVideoTrackImpl = null

                try {
                    videoSource?.dispose()
                } catch (_: Exception) {
                }
                videoSource = null

                try {
                    surfaceTextureHelper?.dispose()
                } catch (_: Exception) {
                }
                surfaceTextureHelper = null

                try {
                    localAudioTrack?.dispose()
                } catch (_: Exception) {
                }
                localAudioTrack = null

                try {
                    audioSource?.dispose()
                } catch (_: Exception) {
                }
                audioSource = null

                try {
                    audioDeviceModule?.release()
                } catch (_: Exception) {
                }
                try {
                    factory.dispose()
                } catch (_: Exception) {
                }
                try {
                    eglBase.release()
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            }
        }
        executor.shutdown()
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {
            Log.w(TAG, "SDP create failure: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.w(TAG, "SDP set failure: $error")
        }
    }

    companion object {
        private const val TAG = "NeovWebRTC"
        private val initialized = AtomicBoolean(false)

        fun ensureFactoryInitialized(context: Context) {
            if (initialized.compareAndSet(false, true)) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
            }
        }
    }
}
