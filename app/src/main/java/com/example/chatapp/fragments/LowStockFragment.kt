package com.example.chatapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.adapters.InventoryAdapter
import com.example.chatapp.databinding.FragmentLowStockBinding
import com.example.chatapp.models.InventoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LowStockFragment : Fragment() {

    private var _binding: FragmentLowStockBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val lowStockList = mutableListOf<InventoryItem>()
    private lateinit var adapter: InventoryAdapter
    private var threshold = 10
    private val DB_URL = "https://chatapp-8536b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLowStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        dbRef = FirebaseDatabase.getInstance(DB_URL).reference.child("inventory").child(userId).child("items")

        setupSpinner()
        
        adapter = InventoryAdapter(lowStockList)
        binding.rvLowStock.layoutManager = LinearLayoutManager(context)
        binding.rvLowStock.adapter = adapter

        loadLowStockItems()
    }

    private fun setupSpinner() {
        val thresholds = arrayOf(5, 10, 20, 50)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, thresholds)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerThreshold.adapter = adapter
        binding.spinnerThreshold.setSelection(1) // Default to 10

        binding.spinnerThreshold.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                threshold = thresholds[position]
                loadLowStockItems()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadLowStockItems() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lowStockList.clear()
                for (child in snapshot.children) {
                    val item = child.getValue(InventoryItem::class.java)
                    if (item != null && item.quantity <= threshold) {
                        lowStockList.add(item)
                    }
                }
                lowStockList.sortBy { it.quantity }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
