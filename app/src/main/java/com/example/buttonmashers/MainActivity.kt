package com.example.buttonmashers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

fun copyDatabaseFromAssets(context: Context, overwrite: Boolean = false) {
    val dbName = "gamestore.db"
    val dbPath = context.getDatabasePath(dbName).path

    // Check if the database already exists or should be overwritten.
    if (overwrite || !File(dbPath).exists()) {
        context.assets.open(dbName).use { inputStream ->
            FileOutputStream(dbPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}

class MainActivity : AppCompatActivity(), OnGameClickListener {
    private lateinit var dbHelper: GameDatabaseHelper
    private lateinit var gameAdapter: GameAdapter

    // Data
    private lateinit var allGames: List<Game>
    private lateinit var categories: List<Category>
    private val sortOptions = listOf("Featured", "Price: Low to High", "Price: High to Low")

    // UI state
    private var selectedCategory = 0
    private var selectedSortOption = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Copy the database from assets into the app's internal storage.
        copyDatabaseFromAssets(this, overwrite = false)

        // Setup DB helper.
        dbHelper = GameDatabaseHelper(
            this,
            { fileName -> resources.getIdentifier(fileName, "drawable", packageName) }
        )

        // Request permissions for notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // Get data from the database.
        allGames = dbHelper.getAllGames()
        categories = listOf(Category(0, "All")) + dbHelper.getAllCategories()

        // Get UI elements.
        val profileBtn = findViewById<ImageButton>(R.id.profile_btn)
        val cartBtn = findViewById<ImageButton>(R.id.cart_btn)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val categorySpinner = findViewById<Spinner>(R.id.category_spinner)
        val sortBySpinner = findViewById<Spinner>(R.id.sort_by_spinner)

        profileBtn.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        cartBtn.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        // Populate game list with games.
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        gameAdapter = GameAdapter(allGames, this)
        recyclerView.adapter = gameAdapter

        // Setup category spinner.
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedCategory = p2
                updateGameList()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Setup sort by spinner.
        val sortByAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortBySpinner.adapter = sortByAdapter
        sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedSortOption = p2
                updateGameList()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun updateGameList() {
        val selectedCategory = categories[selectedCategory]

        // Filter games based on selected category.
        val filteredGames = if (selectedCategory.id == 0) {
            allGames
        } else {
            allGames.filter { it.categoryId == selectedCategory.id }
        }

        // Sort games based on selected sort option.
        val sortedAndFilteredGames = when (selectedSortOption) {
            1 -> filteredGames.sortedBy { it.price }
            2 -> filteredGames.sortedByDescending { it.price }
            else -> filteredGames
        }

        // Update the adapter with the new list of games.
        gameAdapter.replaceGames(sortedAndFilteredGames)
    }

    override fun onResume() {
        super.onResume()

        // Update cart notification count.
        val cartItemCount = getCartItemCount()
        val cartNotification = findViewById<TextView>(R.id.notification_count)
        cartNotification.text = cartItemCount.toString()
        cartNotification.visibility = if (cartItemCount > 0) View.VISIBLE else View.GONE

        // Start the cart reminder service if there are items in the cart.
        if (cartItemCount > 0) {
            CartReminderService.startService(this, cartItemCount)
        }
    }

    private fun getCartItemCount(): Int {
        val cart = dbHelper.getCart()
        return cart?.items?.sumOf { it.quantity } ?: 0
    }

    override fun onGameClick(game: Game) {
        val intent = Intent(this, ItemActivity::class.java)
        intent.putExtra("gameId", game.id)
        intent.putExtra("gameTitle", game.title)
        intent.putExtra("gameDescription", game.description)
        intent.putExtra("gameReleaseDate", game.releaseDate)
        intent.putExtra("gamePrice", game.price)
        intent.putExtra("gameCategoryId", game.categoryId)
        intent.putExtra("gameImageResId", game.imageResId)
        startActivity(intent)
    }
}

interface OnGameClickListener {
    fun onGameClick(game: Game)
}

class GameAdapter(
    private var games: List<Game>,
    private val listener: OnGameClickListener
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameImage: ImageView = view.findViewById(R.id.game_image)
        val gameTitle: TextView = view.findViewById(R.id.game_title)
        val gamePrice: TextView = view.findViewById(R.id.game_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.gameTitle.text = game.title
        holder.gamePrice.text = "$${game.price}"

        if (game.imageResId != 0) {
            holder.gameImage.setImageResource(game.imageResId)
        }

        holder.itemView.setOnClickListener {
            listener.onGameClick(game)
        }
    }

    fun replaceGames(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()
    }

    override fun getItemCount() = games.size
}
