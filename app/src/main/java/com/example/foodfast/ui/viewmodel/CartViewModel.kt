package com.example.foodfast.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.MenuItem
import com.example.foodfast.data.StudentOrder
import com.example.foodfast.data.sampleRestaurants
import com.example.foodfast.ui.screens.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int
)

class CartViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    var currentRestaurantId = mutableStateOf<String?>(null)
    val cartItems = mutableStateMapOf<String, CartItem>()

    // Lista global de pedidos de la sesión sincronizada con Firestore
    val ordersHistory = mutableStateListOf(
        StudentOrder("PED-001", "carlos_estudiante", "Carlos López", "Cocas", "2x Pizza Pepperoni, 1x Coca-Cola", "$320.00", "22/09/2026 14:30", OrderStatus.EN_PREPARACION),
        StudentOrder("PED-002", "carlos_estudiante", "Carlos López", "Periqueños", "1x Clásica con Queso", "$100.00", "21/09/2026 12:15", OrderStatus.ENTREGADO)
    )

    init {
        // Escuchar cambios de pedidos en Firestore en tiempo real
        repository.escucharPedidos { dbOrders ->
            if (dbOrders.isNotEmpty()) {
                ordersHistory.clear()
                ordersHistory.addAll(dbOrders)
            }
        }
    }

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

    fun placeOrder(studentUsername: String, studentName: String): StudentOrder {
        val restaurantName = getRestaurantName().ifEmpty { "Restaurante" }
        val totalFormatted = "$${String.format(Locale.getDefault(), "%.2f", getTotal())}"
        val summary = cartItems.values.joinToString(", ") { "${it.quantity}x ${it.menuItem.name}" }
        val orderNum = ordersHistory.size + 1
        val orderId = "PED-" + String.format(Locale.getDefault(), "%03d", orderNum)
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val newOrder = StudentOrder(
            id = orderId,
            studentUsername = studentUsername,
            studentName = studentName,
            restaurantName = restaurantName,
            itemsSummary = summary.ifEmpty { "Pedido Variado" },
            total = totalFormatted,
            date = now,
            status = OrderStatus.PENDIENTE
        )

        // Guardar pedido en Firestore
        repository.guardarPedido(
            order = newOrder,
            onSuccess = {},
            onFailure = { ordersHistory.add(0, newOrder) }
        )

        clearCart()
        return newOrder
    }
}
