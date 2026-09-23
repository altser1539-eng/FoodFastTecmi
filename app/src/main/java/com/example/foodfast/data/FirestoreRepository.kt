package com.example.foodfast.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FirestoreRepository {
    private val db = Firebase.firestore

    fun registrarUsuario(
        user: User,
        passwordHash: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userData = mapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "password" to passwordHash,
            "role" to user.role.name,
            "nombreCompleto" to user.nombreCompleto,
            "identificador" to user.identificador
        )

        db.collection("users")
            .document(user.username.lowercase())
            .set(userData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun iniciarSesion(
        username: String,
        passwordInput: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userDocRef = db.collection("users").document(username.lowercase())
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPassword = document.getString("password")
                    if (storedPassword == passwordInput) {
                        val roleString = document.getString("role")
                        val role = UserRole.fromString(roleString)
                        val user = User(
                            id = document.getString("id") ?: document.id,
                            username = document.getString("username") ?: username,
                            email = document.getString("email") ?: "",
                            role = role,
                            nombreCompleto = document.getString("nombreCompleto") ?: "",
                            identificador = document.getString("identificador") ?: ""
                        )
                        onSuccess(user)
                    } else {
                        onFailure("Contraseña incorrecta")
                    }
                } else {
                    onFailure("Usuario no encontrado")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Error de conexión con Firestore")
            }
    }

    fun obtenerTodosLosUsuarios(
        onSuccess: (List<User>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaUsuarios = snapshot.documents.mapNotNull { doc ->
                    val roleString = doc.getString("role")
                    val role = UserRole.fromString(roleString)
                    User(
                        id = doc.getString("id") ?: doc.id,
                        username = doc.getString("username") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        role = role,
                        nombreCompleto = doc.getString("nombreCompleto") ?: "",
                        identificador = doc.getString("identificador") ?: ""
                    )
                }
                onSuccess(listaUsuarios)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun eliminarUsuario(
        username: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(username.lowercase())
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
