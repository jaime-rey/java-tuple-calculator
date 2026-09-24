package com.calculator;

import java.util.Arrays;

public class Calculator {

    public static double evaluate(String input) {
        Parsed p = parse(input);
        if (p.op == 'X' || p.op == 'Y') {
            throw new IllegalStateException(
                "Usa Calculator.solve() para ecuaciones polinómicas");
        }
        if (p.nums.length == 1) return unaryOp(p.op, p.nums[0]);
        if (p.nums.length == 2) return binaryOp(p.op, p.nums[0], p.nums[1]);
        throw new IllegalArgumentException(
            "Formato inválido para operador escalar: " + input);
    }

    public static Complex[] solve(String input) {
        Parsed p = parse(input);
        if (p.op != 'X' && p.op != 'Y') {
            throw new IllegalArgumentException(
                "Se esperaba operador 'X' o 'Y' para ecuación polinómica");
        }
        if (p.nums.length < 2) {
            throw new IllegalArgumentException(
                "Se necesitan al menos 2 coeficientes (grado >= 1)");
        }
        return solvePolynomial(p.nums);
    }

    public static String process(String input) {
        if (input != null && input.trim().startsWith("[")) return processPolyArith(input.trim());
        Parsed p = parse(input);
        if (p.op == 'P') return formatPrimes(primesFromArgs(p.nums));
        if (p.op == 'X' || p.op == 'Y') return formatRoots(p.op, solvePolynomial(p.nums));
        return formatDouble(evaluate(input));
    }

    private static String processPolyArith(String input) {
        if (!input.endsWith("]")) throw new IllegalArgumentException(
            "Formato inválido: falta ']' en " + input);
        String inner = input.substring(1, input.length() - 1);
        java.util.List<String> parts = splitTopLevel(inner);
        String op = parts.get(parts.size() - 1).trim();
        if (op.length() != 1) throw new IllegalArgumentException("Operador inválido: " + op);
        char opChar = op.charAt(0);

        if (parts.size() == 2) {
            double[][] m = parseMatrix(parts.get(0).trim());
            return switch (opChar) {
                case 'T' -> formatMatrix(matrixTranspose(m));
                case 'D' -> formatDouble(determinant(m));
                default -> throw new IllegalArgumentException(
                    "Operador unario no soportado en corchetes: " + op + " (usa T, D)");
            };
        }
        if (parts.size() != 3) throw new IllegalArgumentException(
            "Formato inválido — recibido: " + input);

        String first = parts.get(0).trim();
        String second = parts.get(1).trim();

        if (first.startsWith("[") && second.startsWith("[")) {
            return formatMatrix(matrixOp(opChar, parseMatrix(first), parseMatrix(second)));
        }
        boolean firstIsMatrix = first.startsWith("[");
        boolean secondIsMatrix = second.startsWith("[");
        if (firstIsMatrix ^ secondIsMatrix) {
            if (opChar != '*') throw new IllegalArgumentException(
                "Escalar y matriz solo admiten '*'");
            String matrixStr = firstIsMatrix ? first : second;
            String scalarStr = firstIsMatrix ? second : first;
            return formatMatrix(matrixScale(parseMatrix(matrixStr), parseNumber(scalarStr)));
        }
        double[] p = parseCoeffs(first);
        double[] q = parseCoeffs(second);
        double[] r = switch (opChar) {
            case '+' -> polyAdd(p, q);
            case '-' -> polySub(p, q);
            case '*' -> polyMul(p, q);
            default -> throw new IllegalArgumentException(
                "Operador de polinomios no soportado: " + op + " (usa +, -, *)");
        };
        return formatCoeffs(r);
    }

    private static double[][] matrixTranspose(double[][] m) {
        double[][] r = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                r[j][i] = m[i][j];
        return r;
    }

    private static double[][] matrixScale(double[][] m, double k) {
        double[][] r = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                r[i][j] = k * m[i][j];
        return r;
    }

    private static double determinant(double[][] m) {
        if (m.length != m[0].length) throw new IllegalArgumentException(
            "El determinante requiere matriz cuadrada, recibida "
                + m.length + "x" + m[0].length);
        int n = m.length;
        double[][] a = copyMatrix(m);
        double det = 1;
        for (int col = 0; col < n; col++) {
            int pivot = findPivot(a, col);
            if (a[pivot][col] == 0) return 0;
            if (pivot != col) {
                swapRows(a, col, pivot);
                det = -det;
            }
            det *= a[col][col];
            eliminateColumn(a, col);
        }
        return det;
    }

    private static double[][] copyMatrix(double[][] m) {
        double[][] a = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++) System.arraycopy(m[i], 0, a[i], 0, m[0].length);
        return a;
    }

    private static int findPivot(double[][] a, int col) {
        int pivot = col;
        for (int row = col + 1; row < a.length; row++) {
            if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) pivot = row;
        }
        return pivot;
    }

    private static void swapRows(double[][] a, int i, int j) {
        double[] tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    private static void eliminateColumn(double[][] a, int col) {
        int n = a.length;
        for (int row = col + 1; row < n; row++) {
            double factor = a[row][col] / a[col][col];
            for (int k = col; k < n; k++) a[row][k] -= factor * a[col][k];
        }
    }

    private static double[][] parseMatrix(String token) {
        String t = token.trim();
        if (!t.startsWith("[") || !t.endsWith("]")) throw new IllegalArgumentException(
            "Matriz inválida: " + token);
        String inner = t.substring(1, t.length() - 1);
        java.util.List<String> rows = splitTopLevel(inner);
        double[][] m = new double[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            m[i] = parseCoeffs(rows.get(i).trim());
            if (m[i].length != m[0].length) throw new IllegalArgumentException(
                "Filas de longitud inconsistente en la matriz");
        }
        return m;
    }

    private static double[][] matrixOp(char op, double[][] a, double[][] b) {
        return switch (op) {
            case '+' -> matrixAdd(a, b, 1);
            case '-' -> matrixAdd(a, b, -1);
            case '*' -> matrixMul(a, b);
            default -> throw new IllegalArgumentException(
                "Operador de matrices no soportado: " + op + " (usa +, -, *)");
        };
    }

    private static double[][] matrixAdd(double[][] a, double[][] b, int sign) {
        if (a.length != b.length || a[0].length != b[0].length)
            throw new IllegalArgumentException(
                "Dimensiones incompatibles: "
                    + a.length + "x" + a[0].length + " vs "
                    + b.length + "x" + b[0].length);
        double[][] r = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++)
                r[i][j] = a[i][j] + sign * b[i][j];
        return r;
    }

    private static double[][] matrixMul(double[][] a, double[][] b) {
        if (a[0].length != b.length) throw new IllegalArgumentException(
            "Dimensiones incompatibles para producto: "
                + a.length + "x" + a[0].length + " * "
                + b.length + "x" + b[0].length);
        double[][] r = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b[0].length; j++)
                for (int k = 0; k < a[0].length; k++)
                    r[i][j] += a[i][k] * b[k][j];
        return r;
    }

    private static String formatMatrix(double[][] m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("(");
            for (int j = 0; j < m[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(formatDouble(m[i][j]));
            }
            sb.append(")");
        }
        return sb.append("]").toString();
    }

    private static java.util.List<String> splitTopLevel(String s) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    private static double[] parseCoeffs(String token) {
        String t = token.trim();
        if (t.startsWith("(") && t.endsWith(")")) t = t.substring(1, t.length() - 1);
        String[] items = t.split(",");
        double[] out = new double[items.length];
        for (int i = 0; i < items.length; i++) out[i] = parseNumber(items[i].trim());
        return out;
    }

    private static double[] polyAdd(double[] p, double[] q) {
        int n = Math.max(p.length, q.length);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            double a = i < p.length ? p[p.length - 1 - i] : 0;
            double b = i < q.length ? q[q.length - 1 - i] : 0;
            r[n - 1 - i] = a + b;
        }
        return trimLeadingZeros(r);
    }

    private static double[] polySub(double[] p, double[] q) {
        int n = Math.max(p.length, q.length);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            double a = i < p.length ? p[p.length - 1 - i] : 0;
            double b = i < q.length ? q[q.length - 1 - i] : 0;
            r[n - 1 - i] = a - b;
        }
        return trimLeadingZeros(r);
    }

    private static double[] polyMul(double[] p, double[] q) {
        double[] r = new double[p.length + q.length - 1];
        for (int i = 0; i < p.length; i++) {
            for (int j = 0; j < q.length; j++) {
                r[i + j] += p[i] * q[j];
            }
        }
        return trimLeadingZeros(r);
    }

    private static double[] trimLeadingZeros(double[] r) {
        int start = 0;
        while (start < r.length - 1 && r[start] == 0) start++;
        if (start == 0) return r;
        double[] out = new double[r.length - start];
        System.arraycopy(r, start, out, 0, out.length);
        return out;
    }

    private static String formatCoeffs(double[] r) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < r.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(formatDouble(r[i]));
        }
        return sb.append(")").toString();
    }

    private static double unaryOp(char op, double a) {
        return switch (op) {
            case '!' -> factorial(a);
            case 'F' -> fibonacci(a);
            default -> throw new IllegalArgumentException("Operador unario no soportado: " + op);
        };
    }

    private static double binaryOp(char op, double a, double b) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> divide(a, b);
            case '^' -> Math.pow(a, b);
            case 'R' -> root(a, b);
            case 'L' -> logarithm(a, b);
            default -> throw new IllegalArgumentException("Operador no soportado: " + op);
        };
    }

    private static double divide(double a, double b) {
        if (b == 0) throw new ArithmeticException("División por cero");
        return a / b;
    }

    private static double root(double a, double b) {
        if (b == 0) throw new ArithmeticException("Índice de la raíz no puede ser 0");
        if (a < 0 && b % 2 == 0) throw new ArithmeticException(
            "Raíz de índice par de un número negativo no es real");
        if (a < 0) return -Math.pow(-a, 1.0 / b);
        return Math.pow(a, 1.0 / b);
    }

    private static double logarithm(double a, double b) {
        if (a <= 0 || b <= 0 || b == 1) throw new ArithmeticException(
            "Logaritmo inválido: argumento > 0 y base > 0 y != 1");
        return Math.log(a) / Math.log(b);
    }

    private static long[] primesFromArgs(double[] nums) {
        if (nums.length == 1) return primesInRange(0.0, nums[0]);
        if (nums.length == 2) return primesInRange(nums[0], nums[1]);
        throw new IllegalArgumentException("Formato inválido para P: (n,P) o (a,b,P)");
    }

    private static String formatPrimes(long[] primes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < primes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(primes[i]);
        }
        return sb.toString();
    }

    private static String formatRoots(char label, Complex[] roots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roots.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(label).append(i + 1).append(" = ").append(roots[i]);
        }
        return sb.toString();
    }

    private static Parsed parse(String input) {
        if (input == null) throw new IllegalArgumentException("La entrada no puede ser nula");
        String cleaned = stripParens(input.trim());
        String[] parts = cleaned.split(",");
        if (parts.length < 2) throw new IllegalArgumentException(
            "Formato inválido. Mínimo (n,op) — recibido: " + input);
        String op = parts[parts.length - 1].trim();
        if (op.length() != 1) throw new IllegalArgumentException("Operador inválido: " + op);
        double[] nums = new double[parts.length - 1];
        for (int i = 0; i < nums.length; i++) nums[i] = parseNumber(parts[i].trim());
        return new Parsed(nums, op.charAt(0));
    }

    private static String stripParens(String s) {
        if (s.startsWith("(") && s.endsWith(")")) return s.substring(1, s.length() - 1);
        return s;
    }

    private static final long MAX_PRIME_LIMIT = 100_000_000L;

    private static long[] primesInRange(double lo, double hi) {
        if (lo != Math.floor(lo) || hi != Math.floor(hi)) throw new ArithmeticException(
            "Primos requiere enteros");
        if (hi < lo) throw new ArithmeticException(
            "Intervalo inválido: el límite superior debe ser >= al inferior");
        if (hi > MAX_PRIME_LIMIT) throw new ArithmeticException(
            "Límite superior demasiado grande (máx " + MAX_PRIME_LIMIT + ")");
        long end = (long) hi;
        if (end < 2) return new long[0];
        long start = Math.max(2, (long) lo);
        boolean[] sieve = sieveOfEratosthenes((int) end);
        return collectPrimes(sieve, start, end);
    }

    private static boolean[] sieveOfEratosthenes(int end) {
        boolean[] sieve = new boolean[end + 1];
        Arrays.fill(sieve, true);
        sieve[0] = false;
        sieve[1] = false;
        for (int i = 2; (long) i * i <= end; i++) {
            if (sieve[i]) {
                for (int j = i * i; j <= end; j += i) sieve[j] = false;
            }
        }
        return sieve;
    }

    private static long[] collectPrimes(boolean[] sieve, long start, long end) {
        int count = 0;
        for (long i = start; i <= end; i++) if (sieve[(int) i]) count++;
        long[] out = new long[count];
        int k = 0;
        for (long i = start; i <= end; i++) if (sieve[(int) i]) out[k++] = i;
        return out;
    }

    private static double factorial(double a) {
        if (a < 0 || a != Math.floor(a)) throw new ArithmeticException(
            "Factorial requiere entero >= 0");
        if (a > 170) throw new ArithmeticException(
            "Factorial demasiado grande (overflow para double)");
        double r = 1;
        for (int i = 2; i <= (int) a; i++) r *= i;
        return r;
    }

    private static double fibonacci(double a) {
        if (a < 0 || a != Math.floor(a)) throw new ArithmeticException(
            "Fibonacci requiere entero >= 0");
        int n = (int) a;
        if (n == 0) return 0;
        double prev = 0;
        double curr = 1;
        for (int i = 1; i < n; i++) {
            double next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
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

    private static Complex[] solvePolynomial(double[] coeffs) {
        int n = coeffs.length - 1;
        if (n < 1) throw new IllegalArgumentException("Grado inválido");
        if (coeffs[0] == 0) throw new IllegalArgumentException(
            "El coeficiente principal no puede ser 0");
        if (n == 1) return new Complex[] { new Complex(-coeffs[1] / coeffs[0], 0) };
        if (n == 2) return solveQuadratic(coeffs[0], coeffs[1], coeffs[2]);
        return durandKerner(coeffs);
    }

    private static Complex[] solveQuadratic(double a, double b, double c) {
        double disc = b * b - 4 * a * c;
        if (disc >= 0) {
            double sq = Math.sqrt(disc);
            return new Complex[] {
                new Complex((-b + sq) / (2 * a), 0),
                new Complex((-b - sq) / (2 * a), 0)
            };
        }
        double sq = Math.sqrt(-disc);
        return new Complex[] {
            new Complex(-b / (2 * a),  sq / (2 * a)),
            new Complex(-b / (2 * a), -sq / (2 * a))
        };
    }

    private static Complex[] durandKerner(double[] coeffs) {
        int n = coeffs.length - 1;
        Complex[] c = new Complex[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) c[i] = new Complex(coeffs[i] / coeffs[0], 0);

        Complex[] z = initialGuesses(n);

        for (int iter = 0; iter < 2000; iter++) {
            if (iteratePolynomialRoots(c, z) < 1e-12) break;
        }

        for (int k = 0; k < n; k++) z[k] = z[k].clean();
        return z;
    }

    private static Complex[] initialGuesses(int n) {
        Complex[] z = new Complex[n];
        Complex seed = new Complex(0.4, 0.9);
        Complex acc = new Complex(1, 0);
        for (int k = 0; k < n; k++) {
            z[k] = acc;
            acc = acc.mul(seed);
        }
        return z;
    }

    private static double iteratePolynomialRoots(Complex[] c, Complex[] z) {
        double maxDelta = 0;
        for (int k = 0; k < z.length; k++) {
            Complex delta = polyEval(c, z[k]).div(denominator(z, k));
            z[k] = z[k].sub(delta);
            if (delta.abs() > maxDelta) maxDelta = delta.abs();
        }
        return maxDelta;
    }

    private static Complex denominator(Complex[] z, int k) {
        Complex denom = new Complex(1, 0);
        for (int j = 0; j < z.length; j++) {
            if (j != k) denom = denom.mul(z[k].sub(z[j]));
        }
        return denom;
    }

    private static Complex polyEval(Complex[] c, Complex x) {
        Complex r = c[0];
        for (int i = 1; i < c.length; i++) r = r.mul(x).add(c[i]);
        return r;
    }

    private static String formatDouble(double r) {
        if (r == Math.floor(r) && !Double.isInfinite(r)) return String.valueOf((long) r);
        return String.valueOf(r);
    }

    public static class Complex {
        public final double re;
        public final double im;
        public Complex(double re, double im) { this.re = re; this.im = im; }
        Complex add(Complex o) { return new Complex(re + o.re, im + o.im); }
        Complex sub(Complex o) { return new Complex(re - o.re, im - o.im); }
        Complex mul(Complex o) {
            return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
        }
        Complex div(Complex o) {
            double d = o.re * o.re + o.im * o.im;
            return new Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d);
        }
        double abs() { return Math.hypot(re, im); }
        Complex clean() {
            double q = Math.abs(re) < 1e-9 ? 0 : re;
            double r = Math.abs(im) < 1e-9 ? 0 : im;
            return new Complex(q, r);
        }
        @Override public String toString() {
            String rs = fmt(re);
            String is = fmt(Math.abs(im));
            if (im == 0) return rs;
            if (re == 0) return (im < 0 ? "-" : "") + is + "i";
            return rs + (im < 0 ? " - " : " + ") + is + "i";
        }
        private static String fmt(double v) {
            if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
            return String.valueOf(v);
        }
    }

    private static class Parsed {
        final double[] nums;
        final char op;
        Parsed(double[] nums, char op) { this.nums = nums; this.op = op; }
    }

    @SuppressWarnings("java:S106")
    public static void main(String[] args) {
        String[] ejemplos = {
            "(1,1,+)", "(4,2,/)", "(3,2,^)", "(100,10,L)",
            "(2,E,*)", "(PI,3,*)", "(10,E,L)", "(9,2,R)",
            "(1,-3,2,X)", "(2,2,2,X)", "(1,0,-1,Y)",
            "(1,0,0,-8,X)", "(1,-6,11,-6,X)",
            "(5,!)", "(0,!)", "(5,F)", "(10,F)",
            "(10,P)", "(30,P)", "(1,100,P)", "(50,70,P)",
            "[(1,1),(1,-1),*]",
            "[(1,1),(1,1),*]",
            "[(1,2,3),(4,5,6),+]",
            "[(1,2,3),(1,1),-]",
            "[(1,0,0,0),(1,0,0,0),*]",
            "[[(1,1),(1,1)],[(1,1),(1,1)],+]",
            "[[(1,2),(3,4)],[(5,6),(7,8)],-]",
            "[[(1,2),(3,4)],[(5,6),(7,8)],*]",
            "[[(1,2,3)],[(4),(5),(6)],*]",
            "[3,[(1,2),(3,4)],*]",
            "[[(1,2),(3,4)],2,*]",
            "[[(1,2,3),(4,5,6)],T]",
            "[[(1,2),(3,4)],D]",
            "[[(6,1,1),(4,-2,5),(2,8,7)],D]"
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
            System.out.println(input + " -> " + process(input));
        } catch (RuntimeException ex) {
            System.out.println(input + " -> ERROR: " + ex.getMessage());
        }
    }
}
