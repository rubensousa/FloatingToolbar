# FloatingToolbar
A toolbar that morphs from a FloatingActionButton

Inspired by the material design spec: https://www.google.com/design/spec/components/buttons-floating-action-button.html#buttons-floating-action-button-transitions

Available from API 14.


<img src="screenshots/demo.gif" width="350"><img src="screenshots/demo_scroll.gif" width="350">

## How to use

#####1. Add the following to your build.gradle:
```groovy
repositories{
  maven { url "https://jitpack.io" }
}

dependencies {
  compile 'com.github.rubensousa:FloatingToolbar:0.3'
}
```
#####2. Add FloatingToolbar as a direct child of CoordinatorLayout and before the FloatingActionButton:
```xml
<android.support.design.widget.CoordinatorLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/coordinatorLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">
    
    <!-- Appbar -->

    <com.github.rubensousa.floatingtoolbar.FloatingToolbar
        android:id="@+id/floatingToolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:layout_gravity="bottom"
        app:floatingMenu="@menu/main" />

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="@dimen/fab_margin"
        android:src="@drawable/ic_share_black_24dp" />
        
        
</android.support.design.widget.CoordinatorLayout>
```
#####3. Specify a menu resource file or custom layout with app:floatingMenu or app:floatingCustomView

#####4. Attach the FAB to the FloatingToolbar to automatically start the transition on click event:

```java
mFloatingToolbar.attachFab(fab);
```

##### 5. Set a click listener
```java
mFloatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {
                
            }

            @Override
            public void onItemLongClick(MenuItem item) {

            }
        });
```
##### 6. (Optional) Attach a RecyclerView to hide the FloatingToolbar on scroll:

```java
mFloatingToolbar.attachRecyclerView(recyclerView);
```

##### 7 . (Optional) Use show() and hide() to trigger the transition anytime:

```java
mFloatingToolbar.show();
mFloatingToolbar.hide();
```        


## Attributes

- app:floatingMenu -> Menu resource
- app:floatingItemBackground -> Drawable resource
- app:floatingCustomView -> Layout resource

## Changelog

### 0.3 (29/04/2016)
- Improved behavior with SnackBar (Still needs some fixes though)
- Improved morph animation
- Throw IllegalStateException if FAB is not attached and show() or hide() are called

### 0.2 (26/04/2016)
- Renamed attributes

## License

    Copyright 2016 Rúben Sousa
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
