package com.example.lamontana.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lamontana.R;
import com.example.lamontana.model.Category;
import com.example.lamontana.model.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ============================================================
 * Archivo: CatalogViewModel.java
 * Paquete: com.example.lamontana.viewmodel
 *
 * ¿De qué se encarga este archivo?
 *   - Actúa como ViewModel de la pantalla de Catálogo.
 *   - Carga los productos desde la colección "productos" de
 *     Firebase Firestore respetando la estructura real de
 *     la base de datos (colecciones-db.txt).
 *   - Expone la lista de productos y estados de carga/error
 *     mediante LiveData para que CatalogActivity observe
 *     y renderice la UI.
 *
 * Alcance:
 *   - Es utilizado por CatalogActivity (vista principal de
 *     catálogo luego del login).
 *
 * Métodos presentes:
 *   - getProducts(): LiveData<List<Product>>
 *   - getLoading(): LiveData<Boolean>
 *   - getErrorMessage(): LiveData<String>
 *   - loadProductsIfNeeded(): carga los productos una sola vez.
 *   - reloadProducts(): fuerza recarga desde Firestore.
 *   - mapDocumentToProduct(DocumentSnapshot): mapea un doc de
 *     la colección "productos" a un objeto Product.
 *
 * NOTA SOBRE IMÁGENES:
 *   - En tu Firestore, "productos" tiene un campo "imagenes"
 *     (array de String) con rutas/URLs.
 *   - Para el parcial vamos a seguir usando drawables locales
 *     (imageRes) y no cargamos aún URLs desde red.
 *   - Más adelante se puede extender el modelo Product para
 *     soportar URLs (Firebase Storage) si necesitás compartir
 *     imágenes con otros proyectos.
 * ============================================================
 */
public class CatalogViewModel extends ViewModel {

    // Nombre REAL de la colección en Firestore según colecciones-db.txt:
    // "productos": [{ id, nombre, descripcion, tipo, precio, disponible, imagenes[] }]
    private static final String COLLECTION_PRODUCTS = "productos";

    // LiveData con la lista de productos del catálogo
    private final MutableLiveData<List<Product>> productsLiveData =
            new MutableLiveData<>(Collections.emptyList());

    // Estado de carga (para spinners, etc.)
    private final MutableLiveData<Boolean> loadingLiveData =
            new MutableLiveData<>(false);

    // Mensaje de error simple para la UI
    private final MutableLiveData<String> errorLiveData =
            new MutableLiveData<>(null);

    // Instancia de Firestore (usa la config global donde ya activaste persistencia offline)
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Flag para evitar recargas innecesarias si ya tenemos datos
    private boolean hasLoadedOnce = false;

    // -------------------------------------------------------------------------
    // Getters de LiveData para que la Activity observe cambios
    // -------------------------------------------------------------------------

    public LiveData<List<Product>> getProducts() {
        return productsLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorLiveData;
    }

    // -------------------------------------------------------------------------
    // API pública para la UI
    // -------------------------------------------------------------------------

    /**
     * Carga los productos desde Firestore sólo si aún no se han cargado.
     * Úsalo típicamente en onCreate/onStart de CatalogActivity.
     */
    public void loadProductsIfNeeded() {
        List<Product> current = productsLiveData.getValue();
        if (hasLoadedOnce && current != null && !current.isEmpty()) {
            // Ya hay datos cargados, no volvemos a leer Firestore
            return;
        }
        loadProductsInternal();
    }

    /**
     * Fuerza una recarga desde Firestore (ignorando cache en memoria del ViewModel).
     * Útil si más adelante agregás un botón de "recargar" o "pull to refresh".
     */
    public void reloadProducts() {
        hasLoadedOnce = false;
        loadProductsInternal();
    }

    // -------------------------------------------------------------------------
    // Implementación interna de carga desde Firestore
    // -------------------------------------------------------------------------

    private void loadProductsInternal() {
        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);  // limpiamos error previo

        firestore.collection(COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(this::onProductsLoaded)
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Error al cargar catálogo: " + e.getMessage());
                });
    }

    private void onProductsLoaded(QuerySnapshot snapshot) {
        List<Product> result = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Product p = mapDocumentToProduct(doc);
            if (p != null) {
                result.add(p);
            }
        }

        productsLiveData.setValue(result);
        hasLoadedOnce = true;
        loadingLiveData.setValue(false);
    }

    // -------------------------------------------------------------------------
    // Mapeo de documentos Firestore -> Product
    // -------------------------------------------------------------------------

    @Nullable
    private Product mapDocumentToProduct(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }

        // Campos reales según colecciones-db.txt para "productos":
        //  - nombre: string
        //  - descripcion: string
        //  - tipo: string (ej: "libro", "cuaderno", etc.)
        //  - precio: number
        //  - disponible: boolean
        //  - imagenes: array<string> (por ahora no lo usamos en la app nativa)

        String nombre = doc.getString("nombre");
        String descripcion = doc.getString("descripcion");
        String tipo = doc.getString("tipo");

        // Leemos el precio como Number para soportar int/double
        Number precioNumber = doc.get("precio", Number.class);
        int precio = (precioNumber != null) ? precioNumber.intValue() : 0;

        Boolean disponible = doc.getBoolean("disponible");
        if (disponible != null && !disponible) {
            // Si el producto está marcado como NO disponible, lo excluimos del catálogo
            return null;
        }

        if (nombre == null) {
            // Si falta el nombre, descartamos el doc por datos incompletos
            return null;
        }

        // Mapeamos el campo "tipo" a tu enum Category (PRINT / BINDING).
        Category category = mapCategoryFromTipo(tipo);

        // NOTA SOBRE IMÁGENES:
        //   Aquí podríamos leer el campo "imagenes" (lista de rutas/URLs).
        //   De momento lo ignoramos y elegimos un drawable local de ejemplo.
        int imageRes = mapImageRes(category, nombre);

        // El último parámetro (copyBased) lo fijamos true por defecto para productos
        // que se cobran "por unidad/copia". Si más adelante querés que dependa de Firestore,
        // podés agregar un campo específico.
        boolean copyBased = true;

        // Respetamos las validaciones del constructor Product:
        // - nombre no nulo/ vacío
        // - descripcion no nula (en caso de null, usamos "").
        // - precio >= 0
        // - category no nula
        return new Product(
                nombre,
                descripcion != null ? descripcion : "",
                precio,
                category,
                imageRes,
                copyBased
        );
    }

    /**
     * Traduce el campo "tipo" de la colección "productos" a tu enum Category.
     */
    private Category mapCategoryFromTipo(@Nullable String tipo) {
        if (tipo == null) {
            return Category.PRINT; // default
        }

        String value = tipo.trim().toLowerCase();

        // Heurísticas simples: podés ajustarlas a tus tipos reales
        if (value.contains("anill") || value.contains("encuad") || value.contains("tapa dura")) {
            return Category.BINDING;
        }

        // Default: lo consideramos como impresión/producto general
        return Category.PRINT;
    }

    /**
     * Asigna un drawable de ejemplo según la categoría y/o nombre del producto.
     * Por ahora seguimos usando los drawables locales de la app:
     *   - sample_binding
     *   - sample_print_color
     *   - sample_print_bw
     */
    private int mapImageRes(Category category, String nombre) {
        if (category == Category.BINDING) {
            return R.drawable.sample_binding;
        }

        String lower = nombre.toLowerCase();
        if (lower.contains("color")) {
            return R.drawable.sample_print_color;
        }

        // Default: impresión B/N
        return R.drawable.sample_print_bw;
    }
}
