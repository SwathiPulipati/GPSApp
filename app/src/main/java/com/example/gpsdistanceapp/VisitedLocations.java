package com.example.gpsdistanceapp;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class VisitedLocations implements Parcelable{
    String address;
    int timesVisited;
    long timeSpent;

    public VisitedLocations(String address){
        this.address = address;
        timesVisited = 1;
        timeSpent = 0;
    }

    public VisitedLocations(String address, int timesVisited, long timeSpent){
        this.address = address;
        this.timesVisited = timesVisited;
        this.timeSpent = timeSpent;
    }

    public void addVisit(){
        timesVisited++;
    }

    public int getTimesVisited() {
        return timesVisited;
    }

    public String getAddress() {
        return address;
    }

    public void setTimeSpent(long timeSpent) {
        this.timeSpent += timeSpent;
    }

    public int getTimeSpent() {
        return (int) (timeSpent/1000000000);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeInt(timesVisited);
        parcel.writeLong(timeSpent);
    }

    public Creator<VisitedLocations> CREATOR = new Creator<VisitedLocations>(){
        @Override
        public VisitedLocations createFromParcel(Parcel parcel) {
            return new VisitedLocations(address, timesVisited, timeSpent);
        }

        @Override
        public VisitedLocations[] newArray(int i) {
            return new VisitedLocations[i];
        }
    };
}
