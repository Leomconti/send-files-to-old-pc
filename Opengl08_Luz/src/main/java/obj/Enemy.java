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
        
        x += vx * diftime / 1000.0f;
        y += vy * diftime / 1000.0f;
        z += vz * diftime / 1000.0f;
        
        // here we could add some movement or something, just make them spin around or summ
    }
}
