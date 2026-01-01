# Historias de usuario

Este documento detalla las historias de usuario implementadas en la versión actual del proyecto, organizadas por módulo funcional.

## 1. Gestión de Identidad y Acceso

### HU-01: Registro de Nuevo Usuario
**Como** usuario (no registrado),  
**Quiero** crear una cuenta personal proporcionando mi nombre completo, nombre de usuario y contraseña,  
**Para** poder acceder a la aplicación y gestionar mis gastos.

* **Criterios de Aceptación:**
  * El sistema debe validar que el nombre de usuario no exista previamente.
  * Todos los campos (nombre completo, usuario, contraseña) son obligatorios.
  * Al registrarse con éxito, se debe crear automáticamente una "Cuenta Personal" para el usuario.

### HU-02: Inicio de Sesión
**Como** usuario registrado,  
**Quiero** ingresar con mi nombre de usuario y contraseña,  
**Para** acceder a mis datos, cuentas y configuración.

* **Criterios de Aceptación:**
  * El sistema debe verificar las credenciales contra los datos almacenados.
  * Si el usuario o contraseña son incorrectos, se debe mostrar un mensaje de error visual.
  * Al autenticarse correctamente, se debe redirigir al usuario a la pantalla principal cargando sus datos.

---

## 2. Gestión de Cuentas

### HU-03: Crear Cuenta Compartida
**Como** usuario,  
**Quiero** crear un grupo de gastos y agregar a otros participantes,  
**Para** gestionar gastos comunes y repartirlos entre varias personas.

* **Criterios de Aceptación:**
  * Se debe permitir seleccionar múltiples participantes de la lista de usuarios registrados.
  * Se debe definir un reparto de porcentajes, validando que el total sume el 100%.
  * El creador de la cuenta se incluye automáticamente como participante.

### HU-04: Consultar Estado y Saldos de Cuenta
**Como** miembro de un grupo,  
**Quiero** ver el detalle de una cuenta específica,  
**Para** conocer el gasto total, mi saldo actual y quién debe dinero.

* **Criterios de Aceptación:**
  * Mostrar el gasto total acumulado de la cuenta.
  * Mostrar una tabla con cada participante, su porcentaje de participación, el gasto que ha asumido y su saldo resultante.

### HU-05: Importar Cuenta desde Archivo
**Como** usuario,  
**Quiero** importar una cuenta y sus gastos desde un fichero externo,  
**Para** integrar datos de otros sistema.

* **Criterios de Aceptación:**
  * El sistema debe soportar diferentes formatos de archivos.
  * Si el archivo contiene usuarios que no existen en el sistema, estos deben crearse automáticamente durante la importación.
  * Se deben importar tanto los participantes y sus porcentajes como el historial de gastos asociado.

---

## 3. Gestión de Gastos

### HU-06: Registrar Gasto
**Como** usuario,  
**Quiero** añadir un nuevo gasto indicando concepto, importe, fecha y categoría,  
**Para** que el sistema calcule las deudas correspondientes en la cuenta seleccionada.

* **Criterios de Aceptación:**
  * Es obligatorio seleccionar una cuenta destino para imputar el gasto.
  * No se permite registrar gastos con fechas futuras.

### HU-07: Modificar Gasto Existente
**Como** usuario,  
**Quiero** editar los detalles de un gasto, cambiar su pagador o moverlo de cuenta,  
**Para** corregir errores cometidos durante el registro.

* **Criterios de Aceptación:**
  * Si se cambia la cuenta destino, el sistema debe validar que el pagador asignado sea miembro de la nueva cuenta.
  * Al modificar el importe o el pagador, se deben recalcular automáticamente los saldos de todos los participantes.

### HU-08: Crear Categorías Personalizadas
**Como** usuario organizado,  
**Quiero** poder definir nuevas categorías además de las predefinidas,  
**Para** clasificar mis gastos de forma más precisa según mi estilo de vida.

### HU-09: Filtrado de Gastos
**Como** usuario,  
**Quiero** filtrar mi listado de gastos por categoría, cuenta o rango de fechas,  
**Para** encontrar movimientos específicos rápidamente.

* **Criterios de Aceptación:**
  * Los filtros deben ser acumulativos (ej. Categoría "Comida" + Cuenta "Piso").
  * El indicador de "Mi gasto total" debe actualizarse dinámicamente según los filtros aplicados.

---

## 4. Informes y Análisis

### HU-10: Análisis Gráfico de Gastos
**Como** usuario,  
**Quiero** visualizar mis gastos mediante representaciones gráficas (circular y barras),  
**Para** comprender de un vistazo tanto la distribución de mi dinero como mi tendencia de consumo.

---

## 5. Alertas y Control

### HU-11: Configurar Alertas de Gasto
**Como** usuario previsor,  
**Quiero** establecer límites de gasto por categoría y periodicidad,  
**Para** controlar mi presupuesto y evitar excesos.

* **Criterios de Aceptación:**
  * El usuario puede definir alertas globales o filtradas por una categoría específica.
  * Se puede seleccionar la periodicidad de control: Semanal o Mensual.
