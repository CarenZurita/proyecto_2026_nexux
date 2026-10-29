package view;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** Componentes locales: no cambian los estilos de otras pantallas ni los logos. */
public final class ComponentesRedondeados {
    private ComponentesRedondeados() { }

    /** Los mismos dibujos de Inicio, escalados para tarjetas y navegación. */
    public static class IconoModulo implements Icon {
        private final String nombre;
        private final int tamano;
        public IconoModulo(String nombre, int tamano) {
            this.nombre = nombre.toLowerCase(java.util.Locale.ROOT);
            this.tamano = tamano;
        }
        @Override public int getIconWidth() { return tamano; }
        @Override public int getIconHeight() { return tamano; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D d = preparar(g);
            d.translate(x + tamano / 2.0, y + tamano / 2.0);
            d.scale(tamano / 96.0, tamano / 96.0);
            Color color = switch (nombre) {
                case "medicamentos" -> new Color(16, 185, 129);
                case "ventas" -> new Color(37, 99, 235);
                case "usuarios" -> new Color(139, 92, 246);
                case "reportes" -> new Color(245, 158, 11);
                default -> Color.WHITE;
            };
            if (!color.equals(Color.WHITE)) {
                // Fondo claro opaco: conserva el contraste sobre el menú oscuro.
                d.setColor(new Color((color.getRed() * 28 + 255 * 227) / 255,
                        (color.getGreen() * 28 + 255 * 227) / 255,
                        (color.getBlue() * 28 + 255 * 227) / 255));
                d.fillOval(-48, -48, 96, 96);
            }
            d.setColor(color);
            d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (nombre) {
                case "medicamentos" -> {
                    d.rotate(Math.toRadians(-35));
                    d.drawRoundRect(-30, -14, 60, 28, 28, 28);
                    d.drawLine(0, -14, 0, 14);
                }
                case "ventas" -> {
                    d.drawLine(-33, -26, -24, -26);
                    d.drawLine(-24, -26, -14, 13);
                    d.drawLine(-14, 13, 28, 13);
                    d.drawLine(-18, -16, 34, -16);
                    d.drawLine(34, -16, 26, 4);
                    d.drawLine(26, 4, -17, 4);
                    d.fillOval(-14, 20, 10, 10);
                    d.fillOval(18, 20, 10, 10);
                }
                case "usuarios" -> {
                    d.fillOval(-12, -32, 24, 24);
                    d.fillOval(-39, -17, 17, 17);
                    d.fillOval(22, -17, 17, 17);
                    d.drawArc(-23, -5, 46, 48, 0, 180);
                    d.drawArc(-46, 5, 27, 27, 0, 180);
                    d.drawArc(19, 5, 27, 27, 0, 180);
                }
                case "reportes" -> {
                    d.fillRoundRect(-32, 1, 15, 29, 4, 4);
                    d.fillRoundRect(-7, -17, 15, 47, 4, 4);
                    d.fillRoundRect(18, -31, 15, 61, 4, 4);
                }
                case "inicio" -> {
                    java.awt.geom.Path2D casa = new java.awt.geom.Path2D.Double();
                    casa.moveTo(-34, -2); casa.lineTo(0, -33); casa.lineTo(34, -2);
                    casa.lineTo(24, -2); casa.lineTo(24, 31); casa.lineTo(7, 31);
                    casa.lineTo(7, 9); casa.lineTo(-7, 9); casa.lineTo(-7, 31);
                    casa.lineTo(-24, 31); casa.lineTo(-24, -2); casa.closePath();
                    d.fill(casa);
                }
                case "configuracion", "configuración" -> {
                    java.awt.geom.Path2D engranaje = new java.awt.geom.Path2D.Double();
                    for (int i = 0; i < 32; i++) {
                        double angulo = i * Math.PI / 16;
                        double radio = i % 4 < 2 ? 36 : 27;
                        double px = Math.cos(angulo) * radio, py = Math.sin(angulo) * radio;
                        if (i == 0) engranaje.moveTo(px, py); else engranaje.lineTo(px, py);
                    }
                    engranaje.closePath();
                    java.awt.geom.Area forma = new java.awt.geom.Area(engranaje);
                    forma.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24)));
                    d.fill(forma);
                }
                case "salir", "cerrar sesión" -> {
                    d.drawRoundRect(-31, -31, 44, 62, 8, 8);
                    d.drawLine(-13, 0, 34, 0);
                    d.drawLine(18, -15, 34, 0);
                    d.drawLine(18, 15, 34, 0);
                }
                default -> { }
            }
            d.dispose();
        }
    }

    private static Graphics2D preparar(Graphics g) {
        Graphics2D copia = (Graphics2D) g.create();
        copia.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return copia;
    }

    public static class Boton extends JButton {
        public Boton(String texto) { this(texto, null); }
        public Boton(String texto, Icon icono) {
            super(texto, icono);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }
        // Algunos estilos existentes llaman setOpaque(true); evitamos el fondo cuadrado.
        @Override public void setOpaque(boolean valor) { super.setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D d = preparar(g);
            Color color = getBackground();
            if (!isEnabled()) color = new Color(0xCBD5E1);
            else if (getModel().isPressed() && getModel().isArmed()) color = color.darker();
            d.setColor(color);
            d.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            d.dispose();
            super.paintComponent(g);
        }
        @Override public boolean contains(int x, int y) {
            return new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 22, 22).contains(x, y);
        }
    }

    public static class Campo extends JTextField {
        public Campo() { this(0); }
        public Campo(int columnas) {
            super(columnas);
            setOpaque(false);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D d = preparar(g);
            d.setColor(getBackground());
            d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            d.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            super.paintComponent(d);
            d.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            Graphics2D d = preparar(g);
            d.setColor(hasFocus() ? new Color(0x2563EB) : new Color(0x94A3B8));
            d.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), 20, 20);
            d.dispose();
        }
    }

    public static class Panel extends JPanel {
        private final int arco;
        private final Color borde;
        public Panel(LayoutManager layout, Color fondo, int arco) { this(layout, fondo, arco, null); }
        public Panel(LayoutManager layout, Color fondo, int arco, Color borde) {
            super(layout);
            this.arco = arco;
            this.borde = borde;
            setBackground(fondo);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D d = preparar(g);
            d.setColor(getBackground());
            d.fillRoundRect(0, 0, getWidth(), getHeight(), arco, arco);
            d.dispose();
        }
        @Override protected void paintChildren(Graphics g) {
            Graphics2D d = preparar(g);
            d.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arco, arco));
            super.paintChildren(d);
            d.dispose();
        }
        @Override protected void paintBorder(Graphics g) {
            super.paintBorder(g);
            if (borde != null) {
                Graphics2D d = preparar(g);
                d.setColor(borde);
                d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
                d.dispose();
            }
        }
    }
}
