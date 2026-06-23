# 🏷️ Sistema de Subastas Online - TPI Programación IV

**Universidad Tecnológica Nacional - Facultad Regional Villa María**

> Proyecto Integrador para la materia Programación IV.  
> Implementación de un backend robusto para una plataforma de subastas con cierre temporal, puja segura, control de concurrencia y sistema de reputación.

---

## 📖 Descripción General

Este sistema permite gestionar subastas online donde los usuarios pueden:

- **Registrarse** y autenticarse mediante **JWT**.
- **Publicar productos** y crear subastas (rol `SELLER`).
- **Realizar pujas** en tiempo real con control de concurrencia (rol `USER`).
- **Moderar** publicaciones, resolver disputas y bloquear usuarios (rol `ADMIN`).

**Características principales**:
- ✅ Roles exclusivos (negociación: un usuario = un rol).
- ✅ Sistema de **reputación/incidencias**: 3 incidencias = bloqueo automático.
- ✅ Bloqueo **manual** por administrador + bloqueo **automático** por incidencias.
- ✅ Control de concurrencia en pujas mediante bloqueo pesimista (`SELECT FOR UPDATE`).
- ✅ Auditoría completa de cambios de estado de subastas.
- ✅ Almacenamiento de fechas en **UTC**.
- ✅ Notificaciones almacenadas en Base de Datos.

---

## 👥 Equipo de Desarrollo

| Integrante | Legajo | Email |
| :--- | :--- | :--- |
| Gaspar Fassi Campión | 16354 | gasparuniversidad@gmail.com |
| Gonzalo Gazzero | 17099 | gonzagazzero@gmail.com |

---

## 🛠️ Tecnologías Utilizadas

| Capa | Tecnología |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.x, Spring Security, JWT |
| **Persistencia** | MySQL 8.x, Spring Data JPA (Hibernate) |
| **Frontend (Plus)** | HTML5, CSS3, JavaScript (Fetch API) |
| **Control de Versiones** | Git + GitHub |
| **Documentación API** | Postman / OpenAPI (Swagger) |
| **Construcción** | Maven |

```sql
CREATE DATABASE subastas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
