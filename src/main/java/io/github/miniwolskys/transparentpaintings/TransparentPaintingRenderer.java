package io.github.miniwolskys.transparentpaintings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Same geometry as vanilla's PaintingRenderer, split into two submissions:
 * the back and edges with vanilla's solid render type, and the front with a
 * culled translucent render type so its alpha channel is respected.
 */
public final class TransparentPaintingRenderer {
	private static final float HALF_DEPTH = 0.03125F;

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
		submitNodeCollector.submitCustomGeometry(poseStack, solidRenderType,
			(pose, buffer) -> buildBackAndEdges(pose, buffer, lightCoordsMap, width, height, back));
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

	private static void buildBackAndEdges(PoseStack.Pose pose, VertexConsumer buffer, int[] lightCoordsMap, int width, int height, TextureAtlasSprite back) {
		float offsetX = -width / 2.0F;
		float offsetY = -height / 2.0F;
		float backU0 = back.getU0();
		float backU1 = back.getU1();
		float backV0 = back.getV0();
		float backV1 = back.getV1();
		float topBottomV1 = back.getV(0.0625F);
		float leftRightU1 = back.getU(0.0625F);

		for (int segmentX = 0; segmentX < width; segmentX++) {
			for (int segmentY = 0; segmentY < height; segmentY++) {
				float x0 = offsetX + (segmentX + 1);
				float x1 = offsetX + segmentX;
				float y0 = offsetY + (segmentY + 1);
				float y1 = offsetY + segmentY;
				int light = lightCoordsMap[segmentX + segmentY * width];
				vertex(pose, buffer, x0, y0, backU1, backV0, HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x1, y0, backU0, backV0, HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x1, y1, backU0, backV1, HALF_DEPTH, 0, 0, 1, light);
				vertex(pose, buffer, x0, y1, backU1, backV1, HALF_DEPTH, 0, 0, 1, light);
				if (segmentY == height - 1) {
					vertex(pose, buffer, x0, y0, backU0, backV0, -HALF_DEPTH, 0, 1, 0, light);
					vertex(pose, buffer, x1, y0, backU1, backV0, -HALF_DEPTH, 0, 1, 0, light);
					vertex(pose, buffer, x1, y0, backU1, topBottomV1, HALF_DEPTH, 0, 1, 0, light);
					vertex(pose, buffer, x0, y0, backU0, topBottomV1, HALF_DEPTH, 0, 1, 0, light);
				}

				if (segmentY == 0) {
					vertex(pose, buffer, x0, y1, backU0, backV0, HALF_DEPTH, 0, -1, 0, light);
					vertex(pose, buffer, x1, y1, backU1, backV0, HALF_DEPTH, 0, -1, 0, light);
					vertex(pose, buffer, x1, y1, backU1, topBottomV1, -HALF_DEPTH, 0, -1, 0, light);
					vertex(pose, buffer, x0, y1, backU0, topBottomV1, -HALF_DEPTH, 0, -1, 0, light);
				}

				if (segmentX == width - 1) {
					vertex(pose, buffer, x0, y0, leftRightU1, backV0, HALF_DEPTH, -1, 0, 0, light);
					vertex(pose, buffer, x0, y1, leftRightU1, backV1, HALF_DEPTH, -1, 0, 0, light);
					vertex(pose, buffer, x0, y1, backU0, backV1, -HALF_DEPTH, -1, 0, 0, light);
					vertex(pose, buffer, x0, y0, backU0, backV0, -HALF_DEPTH, -1, 0, 0, light);
				}

				if (segmentX == 0) {
					vertex(pose, buffer, x1, y0, leftRightU1, backV0, -HALF_DEPTH, 1, 0, 0, light);
					vertex(pose, buffer, x1, y1, leftRightU1, backV1, -HALF_DEPTH, 1, 0, 0, light);
					vertex(pose, buffer, x1, y1, backU0, backV1, HALF_DEPTH, 1, 0, 0, light);
					vertex(pose, buffer, x1, y0, backU0, backV0, HALF_DEPTH, 1, 0, 0, light);
				}
			}
		}
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
