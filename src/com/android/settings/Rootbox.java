/*
 * Copyright (C) 2012 RootBox Project
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

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
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
    private static final String KEY_EXPANDED_DESKTOP = "power_menu_expanded_desktop";
    private static final String KEY_HEADSET_CONNECT_PLAYER = "headset_connect_player";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String PREF_KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final String PREF_POWER_CRT_SCREEN_ON = "system_power_crt_screen_on";
    private static final String PREF_POWER_CRT_SCREEN_OFF = "system_power_crt_screen_off";
    private static final String PREF_FULLSCREEN_KEYBOARD = "fullscreen_keyboard";
    private static final String VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    
    private PreferenceScreen mLockscreenButtons;
    private CheckBoxPreference mExpandedDesktopPref;
    private CheckBoxPreference mHeadsetConnectPlayer;
    private CheckBoxPreference mVolumeAdjustSounds;
    private CheckBoxPreference mKillAppLongpressBack;
    private CheckBoxPreference mCrtOff;
    private CheckBoxPreference mCrtOn;
    private CheckBoxPreference mFullscreenKeyboard;
    private ListPreference mVolumeKeyCursorControl;
    private final Configuration mCurConfig = new Configuration();
    private Context mContext;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }
    
    private boolean isCrtOffChecked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        mContext = getActivity();

        addPreferencesFromResource(R.xml.rootbox_settings);
        PreferenceScreen prefs = getPreferenceScreen();

        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
            getPreferenceScreen().removePreference(mLockscreenButtons);
        }

       // respect device default configuration
        // true fades while false animates
        boolean electronBeamFadesConfig = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);

        // use this to enable/disable crt on feature
        // crt only works if crt off is enabled
        // total system failure if only crt on is enabled
        isCrtOffChecked = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                electronBeamFadesConfig ? 0 : 1) == 1;

        mCrtOff = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_OFF);
        mCrtOff.setChecked(isCrtOffChecked);
        mCrtOff.setOnPreferenceChangeListener(this);

        mCrtOn = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_ON);
        mCrtOn.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_POWER_ENABLE_CRT_ON, 0) == 1);
        mCrtOn.setEnabled(isCrtOffChecked);
        mCrtOn.setOnPreferenceChangeListener(this);

        mFullscreenKeyboard = (CheckBoxPreference) findPreference(PREF_FULLSCREEN_KEYBOARD);
        mFullscreenKeyboard.setChecked(Settings.System.getInt(resolver,
                Settings.System.FULLSCREEN_KEYBOARD, 0) == 1);

        mVolumeKeyCursorControl = (ListPreference) findPreference(VOLUME_KEY_CURSOR_CONTROL);
        if(mVolumeKeyCursorControl != null) {
            mVolumeKeyCursorControl.setOnPreferenceChangeListener(this);
            mVolumeKeyCursorControl.setValue(Integer.toString(Settings.System.getInt(getActivity()
                    .getContentResolver(), Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0)));
            mVolumeKeyCursorControl.setSummary(mVolumeKeyCursorControl.getEntry());
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

        mExpandedDesktopPref = (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP);
        boolean showExpandedDesktopPref =
            getResources().getBoolean(R.bool.config_show_expandedDesktop);
        if (!showExpandedDesktopPref) {
            if (mExpandedDesktopPref != null) {
                getPreferenceScreen().removePreference(mExpandedDesktopPref);
            }
        } else {
            mExpandedDesktopPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0) == 1));
        }

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

         if (preference == mExpandedDesktopPref) {
            value = mExpandedDesktopPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED,
                    value ? 1 : 0);
         } else if (preference == mHeadsetConnectPlayer) {
            Settings.System.putInt(getContentResolver(), Settings.System.HEADSET_CONNECT_PLAYER,
                    mHeadsetConnectPlayer.isChecked() ? 1 : 0);
         } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                    mVolumeAdjustSounds.isChecked() ? 1 : 0);
         } else if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
         } else if (preference == mFullscreenKeyboard) {
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FULLSCREEN_KEYBOARD,
                    mFullscreenKeyboard.isChecked() ? 1 : 0);
         }  else {
              // If not handled, let preferences handle it.
              return super.onPreferenceTreeClick(preferenceScreen, preference);
         }
         return true;    
     }

    public boolean onPreferenceChange(Preference preference, Object Value) {
        final String key = preference.getKey();
         if (mCrtOff.equals(preference)) {
            isCrtOffChecked = ((Boolean) Value).booleanValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                    (isCrtOffChecked ? 1 : 0));
            // if crt off gets turned off, crt on gets turned off and disabled
            if (!isCrtOffChecked) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SYSTEM_POWER_ENABLE_CRT_ON, 0);
                mCrtOn.setChecked(false);
            }
            mCrtOn.setEnabled(isCrtOffChecked);
            return true;
        } else if (mCrtOn.equals(preference)) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_ON,
                    ((Boolean) Value).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mVolumeKeyCursorControl) {
            String volumeKeyCursorControl = (String) Value;
            int val = Integer.parseInt(volumeKeyCursorControl);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, val);
            int index = mVolumeKeyCursorControl.findIndexOfValue(volumeKeyCursorControl);
            mVolumeKeyCursorControl.setSummary(mVolumeKeyCursorControl.getEntries()[index]);
            return true;
        }
        return false;
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
