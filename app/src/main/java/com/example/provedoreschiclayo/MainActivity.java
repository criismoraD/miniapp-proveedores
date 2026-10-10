package com.example.provedoreschiclayo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.provedoreschiclayo.adapters.ProductAdapter;
import com.example.provedoreschiclayo.data.DataRepository;
import com.example.provedoreschiclayo.models.CartItem;
import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProveeChiclayo - B2B Wholesale Marketplace
 * Initial Corporate Navy & Gold Design (commit c784cfe)
 */
public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductActionListener {

    // GPS Constants & Permission Launcher
    private static final long GPS_MAX_AGE_MS = 10 * 60 * 1000L;
    private static final long GPS_TIMEOUT_MS = 20 * 1000L;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private List<DistrictLocation> locations;
    private DistrictLocation currentLocation;
    private DistrictLocation customLocation;
    private List<Supplier> suppliers;
    private List<Product> allProducts;
    private final List<Product> filteredProducts = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();

    // Estado de filtros
    private String selectedCategory = "ALL";
    private String selectedSort = "convenient"; // "convenient", "price", "distance", "delivery"
    private String currentSearchQuery = "";

    // Vistas Principales
    private TextView tvSelectedLocation;
    private LinearLayout btnLocationPicker;
    private LinearLayout btnOpenMap;
    private LinearLayout btnRoleToggle;
    private TextView tvRoleLabel;

    private EditText etSearch;
    private TextView btnClearSearch;

    private TextView chipCatAll, chipCatDrinks, chipCatGroceries, chipCatSweets, chipCatCleaning;

    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private LinearLayout llEmptyState;

    // Barra flotante de Carrito
    private CardView cardCartBar;
    private LinearLayout layoutCartBarContent;
    private TextView tvCartSummaryCountAndTotal;
    private TextView tvCartSummarySubtitle;
    private TextView btnOpenCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onLocationPermissionResult);

        setContentView(R.layout.activity_main);

        initData();
        initViews();
        setupEvents();
        refreshProductList();
        updateCartBar();

        // Solicitar activación de GPS automáticamente desde el inicio
        new Handler(Looper.getMainLooper()).postDelayed(this::requestGpsLocation, 600);
    }

    private void initData() {
        locations = DataRepository.getLocations();
        currentLocation = locations.get(0); // Por defecto Moshoqueque (J.L. Ortiz)
        suppliers = DataRepository.getSuppliers();
        allProducts = DataRepository.getProducts(suppliers);
    }

    private void initViews() {
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        btnLocationPicker = findViewById(R.id.btnLocationPicker);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnRoleToggle = findViewById(R.id.btnRoleToggle);
        tvRoleLabel = findViewById(R.id.tvRoleLabel);

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        chipCatAll = findViewById(R.id.chipCatAll);
        chipCatDrinks = findViewById(R.id.chipCatDrinks);
        chipCatGroceries = findViewById(R.id.chipCatGroceries);
        chipCatSweets = findViewById(R.id.chipCatSweets);
        chipCatCleaning = findViewById(R.id.chipCatCleaning);

        rvProducts = findViewById(R.id.rvProducts);
        llEmptyState = findViewById(R.id.llEmptyState);

        cardCartBar = findViewById(R.id.cardCartBar);
        layoutCartBarContent = findViewById(R.id.layoutCartBarContent);
        tvCartSummaryCountAndTotal = findViewById(R.id.tvCartSummaryCountAndTotal);
        tvCartSummarySubtitle = findViewById(R.id.tvCartSummarySubtitle);
        btnOpenCart = findViewById(R.id.btnOpenCart);

        tvSelectedLocation.setText(currentLocation.name);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, currentLocation, this);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupEvents() {
        // Selector de ubicación de Bodega
        btnLocationPicker.setOnClickListener(v -> showLocationPickerDialog());

        // Botón para ver el Mapa interactivo de Chiclayo
        btnOpenMap.setOnClickListener(v -> showSuppliersMapDialog());

        // Selector / Indicador de Modo (Bodeguero / Proveedor)
        btnRoleToggle.setOnClickListener(v -> showWholesalerModeDialog());

        // Búsqueda en tiempo real
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                refreshProductList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                currentSearchQuery = "";
                btnClearSearch.setVisibility(View.GONE);
                refreshProductList();
            });
        }

        // Filtros de Categoría
        chipCatAll.setOnClickListener(v -> selectCategory("ALL", chipCatAll));
        chipCatDrinks.setOnClickListener(v -> selectCategory("Bebidas", chipCatDrinks));
        chipCatGroceries.setOnClickListener(v -> selectCategory("Abarrotes", chipCatGroceries));
        chipCatSweets.setOnClickListener(v -> selectCategory("Golosinas", chipCatSweets));
        chipCatCleaning.setOnClickListener(v -> selectCategory("Limpieza", chipCatCleaning));

        // Click en la barra de carrito
        layoutCartBarContent.setOnClickListener(v -> showCartDialog());
        btnOpenCart.setOnClickListener(v -> showCartDialog());
    }

    private void selectCategory(String category, TextView selectedChip) {
        this.selectedCategory = category;
        resetCategoryChipsStyle();
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected);
        selectedChip.setTextColor(Color.WHITE);
        refreshProductList();
    }

    private void resetCategoryChipsStyle() {
        TextView[] chips = {chipCatAll, chipCatDrinks, chipCatGroceries, chipCatSweets, chipCatCleaning};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void refreshProductList() {
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
                // Conveniente: Total costo para 1 unidad
                double t1 = o1.calculateTotalCost(currentLocation, 1);
                double t2 = o2.calculateTotalCost(currentLocation, 1);
                return Double.compare(t1, t2);
            }
        });

        if (filteredProducts.isEmpty()) {
            rvProducts.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvProducts.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }

        productAdapter.updateData(filteredProducts, currentLocation, selectedSort);
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
        }

        TextView btnClose = dialog.findViewById(R.id.btnMapClose);
        TextView tvBodegaSub = dialog.findViewById(R.id.tvMapBodegaSubtitle);
        final WebView wvMap = dialog.findViewById(R.id.wvMap);
        final TextView tvSupName = dialog.findViewById(R.id.tvMapSelectedSupplierName);
        final TextView tvSupDist = dialog.findViewById(R.id.tvMapSelectedSupplierDistance);
        final TextView tvSupDetails = dialog.findViewById(R.id.tvMapSelectedSupplierDetails);

        tvBodegaSub.setText("📍 Tu Bodega en: " + currentLocation.name);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        wvMap.getSettings().setJavaScriptEnabled(true);
        wvMap.getSettings().setDomStorageEnabled(true);
        wvMap.setWebChromeClient(new WebChromeClient());
        wvMap.setWebViewClient(new WebViewClient());

        // Interface para recibir clicks de marcadores en Android
        class MapInterface {
            @JavascriptInterface
            public void onSupplierClicked(final String name, final String district, final String address, final double dist, final double flete, final String time) {
                runOnUiThread(() -> {
                    tvSupName.setText("🏢 " + name);
                    tvSupDist.setText(String.format(Locale.US, "📍 %.1f km", dist));
                    tvSupDetails.setText(String.format(Locale.US, "%s (%s) • Flete: S/ %.2f • ⏱️ %s", address, district, flete, time));
                });
            }
        }
        wvMap.addJavascriptInterface(new MapInterface(), "AndroidMap");

        // Construir HTML del mapa interactivo con Leaflet
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
                + "  html, body { height: 100%; margin: 0; padding: 0; background: #0A192F; font-family: -apple-system, sans-serif; }"
                + "  #map { height: 100%; width: 100%; }"
                + "  .leaflet-popup-content-wrapper { border-radius: 14px; border: 2px solid #D4AF37; box-shadow: 0 6px 16px rgba(0,0,0,0.3); padding: 4px; }"
                + "  .bodega-marker { background: #D4AF37; border: 3px solid #0A192F; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 0 14px #D4AF37; animation: pulse 2s infinite; }"
                + "  .sup-marker { background: #0A192F; border: 2px solid #D4AF37; border-radius: 50%; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 4px 8px rgba(0,0,0,0.4); }"
                + "  @keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.15); } 100% { transform: scale(1); } }"
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
                + "    var bodegaIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\"bodega-marker\">🏪</div>', iconSize: [34, 34], iconAnchor: [17, 17] });"
                + "    var bMarker = L.marker([bodegaLat, bodegaLng], {icon: bodegaIcon}).addTo(map);"
                + "    bMarker.bindPopup('<b style=\"color:#0A192F; font-size:14px;\">🏪 TU BODEGA</b><br/><span style=\"color:#64748B; font-size:12px;\">" + currentLocation.name + "</span>').openPopup();"
                + "    suppliers.forEach(function(s) {"
                + "      var supIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\"sup-marker\">🏢</div>', iconSize: [30, 30], iconAnchor: [15, 15] });"
                + "      var m = L.marker([s.lat, s.lng], {icon: supIcon}).addTo(map);"
                + "      var popupHtml = '<div style=\"font-size:13px; min-width:180px;\">' +"
                + "        '<b style=\"color:#0A192F; font-size:14px;\">' + s.name + '</b><br/>' +"
                + "        '<span style=\"color:#64748B;\">📍 ' + s.address + ' (' + s.district + ')</span><br/>' +"
                + "        '<div style=\"margin:6px 0; padding:4px 8px; background:#FEF3C7; border-radius:6px; font-weight:bold; color:#996515;\">' + s.badge + '</div>' +"
                + "        '<b style=\"color:#0A192F;\">📏 Distancia: </b>' + s.dist + ' km<br/>' +"
                + "        '<b style=\"color:#059669;\">🚚 Flete: </b>S/ ' + s.flete.toFixed(2) + '<br/>' +"
                + "        '<span style=\"color:#64748B;\">⏱️ ' + s.time + '</span>' +"
                + "        '</div>';"
                + "      m.bindPopup(popupHtml);"
                + "      m.on('click', function() {"
                + "        if (window.AndroidMap) { window.AndroidMap.onSupplierClicked(s.name, s.district, s.address, s.dist, s.flete, s.time); }"
                + "      });"
                + "      L.polyline([[bodegaLat, bodegaLng], [s.lat, s.lng]], {color: '#D4AF37', weight: 2.5, dashArray: '5, 8', opacity: 0.8}).addTo(map);"
                + "    });"
                + "  } catch(e) {"
                + "    document.getElementById('map').innerHTML = '<div style=\"color:white; padding:20px; text-align:center;\"><h3>Radar de Chiclayo</h3><p>Modo visual activado</p></div>';"
                + "  }"
                + "</script></body></html>";

        wvMap.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null);

        dialog.show();
    }

    // ==========================================
    // MAPA INDIVIDUAL DE UN PROVEEDOR ESPECÍFICO
    // ==========================================
    @SuppressLint("SetJavaScriptEnabled")
    private void showSingleSupplierMapDialog(final Supplier supplier) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_supplier_map);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.96),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.88)
            );
        }

        TextView btnClose = dialog.findViewById(R.id.btnMapClose);
        TextView tvBodegaSub = dialog.findViewById(R.id.tvMapBodegaSubtitle);
        final WebView wvMap = dialog.findViewById(R.id.wvMap);
        final TextView tvSupName = dialog.findViewById(R.id.tvMapSelectedSupplierName);
        final TextView tvSupDist = dialog.findViewById(R.id.tvMapSelectedSupplierDistance);
        final TextView tvSupDetails = dialog.findViewById(R.id.tvMapSelectedSupplierDetails);

        double dist = currentLocation.distanceTo(supplier.lat, supplier.lng);
        double flete = supplier.calculateDeliveryFee(currentLocation, 50.0);

        tvBodegaSub.setText("📍 Tu Bodega (" + currentLocation.name + ") ➔ " + supplier.name);
        tvSupName.setText("🏢 " + supplier.name);
        tvSupDist.setText(String.format(Locale.US, "📍 %.1f km", dist));
        tvSupDetails.setText(String.format(Locale.US, "%s (%s) • Flete: S/ %.2f • ⏱️ %s • Tel: %s",
                supplier.address, supplier.district, flete, supplier.deliveryTimeEstimate, supplier.phone));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        wvMap.getSettings().setJavaScriptEnabled(true);
        wvMap.getSettings().setDomStorageEnabled(true);
        wvMap.setWebChromeClient(new WebChromeClient());
        wvMap.setWebViewClient(new WebViewClient());

        String html = "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'/>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>"
                + "  html, body { height: 100%; margin: 0; padding: 0; background: #0A192F; font-family: -apple-system, sans-serif; }"
                + "  #map { height: 100%; width: 100%; }"
                + "  .leaflet-popup-content-wrapper { border-radius: 14px; border: 2px solid #D4AF37; box-shadow: 0 6px 16px rgba(0,0,0,0.3); padding: 4px; }"
                + "  .bodega-marker { background: #D4AF37; border: 3px solid #0A192F; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 0 14px #D4AF37; }"
                + "  .sup-marker { background: #0A192F; border: 2px solid #D4AF37; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 4px 8px rgba(0,0,0,0.4); animation: pulse 2s infinite; }"
                + "  @keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.15); } 100% { transform: scale(1); } }"
                + "</style>"
                + "</head><body>"
                + "<div id='map'></div>"
                + "<script>"
                + "  var bodegaLat = " + currentLocation.lat + ";"
                + "  var bodegaLng = " + currentLocation.lng + ";"
                + "  var supLat = " + supplier.lat + ";"
                + "  var supLng = " + supplier.lng + ";"
                + "  try {"
                + "    var map = L.map('map', {zoomControl: true});"
                + "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18, attribution: 'Chiclayo Mayorista' }).addTo(map);"
                + "    var bodegaIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\"bodega-marker\">🏪</div>', iconSize: [34, 34], iconAnchor: [17, 17] });"
                + "    var bMarker = L.marker([bodegaLat, bodegaLng], {icon: bodegaIcon}).addTo(map);"
                + "    bMarker.bindPopup('<b style=\"color:#0A192F; font-size:14px;\">🏪 TU BODEGA</b><br/><span style=\"color:#64748B;\">" + currentLocation.name + "</span>');"
                + "    var supIcon = L.divIcon({ className: 'custom-icon', html: '<div class=\"sup-marker\">🏢</div>', iconSize: [34, 34], iconAnchor: [17, 17] });"
                + "    var sMarker = L.marker([supLat, supLng], {icon: supIcon}).addTo(map);"
                + "    sMarker.bindPopup('<div style=\"font-size:13px;\"><b style=\"color:#0A192F; font-size:14px;\">🏢 " + supplier.name + "</b><br/>"
                + "      <span style=\"color:#64748B;\">📍 " + supplier.address + " (" + supplier.district + ")</span><br/>"
                + "      <b style=\"color:#0A192F;\">📏 " + String.format(Locale.US, "%.1f", dist) + " km de tu bodega</b><br/>"
                + "      <b style=\"color:#059669;\">🚚 Flete: S/ " + String.format(Locale.US, "%.2f", flete) + "</b></div>').openPopup();"
                + "    L.polyline([[bodegaLat, bodegaLng], [supLat, supLng]], {color: '#D4AF37', weight: 3, dashArray: '6, 8'}).addTo(map);"
                + "    var bounds = L.latLngBounds([[bodegaLat, bodegaLng], [supLat, supLng]]);"
                + "    map.fitBounds(bounds, {padding: [50, 50]});"
                + "  } catch(e) {"
                + "    document.getElementById('map').innerHTML = '<div style=\"color:white; padding:20px; text-align:center;\"><h3>Mapa de Proveedor</h3></div>';"
                + "  }"
                + "</script></body></html>";

        wvMap.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null);
        dialog.show();
    }

    // ==========================================
    // DIÁLOGO SELECTOR DE UBICACIÓN (GPS & MAPA)
    // ==========================================
    private void showLocationPickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_location_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.94),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }

        TextView btnClose = dialog.findViewById(R.id.btnLocationClose);
        TextView tvCurrentBodega = dialog.findViewById(R.id.tvCurrentBodegaLocation);
        TextView btnUseGps = dialog.findViewById(R.id.btnUseGps);
        TextView btnPickOnMap = dialog.findViewById(R.id.btnPickOnMap);

        if (tvCurrentBodega != null) {
            tvCurrentBodega.setText(currentLocation != null ? currentLocation.name : "Sin fijar");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnUseGps != null) {
            btnUseGps.setOnClickListener(v -> {
                dialog.dismiss();
                requestGpsLocation();
            });
        }

        if (btnPickOnMap != null) {
            btnPickOnMap.setOnClickListener(v -> {
                dialog.dismiss();
                showMapLocationPicker();
            });
        }

        dialog.show();
    }

    // ==========================================
    // UBICACIÓN PRECISA: GPS Y MARCADO EN MAPA
    // ==========================================
    public static class PickBridge {
        public interface Listener {
            void onPicked(double lat, double lng);
        }

        private final Listener listener;

        PickBridge(Listener listener) {
            this.listener = listener;
        }

        @JavascriptInterface
        public void onPicked(double lat, double lng) {
            listener.onPicked(lat, lng);
        }
    }

    private void requestGpsLocation() {
        if (hasLocationPermission()) {
            obtainCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (granted) {
            obtainCurrentLocation();
        } else {
            Toast.makeText(this, "Sin permiso de ubicación. Puedes marcar tu bodega en el mapa.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void obtainCurrentLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            Toast.makeText(this, "Este teléfono no ofrece ubicación. Márcala en el mapa.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Última posición conocida y reciente
        Location lastKnown = null;
        for (String provider : lm.getProviders(true)) {
            Location candidate = lm.getLastKnownLocation(provider);
            if (candidate != null && (lastKnown == null || candidate.getTime() > lastKnown.getTime())) {
                lastKnown = candidate;
            }
        }
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.getTime() < GPS_MAX_AGE_MS) {
            applyGpsLocation(lastKnown);
            return;
        }

        // 2) Pedir lectura nueva
        String provider = null;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        if (provider == null) {
            Toast.makeText(this, "Activa el GPS del teléfono o marca tu bodega en el mapa.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Buscando tu ubicación GPS...", Toast.LENGTH_SHORT).show();
        final Handler handler = new Handler(Looper.getMainLooper());
        final LocationListener[] listenerRef = new LocationListener[1];
        listenerRef[0] = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lm.removeUpdates(this);
                handler.removeCallbacksAndMessages(null);
                applyGpsLocation(location);
            }

            @Override
            public void onStatusChanged(String p, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String p) {}

            @Override
            public void onProviderDisabled(@NonNull String p) {}
        };
        lm.requestLocationUpdates(provider, 0L, 0f, listenerRef[0], Looper.getMainLooper());
        handler.postDelayed(() -> {
            lm.removeUpdates(listenerRef[0]);
            Toast.makeText(MainActivity.this, "No se pudo obtener lectura GPS. Márcala en el mapa.", Toast.LENGTH_SHORT).show();
        }, GPS_TIMEOUT_MS);
    }

    private void applyGpsLocation(Location loc) {
        String ref = String.format(Locale.US, "Lat %.4f, Lng %.4f (±%.0f m)",
                loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());
        applyCustomLocation(new DistrictLocation("gps", "Mi ubicación GPS", ref,
                loc.getLatitude(), loc.getLongitude()), "Ubicación GPS aplicada");
    }

    private void applyCustomLocation(DistrictLocation loc, String message) {
        customLocation = loc;
        currentLocation = loc;
        tvSelectedLocation.setText(loc.name);
        refreshProductList();
        updateCartBar();
        Toast.makeText(this, message + ". Fletes recalculados.", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showMapLocationPicker() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_location_map);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.96),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.9));
        }

        TextView btnClose = dialog.findViewById(R.id.btnPickClose);
        TextView btnGps = dialog.findViewById(R.id.btnPickGps);
        final TextView btnConfirm = dialog.findViewById(R.id.btnPickConfirm);
        final TextView tvCoords = dialog.findViewById(R.id.tvPickCoords);
        final WebView wvPick = dialog.findViewById(R.id.wvPickMap);

        final double[] picked = {Double.NaN, Double.NaN};

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnGps.setOnClickListener(v -> {
            dialog.dismiss();
            requestGpsLocation();
        });
        btnConfirm.setOnClickListener(v -> {
            if (Double.isNaN(picked[0])) {
                return;
            }
            dialog.dismiss();
            applyCustomLocation(new DistrictLocation("mapa", "Punto marcado en el mapa",
                    String.format(Locale.US, "Lat %.4f, Lng %.4f", picked[0], picked[1]),
                    picked[0], picked[1]), "Ubicación del mapa guardada");
        });

        wvPick.getSettings().setJavaScriptEnabled(true);
        wvPick.getSettings().setDomStorageEnabled(true);
        wvPick.setWebChromeClient(new WebChromeClient());
        wvPick.setWebViewClient(new WebViewClient());
        wvPick.addJavascriptInterface(new PickBridge((lat, lng) -> runOnUiThread(() -> {
            picked[0] = lat;
            picked[1] = lng;
            tvCoords.setText(String.format(Locale.US, "Punto marcado: %.4f, %.4f", lat, lng));
            btnConfirm.setEnabled(true);
            btnConfirm.setAlpha(1.0f);
        })), "AndroidPicker");

        String startLat = Double.toString(currentLocation.lat);
        String startLng = Double.toString(currentLocation.lng);
        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0;padding:0;background:#0A192F}"
                + ".bodega-pin{background:#D4AF37;border:3px solid #0A192F;border-radius:50%;width:34px;height:34px;display:flex;align-items:center;justify-content:center;font-size:16px;box-shadow:0 0 14px #D4AF37;}</style></head><body>"
                + "<div id='map'></div><script>"
                + "var map=L.map('map').setView([" + startLat + "," + startLng + "],15);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'OpenStreetMap'}).addTo(map);"
                + "var pinIcon=L.divIcon({className:'custom-pin',html:'<div class=\"bodega-pin\">🏪</div>',iconSize:[34,34],iconAnchor:[17,17]});"
                + "var marker=L.marker([" + startLat + "," + startLng + "],{draggable:true,icon:pinIcon}).addTo(map);"
                + "marker.bindPopup('<b style=\"color:#0A192F\">🏪 Mueve el pin a tu bodega</b>').openPopup();"
                + "function updatePick(lat,lng){if(window.AndroidPicker){window.AndroidPicker.onPicked(lat,lng);}}"
                + "marker.on('dragend',function(e){var p=marker.getLatLng();updatePick(p.lat,p.lng);});"
                + "map.on('click',function(e){marker.setLatLng(e.latlng);updatePick(e.latlng.lat,e.latlng.lng);});"
                + "</script></body></html>";

        wvPick.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null);
        dialog.show();
    }

    // ==========================================
    // DIÁLOGO VISTA PROVEEDOR
    // ==========================================
    private void showWholesalerModeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wholesaler_mode);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }

        TextView btnClose = dialog.findViewById(R.id.btnWholesalerClose);
        TextView btnContinue = dialog.findViewById(R.id.btnContinueAsBuyer);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnContinue.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ==========================================
    // DIÁLOGO COMPARADOR DE PROVEEDORES
    // ==========================================
    @Override
    public void onCompareSuppliers(Product product) {
        showSupplierComparisonDialog(product);
    }

    @Override
    public void onQuickAddToCart(Product product, SupplierOffer offer) {
        addToCart(product, offer, 1);
        Toast.makeText(this, "✅ 1x " + product.name + " añadido de " + offer.supplier.name, Toast.LENGTH_SHORT).show();
    }

    private void showSupplierComparisonDialog(final Product product) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_compare_suppliers);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.96),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.92)
            );
        }

        ImageView ivProduct = dialog.findViewById(R.id.ivCompareProductImage);
        TextView tvName = dialog.findViewById(R.id.tvCompareProductName);
        TextView tvMeta = dialog.findViewById(R.id.tvCompareProductMeta);
        TextView tvProductRating = dialog.findViewById(R.id.tvCompareProductRating);
        TextView btnClose = dialog.findViewById(R.id.btnCompareClose);
        TextView tvDestination = dialog.findViewById(R.id.tvCompareDestination);
        TextView btnMinus = dialog.findViewById(R.id.btnQtyMinus);
        final TextView tvQuantity = dialog.findViewById(R.id.tvCompareQuantity);
        TextView btnPlus = dialog.findViewById(R.id.btnQtyPlus);
        final LinearLayout llContainer = dialog.findViewById(R.id.llSuppliersListContainer);

        // Filter chips inside comparison dialog
        final TextView chipModalSortConvenient = dialog.findViewById(R.id.chipModalSortConvenient);
        final TextView chipModalSortPrice = dialog.findViewById(R.id.chipModalSortPrice);
        final TextView chipModalSortDistance = dialog.findViewById(R.id.chipModalSortDistance);

        if (product.imageResId != 0) {
            ivProduct.setImageResource(product.imageResId);
        } else {
            ivProduct.setImageResource(R.drawable.img_sporade);
        }

        tvName.setText(product.name);
        tvMeta.setText(product.presentation + " • " + product.brand);
        if (tvProductRating != null) {
            tvProductRating.setText(String.format(Locale.US, "★ %.1f", product.rating));
        }
        tvDestination.setText("Cotizando fletes a: " + currentLocation.name);

        final int[] currentQty = {1};
        final String[] compareSortMode = {"convenient"}; // "convenient", "price", "distance"

        final Runnable[] renderHolder = new Runnable[1];

        final Runnable updateSortChips = () -> {
            TextView[] chips = {chipModalSortConvenient, chipModalSortPrice, chipModalSortDistance};
            for (TextView c : chips) {
                if (c != null) {
                    c.setBackgroundResource(R.drawable.bg_chip_unselected);
                    c.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                }
            }
            TextView active = chipModalSortConvenient;
            if ("price".equals(compareSortMode[0])) {
                active = chipModalSortPrice;
            } else if ("distance".equals(compareSortMode[0])) {
                active = chipModalSortDistance;
            }
            if (active != null) {
                active.setBackgroundResource(R.drawable.bg_chip_selected);
                active.setTextColor(Color.WHITE);
            }
        };

        if (chipModalSortConvenient != null) {
            chipModalSortConvenient.setOnClickListener(v -> {
                compareSortMode[0] = "convenient";
                updateSortChips.run();
                if (renderHolder[0] != null) renderHolder[0].run();
            });
        }
        if (chipModalSortPrice != null) {
            chipModalSortPrice.setOnClickListener(v -> {
                compareSortMode[0] = "price";
                updateSortChips.run();
                if (renderHolder[0] != null) renderHolder[0].run();
            });
        }
        if (chipModalSortDistance != null) {
            chipModalSortDistance.setOnClickListener(v -> {
                compareSortMode[0] = "distance";
                updateSortChips.run();
                if (renderHolder[0] != null) renderHolder[0].run();
            });
        }

        Runnable renderOffers = new Runnable() {
            @Override
            public void run() {
                llContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

                SupplierOffer bestTotalOffer = product.getMostConvenientOffer(currentLocation, currentQty[0]);
                SupplierOffer lowestPriceOffer = product.getLowestPriceOffer();
                SupplierOffer closestOffer = product.getClosestOffer(currentLocation);

                List<SupplierOffer> sortedOffers = new ArrayList<>(product.offers);
                if ("price".equals(compareSortMode[0])) {
                    Collections.sort(sortedOffers, (o1, o2) -> Double.compare(o1.price, o2.price));
                } else if ("distance".equals(compareSortMode[0])) {
                    Collections.sort(sortedOffers, (o1, o2) -> {
                        double d1 = currentLocation.distanceTo(o1.supplier.lat, o1.supplier.lng);
                        double d2 = currentLocation.distanceTo(o2.supplier.lat, o2.supplier.lng);
                        return Double.compare(d1, d2);
                    });
                } else {
                    // Conveniente: Total costo para la cantidad seleccionada
                    Collections.sort(sortedOffers, (o1, o2) -> {
                        double t1 = o1.calculateTotalCost(currentLocation, currentQty[0]);
                        double t2 = o2.calculateTotalCost(currentLocation, currentQty[0]);
                        return Double.compare(t1, t2);
                    });
                }

                for (final SupplierOffer offer : sortedOffers) {
                    View itemView = inflater.inflate(R.layout.item_supplier_comparison, llContainer, false);

                    TextView tvSupName = itemView.findViewById(R.id.tvSupplierName);
                    TextView tvSupRating = itemView.findViewById(R.id.tvSupplierRating);
                    TextView tvSupDist = itemView.findViewById(R.id.tvSupplierDistrictAndDistance);
                    TextView tvSupBadge = itemView.findViewById(R.id.tvSupplierBadge);
                    TextView tvNote = itemView.findViewById(R.id.tvOfferNote);
                    TextView tvShipping = itemView.findViewById(R.id.tvFreeShippingCondition);
                    TextView tvUnit = itemView.findViewById(R.id.tvUnitPrice);
                    TextView tvFlete = itemView.findViewById(R.id.tvFleteCost);
                    TextView tvTotal = itemView.findViewById(R.id.tvFinalTotal);
                    TextView btnSelect = itemView.findViewById(R.id.btnSelectSupplier);
                    TextView btnMap = itemView.findViewById(R.id.btnSupplierMap);
                    View rootCard = itemView.findViewById(R.id.cardSupplierComparison);

                    double dist = currentLocation.distanceTo(offer.supplier.lat, offer.supplier.lng);
                    double subtotal = offer.price * currentQty[0];
                    double flete = offer.supplier.calculateDeliveryFee(currentLocation, subtotal);
                    double total = subtotal + flete;

                    tvSupName.setText(offer.supplier.name);
                    if (tvSupRating != null) {
                        tvSupRating.setText(String.format(Locale.US, "★ %.1f", offer.supplier.rating));
                    }
                    tvSupDist.setText(String.format(Locale.US, "📍 %.1f km • %s • ⏱️ %s", dist, offer.supplier.district, offer.supplier.deliveryTimeEstimate));
                    tvNote.setText(offer.note + " • Stock: " + offer.stock + " u.");

                    if (subtotal >= offer.supplier.freeShippingThreshold) {
                        tvShipping.setText("🎉 ¡Envío gratis aplicado!");
                        tvShipping.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green_save));
                        tvFlete.setText("Envío GRATIS");
                        tvFlete.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green_save));
                    } else {
                        double missing = offer.supplier.freeShippingThreshold - subtotal;
                        tvShipping.setText(String.format(Locale.US, "Envío gratis desde S/ %.0f (Faltan S/ %.2f)",
                                offer.supplier.freeShippingThreshold, missing));
                        tvShipping.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.navy_card));
                        tvFlete.setText(String.format(Locale.US, "S/ %.2f flete", flete));
                        tvFlete.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_secondary));
                    }

                    tvUnit.setText(String.format(Locale.US, "S/ %.2f (%d u.)", subtotal, currentQty[0]));
                    tvTotal.setText(String.format(Locale.US, "Total: S/ %.2f", total));

                    if (offer == bestTotalOffer) {
                        tvSupBadge.setVisibility(View.VISIBLE);
                        tvSupBadge.setText("🌟 MEJOR TOTAL");
                        tvSupBadge.setBackgroundResource(R.drawable.bg_gold_badge);
                        rootCard.setBackgroundResource(R.drawable.bg_card_highlight);
                    } else if (offer == lowestPriceOffer) {
                        tvSupBadge.setVisibility(View.VISIBLE);
                        tvSupBadge.setText("💰 MÁS BARATO");
                        tvSupBadge.setBackgroundResource(R.drawable.bg_pill_blue);
                    } else if (offer == closestOffer) {
                        tvSupBadge.setVisibility(View.VISIBLE);
                        tvSupBadge.setText("📍 MÁS CERCANO");
                        tvSupBadge.setBackgroundResource(R.drawable.bg_pill_green);
                    } else {
                        tvSupBadge.setVisibility(View.GONE);
                    }

                    btnSelect.setText(String.format(Locale.US, "+ Añadir (S/ %.2f)", total));
                    btnSelect.setOnClickListener(v -> {
                        addToCart(product, offer, currentQty[0]);
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,
                                "✅ Agregado " + currentQty[0] + "x " + product.name + " (" + offer.supplier.name + ")",
                                Toast.LENGTH_SHORT).show();
                    });

                    if (btnMap != null) {
                        btnMap.setOnClickListener(v -> showSingleSupplierMapDialog(offer.supplier));
                    }

                    llContainer.addView(itemView);
                }
            }
        };

        renderHolder[0] = renderOffers;
        renderOffers.run();

        btnPlus.setOnClickListener(v -> {
            if (currentQty[0] < 50) {
                currentQty[0]++;
                tvQuantity.setText(String.valueOf(currentQty[0]));
                renderOffers.run();
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (currentQty[0] > 1) {
                currentQty[0]--;
                tvQuantity.setText(String.valueOf(currentQty[0]));
                renderOffers.run();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
            cardCartBar.setVisibility(View.GONE);
            return;
        }

        int totalUnits = 0;
        double productsSubtotal = 0.0;

        Map<String, Double> supplierSubtotals = new HashMap<>();
        Map<String, Supplier> supplierMap = new HashMap<>();

        for (CartItem item : cartItems) {
            totalUnits += item.quantity;
            double lineTotal = item.getSubtotal();
            productsSubtotal += lineTotal;

            String supId = item.offer.supplier.id;
            supplierMap.put(supId, item.offer.supplier);
            double currentSub = supplierSubtotals.containsKey(supId) ? supplierSubtotals.get(supId) : 0.0;
            supplierSubtotals.put(supId, currentSub + lineTotal);
        }

        double totalFlete = 0.0;
        for (Map.Entry<String, Double> entry : supplierSubtotals.entrySet()) {
            Supplier sup = supplierMap.get(entry.getKey());
            if (sup != null) {
                totalFlete += sup.calculateDeliveryFee(currentLocation, entry.getValue());
            }
        }

        double grandTotal = productsSubtotal + totalFlete;

        cardCartBar.setVisibility(View.VISIBLE);
        tvCartSummaryCountAndTotal.setText(String.format(Locale.US, "🛒 %d ítem%s • S/ %.2f",
                totalUnits, totalUnits > 1 ? "s" : "", grandTotal));
        tvCartSummarySubtitle.setText(String.format(Locale.US, "Incluye flete a tu bodega (S/ %.2f)", totalFlete));
    }

    private void showCartDialog() {
        if (cartItems.isEmpty()) return;

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_cart);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.96),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.90)
            );
        }

        TextView btnClose = dialog.findViewById(R.id.btnCartClose);
        TextView tvDest = dialog.findViewById(R.id.tvCartDestination);
        final LinearLayout llContainer = dialog.findViewById(R.id.llCartItemsContainer);
        final TextView tvSubtotal = dialog.findViewById(R.id.tvCartSubtotal);
        final TextView tvFlete = dialog.findViewById(R.id.tvCartTotalFlete);
        final TextView tvGrandTotal = dialog.findViewById(R.id.tvCartGrandTotal);
        TextView btnSendWhatsApp = dialog.findViewById(R.id.btnSendOrderWhatsApp);
        TextView btnClear = dialog.findViewById(R.id.btnClearCart);

        tvDest.setText("Dirección de Bodega: " + currentLocation.name);

        final Runnable refreshCartContent = new Runnable() {
            @Override
            public void run() {
                llContainer.removeAllViews();
                if (cartItems.isEmpty()) {
                    dialog.dismiss();
                    updateCartBar();
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                double subtotalAll = 0.0;
                Map<String, Double> supplierSubtotals = new HashMap<>();
                Map<String, Supplier> supplierMap = new HashMap<>();

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
                    TextView tvQty = itemView.findViewById(R.id.tvCartItemQuantity);
                    TextView tvItemSubtotal = itemView.findViewById(R.id.tvCartItemSubtotal);
                    TextView tvUnitPrice = itemView.findViewById(R.id.tvCartItemUnitPrice);
                    TextView btnRemove = itemView.findViewById(R.id.btnRemoveCartItem);
                    TextView btnMinus = itemView.findViewById(R.id.btnCartItemMinus);
                    TextView btnPlus = itemView.findViewById(R.id.btnCartItemPlus);

                    if (item.product.imageResId != 0) {
                        ivItemImage.setImageResource(item.product.imageResId);
                    } else {
                        ivItemImage.setImageResource(R.drawable.img_sporade);
                    }

                    tvTitle.setText(item.product.name);
                    tvSupplier.setText("🏢 " + item.offer.supplier.name);
                    tvQty.setText(String.valueOf(item.quantity));
                    tvUnitPrice.setText(String.format(Locale.US, "S/ %.2f c/u", item.offer.price));
                    tvItemSubtotal.setText(String.format(Locale.US, "S/ %.2f", lineSubtotal));

                    btnPlus.setOnClickListener(v -> {
                        item.quantity++;
                        run();
                        updateCartBar();
                    });

                    btnMinus.setOnClickListener(v -> {
                        if (item.quantity > 1) {
                            item.quantity--;
                        } else {
                            cartItems.remove(item);
                        }
                        run();
                        updateCartBar();
                    });

                    btnRemove.setOnClickListener(v -> {
                        cartItems.remove(item);
                        run();
                        updateCartBar();
                    });

                    llContainer.addView(itemView);
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
            }
        };

        refreshCartContent.run();

        // Enviar pedido vía WhatsApp
        btnSendWhatsApp.setOnClickListener(v -> {
            sendOrderViaWhatsApp();
            dialog.dismiss();
        });

        // Vaciar Carrito
        btnClear.setOnClickListener(v -> {
            cartItems.clear();
            dialog.dismiss();
            updateCartBar();
            Toast.makeText(MainActivity.this, "🗑️ Carrito vaciado", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void sendOrderViaWhatsApp() {
        if (cartItems.isEmpty()) return;

        // Armar mensaje formal de abastecimiento B2B
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
            Toast.makeText(this, "Abriendo selector de mensajería...", Toast.LENGTH_SHORT).show();
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
            startActivity(Intent.createChooser(sendIntent, "Enviar Pedido"));
        }
    }
}
