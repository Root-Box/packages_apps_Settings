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

import android.app.Activity;
import android.app.AlertDialog;
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
public class Lockscreen extends SettingsPreferenceFragment
         {

    // Lock Settings
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    public static final String KEY_WIDGETS_PREF = "lockscreen_widgets";

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;
    private CheckBoxPreference mTactileFeedback;
    private ListPreference mWidgetsAlignment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        mWidgetsAlignment = (ListPreference) findPreference(KEY_WIDGETS_PREF);
        mWidgetsAlignment.setOnPreferenceChangeListener(this);
        mWidgetsAlignment.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.LOCKSCREEN_LAYOUT,
                0) + "");
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;

    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        boolean handled = false;
        if (preference == mWidgetsAlignment) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                                    Settings.System.LOCKSCREEN_LAYOUT, value);
            return true;
        }
        return false;   
    }
    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }
}
