package com.example.foodfast.data

data class Restaurant(
    val id: String,
    val name: String,
    val rating: String = "4.5",
    val time: String = "15-25 min",
    val imageUrl: String = "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500",
    val isOpen: Boolean = true
)

data class MenuItem(
    val name: String,
    val price: String,
    val description: String
)

data class SearchResult(
    val restaurantId: String,
    val restaurantName: String,
    val menuItem: MenuItem
)

val sampleRestaurants: List<Restaurant> = emptyList()

val restaurantMenus: Map<String, List<MenuItem>> = emptyMap()

