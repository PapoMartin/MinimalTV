package com.valen.minilauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Minimal TV: launcher minimalista para Android TV 5.1+.
 * OK = abrir app. Mantener OK = agregar/quitar de Favoritos.
 */
public class MainActivity extends Activity {

    private static final int COLS = 5;
    private static final int TILE_W = 144;
    private static final int TILE_H = 81;
    private static final String SETTINGS_PKG = "__ajustes__";

    private static final int[] PALETTE = {
            0xFF2B3A55, 0xFF3D2B55, 0xFF24504A, 0xFF55402B, 0xFF552F3F, 0xFF2B4C55
    };

    static class App {
        String pkg;
        String cls;
        String category;
        String label;
        Drawable banner;
        Drawable icon;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Locale es = new Locale("es", "AR");

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout content;
    private TextView greeting, clock, date;
    private String focusTag;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 10_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.bg);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setSmoothScrollingEnabled(true);

        LinearLayout main = vertical();
        main.setPadding(dp(56), dp(36), dp(56), dp(56));
        main.setClipChildren(false);
        main.setClipToPadding(false);

        // Encabezado: saludo a la izquierda, hora y fecha a la derecha
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), 0, dp(10), 0);

        greeting = text(28, 0xFFFFFFFF, "sans-serif-light");
        header.addView(greeting, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = vertical();
        right.setGravity(Gravity.END);
        clock = text(36, 0xFFFFFFFF, "sans-serif-thin");
        clock.setGravity(Gravity.END);
        date = text(14, 0x99FFFFFF, "sans-serif");
        date.setGravity(Gravity.END);
        right.addView(clock);
        right.addView(date);
        header.addView(right);
        main.addView(header);

        content = vertical();
        content.setClipChildren(false);
        content.setClipToPadding(false);
        main.addView(content);

        scroll.addView(main);
        root.addView(scroll);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Botón Home estando ya en el launcher: volver arriba
        scroll.smoothScrollTo(0, 0);
        focusTag = null;
        focusFirst();
    }

    @Override
    public void onBackPressed() {
        // Un launcher no se cierra con Atrás
    }

    // ---------- Construcción de la pantalla ----------

    private void build() {
        content.removeAllViews();
        List<App> apps = loadApps();
        Set<String> favs = prefs.getStringSet("favs", new HashSet<String>());

        List<App> favList = new ArrayList<>();
        for (App a : apps) if (favs.contains(a.pkg)) favList.add(a);

        if (!favList.isEmpty()) {
            content.addView(section("Favoritos"));
            addGrid(favList, true);
            content.addView(section("Todas las apps"));
        } else {
            content.addView(section("Tus apps  ·  mantené OK para fijar en Favoritos"));
        }
        addGrid(apps, false);

        View target = focusTag != null ? content.findViewWithTag(focusTag) : null;
        if (target != null) target.requestFocus();
        else focusFirst();
    }

    private void focusFirst() {
        if (content.getChildCount() > 1) {
            View grid = content.getChildAt(1);
            View first = findFirstFocusable(grid);
            if (first != null) first.requestFocus();
        }
    }

    private View findFirstFocusable(View v) {
        if (v.isFocusable()) return v;
        if (v instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) v;
            for (int i = 0; i < l.getChildCount(); i++) {
                View f = findFirstFocusable(l.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }

    private void addGrid(List<App> list, boolean inFav) {
        LinearLayout grid = vertical();
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        LinearLayout row = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(10), dp(4), dp(10), dp(20));
            row.addView(makeCell(list.get(i), inFav), lp);
        }
        content.addView(grid);
    }

    private View makeCell(final App app, final boolean inFav) {
        final LinearLayout cell = vertical();
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setFocusable(true);
        cell.setClickable(true);
        cell.setClipChildren(false);
        cell.setTag(app.pkg + (inFav ? "#f" : "#a"));

        final FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(app.banner != null ? 0xFF1A1C22 : colorFor(app.pkg));
        card.setBackground(bg);
        card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        card.setClipToOutline(true);

        ImageView img = new ImageView(this);
        if (app.banner != null) {
            img.setImageDrawable(app.banner);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            img.setImageDrawable(app.icon);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER));
        }
        cell.addView(card, new LinearLayout.LayoutParams(dp(TILE_W), dp(TILE_H)));

        final TextView label = text(13, 0x80FFFFFF, "sans-serif");
        label.setText(app.label);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(TILE_W), LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(10);
        cell.addView(label, llp);

        cell.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                float s = has ? 1.12f : 1f;
                cell.animate().scaleX(s).scaleY(s).setDuration(160).start();
                card.setElevation(has ? dp(18) : 0);
                bg.setStroke(has ? dp(2) : 0, 0xDDFFFFFF);
                label.setTextColor(has ? 0xFFFFFFFF : 0x80FFFFFF);
            }
        });

        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(app);
            }
        });

        cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleFav(app);
                return true;
            }
        });
        return cell;
    }

    private View section(String title) {
        TextView t = text(15, 0xB3FFFFFF, "sans-serif-medium");
        t.setText(title);
        t.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(30), 0, dp(12));
        t.setLayoutParams(lp);
        return t;
    }

    // ---------- Apps ----------

    private List<App> loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, App> map = new LinkedHashMap<>();
        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, map);
        collect(pm, Intent.CATEGORY_LAUNCHER, map);

        List<App> list = new ArrayList<>(map.values());
        final Collator col = Collator.getInstance(es);
        Collections.sort(list, new Comparator<App>() {
            @Override
            public int compare(App a, App b) {
                return col.compare(a.label, b.label);
            }
        });

        App s = new App();
        s.pkg = SETTINGS_PKG;
        s.label = "Ajustes";
        s.icon = getResources().getDrawable(android.R.drawable.ic_menu_preferences, null);
        list.add(s);
        return list;
    }

    private void collect(PackageManager pm, String category, Map<String, App> map) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> found;
        try {
            found = pm.queryIntentActivities(i, 0);
        } catch (Exception e) {
            return;
        }
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || map.containsKey(pkg)) continue;
            App a = new App();
            a.pkg = pkg;
            a.cls = ri.activityInfo.name;
            a.category = category;
            a.label = ri.loadLabel(pm).toString();
            try {
                a.banner = ri.activityInfo.loadBanner(pm);
                if (a.banner == null) a.banner = ri.activityInfo.applicationInfo.loadBanner(pm);
            } catch (Exception ignored) {
            }
            a.icon = ri.loadIcon(pm);
            map.put(pkg, a);
        }
    }

    private void launch(App app) {
        try {
            if (SETTINGS_PKG.equals(app.pkg)) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                return;
            }
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(app.category);
            i.setComponent(new ComponentName(app.pkg, app.cls));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "No se pudo abrir " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFav(App app) {
        Set<String> favs = new HashSet<>(prefs.getStringSet("favs", new HashSet<String>()));
        boolean added = !favs.contains(app.pkg);
        if (added) favs.add(app.pkg);
        else favs.remove(app.pkg);
        prefs.edit().putStringSet("favs", favs).apply();
        Toast.makeText(this, added ? app.label + " agregado a Favoritos" : app.label + " quitado de Favoritos",
                Toast.LENGTH_SHORT).show();
        focusTag = app.pkg + (added ? "#f" : "#a");
        build();
    }

    // ---------- Reloj ----------

    private void updateClock() {
        Date now = new Date();
        clock.setText(new SimpleDateFormat("HH:mm", es).format(now));
        String d = new SimpleDateFormat("EEEE d 'de' MMMM", es).format(now);
        date.setText(Character.toUpperCase(d.charAt(0)) + d.substring(1));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 6 ? "Buenas noches" : h < 13 ? "Buen día" : h < 20 ? "Buenas tardes" : "Buenas noches");
    }

    // ---------- Utilidades ----------

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(float sp, int color, String family) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        return t;
    }

    private int colorFor(String pkg) {
        return PALETTE[(pkg.hashCode() & 0x7fffffff) % PALETTE.length];
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
