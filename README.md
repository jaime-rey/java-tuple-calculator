# java-tuple-calculator

Calculadora en Java que interpreta expresiones en forma de tupla y devuelve el resultado.

Soporta:

- Operaciones escalares: `+`, `-`, `*`, `/`, `^`, raíz (`R`), logaritmo (`L`).
- Constantes: `E`, `PI`.
- Operadores unarios: factorial (`!`), Fibonacci (`F`).
- Ecuaciones polinómicas (`X` / `Y`) de cualquier grado, con raíces reales y complejas.
- Listas de números primos (`P`) hasta un valor o en un intervalo.
- Aritmética de polinomios (`+`, `-`, `*`).
- Aritmética de matrices (`+`, `-`, `*`), escalar × matriz, traspuesta (`T`), determinante (`D`).

## Ejecutar

```powershell
javac com\calculator\Calculator.java
java com.calculator.Calculator "(4,2,/)" "[[(1,2),(3,4)],D]"
```

## Documentación

- [MANUAL.md](com/calculator/MANUAL.md) — referencia completa con ejemplos.
- [MANUAL.pdf](com/calculator/MANUAL.pdf) — misma referencia en PDF.

## Versión reducida

`CalculatorFree.java` es un ejecutable independiente que solo ofrece `+`, `-`, `*`, `/` con las constantes `E` y `PI`.

```powershell
javac com\calculator\CalculatorFree.java
java com.calculator.CalculatorFree "(2,E,*)"
```
