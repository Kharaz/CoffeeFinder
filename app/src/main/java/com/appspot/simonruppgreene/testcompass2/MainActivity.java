package com.appspot.simonruppgreene.testcompass2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
//import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Canvas;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;


public class MainActivity extends Activity implements SensorEventListener,
                                                        GooglePlayServicesClient.ConnectionCallbacks,
                                                        GooglePlayServicesClient.OnConnectionFailedListener,
                                                        LocationListener
{
    Float azimuth;
    Float testAzimuth_pub;

    //double testLat = 40.014611;
    //double testLon = -105.22842;
    //double testLat = 39.999832;
    //double testLon = -105.262965;
    //39.997007, -105.262252
    double testLat = 39.997007;
    double testLon = -105.262252;


    CustomDrawableView mCustomDrawableView;

    /*
    LocationManager mLocationManager;
    Location currLocation;
    double lon;
    double lat;
    */
    public static SharedPreferences mPrefs;
    public static SharedPreferences.Editor mEditor;

    private static final int MS_PER_SEC = 1000;
    public static final int UPDATE_INTERVAL_SECONDS = 5;
    private static final long UPDATE_INTERVAL = MS_PER_SEC * UPDATE_INTERVAL_SECONDS;

    private static final int MAX_INTERVAL_SECONDS = 1;
    private static final long UPDATE_INTERVAL_MAX = MS_PER_SEC * MAX_INTERVAL_SECONDS;

    LocationClient mLocationClient;
    LocationRequest mLocationRequest;
    Location mCurrentLocation;
    boolean mUpdatesRequested;

    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    float[] mGravity;
    float[] mGeomagnetic;

    //lifecycle functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCustomDrawableView = new CustomDrawableView(this);
        setContentView(mCustomDrawableView);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();

        mLocationClient = new LocationClient(this, this, this);
        mUpdatesRequested = false;

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL_MAX);
        /*
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        currLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        */
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        if(mLocationClient.isConnected()){
            mLocationClient.removeLocationUpdates(this);
        }

        mLocationClient.disconnect();
        super.onStop();
    }


    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        if(mPrefs.contains("KEY_UPDATES_ON")) {
            mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);
        } else {
            mEditor.putBoolean("KEY_UPDATES_ON", false);
            mEditor.commit();
        }
    }

    protected void onPause() {
        mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
        mEditor.commit();

        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = sensorEvent.values;
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic = sensorEvent.values;
        }

        if(mGravity != null && mGeomagnetic != null){
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if(success){
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = orientation[0]; //azimuth, pitch, roll
                testAzimuth_pub = orientation[0] + (float) doMath();
            }
        }
        mCustomDrawableView.invalidate();
    }

    @Override //locationlistener callback
    public void onLocationChanged(Location location) {
        //report to UI that location is updated
        String msg = "Updated Location: " + Double.toString(location.getLatitude()) + ","
                + Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d("Location", location.toString());
    }

    public class CustomDrawableView extends View {
        Paint paint = new Paint();

        public CustomDrawableView(Context context){
            super(context);
            paint.setColor(0xff00ff00);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
        };

        protected void onDraw(Canvas canvas){
            int width = getWidth();
            int height = getHeight();

            //Float dist = (float) getDistanceToPoint();
            float[] dist = new float[3];
            Location.distanceBetween(mCurrentLocation.getLatitude(),
                                                  mCurrentLocation.getLongitude(),
                                                  testLat, testLon, dist);
            Float distance = Math.abs(dist[1]);

            //convert azimuth from rad to deg
            Float azimuth_deg = (-azimuth*180/3.14159f);
            Float testAzimuth = (float) doMath();
            Float testAzimuth_deg = (-testAzimuth*180/3.14159f);

            Float testAzimuth_pub_deg = (-testAzimuth_pub*180/3.14159f);

            int centerx = width/2;
            int centery = width/2;

            canvas.drawLine(centerx, 0, centery, height, paint);
            canvas.drawLine(centerx, -1000, centerx, +1000, paint);

            if(azimuth != null){
                //canvas.rotate(azimuth_deg, centerx, centery);
                //canvas.rotate(testAzimuth_deg, centerx, centery);
                canvas.rotate(testAzimuth_pub_deg, centerx, centery);
            }

            paint.setColor(0xff0000ff);
            canvas.drawLine(centerx, -1000, centerx, +1000, paint);
            canvas.drawLine(-1000, centery, 1000, centery, paint);

            canvas.drawText("N", centerx, centery - 10, paint);
            canvas.drawText("Heading: " + azimuth_deg.toString(), centerx, centery - 20, paint);

            canvas.drawText("S", centerx-10, centery+15, paint);
            canvas.drawText("Test: " + testAzimuth_deg.toString(), centerx, centery+15, paint);
            canvas.drawText("Distance: " + distance.toString() + " meters", centerx, centery + 25, paint);
            paint.setColor(0xff00ff00);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {} //sensor accuracy change callback

    //google play services stuff
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    public static class ErrorDialogFragment extends DialogFragment {
        private Dialog mDialog; //contains error dialog

        public ErrorDialogFragment(){
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog){
            mDialog = dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState){
            return mDialog;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch(resultCode){
                    case Activity.RESULT_OK:
                        break;
                }
        }
    }

    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", " Google Play services is available.");
            return true;
        } else {
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            if(errorDialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(this.getFragmentManager(), "Location Updates");
            }
            return false;
        }
    }

    //google play services location shenanigans
    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        mCurrentLocation = mLocationClient.getLastLocation();
        Log.d("Loc", mCurrentLocation.toString());

        if(mUpdatesRequested){
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }

    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()){
            try{
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            errorDialog.show();
            Log.d("Location Services", "Failed connection with error code " + Integer.toString(connectionResult.getErrorCode()));
        }
    }

    public double doMath() {
        if(mCurrentLocation != null){
            double x0 = mCurrentLocation.getLatitude();
            double y0 = mCurrentLocation.getLongitude();

            double x1 = testLat;
            double y1 = testLon;

            double beta = Math.atan((x1-x0)/(y1-y0));

            return beta;
        } else {
            return 0;
        }
    }

    public double getDistanceToPoint(){
        float a = (float) testLat - (float) mCurrentLocation.getLatitude();
        float b = (float) testLon - (float) mCurrentLocation.getLongitude();
        return Math.sqrt(Math.pow(a,2) + Math.pow(b,2));
    }
}
