# Privacy

Whereabouts sends the phone's location only where you have set it up to: to your own
server, by text to the numbers you allow, or as a reply in a messaging app to a message
that carries your password. Nothing is sent to this project, and there is
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
- **Replies in other apps**, only if you let Whereabouts read notifications and set a
  password for it. It reads the text of other apps' notifications (not the text messaging
  app's) to find ones that start with the command word, and acts only on a command that
  carries the password. The answer goes back through that app's own reply button, to
  whoever sent the message: for a location command, that is the coordinates and an
  OpenStreetMap link. That notification is then marked read and dismissed. From there, the
  other app carries the reply as it carries any message.

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
permissions read the cell towers the phone sees, for locating by cell; your fingerprint or screen
lock is asked for only before a backup is exported. Contacts are read only for a contact you pick when adding a number. The
notification listener reads other apps' notifications only to spot the command word, and
answers a command through that notification's reply, as described above. Device
administration lets the app lock the screen, and nothing else: it cannot erase the phone. There is no camera
permission. Sharing a location with another app needs no permission beyond the location access
Whereabouts already has.

<!-- shared-about privacy section -->
## Who we are

Whereabouts is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. What you put into Whereabouts stays on your phone. The only things that go out are the ones described above: the phone's location, encrypted on the phone first, to your own server; replies by text to the numbers you allow, carrying the location or the answer to their command; if you let it read notifications, replies through another app's notification to whoever sent a command with your password; a connection to Mozilla's push service so your server can wake the phone, which carries no command; and, if you turn on locating by cell towers and Wi-Fi, a request to BeaconDB or OpenCelliD naming the towers and networks the phone sees. The developer receives nothing, so the developer holds no data about you.

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
