package in.ceeq.lens.helpers;

import in.ceeq.lens.helpers.Helper.Corner;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class to hold a point.
 * 
 * @author x
 * 
 */
public class Poynt {

	// X coordinate
	private double x;

	// Y coordinate
	private double y;

	// Next point to current point.
	private Poynt next;

	// Center point to current point.
	private Poynt center;

	/**
	 * Default Constructor.
	 */
	public Poynt() {
		this(0, 0);
	}

	/**
	 * Constructor
	 * 
	 * @param points
	 */
	public Poynt(double[] points) {
		this();
		set(points);
	}

	/**
	 * Constructor
	 * 
	 * @param points
	 */
	public void set(double[] points) {
		if (points != null) {
			x = points.length > 0 ? points[0] : 0;
			y = points.length > 1 ? points[1] : 0;
		} else {
			x = 0;
			y = 0;
		}
	}

	/**
	 * Constructor
	 * 
	 * @param x
	 * @param y
	 */
	public Poynt(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Set X coordinate for this point.
	 * 
	 * @param x
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Set Y coordinate for this point.
	 * 
	 * @param y
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * Set Y coordinate for this point.
	 * 
	 * @param y
	 */
	public void setXY(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Get X coordinate for this point.
	 * 
	 * @return
	 */
	public double getX() {
		return x;
	}

	/**
	 * Get Y coordinate for this point.
	 * 
	 * @return
	 */
	public double getY() {
		return y;
	}

	/**
	 * Set next point to this point.
	 * 
	 * @param p
	 */
	public void setNext(Poynt p) {
		this.next = p;
	}

	/**
	 * Get next point to this point.
	 * 
	 * @return next
	 */
	public Poynt getNext() {
		return next;
	}

	/**
	 * Set center point to this point.
	 * 
	 * @param p
	 */
	public void setCenter(Poynt p) {
		this.center = p;
	}

	/**
	 * Get center point to this point.
	 * 
	 * @return next
	 */
	public Poynt getCenter() {
		return center;
	}

	/**
	 * Get distance from this point.
	 * 
	 * @param p
	 * @return distance
	 */
	public double getDistance(Poynt p) {
		return Math.sqrt(Math.pow((p.getX() - this.x), 2) + Math.pow((p.getY() - this.y), 2));
	}

	/**
	 * Get distance from this point.
	 * 
	 * @param p
	 * @return distance
	 */
	public Poynt getCenter(Poynt p) {
		return new Poynt((x + p.getX()) / 2, (y + p.getY()) / 2);
	}

	/**
	 * Clone this point.
	 * 
	 * @return point.
	 */
	public Poynt clone() {
		return new Poynt(x, y);
	}

	/**
	 * Check equality with other point.
	 * 
	 * @return equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Poynt))
			return false;
		Poynt it = (Poynt) obj;
		return x == it.x && y == it.y;
	}

	/**
	 * Print this point.
	 */
	@Override
	public String toString() {
		return "{" + x + ", " + y + "}";
	}

	/**
	 * Sort points.
	 * 
	 * @param corners
	 * @return
	 */
	public static List<Poynt> sortPoynts(List<Poynt> corners) {

		List<Poynt> result = new ArrayList<Poynt>();

		List<Poynt> top = new ArrayList<Poynt>(), bottom = new ArrayList<Poynt>();
		double cX = 0, cY = 0;

		for (Poynt pointer : corners) {
			cX += pointer.getX();
			cY += pointer.getY();
		}

		Poynt cPoint = new Poynt(cX / corners.size(), cY / corners.size());

		for (Poynt pointer : corners) {
			if (pointer.getY() < cPoint.getY()) {
				top.add(pointer);
			} else {
				bottom.add(pointer);
			}
		}

		Poynt topLeft = top.get(0).getX() > top.get(1).getX() ? top.get(1) : top.get(0);
		Poynt topRight = top.get(0).getX() > top.get(1).getX() ? top.get(0) : top.get(1);
		Poynt bottomLeft = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(1) : bottom.get(0);
		Poynt bottomRight = bottom.get(0).getX() > bottom.get(1).getX() ? bottom.get(0) : bottom.get(1);

		result.add(topLeft);
		result.add(topRight);
		result.add(bottomRight);
		result.add(bottomLeft);

		return result;
	}

	/**
	 * Get skew angle for top and bottom lines.
	 * 
	 * @param edges
	 * @return
	 */
	public static double getSkewAngle(List<Poynt> corners) {
		double skewAngle = 0;

		skewAngle += Math.atan2(corners.get(Corner.TOP_RIGHT).getY() - corners.get(Corner.TOP_LEFT).getY(),
				corners.get(Corner.TOP_RIGHT).getX() - corners.get(Corner.TOP_LEFT).getX());
		skewAngle += Math.atan2(corners.get(Corner.BOTTOM_RIGHT).getX() - corners.get(Corner.BOTTOM_LEFT).getX(), corners.get(Corner.BOTTOM_RIGHT).getX()
				- corners.get(Corner.BOTTOM_LEFT).getX());

		skewAngle /= 2;

		return skewAngle;
	}

}