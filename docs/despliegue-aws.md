# Preparación para desplegar Ricetta en AWS

**Estado:** propuesta técnica únicamente. No se han creado recursos, configurado una cuenta, comprado un dominio ni desplegado la API.

## Arquitectura propuesta

- Instancia Linux pequeña de Amazon Lightsail para una demostración académica.
- FastAPI/Uvicorn escuchando únicamente en 127.0.0.1:8000.
- Caddy o Nginx como proxy inverso; solo se publican HTTP (80, para redirección/ACME si se usa) y HTTPS (443). El puerto interno de Uvicorn permanece cerrado al exterior.
- IP estática asociada a la instancia y un dominio propio con DNS apuntando a esa IP.
- TLS válido administrado por Caddy o configurado en el proxy. La app release consumirá únicamente la URL HTTPS.
- SQLite en disco persistente de la instancia, con permisos restringidos, respaldo periódico fuera de la instancia y una prueba documentada de recuperación.

Esta propuesta es adecuada para una demo de bajo tráfico. SQLite en un solo servidor no ofrece alta disponibilidad ni es una solución multiinstancia. Si el uso exige escalabilidad, recuperación avanzada o concurrencia sostenida, revisar una base administrada antes de ampliar el despliegue.

## Requisitos previos

- Cuenta AWS habilitada, MFA y método de pago; revisar presupuesto y alertas de facturación.
- Región y plan Linux elegidos según disponibilidad, memoria, transferencia y latencia.
- Dominio registrado y control de su DNS para validar TLS.
- Clave SSH protegida fuera del repositorio.
- Revisión de variables requeridas por backend/app/config.py, dependencias fijadas en backend/requirements.txt y ruta SQLite persistente.
- Una copia de seguridad de cualquier base de datos que se vaya a conservar. No copiar ni subir la base local de desarrollo sin una decisión explícita sobre sus datos.

## Configuración segura prevista

- Generar un JWT_SECRET_KEY aleatorio y privado (al menos 32 bytes según la configuración actual); guardarlo en un archivo de entorno con permisos de propietario o en un gestor de secretos. Nunca incluirlo en Git, comandos compartidos, APK o logs.
- Configurar DATABASE_URL con una ruta SQLite absoluta persistente y ACCESS_TOKEN_EXPIRE_MINUTES según la política acordada.
- Ejecutar Uvicorn como usuario de servicio sin privilegios mediante systemd. Guardar código y entorno virtual bajo un directorio de servicio con permisos mínimos.
- Configurar el firewall de Lightsail y el firewall del sistema operativo con SSH limitado a IPs administrativas cuando sea viable, y 80/443 públicos. No abrir 8000 ni permitir acceso HTTP directo a SQLite.
- El proxy debe terminar TLS con certificado válido y reenviar a Uvicorn por loopback. No desactivar validación TLS en Android.
- Mantener secretos, archivos .env, claves SSH, keystores Android y bases SQLite excluidos del repositorio y de artefactos públicos.
- Activar monitoreo de uso/costos y copias de seguridad antes de guardar información real.

## Pasos para una etapa futura

1. Confirmar presupuesto, región, plan, titular de la cuenta, dominio y política de conservación de datos.
2. Crear la instancia Linux y asociar una IP estática. Crear el registro DNS para el dominio elegido.
3. Preparar el servidor, usuario de servicio, Python y entorno virtual; instalar únicamente las dependencias fijadas por el backend.
4. Configurar variables privadas y una base de datos nueva o migrada desde una copia previamente aprobada. Ejecutar la migración y validar integridad sin borrar tablas.
5. Instalar y probar el servicio systemd de FastAPI en loopback.
6. Configurar Caddy o Nginx, emitir/renovar TLS, abrir solo los puertos necesarios y verificar desde fuera que HTTP redirige a HTTPS y que 8000 no responde públicamente.
7. Crear y probar respaldos de SQLite; ensayar una restauración en una ubicación separada.
8. Configurar ricetta.releaseBaseUrl como URL HTTPS real durante la compilación release. No modificar la URL debug local ni reemplazarla por un dominio ficticio.
9. Generar el APK firmado desde Android Studio con un keystore guardado fuera del repositorio. Compartir APK y credenciales por canales separados. No publicar el keystore ni las contraseñas de firma.
10. Instalar en un teléfono físico y probar registro nuevo, conflicto de correo, login, persistencia, CRUD, logout, certificado TLS y conectividad por Wi‑Fi y datos móviles.
11. Documentar monitoreo, rotación de secretos, expiración de tokens, actualización y recuperación.

## Verificaciones antes de autorizar costos o publicación

- OpenAPI accesible mediante HTTPS y contrato de autenticación/recetas sin cambios accidentales.
- Registro/login y CRUD probados en base de prueba; aislamiento por usuario y códigos HTTP correctos.
- Certificado vigente y validado por Android; no hay cleartext HTTP en release.
- Uvicorn no es accesible directamente desde Internet.
- SSH restringido, secretos fuera de Git, sistema actualizado y respaldos restaurables.
- Prueba desde un teléfono fuera de la red del desarrollador, con ADB desconectado.
- Revisar estimación mensual y límites vigentes en la región elegida: instancia, transferencia excedente, almacenamiento/snapshots, IP, DNS/dominio e impuestos. No asumir que una oferta gratuita aplica ni que cubre todos los componentes.

## Costos a revisar

Lightsail ofrece planes con cargo por hora hasta un máximo mensual y cuotas de transferencia; precios, paquetes, exceso de transferencia y ofertas dependen de región y fecha. Consultar la tabla vigente antes de crear cualquier recurso: [planes de instancias Lightsail](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-bundles.html), [precios de Lightsail](https://aws.amazon.com/lightsail/pricing/) y [FAQ de facturación](https://aws.amazon.com/lightsail/faq/). Una IP estática puede tener cargo si queda desvinculada; revisar el importe actual. Sumar dominio/DNS, snapshots y posibles impuestos. Establecer alertas de presupuesto, recordando que pueden tardar y no necesariamente detienen recursos.

## Pendiente de autorización

No se debe crear infraestructura, activar facturación, comprar dominio, cargar credenciales, desplegar FastAPI ni emitir un APK release hasta contar con autorización expresa y decisiones sobre región, presupuesto, dominio, datos y firma.
