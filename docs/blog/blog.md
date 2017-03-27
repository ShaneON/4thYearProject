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

## My Fifth Blog Entry
I have my directions background service connecting to the google directions web service, it returns a JSON file which I will need to parse to extract the information from.
So that is my next task.

## My Sixth Blog Entry
I now have the app pulling directions from the google server, it comes in the form of a JSON which I am currently working on parsing and turning into usable data.
I am almost ready to start working on an algorithm for detecting when a user needs to turn and also one for updating the routes. It is difficult to test my app to se if I am
heading in the right direction as it is supposed to be used on the road, but I would be hoping to be able to road test a very rough version of it in a few weeks time.

## My Seventh Blog Entry
I now have a functioning version of the app which I have tested by itself by walking a few routes and the GPS seems to hold up well and
it directs me to my destination without any problems. I have not however tested it with the flora board yet. This is because the weather has
been very bad lately and the hardware is very out in the open so it could be damaged by rain. As soon as i get a clear day I will test
the whole system together but I expect it to all work together well.
My next task is to test out the hardware accelerometer and GPS modules that come with the flora to see if they can be of use for my project.
The GPS module will be very difficult to test properly as it can only be used outdoors with a clear view of the sky, which is unfortunately
a big ask in Ireland.

## My Eighth blog entry
At the moment I am working on send the latitudes and longitudes as strings from the app to the arduino, and then converting them
into floats for the next step where i will work on an algorithm to calculate the distance between two sets of lats and lngs.
I have almost completed this task but I am having some trouble with the confirmation data sent back from the arduino. As when
I send data to it, I wait for confirmation that it was received properly before I send the next data packet. I am encountering some
problems where for some reason it stops letting me write to the arduino so I only get half of the array sent. I have not been able to work
out why yet as it happens at seemingly random and unrelated times. I am hoping I can find a solution for it soon.