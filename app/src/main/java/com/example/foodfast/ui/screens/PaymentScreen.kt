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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import com.example.foodfast.data.User
import com.example.foodfast.ui.viewmodel.CartViewModel
import com.example.foodfast.data.SavedCard
import com.example.foodfast.data.FirestoreRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import java.util.Locale

class ExpiryDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

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
    
    val repository = remember { FirestoreRepository() }
    var savedCards by remember { mutableStateOf(currentUser?.savedCards ?: emptyList()) }
    var selectedSavedCardIndex by remember { mutableStateOf(-1) }
    
    // Obtenemos de Firestore al abrir para asegurarnos de tener la versión más fresca
    LaunchedEffect(currentUser, Unit) {
        if (currentUser != null) {
            repository.obtenerTarjetasGuardadas(
                username = currentUser.username,
                onSuccess = { cards ->
                    savedCards = cards
                    if (cards.isNotEmpty() && selectedSavedCardIndex == -1) {
                        selectedSavedCardIndex = 0
                    }
                },
                onFailure = {}
            )
        }
    }
    
    val isUsingNewCard = selectedSavedCardIndex == -1 || savedCards.isEmpty()
    
    // Error States
    val isCardError = cardNumber.isNotEmpty() && !cardNumber.all { it.isDigit() }
    val isExpiryError = expiryDate.isNotEmpty() && expiryDate.length < 4
    val isCvvError = cvv.isNotEmpty() && !cvv.all { it.isDigit() }

    val context = LocalContext.current
    val total = viewModel.getTotal()

    val processPayment: (saveCard: Boolean) -> Unit = { saveCard ->
        val studentUser = currentUser?.username ?: "estudiante"
        val studentName = currentUser?.nombreCompleto?.ifEmpty { studentUser } ?: "Estudiante"
        
        viewModel.placeOrder(studentUser, studentName)
        
        if (saveCard && isUsingNewCard && currentUser != null) {
            val finalExpiryDate = if (expiryDate.length == 4) "${expiryDate.substring(0, 2)}/${expiryDate.substring(2, 4)}" else expiryDate
            val newSavedCard = SavedCard(cardNumber, finalExpiryDate, cvv)
            repository.agregarTarjeta(
                username = currentUser.username,
                card = newSavedCard,
                onSuccess = {
                    Toast.makeText(context, "Tarjeta guardada y pago realizado con éxito", Toast.LENGTH_LONG).show()
                },
                onFailure = {
                    Toast.makeText(context, "Pago realizado, pero no se pudo guardar la tarjeta", Toast.LENGTH_SHORT).show()
                }
            )
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
            
            Spacer(modifier = Modifier.height(24.dp))

            if (savedCards.isNotEmpty()) {
                Text(
                    text = "Métodos de pago",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(savedCards) { index, card ->
                        val isSelected = selectedSavedCardIndex == index
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedSavedCardIndex = index },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { selectedSavedCardIndex = index })
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "•••• •••• •••• ${card.cardNumber.takeLast(4)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Vence: ${card.expiryDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedSavedCardIndex = -1 },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUsingNewCard) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isUsingNewCard, onClick = { selectedSavedCardIndex = -1 })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Usar una tarjeta nueva", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isUsingNewCard) {
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
                        onValueChange = { input -> 
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 4) {
                                expiryDate = digits 
                            }
                        },
                        label = { Text("MM/AA") },
                        modifier = Modifier.weight(1f),
                        isError = isExpiryError,
                        supportingText = {
                            if (isExpiryError) {
                                Text("Faltan dígitos", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ExpiryDateVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("0826") }
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
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (!isUsingNewCard) {
                        // Using a saved card, no extra validation needed
                        processPayment(false)
                    } else {
                        if (isCardError || isExpiryError || isCvvError) {
                            Toast.makeText(context, "Corrige los errores en rojo", Toast.LENGTH_SHORT).show()
                        } else if (cardNumber.length == 16 && expiryDate.length >= 4 && cvv.length == 3) {
                            showSaveCardDialog = true
                        } else {
                            Toast.makeText(context, "Por favor, completa los datos correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isUsingNewCard || (!isCardError && !isExpiryError && !isCvvError),
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
