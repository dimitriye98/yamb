package net.danilovic.dimitriye.yamb;

import javax.swing.JFrame;

import net.danilovic.dimitriye.yamb.gui.DiceFrame;

public final class Main implements Runnable {
	
	private static Main main;
	
	public static Main getMain() {
		return main;
	}

	private Main() {}
	
	public void run() {
		DiceFrame window = new DiceFrame("Dice");
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		window.setVisible(true);
	}

	public static void main(String[] args) {
		main = new Main();
		main.run();
	}

}
