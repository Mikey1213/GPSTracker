package michaelgomez.example.gpstracker;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.Calendar;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_time, tv_accuracy, tv_speed, tv_updates, fileIO;

    Switch sw_locationupdates;
    Button bt_clearFile;

    //Google's api for location services. The majority of the app function using this class.
    FusedLocationProviderClient fusedLocationProviderClient;

    LocationRequest locationRequest;

    LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationRequest = new LocationRequest();
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_time = findViewById(R.id.tv_time);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_updates = findViewById(R.id.tv_updates);
        tv_lat = findViewById(R.id.tv_lat);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        fileIO = findViewById(R.id.fileIO);
        bt_clearFile = findViewById(R.id.bt_clearFile);

        //set all properties of LocationRequest
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(20);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //event triggered whenever update interval is met (ex. 5 sec)
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                try {
                    updateUIValues(locationResult.getLastLocation());
                } catch (IOException e) {
                    //tv_accuracy.setText(e.getMessage());
                }
            }
        };

        bt_clearFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearGPSFile();
                readGPSFile();
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
        tv_time.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");

        // TODO: fix for separate file open/close functions
        //closeFile();
        //readGPSFile();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        Date currentTime = Calendar.getInstance().getTime();
        tv_time.setText(currentTime.toString());

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
                //tv_altitude.setText("write success " + Calendar.getInstance().getTime());
                myOutWriter.close();
                fOut.close();
            } catch (Exception e){
                //tv_accuracy.setText("error in writing to file");
            }
        }else {
            // else request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    // read from GPS file and print to File TextView
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
                //tv_accuracy.setText("exception in reading file");
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
                //tv_accuracy.setText("exception in clearing file: " + e.getMessage());
            }
        }
    }

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

}