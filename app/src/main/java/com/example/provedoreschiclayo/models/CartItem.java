package com.example.provedoreschiclayo.models;

/**
 * Elemento agregado al pedido de abastecimiento
 */
public class CartItem {
    public final Product product;
    public final SupplierOffer offer;
    public int quantity;

    public CartItem(Product product, SupplierOffer offer, int quantity) {
        this.product = product;
        this.offer = offer;
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return offer.price * quantity;
    }
}
