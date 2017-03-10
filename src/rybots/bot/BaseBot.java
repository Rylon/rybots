package rybots.bot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

abstract class BaseBot {

    RobotController rc;
    BaseBot(RobotController rc) {
        this.rc = rc;
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
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
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
     * @param center          the center of the outer circle
     * @param buildItemRadius the radius of the inner circles
     * @param outerRadius     the radius of the outer circle
     * @param offset          the offset, in radians, to start at
     * @return                a List of MapLocations
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
     * @param center       the center point of the circle
     * @param numLocations number of locations to find around the circle
     * @param radius       the radius of the circle
     * @param offset       the offset, in radians, to start the circle of locations from
     * @return             a List of MapLocations
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


}
