package com.gallopdevs.athanhelper.views;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.gallopdevs.athanhelper.R;
import com.gallopdevs.athanhelper.model.PrayTime;
import com.gallopdevs.athanhelper.utils.CalendarPrayerTimes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimerActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "TimerActivity";

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static final int DEFAULT_CALC_METHOD = 2;
    private static final int DEFAULT_JURISTIC_METHOD = 0;
    private static final int DEFAULT_HIGH_LATITUDES = 0;
    private static final int DEFAULT_TIME_FORMAT = 1;
    private static final int NEXT_DAY_TIMES = 1;

    private static final String KEY_PREF_CALC_METHOD = "calculation_method";
    private static final String KEY_PREF_JURISTIC_METHOD = "juristic_method";
    private static final String KEY_PREF_HIGH_LATITUDES = "high_latitudes";
    private static final String KEY_PREF_TIME_FORMATS = "time_formats";

    private CountDownTimer timer;

    private int currentTimeIndex = 0;

    private long difference1 = 0;
    private long difference2 = 0;
    private long difference3 = 0;
    private long difference4 = 0;
    private long difference5 = 0;
    private long difference6 = 0;
    private long[] differences = {difference1, difference2, difference3, difference4, difference5, difference6};

    private PrayTime prayerTime;

    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.prayerTimer)
    TextView prayerTimer;
    @BindView(R.id.bottom_nav)
    BottomNavigationViewEx bottomNav;

    private FusedLocationProviderClient mFusedLocationClient;
    private double latitude;
    private double longitude;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        ButterKnife.bind(this);
        init();

        getLocation();

//        startNewTimer();
    }

    private void init() {
        // bottom nav init
        bottomNav.enableAnimation(false);
        bottomNav.enableShiftingMode(false);
        bottomNav.enableItemShiftingMode(false);

        // location listener
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // settings listener
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // set default settings
        prayerTime = PrayTime.getInstance();
        prayerTime.setCalcMethod(DEFAULT_CALC_METHOD);
        prayerTime.setAsrJuristic(DEFAULT_JURISTIC_METHOD);
        prayerTime.setAdjustHighLats(DEFAULT_HIGH_LATITUDES);
        prayerTime.setTimeFormat(DEFAULT_TIME_FORMAT);
    }

    private void initSwipeAdapter() {
        DayViewAdapter dayViewAdapter = new DayViewAdapter(getSupportFragmentManager());
        viewPager.setAdapter(dayViewAdapter);
    }

    private void getLocation() {
        if (hasPermissions()) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d(TAG, "onSuccessTimerActivity: this is happening");
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                Log.d(TAG, "onSuccessTimerActivity: latitude: " + String.valueOf(latitude) + " longitude: " + String.valueOf(longitude));
                                CalendarPrayerTimes.setLatitude(latitude);
                                CalendarPrayerTimes.setLongitude(longitude);
                                initSwipeAdapter();
                            } else {
                                Toast.makeText(TimerActivity.this, "We cannot find your location. Please enable in settings.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(TimerActivity.this, "We cannot find your location. Please enable in settings.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: " + e.getMessage());
                        }
                    });
        } else {
            requestPerms();
        }

    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    private boolean hasPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        for (String perms : permissions) {
            int res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                for (int res : grantResults) {
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }
                break;
            default:
                allowed = false;
                break;
        }
        if (allowed) {
            timer.cancel();
            getLocation();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "LocationOfPrayer permissions denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private long[] getTimerDifference() {

        ArrayList<String> newTimes = CalendarPrayerTimes.getNewTimes();
        ArrayList<String> nextDayTimes = CalendarPrayerTimes.getNextDayTimes(NEXT_DAY_TIMES);

        // get currentTime and set format
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        final String currentTime = simpleDateFormat.format(Calendar.getInstance().getTime());

        // format times received from PrayTime model
        String dawnTime = newTimes.get(0) + ":00";
        String middayTime = newTimes.get(2) + ":00";
        String afternoonTime = newTimes.get(3) + ":00";
        String sunsetTime = newTimes.get(4) + ":00";
        String nightTime = newTimes.get(6) + ":00";
        String nextDawnTime = nextDayTimes.get(0) + ":00";
        try {
            // get milliseconds from parsing dates
            long dawnMillis = simpleDateFormat.parse(dawnTime).getTime();
            long middayMillis = simpleDateFormat.parse(middayTime).getTime();
            long afMillis = simpleDateFormat.parse(afternoonTime).getTime();
            long sunsetMillis = simpleDateFormat.parse(sunsetTime).getTime();
            long nightMillis = simpleDateFormat.parse(nightTime).getTime();
            long nextDawnMillis = simpleDateFormat.parse(nextDawnTime).getTime();

            //get milliseconds from parsing currentTime
            long currentTimeMilliSeconds = simpleDateFormat.parse(currentTime).getTime();

            //get intervals between times
            difference1 = dawnMillis - currentTimeMilliSeconds;
            difference2 = middayMillis - currentTimeMilliSeconds;
            difference3 = afMillis - currentTimeMilliSeconds;
            difference4 = sunsetMillis - currentTimeMilliSeconds;
            difference5 = nightMillis - currentTimeMilliSeconds;
            difference6 = nextDawnMillis - currentTimeMilliSeconds + 86400000;

            // set index of each element in differences array
            differences[0] = difference1;
            differences[1] = difference2;
            differences[2] = difference3;
            differences[3] = difference4;
            differences[4] = difference5;
            differences[5] = difference6;
            return differences;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        differences[0] = difference1;
        differences[1] = difference2;
        differences[2] = difference3;
        differences[3] = difference4;
        differences[4] = difference5;
        differences[5] = difference6;
        return differences;
    }

    private void startNewTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(getTimerDifference()[getNextTime()], 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // format for prayerTimer
                SimpleDateFormat offset = new SimpleDateFormat("HH:mm:ss", Locale.US);
                offset.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date newDateTimer = new Date();
                newDateTimer.setTime(millisUntilFinished);
                prayerTimer.setText(offset.format(newDateTimer) + "s");
            }

            @Override
            public void onFinish() {
                prayerTimer.setText("00:00:00s");

                // TODO send notification

                startNewTimer();
            }
        }.start();
    }

    private int getNextTime() {
        for (int i = 0; i < differences.length; i++) {
            if (differences[i] < 0) {
                currentTimeIndex = i + 1;
                if (currentTimeIndex > 5) {
                    currentTimeIndex = 0;
                }
            }
        }
        return currentTimeIndex;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_PREF_CALC_METHOD:
                String calcMethod = sharedPreferences.getString(KEY_PREF_CALC_METHOD, "");
                prayerTime.setCalcMethod(Integer.parseInt(calcMethod));
                break;
            case KEY_PREF_JURISTIC_METHOD:
                String juristicMethod = sharedPreferences.getString(KEY_PREF_JURISTIC_METHOD, "");
                prayerTime.setAsrJuristic(Integer.parseInt(juristicMethod));
                break;
            case KEY_PREF_HIGH_LATITUDES:
                String highLatitudes = sharedPreferences.getString(KEY_PREF_HIGH_LATITUDES, "");
                prayerTime.setAdjustHighLats(Integer.parseInt(highLatitudes));
                break;
            case KEY_PREF_TIME_FORMATS:
                String timeFormatsString = sharedPreferences.getString(KEY_PREF_TIME_FORMATS, "");
                prayerTime.setTimeFormat(Integer.parseInt(timeFormatsString));
                break;
        }
//        startNewTimer();
    }
}
