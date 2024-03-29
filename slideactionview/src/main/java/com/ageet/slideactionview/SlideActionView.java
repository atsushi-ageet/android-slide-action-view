package com.ageet.slideactionview;

import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public class SlideActionView extends View {
    private enum State {
        NORMAL,
        DRAG_SLIDER_LEFT,
        DRAG_SLIDER_RIGHT,
        COMPLETE_SLIDER_LEFT,
        COMPLETE_SLIDER_RIGHT
    }

    private static abstract class DrawableHolder<T extends Drawable> {
        T drawable = null;
        Rect position = new Rect();

        Rect getCurrentBounds() {
            if (drawable != null) {
                return drawable.getBounds();
            } else {
                return position;
            }
        }
        int getWidth() {
            return position.width();
        }
        int getHeight() {
            return position.height();
        }
        boolean canDraw() {
            return drawable != null;
        }
        void drawIfPossible(Canvas canvas) {
            if (canDraw()) drawable.draw(canvas);
        }
    }

    private class Background extends DrawableHolder<Drawable> implements StateObserver {
        void initDimensions() {
            position.set(contentPlace);
            if (drawable != null) drawable.setBounds(position);
        }

        @Override
        void drawIfPossible(Canvas canvas) {
            if (!canDraw()) return;
            if (state == State.DRAG_SLIDER_LEFT || state == State.DRAG_SLIDER_RIGHT) {
                int left = sliderL.drawable != null ? sliderL.drawable.getBounds().left - sliderL.marginHorizontal : position.left;
                int right = sliderR.drawable != null ? sliderR.drawable.getBounds().right + sliderR.marginHorizontal : position.right;
                temporaryRect.set(left, contentPlace.top, right, contentPlace.bottom);
                drawable.setBounds(temporaryRect);
            }
            drawable.draw(canvas);
        }
        @Override
        public void onStateChanged(State state) {
            if (state == State.NORMAL) {
                move(drawable, DURATION_MOVE_SLIDER, position);
            } else if (state == State.COMPLETE_SLIDER_LEFT || state == State.COMPLETE_SLIDER_RIGHT) {
                Slider slider = state == State.COMPLETE_SLIDER_LEFT ? sliderL : sliderR;
                if (slider.completeToCenter) {
                    temporaryRect.set(slider.position);
                    temporaryRect.offsetTo(
                            contentPlace.centerX() - temporaryRect.width() / 2,
                            contentPlace.centerY() - temporaryRect.height() / 2);
                } else {
                    temporaryRect.set(slider.target);
                }
                temporaryRect.inset(-slider.marginHorizontal, -slider.marginVertical);
                move(sliderBg.drawable, DURATION_COMPLETE_SLIDER, temporaryRect);
            }
        }
    }

    private abstract class Slider extends DrawableHolder<Drawable> implements StateObserver {
        SliderListener listener;
        Rect target = new Rect();
        int marginHorizontal = dip(4);
        int marginVertical = dip(4);
        boolean completeToCenter = true;

        boolean performDrop(Point point) {
            if (checkSlideComplete(point)) {
                debug("slide complete");
                if (listener != null) listener.onSlideComplete();
                return true;
            } else {
                return false;
            }
        }
        void performMove(Point point) {
            temporaryRect.set(
                    point.x - getWidth() / 2,
                    point.y - getHeight() / 2,
                    point.x + getWidth() / 2,
                    point.y + getHeight() / 2);
            drawable.setBounds(temporaryRect);
        }
        boolean checkSlideStart(Point point) {
            return position.contains(point.x, point.y);
        }
        abstract boolean checkSlideComplete(Point point);
        abstract Point getPositionOffset();
        abstract Point getTargetOffset();

        void initDimensions() {
            int width = 0;
            int height = 0;
            if (drawable != null) {
                if (drawable.getIntrinsicHeight() < 0 || contentPlace.height() < drawable.getIntrinsicHeight() + marginVertical) {
                    debug("fill drawable size for slider");
                    float aspectRatio = (float) drawable.getIntrinsicWidth() / (float) drawable.getIntrinsicHeight();
                    width = (int) (contentPlace.height() * aspectRatio) - marginHorizontal * 2;
                    height = contentPlace.height() - marginVertical * 2;
                } else {
                    debug("original drawable size for slider");
                    width = drawable.getIntrinsicWidth();
                    height = drawable.getIntrinsicHeight();
                }
            }
            debug("width = " + width + ", height = " + height);
            position.set(0, 0, width, height);
            target.set(0, 0, width, height);
            Point positionOffset = getPositionOffset();
            Point targetOffset = getTargetOffset();
            debug("positionOffset = " + positionOffset + ", targetOffset = " + targetOffset);
            position.offset(positionOffset.x, positionOffset.y);
            target.offset(targetOffset.x, targetOffset.y);
            if (drawable != null) drawable.setBounds(position);
        }

        @Override
        public void onStateChanged(State state) {
            if (state == State.NORMAL) {
                move(drawable, DURATION_MOVE_SLIDER, position);
                show(drawable, DURATION_SHOW_SLIDER);
            } else if ((state == State.COMPLETE_SLIDER_LEFT || state == State.COMPLETE_SLIDER_RIGHT)) {
                if (completeToCenter) {
                    temporaryRect.set(position);
                    temporaryRect.offsetTo(contentPlace.centerX() - getWidth() / 2, position.top);
                } else {
                    temporaryRect.set(target);
                }
                move(drawable, DURATION_COMPLETE_SLIDER, temporaryRect);
            }
        }
    }

    private class SliderLeft extends Slider {
        @Override
        boolean checkSlideComplete(Point point) {
            return target.left <= point.x;
        }
        @Override
        Point getPositionOffset() {
            return new Point(contentPlace.left + marginHorizontal, contentPlace.centerY() - getHeight() / 2);
        }
        @Override
        Point getTargetOffset() {
            return new Point(contentPlace.right - marginHorizontal - getWidth(), contentPlace.centerY() - getHeight() / 2);
        }
        @Override
        public void onStateChanged(State state) {
            super.onStateChanged(state);
            if (state == State.DRAG_SLIDER_RIGHT) {
                hide(drawable, DURATION_HIDE_SLIDER);
            }
            if (drawable != null) {
                drawable.setState(state == State.DRAG_SLIDER_LEFT ? PRESSED_STATE_SET : EMPTY_STATE_SET);
            }
        }
    }

    private class SliderRight extends Slider {
        @Override
        boolean checkSlideComplete(Point point) {
            return target.right >= point.x;
        }
        @Override
        Point getPositionOffset() {
            return new Point(contentPlace.right - marginHorizontal - getWidth(), contentPlace.centerY() - getHeight() / 2);
        }
        @Override
        Point getTargetOffset() {
            return new Point(contentPlace.left + marginHorizontal, contentPlace.centerY() - getHeight() / 2);
        }
        @Override
        public void onStateChanged(State state) {
            super.onStateChanged(state);
            if (state == State.DRAG_SLIDER_LEFT) {
                hide(drawable, DURATION_HIDE_SLIDER);
            }
            if (drawable != null) {
                drawable.setState(state == State.DRAG_SLIDER_RIGHT ? PRESSED_STATE_SET : EMPTY_STATE_SET);
            }
        }
    }

    private class Description extends DrawableHolder<TextDrawable> {
        private State targetState;
        private int marginHorizontal = dip(16);

        public Description(State targetState) {
            this.targetState = targetState;
        }
        @Override
        boolean canDraw() {
            return state == targetState &&  super.canDraw()
                    && sliderDesc.getCurrentBounds().left >= sliderL.getCurrentBounds().right
                    && sliderDesc.getCurrentBounds().right <= sliderR.getCurrentBounds().left;
        }
        void initDimensions() {
            position.set(
                    sliderL.position.right + marginHorizontal,
                    contentPlace.top,
                    sliderR.position.left - marginHorizontal,
                    contentPlace.bottom);
            if (drawable != null) drawable.setBounds(position);
        }
    }

    private static boolean debugMode = false;

    public static void setDebugMode(boolean debugMode) {
        SlideActionView.debugMode = debugMode;
    }

    private static final int DURATION_SHOW_SLIDER = 200;
    private static final int DURATION_HIDE_SLIDER = 100;
    private static final int DURATION_MOVE_SLIDER = 500;
    private static final int DURATION_COMPLETE_SLIDER = 1000;

    private State state = State.NORMAL;
    private Background sliderBg = new Background();
    private Slider sliderL = new SliderLeft();
    private Slider sliderR = new SliderRight();
    private Description sliderDesc = new Description(State.NORMAL);
    private Description sliderLeftDesc = new Description(State.DRAG_SLIDER_LEFT);
    private Description sliderRightDesc = new Description(State.DRAG_SLIDER_RIGHT);

    private Float paddingLeftFactor = null;
    private Float paddingRightFactor = null;

    private Rect temporaryRect = new Rect();

    private ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            invalidate();
        }
    };

    public interface SliderListener {
        void onSlideComplete();
    }

    private interface StateObserver {
        void onStateChanged(State state);
    }

    private final Set<StateObserver> stateObservers = new HashSet<>();
    {
        stateObservers.add(sliderBg);
        stateObservers.add(sliderL);
        stateObservers.add(sliderR);
    }

    private void setState(State state) {
        debug("set state to " + state);
        if (this.state != state) {
            this.state = state;
            for (StateObserver observer : stateObservers) {
                observer.onStateChanged(state);
            }
            invalidate();
        }
    }

    public void setLeftSliderListener(SliderListener leftSliderListener) {
        sliderL.listener = leftSliderListener;
    }

    public void setRightSliderListener(SliderListener rightSliderListener) {
        sliderR.listener = rightSliderListener;
    }

    public SlideActionView(Context context) {
        super(context);
    }

    public SlideActionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SlideActionView);
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderBackground))
            sliderBg.drawable = array.getDrawable(R.styleable.SlideActionView_slideactionview_sliderBackground).mutate();
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderRight))
            sliderR.drawable = array.getDrawable(R.styleable.SlideActionView_slideactionview_sliderRight).mutate();
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderLeft))
            sliderL.drawable = array.getDrawable(R.styleable.SlideActionView_slideactionview_sliderLeft).mutate();
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderDescription))
            sliderDesc.drawable = new TextDrawable(array.getString(R.styleable.SlideActionView_slideactionview_sliderDescription));
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderLeftDescription))
            sliderLeftDesc.drawable = new TextDrawable(array.getString(R.styleable.SlideActionView_slideactionview_sliderLeftDescription));
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_sliderRightDescription))
            sliderRightDesc.drawable = new TextDrawable(array.getString(R.styleable.SlideActionView_slideactionview_sliderRightDescription));
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_paddingLeftFactor))
            paddingLeftFactor = array.getFloat(R.styleable.SlideActionView_slideactionview_paddingLeftFactor, 0);
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_paddingRightFactor))
            paddingRightFactor = array.getFloat(R.styleable.SlideActionView_slideactionview_paddingRightFactor, 0);
        if (array.hasValue(R.styleable.SlideActionView_slideactionview_completeToCenter))
            sliderL.completeToCenter = sliderR.completeToCenter = array.getBoolean(R.styleable.SlideActionView_slideactionview_completeToCenter, false);
        array.recycle();
    }

    private Drawable getDrawable(int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getResources().getDrawable(drawableId, getContext().getTheme());
        } else {
            //noinspection deprecation
            return getResources().getDrawable(drawableId);
        }
    }

    public void setSliderBackgroundDrawable(int drawableId) {
        setSliderBackgroundDrawable(getDrawable(drawableId));
    }

    public void setSliderBackgroundDrawable(Drawable drawable) {
        sliderBg.drawable = drawable.mutate();
        requestLayout();
    }

    public void setSliderLeftDrawable(int drawableId) {
        setSliderLeftDrawable(getDrawable(drawableId));
    }

    public void setSliderLeftDrawable(Drawable drawable) {
        sliderL.drawable = drawable.mutate();
        requestLayout();
    }

    public void setSliderRightDrawable(int drawableId) {
        setSliderRightDrawable(getDrawable(drawableId));
    }

    public void setSliderRightDrawable(Drawable drawable) {
        sliderR.drawable = drawable.mutate();
        requestLayout();
    }

    public void setSliderDescription(int descriptionId) {
        sliderDesc.drawable = new TextDrawable(getResources().getString(descriptionId));
        requestLayout();
    }

    public void setSliderDescription(String description) {
        sliderDesc.drawable = new TextDrawable(description);
        requestLayout();
    }

    public void setSliderLeftDescription(int descriptionId) {
        sliderLeftDesc.drawable = new TextDrawable(getResources().getString(descriptionId));
        requestLayout();
    }

    public void setSliderLeftDescription(String description) {
        sliderLeftDesc.drawable = new TextDrawable(description);
        requestLayout();
    }

    public void setSliderRightDescription(int descriptionId) {
        sliderRightDesc.drawable = new TextDrawable(getResources().getString(descriptionId));
        requestLayout();
    }

    public void setSliderRightDescription(String description) {
        sliderRightDesc.drawable = new TextDrawable(description);
        requestLayout();
    }

    public void setPaddingLeftFactor(float padding) {
        paddingLeftFactor = padding;
        requestLayout();
    }

    public void setPaddingRightFactor(float padding) {
        paddingLeftFactor = padding;
        requestLayout();
    }

    public void setCompleteToCenter(boolean completeToCenter) {
        sliderL.completeToCenter = sliderR.completeToCenter = completeToCenter;
        requestLayout();
    }

    public void reset() {
        setState(State.NORMAL);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        debug("onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        debug("onLayout");
        super.onLayout(changed, left, top, right, bottom);
        this.initDimensions();
    }

    private Rect contentPlace = new Rect();

    void initDimensions() {
        int paddingLeft = paddingLeftFactor != null ? (int) (getWidth() * paddingLeftFactor) : getPaddingLeft();
        int paddingRight = paddingRightFactor != null ? getWidth() - (int) (getWidth() * paddingRightFactor) : getPaddingRight();
        int contentWidth = Math.max(getWidth() - paddingLeft - paddingRight, 0);
        contentPlace.set(
                paddingLeft,
                getPaddingTop(),
                paddingLeft + contentWidth,
                getHeight() - getPaddingBottom());
        sliderBg.initDimensions();
        sliderL.initDimensions();
        sliderR.initDimensions();
        sliderDesc.initDimensions();
        sliderLeftDesc.initDimensions();
        sliderRightDesc.initDimensions();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float touchGap;
    private Point touchPoint = new Point();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        debug("onTouchEvent " + event.getAction());
        touchPoint.set((int) event.getX(), (int) event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (state) {
                case NORMAL:
                    if (sliderL.checkSlideStart(touchPoint)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        debug("left active");
                        touchGap = Math.max(touchPoint.x - sliderL.position.centerX(), 0);
                        setState(State.DRAG_SLIDER_LEFT);
                        return true;
                    } else if (sliderR.checkSlideStart(touchPoint)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        debug("right active");
                        touchGap = Math.min(touchPoint.x - sliderR.position.centerX(), 0);
                        setState(State.DRAG_SLIDER_RIGHT);
                        return true;
                    }
                    break;
                case DRAG_SLIDER_LEFT:
                case DRAG_SLIDER_RIGHT:
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            switch (state) {
                case DRAG_SLIDER_LEFT:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (sliderL.performDrop(touchPoint)) {
                        setState(State.COMPLETE_SLIDER_LEFT);
                    } else {
                        setState(State.NORMAL);
                    }
                    return true;
                case DRAG_SLIDER_RIGHT:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (sliderR.performDrop(touchPoint)) {
                        setState(State.COMPLETE_SLIDER_RIGHT);
                    } else {
                        setState(State.NORMAL);
                    }
                    return true;
                case NORMAL:
            }
            performClick();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Point sliderCenter = new Point((int) (touchPoint.x - touchGap), contentPlace.centerY());
            switch (state) {
                case DRAG_SLIDER_LEFT:
                    if (sliderL.position.centerX() > sliderCenter.x) {
                        sliderCenter.x = sliderL.position.centerX();
                        if (touchGap > 0) touchGap = Math.max(touchPoint.x, sliderL.position.left) - sliderCenter.x;
                    } else if (sliderL.target.centerX() < sliderCenter.x) {
                        sliderCenter.x = sliderL.target.centerX();
                        touchGap = 0;
                    }
                    sliderL.performMove(sliderCenter);
                    invalidate();
                    return true;
                case DRAG_SLIDER_RIGHT:
                    if (sliderR.position.centerX() < sliderCenter.x) {
                        sliderCenter.x = sliderR.position.centerX();
                        if (touchGap < 0) touchGap = Math.min(touchPoint.x, sliderR.position.right) - sliderCenter.x;
                    } else if (sliderR.target.centerX() > sliderCenter.x) {
                        sliderCenter.x = sliderR.target.centerX();
                        touchGap = 0;
                    }
                    sliderR.performMove(sliderCenter);
                    invalidate();
                    return true;
                case NORMAL:
            }
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        sliderBg.drawIfPossible(canvas);
        sliderDesc.drawIfPossible(canvas);
        sliderLeftDesc.drawIfPossible(canvas);
        sliderRightDesc.drawIfPossible(canvas);
        sliderL.drawIfPossible(canvas);
        sliderR.drawIfPossible(canvas);
    }

    private final TypeEvaluator<Rect> RECT_TYPE_EVALUATOR = new RectEvaluator();

    private void move(Drawable target, long duration, Rect dist) {
        if (target != null) startAnimation(ObjectAnimator.ofObject(target, "bounds", RECT_TYPE_EVALUATOR, target.getBounds(), new Rect(dist)), duration);
    }

    private void show(Drawable target, long duration) {
        if (target != null) startAnimation(ObjectAnimator.ofInt(target, "alpha", target.getAlpha(), 255), duration);
    }

    private void hide(Drawable target, long duration) {
        if (target != null) startAnimation(ObjectAnimator.ofInt(target, "alpha", target.getAlpha(), 0), duration);
    }

    private void startAnimation(ObjectAnimator animator, long duration) {
        animator.setDuration(duration);
        animator.setAutoCancel(true);
        animator.addUpdateListener(animatorUpdateListener);
        animator.start();
    }

    private static int dip(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, Resources.getSystem().getDisplayMetrics());
    }

    private static void debug(String message) {
        if (debugMode) Log.d("SlideActionView", message);
    }
}
