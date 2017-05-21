// This program tests the distance function within my program which calculates the distance
// between two sets of latitude and longitude values.

//DCU Glasnevin campus
float lat1 = 53.3846353;
float lng1 = -6.2582516;

//DCU St.Pat's Campus
float lat2 = 53.370398;
float lng2 = -6.256567;

void setup(){  
  while (!Serial);  
    delay(500);
    
  Serial.begin(115200);
  
  float distance = calculateLatLngDist(lat1, lng1, lat2, lng2);
  Serial.print("Distance from Glasnevin campus to St.Pats campus: ");
  Serial.print(distance);
  Serial.print(" metres.");
}

void loop(){}

//was unsigned long
float calculateLatLngDist(float originLat, float originLng, 
                                    float targetLat, float targetLng){
  float latDif = radians(targetLat - originLat);
  originLat = radians(originLat);
  targetLat = radians(targetLat);
  float lngDif = radians(targetLng - originLng);
 
  float distance1 = (sin(latDif / 2.0) * sin(latDif / 2.0));
  float distance2 = cos(originLat);
  distance2 *= cos(targetLat);
  distance2 *= sin(lngDif / 2.0);
  distance2 *= sin(lngDif / 2.0);
  distance1 += distance2;
 
  distance1 = (2 * atan2(sqrt(distance1), sqrt(1.0 - distance1)));
  distance1 *= 6371000.0; //Converting to meters
  
  return distance1;
}

