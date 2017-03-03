package com.fourthyearproject.shane.cycled;

/**
 * Created by hp on 14/02/2017.
 */

public class Step {

    private String endLat;
    private String endLng;
    private String distance;
    private String maneuver;

    public Step(String endLat, String endLng, String distance, String maneuver)
    {
        this.endLat = endLat;
        this.endLng = endLng;
        this.distance = distance;
        this.maneuver = maneuver;
    }

    public String getManeuver() {
        return maneuver;
    }

    public String getEndLat() {
        return endLat;
    }

    public String getEndLng() {
        return endLng;
    }

    public String getDistance() {
        return distance;
    }

}
