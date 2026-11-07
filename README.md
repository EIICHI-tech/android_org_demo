# Hi Elevator Demo

Android 8.0+ / portrait demo application.

## Voice flow
1. Say `Hi エレベーター`.
2. The app replies `はい、何階ですか？`.
3. Say a floor from 1 to 30, for example `12階`.
4. The selected button lights up and TTS confirms the destination.

You can also say `Hi エレベーター、12階` in one phrase.

## Build
Open the folder in Android Studio, allow Gradle sync, and run the `app` configuration.
The first launch requests microphone permission.

## Notes
This is a UI/demo controller only. It does not operate a real elevator. For real equipment, use the elevator manufacturer's approved interface and safety design.
