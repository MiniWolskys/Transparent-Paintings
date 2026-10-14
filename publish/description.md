# Transparent Paintings

Paintings can finally be see-through.

Vanilla Minecraft ignores the alpha channel of painting textures, so transparent pixels show up as solid colour. With **Transparent Paintings**, any painting whose texture has transparent or semi-transparent pixels is drawn with real transparency: cut-out shapes, stained-glass windows, ghosts, fades… whatever your resource pack draws.

## Features

- **Full and partial transparency.** Every alpha value from 0 to 255 is respected, not just on/off.
- **Automatic.** No configuration and no special files. A painting gets transparency only if its texture actually has non-opaque pixels; fully opaque paintings render exactly like vanilla.
- **Clean edges.** The wooden sides of a painting are only drawn where the border of the image is visible (at least 50% opaque), so a round painting doesn't get a square frame around it.
- **Client-side only.** Works on any server: vanilla, Paper or Fabric. The server doesn't need the mod.
- **Shader friendly.** Tested with Sodium, Iris and Complementary Reimagined.
- **Lightweight.** No dependencies, not even Fabric API.

## Making transparent paintings

Nothing mod-specific is needed. Add paintings the vanilla way:

1. **Datapack** (on the server): `data/<namespace>/painting_variant/<name>.json`
   ```json
   {
     "asset_id": "<namespace>:<name>",
     "width": 2,
     "height": 1,
     "title": { "text": "My Painting" },
     "author": { "text": "Me" }
   }
   ```
   Add it to the `minecraft:placeable` painting variant tag if it should be picked when placing a random painting.
2. **Resource pack** (on the client, or pushed by the server): `assets/<namespace>/textures/painting/<name>.png`, a PNG with an alpha channel.

You can also make vanilla paintings transparent by overriding their textures in a resource pack.

Get a specific painting with `/give @s painting[painting/variant="<namespace>:<name>"]`.

## Good to know

- Players **without** the mod see transparent pixels as opaque, so it's best installed by everyone on a server that uses transparent paintings.
- The back of a painting keeps the vanilla look.
- Animated painting textures use their first frame to decide which edges to draw.

## Links

- Source code and issues: [GitHub](https://github.com/MiniWolskys/Transparent-Paintings)
- A test datapack and resource pack can be generated from the repository (`testpacks/`).

## Credits

Inspired by [Just Make Paintings Transparent](https://modrinth.com/mod/jumapat) and Paintings Need Glass, which did this for older versions.
