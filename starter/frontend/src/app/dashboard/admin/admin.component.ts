import { CommonModule } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminUser, AdminUserDetail } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styles: [`
    .admin-page { max-width: 1400px; margin: 0 auto; }
    .admin-toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin: 28px 0 18px; }
    .admin-search { flex: 1 1 280px; min-width: 220px; padding: 12px 14px; border: 1px solid var(--border); border-radius: 8px; background: var(--card); color: var(--foreground); outline: none; }
    .admin-filter { padding: 12px 14px; border: 1px solid var(--border); border-radius: 8px; background: var(--card); color: var(--foreground); }
    .admin-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(360px, .9fr); gap: 20px; align-items: start; }
    .admin-user-row { width: 100%; display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; gap: 12px; text-align: left; align-items: center; padding: 14px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--foreground); cursor: pointer; }
    .admin-user-row:hover, .admin-user-row.is-selected { background: var(--muted); }
    .admin-avatar { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; background: color-mix(in srgb, var(--primary) 14%, transparent); color: var(--primary); font-weight: 700; }
    .admin-user-name { font-weight: 650; }
    .admin-user-meta, .admin-muted { color: var(--muted-foreground); font-size: .8rem; }
    .admin-status { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; background: var(--muted); font-size: .75rem; white-space: nowrap; }
    .admin-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--success); }
    .admin-status.is-suspended::before { background: var(--error); }
    .admin-panel { overflow: hidden; }
    .admin-panel-heading { padding: 20px; border-bottom: 1px solid var(--border); }
    .admin-detail { padding: 20px; }
    .admin-detail-identity { display: flex; gap: 13px; align-items: center; margin-bottom: 22px; }
    .admin-detail-identity h2 { margin: 0; }
    .admin-detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-bottom: 24px; }
    .admin-detail-label { color: var(--muted-foreground); font-size: .72rem; text-transform: uppercase; letter-spacing: .06em; }
    .admin-detail-value { margin-top: 4px; color: var(--foreground); font-size: .9rem; word-break: break-word; }
    .admin-section-title { margin: 20px 0 8px; font-size: .95rem; }
    .admin-section-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .admin-section-heading .admin-section-title { margin-bottom: 8px; }
    .admin-see-more { border: 0; padding: 4px 0; background: transparent; color: var(--primary); font-size: .8rem; font-weight: 650; cursor: pointer; }
    .admin-see-more:hover { text-decoration: underline; }
    .admin-table-wrap { overflow-x: auto; }
    .admin-table { width: 100%; border-collapse: collapse; font-size: .82rem; }
    .admin-table th, .admin-table td { padding: 10px 8px; border-bottom: 1px solid var(--border); text-align: left; white-space: nowrap; }
    .admin-table th { color: var(--muted-foreground); font-weight: 500; }
    .admin-empty, .admin-error { padding: 34px 20px; text-align: center; color: var(--muted-foreground); }
    .admin-error { color: var(--error); }
    @media (max-width: 1000px) { .admin-layout { grid-template-columns: 1fr; } }
  `]
})
export class AdminComponent implements OnInit {
  users = signal<AdminUser[]>([]);
  selectedUser = signal<AdminUserDetail | null>(null);
  query = signal('');
  status = signal('all');
  loading = signal(true);
  error = signal('');
  showAllHoldings = signal(false);
  showAllActivity = signal(false);

  filteredUsers = computed(() => {
    const query = this.query().trim().toLowerCase();
    const status = this.status();
    return this.users().filter(user => {
      const matchesQuery = !query || [user.fullName, user.username, user.email].some(value => value.toLowerCase().includes(query));
      const matchesStatus = status === 'all' || user.status.toLowerCase() === status;
      return matchesQuery && matchesStatus;
    });
  });

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getUsers().subscribe({
      next: users => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: error => {
        this.error.set(error.status === 403 ? 'Administrator access is required to view this page.' : 'Users could not be loaded.');
        this.loading.set(false);
      }
    });
  }

  selectUser(user: AdminUser): void {
    this.selectedUser.set(null);
    this.showAllHoldings.set(false);
    this.showAllActivity.set(false);
    this.adminService.getUser(user.id).subscribe({
      next: detail => this.selectedUser.set(detail),
      error: () => this.error.set('The selected user could not be loaded.')
    });
  }

  toggleHoldings(): void {
    this.showAllHoldings.update(value => !value);
  }

  toggleActivity(): void {
    this.showAllActivity.update(value => !value);
  }

  initials(user: AdminUser): string {
    return user.fullName.split(' ').filter(Boolean).map(part => part[0]).join('').slice(0, 2).toUpperCase();
  }

  formatMoney(value: number | null): string {
    return value == null ? '—' : `$${Number(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}
