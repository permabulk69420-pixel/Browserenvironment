package com.permabulk.browserenvironment

import android.os.Bundle
import android.webkit.CookieManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.meta.spatial.animation.PanelAnimationFeature
import com.meta.spatial.animation.PanelQuadCylinderAnimation
import com.meta.spatial.animation.PanelQuadCylinderAnimationType
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Color4
import com.meta.spatial.core.DataModel
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.SpatialSDKExperimentalAPI
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpDisplayOptions
import com.meta.spatial.toolkit.Grabbable
import com.meta.spatial.toolkit.GrabbableType
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.createPanelEntity
import com.meta.spatial.toolkit.fromBox
import com.meta.spatial.vr.LocomotionSystem
import com.meta.spatial.vr.VRFeature

/**
 * Main (and only) activity. Builds a small night-sky environment and puts one big
 * browser screen in front of you. The screen is an Android WebView living inside a
 * Spatial SDK panel, so it's a real browser: logins, video, fullscreen, the lot.
 */
@OptIn(SpatialSDKExperimentalAPI::class)
class BrowserActivity : AppSystemActivity() {

  private var browserEntity: Entity? = null
  private var curved = false
  private var screenScale = 1f
  private var lastCurveChangeMs = 0L

  override fun registerFeatures(): List<SpatialFeature> =
      listOf(VRFeature(this), ComposeFeature(), PanelAnimationFeature())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    BrowserState.init(this)
    // No teleport / snap turn: swap the SDK's default locomotion for smooth movement.
    systemManager.unregisterSystem<LocomotionSystem>()
    systemManager.registerSystem(SmoothLocomotionSystem())
  }

  override fun onSceneReady() {
    super.onSceneReady()

    scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
    scene.setLightingEnvironment(
        ambientColor = Vector3(0.6f, 0.6f, 0.8f),
        sunColor = Vector3(2.0f, 2.0f, 2.5f),
        sunDirection = -Vector3(0.3f, 1.0f, -0.4f),
        environmentIntensity = 0.3f,
    )
    scene.updateIBLEnvironment("environment.env")
    // Viewer at the origin facing +Z; everything is placed in front of that.
    scene.setViewOrigin(0f, 0f, 0f, 0f)

    buildEnvironment()

    browserEntity =
        Entity.createPanelEntity(
            R.id.browser_panel,
            Transform(Pose(Vector3(0f, SCREEN_CENTER_Y, SCREEN_DISTANCE), Quaternion(0f, 0f, 0f))),
            Grabbable(true, GrabbableType.FACE),
            Scale(Vector3(1f)),
        )
  }

  private fun buildEnvironment() {
    // Sky
    Entity.create(
        listOf(
            Mesh("mesh://skybox".toUri(), hittable = MeshCollision.NoCollision),
            Material().apply {
              baseTextureAndroidResourceId = R.drawable.skydome
              unlit = true
            },
            Transform(Pose(Vector3(0f))),
        ),
    )

    // Glowing grid floor (a very flat box so it has proper UVs on top)
    Entity.fromBox(
            1f,
            Transform(Pose(Vector3(0f, -0.01f, 0f))),
            Material().apply {
              baseTextureAndroidResourceId = R.drawable.floor
              unlit = true
            },
        )
        .setComponent(Scale(Vector3(40f, 0.02f, 40f)))

    // Ring of thin light pillars for a sense of space and scale
    val count = 14
    for (i in 0 until count) {
      val angle = (2.0 * Math.PI * i / count)
      val radius = 9f
      val x = (Math.sin(angle) * radius).toFloat()
      val z = (Math.cos(angle) * radius).toFloat()
      val cyan = i % 2 == 0
      Entity.fromBox(
              1f,
              Transform(Pose(Vector3(x, 2f, z))),
              Material().apply {
                baseColor =
                    if (cyan) Color4(0.25f, 0.65f, 1f, 1f) else Color4(0.6f, 0.35f, 1f, 1f)
                unlit = true
              },
          )
          .setComponent(Scale(Vector3(0.06f, 4f, 0.06f)))
    }

    // Low dark stage under the screen
    Entity.fromBox(
            1f,
            Transform(Pose(Vector3(0f, 0.05f, SCREEN_DISTANCE + 0.4f))),
            Material().apply {
              baseColor = Color4(0.05f, 0.05f, 0.12f, 1f)
              roughness = 0.4f
            },
        )
        .setComponent(Scale(Vector3(3.6f, 0.1f, 0.8f)))
  }

  override fun registerPanels(): List<PanelRegistration> =
      listOf(
          ComposeViewPanelRegistration(
              R.id.browser_panel,
              composeViewCreator = { _, ctx ->
                ComposeView(ctx).apply {
                  setContent {
                    BrowserPanel(
                        onToggleCurve = { toggleCurve() },
                        onResize = { delta -> resizeScreen(delta) },
                        isCurved = { curved },
                    )
                  }
                }
              },
              settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = SCREEN_WIDTH_M, height = SCREEN_HEIGHT_M),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpDisplayOptions(width = PANEL_WIDTH_DP, height = PANEL_HEIGHT_DP, dpi = PANEL_DPI),
                )
              },
          ),
      )

  /** Bends the screen into an arc centred on you, or flattens it back. */
  private fun toggleCurve() {
    val entity = browserEntity ?: return
    val now = DataModel.getLocalDataModelTime()
    if (now - lastCurveChangeMs < CURVE_ANIM_MS + 50) return
    lastCurveChangeMs = now
    entity.setComponent(
        PanelQuadCylinderAnimation(
            animationType =
                if (curved) PanelQuadCylinderAnimationType.CYLINDER_TO_QUAD
                else PanelQuadCylinderAnimationType.QUAD_TO_CYLINDER,
            targetRadius = SCREEN_DISTANCE * screenScale,
            startTime = now,
            durationInMs = CURVE_ANIM_MS,
        ),
    )
    curved = !curved
  }

  private fun resizeScreen(delta: Float) {
    val entity = browserEntity ?: return
    screenScale = (screenScale + delta).coerceIn(0.5f, 2.5f)
    entity.setComponent(Scale(Vector3(screenScale)))
  }

  override fun onPause() {
    CookieManager.getInstance().flush()
    super.onPause()
  }

  companion object {
    // Physical size of the screen in metres (16:10-ish including the toolbar)
    const val SCREEN_WIDTH_M = 2.4f
    const val PANEL_WIDTH_DP = 1280f
    const val PANEL_HEIGHT_DP = 800f
    const val SCREEN_HEIGHT_M = SCREEN_WIDTH_M * PANEL_HEIGHT_DP / PANEL_WIDTH_DP
    // 240 dpi => the WebView renders at 1920x1200 real pixels
    const val PANEL_DPI = 240
    const val SCREEN_DISTANCE = 2.4f
    const val SCREEN_CENTER_Y = 1.45f
    const val CURVE_ANIM_MS = 400L
  }
}
