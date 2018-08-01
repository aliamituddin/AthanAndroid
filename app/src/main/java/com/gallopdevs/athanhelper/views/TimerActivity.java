package com.gallopdevs.athanhelper.views;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.gallopdevs.athanhelper.R;
import com.gallopdevs.athanhelper.model.PrayTime;
import com.gallopdevs.athanhelper.settings.SettingsActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    private static final String KEY_PREF_CALC_METHOD = "calculation_method";
    private static final String KEY_PREF_JURISTIC_METHOD = "juristic_method";
    private static final String KEY_PREF_HIGH_LATITUDES = "high_latitudes";

    private CountDownTimer timer;

    private int currentTimeIndex = 0;

    private ArrayList<String> newTimes = new ArrayList<>();
    private ArrayList<String> nextDayTimes = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        ButterKnife.bind(this);
        init();

        // ask for permissions
        if (hasPermissions()) {
            getLocation();
        } else {
            requestPerms();
        }

        searchPrayerTimes();
        startNewTimer();
    }

    private void init() {
        // bottom nav init
        bottomNav.enableAnimation(false);
        bottomNav.enableShiftingMode(false);
        bottomNav.enableItemShiftingMode(false);

        // location listener
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // settings listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // set default settings
        prayerTime = new PrayTime();
        prayerTime.setCalcMethod(DEFAULT_CALC_METHOD);
        prayerTime.setAsrJuristic(DEFAULT_JURISTIC_METHOD);
        prayerTime.setAdjustHighLats(DEFAULT_HIGH_LATITUDES);

        // init swipeAdapter
        DayViewAdapter dayViewAdapter = new DayViewAdapter(getSupportFragmentManager());
        viewPager.setAdapter(dayViewAdapter);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPerms();
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        });
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
                    Toast.makeText(this, "Location permissions denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void searchPrayerTimes() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        int dstOffset = calendar.get(Calendar.DST_OFFSET) / 3600000;
        int timeZoneOffset = calendar.get(Calendar.ZONE_OFFSET) / 3600000 + dstOffset;
        newTimes = prayerTime.getPrayerTimes(calendar, latitude, longitude, timeZoneOffset);

        calendar.set(year, month, dayOfMonth + 1);
        nextDayTimes = prayerTime.getPrayerTimes(calendar, latitude, longitude, timeZoneOffset);
    }

    private long[] getTimerDifference() {

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
        } else {
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
        }
        searchPrayerTimes();
        startNewTimer();
    }
}
