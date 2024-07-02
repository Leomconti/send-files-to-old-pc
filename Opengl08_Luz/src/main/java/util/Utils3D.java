package util;

import static org.lwjgl.opengl.GL11.glFrustum;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import org.lwjgl.util.vector.Vector3f;

public class Utils3D {
  public static Vector4f crossProduct(Vector4f v1, Vector4f v2) {
      return new Vector4f(
          v1.y * v2.z - v1.z * v2.y,
          v1.z * v2.x - v1.x * v2.z,
          v1.x * v2.y - v1.y * v2.x,
          1.0f
      );
  }
	
	public static Matrix4f setLookAtMatrix(Vector4f pos,Vector4f front,Vector4f up,Vector4f right) {
		Matrix4f m = new Matrix4f();
		m.m00 = right.x;
		m.m01 = up.x;
		m.m02 = front.x;
		m.m03 = 0.0f;
		
		m.m10 = right.y;
		m.m11 = up.y;
		m.m12 = front.y;
		m.m13 = 0.0f;
		
		m.m20 = right.z;
		m.m21 = up.z;
		m.m22 = front.z;
		m.m23 = 0.0f;
		
		m.m30 = -pos.x;
		m.m31 = -pos.y;
		m.m32 = -pos.z;
		m.m33 = 1.0f;		
		
		return m;
	}
	
	public static Matrix4f setLookAtMatrixB(Vector4f pos,Vector4f front,Vector4f up,Vector4f right) {
		Matrix4f m = new Matrix4f();
		m.m00 = right.x;
		m.m01 = right.y;
		m.m02 = right.z;
		m.m03 = pos.x;
		
		m.m10 = up.x;
		m.m11 = up.y;
		m.m12 = up.z;
		m.m13 = pos.y;
		
		m.m20 = front.x;
		m.m21 = front.y;
		m.m22 = front.z;
		m.m23 = pos.z;
		
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;		
		
		return m;
	}	
	
	public static Matrix4f positionMatrix(Vector4f front,Vector4f up,Vector4f right) {
		Matrix4f m = new Matrix4f();
		m.m00 = right.x;
		m.m01 = right.y;
		m.m02 = right.z;
		m.m03 = 0;
		
		m.m10 = up.x;
		m.m11 = up.y;
		m.m12 = up.z;
		m.m13 = 0;
		
		m.m20 = front.x;
		m.m21 = front.y;
		m.m22 = front.z;
		m.m23 = 0;
		
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;		
		
		return m;
	}	
	
	public static double vecMag(Vector4f v) {
		return Math.sqrt(v.x*v.x+v.y*v.y+v.z*v.z);
	}
	
	public static void vec3dNormilize(Vector4f v) {
		double mag = vecMag(v);
		v.setX((float)(v.x/mag));
		v.setY((float)(v.y/mag));
		v.setZ((float)(v.z/mag));
	}
	
//////////////////////////////////////////////////////////////////////////////
//equivalent to glFrustum()
//PARAMS: (left, right, bottom, top, near, far)
///////////////////////////////////////////////////////////////////////////////
	public static Matrix4f setFrustum(float l, float r, float b, float t, float n, float f)
	{
		Matrix4f m = new Matrix4f();
		m.m00 = 2 * n / (r - l);
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		
		m.m10 = 0.0f;
		m.m11 = 2 * n / (t - b);
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		
		m.m20 = (r + l) / (r - l);
		m.m21 = (t + b) / (t - b);
		m.m22 = -(f + n) / (f - n);
		m.m23 = -1;
		
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = -(2 * f * n) / (f - n);
		m.m33 = 0;

		return m;
	}
	
	public static Vector4f transformVector(Matrix4f matrix, Vector4f vector) {
        Vector4f result = new Vector4f();
        result.x = matrix.m00 * vector.x + matrix.m10 * vector.y + matrix.m20 * vector.z + matrix.m30 * vector.w;
        result.y = matrix.m01 * vector.x + matrix.m11 * vector.y + matrix.m21 * vector.z + matrix.m31 * vector.w;
        result.z = matrix.m02 * vector.x + matrix.m12 * vector.y + matrix.m22 * vector.z + matrix.m32 * vector.w;
        result.w = matrix.m03 * vector.x + matrix.m13 * vector.y + matrix.m23 * vector.z + matrix.m33 * vector.w;
        return result;
    }

	public static Vector4f subtractVectors(Vector4f v1, Vector4f v2) {
        return new Vector4f(
            v1.x - v2.x,
            v1.y - v2.y,
            v1.z - v2.z,
            0.0f  // We set w to 0 for direction vectors
        );
    }
}
