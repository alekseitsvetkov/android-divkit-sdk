# BDUI SDK Specification (v1)

## 1. Общая часть

### 1.1. Назначение

Цель SDK:

1. Встраивать DivKit-виджеты как часть существующего экрана приложения.
2. Обрабатывать navigation action из DivKit JSON.
3. Открывать standalone BDUI-flow с переходами между виджетами.

Базовый поток:

1. Приложение получает DivKit JSON с backend.
2. Приложение создает `BduiView` через SDK и добавляет нативный view в свой контейнер.
3. Приложение вызывает `bduiView.setJson(json)`.
4. Пользователь нажимает кнопку с action `app://nav/v1/...`.
5. SDK перехватывает action через внутренний `BduiDivActionHandler`.
6. SDK выполняет `push/replace/pop`.
7. Новый экран показывает `Loading`, загружает JSON, затем показывает `Content` или `Error` с `Retry`.

### 1.2. Навигационный протокол v1

Формат URI:

```txt
app://nav/v1/{command}?{query}
```

Команды:

1. `push?url=<encoded_https_url>`
2. `replace?url=<encoded_https_url>`
3. `pop?count=<int>` (если не передан, используется `1`)

Семантика:

1. `push` открывает новый BDUI-экран.
2. `replace` заменяет текущий BDUI-экран.
3. `pop` возвращает назад на `count` экранов.

### 1.3. Примеры action в DivKit JSON

`push`:

```json
{
  "log_id": "open_profile",
  "url": "app://nav/v1/push?url=https%3A%2F%2Fwidgets.example.com%2Fprofile.json"
}
```

`replace`:

```json
{
  "log_id": "open_checkout",
  "url": "app://nav/v1/replace?url=https%3A%2F%2Fwidgets.example.com%2Fcheckout.json"
}
```

`pop`:

```json
{
  "log_id": "go_back",
  "url": "app://nav/v1/pop?count=1"
}
```

## 2. Фасад SDK

### 2.1. `BduiSDK`

`BduiSDK` — единственная публичная точка входа SDK.

#### 2.1.1. Публичный API

```kotlin
interface BduiSDK {
    fun initialize(context: BduiContext)
    fun createView(): BduiView
}

typealias BduiContext = Any
```

Правила:

1. `initialize(context)` обязателен и выполняется один раз при старте приложения.
2. `context` нужен SDK для инициализации DivKit.

### 2.2. `BduiView`

`BduiView` — минимальный публичный контракт виджета.

#### 2.2.1. Публичный API

```kotlin
interface BduiView {
    fun setJson(json: String)
}
```

### 2.3. Внутренние сущности SDK (непубличные)

```kotlin
internal sealed interface NavigationCommand {
    data class Push(val widgetUrl: String) : NavigationCommand
    data class Replace(val widgetUrl: String) : NavigationCommand
    data class Pop(val count: Int = 1) : NavigationCommand
}

internal interface ScreenOpener {
    fun open(widgetUrl: String)
    fun replace(widgetUrl: String)
    fun pop(count: Int = 1): Boolean
}

internal interface NavigationCommandParser {
    fun parse(rawUrl: String): NavigationCommand?
}
```

### 2.4. Логика action в `BduiDivActionHandler`

В v1 вся логика обработки action находится внутри `BduiDivActionHandler`.

```kotlin
internal class BduiDivActionHandler(
    private val parser: NavigationCommandParser,
    private val openerProvider: () -> ScreenOpener,
    private val logger: BduiLogger
) : DivActionHandler() {

    override fun handleAction(action: DivAction, view: DivViewFacade): Boolean {
        val rawUrl = action.url?.evaluate(view.expressionResolver)?.toString() ?: return false

        val command = parser.parse(rawUrl)
        if (command == null) {
            logger.warn("bdui_nav_invalid_action", mapOf("rawUrl" to rawUrl))
            return false
        }

        val opener = openerProvider()
        when (command) {
            is NavigationCommand.Push -> opener.open(command.widgetUrl)
            is NavigationCommand.Replace -> opener.replace(command.widgetUrl)
            is NavigationCommand.Pop -> opener.pop(command.count)
        }
        return true
    }
}
```

### 2.5. Модель состояния standalone-экрана

Каждый экран standalone flow обязан иметь state-owner (ViewModel или аналог):

```kotlin
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Content(val json: String) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}

interface ScreenStateOwner {
    val state: StateStream<ScreenUiState>
    fun load(widgetUrl: String)
    fun retry()
}
```

Поведение:

1. При старте экрана `state = Loading`.
2. На успехе загрузки `state = Content(json)`.
3. На ошибке `state = Error(message)`.
4. `retry()` повторяет загрузку и снова проходит через `Loading`.

## 3. Платформы

Единые требования:

1. DivKit и action-handler настраиваются внутри SDK.
2. `BduiDivActionHandler` парсит и исполняет `push/replace/pop`.
3. Standalone flow использует нативный стек платформы.
4. Каждый standalone экран имеет state-owner с `Loading/Content/Error`.

## 3.1 Android

### 3.1.1. Устройство СДК под Android

Компоненты:

1. `BduiSDKAndroid`.
2. `BduiViewAndroid` (реализует `BduiView`, хранит `DivView`).
3. `BduiDivActionHandler` (внутренний action-handler).
4. `AndroidScreenOpener` (внутренний opener).
5. `BduiHostActivity` (одна Activity для standalone flow).
6. `BduiScreenFragment` (каждый standalone экран).
7. `BduiScreenViewModel` (`Loading/Content/Error`).

Ключевое правило standalone flow:

1. Первая навигация открывает `BduiHostActivity`.
2. Все последующие BDUI-экраны внутри этой же Activity открываются фрагментами.

Каркас SDK:

```kotlin
class BduiSDKAndroid : BduiSDK {
    private lateinit var appContext: Context
    private lateinit var divConfiguration: DivConfiguration

    override fun initialize(context: BduiContext) {
        appContext = (context as Context).applicationContext

        val parser = DefaultNavigationCommandParser(
            allowHosts = setOf("widgets.example.com"),
            navPrefix = "app://nav/v1"
        )

        val actionHandler = BduiDivActionHandler(
            parser = parser,
            openerProvider = {
                AndroidScreenOpener(appContext, BduiHostActivity.current())
            },
            logger = AndroidBduiLogger()
        )

        divConfiguration = DivConfiguration.Builder(...)
            .actionHandler(actionHandler)
            .build()
    }

    override fun createView(): BduiView {
        return BduiViewAndroid(divConfiguration)
    }
}
```

`BduiViewAndroid`:

```kotlin
internal class BduiViewAndroid(
    private val divConfiguration: DivConfiguration
) : BduiView {

    private var divView: DivView? = null

    fun asView(context: Context): View {
        if (divView == null) {
            divView = DivView(context, divConfiguration)
        }
        return checkNotNull(divView)
    }

    override fun setJson(json: String) {
        // parse + set data to divView
    }
}
```

`AndroidScreenOpener`:

```kotlin
internal class AndroidScreenOpener(
    private val appContext: Context,
    private val hostActivity: BduiHostActivity?
) : ScreenOpener {

    override fun open(widgetUrl: String) {
        if (hostActivity == null) {
            val intent = BduiHostActivity.newIntent(appContext, widgetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return
        }
        hostActivity.pushScreen(widgetUrl)
    }

    override fun replace(widgetUrl: String) {
        if (hostActivity == null) {
            val intent = BduiHostActivity.newIntent(appContext, widgetUrl)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return
        }
        hostActivity.replaceTopScreen(widgetUrl)
    }

    override fun pop(count: Int): Boolean {
        val host = hostActivity ?: return false
        return host.popScreens(count)
    }
}
```

`BduiScreenViewModel`:

```kotlin
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Content(val json: String) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}

class BduiScreenViewModel(
    private val repository: WidgetRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val widgetUrl: String = checkNotNull(savedStateHandle["widget_url"])
    private val _state = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val state: StateFlow<ScreenUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenUiState.Loading
            runCatching { repository.loadJsonByUrl(widgetUrl) }
                .onSuccess { _state.value = ScreenUiState.Content(it) }
                .onFailure { _state.value = ScreenUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun retry() = load()
}
```

`BduiScreenFragment`:

```kotlin
class BduiScreenFragment : Fragment(R.layout.bdui_screen_fragment) {
    private val sdk get() = (requireActivity().application as App).bduiSdk
    private val vm: BduiScreenViewModel by viewModels()
    private lateinit var bduiView: BduiViewAndroid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<ViewGroup>(R.id.contentContainer)
        bduiView = sdk.createView() as BduiViewAndroid
        container.addView(bduiView.asView(requireContext()))

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collect { state ->
                when (state) {
                    ScreenUiState.Loading -> renderLoading()
                    is ScreenUiState.Content -> {
                        renderContent()
                        bduiView.setJson(state.json)
                    }
                    is ScreenUiState.Error -> renderError(state.message, onRetry = vm::retry)
                }
            }
        }
    }
}
```

Системный back на Android:

1. Обрабатывается в `BduiHostActivity` через `OnBackPressedDispatcher`.
2. Если в back stack есть фрагменты, выполняется `popScreens(1)`.
3. Если это root standalone экран, Activity закрывается.

```kotlin
class BduiHostActivity : AppCompatActivity(R.layout.bdui_host_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val firstUrl = checkNotNull(intent.getStringExtra("widget_url"))
            supportFragmentManager.commit {
                replace(R.id.bduiHostContainer, BduiScreenFragment.newInstance(firstUrl))
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!popScreens(1)) finish()
        }
    }

    fun pushScreen(widgetUrl: String) {
        supportFragmentManager.commit {
            replace(R.id.bduiHostContainer, BduiScreenFragment.newInstance(widgetUrl))
            addToBackStack(widgetUrl)
        }
    }

    fun replaceTopScreen(widgetUrl: String) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        pushScreen(widgetUrl)
    }

    fun popScreens(count: Int): Boolean {
        var left = count.coerceAtLeast(1)
        var didPop = false
        while (left > 0 && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
            left--
            didPop = true
        }
        return didPop
    }

    companion object {
        fun newIntent(context: Context, widgetUrl: String): Intent {
            return Intent(context, BduiHostActivity::class.java)
                .putExtra("widget_url", widgetUrl)
        }

        fun current(): BduiHostActivity? {
            // activity tracker implementation
            return null
        }
    }
}
```

### 3.1.2. Примеры использования СДК под Android в целевом приложении

Инициализация SDK:

```kotlin
class App : Application() {
    lateinit var bduiSdk: BduiSDK

    override fun onCreate() {
        super.onCreate()
        bduiSdk = BduiSDKAndroid().apply {
            initialize(this@App)
        }
    }
}
```

Embedded виджет в обычном экране:

```kotlin
class HomeFragment : Fragment(R.layout.home_fragment) {
    private val sdk get() = (requireActivity().application as App).bduiSdk
    private lateinit var bduiView: BduiViewAndroid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<ViewGroup>(R.id.widgetContainer)

        bduiView = sdk.createView() as BduiViewAndroid
        container.addView(bduiView.asView(requireContext()))

        viewLifecycleOwner.lifecycleScope.launch {
            val json = backendApi.loadWidgetJson("home")
            bduiView.setJson(json)
        }
    }
}
```

Клик по `app://nav/v1/push|replace|pop` обрабатывается внутри SDK:

1. DivKit вызывает `BduiDivActionHandler`.
2. Handler парсит URI.
3. Handler выбирает `AndroidScreenOpener` и выполняет команду.

## 3.2 iOS

### 3.2.1. Устройство СДК под iOS

Компоненты:

1. `BduiSDKiOS`.
2. `BduiViewiOS`.
3. Внутренний `DivUrlHandler`.
4. Внутренний `IOSScreenOpener`.
5. `BduiHostNavigationController` для standalone flow.
6. `BduiScreenViewController`.
7. `BduiScreenViewModel`.

Каркас SDK:

```swift
final class BduiSDKiOS: BduiSDK {
    private weak var appRoot: UIViewController?
    private var divContext: DivKitContext!

    func initialize(context: BduiContext) {
        appRoot = context as? UIViewController

        let parser = DefaultNavigationCommandParser(
            allowHosts: ["widgets.example.com"],
            navPrefix: "app://nav/v1"
        )

        let urlHandler = BduiDivActionHandlerIOS(
            parser: parser,
            openerProvider: { [weak self] in
                IOSScreenOpener(root: self?.appRoot)
            },
            logger: IOSBduiLogger()
        )

        divContext = DivKitContext(urlHandler: urlHandler)
    }

    func createView() -> BduiView {
        BduiViewiOS(divContext: divContext)
    }
}
```

`BduiDivActionHandlerIOS` (логика внутри handler):

```swift
final class BduiDivActionHandlerIOS: DivUrlHandler {
    private let parser: NavigationCommandParser
    private let openerProvider: () -> ScreenOpener
    private let logger: BduiLogger

    init(parser: NavigationCommandParser, openerProvider: @escaping () -> ScreenOpener, logger: BduiLogger) {
        self.parser = parser
        self.openerProvider = openerProvider
        self.logger = logger
    }

    func handle(_ url: URL, info: DivActionInfo) -> Bool {
        let rawUrl = url.absoluteString
        guard let command = parser.parse(rawUrl: rawUrl) else {
            logger.warn("bdui_nav_invalid_action", ["rawUrl": rawUrl])
            return false
        }

        let opener = openerProvider()
        switch command {
        case .push(let widgetUrl): opener.open(widgetUrl: widgetUrl)
        case .replace(let widgetUrl): opener.replace(widgetUrl: widgetUrl)
        case .pop(let count): _ = opener.pop(count: count)
        }
        return true
    }
}
```

`BduiScreenViewModel`:

```swift
@MainActor
final class BduiScreenViewModel: ObservableObject {
    enum State {
        case loading
        case content(String)
        case error(String)
    }

    @Published private(set) var state: State = .loading

    private let widgetUrl: String
    private let repository: WidgetRepository

    init(widgetUrl: String, repository: WidgetRepository) {
        self.widgetUrl = widgetUrl
        self.repository = repository
    }

    func load() {
        state = .loading
        Task {
            do {
                let json = try await repository.loadJson(url: widgetUrl)
                state = .content(json)
            } catch {
                state = .error(error.localizedDescription)
            }
        }
    }

    func retry() {
        load()
    }
}
```

Системный back на iOS:

1. `UINavigationController` back button.
2. interactive pop gesture.
3. На root стандартная логика контейнера (`dismiss`/`pop`).

### 3.2.2. Примеры использования СДК под iOS в целевом приложении

Инициализация SDK:

```swift
final class AppCompositionRoot {
    static let shared = AppCompositionRoot()
    let bduiSdk: BduiSDK

    private init() {
        let sdk = BduiSDKiOS()
        sdk.initialize(context: UIApplication.shared.topViewController as Any)
        bduiSdk = sdk
    }
}
```

Embedded виджет:

```swift
final class HomeViewController: UIViewController {
    private let sdk = AppCompositionRoot.shared.bduiSdk
    private lazy var bduiView = sdk.createView()

    override func viewDidLoad() {
        super.viewDidLoad()

        if let view = (bduiView as? BduiViewiOS)?.uiView {
            widgetContainer.addSubview(view)
            view.frame = widgetContainer.bounds
        }

        Task {
            let json = try await backendApi.loadWidgetJson(id: "home")
            bduiView.setJson(json: json)
        }
    }
}
```

Клик по action:

1. DivKit дергает `BduiDivActionHandlerIOS`.
2. Handler парсит URI и вызывает внутренний `IOSScreenOpener`.
3. Standalone flow живет внутри `BduiHostNavigationController`.
