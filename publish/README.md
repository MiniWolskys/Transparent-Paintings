# Modrinth publishing kit

Everything needed to fill in the Modrinth project and its first version.

## Project settings

| Field | Value |
|---|---|
| Name | Transparent Paintings |
| URL | `transparent-paintings` |
| Summary | Lets resource packs give paintings transparent and semi-transparent pixels. Automatic, client-side only, works on any server and with shaders. |
| Icon | `icon.png` (512×512) |
| Description | Contents of `description.md` |
| Categories | Decoration, Utility |
| Additional tags | Fabric (set automatically by the version) |
| Client side | Required |
| Server side | Unsupported |
| License | MIT |
| Source code | https://github.com/MiniWolskys/Transparent-Paintings |
| Issue tracker | https://github.com/MiniWolskys/Transparent-Paintings/issues |

## Gallery (`gallery/`)

| File | Title | Description | Featured |
|---|---|---|---|
| `1-sunset-complementary.webp` | Sunset with shaders | Transparent paintings with Complementary Reimagined. | Yes |
| `2-lake-complementary.webp` | By the lake | Cut-out and gradient paintings with Complementary Reimagined. | |
| `3-stained-window.webp` | Stained window | Semi-transparent panes with an opaque frame. | |
| `4-alpha-gradient.webp` | Alpha gradient | Every level of transparency, from opaque to invisible. | |
| `5-cutout-disc.webp` | Cut-out shape | Fully transparent pixels around a round painting. | |
| `6-ghost.webp` | Ghost | A faint, low-opacity painting. | |
| `7-edges-side-view.webp` | Clean edges | Painting sides only appear where the image border is visible. | |

## Version 1.0.0

| Field | Value |
|---|---|
| File | `build/libs/transparentpaintings-1.0.0+26.3.jar` (not the `-sources` jar) |
| Version number | `1.0.0+26.3` |
| Version name | Transparent Paintings 1.0.0 |
| Channel | Release |
| Loaders | Fabric |
| Game versions | 26.3 |
| Dependencies | None |
| Changelog | Contents of `changelog-1.0.0.md` |

## Files here

- `make_icon.py`: regenerates the icon (`python make_icon.py publish`).
