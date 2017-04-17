#include <Adafruit_GPS.h>
#include <Arduino.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif
//#include <SPI.h>
#if not defined (_VARIANT_ARDUINO_DUE_X_) && not defined (_VARIANT_ARDUINO_ZERO_)
  #include <SoftwareSerial.h>
#endif

#define PIN            6
#define GPSECHO false
#define DESTINATION_DISTANCE 20

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"

#include "BluefruitConfig.h"

    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
    #define BLUEFRUIT_HWSERIAL_NAME          Serial1
    #define BLUEFRUIT_UART_MODE_PIN         -1   // Not used with FLORA
    #define BLUEFRUIT_UART_CTS_PIN          -1   // Not used with FLORA
    #define BLUEFRUIT_UART_RTS_PIN          -1   // Not used with FLORA
/*=========================================================================*/

// Create the bluefruit object, either software serial...uncomment these lines

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);
Adafruit_GPS GPS(&Serial2);

float testTargetLat = 53.3692319;
float testTargetLng = -6.2391872;
float tripDistance;
boolean isStarted = false;

float latLngArray[100];
int addToIndex = 0;
int latLngArrayPointer = 0;
int tethered;

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

void setup(void){
  while (!Serial);  // required for Flora & Micro
  delay(500);
  Serial.begin(115200);

  if ( !ble.begin(VERBOSE_MODE) )
    error(F("Couldn't find Bluefruit make sure it's in CoMmanD mode & check wiring?"));
  
  if( FACTORYRESET_ENABLE ){
    /* Perform a factory reset to make sure everything is in a known state */
    if(!ble.factoryReset())
      error(F("Couldn't factory reset"));
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);
  ble.info();
  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while(! ble.isConnected()) delay(500);
  
  if( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);

  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);
  delay(1000);
  Serial2.println(PMTK_Q_RELEASE);
}

void loop(void){
  //-----------------------------Flora GPS----------------------------------------
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
  //------------------------------Tethered to phone--------------------------
  String bufferData = "";
  // Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  //Serial.println("About to read Line");
  ble.readline();
  ble.waitForOK();
  
  bufferData = readFromBuffer();

  if(bufferData != '0') processData(bufferData);
  else tethered = 0;
}

void processData(String data){
  if(condition(data, 'l', 'f', 't')) sendData(data);
  else if(condition(data, 'r', 'g', 't')) sendData(data);
  else if(condition(data, 'e', 'n', 'd')) {
    sendData(data);
    Serial.println("Floats");
    int i = 0;
    while(latLngArray[i] != NULL){
      Serial.print("Lat and Lng No.");
      Serial.println(i);
      Serial.println(latLngArray[i++], 5);
      Serial.println(latLngArray[i++], 5);
    }
  }
  else if(condition(data, 'l', 'a', 't') || condition(data, 'l', 'n', 'g')){
    latLngArray[addToIndex++] = data.substring(3).toFloat();
    sendData(data);
  }
}

String readFromBuffer(){
  int i = 0;
  char bufferValue;
  String bufferString = "";
  while((bufferValue = ble.buffer[i++]) != NULL) 
    bufferString = bufferString + bufferValue;
  return bufferString;
}

void sendData(String data){
  Serial.println(data);
  // Send characters to Bluefruit
  ble.print("AT+BLEUARTTX=");
  ble.println(data);
  // check response status
  ble.waitForOK();
}

boolean condition(String bufferData, char zero, char one, char two){
  return bufferData.charAt(0) == zero && 
          bufferData.charAt(1) == one && 
          bufferData.charAt(2) == two; 
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
 
  if (dir == 'W' || dir == 'S') modifier = -1;
 
  return (wholeDegrees + (nmeaCoord - 100.0 * wholeDegrees) / 60.0) * modifier;
}


