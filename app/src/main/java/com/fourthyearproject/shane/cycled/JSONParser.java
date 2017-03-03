package com.fourthyearproject.shane.cycled;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;


/**
 * Created by hp on 14/02/2017.
 */

public class JSONParser {

    private final String TAG = "JSONParser";

    private JSONObject jsonDirectionsObject;
    private LinkedList<Step> stepsList;
    private boolean listFull = false;
    private String polyline;

    public JSONParser(String json)
    {
        try
        {
            jsonDirectionsObject = new JSONObject(json);
            parseJSON();
        }
        catch(JSONException e){}

    }

    public boolean getListFull()
    {
        return listFull;
    }

    public String getPolyline()
    {
        return polyline;
    }

    LinkedList<Step> getStepsList()
    {
        return stepsList;
    }

    private void parseJSON() {
        try
        {
            stepsList = new LinkedList<>();
            JSONArray routesJSON = jsonDirectionsObject.getJSONArray("routes");
            JSONArray legsJSON = routesJSON.getJSONObject(0).getJSONArray("legs");
            JSONObject journeyDetailsJSON = legsJSON.getJSONObject(0);
            JSONArray stepsJSON = legsJSON.getJSONObject(0).getJSONArray("steps");
            polyline = routesJSON.getJSONObject(0).getJSONObject("overview_polyline").getString("points");

            for(int i = 0; i < stepsJSON.length(); i++)
            {
                String distance = stepsJSON.getJSONObject(i).getJSONObject("distance").getString("value");
                String endLat = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lat");
                String endLng = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lng");
                String maneuver;
                if(stepsJSON.getJSONObject(i).has("maneuver"))
                    maneuver = stepsJSON.getJSONObject(i).getString("maneuver");
                else maneuver = "";
                stepsList.add(new Step(endLat, endLng, distance, maneuver));
            }
            listFull = true;
        }
        catch(JSONException e) {}

    }
}