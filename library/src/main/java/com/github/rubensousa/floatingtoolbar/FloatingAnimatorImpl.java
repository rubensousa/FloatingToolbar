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
        getFloatingToolbar().setScaleX(0f);

        float endFabX;

        if (getFabOriginalX() > rootWidth / 2f) {
            endFabX = rootWidth / 2f + (getFabOriginalX() - rootWidth / 2f) / 4f;
        } else {
            endFabX = rootWidth / 2f - (getFabOriginalX() - rootWidth / 2f) / 4f;
        }


        PropertyValuesHolder xProperty = PropertyValuesHolder.ofFloat(View.X, endFabX);
        PropertyValuesHolder yProperty
                = PropertyValuesHolder.ofFloat(View.Y, getFabOriginalY() * 1.05f);
        PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(View.SCALE_X, 0);
        PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(getFab(), xProperty,
                yProperty, scaleXProperty, scaleYProperty);
        animator.setDuration(FAB_MORPH_DURATION);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.start();

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f);
        objectAnimator.setDuration(CIRCULAR_REVEAL_DURATION);
        objectAnimator.setStartDelay(CIRCULAR_REVEAL_DELAY);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                getFloatingToolbar().setVisibility(View.VISIBLE);
                getFab().setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                getAnimationListener().onAnimationFinished();
            }
        });
        objectAnimator.start();
    }

    @Override
    public void hide() {
        super.hide();
        ViewCompat.animate(getFab())
                .x(getFabOriginalX())
                .y(getFabNewY() + getFloatingToolbar().getTranslationY())
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(FAB_UNMORPH_DELAY)
                .setDuration(FAB_UNMORPH_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(View view) {
                        super.onAnimationStart(view);
                        getFab().setVisibility(View.VISIBLE);
                        ViewCompat.animate(getFab()).setListener(null);
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
                        super.onAnimationEnd(view);
                        getFloatingToolbar().setVisibility(View.INVISIBLE);
                        ViewCompat.animate(getFloatingToolbar()).setListener(null);
                        getAnimationListener().onAnimationFinished();
                    }
                });
    }
}
