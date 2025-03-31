package pcd.ass01.view;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pcd.ass01.concurrent.thread.BoidsSimulator;
import pcd.ass01.model.BoidModel;

import java.awt.*;
import java.util.Hashtable;
import pcd.ass01.concurrent.Simulator;;

public class BoidsView implements ChangeListener {

	private JFrame frame;
	private BoidsPanel boidsPanel;
	private JSlider cohesionSlider, separationSlider, alignmentSlider;
	private BoidModel model;
	private int width, height;
	private Simulator simulator;
	private JTextField boidsCountField;
	
	public BoidsView(BoidModel model, Simulator simulator,int width, int height) {
		this.model = model;
		this.width = width;
		this.simulator = simulator;
		this.height = height;
		
		frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//pannello principale con BorderLayout
		JPanel cp = new JPanel();
		LayoutManager layout = new BorderLayout();
		cp.setLayout(layout);

		//pannello per la simulazione dei boids
        boidsPanel = new BoidsPanel(this, model);
		cp.add(BorderLayout.CENTER, boidsPanel);

		 // Pannello di controllo in alto: bottone per mettere in pausa/riprendere
		 JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		 JButton pauseButton = new JButton("Pause");
		 pauseButton.addActionListener(e -> {
			 simulator.togglePause();
			 pauseButton.setText(simulator.isPaused() ? "Resume" : "Pause");
		 });
		 controlPanel.add(pauseButton);


		 // Bottone Reset
		 JButton resetButton = new JButton("Reset");
		 resetButton.addActionListener(e -> {
			// Ferma la simulazione corrente
			simulator.stop();
			
			// Chiedi all'utente di inserire un nuovo numero di boids
			String input = JOptionPane.showInputDialog("Inserisci il numero di boids:");
			int nBoids = model.getBoids().size(); // valore predefinito: usa il numero attuale
			try {
				nBoids = Integer.parseInt(input);
			} catch(NumberFormatException ex) {
				// Se l'input non è valido, continua con il valore predefinito
				System.out.println("Numero non valido, utilizzo il valore attuale " + nBoids);
			}
			
			// Reinizializza il modello con il nuovo numero di boid
			// Nota: Dovresti aggiungere un metodo resetWithNewBoids in BoidModel
			model.resetWithNewBoids(nBoids);
			
			// Avvia la nuova simulazione
			simulator.start();
			
			// Aggiorna il campo che mostra il numero di boid
			boidsCountField.setText(String.valueOf(model.getBoids().size()));
		});
		 controlPanel.add(resetButton);

        // Aggiungiamo il campo di testo per il numero totale di boids
        JLabel boidsCountLabel = new JLabel("Total boids:");
        boidsCountField = new JTextField(String.valueOf(model.getBoids().size()), 5);
        boidsCountField.setEditable(false);
        controlPanel.add(boidsCountLabel);
        controlPanel.add(boidsCountField);

		cp.add(controlPanel, BorderLayout.NORTH);
 
        

        JPanel slidersPanel = new JPanel();
        
        cohesionSlider = makeSlider();
        separationSlider = makeSlider();
        alignmentSlider = makeSlider();
        
		

        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);
        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);
		        
		cp.add(BorderLayout.SOUTH, slidersPanel);

		frame.setContentPane(cp);	
		
        frame.setVisible(true);
	}

	private JSlider makeSlider() {
		var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);        
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		Hashtable labelTable = new Hashtable<>();
		labelTable.put( 0, new JLabel("0") );
		labelTable.put( 10, new JLabel("1") );
		labelTable.put( 20, new JLabel("2") );
		slider.setLabelTable( labelTable );
		slider.setPaintLabels(true);
        slider.addChangeListener(this);
		return slider;
	}
	
	public void update(int frameRate) {
		boidsPanel.setFrameRate(frameRate);
		boidsPanel.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == separationSlider) {
			var val = separationSlider.getValue();
			model.setSeparationWeight(0.1*val);
		} else if (e.getSource() == cohesionSlider) {
			var val = cohesionSlider.getValue();
			model.setCohesionWeight(0.1*val);
		} else {
			var val = alignmentSlider.getValue();
			model.setAlignmentWeight(0.1*val);
		}
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
