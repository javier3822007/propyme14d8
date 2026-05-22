/*
 * ProPyme Transparente — Sistema de declaración tributaria 14D N°8
 * Copyright (C) 2026 Javier Ignacio Aguilar G. <javier.aguilar382@protonmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License v3 as published by the
 * Free Software Foundation. See the LICENSE file for the full text.
 *
 * Distributed WITHOUT ANY WARRANTY. See https://www.gnu.org/licenses/agpl-3.0.html
 */
package cl.propyme.service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * BackupService — Sistema de respaldo de la carpeta propyme-data.
 *
 * Crea archivos ZIP con timestamp en propyme-data/backups/.
 * El módulo es INDEPENDIENTE — no toca DataStore ni los archivos de datos
 * existentes. Solo lee de propyme-data/ y escribe a propyme-data/backups/.
 *
 * Soporta:
 *   - Backup manual:    crear() — genera ZIP inmediatamente
 *   - Backup automático: crearAuto() — se invoca al guardar, sin diálogos
 *   - Restauración:     restaurar(path) — descomprime un ZIP a propyme-data/
 *   - Listado:          listarBackups() — devuelve los respaldos disponibles
 *
 * Política de retención automática: conserva los últimos N backups automáticos
 * (default: 10) para no llenar el disco.
 */
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class.getName());

    private static final String DATA_DIR    = "propyme-data";
    private static final String BACKUP_DIR  = "backups";
    private static final String AUTO_PREFIX = "auto_";
    private static final String MAN_PREFIX  = "manual_";
    private static final int    MAX_AUTO_BACKUPS = 10;

    private static final DateTimeFormatter TS_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    // ── Rutas ─────────────────────────────────────────────────────────────────

    private static Path dataPath(String baseDir) {
        return Paths.get(baseDir, DATA_DIR);
    }

    private static Path backupDirPath(String baseDir) {
        return Paths.get(baseDir, DATA_DIR, BACKUP_DIR);
    }

    // ── Crear backup ──────────────────────────────────────────────────────────

    /** Crea un backup MANUAL con timestamp. Devuelve la ruta del archivo creado. */
    public static Path crear(String baseDir) throws IOException {
        return crearInterno(baseDir, MAN_PREFIX);
    }

    /**
     * Crea un backup AUTOMÁTICO. No lanza excepciones — falla silenciosamente.
     * Se invoca al guardar para no interrumpir el flujo del usuario.
     * Aplica retención de los últimos MAX_AUTO_BACKUPS.
     */
    public static Path crearAuto(String baseDir) {
        try {
            Path p = crearInterno(baseDir, AUTO_PREFIX);
            limpiarAutosViejos(baseDir);
            return p;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Falló backup automático", ex);
            return null;
        }
    }

    private static Path crearInterno(String baseDir, String prefix) throws IOException {
        Path origen = dataPath(baseDir);
        if (!Files.isDirectory(origen)) {
            throw new IOException("No existe la carpeta de datos: " + origen);
        }
        Path destinoDir = backupDirPath(baseDir);
        Files.createDirectories(destinoDir);

        String nombre = prefix + LocalDateTime.now().format(TS_FMT) + ".zip";
        Path destino = destinoDir.resolve(nombre);

        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW))) {
            comprimirDirectorio(origen, origen, zos);
        }
        return destino;
    }

    private static void comprimirDirectorio(Path raiz, Path actual, ZipOutputStream zos)
            throws IOException {
        try (var stream = Files.newDirectoryStream(actual)) {
            for (Path entrada : stream) {
                // Saltar la propia carpeta de backups para evitar recursión
                if (entrada.getFileName().toString().equals(BACKUP_DIR)) continue;

                String relativa = raiz.relativize(entrada).toString().replace('\\','/');
                if (Files.isDirectory(entrada)) {
                    zos.putNextEntry(new ZipEntry(relativa + "/"));
                    zos.closeEntry();
                    comprimirDirectorio(raiz, entrada, zos);
                } else {
                    zos.putNextEntry(new ZipEntry(relativa));
                    Files.copy(entrada, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    // ── Limpieza de auto-backups antiguos ─────────────────────────────────────

    private static void limpiarAutosViejos(String baseDir) {
        try {
            List<Path> autos = listarConPrefijo(baseDir, AUTO_PREFIX);
            // Ordenar más antiguos primero
            Collections.sort(autos);
            int sobran = autos.size() - MAX_AUTO_BACKUPS;
            for (int i = 0; i < sobran; i++) {
                try { Files.deleteIfExists(autos.get(i)); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ── Listar backups ────────────────────────────────────────────────────────

    public static List<Path> listarBackups(String baseDir) {
        List<Path> result = new ArrayList<>();
        Path dir = backupDirPath(baseDir);
        if (!Files.isDirectory(dir)) return result;
        try (var stream = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path p : stream) result.add(p);
        } catch (IOException ignored) {}
        // Más recientes primero
        result.sort(Collections.reverseOrder());
        return result;
    }

    private static List<Path> listarConPrefijo(String baseDir, String prefijo) throws IOException {
        List<Path> result = new ArrayList<>();
        Path dir = backupDirPath(baseDir);
        if (!Files.isDirectory(dir)) return result;
        try (var stream = Files.newDirectoryStream(dir, prefijo + "*.zip")) {
            for (Path p : stream) result.add(p);
        }
        return result;
    }

    // ── Restaurar backup ──────────────────────────────────────────────────────

    /**
     * Restaura un backup. ATENCIÓN: sobreescribe los datos actuales.
     * Antes de restaurar, crea un backup automático de seguridad.
     */
    public static void restaurar(String baseDir, Path archivoZip) throws IOException {
        if (!Files.isRegularFile(archivoZip))
            throw new IOException("El archivo de respaldo no existe: " + archivoZip);

        // Seguridad: backup del estado actual antes de restaurar
        crearAuto(baseDir);

        Path destino = dataPath(baseDir);
        Files.createDirectories(destino);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path salida = destino.resolve(entry.getName()).normalize();
                if (!salida.startsWith(destino)) continue; // protección contra zip-slip
                if (entry.isDirectory()) {
                    Files.createDirectories(salida);
                } else {
                    Files.createDirectories(salida.getParent());
                    Files.copy(zis, salida, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    // ── Helpers de presentación ───────────────────────────────────────────────

    public static String descripcionBackup(Path archivo) {
        String nombre = archivo.getFileName().toString();
        String tipo   = nombre.startsWith(AUTO_PREFIX) ? "Automático" : "Manual";
        try {
            long tam = Files.size(archivo);
            return tipo + "  ·  " + nombre + "  ·  " + formatearTam(tam);
        } catch (IOException e) {
            return tipo + "  ·  " + nombre;
        }
    }

    private static String formatearTam(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
