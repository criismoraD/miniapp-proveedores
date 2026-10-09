package com.example.provedoreschiclayo.models;

/**
 * Oferta de un producto específico por un proveedor determinado
 */
public class SupplierOffer {
    public final Supplier supplier;
    public final double price; // Precio por paquete o saco mayorista
    public final int stock;
    public final String note; // Ej: "Pack 12 unid surtidas", "Saco 50kg sellado"

    public SupplierOffer(Supplier supplier, double price, int stock, String note) {
        this.supplier = supplier;
        this.price = price;
        this.stock = stock;
        this.note = note;
    }

    /**
     * Calcula el costo total considerando producto + delivery a la bodega del usuario
     */
    public double calculateTotalCost(DistrictLocation userLoc, int qty) {
        double subtotal = price * qty;
        double flete = supplier.calculateDeliveryFee(userLoc, subtotal);
        return subtotal + flete;
    }
}
