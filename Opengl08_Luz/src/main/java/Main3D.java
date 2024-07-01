
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
		float vel = 5.0f;

		tirotimer += diftime;

		//angluz+=(Math.PI/4)*diftime/1000.0f;
		angluz = 0;

		float acceleration = 10.0f;
		if (mouseLeftPressed) {
			cameraPos.x -= cameraVectorFront.x * acceleration * diftime / 1000.0f;
			cameraPos.y -= cameraVectorFront.y * acceleration * diftime / 1000.0f;
			cameraPos.z -= cameraVectorFront.z * acceleration * diftime / 1000.0f;
		}

		Matrix4f rotTmp = new Matrix4f();
		rotTmp.setIdentity();
		if (RIGHT) {
			rotTmp.rotate(-1.0f * diftime / 1000.0f,
					new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if (LEFT) {
			rotTmp.rotate(1.0f * diftime / 1000.0f, new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if (UP) {
			rotTmp.rotate(-1.0f * diftime / 1000.0f,
					new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}
		if (DOWN) {
			rotTmp.rotate(1.0f * diftime / 1000.0f,
					new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}
		if (QBu) {
			rotTmp.rotate(-1.0f * diftime / 1000.0f,
					new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}
		if (EBu) {
			rotTmp.rotate(1.0f * diftime / 1000.0f,
					new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}
  
		rotTmp.transform(rotTmp, cameraVectorFront, cameraVectorFront);
		rotTmp.transform(rotTmp, cameraVectorRight, cameraVectorRight);
		rotTmp.transform(rotTmp, cameraVectorUP, cameraVectorUP);

		Utils3D.vec3dNormilize(cameraVectorFront);
		Utils3D.vec3dNormilize(cameraVectorRight);
		Utils3D.vec3dNormilize(cameraVectorUP);
    
		Vector4f t = new Vector4f(cameraPos.dot(cameraPos, cameraVectorRight), cameraPos.dot(cameraPos, cameraVectorUP),
		cameraPos.dot(cameraPos, cameraVectorFront), 1.0f);

		view = Utils3D.setLookAtMatrix(t, cameraVectorFront, cameraVectorUP, cameraVectorRight);

		Matrix4f transf = new Matrix4f();
		transf.setIdentity();
		transf.translate(new Vector3f(1, 1, 0));
		view.mul(transf, view, view);

		m29.raio = 0.01f;
		m29.Front = cameraVectorFront;
		m29.UP = cameraVectorUP;
		m29.Right = cameraVectorRight;
		m29.x = cameraPos.x - cameraVectorFront.x * 2;
		m29.y = cameraPos.y - cameraVectorFront.y * 2;
		m29.z = cameraPos.z - cameraVectorFront.z * 2;

		Vector4f cameraOffset = new Vector4f(cameraVectorFront.x * 10, cameraVectorFront.y * 10, cameraVectorFront.z * 10, 0.0f); // Distance behind the player
		cameraPos.x = m29.x - cameraOffset.x;
		cameraPos.y = m29.y - cameraOffset.y;
		cameraPos.z = m29.z - cameraOffset.z;

		// Update the LookAt matrix to keep the player in the center of the view
		Vector4f cameraTarget = new Vector4f(m29.x, m29.y, m29.z, 1.0f);
		view = Utils3D.setLookAtMatrix(cameraTarget, cameraVectorFront, cameraVectorUP, cameraVectorRight);


		Constantes.mapa.testaColisao(m29.x, m29.y, m29.z, 0.1f);

		// ATIRAR
		if (FIRE && tirotimer >= 100) {
			float velocidade_projetil = 14;
			Projetil pj = new Projetil(m29.x + cameraVectorRight.x * 0.5f + cameraVectorUP.x * 0.2f,
					m29.y + cameraVectorRight.y * 0.5f + cameraVectorUP.y * 0.2f,
					m29.z + cameraVectorRight.z * 0.5f + cameraVectorUP.z * 0.2f);
			pj.vx = -cameraVectorFront.x * velocidade_projetil;
			pj.vy = -cameraVectorFront.y * velocidade_projetil;
			pj.vz = -cameraVectorFront.z * velocidade_projetil;
			pj.raio = 0.2f;
			pj.model = vboBilbord;
			pj.setRotation(cameraVectorFront, cameraVectorUP, cameraVectorRight);

			listaObjetos.add(pj);

			pj = new Projetil(m29.x - cameraVectorRight.x * 0.5f + cameraVectorUP.x * 0.2f,
					m29.y - cameraVectorRight.y * 0.5f + cameraVectorUP.y * 0.2f,
					m29.z - cameraVectorRight.z * 0.5f + cameraVectorUP.z * 0.2f);
			pj.vx = -cameraVectorFront.x * velocidade_projetil;
			pj.vy = -cameraVectorFront.y * velocidade_projetil;
			pj.vz = -cameraVectorFront.z * velocidade_projetil;
			pj.raio = 0.2f;
			pj.model = vboBilbord;
			pj.setRotation(cameraVectorFront, cameraVectorUP, cameraVectorRight);

			listaObjetos.add(pj);
		tirotimer = 0;
		}

		for(int i = 0; i < listaObjetos.size();i++) {
			Object3D obj = listaObjetos.get(i);
			obj.SimulaSe(diftime);
			if(obj.vivo==false) {
				listaObjetos.remove(i);
				i--;
			}
		}

		ArrayList<Object3D> objectsToRemove = new ArrayList<>();

		for (Object3D obj : listaObjetos) {
			obj.SimulaSe(diftime);
			if (obj instanceof Projetil) {
				Projetil projetil = (Projetil) obj;
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
					}
				}
			}
		}
    }

	private boolean checkCollision(Object3D obj1, Object3D obj2) {
		float dx = obj1.x - obj2.x;
		float dy = obj1.y - obj2.y;
		float dz = obj1.z - obj2.z;
		float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		return distance < (obj1.raio + obj2.raio);
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
