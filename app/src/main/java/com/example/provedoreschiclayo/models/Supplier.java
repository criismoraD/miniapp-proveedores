package com.example.provedoreschiclayo.models;

/**
 * Proveedor / Distribuidor Mayorista en Chiclayo o Lambayeque
 */
public class Supplier {
    public final String id;
    public final String name;
    public final String district;
    public final String address;
    public final String phone;
    public final double lat;
    public final double lng;
    public final double baseDeliveryFee; // Tarifa base de delivery
    public final double perKmDeliveryFee; // Costo adicional por km
    public final double freeShippingThreshold; // Monto para envío gratis
    public final double rating;
    public final String deliveryTimeEstimate;
    public final String badge; // Ej: "Mayorista Moshoqueque", "Distribuidor Oficial"

    public Supplier(String id, String name, String district, String address, String phone,
                    double lat, double lng, double baseDeliveryFee, double perKmDeliveryFee,
                    double freeShippingThreshold, double rating, String deliveryTimeEstimate, String badge) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.address = address;
        this.phone = phone;
        this.lat = lat;
        this.lng = lng;
        this.baseDeliveryFee = baseDeliveryFee;
        this.perKmDeliveryFee = perKmDeliveryFee;
        this.freeShippingThreshold = freeShippingThreshold;
        this.rating = rating;
        this.deliveryTimeEstimate = deliveryTimeEstimate;
        this.badge = badge;
    }

    /**
     * Calcula el flete según la ubicación del usuario y el subtotal de compra
     */
    public double calculateDeliveryFee(DistrictLocation userLoc, double subtotal) {
        if (subtotal >= freeShippingThreshold) {
            return 0.0; // ¡Envío gratis alcanzado!
        }
        double dist = userLoc.distanceTo(lat, lng);
        // Tarifa realista moto-flete Chiclayo / Lambayeque
        double fee = baseDeliveryFee + (dist * perKmDeliveryFee);
        // Mínimo S/ 3.00, máximo S/ 16.00 dentro de la provincia
        return Math.round(Math.max(3.0, Math.min(fee, 18.0)) * 10.0) / 10.0;
    }
}
