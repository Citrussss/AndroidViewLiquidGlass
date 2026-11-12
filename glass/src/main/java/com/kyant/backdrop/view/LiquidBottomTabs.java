package com.kyant.backdrop.view;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LiquidBottomTabs extends GlassView {
  private final LinearLayout row;
  private final GlassView selector;
  private int selected;

  public interface OnTabSelectedListener {
    void onTabSelected(int index);
  }

  private OnTabSelectedListener listener;

  public LiquidBottomTabs(Context c) {
    super(c);
    selector = new GlassView(c);
    selector.style.radius = 100;
    selector.style.blur = 0;
    selector.style.refractionHeight = 12;
    selector.style.refractionAmount = 16;
    selector.style.tint = 0x44ffffff;
    selector.style.shadow = 0;
    selector.setBackdropLayers(this);
    selector.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    addView(selector);
    row = new LinearLayout(c);
    addView(row, new LayoutParams(-1, -1));
  }

  public void setOnTabSelectedListener(OnTabSelectedListener l) {
    listener = l;
  }

  public void setTabs(String... names) {
    row.removeAllViews();
    for (int i = 0; i < names.length; i++) {
      final int index = i;
      TextView t = new TextView(getContext());
      t.setText(names[i]);
      t.setTextSize(15);
      t.setTextColor(-1);
      t.setGravity(Gravity.CENTER);
      t.setFocusable(true);
      t.setOnClickListener(v -> setSelectedTab(index));
      row.addView(t, new LinearLayout.LayoutParams(0, -1, 1));
    }
    setSelectedTab(Math.min(selected, names.length - 1));
  }

  public int getSelectedTab() {
    return selected;
  }

  public void setSelectedTab(int index) {
    if (index < 0 || index >= row.getChildCount()) return;
    selected = index;
    for (int i = 0; i < row.getChildCount(); i++) {
      android.view.View t = row.getChildAt(i);
      t.setSelected(i == index);
    }
    positionSelector(true);
    if (listener != null) listener.onTabSelected(index);
  }

  private void positionSelector(boolean animate) {
    if (row.getChildCount() == 0) return;
    int visual =
        getLayoutDirection() == LAYOUT_DIRECTION_RTL
            ? row.getChildCount() - 1 - selected
            : selected;
    float x = getWidth() / (float) row.getChildCount() * visual;
    if (animate)
      selector
          .animate()
          .translationX(x)
          .setDuration(250)
          .setInterpolator(new android.view.animation.OvershootInterpolator(.6f))
          .start();
    else selector.setTranslationX(x);
    selector.invalidate();
  }

  @Override
  protected void onMeasure(int w, int h) {
    super.onMeasure(w, h);
    int width = getMeasuredWidth() / Math.max(1, row.getChildCount());
    selector.measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    selector.layout(0, 0, selector.getMeasuredWidth(), getHeight());
    positionSelector(false);
  }

  @Override
  protected android.os.Parcelable onSaveInstanceState() {
    android.os.Bundle state = new android.os.Bundle();
    state.putParcelable("parent", super.onSaveInstanceState());
    state.putInt("selected", selected);
    return state;
  }

  @Override
  protected void onRestoreInstanceState(android.os.Parcelable state) {
    if (state instanceof android.os.Bundle) {
      android.os.Bundle b = (android.os.Bundle) state;
      super.onRestoreInstanceState(b.getParcelable("parent"));
      setSelectedTab(b.getInt("selected"));
    } else super.onRestoreInstanceState(state);
  }
}
