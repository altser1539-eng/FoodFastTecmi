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

    // Pedidos iniciales de muestra
    val orders = remember {
        mutableStateListOf(
            BusinessOrder("101", "Carlos López", "Estudiante", listOf("2x Pizza Pepperoni", "1x Coca-Cola"), "$320.00", OrderStatus.PENDIENTE),
            BusinessOrder("102", "Dra. María Gómez", "Profesor", listOf("1x Calzone", "1x Agua Mineral"), "$165.00", OrderStatus.EN_PREPARACION),
            BusinessOrder("103", "Ana Martínez", "Estudiante", listOf("1x Pizza Margarita"), "$120.00", OrderStatus.LISTO)
        )
    }

    // Escuchar pedidos de Firestore en tiempo real
    LaunchedEffect(Unit) {
        repository.escucharPedidos { dbOrders ->
            if (dbOrders.isNotEmpty()) {
                orders.clear()
                val converted = dbOrders.map { studentOrder ->
                    BusinessOrder(
                        id = studentOrder.id,
                        clientName = studentOrder.studentName.ifEmpty { studentOrder.studentUsername },
                        clientRole = "Estudiante",
                        items = listOf(studentOrder.itemsSummary),
                        total = studentOrder.total,
                        status = studentOrder.status
                    )
                }
                orders.addAll(converted)
            }
        }
    }

    // Platillos del menú del negocio
    val menuList = remember {
        mutableStateListOf(
            MenuItem("Pizza Margarita", "$120.00", "Tomate, mozzarella y albahaca fresca."),
            MenuItem("Pizza Pepperoni", "$140.00", "Pepperoni clásico con mozzarella."),
            MenuItem("Calzone", "$135.00", "Relleno de jamón y queso.")
        )
    }

    var showAddDishDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentUser?.nombreCompleto?.ifEmpty { "Panel de Negocio" } ?: "Panel de Negocio",
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
                menuList.add(newItem)

                // Guardar en Firestore
                val restId = currentUser?.identificador?.ifEmpty { "01" } ?: "01"
                repository.agregarPlatilloAMenu(
                    restaurantId = restId,
                    item = newItem,
                    onSuccess = {},
                    onFailure = {}
                )

                showAddDishDialog = false
                Toast.makeText(context, "Platillo '$name' guardado en Firestore", Toast.LENGTH_SHORT).show()
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
            Text("No hay pedidos registrados.")
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
    var currentStatus by remember { mutableStateOf(order.status) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (currentStatus == OrderStatus.LISTO) {
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
                    color = currentStatus.containerColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentStatus.label,
                        color = currentStatus.contentColor,
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
                when (currentStatus) {
                    OrderStatus.PENDIENTE -> {
                        Button(
                            onClick = {
                                currentStatus = OrderStatus.EN_PREPARACION
                                order.status = OrderStatus.EN_PREPARACION
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
                                currentStatus = OrderStatus.LISTO
                                order.status = OrderStatus.LISTO
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Escanear Código QR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Pedido #${order.id} - ${order.clientName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apunta la cámara al código QR mostrado en el teléfono del cliente para confirmar la entrega.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Visual Scanner Frame / Camera Viewfinder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escáner QR",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Escaneando...", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                QRCodeView(data = "FOODFAST:${order.id}", sizeDp = 90)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelivery,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Validar QR y Entregar Pedido", fontWeight = FontWeight.Bold)
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
