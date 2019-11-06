package com.example.cs160_prog2b;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.libraries.places.api.model.Place;

public class Station implements Parcelable {
    String placeID;
    double latitude;
    double longitude;
    Place place;
    int minToGetThere;
    int price; //cents
    int wattage;
    double distance;
    int available;
    double chargeHours;
    double chargeMins;


    Station (){    }

    public Station(String placeID, double latitude, double longitude) {
        this.placeID = placeID;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setPlace(Place place){
        this.place = place;
    }

    protected Station(Parcel in) {

        placeID = in.readString();
        latitude  = in.readDouble();
        longitude = in.readDouble();
        minToGetThere = in.readInt();
        price = in.readInt();
        distance = in.readDouble();
        available = in.readInt();
        chargeHours = in.readDouble();
        chargeMins = in.readDouble();
        wattage = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    public static Creator<Station> getCREATOR() {
        return CREATOR;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeString(placeID);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeInt(minToGetThere);
        parcel.writeInt(price);
        parcel.writeDouble(distance);
        parcel.writeInt(available);
        parcel.writeDouble(chargeHours);
        parcel.writeDouble(chargeMins);
        parcel.writeInt(wattage);

    }
}
