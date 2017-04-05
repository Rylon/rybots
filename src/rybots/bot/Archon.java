package rybots.bot;

import battlecode.common.*;
import rybots.utils.Comms;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public strictfp class Archon extends BaseBot {

    List<Float> bulletCountHistory = new ArrayList<>();
    Boolean gardenersBuildGardens  = true;
    Direction enemyArchonDirection;

    public Archon(RobotController rc) {
        super(rc);
    }

    public final void sayHello() throws GameActionException {
        System.out.println("Spawning: Archon");

        // Set up some controls for the first turn...
        rc.broadcastBoolean( Comms.GARDENERS_BUILD_GARDENS_CHANNEL, true );
        rc.broadcastBoolean( Comms.SCOUT_CONSTRUCTION_ENABLED, true );

        // Find the direction to the enemy archon, so we can spawn units on that side of our Archon.
        MapLocation[] enemyArchon = rc.getInitialArchonLocations( rc.getTeam().opponent() );
        enemyArchonDirection = rc.getLocation().directionTo( enemyArchon[0] );
    }

    public final void takeTurn() throws GameActionException {

        setSoldierRallyPoints();
        setGardenerRallyPoints();

        // Measure the percentage rate of change of bullets over 100 turns, and if it is 30% or more,
        // stop hiring Gardeners and hire Soldiers!
        if(bulletCountHistory.size() >= 101) {
            System.out.println("[archon] Taking bullet sample!");
            System.out.println("[archon]   - % diff : " + ((bulletCountHistory.get(100) - bulletCountHistory.get(0)) / bulletCountHistory.get(100) * 100) );
            System.out.println("[archon]   - start  : " + bulletCountHistory.get(0)  );
            System.out.println("[archon]   - mid    : " + bulletCountHistory.get(50) );
            System.out.println("[archon]   - end    : " + bulletCountHistory.get(100));

            // The rules for disabling gardeners:
            //   * Rate of change over the sampling period is 30% or more.
            //   * The start, mid and end points all showed a surplus of 1000 bullets or more.
            if( (((bulletCountHistory.get(100) - bulletCountHistory.get(0)) / bulletCountHistory.get(100) * 100) >= 30) ||
                ((bulletCountHistory.get(0) >= 1000) && (bulletCountHistory.get(50) >= 1000) && (bulletCountHistory.get(100) >= 1000)) ) {
                    System.out.println("[archon]   = disabling gardens!");
                    rc.broadcastBoolean( Comms.GARDENERS_BUILD_GARDENS_CHANNEL, false);
                    gardenersBuildGardens = false;
            }
            else if( (((bulletCountHistory.get(100) - bulletCountHistory.get(0)) / bulletCountHistory.get(100) * 100) <= 30) ||
                    ((bulletCountHistory.get(0) <= 1000) && (bulletCountHistory.get(50) <= 1000) && (bulletCountHistory.get(100) <= 1000)) ) {
                System.out.println("[archon]   = enabling gardens!");
                rc.broadcastBoolean( Comms.GARDENERS_BUILD_GARDENS_CHANNEL, true);
                gardenersBuildGardens = true;
            }
            bulletCountHistory.clear();
        }
        else {
            // Add the current bullet count
            bulletCountHistory.add( rc.getTeamBullets() );
        }

        // Debug gardener hiring or not...
        if(gardenersBuildGardens) {
            rc.setIndicatorDot(rc.getLocation(), 128, 255, 0);
        }
        else {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
        }

        // Hiring time!
        // Wait for a random interval before attempting to hire a gardener.
        // If gardens are enabled, hire slower, as they take up more space.
        // If gardens are disabled due to bullet surplus, hire more frequently
        // as these "wandering gardeners" will just bumble around hiring more soldiers.
        if ( gardenersBuildGardens ) {
            hireGardenerWithChance(.2f);
        }
        else {
//            hireGardenerWithChance(.03f);
        }

    }

    /**
     * The Archon attempts to hire a gardener with a specified random chance.
     *
     * @param  chance A float representing the chance of actually hiring, for example .5 is roughly 50% of the time.
     * @throws GameActionException
     */
    private void hireGardenerWithChance(Float chance) throws GameActionException {
        if( Math.random() < chance ) {
            // If hiring gardeners is allowed, and we have the resources, do it!

            Direction direction = randomDirection();
//            if ( rc.canHireGardener( enemyArchonDirection ) ) {
//                rc.hireGardener( enemyArchonDirection );
//            }
            if ( rc.canHireGardener( direction ) ) {
                rc.hireGardener( direction );
            }
        }
    }

    private void setGardenerRallyPoints() throws GameActionException {
         List<MapLocation> rallyPoints = getNSurroundingLocations(rc.getLocation(),7, 14.0f, -1.0f);

         MapLocation[] enemyArchon = rc.getInitialArchonLocations( rc.getTeam().opponent() );

         Collections.sort(rallyPoints, (x, y) -> Float.compare( x.distanceTo(enemyArchon[0]), y.distanceTo(enemyArchon[0]) ));

         for( int i=0; i<rallyPoints.size(); i++ ) {
             if(i==0 || i==1 || i==2) {
                 rc.broadcastFloat( (int)Comms.GARDENER_RALLY_POINTS.get(i).get("x"), rallyPoints.get(i).x );
                 rc.broadcastFloat( (int)Comms.GARDENER_RALLY_POINTS.get(i).get("y"), rallyPoints.get(i).y );

                 rc.setIndicatorDot(rallyPoints.get(i), 102, 255, 255);
             }
             else {
                 rc.setIndicatorDot(rallyPoints.get(i), 0, 128, 128);
             }
        }
    }

    private void setSoldierRallyPoints() throws GameActionException {
        List<MapLocation> rallyPoints = getNSurroundingLocations(rc.getLocation(),14, 30.0f, -0.3f);

        MapLocation[] enemyArchon = rc.getInitialArchonLocations( rc.getTeam().opponent() );

        Collections.sort(rallyPoints, (x, y) -> Float.compare( x.distanceTo(enemyArchon[0]), y.distanceTo(enemyArchon[0]) ));

        for( int i=0; i<rallyPoints.size(); i++ ) {
            if(i==0 || i==1 || i==2) {
                rc.broadcastFloat( (int)Comms.SOLDIER_RALLY_POINTS.get(i).get("x"), rallyPoints.get(i).x );
                rc.broadcastFloat( (int)Comms.SOLDIER_RALLY_POINTS.get(i).get("y"), rallyPoints.get(i).y );

                rc.setIndicatorDot(rallyPoints.get(i), 102, 255, 255);
            }
            else {
                rc.setIndicatorDot(rallyPoints.get(i), 0, 128, 128);
            }
        }
    }
}
