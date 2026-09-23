/** Mirrors the server password policy so users see problems before submitting. */
export function passwordProblems(password: string, confirmation: string): string[] {
  const problems: string[] = [];
  if (password.length < 10) {
    problems.push('At least 10 characters');
  }
  if (!/[A-Z]/.test(password)) {
    problems.push('An upper-case letter');
  }
  if (!/[a-z]/.test(password)) {
    problems.push('A lower-case letter');
  }
  if (!/\d/.test(password)) {
    problems.push('A digit');
  }
  if (!/[^A-Za-z0-9]/.test(password)) {
    problems.push('A symbol');
  }
  if (password !== confirmation) {
    problems.push('Both new passwords must match');
  }
  return problems;
}
