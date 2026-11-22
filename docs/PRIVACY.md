# Privacy

There is no analytics SDK and no crash reporter. The app does make one small call to the
project's own panel (`app.fbtec.site`), described below.

## Panel check-in

Each time the app opens or returns to the foreground, it sends a short message to
`https://app.fbtec.site/api/v1/ping` containing:

- the device identifier (Android ID)
- device model and manufacturer
- the app version and the Android version

It is used only to know which versions and devices are in use. It never includes your provider
username, password, channels, favourites or viewing history. The panel may reply with an updated
server address, which the app then uses for sources that point to the default server.

## What is stored, and where

Everything lives in a SQLite database in the app's private storage on your device:

- Your provider's address, username and password
- The channel, movie and series catalogue downloaded from your provider
- The guide (EPG)
- Your favourites, hidden channels and resume positions

None of it is uploaded anywhere. There is no account to create and no server to sync with.

## Who your device talks to

Your provider (at the address you entered) and the panel check-in above. Channel logos and posters are fetched
from whatever URLs your provider puts in its catalogue, which are usually its own servers but
may be third parties — that is your provider's choice, not ours.

## Cleartext HTTP

The app allows plain HTTP (`usesCleartextTraffic="true"`). The overwhelming majority of IPTV
panels are HTTP-only; refusing them would make the app useless to most of the people it is for.

Be aware of what that means: **on an HTTP connection, your provider username and password are
sent unencrypted**, and anyone able to observe the network between you and your provider can
read them. This is a property of how these services are built, not of OpenTV. If your provider
offers an HTTPS endpoint, use it.

## Uninstalling

Removing the app removes the database and everything in it.

## Reporting a privacy problem

Open an issue. If it involves something you would rather not post publicly, say so in the issue
without the details and a maintainer will arrange somewhere better.
