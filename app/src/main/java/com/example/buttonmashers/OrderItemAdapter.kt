package com.example.buttonmashers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderItemAdapter(
    private val orderItems: List<OrderItem>
) : RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_item, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        val orderItem = orderItems[position]
        holder.orderItemTitleTextView.text = orderItem.game.title
        holder.orderItemQuantityTextView.text = "Quantity: ${orderItem.quantity}"
        holder.orderItemPriceTextView.text = "Price: $${String.format("%.2f", orderItem.game.price * orderItem.quantity)}"
        holder.orderItemImageView.setImageResource(orderItem.game.imageResId)
    }

    override fun getItemCount(): Int = orderItems.size

    class OrderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderItemImageView: ImageView = itemView.findViewById(R.id.orderItemImageView)
        val orderItemTitleTextView: TextView = itemView.findViewById(R.id.orderItemTitleTextView)
        val orderItemQuantityTextView: TextView = itemView.findViewById(R.id.orderItemQuantityTextView)
        val orderItemPriceTextView: TextView = itemView.findViewById(R.id.orderItemPriceTextView)
    }
}
