import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import org.joml.Matrix4f;

import components.GraphicsComponent;
import components.TransformComponent;
import graphics.Renderer;
import graphics.Texture;
import graphics.Window;
import graphics.graphicsUtil.Framebuffer;
 
public class Main 
{
	public static void main(String[] args) 
    {
        new Main();
    }
     
    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    
    private Window window;
    private Texture tex1;
    private Texture tex2;
    private Framebuffer fbo;
     
    public Main() 
    {
        // Initialize OpenGL and GLFW
    	window = new Window();
        window.init(WIDTH, HEIGHT, "Lanorian Roguelite");
        
        tex1 = new Texture("resources/images/narry");
        tex2 = new Texture("resources/images/birboi");
        
        fbo = new Framebuffer(window.getWidth(), window.getHeight());
        fbo.setWindow(window);
        
        Renderer renderer = new Renderer();
        renderer.setFramebuffer(fbo);
           
        GraphicsComponent gc1 = new GraphicsComponent(tex1, "default");
        GraphicsComponent gc2 = new GraphicsComponent(tex2, "idle");
        
        TransformComponent t1 = new TransformComponent();
        t1.setPosition(100, 50);
        t1.setScale(2.0f, 2.0f);
        t1.updateLocal();
        t1.updateParent();
        
        TransformComponent t2 = new TransformComponent();
        t2.setPosition(10, 10);
        t2.updateLocal();
        t1.addChild(t2);
        
        gc1.setTransformComponent(t1);
        gc2.setTransformComponent(t2);
             
        // Game loop
        while (!window.closing()) 
        {
        	// Get input
            glfwPollEvents();

            t1.render(new TransformComponent());
            
            gc1.render(renderer);
            gc2.render(renderer);
            
            renderer.render();
            
            window.display();
            window.printFPS();
             
        }
        this.destroyOpenGL();
    }
     
    private void destroyOpenGL()
    {  
        tex1.delete();
        fbo.delete();
        glfwTerminate();
    }
}