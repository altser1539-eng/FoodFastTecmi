package com.example.foodfast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodfast.data.Restaurant
import com.example.foodfast.data.SearchResult
import com.example.foodfast.data.StudentOrder
import com.example.foodfast.data.User
import com.example.foodfast.ui.components.QRCodeView
import com.example.foodfast.ui.viewmodel.CartViewModel

@Composable
fun HomeScreen(
    viewModel: CartViewModel,
    currentUser: User? = null,
    onRestaurantClick: (String, String?) -> Unit,
    onViewCart: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedOrderForQr by remember { mutableStateOf<StudentOrder?>(null) }

    val searchResults = remember(query, viewModel.menusMap.size) {
        if (query.isEmpty()) emptyList<SearchResult>()
        else {
            val results = mutableListOf<SearchResult>()
            viewModel.menusMap.forEach { (restaurantId, menu) ->
                val restaurant = viewModel.restaurantsList.find { it.id == restaurantId }
                val restName = restaurant?.name ?: "Local $restaurantId"
                menu.filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
                    .forEach { item ->
                        results.add(SearchResult(restaurantId, restName, item))
                    }
            }
            results
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentUser?.nombreCompleto?.isNotEmpty() == true) "¡Hola, ${currentUser.nombreCompleto}!" else "¡Hola, Gourmet!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentUser != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Rol: ${currentUser.role.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedBox(
                            badge = {
                                if (viewModel.cartItems.isNotEmpty()) {
                                    Badge {
                                        Text(viewModel.cartItems.values.sumOf { it.quantity }.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = onViewCart) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Cerrar Sesión",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                    label = { Text("Restaurantes") }
                )
                NavigationBarItem(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
                    label = { Text("Monitoreo") }
                )
                NavigationBarItem(
                    selected = (selectedTab == 2),
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Mis Pedidos") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> CatalogTabContent(
                    query = query,
                    onQueryChange = { query = it },
                    searchResults = searchResults,
                    restaurants = viewModel.restaurantsList,
                    onRestaurantClick = onRestaurantClick
                )
                1 -> ActiveOrderMonitoringTab(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onOrderClick = { order -> selectedOrderForQr = order }
                )
                2 -> OrderHistoryTab(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    onOrderClick = { order -> selectedOrderForQr = order }
                )
            }
        }
    }

    if (selectedOrderForQr != null) {
        StudentQRDialog(
            order = selectedOrderForQr!!,
            onDismiss = { selectedOrderForQr = null }
        )
    }
}

@Composable
fun CatalogTabContent(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<SearchResult>,
    restaurants: List<Restaurant>,
    onRestaurantClick: (String, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "¿Qué te apetece comer hoy?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SearchBar(query = query, onQueryChange = onQueryChange)

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isNotEmpty()) {
            Text(
                text = "Resultados de búsqueda (${searchResults.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (searchResults.isEmpty()) {
                Text(
                    text = "No se encontraron platillos que coincidan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                searchResults.forEach { result ->
                    SearchResultItem(
                        result = result,
                        onClick = { onRestaurantClick(result.restaurantId, result.menuItem.name) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Text(
                text = "Restaurantes Disponibles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            RestaurantList(restaurants = restaurants, onRestaurantClick = { id -> onRestaurantClick(id, null) })
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActiveOrderMonitoringTab(
    viewModel: CartViewModel,
    currentUser: User? = null,
    onOrderClick: (StudentOrder) -> Unit
) {
    val studentUsername = currentUser?.username ?: ""
    val userOrders = if (studentUsername.isNotEmpty()) {
        viewModel.ordersHistory.filter { 
            it.studentUsername.equals(studentUsername, ignoreCase = true) ||
            it.studentUsername.isEmpty() 
        }
    } else {
        viewModel.ordersHistory
    }
    val activeOrders = userOrders.filter { it.status != OrderStatus.ENTREGADO }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Monitoreo de Pedido Activo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sigue el estado de tu comida en tiempo real",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (activeOrders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TrackChanges,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No tienes ningún pedido activo en este momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Realiza un pedido desde el menú para monitorearlo aquí.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeOrders) { order ->
                    OrderMonitoringCard(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
fun OrderMonitoringCard(
    order: StudentOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pedido #${order.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Restaurante: ${order.restaurantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = order.status.containerColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status.label,
                        color = order.status.contentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Detalle: ${order.itemsSummary}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Fecha: ${order.date}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Total: ${order.total}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Bar Indicator
            Text("Progreso del Pedido:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OrderStepProgress(currentStatus = order.status)

            if (order.status == OrderStatus.LISTO) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Código QR para Recoger", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OrderStepProgress(currentStatus: OrderStatus) {
    val steps = listOf(
        OrderStatus.PENDIENTE,
        OrderStatus.EN_PREPARACION,
        OrderStatus.LISTO,
        OrderStatus.ENTREGADO
    )

    val currentStepIndex = steps.indexOf(currentStatus).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index <= currentStepIndex
            val isCurrent = index == currentStepIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (step) {
                        OrderStatus.PENDIENTE -> "Recibido"
                        OrderStatus.EN_PREPARACION -> "Preparando"
                        OrderStatus.LISTO -> "Listo"
                        OrderStatus.ENTREGADO -> "Entregado"
                    },
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun OrderHistoryTab(
    viewModel: CartViewModel,
    currentUser: User? = null,
    onOrderClick: (StudentOrder) -> Unit
) {
    val studentUsername = currentUser?.username ?: ""
    val allOrders = if (studentUsername.isNotEmpty()) {
        viewModel.ordersHistory.filter { 
            it.studentUsername.equals(studentUsername, ignoreCase = true) ||
            it.studentUsername.isEmpty() 
        }
    } else {
        viewModel.ordersHistory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de Pedidos Realizados",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Lista completa de todas tus compras pasadas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (allOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no has realizado ningún pedido.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allOrders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOrderClick(order) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pedido #${order.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    color = order.status.containerColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = order.status.label,
                                        color = order.status.contentColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Restaurante: ${order.restaurantName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Platillos: ${order.itemsSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Fecha: ${order.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = order.total,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentQRDialog(
    order: StudentOrder,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Código QR de Entrega", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Pedido #${order.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Muestra este código QR en el mostrador de ${order.restaurantName} para recoger tu comida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                QRCodeView(data = "FOODFAST:${order.id}:${order.studentUsername}")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Estado: ${order.status.label}",
                    fontWeight = FontWeight.Bold,
                    color = order.status.contentColor
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Entendido / Cerrar")
            }
        }
    )
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar platillo (ej. Pizza, Burger...)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.menuItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "En ${result.restaurantName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = result.menuItem.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Text(
                text = result.menuItem.price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun RestaurantList(
    restaurants: List<Restaurant>,
    onRestaurantClick: (String) -> Unit
) {
    if (restaurants.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No hay restaurantes registrados en la base de datos.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El administrador debe registrar un nuevo negocio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            restaurants.forEach { restaurant ->
                RestaurantCard(restaurant = restaurant, onClick = onRestaurantClick)
            }
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(restaurant.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = restaurant.imageUrl,
                contentDescription = restaurant.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Calificación",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = restaurant.rating,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = "Tiempo",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = restaurant.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
