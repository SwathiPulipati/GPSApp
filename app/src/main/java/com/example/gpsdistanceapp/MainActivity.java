package com.example.gpsdistanceapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gpsdistanceapp.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{
    ActivityMainBinding binding;
    double lat, lon, lastLat, lastLon, startLat = 0, startLon = 0;
    float totalDistance;
    long startTime, endTime, timeSpent;
    String lastAddress, address;
    ArrayList<VisitedLocations> locationsList;
    LocationsAdapter locationsAdapter;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.cardView.setBackgroundResource(R.drawable.card_view_background);

        String[] permissions = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"};
        ActivityCompat.requestPermissions(this,permissions, 4);

        locationsList = new ArrayList<>();

        if (savedInstanceState != null){
            address = savedInstanceState.getString("current address");
            binding.address.setText(address);

            startTime = savedInstanceState.getLong("start time");
            endTime = savedInstanceState.getLong("end time");
            timeSpent = savedInstanceState.getLong("time spent");

            lat = savedInstanceState.getDouble("current lat");
            lon = savedInstanceState.getDouble("current lon");
            binding.lat.setText("Latitude: " +lat);
            binding.lon.setText("Longitude: " +lon);

            lastLat = savedInstanceState.getDouble("last lat");
            lastLon = savedInstanceState.getDouble("last lon");
            startLat = savedInstanceState.getDouble("start lat");
            startLon = savedInstanceState.getDouble("start lon");

            totalDistance = savedInstanceState.getFloat("total distance");
            binding.distanceTextView.setText("Distance: " + determineDistance(lat, lon) + " meters");

            locationsList = savedInstanceState.getParcelableArrayList("locations list");
        }

        locationsAdapter = new LocationsAdapter(locationsList);
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(locationsAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener();

        binding.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLat = lat;
                startLon = lon;
            }
        });

        binding.resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLat = 0;
                startLon = 0;
                totalDistance = 0;
            }
        });

    }



    public class LocationListener implements android.location.LocationListener{

        @Override
        public void onLocationChanged(@NonNull Location location) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            binding.lat.setText("Latitude: " +lat);
            binding.lon.setText("Longitude: " +lon);

            binding.distanceTextView.setText("Distance: " + determineDistance(lat, lon) + " meters");

            address = determineAddress(lat, lon);
            binding.address.setText(address);

            if(!address.equals(lastAddress)){
                if (lastAddress != null){
                    endTime = System.nanoTime();
                    timeSpent = endTime - startTime;
                    if (inList(address) == -1){
                        locationsList.add(new VisitedLocations(address));
                        if (timeSpent != 0 && locationsList.size() > 1)
                            locationsList.get(inList(lastAddress)).setTimeSpent(timeSpent);
                    }
                    else{
                        if(timeSpent != 0)
                            locationsList.get(inList(lastAddress)).setTimeSpent(timeSpent);
                        locationsList.get(inList(address)).addVisit();
                    }

                }

                startTime = System.nanoTime();
                locationsAdapter.notifyDataSetChanged();
            }

            lastAddress = address;

        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4) {
            for(int i = 0; i<permissions.length; i++){
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,1, locationListener);
                }
            }
        }
    }

    public String determineAddress(double lat, double lon){
        try {
            Geocoder geocoder = new Geocoder(this, Locale.US);
            ArrayList<Address> addresses = (ArrayList) geocoder.getFromLocation(lat, lon, 1);
            return addresses.get(0).getAddressLine(0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public float determineDistance(double lat, double lon){
        float [] results = new float[1];
        if(lastLat != 0.0 && lastLon != 0.0){
            Location.distanceBetween(lastLat, lastLon, lat, lon, results);
            Log.d("RESULTS", String.valueOf(results[0]));
        }
        lastLat = lat;
        lastLon = lon;

        if (startLat == 0 && startLon == 0){
            return results[0];
        }

        totalDistance += results[0];
        return totalDistance;
    }

    public int inList(String address){
        for (int i=0; i<locationsList.size(); i++){
            if (address.equals(locationsList.get(i).getAddress()))
                return i;
        }
        return -1;
    }

    public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.ViewHolder>{

        private ArrayList<VisitedLocations> list;

        public class ViewHolder extends RecyclerView.ViewHolder{
            public TextView address, timeSpent, timesVisited;
            public CardView locationsCardView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                address = itemView.findViewById(R.id.visitedAddressTextView);
                timeSpent = itemView.findViewById(R.id.timeSpentTextView);
                timesVisited = itemView.findViewById(R.id.timesVisitedTextView);
                locationsCardView = itemView.findViewById(R.id.locationsCardView);
            }
        }

        public LocationsAdapter(ArrayList<VisitedLocations> list){
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View locationsView = inflater.inflate(R.layout.visited_locations_layout, parent, false);
            ViewHolder viewHolder = new ViewHolder(locationsView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VisitedLocations location = list.get(position);

            holder.locationsCardView.setBackgroundResource(R.drawable.card_view_background);

            holder.address.setText(location.getAddress());

            holder.timeSpent.setText(location.getTimeSpent()+ " seconds spent.");

            holder.timesVisited.setText("Visited " +location.getTimesVisited()+ " times.");

        }

        @Override
        public int getItemCount() {
            return list.size();
        }


    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("locations list", locationsList);
        outState.putString("current address", address);
        outState.putLong("start time", startTime);
        outState.putLong("end time", endTime);
        outState.putLong("time spent", timeSpent);
        outState.putDouble("current lat", lat);
        outState.putDouble("current lon", lon);
        outState.putDouble("last lat", lastLat);
        outState.putDouble("last lon", lastLon);
        outState.putDouble("start lat", startLat);
        outState.putDouble("start lon", startLon);
        outState.putFloat("total distance", totalDistance);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }
}