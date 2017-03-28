package rybots.bot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseBot {

    Boolean turnEnded = false;
    RobotController rc;

    BaseBot(RobotController rc) {
        this.rc = rc;
    }

    public abstract void sayHello();
    public abstract void takeTurn() throws GameActionException;

    /**
     * Starts a new turn by setting the "turnEnded" boolean to false.
     * Turn methods should check for this boolean and return immediately if it is true
     * to allow for skipping to the end of the robots turn.
     */
    public void newTurn() {
        turnEnded = false;
    }

    /**
     * Marks the turn as ended, so any further turn actions can be skipped.
     * Turn methods should check for this boolean and return immediately if it is true.
     * to allow for skipping to the end of the robots turn.
     */
    protected void endTurn() {
        turnEnded = true;
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    protected Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param  dir The intended direction of movement
     * @return     true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,5);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param  dir           The intended direction of movement
     * @param  degreeOffset  Spacing between checked directions (degrees)
     * @param  checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return               true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }



    /**
     * Returns a list of MapLocations, each one representing a circle of radius `buildItemRadius`,
     * arranged evenly in a circle of radius `outerRadius`.
     *
     * Useful to find potential locations nearby capable of fitting an item of radius `buildItemRadius`.
     *
     * @param  center          the center of the outer circle
     * @param  buildItemRadius the radius of the inner circles
     * @param  outerRadius     the radius of the outer circle
     * @param  offset          the offset, in radians, to start at
     * @return                 a List of MapLocations
     */
    public static List<MapLocation> getSurroundingBuildLocations(MapLocation center, float buildItemRadius, float outerRadius, float offset) {
        double opposite = (double)buildItemRadius;
        double hypotenuse = (double)outerRadius;
        double wedgeAngle = Math.asin(opposite / hypotenuse) * 2;
        int numLocations = (int) ((Math.PI * 2) / wedgeAngle);

        return getNSurroundingLocations(center, numLocations, outerRadius, offset);
    }

    /**
     * Gets a list of MapLocations, equally spaced in a circle of radius `distance` from a `center` MapLocation
     *
     * Similar to `getSurroundingBuildLocations`, but allows you to specify the number of locations around the circle.
     *
     * @param  center       the center point of the circle
     * @param  numLocations number of locations to find around the circle
     * @param  radius       the radius of the circle
     * @param  offset       the offset, in radians, to start the circle of locations from
     * @return              a List of MapLocations
     */
    public static List<MapLocation> getNSurroundingLocations(MapLocation center, int numLocations, float radius, float offset) {
        double step = (Math.PI * 2) / numLocations;
        double currentAngle = offset;
        List<MapLocation> locations = new ArrayList<>(numLocations);

        for (int i = 0; i < numLocations; i++) {
            Direction direction = new Direction( (float)currentAngle );
            locations.add( center.add(direction, radius) );
            currentAngle += step;
        }

        return locations;
    }

    /**
     * Checks whether a robot is able to move in a given direction
     *
     * @param  direction The intended direction to move to.
     * @return           true if this robot is able to move to the location and hasn't already moved this turn.
     * @throws GameActionException
     */
    protected boolean canMove(Direction direction) throws GameActionException {
        return rc.canMove(direction) && !rc.hasMoved();
    }

}
