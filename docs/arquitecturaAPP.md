# Memoria Técnica: Arquitectura y Decisiones de Diseño

## 1. Arquitectura del Sistema: Modelo-Vista-Controlador (MVC)
La aplicación implementa el patrón MVC, desacoplando la lógica de negocio de la interfaz de usuario para facilitar la mantenibilidad del proyecto.

### El Modelo (Rich Domain Model)
A diferencia de los modelos anémicos donde las clases son meros contenedores de datos, el diseño implementado incluye lógica de negocio directamente en las entidades.

* Evidencia en Código: En la clase Gasto.java, el método getCostePara(Persona p) encapsula la lógica matemática necesaria para calcular la parte proporcional de un gasto según el peso del usuario en la cuenta. Esto centraliza las reglas de negocio en el modelo.
* Preparación para Serialización: Las clases del modelo están decoradas con anotaciones de la librería Jackson (como @JsonIdentityInfo y @JsonFormat), lo que permite que los objetos sean persistidos directamente a JSON sin necesidad de clases intermedias.

### El Controlador (Orquestador)
La clase Controlador.java actúa como el punto de entrada único y gestor del flujo de la aplicación.

* Evidencia en Código: Este componente gestiona el flujo completo, desde la autenticación (método autenticar) hasta la lógica de alertas. Su diseño asegura que la Vista nunca acceda directamente al Modelo o a la Persistencia, sino que solicite operaciones al Controlador.

---

## 2. Patrones de Diseño Aplicados
Para resolver problemas recurrentes de ingeniería de software, se han utilizado los siguientes patrones estándar:

### Patrón Singleton (Creacional)
Se utiliza para asegurar que existe una única instancia del gestor lógico (Controlador) durante toda la ejecución del programa.
- Implementación: Se observa en el método estático getInstancia() y en el constructor privado, que impiden la creación de múltiples instancias que podrían desincronizar los datos.

### Patrón Observer (Comportamiento)
Se ha implementado un mecanismo para desacoplar el controlador de la vista y permitir actualizaciones reactivas.
- Implementación: El controlador define un Runnable onModeloCambiado. Cuando ocurre una operación de escritura (como registrarGasto o importarCuenta), se invoca el método notificarModeloCambiado(). Esto permite que la interfaz gráfica se redibuje automáticamente sin que el Controlador conozca los detalles de la ventana.

### Patrón Strategy (Comportamiento)
Detectado en el sistema de alertas para permitir flexibilidad en las reglas de negocio.
- Implementación: En el método crearAlerta, se instancia una new EstrategiaPorUmbral(umbralMaximo). Esto permite que el comportamiento de cuándo salta una alerta sea intercambiable.

### Patrón Data Transfer Object (DTO)
Utilizado durante la importación de datos para separar la estructura de archivos externos de la estructura interna del dominio.
- Implementación: El método importarCuenta utiliza objetos auxiliares como CuentaDTO y GastoDTO para procesar y validar la información externa antes de convertirla en entidades del sistema.

---

## 3. Estrategia de Persistencia: Caché + JSON
Se ha diseñado un sistema de persistencia híbrido optimizado para el rendimiento en una aplicación de escritorio.

- Componente DAO: La clase RepositorioJSON actúa como un DAO que encapsula la complejidad de la librería Jackson.
- Decisión de Diseño (Carga Temprana): En lugar de leer el disco en cada consulta, el repositorio carga todos los datos en memoria al arrancar mediante el método cargarTodo(), llenando los mapas de personas y categorías.
- Ventaja: Las operaciones de lectura como buscarUsuarioPorNombre son instantáneas al leer directamente de un HashMap en memoria.
- Consistencia: La persistencia sigue una estrategia síncrona. Al llamar a registrarUsuario, se actualiza el mapa en memoria y se fuerza inmediatamente el volcado a disco con guardarUsuarios(), garantizando que no se pierdan datos.

---

## 4. Gestión de Dependencias (Maven)
El archivo pom.xml refleja una selección tecnológica estandarizada:

- JavaFX 21: Se utiliza la última versión, con los módulos javafx-controls y javafx-fxml declarados explícitamente.
- Jackson 2.15: Se incluye el módulo jackson-datatype-jsr310. Esta decisión permite serializar correctamente tipos de fecha modernos como LocalDate (usados en la clase Gasto), algo que las librerías antiguas no soportan nativamente.