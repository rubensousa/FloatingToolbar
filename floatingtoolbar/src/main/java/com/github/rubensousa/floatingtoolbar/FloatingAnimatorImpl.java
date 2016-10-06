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
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

class FloatingAnimatorImpl extends FloatingAnimator {

    public FloatingAnimatorImpl(FloatingToolbar toolbar) {
        super(toolbar);
    }

    @Override
    public void show() {
        super.show();
        int rootWidth = getRootView().getWidth();
        float endFabX;

        if (getFab().getLeft() > rootWidth / 2f) {
            endFabX = getFab().getLeft() - getFab().getWidth();
        } else {
            endFabX = getFab().getLeft() + getFab().getWidth();
        }

        PropertyValuesHolder xProperty = PropertyValuesHolder.ofFloat(View.X, endFabX);
        PropertyValuesHolder yProperty
                = PropertyValuesHolder.ofFloat(View.Y, getFloatingToolbar().getY() * 0.95f);
        PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(View.SCALE_X, 0);
        PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(getFab(), xProperty,
                yProperty, scaleXProperty, scaleYProperty);
        animator.setDuration(FAB_MORPH_DURATION);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.start();

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(getFloatingToolbar(), "scaleX", 1f);
        objectAnimator.setDuration(CIRCULAR_REVEAL_DURATION);
        objectAnimator.setStartDelay(CIRCULAR_REVEAL_DELAY);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                getFloatingToolbar().setVisibility(View.VISIBLE);
                getFab().setVisibility(View.INVISIBLE);
            }
        });
        objectAnimator.start();
    }

    @Override
    public void hide() {
        super.hide();

        // A snackbar might have appeared, so we need to update the fab position again
        if (getAppBar() != null) {
            getFab().setY(getFloatingToolbar().getY());
        } else {
            getFab().setTranslationY(getFloatingToolbar().getTranslationY());
        }

        final int fabNewY = getFab().getTop();
        ViewCompat.animate(getFab())
                .x(getFab().getLeft())
                .y(fabNewY)
                .translationY(getFloatingToolbar().getTranslationY())
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(FAB_UNMORPH_DELAY)
                .setDuration(FAB_UNMORPH_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(View view) {
                        getFab().setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        // Make sure the fab goes to the right place after the animation ends
                        // when the Appbar is attached
                        if (getAppBar() != null && getFab().getVisibility() == View.INVISIBLE) {
                            getFab().show();
                        }
                        getAnimationListener().onAnimationFinished();
                    }
                });

        ViewCompat.animate(getFloatingToolbar())
                .scaleX(0f)
                .setDuration(CIRCULAR_UNREVEAL_DURATION)
                .setStartDelay(CIRCULAR_UNREVEAL_DELAY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(View view) {
                        getFloatingToolbar().setVisibility(View.INVISIBLE);
                        ViewCompat.animate(getFloatingToolbar()).setListener(null);
                    }
                });
    }
}
