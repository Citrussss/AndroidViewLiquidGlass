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
    style.shadow = 0;
    style.highlight = 0;
    setClipChildren(false);
    setClipToPadding(false);
    setContentDescription("Toggle");
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
    animator.setDuration(500);
    animator.setInterpolator(new SpringInterpolator(1, 1000, .5f));
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
    boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
    int off = dark ? 0x5c787880 : 0x33787878;
    int on = dark ? 0xff30d158 : 0xff34c759;
    p.setColor((Integer) new android.animation.ArgbEvaluator().evaluate(progress, off, on));
    c.drawRoundRect(0, 0, getWidth(), getHeight(), getHeight() / 2f, getHeight() / 2f, p);
  }

  private void positionThumb() {
    thumb.setTranslationX(
        Math.max(0, getWidth() - dp(44))
            * (getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1 - progress : progress));
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
    int inset = (int) dp(2);
    thumb.layout(inset, inset, inset + thumb.getMeasuredWidth(), inset + thumb.getMeasuredHeight());
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
      thumb.style.refractionHeight = 5 * pressProgress;
      thumb.style.refractionAmount = 10 * pressProgress;
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
    if (pressAnimation != null) pressAnimation.cancel();
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
