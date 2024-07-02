package obj;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import shaders.ShaderProgram;
import util.Utils3D;
import java.nio.FloatBuffer;
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
    
    private float speed = 0.01f; // Adjust this value to change enemy speed
    
    public Enemy(float x, float y, float z, float r) {
        super(x, y, z);
        raio = r;
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
    }

    public void moveTowardsPlayer(float playerX, float playerY, float playerZ) {
        Vector3f direction = new Vector3f(playerX - x, playerY - y, playerZ - z);
        float length = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);
        direction.x /= length;
        direction.y /= length;
        direction.z /= length;

        speed = (float) Math.random() * 0.01f + 0.01f;
        x += direction.x * speed;
        y += direction.y * speed;
        z += direction.z * speed;

        // Update orientation
        Front.set(-direction.x, -direction.y, -direction.z, 1.0f);
        Utils3D.vec3dNormilize(Front);
        Right = Utils3D.crossProduct(UP, Front);
        Utils3D.vec3dNormilize(Right);
        UP = Utils3D.crossProduct(Front, Right);
        Utils3D.vec3dNormilize(UP);
    }
}
