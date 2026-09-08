package com.example.foodfast.ui.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.foodfast.data.MenuItem
import com.example.foodfast.data.Restaurant
import com.example.foodfast.data.sampleRestaurants

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

class CartViewModel : ViewModel() {
    var currentRestaurantId = mutableStateOf<String?>(null)
    val cartItems = mutableStateMapOf<String, CartItem>()

    fun addToCart(restaurantId: String, item: MenuItem): Boolean {
        // Validation: Cannot mix restaurants
        if (currentRestaurantId.value != null && currentRestaurantId.value != restaurantId && cartItems.isNotEmpty()) {
            return false // Deny addition
        }

        currentRestaurantId.value = restaurantId
        val currentQty = cartItems[item.name]?.quantity ?: 0
        cartItems[item.name] = CartItem(item, currentQty + 1)
        return true
    }

    fun removeFromCart(item: MenuItem) {
        val currentQty = cartItems[item.name]?.quantity ?: 0
        if (currentQty > 1) {
            cartItems[item.name] = CartItem(item, currentQty - 1)
        } else {
            cartItems.remove(item.name)
            if (cartItems.isEmpty()) {
                currentRestaurantId.value = null
            }
        }
    }

    fun clearCart() {
        cartItems.clear()
        currentRestaurantId.value = null
    }

    fun getTotal(): Double {
        return cartItems.values.sumOf { 
            val price = it.menuItem.price.replace("$", "").toDoubleOrNull() ?: 0.0
            price * it.quantity 
        }
    }
    
    fun getRestaurantName(): String {
        return sampleRestaurants.find { it.id == currentRestaurantId.value }?.name ?: ""
    }
}
