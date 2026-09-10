# IBooks

IntelliJ IDEA 阅读插件。用 **Java 17** + **epublib-core 3.1** 在 IDE 里打开、阅读、生成 EPUB。

IBooks is an IntelliJ IDEA plugin: an EPUB reader and generator built on `nl.siegmann.epublib:epublib-core`.

## 功能

- 工具窗口 **IBooks**（右侧）：封面信息、嵌套目录、章节正文
- `Tools → IBooks → Open EPUB...`（`Ctrl+Alt+O`）或把 `.epub` 拖进窗口
- 纸页 / 亚麻 / 夜间 / 墨色四种阅读主题，字号与行高可调
- 书库、阅读进度、书签，启动时恢复上次阅读
- `Tools → IBooks → Create EPUB from Files...`：把项目里的 HTML / Markdown 打成 EPUB（`EpubWriter`）
- 中 / 英界面（`messages/IBooksBundle*.properties`）

## 工程结构

```
IBooks/
├── build.gradle.kts              # IntelliJ Platform Gradle Plugin 2.x + epublib-core
├── settings.gradle.kts
├── gradle.properties
├── src/main/java/com/ibooks/
│   ├── IBooksToolWindowFactory.java
│   ├── actions/                  # 打开 / 生成 / 章节 / 书签 / 主题
│   ├── epub/
│   │   ├── EpubParser.java       # EpubReader
│   │   ├── EpubGenerator.java    # EpubWriter
│   │   ├── HtmlRewriter.java
│   │   └── SimpleMarkdown.java
│   ├── service/                  # 书库 / 书签 / 进度（PersistentStateComponent）
│   ├── settings/                 # Settings → Tools → IBooks
│   └── ui/                       # Swing 阅读器
└── src/main/resources/META-INF/plugin.xml
```

## 环境

| 项 | 版本 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.10.2 |
| IntelliJ Platform | 2024.2.4 Community（`sinceBuild 232` → `untilBuild 253.*`） |
| epublib-core | 3.1 |

## 在 IDEA 里打开

1. **File → Open** 选中本目录 `IBooks/`
2. 信任 Gradle 项目，等待索引
3. 若没有 Gradle Wrapper JAR：IDEA 会用自带 Gradle；或在终端执行  
   `gradle wrapper --gradle-version 8.10.2`
4. 运行配置 **Run IDE with Plugin**（即 Gradle 任务 `runIde`）
5. 沙箱 IDEA 启动后：**View → Tool Windows → IBooks**，或 **Tools → IBooks → Open EPUB...**

命令行：

```bash
./gradlew runIde          # 启动带插件的 IDE
./gradlew buildPlugin     # 产出 build/distributions/IBooks-1.0.0.zip
./gradlew test
```

把 `build/distributions/IBooks-1.0.0.zip` 拖到 **Settings → Plugins → ⚙️ → Install Plugin from Disk** 即可安装到本机 IDEA。

## epublib-core

解析：

```java
Book book = new EpubReader().readEpub(Files.newInputStream(path));
String title = book.getMetadata().getTitles().get(0);
book.getSpine().getSpineReferences();
book.getTableOfContents().getTocReferences();
```

生成：

```java
Book book = new Book();
book.getMetadata().addTitle("My Book");
book.getMetadata().addAuthor(new Author("Jane", "Doe"));
book.addSection("Chapter 1", new Resource(xhtmlBytes, "chapter-001.xhtml"));
new EpubWriter().write(book, Files.newOutputStream(out));
```

依赖仓库：Maven Central 不可用时走 `https://github.com/psiegman/mvn-repo/raw/master/releases` 与 JitPack（见 `build.gradle.kts`）。slf4j 从 epublib 中排除，改用 IDE 自带实现。

## 快捷键

| 动作 | Windows / Linux | macOS |
| --- | --- | --- |
| 打开 EPUB | Ctrl+Alt+O | ⌘⌥O |
| 上一章 / 下一章 | Ctrl+Alt+← / → | ⌘⌥← / → |
| 书签 | Ctrl+Alt+B | ⌘⌥B |

## 许可

插件源码 MIT。`epublib-core` 为 LGPL，随发行包以依赖形式提供，请遵守其条款。
