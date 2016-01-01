package babymonitor.app;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class MyActivity extends ActionBarActivity implements SensorEventListener {

    private  int counter = 0;
    private boolean crying = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final float SHAKE_THRESHOLD = 1.04f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private long movingTime = 10000;
    private int movingCounter =0;

    private long mShakeTime = 0;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private TextView currentX, currentY, currentZ;

    /* Sound part */
    private PowerManager.WakeLock wl;
    private Handler handler;
    private SoundMeter mSensor;
    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
     /* Sound part END */


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }


    // Called when an options item is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemRecordingStart:
                //start recording
                startSoundRecorder();
                startAccelerometer();
                Toast.makeText(getBaseContext(), "Start Recording", Toast.LENGTH_SHORT).show();
                break;
            case R.id.itemRecordingStop:
                //stop recording
                stopSoundRecorder();
                stopAccelerometer();
                Toast.makeText(getBaseContext(), "Stop Recording", Toast.LENGTH_SHORT).show();
                break;

        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        createWakeLocks();


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
                            if (!crying) {
                                if (volume > 10) {
                                    counter++;
                                } else if (counter > 0) {
                                    counter--;
                                }
                                if (counter == 10) {
                                    crying = true;
                                    counter = 0;
                                    wakeDevice();
                                    fullWakeLock.release();
                                    Toast.makeText(getBaseContext(), "Crying!", Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(getBaseContext(), "Not Crying!", Toast.LENGTH_LONG).show();
                                }
                            }

                            int volumeToSend = (int) volume;
                            updateTextView(R.id.volumeLevel, "Volume: " + String.valueOf(volumeToSend));

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

    private void startAccelerometer() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                // success! we have an accelerometer

                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                // fai! we dont have an accelerometer!
            }
        }
    }

    private void stopAccelerometer() throws NullPointerException{
        if (sensorManager != null) {
            displayCleanValues();
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
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
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
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        partialWakeLock.acquire();
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
            currentZ.setText(Float.toString(gForce));
            // Alert if gForce exceeds threshold;
            if(gForce > SHAKE_THRESHOLD) {
                movingCounter++;
                if(movingCounter >= 5)
                {
                    Toast.makeText(getBaseContext(), "It's moving!", Toast.LENGTH_SHORT).show();
                }
            }
            else if (movingTime == 0){
                movingCounter = 0 ;
                movingTime = 10000;
                Toast.makeText(getBaseContext(), "It's  NOT moving!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // clean current values
        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues();

        deltaX = event.values[0];
        deltaY = event.values[1];
        deltaZ = event.values[2];


        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }


        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }
    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY ));
        currentZ.setText(Float.toString(movingCounter));
    }

    /*Sound part*/
    public void updateTextView(int text_id, String toThis) {

        TextView val = (TextView) findViewById(text_id);
        val.setText(toThis);

        return;
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
}
