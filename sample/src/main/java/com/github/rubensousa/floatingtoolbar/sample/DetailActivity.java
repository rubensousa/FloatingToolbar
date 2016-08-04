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

package com.github.rubensousa.floatingtoolbar.sample;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.github.rubensousa.floatingtoolbar.FloatingToolbar;

public class DetailActivity extends AppCompatActivity implements FloatingToolbar.ItemClickListener {

    private FloatingActionButton mFabAppBar;
    private FloatingActionButton mFab;
    private AppBarLayout mAppBar;
    private FloatingToolbar mFloatingToolbar;
    private boolean mShowingFromAppBar;
    private boolean mShowingFromNormal;

    // 808100900

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);

        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mFloatingToolbar = (FloatingToolbar) findViewById(R.id.floatingToolbar);
        mFabAppBar = (FloatingActionButton) findViewById(R.id.fab);
        mFab = (FloatingActionButton) findViewById(R.id.fab2);

        // Don't handle fab click since we'll have 2 of them
        mFloatingToolbar.handleFabClick(false);
        mFloatingToolbar.setClickListener(this);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAppBar.setExpanded(false, true);
                mFloatingToolbar.attachFab(mFab);
                mFloatingToolbar.show();
                mShowingFromNormal = true;
            }
        });

        mFabAppBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mFloatingToolbar.isShowing()) {
                    mFab.hide();
                    mFloatingToolbar.attachAppBarLayout(mAppBar);
                    mFloatingToolbar.attachFab(mFabAppBar);
                    mFloatingToolbar.show();
                    mShowingFromAppBar = true;
                }
            }
        });
    }

    @Override
    public void onItemClick(MenuItem item) {
        if (mShowingFromAppBar) {
            mFab.show();
        }

        if (mShowingFromNormal) {
            mFabAppBar.show();
        }

        mShowingFromAppBar = false;
        mShowingFromNormal = false;
    }

    @Override
    public void onItemLongClick(MenuItem item) {

    }
}
