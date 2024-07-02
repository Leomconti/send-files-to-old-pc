
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
	long diftime;
	VboCube vboc;
	VboBilboard vboBilbord;
	StaticShader shader;
	ArrayList<Object3D> listaObjetos = new ArrayList<>();
	Vector4f cameraPos = new  Vector4f(0.0f,10.0f, 0.0f,1.0f);
	Vector4f cameraVectorFront = new Vector4f(0.0f, 0.0f, -1.0f,1.0f);
	Vector4f cameraVectorUP = new Vector4f(0.0f, 1.0f, 0.0f,1.0f);
	Vector4f cameraVectorRight = new Vector4f(1.0f, 0.0f, 0.0f,1.0f);
	Matrix4f view = new Matrix4f();
	boolean QBu = false;
	boolean EBu = false;
	boolean FIRE = false;
	boolean WBu = false;
	boolean SBu = false;
	boolean enemiesCreated = false;
	FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
	Cubo3D umcubo;
	Player m29;
	Matrix4f rotTmp = new Matrix4f();
	double angluz = 0;
	float mouseX, mouseY;
	boolean mouseLeftPressed = false;
	boolean mouseRightPressed = false;
	private float cameraDistance = -2.0f;
	private float cameraHeight = 1.5f;

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
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop

			if (action == GLFW_PRESS) {
				if (key == GLFW_KEY_Q) {
					QBu = true;
				}
				if (key == GLFW_KEY_E) {
					EBu = true;
				}
				if(key == GLFW_KEY_W) {
					WBu = true;
				}
				if(key == GLFW_KEY_S) {
					SBu = true;
				}
			}
			if (action == GLFW_RELEASE) {
				if (key == GLFW_KEY_Q) {
					QBu = false;
				}
				if (key == GLFW_KEY_E) {
					EBu = false;
				}
				if(key == GLFW_KEY_W) {
					WBu = false;
				}
				if(key == GLFW_KEY_S) {
					SBu = false;
				}
			}
			;
		});

		glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
			float dx = (float) xpos - mouseX;
			float dy = (float) ypos - mouseY;
			mouseX = (float) xpos;
			mouseY = (float) ypos;

			// Invert the horizontal camera rotation direction
			float sensitivity = 0.1f;
			viewAngY -= dx * sensitivity; // Here, change '+=' to '-=' to invert the control
			viewAngX += dy * sensitivity;

			// Clamp the vertical camera rotation
			if (viewAngX > 89.0f) {
				viewAngX = 89.0f;
			}

			boolean RIGHT = dx < 0;
			boolean LEFT = dx > 0;
			boolean UP = dy < 0;
			boolean DOWN = dy > 0;

			// Update camera vectors
			updateCameraVectors(RIGHT, LEFT, UP, DOWN);
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
		float mapSize = 1000.0f;
		float formationHeight = 15.0f; // Fixed height for the formation
		float formationRadius = 50.0f; // Radius of the circular formation

		for (int i = 0; i < 10; i++) {
			float angle = (float) (i * 2 * Math.PI / 10);
			float x = (float) (Math.cos(angle) * formationRadius);
			float z = (float) (Math.sin(angle) * formationRadius);

			Enemy enemy = new Enemy(x, formationHeight, z, 0.01f);
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
		long lastSpawnTime = ultimoTempo;
		long spawnInterval = 10000;

		while (!glfwWindowShouldClose(window)) {
			diftime = System.currentTimeMillis()-ultimoTempo;
			ultimoTempo = System.currentTimeMillis();




			gameUpdate();
			gameRender();


			frame++;
			long actualTime = System.currentTimeMillis();
			if ((lasttime / 1000) != (actualTime / 1000)) {
				// System.out.println("FPS " + frame);
				frame = 0;
				lasttime = actualTime;
			}

		}
	}

	long tirotimer = 0;
	float speed = 5.0f;
	float acceleration = 0.1f;
	float deceleration = 0.05f;
	float maxSpeed = 10.0f;
	float constSpeed = 3.0f;
	float minSpeed = 1.0f;

	private void gameUpdate() {
		tirotimer += diftime;
		angluz = 0;

		float newCameraPosX = cameraPos.x - cameraVectorFront.x * speed * diftime / 1000.0f;
		float newCameraPosY = cameraPos.y - cameraVectorFront.y * speed * diftime / 1000.0f;
		float newCameraPosZ = cameraPos.z - cameraVectorFront.z * speed * diftime / 1000.0f;

		boolean camera_collided = Constantes.mapa.testaColisao(
		    newCameraPosX + cameraVectorFront.x * cameraDistance,
		    newCameraPosY + cameraVectorFront.y * cameraDistance - cameraHeight,
		    newCameraPosZ + cameraVectorFront.z * cameraDistance,
		    0.1f
		);

		if (!camera_collided) {
		    cameraPos.x = newCameraPosX;
		    cameraPos.y = newCameraPosY;
		    cameraPos.z = newCameraPosZ;
		}

		// Q and E rotation around camera's front vector (roll)
		rotTmp.setIdentity();
		float rotationSpeed = 2.0f * diftime / 1000.0f;
		if (QBu) {
			rotTmp.rotate(-rotationSpeed,
					new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}
		if (EBu) {
			rotTmp.rotate(rotationSpeed,
					new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}

		rotTmp.transform(rotTmp, cameraVectorFront, cameraVectorFront);
		rotTmp.transform(rotTmp, cameraVectorRight, cameraVectorRight);
		rotTmp.transform(rotTmp, cameraVectorUP, cameraVectorUP);

		Utils3D.vec3dNormilize(cameraVectorFront);
		Utils3D.vec3dNormilize(cameraVectorRight);
		Utils3D.vec3dNormilize(cameraVectorUP);


		if (WBu) {
			speed += acceleration;
			if (speed > maxSpeed) {
				speed = maxSpeed;
			}
		} else if (SBu) {
			speed -= acceleration;
			if (speed < minSpeed) {
				speed = minSpeed;
			}
		} else {
			// Desaceleração gradual quando nenhuma tecla está pressionada
			if (speed > constSpeed) {
				speed -= deceleration;
			}
			if(speed < constSpeed) {
				speed -= -(deceleration);
			}
		}

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
		m29.x = cameraPos.x + cameraVectorFront.x * cameraDistance;
		m29.y = cameraPos.y + cameraVectorFront.y * cameraDistance - cameraHeight;
		m29.z = cameraPos.z + cameraVectorFront.z * cameraDistance;



		boolean player_collided = Constantes.mapa.testaColisao(m29.x, m29.y, m29.z, 0.1f);
		if (player_collided) {
			System.out.println("Player collided with the map");
		}

		// ATIRAR
		if (FIRE && tirotimer >= 100) {
			float velocidade_projetil = 14;
			createProjectile(m29.x + cameraVectorRight.x * 0.5f, m29.y + cameraVectorRight.y * 0.5f, m29.z + cameraVectorRight.z * 0.5f, velocidade_projetil);
			createProjectile(m29.x - cameraVectorRight.x * 0.5f, m29.y - cameraVectorRight.y * 0.5f, m29.z - cameraVectorRight.z * 0.5f, velocidade_projetil);
			tirotimer = 0;
		}


		for (int i = 0; i < listaObjetos.size(); i++) {
			Object3D obj1 = listaObjetos.get(i);
			obj1.SimulaSe(diftime);

			if (!obj1.vivo) {
				listaObjetos.remove(i);
				i--;
				continue;
			}

			if (obj1 instanceof Projetil) {
				for (int j = 0; j < listaObjetos.size(); j++) {
					Object3D obj2 = listaObjetos.get(j);
					if (obj2 instanceof Enemy && checkCollision(obj1, obj2)) {
						((Projetil) obj1).morrendo = true;
						obj2.vivo = false;
						createExplosion(obj2.x, obj2.y, obj2.z);
						break;
					}
				}
			}

			if (obj1 instanceof Enemy) {
				if (checkCollision(m29, obj1)) {
					obj1.vivo = false;
					createExplosion(obj1.x, obj1.y, obj1.z);
					System.out.println("Player collided with an enemy!");
				} else {
					((Enemy) obj1).moveTowardsPlayer(m29.x, m29.y, m29.z);
				}
			}
		}

	}

	private void createProjectile(float x, float y, float z, float velocidade_projetil) {
		Projetil pj = new Projetil(x + cameraVectorUP.x * 0.2f, y + cameraVectorUP.y * 0.2f, z + cameraVectorUP.z * 0.2f);
		pj.vx = -cameraVectorFront.x * velocidade_projetil;
		pj.vy = -cameraVectorFront.y * velocidade_projetil;
		pj.vz = -cameraVectorFront.z * velocidade_projetil;
		pj.raio = 0.2f;
		pj.model = vboBilbord;
		pj.setRotation(cameraVectorFront, cameraVectorUP, cameraVectorRight);
		listaObjetos.add(pj);
	}

	private void createExplosion(float x, float y, float z) {
		Projetil explosion = new Projetil(x, y, z);
		explosion.raio = 0.5f;
		explosion.model = vboBilbord;
		explosion.morrendo = true;
		listaObjetos.add(explosion);
	}

	private boolean checkCollision(Object3D obj1, Object3D obj2) {
	    float dx = obj1.x - obj2.x;
	    float dy = obj1.y - obj2.y;
	    float dz = obj1.z - obj2.z;

	    float distanceSquared = dx * dx + dy * dy + dz * dz;

	    float radiusSum = obj1.raio + obj2.raio;
	    float radiusSumSquared = radiusSum * radiusSum;

	    return distanceSquared < radiusSumSquared;
	}


	private void updateCameraVectors(boolean RIGHT, boolean LEFT, boolean UP, boolean DOWN) {
		rotTmp.setIdentity();

		if(RIGHT) {
			rotTmp.rotate(-1.0f*diftime/5000.0f, new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if(LEFT) {
			rotTmp.rotate(1.0f*diftime/5000.0f, new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if(UP) {
			rotTmp.rotate(-1.0f*diftime/5000.0f, new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}
		if(DOWN) {
			rotTmp.rotate(1.0f*diftime/5000.0f, new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}

		rotTmp.transform(rotTmp, cameraVectorFront, cameraVectorFront);
		rotTmp.transform(rotTmp, cameraVectorRight, cameraVectorRight);
		rotTmp.transform(rotTmp, cameraVectorUP, cameraVectorUP);

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