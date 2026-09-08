package com.example.foodfast.data

data class Restaurant(
    val id: String,
    val name: String,
    val rating: String,
    val time: String,
    val imageUrl: String
)

data class MenuItem(
    val name: String,
    val price: String,
    val description: String
)

val sampleRestaurants = listOf(
    Restaurant(
        id = "cocas",
        name = "Pizzería Roma",
        rating = "4.5",
        time = "20-30 min",
        imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500"
    ),
    Restaurant(
        id = "periquenos",
        name = "Burger Queen",
        rating = "4.2",
        time = "15-25 min",
        imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=500"
    ),
    Restaurant(
        id = "vida_sana",
        name = "Sushi House",
        rating = "4.8",
        time = "30-45 min",
        imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?q=80&w=500"
    )
)

val restaurantMenus = mapOf(
    "cocas" to listOf(
        MenuItem("Pizza Margarita", "$12.00", "Tomate, mozzarella y albahaca fresca."),
        MenuItem("Pizza Pepperoni", "$14.00", "Pepperoni clásico con mozzarella."),
        MenuItem("Calzone", "$13.50", "Relleno de jamón y queso.")
    ),
    "periquenos" to listOf(
        MenuItem("Clásica con Queso", "$10.00", "Carne angus, cheddar y vegetales."),
        MenuItem("BBQ Bacon Burger", "$12.50", "Salsa BBQ, tocino crujiente y cebolla frita."),
        MenuItem("Veggie Burger", "$11.00", "Medallón de garbanzo y especias.")
    ),
    "vida_sana" to listOf(
        MenuItem("Salmon Roll (10pcs)", "$15.00", "Salmón fresco, aguacate y crema."),
        MenuItem("Tempura Roll", "$14.00", "Camarón tempurizado y salsa dulce."),
        MenuItem("Miso Soup", "$5.00", "Sopa tradicional con tofu.")
    )
)
