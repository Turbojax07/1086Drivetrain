package frc.robot.subsystems.drivetrain.commands;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.DriveConstants;
import frc.robot.subsystems.drivetrain.Drivetrain;
import java.util.Arrays;

public class PathfindToPose extends Command {
    private Drivetrain drivetrain;
    private Pose2d[] internalPoses;

    private Trajectory trajectory;
    private double startTime;

    private PIDController xController = new PIDController(10, 0, 0);
    private PIDController yController = new PIDController(10, 0, 0);
    private ProfiledPIDController headingController = new ProfiledPIDController(7, 0, 0, new TrapezoidProfile.Constraints(DriveConstants.maxAngularVelocity.in(RadiansPerSecond), DriveConstants.maxAngularAcceleration.in(RadiansPerSecondPerSecond)));
    
    private HolonomicDriveController holonomicController = new HolonomicDriveController(xController, yController, headingController);

    /**
     * Creates a new PathfindToPose command.
     * It controls the drivetrain and moves it to a certain pose on the field, while also travelling to other poses on the way.
     * 
     * @param drivetrain The drivetrain subsystem to control.
     * @param internalPoses The poses to pathfind to.
     */
    public PathfindToPose(Drivetrain drivetrain, Pose2d... internalPoses) {
        this.drivetrain = drivetrain;
        this.internalPoses = internalPoses;

        addRequirements(drivetrain);
    }

    /**
     * Called when the command is initially scheduled.
     * 
     * It generates the trajectory for the robot to follow.
     */
    @Override
    public void initialize() {
        // Recording the time the command starts
        startTime = System.currentTimeMillis();

        // Initializing the trajectory config
        TrajectoryConfig trajectoryConfig = new TrajectoryConfig(DriveConstants.maxLinearVelocity, DriveConstants.maxLinearAcceleration);
        trajectoryConfig.setEndVelocity(MetersPerSecond.zero());
        trajectoryConfig.setKinematics(drivetrain.getKinematics());

        // Generating the trajectory to follow;
        trajectory = TrajectoryGenerator.generateTrajectory(Arrays.asList(internalPoses), trajectoryConfig);
    }

    /**
     * Called every time the scheduler runs while the command is scheduled.
     * 
     * It gets the state of the trajectory at the current point in time and drives the robot with the output.
     */
    @Override
    public void execute() {
        // Sampling the state for this current point in time
        Trajectory.State state = trajectory.sample(System.currentTimeMillis() / 1000.0);

        // Driving the robot based on the holonomic controller output
        drivetrain.drive(holonomicController.calculate(drivetrain.getPose(), state, state.poseMeters.getRotation()));
    }

    /**
     * Returns true when the command should end.
     * 
     * It returns true when the trajectory has finished running.
     */
    @Override
    public boolean isFinished() {
        return (System.currentTimeMillis() - startTime) / 1000 > trajectory.getTotalTimeSeconds();
    }

    /**
     * Called once the command ends or is interrupted.
     * 
     * It sets the speeds of the robot to 0.
     */
    @Override
    public void end(boolean interrupted) {
        drivetrain.drive(new ChassisSpeeds());
    }
}