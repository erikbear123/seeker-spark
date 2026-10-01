# Seeker Spark

A 30-second, one-thumb arcade game built for Android and the Solana Seeker.

Tap green sparks, avoid red traps, and build a streak. Connect Seed Vault Wallet
through the official Mobile Wallet Adapter. After a round, sign a human-readable
score message; the app verifies its Ed25519 signature before saving the proof.

## Play

1. Install the submitted APK on Seeker or another Android device (Android 8+).
2. Open **Seeker Spark**. Choose **Connect Seeker wallet** or play as a guest.
3. Tap **Play 30-second round**. Green sparks add points; red traps lose a life.
4. At the end, choose **Sign score with wallet**, approve the message in the wallet,
   and return to the game to see **Wallet signature verified**.

## Wallet integration

- Official `com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.7`.
- Solana mainnet identity for compatibility with Seed Vault Wallet.
- `connect`, `disconnect`, and `signMessagesDetached`.
- Local Ed25519 verification with Bouncy Castle, including a check that the wallet
  returned the exact requested message.
- Every proof includes the wallet address, round UUID, score, hit count, and time.
- No SOL transfers, paid entry, token rewards, or smart contracts.
- Scores and proofs are local to the phone. This prototype has no shared leaderboard
  or server-side anti-cheat. A signature proves that a wallet signed the result;
  it does not prove that gameplay was independently verified.
- Authorization tokens remain in memory. Android backups are disabled.

## Build and test

Install JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).
Set `ANDROID_HOME` or create a local, uncommitted `local.properties` with `sdk.dir`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
```

On Windows, use `gradlew.bat`.

To generate a signed release, set `SPARK_KEYSTORE` and `SPARK_KEY_PASSWORD`.
The key alias must be `seeker-spark`. Run `./gradlew assembleRelease lintRelease`.
Signing keys and passwords must stay outside this repository.

## Checks

Unit tests cover round expiry, duplicate taps, traps/lives, expired targets,
board occupancy, valid signatures, tampered messages, and Base58 leading zeros.
A device smoke test covers launching the app, starting a game, and ending a round.
Wallet approval must also be checked on a real device with the user's wallet.

## Architecture

- `GameEngine.kt`: pure, deterministic game rules with an injected random source.
- `SparkBoard.kt`: native Canvas board and monotonic active-play timer.
- `MainActivity.kt`: native UI and lifecycle-bound wallet operations.
- `ScoreProof.kt`: score message, Base58 encoding, and signature verification.

The game pauses while the app is in the background. An interrupted round is
discarded if the Android process is destroyed. Completed result screens survive
activity recreation.

## Privacy

The app requests Internet permission only for local Mobile Wallet Adapter
communication. Cleartext traffic is allowed only for localhost/127.0.0.1.
There is no analytics, advertising, remote score database, or collection of
private keys. A signed proof is shared only when the user chooses to copy it.

## Project status

Prepared for the CLOCK IN Solana Mobile hackathon. Submission still requires
the organizer's registration and submission forms, the APK, this source repository,
a real-device demo video, and a brief product presentation.

Official event: https://solanamobile.com/blog/clock-in-the-solana-mobile-hackathon
