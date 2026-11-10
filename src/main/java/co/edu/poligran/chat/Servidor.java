package co.edu.poligran.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Servidor del chat bidireccional basado en sockets TCP.
 * <p>
 * Abre un {@link ServerSocket} en el puerto indicado y espera conexiones de clientes.
 * Atiende cada conexión en un hilo propio mediante un {@link ManejadorCliente}.
 * Lleva el registro de los usuarios conectados y muestra en consola el log de eventos:
 * conexiones, conversaciones, mensajes y abandonos.
 * </p>
 * <p>
 * Uso: {@code java -cp <classpath> co.edu.poligran.chat.Servidor [puerto]}
 * </p>
 *
 * @author SubGrupo 3 - Grupo B02
 * @version 1.0.0
 */
public class Servidor {

    /** Puerto que se usa cuando no se indica otro; es el mismo del ejemplo de la guía. */
    public static final int PUERTO_POR_DEFECTO = 59420;

    /** Formato de la hora con la que se marca cada línea del log. */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Puerto TCP en el que escucha el servidor. */
    private final int puerto;

    /** Usuarios conectados. La clave es el nombre en minúsculas, así que los nombres no distinguen mayúsculas. */
    private final Map<String, ManejadorCliente> usuarios = new ConcurrentHashMap<>();

    /** Grupo de hilos que atiende a los clientes de forma concurrente. */
    private final ExecutorService hilos = Executors.newCachedThreadPool();

    /**
     * Crea un servidor que escuchará en el puerto indicado.
     *
     * @param puerto puerto TCP entre 1 y 65535
     */
    public Servidor(int puerto) {
        this.puerto = puerto;
    }

    /**
     * Abre el socket servidor y acepta clientes indefinidamente.
     * Entrega cada conexión aceptada a un {@link ManejadorCliente} que corre en su propio hilo.
     *
     * @throws IOException si el puerto está ocupado o falla la red
     */
    public void iniciar() throws IOException {
        try (ServerSocket socketServidor = new ServerSocket(puerto)) {
            registrar("Servidor iniciado y contestando OK en el puerto " + puerto);
            while (!socketServidor.isClosed()) {
                Socket socketCliente = socketServidor.accept();
                hilos.execute(new ManejadorCliente(socketCliente, this));
            }
        } finally {
            hilos.shutdownNow();
        }
    }

    /**
     * Registra un usuario como conectado si su nombre no está en uso.
     *
     * @param cliente manejador del cliente que se identificó
     * @return {@code true} si quedó registrado; {@code false} si ya existe un usuario con ese nombre
     */
    boolean conectar(ManejadorCliente cliente) {
        boolean registrado = usuarios.putIfAbsent(clave(cliente.getNombre()), cliente) == null;
        if (registrado) {
            registrar("Usuario \"" + cliente.getNombre() + "\" conectado");
        }
        return registrado;
    }

    /**
     * Retira a un usuario de la lista de conectados y avisa a quienes conversaban con él.
     * Si el cliente nunca llegó a registrarse, no hace nada.
     *
     * @param cliente manejador del cliente que abandona el chat
     */
    void desconectar(ManejadorCliente cliente) {
        if (cliente.getNombre() == null || !usuarios.remove(clave(cliente.getNombre()), cliente)) {
            return;
        }
        registrar("El usuario \"" + cliente.getNombre() + "\" abandonó");
        usuarios.values().forEach(otro -> otro.interlocutorAbandono(cliente));
    }

    /**
     * Busca un usuario conectado por su nombre, sin distinguir mayúsculas.
     *
     * @param nombre nombre del usuario
     * @return el manejador del usuario, o vacío si no está conectado
     */
    Optional<ManejadorCliente> buscar(String nombre) {
        return Optional.ofNullable(usuarios.get(clave(nombre)));
    }

    /**
     * Devuelve los nombres de los usuarios conectados en orden alfabético.
     *
     * @return lista de nombres
     */
    List<String> nombresConectados() {
        return usuarios.values().stream()
                .map(ManejadorCliente::getNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Escribe un evento en el log del servidor (consola) con la hora actual.
     *
     * @param evento descripción del evento
     */
    void registrar(String evento) {
        System.out.println("[" + LocalTime.now().format(FORMATO_HORA) + "] " + evento);
    }

    /**
     * Normaliza un nombre de usuario para usarlo como clave del mapa.
     *
     * @param nombre nombre original
     * @return nombre en minúsculas
     */
    private static String clave(String nombre) {
        return nombre.toLowerCase(Locale.ROOT);
    }

    /**
     * Punto de entrada del servidor.
     *
     * @param args opcionalmente, el puerto en la primera posición
     */
    public static void main(String[] args) {
        int puerto = PUERTO_POR_DEFECTO;
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido: " + args[0]);
                return;
            }
        }
        try {
            new Servidor(puerto).iniciar();
        } catch (IOException e) {
            System.err.println("No fue posible iniciar el servidor: " + e.getMessage());
        }
    }
}
