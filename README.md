# PersistenciaTransaccional: chat bidireccional con sockets

Entrega 1 (Escenario 3) del proyecto grupal de **Persistencia y Datos Transaccionales**.
Politécnico Grancolombiano, Grupo B02, SubGrupo 3.

Es un sistema de chat por consola con dos programas que se comunican con *sockets* TCP:

- **Servidor**: espera a los clientes, lleva la lista de usuarios conectados, reenvía los mensajes entre ellos y muestra el log de lo que pasa.
- **Cliente**: se conecta con la IP, el puerto y un nombre de usuario, elige con quién conversar y chatea hasta escribir `chao`.

---

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | **11 o superior** (probado con 21) | Compilar y ejecutar |
| Maven | 3.6 o superior (opcional) | Compilar y empaquetar con un solo comando |

Para verificar las versiones instaladas:

```bash
java -version
mvn -version
```

> El proyecto **no tiene dependencias externas**: solo usa la biblioteca estándar de Java (`java.net`, `java.io`, `java.util.concurrent`). Maven solo se usa para compilar con `--release 11` y generar el `.jar`. Si no tiene Maven, puede compilar con `javac` (sección 3.2).

---

## 2. Estructura del proyecto

```
PersistenciaTransaccional/
├── pom.xml                                   Configuración de Maven (Java 11, UTF-8)
├── README.md                                 Este documento
└── src/main/java/co/edu/poligran/chat/
    ├── Servidor.java                         Socket servidor: acepta clientes, registro de usuarios y log
    ├── ManejadorCliente.java                 Hilo que atiende a un cliente: protocolo, mensajes y comandos
    └── Cliente.java                          Socket cliente: pide los datos de conexión y chatea por consola
```

---

## 3. Compilar

Abra una terminal en la carpeta `PersistenciaTransaccional`.

### 3.1 Con Maven (recomendado)

```bash
mvn clean package
```

Esto genera:

- `target/classes/`: las clases compiladas.
- `target/persistencia-transaccional-1.0.0.jar`: el proyecto empaquetado.

### 3.2 Sin Maven (solo con el JDK)

**Windows (PowerShell o CMD):**
```bat
javac --release 11 -encoding UTF-8 -d out src\main\java\co\edu\poligran\chat\*.java
```

**Linux / macOS:**
```bash
javac --release 11 -encoding UTF-8 -d out src/main/java/co/edu/poligran/chat/*.java
```

Si compiló así, en los comandos de ejecución reemplace `target/classes` por `out`.

---

## 4. Ejecutar

Necesita **una terminal para el servidor** y **una terminal por cada cliente** (por ejemplo, tres clientes: `poli01`, `poli02` y `poli03`).

### 4.1 Iniciar el servidor

```bash
java -cp target/classes co.edu.poligran.chat.Servidor
```

Por defecto escucha en el puerto **59420**. Para usar otro puerto, páselo como argumento:

```bash
java -cp target/classes co.edu.poligran.chat.Servidor 6000
```

Salida esperada:

```
[21:35:39] Servidor iniciado y contestando OK en el puerto 59420
```

### 4.2 Iniciar cada cliente

En otra terminal:

```bash
java -cp target/classes co.edu.poligran.chat.Cliente
```

El cliente pide tres datos. Si presiona **Enter** sin escribir nada, usa el valor entre corchetes:

```
IP del servidor [127.0.0.1]:
Puerto de la conexión [59420]:
Escriba su nombre: poli01
```

> También puede usar el `.jar`: `java -cp target/persistencia-transaccional-1.0.0.jar co.edu.poligran.chat.Cliente` (y lo mismo para `Servidor`).

### 4.3 Ejecutar desde Eclipse o NetBeans

1. **Eclipse:** `File > Import > Maven > Existing Maven Projects` y seleccione la carpeta `PersistenciaTransaccional`.
   **NetBeans:** `File > Open Project` y seleccione la carpeta (NetBeans reconoce el `pom.xml`).
2. Ejecute `Servidor.java` con *Run As > Java Application*.
3. Ejecute `Cliente.java` varias veces, una por cada usuario. Cada ejecución abre su propia consola; en Eclipse se cambia entre consolas con el botón *Display Selected Console*.

---

## 5. Cómo usar el chat

1. **Identificarse:** al conectarse, el servidor da la bienvenida y muestra la lista de usuarios conectados. Si el nombre ya está en uso (sin importar mayúsculas), el servidor pide otro.
2. **Elegir con quién conversar:** escriba el nombre de un usuario de la lista (por ejemplo `poli03`).
3. **Conversar:** cada línea que escriba le llega al otro usuario con el formato `poli01--> mensaje`. Si alguien le escribe y usted aún no ha elegido interlocutor, el servidor lo conecta con esa persona para que pueda responder de inmediato.
4. **Salir:** escriba `chao`. El mensaje le llega a su interlocutor, el servidor marca que usted abandonó y se cierra su conexión.

### Comandos disponibles

| Comando | Acción |
|---|---|
| `<nombre>` | Elige el usuario con el que va a conversar (cuando aún no tiene interlocutor) |
| *(cualquier texto)* | Mensaje para su interlocutor actual |
| `/usuarios` | Muestra la lista de usuarios conectados |
| `/cambiar` | Deja la conversación actual para elegir otro usuario |
| `chao` | Abandona el chat y cierra la conexión |

### Ejemplo de sesión (prueba real)

**Log del servidor:**
```
[21:35:39] Servidor iniciado y contestando OK en el puerto 59420
[21:35:41] Usuario "poli01" conectado
[21:35:41] Usuario "poli02" conectado
[21:35:42] Usuario "poli03" conectado
[21:35:43] El usuario "poli02" abandonó
[21:35:44] "poli01" eligió conversar con "poli03"
[21:35:44] poli01 --> poli03: Buenos días poli03
[21:35:45] poli03 --> poli01: Buenos días Poli01
[21:35:46] poli01 --> poli03: Hoy vamos a ir a jugar?
[21:35:47] poli03 --> poli01: Si no llueve, nos vemos.
[21:35:47] poli03 --> poli01: chao
[21:35:47] El usuario "poli03" abandonó
```

**Consola de poli03:**
```
IP del servidor [127.0.0.1]:
Puerto de la conexión [59420]:
Escriba su nombre: poli03
Conectado a 127.0.0.1:59420
[Servidor] Bienvenido(a), poli03.
[Servidor] Usuarios conectados:
   - poli01
   - poli02
[Servidor] Escriba el nombre del usuario con el que desea conversar.
[Servidor] poli01 inició una conversación con usted. Sus mensajes ahora van dirigidos a poli01.
poli01--> Buenos días poli03
Buenos días Poli01
poli01--> Hoy vamos a ir a jugar?
Si no llueve, nos vemos.
chao
[Servidor] Hasta pronto, poli03.
Conexión con el servidor finalizada.
```

---

## 6. Diseño

### 6.1 Modelo de comunicaciones

```
 ┌──────────────┐         TCP          ┌────────────────────────────────────┐
 │ Cliente      │ ───────────────────▶ │ Servidor  (ServerSocket :59420)    │
 │ poli01       │ ◀─────────────────── │                                    │
 └──────────────┘                      │  usuarios: Map<nombre, Manejador>  │
 ┌──────────────┐                      │                                    │
 │ Cliente      │ ◀──────────────────▶ │  ManejadorCliente (hilo poli01)    │
 │ poli02       │                      │  ManejadorCliente (hilo poli02)    │
 └──────────────┘                      │  ManejadorCliente (hilo poli03)    │
 ┌──────────────┐                      │                                    │
 │ Cliente      │ ◀──────────────────▶ │  Log en consola                    │
 │ poli03       │                      └────────────────────────────────────┘
 └──────────────┘
```

- Los clientes **nunca se conectan entre sí**: todo mensaje pasa por el servidor, que lo reenvía al destinatario (topología en estrella).
- **Puerto del servidor:** `59420/TCP` (configurable).
- **IP en pruebas locales:** `127.0.0.1` (*localhost*), con servidor y clientes en el mismo equipo.
- **IP en una red local (LAN):** los clientes usan la IP privada del equipo servidor, por ejemplo `192.168.1.10`. Esa IP se consulta con `ipconfig` (Windows) o `ip a` (Linux). El firewall del servidor debe permitir conexiones entrantes al puerto 59420.

### 6.2 Protocolo

Es un protocolo de texto en **UTF-8** donde **cada mensaje es una línea**:

1. Cliente → Servidor: la primera línea es el nombre de usuario.
2. Servidor → Cliente: bienvenida y lista de usuarios conectados (las líneas de aviso empiezan por `[Servidor]`).
3. Cliente → Servidor: el nombre del interlocutor, luego los mensajes o comandos.
4. Servidor → Destinatario: `remitente--> mensaje`.
5. `chao`: el servidor reenvía el mensaje, registra el abandono, avisa al interlocutor y cierra el socket.

### 6.3 Clases (POO)

| Clase | Responsabilidad |
|---|---|
| `Servidor` | Abre el `ServerSocket`, acepta conexiones y le entrega cada una a un hilo del `ExecutorService`. Mantiene el registro concurrente de usuarios (`ConcurrentHashMap`) y escribe el log con hora. |
| `ManejadorCliente` (`Runnable`) | Atiende a un cliente en su propio hilo: identificación, elección de interlocutor, reenvío de mensajes, comandos y desconexión. Cada usuario conectado es un objeto de esta clase. |
| `Cliente` | Pide IP, puerto y nombre, y abre el `Socket`. Usa **dos hilos** para que la comunicación sea bidireccional: el principal muestra lo que llega del servidor y otro hilo (*daemon*) envía lo que se escribe en el teclado. |

### 6.4 Concurrencia y manejo de errores

- El servidor atiende muchos clientes a la vez: **un hilo por cliente**, tomado de un *pool* (`Executors.newCachedThreadPool()`).
- El registro de usuarios es un `ConcurrentHashMap`, y su operación `putIfAbsent` hace atómica la validación de nombres repetidos.
- El campo `interlocutor` es `volatile` porque lo modifican varios hilos (por ejemplo, cuando otro usuario abandona).
- Si un cliente se cierra de forma abrupta (Ctrl+C, cierre de la ventana o caída de la red), el servidor detecta el error de lectura, registra el abandono y avisa a su interlocutor.
- Los recursos (sockets y flujos) se cierran con *try-with-resources*.

---

## 7. Documentación Javadoc

Todas las clases y métodos tienen comentarios Javadoc en español. Para generar la documentación HTML:

```bash
mvn javadoc:javadoc
```

Se genera en `target/reports/apidocs/index.html`.

---

## 8. Solución de problemas

| Problema | Causa / solución |
|---|---|
| `No fue posible iniciar el servidor: Address already in use` | El puerto ya está ocupado, quizá por un servidor anterior que sigue abierto. Ciérrelo o use otro puerto: `... Servidor 6000`. |
| `No fue posible conectarse al servidor: Connection refused` | El servidor no está corriendo, o la IP o el puerto están mal escritos. |
| Desde otro equipo no conecta | Revise que la IP sea la del servidor y que el firewall de Windows permita Java en el puerto 59420. |
| Las tildes o la ñ se ven mal en la consola de Windows | Antes de ejecutar, cambie la consola a UTF-8 con `chcp 65001`, o use Windows Terminal. |
| Maven no descarga los *plugins* | Revise su conexión a Internet y que `~/.m2/settings.xml` no apunte a un repositorio privado inaccesible. |
| `invalid target release: 11` | Está usando un JDK anterior a 11. Instale el JDK 11 o superior. |
