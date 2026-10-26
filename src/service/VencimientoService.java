package service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import model.Medicamento;
import persistence.MedicamentoRepository;

/** Consulta de solo lectura del inventario, ordenada por fecha de vencimiento. */
public class VencimientoService {
    public static final int DIAS_ALERTA = 30;
    public static final int LIMITE_ROJO = 10;
    public static final int LIMITE_AMARILLO = 20;
    private final MedicamentoRepository medicamentos;
    private final Clock reloj;

    public VencimientoService() {
        this(new MedicamentoRepository(), Clock.systemDefaultZone());
    }

    public VencimientoService(MedicamentoRepository medicamentos, Clock reloj) {
        this.medicamentos = Objects.requireNonNull(medicamentos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public record Fila(String nombre, String presentacion,
                       LocalDate fechaVencimiento, long diasRestantes) { }

    public List<Fila> consultar() throws IOException {
        LocalDate hoy = LocalDate.now(reloj);
        return medicamentos.listar().stream()
                .sorted(Comparator.comparing(Medicamento::getFechaVencimientoDate)
                        .thenComparing(Medicamento::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(m -> new Fila(m.getNombre(), m.getPresentacion(),
                        m.getFechaVencimientoDate(),
                        ChronoUnit.DAYS.between(hoy, m.getFechaVencimientoDate())))
                .toList();
    }
}
