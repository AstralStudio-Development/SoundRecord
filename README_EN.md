# SoundRecord

![SoundRecord](https://r2.yume.games/srd/c4e32c63-40d2-40f5-a8d8-09dc0646e5ca.png)

Save the sounds you love forever.

SoundRecord is designed to preserve music you enjoy on servers but cannot easily recreate yourself. It includes a client-side Fabric mod and a server-side Paper plugin: the client records in-game sounds into `.srd` files, while the server receives, manages, and plays those recordings.

Before recording, please make sure your network connection is stable. Network conditions may affect the final recording result.

## Features

- Adds a recording entry to the pause menu.
- Supports All Sound and MusicMode recording.
- Supports Modern mode, which starts playback from the first non-click sound.
- Automatically excludes UI button click sounds.
- Saves `.srd` files and uploads them to the server plugin directory.
- Provides `/record` commands for playback, stopping, and checking player playback status.

## Build

```powershell
.\build.bat
```

Built files will be placed in:

- `build/dist/SoundRecord-Fabric-1.0.0.jar`
- `build/dist/SoundRecord-Paper-1.0.0.jar`

## Installation

- Put `SoundRecord-Fabric-1.0.0.jar` into the client `mods` folder.
- Put `SoundRecord-Paper-1.0.0.jar` into the server `plugins` folder.
- Server recording files are stored in `plugins/SoundRecord/records`.

## Commands

```text
/record
```

## License

This project is open source under the GPL v3 license.

## Supported Languages

- Simplified Chinese
- Traditional Chinese (Hong Kong/Taiwan)
- English
