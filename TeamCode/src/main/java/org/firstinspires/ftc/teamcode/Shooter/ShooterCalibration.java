package org.firstinspires.ftc.teamcode.Shooter;

import com.qualcomm.robotcore.util.ReadWriteFile;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.Locale;

/** Voltage-based shooter gains shared by the tuner and shot-map test. */
public class ShooterCalibration {
    public static final String FILE_NAME = "shooter-calibration.txt";

    // kS: V, kV: V/RPM, kA and kD: V/(RPM/s), kP: V/RPM, kI: V/(RPM*s).
    public double kS = 0.0;
    public double kV = 0.0020;
    public double kA = 0.0;
    public double kP = 0.0010;
    public double kI = 0.0;
    public double kD = 0.0;
    public double maxAccelerationRpmPerSecond = 12000.0;
    public double integralZoneRpm = 250.0;
    public double integralLimitVolts = 1.0;
    public double readyToleranceRpm = 75.0;
    public double readyAccelerationRpmPerSecond = 400.0;
    public double readyTimeSeconds = 0.15;
    /** Motor RPM multiplied by this value equals flywheel RPM. */
    public double motorToFlywheelRatio = 1.0;

    public static ShooterCalibration load() {
        ShooterCalibration calibration = new ShooterCalibration();
        File file = new File(AppUtil.FIRST_FOLDER, FILE_NAME);
        if (!file.exists()) return calibration;

        for (String rawLine : ReadWriteFile.readFile(file).split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
            String[] pieces = line.split("=", 2);
            try {
                calibration.set(pieces[0].trim(), Double.parseDouble(pieces[1].trim()));
            } catch (NumberFormatException ignored) {
                // Keep the default for a malformed line instead of disabling the shooter.
            }
        }
        return calibration;
    }

    public void save() {
        File file = new File(AppUtil.FIRST_FOLDER, FILE_NAME);
        ReadWriteFile.writeFile(file, String.format(Locale.US,
                "# Voltage-based dual-motor shooter calibration%n" +
                        "kS=%.10f%n" +
                        "kV=%.10f%n" +
                        "kA=%.10f%n" +
                        "kP=%.10f%n" +
                        "kI=%.10f%n" +
                        "kD=%.10f%n" +
                        "maxAccelerationRpmPerSecond=%.6f%n" +
                        "integralZoneRpm=%.6f%n" +
                        "integralLimitVolts=%.6f%n" +
                        "readyToleranceRpm=%.6f%n" +
                        "readyAccelerationRpmPerSecond=%.6f%n" +
                        "readyTimeSeconds=%.6f%n" +
                        "motorToFlywheelRatio=%.10f%n",
                kS, kV, kA, kP, kI, kD,
                maxAccelerationRpmPerSecond, integralZoneRpm, integralLimitVolts,
                readyToleranceRpm, readyAccelerationRpmPerSecond, readyTimeSeconds,
                motorToFlywheelRatio));
    }

    private void set(String name, double value) {
        switch (name) {
            case "kS": kS = value; break;
            case "kV": kV = value; break;
            case "kA": kA = value; break;
            case "kP": kP = value; break;
            case "kI": kI = value; break;
            case "kD": kD = value; break;
            case "maxAccelerationRpmPerSecond": maxAccelerationRpmPerSecond = value; break;
            case "integralZoneRpm": integralZoneRpm = value; break;
            case "integralLimitVolts": integralLimitVolts = value; break;
            case "readyToleranceRpm": readyToleranceRpm = value; break;
            case "readyAccelerationRpmPerSecond": readyAccelerationRpmPerSecond = value; break;
            case "readyTimeSeconds": readyTimeSeconds = value; break;
            case "motorToFlywheelRatio": motorToFlywheelRatio = value; break;
            default: break;
        }
    }
}
