## Arquitectura de la aplicación y decisiones de diseño

La arquitectura del sistema se ha diseñado siguiendo el patrón Modelo-Vista-Controlador (MVC), desacoplando la lógica de negocio, la interfaz de usuario y la persistencia de datos. Esta separación de responsabilidades facilita el mantenimiento y la escalabilidad del proyecto, cumpliendo con los requisitos arquitectónicos de la asignatura.

### 4.1. Estructura de Paquetes y Tecnologías

El proyecto, gestionado mediante **Maven**, se organiza en paquetes que reflejan las capas lógicas de la aplicación:

* **`es.um.gestiongastos.ui` (Vista):** Contiene la interfaz gráfica implementada con **JavaFX**. Se ha optado por separar la definición visual (clases como `PanelGestionGastos`, `PanelInformes`) de la lógica de control.
* **`es.um.gestiongastos.model` (Modelo):** Define el dominio del problema (`Gasto`, `Persona`, `GastosCompartidos`). Se ha implementado un modelo donde las clases además de contener datos contienen funciones relacionadas con la obtención y el cálculo de los mismos, encapsulando la mencionada lógica de negocio.
* **`es.um.gestiongastos.controlador` (Controlador):** La clase `Controlador` actúa como orquestador único, gestionando el flujo entre la vista y el modelo.
* **`es.um.gestiongastos.persistencia` (Infraestructura):** Encarga del almacenamiento de datos.

---

### 4.2. Estrategia de Persistencia (Patrón Repositorio y DAO)

Para la persistencia de datos se ha implementado el **Patrón Repositorio** a través de la clase `RepositorioJSON`.

* **Decisión de Diseño:** Se ha utilizado la librería **Jackson** para la serialización de objetos a JSON.
* **Optimización (Caché en memoria):** Para evitar la latencia de lectura/escritura continua en disco, al iniciar la aplicación, todos los datos se cargan en mapas en memoria (`Map<String, Persona>`). Las operaciones de lectura consultan esta memoria (instantáneo), mientras que las operaciones de escritura actualizan la memoria y vuelcan los datos a los ficheros JSON (`datos_usuarios.json`, `datos_categorias.json`) para garantizar la integridad.

---

### 4.3. Patrones de Diseño Aplicados

Se han aplicado diversos patrones de diseño para resolver problemas de extensibilidad y gestión de instancias:

1. **Patrón Singleton:**
* *Ubicación:* Clase `Controlador`.
* *Justificación:* Garantiza que existe una única instancia del gestor principal durante toda la vida de la aplicación, centralizando el acceso al repositorio y el estado del usuario autenticado (`Controlador.getInstancia()`).


2. **Patrón Estrategia:**
* *Ubicación:* Sistema de Alertas (`EstrategiaAlerta`, `EstrategiaPorUmbral`).
* *Justificación:* Permite definir algoritmos intercambiables para comprobar si una alerta debe dispararse. Actualmente se implementa la estrategia por umbral de importe, pero el diseño queda abierto a futuras estrategias (ej. alerta por variación porcentual) sin modificar la clase `Alerta`.


3. **Patrones Método Factoría y Adaptador:**
* *Ubicación:* Módulo de importación (`es.um.gestiongastos.importer`).
* *Justificación:* Para soportar múltiples formatos de ficheros externos (CSV, JSON, YAML):
* **Factory Method:** La clase `ImportadorFactory` decide dinámicamente qué importador instanciar según la extensión del archivo.
* **Adapter:** Las clases `AdaptadorCSV`, `AdaptadorJSON`, etc., implementan la interfaz común `IImportadorCuenta`, traduciendo los datos externos al modelo interno de la aplicación.


4. **Patrón Observer:**
* *Ubicación:* `Controlador` y Vistas.
* *Justificación:* Se utiliza un mecanismo de retrollamada (`Runnable onModeloCambiado`) para que el Controlador notifique a la interfaz gráfica cuando hay cambios en los datos (como tras una importación o registro de gasto), provocando un refresco automático de las pantallas sin acoplar las clases.


5. **Data Transfer Object (DTO):**
* *Ubicación:* Paquete `importer` (`GastoDTO`, `CuentaDTO`).
* *Justificación:* Se utilizan objetos simples para transportar los datos brutos desde los ficheros de importación hasta el sistema, desacoplando la estructura del archivo externo de las entidades de dominio finales.

---

### 4.4 Gestión de Dependencias (Maven)
El archivo pom.xml refleja una selección tecnológica estandarizada:

- JavaFX 21: Se utiliza la última versión, con los módulos javafx-controls y javafx-fxml declarados explícitamente.
- Jackson 2.15: Se incluye el módulo jackson-datatype-jsr310. Esta decisión permite serializar correctamente tipos de fecha modernos como LocalDate (usados en la clase Gasto), algo que las librerías antiguas no soportan nativamente.
