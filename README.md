# Rosewood Payroll: Net Pay Service

This service works out an employee's net pay from the hours they worked and their hourly
rate. It started life as a quick AI-generated class whose tests all passed but proved very
little. I've brought it up to the AmaliTech Coding Standards (Java Edition) without
changing what it pays people.

If you want to know what was wrong with the original, start with
[`docs/ANALYSIS.md`](docs/ANALYSIS.md). This README covers what I did about it and why.

## Running it

You need JDK 21 and Maven.


## How it's put together

The original did everything inside one method. Now each class has a single job, and
`PayrollCalculator` just calls the others in order.

| Class | What it does |
|-------|--------------|
| `PayrollCalculator` | Checks the input, finds the employee, then asks for gross pay and tax and works out the net. It has no pay rules of its own. |
| `GrossPayCalculator` | Hours times rate, plus overtime (anything over 40 hours is paid at time-and-a-half). |
| `TaxCalculator` and `TaxBand` | Finds the tax band that matches a gross pay and applies its rate. The bands are plain data. |
| `EmployeeRepository` | Where employees come from. It's handed to the calculator rather than created inside it. |
| `PayrollException`, `EmployeeNotFoundException`, `InvalidPayInputException` | The failures the service reports on purpose. |

`Employee` and `InMemoryEmployeeRepository` are exactly as I found them.

Because everything the calculator depends on is passed in through its constructor, any of
it can be swapped out in a test. That's how the tests simulate a missing employee or a
broken database.

## The tax band that's coming

Rosewood is about to add a new tax band, so I shaped the code to make that easy. Each band
only says where it **starts**, and it applies until the next band begins. That means adding
a band is one new entry in the list that `TaxCalculator` is built with, and nothing else
needs to change:

```java
new TaxBand("TOP", 20000, 0.30)
```

I chose "where it starts" over "where it starts and ends" on purpose. If `HIGH` had an upper
limit, adding `TOP` would mean editing `HIGH` too. This way existing bands are never
touched, and there's no way to leave a gap or an overlap between them.
`TaxCalculatorTest` has a test that adds a band and checks the old ones still behave.

One honest limit: a `TaxBand` is just a name, a starting point and a percentage. If the new
band turns out to work differently (a fixed levy, say), `TaxBand` would need to become an
interface. I haven't built that yet because I don't know that it's needed, and it's a small
change if it is.

## The decisions I made

- **Failures are loud now.** The original wrapped everything in `catch (Exception)` and
  returned `0`, so a missing employee got a payslip of zero and nobody was told. That's gone.
  An unknown employee throws `EmployeeNotFoundException`, and if the repository itself
  breaks, that error goes straight to the caller untouched.
- **"Employee not found" is checked on purpose.** In the original it only worked by accident:
  the code noticed a missing employee because a `println` crashed on it. Now the check is
  explicit.
- **No printing from inside a calculation.** The `println` was a side effect. Only `Demo`
  prints now.
- **Magic numbers have names.** `40` and `0.5` became constants, and the tax rates and limits
  live in one place instead of being scattered through an `if` chain.

## What I did with the AI's code and tests

| What it was | What I did | Why |
|-------------|-----------|-----|
| The overtime rule (40 hours, time-and-a-half) | **Kept** it, moved to `GrossPayCalculator` | The maths was right. Where it lived, and the bare numbers, were the problem. |
| The tax rates and band edges (5%, 15%, 25% at 1000 and 5000) | **Kept** the behaviour, rewrote the structure | These are business rules. It isn't my place to change them quietly. |
| `catch (Exception) { return 0; }` | **Removed** | It was the most serious defect, because it hid every failure. |
| `System.out.println` in the calculation | **Removed** | A side effect, and the reason "not found" only worked by accident. |
| `new InMemoryEmployeeRepository()` inside the calculator | **Rewrote** as constructor injection | It couldn't be tested or swapped. |
| Test `calculatesOvertimePay` | **Deleted** | It mocked the class it was meant to test, so it only tested Mockito. Overtime is now tested properly in `GrossPayCalculatorTest`. |
| Test `calculatesNetPay` | **Rewrote** with exact figures | It only checked that the answer was above zero, which almost anything satisfies. |
| Test `returnsPayForAnEmployee` | **Deleted** | It checked a `double` wasn't null, which can never fail. |

## My one extra: checking the input

The original accepted any numbers at all. `-5` hours produced a net pay of `-95.0`, and `NaN`
hours produced `NaN`. Negative, `NaN` and infinite hours or rates are now rejected with an
`InvalidPayInputException` before any money is worked out.

I picked this because a wrong payslip from bad input is the kind of mistake that slips
through unnoticed. It's a small change, and it closes a gap that the main fixes didn't cover.

## Things I left alone for the business to decide

1. **Money is still a `double`.** Amounts aren't rounded, so `1 hour @ 999.99` nets
   `949.9905`. Switching to `BigDecimal` would change the public API and every figure, and
   the brief didn't ask for it.
2. **Tax is a flat rate on the whole gross pay.** So earning slightly more can leave you with
   less: gross `999.99` nets `949.99`, but gross `1000.00` nets `850.00`. I've kept it exactly
   as it was. The new band is a good moment to ask whether bands should be marginal instead.
3. **`EmployeeRepository` returns `null` for "not found".** I didn't change that contract. The
   one place that calls it checks for `null` explicitly.
