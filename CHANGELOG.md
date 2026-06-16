# Changelog

## Released

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