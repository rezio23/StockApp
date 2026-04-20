package com.example.chatapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.tvTransItem)
        val type: TextView = view.findViewById(R.id.tvTransType)
        val details: TextView = view.findViewById(R.id.tvTransDetails)
        val extra: TextView = view.findViewById(R.id.tvTransExtra)
        val date: TextView = view.findViewById(R.id.tvTransDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val trans = transactions[position]
        holder.itemName.text = trans.itemName
        holder.type.text = trans.type
        
        if (trans.type == "STOCK") {
            holder.type.setBackgroundColor(Color.parseColor("#4CAF50"))
            holder.details.text = "Qty: ${trans.quantity} | Purchase Price: $${String.format("%.2f", trans.price)}"
            holder.extra.visibility = View.GONE
        } else {
            holder.type.setBackgroundColor(Color.parseColor("#FF9800"))
            holder.details.text = "Qty: ${trans.quantity} | Sale Price: $${String.format("%.2f", trans.price)}"
            holder.extra.visibility = View.VISIBLE
            holder.extra.text = "Buyer: ${trans.buyerName} | Profit: $${String.format("%.2f", trans.profit ?: 0.0)}"
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.date.text = sdf.format(Date(trans.date))
    }

    override fun getItemCount() = transactions.size
}
