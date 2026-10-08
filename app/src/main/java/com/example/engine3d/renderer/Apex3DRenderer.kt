        // 5. Render Player Character (ANIMATED & SEAMLESS)
        if (!camera.isFirstPerson) {
            val customChar = customModelManager.activeCustomCharacterMesh
            if (customChar != null) {
                val pConfig = customModelManager.playerConfig
                val speed = kotlin.math.sqrt(physicsEngine.characterVel.x * physicsEngine.characterVel.x + physicsEngine.characterVel.z * physicsEngine.characterVel.z)
                val isSlashing = actionManager.isActionInProgress

                val deltaTime = (currentFrameTimeMs / 1000f).coerceIn(0.001f, 0.1f)
                val animPose = animationPlayer.evaluatePose(
                    mesh = customChar,
                    config = pConfig,
                    speed = speed,
                    isGrounded = physicsEngine.isGrounded,
                    isSprinting = physicsEngine.isSprinting,
                    isSlashing = isSlashing,
                    dt = deltaTime
                )

                // ⚡ GERAKKAN 42 TULANG DAN LENTURKAN DAGING/KULIT (KELUAR DARI T-POSE!)
                if (customChar.nodes.isNotEmpty()) {
                    updateGlbNodeTransforms(
                        mesh = customChar,
                        clipName = animPose.activeClipName,
                        time = animationPlayer.animTimeSec,
                        preferredClip = animPose.matchedGlbClip,
                        limbSwingAngle = animPose.limbSwingAngle
                    )
                    // Evaluasi Linear Blend Skinning di CPU:
                    customChar.applySkinning()
                }

                val facingOffset = if (settings.invertCharacterFacing) 180f else 0f
                val charMat = Mat4()
                    .translate(
                        physicsEngine.characterPos.x + animPose.offsetX,
                        physicsEngine.characterPos.y + pConfig.heightOffset + animPose.offsetY,
                        physicsEngine.characterPos.z + animPose.offsetZ
                    )
                    .rotate(-physicsEngine.characterYawDeg + pConfig.rotationOffsetYDeg + animPose.rotationYDeg + facingOffset, 0f, 1f, 0f)
                    .rotate(animPose.pitchXDeg, 1f, 0f, 0f)
                    .rotate(animPose.rollZDeg, 0f, 0f, 1f)
                    .scale(
                        pConfig.scaleX * animPose.scaleMultX,
                        pConfig.scaleY * animPose.scaleMultY,
                        pConfig.scaleZ * animPose.scaleMultZ
                    )

                val charMvp = Mat4().set(camera.viewProjMatrix).multiply(charMat)

                val prevCullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE)
                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glDisable(GLES20.GL_CULL_FACE)
                }

                GLES20.glUniformMatrix4fv(shader.uMVPMatrixLocation, 1, false, charMvp.data, 0)
                GLES20.glUniformMatrix4fv(shader.uModelMatrixLocation, 1, false, charMat.data, 0)
                GLES20.glUniform4f(shader.uBaseColorLocation, 1f, 1f, 1f, 1f)
                GLES20.glUniform1f(shader.uSpecularStrengthLocation, 0.35f)
                GLES20.glUniform1f(shader.uShininessLocation, 24f)
                GLES20.glUniform1i(shader.uUseVertexColorLocation, if (customChar.colors != null) 1 else 0)

                // Render model utuh yang sudah di-skinning dan beranimasi
                customChar.render(
                    shader.aPositionLocation,
                    shader.aNormalLocation,
                    shader.aColorLocation,
                    settings.enableWireframe
                )
                GLES20.glUniform1i(shader.uUseVertexColorLocation, 0)

                if (settings.twoSidedGlbRendering && prevCullFace) {
                    GLES20.glEnable(GLES20.GL_CULL_FACE)
                }

                triCount += customChar.triangleCount
                drawCallCount++
            } else {
                renderAnimatedCharacter(shader, triCount, drawCallCount).also { (t, d) ->
                    triCount = t
                    drawCallCount = d
                }
            }
        }