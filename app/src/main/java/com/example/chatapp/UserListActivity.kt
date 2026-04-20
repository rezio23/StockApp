package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.adapters.UserAdapter
import com.example.chatapp.databinding.ActivityUserListBinding
import com.example.chatapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter
    private val DB_URL = "https://chatapp-8536b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        // Explicitly use the Asia-Southeast1 URL
        dbRef = FirebaseDatabase.getInstance(DB_URL).reference

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "ChatApp"

        adapter = UserAdapter(userList) { user ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverId", user.uid)
            intent.putExtra("receiverName", user.name)
            startActivity(intent)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Log.e("UserListActivity", "No user logged in!")
            binding.progressBar.visibility = View.GONE
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        Log.d("UserListActivity", "Loading users from database...")

        dbRef.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserListActivity", "Data received. Count: ${snapshot.childrenCount}")
                userList.clear()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null && user.uid != currentUid) {
                        userList.add(user)
                    }
                }
                
                binding.progressBar.visibility = View.GONE
                if (userList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserListActivity", "Database error: ${error.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@UserListActivity, "Database error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
