# Triangle OS Workspace

Рабочая среда для сборки кастомной Treble GSI прошивки на базе AOSP 15
и двух собственных приложений: TriangleSettings и TriangleLauncher.

## Структура каталогов

- `src/TriangleSettings/` — исходники системных настроек (`com.android.settings`), Kotlin + Compose.
- `src/TriangleLauncher/` — исходники лаунчера (`org.triangle.launcher`), Kotlin + Compose.
- `tools/` — рабочие утилиты автоматизации (подпись системных APK).
- `tools/legacy/` — исторические одноразовые скрипты (не использовать, оставлены для истории).
- `bootanim/` — загрузочная анимация и Magisk-модуль.
- `docs/screenshots/` — скриншоты интерфейса.
- `keys/` — ключи для подписи системных приложений (AOSP test keys + ROM cert).
- `custom_apps/` — собранные APK, готовые к установке (в git не попадают).
- `base_image/` — исходный образ GSI (в git не попадает).
- `output/` — итоговые собранные образы прошивки (в git не попадает).
- `reference/` — распакованные системные APK и дампы устройства для сверки (в git не попадает).

## Сборка приложений

```powershell
cd src\TriangleSettings
.\gradlew.bat assembleRelease
```

Подпись системного APK платформенным ключом:

```powershell
powershell -File tools\sign_system_app.ps1
```

## Подпись

Приложения подписываются `keys/platform.pk8` и `keys/platform.x509.pem`,
чтобы получить системные разрешения (`WRITE_SECURE_SETTINGS` и др.)
и заменить штатный `Settings`.

## Известный технический долг

Полный список нереализованных и оценочных мест — в [`docs/TECH_DEBT.md`](docs/TECH_DEBT.md).
Документ обновляется по мере закрытия пунктов.
