package com.kyant.backdrop.view;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;

public class LiquidToggle extends GlassView implements Checkable {
  private boolean checked;
  private float progress;
  private final Paint p = new Paint(3);
  private android.animation.ValueAnimator animator;
  private final GlassView thumb;
  private float downX;
  private boolean dragging;

  public interface OnCheckedChangeListener {
    void onCheckedChanged(LiquidToggle view, boolean checked);
  }

  private OnCheckedChangeListener listener;

  public LiquidToggle(Context c) {
    super(c);
    setClickable(true);
    setFocusable(true);
    style.radius = 100;
    setContentDescription("Toggle");
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

  public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
    listener = l;
  }

  @Override
  public boolean isChecked() {
    return checked;
  }

  @Override
  public void toggle() {
    setChecked(!checked);
  }

  @Override
  public void setChecked(boolean value) {
    if (value == checked) return;
    checked = value;
    if (animator != null) animator.cancel();
    animator = android.animation.ValueAnimator.ofFloat(progress, value ? 1 : 0);
    animator.setDuration(250);
    animator.addUpdateListener(
        a -> {
          progress = (float) a.getAnimatedValue();
          positionThumb();
          invalidate();
        });
    animator.start();
    if (listener != null) listener.onCheckedChanged(this, value);
    sendAccessibilityEvent(
        android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
  }

  @Override
  public boolean performClick() {
    toggle();
    super.performClick();
    return true;
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    p.setColor(Color.argb((int) (progress * 160), 40, 220, 140));
    c.drawRoundRect(0, 0, getWidth(), getHeight(), getHeight() / 2f, getHeight() / 2f, p);
  }

  private void positionThumb() {
    thumb.setTranslationX(
        (getWidth() - getHeight())
            * (getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - progress : progress));
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
    switch (e.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        downX = e.getX();
        dragging = false;
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      case MotionEvent.ACTION_MOVE:
        if (Math.abs(e.getX() - downX)
            > android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
          dragging = true;
          if (animator != null) animator.cancel();
          float v = (e.getX() - getHeight() / 2f) / Math.max(1, getWidth() - getHeight());
          progress =
              Math.max(0, Math.min(1, getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - v : v));
          positionThumb();
          invalidate();
        }
        return true;
      case MotionEvent.ACTION_UP:
        getParent().requestDisallowInterceptTouchEvent(false);
        if (dragging) {
          boolean target = progress >= .5f;
          if (target == checked) {
            checked = !target;
            setChecked(target);
          } else setChecked(target);
        } else performClick();
        return true;
      case MotionEvent.ACTION_CANCEL:
        getParent().requestDisallowInterceptTouchEvent(false);
        progress = checked ? 1 : 0;
        positionThumb();
        invalidate();
        return true;
    }
    return true;
  }

  @Override
  protected void onDetachedFromWindow() {
    if (animator != null) animator.cancel();
    super.onDetachedFromWindow();
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    info.setClassName("android.widget.Switch");
    info.setCheckable(true);
    info.setChecked(checked);
  }

  @Override
  protected android.os.Parcelable onSaveInstanceState() {
    android.os.Bundle b = new android.os.Bundle();
    b.putParcelable("parent", super.onSaveInstanceState());
    b.putBoolean("checked", checked);
    return b;
  }

  @Override
  protected void onRestoreInstanceState(android.os.Parcelable state) {
    if (state instanceof android.os.Bundle) {
      android.os.Bundle b = (android.os.Bundle) state;
      super.onRestoreInstanceState(b.getParcelable("parent"));
      checked = b.getBoolean("checked");
      progress = checked ? 1 : 0;
    } else super.onRestoreInstanceState(state);
  }
}
