#include <Adafruit_GPS.h>
#include <Arduino.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif
//#include <SPI.h>
#if not defined (_VARIANT_ARDUINO_DUE_X_) && not defined (_VARIANT_ARDUINO_ZERO_)
  #include <SoftwareSerial.h>
#endif

#define PIN 6
#define GPSECHO false
#define DESTINATION_DISTANCE 20

    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
/*=========================================================================*/

Adafruit_GPS GPS(&Serial1);

float testTargetLat = 53.3692319;
float testTargetLng = -6.2391872;
float tripDistance;
boolean isStarted = false;

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

void setup(void){
  // required for Flora & Micro
  while (!Serial);  
  delay(500);
  Serial.begin(115200);
  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  delay(1000);
  Serial1.println(PMTK_Q_RELEASE);
}

uint32_t timer = millis();

void loop(void){
  char c = GPS.read();
  if (GPSECHO)
      if (c) Serial.print(c);
  if (GPS.newNMEAreceived()) {
    if (!GPS.parse(GPS.lastNMEA()))  
      return;  
  }
  if (GPS.fix) {
      //Serial.print("Location: ");
      //Serial.print(GPS.latitude, 2); Serial.print(GPS.lat);
      //Serial.print(", "); 
      //Serial.print(GPS.longitude, 2); Serial.println(GPS.lon);
      
      float currentLat = latLngToDecimal(GPS.latitude, GPS.lat);
      float currentLng = latLngToDecimal(GPS.longitude, GPS.lon);
    
      tripDistance = (double)calculateLatLngDist(currentLat, currentLng, testTargetLat, testTargetLng);
      Serial.print("Distance = ");
      Serial.println(tripDistance);
  }
}

unsigned long calculateLatLngDist(float originLat, float originLng, 
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

// Convert NMEA coordinate to decimal degrees
float latLngToDecimal(float nmeaCoord, char dir) {
  uint16_t wholeDegrees = 0.01 * nmeaCoord;
  int modifier = 1;
 
  if (dir == 'W' || dir == 'S') {
    modifier = -1;
  }
  return (wholeDegrees + (nmeaCoord - 100.0 * wholeDegrees) / 60.0) * modifier;
}

