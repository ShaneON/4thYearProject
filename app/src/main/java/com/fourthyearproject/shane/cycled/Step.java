package com.fourthyearproject.shane.cycled;

/**
 * Created by hp on 14/02/2017.
 */

public class Step {

    private String startLat;
    private String startLng;
    private String endLat;
    private String endLng;
    private String distance;
    private String polyline;
    private int id;

    public Step(String startLat, String startLng, String endLat,
                String endLng, String distance, String polyline,
                int id)
    {
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.distance = distance;
        this.polyline = polyline;
        this.id = id;
    }

    public String getStartLat() {
        return startLat;
    }

    public String getStartLng() {
        return startLng;
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

    public String getPolyline() {
        return polyline;
    }

}
