package com.example.parcial_1.model;

/*
 * ============================================================
 * Archivo: Category.java
 * Paquete: com.example.parcial_1.model
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Define las categorías de productos disponibles en la app.
 *     Se utiliza para clasificar los servicios ofrecidos y aplicar
 *     filtros visuales en la vista Catálogo.
 *
 * ¿Qué clase contiene?
 *   - Category: enumeración (enum) pública.
 *
 * ¿Qué valores define?
 *   - PRINT   → productos relacionados con impresiones
 *                (blanco y negro, color, copias).
 *   - BINDING → productos de encuadernado y anillado.
 *
 * ¿Qué función cumple en las vistas?
 *   - En la vista Catálogo (MainActivity):
 *       * Permite filtrar la lista de productos por tipo.
 *       * Ejemplo: el botón “Impresiones” muestra solo Category.PRINT.
 *
 * ¿Qué ventajas aporta?
 *   - Evita el uso de strings “mágicos” al clasificar productos.
 *   - Simplifica las comparaciones y mejora la mantenibilidad del código.
 *
 * Notas:
 *   - Si en el futuro se agregan más tipos de servicios (por ejemplo “Copiado”),
 *     basta con sumar un nuevo valor al enum.
 * ============================================================
 */

public enum Category {
    /** Productos de impresión (B/N, color, copias, etc.) */
    PRINT,

    /** Productos de encuadernado o anillado. */
    BINDING
}
