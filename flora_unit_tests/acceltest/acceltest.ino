#include <Adafruit_GPS.h>
#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include <Adafruit_L3GD20_U.h>
#include <Adafruit_9DOF.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

#define PIN            12
#define NUMPIXELS      2
#define GPSECHO false
#define DESTINATION_DISTANCE 20

    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
    #define BLUEFRUIT_HWSERIAL_NAME          Serial1
    #define BLUEFRUIT_UART_MODE_PIN         -1   // Not used with FLORA
    #define BLUEFRUIT_UART_CTS_PIN          -1   // Not used with FLORA
    #define BLUEFRUIT_UART_RTS_PIN          -1   // Not used with FLORA
/*=========================================================================*/
Adafruit_9DOF dof = Adafruit_9DOF();
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);

boolean deliveryStarted = false;
boolean latLngArrayFull = false;
boolean waitingForJob = false;
uint8_t tetheredFlag = 2;
float latLngArray[30];
char turns[15];
const uint8_t left = 0;
const uint8_t right = 1;
uint8_t turnsArrayPointer = 0;
uint8_t addToLatLngIndex = 0;
uint8_t addToTurnsIndex = 0;
uint8_t latLngArrayPointer = 0;
uint8_t gpsSignalCount = 0;
unsigned long previousMillisLights = 0;
unsigned long previousMillisAccel = 0;
unsigned long flashInterval = 0;
boolean firstFix = false;
boolean lightOn = false;
uint8_t red = 0;
uint8_t green = 0;
uint8_t blue = 0;
uint8_t flashes = 0;
uint8_t flashNum = 0;
float oldTarget = 0.0;
float oldGpsLat = 0.0;
float oldGpsLng = 0.0;
sensors_event_t event;
unsigned long accelInterval = 100;
const float time0 = 1.0;
float acceleration = 0.0;
float targetDistance;
float initVel = 0.0;
float distance = 0;


// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void setup(void){
  Serial.begin(115200);

}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void loop(void){
  sensors_event_t accel_event;
  sensors_vec_t   orientation;
   
  /* Calculate pitch and roll from the raw accelerometer data */
  accel.getEvent(&accel_event);
  if (dof.accelGetOrientation(&accel_event, &orientation))
  {
    /* 'orientation' should have valid .roll and .pitch fields */
    Serial.print(F("Roll: "));
    Serial.print(orientation.roll);
    Serial.print(F("; "));
    Serial.print(F("Pitch: "));
    Serial.print(orientation.pitch);
    Serial.print(F("; "));
  }
  accel.getEvent(&event);
  Serial.print("X: "); Serial.print(event.acceleration.x); Serial.print("  ");
  Serial.print("Y: "); Serial.print(event.acceleration.y); Serial.print("  ");
  Serial.print("Z: "); Serial.print(event.acceleration.z); Serial.print("  ");
  Serial.println("m/s^2 ");
  acceleration = event.acceleration.x;
  float dist = distFormula(acceleration);
  distance += dist;
  Serial.print("Distance = "); Serial.println(distance);
  initVel = calculateVel(acceleration, dist);
  delay(1000);
}


float distFormula(float accel){
  return (initVel * 1.0) + (0.5 * accel * (1.0 * 1.0));
}


float calculateVel(float accel, float dist){
  return (initVel * 1.0) + (0.5 * accel * (1.0 * 1.0));
}


