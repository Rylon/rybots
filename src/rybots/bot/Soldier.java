package rybots.bot;

import rybots.utils.Comms;

import battlecode.common.*;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public strictfp class Soldier extends BaseBot {

    private Team enemy;

    public Soldier(RobotController rc) {
        super(rc);
        enemy = rc.getTeam().opponent();
    }

    public final void sayHello() throws GameActionException {
        System.out.println("Spawning: Soldier");

        // Choose a random rally point, determine the coordinates and set it as our destination.
        rallyPoint = new Random().nextInt(3);

        Float x = rc.readBroadcastFloat((int)Comms.SOLDIER_RALLY_POINTS.get(rallyPoint).get("x"));
        Float y = rc.readBroadcastFloat((int)Comms.SOLDIER_RALLY_POINTS.get(rallyPoint).get("y"));

        setDestination(new MapLocation(x, y), rc.getType().bodyRadius * 2, 64, 0 , 128);
    }

    public final void takeTurn() throws GameActionException {
        dodgeIncomingFire();
        shootAtEnemies();
        lookForTrouble();
        continueToDestination();
        patrol();
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
            if (rc.canFireTriadShot()) {
                // ...Then fire a bullet in the direction of the enemy.
                rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
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
            // See if there are any coordinates broadcasted yet and set them as our current destination
            // if there are.
            Float x = rc.readBroadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_X_CHANNEL);
            Float y = rc.readBroadcastFloat(Comms.SOLDIER_ENEMY_SPOTTED_Y_CHANNEL);

            if (x != 0.0 && y != 0.0) {
                setDestination(new MapLocation(x, y), rc.getType().bodyRadius * 4, 128,0,0);
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

        // TODO: Consider all nearby bullets for evasion = try to work out the best spot.

        List<Direction> evadeDirections = new ArrayList<>();

        evadeDirections.add( bullet.dir.rotateRightDegrees(110) );
        evadeDirections.add( bullet.dir.rotateLeftDegrees(110) );
        evadeDirections.add( bullet.dir.rotateRightDegrees(70) );
        evadeDirections.add( bullet.dir.rotateLeftDegrees(70) );

        Collections.shuffle(evadeDirections);

        for( Direction direction : evadeDirections ) {
            if (canMove(direction)) {
                tryMove(direction);
                return;
            }
        }
    }

}
