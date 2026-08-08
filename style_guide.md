# College Football Head Coach (CFHC) — Style Guide

This style guide defines the visual design system, color palette tokens, typography hierarchy, UI component specifications, and pixel-art asset standards for the **College Football Head Coach (CFHC)** dark sports broadcast console HUD user interface.

---

## 1. Color System Tokens

### Primary Palette
| Token | Hex Code | RGB | Purpose |
| :--- | :--- | :--- | :--- |
| `Midnight Obsidian` | `#060C14` | `rgb(6, 12, 20)` | Main window, app viewport background, outer viewport frame |
| `Obsidian Subtitle` | `#0A111C` | `rgb(10, 17, 28)` | Sidebar navigation container, dropdown menus |
| `Card Slate` | `#0D1726` | `rgb(13, 23, 38)` | Primary dashboard card containers, default panel fill |
| `Card Slate Elevated` | `#111C2E` | `rgb(17, 28, 46)` | Sub-cards, table headers, hover elevation panels |
| `Card Slate Hover` | `#16243B` | `rgb(22, 36, 59)` | Interactive element hover states |
| `Dark Slate Border` | `#1E293B` | `rgb(30, 41, 59)` | 1px panel borders, table grid lines |
| `Border Highlight` | `#2A3A52` | `rgb(42, 58, 82)` | Active card hover border glow |

### Accent Palette
| Token | Hex Code | RGB | Purpose |
| :--- | :--- | :--- | :--- |
| `Neon Emerald Green` | `#00E676` | `rgb(0, 230, 118)` | Overall rating digits, active tab indicators, high morale |
| `Athletic Green` | `#10B981` | `rgb(16, 185, 129)` | Progress bar fills, positive trends |
| `Trophy Gold` | `#F59E0B` | `rgb(245, 158, 11)` | National rank #, star ratings, prestige badges, script accents |
| `Amber Accent` | `#FBBF24` | `rgb(251, 191, 36)` | Secondary star highlight |
| `Crimson Maroon` | `#881337` | `rgb(136, 19, 55)` | Matchup opponent banners, negative factors, game day highlight |
| `Maroon Bright` | `#9F1239` | `rgb(159, 18, 57)` | Alert text, game day card borders |

---

## 2. Typography System

### Font Families
1. **Display & Header Font**: `Outfit` / `Inter` (Bold, ExtraBold, Heavy)
   - Used for school names, card titles, overall rating digits, and major stat headings.
2. **Body & Interface Font**: `Inter` (Regular, Medium, SemiBold)
   - Used for sidebar nav items, table rows, metadata labels, and body text.
3. **Monospaced Numeric Font**: `JetBrains Mono` / `Consolas`
   - Used for numbers, stats tables, records, financial figures, and clock times.
4. **Script Mascot Font**: `Caveat` / `Brush Script`
   - Used exclusively for team nickname accents ("*Owls*", "*Wolverines*").

### Hierarchy & Sizes
- **Headline XXL (Rating Digits)**: 64px Bold (Line height: 1.0)
- **School Title XL**: 26px ExtraBold (Upper-case, Letter spacing: 1px)
- **Mascot Script XL**: 28px Cursive
- **Header Section L**: 14px ExtraBold (Letter spacing: 0.5px)
- **Card Title M**: 11px Heavy (Upper-case, Letter spacing: 1px)
- **Body Standard**: 11px Medium (Line height: 1.4)
- **Stat Label Micro**: 9px Heavy (Letter spacing: 0.5px, Color: `--text-muted`)

---

## 3. UI Component Specifications

### 1. Panel & Card Container
- **Background**: `#0D1726` (Card Slate)
- **Border**: `1px solid #1E293B`
- **Border Radius**: `8px` (`--radius-md`)
- **Padding**: `12px 14px`
- **Hover Transition**: `box-shadow 0.15s ease, border-color 0.15s ease`
- **Hover Shadow**: `0 4px 16px rgba(0, 0, 0, 0.4)`

### 2. Rating Badge & Star Gauge
- **Overall Rating Digit**: 64px Neon Emerald (`#00E676`) with text-shadow glow `0 0 16px rgba(0, 230, 118, 0.25)`.
- **Grade Pill Badge**: Background `#111C2E`, Border `1px solid #1E293B`, Text `#00E676`, Padding `2px 8px`, Radius `4px`.
- **Stars**: Filled `★` in Trophy Gold (`#F59E0B`), Empty `☆` in Dark Slate (`#1E293B`).

### 3. Sidebar Navigation Menu Item
- **Height**: 36px
- **Padding**: `10px 18px`
- **Active Fill**: `linear-gradient(90deg, rgba(0, 230, 118, 0.15) 0%, rgba(0, 230, 118, 0) 100%)`
- **Active Left Indicator**: `4px` solid `#00E676` with `box-shadow 0 0 8px #00E676`.
- **Badge Counter**: Pill background `#F59E0B`, text `#000`, 10px bold, radius `10px`.

### 4. Roster Spotlight Player Card
- **Background**: `#060C14` (Midnight Obsidian) inside `#0D1726` card frame.
- **Avatar Container**: `44px x 52px`, `1px solid #1E293B`, `border-radius: 4px`.
- **Sprite Image**: Pixelated rendering (`image-rendering: pixelated`), cropped top-center.
- **Stats Cell Grid**: Background `#111C2E`, rounded 4px, monospace stat values.

### 5. Status Footer & Audio Ticker
- **Height**: 36px
- **Background**: `#050A12` with top border `1px solid #1E293B`.
- **Controller Legend Buttons**: 16px circular badge with black bold text (`(A)` Green `#00E676`, `(B)` Red `#EF4444`, `(Y)` Gold `#F59E0B`).
- **Audio Equalizer**: 4 animated spectrum bars (`width: 2px`, `background: #00E676`, `height: 40%-100%`).

---

## 4. Pixel-Art Asset Pipeline
- **Resolution**: 16-bit retro arcade aesthetic (60x70px sprite box).
- **Outlines**: 1px sharp dark pixel outline for contrast against dark backgrounds.
- **Color Palette Constraints**: Clean skin tones, team primary/secondary uniform colors, high-contrast helmet facemasks.
