# Player Profile UI Redesign Plan

## Objective
Create a "crisp," high-fidelity Player Profile interface that adopts the layout and information density of the reference design, while seamlessly integrating pixel-art/sprite player assets to maintain the game's core aesthetic.

## Visual Identity & Style
- **Color Palette**: Dark, professional "sports broadcast" theme. Deep navy/charcoal backgrounds with high-contrast accents (e.g., gold for stars, sharp vibrant teal/green for progress, team-specific colors for highlights).
- **Typography**: 
    - **Names**: Bold, impactful sans-serif (e.g., Inter or Roboto) with optimized kerning for sharpness.
    - **Labels/Stats**: Highly legible monospaced font for numeric data to ensure perfect vertical alignment across stats tables.
- **Assets**:
    - **Character Sprites**: High-quality pixel art sprites in various poses, rendered with a consistent pixel-perfect outline to ensure they pop against the background.
    - **Icons**: Minimalist, modern vector-style icons with precise, clean lines.
    - **UI Elements**: Ultra-thin, sharp-edged panel borders, crisp circular gauges for OVR/Stats, and refined progress bars with subtle, defined gradients.

## Principles for a "Crisper" UI
To ensure the UI is even sharper than the reference, we will adhere to these design constraints:
- **Strict Grid Alignment**: Every component, text label, and icon must adhere to a rigid, pixel-perfect grid for absolute consistency.
- **Optimized Spacing (Whitespace)**: Ample, consistent padding between panels and text elements to eliminate visual clutter and increase readability.
- **Panel Definition**: Distinct, high-contrast panel backgrounds and sharp borders, ensuring each data group is visually isolated.
- **Text Clarity**: Using high-legibility fonts at scale, ensuring text is never fuzzy, even when resizing.
- **Enhanced Contrast**: Ensuring all text and data points have maximum readability against the dark, deep navy backgrounds.

## Interface Layout (Modular Design)


### 1. Header & Navigation
- **Breadcrumbs**: `ROSTERS > PLAYER PROFILE` (Top-left).
- **Tabs**: Horizontal navigation (OVERVIEW, RATINGS, STATS, HISTORY) with clear active/inactive states.

### 2. Primary Identity Block (Top Left/Center)
- **Player Identity**: Large Number, Full Name, Star Rating, and Archetype text.
- **OVR Gauge**: A prominent circular progress bar showing the current Overall rating.
- **XP Progress**: A sleek progress bar showing current XP vs. total XP required.
- **Quick Bio**: A concise list of Position, Class, Height, Weight, and Hometown.

### 3. Visual Anchor (Right Side)
- **Sprite Display**: The player's pixel-art sprite, prominently displayed.
- **Background**: A thematic, low-opacity team logo watermark or subtle gradient background to provide depth.

### 4. Data Panels (Main Body)
- **Attributes Panel**: A two-column grid of attribute names and styled progress bars (e.g., Throw Power, Speed, Awareness).
- **Archetypes Panel**: Icon-based list showing secondary archetypes and their relative ratings.
- **Season Stats Panel**: 
    - Key seasonal metrics (COMP/ATT, YARDS, etc.).
    - A large circular "Completion %" gauge for visual impact.
- **Career Stats Panel**: A horizontal summary bar displaying long-term career totals.
- **Development Panel**: Dedicated section for XP progression and "Skill Points Available" indicator.

### 5. Footer (Navigation Controls)
- **Input Legend**: Minimalist icons and text indicating controller mappings (e.g., `[ENTER] SELECT`, `[ESC] BACK`).
- **Team Branding**: Team name/logo in the bottom-right corner.

## Implementation Roadmap

### Phase 1: Asset Pipeline
- [ ] Develop/Acquire set of high-quality player sprites in various poses.
- [ ] Design the icon set for archetypes and attribute groups.
- [ ] Create themed UI background textures and gradients.

### Phase 2: Component Prototyping
- [ ] Implement the new Progress Bar and Circular Gauge components.
- [ ] Build the modular "Data Panel" containers.
- [ ] Design the typography/text styling system.

### Phase 3: Integration & Mapping
- [ ] Map existing `Player` and `Team` data models to the new UI components.
- [ ] Implement the tabbed navigation logic.
- [ ] Integrate the sprite rendering system into the profile view.

### Phase 4: Polish & Animation
- [ ] Add smooth transitions (e.g., fade-ins for panels, growing progress bars).
- [ ] Implement subtle hover/select animations for UI elements.
- [ ] Final color grading and contrast adjustments.
