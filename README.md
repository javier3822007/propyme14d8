# ProPyme Transparente
## Régimen Art. 14 D N°8 LIR

---

## Requisitos

- **JDK 21 o superior** (Java Development Kit)
  - Windows/Mac/Linux: https://adoptium.net/temurin/releases/?version=21
  - Al instalar, marcar "Add to PATH" en Windows

---

## Cómo ejecutar

### Primera vez (compila y ejecuta):

**Windows:**
```
Doble clic en run.bat
```

**Mac / Linux:**
```bash
chmod +x build.sh
./build.sh
```

La primera vez compila el código fuente y genera `ProPyme.jar`.
Las siguientes veces ejecuta directamente el JAR, en Linux/Mac ejecuta run.sh

---

## Estructura del proyecto

```
ProPyme/
├── src/                    ← Código fuente Java
├── build.sh                ← Construye en Linux/Mac
├── run.sh                  ← Ejecutor Mac/Linux
├── run.bat                 ← Construye Windows
├── run.ps1                 ← Construye Windows (PowerShell)
├── ProPyme.jar             ← Generado al compilar
└── propyme-data/           ← Datos (se crea automatico)
    └── {rut-empresa}/
        ├── empresa.json
        └── {anio}.json
```

**Importante:** La carpeta `propyme-data/` se crea automáticamente en el mismo directorio donde se ejecuta el programa.
Todos los datos quedan locales, sin conexión a internet.

---

## Funcionalidades

- **Gestión de empresas:** Crear, seleccionar y eliminar empresas
- **Datos mensuales:** Ingreso directo o importación desde CSV del SII
- **Recuadro 22:** Base Imponible completa con todos los códigos F-22
- **Recuadro 23:** Capital Propio Tributario Simplificado (CPTS)
- **Certificación F1947:** Distribución a socios y liquidación IGC
- **Balance 8 Columnas:** Balance tributario REFERENCIAL adaptado al regimen simplificado

---

## Solución de problemas

### El programa muestra "Datos parcialmente cargados"

Si aparece este mensaje al abrir un ejercicio, significa que uno o más meses
no pudieron leerse correctamente del archivo JSON (posible corrupción por
fallo de energía o cierre inesperado). Los meses afectados deben reingresarse
manualmente.

Se recomienda restaurar desde un respaldo automático en
**Configuración → Sistema de Respaldo** si el daño es extenso.

### El programa muestra "Archivo JSON corrupto" al cargar

El archivo de datos del ejercicio está truncado o malformado. Causas comunes:

- Cierre inesperado del programa durante el guardado
- Edición manual del archivo `.json`
- Falla de disco o sistema de archivos

**Solución:** Restaurar desde el último respaldo automático en
**Configuración → Sistema de Respaldo**.

### Diferencia de algunos pesos entre el programa y el SII

Pueden aparecer diferencias de $0 a $5 en algunos códigos del F-22.
Esto se debe a redondeos internos del SII que el programa replica lo más
fielmente posible, pero pueden existir diferencias mínimas por tramos de
operaciones donde el SII usa un criterio interno no documentado.

Si la diferencia supera los $10, revisar:

- Que los valores de IVA Débito y Crédito Fiscal estén ingresados correctamente
- Que las Notas de Crédito y Débito coincidan con el RCV oficial

###La aplicación utiliza `double` + redondeo explícito en lugar de `BigDecimal`.

Motivos:
- El sistema opera principalmente con pesos chilenos enteros.
- Se prioriza simplicidad, mantenibilidad y legibilidad.
- Los cálculos tributarios manejados por la aplicación no requieren precisión decimal arbitraria.
- Los valores monetarios relevantes son redondeados explícitamente antes de persistencia o presentación.

Esta decisión es intencional.

*Sistema desarrollado para el régimen Pro Pyme Transparente Art. 14 D N°8 LIR*

