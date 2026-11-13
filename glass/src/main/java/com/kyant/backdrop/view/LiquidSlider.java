package com.kyant.backdrop.view;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class LiquidSlider extends GlassView {
  private float value = .5f;
  private final Paint p = new Paint(3);
  private final GlassView thumb;

  public interface OnValueChangeListener {
    void onValueChanged(float value);
  }

  private OnValueChangeListener listener;

  public LiquidSlider(Context c) {
    super(c);
    setFocusable(true);
    setClickable(true);
    style.radius = 100;
    style.shadow = 0;
    style.highlight = 0;
    setClipChildren(false);
    setClipToPadding(false);
    setContentDescription("Value");
    thumb = new GlassView(c);
    thumb.style.radius = 100;
    thumb.style.blur = 8;
    thumb.style.refractionHeight = 0;
    thumb.style.refractionAmount = 0;
    thumb.style.tint = Color.WHITE;
    thumb.style.highlight = 0;
    thumb.style.shadow = 4;
    if (android.os.Build.VERSION.SDK_INT >= 28) {
      thumb.setOutlineAmbientShadowColor(0x0d000000);
      thumb.setOutlineSpotShadowColor(0x0d000000);
    }
    thumb.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    thumb.setBackdropLayers(this);
    addView(thumb);
  }

  public void setOnValueChangeListener(OnValueChangeListener l) {
    listener = l;
  }

  public float getValue() {
    return value;
  }

  public void setValue(float v) {
    float next = Math.max(0, Math.min(1, v));
    if (next == value) return;
    value = next;
    positionThumb();
    invalidate();
    if (listener != null) listener.onValueChanged(value);
  }

  @Override
  protected void onDraw(Canvas c) {
    float center = getHeight() / 2f;
    boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
    p.setColor(dark ? 0x5c787880 : 0x33787878);
    c.drawRoundRect(0, center - dp(3), getWidth(), center + dp(3), dp(3), dp(3), p);
    p.setColor(dark ? 0xff0091ff : 0xff0088ff);
    float x = getWidth() * value;
    if (getLayoutDirection() == LAYOUT_DIRECTION_RTL)
      c.drawRoundRect(getWidth() - x, center - dp(3), getWidth(), center + dp(3), dp(3), dp(3), p);
    else c.drawRoundRect(0, center - dp(3), x, center + dp(3), dp(3), dp(3), p);
  }

  private void positionThumb() {
    float progress = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - value : value;
    thumb.setTranslationX(Math.max(-dp(10), Math.min(getWidth() - dp(30), getWidth() * progress - dp(20))));
    thumb.invalidate();
  }

  @Override
  protected void onMeasure(int w, int h) {
    super.onMeasure(w, h);
    int size = (int) dp(40);
    thumb.measure(
        MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec((int) dp(24), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    int inset = 0;
    thumb.layout(0, (getHeight() - thumb.getMeasuredHeight()) / 2, thumb.getMeasuredWidth(), (getHeight() + thumb.getMeasuredHeight()) / 2);
    positionThumb();
  }

  private android.animation.ValueAnimator pressAnimation;
  private float pressProgress;
  private void pressThumb(boolean pressed) {
    if (pressAnimation != null) pressAnimation.cancel();
    pressAnimation = android.animation.ValueAnimator.ofFloat(pressProgress, pressed ? 1 : 0);
    pressAnimation.setDuration(500);
    pressAnimation.setInterpolator(new SpringInterpolator(1, 1000, .5f));
    pressAnimation.addUpdateListener(animation -> {
      pressProgress = (float) animation.getAnimatedValue();
      thumb.style.blur = 8 * (1 - pressProgress);
      thumb.style.refractionHeight = 10 * pressProgress;
      thumb.style.refractionAmount = 14 * pressProgress;
      thumb.style.highlight = pressProgress;
      thumb.style.tint = Color.argb(Math.round(255 * (1 - pressProgress)), 255, 255, 255);
      thumb.invalidate();
    });
    pressAnimation.start();
    thumb.animate().scaleX(pressed ? 1.5f : 1).scaleY(pressed ? 1.5f : 1).setDuration(700)
        .setInterpolator(new SpringInterpolator(.65f, 250, .7f)).start();
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) pressThumb(true);
    if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) pressThumb(false);
    if (!isEnabled()) return false;
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
      getParent().requestDisallowInterceptTouchEvent(true);
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN
        || e.getActionMasked() == MotionEvent.ACTION_MOVE
        || e.getActionMasked() == MotionEvent.ACTION_UP) {
      float v = e.getX() / Math.max(1, getWidth());
      setValue(getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - v : v);
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP) {
      getParent().requestDisallowInterceptTouchEvent(false);
      performClick();
    }
    if (e.getActionMasked() == MotionEvent.ACTION_CANCEL)
      getParent().requestDisallowInterceptTouchEvent(false);
    return true;
  }

  @Override
  protected void onDetachedFromWindow() {
    if (pressAnimation != null) pressAnimation.cancel();
    super.onDetachedFromWindow();
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.setClassName("android.widget.SeekBar");
    info.setRangeInfo(
        AccessibilityNodeInfo.RangeInfo.obtain(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0, 1, value));
    if (android.os.Build.VERSION.SDK_INT >= 24)
      info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
  }

  @Override
  public boolean performAccessibilityAction(int action, android.os.Bundle args) {
    if (android.os.Build.VERSION.SDK_INT >= 24
        && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
        && args != null) {
      setValue(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));
      return true;
    }
    if (action == 4096 || action == 8192) {
      setValue(value + (action == 4096 ? .05f : -.05f));
      return true;
    }
    return super.performAccessibilityAction(action, args);
  }

  @Override
  public boolean onKeyDown(int key, android.view.KeyEvent e) {
    if (key == 21 || key == 22) {
      setValue(value + (key == 22 ? .05f : -.05f));
      return true;
    }
    return super.onKeyDown(key, e);
  }

  @Override
  protected android.os.Parcelable onSaveInstanceState() {
    android.os.Bundle b = new android.os.Bundle();
    b.putParcelable("parent", super.onSaveInstanceState());
    b.putFloat("value", value);
    return b;
  }

  @Override
  protected void onRestoreInstanceState(android.os.Parcelable state) {
    if (state instanceof android.os.Bundle) {
      android.os.Bundle b = (android.os.Bundle) state;
      super.onRestoreInstanceState(b.getParcelable("parent"));
      value = b.getFloat("value");
    } else super.onRestoreInstanceState(state);
  }
}
