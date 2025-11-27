package com.example.lamontana.model;

/*
 * ============================================================
 * Archivo: Product.java
 * Paquete: com.example.lamontana.model
 * ------------------------------------------------------------
 * Representa un producto disponible en el Catálogo.
 * Ahora incluye soporte para URL remota de imagen (imageUrl)
 * proveniente de Firebase Storage.
 * ============================================================
 */

public class Product {

    /** Nombre del producto (visible en el Catálogo). */
    public final String name;

    /** Descripción corta o técnica del producto. */
    public final String desc;

    /** Precio unitario en pesos argentinos (ARS). */
    public final int price;

    /** Categoría del producto (PRINT o BINDING). */
    public final Category category;

    /** Recurso drawable local usado como fallback. */
    public final int imageRes;

    /** true si el producto se calcula por copia/archivo PDF. */
    public final boolean copyBased;

    /** URL remota en Firebase Storage (puede ser null). */
    public final String imageUrl;

    /**
     * Constructor completo (7 parámetros).
     */
    public Product(
            String name,
            String desc,
            int price,
            Category category,
            int imageRes,
            boolean copyBased,
            String imageUrl
    ) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede ser nulo ni vacío");
        }
        if (desc == null) {
            throw new IllegalArgumentException("La descripción no puede ser nula");
        }
        if (price < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (category == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula");
        }

        this.name = name.trim();
        this.desc = desc.trim();
        this.price = price;
        this.category = category;
        this.imageRes = imageRes;
        this.copyBased = copyBased;

        // Nuevo campo
        this.imageUrl = imageUrl;  // puede ser null o una URL completa de Firebase Storage
    }

    @Override
    public String toString() {
        return "Product{name='" + name +
                "', precio=" + price +
                ", categoría=" + category +
                ", imageUrl=" + imageUrl +
                "}";
    }
}
