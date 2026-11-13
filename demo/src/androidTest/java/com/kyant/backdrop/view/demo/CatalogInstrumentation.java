package com.kyant.backdrop.view.demo;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import com.kyant.backdrop.view.*;

/** Device smoke test exercises all destinations and renders screenshots using the real GPU. */
public class CatalogInstrumentation extends Instrumentation {
  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      for (int page = -1; page < 13; page++) {
        Intent intent =
            new Intent(getTargetContext(), MainActivity.class)
                .putExtra("page", page)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = startActivitySync(intent);
        waitForIdleSync();
        SystemClock.sleep(350);
        runOnMainSync(() -> exercise(activity.getWindow().getDecorView()));
        if (page == 4)
          runOnMainSync(() -> clickNamed(activity.getWindow().getDecorView(), "Open glass dialog"));
        if (page == 7) runOnMainSync(() -> dragMagnifier(activity.getWindow().getDecorView()));
        if (page == 11 || page == 12) checkScrolling(activity);
        waitForIdleSync();
        SystemClock.sleep(300);
        Bitmap frame = getUiAutomation().takeScreenshot();
        if (frame == null || frame.getWidth() == 0) throw new AssertionError("Empty GPU frame");
        java.io.File folder = getTargetContext().getExternalFilesDir("screenshots");
        folder.mkdirs();
        try (java.io.FileOutputStream out =
            new java.io.FileOutputStream(new java.io.File(folder, "page-" + page + ".png"))) {
          frame.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        frame.recycle();
        runOnMainSync(activity::finish);
        waitForIdleSync();
        Bundle status = new Bundle();
        status.putString("stream", "Rendered page " + page + "\n");
        sendStatus(0, status);
      }
      result.putString(
          "stream", "PASS: 14 destinations rendered; toggle, slider, tab state verified.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable error) {
      result.putString("stream", android.util.Log.getStackTraceString(error));
      finish(Activity.RESULT_CANCELED, result);
    }
  }

  private BackdropLayout findBackdrop(View view) {
    if (view instanceof BackdropLayout) return (BackdropLayout) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        BackdropLayout found = findBackdrop(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  private void checkScrolling(Activity activity) {
    BackdropLayout[] source = new BackdropLayout[1];
    runOnMainSync(() -> source[0] = findBackdrop(activity.getWindow().getDecorView()));
    Bitmap before = getUiAutomation().takeScreenshot();
    runOnMainSync(
        () -> {
          View child = source[0].foreground().getChildAt(0);
          if (child instanceof android.widget.ScrollView)
            ((android.widget.ScrollView) child).scrollTo(0, 600);
          if (child instanceof android.widget.ListView)
            ((android.widget.ListView) child).setSelectionFromTop(5, -90);
        });
    waitForIdleSync();
    SystemClock.sleep(250);
    Bitmap after = getUiAutomation().takeScreenshot();
    int[] location = new int[2];
    runOnMainSync(() -> source[0].getLocationOnScreen(location));
    float density = getTargetContext().getResources().getDisplayMetrics().density;
    int y = Math.min(after.getHeight() - 1, location[1] + (int) (140 * density));
    boolean changed = false;
    for (int x = after.getWidth() / 4; x < after.getWidth() * 3 / 4; x += 23)
      if (before.getPixel(x, y) != after.getPixel(x, y)) {
        changed = true;
        break;
      }
    before.recycle();
    after.recycle();
    if (!changed) throw new AssertionError("Scrolling glass cards did not update the rendered frame");
  }

  private boolean clickNamed(View v, String name) {
    if (name.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) {
      v.performClick();
      return true;
    }
    if (v instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) v;
      for (int i = 0; i < group.getChildCount(); i++)
        if (clickNamed(group.getChildAt(i), name)) return true;
    }
    return false;
  }

  private void dragMagnifier(View v) {
    if (v instanceof GlassView && ((GlassView) v).style.zoom > 1) {
      long now = SystemClock.uptimeMillis();
      MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 40, 40, 0),
          move = MotionEvent.obtain(now, now + 16, MotionEvent.ACTION_MOVE, 90, 100, 0),
          up = MotionEvent.obtain(now, now + 32, MotionEvent.ACTION_UP, 90, 100, 0);
      v.dispatchTouchEvent(down);
      v.dispatchTouchEvent(move);
      v.dispatchTouchEvent(up);
      down.recycle();
      move.recycle();
      up.recycle();
      if (v.getTranslationX() <= 0)
        throw new AssertionError("Magnifier drag did not move the lens");
      return;
    }
    if (v instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) v;
      for (int i = 0; i < group.getChildCount(); i++) dragMagnifier(group.getChildAt(i));
    }
  }

  private void exercise(View v) {
    if (v instanceof LiquidToggle) {
      LiquidToggle t = (LiquidToggle) v;
      boolean before = t.isChecked();
      t.performClick();
      if (t.isChecked() == before) throw new AssertionError("Toggle did not toggle");
    }
    if (v instanceof LiquidSlider) {
      LiquidSlider s = (LiquidSlider) v;
      s.setValue(.75f);
      if (Math.abs(s.getValue() - .75f) > .001f) throw new AssertionError("Slider value");
    }
    if (v instanceof LiquidBottomTabs) {
      LiquidBottomTabs t = (LiquidBottomTabs) v;
      t.setSelectedTab(2);
      if (t.getSelectedTab() != 2) throw new AssertionError("Tab selection");
      t.setSelectedTab(0);
      // Exercise real dispatch through the clickable label children, not the selection setter.
      t.jumpDrawablesToCurrentState();
      int width = t.getWidth();
      long now = SystemClock.uptimeMillis();
      float x = width / 6f, y = t.getHeight() / 2f;
      MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
      t.dispatchTouchEvent(down); down.recycle();
      for (int step = 1; step <= 5; step++) {
        MotionEvent move = MotionEvent.obtain(now, now + step * 16, MotionEvent.ACTION_MOVE, x + width * .65f * step / 5, y, 0);
        t.dispatchTouchEvent(move); move.recycle();
      }
      MotionEvent up = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, width * .82f, y, 0);
      t.dispatchTouchEvent(up); up.recycle();
      if (t.getSelectedTab() == 0) throw new AssertionError("Horizontal tab drag did not select another tab");
    }
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) exercise(g.getChildAt(i));
    }
  }
}
