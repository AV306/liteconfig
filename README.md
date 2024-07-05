# LiteConfig

[![](https://jitpack.io/v/AV306/liteconfig.svg)](https://jitpack.io/#AV306/liteconfig)

<sup>Honestly, you could just copy/paste [ConfigManager.java](https://github.com/AV306/liteconfig/blob/main/src/main/java/me/av306/liteconfig/ConfigManager.java) into your project; it's literally just a single class.</sup>

A very small and lightweight Java configuration manager.

## Features

- Smol - only two classes, artifact size 5 kB
- Simple - just do a `new ConfigManager( [...] )` and it handles everything for you
- No dependencies!

## How to use

1. Create (public, optionally static) fields to hold configurable values in a clas of your choice (e.g. `MainClass`)
```java
public class MainClass [
  public int settingA = 4; // Default value: 4
  public booelan settingB = false; // Default value: false
  ...
```
2. Create a template config file in your resource folder (`src/main/resources/` for Gradle)
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
      this.getClass(), // or MainClass.class if static context
      this // or null if static fields are used
    );
    ...
```
3. The constructor handles the initial creation and reading of the config file
4. To update configs, call `ConfigManager#readConfigFile()`
5. To save configs if they were changed during runtime, call `ConfigManager#saveConfigFile()`

That's it :D

### License: MIT
