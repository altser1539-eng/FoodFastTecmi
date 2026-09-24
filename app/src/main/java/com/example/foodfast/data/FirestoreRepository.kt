package com.example.foodfast.data

import com.example.foodfast.ui.screens.OrderStatus
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FirestoreRepository {
    private val db = Firebase.firestore

    // ==========================================
    // 1. GESTIÓN DE USUARIOS
    // ==========================================

    fun registrarUsuario(
        user: User,
        passwordHash: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userData = mapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "password" to passwordHash,
            "role" to user.role.name,
            "nombreCompleto" to user.nombreCompleto,
            "identificador" to user.identificador
        )

        db.collection("users")
            .document(user.username.lowercase())
            .set(userData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun iniciarSesion(
        username: String,
        passwordInput: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userDocRef = db.collection("users").document(username.lowercase())
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPassword = document.getString("password")
                    if (storedPassword == passwordInput) {
                        val roleString = document.getString("role")
                        val role = UserRole.fromString(roleString)
                        val user = User(
                            id = document.getString("id") ?: document.id,
                            username = document.getString("username") ?: username,
                            email = document.getString("email") ?: "",
                            role = role,
                            nombreCompleto = document.getString("nombreCompleto") ?: "",
                            identificador = document.getString("identificador") ?: ""
                        )
                        onSuccess(user)
                    } else {
                        onFailure("Contraseña incorrecta")
                    }
                } else {
                    onFailure("Usuario no encontrado")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error de conexión con Firestore")
            }
    }

    fun obtenerTodosLosUsuarios(
        onSuccess: (List<User>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaUsuarios = snapshot.documents.mapNotNull { doc ->
                    val roleString = doc.getString("role")
                    val role = UserRole.fromString(roleString)
                    User(
                        id = doc.getString("id") ?: doc.id,
                        username = doc.getString("username") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        role = role,
                        nombreCompleto = doc.getString("nombreCompleto") ?: "",
                        identificador = doc.getString("identificador") ?: ""
                    )
                }
                onSuccess(listaUsuarios)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun eliminarUsuario(
        username: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(username.lowercase())
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ==========================================
    // 2. GESTIÓN DE RESTAURANTES Y PLATILLOS
    // ==========================================

    fun guardarRestaurante(
        restaurant: Restaurant,
        menu: List<MenuItem>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val menuListMaps = menu.map { item ->
            mapOf(
                "name" to item.name,
                "price" to item.price,
                "description" to item.description
            )
        }

        val restaurantData = mapOf(
            "id" to restaurant.id,
            "name" to restaurant.name,
            "rating" to restaurant.rating,
            "time" to restaurant.time,
            "imageUrl" to restaurant.imageUrl,
            "menu" to menuListMaps
        )

        db.collection("restaurants")
            .document(restaurant.id)
            .set(restaurantData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun obtenerRestaurantesYMenus(
        onSuccess: (List<Restaurant>, Map<String, List<MenuItem>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("restaurants")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaRestaurantes = mutableListOf<Restaurant>()
                val mapaMenus = mutableMapOf<String, List<MenuItem>>()

                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val rating = doc.getString("rating") ?: "4.5"
                    val time = doc.getString("time") ?: "15-25 min"
                    val imageUrl = doc.getString("imageUrl") ?: "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500"

                    val rest = Restaurant(id, name, rating, time, imageUrl)
                    listaRestaurantes.add(rest)

                    val rawMenu = doc.get("menu") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawMenu.map { map ->
                        MenuItem(
                            name = map["name"] as? String ?: "",
                            price = map["price"] as? String ?: "$0.00",
                            description = map["description"] as? String ?: ""
                        )
                    }
                    mapaMenus[id] = items
                    mapaMenus[doc.id] = items
                }

                onSuccess(listaRestaurantes, mapaMenus)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun escucharRestaurantesYMenus(
        onDataChanged: (List<Restaurant>, Map<String, List<MenuItem>>) -> Unit
    ) {
        db.collection("restaurants")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val listaRestaurantes = mutableListOf<Restaurant>()
                val mapaMenus = mutableMapOf<String, List<MenuItem>>()

                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val rating = doc.getString("rating") ?: "4.5"
                    val time = doc.getString("time") ?: "15-25 min"
                    val imageUrl = doc.getString("imageUrl") ?: "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500"

                    val rest = Restaurant(id, name, rating, time, imageUrl)
                    listaRestaurantes.add(rest)

                    val rawMenu = doc.get("menu") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawMenu.map { map ->
                        MenuItem(
                            name = map["name"] as? String ?: "",
                            price = map["price"] as? String ?: "$0.00",
                            description = map["description"] as? String ?: ""
                        )
                    }
                    mapaMenus[id] = items
                    mapaMenus[doc.id] = items
                }

                onDataChanged(listaRestaurantes, mapaMenus)
            }
    }

    fun agregarPlatilloAMenu(
        restaurantId: String,
        item: MenuItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val docRef = db.collection("restaurants").document(restaurantId)
        docRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val rawMenu = (doc.get("menu") as? List<Map<String, Any>>)?.toMutableList() ?: mutableListOf()
                    val newItemMap = mapOf(
                        "name" to item.name,
                        "price" to item.price,
                        "description" to item.description
                    )
                    rawMenu.add(newItemMap)

                    docRef.update("menu", rawMenu)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    // Si el restaurante aún no existe como documento, crearlo
                    val initialMenu = listOf(
                        mapOf(
                            "name" to item.name,
                            "price" to item.price,
                            "description" to item.description
                        )
                    )
                    val data = mapOf(
                        "id" to restaurantId,
                        "name" to "Local $restaurantId",
                        "rating" to "4.5",
                        "time" to "15-25 min",
                        "imageUrl" to "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500",
                        "menu" to initialMenu
                    )
                    docRef.set(data)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ==========================================
    // 3. GESTIÓN DE PEDIDOS Y MONITOREO
    // ==========================================

    fun guardarPedido(
        order: StudentOrder,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderData = mapOf(
            "id" to order.id,
            "studentUsername" to order.studentUsername,
            "studentName" to order.studentName,
            "restaurantId" to order.restaurantId,
            "restaurantName" to order.restaurantName,
            "itemsSummary" to order.itemsSummary,
            "total" to order.total,
            "date" to order.date,
            "status" to order.status.name
        )

        db.collection("pedidos")
            .document(order.id)
            .set(orderData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun escucharPedidos(
        onDataChanged: (List<StudentOrder>) -> Unit
    ) {
        db.collection("pedidos")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val listaPedidos = snapshot.documents.mapNotNull { doc ->
                    val statusStr = doc.getString("status") ?: "PENDIENTE"
                    val status = try {
                        OrderStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        OrderStatus.PENDIENTE
                    }

                    StudentOrder(
                        id = doc.getString("id") ?: doc.id,
                        studentUsername = doc.getString("studentUsername") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        restaurantId = doc.getString("restaurantId") ?: "",
                        restaurantName = doc.getString("restaurantName") ?: "",
                        itemsSummary = doc.getString("itemsSummary") ?: "",
                        total = doc.getString("total") ?: "$0.00",
                        date = doc.getString("date") ?: "",
                        status = status
                    )
                }
                onDataChanged(listaPedidos)
            }
    }

    fun actualizarEstadoPedido(
        orderId: String,
        newStatus: OrderStatus,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("pedidos")
            .document(orderId)
            .update("status", newStatus.name)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
