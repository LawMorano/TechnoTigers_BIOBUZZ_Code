# Sloth hot reload

This project uses Sloth and the Load plugin `0.3.2`. It also uses the matching
Sloth-compatible FTC Dashboard artifact, Slothboard `0.3.2+0.6.0`.

## One-time setup

1. Use Android Studio's bundled JDK (17 or newer).
2. Connect the development computer to the Control Hub over USB, or connect to its Wi-Fi
   and run `adb connect 192.168.43.1:5555`. For a Robot Controller phone, use
   `adb connect 192.168.49.1:5555`.
3. Confirm that the robot appears under `adb devices`.
4. Perform one normal full install of the `TeamCode` application. Sloth and Slothboard are
   libraries, so they must already be installed on the robot before hot reload can work.

## Hot reload

End the running OpMode, then run this Gradle task from Android Studio:

```text
:TeamCode:deploySloth
```

Or run it from PowerShell with Android Studio's JDK selected:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :TeamCode:deploySloth
```

Sloth only hot reloads classes in `org.firstinspires.ftc.teamcode` and its subpackages.
Use a normal full install after any of these changes:

- Gradle files or dependencies
- Android resources or manifests
- code outside `org.firstinspires.ftc.teamcode`
- adding, removing, or changing `@Pinned`

## Safe full installs

Before a normal install, remove any remotely loaded classes so they cannot override the
new APK:

```text
:TeamCode:removeSlothRemote
```

In Android Studio, edit the normal `TeamCode` run configuration and add a **Run Gradle
task** before launch. Set the Gradle project to `:TeamCode`, the task to
`removeSlothRemote`, and place it before **Gradle-aware Make**.

If cleanup ever has to be done manually while ADB is connected:

```text
adb shell rm -rf /storage/emulated/0/FIRST/dairy/sloth/*
```

Do not run the manual deletion command while an OpMode is active.
