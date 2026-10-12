package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.core.view.MenuProvider;
import androidx.preference.PreferenceManager;

import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.fragments.AppsToggles;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.Prefs;

import java.util.List;
import java.util.Set;

public class AppFilterActivity extends BaseActivity implements MenuProvider {
    private static final String TAG = "AppFilterActivity";
    private AppFilterFragment mFragment;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.target_apps);
        setContentView(R.layout.fragment_activity);
        addMenuProvider(this);
        displayBackAction();

        if (savedInstanceState != null)
            mFragment = (AppFilterFragment) getSupportFragmentManager().getFragment(savedInstanceState, "fragment");
        if (mFragment == null)
            mFragment = new AppFilterFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, mFragment)
                .commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        else
            overridePendingTransition(0, 0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
                else
                    overridePendingTransition(0, 0);
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, "fragment", mFragment);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.hint_menu, menu);
        inflater.inflate(R.menu.target_apps_bulk_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.show_hint) {
            Utils.showHelpDialog(this, R.string.target_apps_help);
            return true;
        }
        if (id == R.id.bulk_select_system) {
            if (mFragment != null)
                mFragment.selectAppsOfType(true);
            return true;
        }
        if (id == R.id.bulk_select_third_party) {
            if (mFragment != null)
                mFragment.selectAppsOfType(false);
            return true;
        }

        return false;
    }

    public static class AppFilterFragment extends AppsToggles {
        private final Set<String> mSelectedApps = new ArraySet<>();
        private @Nullable SharedPreferences mPrefs;
        private View mSelectSystemButton;
        private View mSelectThirdPartyButton;
        private boolean mAppsLoaded;

        @Override
        protected int getLayoutResId() {
            return R.layout.target_apps_selection;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mSelectSystemButton = view.findViewById(R.id.select_system_apps);
            mSelectThirdPartyButton = view.findViewById(R.id.select_third_party_apps);
            mSelectSystemButton.setOnClickListener(v -> selectAppsOfType(true));
            mSelectThirdPartyButton.setOnClickListener(v -> selectAppsOfType(false));
        }

        @Override
        public void onAppsInfoLoaded(List<AppDescriptor> apps) {
            super.onAppsInfoLoaded(apps);
            mAppsLoaded = true;
        }

        @Override
        public void onDestroyView() {
            mSelectSystemButton = null;
            mSelectThirdPartyButton = null;
            mAppsLoaded = false;
            super.onDestroyView();
        }

        public void selectAppsOfType(boolean systemApps) {
            if (!mAppsLoaded)
                return;

            Set<String> addedPackages = selectAppsByType(systemApps);
            if (addedPackages.isEmpty())
                return;

            mSelectedApps.addAll(addedPackages);
            saveSelection();
        }

        private void saveSelection() {
            Log.d(TAG, "Saving " + mSelectedApps.size() + " target apps");
            if (mPrefs != null) {
                // SharedPreferences requires the supplied set to remain immutable.
                mPrefs.edit()
                        .putStringSet(Prefs.PREF_APP_FILTER, new ArraySet<>(mSelectedApps))
                        .apply();
            }
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            assert mPrefs != null;

            mSelectedApps.clear();

            Set<String> saved = Prefs.getStringSet(mPrefs, Prefs.PREF_APP_FILTER);
            if(!saved.isEmpty()) {
                Log.d(TAG, "Loading " + saved.size() + " target apps");
                mSelectedApps.addAll(saved);
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mPrefs = null;
        }

        @Override
        protected Set<String> getCheckedApps() {
            return mSelectedApps;
        }

        @Override
        public void onAppToggled(AppDescriptor app, boolean checked) {
            String packageName = app.getPackageName();
            if(mSelectedApps.contains(packageName) == checked)
                return; // nothing to do

            if(checked)
                mSelectedApps.add(packageName);
            else
                mSelectedApps.remove(packageName);

            saveSelection();
        }
    }
}
