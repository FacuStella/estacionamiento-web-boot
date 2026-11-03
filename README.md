# Sherlock Homes Web

Front del proyecto `TP-Progra-II-Spring`, hecho **solo con Java**: Spring MVC + Thymeleaf + Bootstrap.
No tiene una sola línea de JavaScript: Bootstrap se usa únicamente por su CSS (link en `fragmentos.html`).

Es un proyecto aparte: el navegador habla con este servidor (puerto 8081) y este servidor llama a la API REST
del backend (puerto 8080) con un `RestClient`, mandando el token JWT de la sesión. Por eso **no hace falta
tocar el backend ni configurar CORS**.

## Cómo correrlo

1. Levantar el backend (`SherlockhomesApplication`, puerto 8080, con MySQL).
2. Abrir esta carpeta como proyecto Maven en NetBeans y ejecutar `FrontApplication`
   (o `./mvnw spring-boot:run`).
3. Entrar a http://localhost:8081 (usuario inicial del backend: `admin` / `Admin1234`).

Si el backend no está en `localhost:8080`, cambiar `backend.url` en `application.properties`.
Bootstrap se carga desde un CDN, así que hace falta internet.

## Estructura

| Paquete / archivo | Qué hace |
|---|---|
| `api/ApiClient` | Único lugar que llama al backend. Agrega el token y traduce los errores `{status, mensaje, detalles}`. |
| `sesion/SesionUsuario` | Token, rol y usuario, uno por sesión HTTP (`@SessionScope`). |
| `sesion/AuthInterceptor` | `/admin`, `/empleado` y `/socio` exigen su rol; si no, vuelve al login. Con una contraseña temporal, manda a cambiarla. |
| `modelo/Entidades` | Catálogo de socios, empleados, zonas, garages, vehículos y administradores: columnas, campos del formulario y acciones. |
| `modelo/Campo`, `Accion`, `Entidad`, `Columna`, `Opcion`, `Fila` | Clases que describen formularios, acciones y tablas. |
| `controller/AdminController` | Playa (planta por planta, entradas y salidas), estadías, listado, alta, edición y acciones del administrador (un controlador para todas las entidades). |
| `controller/PanelController` | Pantallas de solo lectura de empleado y socio. |
| `controller/LoginController`, `Vista` | Login/logout; menú y manejo de errores comunes. |
| `controller/CuentaController` | Cambio de contraseña del usuario logueado (obligatorio en el primer ingreso). |
| `templates/*.html` | Plantillas Thymeleaf con clases de Bootstrap. |

## Cómo funciona sin JavaScript

- Todo es HTML + formularios `POST` + redirecciones.
- Las acciones (asignar zona, quitar socio, eliminar…) son links a una página propia: un desplegable para elegir
  o una pantalla de confirmación, y al aceptar se hace el `POST`.
- La búsqueda de los listados es un formulario `GET` que filtra en Java.

## Agregar una entidad o una acción

Se agrega en `Entidades.java` (columnas, `Campo`s y `Accion`es); los controladores y las plantillas no cambian.
# TP-Progra-II-Web-Bootstrap
