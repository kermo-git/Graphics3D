package Graphics3D.Vanilla

import Graphics3D.Geometry._
import Graphics3D.Color
import Graphics3D.LinearColors._
import Graphics3D.Vanilla.Components.{Material, Scene}

case class ReflectivePhong(diffuse: Color = SILVER,
                           specular: Color = WHITE,
                           shininess: Double = 128,
                           ior: Double = 1) extends Material {

  val phong: Phong = Phong(diffuse, specular, shininess)

  override def shade(scene: Scene,
                     incident: Vec3,
                     hitPoint: Vec3,
                     normal: Vec3,
                     depth: Int,
                     inside: Boolean): Color = {

    val diffuseColor = phong.shade(scene, incident, hitPoint, normal, depth, inside)

    val cos = -(incident dot normal)
    val reflectionRatio = schlick(1, ior, cos)
    val reflectionColor = scene.castRay(hitPoint, reflection(incident, normal), depth + 1)

    diffuseColor * (1 - reflectionRatio) + reflectionColor * specular * reflectionRatio
  }
}
