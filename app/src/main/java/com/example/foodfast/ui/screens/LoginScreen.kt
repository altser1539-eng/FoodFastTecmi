package com.example.foodfast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodfast.data.FirestoreRepository
import com.example.foodfast.data.User
import com.example.foodfast.data.UserRole

@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val repository = remember { FirestoreRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo or App Name
        Text(
            text = "FoodFast",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )
        Text(
            text = "Acceso Institucional Tecmilenio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = {
                val inputUser = username.trim()
                val inputPass = password.trim()

                if (inputUser.isEmpty() || inputPass.isEmpty()) {
                    Toast.makeText(context, "Por favor, ingresa tus credenciales", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // 1. Verificación para el Administrador "Roberto"
                if (inputUser.equals("Roberto", ignoreCase = true)) {
                    if (inputPass == "1234") {
                        val adminUser = User(
                            id = "admin-1",
                            username = "Roberto",
                            email = "roberto@tecmilenio.mx",
                            role = UserRole.ADMIN,
                            nombreCompleto = "Roberto (Administrador)"
                        )
                        Toast.makeText(context, "¡Bienvenido Administrador Roberto!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(adminUser)
                    } else {
                        Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                    return@Button
                }

                // 2. Intentar autenticar en Firestore
                isLoading = true
                repository.iniciarSesion(
                    username = inputUser,
                    passwordInput = inputPass,
                    onSuccess = { user ->
                        isLoading = false
                        Toast.makeText(context, "¡Bienvenido ${user.nombreCompleto.ifEmpty { user.username }} (${user.role.displayName})!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(user)
                    },
                    onFailure = { errorMessage ->
                        isLoading = false
                        if (errorMessage == "Contraseña incorrecta") {
                            Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        } else {
                            // Usuario no encontrado en Firestore, revisar lista local de pruebas
                            val lowerUser = inputUser.lowercase()
                            val localSampleAccounts = mapOf(
                                "carlos_estudiante" to Pair("1234", User("2", "carlos_estudiante", "carlos@tecmilenio.mx", UserRole.ESTUDIANTE, "Carlos López", "02")),
                                "carlos" to Pair("1234", User("2", "carlos_estudiante", "carlos@tecmilenio.mx", UserRole.ESTUDIANTE, "Carlos López", "02")),
                                "maria_profe" to Pair("1234", User("3", "maria_profe", "maria@tecmilenio.mx", UserRole.PROFESOR, "Dra. María Gómez", "03")),
                                "maria" to Pair("1234", User("3", "maria_profe", "maria@tecmilenio.mx", UserRole.PROFESOR, "Dra. María Gómez", "03")),
                                "cocas_local" to Pair("1234", User("4", "cocas_local", "cocas@tecmilenio.mx", UserRole.NEGOCIO, "Cocas", "01")),
                                "cocas" to Pair("1234", User("4", "cocas_local", "cocas@tecmilenio.mx", UserRole.NEGOCIO, "Cocas", "01"))
                            )

                            if (localSampleAccounts.containsKey(lowerUser)) {
                                val (expectedPass, matchedUser) = localSampleAccounts[lowerUser]!!
                                if (inputPass == expectedPass) {
                                    Toast.makeText(context, "¡Bienvenido ${matchedUser.nombreCompleto} (${matchedUser.role.displayName})!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(matchedUser)
                                } else {
                                    Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
