package com.example.provedoreschiclayo.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Producto comercial mayorista para tiendas de abarrotes y golosinas
 */
public class Product {
    public final String id;
    public final String name;
    public final String category;
    public final String brand;
    public final String presentation;
    public final String emoji;
    public final int imageResId;
    public final double rating;
    public final int reviewCount;
    public final List<SupplierOffer> offers = new ArrayList<>();

    public Product(String id, String name, String category, String brand, String presentation, String emoji, int imageResId) {
        this(id, name, category, brand, presentation, emoji, imageResId, 4.8, 120);
    }

    public Product(String id, String name, String category, String brand, String presentation, String emoji, int imageResId, double rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.presentation = presentation;
        this.emoji = emoji;
        this.imageResId = imageResId;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public void addOffer(SupplierOffer offer) {
        offers.add(offer);
    }

    /**
     * Retorna la oferta con menor precio unitario de producto
     */
    public SupplierOffer getLowestPriceOffer() {
        if (offers.isEmpty()) return null;
        SupplierOffer best = offers.get(0);
        for (SupplierOffer o : offers) {
            if (o.price < best.price) {
                best = o;
            }
        }
        return best;
    }

    /**
     * Retorna la oferta más cercana a la ubicación de la bodega del usuario
     */
    public SupplierOffer getClosestOffer(DistrictLocation userLoc) {
        if (offers.isEmpty()) return null;
        SupplierOffer closest = offers.get(0);
        double minDistance = userLoc.distanceTo(closest.supplier.lat, closest.supplier.lng);
        for (SupplierOffer o : offers) {
            double d = userLoc.distanceTo(o.supplier.lat, o.supplier.lng);
            if (d < minDistance) {
                minDistance = d;
                closest = o;
            }
        }
        return closest;
    }

    /**
     * Retorna la oferta más conveniente (Costo total = Producto + Flete de entrega)
     */
    public SupplierOffer getMostConvenientOffer(DistrictLocation userLoc, int qty) {
        if (offers.isEmpty()) return null;
        SupplierOffer best = offers.get(0);
        double minTotal = best.calculateTotalCost(userLoc, qty);
        for (SupplierOffer o : offers) {
            double tot = o.calculateTotalCost(userLoc, qty);
            if (tot < minTotal) {
                minTotal = tot;
                best = o;
            }
        }
        return best;
    }
}
