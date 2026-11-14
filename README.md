# Patitas al Rescate

Continuación de la entrega individual E1-E4 conforme a Patitas_Grupal_Continuacion.pdf,
requisitos G1-G4. Se conserva el mismo repositorio y su historial.
Java 21, Spring Boot 3.5.7, Maven y PostgreSQL; paquete com.krakedev.patitas_rescate.
Se continúa el proyecto patitas_rescate de pro2, conservando sus paquetes config,
controllers, entidades, repositories y services.

## Ejecutar en Windows y Eclipse

1. Iniciar PostgreSQL y crear patitas_db si todavía no existe.
2. Copiar application-local.properties.example como application-local.properties
   en la raíz y completar allí usuario y contraseña local de PostgreSQL.
   Ese archivo está excluido de Git; no subir credenciales reales.
3. También se admiten DB_URL, DB_USERNAME y DB_PASSWORD por entorno.
   La URL predeterminada es jdbc:postgresql://localhost:5432/patitas_db.
4. Importar como Maven > Existing Maven Projects. Si ya existe en Eclipse,
   pulsar F5 y Maven > Update Project. Seleccionar JDK 21.
5. Ejecutar com.krakedev.patitas_rescate.PatitasRescateApplication. Esperar Started PatitasRescateApplication
   y el servidor en el puerto 8080.
6. Alternativa desde CMD: mvnw.cmd spring-boot:run.
   En Git Bash: bash mvnw spring-boot:run.

## Arquitectura

Controller recibe HTTP, Service valida y aplica reglas del negocio y Repository
consulta/persiste las entidades Mascota y Usuario en PostgreSQL. JwtAuthenticationFilter
valida el Bearer antes del Controller y establece la autenticación; SecurityConfig
protege las rutas y @PreAuthorize verifica roles. UsuarioService emplea
PasswordEncoder.encode al registrar y matches al hacer login.
TokenBlacklistService conserva revocaciones exclusivamente en memoria.

## Contrato y pruebas

Importar postman/Patitas_Grupal.postman_collection.json. Ejecutar sus carpetas en orden.
Después de la carpeta 1, detenerse para que el tutor habilite ADMIN antes de la carpeta 2.
La colección tiene 28 peticiones y guarda automáticamente tokens e id de las mascotas.

| Ruta | Acceso | Resultado |
| --- | --- | --- |
| POST /api/auth/registrar | Público | 201, siempre USER |
| POST /api/auth/login | Público | 200 con JWT; credenciales erróneas 401 |
| GET /api/mascotas | USER y ADMIN | 200 |
| POST /api/mascotas | ADMIN | 201 |
| GET /api/mascotas/disponibles | USER y ADMIN | 200, solo DISPONIBLE |
| PUT /api/mascotas/{id}/adoptar | ADMIN | 200; inexistente 404; ya adoptada 409 |
| POST /api/auth/logout | Autenticado | 200; revoca el JWT presentado |
| GET /api/auth/perfil | Autenticado | 200; apoyo para verificar la identidad |

Sin un token válido: 401. Con token válido y rol insuficiente: 403.
El registro de mascotas recorta nombre/especie, valida edad y fija DISPONIBLE.

## Habilitar ADMIN con supervisión del tutor

1. Registrar admin_grupal desde Postman; el backend asigna USER.
2. El tutor o administrador autorizado, en Query Tool de patitas_db, ejecuta:

    UPDATE public.usuarios SET rol = 'ADMIN' WHERE username = 'admin_grupal';
    SELECT id, username, rol FROM public.usuarios WHERE username = 'admin_grupal';

3. Hacer login nuevamente para recibir un token con rol ADMIN.
   Un JWT ya emitido conserva el rol anterior hasta vencer o ser revocado.
   No se admite elegir ADMIN desde el registro público.

Si los usernames ya existen, usar otros en las variables de Postman y en el SQL.
Las claves de la colección son ejemplos de práctica, no credenciales personales.

## Disponibles y adopción

MascotaRepository.findByEstado consulta PostgreSQL para devolver solo DISPONIBLE.
MascotaService.adoptar ejecuta una transacción: busca por id, devuelve 404 si no existe,
409 si no está DISPONIBLE y, en caso válido, guarda ADOPTADA.
La búsqueda usa un bloqueo de escritura sobre esa fila para que dos solicitudes
concurrentes no puedan confirmar dos adopciones de la misma mascota.
No se crea una entidad ni tabla de adopciones y no se modifican otras mascotas.
En Postman se guarda el id real devuelto al crear Luna; no se supone que sea 1.

Verificar en pgAdmin:

    SELECT id, nombre, especie, edad, estado FROM public.mascotas ORDER BY id;
    SELECT id, nombre, estado FROM public.mascotas WHERE estado = 'DISPONIBLE';
    SELECT id, username, rol FROM public.usuarios ORDER BY id;

## JWT, blacklist en memoria y reinicios

JwtUtil genera JWT HS256 con emisor patitas-rescate, subject, rol, jti y expiración.
El filtro verifica firma, emisor, expiración y claims antes de confiar en ellos;
comprueba también que el token no esté revocado. Sesiones STATELESS.
La firma no cifra el payload: nunca contiene contraseñas.

TokenBlacklistService usa ConcurrentHashMap<String, Instant>. Al hacer logout guarda
el token y su vencimiento; el filtro rechaza su reutilización. Se eliminan revocaciones
vencidas al consultarlas o registrar otro logout. El logout afecta solo al token presentado.
La blacklist no usa PostgreSQL, no crea tablas y se pierde al reiniciar la aplicación.
Con una clave JWT fija, un token cerrado pero aún vigente podría volver a aceptarse
tras reiniciar: es la limitación en memoria aceptada por el documento para esta prueba local.

Sin JWT_SECRET se genera una clave aleatoria por arranque; en ese caso todos los tokens
anteriores serán inválidos tras reiniciar, independientemente de la blacklist.
Para demostrar exclusivamente la pérdida de la blacklist, mantener JWT_SECRET local
constante (al menos 32 bytes), reiniciar y probar el mismo JWT antes de que expire.
No subir ese secreto a GitHub.

## Verificación y evidencias G4

CMD: mvnw.cmd clean verify. Git Bash: bash mvnw clean verify.
GitHub Actions también ejecuta verify con Java 21.
Las pruebas usan H2 en modo PostgreSQL y no modifican patitas_db.
La conexión y persistencia en PostgreSQL deben comprobarse en el equipo de la defensa.

Tomar evidencias reales, organizadas por G1-G4:

- G1: SecurityConfig, filtro y permisos; ausencia de token 401, USER consultando 200,
  USER registrando 403 y ADMIN registrando 201.
- G2: disponibles antes/después, USER adoptando 403, ADMIN adoptando 200,
  repetición 409, inexistente 404; SELECT con ADOPTADA y otra mascota DISPONIBLE.
- G3: acceso exitoso, logout 200 y mismo token rechazado con 401; código de la colección concurrente.
- G4: inicio de consola y flujo completo con persistencia real. Cada integrante explica
  un componente que comprenda; no se inventan participantes ni capturas.

## Continuidad de la base individual

El proyecto original usa entidades Mascota/Usuario y sus servicios; se mantiene esa
organización. Se añaden los métodos de disponibles y adopción sin crear un proyecto nuevo.
Se ajustó Spring Boot 4.1.2 / Java 25 a Spring Boot 3.5.7 / Java 21 para seguir el requisito
3.x de la entrega individual y usar un entorno compatible con la verificación.
El username duplicado mantiene la respuesta 400 de la base de esta compañera.
Los errores individuales de validación se conservan como JSON con campo error.
El registro ignora el id enviado por el cliente para que solo cree registros nuevos.
La configuración de credenciales y JWT se realiza localmente; no se publican secretos nuevos.

## Respuestas técnicas grupales

### G1
1. El filtro JWT lee Authorization, extrae el Bearer, comprueba firma, expiración y
   revocación y establece el usuario y sus autoridades antes de llegar al Controller.
2. Autenticación verifica quién hace la petición. Autorización decide si ese usuario
   tiene permiso para la operación: un USER autenticado recibe 403 al crear o adoptar.

### G2
1. Repository aporta una consulta real a la base mediante findByEstado; devuelve solo
   registros DISPONIBLE y evita mantener un listado ficticio o filtrar datos quemados.
2. Una mascota ADOPTADA ya no cumple la condición del negocio. La segunda adopción
   devuelve 409 para informar el conflicto y conserva el estado, sin repetir la operación.

### G3
1. La firma válida no informa que el usuario cerró sesión. La blacklist agrega esa
   revocación al control de firma y expiración e impide reutilizar el JWT cerrado.
2. La blacklist en memoria se pierde al reiniciar y no se comparte entre instancias.
   Con la misma clave, un JWT revocado todavía vigente puede volver a ser aceptado.

### G4: defensa
- La contraseña se valida en UsuarioService.autenticar con PasswordEncoder.matches.
- La petición llega al Controller, que llama al Service; este valida y usa el Repository,
  que opera sobre PostgreSQL mediante JPA. El resultado regresa como respuesta JSON.
- El filtro valida firma, emisor, expiración, claims y revocación antes de autenticar.
- @EnableMethodSecurity activa @PreAuthorize; hasRole('ADMIN') requiere ROLE_ADMIN
  en las autoridades que el filtro creó desde un JWT verificado.
- La adopción duplicada se evita verificando DISPONIBLE dentro de una transacción
  con bloqueo de fila; la segunda solicitud ve ADOPTADA y devuelve 409.

## Preguntas técnicas

### E1
1. @Entity indica que JPA debe mapear la clase a una tabla. @Id identifica la clave primaria
   que distingue cada fila; @GeneratedValue permite generarla.
2. Una clase define atributos y comportamiento; un objeto es una instancia en memoria.
   Una entidad persistente es un objeto con identidad que JPA puede guardar y recuperar.

### E2
3. El Service aplica las reglas del negocio y coordina operaciones; el Repository se ocupa
   de consultar y persistir. Separarlos evita mezclar validaciones con el acceso a los datos.
4. @PostMapping atiende POST para registrar datos; @GetMapping atiende GET para consultarlos.
   En esta API POST crea mascotas y GET devuelve su listado.

### E3
5. BCrypt es un hash unidireccional con sal y costo de cálculo. No tiene una operación para
   descifrar y recuperar la clave original. PasswordEncoder.matches verifica la clave ingresada.
6. Permitir elegir el rol en un registro público permitiría crear cuentas ADMIN sin autorización.
   El backend fija USER e ignora el rol que intente enviar el cliente.

### E4
7. Un JWT contiene header, payload y firma. El payload incluye datos como identidad y expiración;
   la firma permite detectar alteraciones. Firmar no oculta el contenido y no debe incluir claves.
8. Registrar crea y guarda una cuenta con el hash de su clave. Iniciar sesión verifica las
   credenciales de una cuenta existente y emite el JWT si son correctas.
