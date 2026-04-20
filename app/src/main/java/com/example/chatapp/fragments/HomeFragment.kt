package com.example.chatapp.fragments

import android.app.AlertDialog
import android.os.Bundle
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
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val inventoryList = mutableListOf<InventoryItem>()
    private lateinit var adapter: InventoryAdapter
    private val DB_URL = "https://chatapp-8536b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        dbRef = FirebaseDatabase.getInstance(DB_URL).reference.child("inventory").child(userId)

        adapter = InventoryAdapter(inventoryList)
        binding.rvInventory.layoutManager = LinearLayoutManager(context)
        binding.rvInventory.adapter = adapter

        loadDashboardData()

        binding.btnAddStock.setOnClickListener { showAddStockDialog() }
        binding.btnSellStock.setOnClickListener { showSellStockDialog() }
    }

    private fun loadDashboardData() {
        // Show loading initially
        binding.progressBar.visibility = View.VISIBLE
        
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                inventoryList.clear()
                var totalValue = 0.0
                
                // Get items
                val itemsSnapshot = snapshot.child("items")
                for (child in itemsSnapshot.children) {
                    val item = child.getValue(InventoryItem::class.java)
                    if (item != null) {
                        inventoryList.add(item)
                        totalValue += (item.quantity * item.purchasePrice)
                    }
                }
                
                binding.tvTotalValue.text = "$${String.format("%.2f", totalValue)}"
                adapter.notifyDataSetChanged()

                // Load Profit
                val totalProfit = snapshot.child("totalProfit").getValue(Double::class.java) ?: 0.0
                binding.tvTotalProfit.text = "$${String.format("%.2f", totalProfit)}"
                
                // Hide loading as data (or empty state) has arrived
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddStockDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_stock, null)
        AlertDialog.Builder(context)
            .setTitle("Add New Stock")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogView.findViewById<EditText>(R.id.etItemName).text.toString()
                val qtyString = dialogView.findViewById<EditText>(R.id.etQuantity).text.toString()
                val priceString = dialogView.findViewById<EditText>(R.id.etPrice).text.toString()
                
                val qty = qtyString.toIntOrNull() ?: 0
                val price = priceString.toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty() && qty > 0) {
                    addStockToFirebase(name, qty, price)
                } else {
                    Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addStockToFirebase(name: String, qty: Int, price: Double) {
        val itemRef = dbRef.child("items").push()
        val item = InventoryItem(itemRef.key ?: "", name, qty, price)
        
        itemRef.setValue(item).addOnSuccessListener {
            // Log transaction
            val transactionRef = FirebaseDatabase.getInstance(DB_URL).reference
                .child("transactions")
                .child(auth.currentUser!!.uid)
                .push()
                
            val transaction = Transaction(
                id = transactionRef.key ?: "",
                type = "STOCK",
                itemName = name,
                quantity = qty,
                price = price,
                date = System.currentTimeMillis()
            )
            transactionRef.setValue(transaction)
            Toast.makeText(context, "Stock Added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSellStockDialog() {
        if (inventoryList.isEmpty()) {
            Toast.makeText(context, "No inventory to sell", Toast.LENGTH_SHORT).show()
            return
        }

        val itemNames = inventoryList.map { "${it.name} (Qty: ${it.quantity})" }.toTypedArray()
        var selectedItemIndex = 0

        AlertDialog.Builder(context)
            .setTitle("Select Item to Sell")
            .setSingleChoiceItems(itemNames, 0) { _, which -> selectedItemIndex = which }
            .setPositiveButton("Next") { _, _ ->
                showSellDetailsDialog(inventoryList[selectedItemIndex])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSellDetailsDialog(item: InventoryItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sell_stock, null)
        AlertDialog.Builder(context)
            .setTitle("Sell ${item.name}")
            .setView(dialogView)
            .setPositiveButton("Sell") { _, _ ->
                val buyer = dialogView.findViewById<EditText>(R.id.etBuyerName).text.toString()
                val qtyString = dialogView.findViewById<EditText>(R.id.etQuantity).text.toString()
                val priceString = dialogView.findViewById<EditText>(R.id.etSellingPrice).text.toString()
                
                val qty = qtyString.toIntOrNull() ?: 0
                val price = priceString.toDoubleOrNull() ?: 0.0

                if (qty > 0 && qty <= item.quantity) {
                    processSale(item, buyer, qty, price)
                } else {
                    Toast.makeText(context, "Invalid Quantity (Max: ${item.quantity})", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processSale(item: InventoryItem, buyer: String, qty: Int, sellingPrice: Double) {
        val profit = (sellingPrice - item.purchasePrice) * qty
        
        // Update Inventory
        val newQty = item.quantity - qty
        dbRef.child("items").child(item.id).child("quantity").setValue(newQty)

        // Update Total Profit
        dbRef.child("totalProfit").runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: MutableData): com.google.firebase.database.Transaction.Result {
                val currentProfit = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = currentProfit + profit
                return com.google.firebase.database.Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })

        // Log transaction
        val transactionRef = FirebaseDatabase.getInstance(DB_URL).reference
            .child("transactions")
            .child(auth.currentUser!!.uid)
            .push()

        val transaction = Transaction(
            id = transactionRef.key ?: "",
            type = "SALE",
            itemName = item.name,
            quantity = qty,
            price = sellingPrice,
            buyerName = buyer,
            profit = profit,
            date = System.currentTimeMillis()
        )
        transactionRef.setValue(transaction)
        Toast.makeText(context, "Sale Recorded", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
