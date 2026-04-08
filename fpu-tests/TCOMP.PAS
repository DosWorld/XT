{$E-,N+,R-,S-,O-}
program CompMathTest;

var
  a, b, c: Comp;
  epsilon: Extended;
  testCount, passCount: Integer;

procedure AssertCompEqual(actual, expected: Comp; testName: string);
begin
  if actual = expected then
  begin
    passCount := passCount + 1;
    WriteLn(testName, ': PASS');
  end
  else
  begin
    WriteLn(testName, ': FAIL (', actual:0:0, ' vs ', expected:0:0, ')');
  end;
  testCount := testCount + 1;
end;

procedure AssertAlmostEqual(actual, expected, eps: Extended; testName: string);
begin
  if Abs(actual - expected) <= eps then
  begin
    passCount := passCount + 1;
    WriteLn(testName, ': PASS');
  end
  else
  begin
    WriteLn(testName, ': FAIL (', actual:0:10, ' vs ', expected:0:10, ')');
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

  a := 100000;
  b := 200000;
  AssertCompEqual(a + b, 300000, 'Addition');

  a := 200000;
  b := 100000;
  AssertCompEqual(a - b, 100000, 'Subtraction');

  a := 100000;
  AssertCompEqual(a * 2, 200000, 'Multiplication');

  a := 100000;
  AssertAlmostEqual(a / 2, 50000.0, epsilon, 'Division');

  a := 100000;
  AssertCompEqual(-a, -100000, 'UnaryMinus');

  AssertCompEqual(-5 * 3, -15, 'MulNegative');

  a := 30000;
  b := 20000;
  AssertCompEqual(a + b, 50000, 'MediumAdd');

  a := 32767;
  b := 1;
  AssertCompEqual(a + b, 32768, 'OverflowCheck');

  a := -50000;
  AssertCompEqual(Abs(a), 50000, 'Abs');

  a := 12345;
  AssertCompEqual(Sqr(a), 152399025, 'Sqr');

  a := 123456;
  AssertCompEqual(Trunc(a + 0.7), a, 'Trunc');
  AssertCompEqual(Round(a + 0.5), a + 1, 'Round');
  AssertCompEqual(Round(a + 0.4), a, 'RoundDown');

  a := 12345;
  AssertBoolean(Int(a) = a, 'Int');
  AssertAlmostEqual(Frac(a), 0.0, epsilon, 'Frac');

  AssertBoolean(a = a, 'Equal true');
  AssertBoolean(a <> a + 1, 'NotEqual true');
  AssertBoolean(a < a + 1, 'Less true');
  AssertBoolean(a + 1 > a, 'Greater true');
  AssertBoolean(a <= a, 'LessEqual true');
  AssertBoolean(a >= a, 'GreaterEqual true');
  AssertBoolean(a = a + 1, 'Equal false');
  AssertBoolean(a <> a, 'NotEqual false');
  AssertBoolean(a + 1 < a, 'Less false');
  AssertBoolean(a > a + 1, 'Greater false');

  WriteLn;
  WriteLn('Total tests: ', testCount);
  WriteLn('Passed: ', passCount);
  WriteLn('Failed: ', testCount - passCount);
end.

