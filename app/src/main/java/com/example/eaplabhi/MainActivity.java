package com.example.eaplabhi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private String fileName;


    private boolean isRecording = false;
    private boolean isPaused = false;
    private int dataCounter = 0;
    private List<Location> locationList = new ArrayList<>();

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnPause = findViewById(R.id.btnPause);
        Button btnFinish = findViewById(R.id.btnFinish);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileNameDialog();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPaused = !isPaused;
                if (isPaused) {
                    showToast("Recording paused");
                    stopLocationUpdates();
                } else {
                    showToast("Recording resumed");
                    startLocationUpdates();
                }
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToast("Total recorded points: " + dataCounter);
                stopLocationUpdates();
                // Implement logic to create a file with recorded data
                if (isRecording) {
                    isRecording = false;
                    createExcelFile(fileName);

                }
            }

        });

        createLocationCallback();
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Excel Sheet Name");

        // Set up the input
        final EditText input = new EditText(this);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                startRecording(fileName);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void startRecording(String fileName) {
        if (checkLocationPermission()) {
            showToast("Recording started");
            isRecording = true;
            isPaused = false;
            dataCounter = 0;
            this.fileName=fileName;
            startLocationUpdates();
        }
    }
    private void createExcelFile(String fileName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("LocationData");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Timestamp");
        headerRow.createCell(1).setCellValue("Latitude");
        headerRow.createCell(2).setCellValue("Longitude");

        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < locationList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Location location = locationList.get(i);
            row.createCell(0).setCellValue(dateFormat.format(new Date()));
            row.createCell(1).setCellValue(location.getLatitude());
            row.createCell(2).setCellValue(location.getLongitude());
        }

        try {
            String filePath = getExternalFilesDir(null) + "/" + fileName + ".xlsx";
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            showToast("Excel file created: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Error creating Excel file");
        }
    }


    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (isRecording && !isPaused) {
                    Location location = locationResult.getLastLocation();
                    // Process the location data as needed
                    dataCounter++;
                    showToast("Recorded point: " + dataCounter);
                    Log.d("LocationData","Lat:"+location.getLatitude()+",long:"+location.getLongitude());
                    locationList.add(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if(checkLocationPermission()) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(500); // Update interval in milliseconds
            locationRequest.setFastestInterval(500); // Fastest update interval in milliseconds
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Location permission granted. Click Start again.");
            } else {
                showToast("Location permission denied");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}



