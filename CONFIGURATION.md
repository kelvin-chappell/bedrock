# Configuration Guide

This project uses **PureConfig** to manage configuration through HOCON (Human-Optimized Config Object Notation) format, with environment variables loaded from a `.env` file.

## Configuration Architecture

### Files

1. **`src/main/resources/application.conf`** - Main HOCON configuration file with defaults
2. **`.env`** - Environment variables file (not committed to Git)
3. **`.env.example`** - Template for environment variables (committed to Git)

### How It Works

1. The application loads the `.env` file at startup (if it exists)
2. Environment variables from `.env` are made available to the application
3. PureConfig reads `application.conf` and substitutes environment variables using `${?VAR_NAME}` syntax
4. Configuration is parsed into type-safe Scala case classes

## Setting Up Configuration

### 1. Create your `.env` file

Copy the example file:

```bash
cp .env.example .env
```

## Overriding Configuration

Configuration can be overridden in multiple ways (in order of precedence):

1. **Environment variables** - Set directly in your shell or `.env` file
2. **System properties** - Pass via `-Dkey=value` to the JVM
3. **application.conf** - Default values in the HOCON file

## HOCON Syntax Features

### Environment Variable Substitution

```hocon
# Required variable (fails if not set)
api-key = ${API_KEY}

# Optional variable (uses default if not set)
region = "us-east-1"
region = ${?AWS_REGION}
```

## Benefits of This Approach

1. **Type Safety**: Configuration is parsed into strongly-typed case classes
2. **Validation**: PureConfig validates configuration at startup
3. **Environment Separation**: Easy to maintain different configs per environment
4. **Security**: Sensitive data in `.env` is never committed
5. **Readability**: HOCON is more human-friendly than JSON or properties files
6. **Flexibility**: Multiple override mechanisms for different deployment scenarios
