# ProveeChiclayo (B2B Mayorista)

Aplicación móvil Android para abastecimiento mayorista de bodegas y tiendas de abarrotes en **Chiclayo y Lambayeque**, inspirada en la experiencia de usuario de PedidosYa y Rappi pero adaptada al comercio B2B entre distribuidores y bodegueros.

---

## 🌟 Características Principales

1. **Diseño Corporativo & Fotos Reales de Catálogo:**
   - Paleta corporativa Azul Marino Profundo (`#0A192F`, `#102A56`) y Dorado (`#D4AF37`).
   - Packshots reales en alta definición para productos peruanos de alta rotación: *Sporade 500ml, Inca Kola 3L, Arroz Superior Costeño 50kg, Aceite Primor 1L, Leche Gloria Azul, Galletas Casino, Gatorade, Detergente Ace, Atún Campomar, Azúcar Cartavio, Chocolate Triángulo*.

2. **Motor de Flete y Distancia en Chiclayo & Lambayeque:**
   - Cálculo en tiempo real de distancias (fórmula de Haversine) entre la bodega y los distribuidores.
   - Cálculo automático de flete de transporte (moto-flete y furgón) por kilómetro, con tarifas mínimas y montos para **Envío Gratis**.
   - Cobertura de núcleos comerciales:
     - 🏢 *Distribuidora Moshoqueque Central* (Av. Kennedy, J.L. Ortiz)
     - 🏢 *Comercializadora San José & Hnos* (Av. José Balta, Chiclayo Centro)
     - 🏢 *Depósito Mayorista La Victoria* (Av. Chinchaysuyo)
     - 🏢 *Mega Abarrotes Lambayeque Norte* (Calle Huamachuco)
     - 🏢 *Distribuidora Bebidas del Norte SAC* (Km 7.8 Panamericana Norte)

3. **Comparación Inteligente y Filtros:**
   - **🌟 Más Conveniente (Costo Total: Producto + Flete):** Detecta si un producto con menor precio en Lambayeque realmente conviene tras sumarle el transporte a Moshoqueque, o si conviene comprar más cerca.
   - **💰 Menor Precio:** Ordena por precio unitario del producto.
   - **📍 Más Cercano:** Ordena por proximidad en kilómetros.
   - **🚚 Menor Flete:** Ordena por costo de delivery.

4. **🗺️ Mapa Interactivo de Proveedores:**
   - Mapa con marcadores personalizados para tu Bodega y los distribuidores de Chiclayo y Lambayeque.
   - Líneas de distancia y costo de flete directo a tu local.
   - Tarjetas informativas dinámicas al pulsar cualquier mayorista.

5. **Comparador Lado a Lado & Simulador de Cantidad:**
   - Modal comparativo con control `[-] Cantidad [+]` que recalcula en vivo subtotales y muestra si el volumen califica para flete gratuito.

6. **Carrito B2B y Pedido por WhatsApp:**
   - Agrupación automática por distribuidor mayorista.
   - Botón directo para generar y enviar el ticket de pedido con formato formal a través de la API de WhatsApp.

---

## 🛠️ Tecnologías Utilizadas
- **Lenguaje:** Java 11
- **Plataforma:** Android Nativo (SDK 24 - 34+)
- **UI:** Material Design 3, CardView, RecyclerView, WebView (Leaflet OSM)
- **Logística:** Haversine Distance Algorithm
