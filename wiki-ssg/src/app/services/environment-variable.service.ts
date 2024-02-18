export class EnvironmentVariableService {

  static getOrFail(name: string): string {
    if(process) {
      const value = process.env[name];
      if(!value) {
        throw new Error(`Missing env var ${name}`);
      }
      return value;
    } else {
      return '';
    }
  }
  static doWith(name: string, action: (value: string) => {}, defaultValue?: string): void {
    const val = process?.env[name]
      ? process.env[name]
      : (defaultValue);
    if(val) {
      action(val);
    }
  }
}
