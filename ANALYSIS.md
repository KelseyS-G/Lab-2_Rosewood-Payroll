# What's wrong with the code we inherited



## The biggest problem: it quietly pays zero when anything goes wrong

```java
} catch (Exception e) {
    // something went wrong (for example the employee was not found) - return zero
    return 0;
}
```

Whatever goes wrong (an unknown employee, a null id, a plain bug), the caller gets `0.0`
and no hint of why. In payroll that is about the worst thing code can do. Nothing crashes,
nothing gets logged, and somebody just doesn't get paid. The standard calls this out
directly: the program carries on as if nothing happened (§6.1). I checked, and the demo
really does print `Result unknown employee -> 0.0`.

What makes it scarier is that the "employee not found" case only works by luck. The code
looks the employee up, but the only place it uses the result is the `println`
(`employee.getName()`). So "not found" is only noticed because that call blows up with a
`NullPointerException`, which the catch-all then hides. If someone tidied away the
`println` one day, unknown employees would be paid in full. That means the fix isn't just
"delete the catch". The existence check has to be written out on purpose.

I rank this first because it hides every other problem. The hollow tests (further down)
come a close second, because they are the reason nobody spotted any of this.

## The other problems, most important first

1. **One method does everything (§4.1, §4.2, §12.1).** It finds the employee, works out
   overtime, picks a tax band, subtracts tax, prints to the console and handles errors.
   That is five or six reasons to change, in about 25 lines against the 20-line guideline.

2. **The repository is hard-wired (§12.5).** `new InMemoryEmployeeRepository()` sits in a
   field, so nobody calling the class, and no test, can give it a different repository. You
   can't even simulate "the database is down", which is exactly the case we most need to
   test.

3. **Tax bands are an `if / else if / else` chain (§12.2).** This is the one the business
   cares about: a new band is coming, and today that would mean editing the middle of the
   calculation and re-testing all of it.

4. **It prints from inside a calculation (§4.4).** A method that returns a number
   shouldn't also write to the console. It is also the reason problem #1 works by accident.

5. **Nothing checks the input (§6).** I checked: `-5` hours gives a net pay of `-95.0`, a
   negative rate gives `-760.0`, and `NaN` hours gives `NaN`. (Every comparison with `NaN`
   is false, so it slides straight into the HIGH tax band.)

6. **Magic numbers (§3.4, §10.1).** `40`, `0.5`, `1000`, `5000`, `0.05`, `0.15` and `0.25`
   appear bare, with comments like `// LOW` doing the job a name should do.

## Things I noticed but am leaving alone on purpose

- **Money is a `double`**, and nothing is rounded (`1 h @ 999.99` nets `949.9905`).
  Switching to `BigDecimal` would change the public API and every figure, and the brief
  doesn't ask for it. I'll flag it rather than slip it in.
- **`findById` returns `null` for "not found".** It isn't pretty, but it is the existing
  contract. I'll keep it and check for null in the one place that calls it.
- **Tax is a flat rate on the whole gross pay.** That means earning a pesewa more can
  leave you with less: gross `999.99` nets `949.99`, but gross `1000.00` nets `850.00`
  (I checked). That is a business rule, not a coding mistake, so I'll preserve it and ask
  whether it's intended when the new band goes in.

## The tests: all three pass without testing anything

| Test | What it actually does |
|------|-----------------------|
| `calculatesOvertimePay` | Mocks `PayrollCalculator` itself, tells the mock to return `1402.5`, then checks that it returned `1402.5`. None of the real code runs, so this is really a test of Mockito. |
| `calculatesNetPay` | Only checks `net > 0`. Nearly any answer would pass, including a wildly wrong one. |
| `returnsPayForAnEmployee` | Calls `assertNotNull` on a primitive `double`. A `double` can never be null, so this assertion cannot fail. |

To make sure I wasn't just being harsh, I broke the production code five different ways,
each one producing wrong payslips: every tax rate set to 99%, overtime switched off,
overtime paid at zero, the LOW band edge moved from 1000 to 100, and tax never deducted.
**All five broken versions passed all three tests.** Nothing in the suite looks at band
boundaries, overtime arithmetic, unknown employees or bad input. A coverage report would
look healthy on these tests while proving nothing, which is the trap §9 warns about.

## What I plan to do

1. Pin down what the code does today with real, hand-worked assertions, so I can refactor
   without changing anyone's pay by accident.
2. Make failures loud: an explicit existence check and custom exceptions.
3. Split gross pay and tax out of the calculator and inject every dependency.
4. Turn the tax bands into data, so the new band is something you add, not something you
   rewrite.
5. As my one extra, reject nonsense input (negative, `NaN`, infinite).
