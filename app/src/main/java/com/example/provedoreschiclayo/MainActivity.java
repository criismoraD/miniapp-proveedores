package com.example.provedoreschiclayo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import com.example.provedoreschiclayo.adapters.ProductAdapter;
import com.example.provedoreschiclayo.data.DataRepository;
import com.example.provedoreschiclayo.models.CartItem;
import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;
import com.example.provedoreschiclayo.ui.Motion;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductActionListener {

    // Repositorio y Datos
    private List<DistrictLocation> locations;
    private DistrictLocation currentLocation;
    private List<Supplier> suppliers;
    private List<Product> allProducts;
    private final List<Product> filteredProducts = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();

    // Estado de filtros
    private String selectedCategory = "ALL";
    private String selectedSort = "convenient"; // "convenient", "price", "distance", "delivery"
    private String currentSearchQuery = "";

    // Cabecera (portada)
    private TextView tvMastheadDate;
    private TextView tvBrand;
    private TextView tvSelectedLocation;
    private View btnLocationPicker;
    private View btnOpenMap;
    private MaterialButton btnRoleToggle;

    // Búsqueda y filtros
    private TextInputEditText etSearch;
    private ChipGroup cgCategories;
    private ChipGroup cgSort;
    private Chip chipCatAll, chipCatDrinks, chipCatGroceries, chipCatSweets, chipCatCleaning;
    private Chip chipSortConvenient, chipSortPrice, chipSortDistance, chipSortDelivery;

    // Listado
    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private View llEmptyState;

    // Barra flotante de pedido
    private MaterialCardView cardCartBar;
    private View layoutCartBarContent;
    private TextView tvCartSummaryCountAndTotal;
    private TextView tvCartSummarySubtitle;
    private MaterialButton btnOpenCart;
    private int lastCartUnits = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initViews();
        setupEvents();
        refreshProductList(true);
        updateCartBar();

        if (savedInstanceState == null) {
            playEntranceAnimations();
        }
    }

    private void initData() {
        locations = DataRepository.getLocations();
        currentLocation = locations.get(0); // Por defecto Moshoqueque (J.L. Ortiz)
        suppliers = DataRepository.getSuppliers();
        allProducts = DataRepository.getProducts(suppliers);
    }

    private void initViews() {
        tvMastheadDate = findViewById(R.id.tvMastheadDate);
        tvBrand = findViewById(R.id.tvBrand);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        btnLocationPicker = findViewById(R.id.btnLocationPicker);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnRoleToggle = findViewById(R.id.btnRoleToggle);

        etSearch = findViewById(R.id.etSearch);

        cgCategories = findViewById(R.id.cgCategories);
        chipCatAll = findViewById(R.id.chipCatAll);
        chipCatDrinks = findViewById(R.id.chipCatDrinks);
        chipCatGroceries = findViewById(R.id.chipCatGroceries);
        chipCatSweets = findViewById(R.id.chipCatSweets);
        chipCatCleaning = findViewById(R.id.chipCatCleaning);

        cgSort = findViewById(R.id.cgSort);
        chipSortConvenient = findViewById(R.id.chipSortConvenient);
        chipSortPrice = findViewById(R.id.chipSortPrice);
        chipSortDistance = findViewById(R.id.chipSortDistance);
        chipSortDelivery = findViewById(R.id.chipSortDelivery);

        rvProducts = findViewById(R.id.rvProducts);
        llEmptyState = findViewById(R.id.llEmptyState);

        cardCartBar = findViewById(R.id.cardCartBar);
        layoutCartBarContent = findViewById(R.id.layoutCartBarContent);
        tvCartSummaryCountAndTotal = findViewById(R.id.tvCartSummaryCountAndTotal);
        tvCartSummarySubtitle = findViewById(R.id.tvCartSummarySubtitle);
        btnOpenCart = findViewById(R.id.btnOpenCart);

        tvSelectedLocation.setText(currentLocation.name);

        // Fecha de "edición" del día, en español
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));
        String today = dateFormat.format(new Date());
        tvMastheadDate.setText(today.substring(0, 1).toUpperCase(Locale.ROOT) + today.substring(1));

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(260);
        itemAnimator.setRemoveDuration(180);
        itemAnimator.setMoveDuration(260);
        itemAnimator.setChangeDuration(200);
        rvProducts.setItemAnimator(itemAnimator);

        productAdapter = new ProductAdapter(this, currentLocation, this);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupEvents() {
        // Selector de ubicación de bodega
        Motion.pressScale(btnLocationPicker);
        btnLocationPicker.setOnClickListener(v -> showLocationPickerDialog());

        // Mapa interactivo de Chiclayo
        Motion.pressScale(btnOpenMap);
        btnOpenMap.setOnClickListener(v -> showSuppliersMapDialog());

        // Modo (Bodeguero / Proveedor)
        btnRoleToggle.setOnClickListener(v -> showWholesalerModeDialog());

        // Búsqueda en tiempo real (sin animación de entrada para no distraer al escribir)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                refreshProductList(false);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Categorías (selección única)
        cgCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            Motion.popBump(group.findViewById(id));
            if (id == R.id.chipCatDrinks) {
                selectedCategory = "Bebidas";
            } else if (id == R.id.chipCatGroceries) {
                selectedCategory = "Abarrotes";
            } else if (id == R.id.chipCatSweets) {
                selectedCategory = "Golosinas";
            } else if (id == R.id.chipCatCleaning) {
                selectedCategory = "Limpieza";
            } else {
                selectedCategory = "ALL";
            }
            refreshProductList(true);
        });

        // Criterio de comparación (selección única)
        cgSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            Motion.popBump(group.findViewById(id));
            if (id == R.id.chipSortPrice) {
                selectSort("price");
            } else if (id == R.id.chipSortDistance) {
                selectSort("distance");
            } else if (id == R.id.chipSortDelivery) {
                selectSort("delivery");
            } else {
                selectSort("convenient");
            }
        });

        // Barra de pedido
        Motion.pressScale(layoutCartBarContent);
        layoutCartBarContent.setOnClickListener(v -> showCartDialog());
        btnOpenCart.setOnClickListener(v -> showCartDialog());
    }

    /** Entrada de la portada: máquina de escribir en el nombre y fundido de la cabecera. */
    private void playEntranceAnimations() {
        Motion.typewriter(tvBrand, "ProveeChiclayo", 900);

        View header = findViewById(R.id.llHeaderContainer);
        header.setAlpha(0f);
        header.setTranslationY(-Motion.dp(header, 10f));
        header.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80)
                .setDuration(520)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    private void selectSort(String sort) {
        this.selectedSort = sort;
        refreshProductList(true);

        String feedback;
        switch (sort) {
            case "price":
                feedback = "Ordenado por menor precio de producto";
                break;
            case "distance":
                feedback = "Ordenado por proveedores más cercanos a tu bodega";
                break;
            case "delivery":
                feedback = "Ordenado por menor flete de envío";
                break;
            default:
                feedback = "Ordenado por mayor conveniencia (producto + flete)";
                break;
        }
        showMessage(feedback);
    }

    /** Muestra avisos con Snackbar de Material, por encima de la barra de pedido si está visible. */
    private void showMessage(CharSequence message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT);
        if (cardCartBar.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(cardCartBar);
        }
        snackbar.show();
    }

    /**
     * Recalcula y muestra la lista de productos.
     * @param animateEntrance true para re-escalonar la entrada de las hojas (cambio de filtro u orden).
     */
    private void refreshProductList(boolean animateEntrance) {
        filteredProducts.clear();

        for (Product p : allProducts) {
            boolean matchesCat = "ALL".equalsIgnoreCase(selectedCategory) || p.category.equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || p.name.toLowerCase(Locale.ROOT).contains(currentSearchQuery)
                    || p.brand.toLowerCase(Locale.ROOT).contains(currentSearchQuery)
                    || p.category.toLowerCase(Locale.ROOT).contains(currentSearchQuery);

            if (matchesCat && matchesSearch) {
                filteredProducts.add(p);
            }
        }

        // Ordenar lista según el criterio activo
        Collections.sort(filteredProducts, (p1, p2) -> {
            SupplierOffer o1 = getOfferForSort(p1);
            SupplierOffer o2 = getOfferForSort(p2);
            if (o1 == null || o2 == null) return 0;

            if ("price".equals(selectedSort)) {
                return Double.compare(o1.price, o2.price);
            } else if ("distance".equals(selectedSort)) {
                double d1 = currentLocation.distanceTo(o1.supplier.lat, o1.supplier.lng);
                double d2 = currentLocation.distanceTo(o2.supplier.lat, o2.supplier.lng);
                return Double.compare(d1, d2);
            } else if ("delivery".equals(selectedSort)) {
                double f1 = o1.supplier.calculateDeliveryFee(currentLocation, o1.price);
                double f2 = o2.supplier.calculateDeliveryFee(currentLocation, o2.price);
                return Double.compare(f1, f2);
            } else {
                // Conveniente: costo total para 1 unidad
                double t1 = o1.calculateTotalCost(currentLocation, 1);
                double t2 = o2.calculateTotalCost(currentLocation, 1);
                return Double.compare(t1, t2);
            }
        });

        boolean isEmpty = filteredProducts.isEmpty();
        if (isEmpty) {
            rvProducts.setVisibility(View.GONE);
            if (llEmptyState.getVisibility() != View.VISIBLE) {
                Motion.fadeRiseIn(llEmptyState);
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
        }

        productAdapter.updateData(filteredProducts, currentLocation, selectedSort);
        if (animateEntrance && !isEmpty) {
            rvProducts.scheduleLayoutAnimation();
        }
    }

    private SupplierOffer getOfferForSort(Product product) {
        if (product.offers.isEmpty()) return null;
        if ("price".equals(selectedSort)) {
            return product.getLowestPriceOffer();
        } else if ("distance".equals(selectedSort)) {
            return product.getClosestOffer(currentLocation);
        } else if ("delivery".equals(selectedSort)) {
            SupplierOffer best = product.offers.get(0);
            double minFlete = best.supplier.calculateDeliveryFee(currentLocation, best.price);
            for (SupplierOffer o : product.offers) {
                double f = o.supplier.calculateDeliveryFee(currentLocation, o.price);
                if (f < minFlete) {
                    minFlete = f;
                    best = o;
                }
            }
            return best;
        } else {
            return product.getMostConvenientOffer(currentLocation, 1);
        }
    }

    // ==========================================
    // MAPA INTERACTIVO DE CHICLAYO & LAMBAYEQUE
    // ==========================================
    @SuppressLint("SetJavaScriptEnabled")
    private void showSuppliersMapDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_supplier_map);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.96),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.88)
            );
            dialog.getWindow().setWindowAnimations(R.style.Anim_ProvedoresChiclayo_Dialog);
        }

        MaterialButton btnClose = dialog.findViewById(R.id.btnMapClose);
        TextView tvBodegaSub = dialog.findViewById(R.id.tvMapBodegaSubtitle);
        final WebView wvMap = dialog.findViewById(R.id.wvMap);
        final TextView tvSupName = dialog.findViewById(R.id.tvMapSelectedSupplierName);
        final TextView tvSupDist = dialog.findViewById(R.id.tvMapSelectedSupplierDistance);
        final TextView tvSupDetails = dialog.findViewById(R.id.tvMapSelectedSupplierDetails);

        tvBodegaSub.setText("Tu bodega en: " + currentLocation.name);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        wvMap.getSettings().setJavaScriptEnabled(true);
        wvMap.getSettings().setDomStorageEnabled(true);
        wvMap.setWebChromeClient(new WebChromeClient());
        wvMap.setWebViewClient(new WebViewClient());

        // Puente JavaScript -> Android para los clics en marcadores
        class MapInterface {
            @JavascriptInterface
            public void onSupplierClicked(final String name, final String district, final String address, final double dist, final double flete, final String time) {
                runOnUiThread(() -> {
                    Motion.fadeSwapText(tvSupName, name);
                    Motion.fadeSwapText(tvSupDist, String.format(Locale.US, "%.1f km", dist));
                    Motion.fadeSwapText(tvSupDetails, String.format(Locale.US,
                            "%s (%s) · Flete: S/ %.2f · %s", address, district, flete, time));
                });
            }
        }
        wvMap.addJavascriptInterface(new MapInterface(), "AndroidMap");

        // Construir HTML del mapa con Leaflet
        StringBuilder jsSuppliers = new StringBuilder("[");
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier s = suppliers.get(i);
            double dist = currentLocation.distanceTo(s.lat, s.lng);
            double flete = s.calculateDeliveryFee(currentLocation, 50.0);
            jsSuppliers.append(String.format(Locale.US,
                    "{name: '%s', district: '%s', address: '%s', lat: %.5f, lng: %.5f, dist: %.1f, flete: %.2f, time: '%s', badge: '%s'}",
                    s.name, s.district, s.address, s.lat, s.lng, dist, flete, s.deliveryTimeEstimate, s.badge));
            if (i < suppliers.size() - 1) jsSuppliers.append(",");
        }
        jsSuppliers.append("]");

        String html = "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'/>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>"
                + "  html, body { height: 100%; margin: 0; padding: 0; background: #F2ECDF; font-family: Georgia, serif; }"
                + "  #map { height: 100%; width: 100%; }"
                + "  .leaflet-tile-pane { filter: sepia(0.30) saturate(0.82) contrast(0.96); }"
                + "  .leaflet-popup-content-wrapper { border-radius: 6px; border: 1px solid #9C917A; box-shadow: 0 4px 14px rgba(30,35,49,0.18); background: #FBF8F1; }"
                + "  .leaflet-popup-tip { background: #FBF8F1; }"
                + "  .bodega-marker { background: #A8803B; border: 3px solid #1E2331; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 0 0 4px rgba(168,128,59,0.25); animation: pulse 2.4s ease-in-out infinite; }"
                + "  .sup-marker { background: #1E2331; border: 2px solid #A8803B; border-radius: 50%; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 3px 7px rgba(30,35,49,0.35); }"
                + "  @keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.12); } 100% { transform: scale(1); } }"
                + "</style>"
                + "</head><body>"
                + "<div id='map'></div>"
                + "<script>"
                + "  var bodegaLat = " + currentLocation.lat + ";"
                + "  var bodegaLng = " + currentLocation.lng + ";"
                + "  var suppliers = " + jsSuppliers.toString() + ";"
                + "  try {"
                + "    var map = L.map('map', {zoomControl: true}).setView([bodegaLat, bodegaLng], 13);"
                + "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {"
                + "      maxZoom: 18,"
                + "      attribution: 'Chiclayo Mayorista'"
                + "    }).addTo(map);"
                + "    var bodegaIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\\\"bodega-marker\\\">&#9632;</div>', iconSize: [34, 34], iconAnchor: [17, 17] });"
                + "    var bMarker = L.marker([bodegaLat, bodegaLng], {icon: bodegaIcon}).addTo(map);"
                + "    bMarker.bindPopup('<b style=\\\"color:#1E2331; font-size:14px;\\\">TU BODEGA</b><br/><span style=\\\"color:#6E6755; font-size:12px;\\\">" + currentLocation.name + "</span>').openPopup();"
                + "    suppliers.forEach(function(s) {"
                + "      var supIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\\\"sup-marker\\\">&#9650;</div>', iconSize: [30, 30], iconAnchor: [15, 15] });"
                + "      var m = L.marker([s.lat, s.lng], {icon: supIcon}).addTo(map);"
                + "      var popupHtml = '<div style=\\\"font-size:13px; min-width:180px;\\\">' +"
                + "        '<b style=\\\"color:#1E2331; font-size:15px;\\\">' + s.name + '</b><br/>' +"
                + "        '<span style=\\\"color:#6E6755;\\\">' + s.address + ' (' + s.district + ')</span><br/>' +"
                + "        '<div style=\\\"margin:6px 0; padding:3px 8px; background:#F1E6CC; border:1px solid #A8803B; border-radius:4px; font-weight:bold; color:#6B4F17;\\\">' + s.badge + '</div>' +"
                + "        '<b style=\\\"color:#1E2331;\\\">Distancia: </b>' + s.dist + ' km<br/>' +"
                + "        '<b style=\\\"color:#3B6A50;\\\">Flete: </b>S/ ' + s.flete.toFixed(2) + '<br/>' +"
                + "        '<span style=\\\"color:#6E6755;\\\">' + s.time + '</span>' +"
                + "        '</div>';"
                + "      m.bindPopup(popupHtml);"
                + "      m.on('click', function() {"
                + "        if (window.AndroidMap) { window.AndroidMap.onSupplierClicked(s.name, s.district, s.address, s.dist, s.flete, s.time); }"
                + "      });"
                + "      L.polyline([[bodegaLat, bodegaLng], [s.lat, s.lng]], {color: '#A8803B', weight: 2, dashArray: '4, 7', opacity: 0.9}).addTo(map);"
                + "    });"
                + "  } catch(e) {"
                + "    document.getElementById('map').innerHTML = '<div style=\\\"color:#1E2331; padding:20px; text-align:center; font-family:Georgia,serif;\\\"><h3>Mapa no disponible</h3><p>Revisa tu conexión a internet</p></div>';"
                + "  }"
                + "</script></body></html>";

        wvMap.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null);

        dialog.show();
    }

    // ==========================================
    // HOJA SELECTORA DE UBICACIÓN
    // ==========================================
    private void showLocationPickerDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_location_picker);

        MaterialButton btnClose = dialog.findViewById(R.id.btnLocationClose);
        LinearLayout llContainer = dialog.findViewById(R.id.llLocationListContainer);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        LayoutInflater inflater = LayoutInflater.from(this);
        List<View> rows = new ArrayList<>();
        for (DistrictLocation loc : locations) {
            View itemView = inflater.inflate(R.layout.item_location_option, llContainer, false);
            TextView tvName = itemView.findViewById(R.id.tvLocationName);
            TextView tvRef = itemView.findViewById(R.id.tvLocationReference);
            TextView tvBadge = itemView.findViewById(R.id.tvLocationStatusBadge);

            tvName.setText(loc.name);
            tvRef.setText(loc.reference);

            boolean isCurrent = loc.id.equals(currentLocation.id);
            if (isCurrent) {
                tvBadge.setText("ACTUAL");
                tvBadge.setBackgroundResource(R.drawable.bg_pill_gilt);
                tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.gilt_dark));
                itemView.setBackgroundResource(R.drawable.bg_paper_card_active);
            } else {
                tvBadge.setText("Elegir");
                tvBadge.setBackgroundResource(R.drawable.bg_pill_rule);
                tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ink));
                itemView.setBackgroundResource(R.drawable.bg_paper_card);
            }

            itemView.setOnClickListener(v -> {
                currentLocation = loc;
                tvSelectedLocation.setText(loc.name);
                dialog.dismiss();
                refreshProductList(true);
                updateCartBar();
                showMessage("Fletes recalculados para " + loc.name);
            });

            llContainer.addView(itemView);
            rows.add(itemView);
        }

        dialog.show();
        Motion.staggerIn(rows, 60);
    }

    // ==========================================
    // HOJA VISTA PROVEEDOR (PRÓXIMAMENTE)
    // ==========================================
    private void showWholesalerModeDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_wholesaler_mode);

        MaterialButton btnClose = dialog.findViewById(R.id.btnWholesalerClose);
        MaterialButton btnContinue = dialog.findViewById(R.id.btnContinueAsBuyer);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnContinue.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ==========================================
    // COMPARADOR DE PROVEEDORES
    // ==========================================
    @Override
    public void onCompareSuppliers(Product product) {
        showSupplierComparisonDialog(product);
    }

    @Override
    public void onQuickAddToCart(Product product, SupplierOffer offer) {
        addToCart(product, offer, 1);
        showMessage("1× " + product.name + " añadido · " + offer.supplier.name);
    }

    private void showSupplierComparisonDialog(final Product product) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_compare_suppliers);

        ImageView ivProduct = dialog.findViewById(R.id.ivCompareProductImage);
        TextView tvName = dialog.findViewById(R.id.tvCompareProductName);
        TextView tvMeta = dialog.findViewById(R.id.tvCompareProductMeta);
        TextView tvDestination = dialog.findViewById(R.id.tvCompareDestination);
        MaterialButton btnClose = dialog.findViewById(R.id.btnCompareClose);
        final TextView tvQuantity = dialog.findViewById(R.id.tvCompareQuantity);
        MaterialButton btnMinus = dialog.findViewById(R.id.btnQtyMinus);
        MaterialButton btnPlus = dialog.findViewById(R.id.btnQtyPlus);
        final LinearLayout llContainer = dialog.findViewById(R.id.llSuppliersListContainer);

        if (product.imageResId != 0) {
            ivProduct.setImageResource(product.imageResId);
        } else {
            ivProduct.setImageResource(R.drawable.img_sporade);
        }

        tvName.setText(product.name);
        tvMeta.setText(product.presentation + " · " + product.brand);
        tvDestination.setText("Cotizando fletes a: " + currentLocation.name);

        final int[] currentQty = {1};
        renderSupplierOffers(dialog, llContainer, product, currentQty[0], true);

        btnPlus.setOnClickListener(v -> {
            if (currentQty[0] < 50) {
                currentQty[0]++;
                tvQuantity.setText(String.valueOf(currentQty[0]));
                Motion.popBump(tvQuantity);
                renderSupplierOffers(dialog, llContainer, product, currentQty[0], false);
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (currentQty[0] > 1) {
                currentQty[0]--;
                tvQuantity.setText(String.valueOf(currentQty[0]));
                Motion.popBump(tvQuantity);
                renderSupplierOffers(dialog, llContainer, product, currentQty[0], false);
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** Pinta las ofertas ordenadas por costo total para la cantidad indicada. */
    private void renderSupplierOffers(final Dialog dialog, final LinearLayout container,
                                      final Product product, final int qty, boolean animate) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        // Oferta más conveniente para esta cantidad específica
        SupplierOffer bestTotalOffer = product.getMostConvenientOffer(currentLocation, qty);
        SupplierOffer lowestPriceOffer = product.getLowestPriceOffer();
        SupplierOffer closestOffer = product.getClosestOffer(currentLocation);

        // Ordenar ofertas por costo total con flete
        List<SupplierOffer> sortedOffers = new ArrayList<>(product.offers);
        Collections.sort(sortedOffers, (o1, o2) -> {
            double t1 = o1.calculateTotalCost(currentLocation, qty);
            double t2 = o2.calculateTotalCost(currentLocation, qty);
            return Double.compare(t1, t2);
        });

        List<View> rows = new ArrayList<>();
        for (final SupplierOffer offer : sortedOffers) {
            View itemView = inflater.inflate(R.layout.item_supplier_comparison, container, false);

            TextView tvSupName = itemView.findViewById(R.id.tvSupplierName);
            TextView tvSupDist = itemView.findViewById(R.id.tvSupplierDistrictAndDistance);
            TextView tvSupBadge = itemView.findViewById(R.id.tvSupplierBadge);
            TextView tvNote = itemView.findViewById(R.id.tvOfferNote);
            TextView tvShipping = itemView.findViewById(R.id.tvFreeShippingCondition);
            TextView tvUnit = itemView.findViewById(R.id.tvUnitPrice);
            TextView tvFlete = itemView.findViewById(R.id.tvFleteCost);
            TextView tvTotal = itemView.findViewById(R.id.tvFinalTotal);
            MaterialButton btnSelect = itemView.findViewById(R.id.btnSelectSupplier);
            MaterialCardView rootCard = itemView.findViewById(R.id.cardSupplierComparison);

            double dist = currentLocation.distanceTo(offer.supplier.lat, offer.supplier.lng);
            double subtotal = offer.price * qty;
            double flete = offer.supplier.calculateDeliveryFee(currentLocation, subtotal);
            double total = subtotal + flete;

            tvSupName.setText(offer.supplier.name);
            tvSupDist.setText(String.format(Locale.US, "%.1f km • %s • %s", dist, offer.supplier.district, offer.supplier.badge));
            tvNote.setText(offer.note + " • Stock: " + offer.stock + " unid.");

            if (subtotal >= offer.supplier.freeShippingThreshold) {
                tvShipping.setText("Envío gratis aplicado · superó S/ " + String.format(Locale.US, "%.0f", offer.supplier.freeShippingThreshold));
                tvShipping.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.sage));
                tvFlete.setText("GRATIS");
                tvFlete.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.sage));
            } else {
                double missing = offer.supplier.freeShippingThreshold - subtotal;
                tvShipping.setText(String.format(Locale.US, "%s · Envío gratis desde S/ %.0f (faltan S/ %.2f)",
                        offer.supplier.deliveryTimeEstimate, offer.supplier.freeShippingThreshold, missing));
                tvShipping.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.ink_muted));
                tvFlete.setText(String.format(Locale.US, "S/ %.2f", flete));
                tvFlete.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.ink));
            }

            tvUnit.setText(String.format(Locale.US, "S/ %.2f (%d u.)", subtotal, qty));
            tvTotal.setText(String.format(Locale.US, "S/ %.2f", total));

            // Distintivos
            if (offer == bestTotalOffer) {
                tvSupBadge.setVisibility(View.VISIBLE);
                tvSupBadge.setText("MEJOR TOTAL");
                tvSupBadge.setBackgroundResource(R.drawable.bg_pill_gilt);
                tvSupBadge.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.gilt_dark));
                rootCard.setStrokeColor(androidx.core.content.ContextCompat.getColorStateList(MainActivity.this, R.color.gilt));
                rootCard.setStrokeWidth((int) Motion.dp(rootCard, 1.25f));
            } else if (offer == lowestPriceOffer) {
                tvSupBadge.setVisibility(View.VISIBLE);
                tvSupBadge.setText("MENOR PRECIO");
                tvSupBadge.setBackgroundResource(R.drawable.bg_pill_rule);
                tvSupBadge.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.ink));
            } else if (offer == closestOffer) {
                tvSupBadge.setVisibility(View.VISIBLE);
                tvSupBadge.setText("MÁS CERCANO");
                tvSupBadge.setBackgroundResource(R.drawable.bg_pill_sage);
                tvSupBadge.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.sage));
            } else {
                tvSupBadge.setVisibility(View.GONE);
            }

            btnSelect.setOnClickListener(v -> {
                addToCart(product, offer, qty);
                dialog.dismiss();
                showMessage("Agregado " + qty + "× " + product.name + " (" + offer.supplier.name + ")");
            });

            container.addView(itemView);
            rows.add(itemView);
        }

        if (animate) {
            Motion.staggerIn(rows, 70);
        }
    }

    // ==========================================
    // GESTIÓN DEL CARRITO Y PEDIDOS
    // ==========================================
    private void addToCart(Product product, SupplierOffer offer, int qty) {
        boolean found = false;
        for (CartItem item : cartItems) {
            if (item.product.id.equals(product.id) && item.offer.supplier.id.equals(offer.supplier.id)) {
                item.quantity += qty;
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(new CartItem(product, offer, qty));
        }
        updateCartBar();
    }

    private void updateCartBar() {
        if (cartItems.isEmpty()) {
            if (cardCartBar.getVisibility() == View.VISIBLE) {
                Motion.slideDownOut(cardCartBar);
            }
            lastCartUnits = 0;
            return;
        }

        int totalUnits = 0;
        double productsSubtotal = 0.0;

        // Subtotales por proveedor para calcular fletes correctos
        Map<String, Double> supplierSubtotals = new HashMap<>();
        Map<String, Supplier> supplierMap = new HashMap<>();

        for (CartItem item : cartItems) {
            totalUnits += item.quantity;
            double lineTotal = item.getSubtotal();
            productsSubtotal += lineTotal;

            String sId = item.offer.supplier.id;
            supplierMap.put(sId, item.offer.supplier);
            double currentSub = supplierSubtotals.containsKey(sId) ? supplierSubtotals.get(sId) : 0.0;
            supplierSubtotals.put(sId, currentSub + lineTotal);
        }

        double totalFlete = 0.0;
        for (Map.Entry<String, Double> entry : supplierSubtotals.entrySet()) {
            Supplier s = supplierMap.get(entry.getKey());
            if (s != null) {
                totalFlete += s.calculateDeliveryFee(currentLocation, entry.getValue());
            }
        }

        double grandTotal = productsSubtotal + totalFlete;

        boolean wasHidden = cardCartBar.getVisibility() != View.VISIBLE;
        tvCartSummaryCountAndTotal.setText(String.format(Locale.US, "%d %s · S/ %.2f",
                totalUnits, totalUnits == 1 ? "ítem" : "ítems", grandTotal));
        tvCartSummarySubtitle.setText(String.format(Locale.US, "Incluye S/ %.2f de flete a %s", totalFlete, currentLocation.name));

        if (wasHidden) {
            Motion.slideUpIn(cardCartBar);
        } else if (totalUnits != lastCartUnits) {
            Motion.popBump(tvCartSummaryCountAndTotal);
        }
        lastCartUnits = totalUnits;
    }

    private void showCartDialog() {
        if (cartItems.isEmpty()) {
            showMessage("El pedido está vacío");
            return;
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_cart);

        MaterialButton btnClose = dialog.findViewById(R.id.btnCartClose);
        TextView tvDest = dialog.findViewById(R.id.tvCartDestination);
        final LinearLayout llContainer = dialog.findViewById(R.id.llCartItemsContainer);
        final TextView tvSubtotal = dialog.findViewById(R.id.tvCartSubtotal);
        final TextView tvFlete = dialog.findViewById(R.id.tvCartTotalFlete);
        final TextView tvGrandTotal = dialog.findViewById(R.id.tvCartGrandTotal);
        MaterialButton btnSendWhatsApp = dialog.findViewById(R.id.btnSendOrderWhatsApp);
        MaterialButton btnClear = dialog.findViewById(R.id.btnClearCart);

        tvDest.setText("Destino: " + currentLocation.name);

        renderCartSheet(dialog, llContainer, tvSubtotal, tvFlete, tvGrandTotal, true);

        // Enviar pedido vía WhatsApp
        btnSendWhatsApp.setOnClickListener(v -> {
            sendOrderViaWhatsApp();
            dialog.dismiss();
        });

        // Vaciar pedido
        btnClear.setOnClickListener(v -> {
            cartItems.clear();
            dialog.dismiss();
            updateCartBar();
            showMessage("Pedido vaciado");
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** Pinta las líneas del pedido y los totales. Se llama al abrir y tras cada cambio de cantidad. */
    private void renderCartSheet(final BottomSheetDialog dialog, final LinearLayout llContainer,
                                 final TextView tvSubtotal, final TextView tvFlete,
                                 final TextView tvGrandTotal, boolean animateEntrance) {
        llContainer.removeAllViews();
        if (cartItems.isEmpty()) {
            dialog.dismiss();
            updateCartBar();
            return;
        }

        double subtotalAll = 0.0;
        Map<String, Double> supplierSubtotals = new HashMap<>();
        Map<String, Supplier> supplierMap = new HashMap<>();

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        List<View> rows = new ArrayList<>();

        for (final CartItem item : cartItems) {
            double lineSubtotal = item.getSubtotal();
            subtotalAll += lineSubtotal;

            String sId = item.offer.supplier.id;
            supplierMap.put(sId, item.offer.supplier);
            double curr = supplierSubtotals.containsKey(sId) ? supplierSubtotals.get(sId) : 0.0;
            supplierSubtotals.put(sId, curr + lineSubtotal);

            View itemView = inflater.inflate(R.layout.item_cart_entry, llContainer, false);
            ImageView ivItemImage = itemView.findViewById(R.id.ivCartItemImage);
            TextView tvTitle = itemView.findViewById(R.id.tvCartItemTitle);
            TextView tvSupplier = itemView.findViewById(R.id.tvCartItemSupplier);
            final TextView tvQty = itemView.findViewById(R.id.tvCartItemQuantity);
            TextView tvItemSubtotal = itemView.findViewById(R.id.tvCartItemSubtotal);
            TextView tvUnitPrice = itemView.findViewById(R.id.tvCartItemUnitPrice);
            MaterialButton btnRemove = itemView.findViewById(R.id.btnRemoveCartItem);
            MaterialButton btnMinus = itemView.findViewById(R.id.btnCartItemMinus);
            MaterialButton btnPlus = itemView.findViewById(R.id.btnCartItemPlus);

            if (item.product.imageResId != 0) {
                ivItemImage.setImageResource(item.product.imageResId);
            } else {
                ivItemImage.setImageResource(R.drawable.img_sporade);
            }

            tvTitle.setText(item.product.name);
            tvSupplier.setText(item.offer.supplier.name);
            tvQty.setText(String.valueOf(item.quantity));
            tvUnitPrice.setText(String.format(Locale.US, "S/ %.2f c/u", item.offer.price));
            tvItemSubtotal.setText(String.format(Locale.US, "S/ %.2f", lineSubtotal));

            btnPlus.setOnClickListener(v -> {
                item.quantity++;
                Motion.popBump(tvQty);
                renderCartSheet(dialog, llContainer, tvSubtotal, tvFlete, tvGrandTotal, false);
                updateCartBar();
            });

            btnMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                } else {
                    cartItems.remove(item);
                }
                renderCartSheet(dialog, llContainer, tvSubtotal, tvFlete, tvGrandTotal, false);
                updateCartBar();
            });

            btnRemove.setOnClickListener(v -> {
                cartItems.remove(item);
                renderCartSheet(dialog, llContainer, tvSubtotal, tvFlete, tvGrandTotal, false);
                updateCartBar();
            });

            llContainer.addView(itemView);
            rows.add(itemView);
        }

        double fleteAll = 0.0;
        for (Map.Entry<String, Double> entry : supplierSubtotals.entrySet()) {
            Supplier s = supplierMap.get(entry.getKey());
            if (s != null) {
                fleteAll += s.calculateDeliveryFee(currentLocation, entry.getValue());
            }
        }

        double grandTotal = subtotalAll + fleteAll;

        tvSubtotal.setText(String.format(Locale.US, "S/ %.2f", subtotalAll));
        tvFlete.setText(String.format(Locale.US, "S/ %.2f", fleteAll));
        tvGrandTotal.setText(String.format(Locale.US, "S/ %.2f", grandTotal));

        if (animateEntrance) {
            Motion.staggerIn(rows, 70);
        }
    }

    private void sendOrderViaWhatsApp() {
        if (cartItems.isEmpty()) return;

        // Mensaje formal de abastecimiento B2B
        StringBuilder message = new StringBuilder();
        message.append("📦 *PEDIDO DE ABASTECIMIENTO MAYORISTA*\n");
        message.append("🏪 *Bodega Destino*: Mi Bodega Chiclayo\n");
        message.append("📍 *Ubicación de Entrega*: ").append(currentLocation.name).append("\n");
        message.append("🕒 *Referencia*: ").append(currentLocation.reference).append("\n\n");

        // Agrupar items por proveedor
        Map<String, List<CartItem>> grouped = new HashMap<>();
        for (CartItem item : cartItems) {
            String sId = item.offer.supplier.id;
            if (!grouped.containsKey(sId)) {
                grouped.put(sId, new ArrayList<>());
            }
            grouped.get(sId).add(item);
        }

        double grandTotal = 0.0;

        for (Map.Entry<String, List<CartItem>> entry : grouped.entrySet()) {
            Supplier sup = entry.getValue().get(0).offer.supplier;
            message.append("🏢 *DISTRIBUIDOR: ").append(sup.name.toUpperCase(Locale.ROOT)).append("*\n");
            message.append("📍 Almacén: ").append(sup.address).append(" (").append(sup.district).append(")\n");

            double supSubtotal = 0.0;
            for (CartItem ci : entry.getValue()) {
                double line = ci.getSubtotal();
                supSubtotal += line;
                message.append("  • ").append(ci.quantity).append("x ")
                        .append(ci.product.name).append(" (").append(ci.product.presentation).append(")")
                        .append(" - S/ ").append(String.format(Locale.US, "%.2f", line)).append("\n");
            }

            double supFlete = sup.calculateDeliveryFee(currentLocation, supSubtotal);
            double supTotal = supSubtotal + supFlete;
            grandTotal += supTotal;

            message.append("  Subtotal: S/ ").append(String.format(Locale.US, "%.2f", supSubtotal)).append("\n");
            if (supFlete == 0.0) {
                message.append("  Flete: GRATIS (Promoción volumen alcanzada)\n");
            } else {
                message.append("  Flete moto/furgón: S/ ").append(String.format(Locale.US, "%.2f", supFlete)).append("\n");
            }
            message.append("  *Total con Proveedor: S/ ").append(String.format(Locale.US, "%.2f", supTotal)).append("*\n\n");
        }

        message.append("=========================\n");
        message.append("💰 *TOTAL GENERAL DEL PEDIDO: S/ ").append(String.format(Locale.US, "%.2f", grandTotal)).append("*\n");
        message.append("Solicitado mediante la app ProveeChiclayo B2B.");

        try {
            String encoded = URLEncoder.encode(message.toString(), "UTF-8");
            // Teléfono del primer proveedor para el enlace de WhatsApp
            String phone = cartItems.get(0).offer.supplier.phone;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=51" + phone + "&text=" + encoded));
            startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
            startActivity(Intent.createChooser(sendIntent, "Enviar Pedido Mayorista"));
        } catch (Exception e) {
            showMessage("Abriendo selector de mensajería…");
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
            startActivity(Intent.createChooser(sendIntent, "Enviar Pedido"));
        }
    }
}
