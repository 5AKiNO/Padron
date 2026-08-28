# Secure Access & Trial Management System

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase%20Spark-orange.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-navy.svg)

Esto es un sistema de control de acceso ultra-seguro diseñado para aplicaciones Android. El núcleo del proyecto se basa en un modelo de **costo operativo cero ($0 USD)** mediante el uso optimizado del plan Spark de Firebase, implementando validaciones de hardware únicas y gestión dinámica de sesiones

## Filosofía del Proyecto
A diferencia de los sistemas de autenticación convencionales, esta aplicación implementa un **Vínculo Físico (Hardware Binding)**. Esto garantiza que una cuenta no pueda ser compartida, vinculando la identidad digital del usuario directamente a la "huella digital" de su dispositivo físico.

## Características de Seguridad

### 1. Identificación por Hardware Binding
El sistema utiliza el `ANDROID_ID` del dispositivo como identificador único. 
- **Propósito:** Evitar la duplicidad de cuentas en diferentes dispositivos.
- **Validación:** Se realiza una comparación en tiempo real entre el ID del dispositivo que intenta ingresar y el `active_device_id` almacenado en la nube.

### 2. Autenticación Admin-Centric
Se ha eliminado el auto-registro para mantener un control total sobre el ecosistema de usuarios.
- **Validación Estricta:** Solo los usuarios creados manualmente por el administrador pueden acceder.
- **Criptografía SHA-256:** Las contraseñas se gestionan mediante hashing de 64 caracteres. El sistema nunca almacena ni conoce la contraseña en texto plano, cumpliendo con estándares de privacidad industrial.

### 3. Restricción de Sesión Única (Anti-Fraud)
El `AuthRepository` actúa como un árbitro de estado. Si un usuario intenta iniciar sesión en un "Dispositivo B" mientras la sesión en el "Dispositivo A" sigue activa, el sistema bloquea el acceso automáticamente y notifica al usuario la necesidad de un cierre de sesión previo.

## Gestión de Usuarios (Tier System)

El sistema clasifica a los usuarios en dos niveles para optimizar recursos:

*   **Unlimited (Ilimitados):** Usuarios con acceso permanente. El sistema ignora los temporizadores de restricción.
*   **Limited (Prueba/Trial):** Usuarios con acceso temporal. Cuentan con un cronómetro dinámico (`trial_time_minutes`) visible en la interfaz. Una vez agotado el tiempo, el sistema ejecuta una **eliminación atómica** del nodo del usuario para liberar espacio en la base de datos (Plan Spark).

## Arquitectura Técnica

### Motor de Autodestrucción Dinámica
Implementado mediante **Unix Timestamps (Long)** para evitar conflictos de zona horaria.
- **Lógica:** `Login_Time + (Trial_Minutes * 60,000)`.
- **Acción:** Si `Current_Time > Expiration_Time`, el sistema revoca el acceso y limpia los datos obsoletos de forma automática.

### Protocolo de Bloqueo Offline
Para evitar que un usuario evada la expiración apagando la conexión a internet, se implementó un "Secuestro de UI" mediante un `AlertDialog` persistente en Jetpack Compose:
- Bloqueo de botón "Atrás".
- Bloqueo de toques fuera del diálogo.
- Obligatoriedad de reconexión para liberar la sesión.

## Estructura de Datos (Esquema)

La base de datos en Firebase Realtime Database sigue una estructura jerárquica optimizada:

```json
{
  "usuarios": {
    "user_id_alias": {
      "user_type": "limited | unlimited",
      "trial_time_minutes": 0,
      "password_hash": "SHA-256_HASH_STRING",
      "session_active": false,
      "active_device_id": "STRING",
      "last_login_timestamp": 0
    }
  }
}
```
