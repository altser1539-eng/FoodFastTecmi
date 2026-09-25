package com.example.foodfast.data

enum class UserRole(val displayName: String) {
    ESTUDIANTE("Estudiante"),
    PROFESOR("Profesor"),
    NEGOCIO("Negocio"),
    ADMIN("Administrador");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: ESTUDIANTE
        }
    }
}

data class SavedCard(
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = "" // In a real app we shouldn't save CVV, but for testing it's fine
)

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val role: UserRole = UserRole.ESTUDIANTE,
    val nombreCompleto: String = "",
    val identificador: String = "", // Matrícula para Estudiante, Nómina para Profesor, Registro para Negocio
    val savedCards: List<SavedCard> = emptyList()
)
