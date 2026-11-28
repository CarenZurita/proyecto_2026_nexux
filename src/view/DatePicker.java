package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** Selector desplegable de fecha para los formularios Swing. */
final class DatePicker extends JPanel {
    private static final Color TEXTO = new Color(0x1E293B);
    private static final Color AZUL = new Color(0x2563EB);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM uuuu", new Locale("es"));
    private static final String[] DIAS = {"L", "M", "X", "J", "V", "S", "D"};

    private final JTextField texto = new JTextField();
    private LocalDate fecha;

    DatePicker(LocalDate fechaInicial) {
        super(new BorderLayout(4, 0));
        setOpaque(false);
        texto.setEditable(false);
        texto.setFocusable(false);
        texto.setFont(new Font("SansSerif", Font.PLAIN, 13));
        texto.setForeground(TEXTO);
        texto.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        texto.setToolTipText("Seleccione día, mes y año");
        add(texto, BorderLayout.CENTER);

        JButton botonCalendario = new JButton("▦");
        botonCalendario.setFont(new Font("SansSerif", Font.PLAIN, 16));
        botonCalendario.setForeground(TEXTO);
        botonCalendario.setBackground(Color.WHITE);
        botonCalendario.setFocusPainted(false);
        botonCalendario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botonCalendario.setToolTipText("Abrir calendario");
        botonCalendario.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(4, 9, 4, 9)));
        botonCalendario.addActionListener(evento -> mostrarCalendario(botonCalendario));
        add(botonCalendario, BorderLayout.EAST);

        setDate(fechaInicial);
    }

    String getValue() {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    private void setDate(LocalDate nuevaFecha) {
        fecha = nuevaFecha;
        texto.setText(fecha == null ? "" : fecha.format(FORMATO_FECHA));
    }

    private void mostrarCalendario(JButton ancla) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new LineBorder(new Color(0xCBD5E1)));
        JPanel calendario = new JPanel(new BorderLayout(6, 8));
        calendario.setBackground(Color.WHITE);
        calendario.setBorder(new EmptyBorder(10, 10, 10, 10));

        YearMonth mesInicial = YearMonth.from(fecha == null ? LocalDate.now() : fecha);
        YearMonth[] mesVisible = {mesInicial};
        JLabel tituloMes = new JLabel("", SwingConstants.CENTER);
        tituloMes.setFont(new Font("SansSerif", Font.BOLD, 13));
        tituloMes.setForeground(TEXTO);
        JPanel dias = new JPanel(new GridLayout(7, 7, 2, 2));
        dias.setBackground(Color.WHITE);
        calendario.add(dias, BorderLayout.CENTER);

        Runnable actualizarCalendario = () -> {
            tituloMes.setText(mesVisible[0].format(FORMATO_MES));
            dias.removeAll();
            for (String dia : DIAS) {
                JLabel etiqueta = new JLabel(dia, SwingConstants.CENTER);
                etiqueta.setFont(new Font("SansSerif", Font.BOLD, 11));
                etiqueta.setForeground(new Color(0x64748B));
                dias.add(etiqueta);
            }
            int desplazamiento = mesVisible[0].atDay(1).getDayOfWeek().getValue() - 1;
            for (int celda = 0; celda < 42; celda++) {
                int diaDelMes = celda - desplazamiento + 1;
                if (diaDelMes < 1 || diaDelMes > mesVisible[0].lengthOfMonth()) {
                    dias.add(new JLabel());
                    continue;
                }
                LocalDate dia = mesVisible[0].atDay(diaDelMes);
                JButton botonDia = new JButton(Integer.toString(diaDelMes));
                botonDia.setFont(new Font("SansSerif", Font.PLAIN, 12));
                botonDia.setFocusPainted(false);
                botonDia.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                botonDia.setForeground(TEXTO);
                botonDia.setBackground(Color.WHITE);
                botonDia.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                if (dia.equals(fecha)) {
                    botonDia.setBackground(AZUL);
                    botonDia.setForeground(Color.WHITE);
                    botonDia.setOpaque(true);
                }
                botonDia.addActionListener(evento -> {
                    setDate(dia);
                    popup.setVisible(false);
                });
                dias.add(botonDia);
            }
            dias.revalidate();
            dias.repaint();
            calendario.revalidate();
            calendario.repaint();
        };

        JButton anterior = botonMes("‹");
        JButton siguiente = botonMes("›");
        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);
        encabezado.add(anterior, BorderLayout.WEST);
        encabezado.add(tituloMes, BorderLayout.CENTER);
        encabezado.add(siguiente, BorderLayout.EAST);
        calendario.add(encabezado, BorderLayout.NORTH);
        anterior.addActionListener(evento -> {
            mesVisible[0] = mesVisible[0].minusMonths(1);
            actualizarCalendario.run();
        });
        siguiente.addActionListener(evento -> {
            mesVisible[0] = mesVisible[0].plusMonths(1);
            actualizarCalendario.run();
        });

        actualizarCalendario.run();
        popup.add(calendario);
        popup.show(ancla, 0, ancla.getHeight());
    }

    private JButton botonMes(String textoBoton) {
        JButton boton = new JButton(textoBoton);
        boton.setFont(new Font("SansSerif", Font.BOLD, 16));
        boton.setForeground(TEXTO);
        boton.setFocusPainted(false);
        boton.setContentAreaFilled(false);
        boton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }
}
