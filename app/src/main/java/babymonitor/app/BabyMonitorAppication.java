package babymonitor.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BabyMonitorAppication extends Application implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences prefs;
    //private boolean serviceRunning;

    @Override
    public void onCreate() {
        super.onCreate();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefs.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }

    public synchronized String getTopic() {
        String topic = this.prefs.getString("topic", null);
        return topic;
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //Log.i("Topic",sharedPreferences.getString("topic",null));

    }

}

