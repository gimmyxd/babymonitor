package babymonitor.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Dragos on 1/1/2016.
 */
public class TopicActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
