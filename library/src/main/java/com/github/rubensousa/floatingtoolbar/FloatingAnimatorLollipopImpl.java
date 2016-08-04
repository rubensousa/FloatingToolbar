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
import android.annotation.TargetApi;
import android.graphics.Path;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;


class FloatingAnimatorLollipopImpl extends FloatingAnimator {

    public FloatingAnimatorLollipopImpl(FloatingToolbar toolbar) {
        super(toolbar);
    }

    @TargetApi(21)
    @Override
    public void show() {
        super.show();
        ObjectAnimator anim = ObjectAnimator.ofFloat(getFab(), View.X, View.Y, createPath(true));
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_MORPH_DURATION + getDelay());
        anim.start();

        // Animate FAB elevation to 8dp
        anim = ObjectAnimator.ofFloat(getFab(), View.TRANSLATION_Z,
                FloatingToolbar.dpToPixels(getFab().getContext(), 2));
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_MORPH_DURATION + getDelay());
        anim.start();

        // Create circular reveal
        int width = getFloatingToolbar().getWidth();
        int height = getFloatingToolbar().getHeight();
        Animator toolbarReveal = ViewAnimationUtils.createCircularReveal(getFloatingToolbar(),
                width / 2, height / 2, (float) getFab().getWidth() / 2f,
                (float) (Math.hypot(width / 2, height / 2)));

        toolbarReveal.setDuration(CIRCULAR_REVEAL_DURATION + getDelay());
        toolbarReveal.setTarget(this);
        toolbarReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                getFab().setVisibility(View.INVISIBLE);
                getFloatingToolbar().setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                getAnimationListener().onAnimationFinished();
            }
        });

        toolbarReveal.setInterpolator(new AccelerateInterpolator());
        toolbarReveal.setStartDelay(CIRCULAR_REVEAL_DELAY + getDelay());
        toolbarReveal.start();


        // Animate FloatingToolbar elevation to 8dp
        anim = ObjectAnimator.ofFloat(getFloatingToolbar(), View.TRANSLATION_Z,
                FloatingToolbar.dpToPixels(getFab().getContext(), 2));
        anim.setDuration(CIRCULAR_REVEAL_DURATION + getDelay());
        anim.setStartDelay(CIRCULAR_REVEAL_DELAY + getDelay());
        anim.start();
    }

    @TargetApi(21)
    @Override
    public void hide() {
        super.hide();

        // A snackbar might have appeared, so we need to update the fab position again
        if (getAppBar() != null) {
            getFab().setTranslationY(getFloatingToolbar().getTranslationY());
        }

        ObjectAnimator anim = ObjectAnimator.ofFloat(getFab(), View.X, View.Y, createPath(false));
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_UNMORPH_DURATION + getDelay());
        anim.setStartDelay(FAB_UNMORPH_DELAY + getDelay());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Make sure the fab goes to the right place after the animation ends
                // when the Appbar is attached
                if (getAppBar() != null && getFab().getY() != getFab().getTop()) {
                    getFab().setAlpha(0f);
                    getFab().setY(getFab().getTop());
                    getFab().animate().alpha(1f)
                            .setDuration(200)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                }
                getAnimationListener().onAnimationFinished();
            }
        });
        anim.start();


        // Animate FAB elevation back to 6dp
        anim = ObjectAnimator.ofFloat(getFab(), View.TRANSLATION_Z, 0);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_UNMORPH_DURATION + getDelay());
        anim.setStartDelay(FAB_UNMORPH_DELAY + getDelay());
        anim.start();

        int width = getFloatingToolbar().getWidth();
        int height = getFloatingToolbar().getHeight();

        Animator toolbarReveal = ViewAnimationUtils.createCircularReveal(getFloatingToolbar(),
                width / 2, height / 2, (float) (Math.hypot(width / 2, height / 2)),
                (float) getFab().getWidth() / 2f);


        toolbarReveal.setTarget(this);
        toolbarReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getFloatingToolbar().setVisibility(View.INVISIBLE);
                getFab().setVisibility(View.VISIBLE);
            }
        });

        toolbarReveal.setDuration(CIRCULAR_UNREVEAL_DURATION + getDelay());
        toolbarReveal.setInterpolator(new AccelerateInterpolator());
        toolbarReveal.setStartDelay(CIRCULAR_UNREVEAL_DELAY + getDelay());
        toolbarReveal.start();

        // Animate FloatingToolbar animation back to 6dp
        anim = ObjectAnimator.ofFloat(getFloatingToolbar(), View.TRANSLATION_Z, 0);
        anim.setDuration(CIRCULAR_UNREVEAL_DURATION + getDelay());
        anim.setStartDelay(CIRCULAR_UNREVEAL_DELAY + getDelay());
        anim.start();
    }

    private Path createPath(boolean show) {
        float fabOriginalX = getFab().getLeft();
        float x2;
        float y2 = getFloatingToolbar().getY();
        float endX;
        float endY;

        if (!show) {
            endX = fabOriginalX;
        } else {
            if (fabOriginalX > getRootView().getWidth() / 2f) {
                endX = fabOriginalX - getFab().getWidth();
            } else {
                endX = fabOriginalX + getFab().getWidth();
            }
        }

        Path path = new Path();
        path.moveTo(getFab().getX(), getFab().getY());

        if (fabOriginalX > getRootView().getWidth() / 2f) {
            x2 = fabOriginalX - getFab().getWidth() / 4f;
        } else {
            x2 = fabOriginalX + getFab().getWidth() / 4f;
        }

        if (show) {
            endY = getFloatingToolbar().getY();
        } else {
            endY = getFab().getTop() + getFloatingToolbar().getTranslationY();
        }

        path.quadTo(x2, y2, endX, endY);

        return path;
    }
}
