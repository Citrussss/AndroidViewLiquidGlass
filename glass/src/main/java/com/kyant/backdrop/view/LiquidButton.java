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

  public void setTextColor(int color) {
    if (label.getCurrentTextColor() != color) label.setTextColor(color);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
      animate().scaleX(.94f).scaleY(.94f).setDuration(100).start();
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL)
      animate()
          .scaleX(1)
          .scaleY(1)
          .setDuration(250)
          .setInterpolator(new android.view.animation.OvershootInterpolator())
          .start();
    return super.onTouchEvent(e);
  }

  @Override
  public CharSequence getAccessibilityClassName() {
    return "android.widget.Button";
  }
}
