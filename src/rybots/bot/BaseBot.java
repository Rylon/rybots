package rybots.bot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import rybots.utils.Comms;

public abstract class BaseBot {

    RobotController rc;

    BaseBot(RobotController rc) {
        this.rc = rc;
    }

    Boolean turnEnded = false;

    MapLocation currentDestination = null;
    private Float currentDestinationArrivalRange;
    private Integer currentDestinationIndicatorColourRed;
    private Integer currentDestinationIndicatorColourGreen;
    private Integer currentDestinationIndicatorColourBlue;
    private Integer failedMoves = 0;
    private List<String> navigationBadLocations = new ArrayList<>();
    private MapLocation previousLocation = new MapLocation(0,0);

    public Integer rallyPoint = null;
    public Boolean rallied = false;

    public abstract void sayHello() throws GameActionException;

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
     *
     * @return a random Direction
     */
    protected Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir, 20, 10);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir           The intended direction of movement
     * @param degreeOffset  Spacing between checked directions (degrees)
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
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * Attempts to move in a given direction
     *
     * @param dir           The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMoveNoAlternatives(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        return false;
    }

    /**
     * Returns a list of MapLocations, each one representing a circle of radius `buildItemRadius`,
     * arranged evenly in a circle of radius `outerRadius`.
     * <p>
     * Useful to find potential locations nearby capable of fitting an item of radius `buildItemRadius`.
     *
     * @param center          the center of the outer circle
     * @param buildItemRadius the radius of the inner circles
     * @param outerRadius     the radius of the outer circle
     * @param offset          the offset, in radians, to start at
     * @return a List of MapLocations
     */
    public static List<MapLocation> getSurroundingBuildLocations(MapLocation center, float buildItemRadius, float outerRadius, float offset) {
        double opposite = (double) buildItemRadius;
        double hypotenuse = (double) outerRadius;
        double wedgeAngle = Math.asin(opposite / hypotenuse) * 2;
        int numLocations = (int) ((Math.PI * 2) / wedgeAngle);

        return getNSurroundingLocations(center, numLocations, outerRadius, offset);
    }

    /**
     * Gets a list of MapLocations, equally spaced in a circle of radius `distance` from a `center` MapLocation
     * <p>
     * Similar to `getSurroundingBuildLocations`, but allows you to specify the number of locations around the circle.
     *
     * @param center       the center point of the circle
     * @param numLocations number of locations to find around the circle
     * @param radius       the radius of the circle
     * @param offset       the offset, in radians, to start the circle of locations from
     * @return a List of MapLocations
     */
    public static List<MapLocation> getNSurroundingLocations(MapLocation center, int numLocations, float radius, float offset) {
        double step = (Math.PI * 2) / numLocations;
        double currentAngle = offset;
        List<MapLocation> locations = new ArrayList<>(numLocations);

        for (int i = 0; i < numLocations; i++) {
            Direction direction = new Direction((float) currentAngle);
            locations.add(center.add(direction, radius));
            currentAngle += step;
        }

        return locations;
    }

    /**
     * Checks whether a robot is able to move in a given direction
     *
     * @param direction The intended direction to move to.
     * @return true if this robot is able to move to the location and hasn't already moved this turn.
     * @throws GameActionException
     */
    protected boolean canMove(Direction direction) throws GameActionException {
        return rc.canMove(direction) && !rc.hasMoved();
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    public boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction bulletDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = bulletDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    /**
     * The robot moves about randomly
     *
     * @throws GameActionException
     */
    public void patrol() throws GameActionException {
        if (turnEnded) {
            return;
        }

        Direction randomDirection = randomDirection();
        if (canMove(randomDirection)) {
            tryMove(randomDirection);
        }
    }

    /**
     * The robot is given a location to move toward. The destination is considered arrived at when within arrivalRange.
     * Draws an indicator line toward the destination using the given red/green/blue colours.
     *
     * @param location        The MapLocation to use for the destination
     * @param arrivalRange    A float used to judge whether the robot has 'arrived' if it is within this distance of the target.
     * @param indicatorRed    Used to draw an indicator line toward the target.
     * @param indicatorGreen  Used to draw an indicator line toward the target.
     * @param indicatorBlue   Used to draw an indicator line toward the target.
     */
    public void setDestination(MapLocation location, Float arrivalRange, Integer indicatorRed, Integer indicatorGreen, Integer indicatorBlue) {
        currentDestination = location;
        currentDestinationArrivalRange = arrivalRange;
        currentDestinationIndicatorColourRed = indicatorRed;
        currentDestinationIndicatorColourGreen = indicatorGreen;
        currentDestinationIndicatorColourBlue = indicatorBlue;
    }
    /**
     * The robot is given a location to move toward. The destination is considered arrived at when within arrivalRange.
     * No indicator lines are displayed.
     *
     * @param location        The MapLocation to use for the destination
     * @param arrivalRange    A float used to judge whether the robot has 'arrived' if it is within this distance of the target.
     */
    public void setDestination(MapLocation location, Float arrivalRange) {
        setDestination(location, arrivalRange, 0,0,0);
    }

    /**
     * Clears any existing destination.
     *
     */
    public void clearDestination() {
        currentDestination = null;
        currentDestinationArrivalRange = 0.0f;
        currentDestinationIndicatorColourRed = 0;
        currentDestinationIndicatorColourGreen = 0;
        currentDestinationIndicatorColourBlue = 0;
        navigationBadLocations.clear();
    }

    /**
     * The robot continues moving to an existing destination if it has one.
     *
     * @throws GameActionException
     */
    public boolean continueToDestination() throws GameActionException {
        if (turnEnded) {
            return false;
        }

        if (currentDestination == null) {
            return false;
        } else {

            if(currentDestinationIndicatorColourRed != 0) {
                rc.setIndicatorLine(rc.getLocation(), currentDestination,
                        currentDestinationIndicatorColourRed,
                        currentDestinationIndicatorColourGreen,
                        currentDestinationIndicatorColourBlue
                );
            }

            // Continue toward the current destination...

            if (!rc.hasMoved()) {
                // If we are unable to move to the destination this time, increment a counter.

                // Check if location would take us off the map.
                MapLocation intendedMoveLocation = rc.getLocation().add( rc.getLocation().directionTo(currentDestination), rc.getType().strideRadius * 2 );
                if(! rc.onTheMap( intendedMoveLocation ) ) {
                    System.out.println("Not on the map!");
                    failedMoves++;
                }

                List<MapLocation> rallyPoints = getNSurroundingLocations(rc.getLocation(),12, rc.getType().bodyRadius * 2.5f, 0.0f);

                Collections.sort(rallyPoints, (x, y) -> Float.compare( x.distanceTo(currentDestination), y.distanceTo(currentDestination) ));

                float min = rallyPoints.get(0).distanceTo(currentDestination);
                float max = rallyPoints.get(rallyPoints.size()-1).distanceTo(currentDestination);

                int minColour = 255;
                int maxColour = 64;

                boolean moved = false;
                for( MapLocation location : rallyPoints ) {
                    System.out.println("loop");
                    if (
                            (!rc.isCircleOccupiedExceptByThisRobot(location, 1.0f)) &&
                        ( !isValid(previousLocation) || !isSameDirection(rc.getLocation(), location, previousLocation)) ) {








                        if (tryMove(rc.getLocation().directionTo(location))) {
                            System.out.println("yes");
                            rc.setIndicatorDot(location, 128,0,255);
                            previousLocation = location;
                            moved = true;
                            System.out.println("break");
                            break;
                        }
                        else {
                            System.out.println("no");
                            navigationBadLocations.add((int)rc.getLocation().x + "" + (int)rc.getLocation().y);
                        }

                    }
                }

                System.out.println( "DISTANCE START -----------------------------");
                for( MapLocation location : rallyPoints ) {
//                    System.out.println( "Distance: " + (int)location.distanceTo(currentDestination) );
//                    System.out.println( "Distance (norm): " + normalize( location.distanceTo(currentDestination), min, max ) );

                    if ( !location.isWithinDistance( previousLocation, 1.0f ) ) {
                        System.out.println("Potential: " + (int)location.x + "," + (int)location.y);
                        System.out.println("Previous:  " + (int)previousLocation.x  + "," +  (int)previousLocation.y);
                    }
                    else {
                        System.out.println("SAME Potential: " + (int)location.x + "," + (int)location.y);
                        System.out.println("SAME Previous:  " + (int)previousLocation.x  + "," +  (int)previousLocation.y);
                    }

                    System.out.print("\n");

                    float norm = normalize( location.distanceTo(currentDestination), min, max );

                    float bob = maxColour * norm + minColour * (1 - norm);
//                    System.out.println( "Colour: " + (int)bob );

                    if ( rc.isCircleOccupiedExceptByThisRobot(location, 1.0f) ) {
                        rc.setIndicatorDot(location, 128,0,0);
                    }
//                    else if (navigationBadLocations.contains(location.x + "" + location.y)){
//                        rc.setIndicatorDot(location, 255,0,128);
//                    }
//                    else if ((int)location.x != (int)previousLocation.x && (int)location.y != (int)previousLocation.y) {
//                        rc.setIndicatorDot(location, 255,0,128);
//                    }
                    else if (isSameDirection(rc.getLocation(), location, previousLocation)) {
                        rc.setIndicatorDot(location, 255,0,128);
                    }
                    else {
                        rc.setIndicatorDot(location, 0,(int)bob,0);
                    }
                }
                System.out.println( "DISTANCE END -----------------------------");

                if(!moved) {
                    failedMoves++;
                }
//                System.out.println( "DISTANCE END -------------------------------");

            }

            // If we have failed to move to the destination too many times, give up and pick a new destination
            // to avoid getting stuck.
            if (failedMoves >= 10) {
                failedMoves = 0;
                rallied = true; // So the robot doesn't keep trying to rally to points off the map. TODO: Don't have points off the map in the first place.
                previousLocation = new MapLocation(0,0);
                clearDestination();
                endTurn();
                return false;
            } else {

                // Have we arrived yet? If the distance is less than the radius of this robot, we've made it!
                // System.out.println( rc.getLocation().distanceTo( currentDestination ));
                if (rc.getLocation().distanceTo(currentDestination) <= currentDestinationArrivalRange) {
                    previousLocation = new MapLocation(0,0);
                    clearDestination();
                    return true;
                }

            }
        }
        return false;
    }

    /*
    TODO Javadoc
     */
    private float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    /*
    TODO Javadoc
     */
    private boolean isSameDirection(MapLocation currentLocation, MapLocation potentialLocation, MapLocation previousLocation) {

//        currentLocation - previousLocation
        float currentToPotentialX;
        float currentToPotentialY;
        float previousToCurrentX;
        float previousToCurrentY;

        // Vectors
        currentToPotentialX = potentialLocation.x - currentLocation.x;
        currentToPotentialY = potentialLocation.y - currentLocation.y;

        previousToCurrentX = currentLocation.x - previousLocation.x;
        previousToCurrentY = currentLocation.y - previousLocation.y;

        // Dot product of two vectors.
        float dotProduct = currentToPotentialX * previousToCurrentX + currentToPotentialY * previousToCurrentY;

        System.out.println("Stride Radius: " + rc.getType().strideRadius);
        System.out.println("Dot Product  : " + dotProduct);

        if( rc.getType().strideRadius / dotProduct > 0.0 ){
            return true;
        }
        else {
            return false;
        }


    }

    /*
    TODO Javadoc
     */
    private boolean isValid(MapLocation location) {
        if(location.x == 0.0 && location.y == 0.0) {
            return false;
        } else {
            return true;
        }
    }

}
