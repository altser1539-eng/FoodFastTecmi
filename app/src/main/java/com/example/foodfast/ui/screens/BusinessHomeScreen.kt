package com.example.foodfast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.MenuItem
import com.example.foodfast.data.User
import com.example.foodfast.ui.components.QRCodeView
import java.util.Locale

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

    // Escuchar menú del negocio desde Firestore en tiempo real
    LaunchedEffect(businessId) {
        repository.escucharRestaurantesYMenus { _, dbMenus ->
            val myMenu = dbMenus[businessId]
            if (!myMenu.isNullOrEmpty()) {
                menuList.clear()
                menuList.addAll(myMenu)
            } else if (menuList.isEmpty()) {
                // Menú por defecto si Firestore está vacío para este ID
                menuList.addAll(
                    listOf(
                        MenuItem("Pizza Margarita", "$120.00", "Tomate, mozzarella y albahaca fresca."),
                        MenuItem("Pizza Pepperoni", "$140.00", "Pepperoni clásico con mozzarella."),
                        MenuItem("Calzone", "$135.00", "Relleno de jamón y queso.")
                    )
                )
            }
        }
    }

    var showAddDishDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = businessName,
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
                        onCheckedChange = { isBusinessOpen = it }
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
                1 -> MenuTabContent(menuList = menuList)
                2 -> StatsTabContent(orders = orders)
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
            onAddDish = { name, price, desc ->
                val newItem = MenuItem(name, "$$price", desc)

                // Guardar en Firestore (se refrescará en tiempo real vía SnapshotListener)
                repository.agregarPlatilloAMenu(
                    restaurantId = businessId,
                    item = newItem,
                    onSuccess = {
                        Toast.makeText(context, "Platillo '$name' publicado en tiempo real", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        menuList.add(newItem)
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
                    },
                    label = { Text("Código escaneado o texto QR") },
                    placeholder = { Text("Ej: FOODFAST:${order.id}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { validateAndProcess(qrInput) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Validar")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val validCode = "FOODFAST:${order.id}:${order.clientName}"
                            qrInput = validCode
                            validateAndProcess(validCode)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simular QR Correcto", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val wrongCode = "FOODFAST:PED-999-INCORRECTO"
                            qrInput = wrongCode
                            validateAndProcess(wrongCode)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Simular QR Incorrecto", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndProcess(qrInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Validar Código y Entregar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun MenuTabContent(menuList: List<MenuItem>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(menuList) { item ->
            var isAvailable by remember { mutableStateOf(true) }

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
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isAvailable) "Disponible" else "Agotado",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAvailable) Color(0xFF2E7D32) else Color.Red
                        )
                        Switch(
                            checked = isAvailable,
                            onCheckedChange = { isAvailable = it }
                        )
                    }
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
    onAddDish: (name: String, price: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Nuevo Platillo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Platillo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio (ej: 120.00)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        onAddDish(name, price, description)
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
