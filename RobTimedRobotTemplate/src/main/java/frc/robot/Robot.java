// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DriverStation;

import com.ctre.phoenix.motorcontrol.ControlMode;
// import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
// import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.util.sendable.SendableRegistry;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer; // use timer for different modes

// never used import edu.wpi.first.wpilibj.SPI.Port;
import frc.robot.Constants.Gripper;
import frc.robot.Constants.CanBusID;
import frc.robot.Constants.JoystickPortID;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
// never used import com.revrobotics.CANSparkLowLevel;
import com.revrobotics.CANSparkLowLevel.MotorType;
// import edu.wpi.first.wpilibj.interfaces.Gyro;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

  /*
   * The first set of constants are for the autonomous functions
   */

  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto  = "My Auto";
  private static final String kRobAuto     = "Rob Auto";
  private static final String kLeoAuto     = "Leo Auto";
  private static final String kGabAuto     = "Gab Auto";
  private static final String kLeftSide    = "Left Side";
  private static final String kRightSide   = "Right Side";


  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  /*
   * These three private variables are for the three joysticks use for this robot
   */

  private Joystick m_leftStick;
  private Joystick m_rightStick;
  private Joystick m_controlStick;

  /*
   * These four private variables are for the 4 controllers for the base of the robot.
   * The controllers are Talon SRX and currently (11/14/24) running an older version of
   * software on board the controller.
   */

  private TalonSRX leftSimA;
  private TalonSRX leftSimB;
  private TalonSRX rightSimA;
  private TalonSRX rightSimB;

  /*
   * These three private variables are for the 3 controllers for the arm.
   */

  private CANSparkMax gripperMotor;
  private CANSparkMax shoulderMotor;
  private CANSparkMax wristMotor;

  private double leftSpeed;
  private double rightSpeed;

  private boolean button5;
  private boolean button6;
  
  private double          gripperSpeed = 0.0;
  private RelativeEncoder shoulderMotorEncoder;
  private double          shoulderMotorPosition;
  private RelativeEncoder wristMotorEncoder;
  private double          wristMotorPosition;

  private Timer m_timer;
  private double elapsedTime;

  /*
   * The isSimulation flag is set true if the robot code is running in simulation
   * mode. This is set in the robotPerodic function. This is set true if there are
   * not any joysticks connected.
   */

  private boolean isSimulation = false;

  private double gyroAngle = 0.0;

  // navX MXP using SPI AHRS;
  AHRS gyro = new AHRS(SPI.Port.kMXP);

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto",             kCustomAuto);
    m_chooser.addOption("Rob Auto",            kRobAuto);
    m_chooser.addOption("Leo Auto",            kLeoAuto);
    m_chooser.addOption("Gab Auto",            kGabAuto);
    m_chooser.addOption("Left Side",           kLeftSide);
    m_chooser.addOption("Right Side",          kRightSide);

    SmartDashboard.putData("Auto choices", m_chooser);

    leftSimA = new TalonSRX(CanBusID.kLeftSimA);
    leftSimB = new TalonSRX(CanBusID.kLeftSimB);

    rightSimA = new TalonSRX(CanBusID.kRightSimA);
    rightSimB = new TalonSRX(CanBusID.kRightSimB);

    gripperMotor  = new CANSparkMax(CanBusID.kGripper,       MotorType.kBrushed);
    shoulderMotor = new CANSparkMax(CanBusID.kShoulderJoint, MotorType.kBrushless);
    wristMotor    = new CANSparkMax(CanBusID.kWristJoint,    MotorType.kBrushless);

    m_leftStick    = new Joystick(JoystickPortID.kLeftJoystick);
    m_rightStick   = new Joystick(JoystickPortID.kRightJoystick);
    m_controlStick = new Joystick(JoystickPortID.kArmJoystick);

    m_timer = new Timer();

    // Set leftMotor2 to follow leftMotor1
    // Set rightMotor2 to follow rightMotor1
    
    leftSimB.follow(leftSimA);
    rightSimB.follow(rightSimA);

    // We need to invert one side of the drivetrain so that positive voltages
    // result in both sides moving forward. Depending on how your robot's
    // gearbox is constructed, you might have to invert the left side instead.

    rightSimA.setInverted(true);
    // rightSimB.setInverted(true); since rightSimB is following rightSimA I don't think I need the invert

    SendableRegistry.addLW(gyro, "Gyro");

  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {

    isSimulation = !DriverStation.isJoystickConnected(JoystickPortID.kArmJoystick);

    /*
     * Debugging the display of the angle by incrementing the gyroGetAngle by 0.01 each iteration.
     * Not sure if the code would look better if I used gyro.GetAngle() in the function
     * SmartDashboard class?
     */
     
     if (isSimulation) {
      gyroAngle += 0.01;
      if (gyroAngle > 359.99) {
        gyroAngle = 0.0;
      }
     } else {
      gyroAngle = gyro.getAngle();
     }
     
     SmartDashboard.putBoolean("isSimulation", isSimulation);
     SmartDashboard.putNumber("Gyro Angle", gyroAngle);

  }

  /**
   * This autonomous (along with the chooser code above) shows how to select between different
   * autonomous modes using the dashboard. The sendable chooser code works with the Java
   * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
   * uncomment the getString line to get the auto name from the text box below the Gyro
   *
   * <p>You can add additional auto modes by adding additional comparisons to the switch structure
   * below with additional strings. If using the SendableChooser make sure to add them to the
   * chooser code above as well.
   */
  @Override
  public void autonomousInit() {

    System.out.println("Autonomous Init function");
    m_timer.reset(); // Reset the timer at the start of test mode
    m_timer.start(); // Start the timer

    /*
     * These two statements seem like they don't need to both be there.
     */
    
    m_autoSelected = m_chooser.getSelected();
    m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    
    System.out.println("Auto selected: " + m_autoSelected);
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {

    elapsedTime = m_timer.get(); // Get the elapsed time in seconds
    SmartDashboard.putNumber("Elapsed Time", elapsedTime);

    switch (m_autoSelected) {
      case kRobAuto:
        break;
      case kCustomAuto:
        // Put custom auto code here
        break;
      case kDefaultAuto:
        break;
      case kLeoAuto:
        break;
      case kGabAuto:
        break;
      case kLeftSide:
        leftSpeed = 0.0;
        SmartDashboard.putNumber("Left Speed", leftSpeed);
        leftSimA.set(ControlMode.PercentOutput,  leftSpeed); // Set the motor speed
        SmartDashboard.putNumber("left sim a device id: ",      leftSimA.getDeviceID());
        SmartDashboard.putNumber("left sim a firmware version", leftSimA.getFirmwareVersion());
        SmartDashboard.putNumber("left sim a bus voltage: ",    leftSimA.getBusVoltage());
        SmartDashboard.putNumber("left sim a percent output",   leftSimA.getMotorOutputPercent());
        SmartDashboard.putNumber("left sim b device id: ",      leftSimB.getDeviceID());
        SmartDashboard.putNumber("left sim b percent output",   leftSimB.getMotorOutputPercent());
        break;
      case kRightSide:
        rightSpeed = 0.0;
        SmartDashboard.putNumber("Right Speed", rightSpeed);
        rightSimA.set(ControlMode.PercentOutput, rightSpeed);
        SmartDashboard.putNumber("right sim a device id: ",      rightSimA.getDeviceID());
        SmartDashboard.putNumber("right sim a firmware version", rightSimA.getFirmwareVersion());
        SmartDashboard.putNumber("right sim b device id: ",      rightSimB.getDeviceID());
        break;
      default:
        // Put default auto code here
        break;
    }
  }

  /** This function is called once when teleop is enabled. */
  @Override
  public void teleopInit() {
    System.out.println("Teleop Init function");
    m_timer.reset(); // Reset the timer at the start of test mode
    m_timer.start(); // Start the timer
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {
    if (!isSimulation) {
      leftSpeed  = m_leftStick.getY()  * 0.1; // Get the Y axis value from the joystick
      rightSpeed = m_rightStick.getY() * 0.1;
    } else {
      leftSpeed = 0.0;
      rightSpeed = 0.0;
    }

    SmartDashboard.putNumber("Left Joystick", leftSpeed);
    SmartDashboard.putNumber("Right Joystick", rightSpeed);

    leftSimA.set(ControlMode.PercentOutput,  leftSpeed); // Set the motor speed
    rightSimA.set(ControlMode.PercentOutput, rightSpeed); // set the motor speed

    if (!isSimulation) {
      button5 = m_controlStick.getRawButton(5);
      button6 = m_controlStick.getRawButton(6);
    } else {
      button5 = false;
      button6 = true;
    }
  
    gripperSpeed = 0.0;
    if (button5) {
      gripperSpeed = Gripper.kGripperSpeed;
    } else {
      gripperSpeed = 0.0;
      if (button6) {
        gripperSpeed = -Gripper.kGripperSpeed;
      } else {
        gripperSpeed = 0.0;
      }
    }
    
    gripperMotor.set(gripperSpeed);
    SmartDashboard.putNumber("Gripper Speed", gripperSpeed);

    // cmd rejected System.out.println("gripperMotor Encoder:" + gripperMotor.getEncoder());

    elapsedTime = m_timer.get(); // Get the elapsed time in seconds
    SmartDashboard.putNumber("Elapsed Time", elapsedTime);
  }

  /** This function is called once when the robot is disabled. */
  @Override
  public void disabledInit() {
    System.out.println("Disabled Init function");
    m_timer.reset(); // Reset the timer at the start of disabled mode
  }

  /** This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {
    SmartDashboard.putNumber("Elapsed Time", 0.0);
  }

  /** This function is called once when test mode is enabled. */

  /*
   * Initially test mode will be used to verify our code as we work to get everthing implemented.
   * Once we get the robot code running we can start to use test mode as a way to verify the
   * robot is working correctly.
   */
  
  @Override
  public void testInit() {
    System.out.println("Test Init function");
    m_timer.reset(); // Reset the timer at the start of test mode
    m_timer.start(); // Start the timer
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {
    shoulderMotorEncoder  = shoulderMotor.getEncoder();
    shoulderMotorPosition = shoulderMotorEncoder.getPosition();
    SmartDashboard.putNumber("Shoulder Motor Position", shoulderMotorPosition);
    
    wristMotorEncoder    = wristMotor.getEncoder();
    wristMotorPosition   = wristMotorEncoder.getPosition();
    SmartDashboard.putNumber("Wrist Motor Position", wristMotorPosition);

    if (!isSimulation) {
      button5 = m_controlStick.getRawButton(5);
      button6 = m_controlStick.getRawButton(6);
  
      gripperSpeed = 0.0;
      if (button5) {
        gripperSpeed = Gripper.kGripperSpeed;
      } else {
        gripperSpeed = 0.0;
        if (button6) {
          gripperSpeed = -Gripper.kGripperSpeed;
        } else {
          gripperSpeed = 0.0;
        }
      }

      SmartDashboard.putNumber("Gripper Speed", gripperSpeed);
      SmartDashboard.putBoolean("Button 5", button5);
      SmartDashboard.putBoolean("Button 6", button6);
    }

    elapsedTime = m_timer.get(); // Get the elapsed time in seconds
    SmartDashboard.putNumber("Elapsed Time", elapsedTime);
  }

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}

}

