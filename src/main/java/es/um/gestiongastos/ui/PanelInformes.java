package es.um.gestiongastos.ui;

import es.um.gestiongastos.controlador.Controlador;
import es.um.gestiongastos.model.Gasto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PanelInformes {

    private static PieChart graficoCircular;
    private static BarChart<String, Number> graficoBarras;
    private static Label lblResumenTotal;

    public static VBox crearVista() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #ffffff;");

        Label tituloPrincipal = new Label("Informes Visuales");
        tituloPrincipal.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // --- SECCIÓN SUPERIOR ---
        
        // 1. Gráfico Circular
        graficoCircular = new PieChart();
        graficoCircular.setTitle("Gastos por Categoría");
        
        // 🔴 FIX: Activamos las etiquetas (flechitas) y la leyenda (colores abajo)
        graficoCircular.setLabelsVisible(true); 
        graficoCircular.setLegendVisible(true);
        
        // Ajustamos altura mínima para que quepan las etiquetas sin cortarse
        graficoCircular.setMinHeight(350); 

        // 2. Panel Total
        Label lblTituloTotal = new Label("TOTAL GASTADO");
        lblTituloTotal.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        
        lblResumenTotal = new Label("0.00 €");
        lblResumenTotal.setFont(Font.font("System", FontWeight.BOLD, 36));
        lblResumenTotal.setStyle("-fx-text-fill: #27ae60;");

        VBox panelTotal = new VBox(10, lblTituloTotal, lblResumenTotal);
        panelTotal.setAlignment(Pos.CENTER_LEFT);
        panelTotal.setPadding(new Insets(0, 0, 0, 20));

        // 3. Contenedor Horizontal
        HBox filaSuperior = new HBox(10, graficoCircular, panelTotal);
        filaSuperior.setAlignment(Pos.CENTER_LEFT);
        
        HBox.setHgrow(graficoCircular, Priority.ALWAYS);
        HBox.setHgrow(panelTotal, Priority.NEVER);

        // --- SECCIÓN INFERIOR ---
        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Mes");
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Importe (€)");

        graficoBarras = new BarChart<>(ejeX, ejeY);
        graficoBarras.setTitle("Evolución Mensual de Gastos");
        graficoBarras.setLegendVisible(false);
        
        VBox.setVgrow(graficoBarras, Priority.ALWAYS);

        panel.getChildren().addAll(tituloPrincipal, new Separator(), filaSuperior, new Separator(), graficoBarras);
        
        return panel;
    }

    public static void refrescarDatos() {
        List<Gasto> gastos = PanelGestionGastos.getGastosVisibles(); // Usamos datos filtrados

        // 1. TOTAL GLOBAL
        BigDecimal totalAcumulado = BigDecimal.ZERO;
        for (Gasto g : gastos) {
            totalAcumulado = totalAcumulado.add(g.getImporte());
        }
        if (lblResumenTotal != null) {
            lblResumenTotal.setText(totalAcumulado.toString() + " €");
        }

        // 2. PIE CHART
        Map<String, Double> sumaPorCategoria = new HashMap<>();
        for (Gasto g : gastos) {
            String cat = g.getCategoria().getNombre();
            sumaPorCategoria.put(cat, sumaPorCategoria.getOrDefault(cat, 0.0) + g.getImporte().doubleValue());
        }

        ObservableList<PieChart.Data> datosPie = FXCollections.observableArrayList();
        sumaPorCategoria.forEach((cat, total) -> datosPie.add(new PieChart.Data(cat, total)));
        
        if (graficoCircular != null) {
            graficoCircular.setData(datosPie);
        }

        // 3. BAR CHART
        Map<YearMonth, Double> sumaPorMes = new TreeMap<>();
        for (Gasto g : gastos) {
            YearMonth mesAnyo = YearMonth.from(g.getFecha());
            sumaPorMes.put(mesAnyo, sumaPorMes.getOrDefault(mesAnyo, 0.0) + g.getImporte().doubleValue());
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Gastos");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        
        sumaPorMes.forEach((mes, total) -> serie.getData().add(new XYChart.Data<>(mes.format(fmt), total)));

        if (graficoBarras != null) {
            graficoBarras.getData().clear();
            graficoBarras.getData().add(serie);
        }
    }
}