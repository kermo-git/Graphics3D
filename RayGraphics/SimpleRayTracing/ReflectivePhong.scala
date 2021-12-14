package RayGraphics.SimpleRayTracing

import RayGraphics.Geometry._
import RayGraphics.Color
import RayGraphics.LinearColors._
import RayGraphics.SimpleRayTracing.Components.{Material, SceneRenderer}

case class ReflectivePhong(diffuse: Color = SILVER,
                           specular: Color = WHITE,
                           shininess: Double = 128,
                           ior: Double = 1) extends Material {

  val phong: Phong = Phong(diffuse, specular, shininess)

  override def shade(renderer: SceneRenderer,
                     incident: Vec3,
                     hitPoint: Vec3,
                     normal: Vec3,
                     depth: Int,
                     inside: Boolean): Color = {

    val diffuseColor = phong.shade(renderer, incident, hitPoint, normal, depth, inside)

    val cos = -(incident dot normal)
    val reflectionRatio = schlick(1, ior, cos)
    val reflectionColor = renderer.castRay(hitPoint, reflection(incident, normal), depth + 1)

    diffuseColor * (1 - reflectionRatio) + reflectionColor * specular * reflectionRatio
  }
}
