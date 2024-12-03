import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class ConvertidorMoneda {
    private static final String API_KEY = "b0bcdcf0a215be7daa31d975";
    private static final String API_BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final List<String> MONEDAS_DISPONIBLES = Arrays.asList("ARS", "BOB", "BRL", "CLP", "COP", "USD");
    private static Map<String, Double> tasasDeCambio = new HashMap<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Bienvenido al Convertidor de Moneda");

        while (true) {
            mostrarMenuPrincipal();
            int opcion = leerOpcion(1, 3);

            switch (opcion) {
                case 1:
                    realizarConversion();
                    break;
                case 2:
                    verTasasDeCambio();
                    break;
                case 3:
                    System.out.println("Gracias por usar el Convertidor de Moneda. ¡Hasta luego!");
                    return;
            }
        }
    }

    private static void mostrarMenuPrincipal() {
        System.out.println("\n--- Menú Principal ---");
        System.out.println("1. Realizar una conversión");
        System.out.println("2. Ver tasas de cambio actuales");
        System.out.println("3. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static int leerOpcion(int min, int max) {
        while (true) {
            try {
                int opcion = Integer.parseInt(scanner.nextLine());
                if (opcion >= min && opcion <= max) {
                    return opcion;
                } else {
                    System.out.printf("Por favor, ingrese un número entre %d y %d: ", min, max);
                }
            } catch (NumberFormatException e) {
                System.out.print("Por favor, ingrese un número válido: ");
            }
        }
    }

    private static void realizarConversion() {
        if (tasasDeCambio.isEmpty()) {
            actualizarTasasDeCambio();
        }

        System.out.println("\n--- Realizar Conversión ---");
        System.out.print("Ingrese la cantidad a convertir: ");
        double cantidad = leerCantidad();

        System.out.println("Seleccione la moneda de origen:");
        String monedaOrigen = seleccionarMoneda(MONEDAS_DISPONIBLES);

        System.out.println("Seleccione la moneda de destino:");
        String monedaDestino = seleccionarMoneda(MONEDAS_DISPONIBLES);

        double resultado = convertir(cantidad, monedaOrigen, monedaDestino);
        System.out.printf("%.2f %s = %.2f %s%n", cantidad, monedaOrigen, resultado, monedaDestino);
    }

    private static void verTasasDeCambio() {
        if (tasasDeCambio.isEmpty()) {
            actualizarTasasDeCambio();
        }

        System.out.println("\n--- Tasas de Cambio Actuales ---");
        tasasDeCambio.forEach((moneda, tasa) ->
                System.out.printf("%s: %.4f%n", moneda, tasa));
    }

    private static void actualizarTasasDeCambio() {
        System.out.println("Actualizando tasas de cambio...");
        try {
            HttpResponse<String> respuesta = obtenerTasasDeCambio("USD", String.join(",", MONEDAS_DISPONIBLES));
            manejarRespuesta(respuesta);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error al obtener las tasas de cambio: " + e.getMessage());
        }
    }

    private static HttpResponse<String> obtenerTasasDeCambio(String monedaBase, String monedas) throws IOException, InterruptedException {
        String url = API_BASE_URL + API_KEY + "/latest/" + monedaBase;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void manejarRespuesta(HttpResponse<String> respuesta) {
        int statusCode = respuesta.statusCode();

        if (statusCode == 200) {
            analizarJSON(respuesta.body());
        } else {
            System.out.println("Error en la respuesta. Código de estado: " + statusCode);
            System.out.println("Mensaje de error: " + respuesta.body());
        }
    }

    private static void analizarJSON(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        String reqResult = jsonObject.get("result") != null ? jsonObject.get("result").getAsString() : "error";

        if ("success".equals(reqResult)) {
            String base = jsonObject.get("base") != null ? jsonObject.get("base").getAsString() : "";
            JsonObject rates = jsonObject.getAsJsonObject("conversion_rates");

            tasasDeCambio.clear();
            tasasDeCambio.put(base, 1.0);
            MONEDAS_DISPONIBLES.forEach(moneda -> {
                if (rates != null && rates.has(moneda)) {
                    double rate = rates.get(moneda).getAsDouble();
                    tasasDeCambio.put(moneda, rate);
                }
            });
            System.out.println("Tasas de cambio actualizadas correctamente.");
        } else {
            System.out.println("La solicitud no fue exitosa.");
            if (jsonObject.has("error")) {
                JsonObject error = jsonObject.getAsJsonObject("error");
                System.out.println("Error: " + error.get("info").getAsString());
            } else {
                System.out.println("Error desconocido.");
            }
        }
    }

    private static String seleccionarMoneda(List<String> monedas) {
        for (int i = 0; i < monedas.size(); i++) {
            System.out.println((i + 1) + ". " + monedas.get(i));
        }
        int seleccion = leerOpcion(1, monedas.size()) - 1;
        return monedas.get(seleccion);
    }

    private static double leerCantidad() {
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Por favor, ingrese un número válido: ");
            }
        }
    }

    private static double convertir(double cantidad, String monedaOrigen, String monedaDestino) {
        double tasaOrigen = tasasDeCambio.get(monedaOrigen);
        double tasaDestino = tasasDeCambio.get(monedaDestino);
        return (cantidad / tasaOrigen) * tasaDestino;
    }
}
