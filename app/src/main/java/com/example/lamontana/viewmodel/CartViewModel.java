package com.example.lamontana.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lamontana.data.CartStore;
import com.example.lamontana.model.CartItem;
import com.example.lamontana.model.Product;

import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================
 * Archivo: CartViewModel.java
 * Paquete: com.example.lamontana.viewmodel
 * ------------------------------------------------------------
 * ¿De qué se encarga?
 *   - Exponer el estado del carrito como LiveData para la UI:
 *       · Lista de CartItem
 *       · Total en pesos
 *       · Cantidad total de ítems
 *   - Encapsular las operaciones sobre CartStore:
 *       · agregar / incrementar / decrementar / eliminar ítems
 *       · vaciar carrito
 *   - Notificar a la Activity cuando cambian los datos para que
 *     se re-renderice la lista y el total sin leer directo del
 *     CartStore.
 *
 * Relación con otras clases:
 *   - CartStore:
 *       * Fuente real de datos (estado en memoria del carrito).
 *   - CartActivity:
 *       * Observa los LiveData expuestos por este ViewModel y
 *         delega en él todas las operaciones (+, -, eliminar,
 *         vaciar).
 *
 * Beneficios:
 *   - Separa la lógica de negocio/estado (ViewModel + CartStore)
 *     de la lógica de UI (CartActivity).
 *   - Facilita pruebas unitarias del comportamiento del carrito.
 * ============================================================
 */
public class CartViewModel extends ViewModel {

    private final MutableLiveData<List<CartItem>> itemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalAmountLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalQtyLiveData = new MutableLiveData<>();

    public CartViewModel() {
        // Al crear el ViewModel, sincronizamos el estado inicial desde CartStore.
        syncFromStore();
    }

    // ---------- Getters de LiveData para la UI ----------

    public LiveData<List<CartItem>> getItems() {
        return itemsLiveData;
    }

    public LiveData<Integer> getTotalAmount() {
        return totalAmountLiveData;
    }

    public LiveData<Integer> getTotalQty() {
        return totalQtyLiveData;
    }

    // ---------- Operaciones sobre el carrito ----------

    public void inc(Product product) {
        if (product == null) return;
        CartStore.get().inc(product);
        syncFromStore();
    }

    public void dec(Product product) {
        if (product == null) return;
        CartStore.get().dec(product);
        syncFromStore();
    }

    public void remove(Product product) {
        if (product == null) return;
        CartStore.get().remove(product);
        syncFromStore();
    }

    public void clear() {
        CartStore.get().clear();
        syncFromStore();
    }

    /**
     * Fuerza una re-sincronización completa del LiveData con el CartStore.
     * Podés llamarlo desde la UI si por algún motivo se modificó el store
     * desde otra capa.
     */
    public void refresh() {
        syncFromStore();
    }

    // ---------- Sincronización interna ----------

    private void syncFromStore() {
        // Copia defensiva de la lista para no exponer la lista interna del store.
        List<CartItem> snapshot = new ArrayList<>(CartStore.get().getItems());
        itemsLiveData.setValue(snapshot);

        int totalAmount = CartStore.get().getTotalAmount();
        int totalQty = CartStore.get().getTotalQty();

        totalAmountLiveData.setValue(totalAmount);
        totalQtyLiveData.setValue(totalQty);
    }
}
