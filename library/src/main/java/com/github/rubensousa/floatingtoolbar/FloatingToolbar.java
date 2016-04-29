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
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@CoordinatorLayout.DefaultBehavior(FloatingToolbar.Behavior.class)
public class FloatingToolbar extends LinearLayoutCompat implements View.OnClickListener,
        View.OnLongClickListener {

    private static final int FAB_MORPH_DURATION = 200;
    private static final int FAB_UNMORPH_DURATION = 200;
    private static final int FAB_UNMORPH_DELAY = 300;
    private static final int CIRCULAR_REVEAL_DURATION = 300;
    private static final int CIRCULAR_UNREVEAL_DURATION = 200;
    private static final int CIRCULAR_REVEAL_DELAY = 125;
    private static final int CIRCULAR_UNREVEAL_DELAY = 150;
    private static final int MENU_ANIMATION_DELAY = 200;
    private static final int MENU_ANIMATION_DURATION = 300;
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private FloatingActionButton mFab;
    private Menu mMenu;
    private View mCustomView;

    @MenuRes
    private int mMenuRes;

    @DrawableRes
    private int mItemBackground;

    private boolean mMorphed;
    private boolean mMorphing;
    private View mRoot;
    private float mFabOriginalX;
    private float mFabOriginalY;
    private float mOriginalX;
    private ItemClickListener mClickListener;
    private LinearLayoutCompat mMenuLayout;

    public FloatingToolbar(Context context) {
        this(context, null, 0);
    }

    public FloatingToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingToolbar, 0, 0);

        TypedValue outValue = new TypedValue();

        // Set colorAccent as default color
        if (getBackground() == null) {
            getContext().getTheme().resolveAttribute(R.attr.colorAccent, outValue, true);
            setBackgroundResource(outValue.resourceId);
        }

        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);

        mItemBackground = a.getResourceId(R.styleable.FloatingToolbar_floatingItemBackground, outValue.resourceId);

        mMenuRes = a.getResourceId(R.styleable.FloatingToolbar_floatingMenu, 0);

        int customView = a.getResourceId(R.styleable.FloatingToolbar_floatingCustomView, 0);

        if (customView != 0) {
            mCustomView = LayoutInflater.from(context).inflate(customView, this, true);
        }

        a.recycle();

        if (customView == 0) {
            mMenuLayout = new LinearLayoutCompat(context, attrs, defStyleAttr);

            LayoutParams layoutParams
                    = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            mMenuLayout.setId(genViewId());
            addView(mMenuLayout, layoutParams);
        }

        if (!isInEditMode()) {
            setVisibility(View.INVISIBLE);
        }

        if (mMenuRes != 0 && customView == 0) {
            addMenuItems();
        }

        // Set elevation to 6dp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(dpToPixels(6));
        }

        setOrientation(HORIZONTAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mRoot = getRootView();

        if (mFab != null && mFabOriginalX == 0 && mFabOriginalY == 0) {
            mFabOriginalY = mFab.getY();
            mFabOriginalX = mFab.getX();
            mOriginalX = getX();
        }
    }

    public boolean isShowing() {
        return mMorphed;
    }

    public void setClickListener(ItemClickListener listener) {
        mClickListener = listener;
    }

    public View getCustomView() {
        return mCustomView;
    }

    public void setCustomView(View view) {
        removeAllViews();
        mCustomView = view;
        addView(view);
    }

    public void setMenu(@MenuRes int menuRes) {
        removeAllViews();
        Menu menu = new MenuBuilder(getContext());
        new SupportMenuInflater(getContext()).inflate(menuRes, menu);
        setMenu(menu);
    }

    public void setMenu(Menu menu) {
        mMenu = menu;
        removeAllViews();
        addMenuItems();
    }

    public void attachFab(FloatingActionButton fab) {
        mFab = fab;
        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMorphed) {
                    show();
                }
            }
        });
    }

    public void attachRecyclerView(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hide();
                }
            }
        });
    }

    public void show() {
        if (mFab == null) {
            throw new IllegalStateException("FloatingActionButton not attached." +
                    "Please, use attachFab(FloatingActionButton fab).");
        }

        if (mMorphing) {
            return;
        }

        mMorphed = true;
        mMorphing = true;

        if (mMenuLayout != null) {
            mMenuLayout.setAlpha(0f);
            mMenuLayout.setScaleX(0.7f);
        }

        if (mCustomView != null) {
            mCustomView.setAlpha(0f);
            mCustomView.setScaleX(0.7f);
        }

        /**
         * Place view a bit closer to the fab
         */
        float startRevealX;

        if (mFabOriginalX > getWidth() / 2f) {
            startRevealX = mOriginalX + (mFabOriginalX - mOriginalX) / 4f;
        } else {
            startRevealX = mOriginalX - (mFabOriginalX - mOriginalX) / 4f;
        }

        setX(startRevealX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showLollipopImpl();
        } else {
            showDefaultImpl();
        }

        if (mMenuLayout != null) {
            mMenuLayout.animate().alpha(1).scaleX(1f)
                    .setDuration(MENU_ANIMATION_DURATION)
                    .setStartDelay(MENU_ANIMATION_DELAY)
                    .setInterpolator(new AccelerateDecelerateInterpolator());
        }
        if (mCustomView != null) {
            mCustomView.animate().alpha(1).scaleX(1)
                    .setDuration(MENU_ANIMATION_DURATION)
                    .setStartDelay(MENU_ANIMATION_DELAY)
                    .setInterpolator(new AccelerateDecelerateInterpolator());
        }

        /**
         * Move FloatingToolbar to the original position
         */
        animate().x(mOriginalX).setStartDelay(CIRCULAR_REVEAL_DELAY)
                .setDuration(CIRCULAR_REVEAL_DURATION / 2)
                .setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void hide() {
        if (mFab == null) {
            throw new IllegalStateException("FloatingActionButton not attached." +
                    "Please, use attachFab(FloatingActionButton fab).");
        }

        if (mMorphed && !mMorphing) {
            mMorphed = false;

            float x = getX();

            if (mFabOriginalX > x) {
                x = x + (mFabOriginalX - x) / 4f;
            } else {
                x = x - (mFabOriginalX - x) / 4f;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                hideLollipopImpl();
            } else {
                hideDefaultImpl();
            }

            animate().x(x).setDuration(CIRCULAR_UNREVEAL_DURATION)
                    .setStartDelay(CIRCULAR_UNREVEAL_DELAY)
                    .setInterpolator(new AccelerateDecelerateInterpolator());

            if (mMenuLayout != null) {
                mMenuLayout.animate().alpha(0f).scaleX(0.7f)
                        .setStartDelay(CIRCULAR_UNREVEAL_DELAY)
                        .setDuration(MENU_ANIMATION_DURATION / 2);
            }
            if (mCustomView != null) {
                mCustomView.animate().alpha(0f).scaleX(0.7f)
                        .setStartDelay(CIRCULAR_UNREVEAL_DELAY)
                        .setDuration(CIRCULAR_UNREVEAL_DURATION);
            }
        }
    }

    private void addMenuItems() {

        if (mMenu == null) {
            mMenu = new MenuBuilder(getContext());
            new SupportMenuInflater(getContext()).inflate(mMenuRes, mMenu);
        }

        LinearLayoutCompat.LayoutParams layoutParams
                = new LinearLayoutCompat.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT, 1);

        setWeightSum(mMenu.size());

        for (int i = 0; i < mMenu.size(); i++) {
            MenuItem item = mMenu.getItem(i);
            AppCompatImageButton imageButton = new AppCompatImageButton(getContext());
            imageButton.setId(genViewId());
            imageButton.setBackgroundResource(mItemBackground);
            imageButton.setImageDrawable(item.getIcon());
            imageButton.setOnClickListener(this);
            imageButton.setOnLongClickListener(this);
            mMenuLayout.addView(imageButton, layoutParams);
        }

    }

    @Override
    public void onClick(View v) {
        if (!mMorphed) {
            return;
        }

        hide();

        if (mClickListener != null) {
            MenuItem item = mMenu.getItem(mMenuLayout.indexOfChild(v));
            mClickListener.onItemClick(item);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mMorphed) {
            return false;
        }
        if (mClickListener != null) {
            MenuItem item = mMenu.getItem(mMenuLayout.indexOfChild(v));
            mClickListener.onItemLongClick(item);
            return true;
        } else {
            return false;
        }
    }

    private void showDefaultImpl() {
        int rootWidth = mRoot.getWidth();
        setScaleX(0f);

        float endFabX;

        if (mFabOriginalX > rootWidth / 2f) {
            endFabX = rootWidth / 2f + (mFabOriginalX - rootWidth / 2f) / 4f;
        } else {
            endFabX = rootWidth / 2f - (mFabOriginalX - rootWidth / 2f) / 4f;
        }

        if (mFab != null) {
            PropertyValuesHolder xProperty = PropertyValuesHolder.ofFloat(X, endFabX);
            PropertyValuesHolder yProperty = PropertyValuesHolder.ofFloat(Y, mFabOriginalY * 1.05f);
            PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(SCALE_X, 0);
            PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(SCALE_Y, 0);

            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mFab, xProperty,
                    yProperty, scaleXProperty, scaleYProperty);
            animator.setDuration(FAB_MORPH_DURATION);
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
        }

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f);
        objectAnimator.setDuration(CIRCULAR_REVEAL_DURATION);
        objectAnimator.setStartDelay(CIRCULAR_REVEAL_DELAY);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                setVisibility(View.VISIBLE);
                mFab.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mMorphing = false;
            }
        });
        objectAnimator.start();
    }

    private void hideDefaultImpl() {
        ViewCompat.animate(mFab)
                .x(mFabOriginalX)
                .y(mFabOriginalY)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(FAB_UNMORPH_DELAY)
                .setDuration(FAB_UNMORPH_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(View view) {
                        super.onAnimationStart(view);
                        mFab.setVisibility(View.VISIBLE);
                        ViewCompat.animate(mFab).setListener(null);
                    }
                });

        ViewCompat.animate(this)
                .scaleX(0f)
                .setDuration(CIRCULAR_UNREVEAL_DURATION)
                .setStartDelay(CIRCULAR_UNREVEAL_DELAY)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new ViewPropertyAnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(View view) {
                        super.onAnimationEnd(view);
                        setVisibility(View.INVISIBLE);
                        ViewCompat.animate(FloatingToolbar.this).setListener(null);
                        mMorphing = false;
                    }
                });
    }

    @TargetApi(21)
    private void showLollipopImpl() {
        int rootWidth = mRoot.getWidth();

        float endFabX;
        float controlX;

        if (mFabOriginalX > rootWidth / 2f) {
            endFabX = rootWidth / 2f + (mFabOriginalX - rootWidth / 2f) / 4f;
            controlX = mFabOriginalX * 0.98f;
        } else {
            endFabX = rootWidth / 2f - (mFabOriginalX - rootWidth / 2f) / 4f;
            controlX = mFabOriginalX * 1.02f;
        }

        /**
         * Animate FAB movement
         */
        final Path path = new Path();
        path.moveTo(mFab.getX(), mFab.getY());
        final float x2 = controlX;
        final float y2 = getY();
        path.quadTo(x2, y2, endFabX, getY());
        ObjectAnimator anim = ObjectAnimator.ofFloat(mFab, View.X, View.Y, path);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_MORPH_DURATION);
        anim.start();

        /**
         * Fade FAB drawable
         */
        Drawable drawable = mFab.getDrawable();
        if (drawable != null) {
            anim = ObjectAnimator.ofPropertyValuesHolder(drawable,
                    PropertyValuesHolder.ofInt("alpha", 0));
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.setDuration((long) (FAB_MORPH_DURATION / 3f));
            anim.start();
        }

        /**
         * Animate FAB elevation to 8dp
         */
        anim = ObjectAnimator.ofFloat(mFab, View.TRANSLATION_Z, dpToPixels(2));
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_MORPH_DURATION);
        anim.start();

        /**
         * Create circular reveal
         */
        Animator toolbarReveal = ViewAnimationUtils.createCircularReveal(this, getWidth() / 2,
                getHeight() / 2, (float) mFab.getWidth() / 2f,
                (float) (Math.hypot(getWidth() / 2, getHeight() / 2)));

        toolbarReveal.setDuration(CIRCULAR_REVEAL_DURATION);
        toolbarReveal.setTarget(this);
        toolbarReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mFab.setVisibility(View.INVISIBLE);
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mMorphing = false;
            }
        });

        toolbarReveal.setInterpolator(new AccelerateInterpolator());
        toolbarReveal.setStartDelay(CIRCULAR_REVEAL_DELAY);
        toolbarReveal.start();

        /**
         * Animate FloatingToolbar elevation to 8dp
         */
        anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Z, dpToPixels(2));
        anim.setDuration(CIRCULAR_REVEAL_DURATION);
        anim.setStartDelay(CIRCULAR_REVEAL_DELAY);
        anim.start();
    }

    @TargetApi(21)
    private void hideLollipopImpl() {
        int rootWidth = mRoot.getWidth();

        float controlX;

        if (mFabOriginalX > rootWidth / 2f) {
            controlX = mFabOriginalX * 0.98f;
        } else {
            controlX = mFabOriginalX * 1.02f;
        }


        final Path path = new Path();
        path.moveTo(mFab.getX(), mFab.getY());
        final float x2 = controlX;
        final float y2 = getY();
        path.quadTo(x2, y2, mFabOriginalX, mFabOriginalY + getTranslationY());
        ObjectAnimator anim = ObjectAnimator.ofFloat(mFab, View.X, View.Y, path);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_UNMORPH_DURATION);
        anim.setStartDelay(FAB_UNMORPH_DELAY);
        anim.start();

        /**
         * Animate FAB elevation back to 6dp
         */
        anim = ObjectAnimator.ofFloat(mFab, View.TRANSLATION_Z, 0);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(FAB_UNMORPH_DURATION);
        anim.setStartDelay(FAB_UNMORPH_DELAY);
        anim.start();

        /**
         * Restore alpha of FAB drawable
         */
        Drawable drawable = mFab.getDrawable();
        if (drawable != null) {
            anim = ObjectAnimator.ofPropertyValuesHolder(drawable,
                    PropertyValuesHolder.ofInt("alpha", 255));
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.setDuration(FAB_UNMORPH_DURATION);
            anim.setStartDelay(FAB_UNMORPH_DELAY);
            anim.start();
        }

        Animator toolbarReveal = ViewAnimationUtils.createCircularReveal(this, getWidth() / 2,
                getHeight() / 2, (float) (Math.hypot(getWidth() / 2, getHeight() / 2)),
                (float) mFab.getWidth() / 2f);


        toolbarReveal.setTarget(this);
        toolbarReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setVisibility(View.INVISIBLE);
                mFab.setVisibility(View.VISIBLE);
                mMorphing = false;
            }
        });
        toolbarReveal.setDuration(CIRCULAR_UNREVEAL_DURATION);
        toolbarReveal.setInterpolator(new AccelerateInterpolator());
        toolbarReveal.setStartDelay(CIRCULAR_UNREVEAL_DELAY);
        toolbarReveal.start();

        /**
         * Animate FloatingToolbar animation back to 6dp
         */
        anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Z, 0);
        anim.setDuration(CIRCULAR_UNREVEAL_DURATION);
        anim.setStartDelay(CIRCULAR_UNREVEAL_DELAY);
        anim.start();
    }

    public interface ItemClickListener {
        void onItemClick(MenuItem item);

        void onItemLongClick(MenuItem item);
    }

    // FloatingActionButton.Behavior adapted
    public static class Behavior extends CoordinatorLayout.Behavior<FloatingToolbar> {

        private float mTranslationY;
        private ValueAnimator mTranslationYAnimator;

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, FloatingToolbar child, View dependency) {
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingToolbar child,
                                              View dependency) {

            if (dependency instanceof Snackbar.SnackbarLayout) {
                updateTranslationForSnackbar(parent, child);
            }
            return false;
        }

        private void updateTranslationForSnackbar(CoordinatorLayout parent, final FloatingToolbar layout) {

            final float targetTransY = getTranslationYForSnackbar(parent, layout);
            if (mTranslationY == targetTransY) {
                return;
            }

            final float currentTransY = ViewCompat.getTranslationY(layout);

            if (mTranslationYAnimator != null && mTranslationYAnimator.isRunning()) {
                mTranslationYAnimator.cancel();
            }

            if (Math.abs(currentTransY - targetTransY) > (layout.getHeight() * 0.667f)) {
                if (mTranslationYAnimator == null) {
                    mTranslationYAnimator = new ValueAnimator();
                    mTranslationYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    mTranslationYAnimator.addUpdateListener(
                            new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    ViewCompat.setTranslationY(layout, animator.getAnimatedFraction());
                                }
                            });
                }
                mTranslationYAnimator.setFloatValues(currentTransY, targetTransY);
                mTranslationYAnimator.start();
            } else {
                ViewCompat.setTranslationY(layout, targetTransY);
            }

            mTranslationY = targetTransY;
        }

        private float getTranslationYForSnackbar(CoordinatorLayout parent, FloatingToolbar layout) {
            float minOffset = 0;
            final List<View> dependencies = parent.getDependencies(layout);
            for (int i = 0, z = dependencies.size(); i < z; i++) {
                final View view = dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(layout, view)) {
                    minOffset = Math.min(minOffset,
                            ViewCompat.getTranslationY(view) - view.getHeight());
                }
            }

            return minOffset;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.morphed = mMorphed;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;

        super.onRestoreInstanceState(savedState.getSuperState());

        if (savedState.morphed) {
            post(new Runnable() {
                @Override
                public void run() {
                    mRoot = getRootView();
                    mOriginalX = getX();
                    mFabOriginalY = mFab.getY();
                    mFabOriginalX = mFab.getX();
                    show();
                }
            });
        }
    }

    static class SavedState extends BaseSavedState {

        public boolean morphed;

        public SavedState(Parcel source) {
            super(source);
            morphed = source.readByte() == 0x01;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (morphed ? 0x01 : 0x00));
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private float dpToPixels(int dp) {
        return Resources.getSystem().getDisplayMetrics().densityDpi
                / DisplayMetrics.DENSITY_DEFAULT * dp;
    }

    // Generate unique view ids to save state properly
    private int genViewId() {
        if (Build.VERSION.SDK_INT < 17) {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF)
                    newValue = 1;
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        } else {
            return View.generateViewId();
        }
    }

}