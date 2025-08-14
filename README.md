<p align="center">
  <img src="https://github.com/user-attachments/assets/839502c5-3f26-4800-837f-3ba8e5579ecd" width="256" />
  <br/>
  <i>Notify a Garden</i>
</p>

# Notify a Garden

A simple Android app to notify you about stocks in the Roblox game, "[Grow a Garden](https://www.roblox.com/games/126884695634066/Grow-a-Garden)," with UI inspired by Samsung OneUI apps based on their [design guide](https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf) written entirely in Kotlin.

This app uses both a client (this), and a server. The server uses WebSocket API from [JStudio](https://discord.com/invite/growagardenapio), and sends it to Firebase Firestore for almost real time stock notification.

# Usage

## Client

You can simply download the .apk from Releases section, and install it in your device. Make sure to allow Notifications permission.

If not, you must clone this repo

```
git clone https://github.com/Linueue/NotifyaGarden
```

Then, retrieve your own Firebase Firestore `google-services.json` from the website, and place it to `./app/google-services.json`.

Launch Android Studio, compile as release.

## Server

Obtain the API Key in the [JStudio](https://discord.com/invite/growagardenapio) Discord server.

Paste that key as environment variable with

```
set API_KEY = {API_KEY}
```

If you are deploying with Docker, add it to `./NotifyaGarden-backend/Dockerfile`.

```
ENV API_KEY={API_KEY}
```

Obtain the `service-account-file.json` from Firebase, and place it to `./NotifyaGarden-backend/service-account-file.json`.

## Features
- Restock time
- Choosing favorites
- Notification when a favorite is available

## To Do
- [x] Remote Updates
- [x] Event Stocks
- [ ] Refactor
- [x] Weather
- [ ] Travelling Merchant
- [x] Bottom Bar Navigation
