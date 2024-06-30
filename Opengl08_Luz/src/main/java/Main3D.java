
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

//import com.sun.org.apache.xerces.internal.dom.DeepNodeListImpl;

import java.nio.*;
import java.util.ArrayList;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
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
	
	Matrix4f cameraMatrix = new Matrix4f();
	

	FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
	
	Cubo3D umcubo;
	Player m29;
	
	double angluz = 0;

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
		
			if(action == GLFW_PRESS) {
				if ( key == GLFW_KEY_W) {
					UP = true;
				}
				if ( key == GLFW_KEY_S) {
					DOWN = true;
				}
				if ( key == GLFW_KEY_A) {
					RIGHT = true;
				}
				if ( key == GLFW_KEY_D) {
					LEFT = true;
				}
				if ( key == GLFW_KEY_Q) {
					QBu = true;
				}
				if ( key == GLFW_KEY_E) {
					EBu = true;
				}
				if ( key == GLFW_KEY_UP) {
					FORWARD = true;
				}
				if ( key == GLFW_KEY_DOWN) {
					BACKWARD = true;
				}
				if ( key == GLFW_KEY_SPACE) {
					FIRE = true;
				}
			}
			if(action == GLFW_RELEASE) {
				if ( key == GLFW_KEY_W) {
					UP = false;
				}
				if ( key == GLFW_KEY_S) {
					DOWN = false;
				}
				if ( key == GLFW_KEY_A) {
					RIGHT = false;
				}
				if ( key == GLFW_KEY_D) {
					LEFT = false;
				}
				if ( key == GLFW_KEY_Q) {
					QBu = false;
				}
				if ( key == GLFW_KEY_E) {
					EBu = false;
				}
				if ( key == GLFW_KEY_UP) {
					FORWARD = false;
				}
				if ( key == GLFW_KEY_DOWN) {
					BACKWARD = false;
				}
				if ( key == GLFW_KEY_SPACE) {
					FIRE = false;
				}
			}
		});

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
	
	private void createEnemies() {
        ObjModel enemyModel = new ObjModel();
        enemyModel.loadObj("Mig_29_obj.obj");
        enemyModel.load();
        
        for (int i = 0; i < 5; i++) {
            Enemy enemy = new Enemy(
                rnd.nextFloat() * 20 - 10,
                rnd.nextFloat() * 10 + 5,
                rnd.nextFloat() * 20 - 10,
                0.01f
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
		createEnemies();
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
		
		m29 = new Player(0, 0, 0, 0.01f);
		m29.model = mig29;
		
		ObjHTGsrtm model = new ObjHTGsrtm();
		model.load();
		
		Constantes.mapa = new Mapa3D(-10.0f, 0.0f, -10.0f, 10);
		Constantes.mapa.model = model;
		
		umcubo = new Cubo3D(0.0f, 0.0f, 0.8f, 0.1f);
		umcubo.model = vboc;
		
//		for(int i = 0; i < 100; i++) {
//			//Cubo3D cubo = new Cubo3D(rnd.nextFloat()*2-1,rnd.nextFloat()*2-1, rnd.nextFloat()*2-1, rnd.nextFloat()*0.005f+0.0001f);
//			//cubo.model = x35;
//			Cubo3D cubo = new Cubo3D(rnd.nextFloat()*2-1,rnd.nextFloat()*2-1, rnd.nextFloat()*2-1, rnd.nextFloat()*0.1f+0.05f);
//			cubo.model = vboc;
//			cubo.vx = rnd.nextFloat()*0.4f-0.2f;
//			cubo.vy = rnd.nextFloat()*0.4f-0.2f;
//			cubo.vz = rnd.nextFloat()*0.4f-0.2f;
//			cubo.rotvel = rnd.nextFloat()*9;
//			listaObjetos.add(cubo);
//		}
		
		//BufferedImage gatorgba = new BufferedImage(imggato.getWidth(), imggato.getHeight(), BufferedImage.TYPE_INT_ARGB);
		//gatorgba.getGraphics().drawImage(imggato, 0, 0, null);
		
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
		
//		glMatrixMode(GL_PROJECTION);
//		glLoadIdentity();
//		gluPerspective(45, 600f / 800f, 0.5f, 100);
//		glMatrixMode(GL_MODELVIEW);
//		glLoadIdentity();
		
		
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
		
		tirotimer+=diftime;
		
		//angluz+=(Math.PI/4)*diftime/1000.0f;
		angluz = 0;
		
//		
//		if(RIGHT) {
//			cameraPos.x += cameraVectorRight.x*vel*diftime/1000.0f;
//			cameraPos.y += cameraVectorRight.y*vel*diftime/1000.0f;
//			cameraPos.z += cameraVectorRight.z*vel*diftime/1000.0f;
//			//System.out.println("UP "+diftime);
//		}
//		if(LEFT) {
//			cameraPos.x -= cameraVectorRight.x*vel*diftime/1000.0f;
//			cameraPos.y -= cameraVectorRight.y*vel*diftime/1000.0f;
//			cameraPos.z -= cameraVectorRight.z*vel*diftime/1000.0f;
//			//System.out.println("UP "+diftime);
//		}	
		
		Matrix4f rotTmp = new Matrix4f();
		rotTmp.setIdentity();
		if(RIGHT) {
			rotTmp.rotate(-1.0f*diftime/1000.0f, new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if(LEFT) {
			rotTmp.rotate(1.0f*diftime/1000.0f, new Vector3f(cameraVectorUP.x, cameraVectorUP.y, cameraVectorUP.z));
		}
		if(UP) {
			rotTmp.rotate(-1.0f*diftime/1000.0f, new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}
		if(DOWN) {
			rotTmp.rotate(1.0f*diftime/1000.0f, new Vector3f(cameraVectorRight.x, cameraVectorRight.y, cameraVectorRight.z));
		}
		if(QBu) {
			rotTmp.rotate(-1.0f*diftime/1000.0f, new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}
		if(EBu) {
			rotTmp.rotate(1.0f*diftime/1000.0f, new Vector3f(cameraVectorFront.x, cameraVectorFront.y, cameraVectorFront.z));
		}
		
		
		rotTmp.transform(rotTmp,cameraVectorFront, cameraVectorFront);
		rotTmp.transform(rotTmp,cameraVectorRight, cameraVectorRight);
		rotTmp.transform(rotTmp,cameraVectorUP, cameraVectorUP);
		
		Utils3D.vec3dNormilize(cameraVectorFront);
		Utils3D.vec3dNormilize(cameraVectorRight);
		Utils3D.vec3dNormilize(cameraVectorUP);
		
		if(FORWARD) {
			cameraPos.x -= cameraVectorFront.x*vel*diftime/1000.0f;
			cameraPos.y -= cameraVectorFront.y*vel*diftime/1000.0f;
			cameraPos.z -= cameraVectorFront.z*vel*diftime/1000.0f;
			//System.out.println("UP "+diftime);
		}
		if(BACKWARD) {
			cameraPos.x += cameraVectorFront.x*vel*diftime/1000.0f;
			cameraPos.y += cameraVectorFront.y*vel*diftime/1000.0f;
			cameraPos.z += cameraVectorFront.z*vel*diftime/1000.0f;
			//System.out.println("UP "+diftime);
		}		
		
		Vector4f t = new Vector4f(cameraPos.dot(cameraPos, cameraVectorRight),cameraPos.dot(cameraPos, cameraVectorUP),cameraPos.dot(cameraPos, cameraVectorFront),1.0f);
		
		view = Utils3D.setLookAtMatrix(t, cameraVectorFront, cameraVectorUP, cameraVectorRight);
		
		Matrix4f transf = new Matrix4f();
		transf.setIdentity();
		transf.translate(new Vector3f(1,1,0));
		view.mul(transf,view, view);
		
//		float migx = cameraPos.x+cameraVectorFront.x*-2;
//		float migy = cameraPos.y+cameraVectorFront.y*-2;
//		float migz = cameraPos.z+cameraVectorFront.z*-2;
//		
//		m29.x = migx;
//		m29.y = migy;
//		m29.z = migz;
		
		//view.mul(view, cameraMatrix, view);
		//view.translate(new Vector3f(-cameraPos.x,-cameraPos.y,-cameraPos.z));
		
		m29.raio = 0.01f;
		m29.Front = cameraVectorFront;
		m29.UP = cameraVectorUP;
		m29.Right = cameraVectorRight;
		m29.x = cameraPos.x - cameraVectorFront.x*2;
		m29.y = cameraPos.y - cameraVectorFront.y*2;
		m29.z = cameraPos.z - cameraVectorFront.z*2;
		
		Constantes.mapa.testaColisao(m29.x, m29.y, m29.z, 0.1f);
		
		//System.out.println(""+cameraVectorFront);
		//System.out.println(""+m29.x+" "+m29.y+" "+m29.z);
		//m29.y = -0.5f;		
		
		
		if(FIRE&&tirotimer>=100) {
			float velocidade_projetil = 14;
			Projetil pj = new Projetil(m29.x+cameraVectorRight.x*0.5f+cameraVectorUP.x*0.2f, 
									   m29.y+cameraVectorRight.y*0.5f+cameraVectorUP.y*0.2f, 
									   m29.z+cameraVectorRight.z*0.5f+cameraVectorUP.z*0.2f);
			pj.vx = -cameraVectorFront.x*velocidade_projetil;
			pj.vy = -cameraVectorFront.y*velocidade_projetil;
			pj.vz = -cameraVectorFront.z*velocidade_projetil;
			pj.raio = 0.2f;
			pj.model = vboBilbord;
			pj.setRotation(cameraVectorFront, cameraVectorUP, cameraVectorRight);
			
			listaObjetos.add(pj);
			
			pj = new Projetil(m29.x-cameraVectorRight.x*0.5f+cameraVectorUP.x*0.2f, 
					   		  m29.y-cameraVectorRight.y*0.5f+cameraVectorUP.y*0.2f, 
					          m29.z-cameraVectorRight.z*0.5f+cameraVectorUP.z*0.2f);
			pj.vx = -cameraVectorFront.x*velocidade_projetil;
			pj.vy = -cameraVectorFront.y*velocidade_projetil;
			pj.vz = -cameraVectorFront.z*velocidade_projetil;
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
		
		//view.setIdentity();
		//view.scale(new Vector3f(scale,scale,scale));
		//view.rotate(viewAngX*0.0174532f, new Vector3f(1,0,0));
		//view.rotate(viewAngY*0.0174532f, new Vector3f(0,1,0));
		//view.translate(new Vector3f(0,0,0));
		
		int viewlocation = glGetUniformLocation(shader.programID, "view");
		view.storeTranspose(matrixBuffer);
		matrixBuffer.flip();
		glUniformMatrix4fv(viewlocation, false, matrixBuffer);
		
		Constantes.mapa.DesenhaSe(shader);
		umcubo.DesenhaSe(shader);		
		
		
//		viewlocation = glGetUniformLocation(shader.programID, "view");
//		Matrix4f mvn = new Matrix4f();
//		mvn.setIdentity();
//		mvn.storeTranspose(matrixBuffer);
//		matrixBuffer.flip();
//		glUniformMatrix4fv(viewlocation, false, matrixBuffer);
		

		
		glBindTexture(GL_TEXTURE_2D, Constantes.txtmig);
		m29.DesenhaSe(shader);
		
		//glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		glBindTexture(GL_TEXTURE_2D, Constantes.texturaTiro);
		for(int i = 0; i < listaObjetos.size();i++) {
			listaObjetos.get(i).DesenhaSe(shader);
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
