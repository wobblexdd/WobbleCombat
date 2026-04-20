# WCombatLog

Production-ready combat tag plugin for Paper servers.

WCombatLog prevents players from escaping combat abuse by logging out, teleporting away, or bypassing PvP pressure with restricted actions during an active fight. The plugin is built for servers that want clear combat state, admin control, and extensibility for future ecosystem integrations.

## Features

- Combat tag with configurable duration
- Action bar combat timer
- Optional BossBar, titles, and sounds
- Combat log punishment modes:
  - `NONE`
  - `KILL`
  - `STRIP`
  - `COMMANDS`
  - `HYBRID`
- Staff notifications for combat logging
- Customizable death and combat messages
- Blocked commands during combat
- Blacklist or whitelist command restriction mode
- Namespaced command protection
  - example: `/essentials:home`
- Disabled worlds support
- Crystal damage support
- Improved indirect damage attribution:
  - projectiles
  - TNT
  - tameable mobs
  - tracked crystal owner
- Optional anti-abuse restrictions:
  - ender pearl
  - chorus fruit
  - portals
  - elytra
- Bypass permissions for staff/systems
- PlaceholderAPI support
- Native expansion placeholders
- Persistent combat history
  - YAML
  - SQLite
- Debug and admin commands
- Public API events for plugin integrations

## Commands

### Player / Admin
- `/wobblecombat reload`
- `/wobblecombat status [player]`
- `/wobblecombat history [player]`
- `/wobblecombat debug [player]`
- `/wobblecombat clear <player>`
- `/wobblecombat forcetag <attacker> <victim>`
- `/wobblecombat savehistory`

### Aliases
- `/wc`

## Permissions

- `wobblecombat.reload` — reload configuration
- `wobblecombat.status` — view combat status
- `wobblecombat.debug` — view debug data
- `wobblecombat.admin` — admin-only controls
- `wobblecombat.notify` — receive staff combat-log notifications
- `wobblecombat.bypass` — bypass combat restrictions

Compatibility alias retained:
- `donutcombat.reload`

## PlaceholderAPI

Included placeholders:

- `%wobblecombat_player%`
- `%wobblecombat_world%`
- `%wobblecombat_time%`
- `%wobblecombat_attacker%`
- `%wobblecombat_status%`

## Storage

Combat history storage modes:

- `YAML`
- `SQLITE`

Example:

```yml
settings:
  history-storage:
    enabled: true
    type: SQLITE
    file: histories.yml
    sqlite-file: histories.db
    autosave-seconds: 30
```

## API Events

Available Bukkit events:

- `WCombatLogTagEvent`
- `WCombatLogUntagEvent`
- `WCombatLogLogEvent`

## Build

```bash
mvn clean package
```

Output:

```bash
target/WCombatLog-1.0.0.jar
```

## Requirements

- Java 21
- Paper 1.21+

## Installation

1. Build the project with Maven
2. Put the jar into `plugins/`
3. Start the server once
4. Configure `config.yml` and `messages.yml`
5. Restart or use `/wc reload`

## Release Notes Summary

WCombatLog 1.0.0 is the first stable release of the plugin. It includes production-focused combat tagging, punishments, storage backends, PlaceholderAPI integration, admin tooling, and anti-abuse protections intended for serious survival and PvP servers.

## License

Add your license section here if you plan to publish the repository publicly.
