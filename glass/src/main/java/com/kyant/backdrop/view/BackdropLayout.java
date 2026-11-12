package com.kyant.backdrop.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Put background content in content(), and glass/foreground Views in foreground(). The background
 * is recorded once per frame, independently of the glass, preventing feedback.
 */
public class BackdropLayout extends FrameLayout {
  private final FrameLayout content, foreground;
  private RenderNode recording;
  private long generation;
  private android.graphics.Bitmap luminanceBuffer;

  public BackdropLayout(Context c) {
    this(c, null);
  }

  public BackdropLayout(Context c, AttributeSet a) {
    super(c, a);
    content = new FrameLayout(c);
    foreground = new FrameLayout(c);
    addView(content, new LayoutParams(-1, -1));
    addView(foreground, new LayoutParams(-1, -1));
    if (Build.VERSION.SDK_INT >= 29) recording = new RenderNode("Backdrop source");
  }

  public FrameLayout content() {
    return content;
  }

  public FrameLayout foreground() {
    return foreground;
  }

  long generation() {
    return generation;
  }

  float sampleLuminance(View target) {
    if (luminanceBuffer == null)
      luminanceBuffer =
          android.graphics.Bitmap.createBitmap(5, 5, android.graphics.Bitmap.Config.ARGB_8888);
    luminanceBuffer.eraseColor(android.graphics.Color.TRANSPARENT);
    Canvas c = new Canvas(luminanceBuffer);
    c.scale(5f / Math.max(1, target.getWidth()), 5f / Math.max(1, target.getHeight()));
    drawSource(c, target, 1);
    int[] pixels = new int[25];
    luminanceBuffer.getPixels(pixels, 0, 5, 0, 0, 5, 5);
    float sum = 0;
    for (int color : pixels)
      sum +=
          (.2126f * android.graphics.Color.red(color)
                  + .7152f * android.graphics.Color.green(color)
                  + .0722f * android.graphics.Color.blue(color))
              / 255f;
    return sum / 25;
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (Build.VERSION.SDK_INT >= 29
        && canvas.isHardwareAccelerated()
        && getWidth() > 0
        && getHeight() > 0) {
      recording.setPosition(0, 0, getWidth(), getHeight());
      Canvas target = recording.beginRecording();
      try {
        content.draw(target);
      } finally {
        recording.endRecording();
      }
      generation++;
      canvas.drawRenderNode(recording);
      drawChild(canvas, foreground, getDrawingTime());
    } else super.dispatchDraw(canvas);
  }

  void drawSource(Canvas canvas, View target, float zoom) {
    int[] a = new int[2], b = new int[2];
    getLocationInWindow(a);
    target.getLocationInWindow(b);
    canvas.save();
    canvas.scale(zoom, zoom, target.getWidth() / 2f, target.getHeight() / 2f);
    if (Build.VERSION.SDK_INT >= 29) {
      android.graphics.Matrix targetGlobal = new android.graphics.Matrix(),
          sourceGlobal = new android.graphics.Matrix(),
          inverse = new android.graphics.Matrix();
      target.transformMatrixToGlobal(targetGlobal);
      transformMatrixToGlobal(sourceGlobal);
      if (targetGlobal.invert(inverse)) {
        inverse.preConcat(sourceGlobal);
        canvas.concat(inverse);
      }
    } else canvas.translate(a[0] - b[0], a[1] - b[1]);
    if (Build.VERSION.SDK_INT >= 29 && canvas.isHardwareAccelerated())
      canvas.drawRenderNode(recording);
    else content.draw(canvas);
    canvas.restore();
  }
}
