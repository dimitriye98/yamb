package net.danilovic.dimitriye.yamb.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import net.danilovic.dimitriye.yamb.interfaces.DiceScorer;
import net.danilovic.dimitriye.yamb.interfaces.ScoreContainer;

public class Scorekeeper implements ScoreContainer {
	
	private final List<ScoreContainer> subScores;
	private final Collection<Cell>     descendentCells;
	
	private final ToIntFunction<? super List<? extends ScoreContainer>> scoreCombiner;

	private Scorekeeper(Builder builder) {
		subScores       = builder.subScores;
		descendentCells = builder.descendentCells;
		
		// Combiner defaults to summation
		scoreCombiner = builder.scoreCombiner != null ? builder.scoreCombiner :
				subScores ->
				subScores.parallelStream()
				         .mapToInt(ScoreContainer::getScore)
				         .reduce((a, b) -> a + b)
				         .orElse(0);
	}

	@Override
	public int getScore() {
		return scoreCombiner.applyAsInt(subScores);
	}
	
	public Collection<Cell> cells() {
		return Collections.unmodifiableCollection(descendentCells);
	}
	
	public Collection<ScoreContainer> subScores() {
		return Collections.unmodifiableCollection(subScores);
	}
	
	public static final class Cell implements ScoreContainer {
		
		private final DiceScorer      scorer;
		private final Predicate<Turn> predicate;
		private Integer value;
		
		private Cell(DiceScorer scorer) {
			this.scorer  = scorer;
			this.predicate = null;
		}
		
		private Cell(DiceScorer scorer, Predicate<Turn> predicate) {
			this.scorer    = scorer;
			this.predicate = predicate;
		}
		
		public boolean isEnabled(Turn turn) {
			if (isFull()) return false;
			return predicate != null ? predicate.test(turn) : true;
		}
		
		public boolean isFull() {
			return value != null;
		}

		public DiceScorer getScorer() {
			return scorer;
		}
		
		public void enterScore(Turn turn) {
			if (isFull()) throw new IllegalStateException("Cell full");
			value = scorer.score(turn);
		}
		
		public int getEnteredScore() {
			if (!isFull()) throw new IllegalStateException("No value present");
			return value;
		}

		@Override
		public int getScore() {
			return isFull() ? getEnteredScore() : 0;
		}
		
	}
	
	public static final class Builder {
		
		private final List<ScoreContainer>                            subScores;
		private final Collection<Cell>                                descendentCells;
		private ToIntFunction<? super List<? extends ScoreContainer>> scoreCombiner;
		private boolean locked;
		private final Builder parent;
		
		private Builder(Builder parent) {
			this.parent = parent;
			subScores       = new ArrayList<>();
			descendentCells = new ArrayList<>();
		}
		
		private void verifyUnlocked() {
			if (locked) throw new IllegalStateException("Builders may not be reused");
		}
		
		private void addCellCascade(Cell cell) {
			verifyUnlocked();
			descendentCells.add(cell);
			if (parent != null) {
				parent.addCellCascade(cell);
			}
		}
		
		public Builder withGroup() {
			return new Builder(this);
		}
		
		public Builder withCombiner(
				ToIntFunction<? super List<? extends ScoreContainer>> combiner) {
			Objects.requireNonNull(combiner);
			this.scoreCombiner = combiner;
			return this;
		}
		
		private void addCell(Cell cell) {
			subScores.add(cell);
			addCellCascade(cell);
		}
		
		public Builder withCell(DiceScorer scorer) {
			Objects.requireNonNull(scorer);
			addCell(new Cell(scorer));
			return this;
		}
		
		public Builder withRestrictedCell(DiceScorer scorer, Predicate<Turn> predicate) {
			Objects.requireNonNull(scorer);
			addCell(new Cell(scorer, predicate));
			return this;
		}
		
		@SafeVarargs
		public final Builder withCellAndTell(DiceScorer scorer,
		                                     Consumer<? super Cell>... listeners) {
			Objects.requireNonNull(scorer);
			Cell cell = new Cell(scorer);
			addCell(cell);
			Stream.of(listeners).parallel().forEach(listener -> listener.accept(cell));
			return this;
		}
		
		@SafeVarargs
		public final Builder withRestrictedCellAndTell(
				DiceScorer scorer,
				Predicate<Turn> predicate,
				Consumer<? super Cell>... listeners) {
			Objects.requireNonNull(scorer);
			Cell cell = new Cell(scorer, predicate);
			addCell(cell);
			Stream.of(listeners).parallel().forEach(listener -> listener.accept(cell));
			return this;
		}
		
		private Scorekeeper endIMPL() {
			locked = true;
			return new Scorekeeper(this);
		}
		
		public Builder pop() {
			verifyUnlocked();
			if (parent == null)
					throw new UnsupportedOperationException("Cannot pop top-level builder");
			parent.subScores.add(endIMPL());
			return parent;
		}
		
		@SafeVarargs
		public final Builder popAndTell(Consumer<Scorekeeper>... listeners) {
			verifyUnlocked();
			if (parent == null)
					throw new UnsupportedOperationException("Cannot pop top-level builder");
			Scorekeeper built = endIMPL();
			parent.subScores.add(built);
			Stream.of(listeners).parallel().forEach(listener -> listener.accept(built));
			return parent;
		}
		
		public Scorekeeper end() {
			verifyUnlocked();
			if (parent != null)
				throw new UnsupportedOperationException("Only top-level builders may "
						+ "complete construction");
			return endIMPL();
		}
		
	}
	
	public static Builder create() {
		return new Builder(null);
	}

}
