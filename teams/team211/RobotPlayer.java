package team211;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

/*
 * Goals
 * - capture camps
 * - research NUKE
 * 	- when is this appropriate?
 * - protect my camps
 * - prevent enemy from getting camps.
 * - attack enemy HQ
 * - heal damaged units
 * - destroy weak enemy units (isolated and/or damaged)
 * 
 * > Identify camps which are probably good to go after.
 * > Coordinate dispatch of units to camps.
 * > Build something on the camps.
 */

/* Ideas:
 * - fuzzy search for "something" in a particular direction (cone-like expansion of search area).
 * - Use bytecode budget of things that are working (ie: HQ while spawning bots, bots while
 *       laying/defusing/capturing) to do higher level calculations & broadcast the information.
 * - Assign "ROLES" to subsets of RobotTypes (some will be "guards", "attack", "explore", "capture")
 */
public class RobotPlayer {
	
	private static void r_hq(RobotController rc) {
		Direction spawn_dir = null; /* if (!null), indicates that a spawn in this direction is in progress. */
		while(true) {
			try {
				if (rc.isActive()) {
					// We probably just finished spawning a solder.
					// Can we keep track of it?
					// Spawn a soldier
					if (rc.getTeamPower() > 10) {
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir)) {
							rc.spawn(dir);
						} else {
							/* try some other directions? */
						}
					}
				} else {
					int id = rc.getRobot().getID();
					/* Decide where to place info (and how to encode it) */
					rc.broadcast(0, id);
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void careful_move(RobotController rc, Direction dir, MapLocation my_loc, Team my_team) throws GameActionException {
		if(rc.canMove(dir)) {
			MapLocation new_loc = my_loc.add(dir);
			Team maybe_mine = rc.senseMine(new_loc);
			if (maybe_mine != null && maybe_mine != my_team) {
				rc.defuseMine(new_loc);
			} else {
				rc.move(dir);
				rc.setIndicatorString(0, "Last direction moved: " + dir);
			}
		}
	}
	
	private static MapLocation find_closest_camp(RobotController rc) throws GameActionException {
		int r_sq = 50;
		MapLocation [] locs = null;
		while (locs == null || locs.length == 0) {
			locs = rc.senseEncampmentSquares(rc.getLocation(), r_sq, Team.NEUTRAL);
			r_sq = 4 * r_sq;
		}
		return locs[(int)(Math.random() * locs.length)];
	}
	
	private static void random_careful_move(RobotController rc, MapLocation my_loc, Team my_team)
			throws GameActionException {
		// Choose a random direction, and move that way if possible
		Direction [] dirs = Direction.values();
		int start = (int)(Math.random()*8);
		int c = start;
		Direction dir = dirs[c];
		while (!rc.canMove(dir)) {
			c = (c + 3) % 8; /* TODO: does this cover all values? */
			if (c == start)
				return; /* can't move anywhere */
			dir = dirs[c];
		}
		
		careful_move(rc, dir, my_loc, my_team);
	}
	
	private static void careful_move_toward(RobotController rc, MapLocation goal, MapLocation my_loc, Team my_team)
			throws GameActionException {
		Direction dir = my_loc.directionTo(goal);
		careful_move(rc, dir, my_loc, my_team);
	}
	
	private static void r_soilder_capper(RobotController rc) {
		Team my_team = rc.getTeam();
		MapLocation camp_goal = null;
		while(true) {
			try {
				if (rc.isActive()) {
					MapLocation my_loc = rc.getLocation();
					if (camp_goal == null) {
						camp_goal = find_closest_camp(rc);
					}
					
					if (my_loc.equals(camp_goal)) {
						if (Math.random() > 0.5) {
							rc.captureEncampment(RobotType.SUPPLIER);
						} else {
							rc.captureEncampment(RobotType.GENERATOR);
						}
						camp_goal = null;
					} else {
						/* look for closest unclaimed, unoccupied camp. */
						Direction dir = my_loc.directionTo(camp_goal);
						if (rc.canMove(dir)) {
							careful_move(rc, dir, my_loc, my_team);
						} else {
							random_careful_move(rc, my_loc, my_team);
						}
					}
				}
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void r_soilder(RobotController rc) {
		Team my_team = rc.getTeam();
		while(true) {
			try {
				if (rc.isActive()) {
					MapLocation my_loc = rc.getLocation();
					//boolean on_bad_mine = my_team != rc.senseMine(my_loc);
					
					if (Math.random()<0.005 && rc.senseMine(my_loc) == null) {
						if(rc.getTeamPower() > 10)
							rc.layMine();
					} else { 		
						random_careful_move(rc, my_loc, my_team);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void r_arty(RobotController rc) {
		while(true) {
			rc.yield();
		}
	}
	
	private static void r_other(RobotController rc) {
		System.out.println("r_other: robot type = " + rc.getType());
		while(true) {
			try {
				rc.broadcast((int)(Math.random()*GameConstants.BROADCAST_MAX_CHANNELS), (int)(Math.random()*65535));
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	public static void run(RobotController rc) {
		RobotType rt = rc.getType();
		if (rt == RobotType.HQ) {
			r_hq(rc);
		} else if (rt == RobotType.SOLDIER) {
			while(true) {
				r_soilder_capper(rc);
				if (Math.random() > 0.9) {
					r_soilder(rc);
				} else {
					r_soilder_capper(rc);
				}
			}
		} else if (rt == RobotType.ARTILLERY) {
			r_arty(rc);
		} else {
			r_other(rc);
		}
	}
}
