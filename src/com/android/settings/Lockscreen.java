/*
 * Copyright (C) 2007 The Android Open Source Project
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


import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserId;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;

/**
 * Gesture lock pattern settings.
 */
public class Lockscreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {


    // Lock Settings
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_CLOCK_ALIGN = "lockscreen_clock_align";
    private static final String PREF_ALT_LOCKSCREEN = "alt_lockscreen";
    private static final String PREF_ALT_LOCKSCREEN_BG_COLOR = "alt_lock_bg_color";
    

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;
    private CheckBoxPreference mTactileFeedback;
    private ListPreference mClockAlign;
    private CheckBoxPreference mAltLockscreen;
    private ColorPickerPreference mAltLockscreenBgColor;
    private Activity mActivity;
    ContentResolver mResolver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();
        addPreferencesFromResource(R.xml.interface_lockscreen);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        mClockAlign = (ListPreference) findPreference(KEY_CLOCK_ALIGN);
        mClockAlign.setOnPreferenceChangeListener(this);

        mAltLockscreen = (CheckBoxPreference) findPreference(PREF_ALT_LOCKSCREEN);
        mAltLockscreen.setChecked(Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.USE_ALT_LOCKSCREEN, false));

        mAltLockscreenBgColor = (ColorPickerPreference) findPreference(PREF_ALT_LOCKSCREEN_BG_COLOR);
        mAltLockscreenBgColor.setOnPreferenceChangeListener(this);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
	if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

       // Add options for lock/unlock screen
        int resid = 0;
        resid = R.xml.interface_lockscreen;    
        addPreferencesFromResource(resid);

        // tactile feedback. Should be common to all unlock preference screens.
        mTactileFeedback = (CheckBoxPreference) root.findPreference(KEY_TACTILE_FEEDBACK_ENABLED);
        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mTactileFeedback != null) {
                securityCategory.removePreference(mTactileFeedback);
            }
        }
           return root;
        }    

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();


        if (mTactileFeedback != null) {
            mTactileFeedback.setChecked(lockPatternUtils.isTactileFeedbackEnabled());
        }
    }

   private void updateState() {
        int resId;
        // Set the clock align value
        if (mClockAlign != null) {
            int clockAlign = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, 2);
            mClockAlign.setValue(String.valueOf(clockAlign));
            mClockAlign.setSummary(mClockAlign.getEntries()[clockAlign]);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (preference == mAltLockscreen) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.USE_ALT_LOCKSCREEN,
                    ((CheckBoxPreference) preference).isChecked());
            return true;        
        }
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
}

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        boolean handled = false;
         if (preference == mClockAlign) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, value);
            mClockAlign.setSummary(mClockAlign.getEntries()[value]);
            return true;
          } else if (preference == mAltLockscreenBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ALT_LOCK_BG_COLOR, intHex);
            return true;
        }
        return false;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }
}
