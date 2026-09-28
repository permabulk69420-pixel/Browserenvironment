package com.permabulk.browserenvironment

import com.meta.spatial.core.Query
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.Controller
import kotlin.math.sqrt

/**
 * Smooth locomotion (replaces the SDK's teleport + snap turn).
 * Left stick: move relative to where you're looking. Right stick left/right: smooth turn.
 * The SDK only exposes stick directions as on/off bits, so speed is constant.
 */
class SmoothLocomotionSystem : SystemBase() {
  private var lastNanos = 0L

  override fun execute() {
    val now = System.nanoTime()
    val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1e9f).coerceAtMost(0.1f)
    lastNanos = now
    if (dt == 0f) return

    var buttons = 0
    Query.where { has(Controller.id) }
        .eval()
        .forEach { e ->
          val c = e.getComponent<Controller>()
          if (c.isActive) buttons = buttons or c.buttonState
        }
    fun held(bit: Int) = (buttons and bit) != 0

    // Turn
    var turn = 0f
    if (held(ButtonBits.ButtonThumbRR)) turn += 1f
    if (held(ButtonBits.ButtonThumbRL)) turn -= 1f

    // Move
    var fwd = 0f
    var side = 0f
    if (held(ButtonBits.ButtonThumbLU)) fwd += 1f
    if (held(ButtonBits.ButtonThumbLD)) fwd -= 1f
    if (held(ButtonBits.ButtonThumbLR)) side += 1f
    if (held(ButtonBits.ButtonThumbLL)) side -= 1f

    if (turn == 0f && fwd == 0f && side == 0f) return

    val scene = getScene()
    // Same calls the SDK's own locomotion uses: updateViewOrigin pivots around your head,
    // and moves read the current origin/rotation back from the scene.
    if (turn != 0f) scene.updateViewOrigin(0f, turn * TURN_DEG_PER_SEC * dt)

    if (fwd != 0f || side != 0f) {
      val head = scene.getViewerPose()
      val f = head.q.times(Vector3.Forward)
      val r = head.q.times(Vector3.Right)
      var fx = f.x
      var fz = f.z
      var rx = r.x
      var rz = r.z
      val len = sqrt(fx * fx + fz * fz)
      val rlen = sqrt(rx * rx + rz * rz)
      if (len > 1e-4f && rlen > 1e-4f) {
        fx /= len
        fz /= len
        rx /= rlen
        rz /= rlen
        var mx = fx * fwd + rx * side
        var mz = fz * fwd + rz * side
        val ml = sqrt(mx * mx + mz * mz)
        if (ml > 1f) {
          mx /= ml
          mz /= ml
        }
        val o = scene.getViewOrigin()
        scene.setViewOrigin(
            o.x + mx * MOVE_SPEED * dt,
            o.y,
            o.z + mz * MOVE_SPEED * dt,
            scene.getViewSceneRotation(),
        )
      }
    }
  }

  companion object {
    const val MOVE_SPEED = 2.0f // metres per second
    const val TURN_DEG_PER_SEC = 90f
  }
}
