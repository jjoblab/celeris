# celeris

**celeris** (Latin: "swift, quick") is an embeddable code editor for
Android: a library any host app integrates to get an editing surface
comparable to a reference development environment — multi-cursor
editing, syntax highlighting, folds, search & replace, completion,
refactorings, ghost text — designed **touch-first**, offline-first,
and frugal with memory and battery.

The engine core has **no Android dependency**: the guarantee is
enforced in code — every pure module carries an architecture test
that forbids any `android.*` dependency. The same core powers the
Android Canvas view and a command-line executable.

> 🇫🇷 The full documentation lives in French in
> [README.md](README.md) and [USAGE.md](USAGE.md). This page carries
> the quick start in English for the international JitPack audience.

## Quick start (5 minutes)

> **Prerequisites** — Android Gradle Plugin **9.5.1** (the version
> exercised by the consumer sample in CI), **JDK 17** to build,
> `minSdk 26`. Nothing else: no AndroidX dependency, no
> desugaring. **If and only if** you embed language servers
> (`lsp-protocole`): enable library desugaring and keep the embedded
> R8 rules (they travel inside the artifact; R8 picks them up).

**1. The JitPack repository** — in `settings.gradle.kts`, under
`dependencyResolutionManagement` (compatible with
`FAIL_ON_PROJECT_REPOS`):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Groovy flavor (`settings.gradle`):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**2. The dependency** — in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.jjoblab.celeris:celeris:<tag>")
}
```

`<tag>` is the published tag of the repository (see the *Releases*
page); the single `celeris` dependency carries the whole editor —
view, widgets, host facade, grammars.

**3. The XML layout** — in your layout:

```xml
<FrameLayout
    android:id="@+id/scene_edition"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <jo.celeris.android.view.EditorView
        android:id="@+id/editeur"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <jo.celeris.android.widgets.PopupLayer
        android:id="@+id/couche_popups"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
```

**4. The facade** — four lines in your activity:

```java
import jo.celeris.android.host.CelerisEditor;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.syntax.grammars.PackGrammars;

// once per process: the shared loader for the thirty-two languages
LanguageKit languages = LanguageKit.builder(LanguageKit.sharedDaemon())
        .grammarFactory(PackGrammars.INSTANCE)
        .builtinPacks()
        .build();

// per scene: the whole editor — session, detected language, popups,
// smart typing, single-source settings, release
CelerisEditor editor = CelerisEditor.attach(
        findViewById(R.id.editeur), findViewById(R.id.couche_popups), languages);
editor.open("Main.java", text);                  // detected language + highlighting
editor.settings().setTheme(EditorThemes.SOMBRE); // view AND popups, one call
editor.close();                                  // everything is released
```

## Modules

The single `celeris` aggregator covers the common case. If you prefer
picking modules one by one, `celeris-bom` aligns the eleven published
modules (`core-text`, `core-syntax`, `core-syntax-grammars`,
`core-language`, `core-intelligence`, `core-lsp`, `core-ai`,
`android-view`, `android-widgets`, `android-host`, `lsp-protocole`
— the latter optional, for embedded language servers only). The `cli`
and the `sample` example app are plain applications, never published.

## Where to go next

- [README.md](README.md) — the project readme (French): state of the
  project, architecture pointers, roadmap;
- [USAGE.md](USAGE.md) — the complete integration guide (French):
  coordinates (§ 3), mounting (§ 4), the text model (§ 5), language
  registry (§ 6), language servers (§ 8), popups (§ 9);
- [sample/](sample/README.md) — the example app, the short path
  through the facade above (French readme; never published);
- [samples/consumer-smoke/](samples/consumer-smoke/README.md) — the
  mechanical integration proof that knows only the facade above.
