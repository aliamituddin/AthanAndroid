package com.gallopdevs.athanhelper.views;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gallopdevs.athanhelper.R;
import com.gallopdevs.athanhelper.model.PrayTime;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * A simple {@link Fragment} subclass.
 */
public class DayViewFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static final int DEFAULT_CALC_METHOD = 2;
    private static final int DEFAULT_JURISTIC_METHOD = 0;
    private static final int DEFAULT_HIGH_LATITUDES = 0;
    private static final int DEFAULT_TIME_FORMAT = 1;

    private final String KEY_PREF_CALC_METHOD = "calculation_method";
    private final String KEY_PREF_JURISTIC_METHOD = "juristic_method";
    private final String KEY_PREF_HIGH_LATITUDES = "high_latitudes";
    private final String KEY_PREF_TIME_FORMATS = "time_formats";

    private PrayTime prayerTime = new PrayTime();

    private Calendar nextDay = Calendar.getInstance();
    private int dstOffset = nextDay.get(Calendar.DST_OFFSET) / 3600000;
    private int timeZoneOffset = nextDay.get(Calendar.ZONE_OFFSET) / 3600000 + dstOffset;

    private ArrayList<String> nextDayTimes = new ArrayList<>();

    @BindView(R.id.dawnTextView)
    TextView dawnTextView;
    @BindView(R.id.dawnTimeTextView)
    TextView dawnTimeTextView;
    @BindView(R.id.middayTextView)
    TextView middayTextView;
    @BindView(R.id.middayTimeTextView)
    TextView middayTimeTextView;
    @BindView(R.id.afternoonTextView)
    TextView afternoonTextView;
    @BindView(R.id.afternoonTimeTextView)
    TextView afternoonTimeTextView;
    @BindView(R.id.sunsetTextView)
    TextView sunsetTextView;
    @BindView(R.id.sunsetTimeTextView)
    TextView sunsetTimeTextView;
    @BindView(R.id.nightTextView)
    TextView nightTextView;
    @BindView(R.id.nightTimeTextView)
    TextView nightTimeTextView;
    @BindView(R.id.gridLayout)
    GridLayout gridLayout;
    @BindView(R.id.dayTextView)
    TextView dayTextView;
    Unbinder unbinder;

    private FusedLocationProviderClient mFusedLocationClient;
    private double latitude;
    private double longitude;

    public DayViewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        unbinder = ButterKnife.bind(this, view);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        if (hasPermissions()) {
            getLocation();
        } else {
            requestPerms();
        }


        // settings listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        prayerTime.setAsrJuristic(DEFAULT_JURISTIC_METHOD);
        prayerTime.setCalcMethod(DEFAULT_CALC_METHOD);
        prayerTime.setAdjustHighLats(DEFAULT_HIGH_LATITUDES);
        prayerTime.setTimeFormat(DEFAULT_TIME_FORMAT);

        Bundle bundle = getArguments();
        formatDate(bundle);
        formatPrayers(bundle);
        updateView();

        return view;
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    private boolean hasPermissions() {
        int res = 0;
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        for (String perms : permissions) {
            res = getActivity().checkCallingOrSelfPermission(perms);
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
            getLocation();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(getActivity(), "Location permissions denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void formatPrayers(Bundle bundle) {
        int count = bundle.getInt("count");
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int year = c.get(Calendar.YEAR);

        nextDay.set(year, month, dayOfMonth + count);
        nextDayTimes = prayerTime.getPrayerTimes(nextDay, latitude, longitude, timeZoneOffset);
    }

    private void updateView() {
        String newDawnTime = nextDayTimes.get(0).replaceFirst("^0+(?!$)", "");
        String newMiddayTime = nextDayTimes.get(2).replaceFirst("^0+(?!$)", "");
        String newAfternoonTime = nextDayTimes.get(3).replaceFirst("^0+(?!$)", "");
        String newSunsetTime = nextDayTimes.get(5).replaceFirst("^0+(?!$)", "");
        String newNightTime = nextDayTimes.get(6).replaceFirst("^0+(?!$)", "");
        if (prayerTime.getTimeFormat() == 0) {
            dawnTimeTextView.setText(nextDayTimes.get(0));
            middayTimeTextView.setText(nextDayTimes.get(2));
            afternoonTimeTextView.setText(nextDayTimes.get(3));
            sunsetTimeTextView.setText(nextDayTimes.get(5));
            nightTimeTextView.setText(nextDayTimes.get(6));
        } else {
            dawnTimeTextView.setText(newDawnTime);
            middayTimeTextView.setText(newMiddayTime);
            afternoonTimeTextView.setText(newAfternoonTime);
            sunsetTimeTextView.setText(newSunsetTime);
            nightTimeTextView.setText(newNightTime);
        }
    }

    private void formatDate(Bundle bundle) {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int year = c.get(Calendar.YEAR);
        int weekDay = bundle.getInt("day");
        int count = bundle.getInt("count");
        nextDay.set(year, month, dayOfMonth + count);
        int numberDay = nextDay.get(Calendar.DAY_OF_MONTH);
        int monthDay = nextDay.get(Calendar.MONTH) + 1;

        String numberString = String.valueOf(numberDay);

        String monthString;

        if (monthDay < 10) {
            monthString = "0" + String.valueOf(monthDay);
        } else {
            monthString = String.valueOf(monthDay);
        }

        if (weekDay >= 8) {
            weekDay = weekDay - 7;
        }

        String weekDayString;

        switch (weekDay) {
            case 1:
                weekDayString = "Sunday";
                break;
            case 2:
                weekDayString = "Monday";
                break;
            case 3:
                weekDayString = "Tuesday";
                break;
            case 4:
                weekDayString = "Wednesday";
                break;
            case 5:
                weekDayString = "Thursday";
                break;
            case 6:
                weekDayString = "Friday";
                break;
            case 7:
                weekDayString = "Saturday";
                break;
            default:
                weekDayString = "This is not a day";
                break;
        }
        if (numberDay < 10) {
            dayTextView.setText(weekDayString + " " + monthString + "/0" + numberString);
        } else {
            dayTextView.setText(weekDayString + " " + monthString + "/" + numberString);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case KEY_PREF_CALC_METHOD:
                String calcMethodString = sharedPreferences.getString(KEY_PREF_CALC_METHOD, "");
                prayerTime.setCalcMethod(Integer.parseInt(calcMethodString));
                break;
            case KEY_PREF_JURISTIC_METHOD:
                String juristicMethodString = sharedPreferences.getString(KEY_PREF_JURISTIC_METHOD, "");
                prayerTime.setAsrJuristic(Integer.parseInt(juristicMethodString));
                break;
            case KEY_PREF_HIGH_LATITUDES:
                String highLatitudesString = sharedPreferences.getString(KEY_PREF_HIGH_LATITUDES, "");
                prayerTime.setAdjustHighLats(Integer.parseInt(highLatitudesString));
                break;
            case KEY_PREF_TIME_FORMATS:
                String timeFormatsString = sharedPreferences.getString(KEY_PREF_TIME_FORMATS, "");
                prayerTime.setTimeFormat(Integer.parseInt(timeFormatsString));
                break;
        }
        nextDayTimes = prayerTime.getPrayerTimes(nextDay, latitude, longitude, timeZoneOffset);
        updateView();
    }
}
