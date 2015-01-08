/*
 * Copyright Dimitriye Danilovic 2014. All rights reserved.
 */
package net.danilovic.dimitriye.yamb.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A bean representing a single turn a player makes.  Neither value will ever be null.
 * Also provides listening capabilities for the ending of the turn.
 */
public class Turn {
	
	private boolean ended;
	
	private Collection<Die> dice;
	
	private int rolls;
	
	/**
	 * Constructs a new turn with no rolls made so far.
	 */
	public Turn() {
		this(Collections.emptySet(), 0);
	}
	
	/**
	 * Constructs a new turn with given dice.
	 *
	 * @param dice the dice (must not be null)
	 * @throws NullPointerException if dice is null
	 */
	public Turn(Collection<Die> dice) {
		this(dice, 1);
	}
	
	/**
	 * Constructs a new turn with given dice and having
	 * made given number of rolls.
	 *
	 * @param dice the dice (must not be null)
	 * @param rolls the number of rolls made this turn
	 * @throws NullPointerException if dice is null
	 */
	public Turn(Collection<Die> dice, int rolls) {
		Objects.requireNonNull(dice);
		this.dice = dice;
		this.rolls = rolls;
	}
	
	private void verifyNotEnded() {
		if (ended) throw new IllegalStateException("Turn is complete");
	}

	/**
	 * Gets the player's dice for the turn.
	 *
	 * @return the player's dice
	 */
	public Collection<Die> getDice() {
		return dice;
	}

	/**
	 * Sets the player's dice for the turn.
	 *
	 * @param dice the dice to give the player (may not be null)
	 * @throws NullPointerException if dice is null
	 */
	public void setDice(Collection<Die> dice) {
		verifyNotEnded();
		Objects.requireNonNull(dice);
		this.dice = dice;
	}

	/**
	 * Gets how many times the dice have been rolled so far.
	 *
	 * @return the number of rolls made this turn
	 */
	public int getRolls() {
		return rolls;
	}

	/**
	 * Sets how many times the dice have been rolled so far.
	 *
	 * @param rolls the number of rolls made this turn
	 */
	public void setRolls(int rolls) {
		verifyNotEnded();
		this.rolls = rolls;
	}
	
	private final Collection<Consumer<? super Turn>> listeners = new ArrayList<>();
	
	/**
	 * Adds a listener for the end of the turn.
	 *
	 * @param listener the listener to add
	 */
	public void addTurnEndListener(Consumer<? super Turn> listener) {
		listeners.add(listener);
	}
	
	/**
	 * Ends the turn and tells listeners.
	 */
	public void end() {
		verifyNotEnded();
		ended = true;
		listeners.parallelStream().forEach(listener -> listener.accept(this));
	}

}
