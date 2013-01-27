package com.android.settings.rb;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class About extends SettingsPreferenceFragment {

    public static final String TAG = "About";

    Preference mSupportUrl;
    Preference mSourceUrl;
    Preference mFacebookUrl;
    Preference mGooglePlusUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_rom);
        mSupportUrl = findPreference("rootbox_support");
        mSourceUrl = findPreference("rootbox_source");
        mFacebookUrl = findPreference("rootbox_facebook");
        mGooglePlusUrl = findPreference("rootbox_googleplus");

        PreferenceGroup devsGroup = (PreferenceGroup) findPreference("devs");
        ArrayList<Preference> devs = new ArrayList<Preference>();
        for (int i = 0; i < devsGroup.getPreferenceCount(); i++) {
            devs.add(devsGroup.getPreference(i));
        }
        devsGroup.removeAll();
        devsGroup.setOrderingAsAdded(false);
        Collections.shuffle(devs);
        for(int i = 0; i < devs.size(); i++) {
            Preference p = devs.get(i);
            p.setOrder(i);

            devsGroup.addPreference(p);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSupportUrl) {
            launchUrl("http://forum.xda-developers.com/showthread.php?t=2117444");
        } else if (preference == mSourceUrl) {
            launchUrl("http://github.com/Root-Box");
        } else if (preference == mFacebookUrl) {
            launchUrl("http://facebook.com/333083833406934");
        } else if (preference == mGooglePlusUrl) {
            launchUrl("https://plus.google.com/u/0/communities/115833651542488654391");
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent donate = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(donate);
    }
}
