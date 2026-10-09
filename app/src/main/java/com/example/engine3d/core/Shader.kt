package com.example.engine3d.core

import android.opengl.GLES20
import android.util.Log

class Shader {
    var programId: Int = 0
        private set

    // Attribute locations
    var aPositionLocation: Int = -1
    var aNormalLocation: Int = -1
    var aColorLocation: Int = -1
    var aTexCoordLocation: Int = -1

    // Uniform locations
    var uMVPMatrixLocation: Int = -1
    var uModelMatrixLocation: Int = -1
    var uLightDirLocation: Int = -1
    var uLightColorLocation: Int = -1
    var uAmbientColorLocation: Int = -1
    var uViewPosLocation: Int = -1
    var uBaseColorLocation: Int = -1
    var uSpecularStrengthLocation: Int = -1
    var uShininessLocation: Int = -1
    var uFogColorLocation: Int = -1
    var uFogDensityLocation: Int = -1
    var uFogEnabledLocation: Int = -1
    var uUseLightingLocation: Int = -1
    var uShadingQualityLocation: Int = -1
    var uPointLightPosLocation: Int = -1
    var uPointLightColorLocation: Int = -1
    var uPointLightRadiusLocation: Int = -1
    var uUseVertexColorLocation: Int = -1
    var uTextureLocation: Int = -1
    var uUseTextureLocation: Int = -1

    init {
        compileAndLink()
    }

    private fun compileAndLink() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            attribute vec2 aTexCoord;
            
            varying vec3 vFragPos;
            varying vec3 vNormal;
            varying vec4 vColor;
            varying vec2 vTexCoord;
            varying float vDistance;
            
            void main() {
                vFragPos = vec3(uModelMatrix * aPosition);
                // Transform normal with 3x3 model matrix
                vNormal = normalize(mat3(uModelMatrix[0].xyz, uModelMatrix[1].xyz, uModelMatrix[2].xyz) * aNormal);
                vColor = aColor;
                vTexCoord = aTexCoord;
                
                vec4 clipPos = uMVPMatrix * aPosition;
                vDistance = clipPos.w;
                gl_Position = clipPos;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            
            uniform vec3 uLightDir;
            uniform vec3 uLightColor;
            uniform vec3 uAmbientColor;
            uniform vec3 uViewPos;
            
            uniform vec4 uBaseColor;
            uniform float uSpecularStrength;
            uniform float uShininess;
            
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform int uFogEnabled;
            uniform int uUseLighting;
            uniform int uShadingQuality; // 0=Unlit, 1=Fast Gouraud-like, 2=Blinn-Phong
            uniform int uUseVertexColor;
            uniform int uUseTexture;
            uniform sampler2D uTexture;
            
            uniform vec3 uPointLightPos;
            uniform vec3 uPointLightColor;
            uniform float uPointLightRadius;
            
            varying vec3 vFragPos;
            varying vec3 vNormal;
            varying vec4 vColor;
            varying vec2 vTexCoord;
            varying float vDistance;
            
            void main() {
                vec4 base = uBaseColor;
                if (uUseTexture == 1) {
                    vec4 texColor = texture2D(uTexture, vTexCoord);
                    if (uUseVertexColor == 1) {
                        base = texColor * vColor * uBaseColor;
                    } else {
                        base = texColor * uBaseColor;
                    }
                } else if (uUseVertexColor == 1) {
                    base = vColor * uBaseColor;
                }
                
                if (uUseLighting == 0 || uShadingQuality == 0) {
                    // Unlit mode (super light performance)
                    gl_FragColor = base;
                    return;
                }
                
                vec3 norm = vNormal;
                if (length(norm) < 0.01) {
                    norm = vec3(0.0, 1.0, 0.0);
                } else {
                    norm = normalize(norm);
                }
                
                vec3 lightDir = normalize(uLightDir);
                
                // Hemispheric ambient lighting (sky light from above, ground bounce from below)
                float hemi = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 skyAmbient = uAmbientColor * 1.35;
                vec3 groundAmbient = uAmbientColor * 0.85;
                vec3 ambient = mix(groundAmbient, skyAmbient, hemi);
                
                // Natural soft wrap diffuse + subtle bounce fill so characters and objects are clearly lit from all angles
                float NdotL = dot(norm, lightDir);
                float wrapDiff = clamp((NdotL + 0.45) / 1.45, 0.0, 1.0);
                float backFill = clamp((-NdotL + 0.30) / 1.30, 0.0, 1.0) * 0.35;
                vec3 diffuse = (wrapDiff + backFill) * uLightColor;
                
                // Specular (Blinn-Phong)
                vec3 specular = vec3(0.0);
                if (uShadingQuality >= 2 && NdotL > -0.1) {
                    vec3 viewDir = normalize(uViewPos - vFragPos);
                    vec3 halfwayDir = normalize(lightDir + viewDir);
                    float spec = pow(max(dot(norm, halfwayDir), 0.0), uShininess);
                    specular = uSpecularStrength * spec * uLightColor;
                }
                
                // Dynamic Point Light (Torch / Beacon / Crystal)
                vec3 pointLightContrib = vec3(0.0);
                if (uShadingQuality >= 1 && uPointLightRadius > 0.1) {
                    vec3 toPoint = uPointLightPos - vFragPos;
                    float dist = length(toPoint);
                    if (dist < uPointLightRadius) {
                        float atten = clamp(1.0 - (dist / uPointLightRadius), 0.0, 1.0);
                        atten = atten * atten;
                        vec3 pointDir = toPoint / max(dist, 0.001);
                        float pDiff = max(abs(dot(norm, pointDir)), 0.0);
                        pointLightContrib = pDiff * uPointLightColor * atten;
                    }
                }
                
                vec3 lighting = ambient + diffuse + pointLightContrib;
                vec3 resultColor = (lighting * base.rgb) + specular;
                
                // Atmospheric Fog
                if (uFogEnabled == 1 && uFogDensity > 0.0001) {
                    float fogFactor = clamp(exp(-pow(vDistance * uFogDensity, 1.5)), 0.0, 1.0);
                    resultColor = mix(uFogColor, resultColor, fogFactor);
                }
                
                gl_FragColor = vec4(resultColor, base.a);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(programId)
            Log.e("Shader", "Link error: $log")
            GLES20.glDeleteProgram(programId)
            programId = 0
            return
        }

        // Cache locations
        aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLocation = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorLocation = GLES20.glGetAttribLocation(programId, "aColor")
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")

        uMVPMatrixLocation = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixLocation = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uLightDirLocation = GLES20.glGetUniformLocation(programId, "uLightDir")
        uLightColorLocation = GLES20.glGetUniformLocation(programId, "uLightColor")
        uAmbientColorLocation = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uViewPosLocation = GLES20.glGetUniformLocation(programId, "uViewPos")
        uBaseColorLocation = GLES20.glGetUniformLocation(programId, "uBaseColor")
        uSpecularStrengthLocation = GLES20.glGetUniformLocation(programId, "uSpecularStrength")
        uShininessLocation = GLES20.glGetUniformLocation(programId, "uShininess")
        uFogColorLocation = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLocation = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uFogEnabledLocation = GLES20.glGetUniformLocation(programId, "uFogEnabled")
        uUseLightingLocation = GLES20.glGetUniformLocation(programId, "uUseLighting")
        uShadingQualityLocation = GLES20.glGetUniformLocation(programId, "uShadingQuality")
        uPointLightPosLocation = GLES20.glGetUniformLocation(programId, "uPointLightPos")
        uPointLightColorLocation = GLES20.glGetUniformLocation(programId, "uPointLightColor")
        uPointLightRadiusLocation = GLES20.glGetUniformLocation(programId, "uPointLightRadius")
        uUseVertexColorLocation = GLES20.glGetUniformLocation(programId, "uUseVertexColor")
        uTextureLocation = GLES20.glGetUniformLocation(programId, "uTexture")
        uUseTextureLocation = GLES20.glGetUniformLocation(programId, "uUseTexture")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Log.e("Shader", "Compile error in ${if (type == GLES20.GL_VERTEX_SHADER) "VERTEX" else "FRAGMENT"}: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun use() {
        if (programId != 0) {
            GLES20.glUseProgram(programId)
        }
    }
}
