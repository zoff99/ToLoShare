package com.zoffcc.applications.trifa;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RouteManager {

    public static void startRouting(double startLat, double startLon, double endLat, double endLon) {
        new Thread(() -> {
            try {
                // Construct the URL with US Locale to ensure '.' decimal separators
                String urlString = "https://router.project-osrm.org/route/v1/car/" +startLon+ ","+startLat+
                                   ";" + endLon +"," +endLat+
                                   "?steps=true";
                // http://router.project-osrm.org/route/v1/car/16.397009,48.201302;16.44075,48.240645?steps=true

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Read the raw JSON response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                // Process the result
                printManeuverLocations(sb.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void printManeuverLocations(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray routes = root.getJSONArray("routes");

            // Iterate through each route
            for (int i = 0; i < routes.length(); i++) {
                JSONObject route = routes.getJSONObject(i);
                JSONArray legs = route.getJSONArray("legs");

                // Iterate through each leg
                for (int j = 0; j < legs.length(); j++) {
                    JSONObject leg = legs.getJSONObject(j);
                    JSONArray steps = leg.getJSONArray("steps");

                    // Iterate through each step
                    for (int k = 0; k < steps.length(); k++) {
                        JSONObject step = steps.getJSONObject(k);

                        // Access the maneuver object
                        if (step.has("maneuver")) {
                            JSONObject maneuver = step.getJSONObject("maneuver");
                            JSONArray location = maneuver.getJSONArray("location");

                            // Extract Longitude and Latitude [lon, lat]
                            double lon = location.getDouble(0);
                            double lat = location.getDouble(1);

                            System.out.println("Maneuver at: " + lat + ", " + lon);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
