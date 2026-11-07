package com.monolito.historial.util;

/**
 * Tipos de archivo admitidos como adjuntos (facturas): PDF y fotos JPG, PNG o WebP.
 * El tipo se averigua mirando los primeros bytes del archivo: no se fía del nombre
 * ni del tipo que declara el navegador, que se pueden falsear.
 */
public final class TiposArchivo {

    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";

    private TiposArchivo() {
    }

    /** Devuelve el tipo real del archivo o null si no es un PDF ni una imagen admitida. */
    public static String detectar(byte[] datos) {
        if (datos == null) {
            return null;
        }
        if (contiene(datos, "%PDF-", 1024)) {
            return PDF;
        }
        if (empieza(datos, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (empieza(datos, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (datos.length >= 12 && empieza(datos, 'R', 'I', 'F', 'F')
                && datos[8] == 'W' && datos[9] == 'E' && datos[10] == 'B' && datos[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    /** Deja solo el nombre del archivo (sin rutas ni caracteres de control) y lo limita a 150 caracteres. */
    public static String limpiarNombre(String original) {
        if (original == null) {
            return "archivo";
        }
        String nombre = original.replace('\\', '/');
        int barra = nombre.lastIndexOf('/');
        if (barra >= 0) {
            nombre = nombre.substring(barra + 1);
        }
        nombre = nombre.replaceAll("\\p{Cntrl}", "").trim();
        if (nombre.isEmpty()) {
            return "archivo";
        }
        // Si es muy largo se conserva el final, donde está la extensión
        return nombre.length() > 150 ? nombre.substring(nombre.length() - 150) : nombre;
    }

    private static boolean empieza(byte[] datos, int... esperado) {
        if (datos.length < esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((datos[i] & 0xFF) != esperado[i]) {
                return false;
            }
        }
        return true;
    }

    // Busca el texto dentro de los primeros "limite" bytes (algunos PDF llevan un salto de línea delante)
    private static boolean contiene(byte[] datos, String texto, int limite) {
        int max = Math.min(datos.length, limite) - texto.length();
        for (int i = 0; i <= max; i++) {
            boolean coincide = true;
            for (int j = 0; j < texto.length(); j++) {
                if (datos[i + j] != (byte) texto.charAt(j)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                return true;
            }
        }
        return false;
    }
}
