package rybots.bot;

import rybots.utils.Comms;

import battlecode.common.*;

import java.util.Random;

public strictfp class Soldier extends BaseBot {

    private Team enemy;
    private MapLocation currentDestination = null;
    private Integer failedMoves = 0;

    private Integer rallyPoint = null;
    private Boolean rallied = false;

    public Soldier(RobotController rc) {
        super(rc);
        enemy = rc.getTeam().opponent();
    }

    public final void sayHello() {
        System.out.println("Spawning: Soldier");
    }

    public final void takeTurn() throws GameActionException {
        dodgeIncomingFire();
        shootAtEnemies();
        moveToRallyPoint();
        lookForTrouble();
        patrol();
    }

    /**
     * The soldier chooses a random rally point and moves there.
     *
     * @throws GameActionException
     */
    private void moveToRallyPoint() throws GameActionException {
        if (turnEnded) {
            return;
        }

        if (rallied) {
            return;
        } else {

            if ( rallyPoint == null ) {
                rallyPoint = new Random().nextInt(3);
            }

            if (currentDestination == null) {

                // Read the rally point coordinates from the broadcast.
                Float x = rc.readBroadcastFloat((int)Comms.SOLDIER_RALLY_POINTS.get(rallyPoint).get("x"));
                Float y = rc.readBroadcastFloat((int)Comms.SOLDIER_RALLY_POINTS.get(rallyPoint).get("y"));

                if (x != 0.0 && y != 0.0) {
                    currentDestination = new MapLocation(x, y);
                }

            } else {
            rc.setIndicatorLine( rc.getLocation(), currentDestination, 64, 0, 128 );
                // Continue toward the current destination...

                if (!rc.hasMoved()) {
                    // If we are unable to move to the destination this time, increment a counter.
                    if (!tryMove(rc.getLocation().directionTo(currentDestination))) {
                        failedMoves++;
                    }
                }

                // If we have failed to move to the destination too many times, give up and pick a new destination
                // to avoid getting stuck.
                if (failedMoves >= 10) {
                    failedMoves = 0;
                    currentDestination = null;
                    endTurn();
                }

                // Have we arrived yet? If the distance is less than the radius of this robot, we've made it!
                // System.out.println( rc.getLocation().distanceTo( currentDestination ));
                 if ( rc.getLocation().distanceTo( currentDestination ) <= (rc.getType().bodyRadius * 2) ) {
                    currentDestination = null;
                    rallied = true;
                 }
            }

        }
    }

    /**
     * The soldier tries to dodge incoming bullets.
     *
     * @throws GameActionException
     */
    private void dodgeIncomingFire() throws GameActionException {
        if (turnEnded) {
            return;
        }

        // Todo: collect a list of bullets that will collide, and sort by distance to robot, closest one is evaded first...

        // See if there are any nearby bullets.
        BulletInfo[] bullets = rc.senseNearbyBullets(2.0f);

        // If there are some...
        if (bullets.length > 0) {
            for (BulletInfo bullet : bullets) {
                // And the path of the bullet suggests it will collide with me.
                if (willCollideWithMe(bullet)) {
                    rc.setIndicatorLine(bullet.location, bullet.location.add(bullet.dir, 5.0f), 255, 102, 102);
                    takeEvasiveAction(bullet);
                    System.out.println("Evading, return!");
                    endTurn();

                }
            }
        }
    }

    /**
     * The soldier stays put and fires at nearby enemy soldiers.
     *
     * @throws GameActionException
     */
    private void shootAtEnemies() throws GameActionException {
        if (turnEnded) {
            return;
        }

        // See if there are any nearby enemy robots
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

        // If there are some...
        if (robots.length > 0) {

            // Report the location for other soldiers to read
            rc.broadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_X_CHANNEL, robots[0].location.x);
            rc.broadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_Y_CHANNEL, robots[0].location.y);

            // And we have enough bullets, and haven't attacked yet this turn...
            if (rc.canFireSingleShot()) {
                // ...Then fire a bullet in the direction of the enemy.
                rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
            }
            // End turn so the robot stays near these enemies until they are destroyed.
            endTurn();
        }
    }

    /**
     * The soldier attempts to move toward a location which was broadcast, which should contain enemies.
     *
     * @throws GameActionException
     */
    private void lookForTrouble() throws GameActionException {
        if (turnEnded) {
            return;
        }

        if (currentDestination == null) {

            // See if there are any coordinates broadcasted yet...
            Float x = rc.readBroadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_X_CHANNEL);
            Float y = rc.readBroadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_Y_CHANNEL);

            // System.out.println( "Broadcast coordinates: " + x + ":" + y);

            if (x != 0.0 && y != 0.0) {
                currentDestination = new MapLocation(x, y);
            }

        } else {
//            rc.setIndicatorLine( rc.getLocation(), currentDestination, 180, 0, 0 );
            // Continue toward the current destination...

            if (!rc.hasMoved()) {
                // If we are unable to move to the destination this time, increment a counter.
                if (!tryMove(rc.getLocation().directionTo(currentDestination))) {
                    failedMoves++;
                }
            }

            // If we have failed to move to the destination too many times, give up and pick a new destination
            // to avoid getting stuck.
            if (failedMoves >= 10) {
                failedMoves = 0;
                currentDestination = null;
                endTurn();
            }

            // Have we arrived yet? If the distance is less than the radius of this robot, we've made it!
            // System.out.println( rc.getLocation().distanceTo( currentDestination ));
             if ( rc.getLocation().distanceTo( currentDestination ) <= (rc.getType().bodyRadius * 2) ) {
                currentDestination = null;
             }
        }
    }

    /**
     * The solider attempts to evade an incoming bullet which is on a collision course
     *
     * @param bullet The bullet in question
     * @throws GameActionException
     */
    private void takeEvasiveAction(BulletInfo bullet) throws GameActionException {
        Direction evadeDirection = bullet.dir.rotateRightDegrees(90);

        if (canMove(evadeDirection)) {
            tryMove(evadeDirection);
        }
    }

}
