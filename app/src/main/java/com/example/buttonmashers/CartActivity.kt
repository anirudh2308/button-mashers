package com.example.buttonmashers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


private lateinit var dbHelper: GameDatabaseHelper

interface OnCartItemsChangeListener {
    fun onCartItemsChanged(updatedItems: List<OrderItem>)
}

class CartActivity : AppCompatActivity(), OnCartItemsChangeListener {
    lateinit var checkoutButton: Button
    lateinit var total: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Set the toolbar as the support action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        supportActionBar?.setDisplayShowTitleEnabled(true) // Show title

        // Setup DB helper.
        dbHelper = GameDatabaseHelper(
            this,
            { fileName -> resources.getIdentifier(fileName, "drawable", packageName) }
        )

        total = findViewById(R.id.total_amount)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val cart = dbHelper.getCart()
        val items = cart?.items ?: listOf()
        val orderAdapter = OrderAdapter(items,this)
        recyclerView.adapter = orderAdapter
        checkoutButton = findViewById(R.id.checkout_button)

        checkoutButton.setOnClickListener {
            dbHelper.checkoutCart()
            exitToMainActivity()
        }

        // Init UI on the first load
        onCartItemsChanged(items)
    }

    private fun exitToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()

        // Update number of items in the cart reminder notification.
        val cart = dbHelper.getCart()
        val cartItemCount = cart?.items?.sumOf { it.quantity } ?: 0
        if (cartItemCount == 0) {
            CartReminderService.stopService(this)
        } else {
            CartReminderService.startService(this, cartItemCount)
        }
    }

    override fun onCartItemsChanged(updatedItems: List<OrderItem>) {
        // Show/hide empty cart message
        if (updatedItems.isEmpty()) {
            findViewById<TextView>(R.id.no_games).visibility = View.VISIBLE
            checkoutButton.isEnabled = false

        } else {
            findViewById<TextView>(R.id.no_games).visibility = View.GONE
        }

        // Update total price
        val newTotal = if (updatedItems.isNotEmpty()) {
            updatedItems.sumOf { it.game.price * it.quantity }
        } else {
            0.0
        }
        total.text = String.format("$%.2f", newTotal) // Update the TextView with the total price
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Go back to the previous activity
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item) // Keep default behavior
        }
    }
}

class OrderAdapter(
    private var items: List<OrderItem>,
    private val itemsListener: OnCartItemsChangeListener
) : RecyclerView.Adapter<OrderAdapter.OrderItemViewHolder>() {

    class OrderItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameImage: ImageView = view.findViewById(R.id.order_game_image)
        val gameTitle: TextView = view.findViewById(R.id.order_game_title)
        val gameCategory: TextView = view.findViewById(R.id.order_game_category)
        val gamePrice: TextView = view.findViewById(R.id.order_game_price)
        val gameQuantity: TextView = view.findViewById(R.id.order_game_quantity)
        var delete: ImageButton = view.findViewById(R.id.order_game_delete)
        var quantity_increase: ImageButton= view.findViewById(R.id.order_game_increase)
        var quantity_decrease:  ImageButton= view.findViewById(R.id.order_game_decrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.gameTitle.text = items[position].game.title
        holder.gamePrice.text = String.format("$%.2f", items[position].game.price)
        holder.gameQuantity.text = items[position].quantity.toString()
        holder.gameCategory.text = items[position].game.categoryName
        if (items[position].game.imageResId != 0) {
            holder.gameImage.setImageResource(items[position].game.imageResId)
        }
        holder.delete.setOnClickListener() {
            deleteItem(position)
        }

        holder.quantity_increase.setOnClickListener {
            increaseQuantity(position, holder.gameQuantity, holder.gamePrice)
            holder.quantity_decrease.isEnabled = holder.gameQuantity.text.toString().toInt() > 1
        }

        holder.quantity_decrease.setOnClickListener {
            decreaseQuantity(position, holder.gameQuantity, holder.gamePrice)
            holder.quantity_decrease.isEnabled = holder.gameQuantity.text.toString().toInt() > 1
        }
    }

    fun deleteItem(index: Int) {
        if (index >= 0 && index < items.size) {
            dbHelper.removeCartItem(items[index].game.id)
            val mutable_orders = items.toMutableList()
            mutable_orders.removeAt(index)
            items = mutable_orders
            notifyDataSetChanged()
            itemsListener.onCartItemsChanged(items)
        }
    }

    private fun updateOrderItem(order: OrderItem, gameQuantity: TextView, gamePrice: TextView) {
        // Update UI
        gameQuantity.text = order.quantity.toString()
        gamePrice.text = String.format("$%.2f", order.game.price * order.quantity)

        // Notify listener
        itemsListener.onCartItemsChanged(items)

        // Update database
        dbHelper.updateCartItemQuantity(order.game.id, order.quantity)
    }

    private fun increaseQuantity(index: Int, gameQuantity: TextView, gamePrice: TextView) {
        val order = items[index]

        if (order.quantity < GameDatabaseHelper.MAX_QUANTITY) {
            order.quantity += 1
            updateOrderItem(order, gameQuantity, gamePrice)
        }
    }

    private fun decreaseQuantity(index: Int, gameQuantity: TextView, gamePrice: TextView) {
        val order = items[index]

        if (order.quantity > 1) {
            order.quantity -= 1
            updateOrderItem(order, gameQuantity, gamePrice)
        }
    }

    override fun getItemCount() = items.size
}