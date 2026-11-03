package com.estacionamiento.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.estacionamiento.web.sesion.SesionUsuario;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Unico lugar que habla con la API REST del backend. Agrega el token JWT de la sesion. */
@Component
public class ApiClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> OBJETO = new ParameterizedTypeReference<>() { };

    private final RestClient rest;
    private final SesionUsuario sesion;
    private final ObjectMapper mapper;
    private final String url;

    public ApiClient(@Value("${backend.url}") String url, SesionUsuario sesion, ObjectMapper mapper) {
        this.url = url;
        this.sesion = sesion;
        this.mapper = mapper;
        this.rest = RestClient.builder().baseUrl(url).build();
    }

    public Map<String, Object> autenticar(String username, String password) {
        return proteger(() -> preparar(HttpMethod.POST, "auth/autenticar",
                Map.of("username", username, "password", password)).body(OBJETO));
    }

    public List<Map<String, Object>> lista(String path) {
        return proteger(() -> preparar(HttpMethod.GET, path, null).body(LISTA));
    }

    public Map<String, Object> objeto(String path) {
        return proteger(() -> preparar(HttpMethod.GET, path, null).body(OBJETO));
    }

    /** POST / PUT / DELETE que devuelve un objeto (por ejemplo, la salida de un vehiculo con su importe). */
    public Map<String, Object> objeto(HttpMethod metodo, String path) {
        return proteger(() -> preparar(metodo, path, null).body(OBJETO));
    }

    /** POST / PUT / DELETE cuando no interesa el cuerpo de la respuesta. */
    public void enviar(HttpMethod metodo, String path, Object cuerpo) {
        proteger(() -> preparar(metodo, path, cuerpo).toBodilessEntity());
    }

    private RestClient.ResponseSpec preparar(HttpMethod metodo, String path, Object cuerpo) {
        RestClient.RequestBodySpec req = rest.method(metodo).uri("/api/" + path);
        if (sesion.estaLogueado()) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.getToken());
        }
        if (cuerpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
        }
        return req.retrieve().onStatus(HttpStatusCode::isError, (rq, rs) -> {
            throw traducir(rs);
        });
    }

    private <T> T proteger(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (ResourceAccessException e) {
            throw new ApiException(503, "No se pudo conectar con el backend en " + url + ". Verificá que esté corriendo.");
        }
    }

    /** Convierte la respuesta de error del backend {status, mensaje, detalles} en una excepcion. */
    private ApiException traducir(ClientHttpResponse rs) throws IOException {
        int estado = rs.getStatusCode().value();
        String mensaje = switch (estado) {
            case 401 -> "Sesión inválida o vencida.";
            case 403 -> "No tenés permiso para esta operación.";
            default -> "Error " + estado;
        };
        try {
            JsonNode json = mapper.readTree(rs.getBody());
            mensaje = json.path("mensaje").asText(mensaje);
            for (JsonNode detalle : json.path("detalles")) {
                mensaje += " · " + detalle.asText();
            }
        } catch (IOException ignorado) {
            // el cuerpo no era JSON: se usa el mensaje por defecto
        }
        return new ApiException(estado, mensaje);
    }
}
