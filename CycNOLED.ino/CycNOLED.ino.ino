#include <Arduino.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif
//#include <SPI.h>
#if not defined (_VARIANT_ARDUINO_DUE_X_) && not defined (_VARIANT_ARDUINO_ZERO_)
  #include <SoftwareSerial.h>
#endif

#define PIN            6


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

float latLngArray[100];
int addToIndex = 0;
int latLngArrayPointer = 0;

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

void setup(void)
{
  while (!Serial);  // required for Flora & Micro
  delay(500);

  Serial.begin(115200);
  Serial.println(F("Adafruit Bluefruit Command Mode Example"));
  Serial.println(F("---------------------------------------"));

  /* Initialise the module */
  Serial.print(F("Initialising the Bluefruit LE module: "));

  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }
  Serial.println( F("OK!") );

  if( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    Serial.println(F("Performing a factory reset: "));
    if(!ble.factoryReset()){
      error(F("Couldn't factory reset"));
    }
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);

  Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  Serial.println(F("Please use Adafruit Bluefruit LE app to connect in UART mode"));
  Serial.println(F("Then Enter characters to send to Bluefruit"));
  Serial.println();

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while(! ble.isConnected()) {
      delay(500);
  }

  // LED Activity command is only supported from 0.6.6
  if( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    //Serial.println(F("******************************"));
    //Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
    //Serial.println(F("******************************"));
  }
}

void loop(void)
{
  String bufferData = "";
  // Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  //Serial.println("About to read Line");
  ble.readline();
  ble.waitForOK();
  
  bufferData = readFromBuffer();
  
  if(condition(bufferData, 'l', 'f', 't')) {
    sendData(bufferData);
  }
  else if(condition(bufferData, 'r', 'g', 't')) {
    sendData(bufferData);
  }
  else if(condition(bufferData, 'e', 'n', 'd')) {
    sendData(bufferData);
    //Serial.println("Floats");
    int i = 0;
    while(latLngArray[i] != NULL){
      Serial.print("Lat and Lng No.");
      Serial.println(i);
      Serial.println(latLngArray[i++], 5);
      Serial.println(latLngArray[i++], 5);
    }
  }
  else if(condition(bufferData, 'l', 'a', 't') || condition(bufferData, 'l', 'n', 'g')){
    latLngArray[addToIndex++] = bufferData.substring(3).toFloat();
    sendData(bufferData);
  }
}

String readFromBuffer(){
  int i = 0;
  char bufferValue;
  String bufferString = "";
  while((bufferValue = ble.buffer[i++]) != NULL){
    bufferString = bufferString + bufferValue;
  }
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

boolean condition(String bufferData, char zero, char one, char two)
{
  return bufferData.charAt(0) == zero && 
          bufferData.charAt(1) == one && 
          bufferData.charAt(2) == two; 
}

