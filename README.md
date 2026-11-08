# Local Strike 3D 🎮🔫

A fast-paced **3D Multiplayer First-Person Shooter (FPS)** designed for Android devices connected to a **common local Wi-Fi network**.

---

## 🌟 Key Features

- **Local Wi-Fi Multiplayer**:
  - **Auto Room Discovery**: When one player taps **Host Match**, the game broadcasts beacons over local Wi-Fi (UDP). Friends on the same Wi-Fi instantly see the match in their lobby with one-tap **JOIN**!
  - **Direct IP Fallback**: Displays your device's exact local IP (`192.168.x.x`) with manual entry support in case of router AP isolation.
  - **Solo Practice Mode**: Offline bot mode with AI opponents (`Viper Bot`, `Titan Bot`) so you can play and practice anytime.

- **Health & Armor System**:
  - **Health (100 HP)**: Core vitality bar. Red damage vignette when critical.
  - **Armor (100 AP)**: Tactical shield absorbing **66% of incoming shot damage** before health is affected!

- **Dynamic Arena Loots**:
  - 💚 **MedKit**: Restores `+35 Health`
  - 🛡️ **Armor Plate**: Restores `+50 Armor`
  - 📦 **Ammo Crate**: Refills `+30 Ammunition`
  - 💥 **Tactical Shotgun**: 6 high-impact spread pellets for close-quarters dominance
  - ⚡ **Plasma Cannon**: High-energy plasma blaster with explosive power
  - *All loots float with glowing 3D rings and respawn after 15 seconds!*

- **3D Raycasting Engine & Tactical HUD**:
  - Silky smooth 60 FPS perspective rendering in Jetpack Compose Canvas.
  - 3D Billboard player models with animated visors, weapons, and overhead distance/health tags.
  - Left Virtual Joystick (movement & strafe) + Right screen drag (camera aim).
  - Tactical top-down Minimap Radar showing view cones, walls, enemies, and loot blips.
  - Animated first-person weapon model with dynamic muzzle flash, recoil kickback, and hitmarkers.

---

## 🚀 How to Play Over Local Wi-Fi

1. **Connect both Android phones to the same Wi-Fi network** (or have one phone enable a Mobile Hotspot and the other connect to it).
2. Open **Local Strike 3D** on both devices:
   - **Player 1**: Tap **HOST MATCH**. Your local Wi-Fi IP and room name will be broadcasted.
   - **Player 2**: Look at the **Available Local Rooms** list and tap **JOIN** (or tap **Direct IP** and enter Player 1's IP address).
3. The match starts instantly! First operative to **10 kills** wins the round.

---

## 📦 How to Build & Download APK via GitHub Actions

This project includes a ready-to-run GitHub Actions workflow in `.github/workflows/build-apk.yml`.

### Step 1: Push to GitHub
Push your repository to GitHub:
```bash
git add .
git commit -m "feat: Local Strike 3D multiplayer shooter"
git push origin main
```

### Step 2: GitHub Action Automatically Builds the APK
1. Go to your GitHub repository in your browser.
2. Click on the **Actions** tab at the top.
3. You will see the **Build Android APK** workflow running.
4. When finished (green checkmark ✅), click on the completed workflow run.
5. Scroll down to the **Artifacts** section and click on **`LocalStrike3D-Debug-APK`** to download your installable APK!

### (Optional) Create a Release
To automatically generate a GitHub Release with the APK attached:
```bash
git tag v1.0.0
git push origin v1.0.0
```
GitHub Actions will automatically attach `app-debug.apk` directly to the new Release page.
