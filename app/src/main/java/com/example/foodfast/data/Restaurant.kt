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

data class SearchResult(
    val restaurantId: String,
    val restaurantName: String,
    val menuItem: MenuItem
)

val sampleRestaurants = listOf(
    Restaurant(
        id = "01",
        name = "Cocas",
        rating = "4.5",
        time = "20-30 min",
        imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=500"
    ),
    Restaurant(
        id = "02",
        name = "Periqueños",
        rating = "4.2",
        time = "15-25 min",
        imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=500"
    ),
    Restaurant(
        id = "03",
        name = "Vida sana",
        rating = "4.8",
        time = "30-45 min",
        imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?q=80&w=500"
    )
)

val restaurantMenus = mapOf(
    "01" to listOf(
        MenuItem("Pizza Margarita", "$120.00", "Tomate, mozzarella y albahaca fresca."),
        MenuItem("Pizza Pepperoni", "$140.00", "Pepperoni clásico con mozzarella."),
        MenuItem("Calzone", "$135.00", "Relleno de jamón y queso.")
    ),
    "02" to listOf(
        MenuItem("Clásica con Queso", "$100.00", "Carne angus, cheddar y vegetales."),
        MenuItem("BBQ Bacon Burger", "$125.00", "Salsa BBQ, tocino crujiente y cebolla frita."),
        MenuItem("Veggie Burger", "$110.00", "Medallón de garbanzo y especias.")
    ),
    "03" to listOf(
        MenuItem("Salmon Roll (10pcs)", "$150.00", "Salmón fresco, aguacate y crema."),
        MenuItem("Tempura Roll", "$140.00", "Camarón tempurizado y salsa dulce."),
        MenuItem("Sopa Miso", "$50.00", "Sopa tradicional con tofu.")
    )
)
