# Preparación de despliegue en AWS EC2

**Estado:** plan técnico solamente. No se creó infraestructura, no se desplegó FastAPI, no se eligieron región/tipo de instancia y no se consumió el crédito académico de USD 50. Permisos y disponibilidad dependen del laboratorio universitario.

## Arquitectura propuesta

Teléfono Android → HTTPS público → EC2 Linux → Caddy (:443; :80 para redirección/ACME) → Uvicorn en 127.0.0.1:8000 → FastAPI → SQLite sobre volumen EBS persistente.

Una instancia basta para una demostración de bajo tráfico. No se propone RDS, Load Balancer ni microservicios. No publicar el puerto 8000. Caddy necesita DNS real bajo control del usuario y conectividad pública para emitir/renovar TLS; no usar dominio inventado ni certificado autofirmado como solución final.

## Requisitos previos y restricciones por confirmar

Antes de iniciar sesión o gastar crédito, confirmar con el administrador del laboratorio: duración/suspensión de sesiones, regiones habilitadas, tipos de instancia, cuotas, acceso SSH, IPv4 pública/Elastic IP, DNS, Security Groups, snapshots, almacenamiento, transferencia, presupuestos y qué ocurre con recursos al acabar el laboratorio. No asumir que el crédito cubre cada servicio.

Elegir tipo y región tras revisar permisos y cotizar. Mantener la llave SSH fuera de Git. Usar MFA/alertas solo si el entorno lo permite.

## Backend Linux y configuración segura

Las dependencias están fijadas en backend/requirements.txt y backend/app/config.py lee JWT_SECRET_KEY (mínimo 32 bytes), DATABASE_URL y ACCESS_TOKEN_EXPIRE_MINUTES. Las versiones se verificaron en Python 3.13/Windows; instalación efectiva en Linux no se ha probado y debe validarse en el sistema elegido. El backend usa SQLite y tiene migración incremental. No borrar ni sustituir la base durante actualización.

Preparación futura, ajustando distribución y rutas:

1. Instalar Python compatible, venv y utilidades desde repositorios oficiales.
2. Crear usuario de servicio sin privilegios, por ejemplo ricetta; código en /opt/ricetta/backend, venv en /opt/ricetta/.venv.
3. Instalar requirements.txt y ejecutar pytest con una base temporal.
4. Montar EBS de datos en /var/lib/ricetta y asignar propietario/permisos mínimos.
5. Crear /etc/ricetta/ricetta.env con permisos 600, propietario root y grupo del servicio. No versionar:

       JWT_SECRET_KEY=<secreto aleatorio privado de al menos 32 bytes>
       DATABASE_URL=sqlite:////var/lib/ricetta/ricetta.db
       ACCESS_TOKEN_EXPIRE_MINUTES=60

   El proceso puede recibir variables desde systemd; load_dotenv no reemplaza variables ya presentes.
6. Ejecutar migración incremental sobre la base destinada a la demo y verificar integridad. No copiar la DB local automáticamente.
7. Preparar unidad systemd parametrizada usando la factory real create_app de app.main:

       [Unit]
       Description=API Ricetta (FastAPI)
       After=network-online.target
       Wants=network-online.target

       [Service]
       User=ricetta
       Group=ricetta
       WorkingDirectory=/opt/ricetta/backend
       EnvironmentFile=/etc/ricetta/ricetta.env
       ExecStart=/opt/ricetta/.venv/bin/uvicorn app.main:create_app --factory --host 127.0.0.1 --port 8000
       Restart=on-failure
       RestartSec=3
       NoNewPrivileges=true
       PrivateTmp=true
       ProtectSystem=strict
       ProtectHome=true
       ReadWritePaths=/var/lib/ricetta

       [Install]
       WantedBy=multi-user.target

   Revisar rutas/permisos, después systemctl daemon-reload, habilitar e iniciar. Ver: systemctl status ricetta; journalctl -u ricetta -f.
8. Actualizar código en ruta separada, conservar volumen y hacer respaldo previo. No borrar /var/lib/ricetta.

## Caddy, HTTPS y Security Groups

Cuando haya un dominio real apuntado a la IP pública:

       api.<dominio-controlado> {
           reverse_proxy 127.0.0.1:8000
       }

Caddy puede gestionar TLS si DNS y puertos son accesibles. Security Group: SSH 22 limitado al CIDR administrativo; 80 y 443 públicos si se necesitan para ACME/redirección; no 8000. Revisar firewall de Linux también. No publicar SQLite, .env, directorios internos ni credenciales. Android debe verificar TLS normalmente.

La IPv4 efímera puede cambiar al detener/iniciar EC2 y romper DNS. Confirmar si el laboratorio permite Elastic IP y sus cargos/restricciones. La renovación automática requiere servicio y DNS disponibles.

## EBS, SQLite y recuperación

Guardar DB en EBS montado en /var/lib/ricetta, fuera del directorio reemplazable de código, con permisos solo al usuario de servicio. EBS persiste tras reinicios pero no es respaldo: eliminar volumen o terminar una instancia con DeleteOnTermination activo puede perder datos.

Respaldar con API SQLite (por ejemplo, sqlite3.Connection.backup) o detener brevemente FastAPI antes de copiar; no confiar en copiar un archivo activo como único respaldo. Mantener copia protegida fuera del volumen y probar restauración en ruta separada. Confirmar usuarios/recetas tras reiniciar FastAPI y EC2.

## Costos y crédito de USD 50

No fijar costo mensual sin región, tipo y horas de uso. Cotizar cómputo EC2 por tipo/región/tiempo, EBS por GB-mes, snapshots, IPv4 pública, transferencia saliente, DNS/dominio e impuestos. Detener EC2 deja de generar cargos de cómputo cuando llega a detenido, pero EBS y recursos asociados pueden seguir facturándose. En terminación, volúmenes preservados siguen cobrando; otros se borran según DeleteOnTermination. Confirmar las reglas del laboratorio. USD 50 es crédito del laboratorio, no garantía de costo cero.

Revisar precios oficiales al elegir: EC2 [On-Demand](https://aws.amazon.com/ec2/pricing/on-demand/), [EBS](https://aws.amazon.com/ebs/pricing/), [IPv4/VPC](https://aws.amazon.com/vpc/pricing/) y [Calculadora AWS](https://calculator.aws/). AWS especifica que instancia detenida no genera cargo de cómputo pero sí se cobra almacenamiento EBS; precios y créditos dependen de región/condiciones. No seleccionar instancia por precio publicado de otra región.

## Secuencia y verificaciones futuras

1. Obtener autorización del usuario y del laboratorio; seleccionar región, AMI, tipo, almacenamiento, dominio y presupuesto.
2. Cotizar duración prevista y configurar alerta/límite si está permitido.
3. Crear EC2/EBS y reglas mínimas; documentar preservación/desasociación antes de terminar.
4. Configurar Linux, usuario, venv, secretos privados, migración y systemd.
5. Configurar DNS/Caddy; probar certificado, redirección HTTP→HTTPS y que 8000 no sea accesible desde Internet.
6. Ejecutar pruebas backend/API aisladas y Android sobre URL HTTPS.
7. Probar respaldo/restauración y persistencia tras reinicios.
8. Solo con API HTTPS verificada, configurar ricetta.releaseBaseUrl=https://<dominio-real>/ y firmar APK.

No iniciar sesión AWS automáticamente, crear EC2/EBS/Elastic IP/Security Groups/DNS, usar créditos, comprar dominio, desplegar API ni publicar secretos en esta etapa.
