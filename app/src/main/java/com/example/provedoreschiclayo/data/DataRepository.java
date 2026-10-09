package com.example.provedoreschiclayo.data;

import com.example.provedoreschiclayo.R;
import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.Supplier;
import com.example.provedoreschiclayo.models.SupplierOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de datos comerciales para la red de Chiclayo y Lambayeque
 */
public class DataRepository {

    public static List<DistrictLocation> getLocations() {
        List<DistrictLocation> locs = new ArrayList<>();
        locs.add(new DistrictLocation("jlo", "Chiclayo - Moshoqueque (JLO)", "Cerca al Mercado Mayorista", -6.7570, -79.8335));
        locs.add(new DistrictLocation("centro", "Chiclayo - Centro (Balta/Grau)", "Zona comercial cercado", -6.7713, -79.8409));
        locs.add(new DistrictLocation("victoria", "Chiclayo - La Victoria", "Av. Chinchaysuyo / Los Andes", -6.7905, -79.8460));
        locs.add(new DistrictLocation("lambayeque", "Lambayeque - Centro", "Calle Huamachuco / San Martín", -6.7042, -79.9056));
        locs.add(new DistrictLocation("pimentel", "Pimentel - Balneario", "Carretera y malecón", -6.8378, -79.9342));
        return locs;
    }

    public static List<Supplier> getSuppliers() {
        List<Supplier> sups = new ArrayList<>();
        // 1. Moshoqueque Central (El núcleo mayorista por excelencia de Chiclayo)
        sups.add(new Supplier(
                "sup_mosho",
                "Distribuidora Moshoqueque Central",
                "J.L. Ortiz",
                "Av. Kennedy 1240, Moshoqueque",
                "979123456",
                -6.7565, -79.8325,
                3.50, 0.75, 140.0, 4.8,
                "35 - 50 min",
                "⭐ Líder Moshoqueque"
        ));

        // 2. Chiclayo Centro
        sups.add(new Supplier(
                "sup_balta",
                "Comercializadora San José & Hnos",
                "Chiclayo Centro",
                "Av. José Balta 890, Chiclayo",
                "978234567",
                -6.7720, -79.8415,
                4.00, 0.85, 160.0, 4.6,
                "40 - 60 min",
                "⚡ Entrega Express"
        ));

        // 3. Lambayeque Centro
        sups.add(new Supplier(
                "sup_lamba",
                "Mega Abarrotes Lambayeque Norte",
                "Lambayeque",
                "Calle Huamachuco 512, Lambayeque",
                "974345678",
                -6.7035, -79.9048,
                3.50, 0.70, 130.0, 4.7,
                "45 - 70 min",
                "💰 Precios de Fábrica"
        ));

        // 4. La Victoria
        sups.add(new Supplier(
                "sup_victoria",
                "Depósito Mayorista La Victoria",
                "La Victoria",
                "Av. Chinchaysuyo 1420, La Victoria",
                "976456789",
                -6.7890, -79.8450,
                4.00, 0.90, 150.0, 4.5,
                "30 - 45 min",
                "📦 Stock Inmediato"
        ));

        // 5. Panamericana Norte (Bebidas e Hidratantes Mayoristas)
        sups.add(new Supplier(
                "sup_bebidas",
                "Distribuidora Bebidas del Norte SAC",
                "Panamericana Norte",
                "Km 7.8 Panamericana Norte, Chiclayo",
                "971567890",
                -6.7450, -79.8550,
                5.00, 0.60, 180.0, 4.9,
                "40 - 60 min",
                "🥤 Especialista Bebidas"
        ));

        return sups;
    }

    public static List<Product> getProducts(List<Supplier> suppliers) {
        Supplier sMosho = suppliers.get(0);
        Supplier sBalta = suppliers.get(1);
        Supplier sLamba = suppliers.get(2);
        Supplier sVictoria = suppliers.get(3);
        Supplier sBebidas = suppliers.get(4);

        List<Product> products = new ArrayList<>();

        // 1. SPORADE (Destacado del usuario)
        Product p1 = new Product("p_sporade", "Sporade 500ml", "Bebidas", "AJE", "Pack x 12 botellas surtidas", "⚡", R.drawable.img_sporade);
        p1.addOffer(new SupplierOffer(sMosho, 23.50, 180, "Pack x 12 (Mand., Manz., Berry)"));
        p1.addOffer(new SupplierOffer(sBalta, 24.20, 95, "Pack x 12 surtido sellado"));
        p1.addOffer(new SupplierOffer(sLamba, 22.80, 210, "Oferta Lambayeque x 12 unid."));
        p1.addOffer(new SupplierOffer(sBebidas, 23.00, 450, "Caja mayorista fábrica"));
        products.add(p1);

        // 2. GATORADE
        Product p2 = new Product("p_gatorade", "Gatorade 500ml", "Bebidas", "Pepsico", "Pack x 12 botellas", "🏃", R.drawable.img_gatorade);
        p2.addOffer(new SupplierOffer(sMosho, 28.50, 90, "Pack x 12 Tropical / Blue"));
        p2.addOffer(new SupplierOffer(sLamba, 27.90, 60, "Pack x 12 sabor Berry"));
        p2.addOffer(new SupplierOffer(sBebidas, 28.00, 300, "Pack x 12 distribuidor"));
        products.add(p2);

        // 3. INKA KOLA
        Product p3 = new Product("p_inkakola", "Inka Kola 3 Litros", "Bebidas", "Coca-Cola", "Fardo x 4 botellas retornables/desechables", "🥤", R.drawable.img_inkakola);
        p3.addOffer(new SupplierOffer(sMosho, 42.50, 140, "Fardo x 4 unidades no retornables"));
        p3.addOffer(new SupplierOffer(sBalta, 43.00, 75, "Fardo x 4 botellas 3L"));
        p3.addOffer(new SupplierOffer(sBebidas, 41.80, 320, "Fardo x 4 al por mayor"));
        products.add(p3);

        // 4. ARROZ COSTEÑO
        Product p4 = new Product("p_arroz", "Arroz Superior Costeño", "Abarrotes", "Costeño", "Saco sellado x 50 kg", "🌾", R.drawable.img_arroz);
        p4.addOffer(new SupplierOffer(sMosho, 168.00, 85, "Saco 50 kg grano seleccionado"));
        p4.addOffer(new SupplierOffer(sLamba, 165.00, 130, "Saco 50 kg directo molino"));
        p4.addOffer(new SupplierOffer(sVictoria, 167.50, 65, "Saco 50 kg almacén La Victoria"));
        products.add(p4);

        // 5. ACEITE PRIMOR
        Product p5 = new Product("p_aceite", "Aceite Primor Clásico 1L", "Abarrotes", "Alicorp", "Caja x 12 botellas 1 Litro", "🛢️", R.drawable.img_aceite);
        p5.addOffer(new SupplierOffer(sMosho, 86.00, 110, "Caja x 12 de 1000ml"));
        p5.addOffer(new SupplierOffer(sBalta, 87.50, 80, "Caja x 12 sellada"));
        p5.addOffer(new SupplierOffer(sVictoria, 85.50, 95, "Caja x 12 promoción"));
        products.add(p5);

        // 6. AZÚCAR CARTAVIO
        Product p6 = new Product("p_azucar", "Azúcar Rubia Cartavio", "Abarrotes", "Cartavio", "Saco industrial x 50 kg", "⚪", R.drawable.img_azucar);
        p6.addOffer(new SupplierOffer(sMosho, 142.00, 90, "Saco 50 kg cosechado norte"));
        p6.addOffer(new SupplierOffer(sLamba, 139.50, 140, "Saco 50 kg molino"));
        p6.addOffer(new SupplierOffer(sVictoria, 141.00, 50, "Saco 50 kg"));
        products.add(p6);

        // 7. LECHE GLORIA AZUL
        Product p7 = new Product("p_leche", "Leche Gloria Azul 400g", "Abarrotes", "Gloria", "Plancha x 24 latas", "🥛", R.drawable.img_leche);
        p7.addOffer(new SupplierOffer(sMosho, 88.00, 160, "Plancha 24 tarros etiqueta azul"));
        p7.addOffer(new SupplierOffer(sBalta, 89.50, 110, "Plancha 24 latas enteras"));
        p7.addOffer(new SupplierOffer(sLamba, 87.50, 180, "Plancha 24 unidades oferta"));
        products.add(p7);

        // 8. GALLETAS CASINO
        Product p8 = new Product("p_casino", "Galletas Casino Surtidas", "Golosinas", "Victoria", "Caja display x 24 paquetes", "🍪", R.drawable.img_casino);
        p8.addOffer(new SupplierOffer(sMosho, 18.50, 220, "Caja x 24 (Menta, Choc, Fresa)"));
        p8.addOffer(new SupplierOffer(sBalta, 19.00, 130, "Caja display x 24"));
        p8.addOffer(new SupplierOffer(sVictoria, 18.20, 150, "Caja display x 24 unidades"));
        products.add(p8);

        // 9. GALLETAS SODA SAN JORGE
        Product p9 = new Product("p_soda", "Galleta Soda San Jorge", "Golosinas", "San Jorge", "Caja display x 24 paquetes", "🥐", R.drawable.img_soda);
        p9.addOffer(new SupplierOffer(sMosho, 15.80, 190, "Caja display x 24 paquetes"));
        p9.addOffer(new SupplierOffer(sLamba, 15.20, 240, "Caja display x 24"));
        p9.addOffer(new SupplierOffer(sVictoria, 15.50, 100, "Caja display x 24"));
        products.add(p9);

        // 10. CHOCOLATE TRIÁNGULO
        Product p10 = new Product("p_triangulo", "Chocolate Triángulo D'Onofrio", "Golosinas", "Nestlé", "Display x 22 unidades 30g", "🍫", R.drawable.img_triangulo);
        p10.addOffer(new SupplierOffer(sMosho, 31.00, 120, "Display x 22 barras"));
        p10.addOffer(new SupplierOffer(sBalta, 31.50, 70, "Display x 22 unidades"));
        products.add(p10);

        // 11. ATÚN CAMPOMAR
        Product p11 = new Product("p_atun", "Atún Campomar Lomitos", "Abarrotes", "Campomar", "Caja x 24 latas en aceite 170g", "🐟", R.drawable.img_atun);
        p11.addOffer(new SupplierOffer(sMosho, 114.00, 80, "Caja x 24 latas selladas"));
        p11.addOffer(new SupplierOffer(sLamba, 112.50, 110, "Caja x 24 latas"));
        p11.addOffer(new SupplierOffer(sVictoria, 115.00, 60, "Caja x 24 unidades"));
        products.add(p11);

        // 12. DETERGENTE ACE
        Product p12 = new Product("p_ace", "Detergente Ace Floral 800g", "Limpieza", "P&G", "Fardo x 12 bolsas 800g", "🧼", R.drawable.img_ace);
        p12.addOffer(new SupplierOffer(sMosho, 54.00, 130, "Fardo x 12 bolsas"));
        p12.addOffer(new SupplierOffer(sVictoria, 53.50, 90, "Fardo x 12 unidades"));
        products.add(p12);

        return products;
    }
}
