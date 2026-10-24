# 📺 Cinejoy TV - Application Android TV & Fire TV

Application dédiée pour **Android TV**, **Google TV** et **Amazon Fire TV Stick**, optimisée pour le streaming à partir du site **[cinejoy.pk](https://cinejoy.pk)**.

---

## ✨ Fonctionnalités Clés

- 🎯 **100% Optimisé pour Télécommande (D-Pad & Souris Virtuelle)** :
  - **Mode D-Pad classique** : Navigation par les flèches sur les éléments de la page.
  - **Mode Souris Virtuelle (Pointeur)** : Permet de déplacer un curseur fluide et d'effectuer des clics natifs sur n'importe quel lecteur vidéo ou menu d'épisodes (activable via la touche **[MENU]** ou appui long sur **[OK]**).
- 🛡️ **Bloqueur de Popups & Publicités Intégré** :
  - Interception des ouvertures de fenêtres (`window.open`).
  - Blocage des réseaux publicitaires agressifs, redirections trompeuses et overlays invisibles.
- 🎬 **Lecteur Vidéo Plein Écran Automatique** :
  - Prise en charge native de l'accélération matérielle et du mode plein écran HTML5.
- 📺 **Intégration Android TV Leanback** :
  - Déclaration `LEANBACK_LAUNCHER` pour apparaître directement sur l'écran d'accueil de la TV ou du Fire TV Stick avec sa bannière dédiée.
- ⚙️ **Menu Rapide OSD (Affichage à l'écran)** :
  - Accessible à tout moment avec la touche **[MENU]** de la télécommande (Bouton Accueil, Bascule Souris/D-Pad, Rafraîchir, Quitter).

---

## 🚀 Comment générer le fichier APK

### Option 1 : Via GitHub Actions (Recommandé - Sans rien installer sur votre PC)
1. Créez un dépôt GitHub et envoyez-y ce projet (`git push`).
2. Le fichier [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) se déclenchera automatiquement.
3. Allez dans l'onglet **Actions** de votre dépôt GitHub, cliquez sur le build terminé, et téléchargez directement l'artefact **`CinejoyTV-debug-apk`**.

### Option 2 : Avec Android Studio
1. Ouvrez **Android Studio**.
2. Choisissez **Open** et sélectionnez le dossier `cinejoy-android-tv`.
3. Attendez la synchronisation Gradle.
4. Dans la barre de menu, allez dans : **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. L'APK sera généré dans `app/build/outputs/apk/debug/app-debug.apk`.

### Option 3 : En ligne de commande
Si le JDK 17+ et le SDK Android sont installés :
```bash
./gradlew assembleDebug
```
L'APK se trouve ensuite dans : `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📲 Comment installer l'APK sur votre Télévision

### Méthode 1 : Avec l'application "Downloader" (Le plus simple sur Fire TV / Android TV)
1. Installez l'application gratuite **Downloader** depuis le Google Play Store ou l'Amazon Appstore de votre TV.
2. Activez l'option *Sources inconnues* ou *Installer les applications inconnues* pour Downloader dans les paramètres de la TV.
3. Hébergez votre APK (par exemple sur GitHub Releases, Dropbox, Google Drive ou un lien direct).
4. Entrez le lien dans Downloader sur votre TV pour télécharger et installer l'APK en un clic.

### Méthode 2 : Avec "Send Files to TV" ou "LocalSend"
1. Installez **Send Files to TV** ou **LocalSend** sur votre TV et sur votre smartphone/PC.
2. Transférez le fichier `app-debug.apk` depuis votre téléphone ou PC vers la TV par le réseau local Wi-Fi.
3. Sur la TV, ouvrez un gestionnaire de fichiers (ex: *FX File Explorer* ou *File Commander*) et cliquez sur le fichier APK pour l'installer.

### Méthode 3 : Par Clé USB
1. Copiez le fichier `app-debug.apk` sur une clé USB formatée en FAT32.
2. Branchez la clé USB sur votre téléviseur ou box Android TV.
3. Ouvrez un explorateur de fichiers sur votre TV, cliquez sur l'APK et confirmez l'installation.

### Méthode 4 : En Wi-Fi avec ADB
Si vous êtes développeur :
```bash
# Connectez-vous à l'adresse IP de votre téléviseur
adb connect 192.168.1.XX:5555
# Installez l'APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎮 Commandes de la Télécommande

| Touche | Action |
|---|---|
| **Flèches directionnelles** | Déplacer le focus (Mode D-Pad) ou déplacer le pointeur (Mode Souris) |
| **Bouton OK / Centre** | Valider / Clic souris sur l'élément visé |
| **Appui long sur OK** | Basculer rapidement entre Mode D-Pad et Mode Curseur Souris |
| **Touche MENU** | Ouvrir le menu d'actions rapides (Accueil, Recharger, Mode Souris, Quitter) |
| **Touche RETOUR** | Quitter le plein écran vidéo, revenir à la page précédente ou fermer le menu |
