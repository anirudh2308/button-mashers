package com.example.buttonmashers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.min

class GameDatabaseHelper(
    context: Context,
    private val fileNameToIdConverter: (String) -> Int
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "gamestore.db"
        private const val DB_VERSION = 1
        const val MAX_QUANTITY = 10
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Do nothing.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Do nothing.
    }

    // Get a complete list of games.
    fun getAllGames(): List<Game> {
        val categories = this.getAllCategories()
        val games = mutableListOf<Game>()
        val db = readableDatabase
        val cursor = db.query("games", null, null, null, null, null, null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow("id"))
                val title = getString(getColumnIndexOrThrow("title"))
                val description = getString(getColumnIndexOrThrow("description"))
                val releaseDate = getString(getColumnIndexOrThrow("release_date"))
                val price = getDouble(getColumnIndexOrThrow("price"))
                val categoryId = getInt(getColumnIndexOrThrow("category_id"))
                val rating = getFloat(getColumnIndexOrThrow("rating"))
                val hoursPlayed = getInt(getColumnIndexOrThrow("hours_played"))
                val owned = getString(getColumnIndexOrThrow("owned")).toIntOrNull() == 1
                val imagePath = getString(getColumnIndexOrThrow("image_path"))
                games.add(Game(id, title, description, releaseDate, price, categoryId, categories.find { it.id == categoryId }?.name ?: "?", fileNameToIdConverter(imagePath), rating, hoursPlayed, owned))
            }
            close()
        }
        return games
    }

    // Get game categories.
    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query("categories", null, null, null, null, null, null)

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow("id"))
                val name = getString(getColumnIndexOrThrow("name"))
                categories.add(Category(id, name))
            }
            close()
        }
        return categories
    }

    // Get a complete order history.
    fun getAllOrders(): List<Order> {
        val games = this.getAllGames()

        val orders = mutableListOf<Order>()
        val db = readableDatabase
        val cursorOrders = db.query(
            "orders",
            null,
            null,
            null,
            null,
            null,
            "order_date DESC" // Sort by date in descending order
        )
        with(cursorOrders) {
            while (moveToNext()) {
                val orderId = getInt(getColumnIndexOrThrow("id"))
                val displayedOrderId = getString(getColumnIndexOrThrow("displayed_order_id"))
                val orderDate = getString(getColumnIndexOrThrow("order_date"))

                // Get order items for the current order
                val items = mutableListOf<OrderItem>()
                val cursorItems = db.query(
                    "order_items",
                    null,
                    "order_id = ?",
                    arrayOf(orderId.toString()),
                    null,
                    null,
                    null
                )
                with(cursorItems) {
                    while (moveToNext()) {
                        val gameId = getInt(getColumnIndexOrThrow("game_id"))
                        val game = games.find { it.id == gameId }!!
                        val quantity = getInt(getColumnIndexOrThrow("quantity"))
                        items.add(OrderItem(orderId=orderId, game=game, quantity=quantity))
                    }
                    close()
                }
                orders.add(Order(id=orderId, displayedOrderId=displayedOrderId, orderDate=orderDate, items=items))
            }
            close()
        }
        return orders
    }

    // Add a game with quantity to the shopping cart.
    fun addGameToCart(gameId: Int, quantity: Int) {
        val db = writableDatabase

        // Find the existing shopping cart order, or create a new one.
        val cartCursor = db.query(
            "orders",
            null,
            "isShoppingCart = ?",
            arrayOf("1"),
            null,
            null,
            null
        )

        var cartId: Int? = null

        if (cartCursor.moveToFirst()) {
            cartId = cartCursor.getInt(cartCursor.getColumnIndexOrThrow("id"))
        } else {
            // No cart found in DB, create a new one (new empty order with isShoppingCart = 1).
            val values = ContentValues().apply {
                put("isShoppingCart", 1)
                put("displayed_order_id", "Cart") // A default displayed ID
            }
            cartId = db.insert("orders", null, values).toInt()
        }
        cartCursor.close()

        // Check if the game is already in the cart.
        val itemCursor = db.query(
            "order_items",
            null,
            "order_id = ? AND game_id = ?",
            arrayOf(cartId.toString(), gameId.toString()),
            null,
            null,
            null
        )

        if (itemCursor.moveToFirst()) {
            // Game is already in the cart, update the quantity.
            val existingQuantity = itemCursor.getInt(itemCursor.getColumnIndexOrThrow("quantity"))
            val newQuantity = min(existingQuantity + quantity, MAX_QUANTITY) // Limit max quantity
            val values = ContentValues().apply {
                put("quantity", newQuantity)
            }
            db.update(
                "order_items",
                values,
                "order_id = ? AND game_id = ?",
                arrayOf(cartId.toString(), gameId.toString())
            )
        } else {
            // Game is not in the cart, add new order item.
            val values = ContentValues().apply {
                put("order_id", cartId)
                put("game_id", gameId)
                put("quantity", quantity)
            }
            db.insert("order_items", null, values)
        }
        itemCursor.close()
    }

    // Get the current shopping cart order.
    fun getCart(): Order? {
        val games = this.getAllGames()

        val db = readableDatabase
        val cartCursor = db.query(
            "orders",
            null,
            "isShoppingCart = ?",
            arrayOf("1"),
            null,
            null,
            null
        )

        if (cartCursor.moveToFirst()) {
            val orderId = cartCursor.getInt(cartCursor.getColumnIndexOrThrow("id"))
            val displayedOrderId = cartCursor.getString(cartCursor.getColumnIndexOrThrow("displayed_order_id"))
            val orderDate = cartCursor.getString(cartCursor.getColumnIndexOrThrow("order_date"))

            // Get order items for the current order.
            val items = mutableListOf<OrderItem>()
            val cursorItems = db.query(
                "order_items",
                null,
                "order_id = ?",
                arrayOf(orderId.toString()),
                null,
                null,
                null
            )
            with(cursorItems) {
                while (moveToNext()) {
                    val gameId = getInt(getColumnIndexOrThrow("game_id"))
                    val game = games.find { it.id == gameId }!!
                    val quantity = getInt(getColumnIndexOrThrow("quantity"))
                    items.add(
                        OrderItem(
                            orderId=orderId,
                            game=game,
                            quantity=quantity
                        )
                    )
                }
                close()
            }
            cartCursor.close()
            return Order(
                id=orderId,
                displayedOrderId=displayedOrderId,
                orderDate=orderDate,
                items=items
            )
        }
        cartCursor.close()
        return null // No cart found.
    }

    // Helper function to get the order ID of the current shopping cart.
    private fun getCartId(): Int? {
        val db = readableDatabase
        val cartCursor = db.query(
            "orders",
            null,
            "isShoppingCart = ?",
            arrayOf("1"),
            null,
            null,
            null
        )

        val cartId = if (cartCursor.moveToFirst()) {
            cartCursor.getInt(cartCursor.getColumnIndexOrThrow("id"))
        } else {
            null
        }
        cartCursor.close()
        return cartId
    }

    // Update the quantity of a game in the shopping cart.
    fun updateCartItemQuantity(gameId: Int, newQuantity: Int) {
        val db = writableDatabase

        val cartId = getCartId()
        if (cartId != null) {
            val values = ContentValues().apply {
                put("quantity", min(newQuantity, MAX_QUANTITY))
            }
            db.update(
                "order_items",
                values,
                "order_id = ? AND game_id = ?",
                arrayOf(cartId.toString(), gameId.toString())
            )
        }
    }

    // Remove a game from the shopping cart.
    fun removeCartItem(gameId: Int) {
        val db = writableDatabase

        val cartId = getCartId()
        if (cartId != null) {
            db.delete(
                "order_items",
                "order_id = ? AND game_id = ?",
                arrayOf(cartId.toString(), gameId.toString())
            )
        }
    }

    // Checkout the current shopping cart (convert it to an normal order).
    fun checkoutCart() {
        val db = writableDatabase

        val cartId = getCartId()
        if (cartId != null) {
            val values = ContentValues().apply {
                put("isShoppingCart", 0)
            }
            db.update("orders", values, "id = ?", arrayOf(cartId.toString()))
        }
    }

    fun updateRating(gameId: Int, newRating: Float) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("rating", newRating)
        }
        val selection = "id =?"
        val selectionArgs = arrayOf(gameId.toString())
        db.update("games", values, selection, selectionArgs)
        db.close()
    }

    fun updateProfile(name: String, email: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
        }
        val selection = "id =?"
        val selectionArgs = arrayOf("1")
        db.update("users", values, selection, selectionArgs)
        db.close()
    }

    fun updateProfileImage(imagePath: String) {
        println("Updating profile image to $imagePath")
        val db = writableDatabase
        val values = ContentValues().apply {
            put("image_path", imagePath)
        }
        val selection = "id =?"
        val selectionArgs = arrayOf("1")
        db.update("users", values, selection, selectionArgs)
        db.close()
    }

    fun getProfile(): User {
        val db = readableDatabase
        val cursor = db.query("users", null, null, null, null, null, null)
        var user: User? = null
        with(cursor) {
            while (moveToNext()) {
                val name = getString(getColumnIndexOrThrow("name"))
                val email = getString(getColumnIndexOrThrow("email"))
                val imagePath = getString(getColumnIndexOrThrow("image_path"))
                user = User(1, name, email, imagePath)
                }
            close()
        }
        return user!!
    }
}