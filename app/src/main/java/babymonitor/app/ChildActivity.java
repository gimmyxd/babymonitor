package babymonitor.app;

import android.app.KeyguardManager;
import android.content.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class ChildActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MyActivity";
    private static final float SHAKE_THRESHOLD = 1.04f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private  int counter = 0;
    private boolean crying = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private long movingTime = 10000;
    private int movingCounter =0;
    private String topic ="";
    private boolean moving = false;


    private long mShakeTime = 0;

    private TextView movesCount;
    private TextView mInformationTextView;

    private Handler handler;
    private SoundMeter mSensor;
    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private static String token;
    private TextView tokenTextView;

//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.activity_main, menu);
//        return true;
//    }


    // Called when an options item is clicked
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.itemRecordingStart:
//                //start recording
//                startSoundRecorder();
//                startAccelerometer();
//                recordingTextAnimation();
//                Toast.makeText(getBaseContext(), "Start Recording", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.itemRecordingStop:
//                //stop recording
//                stopSoundRecorder();
//                stopAccelerometer();
//                hideTextView();
//                Toast.makeText(getBaseContext(), "Stop Recording", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.itemSubscribingStart:
//                RegistrationIntentService.running ="yes";
//                Intent msgIntent = new Intent(this, RegistrationIntentService.class);
//                startService(msgIntent);
//                break;
//            case R.id.itemSendTestMessage:
//                sendTestMessage();
//                break;
//            case R.id.itemPrefs:
//                startActivity(new Intent(this, TopicActivity.class));
//                break;
//        }
//        return true;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_page);
        initializeViews();
        createWakeLocks();
        hideTextView();
        generateToken();
        final Intent msgIntent = new Intent(this, RegistrationIntentService.class);


        final Button record_button = (Button) findViewById(R.id.record_button);
        final Button stop_button = (Button) findViewById(R.id.stop_button);

        record_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSoundRecorder();
                startAccelerometer();
                recordingTextAnimation();
                RegistrationIntentService.running ="yes";
                startService(msgIntent);
            }
        });

        stop_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideTextView();
                stopSoundRecorder();
                stopAccelerometer();
            }
        });


        mInformationTextView = (TextView) findViewById(R.id.informationTextView);
        tokenTextView = (TextView) findViewById(R.id.token_field );

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void generateToken() {
        SessionIdentifierGenerator SIG = new SessionIdentifierGenerator(4);
        if(token == null)
            token  = SIG.nextSessionId();
        updateTextView(R.id.token_field, "Topic: " + token);

    }

    private void sendTestMessage() {
        try {
            topic = token;

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect", e);

        }
        if (!TextUtils.isEmpty(topic))
            Log.e(TAG, topic);
            SendRequest.send("/topics/" + topic, getString(R.string.testMessage), getApplicationContext());
    }

    private void startSoundRecorder() {
        if(mSensor == null) {
            mSensor = new SoundMeter();

            try {
                mSensor.start();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            handler = new Handler();

            final Runnable r = new Runnable() {

                public void run() {
                    //mSensor.start();
                    Log.d("Amplify", "HERE");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Get the volume from 0 to 255 in 'int'
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            double volume = 0;
                            if (mSensor != null) {
                                volume = 100 * mSensor.getTheAmplitude() / 32768;
                            }
                            checkIfCrying(volume);
                            int volumeToSend = (int) volume;
                            updateTextView(R.id.volume, "Volume: " + String.valueOf(volumeToSend));

                            handler.postDelayed(this, 250); // amount of delay between every cycle of volume level detection + sending the data  out
                        }
                    });
                }
            };

            // Is this line necessary? --- YES IT IS, or else the loop never runs
            // this tells Java to run "r"
            handler.postDelayed(r, 250);
        }
    }

    private void checkIfCrying(double volume) {
        if (!crying) {
            if (volume > 10) {
                counter++;
            } else if (counter > 0) {
                counter--;
            }
            if (counter == 20) {
                crying = true;
                counter = 0;
                wakeDevice();
                fullWakeLock.release();
                Log.e(TAG, topic);
                SendRequest.send("/topics/" + topic, getString(R.string.cryingMessage), getApplicationContext());
            }
        }

        if (crying) {
            if (volume < 10) {
                counter++;
            } else if (counter > 0) {
                counter--;
            }
            if (counter == 10) {
                crying = false;
                counter = 0;
                Log.e(TAG, topic);
                SendRequest.send("/topics/" + topic, getString(R.string.stopCrying), getApplicationContext());
            }
        }
    }

    private void startAccelerometer() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                // success! we have an accelerometer

                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.e(TAG, "Failed to collect data from accelerometer");
                // fail! we dont have an accelerometer!
            }
        }
    }

    private void stopAccelerometer() throws NullPointerException{
        if (sensorManager != null) {
            //displayCleanValues();
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
    }

    private void stopSoundRecorder() throws NullPointerException{
        if (mSensor != null) {
            mSensor.stop();
            mSensor = null;
        }
    }

    public void initializeViews() {
        //movesCount = (TextView) findViewById(R.id.moves_count);
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        if(fullWakeLock.isHeld()){
            fullWakeLock.release();
        }
        if(partialWakeLock.isHeld()){
            partialWakeLock.release();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));

    }

    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        partialWakeLock.acquire();
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();
        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            if(movingTime > 0 )
                movingTime = movingTime - SHAKE_WAIT_TIME_MS ;

            mShakeTime = now;


            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
            int moves = Math.round(gForce);
            //movesCount.setText(Integer.toString(moves));
            // Alert if gForce exceeds threshold;
            if(gForce > SHAKE_THRESHOLD) {
                movingCounter++;
                if(movingCounter >= 5 && !moving)
                {
                    moving = true;
                    Log.e(TAG, topic);
                    SendRequest.send("/topics/" + topic, getString(R.string.movingMessage), getApplicationContext());
                }
            }
            else if (movingTime == 0 && moving){
                moving = false;
                movingCounter = 0 ;
                movingTime = 10000;
                //SendRequest.send("/topics/" + topic, getString(R.string.stopMoving), getApplicationContext());
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // clean current values
        //displayCleanValues();
        // display the current x,y,z accelerometer values
        //displayCurrentValues();

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }


        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }
    }

    public void hideTextView() {
        TextView rec_text = (TextView) findViewById(R.id.textView );
        rec_text.setVisibility(View.INVISIBLE);
        updateTextView(R.id.textView, "");

    }

    public void recordingTextAnimation(){
        TextView rec_text = (TextView) findViewById(R.id.textView );
        updateTextView(R.id.textView, "Recording data...");
        rec_text.setVisibility(View.VISIBLE);

        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(100); //You can manage the time of the blink with this parameter
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        rec_text.startAnimation(anim);
    }


    /*Sound part*/
    public void updateTextView(int text_id, String toThis) {
        TextView val = (TextView) findViewById(text_id);
        val.setText(toThis);
    }

    protected void createWakeLocks(){
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "Loneworker - FULL WAKE LOCK");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loneworker - PARTIAL WAKE LOCK");
    }

    // Called whenever we need to wake up the device
    public void wakeDevice() {
        fullWakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public static String getToken() {
        return token;
    }
}

