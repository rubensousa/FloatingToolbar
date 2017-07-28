/*
 * Copyright 2017 Rúben Sousa
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


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v7.view.menu.MenuBuilder;
import android.view.Menu;

@SuppressWarnings("RestrictedApi")
public class FloatingToolbarMenuBuilder {

    private MenuBuilder menuBuilder;

    public FloatingToolbarMenuBuilder(Context context) {
        menuBuilder = new MenuBuilder(context);
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, Drawable icon, String title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, Drawable icon, @StringRes int title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, @DrawableRes int icon,
                                              @StringRes int title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, @DrawableRes int icon, String title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, @DrawableRes int icon) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, "").setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(@IdRes int id, Drawable icon) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, "").setIcon(icon);
        return this;
    }

    public Menu build() {
        return menuBuilder;
    }

}
