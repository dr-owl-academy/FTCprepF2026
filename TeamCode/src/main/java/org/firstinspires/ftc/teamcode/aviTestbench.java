package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.TouchSensor;

@TeleOp
public class aviTestbench extends OpMode {
    private DcMotor testMotor;
    private TouchSensor touchSensor;

    @Override
    public void init() {
        testMotor  = hardwareMap.get(DcMotor.class, "testMotor");
        touchSensor = hardwareMap.get(TouchSensor.class, "touchSensor");
        telemetry.addData("Status", "Initialized");

    }

    @Override
    public void loop() {
        if (touchSensor.isPressed()){
            testMotor.setPower(0.2);
        }
        if (!touchSensor.isPressed()){
            testMotor.setPower(0);
        }

        telemetry.addData("touch sensor status", touchSensor.isPressed());
        telemetry.addData("motor power", testMotor.getPower());
    }
}