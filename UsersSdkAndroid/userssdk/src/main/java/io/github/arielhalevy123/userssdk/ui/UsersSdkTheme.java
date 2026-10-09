package io.github.arielhalevy123.userssdk.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

import io.github.arielhalevy123.userssdk.R;

/**
 * The look of the SDK's built-in screens, read from the host app's theme.
 *
 * An app brands every SDK screen by setting these attributes in its theme:
 * {@code usersSdkColorPrimary}, {@code usersSdkColorOnPrimary}, {@code usersSdkColorSurface},
 * {@code usersSdkColorCard}, {@code usersSdkColorOnSurface}, {@code usersSdkColorMuted},
 * {@code usersSdkCornerRadius} and {@code usersSdkFontFamily}. Any attribute it leaves out
 * falls back to the matching Material colour, so an unbranded app still gets screens that
 * match it.
 */
public final class UsersSdkTheme {

    @ColorInt public final int primary;
    @ColorInt public final int onPrimary;
    @ColorInt public final int surface;
    @ColorInt public final int card;
    @ColorInt public final int onSurface;
    @ColorInt public final int muted;
    public final float cornerRadiusPx;
    @Nullable public final Typeface typeface;

    private UsersSdkTheme(Context c) {
        primary = color(c, R.attr.usersSdkColorPrimary,
                MaterialColors.getColor(c, com.google.android.material.R.attr.colorPrimary, 0xFF6200EE));
        onPrimary = color(c, R.attr.usersSdkColorOnPrimary,
                MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF));
        surface = color(c, R.attr.usersSdkColorSurface,
                MaterialColors.getColor(c, android.R.attr.colorBackground, 0xFFFFFFFF));
        card = color(c, R.attr.usersSdkColorCard,
                MaterialColors.getColor(c, com.google.android.material.R.attr.colorSurface, surface));
        onSurface = color(c, R.attr.usersSdkColorOnSurface,
                MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F));
        muted = color(c, R.attr.usersSdkColorMuted, ColorUtils.setAlphaComponent(onSurface, 0x99));
        cornerRadiusPx = dimension(c, R.attr.usersSdkCornerRadius,
                12 * c.getResources().getDisplayMetrics().density);
        typeface = font(c, R.attr.usersSdkFontFamily);
    }

    /** The theme as the given context (an activity or a fragment's context) sees it. */
    @NonNull
    public static UsersSdkTheme of(@NonNull Context context) {
        return new UsersSdkTheme(context);
    }

    /** A rounded rectangle in the theme's corner radius. */
    @NonNull
    public GradientDrawable rounded(@ColorInt int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(cornerRadiusPx);
        return d;
    }

    /** A filled circle, for marked calendar days and count badges. */
    @NonNull
    public GradientDrawable dot(@ColorInt int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        return d;
    }

    @NonNull
    public ColorStateList primaryTint() {
        return ColorStateList.valueOf(primary);
    }

    /** Paints a screen's background and gives every text in it the theme's ink and font. */
    public void applyToScreen(@NonNull View root) {
        root.setBackgroundColor(surface);
        applyText(root);
    }

    private void applyText(View v) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            tv.setTextColor(onSurface);
            if (typeface != null) tv.setTypeface(typeface, tv.getTypeface() != null ? tv.getTypeface().getStyle() : Typeface.NORMAL);
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyText(g.getChildAt(i));
        }
    }

    /** Sets the theme's font on one text, keeping its bold/italic style. */
    public void applyFont(@NonNull TextView tv) {
        if (typeface != null) tv.setTypeface(typeface, tv.getTypeface() != null ? tv.getTypeface().getStyle() : Typeface.NORMAL);
    }

    @ColorInt
    private static int color(Context c, @AttrRes int attr, @ColorInt int fallback) {
        TypedValue tv = new TypedValue();
        if (!c.getTheme().resolveAttribute(attr, tv, true)) return fallback;
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) return tv.data;
        if (tv.resourceId != 0) return ContextCompat.getColor(c, tv.resourceId);
        return fallback;
    }

    private static float dimension(Context c, @AttrRes int attr, float fallback) {
        TypedValue tv = new TypedValue();
        if (!c.getTheme().resolveAttribute(attr, tv, true) || tv.type != TypedValue.TYPE_DIMENSION) return fallback;
        return tv.getDimension(c.getResources().getDisplayMetrics());
    }

    @Nullable
    private static Typeface font(Context c, @AttrRes int attr) {
        TypedValue tv = new TypedValue();
        if (!c.getTheme().resolveAttribute(attr, tv, true) || tv.resourceId == 0) return null;
        try {
            return ResourcesCompat.getFont(c, tv.resourceId);
        } catch (Exception e) {
            return null;
        }
    }
}
