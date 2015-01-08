package net.danilovic.dimitriye.yamb.logic;

import net.danilovic.dimitriye.util.interfaces.Copyable;

public class Die implements Copyable, Comparable<Die> {
	
	private int value;
	
	public int getValue() {
		return value;
	}

	public Die() {
		this.value = (int)(Math.random() * 6) + 1;
	}
	
	public Die(int value) {
		if (value < 1 || value > 6) {
			throw new IllegalArgumentException("parameter `value' of Die(int value) must be between 1 and 6 inclusive");
		}
		this.value = value;
	}
	
	public Die(Die other) {
		value = other.value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		return value == ((Die) obj).value;
	}

	@Override
	public Die copy() {
		return new Die(this);
	}
	
	@Override
	public Die clone() {
		return this.copy();
	}
	
	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public int compareTo(Die o) {
		return value - o.value;
	}

}
