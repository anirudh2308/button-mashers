package com.example.buttonmashers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OrderRowAdapter(
    private val orders: List<Order>,
    private val context: Context
) : RecyclerView.Adapter<OrderRowAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_item_layout, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderIdTextView.text = "Order ID: ${order.displayedOrderId}"
        holder.orderDateTextView.text = "Date: ${order.orderDate}"

        // Set up RecyclerView for order items
        holder.orderItemsRecyclerView.layoutManager = LinearLayoutManager(context)
        holder.orderItemsRecyclerView.adapter = OrderItemAdapter(order.items)
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdTextView: TextView = itemView.findViewById(R.id.orderIdTextView)
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
        val orderItemsRecyclerView: RecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView)
    }
}
