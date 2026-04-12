package org.zephyrsoft.sdbviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

/**
 * An activity representing a single Song detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link SongListActivity}.
 */
public class SongDetailActivity extends AppCompatActivity {

    private enum ScrollState { IDLE, SCROLLING, PAUSED }

    // Speed persists across songs within a session (static field).
    private static int speedLevel = 3;
    private static final int SPEED_MIN = 1;
    private static final int SPEED_MAX = 10;
    private static final int TICK_MS = 16; // ~60 fps

    private ScrollState scrollState = ScrollState.IDLE;
    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            NestedScrollView scrollView = findViewById(R.id.song_detail_container);
            if (scrollView != null) {
                if (!scrollView.canScrollVertically(1)) {
                    stopScroll();
                    return;
                }
                scrollView.scrollBy(0, speedLevel);
            }
            scrollHandler.postDelayed(this, TICK_MS);
        }
    };

    private MenuItem playItem;
    private MenuItem pauseItem;
    private MenuItem stopItem;
    private MenuItem fasterItem;
    private MenuItem slowerItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(Constants.ARG_SONG, getIntent().getParcelableExtra(Constants.ARG_SONG));
            SongDetailFragment fragment = new SongDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.song_detail_container, fragment)
                .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_song_detail, menu);
        playItem = menu.findItem(R.id.action_scroll_play);
        pauseItem = menu.findItem(R.id.action_scroll_pause);
        stopItem = menu.findItem(R.id.action_scroll_stop);
        fasterItem = menu.findItem(R.id.action_scroll_faster);
        slowerItem = menu.findItem(R.id.action_scroll_slower);
        updateMenuVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (upIntent == null || NavUtils.shouldUpRecreateTask(this, upIntent)) {
                navigateUpTo(upIntent);
            } else {
                finish();
            }
            return true;
        } else if (id == R.id.action_scroll_play) {
            startScroll();
            return true;
        } else if (id == R.id.action_scroll_pause) {
            pauseScroll();
            return true;
        } else if (id == R.id.action_scroll_stop) {
            stopScroll();
            return true;
        } else if (id == R.id.action_scroll_faster) {
            if (speedLevel < SPEED_MAX) {
                speedLevel++;
            }
            return true;
        } else if (id == R.id.action_scroll_slower) {
            if (speedLevel > SPEED_MIN) {
                speedLevel--;
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart scrolling after e.g. screen rotation
        if (scrollState == ScrollState.SCROLLING) {
            scrollHandler.post(scrollRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scrollHandler.removeCallbacks(scrollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scrollHandler.removeCallbacks(scrollRunnable);
    }

    private void startScroll() {
        scrollState = ScrollState.SCROLLING;
        updateMenuVisibility();
        scrollHandler.removeCallbacks(scrollRunnable);
        scrollHandler.post(scrollRunnable);
    }

    private void pauseScroll() {
        scrollState = ScrollState.PAUSED;
        updateMenuVisibility();
        scrollHandler.removeCallbacks(scrollRunnable);
    }

    private void stopScroll() {
        scrollState = ScrollState.IDLE;
        updateMenuVisibility();
        scrollHandler.removeCallbacks(scrollRunnable);
        NestedScrollView scrollView = findViewById(R.id.song_detail_container);
        if (scrollView != null) {
            scrollView.scrollTo(0, 0);
        }
    }

    private void updateMenuVisibility() {
        if (playItem == null) {
            return;
        }
        switch (scrollState) {
            case IDLE:
                playItem.setVisible(true);
                pauseItem.setVisible(false);
                stopItem.setVisible(false);
                fasterItem.setVisible(false);
                slowerItem.setVisible(false);
                break;
            case SCROLLING:
                playItem.setVisible(false);
                pauseItem.setVisible(true);
                stopItem.setVisible(true);
                fasterItem.setVisible(true);
                slowerItem.setVisible(true);
                break;
            case PAUSED:
                playItem.setVisible(true);
                pauseItem.setVisible(false);
                stopItem.setVisible(true);
                fasterItem.setVisible(true);
                slowerItem.setVisible(true);
                break;
        }
    }
}
