package net.danilovic.dimitriye.yamb.interfaces;

import net.danilovic.dimitriye.yamb.logic.Turn;

@FunctionalInterface
public interface DiceScorer {
	
	int score(Turn turn);

}
