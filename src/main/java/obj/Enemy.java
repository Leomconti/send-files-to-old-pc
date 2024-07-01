package obj;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import shaders.ShaderProgram;
import util.Utils3D;
import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import Model.Model;

public class Enemy extends Object3D {
    public Vector3f cor = new Vector3f();
    public Model model = null;
    
    FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
    public float rotvel = 0;
    
    public Vector4f Front = new Vector4f(0.0f, 0.0f, -1.0f, 1.0f);
    public Vector4f UP = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
    public Vector4f Right = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
    
    private Random random = new Random();
    private float moveTimer = 0;
    private float moveDuration = 2.0f; // Change direction every 2 seconds
    
    public Enemy(float x, float y, float z, float r) {
        super(x, y, z);
        raio = r;
        randomizeVelocity();
    }
    
    private void randomizeVelocity() {
        float speed = 2.0f; // Adjust this value to change enemy speed
        vx = (random.nextFloat() - 0.5f) * speed;
        vy = (random.nextFloat() - 0.5f) * speed;
        vz = (random.nextFloat() - 0.5f) * speed;
    }
    
    @Override
    public void DesenhaSe(ShaderProgram shader) {
        Matrix4f modelm1 = new Matrix4f();
        modelm1.translate(new Vector3f(x, y, z));
        Matrix4f modelm = Utils3D.positionMatrix(Front, UP, Right);
        Matrix4f.mul(modelm1, modelm, modelm);
        modelm.scale(new Vector3f(raio, raio, raio));
        
        int modellocation = glGetUniformLocation(shader.programID, "model");
        modelm.storeTranspose(matrixBuffer);
        matrixBuffer.flip();
        glUniformMatrix4fv(modellocation, false, matrixBuffer);    
        
        model.draw();
    }
    
    @Override
    public void SimulaSe(long diftime) {
        super.SimulaSe(diftime);
        
        float dt = diftime / 1000.0f;
        
        moveTimer += dt;
        if (moveTimer >= moveDuration) {
            randomizeVelocity();
            moveTimer = 0;
        }
        
        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
        
        // Keep the enemy within certain bounds
        float bound = 400.0f; // Adjust this value to change the area enemies move in
        x = Math.max(-bound, Math.min(bound, x));
        y = Math.max(0, Math.min(bound, y)); // Keep y above 0
        z = Math.max(-bound, Math.min(bound, z));
        
        // Update orientation
        Front.set(-vx, -vy, -vz, 1.0f);
        Utils3D.vec3dNormilize(Front);
        Right = Utils3D.crossProduct(UP, Front);
        Utils3D.vec3dNormilize(Right);
        UP = Utils3D.crossProduct(Front, Right);
        Utils3D.vec3dNormilize(UP);
    }
}
