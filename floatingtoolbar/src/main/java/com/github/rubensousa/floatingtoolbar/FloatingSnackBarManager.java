package com.github.rubensousa.floatingtoolbar;


import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;

class FloatingSnackBarManager implements FloatingToolbar.MorphListener {


    private BaseTransientBottomBar.BaseCallback<Snackbar> mShowCallback
            = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            mFloatingToolbar.dispatchShow();
            transientBottomBar.removeCallback(this);
            mSnackBar = null;
        }
    };

    private BaseTransientBottomBar.BaseCallback<Snackbar> mHideCallback
            = new BaseTransientBottomBar.BaseCallback<Snackbar>() {
        @Override
        public void onDismissed(Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);
            mFloatingToolbar.dispatchHide();
            transientBottomBar.removeCallback(this);
            mSnackBar = null;
        }
    };

    Snackbar mSnackBar;
    private FloatingToolbar mFloatingToolbar;

    public FloatingSnackBarManager(FloatingToolbar toolbar) {
        mFloatingToolbar = toolbar;
        mFloatingToolbar.addMorphListener(this);
    }

    public boolean hasSnackBar() {
        return mSnackBar != null && mSnackBar.isShown();
    }

    public void dismissAndShow() {
        mSnackBar.addCallback(mShowCallback);
        mSnackBar.dismiss();
    }

    public void dismissAndHide() {
        mSnackBar.addCallback(mHideCallback);
        mSnackBar.dismiss();
    }

    public void showSnackBar(final Snackbar snackbar) {
        // If we're currently morphing, show the snackbar after
        if (mFloatingToolbar.mMorphing) {
            mSnackBar = snackbar;
        } else {
            showSnackBarInternal(snackbar);
        }

    }

    private void showSnackBarInternal(Snackbar snackbar) {

        if (!mFloatingToolbar.isShowing()) {
            if (!mFloatingToolbar.mMorphing) {
                mSnackBar = snackbar;
                snackbar.show();
            } else if (mSnackBar != null) {
                mSnackBar.removeCallback(mHideCallback);
                mSnackBar.removeCallback(mShowCallback);
                mSnackBar.dismiss();
                mSnackBar = snackbar;
            }
            return;
        }

        View view = snackbar.getView();
        CoordinatorLayout.LayoutParams params
                = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.bottomMargin = mFloatingToolbar.getHeight();
        view.setLayoutParams(params);
        snackbar.show();
        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                mSnackBar = null;
            }
        });
    }

    @Override
    public void onMorphEnd() {
        if (mSnackBar != null && !mSnackBar.isShown()) {
            showSnackBarInternal(mSnackBar);
        }
    }

    @Override
    public void onMorphStart() {

    }

    @Override
    public void onUnmorphStart() {

    }

    @Override
    public void onUnmorphEnd() {
        if (mSnackBar != null && !mSnackBar.isShown()) {
            showSnackBarInternal(mSnackBar);
        }
    }
}
