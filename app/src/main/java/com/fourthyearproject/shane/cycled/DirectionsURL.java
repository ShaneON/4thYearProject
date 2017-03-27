package com.fourthyearproject.shane.cycled;

import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;

/**
 * Created by hp on 22/03/2017.
 */

public class DirectionsURL {

    public String makeDirectionsURL(String origin, String destination,
                                    LinkedList<LatLng> waypointList){

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin + "&destination=" + destination;
        if(null != waypointList && !waypointList.isEmpty()){
            url = url + "&waypoints=" +
                    "via:" + Double.toString(waypointList.get(0).latitude) + "%2C" +
                    Double.toString(waypointList.get(0).longitude);
            for(int i = 1; i < waypointList.size(); i++){
                url = url + "%7Cvia:" + Double.toString(waypointList.get(i).latitude) + "%2C" +
                        Double.toString(waypointList.get(i).longitude);
            }
        }
        return url + "&mode=bicycling";
    }
}
