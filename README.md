## AudioRecorderAndPlayer (ARAP)

`adb shell am broadcast -a com.ss.arap.PLAY_AUDIO --es filePath "/storage/emulated/0/Android/data/com.ss.arap/files/recording_1.mp3"`
`adb shell am broadcast -a com.ss.arap.STOP_RECORDING`
`adb shell am broadcast -a com.ss.arap.START_RECORDING --es fileName "recording_1.mp3"`