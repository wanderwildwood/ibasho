# Privacy

Whereabouts sends the phone's location only where you have set it up to: to your own
server, or by text to the numbers you allow. Nothing is sent to this project, and there is
no account with anyone but your own server.

## Where it connects

- **Your server**, if you set one up ([FMD Server](https://gitlab.com/fmd-foss/fmd-server) or
  [Whereabouts server](https://github.com/wanderwildwood/ibasho-server)). Locations are
  encrypted on the phone before they leave, with a password chosen on the device, so the
  server stores them without being able to read them.
- **Mozilla's push service**, while a server is set up and the built-in push connection is
  chosen, so commands reach the phone. It learns that the phone is connected and when your
  server wakes it, not what the command is. Another push app can be chosen instead.
- **[BeaconDB](https://beacondb.net/) and [OpenCelliD](https://opencellid.org/)**, only if you
  turn on locating by cell towers and Wi-Fi. Each learns roughly where the phone is each time
  it is asked.
- **Text messages**, to and from the numbers on its list, when commands are sent by SMS.

## Other apps on the phone

Another app can ask for the phone's location once, through the screen described in the
README under "Sharing where you are with another app". Whereabouts names the app that asked
and hands nothing over unless you press **Share**. One location goes to that app, and only
to it: what is sent is the latitude, the longitude, when the fix was taken and how accurate
it is. Whereabouts keeps no record of the request. Nothing else in the app is open to other
apps.

## Permissions

The full list is in `app/src/main/AndroidManifest.xml`. Location, SMS, Do Not Disturb access and
display over other apps are what let a lost phone be found, rung or locked. There is no camera
permission. Sharing a location with another app needs no permission beyond the location access
Whereabouts already has.
