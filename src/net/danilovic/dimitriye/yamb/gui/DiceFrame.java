package net.danilovic.dimitriye.yamb.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.danilovic.dimitriye.yamb.logic.Turn;

public class DiceFrame extends JFrame {

	private static final long serialVersionUID = 5238574127806039802L;
	
	private final List<DiePanel> diePanels;
	
	private final YambScorecard scorecard;

	private final JButton rollButton;
	
	private Turn turn;

	public DiceFrame(String name) {
		super(name);
		
		this.scorecard = new YambScorecard("Scoresheet");
		scorecard.setDefaultCloseOperation(HIDE_ON_CLOSE);
		
		diePanels = new ArrayList<>();
		
		final int nDice = 5;
		for (int i = 0; i < nDice; ++i) {
			DiePanel pane = new DiePanel();
			diePanels.add(pane);
		}
		
		JPanel pane = new JPanel(new GridBagLayout());
		
		add(pane);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.insets = new Insets(0, 4, 0, 4);
		
		Iterator<DiePanel> iter = diePanels.iterator();
		while (iter.hasNext()) {
			DiePanel d = iter.next();
			if (!iter.hasNext()) {
				gbc.gridwidth = GridBagConstraints.REMAINDER;
			}
			pane.add(d, gbc);
		}
		
		gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		JPanel buttonPane = new JPanel(new GridLayout(1, 0));
		
			rollButton = new JButton("Roll");
			
				rollButton.addActionListener(e -> reroll());
				
			buttonPane.add(rollButton);
			
			JButton toggleScoresheet = new JButton("View Scoresheet");
			
				toggleScoresheet.addActionListener(e ->
						setScoresheetVisible(!scorecard.isVisible()));
				
			buttonPane.add(toggleScoresheet);
		
		pane.add(buttonPane, gbc);
		
		pack();
		
		nextTurn();
	}
	
	public void setScoresheetVisible(boolean visible) {
		scorecard.updateTurn(turn);
		scorecard.setVisible(visible);
	}
	
	public void reroll() {
		diePanels.parallelStream().forEach(d -> d.reroll());
		turn.setDice(diePanels.parallelStream()
		                      .map(panel -> panel.getDieReference().getValue())
		                      .collect(Collectors.toList()));
		turn.setRolls(turn.getRolls() + 1);
		if (scorecard.isVisible()) {
			scorecard.updateTurn(turn);
		}
		rollButton.setEnabled(turn.getRolls() < 3);
	}
	
	public void nextTurn() {
		turn = new Turn();
		turn.addTurnEndListener(t -> nextTurn());
		diePanels.parallelStream().forEach(panel -> panel.setKeeping(false));
		reroll();
	}

}
