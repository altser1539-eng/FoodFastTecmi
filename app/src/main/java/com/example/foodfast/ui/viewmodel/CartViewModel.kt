package com.example.foodfast.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.MenuItem
import com.example.foodfast.data.Restaurant
import com.example.foodfast.data.StudentOrder
import com.example.foodfast.data.restaurantMenus
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

    // Restaurantes y Menús sincronizados en tiempo real desde Firestore
    val restaurantsList = mutableStateListOf<Restaurant>()
    val menusMap = mutableStateMapOf<String, List<MenuItem>>()

    // Lista global de pedidos de la sesión sincronizada con Firestore
    val ordersHistory = mutableStateListOf<StudentOrder>()

    init {
        // Inicializar con datos locales por defecto
        restaurantsList.addAll(sampleRestaurants)
        menusMap.putAll(restaurantMenus)

        // Escuchar restaurantes y menús desde Firestore en tiempo real
        repository.escucharRestaurantesYMenus { dbRestaurants, dbMenus ->
            if (dbRestaurants.isNotEmpty()) {
                val existingIds = dbRestaurants.map { it.id }.toSet()
                val mergedList = dbRestaurants + sampleRestaurants.filter { it.id !in existingIds }
                restaurantsList.clear()
                restaurantsList.addAll(mergedList)
            }
            if (dbMenus.isNotEmpty()) {
                menusMap.putAll(dbMenus)
            }
        }

        // Escuchar cambios de pedidos en Firestore en tiempo real
        repository.escucharPedidos { dbOrders ->
            ordersHistory.clear()
            if (dbOrders.isNotEmpty()) {
                // Mostrar pedidos más recientes primero
                ordersHistory.addAll(dbOrders.reversed())
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
        return restaurantsList.find { it.id == currentRestaurantId.value }?.name ?: ""
    }

    fun placeOrder(studentUsername: String, studentName: String): StudentOrder {
        val restId = currentRestaurantId.value ?: ""
        val restaurantName = getRestaurantName().ifEmpty { "Restaurante" }
        val totalFormatted = "$${String.format(Locale.getDefault(), "%.2f", getTotal())}"
        val summary = cartItems.values.joinToString(", ") { "${it.quantity}x ${it.menuItem.name}" }
        
        // Generar un ID único basado en timestamp y número aleatorio para evitar colisiones
        val timeCode = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
        val randomDigits = (100..999).random()
        val orderId = "PED-$timeCode-$randomDigits"
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val newOrder = StudentOrder(
            id = orderId,
            studentUsername = studentUsername,
            studentName = studentName,
            restaurantId = restId,
            restaurantName = restaurantName,
            itemsSummary = summary.ifEmpty { "Pedido Variado" },
            total = totalFormatted,
            date = now,
            status = OrderStatus.PENDIENTE
        )

        // Guardar pedido en Firestore (se propagará automáticamente en tiempo real vía SnapshotListener)
        repository.guardarPedido(
            order = newOrder,
            onSuccess = {},
            onFailure = {
                if (!ordersHistory.any { it.id == newOrder.id }) {
                    ordersHistory.add(0, newOrder)
                }
            }
        )

        clearCart()
        return newOrder
    }
}
