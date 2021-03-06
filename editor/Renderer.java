package editor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JComponent;
import javax.swing.JPanel;

import util.DoubleRect;
import util.IntRect;

public class Renderer {
	private BufferedImage frontBuffer, backBuffer;
	private Graphics2D graphics;
	public JPanel panel;

	private int width, height;
	private float scale = 1.0f;

	// Causes stuff to not flicker
	private Lock frameSwapLock = new ReentrantLock();

	private Stack<AffineTransform> transformationStack = new Stack<AffineTransform>();

	/*
	 * Creates a renderer for displaying all entities. Add to every GameState
	 */
	public Renderer() {
		// Get the dimensions of the screen
		width = Toolkit.getDefaultToolkit().getScreenSize().width;
		height = Toolkit.getDefaultToolkit().getScreenSize().height;

		// Calculate the scale according to the screen size
		scale = (width * scale) / 800;

		// Create the buffers to draw stuff on
		frontBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		graphics = backBuffer.createGraphics();

		initialize();
	}

	/*
	 * Creates a renderer that can be attached to any size JPanel. Used in the atlas
	 * editor
	 */
	public Renderer(JPanel p_panel) {
		// Get dimensions of the panel
		width = p_panel.getWidth();
		height = p_panel.getHeight();

		// Create the buffers to draw stuff on
		frontBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		graphics = backBuffer.createGraphics();

		initialize();
	}

	/**
	 * Creates the JPanel canvas. Called in constructor only.
	 */
	private void initialize() {
		// Create the JPanel to hold the canvas
		panel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				frameSwapLock.lock();
				try {
					super.paintComponent(g);
					g.drawImage(frontBuffer, 0, 0, this);
					repaint();
				} finally {
					frameSwapLock.unlock();
				}

			}
		};

		panel.setFocusable(true);

		// Initialize the transformation stack with the identity matrix
		transformationStack.push(new AffineTransform());
	}

	/**
	 * Switches the buffers. Ensures the current frame is being displayed
	 */
	public void display() {
		frameSwapLock.lock();
		try {
			BufferedImage temp = backBuffer;
			backBuffer = frontBuffer;
			frontBuffer = temp;

			graphics = backBuffer.createGraphics();
		} finally {
			frameSwapLock.unlock();
		}
	}

	/**
	 * Clears the screen before anything is drawn every frame
	 */
	public void clear() {
		addScreenOverlay(Color.BLACK, 1.0f);
	}

	/**
	 * Adds a new matrix transformation to the transformation stack
	 * 
	 * @param p_transform the transform to be multiplied by the previous one
	 */
	public void pushTransform(AffineTransform p_transform) {
		if (transformationStack.isEmpty()) {
			transformationStack.push(new AffineTransform(p_transform));
		} else {
			// Multiply the previous transformation by the newest one
			AffineTransform prevTransform = new AffineTransform(transformationStack.peek());
			prevTransform.concatenate(p_transform);
			transformationStack.push(prevTransform);
		}

		graphics.setTransform(transformationStack.peek());
	}

	/**
	 * Reverts back to a previous coordinate system (matrix). Must be called
	 * sometime after every pushTransform() call
	 */
	public void popTransform() {
		if (transformationStack.size() > 1)
			transformationStack.pop();

		graphics.setTransform(transformationStack.peek());
	}

	/*
	 * Set scale that only the size of images are affected by
	 */
	public void setScale(float p_scale) {
		scale = p_scale;
	}

	public float getScale() {
		return scale;
	}

	/**
	 * Draws a sprite
	 * 
	 * @param p_sheet the sprite sheet of the image
	 * @param p_frame the current frame to draw
	 */
	public void drawSprite(SpriteSheet p_sheet, IntRect p_frame) {
		// reset opacity
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));

		// scale image according to the current scale
		AffineTransform scaleTransform = new AffineTransform();
		scaleTransform.scale(scale, scale);

		pushTransform(scaleTransform);

		graphics.drawImage(p_sheet.getImage(), 0, 0, p_frame.w, p_frame.h, p_frame.x, p_frame.y, p_frame.x + p_frame.w,
				p_frame.y + p_frame.h, panel);

		popTransform();
	}

	public void drawSprite(SpriteSheet p_sheet, IntRect p_frame, IntRect p_destination) {
		// reset opacity
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));

		// scale image according to the current scale
		AffineTransform scaleTransform = new AffineTransform();
		scaleTransform.scale(scale, scale);

		pushTransform(scaleTransform);

		graphics.drawImage(p_sheet.getImage(), p_destination.x, p_destination.y, p_destination.x + p_frame.w,
				p_destination.y + p_frame.h, p_frame.x, p_frame.y, p_frame.x + p_frame.w, p_frame.y + p_frame.h, panel);

		popTransform();
	}

	/**
	 * Draws a solid rectangle
	 * 
	 * @param p_rect    the dimensions of the rectangle
	 * @param p_color   the color of the rectangle (Color.RED, Color.BLUE, etc)
	 * @param p_opacity 0.0f transparent to 1.0f (opaque)
	 */
	public void drawRect(DoubleRect p_rect, Color p_color, float p_opacity) {
		graphics.setColor(p_color);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p_opacity));
		graphics.draw(new Rectangle2D.Double(p_rect.x, p_rect.y, p_rect.w, p_rect.h));
	}

	/**
	 * Draws a rectangle border
	 * 
	 * @param p_rect      the dimensions of the rectangle
	 * @param p_color     the color of the rectangle (Color.RED, Color.BLUE, etc.)
	 * @param p_opacity   0.0f transparent to 1.0f (opaque)
	 * @param p_thickness THICCness in pixels
	 */
	public void drawRectBorder(DoubleRect p_rect, Color p_color, float p_opacity, float p_thickness) {
		graphics.setColor(p_color);
		graphics.setStroke(new BasicStroke(p_thickness));
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p_opacity));
		graphics.draw(new Rectangle2D.Double(p_rect.x, p_rect.y, p_rect.w, p_rect.h));
	}

	/**
	 * Cover the canvas with a color
	 * 
	 * @param p_color   The color to overlay the screen
	 * @param p_opacity 0.0f transparent to 1.0f (opaque)
	 */
	public void addScreenOverlay(Color p_color, float p_opacity) {
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p_opacity));

		graphics.setColor(p_color);
		graphics.fillRect(0, 0, width, height);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public JPanel getComponent() {
		return panel;
	}
}
