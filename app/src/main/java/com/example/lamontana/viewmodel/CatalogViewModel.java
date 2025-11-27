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
 * Campos relevantes en la colección "productos":
 *   - nombre: string
 *   - descripcion: string
 *   - tipo: string
 *   - precio: number
 *   - disponible: boolean
 *   - imagenes: array<string>  (primer elemento = URL de Storage)
 *
 * Notas sobre imágenes:
 *   - Leemos el campo "imagenes" como List<String>.
 *   - Si la lista no está vacía, usamos el primer elemento como
 *     imageUrl (URL https:// de Firebase Storage).
 *   - Esa URL se guarda en Product.imageUrl.
 *   - Si imageUrl es null, la UI puede seguir usando imageRes
 *     (drawables locales) como fallback.
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
        String nombre = doc.getString("nombre");
        String descripcion = doc.getString("descripcion");
        String tipo = doc.getString("tipo");

        // --------- Lectura de precio sin usar Number.class ---------
        int precio = 0;

        Long precioLong = doc.getLong("precio");
        if (precioLong != null) {
            precio = precioLong.intValue();
        } else {
            Double precioDouble = doc.getDouble("precio");
            if (precioDouble != null) {
                precio = (int) Math.round(precioDouble);
            }
        }

        Boolean disponible = doc.getBoolean("disponible");
        if (disponible != null && !disponible) {
            // Si el producto está marcado como NO disponible, lo excluimos del catálogo
            return null;
        }

        if (nombre == null) {
            // Si falta el nombre, descartamos el doc por datos incompletos
            return null;
        }

        // ============================
        // NUEVO: leer campo "imagenes"
        // ============================
        // En tu base:
        //   "imagenes": [
        //      "https://firebasestorage.googleapis.com/..."   (posición 0)
        //   ]
        //
        // Tomamos el primer elemento como imageUrl.
        String imagenUrl = null;
        Object rawImagenes = doc.get("imagenes");
        if (rawImagenes instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> imagenes = (List<String>) rawImagenes;
            if (imagenes != null && !imagenes.isEmpty()) {
                imagenUrl = imagenes.get(0);
            }
        }

        // Mapeamos el campo "tipo" a tu enum Category (PRINT / BINDING).
        Category category = mapCategoryFromTipo(tipo);

        // imageRes se sigue usando como fallback/local.
        int imageRes = mapImageRes(category, nombre);

        // El último parámetro (copyBased) lo fijamos true por defecto para productos
        // que se cobran "por unidad/copia".
        boolean copyBased = true;

        return new Product(
                nombre,
                descripcion != null ? descripcion : "",
                precio,
                category,
                imageRes,
                copyBased,
                imagenUrl   // <-- ahora viene del array "imagenes"
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
