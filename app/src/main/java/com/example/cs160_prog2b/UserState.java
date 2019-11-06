package com.example.cs160_prog2b;

import java.util.HashMap;

public class UserState {

    public Vehicle car;
    int batteryLevel;
    int currentZip;


    UserState() {
        car = new Vehicle();
        currentZip = 94706;
        batteryLevel = 19;
    }


    class Vehicle {
        String carName;
        int carCode;
        int maxBattery;
        int chargeRate22;
        int chargeRate50;
        HashMap<Integer, Integer> CodeToBattery = new HashMap<>();
        HashMap<Integer, Integer> CodeToRate22 = new HashMap<>();
        HashMap<Integer, Integer> CodeToRate50 = new HashMap<>();
        HashMap<Integer, String> CodeToCar = new HashMap<>();

        Vehicle() {
            CodeToCar.put(0, "Nissan LEAF 3.ZERO e+");
            CodeToCar.put(1, "Renault Zoe R110 ZE50");
            CodeToCar.put(2, "Volkswagen ID.3");

            CodeToBattery.put(0, 239);// miles
            CodeToBattery.put(1, 242);
            CodeToBattery.put(2, 205);

            CodeToRate22.put(0, 23); // miles per hour
            CodeToRate22.put(1, 87);
            CodeToRate22.put(2, 29);

            CodeToRate50.put(0, 172); // miles per hour
            CodeToRate50.put(1, 178);
            CodeToRate50.put(2, 202);

            carCode = 0;
            carName = CodeToCar.get(carCode);
            // set max battery
            maxBattery = CodeToBattery.get(carCode);
            chargeRate22 = CodeToRate22.get(carCode);
            chargeRate50 = CodeToRate50.get(carCode);
        }

        void changeVehicle(int code) {
            carCode = code;
            carName = CodeToCar.get(carCode);

            // update max battery
            maxBattery = CodeToBattery.get(carCode);
            chargeRate50 = CodeToRate50.get(carCode);
        }
    }
}
