package com.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String codigo;
    private int inicio = 0;
    private int actual = 0;
    private int linea = 1;
    private final List<Token> tokens = new ArrayList<>();
    public Lexer(String codigo){
        this.codigo = codigo;
    }


    public List<Token> scanTokens() {
        while (!estaAlFinal()) {
            inicio = actual;
            scanToken();
        }
        // Anadir el token de fin de archivo
        tokens.add(new Token(tipoToken.EOF, "", linea, inicio));
        return tokens;
    }

    private void scanToken() {
        char c = avanzar();
        switch (c) {
            // símbolos de un solo carácter
            case '.':
                aniadirToken(tipoToken.PUNTO);
                break;
            
            case '[':
                aniadirToken(tipoToken.CORCHETE_IZQ);
                break;

            case ']':
                aniadirToken(tipoToken.CORCHETE_DER);
                break;

            case '(': 
                aniadirToken(tipoToken.PARENTESIS_IZQ);
                break;

            case ')': 
                aniadirToken(tipoToken.PARENTESIS_DER);
                break;

            case '{': 
                aniadirToken(tipoToken.LLAVE_IZQ);
                break;

            case '}': 
                aniadirToken(tipoToken.LLAVE_DER);
                break;

            case ',': 
                aniadirToken(tipoToken.COMA);
                break;
            case ':':
                aniadirToken(tipoToken.DOS_PUNTOS);
                break;

            case ';': 
                aniadirToken(tipoToken.PUNTO_Y_COMA);
                break;

            case '+': 
                aniadirToken(tipoToken.SUMA);
                break;

            case '-': 
                aniadirToken(tipoToken.RESTA);
                break;

            case '*': 
                aniadirToken(tipoToken.ASTERISK);  // CAMBIADO (era MULTIPLICACION, pero sirve para ambos)
                break;

            case '%':
                aniadirToken(tipoToken.MOD);
                break;

            case '&':
                if (match('&')){
                    aniadirToken(tipoToken.AND); //operador logico &&
                } else {
                    aniadirToken(tipoToken.BITAND); //  CORREGIDO (era DESCONOCIDO)
                }
                break;

            case '|':
                if (match('|')){
                    aniadirToken(tipoToken.OR); // operador logico ||
                } else {
                    aniadirToken(tipoToken.BITOR); //  CORREGIDO (era DESCONOCIDO)
                }
                break;

            // operadores que podrían ser de uno o dos caracteres
            case '!': 
                aniadirToken(match('=') ? tipoToken.DIFERENTE : tipoToken.NOT);  // ✅ CORREGIDO (era DESCONOCIDO)
                break;

            case '=': 
                aniadirToken(match('=') ? tipoToken.IGUAL : tipoToken.ASIGNACION);
                break;

            case '<': 
                aniadirToken(match('=') ? tipoToken.MENOR_IGUAL : tipoToken.MENOR_QUE);
                break;

            case '>': 
                aniadirToken(match('=') ? tipoToken.MAYOR_IGUAL : tipoToken.MAYOR_QUE);
                break;

            // la división es especial porque puede iniciar un comentario
            case '/':
                if (match('/')) {
                    // es un comentario, entonces avanza hasta el final de la línea
                    while (mirar() != '\n' && !estaAlFinal()) avanzar();
                } else {
                    aniadirToken(tipoToken.DIVISION);
                }
                break;
                

            // ignorar espacios en blanco
            case ' ':
            case '\r':
            case '\t':
                break;

            // nueva línea
            case '\n':
                linea++;
                break;

            // literales de cadena
            case '"':
                literal();
                break;


            case '?':
                aniadirToken(tipoToken.QUESTION);
                break;

            case '~':
                aniadirToken(tipoToken.TILDE);
                break;

            case '^':
                aniadirToken(tipoToken.BITXOR);
                break;

            case '@':
                aniadirToken(tipoToken.ARROBA);
                break;

            default:
                if (esDigito(c)) {
                    numero();
                } else if (esLetra(c)) {
                    identificador();
                } else {
                    // carácter no reconocido
                    aniadirToken(tipoToken.DESCONOCIDO);
                }
                break;
            
        }
    }
    
    public void identificador() {
        while (esAlfaNumerico(mirar())) {
            avanzar();
        }
        String texto = codigo.substring(inicio, actual);
        tipoToken tipo;
        try {
            tipo = tipoToken.valueOf(texto.toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = tipoToken.IDENTIFICADOR;
        }
        aniadirToken(tipo);
    }

    public void numero(){
        while (esDigito(mirar()))
            avanzar();
        // Analizar si es un numero flotante
        if (mirar() == '.' && esDigito(mirarSiguiente())){
            avanzar(); // consumir '.'
            while (esDigito(mirar()))
                avanzar();
            aniadirToken(tipoToken.LITERAL_FLOTANTE);  //  CORREGIDO
        } else {
            aniadirToken(tipoToken.LITERAL_ENTERA);     //  CORREGIDO
        }
    }

    public void literal() {
        while (mirar() != '"' && !estaAlFinal()) {
            // Si encontramos un salto de línea, salimos de inmediato
            if (mirar() == '\n' || mirar() == '\r') {
                tokens.add(new Token(tipoToken.COMILLA,
                                    "\"",   // lexema = solo la comilla
                                    linea,
                                    inicio));
                return; 
            }
            avanzar();
        }
        if(estaAlFinal()){
            String textoErroneo = codigo.substring(inicio,actual);
            tokens.add(new Token(tipoToken.ERROR_DE_CADENA, textoErroneo, linea, actual));
            System.out.println("Error en la linea " + linea + ": Cadena sin cerrar");
            return;
        }
        // Si llegamos aquí es porque sí encontramos la comilla de cierre en la misma línea
        avanzar();
        String valor = codigo.substring(inicio + 1, actual - 1);
        aniadirToken(tipoToken.LITERAL_CADENA, valor);
    }

    private char avanzar(){
        return codigo.charAt(actual++);
    }

    private boolean esAlfaNumerico(char letra) {
        return esLetra(letra) || esDigito(letra);
    }
    private char mirar() {
        if (estaAlFinal())
            return '\0';
        return codigo.charAt(actual);
    }

    private boolean esLetra(char letra) {
        return (letra >= 'a'  && letra <= 'z') || (letra >= 'A' && letra <= 'Z') || letra == '_';
    }

    private boolean esDigito(char digito){
        return digito >= '0' && digito <= '9';
    }

    private boolean estaAlFinal() {
        return actual >= codigo.length();
    }

    private char mirarSiguiente() {
        if (actual + 1 >= codigo.length())
            return '\0';
        return codigo.charAt(actual + 1);
    }

    private void aniadirToken(tipoToken tipo) {
        aniadirToken(tipo, null);
    }

    private void aniadirToken(tipoToken tipo, Object literal ){
        String texto = codigo.substring(inicio, actual);
        // Si el codigo actual no es nulo, se usa ese para cadenas
        String lexema = (literal == null)  ? texto : literal.toString();
        tokens.add(new Token(tipo, lexema, linea, inicio));
    }


    private boolean match(char expected) {
        if (estaAlFinal() || codigo.charAt(actual) != expected) {
            return false;
        }
        actual++;
        return true;
    }
}