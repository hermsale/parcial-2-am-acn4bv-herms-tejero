package com.example.lamontana.ui.navbar;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.lamontana.R;
import com.example.lamontana.data.auth.AuthRepository;
import com.example.lamontana.data.user.UserStore;
import com.example.lamontana.ui.CartActivity;
import com.example.lamontana.ui.CatalogActivity;
import com.example.lamontana.ui.LoginActivity;
import com.example.lamontana.ui.ProfileActivity;

/*
 * ============================================================
 * Archivo: MenuDesplegableHelper.java
 * Paquete: com.example.lamontana.ui.nav
 * ------------------------------------------------------------
 * ¿Qué hace este helper?
 *   - Encapsula TODA la lógica del menú desplegable superior
 *     (top sheet) usado en varias Activities de la app.
 *   - Evita duplicar código de:
 *       · abrir/cerrar menú
 *       · overlay oscuro
 *       · animaciones
 *       · navegación a: Inicio, Mis datos, Mi carrito, Logout
 *
 * ¿Por qué existe?
 *   - Reduce el tamaño de las Activities.
 *   - Aumenta cohesión y reduce acoplamiento.
 *   - Permite reusar el mismo menú en Catalog, Cart, Profile, etc.
 *
 * Métodos presentes:
 *   - constructor(...)
 *   - initMenu()
 *   - toggleMenu()
 *   - openMenu()
 *   - closeMenu()
 *   - configureButtons()
 *
 * Requisitos:
 *   - La Activity debe tener en su layout:
 *       · btnMenu (ImageView)
 *       · overlay (View)
 *       · topSheet (View)
 *       · Opcionalmente btnInicio
 *       · btnMisDatos
 *       · btnMiCarrito
 *       · btnCerrarSesion
 *
 * Nota:
 *   - Logout se maneja desde AuthRepository y UserStore.
 * ============================================================
 */
public class MenuDesplegableHelper {

    private final Activity activity;

    // Vistas del menú
    private final ImageView btnMenu;
    private final View overlay;
    private final View topSheet;

    // Opcionales según pantalla
    private final View btnInicio;
    private final View btnMisDatos;
    private final View btnMiCarrito;
    private final View btnCerrarSesion;

    private boolean isMenuOpen = false;

    public MenuDesplegableHelper(
            Activity activity,
            ImageView btnMenu,
            View overlay,
            View topSheet,
            View btnInicio,
            View btnMisDatos,
            View btnMiCarrito,
            View btnCerrarSesion
    ) {
        this.activity = activity;
        this.btnMenu = btnMenu;
        this.overlay = overlay;
        this.topSheet = topSheet;
        this.btnInicio = btnInicio;
        this.btnMisDatos = btnMisDatos;
        this.btnMiCarrito = btnMiCarrito;
        this.btnCerrarSesion = btnCerrarSesion;
    }

    /**
     * Enlaza listeners de menú y deja todo listo.
     */
    public void initMenu() {

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> toggleMenu());
        }
        if (overlay != null) {
            overlay.setOnClickListener(v -> closeMenu());
        }

        configureButtons();
    }

    /**
     * Configura las acciones de los botones del menú.
     */
    private void configureButtons() {

        // 1) Ir a Inicio (solo si existe el botón)
        if (btnInicio != null) {
            btnInicio.setOnClickListener(v -> {
                closeMenu();
                Intent i = new Intent(activity, CatalogActivity.class);
                activity.startActivity(i);
                activity.finish();
            });
        }

        // 2) Mis Datos
        if (btnMisDatos != null) {
            btnMisDatos.setOnClickListener(v -> {
                closeMenu();
                Intent i = new Intent(activity, ProfileActivity.class);
                activity.startActivity(i);
            });
        }

        // 3) Mi Carrito (siempre navega a CartActivity)
        if (btnMiCarrito != null) {
            btnMiCarrito.setOnClickListener(v -> {
                closeMenu();
                Intent i = new Intent(activity, CartActivity.class);
                activity.startActivity(i);
            });
        }

        // 4) Cerrar sesión
        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> {
                closeMenu();

                // Logout centralizado
                AuthRepository.getInstance().logout();
                UserStore.get().clear();

                Intent i = new Intent(activity, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(i);
                activity.finish();
            });
        }
    }

    /**
     * Alterna entre abrir y cerrar menú.
     */
    public void toggleMenu() {
        if (isMenuOpen) closeMenu();
        else openMenu();
    }

    /**
     * Abre el menú con animación.
     */
    public void openMenu() {
        if (topSheet == null || overlay == null) return;

        topSheet.setVisibility(View.VISIBLE);
        topSheet.startAnimation(
                AnimationUtils.loadAnimation(activity, R.anim.top_sheet_down)
        );
        overlay.setVisibility(View.VISIBLE);
        isMenuOpen = true;
    }

    /**
     * Cierra el menú con animación.
     */
    public void closeMenu() {
        if (topSheet == null || overlay == null) return;

        topSheet.startAnimation(
                AnimationUtils.loadAnimation(activity, R.anim.top_sheet_up)
        );
        overlay.setVisibility(View.GONE);
        topSheet.setVisibility(View.GONE);
        isMenuOpen = false;
    }
}
