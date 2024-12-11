package com.example.lab9;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private TextView dbm, dateTime, battery, locationText;
    private Button button;
    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        dbm = findViewById(R.id.dbm);
        dateTime = findViewById(R.id.dateTime);
        battery = findViewById(R.id.battery);
        locationText = findViewById(R.id.location);
        button = findViewById(R.id.button);

        // Demande des permissions si nécessaire
        if (!hasLocationPermission()) {
            requestLocationPermission();
        }

        if(!hasStoragePermission()){
            requestStoragePermission();
        }

        // Configuration du bouton
        button.setOnClickListener(view -> {
            // Mise à jour de l'heure et de la date
            getTimeAndDate();

            // Vérification et démarrage des fonctionnalités selon les permissions
            if (hasLocationPermission()) {
                setupSignalStrengthListener();
                setupBatteryListener();
                getLocation();
                saveDataCSV();
            } else {
                dbm.setText("Location permission not granted");
            }
        });
    }

    // Vérifie si la permission de localisation est accordée
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasStoragePermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Demande la permission de localisation
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    // Gestion de la réponse pour les permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            dbm.setText("Permission denied");
        }
    }

    // Affiche la date et l'heure actuelle
    private void getTimeAndDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateTime.setText(sdf.format(System.currentTimeMillis()));
    }

    // Configure le listener pour la force du signal
    private void setupSignalStrengthListener() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    int level = signalStrength.getLevel(); // Niveau 0 à 4
                    dbm.setText("Signal Strength Level: " + level);
                } else {
                    int dBm = -113 + 2 * signalStrength.getGsmSignalStrength();
                    dbm.setText("Signal Strength: " + dBm + " dBm");
                }

                // Sauvegarder les données mises à jour
                saveDataCSV();
            }
        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    // Configure le listener pour la batterie
    private void setupBatteryListener() {

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                battery.setText("Battery Level: " + level + "%");

                // Sauvegarder les données mises à jour
                saveDataCSV();
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    // Récupère la localisation
    @SuppressLint("MissingPermission")
    private void getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
    }

    private String lastSavedData = "";

    private void saveDataCSV() {
        try {
            if (!hasStoragePermission()) {
                requestStoragePermission();
                return;
            }

            // Récupérer les données actuelles
            String dateTimeText = dateTime.getText().toString();
            String dbmText = dbm.getText().toString();
            String batteryText = battery.getText().toString();
            String location = locationText.getText().toString();
            String data = dateTimeText + "," + dbmText + "," + batteryText + "," + location + "\n";
            Toast.makeText(this, "Save Data.", Toast.LENGTH_SHORT).show();
            // Éviter les doublons
            if (data.equals(lastSavedData)) return;
            lastSavedData = data;

            // Écriture dans le fichier
//            File directory = getExternalFilesDir(null); // Dossier privé à l'application
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File csvFile = new File(directory, "data.csv");

            FileWriter fileWriter = new FileWriter(csvFile, true); // 'true' pour ajouter au fichier existant
            fileWriter.append(data);
//            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        String latitude = String.format(Locale.getDefault(), "%.5f", location.getLatitude());
        String longitude = String.format(Locale.getDefault(), "%.5f", location.getLongitude());

        String locationDetails = "Latitude: " + latitude + ", Longitude: " + longitude;
        locationText.setText(locationDetails);

        // Sauvegarder les données mises à jour
        saveDataCSV();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
    }

    // Les méthodes non utilisées du listener peuvent être laissées vides
    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
