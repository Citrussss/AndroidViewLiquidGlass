package com.kyant.backdrop.view.demo;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.kyant.backdrop.view.*;

/** Entire catalog uses Android Views; no Compose runtime or AndroidX UI dependencies. */
public class MainActivity extends Activity {
  private final String[] pages = {
    "Buttons",
    "Toggle",
    "Slider",
    "Bottom Tabs",
    "Dialog",
    "Lock Screen",
    "Control Center",
    "Magnifier",
    "Glass Playground",
    "Adaptive Luminance",
    "Progressive Blur",
    "Scroll Container",
    "Lazy Scroll Container"
  };
  private BackdropLayout root;
  private LinearLayout column;
  private int page = -1;
  private int nextViewId;
  private String wallpaperUri;

  private int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    if (b != null) wallpaperUri = b.getString("wallpaper");
    int initial = b == null ? getIntent().getIntExtra("page", -1) : b.getInt("page", -1);
    show(initial >= -1 && initial < pages.length ? initial : -1);
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("page", page);
    b.putString("wallpaper", wallpaperUri);
  }

  @Override
  public void onBackPressed() {
    if (page >= 0) show(-1);
    else super.onBackPressed();
  }

  private TextView text(String value, int size) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextColor(-1);
    t.setTextSize(size);
    t.setPadding(dp(6), dp(10), dp(6), dp(10));
    return t;
  }

  private void add(View v, int h) {
    if (v.getId() == View.NO_ID) v.setId(nextViewId++);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(h));
    p.setMargins(0, dp(8), 0, dp(8));
    column.addView(v, p);
  }

  private LiquidButton button(String name, Runnable action) {
    LiquidButton b = new LiquidButton(this);
    b.setText(name);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void title(String s) {
    column.addView(text(s, 18));
  }

  private void show(int index) {
    page = index;
    nextViewId = 1;
    root = new BackdropLayout(this);
    setWallpaper();
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setPadding(dp(24), dp(16), dp(24), dp(40));
    scroll.addView(column);
    root.foreground().addView(scroll);
    setContentView(root);
    if (index >= 0) add(button("‹  Catalog", () -> show(-1)), 48);
    column.addView(text(index < 0 ? "Liquid Glass" : pages[index], 34));
    column.addView(
        text(
            "ANDROID VIEWS  /  "
                + (android.os.Build.VERSION.SDK_INT >= 33
                    ? "LIVE REFRACTION"
                    : "COMPATIBILITY MODE"),
            12));
    if (index < 0) {
      title("A native View implementation of the Backdrop catalog");
      for (int i = 0; i < pages.length; i++) {
        final int n = i;
        add(button(pages[i] + "  ›", () -> show(n)), 58);
      }
      return;
    }
    switch (index) {
      case 0:
        buttons();
        break;
      case 1:
        toggles();
        break;
      case 2:
        sliders();
        break;
      case 3:
        tabs();
        break;
      case 4:
        dialogs();
        break;
      case 5:
        lock();
        break;
      case 6:
        controls();
        break;
      case 7:
        magnifier();
        break;
      case 8:
        playground();
        break;
      case 9:
        adaptive();
        break;
      case 10:
        progressive();
        break;
      case 11:
        scrolling(false);
        break;
      case 12:
        scrolling(true);
        break;
    }
    if (index != 11 && index != 12)
      add(
          button(
              "Pick an image",
              () ->
                  startActivityForResult(
                      new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
                          .setType("image/*")
                          .addCategory(android.content.Intent.CATEGORY_OPENABLE),
                      10)),
          52);
  }

  private void setWallpaper() {
    ImageView image = new ImageView(this);
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setImageResource(R.drawable.wallpaper_light);
    if (wallpaperUri != null) {
      try (java.io.InputStream in =
          getContentResolver().openInputStream(android.net.Uri.parse(wallpaperUri))) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, opts);
        opts.inSampleSize = 1;
        while (opts.outWidth / opts.inSampleSize > 2048
            || opts.outHeight / opts.inSampleSize > 4096) opts.inSampleSize *= 2;
        opts.inJustDecodeBounds = false;
        try (java.io.InputStream pixels =
            getContentResolver().openInputStream(android.net.Uri.parse(wallpaperUri))) {
          Bitmap bitmap = BitmapFactory.decodeStream(pixels, null, opts);
          if (bitmap != null) image.setImageBitmap(bitmap);
        }
      } catch (Exception e) {
        wallpaperUri = null;
      }
    }
    root.content().removeAllViews();
    root.content().addView(image, new FrameLayout.LayoutParams(-1, -1));
  }

  @Override
  protected void onActivityResult(int request, int result, android.content.Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 10 && result == RESULT_OK && data != null && data.getData() != null) {
      wallpaperUri = data.getData().toString();
      setWallpaper();
      root.invalidate();
    }
  }

  private void buttons() {
    title("Press to see the spring response");
    for (String s :
        new String[] {"Clear glass", "Frosted glass", "Tinted glass", "Chromatic lens"}) {
      LiquidButton b = button(s, () -> Toast.makeText(this, s, Toast.LENGTH_SHORT).show());
      if (s.startsWith("Frosted")) b.style.blur = 24;
      if (s.startsWith("Tinted")) b.style.tint = 0x666f52e8;
      if (s.startsWith("Clear")) b.style.blur = 0;
      add(b, 64);
    }
  }

  private void toggles() {
    for (String s : new String[] {"Wi-Fi", "Bluetooth", "Airplane mode", "Notifications"}) {
      title(s);
      LiquidToggle t = new LiquidToggle(this);
      t.setContentDescription(s);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(88), dp(48));
      p.setMargins(0, dp(10), 0, dp(10));
      column.addView(t, p);
      t.setId(nextViewId++);
    }
  }

  private void sliders() {
    for (String s : new String[] {"Volume", "Brightness", "Playback"}) {
      title(s);
      LiquidSlider slider = new LiquidSlider(this);
      slider.setContentDescription(s);
      TextView v = text("50%", 14);
      slider.setOnValueChangeListener(n -> v.setText(Math.round(n * 100) + "%"));
      add(slider, 52);
      column.addView(v);
    }
  }

  private void tabs() {
    TextView selected = text("Home selected", 22);
    column.addView(selected);
    LiquidBottomTabs tabs = new LiquidBottomTabs(this);
    tabs.setTabs("Home", "Explore", "Library", "Profile");
    tabs.setOnTabSelectedListener(
        i ->
            selected.setText(
                new String[] {"Home", "Explore", "Library", "Profile"}[i] + " selected"));
    add(tabs, 64);
  }

  private void dialogs() {
    add(
        button(
            "Open glass dialog",
            () -> {
              FrameLayout scrim = new FrameLayout(this);
              scrim.setBackgroundColor(0x55000000);
              scrim.setClickable(true);
              GlassView dialog = new GlassView(this);
              LinearLayout inner = new LinearLayout(this);
              inner.setOrientation(LinearLayout.VERTICAL);
              inner.setPadding(dp(24), dp(16), dp(24), dp(16));
              inner.addView(text("Liquid Glass", 26));
              inner.addView(
                  text("A native glass panel sampling the content behind this dialog.", 17));
              LiquidButton close = button("Done", () -> root.foreground().removeView(scrim));
              inner.addView(close, new LinearLayout.LayoutParams(-1, dp(52)));
              dialog.addView(inner);
              FrameLayout.LayoutParams p =
                  new FrameLayout.LayoutParams(dp(300), -2, Gravity.CENTER);
              scrim.addView(dialog, p);
              scrim.setOnClickListener(v -> root.foreground().removeView(scrim));
              dialog.setClickable(true);
              root.foreground().addView(scrim, new FrameLayout.LayoutParams(-1, -1));
            }),
        64);
  }

  private void lock() {
    title("Drag the original SDF texture clock");
    FrameLayout area = new FrameLayout(this);
    GlassView clock = new GlassView(this);
    clock.style.tint = 0;
    clock.style.highlight = 0;
    clock.style.shadow = 0;
    clock.style.radius = 0;
    clock.style.refractionAmount = 0;
    clock.style.blur = 2;
    clock.style.brightness = -.1f;
    clock.style.contrast = .75f;
    clock.style.saturation = 1.5f;
    Bitmap texture = BitmapFactory.decodeResource(getResources(), R.drawable.clock_sdf);
    clock.setSdfTexture(texture);
    if (android.os.Build.VERSION.SDK_INT < 33) {
      TextView fallback = text("09:41", 72);
      fallback.setGravity(Gravity.CENTER);
      clock.addView(fallback);
    }
    FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(-1, dp(160), Gravity.CENTER);
    area.addView(clock, size);
    draggable(clock);
    add(area, 360);
  }

  private void draggable(View view) {
    final float[] down = new float[4];
    view.setOnTouchListener(
        (v, e) -> {
          if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            down[0] = e.getRawX();
            down[1] = e.getRawY();
            down[2] = v.getTranslationX();
            down[3] = v.getTranslationY();
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
          }
          if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
            v.setTranslationX(down[2] + e.getRawX() - down[0]);
            v.setTranslationY(down[3] + e.getRawY() - down[1]);
            v.invalidate();
            return true;
          }
          if (e.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
          v.getParent().requestDisallowInterceptTouchEvent(false);
          return true;
        });
  }

  private void controls() {
    title("Connectivity");
    toggles();
    title("Media & brightness");
    sliders();
    add(
        button("Focus", () -> Toast.makeText(this, "Focus enabled", Toast.LENGTH_SHORT).show()),
        64);
  }

  private void magnifier() {
    title("Drag the lens across the artwork");
    FrameLayout area = new FrameLayout(this);
    GlassView lens = new GlassView(this);
    lens.style.zoom = 1.8f;
    lens.style.blur = 0;
    lens.style.radius = 90;
    TextView t = text("⊕", 40);
    t.setGravity(Gravity.CENTER);
    lens.addView(t, new FrameLayout.LayoutParams(-1, -1));
    area.addView(lens, new FrameLayout.LayoutParams(dp(150), dp(150)));
    final float[] down = new float[4];
    lens.setOnTouchListener(
        (v, e) -> {
          switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
              down[0] = e.getRawX();
              down[1] = e.getRawY();
              down[2] = v.getTranslationX();
              down[3] = v.getTranslationY();
              v.getParent().requestDisallowInterceptTouchEvent(true);
              return true;
            case MotionEvent.ACTION_MOVE:
              v.setTranslationX(
                  Math.max(
                      0,
                      Math.min(area.getWidth() - v.getWidth(), down[2] + e.getRawX() - down[0])));
              v.setTranslationY(
                  Math.max(
                      0,
                      Math.min(area.getHeight() - v.getHeight(), down[3] + e.getRawY() - down[1])));
              v.invalidate();
              return true;
            case MotionEvent.ACTION_UP:
              v.performClick();
            case MotionEvent.ACTION_CANCEL:
              v.getParent().requestDisallowInterceptTouchEvent(false);
              return true;
          }
          return true;
        });
    add(area, 420);
  }

  private void playground() {
    LiquidButton sample = button("LIVE GLASS", () -> {});
    GlassGestureController.attach(sample);
    add(sample, 140);
    parameter("Blur", sample.style.blur, 40, n -> sample.style.blur = n, sample);
    parameter("Corner radius", sample.style.radius, 80, n -> sample.style.radius = n, sample);
    parameter(
        "Refraction height",
        sample.style.refractionHeight,
        60,
        n -> sample.style.refractionHeight = n,
        sample);
    parameter(
        "Refraction amount",
        sample.style.refractionAmount,
        80,
        n -> sample.style.refractionAmount = n,
        sample);
    parameter("Saturation", sample.style.saturation, 2, n -> sample.style.saturation = n, sample);
    parameter("Highlight", sample.style.highlight, 1, n -> sample.style.highlight = n, sample);
    parameter("Zoom", 1, 3, n -> sample.style.zoom = Math.max(.1f, n), sample);
    for (String name : new String[] {"Depth effect", "Chromatic aberration"}) {
      title(name);
      LiquidToggle toggle = new LiquidToggle(this);
      toggle.setChecked(true);
      toggle.setOnCheckedChangeListener(
          (v, on) -> {
            if (name.startsWith("Depth")) sample.style.depthEffect = on;
            else sample.style.chromaticAberration = on;
            sample.invalidate();
          });
      add(toggle, 48);
    }
  }

  private interface Change {
    void set(float v);
  }

  private void parameter(String name, float initial, float max, Change change, GlassView sample) {
    TextView label = text(name + ": " + initial, 16);
    column.addView(label);
    SeekBar seek = new SeekBar(this);
    seek.setMax(1000);
    seek.setProgress((int) (initial / max * 1000));
    seek.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}

          public void onProgressChanged(SeekBar b, int p, boolean user) {
            float n = max * p / 1000;
            change.set(n);
            label.setText(name + ": " + String.format(java.util.Locale.ROOT, "%.2f", n));
            sample.invalidate();
          }
        });
    add(seek, 40);
  }

  private void adaptive() {
    title("Adaptive glass adjusts its shading from sampled background luminance");
    LiquidButton b = button("Adaptive luminance", () -> {});
    b.style.adaptiveLuminance = true;
    GlassGestureController.attach(b);
    add(b, 180);
    LiquidSlider s = new LiquidSlider(this);
    s.setOnValueChangeListener(
        n -> {
          b.setTranslationY(dp(n * 160));
          b.invalidate();
        });
    add(s, 48);
    title("Move the panel over the background");
  }

  private void progressive() {
    title("Original alpha-masked progressive blur");
    LiquidButton original = button("Alpha-masked progressive blur", () -> {});
    original.style.radius = 0;
    original.style.blur = 4;
    original.style.refractionAmount = 0;
    original.style.alphaMaskedBlur = true;
    original.style.highlight = 0;
    original.style.tint = 0;
    add(original, 128);
    title("Additional variable-radius blur");
    LiquidButton b = button("Variable-radius blur", () -> {});
    b.style.blur = 0;
    b.style.progressiveBlur = 24;
    add(b, 200);
    parameter("Progressive blur", 24, 48, n -> b.style.progressiveBlur = n, b);
  }

  private void scrolling(boolean lazy) {
    // Scroll the sampled source itself, while the foreground glass remains fixed.
    root.content().removeAllViews();
    if (lazy) {
      ListView list = new ListView(this);
      String[] rows = new String[200];
      for (int i = 0; i < rows.length; i++) rows[i] = "Recycled background item " + (i + 1);
      list.setAdapter(
          new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int p, View v, ViewGroup parent) {
              TextView t = (TextView) super.getView(p, v, parent);
              t.setTextColor(-1);
              t.setHeight(dp(80));
              t.setBackgroundColor(Color.HSVToColor(new float[] {p * 23 % 360, .55f, .65f}));
              return t;
            }
          });
      root.content().addView(list);
    } else {
      ScrollView source = new ScrollView(this);
      LinearLayout rows = new LinearLayout(this);
      rows.setOrientation(LinearLayout.VERTICAL);
      for (int i = 0; i < 40; i++) {
        TextView t = text("Background chapter " + (i + 1) + "\nScroll behind the glass", 24);
        t.setHeight(dp(160));
        t.setBackgroundColor(Color.HSVToColor(new float[] {i * 37 % 360, .5f, .65f}));
        rows.addView(t);
      }
      source.addView(rows);
      root.content().addView(source);
    }
    // Keep only a compact header in the foreground so touches reach the source below it.
    root.foreground().removeAllViews();
    GlassView panel = new GlassView(this);
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setPadding(dp(20), dp(8), dp(20), dp(8));
    LiquidButton back = button("‹ Catalog", () -> show(-1));
    header.addView(back, new LinearLayout.LayoutParams(-1, dp(44)));
    header.addView(text(lazy ? "Recycled list backdrop" : "Scrolling backdrop", 22));
    panel.addView(header);
    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(150), Gravity.TOP);
    p.setMargins(dp(16), dp(16), dp(16), 0);
    root.foreground().addView(panel, p);
  }
}
