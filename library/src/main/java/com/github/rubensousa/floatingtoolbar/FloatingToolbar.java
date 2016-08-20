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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import java.util.ArrayList;
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
    private List<MorphListener> mMorphListeners;
    private OnClickListener mViewClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mMorphed && mHandleFabClick) {
                show();
            }
        }
    };

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

        mMorphListeners = new ArrayList<>();
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

    /**
     * Check if the FloatingToolbar is being shown due a previous FloatingActionButton click
     * or a manual call to {@link #show() show()}
     *
     * @return true if the FloatingToolbar is being shown and the fab is hidden
     */
    public boolean isShowing() {
        return mMorphed;
    }

    /**
     * Control whether the FloatingToolbar should handle fab clicks or force manual calls
     * to {@link #show() show}
     *
     * @param handle true if this FloatingToolbar should be shown automatically on fab click
     */
    public void handleFabClick(boolean handle) {
        mHandleFabClick = handle;
        if (mHandleFabClick && mFab != null) {
            mFab.setOnClickListener(mViewClickListener);
        }
    }

    /**
     * @return true if the FloatingToolbar is being shown automatically
     * by handling FloatingActionButton clicks.
     */
    public boolean isHandlingFabClick() {
        return mHandleFabClick;
    }

    /**
     * Set a ItemClickListener that'll receive item click events from the View built from a Menu.
     *
     * @param listener Listener that'll receive item click events
     */
    public void setClickListener(ItemClickListener listener) {
        mClickListener = listener;
    }

    /**
     * @return The custom view associated to this FloatingToolbar, or null if there's none.
     */
    @Nullable
    public View getCustomView() {
        return mCustomView;
    }

    /**
     * @return The menu associated to this FloatingToolbar, or null if there's none
     */
    @Nullable
    public Menu getMenu() {
        return mMenu;
    }

    /**
     * Place a view inside this FlootingToolbar. It'll be animated automatically.
     *
     * @param view View to be shown inside this FloatingToolbar
     */
    public void setCustomView(View view) {
        removeAllViews();
        mCustomView = view;
        mAnimator.setContentView(mCustomView);
        addView(view);
    }

    /**
     * Set a menu from it's resource id.
     *
     * @param menuRes menu resource to be set
     */
    public void setMenu(@MenuRes int menuRes) {
        mMenu = new MenuBuilder(getContext());
        new SupportMenuInflater(getContext()).inflate(menuRes, mMenu);
        setMenu(mMenu);
    }

    /**
     * Set a menu that'll be used to show a set of options using icons
     *
     * @param menu menu to be set
     */
    public void setMenu(Menu menu) {
        mMenu = menu;
        mMenuLayout.removeAllViews();
        addMenuItems();
        mAnimator.setContentView(mMenuLayout);
    }

    /**
     * Attach an AppBarLayout to receive expand and collapse events
     * to adjust the FloatingActionButton position correctly
     *
     * @param appBar AppBarLayout to be attached
     */
    public void attachAppBarLayout(AppBarLayout appBar) {
        if (appBar != null) {
            mAppBar = appBar;
            mAppBar.addOnOffsetChangedListener(mAnimator);
            mAnimator.setAppBarLayout(mAppBar);
        }
    }

    public void detachAppBarLayout() {
        if (mAppBar != null) {
            mAppBar.removeOnOffsetChangedListener(mAnimator);
            mAnimator.setAppBarLayout(null);
            mAppBar = null;
        }
    }

    /**
     * Attach a FloatingActionButton that'll be used for the morph animation.
     * <p>It will be hidden after {@link #show() show()} is called
     * and shown after {@link #hide() hide()} is called.
     * </p>
     *
     * @param fab FloatingActionButton to attach
     */
    public void attachFab(FloatingActionButton fab) {
        if (fab == null) {
            return;
        }
        mFab = fab;
        mAnimator.setFab(mFab);
        mAnimator.setFloatingAnimatorListener(this);

        if (mHandleFabClick) {
            mFab.setOnClickListener(mViewClickListener);
        }
    }

    /**
     * Detach the FloatingActionButton from this FloatingToolbar.
     * <p>This will disable the auto morph on click.</p>
     */
    public void detachFab() {
        mAnimator.setFab(null);
        if (mFab != null) {
            mFab.setOnClickListener(null);
            mFab = null;
        }
    }

    /**
     * Attach a RecyclerView to hide this FloatingToolbar automatically when a scroll is detected.
     *
     * @param recyclerView RecyclerView to listen for scroll events
     */
    public void attachRecyclerView(RecyclerView recyclerView) {
        if (recyclerView != null) {
            mRecyclerView = recyclerView;
            mRecyclerView.addOnScrollListener(mScrollListener);
        }
    }

    /**
     * Detach the current RecyclerView to stop hiding automatically this FloatingToolbar
     * when a scroll is detected.
     */
    public void detachRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
            mRecyclerView = null;
        }
    }

    /**
     * Add a morph listener to listen for animation events
     * @param listener MorphListener to be added
     */
    public void addMorphListener(MorphListener listener) {
        if (!mMorphListeners.contains(listener)) {
            mMorphListeners.add(listener);
        }
    }

    /**
     * Remove a morph listener previous added
     * @param listener MorphListener to be removed
     */
    public void removeMorphListener(MorphListener listener) {
        mMorphListeners.remove(listener);
    }

    /**
     * This method will automatically morph the attached FloatingActionButton
     * into this FloatingToolbar.
     *
     * @throws IllegalStateException if there's no FloatingActionButton attached
     */
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
        for (MorphListener morphListener : mMorphListeners) {
            morphListener.onMorphStart();
        }
    }

    /**
     * This method will automatically morph the FloatingToolbar into the attached FloatingActionButton
     *
     * @throws IllegalStateException if there's no FloatingActionButton attached
     */
    public void hide() {
        if (mFab == null) {
            throw new IllegalStateException("FloatingActionButton not attached." +
                    "Please, use attachFab(FloatingActionButton fab).");
        }

        if (mMorphed && !mMorphing) {
            mMorphed = false;
            mMorphing = true;
            mAnimator.hide();
            for (MorphListener morphListener : mMorphListeners) {
                morphListener.onUnmorphStart();
            }
        }
    }

    /**
     * Place the menu items with icons inside a horizontal LinearLayout
     */
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
        if (!mMorphed) {
            for (MorphListener morphListener : mMorphListeners) {
                morphListener.onUnmorphEnd();
            }
        } else {
            for (MorphListener morphListener : mMorphListeners) {
                morphListener.onMorphEnd();
            }
        }
    }

    /**
     * Interface to listen to click events on views with MenuItems.
     * <p>
     * Each method only gets called once, even if the user spams multiple clicks
     * </p>
     */
    public interface ItemClickListener {
        void onItemClick(MenuItem item);

        void onItemLongClick(MenuItem item);
    }

    /**
     * Interface to listen to the morph animation
     */
    public interface MorphListener {
        void onMorphEnd();

        void onMorphStart();

        void onUnmorphStart();

        void onUnmorphEnd();
    }

    // FloatingActionButton.Behavior adapted
    public static class Behavior extends CoordinatorLayout.Behavior<FloatingToolbar> {

        @Override
        public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams lp) {
            if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
                // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that
                // we dodge any Snackbars
                lp.dodgeInsetEdges = Gravity.BOTTOM;
            }
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
        return dp * context.getResources().getDisplayMetrics().density;
    }

}
