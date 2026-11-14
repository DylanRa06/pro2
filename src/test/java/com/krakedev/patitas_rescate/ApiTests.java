package com.krakedev.patitas_rescate;

import java.time.Instant;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krakedev.patitas_rescate.repositories.MascotaRepository;
import com.krakedev.patitas_rescate.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MascotaRepository mascotas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired com.krakedev.patitas_rescate.config.JwtUtil jwt;
    @Autowired com.krakedev.patitas_rescate.services.TokenBlacklistService blacklist;
    private String adminToken;

    @BeforeEach void limpiar() {
        mascotas.deleteAll(); usuarios.deleteAll();
        var admin = new com.krakedev.patitas_rescate.entidades.Usuario();
        admin.setUsername("voluntario_test"); admin.setRol("ADMIN");
        adminToken = jwt.generarToken(admin.getUsername(), admin.getRol());
    }

    @Test void mascotasSePersistenSeRecortanYSiempreSonDisponibles() throws Exception {
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .content("""
                {"id":99,"nombre":"  Luna  ","especie":"  Perro ","edad":2,"estado":"ADOPTADA"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Luna"))
                .andExpect(jsonPath("$.especie").value("Perro"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .content("""
                {"nombre":"Milo","especie":"Gato","edad":1}
                """)).andExpect(status().isCreated());
        assertEquals(2, mascotas.count());
        mvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test void rechazaMascotasInvalidasSinPersistir() throws Exception {
        for (String body : new String[]{
                "{\"nombre\":\" \",\"especie\":\"Perro\",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\" \",\"edad\":2}",
                "{\"nombre\":\"Luna\",\"especie\":\"Perro\",\"edad\":-1}",
                "{\"edad\":1}", "json incorrecto"}) {
            mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, mascotas.count());
    }

    @Test void registroProtegeClaveYNoPermiteElegirRol() throws Exception {
        registrar();
        var u = usuarios.findByUsername("adoptante_demo").orElseThrow();
        assertTrue(u.getPassword().startsWith("$2a$10$"));
        assertTrue(encoder.matches("ClaveDemo2026!", u.getPassword()));
        assertEquals("USER", u.getRol());
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"OtraClave"}
                """)).andExpect(status().isBadRequest());
        assertEquals(1, usuarios.count());
    }

    @Test void registroRechazaCamposVacios() throws Exception {
        for (String body : new String[]{
                "{\"username\":\" \",\"password\":\"clave\"}",
                "{\"username\":\"usuario\",\"password\":\" \"}", "{}"}) {
            mvc.perform(post("/api/auth/registrar").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, usuarios.count());
    }

    @Test void loginEmiteTokenFirmadoYFallaSinRevelarLaCausa() throws Exception {
        registrar();
        var respuesta = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"ClaveDemo2026!"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(respuesta).get("token").asText();
        var decoded = JWT.require(Algorithm.HMAC256("secreto-exclusivo-de-pruebas-automaticas-32"))
                .withIssuer("patitas-rescate").build().verify(token);
        assertEquals("adoptante_demo", decoded.getSubject());
        assertEquals("USER", decoded.getClaim("rol").asString());
        assertTrue(decoded.getExpiresAtAsInstant().isAfter(Instant.now()));
        assertFalse(decoded.getClaims().containsKey("password"));
        String anterior = null;
        for (String body : new String[]{
                "{\"username\":\"adoptante_demo\",\"password\":\"incorrecta\"}",
                "{\"username\":\"noexiste\",\"password\":\"incorrecta\"}"}) {
            String error = mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist())
                    .andReturn().getResponse().getContentAsString();
            if (anterior != null) assertEquals(anterior, error);
            anterior = error;
        }
    }


    private String loginUsuario() throws Exception {
        registrar();
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"adoptante_demo\",\"password\":\"ClaveDemo2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test void usuarioConsultaPeroNoRegistraMascotas() throws Exception {
        String token = loginUsuario();
        mvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("adoptante_demo"))
                .andExpect(jsonPath("$.rol").value("USER"));
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"nombre\":\"Luna\",\"especie\":\"Perro\",\"edad\":2}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").exists());
        assertEquals(0, mascotas.count());
    }

    @Test void logoutRevocaSoloEseTokenYPermiteNuevoLogin() throws Exception {
        String token = loginUsuario();
        var u = usuarios.findByUsername("adoptante_demo").orElseThrow();
        String otro = jwt.generarToken(u.getUsername(), u.getRol());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertTrue(blacklist.estaRevocado(token));
        for (String ruta : new String[]{"/api/auth/perfil", "/api/mascotas"}) {
            mvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + otro))
                .andExpect(status().isOk());
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"adoptante_demo\",\"password\":\"ClaveDemo2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/auth/perfil").header("Authorization", "Bearer " + json.readTree(body).get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test void rutasProtegidasRechazanAusenciaDeToken() throws Exception {
        mvc.perform(get("/api/auth/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test void rechazaTokensAlteradosExpiradosYOtroEmisor() throws Exception {
        var algoritmo = Algorithm.HMAC256("secreto-exclusivo-de-pruebas-automaticas-32");
        String alterado = (adminToken.charAt(0) == 'a' ? "b" : "a") + adminToken.substring(1);
        String expirado = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "USER").withJWTId("vencido")
                .withExpiresAt(Instant.now().minusSeconds(60)).sign(algoritmo);
        String otroEmisor = JWT.create().withIssuer("otro").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("otro")
                .withExpiresAt(Instant.now().plusSeconds(60)).sign(algoritmo);
        String sinExpiracion = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("sin-exp").sign(algoritmo);
        String firmaFalsa = JWT.create().withIssuer("patitas-rescate").withSubject("usuario")
                .withClaim("rol", "ADMIN").withJWTId("falso").withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256("otra-clave-que-no-es-la-del-servidor"));
        for (String header : new String[]{"Bearer " + alterado, "Bearer " + expirado,
                "Bearer " + otroEmisor, "Bearer " + sinExpiracion, "Bearer " + firmaFalsa,
                "Bearer basura", "Bearer ", "Basic abc"}) {
            mvc.perform(get("/api/auth/perfil").header("Authorization", header))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensaje").exists());
        }
    }


    private long crearMascota(String nombre) throws Exception {
        String respuesta = mvc.perform(post("/api/mascotas")
                .header("Authorization", "Bearer " + adminToken).contentType("application/json")
                .content("{\"nombre\":\"" + nombre + "\",\"especie\":\"Perro\",\"edad\":2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }

    @Test void adopcionActualizaSoloUnaMascotaYLaExcluyeDeDisponibles() throws Exception {
        long luna = crearMascota("Luna");
        long milo = crearMascota("Milo");
        String user = loginUsuario();
        mvc.perform(get("/api/mascotas/disponibles").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/mascotas/" + luna + "/adoptar").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        assertEquals("DISPONIBLE", mascotas.findById(luna).orElseThrow().getEstado());
        mvc.perform(put("/api/mascotas/" + luna + "/adoptar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ADOPTADA"));
        mvc.perform(put("/api/mascotas/" + luna + "/adoptar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/mascotas/999999/adoptar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/mascotas/disponibles").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(milo));
        mvc.perform(get("/api/mascotas").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertEquals("ADOPTADA", mascotas.findById(luna).orElseThrow().getEstado());
        assertEquals("DISPONIBLE", mascotas.findById(milo).orElseThrow().getEstado());
    }

    @Test void adopcionesConcurrentesNoPuedenConfirmarDosVeces() throws Exception {
        long id = crearMascota("Luna");
        var inicio = new java.util.concurrent.CountDownLatch(1);
        try (var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> adoptar = () -> {
                inicio.await();
                return mvc.perform(put("/api/mascotas/" + id + "/adoptar")
                        .header("Authorization", "Bearer " + adminToken)).andReturn().getResponse().getStatus();
            };
            var primera = ejecutor.submit(adoptar);
            var segunda = ejecutor.submit(adoptar);
            inicio.countDown();
            var resultados = new java.util.ArrayList<>(java.util.List.of(
                    primera.get(15, java.util.concurrent.TimeUnit.SECONDS),
                    segunda.get(15, java.util.concurrent.TimeUnit.SECONDS)));
            java.util.Collections.sort(resultados);
            assertEquals(java.util.List.of(200, 409), resultados);
        }
        assertEquals("ADOPTADA", mascotas.findById(id).orElseThrow().getEstado());
    }

    @Test void disponiblesYAdopcionExigenTokenYRechazanTokenRevocado() throws Exception {
        long id = crearMascota("Luna");
        mvc.perform(get("/api/mascotas/disponibles")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/mascotas/" + id + "/adoptar")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/mascotas/disponibles").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/mascotas/" + id + "/adoptar").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
        assertEquals("DISPONIBLE", mascotas.findById(id).orElseThrow().getEstado());
    }

    @Test void blacklistEnMemoriaNoSeComparteConUnaNuevaInstancia() {
        String token = adminToken;
        var primera = new com.krakedev.patitas_rescate.services.TokenBlacklistService();
        primera.revocar(JWT.decode(token));
        assertTrue(primera.estaRevocado(token));
        assertFalse(new com.krakedev.patitas_rescate.services.TokenBlacklistService().estaRevocado(token));
        String vencido = JWT.create().withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256("clave-prueba"));
        primera.revocar(JWT.decode(vencido));
        assertFalse(primera.estaRevocado(vencido));
    }

    @Test void idDelClienteNoPermiteSobrescribirUsuarioNiMascota() throws Exception {
        long idMascota = crearMascota("Original");
        mvc.perform(post("/api/mascotas").header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content("{\"id\":" + idMascota
                        + ",\"nombre\":\"Nueva\",\"especie\":\"Gato\",\"edad\":1}"))
                .andExpect(status().isCreated());
        assertEquals("Original", mascotas.findById(idMascota).orElseThrow().getNombre());
        assertEquals(2, mascotas.count());
        registrar();
        var idUsuario = usuarios.findByUsername("adoptante_demo").orElseThrow().getId();
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("{\"id\":" + idUsuario + ",\"username\":\"otro_usuario\",\"password\":\"ClaveDemo2026!\"}"))
                .andExpect(status().isCreated());
        assertEquals("adoptante_demo", usuarios.findById(idUsuario).orElseThrow().getUsername());
        assertEquals(2, usuarios.count());
    }

    private void registrar() throws Exception {
        mvc.perform(post("/api/auth/registrar").contentType("application/json")
                .content("""
                {"username":"adoptante_demo","password":"ClaveDemo2026!","rol":"ADMIN"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
