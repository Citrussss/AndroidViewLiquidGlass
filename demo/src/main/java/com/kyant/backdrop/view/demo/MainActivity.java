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
  private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
  private int page = -1;
  private int nextViewId;
  private String wallpaperUri;

  private int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    if (android.os.Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

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
    scroll.setClipChildren(false);
    column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setClipChildren(false);
    column.setClipToPadding(false);
    column.setPadding(0, 0, 0, 0);
    column.setGravity(Gravity.CENTER);
    scroll.addView(column);
    root.foreground().addView(scroll);
    setContentView(root);
    if (index < 0) {
      root.content().removeAllViews();
      boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
      int ink = dark ? Color.WHITE : Color.BLACK;
      root.setBackgroundColor(dark ? 0xff121212 : Color.WHITE);
      scroll.setOnApplyWindowInsetsListener((view, insets) -> {
        view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
        return insets;
      });
      column.setGravity(Gravity.TOP);
      TextView heading = text("Backdrop Catalog", 28);
      heading.setTextColor(ink);
      heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      heading.setIncludeFontPadding(false);
      heading.setPadding(dp(16), dp(40), dp(16), dp(32));
      column.addView(heading, new LinearLayout.LayoutParams(-1, -2));
      for (int i = 0; i < pages.length; i++) {
        if (i == 0 || i == 5 || i == 8) {
          TextView section = text(i == 0 ? "Liquid glass components" : i == 5 ? "System UIs" : "Experiments", 15);
          section.setTextColor(0xff0088ff);
          section.setIncludeFontPadding(false);
          section.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
          section.setPadding(dp(16), dp(24), dp(16), dp(8));
          column.addView(section, new LinearLayout.LayoutParams(-1, -2));
        }
        final int n = i;
        TextView row = text(pages[i], 17);
        row.setOnClickListener(v -> show(n));
        row.setTextColor(ink);
        row.setIncludeFontPadding(false);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        column.addView(row, new LinearLayout.LayoutParams(-1, -2));
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
    LiquidButton picker = button(
              "Pick an image",
              () ->
                  startActivityForResult(
                      new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
                          .setType("image/*")
                          .addCategory(android.content.Intent.CATEGORY_OPENABLE),
                      10));
    picker.style.tint = 0xbf0088ff;
    picker.style.hueTint = true;
    picker.style.shadow = 0;
    picker.style.blur = 2;
    picker.style.saturation = 1.5f;
    FrameLayout.LayoutParams pick = new FrameLayout.LayoutParams(dp(148), dp(56), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    pick.bottomMargin = dp(16);
    picker.setOnApplyWindowInsetsListener((view, insets) -> {
      FrameLayout.LayoutParams bounds = (FrameLayout.LayoutParams) view.getLayoutParams();
      bounds.bottomMargin = dp(16) + insets.getSystemWindowInsetBottom();
      view.setLayoutParams(bounds);
      return insets;
    });
    root.foreground().addView(picker, pick);
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
    String[] labels = {"Transparent Liquid Button", "Surface Liquid Button", "Tinted Liquid Button", "Tinted Liquid Button"};
    int[] tints = {0, 0x4dffffff, 0xbf0088ff, 0xbfff8d28};
    for (int i = 0; i < labels.length; i++) {
      LiquidButton b = button(labels[i], () -> {});
      b.style.blur = 2;
      b.style.saturation = 1.5f;
      b.style.refractionHeight = 12;
      b.style.refractionAmount = 24;
      b.style.tint = tints[i];
      b.style.hueTint = i >= 2;
      b.style.shadow = 0;
      b.setTextColor(i < 2 ? Color.BLACK : Color.WHITE);
      b.setTextSize(15);
      Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
      measure.setTextSize(15 * getResources().getDisplayMetrics().scaledDensity);
      LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams((int) Math.ceil(measure.measureText(labels[i])) + dp(32), dp(48));
      bounds.topMargin = i == 0 ? 0 : dp(16);
      column.addView(b, bounds);
    }
  }

  private void toggles() {
    LiquidToggle first = new LiquidToggle(this);
    LiquidToggle second = new LiquidToggle(this);
    LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(64), dp(28));
    column.addView(first, size);
    BackdropLayout panel = comparisonPanel();
    panel.foreground().addView(second, new FrameLayout.LayoutParams(dp(64), dp(28), Gravity.CENTER));
    LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(dp(176), dp(76));
    bounds.topMargin = dp(40);
    bounds.bottomMargin = dp(24);
    column.addView(panel, bounds);
    first.setOnCheckedChangeListener((v, on) -> { if (second.isChecked() != on) second.setChecked(on); });
    second.setOnCheckedChangeListener((v, on) -> { if (first.isChecked() != on) first.setChecked(on); });
  }

  private BackdropLayout comparisonPanel() {
    BackdropLayout panel = new BackdropLayout(this);
    int color = (getResources().getConfiguration().uiMode & 48) == 32 ? 0xff121212 : Color.WHITE;
    panel.content().setBackgroundColor(color);
    android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
    shape.setColor(color);
    shape.setCornerRadius(dp(32));
    panel.setBackground(shape);
    panel.setClipToOutline(true);
    return panel;
  }

  private void sliders() {
    column.setPadding(dp(24), 0, dp(24), 0);
    LiquidSlider first = new LiquidSlider(this);
    LiquidSlider second = new LiquidSlider(this);
    LinearLayout.LayoutParams firstBounds = new LinearLayout.LayoutParams(-1, dp(24));
    firstBounds.setMargins(dp(8), 0, dp(8), 0);
    column.addView(first, firstBounds);
    BackdropLayout panel = comparisonPanel();
    FrameLayout.LayoutParams track = new FrameLayout.LayoutParams(-1, dp(28), Gravity.CENTER);
    track.setMargins(dp(56), 0, dp(56), 0);
    panel.foreground().addView(second, track);
    LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(-1, dp(76));
    bounds.topMargin = dp(40);
    bounds.bottomMargin = dp(24);
    column.addView(panel, bounds);
    first.setOnValueChangeListener(value -> { if (Math.abs(second.getValue() - value) > .0001f) second.setValue(value); });
    second.setOnValueChangeListener(value -> { if (Math.abs(first.getValue() - value) > .0001f) first.setValue(value); });
  }

  private void tabs() {
    for (int count : new int[] {3, 4}) {
      LiquidBottomTabs tabs = new LiquidBottomTabs(this);
      String[] labels = new String[count];
      for (int i = 0; i < count; i++) labels[i] = "Tab " + (i + 1);
      tabs.setTabs(labels);
      tabs.setTabIcon(R.drawable.ic_flight, (getResources().getConfiguration().uiMode & 48) == 32 ? Color.WHITE : Color.BLACK);
      LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(-1, dp(64));
      bounds.setMargins(dp(36), count == 3 ? 0 : dp(32), dp(36), 0);
      column.addView(tabs, bounds);
    }
  }

  private void dialogs() {
    boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
    View dim = new View(this);
    dim.setBackgroundColor(dark ? 0x8f121212 : 0x3b29293a);
    root.content().addView(dim, new FrameLayout.LayoutParams(-1, -1));
    GlassView dialog = new GlassView(this);
    dialog.style.radius = 48;
    dialog.style.plainHighlight = true; dialog.style.highlight = .38f;
    dialog.style.shadow = 0;
    dialog.style.blur = dark ? 8 : 16;
    dialog.style.refractionHeight = 24;
    dialog.style.refractionAmount = 48;
    dialog.style.saturation = 1.5f;
    dialog.style.brightness = dark ? 0 : .2f;
    dialog.style.tint = dark ? 0x66121212 : 0x99fafafa;
    LinearLayout inner = new LinearLayout(this);
    inner.setOrientation(LinearLayout.VERTICAL);
    inner.setPadding(dp(24), dp(24), dp(24), dp(24));
    TextView heading = text("Dialog Title", 24);
    heading.setTextColor(dark ? Color.WHITE : Color.BLACK);
    inner.addView(heading);
    TextView body = text(LOREM, 15);
    body.setMaxLines(5);
    body.setTextColor(dark ? 0xadffffff : 0xad000000);
    inner.addView(body);
    LinearLayout actions = new LinearLayout(this);
    for (String label : new String[] {"Cancel", "Okay"}) {
      TextView action = text(label, 16);
      action.setGravity(Gravity.CENTER);
      action.setTextColor(label.equals("Okay") || dark ? Color.WHITE : Color.BLACK);
      android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
      shape.setColor(label.equals("Okay") ? 0xff0088ff : 0x33fafafa);
      shape.setCornerRadius(dp(24));
      action.setBackground(shape);
      action.setOnClickListener(v -> {});
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
      if (label.equals("Okay")) params.leftMargin = dp(16);
      actions.addView(action, params);
    }
    inner.addView(actions);
    dialog.addView(inner);
    LinearLayout.LayoutParams dialogBounds = new LinearLayout.LayoutParams(-1, -2);
    dialogBounds.setMargins(dp(40), 0, dp(40), 0);
    column.addView(dialog, dialogBounds);
  }

  private void lock() {
    View dim = new View(this);
    dim.setBackgroundColor(0x4d000000);
    root.foreground().addView(dim, 0, new FrameLayout.LayoutParams(-1, -1));
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
    FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(-1, dp(120), Gravity.CENTER);
    size.setMargins(dp(48), 0, dp(48), 0);
    area.addView(clock, size);
    area.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
      int width = Math.min(dp(400), right - left - dp(96));
      int height = Math.round(width * texture.getHeight() / (float) texture.getWidth());
      FrameLayout.LayoutParams bounds = (FrameLayout.LayoutParams) clock.getLayoutParams();
      if (bounds.width != width || bounds.height != height) { bounds.width = width; bounds.height = height; clock.setLayoutParams(bounds); }
    });
    draggable(clock);
    add(area, 360);
    area.setTranslationY(-getResources().getDisplayMetrics().heightPixels / 4f);
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
    View dim = new View(this);
    dim.setBackgroundColor(0x66000000);
    root.content().addView(dim, new FrameLayout.LayoutParams(-1, -1));
    if (android.os.Build.VERSION.SDK_INT >= 31) root.content().setRenderEffect(
        android.graphics.RenderEffect.createBlurEffect(dp(4), dp(4), Shader.TileMode.CLAMP));
    FrameLayout grid = new FrameLayout(this);
    column.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    column.setOnApplyWindowInsetsListener((v, insets) -> {
      v.setPadding(0, dp(80) + insets.getSystemWindowInsetTop(), 0, 0); return insets;
    });
    column.requestApplyInsets();
    int[][] tiles = {{0,0,152,152},{168,0,152,152},{0,168,68,68},
        {84,168,68,68},{0,252,152,68},{168,168,68,152},{252,168,68,152},
        {0,336,152,152},{168,336,68,68},{252,336,68,68},{168,420,68,68}};
    for (int i = 0; i < tiles.length; i++) {
      int[] tile = tiles[i];
      GlassView panel = new GlassView(this);
      panel.style.radius = 34;
      panel.style.saturation = 1.5f;
      panel.style.blur = 0;
      panel.style.refractionHeight = 24;
      panel.style.refractionAmount = 48;
      panel.style.shadow = 0;
      panel.style.highlight = .5f;
      panel.style.highlightFalloff = 2;
      panel.style.tint = i == 4 ? 0 : 0x0d000000;
      FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(dp(tile[2]), dp(tile[3]));
      bounds.leftMargin = dp(tile[0]); bounds.topMargin = dp(tile[1]);
      grid.addView(panel, bounds);
      if (i == 0) {
        for (int n = 0; n < 3; n++) {
          ImageView icon = flightIcon();
          android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
          circle.setColor(n == 0 ? 0x33ffffff : 0xff0088ff);
          circle.setCornerRadius(dp(28));
          icon.setBackground(circle);
          icon.setPadding(dp(18.4f), dp(18.4f), dp(18.4f), dp(18.4f));
          FrameLayout.LayoutParams inner = new FrameLayout.LayoutParams(dp(56), dp(56));
          inner.leftMargin = dp(16 + (n == 1 ? 64 : 0));
          inner.topMargin = dp(16 + (n == 2 ? 64 : 0));
          panel.addView(icon, inner);
        }
      } else if (i == 2 || i == 3 || i >= 8) {
        panel.addView(flightIcon(), new FrameLayout.LayoutParams(-1, -1));
      }
    }
    column.addView(grid, new LinearLayout.LayoutParams(dp(320), dp(488)));
    android.hardware.SensorManager sensors = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
    android.hardware.Sensor accelerometer = sensors.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
    android.hardware.SensorEventListener gravity = new android.hardware.SensorEventListener() {
      private float angle = 45;
      public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
      public void onSensorChanged(android.hardware.SensorEvent event) {
        angle = .5f * angle + .5f * (float) Math.toDegrees(Math.atan2(event.values[1], event.values[0]));
        for (int i = 0; i < grid.getChildCount(); i++) {
          GlassView tile = (GlassView) grid.getChildAt(i);
          tile.style.highlightAngle = angle; tile.invalidate();
        }
      }
    };
    grid.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
      public void onViewAttachedToWindow(View v) { if (accelerometer != null) sensors.registerListener(gravity, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI); }
      public void onViewDetachedFromWindow(View v) { sensors.unregisterListener(gravity); }
    });
    if (grid.isAttachedToWindow() && accelerometer != null) sensors.registerListener(gravity, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI);
    final float[] gesture = {0, 1, 1};
    final android.animation.ValueAnimator[] settling = {null};
    java.util.function.Consumer<Float> update = value -> {
      float progress = Math.max(0, Math.min(1, value));
      gesture[1] = value;
      grid.setAlpha(progress);
      grid.setTranslationY(-dp(48) * (1 - value));
      grid.setScaleX(1 / (1 + .1f * Math.max(0, value - 1)));
      grid.setScaleY(1 + .1f * Math.max(0, value - 1));
      dim.setAlpha(progress);
      for (int i = 0; i < grid.getChildCount(); i++) {
        GlassView tile = (GlassView) grid.getChildAt(i);
        tile.style.refractionHeight = 24 * progress;
        tile.style.refractionAmount = 48 * progress;
        tile.invalidate();
      }
      if (android.os.Build.VERSION.SDK_INT >= 31) root.content().setRenderEffect(progress > 0
          ? android.graphics.RenderEffect.createBlurEffect(dp(4) * progress, dp(4) * progress, Shader.TileMode.CLAMP) : null);
    };
    root.foreground().setOnTouchListener((v, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        if (settling[0] != null) settling[0].cancel();
        gesture[0] = event.getRawY(); gesture[2] = gesture[1];
        return true;
      }
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
        update.accept(gesture[2] + (event.getRawY() - gesture[0]) / 1000f);
        return true;
      }
      if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
        float delta = event.getRawY() - gesture[0];
        float target = Math.abs(delta) > dp(8) ? (delta > 0 ? 1 : 0) : (gesture[1] >= .5f ? 1 : 0);
        settling[0] = android.animation.ValueAnimator.ofFloat(gesture[1], target);
        settling[0].setDuration(400);
        settling[0].setInterpolator(new android.view.animation.OvershootInterpolator(target == 1 ? .5f : 0));
        settling[0].addUpdateListener(animation -> update.accept((Float) animation.getAnimatedValue()));
        settling[0].start();
        return true;
      }
      return true;
    });
  }

  private ImageView flightIcon() {
    ImageView icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_flight);
    icon.setScaleType(ImageView.ScaleType.CENTER);
    return icon;
  }

  private void magnifier() {
    boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
    BackdropLayout textSource = new BackdropLayout(this);
    TextView body = text(LOREM, 16);
    body.setTextColor(dark ? Color.WHITE : Color.BLACK);
    body.setPadding(dp(24), dp(24), dp(24), dp(24));
    android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
    shape.setColor(dark ? 0xe6121212 : 0xe6ffffff);
    shape.setCornerRadius(dp(32));
    body.setBackground(shape);
    textSource.content().addView(body, new FrameLayout.LayoutParams(-1, -1));
    View cursor = new View(this);
    cursor.setBackgroundColor(0xff0088ff);
    textSource.content().addView(cursor, new FrameLayout.LayoutParams(dp(4), dp(24), Gravity.CENTER));
    GlassView lens = new GlassView(this);
    lens.style.zoom = 1.5f;
    lens.style.sampleOffsetY = 80;
    lens.style.shadow = 0;
    lens.style.blur = 0;
    lens.style.radius = 48;
    lens.style.refractionHeight = 8;
    lens.style.refractionAmount = 24;
    lens.style.innerShadow = 16;
    lens.style.tint = 0;
    FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(dp(128), dp(96), Gravity.CENTER);
    textSource.foreground().addView(lens, pos);
    lens.setTranslationY(-dp(80));
    final float[] down = new float[4];
    android.view.View.OnTouchListener drag = (v, e) -> {
      if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
        down[0] = e.getRawX(); down[1] = e.getRawY();
        down[2] = cursor.getTranslationX(); down[3] = cursor.getTranslationY();
        v.getParent().requestDisallowInterceptTouchEvent(true);
      } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
        float x = down[2] + e.getRawX() - down[0];
        float y = down[3] + e.getRawY() - down[1];
        cursor.setTranslationX(x); cursor.setTranslationY(y);
        lens.setTranslationX(x); lens.setTranslationY(y - dp(80));
        lens.invalidate();
      } else {
        v.getParent().requestDisallowInterceptTouchEvent(false);
        if (e.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
      }
      return true;
    };
    cursor.setOnTouchListener(drag);
    lens.setOnTouchListener(drag);
    body.measure(View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels - dp(48), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    LinearLayout.LayoutParams textBounds = new LinearLayout.LayoutParams(-1, body.getMeasuredHeight());
    textBounds.setMargins(dp(24), 0, dp(24), 0);
    column.addView(textSource, textBounds);
  }

  private void playground() {
    root.foreground().removeAllViews();
    GlassView sample = new GlassView(this);
    sample.style.radius = 64;
    sample.style.saturation = 1.5f;
    sample.style.plainHighlight = true; sample.style.highlight = .38f;
    sample.style.blur = 0;
    sample.style.refractionHeight = 25.6f;
    sample.style.refractionAmount = 51.2f;
    sample.style.chromaticAberration = false;
    sample.style.tint = 0;
    sample.style.shadow = 0;
    GlassGestureController.attach(sample);
    FrameLayout.LayoutParams sampleBounds = new FrameLayout.LayoutParams(dp(256), dp(256), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    sampleBounds.topMargin = dp(48);
    sample.setOnApplyWindowInsetsListener((v, insets) -> {
      FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
      p.topMargin = dp(48) + insets.getSystemWindowInsetTop(); v.setLayoutParams(p); return insets;
    });
    root.foreground().addView(sample, sampleBounds);
    GlassView sheet = new GlassView(this);
    sheet.style.plainHighlight = true; sheet.style.highlight = .38f;
    sheet.style.saturation = 1.5f;
    sheet.style.radius = 32; sheet.style.blur = 4;
    sheet.style.refractionHeight = 16; sheet.style.refractionAmount = 32;
    sheet.style.tint = 0x80ffffff; sheet.style.shadow = 0;
    LinearLayout settings = new LinearLayout(this);
    settings.setOrientation(LinearLayout.VERTICAL);
    settings.setPadding(dp(24), dp(24), dp(24), dp(24));
    String[] names = {"Corner radius", "Blur radius", "Refraction height", "Refraction amount", "Chromatic aberration"};
    float[] defaults = {.5f, 0, .2f, .2f, 0};
    LiquidSlider[] sliders = new LiquidSlider[5];
    for (int i = 0; i < names.length; i++) {
      final int setting = i;
      TextView label = text(names[i], 14); label.setTextColor(Color.BLACK); label.setIncludeFontPadding(false);
      label.setPadding(0, 0, 0, dp(8));
      settings.addView(label);
      LiquidSlider slider = new LiquidSlider(this); sliders[i] = slider;
      slider.setValue(defaults[i]);
      slider.setOnValueChangeListener(value -> {
        switch (setting) {
          case 0: sample.style.radius = 128 * value; break;
          case 1: sample.style.blur = 32 * value; break;
          case 2: sample.style.refractionHeight = 128 * value; break;
          case 3: sample.style.refractionAmount = 256 * value; break;
          case 4: sample.style.chromaticAberration = value > 0; break;
        }
        sample.invalidate();
      });
      LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(-1, dp(24));
      bounds.bottomMargin = i < 4 ? dp(16) : 0;
      settings.addView(slider, bounds);
    }
    sheet.addView(settings);
    FrameLayout.LayoutParams sheetBounds = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
    sheetBounds.setMargins(dp(16), 0, dp(16), dp(88));
    sheet.setOnApplyWindowInsetsListener((v, insets) -> {
      FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
      p.bottomMargin = dp(88) + insets.getSystemWindowInsetBottom(); v.setLayoutParams(p); return insets;
    });
    root.foreground().addView(sheet, sheetBounds);
    LiquidButton expand = button("▼", () -> sheet.setVisibility(sheet.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    LiquidButton reset = button("Reset", () -> {
      sample.animate().translationX(0).translationY(0).scaleX(1).scaleY(1).rotation(0).setDuration(300).start();
      for (int i = 0; i < sliders.length; i++) sliders[i].setValue(defaults[i]);
    });
    for (LiquidButton action : new LiquidButton[] {expand, reset}) {
      action.style.tint = 0xbfff8d28; action.style.hueTint = true; action.style.shadow = 0;
      action.setTextSize(15);
      FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(dp(action == reset ? 68 : 48), dp(48), Gravity.BOTTOM | (action == reset ? Gravity.RIGHT : Gravity.LEFT));
      bounds.setMargins(dp(20), 0, dp(20), dp(20));
      action.setOnApplyWindowInsetsListener((v, insets) -> {
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
        p.bottomMargin = dp(20) + insets.getSystemWindowInsetBottom(); v.setLayoutParams(p); return insets;
      });
      root.foreground().addView(action, bounds);
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
    LiquidButton sample = new LiquidButton(this) {
      @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setText(String.format(java.util.Locale.ROOT, "luminance:\n%.2f", getSampledLuminance()));
      }
    };
    sample.setTextSize(16);
    sample.style.adaptiveLuminance = true;
    sample.style.plainHighlight = true; sample.style.highlight = .38f;
    sample.style.shadow = 0;
    sample.style.radius = 24;
    sample.style.refractionHeight = 24;
    sample.style.refractionAmount = 80;
    sample.style.saturation = 1.5f;
    sample.style.tint = 0;
    GlassGestureController.attach(sample);
    column.addView(sample, new LinearLayout.LayoutParams(dp(160), dp(160)));
  }

  private void progressive() {
    LiquidButton original = button("alpha-masked progressive blur", () -> {});
    original.style.radius = 0;
    original.style.blur = 4;
    original.style.refractionAmount = 0;
    original.style.alphaMaskedBlur = true;
    original.style.highlight = 0;
    original.style.shadow = 0;
    original.style.tint = 0;
    original.setTextColor((getResources().getConfiguration().uiMode & 48) == 32 ? Color.WHITE : Color.BLACK);
    column.setPadding(0, 0, 0, 0);
    column.addView(original, new LinearLayout.LayoutParams(-1, dp(128)));
  }

  private GlassView scrollTile() {
    GlassView tile = new GlassView(this);
    tile.style.radius = 32;
    tile.style.saturation = 1.5f;
    tile.style.blur = 0;
    tile.style.refractionHeight = 16;
    tile.style.refractionAmount = 32;
    tile.style.tint = 0;
    return tile;
  }

  private void scrolling(boolean lazy) {
    root.foreground().removeAllViews();
    if (lazy) {
      ListView list = new ListView(this);
      list.setDivider(null);
      list.setPadding(dp(16), dp(16), dp(16), dp(16));
      list.setClipToPadding(false);
      list.setClipChildren(false);
      list.setAdapter(new BaseAdapter() {
        public int getCount() { return 20; }
        public Object getItem(int position) { return position; }
        public long getItemId(int position) { return position; }
        public View getView(int position, View recycled, ViewGroup parent) {
          if (recycled != null) return recycled;
          FrameLayout row = new FrameLayout(MainActivity.this);
          // A row is a layout boundary, not a shadow clipping boundary.
          row.setClipChildren(false);
          row.setClipToPadding(false);
          row.setLayoutParams(new AbsListView.LayoutParams(-1, dp(176)));
          row.addView(scrollTile(), new FrameLayout.LayoutParams(-1, dp(160)));
          return row;
        }
      });
      root.foreground().addView(list);
    } else {
      ScrollView scroll = new ScrollView(this);
      scroll.setClipChildren(false);
      scroll.setClipToPadding(false);
      LinearLayout items = new LinearLayout(this);
      items.setOrientation(LinearLayout.VERTICAL);
      items.setClipChildren(false);
      items.setClipToPadding(false);
      items.setPadding(dp(16), dp(16), dp(16), 0);
      for (int i = 0; i < 20; i++) {
        LinearLayout.LayoutParams bounds = new LinearLayout.LayoutParams(-1, dp(160));
        bounds.bottomMargin = dp(16);
        items.addView(scrollTile(), bounds);
      }
      scroll.addView(items);
      root.foreground().addView(scroll);
    }
  }
}
