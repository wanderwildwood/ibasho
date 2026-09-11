# Whereabouts (居場所, ibasho)

Find your own phone, and see where the people who agreed to share are.

Whereabouts sends a phone's location to a server you run yourself. The location
is encrypted on the phone before it leaves, so the server holds it without being
able to read it: only the password chosen on the device can open it, and it
cannot be reset. Commands can also be sent by SMS, with no server involved at
all.

Built for a 4.3" e-ink phone, so: no animation, no colour, one screen where one
screen will do.

## What it does not do

**It cannot wipe the device.** The wipe command is gone, and `wipe-data` is
absent from the device-admin policy, so Android will refuse it even if something
asked. A remote wipe means whoever learns the server password can factory-reset
the phone, which is the wrong trade for a phone somebody carries by agreement.

**It cannot take a photograph.** The camera command is gone, along with the
camera permission. A silent photo is something the person holding the phone
cannot tell has happened.

Both are reasonable in a stolen-phone tool. Neither belongs here.

## Locating

Location comes from whatever the phone can offer: GPS, the Android fused
provider, and optionally cell towers and Wi-Fi networks looked up through
[BeaconDB](https://beacondb.net/) and [OpenCelliD](https://opencellid.org/).
Those two services learn roughly where the phone is each time they are asked;
that is the trade for working indoors, and it is off unless you turn it on.

On a degoogled phone with no network location provider, expect GPS only —
sparse, and outdoors.

## Server

It talks to [FMD Server](https://gitlab.com/fmd-foss/fmd-server), self-hosted.
There is no public instance behind this app and no default server: you enter
your own.

## Credit

Whereabouts is a fork of [FindMyDevice](https://gitlab.com/Nulide/findmydevice)
by Nulide and its contributors. **Most of the code here is theirs**, and the
parts worth thanking somebody for — the protocol, the encryption design, the
transports — are all upstream's work. What is different here is what has been
taken out and how it reads on a small grey screen.

## Support

This is free software and it stays free; there is nothing here to buy. If you would like to
send something somewhere anyway, there are some llamas who go through a great deal of hay:
<https://hotspringsllamas.org/donate/>

## Licence

GNU General Public License, version 3 or later — the same as upstream, whose
terms this inherits and keeps. See [LICENSE](LICENSE).

    Copyright (C) 2022-2026  Nulide and FindMyDevice contributors
    Copyright (C) 2026  wander wildwood

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
    more details.

    You should have received a copy of the GNU General Public License along
    with this program.  If not, see <https://www.gnu.org/licenses/>.
