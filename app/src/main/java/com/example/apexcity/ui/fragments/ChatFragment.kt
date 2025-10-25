package com.example.apexcity.ui.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apexcity.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    
    private lateinit var micButton: FloatingActionButton
    private lateinit var micAnimation: View
    private lateinit var listeningText: TextView
    
    private val micPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        setupSpeechRecognition()
        setupTextToSpeech()
        setupUI(view)
        
        // Add welcome message
        addBotMessage("Hello! I'm your city assistant. How can I help you report an issue today?")
    }
    
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewChat)
        chatAdapter = ChatAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }
    
    private fun setupUI(view: View) {
        micButton = view.findViewById(R.id.fabMic)
        micAnimation = view.findViewById(R.id.micAnimation)
        listeningText = view.findViewById(R.id.tvListening)
        
        micButton.setOnClickListener {
            if (isListening) {
                stopVoiceRecognition()
            } else {
                checkMicPermissionAndStart()
            }
        }
    }
    
    private fun setupSpeechRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listeningText.text = "Listening..."
            }
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {
                // Animate mic visualization based on audio level
                animateMicVisualization(rmsdB)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                stopVoiceRecognition()
            }
            
            override fun onError(error: Int) {
                stopVoiceRecognition()
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    addUserMessage(text)
                    processUserInput(text)
                }
                stopVoiceRecognition()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
    }
    
    private fun checkMicPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            else -> {
                micPermissionRequest.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startVoiceRecognition() {
        isListening = true
        micButton.setImageResource(R.drawable.ic_mic_off)
        micAnimation.visibility = View.VISIBLE
        listeningText.visibility = View.VISIBLE
        startMicAnimation()
        
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    private fun stopVoiceRecognition() {
        isListening = false
        micButton.setImageResource(R.drawable.ic_mic)
        micAnimation.visibility = View.GONE
        listeningText.visibility = View.GONE
        speechRecognizer?.stopListening()
    }
    
    private fun startMicAnimation() {
        val animator = ValueAnimator.ofFloat(0.8f, 1.2f)
        animator.duration = 1000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            micAnimation.scaleX = scale
            micAnimation.scaleY = scale
        }
        animator.start()
    }
    
    private fun animateMicVisualization(rmsdB: Float) {
        val normalized = (rmsdB + 2) / 12 // Normalize to 0-1 range
        val scale = 0.8f + (normalized * 0.4f)
        micAnimation.scaleX = scale
        micAnimation.scaleY = scale
    }
    
    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
    }
    
    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
        speakMessage(text)
    }
    
    private fun speakMessage(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun processUserInput(input: String) {
        // Simple keyword-based response system
        // In production, integrate with your AI backend
        val lowerInput = input.lowercase()
        
        when {
            lowerInput.contains("garbage") || lowerInput.contains("trash") -> {
                addBotMessage("I understand you want to report a garbage issue. Can you describe the location?")
            }
            lowerInput.contains("pothole") || lowerInput.contains("road") -> {
                addBotMessage("Got it, a road infrastructure issue. Where is this pothole located?")
            }
            lowerInput.contains("streetlight") || lowerInput.contains("light") -> {
                addBotMessage("A streetlight issue. Please share the location details.")
            }
            lowerInput.contains("location") || lowerInput.contains("address") -> {
                addBotMessage("Would you like to share your current location or enter it manually?")
            }
            lowerInput.contains("photo") || lowerInput.contains("picture") || lowerInput.contains("image") -> {
                addBotMessage("Please take a photo of the issue. I'll help you report it.")
                // Open camera
                openCameraForIssue()
            }
            else -> {
                addBotMessage("I'm here to help you report civic issues. You can describe problems with roads, garbage, streetlights, or other infrastructure.")
            }
        }
    }
    
    private fun openCameraForIssue() {
        // Navigate to AI report fragment
        val fragment = AIReportFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.navHostFragment, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
    
    // Chat Adapter
    inner class ChatAdapter(private val messages: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val layoutId = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_bot
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return MessageViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            holder.bind(messages[position])
        }
        
        override fun getItemCount() = messages.size
        
        override fun getItemViewType(position: Int) = if (messages[position].isUser) 1 else 0
        
        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val messageText: TextView = itemView.findViewById(R.id.tvMessage)
            
            fun bind(message: ChatMessage) {
                messageText.text = message.message
            }
        }
    }
}