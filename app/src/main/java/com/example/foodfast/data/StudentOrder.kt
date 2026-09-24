package com.example.foodfast.data

import com.example.foodfast.ui.screens.OrderStatus

data class StudentOrder(
    val id: String = "",
    val studentUsername: String = "",
    val studentName: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val itemsSummary: String = "",
    val total: String = "",
    val date: String = "",
    var status: OrderStatus = OrderStatus.PENDIENTE
)
