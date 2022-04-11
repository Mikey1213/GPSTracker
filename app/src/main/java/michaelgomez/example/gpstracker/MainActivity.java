package michaelgomez.example.gpstracker;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, fileIO;

    Switch sw_locationupdates, sw_gps;

    //Google's api for location services. The majority of the app function using this class.
    FusedLocationProviderClient fusedLocationProviderClient;

    boolean updateOn = true; // TODO: what is this for?

    LocationRequest locationRequest;

    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationRequest = new LocationRequest();
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_gps = findViewById(R.id.sw_gps);
        tv_lat = findViewById(R.id.tv_lat);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        fileIO = findViewById(R.id.fileIO);

        //set all properties of LocationRequest
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(20);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //event triggered whenever update interval is met (ex. 5 sec)
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                try {
                    updateUIValues(locationResult.getLastLocation());
                } catch (IOException e) {
                    tv_altitude.setText(e.getMessage());
                }
            }
        };

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()) {
                    // open file if not already open
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
    } //end onCreate method

    private void stopLocationUpdates() {
        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        //tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        //tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");

        // TODO: fix for separate file open/close functions
        //closeFile();
        //readGPSFile();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        //TODO: fix for separate file open/close functions
        /*if (!isFileOpen) {
            //openFile();
        }*/

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // TODO: what is this for^^^^
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                } else {
                    Toast.makeText(this, "This app requires permission to be granted in order to wkr properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }


    private void updateGPS() {
        //get permissions from the user to track GPS
        //get the current location from the fused client
        //update the UI, set all properties in their associated text view items

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //the user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                //we got permissions. Put the values of location. XXX (lat, long etc.) into the UI components
                tv_updates.setText("Location is being tracked");
                try {
                    updateUIValues(location);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            //permissions not yet granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) throws IOException {
        //update all of the text view objects to a new location
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        /*
        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        } else {
            tv_altitude.setText("No altitude available");
        }*/

        /*
        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Not available");
        }*/

        Date currentTime = Calendar.getInstance().getTime();
        tv_address.setText(currentTime.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           writeToFile("\nLat: " + String.valueOf(location.getLatitude()) + "\nLon: " + String.valueOf(location.getLongitude()));
            sendSms("Lat: " + String.valueOf(location.getLatitude()) + "\nLon: " + String.valueOf(location.getLongitude()));
            readGPSFile();
        }
    }

    private void writeToFile(String toAdd) {
        if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File storageDir = this.getExternalFilesDir(null);
            File myFile = new File(storageDir , "testGPS.txt");
            try {
                if (!myFile.exists()) {
                    myFile.createNewFile();
                    tv_speed.setText("New file created " + Calendar.getInstance().getTime());
                }
                FileOutputStream fOut = new FileOutputStream(myFile, true);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(toAdd);
                tv_altitude.setText("write success " + Calendar.getInstance().getTime());
                myOutWriter.close();
                fOut.close();
            } catch (Exception e){
                tv_altitude.setText("error in writing to file");
            }
        }else {
            // else request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void readGPSFile() {
        if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File storageDir = this.getExternalFilesDir(null);
            File myFile = new File(storageDir , "testGPS.txt");
            // open file^^
            try {
                FileInputStream toRead = new FileInputStream(myFile);
                Scanner reader = new Scanner(toRead);
                StringWriter text = new StringWriter();
                while (reader.hasNext()) {
                    text.append(reader.next());
                }
                fileIO.setText(text.toString() + Calendar.getInstance().getTime());

                text.close();
                reader.close();
                toRead.close();
            } catch (Exception e) {
                tv_altitude.setText("exception in reading file");
            }
        }else {
            // else request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    void clearGPSFile() {
        File storageDir = this.getExternalFilesDir(null);
        File myFile = new File(storageDir , "testGPS.txt");
        if (myFile.exists()) {
            try {
                FileOutputStream fOut = new FileOutputStream(myFile);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append("");
                myOutWriter.close();
                fOut.close();
            } catch (IOException e) {
                tv_altitude.setText("exception in clearing file: " + e.getMessage());
            }
        }


    }

    //TODO: add a clear file function, and a clear file button/switch?

    // TODO: Attempt at separated open/close functions... do we need this?
    //File IO variables that would be required:
    /*
    File externalStorageDir;
    File myFile;
    OutputStreamWriter myOutWriter;
    FileOutputStream fOut;*/
    //boolean isFileOpen = false;

    /*
    private void openFile() {
        // initialize IO variables, and open file called "testGPS.txt"
        if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            externalStorageDir = Environment.getExternalStorageDirectory();
            myFile = new File(externalStorageDir , "testGPS.txt");
            try {
                myFile.createNewFile();
                fOut = new FileOutputStream(myFile);
                myOutWriter = new OutputStreamWriter(fOut);
                isFileOpen = true;
            } catch (Exception e){
                tv_altitude.setText(e.getMessage());
            }
        }else {
            // else request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void closeFile() {
*/
    private void sendSms(String text) {
        //SmsManager sms = SmsManager.getDefault();
        //sms.sendTextMessage("7247133759",null,text,null,null);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage("smsto: 19258225557",null,text,null,null);
            Toast.makeText(getApplicationContext(),"Sent successfully!",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Massive fail", Toast.LENGTH_SHORT).show();
        }
    }

/*
    private String readGPSFile(File myFile) {
        try {
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            tv_altitude.setText(e.getMessage());
        }
        isFileOpen = false;
    }

    private void writeToFile(String toAdd) {
        openFile();
        if(myFile.exists()) {
            try {
                myOutWriter.append(toAdd);
            } catch(Exception e) {
                tv_altitude.setText(e.getMessage());
            }
        }
        else {
            //openFile(); // file should already be open at this point
        }
        closeFile();
    }
    */
    /*
    private void readGPSFile() {
        if (ActivityCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            externalStorageDir = Environment.getExternalStorageDirectory();
            myFile = new File(externalStorageDir , "testGPS.txt");
            // open file^^
            try {
                FileInputStream toRead = new FileInputStream(myFile);
                Scanner reader = new Scanner(toRead);
                StringWriter text = new StringWriter();
                while (reader.hasNext()) {
                    text.append(reader.next());
                }
                tv_speed.setText(text.toString());
                text.close();
                reader.close();
                toRead.close();
            } catch (Exception e) {
                tv_altitude.setText(e.getMessage());
            }
        }else {
            // else request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
            }
        }
        tv_speed.setText("failed to read file");
    } */
}