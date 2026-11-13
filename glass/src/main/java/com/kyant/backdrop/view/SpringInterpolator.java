package com.kyant.backdrop.view;

import android.animation.TimeInterpolator;

/** Unit-mass spring response, using the same damping ratio and stiffness as the catalog. */
final class SpringInterpolator implements TimeInterpolator {
  private final float damping, stiffness, durationSeconds;
  SpringInterpolator(float damping, float stiffness, float durationSeconds) {
    this.damping = damping; this.stiffness = stiffness; this.durationSeconds = durationSeconds;
  }
  @Override public float getInterpolation(float input) {
    if (input >= 1) return 1;
    double t = input * durationSeconds, omega = Math.sqrt(stiffness);
    if (damping >= 1) return (float) (1 - (1 + omega * t) * Math.exp(-omega * t));
    double beta = Math.sqrt(1 - damping * damping);
    return (float) (1 - Math.exp(-damping * omega * t)
        * (Math.cos(omega * beta * t) + damping / beta * Math.sin(omega * beta * t)));
  }
}
