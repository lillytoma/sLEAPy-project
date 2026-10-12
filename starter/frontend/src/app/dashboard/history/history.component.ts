import { Component } from '@angular/core';
import { transactions } from '../../data/mock-data';

@Component({
  selector: 'app-history',
  standalone: true,
  templateUrl: './history.component.html'
    
})
export class HistoryComponent {
  transactions = transactions;
}
