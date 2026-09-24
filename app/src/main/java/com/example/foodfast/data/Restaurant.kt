package com.example.foodfast.data

data class Restaurant(
    val id: String,
    val name: String,
    val rating: String,
    val time: String,
    val imageUrl: String
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

