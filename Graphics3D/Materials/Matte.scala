package Graphics3D.Materials

import scala.math.pow

import Graphics3D.Components._
import Graphics3D.Colors._
import Graphics3D.GeometryUtils._

case class Matte(diffuse: Color = LIGHT_GRAY, specular: Color = WHITE, shininess: Double = 64) extends Material {

  private val ambient = diffuse * 0.1

  override def shade[O <: Shape](
    scene: Scene[O], incident: Vec3, hitPoint: Vec3, normal: Vec3, depth: Int, inside: Boolean
  ): Color = {
    val biasedHitPoint = hitPoint + normal * scene.rayHitBias

    def addLight(color: Color, light: Light): Color = {
      val shadow = if (scene.renderShadows) scene.getShadow(biasedHitPoint, light) else 1
      if (shadow > 0) {

        val lightVec = new Vec3(light.location, hitPoint).normalize
        val diffuseIntensity = -(lightVec dot normal)

        if (diffuseIntensity > 0) {
          val diffuseColor = diffuse * light.color * diffuseIntensity * shadow

          val specularIntensity = pow(reflection(lightVec, normal) dot incident, shininess)
          if (specularIntensity > 0)
            color + diffuseColor + (specular * light.color * specularIntensity * shadow)
          else
            color + diffuseColor
        } else color
      } else color
    }
    scene.lights.foldLeft(ambient)(addLight)
  }
}
