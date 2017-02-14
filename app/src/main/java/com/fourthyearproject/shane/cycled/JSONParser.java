package com.fourthyearproject.shane.cycled;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;


/**
 * Created by hp on 14/02/2017.
 */

public class JSONParser {

    private JSONObject jsonDirectionsObject;
    private LinkedList<Step> stepsList = new LinkedList<>();

    public JSONParser(String json)
    {
        try
        {
            jsonDirectionsObject = new JSONObject(json);
        }
        catch(JSONException e){}

    }

    LinkedList<Step> getStepsList()
    {
        return stepsList;
    }

    private void parseJSON(String directions) {
        try
        {
            JSONArray routesJSON = jsonDirectionsObject.getJSONArray("routes");
            JSONArray legsJSON = routesJSON.getJSONArray(0);
            JSONObject journeyDetailsJSON = legsJSON.getJSONObject(0);
            JSONArray stepsJSON = legsJSON.getJSONArray(1);
            for(int i = 0; i < stepsJSON.length(); i++)
            {
                String distance = stepsJSON.getJSONObject(i).getJSONObject("distance").getString("value");
                String startLat = stepsJSON.getJSONObject(i).getJSONObject("start_location").getString("lat");
                String startLng = stepsJSON.getJSONObject(i).getJSONObject("start_location").getString("lng");
                String endLat = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lat");
                String endLng = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lng");
                String polyline = stepsJSON.getJSONObject(i).getJSONObject("polyline").getString("points");
                stepsList.add(new Step(distance, startLat, startLng, endLat, endLng, polyline, i));
            }
        }
        catch(JSONException e) {}

    }
}
