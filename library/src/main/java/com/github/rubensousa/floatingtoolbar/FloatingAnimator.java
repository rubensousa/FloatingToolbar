/*
 * Copyright 2016 Rúben Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rubensousa.floatingtoolbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

abstract class FloatingAnimator implements AppBarLayout.OnOffsetChangedListener {

    public static final int DELAY_MIN_WIDTH = 300;
    public static final int DELAY_MAX_WIDTH = 900;
    public static final int DELAY_MAX = 150;
    public static final int FAB_MORPH_DURATION = 200;
    public static final int FAB_UNMORPH_DURATION = 200;
    public static final int FAB_UNMORPH_DELAY = 300;
    public static final int CIRCULAR_REVEAL_DURATION = 300;
    public static final int CIRCULAR_UNREVEAL_DURATION = 200;
    public static final int CIRCULAR_REVEAL_DELAY = 50;
    public static final int CIRCULAR_UNREVEAL_DELAY = 150;
    public static final int TOOLBAR_UNREVEAL_DELAY = 200;
    public static final int MENU_ANIMATION_DELAY = 200;
    public static final int MENU_ANIMATION_DURATION = 300;

    private float mAppbarOffset;
    private AppBarLayout mAppBar;
    private FloatingActionButton mFab;
    private FloatingToolbar mToolbar;
    private View mRootView;
    private View mContentView;
    private long mDelay;
    private FloatingAnimatorListener mAnimationListener;

    public FloatingAnimator(FloatingToolbar toolbar) {
        mToolbar = toolbar;
        mRootView = mToolbar.getRootView();
    }

    public void setFab(FloatingActionButton fab) {
        mFab = fab;
    }

    public void setAppBarLayout(AppBarLayout appBarLayout) {
        mAppBar = appBarLayout;
    }

    public AppBarLayout getAppBar() {
        return mAppBar;
    }

    public FloatingActionButton getFab() {
        return mFab;
    }

    public FloatingToolbar getFloatingToolbar() {
        return mToolbar;
    }

    public void setFloatingAnimatorListener(FloatingAnimatorListener listener) {
        mAnimationListener = listener;
    }

    public FloatingAnimatorListener getAnimationListener() {
        return mAnimationListener;
    }

    public void setContentView(View contentView) {
        mContentView = contentView;
    }

    public float getAppBarOffset() {
        return mAppbarOffset;
    }

    public long getDelay() {
        return mDelay;
    }

    public View getRootView() {
        return mRootView;
    }

    public void show() {
        float fabEndX = mFab.getLeft() > mRootView.getWidth() / 2f ?
                mFab.getLeft() - mFab.getWidth() : mFab.getLeft() + mFab.getWidth();

        // Place view a bit closer to the fab
        mToolbar.setX(fabEndX - mToolbar.getWidth() / 2f + mFab.getWidth());

        // If there's a snackbar being shown, we need to change the translationY
        mToolbar.setTranslationY(mFab.getTranslationY());

        // Start showing content view
        if (mContentView != null) {
            mContentView.setAlpha(0f);
            mContentView.setScaleX(0.7f);
            mContentView.animate().alpha(1).scaleX(1f)
                    .setDuration(MENU_ANIMATION_DURATION + mDelay)
                    .setStartDelay(MENU_ANIMATION_DELAY + mDelay)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAnimationListener.onAnimationFinished();
                            mContentView.animate().setListener(null);
                        }
                    });
        }

        // Move FloatingToolbar to the original position
        mToolbar.animate().x(mToolbar.getLeft()).setStartDelay(CIRCULAR_REVEAL_DELAY + mDelay)
                .setDuration((long) (CIRCULAR_REVEAL_DURATION) + mDelay)
                .setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void hide() {

        mToolbar.animate().x(mFab.getLeft() - mToolbar.getWidth() / 2f)
                .setDuration(CIRCULAR_UNREVEAL_DURATION + mDelay)
                .setStartDelay(TOOLBAR_UNREVEAL_DELAY + mDelay)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        if (mContentView != null) {
            mContentView.animate().alpha(0f).scaleX(0.7f)
                    .setStartDelay(CIRCULAR_UNREVEAL_DELAY + mDelay)
                    .setDuration((MENU_ANIMATION_DURATION / 2) + mDelay);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // Fab can be a bit higher than the AppBar when this last covers the whole screen.
        mAppbarOffset = verticalOffset;
    }

    /**
     * Calculate a delay that depends on the screen width so that animations don't happen too quick
     * on larger phones or tablets
     * <p>
     * Base is 300dp.
     * <p>
     * A root view with 300dp as width has 0 delay
     * <p>
     * The max width is 1200dp, with a max delay of 200 ms
     */
    public void updateDelay() {
        float minWidth = FloatingToolbar.dpToPixels(mToolbar.getContext(), DELAY_MIN_WIDTH);
        float maxWidth = FloatingToolbar.dpToPixels(mToolbar.getContext(), DELAY_MAX_WIDTH);
        float diff = maxWidth - minWidth;

        int width = mToolbar.getWidth();

        if (width == 0 || width < minWidth) {
            mDelay = 0;
            return;
        }

        if (width > maxWidth) {
            mDelay = DELAY_MAX;
            return;
        }

        mDelay = (long) (DELAY_MAX / diff * (width - minWidth));
    }

    interface FloatingAnimatorListener {
        void onAnimationFinished();
    }
}
