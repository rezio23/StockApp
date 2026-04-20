package com.example.chatapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.adapters.MessageAdapter
import com.example.chatapp.databinding.ActivityChatBinding
import com.example.chatapp.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private val messageList = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var currentUserId: String
    private lateinit var receiverId: String
    private val DB_URL = "https://chatapp-8536b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance(DB_URL).reference
        currentUserId = auth.currentUser?.uid ?: ""

        receiverId = intent.getStringExtra("receiverId") ?: ""
        val receiverName = intent.getStringExtra("receiverName") ?: "Chat"

        // Chat ID: sorted UIDs joined so both users share the same node
        chatId = if (currentUserId < receiverId) {
            "${currentUserId}_${receiverId}"
        } else {
            "${receiverId}_${currentUserId}"
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = receiverName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = MessageAdapter(messageList, currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        listenForMessages()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            sendMessage(text)
        }
    }

    private fun listenForMessages() {
        dbRef.child("messages").child(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (child in snapshot.children) {
                        val message = child.getValue(Message::class.java)
                        if (message != null) messageList.add(message)
                    }
                    // Sort by timestamp
                    messageList.sortBy { it.timestamp }
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messageList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessage(text: String) {
        val messageRef = dbRef.child("messages").child(chatId).push()
        val message = Message(
            messageId = messageRef.key ?: "",
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        messageRef.setValue(message)
            .addOnSuccessListener {
                binding.etMessage.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
