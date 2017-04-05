package rybots.bot;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import rybots.utils.Comms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Collections;

public strictfp class Gardener extends BaseBot {

    private Boolean inGoodLocation = false;
    private MapLocation spawningGap;
    private Set<MapLocation> gardenTreeLocations;

    // Generates an offset to start drawing the garden circle from. We later split the garden circle into wedges,
    // with one wedge per tree, removing the first tree to create a gap to spawn units from.
    // This ensures the spawning gap will be in a different position each time.
    private float offsetForSpawningGap = new Random().nextFloat() * (float)(Math.PI * 2);

    List<Boolean> scoutHealthChecks = new ArrayList<>();

    private Integer rallyPoint = null;
    private Boolean rallied = false;

    public Gardener(RobotController rc) {
        super(rc);
    }

    public final void sayHello() throws GameActionException {
        System.out.println("Spawning: Gardener");

//        // Choose a random rally point, determine the coordinates and set it as our destination.
//        rallyPoint = new Random().nextInt(3);
//
//        Float x = rc.readBroadcastFloat((int)Comms.GARDENER_RALLY_POINTS.get(rallyPoint).get("x"));
//        Float y = rc.readBroadcastFloat((int)Comms.GARDENER_RALLY_POINTS.get(rallyPoint).get("y"));
//
//        setDestination(new MapLocation(x, y), rc.getType().bodyRadius * 4, 64, 0 , 128);
    }

    public final void takeTurn() throws GameActionException {

        // If we're already in a good garden spot, stay put and maintain it.
        if( inGoodLocation ) {
            buildSoldiersFromGarden();
            buildGarden();
            waterGarden();
            // buildScouts();
        }
        // Not in a good spot, either need to go find one, or act as a wandering gardener if gardens are disabledf.
        else {
            if( currentDestination == null ) {
                searchForGardenLocation();
            }
            if( continueToDestination() ) {
              if( isSuitableLocation(rc.getLocation(), 0.7f) ) {
                  inGoodLocation = true;
              }
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
            if(spawningGap == null) {
                // No spawn time yet...
            } else {
                if (rc.canBuildRobot(RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap))) {
                    rc.buildRobot(RobotType.SOLDIER, rc.getLocation().directionTo(spawningGap));
                }
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

        List<MapLocation> locations = getSurroundingBuildLocations(rc.getLocation(), BULLET_TREE_RADIUS, gardenRadius(), offsetForSpawningGap);

        // Spawning gap should be whichever location in the garden is closest to the archon, which should result in the spawning gap
        // facing the enemy.
        MapLocation[] enemyArchon = rc.getInitialArchonLocations( rc.getTeam().opponent() );
        Collections.sort(locations, (x, y) -> Float.compare( x.distanceTo(enemyArchon[0]), y.distanceTo(enemyArchon[0]) ));
        spawningGap = locations.remove(0);
        gardenTreeLocations = new HashSet(locations);

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
        float distance = rc.getType().sensorRadius - (gardenRadius() * 2) - 0.01f;
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
//                setDestination(location.add(rc.getLocation().directionTo(location), 3.0f), rc.getType().bodyRadius * 3, 128, 255 , 0);
                Direction myLocation = randomDirection();
                setDestination(rc.getLocation().add(myLocation,2.0f),2.1f, 128, 255 , 0 );
            }
        }

        // No good sites nearby, set indicator to red, and move randomly.
//        tryMove( randomDirection() );
//        rc.setIndicatorDot(rc.getLocation(), 128, 0, 0);

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
