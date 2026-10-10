package com.example.provedoreschiclayo.ui;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.List;

/**
 * Animaciones reutilizables con sensación de papel y tinta:
 * movimientos cortos, con desaceleración suave y un leve rebote de "sello".
 */
public final class Motion {

    private static final OvershootInterpolator SETTLE = new OvershootInterpolator(1.2f);
    private static final DecelerateInterpolator ENTER = new DecelerateInterpolator(1.5f);
    private static final AccelerateInterpolator EXIT = new AccelerateInterpolator(1.2f);

    private Motion() {
    }

    /** Convierte dp a píxeles según la pantalla de la vista. */
    public static float dp(View view, float dp) {
        DisplayMetrics metrics = view.getResources().getDisplayMetrics();
        return dp * metrics.density;
    }

    /**
     * Pulsación tipo papel que se hunde ligeramente y vuelve con rebote.
     * Devuelve false en los eventos táctiles para que el ripple y el click sigan funcionando.
     */
    public static void pressScale(final View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(110).setInterpolator(ENTER).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().cancel();
                    v.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(SETTLE).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    /** Entrada escalonada: cada elemento aparece un poco después que el anterior. */
    public static void staggerIn(List<View> views, long stepMs) {
        for (int i = 0; i < views.size(); i++) {
            View v = views.get(i);
            v.animate().cancel();
            v.setAlpha(0f);
            v.setTranslationY(dp(v, 18f));
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(Math.min(i, 6) * stepMs)
                    .setDuration(420)
                    .setInterpolator(ENTER)
                    .start();
        }
    }

    /** Rebote corto: se usa para cambios de cantidad, contadores y selección. */
    public static void popBump(final View view) {
        view.animate().cancel();
        view.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(120)
                .setInterpolator(ENTER)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(260)
                        .setInterpolator(SETTLE)
                        .start())
                .start();
    }

    /** Sello de tinta: cae con escala grande, se asienta girado y se desvanece. */
    public static void stamp(final View stamp) {
        stamp.animate().cancel();
        stamp.setAlpha(0f);
        stamp.setScaleX(1.8f);
        stamp.setScaleY(1.8f);
        stamp.setRotation(-22f);
        stamp.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(-10f)
                .setDuration(240)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .withEndAction(() -> stamp.animate()
                        .alpha(0f)
                        .setStartDelay(950)
                        .setDuration(380)
                        .setInterpolator(EXIT)
                        .start())
                .start();
    }

    /** Cambia el texto con un fundido corto (sin saltos bruscos). */
    public static void fadeSwapText(final TextView view, final CharSequence text) {
        if (text == null || text.toString().contentEquals(view.getText())) {
            return;
        }
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .setDuration(110)
                .setInterpolator(EXIT)
                .withEndAction(() -> {
                    view.setText(text);
                    view.animate().alpha(1f).setDuration(220).setInterpolator(ENTER).start();
                })
                .start();
    }

    /** Aparición de una barra desde abajo (barra de pedido). */
    public static void slideUpIn(final View view) {
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 120f));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(SETTLE)
                .start();
    }

    /** Salida de una barra hacia abajo; al terminar la oculta. */
    public static void slideDownOut(final View view) {
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .translationY(dp(view, 120f))
                .setDuration(260)
                .setInterpolator(EXIT)
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setTranslationY(0f);
                    view.setAlpha(1f);
                })
                .start();
    }

    /** Aparición suave de un bloque (estado vacío, avisos). */
    public static void fadeRiseIn(final View view) {
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 14f));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(ENTER)
                .start();
    }

    /**
     * Máquina de escribir: revela el texto letra a letra sin mover el diseño,
     * ocultando las letras pendientes con color transparente.
     */
    public static void typewriter(final TextView view, final CharSequence text, long durationMs) {
        final int length = text.length();
        ValueAnimator animator = ValueAnimator.ofInt(0, length);
        animator.setDuration(durationMs);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            int shown = (Integer) a.getAnimatedValue();
            Spannable spannable = new SpannableString(text);
            if (shown < length) {
                spannable.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), shown, length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            view.setText(spannable);
        });
        animator.start();
    }
}
