package com.example.alohi.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alohi.data.remote.SocketManager
import com.example.alohi.webrtc.AlohiWebRtcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import org.webrtc.EglBase
import java.util.UUID

data class CallState(
    val isIncoming: Boolean = false,
    val isRinging: Boolean = false,
    val isActive: Boolean = false,
    val callId: String = "",
    val callType: String = "voice", // "voice" or "video"
    val callerName: String = "",
    val callerUrl: String = "",
    val remoteStream: MediaStream? = null,
    val isVideoEnabled: Boolean = false,
    val isAudioEnabled: Boolean = true
)

class CallViewModel(application: Application) : AndroidViewModel(application), AlohiWebRtcClient.WebRtcListener {

    private val eglBase = EglBase.create()
    val eglBaseContext: EglBase.Context = eglBase.eglBaseContext

    private var webRtcClient = AlohiWebRtcClient(application, eglBaseContext, this)
    
    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var pendingCallId: String = ""
    private var pendingTargetId: String = ""
    private var isCaller = false

    init {
        listenToSocket()
    }

    private fun listenToSocket() {
        viewModelScope.launch {
            SocketManager.callIncoming.collect { json ->
                val callId = json.optString("callId")
                val callerName = json.optJSONObject("caller")?.optString("displayName") ?: "Ai đó"
                val sdpOffer = json.optString("sdpOffer")
                val type = json.optString("type", "voice")
                
                pendingCallId = callId
                pendingTargetId = json.optJSONObject("caller")?.optString("_id") ?: ""
                isCaller = false
                
                _callState.value = CallState(
                    isIncoming = true,
                    isRinging = true,
                    callId = callId,
                    callerName = callerName,
                    callType = type,
                    isVideoEnabled = (type == "video")
                )
                Log.d("CallVM", "📱 Incoming socket call from $callerName ($type)")
            }
        }

        viewModelScope.launch {
            // Wait for answer
            SocketManager.callAccepted.collect { json ->
                val sdpAnswer = json.optString("sdpAnswer")
                Log.d("CallVM", "Call accepted: parsing Answer SDP")
                _callState.value = _callState.value.copy(isRinging = false, isActive = true)
                webRtcClient.setRemoteDescription(sdpAnswer)
            }
        }

        viewModelScope.launch {
            // Incoming ICE candidates
            SocketManager.callIceCandidate.collect { json ->
                val candidateObj = json.optJSONObject("candidate") ?: return@collect
                Log.d("CallVM", "Received ICE candidate")
                webRtcClient.addRemoteIceCandidate(
                    candidateObj.optString("sdpMid"),
                    candidateObj.optInt("sdpMLineIndex"),
                    candidateObj.optString("candidate")
                )
            }
        }

        viewModelScope.launch {
            SocketManager.callEnded.collect {
                endCallUI()
            }
        }

        viewModelScope.launch {
            SocketManager.callRejected.collect {
                endCallUI()
            }
        }

        viewModelScope.launch {
            SocketManager.callTimeout.collect {
                endCallUI()
            }
        }
    }

    private var pendingOfferSdp: String = ""

    // ========== Outgoing Call ==========
    fun initCall(receiverId: String, isVideo: Boolean) {
        pendingTargetId = receiverId
        pendingCallId = UUID.randomUUID().toString()
        isCaller = true
        
        _callState.value = CallState(
            isIncoming = false,
            isRinging = true,
            callId = pendingCallId,
            callType = if (isVideo) "video" else "voice",
            isVideoEnabled = isVideo
        )
        // Note: Actual WebRTC call is deferred to startWebRtcSession() after UI attaches camera
    }
    
    fun sendOfferToSocket(sdpOffer: String, callType: String = "audio") {
        SocketManager.initiateCall(pendingTargetId, callType, pendingCallId, sdpOffer)
    }

    // ========== Incoming Call ==========
    fun notifyIncomingCall(callId: String, callerName: String, offerSdp: String, callType: String = "voice") {
        pendingCallId = callId
        isCaller = false
        pendingOfferSdp = offerSdp
        _callState.value = CallState(
            isIncoming = true,
            isRinging = true,
            callId = callId,
            callerName = callerName,
            callType = callType,
            isVideoEnabled = (callType == "video")
        )
    }

    fun answerCall(offerSdp: String = pendingOfferSdp) {
        pendingOfferSdp = offerSdp
        _callState.value = _callState.value.copy(
            isIncoming = false, // transition from incoming to active UI
            isRinging = false,
            isActive = true
        )
        // Actual answering is deferred to startWebRtcSession()
    }
    
    // ========== Deferred WebRTC Execution ==========
    // Called by ActiveCallView when it successfully gets permissions and initializes the local stream
    fun startWebRtcSession() {
        webRtcClient.initializePeerConnection()
        
        if (isCaller) {
            webRtcClient.call { sdp ->
                sendOfferToSocket(sdp, _callState.value.callType)
            }
        } else {
            webRtcClient.answer(pendingOfferSdp) { sdp ->
                sendAnswerToSocket(sdp)
            }
        }
    }

    fun sendAnswerToSocket(sdpAnswer: String) {
        SocketManager.acceptCall(pendingCallId, sdpAnswer)
    }

    fun rejectCall() {
        SocketManager.rejectCall(pendingCallId)
        endCallUI()
    }

    fun endCall() {
        SocketManager.endCall(pendingCallId, 100) // Dummy 100s, usually track time
        endCallUI()
    }

    private fun endCallUI() {
        _callState.value = CallState() 
        webRtcClient.onDestroy()
        webRtcClient = AlohiWebRtcClient(getApplication(), eglBaseContext, this) // Reset
    }

    // ========== WebRtcListener ==========
    override fun onIceCandidate(candidate: IceCandidate) {
        SocketManager.sendIceCandidate(pendingCallId, candidate)
    }

    override fun onAddStream(stream: MediaStream) {
        _callState.value = _callState.value.copy(remoteStream = stream)
    }

    override fun onRemoveStream(stream: MediaStream) {
        _callState.value = _callState.value.copy(remoteStream = null)
    }

    override fun onPeerConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        Log.d("CallVM", "Connection state: $state")
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer) {
        webRtcClient.initLocalSurfaceView(view)
        webRtcClient.startLocalVideoCapture(view)
    }
    
    fun startLocalAudioOnly() {
        webRtcClient.startLocalAudioCapture()
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRtcClient.initRemoteSurfaceView(view)
    }

    fun toggleVideo() {
        val enabled = !_callState.value.isVideoEnabled
        _callState.value = _callState.value.copy(isVideoEnabled = enabled)
        webRtcClient.toggleVideo(enabled)
    }

    fun toggleAudio() {
        val enabled = !_callState.value.isAudioEnabled
        _callState.value = _callState.value.copy(isAudioEnabled = enabled)
        webRtcClient.toggleAudio(enabled)
    }
}
