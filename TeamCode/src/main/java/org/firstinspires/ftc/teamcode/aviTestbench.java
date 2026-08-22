package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.TouchSensor;

@TeleOp
public class aviTestbench extends OpMode {
    private DcMotor testMotor;
    private TouchSensor touchSensor;

    private int counter = 0;

    @Override
    public void init() {
        testMotor  = hardwareMap.get(DcMotor.class, "testMotor");
        touchSensor = hardwareMap.get(TouchSensor.class, "touchSensor");
        telemetry.addData("Status", "Initialized");

    }

    @Override
    public void loop() {
       /*
        if (touchSensor.isPressed()){
            testMotor.setPower(0.2);
        }
        if (!touchSensor.isPressed()){
            testMotor.setPower(0);
        }
*/
        counter = countTouchSensorPresses();

        telemetry.addData("touch sensor status", touchSensor.isPressed());
        telemetry.addData("motor power", testMotor.getPower());
        telemetry.addData("number of presses", counter);

    }

    public int countTouchSensorPresses() {
        boolean wasPressed = false;
        boolean isPressed = touchSensor.isPressed();

        // Count only when the sensor changes from
        // not pressed -> pressed
        if (isPressed && !wasPressed){
            counter++;
            wasPressed = isPressed;
        }

        return counter;
    }
}