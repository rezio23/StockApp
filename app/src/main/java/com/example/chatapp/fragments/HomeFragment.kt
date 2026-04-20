package com.example.chatapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.R
import com.example.chatapp.adapters.InventoryAdapter
import com.example.chatapp.databinding.FragmentHomeBinding
import com.example.chatapp.models.InventoryItem
import com.example.chatapp.models.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val inventoryList = mutableListOf<InventoryItem>()
    private lateinit var adapter: InventoryAdapter
    
    private val DB_URL = "https://chatapp-8536b-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        
        adapter = InventoryAdapter(inventoryList)
        binding.rvInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventory.adapter = adapter

        binding.btnAddStock.setOnClickListener { showAddStockDialog() }
        binding.btnSellStock.setOnClickListener { showSellStockDialog() }

        if (auth.currentUser != null) {
            loadDashboardData()
        }
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("inventory").child(userId)
        
        binding.progressBar.visibility = View.VISIBLE
        
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                inventoryList.clear()
                var totalValue = 0.0
                
                snapshot.child("items").children.forEach { child ->
                    val item = child.getValue(InventoryItem::class.java)
                    if (item != null) {
                        inventoryList.add(item)
                        totalValue += (item.quantity * item.purchasePrice)
                    }
                }
                
                binding.tvTotalValue.text = "$${String.format("%.2f", totalValue)}"
                binding.tvTotalProfit.text = "$${String.format("%.2f", snapshot.child("totalProfit").getValue(Double::class.java) ?: 0.0)}"
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showAddStockDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_stock, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Add New Stock")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = view.findViewById<EditText>(R.id.etItemName).text.toString().trim()
                val qty = view.findViewById<EditText>(R.id.etQuantity).text.toString().toIntOrNull() ?: 0
                val price = view.findViewById<EditText>(R.id.etPrice).text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty() && qty > 0) {
                    saveToFirebase(name, qty, price)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToFirebase(name: String, qty: Int, price: Double) {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("inventory").child(userId).child("items").push()
        
        val item = InventoryItem(ref.key ?: "", name, qty, price)
        
        ref.setValue(item).addOnSuccessListener {
            Toast.makeText(requireContext(), "Stock Added!", Toast.LENGTH_SHORT).show()
            logTransaction("STOCK", name, qty, price)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun logTransaction(type: String, name: String, qty: Int, price: Double) {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("transactions").child(userId).push()
        val trans = Transaction(ref.key ?: "", type, name, qty, price, date = System.currentTimeMillis())
        ref.setValue(trans)
    }

    private fun showSellStockDialog() {
        if (inventoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Inventory is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val names = inventoryList.map { "${it.name} (Qty: ${it.quantity})" }.toTypedArray()
        var selected = 0
        AlertDialog.Builder(requireContext())
            .setTitle("Select Item")
            .setSingleChoiceItems(names, 0) { _, which -> selected = which }
            .setPositiveButton("Next") { _, _ -> showSellDetails(inventoryList[selected]) }
            .show()
    }

    private fun showSellDetails(item: InventoryItem) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sell_stock, null)
        AlertDialog.Builder(requireContext())
            .setTitle("Sell ${item.name}")
            .setView(view)
            .setPositiveButton("Sell") { _, _ ->
                val qty = view.findViewById<EditText>(R.id.etQuantity).text.toString().toIntOrNull() ?: 0
                val price = view.findViewById<EditText>(R.id.etSellingPrice).text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0 && qty <= item.quantity) {
                    performSale(item, qty, price)
                } else {
                    Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun performSale(item: InventoryItem, qty: Int, sellPrice: Double) {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("inventory").child(userId)
        val profit = (sellPrice - item.purchasePrice) * qty
        
        ref.child("items").child(item.id).child("quantity").setValue(item.quantity - qty)
        ref.child("totalProfit").runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(data: MutableData): com.google.firebase.database.Transaction.Result {
                data.value = (data.getValue(Double::class.java) ?: 0.0) + profit
                return com.google.firebase.database.Transaction.success(data)
            }
            override fun onComplete(a: DatabaseError?, b: Boolean, c: DataSnapshot?) {}
        })

        logTransaction("SALE", item.name, qty, sellPrice)
        Toast.makeText(requireContext(), "Sale recorded", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
