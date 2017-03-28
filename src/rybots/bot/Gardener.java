package rybots.bot;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import rybots.utils.Comms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public strictfp class Gardener extends BaseBot {

    private Boolean inGoodLocation = false;
    private MapLocation spawningGap;
    private Set<MapLocation> gardenTreeLocations;

    // Generates an offset to start drawing the garden circle from. We later split the garden circle into wedges,
    // with one wedge per tree, removing the first tree to create a gap to spawn units from.
    // This ensures the spawning gap will be in a different position each time.
    private float offsetForSpawningGap = new Random().nextFloat() * (float)(Math.PI * 2);

    private MapLocation currentDestination = null;
    private Integer failedMoves = 0;

    List<Boolean> scoutHealthChecks = new ArrayList<>();

    public Gardener(RobotController rc) {
        super(rc);
    }

    public final void sayHello() {
        System.out.println("Spawning: Gardener");
    }

    public final void takeTurn() throws GameActionException {

        // If we're already in a good garden spot, stay put and maintain it.
        if( inGoodLocation ) {
            buildGarden();
            waterGarden();
            // buildScouts();
            buildSoldiersFromGarden();
        }
        // Not in a good spot, either need to go find one, or act as a wandering gardener if gardens are disabledf.
        else {
            // Garden building is enabled...
            if( rc.readBroadcastBoolean( Comms.GARDENERS_BUILD_GARDENS_CHANNEL) ) {
                // Pick a destination if we don't already have one, otherwise move toward it!
                if (currentDestination == null) {
                    searchForGardenLocation();
                } else {
                    moveToDestination();
                }
            }
            // Garden building is disabled! We need to wander around building soldiers instead!
            else {
                buildSoldiers();
                patrol();
                System.out.println("patrol mode, building...");
            }
        }

    }

    /**
     * The gardener stays put and attempts to build soldiers on a random interval.
     *
     * @throws GameActionException
     */
    private void buildSoldiersFromGarden() throws GameActionException {

        if ( Math.random() < 0.5 ) {
            if( rc.canBuildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap)) ) {
                rc.buildRobot( RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap) );
            }
        }

    }

    /**
     * The wandering gardener attempts to build soldiers in a random direction.
     *
     * @throws GameActionException
     */
    private void buildSoldiers() throws GameActionException {

        Direction randomDirection = randomDirection();

        if ( Math.random() < 0.8 ) {
            if( rc.canBuildRobot( RobotType.SOLDIER, randomDirection )) {
                rc.buildRobot( RobotType.SOLDIER, randomDirection );
            }
        }

    }

    /**
     * The gardener stays put and attempts to build a single scout.
     *
     * @throws GameActionException
     */
    private void buildScouts() throws GameActionException {
        //    /*
        //    * Gardener builds a scout if there is no scout...
        //    *   * scout sets a boolean on a heartbeat channel
        //    *   * gardener reads this boolean
        //    *   * if during the last three turns, the boolean was false this means the scout
        //    *     hasn't checked in so must be destroyed in which case so we build another...
        //    *   * archon sets the boolean to false each turn.
        //    */
        //    if( ! rc.readBroadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED ) ) {
        //        scoutHealthChecks.add(rc.readBroadcastBoolean(Comms.SCOUT_HEARTBEAT_CHANNEL));
        //        if (scoutHealthChecks.size() >= 6) {
        //            if (!scoutHealthChecks.get(0) && !scoutHealthChecks.get(1) && !scoutHealthChecks.get(2) && !scoutHealthChecks.get(3) && !scoutHealthChecks.get(4)) {
        //                rc.broadcastBoolean(Comms.SCOUT_CONSTRUCTION_ENABLED, true);
        //                System.out.println("[gardener] Scout construction enabled...");
        //            }
        //            scoutHealthChecks.clear();
        //        }
        //    }

        Direction dir = randomDirection();

        Boolean scoutBuildingEnabled = rc.readBroadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED );
        System.out.println("[gardener] Scout building status:  " + scoutBuildingEnabled);

        if( scoutBuildingEnabled && rc.canBuildRobot( RobotType.SCOUT, rc.getLocation().directionTo(spawningGap) )) {
            System.out.println("[gardener] BUILD A SCOUT!");
            rc.buildRobot( RobotType.SCOUT, rc.getLocation().directionTo(spawningGap) );
            rc.broadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED, false );
            return;
        }
    }

    /**
     * The gardener stays put and plants trees around itself.
     *
     * @throws GameActionException
     */
    private void buildGarden() throws GameActionException {

        List<MapLocation> sc = getSurroundingBuildLocations(rc.getLocation(), BULLET_TREE_RADIUS, gardenRadius(), offsetForSpawningGap);
        spawningGap = sc.remove(0);
        gardenTreeLocations = new HashSet(sc);

        // Check all tree locations and plant a tree if it is missing (not yet planted or has been destroyed).
        for (MapLocation treeLocation : gardenTreeLocations) {
            rc.setIndicatorDot(treeLocation, 128, 0, 0);
            Direction plantingLocation = rc.getLocation().directionTo(treeLocation);
            if (rc.canPlantTree(plantingLocation)) {
                rc.plantTree(plantingLocation);
            }

        }

    }

    /**
     * The gardener stays put and waters it's surrounding trees
     *
     * @throws GameActionException
     */
    private void waterGarden() throws GameActionException {

        // Get a list of all the trees in this garden, pick the first one then figure out which one
        // is the weakest and finally water it.
        TreeInfo[] gardenTrees = rc.senseNearbyTrees( rc.getLocation(), gardenRadius(), rc.getTeam() );

        // If we've planted at least one tree already, we can try to water the weakest.
        if (gardenTrees.length >= 1) {
            TreeInfo weakestTree = gardenTrees[0]; // Start with the first tree
            for (TreeInfo tree : gardenTrees) {
                rc.setIndicatorDot(tree.location, 64, 128, 0);
                if (tree.health < weakestTree.health) {
                    weakestTree = tree;
                }
            }

            if (rc.canWater(weakestTree.location)) {
                rc.water(weakestTree.location);
                rc.setIndicatorDot(weakestTree.location, 0, 128, 255);
            }
        }

    }

    /**
     * The gardener searches for suitable locations to try and move towards
     *
     * @throws GameActionException
     */
    private void searchForGardenLocation() throws GameActionException {

        // Look for some locations within sensor range that could fit our garden.
        float distance = rc.getType().sensorRadius - gardenRadius() - 0.01f;
        List<MapLocation> potentialLocations = getNSurroundingLocations(rc.getLocation(),12, distance, (float)(Math.random() * (Math.PI * 2)) );

        // Debug: show all potential spots in yellow and any good spots in green
        for (MapLocation location : potentialLocations) {
            if( isSuitableLocation(location, -2.0f) ) {
                rc.setIndicatorDot(location, 64, 128, 0);
            }
            else {
                rc.setIndicatorDot(location, 255, 255, 0);
            }
        }

        // Behaviour: iterate through the potential spots and set our destination to the first suitable looking one.
        for (MapLocation location : potentialLocations) {
            if ( isSuitableLocation(location, -2.0f) ) {
                currentDestination = location;
                moveToDestination();
                endTurn();
            }
        }

        // No good sites nearby, set indicator to red, and move randomly.
        tryMove( randomDirection() );
        rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);

    }

    /**
     * The gardener moves towards a selected location checking if it is suitable upon arrival.
     *
     * @throws GameActionException
     */
    private void moveToDestination() throws GameActionException {

        rc.setIndicatorDot( currentDestination, 128, 0, 255 );

        // If we are unable to move to the destination this time, increment a counter.
        if (!tryMove(rc.getLocation().directionTo(currentDestination))) {
            failedMoves++;
        }

        // If we have failed to move to the destination too many times, give up and pick a new destination
        // to avoid getting stuck.
        if( failedMoves >= 10 ) {
            failedMoves = 0;
            currentDestination = null;
            searchForGardenLocation();
            endTurn();
        }

        // Have we arrived yet? If the distance is less than the radius of this robot, we've made it!
        System.out.println( rc.getLocation().distanceTo( currentDestination ));
        if ( rc.getLocation().distanceTo( currentDestination ) <= rc.getType().bodyRadius ) {
            System.out.println("arrived");

            if ( isSuitableLocation( rc.getLocation(), 1.20f) ) {
                inGoodLocation = true;
                endTurn();
            } else {
                currentDestination = null;
                searchForGardenLocation();
                endTurn();
            }

        }
    }

    /**
     * Checks whether a location is suitable for building a garden.
     *
     * @param  location the MapLocation to check
     * @param  buffer   a buffer to add to the garden radius when checking if the location is suitable
     * @return          true if the given location can fit a circle of `gardenRadius()` size and there are no other robots there.
     * @throws GameActionException
     */
    private boolean isSuitableLocation(MapLocation location, float buffer) throws GameActionException {

        try {
            return rc.onTheMap(location, gardenRadius()) && !rc.isCircleOccupiedExceptByThisRobot(location, gardenRadius(buffer));
        }
        catch (Exception GameActionException) {
            // Catches the following exception which was occasionally occurring.
            //   battlecode.common.GameActionException: Target circle not completely within sensor range
            return false;
        }
    }

    /**
     * Used to determine the size of the garden as a circle.
     * A "garden" is a circle of trees around a gardener.
     *
     * @param  buffer make the radius slightly larger by this amount
     * @return        the radius of the garden
     */
    private float gardenRadius(float buffer) {
        return BULLET_TREE_RADIUS + rc.getType().bodyRadius + buffer;
    }

    /**
     * Used to determine the size of the garden as a circle, with a default buffer of 0.01f
     *
     * @return the radius of the garden
     */
    private float gardenRadius() {
        return gardenRadius(0.01f);
    }
}
