package com.kyant.backdrop.view;

/** All dimensions are density independent pixels. Call invalidate() after changing a style. */
public final class GlassStyle {
  public float radius = 32, blur = 8, refractionHeight = 20, refractionAmount = 28;
  public float highlight = .65f, shadow = 12, saturation = 1.15f, zoom = 1;
  public float brightness = 0, contrast = 1, opacity = 1;
  public float highlightAngle = 45, highlightFalloff = 1, highlightWidth = 1;
  public float innerShadow = 0;
  public boolean alphaMaskedBlur = false;

  /** Optional TL, TR, BR, BL radii in dp; null uses radius. */
  public float[] cornerRadii;

  public boolean depthEffect = true, chromaticAberration = true, adaptiveLuminance = false;
  public int tint = 0x28ffffff;

  /** 0 = uniform; 1 = progressively stronger blur toward bottom. */
  public float progressiveBlur = 0;
}
