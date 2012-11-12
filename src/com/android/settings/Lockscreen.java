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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Lockscreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Lockscreen";
    private static final String KEY_CLOCK_ALIGN = "lockscreen_clock_align";
    private static final String PREF_SHOW_LOCK_BEFORE_UNLOCK = "show_lock_before_unlock";


    private ListPreference mClockAlign;
    private CheckBoxPreference mShowLockBeforeUnlock;
    private Activity mActivity;
    ContentResolver mResolver;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        addPreferencesFromResource(R.xml.interface_lockscreen);
        mClockAlign = (ListPreference) findPreference(KEY_CLOCK_ALIGN);
        mClockAlign.setOnPreferenceChangeListener(this);

        mShowLockBeforeUnlock = (CheckBoxPreference) findPreference(PREF_SHOW_LOCK_BEFORE_UNLOCK);
        mShowLockBeforeUnlock.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                 Settings.System.SHOW_LOCK_BEFORE_UNLOCK, 0) == 1);
    }


    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
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
        if (preference == mShowLockBeforeUnlock) {
            Settings.System.putInt(getActivity().getContentResolver(),
                     Settings.System.SHOW_LOCK_BEFORE_UNLOCK,
            ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
	return true;
        }      
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
         if (preference == mClockAlign) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, value);
            mClockAlign.setSummary(mClockAlign.getEntries()[value]);
            return true;
        }
        return false;
    }
}
