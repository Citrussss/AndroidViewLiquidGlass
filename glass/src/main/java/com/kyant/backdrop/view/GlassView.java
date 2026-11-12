package com.kyant.backdrop.view;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/** A real ViewGroup: ordinary Android children stay sharp above the filtered backdrop. */
public class GlassView extends FrameLayout {
  @Override
  public boolean performClick() {
    return super.performClick();
  }

  public final GlassStyle style = new GlassStyle();
  private final Paint paint = new Paint(3);
  private final Path clip = new Path();
  private final RectF rect = new RectF();
  private RenderNode node;
  private RenderNode exportedSurface;
  private GlassView[] backdropLayers = new GlassView[0];
  private RuntimeShader lens;
  private RuntimeShader highlightShader, sdfShader, alphaShader;
  private Bitmap sdfTexture;
  private RenderEffect extraEffect;
  private float luminance = .5f;
  private long lastLuminanceSample;
  private float previousRadius = -1;
  private BackdropLayout backdrop;
  private final int[] previousPosition = {Integer.MIN_VALUE, Integer.MIN_VALUE};
  private final android.view.ViewTreeObserver.OnPreDrawListener positionObserver =
      () -> {
        int[] location = new int[2];
        getLocationInWindow(location);
        if (location[0] != previousPosition[0] || location[1] != previousPosition[1]) {
          previousPosition[0] = location[0];
          previousPosition[1] = location[1];
          invalidate();
        }
        return true;
      };

  public GlassView(Context c) {
    this(c, null);
  }

  public GlassView(Context c, AttributeSet a) {
    super(c, a);
    setWillNotDraw(false);
    setClipChildren(false);
    setClipToOutline(true);
    setOutlineProvider(
        new android.view.ViewOutlineProvider() {
          @Override
          public void getOutline(View v, Outline o) {
            float radius = Math.min(dp(style.radius), Math.min(v.getWidth(), v.getHeight()) / 2f);
            if (style.cornerRadii == null)
              o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            else if (!clip.isEmpty()) o.setConvexPath(clip);
          }
        });
    if (Build.VERSION.SDK_INT >= 29) {
      node = new RenderNode("Glass effect");
      exportedSurface = new RenderNode("Exported glass surface");
    }
    if (a != null) {
      android.content.res.TypedArray values = c.obtainStyledAttributes(a, R.styleable.GlassView);
      float density = getResources().getDisplayMetrics().density;
      style.radius =
          values.getDimension(R.styleable.GlassView_glassRadius, dp(style.radius)) / density;
      style.blur = values.getDimension(R.styleable.GlassView_glassBlur, dp(style.blur)) / density;
      style.refractionHeight =
          values.getDimension(
                  R.styleable.GlassView_glassRefractionHeight, dp(style.refractionHeight))
              / density;
      style.refractionAmount =
          values.getDimension(
                  R.styleable.GlassView_glassRefractionAmount, dp(style.refractionAmount))
              / density;
      style.tint = values.getColor(R.styleable.GlassView_glassTint, style.tint);
      style.chromaticAberration =
          values.getBoolean(R.styleable.GlassView_glassChromaticAberration, true);
      values.recycle();
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getViewTreeObserver().addOnPreDrawListener(positionObserver);
  }

  @Override
  protected void onDetachedFromWindow() {
    getViewTreeObserver().removeOnPreDrawListener(positionObserver);
    if (Build.VERSION.SDK_INT >= 29) {
      node.discardDisplayList();
      exportedSurface.discardDisplayList();
    }
    super.onDetachedFromWindow();
  }

  public float dp(float n) {
    return n * getResources().getDisplayMetrics().density;
  }

  public void setBackdrop(BackdropLayout source) {
    backdrop = source;
    invalidate();
  }

  /**
   * Composite exported glass surfaces over the root source, excluding their children. This allows a
   * thumb to refract its track without capturing itself.
   */
  public void setBackdropLayers(GlassView... layers) {
    for (GlassView layer : layers)
      if (layer == this || layer.dependsOn(this))
        throw new IllegalArgumentException("Cyclic backdrop layers");
    backdropLayers = layers.clone();
    invalidate();
  }

  private boolean dependsOn(GlassView view) {
    for (GlassView layer : backdropLayers) if (layer == view || layer.dependsOn(view)) return true;
    return false;
  }

  private void drawSource(Canvas c, BackdropLayout source) {
    source.drawSource(c, this, Math.max(.1f, style.zoom));
    for (GlassView layer : backdropLayers) {
      c.save();
      c.scale(
          Math.max(.1f, style.zoom), Math.max(.1f, style.zoom), getWidth() / 2f, getHeight() / 2f);
      if (Build.VERSION.SDK_INT >= 29) {
        Matrix target = new Matrix(), origin = new Matrix(), inverse = new Matrix();
        transformMatrixToGlobal(target);
        layer.transformMatrixToGlobal(origin);
        if (target.invert(inverse)) {
          inverse.preConcat(origin);
          c.concat(inverse);
        }
      } else {
        int[] a = new int[2], b = new int[2];
        getLocationInWindow(a);
        layer.getLocationInWindow(b);
        c.translate(b[0] - a[0], b[1] - a[1]);
      }
      if (Build.VERSION.SDK_INT >= 29
          && c.isHardwareAccelerated()
          && layer.exportedSurface.hasDisplayList()) c.drawRenderNode(layer.exportedSurface);
      else if (!c.isHardwareAccelerated()) layer.drawGlass(c);
      c.restore();
    }
  }

  /** Optional custom backdrop-only effect applied after the built-in effects; API 31+. */
  public void setBackdropEffect(RenderEffect effect) {
    extraEffect = effect;
    invalidate();
  }

  /** SDF texture uses R=distance, GB=normal, A=coverage, as in the original catalog. API 33+. */
  public void setSdfTexture(Bitmap texture) {
    sdfTexture = texture;
    sdfShader = null;
    invalidate();
  }

  public float getSampledLuminance() {
    return luminance;
  }

  private BackdropLayout source() {
    if (backdrop != null) return backdrop;
    android.view.ViewParent p = getParent();
    while (p != null) {
      if (p instanceof BackdropLayout) return (BackdropLayout) p;
      p = p.getParent();
    }
    return null;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (Build.VERSION.SDK_INT >= 29 && canvas.isHardwareAccelerated()) {
      exportedSurface.setPosition(0, 0, getWidth(), getHeight());
      Canvas recorded = exportedSurface.beginRecording();
      try {
        drawGlass(recorded);
      } finally {
        exportedSurface.endRecording();
      }
      canvas.drawRenderNode(exportedSurface);
    } else drawGlass(canvas);
  }

  private void drawGlass(Canvas canvas) {
    float r = Math.min(dp(style.radius), Math.min(getWidth(), getHeight()) / 2f);
    rect.set(0, 0, getWidth(), getHeight());
    clip.reset();
    float[] corners = {r, r, r, r};
    if (style.cornerRadii != null && style.cornerRadii.length == 4)
      for (int i = 0; i < 4; i++)
        corners[i] =
            Math.min(Math.min(getWidth(), getHeight()) / 2f, Math.max(0, dp(style.cornerRadii[i])));
    clip.addRoundRect(
        rect,
        new float[] {
          corners[0],
          corners[0],
          corners[1],
          corners[1],
          corners[2],
          corners[2],
          corners[3],
          corners[3]
        },
        Path.Direction.CW);
    setElevation(dp(style.shadow));
    if (r != previousRadius || style.cornerRadii != null) {
      previousRadius = r;
      invalidateOutline();
    }
    int save = canvas.save();
    canvas.clipPath(clip);
    BackdropLayout source = source();
    float brightness = style.brightness, contrast = style.contrast, blur = style.blur;
    if (style.adaptiveLuminance && source != null) {
      long now = android.os.SystemClock.uptimeMillis();
      if (now - lastLuminanceSample > 200) {
        luminance = source.sampleLuminance(this);
        lastLuminanceSample = now;
      }
      float l = luminance * 2 - 1;
      l = Math.signum(l) * l * l;
      brightness = l > 0 ? .1f + .4f * l : .1f + .3f * l;
      contrast = l > 0 ? 1 - l : 1;
      blur = l > 0 ? 8 + 8 * l : 8 + 6 * l;
      if (this instanceof LiquidButton)
        ((LiquidButton) this).setTextColor(luminance > .5f ? Color.BLACK : Color.WHITE);
    }
    if (source != null && Build.VERSION.SDK_INT >= 31 && canvas.isHardwareAccelerated()) {
      int padding = (int) Math.ceil(dp(blur * 3 + style.refractionAmount + style.progressiveBlur));
      node.setPosition(-padding, -padding, getWidth() + padding, getHeight() + padding);
      Canvas recording = node.beginRecording();
      try {
        recording.translate(padding, padding);
        drawSource(recording, source);
      } finally {
        node.endRecording();
      }
      RenderEffect effect = null;
      if (blur > 0)
        effect = RenderEffect.createBlurEffect(dp(blur), dp(blur), Shader.TileMode.CLAMP);
      ColorMatrix cm = new ColorMatrix();
      cm.setSaturation(style.saturation);
      float shift = (.5f - .5f * contrast + brightness) * 255;
      cm.postConcat(
          new ColorMatrix(
              new float[] {
                contrast,
                0,
                0,
                0,
                shift,
                0,
                contrast,
                0,
                0,
                shift,
                0,
                0,
                contrast,
                0,
                shift,
                0,
                0,
                0,
                style.opacity,
                0
              }));
      RenderEffect colors = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm));
      effect = effect == null ? colors : RenderEffect.createChainEffect(colors, effect);
      if (Build.VERSION.SDK_INT >= 33) {
        if (lens == null) lens = new RuntimeShader(GlassShaders.LENS);
        lens.setFloatUniform("size", getWidth(), getHeight());
        lens.setFloatUniform("offset", -padding, -padding);
        lens.setFloatUniform("cornerRadii", corners);
        lens.setFloatUniform("refractionHeight", Math.max(.001f, dp(style.refractionHeight)));
        lens.setFloatUniform(
            "refractionAmount", style.refractionHeight > 0 ? -dp(style.refractionAmount) : 0);
        lens.setFloatUniform("depthEffect", style.depthEffect ? 1 : 0);
        lens.setFloatUniform("chromaticAberration", style.chromaticAberration ? 1 : 0);
        lens.setFloatUniform("progressiveBlur", dp(style.progressiveBlur));
        lens.setFloatUniform("adaptiveLuminance", 0);
        effect =
            RenderEffect.createChainEffect(
                RenderEffect.createRuntimeShaderEffect(lens, "content"), effect);
        if (style.alphaMaskedBlur) {
          if (alphaShader == null) alphaShader = new RuntimeShader(GlassShaders.ALPHA_MASK);
          alphaShader.setFloatUniform("size", getWidth(), getHeight());
          alphaShader.setFloatUniform("offset", -padding, -padding);
          effect =
              RenderEffect.createChainEffect(
                  RenderEffect.createRuntimeShaderEffect(alphaShader, "content"), effect);
        }
        if (sdfTexture != null) {
          if (sdfShader == null) {
            sdfShader = new RuntimeShader(GlassShaders.SDF);
            sdfShader.setInputBuffer(
                "sdfTex",
                new BitmapShader(sdfTexture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
          }
          sdfShader.setFloatUniform("size", getWidth(), getHeight());
          sdfShader.setFloatUniform("sdfTexSize", sdfTexture.getWidth(), sdfTexture.getHeight());
          sdfShader.setFloatUniform("offset", -padding, -padding);
          sdfShader.setFloatUniform("refractionHeight", dp(48));
          sdfShader.setFloatUniform("lightAngle", style.highlightAngle);
          effect =
              RenderEffect.createChainEffect(
                  RenderEffect.createRuntimeShaderEffect(sdfShader, "content"), effect);
        }
      }
      if (extraEffect != null) effect = RenderEffect.createChainEffect(extraEffect, effect);
      node.setRenderEffect(effect);
      canvas.drawRenderNode(node);
    } else if (source != null) drawSource(canvas, source);
    paint.setShader(null);
    paint.setColor(style.tint);
    canvas.drawRect(rect, paint);
    if (style.innerShadow > 0) {
      paint.setShader(
          new LinearGradient(
              0,
              0,
              0,
              dp(style.innerShadow),
              0x44000000,
              Color.TRANSPARENT,
              Shader.TileMode.CLAMP));
      canvas.drawRect(rect, paint);
      paint.setShader(null);
    }
    canvas.restoreToCount(save);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(style.highlightWidth));
    paint.setShader(
        new LinearGradient(
            0,
            0,
            getWidth(),
            getHeight(),
            new int[] {
              Color.argb((int) (255 * style.highlight), 255, 255, 255), 0x12ffffff, 0x88ffffff
            },
            null,
            Shader.TileMode.CLAMP));
    if (Build.VERSION.SDK_INT >= 33) {
      if (highlightShader == null) highlightShader = new RuntimeShader(GlassShaders.HIGHLIGHT);
      highlightShader.setFloatUniform("size", getWidth(), getHeight());
      highlightShader.setFloatUniform("cornerRadii", corners);
      highlightShader.setColorUniform(
          "color",
          Color.argb((int) (255 * Math.max(0, Math.min(1, style.highlight))), 255, 255, 255));
      highlightShader.setFloatUniform("angle", (float) Math.toRadians(style.highlightAngle));
      highlightShader.setFloatUniform("falloff", style.highlightFalloff);
      paint.setShader(highlightShader);
    }
    if (style.highlight > 0 && style.highlightWidth > 0) {
      int edgeSave = canvas.save();
      canvas.clipPath(clip);
      canvas.drawPath(clip, paint);
      canvas.restoreToCount(edgeSave);
    }
    paint.setShader(null);
    paint.setStyle(Paint.Style.FILL);
  }
}
