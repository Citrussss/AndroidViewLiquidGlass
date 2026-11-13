package com.kyant.backdrop.view;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LiquidBottomTabs extends GlassView {
  private final LinearLayout row;
  private final GlassView selector;
  private final GlassView sampledTrack;
  private float panelScale = 1;
  private int selected;
  private int contentColor = android.graphics.Color.BLACK;

  public interface OnTabSelectedListener {
    void onTabSelected(int index);
  }

  private OnTabSelectedListener listener;

  public LiquidBottomTabs(Context c) {
    super(c);
    style.blur = 8; style.saturation = 1.5f; style.radius = 100; style.shadow = 0;
    style.refractionHeight = 24; style.refractionAmount = 24; style.tint = 0x66fafafa;
    style.depthEffect = false; style.chromaticAberration = false;
    selector = new GlassView(c);
    selector.style.depthEffect = false;
    selector.style.radius = 100;
    selector.style.blur = 0;
    selector.style.refractionHeight = 0;
    selector.style.refractionAmount = 0;
    selector.style.tint = 0x1a000000;
    selector.style.highlight = 0;
    selector.style.shadow = 0;
    selector.setBackdropLayers(this);
    selector.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    addView(selector);
    row = new LinearLayout(c);
    LayoutParams rowBounds = new LayoutParams(-1, -1);
    rowBounds.setMargins((int) dp(4), (int) dp(4), (int) dp(4), (int) dp(4));
    addView(row, rowBounds);
    sampledTrack = new GlassView(c) {
      @Override protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        drawAccentContent(canvas);
      }
    };
    sampledTrack.style.radius = 100;
    sampledTrack.style.blur = 8;
    sampledTrack.style.saturation = 1.5f;
    sampledTrack.style.tint = style.tint;
    sampledTrack.style.depthEffect = false;
    sampledTrack.style.chromaticAberration = false;
    sampledTrack.style.shadow = 0;
    sampledTrack.style.refractionHeight = 0;
    sampledTrack.style.refractionAmount = 0;
    sampledTrack.style.highlight = 0;
    sampledTrack.setVisibility(INVISIBLE);
    sampledTrack.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    addView(sampledTrack, new LayoutParams(-1, (int) dp(56), Gravity.CENTER_VERTICAL));
    bringChildToFront(selector);
    setClipToOutline(false);
    setClipChildren(false);
    setClipToPadding(false);
  }

  public void setOnTabSelectedListener(OnTabSelectedListener l) {
    listener = l;
  }

  public void setTabs(String... names) {
    row.removeAllViews();
    for (int i = 0; i < names.length; i++) {
      final int index = i;
      TextView t = new TextView(getContext());
      t.setText(names[i]);
      t.setTextSize(15);
      t.setTextColor(-1);
      t.setGravity(Gravity.CENTER);
      t.setFocusable(true);
      t.setOnClickListener(v -> setSelectedTab(index));
      row.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
    }
    setSelectedTab(Math.min(selected, names.length - 1));
  }

  public void setTabIcon(int drawable, int color) {
    contentColor = color;
    for (int i = 0; i < row.getChildCount(); i++) {
      TextView label = (TextView) row.getChildAt(i);
      android.graphics.drawable.Drawable icon = getContext().getDrawable(drawable).mutate();
      icon.setTint(color);
      icon.setBounds(0, 0, (int) dp(24), (int) dp(24));
      label.setCompoundDrawablePadding((int) dp(6));
      label.setCompoundDrawables(null, icon, null, null);
      label.setTextSize(12);
      label.setTextColor(color);
      icon.setTint(color);
      label.setPadding(0, (int) dp(6), 0, (int) dp(6));
      label.setIncludeFontPadding(false);
    }
  }

  public int getSelectedTab() {
    return selected;
  }

  public void setSelectedTab(int index) {
    if (index < 0 || index >= row.getChildCount()) return;
    selected = index;
    for (int i = 0; i < row.getChildCount(); i++) {
      android.view.View t = row.getChildAt(i);
      t.setSelected(i == index);
      TextView label = (TextView) t;
      int color = contentColor;
      label.setTextColor(color);
      for (android.graphics.drawable.Drawable icon : label.getCompoundDrawables()) if (icon != null) icon.setTint(color);
    }
    positionSelector(true);
    if (listener != null) listener.onTabSelected(index);
  }

  private void positionSelector(boolean animate) {
    if (row.getChildCount() == 0) return;
    int visual =
        getLayoutDirection() == LAYOUT_DIRECTION_RTL
            ? row.getChildCount() - 1 - selected
            : selected;
    float x = dp(4) + (getWidth() - dp(8)) / row.getChildCount() * visual;
    if (animate)
      selector
          .animate()
          .translationX(x)
          .setDuration(250)
          .setInterpolator(new android.view.animation.OvershootInterpolator(.6f))
          .start();
    else selector.setTranslationX(x);
    selector.invalidate();
  }

  @Override
  protected void drawExportedBackdrop(android.graphics.Canvas canvas) {
    int save = canvas.save();
    canvas.translate(sampledTrack.getLeft(), sampledTrack.getTop());
    sampledTrack.draw(canvas);
    canvas.restoreToCount(save);
  }

  @Override protected void onDraw(android.graphics.Canvas canvas) {
    int save = canvas.save();
    canvas.scale(panelScale, panelScale, getWidth() / 2f, getHeight() / 2f);
    super.onDraw(canvas);
    canvas.restoreToCount(save);
  }

  private void drawAccentContent(android.graphics.Canvas canvas) {
    // The source catalog exports a second, accent-colored content row for the lens.
    android.graphics.Paint tint = new android.graphics.Paint();
    tint.setColorFilter(new android.graphics.PorterDuffColorFilter(0xff0088ff, android.graphics.PorterDuff.Mode.SRC_IN));
    int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), tint);
    canvas.translate(row.getLeft(), 0);
    for (int i = 0; i < row.getChildCount(); i++) {
      android.view.View item = row.getChildAt(i);
      int itemSave = canvas.save();
      canvas.translate(item.getLeft(), item.getTop());
      float scale = 1 + .2f * pressProgress;
      canvas.scale(scale, scale, item.getWidth() / 2f, item.getHeight() / 2f);
      item.draw(canvas);
      canvas.restoreToCount(itemSave);
    }
    canvas.restoreToCount(save);
  }

  private float pressProgress;
  private android.animation.ValueAnimator pressAnimator;
  private void setLiquidPress(boolean pressed) {
    if (pressAnimator != null) pressAnimator.cancel();
    pressAnimator = android.animation.ValueAnimator.ofFloat(pressProgress, pressed ? 1 : 0);
    pressAnimator.setDuration(500);
    pressAnimator.setInterpolator(new SpringInterpolator(1, 1000, .5f));
    pressAnimator.addUpdateListener(animation -> {
      pressProgress = (float) animation.getAnimatedValue();
      sampledTrack.style.refractionHeight = 24 * pressProgress;
      sampledTrack.style.refractionAmount = 24 * pressProgress;
      sampledTrack.style.highlight = .5f * pressProgress;
      sampledTrack.invalidate();
      panelScale = 1 + dp(16) / Math.max(1, getWidth()) * pressProgress;
      row.setScaleX(panelScale); row.setScaleY(panelScale);
      invalidate();
      selector.style.refractionHeight = 10 * pressProgress;
      selector.style.refractionAmount = 14 * pressProgress;
      selector.style.highlight = .5f * pressProgress;
      selector.style.shadow = 24 * pressProgress;
      selector.style.innerShadow = 8 * pressProgress;
      boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
      int alpha = Math.round(255 * (.1f * (1 - pressProgress) + .03f * pressProgress));
      selector.style.tint = android.graphics.Color.argb(alpha, dark ? 255 : 0, dark ? 255 : 0, dark ? 255 : 0);
      selector.invalidate();
    });
    pressAnimator.start();
    float scale = pressed ? 78f / 56f : 1;
    selector.animate().scaleX(scale).scaleY(scale).setDuration(700)
        .setInterpolator(new SpringInterpolator(.65f, 250, .7f)).start();

  }

  @Override protected void onDetachedFromWindow() {
    if (pressAnimator != null) pressAnimator.cancel();
    super.onDetachedFromWindow();
  }

  private float dragDownX, dragDownY, dragStartX;
  private boolean dragging;

  @Override
  public boolean onInterceptTouchEvent(android.view.MotionEvent event) {
    if (!isEnabled() || row.getChildCount() == 0) return false;
    switch (event.getActionMasked()) {
      case android.view.MotionEvent.ACTION_DOWN:
        dragging = false;
        dragDownX = event.getX(); dragDownY = event.getY();
        selector.animate().cancel();
        dragStartX = selector.getTranslationX();
        setLiquidPress(true);
        break;
      case android.view.MotionEvent.ACTION_MOVE:
        float dx = event.getX() - dragDownX, dy = event.getY() - dragDownY;
        if (Math.abs(dx) > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()
            && Math.abs(dx) > Math.abs(dy)) {
          dragging = true;
          getParent().requestDisallowInterceptTouchEvent(true);
          return true;
        }
        break;
      case android.view.MotionEvent.ACTION_CANCEL:
      case android.view.MotionEvent.ACTION_UP:
        dragging = false;
        setLiquidPress(false);
        break;
    }
    return dragging;
  }

  @Override
  public boolean onTouchEvent(android.view.MotionEvent event) {
    if (!isEnabled() || row.getChildCount() == 0) return false;
    float tabWidth = (getWidth() - dp(8)) / row.getChildCount();
    switch (event.getActionMasked()) {
      case android.view.MotionEvent.ACTION_MOVE:
        if (!dragging) return super.onTouchEvent(event);
        float x = Math.max(dp(4), Math.min(getWidth() - dp(4) - tabWidth, dragStartX + event.getX() - dragDownX));
        selector.setTranslationX(x);
        selector.invalidate();
        return true;
      case android.view.MotionEvent.ACTION_UP:
        if (!dragging) return super.onTouchEvent(event);
        int visual = Math.round((selector.getTranslationX() - dp(4)) / Math.max(1, tabWidth));
        int index = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? row.getChildCount() - 1 - visual : visual;
        dragging = false;
        setLiquidPress(false);
        getParent().requestDisallowInterceptTouchEvent(false);
        setSelectedTab(index);
        return true;
      case android.view.MotionEvent.ACTION_CANCEL:
        if (!dragging) return super.onTouchEvent(event);
        dragging = false;
        setLiquidPress(false);
        getParent().requestDisallowInterceptTouchEvent(false);
        positionSelector(true);
        return true;
    }
    return dragging || super.onTouchEvent(event);
  }

  @Override
  protected void onMeasure(int w, int h) {
    super.onMeasure(w, h);
    int width = Math.max(0, getMeasuredWidth() - (int) dp(8)) / Math.max(1, row.getChildCount());
    selector.measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(Math.max(0, getMeasuredHeight() - (int) dp(8)), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    selector.layout(0, (int) dp(4), selector.getMeasuredWidth(), getHeight() - (int) dp(4));
    if (!dragging) positionSelector(false);
  }

  @Override
  protected android.os.Parcelable onSaveInstanceState() {
    android.os.Bundle state = new android.os.Bundle();
    state.putParcelable("parent", super.onSaveInstanceState());
    state.putInt("selected", selected);
    return state;
  }

  @Override
  protected void onRestoreInstanceState(android.os.Parcelable state) {
    if (state instanceof android.os.Bundle) {
      android.os.Bundle b = (android.os.Bundle) state;
      super.onRestoreInstanceState(b.getParcelable("parent"));
      setSelectedTab(b.getInt("selected"));
    } else super.onRestoreInstanceState(state);
  }
}
