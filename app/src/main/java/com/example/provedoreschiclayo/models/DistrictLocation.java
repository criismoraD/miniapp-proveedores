package com.example.provedoreschiclayo.models;

/**
 * Zonas y distritos comerciales clave de Chiclayo y Lambayeque
 */
public class DistrictLocation {
    public final String id;
    public final String name;
    public final String reference;
    public final double lat;
    public final double lng;

    public DistrictLocation(String id, String name, String reference, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.reference = reference;
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * Calcula distancia aproximada en kilómetros (fórmula de Haversine)
     */
    public double distanceTo(double targetLat, double targetLng) {
        final int R = 6371; // Radio de la Tierra en km
        double latDistance = Math.toRadians(targetLat - this.lat);
        double lonDistance = Math.toRadians(targetLng - this.lng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0; // Redondeado a 1 decimal
    }
}
