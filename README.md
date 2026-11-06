# Minimal TV

Launcher minimalista para Android TV 5.1+ (probado para Philips 55PUG7100/77).

- Fondo oscuro con degradé, saludo, hora y fecha arriba.
- Apps en tarjetas grandes con efecto de foco tipo Apple TV.
- **OK** abre la app. **Mantener OK** la agrega o quita de *Favoritos*.
- Tarjeta *Ajustes* al final para entrar a la configuración de la tele.

## Conseguir el APK (sin instalar nada en la compu)

1. Creá una cuenta en github.com y un repositorio nuevo, por ejemplo `MinimalTV`.
2. En el repositorio: **Add file → Upload files** y subí *todo el contenido* de esta carpeta, incluida la carpeta `.github`.
   Si la web no deja subir `.github`, creá el archivo a mano: **Add file → Create new file**, nombre `.github/workflows/build.yml`, y pegá el contenido.
3. Andá a la pestaña **Actions**. Se va a compilar solo (unos 3–5 minutos).
4. Cuando quede en verde, entrá a la compilación y bajá **MinimalTV-apk** (viene en un .zip con `app-debug.apk` adentro).

## Instalarlo en la tele

1. En la tele: Ajustes → Seguridad → activar **Orígenes desconocidos**.
2. Copiá `app-debug.apk` a un pendrive, enchufalo y abrilo con un explorador de archivos.
   (O subilo a Google Drive y bajalo con la app *Downloader*.)
3. Abrí **Minimal TV**. Si al apretar Home la tele pregunta qué inicio usar, elegí Minimal TV → *Siempre*.

Si Home sigue abriendo el menú de Philips, Minimal TV igual funciona abriéndolo desde las apps.
