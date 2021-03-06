package editor;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import editor.ConfirmWindow.Action;
import util.IntRect;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import java.awt.Component;

public class Editor {
	private JFrame frame;
	private JTextField yField;
	private JTextField xField;
	private JTextField wField;
	private JTextField hField;
	private JTextField intervalField;
	private JTextField frameField;

	private JButton btnNewButton, saveButton;
	private JButton btnPlay, btnStop;
	private JButton newButton, renameButton, deleteButton;

	private AnimationPreviewer animationPreviewer;
	private SpriteSheetPreviewer imagePreviewer;
	private JCheckBox l00pBox, sameDimensionBox;
	private JList animations;

	private int width = 1250;
	private int height = 1000;

	private SpriteSheet spriteSheet;
	private AnimationList animationList;
	private Animation currentAnimation;

	private Color backgroundColor = Color.BLACK;

	private JSONFileGenerator fileSaver = new JSONFileGenerator();
	private File jsonFile;

	private ArrayList<Previewable> previewers = new ArrayList<Previewable>();

	public Editor() {
		initialize();
	}

	private void initialize() {
		// Initialize the editor window. Resizable = false for simplicity
		frame = new JFrame("Atlas Editor");
		frame.setSize(1250, 1000);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// ------------------------------------------------

		// Initialize the container that holds everything in the editor
		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
		frame.getContentPane().add(container);

		// ------------------------------------------------
		// Initialize the panel to display the entire sprite sheet
		// TODO: make separate class and add zoom and move methods

		imagePreviewer = new SpriteSheetPreviewer();
		imagePreviewer.setBackground(Color.BLACK);
		imagePreviewer.setAlignmentX(Component.LEFT_ALIGNMENT);
		imagePreviewer.setPreferredSize(new Dimension((int) (width * 0.8), height));
		container.add(imagePreviewer);
		previewers.add(imagePreviewer);
		imagePreviewer.setLayout(new BorderLayout());

		// ------------------------------------------------
		// Initialize the panel to hold 'load' and 'save' buttons on top

		JPanel panel_1 = new JPanel();
		imagePreviewer.add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel_1.setBackground(Color.BLACK);

		// Load image button opens PNG files only
		btnNewButton = new JButton("Load Image");
		btnNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(new File("."));

				FileNameExtensionFilter filter = new FileNameExtensionFilter(".png Files", "png");
				fileChooser.setFileFilter(filter);

				int returnValue = fileChooser.showOpenDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					String fileName = fileChooser.getSelectedFile().getName();
					String atlasName = fileName.substring(0, fileName.indexOf("."));
					String pathName = fileChooser.getSelectedFile().getPath();

					File atlasFile = new File(pathName.substring(0, pathName.indexOf(".")) + ".json");

					if (atlasFile.exists()) {
						spriteSheet = new SpriteSheet("resources/images/" + atlasName);

						animationList = new AnimationList(spriteSheet.getAnimations());

						fileSaver.setName(atlasName);
					} else {
						try {
							BufferedImage image = ImageIO.read(new File(pathName));
							spriteSheet = new SpriteSheet(image);

							animationList = new AnimationList();
							fileSaver.setName(atlasName);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}

					for (Previewable p : previewers) {
						if (p.getRenderer() == null)
							p.initRenderer();
					}

					imagePreviewer.setAnimationList(animationList);
					imagePreviewer.displayImage(spriteSheet);
					// Set animation list to the animations in the JSON atlas file
					animations.setModel(animationList);
				}
			}
		});
		panel_1.add(btnNewButton);

		saveButton = new JButton("Save File");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (animationList.getSize() > 0) {
					setData();

					fileSaver.setAnimationList(animationList);
					fileSaver.generate();
					// fileSaver.celebrate(); heh it rhymes
				}
			}
		});
		panel_1.add(saveButton);

		// ------------------------------------------------
		// Initialize the panel to hold editable info on the image's atlas

		JPanel dataContainer = new JPanel();
		// frame.getContentPane().add(panel, BorderLayout.CENTER);
		dataContainer.setLayout(new BoxLayout(dataContainer, BoxLayout.Y_AXIS));
		dataContainer.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

		// Top-right panel that previews a selected animation
		animationPreviewer = new AnimationPreviewer();
		animationPreviewer.setBackground(backgroundColor);
		dataContainer.add(animationPreviewer);
		previewers.add(animationPreviewer);

		// ------------------------------------------------
		// Panel that holds the 'play' button

		JPanel playPanel = new JPanel();
		FlowLayout fl_playPanel = (FlowLayout) playPanel.getLayout();
		fl_playPanel.setAlignment(FlowLayout.LEFT);
		playPanel.setBackground(Color.BLACK);
		playPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		dataContainer.add(playPanel);

		// Start or resume the thread playing the animation loop

		btnPlay = new JButton("Play");
		btnPlay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentAnimation != null) {
					if (animationPreviewer.isPaused()) {
						animationPreviewer.resumeAnimation();
					} else {
						animationPreviewer.playAnimation();
					}
				}
			}
		});
		playPanel.add(btnPlay);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentAnimation != null)
					animationPreviewer.pauseAnimation();
			}
		});
		playPanel.add(btnStop);

		// ------------------------------------------------
		// Panel that holds the text field to specify number of frames if animation has

		JPanel framePanel = new JPanel();
		FlowLayout fl_framePanel = (FlowLayout) framePanel.getLayout();
		fl_framePanel.setAlignment(FlowLayout.LEFT);
		framePanel.setBackground(Color.BLACK);
		framePanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		dataContainer.add(framePanel);

		frameField = new JTextField();
		frameField.getDocument().addDocumentListener(addFieldListener());
		framePanel.add(frameField);
		frameField.setColumns(10);

		JLabel lblFrames = new JLabel("Frames");
		lblFrames.setForeground(Color.WHITE);
		framePanel.add(lblFrames);

		// ------------------------------------------------

		l00pBox = new JCheckBox("Loop");
		framePanel.add(l00pBox);

		// ------------------------------------------------
		// Panel that holds the text field to specify the interval of this animation if
		// it has

		JPanel intervalPanel = new JPanel();
		dataContainer.add(intervalPanel);
		intervalPanel.setBackground(Color.BLACK);
		intervalPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		intervalField = new JTextField();
		intervalField.getDocument().addDocumentListener(addFieldListener());
		intervalPanel.add(intervalField);
		intervalField.setColumns(10);

		JLabel lblInterval = new JLabel("Interval");
		lblInterval.setForeground(Color.WHITE);
		intervalPanel.add(lblInterval);

		// ------------------------------------------------

		sameDimensionBox = new JCheckBox("= dims");
		intervalPanel.add(sameDimensionBox);

		JPanel dimensionPanel = new JPanel();
		dimensionPanel.setBackground(Color.BLACK);
		dataContainer.add(dimensionPanel);
		dimensionPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		dimensionPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

		// ------------------------------------------------
		// x, y, width, and height of the animation

		xField = new JTextField("x");
		xField.getDocument().addDocumentListener(addFieldListener());
		dimensionPanel.add(xField);
		xField.setColumns(3);

		yField = new JTextField("y");
		yField.getDocument().addDocumentListener(addFieldListener());
		dimensionPanel.add(yField);
		yField.setColumns(3);

		wField = new JTextField("w");
		wField.getDocument().addDocumentListener(addFieldListener());
		dimensionPanel.add(wField);
		wField.setColumns(3);

		hField = new JTextField("h");
		hField.getDocument().addDocumentListener(addFieldListener());
		dimensionPanel.add(hField);
		hField.setColumns(3);

		// ------------------------------------------------

		JPanel animationPanel = new JPanel();
		animationPanel.setBackground(Color.RED);
		dataContainer.add(animationPanel);
		animationPanel.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		animationPanel.add(scrollPane, BorderLayout.CENTER);

		// ------------------------------------------------
		// Lists all the animations from the JSON atlas file

		animations = new JList();
		animations.setForeground(Color.WHITE);
		animations.setBackground(Color.BLACK);
		scrollPane.setViewportView(animations);

		MouseListener LISTener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				currentAnimation = (Animation) animations.getSelectedValue();

				if (currentAnimation != null) {
					xField.setText(currentAnimation.getFrame().x + ""); // lol
					yField.setText(currentAnimation.getFrame().y + "");
					wField.setText(currentAnimation.getFrame().w + "");
					hField.setText(currentAnimation.getFrame().h + "");

					frameField.setText(currentAnimation.getFrameCount() + "");
					intervalField.setText(currentAnimation.getInterval() + "");

					l00pBox.setSelected(currentAnimation.isLoop());

					if (!animationPreviewer.isPaused() && animationPreviewer.isRunning())
						animationPreviewer.pauseAnimation();

					animationPreviewer.displayAnimation(spriteSheet, currentAnimation);
				}
			}
		};
		animations.addMouseListener(LISTener);

		// ------------------------------------------------

		container.add(dataContainer);

		// ------------------------------------------------

		JPanel filePanel = new JPanel();
		dataContainer.add(filePanel);
		filePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		newButton = new JButton("New");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfirmWindow confirmWindow = new ConfirmWindow(Action.NEW_ANIMATION);
				confirmWindow.setAnimationList(animationList);
			}
		});
		filePanel.add(newButton);

		renameButton = new JButton("Rename");
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentAnimation != null) {
					ConfirmWindow confirmWindow = new ConfirmWindow(Action.RENAME_ANIMATION);

					confirmWindow.setAnimationList(animationList);
					confirmWindow.setRenamableAnimation(currentAnimation);
				}
			}
		});
		filePanel.add(renameButton);

		deleteButton = new JButton("Delete");
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentAnimation != null)
					animationList.removeAnimation(currentAnimation);
			}
		});
		filePanel.add(deleteButton);

		// ------------------------------------------------
	}

	public DocumentListener addFieldListener() {
		return new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent de) {
				updatePreviewer();
			}

			@Override
			public void insertUpdate(DocumentEvent de) {
				updatePreviewer();
			}

			@Override
			public void removeUpdate(DocumentEvent de) {
				updatePreviewer();
			}

			public void updatePreviewer() {
				imagePreviewer.setAnimationList(animationList);
				imagePreviewer.displayImage(spriteSheet);
			}
		};
	}

	public void setData() {
		if (currentAnimation != null) {
			xField.getText().trim();
			yField.getText().trim();
			hField.getText().trim();
			wField.getText().trim();

			currentAnimation.setFrame(new IntRect(Integer.parseInt(xField.getText()),
					/* WHY JAVA WHY */ Integer.parseInt(yField.getText()), Integer.parseInt(wField.getText()),
					Integer.parseInt(hField.getText())));
			currentAnimation.setFrameCount(Integer.parseInt(frameField.getText()));
			currentAnimation.setInterval(Float.parseFloat(intervalField.getText()));
			currentAnimation.setLoop(l00pBox.isSelected());
		}
	}

	public JFrame getFrame() {
		return frame;
	}
}
