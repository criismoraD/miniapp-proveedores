package com.example.provedoreschiclayo.adapters;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.provedoreschiclayo.R;
import com.example.provedoreschiclayo.models.DistrictLocation;
import com.example.provedoreschiclayo.models.Product;
import com.example.provedoreschiclayo.models.SupplierOffer;
import com.example.provedoreschiclayo.ui.Motion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductActionListener {
        void onCompareSuppliers(Product product);
        void onQuickAddToCart(Product product, SupplierOffer offer);
    }

    private static final ColorMatrixColorFilter PLATE_FILTER = buildPlateFilter();

    private final Context context;
    private final List<Product> productList = new ArrayList<>();
    private DistrictLocation currentLocation;
    private String sortMode = "convenient"; // "convenient", "price", "distance", "delivery"
    private final OnProductActionListener actionListener;

    public ProductAdapter(Context context, DistrictLocation currentLocation, OnProductActionListener listener) {
        this.context = context;
        this.currentLocation = currentLocation;
        this.actionListener = listener;
    }

    public void updateData(List<Product> newProducts, DistrictLocation newLocation, String newSortMode) {
        this.productList.clear();
        if (newProducts != null) {
            this.productList.addAll(newProducts);
        }
        this.currentLocation = newLocation;
        this.sortMode = newSortMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, currentLocation, sortMode, actionListener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    /** Lámina ligeramente desaturada y cálida, como una fotografía impresa. */
    private static ColorMatrixColorFilter buildPlateFilter() {
        ColorMatrix saturation = new ColorMatrix();
        saturation.setSaturation(0.84f);
        ColorMatrix warmth = new ColorMatrix(new float[]{
                1f, 0f, 0f, 0f, 6f,
                0f, 1f, 0f, 0f, 3f,
                0f, 0f, 1f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
        });
        saturation.postConcat(warmth);
        return new ColorMatrixColorFilter(saturation);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivProductImage;
        final TextView tvBrandCategory;
        final TextView tvName;
        final TextView tvPresentation;
        final TextView tvStamp;

        final TextView tvBestSupplierName;
        final TextView tvSupplierDistance;
        final TextView tvProductUnitPrice;
        final TextView tvDeliveryBadge;
        final TextView tvTotalCalculatedPrice;
        final TextView tvSavingsHint;

        final TextView btnCompare;
        final TextView btnQuickAdd;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            ivProductImage.setColorFilter(PLATE_FILTER);
            tvBrandCategory = itemView.findViewById(R.id.tvProductBrandCategory);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPresentation = itemView.findViewById(R.id.tvProductPresentation);
            tvStamp = itemView.findViewById(R.id.tvStamp);

            tvBestSupplierName = itemView.findViewById(R.id.tvBestSupplierName);
            tvSupplierDistance = itemView.findViewById(R.id.tvSupplierDistance);
            tvProductUnitPrice = itemView.findViewById(R.id.tvProductUnitPrice);
            tvDeliveryBadge = itemView.findViewById(R.id.tvDeliveryBadge);
            tvTotalCalculatedPrice = itemView.findViewById(R.id.tvTotalCalculatedPrice);
            tvSavingsHint = itemView.findViewById(R.id.tvSavingsHint);

            btnCompare = itemView.findViewById(R.id.btnCompareSuppliers);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAdd);

            Motion.pressScale(itemView);
        }

        void bind(Product product, DistrictLocation userLoc, String sortMode, OnProductActionListener listener) {
            // Lámina real del producto
            if (product.imageResId != 0) {
                ivProductImage.setImageResource(product.imageResId);
            } else {
                ivProductImage.setImageResource(R.drawable.ic_product_placeholder);
            }

            tvBrandCategory.setText(product.category.toUpperCase(Locale.ROOT) + " · " + product.brand);
            tvName.setText(product.name);
            tvPresentation.setText(product.presentation);
            tvStamp.setAlpha(0f);

            // Mejor oferta según el criterio activo
            SupplierOffer bestOffer = getSortedTopOffer(product, userLoc, sortMode);

            if (bestOffer != null) {
                double dist = userLoc.distanceTo(bestOffer.supplier.lat, bestOffer.supplier.lng);
                double flete = bestOffer.supplier.calculateDeliveryFee(userLoc, bestOffer.price);
                double total = bestOffer.price + flete;

                tvBestSupplierName.setText(bestOffer.supplier.name);
                tvSupplierDistance.setText(String.format(Locale.US, "%.1f km", dist));

                tvProductUnitPrice.setText(String.format(Locale.US, "S/ %.2f", bestOffer.price));
                if (flete <= 3.50) {
                    tvDeliveryBadge.setText("Flete S/ 3.50");
                } else {
                    tvDeliveryBadge.setText(String.format(Locale.US, "Flete S/ %.2f", flete));
                }

                tvTotalCalculatedPrice.setText(String.format(Locale.US, "Total en tu local: S/ %.2f", total));

                // Ahorro frente a la opción más cara
                double highestTotal = 0;
                for (SupplierOffer o : product.offers) {
                    double t = o.calculateTotalCost(userLoc, 1);
                    if (t > highestTotal) {
                        highestTotal = t;
                    }
                }
                double diff = highestTotal - total;
                if (diff >= 1.50) {
                    tvSavingsHint.setVisibility(View.VISIBLE);
                    tvSavingsHint.setText(String.format(Locale.US,
                            "Ahorras S/ %.2f comprando aquí frente a otros distribuidores", diff));
                } else {
                    tvSavingsHint.setVisibility(View.GONE);
                }

                btnQuickAdd.setOnClickListener(v -> {
                    // Sello de tinta como confirmación visual inmediata
                    Motion.stamp(tvStamp);
                    Motion.popBump(btnQuickAdd);
                    if (listener != null) listener.onQuickAddToCart(product, bestOffer);
                });
            }

            btnCompare.setText("Comparar (" + product.offers.size() + ")");
            btnCompare.setOnClickListener(v -> {
                if (listener != null) listener.onCompareSuppliers(product);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCompareSuppliers(product);
            });
        }

        private SupplierOffer getSortedTopOffer(Product product, DistrictLocation userLoc, String sortMode) {
            if (product.offers.isEmpty()) return null;

            if ("price".equals(sortMode)) {
                return product.getLowestPriceOffer();
            } else if ("distance".equals(sortMode)) {
                return product.getClosestOffer(userLoc);
            } else if ("delivery".equals(sortMode)) {
                SupplierOffer lowestFleteOffer = product.offers.get(0);
                double minFlete = lowestFleteOffer.supplier.calculateDeliveryFee(userLoc, lowestFleteOffer.price);
                for (SupplierOffer o : product.offers) {
                    double f = o.supplier.calculateDeliveryFee(userLoc, o.price);
                    if (f < minFlete) {
                        minFlete = f;
                        lowestFleteOffer = o;
                    }
                }
                return lowestFleteOffer;
            } else {
                // Por defecto: más conveniente (precio + flete)
                return product.getMostConvenientOffer(userLoc, 1);
            }
        }
    }
}
