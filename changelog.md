## v0.0.3
- added bStats

## v0.0.2
- fixed: teleportation in the same world getting cancelled when worlds are full (feature: world player limit)
- fixed: smart removal entity whitelist not working (due to case mismatch when reading it)
- added: config option for making mobs invulnerable while their AI is disabled due to no players nearby (`mob_ai.disable_ai_when_no_players_nearby.invulnerable`)
- added: `{item_name}` placeholder for the `hologram_format` config option
- changed: the previous `{item}` placeholder for the `hologram_format` config option to `{item_type}`