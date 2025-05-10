package com.example.buttonmashers

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ItemActivity : AppCompatActivity() {
    private lateinit var textViewQuantity: TextView
    private lateinit var iconDecrease : ImageButton
    private lateinit var iconIncrease : ImageButton
    private lateinit var textViewPrice : TextView
    private lateinit var dbHelper: GameDatabaseHelper
    private var quantity: Int = 1
    private var totalPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_item)
        val addToCartButton : Button = findViewById(R.id.addToCartButton)
        val textViewDescription : TextView = findViewById(R.id.textViewDescription)
        val textViewGameName : TextView = findViewById(R.id.textViewGameName)
        val gameImage : ImageView = findViewById(R.id.gameImage)
        textViewPrice = findViewById(R.id.textViewPrice)
        textViewQuantity = findViewById(R.id.textViewQuantity)
        iconDecrease = findViewById(R.id.iconDecrease)
        iconIncrease = findViewById(R.id.iconIncrease)
        iconDecrease.isEnabled = false// Disable decrease button initially
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        dbHelper = GameDatabaseHelper(this) { fileName ->
            resources.getIdentifier(
                fileName,
                "drawable",
                packageName
            )
        }

        //Accessing Values from the main activity screen
        val price = intent.getDoubleExtra("gamePrice",0.0)
        updatePrice(price)
        textViewDescription.text = intent.getStringExtra("gameDescription")
        toolbar.title = intent.getStringExtra("gameTitle")
        textViewGameName.text = intent.getStringExtra("gameTitle")
        val imageResId = intent.getIntExtra("gameImageResId",0)
        gameImage.setImageResource(imageResId)

        setSupportActionBar(toolbar) // Set the toolbar as the support action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        supportActionBar?.setDisplayShowTitleEnabled(true) // Show title

        iconIncrease.setOnClickListener{
            quantity++// Increase number quantity
            updateNumber()
            updatePrice(price)
        }

        iconDecrease.setOnClickListener{
            quantity--// Decrease number quantity
            updateNumber()
            updatePrice(price)
        }

        addToCartButton.setOnClickListener {
            dbHelper.addGameToCart(intent.getIntExtra("gameId",0), quantity)
            finish()
        }
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

    private fun updateNumber() {
        // Update the text view with the current quantity
        textViewQuantity.text = quantity.toString()
        iconDecrease.isEnabled = quantity > 1// Disable decrease button if number is 1
        iconIncrease.isEnabled = quantity < GameDatabaseHelper.MAX_QUANTITY// Disable increase button if number is 10
    }
    private fun updatePrice(price: Double) {
        totalPrice = quantity * price
        textViewPrice.text = "$${String.format("%.2f", totalPrice)}" // Format to last 2 digits
    }
}