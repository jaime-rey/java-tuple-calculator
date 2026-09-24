package com.calculator;

public class CalculatorFree {

    public static double evaluate(String input) {
        if (input == null) throw new IllegalArgumentException("La entrada no puede ser nula");
        String cleaned = input.trim();
        if (cleaned.startsWith("{")) return sumArray(cleaned);
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        String[] parts = cleaned.split(",");
        if (parts.length != 3) throw new IllegalArgumentException(
            "Formato inválido. Se esperaba (n1,n2,op) — recibido: " + input);

        double a = parseNumber(parts[0].trim());
        double b = parseNumber(parts[1].trim());
        String op = parts[2].trim();
        if (op.length() != 1) throw new IllegalArgumentException("Operador inválido: " + op);

        return switch (op.charAt(0)) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> divide(a, b);
            default -> throw new IllegalArgumentException(
                "Operador no soportado en versión Free: " + op
                    + " (solo +, -, *, /). Actualiza a la versión completa.");
        };
    }

    private static double parseNumber(String token) {
        if (token.equalsIgnoreCase("E")) return Math.E;
        if (token.equalsIgnoreCase("PI")) return Math.PI;
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("Número inválido: " + token);
        }
    }

    private static double sumArray(String input) {
        if (!input.endsWith("}")) throw new IllegalArgumentException(
            "Formato inválido: falta '}' en " + input);
        String inner = input.substring(1, input.length() - 1).trim();
        if (inner.isEmpty()) return 0;
        double sum = 0;
        for (String token : inner.split(",")) {
            sum += parseNumber(token.trim());
        }
        return sum;
    }

    private static double divide(double a, double b) {
        if (b == 0) throw new ArithmeticException("División por cero");
        return a / b;
    }

    private static String format(double r) {
        if (r == Math.floor(r) && !Double.isInfinite(r)) return String.valueOf((long) r);
        return String.valueOf(r);
    }

    @SuppressWarnings("java:S106")
    public static void main(String[] args) {
        String[] ejemplos = {
            "(1,1,+)", "(7,5,-)", "(6,3,*)", "(4,2,/)", "(1,0,/)",
            "(2,E,*)", "(PI,2,/)", "(E,PI,+)", "{1,2,3,4,5}", "{PI,E}"
        };
        for (String e : ejemplos) runOne(e);
        if (args.length > 0) {
            System.out.println("---");
            for (String arg : args) runOne(arg);
        }
    }

    @SuppressWarnings("java:S106")
    private static void runOne(String input) {
        try {
            System.out.println(input + " -> " + format(evaluate(input)));
        } catch (RuntimeException ex) {
            System.out.println(input + " -> ERROR: " + ex.getMessage());
        }
    }
}
