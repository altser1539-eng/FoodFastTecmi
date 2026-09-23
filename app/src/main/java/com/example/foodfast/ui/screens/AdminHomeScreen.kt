package com.example.foodfast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.User
import com.example.foodfast.data.UserRole
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    currentUser: User?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirestoreRepository() }

    // Lista de usuarios con identificadores numéricos automáticos iniciales
    val userList = remember {
        mutableStateListOf(
            User("1", "roberto", "roberto@tecmilenio.mx", UserRole.ADMIN, "Roberto (Admin)", "01"),
            User("2", "carlos_estudiante", "carlos@tecmilenio.mx", UserRole.ESTUDIANTE, "Carlos López", "02"),
            User("3", "maria_profe", "maria@tecmilenio.mx", UserRole.PROFESOR, "Dra. María Gómez", "03"),
            User("4", "cocas_local", "cocas@tecmilenio.mx", UserRole.NEGOCIO, "Restaurante Cocas", "NEG-01")
        )
    }

    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Calcular el siguiente número en secuencia automática (01, 02, 03, ...)
    val nextSequentialNum = userList.size + 1
    val autoSequentialId = String.format(Locale.getDefault(), "%02d", nextSequentialNum)

    // Calcular identificador automático para negocios (NEG-01, NEG-02, ...)
    val nextNegocioNum = userList.count { it.role == UserRole.NEGOCIO } + 1
    val autoNegocioId = "NEG-" + String.format(Locale.getDefault(), "%02d", nextNegocioNum)

    // Obtener usuarios desde Firestore al iniciar
    LaunchedEffect(Unit) {
        isLoading = true
        repository.obtenerTodosLosUsuarios(
            onSuccess = { usersFromDb ->
                isLoading = false
                if (usersFromDb.isNotEmpty()) {
                    userList.clear()
                    userList.addAll(usersFromDb)
                }
            },
            onFailure = {
                isLoading = false
                // Mantener lista local en caso de prueba sin conexión
            }
        )
    }

    val filteredUsers = userList.filter { user ->
        val matchesRole = (selectedRoleFilter == null || user.role == selectedRoleFilter)
        val matchesSearch = searchQuery.isBlank() ||
                user.nombreCompleto.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
        matchesRole && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel de Administración",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Administrador: ${currentUser?.nombreCompleto?.ifEmpty { "Roberto" } ?: "Roberto"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddUserDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar Usuario / Negocio") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Tarjetas de métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Total Usuarios", "${userList.size}", Modifier.weight(1f))
                MetricCard("Estudiantes", "${userList.count { it.role == UserRole.ESTUDIANTE }}", Modifier.weight(1f))
                MetricCard("Profesores", "${userList.count { it.role == UserRole.PROFESOR }}", Modifier.weight(1f))
                MetricCard("Negocios", "${userList.count { it.role == UserRole.NEGOCIO }}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre, usuario o correo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtro por Rol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = (selectedRoleFilter == null),
                    onClick = { selectedRoleFilter = null },
                    label = { Text("Todos", fontSize = 11.sp) }
                )
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = (selectedRoleFilter == role),
                        onClick = { selectedRoleFilter = if (selectedRoleFilter == role) null else role },
                        label = { Text(role.displayName, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Encabezado de la lista
            Text(
                text = "Usuarios Registrados (${filteredUsers.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No se encontraron registros.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onDelete = {
                                repository.eliminarUsuario(
                                    username = user.username,
                                    onSuccess = {
                                        userList.remove(user)
                                        Toast.makeText(context, "Registro '${user.username}' eliminado", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        userList.remove(user)
                                        Toast.makeText(context, "Eliminado localmente", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            nextSequentialId = autoSequentialId,
            nextNegocioId = autoNegocioId,
            onDismiss = { showAddUserDialog = false },
            onSaveUser = { newUser, password ->
                repository.registrarUsuario(
                    user = newUser,
                    passwordHash = password,
                    onSuccess = {
                        userList.add(newUser)
                        showAddUserDialog = false
                        Toast.makeText(context, "¡Guardado exitosamente en Firestore!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        userList.add(newUser)
                        showAddUserDialog = false
                        Toast.makeText(context, "Guardado localmente (${e.localizedMessage})", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        }
    }
}

@Composable
fun UserCard(
    user: User,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (user.role == UserRole.NEGOCIO) Icons.Default.Store else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.nombreCompleto.ifEmpty { user.username },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ID: ${user.identificador}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "Usuario: @${user.username} | Correo: ${user.email.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Surface(
                    color = when (user.role) {
                        UserRole.ESTUDIANTE -> Color(0xFFE3F2FD)
                        UserRole.PROFESOR -> Color(0xFFE8F5E9)
                        UserRole.NEGOCIO -> Color(0xFFFFF3E0)
                        UserRole.ADMIN -> Color(0xFFF3E5F5)
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Rol: ${user.role.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (user.role) {
                            UserRole.ESTUDIANTE -> Color(0xFF1565C0)
                            UserRole.PROFESOR -> Color(0xFF2E7D32)
                            UserRole.NEGOCIO -> Color(0xFFE65100)
                            UserRole.ADMIN -> Color(0xFF6A1B9A)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun AddUserDialog(
    nextSequentialId: String,
    nextNegocioId: String,
    onDismiss: () -> Unit,
    onSaveUser: (User, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.ESTUDIANTE) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Campos generales para Estudiante / Profesor / Admin
    var nombreCompleto by remember { mutableStateOf("") }

    // Campos específicos para Negocio
    var nombreNegocio by remember { mutableStateOf("") }
    var categoriaNegocio by remember { mutableStateOf("Comida Rápida") }

    val autoId = if (selectedRole == UserRole.NEGOCIO) nextNegocioId else nextSequentialId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedRole == UserRole.NEGOCIO) "Registrar Nuevo Negocio / Local" else "Registrar Nuevo Usuario",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Selector de Rol
                Text("Seleccionar Rol:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserRole.entries.filter { it != UserRole.ADMIN }.forEach { role ->
                        FilterChip(
                            selected = (selectedRole == role),
                            onClick = { selectedRole = role },
                            label = { Text(role.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                // Banner de Identificador Automático
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Identificador asignado automáticamente: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = autoId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                if (selectedRole == UserRole.NEGOCIO) {
                    // REGISTRO ESPECÍFICO DE NEGOCIO
                    Text("Datos del Establecimiento Comercial", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = nombreNegocio,
                        onValueChange = { nombreNegocio = it },
                        label = { Text("Nombre del Local / Restaurante") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = categoriaNegocio,
                        onValueChange = { categoriaNegocio = it },
                        label = { Text("Categoría (ej: Pizzas, Tacos, Cafetería)") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Credenciales de Acceso para el Negocio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario para Iniciar Sesión") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña de Acceso") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // REGISTRO DE ESTUDIANTE, PROFESOR O ADMINISTRADOR
                    Text("Datos del Usuario", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = nombreCompleto,
                        onValueChange = { nombreCompleto = it },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario / Matrícula") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        val finalNombre = if (selectedRole == UserRole.NEGOCIO) {
                            nombreNegocio.trim().ifEmpty { username.trim() }
                        } else {
                            nombreCompleto.trim().ifEmpty { username.trim() }
                        }

                        val newUser = User(
                            id = UUID.randomUUID().toString(),
                            username = username.trim(),
                            email = email.trim(),
                            role = selectedRole,
                            nombreCompleto = finalNombre,
                            identificador = autoId
                        )
                        onSaveUser(newUser, password)
                    }
                }
            ) {
                Text(if (selectedRole == UserRole.NEGOCIO) "Guardar Negocio" else "Guardar Usuario")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
