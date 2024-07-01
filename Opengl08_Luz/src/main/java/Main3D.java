
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import Model.VboBilboard;
import Model.VboCube;
import dados.Constantes;
import obj.Cubo3D;
import obj.Mapa3D;
import obj.ObjHTGsrtm;
import obj.ObjModel;
import obj.Object3D;
import obj.Player;
import obj.Enemy;
import obj.Projetil;
import shaders.StaticShader;
import util.TextureLoader;
import util.Utils3D;
import java.awt.image.BufferedImage;


import java.nio.*;
import java.util.ArrayList;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main3D {

	// The window handle
	private long window;
	
	float viewAngX = 0;
	float viewAngY = 0;
	float scale = 1.0f;
	
	public Random rnd = new Random();
	
	VboCube vboc;
	VboBilboard vboBilbord;
	StaticShader shader;
	ArrayList<Object3D> listaObjetos = new ArrayList<>();
	
	
	Vector4f cameraPos = new  Vector4f(0.0f,10.0f, 0.0f,1.0f);
	Vector4f cameraVectorFront = new Vector4f(0.0f, 0.0f, -1.0f,1.0f);
	Vector4f cameraVectorUP = new Vector4f(0.0f, 1.0f, 0.0f,1.0f);
	Vector4f cameraVectorRight = new Vector4f(1.0f, 0.0f, 0.0f,1.0f);
	
	Matrix4f view = new Matrix4f();
	
	boolean UP = false;
	boolean DOWN = false;
	boolean LEFT = false;
	boolean RIGHT = false;
	
	boolean FORWARD = false;
	boolean BACKWARD = false;
	
	boolean QBu = false;
	boolean EBu = false;

	boolean FIRE = false;
	
	boolean enemiesCreated = false;
	
	Matrix4f cameraMatrix = new Matrix4f();
	

	FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
	
	Cubo3D umcubo;
	Player m29;
	
	double angluz = 0;

    private float mouseX, mouseY;
    private boolean mouseLeftPressed = false;
    private boolean mouseRightPressed = false;
    private float playerVelocity = 0.0f;
    private float maxVelocity = 20.0f;
    private float acceleration = 10.0f;
    private float deceleration = 5.0f;

    private float cameraOffsetY = -0.5f; // Slightly below center
    private float cameraDistance = 2.0f; // Distance behind the player




	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(1500, 1000, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.

		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			}
		});

		glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
			float dx = (float) xpos - mouseX;
			float dy = (float) ypos - mouseY;
			mouseX = (float) xpos;
			mouseY = (float) ypos;

			// Rotate camera based on mouse movement
			float sensitivity = 0.1f;
			viewAngY += dx * sensitivity;
			viewAngX += dy * sensitivity;

			// Limit vertical rotation
			viewAngX = Math.max(-90, Math.min(90, viewAngX));

			// Update camera vectors
			updateCameraVectors();
		});

		glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
			if (button == GLFW_MOUSE_BUTTON_LEFT) {
				mouseLeftPressed = (action == GLFW_PRESS);
			} else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
				mouseRightPressed = (action == GLFW_PRESS);
				FIRE = mouseRightPressed;
			}
		});

		// Hide the cursor and capture it
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);

	}
	

    private void updateCameraVectors() {
        float yaw = (float) Math.toRadians(viewAngY);
        float pitch = (float) Math.toRadians(viewAngX);

        cameraVectorFront.x = (float) (Math.cos(yaw) * Math.cos(pitch));
        cameraVectorFront.y = (float) Math.sin(pitch);
        cameraVectorFront.z = (float) (Math.sin(yaw) * Math.cos(pitch));
        Utils3D.vec3dNormilize(cameraVectorFront);

        cameraVectorRight = Utils3D.crossProduct(cameraVectorFront, new Vector4f(0, 1, 0, 0));
        Utils3D.vec3dNormilize(cameraVectorRight);

        cameraVectorUP = Utils3D.crossProduct(cameraVectorRight, cameraVectorFront);
        Utils3D.vec3dNormilize(cameraVectorUP);
    }
	
  private void createEnemies(ObjModel enemyModel) {
      for (int i = 0; i < 5; i++) {
          Enemy enemy = new Enemy(
              rnd.nextFloat() * 20 - 10,
              rnd.nextFloat() * 10 + 5,
              rnd.nextFloat() * 20 - 10,
              0.01f  // Increased size for visibility
          );
          enemy.model = enemyModel;
          listaObjetos.add(enemy);
      }
  }

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		// M3 - criar os inimigos
		GL.createCapabilities();

		view.setIdentity();
		
		vboc = new VboCube();
		vboc.load();
		vboBilbord = new VboBilboard();
		vboBilbord.load();
		shader = new StaticShader();
		
		//Cubo3D cubo = new Cubo3D(0.0f, 0.0f, -1.0f, 0.2f);
		//cubo.vbocube = vboc;
		
		//ObjModel x35 = new ObjModel();
		//x35.loadObj("x-35_obj.obj");
		//x35.load();
		
		System.out.println("------> Carrega MIG");
		ObjModel mig29 = new ObjModel();
		mig29.loadObj("Mig_29_obj.obj");
		mig29.load();
		
		if (!enemiesCreated) {
			createEnemies(mig29);
			enemiesCreated = true;
		}
		
		m29 = new Player(0, 0, 0, 0.01f);
		m29.model = mig29;
		
		ObjHTGsrtm model = new ObjHTGsrtm();
		model.load();
		
		Constantes.mapa = new Mapa3D(-10.0f, 0.0f, -10.0f, 10);
		Constantes.mapa.model = model;
		
		umcubo = new Cubo3D(0.0f, 0.0f, 0.8f, 0.1f);
		umcubo.model = vboc;

		
		BufferedImage imggato = TextureLoader.loadImage("texturaGato.jpeg");
		Constantes.tgato = TextureLoader.loadTexture(imggato);
		System.out.println("tgato "+Constantes.tgato);
		
		BufferedImage imgmulttexture = TextureLoader.loadImage("multtexture.png");
		Constantes.tmult = TextureLoader.loadTexture(imgmulttexture);
		System.out.println("tmult "+Constantes.tmult);
		
		
		BufferedImage texturamig = TextureLoader.loadImage("TexturaMig01.png");
		Constantes.txtmig = TextureLoader.loadTexture(texturamig);
		System.out.println("tmult "+Constantes.tmult);
		
		BufferedImage imgtexttiro = TextureLoader.loadImage("texturaTiro.png");
		Constantes.texturaTiro = TextureLoader.loadTexture(imgtexttiro);
		System.out.println("texturaTiro "+Constantes.texturaTiro);
		
		BufferedImage imgtextexp = TextureLoader.loadImage("texturaExplosao.png");
		Constantes.texturaExplosao = TextureLoader.loadTexture(imgtextexp);
		System.out.println("texturaExplosao "+Constantes.texturaExplosao);
		
		

		int frame = 0;
		long lasttime = System.currentTimeMillis();

		float angle = 0;

		
		long ultimoTempo = System.currentTimeMillis();
		while (!glfwWindowShouldClose(window)) {
			
			long diftime = System.currentTimeMillis()-ultimoTempo;
			ultimoTempo = System.currentTimeMillis();
			
			gameUpdate(diftime);
			gameRender();
			
			
			frame++;
			long actualTime = System.currentTimeMillis();
			if ((lasttime / 1000) != (actualTime / 1000)) {
				System.out.println("FPS " + frame);
				frame = 0;
				lasttime = actualTime;
			}

		}
	}

	long tirotimer = 0;

	private void gameUpdate(long diftime) {
		float dt = diftime / 1000.0f;
	
		// Update player velocity based on left mouse button
		if (mouseLeftPressed) {
			playerVelocity = Math.min(playerVelocity + acceleration * dt, maxVelocity);
		} else {
			playerVelocity = Math.max(playerVelocity - deceleration * dt, 0);
		}
	
		// Calculate player movement
		float dx = -cameraVectorFront.x * playerVelocity * dt;
		float dy = -cameraVectorFront.y * playerVelocity * dt;
		float dz = -cameraVectorFront.z * playerVelocity * dt;
	
		// Update player position
		m29.x += dx;
		m29.y += dy;
		m29.z += dz;
	
		// Update player rotation vectors
		m29.Front = new Vector4f(cameraVectorFront);
		m29.UP = new Vector4f(cameraVectorUP);
		m29.Right = new Vector4f(cameraVectorRight);
	
		// Update camera position relative to player
		cameraPos.x = m29.x - cameraVectorFront.x * cameraDistance;
		cameraPos.y = m29.y - cameraVectorFront.y * cameraDistance - cameraOffsetY;
		cameraPos.z = m29.z - cameraVectorFront.z * cameraDistance;
	
		// Handle shooting with right mouse button
		tirotimer += diftime;
		if (mouseRightPressed && tirotimer >= 100) {
			shootProjectile();
			tirotimer = 0;
		}
	
		// Update and remove projectiles
		updateProjectiles(dt);
	
		// Check for collisions with terrain
		Constantes.mapa.testaColisao(m29.x, m29.y, m29.z, 0.1f);
	
		// Update view matrix based on camera position
		Vector4f targetPos = new Vector4f(m29.x, m29.y, m29.z, 1.0f);
		Vector4f cameraToTarget = Utils3D.subtractVectors(targetPos, cameraPos);
		Utils3D.vec3dNormilize(cameraToTarget);
		view = Utils3D.setLookAtMatrix(cameraPos, cameraToTarget, cameraVectorUP, cameraVectorRight);
	}
	
	

    private void shootProjectile() {
		float velocidade_projetil = 14;
		Projetil pj = new Projetil(m29.x, m29.y, m29.z);
		pj.vx = -cameraVectorFront.x * velocidade_projetil;
		pj.vy = -cameraVectorFront.y * velocidade_projetil;
		pj.vz = -cameraVectorFront.z * velocidade_projetil;
		pj.raio = 0.2f;
		pj.model = vboBilbord;
		pj.setRotation(cameraVectorFront, cameraVectorUP, cameraVectorRight);
		listaObjetos.add(pj);
	}
	
	private void updateProjectiles(float dt) {
		ArrayList<Object3D> objectsToRemove = new ArrayList<>();
	
		for (Object3D obj : listaObjetos) {
			obj.SimulaSe((long)(dt * 1000));
	
			if (obj instanceof Projetil) {
				Projetil projetil = (Projetil) obj;
				
				// Remove projectile if it's too far away
				if (Vector3f.sub(new Vector3f(projetil.x, projetil.y, projetil.z), 
								 new Vector3f(cameraPos.x, cameraPos.y, cameraPos.z), null).lengthSquared() > 1000) {
					objectsToRemove.add(projetil);
					continue;
				}
	
				// Check collision with enemies
				for (Object3D target : listaObjetos) {
					if (target instanceof Enemy && checkCollision(projetil, target)) {
						objectsToRemove.add(projetil);
						objectsToRemove.add(target);
						break;
					}
				}
			}
		}
	
		listaObjetos.removeAll(objectsToRemove);
	}
	
	private boolean checkCollision(Object3D obj1, Object3D obj2) {
		float dx = obj1.x - obj2.x;
		float dy = obj1.y - obj2.y;
		float dz = obj1.z - obj2.z;
		float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		return distance < (obj1.raio + obj2.raio);
	}

	private void gameRender() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

		glEnable(GL_LIGHTING);
		glShadeModel(GL_SMOOTH);

		glLoadIdentity();

		shader.start();
		
		int projectionlocation = glGetUniformLocation(shader.programID, "projection");
		//Matrix4f projection = setFrustum(-1f,1f,-1f,1f,1f,100.0f);
		Matrix4f projection = Utils3D.setFrustum(-1.5f,1.5f,-1f,1f,1f,500.0f);
		projection.storeTranspose(matrixBuffer);
		matrixBuffer.flip();
		glUniformMatrix4fv(projectionlocation, false, matrixBuffer);
		
		int lightpos = glGetUniformLocation(shader.programID, "lightPosition");
		
		float yl = (float)(Math.cos(angluz)*50.0);
		float zl = (float)(Math.sin(angluz)*50.0);
		
		float vf[] = {0.0f,yl,zl,1.0f};
		glUniform4fv(lightpos, vf);
		
		glEnable(GL_DEPTH_TEST);
		
		glActiveTexture(GL_TEXTURE0);
		//glBindTexture(GL_TEXTURE_2D, tgato);
		glBindTexture(GL_TEXTURE_2D, Constantes.tmult);
		
		
		int loctexture = glGetUniformLocation(shader.programID, "tex");
		glUniform1i(loctexture, 0);
		
		int viewlocation = glGetUniformLocation(shader.programID, "view");
		view.storeTranspose(matrixBuffer);
		matrixBuffer.flip();
		glUniformMatrix4fv(viewlocation, false, matrixBuffer);
		
		Constantes.mapa.DesenhaSe(shader);
		umcubo.DesenhaSe(shader);		
		
    glBindTexture(GL_TEXTURE_2D, Constantes.txtmig);
    m29.DesenhaSe(shader);
    
    for(int i = 0; i < listaObjetos.size(); i++) {
        Object3D obj = listaObjetos.get(i);
        if (obj instanceof Enemy) {
            glBindTexture(GL_TEXTURE_2D, Constantes.txtmig);
        } else if (obj instanceof Projetil) {
            glBindTexture(GL_TEXTURE_2D, Constantes.texturaTiro);
        }
        obj.DesenhaSe(shader);
    }

		shader.stop();
		
		glfwSwapBuffers(window); // swap the color buffers

		// Poll for window events. The key callback above will only be
		// invoked during this call.
		glfwPollEvents();
	}


	public static void main(String[] args) {
		new Main3D().run();
	}

	public static void gluPerspective(float fovy, float aspect, float near, float far) {
		float bottom = -near * (float) Math.tan(fovy / 2);
		float top = -bottom;
		float left = aspect * bottom;
		float right = -left;
		glFrustum(left, right, bottom, top, near, far);
	}

}
