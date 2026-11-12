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
    setContentDescription("Value");
    thumb = new GlassView(c);
    thumb.style.radius = 100;
    thumb.style.blur = 0;
    thumb.style.refractionHeight = 8;
    thumb.style.refractionAmount = 12;
    thumb.style.tint = 0xaaffffff;
    thumb.style.shadow = 2;
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
    super.onDraw(c);
    float h = getHeight(),
        x =
            h / 2
                + (getWidth() - h)
                    * (getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - value : value);
    p.setColor(0x66ffffff);
    c.drawRoundRect(dp(8), h / 2 - dp(3), x, h / 2 + dp(3), dp(3), dp(3), p);
  }

  private void positionThumb() {
    thumb.setTranslationX(
        (getWidth() - getHeight())
            * (getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - value : value));
    thumb.invalidate();
  }

  @Override
  protected void onMeasure(int w, int h) {
    super.onMeasure(w, h);
    int size = Math.max(0, getMeasuredHeight() - (int) dp(8));
    thumb.measure(
        MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    int inset = (int) dp(4);
    thumb.layout(inset, inset, inset + thumb.getMeasuredWidth(), inset + thumb.getMeasuredHeight());
    positionThumb();
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!isEnabled()) return false;
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
      getParent().requestDisallowInterceptTouchEvent(true);
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN
        || e.getActionMasked() == MotionEvent.ACTION_MOVE
        || e.getActionMasked() == MotionEvent.ACTION_UP) {
      float v = (e.getX() - getHeight() / 2f) / Math.max(1, getWidth() - getHeight());
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
