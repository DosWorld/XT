{$E-,N+,R-,S-,O-}
program DoubleMathTest;

var
  a, b, c: Double;
  epsilon: Double;
  testCount, passCount: Integer;

procedure AssertAlmostEqual(actual, expected, eps: Double; testName: string);
begin
  if Abs(actual - expected) <= eps then
  begin
    passCount := passCount + 1;
    WriteLn(testName, ': PASS');
  end
  else
  begin
    WriteLn(testName, ': FAIL (', actual:0:12, ' vs ', expected:0:12, ')');
  end;
  testCount := testCount + 1;
end;

procedure AssertBoolean(cond: Boolean; testName: string);
begin
  if cond then
  begin
    passCount := passCount + 1;
    WriteLn(testName, ': PASS');
  end
  else
  begin
    WriteLn(testName, ': FAIL');
  end;
  testCount := testCount + 1;
end;

begin
  epsilon := 1e-12;
  testCount := 0;
  passCount := 0;

  a := 1.5;
  b := 2.3;
  AssertAlmostEqual(a + b, 3.8, epsilon, 'Addition');

  a := 5.7;
  b := 2.2;
  AssertAlmostEqual(a - b, 3.5, epsilon, 'Subtraction');

  a := 3.2;
  b := 1.5;
  AssertAlmostEqual(a * b, 4.8, epsilon, 'Multiplication');

  a := 7.5;
  b := 2.5;
  AssertAlmostEqual(a / b, 3.0, epsilon, 'Division');

  a := 3.2;
  AssertAlmostEqual(-a, -3.2, epsilon, 'UnaryMinus');

  AssertAlmostEqual(-5.0 * 2.0, -10.0, epsilon, 'MulNegative');
  AssertAlmostEqual(10.0 / -2.0, -5.0, epsilon, 'DivNegative');

  AssertAlmostEqual(Abs(-3.7), 3.7, epsilon, 'Abs');
  AssertAlmostEqual(Sqr(4.2), 17.64, epsilon, 'Sqr');
  AssertAlmostEqual(Sqrt(16.0), 4.0, epsilon, 'Sqrt');
  AssertAlmostEqual(Sin(0.0), 0.0, epsilon, 'Sin');
  AssertAlmostEqual(Cos(0.0), 1.0, epsilon, 'Cos');
  AssertAlmostEqual(ArcTan(1.0), Pi/4, epsilon, 'ArcTan');
  AssertAlmostEqual(Ln(Exp(1.0)), 1.0, epsilon, 'Ln_Exp');
  AssertAlmostEqual(Exp(1.0), 2.718281828459045, epsilon, 'Exp');

  AssertAlmostEqual(Frac(3.75), 0.75, epsilon, 'Frac');
  AssertAlmostEqual(Int(3.75), 3.0, epsilon, 'Int');

  AssertBoolean(Trunc(3.7) = 3, 'Trunc');
  AssertBoolean(Round(3.5) = 4, 'Round');
  AssertBoolean(Round(3.4) = 3, 'Round2');

  AssertBoolean(1.5 = 1.5, 'Equal true');
  AssertBoolean(1.5 <> 2.0, 'NotEqual true');
  AssertBoolean(1.5 < 2.0, 'Less true');
  AssertBoolean(2.0 > 1.5, 'Greater true');
  AssertBoolean(1.5 <= 1.5, 'LessEqual true');
  AssertBoolean(1.5 >= 1.5, 'GreaterEqual true');
  AssertBoolean(1.5 = 1.6, 'Equal false');
  AssertBoolean(1.5 <> 1.5, 'NotEqual false');
  AssertBoolean(2.0 < 1.5, 'Less false');
  AssertBoolean(1.5 > 2.0, 'Greater false');

  AssertAlmostEqual(Sin(Pi/2), 1.0, epsilon, 'Sin90');
  AssertAlmostEqual(Cos(Pi), -1.0, epsilon, 'Cos180');
  AssertAlmostEqual(ArcTan(0.0), 0.0, epsilon, 'ArcTan0');
  AssertAlmostEqual(Ln(1.0), 0.0, epsilon, 'Ln1');
  AssertAlmostEqual(Exp(0.0), 1.0, epsilon, 'Exp0');

  WriteLn;
  WriteLn('Total tests: ', testCount);
  WriteLn('Passed: ', passCount);
  WriteLn('Failed: ', testCount - passCount);
end.

