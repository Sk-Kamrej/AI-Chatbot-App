package com.example.ai_chatbot_app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ai_chatbot_app.R
import com.example.ai_chatbot_app.data.ChatMessage
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onCopyClicked: (String) -> Unit,
    private val onSpeakClicked: (String) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_ai, parent, false)
            AiViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is UserViewHolder) {
            holder.bind(message)
        } else if (holder is AiViewHolder) {
            holder.bind(message, onCopyClicked, onSpeakClicked)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val tvUserTimestamp: TextView = itemView.findViewById(R.id.tvUserTimestamp)

        fun bind(chatMessage: ChatMessage) {
            tvUserMessage.text = chatMessage.message
            tvUserTimestamp.text = formatTime(chatMessage.timestamp)
        }
    }

    class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAiMessage: TextView = itemView.findViewById(R.id.tvAiMessage)
        private val tvAiTimestamp: TextView = itemView.findViewById(R.id.tvAiTimestamp)
        private val layoutLoading: LinearLayout = itemView.findViewById(R.id.layoutLoading)
        private val layoutFooter: LinearLayout = itemView.findViewById(R.id.layoutFooter)
        private val btnCopy: ImageView = itemView.findViewById(R.id.btnCopy)
        private val btnSpeak: ImageView = itemView.findViewById(R.id.btnSpeak)

        private val markwon: Markwon = Markwon.create(itemView.context)

        fun bind(
            chatMessage: ChatMessage,
            onCopyClicked: (String) -> Unit,
            onSpeakClicked: (String) -> Unit
        ) {
            if (chatMessage.isLoading) {
                layoutLoading.visibility = View.VISIBLE
                tvAiMessage.visibility = View.GONE
                layoutFooter.visibility = View.GONE
            } else {
                layoutLoading.visibility = View.GONE
                tvAiMessage.visibility = View.VISIBLE
                layoutFooter.visibility = View.VISIBLE

                // Render Markdown in AI responses
                markwon.setMarkdown(tvAiMessage, chatMessage.message)

                tvAiTimestamp.text = formatTime(chatMessage.timestamp)

                btnCopy.setOnClickListener {
                    onCopyClicked(chatMessage.message)
                }

                btnSpeak.setOnClickListener {
                    onSpeakClicked(chatMessage.message)
                }
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}