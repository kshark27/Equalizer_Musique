/*
 * Musique - Music player/converter for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oucho.musicplayer.view.verticalseekbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;

import org.oucho.musicplayer.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VerticalSeekBar extends AppCompatSeekBar {
    public static final int ROTATION_ANGLE_CW_90 = 90;
    public static final int ROTATION_ANGLE_CW_270 = 270;

    private boolean mIsDragging;
    private Method mMethodSetProgressFromUser;
    private int mRotationAngle = ROTATION_ANGLE_CW_90;

    public VerticalSeekBar(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        ViewCompat.setLayoutDirection(this, ViewCompat.LAYOUT_DIRECTION_LTR);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar, defStyleAttr, 0);
            final int rotationAngle = typedArray.getInteger(R.styleable.VerticalSeekBar_seekBarRotation, 0);
            if (isValidRotationAngle(rotationAngle)) {
                mRotationAngle = rotationAngle;
            }
            typedArray.recycle();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (useViewRotation()) {
            return onTouchEventUseViewRotation(event);
        } else {
            return onTouchEventTraditionalRotation(event);
        }
    }

    private boolean onTouchEventTraditionalRotation(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag(true);
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    attemptClaimDrag(false);
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private boolean onTouchEventUseViewRotation(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);

        if (handled) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    attemptClaimDrag(true);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    attemptClaimDrag(false);
                    break;
            }
        }

        return handled;
    }

    private void trackTouchEvent(MotionEvent event) {
        final int paddingLeft = super.getPaddingLeft();
        final int paddingRight = super.getPaddingRight();
        final int height = getHeight();

        final int available = height - paddingLeft - paddingRight;
        int y = (int) event.getY();

        final float scale;
        float value = 0;

        switch (mRotationAngle) {
            case ROTATION_ANGLE_CW_90:
                value = y - paddingLeft;
                break;
            case ROTATION_ANGLE_CW_270:
                value = (height - paddingLeft) - y;
                break;
        }

        if (value < 0 || available == 0) {
            scale = 0.0f;
        } else if (value > available) {
            scale = 1.0f;
        } else {
            scale = value / (float) available;
        }

        final int max = getMax();
        final float progress = scale * max;

        _setProgressFromUser((int) progress, true);
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag(boolean active) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(active);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    private void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is
     * canceled.
     */
    private void onStopTrackingTouch() {
        mIsDragging = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            final boolean handled;
            int direction = 0;

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    direction = (mRotationAngle == ROTATION_ANGLE_CW_90) ? 1 : -1;
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    direction = (mRotationAngle == ROTATION_ANGLE_CW_270) ? 1 : -1;
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    // move view focus to previous/next view
                    return false;
                default:
                    handled = false;
                    break;
            }

            if (handled) {
                final int keyProgressIncrement = getKeyProgressIncrement();
                int progress = getProgress();

                progress += (direction * keyProgressIncrement);

                if (progress >= 0 && progress <= getMax()) {
                    _setProgressFromUser(progress, true);
                }

                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        if (!useViewRotation()) {
            refreshThumb();
        }
    }

    private synchronized void _setProgressFromUser(int progress, boolean fromUser) {
        if (mMethodSetProgressFromUser == null) {
            try {
                Method m;
                m = ProgressBar.class.getDeclaredMethod("setProgress", int.class, boolean.class);
                m.setAccessible(true);
                mMethodSetProgressFromUser = m;
            } catch (NoSuchMethodException ignored) {
            }
        }

        if (mMethodSetProgressFromUser != null) {
            try {
                mMethodSetProgressFromUser.invoke(this, progress, fromUser);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {
            }
        } else {
            super.setProgress(progress);
        }
        refreshThumb();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (useViewRotation()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);

            final ViewGroup.LayoutParams lp = getLayoutParams();

            if (isInEditMode() && (lp != null) && (lp.height >= 0)) {
                setMeasuredDimension(super.getMeasuredHeight(), lp.height);
            } else {
                setMeasuredDimension(super.getMeasuredHeight(), super.getMeasuredWidth());
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (useViewRotation()) {
            super.onSizeChanged(w, h, oldw, oldh);
        } else {
            super.onSizeChanged(h, w, oldh, oldw);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (!useViewRotation()) {
            switch (mRotationAngle) {
                case ROTATION_ANGLE_CW_90:
                    canvas.rotate(90);
                    canvas.translate(0, -super.getWidth());
                    break;
                case ROTATION_ANGLE_CW_270:
                    canvas.rotate(-90);
                    canvas.translate(-super.getHeight(), 0);
                    break;
            }
        }

        super.onDraw(canvas);
    }

    public int getRotationAngle() {
        return mRotationAngle;
    }

    // refresh thumb position
    private void refreshThumb() {
        onSizeChanged(super.getWidth(), super.getHeight(), 0, 0);
    }

    boolean useViewRotation() {
        final boolean inEditMode = isInEditMode();
        return !inEditMode;
    }

    private static boolean isValidRotationAngle(int angle) {
        return (angle == ROTATION_ANGLE_CW_90 || angle == ROTATION_ANGLE_CW_270);
    }
}
