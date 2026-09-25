package com.example.foodfast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.MenuItem
import com.example.foodfast.data.User
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer

enum class OrderStatus(val label: String, val containerColor: Color, val contentColor: Color) {
    PENDIENTE("Pendiente", Color(0xFFFFF3CD), Color(0xFF856404)),
    EN_PREPARACION("En Preparación", Color(0xFFCCE5FF), Color(0xFF004085)),
    LISTO("Listo para Entregar", Color(0xFFD4EDDA), Color(0xFF155724)),
    ENTREGADO("Entregado", Color(0xFFE2E3E5), Color(0xFF383D41))
}

data class BusinessOrder(
    val id: String,
    val clientName: String,
    val clientRole: String,
    val items: List<String>,
    val total: String,
    var status: OrderStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHomeScreen(
    currentUser: User?,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusinessOpen by remember { mutableStateOf(true) }
    val repository = remember { FirestoreRepository() }
    var selectedOrderForScan by remember { mutableStateOf<BusinessOrder?>(null) }

    val orders = remember { mutableStateListOf<BusinessOrder>() }
    val menuList = remember { mutableStateListOf<MenuItem>() }

    val context = LocalContext.current
    val businessId = currentUser?.identificador?.ifEmpty { "01" } ?: "01"
    val businessName = currentUser?.nombreCompleto?.ifEmpty { "Panel de Negocio" } ?: "Panel de Negocio"
    var previousOrderCount by remember { mutableIntStateOf(-1) }

    // Escuchar pedidos de Firestore en tiempo real
    LaunchedEffect(currentUser) {
        repository.escucharPedidos { dbOrders ->
            // Filtrar los pedidos correspondientes a este negocio
            val filteredDbOrders = dbOrders.filter { order ->
                if (currentUser == null) true
                else {
                    val idMatch = order.restaurantId.equals(businessId, ignoreCase = true) ||
                            (order.restaurantId == "01" && (businessId == "NEG-01" || businessName.contains("Cocas", ignoreCase = true)))
                    val nameMatch = order.restaurantName.contains(businessName, ignoreCase = true) ||
                            businessName.contains(order.restaurantName, ignoreCase = true) ||
                            order.restaurantName.contains(currentUser.username, ignoreCase = true)
                    idMatch || nameMatch
                }
            }

            val targetList = if (filteredDbOrders.isNotEmpty()) filteredDbOrders else dbOrders

            val converted = targetList.map { studentOrder ->
                BusinessOrder(
                    id = studentOrder.id,
                    clientName = studentOrder.studentName.ifEmpty { studentOrder.studentUsername },
                    clientRole = "Estudiante",
                    items = listOf(studentOrder.itemsSummary),
                    total = studentOrder.total,
                    status = studentOrder.status
                )
            }.reversed() // Pedidos más recientes arriba

            // Notificación visual de nuevo pedido en tiempo real
            if (previousOrderCount != -1 && converted.size > previousOrderCount) {
                val latest = converted.firstOrNull()
                val client = latest?.clientName ?: "Cliente"
                Toast.makeText(context, "¡NUEVO PEDIDO EN TIEMPO REAL! ${latest?.id ?: ""} de $client", Toast.LENGTH_LONG).show()
            }
            previousOrderCount = converted.size

            orders.clear()
            orders.addAll(converted)
        }
    }

    var matchedRestaurantId by remember { mutableStateOf("") }
    var businessNameState by remember { mutableStateOf(businessName) }
    var timeState by remember { mutableStateOf("15-25 min") }
    var imageUrlState by remember { mutableStateOf("https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500") }

    // Escuchar menú del negocio desde Firestore en tiempo real
    LaunchedEffect(businessId, currentUser) {
        repository.escucharRestaurantesYMenus { dbRestaurants, dbMenus ->
            val matchedRest = dbRestaurants.find { rest ->
                rest.id.equals(businessId, ignoreCase = true) ||
                rest.id.equals(currentUser?.identificador, ignoreCase = true) ||
                rest.name.equals(businessName, ignoreCase = true) ||
                rest.name.contains(businessName, ignoreCase = true) ||
                businessName.contains(rest.name, ignoreCase = true) ||
                (rest.id == "01" && (businessId == "NEG-01" || businessName.contains("Cocas", ignoreCase = true)))
            }

            if (matchedRest != null) {
                isBusinessOpen = matchedRest.isOpen
                businessNameState = matchedRest.name
                timeState = matchedRest.time
                imageUrlState = matchedRest.imageUrl
            }

            val targetRestId = matchedRest?.id ?: currentUser?.identificador?.ifEmpty { businessId } ?: businessId
            matchedRestaurantId = targetRestId

            val myMenu = dbMenus[targetRestId]
                ?: dbMenus[businessId]
                ?: dbMenus[currentUser?.identificador]
                ?: dbMenus[currentUser?.username]
                ?: emptyList()

            menuList.clear()
            menuList.addAll(myMenu)
        }
    }

    var showAddDishDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = businessNameState.ifEmpty { businessName },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Estado: ",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (isBusinessOpen) "Abierto" else "Cerrado",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isBusinessOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = " • En vivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    Switch(
                        checked = isBusinessOpen,
                        onCheckedChange = { newState ->
                            isBusinessOpen = newState
                            val targetRestId = matchedRestaurantId.ifEmpty { businessId }
                            repository.actualizarEstadoAbiertoRestaurante(
                                restaurantId = targetRestId,
                                restaurantName = businessNameState.ifEmpty { businessName },
                                isOpen = newState,
                                onSuccess = {
                                    val statusText = if (newState) "Abierto" else "Cerrado"
                                    Toast.makeText(context, "Negocio $statusText (Actualizado para alumnos)", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) },
                    label = { Text("Pedidos (${orders.count { it.status != OrderStatus.ENTREGADO }})") }
                )
                NavigationBarItem(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                    label = { Text("Menú") }
                )
                NavigationBarItem(
                    selected = (selectedTab == 2),
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                    label = { Text("Ventas") }
                )
                NavigationBarItem(
                    selected = (selectedTab == 3),
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddDishDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Platillo")
                }
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
                0 -> OrdersTabContent(
                    orders = orders,
                    onStatusChange = { orderId, newStatus ->
                        repository.actualizarEstadoPedido(orderId, newStatus, {}, {})
                    },
                    onOpenScanQr = { order -> selectedOrderForScan = order }
                )
                1 -> MenuTabContent(
                    menuList = menuList,
                    onUpdateDish = { updatedItem ->
                        val targetRestId = matchedRestaurantId.ifEmpty { businessId }
                        repository.actualizarPlatilloMenu(
                            restaurantId = targetRestId,
                            restaurantName = businessName,
                            originalItemName = updatedItem.name,
                            updatedItem = updatedItem,
                            onSuccess = {
                                Toast.makeText(context, "Platillo actualizado", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error al actualizar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
                2 -> StatsTabContent(orders = orders)
                3 -> BusinessSettingsTabContent(
                    currentName = businessNameState,
                    currentTime = timeState,
                    currentImageUrl = imageUrlState,
                    isOpen = isBusinessOpen,
                    onSaveSettings = { newName, newTime, newImageUrl, newIsOpen ->
                        businessNameState = newName
                        timeState = newTime
                        imageUrlState = newImageUrl
                        isBusinessOpen = newIsOpen

                        val targetRestId = matchedRestaurantId.ifEmpty { businessId }
                        repository.actualizarAjustesRestaurante(
                            restaurantId = targetRestId,
                            restaurantName = businessName,
                            newName = newName,
                            newTime = newTime,
                            newImageUrl = newImageUrl,
                            isOpen = newIsOpen,
                            onSuccess = {
                                Toast.makeText(context, "Ajustes del negocio guardados en Firestore", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error al guardar ajustes: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }

    if (selectedOrderForScan != null) {
        BusinessScanQRDialog(
            order = selectedOrderForScan!!,
            onDismiss = { selectedOrderForScan = null },
            onConfirmDelivery = {
                val targetOrder = orders.find { it.id == selectedOrderForScan!!.id }
                if (targetOrder != null) {
                    targetOrder.status = OrderStatus.ENTREGADO
                    repository.actualizarEstadoPedido(targetOrder.id, OrderStatus.ENTREGADO, {}, {})
                }
                selectedOrderForScan = null
                Toast.makeText(context, "¡Código QR verificado! Pedido entregado con éxito.", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal para agregar platillo
    if (showAddDishDialog) {
        AddDishDialog(
            onDismiss = { showAddDishDialog = false },
            onAddDish = { name, formattedPrice, desc, time ->
                val newItem = MenuItem(name, formattedPrice, desc, time, true)
                val targetRestId = matchedRestaurantId.ifEmpty { businessId }

                // Guardar en Firestore (se refrescará en tiempo real vía SnapshotListener)
                repository.agregarPlatilloAMenu(
                    restaurantId = targetRestId,
                    restaurantName = businessName,
                    item = newItem,
                    onSuccess = {
                        Toast.makeText(context, "Platillo '$name' publicado en tiempo real", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Error al guardar platillo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                )

                showAddDishDialog = false
            }
        )
    }
}

@Composable
fun OrdersTabContent(
    orders: List<BusinessOrder>,
    onStatusChange: (String, OrderStatus) -> Unit,
    onOpenScanQr: (BusinessOrder) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pedidos registrados en tiempo real.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                OrderCard(
                    order = order,
                    onStatusChange = onStatusChange,
                    onOpenScanQr = { onOpenScanQr(order) }
                )
            }
        }
    }
}

@Composable
fun OrderCard(
    order: BusinessOrder,
    onStatusChange: (String, OrderStatus) -> Unit,
    onOpenScanQr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (order.status == OrderStatus.LISTO) {
                    onOpenScanQr()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cliente: ${order.clientName} (${order.clientRole})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Platillos:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            order.items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: ${order.total}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons according to current status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (order.status) {
                    OrderStatus.PENDIENTE -> {
                        Button(
                            onClick = {
                                onStatusChange(order.id, OrderStatus.EN_PREPARACION)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aceptar y Preparar")
                        }
                    }
                    OrderStatus.EN_PREPARACION -> {
                        Button(
                            onClick = {
                                onStatusChange(order.id, OrderStatus.LISTO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Marcar como Listo")
                        }
                    }
                    OrderStatus.LISTO -> {
                        Button(
                            onClick = onOpenScanQr,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear QR para Entregar")
                        }
                    }
                    OrderStatus.ENTREGADO -> {
                        Text(
                            text = "Pedido finalizado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessScanQRDialog(
    order: BusinessOrder,
    onDismiss: () -> Unit,
    onConfirmDelivery: () -> Unit
) {
    var qrInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val validateAndProcess: (String) -> Unit = { input ->
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "No se ha escaneado ningún código. Mantén la cámara apuntando al código QR."
            isSuccess = false
        } else if (trimmed.contains(order.id, ignoreCase = true) ||
            trimmed.equals("FOODFAST:${order.id}", ignoreCase = true) ||
            trimmed.startsWith("FOODFAST:${order.id}")
        ) {
            errorMessage = null
            isSuccess = true
            onConfirmDelivery()
        } else {
            isSuccess = false
            errorMessage = "Código QR incorrecto. El código escaneado no coincide con el Pedido #${order.id}."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Escáner de Código QR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Pedido #${order.id} - ${order.clientName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apunta la cámara al código QR en la pantalla del alumno/profesor para validar la entrega.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val borderColor = when {
                    isSuccess -> Color(0xFF2E7D32)
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .border(3.dp, borderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                            contentDescription = "Escáner QR",
                            tint = borderColor,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                isSuccess -> "¡Código Correcto!"
                                errorMessage != null -> "Error de lectura"
                                else -> "Escaneando... Esperando código QR"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = qrInput,
                    onValueChange = {
                        qrInput = it
                        if (errorMessage != null) errorMessage = null
                        
                        val trimmed = it.trim()
                        if (trimmed.contains(order.id, ignoreCase = true) ||
                            trimmed.startsWith("FOODFAST:${order.id}", ignoreCase = true)
                        ) {
                            validateAndProcess(it)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { validateAndProcess(qrInput) }
                    )
                )
            }
        },
        confirmButton = {
            // Botón de confirmar eliminado, la validación es automática al escanear
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun MenuTabContent(
    menuList: List<MenuItem>,
    onUpdateDish: (MenuItem) -> Unit
) {
    if (menuList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No hay platillos en el menú de este negocio.\nAgrega uno presionado el botón (+).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val sortedMenu = menuList.sortedByDescending { it.isAvailable }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedMenu) { item ->
                var showEditTimeDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = if (!item.isAvailable) Modifier.background(Color.Gray.copy(alpha = 0.5f)) else Modifier
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
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = item.price,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (item.time.isNotBlank()) {
                                    Text(
                                        text = "Tiempo: ${item.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp).clickable { showEditTimeDialog = true }
                                    )
                                } else {
                                    Text(
                                        text = "Añadir tiempo estimado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp).clickable { showEditTimeDialog = true }
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (item.isAvailable) "Disponible" else "Agotado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isAvailable) Color(0xFF2E7D32) else Color.DarkGray
                                )
                                Switch(
                                    checked = item.isAvailable,
                                    onCheckedChange = { newState ->
                                        onUpdateDish(item.copy(isAvailable = newState))
                                    }
                                )
                            }
                        }
                    }
                }

                if (showEditTimeDialog) {
                    var newTime by remember { mutableStateOf(item.time) }
                    AlertDialog(
                        onDismissRequest = { showEditTimeDialog = false },
                        title = { Text("Editar Tiempo Estimado") },
                        text = {
                            OutlinedTextField(
                                value = newTime,
                                onValueChange = { newTime = it },
                                label = { Text("Tiempo (ej. 15-20 min)") }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                onUpdateDish(item.copy(time = newTime))
                                showEditTimeDialog = false
                            }) {
                                Text("Guardar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditTimeDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsTabContent(orders: List<BusinessOrder>) {
    val totalVentas = orders.sumOf {
        it.total.replace("$", "").toDoubleOrNull() ?: 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Ventas Estimadas del Día",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format(Locale.getDefault(), "%.2f", totalVentas)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Pedidos", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "${orders.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Calificación", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "4.8 / 5.0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddDishDialog(
    onDismiss: () -> Unit,
    onAddDish: (name: String, price: String, description: String, time: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Nuevo Platillo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Platillo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' || it == '$' }) {
                            price = input
                        }
                    },
                    label = { Text("Precio (ej: 120.00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Tiempo estimado (ej: 10-15 min)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val rawPrice = price.trim().replace("$", "")
                    val numericPrice = rawPrice.toDoubleOrNull()

                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Ingresa el nombre del platillo", Toast.LENGTH_SHORT).show()
                    } else if (numericPrice == null || numericPrice <= 0) {
                        Toast.makeText(context, "Ingresa un precio válido (ej: 120.00)", Toast.LENGTH_SHORT).show()
                    } else {
                        val formattedPrice = "$${String.format(Locale.getDefault(), "%.2f", numericPrice)}"
                        onAddDish(cleanName, formattedPrice, description.trim(), time.trim())
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun BusinessSettingsTabContent(
    currentName: String,
    currentTime: String,
    currentImageUrl: String,
    isOpen: Boolean,
    onSaveSettings: (newName: String, newTime: String, newImageUrl: String, isOpen: Boolean) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    var time by remember(currentTime) { mutableStateOf(currentTime) }
    var imageUrl by remember(currentImageUrl) { mutableStateOf(currentImageUrl) }
    var isBusinessOpen by remember(isOpen) { mutableStateOf(isOpen) }

    val scrollState = rememberScrollState()
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    if (selectedImageUri != null) {
        ImageCropperDialog(
            uri = selectedImageUri!!,
            onConfirm = { base64Url ->
                imageUrl = base64Url
                selectedImageUri = null
            },
            onCancel = {
                selectedImageUri = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ajustes del Establecimiento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Personaliza la información pública de tu local que verán los clientes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        // Tarjeta de Vista Previa de Portada
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { galleryLauncher.launch("image/*") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                Box {
                    AsyncImage(
                        model = if (imageUrl.startsWith("data:image")) {
                            val base64String = imageUrl.substringAfter("base64,")
                            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            bitmap
                        } else {
                            imageUrl.ifEmpty { "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500" }
                        },
                        contentDescription = "Vista Previa de Imagen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Cambiar Portada",
                            tint = Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Surface(
                        color = if (isBusinessOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = if (isBusinessOpen) "ABIERTO" else "CERRADO",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = name.ifEmpty { "Nombre de tu Local" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Interruptor Abierto / Cerrado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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
                        text = "Estado del Local",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBusinessOpen) "El local es visible y recibe pedidos de alumnos" else "El local está oculto para los alumnos y no recibe pedidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBusinessOpen,
                    onCheckedChange = { isBusinessOpen = it }
                )
            }
        }

        // Formulario de Edición
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del Restaurante / Local") },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onSaveSettings(name.trim(), time.trim(), imageUrl.trim(), isBusinessOpen)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar Ajustes en Firestore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ImageCropperDialog(
    uri: android.net.Uri,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                val srcBmp = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ -> 
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val maxDim = 1024f
                val scale = if (srcBmp.width > maxDim || srcBmp.height > maxDim) {
                    maxOf(srcBmp.width / maxDim, srcBmp.height / maxDim)
                } else 1f
                bitmap = android.graphics.Bitmap.createScaledBitmap(srcBmp, (srcBmp.width / scale).toInt(), (srcBmp.height / scale).toInt(), true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onCancel,
            confirmButton = {},
            title = { Text("Cargando imagen...") },
            text = { Box(modifier=Modifier.fillMaxWidth(), contentAlignment=Alignment.Center) { CircularProgressIndicator() } }
        )
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var boxWidthPx by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Acomodar Portada", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Desliza y usa 2 dedos para hacer zoom y centrar tu imagen.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.DarkGray)
                        .clipToBounds()
                        .onSizeChanged { size ->
                            boxWidthPx = size.width.toFloat()
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Crop",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                    Box(modifier = Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.primary))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val outW = 800
                        val outH = 450
                        val output = android.graphics.Bitmap.createBitmap(outW, outH, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(output)
                        canvas.drawColor(android.graphics.Color.DKGRAY)
                        
                        val imgW = bitmap!!.width.toFloat()
                        val imgH = bitmap!!.height.toFloat()
                        val boxAspect = 16f / 9f
                        val imgAspect = imgW / imgH
                        
                        val drawW: Float
                        val drawH: Float
                        if (imgAspect > boxAspect) {
                            drawW = outW.toFloat()
                            drawH = outW / imgAspect
                        } else {
                            drawH = outH.toFloat()
                            drawW = outH * imgAspect
                        }
                        
                        val dx = (outW - drawW) / 2f
                        val dy = (outH - drawH) / 2f
                        
                        val matrix = android.graphics.Matrix()
                        matrix.postScale(drawW / imgW, drawH / imgH)
                        matrix.postTranslate(dx, dy)
                        
                        val ratio = outW / boxWidthPx
                        matrix.postTranslate(offsetX * ratio, offsetY * ratio)
                        matrix.postScale(scale, scale, outW / 2f, outH / 2f)
                        
                        canvas.drawBitmap(bitmap!!, matrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                        
                        val stream = java.io.ByteArrayOutputStream()
                        output.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, stream)
                        val bytes = stream.toByteArray()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val finalUrl = "data:image/jpeg;base64,$base64"
                        
                        withContext(Dispatchers.Main) {
                            onConfirm(finalUrl)
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        }
    )
}
