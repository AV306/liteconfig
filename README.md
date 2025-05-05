# LiteConfig

[![](https://jitpack.io/v/AV306/liteconfig.svg)](https://jitpack.io/#AV306/liteconfig)

<sup>Honestly, you could just copy/paste [ConfigManager.java](https://github.com/AV306/liteconfig/blob/main/src/main/java/me/av306/liteconfig/ConfigManager.java) into your project - it's literally just a single class</sup>

A very small and lightweight Java configuration manager.

## Features

- Smol - only one class (+1 interface, technically)
- Simple - just do a `new ConfigManager( [...] )` and it handles everything for you
- No dependencies!

## How to use

1. Create (public, optionally static) fields to hold configurable values in a class of your choice (e.g. `MainClass`)
```java
public class MainClass {
  public int settingA = 4;            // Default value: 4
  public boolean settingB = false;    // Default value: false
  ...
```
2. (optional) Create a template config file (to be embedded inside the JAR) in your resource folder (`src/main/resources/` for Gradle)
3. Initialise a new ConfigManager instance in your entrypoint
```java
import me.av306.liteconfig.ConfigManager;
...
public class MainClass {
  ...
  private ConfigManager cm;
  ...
  public void entrypoint() {
    ...
    cm = new ConfigManager(
      "Test App", // Name of program, for logging
      Path.of( "path/to/config/file/directory/" ),
      "testapp_config.properties", // Name of config file (with extension), should be the same as the one you created in step 2
      this.getClass(), // or MainClass.class if static fields are used
      this // or null if static fields are used
    );
    ...
```
3. The constructor handles the initial creation and reading of the config file
4. To update configs, call `ConfigManager#readConfigFile()`
5. To save configs if they were changed during runtime, call `ConfigManager#saveConfigFile()`

That's it!

## Note

You have to provide a default config file for ConfigManager. *Technically*, it could create one by inferring from the fields in the configurable class, but then we would need annotations and stuff and that's way too complex for this project.

If you need those features, try one of the better config libraries out there, like [CompleteConfig](https://github.com/Lortseam/CompleteConfig) (for Minecraft).

### License: MIT
