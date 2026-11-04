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
display over other apps are what let a lost phone be found, rung or locked. Bluetooth lets a command
switch the phone's Bluetooth on or off or scan for nearby devices; the phone state and cell
permissions read the cell towers the phone sees, for locating by cell; biometric is the lock on
the app's settings. Contacts are read only for a contact you pick when adding a number. The
notification listener is used only to spot the command word in a notification. Device
administration lets the app lock the screen, and wipe the phone only if you switch that on
with its own password. There is no camera
permission. Sharing a location with another app needs no permission beyond the location access
Whereabouts already has.

<!-- shared-about privacy section -->
## Who we are

Whereabouts is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Whereabouts keeps what you put in it on your phone, apart from what it sends to your own server, by text to the numbers you allow, and to Mozilla's push service, BeaconDB and OpenCelliD as described above, and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Whereabouts is sold or shared.

Since Whereabouts keeps what it stores on your phone and on a server of your own, you can exercise most of these rights yourself: delete your account and the locations it holds under Server settings, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.

Whereabouts connects to Mozilla's push service ([Mozilla's privacy notices](https://www.mozilla.org/privacy/)), [BeaconDB](https://beacondb.net/) and [OpenCelliD](https://opencellid.org/). They are separate from this app and from its developer, and what they receive is covered by their own privacy policies; ask them about what they hold.
If you point Whereabouts at a server of your own, what that server keeps is yours to manage.
<!-- /shared-about privacy section -->
