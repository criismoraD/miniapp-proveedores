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

3b. **📍 Ubicación de tu bodega (GPS o mapa):**
   - **Usar mi GPS:** toma tu posición del teléfono (pide permiso de ubicación; usa la última lectura reciente o pide una nueva, sin depender de Google Play Services).
   - **En el mapa:** toca o arrastra el marcador de Leaflet para fijar tu bodega en cualquier punto.
   - Con cualquiera de las dos, los fletes, distancias y el orden de los proveedores se recalculan para ese punto.
   - Las cinco zonas de referencia siguen disponibles en la misma hoja.

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

## 🗞️ Diseño: "Papel y Tinta"

La interfaz se inspira en una hoja impresa: fondo de papel crema con fibra sutil, tinta azul-negra para el texto, filetes finos y dorado de imprenta para los acentos.

- **Paleta semántica** (`res/values/colors.xml`): `paper_*` (papel), `ink*` (tinta), `gilt*` (dorado), `stamp` (sello rojo), `sage` (verde musgo para ahorro y envío gratis). Existe una variante nocturna en `res/values-night/`.
- **Tipografía:** EB Garamond (titulares, precios y nombres) con subconjunto latino y pesos estáticos en `res/font/`. Licencia SIL Open Font License en `docs/licenses/EBGaramond_OFL.txt`. El texto de datos usa la sans del sistema.
- **Componentes Material 3:** `MaterialCardView`, `MaterialButton`, `Chip`/`ChipGroup` de selección única, `TextInputLayout` con botón de borrado, `BottomSheetDialog` para las hojas, `Snackbar` para avisos.
- **Animaciones** (`ui/Motion.java` y `res/anim/`):
  - Portada: el nombre se revela como una máquina de escribir y la cabecera se desliza al abrir.
  - Listado: las hojas de producto entran en cascada y vuelven a entrar al cambiar categoría u orden.
  - Pulsación: cada tarjeta y bloque se hunde ligeramente y rebota al soltar.
  - Sello: al añadir un producto aparece un sello "AÑADIDO" que cae, se asienta girado y se desvanece.
  - Barra de pedido: sube desde abajo, rebota al cambiar el total y baja al vaciarse.
  - Hojas y diálogos: entrada escalonada de ofertas y líneas de pedido; el mapa aparece con fundido y escala.

## 🛠️ Tecnologías Utilizadas
- **Lenguaje:** Java 11
- **Plataforma:** Android Nativo (SDK 24 - 34+)
- **UI:** Material Design 3 (Material Components 1.10), RecyclerView, WebView (Leaflet OSM), animaciones de View y ViewPropertyAnimator
- **Logística:** Haversine Distance Algorithm
