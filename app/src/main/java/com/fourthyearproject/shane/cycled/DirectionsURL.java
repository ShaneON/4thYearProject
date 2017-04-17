package com.fourthyearproject.shane.cycled;

import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;

public class DirectionsURL {

    public String makeDirectionsURL(String origin, String destination,
                                    LinkedList<LatLng> waypointList){

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin;
        if(null != waypointList && !waypointList.isEmpty()){
            url = url + "&waypoints=optimize:true";
            for(int i = 1; i < waypointList.size(); i++){
                url = url + "|" + Double.toString(waypointList.get(i).latitude) + "," +
                        Double.toString(waypointList.get(i).longitude);
            }
            url = url + "|&destination=" + destination; // should "&" be there ??????????????????????????????????????
        }
        return url + "&mode=bicycling";
    }
}
