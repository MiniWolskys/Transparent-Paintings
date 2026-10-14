package io.github.miniwolskys.transparentpaintings.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.miniwolskys.transparentpaintings.TransparentPaintingRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingRenderer.class)
abstract class PaintingRendererMixin {
	/**
	 * Vanilla draws the whole painting with a solid render type, which ignores alpha.
	 * When the front texture has any non-opaque pixel, draw the front with a translucent
	 * render type instead, and keep the back and edges as vanilla draws them.
	 */
	@Inject(method = "renderPainting", at = @At("HEAD"), cancellable = true)
	private void transparentpaintings$renderTransparent(
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		RenderType renderType,
		int[] lightCoordsMap,
		int width,
		int height,
		TextureAtlasSprite front,
		TextureAtlasSprite back,
		CallbackInfo ci
	) {
		if (front.transparency().isOpaque()) {
			return;
		}

		TransparentPaintingRenderer.submit(poseStack, submitNodeCollector, renderType, lightCoordsMap, width, height, front, back);
		ci.cancel();
	}
}
