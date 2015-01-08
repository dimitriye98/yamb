package net.danilovic.dimitriye.yamb.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.danilovic.dimitriye.yamb.logic.Die;

public final class ScoreUtil {

	private ScoreUtil() {}
	
	public static Die findMultiples(Collection<? extends Die> dice, int count) {
		List<Die> sortedDice = new ArrayList<>(dice);
		Collections.sort(sortedDice);
		
		int counter = 0;
		Die current = sortedDice.get(0);
		for (Die die : sortedDice) {
			if (counter == count) {
				return current;
			}
			if (current.equals(die)) {
				++counter;
			} else {
				current = die;
				counter = 1;
			}
		}
		if (counter == count) return current;
		return null;
	}

}
