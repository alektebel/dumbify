# Dumbify Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Android OS                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Dumbify Application                       │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐    │  │
│  │  │  UI Layer (Activities)                       │    │  │
│  │  │  - MainActivity (toggles, status)            │    │  │
│  │  │  - SettingsActivity (API key config)         │    │  │
│  │  └──────────────────────────────────────────────┘    │  │
│  │                      ↓                                │  │
│  │  ┌──────────────────────────────────────────────┐    │  │
│  │  │  Repository Layer                            │    │  │
│  │  │  - AppConfigRepository                       │    │  │
│  │  │    • App configurations                      │    │  │
│  │  │    • Time limits                             │    │  │
│  │  │    • Blocked domains                         │    │  │
│  │  │    • SharedPreferences storage               │    │  │
│  │  └──────────────────────────────────────────────┘    │  │
│  │           ↓                ↓              ↓           │  │
│  │  ┌────────────┐  ┌──────────────┐  ┌──────────────┐ │  │
│  │  │  Services  │  │   Utilities   │  │  Receivers   │ │  │
│  │  └────────────┘  └──────────────┘  └──────────────┘ │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. UsageMonitorService (Background Service)

```
┌──────────────────────────────────────────┐
│      UsageMonitorService                 │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │  Initialization                    │ │
│  │  - Create notification channel     │ │
│  │  - Start foreground service        │ │
│  │  - Initialize repositories         │ │
│  └────────────────────────────────────┘ │
│              ↓                           │
│  ┌────────────────────────────────────┐ │
│  │  Main Monitoring Loop (5s)         │ │
│  │  1. Query UsageStatsManager        │ │
│  │  2. Parse usage events             │ │
│  │  3. Track session times            │ │
│  │  4. Update daily counters          │ │
│  └────────────────────────────────────┘ │
│              ↓                           │
│  ┌────────────────────────────────────┐ │
│  │  Analysis & Actions                │ │
│  │  - Check time limits               │ │
│  │  - Trigger AI analysis             │ │
│  │  - Send notifications              │ │
│  │  - Request app closure             │ │
│  └────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

### 2. AI Analysis Flow

```
User Opens App
     ↓
UsageMonitorService detects
     ↓
Check if AI enabled
     ↓
Build usage context:
  - App name
  - Time spent today
  - Open count
  - Recent app switches
     ↓
Send to AiAnalyzer
     ↓
AiAnalyzer.analyzeUsagePattern()
     ↓
Call Gemini API with prompt
     ↓
Receive AI response
     ↓
Parse analysis
     ↓
If problematic pattern detected:
  → Send notification with insight
```

### 3. DNS Filtering Flow

```
User Enables DNS Filter
     ↓
Request VPN permission
     ↓
User grants permission
     ↓
Start DnsFilterService
     ↓
Establish VPN interface
     ↓
┌─────────────────────────────┐
│   Packet Processing Loop    │
│                             │
│  App makes DNS request      │
│         ↓                   │
│  Packet intercepted by VPN  │
│         ↓                   │
│  Extract domain name        │
│         ↓                   │
│  Check against blocklist    │
│         ↓                   │
│  ┌──────────────────────┐   │
│  │ Domain blocked?      │   │
│  │  YES          NO     │   │
│  │   ↓            ↓     │   │
│  │ Return      Forward  │   │
│  │ NXDOMAIN    to DNS   │   │
│  └──────────────────────┘   │
└─────────────────────────────┘
```

### 4. App Closing Mechanism

```
Time Limit Exceeded
     ↓
UsageMonitorService detects
     ↓
Send notification: "Closing app"
     ↓
Broadcast Intent:
  "com.dumbify.CLOSE_APP"
  + package_name
     ↓
AppControlAccessibilityService receives
     ↓
Execute GLOBAL_ACTION_HOME
     ↓
User returns to home screen
```

### 5. Data Flow

```
┌──────────────────────────────────────────────┐
│           App Configuration                  │
│                                              │
│  AppConfigRepository                         │
│       ↓                                      │
│  SharedPreferences                           │
│       ↓                                      │
│  {                                           │
│    "app_configs": {                          │
│      "com.twitter.android": {                │
│        "timeLimit": 30,                      │
│        "warningThreshold": 20,               │
│        "category": "SOCIAL_MEDIA"            │
│      }                                       │
│    },                                        │
│    "blocked_domains": [                      │
│      {"domain": "bet365.com", ...}           │
│    ]                                         │
│  }                                           │
└──────────────────────────────────────────────┘
```

## Class Relationships

```
MainActivity
    ├── uses → AppConfigRepository
    ├── starts → UsageMonitorService
    └── starts → DnsFilterService

UsageMonitorService
    ├── uses → AppConfigRepository
    ├── uses → AiAnalyzer
    ├── queries → UsageStatsManager (Android)
    ├── sends → NotificationManager (Android)
    └── broadcasts → AppControlAccessibilityService

DnsFilterService
    ├── extends → VpnService (Android)
    └── uses → AppConfigRepository

AiAnalyzer
    ├── uses → GenerativeModel (Google AI)
    └── uses → AppConfigRepository

AppControlAccessibilityService
    └── extends → AccessibilityService (Android)

BootReceiver
    └── extends → BroadcastReceiver (Android)
```

## State Management

```
Application States:
├── Monitoring: OFF
│   ├── No usage tracking
│   └── No notifications
│
├── Monitoring: ON, AI: OFF
│   ├── Basic usage tracking
│   ├── Time limit enforcement
│   └── Simple notifications
│
├── Monitoring: ON, AI: ON
│   ├── Full usage tracking
│   ├── AI-powered insights
│   ├── Context-aware warnings
│   └── Daily summaries
│
└── DNS Filter: ON
    ├── VPN active
    ├── DNS interception
    └── Domain blocking
```

## Permission Flow

```
App Launch
    ↓
Check Permissions
    ├── Usage Stats?
    │   └── NO → Prompt user → Settings
    │
    ├── Accessibility?
    │   └── NO → Show dialog → Settings
    │
    └── VPN (when enabling DNS)?
        └── NO → Request via VpnService.prepare()
```

## Notification Strategy

```
Notification Types:

1. Foreground Service Notification
   - Always visible when monitoring
   - Low priority
   - Shows "Dumbify Active"

2. Warning Notification
   - Triggered at threshold
   - High priority
   - App-specific message

3. Closing Notification
   - Triggered at limit
   - High priority
   - "Time limit reached"

4. AI Insight Notification
   - Triggered by pattern analysis
   - Default priority
   - Personalized message

5. DNS Filter Notification
   - Always visible when filtering
   - Low priority
   - Shows "DNS Filter Active"
```

## Threading Model

```
Main Thread (UI)
    ├── Activity lifecycle
    ├── Button clicks
    └── UI updates

Background (Coroutines - Dispatchers.IO)
    ├── Usage stats queries
    ├── AI API calls
    ├── File I/O (SharedPreferences)
    └── Network requests

Background (Coroutines - Dispatchers.Default)
    ├── Service monitoring loops
    ├── Data processing
    └── Notification preparation

VPN Thread (Custom)
    └── Packet processing loop
```

## Key Design Decisions

### 1. Why Foreground Services?
- Ensures Android doesn't kill the monitoring process
- Required for persistent background work
- Provides user visibility

### 2. Why VPN for DNS Filtering?
- No root required
- Works system-wide
- Intercepts all DNS requests
- Easy to enable/disable

### 3. Why Accessibility Service?
- Only reliable way to close apps without root
- Can perform global actions
- Listens for broadcast intents

### 4. Why Gemini AI?
- Free tier available
- Good at conversational analysis
- Easy to integrate
- Can be disabled (optional)

### 5. Why SharedPreferences?
- Simple key-value storage
- No complex queries needed
- Fast access
- Built-in encryption on Android

## Performance Considerations

```
Monitoring Frequency: 5 seconds
    - Balance between responsiveness and battery
    - Adjustable via constant

AI Calls: On-demand only
    - Not every app switch
    - Only when usage > 10 minutes
    - Only for non-productive apps

DNS Filtering: Per-packet
    - Fast domain lookup (HashSet)
    - Minimal processing
    - No external DNS queries for blocked domains

Memory Usage:
    - ConcurrentHashMap for session tracking
    - Limited list sizes (top 20 recent apps)
    - Daily counter reset at midnight
```

## Security Considerations

```
1. Local Storage
   - SharedPreferences for non-sensitive data
   - API key stored locally (user responsibility)

2. Network
   - HTTPS for AI API calls (built-in to SDK)
   - VPN doesn't inspect packet contents
   - Only DNS queries parsed

3. Permissions
   - Only request what's needed
   - Explain each permission to user
   - No access to sensitive data
```

---

This architecture provides a solid foundation for digital wellbeing monitoring while maintaining user privacy and device performance.
