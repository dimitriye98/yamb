package net.danilovic.dimitriye.yamb.gui;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.danilovic.dimitriye.util.Unit;
import net.danilovic.dimitriye.yamb.interfaces.DiceScorer;
import net.danilovic.dimitriye.yamb.interfaces.ScoreContainer;
import net.danilovic.dimitriye.yamb.logic.Die;
import net.danilovic.dimitriye.yamb.logic.Scorekeeper;
import net.danilovic.dimitriye.yamb.logic.Scorekeeper.Builder;
import net.danilovic.dimitriye.yamb.logic.Scorekeeper.Cell;
import net.danilovic.dimitriye.yamb.logic.Turn;


public class YambScorecard extends JFrame implements ScoreContainer {

	private static final long serialVersionUID = 2437955937995726432L;
	
	private final Map<JButton, Cell>               cellMap = new HashMap<>();
	private final Map<JTextField, Scorekeeper> subtotalMap = new HashMap<>();
	private final Scorekeeper                   scoresheet;
	
	private Cell addRestrictedCell(JComponent      parent,
	                               Builder         builder,
	                               DiceScorer      scorer,
	                               Predicate<Turn> predicate) {
		final Unit<Cell> ret = new Unit<>();
		builder.withRestrictedCellAndTell(scorer, predicate, cell -> {
			JButton button = new JButton();
			parent.add(button);
			cellMap.put(button, cell);
			ret.setValue(cell);
		});
		return ret.getValue();
	}
	
	private Cell addCell(JComponent parent, Builder builder, DiceScorer scorer) {
		return addRestrictedCell(parent, builder, scorer, null);
	}
	
	private Cell addNumericCell(JComponent parent, Builder builder, int n) {
		return addCell(parent, builder, turn -> turn.getDice()
		                                            .parallelStream()
		                                            .mapToInt(Die::getValue)
		                                            .filter(i -> i == n)
		                                            .reduce((a, b) -> a + b)
		                                            .orElse(0));
	}
	
	private Cell addRestrictedNumericCell(JComponent parent,
	                                      Builder builder,
	                                      int n,
	                                      Predicate<Turn> predicate) {
		return addRestrictedCell(parent, builder, turn -> turn.getDice()
		                                                      .parallelStream()
		                                                      .mapToInt(Die::getValue)
		                                                      .filter(i -> i == n)
		                                                      .reduce((a, b) -> a + b)
		                                                      .orElse(0), predicate);
	}
	
	private Builder addGroup(JComponent parent, Builder builder) {
		return builder.popAndTell(sheet -> {
			JTextField field = new JTextField();
			field.setHorizontalAlignment(JTextField.CENTER);
			field.setEnabled(false);
			parent.add(field);
			subtotalMap.put(field, sheet);
		});
	}

	public YambScorecard(String title) {
		super(title);
		
		Builder builder = Scorekeeper.create();
		
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
			
			JPanel columns = new JPanel(new GridLayout(1, 0));
			
				JPanel rowLabels = new JPanel(new GridLayout(0, 1));

					rowLabels.add(new JLabel("", JLabel.CENTER));
					rowLabels.add(new JLabel("Aces", JLabel.CENTER));
					rowLabels.add(new JLabel("Deuces", JLabel.CENTER));
					rowLabels.add(new JLabel("Treys", JLabel.CENTER));
					rowLabels.add(new JLabel("Fours", JLabel.CENTER));
					rowLabels.add(new JLabel("Fives", JLabel.CENTER));
					rowLabels.add(new JLabel("Sixes", JLabel.CENTER));
					rowLabels.add(new JLabel("", JLabel.CENTER));
					rowLabels.add(new JLabel("Max", JLabel.CENTER));
					rowLabels.add(new JLabel("Min", JLabel.CENTER));
					rowLabels.add(new JLabel("Subtotal", JLabel.CENTER));
					rowLabels.add(new JLabel("", JLabel.CENTER));
					rowLabels.add(new JLabel("Straight", JLabel.CENTER));
					rowLabels.add(new JLabel("Full", JLabel.CENTER));
					rowLabels.add(new JLabel("Poker", JLabel.CENTER));
					rowLabels.add(new JLabel("Yamb", JLabel.CENTER));
					rowLabels.add(new JLabel("", JLabel.CENTER));
					rowLabels.add(new JLabel("Total", JLabel.CENTER));
				
				columns.add(rowLabels);
				
				JPanel down = new JPanel(new GridLayout(0, 1));
				builder = builder.withGroup();
				
					// FIXME: Massive code duplication
				
					down.add(new JLabel("Down", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						// Use in min-max section scoring
						Cell downAces  = addNumericCell(down, builder, 1);
						Cell prevCell = downAces;
						
						for (int i = 2; i <= 6; ++i) {
							final Cell previous = prevCell;
							prevCell = addRestrictedNumericCell(down, builder, i,
									turn -> previous.isFull());
						}
					
					builder = builder.pop();
					
					down.add(new JLabel("", JLabel.CENTER));
					
					// (Max - Min) * Aces
					builder = builder.withGroup()
					                 .withCombiner(scores ->
					                		 (scores.get(0).getScore()
			                				 - scores.get(1).getScore())
					                		 * downAces.getScore());
					
						final Cell downSixes = prevCell;
						final Cell downMax = addRestrictedCell(down, builder,
								turn -> turn.getDice()
									.parallelStream()
									.mapToInt(Die::getValue)
									.reduce((a, b) -> a + b)
									.getAsInt(),
								
								turn -> downSixes.isFull());
						
						final Cell downMin = addRestrictedCell(down, builder,
								turn -> turn.getDice()
									.parallelStream()
									.mapToInt(Die::getValue)
									.reduce((a, b) -> a + b)
									.getAsInt(),
								
								turn -> downMax.isFull());
						
					builder = addGroup(down, builder);
					
					down.add(new JLabel("", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						final Cell downStraight = addRestrictedCell(down, builder,
								turn -> {
									Collection<Integer> dice = turn.getDice()
									                               .parallelStream()
									                               .map(Die::getValue)
									                               .collect(
									                            	   Collectors.toList()
									                               );
									
									if (dice.containsAll(Arrays.asList(2, 3, 4, 5))) {
										if (dice.contains(1)) {
											return 35;
										} else if (dice.contains(6)) {
											return 40;
										}
									}
									
									return 0;
								},
								
								turn -> downMin.isFull());
						
						final Cell downFull = addRestrictedCell(down, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									final Map<Die, Integer> countMap = new HashMap<>();
									
									dice.stream().forEach(die ->
											countMap.put(die, 
													countMap.getOrDefault(die, 0) + 1));
									
									if (countMap.values()
											.containsAll(Arrays.asList(2, 3))) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 30;
									} else {
										return 0;
									}
								},
								
								turn -> downStraight.isFull());
						
						final Cell downPoker = addRestrictedCell(down, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									Die d = null;
									int c = 1;
									for (Die die : dice) {
										if (d == null) {
											d = die;
										} else if (d.equals(die)) {
											++c;
										}
									}
									
									final Die dFin = d;
									
									if (c == 1 && dice.parallelStream()
									                  .skip(1)
									                  .distinct()
									                  .count() == 1) {
										return dice.parallelStream()
										           .filter(die -> !dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else if (c == 4) {
										return dice.parallelStream()
										           .filter(die -> dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else {
										return 0;
									}
								},
								
								turn -> downFull.isFull());
					
						addRestrictedCell(down, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									if (dice.parallelStream().distinct().count() == 1) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 50;
									} else {
										return 0;
									}
								},
								
								turn -> downPoker.isFull());
						
					builder = builder.pop();
					
					down.add(new JLabel("", JLabel.CENTER));
				
				columns.add(down);
				builder = builder.pop();
				
				JPanel choice = new JPanel(new GridLayout(0, 1));
				builder = builder.withGroup();
				
					choice.add(new JLabel("Choice", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						// Use in min-max section scoring
						Cell choiceAces = addNumericCell(choice, builder, 1);
						
						for (int i = 2; i <= 6; ++i) {
							addNumericCell(choice, builder, i);
						}
					
					builder = builder.pop();
					
					choice.add(new JLabel("", JLabel.CENTER));
					
					// (Max - Min) * Aces
					builder = builder.withGroup()
					                 .withCombiner(scores -> 
					            		     (scores.get(0).getScore()
					            		     - scores.get(1).getScore())
					            		     * choiceAces.getScore());
					
						addCell(choice, builder, turn -> turn.getDice()
						                                     .parallelStream()
						                                     .mapToInt(Die::getValue)
						                                     .reduce((a, b) -> a + b)
						                                     .getAsInt());
						
						addCell(choice, builder, turn -> turn.getDice()
						                                     .parallelStream()
						                                     .mapToInt(Die::getValue)
						                                     .reduce((a, b) -> a + b)
						                                     .getAsInt());
						
					builder = addGroup(choice, builder);
					
					choice.add(new JLabel("", JLabel.CENTER));
					
					builder = builder.withGroup();
						
						addCell(choice, builder,
								turn -> {
									Collection<Integer> dice = turn.getDice()
									                               .parallelStream()
									                               .map(Die::getValue)
									                               .collect(
									                            	   Collectors.toList()
									                               );
									
									if (dice.containsAll(Arrays.asList(2, 3, 4, 5))) {
										if (dice.contains(1)) {
											return 35;
										} else if (dice.contains(6)) {
											return 40;
										}
									}
									
									return 0;
								});
						
						addCell(choice, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									final Map<Die, Integer> countMap = new HashMap<>();
									
									dice.stream().forEach(die ->
											countMap.put(die, 
													countMap.getOrDefault(die, 0) + 1));
									
									if (countMap.values()
											.containsAll(Arrays.asList(2, 3))) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 30;
									} else {
										return 0;
									}
								});
						
						addCell(choice, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									Die d = null;
									int c = 1;
									for (Die die : dice) {
										if (d == null) {
											d = die;
										} else if (d.equals(die)) {
											++c;
										}
									}
									
									final Die dFin = d;
									
									if (c == 1 && dice.parallelStream()
									                  .skip(1)
									                  .distinct()
									                  .count() == 1) {
										return dice.parallelStream()
										           .filter(die -> !dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else if (c == 4) {
										return dice.parallelStream()
										           .filter(die -> dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else {
										return 0;
									}
								});
					
						addCell(choice, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									if (dice.parallelStream().distinct().count() == 1) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 50;
									} else {
										return 0;
									}
								});
						
					builder = builder.pop();
					
					choice.add(new JLabel("", JLabel.CENTER));
					choice.add(new JLabel("", JLabel.CENTER));
				
				columns.add(choice);
				builder = builder.pop();
				
				JPanel up = new JPanel(new GridLayout(0, 1));
				builder = builder.withGroup();
				
					up.add(new JLabel("Up", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						// Use in min-max section scoring
						Cell upAces = null;
						Unit<Cell> prevTup = new Unit<>();
						Unit<Cell> nextTup = new Unit<>();
						
						for (int i = 1; i <= 6; ++i) {
							final Unit<Cell> next = nextTup;
							final int iFin = i;
							prevTup.setValue(addRestrictedNumericCell(up, builder, i,
									turn -> {
										Objects.requireNonNull(next.getValue(),
												String.valueOf(iFin));
										return next.getValue().isFull();
									}));
							if (i == 1) {
								upAces = prevTup.getValue();
							}
							prevTup = nextTup;
							nextTup = new Unit<>();
						}
					
					builder = builder.pop();
					
					up.add(new JLabel("", JLabel.CENTER));
					
					final Cell upAcesFin = upAces;
					// (Max - Min) * Aces
					builder = builder.withGroup()
					                 .withCombiner(scores ->
					                		 (scores.get(0).getScore()
			                				 - scores.get(1).getScore())
					                		 * upAcesFin.getScore());
					
						final Unit<Cell> upMin = new Unit<>();
					
						prevTup.setValue(addRestrictedCell(up, builder,
								turn -> turn.getDice()
								            .parallelStream()
								            .mapToInt(Die::getValue)
								            .reduce((a, b) -> a + b)
								            .getAsInt(),
								
								turn -> upMin.getValue().isFull()));
						
						final Unit<Cell> upStraight = new Unit<>();
						
						upMin.setValue(addRestrictedCell(up, builder,
								turn -> turn.getDice()
								            .parallelStream()
								            .mapToInt(Die::getValue)
								            .reduce((a, b) -> a + b)
								            .getAsInt(),
								
								turn -> upStraight.getValue().isFull()));
						
					builder = addGroup(up, builder);
					
					up.add(new JLabel("", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						final Unit<Cell> upFull = new Unit<>();
						
						upStraight.setValue(addRestrictedCell(up, builder,
								turn -> {
									Collection<Integer> dice = turn.getDice()
									                               .parallelStream()
									                               .map(Die::getValue)
									                               .collect(
									                            	   Collectors.toList()
									                               );
									
									if (dice.containsAll(Arrays.asList(2, 3, 4, 5))) {
										if (dice.contains(1)) {
											return 35;
										} else if (dice.contains(6)) {
											return 40;
										}
									}
									
									return 0;
								},
								
								turn -> upFull.getValue().isFull()));
						
						final Unit<Cell> upPoker = new Unit<>();
						
						upFull.setValue(addRestrictedCell(up, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									final Map<Die, Integer> countMap = new HashMap<>();
									
									dice.stream().forEach(die ->
											countMap.put(die, 
													countMap.getOrDefault(die, 0) + 1));
									
									if (countMap.values()
											.containsAll(Arrays.asList(2, 3))) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 30;
									} else {
										return 0;
									}
								},
								
								turn -> upPoker.getValue().isFull()));
						
						final Unit<Cell> upYamb = new Unit<>();
						
						upPoker.setValue(addRestrictedCell(up, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									Die d = null;
									int c = 1;
									for (Die die : dice) {
										if (d == null) {
											d = die;
										} else if (d.equals(die)) {
											++c;
										}
									}
									
									final Die dFin = d;
									
									if (c == 1 && dice.parallelStream()
									                  .skip(1)
									                  .distinct()
									                  .count() == 1) {
										return dice.parallelStream()
										           .filter(die -> !dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else if (c == 4) {
										return dice.parallelStream()
										           .filter(die -> dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else {
										return 0;
									}
								},
								
								turn -> upYamb.getValue().isFull()));
					
						upYamb.setValue(addCell(up, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									if (dice.parallelStream().distinct().count() == 1) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 50;
									} else {
										return 0;
									}
								}));
						
					builder = builder.pop();
					
					up.add(new JLabel("", JLabel.CENTER));
					up.add(new JLabel("", JLabel.CENTER));
					
				columns.add(up);
				builder = builder.pop();
				
				JPanel call = new JPanel(new GridLayout(0, 1));
				builder = builder.withGroup();
				
					call.add(new JLabel("Call", JLabel.CENTER));
					
					builder = builder.withGroup();
					
						// Use in min-max section scoring
						Cell callAces = addRestrictedNumericCell(call, builder, 1,
								turn -> turn.getRolls() == 1);
						
						for (int i = 2; i <= 6; ++i) {
							addRestrictedNumericCell(call, builder, i,
									turn -> turn.getRolls() == 1);
						}
					
					builder = builder.pop();
					
					call.add(new JLabel("", JLabel.CENTER));
					
					// (Max - Min) * Aces
					builder = builder.withGroup()
					                 .withCombiner(scores -> 
					            		     (scores.get(0).getScore()
					            		     - scores.get(1).getScore())
					            		     * callAces.getScore());
					
						addRestrictedCell(call, builder, turn -> turn.getDice()
								.parallelStream()
								.mapToInt(Die::getValue)
								.reduce((a, b) -> a + b)
								.getAsInt(),
								
								turn -> turn.getRolls() == 1);
						
						addRestrictedCell(call, builder, turn -> turn.getDice()
								.parallelStream()
								.mapToInt(Die::getValue)
								.reduce((a, b) -> a + b)
								.getAsInt(),
								
								turn -> turn.getRolls() == 1);
						
					builder = addGroup(call, builder);
					
					call.add(new JLabel("", JLabel.CENTER));
					
					builder = builder.withGroup();
						
						addRestrictedCell(call, builder,
								turn -> {
									Collection<Integer> dice = turn.getDice()
									                               .parallelStream()
									                               .map(Die::getValue)
									                               .collect(
									                            	   Collectors.toList()
									                               );
									
									if (dice.containsAll(Arrays.asList(2, 3, 4, 5))) {
										if (dice.contains(1)) {
											return 35;
										} else if (dice.contains(6)) {
											return 40;
										}
									}
									
									return 0;
								},
								
								turn -> turn.getRolls() == 1);
						
						addRestrictedCell(call, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									final Map<Die, Integer> countMap = new HashMap<>();
									
									dice.stream().forEach(die ->
											countMap.put(die, 
													countMap.getOrDefault(die, 0) + 1));
									
									if (countMap.values()
											.containsAll(Arrays.asList(2, 3))) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 30;
									} else {
										return 0;
									}
								},
								
								turn -> turn.getRolls() == 1);
						
						addRestrictedCell(call, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									Die d = null;
									int c = 1;
									for (Die die : dice) {
										if (d == null) {
											d = die;
										} else if (d.equals(die)) {
											++c;
										}
									}
									
									final Die dFin = d;
									
									if (c == 1 && dice.parallelStream()
									                  .skip(1)
									                  .distinct()
									                  .count() == 1) {
										return dice.parallelStream()
										           .filter(die -> !dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else if (c == 4) {
										return dice.parallelStream()
										           .filter(die -> dFin.equals(die))
										           .collect(Collectors
										        		   .summingInt(Die::getValue)) + 40;
									} else {
										return 0;
									}
								},
								
								turn -> turn.getRolls() == 1);
					
						addRestrictedCell(call, builder,
								turn -> {
									final Collection<Die> dice = turn.getDice();
									
									if (dice.parallelStream().distinct().count() == 1) {
										return dice.parallelStream().collect(Collectors
												.summingInt(Die::getValue)) + 50;
									} else {
										return 0;
									}
								},
								
								turn -> turn.getRolls() == 1);
						
					builder = builder.pop();
					
					call.add(new JLabel("", JLabel.CENTER));
					call.add(new JLabel("", JLabel.CENTER));
				
				columns.add(call);
				builder = builder.pop();
				
			pane.add(columns);
			
		pane.add(columns);
		
		add(pane);
		
		scoresheet = builder.end();
		
		JTextField total = new JTextField();
		total.setHorizontalAlignment(JTextField.CENTER);
		total.setEnabled(false);
		down.add(total);
		subtotalMap.put(total, scoresheet);
		
		pack();
	}
	
	private final Map<JButton, ActionListener> listeners = new HashMap<>();
	private void clearListeners() {
		listeners.entrySet().parallelStream().forEach(entry ->
				entry.getKey().removeActionListener(entry.getValue()));
	}
	
	public void updateTurn(Turn turn) {
		clearListeners();
		
		cellMap.entrySet().parallelStream().forEach(entry -> {
			JButton button = entry.getKey();
			Cell    cell   = entry.getValue();
			
			button.setEnabled(cell.isEnabled(turn));
			if (button.isEnabled()) {
				ActionListener listener = e -> {
					cell.enterScore(turn);
					clearListeners();
					cellMap.keySet().parallelStream().forEach(b -> b.setEnabled(false));
					turn.end();
				};
				button.addActionListener(listener);
				listeners.put(button, listener);
			}
			
			button.setText(String.valueOf(cell.isFull()
					? cell.getEnteredScore()
					: (button.isEnabled() ? cell.getScorer().score(turn) : "")));
		});
		
		subtotalMap.entrySet()
		           .parallelStream()
		           .forEach(entry ->
		        		   entry.getKey().setText(
		        				   String.valueOf(entry.getValue().getScore())));
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
	}

	@Override
	public int getScore() {
		return scoresheet.getScore();
	}

}
