# Preparación de Android release

**Estado:** guía futura. No se generó APK release ni keystore.

## URL de API y HTTPS

El build obtiene BuildConfig.BASE_URL de la propiedad Gradle ricetta.releaseBaseUrl. Definirla solo cuando exista dominio real y FastAPI opere con TLS válido:

    ricetta.releaseBaseUrl=https://api.<dominio-controlado>/

No guardar una URL ficticia. Release exige HTTPS, no debe usar 127.0.0.1, 10.0.2.2 ni HTTP. La propiedad de debug queda separada y se conserva. El Manifest deshabilita cleartext. No desactivar validación TLS ni incluir JWT_SECRET_KEY en APK; ese secreto es exclusivo del servidor.

## Firma

Cuando el API HTTPS esté listo:

1. Android Studio: Build > Generate Signed Bundle / APK.
2. Crear el keystore durante el flujo autorizado y guardarlo fuera del workspace/Git, con respaldo seguro.
3. Elegir variante release, firmar y generar APK.
4. Revisar firma, URL compilada y ausencia de secretos antes de instalar.
5. No compartir keystore con la APK; conservarlo para futuras actualizaciones.

No crear keystore ni compilar APK final en etapa 8.

## Checklist teléfono físico

- Instalar y abrir sin Android Studio ni ADB.
- Ejecutar por datos móviles fuera de la red del desarrollador.
- Probar registro, correo duplicado, login, restauración de sesión y logout.
- Probar listado, detalle, creación, edición y eliminación confirmada.
- Verificar errores, persistencia al reiniciar FastAPI/EC2 y certificado HTTPS.
- Desconectar ADB/computadora y repetir recorrido principal.
- Registrar dispositivo, versión Android, fecha, URL y evidencia para la demostración académica.

Pendiente de ejecutar después del despliegue autorizado.
