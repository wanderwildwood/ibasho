package com.wanderwildwood.ibasho.ui;

import static com.wanderwildwood.ibasho.net.ServerRequiredVersionCheckKt.isMinRequiredVersion;
import static com.wanderwildwood.ibasho.ui.SetupWarningsActivityKt.shouldShowSetupWarnings;
import static com.wanderwildwood.ibasho.ui.UiUtil.setupEdgeToEdgeAppBar;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

import com.wanderwildwood.ibasho.BuildConfig;
import com.wanderwildwood.ibasho.R;
import com.wanderwildwood.ibasho.ui.setup.SetupGuideActivity;
import com.wanderwildwood.ibasho.data.Settings;
import com.wanderwildwood.ibasho.data.SettingsRepository;
import com.wanderwildwood.ibasho.net.MinRequiredVersionResult;
import com.wanderwildwood.ibasho.net.ServerCommandDownloader;
import com.wanderwildwood.ibasho.ui.home.CommandListFragment;
import com.wanderwildwood.ibasho.ui.home.TransportListFragment;
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity;
import com.wanderwildwood.ibasho.ui.settings.SettingsFragment;
import com.wanderwildwood.ibasho.push.PushChoice;
import com.wanderwildwood.ibasho.warnings.PushWarningsKt;
import kotlin.Unit;


public class MainActivity extends FmdActivity {

    private static final String KEY_ACTIVE_FRAGMENT_TAG = "activeFragmentTag";

    SettingsRepository settings;

    private TaggedFragment commandsFragment, transportFragment, settingsFragment;
    private TaggedFragment activeFragment;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // for some reason, getTag() returns null, so we need to use getStaticTag()
        outState.putString(KEY_ACTIVE_FRAGMENT_TAG, activeFragment.getStaticTag());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        // Make 3-button navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeAppBar(findViewById(R.id.fragment_container)); // shift the container down, too

        settings = SettingsRepository.Companion.getInstance(this);

        // Around the CrashedActivity it can happen that the two activities run in different processes.
        // In different processes, the SettingsRepository instance is different.
        // This can result in an endless "Continue to MainActivity" loop, because one repo sets the
        // flag to 0, but the other repo does not load the updated file.
        // To make sure we load the status correctly, reload from disk.
        settings.load();

        if (((Number) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)).intValue() == 1) {
            Intent intent = new Intent(this, CrashedActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // A new install starts with the setup guide. An existing one, already set up, is
        // counted as done rather than being walked through what it has.
        if (!(Boolean) settings.get(Settings.SET_SETUP_GUIDE_DONE)) {
            if (settings.serverAccountExists() || (Boolean) settings.get(Settings.SET_FIRST_TIME_CONTACT_ADDED)) {
                settings.set(Settings.SET_SETUP_GUIDE_DONE, true);
            } else {
                startActivity(new Intent(this, SetupGuideActivity.class));
            }
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(navListener);

        commandsFragment = new CommandListFragment();
        transportFragment = new TransportListFragment();
        settingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            activeFragment = commandsFragment;
        } else {
            String tag = savedInstanceState.getString(KEY_ACTIVE_FRAGMENT_TAG);
            if (tag == null || tag.equals(commandsFragment.getStaticTag())) {
                activeFragment = commandsFragment;
            } else if (tag.equals(transportFragment.getStaticTag())) {
                activeFragment = transportFragment;
            } else if (tag.equals(settingsFragment.getStaticTag())) {
                activeFragment = settingsFragment;
            }
        }

        if (settings.serverAccountExists()) {
            checkServerVersion();

            new ServerCommandDownloader(this).download();

            if (!PushChoice.INSTANCE.isSetUp(this)) {
                PushWarningsKt.showDialogMissingUnifiedPush(this, () -> {
                    Intent intent = new Intent(this, FMDServerActivity.class);
                    startActivity(intent);
                    return Unit.INSTANCE;
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment, activeFragment.getStaticTag())
                .commit();

        invalidateOptionsMenu();
    }

    private final NavigationBarView.OnItemSelectedListener navListener = (item) -> {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_commands) {
            activeFragment = commandsFragment;
        } else if (itemId == R.id.nav_transports) {
            activeFragment = transportFragment;
        } else if (itemId == R.id.nav_settings) {
            activeFragment = settingsFragment;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, activeFragment)
                .commit();
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (shouldShowSetupWarnings(this)) {
            toolbar.inflateMenu(R.menu.main_app_bar_warnings);
        } else {
            toolbar.inflateMenu(R.menu.main_app_bar);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuItemSetupWarnings) {
            Intent intent = new Intent(this, SetupWarningsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menuItemAbout) {
            AboutDialogKt.showAboutDialog(this);
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkServerVersion() {
        isMinRequiredVersion(this, result -> {
            if (result instanceof MinRequiredVersionResult.ServerOutdated outdated) {
                String text = getString(R.string.server_version_upgrade_required_text)
                        .replace("{CURRENT}", outdated.getActualVersion())
                        .replace("{MIN}", outdated.getMinRequiredVersion());

                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.server_version_upgrade_required_title))
                        .setMessage(text)
                        .setPositiveButton(getString(R.string.Ok), null)
                        .setCancelable(false)
                        .show();
            }
            return Unit.INSTANCE;
        });
    }
}
