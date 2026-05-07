# Industry Simulator - Angular 17 Frontend

🏭 Production Pipeline Management Dashboard for the Industry Simulator distributed system project.

## Features

✅ **Pipeline Configuration**
- Configure production pipeline steps with custom durations
- Set up BOM (Bill of Materials) validation rules
- Visual pipeline builder with real-time duration calculation

✅ **Market Order Management**
- Create production orders with customer information
- Specify product type, quantity, and BOM version
- Priority-based order scheduling
- Delivery date management

✅ **Real-Time Monitoring**
- Live Kafka event streaming visualization
- Inventory level monitoring with stock alerts
- Production progress tracking
- Event statistics and metrics

✅ **Dashboard Analytics**
- Order completion statistics
- Performance metrics (uptime, processing rate, queue size)
- Event processing history
- System health indicators

## Prerequisites

- **Node.js** 18.x or higher
- **npm** 9.x or higher
- **Angular CLI** 17.x (optional, can use `npm start`)

## Installation

### 1. Install Dependencies

```bash
cd frontend-angular
npm install
```

### 2. Start the Development Server

```bash
npm start
```

The application will open automatically at `http://localhost:4200`

## Available Commands

| Command | Description |
|---------|-------------|
| `npm start` | Start development server with auto-reload |
| `npm run build` | Build for production |
| `npm run build:prod` | Build with optimization for production |
| `npm run watch` | Build in watch mode |
| `npm test` | Run unit tests |
| `npm run lint` | Run linter |

## Project Structure

```
frontend-angular/
├── src/
│   ├── app/
│   │   ├── components/
│   │   │   ├── dashboard/              # Main dashboard container
│   │   │   ├── pipeline-config/        # Pipeline configuration form
│   │   │   ├── order-form/             # Market order creation form
│   │   │   ├── inventory-monitor/      # Inventory monitoring component
│   │   │   └── events-monitor/         # Kafka events stream
│   │   ├── services/
│   │   │   ├── api.service.ts          # Backend API communication
│   │   │   └── event.service.ts        # Real-time event management
│   │   ├── models/
│   │   │   └── index.ts                # TypeScript interfaces
│   │   └── app.component.ts            # Root component
│   ├── assets/                         # Static assets
│   ├── styles/
│   │   └── styles.css                  # Global styles
│   ├── index.html                      # HTML entry point
│   └── main.ts                         # Angular bootstrap
├── package.json
├── angular.json
├── tsconfig.json
└── README.md
```

## Component Architecture

### AppComponent
Root component providing the layout shell with header, main content area, and footer.

### DashboardComponent
Main dashboard container with tabbed interface:
- **Configuration Tab**: Pipeline setup and order creation
- **Monitoring Tab**: Real-time events and inventory tracking
- **Statistics Tab**: Analytics and performance metrics
- **Help Tab**: User guide and system documentation

### PipelineConfigComponent
Allows users to configure production pipelines by:
- Defining pipeline steps
- Setting step durations (in milliseconds)
- Calculating total pipeline duration
- Saving configurations

### OrderFormComponent
Market order creation form with:
- Customer information input
- Product type selection (SEDAN, SUV, TRUCK, SPORTS)
- Order quantity and BOM version
- Priority levels and delivery dates
- Form validation

### InventoryMonitorComponent
Real-time inventory monitoring:
- Stock level display with color coding
- Low stock alerts
- Batch tracking
- Warehouse location info
- Refresh functionality

### EventsMonitorComponent
Kafka event stream visualization:
- Real-time event feed (up to 50 recent events)
- Event type classification and color coding
- Event statistics
- Pause/resume and clear options
- Event details expansion

## Services

### ApiService
Handles all HTTP communication with backend microservices:
- **Raw Material Service** (http://localhost:8081)
- **Processing Service** (http://localhost:8082)
- **Component Service** (http://localhost:8083)
- **Assembly Service** (http://localhost:8084)

**Key Methods:**
- `createMarketOrder()` - Create new production order
- `getInventory()` - Fetch inventory items
- `getComponents()` - Get component list
- `healthCheck()` - Service health status
- Loading and error state management

### EventService
Manages real-time events and production status:
- Kafka event collection and broadcasting
- Production status tracking
- Inventory update notifications
- Event polling mechanism (can be extended for WebSocket)

## API Integration

The frontend communicates with four microservices:

### Raw Material Service (8081)
```
GET /api/raw-materials/health
GET /api/raw-materials
GET /api/raw-materials/batch/{batchId}
POST /api/raw-materials
```

### Processing Service (8082)
```
GET /api/processing/health
GET /api/processing
POST /api/processing/start
GET /api/processing/status/{batchId}
```

### Component Service (8083)
```
GET /api/components/health
GET /api/components
GET /api/components/bom
GET /api/components/bom/{bomId}
POST /api/components/validate-bom
```

### Assembly Service (8084)
```
GET /api/assembly/health
GET /api/assembly/inventory
GET /api/assembly/inventory/{productType}
GET /api/assembly/market-orders
POST /api/assembly/market-orders
POST /api/assembly/assembly/start
```

## Styling Guide

The application uses:
- **CSS Custom Properties** for consistent theming
- **CSS Grid** for responsive layouts
- **CSS Flexbox** for component alignment
- **CSS Transitions** for smooth animations

### Color Scheme
- **Primary**: #667eea (Purple)
- **Secondary**: #764ba2 (Dark Purple)
- **Success**: #4caf50 (Green)
- **Error**: #f44336 (Red)
- **Warning**: #ff9800 (Orange)
- **Info**: #2196f3 (Blue)

## Building for Production

```bash
npm run build:prod
```

This generates an optimized build in the `dist/industry-simulator/` directory.

Deploy the contents of this directory to your web server:
```bash
# Example with simple HTTP server
npx http-server dist/industry-simulator/
```

## Development Tips

1. **Hot Module Replacement (HMR)**
   - Changes are automatically detected and reloaded
   - Preserves application state during development

2. **DevTools**
   - Use Angular DevTools Chrome extension for debugging
   - Redux DevTools for state management inspection

3. **Testing**
   - Run `npm test` for unit tests
   - Tests use Jasmine and Karma
   - Component tests should focus on user interactions

4. **Performance**
   - Use Angular's OnPush change detection for optimization
   - Signals provide fine-grained reactivity
   - Lazy loading for large feature modules

## Troubleshooting

### Connection Errors
- Ensure backend services are running on correct ports
- Check CORS configuration in backend
- Verify firewall settings

### Missing Dependencies
```bash
rm -rf node_modules package-lock.json
npm install
```

### Port Already in Use
```bash
ng serve --port 4300  # Use different port
```

### Build Errors
```bash
ng build --configuration development  # Debug build
ng build --verbose  # Verbose output
```

## Future Enhancements

📋 **Planned Features:**
- WebSocket integration for real-time updates
- Advanced filtering and search in event monitor
- Export reports to CSV/PDF
- Dark mode theme
- Multi-language support
- User authentication and authorization
- Order history and analytics
- Component performance profiling
- Stress testing tools

## Technologies Used

| Technology | Version | Purpose |
|-----------|---------|---------|
| Angular | 17 | Frontend framework |
| TypeScript | 5.2+ | Programming language |
| RxJS | 7.8+ | Reactive programming |
| Bootstrap | CSS Grid | Responsive layout |
| Angular CLI | 17 | Development tooling |

## License

MIT - See LICENSE file in project root

## Contributing

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request

## Support

For issues or questions:
1. Check the Help tab in the dashboard
2. Review backend API documentation
3. Check service logs for errors

---

**Happy Production! 🚀**
