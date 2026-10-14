package io.github.miniwolskys.transparentpaintings;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.miniwolskys.transparentpaintings.mixin.SpriteContentsAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.util.ARGB;

/**
 * Which pixels along each side of a painting texture are visible enough to get an edge strip.
 * Indexed in texture space: top/bottom by pixel column, left/right by pixel row.
 * Animated textures use their first frame.
 */
public record EdgeMask(int textureWidth, int textureHeight, boolean[] top, boolean[] bottom, boolean[] left, boolean[] right) {
	/** Border pixels at least this opaque get an edge (50%). */
	private static final int ALPHA_THRESHOLD = 128;

	// Sprite contents are recreated on every resource reload, so stale entries are simply collected.
	private static final Map<SpriteContents, EdgeMask> CACHE = new WeakHashMap<>();

	public static EdgeMask of(SpriteContents contents) {
		return CACHE.computeIfAbsent(contents, EdgeMask::compute);
	}

	private static EdgeMask compute(SpriteContents contents) {
		NativeImage image = ((SpriteContentsAccessor) contents).transparentpaintings$getOriginalImage();
		int width = contents.width();
		int height = contents.height();
		boolean[] top = new boolean[width];
		boolean[] bottom = new boolean[width];
		boolean[] left = new boolean[height];
		boolean[] right = new boolean[height];

		for (int x = 0; x < width; x++) {
			top[x] = isVisible(image, x, 0);
			bottom[x] = isVisible(image, x, height - 1);
		}

		for (int y = 0; y < height; y++) {
			left[y] = isVisible(image, 0, y);
			right[y] = isVisible(image, width - 1, y);
		}

		return new EdgeMask(width, height, top, bottom, left, right);
	}

	private static boolean isVisible(NativeImage image, int x, int y) {
		return ARGB.alpha(image.getPixel(x, y)) >= ALPHA_THRESHOLD;
	}
}
