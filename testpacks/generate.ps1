# Generates the test datapack and resource pack for Transparent Paintings.
# Usage: powershell -ExecutionPolicy Bypass -File testpacks\generate.ps1
# Output: testpacks\build\TransparentPaintingsTest-datapack.zip and -resourcepack.zip

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Join-Path $PSScriptRoot 'build'
$dataDir = Join-Path $root 'datapack'
$resDir = Join-Path $root 'resourcepack'
if (Test-Path $root) { Remove-Item -Recurse -Force $root }

# Pixels per block of painting
$ppb = 32

function Write-Utf8([string]$path, [string]$text) {
	New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
	[IO.File]::WriteAllText($path, $text, (New-Object Text.UTF8Encoding $false))
}

function New-Painting([string]$name, [int]$w, [int]$h, [string]$title, [scriptblock]$pixel) {
	$bmp = New-Object Drawing.Bitmap ($w * $ppb), ($h * $ppb), ([Drawing.Imaging.PixelFormat]::Format32bppArgb)
	for ($y = 0; $y -lt $bmp.Height; $y++) {
		for ($x = 0; $x -lt $bmp.Width; $x++) {
			$argb = & $pixel $x $y $bmp.Width $bmp.Height
			$bmp.SetPixel($x, $y, [Drawing.Color]::FromArgb($argb[0], $argb[1], $argb[2], $argb[3]))
		}
	}
	$png = Join-Path $resDir "assets\tptest\textures\painting\$name.png"
	New-Item -ItemType Directory -Force (Split-Path $png) | Out-Null
	$bmp.Save($png, [Drawing.Imaging.ImageFormat]::Png)
	$bmp.Dispose()

	Write-Utf8 (Join-Path $dataDir "data\tptest\painting_variant\$name.json") @"
{
  "asset_id": "tptest:$name",
  "width": $w,
  "height": $h,
  "title": { "text": "$title", "color": "yellow" },
  "author": { "text": "Transparent Paintings test", "color": "gray" }
}
"@
	return "tptest:$name"
}

function Get-Hue([double]$t) {
	# t in [0,1) -> saturated RGB
	$h = $t * 6.0
	$i = [math]::Floor($h)
	$f = $h - $i
	$q = [int](255 * (1 - $f))
	$p = [int](255 * $f)
	switch ($i % 6) {
		0 { return 255, $p, 0 }
		1 { return $q, 255, 0 }
		2 { return 0, 255, $p }
		3 { return 0, $q, 255 }
		4 { return $p, 0, 255 }
		default { return 255, 0, $q }
	}
}

$ids = @()

# 2x1: rainbow whose alpha fades from opaque (left) to fully transparent (right), with an opaque black border.
$ids += New-Painting 'gradient' 2 1 'Alpha Gradient' {
	param($x, $y, $w, $h)
	if ($x -eq 0 -or $y -eq 0 -or $x -eq $w - 1 -or $y -eq $h - 1) { return 255, 0, 0, 0 }
	$rgb = Get-Hue ($y / $h)
	return [int](255 * (1 - $x / ($w - 1))), $rgb[0], $rgb[1], $rgb[2]
}

# 1x1: opaque disc with a white ring, fully transparent outside (on/off transparency).
$ids += New-Painting 'cutout' 1 1 'Cutout Disc' {
	param($x, $y, $w, $h)
	$d = [math]::Sqrt([math]::Pow($x - $w / 2 + 0.5, 2) + [math]::Pow($y - $h / 2 + 0.5, 2))
	if ($d -lt $w * 0.32) { return 255, 200, 30, 30 }
	if ($d -lt $w * 0.42) { return 255, 245, 245, 245 }
	return 0, 0, 0, 0
}

# 2x2: opaque wooden frame and cross, four tinted semi-transparent panes (stained glass).
$ids += New-Painting 'window' 2 2 'Stained Window' {
	param($x, $y, $w, $h)
	$b = 3
	if ($x -lt $b -or $y -lt $b -or $x -ge $w - $b -or $y -ge $h - $b -or [math]::Abs($x - $w / 2 + 0.5) -lt 2 -or [math]::Abs($y - $h / 2 + 0.5) -lt 2) {
		return 255, 92, 58, 30
	}
	$left = $x -lt $w / 2
	$top = $y -lt $h / 2
	if ($top -and $left) { return 110, 40, 90, 230 }
	if ($top) { return 110, 230, 50, 50 }
	if ($left) { return 110, 50, 200, 70 }
	return 110, 240, 210, 40
}

# 1x2: faint white ghost shape at low alpha, nothing around it.
$ids += New-Painting 'ghost' 1 2 'Ghost' {
	param($x, $y, $w, $h)
	$cx = $x - $w / 2 + 0.5
	$inHead = ([math]::Pow($cx, 2) + [math]::Pow($y - $h * 0.3, 2)) -lt [math]::Pow($w * 0.38, 2)
	$inBody = [math]::Abs($cx) -lt $w * 0.38 -and $y -ge $h * 0.3 -and $y -lt $h * 0.9 - [int](($x % 8) -lt 4) * 4
	if ($inHead -or $inBody) {
		if ($y -gt $h * 0.24 -and $y -lt $h * 0.3 -and [math]::Abs([math]::Abs($cx) - $w * 0.14) -lt 2) { return 230, 10, 10, 20 }
		return 70, 235, 240, 255
	}
	return 0, 0, 0, 0
}

# 2x2: tests partial edges. Opaque L along the left and bottom (edges drawn), fully transparent
# elsewhere, except a 70% strip on the right (edge drawn) and a 30% strip on the top (no edge).
$ids += New-Painting 'edges' 2 2 'Edge Test' {
	param($x, $y, $w, $h)
	if ($x -lt $w / 3 -or $y -ge $h * 2 / 3) { return 255, 30, 110, 60 }
	if ($x -ge $w - 6) { return 178, 230, 120, 30 }
	if ($y -lt 6) { return 77, 40, 120, 230 }
	return 0, 0, 0, 0
}

# 1x1: fully opaque checkerboard. Control: must look exactly like a vanilla painting.
$ids += New-Painting 'opaque' 1 1 'Opaque Control' {
	param($x, $y, $w, $h)
	if ((([math]::Floor($x / 8)) + ([math]::Floor($y / 8))) % 2 -eq 0) { return 255, 40, 40, 40 }
	return 255, 220, 120, 220
}

$tagValues = ($ids | ForEach-Object { "    `"$_`"" }) -join ",`n"
Write-Utf8 (Join-Path $dataDir 'data\minecraft\tags\painting_variant\placeable.json') @"
{
  "replace": false,
  "values": [
$tagValues
  ]
}
"@

Write-Utf8 (Join-Path $dataDir 'pack.mcmeta') @'
{
  "pack": {
    "description": "Transparent Paintings test paintings (data)",
    "min_format": 121,
    "max_format": 121
  }
}
'@

Write-Utf8 (Join-Path $resDir 'pack.mcmeta') @'
{
  "pack": {
    "description": "Transparent Paintings test paintings (textures)",
    "min_format": 97,
    "max_format": [97, 1]
  }
}
'@

# ZipFile.CreateFromDirectory writes backslash entry names on Windows PowerShell, which Minecraft can't read.
function New-Zip([string]$dir, [string]$zipPath) {
	$zip = [IO.Compression.ZipFile]::Open($zipPath, [IO.Compression.ZipArchiveMode]::Create)
	try {
		foreach ($file in Get-ChildItem -Recurse -File $dir) {
			$entry = $file.FullName.Substring($dir.Length + 1).Replace('\', '/')
			[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $entry) | Out-Null
		}
	} finally {
		$zip.Dispose()
	}
}

New-Zip $dataDir (Join-Path $root 'TransparentPaintingsTest-datapack.zip')
New-Zip $resDir (Join-Path $root 'TransparentPaintingsTest-resourcepack.zip')
Write-Host "Generated packs in $root"
