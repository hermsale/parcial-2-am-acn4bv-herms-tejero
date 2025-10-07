package com.example.parcial_1.model;

/*
 * ============================================================
 * Archivo: Product.java
 * Paquete: com.example.parcial_1.model
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Representa un producto o servicio disponible en el Catálogo.
 *     Cada instancia define la información visual y comercial de un ítem.
 *
 * ¿Qué clase contiene?
 *   - Product: clase de modelo inmutable (sus campos son finales).
 *
 * ¿Qué atributos tiene?
 *   - String name       → nombre del producto (ej.: "Impresión B/N").
 *   - String desc       → descripción corta (ej.: "Cara simple · A4").
 *   - int price         → precio unitario en ARS (entero para simplificar).
 *   - Category category → categoría asociada (PRINT o BINDING).
 *   - int imageRes      → recurso drawable usado como miniatura.
 *   - boolean copyBased → indica si el producto depende de un archivo PDF
 *                         para calcular la cantidad (por ejemplo, copias por página).
 *
 * ¿Qué métodos expone?
 *   - Constructor Product(...) con validaciones básicas.
 *   - toString(): representación legible del producto (útil en logs o depuración).
 *
 * ¿Qué función cumple en las vistas?
 *   - En la vista Catálogo (MainActivity):
 *       * Se crea en el método seedMockData() como parte de la lista allProducts.
 *       * Se renderiza dinámicamente dentro del layout item_catalog.xml.
 *       * El botón “Agregar” asocia el objeto Product al evento para pasarlo a CartStore.
 *   - En la vista Carrito (CartActivity):
 *       * Se accede a los campos (name, desc, price, imageRes) para mostrar la línea correspondiente.
 *
 * Notas:
 *   - La clase es inmutable para evitar modificaciones accidentales.
 *   - Los valores se validan en el constructor para evitar estados inconsistentes.
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

    /** ID de recurso drawable usado como miniatura. */
    public final int imageRes;

    /** true si el producto puede calcular cantidad a partir de un PDF (ej. copias). */
    public final boolean copyBased;

    /**
     * Constructor principal.
     *
     * @param name       nombre del producto (no puede ser null ni vacío)
     * @param desc       descripción corta (no puede ser null)
     * @param price      precio unitario en ARS (debe ser mayor o igual a 0)
     * @param category   categoría asociada (no puede ser null)
     * @param imageRes   recurso drawable asociado (debe ser un ID válido)
     * @param copyBased  indica si el producto depende de archivos PDF
     * @throws IllegalArgumentException si algún parámetro obligatorio es inválido
     */
    public Product(String name, String desc, int price,
                   Category category, int imageRes, boolean copyBased) {

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
    }

    /**
     * Devuelve una descripción legible del producto, útil para depuración.
     */
    @Override
    public String toString() {
        return "Product{name='" + name + "', precio=" + price + " ARS, categoría=" + category + "}";
    }
}
