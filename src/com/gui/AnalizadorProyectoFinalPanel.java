package com.gui;

import com.lexer.Lexer;
import com.lexer.Token;
import com.lexer.tipoToken;
import com.lr0.LR0Table;
import com.lr0.LRParser;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class AnalizadorProyectoFinalPanel extends JPanel {

    private final JTextField txtProgPath = new JTextField();
    private final JButton btnOpenP = new JButton("Abrir programa");
    private final JButton btnAnalyze = new JButton("Analizar");
    private final JButton btnClear = new JButton("Limpiar");

    private final JTextArea txtProgram = new JTextArea();

    private File programFile = null;
    
    // Ruta fija de la gramática del proyecto final
    private static final String GRAMMAR_PATH = "pruebas/gramaticas_aumentadas/gramaticaFinal.txt";

    public AnalizadorProyectoFinalPanel() {
        setLayout(new BorderLayout(8,8));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Panel superior
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Título informativo
        c.weightx = 0; 
        c.gridx = 0; 
        c.gridy = 0; 
        c.gridwidth = 3;
        JLabel lblInfo = new JLabel("Analizador Léxico y Sintáctico LR - Proyecto Final");
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        top.add(lblInfo, c);

        c.gridy = 1;
        c.gridwidth = 1;
        top.add(new JLabel("Seleccione un programa fuente (o escriba abajo):"), c);
        
        c.weightx = 1; 
        c.gridx = 1; 
        txtProgPath.setEditable(false); 
        top.add(txtProgPath, c);
        
        c.weightx = 0; 
        c.gridx = 2; 
        top.add(btnOpenP, c);

        // Panel de botones de acción
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        btnAnalyze.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        btnPanel.add(btnAnalyze);
        btnPanel.add(btnClear);
        
        top.add(btnPanel, c);

        add(top, BorderLayout.NORTH);

        // Área de texto para el programa
        txtProgram.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        txtProgram.setLineWrap(false);
        txtProgram.setTabSize(4);

        JPanel center = new JPanel(new BorderLayout());
        JLabel lblProgram = new JLabel("Programa fuente (editable):");
        lblProgram.setFont(new Font("Segoe UI", Font.BOLD, 12));
        center.add(lblProgram, BorderLayout.NORTH);
        center.add(new JScrollPane(txtProgram), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        // Listeners
        btnOpenP.addActionListener(this::onOpenProgram);
        btnAnalyze.addActionListener(this::onAnalyze);
        btnClear.addActionListener(e -> {
            programFile = null;
            txtProgPath.setText("");
            txtProgram.setText("");
        });
    }

    private void onOpenProgram(ActionEvent e) {
        JFileChooser ch = new JFileChooser(new File(System.getProperty("user.dir")));
        ch.setAcceptAllFileFilterUsed(true);
        ch.addChoosableFileFilter(new FileNameExtensionFilter("Java (*.java)", "java"));
        ch.addChoosableFileFilter(new FileNameExtensionFilter("Texto (*.txt)", "txt"));
        ch.addChoosableFileFilter(new FileNameExtensionFilter("Código (*.c, *.java, *.txt)", "c","java","txt"));
        
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            programFile = ch.getSelectedFile();
            txtProgPath.setText(programFile.getAbsolutePath());
            try {
                txtProgram.setText(Files.readString(programFile.toPath()));
                txtProgram.setCaretPosition(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "No se pudo leer el programa:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onAnalyze(ActionEvent e) {
        try {
            // 1. Verificar que existe la gramática del proyecto
            File grammarFile = new File(GRAMMAR_PATH);
            if (!grammarFile.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "No se encontró el archivo de gramática:\n" + GRAMMAR_PATH + 
                    "\n\nAsegúrate de que el archivo existe.",
                    "Error - Gramática no encontrada", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Obtener el programa a analizar
            String prog;
            if (programFile != null) {
                prog = Files.readString(programFile.toPath());
            } else {
                prog = txtProgram.getText();
                if (prog == null || prog.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Por favor, carga o escribe un programa fuente para analizar.",
                        "Atención",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // 3. ANÁLISIS LÉXICO usando tu Lexer
            Lexer lexer = new Lexer(prog);
            List<Token> tokensLexicos = lexer.scanTokens();
            
            // Extraer errores léxicos
            List<Object[]> erroresLexicos = new ArrayList<>();
            for (Token t : tokensLexicos) {
                if (t.tipo == tipoToken.DESCONOCIDO) {
                    erroresLexicos.add(new Object[]{ t.linea, "Léxico", "Token desconocido: '" + t.lexema + "'" });
                }
                if (t.tipo == tipoToken.ERROR_DE_CADENA) {
                    erroresLexicos.add(new Object[]{ t.linea, "Léxico", "Error de cadena: '" + t.lexema + "'" });
                }
            }

            // Preparar datos para la tabla de tokens
            List<Object[]> listaTokens = new ArrayList<>();
            for (Token t : tokensLexicos) {
                if (t.tipo != tipoToken.EOF && t.tipo != tipoToken.DESCONOCIDO) {
                    Object tokenMostrar;
                    switch (t.tipo) {
                        case PUNTO:
                        case PUNTO_Y_COMA:
                        case COMA:
                        case DOS_PUNTOS:
                        case CORCHETE_IZQ:
                        case CORCHETE_DER:
                        case PARENTESIS_IZQ:
                        case PARENTESIS_DER:
                        case LLAVE_IZQ:
                        case LLAVE_DER:
                            tokenMostrar = t.lexema;
                            break;
                        default:
                            tokenMostrar = t.tipo;
                    }
                    listaTokens.add(new Object[]{t.linea, t.lexema, tokenMostrar});
                }
            }

            // Tabla de símbolos
            List<Object[]> listaSimbolos = new ArrayList<>();
            Set<String> idsUnicos = new HashSet<>();
            int idCounter = 1;
            for (Token t : tokensLexicos) {
                if (t.tipo == tipoToken.IDENTIFICADOR && idsUnicos.add(t.lexema)) {
                    listaSimbolos.add(new Object[]{ idCounter++, t.lexema });
                }
            }

            // VENTANA 1: Mostrar tokens léxicos
            VentanaTokensLexicos ventanaTokens = new VentanaTokensLexicos(
                listaTokens.toArray(new Object[0][]),
                listaSimbolos.toArray(new Object[0][])
            );
            ventanaTokens.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            ventanaTokens.setVisible(true);

            // 4. Construir tabla LR desde la gramática fija
            LR0Table.Result tablaLR = LR0Table.buildFromFile(GRAMMAR_PATH);

            // VENTANA 2: Mostrar Tabla de Análisis Sintáctico LR
            VentanaTablaLR ventanaTablaLR = new VentanaTablaLR(tablaLR);
            ventanaTablaLR.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            ventanaTablaLR.setVisible(true);

            // 5. Convertir tokens del Lexer al formato esperado por LRParser
            List<String> tokensParaParser = convertirTokensParaParser(tokensLexicos);

            // 6. ANÁLISIS SINTÁCTICO - Obtener corrida paso a paso
            DefaultTableModel modeloCorrida = LRParser.runLRParse(GRAMMAR_PATH, tablaLR, tokensParaParser);

            // Extraer errores sintácticos del modelo
            List<Object[]> erroresSintacticos = new ArrayList<>();
            for (int i = 0; i < modeloCorrida.getRowCount(); i++) {
                String accion = (String) modeloCorrida.getValueAt(i, 2);
                if (accion != null && accion.startsWith("Error sintáctico")) {
                    String entrada = (String) modeloCorrida.getValueAt(i, 1);
                    erroresSintacticos.add(new Object[]{ i + 1, "Sintáctico", accion });
                }
            }

            // VENTANA 3: Mostrar corrida del análisis sintáctico
            VentanaCorridaSintactico ventanaCorrida = new VentanaCorridaSintactico(modeloCorrida);
            ventanaCorrida.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            ventanaCorrida.setVisible(true);

            // 7. Combinar errores léxicos y sintácticos
            List<Object[]> todosErrores = new ArrayList<>();
            todosErrores.addAll(erroresLexicos);
            todosErrores.addAll(erroresSintacticos);

            // VENTANA 4: Mostrar tabla de errores
            VentanaErrores ventanaErrores = new VentanaErrores(todosErrores.toArray(new Object[0][]));
            ventanaErrores.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            ventanaErrores.setVisible(true);

            // Mensaje de éxito
            String mensaje = "Análisis completado\n\n";
            mensaje += "Tokens generados: " + tokensLexicos.size() + "\n";
            mensaje += "Errores léxicos: " + erroresLexicos.size() + "\n";
            mensaje += "Errores sintácticos: " + erroresSintacticos.size() + "\n";
            mensaje += "\nSe abrieron 4 ventanas con los resultados.";
            JOptionPane.showMessageDialog(this, mensaje, "Análisis Exitoso", 
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error al analizar:\n" + ex.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private List<String> convertirTokensParaParser(List<Token> tokens) {
        List<String> resultado = new ArrayList<>();
        
        for (Token t : tokens) {
            String tokenStr = mapearTokenATerminal(t);
            if (tokenStr != null && !tokenStr.isEmpty()) {
                resultado.add(tokenStr);
            }
        }
        
        resultado.add("$");
        return resultado;
    }

    private String mapearTokenATerminal(Token t) {
        switch (t.tipo) {
            // Palabras reservadas
        case PUBLIC: return "public";
        case CLASS: return "class";
        case STATIC: return "static";
        case VOID: return "void";
        case INT: return "int";
        case FLOAT: return "float";
        case MAIN: return "main";
        case IF: return "if";
        case ELSE: return "else";
        case SWITCH: return "switch";
        case CASE: return "case";
        case DEFAULT: return "default";
        case WHILE: return "while";
        case DO: return "do";
        case FOR: return "for";
        case BREAK: return "break";
        case SYSTEM: return "System";
        case OUT: return "out";
        case PRINTLN: return "println";

        // Identificadores y literales
        case IDENTIFICADOR: return "id";
        case LITERAL_ENTERA:
        case LITERAL_FLOTANTE: return "num";
        case LITERAL_CADENA:
        case CADENA: return "literalCad";

        // Operadores relacionales
        case MENOR_QUE: return "<";
        case MAYOR_QUE: return ">";
        case MENOR_IGUAL: return "<=";
        case MAYOR_IGUAL: return ">=";
        case IGUAL: return "==";
        case DIFERENTE: return "!=";

        // Operadores lógicos
        case AND: return "&&";
        case OR: return "||";

        // Operadores aritméticos
        case SUMA: return "+";
        case RESTA: return "-";
        case MULTIPLICACION: return "*";
        case DIVISION: return "/";
        case MOD: return "%";

        // Asignación
        case ASIGNACION: return "=";

        // Delimitadores
        case PARENTESIS_IZQ: return "(";
        case PARENTESIS_DER: return ")";
        case LLAVE_IZQ: return "{";
        case LLAVE_DER: return "}";
        case CORCHETE_IZQ: return "[";
        case CORCHETE_DER: return "]";
        case PUNTO_Y_COMA: return ";";
        case COMA: return ",";
        case PUNTO: return ".";
        case DOS_PUNTOS: return ":";

        // Ignorar
        case EOF:
        case DESCONOCIDO:
        case ERROR_DE_CADENA:
            return null;

        default:
            return null;
        }
    }
}