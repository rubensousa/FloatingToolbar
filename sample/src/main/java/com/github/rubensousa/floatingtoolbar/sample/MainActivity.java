package com.github.rubensousa.floatingtoolbar.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.github.rubensousa.floatingtoolbar.sample.R;

public class MainActivity extends AppCompatActivity implements FloatingToolbar.ItemClickListener,
        Toolbar.OnMenuItemClickListener, CustomAdapter.ClickListener {

    private Toolbar mToolbar;
    private FloatingToolbar mFloatingToolbar;
    private CustomAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mFloatingToolbar = (FloatingToolbar) findViewById(R.id.floatingToolbar);

        mToolbar.setTitle(R.string.app_name);
        mToolbar.inflateMenu(R.menu.menu_toolbar);
        mToolbar.setOnMenuItemClickListener(this);

        mAdapter = new CustomAdapter(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);


        mFloatingToolbar.setClickListener(this);
        mFloatingToolbar.attachFab(fab);
        mFloatingToolbar.attachRecyclerView(recyclerView);

        View customView = mFloatingToolbar.getCustomView();
        if (customView != null) {
            customView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFloatingToolbar.hide();
                }
            });
        }
    }

    @Override
    public void onItemClick(MenuItem item) {
        mAdapter.addItem(item);
    }

    @Override
    public void onItemLongClick(MenuItem item) {

    }

    @Override
    public void onAdapterItemClick(MenuItem item) {
        Intent intent = new Intent(this, DetailActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_snackbar) {
            Snackbar.make(mToolbar, "Here's a SnackBar", Snackbar.LENGTH_LONG).show();
            return true;
        }
        return false;
    }
}
