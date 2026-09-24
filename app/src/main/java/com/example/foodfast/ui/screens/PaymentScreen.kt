package com.example.foodfast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.foodfast.data.User
import com.example.foodfast.ui.viewmodel.CartViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: CartViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var showSaveCardDialog by remember { mutableStateOf(false) }
    
    // Error States
    val isCardError = cardNumber.isNotEmpty() && !cardNumber.all { it.isDigit() }
    val isExpiryError = expiryDate.isNotEmpty() && !expiryDate.all { it.isDigit() || it == '/' }
    val isCvvError = cvv.isNotEmpty() && !cvv.all { it.isDigit() }

    val context = LocalContext.current
    val total = viewModel.getTotal()

    val processPayment: (saveCard: Boolean) -> Unit = { saveCard ->
        val studentUser = currentUser?.username ?: "estudiante"
        val studentName = currentUser?.nombreCompleto?.ifEmpty { studentUser } ?: "Estudiante"
        
        viewModel.placeOrder(studentUser, studentName)
        
        if (saveCard) {
            Toast.makeText(context, "Tarjeta guardada y pago realizado con éxito", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "¡Pago registrado! Puedes monitorear tu pedido.", Toast.LENGTH_LONG).show()
        }
        
        showSaveCardDialog = false
        onPaymentSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pago con Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total a pagar: $${String.format(Locale.getDefault(), "%.2f", total)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { input -> if (input.length <= 16 && input.all { it.isDigit() }) cardNumber = input },
                label = { Text("Número de Tarjeta (16 dígitos)") },
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                isError = isCardError,
                supportingText = {
                    if (isCardError) {
                        Text("Solo se permiten números", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { input -> if (input.length <= 5 && input.all { it.isDigit() || it == '/' }) expiryDate = input },
                    label = { Text("MM/AA") },
                    modifier = Modifier.weight(1f),
                    isError = isExpiryError,
                    supportingText = {
                        if (isExpiryError) {
                            Text("Formato inválido", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("08/26") }
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { input -> if (input.length <= 3 && input.all { it.isDigit() }) cvv = input },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    isError = isCvvError,
                    supportingText = {
                        if (isCvvError) {
                            Text("Solo números", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isCardError || isExpiryError || isCvvError) {
                        Toast.makeText(context, "Corrige los errores en rojo", Toast.LENGTH_SHORT).show()
                    } else if (cardNumber.length == 16 && expiryDate.length >= 4 && cvv.length == 3) {
                        showSaveCardDialog = true
                    } else {
                        Toast.makeText(context, "Por favor, completa los datos correctamente", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isCardError && !isExpiryError && !isCvvError,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Pagar Ahora", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showSaveCardDialog) {
        AlertDialog(
            onDismissRequest = { showSaveCardDialog = false },
            title = {
                Text("Guardar Tarjeta", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Deseas guardar la información de esta tarjeta para futuras compras?")
            },
            confirmButton = {
                Button(
                    onClick = { processPayment(true) }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { processPayment(false) }
                ) {
                    Text("No guardar")
                }
            }
        )
    }
}
