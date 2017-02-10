# Blog: CycLED

**Shane O'Neill**

## My First Blog Entry

For the past few days I have been working on the first aspect of my project.
So right now the task that I expect to have finished before the start of next week is to be able to send very simple information via bluetooth
from an app on my phone to the flora system and recieve a message back from the flora telling me the message was recieved.
If I get this finished towards the end of the week I can then move on to using the google maps API for the next step of my project.

## My Second Blog Entry

I have completed the task of connecting my app to the flora board via the Bluefruit bluetooth module so I now have back and forth communication. 
I will now begin the task of integrating the google maps API with my app, and also gps location functionality, so that my app can find a person's location
and plan out their routes which the direction system will guide them along.

## My Third Blog Entry

I now have the Google Maps API up and running, and also real time GPS updates. I can see my position change when I move on my app's map screen.
Now I need to programme the route planning functionality, where the user will select a destination and the app will either choose a route for the user, 
or the user can draw in their own route. I am not sure yet how I will go about implementing the route drawing functionality, as the phone screen is quite 
small it could be tricky to come up with a way that works without too much hassle for the user. 
As for the automatic route selection, I think most likely the shortest possible route will be the one selected, although I might give some consideration
to incorporating external sources of information, such as traffic etc. if I have time, but that is just an idea at the moment.

## My Fourth Blog Entry

I ran into an issue while I was getting ready to set up the directions element of my app. I have realised that the bluetooth, gps and directions need to be background services 
in the app rather than activities, because the app needs to be on and communicating with the Flora board even when the phone is locked. So my next task is to move my gps and bluetooth code 
to their own IntentService classes and then make a new directions Service. I have made good progress on the gps and bluetooth services, but still have more things to fix 
before they are properly functional.
I also added a bluetooth button that changes colour depending on if its connected to a device or not. 


