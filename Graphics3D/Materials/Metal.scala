package Graphics3D.Materials

import Graphics3D.BaseObjects._
import Graphics3D.Colors._
import Graphics3D.Utils._

import scala.math.pow

case class Metal(diffuse: Color,
                 specular: Color = WHITE,
                 shininess: Double = 0.5 * 128,
                 ior: Double = 1.3,
                 reflectivity: Double = 0) extends Material {

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
    val diffuseColor = scene.lights.foldLeft(ambient)(addLight)

    if (depth < scene.maxBounces) {
      val cos = -(incident dot normal)
      val reflectionRatio = reflectivity + (1 - reflectivity) * schlick(1, ior, cos)
      val reflectionColor = scene.castRay(biasedHitPoint, reflection(incident, normal), depth + 1)

      diffuseColor * (1 - reflectionRatio) + reflectionColor * reflectionRatio
    }
    else diffuseColor
  }
}
