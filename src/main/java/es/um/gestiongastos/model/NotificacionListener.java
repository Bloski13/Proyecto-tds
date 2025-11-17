package es.um.gestiongastos.model;

/**
 * Listener para notificaciones de alertas.
 */
public interface NotificacionListener {
    /**
     * Invocado cuando una alerta genera una notificación.
     *
     * @param n notificación creada
     * @param fuente alerta que la generó
     */
    void onNotificacion(Notificacion n, Alerta fuente);
}
