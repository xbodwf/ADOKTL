package com.adoktl.platform.desktop

import com.adoktl.render.*
import com.adoktl.math.AdoktlColor
import com.adoktl.math.Vector2
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil

class DesktopOpenGLBackend : RenderBackendApi {

    override val name: String = "OpenGL (Desktop)"

    private var window: Long = 0
    private var width: Int = 1920
    private var height: Int = 1080
    private var vaoId: Int = 0
    private var vboId: Int = 0
    private var eboId: Int = 0
    private var programId: Int = 0

    private var drawCallCount = 0
    private var triangleCount = 0
    private var vertexCount = 0

    override fun init(config: RenderConfig): Boolean {
        width = config.width
        height = config.height

        if (!glfwInit()) {
            System.err.println("Failed to initialize GLFW")
            return false
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        if (config.multisampling > 0) {
            glfwWindowHint(GLFW_SAMPLES, config.multisampling)
        }
        if (config.vsync) {
            glfwSwapInterval(1)
        }

        window = glfwCreateWindow(width, height, "ADOKTL", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) {
            System.err.println("Failed to create GLFW window")
            glfwTerminate()
            return false
        }

        glfwMakeContextCurrent(window)
        glfwShowWindow(window)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        if (config.multisampling > 0) {
            glEnable(GL_MULTISAMPLE)
        }

        vaoId = glGenVertexArrays()
        vboId = glGenBuffers()
        eboId = glGenBuffers()
        programId = createShaderProgram()

        glBindVertexArray(vaoId)
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * 4, 0L)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * 4, 3 * 4L)
        glEnableVertexAttribArray(1)

        return true
    }

    override fun shutdown() {
        glDeleteProgram(programId)
        glDeleteBuffers(eboId)
        glDeleteBuffers(vboId)
        glDeleteVertexArrays(vaoId)
        if (window != 0L) {
            glfwDestroyWindow(window)
        }
        glfwTerminate()
    }

    override fun beginFrame() {
        drawCallCount = 0
        triangleCount = 0
        vertexCount = 0
    }

    override fun endFrame() {
        glfwSwapBuffers(window)
        glfwPollEvents()
    }

    override fun clear(color: AdoktlColor) {
        glClearColor(color.r, color.g, color.b, color.a)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }

    override fun setCamera(camera: CameraData) {
        glUseProgram(programId)
        val projLoc = glGetUniformLocation(programId, "uProjection")
        val viewLoc = glGetUniformLocation(programId, "uView")

        val aspect = camera.projectionRatio
        val zoom = camera.zoom / 100.0
        val left = -aspect * zoom
        val right = aspect * zoom
        val bottom = -1.0 * zoom
        val top = 1.0 * zoom

        val cosR = kotlin.math.cos(camera.rotation * kotlin.math.PI / 180.0)
        val sinR = kotlin.math.sin(camera.rotation * kotlin.math.PI / 180.0)
        val tx = -camera.position.x
        val ty = -camera.position.y

        val ortho = floatArrayOf(
            2f / (right - left).toFloat(), 0f, 0f, 0f,
            0f, 2f / (top - bottom).toFloat(), 0f, 0f,
            0f, 0f, -1f, 0f,
            -((right + left) / (right - left)).toFloat(), -((top + bottom) / (top - bottom)).toFloat(), 0f, 1f
        )

        val view = floatArrayOf(
            cosR.toFloat(), -sinR.toFloat(), 0f, 0f,
            sinR.toFloat(), cosR.toFloat(), 0f, 0f,
            0f, 0f, 1f, 0f,
            (tx * cosR - ty * sinR).toFloat(), (tx * sinR + ty * cosR).toFloat(), 0f, 1f
        )

        glUniformMatrix4fv(projLoc, false, ortho)
        glUniformMatrix4fv(viewLoc, false, view)
    }

    override fun drawMesh(mesh: Mesh) {
        if (mesh.isEmpty()) return

        glUseProgram(programId)
        glBindVertexArray(vaoId)

        val vertexData = FloatArray(mesh.vertices.size + (mesh.colors?.size ?: mesh.vertices.size / 3 * 4))
        var vi = 0
        var ci = 0
        var di = 0

        val colors = mesh.colors ?: FloatArray(mesh.vertices.size / 3 * 4) { 1f }
        while (vi < mesh.vertices.size) {
            vertexData[di++] = mesh.vertices[vi++]
            vertexData[di++] = mesh.vertices[vi++]
            vertexData[di++] = mesh.vertices[vi++]
            vertexData[di++] = colors[ci++]
            vertexData[di++] = colors[ci++]
            vertexData[di++] = colors[ci++]
            vertexData[di++] = if (ci < colors.size) colors[ci++] else 1f
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh.indices, GL_DYNAMIC_DRAW)

        glDrawElements(GL_TRIANGLES, mesh.indices.size, GL_UNSIGNED_INT, 0)

        drawCallCount++
        triangleCount += mesh.indices.size / 3
        vertexCount += mesh.vertices.size / 3
    }

    override fun drawInstanced(mesh: Mesh, positions: List<Vector2>, colors: List<AdoktlColor>) {
        for (i in positions.indices) {
            drawMesh(mesh)
        }
    }

    override fun createTexture(width: Int, height: Int, data: ByteArray): Int {
        val texId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texId)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.memByteBuffer(data))
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        return texId
    }

    override fun updateTexture(id: Int, width: Int, height: Int, data: ByteArray) {
        glBindTexture(GL_TEXTURE_2D, id)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.memByteBuffer(data))
    }

    override fun deleteTexture(id: Int) {
        glDeleteTextures(id)
    }

    override fun pushScissor(x: Int, y: Int, width: Int, height: Int) {
        glEnable(GL_SCISSOR_TEST)
        glScissor(x, this.height - y - height, width, height)
    }

    override fun popScissor() {
        glDisable(GL_SCISSOR_TEST)
    }

    override val stats: RenderStats
        get() = RenderStats(
            triangleCount = triangleCount,
            drawCalls = drawCallCount,
            vertexCount = vertexCount,
            backendInfo = "OpenGL 3.3 Core"
        )

    fun shouldClose(): Boolean = glfwWindowShouldClose(window)

    fun pollEvents() = glfwPollEvents()

    private fun createShaderProgram(): Int {
        val vertexSrc = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec4 aColor;
            uniform mat4 uProjection;
            uniform mat4 uView;
            out vec4 vColor;
            void main() {
                gl_Position = uProjection * uView * vec4(aPos, 1.0);
                vColor = aColor;
            }
        """

        val fragmentSrc = """
            #version 330 core
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vColor;
            }
        """

        val vsId = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vsId, vertexSrc)
        glCompileShader(vsId)
        if (glGetShaderi(vsId, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Vertex shader error: ${glGetShaderInfoLog(vsId)}")
        }

        val fsId = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fsId, fragmentSrc)
        glCompileShader(fsId)
        if (glGetShaderi(fsId, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Fragment shader error: ${glGetShaderInfoLog(fsId)}")
        }

        val progId = glCreateProgram()
        glAttachShader(progId, vsId)
        glAttachShader(progId, fsId)
        glLinkProgram(progId)
        if (glGetProgrami(progId, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Program link error: ${glGetProgramInfoLog(progId)}")
        }

        glDeleteShader(vsId)
        glDeleteShader(fsId)

        return progId
    }
}