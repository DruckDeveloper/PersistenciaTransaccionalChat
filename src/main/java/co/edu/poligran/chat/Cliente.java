package co.edu.poligran.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Cliente de consola del chat bidireccional.
 * <p>
 * Pide la IP del servidor, el puerto y el nombre de usuario, y se conecta con un {@link Socket}.
 * Usa dos hilos para que la comunicación sea bidireccional y simultánea:
 * </p>
 * <ul>
 *     <li>El hilo principal lee lo que llega del servidor y lo muestra en pantalla.</li>
 *     <li>Un hilo secundario lee el teclado y envía cada línea al servidor.</li>
 * </ul>
 * <p>
 * Uso: {@code java -cp <classpath> co.edu.poligran.chat.Cliente}
 * </p>
 *
 * @author SubGrupo 3 - Grupo B02
 * @version 1.0.0
 */
public class Cliente {

    /** IP que se usa cuando el usuario no escribe ninguna (equipo local). */
    private static final String IP_POR_DEFECTO = "127.0.0.1";

    /** Dirección IP o nombre de host del servidor. */
    private final String ip;

    /** Puerto TCP del servidor. */
    private final int puerto;

    /** Nombre con el que el usuario se identifica en el chat. */
    private final String nombre;

    /**
     * Crea un cliente con los datos de conexión.
     *
     * @param ip     IP o nombre de host del servidor
     * @param puerto puerto del servidor
     * @param nombre nombre de usuario
     */
    public Cliente(String ip, int puerto, String nombre) {
        this.ip = ip;
        this.puerto = puerto;
        this.nombre = nombre;
    }

    /**
     * Se conecta al servidor, envía el nombre de usuario y mantiene la conversación
     * hasta que el servidor cierra la conexión, por ejemplo después de enviar {@code chao}.
     *
     * @param consola lector del teclado, compartido con la lectura de los datos de conexión
     * @throws IOException si no es posible conectarse o se pierde la conexión
     */
    public void conectar(BufferedReader consola) throws IOException {
        try (Socket socket = new Socket(ip, puerto);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Conectado a " + ip + ":" + puerto);
            salida.println(nombre);
            iniciarEnvioDesdeTeclado(consola, socket, salida);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
            }
        }
        System.out.println("Conexión con el servidor finalizada.");
    }

    /**
     * Inicia el hilo que lee el teclado y envía cada línea al servidor.
     * El hilo es de tipo <i>daemon</i>, así que no impide que el programa termine
     * cuando el servidor cierra la conexión. Si la entrada del teclado se cierra
     * (Ctrl+Z en Windows o Ctrl+D en Linux), el hilo le indica al servidor que no
     * enviará más datos, y el servidor cierra la sesión.
     *
     * @param consola lector del teclado
     * @param socket  socket conectado con el servidor
     * @param salida  canal de escritura hacia el servidor
     */
    private void iniciarEnvioDesdeTeclado(BufferedReader consola, Socket socket, PrintWriter salida) {
        Thread hilo = new Thread(() -> {
            try {
                String linea;
                while ((linea = consola.readLine()) != null) {
                    salida.println(linea);
                    if (linea.trim().equalsIgnoreCase(ManejadorCliente.COMANDO_SALIR)) {
                        return;
                    }
                }
                socket.shutdownOutput();
            } catch (IOException e) {
                System.err.println("Error leyendo el teclado: " + e.getMessage());
            }
        }, "envio-teclado");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Pide un dato por consola y devuelve el valor por defecto si el usuario solo presiona Enter.
     *
     * @param consola       lector del teclado
     * @param etiqueta      texto que se muestra al usuario
     * @param porDefecto    valor si la respuesta está vacía, o {@code null} si el dato es obligatorio
     * @return el valor escrito o el valor por defecto
     * @throws IOException si se cierra la entrada estándar
     */
    private static String preguntar(BufferedReader consola, String etiqueta, String porDefecto) throws IOException {
        while (true) {
            System.out.print(porDefecto == null ? etiqueta + ": " : etiqueta + " [" + porDefecto + "]: ");
            String respuesta = consola.readLine();
            if (respuesta == null) {
                throw new IOException("Entrada estándar cerrada");
            }
            respuesta = respuesta.trim();
            if (!respuesta.isEmpty()) {
                return respuesta;
            }
            if (porDefecto != null) {
                return porDefecto;
            }
        }
    }

    /**
     * Determina la codificación con la que la consola entrega el texto del teclado.
     * En Windows la consola suele usar una página de códigos como la 850, no UTF-8. Si se
     * leyera con otra codificación, las tildes y la ñ llegarían dañadas al otro usuario.
     *
     * @return codificación de la consola, o la codificación por defecto de la JVM si no se conoce
     */
    private static Charset codificacionConsola() {
        String nombre = System.getProperty("stdin.encoding",
                System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding")));
        try {
            return nombre == null ? Charset.defaultCharset() : Charset.forName(nombre);
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset();
        }
    }

    /**
     * Punto de entrada del cliente: pide los datos de conexión e inicia el chat.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        BufferedReader consola = new BufferedReader(new InputStreamReader(System.in, codificacionConsola()));
        try {
            String ip = preguntar(consola, "IP del servidor", IP_POR_DEFECTO);
            int puerto = Integer.parseInt(
                    preguntar(consola, "Puerto de la conexión", String.valueOf(Servidor.PUERTO_POR_DEFECTO)));
            String nombre = preguntar(consola, "Escriba su nombre", null);
            new Cliente(ip, puerto, nombre).conectar(consola);
        } catch (NumberFormatException e) {
            System.err.println("El puerto debe ser un número.");
        } catch (IOException e) {
            System.err.println("No fue posible conectarse al servidor: " + e.getMessage());
        }
    }
}
