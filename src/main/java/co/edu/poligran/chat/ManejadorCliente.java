package co.edu.poligran.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Atiende en el servidor la conexión de un cliente del chat, en un hilo propio.
 * <p>
 * Implementa el protocolo de texto del chat, en el que cada mensaje es una línea:
 * </p>
 * <ol>
 *     <li>La primera línea que envía el cliente es su nombre de usuario.</li>
 *     <li>El servidor le responde con la lista de usuarios conectados.</li>
 *     <li>Si el usuario no tiene interlocutor, la línea que escriba se toma como el nombre
 *         del usuario con el que quiere conversar.</li>
 *     <li>Las líneas siguientes son mensajes que se reenvían al interlocutor.</li>
 *     <li>{@value #COMANDO_SALIR} se reenvía al interlocutor y cierra la sesión.</li>
 * </ol>
 * <p>
 * Comandos adicionales: {@value #COMANDO_USUARIOS} muestra la lista de conectados y
 * {@value #COMANDO_CAMBIAR} permite elegir otro interlocutor.
 * </p>
 *
 * @author SubGrupo 3 - Grupo B02
 * @version 1.0.0
 */
public class ManejadorCliente implements Runnable {

    /** Mensaje con el que el usuario abandona el chat. */
    public static final String COMANDO_SALIR = "chao";

    /** Comando que muestra la lista de usuarios conectados. */
    public static final String COMANDO_USUARIOS = "/usuarios";

    /** Comando para dejar la conversación actual y elegir otro usuario. */
    public static final String COMANDO_CAMBIAR = "/cambiar";

    /** Prefijo de los avisos que genera el servidor, para distinguirlos de los mensajes de chat. */
    private static final String AVISO = "[Servidor] ";

    /** Socket de la conexión con el cliente. */
    private final Socket socket;

    /** Servidor que mantiene el registro de usuarios. */
    private final Servidor servidor;

    /** Canal de salida hacia el cliente, con vaciado automático en cada línea. */
    private PrintWriter salida;

    /** Nombre del usuario; es {@code null} hasta que se identifica. */
    private volatile String nombre;

    /** Usuario con el que conversa en este momento; es {@code null} si no ha elegido ninguno. */
    private volatile ManejadorCliente interlocutor;

    /**
     * Crea el manejador de una conexión recién aceptada.
     *
     * @param socket   socket conectado con el cliente
     * @param servidor servidor que registra a los usuarios
     */
    public ManejadorCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    /**
     * Ciclo de vida de la conexión: identificación, intercambio de mensajes y cierre.
     * Cuando la conexión termina, sea con {@value #COMANDO_SALIR} o por una caída,
     * el usuario se retira del servidor.
     */
    @Override
    public void run() {
        try (Socket conexion = socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8))) {
            salida = new PrintWriter(
                    new OutputStreamWriter(conexion.getOutputStream(), StandardCharsets.UTF_8), true);
            if (iniciarSesion(entrada)) {
                atenderMensajes(entrada);
            }
        } catch (IOException e) {
            servidor.registrar("Conexión interrumpida con \"" + nombre + "\": " + e.getMessage());
        } finally {
            servidor.desconectar(this);
        }
    }

    /**
     * Lee líneas hasta recibir un nombre de usuario válido y libre, y lo registra en el servidor.
     *
     * @param entrada canal de lectura del cliente
     * @return {@code true} si el usuario quedó registrado; {@code false} si el cliente se desconectó antes
     * @throws IOException si falla la lectura
     */
    private boolean iniciarSesion(BufferedReader entrada) throws IOException {
        String linea;
        while ((linea = entrada.readLine()) != null) {
            String candidato = linea.trim();
            if (!esNombreValido(candidato)) {
                enviar(AVISO + "Nombre inválido. Escriba otro nombre de usuario:");
                continue;
            }
            nombre = candidato;
            if (servidor.conectar(this)) {
                enviar(AVISO + "Bienvenido(a), " + nombre + ".");
                enviarListaUsuarios();
                return true;
            }
            nombre = null;
            enviar(AVISO + "El nombre \"" + candidato + "\" ya está en uso. Escriba otro nombre de usuario:");
        }
        return false;
    }

    /**
     * Procesa las líneas del cliente hasta que escriba {@value #COMANDO_SALIR} o se desconecte.
     *
     * @param entrada canal de lectura del cliente
     * @throws IOException si falla la lectura
     */
    private void atenderMensajes(BufferedReader entrada) throws IOException {
        String linea;
        while ((linea = entrada.readLine()) != null) {
            String texto = linea.trim();
            if (texto.isEmpty()) {
                continue;
            }
            if (texto.equalsIgnoreCase(COMANDO_SALIR)) {
                if (interlocutor != null) {
                    enviarMensaje(texto);
                }
                enviar(AVISO + "Hasta pronto, " + nombre + ".");
                return;
            }
            if (texto.equalsIgnoreCase(COMANDO_USUARIOS)) {
                enviarListaUsuarios();
            } else if (texto.equalsIgnoreCase(COMANDO_CAMBIAR)) {
                interlocutor = null;
                enviarListaUsuarios();
            } else if (interlocutor == null) {
                elegirInterlocutor(texto);
            } else {
                enviarMensaje(texto);
            }
        }
    }

    /**
     * Fija como interlocutor al usuario indicado, si está conectado y no es el mismo usuario.
     *
     * @param nombreDestino nombre del usuario con el que se quiere conversar
     */
    private void elegirInterlocutor(String nombreDestino) {
        Optional<ManejadorCliente> destino = servidor.buscar(nombreDestino);
        if (destino.isEmpty()) {
            enviar(AVISO + "El usuario \"" + nombreDestino + "\" no está conectado. "
                    + "Escriba " + COMANDO_USUARIOS + " para ver la lista.");
        } else if (destino.get() == this) {
            enviar(AVISO + "No puede conversar consigo mismo. Elija otro usuario.");
        } else {
            interlocutor = destino.get();
            servidor.registrar("\"" + nombre + "\" eligió conversar con \"" + interlocutor.getNombre() + "\"");
            enviar(AVISO + "Ahora conversa con " + interlocutor.getNombre()
                    + ". Escriba sus mensajes (\"" + COMANDO_SALIR + "\" para salir).");
        }
    }

    /**
     * Reenvía un mensaje de chat al interlocutor actual y lo deja en el log del servidor.
     * Toma una copia local del interlocutor, porque otro hilo puede liberarlo si ese usuario abandona.
     *
     * @param texto contenido del mensaje
     */
    private void enviarMensaje(String texto) {
        ManejadorCliente destino = interlocutor;
        if (destino == null) {
            enviar(AVISO + "Su interlocutor ya no está conectado. Elija otro usuario.");
            return;
        }
        destino.recibirMensaje(this, texto);
        servidor.registrar(nombre + " --> " + destino.getNombre() + ": " + texto);
    }

    /**
     * Entrega a este cliente un mensaje de otro usuario. Si este cliente aún no tenía
     * interlocutor, el remitente pasa a serlo para que pueda responderle de inmediato.
     *
     * @param remitente usuario que envía el mensaje
     * @param texto     contenido del mensaje
     */
    void recibirMensaje(ManejadorCliente remitente, String texto) {
        if (interlocutor == null) {
            interlocutor = remitente;
            enviar(AVISO + remitente.getNombre() + " inició una conversación con usted. "
                    + "Sus mensajes ahora van dirigidos a " + remitente.getNombre() + ".");
        }
        enviar(remitente.getNombre() + "--> " + texto);
    }

    /**
     * Avisa a este cliente que un usuario abandonó el chat. Si era su interlocutor,
     * lo libera para que pueda elegir a otro.
     *
     * @param usuario usuario que abandonó
     */
    void interlocutorAbandono(ManejadorCliente usuario) {
        if (interlocutor == usuario) {
            interlocutor = null;
            enviar(AVISO + usuario.getNombre() + " abandonó el chat.");
            enviarListaUsuarios();
        }
    }

    /**
     * Envía al cliente la lista de usuarios conectados y le indica cómo elegir uno.
     */
    private void enviarListaUsuarios() {
        List<String> conectados = servidor.nombresConectados().stream()
                .filter(usuario -> !usuario.equals(nombre))
                .collect(Collectors.toList());
        if (conectados.isEmpty()) {
            enviar(AVISO + "Aún no hay otros usuarios conectados. Escriba "
                    + COMANDO_USUARIOS + " para actualizar la lista.");
            return;
        }
        enviar(AVISO + "Usuarios conectados:");
        conectados.forEach(usuario -> enviar("   - " + usuario));
        if (interlocutor == null) {
            enviar(AVISO + "Escriba el nombre del usuario con el que desea conversar.");
        }
    }

    /**
     * Escribe una línea en el canal de salida del cliente.
     *
     * @param linea texto a enviar
     */
    private void enviar(String linea) {
        if (salida != null) {
            salida.println(linea);
        }
    }

    /**
     * Valida un nombre de usuario: no puede estar vacío, tener espacios, empezar por "/"
     * ni ser la palabra reservada {@value #COMANDO_SALIR}.
     *
     * @param candidato nombre propuesto
     * @return {@code true} si el nombre es válido
     */
    private static boolean esNombreValido(String candidato) {
        return !candidato.isEmpty()
                && !candidato.contains(" ")
                && !candidato.startsWith("/")
                && !candidato.equalsIgnoreCase(COMANDO_SALIR);
    }

    /**
     * Devuelve el nombre del usuario atendido.
     *
     * @return nombre del usuario, o {@code null} si aún no se identifica
     */
    public String getNombre() {
        return nombre;
    }
}
