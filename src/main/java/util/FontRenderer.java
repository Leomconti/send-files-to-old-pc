package util;

import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.BufferUtils;

import org.lwjgl.stb.STBTTAlignedQuad;

public class FontRenderer {

    private static final int BITMAP_W = 512;
    private static final int BITMAP_H = 512;

    private STBTTBakedChar.Buffer cdata;
    private int fontTextureID;
    private int fontHeight;

    public FontRenderer(String fontPath, int fontHeight) {
        this.fontHeight = fontHeight;
        ByteBuffer ttf = null;

        try {
            ttf = ioResourceToByteBuffer(fontPath, 160 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

        cdata = STBTTBakedChar.malloc(96);
        stbtt_BakeFontBitmap(ttf, fontHeight, bitmap, BITMAP_W, BITMAP_H, 32, cdata);

        fontTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, BITMAP_W, BITMAP_H, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        memFree(bitmap);
    }

    public void drawText(String text, float x, float y) {
        glBindTexture(GL_TEXTURE_2D, fontTextureID);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xBuffer = stack.floats(x);
            FloatBuffer yBuffer = stack.floats(y);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            glEnable(GL_TEXTURE_2D);
            glBegin(GL_QUADS);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= 32 && c < 128) {
                    stbtt_GetBakedQuad(cdata, BITMAP_W, BITMAP_H, c - 32, xBuffer, yBuffer, quad, true);
                    glTexCoord2f(quad.s0(), quad.t0());
                    glVertex2f(quad.x0(), quad.y0());
                    glTexCoord2f(quad.s1(), quad.t0());
                    glVertex2f(quad.x1(), quad.y0());
                    glTexCoord2f(quad.s1(), quad.t1());
                    glVertex2f(quad.x1(), quad.y1());
                    glTexCoord2f(quad.s0(), quad.t1());
                    glVertex2f(quad.x0(), quad.y1());
                }
            }
            glEnd();
            glDisable(GL_TEXTURE_2D);
        }
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        Path path = Path.of(resource);
        ByteBuffer buffer;
        try (FileChannel fc = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ)) {
            buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
            while (fc.read(buffer) != -1) ;
        }
        buffer.flip();
        return buffer;
    }
}
