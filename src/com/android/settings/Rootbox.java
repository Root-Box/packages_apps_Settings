/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Rootbox extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Rootbox";

    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_HEADSET_CONNECT_PLAYER = "headset_connect_player";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String PREF_EXPANDED_DESKTOP = "power_menu_expanded_desktop";
    private static final String PREF_KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    
    private PreferenceScreen mLockscreenButtons;
    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mHeadsetConnectPlayer;
    private CheckBoxPreference mVolumeAdjustSounds;
    private CheckBoxPreference mKillAppLongpressBack;
    private final Configuration mCurConfig = new Configuration();

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();

        addPreferencesFromResource(R.xml.rootbox_settings);
        PreferenceScreen prefs = getPreferenceScreen();

        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
            getPreferenceScreen().removePreference(mLockscreenButtons);
        }

        mHeadsetConnectPlayer = (CheckBoxPreference) findPreference(KEY_HEADSET_CONNECT_PLAYER);
        mHeadsetConnectPlayer.setChecked(Settings.System.getInt(resolver,
                Settings.System.HEADSET_CONNECT_PLAYER, 0) != 0);

        mVolumeAdjustSounds = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdjustSounds.setPersistent(false);
        mVolumeAdjustSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED, 1) != 0);

        mKillAppLongpressBack = (CheckBoxPreference) findPreference(PREF_KILL_APP_LONGPRESS_BACK);
                updateKillAppLongpressBackOptions();

        boolean hasNavBarByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);

        if (hasNavBarByDefault) {
            getPreferenceScreen().removePreference(mKillAppLongpressBack);
        }

        PreferenceScreen prefSet = getPreferenceScreen();
        mExpandedDesktopPref = (ListPreference) prefSet.findPreference(PREF_EXPANDED_DESKTOP);
        mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        int expandedDesktopValue = Settings.System.getInt(getContentResolver(), Settings.System.EXPANDED_DESKTOP_STATUS_BAR_STATE, 0);
        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        updateExpandedDesktopSummary(expandedDesktopValue);

        // Do not display lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));

        // Only show the hardware keys config on a device that does not have a navbar
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                getPreferenceScreen().removePreference(findPreference(KEY_HARDWARE_KEYS));
            }
        } catch (RemoteException e) {
            // Do nothing
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.KILL_APP_LONGPRESS_BACK, mKillAppLongpressBack.isChecked() ? 1 : 0);
    }

    private void updateKillAppLongpressBackOptions() {
        mKillAppLongpressBack.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.KILL_APP_LONGPRESS_BACK, 0) != 0);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
         boolean value;

         if (preference == mHeadsetConnectPlayer) {
            Settings.System.putInt(getContentResolver(), Settings.System.HEADSET_CONNECT_PLAYER,
                    mHeadsetConnectPlayer.isChecked() ? 1 : 0);
         } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                    mVolumeAdjustSounds.isChecked() ? 1 : 0);
         } else if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
         }  else {
              // If not handled, let preferences handle it.
              return super.onPreferenceTreeClick(preferenceScreen, preference);
         }
         return true;    
     }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.EXPANDED_DESKTOP_STATUS_BAR_STATE, expandedDesktopValue);
            updateExpandedDesktopSummary(expandedDesktopValue);
            return true;
        }
        return false;
    }

    private void updateExpandedDesktopSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            /* expanded desktop deactivated */
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
            mExpandedDesktopPref.setSummary(res.getString(R.string.expanded_desktop_disabled));
        } else if (value == 1) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.expanded_desktop_summary_status_bar);
            mExpandedDesktopPref.setSummary(res.getString(R.string.summary_expanded_desktop, statusBarPresent));
        } else if (value == 2) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.expanded_desktop_summary_no_status_bar);
            mExpandedDesktopPref.setSummary(res.getString(R.string.summary_expanded_desktop, statusBarPresent));
        }
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

}
