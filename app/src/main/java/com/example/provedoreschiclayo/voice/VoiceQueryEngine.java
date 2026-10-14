package com.example.provedoreschiclayo.voice;

import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motor de consultas del asistente de voz de ProveeChiclayo.
 * Responde con los datos locales: productos, ofertas, proveedores, zonas y envíos.
 */
public final class VoiceQueryEngine {

    private VoiceQueryEngine() {}

    /** Datos que el motor necesita para responder. */
    public static class Ctx {
        public List<Product> products;
        public List<Supplier> suppliers;
        public List<DistrictLocation> locations;
        public DistrictLocation currentLocation;
    }

    /** Resultado de una consulta. */
    public static class Answer {
        public String title = "";
        public String spoken = "";
        public final List<String> rows = new ArrayList<>();
        public String filterQuery = null;
    }

    // ---------------- Normalización ----------------

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(new Locale("es", "PE"));
        t = t.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o')
             .replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n');
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ') sb.append(c);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    /** Palabras que nunca forman parte de un producto o proveedor: se pueden quitar. */
    private static final String[] INTENT_WORDS = {
        "precio", "precios", "cuanto", "cuanta", "cuantos", "cuesta", "cuestan", "sale", "salen",
        "vale", "valen", "cual", "cuales", "que", "quien", "quienes", "donde", "cuando", "como",
        "para", "por", "con", "sobre", "el", "la", "los", "las", "un", "una", "unos", "unas",
        "de", "del", "al", "y", "e", "o", "u", "ni", "es", "son", "esta", "estan", "hay",
        "tienen", "tiene", "tengo", "dame", "dime", "muestra", "muestrame", "ver", "busca",
        "buscar", "quiero", "necesito", "saber", "hola", "buenas", "gracias", "favor", "app",
        "aplicacion", "ayuda", "puedes", "hacer", "pedir", "pedidos", "mas", "menos", "barato",
        "barata", "baratos", "baratas", "economico", "economica", "caro", "cara", "mejor",
        "mejores", "peor", "producto", "productos", "proveedor", "proveedores", "distribuidor",
        "distribuidores", "telefono", "celular", "contacto", "numero", "llamar", "whatsapp",
        "cerca", "cercano", "cercanos", "lejos", "zona", "zonas", "ubican", "ubicacion",
        "envio", "envios", "flete", "fletes", "delivery", "demora", "tarda", "entrega",
        "calificacion", "rating", "Estrella", "estrellas"
    };

    private static List<String> contentTokens(String q) {
        List<String> out = new ArrayList<>();
        for (String t : q.split(" ")) {
            if (t.length() < 3) continue;
            if (has(q, "x") && false) continue;
            boolean intent = false;
            for (String w : INTENT_WORDS) {
                if (t.equals(w)) { intent = true; break; }
            }
            if (!intent) out.add(t);
        }
        return out;
    }

    // ---------------- Punto de entrada ----------------

    public static Answer answer(String rawQuery, Ctx ctx) {
        String q = norm(rawQuery);
        Answer a = new Answer();
        if (q.isEmpty()) return help(a);

        if (has(q, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches") && q.length() < 24) {
            return greeting(a);
        }
        if (has(q, "ayuda", "que puedes hacer", "como te pregunto", "para que sirves", "comandos")) {
            return help(a);
        }
        if (has(q, "telefono", "celular", "contacto", "numero de", "llamar a", "whatsapp de")) {
            Answer r = supplierContact(q, ctx);
            if (r != null) return r;
        }
        if (has(q, "calificacion", "mejor valorado", "mas confiable", "rating", "estrellas")) {
            return bestRated(ctx);
        }
        if (has(q, "zona", "zonas", "ubican", "ubicacion", "cobertura", "distrito", "distritos")
                || has(q, "donde entregan", "a donde llegan")) {
            return zones(ctx);
        }
        if (has(q, "flete", "envio", "delivery", "demora", "tarda", "cuanto tardo", "cuanto demora")) {
            return delivery(ctx);
        }
        if (has(q, "proveedor", "proveedores", "distribuidor", "mayorista")
                || has(q, "quien vende", "quienes vende", "quien tiene", "quienes tienen")) {
            return suppliers(ctx);
        }
        if (has(q, "mas barato", "mas economico") && contentTokens(q).isEmpty()) {
            return cheapestProducts(ctx);
        }
        return product(q, ctx);
    }

    // ---------------- Respuestas ----------------

    private static Answer greeting(Answer a) {
        a.title = "👋 ¡Hola! Tu asistente mayorista";
        a.spoken = "¡Hola! Soy tu asistente de ProveeChiclayo. Puedes preguntarme precios de "
                + "productos, quién vende más barato, el teléfono de un proveedor, "
                + "las zonas de reparto y cuánto cuesta el envío.";
        a.rows.add("Precios de productos (Sporade, Arroz, Aceite…)");
        a.rows.add("Proveedores por zona o los más cercanos");
        a.rows.add("Teléfonos y datos de contacto");
        a.rows.add("Fletes, tiempos de entrega y cobertura");
        return a;
    }

    private static Answer help(Answer a) {
        a.title = "❓ ¿Qué puedo responder?";
        a.spoken = "Puedo responder sobre productos y precios, ofertas de cada proveedor, "
                + "teléfonos de distribuidores, zonas de Chiclayo y Lambayeque, y envíos. "
                + "Solo toca el micrófono y pregunta.";
        a.rows.add("«¿Cuál es el arroz más barato?»");
        a.rows.add("«¿Quién vende Sporade?»");
        a.rows.add("«¿Teléfono de Mega Abarrotes Lambayeque?»");
        a.rows.add("«¿Qué proveedores hay en La Victoria?»");
        a.rows.add("«¿Cuánto demora el envío?»");
        a.rows.add("«¿Qué zonas cubren?»");
        return a;
    }

    private static Answer product(String q, Ctx ctx) {
        List<String> tokens = contentTokens(q);
        Product best = null;
        int bestScore = 0;
        for (Product p : ctx.products) {
            int s = score(p, tokens, q);
            if (s > bestScore) { bestScore = s; best = p; }
        }
        if (best == null) {
            Answer a = new Answer();
            a.title = "🔍 No encontré ese producto";
            a.spoken = "No encontré información sobre eso. Tengo "
                    + ctx.products.size() + " productos de la red mayorista. "
                    + "Prueba con una marca como Sporade, Gatorade, Costeño, Primor, Gloria o Ace.";
            a.rows.add("Puedes preguntar por:");
            for (Product p : sample(ctx.products, 6)) a.rows.add("• " + p.name);
            return a;
        }

        SupplierOffer cheap = best.getLowestPriceOffer();
        SupplierOffer nice = best.getMostConvenientOffer(ctx.currentLocation, 1);
        Answer a = new Answer();
        a.filterQuery = firstToken(tokens, best.name);
        a.title = "💰 " + best.name;
        a.rows.add(best.emoji + " " + best.brand + " · " + best.presentation);
        a.rows.add("Rating " + String.format(Locale.US, "%.1f", best.rating)
                + " (" + best.reviewCount + " bodegueros)");
        a.rows.add("");
        a.rows.add("🏆 Más barato: S/ " + money(cheap.price) + " en " + cheap.supplier.name);
        a.rows.add("🚚 Más conveniente (con flete): S/ "
                + money(nice.calculateTotalCost(ctx.currentLocation, 1)) + " en " + nice.supplier.name);
        a.rows.add("");
        List<SupplierOffer> top = new ArrayList<>(best.offers);
        top.sort((o1, o2) -> Double.compare(o1.price, o2.price));
        int shown = 0;
        for (SupplierOffer o : top) {
            if (shown++ >= 3) break;
            a.rows.add("S/ " + money(o.price) + " · " + o.supplier.name
                    + " (" + o.supplier.district + ") · " + dist(ctx.currentLocation, o.supplier) + " km · stock " + o.stock);
        }
        a.spoken = best.name + ". La oferta más barata está en " + shortName(cheap.supplier.name)
                + " a " + speakMoney(cheap.price) + ". Con el flete desde "
                + zoneShort(ctx.currentLocation) + ", te conviene "
                + shortName(nice.supplier.name) + " con un total de "
                + speakMoney(nice.calculateTotalCost(ctx.currentLocation, 1)) + ".";
        return a;
    }

    private static int score(Product p, List<String> tokens, String q) {
        String name = norm(p.name), brand = norm(p.brand), cat = norm(p.category);
        int s = 0;
        if (!q.isEmpty()) {
            if (name.contains(q)) s += 120;
            else if (brand.contains(q)) s += 80;
        }
        for (String t : tokens) {
            if (name.contains(t)) s += 40;
            if (brand.contains(t)) s += 30;
            if (cat.contains(t)) s += 25;
            if (norm(p.presentation).contains(t)) s += 10;
        }
        return s;
    }

    private static List<Product> sample(List<Product> list, int n) {
        List<Product> out = new ArrayList<>();
        for (int i = 0; i < list.size() && out.size() < n; i += Math.max(1, list.size() / n)) out.add(list.get(i));
        return out;
    }

    private static String firstToken(List<String> tokens, String fallback) {
        if (!tokens.isEmpty()) return tokens.get(0);
        return fallback;
    }

    private static Answer supplierContact(String q, Ctx ctx) {
        List<String> tokens = contentTokens(q);
        Supplier best = null;
        int bestScore = 0;
        for (Supplier s : ctx.suppliers) {
            int sc = 0;
            for (String t : tokens) {
                if (norm(s.name).contains(t)) sc += 40;
                if (norm(s.district).contains(t)) sc += 25;
                if (norm(s.address).contains(t)) sc += 15;
                if (norm(s.badge).contains(t)) sc += 10;
            }
            if (sc > bestScore) { bestScore = sc; best = s; }
        }
        if (best == null) return null;
        Answer a = new Answer();
        a.title = "📞 " + best.name;
        a.rows.add("Teléfono: " + spacedPhone(best.phone));
        a.rows.add("Dirección: " + best.address);
        a.rows.add("Distrito: " + best.district + "  ·  " + best.badge);
        a.rows.add("Calificación: " + String.format(Locale.US, "%.1f", best.rating) + " ⭐");
        a.rows.add("Entrega estimada: " + best.deliveryTimeEstimate);
        a.rows.add("Distancia a tu bodega: " + dist(ctx.currentLocation, best) + " km");
        a.rows.add("Envío gratis desde S/ " + money(best.freeShippingThreshold));
        a.spoken = "El teléfono de " + shortName(best.name) + " es "
                + spacedPhone(best.phone) + ". Está en " + best.district
                + ", a " + dist(ctx.currentLocation, best) + " kilómetros de tu bodega. "
                + "Calificación " + String.format(Locale.US, "%.1f", best.rating) + ".";
        return a;
    }

    private static Answer suppliers(Ctx ctx) {
        List<Supplier> sorted = new ArrayList<>(ctx.suppliers);
        sorted.sort((s1, s2) -> Double.compare(
                ctx.currentLocation.distanceTo(s1.lat, s1.lng),
                ctx.currentLocation.distanceTo(s2.lat, s2.lng)));
        Answer a = new Answer();
        a.title = "🤝 Proveedores cerca de ti";
        a.rows.add("Tu bodega: " + ctx.currentLocation.name);
        a.rows.add("");
        for (Supplier s : sorted) {
            a.rows.add("• " + s.name);
            a.rows.add("   " + s.district + " · " + dist(ctx.currentLocation, s) + " km · "
                    + String.format(Locale.US, "%.1f", s.rating) + "⭐ · " + s.deliveryTimeEstimate
                    + " · " + s.badge);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Los más cercanos a tu bodega en ").append(zoneShort(ctx.currentLocation)).append(" son: ");
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Supplier s = sorted.get(i);
            sb.append(shortName(s.name)).append(" a ").append(dist(ctx.currentLocation, s))
              .append(" kilómetros, ");
        }
        a.spoken = sb.toString();
        return a;
    }

    private static Answer bestRated(Ctx ctx) {
        List<Supplier> sorted = new ArrayList<>(ctx.suppliers);
        sorted.sort((s1, s2) -> Double.compare(s2.rating, s1.rating));
        Answer a = new Answer();
        a.title = "⭐ Mejor calificados";
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Supplier s = sorted.get(i);
            a.rows.add("• " + s.name + " — " + String.format(Locale.US, "%.1f", s.rating)
                    + "⭐ (" + s.district + ") " + s.badge);
        }
        a.spoken = "Los proveedores mejor calificados son: "
                + shortName(sorted.get(0).name) + " con "
                + String.format(Locale.US, "%.1f", sorted.get(0).rating)
                + " estrellas, seguido de " + shortName(sorted.get(1).name) + ".";
        return a;
    }

    private static Answer zones(Ctx ctx) {
        Answer a = new Answer();
        a.title = "📍 Zonas de reparto";
        a.spoken = "Cubrimos Chiclayo y Lambayeque. Las zonas son: ";
        StringBuilder sb = new StringBuilder();
        for (DistrictLocation l : ctx.locations) {
            a.rows.add("• " + l.name + " — " + l.reference);
            sb.append(l.name).append(", ");
        }
        a.spoken += sb.toString();
        if (ctx.currentLocation != null) {
            a.rows.add("");
            a.rows.add("Tu bodega está en: " + ctx.currentLocation.name);
        }
        return a;
    }

    private static Answer delivery(Ctx ctx) {
        Supplier nearest = ctx.suppliers.get(0);
        double d = ctx.currentLocation.distanceTo(nearest.lat, nearest.lng);
        for (Supplier s : ctx.suppliers) {
            double dd = ctx.currentLocation.distanceTo(s.lat, s.lng);
            if (dd < d) { d = dd; nearest = s; }
        }
        double fee = nearest.calculateDeliveryFee(ctx.currentLocation, 100.0);
        Answer a = new Answer();
        a.title = "🚚 Envíos y fletes";
        a.rows.add("El flete se calcula por distancia desde tu bodega.");
        a.rows.add("Mínimo S/ 3.00 · tope S/ 18.00 dentro de la provincia.");
        a.rows.add("Cada mayorista tiene monto para envío gratis.");
        a.rows.add("");
        a.rows.add("Proveedor más cercano: " + nearest.name);
        a.rows.add("Distancia: " + d + " km · entrega " + nearest.deliveryTimeEstimate);
        a.rows.add("Ejemplo: compra de S/ 100 → flete S/ " + money(fee));
        a.rows.add("Envío gratis desde S/ " + money(nearest.freeShippingThreshold));
        a.spoken = "El flete depende de la distancia a tu bodega, con un mínimo de 3 soles "
                + "y un tope de 18 soles. El mayorista más cercano es "
                + shortName(nearest.name) + ", a " + d + " kilómetros; para una compra de 100 soles "
                + "el flete sería de " + speakMoney(fee) + ", y es gratis desde "
                + speakMoney(nearest.freeShippingThreshold) + ".";
        return a;
    }

    private static Answer cheapestProducts(Ctx ctx) {
        List<Product> sorted = new ArrayList<>(ctx.products);
        sorted.sort((p1, p2) -> Double.compare(
                p1.getLowestPriceOffer().price, p2.getLowestPriceOffer().price));
        Answer a = new Answer();
        a.title = "🏷️ Los más económicos";
        StringBuilder sb = new StringBuilder("Los productos con mejor precio unitario son: ");
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Product p = sorted.get(i);
            SupplierOffer o = p.getLowestPriceOffer();
            a.rows.add("• " + p.name + " — desde S/ " + money(o.price)
                    + " (" + shortName(o.supplier.name) + ")");
            if (i < 3) sb.append(shortName(p.name)).append(" a ").append(speakMoney(o.price)).append(", ");
        }
        a.spoken = sb.toString();
        return a;
    }

    // ---------------- Utilidades ----------------

    private static double dist(DistrictLocation from, Supplier to) {
        return from.distanceTo(to.lat, to.lng);
    }

    private static String zoneShort(DistrictLocation l) {
        if (l == null) return "tu zona";
        int i = l.name.indexOf(" - ");
        return i > 0 ? l.name.substring(i + 3) : l.name;
    }

    private static String shortName(String name) {
        return name.length() > 42 ? name.substring(0, 42) + "…" : name;
    }

    private static String spacedPhone(String phone) {
        if (phone == null || phone.length() != 9) return phone;
        return phone.substring(0, 3) + " " + phone.substring(3, 6) + " " + phone.substring(6);
    }

    private static String money(double n) {
        return String.format(Locale.US, "%.2f", n);
    }

    /** Formato para voz: "23.50" → "23 soles con 50 céntimos". */
    private static String speakMoney(double n) {
        long soles = (long) n;
        int cent = Math.round((float) ((n - soles) * 100));
        if (cent == 0) return soles + " soles";
        if (soles == 0) return cent + " céntimos";
        return soles + " soles con " + cent + " céntimos";
    }
}
