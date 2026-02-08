# Contributing to Scribes' Lectern

Thank you for your interest in contributing to this project! This document provides guidelines and best practices for development.

## Development Setup

1. **Clone the repository**
   ```sh
   git clone <repository-url>
   cd scribes_lectern
   ```

2. **Install dependencies**
   - Java 25 or higher
   - Maven 3.x
   - Place `HytaleServer.jar` in the parent directory

3. **Configure environment**
   ```sh
   cp .env.example .env
   # Edit .env with your server details
   ```

## Development Guidelines

### Code Style
- Follow standard Java conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise

### Hytale-Specific Patterns
- **Diagnostics First**: When debugging, add logging (`System.out` or plugin logger) before rewriting code
- **ECS Architecture**: Follow Entity Component System patterns as documented at hytalemodding.dev
- **Asset Awareness**: Verify assets exist in the project before referencing them
- **Baseline First**: Implement simple, working versions before adding complexity

### Asset Path Conventions
- Use logical namespacing: `my_mod:path/to/resource`
- Verify physical file paths match logical URIs
- Document the mapping between physical and logical paths

## Building and Testing

### Local Build
```sh
mvn clean package
```

### Testing on Server
1. Build the plugin
2. Deploy using `./deploy.sh`
3. Monitor server logs for errors
4. Test in-game functionality

### Configuration Testing
Test configuration changes relevant to this project once they are implemented.

## Pull Request Process

1. **Create a feature branch**
   ```sh
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Write clear, focused commits
   - Test thoroughly before committing
   - Update documentation if needed

3. **Submit PR**
   - Provide a clear description of changes
   - Reference any related issues
   - Ensure code builds successfully
   - Include testing steps

4. **Code Review**
   - Address feedback promptly
   - Keep discussions focused and professional
   - Be open to suggestions

## Reporting Issues

When reporting bugs, include:
- Hytale server version
- Plugin version
- Steps to reproduce
- Expected vs actual behavior
- Relevant log output
- Server configuration

## Questions?

For questions about:
- **Hytale modding**: Check hytalemodding.dev
- **This plugin**: Open an issue
- **Development setup**: See README.md

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
