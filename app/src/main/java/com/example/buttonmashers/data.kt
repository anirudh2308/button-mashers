package com.example.buttonmashers

data class Game(
    val id: Int,
    val title: String,
    val description: String,
    val releaseDate: String,
    val price: Double,
    val categoryId: Int,
    val categoryName: String,
    val imageResId: Int,
    val rating: Float,
    val hoursPlayed: Int,
    val owned: Boolean
)

data class Category(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}

data class Order(
    val id: Int,
    val displayedOrderId: String,
    val orderDate: String,
    val items: List<OrderItem>
) {
    val total: Double get() = items.sumOf { it.game.price * it.quantity }
}

data class OrderItem(
    val orderId: Int,
    val game: Game,
    var quantity: Int,
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val imagePath: String? = null
)