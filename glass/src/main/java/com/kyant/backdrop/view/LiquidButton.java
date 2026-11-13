package com.kyant.backdrop.view;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;

public class LiquidButton extends GlassView {
  @Override
  public boolean performClick() {
    return super.performClick();
  }

  protected final TextView label;

  public LiquidButton(Context c) {
    super(c);
    setClickable(true);
    setFocusable(true);
    label = new TextView(c);
    label.setGravity(Gravity.CENTER);
    label.setTextColor(Color.WHITE);
    label.setTextSize(17);
    label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    addView(label, new LayoutParams(-1, -1));
  }

  public void setText(CharSequence text) {
    label.setText(text);
    setContentDescription(text);
  }

  public void setTextSize(float sp) { label.setTextSize(sp); }

  public void setTextColor(int color) {
    if (label.getCurrentTextColor() != color) label.setTextColor(color);
  }

  private float pressX, pressY;

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!isEnabled()) return false;
    float height = Math.max(1, getHeight());
    float expanded = 1 + dp(4) / height;
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      pressX = e.getRawX(); pressY = e.getRawY();
      getParent().requestDisallowInterceptTouchEvent(true);
      animate().scaleX(expanded).scaleY(expanded).setDuration(150).start();
    } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
      float dx = e.getRawX() - pressX, dy = e.getRawY() - pressY;
      float maxOffset = Math.max(1, Math.min(getWidth(), getHeight()));
      setTranslationX(maxOffset * (float) Math.tanh(.05f * dx / maxOffset));
      setTranslationY(maxOffset * (float) Math.tanh(.05f * dy / maxOffset));
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      getParent().requestDisallowInterceptTouchEvent(false);
      animate().scaleX(1).scaleY(1).translationX(0).translationY(0).setDuration(300)
          .setInterpolator(new android.view.animation.OvershootInterpolator(.5f)).start();
    }
    return super.onTouchEvent(e);
  }

  @Override
  public CharSequence getAccessibilityClassName() {
    return "android.widget.Button";
  }
}
