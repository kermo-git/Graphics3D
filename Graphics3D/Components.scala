package Graphics3D

import scala.math.{tan, toRadians}

import Colors._, Geometry._

object Components {
  trait Renderable {
    val imageWidth: Int
    val imageHeight: Int
    def getPixelColor(x: Int, y: Int): Color
  }

  type NoiseFunction = Vec3 => Double
  type TextureFunction = Vec3 => Color

  class NoiseDisplay(val imageWidth: Int,
                     val imageHeight: Int,
                     val unitSizePx: Int,
                     val noiseZ: Double = 0,
                     val noise: NoiseFunction) extends Renderable {

    override def getPixelColor(x: Int, y: Int): Color = {
      val noiseX = 1.0 * x / unitSizePx
      val noiseY = 1.0 * y / unitSizePx
      val noiseValue = noise(Vec3(noiseX, noiseY, noiseZ))
      new Color(noiseValue, noiseValue, noiseValue)
    }
  }

  class TextureDisplay(val imageWidth: Int,
                       val imageHeight: Int,
                       val unitSizePx: Int,
                       val textureZ: Double = 0,
                       val texture: TextureFunction) extends Renderable {

    override def getPixelColor(x: Int, y: Int): Color = {
      val textureX = 1.0 * x / unitSizePx
      val textureY = 1.0 * y / unitSizePx
      texture(Vec3(textureX, textureY, textureZ))
    }
  }

  abstract class Scene(val imageWidth: Int,
                       val imageHeight: Int,
                       val FOVDegrees: Int) extends Renderable {

    def getPixelColor(x: Int, y: Int): Color = castRay(ORIGIN, getCameraRay(x, y))

    private val imagePlaneWidth = 2 * tan(toRadians(FOVDegrees / 2))
    private val imagePlaneHeight = imagePlaneWidth * imageHeight / imageWidth

    def getCameraRay(x: Int, y: Int): Vec3 = {
      val _x = (imagePlaneWidth * x / imageWidth) - 0.5 * imagePlaneWidth
      val _y = (imagePlaneHeight - imagePlaneHeight * y / imageHeight) - 0.5 * imagePlaneHeight
      Vec3(_x, _y, 1).normalize
    }

    def castRay(origin: Vec3, direction: Vec3, depth: Int = 0, inside: Boolean = false): Color
  }

  abstract class PointLightScene(imageWidth: Int,
                                 imageHeight: Int,
                                 FOVDegrees: Int,

                                 val maxBounces: Int,
                                 val rayHitBias: Double,
                                 val renderShadows: Boolean,

                                 val lights: List[PointLight]) extends Scene(imageWidth, imageHeight, FOVDegrees) {

    def getShadow(point: Vec3, light: PointLight): Double
  }

  case class PointLight(location: Vec3, color: Color = WHITE,
                        intensity: Double = 50000,
                        shadowSharpness: Int = 20) {
    val energy: Color = color * intensity
  }

  trait Material {
    def shade(scene: PointLightScene, incident: Vec3, hitPoint: Vec3, normal: Vec3, recDepth: Int, inside: Boolean): Color
  }

  sealed trait MCMaterial
  case class MCDiffuse(color: Color = LIGHT_GRAY) extends MCMaterial
  case class MCLight(color: Color = WHITE, intensity: Double) extends MCMaterial {
    val energy: Color = color * intensity
  }

  case class RayHit(hitPoint: Vec3, distFromOrigin: Double)

  trait Shape[M] {
    val material: M
    def getNormal(point: Vec3): Vec3
  }

  trait RTShape[M] extends Shape[M] {
    def getRayHitDist(origin: Vec3, direction: Vec3): Option[Double]
  }

  type RTShapeHit[M] = Option[(RTShape[M], Double)]

  def trace[M](shapes: List[RTShape[M]], origin: Vec3, direction: Vec3): RTShapeHit[M] = {
    shapes.foldLeft[RTShapeHit[M]](None)((prevResult, nextShape) => {
      nextShape.getRayHitDist(origin, direction) match {
        case None => prevResult
        case Some(nextDist) =>
          val nextResult = Some((nextShape, nextDist))
          prevResult match {
            case None => nextResult
            case Some((_, prevDist)) =>
              if (prevDist < nextDist)
                prevResult
              else
                nextResult
          }
      }
    })
  }

  val SURFACE_BIAS = 0.005

  val (incX, incY, incZ) = (
    Vec3(0.001, 0, 0),
    Vec3(0, 0.001, 0),
    Vec3(0, 0, 0.001)
  )

  trait RMShape[M] extends Shape[M] {
    def getDistance(point: Vec3): Double

    override def getNormal(point: Vec3): Vec3 = {
      val gradX = getDistance(point - incX) - getDistance(point + incX)
      val gradY = getDistance(point - incY) - getDistance(point + incY)
      val gradZ = getDistance(point - incZ) - getDistance(point + incZ)

      Vec3(gradX, gradY, gradZ).normalize
    }
  }
}
