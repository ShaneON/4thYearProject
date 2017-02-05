# CycLED

CycLED is a direction system for cyclists. Two lights in the helmet guide the user to their destination by indicating which way they should turn.
The hardware consists of a Flora board which is attached to a bluetooth module, an accelerometer and two LED lights. An android app communicates 
with the flora board via bluetooth and sends the turn signal to it. The accelerometer information is sent back to the phone to correct for errors in the GPS.
The app uses GPS and the google maps API to update the users position and plan out their route. Users also have the option to draw in their own route.