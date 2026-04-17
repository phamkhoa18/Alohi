package com.example.alohi.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class AlohiWebRtcClient(
    private val context: Context,
    private val eglBaseContext: EglBase.Context,
    private val listener: WebRtcListener
) {

    interface WebRtcListener {
        fun onIceCandidate(candidate: IceCandidate)
        fun onAddStream(stream: MediaStream)
        fun onRemoveStream(stream: MediaStream)
        fun onPeerConnectionStateChange(state: PeerConnection.PeerConnectionState)
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    var localAudioTrack: AudioTrack? = null
    var localVideoTrack: VideoTrack? = null
    var localStream: MediaStream? = null

    private var videoCapturer: VideoCapturer? = null

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryBuilder = PeerConnectionFactory.builder()
        
        val eglBase = EglBase.create()
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        val videoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)

        factoryBuilder.setVideoDecoderFactory(videoDecoderFactory)
        factoryBuilder.setVideoEncoderFactory(videoEncoderFactory)

        peerConnectionFactory = factoryBuilder.createPeerConnectionFactory()
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer) {
        view.init(eglBaseContext, null)
        view.setMirror(true)
        view.setEnableHardwareScaler(true)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        view.init(eglBaseContext, null)
        view.setMirror(false)
        view.setEnableHardwareScaler(true)
    }

    fun startLocalVideoCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        videoCapturer = createCameraCapturer(Camera1Enumerator(false))
        
        val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video_track", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)

        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio_track", audioSource)

        localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
        localStream?.addTrack(localVideoTrack)
        localStream?.addTrack(localAudioTrack)
    }

    fun startLocalAudioCapture() {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio_track", audioSource)

        localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
        localStream?.addTrack(localAudioTrack)
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // Front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Back facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun initializePeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    listener.onIceCandidate(candidate)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                if (stream != null) {
                    listener.onAddStream(stream)
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                if (stream != null) {
                    listener.onRemoveStream(stream)
                }
            }

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                if (newState != null) {
                    listener.onPeerConnectionStateChange(newState)
                }
            }
        })

        localStream?.let {
            peerConnection?.addStream(it)
        }
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun call(onSdpCreated: (String) -> Unit) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(this, sdp)
                sdp?.description?.let { onSdpCreated(it) }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(reason: String?) { Log.e("WebRTC", "Offer failed: $reason") }
            override fun onSetFailure(reason: String?) { Log.e("WebRTC", "Set local failed: $reason") }
        }, constraints)
    }

    fun answer(offerSdp: String, onSdpCreated: (String) -> Unit) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        peerConnection?.setLocalDescription(this, answerSdp)
                        answerSdp?.description?.let { onSdpCreated(it) }
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) { Log.e("WebRTC", "Answer failed: $error") }
                    override fun onSetFailure(error: String?) { Log.e("WebRTC", "Set local answer failed: $error") }
                }, constraints)
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) { Log.e("WebRTC", "Set remote failed: $error") }
        }, sessionDescription)
    }

    fun setRemoteDescription(answerSdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) { Log.e("WebRTC", "Set remote desc failed: $error") }
        }, sessionDescription)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
    }

    fun onDestroy() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnectionFactory?.dispose()
            PeerConnectionFactory.shutdownInternalTracer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
