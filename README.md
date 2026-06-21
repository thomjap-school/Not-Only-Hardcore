# DeathBan

A Minecraft Paper plugin that places players in a configurable prison after they die, adds player heads with special effects, protects new players, and supports duels.

## Features

- Prison system on player death
- Configurable jail location and duration
- Drop player heads on death with poison effect on use
- New player protection with resistance effect
- Duel request system with dedicated duel arenas
- `/db` command for administration and duel control

## Commands

- `/db unprison <joueur>` - release a player from prison
- `/db set prison <x|y|z|world|yaw|pitch|duration> <valeur>` - configure prison location and length
- `/db set head <effect|duration|amplifier|drop> <valeur>` - configure player head behavior
- `/db set deathsound <sound|volume|pitch> <valeur>` - configure death sound
- `/db set protection <duration|amplifier> <valeur>` - configure new player protection
- `/db set duel world <nom>` - set duel world name
- `/db set duel delay <secondes>` - set duel return delay
- `/db set duel <1|2|3> <a|b> <x|y|z|yaw|pitch> <valeur>` - configure duel arena positions
- `/db duel <joueur>` - send a duel request
- `/db duel accept <joueur>` - accept a duel request
- `/db duel deny <joueur>` - deny a duel request
- `/db reload` - reload plugin configuration

## Configuration

Default values are stored in `src/main/resources/config.yml` and copied to the plugin config on first run.

Key configuration sections:

- `prison` - jail world, coordinates, yaw/pitch, prison duration in minutes
- `head` - drop chance, potion effect, duration, amplifier
- `death` - death sound, volume, pitch
- `new-player-protection` - protection duration and resistance amplifier for new players
- `duel` - duel world, return delay, and arena zone coordinates

Example from `config.yml`:

```yaml
prison:
  world: Jail
  x: -32.0
  y: 28.0
  z: -74.0
  yaw: 0.0
  pitch: 0.0
  duration-minutes: 2880

head:
  drop-chance-percent: 100.0
  effect: POISON
  duration-seconds: 30
  amplifier: 0

death:
  sound: ENTITY_WITHER_SPAWN
  sound-volume: 1.0
  sound-pitch: 1.0

new-player-protection:
  duration-minutes: 30
  resistance-amplifier: 4

duel:
  world: duel
  return-delay-seconds: 30
  zones:
    1:
      a: {x: 0.0, y: 100.0, z: 0.0, yaw: 0.0, pitch: 0.0}
      b: {x: 10.0, y: 100.0, z: 0.0, yaw: 180.0, pitch: 0.0}
    2:
      a: {x: 0.0, y: 100.0, z: 50.0, yaw: 0.0, pitch: 0.0}
      b: {x: 10.0, y: 100.0, z: 50.0, yaw: 180.0, pitch: 0.0}
    3:
      a: {x: 0.0, y: 100.0, z: 100.0, yaw: 0.0, pitch: 0.0}
      b: {x: 10.0, y: 100.0, z: 100.0, yaw: 180.0, pitch: 0.0}
```

## Build

This project uses Gradle and the Paper API.

Requirements:

- Java 25
- Gradle Wrapper (`./gradlew`)

Build the plugin jar:

```bash
./gradlew build
```

The resulting plugin jar will be located in `build/libs/DeathBan-1.0.jar`.

## Installation

1. Build the plugin using `./gradlew build`
2. Copy the jar from `build/libs/` into your Paper server `plugins/` folder
3. Start or restart the server
4. Configure `config.yml` as needed and use `/db reload`

## Notes

- The plugin directly uses Paper API `26.1.2.build.67-stable`
- Main class: `com.example.deathban.DeathBan`
- Plugin metadata is defined in `src/main/resources/plugin.yml`
