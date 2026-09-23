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

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val role: UserRole = UserRole.ESTUDIANTE,
    val nombreCompleto: String = "",
    val identificador: String = "" // Matrícula para Estudiante, Nómina para Profesor, Registro para Negocio
)
