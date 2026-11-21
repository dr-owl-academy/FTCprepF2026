/*   MIT License
 *   Copyright (c) [2026] [Base 10 Assets, LLC]
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */

package org.firstinspires.ftc.teamcode;

import static com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@TeleOp
public class aviFSMStarterBot extends OpMode {

    private DcMotor leftFrontDrive;
    private DcMotor rightFrontDrive;
    private DcMotor leftBackDrive;
    private DcMotor rightBackDrive;
    private DcMotor intake;
    private DcMotorEx launcher;
    private CRServo leftIntakeServo;
    private CRServo rightIntakeServo;
    private CRServo windmillServo;

    // The launcher encoder reports velocity in ticks per second.
    public final int LAUNCHER_TARGET_VELOCITY = 1250;
    public final int LAUNCHER_MIN_VELOCITY = 1200;

    // An enum lists all the modes the launcher and transfer can be in.
    private enum LauncherState {
        IDLE,
        MANUAL_FEED,
        SPINNING_UP,
        FEEDING
    }

    // This variable remembers the current mode between calls to loop().
    private LauncherState launcherState = LauncherState.IDLE;

    @Override
    public void init() {
        leftFrontDrive = hardwareMap.get(DcMotor.class, "leftFront");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "rightFront");
        leftBackDrive = hardwareMap.get(DcMotor.class, "leftBack");
        rightBackDrive = hardwareMap.get(DcMotor.class, "rightBack");
        intake = hardwareMap.get(DcMotor.class, "intake");
        launcher = hardwareMap.get(DcMotorEx.class, "flywheel");
        windmillServo = hardwareMap.get(CRServo.class, "transfer");
        leftIntakeServo = hardwareMap.get(CRServo.class, "leftIntake");
        rightIntakeServo = hardwareMap.get(CRServo.class, "rightIntake");

        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        leftBackDrive.setDirection(DcMotor.Direction.REVERSE);
        rightBackDrive.setDirection(DcMotor.Direction.FORWARD);

        leftFrontDrive.setZeroPowerBehavior(BRAKE);
        rightFrontDrive.setZeroPowerBehavior(BRAKE);
        leftBackDrive.setZeroPowerBehavior(BRAKE);
        rightBackDrive.setZeroPowerBehavior(BRAKE);
        intake.setZeroPowerBehavior(BRAKE);

        // Use the encoder and PIDF controller to hold the requested speed.
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        launcher.setPIDFCoefficients(
                DcMotor.RunMode.RUN_USING_ENCODER,
                new PIDFCoefficients(40, 0, 0, 12.5));

        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);

        launcherState = LauncherState.IDLE;
        runLauncherFSM(false, false, 0, 0);
        telemetry.addData("Status", "Initialized");
    }

    @Override
    public void loop() {
        // The drivetrain responds to the sticks on every loop.
        mecanumDrive(-gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x);

        // Read inputs. Right trigger intakes and left trigger reverses.
        double triggerPower = gamepad1.right_trigger - gamepad1.left_trigger;
        double launcherVelocity = launcher.getVelocity();

        runLauncherFSM(gamepad1.right_bumper, gamepad1.left_bumper, launcherVelocity, triggerPower);

        telemetry.addData("Launcher state", launcherState);
        telemetry.addData("Flywheel (ticks/sec)", "%.0f", launcherVelocity);
        telemetry.addData("Triggers", "left (%.2f), right (%.2f)",
                gamepad1.left_trigger, gamepad1.right_trigger);
    }

    private void runLauncherFSM(boolean shootRequested, boolean manualFeedRequested,
                                double launcherVelocity, double triggerPower) {
        double targetVelocity = 0;
        double transferPower = 0;
        double intakePower = triggerPower;

        // Each case chooses the next state and the commands to use this loop.
        switch (launcherState) {
            case IDLE:
            case MANUAL_FEED:
                if (shootRequested) {
                    launcherState = LauncherState.SPINNING_UP;
                    targetVelocity = LAUNCHER_TARGET_VELOCITY;
                } else if (manualFeedRequested) {
                    launcherState = LauncherState.MANUAL_FEED;
                    transferPower = 1;
                } else {
                    launcherState = LauncherState.IDLE;
                }
                break;

            case SPINNING_UP:
                if (!shootRequested) {
                    launcherState = manualFeedRequested ? LauncherState.MANUAL_FEED : LauncherState.IDLE;
                    transferPower = manualFeedRequested ? 1 : 0;
                    break;
                }

                targetVelocity = LAUNCHER_TARGET_VELOCITY;
                if (launcherVelocity > LAUNCHER_MIN_VELOCITY) {
                    launcherState = LauncherState.FEEDING;
                    transferPower = 1;
                    intakePower = Math.min(1.0, triggerPower + 0.5);
                }
                break;

            case FEEDING:
                if (!shootRequested) {
                    launcherState = manualFeedRequested ? LauncherState.MANUAL_FEED : LauncherState.IDLE;
                    transferPower = manualFeedRequested ? 1 : 0;
                    break;
                }

                targetVelocity = LAUNCHER_TARGET_VELOCITY;
                if (launcherVelocity <= LAUNCHER_MIN_VELOCITY) {
                    launcherState = LauncherState.SPINNING_UP;
                } else {
                    transferPower = 1;
                    intakePower = Math.min(1.0, triggerPower + 0.5);
                }
                break;
        }

        // Write each output once, after deciding what every mechanism should do.
        launcher.setVelocity(targetVelocity);
        windmillServo.setPower(transferPower);
        intake.setPower(intakePower);
        leftIntakeServo.setPower(intakePower);
        rightIntakeServo.setPower(intakePower);
    }

    private void mecanumDrive(double forward, double strafe, double rotate) {
        double leftFrontPower = forward + strafe + rotate;
        double rightFrontPower = forward - strafe - rotate;
        double leftBackPower = forward - strafe + rotate;
        double rightBackPower = forward + strafe - rotate;

        // Scale the four powers together if any one exceeds the allowed range.
        double max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
        max = Math.max(max, Math.abs(leftBackPower));
        max = Math.max(max, Math.abs(rightBackPower));

        if (max > 1.0) {
            leftFrontPower /= max;
            rightFrontPower /= max;
            leftBackPower /= max;
            rightBackPower /= max;
        }

        leftFrontDrive.setPower(leftFrontPower);
        rightFrontDrive.setPower(rightFrontPower);
        leftBackDrive.setPower(leftBackPower);
        rightBackDrive.setPower(rightBackPower);

        telemetry.addData("Drive", "left front (%.2f), right front (%.2f)",
                leftFrontPower, rightFrontPower);
    }
}