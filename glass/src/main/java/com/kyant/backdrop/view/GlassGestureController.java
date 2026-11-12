package com.kyant.backdrop.view;

import android.view.MotionEvent;
import android.view.View;

/** Native pan, pinch-to-zoom and two-finger rotation for experimental glass surfaces. */
public final class GlassGestureController implements View.OnTouchListener {
  private float x, y, distance, angle, scale, rotation, translationX, translationY;
  private int pointers;

  public static void attach(View view) {
    view.setOnTouchListener(new GlassGestureController());
    view.setClickable(true);
  }

  private float rawX(MotionEvent e, int i) {
    return android.os.Build.VERSION.SDK_INT >= 29
        ? e.getRawX(i)
        : e.getX(i) + e.getRawX() - e.getX();
  }

  private float rawY(MotionEvent e, int i) {
    return android.os.Build.VERSION.SDK_INT >= 29
        ? e.getRawY(i)
        : e.getY(i) + e.getRawY() - e.getY();
  }

  private float focusX(MotionEvent e) {
    float n = 0;
    for (int i = 0; i < e.getPointerCount(); i++) n += rawX(e, i);
    return n / e.getPointerCount();
  }

  private float focusY(MotionEvent e) {
    float n = 0;
    for (int i = 0; i < e.getPointerCount(); i++) n += rawY(e, i);
    return n / e.getPointerCount();
  }

  private float distance(MotionEvent e) {
    return e.getPointerCount() < 2
        ? 0
        : (float) Math.hypot(rawX(e, 1) - rawX(e, 0), rawY(e, 1) - rawY(e, 0));
  }

  private float angle(MotionEvent e) {
    return e.getPointerCount() < 2
        ? 0
        : (float) Math.toDegrees(Math.atan2(rawY(e, 1) - rawY(e, 0), rawX(e, 1) - rawX(e, 0)));
  }

  private void begin(View v, MotionEvent e) {
    pointers = e.getPointerCount();
    x = focusX(e);
    y = focusY(e);
    distance = distance(e);
    angle = angle(e);
    scale = v.getScaleX();
    rotation = v.getRotation();
    translationX = v.getTranslationX();
    translationY = v.getTranslationY();
  }

  @Override
  public boolean onTouch(View v, MotionEvent e) {
    int action = e.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN
        || action == MotionEvent.ACTION_POINTER_DOWN
        || pointers != e.getPointerCount()) {
      begin(v, e);
      v.getParent().requestDisallowInterceptTouchEvent(true);
      return true;
    }
    if (action == MotionEvent.ACTION_MOVE) {
      float dx = focusX(e) - x, dy = focusY(e) - y;
      v.setTranslationX(translationX + dx);
      v.setTranslationY(translationY + dy);
      if (e.getPointerCount() > 1 && distance > 0) {
        float zoom = Math.max(.4f, Math.min(3, scale * distance(e) / distance));
        v.setScaleX(zoom);
        v.setScaleY(zoom);
        v.setRotation(rotation + angle(e) - angle);
      }
      v.invalidate();
      begin(v, e);
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      v.getParent().requestDisallowInterceptTouchEvent(false);
      if (action == MotionEvent.ACTION_UP) v.performClick();
      pointers = 0;
      return true;
    }
    if (action == MotionEvent.ACTION_POINTER_UP) pointers = 0;
    return true;
  }
}
