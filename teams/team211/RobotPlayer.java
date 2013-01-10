package team211;

import battlecode.common.Direction;
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
 */

public class RobotPlayer {
	
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						if (rc.getTeamPower() > 10) {
							Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							if (rc.canMove(dir))
								rc.spawn(dir);
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						long [] mem = rc.getTeamMemory();
						if (mem[0] == 'H')
							System.out.println("SYS MEM IS A GO.");
						Team my_team = rc.getTeam();
						MapLocation my_loc = rc.getLocation();
						//boolean on_bad_mine = my_team != rc.senseMine(my_loc);
						
						if (Math.random()<0.005) {
							// Lay a mine 
							if(rc.senseMine(my_loc) == null && rc.getTeamPower() > 10)
								rc.layMine();
						} else { 
							// Choose a random direction, and move that way if possible
							Direction dir = Direction.values()[(int)(Math.random()*8)];
							if(rc.canMove(dir)) {
								MapLocation new_loc = my_loc.add(dir);
								Team maybe_mine = rc.senseMine(new_loc);
								if (maybe_mine != null && maybe_mine != my_team) {
									rc.defuseMine(new_loc);
								} else {
									rc.move(dir);
									rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
								}
							}
						}
					}
				} else if (rc.getType() == RobotType.ARTILLERY) {
					/* TODO: Some arty logic */
				} else {
					System.out.println("Unknown type " + rc.getType());
				}

				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
