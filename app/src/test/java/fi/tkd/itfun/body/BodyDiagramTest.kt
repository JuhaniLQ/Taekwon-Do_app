package fi.tkd.itfun.body

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyDiagramTest {
    @Test
    fun ellipseIncludesCenterAndEdgeButExcludesBoundingBoxCorners() {
        val ellipse = EllipseNorm(cx = 0.5f, cy = 0.5f, rx = 0.25f, ry = 0.125f)

        assertTrue(ellipse.contains(0.5f, 0.5f))
        assertTrue(ellipse.contains(0.75f, 0.5f))
        assertTrue(ellipse.contains(0.5f, 0.625f))
        assertFalse(ellipse.contains(0.75f, 0.625f))
        assertFalse(ellipse.contains(0.8f, 0.5f))
    }

    @Test
    fun zeroRadiusIsNotTappable() {
        assertFalse(EllipseNorm(0.5f, 0.5f, 0f, 0.1f).contains(0.5f, 0.5f))
    }

    @Test
    fun wideContainerCentersImageHorizontally() {
        val bounds = bodyDiagramBounds(width = 800f, height = 300f)

        assertEquals(200f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(400f, bounds.width, 0.001f)
        assertEquals(300f, bounds.height, 0.001f)
    }

    @Test
    fun tallContainerCentersImageVertically() {
        val bounds = bodyDiagramBounds(width = 400f, height = 600f)

        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(150f, bounds.top, 0.001f)
        assertEquals(400f, bounds.width, 0.001f)
        assertEquals(300f, bounds.height, 0.001f)
    }
}
