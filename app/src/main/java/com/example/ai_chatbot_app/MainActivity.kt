
package com.example.ai_chatbot_app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_chatbot_app.ui.ChatAdapter
import com.example.ai_chatbot_app.ui.ChatViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnClearChat: ImageButton
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var layoutWelcome: ScrollView

    private val speechLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    etMessage.setText(spokenText)
                    etMessage.setSelection(etMessage.text.length)
                }
            }
        }

    private val requestAudioPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startSpeechRecognition()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required for voice input.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupWindowInsets()
        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        updateThemeToggleIcon()
    }

    private fun setupWindowInsets() {
        val mainLayout = findViewById<View>(R.id.mainLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets =
                insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding =
                if (imeInsets.bottom > 0) imeInsets.bottom
                else systemBars.bottom

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                bottomPadding
            )
            insets
        }
    }

    private fun initViews() {
        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        btnClearChat = findViewById(R.id.btnClearChat)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        layoutWelcome = findViewById(R.id.layoutWelcome)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onCopyClicked = { textToCopy ->
                copyToClipboard(textToCopy)
            },
            onSpeakClicked = { textToSpeak ->
                // Add text-to-speech playback separately if needed.
            }
        )

        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            sendMessageFromInput()
        }

        btnMic.setOnClickListener {
            checkMicrophonePermission()
        }

        btnClearChat.setOnClickListener {
            viewModel.clearChat()
            Toast.makeText(
                this,
                getString(R.string.chat_cleared),
                Toast.LENGTH_SHORT
            ).show()
        }

        btnThemeToggle.setOnClickListener {
            toggleTheme()
        }

        findViewById<TextView>(R.id.chipSuggestion1).setOnClickListener {
            sendPrompt(getString(R.string.suggestion_1))
        }

        findViewById<TextView>(R.id.chipSuggestion2).setOnClickListener {
            sendPrompt(getString(R.string.suggestion_2))
        }

        findViewById<TextView>(R.id.chipSuggestion3).setOnClickListener {
            sendPrompt(getString(R.string.suggestion_3))
        }
    }

    private fun sendMessageFromInput() {
        val userText = etMessage.text.toString().trim()

        if (userText.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a message.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.sendMessage(userText)
        etMessage.text.clear()
    }

    private fun sendPrompt(prompt: String) {
        if (prompt.isNotBlank()) {
            viewModel.sendMessage(prompt)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { messages ->
                    chatAdapter.submitList(messages.toList())

                    val hasMessages = messages.isNotEmpty()

                    layoutWelcome.visibility =
                        if (hasMessages) View.GONE else View.VISIBLE

                    rvChat.visibility =
                        if (hasMessages) View.VISIBLE else View.GONE

                    if (hasMessages) {
                        rvChat.post {
                            if (chatAdapter.itemCount > 0) {
                                rvChat.scrollToPosition(
                                    chatAdapter.itemCount - 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkMicrophonePermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startSpeechRecognition()
        } else {
            requestAudioPermission.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition is unavailable on this device.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val speechIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().toLanguageTag()
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak your message"
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
            )
        }

        try {
            speechLauncher.launch(speechIntent)
        } catch (exception: Exception) {
            Toast.makeText(
                this,
                "Unable to start speech recognition.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()

        val newMode =
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }

        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun updateThemeToggleIcon() {
        val isDarkMode =
            (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES

        btnThemeToggle.setImageResource(
            if (isDarkMode) {
                R.drawable.ic_light_mode
            } else {
                R.drawable.ic_dark_mode
            }
        )

        btnThemeToggle.contentDescription =
            if (isDarkMode) "Switch to light theme"
            else "Switch to dark theme"
    }

    private fun copyToClipboard(text: String) {
        val clipboard =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("Chat response", text)
        )

        Toast.makeText(
            this,
            "Copied to clipboard",
            Toast.LENGTH_SHORT
        ).show()
    }
}
