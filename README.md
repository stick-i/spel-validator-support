# SpEL Validator Support - IntelliJ IDEA Plugin

[English](#english) | [中文](#中文)

## 中文

### 简介

SpEL Validator Support 是一个为 [SpEL Validator](https://github.com/stick-i/spel-validator) 框架提供智能开发支持的 IntelliJ IDEA 插件。

SpEL Validator 是一个基于 Spring Expression Language 的 Java 参数校验框架，允许开发者在注解属性中编写 SpEL 表达式进行复杂的参数校验。本插件旨在解决开发者在使用 SpEL Validator 时遇到的痛点：

- IDEA 不识别注解属性中的 SpEL 表达式，缺少语法高亮
- 输入 `#this.` 时无法自动补全当前类的字段
- 无法通过 Ctrl+Click 跳转到字段定义
- 字段重命名时 SpEL 表达式中的引用不会自动更新
- 引用不存在的字段时没有错误提示

### 主要功能

#### 1. SpEL 语言注入
自动识别 SpEL Validator 注解并注入 SpEL 语言支持：
- 语法高亮显示
- SpEL 表达式基础补全（如 `@beanName`、`T(ClassName)`）
- 需要安装 Spring 插件以获得完整的 SpEL 支持

#### 2. 智能字段补全
在 SpEL 表达式中输入 `#this.` 时自动补全：
- 当前类的所有字段（包括私有字段）
- 父类继承的字段
- 嵌套字段访问（如 `#this.user.address.city`）
- 显示字段类型信息

#### 3. 字段引用导航
- **Ctrl+Click 跳转**：点击字段名跳转到字段定义
- **Find Usages**：查找字段在 SpEL 表达式中的所有使用
- 支持嵌套字段的导航

#### 4. 重构支持
- 字段重命名时自动更新 SpEL 表达式中的引用
- 支持重命名预览
- 支持嵌套字段的重命名

#### 5. 错误检查
- 实时检查字段引用的有效性
- 对不存在的字段显示警告
- 鼠标悬停显示错误消息
- 支持嵌套字段的错误检查

### 支持的注解

插件支持以下 SpEL Validator 注解：
- `@SpelValid`
- `@SpelAssert`
- `@SpelNotNull`
- `@SpelNotBlank`
- `@SpelNotEmpty`
- `@SpelNull`
- `@SpelSize`
- `@SpelMin`
- `@SpelMax`
- `@SpelDigits`
- `@SpelFuture`
- `@SpelPast`
- `@SpelFutureOrPresent`
- `@SpelPastOrPresent`

同时支持使用 `@SpelConstraint` 元注解标注的自定义约束注解。

### 系统要求

- IntelliJ IDEA 2023.2 (Ultimate Edition) 或更高版本
- Java 17 或更高版本（开发环境需要 Java 21）
- 需要启用 Spring 插件以获得的 SpEL 语言支持，默认状态下已经启用了

### 安装

#### 方式一：从 JetBrains Marketplace 安装
1. 打开 IntelliJ IDEA
2. 进入 Settings/Preferences → Plugins
3. 搜索 "SpEL Validator Support"
4. 点击 Install
5. 重启 IDEA

#### 方式二：手动安装
1. 从 [Releases](https://github.com/stick-i/spel-validator-support/releases) 下载最新的插件 ZIP 文件
2. 打开 IntelliJ IDEA
3. 进入 Settings/Preferences → Plugins
4. 点击齿轮图标 → Install Plugin from Disk
5. 选择下载的 ZIP 文件
6. 重启 IDEA

### 使用方法

安装插件后，在使用 SpEL Validator 注解时，插件会自动提供智能支持：

```java
@Data
public class UserDto {
    private String userName;
    private Integer age;
    private Address address;
    
    // 输入 #this. 时会自动补全 userName、age、address
    @SpelNotNull(condition = "#this.age != null")
    private String userNameCheck;
    
    // 支持嵌套字段补全：#this.address. 会补全 Address 类的字段
    @SpelAssert(assertTrue = "#this.address.city != null")
    private String addressCheck;
}

@Data
public class Address {
    private String city;
    private String street;
}
```

### 自定义约束注解

如果你创建了自定义的约束注解，需要使用 `@SpelConstraint` 元注解标注，插件才能识别：

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@SpelConstraint  // 添加此注解
public @interface MyCustomConstraint {
    @Language("SpEL")  // 标注需要 SpEL 支持的属性
    String condition() default "";
    
    String message() default "validation failed";
}
```

### 常见问题

#### Q: 为什么没有 SpEL 语法高亮？
A: 请确保已安装 Spring 插件。SpEL 语言支持由 Spring 插件提供。

#### Q: 为什么字段补全不工作？
A: 请检查：
1. 注解是否为 SpEL Validator 的约束注解
2. 光标是否在 `#this.` 之后
3. 项目是否正确配置了 SpEL Validator 依赖

#### Q: 插件是否支持 Kotlin？
A: 目前仅支持 Java。Kotlin 支持计划在未来版本中添加。

### 开发指南

#### 环境要求
- JDK 21
- Gradle 8.x
- IntelliJ IDEA 2023.2+

#### 构建项目

```bash
./gradlew build
```

#### 运行插件（启动带插件的 IDEA 实例）

```bash
./gradlew runIde
```

#### 运行测试

```bash
./gradlew test
```

#### 构建插件发布包

```bash
./gradlew buildPlugin
```

构建产物位于 `build/distributions/` 目录。

### 技术栈

- Java 21
- Gradle 8.x
- IntelliJ Platform SDK 2.5.0
- JUnit 5
- QuickTheories (属性测试)
- AssertJ

### 项目结构

```
src/
├── main/
│   ├── java/cn/sticki/spel/validator/support/
│   │   ├── util/                    # 核心工具类
│   │   │   └── SpelValidatorUtil.java
│   │   ├── injection/               # 语言注入
│   │   │   └── SpelLanguageInjector.java
│   │   ├── completion/              # 代码补全
│   │   │   └── SpelFieldCompletionContributor.java
│   │   ├── reference/               # 引用解析
│   │   │   ├── SpelFieldReferenceContributor.java
│   │   │   └── SpelFieldReference.java
│   │   └── inspection/              # 错误检查
│   │       └── SpelFieldAnnotator.java
│   └── resources/META-INF/
│       ├── plugin.xml               # 插件配置
│       └── pluginIcon.svg           # 插件图标
└── test/
    └── java/cn/sticki/spel/validator/support/
        ├── util/                    # 工具类测试
        ├── injection/               # 语言注入测试
        ├── completion/              # 代码补全测试
        ├── reference/               # 引用解析测试
        ├── inspection/              # 错误检查测试
        └── integration/             # 集成测试
```

### 贡献

欢迎提交 Issue 和 Pull Request！

在提交 PR 之前，请确保：
1. 所有测试通过：`./gradlew test`
2. 代码符合项目风格
3. 添加了必要的测试用例

### 许可证

[Apache License 2.0](LICENSE)

### 相关链接

- [SpEL Validator 框架](https://github.com/stick-i/spel-validator)
- [IntelliJ Platform SDK 文档](https://plugins.jetbrains.com/docs/intellij/welcome.html)

---

## English

### Introduction

SpEL Validator Support is an IntelliJ IDEA plugin that provides intelligent development support for the [SpEL Validator](https://github.com/stick-i/spel-validator) framework.

SpEL Validator is a Java parameter validation framework based on Spring Expression Language, allowing developers to write SpEL expressions in annotation attributes for complex parameter validation. This plugin aims to solve the pain points developers encounter when using SpEL Validator:

- IDEA doesn't recognize SpEL expressions in annotation attributes, lacking syntax highlighting
- No auto-completion for class fields when typing `#this.`
- Cannot Ctrl+Click to jump to field definitions
- Field references in SpEL expressions don't update when renaming fields
- No error hints when referencing non-existent fields

### Features

#### 1. SpEL Language Injection
Automatically recognizes SpEL Validator annotations and injects SpEL language support:
- Syntax highlighting
- Basic SpEL expression completion (e.g., `@beanName`, `T(ClassName)`)
- Requires Spring plugin for full SpEL support

#### 2. Smart Field Completion
Auto-completes when typing `#this.` in SpEL expressions:
- All fields of the current class (including private fields)
- Inherited fields from parent classes
- Nested field access (e.g., `#this.user.address.city`)
- Shows field type information

#### 3. Field Reference Navigation
- **Ctrl+Click Navigation**: Click on field name to jump to field definition
- **Find Usages**: Find all usages of a field in SpEL expressions
- Supports navigation for nested fields

#### 4. Refactoring Support
- Automatically updates references in SpEL expressions when renaming fields
- Supports rename preview
- Supports renaming nested fields

#### 5. Error Checking
- Real-time validation of field references
- Shows red wavy underline for non-existent fields
- Shows error message on hover
- Supports error checking for nested fields

### Supported Annotations

The plugin supports the following SpEL Validator annotations:
- `@SpelValid`
- `@SpelAssert`
- `@SpelNotNull`
- `@SpelNotBlank`
- `@SpelNotEmpty`
- `@SpelNull`
- `@SpelSize`
- `@SpelMin`
- `@SpelMax`
- `@SpelDigits`
- `@SpelFuture`
- `@SpelPast`
- `@SpelFutureOrPresent`
- `@SpelPastOrPresent`

Also supports custom constraint annotations marked with `@SpelConstraint` meta-annotation.

### Requirements

- IntelliJ IDEA 2023.2 or higher
- Java 17 or higher (Java 21 required for development)
- Spring plugin recommended for full SpEL language support

### Installation

#### Option 1: Install from JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to Settings/Preferences → Plugins
3. Search for "SpEL Validator Support"
4. Click Install
5. Restart IDEA

#### Option 2: Manual Installation
1. Download the latest plugin ZIP file from [Releases](https://github.com/stick-i/spel-validator-support/releases)
2. Open IntelliJ IDEA
3. Go to Settings/Preferences → Plugins
4. Click the gear icon → Install Plugin from Disk
5. Select the downloaded ZIP file
6. Restart IDEA

### Usage

After installing the plugin, it will automatically provide intelligent support when using SpEL Validator annotations:

```java
@Data
public class UserDto {
    private String userName;
    private Integer age;
    private Address address;
    
    // Auto-completes userName, age, address when typing #this.
    @SpelNotNull(condition = "#this.age != null")
    private String userNameCheck;
    
    // Supports nested field completion: #this.address. completes Address fields
    @SpelAssert(assertTrue = "#this.address.city != null")
    private String addressCheck;
}

@Data
public class Address {
    private String city;
    private String street;
}
```

### Custom Constraint Annotations

If you create custom constraint annotations, you need to mark them with `@SpelConstraint` meta-annotation for the plugin to recognize:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@SpelConstraint  // Add this annotation
public @interface MyCustomConstraint {
    @Language("SpEL")  // Mark attributes that need SpEL support
    String condition() default "";
    
    String message() default "validation failed";
}
```

### FAQ

#### Q: Why is there no SpEL syntax highlighting?
A: Please make sure the Spring plugin is installed. SpEL language support is provided by the Spring plugin.

#### Q: Why doesn't field completion work?
A: Please check:
1. Is the annotation a SpEL Validator constraint annotation?
2. Is the cursor after `#this.`?
3. Is the project correctly configured with SpEL Validator dependency?

#### Q: Does the plugin support Kotlin?
A: Currently only Java is supported. Kotlin support is planned for future versions.

### Development Guide

#### Requirements
- JDK 21
- Gradle 8.x
- IntelliJ IDEA 2023.2+

#### Build Project

```bash
./gradlew build
```

#### Run Plugin (Start IDEA instance with plugin)

```bash
./gradlew runIde
```

#### Run Tests

```bash
./gradlew test
```

#### Build Plugin Distribution

```bash
./gradlew buildPlugin
```

Build artifacts are located in `build/distributions/` directory.

### Tech Stack

- Java 21
- Gradle 8.x
- IntelliJ Platform SDK 2.5.0
- JUnit 5
- QuickTheories (Property-based testing)
- AssertJ

### Project Structure

```
src/
├── main/
│   ├── java/cn/sticki/spel/validator/support/
│   │   ├── util/                    # Core utilities
│   │   │   └── SpelValidatorUtil.java
│   │   ├── injection/               # Language injection
│   │   │   └── SpelLanguageInjector.java
│   │   ├── completion/              # Code completion
│   │   │   └── SpelFieldCompletionContributor.java
│   │   ├── reference/               # Reference resolution
│   │   │   ├── SpelFieldReferenceContributor.java
│   │   │   └── SpelFieldReference.java
│   │   └── inspection/              # Error checking
│   │       └── SpelFieldAnnotator.java
│   └── resources/META-INF/
│       ├── plugin.xml               # Plugin configuration
│       └── pluginIcon.svg           # Plugin icon
└── test/
    └── java/cn/sticki/spel/validator/support/
        ├── util/                    # Utility tests
        ├── injection/               # Language injection tests
        ├── completion/              # Code completion tests
        ├── reference/               # Reference resolution tests
        ├── inspection/              # Error checking tests
        └── integration/             # Integration tests
```

### Contributing

Issues and Pull Requests are welcome!

Before submitting a PR, please ensure:
1. All tests pass: `./gradlew test`
2. Code follows project style
3. Necessary test cases are added

### License

[Apache License 2.0](LICENSE)

### Related Links

- [SpEL Validator Framework](https://github.com/stick-i/spel-validator)
- [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
