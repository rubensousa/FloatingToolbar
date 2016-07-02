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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@CoordinatorLayout.DefaultBehavior(FloatingToolbar.Behavior.class)
public class FloatingToolbar extends LinearLayoutCompat implements View.OnClickListener,
        View.OnLongClickListener, FloatingAnimator.FloatingAnimatorListener {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    @MenuRes
    private int mMenuRes;

    @DrawableRes
    private int mItemBackground;

    private FloatingScrollListener mScrollListener;
    private RecyclerView mRecyclerView;
    private AppBarLayout mAppBar;
    private FloatingActionButton mFab;
    private View mCustomView;
    private Menu mMenu;
    private boolean mMorphed;
    private boolean mMorphing;
    private boolean mHandleFabClick;
    private boolean mShowToast;
    private Toast mToast;
    private ItemClickListener mClickListener;
    private LinearLayoutCompat mMenuLayout;
    private FloatingAnimator mAnimator;

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

        mScrollListener = new FloatingScrollListener(this);
        mShowToast = a.getBoolean(R.styleable.FloatingToolbar_floatingToastOnLongClick, true);
        mHandleFabClick = a.getBoolean(R.styleable.FloatingToolbar_floatingHandleFabClick, true);
        mItemBackground = a.getResourceId(R.styleable.FloatingToolbar_floatingItemBackground,
                outValue.resourceId);
        mMenuRes = a.getResourceId(R.styleable.FloatingToolbar_floatingMenu, 0);

        int customView = a.getResourceId(R.styleable.FloatingToolbar_floatingCustomView, 0);

        if (customView != 0) {
            mCustomView = LayoutInflater.from(context).inflate(customView, this, true);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mAnimator = new FloatingAnimatorImpl(this);
        } else {
            mAnimator = new FloatingAnimatorLollipopImpl(this);
        }

        if (mCustomView != null) {
            mAnimator.setContentView(mCustomView);
        }

        // Set elevation to 6dp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(dpToPixels(context, 6));
        }

        if (mMenuRes != 0 && customView == 0) {
            mMenuLayout = new LinearLayoutCompat(context, attrs, defStyleAttr);

            LayoutParams layoutParams
                    = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            mMenuLayout.setId(genViewId());
            addView(mMenuLayout, layoutParams);
            addMenuItems();
            mAnimator.setContentView(mMenuLayout);
        }

        if (!isInEditMode()) {
            setVisibility(View.INVISIBLE);
        }

        a.recycle();

        setOrientation(HORIZONTAL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mAnimator.updateDelay();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAppBar != null) {
            mAppBar.removeOnOffsetChangedListener(mAnimator);
        }
        super.onDetachedFromWindow();
    }

    public boolean isShowing() {
        return mMorphed;
    }

    public void handleFabClick(boolean handle) {
        mHandleFabClick = handle;
    }

    public boolean isHandlingFabClick() {
        return mHandleFabClick;
    }

    public void setClickListener(ItemClickListener listener) {
        mClickListener = listener;
    }

    public View getCustomView() {
        return mCustomView;
    }

    public Menu getMenu() {
        return mMenu;
    }

    public void setCustomView(View view) {
        removeAllViews();
        mCustomView = view;
        mAnimator.setContentView(mCustomView);
        addView(view);
    }

    public void setMenu(@MenuRes int menuRes) {
        mMenu = new MenuBuilder(getContext());
        new SupportMenuInflater(getContext()).inflate(menuRes, mMenu);
        setMenu(mMenu);
    }

    public void setMenu(Menu menu) {
        mMenu = menu;
        mMenuLayout.removeAllViews();
        addMenuItems();
        mAnimator.setContentView(mMenuLayout);
    }

    public void attachAppBarLayout(AppBarLayout appbar) {
        mAppBar = appbar;
        mAppBar.addOnOffsetChangedListener(mAnimator);
        mAnimator.setAppBarLayout(mAppBar);
    }

    public void detachAppBarLayout() {
        mAppBar.removeOnOffsetChangedListener(mAnimator);
        mAnimator.setAppBarLayout(null);
        mAppBar = null;
    }

    public void attachFab(FloatingActionButton fab) {
        mFab = fab;
        mAnimator.setFab(mFab);
        mAnimator.setFloatingAnimatorListener(this);

        if (mHandleFabClick) {
            mFab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mMorphed && mHandleFabClick) {
                        show();
                    }
                }
            });
        }

        mFab.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mAnimator.setFabOriginalY(top);
                mAnimator.setFabOriginalX(left);
                mAnimator.setFabNewY(top);
                mFab.removeOnLayoutChangeListener(this);
            }
        });
    }

    public void detachFab() {
        mAnimator.setFab(null);
        mFab = null;
    }

    public void attachRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    public void detachRecyclerView() {
        mRecyclerView.removeOnScrollListener(mScrollListener);
        mRecyclerView = null;
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
        mAnimator.show();
    }

    public void hide() {
        if (mFab == null) {
            throw new IllegalStateException("FloatingActionButton not attached." +
                    "Please, use attachFab(FloatingActionButton fab).");
        }

        if (mMorphed && !mMorphing) {
            mMorphed = false;
            mMorphing = true;
            mAnimator.hide();
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
            if (item.isVisible()) {
                AppCompatImageButton imageButton = new AppCompatImageButton(getContext());
                //noinspection ResourceType
                imageButton.setId(item.getItemId() == Menu.NONE ? genViewId() : item.getItemId());
                imageButton.setBackgroundResource(mItemBackground);
                imageButton.setImageDrawable(item.getIcon());
                imageButton.setOnClickListener(this);
                imageButton.setOnLongClickListener(this);
                imageButton.setTag(item);
                mMenuLayout.addView(imageButton, layoutParams);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!mMorphed || mMorphing) {
            return;
        }

        hide();

        if (mClickListener != null) {
            MenuItem item = (MenuItem) v.getTag();
            mClickListener.onItemClick(item);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mMorphed || mMorphing) {
            return false;
        }

        if (mClickListener != null) {
            MenuItem item = (MenuItem) v.getTag();
            if (mShowToast) {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(getContext(), item.getTitle(), Toast.LENGTH_SHORT);
                mToast.setGravity(Gravity.BOTTOM, 0, (int) (getHeight() * 1.25f));
                mToast.show();
            }
            mClickListener.onItemLongClick(item);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onAnimationFinished() {
        mMorphing = false;
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
        public boolean layoutDependsOn(CoordinatorLayout parent, FloatingToolbar child,
                                       View dependency) {
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

        @Override
        public void onDependentViewRemoved(CoordinatorLayout parent, FloatingToolbar child,
                                           View dependency) {
            if (dependency instanceof Snackbar.SnackbarLayout) {
                updateTranslationForSnackbar(parent, child);
            }
        }

        private void updateTranslationForSnackbar(CoordinatorLayout parent,
                                                  final FloatingToolbar layout) {

            final float targetTransY = getTranslationYForSnackbar(parent, layout);
            if (mTranslationY == targetTransY) {
                return;
            }

            final float currentTransY = ViewCompat.getTranslationY(layout);

            if (mTranslationYAnimator != null && mTranslationYAnimator.isRunning()) {
                mTranslationYAnimator.cancel();
            }

            if (layout.isShowing()
                    && Math.abs(currentTransY - targetTransY) > (layout.getHeight() * 0.667f)) {
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
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        //noinspection deprecation
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    setVisibility(View.VISIBLE);
                    mFab.setVisibility(View.INVISIBLE);
                    mMorphed = true;
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

    static float dpToPixels(Context context, int dp) {
        return context.getResources().getDisplayMetrics().densityDpi
                / DisplayMetrics.DENSITY_DEFAULT * dp;
    }

}
