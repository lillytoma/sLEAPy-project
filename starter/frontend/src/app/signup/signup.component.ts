import { Component, signal, computed } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ThemeService } from '../services/theme.service';

function passwordStrength(password: string): number {
  if (!password) return 0;
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  return score;
}

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const pw = control.get('password')?.value;
  const cpw = control.get('confirmPassword')?.value;
  return pw && cpw && pw !== cpw ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule],
  template: `
    <div class="min-h-screen py-10 px-4" style="background-color:var(--background);color:var(--foreground)">
      <!-- Dark Mode Toggle -->
      <button (click)="themeService.toggleDark()"
        class="fixed top-4 right-4 p-2.5 rounded-full border z-10"
        style="background-color:var(--card);border-color:var(--border);color:var(--foreground)">
        @if (themeService.dark()) {
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
        } @else {
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        }
      </button>

      <div class="max-w-2xl mx-auto">
        <div class="text-center mb-8">
          <a routerLink="/" class="font-serif text-3xl font-bold" style="color:var(--primary)">sLEAPy Stocks</a>
          <p class="mt-2 text-sm" style="color:var(--muted-foreground)">Create your account</p>
        </div>

        <div class="p-8 rounded-xl border" style="background-color:var(--card);border-color:var(--border)">
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-6">
            <!-- Personal Info -->
            <div>
              <h3 class="font-semibold text-base mb-4 pb-2 border-b" style="color:var(--foreground);border-color:var(--border)">Personal Information</h3>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">First Name</label>
                  <input type="text" formControlName="firstName" placeholder="John"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['firstName'].invalid && f['firstName'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">First name is required</p>
                  }
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Last Name</label>
                  <input type="text" formControlName="lastName" placeholder="Doe"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['lastName'].invalid && f['lastName'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Last name is required</p>
                  }
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Date of Birth</label>
                  <input type="date" formControlName="dob" [max]="maxDob"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['dob'].hasError('required') && f['dob'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Date of birth is required</p>
                  }
                  @if (f['dob'].hasError('underage') && f['dob'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">You must be at least 18 years old</p>
                  }
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Phone Number</label>
                  <input type="tel" formControlName="phone" placeholder="(555) 123-4567"
                    (input)="formatPhone($event)"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['phone'].invalid && f['phone'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Valid phone number required</p>
                  }
                </div>
              </div>
            </div>

            <!-- Address -->
            <div>
              <h3 class="font-semibold text-base mb-4 pb-2 border-b" style="color:var(--foreground);border-color:var(--border)">Address</h3>
              <div class="space-y-4">
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Street Address</label>
                  <input type="text" formControlName="street" placeholder="123 Main St"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['street'].invalid && f['street'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Street address is required</p>
                  }
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">ZIP Code</label>
                    <input type="text" formControlName="zip" placeholder="10001"
                      class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                      style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                    @if (f['zip'].invalid && f['zip'].touched) {
                      <p class="text-xs mt-1" style="color:var(--error)">ZIP code required</p>
                    }
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Region</label>
                    <select formControlName="region"
                      class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                      style="background-color:var(--background);color:var(--foreground);border-color:var(--border)">
                      <option value="">Select region</option>
                      <option value="USA">USA</option>
                      <option value="India">India</option>
                      <option value="UK">UK</option>
                    </select>
                    @if (f['region'].invalid && f['region'].touched) {
                      <p class="text-xs mt-1" style="color:var(--error)">Region is required</p>
                    }
                  </div>
                  @if (f['region'].value === 'USA') {
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">State</label>
                      <select formControlName="state"
                        class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                        style="background-color:var(--background);color:var(--foreground);border-color:var(--border)">
                        <option value="">Select state</option>
                        @for (s of usStates; track s) {
                          <option [value]="s">{{ s }}</option>
                        }
                      </select>
                      @if (f['state'].invalid && f['state'].touched && f['region'].value === 'USA') {
                        <p class="text-xs mt-1" style="color:var(--error)">State is required for USA</p>
                      }
                    </div>
                  }
                </div>
              </div>
            </div>

            <!-- Identity -->
            <div>
              <h3 class="font-semibold text-base mb-4 pb-2 border-b" style="color:var(--foreground);border-color:var(--border)">Identity Verification</h3>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Social Security Number (SSN)</label>
                <div class="relative">
                  <input [type]="showSSN() ? 'text' : 'password'" formControlName="ssn"
                    placeholder="XXX-XX-XXXX"
                    (input)="formatSSN($event)"
                    class="w-full px-4 py-2.5 pr-10 rounded-md text-sm border outline-none font-mono"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  <button type="button" (click)="showSSN.set(!showSSN())"
                    class="absolute right-3 top-1/2 -translate-y-1/2"
                    style="color:var(--muted-foreground)">
                    @if (showSSN()) {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                        <line x1="1" y1="1" x2="23" y2="23"/>
                      </svg>
                    } @else {
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                      </svg>
                    }
                  </button>
                </div>
                <p class="text-xs mt-1" style="color:var(--muted-foreground)">Used to verify your identity. Encrypted and never shared.</p>
                @if (f['ssn'].invalid && f['ssn'].touched) {
                  <p class="text-xs mt-1" style="color:var(--error)">Valid SSN required (XXX-XX-XXXX)</p>
                }
              </div>
            </div>

            <!-- Account Info -->
            <div>
              <h3 class="font-semibold text-base mb-4 pb-2 border-b" style="color:var(--foreground);border-color:var(--border)">Account Information</h3>
              <div class="space-y-4">
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Username</label>
                  <input type="text" formControlName="username" placeholder="johndoe42"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['username'].invalid && f['username'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Username is required (3+ chars)</p>
                  }
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Email Address</label>
                  <input type="email" formControlName="email" placeholder="john@example.com"
                    class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                    style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                  @if (f['email'].invalid && f['email'].touched) {
                    <p class="text-xs mt-1" style="color:var(--error)">Valid email is required</p>
                  }
                </div>
                <!-- Password group -->
                <div formGroupName="passwords">
                  <div class="mb-4">
                    <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Password</label>
                    <input type="password" formControlName="password" placeholder="Min. 8 characters"
                      (input)="updatePasswordStrength()"
                      class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                      style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />

                    <!-- Strength meter -->
                    <div class="mt-2 flex gap-1">
                      @for (i of [0,1,2,3]; track i) {
                        <div class="h-1.5 flex-1 rounded-full transition-all"
                          [style.background-color]="i < pwStrength() ? pwStrengthColor() : 'var(--muted)'"></div>
                      }
                    </div>
                    <p class="text-xs mt-1" [style.color]="pwStrengthColor()">{{ pwStrengthLabel() }}</p>

                    <!-- Requirements checklist -->
                    <ul class="mt-2 space-y-1">
                      @for (req of pwRequirements(); track req.label) {
                        <li class="flex items-center gap-2 text-xs">
                          <span [style.color]="req.met ? 'var(--success)' : 'var(--muted-foreground)'">
                            {{ req.met ? '✓' : '○' }}
                          </span>
                          <span [style.color]="req.met ? 'var(--foreground)' : 'var(--muted-foreground)'">{{ req.label }}</span>
                        </li>
                      }
                    </ul>

                    @if (pg['password'].invalid && pg['password'].touched) {
                      <p class="text-xs mt-1" style="color:var(--error)">Password must be at least 8 characters</p>
                    }
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color:var(--foreground)">Confirm Password</label>
                    <input type="password" formControlName="confirmPassword" placeholder="Repeat password"
                      class="w-full px-4 py-2.5 rounded-md text-sm border outline-none"
                      style="background-color:var(--background);color:var(--foreground);border-color:var(--border)" />
                    @if (passwordsGroup.hasError('passwordMismatch') && pg['confirmPassword'].touched) {
                      <p class="text-xs mt-1" style="color:var(--error)">Passwords do not match</p>
                    }
                  </div>
                </div>
              </div>
            </div>

            <!-- Submit -->
            <button type="submit" [disabled]="form.invalid"
              class="w-full py-3 rounded-md font-semibold text-sm transition-opacity"
              style="background-color:var(--primary);color:var(--primary-foreground)"
              [style.opacity]="form.invalid ? '0.5' : '1'">
              Create Account
            </button>

            @if (submitted()) {
              <div class="px-4 py-3 rounded-md text-sm text-center" style="background-color:var(--success);color:#fff">
                Account created successfully! Redirecting to login...
              </div>
            }
          </form>
        </div>

        <p class="mt-6 text-center text-sm" style="color:var(--muted-foreground)">
          Already have an account?
          <a routerLink="/login" class="font-semibold" style="color:var(--primary)">Sign in</a>
        </p>
        <p class="mt-2 text-center text-xs" style="color:var(--muted-foreground)">
          <a routerLink="/" class="hover:underline">← Back to home</a>
        </p>
      </div>
    </div>
  `,
})
export class SignupComponent {
  form: FormGroup;
  showSSN = signal(false);
  submitted = signal(false);
  pwStrength = signal(0);

  usStates = ['AL','AK','AZ','AR','CA','CO','CT','DE','FL','GA','HI','ID','IL','IN','IA','KS','KY','LA','ME','MD','MA','MI','MN','MS','MO','MT','NE','NV','NH','NJ','NM','NY','NC','ND','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VT','VA','WA','WV','WI','WY'];

  maxDob: string;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    public themeService: ThemeService
  ) {
    const today = new Date();
    today.setFullYear(today.getFullYear() - 18);
    this.maxDob = today.toISOString().split('T')[0];

    this.form = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      dob: ['', [Validators.required, this.ageValidator]],
      phone: ['', [Validators.required, Validators.pattern(/^\(\d{3}\) \d{3}-\d{4}$/)]],
      street: ['', Validators.required],
      zip: ['', [Validators.required, Validators.pattern(/^\d{5}(-\d{4})?$/)]],
      region: ['', Validators.required],
      state: [''],
      ssn: ['', [Validators.required, Validators.pattern(/^\d{3}-\d{2}-\d{4}$/)]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      passwords: this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
      }, { validators: passwordMatchValidator }),
    });
  }

  get f() { return this.form.controls; }
  get passwordsGroup(): FormGroup { return this.form.get('passwords') as FormGroup; }
  get pg() { return this.passwordsGroup.controls; }

  ageValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    const dob = new Date(control.value);
    const today = new Date();
    const age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    const actualAge = m < 0 || (m === 0 && today.getDate() < dob.getDate()) ? age - 1 : age;
    return actualAge < 18 ? { underage: true } : null;
  }

  pwStrengthColor = computed(() => {
    const s = this.pwStrength();
    if (s <= 1) return 'var(--error)';
    if (s === 2) return '#F59E0B';
    if (s === 3) return '#3B82F6';
    return 'var(--success)';
  });

  pwStrengthLabel = computed(() => {
    const s = this.pwStrength();
    if (s === 0) return '';
    if (s === 1) return 'Weak';
    if (s === 2) return 'Fair';
    if (s === 3) return 'Good';
    return 'Strong';
  });

  pwRequirements = computed(() => {
    const pw = this.pg['password']?.value ?? '';
    return [
      { label: 'At least 8 characters', met: pw.length >= 8 },
      { label: 'Uppercase letter', met: /[A-Z]/.test(pw) },
      { label: 'Number', met: /[0-9]/.test(pw) },
      { label: 'Special character', met: /[^A-Za-z0-9]/.test(pw) },
    ];
  });

  updatePasswordStrength(): void {
    const pw = this.pg['password']?.value ?? '';
    this.pwStrength.set(passwordStrength(pw));
  }

  formatPhone(event: Event): void {
    const input = event.target as HTMLInputElement;
    let val = input.value.replace(/\D/g, '');
    if (val.length > 10) val = val.substring(0, 10);
    let formatted = '';
    if (val.length > 0) formatted = '(' + val.substring(0, 3);
    if (val.length >= 4) formatted += ') ' + val.substring(3, 6);
    if (val.length >= 7) formatted += '-' + val.substring(6, 10);
    input.value = formatted;
    this.form.patchValue({ phone: formatted }, { emitEvent: false });
  }

  formatSSN(event: Event): void {
    const input = event.target as HTMLInputElement;
    let val = input.value.replace(/\D/g, '');
    if (val.length > 9) val = val.substring(0, 9);
    let formatted = val;
    if (val.length > 3) formatted = val.substring(0, 3) + '-' + val.substring(3);
    if (val.length > 5) formatted = val.substring(0, 3) + '-' + val.substring(3, 5) + '-' + val.substring(5);
    input.value = formatted;
    this.form.patchValue({ ssn: formatted }, { emitEvent: false });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.set(true);
    setTimeout(() => this.router.navigate(['/login']), 2000);
  }
}
