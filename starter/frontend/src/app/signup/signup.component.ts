import { Component, signal, computed } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ThemeService } from '../services/theme.service';

import { HttpClientModule } from '@angular/common/http';
import {SignupService} from '../services/signup.service';
// Component for handling user signup, including personal information, email, and password input with validation.
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
  templateUrl: './signup.component.html'
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
    public themeService: ThemeService,
    private signupService: SignupService
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
      city: ['', Validators.required],
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
    console.log('Form submitted. Form valid:', this.form.valid, 'Form errors:', this.form.errors);
    if (this.form.invalid) {
      console.log('Form is invalid. Marking all fields as touched.');
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.set(true);
    setTimeout(() => this.router.navigate(['/login']), 2000);
    // Prepare data matching backend DTO field names
    const signupData = {
      firstName: this.form.get('firstName')?.value,
      lastName: this.form.get('lastName')?.value,
      username: this.form.get('username')?.value,
      email: this.form.get('email')?.value,
      password: this.form.get('passwords')?.get('password')?.value,
      phoneNumber: this.form.get('phone')?.value,
      ssn: this.form.get('ssn')?.value,
      stateName: this.form.get('state')?.value,
      zipCode: this.form.get('zip')?.value,
      streetAddress: this.form.get('street')?.value,
      city: this.form.get('city')?.value,
      region: this.form.get('region')?.value,
      dob: this.form.get('dob')?.value
    };
    
    // Log data without password for security
    //const { password, ...safeData } = signupData;
    //console.log('Sending signup data to backend:', safeData);
    // Send data to backend
    this.signupService.signup(signupData).subscribe(
     {next: (response) => {
        console.log('Signup successful:', response);
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error:(error) => {
        console.error('Signup failed:', error);
        console.error('Error details - Status:', error.status, 'Message:', error.message, 'URL:', error.url);
        this.submitted.set(false);
      }}
    );
  }
}
