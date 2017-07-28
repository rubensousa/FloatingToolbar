package com.github.rubensousa.floatingtoolbar.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.github.rubensousa.floatingtoolbar.FloatingToolbarMenuBuilder;

public class MainActivity extends AppCompatActivity implements FloatingToolbar.ItemClickListener,
        Toolbar.OnMenuItemClickListener, CustomAdapter.ClickListener, FloatingToolbar.MorphListener {

    private Toolbar mToolbar;
    private FloatingToolbar mFloatingToolbar;
    private CustomAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mFloatingToolbar = findViewById(R.id.floatingToolbar);

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
        mFloatingToolbar.addMorphListener(this);

        //Create a custom menu
        mFloatingToolbar.setMenu(new FloatingToolbarMenuBuilder(this)
                .addItem(R.id.action_unread, R.drawable.ic_markunread_black_24dp, "Mark unread")
                .addItem(R.id.action_copy, R.drawable.ic_content_copy_black_24dp, "Copy")
                .addItem(R.id.action_google, R.drawable.ic_google_plus_box, "Google+")
                .addItem(R.id.action_facebook, R.drawable.ic_facebook_box, "Facebook")
                .addItem(R.id.action_twitter, R.drawable.ic_twitter_box, "Twitter")
                .build());

        // Usage with custom view
        /*View customView = mFloatingToolbar.getCustomView();
        if (customView != null) {
            customView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFloatingToolbar.hide();
                }
            });
        }*/

        // How to edit current menu
        // Menu menu = mFloatingToolbar.getMenu();
        // menu.findItem(R.id.action_copy).setVisible(false);
        // mFloatingToolbar.setMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFloatingToolbar.removeMorphListener(this);
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
            Snackbar snackbar = Snackbar.make(mToolbar, "Here's a SnackBar",
                    Snackbar.LENGTH_INDEFINITE);
            mFloatingToolbar.showSnackBar(snackbar);
            return true;
        }
        return false;
    }

    @Override
    public void onMorphEnd() {

    }

    @Override
    public void onMorphStart() {

    }

    @Override
    public void onUnmorphStart() {

    }

    @Override
    public void onUnmorphEnd() {

    }
}
