import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * TreeViewerComponent
 * Recursively renders a component tree (id, name, components, producer)
 * Compliance with v2 recursive requirement
 */
@Component({
  selector: 'app-tree-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="tree-node" [class.root]="isRoot">
      <div class="node-info" (click)="toggle()">
        <i class="material-icons expand-icon" *ngIf="node.components && node.components.length > 0">
          {{ expanded ? 'expand_more' : 'chevron_right' }}
        </i>
        <i class="material-icons node-type-icon">{{ getNodeIcon() }}</i>
        
        <div class="node-details">
          <span class="node-name">{{ node.name }}</span>
          <span class="node-meta">ID: {{ node.id }}</span>
          <span class="node-producer" *ngIf="node.producer">
            <i class="material-icons">factory</i> {{ node.producer.service }} | {{ node.producer.factory }}
          </span>
        </div>
        
        <span class="node-badge" *ngIf="node.type">{{ node.type }}</span>
      </div>

      <div class="children" *ngIf="expanded && node.components && node.components.length > 0">
        <app-tree-viewer 
          *ngFor="let child of node.components" 
          [node]="child" 
          [isRoot]="false">
        </app-tree-viewer>
      </div>
    </div>
  `,
  styles: [`
    .tree-node {
      margin-left: 1rem;
      border-left: 1px dashed rgba(255, 255, 255, 0.1);
      padding-left: 0.5rem;
      font-family: 'Inter', sans-serif;
    }

    .tree-node.root {
      margin-left: 0;
      border-left: none;
      padding-left: 0;
    }

    .node-info {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem;
      background: rgba(255, 255, 255, 0.03);
      border-radius: 0.5rem;
      margin-bottom: 0.5rem;
      cursor: pointer;
      transition: all 0.2s;
      border: 1px solid transparent;
    }

    .node-info:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(99, 102, 241, 0.3);
    }

    .expand-icon {
      font-size: 1.2rem;
      color: #94a3b8;
    }

    .node-type-icon {
      color: #6366f1;
    }

    .node-details {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .node-name {
      font-weight: 600;
      color: #f8fafc;
      font-size: 0.95rem;
    }

    .node-meta {
      font-size: 0.75rem;
      color: #64748b;
      font-family: monospace;
    }

    .node-producer {
      font-size: 0.7rem;
      color: #94a3b8;
      display: flex;
      align-items: center;
      gap: 0.3rem;
    }

    .node-producer .material-icons {
      font-size: 0.8rem;
    }

    .node-badge {
      font-size: 0.65rem;
      background: rgba(99, 102, 241, 0.2);
      color: #818cf8;
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
      font-weight: 700;
      text-transform: uppercase;
    }

    .children {
      margin-top: 0.25rem;
      animation: fadeIn 0.3s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-5px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class TreeViewerComponent {
  @Input() node: any;
  @Input() isRoot: boolean = true;
  
  expanded: boolean = true;

  toggle() {
    this.expanded = !this.expanded;
  }

  getNodeIcon(): string {
    if (!this.node.type) return 'inventory_2';
    const type = this.node.type.toUpperCase();
    if (type.includes('PRODUCT')) return 'directions_car';
    if (type.includes('COMPONENT')) return 'extension';
    if (type.includes('RAW')) return 'diamond';
    if (type.includes('PROCESSED')) return 'architecture';
    return 'inventory_2';
  }
}
