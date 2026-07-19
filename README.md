# DexLauncher — Fase 1

Launcher personalizable estilo Windows/Samsung DeX para Android.

## Qué hace esta fase 1
- Se puede poner como launcher predeterminado (pantalla de Inicio del celular)
- Grid de iconos cuadrados (estilo Windows, no burbujas redondas)
- Botón "Inicio" abajo a la izquierda (logo tipo Windows) que abre menú con buscador
- Fondo de escritorio personalizable (elegís una imagen de la galería)
- Barra inferior tipo taskbar con reloj

## Cómo subir esto a GitHub desde Termux

1. Instalar lo necesario:
```
pkg update && pkg upgrade
pkg install git
```

2. Crear el repositorio vacío en github.com desde el navegador del cel (botón "New repository"). No lo inicialices con README.

3. Generar un token de acceso (GitHub no deja usar la contraseña normal por git):
   - Entrá a github.com → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token
   - Marcá el permiso "repo" y copiá el token (empieza con `ghp_...`)

4. En Termux, dentro de la carpeta del proyecto (`DexLauncher`):
```
cd DexLauncher
git init
git add .
git commit -m "Fase 1: launcher base"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
git push -u origin main
```
Cuando pida usuario y contraseña: usuario = tu usuario de GitHub, contraseña = el token `ghp_...` (no tu password real).

## Cómo compilar el APK
- **Opción A (Termux):** instalar `openjdk-17`, el Android SDK command-line tools, y correr `./gradlew assembleDebug`. Es pesado, puede tardar bastante la primera vez.
- **Opción B (Android Studio en PC):** abrir la carpeta del proyecto → Build → Build APK(s). Mucho más simple.

El APK generado va a estar en `app/build/outputs/apk/debug/app-debug.apk`.

## Cómo instalarlo
1. Copiar el APK al celular (si se compiló en PC)
2. Instalar el APK (activar "orígenes desconocidos" si lo pide)
3. Apretar el botón Inicio del celular → elegir "DexLauncher" → "Siempre"

## Próximas fases (no incluidas todavía)
- Fase 2: barra de tareas con apps abiertas
- Fase 3: ventanas flotantes/redimensionables
- Fase 4: zoom de UI simulando cambio de resolución (sin root no se puede cambiar la resolución real del sistema)
