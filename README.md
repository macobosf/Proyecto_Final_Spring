# Academic Events API

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Data%20Redis-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

Proyecto integrador de la materia: una API REST para gestionar eventos académicos (usuarios, eventos, sesiones e inscripciones), con autenticación JWT, autorización por roles, límites de uso con Redis y generación de reportes en PDF y Excel.

Es un trabajo individual, aunque el enunciado original estaba planteado para parejas; esto fue confirmado con el docente.

## En qué va el proyecto

Por ahora se avanzó en la parte inicial de configuración, antes de empezar con el código de dominio:

- Proyecto generado con Spring Initializr (Java 25, Spring Boot 4.1.0, Gradle con Kotlin DSL). Compila sin problemas.
- Se agregaron a mano las dependencias que Initializr no trae como opción: JWT (jjwt), Springdoc para la documentación OpenAPI, Bucket4j para el rate limiting sobre Redis, y Apache POI más OpenPDF para los reportes. Las versiones se verificaron contra Maven Central en vez de dejarlas fijas de memoria, y dos de ellas terminaron siendo distintas a lo que se tenía anotado inicialmente (POI y OpenPDF, ambas más nuevas).
- Un detalle a tener en cuenta más adelante: OpenPDF a partir de la versión 3 cambió su paquete raíz de `com.lowagie` a `org.openpdf`, así que cuando se escriba el módulo de reportes en PDF hay que usar los imports nuevos.
- Se recibieron los scripts SQL que entrega el docente para crear la base de datos: `sql/00_create_database.sql` y `sql/V1__initial_schema_and_data.sql`. La base de datos no la va a crear Hibernate; las entidades JPA se van a escribir a partir de este esquema ya definido, no al revés.

## Corrección en el script de datos iniciales

Revisando el script `V1__initial_schema_and_data.sql` antes de ejecutarlo, apareció un carácter suelto que rompía la migración. En la línea 742, entre el bloque de inscripciones (`registrations`) y el de auditoría (`audit_logs`), había una línea con solo una `s`, algo que no es ni una sentencia SQL válida ni un comentario.

Eso hace que la ejecución del script se corte justo ahí, así que los cinco registros de `audit_logs` nunca llegarían a insertarse. Se quitó esa línea y el resto del archivo queda igual.

Antes:

```sql
    (45, '00000000-0000-4000-8000-000000000045', 10, 15, 'CONFIRMED', ...);

s
-- --------------------------------------------------------------------------
-- Auditoría de ejemplo
```

Después:

```sql
    (45, '00000000-0000-4000-8000-000000000045', 10, 15, 'CONFIRMED', ...);

-- --------------------------------------------------------------------------
-- Auditoría de ejemplo
```

## Modelo de datos

El esquema entregado incluye: `roles`, `users`, `user_roles`, `categories`, `events`, `sessions`, `registrations`, `refresh_tokens` y `audit_logs`. Entre las cosas que ya vienen resueltas en el script están las restricciones de negocio a nivel de base de datos (por ejemplo, que un evento presencial tenga ubicación y no enlace virtual, o que una inscripción confirmada tenga fecha de confirmación), los triggers para mantener `updated_at`, y el manejo de concurrencia optimista con una columna `version` en `events` y `registrations`.

## Cómo ejecutar los scripts (con Postgres en Docker)

```bash
docker exec -i <contenedor-postgres> psql -U <usuario> -d postgres < sql/00_create_database.sql
docker exec -i <contenedor-postgres> psql -U <usuario> -d academic_events_db < sql/V1__initial_schema_and_data.sql
docker exec -it <contenedor-postgres> psql -U <usuario> -d academic_events_db -c "\dt"
```

El nombre del contenedor y del usuario dependen de cómo se levante Postgres para este proyecto (todavía no está definido el docker-compose propio).

## Lo que falta

- Todo el código de dominio: entidades, DTOs, servicios, controladores, seguridad con JWT, integración con Redis y el módulo de reportes.
- Dockerfile, docker-compose (o equivalente) y despliegue en Render.
- Documentación OpenAPI, pruebas, colección de Postman/Bruno y el resto de entregables del curso.
