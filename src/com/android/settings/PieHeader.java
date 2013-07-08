
package com.android.settings;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

public class PieHeader extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String SLIM_PIE = "slim_pie";
    private static final String PARANOID_PIE = "paranoid_pie";
    private static final String PIE_TOGGLE_BEHAVIOR = "pie_toggle_behavior";

    private Context mContext;

    ListPreference mPieToggle;
    Preference mSlimPie;
    Preference mParanoidPie;

    public static class HelpFragment extends DialogFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            View v = inflater.inflate(R.layout.pie_help, null);
            TextView tv = (TextView) v.findViewById(R.id.help);
            tv.setText(R.string.pie_help_text);
            return v;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pie_header);

        mSlimPie = (Preference) findPreference(SLIM_PIE);
        mParanoidPie = (Preference) findPreference(PARANOID_PIE);

        mPieToggle = (ListPreference) findPreference(PIE_TOGGLE_BEHAVIOR);
        int pieToggle = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_TOGGLE_BEHAVIOR, 0);
        mPieToggle.setValue(String.valueOf(pieToggle));
        mPieToggle.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieToggle) {
            int pieToggle = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_TOGGLE_BEHAVIOR, pieToggle);
            Helpers.restartSystemUI();
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pie_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int choice = item.getItemId();
        switch (choice) {
            case R.id.help:
                DialogFragment df = new HelpFragment();
                df.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                df.show(getFragmentManager(), "help");
                return true;
            default:
                return false;
        }
    }
}
