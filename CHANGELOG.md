# Changelog

## Released

## 0.1.4

### Added

- Added a global calendar derived from the server world tick counter and shared across all dimensions.
- Added a live in-game date and time display to the shared catalog interface.
- Added a Home control for returning directly to the catalog root.
- Added an Exit control to the root page as the counterpart to Back on nested pages.
- Added text-page separators for lines containing three or more hyphens.

### Changed

- Reworked the shared catalog interface around persistent top status and bottom navigation bars.
- Moved gallery navigation into the main interface while retaining click-to-open fullscreen image viewing.
- Fullscreen image viewing now returns to the embedded gallery on click or Escape and no longer includes page navigation.
- Gallery images now preserve their original aspect ratio, scale to the maximum available content area, and remain centered.
- Expanded the usable content width by reducing horizontal padding.
- Standardized content spacing, scrollbar placement, and separator widths across menus, text pages, and galleries.

### Fixed

- Fixed duplicated gallery page numbering.
- Fixed Back and Exit label alignment and improved the navigation triangle rendering.
- Fixed the text scrollbar overlapping the bottom navigation bar.
- Restored the thinner header separator while preserving catalog and text separator styling.

## 0.1.3

### Added

- Added separator entries for visually dividing groups of catalog menu items.

### Changed

- Replaced the catalog `pages` field with `gallery` for image-based entries.
- Updated the built-in fallback catalog to use the new `gallery` field.

## 0.1.2

### Added

- Added Info Frame, a compact placeable catalog object with standing and wall-mounted variants.
- Added a dedicated three-dimensional inventory model for Info Frame.

### Changed

- Renamed Player Choice Digest to Info Pad.
- Renamed Player Choice Board to Info Stand.
- Shortened internal Java package and class naming while keeping the public Minecraft namespace as `domestia_player_choice`.
- Removed repeated "Domestia Player Choice" prefixes from player-facing item and block names.
- Info Pad recipe now uses an amethyst shard.
- Info Frame recipe uses an amethyst cluster.
- Info Stand recipe now uses an amethyst block.
- Updated recipe-book unlock criteria to match the corresponding amethyst ingredients.

### Fixed

- Info Stand now drops itself when broken.
- Info Stand now has stronger metal-like block hardness and resistance.
- Improved Info Stand and Info Frame selection bounds to better follow their model geometry.

## 0.1.0-alpha

### Added

- Added Player Choice Digest, a handheld in-game catalog item for browsing server-curated content.
- Added Player Choice Board, a placeable world object that opens the same catalog interface.
- Player Choice Board supports both standing and wall-mounted placement.
- Added hierarchical catalog menus with image pages, text entries, badges, and external links.
- Added built-in fallback catalog content for clients without a server content pack.
- Added server-hosted resource pack support for custom catalog content.
- Added configurable server content pack delivery from the mod config folder.
- Added PBR-ready Player Choice Board textures with normal and specular maps.
