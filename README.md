# Estacionamiento · Web

Front del sistema de gestión de un **estacionamiento por horas** (estilo shopping o aeropuerto): una playa con
varias plantas y zonas, vehículos que entran y salen pagando por hora, socios con descuento y cocheras con abono.

Está hecho **solo con Java**: Spring MVC + Thymeleaf + Bootstrap. No tiene JavaScript; Bootstrap se usa
únicamente por su CSS (link en `fragmentos.html`).

Es un proyecto aparte del backend (`estacionamiento-spring`): el navegador habla con este servidor (puerto **8083**)
y este servidor llama a la API REST del backend (puerto **8082**) con un `RestClient`, mandando el token JWT
guardado en la sesión. El token nunca llega al navegador, y no hace falta configurar CORS.

## Cómo correrlo

1. Levantar el backend (`StartApplication` de `estacionamiento-spring`, puerto 8082, con MySQL).
2. Abrir esta carpeta como proyecto Maven en NetBeans y ejecutar `FrontApplication` (o `./mvnw spring-boot:run`).
3. Entrar a http://localhost:8083 (usuario inicial del backend: `admin` / `Admin1234`; en el primer ingreso
   pide cambiar la contraseña).

Si el backend no está en `localhost:8082`, cambiar `backend.url` en `application.properties`.
Bootstrap se carga desde un CDN, así que hace falta internet.

## Qué puede hacer cada rol

| Rol | Pantallas |
|---|---|
| Administrador | **Playa** (plano por planta con las cocheras libres, con abono u ocupadas; registrar entrada y salida con el cobro), **Estadías** (historial ordenable por columna y exportable a CSV) y ABM de socios, empleados, zonas, garages, vehículos y administradores. |
| Empleado | Sus vehículos, garages y zonas asignados (solo lectura). |
| Socio | Su perfil, sus vehículos y sus garages con abono (solo lectura). |
| Todos | Cambiar su contraseña (desde su nombre de usuario en la barra superior). |

## Estructura

| Paquete / archivo | Qué hace |
|---|---|
| `api/ApiClient` | Único lugar que llama al backend. Agrega el token y traduce los errores `{status, mensaje, detalles}`. |
| `sesion/SesionUsuario` | Token, rol, usuario y si tiene contraseña temporal; uno por sesión HTTP (`@SessionScope`). |
| `sesion/AuthInterceptor` | `/admin`, `/empleado` y `/socio` exigen su rol; si no, vuelve al login. Con una contraseña temporal, manda a cambiarla. |
| `config/WebConfig` | Registra el interceptor de acceso y manda `Cache-Control: no-store` para que Atrás/Adelante no muestren páginas guardadas. |
| `modelo/Entidades` | Catálogo de socios, empleados, zonas, garages, vehículos y administradores: columnas, campos del formulario y acciones. |
| `modelo/Campo`, `Accion`, `Entidad`, `Columna`, `Opcion`, `Fila` | Clases que describen formularios, acciones y tablas. |
| `modelo/PlayaPlanta`, `PlayaZona`, `Lote` | Datos ya armados para dibujar la playa (plantas, zonas, hileras y cocheras). |
| `controller/AdminController` | Playa, estadías (con CSV), listado, alta, edición y acciones del administrador (un controlador para todas las entidades). |
| `controller/PanelController` | Pantallas de solo lectura de empleado y socio. |
| `controller/LoginController`, `Vista` | Login/logout; menú y manejo de errores comunes. |
| `controller/CuentaController` | Cambio de contraseña del usuario logueado (obligatorio en el primer ingreso). |
| `util/Valores`, `Csv` | Lectura de datos del JSON, formato de celdas y armado del CSV. |
| `templates/*.html`, `static/css/playa.css` | Plantillas Thymeleaf con clases de Bootstrap; estilos del plano de la playa. |

## Cómo funciona sin JavaScript

- Todo es HTML + formularios `POST` + redirecciones.
- Las acciones (asignar zona, quitar abono, restablecer contraseña, eliminar…) son links a una página propia:
  un desplegable para elegir o una pantalla de confirmación, y al aceptar se hace el `POST`.
- Cuando un desplegable depende de otro (las marcas según el tipo de vehículo), el formulario va en dos pasos:
  primero se elige el tipo y la página se recarga con las marcas que corresponden.
- La búsqueda de los listados es un formulario `GET` que filtra en Java; el orden de las estadías se elige
  con links en los encabezados.

## Seguridad de la sesión

- Volver al login (por ejemplo con Atrás) cierra la sesión.
- Al loguearse se cambia el id de sesión (evita la fijación de sesión) y el id viaja solo en la cookie, nunca en la URL.
- Las páginas no se guardan en la caché del navegador.

## Agregar una entidad o una acción

Se agrega en `Entidades.java` (columnas, `Campo`s y `Accion`es); los controladores y las plantillas no cambian.
