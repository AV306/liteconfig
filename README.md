# LiteConfig

[![](https://jitpack.io/v/AV306/liteconfig.svg)](https://jitpack.io/#AV306/liteconfig)

A small and lightweight Java configuration system. It directly bridges the configuration file and the class with the configuration values, no manual type-guessing or string parsing needed!

## Features

- Small -- (pending tests)
- Simple -- instantiate, deserialise, and you're good to go!
- No extra setup -- configuration files are automatically generated from your configuration class
- Only one external dependency -- SLF4J (you probably have this already)

## How to use

(Check out [the tests](https://github.com/AV306/liteconfig/blob/main/src/test/java/me/av306/liteconfig/tests/ConfigManagerTest.java) for some examples!)

(TODO)

## Note

LiteConfig does *not* support multiple `ConfigManager`s using the same configuration file. It won't stop you, but there are no provisions for concurrency safety.

### License: MIT
