package net.danilovic.dimitriye.yamb.gui;

import java.awt.GridLayout;
import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.danilovic.dimitriye.util.Unit;
import net.danilovic.dimitriye.yamb.logic.Die;

public class DiePanel extends JPanel {

	private static final long serialVersionUID = -417268702308515163L;
	private final Unit<Die> die;
	private final JLabel dieLabel;
	private final JCheckBox keeperBox;
	private boolean keeping;
	
	private static ImageIcon imageGet(Die die) {
		try {
			return new ImageIcon(ImageIO.read(DiePanel.class.getResource("/net/danilovic/dimitriye/yamb/images/die" + die.getValue() + ".png")).getScaledInstance(64, 64, Image.SCALE_DEFAULT));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	};
	
	public void setDie(Die die) {
		this.die.setValue(die);
		dieLabel.setIcon(imageGet(die));
	}
	
	public Unit<Die> getDieReference() {
		return die;
	}
	
	public boolean getKeeping() {
		return keeping;
	}
	
	public void setKeeping(boolean keeping) {
		this.keeping = keeping;
		keeperBox.setSelected(keeping);
	}
	
	public boolean reroll() {
		if (!keeping) {
			setDie(new Die());
			return true;
		} else return false;
	}

	public DiePanel(Unit<Die> reference) {
		super();
		
		die = reference;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JPanel dieWrapper = new JPanel(new GridLayout(1, 1));
		
			dieLabel = new JLabel(new ImageIcon(), JLabel.CENTER);
			dieLabel.setAlignmentX(CENTER_ALIGNMENT);
			dieWrapper.add(dieLabel);
		
		add(dieWrapper);
		
		setDie(new Die());
		
		keeperBox = new JCheckBox("Keep?");
		keeperBox.setAlignmentX(CENTER_ALIGNMENT);
		keeperBox.addItemListener(e -> keeping = (e.getStateChange() == 1) ? true : false);
		
		add(keeperBox);
	}
	
	public DiePanel() {
		this(new Unit<Die>());
	}

}
