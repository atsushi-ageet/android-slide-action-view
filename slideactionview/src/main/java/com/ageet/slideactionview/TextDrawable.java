package com.ageet.slideactionview;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

class TextDrawable extends Drawable {
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_SIZE = sp(48);
    private static final int DEFAULT_SHADOW_COLOR = Color.BLACK;
    private static final float DEFAULT_SHADOW_RADIUS = 3f;
    private Paint mPaint = new Paint();
    private String mText;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    TextDrawable(String text) {
        mText = text;
        mPaint.setColor(DEFAULT_TEXT_COLOR);
        mPaint.setTextSize(DEFAULT_TEXT_SIZE);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setShadowLayer(DEFAULT_SHADOW_RADIUS, 0, 0, DEFAULT_SHADOW_COLOR);
        mPaint.setAntiAlias(true);
        mIntrinsicWidth = (int) (mPaint.measureText(mText, 0, mText.length()) + .5);
        mIntrinsicHeight = mPaint.getFontMetricsInt(null);
    }
    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        setTextSizeForWidth(bounds.width());
        Rect textBounds = new Rect();
        mPaint.getTextBounds(mText, 0, mText.length(), textBounds);
        canvas.drawText(mText, bounds.centerX(), bounds.centerY() - ((mPaint.descent() + mPaint.ascent()) / 2), mPaint);
    }
    @Override
    public int getOpacity() {
        return mPaint.getAlpha();
    }
    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }
    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }
    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }
    @Override
    public void setColorFilter(ColorFilter filter) {
        mPaint.setColorFilter(filter);
    }

    public void setTextColor(int color) {
        mPaint.setColor(color);
    }

    public void setShadowColor(int color) {
        mPaint.setShadowLayer(DEFAULT_SHADOW_RADIUS, 0, 0, color);
    }

    private Rect mTempRect = new Rect();

    private void setTextSizeForWidth(float desiredWidth) {
        mPaint.setTextSize(DEFAULT_TEXT_SIZE);
        mPaint.getTextBounds(mText, 0, mText.length(), mTempRect);
        float desiredTextSize = mPaint.getTextSize() * desiredWidth / mTempRect.width();
        mPaint.setTextSize(desiredTextSize);
    }

    private static int sp(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().getDisplayMetrics());
    }
}
