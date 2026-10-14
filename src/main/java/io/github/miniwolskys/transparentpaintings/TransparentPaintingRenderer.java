package io.github.miniwolskys.transparentpaintings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * Same geometry as vanilla's PaintingRenderer, split into two submissions:
 * the back and edges with vanilla's solid render type, and the front with a
 * culled translucent render type so its alpha channel is respected.
 *
 * <p>Edges are cut into one strip per border pixel of the front texture, and a
 * strip is only drawn where that pixel is visible (see {@link EdgeMask}), so
 * see-through borders don't show a wooden side.
 *
 * <p>Texture to painting space: u = 0 is at the painting's max x, v = 0 at its max y.
 */
public final class TransparentPaintingRenderer {
	private static final float HALF_DEPTH = 0.03125F;
	/** Thickness of the edge strips in the back texture, as in vanilla. */
	private static final float EDGE_TEXTURE_SIZE = 0.0625F;

	private TransparentPaintingRenderer() {
	}

	public static void submit(
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		RenderType solidRenderType,
		int[] lightCoordsMap,
		int width,
		int height,
		TextureAtlasSprite front,
		TextureAtlasSprite back
	) {
		EdgeMask edges = EdgeMask.of(front.contents());
		submitNodeCollector.submitCustomGeometry(poseStack, solidRenderType, (pose, buffer) -> {
			buildBack(pose, buffer, lightCoordsMap, width, height, back);
			buildEdges(pose, buffer, lightCoordsMap, width, height, back, edges);
		});
		// Culling hides the front when seen from behind; the back face covers that side.
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentCull(front.atlasLocation()),
			(pose, buffer) -> buildFront(pose, buffer, lightCoordsMap, width, height, front));
	}

	private static void buildFront(PoseStack.Pose pose, VertexConsumer buffer, int[] lightCoordsMap, int width, int height, TextureAtlasSprite front) {
		float offsetX = -width / 2.0F;
		float offsetY = -height / 2.0F;
		double deltaU = 1.0 / width;
		double deltaV = 1.0 / height;

		for (int segmentX = 0; segmentX < width; segmentX++) {
			for (int segmentY = 0; segmentY < height; segmentY++) {
				float x0 = offsetX + (segmentX + 1);
				float x1 = offsetX + segmentX;
				float y0 = offsetY + (segmentY + 1);
				float y1 = offsetY + segmentY;
				int light = lightCoordsMap[segmentX + segmentY * width];
				float u0 = front.getU((float) (deltaU * (width - segmentX)));
				float u1 = front.getU((float) (deltaU * (width - (segmentX + 1))));
				float v0 = front.getV((float) (deltaV * (height - segmentY)));
				float v1 = front.getV((float) (deltaV * (height - (segmentY + 1))));
				vertex(pose, buffer, x0, y1, u1, v0, -HALF_DEPTH, 0, 0, -1, light);
				vertex(pose, buffer, x1, y1, u0, v0, -HALF_DEPTH, 0, 0, -1, light);
				vertex(pose, buffer, x1, y0, u0, v1, -HALF_DEPTH, 0, 0, -1, light);
				vertex(pose, buffer, x0, y0, u1, v1, -HALF_DEPTH, 0, 0, -1, light);
			}
		}
	}

	private static void buildBack(PoseStack.Pose pose, VertexConsumer buffer, int[] lightCoordsMap, int width, int height, TextureAtlasSprite back) {
		float offsetX = -width / 2.0F;
		float offsetY = -height / 2.0F;

		for (int segmentX = 0; segmentX < width; segmentX++) {
			for (int segmentY = 0; segmentY < height; segmentY++) {
				float x0 = offsetX + (segmentX + 1);
				float x1 = offsetX + segmentX;
				float y0 = offsetY + (segmentY + 1);
				float y1 = offsetY + segmentY;
				int light = lightCoordsMap[segmentX + segmentY * width];
				vertex(pose, buffer, x0, y0, back.getU1(), back.getV0(), HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x1, y0, back.getU0(), back.getV0(), HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x1, y1, back.getU0(), back.getV1(), HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x0, y1, back.getU1(), back.getV1(), HALF_DEPTH, 0, 0, 1, light);
			}
		}
	}

	private static void buildEdges(
		PoseStack.Pose pose, VertexConsumer buffer, int[] lightCoordsMap, int width, int height, TextureAtlasSprite back, EdgeMask edges
	) {
		float offsetX = -width / 2.0F;
		float offsetY = -height / 2.0F;
		float maxX = offsetX + width;
		float maxY = offsetY + height;
		float topBottomV1 = back.getV(EDGE_TEXTURE_SIZE);
		float leftRightU1 = back.getU(EDGE_TEXTURE_SIZE);
		float pixelWidth = (float) width / edges.textureWidth();
		float pixelHeight = (float) height / edges.textureHeight();

		// Top and bottom edges: one strip per pixel column, column 0 at max x.
		for (int px = 0; px < edges.textureWidth(); px++) {
			float x0 = maxX - px * pixelWidth;
			float x1 = x0 - pixelWidth;
			int segmentX = segment(x1 - offsetX, width);
			float segmentStart = offsetX + segmentX;
			// Vanilla maps each block of edge to the full width of the back texture, high x to u0.
			float u0 = back.getU(segmentStart + 1 - x0);
			float u1 = back.getU(segmentStart + 1 - x1);

			if (edges.top()[px]) {
				int light = lightCoordsMap[segmentX + (height - 1) * width];
				vertex(pose, buffer, x0, maxY, u0, back.getV0(), -HALF_DEPTH, 0, 1, 0, light);
				vertex(pose, buffer, x1, maxY, u1, back.getV0(), -HALF_DEPTH, 0, 1, 0, light);
				vertex(pose, buffer, x1, maxY, u1, topBottomV1, HALF_DEPTH, 0, 1, 0, light);
				vertex(pose, buffer, x0, maxY, u0, topBottomV1, HALF_DEPTH, 0, 1, 0, light);
			}

			if (edges.bottom()[px]) {
				int light = lightCoordsMap[segmentX];
				vertex(pose, buffer, x0, offsetY, u0, back.getV0(), HALF_DEPTH, 0, -1, 0, light);
				vertex(pose, buffer, x1, offsetY, u1, back.getV0(), HALF_DEPTH, 0, -1, 0, light);
				vertex(pose, buffer, x1, offsetY, u1, topBottomV1, -HALF_DEPTH, 0, -1, 0, light);
				vertex(pose, buffer, x0, offsetY, u0, topBottomV1, -HALF_DEPTH, 0, -1, 0, light);
			}
		}

		// Side edges: one strip per pixel row, row 0 at max y. Texture column 0 is the max x side.
		for (int py = 0; py < edges.textureHeight(); py++) {
			float y0 = maxY - py * pixelHeight;
			float y1 = y0 - pixelHeight;
			int segmentY = segment(y1 - offsetY, height);
			float segmentStart = offsetY + segmentY;
			float v0 = back.getV(segmentStart + 1 - y0);
			float v1 = back.getV(segmentStart + 1 - y1);

			if (edges.left()[py]) {
				int light = lightCoordsMap[width - 1 + segmentY * width];
				vertex(pose, buffer, maxX, y0, leftRightU1, v0, HALF_DEPTH, -1, 0, 0, light);
				vertex(pose, buffer, maxX, y1, leftRightU1, v1, HALF_DEPTH, -1, 0, 0, light);
				vertex(pose, buffer, maxX, y1, back.getU0(), v1, -HALF_DEPTH, -1, 0, 0, light);
				vertex(pose, buffer, maxX, y0, back.getU0(), v0, -HALF_DEPTH, -1, 0, 0, light);
			}

			if (edges.right()[py]) {
				int light = lightCoordsMap[segmentY * width];
				vertex(pose, buffer, offsetX, y0, leftRightU1, v0, -HALF_DEPTH, 1, 0, 0, light);
				vertex(pose, buffer, offsetX, y1, leftRightU1, v1, -HALF_DEPTH, 1, 0, 0, light);
				vertex(pose, buffer, offsetX, y1, back.getU0(), v1, HALF_DEPTH, 1, 0, 0, light);
				vertex(pose, buffer, offsetX, y0, back.getU0(), v0, HALF_DEPTH, 1, 0, 0, light);
			}
		}
	}

	/** Block segment containing a strip that starts at {@code offset} blocks from the painting's min side. */
	private static int segment(float offset, int size) {
		return Mth.clamp(Mth.floor(offset + 1.0E-4F), 0, size - 1);
	}

	private static void vertex(
		PoseStack.Pose pose, VertexConsumer buffer,
		float x, float y, float u, float v, float z,
		int nx, int ny, int nz, int lightCoords
	) {
		buffer.addVertex(pose, x, y, z)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(lightCoords)
			.setNormal(pose, nx, ny, nz);
	}
}
